# Branching Strategy & Git Workflow

A trunk-based, PR-driven flow that maps cleanly onto the assignment's configuration-management
marks (Part C) and CI/CD (Part E).

## Branches

- **`main`** — always deployable. Protected: no direct pushes; merge only via reviewed PR.
- **`develop`** — integration branch where all module features come together before a release.
- **`feature/<module>-<short-desc>`** — one per unit of work. Examples:
  - `feature/auth-refresh-tokens`
  - `feature/student-crud`
  - `feature/course-search`
- **`fix/<desc>`** — bug fixes. **`chore/<desc>`** — tooling/docs.

```
main  ──●──────────────────●────────────────●     (tagged releases: v1.0.0, v1.1.0)
         \                 /                /
develop  ─●──●────●───●───●────●───●───●───●       (integration)
              \      /         \      /
feature/...    ●──●            ●──●                (short-lived, one feature each)
```

## Team → branch ownership

Each team owns a prefix. Only that team creates branches under its prefix, so two teams never
touch the same files on the same branch.

| Team | Module | Branch prefix |
| ---- | ------ | ------------- |
| 1    | Auth & Users        | `feature/auth-*`, `feature/user-*` |
| 2    | Student Management  | `feature/student-*` |
| 3    | Course & Department | `feature/course-*`, `feature/dept-*` |
| 4    | Results & Records   | `feature/result-*` |
| 5    | Reports & Analytics | `feature/report-*`, `feature/analytics-*` |
| 6    | Notifications / Config / CI-CD / Deploy | `feature/notify-*`, `feature/settings-*`, `feature/ci-*`, `feature/deploy-*` |

## Anticipated feature branches

The concrete branches each team is expected to create for Version 1.0. This is a planning
guide, not a hard limit — split further if a branch gets too big, and add `fix/*` branches as
bugs surface. Keep each branch to **one** cohesive unit of work.

**Team 1 — Auth & Users**
- `feature/auth-login-refresh` — login, refresh-token rotation, logout ✅ *(done)*
- `feature/user-management` — admin create / list / delete users with roles ✅ *(done)*
- `feature/user-edit` — update a user's details and roles
- `feature/auth-password-change` — let a user change their own password

**Team 2 — Student Management**
- `feature/student-crud` — create / read / update / delete students
- `feature/student-search` — search, filter and paginate the student list
- `feature/student-profile` — student detail/profile view

**Team 3 — Course & Department Management**
- `feature/dept-crud` — departments CRUD
- `feature/course-crud` — courses CRUD (linked to a department)
- `feature/course-assignment` — assign lecturers / register students to courses

**Team 4 — Results & Academic Records**
- `feature/result-entry` — record and edit student results per course
- `feature/result-gpa` — GPA / grade computation
- `feature/result-transcript` — per-student academic transcript

**Team 5 — Reports, Dashboard & Analytics**
- `feature/report-dashboard` — summary dashboard with key stats
- `feature/analytics-charts` — charts (enrolments, pass rates, etc.)
- `feature/report-export` — export reports (PDF / CSV)

**Team 6 — Notifications, Config, CI-CD, Deployment** *(deferred for now)*
- `feature/settings-config` — application configuration & settings screen
- `feature/notify-email` — email / in-app notifications
- `feature/ci-pipeline` — GitHub Actions build + test workflow
- `feature/deploy-cloud` — cloud deployment (Render / Railway / Azure / …)

**Cross-cutting (any team, as needed)**
- `fix/<desc>` — bug fixes  ·  `chore/<desc>` — tooling/config  ·  `docs/<desc>` — documentation

## Workflow (every change)

1. `git switch develop && git pull`
2. `git switch -c feature/student-crud`
3. Commit often, meaningful messages (Conventional Commits):
   - `feat(student): add create-student endpoint`
   - `fix(auth): reject expired refresh tokens`
   - `test(course): cover duplicate-code validation`
4. `git push -u origin feature/student-crud`
5. Open a **Pull Request → `develop`**. Fill the PR template.
6. At least **one reviewer from another team** approves. CI must be green.
7. Squash-merge. Delete the feature branch.

## Integration & releases (Part D)

- Merge `develop → main` when a milestone is reached.
- Tag: `git tag -a v1.0.0 -m "Version 1.0"` then `git push --tags`.
- Version 1.1 = the six evolution improvements, each its own PR, released as `v1.1.0`.

## Anti-patterns to avoid

- ❌ Long-lived feature branches that drift for weeks (merge/rebase from `develop` often).
- ❌ Committing directly to `main`.
- ❌ One giant "final" commit — commit incrementally.
- ❌ Editing an already-merged Flyway migration — add a new `V*` file instead.
- ❌ Merging your own PR without review.
