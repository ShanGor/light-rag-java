package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.DocStatusEntity;
import cn.gzten.rag.data.postgres.dao.DocStatusRepository;
import cn.gzten.rag.data.storage.DocProcessingStatus;
import cn.gzten.rag.data.storage.DocStatus;
import cn.gzten.rag.data.storage.DocStatusStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("docStatusStorage")
@Slf4j
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
@RequiredArgsConstructor
public class PGDocStatusStorage implements DocStatusStorage<DocStatusEntity> {
    private final DocStatusRepository docStatusRepo;
    @Value("${rag.storage.workspace}")
    private String workspace;
    @Override
    public Map<String, Integer> getStatusCounts() {
        var result = docStatusRepo.getDocStatusCounts(this.workspace);
        var resMap = new HashMap<String, Integer>();
        for (var item : result) {
            resMap.put(item.getStatus(), item.getCount());
        }
        return resMap;
    }

    public Map<String, DocProcessingStatus> getDocsByStatus(DocStatus status) {
        var result = docStatusRepo.findByWorkspaceAndStatus(this.workspace, status.name());
        var resMap = new HashMap<String, DocProcessingStatus>();
        for (var item : result) {
            var o = DocProcessingStatus.builder()
                    .contentSummary(item.getContentSummary())
                    .contentLength(item.getContentLength())
                    .createdAt(item.getCreatedAt())
                    .updatedAt(item.getUpdatedAt())
                    .status(item.getStatus())
                    .chunksCount(item.getChunksCount())
                    .build();
            resMap.put(item.getCId().getId(), o);
        }

        return resMap;
    }

    @Override
    public Map<String, DocProcessingStatus> getFailedDocs() {
        return getDocsByStatus(DocStatus.FAILED);
    }

    @Override
    public Map<String, DocProcessingStatus> getPendingDocs() {
        return getDocsByStatus(DocStatus.PENDING);
    }

    @Override
    public List<String> allKeys() {
        return null;
    }

    @Override
    public Optional<DocStatusEntity> getById(String id) {
        return Optional.empty();
    }


    @Override
    public List<DocStatusEntity> getByIds(List<String> ids) {
        return null;
    }

    @Override
    public Set<String> filterKeys(List<String> data) {
        var existingSet = new HashSet<>(docStatusRepo.findByWorkspaceAndIds(this.workspace, data));
        if (existingSet.isEmpty()) {
            existingSet.addAll(data);
            return existingSet;
        } else {
            return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
        }
    }

    @Override
    public void upsert(DocStatusEntity data) {
        if (data == null) {
            return;
        }
        docStatusRepo.upsert(this.workspace,
                data.getId(),
                data.getContentSummary(),
                data.getContentLength(),
                data.getChunksCount(),
                data.getStatus().getStatus());
    }

    @Override
    public void drop() {

    }

    @Override
    public void indexDoneCallback() {
        log.info("index done for PGDocStatusStorage");
    }
}
