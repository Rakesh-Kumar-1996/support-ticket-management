# PostgreSQL persistence / restart verification (PER-001, AC-012, T-022)

**Status in this repository verification run:** **NOT VERIFIED** — no PostgreSQL server was available in the agent environment (`pg_isready` absent, `postgresql` service inactive). H2 test results do **not** satisfy this check.

## Expected runtime configuration

From `backend/src/main/resources/application.yml` and `.env.example`:

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` | e.g. `jdbc:postgresql://localhost:5432/support_tickets` |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |

Copy `.env.example` to `.env` locally (`.env` is gitignored). **Do not commit real credentials.**

Flyway applies `backend/src/main/resources/db/migration/V1__tickets_and_comments.sql` on startup.

## Manual PER-001 procedure

1. **Create database** (example):

   ```bash
   createdb support_tickets
   ```

2. **Export env** (replace placeholders):

   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/support_tickets
   export SPRING_DATASOURCE_USERNAME=<username>
   export SPRING_DATASOURCE_PASSWORD=<password>
   ```

3. **Start API** (terminal A):

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

4. **Create ticket** (terminal B):

   ```bash
   curl -s -X POST http://localhost:8080/api/tickets \
     -H 'Content-Type: application/json' \
     -d '{"title":"PER-001 ticket","description":"restart check","priority":"HIGH","assignee":"qa"}'
   ```

   Record the `id` from the JSON response as `TICKET_ID`.

5. **Add comment**:

   ```bash
   curl -s -X POST "http://localhost:8080/api/tickets/${TICKET_ID}/comments" \
     -H 'Content-Type: application/json' \
     -d '{"body":"PER-001 comment","author":"qa"}'
   ```

6. **Stop** Spring Boot in terminal A (Ctrl+C).

7. **Start** Spring Boot again with the same env vars.

8. **GET ticket**:

   ```bash
   curl -s "http://localhost:8080/api/tickets/${TICKET_ID}"
   ```

9. **Expected:** HTTP 200; same `id`, `title`, `description`, `priority`, `assignee`, `status` `OPEN`; `comments` array contains the comment body and author from step 5.

## Sign-off

| Field | Value |
|-------|--------|
| Verified by | |
| Date | |
| PostgreSQL version | |
| Result | PASS / FAIL |
