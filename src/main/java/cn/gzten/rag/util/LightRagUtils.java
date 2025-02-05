package cn.gzten.rag.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
public class LightRagUtils {
    public static String computeMd5(String input) {
        if (input == null) return "";
        String inputTrim = input.trim();
        if (inputTrim.isEmpty()) return "";

        try {
            var md5 = MessageDigest.getInstance("MD5");
            md5.update(inputTrim.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initiate an MD5 instance!");
            throw new RuntimeException(e);
        }
    }

    public static String computeMd5(String input, String prefix) {
        String result = computeMd5(input);
        if (StringUtils.isBlank(prefix)) return result;

        return "%s%s".formatted(prefix, result);
    }

    /**
     * Default encodingType is CL100K_BASE
     * @param content
     * @return
     */
    public static IntArrayList encodeStringByTiktoken(String content) {
        return encodeStringByTiktoken(content, EncodingType.CL100K_BASE);
    }

    private static final EncodingRegistry tikTokenRegistry = Encodings.newDefaultEncodingRegistry();
    private static final Encoding defaultEncoding = tikTokenRegistry.getEncoding(EncodingType.CL100K_BASE);

    public static IntArrayList encodeStringByTiktoken(String content, EncodingType encodingType) {
        if (encodingType.equals(EncodingType.CL100K_BASE)) {
            return defaultEncoding.encode(content);
        }

        var enc = tikTokenRegistry.getEncoding(encodingType);
        return enc.encode(content);
    }

    public static String decodeTokensByTiktoken(IntArrayList tokens, EncodingType encodingType) {
        if (encodingType.equals(EncodingType.CL100K_BASE)) {
            return defaultEncoding.decode(tokens);
        }

        Encoding enc = tikTokenRegistry.getEncoding(encodingType);
        return enc.decode(tokens);
    }

    /**
     * Default encodingType is CL100K_BASE
     * @param tokens
     * @return
     */
    public static String decodeTokensByTiktoken(IntArrayList tokens) {
        return decodeTokensByTiktoken(tokens, EncodingType.CL100K_BASE);
    }

    static Pattern JS_TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)}");

    /**
     * Python template format: f"hello, {name}".format(name="world")
     * The implementation is naive, please do not add space between ${}
     * @param template
     * @param args
     * @return
     */
    public static <T> String pythonTemplateFormat(String template, Map<String, T> args) {
        var str = template;
        var m = JS_TEMPLATE_PATTERN.matcher(str);
        while (m.find()) {
            var field = m.group(1);
            var value = args.get(field);
            if (value == null) {
                throw new IllegalArgumentException("No value found for field: %s".formatted(field));
            }
            str = str.replace("{%s}".formatted(field), value.toString());
        }
        return str;
    }

    /**
     * Split a string by multiple markers
     */
    public static List<String> splitStringByMultiMarkers(String content, List<String> markers) {
        if (isEmptyCollection(markers)) {
            return List.of(content);
        }
        var escapedMarkers = new ArrayList<String>(markers.size());
        for (var marker : markers) {
            escapedMarkers.add(Pattern.quote(marker));
        }
        var regex = String.join("|", escapedMarkers);
        var results = content.split(regex);
        if (results.length == 0) return List.of(content);

        return Arrays.stream(results).map(String::trim).filter(StringUtils::isNotBlank).toList();
    }

    public static <T extends Collection<?>> boolean isEmptyCollection(T obj) {
        return obj == null || obj.isEmpty();
    }

    public static <T extends Map<?, ?>> boolean isEmptyCollection(T obj) {
        return obj == null || obj.isEmpty();
    }

    public static <T> List<T> truncateListByTokenSize(List<T> list, Function<T, String> keyFunc, int maxTokenSize) {
        if (maxTokenSize <= 0) {
            return Collections.emptyList();
        }

        int tokens = 0;
        for (int i=0; i<list.size(); i++) {
            var str = keyFunc.apply(list.get(i));
            var intArray = encodeStringByTiktoken(str);
            tokens += intArray.size();
            if (tokens > maxTokenSize) {
                return list.subList(0, i);
            }
        }
        return list;
    }

    /**
     *  Python equivalent:
     * header = None
     *     list_hl = csv_string_to_list(hl.strip())
     *     list_ll = csv_string_to_list(ll.strip())
     *
     *     if list_hl:
     *         header = list_hl[0]
     *         list_hl = list_hl[1:]
     *     if list_ll:
     *         header = list_ll[0]
     *         list_ll = list_ll[1:]
     *     if header is None:
     *         return ""
     *
     *     if list_hl:
     *         list_hl = [",".join(item[1:]) for item in list_hl if item]
     *     if list_ll:
     *         list_ll = [",".join(item[1:]) for item in list_ll if item]
     *
     *     combined_sources = []
     *     seen = set()
     *
     *     for item in list_hl + list_ll:
     *         if item and item not in seen:
     *             combined_sources.append(item)
     *             seen.add(item)
     *
     *     combined_sources_result = [",\t".join(header)]
     *
     *     for i, item in enumerate(combined_sources, start=1):
     *         combined_sources_result.append(f"{i},\t{item}")
     *
     *     combined_sources_result = "\n".join(combined_sources_result)
     *
     *     return combined_sources_result
     */
    public static String processCombineContexts(String hl, String ll) {
        var list_hl = CsvUtil.parseCsv(hl.strip());
        var list_ll = CsvUtil.parseCsv(ll.strip());
        List<String> header = null;
        if (!isEmptyCollection(list_hl)) {
            header = list_hl.get(0);
            list_hl = list_hl.subList(1, list_hl.size());
        }
        if (!isEmptyCollection(list_ll)) {
            header = list_ll.get(0);
            list_ll = list_ll.subList(1, list_ll.size());
        }
        if (isEmptyCollection(header)) {
            return "";
        }

        Set<String> seen = new HashSet<>();
        var combined_sources_result = new ArrayList<String>();
        combined_sources_result.add(String.join(",\t", header));

        var i = new AtomicInteger(0);

        BiConsumer<List<String>, AtomicInteger> addItem = (item, idx) -> {
            if (!isEmptyCollection(item)) {
                var value = String.join(",", item.subList(1, item.size()));
                if (StringUtils.isNotBlank(value) && !seen.contains(value)) {
                    combined_sources_result.add("%d,\t%s".formatted(idx.incrementAndGet(), value));
                    seen.add(value);
                }
            }
        };

        if (!isEmptyCollection(list_hl)) {
            for (var item : list_hl) {
                addItem.accept(item, i);
            }
        }
        if (!isEmptyCollection(list_ll)) {
            for (var item : list_ll) {
                addItem.accept(item, i);
            }
        }
        return String.join("\n", combined_sources_result);
    }

    public static String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        var sb = new StringBuilder("[");
        var start = true;
        for (var v : vector) {
            if (start) {
                start = false;
            } else {
                sb.append(",");
            }
            sb.append(v);
        }
        sb.append("]");
        return sb.toString();
    }

    public static float[] stringToVector(String vector) {
        if (StringUtils.isBlank(vector)) {
            return new float[0];
        }
        String vectorStr = vector.replace("[", "").replace("]", "");
        var tokens = vectorStr.split(",");
        var vectorArray = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            vectorArray[i] = Float.parseFloat(tokens[i]);
        }
        return vectorArray;
    }

    private static final ObjectMapper objectMapperSnake = new ObjectMapper();
    static {
        objectMapperSnake.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapperSnake.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapperSnake.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapperSnake.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    public static String objectToJsonSnake(Object o) {
        try {
            return objectMapperSnake.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) {
        try {
            return objectMapperSnake.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Some string may be wrapped by json string, like: "\"hello world\"", we need to unwrap it.
     */
    public static String unwrapJsonString(String strInJson) {
        if (StringUtils.isBlank(strInJson)) {
            return strInJson;
        }
        var trimmed = strInJson.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                return objectMapperSnake.readValue(trimmed, String.class);
            } catch (JsonProcessingException e) {
                return trimmed;
            }
        } else {
            return trimmed;
        }
    }

    public static CompletableFuture[] fromList(List<CompletableFuture> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new CompletableFuture[0];
        }
        var arr = new CompletableFuture[tasks.size()];
        var i = 0;
        for (var task : tasks) {
            arr[i++] = task;
        }
        return arr;
    }

    /**
     * Mono and Flux is not supposed to be blocked, to avoid problem, we need to block it in a new thread. Here we play a trick to use virtual thread to get the result.
     * Only valid in Java 19+;
     */
    public static <T> T monoBlock(Mono<T> mono) {
        var ref = new AtomicReference<T>();
        try {
            Thread.ofVirtual().start(() -> {
                ref.set(mono.block());
            }).join();
            return ref.get();
        } catch (InterruptedException e) {
            log.error("monoBlock error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
