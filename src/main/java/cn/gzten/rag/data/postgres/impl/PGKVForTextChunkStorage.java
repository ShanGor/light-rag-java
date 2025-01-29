package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseTextChunkStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
    @Cacheable(value = "text_chunk", key = "#id")
    public Optional<DocChunkEntity> getById(String id) {
        return docChunkRepo.findByWorkspaceAndId(this.workspace, id);
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public List<DocChunkEntity> getByIds(List<String> ids) {
        return docChunkRepo.findAllByWorkspaceAndIds(this.workspace, ids);
    }

    /**
     * Filter out duplicated content.
     * @param data
     * @return
     */
    @Override
    public Set<String> filterKeys(List<String> data) {
        if (isEmptyCollection(data)) return Set.of();

        var existingSet = new HashSet<>(docChunkRepo.findByWorkspaceAndIds(this.workspace, data));
        if (existingSet.isEmpty()) {
            existingSet.addAll(data);
            return existingSet;
        } else {
            return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
        }
    }

    @Override
    public void upsert(DocChunkEntity data) {
        // No need to do it here, the PGVectorStorage will handle it
    }

    @Override
    public void drop() {

    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVForTextChunkStorage {}", this.workspace);
    }
}
