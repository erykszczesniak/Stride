-- Add privacy settings columns to users table
ALTER TABLE users
    ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN hide_from_search BOOLEAN NOT NULL DEFAULT FALSE;
