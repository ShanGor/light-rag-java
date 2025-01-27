package cn.gzten.rag.data.storage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public interface BaseKVStorage <T> extends BaseStorage{

    Flux<String> allKeys();
    Mono<T> getById(String id);

    Flux<T> getByIds(List<String> ids);
    Mono<Set<String>> filterKeys(List<String> data);
    void upsert(T data);
    void drop();

}
