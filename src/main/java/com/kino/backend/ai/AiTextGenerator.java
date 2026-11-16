package com.kino.backend.ai;

import com.kino.backend.dto.ChatMessage;
import java.util.List;
import java.util.Optional;

public interface AiTextGenerator {
    String name();

    default String model() {
        return null;
    }

    Optional<String> summarizeTrends(TrendingSnapshot snapshot);

    default Optional<String> chat(String instructions, List<ChatMessage> conversation) {
        return Optional.empty();
    }
}
