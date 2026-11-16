package com.kino.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotBlank(message = "Indica quién envía el mensaje")
        @Pattern(regexp = "user|assistant", message = "El rol debe ser user o assistant")
        String role,
        @NotBlank(message = "Escribe un mensaje")
        @Size(max = ChatMessage.MAX_CONTENT_LENGTH, message = "El mensaje no puede superar 1000 caracteres")
        String content) {
    public static final int MAX_CONTENT_LENGTH = 1000;
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public static ChatMessage user(String content) {
        return new ChatMessage(USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ASSISTANT, content);
    }

    public boolean fromUser() {
        return USER.equals(role);
    }
}
