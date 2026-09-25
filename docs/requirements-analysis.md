# Requirements Analysis — Support Ticket Management System

**Document type:** Requirements analysis (Prompt 001)  
**Date:** 2026-09-25  
**Status:** Draft for specification — not an implementation plan  
**Stack (stated):** Java 21, Spring Boot, PostgreSQL/H2, REST API, React/Next.js  
**Process:** Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix  

This document analyses the given requirements only. It does **not** specify implementation design, class names, or code. Items marked **stipulated** come from the prompt. Items marked **proposed** are working interpretations that must be confirmed before specification freeze.

---

## 1. Functional requirements

| ID | Requirement | Source | Acceptance criteria linkage |
|----|-------------|--------|-----------------------------|
| FR-01 | Create a ticket from the UI | Stipulated | Ticket can be created from UI |
| FR-02 | List tickets | Stipulated | Tickets can be listed |
| FR-03 | View ticket details | Stipulated | Ticket details can be viewed |
| FR-04 | Update ticket title, description, priority, and assignee | Stipulated | Ticket fields can be updated; assignee can be changed |
| FR-05 | Add comments to a ticket | Stipulated | Comments can be added |
| FR-06 | Search tickets by keyword | Stipulated | Search works |
| FR-07 | Filter tickets by status | Stipulated | Status filter works |
| FR-08 | Persist tickets (and related data) in a database | Stipulated | Data survives application restart |
| FR-09 | Validate input on the backend | Stipulated | Backend validation works |
| FR-10 | Display meaningful errors in the UI | Stipulated | UI shows meaningful errors |
| FR-11 | Apply ticket status changes according to the backend state machine | Implied by state machine + AC | Valid status transitions work |
| FR-12 | Reject invalid status transitions on the backend | Stipulated | Invalid status transitions are rejected by backend |
| FR-13 | Prove state-machine behaviour with integration tests | Stipulated | State-machine integration tests pass |
| FR-14 | Keep secrets out of version control | Stipulated | No secrets are committed |

### 1.1 Capability notes (not yet frozen)

- **Create (FR-01):** A user must be able to submit a new ticket from the frontend. Initial status is **proposed** as `OPEN` (not stated).
- **List (FR-02):** A collection view of tickets. Sort order, pagination, and which fields appear in the list are **not stated**.
- **Details (FR-03):** A single-ticket view including identity, mutable fields, status, and comments (comments are otherwise unusable).
- **Update (FR-04):** Title, description, priority, and assignee are independently updatable. Status is **not** listed as an updatable field in FR-04; status change is a separate capability (FR-11).
- **Comments (FR-05):** Append-only comments are **proposed** (edit/delete not required).
- **Search (FR-06):** Keyword search. Target fields, matching rules, and composition with status filter are **not stated**.
- **Filter (FR-07):** Restrict the list by ticket status. Multi-status vs single-status is **not stated**.
- **Persistence (FR-08):** Database-backed; restart must not lose tickets, comments, or assignments.
- **Validation (FR-09):** Server is the authority; the UI must not be the only gate.
- **Errors (FR-10):** User-visible messages that explain what failed and why, including validation and illegal transitions.

**Out of stated scope (unless later added):** authentication, roles, notifications, attachments, SLA, audit log UI, bulk operations, ticket deletion, comment edit/delete, reopening closed/cancelled tickets.

---

## 2. Non-functional requirements

| ID | Requirement | Category | Source |
|----|-------------|----------|--------|
| NFR-01 | Backend implemented with Java 21 and Spring Boot | Technology | Stipulated |
| NFR-02 | Data stored in PostgreSQL and/or H2 | Technology / persistence | Stipulated |
| NFR-03 | Backend exposed as a REST API | Interface | Stipulated |
| NFR-04 | Frontend implemented with React / Next.js | Technology | Stipulated |
| NFR-05 | Data durability across process restart | Reliability | Stipulated (AC) |
| NFR-06 | Backend is the enforcement point for validation and state transitions | Integrity | Stipulated |
| NFR-07 | UI communicates failures in human-readable form | Usability | Stipulated |
| NFR-08 | No secrets committed to the repository | Security | Stipulated |
| NFR-09 | State-machine behaviour is covered by backend integration tests | Quality | Stipulated |
| NFR-10 | Artefacts support Spec-Driven Development demonstration | Process | Stipulated |

