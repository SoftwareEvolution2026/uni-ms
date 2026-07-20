# Academic Registry architecture

## System shape

The backend is one Spring Boot modular monolith backed exclusively by PostgreSQL.
The frontend is one React SPA. The public API prefix is `/api/v1`; there is no
parallel legacy or v2 contract.

Active business modules are limited to:

- `identity`: authentication, user identity and refresh-token rotation;
- `academiccatalog.department`: Department management;
- `academiccatalog.course`: Course management;
- `dashboard`: read-only aggregate statistics;
- `common`: technical security, audit, Problem Details and shared API primitives.

## Module layers

Each business module follows this dependency direction:

```text
api → application → domain
          ↓
    infrastructure
```

The API layer validates HTTP input and delegates to application services. The
application layer owns transactions and use cases. Domain entities own lifecycle
state. Infrastructure implements persistence and cross-module query ports.
Controllers cannot depend on repositories, and shared infrastructure cannot own
Department or Course rules. ArchUnit verifies these constraints.

Department and Course do not import one another's internal Java types. Course
checks Department availability through an application port implemented with a
read-only adapter. Department checks Course references through the same pattern.
PostgreSQL also enforces `courses.department_id` with `ON DELETE RESTRICT`.

## Persistence

Flyway is the only schema migration mechanism. PostgreSQL migrations live under
`db/migration/postgresql`; historic legacy migration files remain in Git but are
not configured as runtime locations. Hibernate uses `ddl-auto=validate`.

The active schema contains only `users`, `refresh_tokens`, `departments`,
`courses`, and `audit_logs`. Department and Course use optimistic versions and
soft deletion. Codes remain unique while rows are in trash. Timestamps are UTC.

## Security and errors

Authentication uses short-lived signed JWT access tokens and rotating opaque
refresh tokens stored only as hashes. Refresh-token family reuse revokes the
family. BCrypt protects passwords. Routes are stateless and CORS origins are
explicit.

Roles are `ADMIN` and `ACADEMIC_MANAGER`. Both operate the registry; permanent
deletion is restricted to `ADMIN`. MVC, validation, security and business errors
use the shared RFC 9457-style `application/problem+json` representation.

## Runtime and tests

OpenAPI exposes the authenticated contract. Actuator exposes health and info.
Integration, repository and security behavior runs against PostgreSQL 16 through
Testcontainers; no in-memory database substitutes for PostgreSQL semantics.
