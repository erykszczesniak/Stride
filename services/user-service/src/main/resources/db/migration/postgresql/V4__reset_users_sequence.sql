-- Postgres counterpart of db/migration/h2/V4: advance the BIGSERIAL sequence past the
-- explicit seed ids (V3) so the next registered user gets id 5.
SELECT setval(pg_get_serial_sequence('users', 'id'), 5, false);
