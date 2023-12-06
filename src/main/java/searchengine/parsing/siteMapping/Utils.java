package searchengine.parsing.siteMapping;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.regex.Pattern;

public class Utils {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
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

    public static Timestamp setNow() {
        return new Timestamp(System.currentTimeMillis());
    }
}
