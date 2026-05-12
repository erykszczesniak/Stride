-- ============================================
-- activity-service initial schema
-- Owns: activities, personal_records
-- user_id columns are logical references to user-service.users.id;
-- no FK constraint — that would couple the schemas (database-per-service).
-- ============================================

CREATE TABLE activities (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    description           VARCHAR(2000),
    activity_type         VARCHAR(32)  NOT NULL,
    distance              DOUBLE PRECISION NOT NULL,
    duration              INTEGER      NOT NULL,
    elevation_gain        INTEGER,
    calories              INTEGER,
    average_speed         DOUBLE PRECISION,
    max_speed             DOUBLE PRECISION,
    average_heart_rate    INTEGER,
    max_heart_rate        INTEGER,
    start_time            TIMESTAMP    NOT NULL,
    end_time              TIMESTAMP,
    start_location        VARCHAR(255),
    end_location          VARCHAR(255),
    route_polyline        TEXT,
    map_image_url         VARCHAR(500),
    is_private            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

CREATE INDEX idx_activities_user_started ON activities (user_id, start_time);
CREATE INDEX idx_activities_user_sport   ON activities (user_id, activity_type);

CREATE TABLE personal_records (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT      NOT NULL,
    activity_id                 BIGINT      NOT NULL,
    activity_type               VARCHAR(32) NOT NULL,
    distance_type               VARCHAR(32) NOT NULL,
    time_in_seconds             INTEGER     NOT NULL,
    distance                    DOUBLE PRECISION NOT NULL,
    achieved_at                 TIMESTAMP   NOT NULL,
    previous_time_in_seconds    INTEGER,
    created_at                  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    CONSTRAINT uq_user_sport_distance UNIQUE (user_id, activity_type, distance_type)
);

CREATE INDEX idx_pr_user_sport ON personal_records (user_id, activity_type);
