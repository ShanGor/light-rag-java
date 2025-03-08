package cn.gzten.rag.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void parseOllamaDateTime() {
        assertEquals(1670371200000L, DateTimeUtils.parseOllamaDateTime("2022-12-07T08:00:00.000000Z"));
        assertEquals(1691148664127L, DateTimeUtils.parseOllamaDateTime("2023-08-04T19:22:45.499127Z"));
    }
}