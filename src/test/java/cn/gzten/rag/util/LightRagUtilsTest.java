package cn.gzten.rag.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

    @Test
    void testIsEmptyCollection() {
        Map<String, String> s = null;
        assertTrue(LightRagUtils.isEmptyCollection(s));
        s = new HashMap<>();
        assertTrue(LightRagUtils.isEmptyCollection(s));
        s.put("", "");
        assertFalse(LightRagUtils.isEmptyCollection(s));

        List<String> l = null;
        assertTrue(LightRagUtils.isEmptyCollection(l));
        l = new LinkedList<>();
        assertTrue(LightRagUtils.isEmptyCollection(l));
        l.add("");
        assertFalse(LightRagUtils.isEmptyCollection(l));
    }

    @Test
    void stringToVector() {
        assertArrayEquals(new float[]{1, 2, 3}, LightRagUtils.stringToVector("[1.0,2.0,3.0]"));
    }

    @Test
    void vectorToString() {
        assertEquals("[1.0,2.0,3.0]", LightRagUtils.vectorToString(new float[]{1, 2, 3}));
    }

    @Test
    void unwrapJsonString() {
        assertEquals("hello world", LightRagUtils.unwrapJsonString("\"hello world\""));
    }
}