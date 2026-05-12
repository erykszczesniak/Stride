-- Postgres-only: case-insensitive search indexes (functional indexes on LOWER()).
-- H2 (used in the standalone smoke-test profile) doesn't support these.

CREATE INDEX idx_users_first_name_lower ON users (LOWER(first_name));
CREATE INDEX idx_users_last_name_lower  ON users (LOWER(last_name));
