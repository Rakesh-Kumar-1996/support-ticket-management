package com.supporttickets.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus from;
    private final TicketStatus to;

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("Transition from " + from + " to " + to + " is not allowed.");
        this.from = from;
        this.to = to;
    }

    public TicketStatus getFrom() {
        return from;
    }

    public TicketStatus getTo() {
        return to;
    }
}
