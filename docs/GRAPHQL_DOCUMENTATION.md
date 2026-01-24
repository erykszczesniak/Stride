# GraphQL API Documentation

**Endpoint:** `http://localhost:8080/graphql`
**GraphiQL Interface:** `http://localhost:8080/graphiql`

---

## Testowanie

### cURL

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "YOUR_QUERY_HERE"}'
```

### Postman/Insomnia

1. **Method:** POST
2. **URL:** `http://localhost:8080/graphql`
3. **Headers:** `Content-Type: application/json`
4. **Body (raw JSON):**
```json
{
  "query": "{ currentUser { id firstName lastName } }"
}
```

---

## Queries

### 1. User Queries

#### currentUser
Pobierz aktualnego użytkownika (domyślnie userId=1).

```graphql
query {
  currentUser {
    id
    firstName
    lastName
    email
    bio
    city
    country
    avatar
    followersCount
    followingCount
    activitiesCount
    createdAt
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ currentUser { id firstName lastName email followersCount followingCount } }"}'
```

#### user(id: ID!)
Pobierz użytkownika po ID.

```graphql
query {
  user(id: 1) {
    id
    firstName
    lastName
    email
    bio
    followersCount
    followingCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ user(id: 1) { id firstName lastName email bio } }"}'
```

#### users(page: Int, size: Int)
Pobierz listę użytkowników.

```graphql
query {
  users(page: 0, size: 10) {
    id
    firstName
    lastName
    email
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ users(page: 0, size: 10) { id firstName lastName email } }"}'
```

---

### 2. Activity Queries

#### activity(id: ID!)
Pobierz aktywność po ID.

```graphql
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
    maxSpeed
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
        lastName
      }
    }
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ activity(id: 1) { id title activityType distance duration likesCount } }"}'
```

#### activities(userId: ID, page: Int, size: Int)
Pobierz aktywności użytkownika.

```graphql
query {
  activities(userId: 1, page: 0, size: 10) {
    id
    title
    activityType
    distance
    duration
    startTime
    likesCount
    commentsCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ activities(userId: 1) { id title activityType distance duration startTime } }"}'
```

#### feedActivities(page: Int, size: Int)
Pobierz feed aktywności.

```graphql
query {
  feedActivities(page: 0, size: 10) {
    id
    title
    activityType
    distance
    duration
    startTime
    likesCount
    user {
      firstName
      lastName
    }
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ feedActivities(page: 0, size: 10) { id title activityType distance user { firstName } } }"}'
```

#### activityStats(userId: ID!)
Pobierz statystyki aktywności (miesięczne).

```graphql
query {
  activityStats(userId: 1) {
    totalDistance
    totalDuration
    totalActivities
    totalCalories
    totalElevationGain
    averageDistance
    averageDuration
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ activityStats(userId: 1) { totalDistance totalDuration totalActivities totalCalories } }"}'
```

#### weeklyStats(userId: ID!)
Pobierz tygodniowe statystyki.

```graphql
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

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ weeklyStats(userId: 1) { totalDistance trendPercentage dailyStats { dayName distance } } }"}'
```

---

### 3. Goal Queries

#### goals(userId: ID!)
Pobierz wszystkie cele użytkownika.

```graphql
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
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ goals(userId: 1) { id goalType period targetValue currentValue progressPercentage isActive } }"}'
```

#### activeGoals(userId: ID!)
Pobierz aktywne cele użytkownika.

```graphql
query {
  activeGoals(userId: 1) {
    id
    goalType
    targetValue
    currentValue
    progressPercentage
    remainingValue
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ activeGoals(userId: 1) { id goalType targetValue currentValue progressPercentage } }"}'
```

---

### 4. Challenge Queries

#### challenge(id: ID!)
Pobierz wyzwanie po ID.

```graphql
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
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ challenge(id: 1) { id name challengeType targetValue participantsCount isParticipating } }"}'
```

#### activeChallenges(page: Int, size: Int)
Pobierz aktywne wyzwania.

```graphql
query {
  activeChallenges(page: 0, size: 10) {
    id
    name
    challengeType
    targetValue
    participantsCount
    startDate
    endDate
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ activeChallenges { id name challengeType targetValue participantsCount } }"}'
```

#### userChallenges(userId: ID!)
Pobierz wyzwania użytkownika.

```graphql
query {
  userChallenges(userId: 1) {
    id
    name
    challengeType
    targetValue
    userProgress
    isActive
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ userChallenges(userId: 1) { id name challengeType userProgress } }"}'
```

---

### 5. Club Queries

