package com.supporttickets.api;

import com.supporttickets.dto.CommentResponse;
import com.supporttickets.dto.CreateCommentRequest;
import com.supporttickets.dto.CreateTicketRequest;
import com.supporttickets.dto.TicketDetailResponse;
import com.supporttickets.dto.TicketListResponse;
import com.supporttickets.dto.TicketResponse;
import com.supporttickets.dto.TicketSummaryResponse;
import com.supporttickets.dto.UpdateStatusRequest;
import com.supporttickets.dto.UpdateTicketRequest;
import com.supporttickets.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + created.id())).body(created);
    }

    @GetMapping("/summary")
    public TicketSummaryResponse summary() {
        return ticketService.getSummary();
    }

    @GetMapping
    public TicketListResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        return ticketService.list(q, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable UUID id) {
        return ticketService.get(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable UUID id, @RequestBody UpdateTicketRequest request) {
        return ticketService.updateFields(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return ticketService.changeStatus(id, request);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @RequestBody CreateCommentRequest request
    ) {
        CommentResponse created = ticketService.addComment(id, request);
        return ResponseEntity.created(URI.create("/api/tickets/" + id)).body(created);
    }
}
