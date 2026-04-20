# STRIDE - Fitness Activity Tracker

A full-stack fitness activity tracking application inspired by Strava. Track your workouts, join challenges, connect with other athletes, and achieve your fitness goals.

<img width="1985" height="1239" alt="image" src="https://github.com/user-attachments/assets/eb45b949-40b1-4a08-b218-1cc0d3e0896c" />



> **Note:** This project is currently in active development. Features and UI may change.

![Dashboard Preview](docs/screenshots/dashboard.png)

## Features

### Activity Tracking
- Record and track various activity types (Running, Cycling, Swimming, Hiking, Workouts)
- View detailed activity statistics (distance, duration, pace, calories, elevation)
- **Pace display for running** — RUN/WALK/HIKE activities show pace (min/km) instead of speed (km/h), matching how runners think about their stats
- **Hours/minutes/seconds duration input** — record an activity down to the second (a 24:30 run is no longer rounded to whole minutes)
- Upload up to 5 photos per activity (gallery view, card cover)
- Like and comment on activities
- Add reactions to comments

### Activity File Import
Import activities directly from any GPS watch or fitness platform that exports **GPX**, **TCX**, or **FIT** files (Garmin, Polar, Suunto, Decathlon Kiprun, Wahoo, etc.).

- **GPX parser** — XML format, extracts GPS track, elevation, heart rate, cadence (via Garmin TrackPointExtension)
- **TCX parser** — XML format, extracts multi-lap data, heart rate, cadence, power, `<Notes>` as activity description
- **FIT parser** — Garmin/ANT+ binary format, hand-rolled parser with little-endian handling, developer fields, compressed timestamps, and FIT "invalid" sentinel filtering
- **Drag-and-drop upload** on the Record Activity page with a Manual Entry / Upload File mode toggle
- **Bundled sample files** — real-world GPX/TCX/FIT examples (5K run in Warsaw, 20K cycling in Amsterdam, Tatras hike, interval training, Lake Zegrze cycling loop, Night Run captured by a Decathlon Kiprun GPS 900) that you can download and re-import to test the feature
- Automatic title synthesis for FIT files from sport + time-of-day ("Morning Run", "Evening Ride")
- GPS tracks stored as compact Google Encoded Polyline (~10× smaller than JSON)

### Personal Records
Automatic detection and tracking of personal best times for standard distances across each sport.

- **Standard distances:** 1 km, 5 km, 10 km, Half Marathon (21.0975 km), Marathon (42.195 km)
- **Sliding-window GPS analysis** — when the activity has a GPS track, finds the fastest sub-segment of each standard distance. The best 5 km within a 10 km run still counts as a 5 km PR.
- **Pace estimation fallback** — for activities without GPS, time is scaled proportionally from the overall pace
- Detection runs automatically on every activity create/update — no manual trigger
- **Recalculate** button on the profile rebuilds every PR from the full activity history (useful after bulk-importing GPX files)
- Displayed as a new **Personal Records** tab on the profile, grouped by sport, showing distance, best time, pace, improvement vs. previous PR, and a link to the activity where the PR was set

### Social Features
- Follow other athletes
- Public user profiles with stats, recent activities, and follow button
- Activity feed showing followed users' activities
- Clickable usernames/avatars → navigate to user profile
- Clubs - join communities based on sport types
- Events - create and join local fitness events

### Challenges
- Join monthly challenges
- Track progress against goals
- Leaderboard rankings
- Earn badges for completing challenges

### Goals & Progress
- Set personal fitness goals
- Track weekly and monthly progress
- Visual progress charts and statistics

### Notifications
- Real-time toast notifications via SSE (Server-Sent Events)
- Email notifications (likes, comments, follows, achievements)
- Weekly training summary emails
- Configurable per-user: toast notifications, email notifications, weekly summary
- Duplicate notification prevention (e.g. re-liking same activity)

### Report Builder
- Dynamic report generation via GraphQL query
- Drag-and-drop column reorder
- Sortable table columns (asc/desc)
- Activity type filter
- Date range picker with quick presets (7 days, 30 days, this month, last month, this year)
- Save/load report templates (localStorage)
- Export to CSV (backend) and PDF (frontend, branded)
- Summary row with totals and averages

