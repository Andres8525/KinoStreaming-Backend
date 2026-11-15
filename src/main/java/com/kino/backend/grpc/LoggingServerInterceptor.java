package com.kino.backend.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingServerInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingServerInterceptor.class);

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers,
                                                       ServerCallHandler<Q, R> next) {
        long startedAt = System.nanoTime();
        String method = call.getMethodDescriptor().getFullMethodName();
        ServerCall<Q, R> timedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                if (status.isOk()) {
                    LOGGER.debug("gRPC {} OK in {} ms", method, elapsedMs);
                } else {
                    LOGGER.info("gRPC {} {} in {} ms: {}", method, status.getCode(), elapsedMs, status.getDescription());
                }
                super.close(status, trailers);
            }
        };
        return next.startCall(timedCall, headers);
    }
}
