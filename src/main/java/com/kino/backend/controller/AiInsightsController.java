package com.kino.backend.controller;

import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.dto.TrendingInsightsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Kino IA": public trending analysis shown on the dashboard. */
@RestController
@RequestMapping("/api/ai")
public class AiInsightsController {

    private final TrendingInsightsService trendingInsightsService;

    public AiInsightsController(TrendingInsightsService trendingInsightsService) {
        this.trendingInsightsService = trendingInsightsService;
    }

    @GetMapping("/trending-insights")
    public TrendingInsightsResponse trendingInsights() {
        return trendingInsightsService.insights();
    }
}
