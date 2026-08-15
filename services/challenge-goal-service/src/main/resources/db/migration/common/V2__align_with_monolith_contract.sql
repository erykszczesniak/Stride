-- ============================================
-- Phase 18: align the challenge/goal schema with the monolith's data model
-- so the /api/v1 contract (DTO shapes, enum values, computed progress)
-- matches main exactly.
-- ============================================

-- goals: title/sport_filter/period_* were a branch-only shape; the monolith
-- models goals as (goal_type, period, activity_type, target/current value,
-- start/end date, is_active).
ALTER TABLE goals DROP COLUMN title;
ALTER TABLE goals RENAME COLUMN sport_filter TO activity_type;
ALTER TABLE goals RENAME COLUMN period_start TO start_date;
ALTER TABLE goals RENAME COLUMN period_end TO end_date;
ALTER TABLE goals RENAME COLUMN active TO is_active;
ALTER TABLE goals ADD COLUMN current_value DOUBLE PRECISION;
UPDATE goals SET current_value = 0;

-- challenges: goal_type → challenge_type (monolith enum has ACTIVITIES_COUNT
-- instead of ACTIVITIES), visibility enum → is_public boolean, plus the
-- cover image and updated_at columns the monolith exposes.
ALTER TABLE challenges RENAME COLUMN goal_type TO challenge_type;
UPDATE challenges SET challenge_type = 'ACTIVITIES_COUNT' WHERE challenge_type = 'ACTIVITIES';
ALTER TABLE challenges RENAME COLUMN sport_filter TO activity_type;
ALTER TABLE challenges RENAME COLUMN start_time TO start_date;
ALTER TABLE challenges RENAME COLUMN end_time TO end_date;
ALTER TABLE challenges RENAME COLUMN creator_user_id TO creator_id;
ALTER TABLE challenges ADD COLUMN cover_image VARCHAR(255);
ALTER TABLE challenges ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE challenges SET is_public = (visibility = 'PUBLIC');
ALTER TABLE challenges DROP COLUMN visibility;
ALTER TABLE challenges ADD COLUMN updated_at TIMESTAMP;

-- challenge_participants keeps its (challenge_id, user_id) shape — it now
-- backs an @ElementCollection of participant user ids.
