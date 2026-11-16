package com.kino.backend.ai;

public class AiRateLimitException extends RuntimeException {
    public static final String MESSAGE =
            "Has enviado muchas preguntas a Kino IA. Espera un minuto e inténtalo de nuevo.";

    public AiRateLimitException() {
        super(MESSAGE);
    }
}
