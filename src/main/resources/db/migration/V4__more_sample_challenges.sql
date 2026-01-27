-- Add more sample challenges for Explore tab (user 1 is NOT participating in these)

INSERT INTO challenges (name, description, challenge_type, activity_type, target_value, start_date, end_date, is_public, creator_id, created_at) VALUES
('February Cycling Sprint', 'Cycle 200km next month', 'DISTANCE', 'RIDE', 200.0, DATEADD('DAY', 5, NOW()), DATEADD('DAY', 40, NOW()), TRUE, 2, NOW()),
('Early Bird Challenge', 'Complete 15 workouts before 7 AM', 'ACTIVITIES_COUNT', NULL, 15.0, NOW(), DATEADD('DAY', 30, NOW()), TRUE, 3, NOW()),
('Team Marathon', 'Complete a marathon distance as a team', 'DISTANCE', 'RUN', 42.2, NOW(), DATEADD('DAY', 14, NOW()), TRUE, 2, NOW()),
('Swim a Mile', 'Swim 1 mile total this month', 'DISTANCE', 'SWIM', 1.6, NOW(), DATEADD('DAY', 30, NOW()), TRUE, 3, NOW()),
('Weekend Warrior', 'Complete 8 weekend workouts', 'ACTIVITIES_COUNT', NULL, 8.0, NOW(), DATEADD('DAY', 60, NOW()), TRUE, 2, NOW());

-- Add participants to new challenges (user 1 is NOT included)
-- Only users 2, 3, 4 exist in sample data
INSERT INTO challenge_participants (challenge_id, user_id) VALUES
(4, 2), (4, 3), (4, 4),
(5, 2), (5, 3), (5, 4),
(6, 2), (6, 3), (6, 4),
(7, 3), (7, 4),
(8, 2), (8, 4);
