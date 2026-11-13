package com.kino.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartLiveStreamRequest(
        @NotBlank(message = "Escribe un título para la transmisión")
        @Size(max = 180, message = "El título puede tener como máximo 180 caracteres")
        String title,
        @Size(max = 1000, message = "La descripción puede tener como máximo 1000 caracteres")
        String description,
        @NotBlank(message = "Elige una categoría")
        @Size(max = 60, message = "La categoría puede tener como máximo 60 caracteres")
        String category,
        @NotBlank(message = "Indica la URL de reproducción HLS")
        @Size(max = 2048, message = "La URL puede tener como máximo 2048 caracteres")
        @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida")
        String playbackUrl,
        @Size(max = 2048, message = "La URL puede tener como máximo 2048 caracteres")
        @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida")
        String thumbnailUrl) {
}
