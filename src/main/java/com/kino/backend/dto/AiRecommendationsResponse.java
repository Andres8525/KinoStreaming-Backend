package com.kino.backend.dto;

import java.time.Instant;
import java.util.List;

public record AiRecommendationsResponse(
        String category,
        String source,
        String summary,
        List<VideoRecommendation> recommendations,
        Instant generatedAt) {
    public AiRecommendationsResponse {
        recommendations = List.copyOf(recommendations);
    }
}