### Authentication
- Email/password login with JWT tokens
- Google OAuth2 login ("Continue with Google")
- Auto-creates user on first Google login

### User Experience
- Dark/Light theme toggle
- Responsive design
- User profile customization

## Screenshots

<details>
<summary>Click to expand all screenshots</summary>

### Authentication

#### Login
![Login](docs/screenshots/login.png)
*Clean login page with email and password fields, password visibility toggle*

#### Register
![Register](docs/screenshots/register.png)
*Registration form with password strength indicator*

---

### Main Application

#### Dashboard
![Dashboard](docs/screenshots/dashboard.png)
*Main dashboard with stats cards, weekly chart, recent activities, and goals progress*

#### Dashboard - User Menu
![Dashboard User Menu](docs/screenshots/dashboard-user-menu.png)
*User avatar dropdown with profile link, settings, and logout*

---

### Activities

#### Activities Feed
![Activities](docs/screenshots/activities.png)
*Activity feed with filters (All/Following/My Activities), type filtering, and sidebar*

#### Record Activity
![Record Activity](docs/screenshots/record-activity.png)
*Create new activity - select type (Run, Ride, Swim, Hike, Walk, Workout) and enter stats*

#### Activity Details
![Activity Details](docs/screenshots/activity-details.png)
*Full activity view with stats, map, comments section, and like/share buttons*

---

### Profile

#### User Profile
![Profile](docs/screenshots/profile.png)
*User profile with cover photo, avatar, stats (followers, following, activities)*

#### Profile - Stats Tab
![Profile Stats](docs/screenshots/profile-stats.png)
*Detailed statistics with charts and visualizations*

---

### Goals & Challenges

#### Goals
![Goals](docs/screenshots/goals.png)
*Personal fitness goals with progress bars and deadlines*

#### Challenges
![Challenges](docs/screenshots/challenges.png)
*Active challenges with progress tracking and participant counts*

---

### Social Features

#### Clubs
![Clubs](docs/screenshots/clubs.png)
*Sport communities - browse, search, and join clubs*

#### Events
![Events](docs/screenshots/events.png)
*Local fitness events with date, location, and registration*

#### Create Event
![Create Event](docs/screenshots/create-event.png)
*Event creation form with date picker, location, and details*

---

### Training

#### Training Plans
![Training Plans](docs/screenshots/training-plans.png)
*AI-generated training plans with workouts schedule*

---

### Settings

#### Settings - Profile
![Settings Profile](docs/screenshots/settings-profile.png)
*Edit profile information - name, bio, location, weight, height*

#### Settings - Notifications
![Settings Notifications](docs/screenshots/settings-notifications.png)
*Notification settings - email, toast notifications, weekly summary*

#### Settings - Preferences
![Settings Preferences](docs/screenshots/settings-preferences.png)
*App preferences - units, activity type, pace display, language, theme*

---

### Admin Panel

#### Admin - Overview
![Admin Overview](docs/screenshots/admin-overview.png)
*Admin dashboard with user and activity statistics*

#### Admin - Users
![Admin Users](docs/screenshots/admin-users.png)
*User management table with search and actions*

#### Admin - Export
![Admin Export](docs/screenshots/admin-export.png)
*Export data to Excel/PDF*

---

### Theme Variants

#### Light Mode
![Light Mode](docs/screenshots/light-mode.png)
*Default light theme*

#### Dark Mode
![Dark Mode](docs/screenshots/dark-mode.png)
*Full dark mode support across all pages*

---

### Responsive Design

#### Mobile View
![Mobile View](docs/screenshots/mobile-view.png)
*Responsive layout optimized for mobile devices*

</details>

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.4**
- **Spring Security** with JWT authentication + **Google OAuth2**
- **Spring Data JPA** with PostgreSQL
- **GraphQL** API (queries for report builder)
- **REST API** for all CRUD operations
- **SSE** (Server-Sent Events) for real-time notifications
- **Async event-driven** architecture (Spring ApplicationEventPublisher)
- **Elasticsearch 8** for full-text user search (synced via async event listener)
- **Flyway** for database migrations
- **Hand-rolled GPX/TCX/FIT parsers** for activity file import (no external SDK dependencies)
- **Thymeleaf** email templates
- **Swagger/OpenAPI** documentation
- **Testcontainers** for integration testing

