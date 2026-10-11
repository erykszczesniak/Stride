// k6 load profile for the Stride monolith, gated by the SLOs in docs/slo.md.
//
// One iteration is the path a signed-in user walks after opening the app:
// profile, my activities, the feed, the dashboard, this week's stats and one
// user search. Read-only on purpose: nothing here grows the database, and the
// AI endpoints (two external providers, their own SLO) are left out.
//
// Thresholds encode the SLOs, so a run that fails them is a release blocker:
//   http_req_failed   rate  < 0.5 %   edge availability 99.5 %
//   http_req_duration p(95) < 500 ms  edge latency: 95 % of requests under 500 ms
//   checks            rate == 100 %   every response had the shape the UI expects
//   feed_duration     p(95) < 500 ms  the slowest single path (see perf/README.md)
//
// Everything is tunable through the environment (see perf/README.md):
//   BASE_URL  VUS  RAMP  HOLD  THINK_TIME  USER_EMAIL  USER_PASSWORD
//
// Run:  docker run --rm --network host -v "$PWD/perf/k6:/scripts" grafana/k6:0.54.0 \
//         run --summary-export /scripts/out/summary.json /scripts/user-journey.js

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const VUS = Number(__ENV.VUS || 10);
const RAMP = __ENV.RAMP || '30s';
const HOLD = __ENV.HOLD || '2m';
const THINK_TIME = Number(__ENV.THINK_TIME || 1); // seconds between iterations, per VU
const USER_EMAIL = __ENV.USER_EMAIL || 'john@example.com';     // V2__sample_data.sql
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password123';

// The feed joins followed users, their activities, likes and comments; it was
// the slowest path in the baseline run (perf/README.md), so it gets its own
// trend and its own SLO-level threshold instead of hiding behind the fast
// endpoints in the overall p95.
export const feedDuration = new Trend('feed_duration', true);

export const options = {
  scenarios: {
    user_journey: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP, target: VUS },
        { duration: HOLD, target: VUS },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // docs/slo.md — edge availability 99.5 %: fewer than 0.5 % of requests may fail
    http_req_failed: ['rate<0.005'],
    // docs/slo.md — edge latency: 95 % of requests under 500 ms
    http_req_duration: ['p(95)<500'],
    // every check passed; a 200 with the wrong body still counts as a failure
    checks: ['rate==1'],
    // the slowest single path must meet the SLO on its own, not hide behind fast ones
    feed_duration: ['p(95)<500'],
    // per-endpoint p95 (the `name` tag), so the summary shows which path is slow
    'http_req_duration{name:users_me}': ['p(95)<500'],
    'http_req_duration{name:activities_my}': ['p(95)<500'],
    'http_req_duration{name:activities_feed}': ['p(95)<500'],
    'http_req_duration{name:dashboard}': ['p(95)<500'],
    'http_req_duration{name:stats_weekly}': ['p(95)<500'],
    'http_req_duration{name:users_search}': ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// Login once for the whole run: /api/v1/auth/login is rate-limited to 10
// requests per minute per IP, so per-VU logins would be throttled above
// 10 VUs. The access token lives 30 minutes, which bounds RAMP + HOLD.
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: USER_EMAIL, password: USER_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  );
  const ok = check(res, {
    'login: 200': (r) => r.status === 200,
    'login: access token present': (r) => !!r.json('accessToken'),
  });
  if (!ok) {
    throw new Error(`login failed for ${USER_EMAIL} against ${BASE_URL}: HTTP ${res.status} ${res.body}`);
  }
  return { token: res.json('accessToken') };
}

function get(path, name, token) {
  return http.get(`${BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name },
  });
}

export default function (data) {
  const token = data.token;

  group('profile', () => {
    const res = get('/api/v1/users/me', 'users_me', token);
    check(res, {
      'me: 200': (r) => r.status === 200,
      'me: has email': (r) => r.json('email') === USER_EMAIL,
    });
  });

  group('my activities', () => {
    const res = get('/api/v1/activities/my?page=0&size=10', 'activities_my', token);
    check(res, {
      'my activities: 200': (r) => r.status === 200,
      'my activities: array': (r) => Array.isArray(r.json()),
    });
  });

  group('feed', () => {
    const res = get('/api/v1/activities/feed?page=0&size=10', 'activities_feed', token);
    feedDuration.add(res.timings.duration);
    check(res, {
      'feed: 200': (r) => r.status === 200,
      'feed: array': (r) => Array.isArray(r.json()),
    });
  });

  group('dashboard', () => {
    const res = get('/api/v1/dashboard', 'dashboard', token);
    check(res, {
      'dashboard: 200': (r) => r.status === 200,
      'dashboard: json object': (r) => typeof r.json() === 'object' && r.json() !== null,
    });
  });

  group('weekly stats', () => {
    const res = get('/api/v1/activities/stats/weekly', 'stats_weekly', token);
    check(res, {
      'weekly stats: 200': (r) => r.status === 200,
    });
  });

  group('user search', () => {
    const res = get('/api/v1/users/search?query=john', 'users_search', token);
    check(res, {
      'search: 200': (r) => r.status === 200,
    });
  });

  sleep(THINK_TIME);
}
