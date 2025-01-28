package cn.gzten.rag.data.postgres.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@ActiveProfiles("test-postgres")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
class PGGraphStorageTest {
    @Resource
    PGGraphStorage pgGraphStorage;
    @Test
    void testGraph() {
        pgGraphStorage.upsertNode("a", Map.of("name", "Samuel")).block();
        pgGraphStorage.upsertNode("b", Map.of("name", "Chan")).block();
        pgGraphStorage.upsertEdge("a", "b", Map.of("surname", "is")).block();

        assertTrue(pgGraphStorage.hasNode("a").block());
        assertTrue(pgGraphStorage.hasNode("b").block());
        assertFalse(pgGraphStorage.hasNode("c").block());

        var node = pgGraphStorage.getNodeAsMap("a").block();
        assertEquals("Samuel", node.get("name"));

        assertTrue(pgGraphStorage.hasEdge("a", "b").block());

        var res = pgGraphStorage.getEdgeAsMap("\"a\"", "b").block();
        assertEquals("is", res.get("surname"));

    }

    @Test
    void randomTest() {
        var entityName = "HOW, WHEN, AND WHERE GAME";
        var node = pgGraphStorage.getNode(entityName).block();
        assertNotNull(node);
        log.info("node: {}", node);

        var srcId = "A CHRISTMAS CAROL";
        var tgtId = "PROJECT GUTENBERG";
        var edge = pgGraphStorage.getEdge(srcId, tgtId).block();
        assertNotNull(edge);
        log.info("edge: {}", edge);
    }
}