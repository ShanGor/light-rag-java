package cn.gzten.rag.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    public static final DateTimeFormatter STD_GMT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * parse it to be a timestamp
     * @param dateTime format is "2023-08-04T19:22:45.499127Z"
     * @return
     */
    public static long parseOllamaDateTime(String dateTime) {
        var sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        try {
            return sdf.parse(dateTime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
