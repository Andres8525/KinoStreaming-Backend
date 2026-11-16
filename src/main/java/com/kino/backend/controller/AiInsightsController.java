package com.kino.backend.controller;

import com.kino.backend.ai.AiRateLimiter;
import com.kino.backend.ai.AiRecommendationService;
import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.dto.AiChatRequest;
import com.kino.backend.dto.AiChatResponse;
import com.kino.backend.dto.AiRecommendationsResponse;
import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.realtime.KinoRealtimeHub;
import com.kino.backend.worker.AiWorker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ai")
public class AiInsightsController {
    private final TrendingInsightsService trendingInsightsService;
    private final AiRecommendationService recommendationService;
    private final AiWorker aiWorker;
    private final AiRateLimiter rateLimiter;
    private final KinoRealtimeHub realtimeHub;

    public AiInsightsController(TrendingInsightsService trendingInsightsService,
                                AiRecommendationService recommendationService, AiWorker aiWorker,
                                AiRateLimiter rateLimiter, KinoRealtimeHub realtimeHub) {
        this.trendingInsightsService = trendingInsightsService;
        this.recommendationService = recommendationService;
        this.aiWorker = aiWorker;
        this.rateLimiter = rateLimiter;
        this.realtimeHub = realtimeHub;
    }

    @GetMapping("/trending-insights")
    public TrendingInsightsResponse trendingInsights() {
        return trendingInsightsService.insights();
    }

    @PostMapping("/analysis")
    public CompletableFuture<TrendingInsightsResponse> generateAnalysis(HttpServletRequest httpRequest,
                                                                        Principal principal) {
        rateLimiter.acquire(clientKey(httpRequest, principal));
        return aiWorker.run(trendingInsightsService::regenerate).thenApply(insights -> {
            realtimeHub.broadcast("insights", insights);
            return insights;
        });
    }

    @GetMapping("/recommendations")
    public AiRecommendationsResponse recommendations(
            @RequestParam(required = false) @Size(max = 60, message = "La categoría es demasiado larga")
            String category) {
        return recommendationService.recommend(category);
    }

    @PostMapping("/chat")
    public CompletableFuture<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                                  HttpServletRequest httpRequest, Principal principal) {
        rateLimiter.acquire(clientKey(httpRequest, principal));
        return aiWorker.chat(request.messages());
    }

    private static String clientKey(HttpServletRequest httpRequest, Principal principal) {
        return principal == null ? "ip:" + httpRequest.getRemoteAddr() : "user:" + principal.getName();
    }
}
