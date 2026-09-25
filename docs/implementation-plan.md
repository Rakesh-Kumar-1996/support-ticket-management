# Implementation Plan — Support Ticket Management v1

**Status:** Ready for task-by-task implementation  
**Does not change:** `spec/*.md`  
**Architecture (frozen):** Next.js/React → Spring Boot REST API → Controller → Service → Repository → PostgreSQL  
**State machine:** enforced only in backend service/domain; UI may hide illegal actions  

**Out of plan (must not appear):** authentication, microservices, Kafka, Redis, Elasticsearch, extra infrastructure, new product features.

---

## How to use this plan

Implement **one task ID at a time**. Do not implement a later task until its dependencies’ definitions of done are met. File paths below are **plan conventions** (not specification). If a path differs, keep the same layering.

### Repository layout (implementation choice, not a spec change)

| Path | Role |
|------|------|
| `backend/` | Spring Boot 3.x, Java 21 |
| `frontend/` | Next.js (App Router) + React |
| `backend/src/main/resources/application.yml` | Runtime config; **no secrets** |
| `backend/src/main/resources/application-test.yml` | H2 for automated tests |
| `.env.example` | Placeholder DB vars only |
| `.gitignore` | `.env`, local YAML overrides, IDE files |

**Tooling choices (implementation, allowed by spec ADs):** Maven or Gradle; Flyway (preferred) or Liquibase; JPA; JUnit 5 + Spring Boot test; Next.js rewrite of `/api` → `http://localhost:8080` (**AD-ARCH-003**). Default ports: API `8080`, UI `3000`.

**No specification conflict identified.** Remaining choices that do not block work are listed at the end of this document.

---

## Phase A — Foundation

### T-001 — Project and build setup

| | |
|--|--|
| **Purpose** | Create the two-app repo skeleton, Java 21 / Node toolchains, and ignore secrets. |
| **Spec** | NFR-001, NFR-005, NFR-007, NFR-009, AD-ARCH-001 |
| **Files** | `backend/pom.xml` or `build.gradle`; `frontend/package.json`; root `README.md` (run instructions only); `.gitignore`; `.env.example` |
| **Depends on** | None |
| **Testing** | `backend` compiles (`./mvnw -q test` or equivalent may be empty); `frontend` `npm install` succeeds |
| **Done** | Two apps exist; Java 21; no committed passwords; README states PostgreSQL for run, H2 for tests |

### T-002 — Backend foundation

| | |
|--|--|
| **Purpose** | Boot a Spring Boot app with `/api` context, Jackson Java 8 date/time (ISO-8601 UTC), no security starter. |
| **Spec** | NFR-001, NFR-004, AD-ARCH-002, AD-NFR-001, BR-007, OOS-001 |
| **Files** | `backend/src/main/java/**/Application.java`; `application.yml` (`server.port`, `spring.jackson.time-zone=UTC`); package root e.g. `com.supporttickets` |
| **Depends on** | T-001 |
| **Testing** | Context loads (`@SpringBootTest` smoke) |
| **Done** | App starts without DB credentials in git; **no** Spring Security filter chain; base path `/api` ready for controllers |

---

## Phase B — Persistence

### T-003 — Database and migrations

| | |
|--|--|
| **Purpose** | PostgreSQL schema for runtime; H2 schema compatible for tests. |
| **Spec** | NFR-002, NFR-003, FR-009, AD-ARCH-004, AD-ARCH-005, `spec/data-model.md` |
| **Files** | Flyway `V1__tickets_and_comments.sql` (or Liquibase); `application.yml` datasource via `SPRING_DATASOURCE_*`; `application-test.yml` H2 |
| **Depends on** | T-002 |
| **Testing** | Migration applies on empty Postgres locally; test profile creates schema |
| **Work** | Tables Ticket + Comment; UUID PKs; FK `comment.ticket_id` → ticket; **AD-DATA-001** ON DELETE CASCADE allowed; index `ticket.status`; index `comment.ticket_id`; timestamptz; varchar lengths per **AD-VAL-003**; status/priority as VARCHAR + CHECK or PG enum |
| **Done** | Runtime profile **cannot** use in-memory H2; placeholders only in git |

### T-004 — Domain model and enums

