package com.kino.backend.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the {@code CORS_ALLOWED_ORIGINS} setting into exact origins and wildcard patterns.
 *
 * <p>The value is usually typed by hand in a hosting dashboard, so it is normalized: entries may be
 * separated by commas, semicolons or whitespace, surrounding quotes and trailing slashes are removed
 * ({@code "https://kino.vercel.app/"} becomes {@code https://kino.vercel.app}) and duplicates are dropped.
 * Entries containing {@code *} (for example {@code https://kino-*.vercel.app}, useful for Vercel
 * preview deployments) are returned as patterns.
 */
public record CorsOrigins(List<String> origins, List<String> patterns) {

    public CorsOrigins {
        origins = List.copyOf(origins);
        patterns = List.copyOf(patterns);
    }

    public static CorsOrigins parse(String raw) {
        Set<String> origins = new LinkedHashSet<>();
        Set<String> patterns = new LinkedHashSet<>();
        if (raw != null) {
            for (String token : raw.split("[,;\\s]+")) {
                String origin = normalize(token);
                if (origin.isEmpty()) {
                    continue;
                }
                if (origin.contains("*")) {
                    patterns.add(origin);
                } else {
                    origins.add(origin);
                }
            }
        }
        return new CorsOrigins(new ArrayList<>(origins), new ArrayList<>(patterns));
    }

    private static String normalize(String token) {
        String value = token.trim();
        while (!value.isEmpty() && (value.startsWith("\"") || value.startsWith("'"))) {
            value = value.substring(1);
        }
        while (!value.isEmpty() && (value.endsWith("\"") || value.endsWith("'") || value.endsWith("/"))) {
            value = value.substring(0, value.length() - 1);
        }
        return stripPath(value.trim());
    }

    /** An origin is scheme://host[:port]; drop any path, query or fragment pasted from the address bar. */
    private static String stripPath(String value) {
        int schemeEnd = value.indexOf("://");
        if (schemeEnd < 0) {
            return value;
        }
        int pathStart = indexOfAny(value, schemeEnd + 3, '/', '?', '#');
        return pathStart < 0 ? value : value.substring(0, pathStart);
    }

    private static int indexOfAny(String value, int from, char... characters) {
        for (int index = from; index < value.length(); index++) {
            for (char character : characters) {
                if (value.charAt(index) == character) {
                    return index;
                }
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return origins.isEmpty() && patterns.isEmpty();
    }
}
