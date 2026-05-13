-- ============================================
-- social-service initial schema
--
-- Owns:
--   comments, reactions          — write side (CRUD endpoints)
--   feed_items, user_cache       — read side (CQRS projections)
--
-- activity_id and user_id are logical references to ids owned by other
-- services (activity-service, user-service). No FK constraints across
-- schemas — that would couple the schemas, which is exactly what
-- database-per-service is designed to avoid.
-- ============================================

CREATE TABLE comments (
    id           BIGSERIAL PRIMARY KEY,
    activity_id  BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    content      VARCHAR(2000) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP
);
CREATE INDEX idx_comments_activity ON comments (activity_id);
CREATE INDEX idx_comments_user     ON comments (user_id);

CREATE TABLE reactions (
    id             BIGSERIAL PRIMARY KEY,
    activity_id    BIGINT      NOT NULL,
    user_id        BIGINT      NOT NULL,
    reaction_type  VARCHAR(32) NOT NULL DEFAULT 'KUDOS',
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reaction_activity_user_type UNIQUE (activity_id, user_id, reaction_type)
);
CREATE INDEX idx_reactions_activity ON reactions (activity_id);

CREATE TABLE feed_items (
    id                       BIGSERIAL PRIMARY KEY,
    activity_id              BIGINT      NOT NULL,
    user_id                  BIGINT      NOT NULL,
    sport                    VARCHAR(32) NOT NULL,
    distance_meters          DOUBLE PRECISION,
    duration_seconds         BIGINT,
    elevation_gain_meters    DOUBLE PRECISION,
    started_at               TIMESTAMP   NOT NULL,
    comment_count            INTEGER     NOT NULL DEFAULT 0,
    reaction_count           INTEGER     NOT NULL DEFAULT 0,
    occurred_at              TIMESTAMP   NOT NULL,
    CONSTRAINT uq_feed_activity UNIQUE (activity_id)
);
CREATE INDEX idx_feed_started      ON feed_items (started_at);
CREATE INDEX idx_feed_user_started ON feed_items (user_id, started_at);

CREATE TABLE user_cache (
    user_id       BIGINT       PRIMARY KEY,
    display_name  VARCHAR(255) NOT NULL,
    avatar        VARCHAR(500),
    updated_at    TIMESTAMP    NOT NULL
);
