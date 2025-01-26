package cn.gzten.rag.data.postgres.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test-postgres")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
class PGGraphStorageTest {
    @Resource
    PGGraphStorage pgGraphStorage;
    @Test
    void testGraph() {
        pgGraphStorage.upsertNode("a", Map.of("name", "Samuel"));
        pgGraphStorage.upsertNode("b", Map.of("name", "Chan"));
        pgGraphStorage.upsertEdge("a", "b", Map.of("surname", "is"));

        assertTrue(pgGraphStorage.hasNode("a"));
        assertTrue(pgGraphStorage.hasNode("b"));
        assertFalse(pgGraphStorage.hasNode("c"));

        var node = pgGraphStorage.getNode("a");
        assertEquals("Samuel", node.get("name"));

        assertTrue(pgGraphStorage.hasEdge("a", "b"));

        var res = pgGraphStorage.getEdge("\"a\"", "b");
        assertEquals("is", res.get("surname"));

    }

    @Test
    void randomTest() {
        var entityName = "HOW, WHEN, AND WHERE GAME";
        var node = pgGraphStorage.getNode(entityName);
        assertNotNull(node);
        log.info("node: {}", node);
    }
}