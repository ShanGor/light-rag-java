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
class PGVectorForChunksStorageTest {
    @Resource
    PGVectorForChunksStorage storage;

    @Test
    void query() {
        var str = "HOW, WHEN, AND WHERE GAME";
        var list = storage.query(str, 5).collectList().block();
        assertNotNull(list);
        log.info("query: {}, result: {}", str, list);
    }
}