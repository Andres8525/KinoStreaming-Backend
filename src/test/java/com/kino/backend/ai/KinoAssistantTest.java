package com.kino.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kino.backend.dto.AiChatResponse;
import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.ChatMessage;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.model.User;
import com.kino.backend.model.UserRole;
import com.kino.backend.model.Video;
import com.kino.backend.repository.VideoRepository;
import com.kino.backend.service.FieldValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class KinoAssistantTest {
    private final Video sintel = video("Sintel: la búsqueda", "Animación", 7350);
    private final Video azotea = video("Sesiones en la azotea", "Música", 6200);
    private final Video raices = video("Raíces de agua", "Documental", 4800);

    private TrendingInsightsService insightsService;
    private AiChatGateway gateway;
    private KinoAssistant assistant;

    @BeforeEach
    void setUp() {
        insightsService = mock(TrendingInsightsService.class);
        gateway = mock(AiChatGateway.class);
        VideoRepository videoRepository = mock(VideoRepository.class);
        when(videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(sintel, azotea, raices));
        when(insightsService.snapshot()).thenReturn(new TrendingSnapshot(
                List.of(VideoResponse.from(sintel), VideoResponse.from(azotea), VideoResponse.from(raices)),
                List.of(VideoResponse.from(raices)),
                List.of(new CategoryInsight("Animación", 1, 7350), new CategoryInsight("Música", 1, 6200)),
                18350, 0));
        when(gateway.chat(anyString(), anyList())).thenReturn(Optional.empty());
        assistant = new KinoAssistant(insightsService, videoRepository, gateway);
    }

    @Test
    void returnsTheAiReplyWithTheCatalogVideosItMentions() {
        when(gateway.chat(anyString(), anyList()))
                .thenReturn(Optional.of("  Te recomiendo «Raíces de agua» y «Sintel: la búsqueda».  "));

        AiChatResponse response = assistant.reply(List.of(ChatMessage.user("¿Qué veo hoy?")));

        assertThat(response.source()).isEqualTo("ai");
        assertThat(response.reply()).contains("Te recomiendo «Raíces de agua»");
        assertThat(response.suggestions()).extracting(VideoResponse::title)
                .containsExactly("Sintel: la búsqueda", "Raíces de agua");
    }

    @Test
    void sendsTheCatalogAndStatisticsToTheModel() {
        assistant.reply(List.of(ChatMessage.user("Hola")));

        org.mockito.Mockito.verify(gateway).chat(org.mockito.ArgumentMatchers.argThat(instructions ->
                instructions.contains("Kino IA") && instructions.contains("Sesiones en la azotea | ")
                        && instructions.contains("Reproducciones totales: 18350")), anyList());
    }

    @Test
    void answersWithTheMostWatchedVideosWithoutAi() {
        AiChatResponse response = assistant.reply(List.of(ChatMessage.user("¿Qué es lo más visto?")));

        assertThat(response.source()).isEqualTo("kino-analytics");
        assertThat(response.reply()).contains("lo más visto").contains("«Sintel: la búsqueda» de Ana Ruiz (7.350 vistas)");
        assertThat(response.suggestions()).hasSize(3);
    }

    @Test
    void recommendsVideosOfTheCategoryInTheQuestion() {
        AiChatResponse response = assistant.reply(List.of(ChatMessage.user("Recomiéndame documentales")));

        assertThat(response.reply()).startsWith("En Documental te recomiendo");
        assertThat(response.suggestions()).extracting(VideoResponse::title).containsExactly("Raíces de agua");
    }

    @Test
    void mixesPopularAndNewVideosForGeneralRecommendations() {
        AiChatResponse response = assistant.reply(List.of(ChatMessage.user("Recomiéndame algo")));

        assertThat(response.suggestions()).extracting(VideoResponse::title)
                .containsExactly("Sintel: la búsqueda", "Raíces de agua", "Sesiones en la azotea");
    }

    @Test
    void explainsThatNobodyIsLive() {
        AiChatResponse response = assistant.reply(List.of(ChatMessage.user("¿Hay alguien en vivo?")));

        assertThat(response.reply()).contains("no hay transmisiones en vivo");
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    void keepsOnlyTheRecentHistoryStartingWithAQuestion() {
        List<ChatMessage> conversation = IntStream.range(0, 20)
                .mapToObj(index -> index % 2 == 0 ? ChatMessage.user("Pregunta " + index)
                        : ChatMessage.assistant("Respuesta " + index))
                .toList();

        List<ChatMessage> history = assistant.prepare(new java.util.ArrayList<>(conversation.subList(0, 19)));

        assertThat(history).hasSizeLessThanOrEqualTo(KinoAssistant.MAX_HISTORY);
        assertThat(history.get(0).fromUser()).isTrue();
        assertThat(history.get(history.size() - 1).content()).isEqualTo("Pregunta 18");
    }

    @Test
    void rejectsAConversationThatDoesNotEndWithAQuestion() {
        assertThatThrownBy(() -> assistant.prepare(List.of(ChatMessage.user("Hola"), ChatMessage.assistant("¡Hola!"))))
                .isInstanceOf(FieldValidationException.class);
        assertThatThrownBy(() -> assistant.prepare(List.of())).isInstanceOf(FieldValidationException.class);
    }

    private static Video video(String title, String category, long views) {
        User creator = new User("Ana Ruiz", "ana@example.com", "hash", UserRole.CREATOR);
        Video video = new Video(title, "Descripción", "https://cdn.example.com/v.mp4", null, List.of(category),
                BigDecimal.ZERO, null, creator);
        ReflectionTestUtils.setField(video, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(video, "viewCount", views);
        ReflectionTestUtils.setField(video, "createdAt", Instant.now());
        ReflectionTestUtils.setField(creator, "id", UUID.randomUUID());
        return video;
    }
}