### Frontend
- **React 18** with Hooks
- **React Router** for navigation
- **Styled Components** for styling
- **Recharts** for data visualization
- **Leaflet** + **React-Leaflet** for interactive route maps (decodes both legacy JSON and Google Encoded Polyline formats)
- **Axios** for API communication
- **jsPDF** for PDF report generation
- **Jest** + **React Testing Library** for unit/component tests
- **Playwright** for E2E testing
- **React Icons** for iconography

### Infrastructure & Observability
- **Docker** & **Docker Compose**
- **Terraform** for AWS infrastructure (EC2, RDS, ALB, ECR, VPC)
- **Kubernetes** manifests (Deployments, Services, Ingress, Secrets, ConfigMaps)
- **PostgreSQL 16**
- **Nginx** (production)
- **Prometheus** + **Grafana** for metrics & dashboards
- **OpenTelemetry** distributed tracing exported via OTLP to **Jaeger**
- **Structured logging** (JSON in prod via Logstash encoder) with `traceId` correlation
- **Rate limiting** on auth endpoints

## Getting Started

### Quick Start with Docker (Recommended)

The easiest way to run the application is with Docker Compose.

#### Prerequisites
- Docker & Docker Compose
- Make (optional, for convenience commands)

#### Run the Application

```bash
# Clone the repository
git clone https://github.com/erykszczesniak/strava.git
cd strava

# Start all services (backend, frontend, PostgreSQL)
make up

# Or without make:
docker compose up -d --build
```

That's it! The application will be available at:
- **Frontend:** http://localhost (port 80)
- **Backend API:** http://localhost:8081
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **GraphQL:** http://localhost:8081/graphql
- **Grafana:** http://localhost:3003 (admin/admin)
- **Prometheus:** http://localhost:9091
- **Elasticsearch:** http://localhost:9200
- **Jaeger (Tracing):** http://localhost:16686

#### Sample Users

| Email | Password | Role |
|-------|----------|------|
| pan@example.com | password123 | USER |
| john@example.com | password123 | USER |
| jane@example.com | password123 | USER |
| admin@example.com | password123 | ADMIN |

#### Useful Commands

```bash
# View logs
make logs

# Stop all services
make down

# Restart backend only
docker compose restart backend

# Access database
docker compose exec db psql -U strava -d stravadb

```

> For more Docker commands, see [DOCKER.md](DOCKER.md)

---

### Manual Setup (Development)

If you prefer to run services manually without Docker.

#### Prerequisites
- Java 21+
- Node.js 18+
- Maven 3.9+

#### Backend

```bash
# Clone and enter directory
git clone https://github.com/erykszczesniak/strava.git
cd strava

# Run with H2 in-memory database (default for dev)
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

#### Frontend

```bash
cd frontend
npm install
npm start
```

The app will be available at `http://localhost:3000`

#### Email Notifications (Optional)

To enable email notifications locally, create `src/main/resources/application-local.yml`:

```yaml
spring:
  mail:
    username: your.email@gmail.com
    password: xxxx-xxxx-xxxx-xxxx   # Gmail App Password
```

Then run the backend with the `local` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Gmail setup:**
1. Enable 2FA on your Google account
2. Go to https://myaccount.google.com/apppasswords
3. Generate an App Password for "Mail"
4. Paste it in `application-local.yml`

> **Note:** `application-local.yml` is in `.gitignore` and will not be committed.

#### Testing Weekly Summary Email

The weekly summary runs automatically every Monday at 9:00 AM. To trigger it manually for testing, log in as admin and run:

