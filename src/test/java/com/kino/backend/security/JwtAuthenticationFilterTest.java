package com.kino.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JwtAuthenticationFilterTest {

    @Test
    void extractsTheBearerToken() {
        assertEquals("abc.def.ghi", JwtAuthenticationFilter.bearerToken("Bearer abc.def.ghi"));
        assertEquals("abc.def.ghi", JwtAuthenticationFilter.bearerToken("  bearer    abc.def.ghi  "));
        assertEquals("abc.def.ghi", JwtAuthenticationFilter.bearerToken("BEARER abc.def.ghi"));
    }

    @Test
    void ignoresOtherSchemesAndEmptyTokens() {
        assertNull(JwtAuthenticationFilter.bearerToken(null));
        assertNull(JwtAuthenticationFilter.bearerToken("Basic dXNlcjpwYXNz"));
        assertNull(JwtAuthenticationFilter.bearerToken("Bearer    "));
        assertNull(JwtAuthenticationFilter.bearerToken("Bearer"));
    }
}
