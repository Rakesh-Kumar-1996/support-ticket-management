package com.supporttickets.domain;

import java.util.Set;

public final class TicketStatusMachine {

    private record Edge(TicketStatus from, TicketStatus to) {
    }

    private static final Set<Edge> ALLOWED = Set.of(
            new Edge(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
            new Edge(TicketStatus.OPEN, TicketStatus.CANCELLED),
            new Edge(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            new Edge(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            new Edge(TicketStatus.RESOLVED, TicketStatus.CLOSED)
    );

    public boolean allows(TicketStatus from, TicketStatus to) {
        return ALLOWED.contains(new Edge(from, to));
    }

    public void assertAllowed(TicketStatus from, TicketStatus to) {
        if (!allows(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
