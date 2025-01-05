package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseKVStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGKVForLlmCacheStorage implements BaseKVStorage {
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
    public Object getById(String id) {
        return null;
    }

    /**
     * For llm_response_cache
     * @param mode
     * @param id
     * @return
     */
    @Override
    public Object getByModeAndId(String mode, String id) {
        var cId = new LlmCacheEntity.Id(this.workspace, mode, id);
        return llmCacheRepo.findById(cId).orElse(null);
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public List<Object> getByIds(List<String> ids) {
        return null;
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Set<String> filterKeys(List<String> data) {
        if (data == null || data.isEmpty()) return Set.of();

        var existingSet = new HashSet<>(llmCacheRepo.findByWorkspaceAndIds(this.workspace, data));
        if (existingSet.isEmpty()) {
            existingSet.addAll(data);
            return existingSet;
        } else {
            return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
        }
    }

    @Override
    public void upsert(Map<String, Object> data) {
        llmCacheRepo.upsert(this.workspace,
                (String) data.get("id"),
                (String) data.get("original_prompt"),
                (String) data.get("return_value"),
                (String) data.get("mode"));
    }

    @Override
    public void drop() {

    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVStorage {}", this.workspace);
    }
}
