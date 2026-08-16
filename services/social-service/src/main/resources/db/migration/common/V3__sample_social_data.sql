-- ============================================
-- Demo dataset, ported from the monolith's V2__sample_data.sql (social slice)
-- so the microservices stack serves the same sample product:
--
--   * user_cache        — warm-start for users 1..4 (names from user-service's
--                         V3__sample_users.sql; avatars are NULL in main too)
--   * feed_items        — snapshot of the 24 activities seeded by
--                         activity-service's V3__sample_activities.sql (same
--                         ids/titles/sports/privacy; distance km -> meters,
--                         matching the ActivityCreatedEvent projection)
--   * activity_likes    — the like matrix from main's V2 sample data
--   * comments          — the 15 sample comments (ids left to the identity
--                         column: nothing references comment ids across seeds)
--   * follow_edges      — main's user_followers rows (user_followers.user_id
--                         is the followee, follower_id the follower)
--   * user_feed_entries — backfill mirroring main's V16: every author sees
--                         their own activities; followers get the PUBLIC ones
--
-- INTERVAL '<n>' DAY (SQL-standard qualified form) is used so the statements
-- run on both H2 (MODE=PostgreSQL) and PostgreSQL. In production the Kafka
-- consumers keep all of this fresh; the seeds only guarantee the demo dataset
-- is present without replaying events.
-- ============================================

INSERT INTO user_cache (user_id, first_name, last_name, avatar, updated_at) VALUES
(1, 'Pan', 'Szczesniak', NULL, CURRENT_TIMESTAMP),
(2, 'John', 'Doe', NULL, CURRENT_TIMESTAMP),
(3, 'Jane', 'Smith', NULL, CURRENT_TIMESTAMP),
(4, 'Admin', 'User', NULL, CURRENT_TIMESTAMP);

-- Snapshot of activity-service's 24 sample activities (all public there).
-- distance_meters = seed km * 1000; occurred_at mirrors the activity's created_at.
INSERT INTO feed_items (activity_id, user_id, sport, title, is_private, distance_meters, duration_seconds, elevation_gain_meters, started_at, occurred_at) VALUES
(1,  1, 'RUN',  'Morning Trail Run',      FALSE, 8500,  2880,  156, NOW() - INTERVAL '6' DAY, NOW() - INTERVAL '6' DAY),
(2,  1, 'RIDE', 'Weekend Long Ride',      FALSE, 45200, 7200,  420, NOW() - INTERVAL '5' DAY, NOW() - INTERVAL '5' DAY),
(3,  1, 'SWIM', 'Recovery Swim',          FALSE, 1500,  2100,  0,   NOW() - INTERVAL '4' DAY, NOW() - INTERVAL '4' DAY),
(4,  1, 'RUN',  'Interval Training',      FALSE, 6000,  1800,  20,  NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY),
(5,  1, 'RIDE', 'Evening Ride',           FALSE, 22000, 3600,  80,  NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY),
(6,  1, 'RUN',  'Park Run',               FALSE, 5000,  1500,  15,  NOW(),                    NOW()),
(7,  2, 'RUN',  'Golden Gate Run',        FALSE, 12000, 3600,  200, NOW() - INTERVAL '5' DAY, NOW() - INTERVAL '5' DAY),
(8,  2, 'RIDE', 'Hill Repeats',           FALSE, 35000, 5400,  650, NOW() - INTERVAL '4' DAY, NOW() - INTERVAL '4' DAY),
(9,  2, 'RUN',  'Easy Morning Jog',       FALSE, 5500,  1800,  30,  NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY),
(10, 2, 'SWIM', 'Lunch Swim',             FALSE, 1000,  1500,  0,   NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY),
(11, 2, 'RIDE', 'Bay Trail Ride',         FALSE, 52000, 7200,  320, NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY),
(12, 2, 'RUN',  'Morning Speed Work',     FALSE, 8000,  2400,  10,  NOW(),                    NOW()),
(13, 3, 'RUN',  'Central Park Long Run',  FALSE, 32200, 10800, 180, NOW() - INTERVAL '6' DAY, NOW() - INTERVAL '6' DAY),
(14, 3, 'RUN',  'Tempo Run',              FALSE, 10000, 2700,  45,  NOW() - INTERVAL '4' DAY, NOW() - INTERVAL '4' DAY),
(15, 3, 'RUN',  'Brooklyn Bridge Run',    FALSE, 8000,  2400,  60,  NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY),
(16, 3, 'WALK', 'Recovery Walk',          FALSE, 5000,  3600,  20,  NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY),
(17, 3, 'RUN',  'Speed Intervals',        FALSE, 8500,  2100,  10,  NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY),
(18, 3, 'RUN',  'Prospect Park Loop',     FALSE, 6200,  1980,  35,  NOW(),                    NOW()),
(19, 3, 'RIDE', 'Hudson River Ride',      FALSE, 28500, 4200,  45,  NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY),
(20, 3, 'SWIM', 'Morning Swim',           FALSE, 2000,  2700,  0,   NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY),
(21, 4, 'RUN',  'Thames Path Run',        FALSE, 10000, 3300,  25,  NOW() - INTERVAL '3' DAY, NOW() - INTERVAL '3' DAY),
(22, 4, 'RIDE', 'Hyde Park Cycle',        FALSE, 15000, 2400,  35,  NOW() - INTERVAL '2' DAY, NOW() - INTERVAL '2' DAY),
(23, 4, 'RUN',  'Regent Park Jog',        FALSE, 7500,  2400,  15,  NOW() - INTERVAL '1' DAY, NOW() - INTERVAL '1' DAY),
(24, 4, 'SWIM', 'Serpentine Swim',        FALSE, 1200,  1800,  0,   NOW(),                    NOW());

