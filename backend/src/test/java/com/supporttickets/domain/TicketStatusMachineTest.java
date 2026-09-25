package com.supporttickets.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusMachineTest {

    private final TicketStatusMachine machine = new TicketStatusMachine();

    @ParameterizedTest
    @CsvSource({
            "OPEN,IN_PROGRESS",
            "OPEN,CANCELLED",
            "IN_PROGRESS,RESOLVED",
            "IN_PROGRESS,CANCELLED",
            "RESOLVED,CLOSED"
    })
    void ut001_validEdgesAllowed(TicketStatus from, TicketStatus to) {
        assertTrue(machine.allows(from, to));
        machine.assertAllowed(from, to);
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED,OPEN",
            "RESOLVED,OPEN",
            "CANCELLED,OPEN",
            "OPEN,RESOLVED",
            "RESOLVED,CANCELLED"
    })
    void ut002_specifiedInvalidExamplesDenied(TicketStatus from, TicketStatus to) {
        assertFalse(machine.allows(from, to));
        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> machine.assertAllowed(from, to)
        );
        assertEquals(from, ex.getFrom());
        assertEquals(to, ex.getTo());
        assertEquals("Transition from " + from + " to " + to + " is not allowed.", ex.getMessage());
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void ut003_selfTransitionsDenied(TicketStatus status) {
        assertFalse(machine.allows(status, status));
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED,IN_PROGRESS",
            "CANCELLED,CLOSED",
            "OPEN,CLOSED",
            "IN_PROGRESS,OPEN",
            "IN_PROGRESS,CLOSED",
            "RESOLVED,IN_PROGRESS",
            "CANCELLED,OPEN"
    })
    void ut004_extraInvalidCellsDenied(TicketStatus from, TicketStatus to) {
        assertFalse(machine.allows(from, to));
    }

    @Test
    void enumNamesMatchApiValues() {
        assertEquals("OPEN", TicketStatus.OPEN.name());
        assertEquals("IN_PROGRESS", TicketStatus.IN_PROGRESS.name());
        assertEquals("RESOLVED", TicketStatus.RESOLVED.name());
        assertEquals("CLOSED", TicketStatus.CLOSED.name());
        assertEquals("CANCELLED", TicketStatus.CANCELLED.name());
        assertEquals("LOW", TicketPriority.LOW.name());
        assertEquals("MEDIUM", TicketPriority.MEDIUM.name());
        assertEquals("HIGH", TicketPriority.HIGH.name());
    }
}