| | |
|--|--|
| **Purpose** | JPA entities and Java enums matching API strings. |
| **Spec** | `spec/data-model.md`; BR-001–BR-006, BR-008; AD-DATA-004 |
| **Files** | `Ticket.java`, `Comment.java`, `TicketStatus.java`, `TicketPriority.java` |
| **Depends on** | T-003 |
| **Testing** | Enum `name()` equals `OPEN`, `IN_PROGRESS`, etc. |
| **Work** | Ticket 1→N Comment; UUID ids generated in app or DB; `createdAt`/`updatedAt` set in service or `@PrePersist`; **no User entity** |
| **Done** | Entities map 1:1 to spec fields; no extra product columns (no auth, no version required — **AD-SM-001**) |

---

## Phase C — Domain services

### T-005 — Repository layer

| | |
|--|--|
| **Purpose** | Persist/load tickets and comments; list with optional `q` + `status`. |
| **Spec** | Architecture §6; AD-API-004, AD-API-005, AD-API-006; AD-DATA-007; RT-001–RT-006 |
| **Files** | `TicketRepository`, `CommentRepository` (Spring Data or explicit queries) |
| **Depends on** | T-004 |
| **Testing** | `@DataJpaTest` (H2): RT-001–RT-006 |
| **Work** | `findById`; save; list all ordered `createdAt DESC, id DESC`; `ILIKE` title OR description when `q` non-blank; equality on status when present; AND when both; comments by ticketId ordered `createdAt ASC, id ASC` |
| **Done** | Search is SQL, not Elasticsearch; no pagination |

### T-006 — Service layer (without HTTP)

| | |
|--|--|
| **Purpose** | Use-cases: create, list/search/filter, get, update fields, add comment, change status (delegates to T-007). Transactions. |
| **Spec** | Architecture §5; FR-001–FR-010, FR-012–FR-014; BR-001–BR-013; VAL-001–VAL-017 |
| **Files** | `TicketService`; mapping to/from domain; no `HttpServletRequest` |
| **Depends on** | T-005, T-007 (status), T-008 (validation helpers may land with T-008 — implement T-007 first as a pure function, then wire) |
| **Testing** | Service tests with mocked repos **or** wait for T-011/T-012; at minimum compile against T-007 |
| **Work** | Create: ignore client id/status/timestamps (**AD-API-007**); status `OPEN`; priority default `MEDIUM`; trim title only. Update fields: omit = unchanged; `null`/`""` clear description/assignee; **if status key present → validation error** (not transition). Get: 404 if missing. Comments: 404 if ticket missing; trim body only. `updatedAt` on ticket field and status changes, not necessarily on comment-only (spec: updatedAt on **ticket** update including status — comment add does not require bumping ticket `updatedAt`; **do not invent** a bump) |
| **Done** | All use-cases exist; state change only via transition API method |

**Note on `updatedAt` vs comments:** Spec says `updatedAt` is set on insert and every **ticket** update including status. Comment insert is not a ticket field update. Plan: **do not** change ticket `updatedAt` when only adding a comment (no spec conflict; do not add a new requirement to bump it).

### T-007 — State-machine implementation

| | |
|--|--|
| **Purpose** | Closed-world transition table as the **authoritative** domain component used by the service. |
| **Spec** | `spec/state-machine.md`; NFR-006; BR-011; SM matrix |
| **Files** | e.g. `TicketStatusMachine` / `TicketTransitions` (no Spring Web types) |
| **Depends on** | T-004 |
| **Testing** | T-011 (UT-001–UT-004) **in this task or T-011**; must exist before T-012 |
| **Work** | `allows(from, to): boolean` or `assertAllowed` throwing a **domain** `InvalidStatusTransitionException` (not 409 here). Exactly five allowed edges; all others false. Service: load ticket → 404 if missing → if target enum invalid, that is **validation** (400), not this component → if not allowed, throw domain exception → else persist |
| **Done** | No transition logic in controllers or SQL CHECK of from→to; DB may constrain **status values** only (**AD-ARCH-006**) |

### T-008 — DTOs and validation

