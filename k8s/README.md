# Kubernetes deployment

Production-style manifests for the Stride microservices stack.

## Layout

```
k8s/
├── 00-namespace.yaml          # stride namespace
├── 01-secrets-example.yaml    # copy → 01-secrets.yaml + fill in real values
├── infra/                     # gateway, eureka, config-server
├── services/                  # 7 domain services + 1 HPA on activity-service
├── ingress.yaml               # single ingress → api-gateway
└── network-policies.yaml      # defense-in-depth (no direct hits to services)
```

## What's deliberately NOT in this directory

- **Kafka, Redis, Postgres** — in prod these come from managed services
  (Amazon MSK or Confluent Cloud for Kafka, ElastiCache for Redis, RDS for
  Postgres) or from off-the-shelf Helm charts (`bitnami/kafka`,
  `bitnami/redis`, `bitnami/postgresql`). Authoring those StatefulSets by
  hand for a portfolio repo would be code with no architectural insight.
- **Prometheus, Grafana, Jaeger** — same reasoning, use the
  `prometheus-community/kube-prometheus-stack` + `jaegertracing/jaeger`
  charts. The `monitoring/` directory in the repo root has the application-
  level scrape config and dashboard provisioning these charts consume.

## Run locally on kind

```bash
# 1. Spin up the kind cluster.
kind create cluster --name stride

# 2. Pre-pull images into the cluster (built locally by ./mvnw + docker compose build).
for img in config-server discovery-server api-gateway \
           user-service activity-service notification-service \
           social-service club-event-service challenge-goal-service ai-service; do
  kind load docker-image "stride/$img:dev" --name stride
done

# 3. Install Kafka / Redis / Postgres / Prometheus via Helm (off-the-shelf).
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kafka bitnami/kafka \
  --namespace stride --create-namespace \
  --set listeners.client.protocol=PLAINTEXT --set sasl.enabled=false
helm install redis bitnami/redis \
  --namespace stride --set auth.enabled=false
# (postgres-per-service via 7× helm install bitnami/postgresql, names matching k8s/services/*)

# 4. Apply Stride manifests.
cp k8s/01-secrets-example.yaml k8s/01-secrets.yaml   # gitignored
# edit k8s/01-secrets.yaml with real values
kubectl apply -f k8s/

# 5. /etc/hosts → "127.0.0.1 stride.local"
# Visit http://stride.local

# 6. Smoke test through the ingress + port-forwarded monitoring
#    (kubectl -n monitoring port-forward svc/prometheus 9090 & svc/alertmanager 9093)
BASE_URL=http://stride.local EUREKA_URL=http://stride.local:8761 scripts/smoke.sh
```

## Resource budget (per-pod requests)

| Pod                       | mem req | cpu req | Notes                                      |
|---------------------------|---------|---------|--------------------------------------------|
| config-server             |  384Mi  |   50m   | low traffic, tiny working set              |
| discovery-server          |  512Mi  |  100m   | Eureka registry — modest                   |
| api-gateway × 2 replicas  |  512Mi  |  100m   | reactive WebFlux, single ingress           |
| user-service              |  512Mi  |  100m   |                                            |
| activity-service          |  512Mi  |  100m   | HPA 1→4 on 70% CPU                         |
| notification-service      |  512Mi  |  100m   | Kafka consumer + SSE fanout                |
| social-service            |  512Mi  |  100m   | CQRS read model + 4 consumers              |
| club-event-service        |  512Mi  |  100m   |                                            |
| challenge-goal-service    |  512Mi  |  100m   | Resilience4j + Redis cache                 |
| ai-service                |  384Mi  |   50m   | stateless proxy                            |

Total requests (without HPA scale-out): ~4.8 GiB / 950m CPU. Comfortably
fits in a single Oracle Cloud Always Free instance (4 ARM CPUs, 24 GiB
RAM) or a 2-node t3.medium EKS cluster.

## Network model

```
Internet
   │
   ▼
[ Ingress (nginx) ]
   │
   ▼
[ api-gateway:8080 ]──validate JWT, strip X-User-* spoofing, propagate identity
   │
   ▼  (lb:// resolved via Eureka)
[ user/activity/notification/social/club-event/challenge-goal/ai ]
   │
   ▼
[ Kafka · Redis · Postgres × N · Jaeger · Prometheus ]
```

Domain services are blocked from external traffic by the default-deny
NetworkPolicy plus the explicit "only gateway + infra + Prometheus can
hit role=domain pods" rule.
