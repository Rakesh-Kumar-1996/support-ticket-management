package com.supporttickets.repository;

import com.supporttickets.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
