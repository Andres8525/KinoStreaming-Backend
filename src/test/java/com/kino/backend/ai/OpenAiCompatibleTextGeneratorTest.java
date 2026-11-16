package com.kino.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleTextGeneratorTest {
    private static final TrendingSnapshot SNAPSHOT = new TrendingSnapshot(
            List.of(new VideoResponse(UUID.randomUUID(), "Fenómeno de la semana", "desc", "https://cdn.example.com/v.mp4",
                    null, List.of("Documental"), BigDecimal.ZERO, null,
                    new CreatorSummaryResponse(UUID.randomUUID(), "Jhoan"), 25, Instant.now())),
            List.of(), List.of(new CategoryInsight("Documental", 1, 25)), 25, 1);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiConfiguration.class, OpenAiCompatibleTextGenerator.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void asksGroqWithItsDefaultsAndReturnsTheAnswer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleTextGenerator generator = new OpenAiCompatibleTextGenerator(builder.build(),
                properties("groq", "test-key", "", ""));

        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("llama-3.3-70b-versatile"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content", Matchers.containsString("Fenómeno de la semana")))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":" Hoy todos ven Fenómeno de la semana. "}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(generator.summarizeTrends(SNAPSHOT)).contains("Hoy todos ven Fenómeno de la semana.");
        server.verify();
    }

    @Test
    void sendsTheChatAfterTheSystemMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleTextGenerator generator = new OpenAiCompatibleTextGenerator(builder.build(),
                properties("groq", "test-key", "", ""));

        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[0].content").value("Eres Kino IA"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"Prueba Raíces de agua."}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(generator.chat("Eres Kino IA", List.of(ChatMessage.user("Hola"), ChatMessage.assistant("¡Hola!"))))
                .contains("Prueba Raíces de agua.");
        server.verify();
    }

    @Test
    void usesTheConfiguredModelAndBaseUrl() {
        OpenAiCompatibleTextGenerator generator = new OpenAiCompatibleTextGenerator(RestClient.create(),
                properties("OpenRouter", "k", "mistralai/mistral-7b-instruct:free", "https://proxy.example.com/v1/"));

        assertThat(generator.name()).isEqualTo("openrouter");
        assertThat(generator.model()).isEqualTo("mistralai/mistral-7b-instruct:free");
        assertThat(generator.baseUrl()).isEqualTo("https://proxy.example.com/v1");
    }

    @Test
    void skipsTheCallWithoutAnApiKeyOrWithoutACustomEndpoint() {
        assertThat(new OpenAiCompatibleTextGenerator(RestClient.create(), properties("groq", " ", "", ""))
                .summarizeTrends(SNAPSHOT)).isEmpty();
        assertThat(new OpenAiCompatibleTextGenerator(RestClient.create(), properties("openai-compatible", "k", "", ""))
                .summarizeTrends(SNAPSHOT)).isEmpty();
    }

    @Test
    void returnsNothingWhenTheProviderSendsNoText() {
        assertThat(OpenAiCompatibleTextGenerator.extractText(null)).isNull();
    }

    @Test
    void isRegisteredOnlyForOpenAiCompatibleProviders() {
        contextRunner.withPropertyValues("kino.ai.provider=Groq")
                .run(context -> assertThat(context).hasSingleBean(OpenAiCompatibleTextGenerator.class));
        contextRunner.withPropertyValues("kino.ai.provider=gemini")
                .run(context -> assertThat(context).doesNotHaveBean(OpenAiCompatibleTextGenerator.class));
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OpenAiCompatibleTextGenerator.class));
    }

    private static AiProperties properties(String provider, String apiKey, String model, String baseUrl) {
        return new AiProperties(true, provider, apiKey, model, baseUrl, Duration.ofMinutes(5));
    }
}
