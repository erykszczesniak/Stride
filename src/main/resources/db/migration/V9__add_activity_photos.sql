CREATE TABLE activity_photos (
    activity_id BIGINT NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_activity_photos_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_photos_activity_id ON activity_photos(activity_id);
