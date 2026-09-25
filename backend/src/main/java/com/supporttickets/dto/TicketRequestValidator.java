package com.supporttickets.dto;

import com.supporttickets.domain.TicketPriority;
import com.supporttickets.domain.TicketStatus;
import com.supporttickets.domain.TicketValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

public final class TicketRequestValidator {

    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 10_000;
    public static final int ASSIGNEE_MAX = 200;
    public static final int BODY_MAX = 5_000;
    public static final int AUTHOR_MAX = 200;

    public static final String STATUS_ON_FIELD_PATCH_MESSAGE =
            "Status cannot be changed on this endpoint. Use PATCH /api/tickets/{id}/status.";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "priority", "status"
    );

    private TicketRequestValidator() {
    }

    public static String requireTrimmedTitle(String title) {
        if (title == null) {
            throw new TicketValidationException("title", "Title is required.");
        }
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            throw new TicketValidationException("title", "Title must not be blank.");
        }
        if (trimmed.length() > TITLE_MAX) {
            throw new TicketValidationException("title", "Title must be at most " + TITLE_MAX + " characters.");
        }
        return trimmed;
    }

    public static void validateOptionalDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX) {
            throw new TicketValidationException("description", "Description must be at most " + DESCRIPTION_MAX + " characters.");
        }
    }

    public static void validateOptionalAssignee(String assignee) {
        if (assignee != null && assignee.length() > ASSIGNEE_MAX) {
            throw new TicketValidationException("assignee", "Assignee must be at most " + ASSIGNEE_MAX + " characters.");
        }
    }

    public static TicketPriority parsePriorityOrDefault(String priority) {
        if (priority == null) {
            return TicketPriority.MEDIUM;
        }
        return parsePriority(priority);
    }

    public static TicketPriority parsePriority(String priority) {
        if (priority == null) {
            throw new TicketValidationException("priority", "Priority is required.");
        }
        try {
            return TicketPriority.valueOf(priority);
        } catch (IllegalArgumentException ex) {
            throw new TicketValidationException("priority", "Priority must be one of LOW, MEDIUM, HIGH.");
        }
    }

    public static TicketStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new TicketValidationException("status", "Status is required.");
        }
        try {
            return TicketStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new TicketValidationException("status", "Status is not a valid ticket status.");
        }
    }

    public static TicketStatus parseOptionalListStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseStatus(status);
    }

    public static TicketListQuery parseListQuery(Integer page, Integer size, String sort) {
        Sort resolvedSort = parseSort(sort);
        if (page == null && size == null) {
            return TicketListQuery.unpaginated(null, resolvedSort);
        }
        int resolvedPage = parsePage(page);
        int resolvedSize = parseSize(size);
        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedSize, resolvedSort);
        return TicketListQuery.paginated(null, pageable);
    }

    public static int parsePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new TicketValidationException("page", "Page must be at least 1.");
        }
        return page;
    }

    public static int parseSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1) {
            throw new TicketValidationException("size", "Page size must be at least 1.");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new TicketValidationException("size", "Page size must be at most " + MAX_PAGE_SIZE + ".");
        }
        return size;
    }

    public static Sort parseSort(String sort) {
        String raw = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort.trim();
        String[] parts = raw.split(",", 2);
        if (parts.length != 2) {
            throw new TicketValidationException("sort", "Sort must be in the form field,direction (e.g. createdAt,desc).");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new TicketValidationException(
                    "sort",
                    "Sort field must be one of: createdAt, updatedAt, title, priority, status."
            );
        }
        Sort.Direction resolvedDirection;
        if ("asc".equals(direction)) {
            resolvedDirection = Sort.Direction.ASC;
        } else if ("desc".equals(direction)) {
            resolvedDirection = Sort.Direction.DESC;
        } else {
            throw new TicketValidationException("sort", "Sort direction must be asc or desc.");
        }
        return Sort.by(new Sort.Order(resolvedDirection, field), new Sort.Order(Sort.Direction.DESC, "id"));
    }

    public static String requireTrimmedBody(String body) {
        if (body == null) {
            throw new TicketValidationException("body", "Body is required.");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new TicketValidationException("body", "Body must not be blank.");
        }
        if (trimmed.length() > BODY_MAX) {
            throw new TicketValidationException("body", "Body must be at most " + BODY_MAX + " characters.");
        }
        return trimmed;
    }

    public static void validateOptionalAuthor(String author) {
        if (author != null && author.length() > AUTHOR_MAX) {
            throw new TicketValidationException("author", "Author must be at most " + AUTHOR_MAX + " characters.");
        }
    }

    public static void validateCreate(CreateTicketRequest request) {
        requireTrimmedTitle(request.getTitle());
        validateOptionalDescription(request.getDescription());
        validateOptionalAssignee(request.getAssignee());
        parsePriorityOrDefault(request.getPriority());
    }

    public static void validateUpdate(UpdateTicketRequest request) {
        if (request.isStatusPresent()) {
            throw new TicketValidationException("status", STATUS_ON_FIELD_PATCH_MESSAGE);
        }
        if (!request.hasFieldUpdate()) {
            throw new TicketValidationException("At least one of title, description, priority, or assignee must be provided.");
        }
        if (request.isTitlePresent()) {
            requireTrimmedTitle(request.getTitle());
        }
        if (request.isDescriptionPresent() && request.getDescription() != null) {
            validateOptionalDescription(request.getDescription());
        }
        if (request.isPriorityPresent()) {
            parsePriority(request.getPriority());
        }
        if (request.isAssigneePresent() && request.getAssignee() != null) {
            validateOptionalAssignee(request.getAssignee());
        }
    }

    public static TicketStatus validateStatusChange(UpdateStatusRequest request) {
        return parseStatus(request.getStatus());
    }

    public static void validateComment(CreateCommentRequest request) {
        requireTrimmedBody(request.getBody());
        validateOptionalAuthor(request.getAuthor());
    }

    public static String clearable(String value) {
        if (value != null && value.isEmpty()) {
            return null;
        }
        return value;
    }
}
