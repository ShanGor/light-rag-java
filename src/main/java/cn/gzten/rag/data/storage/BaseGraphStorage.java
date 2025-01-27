package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.storage.pojo.RagGraphEdge;
import cn.gzten.rag.data.storage.pojo.RagGraphNode;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface BaseGraphStorage extends BaseStorage{

    Mono<Boolean> hasNode(String nodeId) ;

    Mono<Boolean> hasEdge(String source_node_id, String target_node_id);

    Mono<Integer> nodeDegree(String node_id);

    Mono<Integer> edgeDegree(String src_id, String tgt_id);

    Mono<RagGraphNode> getNode(String node_id);

    Mono<RagGraphEdge> getEdge(
            String source_node_id, String target_node_id
    );

    Mono<Map<String, Object>> getNodeAsMap(String node_id);

    Mono<Map<String, Object>> getEdgeAsMap(
            String source_node_id, String target_node_id
    );

    Flux<NullablePair<String, String>> getNodeEdges(
            String source_node_id
    ) ;

    Mono<Void> upsertNode(String node_id, Map<String, String> node_data);

    Mono<Void> upsertNode(RagGraphNode node);

    Mono<Void> upsertEdge(
            String source_node_id, String target_node_id, Map<String, String> edge_data
    );

    Mono<Void> upsertEdge(String source_node_id, String target_node_id, RagGraphEdge edge);

    Mono<Void> deleteNode(String node_id);

    Pair<float[], List<String>> embedNodes(String algorithm);
}
