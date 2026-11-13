package com.kino.backend.dto;

import com.kino.backend.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Explicit Spanish messages: the frontend shows them next to each field whatever the server locale is. */
public record RegisterRequest(
        @NotBlank(message = "Escribe tu nombre")
        @Size(max = 120, message = "El nombre puede tener como máximo 120 caracteres")
        String displayName,
        @NotBlank(message = "Escribe tu correo electrónico")
        @Email(message = "Introduce un correo electrónico válido")
        @Size(max = 254, message = "El correo puede tener como máximo 254 caracteres")
        String email,
        @NotBlank(message = "Escribe una contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,
        UserRole role) {
}
