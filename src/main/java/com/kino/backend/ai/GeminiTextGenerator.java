package com.kino.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "kino.ai", name = "provider", havingValue = "gemini")
public class GeminiTextGenerator implements AiTextGenerator {
    static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiTextGenerator.class);

    private final RestClient restClient;
    private final AiProperties properties;

    @Autowired
    public GeminiTextGenerator(RestClient.Builder builder, AiProperties properties) {
        this(builder.requestFactory(timeouts()).baseUrl(baseUrl(properties)).build(), properties);
    }

    GeminiTextGenerator(RestClient restClient, AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public Optional<String> summarizeTrends(TrendingSnapshot snapshot) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            LOGGER.warn("AI_PROVIDER=gemini but AI_API_KEY is empty; using the built-in summary");
            return Optional.empty();
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", TrendingPrompt.full(snapshot))))),
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024));

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model())
                .header("x-goog-api-key", properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return Optional.ofNullable(extractText(response));
    }

    @Override
    public String model() {
        return properties.model() == null || properties.model().isBlank() ? DEFAULT_MODEL : properties.model().trim();
    }

    static String extractText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        String text = StreamSupport.stream(parts.spliterator(), false)
                .map(part -> part.path("text").asText(""))
                .collect(Collectors.joining())
                .trim();
        return text.isEmpty() ? null : text;
    }

    private static String baseUrl(AiProperties properties) {
        String baseUrl = properties.baseUrl() == null || properties.baseUrl().isBlank()
                ? DEFAULT_BASE_URL : properties.baseUrl().trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}
