package com.kino.backend.service;

/**
 * A request field that is invalid for a reason bean validation cannot express (it depends on another
 * field). Returned as 400 with the message under {@code validationErrors.<field>}.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
