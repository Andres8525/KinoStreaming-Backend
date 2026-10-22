package com.kino.backend.dto;

import com.kino.backend.model.Interaction;
import java.time.Instant;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        UUID videoId,
        String videoTitle,
        Integer rating,
        Integer watchedSeconds,
        Instant watchedAt) {

    public static InteractionResponse from(Interaction interaction) {
        return new InteractionResponse(
                interaction.getId(),
                interaction.getVideo().getId(),
                interaction.getVideo().getTitle(),
                interaction.getRating(),
                interaction.getWatchedSeconds(),
                interaction.getWatchedAt());
    }
}