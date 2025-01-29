package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.pojo.LlmCache;
import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.LlmCacheStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
    public Optional<LlmCache> getById(String id) {
        return Optional.empty();
    }

    /**
     * For llm_response_cache
     * @param mode
     * @param id
     * @return
     */
    @Override
    public Optional<LlmCache> getByModeAndId(String mode, String id) {
        return (Optional)llmCacheRepo.findByWorkspaceAndModeAndId(this.workspace, mode, id);
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public List<LlmCache> getByIds(List<String> ids) {
        return null;
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Set<String> filterKeys(List<String> data) {
        if (isEmptyCollection(data)) return Set.of();

        var existingSet = new HashSet<>(llmCacheRepo.findByWorkspaceAndIds(this.workspace, data));
        if (existingSet.isEmpty()) {
            existingSet.addAll(data);
            return existingSet;
        } else {
            return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
        }
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
