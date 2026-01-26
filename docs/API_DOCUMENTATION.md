# Strava Clone API Documentation

**Base URL:** `http://localhost:8080`

**Note:** Authorization is temporarily disabled. All endpoints accept an optional `userId` parameter - if not provided, `DEFAULT_USER_ID = 1` is used.

---

## Table of Contents

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
Login user.

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
Register a new user.

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
Refresh JWT token.

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### POST /api/v1/auth/forgot-password
Request password reset.

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

### POST /api/v1/auth/reset-password
Reset password with code.

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
Get current user.

```bash
curl http://localhost:8080/api/v1/users/me

# or with specific userId
curl "http://localhost:8080/api/v1/users/me?userId=2"
```

### GET /api/v1/users/{userId}
Get user by ID.

```bash
curl http://localhost:8080/api/v1/users/1
```

### GET /api/v1/users
Get list of all users (with pagination).

```bash
curl "http://localhost:8080/api/v1/users?page=0&size=20"
```

### PUT /api/v1/users/me
Update user profile.

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
Follow a user.

```bash
curl -X POST "http://localhost:8080/api/v1/users/2/follow?currentUserId=1"
```

### DELETE /api/v1/users/{userId}/follow
Unfollow a user.

```bash
curl -X DELETE "http://localhost:8080/api/v1/users/2/follow?currentUserId=1"
```

### GET /api/v1/users/{userId}/followers
Get user's followers list.

```bash
curl http://localhost:8080/api/v1/users/1/followers
```

### GET /api/v1/users/{userId}/following
Get list of users followed by user.

```bash
curl http://localhost:8080/api/v1/users/1/following
```

### GET /api/v1/users/suggested
Get suggested users to follow. Returns users that the current user is not following yet, sorted by follower count (most popular first).

```bash
curl "http://localhost:8080/api/v1/users/suggested?userId=1&limit=5"
```

**Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | Long | No | 1 | Current user ID |
| limit | int | No | 5 | Maximum number of suggested users |

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

**Suggestion logic:**
- Excludes current user
- Excludes already followed users
- Sorted by follower count (descending)

---

## 3. Activities

**Base Path:** `/api/v1/activities`

### GET /api/v1/activities/{activityId}
Get activity by ID.

```bash
curl http://localhost:8080/api/v1/activities/1
```

### GET /api/v1/activities/feed
Get activity feed (own + followed users).

```bash
curl "http://localhost:8080/api/v1/activities/feed?page=0&size=10"
```

### GET /api/v1/activities/my
Get own activities.

```bash
curl "http://localhost:8080/api/v1/activities/my?page=0&size=10"
```

### GET /api/v1/activities/user/{targetUserId}
Get activities of a specific user.

```bash
curl "http://localhost:8080/api/v1/activities/user/2?page=0&size=10"
```

### GET /api/v1/activities/public
Get public activities.

```bash
curl "http://localhost:8080/api/v1/activities/public?page=0&size=10"
```

### GET /api/v1/activities/by-type
Get activities by type.

```bash
curl "http://localhost:8080/api/v1/activities/by-type?activityType=RUN&page=0&size=10"
```

**Available types:** `RUN`, `RIDE`, `SWIM`, `WALK`, `HIKE`, `WORKOUT`, `YOGA`, `OTHER`, `ALPINE_SKI`, `BACKCOUNTRY_SKI`, `CROSSFIT`, `ELLIPTICAL`, `ICE_SKATE`, `INLINE_SKATE`, `KAYAKING`, `KITESURF`, `NORDIC_SKI`, `ROCK_CLIMBING`, `ROLLER_SKI`, `ROWING`, `SNOWBOARD`, `SNOWSHOE`, `STAIR_STEPPER`, `STAND_UP_PADDLING`, `SURFING`, `VIRTUAL_RIDE`, `VIRTUAL_RUN`, `WEIGHT_TRAINING`, `WINDSURF`

### POST /api/v1/activities
Create a new activity.

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
Update an activity.

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
Delete an activity.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1
```

### POST /api/v1/activities/{activityId}/like
Like an activity.

```bash
curl -X POST http://localhost:8080/api/v1/activities/1/like
```

### DELETE /api/v1/activities/{activityId}/like
Unlike an activity.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1/like
```

### GET /api/v1/activities/stats/monthly
Get monthly statistics.

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
Get weekly statistics.

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
Get activity comments.

```bash
curl http://localhost:8080/api/v1/activities/1/comments
```

