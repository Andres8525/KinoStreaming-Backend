package com.kino.backend.dto;

public record VideoRecommendation(VideoResponse video, String reason, double score) {
}
