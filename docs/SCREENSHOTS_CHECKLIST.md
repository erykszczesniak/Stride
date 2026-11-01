# Screenshots

All images live in `docs/screenshots/` and are embedded in the README.
They were captured from the Docker Compose stack with the sample accounts
(`john@example.com` and `admin@example.com`, password `password123`).

## Captured

| File | Route | Shows |
|---|---|---|
| `login.png` | `/login` | Sign-in form and Strava OAuth button |
| `register.png` | `/register` | Registration form with password strength |
| `dashboard.png` | `/` | Stats, recent activities, weekly goals, upcoming events |
| `activities.png` | `/activities` | Activity feed with filters and search |
| `record-activity.png` | `/activities/new` | Manual entry and file import |
| `activity-details.png` | `/activities/:id` | Route map, splits, elevation, likes and comments |
| `profile.png` | `/profile` | Athlete overview and statistics |
| `profile-personal-records.png` | `/profile` | Personal Records tab |
| `goals.png` | `/goals` | Goal progress and deadlines |
| `challenges.png` | `/challenges` | Challenges and leaderboards |
| `training-plans.png` | `/training` | Templates and AI-generated plans |
| `reports.png` | `/reports` | Weekly report and PDF export |
| `clubs.png` | `/clubs` | Club discovery and membership |
| `events.png` | `/events` | Event list and registration |
| `create-event.png` | `/events/new` | Event creation form |
| `settings-profile.png` | `/settings` | Profile settings |
| `settings-preferences.png` | `/settings` | Units, language and theme |
| `settings-gear.png` | `/settings` | Gear tracking |
| `dark-mode.png` | `/` | Dashboard in dark mode |
| `activity-details-dark.png` | `/activities/:id` | Activity details in dark mode |
| `mobile-view.png` | `/` | Dashboard at phone width |
| `admin-overview.png` | `/admin` | Platform statistics and activities |
| `admin-users.png` | `/admin` | Users tab |
| `admin-export.png` | `/admin` | Export tab |

## How to refresh

1. Start the stack: `cp .env.example .env && docker compose up -d --build`.
2. Open `http://localhost` in a 1920x1080 window and sign in with a sample account.
3. Capture the viewport only, without the browser frame.
4. Scale images to 1600 px wide and keep the file names above.
