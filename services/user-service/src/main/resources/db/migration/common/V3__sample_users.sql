-- ============================================
-- Sample users + follow graph, ported from the monolith's V2__sample_data.sql
-- (users / user_followers slice only — activities live in activity-service).
-- The fixed ids 1..4 are load-bearing: activity-service seed data references them.
-- Password for every user is 'password123' (BCrypt).
-- ============================================

INSERT INTO users (id, first_name, last_name, email, password, auth_provider, role, bio, city, country, created_at) VALUES
(1, 'Pan', 'Szczesniak', 'pan@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Triathlete and fitness enthusiast', 'Warsaw', 'Poland', CURRENT_TIMESTAMP),
(2, 'John', 'Doe', 'john@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Runner and cyclist', 'San Francisco', 'USA', CURRENT_TIMESTAMP),
(3, 'Jane', 'Smith', 'jane@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'USER', 'Marathon runner', 'New York', 'USA', CURRENT_TIMESTAMP),
(4, 'Admin', 'User', 'admin@example.com', '$2a$10$v8HMckD4K0YOwPGjMjsPk.MElSqLFH2PQeykM7QOzt4RMcocHSpJm', 'LOCAL', 'ADMIN', 'Platform administrator', 'London', 'UK', CURRENT_TIMESTAMP);

INSERT INTO user_followers (user_id, follower_id) VALUES
(1, 2), (1, 3), (2, 1), (2, 3), (3, 1), (3, 2), (4, 1), (4, 2), (4, 3);

-- Identity/sequence advancement past the explicit ids is vendor-specific and lives in
-- db/migration/h2/V4 (ALTER ... RESTART) and db/migration/postgresql/V4 (setval).