### 2.1 Implied but unspecified NFRs

These commonly apply to this stack and should be decided in specification:

- API error format (e.g. consistent JSON problem body, HTTP status mapping).
- CORS / how the Next.js app calls the API (same-origin proxy vs separate origin).
- Timezone and datetime representation (UTC ISO-8601 **proposed**).
- Performance / scale (single-user demo vs concurrent users) — **not stated**.
- Accessibility of the UI — **not stated**.
- Logging without leaking secrets — **not stated**, related to NFR-08.

---

## 3. Business rules

| ID | Rule | Notes |
|----|------|-------|
| BR-01 | A ticket is the primary work item of the system. | Stipulated by feature set |
| BR-02 | A ticket has a lifecycle expressed as status. | Stipulated |
| BR-03 | Status may only change along allowed transitions. | Stipulated |
| BR-04 | Terminal statuses (`CLOSED`, `CANCELLED`) cannot return to `OPEN`. | Stipulated examples |
| BR-05 | `RESOLVED` cannot return to `OPEN`. | Stipulated example |
| BR-06 | Title, description, priority, and assignee may be changed (subject to validation). | Stipulated; whether this is allowed in all statuses is **unspecified** |
| BR-07 | Comments may be added to a ticket. | Stipulated; whether allowed on closed/cancelled tickets is **unspecified** |
| BR-08 | Search is keyword-based. | Stipulated |
| BR-09 | Listing can be restricted by status. | Stipulated |
| BR-10 | Persistence is mandatory; in-memory-only storage does not satisfy restart AC. | Stipulated |
| BR-11 | Client-side checks do not replace backend rules. | Stipulated |
| BR-12 | Secrets (DB passwords, API keys, etc.) must not appear in committed files. | Stipulated |

**Proposed default business interpretations (to confirm):**

- New tickets start in `OPEN`.
- Assignee may be empty (unassigned) or a named person/identifier.
- Priority is a constrained set (not free text).
- Comments are immutable after creation.
- Tickets are not deleted; cancellation is the abandon path.

---

## 4. State-machine rules

### 4.1 Stated states

`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`

### 4.2 Stated happy-path chain

`OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`

### 4.3 Stated cancel paths

- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `CANCELLED`

### 4.4 Stated illegal examples

| From | To | Result |
|------|-----|--------|
| CLOSED | OPEN | Invalid |
| RESOLVED | OPEN | Invalid |
| CANCELLED | OPEN | Invalid |

### 4.5 Complete transition matrix (stated vs inferred)

Legend: **Allow** = stipulated; **Deny** = stipulated by example or by “only listed transitions”; **Unspecified** = must be decided.

| From \ To | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|-----------|------|-------------|----------|--------|-----------|
| OPEN | Unspecified (self) | **Allow** | Unspecified | Unspecified | **Allow** |
| IN_PROGRESS | Unspecified | Unspecified (self) | **Allow** | Unspecified | **Allow** |
| RESOLVED | **Deny** | Unspecified | Unspecified (self) | **Allow** | Unspecified |
| CLOSED | **Deny** | Unspecified | Unspecified | Unspecified (self) | Unspecified |
| CANCELLED | **Deny** | Unspecified | Unspecified | Unspecified | Unspecified (self) |

### 4.6 Proposed closed-world rule (recommended for specification)

Unless a later requirement adds transitions, **only** the five stipulated edges are legal:

1. `OPEN` → `IN_PROGRESS`
2. `OPEN` → `CANCELLED`
3. `IN_PROGRESS` → `RESOLVED`
4. `IN_PROGRESS` → `CANCELLED`
5. `RESOLVED` → `CLOSED`

Under this rule, all other pairs (including skips such as `OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`, `RESOLVED` → `CANCELLED`, any backward move, and self-transitions) are **rejected**.

Integration tests (FR-13) should cover every **allowed** edge and a representative set of **illegal** edges, including the three stipulated examples.

