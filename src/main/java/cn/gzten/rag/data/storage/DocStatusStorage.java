package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.DocStatusStore;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DocStatusStorage <T extends DocStatusStore> extends BaseKVStorage <T> {
    Mono<Map<String, Integer>> getStatusCounts();

    Mono<Map<String, DocProcessingStatus>> getFailedDocs();

    Mono<Map<String, DocProcessingStatus>> getPendingDocs();
}
