-- Per-point elevation series captured at import time from GPX/TCX/FIT trackpoints.
-- Stored as a JSON-serialized array of doubles (meters). Null for legacy activities
-- and anything imported without altitude data — the frontend hides the chart
-- gracefully when this is null or has fewer than 2 samples.
ALTER TABLE activities
    ADD COLUMN elevation_profile TEXT;

-- Seed a hand-made hill-shaped elevation profile on one sample activity so the
-- chart is visible locally without needing to import a GPX file. 20 samples of
-- a gentle climb up to ~185m and back down, on the "Weekend Long Ride"
-- (id=2, 45.2 km). This is dev-only data; V12 runs after V2 so the row exists.
UPDATE activities
SET elevation_profile = '[110.0, 118.0, 127.0, 138.0, 149.0, 158.0, 165.0, 172.0, 178.0, 183.0, 185.0, 182.0, 176.0, 168.0, 159.0, 148.0, 136.0, 124.0, 115.0, 108.0]'
WHERE id = 2;
