-- Sample users (password is 'password123' encoded with BCrypt)
INSERT INTO users (id, first_name, last_name, email, password, auth_provider, role, bio, city, country, created_at) VALUES
(1, 'Pan', 'Szczesniak', 'pan@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Triathlete and fitness enthusiast', 'Warsaw', 'Poland', NOW()),
(2, 'John', 'Doe', 'john@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Runner and cyclist', 'San Francisco', 'USA', NOW()),
(3, 'Jane', 'Smith', 'jane@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Marathon runner', 'New York', 'USA', NOW()),
(4, 'Admin', 'User', 'admin@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'ADMIN', 'Platform administrator', 'London', 'UK', NOW());

-- Reset the sequence to continue after our manual IDs
SELECT setval('users_id_seq', 4);

-- Sample followers
INSERT INTO user_followers (user_id, follower_id) VALUES
(1, 2), (1, 3), (2, 1), (2, 3), (3, 1), (3, 2), (4, 1), (4, 2), (4, 3);

-- Sample activities for user 1 (Pan)
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(1, 'Morning Trail Run', 'Beautiful sunrise run through the park. Feeling energized!', 'RUN', 8.5, 2880, 156, 620, 10.625, NOW() - INTERVAL '4 days', 1, NOW() - INTERVAL '4 days'),
(2, 'Weekend Long Ride', 'Great coastal route with some challenging climbs.', 'RIDE', 45.2, 7200, 420, 1240, 22.6, NOW() - INTERVAL '3 days', 1, NOW() - INTERVAL '3 days'),
(3, 'Recovery Swim', 'Easy pool session to loosen up the muscles.', 'SWIM', 1.5, 2100, 0, 320, 2.57, NOW() - INTERVAL '2 days', 1, NOW() - INTERVAL '2 days');

-- Sample activities for user 2 (John)
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(4, 'Golden Gate Run', 'Perfect weather for a bridge run!', 'RUN', 12.0, 3600, 200, 800, 12.0, NOW() - INTERVAL '3 days', 2, NOW() - INTERVAL '3 days'),
(5, 'Hill Repeats', 'Tough hill workout in Marin.', 'RIDE', 35.0, 5400, 650, 1100, 23.33, NOW() - INTERVAL '2 days', 2, NOW() - INTERVAL '2 days'),
(6, 'Easy Morning Jog', 'Light recovery run along the Embarcadero.', 'RUN', 5.5, 1800, 30, 350, 11.0, NOW() - INTERVAL '1 day', 2, NOW() - INTERVAL '1 day'),
(7, 'Lunch Swim', 'Quick swim session during lunch break.', 'SWIM', 1.0, 1500, 0, 250, 2.4, NOW(), 2, NOW());

-- Sample activities for user 3 (Jane)
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(8, 'Central Park Long Run', 'Marathon training - 20 mile long run around Central Park.', 'RUN', 32.2, 10800, 180, 2100, 10.73, NOW() - INTERVAL '5 days', 3, NOW() - INTERVAL '5 days'),
(9, 'Tempo Run', 'Threshold workout along Hudson River.', 'RUN', 10.0, 2700, 45, 720, 13.33, NOW() - INTERVAL '3 days', 3, NOW() - INTERVAL '3 days'),
(10, 'Brooklyn Bridge Run', 'Beautiful evening run across the bridge and back.', 'RUN', 8.0, 2400, 60, 550, 12.0, NOW() - INTERVAL '2 days', 3, NOW() - INTERVAL '2 days'),
(11, 'Recovery Walk', 'Active recovery day with a relaxing walk.', 'HIKE', 5.0, 3600, 20, 200, 5.0, NOW() - INTERVAL '1 day', 3, NOW() - INTERVAL '1 day'),
(12, 'Speed Intervals', 'Track session - 12x400m at race pace.', 'RUN', 8.5, 2100, 10, 620, 14.57, NOW(), 3, NOW());

-- Sample activities for user 4 (Admin)
INSERT INTO activities (id, title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
(13, 'Thames Path Run', 'Running along the Thames on a foggy morning.', 'RUN', 10.0, 3300, 25, 680, 10.9, NOW() - INTERVAL '2 days', 4, NOW() - INTERVAL '2 days'),
(14, 'Hyde Park Cycle', 'Quick ride through Hyde Park during lunch.', 'RIDE', 15.0, 2400, 35, 400, 22.5, NOW() - INTERVAL '1 day', 4, NOW() - INTERVAL '1 day');

-- Reset the sequence
SELECT setval('activities_id_seq', 14);

-- Sample activity likes
INSERT INTO activity_likes (activity_id, user_id) VALUES
(1, 2), (1, 3), (1, 4),
(2, 2), (2, 3),
(3, 2), (3, 4),
(4, 1), (4, 3), (4, 4),
(5, 1), (5, 3),
(6, 1), (6, 4),
(7, 1), (7, 3),
(8, 1), (8, 2), (8, 4),
(9, 1), (9, 2),
(10, 1), (10, 2), (10, 4),
(11, 2), (11, 4),
(12, 1), (12, 2),
(13, 1), (13, 2), (13, 3),
(14, 1), (14, 3);

-- Sample comments
INSERT INTO comments (id, content, activity_id, user_id, created_at) VALUES
(1, 'Great pace! Keep it up!', 1, 2, NOW() - INTERVAL '3 days'),
(2, 'That elevation gain is impressive!', 1, 3, NOW() - INTERVAL '3 days'),
(3, 'Beautiful route!', 2, 2, NOW() - INTERVAL '2 days'),
(4, 'Love that coastal road', 2, 3, NOW() - INTERVAL '2 days'),
(5, 'Nice recovery session', 3, 4, NOW() - INTERVAL '1 day'),
(6, 'The Golden Gate views are amazing!', 4, 1, NOW() - INTERVAL '2 days'),
(7, 'Those hills are brutal!', 5, 1, NOW() - INTERVAL '1 day'),
(8, 'Marathon training is going strong!', 8, 1, NOW() - INTERVAL '4 days'),
(9, 'That is dedication!', 8, 2, NOW() - INTERVAL '4 days'),
(10, 'Keep pushing Jane!', 9, 2, NOW() - INTERVAL '2 days'),
(11, 'Love the Brooklyn Bridge route', 10, 1, NOW() - INTERVAL '1 day'),
(12, 'Fast intervals! Ready for race day!', 12, 1, NOW()),
(13, 'Thames Path is beautiful!', 13, 1, NOW() - INTERVAL '1 day');

SELECT setval('comments_id_seq', 13);

-- Sample clubs
INSERT INTO clubs (id, name, description, city, country, sport_type, is_private, owner_id, created_at) VALUES
(1, 'Warsaw Running Club', 'Community of passionate runners in Warsaw. Join us for weekly group runs!', 'Warsaw', 'Poland', 'RUN', FALSE, 1, NOW()),
(2, 'Bay Area Cyclists', 'Bay Area cycling group. Weekly rides through scenic routes!', 'San Francisco', 'USA', 'RIDE', FALSE, 2, NOW()),
(3, 'NYC Marathon Training', 'Training together for the NYC Marathon. All levels welcome!', 'New York', 'USA', 'RUN', FALSE, 3, NOW()),
(4, 'London Fitness Club', 'Multi-sport community in London', 'London', 'UK', 'OTHER', FALSE, 4, NOW()),
(5, 'Weekend Warriors', 'For those who train hard on weekends! All sports welcome, great community vibes.', 'Austin', 'USA', NULL, FALSE, 2, NOW()),
(6, 'City Cyclists', 'Urban cycling enthusiasts. Weekly rides through the city, all levels welcome!', 'New York', 'USA', 'RIDE', FALSE, 3, NOW()),
(7, 'Morning Runners Club', 'Early birds who love to start their day with a run. Join us for 6 AM group runs every weekday!', 'San Francisco', 'USA', 'RUN', FALSE, 2, NOW()),
(8, 'Trail Blazers', 'Adventure seekers exploring mountain trails and scenic routes every weekend.', 'Boulder', 'USA', 'HIKE', FALSE, 3, NOW()),
(9, 'Triathlon Warriors', 'Train for triathlons with experienced coaches and supportive teammates.', 'San Diego', 'USA', 'OTHER', FALSE, 2, NOW()),
(10, 'Swim Squad Elite', 'Competitive swimming team for serious swimmers looking to improve technique and speed.', 'Miami', 'USA', 'SWIM', TRUE, 3, NOW());

SELECT setval('clubs_id_seq', 10);

-- Sample club members
INSERT INTO club_members (club_id, user_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 3), (3, 4),
(4, 2), (4, 3), (4, 4),
(5, 1), (5, 2), (5, 3), (5, 4),
(6, 1), (6, 2), (6, 3),
(7, 2), (7, 3), (7, 4),
(8, 2), (8, 3),
(9, 2), (9, 3), (9, 4),
(10, 3), (10, 4);

-- Sample challenges
INSERT INTO challenges (id, name, description, challenge_type, activity_type, target_value, start_date, end_date, is_public, creator_id, created_at) VALUES
(1, 'January Running Challenge', 'Run 100km this month!', 'DISTANCE', 'RUN', 100.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 1, NOW()),
(2, 'Weekly Cycling Challenge', 'Ride 200km this week', 'DISTANCE', 'RIDE', 200.0, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 2, NOW()),
(3, 'Elevation Challenge', 'Climb 5000m this month', 'ELEVATION', NULL, 5000.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 3, NOW()),
(4, 'Marathon Training Plan', 'Complete 250km of running this month', 'DISTANCE', 'RUN', 250.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 3, NOW()),
(5, 'Active Days Challenge', 'Be active for 20 days this month', 'ACTIVITIES_COUNT', NULL, 20.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 4, NOW());

SELECT setval('challenges_id_seq', 5);

-- Sample challenge participants
INSERT INTO challenge_participants (challenge_id, user_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 2), (3, 3),
(4, 1), (4, 3),
(5, 1), (5, 2), (5, 3), (5, 4);

-- Sample events
INSERT INTO events (id, name, description, activity_type, event_date, location, city, country, distance, max_participants, is_public, organizer_id, created_at) VALUES
(1, 'Warsaw Half Marathon', 'Annual half marathon through the historic city center', 'RUN', NOW() + INTERVAL '30 days', 'Old Town Square', 'Warsaw', 'Poland', 21.1, 3000, TRUE, 1, NOW()),
(2, 'City Marathon', 'Annual city marathon event', 'RUN', NOW() + INTERVAL '60 days', 'Downtown San Francisco', 'San Francisco', 'USA', 42.2, 5000, TRUE, 2, NOW()),
(3, 'NYC Marathon', 'The famous New York City Marathon', 'RUN', NOW() + INTERVAL '90 days', 'Staten Island', 'New York', 'USA', 42.2, 50000, TRUE, 3, NOW()),
(4, 'Trail Run Challenge', 'Scenic trail running event in Golden Gate Park', 'RUN', NOW() + INTERVAL '45 days', 'Golden Gate Park', 'San Francisco', 'USA', 15.0, 500, TRUE, 2, NOW()),
(5, 'London Triathlon', 'Olympic distance triathlon in the heart of London', 'OTHER', NOW() + INTERVAL '120 days', 'ExCeL London', 'London', 'UK', 51.5, 2000, TRUE, 4, NOW());

SELECT setval('events_id_seq', 5);

-- Sample event registrations
INSERT INTO event_participants (event_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 3), (3, 4),
(4, 2), (4, 3),
(5, 1), (5, 4);

-- Sample goals for user 1 (Pan)
INSERT INTO goals (id, goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
(1, 'DISTANCE', 'WEEKLY', 'RUN', 50.0, 42.8, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 1, NOW()),
(2, 'DISTANCE', 'MONTHLY', NULL, 200.0, 75.5, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 1, NOW()),
(3, 'ACTIVITIES', 'WEEKLY', NULL, 5.0, 3.0, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 1, NOW());

-- Sample goals for user 2 (John)
INSERT INTO goals (id, goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
(4, 'DISTANCE', 'WEEKLY', 'RIDE', 100.0, 35.0, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 2, NOW()),
(5, 'TIME', 'MONTHLY', NULL, 20.0, 8.5, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 2, NOW()),
(6, 'DISTANCE', 'WEEKLY', 'RUN', 30.0, 17.5, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 2, NOW());

-- Sample goals for user 3 (Jane)
INSERT INTO goals (id, goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
(7, 'DISTANCE', 'WEEKLY', 'RUN', 80.0, 63.7, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 3, NOW()),
(8, 'DISTANCE', 'MONTHLY', 'RUN', 300.0, 180.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 3, NOW()),
(9, 'ACTIVITIES', 'WEEKLY', 'RUN', 6.0, 5.0, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 3, NOW());

-- Sample goals for user 4 (Admin)
INSERT INTO goals (id, goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
(10, 'DISTANCE', 'WEEKLY', NULL, 40.0, 25.0, DATE_TRUNC('week', CURRENT_DATE), DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days', TRUE, 4, NOW()),
(11, 'ACTIVITIES', 'MONTHLY', NULL, 15.0, 6.0, DATE_TRUNC('month', CURRENT_DATE), DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month', TRUE, 4, NOW());

SELECT setval('goals_id_seq', 11);