```bash
curl -X POST http://localhost:8080/api/v1/notifications/weekly-summary/trigger \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

Get the admin token by logging in as `admin@example.com` / `password123` and copying `localStorage.token` from DevTools.

---

### Kubernetes Deployment

Kubernetes manifests are provided in the `k8s/` directory for deploying to any K8s cluster (minikube, kind, EKS, GKE, etc.).

#### Prerequisites
- A running Kubernetes cluster
- `kubectl` configured and connected
- Nginx Ingress Controller installed
- Container images pushed to a registry (default: `ghcr.io/erykszczesniak/strava-*`)

#### Deploy

```bash
# 1. Create the namespace
kubectl apply -f k8s/namespace.yaml

# 2. Deploy all resources
kubectl apply -f k8s/ --recursive

# 3. Verify everything is running
kubectl get all -n strava
```

#### Quick Start with minikube

```bash
# Start minikube
minikube start

# Enable Ingress addon
minikube addons enable ingress

# Build images inside minikube (no registry needed)
eval $(minikube docker-env)
docker build -t ghcr.io/erykszczesniak/strava-backend:latest .
docker build -t ghcr.io/erykszczesniak/strava-frontend:latest ./frontend

# Deploy
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/ --recursive

# Add host entry
echo "$(minikube ip) strava.local" | sudo tee -a /etc/hosts

# Open in browser
open http://strava.local
```

#### Update Secrets

Secrets use base64-encoded placeholder values. Replace them before deploying to a real environment:

```bash
# Generate a base64-encoded value
echo -n 'your-real-password' | base64

# Edit the secret files, then apply
kubectl apply -f k8s/postgres/secret.yaml
kubectl apply -f k8s/backend/secret.yaml
```

#### Manifest Structure

```
k8s/
├── namespace.yaml          # strava namespace
├── ingress.yaml            # Nginx Ingress (strava.local)
├── postgres/
│   ├── secret.yaml         # DB credentials
│   ├── configmap.yaml      # PGDATA path
│   ├── pvc.yaml            # 1Gi persistent volume
│   ├── deployment.yaml     # Single replica, liveness/readiness probes
│   └── service.yaml        # ClusterIP :5432
├── backend/
│   ├── secret.yaml         # JWT secret, DB password, API keys
│   ├── configmap.yaml      # Spring config, CORS, Flyway
│   ├── deployment.yaml     # 2 replicas, startup/liveness/readiness probes
│   └── service.yaml        # ClusterIP :8080
└── frontend/
    ├── configmap.yaml       # Nginx config reference
    ├── deployment.yaml      # 2 replicas, liveness/readiness probes
    └── service.yaml         # ClusterIP :80
```

#### Useful Commands

```bash
# Check pod status
kubectl get pods -n strava

# View logs for a pod
kubectl logs -f deployment/backend -n strava

# Port-forward backend for local debugging
kubectl port-forward svc/backend 8080:8080 -n strava

# Scale backend replicas
kubectl scale deployment backend --replicas=3 -n strava

# Delete everything
kubectl delete namespace strava
```

## AWS Deployment (Terraform)

Deploy to AWS Free Tier using Terraform. Architecture: ALB → EC2 (Docker) → RDS PostgreSQL.

> **Note:** Currently `docker-compose.prod.yml` includes a local PostgreSQL service. Once AWS RDS is configured via Terraform, the local `postgres` service should be removed and the backend should point to the RDS endpoint instead.

### Prerequisites
- AWS CLI configured (`aws configure`)
- Terraform >= 1.5
- Docker (for building images)
- An EC2 key pair created in your AWS region

### Deploy

```bash
cd terraform

# Copy and edit variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values (db_password, jwt_secret, etc.)

# Initialize and apply
terraform init
terraform apply
```

### Push Docker Images

After `terraform apply`, push your images to ECR:

```bash
# Login to ECR
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $(terraform output -raw ecr_backend_url | cut -d'/' -f1)

# Build and push backend
docker build -t $(terraform output -raw ecr_backend_url):latest ..
docker push $(terraform output -raw ecr_backend_url):latest

