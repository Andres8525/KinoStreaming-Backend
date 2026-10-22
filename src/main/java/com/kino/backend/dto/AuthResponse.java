package com.kino.backend.dto;

import com.kino.backend.model.UserRole;

public record AuthResponse(String accessToken, String tokenType, String email, UserRole role) {
}