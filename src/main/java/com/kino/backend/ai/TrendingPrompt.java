package com.kino.backend.ai;

import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.VideoResponse;
import java.util.List;
import java.util.stream.Collectors;

public final class TrendingPrompt {
    public static final String SYSTEM_INSTRUCTIONS =
            "Eres el analista de Kino Streaming, una plataforma de cine y video independiente. "
                    + "Respondes siempre en español, con un resumen breve para la portada (máximo 3 frases, "
                    + "tono cercano, sin markdown ni listas) que recomiende qué ver ahora y destaque lo más visto. "
                    + "No inventes títulos ni cifras que no aparezcan en los datos.";

    private TrendingPrompt() {
    }

    public static String data(TrendingSnapshot snapshot) {
        return new StringBuilder()
                .append("Reproducciones totales: ").append(snapshot.totalViews()).append('\n')
                .append("Transmisiones en vivo ahora: ").append(snapshot.liveNow()).append('\n')
                .append("Lo más visto:\n").append(videoLines(snapshot.mostWatched()))
                .append("Novedades en ascenso (últimos 7 días):\n").append(videoLines(snapshot.rising()))
                .append("Categorías con más vistas:\n").append(categoryLines(snapshot.topCategories()))
                .toString();
    }

    public static String full(TrendingSnapshot snapshot) {
        return SYSTEM_INSTRUCTIONS + "\n\n" + data(snapshot);
    }

    private static String videoLines(List<VideoResponse> videos) {
        if (videos.isEmpty()) {
            return "- (ninguno)\n";
        }
        return videos.stream()
                .map(video -> "- \"" + video.title() + "\" de " + video.creator().displayName() + ", "
                        + video.viewCount() + " vistas" + (video.tags().isEmpty() ? "" : ", " + video.tags().get(0)))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String categoryLines(List<CategoryInsight> categories) {
        if (categories.isEmpty()) {
            return "- (ninguna)\n";
        }
        return categories.stream()
                .map(category -> "- " + category.category() + ": " + category.totalViews() + " vistas en "
                        + category.videoCount() + " videos")
                .collect(Collectors.joining("\n", "", "\n"));
    }
}
