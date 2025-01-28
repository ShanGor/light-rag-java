package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseTextChunkStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static cn.gzten.rag.util.LightRagUtils.isEmptyCollection;

@Service("textChunkStorage")
@Slf4j
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGKVForTextChunkStorage implements BaseTextChunkStorage<DocChunkEntity> {
    private final DocChunkRepository docChunkRepo;
    private int maxBatchSize;
    private String workspace;

    public PGKVForTextChunkStorage(DocChunkRepository docChunkRepo,
                                   @Value("${rag.storage.embedding-batch-num}") int maxBatchSize,
                                   @Value("${rag.storage.workspace}") String workspace) {
        this.maxBatchSize = maxBatchSize;
        this.docChunkRepo = docChunkRepo;
        this.workspace = workspace;
    }

    @Override
    public Mono<DocChunkEntity> getById(String id) {
        return docChunkRepo.findByWorkspaceAndId(this.workspace, id);
    }

    @Override
    public Flux<String> allKeys() {
        return Flux.empty();
    }

    @Override
    public Flux<DocChunkEntity> getByIds(List<String> ids) {
        return docChunkRepo.findByIds(workspace, ids);
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Mono<Set<String>> filterKeys(List<String> data) {
        if (isEmptyCollection(data)) return Mono.empty();

        return docChunkRepo.findByWorkspaceAndIds(this.workspace, data).collectList().map(result -> {
            var existingSet = new HashSet<>(result);
            if (existingSet.isEmpty()) {
                existingSet.addAll(data);
                return existingSet;
            } else {
                return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
            }
        });
    }

    @Override
    public Mono<Void> upsert(DocChunkEntity data) {
        // No need to do it here, the PGVectorStorage will handle it
        return Mono.empty();
    }

    @Override
    public Mono<Void> drop() {
        return Mono.empty();
    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVForTextChunkStorage {}", this.workspace);
    }
}
