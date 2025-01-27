package cn.gzten.rag.controller;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.postgres.dao.VectorForEntityEntity;
import cn.gzten.rag.data.postgres.dao.VectorForRelationshipEntity;
import cn.gzten.rag.data.storage.BaseGraphStorage;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.pojo.RagEntity;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class GraphRagUtilController {
    @Resource
    BaseGraphStorage graphStorage;
    @Resource
    @Qualifier("entityStorage")
    private BaseVectorStorage<RagEntity, String> entityStorageService;
    @Resource
    @Qualifier("relationshipStorage")
    private BaseVectorStorage<RagRelation, NullablePair<String, String>> relationshipStorage;
    @GetMapping("/graph/cache")
    public void cacheGraphs() {
        entityStorageService.traverse((VectorForEntityEntity entity) -> {
            var entityName = entity.getEntityName();
            var node = graphStorage.getNode(entityName);

        });

        relationshipStorage.traverse((VectorForRelationshipEntity relation) -> {

        });
    }
}
