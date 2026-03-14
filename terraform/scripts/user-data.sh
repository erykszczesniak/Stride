#!/bin/bash
set -euo pipefail

# Log all output
exec > >(tee /var/log/user-data.log) 2>&1
echo "=== User data script started at $(date) ==="

# Install Docker
dnf update -y
dnf install -y docker
systemctl enable docker
systemctl start docker

# Install Docker Compose plugin
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# ECR login
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin $(echo "${ecr_backend_url}" | cut -d'/' -f1)

# Create app directory
mkdir -p /opt/strava
cd /opt/strava

# Write docker-compose file
cat > docker-compose.yml <<'COMPOSE'
version: '3.8'

services:
  backend:
    image: ${ecr_backend_url}:${backend_image_tag}
    container_name: strava-backend
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://${db_host}:${db_port}/${db_name}
      DATABASE_USERNAME: ${db_username}
      DATABASE_PASSWORD: ${db_password}
      DATABASE_DRIVER: org.postgresql.Driver
      HIBERNATE_DIALECT: org.hibernate.dialect.PostgreSQLDialect
      HIBERNATE_DDL_AUTO: validate
      FLYWAY_ENABLED: "true"
      JWT_SECRET: ${jwt_secret}
      CORS_ORIGINS: ${cors_origins}
      GROQ_API_KEY: ${groq_api_key}
      GOOGLE_CLIENT_ID: ${google_client_id}
      GOOGLE_CLIENT_SECRET: ${google_client_secret}
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 3
    networks:
      - strava-network
    deploy:
      replicas: 1
      resources:
        limits:
          cpus: '1'
          memory: 512M

  frontend:
    image: ${ecr_frontend_url}:${frontend_image_tag}
    container_name: strava-frontend
    restart: always
    ports:
      - "80:80"
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - strava-network
    deploy:
      replicas: 1
      resources:
        limits:
          cpus: '0.5'
          memory: 128M

networks:
  strava-network:
    driver: bridge
COMPOSE

# Start containers
docker compose up -d

echo "=== User data script completed at $(date) ==="
