# ADR-003 — gRPC for cluster-internal calls, REST for the public contract

| | |
|---|---|
| **Status** | Accepted — supersedes the REST `/internal/**` endpoints removed in #150 |
| **Date** | 2026-08-17 (Phase 23) |
| **Deciders** | platform |

## Context

Phases 2–22 exposed service-to-service reads as REST `/internal/**`
endpoints: untyped JSON, hand-written clients per caller, no deadlines, and
a second HTTP surface per service to secure. Callers included
challenge-goal → activity (goal progress), social → activity (feed
hydration), ai → activity (chat context), pipeline → activity (power
enrichment).

## Decision

Cluster-internal calls use gRPC with proto contracts in
`shared/grpc-contracts`, blocking stubs resolved through Eureka
(`discovery:///service-id`, port published as `gRPC.port` metadata),
plaintext inside the cluster, a deadline on every call, and Resilience4j
around every stub. The public contract (`/api/v1`, `/graphql`,
`/pipeline/v1`) and every BFF composition stay REST at the gateway. The
`/internal` controllers were deleted (#150); those paths now 403 like any
unknown route.

## Alternatives considered

- **Keep REST internally with OpenAPI-generated clients** — typed, but
  still no deadlines by default and a public-looking surface to protect.
- **gRPC everywhere including the browser (grpc-web)** — the frontend and
  iOS client would need new transports for no product gain.
- **Async only (events for everything)** — reads that need an answer now
  (ownership check before an AI chat) do not fit.

## Consequences

- Contract changes are explicit proto diffs reviewed with the callers.
- Every internal call has a deadline; `StatusRuntimeException` is a
  recorded exception in each breaker, so a dead peer opens the circuit
  (`CircuitBreakerOpen`).
- Build images must be glibc-based for `protoc` (Debian build stage,
  Alpine runtime).
- Two transports to trace; OpenTelemetry covers both.

## Known gaps

- No mTLS inside the cluster; the NetworkPolicy is the only isolation.
