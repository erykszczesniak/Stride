# ADR-004 — JWT validated only at the gateway; identity propagated as headers

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-07 (Phase 1), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

Twelve services, one identity. Validating the JWT in every service means
twelve copies of the secret, twelve places to get expiry or algorithm
checks wrong, and twelve config changes on rotation.

## Decision

The API gateway validates the HS256 token (signature, expiry, claims),
strips any inbound `X-User-*` headers, and forwards the verified identity
as `X-User-Id`, `X-User-Email`, `X-User-Roles`. Services translate those
headers into a Spring Security `Authentication` with the shared
`HeaderAuthenticationFilter` and never see the token. Paths that must not
require a token (`/api/v1/auth/**`, the Strava callback, `/uploads/**`,
`/actuator/**`, `/pipeline/v1/**` with its own `tri_` tokens) are a list in
Config Server bound through `@ConfigurationProperties`.

This is safe only because the gateway strips spoofed headers and, in
Kubernetes, the `NetworkPolicy` lets only the gateway (and Prometheus)
reach domain services.

## Alternatives considered

- **Validate in every service** (shared library) — robust to a misrouted
  request, but multiplies secrets and config; rejected for operational
  cost.
- **Opaque session in Redis, looked up per request** — one more hop per
  call; JWT already carries what services need.
- **Service mesh with mTLS and JWT filters** — see ADR-005.

## Consequences

- One place to rotate the secret and one place to review auth logic.
- The header contract is a trust boundary: a service reachable from
  outside the gateway is a vulnerability, hence the NetworkPolicy and the
  `services-talk-to-services` rule.
- Configuration binding bugs at the gateway are security bugs: an empty
  public-path list made every login 401
  ([post-mortem 2026-08-16](../postmortems/2026-08-16-gateway-public-paths.md)).

## Known gaps

- No token revocation before expiry (logout is client-side).
- The `tri_` personal tokens for iOS are validated inside pipeline-service,
  a second auth path to keep in sync.
