package com.supporttickets.dto;

public record TicketSummaryResponse(
        long total,
        long open,
        long inProgress,
        long resolved,
        long closed,
        long cancelled
) {
}