### 4.7 Enforcement

- The backend must reject illegal transitions (HTTP error **proposed** `409 Conflict` or `422 Unprocessable Entity` — choose in specification).
- The UI must show a meaningful message when rejection occurs (FR-10).
- The UI may hide illegal actions, but hiding is not a substitute for backend rejection.

---

## 5. Validation requirements

Backend must validate before persisting. Exact constraints are largely **unspecified**; the table records stipulated intent and proposed rules.

| ID | Field / action | Stipulated | Proposed validation |
|----|----------------|------------|---------------------|
| VAL-01 | Title | Required conceptually (update implies it exists) | Non-blank; max length (e.g. 200) |
| VAL-02 | Description | Same | Optional or required — **undecided**; max length if present |
| VAL-03 | Priority | Updatable | Enum only (e.g. LOW / MEDIUM / HIGH / CRITICAL) — **undecided set** |
| VAL-04 | Assignee | Updatable | Optional; format (email vs display name vs user id) **undecided** |
| VAL-05 | Status | State machine | Must be a known status; transition must be legal for current status |
| VAL-06 | Comment body | Add comments | Non-blank; max length |
| VAL-07 | Search keyword | Search by keyword | Empty vs too-short keyword behaviour **undecided** |
| VAL-08 | Status filter | Filter by status | Unknown status value → 400 **proposed** |
| VAL-09 | Identifiers | View/update/comment | Unknown ticket id → 404 **proposed** |
| VAL-10 | Malformed JSON / wrong types | Backend validation | 400 with field-level errors **proposed** |
| VAL-11 | Illegal transition | Stipulated reject | Dedicated error code/message, not a generic 500 |

**Error payload (proposed):** machine-readable `code`, human-readable `message`, optional `fieldErrors[]` so the UI can bind messages to inputs (FR-10).

**Principle:** Frontend validation may improve UX; it must not be the only check.

---

## 6. API requirements

The prompt requires a REST API. Concrete paths are **not stipulated**. The following is a **requirements-level resource model** for the next specification, not an implementation.

### 6.1 Resources

| Resource | Purpose |
|----------|---------|
| Ticket collection | Create, list, search, filter |
| Ticket instance | Get details, update fields, change status |
| Comment sub-resource | Add and list comments |

### 6.2 Required operations (capability mapping)

| Capability | HTTP intent (proposed) | Notes |
|------------|------------------------|-------|
| Create ticket | `POST /tickets` | Returns created ticket |
| List tickets | `GET /tickets` | Supports status filter |
| Search | `GET /tickets?q=` or dedicated search | Composition with filter **undecided** |
| Get details | `GET /tickets/{id}` | Includes comments or comments fetched separately |
| Update fields | `PATCH /tickets/{id}` | Title, description, priority, assignee |
| Change status | `POST /tickets/{id}/status` or PATCH status field | Separate from field update is safer for the state machine |
| Add comment | `POST /tickets/{id}/comments` | |

### 6.3 API behavioural requirements

- JSON request/response (**proposed**).
- Validation failures return structured errors, not empty 400s.
- Illegal transitions are explicitly rejected.
- List/search/filter results must reflect persisted data.
- Idempotency of GET; POST create is not idempotent unless specified later.

### 6.4 Out of API scope unless added

Auth headers, pagination contract, sorting contract, ETags/optimistic locking, OpenAPI as a delivery artefact (useful for SDD but not required by the prompt).

---

## 7. Data requirements

### 7.1 Persistence

- Relational database: PostgreSQL and/or H2 (H2 typically for local/test, PostgreSQL for durable demo — **deployment split not stated**).
- Restart durability implies file- or server-backed storage, not ephemeral-only H2 in-memory **unless** that configuration is never used for the “survives restart” demonstration.

### 7.2 Entities (proposed)

**Ticket**

| Attribute | Required? | Notes |
|-----------|-----------|-------|
| id | Yes | Stable identifier |
| title | Yes (proposed) | |
| description | Undecided | |
| status | Yes | One of the five states |
| priority | Yes if FR-04 implies always present | Enum **undecided** |
| assignee | Optional (proposed) | |
| createdAt | Proposed | Audit |
| updatedAt | Proposed | Audit |
| createdBy | Unspecified | No auth model |

