package com.kino.backend.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health check used by the hosting platform (Render) to know the service is up.
 * It always answers 200 while the application runs and reports the database status separately,
 * so a paused or unreachable database does not make the platform restart the service in a loop.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("database", databaseStatus());
        body.put("timestamp", Instant.now());
        return body;
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
