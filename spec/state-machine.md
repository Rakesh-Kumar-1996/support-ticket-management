# State Machine Specification

**Authoritative enforcement:** backend service/domain layer.  
**HTTP for illegal transitions:** `409 Conflict` with `code`: `INVALID_STATUS_TRANSITION`.  
**UI:** may hide illegal actions; must not be the only control.

---

## 1. States

| Status | Meaning (informational) |
|--------|-------------------------|
| OPEN | Newly created; not started |
| IN_PROGRESS | Work started |
| RESOLVED | Work finished; awaiting close |
| CLOSED | Finished and closed |
| CANCELLED | Abandoned |

Informational meanings are not extra product rules; they only label the enum.

**Initial state:** every created ticket is `OPEN`. No other entry point exists.

---

## 2. Valid transitions

Exactly these five edges are allowed. No others.

| From | To | Allowed |
|------|-----|---------|
| OPEN | IN_PROGRESS | yes |
| OPEN | CANCELLED | yes |
| IN_PROGRESS | RESOLVED | yes |
| IN_PROGRESS | CANCELLED | yes |
| RESOLVED | CLOSED | yes |

```
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
  │            │
  └──► CANCELLED ◄──┘
```

---

## 3. Invalid transitions

**Closed-world rule:** any pair `(from, to)` not listed in §2 is invalid, including:

- Self-transitions (`OPEN` → `OPEN`, etc.)
- Skips (`OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`)
- Backward moves
- Cancel from `RESOLVED`, `CLOSED`
- Any change out of `CLOSED` or `CANCELLED`

### 3.1 Specified invalid examples (must be tested)

| From | To | Result |
|------|-----|--------|
| CLOSED | OPEN | invalid, 409 |
| RESOLVED | OPEN | invalid, 409 |
| CANCELLED | OPEN | invalid, 409 |
| OPEN | RESOLVED | invalid, 409 |
| RESOLVED | CANCELLED | invalid, 409 |

### 3.2 Full matrix

R = allowed (200). X = invalid (409). Rows = current; columns = target.

|  | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|--|------|-------------|----------|--------|-----------|
| OPEN | X | R | X | X | R |
| IN_PROGRESS | X | X | R | X | R |
| RESOLVED | X | X | X | R | X |
| CLOSED | X | X | X | X | X |
| CANCELLED | X | X | X | X | X |

If `to` is not a known status string, the request is **400 VALIDATION_ERROR**, not 409.

---

## 4. Enforcement rules

1. Load the ticket. Missing → 404 (do not apply transition logic).
2. Parse target `status`. Invalid enum → 400.
3. If `(current, target)` is X in the matrix → **409**, persist nothing.
4. If R → set status, bump `updatedAt`, persist.
5. Field `PATCH /api/tickets/{id}` and comment POST do **not** apply this table. If field PATCH includes `status`, the API returns **400 VALIDATION_ERROR** (not 409) and does not persist a transition. Illegal transitions are evaluated only on `PATCH /api/tickets/{id}/status`.

The database must not be the only place that knows this graph.

---

## 5. Concurrency

Not specified by product requirements. **AD-SM-001:** last write wins on status if two requests race; no optimistic-lock requirement in v1. Implementers may add a version column later without changing the transition table.
