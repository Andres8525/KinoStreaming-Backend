package com.kino.backend.dto;

import java.time.Instant;
import java.util.List;

public record AiChatResponse(String reply, String source, List<VideoResponse> suggestions, Instant generatedAt) {
    public AiChatResponse {
        suggestions = List.copyOf(suggestions);
    }
}
