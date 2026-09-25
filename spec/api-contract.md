# API Contract

**Base path:** `/api`  
**Format:** JSON, UTF-8  
**Auth:** none (v1)

**Architectural decisions**

| ID | Decision |
|----|----------|
| AD-API-001 | Successful GET/PATCH return 200. Successful POST create ticket and add comment return 201 with `Location` header optional. |
| AD-API-002 | List and detail `createdAt`/`updatedAt` are ISO-8601 UTC. |
| AD-API-003 | List tickets does **not** embed comments. Get ticket **does** embed `comments` (so FR-003 is one round trip). |
| AD-API-004 | List default order: `createdAt` descending, then `id` descending. |
| AD-API-005 | Keyword match is case-insensitive substring on `title` **or** `description` (SQL `ILIKE` / equivalent). Both fields are searched; a ticket matches if either field contains `q`. |
| AD-API-006 | Query `q` and `status` are both optional; when both are present they are **AND**ed. |

---

## 1. Error format

All 400, 404, and 409 responses use this body:

```json
{
  "status": 409,
  "error": "CONFLICT",
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Transition from CLOSED to OPEN is not allowed.",
  "path": "/api/tickets/550e8400-e29b-41d4-a716-446655440000/status",
  "fieldErrors": [
    {
      "field": "title",
      "message": "Title must not be blank."
    }
  ]
}
```

| JSON field | Meaning |
|------------|---------|
| status | HTTP status code |
| error | Short reason phrase (`BAD_REQUEST`, `NOT_FOUND`, `CONFLICT`) |
| code | Stable machine code (see below) |
| message | Human-readable; UI may show this as-is |
| path | Request path |
| fieldErrors | Optional; empty array or omitted if not field-level |

**Codes**

| code | HTTP | When |
|------|------|------|
| VALIDATION_ERROR | 400 | VAL rules, malformed UUID, malformed JSON, unknown enum on input, `status` on field-update PATCH |
| TICKET_NOT_FOUND | 404 | Well-formed UUID, no ticket |
| INVALID_STATUS_TRANSITION | 409 | Target status is a valid enum but the edge is not allowed |

`fieldErrors` is used for VALIDATION_ERROR when a field is at fault.

---

## 2. Shared resource shapes

