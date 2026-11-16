package com.kino.backend.worker;

import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.realtime.KinoRealtimeHub;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsightsRefreshWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsRefreshWorker.class);

    private final TrendingInsightsService insightsService;
    private final KinoRealtimeHub hub;
    private final boolean enabled;
    private final AtomicReference<String> lastFingerprint = new AtomicReference<>("");
    private final AtomicLong broadcasts = new AtomicLong();
    private final AtomicReference<Instant> lastRun = new AtomicReference<>();

    public InsightsRefreshWorker(TrendingInsightsService insightsService, KinoRealtimeHub hub,
                                 @Value("${kino.workers.insights-refresh.enabled:true}") boolean enabled) {
        this.insightsService = insightsService;
        this.hub = hub;
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${kino.workers.insights-refresh.interval:PT20S}",
            fixedDelayString = "${kino.workers.insights-refresh.interval:PT20S}")
    public void refresh() {
        if (!enabled || hub.connectionCount() == 0) {
            return;
        }
        lastRun.set(Instant.now());
        try {
            TrendingInsightsResponse insights = insightsService.refreshStatistics();
            String fingerprint = fingerprint(insights);
            if (!fingerprint.equals(lastFingerprint.getAndSet(fingerprint))) {
                hub.broadcast("insights", insights);
                broadcasts.incrementAndGet();
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("The insights refresh worker failed: {}", exception.getMessage());
        }
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("lastRun", lastRun.get());
        status.put("broadcasts", broadcasts.get());
        return status;
    }

    static String fingerprint(TrendingInsightsResponse insights) {
        return insights.totalViews() + "|" + insights.liveNow() + "|" + insights.summary() + "|"
                + insights.mostWatched().stream()
                .map(video -> video.id() + ":" + video.viewCount())
                .collect(Collectors.joining(",")) + "|"
                + insights.rising().stream().map(VideoResponse::id).map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
