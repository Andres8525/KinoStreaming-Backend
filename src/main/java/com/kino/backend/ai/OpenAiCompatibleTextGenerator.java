package com.kino.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("T(com.kino.backend.ai.OpenAiCompatibleProviders).supports('${kino.ai.provider:}')")
public class OpenAiCompatibleTextGenerator implements AiTextGenerator {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleTextGenerator.class);

    private final RestClient restClient;
    private final AiProperties properties;
    private final OpenAiCompatibleProviders.Defaults defaults;

    @Autowired
    public OpenAiCompatibleTextGenerator(RestClient.Builder builder, AiProperties properties) {
        this(builder.requestFactory(timeouts()).build(), properties);
    }

    OpenAiCompatibleTextGenerator(RestClient restClient, AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.defaults = OpenAiCompatibleProviders.defaults(properties.provider())
                .orElse(new OpenAiCompatibleProviders.Defaults("", ""));
    }

    @Override
    public String name() {
        return OpenAiCompatibleProviders.normalize(properties.provider());
    }

    @Override
    public String model() {
        return properties.model() == null || properties.model().isBlank() ? defaults.model() : properties.model().trim();
    }

    @Override
    public Optional<String> summarizeTrends(TrendingSnapshot snapshot) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            LOGGER.warn("AI_PROVIDER={} but AI_API_KEY is empty; using the built-in summary", name());
            return Optional.empty();
        }
        if (baseUrl().isEmpty() || model().isEmpty()) {
            LOGGER.warn("AI_PROVIDER={} needs AI_BASE_URL and AI_MODEL; using the built-in summary", name());
            return Optional.empty();
        }
        Map<String, Object> body = Map.of(
                "model", model(),
                "messages", List.of(
                        Map.of("role", "system", "content", TrendingPrompt.SYSTEM_INSTRUCTIONS),
                        Map.of("role", "user", "content", TrendingPrompt.data(snapshot))),
                "temperature", 0.7,
                "max_tokens", 400);

        JsonNode response = restClient.post()
                .uri(baseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + properties.apiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return Optional.ofNullable(extractText(response));
    }

    static String extractText(JsonNode response) {
        if (response == null) {
            return null;
        }
        String text = response.path("choices").path(0).path("message").path("content").asText("").trim();
        return text.isEmpty() ? null : text;
    }

    String baseUrl() {
        String baseUrl = properties.baseUrl() == null || properties.baseUrl().isBlank()
                ? defaults.baseUrl() : properties.baseUrl().trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}
