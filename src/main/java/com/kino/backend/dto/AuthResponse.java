package com.kino.backend.dto;

import com.kino.backend.model.UserRole;

public record AuthResponse(String accessToken, String tokenType, String email, String displayName, UserRole role) {
}