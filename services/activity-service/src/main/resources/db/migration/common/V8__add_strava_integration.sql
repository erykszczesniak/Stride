-- ============================================
-- Strava integration (mirrors main's V14__add_strava_integration.sql).
-- Deviation forced by database-per-service: user_id columns are logical
-- references to user-service.users.id — no cross-schema FK.
-- ============================================

-- Strava OAuth2 connection for each user.
-- Tokens stored as TEXT (no encryption in this scaffold — see StravaConnection entity).
CREATE TABLE strava_connections (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT    NOT NULL UNIQUE,
    strava_athlete_id BIGINT,
    access_token      TEXT,
    refresh_token     TEXT,
    token_expires_at  TIMESTAMP,
    scope             VARCHAR(255),
    connected_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_imported_at  TIMESTAMP,
    updated_at        TIMESTAMP
);

CREATE INDEX idx_strava_connections_user_id ON strava_connections(user_id);
CREATE INDEX idx_strava_connections_athlete_id ON strava_connections(strava_athlete_id);

-- Track which Strava activities have already been imported so repeated imports are idempotent.
-- Using a separate table keeps the Activity entity clean and avoids a nullable column that's
-- only meaningful for Strava-imported rows.
CREATE TABLE strava_imported_activities (
    strava_activity_id BIGINT    NOT NULL,
    user_id            BIGINT    NOT NULL,
    activity_id        BIGINT    NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    imported_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (strava_activity_id, user_id)
);

CREATE INDEX idx_strava_imported_activities_user_id ON strava_imported_activities(user_id);
CREATE INDEX idx_strava_imported_activities_activity_id ON strava_imported_activities(activity_id);
