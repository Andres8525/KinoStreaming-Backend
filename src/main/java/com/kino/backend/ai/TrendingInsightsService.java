package com.kino.backend.ai;

import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.model.LiveStreamStatus;
import com.kino.backend.repository.LiveStreamRepository;
import com.kino.backend.repository.VideoRepository;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Kino IA" trending analyzer: finds the most watched videos, rising videos and top categories,
 * and asks the configured {@link AiTextGenerator} for a summary. Results are cached for
 * {@code kino.ai.cache-ttl} so a paid AI provider is not called on every dashboard load.
 */
@Service
public class TrendingInsightsService {

    static final String SOURCE_AI = "ai";
    static final String SOURCE_ANALYTICS = "kino-analytics";
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendingInsightsService.class);
    private static final int ANALYZED_VIDEOS = 500;
    private static final int MOST_WATCHED_SIZE = 6;
    private static final int RISING_SIZE = 6;
    private static final int TOP_CATEGORIES_SIZE = 5;
    private static final Duration RISING_WINDOW = Duration.ofDays(7);
    private static final String NO_CATEGORY = "Sin categoría";

    private final VideoRepository videoRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final ObjectProvider<AiTextGenerator> aiTextGenerators;
    private final AiProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final AtomicReference<CachedInsights> cache = new AtomicReference<>();

    public TrendingInsightsService(VideoRepository videoRepository, LiveStreamRepository liveStreamRepository,
                                   ObjectProvider<AiTextGenerator> aiTextGenerators, AiProperties properties) {
        this.videoRepository = videoRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.aiTextGenerators = aiTextGenerators;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public TrendingInsightsResponse insights() {
        Instant now = clock.instant();
        CachedInsights cached = cache.get();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }
        TrendingInsightsResponse response = analyze(now);
        cache.set(new CachedInsights(response, now.plus(properties.cacheTtl())));
        return response;
    }

    /** Drops the cached analysis so the next request recomputes it. */
    public void evictCache() {
        cache.set(null);
    }

    private TrendingInsightsResponse analyze(Instant now) {
        List<VideoResponse> videos = videoRepository
                .findAllByOrderByViewCountDescCreatedAtDesc(PageRequest.of(0, ANALYZED_VIDEOS)).stream()
                .map(VideoResponse::from)
                .toList();
        List<VideoResponse> mostWatched = videos.stream()
                .filter(video -> video.viewCount() > 0)
                .limit(MOST_WATCHED_SIZE)
                .toList();
        Instant risingSince = now.minus(RISING_WINDOW);
        List<VideoResponse> rising = videos.stream()
                .filter(video -> video.createdAt() != null && video.createdAt().isAfter(risingSince))
                .filter(video -> mostWatched.stream().noneMatch(top -> top.id().equals(video.id())))
                .limit(RISING_SIZE)
                .toList();
        List<CategoryInsight> topCategories = topCategories(videos);
        long totalViews = videos.stream().mapToLong(VideoResponse::viewCount).sum();
        int liveNow = liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE).size();

        TrendingSnapshot snapshot = new TrendingSnapshot(mostWatched, rising, topCategories, totalViews, liveNow);
        Optional<String> aiSummary = aiSummary(snapshot);
        return new TrendingInsightsResponse(now, aiSummary.isPresent() ? SOURCE_AI : SOURCE_ANALYTICS,
                aiSummary.orElseGet(() -> builtInSummary(snapshot)), mostWatched, rising, topCategories,
                totalViews, liveNow);
    }

    private Optional<String> aiSummary(TrendingSnapshot snapshot) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        AiTextGenerator generator = aiTextGenerators.getIfAvailable();
        if (generator == null) {
            LOGGER.warn("AI_ENABLED is true but no AiTextGenerator bean is registered; using the built-in summary");
            return Optional.empty();
        }
        try {
            return generator.summarizeTrends(snapshot).map(String::trim).filter(summary -> !summary.isEmpty());
        } catch (RuntimeException exception) {
            LOGGER.warn("AI provider '{}' failed; using the built-in summary: {}", generator.name(),
                    exception.getMessage());
            return Optional.empty();
        }
    }

    private static List<CategoryInsight> topCategories(List<VideoResponse> videos) {
        Map<String, long[]> totals = new LinkedHashMap<>();
        for (VideoResponse video : videos) {
            String category = video.tags().isEmpty() ? NO_CATEGORY : video.tags().get(0);
            long[] values = totals.computeIfAbsent(category, key -> new long[2]);
            values[0]++;
            values[1] += video.viewCount();
        }
        return totals.entrySet().stream()
                .map(entry -> new CategoryInsight(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparingLong(CategoryInsight::totalViews).reversed()
                        .thenComparing(Comparator.comparingLong(CategoryInsight::videoCount).reversed()))
                .limit(TOP_CATEGORIES_SIZE)
                .toList();
    }

    static String builtInSummary(TrendingSnapshot snapshot) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es"));
        StringBuilder summary = new StringBuilder();
        if (snapshot.mostWatched().isEmpty()) {
            summary.append("Todavía no hay reproducciones suficientes para detectar tendencias.");
        } else {
            VideoResponse top = snapshot.mostWatched().get(0);
            summary.append("Lo más visto en Kino ahora es «").append(top.title()).append("» de ")
                    .append(top.creator().displayName()).append(" con ").append(numbers.format(top.viewCount()))
                    .append(top.viewCount() == 1 ? " reproducción." : " reproducciones.");
        }
        if (!snapshot.topCategories().isEmpty() && snapshot.topCategories().get(0).totalViews() > 0) {
            summary.append(" La categoría más popular es ").append(snapshot.topCategories().get(0).category())
                    .append('.');
        }
        if (!snapshot.rising().isEmpty()) {
            summary.append(" En ascenso esta semana: «").append(snapshot.rising().get(0).title()).append("».");
        }
        if (snapshot.liveNow() > 0) {
            summary.append(' ').append(snapshot.liveNow() == 1
                    ? "Hay 1 transmisión en vivo."
                    : "Hay " + snapshot.liveNow() + " transmisiones en vivo.");
        }
        return summary.toString();
    }

    private record CachedInsights(TrendingInsightsResponse response, Instant expiresAt) {
    }
}