#### club(id: ID!)
Pobierz klub po ID.

```graphql
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
    isPrivate
    owner {
      id
      firstName
      lastName
    }
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ club(id: 1) { id name description membersCount isMember } }"}'
```

#### clubs(page: Int, size: Int)
Pobierz listę klubów.

```graphql
query {
  clubs(page: 0, size: 10) {
    id
    name
    description
    sportType
    membersCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ clubs { id name sportType membersCount } }"}'
```

#### userClubs(userId: ID!)
Pobierz kluby użytkownika.

```graphql
query {
  userClubs(userId: 1) {
    id
    name
    sportType
    membersCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ userClubs(userId: 1) { id name sportType } }"}'
```

---

### 6. Event Queries

#### event(id: ID!)
Pobierz event po ID.

```graphql
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
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ event(id: 1) { id name eventDate location registeredCount isRegistered } }"}'
```

#### upcomingEvents(page: Int, size: Int)
Pobierz nadchodzące eventy.

```graphql
query {
  upcomingEvents(page: 0, size: 10) {
    id
    name
    eventDate
    location
    city
    registeredCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ upcomingEvents { id name eventDate location } }"}'
```

#### userEvents(userId: ID!)
Pobierz eventy użytkownika.

```graphql
query {
  userEvents(userId: 1) {
    id
    name
    eventDate
    location
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ userEvents(userId: 1) { id name eventDate } }"}'
```

---

### 7. Dashboard Query

#### dashboard
Pobierz pełne dane dashboardu.

```graphql
query {
  dashboard {
    user {
      id
      firstName
      lastName
      email
      followersCount
      followingCount
    }
    stats {
      totalDistance
      totalDuration
      totalActivities
      totalCalories
      totalElevationGain
    }
    weeklyStats {
      totalDistance
      trendPercentage
      dailyStats {
        dayName
        distance
        duration
        activities
      }
    }
    recentActivities {
      id
      title
      activityType
      distance
      duration
      startTime
      likesCount
    }
    weeklyGoal {
      id
      goalType
      targetValue
      currentValue
      progressPercentage
    }
    upcomingEvents {
      id
      name
      eventDate
      location
    }
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ dashboard { user { id firstName lastName } stats { totalDistance totalActivities } recentActivities { id title } } }"}'
```

---

## Mutations

### 1. Activity Mutations

#### createActivity
Utwórz nową aktywność.

```graphql
mutation {
  createActivity(input: {
    title: "Morning Run"
    description: "Easy morning jog"
    activityType: RUN
    distance: 5.5
    duration: 1800
    elevationGain: 50
    calories: 450
    averageSpeed: 11.0
    startTime: "2026-01-19T07:00:00"
    isPrivate: false
  }) {
    id
    title
    activityType
    distance
    duration
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { createActivity(input: { title: \"Test Run\", activityType: RUN, distance: 5.0, duration: 1800, startTime: \"2026-01-19T07:00:00\" }) { id title } }"}'
```

#### updateActivity
Zaktualizuj aktywność.

```graphql
mutation {
  updateActivity(id: 1, input: {
    title: "Morning Run - Updated"
    description: "Great run!"
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
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { updateActivity(id: 1, input: { title: \"Updated Run\", activityType: RUN, distance: 6.0, duration: 2000, startTime: \"2026-01-19T07:00:00\" }) { id title } }"}'
```

#### deleteActivity
Usuń aktywność.

```graphql
mutation {
  deleteActivity(id: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { deleteActivity(id: 1) }"}'
```

#### likeActivity
Polub aktywność.

```graphql
mutation {
  likeActivity(id: 1) {
    id
    likesCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { likeActivity(id: 1) { id likesCount } }"}'
```

#### unlikeActivity
Cofnij polubienie.

```graphql
mutation {
  unlikeActivity(id: 1) {
    id
    likesCount
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { unlikeActivity(id: 1) { id likesCount } }"}'
```

#### addComment
Dodaj komentarz do aktywności.

```graphql
mutation {
  addComment(activityId: 1, content: "Great workout!") {
    id
    content
    createdAt
    user {
      firstName
      lastName
    }
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { addComment(activityId: 1, content: \"Nice run!\") { id content createdAt } }"}'
```

---

### 2. User Mutations

#### updateProfile
Zaktualizuj profil użytkownika.

```graphql
mutation {
  updateProfile(input: {
    firstName: "John"
    lastName: "Smith"
    bio: "Running enthusiast"
    city: "Warsaw"
    country: "Poland"
  }) {
    id
    firstName
    lastName
    bio
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { updateProfile(input: { firstName: \"John\", lastName: \"Smith\", bio: \"Runner\" }) { id firstName lastName bio } }"}'
```

