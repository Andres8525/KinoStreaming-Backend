package com.kino.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @NotEmpty(message = "Escribe una pregunta para Kino IA")
        @Size(max = AiChatRequest.MAX_MESSAGES, message = "La conversación es demasiado larga")
        List<@Valid ChatMessage> messages) {
    public static final int MAX_MESSAGES = 30;
}
