package cn.gzten.rag.data.postgres.impl;

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
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
class PGKVForTextChunkStorageTest {
    @Resource
    PGKVForTextChunkStorage textChunkStorage;
    @Test
    void testRead() {
        var o = textChunkStorage.getById("chunk-9e3921da66da5d761ab73cd849af6c43").block();
        assertTrue(o != null);
        log.info("{}", o);
    }
}