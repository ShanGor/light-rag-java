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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<Map<String, Integer>> getStatusCounts() {
        return docStatusRepo.getDocStatusCounts(this.workspace).collectList().map(result -> {
            var resMap = new HashMap<String, Integer>();
            for (var item : result) {
                resMap.put(item.getStatus(), item.getCount());
            }
            return resMap;
        });
    }

    public Mono<Map<String, DocProcessingStatus>> getDocsByStatus(DocStatus status) {
        return docStatusRepo.findByWorkspaceAndStatus(this.workspace, status.name()).collectList().map(result -> {
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
                resMap.put(item.getId(), o);
            }

            return resMap;
        });

    }

    @Override
    public Mono<Map<String, DocProcessingStatus>> getFailedDocs() {
        return getDocsByStatus(DocStatus.FAILED);
    }

    @Override
    public Mono<Map<String, DocProcessingStatus>> getPendingDocs() {
        return getDocsByStatus(DocStatus.PENDING);
    }

    @Override
    public Flux<String> allKeys() {
        return Flux.empty();
    }

    @Override
    public Mono<DocStatusEntity> getById(String id) {
        return Mono.empty();
    }


    @Override
    public Flux<DocStatusEntity> getByIds(List<String> ids) {
        return Flux.empty();
    }

    @Override
    public Mono<Set<String>> filterKeys(List<String> data) {
        return docStatusRepo.findByWorkspaceAndIds(this.workspace, data).collectList().map(list -> {
            var existingSet = new HashSet<>(list);
            if (existingSet.isEmpty()) {
                existingSet.addAll(data);
                return existingSet;
            } else {
                return data.stream().filter(item -> !existingSet.contains(item)).collect(Collectors.toSet());
            }
        });
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
