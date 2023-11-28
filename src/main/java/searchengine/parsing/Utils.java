package searchengine.parsing;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Utils {
    public static String getProtocolAndDomain(String url) {
        String regEx = "(^https:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(regEx);
        String utf8EncodedString = StandardCharsets.UTF_8.decode(buffer).toString();
        Pattern pattern = Pattern.compile(utf8EncodedString);
        return pattern.matcher(url)
                .results()
                .map(m -> m.group(1) + m.group(2))
                .findFirst()
                .orElseThrow();
    }
}
