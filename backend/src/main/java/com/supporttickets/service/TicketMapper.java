package com.supporttickets.service;

import com.supporttickets.domain.Comment;
import com.supporttickets.domain.Ticket;
import com.supporttickets.dto.CommentResponse;
import com.supporttickets.dto.TicketDetailResponse;
import com.supporttickets.dto.TicketResponse;

import java.util.List;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public static CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicketId(),
                comment.getBody(),
                comment.getAuthor(),
                comment.getCreatedAt()
        );
    }

    public static TicketDetailResponse toDetailResponse(Ticket ticket, List<Comment> comments) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments.stream().map(TicketMapper::toCommentResponse).toList()
        );
    }
}
