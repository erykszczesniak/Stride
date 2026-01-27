-- Add more sample clubs to match the design

-- First, update existing clubs with more descriptive data
UPDATE clubs SET description = 'Community of passionate runners in Warsaw. Join us for weekly group runs!' WHERE id = 1;
UPDATE clubs SET description = 'Bay Area cycling group. Weekly rides through scenic routes!' WHERE id = 2;
UPDATE clubs SET description = 'Training together for the NYC Marathon. All levels welcome!' WHERE id = 3;

-- Add new clubs with various sport types
INSERT INTO clubs (name, description, city, country, sport_type, is_private, owner_id, created_at) VALUES
('Weekend Warriors', 'For those who train hard on weekends! All sports welcome, great community vibes.', 'Austin', 'USA', NULL, FALSE, 2, NOW()),
('City Cyclists', 'Urban cycling enthusiasts. Weekly rides through the city, all levels welcome!', 'New York', 'USA', 'RIDE', FALSE, 3, NOW()),
('Morning Runners Club', 'Early birds who love to start their day with a run. Join us for 6 AM group runs every weekday!', 'San Francisco', 'USA', 'RUN', FALSE, 2, NOW()),
('Trail Blazers', 'Adventure seekers exploring mountain trails and scenic routes every weekend.', 'Boulder', 'USA', 'HIKE', FALSE, 3, NOW()),
('Triathlon Warriors', 'Train for triathlons with experienced coaches and supportive teammates.', 'San Diego', 'USA', 'OTHER', FALSE, 2, NOW()),
('Swim Squad Elite', 'Competitive swimming team for serious swimmers looking to improve technique and speed.', 'Miami', 'USA', 'SWIM', TRUE, 3, NOW());

-- Add members to new clubs (user 1 joins some, not all)
INSERT INTO club_members (club_id, user_id) VALUES
-- Weekend Warriors (club 4): user 1 is member
(4, 1), (4, 2), (4, 3), (4, 4),
-- City Cyclists (club 5): user 1 is member
(5, 1), (5, 2), (5, 3),
-- Morning Runners Club (club 6): user 1 is NOT member
(6, 2), (6, 3), (6, 4),
-- Trail Blazers (club 7): user 1 is NOT member
(7, 2), (7, 3),
-- Triathlon Warriors (club 8): user 1 is NOT member
(8, 2), (8, 3), (8, 4),
-- Swim Squad Elite (club 9 - private): user 1 is NOT member
(9, 3), (9, 4);
