-- ============================================
-- user-service initial schema
-- Owns: identity, profiles, follow graph
-- ============================================

CREATE TABLE users (
    id                          BIGSERIAL PRIMARY KEY,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password                    VARCHAR(255),
    avatar                      VARCHAR(500),
    bio                         VARCHAR(1000),
    city                        VARCHAR(100),
    country                     VARCHAR(100),
    weight                      DOUBLE PRECISION,
    height                      DOUBLE PRECISION,
    auth_provider               VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id                 VARCHAR(255),
    role                        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    password_reset_code         VARCHAR(255),
    password_reset_code_expiry  TIMESTAMP,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    email_notifications         BOOLEAN      NOT NULL DEFAULT TRUE,
    push_notifications          BOOLEAN      NOT NULL DEFAULT TRUE,
    weekly_summary              BOOLEAN      NOT NULL DEFAULT TRUE,
    is_private                  BOOLEAN      NOT NULL DEFAULT FALSE,
    hide_from_search            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP
);

-- email already gets a unique index from the UNIQUE constraint above.
-- Case-insensitive functional indexes on first_name / last_name live in a
-- Postgres-only follow-up migration (V2) so the H2 standalone profile (used
-- for fast local smoke tests) can still bootstrap.
CREATE INDEX idx_users_active ON users (active);

CREATE TABLE user_followers (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, follower_id),
    CHECK (user_id <> follower_id)
);

CREATE INDEX idx_user_followers_follower ON user_followers (follower_id);
