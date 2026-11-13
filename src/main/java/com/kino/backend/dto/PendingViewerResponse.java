package com.kino.backend.dto;

import java.util.UUID;

public record PendingViewerResponse(UUID sessionId, String viewerName, String offer) {
}
