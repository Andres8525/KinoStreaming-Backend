package com.kino.backend.service;

/**
 * A request that conflicts with existing data because of one specific field (for example an email
 * that is already registered). Returned as 409 with the message also listed under that field, so
 * the frontend can show it next to the input.
 */
public class FieldConflictException extends RuntimeException {

    private final String field;

    public FieldConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