### POST /api/v1/activities/{activityId}/comments
Add a comment to an activity.

```bash
curl -X POST http://localhost:8080/api/v1/activities/1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Great run! Keep it up!"
  }'
```

### PUT /api/v1/activities/{activityId}/comments/{commentId}
Update a comment.

```bash
curl -X PUT http://localhost:8080/api/v1/activities/1/comments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Updated comment text"
  }'
```

### DELETE /api/v1/activities/{activityId}/comments/{commentId}
Delete a comment.

```bash
curl -X DELETE http://localhost:8080/api/v1/activities/1/comments/1
```

---

## 5. Goals

**Base Path:** `/api/v1/goals`

### GET /api/v1/goals
Get user's active goals.

```bash
curl http://localhost:8080/api/v1/goals
```

### GET /api/v1/goals/all
Get all user's goals.

```bash
curl http://localhost:8080/api/v1/goals/all
```

### GET /api/v1/goals/{goalId}
Get goal by ID.

```bash
curl http://localhost:8080/api/v1/goals/1
```

### POST /api/v1/goals
Create a new goal.

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

**Goal types:** `DISTANCE`, `TIME`, `ACTIVITIES`, `ELEVATION`
**Periods:** `WEEKLY`, `MONTHLY`, `YEARLY`

### PUT /api/v1/goals/{goalId}
Update a goal.

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
Delete a goal.

```bash
curl -X DELETE http://localhost:8080/api/v1/goals/1
```

### POST /api/v1/goals/progress
Recalculate user's goals progress.

```bash
curl -X POST http://localhost:8080/api/v1/goals/progress
```

---

## 6. Clubs

**Base Path:** `/api/v1/clubs`

### GET /api/v1/clubs
Get list of public clubs.

```bash
curl "http://localhost:8080/api/v1/clubs?page=0&size=20"
```

### GET /api/v1/clubs/{clubId}
Get club by ID.

```bash
curl http://localhost:8080/api/v1/clubs/1
```

### GET /api/v1/clubs/my
Get user's clubs.

```bash
curl http://localhost:8080/api/v1/clubs/my
```

### GET /api/v1/clubs/search
Search clubs.

```bash
curl "http://localhost:8080/api/v1/clubs/search?q=runners&page=0&size=20"
```

### POST /api/v1/clubs
Create a new club.

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
Update a club.

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
Delete a club (owner only).

```bash
curl -X DELETE http://localhost:8080/api/v1/clubs/1
```

### POST /api/v1/clubs/{clubId}/join
Join a club.

```bash
curl -X POST http://localhost:8080/api/v1/clubs/1/join
```

### POST /api/v1/clubs/{clubId}/leave
Leave a club.

```bash
curl -X POST http://localhost:8080/api/v1/clubs/1/leave
```

### GET /api/v1/clubs/{clubId}/members
Get club members.

```bash
curl http://localhost:8080/api/v1/clubs/1/members
```

---

## 7. Challenges

**Base Path:** `/api/v1/challenges`

### GET /api/v1/challenges
Get active challenges.

```bash
curl "http://localhost:8080/api/v1/challenges?page=0&size=20"
```

### GET /api/v1/challenges/upcoming
Get upcoming challenges.

```bash
curl "http://localhost:8080/api/v1/challenges/upcoming?page=0&size=20"
```

### GET /api/v1/challenges/{challengeId}
Get challenge by ID.

```bash
curl http://localhost:8080/api/v1/challenges/1
```

### GET /api/v1/challenges/my
Get user's challenges.

```bash
curl http://localhost:8080/api/v1/challenges/my
```

### GET /api/v1/challenges/search
Search challenges.

```bash
curl "http://localhost:8080/api/v1/challenges/search?q=marathon&page=0&size=20"
```

### POST /api/v1/challenges
Create a new challenge.

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

**Challenge types:** `DISTANCE`, `TIME`, `ELEVATION`, `ACTIVITIES_COUNT`, `SEGMENT`

### PUT /api/v1/challenges/{challengeId}
Update a challenge.

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
Delete a challenge.

```bash
curl -X DELETE http://localhost:8080/api/v1/challenges/1
```

### POST /api/v1/challenges/{challengeId}/participate
Join a challenge.

```bash
curl -X POST http://localhost:8080/api/v1/challenges/1/participate
```

### DELETE /api/v1/challenges/{challengeId}/participate
Leave a challenge.

```bash
curl -X DELETE http://localhost:8080/api/v1/challenges/1/participate
```