#### followUser
Zacznij obserwować użytkownika.

```graphql
mutation {
  followUser(userId: 2)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { followUser(userId: 2) }"}'
```

#### unfollowUser
Przestań obserwować użytkownika.

```graphql
mutation {
  unfollowUser(userId: 2)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { unfollowUser(userId: 2) }"}'
```

---

### 3. Goal Mutations

#### createGoal
Utwórz nowy cel.

```graphql
mutation {
  createGoal(input: {
    goalType: DISTANCE
    period: WEEKLY
    activityType: RUN
    targetValue: 30.0
  }) {
    id
    goalType
    period
    targetValue
    progressPercentage
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { createGoal(input: { goalType: DISTANCE, period: WEEKLY, targetValue: 30.0 }) { id goalType targetValue } }"}'
```

#### updateGoal
Zaktualizuj cel.

```graphql
mutation {
  updateGoal(id: 1, input: {
    goalType: DISTANCE
    period: WEEKLY
    targetValue: 40.0
  }) {
    id
    targetValue
  }
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { updateGoal(id: 1, input: { goalType: DISTANCE, period: WEEKLY, targetValue: 40.0 }) { id targetValue } }"}'
```

#### deleteGoal
Usuń cel.

```graphql
mutation {
  deleteGoal(id: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { deleteGoal(id: 1) }"}'
```

---

### 4. Challenge Mutations

#### joinChallenge
Dołącz do wyzwania.

```graphql
mutation {
  joinChallenge(challengeId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { joinChallenge(challengeId: 1) }"}'
```

#### leaveChallenge
Opuść wyzwanie.

```graphql
mutation {
  leaveChallenge(challengeId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { leaveChallenge(challengeId: 1) }"}'
```

---

### 5. Club Mutations

#### joinClub
Dołącz do klubu.

```graphql
mutation {
  joinClub(clubId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { joinClub(clubId: 1) }"}'
```

#### leaveClub
Opuść klub.

```graphql
mutation {
  leaveClub(clubId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { leaveClub(clubId: 1) }"}'
```

---

### 6. Event Mutations

#### registerForEvent
Zarejestruj się na event.

```graphql
mutation {
  registerForEvent(eventId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { registerForEvent(eventId: 1) }"}'
```

#### unregisterFromEvent
Wyrejestruj się z eventu.

```graphql
mutation {
  unregisterFromEvent(eventId: 1)
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { unregisterFromEvent(eventId: 1) }"}'
```

---

## Enums

### ActivityType
```
RUN, RIDE, SWIM, WALK, HIKE, ALPINE_SKI, BACKCOUNTRY_SKI, CROSSFIT,
ELLIPTICAL, ICE_SKATE, INLINE_SKATE, KAYAKING, KITESURF, NORDIC_SKI,
ROCK_CLIMBING, ROLLER_SKI, ROWING, SNOWBOARD, SNOWSHOE, STAIR_STEPPER,
STAND_UP_PADDLING, SURFING, VIRTUAL_RIDE, VIRTUAL_RUN, WEIGHT_TRAINING,
WINDSURF, WORKOUT, YOGA, OTHER
```

### GoalType
```
DISTANCE, TIME, ACTIVITIES, ELEVATION
```

### GoalPeriod
```
WEEKLY, MONTHLY, YEARLY
```

### ChallengeType
```
DISTANCE, TIME, ELEVATION, ACTIVITIES_COUNT, SEGMENT
```

---

## Obsługa błędów

GraphQL zwraca błędy w formacie:

```json
{
  "errors": [
    {
      "message": "Activity not found",
      "locations": [{"line": 1, "column": 3}],
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ],
  "data": null
}
```

---

## Tips

1. **GraphiQL** - najwygodniejszy sposób testowania, ma autouzupełnianie i dokumentację
2. **Fragmenty** - używaj fragmentów dla powtarzających się pól
3. **Aliasy** - możesz używać aliasów dla wielu zapytań tego samego typu
4. **Variables** - używaj zmiennych dla dynamicznych wartości

### Przykład z aliasami:

```graphql
query {
  userOne: user(id: 1) { id firstName }
  userTwo: user(id: 2) { id firstName }
}
```

### Przykład z fragmentami:

```graphql
fragment UserFields on User {
  id
  firstName
  lastName
  email
}

query {
  currentUser {
    ...UserFields
    bio
  }
  user(id: 2) {
    ...UserFields
  }
}
```
