package com.kino.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kino.backend.dto.CategoryInsight;
import com.kino.backend.dto.ChatMessage;
import com.kino.backend.dto.CreatorSummaryResponse;
import com.kino.backend.dto.VideoResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiTextGeneratorTest {

    private static final TrendingSnapshot SNAPSHOT = new TrendingSnapshot(
            List.of(new VideoResponse(UUID.randomUUID(), "Fenómeno de la semana", "desc", "https://cdn.example.com/v.mp4",
                    null, List.of("Documental"), BigDecimal.ZERO, null,
                    new CreatorSummaryResponse(UUID.randomUUID(), "Jhoan"), 25, Instant.now())),
            List.of(), List.of(new CategoryInsight("Documental", 1, 25)), 25, 1);

    @Test
    void asksGeminiForTheSummaryAndReturnsItsText() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeminiTextGenerator.DEFAULT_BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiTextGenerator generator = new GeminiTextGenerator(builder.build(), properties("test-key", ""));

        server.expect(requestTo(GeminiTextGenerator.DEFAULT_BASE_URL + "/models/gemini-flash-lite-latest:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text", Matchers.containsString("Fenómeno de la semana")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Hoy todos ven "},{"text":"Fenómeno de la semana."}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("Hoy todos ven Fenómeno de la semana."), generator.summarizeTrends(SNAPSHOT));
        server.verify();
    }

    @Test
    void usesTheConfiguredModel() {
        assertEquals("gemini-2.5-pro", new GeminiTextGenerator(RestClient.create(), properties("k", "gemini-2.5-pro")).model());
    }

    @Test
    void returnsNothingWithoutAnApiKeySoKinoUsesItsOwnSummary() {
        GeminiTextGenerator generator = new GeminiTextGenerator(RestClient.create(), properties(" ", ""));

        assertTrue(generator.summarizeTrends(SNAPSHOT).isEmpty());
    }

    @Test
    void returnsNothingWhenGeminiSendsNoText() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeminiTextGenerator.DEFAULT_BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GeminiTextGenerator.DEFAULT_BASE_URL + "/models/gemini-flash-lite-latest:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", MediaType.APPLICATION_JSON));

        assertTrue(new GeminiTextGenerator(builder.build(), properties("test-key", "")).summarizeTrends(SNAPSHOT).isEmpty());
    }

    @Test
    void sendsTheConversationWithTheSystemInstructionsAndModelRoles() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeminiTextGenerator.DEFAULT_BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiTextGenerator generator = new GeminiTextGenerator(builder.build(), properties("test-key", ""));

        server.expect(requestTo(GeminiTextGenerator.DEFAULT_BASE_URL + "/models/gemini-flash-lite-latest:generateContent"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("Eres Kino IA"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[1].role").value("model"))
                .andExpect(jsonPath("$.contents[2].parts[0].text").value("¿Y de música?"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Mira Sesiones en la azotea."}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("Mira Sesiones en la azotea."), generator.chat("Eres Kino IA", List.of(
                ChatMessage.user("¿Qué veo hoy?"), ChatMessage.assistant("Te recomiendo Sintel."),
                ChatMessage.user("¿Y de música?"))));
        server.verify();
    }

    @Test
    void doesNotChatWithoutAnApiKey() {
        GeminiTextGenerator generator = new GeminiTextGenerator(RestClient.create(), properties("", ""));

        assertTrue(generator.chat("Eres Kino IA", List.of(ChatMessage.user("Hola"))).isEmpty());
    }

    private static AiProperties properties(String apiKey, String model) {
        return new AiProperties(true, "gemini", apiKey, model, "", Duration.ofMinutes(5));
    }
}
