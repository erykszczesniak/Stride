# STRIDE - Fitness Activity Tracker

A full-stack fitness activity tracking application inspired by Strava. Track your workouts, join challenges, connect with other athletes, and achieve your fitness goals.

> **Note:** This project is currently in active development. Features and UI may change.

![Dashboard Preview](docs/screenshots/dashboard.png)

## Features

### Activity Tracking
- Record and track various activity types (Running, Cycling, Swimming, Hiking, Workouts)
- View detailed activity statistics (distance, duration, pace, calories, elevation)
- Like and comment on activities
- Add reactions to comments

### Social Features
- Follow other athletes
- Activity feed showing followed users' activities
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

### User Experience
- Dark/Light theme toggle
- Responsive design
- Real-time notifications
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

#### Settings - Preferences
![Settings Preferences](docs/screenshots/settings-preferences.png)
*App preferences - notifications, units, language, theme*

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
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **GraphQL** API support
- **Flyway** for database migrations
- **Redis** for caching
- **Swagger/OpenAPI** documentation
- **Testcontainers** for integration testing

### Frontend
- **React 18** with Hooks
- **React Router** for navigation
- **Styled Components** for styling
- **Recharts** for data visualization
- **Axios** for API communication
- **React Icons** for iconography

### Infrastructure
- **Docker** & **Docker Compose**
- **PostgreSQL 15**
- **Redis 7**
- **Nginx** (production)

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

# Start all services (backend, frontend, PostgreSQL, Redis)
make up

# Or without make:
docker compose up -d --build
```

That's it! The application will be available at:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **GraphQL:** http://localhost:8080/graphql

#### Sample Users

| Email | Password | Role |
|-------|----------|------|
| john@example.com | password123 | USER |
| jane@example.com | password123 | USER |
| admin@example.com | admin123 | ADMIN |

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

# Access Redis CLI
docker compose exec redis redis-cli
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

## API Documentation

Once the backend is running, API documentation is available at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **GraphQL Playground:** http://localhost:8080/graphql

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

## Project Structure

```
strava/
├── docker-compose.yml     # Docker services configuration
├── Dockerfile             # Backend Docker image
├── Makefile               # Convenience commands
├── DOCKER.md              # Docker commands reference
├── src/main/java/com/strava/backend/
│   ├── configuration/     # Spring & Redis configurations
│   ├── controller/        # REST controllers
│   ├── domain/            # Entity models
│   ├── dto/               # Data transfer objects
│   ├── graphql/           # GraphQL resolvers
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

## Running Tests

### Backend Tests
```bash
./mvnw test
```

### Frontend Tests
```bash
cd frontend
npm test
```

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
