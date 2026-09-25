package com.supporttickets.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketListResponse(
        List<TicketResponse> items,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer pageSize,
        String sort
) {
    public TicketListResponse(List<TicketResponse> items) {
        this(items, null, null, null, null, null);
    }
}
