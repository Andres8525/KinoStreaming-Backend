package com.kino.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CreateInteractionRequest(
        @NotNull UUID videoId,
        @Min(1) @Max(5) Integer rating,
        @NotNull @PositiveOrZero Integer watchedSeconds) {
}