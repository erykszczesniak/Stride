-- Sample users (password is 'password123' encoded with BCrypt)
INSERT INTO users (first_name, last_name, email, password, auth_provider, role, bio, city, country, created_at) VALUES
('Pan', 'Szczesniak', 'pan@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Triathlete and fitness enthusiast', 'Warsaw', 'Poland', NOW()),
('John', 'Doe', 'john@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Runner and cyclist', 'San Francisco', 'USA', NOW()),
('Jane', 'Smith', 'jane@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Marathon runner', 'New York', 'USA', NOW()),
('Admin', 'User', 'admin@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'ADMIN', 'Platform administrator', 'London', 'UK', NOW());

-- Sample followers
INSERT INTO user_followers (user_id, follower_id) VALUES
(1, 2), (1, 3), (2, 1), (2, 3), (3, 1);

-- Sample activities for user 1 (Pan)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Morning Trail Run', 'Beautiful sunrise run through the park. Feeling energized!', 'RUN', 8.5, 2880, 156, 620, 10.625, DATEADD('DAY', -4, NOW()), 1, DATEADD('DAY', -4, NOW())),
('Weekend Long Ride', 'Great coastal route with some challenging climbs.', 'RIDE', 45.2, 7200, 420, 1240, 22.6, DATEADD('DAY', -4, NOW()), 1, DATEADD('DAY', -4, NOW())),
('Recovery Swim', 'Easy pool session to loosen up the muscles.', 'SWIM', 1.5, 2100, 0, 320, 2.57, DATEADD('DAY', -4, NOW()), 1, DATEADD('DAY', -4, NOW())),
('Interval Training', 'Track workout with 8x400m repeats.', 'RUN', 6.2, 1800, 25, 450, 12.4, DATEADD('DAY', -2, NOW()), 1, DATEADD('DAY', -2, NOW())),
('Evening Ride', 'Quick spin around the neighborhood.', 'RIDE', 15.3, 2700, 85, 380, 20.4, DATEADD('DAY', -1, NOW()), 1, DATEADD('DAY', -1, NOW()));

-- Sample activities for user 2 (John)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Golden Gate Run', 'Perfect weather for a bridge run!', 'RUN', 12.0, 3600, 200, 800, 12.0, DATEADD('DAY', -3, NOW()), 2, DATEADD('DAY', -3, NOW())),
('Hill Repeats', 'Tough hill workout in Marin.', 'RIDE', 35.0, 5400, 650, 1100, 23.33, DATEADD('DAY', -2, NOW()), 2, DATEADD('DAY', -2, NOW()));

-- Sample activity likes
INSERT INTO activity_likes (activity_id, user_id) VALUES
(1, 2), (1, 3), (2, 2), (2, 3), (3, 2), (4, 3), (5, 2), (5, 3);

-- Sample comments
INSERT INTO comments (content, activity_id, user_id, created_at) VALUES
('Great pace! Keep it up!', 1, 2, DATEADD('DAY', -3, NOW())),
('That elevation gain is impressive!', 1, 3, DATEADD('DAY', -3, NOW())),
('Beautiful route!', 2, 2, DATEADD('DAY', -3, NOW())),
('Love that coastal road', 2, 3, DATEADD('DAY', -3, NOW())),
('Nice recovery session', 3, 2, DATEADD('DAY', -3, NOW()));

-- Sample clubs
INSERT INTO clubs (name, description, city, country, sport_type, is_private, owner_id, created_at) VALUES
('Warsaw Running Club', 'Community of passionate runners in Warsaw', 'Warsaw', 'Poland', 'RUN', FALSE, 1, NOW()),
('Bay Area Cyclists', 'Cycling group for Bay Area enthusiasts', 'San Francisco', 'USA', 'RIDE', FALSE, 2, NOW()),
('NYC Marathon Training', 'Preparing for the NYC Marathon together', 'New York', 'USA', 'RUN', FALSE, 3, NOW());

-- Sample club members
INSERT INTO club_members (club_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 2),
(3, 3);

-- Sample challenges
INSERT INTO challenges (name, description, challenge_type, activity_type, target_value, start_date, end_date, is_public, creator_id, created_at) VALUES
('January Running Challenge', 'Run 100km this month!', 'DISTANCE', 'RUN', 100.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 1, NOW()),
('Weekly Cycling Challenge', 'Ride 200km this week', 'DISTANCE', 'RIDE', 200.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 2, NOW()),
('Elevation Challenge', 'Climb 5000m this month', 'ELEVATION', NULL, 5000.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 3, NOW());

-- Sample challenge participants
INSERT INTO challenge_participants (challenge_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 2),
(3, 1), (3, 3);

-- Sample events
INSERT INTO events (name, description, activity_type, event_date, location, city, country, distance, max_participants, is_public, organizer_id, created_at) VALUES
('City Marathon', 'Annual city marathon event', 'RUN', DATEADD('DAY', 26, NOW()), 'Downtown San Francisco', 'San Francisco', 'USA', 42.2, 5000, TRUE, 2, NOW()),
('Trail Run Challenge', 'Scenic trail running event in Golden Gate Park', 'RUN', DATEADD('DAY', 45, NOW()), 'Golden Gate Park', 'San Francisco', 'USA', 15.0, 500, TRUE, 2, NOW()),
('Spring Triathlon', 'Sprint distance triathlon', 'OTHER', DATEADD('DAY', 90, NOW()), 'City Beach', 'Warsaw', 'Poland', 25.75, 300, TRUE, 1, NOW());

-- Sample event registrations
INSERT INTO event_participants (event_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 3),
(3, 1);

-- Sample goals for user 1
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', 'RUN', 50.0, 42.8, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 1, NOW()),
('DISTANCE', 'MONTHLY', NULL, 200.0, 75.5, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 1, NOW()),
('ACTIVITIES', 'WEEKLY', NULL, 5.0, 3.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 1, NOW());

-- Sample goals for user 2
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', 'RIDE', 100.0, 35.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 2, NOW()),
('TIME', 'MONTHLY', NULL, 20.0, 8.5, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 2, NOW());
