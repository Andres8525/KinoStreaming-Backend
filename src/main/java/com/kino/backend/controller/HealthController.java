package com.kino.backend.controller;

import com.kino.backend.config.CorsOrigins;
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

/**
 * Public health check used by the hosting platform (Render) to know the service is up.
 * It always answers 200 while the application runs and reports the database status separately,
 * so a paused or unreachable database does not make the platform restart the service in a loop.
 *
 * <p>For deployment diagnostics it also reports the build time of the running version and the CORS
 * origins the backend understood from CORS_ALLOWED_ORIGINS (public information: any browser can probe it).
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final CorsOrigins corsOrigins;
    private final ObjectProvider<BuildProperties> buildProperties;

    public HealthController(JdbcTemplate jdbcTemplate, CorsOrigins corsOrigins,
                            ObjectProvider<BuildProperties> buildProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.corsOrigins = corsOrigins;
        this.buildProperties = buildProperties;
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
