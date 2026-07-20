# Course API

The Course module is exposed below `/api/v1/courses`. Every endpoint requires an
authenticated user with either `ADMIN` or `ACADEMIC_MANAGER`; permanent deletion
requires `ADMIN`.

Course codes are normalized to uppercase and remain unique even while a Course is
soft-deleted. A Course must reference an active, non-deleted Department. Updates
use the `version` supplied by the latest response for optimistic concurrency.

## Lifecycle

- `POST /api/v1/courses` creates a Course.
- `GET /api/v1/courses/{id}` returns a non-deleted Course.
- `PUT /api/v1/courses/{id}` fully updates a Course.
- `DELETE /api/v1/courses/{id}` moves it to trash.
- `GET /api/v1/courses/trash` searches trash.
- `POST /api/v1/courses/{id}/restore` restores a Course when its Department is available.
- `DELETE /api/v1/courses/{id}/permanent` permanently removes a trashed Course.

The normal and trash lists accept `search`, `departmentId`, `semester`,
`academicYear`, `status`, `page`, `size`, and `sort`. Search matches Course name,
code, description, and academic year. Page size is limited to 100. Sort uses
`property,direction` and only documented Course properties are accepted.

All failures use the shared Problem Details representation with a stable `code`,
and validation failures also include `fieldErrors`.
