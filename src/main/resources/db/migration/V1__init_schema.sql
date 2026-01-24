-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    avatar VARCHAR(500),
    bio VARCHAR(1000),
    city VARCHAR(100),
    country VARCHAR(100),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    password_reset_code VARCHAR(255),
    password_reset_code_expiry TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- User followers (many-to-many self-referential)
CREATE TABLE user_followers (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, follower_id)
);

-- Activities table
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    activity_type VARCHAR(50) NOT NULL,
    distance DOUBLE PRECISION NOT NULL,
    duration INTEGER NOT NULL,
    elevation_gain INTEGER,
    calories INTEGER,
    average_speed DOUBLE PRECISION,
    max_speed DOUBLE PRECISION,
    average_heart_rate INTEGER,
    max_heart_rate INTEGER,
    average_cadence INTEGER,
    average_power INTEGER,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    start_location VARCHAR(255),
    end_location VARCHAR(255),
    route_polyline TEXT,
    map_image_url VARCHAR(500),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    has_kudos BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Activity likes (many-to-many)
CREATE TABLE activity_likes (
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id, user_id)
);

-- Comments table
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content VARCHAR(1000) NOT NULL,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Clubs table
CREATE TABLE clubs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    cover_image VARCHAR(500),
    profile_image VARCHAR(500),
    city VARCHAR(100),
    country VARCHAR(100),
    sport_type VARCHAR(50),
    website VARCHAR(500),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Club members (many-to-many)
CREATE TABLE club_members (
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (club_id, user_id)
);

-- Club admins (many-to-many)
CREATE TABLE club_admins (
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (club_id, user_id)
);

-- Challenges table
CREATE TABLE challenges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    cover_image VARCHAR(500),
    challenge_type VARCHAR(50) NOT NULL,
    activity_type VARCHAR(50),
    target_value DOUBLE PRECISION NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Challenge participants (many-to-many)
CREATE TABLE challenge_participants (
    challenge_id BIGINT NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id, user_id)
);

-- Events table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    cover_image VARCHAR(500),
    activity_type VARCHAR(50),
    event_date TIMESTAMP NOT NULL,
    registration_deadline TIMESTAMP,
    location VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    distance DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    max_participants INTEGER,
    entry_fee DOUBLE PRECISION,
    currency VARCHAR(10),
    external_url VARCHAR(500),
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    organizer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    club_id BIGINT REFERENCES clubs(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Event participants (many-to-many)
CREATE TABLE event_participants (
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, user_id)
);

-- Goals table
CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    goal_type VARCHAR(50) NOT NULL,
    period VARCHAR(20) NOT NULL,
    activity_type VARCHAR(50),
    target_value DOUBLE PRECISION NOT NULL,
    current_value DOUBLE PRECISION DEFAULT 0,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for better query performance
CREATE INDEX idx_activities_user_id ON activities(user_id);
CREATE INDEX idx_activities_start_time ON activities(start_time);
CREATE INDEX idx_activities_activity_type ON activities(activity_type);
CREATE INDEX idx_comments_activity_id ON comments(activity_id);
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_goals_period ON goals(period);
CREATE INDEX idx_events_event_date ON events(event_date);
CREATE INDEX idx_challenges_start_date ON challenges(start_date);
CREATE INDEX idx_challenges_end_date ON challenges(end_date);
CREATE INDEX idx_users_email ON users(email);
