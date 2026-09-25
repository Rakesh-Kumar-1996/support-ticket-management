package com.supporttickets.repository;

import com.supporttickets.domain.Comment;
import com.supporttickets.domain.Ticket;
import com.supporttickets.domain.TicketPriority;
import com.supporttickets.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void rt001_saveAndLoadById() {
        Ticket saved = ticketRepository.save(ticket("Reset password", "Email never arrives", TicketStatus.OPEN));
        Ticket loaded = ticketRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Reset password", loaded.getTitle());
        assertEquals(TicketStatus.OPEN, loaded.getStatus());
    }

    @Test
    void listWithNoKeywordAndNoStatusReturnsAllTickets() {
        ticketRepository.save(ticket("First", "d", TicketStatus.OPEN));
        ticketRepository.save(ticket("Second", "d", TicketStatus.CANCELLED));
        List<Ticket> all = ticketRepository.search(null, null);
        assertEquals(2, all.size());
    }

    @Test
    void rt002_statusFilter() {
        ticketRepository.save(ticket("Open one", "d", TicketStatus.OPEN));
        ticketRepository.save(ticket("Cancelled one", "d", TicketStatus.CANCELLED));
        List<Ticket> open = ticketRepository.search(null, TicketStatus.OPEN);
        assertTrue(open.stream().allMatch(t -> t.getStatus() == TicketStatus.OPEN));
        assertEquals(1, open.size());
    }

    @Test
    void rt003_keywordMatchesTitle() {
        ticketRepository.save(ticket("Cannot reset password", "other", TicketStatus.OPEN));
        ticketRepository.save(ticket("Printer jam", "paper", TicketStatus.OPEN));
        List<Ticket> found = ticketRepository.search("RESET", null);
        assertEquals(1, found.size());
        assertEquals("Cannot reset password", found.getFirst().getTitle());
    }

    @Test
    void rt004_keywordMatchesDescriptionNotUnrelated() {
        ticketRepository.save(ticket("Alpha", "UniqueZebraToken", TicketStatus.OPEN));
        ticketRepository.save(ticket("Beta", "nothing here", TicketStatus.OPEN));
        List<Ticket> found = ticketRepository.search("zebra", null);
        assertEquals(1, found.size());
        assertEquals("Alpha", found.getFirst().getTitle());
    }

    @Test
    void rt005_keywordAndStatusAnd() {
        ticketRepository.save(ticket("Network down", "wifi outage", TicketStatus.OPEN));
        ticketRepository.save(ticket("Network down", "wifi outage", TicketStatus.CANCELLED));
        List<Ticket> found = ticketRepository.search("wifi", TicketStatus.OPEN);
        assertEquals(1, found.size());
        assertEquals(TicketStatus.OPEN, found.getFirst().getStatus());
    }

    @Test
    void rt006_commentsLoadedForTicketOnly() {
        Ticket first = ticketRepository.save(ticket("One", "d", TicketStatus.OPEN));
        Ticket second = ticketRepository.save(ticket("Two", "d", TicketStatus.OPEN));
        Instant t1 = Instant.parse("2026-09-24T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-24T11:00:00Z");
        commentRepository.save(new Comment(UUID.randomUUID(), first.getId(), "later", null, t2));
        commentRepository.save(new Comment(UUID.randomUUID(), first.getId(), "earlier", null, t1));
        commentRepository.save(new Comment(UUID.randomUUID(), second.getId(), "other ticket", null, t1));

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(first.getId());
        assertEquals(2, comments.size());
        assertEquals("earlier", comments.get(0).getBody());
        assertEquals("later", comments.get(1).getBody());
        assertTrue(comments.stream().allMatch(c -> c.getTicketId().equals(first.getId())));
    }

    @Test
    void commentsWithSameCreatedAtAreOrderedByIdAsc() {
        Ticket ticket = ticketRepository.save(ticket("One", "d", TicketStatus.OPEN));
        Instant sameTime = Instant.parse("2026-09-24T10:00:00Z");
        UUID laterId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID earlierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        commentRepository.save(new Comment(laterId, ticket.getId(), "second", null, sameTime));
        commentRepository.save(new Comment(earlierId, ticket.getId(), "first", null, sameTime));

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId());
        assertEquals("first", comments.get(0).getBody());
        assertEquals("second", comments.get(1).getBody());
    }

    @Test
    void listOrderIsCreatedAtDescThenIdDesc() {
        Instant older = Instant.parse("2026-09-24T09:00:00Z");
        Instant newer = Instant.parse("2026-09-24T10:00:00Z");
        UUID lowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID highId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ticketRepository.save(new Ticket(lowId, "A", null, TicketStatus.OPEN, TicketPriority.MEDIUM, null, newer, newer));
        ticketRepository.save(new Ticket(highId, "B", null, TicketStatus.OPEN, TicketPriority.MEDIUM, null, newer, newer));
        ticketRepository.save(new Ticket(UUID.randomUUID(), "C", null, TicketStatus.OPEN, TicketPriority.MEDIUM, null, older, older));

        List<Ticket> all = ticketRepository.search(null, null);
        assertEquals("B", all.get(0).getTitle());
        assertEquals("A", all.get(1).getTitle());
        assertEquals("C", all.get(2).getTitle());
    }

    private Ticket ticket(String title, String description, TicketStatus status) {
        Instant now = Instant.parse("2026-09-24T12:00:00Z");
        return new Ticket(UUID.randomUUID(), title, description, status, TicketPriority.MEDIUM, null, now, now);
    }
}
