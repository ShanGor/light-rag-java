package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.GPTKeywordExtractionFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Resource
    ObjectMapper objectMapper;
    @Test
    void testComplete() {
        var resp = llmCompletionFunc.complete("hello, what can you do for me?");
        assertNotNull(resp);
        log.info("Response is: {}", resp);
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