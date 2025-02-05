package cn.gzten.rag.controller;

import cn.gzten.rag.data.postgres.dao.DocChunkRepository;
import cn.gzten.rag.data.postgres.dao.VectorForEntityRepository;
import cn.gzten.rag.data.postgres.dao.VectorForRelationshipRepository;
import cn.gzten.rag.data.storage.BaseGraphStorage;
import cn.gzten.rag.llm.EmbeddingFunc;
import cn.gzten.rag.util.LightRagUtils;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PostgresGraphRagUtilController {
    @Resource
    BaseGraphStorage graphStorage;
    @Resource
    VectorForEntityRepository entityRepo;
    @Resource
    VectorForRelationshipRepository relationRepo;
    @Resource
    DocChunkRepository chunkRepo;
    @Resource
    EmbeddingFunc embeddingFunc;
    @GetMapping("/graph/cache")
    @Transactional
    public String cacheGraphs() {
        Mono.defer(() -> {
            cachePostgresGraphs();
            return Mono.just("Done");
        }).subscribe();

        return "job submitted";
    }

    @GetMapping("/vector/update")
    @Transactional
    public String updateVectorWithNewModel() {
        Mono.defer(() -> {
            updateVectors();
            return Mono.just("Done");
        }).subscribe();
        return "job submitted";
    }

    public void updateVectors() {
        var count = new AtomicInteger(0);
        entityRepo.streamAll().forEach(entity -> {
            var processed = count.incrementAndGet();
            var content = entity.getContent();
            if (StringUtils.isNotBlank(entity.getContent())) {
                var vector = embeddingFunc.convert(content);
                entity.setContentVector(vector);
                entityRepo.save(entity);
            }
            if (processed % 10 == 0) {
                log.info("=== Entity vector update, count: {}", processed);
            }
        });
        log.info("=== finish Entity vector update! {} records processed!", count.get());

        count.set(0);
        relationRepo.streamAllForGraph().forEach(relation -> {
            var processed = count.incrementAndGet();
            var content = relation.getContent();
            if (StringUtils.isNotBlank(relation.getContent())) {
                var vector = embeddingFunc.convert(content);
                assert vector.length == 1024;
                relation.setContentVector(vector);
                relationRepo.save(relation);
                if (processed % 10 == 0) {
                    log.info("=== Relation vector update, count: {}", processed);
                }
            }
        });
        log.info("=== finish Relation vector update! {} records processed!", count.get());

        count.set(0);
        chunkRepo.streamAll().forEach(chunk -> {
            var processed = count.incrementAndGet();
            var content = chunk.getContent();
            if (StringUtils.isNotBlank(chunk.getContent())) {
                var vector = embeddingFunc.convert(content);
                assert vector.length == 1024;
                chunk.setContentVector(vector);
                chunkRepo.save(chunk);
                if (processed % 10 == 0) {
                    log.info("=== Chunk vector update, count: {}", processed);
                }
           }
        });
        log.info("=== finish Chunk vector update! {} records processed!", count.get());
    }

    public void cachePostgresGraphs() {
        log.info("=== start to cache entity for graph data");
        var count = new AtomicInteger(0);
        entityRepo.streamAllForGraph().forEach(entity -> {
            try {
                count.incrementAndGet();
                if (StringUtils.isBlank(entity.getGraphProperties())) {
                    var entityName = entity.getEntityName();
                    var node = graphStorage.getNode(entityName);
                    if (node != null) {
                        entity.setGraphProperties(LightRagUtils.objectToJsonSnake(node));
                        var degree = graphStorage.nodeDegree(entityName);
                        entity.setGraphNodeDegree(degree);
                        entityRepo.save(entity);
                    }
                }
                if (count.get() % 10 == 0) {
                    log.info("=== cache entity for graph data, count: {}", count.get());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        });
        log.info("=== finish to cache entity for graph data! {} records processed!", count.get());

        count.set(0);
        log.info("=== start to cache relation for graph data");
        relationRepo.streamAllForGraph().forEach(relation -> {
            try {
                count.incrementAndGet();
                if (StringUtils.isBlank(relation.getGraphProperties())) {
                    var sourceId = relation.getSourceId();
                    var targetId = relation.getTargetId();
                    var edge = graphStorage.getEdge(sourceId, targetId);
                    if (edge != null) {
                        relation.setGraphProperties(LightRagUtils.objectToJsonSnake(edge));
                        relation.setGraphEdgeDegree(graphStorage.edgeDegree(sourceId, targetId));
                        relation.setGraphStartNodeDegree(graphStorage.nodeDegree(sourceId));
                        relation.setGraphEndNodeDegree(graphStorage.nodeDegree(targetId));
                        relation.setGraphStartNode(LightRagUtils.objectToJsonSnake(graphStorage.getNode(sourceId)));
                        relation.setGraphEndNode(LightRagUtils.objectToJsonSnake(graphStorage.getNode(targetId)));
                        relationRepo.save(relation);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            if (count.get() % 10 == 0) {
                log.info("=== cache relation for graph data, count: {}", count.get());
            }
        });
        log.info("=== finish to cache relation for graph data! {} records processed!", count.get());
    }
}
