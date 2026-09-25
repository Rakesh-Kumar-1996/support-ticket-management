# Support Ticket Management — full build history

Workflow: Requirement → Specification → Plan → Implementation → Testing → Review → Fix

---

## Step 1 — Requirements analysis

**Human:** Build a Support Ticket Management System (Java 21, Spring Boot, PostgreSQL/H2, REST, React/Next.js). Use spec-driven development. 
Do not write code yet. Analyse functional requirements (create/list/detail/update/comment/search/filter/persist/validate/errors), state machine rules, and acceptance criteria. List ambiguities and edge cases.

**System:**
1. Produced `docs/requirements-analysis.md` (structured FR/NFR, business rules, state machine, validation, API/data/frontend/testing needs, ambiguities).
2. Did not create application code.
3. Recorded five valid transitions and invalid examples (e.g. CLOSED → OPEN).

---

## Step 2 — Create specifications

**Human:** Specifications are approved. Create frozen spec files: requirements, architecture, data model, API contract, state machine, test strategy. 
UUID tickets, OPEN initial status, priority LOW/MEDIUM/HIGH (default MEDIUM), string assignee, append-only comments, dedicated status endpoint, PostgreSQL runtime, no auth, no pagination v1, no delete.

**System:**
1. Created/updated:
   - `spec/requirements.md`
   - `spec/architecture.md`
   - `spec/data-model.md`
   - `spec/api-contract.md`
   - `spec/state-machine.md`
   - `spec/test-strategy.md`
2. Documented six API operations, error JSON format, list envelope `{ items }`.
3. Marked pagination and delete as out of scope.
4. Appended Prompt 002 to `docs/prompt-history.md`.

---

## Step 3 — Specification review and corrections

**Human:** Review all six spec files. Fix: (1) PATCH with `status` field must return 400, not ignore; (2) trim title and comment body only — do not auto-trim description/assignee/author; (3) keep all other approved decisions. No code.

**System:**
1. Updated all six spec files for consistency.
2. Added VAL-016 / AD rules for status-on-field-PATCH.
3. Clarified trim vs preserve policy in data model and API contract.
4. Appended Prompt 003 to `docs/prompt-history.md`.

---

## Step 4 — Implementation plan

**Human:** Specs are frozen. Create task-by-task implementation plan (T-001–T-025): backend foundation → DB → domain → repo → service → state machine → DTOs → exceptions → controllers → backend tests → frontend screens → verification. No code. Do not change spec files.

**System:**
1. Created `docs/implementation-plan.md` with 25 tasks, phases, dependencies, definition of done.
2. Defined repo layout: `backend/` Spring Boot, `frontend/` Next.js, `.env.example`, Flyway V1.
3. Identified no spec conflicts.

---

## Step 5 — T-001 Project and build setup

**Human:** Implement T-001 — two-app skeleton, Java 21, Node toolchain, gitignore secrets.

**System:**
1. Created `backend/` Maven project (`pom.xml`, Java 21, Spring Boot parent).
2. Created `frontend/` Next.js App Router project (`package.json`, TypeScript).
3. Added root `README.md`, `.gitignore`, `.env.example` with placeholders only.
4. Verified backend compiles; frontend `npm install` works.

---

## Step 6 — T-002 Backend foundation

**Human:** Implement T-002 — Spring Boot app, `/api` context path, UTC Jackson, no Spring Security.

**System:**
1. Added `Application.java`, `application.yml` (port 8080, context `/api`).
2. Smoke `@SpringBootTest` — context loads.
3. No auth filter chain.

---

## Step 7 — T-003 Database and migrations

**Human:** Implement T-003 — Flyway migration, PostgreSQL runtime config via env vars, H2 for tests.

**System:**
1. Added `V1__tickets_and_comments.sql` (tickets + comments tables, UUID PKs, FK cascade, indexes).
2. `application.yml` — datasource from `SPRING_DATASOURCE_*`.
3. `application-test.yml` — H2 in-memory PostgreSQL mode for tests.
4. `ddl-auto: validate`; Flyway enabled.

---

## Step 8 — T-004 Domain model and enums

**Human:** Implement T-004 — JPA entities matching spec.

**System:**
1. Added `Ticket`, `Comment`, `TicketStatus`, `TicketPriority` entities/enums.
2. Mapped fields 1:1 with data model (no user table).

