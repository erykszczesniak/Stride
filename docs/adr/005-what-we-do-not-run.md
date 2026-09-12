# ADR-005 — No service mesh, no full event sourcing, no EKS

| | |
|---|---|
| **Status** | Accepted |
| **Date** | 2026-08 (Phase 22), recorded 2026-09-12 |
| **Deciders** | platform |

## Context

A twelve-service portfolio system invites every piece of cloud-native
tooling. Each one has a cost in setup, cognitive load and money, and most
of them would demonstrate nothing this codebase does not already show.

## Decision

- **No service mesh (Istio/Linkerd).** Discovery and load balancing are
  Spring Cloud LoadBalancer + Eureka; retries, breakers and deadlines are
  Resilience4j in code; isolation is a `NetworkPolicy`. A mesh would add
  sidecars to 12 pods to duplicate what the application already does.
- **No full event sourcing.** Only the social feed is materialised from
  events (CQRS read model). Everything else is conventional CRUD with
  Flyway-managed schemas; the write model is the source of truth.
- **No EKS in Terraform.** The control plane alone (~$73/month) breaks the
  free-tier frame of `terraform/aws`. Kubernetes is exercised on kind with
  production-shaped manifests; the AWS slice proves real-cloud provisioning
  (RDS, S3, ECR, EC2, Parameter Store) without a standing bill.

## Alternatives considered

Each of the three was prototyped or costed and reversed for the reasons
above; the README section "What's intentionally not here" is the public
statement of this record.

## Consequences

- Resilience behaviour lives in code and config, visible in tests and in
  `resilience4j_*` metrics rather than in mesh telemetry.
- Cross-service history questions ("what did the activity look like last
  week") cannot be answered from an event store.
- Anyone reproducing the k8s deployment needs kind or their own cluster.

## Known gaps

- Without mTLS, in-cluster traffic is plaintext; acceptable for a demo
  cluster, a mesh or cert-manager-issued certificates would be the first
  step toward a production posture.
