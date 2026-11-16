package com.kino.backend.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kino.workers.insights-refresh.enabled=false")
class KinoRealtimeHubIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KinoRealtimeHub hub;

    @Test
    void sendsTheWelcomeAndInsightsAndAnswersPingsAndChats() throws Exception {
        BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        WebSocketSession session = connect("http://localhost:4200", received);
        try {
            assertThat(next(received, "welcome").path("data").path("features").toString()).contains("chat");
            assertThat(next(received, "insights").path("data").has("totalViews")).isTrue();
            assertThat(hub.connectionCount()).isEqualTo(1);

            session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
            assertThat(next(received, "pong").path("data").has("timestamp")).isTrue();

            session.sendMessage(new TextMessage(
                    "{\"type\":\"chat\",\"requestId\":\"q1\",\"messages\":[{\"role\":\"user\",\"content\":\"¿Qué es lo más visto?\"}]}"));
            JsonNode reply = next(received, "chat-reply");
            assertThat(reply.path("requestId").asText()).isEqualTo("q1");
            assertThat(reply.path("data").path("reply").asText()).isNotBlank();

            session.sendMessage(new TextMessage(
                    "{\"type\":\"chat\",\"requestId\":\"q2\",\"messages\":[{\"role\":\"robot\",\"content\":\"x\"}]}"));
            JsonNode error = next(received, "chat-error");
            assertThat(error.path("requestId").asText()).isEqualTo("q2");
            assertThat(error.path("data").path("message").asText()).isEqualTo("El rol debe ser user o assistant");

            session.sendMessage(new TextMessage("no es json"));
            assertThat(next(received, "error").path("data").path("message").asText()).contains("JSON");
        } finally {
            session.close();
        }
    }

    @Test
    void rejectsOriginsThatAreNotAllowed() {
        assertThatThrownBy(() -> connect("https://evil.example.com", new LinkedBlockingQueue<>()))
                .isInstanceOf(ExecutionException.class);
    }

    private WebSocketSession connect(String origin, BlockingQueue<JsonNode> received) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        return new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                received.add(objectMapper.readTree(message.getPayload()));
            }
        }, headers, URI.create("ws://localhost:" + port + KinoRealtimeHub.PATH)).get(10, TimeUnit.SECONDS);
    }

    private static JsonNode next(BlockingQueue<JsonNode> received, String type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            JsonNode message = received.poll(500, TimeUnit.MILLISECONDS);
            if (message != null && type.equals(message.path("type").asText())) {
                return message;
            }
        }
        throw new AssertionError("No '" + type + "' message received");
    }
}