# Build and push frontend
docker build -t $(terraform output -raw ecr_frontend_url):latest ../frontend
docker push $(terraform output -raw ecr_frontend_url):latest
```

### Access the App

```bash
# Get the ALB URL
curl $(terraform output -raw alb_dns)
```

### Infrastructure Overview

| Resource | Type | Free Tier |
|----------|------|-----------|
| EC2 | t3.micro | 750 hrs/month |
| RDS | db.t3.micro PostgreSQL | 750 hrs/month |
| ALB | Application LB | 750 hrs/month |
| ECR | Container registry | 500 MB storage |

### Tear Down

```bash
terraform destroy
```

---

## API Documentation

Once the backend is running, API documentation is available at:
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **GraphQL Playground:** http://localhost:8081/graphql

### REST API Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/register` | User registration |
| `POST /api/v1/auth/login` | User login |
| `GET /api/v1/activities` | Get activities |
| `POST /api/v1/activities` | Create activity |
| `GET /api/v1/challenges` | Get challenges |
| `GET /api/v1/clubs` | Get clubs |
| `GET /api/v1/events` | Get events |
| `GET /api/v1/users/profile` | Get user profile |
| `GET /api/v1/notifications` | Get user notifications |
| `GET /api/v1/notifications/unread-count` | Get unread count |
| `PUT /api/v1/notifications/{id}/read` | Mark as read |
| `PUT /api/v1/notifications/read-all` | Mark all as read |
| `GET /api/v1/notifications/stream` | SSE stream (real-time) |
| `POST /api/v1/auth/google` | Google OAuth2 login |
| `POST /api/v1/activities/{id}/photos` | Upload activity photos |
| `DELETE /api/v1/activities/{id}/photos/{index}` | Delete activity photo |
| `POST /api/v1/import/activities` | Import GPX/TCX/FIT file and create activity |
| `GET /api/v1/import/samples` | List bundled sample files available for download |
| `GET /api/v1/import/samples/{filename}` | Download a bundled sample file |
| `GET /api/v1/personal-records` | Current user's personal records |
| `GET /api/v1/personal-records/user/{userId}` | Another user's personal records |
| `GET /api/v1/personal-records/activity-type/{type}` | Filter PRs by sport |
| `POST /api/v1/personal-records/recalculate` | Rebuild all PRs from activity history |
| `GET /api/v1/users/{id}/profile` | Public user profile |
| `POST /api/v1/export/report/csv` | Export report as CSV |

### GraphQL API

| Query/Mutation | Description |
|----------------|-------------|
| `generateReport(dateFrom, dateTo, fields, activityType)` | Dynamic report with selected fields |
| `dashboard` | Dashboard stats, weekly chart, recent activities |
| `activities(userId, page, size)` | Paginated activities |
| `createActivity(input)` | Create new activity |
| `likeActivity(id)` / `unlikeActivity(id)` | Like/unlike |
| `followUser(userId)` / `unfollowUser(userId)` | Follow/unfollow |

## Project Structure

```
strava/
├── docker-compose.yml     # Docker services configuration
├── Dockerfile             # Backend Docker image
├── Makefile               # Convenience commands
├── DOCKER.md              # Docker commands reference
├── terraform/             # AWS infrastructure (Terraform)
├── k8s/                   # Kubernetes manifests
│   ├── namespace.yaml
│   ├── ingress.yaml
│   ├── postgres/          # DB deployment, service, PVC, secrets
│   ├── backend/           # API deployment, service, configmap, secrets
│   └── frontend/          # Web deployment, service, configmap
├── monitoring/
│   ├── prometheus/        # Prometheus scrape config
│   └── grafana/           # Grafana dashboards & provisioning
├── src/main/java/com/strava/backend/
│   ├── configuration/     # Spring configurations
│   ├── controller/        # REST controllers
│   ├── domain/            # Entity models
│   ├── dto/               # Data transfer objects
│   ├── graphql/           # GraphQL resolvers
│   ├── event/             # Spring events (async notifications)
│   ├── repository/        # JPA repositories
│   ├── security/          # JWT & auth
│   └── service/           # Business logic
├── frontend/
│   ├── Dockerfile         # Frontend Docker image
│   ├── nginx.conf         # Nginx configuration
│   ├── src/
│   │   ├── api/           # API services
│   │   ├── components/    # Reusable components
│   │   ├── context/       # React contexts
│   │   ├── pages/         # Page components
│   │   └── styles/        # Theme & global styles
│   └── public/
└── docs/                   # Documentation & screenshots
```

