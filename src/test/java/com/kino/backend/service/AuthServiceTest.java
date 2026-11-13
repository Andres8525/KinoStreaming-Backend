package com.kino.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthServiceTest {

    @Test
    void normalizesDisplayNames() {
        assertEquals("Ana Ruiz", AuthService.normalizeDisplayName("  Ana \t  Ruiz  "));
        assertEquals("Kino", AuthService.normalizeDisplayName("Kino"));
    }
}
