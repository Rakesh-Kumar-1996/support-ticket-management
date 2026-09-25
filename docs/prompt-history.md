# Prompt History

## Prompt 001 — Initial Requirements Analysis

Date: 2026-09-24

### Prompt

We are building a Support Ticket Management System using Java 21, Spring Boot, PostgreSQL/H2, REST API, and React/Next.js.

The goal of this project is to demonstrate Spec-Driven Development using Cursor and GitHub Copilot.

The required workflow is:

Requirement
→ Specification
→ Plan / Tasks
→ Implementation
→ Testing
→ Review
→ Fix

Do NOT implement any application code yet.

Analyse the requirements below and produce a structured requirements analysis.

Functional requirements:
1. Create a ticket.
2. List tickets.
3. View ticket details.
4. Update title, description, priority and assignee.
5. Add comments.
6. Search tickets by keyword.
7. Filter tickets by status.
8. Persist data in a database.
9. Validate input at the backend.
10. Display meaningful errors in the UI.

Backend state machine:
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED

Invalid transitions must be rejected by the backend.

Examples:
CLOSED → OPEN = invalid
RESOLVED → OPEN = invalid
CANCELLED → OPEN = invalid

Acceptance criteria:
- Ticket can be created from UI.
- Tickets can be listed.
- Ticket details can be viewed.
- Ticket fields can be updated.
- Assignee can be changed.
- Comments can be added.
- Search works.
- Status filter works.
- Valid status transitions work.
- Invalid status transitions are rejected by backend.
- Data survives application restart.
- Backend validation works.
- UI shows meaningful errors.
- State-machine integration tests pass.
- No secrets are committed.

Analyse the requirements and identify:
1. Functional requirements
2. Non-functional requirements
3. Business rules
4. State-machine rules
5. Validation requirements
6. API requirements
7. Data requirements
8. Frontend requirements
9. Testing requirements
10. Ambiguities or missing requirements
11. Important edge cases
12. Questions that should be resolved before implementation

Do not write implementation code.
Do not create the complete application.
Focus only on requirements analysis.

## Prompt 002 — Specification Creation

Date: 2026-09-24

### Prompt

We have completed and reviewed the requirements analysis for the Support Ticket Management System.

Do NOT implement application code.

Create the project specifications based on the requirements and the following human-approved decisions.

### Technology

- Java 21
- Spring Boot
- PostgreSQL for the running application
- H2 may be used for automated tests where appropriate
- REST API
- React/Next.js frontend

### Ticket

A ticket contains:

- id
- title
- description
- status
- priority
- assignee
- createdAt
- updatedAt

Use UUID for the ticket identifier.

Initial ticket status must always be OPEN.

Priority values are:
- LOW
- MEDIUM
- HIGH

Default priority is MEDIUM.

Title is required and must not be blank.

Description is optional.

Assignee is optional and is represented as a simple string in v1. No user table or authentication is required.

### Comments

A ticket can have multiple comments.

A comment contains:
- id
- ticketId
- body
- author
- createdAt

Comment body is required and must not be blank.

Author is represented as a string because authentication is out of scope.

### Status machine

The backend must enforce exactly these valid transitions:

OPEN → IN_PROGRESS
OPEN → CANCELLED

IN_PROGRESS → RESOLVED
IN_PROGRESS → CANCELLED

RESOLVED → CLOSED

All other transitions are invalid.

Examples:

CLOSED → OPEN = invalid
RESOLVED → OPEN = invalid
CANCELLED → OPEN = invalid
OPEN → RESOLVED = invalid
RESOLVED → CANCELLED = invalid

The frontend may prevent invalid actions from being selected, but the backend must independently enforce the state machine.

Invalid status transitions must return HTTP 409 Conflict.

### API behavior

Unknown ticket ID:
HTTP 404 Not Found

Invalid request validation:
HTTP 400 Bad Request

Invalid status transition:
HTTP 409 Conflict

Use a consistent JSON error response.

Status changes should use a dedicated endpoint:

PATCH /api/tickets/{id}/status

Ticket field updates should support:
- title
- description
- priority
- assignee

Comments should have a dedicated endpoint.

Search must operate on:
- title
- description

Search and status filtering must be combinable.

