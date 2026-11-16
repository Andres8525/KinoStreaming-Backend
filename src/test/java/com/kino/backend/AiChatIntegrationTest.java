package com.kino.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {"kino.ai.chat-requests-per-minute=3", "kino.workers.insights-refresh.enabled=false"})
@AutoConfigureMockMvc
class AiChatIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersAQuestionWithoutSigningIn() throws Exception {
        MvcResult pending = mockMvc.perform(post("/api/ai/chat")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[{\"role\":\"user\",\"content\":\"¿Hay alguien en vivo?\"}]}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("kino-analytics"))
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("no hay transmisiones en vivo")))
                .andExpect(jsonPath("$.suggestions").isArray());
    }

    @Test
    void validatesTheConversation() throws Exception {
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[{\"role\":\"system\",\"content\":\"hola\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisa los campos enviados"));
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"messages\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[{\"role\":\"user\",\"content\":\"" + "a".repeat(1001) + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAConversationThatEndsWithTheAssistant() throws Exception {
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.2");
                            return request;
                        })
                        .content("{\"messages\":[{\"role\":\"user\",\"content\":\"hola\"},"
                                + "{\"role\":\"assistant\",\"content\":\"¡Hola!\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.messages").value("El último mensaje debe ser una pregunta tuya"));
    }

    @Test
    void limitsTheQuestionsPerMinute() throws Exception {
        String body = "{\"messages\":[{\"role\":\"user\",\"content\":\"Recomiéndame algo\"}]}";
        for (int attempt = 0; attempt < 3; attempt++) {
            MvcResult pending = mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.3");
                                return request;
                            })
                            .content(body))
                    .andReturn();
            mockMvc.perform(asyncDispatch(pending)).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.3");
                            return request;
                        })
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Espera un minuto")));
    }

    @Test
    void returnsRecommendationsForACategory() throws Exception {
        mockMvc.perform(get("/api/ai/recommendations").param("category", "Documental"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Documental"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void reportsTheWorkersAndRealtimeStatusInTheHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realtime.path").value("/ws/kino"))
                .andExpect(jsonPath("$.workers.ai.threads").value(2))
                .andExpect(jsonPath("$.workers.insightsRefresh.enabled").value(false));
    }
}
