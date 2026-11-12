package com.kino.backend.dto;

import com.kino.backend.model.User;
import com.kino.backend.model.UserRole;
import java.util.UUID;

public record UserProfileResponse(UUID id, String displayName, String email, UserRole role) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole());
    }
}
