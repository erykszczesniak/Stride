-- Sample activities ported from the monolith's V2__sample_data.sql with the
-- SAME fixed activity ids (1..24) for users 1..4, so the demo dataset matches
-- across services. Likes/comments belong to social-service; club/challenge
-- seeds belong to their owning services.
--
-- INTERVAL '<n>' DAY (SQL-standard qualified form) is used instead of
-- PostgreSQL's INTERVAL '<n> days' so the statements run on both H2
-- (MODE=PostgreSQL) and PostgreSQL. Identity/sequence restart after these
-- explicit-id inserts is vendor-specific and lives in db/migration/h2 and
-- db/migration/postgresql respectively.

-- Sample activities for user 1 (Pan) - spread across this week and last week
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(1, 'Morning Trail Run', 'Beautiful sunrise run through the park. Feeling energized!', 'RUN', 8.5, 2880, 156, 620, 10.625, NOW() - INTERVAL '6' DAY, 1, NOW() - INTERVAL '6' DAY),
(2, 'Weekend Long Ride', 'Great coastal route with some challenging climbs.', 'RIDE', 45.2, 7200, 420, 1240, 22.6, NOW() - INTERVAL '5' DAY, 1, NOW() - INTERVAL '5' DAY),
(3, 'Recovery Swim', 'Easy pool session to loosen up the muscles.', 'SWIM', 1.5, 2100, 0, 320, 2.57, NOW() - INTERVAL '4' DAY, 1, NOW() - INTERVAL '4' DAY),
(4, 'Interval Training', 'High intensity intervals on the track.', 'RUN', 6.0, 1800, 20, 480, 12.0, NOW() - INTERVAL '2' DAY, 1, NOW() - INTERVAL '2' DAY),
(5, 'Evening Ride', 'Relaxing ride along the river after work.', 'RIDE', 22.0, 3600, 80, 550, 22.0, NOW() - INTERVAL '1' DAY, 1, NOW() - INTERVAL '1' DAY),
(6, 'Park Run', 'Saturday morning parkrun with friends.', 'RUN', 5.0, 1500, 15, 380, 12.0, NOW(), 1, NOW());

-- Sample activities for user 2 (John) - spread across this week and last week
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(7, 'Golden Gate Run', 'Perfect weather for a bridge run!', 'RUN', 12.0, 3600, 200, 800, 12.0, NOW() - INTERVAL '5' DAY, 2, NOW() - INTERVAL '5' DAY),
(8, 'Hill Repeats', 'Tough hill workout in Marin.', 'RIDE', 35.0, 5400, 650, 1100, 23.33, NOW() - INTERVAL '4' DAY, 2, NOW() - INTERVAL '4' DAY),
(9, 'Easy Morning Jog', 'Light recovery run along the Embarcadero.', 'RUN', 5.5, 1800, 30, 350, 11.0, NOW() - INTERVAL '3' DAY, 2, NOW() - INTERVAL '3' DAY),
(10, 'Lunch Swim', 'Quick swim session during lunch break.', 'SWIM', 1.0, 1500, 0, 250, 2.4, NOW() - INTERVAL '2' DAY, 2, NOW() - INTERVAL '2' DAY),
(11, 'Bay Trail Ride', 'Long ride around the bay. Great views!', 'RIDE', 52.0, 7200, 320, 1400, 26.0, NOW() - INTERVAL '1' DAY, 2, NOW() - INTERVAL '1' DAY),
(12, 'Morning Speed Work', 'Track session at Kezar Stadium.', 'RUN', 8.0, 2400, 10, 560, 12.0, NOW(), 2, NOW());

-- Sample activities for user 3 (Jane) - spread across this week and last week
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(13, 'Central Park Long Run', 'Marathon training - long run around Central Park.', 'RUN', 32.2, 10800, 180, 2100, 10.73, NOW() - INTERVAL '6' DAY, 3, NOW() - INTERVAL '6' DAY),
(14, 'Tempo Run', 'Threshold workout along Hudson River.', 'RUN', 10.0, 2700, 45, 720, 13.33, NOW() - INTERVAL '4' DAY, 3, NOW() - INTERVAL '4' DAY),
(15, 'Brooklyn Bridge Run', 'Beautiful evening run across the bridge and back.', 'RUN', 8.0, 2400, 60, 550, 12.0, NOW() - INTERVAL '3' DAY, 3, NOW() - INTERVAL '3' DAY),
(16, 'Recovery Walk', 'Active recovery day with a relaxing walk.', 'WALK', 5.0, 3600, 20, 200, 5.0, NOW() - INTERVAL '2' DAY, 3, NOW() - INTERVAL '2' DAY),
(17, 'Speed Intervals', 'Track session - 12x400m at race pace.', 'RUN', 8.5, 2100, 10, 620, 14.57, NOW() - INTERVAL '1' DAY, 3, NOW() - INTERVAL '1' DAY),
(18, 'Prospect Park Loop', 'Easy morning run around Prospect Park.', 'RUN', 6.2, 1980, 35, 440, 11.3, NOW(), 3, NOW()),
(19, 'Hudson River Ride', 'Cycling along the Hudson River Greenway.', 'RIDE', 28.5, 4200, 45, 680, 24.4, NOW() - INTERVAL '1' DAY, 3, NOW() - INTERVAL '1' DAY),
(20, 'Morning Swim', 'Pool session at the Y. Working on technique.', 'SWIM', 2.0, 2700, 0, 420, 2.67, NOW() - INTERVAL '3' DAY, 3, NOW() - INTERVAL '3' DAY);

-- Sample activities for user 4 (Admin) - spread across this week
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(21, 'Thames Path Run', 'Running along the Thames on a foggy morning.', 'RUN', 10.0, 3300, 25, 680, 10.9, NOW() - INTERVAL '3' DAY, 4, NOW() - INTERVAL '3' DAY),
(22, 'Hyde Park Cycle', 'Quick ride through Hyde Park during lunch.', 'RIDE', 15.0, 2400, 35, 400, 22.5, NOW() - INTERVAL '2' DAY, 4, NOW() - INTERVAL '2' DAY),
(23, 'Regent Park Jog', 'Easy afternoon jog through Regent Park.', 'RUN', 7.5, 2400, 15, 520, 11.25, NOW() - INTERVAL '1' DAY, 4, NOW() - INTERVAL '1' DAY),
(24, 'Serpentine Swim', 'Open water swimming in the Serpentine.', 'SWIM', 1.2, 1800, 0, 300, 2.4, NOW(), 4, NOW());

-- Warm-start of the user_cache projection for the sample users seeded by
-- user-service. In production the Kafka listeners keep this fresh; the seed
-- only guarantees the demo dataset renders author names without waiting for
-- events to be replayed.
INSERT INTO user_cache (user_id, first_name, last_name, avatar, updated_at) VALUES
(1, 'Pan', 'Szczesniak', NULL, NOW()),
(2, 'John', 'Doe', NULL, NOW()),
(3, 'Jane', 'Smith', NULL, NOW()),
(4, 'Admin', 'User', NULL, NOW());
