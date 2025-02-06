package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.DocStatusStore;
import cn.gzten.rag.data.pojo.FullDoc;
import cn.gzten.rag.data.pojo.TextChunk;
import cn.gzten.rag.data.storage.*;
import cn.gzten.rag.data.storage.pojo.RagEntity;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import cn.gzten.rag.data.storage.pojo.RagVectorChunk;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
@ActiveProfiles({"test", "test-ollama-completion"})
@ConditionalOnProperty(value = "rag.llm.embedding.provider", havingValue = "ollama")
class OllamaEmbeddingFuncTest {
    @Resource
    OllamaEmbeddingFunc ollamaEmbeddingFunc;

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
    void testConvert() {
        var resp = ollamaEmbeddingFunc.convert("Why is the sky blue?");
        assertEquals(1024, resp.length);
        log.info("resp: {}", resp);
    }
}