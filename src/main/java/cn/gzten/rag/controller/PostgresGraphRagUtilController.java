package cn.gzten.rag.controller;

import cn.gzten.rag.data.postgres.dao.VectorForEntityRepository;
import cn.gzten.rag.data.postgres.dao.VectorForRelationshipRepository;
import cn.gzten.rag.data.storage.BaseGraphStorage;
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
    @GetMapping("/graph/cache")
    @Transactional
    public String cacheGraphs() {
        Mono.defer(() -> {
            cachePostgresGraphs();
            return Mono.just("Done");
        }).subscribe();

        return "job submitted";
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
