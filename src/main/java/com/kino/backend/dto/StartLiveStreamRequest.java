package com.kino.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartLiveStreamRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida") String playbackUrl,
        @Size(max = 2048) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida") String thumbnailUrl) {
}
