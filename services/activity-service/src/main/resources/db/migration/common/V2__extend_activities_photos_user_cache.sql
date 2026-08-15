-- ============================================
-- Parity with the monolith's activities schema:
--   * missing metric columns (cadence, power, kudos flag)
--   * per-point GPS streams (elevation_profile, point_timestamps)
--   * activity_photos element collection
--   * user_cache — local denormalized projection of user identity data,
--     maintained by Kafka listeners on user.registered/updated/deleted so
--     activity responses can embed the author without calling user-service
--     on the read path.
-- ============================================

ALTER TABLE activities ADD COLUMN average_cadence INTEGER;
ALTER TABLE activities ADD COLUMN average_power INTEGER;
ALTER TABLE activities ADD COLUMN has_kudos BOOLEAN NOT NULL DEFAULT FALSE;

-- JSON-serialized array of doubles (meters), one per polyline point.
-- Null when the source had no altitude data.
ALTER TABLE activities ADD COLUMN elevation_profile TEXT;

-- JSON-serialized array of longs (epoch seconds), 1:1 aligned with the
-- polyline points. Null when the source had no per-trackpoint time data —
-- consumers fall back to uniform-pace splits.
ALTER TABLE activities ADD COLUMN point_timestamps TEXT;

CREATE TABLE activity_photos (
    activity_id BIGINT       NOT NULL,
    photo_url   VARCHAR(500) NOT NULL,
    CONSTRAINT fk_activity_photos_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_photos_activity_id ON activity_photos (activity_id);

-- user_id is a logical reference to user-service.users.id; no FK — that
-- would couple the schemas (database-per-service).
CREATE TABLE user_cache (
    user_id    BIGINT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    avatar     VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
