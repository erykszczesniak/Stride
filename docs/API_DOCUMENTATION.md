# Strava Clone API Documentation

**Base URL:** `http://localhost:8080`

**Uwaga:** Autoryzacja jest tymczasowo wyłączona. Wszystkie endpointy przyjmują opcjonalny parametr `userId` - jeśli nie podany, używany jest `DEFAULT_USER_ID = 1`.

---

## Spis Treści

1. [Authentication](#1-authentication)
2. [Users](#2-users)
3. [Activities](#3-activities)
4. [Comments](#4-comments)
5. [Goals](#5-goals)
6. [Clubs](#6-clubs)
7. [Challenges](#7-challenges)
8. [Events](#8-events)
9. [Dashboard](#9-dashboard)
10. [Search](#10-search)
11. [Admin](#11-admin)
12. [Export](#12-export)
13. [GraphQL](#13-graphql)

---

## 1. Authentication

**Base Path:** `/api/v1/auth`

### POST /api/v1/auth/login
Logowanie użytkownika.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

### POST /api/v1/auth/register
Rejestracja nowego użytkownika.

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### POST /api/v1/auth/refresh
Odświeżenie tokena JWT.

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### POST /api/v1/auth/forgot-password
Żądanie resetu hasła.

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

### POST /api/v1/auth/reset-password
Reset hasła z kodem.

```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "code": "123456",
    "password": "newpassword123"
  }'
```

---

## 2. Users

**Base Path:** `/api/v1/users`

### GET /api/v1/users/me
Pobierz aktualnego użytkownika.

```bash
curl http://localhost:8080/api/v1/users/me

# lub z konkretnym userId
curl "http://localhost:8080/api/v1/users/me?userId=2"
```

### GET /api/v1/users/{userId}
Pobierz użytkownika po ID.

```bash
curl http://localhost:8080/api/v1/users/1
```

### GET /api/v1/users
Pobierz listę wszystkich użytkowników (z paginacją).

```bash
curl "http://localhost:8080/api/v1/users?page=0&size=20"
```

### PUT /api/v1/users/me
Aktualizuj profil użytkownika.

```bash
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "bio": "Running enthusiast",
    "city": "Warsaw",
    "country": "Poland"
  }'
```

### POST /api/v1/users/{userId}/follow
Zacznij obserwować użytkownika.

```bash
curl -X POST "http://localhost:8080/api/v1/users/2/follow?currentUserId=1"
```

### DELETE /api/v1/users/{userId}/follow
Przestań obserwować użytkownika.

```bash
curl -X DELETE "http://localhost:8080/api/v1/users/2/follow?currentUserId=1"
```

### GET /api/v1/users/{userId}/followers
Pobierz listę obserwujących użytkownika.

```bash
curl http://localhost:8080/api/v1/users/1/followers
```

### GET /api/v1/users/{userId}/following
Pobierz listę obserwowanych przez użytkownika.

```bash
curl http://localhost:8080/api/v1/users/1/following
```

### GET /api/v1/users/suggested
Pobierz sugerowanych użytkowników do obserwowania. Zwraca użytkowników, których aktualny użytkownik jeszcze nie obserwuje, posortowanych według liczby obserwujących (najpopularniejsi pierwsi).

```bash
curl "http://localhost:8080/api/v1/users/suggested?userId=1&limit=5"
```

**Parametry:**
| Parametr | Typ | Wymagany | Domyślnie | Opis |
|----------|-----|----------|-----------|------|
| userId | Long | Nie | 1 | ID aktualnego użytkownika |
| limit | int | Nie | 5 | Maksymalna liczba sugerowanych użytkowników |

**Response:**
```json
[
  {
    "id": 4,
    "firstName": "Admin",
    "lastName": "User",
    "avatar": null,
    "fullName": "Admin User"
  },
  {
    "id": 5,
    "firstName": "Jane",
    "lastName": "Smith",
    "avatar": "https://example.com/avatar.jpg",
    "fullName": "Jane Smith"
  }
]
```

**Logika sugestii:**
- Wyklucza aktualnego użytkownika
- Wyklucza użytkowników już obserwowanych
- Sortuje według liczby obserwujących (malejąco)

---

## 3. Activities

**Base Path:** `/api/v1/activities`

### GET /api/v1/activities/{activityId}
Pobierz aktywność po ID.

```bash
curl http://localhost:8080/api/v1/activities/1
```

### GET /api/v1/activities/feed
Pobierz feed aktywności (własne + obserwowanych).

```bash
curl "http://localhost:8080/api/v1/activities/feed?page=0&size=10"
```

### GET /api/v1/activities/my
Pobierz własne aktywności.

```bash
curl "http://localhost:8080/api/v1/activities/my?page=0&size=10"
```

### GET /api/v1/activities/user/{targetUserId}
Pobierz aktywności konkretnego użytkownika.

```bash
curl "http://localhost:8080/api/v1/activities/user/2?page=0&size=10"
```

### GET /api/v1/activities/public
Pobierz publiczne aktywności.

```bash
curl "http://localhost:8080/api/v1/activities/public?page=0&size=10"
```

### GET /api/v1/activities/by-type
Pobierz aktywności po typie.

```bash
curl "http://localhost:8080/api/v1/activities/by-type?activityType=RUN&page=0&size=10"
```

**Dostępne typy:** `RUN`, `RIDE`, `SWIM`, `WALK`, `HIKE`, `WORKOUT`, `YOGA`, `OTHER`, `ALPINE_SKI`, `BACKCOUNTRY_SKI`, `CROSSFIT`, `ELLIPTICAL`, `ICE_SKATE`, `INLINE_SKATE`, `KAYAKING`, `KITESURF`, `NORDIC_SKI`, `ROCK_CLIMBING`, `ROLLER_SKI`, `ROWING`, `SNOWBOARD`, `SNOWSHOE`, `STAIR_STEPPER`, `STAND_UP_PADDLING`, `SURFING`, `VIRTUAL_RIDE`, `VIRTUAL_RUN`, `WEIGHT_TRAINING`, `WINDSURF`

### POST /api/v1/activities
Utwórz nową aktywność.

```bash
curl -X POST http://localhost:8080/api/v1/activities \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Morning Run",
    "description": "Easy morning jog in the park",
    "activityType": "RUN",
    "distance": 5.5,
    "duration": 1800,
    "elevationGain": 50,
    "calories": 450,
    "averageSpeed": 11.0,
    "maxSpeed": 14.5,
    "startTime": "2026-01-19T07:00:00",
    "startLocation": "Central Park",
    "isPrivate": false
  }'
```

### PUT /api/v1/activities/{activityId}
Zaktualizuj aktywność.

```bash
curl -X PUT http://localhost:8080/api/v1/activities/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Morning Run - Updated",
    "description": "Great morning run!",
    "activityType": "RUN",
    "distance": 6.0,
    "duration": 2000,
    "startTime": "2026-01-19T07:00:00"
  }'
```

### DELETE /api/v1/activities/{activityId}
Usuń aktywność.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1
```

### POST /api/v1/activities/{activityId}/like
Polub aktywność.

```bash
curl -X POST http://localhost:8080/api/v1/activities/1/like
```

### DELETE /api/v1/activities/{activityId}/like
Cofnij polubienie aktywności.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1/like
```

### GET /api/v1/activities/stats/monthly
Pobierz miesięczne statystyki.

```bash
curl http://localhost:8080/api/v1/activities/stats/monthly
```

**Response:**
```json
{
  "totalDistance": 150.5,
  "totalDuration": 54000,
  "totalActivities": 12,
  "totalCalories": 5400,
  "totalElevationGain": 850,
  "averageDistance": 12.54,
  "averageDuration": 4500.0
}
```

### GET /api/v1/activities/stats/weekly
Pobierz tygodniowe statystyki.

```bash
curl http://localhost:8080/api/v1/activities/stats/weekly
```

**Response:**
```json
{
  "totalDistance": 45.2,
  "trendPercentage": "+15%",
  "dailyStats": [
    {"dayName": "Mon", "distance": 5.5, "duration": 1800, "activities": 1},
    {"dayName": "Tue", "distance": 0, "duration": 0, "activities": 0}
  ]
}
```

---

## 4. Comments

**Base Path:** `/api/v1/activities/{activityId}/comments`

### GET /api/v1/activities/{activityId}/comments
Pobierz komentarze aktywności.

```bash
curl http://localhost:8080/api/v1/activities/1/comments
```

### POST /api/v1/activities/{activityId}/comments
Dodaj komentarz do aktywności.

```bash
curl -X POST http://localhost:8080/api/v1/activities/1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Great run! Keep it up!"
  }'
```

### PUT /api/v1/activities/{activityId}/comments/{commentId}
Zaktualizuj komentarz.

```bash
curl -X PUT http://localhost:8080/api/v1/activities/1/comments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Updated comment text"
  }'
```

### DELETE /api/v1/activities/{activityId}/comments/{commentId}
Usuń komentarz.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1/comments/1
```

---

## 5. Goals

**Base Path:** `/api/v1/goals`

### GET /api/v1/goals
Pobierz aktywne cele użytkownika.

```bash
curl http://localhost:8080/api/v1/goals
```

### GET /api/v1/goals/all
Pobierz wszystkie cele użytkownika.

```bash
curl http://localhost:8080/api/v1/goals/all
```

### GET /api/v1/goals/{goalId}
Pobierz cel po ID.

```bash
curl http://localhost:8080/api/v1/goals/1
```

### POST /api/v1/goals
Utwórz nowy cel.

```bash
curl -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "goalType": "DISTANCE",
    "period": "WEEKLY",
    "activityType": "RUN",
    "targetValue": 30.0,
    "startDate": "2026-01-20T00:00:00",
    "endDate": "2026-01-26T23:59:59"
  }'
```

**Typy celów:** `DISTANCE`, `TIME`, `ACTIVITIES`, `ELEVATION`
**Okresy:** `WEEKLY`, `MONTHLY`, `YEARLY`

### PUT /api/v1/goals/{goalId}
Zaktualizuj cel.

```bash
curl -X PUT http://localhost:8080/api/v1/goals/1 \
  -H "Content-Type: application/json" \
  -d '{
    "goalType": "DISTANCE",
    "period": "WEEKLY",
    "targetValue": 40.0,
    "startDate": "2026-01-20T00:00:00",
    "endDate": "2026-01-26T23:59:59"
  }'
```

### DELETE /api/v1/goals/{goalId}
Usuń cel.

```bash
curl -X DELETE http://localhost:8080/api/v1/goals/1
```

### POST /api/v1/goals/progress
Przelicz postęp celów użytkownika.

```bash
curl -X POST http://localhost:8080/api/v1/goals/progress
```

---

## 6. Clubs

**Base Path:** `/api/v1/clubs`

### GET /api/v1/clubs
Pobierz listę publicznych klubów.

```bash
curl "http://localhost:8080/api/v1/clubs?page=0&size=20"
```

### GET /api/v1/clubs/{clubId}
Pobierz klub po ID.

```bash
curl http://localhost:8080/api/v1/clubs/1
```

### GET /api/v1/clubs/my
Pobierz kluby użytkownika.

```bash
curl http://localhost:8080/api/v1/clubs/my
```

### GET /api/v1/clubs/search
Szukaj klubów.

```bash
curl "http://localhost:8080/api/v1/clubs/search?q=runners&page=0&size=20"
```

### POST /api/v1/clubs
Utwórz nowy klub.

```bash
curl -X POST http://localhost:8080/api/v1/clubs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warsaw Runners",
    "description": "Running club for Warsaw enthusiasts",
    "city": "Warsaw",
    "country": "Poland",
    "sportType": "RUN",
    "isPrivate": false
  }'
```

### PUT /api/v1/clubs/{clubId}
Zaktualizuj klub.

```bash
curl -X PUT http://localhost:8080/api/v1/clubs/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warsaw Runners Club",
    "description": "Updated description",
    "city": "Warsaw",
    "country": "Poland",
    "sportType": "RUN"
  }'
```

### DELETE /api/v1/clubs/{clubId}
Usuń klub (tylko właściciel).

```bash
curl -X DELETE http://localhost:8080/api/v1/clubs/1
```

### POST /api/v1/clubs/{clubId}/join
Dołącz do klubu.

```bash
curl -X POST http://localhost:8080/api/v1/clubs/1/join
```

### POST /api/v1/clubs/{clubId}/leave
Opuść klub.

```bash
curl -X POST http://localhost:8080/api/v1/clubs/1/leave
```

### GET /api/v1/clubs/{clubId}/members
Pobierz członków klubu.

```bash
curl http://localhost:8080/api/v1/clubs/1/members
```

---

## 7. Challenges

**Base Path:** `/api/v1/challenges`

### GET /api/v1/challenges
Pobierz aktywne wyzwania.

```bash
curl "http://localhost:8080/api/v1/challenges?page=0&size=20"
```

### GET /api/v1/challenges/upcoming
Pobierz nadchodzące wyzwania.

```bash
curl "http://localhost:8080/api/v1/challenges/upcoming?page=0&size=20"
```

### GET /api/v1/challenges/{challengeId}
Pobierz wyzwanie po ID.

```bash
curl http://localhost:8080/api/v1/challenges/1
```

### GET /api/v1/challenges/my
Pobierz wyzwania użytkownika.

```bash
curl http://localhost:8080/api/v1/challenges/my
```

### GET /api/v1/challenges/search
Szukaj wyzwań.

```bash
curl "http://localhost:8080/api/v1/challenges/search?q=marathon&page=0&size=20"
```

### POST /api/v1/challenges
Utwórz nowe wyzwanie.

```bash
curl -X POST http://localhost:8080/api/v1/challenges \
  -H "Content-Type: application/json" \
  -d '{
    "name": "January Running Challenge",
    "description": "Run 100km in January",
    "challengeType": "DISTANCE",
    "activityType": "RUN",
    "targetValue": 100.0,
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-31T23:59:59",
    "isPublic": true
  }'
```

**Typy wyzwań:** `DISTANCE`, `TIME`, `ELEVATION`, `ACTIVITIES_COUNT`, `SEGMENT`

### PUT /api/v1/challenges/{challengeId}
Zaktualizuj wyzwanie.

```bash
curl -X PUT http://localhost:8080/api/v1/challenges/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Challenge Name",
    "description": "Updated description",
    "challengeType": "DISTANCE",
    "targetValue": 150.0,
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-31T23:59:59"
  }'
```

### DELETE /api/v1/challenges/{challengeId}
Usuń wyzwanie.

```bash
curl -X DELETE http://localhost:8080/api/v1/challenges/1
```

### POST /api/v1/challenges/{challengeId}/participate
Dołącz do wyzwania.

```bash
curl -X POST http://localhost:8080/api/v1/challenges/1/participate
```

### DELETE /api/v1/challenges/{challengeId}/participate
Opuść wyzwanie.

```bash
curl -X DELETE http://localhost:8080/api/v1/challenges/1/participate
```

### GET /api/v1/challenges/{challengeId}/leaderboard
Pobierz ranking wyzwania.

```bash
curl http://localhost:8080/api/v1/challenges/1/leaderboard
```

**Response:**
```json
[
  {
    "userId": 1,
    "userName": "John Doe",
    "avatar": "https://...",
    "progress": 75.5,
    "percentage": 75.5
  }
]
```

---

## 8. Events

**Base Path:** `/api/v1/events`

### GET /api/v1/events
Pobierz nadchodzące publiczne eventy.

```bash
curl "http://localhost:8080/api/v1/events?page=0&size=20"
```

### GET /api/v1/events/{eventId}
Pobierz event po ID.

```bash
curl http://localhost:8080/api/v1/events/1
```

### GET /api/v1/events/my
Pobierz eventy użytkownika.

```bash
curl http://localhost:8080/api/v1/events/my
```

### POST /api/v1/events
Utwórz nowy event.

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warsaw Marathon 2026",
    "description": "Annual Warsaw Marathon",
    "activityType": "RUN",
    "eventDate": "2026-09-15T08:00:00",
    "location": "National Stadium",
    "city": "Warsaw",
    "distance": 42.195,
    "maxParticipants": 10000,
    "isPublic": true
  }'
```

### PUT /api/v1/events/{eventId}
Zaktualizuj event.

```bash
curl -X PUT http://localhost:8080/api/v1/events/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warsaw Marathon 2026 - Updated",
    "description": "Updated description",
    "activityType": "RUN",
    "eventDate": "2026-09-15T08:00:00",
    "location": "National Stadium",
    "city": "Warsaw"
  }'
```

### DELETE /api/v1/events/{eventId}
Usuń event.

```bash
curl -X DELETE http://localhost:8080/api/v1/events/1
```

### POST /api/v1/events/{eventId}/register
Zarejestruj się na event.

```bash
curl -X POST http://localhost:8080/api/v1/events/1/register
```

### DELETE /api/v1/events/{eventId}/register
Wyrejestruj się z eventu.

```bash
curl -X DELETE http://localhost:8080/api/v1/events/1/register
```

---

## 9. Dashboard

**Base Path:** `/api/v1/dashboard`

### GET /api/v1/dashboard
Pobierz dane dashboardu.

```bash
curl http://localhost:8080/api/v1/dashboard
```

**Response:**
```json
{
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  },
  "stats": {
    "totalDistance": 150.5,
    "totalDuration": 54000,
    "totalActivities": 12
  },
  "weeklyStats": {
    "totalDistance": 45.2,
    "dailyStats": [...]
  },
  "recentActivities": [...],
  "weeklyGoal": {...},
  "upcomingEvents": [...]
}
```

---

## 10. Search

**Base Path:** `/api/v1/search`

### GET /api/v1/search
Globalne wyszukiwanie (użytkownicy, aktywności, kluby, wyzwania, eventy).

```bash
curl "http://localhost:8080/api/v1/search?query=marathon&limit=5"
```

**Response:**
```json
{
  "users": [...],
  "activities": [...],
  "clubs": [...],
  "challenges": [...],
  "events": [...]
}
```

### GET /api/v1/search/users
Szukaj użytkowników.

```bash
curl "http://localhost:8080/api/v1/search/users?query=john&limit=10"
```

### GET /api/v1/search/activities
Szukaj aktywności.

```bash
curl "http://localhost:8080/api/v1/search/activities?query=morning%20run&limit=10"
```

### GET /api/v1/search/clubs
Szukaj klubów.

```bash
curl "http://localhost:8080/api/v1/search/clubs?query=runners&limit=10"
```

### GET /api/v1/search/challenges
Szukaj wyzwań.

```bash
curl "http://localhost:8080/api/v1/search/challenges?query=january&limit=10"
```

### GET /api/v1/search/events
Szukaj eventów.

```bash
curl "http://localhost:8080/api/v1/search/events?query=marathon&limit=10"
```

---

## 11. Admin

**Base Path:** `/api/v1/admin`

### GET /api/v1/admin/stats
Pobierz statystyki administracyjne.

```bash
curl http://localhost:8080/api/v1/admin/stats
```

**Response:**
```json
{
  "totalUsers": 150,
  "totalActivities": 1250,
  "totalClubs": 25,
  "totalChallenges": 10,
  "totalEvents": 8,
  "newUsersThisMonth": 15,
  "activeUsersThisWeek": 45
}
```

### GET /api/v1/admin/users
Pobierz listę użytkowników (admin).

```bash
curl "http://localhost:8080/api/v1/admin/users?page=0&size=20"
```

### GET /api/v1/admin/users/{userId}
Pobierz szczegóły użytkownika (admin).

```bash
curl http://localhost:8080/api/v1/admin/users/1
```

### PUT /api/v1/admin/users/{userId}/role
Zmień rolę użytkownika.

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/1/role \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ADMIN"
  }'
```

**Dostępne role:** `USER`, `ADMIN`

### PUT /api/v1/admin/users/{userId}/toggle-active
Przełącz status aktywności użytkownika.

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/1/toggle-active
```

### DELETE /api/v1/admin/users/{userId}
Usuń użytkownika.

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/1
```

---

## 12. Export

**Base Path:** `/api/v1/export`

### GET /api/v1/export/activities/json
Eksportuj aktywności do JSON.

```bash
curl -o activities.json http://localhost:8080/api/v1/export/activities/json
```

### GET /api/v1/export/activities/csv
Eksportuj aktywności do CSV.

```bash
curl -o activities.csv http://localhost:8080/api/v1/export/activities/csv
```

### GET /api/v1/export/activities/txt
Eksportuj aktywności do TXT (raport tekstowy).

```bash
curl -o activities.txt http://localhost:8080/api/v1/export/activities/txt
```

### GET /api/v1/export/activities/gpx/{activityId}
Eksportuj pojedynczą aktywność do GPX.

```bash
curl -o activity.gpx http://localhost:8080/api/v1/export/activities/gpx/1
```

---

## 13. GraphQL

**Endpoint:** `/graphql`
**GraphiQL Interface:** `/graphiql`

### Przykładowe Query

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ currentUser { id firstName lastName email } }"
  }'
```

### Dashboard Query

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ dashboard { user { id firstName lastName } stats { totalDistance totalActivities } recentActivities { id title activityType } } }"
  }'
```

### Activities Query

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ activities(userId: 1) { id title activityType distance duration startTime } }"
  }'
```

### Create Activity Mutation

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { createActivity(input: { title: \"Test Run\", activityType: RUN, distance: 5.0, duration: 1800, startTime: \"2026-01-19T07:00:00\" }) { id title } }"
  }'
```

### Pełna dokumentacja GraphQL

Więcej przykładów znajdziesz w GraphiQL pod adresem: `http://localhost:8080/graphiql`

---

## Kody odpowiedzi

| Kod | Opis |
|-----|------|
| 200 | OK - sukces |
| 201 | Created - zasób utworzony |
| 204 | No Content - sukces bez zwracanej treści |
| 400 | Bad Request - błędne dane wejściowe |
| 401 | Unauthorized - brak autoryzacji |
| 403 | Forbidden - brak uprawnień |
| 404 | Not Found - zasób nie istnieje |
| 409 | Conflict - konflikt (np. duplikat) |
| 500 | Internal Server Error - błąd serwera |

---

## Paginacja

Endpointy z listami obsługują paginację przez parametry:
- `page` - numer strony (od 0)
- `size` - liczba elementów na stronie (domyślnie 10-20)

Nagłówki odpowiedzi:
- `page-total-count` - liczba stron
- `page-total-elements` - łączna liczba elementów

---

## Testowanie w Postman/Insomnia

1. Ustaw Base URL: `http://localhost:8080`
2. Content-Type: `application/json`
3. Dla testowania bez autoryzacji dodaj `?userId=1` do URL
