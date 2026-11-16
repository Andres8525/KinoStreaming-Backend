package com.kino.backend.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kino.backend.ai.AiRateLimitException;
import com.kino.backend.ai.AiRateLimiter;
import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.dto.AiChatRequest;
import com.kino.backend.dto.ChatMessage;
import com.kino.backend.service.FieldValidationException;
import com.kino.backend.worker.AiWorker;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class KinoRealtimeHub extends TextWebSocketHandler {
    public static final String PATH = "/ws/kino";
    static final int MAX_MESSAGE_LENGTH = 32 * 1024;
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int SEND_BUFFER_LIMIT = 512 * 1024;
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger(KinoRealtimeHub.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AiWorker aiWorker;
    private final AiRateLimiter rateLimiter;
    private final TrendingInsightsService insightsService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public KinoRealtimeHub(ObjectMapper objectMapper, Validator validator, AiWorker aiWorker,
                           AiRateLimiter rateLimiter, TrendingInsightsService insightsService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.aiWorker = aiWorker;
        this.rateLimiter = rateLimiter;
        this.insightsService = insightsService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession concurrentSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);
        sessions.put(session.getId(), concurrentSession);
        send(concurrentSession, "welcome", Map.of("features", List.of("insights", "chat"), "timestamp", Instant.now()));
        sendInsights(concurrentSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketSession target = sessions.getOrDefault(session.getId(), session);
        if (message.getPayloadLength() > MAX_MESSAGE_LENGTH) {
            send(target, "error", Map.of("message", "El mensaje es demasiado grande"));
            return;
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException exception) {
            send(target, "error", Map.of("message", "El mensaje no es un JSON válido"));
            return;
        }
        switch (payload.path("type").asText("")) {
            case "ping" -> send(target, "pong", Map.of("timestamp", Instant.now()));
            case "insights" -> sendInsights(target);
            case "chat" -> chat(target, payload);
            default -> send(target, "error", Map.of("message", "Tipo de mensaje desconocido"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOGGER.debug("WebSocket transport error on {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    public int connectionCount() {
        return sessions.size();
    }

    public void broadcast(String type, Object data) {
        String json = envelope(type, null, data);
        if (json == null) {
            return;
        }
        sessions.values().forEach(session -> sendRaw(session, json));
    }

    private void chat(WebSocketSession session, JsonNode payload) {
        String requestId = payload.path("requestId").asText("");
        if (requestId.length() > MAX_REQUEST_ID_LENGTH) {
            requestId = requestId.substring(0, MAX_REQUEST_ID_LENGTH);
        }
        String id = requestId;
        AiChatRequest request;
        try {
            request = objectMapper.treeToValue(payload, AiChatRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            sendChatError(session, id, "La conversación no tiene un formato válido");
            return;
        }
        Set<ConstraintViolation<AiChatRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            sendChatError(session, id, violations.iterator().next().getMessage());
            return;
        }
        try {
            rateLimiter.acquire("ip:" + clientAddress(session));
            List<ChatMessage> messages = request.messages();
            aiWorker.chat(messages).whenComplete((response, error) -> {
                if (error == null) {
                    send(session, "chat-reply", id, response);
                } else {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause() : error;
                    LOGGER.warn("Kino IA chat over WebSocket failed: {}", cause.getMessage());
                    sendChatError(session, id, cause instanceof FieldValidationException
                            ? cause.getMessage() : "Kino IA no pudo responder ahora. Inténtalo de nuevo.");
                }
            });
        } catch (AiRateLimitException | FieldValidationException exception) {
            sendChatError(session, id, exception.getMessage());
        } catch (RejectedExecutionException exception) {
            sendChatError(session, id, "Kino IA está atendiendo muchas preguntas. Inténtalo en unos segundos.");
        }
    }

    private void sendInsights(WebSocketSession session) {
        try {
            aiWorker.run(insightsService::insights).whenComplete((insights, error) -> {
                if (error == null) {
                    send(session, "insights", insights);
                } else {
                    LOGGER.warn("Could not send the Kino IA insights over WebSocket: {}", error.getMessage());
                }
            });
        } catch (RejectedExecutionException exception) {
            LOGGER.warn("AI workers are busy; the client will receive the insights on the next refresh");
        }
    }

    private void sendChatError(WebSocketSession session, String requestId, String message) {
        send(session, "chat-error", requestId, Map.of("message", message));
    }

    private void send(WebSocketSession session, String type, Object data) {
        send(session, type, null, data);
    }

    private void send(WebSocketSession session, String type, String requestId, Object data) {
        String json = envelope(type, requestId, data);
        if (json != null) {
            sendRaw(session, json);
        }
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException | IllegalStateException exception) {
            LOGGER.debug("Could not send a WebSocket message to {}: {}", session.getId(), exception.getMessage());
            sessions.remove(session.getId());
        }
    }

    private String envelope(String type, String requestId, Object data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", type);
        if (requestId != null) {
            envelope.put("requestId", requestId);
        }
        envelope.put("data", data);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Could not serialize the WebSocket message '{}'", type, exception);
            return null;
        }
    }

    private static String clientAddress(WebSocketSession session) {
        String forwarded = session.getHandshakeHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return session.getRemoteAddress() == null ? "unknown" : session.getRemoteAddress().getAddress().getHostAddress();
    }
}
