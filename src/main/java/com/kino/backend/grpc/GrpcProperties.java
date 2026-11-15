package com.kino.backend.grpc;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kino.grpc")
public record GrpcProperties(boolean enabled, int port, int maxInboundMessageBytes, Duration shutdownGracePeriod) {
    public GrpcProperties {
        port = port < 0 ? 9090 : port;
        maxInboundMessageBytes = maxInboundMessageBytes <= 0 ? 1_048_576 : maxInboundMessageBytes;
        shutdownGracePeriod = shutdownGracePeriod == null ? Duration.ofSeconds(5) : shutdownGracePeriod;
    }
}
