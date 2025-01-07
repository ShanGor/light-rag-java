package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.storage.BaseGraphStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

import static cn.gzten.rag.util.LightRagUtils.pythonTemplateFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGGraphStorage implements BaseGraphStorage {
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    @Value("${rag.storage.graph.name}")
    private String graphName;

    public List<Map<String, Object>> query(String query, boolean readOnly) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("LOAD 'age'");
            stmt.execute("SET search_path = ag_catalog, \"$user\", public");
            try {
                stmt.execute("select create_graph('%s')".formatted(graphName));
            } catch (SQLException e) {
                if (e.getMessage().contains("already exists")) {
                    log.warn("Graph already exists, skip creating it");
                } else {
                    throw e;
                }
            }

            if (readOnly) {
                var rs = stmt.executeQuery(query);
                return mapRows(rs);
            } else {
                /*
                 * For upserting an edge, need to run the SQL twice, otherwise cannot update the properties. (First time it will try to create the edge, second time is MERGING)
                 * It is a bug of AGE as of 2025-01-03, hope it can be resolved in the future.
                 */
                stmt.execute(query);
                return List.of();
            }
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to execute query", query, e.getMessage());
        }
    }

    public static String encodeGraphLabel(String label) {
        return "x%s".formatted(HexFormat.of().formatHex(label.getBytes(StandardCharsets.UTF_8)));
    }
    public static String decodeGraphLabel(String encodedLabelWithPrefix) {
        var encodedLabel = encodedLabelWithPrefix.substring(1);
        return new String(HexFormat.of().parseHex(encodedLabel), StandardCharsets.UTF_8);
    }

    @Override
    public boolean hasNode(String nodeId) {
        var entity_name_label = StringUtils.strip(nodeId, "\"");

        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (n:`{label}`) RETURN count(n) > 0 AS node_exists
            $$) AS (node_exists bool)""";
        Map<String, Object> params = Map.of(
                "label", PGGraphStorage.encodeGraphLabel(entity_name_label),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        var single_result = this.query(wrappedQuery, true).get(0);
        var result = single_result.get("node_exists");
        log.debug(
                "query node:{}, result:{}",
                wrappedQuery,
                result
        );

        return (Boolean) result;
    }

    @Override
    public boolean hasEdge(String source_node_id, String target_node_id) {
        var entity_name_label_source = StringUtils.strip(source_node_id, "\"");
        var entity_name_label_target = StringUtils.strip(target_node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (a:`{src_label}`)-[r]-(b:`{tgt_label}`)
                RETURN COUNT(r) > 0 AS edge_exists
            $$) AS (edge_exists bool)""";

        Map<String, Object> params = Map.of(
                "graph_name", graphName,
                "src_label", PGGraphStorage.encodeGraphLabel(entity_name_label_source),
                "tgt_label", PGGraphStorage.encodeGraphLabel(entity_name_label_target)
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        var single_result = query(wrappedQuery, true).get(0);
        var result = single_result.get("edge_exists");
        log.debug("query edge:{}, result:{}", wrappedQuery, result);
        return (Boolean) result;
    }

    @Override
    public int nodeDegree(String node_id) {
        var entity_name_label = StringUtils.strip(node_id, "\"");

        var query = """
           SELECT * FROM ag_catalog.cypher('{graph_name}', $$
             MATCH (n:`{label}`)-[]->(x) RETURN count(x) AS total_edge_count
           $$) AS (total_edge_count integer)""";
        Map<String, Object> params = Map.of("label", PGGraphStorage.encodeGraphLabel(entity_name_label),
                "graph_name", graphName);
        var wrappedQuery = pythonTemplateFormat(query, params);
        var records = query(wrappedQuery, true);
        if (!records.isEmpty()) {
            var record = records.get(0);
            var edge_count = (Integer) record.get("total_edge_count");
            log.debug("NodeDegree query:{}:result:{}", wrappedQuery, edge_count);
            return edge_count;
        }
        return 0;
    }

    @Override
    public int edgeDegree(String srcId, String tgtId) {
        var src_degree = nodeDegree(srcId);
        var trg_degree = nodeDegree(tgtId);

        return src_degree + trg_degree;
    }

    @Override
    public Map<String, Object> getNode(String node_id) {
        var entity_name_label = StringUtils.strip(node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (n:`{label}`) RETURN n
            $$) AS (n agtype)""";
        Map<String, Object> params = Map.of(
                "label", PGGraphStorage.encodeGraphLabel(entity_name_label),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        var record = query(wrappedQuery, true);
        if (record != null && !record.isEmpty()) {
            var node = record.get(0);
            String node_dict = (String) node.get("n");
            log.debug("query: {}, result: {}", wrappedQuery, node_dict);
            var parsedNode = parseAgType(node_dict, "node");
            var result = new HashMap<String, Object>();
            for (var entry : parsedNode.entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if (k.equals("label")) {
                    result.put("label", decodeGraphLabel((String) v));
                } else if (k.equals("properties")) {
                    var properties = (Map) v;
                    result.putAll(properties);
                } else {
                    result.put(k, v);
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Find all edges between nodes of two given labels
     * @param source_node_id (str): Label of the source nodes
     * @param target_node_id (str): Label of the target nodes
     * @return list: List of all relationships/edges found
     */
    @Override
    public Map<String, Object> getEdge(String source_node_id, String target_node_id) {
        var entity_name_label_source = StringUtils.strip(source_node_id, "\"");
        var entity_name_label_target = StringUtils.strip(target_node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (a:`{src_label}`)-[r]->(b:`{tgt_label}`)
              RETURN properties(r) as edge_properties
              LIMIT 1
            $$) AS (edge_properties agtype)""";
        Map<String, Object> params = Map.of(
                "src_label", PGGraphStorage.encodeGraphLabel(entity_name_label_source),
                "tgt_label", PGGraphStorage.encodeGraphLabel(entity_name_label_target),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        var records = query(wrappedQuery, true);
        var record = records.get(0);
        var result = (String) record.get("edge_properties");
        log.debug("GetEdge query:{}:result:{}", wrappedQuery, result);
        return parseAgType(result, "edge");
    }

    private static final TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {};
    private Map<String, Object> parseAgType(String str, String type) {
        try {
            return objectMapper.readValue(str, mapTypeRef) ;
        } catch (JsonProcessingException e) {
            throw new PGGraphQueryException("Failed to parse %s".formatted(type), str, e.getMessage());
        }
    }

    /**
     * Retrieves all edges (relationships) for a particular node identified by its label.
     * @param source_node_id
     * @return List of dictionaries containing edge information
     */
    @Override
    public List<Pair<String, String>> getNodeEdges(String source_node_id) {
        var node_label = StringUtils.strip(source_node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (n:`{label}`)
              OPTIONAL MATCH (n)-[r]-(connected)
              RETURN n, r, connected
            $$) AS (n agtype, r agtype, connected agtype)""";
        Map<String, Object> params = Map.of(
                "label", PGGraphStorage.encodeGraphLabel(node_label),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        var results = query(wrappedQuery, true);
        var edges = new LinkedList<Pair<String, String>>();
        for(var record : results) {
            String sourceNodeStr = (String) record.get("n");
            Map<String, Object> sourceNode = null;
            if (sourceNodeStr != null) {
                sourceNode = parseAgType(sourceNodeStr, "node");
            }

            String connectedNodeStr = (String) record.get("connected");
            Map<String, Object> connectedNode = null;
            if (connectedNodeStr != null) {
                connectedNode = parseAgType(connectedNodeStr, "node");
            }

            String source_label = "";
            if (sourceNode != null) {
                source_label = (String) sourceNode.get("label");
            }

            String target_label = "";
            if (connectedNode != null) {
                target_label = (String) connectedNode.get("label");
            }

            if (StringUtils.isNotBlank(source_label)) {
                edges.add(Pair.of(source_label, target_label));
            }
        }
        return edges;
    }

    @Override
    public void upsertNode(String node_id, Map<String, String> node_data) {
        var label = StringUtils.strip(node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MERGE (n:`{label}`)
                SET n += {properties}
            $$) AS (n agtype)""";
        Map<String, Object> params = Map.of(
                "label", PGGraphStorage.encodeGraphLabel(label),
                "properties", formatProperties(node_data),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);

        try {
            query(wrappedQuery, false);
            log.debug("Upserted node with label '{}' and properties: {}", label, node_data);
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert node", wrappedQuery, e.getMessage());
        }
    }

    @Override
    public void upsertEdge(String source_node_id, String target_node_id, Map<String, String> edge_data) {
        var source_node_label = StringUtils.strip(source_node_id, "\"");
        var target_node_label = StringUtils.strip(target_node_id, "\"");

        var query = """
            SELECT * FROM ag_catalog.cypher('{graph_name}', $$
              MATCH (source:`{src_label}`)
                WITH source
                MATCH (target:`{tgt_label}`)
                MERGE (source)-[r:DIRECTED]->(target)
                SET r += {properties}
                RETURN r
            $$) AS (n agtype)""";
        Map<String, Object> params = Map.of(
                "src_label", PGGraphStorage.encodeGraphLabel(source_node_label),
                "tgt_label", PGGraphStorage.encodeGraphLabel(target_node_label),
                "properties", formatProperties(edge_data),
                "graph_name", graphName
        );
        var wrappedQuery = pythonTemplateFormat(query, params);
        try {
            query(wrappedQuery, false);
            log.debug("Upserted edge from '{}' to '{}' with properties: {}", source_node_label, target_node_label, edge_data);
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert edge", wrappedQuery, e.getMessage());
        }
    }

    @Override
    public void deleteNode(String node_id) {

    }

    @Override
    public Pair<float[], List<String>> embedNodes(String algorithm) {
        return null;
    }

    @Override
    public void indexDoneCallback() {
        log.info("index done for PGGraphStorage: {}", this.graphName);
    }

    public static List<Map<String, Object>> mapRows(ResultSet rs) throws SQLException {
        var result = new LinkedList<Map<String, Object>>();
        while (rs.next()) {
            var map = new HashMap<String, Object>();
            var metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                var columnName = metaData.getColumnName(i);
                var columnType = metaData.getColumnTypeName(i);
                if (columnType.equals("agtype")) {
                    map.put(columnName, rs.getString(i));
                } else {
                    map.put(columnName, rs.getObject(columnName));
                }
            }
            result.add(map);
        }
        return result;
    }

    public static class PGGraphQueryException extends RuntimeException {
        private final String wrapped;
        private final String detail;
        public PGGraphQueryException(String message, String wrapped, String detail) {
            super(message);
            this.detail = detail;
            this.wrapped = wrapped;
        }

        public String toString() {
            return """
                PGGraphQueryException: %s
                Wrapped: %s
                Detail: %s
                """.formatted(getMessage(), wrapped, detail);
        }
    }

    private String objectToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new PGGraphQueryException("Failed to convert object to JSON", obj.toString(), e.getMessage());
        }
    }

    /**
     * Convert a dictionary of properties to a string representation that can be used in a cypher query insert/merge statement.
     * @param properties
     * @return
     */
    private String formatProperties(Map<String, ?> properties) {
        var props = new LinkedList<String>();
        // wrap property key in backticks to escape

        for (var entry : properties.entrySet()) {
            var k = entry.getKey();
            var v = entry.getValue();
            var prop = "`%s`: %s".formatted(k, objectToJson(v));
            props.add(prop);
        }

        return "{%s}".formatted(String.join(", ", props));
    }
}