| | |
|--|--|
| **Purpose** | Request/response types and VAL rules including trim policy and VAL-016. |
| **Spec** | `spec/api-contract.md`; VAL-*; AD-VAL-001, AD-VAL-002, AD-VAL-003; AD-API-007–011 |
| **Files** | `CreateTicketRequest`, `UpdateTicketRequest`, `UpdateStatusRequest`, `CreateCommentRequest`, `TicketResponse`, `TicketDetailResponse`, `CommentResponse`, `TicketListResponse` (`items`) |
| **Depends on** | T-004 |
| **Testing** | Validator unit tests VAL-T-001–003, 006–011 (UT-005); VAL-016 presence of `status` |
| **Work** | **PATCH presence:** detect JSON `status` property (e.g. `JsonNode`, `@JsonAnyGetter` trap, or a `status` field that if non-absent fails). Empty `{}` → 400 (**AD-API-009**). Title/body: trim then blank/length. Description/assignee/author: no trim; `""` → null for description/assignee. Create DTO **without** status or with ignored unknown properties (**AD-API-007**). Invalid enum → 400 not 409 |
| **Done** | DTO JSON matches contract examples; Jackson dates ISO-8601 UTC |

---

## Phase D — HTTP API

### T-009 — Exception handling

| | |
|--|--|
| **Purpose** | One error JSON for 400/404/409; no stack traces or secrets. |
| **Spec** | Architecture §8; api-contract §1; ERR-001–003; VAL-012–014 |
| **Files** | `ApiError`, `FieldErrorItem`, `@RestControllerAdvice`; `TicketNotFoundException`; `InvalidStatusTransitionException`; `MethodArgumentNotValidException` / `HttpMessageNotReadableException` handlers; malformed UUID → 400 |
| **Depends on** | T-008 |
| **Testing** | Covered in T-012 (API-014, ERR-*) |
| **Done** | `status`, `error` (`BAD_REQUEST` \| `NOT_FOUND` \| `CONFLICT`), `code`, `message`, `path`, `fieldErrors`; codes `VALIDATION_ERROR`, `TICKET_NOT_FOUND`, `INVALID_STATUS_TRANSITION`. 409 message names from→to |

### T-010 — REST controllers

| | |
|--|--|
| **Purpose** | Thin HTTP mapping of the six operations. |
| **Spec** | api-contract §3; FR-014; Architecture §4 |
| **Files** | `TicketController` under `/api/tickets` |
| **Depends on** | T-006, T-007, T-008, T-009 |
| **Testing** | T-012 |
| **Work** | `POST /api/tickets` 201; `GET /api/tickets?q&status` 200 `{items}`; `GET /api/tickets/{id}` 200 detail+comments; `PATCH /api/tickets/{id}` 200 fields; `PATCH /api/tickets/{id}/status` 200; `POST /api/tickets/{id}/comments` 201. Optional `Location` on 201. CORS **or** rely on Next proxy (**AD-ARCH-003**) — if CORS, allow frontend origin only, still **no auth**. No delete routes |
| **Done** | Controllers only bind + call service; no transition table in the controller |

---

## Phase E — Backend tests

### T-011 — Backend unit tests

| | |
|--|--|
| **Purpose** | Transition table and trim/blank helpers without HTTP. |
| **Spec** | test-strategy §1; UT-001–UT-005 |
| **Files** | `TicketStatusMachineTest`, validation helper tests |
| **Depends on** | T-007, T-008 |
| **Testing** | This task **is** the tests |
| **Done** | Five valid edges allowed; SM-I examples denied; self-transition denied; extra invalid cells denied; blank title/body rejected |

### T-012 — Backend integration / API tests

| | |
|--|--|
| **Purpose** | HTTP + DB (H2) prove contract, search/filter, validation, **mandatory** state-machine tests. |
| **Spec** | NFR-008; AC-010, AC-011, AC-013, AC-014, AC-016; API-001–015; SM-V-001–005; SM-I-001–005; VAL-T-*; SF-001–006; ERR-001–003 |
| **Files** | `@SpringBootTest` + `MockMvc`/`WebTestClient`; test profile H2 |
| **Depends on** | T-010, T-011 |
| **Testing** | This task **is** the tests |
| **Work** | Include API-015 (`status` on field PATCH → 400, row unchanged). SM-I-001: walk OPEN→IN_PROGRESS→RESOLVED→CLOSED then PATCH OPEN. Invalid enum on status endpoint → 400 not 409. Persist-nothing assertions on 409 |
| **Done** | All IDs above green on H2; these tests **do not** satisfy PER-001 |

