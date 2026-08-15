-- H2 does not advance the identity counter after explicit-id inserts (V3 seed data);
-- restart it so the next registered user gets id 5 instead of colliding with id 1.
ALTER TABLE users ALTER COLUMN id RESTART WITH 5;
