# GraphQL Example Queries

Endpoint: `POST /graphql`

## User Queries

```graphql
# Get user by ID
query {
  user(id: 1) {
    id
    firstName
    lastName
    email
    bio
    city
    country
    followersCount
    followingCount
    activitiesCount
  }
}

# Get current user
query {
  currentUser {
    id
    firstName
    lastName
    email
  }
}

# Get paginated users
query {
  users(page: 0, size: 10) {
    id
    firstName
    lastName
  }
}
```

## Activity Queries

```graphql
# Get single activity
query {
  activity(id: 1) {
    id
    title
    description
    activityType
    distance
    duration
    elevationGain
    calories
    averageSpeed
    startTime
    likesCount
    commentsCount
    likedByCurrentUser
    user {
      id
      firstName
      lastName
    }
    comments {
      id
      content
      createdAt
      user {
        firstName
      }
    }
  }
}

# Get user activities
query {
  activities(userId: 1, page: 0, size: 10) {
    id
    title
    activityType
    distance
    duration
    startTime
  }
}

# Get feed activities
query {
  feedActivities(page: 0, size: 10) {
    id
    title
    activityType
    distance
    user {
      firstName
      lastName
    }
  }
}

# Get activity stats
query {
  activityStats(userId: 1) {
    totalDistance
    totalDuration
    totalCalories
    totalActivities
    totalElevationGain
    averageDistance
    averageDuration
    trendPercentage
  }
}

# Get weekly stats
query {
  weeklyStats(userId: 1) {
    totalDistance
    trendPercentage
    dailyStats {
      dayName
      distance
      duration
      activities
    }
  }
}
```

## Goal Queries

```graphql
# Get all goals for user
query {
  goals(userId: 1) {
    id
    goalType
    period
    activityType
    targetValue
    currentValue
    progressPercentage
    remainingValue
    startDate
    endDate
    isActive
    isCompleted
  }
}

# Get active goals
query {
  activeGoals(userId: 1) {
    id
    goalType
    period
    targetValue
    currentValue
    progressPercentage
  }
}
```

## Event Queries

```graphql
# Get single event
query {
  event(id: 1) {
    id
    name
    description
    activityType
    eventDate
    location
    city
    distance
    registeredCount
    maxParticipants
    isRegistered
  }
}

# Get upcoming events
query {
  upcomingEvents(page: 0, size: 10) {
    id
    name
    eventDate
    location
    registeredCount
  }
}

# Get user events
query {
  userEvents(userId: 1) {
    id
    name
    eventDate
  }
}
```

## Challenge Queries

```graphql
# Get single challenge
query {
  challenge(id: 1) {
    id
    name
    description
    challengeType
    activityType
    targetValue
    startDate
    endDate
    participantsCount
    isParticipating
    isActive
    userProgress
  }
}

# Get active challenges
query {
  activeChallenges(page: 0, size: 10) {
    id
    name
    challengeType
    targetValue
    participantsCount
    isActive
  }
}

# Get user challenges
query {
  userChallenges(userId: 1) {
    id
    name
    userProgress
  }
}
```

## Club Queries

```graphql
# Get single club
query {
  club(id: 1) {
    id
    name
    description
    city
    country
    sportType
    membersCount
    isMember
    owner {
      firstName
      lastName
    }
  }
}

# Get all public clubs
query {
  clubs(page: 0, size: 10) {
    id
    name
    sportType
    membersCount
  }
}

# Get user clubs
query {
  userClubs(userId: 1) {
    id
    name
    isMember
  }
}
```

## Dashboard Query

```graphql
query {
  dashboard {
    user {
      id
      firstName
      lastName
    }
    stats {
      totalDistance
      totalDuration
      totalActivities
    }
    weeklyStats {
      totalDistance
      dailyStats {
        dayName
        distance
      }
    }
    recentActivities {
      id
      title
      activityType
      distance
    }
    weeklyGoal {
      goalType
      targetValue
      currentValue
      progressPercentage
    }
    upcomingEvents {
      id
      name
      eventDate
    }
  }
}
```

---

## Mutations

### Activity Mutations

```graphql
# Create activity
mutation {
  createActivity(input: {
    title: "Morning Run"
    description: "Easy recovery run"
    activityType: RUN
    distance: 5.5
    duration: 1800
    elevationGain: 50
    calories: 350
    averageSpeed: 11.0
    startTime: "2026-01-19T07:00:00"
    startLocation: "Central Park"
    isPrivate: false
  }) {
    id
    title
    distance
    duration
  }
}

# Update activity
mutation {
  updateActivity(id: 1, input: {
    title: "Updated Morning Run"
    description: "Changed description"
    activityType: RUN
    distance: 6.0
    duration: 2000
    startTime: "2026-01-19T07:00:00"
  }) {
    id
    title
    distance
  }
}

# Delete activity
mutation {
  deleteActivity(id: 1)
}

# Like activity
mutation {
  likeActivity(id: 1) {
    id
    likesCount
    likedByCurrentUser
  }
}

# Unlike activity
mutation {
  unlikeActivity(id: 1) {
    id
    likesCount
    likedByCurrentUser
  }
}

# Add comment
mutation {
  addComment(activityId: 1, content: "Great run!") {
    id
    content
    createdAt
    user {
      firstName
    }
  }
}
```

