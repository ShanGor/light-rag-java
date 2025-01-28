package cn.gzten.rag.data.storage;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface BaseVectorStorage<C, R> extends BaseStorage {
    Set<String> getMetaFields();
    void setMetaFields(Set<String> metaFields);

    Mono<Void> upsert(C data);

    Flux<R> query(String query, int topK);
}