Pagination is out of scope for v1.

Authentication and authorization are out of scope.

Delete ticket is out of scope.

Editing or deleting comments is out of scope.

### Persistence

The application must use PostgreSQL so ticket and comment data survive application restart.

Do not commit database credentials or other secrets.

### Required specifications

Create or update the following files:

spec/requirements.md
spec/architecture.md
spec/data-model.md
spec/api-contract.md
spec/state-machine.md
spec/test-strategy.md

### Requirements specification

Document:
- functional requirements
- non-functional requirements
- business rules
- validation rules
- acceptance criteria
- explicit out-of-scope items

Give each requirement a stable identifier such as FR-001 or NFR-001.

### Architecture specification

Describe:
- frontend
- REST API
- controller layer
- service layer
- repository layer
- PostgreSQL
- exception handling
- validation
- state-machine enforcement

Explain the responsibility of each layer.

Do not introduce unnecessary microservices or infrastructure.

### Data model specification

Define:
- Ticket
- Comment
- fields
- types
- constraints
- relationships
- indexes where appropriate

Describe the database relationship between tickets and comments.

### API contract

Document:
- endpoint
- HTTP method
- purpose
- request body
- response body
- validation
- status codes
- error format

Include examples for the major endpoints.

At minimum cover:
- create ticket
- list tickets
- get ticket
- update ticket
- update status
- add comment
- search
- status filtering

### State machine

Create an explicit transition table.

Clearly distinguish:
- valid transitions
- invalid transitions

The backend must be the authoritative enforcement point.

### Test strategy

Define:
- unit testing
- repository testing where useful
- controller/API testing
- integration testing
- state-machine integration tests
- validation tests
- search/filter tests
- persistence/restart verification
- frontend testing
- error handling tests

Include explicit tests for all valid transitions and the invalid transitions listed above.

### Important constraints

Do not invent new product requirements.

If an implementation detail is necessary, mark it clearly as an architectural decision rather than pretending it came from the original requirements.

Do not write Java, SQL, React, or Spring Boot implementation code.

Do not modify files outside:
- spec/
- docs/prompt-history.md

The result must be a specification that another developer could implement without needing to guess the core business behavior.

## Prompt 003 — Specification Review and Corrections

Date: 2026-09-24

### Prompt

Review the six existing specification documents for internal consistency and adherence to the approved requirements.

Do NOT implement application code.

Apply only the following approved corrections:

1. PATCH /api/tickets/{id} must NOT silently ignore a client-supplied `status` field.
   If status is included in the field-update PATCH request, reject the request with HTTP 400 VALIDATION_ERROR and explain that status must be changed through the dedicated status endpoint.

2. Do not silently trim description, assignee, comment author, or other free-form fields unless explicitly required by the specification.
   For title and comment body:
   - validate using trimmed content
   - store the trimmed value
   For description, assignee, and author:
   - preserve the supplied value except where length/blank validation explicitly requires otherwise.

3. Keep the existing approved decisions:
   - UUID ticket and comment IDs
   - initial status OPEN
   - LOW/MEDIUM/HIGH priority
   - default MEDIUM priority
   - PostgreSQL runtime persistence
   - no authentication
   - assignee as a string
   - comments append-only
   - search title + description
   - search and status filter can be combined
   - exactly five valid state transitions
   - invalid transitions return 409
   - validation failures return 400
   - unknown ticket returns 404

4. Review all six specification files for contradictions caused by these changes and update references consistently.

5. Do not introduce new product requirements.

6. Clearly mark any implementation choices as architectural decisions rather than requirements.

7. Do not create or modify Java, SQL, React, or Spring Boot implementation code.

Files allowed to change:
- spec/requirements.md
- spec/architecture.md
- spec/data-model.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md
- docs/prompt-history.md

After making the changes, provide a concise summary of what changed and identify any remaining ambiguity that genuinely blocks implementation.

## Prompt 004 — Implementation Plan and Task Breakdown

Date: 2026-09-24

### Prompt

The v1 specifications are now reviewed and frozen.

Read ONLY these specification files:

- spec/requirements.md
- spec/architecture.md
- spec/data-model.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md

Create an implementation plan for the Support Ticket Management System.

Do NOT write application code.