## Observability & Security

### Monitoring (Prometheus + Grafana)

Prometheus scrapes backend metrics every 15s. Grafana comes with a pre-built **JVM & Spring Boot** dashboard.

```bash
# Start the full stack (includes Prometheus + Grafana)
docker compose up -d --build
```

**Prometheus** — http://localhost:9091

Check targets status at [/targets](http://localhost:9091/targets) — backend should show as `UP`.

Example queries to try in [/graph](http://localhost:9091/graph):

```promql
# HTTP request rate per endpoint
rate(http_server_requests_seconds_count[1m])

# Response time (p95)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap memory usage
jvm_memory_used_bytes{area="heap"}

# CPU usage
process_cpu_usage

# HikariCP active connections
hikaricp_connections_active

# GC pause duration
rate(jvm_gc_pause_seconds_sum[1m])
```

**Grafana** — http://localhost:3003 (login: `admin` / `admin`)

Go to **Dashboards → Strava Backend - JVM & Spring Boot** to see:
- HTTP request rate, p95 latency, error rates (4xx/5xx)
- JVM heap & non-heap memory
- GC pause duration
- Thread counts (live, daemon, peak)
- System & process CPU usage
- HikariCP connection pool (active, idle, pending, total)

### Elasticsearch (user search)

Full-text user search (`GET /api/v1/users/search?query=...`) is backed by an
Elasticsearch 8 index synced from PostgreSQL via an async event listener
(`UserSearchIndexSyncListener`). If Elasticsearch is unreachable the controller
falls back to the legacy SQL `ILIKE` implementation so search keeps working
(possibly with lower quality) during an outage.

The index is named `users` and is seeded on startup from the database (capped at
1000 users) whenever it's missing or empty, so fresh clones / docker volumes
don't need manual backfill.

Manual queries:

```bash
# List everything in the index
curl http://localhost:9200/users/_search?q=*

# Match users by bio / city / name (multi_match with fullName boosted)
curl 'http://localhost:9200/users/_search' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "multi_match": {
        "query": "marathon",
        "fields": ["fullName^3", "firstName^2", "lastName^2", "bio", "city"]
      }
    }
  }'

# Check the mapping produced from UserDocument
curl http://localhost:9200/users/_mapping
```

### Distributed Tracing (OpenTelemetry + Jaeger)

The backend emits OpenTelemetry spans via the Micrometer tracing bridge and exports them with the OTLP HTTP protocol to a Jaeger all-in-one container. 100% sampling in dev, 10% in prod.

Automatic instrumentation covers:
- HTTP server spans (every REST/GraphQL controller call)
- JDBC spans (via Spring Boot's DataSource auto-config)
- Outgoing RestTemplate/WebClient calls
- Async execution: `@Async` methods propagate the parent trace context via a Micrometer `ContextExecutorService` wired through `AsyncConfiguration` (otherwise the `NotificationEventListener` spans would start as new roots)

Manual instrumentation via Micrometer `Observation` API:
- `PersonalRecordServiceImpl.recalculateAllPRs` opens a parent observation and two child observations (`personal-record.delete-existing` and `personal-record.recompute-loop`), so a trace in Jaeger clearly shows which phase dominated the overall call. The per-activity `checkAndUpdatePRs` calls then emit JDBC child spans via auto-instrumentation.

**Jaeger UI** — http://localhost:16686

Quick walkthrough:

```bash
# Generate some traffic
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8081/api/v1/activities

# Confirm Jaeger has registered the backend as a service
curl -s http://localhost:16686/api/services | jq
# => { "data": ["stride-backend", "jaeger-query"], ... }
```

Then open http://localhost:16686, pick `service=stride-backend`, find the span for `GET /api/v1/activities`, and inspect the nested JDBC children. For the PR recalculation, hit `POST /api/v1/personal-records/recalculate` and you'll see the `recalculateAllPRs → deleteExistingPRsForUser → recomputePRsForActivities → checkAndUpdatePRs → JDBC` nested trace.

Log lines in the `dev` profile now carry `[service,traceId,spanId]` so Jaeger traces can be cross-referenced with application logs. Prod JSON logs include `traceId` / `spanId` as top-level fields for log-aggregation platforms.

**Config override:** The OTLP endpoint is exposed via the `OTEL_EXPORTER_OTLP_ENDPOINT` env var (default `http://localhost:4318/v1/traces`) — set it to your collector if you prefer Tempo, Honeycomb, etc.

Grafana also has Jaeger wired as a datasource, so traces can be queried from Grafana dashboards alongside Prometheus metrics.

### Structured Logging

Logging format depends on the active Spring profile:

| Profile | Format | Use case |
|---------|--------|----------|
| `dev` (default) | Colored console | Local development |
| `prod` | JSON (Logstash encoder) | Production / log aggregation |

```bash
# View backend logs (dev - human readable)
docker compose logs -f backend

# Test JSON output with prod profile
SPRING_PROFILES_ACTIVE=prod docker compose up backend
```

Example JSON log line (prod):
```json
{
  "@timestamp": "2026-03-10T16:40:12.345Z",
  "level": "INFO",
  "logger_name": "c.s.b.StravaApplication",
  "message": "Started StravaApplication in 4.2 seconds",
  "thread_name": "main"
}
```

### Rate Limiting

Auth endpoints are protected against brute-force attacks with a per-IP sliding window rate limiter:

| Endpoint | Limit | Window |
|----------|-------|--------|
| `POST /api/v1/auth/login` | 10 requests | 1 minute |
| `POST /api/v1/auth/register` | 5 requests | 1 minute |

When the limit is exceeded, the API returns `429 Too Many Requests`:

```json
{"error": "Too many requests. Please try again later."}
```

Test it:

```bash
# Send 12 login requests — the last 2 should return 429
for i in $(seq 1 12); do
  echo "Request $i: $(curl -s -o /dev/null -w '%{http_code}' \
    -X POST http://localhost:8081/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"test@test.com","password":"wrong"}')"
done
```

Expected output:
```
Request 1: 401
...
Request 10: 401
Request 11: 429
Request 12: 429
```

---

## Running Tests

The project has a three-layer test pyramid covering backend, frontend unit/component, and frontend end-to-end.

### Backend (JUnit 5 + Mockito + Testcontainers)
```bash
./mvnw test
```

Key suites for recently added features:
- `GeoUtilsTest` — polyline format detection (legacy JSON vs Google Encoded), Haversine, encode/decode round-trip
- `FitParserTest` — hand-crafted byte-stream fixtures covering little-endian header parsing, developer fields, compressed timestamps, signature validation
- `PersonalRecordServiceImplTest` — PR detection thresholds, sliding-window algorithm, recalculate delete-before-fetch ordering
- `ImportServiceImplTest` — parser selection, validation, sample listing, path-traversal rejection
- `FitParserDebugTest` — opt-in regression test that runs the parser against a real `.fit` file passed via `-Dfit.debug.file=/path/to/file.fit`

### Frontend unit + component (Jest + React Testing Library)
```bash
cd frontend
npm test
```

Covers `utils/polyline`, `utils/formatters`, API services (`ImportService`, `PersonalRecordService`), and the `PersonalRecords` component (loading/empty/data states, Recalculate flow, improvement badge, date-as-link).

### Frontend end-to-end (Playwright)
```bash
# Requires backend on :8080 and frontend on :3000
cd frontend
npx playwright test
```

Suites in `frontend/e2e/`:
- `auth.spec.ts` — login / logout flow
- `activities.spec.ts` — create, view, like/unlike
- `notifications.spec.ts` — real-time notification delivery
- `import.spec.ts` — mode toggle, dropzone, sample download, GPX upload + clean-up, client-side validation rejection
- `personal-records.spec.ts` — PR tab on own and public profiles, Recalculate network roundtrip, seeded activity → grouped PR row

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is for educational purposes.

## Acknowledgments

- Inspired by [Strava](https://www.strava.com/)
- Icons by [React Icons](https://react-icons.github.io/react-icons/)
- Charts by [Recharts](https://recharts.org/)