**Comment**

| Attribute | Required? | Notes |
|-----------|-----------|-------|
| id | Yes | |
| ticketId | Yes | FK to ticket |
| body | Yes | |
| author | Unspecified | No user model |
| createdAt | Proposed | |

### 7.3 Relationships

- Ticket 1 → N Comments.
- Assignee is **not** specified as a User entity; it may be a string on the ticket until a user directory is added.

### 7.4 Secrets and configuration

- Connection strings, passwords, and cloud keys live in environment or uncommitted local config.
- Sample `.env.example` without real secrets is compatible with FR-14.

---

## 8. Frontend requirements

| ID | Requirement |
|----|-------------|
| UI-01 | Create-ticket form (title and other create fields once specified) |
| UI-02 | Ticket list |
| UI-03 | Ticket detail view |
| UI-04 | Edit title, description, priority, assignee |
| UI-05 | Change status using only (or clearly labelling) valid transitions; still handle backend rejection |
| UI-06 | Add comment on a ticket |
| UI-07 | Keyword search control on the list |
| UI-08 | Status filter control on the list |
| UI-09 | Surface backend validation and transition errors in readable language |
| UI-10 | Next.js / React as the UI technology |

**Unspecified UI details:** routing, auth screens, empty states, loading states, pagination UI, which columns appear in the list, whether search and filter combine, toast vs inline errors.

**Error UX (stipulated intent):** messages must be meaningful — e.g. “Cannot reopen a closed ticket” rather than “Request failed”.

---

## 9. Testing requirements

| ID | Layer | Requirement |
|----|-------|-------------|
| T-01 | Backend integration | State-machine: all allowed transitions succeed |
| T-02 | Backend integration | Stipulated illegal transitions fail (`CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`) |
| T-03 | Backend integration | Additional illegal transitions under the closed-world rule (once frozen) |
| T-04 | Backend | Input validation (blank title, invalid priority, unknown id, malformed body) |
| T-05 | Persistence | Data remains after restart (manual AC or automated with real DB) |
| T-06 | API | Create, list, get, update, comment, search, filter |
| T-07 | Frontend | Create/list/detail/update/comment/search/filter from UI (acceptance) |
| T-08 | Frontend | Meaningful error display for validation and illegal transition |
| T-09 | Repo | Scan or review that secrets are not committed |

The prompt **requires** state-machine **integration** tests; it does not require a specific test framework. Unit tests for the transition table are valuable but do not replace T-01–T-03.

UI end-to-end tests are implied by UI acceptance criteria but not named as a mandatory automated suite.

---

## 10. Ambiguities and missing requirements

1. **Identity and access:** No login, roles (customer vs agent), or permission model.
2. **User / assignee directory:** Assignee is a field with no user CRUD.
3. **Create payload:** Which fields are mandatory at creation besides an implied title.
4. **Priority values:** Set and default are missing.
5. **Status vs field updates:** Whether status can be PATCHed with title, or must be a dedicated operation.
6. **Mutability by status:** Can title/assignee/comments change after `RESOLVED` / `CLOSED` / `CANCELLED`?
7. **Skip transitions:** `OPEN` → `RESOLVED` or `CLOSED` not mentioned.
8. **`RESOLVED` → `CANCELLED`:** Not mentioned.
9. **Self-transitions:** Same status submitted again — no-op vs 409.
10. **Search semantics:** Fields, case sensitivity, partial match, comments included or not.
11. **Filter + search together.**
12. **Pagination, sort, max list size.**
13. **Comment author** without authentication.
14. **Ticket numbering** (UUID vs sequential).
15. **H2 vs PostgreSQL** for which environments; in-memory H2 vs restart AC.
16. **Deletion** of tickets.
17. **Optimistic concurrency** (two agents editing).
18. **Internationalisation** of error messages.
19. **API versioning and base path.**
20. **Observability** (health endpoint, logging).

---

