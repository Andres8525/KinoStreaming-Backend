package com.kino.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.model.LiveStreamStatus;
import com.kino.backend.model.User;
import com.kino.backend.model.UserRole;
import com.kino.backend.model.Video;
import com.kino.backend.repository.LiveStreamRepository;
import com.kino.backend.repository.VideoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class TrendingInsightsServiceTest {

    private VideoRepository videoRepository;
    private LiveStreamRepository liveStreamRepository;
    private ObjectProvider<AiTextGenerator> generators;
    private AiTextGenerator generator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        liveStreamRepository = mock(LiveStreamRepository.class);
        generators = mock(ObjectProvider.class);
        generator = mock(AiTextGenerator.class);
        when(generators.getIfAvailable()).thenReturn(generator);
        when(generator.name()).thenReturn("test-ai");
        when(liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE)).thenReturn(List.of());
        when(videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(
                video("Ruta andina", "Documental", 40),
                video("Ritmos urbanos", "Música", 12),
                video("Sin vistas", "Música", 0)));
    }

    @Test
    void usesTheBuiltInSummaryWhenAiIsDisabled() {
        TrendingInsightsResponse response = service(false).insights();

        assertEquals("kino-analytics", response.source());
        assertTrue(response.summary().contains("Ruta andina"));
        assertTrue(response.summary().contains("Documental"));
        assertEquals(2, response.mostWatched().size(), "videos without views are not 'most watched'");
        assertEquals(52, response.totalViews());
        assertEquals("Documental", response.topCategories().get(0).category());
        verify(generator, never()).summarizeTrends(any());
    }

    @Test
    void usesTheAiSummaryWhenEnabled() {
        when(generator.summarizeTrends(any())).thenReturn(Optional.of("  Resumen escrito por la IA.  "));

        TrendingInsightsResponse response = service(true).insights();

        assertEquals("ai", response.source());
        assertEquals("Resumen escrito por la IA.", response.summary());
    }

    @Test
    void fallsBackWhenTheAiProviderFails() {
        when(generator.summarizeTrends(any())).thenThrow(new IllegalStateException("timeout"));

        TrendingInsightsResponse response = service(true).insights();

        assertEquals("kino-analytics", response.source());
        assertTrue(response.summary().startsWith("Lo más visto en Kino ahora es"));
    }

    @Test
    void cachesTheAnalysisUntilItIsEvicted() {
        TrendingInsightsService service = service(false);

        service.insights();
        service.insights();
        verify(videoRepository, times(1)).findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class));

        service.evictCache();
        service.insights();
        verify(videoRepository, times(2)).findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void explainsWhenThereIsNoActivityYet() {
        when(videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());

        assertEquals("Todavía no hay reproducciones suficientes para detectar tendencias.",
                service(false).insights().summary());
    }

    private TrendingInsightsService service(boolean aiEnabled) {
        return new TrendingInsightsService(videoRepository, liveStreamRepository, generators,
                new AiProperties(aiEnabled, "test", "secret", "model", null, Duration.ofMinutes(5)));
    }

    private static Video video(String title, String category, long views) {
        User creator = new User("Ana Ruiz", "ana@example.com", "hash", UserRole.CREATOR);
        Video video = new Video(title, "Descripción", "https://cdn.example.com/v.mp4", null, List.of(category),
                BigDecimal.ZERO, null, creator);
        ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(video, "viewCount", views);
        ReflectionTestUtils.setField(creator, "id", UUID.randomUUID());
        return video;
    }
}
