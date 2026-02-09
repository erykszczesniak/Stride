# ============================================
# Strava Clone - Makefile
# ============================================

.PHONY: help build up down logs clean test backend frontend

# Default target
help:
	@echo "Strava Clone - Available commands:"
	@echo ""
	@echo "  make build         - Build all Docker images"
	@echo "  make up            - Start all services"
	@echo "  make down          - Stop all services"
	@echo "  make logs          - View logs (all services)"
	@echo "  make logs-backend  - View backend logs"
	@echo "  make logs-frontend - View frontend logs"
	@echo "  make clean         - Remove containers, volumes, images"
	@echo "  make restart       - Restart all services"
	@echo "  make shell-backend - Open shell in backend container"
	@echo "  make shell-db      - Open psql shell"
	@echo "  make test          - Run backend tests"
	@echo ""
	@echo "  make dev-backend   - Run backend locally (no Docker)"
	@echo "  make dev-frontend  - Run frontend locally (no Docker)"
	@echo ""
	@echo "  make prod-build    - Build production images"
	@echo "  make prod-up       - Start production stack"
	@echo "  make prod-down     - Stop production stack"

# ============================================
# Development (Docker Compose)
# ============================================

build:
	docker-compose build

up:
	docker-compose up -d

down:
	docker-compose down

restart: down up

logs:
	docker-compose logs -f

logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

logs-db:
	docker-compose logs -f postgres

clean:
	docker-compose down -v --rmi local
	docker system prune -f

shell-backend:
	docker-compose exec backend sh

shell-db:
	docker-compose exec postgres psql -U strava -d strava

# ============================================
# Local Development (no Docker)
# ============================================

dev-backend:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

dev-frontend:
	cd frontend && npm start

test:
	./mvnw test

# ============================================
# Production
# ============================================

prod-build:
	docker-compose -f docker-compose.prod.yml build

prod-up:
	docker-compose -f docker-compose.prod.yml up -d

prod-down:
	docker-compose -f docker-compose.prod.yml down

prod-logs:
	docker-compose -f docker-compose.prod.yml logs -f

# ============================================
# Utilities
# ============================================

# Check container status
ps:
	docker-compose ps

# Check health endpoints
health:
	@echo "Backend health:"
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Backend not running"
	@echo "\nFrontend health:"
	@curl -s http://localhost:80/health || echo "Frontend not running"

# Database backup
db-backup:
	@mkdir -p backups
	docker-compose exec postgres pg_dump -U strava strava > backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Backup created in backups/"

# Database restore (usage: make db-restore FILE=backups/backup_xxx.sql)
db-restore:
	docker-compose exec -T postgres psql -U strava strava < $(FILE)
