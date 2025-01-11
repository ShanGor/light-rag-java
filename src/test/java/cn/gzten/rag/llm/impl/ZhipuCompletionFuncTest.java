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
@ActiveProfiles("test-postgres")
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "zhipu")
class ZhipuCompletionFuncTest {
    @Resource
    ZhipuCompletionFunc llmCompletionFunc;
    @Test
    void testComplete() {
        var resp = llmCompletionFunc.complete("hello, what can you do for me?");
        assertNotNull(resp);
        log.info("Response is: {}", resp);
    }

}