---

## Step 9 — T-005 Repository layer

**Human:** Implement T-005 — repositories, list/search/filter, comment ordering.

**System:**
1. Added `TicketRepository`, `CommentRepository`.
2. List order: `createdAt DESC, id DESC`.
3. Keyword search on title OR description (case-insensitive).
4. Combined `q` + `status` with AND semantics.
5. Repository tests RT-001–RT-006 on H2.

---

## Step 10 — T-006 Service layer

**Human:** Implement T-006 — use cases without HTTP: create, list, get, update fields, add comment, change status.

**System:**
1. Added `TicketService` with transactions.
2. Create: trim title, default OPEN/MEDIUM, ignore client id/status/timestamps.
3. Update fields: clear description/assignee with empty string; reject status on field PATCH.
4. Comments: trim body; ticket `updatedAt` unchanged on comment-only add.

---

## Step 11 — T-007 State machine

**Human:** Implement T-007 — authoritative transition table in domain layer.

**System:**
1. Added `TicketStatusMachine` with exactly five allowed edges.
2. `InvalidStatusTransitionException` for illegal moves (mapped to 409 in HTTP layer).
3. Unit tests UT-001–UT-004 for valid/invalid transitions.

---

## Step 12 — T-008 DTOs and validation

**Human:** Implement T-008 — request/response DTOs, manual validator, trim rules, VAL-016.

**System:**
1. Added create/update/status/comment request DTOs and response records.
2. Added `TicketRequestValidator` (title/body trim, length limits, status-on-PATCH check).
3. `UpdateTicketRequest` tracks field presence for PATCH semantics.
4. Validator unit tests VAL-T-001–011, VAL-016.

---

## Step 13 — T-009 Exception handling

**Human:** Implement T-009 — unified ApiError JSON for 400/404/409.

**System:**
1. Added `RestExceptionHandler`, `ApiError`, `FieldErrorItem`.
2. Codes: `VALIDATION_ERROR`, `TICKET_NOT_FOUND`, `INVALID_STATUS_TRANSITION`.
3. Malformed UUID and JSON → 400. No stack traces in response body.

---

## Step 14 — T-010 REST controllers

**Human:** Implement T-010 — six HTTP endpoints per API contract.

**System:**
1. Added `TicketController`:
   - `POST /tickets` → 201
   - `GET /tickets?q&status` → 200 `{ items }`
   - `GET /tickets/{id}` → detail + comments
   - `PATCH /tickets/{id}` → field update
   - `PATCH /tickets/{id}/status` → status change
   - `POST /tickets/{id}/comments` → 201
2. Thin controllers — delegate to service only.

---

## Step 15 — T-011 Backend unit tests

**Human:** Implement T-011 — state machine and validation unit tests.

**System:**
1. `TicketStatusMachineTest` — all SM matrix cases.
2. `TicketRequestValidatorTest` — trim, blank, length, VAL-016.
3. All unit tests green.

---

## Step 16 — T-012 Backend integration / API tests

**Human:** Implement T-012 — MockMvc integration tests on H2 (API + SM + search/filter).

**System:**
1. Added `TicketApiIntegrationTest` — create, list, search, filter, PATCH, status SM, comments, 400/404/409.
2. Parameterized tests for valid and invalid transitions.
3. **80 backend tests passing** on H2 profile.

---

## Step 17 — T-013 Frontend setup (Prompt 009)

**Human:** Backend T-001–T-012 done. Implement T-013 only: API client, TypeScript types, `/api` proxy to backend. No auth, no localStorage as source of truth.

**System:**
1. Added `frontend/lib/types.ts`, `frontend/lib/api.ts` (`ApiError`, fetch helper).
2. `next.config.ts` rewrite `/api/*` → `http://localhost:8080/api/*`.
3. No new dependencies beyond Next.js stack.

---

## Step 18 — T-014 List / search / filter UI

**Human:** Implement T-014 — ticket list with keyword + status filter, loading/empty/error, link to detail. No pagination.

**System:**
1. Added `TicketListView.tsx` on home page.
2. Apply-filters form for `q` and `status`.
3. `EmptyState`, skeleton loading, error banner placeholder.
4. Styled in `globals.css`.

---

## Step 19 — T-015 Create ticket UI

