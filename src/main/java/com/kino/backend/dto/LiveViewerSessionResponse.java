package com.kino.backend.dto;

import java.util.UUID;

/** A viewer's WebRTC connection request; {@code answer} stays null until the creator's browser answers. */
public record LiveViewerSessionResponse(UUID sessionId, String answer) {
}
