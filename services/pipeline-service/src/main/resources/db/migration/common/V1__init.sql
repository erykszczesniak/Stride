-- ============================================
-- pipeline-service initial schema
--
-- Owns: pipeline_tokens (mirrors main's V18__add_pipeline_tokens.sql).
-- user_id is a logical ref to user-service.users.id — no FK, the user
-- lives in another service's database.
--
-- Personal API tokens for the read-only pipeline facade (/pipeline/v1/**).
-- Tokens are long-lived bearer credentials issued per user (format: tri_<random>).
-- Only a SHA-256 hash is stored; the plaintext is shown once at creation.
-- The suffix (last 4 chars) is kept for display ("••••••••7f2a") in token lists.
-- ============================================

CREATE TABLE pipeline_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    token_suffix VARCHAR(8)   NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_pipeline_tokens_user_id ON pipeline_tokens(user_id);