### Goal Mutations

```graphql
# Create goal
mutation {
  createGoal(input: {
    goalType: DISTANCE
    period: WEEKLY
    activityType: RUN
    targetValue: 50.0
  }) {
    id
    goalType
    period
    targetValue
    startDate
    endDate
  }
}

# Update goal
mutation {
  updateGoal(id: 1, input: {
    goalType: DISTANCE
    period: MONTHLY
    targetValue: 200.0
  }) {
    id
    targetValue
  }
}

# Delete goal
mutation {
  deleteGoal(id: 1)
}
```

### User Mutations

```graphql
# Update profile
mutation {
  updateProfile(input: {
    firstName: "John"
    lastName: "Doe"
    bio: "Running enthusiast"
    city: "New York"
    country: "USA"
  }) {
    id
    firstName
    lastName
    bio
  }
}

# Follow user
mutation {
  followUser(userId: 2)
}

# Unfollow user
mutation {
  unfollowUser(userId: 2)
}
```

### Event Mutations

```graphql
# Register for event
mutation {
  registerForEvent(eventId: 1)
}

# Unregister from event
mutation {
  unregisterFromEvent(eventId: 1)
}
```

### Challenge Mutations

```graphql
# Join challenge
mutation {
  joinChallenge(challengeId: 1)
}

# Leave challenge
mutation {
  leaveChallenge(challengeId: 1)
}
```

### Club Mutations

```graphql
# Join club
mutation {
  joinClub(clubId: 1)
}

# Leave club
mutation {
  leaveClub(clubId: 1)
}
```

---

## Testing with cURL

```bash
# Basic query
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ currentUser { id firstName lastName } }"}'

# Query with variables
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetUser($id: ID!) { user(id: $id) { id firstName lastName } }",
    "variables": { "id": 1 }
  }'

# Mutation example
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { likeActivity(id: 1) { id likesCount } }"
  }'
```

## Testing GraphQL

Use Postman, Insomnia or other GraphQL client at: `http://localhost:8080/graphql`

---

## Narzędzia do testowania GraphQL

### 1. Postman
- **URL:** https://www.postman.com/downloads/
- Darmowa aplikacja desktopowa
- Wsparcie dla GraphQL z autouzupełnianiem
- Możliwość zapisywania kolekcji zapytań
- Jak użyć:
  1. Utwórz nowy request POST
  2. URL: `http://localhost:8080/graphql`
  3. Body → GraphQL
  4. Wklej query

### 2. Insomnia
- **URL:** https://insomnia.rest/download
- Darmowa aplikacja desktopowa
- Natywne wsparcie GraphQL
- Automatyczne pobieranie schematu
- Przejrzysty interfejs

### 3. Apollo Studio (online)
- **URL:** https://studio.apollographql.com/sandbox/explorer
- Darmowe narzędzie online
- Nie wymaga instalacji
- Wpisz endpoint: `http://localhost:8080/graphql`
- Automatycznie pobiera schemat i pokazuje dokumentację

### 4. Altair GraphQL Client
- **URL:** https://altairgraphql.dev/
- Dostępne jako:
  - Rozszerzenie Chrome/Firefox
  - Aplikacja desktopowa
- Darmowe i open source
- Wsparcie dla subscriptions

### 5. Banana Cake Pop (online)
- **URL:** https://eat.bananacakepop.com/
- Darmowe narzędzie online od twórców Hot Chocolate
- Nie wymaga instalacji
- Przejrzysty interfejs

### 6. Rozszerzenia do VS Code
- **GraphQL for VSCode** - podświetlanie składni
- **Thunder Client** - jak Postman w VS Code
- **REST Client** - wysyłanie requestów z plików `.http`

### 7. IntelliJ IDEA (Ultimate)
- Wbudowane wsparcie GraphQL
- HTTP Client z plikami `.http` lub `.graphql`
- Przykład pliku `requests.http`:
  ```http
  POST http://localhost:8080/graphql
  Content-Type: application/json

  {
    "query": "{ currentUser { id firstName lastName } }"
  }
  ```

---

## Szybki start - testowanie

1. Uruchom aplikację Spring Boot
2. Użyj Postman/Insomnia z endpoint: `http://localhost:8080/graphql`
3. Wyślij przykładowe query:
   ```graphql
   {
     currentUser {
       id
       firstName
       lastName
     }
   }
   ```
4. Kliknij przycisk "Play" lub Ctrl+Enter
