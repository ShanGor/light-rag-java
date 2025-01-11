package cn.gzten.rag.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
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

    public static IntArrayList encodeStringByTiktoken(String content, EncodingType encodingType) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncoding(encodingType);
        return enc.encode(content);
    }

    public static String decodeTokensByTiktoken(IntArrayList tokens, EncodingType encodingType) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncoding(encodingType);
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

    static Pattern JS_TEMPLATE_PATTERN = Pattern.compile("\\$\\{(.+?)}");

    /**
     * Javascript template format: "hello, ${name}".format(name="world")
     * The implementation is naive, please do not add space between ${}
     * @param template
     * @param args
     * @return
     */
    public static String jsTemplateFormat(String template, Map<String, Object> args) {
        var str = template;
        var m = JS_TEMPLATE_PATTERN.matcher(str);
        while (m.find()) {
            var field = m.group(1);
            var value = args.get(field);
            if (value == null) {
                throw new IllegalArgumentException("No value found for field: %s".formatted(field));
            }
            str = str.replace("${%s}".formatted(field), value.toString());
        }
        return str;
    }
}
