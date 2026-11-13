package com.kino.backend.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the AI analyzer, bound from {@code kino.ai.*} (environment variables {@code AI_*}).
 *
 * @param enabled  use the registered {@link AiTextGenerator} (AI_ENABLED)
 * @param provider free-form provider name for your implementation (AI_PROVIDER)
 * @param apiKey   secret API key for the provider (AI_API_KEY); never commit it
 * @param model    model identifier for the provider (AI_MODEL)
 * @param baseUrl  optional custom endpoint for the provider (AI_BASE_URL)
 * @param cacheTtl how long a computed analysis is reused (AI_CACHE_TTL, ISO-8601, default PT5M)
 */
@ConfigurationProperties(prefix = "kino.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        String apiKey,
        String model,
        String baseUrl,
        Duration cacheTtl) {

    public AiProperties {
        cacheTtl = cacheTtl == null || cacheTtl.isNegative() ? Duration.ofMinutes(5) : cacheTtl;
    }

    /** Avoids printing the API key if this record is ever logged. */
    @Override
    public String toString() {
        return "AiProperties[enabled=" + enabled + ", provider=" + provider + ", model=" + model
                + ", baseUrl=" + baseUrl + ", cacheTtl=" + cacheTtl + ", apiKey="
                + (apiKey == null || apiKey.isBlank() ? "<empty>" : "<set>") + "]";
    }
}
