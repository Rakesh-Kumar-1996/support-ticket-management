package com.supporttickets.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supporttickets.domain.TicketPriority;
import com.supporttickets.domain.TicketStatus;
import com.supporttickets.domain.TicketValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketRequestValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void valT001_missingTitleOnCreate() {
        CreateTicketRequest request = new CreateTicketRequest();
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateCreate(request)
        );
        assertEquals("title", ex.getField());
    }

    @Test
    void valT002_whitespaceOnlyTitle() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("   ");
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateCreate(request)
        );
        assertEquals("title", ex.getField());
        assertEquals("Title must not be blank.", ex.getMessage());
    }

    @Test
    void valT003_invalidPriorityString() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Valid");
        request.setPriority("URGENT");
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateCreate(request)
        );
        assertEquals("priority", ex.getField());
    }

    @Test
    void valT006_whitespaceOnlyCommentBody() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setBody(" \n\t ");
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateComment(request)
        );
        assertEquals("body", ex.getField());
    }

    @Test
    void ut005_blankTitleAndBodyRejected() {
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.requireTrimmedTitle(""));
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.requireTrimmedBody(""));
    }

    @Test
    void valT007_titleLongerThanMaxAfterTrim() {
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.requireTrimmedTitle("  " + "a".repeat(201) + "  ")
        );
        assertEquals("title", ex.getField());
    }

    @Test
    void valT008_titleTrimmedForStorage() {
        assertEquals("Hello", TicketRequestValidator.requireTrimmedTitle("  Hello  "));
    }

    @Test
    void valT011_commentBodyTrimmedForStorage() {
        assertEquals("Note", TicketRequestValidator.requireTrimmedBody("  Note  "));
    }

    @Test
    void descriptionAndAssigneeLengthsUseSuppliedString() {
        assertDoesNotThrow(() -> TicketRequestValidator.validateOptionalDescription("  padded  "));
        assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateOptionalDescription("x".repeat(10_001))
        );
        assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateOptionalAssignee("x".repeat(201))
        );
    }

    @Test
    void val016_statusPresentOnFieldPatch() throws Exception {
        UpdateTicketRequest request = objectMapper.readValue(
                "{\"title\":\"ok\",\"status\":\"OPEN\"}",
                UpdateTicketRequest.class
        );
        assertTrue(request.isStatusPresent());
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateUpdate(request)
        );
        assertEquals("status", ex.getField());
        assertEquals(TicketRequestValidator.STATUS_ON_FIELD_PATCH_MESSAGE, ex.getMessage());
    }

    @Test
    void emptyPatchIsInvalid() throws Exception {
        UpdateTicketRequest request = objectMapper.readValue("{}", UpdateTicketRequest.class);
        assertFalse(request.hasFieldUpdate());
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.validateUpdate(request));
    }

    @Test
    void createIgnoresServerOwnedFields() throws Exception {
        CreateTicketRequest request = objectMapper.readValue(
                """
                {
                  "title":"New",
                  "id":"550e8400-e29b-41d4-a716-446655440000",
                  "status":"CLOSED",
                  "createdAt":"2020-01-01T00:00:00Z",
                  "updatedAt":"2020-01-01T00:00:00Z"
                }
                """,
                CreateTicketRequest.class
        );
        TicketRequestValidator.validateCreate(request);
        assertEquals("New", request.getTitle());
        assertNull(request.getPriority());
        assertEquals(TicketPriority.MEDIUM, TicketRequestValidator.parsePriorityOrDefault(request.getPriority()));
    }

    @Test
    void invalidStatusIsValidationNotTransition() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("NOPE");
        TicketValidationException ex = assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.validateStatusChange(request)
        );
        assertEquals("status", ex.getField());
    }

    @Test
    void parseOptionalListStatus() {
        assertNull(TicketRequestValidator.parseOptionalListStatus(null));
        assertNull(TicketRequestValidator.parseOptionalListStatus("  "));
        assertEquals(TicketStatus.OPEN, TicketRequestValidator.parseOptionalListStatus("OPEN"));
        assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.parseOptionalListStatus("open")
        );
    }

    @Test
    void emptyStringClearsToNull() {
        assertNull(TicketRequestValidator.clearable(""));
        assertEquals("  keep  ", TicketRequestValidator.clearable("  keep  "));
    }

    @Test
    void parsePageRejectsInvalidValues() {
        assertEquals(1, TicketRequestValidator.parsePage(null));
        assertEquals(2, TicketRequestValidator.parsePage(2));
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.parsePage(0));
    }

    @Test
    void parseSizeRejectsInvalidValues() {
        assertEquals(TicketRequestValidator.DEFAULT_PAGE_SIZE, TicketRequestValidator.parseSize(null));
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.parseSize(0));
        assertThrows(
                TicketValidationException.class,
                () -> TicketRequestValidator.parseSize(TicketRequestValidator.MAX_PAGE_SIZE + 1)
        );
    }

    @Test
    void parseSortAcceptsWhitelistAndRejectsInvalid() {
        assertEquals("createdAt", TicketRequestValidator.parseSort(null).iterator().next().getProperty());
        assertEquals("updatedAt", TicketRequestValidator.parseSort("updatedAt,asc").iterator().next().getProperty());
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.parseSort("invalid,desc"));
        assertThrows(TicketValidationException.class, () -> TicketRequestValidator.parseSort("createdAt,up"));
    }

    @Test
    void parseListQueryDefaultsToUnpaginated() {
        var query = TicketRequestValidator.parseListQuery(null, null, null);
        assertFalse(query.paginated());
    }

    @Test
    void parseListQueryPaginatesWhenPageOrSizeProvided() {
        var query = TicketRequestValidator.parseListQuery(2, 10, "title,asc");
        assertTrue(query.paginated());
        assertEquals(1, query.pageable().getPageNumber());
        assertEquals(10, query.pageable().getPageSize());
    }
}
