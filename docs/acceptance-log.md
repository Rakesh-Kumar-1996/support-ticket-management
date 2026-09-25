# Final acceptance log (T-024)

**Run date (agent):** 2026-09-25  
**Environment:** Backend tests executed on agent host; PostgreSQL and Node/npm **not** available on agent host.

Legend: **PASS** = verified in this run · **NOT VERIFIED** = not executed here · **N/A** = not applicable

| AC | Criterion | Status | Evidence |
|----|-----------|--------|----------|
| AC-001 | Create from UI | NOT VERIFIED | UI not exercised in agent; API create covered by `TicketApiIntegrationTest` |
| AC-002 | List tickets | PASS | `TicketApiIntegrationTest.listSearchFilterAndOrdering` |
| AC-003 | Detail + comments | PASS | `TicketApiIntegrationTest.getDetailIncludesCommentsInOrder` |
| AC-004 | Update fields | PASS | `TicketApiIntegrationTest.patchFieldsUpdatesAndClearsNullableFields` |
| AC-005 | Clear assignee | PASS | Same PATCH test clears assignee/description |
| AC-006 | Add comment | PASS | `TicketApiIntegrationTest.addCommentAndValidation` |
| AC-007 | Keyword search | PASS | List integration tests (`q` title/description) |
| AC-008 | Status filter | PASS | List integration tests `status=OPEN` |
| AC-009 | Search + status | PASS | List integration tests combined params |
| AC-010 | Five valid transitions | PASS | `smValidTransitionsReturn200`, `smHappyPathOpenThroughClosed` |
| AC-011 | Invalid → 409 | PASS | `smClosedToOpenReturns409`, `smInvalidTransitionsReturn409AndLeaveStatus` |
| AC-012 | PostgreSQL restart | NOT VERIFIED | See `docs/verification-restart.md` |
| AC-013 | 400 error JSON | PASS | API integration + `TicketRequestValidatorTest` |
| AC-014 | 404 error JSON | PASS | `TicketApiIntegrationTest.unknownTicketReturns404` |
| AC-015 | UI meaningful errors | NOT VERIFIED | Agent: no `npm run dev`; implementation: `ApiErrorBanner` + `docs/manual-frontend-ac.md` |
| AC-016 | SM integration tests | PASS | Backend 80 tests, 0 failures (2026-09-25 agent run) |
| AC-017 | No secrets committed | PASS | T-023 review |

## Automated runs (agent)

| Check | Status | Detail |
|-------|--------|--------|
| Backend `./mvnw test` | PASS | 80 tests, 0 failures |
| Frontend `npm run test` | NOT VERIFIED | `npm` not installed on agent host |
| Frontend `npm run build` | NOT VERIFIED | `npm` not installed on agent host (user reported prior PASS) |

## State-machine spot checks (backend API, automated)

| Transition | Expected | Status |
|------------|----------|--------|
| OPEN → IN_PROGRESS → RESOLVED → CLOSED | 200 each step | PASS (`smHappyPathOpenThroughClosed`) |
| CLOSED → OPEN | 409 | PASS (`smClosedToOpenReturns409`) |
| OPEN → RESOLVED, OPEN → CLOSED, IN_PROGRESS → CLOSED, RESOLVED → CANCELLED, CANCELLED → OPEN | 409 | PASS (parameterized `smInvalidTransitionsReturn409AndLeaveStatus`) |