-- Sample activity likes (main's V2 matrix, verbatim)
INSERT INTO activity_likes (activity_id, user_id) VALUES
(1, 2), (1, 3), (1, 4),
(2, 2), (2, 3),
(3, 2), (3, 4),
(4, 2), (4, 3),
(5, 2), (5, 3),
(6, 2), (6, 3),
(7, 1), (7, 3), (7, 4),
(8, 1), (8, 3),
(9, 1), (9, 4),
(10, 1), (10, 3),
(11, 1), (11, 3),
(12, 1), (12, 3),
(13, 1), (13, 2), (13, 4),
(14, 1), (14, 2),
(15, 1), (15, 2), (15, 4),
(16, 2), (16, 4),
(17, 1), (17, 2),
(18, 1), (18, 2),
(19, 1), (19, 2),
(20, 1), (20, 4),
(21, 1), (21, 2), (21, 3),
(22, 1), (22, 3),
(23, 1), (23, 2),
(24, 2), (24, 3);

-- Sample comments (main's V2, without explicit ids — the identity column
-- assigns 1..15 on a fresh schema anyway, and nothing references comment ids)
INSERT INTO comments (content, activity_id, user_id, created_at) VALUES
('Great pace! Keep it up!', 1, 2, NOW() - INTERVAL '5' DAY),
('That elevation gain is impressive!', 1, 3, NOW() - INTERVAL '5' DAY),
('Beautiful route!', 2, 2, NOW() - INTERVAL '4' DAY),
('Love that coastal road', 2, 3, NOW() - INTERVAL '4' DAY),
('Nice recovery session', 3, 4, NOW() - INTERVAL '3' DAY),
('The Golden Gate views are amazing!', 7, 1, NOW() - INTERVAL '4' DAY),
('Those hills are brutal!', 8, 1, NOW() - INTERVAL '3' DAY),
('Marathon training is going strong!', 13, 1, NOW() - INTERVAL '5' DAY),
('That is dedication!', 13, 2, NOW() - INTERVAL '5' DAY),
('Keep pushing Jane!', 14, 2, NOW() - INTERVAL '3' DAY),
('Love the Brooklyn Bridge route', 15, 1, NOW() - INTERVAL '2' DAY),
('Fast intervals! Ready for race day!', 17, 1, NOW() - INTERVAL '1' DAY),
('Thames Path is beautiful!', 21, 1, NOW() - INTERVAL '2' DAY),
('Great ride along the bay!', 11, 1, NOW() - INTERVAL '1' DAY),
('Nice Prospect Park loop!', 18, 2, NOW());

-- Follow graph from user-service's V3__sample_users.sql: user_followers
-- (user_id, follower_id) means follower_id follows user_id.
INSERT INTO follow_edges (follower_id, followee_id) VALUES
(2, 1), (3, 1),
(1, 2), (3, 2),
(1, 3), (2, 3),
(1, 4), (2, 4), (3, 4);

-- Feed backfill #1 (mirrors main's V16): every author sees their own
-- activities — public AND private — in their own feed.
INSERT INTO user_feed_entries (recipient_user_id, activity_id, actor_user_id, activity_created_at)
SELECT f.user_id, f.activity_id, f.user_id, f.occurred_at
FROM feed_items f;

-- Feed backfill #2 (mirrors main's V16): fan each followee's PUBLIC
-- activities out to their followers. No ON CONFLICT needed: follow_edges
-- contains no self-follows, so these rows are disjoint from backfill #1.
INSERT INTO user_feed_entries (recipient_user_id, activity_id, actor_user_id, activity_created_at)
SELECT fe.follower_id, f.activity_id, f.user_id, f.occurred_at
FROM feed_items f
JOIN follow_edges fe ON fe.followee_id = f.user_id
WHERE f.is_private = FALSE;
