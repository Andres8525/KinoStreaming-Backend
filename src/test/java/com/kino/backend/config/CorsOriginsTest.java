package com.kino.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsOriginsTest {

    @Test
    void normalizesHandTypedValues() {
        CorsOrigins origins = CorsOrigins.parse(
                " \"https://kino.vercel.app/\" ; http://localhost:4200,  'https://kino.example.com//' https://kino.vercel.app");

        assertEquals(List.of("https://kino.vercel.app", "http://localhost:4200", "https://kino.example.com"),
                origins.origins());
        assertTrue(origins.patterns().isEmpty());
    }

    @Test
    void separatesWildcardPatterns() {
        CorsOrigins origins = CorsOrigins.parse("https://kino.vercel.app,https://kino-*.vercel.app/");

        assertEquals(List.of("https://kino.vercel.app"), origins.origins());
        assertEquals(List.of("https://kino-*.vercel.app"), origins.patterns());
    }

    @Test
    void keepsOnlyTheOriginOfUrlsWithPaths() {
        CorsOrigins origins = CorsOrigins.parse(
                "https://kino-streamingg-frontend.vercel.app/login?returnUrl=%2F,http://localhost:4200/#top");

        assertEquals(List.of("https://kino-streamingg-frontend.vercel.app", "http://localhost:4200"), origins.origins());
    }

    @Test
    void handlesEmptyValues() {
        assertTrue(CorsOrigins.parse(null).isEmpty());
        assertTrue(CorsOrigins.parse(" , ; ").isEmpty());
    }
}
