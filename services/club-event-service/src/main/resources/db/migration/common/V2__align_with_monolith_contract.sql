-- ============================================
-- Phase 18: align the club/event schema with the monolith's data model so
-- the /api/v1 contract (DTO shapes, sport types, event metadata) matches
-- main exactly.
-- ============================================

-- clubs: the monolith models city/country/sport_type/website plus cover and
-- profile images; "location" was a branch-only free-text field.
ALTER TABLE clubs RENAME COLUMN owner_user_id TO owner_id;
ALTER TABLE clubs DROP COLUMN location;
ALTER TABLE clubs ADD COLUMN cover_image VARCHAR(255);
ALTER TABLE clubs ADD COLUMN profile_image VARCHAR(255);
ALTER TABLE clubs ADD COLUMN city VARCHAR(255);
ALTER TABLE clubs ADD COLUMN country VARCHAR(255);
ALTER TABLE clubs ADD COLUMN sport_type VARCHAR(32);
ALTER TABLE clubs ADD COLUMN website VARCHAR(255);

-- club admins get their own join table like the monolith's club_admins;
-- carry over the roles previously encoded in club_members.role, then drop
-- the column — club_members becomes a plain (club_id, user_id) set.
CREATE TABLE club_admins (
    club_id  BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL,
    PRIMARY KEY (club_id, user_id)
);
INSERT INTO club_admins (club_id, user_id)
    SELECT club_id, user_id FROM club_members WHERE role IN ('OWNER', 'ADMIN');
ALTER TABLE club_members DROP COLUMN role;

-- events: event_type/end_time were branch-only; the monolith models
-- activity_type + a single event_date plus rich metadata (deadline, geo,
-- entry fee, external url, visibility).
ALTER TABLE events RENAME COLUMN organizer_user_id TO organizer_id;
ALTER TABLE events RENAME COLUMN start_time TO event_date;
ALTER TABLE events DROP COLUMN end_time;
ALTER TABLE events DROP COLUMN event_type;
ALTER TABLE events ADD COLUMN activity_type VARCHAR(32);
ALTER TABLE events ADD COLUMN cover_image VARCHAR(255);
ALTER TABLE events ADD COLUMN registration_deadline TIMESTAMP;
ALTER TABLE events ADD COLUMN city VARCHAR(255);
ALTER TABLE events ADD COLUMN country VARCHAR(255);
ALTER TABLE events ADD COLUMN distance DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN entry_fee DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN currency VARCHAR(8);
ALTER TABLE events ADD COLUMN external_url VARCHAR(512);
ALTER TABLE events ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE events SET location = 'Unknown' WHERE location IS NULL;
ALTER TABLE events ALTER COLUMN location SET NOT NULL;

-- event_participants becomes a plain (event_id, user_id) registration set
-- like the monolith's — the status column had no consumer.
ALTER TABLE event_participants DROP COLUMN status;
