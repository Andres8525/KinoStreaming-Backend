package com.kino.backend.ai;

import java.util.Optional;

public interface AiTextGenerator {
    String name();

    Optional<String> summarizeTrends(TrendingSnapshot snapshot);
}
