package com.supporttickets.dto;

import com.supporttickets.domain.TicketPriority;
import com.supporttickets.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {
}
