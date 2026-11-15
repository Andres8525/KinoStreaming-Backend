package com.kino.backend.ai;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class OpenAiCompatibleProviders {
    public record Defaults(String baseUrl, String model) {
    }

    public static final String CUSTOM = "openai-compatible";

    private static final Map<String, Defaults> PROVIDERS = Map.of(
            "groq", new Defaults("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
            "openrouter", new Defaults("https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free"),
            "mistral", new Defaults("https://api.mistral.ai/v1", "mistral-small-latest"),
            "github", new Defaults("https://models.github.ai/inference", "openai/gpt-4o-mini"),
            "cerebras", new Defaults("https://api.cerebras.ai/v1", "llama-3.3-70b"),
            "openai", new Defaults("https://api.openai.com/v1", "gpt-4o-mini"),
            CUSTOM, new Defaults("", ""));

    private OpenAiCompatibleProviders() {
    }

    public static boolean supports(String provider) {
        return provider != null && PROVIDERS.containsKey(normalize(provider));
    }

    public static Optional<Defaults> defaults(String provider) {
        return Optional.ofNullable(provider).map(OpenAiCompatibleProviders::normalize).map(PROVIDERS::get);
    }

    public static String normalize(String provider) {
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