---

## Phase F — Frontend

### T-013 — Frontend project setup

| | |
|--|--|
| **Purpose** | Next.js app, API client, proxy/CORS to `/api`. |
| **Spec** | NFR-005; AD-ARCH-003; AD-NFR-002 |
| **Files** | `frontend/` App Router; `next.config` rewrite `/api/:path*` → `http://localhost:8080/api/:path*`; `lib/api.ts`; shared TypeScript types matching contract |
| **Depends on** | T-010 (API running locally for manual check) |
| **Testing** | Dev server starts; one fetch to list (may be empty) |
| **Done** | Browser talks to API **only** via HTTP JSON; no localStorage as system of record |

### T-014 — Ticket list / search / filter UI

| | |
|--|--|
| **Purpose** | List tickets; `q` + `status` combinable; empty/loading states. |
| **Spec** | FR-002, FR-006–008; AC-002, AC-007–009; OOS-003, OOS-009 |
| **Files** | e.g. `app/page.tsx`; search input; single status `<select>` |
| **Depends on** | T-013 |
| **Testing** | FE-002, FE-005 (T-021 / manual) |
| **Done** | One status value max; no pagination UI; row links to detail |

### T-015 — Ticket creation UI

| | |
|--|--|
| **Purpose** | Create ticket; do not send status/id/timestamps. |
| **Spec** | FR-001; AC-001; BR-001, BR-003–BR-006 |
| **Files** | create form; POST `/api/tickets` |
| **Depends on** | T-013 |
| **Testing** | FE-001; FE-008 |
| **Done** | Title required in UI (backend still authoritative); default priority MEDIUM if omitted; redirect to list or detail after 201 |

### T-016 — Ticket detail UI

| | |
|--|--|
| **Purpose** | Show all ticket fields and comments. |
| **Spec** | FR-003; AC-003; AD-API-003 |
| **Files** | `app/tickets/[id]/page.tsx`; GET `/api/tickets/{id}` |
| **Depends on** | T-013 |
| **Testing** | FE-002, FE-009 |
| **Done** | Comments listed oldest-first (API order); 404 path shows API `message` (may share T-020) |

### T-017 — Ticket editing UI

| | |
|--|--|
| **Purpose** | PATCH title, description, priority, assignee; never send `status` on this request. |
| **Spec** | FR-004; AC-004, AC-005; VAL-016; BR-013 |
| **Files** | edit form on detail; PATCH `/api/tickets/{id}` |
| **Depends on** | T-016 |
| **Testing** | FE-003 |
| **Done** | Clear assignee/description via null or `""`; allowed in all statuses; **no** status field in PATCH body |

### T-018 — Status transition UI

| | |
|--|--|
| **Purpose** | Change status via dedicated endpoint; optional hide of illegal targets. |
| **Spec** | FR-012–FR-014; AC-010, AC-011; state-machine; UI may hide illegal actions |
| **Files** | status control; PATCH `/api/tickets/{id}/status` **only** |
| **Depends on** | T-016 |
| **Testing** | FE-006, FE-007 |
| **Done** | Valid edges work; if user still submits illegal (or API 409), T-020 shows message; backend remains authority |

### T-019 — Comment UI

| | |
|--|--|
| **Purpose** | Append comments; no edit/delete. |
| **Spec** | FR-005; AC-006; BR-008–BR-010; OOS-005 |
| **Files** | comment list + form; POST `/api/tickets/{id}/comments` |
| **Depends on** | T-016 |
| **Testing** | FE-004 |
| **Done** | Author optional string; body required; works in all ticket statuses |

### T-020 — Frontend error handling

| | |
|--|--|
| **Purpose** | Meaningful 400/404/409 from `message` + `fieldErrors`. |
| **Spec** | FR-011; AC-015; api-contract §5; ERR-004 |
| **Files** | `lib/api-error.ts`; banner/inline field errors on all forms |
| **Depends on** | T-014–T-019 (wire into each) |
| **Testing** | FE-007–FE-009; ERR-004 |
| **Done** | Blank title ≠ illegal transition ≠ missing ticket; raw `Request failed` without body is insufficient |

