# Department module decisions

Department codes are trimmed, uppercased, and globally unique across active and soft-deleted
rows. Department names are not unique: the same display name may legitimately occur in different
faculties, while the code remains the stable business identifier.

`PUT /api/v1/departments/{id}` uses a required `version` field in the JSON request. This keeps the
optimistic-locking contract explicit without introducing ETag parsing. A stale version returns
`DEPARTMENT_VERSION_CONFLICT` with HTTP 409.

Normal deletion is a soft delete and sets `deletedAt` using the UTC `Instant` clock. Repeating a
soft delete returns `DEPARTMENT_ALREADY_DELETED` with HTTP 409. Normal list and detail operations
exclude deleted rows. Restore preserves the previous Department status.

Permanent deletion requires `ADMIN`, requires the Department to be in trash, and checks all Course
rows through `DepartmentCourseReferenceQuery`. The PostgreSQL foreign key with `ON DELETE RESTRICT`
remains the final concurrency safeguard.
