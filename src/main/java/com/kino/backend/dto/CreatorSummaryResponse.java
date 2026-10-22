package com.kino.backend.dto;

import com.kino.backend.model.User;
import java.util.UUID;

public record CreatorSummaryResponse(UUID id, String displayName) {

    public static CreatorSummaryResponse from(User user) {
        return new CreatorSummaryResponse(user.getId(), user.getDisplayName());
    }
}