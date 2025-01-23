package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseTextChunkStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public Optional<DocChunkEntity> getById(String id) {
        var cId = new WorkspaceId(this.workspace, id);
        return docChunkRepo.findById(cId);
    }

    /**
     * For llm_response_cache
     * @param mode
     * @param id
     * @return
     */
    @Override
    public Optional<DocChunkEntity> getByModeAndId(String mode, String id) {
        return null;
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public List<DocChunkEntity> getByIds(List<String> ids) {
        var idList = ids.stream().map(id -> new WorkspaceId(this.workspace, id)).toList();
        var result = docChunkRepo.findAllById(idList);
        var resultList = new LinkedList<DocChunkEntity>();
        result.forEach(resultList::add);
        return resultList;
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
    public void upsert(Map<String, Object> data) {
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
