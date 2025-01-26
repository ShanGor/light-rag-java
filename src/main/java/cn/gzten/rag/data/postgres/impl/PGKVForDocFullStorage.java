package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.*;
import cn.gzten.rag.data.storage.BaseKVStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
    public Optional<DocFullEntity> getById(String id) {
        var cId = new WorkspaceId(this.workspace, id);
        return docFullRepo.findById(cId);
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public List<DocFullEntity> getByIds(List<String> ids) {
        var idList = ids.stream().map(id -> new WorkspaceId(this.workspace, id)).toList();
        var result = docFullRepo.findAllById(idList);
        var resultList = new LinkedList<DocFullEntity>();
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

        Set<String> existingSet = new HashSet<>(docFullRepo.findByWorkspaceAndIds(this.workspace, data));
        if (existingSet.isEmpty()) {
            existingSet.addAll(data);
            return existingSet;
        } else {
            return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
        }
    }

    @Override
    public void upsert(DocFullEntity data) {
        docFullRepo.upsert(this.workspace, data.getCId().getId(), data.getContent());
    }

    @Override
    public void drop() {

    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGKVForDocFullStorage {}", this.workspace);
    }
}
