-- ============================================
-- challenge-goal-service initial schema
--
-- Owns: goals, challenges, challenge_participants.
-- user_id / creator_user_id are logical refs to user-service.users.id —
-- progress data lives in activity-service (called via lb:// + Resilience4j).
-- ============================================

CREATE TABLE goals (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(255) NOT NULL,
    goal_type     VARCHAR(32)  NOT NULL,
    period        VARCHAR(16)  NOT NULL,
    sport_filter  VARCHAR(32),
    target_value  DOUBLE PRECISION NOT NULL,
    period_start  TIMESTAMP    NOT NULL,
    period_end    TIMESTAMP    NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);
CREATE INDEX idx_goals_user ON goals (user_id, period_end);

CREATE TABLE challenges (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(2000),
    goal_type        VARCHAR(32)  NOT NULL,
    sport_filter     VARCHAR(32),
    target_value     DOUBLE PRECISION NOT NULL,
    start_time       TIMESTAMP    NOT NULL,
    end_time         TIMESTAMP    NOT NULL,
    creator_user_id  BIGINT       NOT NULL,
    visibility       VARCHAR(16)  NOT NULL DEFAULT 'PUBLIC',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_challenges_end_time ON challenges (end_time);
CREATE INDEX idx_challenges_creator  ON challenges (creator_user_id);

CREATE TABLE challenge_participants (
    challenge_id  BIGINT    NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
    user_id       BIGINT    NOT NULL,
    joined_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id)
);
CREATE INDEX idx_challenge_participants_user ON challenge_participants (user_id);
