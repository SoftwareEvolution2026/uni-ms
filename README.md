# University Academic Registry

`uni-ms` is a modular academic registry for identity, Departments, Courses and
operational Dashboard statistics. It consists of a Spring Boot modular monolith
and a React single-page application.

## Technology

- Java 17, Spring Boot 3.3, Spring Security and JWT access tokens
- rotating, hashed opaque refresh tokens
- PostgreSQL 16, Flyway and Hibernate schema validation
- RFC 9457-style Problem Details, OpenAPI and Actuator
- Testcontainers PostgreSQL and ArchUnit
- React 18, TypeScript, Vite 8, React Router and Axios

## Modules

```text
backend/src/main/java/com/uni/ms
├── identity                 authentication and user identity
├── academiccatalog
│   ├── department           Department lifecycle and search
│   └── course               Course lifecycle and search
├── dashboard                registry statistics
└── common                   security, audit, errors and shared API types
```

Feature modules use `api`, `application`, `domain` and `infrastructure` layers.
Controllers never access repositories directly. The Student module and all other
legacy business modules are outside the active application scope.

## Local setup

Requirements: Java 17, Maven, Docker, Node.js and npm.

1. Copy `backend/.env.example` to `backend/.env` and replace all example secrets.
2. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

3. Export the backend environment and run the API:

   ```bash
   cd backend
   set -a && source .env && set +a
   mvn spring-boot:run
   ```

4. Run the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

The API is available at `http://localhost:8081/api/v1`, Swagger UI at
`http://localhost:8081/swagger-ui.html`, health at
`http://localhost:8081/actuator/health`, and the frontend at
`http://localhost:5173`.

Initial administrator creation is disabled by default. To bootstrap a local
administrator, explicitly set `INITIAL_ADMIN_ENABLED=true` and provide
`INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_EMAIL` and a strong `INITIAL_ADMIN_PASSWORD`.
Never keep those bootstrap credentials enabled in production.

## API overview

- `/api/v1/auth`: login, refresh, logout, current identity and password change
- `/api/v1/departments`: CRUD, search, filters, pagination, trash and restore
- `/api/v1/courses`: CRUD, search, filters, pagination, trash and restore
- `/api/v1/dashboard`: non-deleted Department and Course statistics

`ADMIN` and `ACADEMIC_MANAGER` can use the registry. Only `ADMIN` may permanently
delete records. All failures use `application/problem+json`.

## Verification

```bash
cd backend && mvn clean test && mvn verify
cd frontend && npm run typecheck && npm run build
```

See [architecture](docs/ARCHITECTURE.md), [security](docs/SECURITY.md),
[Departments](docs/DEPARTMENTS.md), [Courses](docs/COURSES.md), and the
[contribution guide](CONTRIBUTING.md).
