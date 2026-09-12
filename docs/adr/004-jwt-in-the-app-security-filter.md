# ADR-004 — JWT issued and validated in the app's own security filter

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-04, recorded 2026-09-12 |
| **Deciders** | platform |

## Context

One deployable (ADR-001) serves the SPA, GraphQL and the iOS pipeline
facade. Identity has to be stateless (replicas, no session store), work
over plain `fetch`/axios and over `EventSource` (which cannot set headers),
and coexist with two other credentials: Google / Strava OAuth2 logins that
end in a Stride token, and long-lived personal tokens for the iOS client.

## Decision

- **The app issues and validates its own HS256 JWT.** `JwtProvider` (jjwt)
  signs with `JWT_SECRET` (base64, ≥ 256 bits), `jwt.expiration` 1800 s by
  default, refresh tokens 7× longer. `JwtAuthenticationFilter`
  (`OncePerRequestFilter`) reads `Authorization: Bearer …` or, for SSE, the
  `token` query parameter, validates the token and loads the user through
  `CustomUserDetailsService` into the `SecurityContext`.
- **Two filter chains** in `SecurityConfiguration`: `/pipeline/v1/**`
  (order 1, `PipelineTokenFilter`: a hashed `tri_…` personal token from the
  `pipeline_tokens` table *or* a regular JWT, no CORS) and everything else
  (order 2, JWT + the per-IP `RateLimitingFilter` on login / register).
  Stateless sessions, CSRF off, 401 from `HttpStatusEntryPoint`.
- **Public paths are code, not config:** `/api/v1/auth/**`, the Strava
  callback, Swagger, `/uploads/**`, `/actuator/health/**`,
  `/actuator/prometheus`; `/api/v1/admin/**` needs `ROLE_ADMIN`; everything
  else is authenticated.
- Controllers read identity only through `BaseController.getAuthenticatedUserId()`;
  services take the id as a parameter (`ArchitectureTest`:
  `services_do_not_read_the_servlet_request`).

## Alternatives considered

- **Validate at a gateway, propagate headers** — the `microservices` branch
  does this. With one process there is nothing to propagate to; a gateway
  would exist only to validate a token the app can validate itself.
- **Server-side sessions** — a session store (Redis) or sticky replicas;
  rejected, the token already carries what the app needs.
- **Spring's OAuth2 resource server with an external issuer** — the right
  move if a second application ever needs the same identity; today the
  issuer would be this app anyway.

## Consequences

- One secret, one place to rotate it, one class to review for expiry and
  algorithm checks. Rotation logs every user out (there is no key id or
  overlap window).
- Every authenticated request costs one `users` lookup by email
  (`CustomUserDetailsService`); at 10 pool connections this is part of the
  DB load ([db-pool-exhausted.md](../runbooks/db-pool-exhausted.md)).
- A malformed or too-short secret fails at the first token operation, not
  at boot (the key is decoded per call) — every login 500s while the rest
  of the app works.
- Token-in-query-string for SSE means the token can land in proxy access
  logs; nginx access logging is on in the frontend container.

## Known gaps

- No JWT revocation before expiry; logout is client-side. Personal
  `tri_` tokens *are* revocable (`pipeline_tokens.revoked`), a second auth
  path to keep in sync.
- The per-IP limiter trusts the first `X-Forwarded-For` entry; behind a
  proxy that does not set it, all users share one bucket.
