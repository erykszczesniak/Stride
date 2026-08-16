-- ============================================
-- ai-service initial schema — mirrors the monolith's V17__add_ai_chat.sql.
-- One conversation per (user, activity); messages are the ordered turns
-- within it. Database-per-service: user_id and activity_id are logical
-- references into user-service / activity-service (no cross-schema FKs),
-- so only the conversation -> messages cascade is a real constraint here.
-- H2 (MODE=PostgreSQL) + PostgreSQL compatible.
-- ============================================

CREATE TABLE ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_ai_conversation_user_activity UNIQUE (user_id, activity_id)
);

CREATE INDEX idx_ai_conversations_user ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_activity ON ai_conversations(activity_id);

CREATE TABLE ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_messages_conversation ON ai_messages(conversation_id);