The plan must follow this order:

1. Project and build setup
2. Backend foundation
3. Database and migrations
4. Domain model and enums
5. Repository layer
6. Service layer
7. State-machine implementation
8. DTOs and validation
9. Exception handling
10. REST controllers
11. Backend unit tests
12. Backend integration/API tests
13. Frontend project setup
14. Ticket list/search/filter UI
15. Ticket creation UI
16. Ticket detail UI
17. Ticket editing UI
18. Status transition UI
19. Comment UI
20. Frontend error handling
21. Frontend tests
22. PostgreSQL persistence/restart verification
23. Security/secrets review
24. Final acceptance testing
25. Final code/specification review

For every task provide:

- Task ID
- Task name
- Purpose
- Specification references
- Files/modules expected to be affected
- Dependencies on previous tasks
- Testing required
- Definition of done

Group tasks into logical phases.

The plan must preserve the architecture from the specification:

Next.js/React
→ Spring Boot REST API
→ Controller
→ Service
→ Repository
→ PostgreSQL

The state machine must remain enforced in the backend service/domain layer.

Do not introduce:
- authentication
- microservices
- Kafka
- Redis
- Elasticsearch
- unnecessary infrastructure
- new product requirements

If you identify a conflict between the specification and the implementation plan, STOP and identify the conflict rather than silently changing the specification.

The resulting plan must be detailed enough that implementation can proceed task-by-task rather than asking an AI agent to build the whole application.

Do not modify the specification files.

Only produce the plan and identify any remaining implementation blockers.

---

## Prompt 009 — Frontend Slice T-013 to T-016

Date: 2026-09-25

Backend T-001 through T-012 is complete and verified with 80 passing tests.

Implement ONLY:

- T-013 Frontend project setup
- T-014 Ticket list/search/filter UI
- T-015 Ticket creation UI
- T-016 Ticket detail UI

Read:
- docs/implementation-plan.md
- spec/requirements.md
- spec/api-contract.md

Do NOT implement T-017 through T-021 yet.

==================================================
T-013 — FRONTEND SETUP
==================================================

Use the existing Next.js + React + TypeScript App Router project.

Create:
- API client/helper
- shared TypeScript API types matching the frozen backend contract
- `/api` proxy/rewrite to backend where required

Browser must communicate with backend through HTTP JSON.

Do not use localStorage as the system of record.

Do not add authentication.

==================================================
T-014 — LIST / SEARCH / FILTER
==================================================

Implement the ticket list screen.

Requirements:
- display tickets
- search by keyword
- filter by one status
- search + status can be combined
- loading state
- empty state
- useful error state
- row/link opens ticket detail

Do NOT add pagination.

Use the existing backend API.

==================================================
T-015 — CREATE
==================================================

Implement ticket creation UI.

Fields:
- title
- description
- priority
- assignee

Rules:
- title required
- backend remains authoritative for validation
- do not send id
- do not send status
- do not send timestamps
- priority may default to MEDIUM

On successful creation, navigate appropriately to list/detail.

==================================================
T-016 — DETAIL
==================================================

Implement ticket detail page.

Display:
- id
- title
- description
- priority
- status
- assignee
- createdAt
- updatedAt
- comments

Comments must be displayed in API order.

Unknown ticket / 404 must show the API error message rather than a generic unexplained failure.

==================================================
UI SCOPE
==================================================

Keep UI simple and functional.

Do NOT introduce:
- authentication
- Redux/global state library unless already required
- complex design systems
- unnecessary dependencies
- new backend endpoints
- local database
- mock ticket data as the system of record

Use the existing API.

Do not modify backend business logic unless a genuine frontend integration issue requires it. If such an issue is found, report it instead of silently changing the backend contract.

