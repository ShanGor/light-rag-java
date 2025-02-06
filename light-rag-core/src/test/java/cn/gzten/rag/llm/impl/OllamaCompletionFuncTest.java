package cn.gzten.rag.llm.impl;

import cn.gzten.rag.LightRagApplication;
import cn.gzten.rag.data.pojo.DocStatusStore;
import cn.gzten.rag.data.pojo.FullDoc;
import cn.gzten.rag.data.pojo.TextChunk;
import cn.gzten.rag.data.storage.*;
import cn.gzten.rag.data.storage.pojo.RagEntity;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import cn.gzten.rag.data.storage.pojo.RagVectorChunk;
import cn.gzten.rag.llm.LlmCompletionFunc;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@SpringBootTest(classes = LightRagApplication.class)
@ActiveProfiles({"test", "test-ollama-completion"})
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "ollama")
class OllamaCompletionFuncTest {
    @Resource
    LlmCompletionFunc llmCompletionFunc;

    @MockitoBean
    BaseGraphStorage graphStorageService;
    @MockitoBean("entityStorage")
    BaseVectorStorage<RagEntity, RagEntity> entityStorageService;
    @MockitoBean("relationshipStorage")
    BaseVectorStorage<RagRelation, RagRelation> relationshipStorage;
    @MockitoBean("vectorForChunksStorage")
    BaseVectorStorage<RagVectorChunk, RagVectorChunk> vectorForChunksStorageService;
    @MockitoBean("docFullStorage")
    BaseKVStorage<? extends FullDoc> docFullStorageService;
    @MockitoBean("textChunkStorage")
    BaseTextChunkStorage<? extends TextChunk> textChunkStorageService;
    @MockitoBean("llmCacheStorage")
    LlmCacheStorage llmCacheStorageService;
    @MockitoBean("docStatusStorage")
    DocStatusStorage<? extends DocStatusStore> docStatusStorageService;

    @Test
    void complete() {
        var resp = llmCompletionFunc.completeStream("hello, what can you do for me?");
        resp.subscribe(s -> log.info("Response is: {}", s));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}