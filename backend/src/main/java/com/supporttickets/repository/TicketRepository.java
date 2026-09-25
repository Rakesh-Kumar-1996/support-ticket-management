package com.supporttickets.repository;

import com.supporttickets.domain.Ticket;
import com.supporttickets.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByOrderByCreatedAtDescIdDesc();

    List<Ticket> findByStatusOrderByCreatedAtDescIdDesc(TicketStatus status);

    @Query("""
            SELECT t FROM Ticket t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.createdAt DESC, t.id DESC
            """)
    List<Ticket> searchByKeyword(@Param("q") String q);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.status = :status
              AND (
                    LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            ORDER BY t.createdAt DESC, t.id DESC
            """)
    List<Ticket> searchByKeywordAndStatus(@Param("q") String q, @Param("status") TicketStatus status);

    default List<Ticket> search(String q, TicketStatus status) {
        boolean hasKeyword = q != null && !q.isBlank();
        if (!hasKeyword && status == null) {
            return findAllByOrderByCreatedAtDescIdDesc();
        }
        if (!hasKeyword) {
            return findByStatusOrderByCreatedAtDescIdDesc(status);
        }
        if (status == null) {
            return searchByKeyword(q);
        }
        return searchByKeywordAndStatus(q, status);
    }
}
