# Test Strategy

**System:** Support Ticket Management v1

Tests must make the behaviour in the other spec files observable. H2 may be used for automated tests; persistence/restart verification uses PostgreSQL.

---

## 1. Unit testing

**Scope:** pure domain logic, especially the transition table (given current + target → allow/deny).

| ID | Test |
|----|------|
| UT-001 | Each of the five valid edges returns allowed. |
| UT-002 | Each specified invalid example returns denied. |
| UT-003 | Self-transition denied. |
| UT-004 | Representative extra invalid cells (e.g. `CLOSED` → `IN_PROGRESS`, `CANCELLED` → `CLOSED`). |
| UT-005 | Blank title / blank comment body rejected at the validation helper or equivalent. |

Unit tests do not replace API integration tests for the state machine.

---

## 2. Repository testing

Useful where search/filter SQL would otherwise only be proven through HTTP.

| ID | Test |
|----|------|
| RT-001 | Save and load ticket by id. |
| RT-002 | Status filter returns only that status. |
| RT-003 | Keyword matches title. |
| RT-004 | Keyword matches description and not unrelated tickets. |
| RT-005 | Keyword + status together (AND). |
| RT-006 | Comments saved with ticketId are loaded for that ticket only. |

---

## 3. Controller / API testing

Slice or MockMvc/WebTestClient (or equivalent) against the HTTP contract.

| ID | Test |
|----|------|
| API-001 | POST `/api/tickets` 201, status `OPEN`, default priority `MEDIUM` when omitted. |
| API-002 | POST with blank title → 400 VALIDATION_ERROR, fieldErrors on title. |
| API-003 | GET `/api/tickets` 200, `items` array. |
| API-004 | GET `/api/tickets/{id}` 200 with comments. |
| API-005 | GET unknown UUID → 404 TICKET_NOT_FOUND. |
| API-006 | GET malformed id → 400. |
| API-007 | PATCH fields updates title/description/priority/assignee; status unchanged. |
| API-008 | PATCH empty body → 400. |
| API-009 | PATCH `/status` valid edge → 200 and new status. |
| API-010 | PATCH `/status` invalid edge → 409 INVALID_STATUS_TRANSITION, status unchanged. |
| API-011 | POST comment 201; GET ticket includes the comment. |
| API-012 | POST comment blank body → 400. |
| API-013 | POST comment unknown ticket → 404. |
| API-014 | Error bodies match the JSON schema (status, error, code, message, path). |
| API-015 | PATCH `/api/tickets/{id}` with `status` in the body → 400 VALIDATION_ERROR; persisted status unchanged; message names the dedicated status endpoint. |

---

## 4. Integration testing

Spring Boot test with real HTTP + database (H2 or PostgreSQL).

Covers wiring: controller → service → repository → DB.

Prefer this layer for search/filter and state machine if API slice tests mock the repository.

---

## 5. State-machine integration tests

**Required (NFR-008 / AC-016).** Use the API (or service + DB), not UI.

### Valid (must all pass)

| ID | Transition |
|----|------------|
| SM-V-001 | OPEN → IN_PROGRESS |
| SM-V-002 | OPEN → CANCELLED |
| SM-V-003 | IN_PROGRESS → RESOLVED |
| SM-V-004 | IN_PROGRESS → CANCELLED |
| SM-V-005 | RESOLVED → CLOSED |

Each: create/setup ticket in `from`, PATCH status to `to`, assert 200 and persisted status.

### Invalid examples (must all pass)

| ID | Transition | Expect |
|----|------------|--------|
| SM-I-001 | CLOSED → OPEN | 409, remains CLOSED |
| SM-I-002 | RESOLVED → OPEN | 409, remains RESOLVED |
| SM-I-003 | CANCELLED → OPEN | 409, remains CANCELLED |
| SM-I-004 | OPEN → RESOLVED | 409, remains OPEN |
| SM-I-005 | RESOLVED → CANCELLED | 409, remains RESOLVED |

Setup for SM-I-001: walk OPEN → IN_PROGRESS → RESOLVED → CLOSED then attempt OPEN.

---

## 6. Validation tests

| ID | Case | Expect |
|----|------|--------|
| VAL-T-001 | Missing title on create | 400 |
| VAL-T-002 | Whitespace-only title | 400 |
| VAL-T-003 | Invalid priority string | 400 |
| VAL-T-004 | Invalid status on PATCH status | 400 (not 409) |
| VAL-T-005 | Invalid `status` query on list | 400 |
| VAL-T-006 | Whitespace-only comment body | 400 |
| VAL-T-007 | Title longer than max after trim (AD-VAL-003) | 400 |
| VAL-T-008 | Title with leading/trailing spaces | 201/200; stored title is trimmed |
| VAL-T-009 | Description with leading/trailing spaces | stored exactly as supplied |
| VAL-T-010 | Assignee/author with leading/trailing spaces | stored exactly as supplied |
| VAL-T-011 | Comment body with padding | stored trimmed |

---

## 7. Search and filter tests

| ID | Case | Expect |
|----|------|--------|
| SF-001 | `q` matches title (case-insensitive) | ticket in `items` |
| SF-002 | `q` matches description | ticket in `items` |
| SF-003 | `q` matches neither | ticket absent |
| SF-004 | `status=OPEN` excludes other statuses | |
| SF-005 | `q` + `status` together | AND semantics |
| SF-006 | No query params | all tickets (v1, no pagination) |

---

## 8. Persistence / restart verification

| ID | Case |
|----|------|
| PER-001 | Create ticket + comment against **PostgreSQL**, stop API process, start again, GET ticket: same id, fields, and comments. |
| PER-002 | Secrets not in git (review `.env`, `application.properties` / yaml: no real passwords). |

PER-001 may be a documented manual checklist if CI has no PostgreSQL; automated is preferred. H2 in-memory does **not** satisfy PER-001.

---

## 9. Frontend testing

Not a named mandatory E2E framework in the product requirements. For acceptance:

| ID | Case |
|----|------|
| FE-001 | Create ticket from UI; it appears in the list. |
| FE-002 | List, open detail. |
| FE-003 | Edit title, description, priority, assignee. |
| FE-004 | Add comment; visible on detail. |
| FE-005 | Search and status filter (including combined). |
| FE-006 | Valid transition control works. |
| FE-007 | Illegal transition (if triggered, or via API then refresh) shows a meaningful 409 message. |
| FE-008 | Validation errors show meaningful 400 text (e.g. blank title). |
| FE-009 | Opening a missing id shows meaningful 404 text. |

Implement with component tests and/or Playwright/Cypress as an implementation choice. Manual QA against AC-* is acceptable if automated UI tests are not yet added; backend SM tests remain mandatory.

---

## 10. Error-handling tests

| ID | Case |
|----|------|
| ERR-001 | 400 body includes `code` VALIDATION_ERROR and a usable `message`. |
| ERR-002 | 404 body includes TICKET_NOT_FOUND. |
| ERR-003 | 409 body includes INVALID_STATUS_TRANSITION and names the illegal transition in `message`. |
| ERR-004 | UI maps those three to distinct user-visible text (frontend test or manual AC-015). |

---

## 11. Suggested pyramid

1. Unit: transition table  
2. API/integration: contract + SM-V-* + SM-I-* + search/filter + validation  
3. Few UI tests or a manual AC script  
4. One PostgreSQL restart check
