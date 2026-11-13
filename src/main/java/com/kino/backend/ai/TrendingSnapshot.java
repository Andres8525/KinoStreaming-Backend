package com.kino.backend.ai;

import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.VideoResponse;
import java.util.List;

public record TrendingSnapshot(
        List<VideoResponse> mostWatched,
        List<VideoResponse> rising,
        List<CategoryInsight> topCategories,
        long totalViews,
        int liveNow) {
}
