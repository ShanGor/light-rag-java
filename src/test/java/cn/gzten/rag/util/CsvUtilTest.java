package cn.gzten.rag.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    void convertToCSV() {
        List<List<Object>> listOfList = new ArrayList<>();
        listOfList.add(Arrays.asList("a", "b", "c"));
        listOfList.add(Arrays.asList(1, "hello \"world\"", "3"));
        listOfList.add(Arrays.asList(4, """
                Someone like, you""", "6"));
        String csv = CsvUtil.convertToCSV(listOfList);
        assertEquals("""
                a,b,c
                1,"hello ""world""\",3
                4,"Someone like, you",6
                """.replace("\n", System.lineSeparator()), csv);
    }
}