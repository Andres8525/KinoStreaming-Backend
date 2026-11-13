package com.kino.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A WebRTC session description (SDP offer or answer) with its ICE candidates included. */
public record SessionDescriptionRequest(
        @NotBlank(message = "Falta la descripción de la conexión")
        @Size(max = 30000, message = "La descripción de la conexión es demasiado grande")
        String sdp) {
}
