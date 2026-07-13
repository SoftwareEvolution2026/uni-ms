# Software Architecture Document — uni-ms

## 1. Architectural style

**Modular monolith** with a **layered architecture inside each module**.

- One deployable backend (Spring Boot) and one deployable frontend (React SPA).
- The backend is divided into **vertical feature modules** (one per team). Each module owns
  its controllers, services, repositories, entities and DTOs.
- A shared **`common`** package provides cross-cutting platform services that every module
  depends on: security (JWT), global error handling, and audit logging.

Why a modular monolith (not microservices) for a 10-person student project:
- One repo, one build, one deployment — realistic for the timeline and the CI/CD requirement.
- Clear module boundaries still teach separation of concerns and configuration management.
- Modules can be extracted into services later — this is the *evolution* story (Lehman's laws).

## 2. Module map & dependencies

```
                 ┌───────────────────────┐
                 │   common (platform)   │  security · audit · errors · base entity
                 └───────────┬───────────┘
                             │ (every module depends on common)
   ┌──────────┬─────────────┼─────────────┬─────────────┐
   ▼          ▼             ▼             ▼             ▼
 auth/user  student       course        result        report
 (Team 1)   (Team 2)      (Team 3)      (Team 4)      (Team 5)
                │             ▲             │             ▲
                └──────► needs course ◄─────┘             │
                              student results feed reports┘
```

Allowed dependency direction: **feature modules → common only.** Feature modules must NOT
import each other's internal classes. When one module needs another's data, it goes through a
**public service interface** (e.g. `StudentDirectory`) exposed by the owning module — never
through its repository or entity directly. This keeps the anti-corruption boundary that lets
teams work in parallel.

## 3. Layers inside a module

```
Controller  (REST, validation, DTO in/out)      @RestController
   │
Service     (business logic, transactions)       @Service
   │
Repository  (persistence)                         Spring Data JPA
   │
Entity      (JPA @Entity, maps to a table)
```

## 4. Cross-cutting platform (`common`)

- **Security** — stateless JWT. `JwtAuthenticationFilter` validates the access token on every
  request and populates the `SecurityContext`. `SecurityConfig` defines which routes are public.
- **Audit** — `AuditLog` entity + `AuditService`; security-relevant events (login, register,
  token refresh, admin actions) are recorded. Satisfies the *Audit Logs* requirement.
- **Error handling** — `GlobalExceptionHandler` (`@RestControllerAdvice`) turns exceptions into
  a consistent `ErrorResponse` JSON shape. No stack traces leak to clients.
- **Base entity** — `id`, `createdAt`, `updatedAt` auditing columns shared by all entities.

## 5. Authentication design (Team 1)

Two-token model:

| Token         | Format            | Lifetime | Stored where            | Purpose                    |
| ------------- | ----------------- | -------- | ----------------------- | -------------------------- |
| Access token  | signed JWT        | 15 min   | client memory           | sent as `Bearer` on API calls |
| Refresh token | opaque UUID (hashed in DB) | 7 days | `refresh_tokens` table | mints new access tokens    |

Flow:
1. `login` / `register` → returns `{accessToken, refreshToken}`.
2. Client calls APIs with `Authorization: Bearer <accessToken>`.
3. On `401`, client calls `/auth/refresh` with the refresh token.
4. Refresh **rotates**: the old refresh token is revoked and a new one issued (detects reuse).
5. `logout` revokes the refresh token.

Passwords are hashed with BCrypt. The refresh token is stored **hashed** so a DB leak can't be
replayed. Roles (`ROLE_ADMIN`, `ROLE_LECTURER`, `ROLE_STUDENT`, `ROLE_STAFF`) drive
authorization via `@PreAuthorize` / `SecurityConfig`.

## 6. Data model (V1)

`users`, `user_roles`, `refresh_tokens`, `audit_logs` (see `db/migration/V1__init.sql`).
Later teams add `students`, `courses`, `departments`, `results`, etc. as new Flyway migrations
(`V2__…`, `V3__…`) — migrations are **append-only and never edited once merged**.

## 7. Deployment view (Team 6)

```
Developer (VS Code) → Git → GitHub → PR + review → GitHub Actions
   → build (mvn / pnpm) → tests → deploy frontend + backend + managed Postgres → live URL
```
