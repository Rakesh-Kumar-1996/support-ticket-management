package com.supporttickets.domain;

public class TicketValidationException extends RuntimeException {

    private final String field;

    public TicketValidationException(String message) {
        this(null, message);
    }

    public TicketValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
