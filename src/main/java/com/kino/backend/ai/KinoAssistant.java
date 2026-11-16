package com.kino.backend.ai;

import com.kino.backend.dto.AiChatResponse;
import com.kino.backend.dto.ChatMessage;
import com.kino.backend.dto.VideoResponse;
import com.kino.backend.repository.VideoRepository;
import com.kino.backend.service.FieldValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KinoAssistant {
    static final String SOURCE_AI = "ai";
    static final String SOURCE_ANALYTICS = "kino-analytics";
    static final int MAX_HISTORY = 12;
    static final int MAX_SUGGESTIONS = 4;
    private static final int CATALOG_SIZE = 60;

    private final TrendingInsightsService insightsService;
    private final VideoRepository videoRepository;
    private final AiChatGateway aiChatGateway;
    private final Clock clock = Clock.systemUTC();

    public KinoAssistant(TrendingInsightsService insightsService, VideoRepository videoRepository,
                         AiChatGateway aiChatGateway) {
        this.insightsService = insightsService;
        this.videoRepository = videoRepository;
        this.aiChatGateway = aiChatGateway;
    }

    public List<ChatMessage> prepare(List<ChatMessage> conversation) {
        if (conversation == null || conversation.isEmpty()) {
            throw new FieldValidationException("messages", "Escribe una pregunta para Kino IA");
        }
        List<ChatMessage> recent = conversation.subList(Math.max(0, conversation.size() - MAX_HISTORY),
                conversation.size());
        int firstUserMessage = 0;
        while (firstUserMessage < recent.size() && !recent.get(firstUserMessage).fromUser()) {
            firstUserMessage++;
        }
        List<ChatMessage> history = List.copyOf(recent.subList(firstUserMessage, recent.size()));
        if (history.isEmpty() || !history.get(history.size() - 1).fromUser()) {
            throw new FieldValidationException("messages", "El último mensaje debe ser una pregunta tuya");
        }
        return history;
    }

    @Transactional(readOnly = true)
    public AiChatResponse reply(List<ChatMessage> conversation) {
        List<ChatMessage> history = prepare(conversation);
        TrendingSnapshot snapshot = insightsService.snapshot();
        List<VideoResponse> catalog = catalog();
        Optional<String> aiReply = aiChatGateway.chat(AssistantPrompt.instructions(snapshot, catalog), history);
        if (aiReply.isPresent()) {
            return new AiChatResponse(aiReply.get(), SOURCE_AI, mentionedVideos(aiReply.get(), catalog),
                    clock.instant());
        }
        return builtInReply(lastQuestion(history), snapshot, catalog);
    }

    @Transactional(readOnly = true)
    public AiChatResponse builtInReply(List<ChatMessage> conversation) {
        return builtInReply(lastQuestion(prepare(conversation)), insightsService.snapshot(), catalog());
    }

    private AiChatResponse builtInReply(String question, TrendingSnapshot snapshot, List<VideoResponse> catalog) {
        String normalized = KinoText.normalize(question);
        Optional<String> category = mentionedCategory(normalized, catalog);
        Answer answer;
        if (KinoText.containsAny(normalized, "vivo", "live", "transmision", "directo")) {
            answer = liveAnswer(snapshot);
        } else if (category.isPresent()) {
            answer = categoryAnswer(category.get(), catalog);
        } else if (KinoText.containsAny(normalized, "nuevo", "novedad", "reciente", "ascenso", "semana", "estreno")) {
            answer = listAnswer("Esto es lo nuevo que está creciendo en Kino esta semana:", snapshot.rising(),
                    "Esta semana todavía no hay novedades en ascenso. Mientras tanto, mira lo más visto:",
                    snapshot.mostWatched());
        } else if (KinoText.containsAny(normalized, "visto", "popular", "top", "ranking", "tendencia", "exito")) {
            answer = listAnswer("Esto es lo más visto en Kino ahora mismo:", snapshot.mostWatched(),
                    "Todavía no hay reproducciones suficientes para armar el ranking. Estos son algunos videos:",
                    catalog);
        } else if (KinoText.containsAny(normalized, "recomiend", "que veo", "que ver", "sugier", "sugerencia",
                "aburr", "algo bueno")) {
            answer = recommendationAnswer(snapshot, catalog);
        } else if (KinoText.containsAny(normalized, "hola", "buenas", "hey", "saludos")) {
            answer = new Answer("¡Hola! Soy Kino IA. Puedo contarte qué es lo más visto, recomendarte videos por "
                    + "categoría o avisarte si hay transmisiones en vivo. ¿Qué te apetece ver?",
                    limit(snapshot.mostWatched()));
        } else {
            answer = new Answer("Puedo ayudarte a descubrir videos en Kino: pregúntame qué es lo más visto, pídeme "
                    + "una recomendación por categoría (por ejemplo «Recomiéndame un documental») o pregúntame si "
                    + "hay alguien transmitiendo en vivo.", limit(snapshot.mostWatched()));
        }
        return new AiChatResponse(answer.text(), SOURCE_ANALYTICS, answer.videos(), clock.instant());
    }

    private static Answer liveAnswer(TrendingSnapshot snapshot) {
        if (snapshot.liveNow() == 0) {
            return new Answer("En este momento no hay transmisiones en vivo. Puedes abrir tu cámara y empezar la tuya "
                    + "desde Creator Studio, en la pestaña «En vivo».", List.of());
        }
        return new Answer(snapshot.liveNow() == 1
                ? "Ahora mismo hay 1 transmisión en vivo. Entra en «En vivo ahora» en la portada para verla."
                : "Ahora mismo hay " + snapshot.liveNow() + " transmisiones en vivo. Entra en «En vivo ahora» en la "
                + "portada para verlas.", List.of());
    }

    private static Answer categoryAnswer(String category, List<VideoResponse> catalog) {
        List<VideoResponse> videos = catalog.stream()
                .filter(video -> video.tags().stream().anyMatch(tag -> tag.equalsIgnoreCase(category)))
                .sorted(Comparator.comparingLong(VideoResponse::viewCount).reversed())
                .limit(MAX_SUGGESTIONS)
                .toList();
        if (videos.isEmpty()) {
            return new Answer("Todavía no hay videos de " + category + " en Kino. Puedes publicar el primero desde "
                    + "Creator Studio.", List.of());
        }
        return new Answer("En " + category + " te recomiendo:\n" + lines(videos), videos);
    }

    private static Answer listAnswer(String intro, List<VideoResponse> videos, String emptyIntro,
                                     List<VideoResponse> alternatives) {
        List<VideoResponse> selection = limit(videos);
        if (!selection.isEmpty()) {
            return new Answer(intro + "\n" + lines(selection), selection);
        }
        List<VideoResponse> fallback = limit(alternatives);
        if (fallback.isEmpty()) {
            return new Answer("Todavía no hay videos publicados en Kino. Vuelve pronto o publica el primero desde "
                    + "Creator Studio.", List.of());
        }
        return new Answer(emptyIntro + "\n" + lines(fallback), fallback);
    }

    private static Answer recommendationAnswer(TrendingSnapshot snapshot, List<VideoResponse> catalog) {
        Set<VideoResponse> picks = new LinkedHashSet<>();
        snapshot.mostWatched().stream().findFirst().ifPresent(picks::add);
        snapshot.rising().stream().findFirst().ifPresent(picks::add);
        snapshot.topCategories().stream().skip(1).findFirst().ifPresent(category -> catalog.stream()
                .filter(video -> video.tags().contains(category.category()))
                .findFirst()
                .ifPresent(picks::add));
        snapshot.mostWatched().stream().skip(1).forEach(video -> {
            if (picks.size() < MAX_SUGGESTIONS) {
                picks.add(video);
            }
        });
        if (picks.isEmpty()) {
            return listAnswer("", List.of(), "Estos son algunos videos publicados:", catalog);
        }
        List<VideoResponse> videos = new ArrayList<>(picks).stream().limit(MAX_SUGGESTIONS).toList();
        return new Answer("Te recomiendo empezar por estos:\n" + lines(videos)
                + "\nSi prefieres algo concreto, pídeme una categoría como Música o Documental.", videos);
    }

    private static Optional<String> mentionedCategory(String normalizedQuestion, List<VideoResponse> catalog) {
        return catalog.stream()
                .flatMap(video -> video.tags().stream())
                .distinct()
                .filter(tag -> !tag.isBlank() && normalizedQuestion.contains(KinoText.normalize(tag)))
                .findFirst();
    }

    static List<VideoResponse> mentionedVideos(String reply, List<VideoResponse> catalog) {
        String normalizedReply = KinoText.normalize(reply);
        return catalog.stream()
                .filter(video -> video.title() != null && video.title().length() > 2)
                .filter(video -> normalizedReply.contains(KinoText.normalize(video.title())))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private static String lastQuestion(List<ChatMessage> history) {
        return history.get(history.size() - 1).content();
    }

    private static String lines(List<VideoResponse> videos) {
        return videos.stream()
                .map(video -> "- «" + video.title() + "» de " + video.creator().displayName() + " ("
                        + KinoText.views(video.viewCount()) + ")")
                .collect(Collectors.joining("\n"));
    }

    private static List<VideoResponse> limit(List<VideoResponse> videos) {
        return videos.stream().limit(MAX_SUGGESTIONS).toList();
    }

    private List<VideoResponse> catalog() {
        return videoRepository.findAllByOrderByViewCountDescCreatedAtDesc(PageRequest.of(0, CATALOG_SIZE)).stream()
                .map(VideoResponse::from)
                .toList();
    }

    private record Answer(String text, List<VideoResponse> videos) {
    }
}
