package com.supporttickets.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        String body,
        String author,
        Instant createdAt
) {
}