### GET /api/v1/challenges/{challengeId}/leaderboard
Get challenge leaderboard.

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
Get upcoming public events.

```bash
curl "http://localhost:8080/api/v1/events?page=0&size=20"
```

### GET /api/v1/events/{eventId}
Get event by ID.

```bash
curl http://localhost:8080/api/v1/events/1
```

### GET /api/v1/events/my
Get user's events.

```bash
curl http://localhost:8080/api/v1/events/my
```

### POST /api/v1/events
Create a new event.

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
Update an event.

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
Delete an event.

```bash
curl -X DELETE http://localhost:8080/api/v1/events/1
```

### POST /api/v1/events/{eventId}/register
Register for an event.

```bash
curl -X POST http://localhost:8080/api/v1/events/1/register
```

### DELETE /api/v1/events/{eventId}/register
Unregister from an event.

```bash
curl -X DELETE http://localhost:8080/api/v1/events/1/register
```

---

## 9. Dashboard

**Base Path:** `/api/v1/dashboard`

### GET /api/v1/dashboard
Get dashboard data.

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
Global search (users, activities, clubs, challenges, events).

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
Search users.

```bash
curl "http://localhost:8080/api/v1/search/users?query=john&limit=10"
```

### GET /api/v1/search/activities
Search activities.

```bash
curl "http://localhost:8080/api/v1/search/activities?query=morning%20run&limit=10"
```

### GET /api/v1/search/clubs
Search clubs.

```bash
curl "http://localhost:8080/api/v1/search/clubs?query=runners&limit=10"
```

### GET /api/v1/search/challenges
Search challenges.

```bash
curl "http://localhost:8080/api/v1/search/challenges?query=january&limit=10"
```

### GET /api/v1/search/events
Search events.

```bash
curl "http://localhost:8080/api/v1/search/events?query=marathon&limit=10"
```

---

## 11. Admin

**Base Path:** `/api/v1/admin`

### GET /api/v1/admin/stats
Get admin statistics.

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
Get user list (admin).

```bash
curl "http://localhost:8080/api/v1/admin/users?page=0&size=20"
```

### GET /api/v1/admin/users/{userId}
Get user details (admin).

```bash
curl http://localhost:8080/api/v1/admin/users/1
```

### PUT /api/v1/admin/users/{userId}/role
Change user role.

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/1/role \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ADMIN"
  }'
```

**Available roles:** `USER`, `ADMIN`

### PUT /api/v1/admin/users/{userId}/toggle-active
Toggle user active status.

```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/1/toggle-active
```

### DELETE /api/v1/admin/users/{userId}
Delete a user.

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/1
```

---

## 12. Export

**Base Path:** `/api/v1/export`

### GET /api/v1/export/activities/json
Export activities to JSON.

```bash
curl -o activities.json http://localhost:8080/api/v1/export/activities/json
```

### GET /api/v1/export/activities/csv
Export activities to CSV.

```bash
curl -o activities.csv http://localhost:8080/api/v1/export/activities/csv
```

### GET /api/v1/export/activities/txt
Export activities to TXT (text report).

```bash
curl -o activities.txt http://localhost:8080/api/v1/export/activities/txt
```

### GET /api/v1/export/activities/gpx/{activityId}
Export single activity to GPX.

```bash
curl -o activity.gpx http://localhost:8080/api/v1/export/activities/gpx/1
```

---

## 13. GraphQL

**Endpoint:** `/graphql`
**GraphiQL Interface:** `/graphiql`

### Example Query

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

### Full GraphQL Documentation

More examples can be found in GraphiQL at: `http://localhost:8080/graphiql`

---

## Response Codes

| Code | Description |
|------|-------------|
| 200 | OK - Success |
| 201 | Created - Resource created |
| 204 | No Content - Success without returned content |
| 400 | Bad Request - Invalid input data |
| 401 | Unauthorized - Not authenticated |
| 403 | Forbidden - No permission |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Conflict (e.g., duplicate) |
| 500 | Internal Server Error - Server error |

---

## Pagination

List endpoints support pagination with parameters:
- `page` - page number (starting from 0)
- `size` - number of elements per page (default 10-20)

Response headers:
- `page-total-count` - total number of pages
- `page-total-elements` - total number of elements

---

## Testing in Postman/Insomnia

1. Set Base URL: `http://localhost:8080`
2. Content-Type: `application/json`
3. For testing without authorization add `?userId=1` to URL
