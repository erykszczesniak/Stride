-- ============================================
-- notification-service initial schema
-- Owns: notifications. recipient_user_id is a logical reference to
-- user-service.users.id; no FK across schemas (database-per-service).
-- ============================================

CREATE TABLE notifications (
    id                  BIGSERIAL PRIMARY KEY,
    recipient_user_id   BIGINT       NOT NULL,
    type                VARCHAR(32)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    body                VARCHAR(2000),
    metadata            TEXT,
    read                BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_user_id, created_at);
CREATE INDEX idx_notifications_recipient_unread  ON notifications (recipient_user_id, read);
