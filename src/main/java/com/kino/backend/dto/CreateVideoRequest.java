package com.kino.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Explicit Spanish messages so Creator Studio shows readable errors whatever the server locale is. */
public record CreateVideoRequest(
        @NotBlank(message = "Escribe un título")
        @Size(max = 180, message = "El título puede tener como máximo 180 caracteres")
        String title,
        @NotBlank(message = "Escribe una descripción")
        @Size(max = 5000, message = "La descripción puede tener como máximo 5000 caracteres")
        String description,
        @NotBlank(message = "Indica la URL del video")
        @Size(max = 2048, message = "La URL puede tener como máximo 2048 caracteres")
        @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida")
        String videoUrl,
        @Size(max = 2048, message = "La URL puede tener como máximo 2048 caracteres")
        @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida")
        String thumbnailUrl,
        @NotNull(message = "Indica al menos una etiqueta")
        @Size(max = 20, message = "Usa como máximo 20 etiquetas")
        List<@NotBlank(message = "Las etiquetas no pueden estar vacías")
             @Size(max = 40, message = "Cada etiqueta puede tener como máximo 40 caracteres") String> tags,
        @NotNull(message = "Indica el precio de acceso (0 para gratis)")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Usa como máximo 2 decimales")
        BigDecimal accessPrice,
        @DecimalMin(value = "0.00", message = "La donación sugerida no puede ser negativa")
        @Digits(integer = 8, fraction = 2, message = "Usa como máximo 2 decimales")
        BigDecimal suggestedDonation) {
}
