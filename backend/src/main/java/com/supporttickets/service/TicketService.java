package com.supporttickets.service;

import com.supporttickets.domain.Comment;
import com.supporttickets.domain.Ticket;
import com.supporttickets.domain.TicketNotFoundException;
import com.supporttickets.domain.TicketStatus;
import com.supporttickets.domain.TicketStatusMachine;
import com.supporttickets.dto.CommentResponse;
import com.supporttickets.dto.CreateCommentRequest;
import com.supporttickets.dto.CreateTicketRequest;
import com.supporttickets.dto.TicketDetailResponse;
import com.supporttickets.dto.TicketListResponse;
import com.supporttickets.dto.TicketRequestValidator;
import com.supporttickets.dto.TicketResponse;
import com.supporttickets.dto.UpdateStatusRequest;
import com.supporttickets.dto.UpdateTicketRequest;
import com.supporttickets.repository.CommentRepository;
import com.supporttickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketStatusMachine statusMachine;

    public TicketService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            TicketStatusMachine statusMachine
    ) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.statusMachine = statusMachine;
    }

    public TicketResponse create(CreateTicketRequest request) {
        TicketRequestValidator.validateCreate(request);
        Instant now = Instant.now();
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                TicketRequestValidator.requireTrimmedTitle(request.getTitle()),
                request.getDescription(),
                TicketStatus.OPEN,
                TicketRequestValidator.parsePriorityOrDefault(request.getPriority()),
                request.getAssignee(),
                now,
                now
        );
        return TicketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketListResponse list(String q, String status) {
        TicketStatus statusFilter = TicketRequestValidator.parseOptionalListStatus(status);
        String keyword = (q == null || q.isBlank()) ? null : q;
        return new TicketListResponse(
                ticketRepository.search(keyword, statusFilter).stream()
                        .map(TicketMapper::toTicketResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse get(UUID id) {
        Ticket ticket = requireTicket(id);
        return TicketMapper.toDetailResponse(
                ticket,
                commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(id)
        );
    }

    public TicketResponse updateFields(UUID id, UpdateTicketRequest request) {
        TicketRequestValidator.validateUpdate(request);
        Ticket ticket = requireTicket(id);
        if (request.isTitlePresent()) {
            ticket.setTitle(TicketRequestValidator.requireTrimmedTitle(request.getTitle()));
        }
        if (request.isDescriptionPresent()) {
            ticket.setDescription(TicketRequestValidator.clearable(request.getDescription()));
        }
        if (request.isPriorityPresent()) {
            ticket.setPriority(TicketRequestValidator.parsePriority(request.getPriority()));
        }
        if (request.isAssigneePresent()) {
            ticket.setAssignee(TicketRequestValidator.clearable(request.getAssignee()));
        }
        ticket.setUpdatedAt(Instant.now());
        return TicketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    public TicketResponse changeStatus(UUID id, UpdateStatusRequest request) {
        Ticket ticket = requireTicket(id);
        TicketStatus target = TicketRequestValidator.validateStatusChange(request);
        statusMachine.assertAllowed(ticket.getStatus(), target);
        ticket.setStatus(target);
        ticket.setUpdatedAt(Instant.now());
        return TicketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    public CommentResponse addComment(UUID id, CreateCommentRequest request) {
        TicketRequestValidator.validateComment(request);
        requireTicket(id);
        Comment comment = new Comment(
                UUID.randomUUID(),
                id,
                TicketRequestValidator.requireTrimmedBody(request.getBody()),
                request.getAuthor(),
                Instant.now()
        );
        return TicketMapper.toCommentResponse(commentRepository.save(comment));
    }

    private Ticket requireTicket(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
