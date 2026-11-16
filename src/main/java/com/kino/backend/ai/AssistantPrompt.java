package com.kino.backend.ai;

import com.kino.backend.dto.VideoResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public final class AssistantPrompt {
    public static final String ROLE =
            "Eres Kino IA, el asistente de Kino Streaming, una plataforma de cine y video independiente. "
                    + "Respondes siempre en español, con tono cercano y breve: máximo 4 frases o una lista corta "
                    + "de hasta 5 elementos que empiecen con guion. No uses markdown con asteriscos ni encabezados. "
                    + "Recomienda solo videos del catálogo de abajo y escribe sus títulos exactamente como aparecen, "
                    + "entre comillas «». Si te preguntan algo que no tiene relación con Kino o que no está en el "
                    + "catálogo, dilo con amabilidad y ofrece alternativas del catálogo. No inventes títulos, cifras "
                    + "ni transmisiones en vivo.";

    private AssistantPrompt() {
    }

    public static String instructions(TrendingSnapshot snapshot, List<VideoResponse> catalog) {
        return ROLE + "\n\nDatos actuales de Kino:\n" + TrendingPrompt.data(snapshot)
                + "Catálogo disponible (título | creador | categoría | vistas | publicado):\n" + catalogLines(catalog);
    }

    private static String catalogLines(List<VideoResponse> catalog) {
        if (catalog.isEmpty()) {
            return "- (todavía no hay videos publicados)\n";
        }
        return catalog.stream()
                .map(video -> "- " + video.title() + " | " + video.creator().displayName() + " | "
                        + (video.tags().isEmpty() ? "Sin categoría" : video.tags().get(0)) + " | "
                        + video.viewCount() + " | " + publishedOn(video))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String publishedOn(VideoResponse video) {
        return video.createdAt() == null ? "-" : LocalDate.ofInstant(video.createdAt(), ZoneOffset.UTC).toString();
    }
}
