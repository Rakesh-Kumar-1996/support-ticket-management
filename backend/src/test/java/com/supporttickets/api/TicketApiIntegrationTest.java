package com.supporttickets.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supporttickets.dto.TicketRequestValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void api001_createTicketReturns201WithOpenAndDefaultPriority() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Cannot reset password","description":"Reset email never arrives."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tickets/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Cannot reset password")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void api002_blankTitleReturns400ValidationError() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")));
    }

    @Test
    void listSearchFilterAndOrdering() throws Exception {
        String alphaId = createTicket("Alpha ticket", "needle in haystack", null);
        createTicket("Beta ticket", "unrelated body", null);

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(get("/tickets").param("q", "NEEDLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title", is("Alpha ticket")));

        mockMvc.perform(get("/tickets").param("q", "haystack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));

        String gammaId = createTicket("Gamma", "other", null);
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(gammaId));

        mockMvc.perform(get("/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));

        mockMvc.perform(get("/tickets").param("q", "Beta").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title", is("Beta ticket")));
    }

    @Test
    void getDetailIncludesCommentsInOrder() throws Exception {
        String id = createTicket("Detail ticket", null, null);

        mockMvc.perform(post("/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"First comment\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Second comment\"}"))
                .andExpect(status().isCreated());

        MvcResult detail = mockMvc.perform(get("/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(2)))
                .andExpect(jsonPath("$.comments[0].body", is("First comment")))
                .andExpect(jsonPath("$.comments[1].body", is("Second comment")))
                .andReturn();

        JsonNode comments = objectMapper.readTree(detail.getResponse().getContentAsString()).get("comments");
        String firstId = comments.get(0).get("id").asText();
        String secondId = comments.get(1).get("id").asText();
        assertTrue(firstId.compareTo(secondId) <= 0 || comments.get(0).get("createdAt").asText()
                .compareTo(comments.get(1).get("createdAt").asText()) <= 0);
    }

    @Test
    void patchFieldsUpdatesAndClearsNullableFields() throws Exception {
        String id = createTicket("Original", "keep", "alex");

        mockMvc.perform(patch("/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"  Updated title  ",
                                  "description":"",
                                  "priority":"HIGH",
                                  "assignee":""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated title")))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void patchEmptyBodyReturns400() throws Exception {
        String id = createTicket("Title", null, null);
        mockMvc.perform(patch("/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));
    }

    @Test
    void patchWithStatusReturns400AndDoesNotPersist() throws Exception {
        String id = createTicket("Stable title", null, null);

        mockMvc.perform(patch("/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Changed\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)))
                .andExpect(jsonPath("$.message", is(TicketRequestValidator.STATUS_ON_FIELD_PATCH_MESSAGE)));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Stable title")))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void addCommentAndValidation() throws Exception {
        String id = createTicket("Comment host", null, null);

        mockMvc.perform(post("/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"  trimmed body  \",\"author\":\"  sam  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body", is("trimmed body")))
                .andExpect(jsonPath("$.author", is("  sam  ")));

        mockMvc.perform(post("/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));
    }

    @Test
    void unknownTicketReturns404() throws Exception {
        UUID missing = UUID.randomUUID();
        mockMvc.perform(get("/tickets/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.TICKET_NOT_FOUND)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));

        mockMvc.perform(post("/tickets/" + missing + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.TICKET_NOT_FOUND)));
    }

    @Test
    void malformedUuidReturns400() throws Exception {
        mockMvc.perform(get("/tickets/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));
    }

    @Test
    void invalidListStatusQueryReturns400() throws Exception {
        mockMvc.perform(get("/tickets").param("status", "OPENED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,IN_PROGRESS",
            "OPEN,CANCELLED",
            "IN_PROGRESS,RESOLVED",
            "IN_PROGRESS,CANCELLED",
            "RESOLVED,CLOSED"
    })
    void smValidTransitionsReturn200(String from, String to) throws Exception {
        String id = ticketInStatus(from);
        mockMvc.perform(patch("/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(to)));
    }

    @Test
    void smHappyPathOpenThroughClosed() throws Exception {
        String id = createTicket("Lifecycle", null, null);
        assertStatus(id, "IN_PROGRESS");
        assertStatus(id, "RESOLVED");
        assertStatus(id, "CLOSED");
        mockMvc.perform(get("/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));
    }

    @Test
    void smClosedToOpenReturns409() throws Exception {
        String id = createTicket("Close me", null, null);
        assertStatus(id, "IN_PROGRESS");
        assertStatus(id, "RESOLVED");
        assertStatus(id, "CLOSED");

        mockMvc.perform(patch("/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.INVALID_STATUS_TRANSITION)))
                .andExpect(jsonPath("$.error", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("Transition from CLOSED to OPEN is not allowed.")));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(jsonPath("$.status", is("CLOSED")));
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,RESOLVED",
            "OPEN,CLOSED",
            "IN_PROGRESS,CLOSED",
            "RESOLVED,CANCELLED",
            "CANCELLED,OPEN"
    })
    void smInvalidTransitionsReturn409AndLeaveStatus(String from, String to) throws Exception {
        String id = ticketInStatus(from);
        mockMvc.perform(patch("/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.INVALID_STATUS_TRANSITION)))
                .andExpect(jsonPath("$.message", containsString("Transition from " + from + " to " + to)));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(jsonPath("$.status", is(from)));
    }

    @Test
    void invalidStatusValueOnStatusEndpointReturns400Not409() throws Exception {
        String id = createTicket("Status check", null, null);
        mockMvc.perform(patch("/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(RestExceptionHandler.VALIDATION_ERROR)));

        mockMvc.perform(get("/tickets/" + id))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    private String createTicket(String title, String description, String assignee) throws Exception {
        StringBuilder json = new StringBuilder("{\"title\":\"").append(title).append("\"");
        if (description != null) {
            json.append(",\"description\":\"").append(description).append("\"");
        }
        if (assignee != null) {
            json.append(",\"assignee\":\"").append(assignee).append("\"");
        }
        json.append("}");
        MvcResult result = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void assertStatus(String id, String status) throws Exception {
        mockMvc.perform(patch("/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(status)));
    }

    private String ticketInStatus(String status) throws Exception {
        String id = createTicket("Ticket in " + status, null, null);
        if ("OPEN".equals(status)) {
            return id;
        }
        if ("IN_PROGRESS".equals(status)) {
            assertStatus(id, "IN_PROGRESS");
            return id;
        }
        if ("RESOLVED".equals(status)) {
            assertStatus(id, "IN_PROGRESS");
            assertStatus(id, "RESOLVED");
            return id;
        }
        if ("CLOSED".equals(status)) {
            assertStatus(id, "IN_PROGRESS");
            assertStatus(id, "RESOLVED");
            assertStatus(id, "CLOSED");
            return id;
        }
        if ("CANCELLED".equals(status)) {
            assertStatus(id, "CANCELLED");
            return id;
        }
        throw new IllegalArgumentException("Unsupported status: " + status);
    }
}
