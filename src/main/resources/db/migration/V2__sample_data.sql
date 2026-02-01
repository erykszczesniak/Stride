-- Sample users (password is 'password123' encoded with BCrypt)
INSERT INTO users (first_name, last_name, email, password, auth_provider, role, bio, city, country, created_at) VALUES
('Pan', 'Szczesniak', 'pan@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Triathlete and fitness enthusiast', 'Warsaw', 'Poland', NOW()),
('John', 'Doe', 'john@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Runner and cyclist', 'San Francisco', 'USA', NOW()),
('Jane', 'Smith', 'jane@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Marathon runner', 'New York', 'USA', NOW()),
('Admin', 'User', 'admin@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'ADMIN', 'Platform administrator', 'London', 'UK', NOW());

-- Sample followers
INSERT INTO user_followers (user_id, follower_id) VALUES
(1, 2), (1, 3), (2, 1), (2, 3), (3, 1), (3, 2), (4, 1), (4, 2), (4, 3);

-- Sample activities for user 1 (Pan)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Morning Trail Run', 'Beautiful sunrise run through the park. Feeling energized!', 'RUN', 8.5, 2880, 156, 620, 10.625, DATEADD('DAY', -4, NOW()), 1, DATEADD('DAY', -4, NOW())),
('Weekend Long Ride', 'Great coastal route with some challenging climbs.', 'RIDE', 45.2, 7200, 420, 1240, 22.6, DATEADD('DAY', -3, NOW()), 1, DATEADD('DAY', -3, NOW())),
('Recovery Swim', 'Easy pool session to loosen up the muscles.', 'SWIM', 1.5, 2100, 0, 320, 2.57, DATEADD('DAY', -2, NOW()), 1, DATEADD('DAY', -2, NOW()));

-- Sample activities for user 2 (John)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Golden Gate Run', 'Perfect weather for a bridge run!', 'RUN', 12.0, 3600, 200, 800, 12.0, DATEADD('DAY', -3, NOW()), 2, DATEADD('DAY', -3, NOW())),
('Hill Repeats', 'Tough hill workout in Marin.', 'RIDE', 35.0, 5400, 650, 1100, 23.33, DATEADD('DAY', -2, NOW()), 2, DATEADD('DAY', -2, NOW())),
('Easy Morning Jog', 'Light recovery run along the Embarcadero.', 'RUN', 5.5, 1800, 30, 350, 11.0, DATEADD('DAY', -1, NOW()), 2, DATEADD('DAY', -1, NOW())),
('Lunch Swim', 'Quick swim session during lunch break.', 'SWIM', 1.0, 1500, 0, 250, 2.4, NOW(), 2, NOW());

-- Sample activities for user 3 (Jane)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Central Park Long Run', 'Marathon training - 20 mile long run around Central Park.', 'RUN', 32.2, 10800, 180, 2100, 10.73, DATEADD('DAY', -5, NOW()), 3, DATEADD('DAY', -5, NOW())),
('Tempo Run', 'Threshold workout along Hudson River.', 'RUN', 10.0, 2700, 45, 720, 13.33, DATEADD('DAY', -3, NOW()), 3, DATEADD('DAY', -3, NOW())),
('Brooklyn Bridge Run', 'Beautiful evening run across the bridge and back.', 'RUN', 8.0, 2400, 60, 550, 12.0, DATEADD('DAY', -2, NOW()), 3, DATEADD('DAY', -2, NOW())),
('Recovery Walk', 'Active recovery day with a relaxing walk.', 'HIKE', 5.0, 3600, 20, 200, 5.0, DATEADD('DAY', -1, NOW()), 3, DATEADD('DAY', -1, NOW())),
('Speed Intervals', 'Track session - 12x400m at race pace.', 'RUN', 8.5, 2100, 10, 620, 14.57, NOW(), 3, NOW());

-- Sample activities for user 4 (Admin)
INSERT INTO activities (title, description, activity_type, distance, duration, elevation_gain, calories, average_speed, start_time, user_id, created_at) VALUES
('Thames Path Run', 'Running along the Thames on a foggy morning.', 'RUN', 10.0, 3300, 25, 680, 10.9, DATEADD('DAY', -2, NOW()), 4, DATEADD('DAY', -2, NOW())),
('Hyde Park Cycle', 'Quick ride through Hyde Park during lunch.', 'RIDE', 15.0, 2400, 35, 400, 22.5, DATEADD('DAY', -1, NOW()), 4, DATEADD('DAY', -1, NOW()));

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
INSERT INTO comments (content, activity_id, user_id, created_at) VALUES
('Great pace! Keep it up!', 1, 2, DATEADD('DAY', -3, NOW())),
('That elevation gain is impressive!', 1, 3, DATEADD('DAY', -3, NOW())),
('Beautiful route!', 2, 2, DATEADD('DAY', -2, NOW())),
('Love that coastal road', 2, 3, DATEADD('DAY', -2, NOW())),
('Nice recovery session', 3, 4, DATEADD('DAY', -1, NOW())),
('The Golden Gate views are amazing!', 4, 1, DATEADD('DAY', -2, NOW())),
('Those hills are brutal!', 5, 1, DATEADD('DAY', -1, NOW())),
('Marathon training is going strong!', 8, 1, DATEADD('DAY', -4, NOW())),
('That is dedication!', 8, 2, DATEADD('DAY', -4, NOW())),
('Keep pushing Jane!', 9, 2, DATEADD('DAY', -2, NOW())),
('Love the Brooklyn Bridge route', 10, 1, DATEADD('DAY', -1, NOW())),
('Fast intervals! Ready for race day!', 12, 1, NOW()),
('Thames Path is beautiful!', 13, 1, DATEADD('DAY', -1, NOW()));

-- Sample clubs
INSERT INTO clubs (name, description, city, country, sport_type, is_private, owner_id, created_at) VALUES
('Warsaw Running Club', 'Community of passionate runners in Warsaw. Join us for weekly group runs!', 'Warsaw', 'Poland', 'RUN', FALSE, 1, NOW()),
('Bay Area Cyclists', 'Bay Area cycling group. Weekly rides through scenic routes!', 'San Francisco', 'USA', 'RIDE', FALSE, 2, NOW()),
('NYC Marathon Training', 'Training together for the NYC Marathon. All levels welcome!', 'New York', 'USA', 'RUN', FALSE, 3, NOW()),
('London Fitness Club', 'Multi-sport community in London', 'London', 'UK', 'OTHER', FALSE, 4, NOW()),
('Weekend Warriors', 'For those who train hard on weekends! All sports welcome, great community vibes.', 'Austin', 'USA', NULL, FALSE, 2, NOW()),
('City Cyclists', 'Urban cycling enthusiasts. Weekly rides through the city, all levels welcome!', 'New York', 'USA', 'RIDE', FALSE, 3, NOW()),
('Morning Runners Club', 'Early birds who love to start their day with a run. Join us for 6 AM group runs every weekday!', 'San Francisco', 'USA', 'RUN', FALSE, 2, NOW()),
('Trail Blazers', 'Adventure seekers exploring mountain trails and scenic routes every weekend.', 'Boulder', 'USA', 'HIKE', FALSE, 3, NOW()),
('Triathlon Warriors', 'Train for triathlons with experienced coaches and supportive teammates.', 'San Diego', 'USA', 'OTHER', FALSE, 2, NOW()),
('Swim Squad Elite', 'Competitive swimming team for serious swimmers looking to improve technique and speed.', 'Miami', 'USA', 'SWIM', TRUE, 3, NOW());

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
INSERT INTO challenges (name, description, challenge_type, activity_type, target_value, start_date, end_date, is_public, creator_id, created_at) VALUES
('January Running Challenge', 'Run 100km this month!', 'DISTANCE', 'RUN', 100.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 1, NOW()),
('Weekly Cycling Challenge', 'Ride 200km this week', 'DISTANCE', 'RIDE', 200.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 2, NOW()),
('Elevation Challenge', 'Climb 5000m this month', 'ELEVATION', NULL, 5000.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 3, NOW()),
('Marathon Training Plan', 'Complete 250km of running this month', 'DISTANCE', 'RUN', 250.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 3, NOW()),
('Active Days Challenge', 'Be active for 20 days this month', 'ACTIVITIES_COUNT', NULL, 20.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 4, NOW());

-- Sample challenge participants
INSERT INTO challenge_participants (challenge_id, user_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 2), (3, 3),
(4, 1), (4, 3),
(5, 1), (5, 2), (5, 3), (5, 4);

-- Sample events
INSERT INTO events (name, description, activity_type, event_date, location, city, country, distance, max_participants, is_public, organizer_id, created_at) VALUES
('Warsaw Half Marathon', 'Annual half marathon through the historic city center', 'RUN', DATEADD('DAY', 30, NOW()), 'Old Town Square', 'Warsaw', 'Poland', 21.1, 3000, TRUE, 1, NOW()),
('City Marathon', 'Annual city marathon event', 'RUN', DATEADD('DAY', 60, NOW()), 'Downtown San Francisco', 'San Francisco', 'USA', 42.2, 5000, TRUE, 2, NOW()),
('NYC Marathon', 'The famous New York City Marathon', 'RUN', DATEADD('DAY', 90, NOW()), 'Staten Island', 'New York', 'USA', 42.2, 50000, TRUE, 3, NOW()),
('Trail Run Challenge', 'Scenic trail running event in Golden Gate Park', 'RUN', DATEADD('DAY', 45, NOW()), 'Golden Gate Park', 'San Francisco', 'USA', 15.0, 500, TRUE, 2, NOW()),
('London Triathlon', 'Olympic distance triathlon in the heart of London', 'OTHER', DATEADD('DAY', 120, NOW()), 'ExCeL London', 'London', 'UK', 51.5, 2000, TRUE, 4, NOW());

-- Sample event registrations
INSERT INTO event_participants (event_id, user_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 1), (2, 2), (2, 4),
(3, 1), (3, 3), (3, 4),
(4, 2), (4, 3),
(5, 1), (5, 4);

-- Sample goals for user 1 (Pan)
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', 'RUN', 50.0, 42.8, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 1, NOW()),
('DISTANCE', 'MONTHLY', NULL, 200.0, 75.5, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 1, NOW()),
('ACTIVITIES', 'WEEKLY', NULL, 5.0, 3.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 1, NOW());

-- Sample goals for user 2 (John)
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', 'RIDE', 100.0, 35.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 2, NOW()),
('TIME', 'MONTHLY', NULL, 20.0, 8.5, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 2, NOW()),
('DISTANCE', 'WEEKLY', 'RUN', 30.0, 17.5, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 2, NOW());

-- Sample goals for user 3 (Jane)
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', 'RUN', 80.0, 63.7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 3, NOW()),
('DISTANCE', 'MONTHLY', 'RUN', 300.0, 180.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 3, NOW()),
('ACTIVITIES', 'WEEKLY', 'RUN', 6.0, 5.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 3, NOW());

-- Sample goals for user 4 (Admin)
INSERT INTO goals (goal_type, period, activity_type, target_value, current_value, start_date, end_date, is_active, user_id, created_at) VALUES
('DISTANCE', 'WEEKLY', NULL, 40.0, 25.0, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE), DATEADD('DAY', 7, DATEADD('DAY', -DAYOFWEEK(NOW())+2, CURRENT_DATE)), TRUE, 4, NOW()),
('ACTIVITIES', 'MONTHLY', NULL, 15.0, 6.0, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE), DATEADD('MONTH', 1, DATEADD('DAY', -DAY(NOW())+1, CURRENT_DATE)), TRUE, 4, NOW());
