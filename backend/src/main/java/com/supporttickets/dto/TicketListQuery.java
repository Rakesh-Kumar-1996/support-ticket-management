package com.supporttickets.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record TicketListQuery(
        String keyword,
        boolean paginated,
        Pageable pageable,
        String sortParam
) {
    public static TicketListQuery unpaginated(String keyword, Sort sort) {
        return new TicketListQuery(keyword, false, Pageable.unpaged(sort), sortParam(sort));
    }

    public static TicketListQuery paginated(String keyword, Pageable pageable) {
        return new TicketListQuery(keyword, true, pageable, sortParam(pageable.getSort()));
    }

    private static String sortParam(Sort sort) {
        if (sort.isUnsorted()) {
            return "createdAt,desc";
        }
        Sort.Order primary = sort.iterator().next();
        return primary.getProperty() + "," + primary.getDirection().name().toLowerCase();
    }
}
