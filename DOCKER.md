# Docker Commands Reference

Quick reference for running and managing the Strava Clone application with Docker.

## Quick Start

```bash
# Build and start everything
make build && make up

# Or using docker-compose directly
docker-compose build
docker-compose up -d
```

**Access points:**
- Frontend: http://localhost
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

---

## Make Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make build` | Build all Docker images |
| `make up` | Start all services |
| `make down` | Stop all services |
| `make restart` | Restart all services |
| `make logs` | View logs (all services) |
| `make logs-backend` | View backend logs only |
| `make logs-frontend` | View frontend logs only |
| `make logs-db` | View database logs |
| `make ps` | Show container status |
| `make health` | Check health endpoints |
| `make clean` | Remove containers, volumes, images |

---

## Docker Compose Commands

### Starting Services

```bash
# Start all services in background
docker-compose up -d

# Start with rebuild
docker-compose up -d --build

# Start specific service
docker-compose up -d backend

# Force recreate containers
docker-compose up -d --force-recreate
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears database!)
docker-compose down -v

# Stop specific service
docker-compose stop backend
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
# Last 100 lines
docker-compose logs --tail=100 backend

# Since timestamp
docker-compose logs --since="2024-01-01T00:00:00" backend
```

### Building Images

```bash
# Build all
docker-compose build

# Build specific service
docker-compose build backend

# Build without cache
docker-compose build --no-cache

# Build with progress output
docker-compose build --progress=plain
```

---

## Container Management

### Status & Info

```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# Container details
docker inspect strava-backend

# Resource usage
docker stats
```

### Shell Access

```bash
# Backend container
docker exec -it strava-backend sh

# PostgreSQL (psql)
docker exec -it strava-postgres psql -U strava -d strava

# Frontend container
docker exec -it strava-frontend sh
```

### Container Operations

```bash
# Restart container
docker restart strava-backend

# Stop container
docker stop strava-backend

# Start stopped container
docker start strava-backend

# Remove container
docker rm strava-backend

# View container logs
docker logs strava-backend
docker logs -f strava-backend  # follow mode
```

---

## Database Operations

### PostgreSQL

```bash
# Connect to database
docker exec -it strava-postgres psql -U strava -d strava

# Run SQL query
docker exec strava-postgres psql -U strava -d strava -c "SELECT * FROM users;"

# Backup database
docker exec strava-postgres pg_dump -U strava strava > backup.sql

# Restore database
docker exec -i strava-postgres psql -U strava strava < backup.sql

# List tables
docker exec strava-postgres psql -U strava -d strava -c "\dt"

# Describe table
docker exec strava-postgres psql -U strava -d strava -c "\d users"
```

---

## Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Backend health (detailed)
curl http://localhost:8080/actuator/health | jq

# Frontend health
curl http://localhost:80/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# All actuator endpoints
curl http://localhost:8080/actuator
```

---

## Cleanup Commands

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune

# Nuclear option - remove everything
docker system prune -a --volumes

# Remove project containers, volumes, and images
docker-compose down -v --rmi local
```

---

## Troubleshooting

### Container won't start

```bash
# Check logs for errors
docker logs strava-backend

# Check if ports are in use
lsof -i :8080
lsof -i :80
lsof -i :5432

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### Database connection issues

```bash
# Check if postgres is healthy
docker exec strava-postgres pg_isready -U strava

# Check database exists
docker exec strava-postgres psql -U strava -l

# Check connection from backend
docker exec strava-backend wget -qO- http://postgres:5432 || echo "Cannot connect"
```

### Out of disk space

```bash
# Check Docker disk usage
docker system df

# Clean up everything
docker system prune -a --volumes
```

### Slow builds

```bash
# Use BuildKit for faster builds
DOCKER_BUILDKIT=1 docker-compose build

# Check .dockerignore is working
docker build --no-cache . 2>&1 | head -50
```

---

## Environment Variables

### Backend (docker-compose.yml)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | dev | Spring profile |
| `DATABASE_URL` | jdbc:postgresql://postgres:5432/strava | DB connection |
| `DATABASE_USERNAME` | strava | DB user |
| `DATABASE_PASSWORD` | strava123 | DB password |
| `JWT_SECRET` | (generated) | JWT signing key |
| `CORS_ORIGINS` | http://localhost:3000,http://localhost | Allowed origins |

### Override with .env file

Create `.env` file in project root:

```env
POSTGRES_PASSWORD=my-secure-password
JWT_SECRET=my-super-secret-key
GROQ_API_KEY=your-api-key
```

---

## Production

```bash
# Build production images
make prod-build

# Start production stack
make prod-up

# View production logs
make prod-logs

# Stop production stack
make prod-down
```

---

## Useful Aliases

Add to your `~/.bashrc` or `~/.zshrc`:

```bash
alias dc="docker-compose"
alias dcu="docker-compose up -d"
alias dcd="docker-compose down"
alias dcl="docker-compose logs -f"
alias dcp="docker-compose ps"
alias dcb="docker-compose build"
alias dcr="docker-compose restart"

# Strava specific
alias strava-up="cd ~/Desktop/strava && make up"
alias strava-down="cd ~/Desktop/strava && make down"
alias strava-logs="cd ~/Desktop/strava && make logs"
```

---

## Sample Users

After starting the application, these test accounts are available:

| Email | Password | Role |
|-------|----------|------|
| pan@example.com | password123 | USER |
| john@example.com | password123 | USER |
| jane@example.com | password123 | USER |
| admin@example.com | password123 | ADMIN |
