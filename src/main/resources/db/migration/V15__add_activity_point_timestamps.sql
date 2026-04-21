-- Per-point epoch-second timestamps captured at import time from GPX/TCX/FIT trackpoints.
-- Stored as a JSON-serialized array of longs (seconds since 1970-01-01Z), 1:1 aligned
-- with the encoded polyline points. Null for legacy activities and for imports whose
-- source file did not carry per-trackpoint time data — consumers fall back to
-- uniform-pace splits computed from total duration / total distance in that case.
ALTER TABLE activities
    ADD COLUMN point_timestamps TEXT;
