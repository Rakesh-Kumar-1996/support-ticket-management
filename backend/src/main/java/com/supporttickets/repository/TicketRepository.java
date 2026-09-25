package com.supporttickets.repository;

import com.supporttickets.domain.Ticket;
import com.supporttickets.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countByStatus();

    default List<Ticket> search(String q, TicketStatus status) {
        return TicketSpecifications.search(this, q, status);
    }
}
