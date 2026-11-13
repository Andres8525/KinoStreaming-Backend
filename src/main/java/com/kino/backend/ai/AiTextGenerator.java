package com.kino.backend.ai;

import java.util.Optional;

/**
 * Integration point for the AI provider that writes the "Kino IA" trending summary.
 *
 * <p>To connect an AI API, add a Spring bean ({@code @Component}) implementing this interface that
 * sends the snapshot (most watched videos, rising videos, top categories) to the provider and returns
 * a short summary in Spanish. Read its settings from {@link AiProperties} (environment variables
 * {@code AI_PROVIDER}, {@code AI_API_KEY}, {@code AI_MODEL}, {@code AI_BASE_URL}) and set
 * {@code AI_ENABLED=true}.
 *
 * <p>Returning {@link Optional#empty()} or throwing makes Kino fall back to its built-in summary,
 * so the dashboard keeps working when the provider is slow, misconfigured or down.
 */
public interface AiTextGenerator {

    /** Short provider name used in logs. */
    String name();

    Optional<String> summarizeTrends(TrendingSnapshot snapshot);
}
