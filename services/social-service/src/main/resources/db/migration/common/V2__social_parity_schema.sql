-- ============================================
-- Phase 16: full social parity with the monolith.
--
--   * activity_likes       — mirrors main's V1 activity_likes (minus cross-
--                            schema FKs; activities/users live in other DBs)
--   * comment_reactions    — mirrors main's V4__add_comment_reactions.sql
--   * follow_edges         — local projection of the follow graph, maintained
--                            from user.followed/unfollowed/deleted events
--   * user_feed_entries    — CQRS feed read model, mirrors main's V16
--   * feed_items           — gains title + is_private (needed for public feed
--                            filtering and degraded hydration fallback); loses
--                            the denormalized comment/reaction counters —
--                            counts are now indexed COUNT(*) over the local
--                            comments / activity_likes tables, which cannot
--                            drift under at-least-once event delivery
--   * user_cache           — display_name split into first_name/last_name so
--                            responses can embed main's BaseUserResponse shape
--   * reactions            — dropped; the old KUDOS activity-reaction model is
--                            replaced by main's activity like contract
-- ============================================

-- main's comments.content is VARCHAR(1000)
ALTER TABLE comments ALTER COLUMN content SET DATA TYPE VARCHAR(1000);

-- Activity likes (mirrors main's V1 table, without cross-service FKs)
CREATE TABLE activity_likes (
    activity_id BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id, user_id)
);
CREATE INDEX idx_activity_likes_user ON activity_likes (user_id);

-- Comment reactions (mirrors main's V4, without the users FK)
CREATE TABLE comment_reactions (
    id         BIGSERIAL   PRIMARY KEY,
    emoji      VARCHAR(10) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    comment_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    CONSTRAINT fk_comment_reaction_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_reaction UNIQUE (comment_id, user_id, emoji)
);
CREATE INDEX idx_comment_reactions_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reactions_user ON comment_reactions(user_id);

-- Follow graph projection (recipients for feed fan-out)
CREATE TABLE follow_edges (
    id          BIGSERIAL PRIMARY KEY,
    follower_id BIGINT    NOT NULL,
    followee_id BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_follow_edge UNIQUE (follower_id, followee_id)
);
CREATE INDEX idx_follow_followee ON follow_edges (followee_id);

-- CQRS feed read model (mirrors main's V16, without cross-schema FKs —
-- self-cleaning happens via the activity.deleted / user.deleted consumers)
CREATE TABLE user_feed_entries (
    id                  BIGSERIAL PRIMARY KEY,
    recipient_user_id   BIGINT    NOT NULL,
    activity_id         BIGINT    NOT NULL,
    actor_user_id       BIGINT    NOT NULL,
    activity_created_at TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uniq_user_feed_recipient_activity UNIQUE (recipient_user_id, activity_id)
);
CREATE INDEX idx_user_feed_recipient_created
    ON user_feed_entries (recipient_user_id, activity_created_at DESC);
CREATE INDEX idx_user_feed_recipient_actor
    ON user_feed_entries (recipient_user_id, actor_user_id);

-- feed_items: snapshot columns needed by the public feed + degraded fallback
ALTER TABLE feed_items ADD COLUMN title VARCHAR(255);
ALTER TABLE feed_items ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE feed_items DROP COLUMN comment_count;
ALTER TABLE feed_items DROP COLUMN reaction_count;
CREATE INDEX idx_feed_private_started ON feed_items (is_private, started_at);

-- user_cache: split display_name into first/last (BaseUserResponse shape)
ALTER TABLE user_cache ADD COLUMN first_name VARCHAR(100);
ALTER TABLE user_cache ADD COLUMN last_name VARCHAR(100);
UPDATE user_cache SET
    first_name = CASE WHEN POSITION(' ' IN display_name) > 0
                      THEN SUBSTRING(display_name, 1, POSITION(' ' IN display_name) - 1)
                      ELSE display_name END,
    last_name  = CASE WHEN POSITION(' ' IN display_name) > 0
                      THEN SUBSTRING(display_name, POSITION(' ' IN display_name) + 1)
                      ELSE NULL END;
ALTER TABLE user_cache DROP COLUMN display_name;

-- Old KUDOS reaction model replaced by activity_likes
DROP TABLE reactions;