Do not modify:
- spec/*
- docs/implementation-plan.md

==================================================
TESTING
==================================================

Verify:
- frontend installs/builds
- TypeScript compiles
- list screen works against backend
- create flow works against backend
- detail flow works against backend
- search/filter requests use the documented API

Do not implement T-021 yet.

==================================================
PROMPT HISTORY
==================================================

Append this exact Prompt 009 and a concise result to:

docs/prompt-history.md

Do not put the prompt into docs/implementation-plan.md.

==================================================
FINAL REPORT
==================================================

Report:
- T-013 result
- T-014 result
- T-015 result
- T-016 result
- files created/changed
- build/test results
- any integration issue
- confirmation that T-017 through T-021 were NOT implemented

### Result

T-013: `lib/types.ts` and `lib/api.ts` (fetch + `ApiError`) with Next.js rewrite `/api/*` → `http://localhost:8080/api/*`; no localStorage. T-014: home list with keyword + status filters (combined via `q`/`status` query params), loading/empty/error states, links to detail. T-015: `/tickets/new` form POSTing only contract fields; redirects to detail on 201; client title check + backend `fieldErrors`. T-016: `/tickets/[id]` detail showing all fields and comments in API order; 404 shows API `message`. Simple layout/styles in `globals.css`. `npm run build` not executed in agent environment (Node.js/npm unavailable); code follows existing Next 16 + strict TS patterns. No backend changes. T-017–T-021 not implemented.

---

## Prompt 010 — Complete Frontend Slice T-017 to T-021

Date: 2026-09-25

T-001 through T-016 are implemented.
The frontend production build has been independently verified with:

npm install
npm run build

Implement ONLY:
- T-017 Ticket editing UI
- T-018 Status transition UI
- T-019 Comment UI
- T-020 Frontend error handling
- T-021 Frontend tests

Read:
- docs/implementation-plan.md
- spec/requirements.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md

Do NOT modify the backend unless a genuine API-contract bug is discovered. Report such a conflict instead of changing the contract.

==================================================
T-017 — EDIT TICKET
==================================================

On the ticket detail page, allow updating:
- title
- description
- priority
- assignee

Use:
PATCH /api/tickets/{id}

Rules:
- never send status
- never send id
- never send timestamps
- omitted fields remain unchanged
- description/assignee can be cleared using the documented null/empty behavior
- backend remains authoritative for validation

Editing is allowed regardless of current ticket status.

==================================================
T-018 — STATUS
==================================================

Add status transition UI.

Use ONLY:

PATCH /api/tickets/{id}/status

Allowed transitions:

OPEN → IN_PROGRESS
OPEN → CANCELLED
IN_PROGRESS → RESOLVED
IN_PROGRESS → CANCELLED
RESOLVED → CLOSED

The UI may hide illegal actions, but backend remains authoritative.

If backend returns 409:
- show the API message meaningfully
- do not silently ignore it

Do not implement transition logic in the frontend as a replacement for backend validation.

==================================================
T-019 — COMMENTS
==================================================

Add comment functionality to ticket detail.

Use:

POST /api/tickets/{id}/comments

Fields:
- author
- body

Rules:
- body required
- comments append-only
- no edit/delete UI
- works for all ticket statuses
- display comments in API order

==================================================
T-020 — ERROR HANDLING
==================================================

Centralize frontend handling of backend ApiError responses.

Meaningful handling for:
- 400 validation
- 404 missing ticket
- 409 invalid transition
- network/server failures

Use backend:
- message
- fieldErrors
- code/status where appropriate

Do not show only a generic "Request failed" when the API supplied a meaningful message.

Show field-specific validation errors where practical.

==================================================
T-021 — FRONTEND TESTS
==================================================

Implement the frontend testing approach allowed by the frozen test strategy.

Prefer a lightweight approach that does not introduce unnecessary dependencies.

At minimum cover the important frontend acceptance flows:
- create
- list
- search/filter
- detail
- edit
- status transition
- comment
- meaningful API errors

If automated frontend testing infrastructure is already present, extend it.

If adding a new test framework would create unnecessary complexity, create a clear manual acceptance checklist at:

docs/manual-frontend-ac.md

The checklist must be executable against the running application and cover FE-001 through FE-009.

Do not weaken mandatory backend state-machine tests.

==================================================
STRICT SCOPE
==================================================

Do NOT add:
- authentication
- user management
- Redux or unnecessary state libraries
- new backend endpoints
- local database
- localStorage as source of truth
- pagination
- notifications
- new product features
- Kafka
- Redis
- Elasticsearch
- microservices

Do not modify:
- spec/*
- docs/implementation-plan.md

Keep the existing Next.js App Router architecture.

==================================================
VERIFICATION
==================================================

Run the appropriate frontend checks.

At minimum:

npm run build

If automated frontend tests are added, run them too.

Fix compilation/type/test failures caused by T-017 through T-021.

Do not modify product behavior just to make tests pass.

==================================================
PROMPT HISTORY
==================================================

Append this exact Prompt 010 and a concise implementation result to:

docs/prompt-history.md

Do not put the prompt into docs/implementation-plan.md.

==================================================
FINAL REPORT
==================================================

Report:
- T-017 result
- T-018 result
- T-019 result
- T-020 result
- T-021 result
- files created/changed
- tests/build results
- any technical decisions
- confirmation that T-022 through T-025 were NOT implemented

### Result

T-017: `TicketEditForm` on detail PATCHes title/description/priority/assignee only (empty string clears); preserves comments on merge. T-018: `TicketStatusControl` shows allowed targets from `lib/transitions.ts` (UI hints); PATCH status; 409 via `ApiErrorBanner`. T-019: `TicketCommentForm` POST comment then reloads detail; append-only list. T-020: `lib/api-error.ts` + `ApiErrorBanner`; improved `lib/api.ts` network/server fallbacks; wired on list/create/detail/forms. T-021: Vitest unit tests (`api-error`, `transitions`) + `docs/manual-frontend-ac.md` (FE-001–FE-009). No backend changes. Agent env lacked npm; run `npm install && npm run test && npm run build` locally. T-022–T-025 not implemented.

---

## Prompt 011 — Final Verification, Acceptance and Review (T-022 to T-025)

Date: 2026-09-25

T-001 through T-021 have been implemented.

Now complete ONLY:
- T-022 PostgreSQL persistence/restart verification
- T-023 Security/secrets review
- T-024 Final acceptance testing
- T-025 Final code/specification review

Do not introduce new product features.

Read:
- docs/implementation-plan.md
- spec/requirements.md
- spec/architecture.md
- spec/data-model.md
- spec/api-contract.md
- spec/state-machine.md
- spec/test-strategy.md
- docs/prompt-history.md

==================================================
T-022 — POSTGRESQL PERSISTENCE / RESTART
==================================================

Verify the application against REAL PostgreSQL, not H2.

First inspect the existing configuration and determine the expected PostgreSQL setup.

Do not commit credentials.

If PostgreSQL is available locally:
1. Start/verify PostgreSQL.
2. Configure runtime datasource using environment variables.
3. Start Spring Boot.
4. Create a ticket through the REST API.
5. Add a comment.
6. Record the ticket ID.
7. Stop Spring Boot.
8. Start Spring Boot again.
9. GET the same ticket.
10. Verify the ticket and comment still exist with the expected values.

If PostgreSQL is not available in the environment:
- do not install unnecessary infrastructure automatically
- document the exact commands/configuration required for manual verification
- clearly mark PER-001 as NOT VERIFIED rather than pretending it passed.

Do not use H2 as evidence for PostgreSQL persistence.

==================================================
T-023 — SECURITY / SECRETS REVIEW
==================================================

Review the complete repository for:

- passwords
- API keys
- tokens
- private credentials
- real database credentials
- production JDBC URLs containing credentials
- secrets committed to configuration

Check:
- .gitignore
- .env.example
- application.yml
- application-test.yml
- frontend configuration
- README
- source code

Placeholders are acceptable.

Real credentials are not.

Also verify that API error responses do not expose:
- stack traces
- SQL/database credentials
- internal sensitive configuration

Do not add authentication because authentication is explicitly out of scope.

==================================================
T-024 — FINAL ACCEPTANCE TEST
==================================================

Run the backend test suite.

Run the frontend test suite.

Run the frontend production build.

Then verify the complete acceptance criteria from spec/requirements.md:

AC-001 through AC-017.

Verify:

1. Create ticket
2. List tickets
3. View ticket detail
4. Update title
5. Update description
6. Update priority
7. Update assignee
8. Add comment
9. Search keyword
10. Filter by status
11. Search + status together
12. PostgreSQL persistence across application restart
13. Valid state transitions
14. Invalid state transitions rejected by backend
15. Backend validation
16. Meaningful frontend errors
17. No secrets committed

For state-machine verification, explicitly verify:

OPEN → IN_PROGRESS → RESOLVED → CLOSED

and invalid:

CLOSED → OPEN

Also verify the other invalid transitions listed in the frozen state-machine specification.

If something fails:
- diagnose it
- fix only genuine implementation defects
- do not change product requirements
- do not weaken tests
- do not modify spec files

==================================================
T-025 — FINAL CODE / SPEC REVIEW
==================================================

Compare the implementation against ALL frozen specs.

Check especially:

1. Architecture layering:

Next.js/React
→ Spring Boot REST API
→ Controller
→ Service
→ Repository
→ PostgreSQL

2. State machine:
- exactly five allowed transitions
- enforcement in backend service/domain
- no controller-only enforcement

3. PATCH behavior:
- status supplied to normal PATCH → 400
- no persistence on that validation error

4. Validation:
- title trimming
- comment body trimming
- description/assignee/author not automatically trimmed
- exact field limits

5. API:
- correct paths
- HTTP status codes
- error contract
- list envelope
- comments ordering
- ticket ordering

6. Persistence:
- UUID
- PostgreSQL
- Flyway
- comments relationship

7. Frontend:
- all required UI flows
- meaningful errors
- no localStorage as source of truth

8. Scope:
Confirm NONE of these were introduced:
- authentication
- user management
- Kafka
- Redis
- Elasticsearch
- microservices
- pagination
- notifications
- delete/comment-edit features
- unrelated infrastructure
- new product requirements

9. Confirm:
- spec/* was not changed
- docs/implementation-plan.md was not changed
- prompt history contains Prompts 001–011 and results

==================================================
IMPORTANT
==================================================

Do not blindly report success.

For every verification, distinguish:

PASS
FAIL
NOT VERIFIED
NOT APPLICABLE

If something cannot be verified because the environment lacks PostgreSQL, Node, etc., explicitly say NOT VERIFIED.

Do not claim a test passed unless you actually ran it.

==================================================
DOCUMENTATION
==================================================

Update:
docs/prompt-history.md

Append Prompt 011 and its concise result.

Do NOT modify:
- spec/*
- docs/implementation-plan.md

If you discover a genuine conflict between implementation and frozen specification, report it clearly instead of silently changing the specification.

==================================================
FINAL REPORT
==================================================

Return a concise final report containing:

### T-022
Status:
Evidence:

### T-023
Status:
Evidence:

### T-024
Status:
Backend tests:
Frontend tests:
Frontend build:
Acceptance:

### T-025
Status:
Spec conflicts:
Scope violations:
Remaining issues:

### Overall
PASS / FAIL / NOT READY

List any fixes made during this verification.

### Result

T-022: **NOT VERIFIED** — no PostgreSQL in agent environment; added `docs/verification-restart.md` with env vars and curl steps for manual PER-001/AC-012. T-023: **PASS** — placeholders only in git; `.env` gitignored; `RestExceptionHandler` returns generic 500 without stack/SQL in body. T-024: Backend **PASS** (80/80); frontend test/build **NOT VERIFIED** on agent (no npm); `docs/acceptance-log.md` maps AC-001–AC-017. T-025: **PASS** (code review) — layering, five transitions in `TicketStatusMachine`, no scope violations found; **remaining:** PER-001/AC-012 manual sign-off, full UI AC walkthrough, prompt-history gaps for Prompts 005–008 (not in file). No implementation fixes required. Overall: **NOT READY** until PostgreSQL restart and frontend checks are signed off locally.

---

## Fix — PostgreSQL list/search query failure

Date: 2026-09-25

Production-style issue: `GET /api/tickets` returned HTTP 500 on PostgreSQL 16 with `ERROR: function lower(bytea) does not exist` when list ran without a search keyword (`q` null). H2 tests did not reproduce the nullable-parameter typing inside `LOWER(CONCAT('%', :q, '%'))`.

### Result

**Root cause:** Single JPQL query used `:q IS NULL OR LOWER(... :q ...)`. PostgreSQL still type-checked/bound `:q` in the `LOWER` branch as `bytea` when null.

**Fix:** Split `TicketRepository.search` into a default method dispatching to four paths: `findAllByOrderByCreatedAtDescIdDesc`, `findByStatusOrderByCreatedAtDescIdDesc`, `searchByKeyword`, `searchByKeywordAndStatus` — keyword queries only run when `q` is non-null.

**Files:** `backend/src/main/java/com/supporttickets/repository/TicketRepository.java`; `backend/src/test/java/com/supporttickets/repository/TicketRepositoryTest.java` (added `listWithNoKeywordAndNoStatusReturnsAllTickets`).

**Tests:** `./mvnw test` — **81** tests, **0** failures.

**PostgreSQL:** Verified against PostgreSQL **16.15** (Docker): `GET http://localhost:8092/api/tickets` → **HTTP 200** `{"items":[]}`. No spec/plan changes.

---

## Frontend UI/UX Redesign — Production-Quality Support Ticket Management

Date: 2026-09-25

Redesign frontend only (no backend/API/spec changes): modern SaaS shell with sidebar + top bar, polished tickets list (stats from unfiltered `listTickets()` call, table + mobile cards, badges, skeletons), create/detail layouts, conversation-style comments, status actions with `transitionActionLabel`, shared UI primitives (`StatusBadge`, `PriorityBadge`, `PageHeader`, `EmptyState`, skeletons), expanded `globals.css` design tokens. Removed `AppHeader` in favor of `AppShell`. Vitest extended for transition labels. **No new dependencies.** Agent could not run `npm test`/`npm run build` (no npm on host); run locally to verify. Functional behavior and API calls unchanged.

---

## Final Frontend UI Polish — Detail, Create, Edit & Navigation

Date: 2026-09-25

**Scope:** Frontend UI/UX only; tickets dashboard (`TicketListView`) preserved; no backend/API/spec/plan changes.

**Navigation:** Top bar breadcrumb `Support Tickets / {Tickets | New ticket | Ticket detail}`; sidebar branding and CTAs unchanged.

**Detail page:** Back link, header (title, summary line, ID, badges), two-column layout (description + comment timeline | ticket details, status actions, inline edit). Loading/error/empty comment states use dashboard cards.

**Create / edit:** Centered form page with `BackLink`, `FormPageHeader`, split Cancel/submit actions; edit panel notes status is managed in workflow section.

**Comments:** Timeline cards; compose section with author-first fields and end-aligned “Add comment”.

**Status:** Prominent current status; primary/secondary buttons via `isDestructiveTransition` for cancel; existing state machine only; 409 surfaced through existing `ApiErrorBanner`.

**CSS:** `globals.css` extended for breadcrumbs, detail header, form actions, status block, comment compose, alert cards.

**Tests:** `transitions.test.ts` — `isDestructiveTransition`.

**Files:** `AppShell.tsx`, `TicketDetailView.tsx`, `CreateTicketForm.tsx`, `TicketEditForm.tsx`, `TicketStatusControl.tsx`, `TicketCommentForm.tsx`, `ui/BackLink.tsx`, `ui/FormPageHeader.tsx`, `lib/transitions.ts`, `lib/transitions.test.ts`, `app/globals.css`.

**Dependencies:** None added.

**Verification:** Run `npm run test` and `npm run build` in `frontend/` locally (agent host has no `npm`).

---

## Final visual polish pass (scoped)

Date: 2026-09-25

**Scope:** Frontend only; approved Tickets dashboard layout/styling unchanged.

**Changes:** Removed duplicate “New ticket” CTA from `PageHeader` on the list page (retained sidebar + top bar CTAs). Replaced top bar “Menu” label with an icon-only nav toggle (`aria-label` for accessibility). Top bar “New ticket” visible at all breakpoints. Create, edit, and detail pages already share `page-title` / `page-description`, cards, buttons, and badges — no dashboard CSS changes.

**Files:** `TicketListView.tsx`, `AppShell.tsx`, `app/globals.css`.

**Dependencies:** None. Backend/API/spec/plan unchanged.

---

## Prompt 012 — Handover check, rename request, run instructions

Date: 2026-09-25

### AI review (mistakes / corrections)

| AI suggestion | Correction |
|---------------|------------|
| Implied a rename might be needed in code | Verified first; nothing to change in git |
| Listed run steps before checking host | Machine had no Java, no Node 20, no `.env` — steps were right but environment wasn't ready |

**SpecStory:** `../.specstory/history/history.md` (Step 1)

---

## Prompt 013 — Balanced frontend and backend enhancements

Date: 2026-09-25

### Prompt

Improve FE and BE: dashboard summary, server pagination/sorting, debounced search, query indexes, tests. Keep API backward compatible when pagination params omitted.

### Result

**Backend:** Optional `page`/`size`/`sort` on `GET /api/tickets`; `GET /api/tickets/summary`; `TicketListResponse` metadata; `V2__ticket_list_indexes.sql`; validator + integration tests.

**Frontend:** Summary-driven stat cards (incl. CANCELLED), debounced search, pagination, sortable columns, refresh overlay, abortable fetches; `ticket-list-params.ts`, `ticket-stats.ts`, CSS for pagination/sort.

**Spec:** Updated `api-contract.md`, `requirements.md` (pagination no longer OOS), `test-strategy.md` SF-007–SF-009.

### AI review (mistakes / corrections)

| Issue | What went wrong | Fix |
|-------|-----------------|-----|
| `TicketSpecifications` visibility | Package-private class used from `TicketService` | Made class + `findPage` public — backend wouldn't compile |
| Table columns | Extra Created column during sort work | Restored original column order |
| Page reset `useEffect` | Compared identical filter objects | Reset page when debounced keyword changes |

**Verification:** Frontend `npm test` + `npm run build` passed on Node 20.20.2. Backend tests deferred until JDK installed (Prompt 014).

**SpecStory:** `../.specstory/history/history.md` (Steps 2–3)

---

## Prompt 014 — Local environment troubleshooting (Node, Java, PostgreSQL)

Date: 2026-09-25

### Prompts (same day, sequential)

1. UI error: `Unexpected token 't', "internal S"... is not valid JSON`
2. `JAVA_HOME environment variable is not defined correctly` on `./mvnw spring-boot:run`
3. `Failed to configure a DataSource: 'url' attribute is not specified`
4. How to push to GitHub
5. Set up prompt history / SpecStory folders

### Result

**JSON error:** Next.js proxy returned plain-text `Internal Server Error` because backend wasn't on 8080 — not a frontend JSON bug. Improved `api.ts` to handle non-JSON bodies with a clear message; still requires API running.

**JAVA_HOME:** Installed Temurin JDK 21 under `~/.local/java/`; added `JAVA_HOME` to `~/.bashrc`. `sudo apt install openjdk-21-jdk` was suggested first but failed without sudo password.

**DataSource:** `.env` still had `<database>` placeholders; Spring Boot doesn't load `.env` by default. Fixed `application.yml` (defaults + `spring.config.import` for `../.env`), updated `.env.example`, added `backend/run-dev.sh` and `scripts/setup-local-db.sh`.

**GitHub:** Remote `origin` → `https://github.com/Rakesh-Kumar-1996/support-ticket-management.git`; standard `git add` / `commit` / `push origin main`; `.env` stays gitignored.

**Prompt history:** Added `.specstory/history/` session files + this appendix.

### AI review (mistakes / corrections)

| AI suggestion | Why we corrected it |
|---------------|---------------------|
| JSON parse fix = problem solved | Symptom fix only; root cause was stopped backend |
| Use `apt` for Java/PostgreSQL in agent shell | No passwordless sudo — used user-local JDK instead |
| Assume `.env` is picked up by Spring Boot | It isn't unless exported or imported — documented + configured import |
| Backend "verified" when background task exited 137 | Process started then was killed; user must run `./run-dev.sh` in their own terminal |

**SpecStory:** `../.specstory/history/history.md` (Steps 4–10)

---

## Prompt history tooling

Date: 2026-09-25

Repository layout for prompt capture:

```
.specstory/
  README.md
  history/
    session-history.md   ← single file: human input + AI output, every step
docs/
  prompt-history.md      (this file — longer index for spec-driven workflow)
```

Append new Cursor steps to `history.md` using `**Human:**` / `**AI:**` blocks. No personal names in that file. That file now contains the **full project build** (Steps 1–37): requirements → spec → plan → T-001–T-025 → fixes → enhancements → local setup.
