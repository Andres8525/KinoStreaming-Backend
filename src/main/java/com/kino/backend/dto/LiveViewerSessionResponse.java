package com.kino.backend.dto;

import java.util.UUID;

public record LiveViewerSessionResponse(UUID sessionId, String answer) {
}
