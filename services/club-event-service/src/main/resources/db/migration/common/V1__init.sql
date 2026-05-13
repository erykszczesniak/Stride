-- ============================================
-- club-event-service initial schema
--
-- Owns: clubs, club_members, events, event_participants.
-- owner_user_id, user_id, organizer_user_id are logical references to
-- user-service.users.id — no FK constraints across schemas.
-- ============================================

CREATE TABLE clubs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    location        VARCHAR(255),
    owner_user_id   BIGINT       NOT NULL,
    is_private      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX idx_clubs_owner ON clubs (owner_user_id);

CREATE TABLE club_members (
    club_id    BIGINT       NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    user_id    BIGINT       NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    joined_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (club_id, user_id)
);
CREATE INDEX idx_club_members_user ON club_members (user_id);

CREATE TABLE events (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(2000),
    event_type          VARCHAR(32)  NOT NULL,
    start_time          TIMESTAMP    NOT NULL,
    end_time            TIMESTAMP,
    location            VARCHAR(255),
    club_id             BIGINT       REFERENCES clubs(id) ON DELETE SET NULL,
    organizer_user_id   BIGINT       NOT NULL,
    max_participants    INTEGER,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP
);
CREATE INDEX idx_events_start_time ON events (start_time);
CREATE INDEX idx_events_club       ON events (club_id);
CREATE INDEX idx_events_organizer  ON events (organizer_user_id);

CREATE TABLE event_participants (
    event_id   BIGINT       NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT       NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'REGISTERED',
    joined_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, user_id)
);
CREATE INDEX idx_event_participants_user ON event_participants (user_id);
