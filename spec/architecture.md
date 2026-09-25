# Architecture Specification

**System:** Support Ticket Management v1  
**Style:** Two-process modular monolith (web UI + REST API). No microservices.

---

## 1. System overview

```
[ Browser ]
    |
    v
[ Next.js / React frontend ]
    |  HTTP JSON
    v
[ Spring Boot REST API ]
    |
    +-- Controller layer
    +-- Service layer (validation orchestration, state machine)
    +-- Repository layer
    |
    v
[ PostgreSQL ]
```

Automated tests may substitute **H2** for PostgreSQL. The **running application** uses PostgreSQL only.

---

## 2. Frontend (Next.js / React)

**Responsibility**

- Render screens: create ticket, list (search + status filter), detail, edit fields, change status, add comment.
- Call the REST API.
- Show loading and empty states as needed for a usable UI.
- Display **meaningful** messages from the API error JSON (`message` and, when present, `fieldErrors`).
- Optionally hide status actions that are illegal for the current status (**UX only**).

**Must not**

- Be the only enforcer of the state machine or field validation.
- Store tickets only in browser memory as the system of record.

**Architectural decisions**

| ID | Decision |
|----|----------|
| AD-ARCH-001 | Frontend and backend are separate deployable applications. |
| AD-ARCH-002 | API base path is `/api` on the backend. |
| AD-ARCH-003 | Locally, the Next.js app may proxy `/api` to Spring Boot to simplify CORS; if not proxied, the backend enables CORS for the frontend origin. |

---

## 3. REST API

**Responsibility**

- Expose HTTP JSON endpoints defined in `spec/api-contract.md`.
- Map HTTP status codes: 200/201 success, 400 validation, 404 unknown ticket, 409 illegal transition.
- Remain stateless aside from the database.

No API gateway, no separate auth service.

---

## 4. Controller layer

**Responsibility**

- Declare routes, HTTP methods, and request/response DTOs.
- Bind query parameters (`q`, `status`) and path `{id}`.
- Delegate to the service layer.
- Translate service outcomes to HTTP responses.
- Must not contain business rules (transition table, “title not blank” beyond framework annotations that duplicate VAL rules).

Controllers stay thin.

---

## 5. Service layer

**Responsibility**

- Create tickets with `status = OPEN`, generated UUID, timestamps.
- Apply field updates (title, description, priority, assignee) without changing status. If a field-update request includes `status`, fail validation (400); do not apply a transition.
- **Enforce the state machine** (see §9) only for the dedicated status endpoint, before any status persist.
- Add comments only if the ticket exists.
- Load tickets for list/detail/search/filter.
- Orchestrate transactions (one use case, one transaction as a default).

**Must not** talk to HTTP types (avoid `HttpServletRequest` in services).

---

## 6. Repository layer

**Responsibility**

- Persist and load `Ticket` and `Comment`.
- Implement keyword search on title and description, optional status equality filter, combination of both.
- Isolate SQL/JPA from the rest of the application.

No direct HTTP or UI knowledge.

---

## 7. PostgreSQL

**Responsibility**

- System of record for tickets and comments.
- Survive process restart of the API and of the frontend.

**Architectural decisions**

| ID | Decision |
|----|----------|
| AD-ARCH-004 | Schema is owned by the application (migration tool such as Flyway/Liquibase, or equivalent). Exact tool is an implementation choice. |
| AD-ARCH-005 | Connection URL, username, and password come from environment or uncommitted local config. Example files contain placeholders only. |

H2 is allowed in automated tests so CI need not require PostgreSQL, provided tests that claim **restart durability** use PostgreSQL or an equivalent durable store.

---

## 8. Exception handling

**Responsibility**

- Central handler maps domain/validation failures to the **same JSON error shape** (`spec/api-contract.md`).
- Do not leak stack traces or secrets to clients.
- Distinct mappings:
  - validation / malformed id → 400
  - ticket not found → 404
  - illegal status transition → 409

Unexpected failures may be 500; they are not a product requirement to expose internally.

---

## 9. Validation

**Layers**

1. **Request parsing:** invalid JSON / type mismatch → 400.
2. **Bean/input validation:** title/body blank (using trimmed content), illegal enum, length (**AD-VAL-003**), `status` present on field PATCH (**VAL-016**).
3. **Domain rules in service:** ticket exists; transition legal on the status endpoint only.

**Trim:** persist trimmed `title` and comment `body`. Do not trim `description`, `assignee`, or `author`.

Frontend validation is optional and redundant.

---

## 10. State-machine enforcement

**Authoritative point:** backend service (or a small domain component used only by the service). Not the controller, not the database check constraint alone, and not the UI.

**Behaviour (dedicated status endpoint only)**

1. Load current ticket status.
2. If target status is not a valid enum value → 400 (validation).
3. If `(current, target)` is not an allowed edge → 409, no persist.
4. If allowed → persist new status and `updatedAt`.

A field-update PATCH that includes `status` is rejected at validation (400) and never reaches this procedure.

A database enum or check constraint may **mirror** statuses as defense in depth (**AD-ARCH-006**); it does not replace the service-level transition table.

Self-transitions (e.g. `OPEN` → `OPEN`) are invalid (closed-world table).

---

## 11. What this architecture excludes

- Extra runtimes (Redis, Kafka, Elasticsearch) for v1 search — search is SQL against title and description.
- Shared libraries beyond the two apps unless the implementer needs a trivial client type.
- Kubernetes/service mesh as a requirement.
