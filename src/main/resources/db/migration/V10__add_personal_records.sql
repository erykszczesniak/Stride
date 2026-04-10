CREATE TABLE personal_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    distance_type VARCHAR(50) NOT NULL,
    time_in_seconds INTEGER NOT NULL,
    distance DOUBLE PRECISION NOT NULL,
    achieved_at TIMESTAMP NOT NULL,
    previous_time_in_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_user_activity_type_distance UNIQUE (user_id, activity_type, distance_type)
);

CREATE INDEX idx_personal_records_user_id ON personal_records(user_id);
CREATE INDEX idx_personal_records_activity_type ON personal_records(activity_type);
CREATE INDEX idx_personal_records_user_activity_type ON personal_records(user_id, activity_type);
