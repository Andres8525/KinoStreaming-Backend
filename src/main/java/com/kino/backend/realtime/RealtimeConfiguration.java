package com.kino.backend.realtime;

import com.kino.backend.config.CorsOrigins;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeConfiguration implements WebSocketConfigurer {
    private final KinoRealtimeHub hub;
    private final CorsOrigins corsOrigins;

    public RealtimeConfiguration(KinoRealtimeHub hub, CorsOrigins corsOrigins) {
        this.hub = hub;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(hub, KinoRealtimeHub.PATH)
                .setAllowedOrigins(corsOrigins.origins().toArray(String[]::new))
                .setAllowedOriginPatterns(corsOrigins.patterns().toArray(String[]::new));
    }
}
