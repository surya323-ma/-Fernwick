package com.nursery;

/**
 * The servlets in this project build JSON responses by hand (no extra
 * dependency needed on top of the JDK + servlet API). This helper takes
 * care of escaping any text that gets embedded in a JSON string.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    public static String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }
}
