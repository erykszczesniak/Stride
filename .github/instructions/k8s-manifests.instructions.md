---
applyTo: "k8s/**"
---

# Kubernetes manifests

- One directory per workload (`k8s/backend`, `k8s/frontend`, `k8s/postgres`), one file per object (`deployment.yaml`, `service.yaml`, `hpa.yaml`, `configmap.yaml`, `secret.yaml`). Namespace is always `strava` (`k8s/namespace.yaml`).
- Every container has: readiness + liveness probes on `/actuator/health/{readiness,liveness}` (backend), resource requests **and** limits, `imagePullPolicy: IfNotPresent`.
- Only the Ingress (`k8s/ingress.yaml`) is reachable from outside; the backend Service stays `ClusterIP`.
- Secrets only via `secretKeyRef` from `backend-secret` / the postgres secret (`k8s/create-secrets.sh` creates them; committed `secret.yaml` files hold placeholders). No literals in `env`. A new provider key (for example `ANTHROPIC_API_KEY`) goes into the secret **and** into `create-secrets.sh`.
- Memory limits mirror `docker-compose.yml` so heap sizing via `MaxRAMPercentage=75` behaves the same in both runtimes; a change to one is a change to both.
- Validate before pushing: `kubectl apply --dry-run=client -f k8s/ -R` and, if a cluster is up, `kubectl rollout status deploy/backend -n strava`.