**Human:** Implement T-015 — create form, POST only contract fields, navigate to detail on success.

**System:**
1. Added `/tickets/new` page and `CreateTicketForm.tsx`.
2. Sends title, optional description, priority, assignee only.
3. Client title check + backend `fieldErrors` on 400.

---

## Step 20 — T-016 Ticket detail UI

**Human:** Implement T-016 — detail page with all fields and comments in API order; show API message on 404.

**System:**
1. Added `/tickets/[id]/page.tsx` and `TicketDetailView.tsx`.
2. Displays id, title, description, priority, status, assignee, timestamps, comments.
3. 404 shows backend `message`.

---

## Step 21 — T-017 to T-021 (Prompt 010)

**Human:** Implement T-017 edit, T-018 status, T-019 comments, T-020 error handling, T-021 tests. No backend changes unless contract bug.

**System:**
1. **T-017:** `TicketEditForm` — PATCH fields only; empty string clears description/assignee.
2. **T-018:** `TicketStatusControl` + `lib/transitions.ts` — UI hints for allowed targets; PATCH status; 409 via banner.
3. **T-019:** `TicketCommentForm` — POST comment, reload detail, append-only list.
4. **T-020:** `lib/api-error.ts`, `ApiErrorBanner` — map 400/404/409/network/server with `fieldErrors`.
5. **T-021:** Vitest tests (`api-error`, `transitions`); `docs/manual-frontend-ac.md` FE-001–FE-009.
6. T-022–T-025 not implemented in this step.

---

## Step 22 — T-022 to T-025 verification (Prompt 011)

**Human:** Complete final verification: PostgreSQL restart, secrets review, acceptance tests, spec review. Report PASS/FAIL/NOT VERIFIED honestly.

**System:**
1. **T-022:** NOT VERIFIED in agent environment (no PostgreSQL). Added `docs/verification-restart.md` with manual curl steps.
2. **T-023:** PASS — `.env` gitignored, placeholders only, generic 500 body.
3. **T-024:** Backend 80/80 PASS; frontend test/build NOT VERIFIED on agent (no npm).
4. **T-025:** PASS code review — layering, five transitions, no scope violations.
5. Added `docs/acceptance-log.md`. Overall: NOT READY until local PostgreSQL + frontend checks.

---

## Step 23 — PostgreSQL list bug fix

**Human:** (Issue found in testing) `GET /api/tickets` returns 500 on PostgreSQL — `function lower(bytea) does not exist` when `q` is null.

**System:**
1. Root cause: single JPQL query bound null `q` as bytea inside `LOWER()`.
2. Split `TicketRepository.search` into four query paths (no keyword / status only / keyword / both).
3. Added repository test for list without keyword.
4. **81 tests passing.** Verified HTTP 200 on PostgreSQL 16.

**Human correction:** H2 tests did not catch PostgreSQL null-parameter typing — real DB test was required.

---

## Step 24 — Frontend UI redesign

**Human:** Redesign frontend for production-quality SaaS look. Frontend only — no backend/API/spec changes.

**System:**
1. Added `AppShell` (sidebar + top bar), stat cards on list, table + mobile cards.
2. Added `StatusBadge`, `PriorityBadge`, `PageHeader`, `EmptyState`, skeletons.
3. Expanded `globals.css` design tokens.
4. Removed old `AppHeader`. No new npm dependencies.

---

## Step 25 — Detail / create / edit polish

**Human:** Polish detail, create, edit, navigation. Keep tickets dashboard unchanged.

**System:**
1. Breadcrumbs in `AppShell`, `BackLink`, `FormPageHeader`.
2. Two-column detail layout; comment timeline; status block with destructive styling for cancel.
3. `isDestructiveTransition` in `lib/transitions.ts` + test.

---

## Step 26 — Final visual polish

**Human:** Small UI fixes — duplicate CTA, mobile menu icon, consistent page headers.

**System:**
1. Removed duplicate “New ticket” from list `PageHeader`.
2. Icon-only mobile nav toggle with `aria-label`.
3. Top bar CTA visible at all breakpoints.

---

## Step 27 — Balanced FE/BE enhancements

**Human:** Optimize and enhance frontend and backend — pagination, sorting, dashboard summary, debounced search, indexes, tests. Keep API backward compatible.

