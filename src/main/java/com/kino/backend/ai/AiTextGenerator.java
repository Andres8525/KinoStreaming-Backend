package com.kino.backend.ai;

import java.util.Optional;

public interface AiTextGenerator {
    String name();

    default String model() {
        return null;
    }

    Optional<String> summarizeTrends(TrendingSnapshot snapshot);
}
