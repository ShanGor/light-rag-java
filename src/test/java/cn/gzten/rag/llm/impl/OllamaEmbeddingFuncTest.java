package cn.gzten.rag.llm.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
@ActiveProfiles({"test", "test-postgres"})
@ConditionalOnProperty(value = "rag.llm.embedding.provider", havingValue = "ollama")
class OllamaEmbeddingFuncTest {
    @Resource
    OllamaEmbeddingFunc ollamaEmbeddingFunc;
    @Test
    void testConvert() {
        var resp = ollamaEmbeddingFunc.convert("Why is the sky blue?");
        assertEquals(1024, resp.length);
        log.info("resp: {}", resp);
    }
}