### Ticket (list item — no comments)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "status": "OPEN",
  "priority": "MEDIUM",
  "assignee": "alex",
  "createdAt": "2026-09-24T10:00:00Z",
  "updatedAt": "2026-09-24T10:00:00Z"
}
```

`description` and `assignee` may be `null`.

### Ticket detail

Same fields plus:

```json
"comments": [
  {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "ticketId": "550e8400-e29b-41d4-a716-446655440000",
    "body": "Asked the user for a screenshot.",
    "author": "alex",
    "createdAt": "2026-09-24T11:00:00Z"
  }
]
```

`comments` is `[]` when none exist. `author` may be `null`.

---

## 3. Endpoints

### 3.1 Create ticket

| | |
|--|--|
| **Endpoint** | `/api/tickets` |
| **Method** | `POST` |
| **Purpose** | Create a ticket with status `OPEN` |

**Request body**

```json
{
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "priority": "HIGH",
  "assignee": "alex"
}
```

| Field | Required | Validation |
|-------|----------|------------|
| title | yes | non-blank after trim; **stored trimmed** |
| description | no | stored as supplied (not trimmed) |
| priority | no | `LOW` \| `MEDIUM` \| `HIGH`; default `MEDIUM` |
| assignee | no | stored as supplied (not trimmed) |

Do not send `id`, `status`, `createdAt`, `updatedAt`. If sent, they are ignored (**AD-API-007**).

**Success:** `201 Created` — Ticket (list-item shape; `comments` omitted or `[]`).

**Errors:** `400` VALIDATION_ERROR.

---

### 3.2 List tickets (includes search and status filter)

| | |
|--|--|
| **Endpoint** | `/api/tickets` |
| **Method** | `GET` |
| **Purpose** | List all tickets, optionally filtered |

**Query parameters**

| Name | Required | Validation / behaviour |
|------|----------|------------------------|
| q | no | Keyword; case-insensitive substring on title or description. Missing or blank: no keyword filter. |
| status | no | Exact status enum. Invalid value: 400. |

Pagination is out of scope: return all matches.

**Success:** `200 OK`

```json
{
  "items": [ { "...": "ticket list item" } ]
}
```

**AD-API-008:** Envelope `{ "items": [ ... ] }` rather than a bare array.

**Errors:** `400` if `status` is not a valid enum.

---

### 3.3 Get ticket

| | |
|--|--|
| **Endpoint** | `/api/tickets/{id}` |
| **Method** | `GET` |
| **Purpose** | Ticket details including comments |

**Path:** `id` must be a UUID.

**Success:** `200 OK` — Ticket detail.

**Errors:** `400` malformed id; `404` unknown id.

---

### 3.4 Update ticket fields

| | |
|--|--|
| **Endpoint** | `/api/tickets/{id}` |
| **Method** | `PATCH` |
| **Purpose** | Update title, description, priority, and/or assignee. **Does not change status.** |

**Request body** (all fields optional; at least one should be present — **AD-API-009:** empty object is 400)

```json
{
  "title": "Password reset emails",
  "description": null,
  "priority": "LOW",
  "assignee": "sam"
}
```

| Field | If present |
|-------|------------|
| title | non-blank after trim; stored trimmed |
| description | string or JSON `null` (`null` or `""` clears per **AD-VAL-002**); otherwise stored as supplied (not trimmed) |
| priority | valid enum |
| assignee | string or JSON `null` (`null` or `""` clears per **AD-VAL-002**); otherwise stored as supplied (not trimmed) |
| status | **Must not be present.** If present → 400 `VALIDATION_ERROR` (**VAL-016**). Status changes only via §3.5. |

**Success:** `200 OK` — Ticket list-item shape (comments not required).

**Errors:** `400`, `404`. Example `message` when `status` is included: `Status cannot be changed on this endpoint. Use PATCH /api/tickets/{id}/status.`

---

### 3.5 Update status

| | |
|--|--|
| **Endpoint** | `/api/tickets/{id}/status` |
| **Method** | `PATCH` |
| **Purpose** | Apply a state-machine transition |

**Request body**

```json
{
  "status": "IN_PROGRESS"
}
```

| Field | Required | Validation |
|-------|----------|------------|
| status | yes | Must be a TicketStatus value |

**Success:** `200 OK` — Ticket with updated `status` and `updatedAt`.

**Errors:**

| HTTP | code | Condition |
|------|------|-----------|
| 400 | VALIDATION_ERROR | Missing/invalid `status` enum, malformed id |
| 404 | TICKET_NOT_FOUND | Unknown ticket |
| 409 | INVALID_STATUS_TRANSITION | Enum valid but edge not allowed |

Example 409 message: `Transition from OPEN to RESOLVED is not allowed.`

---

### 3.6 Add comment

| | |
|--|--|
| **Endpoint** | `/api/tickets/{id}/comments` |
| **Method** | `POST` |
| **Purpose** | Append a comment |

**Request body**

```json
{
  "body": "Asked the user for a screenshot.",
  "author": "alex"
}
```

| Field | Required | Validation |
|-------|----------|------------|
| body | yes | non-blank after trim; **stored trimmed** |
| author | no | string; stored as supplied (not trimmed) |

**Success:** `201 Created` — Comment object.

**Errors:** `400` validation; `404` unknown ticket; `400` malformed id.

There is no `GET /comments` collection besides embedding on get ticket (**AD-API-011**).

---

## 4. Status code summary

| Code | Meaning in this API |
|------|---------------------|
| 200 | GET or PATCH success |
| 201 | POST create ticket or comment |
| 400 | Validation, malformed UUID, invalid enum, malformed JSON |
| 404 | Ticket does not exist |
| 409 | Illegal status transition |
| 500 | Unexpected (not specified beyond not leaking secrets) |

---

## 5. Frontend mapping

The UI must show `message` (and field messages when present) for 400/404/409 so operators understand blank title vs illegal transition vs missing ticket.
