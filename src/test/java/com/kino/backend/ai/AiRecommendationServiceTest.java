package com.kino.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kino.backend.dto.AiRecommendationsResponse;
import com.kino.backend.dto.VideoRecommendation;
import com.kino.backend.model.User;
import com.kino.backend.model.UserRole;
import com.kino.backend.model.Video;
import com.kino.backend.repository.VideoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class AiRecommendationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    private VideoRepository videoRepository;
    private AiChatGateway gateway;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        gateway = mock(AiChatGateway.class);
        when(gateway.chat(anyString(), anyList())).thenReturn(Optional.empty());
        when(videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(
                video("Sintel: la búsqueda", "Animación", 7350, Duration.ofDays(60)),
                video("Raíces de agua", "Documental", 4800, Duration.ofDays(40)),
                video("Últimos tranvías", "Documental", 300, Duration.ofDays(2)),
                video("Guardianes del páramo", "Documental", 10, Duration.ofDays(90))));
    }

    @Test
    void ranksPopularVideosAndNewReleasesWithAReason() {
        AiRecommendationsResponse response = service().recommend(null);

        assertThat(response.category()).isEqualTo("Todos");
        assertThat(response.source()).isEqualTo("kino-analytics");
        assertThat(response.recommendations()).extracting(item -> item.video().title())
                .containsExactly("Sintel: la búsqueda", "Raíces de agua", "Últimos tranvías", "Guardianes del páramo");
        assertThat(response.recommendations().get(0).reason()).isEqualTo("Lo más visto en Kino (7.350 vistas)");
        assertThat(response.recommendations().get(2).reason()).isEqualTo("Novedad de esta semana");
        assertThat(response.summary()).contains("Empieza por «Sintel: la búsqueda»");
    }

    @Test
    void filtersByCategoryIgnoringAccentsAndCase() {
        AiRecommendationsResponse response = service().recommend("documental");

        assertThat(response.category()).isEqualTo("Documental");

        assertThat(response.recommendations()).extracting(VideoRecommendation::video)
                .allMatch(video -> video.tags().contains("Documental"));
        assertThat(response.recommendations().get(0).reason()).isEqualTo("Lo más visto en Documental (4.800 vistas)");
        assertThat(response.recommendations().get(2).reason()).isEqualTo("Una joya por descubrir en Documental");
    }

    @Test
    void explainsWhenTheCategoryHasNoVideos() {
        AiRecommendationsResponse response = service().recommend("Terror");

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.summary()).isEqualTo("Todavía no hay videos de Terror para recomendar.");
    }

    @Test
    void usesTheAiSummaryAndCachesItPerCategory() {
        when(gateway.chat(anyString(), anyList())).thenReturn(Optional.of("Dos joyas para hoy."));
        AiRecommendationService service = service();

        AiRecommendationsResponse first = service.recommend("Documental");
        service.recommend("documental");

        assertThat(first.source()).isEqualTo("ai");
        assertThat(first.summary()).isEqualTo("Dos joyas para hoy.");
        verify(gateway, times(1)).chat(anyString(), anyList());
    }

    private AiRecommendationService service() {
        return new AiRecommendationService(videoRepository, gateway,
                new AiProperties(true, "test", "key", "model", null, Duration.ofMinutes(5)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Video video(String title, String category, long views, Duration age) {
        User creator = new User("Ana Ruiz", "ana@example.com", "hash", UserRole.CREATOR);
        Video video = new Video(title, "Descripción", "https://cdn.example.com/v.mp4", null, List.of(category),
                BigDecimal.ZERO, null, creator);
        ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(video, "viewCount", views);
        ReflectionTestUtils.setField(video, "createdAt", NOW.minus(age));
        ReflectionTestUtils.setField(creator, "id", UUID.randomUUID());
        return video;
    }
}