**System:**
1. **Backend:** Optional `page`/`size`/`sort` on list; `GET /tickets/summary`; `TicketListResponse` metadata; `V2__ticket_list_indexes.sql`; `TicketSpecifications`; validator + tests; spec updates (pagination no longer OOS).
2. **Frontend:** Summary API for stat cards (incl. CANCELLED), debounced search, pagination, sortable columns, refresh overlay, abortable fetches.
3. **Fix:** `TicketSpecifications` was package-private → compile error → made public.
4. **Fix:** Table column order and page-reset logic corrected during review.

---

## Step 28 — Node.js version

**Human:** Next.js requires Node `>=20.9.0`; system has 18. Install correct version.

**System:**
1. Installed nvm + Node v20.20.2, set as default in shell config.
2. `npm install`, `npm test`, `npm run build` — all passed.

---

## Step 29 — JSON parse error on dashboard

**Human:** UI error: `Unexpected token 't', "internal S"... is not valid JSON`.

**System:**
1. Diagnosed: backend not running; Next.js proxy returned plain text `Internal Server Error`.
2. Updated `api.ts` to handle non-JSON responses with clear message.
3. Explained API must be on port 8080 — parse fix is UX only.

**Human correction:** Fixing JSON parsing alone does not start the backend.

---

## Step 30 — JAVA_HOME error

**Human:** `./mvnw spring-boot:run` → `JAVA_HOME environment variable is not defined correctly`.

**System:**
1. No JDK on system. `sudo apt install openjdk-21-jdk` failed (no sudo password in agent shell).
2. Installed Temurin JDK 21 under `~/.local/java/`.
3. Set `JAVA_HOME` and `PATH` in shell config.
4. `./mvnw -version` OK. Backend started; Flyway V1+V2 applied.

---

## Step 31 — DataSource URL missing

**Human:** Spring Boot fails: `Failed to configure a DataSource: 'url' attribute is not specified`.

**System:**
1. `.env` still had `<database>` placeholders.
2. Spring Boot does not load `.env` unless configured.
3. Updated `application.yml`: `spring.config.import` for `../.env`, local defaults for URL/user/password.
4. Updated `.env` and `.env.example` with `support_tickets` / `support` / `support`.
5. Added `backend/run-dev.sh`, `scripts/setup-local-db.sh`.

**Human correction:** Placeholder `.env` looked complete but variables were never exported to Spring.

---

## Step 32 — Git push instructions

**Human:** Steps to push all local changes to git.

**System:**
1. Listed modified/untracked files on `main`.
2. Gave `git add` → `commit` → `push origin main`.
3. Reminded: do not commit `.env`.

---

## Step 33 — Prompt history / SpecStory setup

**Human:** Create `.specstory/history/` and maintain `docs/prompt-history.md`. Show AI mistakes, not blind acceptance. Sound like real dev challenges.

**System:**
1. Created `.specstory/` structure and session files.
2. Appended Prompts 012–014 to `docs/prompt-history.md` with AI review tables.

---

## Step 34 — Own GitHub remote

**Human:** Push to own GitHub account instead of original remote.

**System:**
1. Explained `git remote set-url origin https://github.com/<account>/support-ticket-management.git`.
2. Steps: create empty repo → commit → `git push -u origin main`.
3. GitHub login / token notes if push fails.

---

## Step 35 — Single SpecStory file

**Human:** Spec history must be one file only — each human prompt and Cursor action, every step, no personal names.

**System:**
1. Consolidated into this file: `history.md`.
2. Removed separate per-topic history files.
3. Format: **Human:** / **System:** per step.

---

## Step 36 — Full step-by-step build in one history file

**Human:** Write the complete project build step by step in the SpecStory history — every prompt and every Cursor action from the start.

**System:**
1. Rewrote this file to cover Steps 1–37: requirements → spec → plan → T-001–T-025 → bug fix → UI passes → enhancements → local env fixes → git → SpecStory.
2. One file only; no personal names; human vs AI labelled each step.

---

## Run locally (current)

**Backend:**
```bash
cd backend
./run-dev.sh
```

**Frontend:**
```bash
nvm use 20
cd frontend && npm run dev
```

**Check API:**
```bash
curl http://localhost:8080/api/tickets/summary
```

**Push to git:**
```bash
git add .
git commit -m "Your message"
git push -u origin main
```

Do not commit `.env`.
