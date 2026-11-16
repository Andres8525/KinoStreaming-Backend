package com.kino.backend.ai;

import com.kino.backend.dto.AiRecommendationsResponse;
import com.kino.backend.dto.ChatMessage;
import com.kino.backend.dto.VideoRecommendation;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.repository.VideoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiRecommendationService {
    static final String ALL_CATEGORIES = "Todos";
    static final int RECOMMENDATIONS_SIZE = 6;
    private static final int ANALYZED_VIDEOS = 300;
    private static final int MAX_CACHED_CATEGORIES = 50;
    private static final Duration NEW_RELEASE_WINDOW = Duration.ofDays(7);
    private static final Duration RECENT_WINDOW = Duration.ofDays(30);

    private final VideoRepository videoRepository;
    private final AiChatGateway aiChatGateway;
    private final AiProperties properties;
    private final Clock clock;
    private final Map<String, CachedRecommendations> cache = new ConcurrentHashMap<>();

    @Autowired
    public AiRecommendationService(VideoRepository videoRepository, AiChatGateway aiChatGateway,
                                   AiProperties properties) {
        this(videoRepository, aiChatGateway, properties, Clock.systemUTC());
    }

    AiRecommendationService(VideoRepository videoRepository, AiChatGateway aiChatGateway, AiProperties properties,
                            Clock clock) {
        this.videoRepository = videoRepository;
        this.aiChatGateway = aiChatGateway;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiRecommendationsResponse recommend(String requestedCategory) {
        String category = requestedCategory == null || requestedCategory.isBlank()
                || ALL_CATEGORIES.equalsIgnoreCase(requestedCategory.trim()) ? ALL_CATEGORIES : requestedCategory.trim();
        String key = KinoText.normalize(category);
        Instant now = clock.instant();
        CachedRecommendations cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }
        AiRecommendationsResponse response = compute(category, now);
        if (cache.size() >= MAX_CACHED_CATEGORIES) {
            cache.clear();
        }
        cache.put(key, new CachedRecommendations(response, now.plus(properties.cacheTtl())));
        return response;
    }

    private AiRecommendationsResponse compute(String requestedCategory, Instant now) {
        String normalizedCategory = KinoText.normalize(requestedCategory);
        List<VideoResponse> candidates = videoRepository
                .findAllByOrderByViewCountDescCreatedAtDesc(PageRequest.of(0, ANALYZED_VIDEOS)).stream()
                .map(VideoResponse::from)
                .filter(video -> ALL_CATEGORIES.equals(requestedCategory) || video.tags().stream()
                        .anyMatch(tag -> KinoText.normalize(tag).equals(normalizedCategory)))
                .toList();
        String category = candidates.stream()
                .flatMap(video -> video.tags().stream())
                .filter(tag -> KinoText.normalize(tag).equals(normalizedCategory))
                .findFirst()
                .orElse(requestedCategory);
        long topViews = candidates.stream().mapToLong(VideoResponse::viewCount).max().orElse(0);
        List<VideoRecommendation> recommendations = candidates.stream()
                .map(video -> new VideoRecommendation(video, reason(video, category, topViews, now),
                        Math.round(score(video, now) * 100) / 100.0))
                .sorted(Comparator.comparingDouble(VideoRecommendation::score).reversed())
                .limit(RECOMMENDATIONS_SIZE)
                .toList();

        Optional<String> aiSummary = recommendations.isEmpty() ? Optional.empty() : aiSummary(category, recommendations);
        return new AiRecommendationsResponse(category, aiSummary.isPresent() ? "ai" : "kino-analytics",
                aiSummary.orElseGet(() -> builtInSummary(category, recommendations)), recommendations, now);
    }

    static double score(VideoResponse video, Instant now) {
        double popularity = Math.log1p(Math.max(0, video.viewCount()));
        if (video.createdAt() == null) {
            return popularity;
        }
        Duration age = Duration.between(video.createdAt(), now);
        if (age.compareTo(NEW_RELEASE_WINDOW) <= 0) {
            return popularity + 2.5;
        }
        return age.compareTo(RECENT_WINDOW) <= 0 ? popularity + 1 : popularity;
    }

    private static String reason(VideoResponse video, String category, long topViews, Instant now) {
        String scope = ALL_CATEGORIES.equals(category) ? "Kino" : category;
        if (video.viewCount() > 0 && video.viewCount() == topViews) {
            return "Lo más visto en " + scope + " (" + KinoText.views(video.viewCount()) + ")";
        }
        if (video.createdAt() != null && Duration.between(video.createdAt(), now).compareTo(NEW_RELEASE_WINDOW) <= 0) {
            return "Novedad de esta semana";
        }
        if (video.viewCount() * 2 >= topViews && video.viewCount() > 0) {
            return "Muy popular en " + scope + " (" + KinoText.views(video.viewCount()) + ")";
        }
        return "Una joya por descubrir en " + scope;
    }

    private Optional<String> aiSummary(String category, List<VideoRecommendation> recommendations) {
        String videos = recommendations.stream()
                .map(item -> "- «" + item.video().title() + "» de " + item.video().creator().displayName() + ", "
                        + item.video().viewCount() + " vistas: " + item.reason())
                .collect(Collectors.joining("\n"));
        String question = "Explica en máximo 2 frases, para la portada de Kino, por qué recomiendas estos videos"
                + (ALL_CATEGORIES.equals(category) ? "" : " de " + category) + ":\n" + videos;
        return aiChatGateway.chat(AssistantPrompt.ROLE, List.of(ChatMessage.user(question)));
    }

    static String builtInSummary(String category, List<VideoRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return ALL_CATEGORIES.equals(category)
                    ? "Todavía no hay videos publicados para recomendar."
                    : "Todavía no hay videos de " + category + " para recomendar.";
        }
        String scope = ALL_CATEGORIES.equals(category) ? "todo Kino" : category;
        return "Seleccionamos " + recommendations.size() + (recommendations.size() == 1 ? " video" : " videos")
                + " de " + scope + " combinando popularidad y novedades. Empieza por «"
                + recommendations.get(0).video().title() + "».";
    }

    public void evictCache() {
        cache.clear();
    }

    private record CachedRecommendations(AiRecommendationsResponse response, Instant expiresAt) {
    }
}
