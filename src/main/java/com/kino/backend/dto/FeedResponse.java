package com.kino.backend.dto;

import java.time.Instant;
import java.util.List;

public record FeedResponse(String strategy, Instant generatedAt, List<VideoResponse> recommendations) {
}