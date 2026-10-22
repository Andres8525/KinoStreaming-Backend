package com.kino.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateVideoRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 2048) @Pattern(regexp = "https?://\\S+", message = "Debe ser una URL HTTP o HTTPS válida") String videoUrl,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 40) String> tags,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal accessPrice,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal suggestedDonation) {
}