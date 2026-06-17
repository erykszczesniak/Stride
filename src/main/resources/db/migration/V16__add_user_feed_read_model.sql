-- CQRS read model for the activity feed.
--
-- The write side stays as-is: `activities` is the source of truth. This table
-- is a denormalized projection — one row per (recipient_user, activity) pair —
-- maintained asynchronously by UserFeedProjector in response to domain events.
-- Reading a user's feed becomes a single index scan instead of the previous
-- "WHERE user_id IN (followingIds) ORDER BY created_at DESC" plan that
-- scaled badly with follow-graph size.
--
-- FK ON DELETE CASCADE is intentional: it acts as a safety net so the
-- projection self-cleans on user/activity deletion even if the corresponding
-- domain event is ever lost or fails to apply.

CREATE TABLE user_feed_entries (
    id                   BIGSERIAL    PRIMARY KEY,
    recipient_user_id    BIGINT       NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    activity_id          BIGINT       NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    actor_user_id        BIGINT       NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    activity_created_at  TIMESTAMP    NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uniq_user_feed_recipient_activity UNIQUE (recipient_user_id, activity_id)
);

-- The single covering index for the feed query: ORDER BY activity_created_at DESC
-- within a recipient partition. Postgres will index-scan this without sorting.
CREATE INDEX idx_user_feed_recipient_created
    ON user_feed_entries (recipient_user_id, activity_created_at DESC);

-- Used by the projector's unfollow path and by the by-actor lookups.
CREATE INDEX idx_user_feed_recipient_actor
    ON user_feed_entries (recipient_user_id, actor_user_id);

-- Backfill #1: every user sees their own activities (public AND private) in
-- their own feed. Mirrors the read behavior of the old code path.
INSERT INTO user_feed_entries (recipient_user_id, activity_id, actor_user_id, activity_created_at)
SELECT a.user_id, a.id, a.user_id, a.created_at
FROM activities a;

-- Backfill #2: for each follow relationship, fan out the followee's PUBLIC
-- activities into the follower's feed. Private activities are deliberately
-- excluded — they should never leak to followers even via historical backfill.
INSERT INTO user_feed_entries (recipient_user_id, activity_id, actor_user_id, activity_created_at)
SELECT uf.follower_id, a.id, a.user_id, a.created_at
FROM activities a
JOIN user_followers uf ON uf.user_id = a.user_id
WHERE a.is_private = false
ON CONFLICT (recipient_user_id, activity_id) DO NOTHING;
