package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.pojo.LlmCache;
import cn.gzten.rag.data.postgres.dao.LlmCacheEntity;
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
class PGKVForLlmCacheStorageTest {
    @Resource
    PGKVForLlmCacheStorage storage;

    @Test
    void upsert() {
        var o = LlmCacheEntity.builder().id("test").mode("test").originalPrompt("test").returnValue("test").workspace("default").build();
        storage.upsert(o).block();
        var cache = storage.getByModeAndId("test", "test").block();
        assertNotNull(cache);
        assertEquals(o.getId(), cache.getId());
        assertEquals(o.getMode(), cache.getMode());
        log.info("Cache is inserted as {}", cache);
    }

    @Test
    void testNotFound() {
        storage.getByModeAndId("hello", "world").defaultIfEmpty(LlmCache.EMPTY).mapNotNull(cache -> {
            if (cache.isEmpty()) {
                log.info("Cache is not found");
            } else {
                log.info("Cache is found as {}", cache);
            }
            return null;
        }).block();
    }
}