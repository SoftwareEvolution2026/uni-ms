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

| Team | Feature branch prefix |
| ---- | --------------------- |
| 1    | `feature/auth-*`, `feature/user-*` |
| 2    | `feature/student-*`   |
| 3    | `feature/course-*`, `feature/dept-*` |
| 4    | `feature/result-*`    |
| 5    | `feature/report-*`    |
| 6    | `feature/ci-*`, `feature/deploy-*`, `feature/notify-*` |

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
