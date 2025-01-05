package cn.gzten.rag.data.storage;

import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;

public interface BaseGraphStorage extends BaseStorage{

    boolean hasNode(String nodeId) ;

    boolean hasEdge(String source_node_id, String target_node_id);

    int nodeDegree(String node_id);

    int edgeDegree(String src_id, String tgt_id);

    Map<String, Object> getNode(String node_id);

    Map<String, Object> getEdge(
            String source_node_id, String target_node_id
    );

    List<Pair<String, String>> getNodeEdges(
            String source_node_id
    ) ;

    void upsertNode(String node_id, Map<String, String> node_data);

    void upsertEdge(
            String source_node_id, String target_node_id, Map<String, String> edge_data
    );

    void deleteNode(String node_id);

    Pair<float[], List<String>> embedNodes(String algorithm);
}