### T-021 — Frontend tests

| | |
|--|--|
| **Purpose** | Cover FE-001–FE-009 as automated **or** a manual AC script (spec allows manual UI). |
| **Spec** | test-strategy §9 |
| **Files** | Playwright/Cypress **or** `docs/manual-frontend-ac.md` checklist |
| **Depends on** | T-020 |
| **Testing** | This task |
| **Done** | Either green E2E against running stack **or** signed-off manual script; **backend SM tests remain mandatory** (already T-012) |

---

## Phase G — Release verification

### T-022 — PostgreSQL persistence / restart verification

| | |
|--|--|
| **Purpose** | Prove AC-012 / PER-001 on real PostgreSQL. |
| **Spec** | FR-009; NFR-002; PER-001 |
| **Files** | `docs/verification-restart.md` (steps + expected GET); optional Testcontainers test |
| **Depends on** | T-010, T-003 |
| **Testing** | Create ticket+comment → stop API → start → GET same id/fields/comments |
| **Done** | H2 in-memory **not** used for this check |

### T-023 — Security / secrets review

| | |
|--|--|
| **Purpose** | AC-017 / PER-002 / NFR-007. |
| **Spec** | NFR-007; AD-ARCH-005 |
| **Files** | `.gitignore`; grep of `application*.yml`, `.env` |
| **Depends on** | T-001, T-003, T-022 |
| **Testing** | Repo search: no real passwords, cloud keys, or prod JDBC URLs with credentials |
| **Done** | Only placeholders committed; 500 responses do not include connection strings |

### T-024 — Final acceptance testing

| | |
|--|--|
| **Purpose** | Walk AC-001–AC-017 against running UI + API + PostgreSQL. |
| **Spec** | requirements §5 |
| **Files** | Checklist results in `docs/acceptance-log.md` (optional) |
| **Depends on** | T-012, T-021, T-022, T-023 |
| **Testing** | Full AC list |
| **Done** | Every AC checked; failures filed as fixes **without** adding product scope |

### T-025 — Final code / specification review

| | |
|--|--|
| **Purpose** | Confirm code matches frozen spec; no spec edits unless a true conflict is found (then stop). |
| **Spec** | All of `spec/`; OOS-*; architecture exclusions |
| **Files** | Review notes only |
| **Depends on** | T-024 |
| **Testing** | Diff vs spec: layers, five transitions, VAL-016, trim rules, no auth/Kafka/Redis/ES |
| **Done** | Implementation follows this plan’s order and spec; secrets still absent |

---

## Dependency graph (summary)

```
T-001 → T-002 → T-003 → T-004 → T-005
                              → T-007 → T-011
                     T-004 → T-008 → T-009
T-005 + T-007 + T-008 → T-006 → T-010 → T-012
T-010 → T-013 → T-014, T-015, T-016
T-016 → T-017, T-018, T-019
T-014–T-019 → T-020 → T-021
T-010 + T-003 → T-022 → T-023 → T-024 → T-025
```

Suggested implementation slices for an agent:

1. T-001 … T-012 (backend complete including mandatory SM tests)  
2. T-013 … T-021 (frontend)  
3. T-022 … T-025 (verify)

---

## Spec vs plan

No conflict requiring a spec change. Plan-only clarifications (not new product requirements):

| Topic | Plan choice |
|-------|-------------|
| Comment does not bump ticket `updatedAt` | Spec lists ticket updates + status; comment is a separate resource |
| Flyway + Maven + App Router | Allowed implementation choices |
| Next.js `/api` rewrite | **AD-ARCH-003** |
| PATCH JSON presence for `status` and partial fields | Implement with nullable/presence types; behaviour is VAL-016 / VAL-006 |
| Manual frontend AC script | Allowed by test-strategy §9 |

---

## Remaining implementation blockers

**None that block starting T-001.** Non-blocking local choices:

- Whether unknown JSON properties other than documented fields are ignored (create already ignores server-owned fields).
- Whether list `q` is trimmed before `ILIKE` (blank `q` means no keyword filter — treat whitespace-only `q` as blank is a small local choice; either is consistent if documented in code comments).
- Playwright vs manual FE tests.
- Testcontainers vs documented manual PER-001.

