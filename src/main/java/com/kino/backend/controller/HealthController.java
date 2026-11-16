package com.kino.backend.controller;

import com.kino.backend.ai.AiProperties;
import com.kino.backend.ai.AiTextGenerator;
import com.kino.backend.config.CorsOrigins;
import com.kino.backend.realtime.KinoRealtimeHub;
import com.kino.backend.worker.AiWorker;
import com.kino.backend.worker.InsightsRefreshWorker;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final CorsOrigins corsOrigins;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final AiProperties aiProperties;
    private final ObjectProvider<AiTextGenerator> aiTextGenerators;
    private final KinoRealtimeHub realtimeHub;
    private final AiWorker aiWorker;
    private final InsightsRefreshWorker insightsRefreshWorker;

    public HealthController(JdbcTemplate jdbcTemplate, CorsOrigins corsOrigins,
                            ObjectProvider<BuildProperties> buildProperties, AiProperties aiProperties,
                            ObjectProvider<AiTextGenerator> aiTextGenerators, KinoRealtimeHub realtimeHub,
                            AiWorker aiWorker, InsightsRefreshWorker insightsRefreshWorker) {
        this.jdbcTemplate = jdbcTemplate;
        this.corsOrigins = corsOrigins;
        this.buildProperties = buildProperties;
        this.aiProperties = aiProperties;
        this.aiTextGenerators = aiTextGenerators;
        this.realtimeHub = realtimeHub;
        this.aiWorker = aiWorker;
        this.insightsRefreshWorker = insightsRefreshWorker;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("database", databaseStatus());
        body.put("timestamp", Instant.now());
        BuildProperties build = buildProperties.getIfAvailable();
        body.put("build", build == null ? "unknown" : build.getTime());
        body.put("cors", Map.of("origins", corsOrigins.origins(), "patterns", corsOrigins.patterns()));
        body.put("ai", aiStatus());
        body.put("realtime", Map.of("path", KinoRealtimeHub.PATH, "connections", realtimeHub.connectionCount()));
        body.put("workers", Map.of("ai", aiWorker.status(), "insightsRefresh", insightsRefreshWorker.status()));
        return body;
    }

    private Map<String, Object> aiStatus() {
        AiTextGenerator generator = aiTextGenerators.getIfAvailable();
        boolean apiKeyConfigured = aiProperties.apiKey() != null && !aiProperties.apiKey().isBlank();
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("enabled", aiProperties.enabled());
        ai.put("provider", generator == null ? aiProperties.provider() : generator.name());
        ai.put("model", generator == null ? null : generator.model());
        ai.put("apiKeyConfigured", apiKeyConfigured);
        ai.put("active", aiProperties.enabled() && generator != null && apiKeyConfigured);
        return ai;
    }

    private String databaseStatus() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (RuntimeException exception) {
            LOGGER.warn("Database health check failed: {}", exception.getMessage());
            return "DOWN";
        }
    }
}
