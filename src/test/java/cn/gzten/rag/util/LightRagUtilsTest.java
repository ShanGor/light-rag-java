package cn.gzten.rag.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LightRagUtilsTest {

    @Test
    void testComputeMd5() {
        assertEquals("", LightRagUtils.computeMd5(null));
        assertEquals("", LightRagUtils.computeMd5("  "));
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", LightRagUtils.computeMd5("hello world"));

        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", LightRagUtils.computeMd5("hello world", ""));
        assertEquals("doc-5eb63bbbe01eeed093cb22bb8f5acdc3", LightRagUtils.computeMd5("hello world", "doc-"));
    }

    @Test
    void testTiktoken() {
        var tokens = LightRagUtils.encodeStringByTiktoken("hello world");
        assertEquals("hello world", LightRagUtils.decodeTokensByTiktoken(tokens));
    }

    @Test
    void testPythonTemplateFormat() {
        assertEquals("hello world", LightRagUtils.pythonTemplateFormat("hello {name}", Map.of("name", "world")));
        assertEquals("hello world! I like world", LightRagUtils.pythonTemplateFormat("hello {name}! I like {name}", Map.of("name", "world")));
        assertEquals("hello world! At this moment, I like world",
                LightRagUtils.pythonTemplateFormat("hello {name}! At {someWhereWhat}, I like {name}", Map.of("name", "world", "someWhereWhat", "this moment")));
    }
}