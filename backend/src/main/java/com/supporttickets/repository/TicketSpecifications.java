package com.supporttickets.repository;

import com.supporttickets.domain.Ticket;
import com.supporttickets.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static List<Ticket> search(TicketRepository repository, String q, TicketStatus status) {
        return findAll(repository, q, status, Pageable.unpaged(defaultSort())).getContent();
    }

    public static Page<Ticket> findPage(
            TicketRepository repository,
            String q,
            TicketStatus status,
            Pageable pageable
    ) {
        return findAll(repository, q, status, pageable);
    }

    private static Page<Ticket> findAll(
            TicketRepository repository,
            String q,
            TicketStatus status,
            Pageable pageable
    ) {
        Specification<Ticket> spec = Specification.where(hasKeyword(q)).and(hasStatus(status));
        if (pageable.isPaged()) {
            return repository.findAll(spec, pageable);
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort();
        return repository.findAll(spec, Pageable.unpaged(sort));
    }

    private static Sort defaultSort() {
        return Sort.by(
                new Sort.Order(Sort.Direction.DESC, "createdAt"),
                new Sort.Order(Sort.Direction.DESC, "id")
        );
    }

    private static Specification<Ticket> hasKeyword(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            );
        };
    }

    private static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