Do not resolve these by editing `spec/`. Pick one in code and proceed.

## Prompt 005 — Implement T-001 Only

Date: 2026-09-25

We are implementing the approved plan in:
docs/implementation-plan.md

Implement ONLY T-001.

Do NOT implement T-002 or any later task.

Read these files first:
- docs/implementation-plan.md
- spec/requirements.md
- spec/architecture.md

### T-001 — Project and build setup

Create the two-app repository foundation:

backend/
frontend/

### Backend

- Java 21
- Spring Boot project
- Use Maven
- Create only the minimum project/build structure required for T-001.
- Do NOT implement business logic.
- Do NOT create entities, repositories, services, controllers, APIs, database schema, or migrations.
- Do NOT add Spring Security.

### Frontend

- Next.js
- React
- TypeScript
- App Router
- Create only the project/build foundation.
- Do NOT create ticket management UI.

### Root files

Create/update only what is required for T-001:

- .gitignore
- .env.example
- README.md

README should contain only basic local setup/prerequisites and mention:
- Java 21
- Node.js
- PostgreSQL for runtime
- H2 will be used for automated tests

### Security

Never add real:
- passwords
- database credentials
- API keys
- tokens
- secrets

.env.example must contain placeholders only.

### Strict scope

Do NOT introduce:
- authentication
- Kafka
- Redis
- Elasticsearch
- microservices
- Docker infrastructure
- ticket business logic
- database tables
- Flyway/Liquibase migrations
- JPA entities
- repositories
- services
- REST controllers
- ticket APIs
- ticket UI

Do NOT modify anything under spec/.

### Prompt history

Append this exact Prompt 005 to:
docs/prompt-history.md

Also add a short result entry describing what was actually implemented and verified.

### Verification

Before finishing:

1. Verify Java 21.
2. Verify backend builds successfully.
3. Verify frontend dependencies/project build foundation successfully.
4. Verify no secrets were added.
5. Verify spec/ files were not modified.

Do not silently fix or change any specification.

At the end, report:

- files created/changed
- commands executed
- verification results
- any problems encountered
- confirmation that only T-001 was implemented

Do not implement any other task.

## Prompt 006 — Implement T-002 Only

Date: 2026-09-25

T-001 has been reviewed and approved.

Implement ONLY T-002 from:
docs/implementation-plan.md

Do NOT implement T-003 or any later task.

Read:
- docs/implementation-plan.md
- spec/requirements.md
- spec/architecture.md
- spec/api-contract.md

### T-002 — Backend Foundation

Create the minimal runnable Spring Boot backend foundation.

Requirements:

1. Create the Spring Boot application entry point.

2. Use:
   - Java 21
   - existing Maven setup from T-001
   - existing Spring Boot version from T-001

3. Configure the backend with:
   - server port: 8080
   - `/api` base path as specified by the architecture
   - Jackson Java time support
   - UTC timezone for JSON date/time handling

4. The application must start successfully.

5. Add the minimum Spring Boot configuration required for the above.

6. Add a minimal Spring Boot context-load test.

7. Keep the backend free of Spring Security.

8. Do NOT add:
   - database configuration requiring real credentials
   - PostgreSQL schema
   - Flyway/Liquibase
   - JPA entities
   - repositories
   - services
   - controllers for tickets
   - ticket APIs
   - DTOs
   - validation logic
   - state-machine logic
   - frontend changes
   - authentication
   - Kafka
   - Redis
   - Elasticsearch
   - Docker infrastructure

9. Do not put real credentials/secrets anywhere.

10. Do not modify anything under `spec/`.

11. Do not modify `docs/implementation-plan.md`.

### Verification

Run:

- Java version check
- `./mvnw -q test`
- verify the Spring Boot context-load test passes
- if practical, verify the application can start successfully

Do not require a database to start at this stage unless the existing configuration makes that unavoidable.

### Prompt history

Append this exact Prompt 006 to:

docs/prompt-history.md

Then add a short result entry describing:
- what was implemented
- verification performed
- any issues encountered

### Final response

Report:

- files created/changed
- commands executed
- verification results
- whether the application started successfully
- any problems
- confirmation that ONLY T-002 was implemented

If anything in the specification conflicts with this task, STOP and report the conflict instead of changing the specification.
