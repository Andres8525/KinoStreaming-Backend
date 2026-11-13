package com.kino.backend.dto;

import java.util.UUID;

/** A viewer waiting for the creator's browser to answer its WebRTC offer. */
public record PendingViewerResponse(UUID sessionId, String viewerName, String offer) {
}
