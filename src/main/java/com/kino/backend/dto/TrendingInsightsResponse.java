package com.kino.backend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Result of the Kino trending analyzer.
 *
 * @param source "ai" when the summary was written by the configured AI provider,
 *               "kino-analytics" when it was generated from the platform statistics.
 */
public record TrendingInsightsResponse(
        Instant generatedAt,
        String source,
        String summary,
        List<VideoResponse> mostWatched,
        List<VideoResponse> rising,
        List<CategoryInsight> topCategories,
        long totalViews,
        int liveNow) {

    public TrendingInsightsResponse {
        mostWatched = List.copyOf(mostWatched);
        rising = List.copyOf(rising);
        topCategories = List.copyOf(topCategories);
    }
}
