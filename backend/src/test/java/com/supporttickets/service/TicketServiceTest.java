package com.supporttickets.service;

import com.supporttickets.domain.InvalidStatusTransitionException;
import com.supporttickets.domain.TicketNotFoundException;
import com.supporttickets.domain.TicketPriority;
import com.supporttickets.domain.TicketStatus;
import com.supporttickets.domain.TicketValidationException;
import com.supporttickets.dto.CreateCommentRequest;
import com.supporttickets.dto.CreateTicketRequest;
import com.supporttickets.dto.TicketDetailResponse;
import com.supporttickets.dto.TicketResponse;
import com.supporttickets.dto.UpdateStatusRequest;
import com.supporttickets.dto.UpdateTicketRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Test
    void createUsesOpenAndDefaultMediumAndIgnoresClientOwnedFields() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("  Need help  ");
        request.setDescription("  keep spaces  ");
        request.setAssignee("  alex  ");
        TicketResponse created = ticketService.create(request);
        assertEquals(TicketStatus.OPEN, created.status());
        assertEquals(TicketPriority.MEDIUM, created.priority());
        assertEquals("Need help", created.title());
        assertEquals("  keep spaces  ", created.description());
        assertEquals("  alex  ", created.assignee());
        assertEquals(created.createdAt(), created.updatedAt());
    }

    @Test
    void getUnknownTicketThrowsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> ticketService.get(UUID.randomUUID()));
    }

    @Test
    void updateClearsEmptyDescriptionAndAssigneeAndBumpsUpdatedAt() {
        TicketResponse created = ticketService.create(request("Title"));
        Instant originalUpdated = created.updatedAt();

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("  New title  ");
        update.setDescription("");
        update.setAssignee("");
        update.setPriority("HIGH");
        TicketResponse updated = ticketService.updateFields(created.id(), update);

        assertEquals("New title", updated.title());
        assertNull(updated.description());
        assertNull(updated.assignee());
        assertEquals(TicketPriority.HIGH, updated.priority());
        assertEquals(TicketStatus.OPEN, updated.status());
        assertTrue(!updated.updatedAt().isBefore(originalUpdated));
    }

    @Test
    void statusOnFieldUpdateIsValidationAndDoesNotPersist() {
        TicketResponse created = ticketService.create(request("Title"));
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("Changed");
        update.setStatus(null);
        assertThrows(TicketValidationException.class, () -> ticketService.updateFields(created.id(), update));
        TicketDetailResponse loaded = ticketService.get(created.id());
        assertEquals("Title", loaded.title());
        assertEquals(TicketStatus.OPEN, loaded.status());
    }

    @Test
    void commentDoesNotChangeTicketUpdatedAtAndUnknownTicketIsNotFound() {
        TicketResponse created = ticketService.create(request("Title"));
        Instant updatedAt = created.updatedAt();
        CreateCommentRequest comment = new CreateCommentRequest();
        comment.setBody("  hello  ");
        comment.setAuthor("  sam  ");
        var saved = ticketService.addComment(created.id(), comment);
        assertEquals("hello", saved.body());
        assertEquals("  sam  ", saved.author());
        assertEquals(updatedAt, ticketService.get(created.id()).updatedAt());

        CreateCommentRequest orphan = new CreateCommentRequest();
        orphan.setBody("nope");
        assertThrows(TicketNotFoundException.class, () -> ticketService.addComment(UUID.randomUUID(), orphan));
    }

    @Test
    void changeStatusUnknownTicketIsNotFoundEvenIfStatusInvalid() {
        assertThrows(
                TicketNotFoundException.class,
                () -> ticketService.changeStatus(UUID.randomUUID(), status("NOT_A_STATUS"))
        );
    }

    @Test
    void changeStatusAllowsValidEdgeAndRejectsIllegal() {
        TicketResponse created = ticketService.create(request("Title"));
        TicketResponse inProgress = ticketService.changeStatus(created.id(), status("IN_PROGRESS"));
        assertEquals(TicketStatus.IN_PROGRESS, inProgress.status());
        assertThrows(
                InvalidStatusTransitionException.class,
                () -> ticketService.changeStatus(created.id(), status("OPEN"))
        );
        assertEquals(TicketStatus.IN_PROGRESS, ticketService.get(created.id()).status());
    }

    @Test
    void listSearchAndFilter() {
        ticketService.create(titled("Alpha", "needle in description"));
        CreateTicketRequest beta = request("Beta");
        beta.setDescription("other");
        ticketService.create(beta);
        assertEquals(1, ticketService.list("NEEDLE", "OPEN").items().size());
        assertEquals(2, ticketService.list(null, null).items().size());
    }

    private static CreateTicketRequest request(String title) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        return request;
    }

    private static CreateTicketRequest titled(String title, String description) {
        CreateTicketRequest request = request(title);
        request.setDescription(description);
        return request;
    }

    private static UpdateStatusRequest status(String status) {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(status);
        return request;
    }
}
