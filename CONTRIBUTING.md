# Contributing to uni-ms

This project is built collaboratively by 5 development teams (2 members each). Please follow
these conventions so we can integrate everyone's work smoothly.

## Prerequisites

- JDK 17
- Maven 3.9+
- Node 20+ and npm 10+
- Docker Desktop for the official PostgreSQL 16 development database

## Local setup

```bash
# clone
git clone https://github.com/SoftwareEvolution2026/uni-ms.git
cd uni-ms

# backend
cd backend
cp .env.example .env           # adjust DB creds if needed
mvn spring-boot:run            # http://localhost:8081

# frontend (new terminal)
cd frontend
npm install
npm run dev                    # http://localhost:5173
```

The database schema is created automatically by Flyway on startup, and demo accounts are
seeded (see the README for credentials).

## Branching

We use short-lived feature branches off `develop`. See [docs/BRANCHING.md](docs/BRANCHING.md)
for the full strategy and each team's branch prefix.

```bash
git switch develop && git pull
git switch -c feature/course-search
```

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(course): add course search endpoint
fix(auth): reject expired refresh tokens
test(course): cover duplicate-code validation
docs(readme): document deployment steps
```

Scope = your module (`identity`, `department`, `course`, `dashboard`, `shared`, `ci`).
Commit small and often; do not bundle unrelated changes.

## Pull requests

1. Push your feature branch and open a PR **into `develop`**.
2. Fill in the PR template.
3. Get at least **one review from a member of another team**.
4. CI (build + tests) must be green.
5. Squash-merge and delete the branch.

## Coding standards

**Backend (Java / Spring Boot)**
- Layered per module: `controller → service → repository → entity`.
- A module may depend on `common` and on another module's **public service** only —
  never another module's repository or entity.
- Validate request DTOs with `jakarta.validation` annotations.
- Throw `ApiException` (or a subclass) for expected errors; never leak stack traces.
- New tables go in a **new** Flyway migration (`V2__…`, `V3__…`). Never edit a merged migration.
- Write at least one test per feature (unit or MockMvc slice).

**Frontend (React / TypeScript)**
- TypeScript `strict` must pass (`npm run build`).
- Call the backend through the shared `api` client (`src/api/client.ts`) — auth is automatic.
- Put each module's UI under `src/features/<module>/` or `src/pages/`.
- Use the toast service for feedback and the `Modal` / `ConfirmDialog` components for dialogs —
  no `window.alert` / `window.confirm`.

## Before you push

```bash
cd backend  && mvn test        # backend tests pass
cd frontend && npm run build   # typecheck + build pass
```
