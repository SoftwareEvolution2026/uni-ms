# Authentication security

The API returns opaque refresh tokens in JSON. It does not use authentication cookies, so the
security filter chain remains stateless and CSRF protection is disabled. A browser client must
avoid persistent JavaScript-accessible storage where possible and must treat any XSS issue as a
refresh-token compromise. Moving refresh tokens to `Secure`, `HttpOnly`, `SameSite` cookies would
require a deliberate transport migration and corresponding CSRF protection.

Access tokens are short-lived signed JWTs. They contain the stable user id as `sub`, a unique
`jti`, issuer, audience, expiration, and one authority. They contain no email, password, or token
material. The symmetric signing secret is appropriate while this remains a single monolith and
must be supplied through the environment.

Login throttling is keyed by normalized email and client IP. The current implementation is held
in application memory, so counters are neither shared nor preserved across multiple instances or
restarts. The `LoginAttemptPolicy` interface is the replacement boundary for a future Redis-backed
implementation.

Initial administrator creation is controlled by `INITIAL_ADMIN_ENABLED` and the related
environment variables. Production defaults to disabled and has no built-in credentials.
