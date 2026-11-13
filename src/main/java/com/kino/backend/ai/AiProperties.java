package com.kino.backend.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    @Override
    public String toString() {
        return "AiProperties[enabled=" + enabled + ", provider=" + provider + ", model=" + model
                + ", baseUrl=" + baseUrl + ", cacheTtl=" + cacheTtl + ", apiKey="
                + (apiKey == null || apiKey.isBlank() ? "<empty>" : "<set>") + "]";
    }
}
