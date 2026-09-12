# Gateway ignored the public-path list: every login 401 on the composed stack

**Date:** 2026-08-16 · **Duration:** discovered and fixed within the Phase 22 verification session · **Severity:** would have been critical in prod (auth unusable); no users affected · **Author:** platform · **Status:** reviewed

Source of record: PR #145 ("full-stack wiring, image fixes, docs and E2E verification"), fix 4 of 4.

## Summary

The API gateway keeps a list of paths that must bypass JWT validation
(`/api/v1/auth/**`, the Strava OAuth callback, `/uploads/**`, `/actuator/**`,
`/pipeline/v1/**`). The list lives in Config Server's `api-gateway.yml`. The
gateway read it through a flat `@Value("${jwt.public-paths}")` placeholder.
Spring's placeholder resolution does not bind YAML *lists* — the property is
simply absent — so the list resolved to empty, every path required a token,
and `POST /api/v1/auth/login` (the endpoint that issues tokens) answered 401.
Each service in isolation, and the unit tests, were green: they never read
the list from Config Server.

## Impact

- On the first real `docker compose up` of the 24-container stack, registration and login were impossible; the frontend could not get past the login page.
- No production impact: the branch had never been deployed with Config Server before. The bug had existed since Phase 1 (gateway scaffold).

## Timeline

| Time (relative) | Event |
|---|---|
| T0 | Full stack booted for Phase 22 E2E verification (PR #145) |
| T0 + minutes | Frontend login returns 401; `curl` to `/api/v1/auth/login` confirms 401 from the gateway, not from user-service |
| | Gateway logs show `JwtAuthenticationFilter` rejecting the request; user-service never receives it |
| | `curl config-server:8888/api-gateway/default` shows the list is served correctly → the *binding* is wrong, not the config |
| | `@Value` replaced with a `@ConfigurationProperties` binder (`JwtFilterProperties`); test updated to load the list from YAML |
| T0 + hours | E2E verification passes end to end (auth, feed, notifications, GraphQL, pipeline tokens, AI chat) |

## Root cause

Configuration was consumed through an API (`@Value` placeholder) that cannot
express the data type (a list) that the configuration used. Nothing failed
loudly: an unbound list is an empty list, and an empty allow-list is the
"secure" default, so the failure mode looked like a working security filter.

Contributing: the gateway's unit test constructed the filter with an inline
list instead of binding from a YAML document, so the test could not catch a
binding bug. The standalone profile had `jwt.public-paths` inline in
`application.yml`, where `@Value` on a list *also* silently yields nothing —
but the standalone smoke test only hit `/actuator/**`, which the filter
exempted through a second, hard-coded path.

## What went well

- The composed-stack E2E pass was scheduled as an explicit phase, so the bug surfaced before any deploy.
- Config Server's `/api-gateway/default` endpoint made it a two-minute job to prove the config was right and the consumer was wrong.

## What went badly

- Three earlier phases shipped a gateway that had never once run against Config Server.
- The test for the most security-sensitive piece of config was testing a hand-built object, not the binding.

## Where we got lucky

- The failure was closed (everything required auth) rather than open (nothing required auth). The same class of bug with the inverse default would have exposed every domain service.

## Actions

| Action | Owner | Status |
|---|---|---|
| Bind list-valued config through `@ConfigurationProperties` everywhere; grep for `@Value` on collection-typed fields | platform | done in #145 |
| Gateway test loads `jwt.public-paths` from a YAML document, not from an inline list | platform | done in #145 |
| E2E on the composed stack is part of the definition of done for any PR touching the gateway or Config Server (AGENTS.md) | platform | done |
| Alert on the failure mode: `GatewayHighErrorRate` would not have caught 401s (not 5xx); `GatewayTrafficCollapse` catches "nothing gets through" (`monitoring/alerts/platform.rules.yml`) | platform | done |
| Add a synthetic login check to the e2e smoke suite so a broken login pages within minutes | platform | open |

## Lessons for the runbooks

- [gateway-high-error-rate.md](../runbooks/gateway-high-error-rate.md): 401/403-only traffic is a config problem at the gateway (secret or public paths), not a downstream one.
- [gateway-traffic-collapse.md](../runbooks/gateway-traffic-collapse.md): "only 401s" is now an explicit branch.