## 11. Important edge cases

| Edge case | Risk if ignored |
|-----------|-----------------|
| Illegal transition from UI and from raw API | UI-only guards fail FR-12 |
| Update of unknown ticket id | Ambiguous 500 vs 404 |
| Comment on missing ticket | Same |
| Empty or whitespace-only title/comment | Weak validation |
| Extremely long text fields | DB/UI breakage |
| Search with empty query | Unbounded list vs error |
| Invalid status filter value | Silent empty list vs 400 |
| Concurrent status changes | Lost updates / illegal intermediate state |
| Restart with H2 in-memory | False pass on persistence AC |
| Special characters in search | Injection / unexpected empty results |
| Duplicate submit on create | Duplicate tickets |
| Status change with extra unknown JSON fields | Silent ignore vs reject |
| Assignee cleared to empty | Unassign vs validation error |
| Filter `CANCELLED` / `CLOSED` | Hidden tickets if default list is “open only” (default list scope unspecified) |

---

## 12. Questions to resolve before implementation

These should be answered (or explicitly deferred with a default) in the **Specification** step.

1. Is the **closed-world** transition table (five edges only) accepted?
2. What is the **initial status** on create?
3. What are the **priority** values and default?
4. Is **description** required?
5. Is **assignee** a free-text string or a reference to a user? Must users exist first?
6. Is there **authentication** in v1, or is this a single-trust demo?
7. Who is the **comment author** if there is no login?
8. May fields and comments be changed when status is `RESOLVED`, `CLOSED`, or `CANCELLED`?
9. Is **status change** a dedicated API operation?
10. Do **search** and **status filter** combine on one list request?
11. Which ticket fields (and comments?) does **keyword search** cover?
12. Is the list **paginated**?
13. H2 for tests only, PostgreSQL for the app, or both first-class?
14. HTTP status for illegal transitions: **409** or **422**?
15. Ticket **id** format?
16. Should the specification freeze an **error JSON schema** for the UI?
17. Any **reopen** path in a later phase, or permanently out of scope?

---

## Traceability: acceptance criteria → requirements

| Acceptance criterion | Primary IDs |
|----------------------|-------------|
| Ticket can be created from UI | FR-01, UI-01 |
| Tickets can be listed | FR-02, UI-02 |
| Ticket details can be viewed | FR-03, UI-03 |
| Ticket fields can be updated | FR-04, UI-04 |
| Assignee can be changed | FR-04, UI-04 |
| Comments can be added | FR-05, UI-06 |
| Search works | FR-06, UI-07 |
| Status filter works | FR-07, UI-08 |
| Valid status transitions work | FR-11, §4 |
| Invalid status transitions rejected by backend | FR-12, VAL-11 |
| Data survives application restart | FR-08, NFR-05 |
| Backend validation works | FR-09, §5 |
| UI shows meaningful errors | FR-10, UI-09 |
| State-machine integration tests pass | FR-13, T-01–T-03 |
| No secrets are committed | FR-14, NFR-08, T-09 |

---

## Recommended defaults if specification needs a freeze

Use only if stakeholders accept them; they are **not** in the original prompt.

| Topic | Default |
|-------|---------|
| Transitions | Closed-world: five edges only |
| Initial status | `OPEN` |
| Priority | `LOW`, `MEDIUM`, `HIGH`; default `MEDIUM` |
| Description | Optional |
| Assignee | Optional string |
| Auth | Out of v1; comment `author` optional string or `"anonymous"` |
| Terminal mutability | No status change; field/comment updates still allowed (or freeze as “comments allowed, fields allowed”) — **still needs a product choice** |
| Search | Case-insensitive match on title and description; combinable with status |
| Illegal transition HTTP | 409 |
| Missing ticket HTTP | 404 |
| Validation HTTP | 400 with field errors |
| IDs | Server-generated UUID |
| Persistence | PostgreSQL for run; H2 only if file-mode or tests that do not claim restart durability |

---

## Next SDD step

Produce a **specification** that freezes: domain model, transition table, validation rules, API contract, UI screens, test plan, and answers to §12. Do not implement until that specification is accepted.
