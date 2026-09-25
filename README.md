# Support Ticket Management

Two-application layout: `backend/` (Spring Boot) and `frontend/` (Next.js).

## Prerequisites

- Java 21
- Node.js (with npm)
- PostgreSQL for the running application
- Maven (optional; the backend Maven Wrapper can be used instead)

H2 will be used for automated tests. It is not the runtime store for the running application.

## Backend

```bash
cd backend
./mvnw test
```

## Frontend

```bash
cd frontend
npm install
npm run build
```

Default local ports (later tasks): API `8080`, UI `3000`.
