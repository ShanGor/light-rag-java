package cn.gzten.rag.service;

import cn.gzten.rag.data.pojo.QueryParam;
import cn.gzten.rag.data.storage.BaseGraphStorage;
import cn.gzten.rag.data.storage.BaseKVStorage;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.DocStatusStorage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LightRagService {
    @Resource
    private BaseGraphStorage graphStorageService;
    @Resource
    @Qualifier("entityStorage")
    private BaseVectorStorage entityStorageService;
    @Resource
    @Qualifier("relationshipStorage")
    private BaseVectorStorage RelationshipStorageService;
    @Resource
    @Qualifier("vectorForChunksStorage")
    private BaseVectorStorage vectorForChunksStorageService;
    @Resource
    @Qualifier("docFullStorage")
    private BaseKVStorage docFullStorageService;
    @Resource
    @Qualifier("textChunkStorage")
    private BaseKVStorage textChunkStorageService;
    @Resource
    @Qualifier("llmCacheStorage")
    private BaseKVStorage llmCacheStorageService;
    @Resource
    @Qualifier("docStatusStorage")
    private DocStatusStorage docStatusStorageService;

    public void insert(String doc) {


    }

    public void query(String query, QueryParam param) {
        switch (param.getMode()) {
            case GLOBAL, LOCAL, HYBRID ->
                knowledgeGraphQuery(query, param);
        }
    }

    public void knowledgeGraphQuery(String query, QueryParam param) {

    }
}
