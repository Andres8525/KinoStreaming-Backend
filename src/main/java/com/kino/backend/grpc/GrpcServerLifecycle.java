package com.kino.backend.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "kino.grpc", name = "enabled", havingValue = "true")
public class GrpcServerLifecycle implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final GrpcProperties properties;
    private final List<BindableService> services;
    private final HealthStatusManager health = new HealthStatusManager();
    private volatile Server server;

    public GrpcServerLifecycle(GrpcProperties properties, CatalogGrpcService catalog, InsightsGrpcService insights,
                               LiveGrpcService live) {
        this.properties = properties;
        this.services = List.of(catalog, insights, live);
    }

    @Override
    public void start() {
        LoggingServerInterceptor logging = new LoggingServerInterceptor();
        NettyServerBuilder builder = NettyServerBuilder.forPort(properties.port())
                .maxInboundMessageSize(properties.maxInboundMessageBytes())
                .keepAliveTime(60, TimeUnit.SECONDS)
                .permitKeepAliveTime(30, TimeUnit.SECONDS)
                .addService(health.getHealthService())
                .addService(ProtoReflectionServiceV1.newInstance());
        services.forEach(service -> builder.addService(ServerInterceptors.intercept(service, logging)));
        try {
            server = builder.build().start();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not start the gRPC server on port " + properties.port(), exception);
        }
        services.forEach(service -> health.setStatus(service.bindService().getServiceDescriptor().getName(),
                ServingStatus.SERVING));
        health.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
        LOGGER.info("gRPC server listening on port {}", server.getPort());
    }

    @Override
    public void stop() {
        Server current = server;
        if (current == null) {
            return;
        }
        health.enterTerminalState();
        current.shutdown();
        try {
            if (!current.awaitTermination(properties.shutdownGracePeriod().toMillis(), TimeUnit.MILLISECONDS)) {
                current.shutdownNow();
            }
        } catch (InterruptedException exception) {
            current.shutdownNow();
            Thread.currentThread().interrupt();
        }
        server = null;
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }

    public int port() {
        return server == null ? -1 : server.getPort();
    }
}
