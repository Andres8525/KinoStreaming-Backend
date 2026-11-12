package com.kino.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes";

    private final UserDetails viewer = User.withUsername("viewer@example.com")
            .password("ignored")
            .roles("VIEWER")
            .build();

    @Test
    void generatedTokenContainsSubjectAndIsValidForSameUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken(viewer);

        assertEquals("viewer@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, viewer));
    }

    @Test
    void tokenIsNotValidForAnotherUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails other = User.withUsername("other@example.com").password("ignored").roles("VIEWER").build();

        assertFalse(jwtService.isTokenValid(jwtService.generateToken(viewer), other));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1_000);

        String token = jwtService.generateToken(viewer);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreignToken = new JwtService("another-secret-key-that-is-at-least-32-bytes", 60_000)
                .generateToken(viewer);

        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000).extractUsername(foreignToken));
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }
}
