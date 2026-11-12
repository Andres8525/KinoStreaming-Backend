package com.kino.backend.dto;

import java.util.List;

public record CreatorAnalyticsResponse(long videoCount, long totalViews, List<VideoResponse> topVideos) {
    public CreatorAnalyticsResponse {
        topVideos = List.copyOf(topVideos);
    }
}
