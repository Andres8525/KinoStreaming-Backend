package com.kino.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Escribe tu correo electrónico")
        @Email(message = "Introduce un correo electrónico válido")
        String email,
        @NotBlank(message = "Escribe tu contraseña")
        String password) {
}
