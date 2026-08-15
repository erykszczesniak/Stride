-- ============================================
-- Align the notifications table with the monolith's semantics
-- (main: V7__add_notifications.sql). Runs on both Postgres and H2
-- (MODE=PostgreSQL) — only portable statements below.
--
-- Shape change: title/body/metadata/read_at collapse into a single
-- message column; sender identity + reference fields are added.
-- Sender names are denormalized (database-per-service: no users table
-- to join). Enum values are remapped onto main's NotificationType:
--   NEW_FOLLOWER          -> FOLLOW
--   PR_ACHIEVED           -> ACHIEVEMENT
--   CHALLENGE_COMPLETED   -> ACHIEVEMENT
--   REACTION              -> LIKE
--   NEW_ACTIVITY          -> SYSTEM
--   WELCOME               -> dropped (main has no welcome notification)
-- ============================================

DELETE FROM notifications WHERE type = 'WELCOME';

UPDATE notifications SET type = 'FOLLOW'      WHERE type = 'NEW_FOLLOWER';
UPDATE notifications SET type = 'ACHIEVEMENT' WHERE type IN ('PR_ACHIEVED', 'CHALLENGE_COMPLETED');
UPDATE notifications SET type = 'LIKE'        WHERE type = 'REACTION';
UPDATE notifications SET type = 'SYSTEM'      WHERE type = 'NEW_ACTIVITY';

ALTER TABLE notifications RENAME COLUMN recipient_user_id TO recipient_id;
ALTER TABLE notifications RENAME COLUMN read TO is_read;

ALTER TABLE notifications ADD COLUMN sender_id BIGINT;
ALTER TABLE notifications ADD COLUMN sender_first_name VARCHAR(100);
ALTER TABLE notifications ADD COLUMN sender_last_name VARCHAR(100);
ALTER TABLE notifications ADD COLUMN message VARCHAR(500);
ALTER TABLE notifications ADD COLUMN reference_id BIGINT;
ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(50);

-- Preserve legacy rows: fold title + body into the single message column.
UPDATE notifications
SET message = LEFT(
    CASE WHEN body IS NULL OR body = '' THEN title ELSE title || ' ' || body END,
    500);

ALTER TABLE notifications ALTER COLUMN message SET NOT NULL;

ALTER TABLE notifications DROP COLUMN title;
ALTER TABLE notifications DROP COLUMN body;
ALTER TABLE notifications DROP COLUMN metadata;
ALTER TABLE notifications DROP COLUMN read_at;
