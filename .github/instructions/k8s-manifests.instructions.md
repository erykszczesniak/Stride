---
applyTo: "k8s/**"
---

# Kubernetes manifests

- One file per workload under `k8s/services/` or `k8s/infra/`, containing the Deployment and its Service (and HPA if any). Namespace is always `stride`.
- Every container has: readiness + liveness probes on `/actuator/health/{readiness,liveness}`, resource requests **and** limits, `imagePullPolicy: IfNotPresent` (images are side-loaded into kind).
- Domain services are never exposed by an Ingress. External traffic enters only via `api-gateway`; `network-policies.yaml` enforces it. If you add a service, add it to the `domain-services-from-gateway-and-infra` policy selector.
- gRPC ports (`908x`) are cluster-internal: declare them on the Service but never on the Ingress.
- Secrets only from `stride-secrets` (`01-secrets-example.yaml` documents keys; the real file is gitignored). No literals in `env`.
- Memory limits mirror `docker-compose.yml` (`mem_limit`) so heap sizing via `MaxRAMPercentage=75` behaves the same in both runtimes.
- Validate before pushing: `kubectl apply --dry-run=client -f k8s/` and, if kind is up, `kubectl rollout status deploy/<name> -n stride`.
