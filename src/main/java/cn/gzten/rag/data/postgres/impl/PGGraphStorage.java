package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.storage.BaseGraphStorage;
import cn.gzten.rag.data.storage.pojo.RagGraphEdge;
import cn.gzten.rag.data.storage.pojo.RagGraphNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.util.Pair;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service("graphStorage")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGGraphStorage implements BaseGraphStorage {
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    @Qualifier("noNamedExpanderDbClient")
    private DatabaseClient databaseClient;

    @Value("${rag.storage.graph.name}")
    private String graphName;

    public Flux<Map<String, Object>> query(String query) {
        try {
            return databaseClient.sql(query).fetch().all();
        } catch (Exception e) {
            e.printStackTrace();
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
    public Mono<Boolean> hasNode(String nodeId) {
        var entity_name_label = StringUtils.strip(nodeId, "\"");

        var query = """
            SELECT * FROM ag_catalog.cypher('%s', $$
              MATCH (n:Entity {node_id: "%s"})
              RETURN count(n) > 0 AS node_exists
            $$) AS (node_exists bool)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(entity_name_label));

        return this.query(query).single().map(single_result -> {
            var result = single_result.get("node_exists");
            log.debug(
                    "query node:{}, result:{}",
                    query,
                    result
            );

            return (Boolean) result;
        });
    }

    @Override
    public Mono<Boolean> hasEdge(String source_node_id, String target_node_id) {
        var entity_name_label_source = StringUtils.strip(source_node_id, "\"");
        var entity_name_label_target = StringUtils.strip(target_node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('%s', $$
              MATCH (a:Entity {node_id: "%s"})-[r]-(b:Entity {node_id: "%s"})
              RETURN COUNT(r) > 0 AS edge_exists
            $$) AS (edge_exists bool)""".formatted(graphName,
                PGGraphStorage.encodeGraphLabel(entity_name_label_source),
                PGGraphStorage.encodeGraphLabel(entity_name_label_target)
        );

        return query(query).single().map(single_result -> {
            var result = single_result.get("edge_exists");
            log.debug("query edge:{}, result:{}", query, result);
            return (Boolean) result;
        });

    }

    @Override
    @Cacheable(value = "graph_degree", key = "#nodeId")
    public Mono<Integer> nodeDegree(String nodeId) {
        var entity_name_label = StringUtils.strip(nodeId, "\"");

        var query = """
           SELECT * FROM ag_catalog.cypher('%s', $$
             MATCH (n:Entity {node_id: "%s"})-[]->(x)
             RETURN count(x) AS total_edge_count
           $$) AS (total_edge_count integer)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(entity_name_label));

        return query(query).collectList().map(records -> {
            if (!records.isEmpty()) {
                var record = records.get(0);
                var edge_count = (Integer) record.get("total_edge_count");
                log.debug("NodeDegree query:{}:result:{}", query, edge_count);
                return edge_count;
            }
            return 0;
        });

    }

    @Override
    @Cacheable(value = "graph_degree", key = "#srcId + ',' + #tgtId")
    public Mono<Integer> edgeDegree(String srcId, String tgtId) {
        var src_degree = nodeDegree(srcId);
        var trg_degree = nodeDegree(tgtId);

        return src_degree.zipWith(trg_degree).map(pair -> pair.getT1() + pair.getT2());
    }

    @Override
    @Cacheable(value = "graph_entity", key = "#node_id")
    public Mono<RagGraphNode> getNode(String node_id) {
        return retrieveNode(node_id).map(parsedNode -> {
            var result = new RagGraphNode();
            var id = (long) parsedNode.get("id");
            var properties = (Map) parsedNode.get("properties");
            var nodeId = (String) properties.get("node_id");
            result.setId(id);
            result.setNodeId(nodeId);
            result.setLabel(decodeGraphLabel(nodeId));
            result.setSourceId((String) properties.get("source_id"));
            result.setEntityType((String) properties.get("entity_type"));
            result.setDescription((String) properties.get("description"));

            return result;
        });
    }

    /**
     * Find all edges between nodes of two given labels
     * @param srcNodeId (str): Label of the source nodes
     * @param targetNodeId (str): Label of the target nodes
     * @return list: List of all relationships/edges found
     */
    @Override
    @Cacheable(value = "graph_relation", key = "#srcNodeId + ',' + #targetNodeId")
    public Mono<RagGraphEdge> getEdge(String srcNodeId, String targetNodeId) {
        return getEdgeAsMap(srcNodeId, targetNodeId).map(properties -> {
            var edge = new RagGraphEdge();
            edge.setWeight((double) properties.get("weight"));
            edge.setKeywords((String) properties.get("keywords"));
            edge.setSourceId((String) properties.get("source_id"));
            edge.setDescription((String) properties.get("description"));
            return edge;
        });
    }

    public Mono<Map<String, Object>> retrieveNode(String node_id) {
        var entity_name_label = StringUtils.strip(node_id, "\"");
        var query = """
            SELECT * FROM ag_catalog.cypher('%s', $$
              MATCH (n:Entity {node_id: "%s"})
              RETURN n
            $$) AS (n agtype)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(entity_name_label));

        return query(query).collectList().single().map(record -> {
            var node = record.get(0);
            String node_dict = (String) node.get("n");
            log.debug("query: {}, result: {}", query, node_dict);
            return parseAgType(node_dict, "node");
        });

    }

    @Override
    public Mono<Map<String, Object>> getNodeAsMap(String node_id) {
        return retrieveNode(node_id).map(parsedNode -> {
            var result = new HashMap<String, Object>();
            for (var entry : parsedNode.entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if (k.equals("properties")) {
                    var properties = (Map) v;
                    result.putAll(properties);
                    result.put("label", decodeGraphLabel((String) properties.get("node_id")));
                } else {
                    result.put(k, v);
                }
            }
            return result;
        });
    }

    @Override
    public Mono<Map<String, Object>> getEdgeAsMap(String source_node_id, String target_node_id) {
        var entity_name_label_source = StringUtils.strip(source_node_id, "\"");
        var entity_name_label_target = StringUtils.strip(target_node_id, "\"");
        var query = """
            SELECT * FROM cypher('%s', $$
              MATCH (a:Entity {node_id: "%s"})-[r]->(b:Entity {node_id: "%s"})
              RETURN properties(r) as edge_properties
              LIMIT 1
            $$) AS (edge_properties agtype)""".formatted(graphName,
                PGGraphStorage.encodeGraphLabel(entity_name_label_source),
                PGGraphStorage.encodeGraphLabel(entity_name_label_target)
        );
        return query(query).single().map(record -> {
            var result = (String) record.get("edge_properties");
            log.debug("GetEdge query:{}:result:{}", query, result);
            return parseAgType(result, "edge");
        });
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
    public Flux<NullablePair<String, String>> getNodeEdges(String source_node_id) {
        var node_label = StringUtils.strip(source_node_id, "\"");
        var query = """
            SELECT * FROM cypher('%s', $$
              MATCH (n:Entity {node_id: "%s"})
              OPTIONAL MATCH (n)-[r]-(connected)
              RETURN n, r, connected
            $$) AS (n agtype, r agtype, connected agtype)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(node_label));

        return query(query).mapNotNull(record -> {
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
                source_label = (String) sourceNode.get("node_id");
            }

            String target_label = "";
            if (connectedNode != null) {
                target_label = (String) connectedNode.get("node_id");
            }

            if (StringUtils.isNotBlank(source_label)) {
                return new NullablePair<>(decodeGraphLabel(source_label), decodeGraphLabel(target_label));
            } else {
                return null;
            }
        });
    }

    @Override
    public Mono<Void> upsertNode(String node_id, Map<String, String> node_data) {
        var label = StringUtils.strip(node_id, "\"");
        var query = """
            SELECT * FROM cypher('%s', $$
              MERGE (n:Entity {node_id: "%s"})
              SET n += %s
              RETURN n
            $$) AS (n agtype)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(label), formatProperties(node_data));

        try {
            return query(query).doFinally(v -> {
                log.debug("Upserted node with label '{}' and properties: {}", label, node_data);
            }).then();

        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert node", query, e.getMessage());
        }
    }

    @Override
    public Mono<Void> upsertNode(RagGraphNode node) {
        var label = StringUtils.strip(node.getLabel(), "\"");
        var properties = node.toGraphProperties();
        var query = """
            SELECT * FROM cypher('%s', $$
              MERGE (n:Entity {node_id: "%s"})
              SET n += %s
              RETURN n
            $$) AS (n agtype)""".formatted(graphName, PGGraphStorage.encodeGraphLabel(label), properties);

        try {
            return query(query).doFinally(v -> {
                log.debug("Upserted node with label '{}' and properties: {}", label, properties);
            }).then();
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert node", query, e.getMessage());
        }
    }

    @Override
    public Mono<Void> upsertEdge(String source_node_id, String target_node_id, Map<String, String> edge_data) {
        var source_node_label = StringUtils.strip(source_node_id, "\"");
        var target_node_label = StringUtils.strip(target_node_id, "\"");

        var query = """
            SELECT * FROM cypher('%s', $$
              MATCH (source:Entity {node_id: "%s"})
              WITH source
              MATCH (target:Entity {node_id: "%s"})
              MERGE (source)-[r:DIRECTED]->(target)
              SET r += %s
              RETURN r
            $$) AS (n agtype)""".formatted(graphName,
                PGGraphStorage.encodeGraphLabel(source_node_label),
                PGGraphStorage.encodeGraphLabel(target_node_label),
                formatProperties(edge_data)
        );

        try {
            return query(query).doFinally(v -> {
                log.debug("Upserted edge from '{}' to '{}' with properties: {}", source_node_label, target_node_label, edge_data);
            }).then();
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert edge", query, e.getMessage());
        }
    }

    @Override
    public Mono<Void> upsertEdge(String source_node_id, String target_node_id, RagGraphEdge edge) {
        var source_node_label = StringUtils.strip(source_node_id, "\"");
        var target_node_label = StringUtils.strip(target_node_id, "\"");

        var query = """
            SELECT * FROM cypher('%s', $$
              MATCH (source:Entity {node_id: "%s"})
              WITH source
              MATCH (target:Entity {node_id: "%s"})
              MERGE (source)-[r:DIRECTED]->(target)
              SET r += %s
              RETURN r
            $$) AS (n agtype)""".formatted(graphName,
                PGGraphStorage.encodeGraphLabel(source_node_label),
                PGGraphStorage.encodeGraphLabel(target_node_label),
                edge.toGraphProperties()
        );

        try {
            return query(query).doFinally(v -> {
                log.debug("Upserted edge from '{}' to '{}' with properties: {}", source_node_label, target_node_label, edge);
            }).then();
        } catch (Exception e) {
            throw new PGGraphQueryException("Failed to upsert edge", query, e.getMessage());
        }
    }

    @Override
    public Mono<Void> deleteNode(String node_id) {
        return Mono.empty();
    }

    @Override
    public Pair<float[], List<String>> embedNodes(String algorithm) {
        return null;
    }

    @Override
    public void indexDoneCallback() {
        log.info("index done for PGGraphStorage: {}", this.graphName);
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
