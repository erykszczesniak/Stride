-- ============================================
-- Gear tracking (mirrors main's V13__add_gear.sql).
-- Deviation forced by database-per-service: gear.user_id is a logical
-- reference to user-service.users.id — no cross-schema FK.
-- All statements are H2(MODE=PostgreSQL) + PostgreSQL compatible.
-- ============================================

CREATE TABLE gear (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    name              VARCHAR(255) NOT NULL,
    brand             VARCHAR(255),
    model             VARCHAR(255),
    type              VARCHAR(50)  NOT NULL,
    distance_km_total DOUBLE PRECISION NOT NULL DEFAULT 0,
    activity_count    INTEGER      NOT NULL DEFAULT 0,
    retired           BOOLEAN      NOT NULL DEFAULT FALSE,
    retired_at        TIMESTAMP,
    notes             VARCHAR(2000),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gear_user_id ON gear(user_id);
CREATE INDEX idx_gear_user_retired ON gear(user_id, retired);

CREATE TABLE gear_activity_types (
    gear_id       BIGINT      NOT NULL REFERENCES gear(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    CONSTRAINT pk_gear_activity_types PRIMARY KEY (gear_id, activity_type)
);

CREATE INDEX idx_gear_activity_types_gear ON gear_activity_types(gear_id);

-- Tie activities to optional gear. Nullable: not every activity has a gear tagged.
-- ON DELETE SET NULL so deleting a gear record doesn't cascade-wipe activity history.
-- (Split into two statements — H2 doesn't accept the combined
-- "ADD COLUMN ..., ADD CONSTRAINT ..." form PostgreSQL allows.)
ALTER TABLE activities ADD COLUMN gear_id BIGINT;

ALTER TABLE activities
    ADD CONSTRAINT fk_activities_gear
        FOREIGN KEY (gear_id) REFERENCES gear(id) ON DELETE SET NULL;

CREATE INDEX idx_activities_gear_id ON activities(gear_id);
