# Manual frontend acceptance checklist

Run with the stack up:

1. Backend: `cd backend && ./mvnw spring-boot:run` (port **8080**)
2. Frontend: `cd frontend && npm run dev` (port **3000**)

Open **http://localhost:3000**.

| ID | Scenario | Steps | Expected |
|----|----------|-------|----------|
| FE-001 | Create ticket | **New ticket** → fill title (required) → **Create ticket** | Redirects to detail; status **OPEN**; priority **MEDIUM** if unchanged |
| FE-002 | List and detail | Home lists tickets; click a row | Detail shows id, fields, comments section |
| FE-003 | Edit ticket | On detail, **Edit ticket** → change title/description/priority/assignee → **Save** | **200** behavior: fields update; `updatedAt` changes; status unchanged |
| FE-004 | Add comment | On detail, **Add comment** with body → submit | Comment appears at bottom in API order; no edit/delete controls |
| FE-005 | Search and filter | Home: search keyword; filter status; combine both | List matches backend `q` + `status` AND semantics |
| FE-006 | Valid transition | OPEN ticket → **Move to IN PROGRESS** (or other allowed button) | Status updates; only allowed targets shown |
| FE-007 | Illegal transition | Walk ticket to **CLOSED**; if UI hides reopen, use API or prior 409 test | If 409 occurs, banner shows API message (e.g. transition not allowed), status unchanged |
| FE-008 | Validation 400 | Create with blank title or comment with blank body | Banner shows API **message**; field errors on title/body when provided |
| FE-009 | Missing ticket 404 | Open `/tickets/00000000-0000-0000-0000-000000000099` | Shows API message **Ticket was not found.** (not a generic failure) |

## Error spot checks (T-020)

- Stop backend → reload list → **Connection problem** message and **Retry**.
- Invalid list status (if triggered via API) → validation message from API.
- Status **409** → **Action not allowed** heading with transition message from API.

Sign-off: _______________  Date: _______________
