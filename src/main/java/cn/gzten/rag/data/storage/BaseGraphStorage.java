package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.storage.pojo.RagGraphEdge;
import cn.gzten.rag.data.storage.pojo.RagGraphNode;
import org.springframework.data.util.Pair;

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

    Pair<float[], List<String>> embedNodes(String algorithm);
}
