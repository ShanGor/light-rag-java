package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.storage.pojo.RagEntity;
import cn.gzten.rag.data.storage.pojo.RagGraphEdge;
import cn.gzten.rag.data.storage.pojo.RagGraphNode;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import cn.gzten.rag.util.LightRagUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;

public interface BaseGraphStorage extends BaseStorage{

    boolean hasNode(String nodeId) ;

    boolean hasEdge(String source_node_id, String target_node_id);

    int nodeDegree(String node_id);

    int edgeDegree(String src_id, String tgt_id);

    RagGraphNode getNode(String node_id);

    RagGraphEdge getEdge(
            String source_node_id, String target_node_id
    );

    Map<String, Object> getNodeAsMap(String node_id);

    Map<String, Object> getEdgeAsMap(
            String source_node_id, String target_node_id
    );

    List<NullablePair<String, String>> getNodeEdges(
            String source_node_id
    ) ;

    void upsertNode(String node_id, Map<String, String> node_data);

    void upsertNode(RagGraphNode node);

    void upsertEdge(
            String source_node_id, String target_node_id, Map<String, String> edge_data
    );

    void upsertEdge(String source_node_id, String target_node_id, RagGraphEdge edge);

    void deleteNode(String node_id);

    NullablePair<float[], List<String>> embedNodes(String algorithm);

    public static final String CACHE_NAME_GRAPH_NODE = "graph_node";
    public static final String CACHE_NAME_GRAPH_NODE_DEGREE = "graph_node_degree";
    public static final String CACHE_NAME_GRAPH_EDGE = "graph_edge";
    public static final String CACHE_NAME_GRAPH_EDGE_DEGREE = "graph_edge_degree";

    static void tryToCacheNodeInfo(CacheManager cacheManager, RagEntity entity, String entityName) {
        if (cacheManager == null) return;
        if (StringUtils.isNotBlank(entity.getGraphProperties())) {
            cacheManager.getCache(CACHE_NAME_GRAPH_NODE).put(entityName, LightRagUtils.jsonToObject(entity.getGraphProperties(), RagGraphNode.class));
            var degree = entity.getGraphNodeDegree();
            if (degree != null) {
                cacheManager.getCache(CACHE_NAME_GRAPH_NODE_DEGREE).put(entityName, degree);
            }
        }
    }

    static void tryToCacheEdgeInfo(CacheManager cacheManager, RagRelation result, String srcId, String tgtId) {
        if (cacheManager == null) return;
        if (StringUtils.isNotBlank(result.getGraphProperties())) {
            var edge = LightRagUtils.jsonToObject(result.getGraphProperties(), RagGraphEdge.class);
            cacheManager.getCache(CACHE_NAME_GRAPH_EDGE).put(srcId + "," + tgtId, edge);
            var startNode = LightRagUtils.jsonToObject(result.getGraphStartNode(), RagGraphNode.class);
            cacheManager.getCache(CACHE_NAME_GRAPH_NODE).put(srcId, startNode);
            var endNode = LightRagUtils.jsonToObject(result.getGraphEndNode(), RagGraphNode.class);
            cacheManager.getCache(CACHE_NAME_GRAPH_NODE).put(tgtId, endNode);
            var edgeDegree = result.getGraphEdgeDegree();
            var startNodeDegree = result.getGraphStartNodeDegree();
            var endNodeDegree = result.getGraphEndNodeDegree();
            cacheManager.getCache(CACHE_NAME_GRAPH_EDGE_DEGREE).put(srcId + "," + tgtId, edgeDegree);
            cacheManager.getCache(CACHE_NAME_GRAPH_NODE_DEGREE).put(srcId, startNodeDegree);
            cacheManager.getCache(CACHE_NAME_GRAPH_NODE_DEGREE).put(tgtId, endNodeDegree);
        }
    }
}
