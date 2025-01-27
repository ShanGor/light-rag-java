package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.pojo.LlmCache;
import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.LlmCacheStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static cn.gzten.rag.util.LightRagUtils.isEmptyCollection;

@Service("llmCacheStorage")
@Slf4j
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGKVForLlmCacheStorage implements LlmCacheStorage {
    private final LlmCacheRepository llmCacheRepo;
    private int maxBatchSize;
    private String workspace;

    public PGKVForLlmCacheStorage(@Value("${rag.storage.embedding-batch-num}") int maxBatchSize,
                                  @Value("${rag.storage.workspace}") String workspace,
                                  LlmCacheRepository llmCacheRepo) {
        this.maxBatchSize = maxBatchSize;
        this.llmCacheRepo = llmCacheRepo;
        this.workspace = workspace;
    }

    @Override
    public Mono<LlmCache> getById(String id) {
        return Mono.empty();
    }

    /**
     * For llm_response_cache
     * @param mode
     * @param id
     * @return
     */
    @Override
    public Mono<LlmCache> getByModeAndId(String mode, String id) {
        return llmCacheRepo.findByWorkspaceAndModeAndId(workspace, mode, id)
                .map(o -> (LlmCache)o );
    }

    @Override
    public Flux<String> allKeys() {
        return Flux.empty();
    }

    @Override
    public Flux<LlmCache> getByIds(List<String> ids) {
        return Flux.empty();
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Mono<Set<String>> filterKeys(List<String> data) {
        if (isEmptyCollection(data)) return Mono.empty();

        return llmCacheRepo.findByWorkspaceAndIds(this.workspace, data).collectList().map(result -> {
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
    public void upsert(LlmCache data) {
        llmCacheRepo.upsert(this.workspace,
                data.getId(),
                data.getOriginalPrompt(),
                data.getReturnValue(),
                data.getMode());
    }

    @Override
    public void drop() {

    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVStorage {}", this.workspace);
    }
}
