package com.kino.backend.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

final class GrpcErrors {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcErrors.class);

    private GrpcErrors() {
    }

    static StatusRuntimeException invalidArgument(String message) {
        return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }

    static StatusRuntimeException toStatus(RuntimeException exception) {
        if (exception instanceof StatusRuntimeException statusException) {
            return statusException;
        }
        if (exception instanceof ResponseStatusException responseStatus) {
            Status status = switch (responseStatus.getStatusCode().value()) {
                case 400 -> Status.INVALID_ARGUMENT;
                case 401 -> Status.UNAUTHENTICATED;
                case 403 -> Status.PERMISSION_DENIED;
                case 404 -> Status.NOT_FOUND;
                case 409 -> Status.FAILED_PRECONDITION;
                case 429 -> Status.RESOURCE_EXHAUSTED;
                default -> Status.INTERNAL;
            };
            return status.withDescription(responseStatus.getReason()).asRuntimeException();
        }
        LOGGER.error("Unexpected gRPC failure", exception);
        return Status.INTERNAL.withDescription("Ocurrió un error inesperado").asRuntimeException();
    }
}
