package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseKVStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static cn.gzten.rag.util.LightRagUtils.isEmptyCollection;

@Service("docFullStorage")
@Slf4j
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGKVForDocFullStorage implements BaseKVStorage<DocFullEntity> {
    private final DocFullRepository docFullRepo;
    private int maxBatchSize;
    private String workspace;

    public PGKVForDocFullStorage(@Value("${rag.storage.embedding-batch-num}") int maxBatchSize,
                                 @Value("${rag.storage.workspace}") String workspace,
                                 DocFullRepository docFullRepo) {
        this.maxBatchSize = maxBatchSize;
        this.docFullRepo = docFullRepo;
        this.workspace = workspace;
    }

    @Override
    public Mono<DocFullEntity> getById(String id) {
        return docFullRepo.findByWorkspaceAndId(this.workspace, id);
    }

    @Override
    public Flux<String> allKeys() {
        return Flux.empty();
    }

    @Override
    public Flux<DocFullEntity> getByIds(List<String> ids) {
        var idList = ids.stream().map(id -> new WorkspaceId(this.workspace, id)).toList();
        return docFullRepo.findByIds(this.workspace, ids);
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Mono<Set<String>> filterKeys(List<String> data) {
        if (isEmptyCollection(data)) return Mono.empty();

        return docFullRepo.findByWorkspaceAndIds(this.workspace, data).collectList().map(s -> {
            Set<String> existingSet = new HashSet<>(s);
            if (existingSet.isEmpty()) {
                existingSet.addAll(data);
                return existingSet;
            } else {
                return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
            }
        });

    }

    @Override
    public Mono<Void> upsert(DocFullEntity data) {
        return docFullRepo.upsert(this.workspace, data.getId(), data.getContent());
    }

    @Override
    public Mono<Void> drop() {
        return Mono.empty();
    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVForDocFullStorage {}", this.workspace);
    }
}
