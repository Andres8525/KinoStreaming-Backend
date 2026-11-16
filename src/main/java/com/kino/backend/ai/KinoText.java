package com.kino.backend.ai;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

final class KinoText {
    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private KinoText() {
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    static boolean containsAny(String normalizedText, String... keywords) {
        for (String keyword : keywords) {
            if (normalizedText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    static String views(long views) {
        return NumberFormat.getIntegerInstance(SPANISH).format(views) + (views == 1 ? " vista" : " vistas");
    }
}
