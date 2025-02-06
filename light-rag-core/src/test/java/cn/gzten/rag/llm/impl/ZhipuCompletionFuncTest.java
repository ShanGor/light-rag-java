package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.DocStatusStore;
import cn.gzten.rag.data.pojo.FullDoc;
import cn.gzten.rag.data.pojo.GPTKeywordExtractionFormat;
import cn.gzten.rag.data.pojo.TextChunk;
import cn.gzten.rag.data.storage.*;
import cn.gzten.rag.data.storage.pojo.RagEntity;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import cn.gzten.rag.data.storage.pojo.RagVectorChunk;
import cn.gzten.rag.llm.EmbeddingFunc;
import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles({"test", "test-zhipu-completion"})
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "zhipu")
class ZhipuCompletionFuncTest {
    @Resource
    LlmCompletionFunc llmCompletionFunc;
    @Resource
    ObjectMapper objectMapper;

    @MockitoBean
    CacheManager cacheManager;

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
    @MockitoBean
    EmbeddingFunc func;

    @Test
    void testComplete() {
        var resp = llmCompletionFunc.complete("hello, what can you do for me?");
        assertNotNull(resp);
        log.info("Response is: {}", resp);
    }

    @Test
    void testCompleteStream() {
        var resp = llmCompletionFunc.completeStream("hello, what can you do for me?");
        resp.subscribe(s -> log.info("Response is: {}", s));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testKW_JSON_PATTERN() {
        var str = """
               The result is:
               {
                    "high_level_keywords": ["keyword1", "keyword2"],
                    "low_level_keywords": ["keyword1", "keyword2", "keyword3"]
               }
               """;
        var match = ZhipuCompletionFunc.KW_JSON_PATTERN.matcher(str);
        if (match.find()) {
            try {
                var o = objectMapper.readValue(match.group(), GPTKeywordExtractionFormat.class);
                log.info("Result is: {}", o);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse keyword extraction response: {}", e.getMessage());
            }
        }
    }

}