package cn.gzten.rag.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

public class CsvUtil {
    private static final CSVFormat PARSER_FORMAT = CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.MINIMAL).build();

    public static String convertToCSV(List<List<Object>> listOfList) {
        if (listOfList == null || listOfList.isEmpty()) return "";

        StringWriter sw = new StringWriter();

        String[] HEADERS = getHeaders(listOfList);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setQuoteMode(QuoteMode.MINIMAL)
                .build();

        try (final CSVPrinter printer = new CSVPrinter(sw, csvFormat)) {
            for (var row : listOfList.subList(1, listOfList.size())) {
                printer.printRecord(row.toArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public static List<List<String>> parseCsv(String csv) {
        if (csv == null || csv.isEmpty()) return List.of();
        var csvReader = new StringReader(csv);
        try {
            return PARSER_FORMAT.parse(csvReader).getRecords().stream()
                    .map(record -> record.toList().stream().map(Object::toString).toList())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] getHeaders(List<List<Object>> listOfList) {
        if (listOfList == null || listOfList.isEmpty()) return new String[0];
        var headerRow = listOfList.get(0);
        String[] headers = new String[headerRow.size()];
        for (int i = 0; i < headerRow.size(); i++) {
            headers[i] = headerRow.get(i).toString();
        }
        return headers;
    }
}
