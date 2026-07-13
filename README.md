# University Student Management System (uni-ms)

Version 1.0 — collaborative software-evolution project.

A **modular monolith**: one Spring Boot backend + one React (Vite) frontend, one shared
MySQL/MariaDB database. The system is split into vertical **modules**, one per development team.
Each module is an independent package/folder that plugs into the shared platform (security,
error handling, audit, database).

## Tech stack

| Layer     | Technology                                            |
| --------- | ----------------------------------------------------- |
| Frontend  | React 18 + Vite + TypeScript, React Router, Axios     |
| Backend   | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Auth      | JWT access token + rotating refresh token             |
| Database  | MySQL / MariaDB                                       |
| Migrations| Flyway (`backend/src/main/resources/db/migration`)    |
| Build     | Maven (backend), pnpm (frontend)                      |
| CI/CD     | GitHub Actions → cloud deploy (Team 6)                |

## Repository layout

```
uni-ms/
├── backend/                 # Spring Boot modular monolith
│   └── src/main/java/com/uni/ms/
│       ├── common/          # shared platform: security, audit, error handling
│       ├── user/            # Team 1 – user & role management
│       ├── auth/            # Team 1 – authentication (access + refresh tokens)
│       ├── student/         # Team 2 – student management        (stub)
│       ├── course/          # Team 3 – course & department        (stub)
│       ├── result/          # Team 4 – results & academic records (stub)
│       └── report/          # Team 5 – reports & analytics         (stub)
├── frontend/                # React + Vite app (same module split under src/features/)
└── docs/                    # SRS, architecture, branching strategy
```

## Modules & teams (10 members = 5 teams of 2)

| Team | Module            | Backend owner | Frontend owner |
| ---- | ----------------- | ------------- | -------------- |
| 1    | Auth & Users      | Student 1     | Student 2      |
| 2    | Student mgmt      | Student 3     | Student 4      |
| 3    | Course & Dept     | Student 5     | Student 6      |
| 4    | Results & Records | Student 7     | Student 8      |
| 5    | Reports/Analytics | Student 9     | Student 10     |
| 6    | Notifications / Config / CI-CD / Deploy | _left for now_ | |

## Quick start

### 1. Database
Use a local MySQL/MariaDB (or a free cloud one like Railway / Aiven). The database is created
automatically on first run via `createDatabaseIfNotExist=true`, so you only need the server
running. Copy `backend/.env.example` and adjust:

```
DB_URL=jdbc:mariadb://localhost:3306/uni_ms?createDatabaseIfNotExist=true
DB_USER=root
DB_PASSWORD=root
JWT_SECRET=change-me-to-a-long-random-string-at-least-32-chars
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run       # Flyway auto-creates the schema on startup
```
API runs on http://localhost:8081

### 3. Frontend
```bash
cd frontend
pnpm install
pnpm dev                  # http://localhost:5173
```

## Auth API (Team 1)

There is **no public self-registration**. An admin creates accounts and assigns roles.

| Method | Endpoint                | Body                                      | Auth  |
| ------ | ----------------------- | ----------------------------------------- | ----- |
| POST   | `/api/v1/auth/login`    | `{email,password}`                        | no    |
| POST   | `/api/v1/auth/refresh`  | `{refreshToken}`                          | no    |
| POST   | `/api/v1/auth/logout`   | `{refreshToken}`                          | no    |
| GET    | `/api/v1/users/me`      | —                                         | yes   |
| GET    | `/api/v1/users`         | —                                         | ADMIN |
| POST   | `/api/v1/users`         | `{fullName,email,password,roles:[...]}`   | ADMIN |
| PUT    | `/api/v1/users/{id}`    | `{fullName,email,roles:[...],enabled}`    | ADMIN |
| DELETE | `/api/v1/users/{id}`    | —                                         | ADMIN |

**Seeded accounts** (created on first startup): `admin@uni.ms` / `Admin123!` (ADMIN),
`lecturer@uni.ms` / `Lecturer123!`, `staff@uni.ms` / `Staff123!`, `student@uni.ms` / `Student123!`.

See [docs/BRANCHING.md](docs/BRANCHING.md) for the Git workflow every team follows.

## Contributing

All contributors must read [CONTRIBUTING.md](CONTRIBUTING.md) — it covers local setup, the
branching strategy, commit-message conventions, the pull-request process, and coding standards.

## Documentation

| Document | Purpose |
| -------- | ------- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Software architecture: modular-monolith design, module map & dependency rules, auth design, data model, deployment view |
| [docs/BRANCHING.md](docs/BRANCHING.md) | Git branching strategy and workflow every team follows |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Setup, commit conventions, PR process, coding standards |

Start with the [architecture document](docs/ARCHITECTURE.md) — it explains how the modules fit
together and the rules that let all five teams work in parallel without stepping on each other.
