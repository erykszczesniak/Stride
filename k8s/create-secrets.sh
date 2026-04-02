#!/usr/bin/env bash
# =============================================================================
# Create Kubernetes secrets from environment variables.
# Usage:
#   export JWT_SECRET="..."
#   export DATABASE_PASSWORD="..."
#   export GOOGLE_CLIENT_ID="..."
#   export GOOGLE_CLIENT_SECRET="..."
#   export GROQ_API_KEY="..."           # optional
#   ./k8s/create-secrets.sh
# =============================================================================
set -euo pipefail

NAMESPACE="strava"

required_vars=(JWT_SECRET DATABASE_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET)
for var in "${required_vars[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: $var is not set" >&2
    exit 1
  fi
done

echo "Creating namespace ${NAMESPACE} (if not exists)..."
kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

echo "Creating backend-secret..."
kubectl create secret generic backend-secret \
  --namespace="${NAMESPACE}" \
  --from-literal=JWT_SECRET="${JWT_SECRET}" \
  --from-literal=DATABASE_PASSWORD="${DATABASE_PASSWORD}" \
  --from-literal=GROQ_API_KEY="${GROQ_API_KEY:-}" \
  --from-literal=GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
  --from-literal=GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Creating postgres-secret..."
kubectl create secret generic postgres-secret \
  --namespace="${NAMESPACE}" \
  --from-literal=POSTGRES_DB=strava \
  --from-literal=POSTGRES_USER=strava \
  --from-literal=POSTGRES_PASSWORD="${DATABASE_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secrets created successfully in namespace ${NAMESPACE}."
