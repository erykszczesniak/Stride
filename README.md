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
<summary>Click to expand screenshots</summary>

### Dashboard
![Dashboard](docs/screenshots/dashboard.png)
*Main dashboard with activity summary, weekly stats, and goals*

### Activities
![Activities](docs/screenshots/activities.png)
*Activity feed with filtering and search*

### Activity Details
![Activity Details](docs/screenshots/activity-details.png)
*Detailed view with stats, comments, and reactions*

### Challenges
![Challenges](docs/screenshots/challenges.png)
*Monthly challenges with leaderboard*

### Clubs
![Clubs](docs/screenshots/clubs.png)
*Join sport-specific communities*

### Events
![Events](docs/screenshots/events.png)
*Upcoming local fitness events*

### Profile
![Profile](docs/screenshots/profile.png)
*User profile with statistics and activity history*

### Dark Mode
![Dark Mode](docs/screenshots/dark-mode.png)
*Full dark mode support*

</details>

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.4**
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **GraphQL** API support
- **Flyway** for database migrations
- **Swagger/OpenAPI** documentation
- **Testcontainers** for integration testing

### Frontend
- **React 18** with Hooks
- **React Router** for navigation
- **Styled Components** for styling
- **Recharts** for data visualization
- **Axios** for API communication
- **React Icons** for iconography

### Database
- **PostgreSQL** (production)
- **H2** (development/testing)

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL 15+ (or use H2 for development)
- Maven 3.9+

### Backend Setup

1. Clone the repository
```bash
git clone https://github.com/erykszczesniak/strava.git
cd strava
```

2. Configure database (optional - uses H2 by default)
```bash
# For PostgreSQL, update src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/strava
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Run the backend
```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory
```bash
cd frontend
```

2. Install dependencies
```bash
npm install
```

3. Start the development server
```bash
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
├── src/main/java/com/strava/backend/
│   ├── configuration/     # Spring configurations
│   ├── controller/        # REST controllers
│   ├── domain/           # Entity models
│   ├── dto/              # Data transfer objects
│   ├── graphql/          # GraphQL resolvers
│   ├── repository/       # JPA repositories
│   ├── security/         # JWT & auth
│   └── service/          # Business logic
├── frontend/
│   ├── src/
│   │   ├── api/          # API services
│   │   ├── components/   # Reusable components
│   │   ├── context/      # React contexts
│   │   ├── pages/        # Page components
│   │   └── styles/       # Theme & global styles
│   └── public/
└── docs/                  # Documentation
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
