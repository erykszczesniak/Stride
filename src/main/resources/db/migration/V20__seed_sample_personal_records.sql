-- Seed personal records for the sample users so the records surfaces (web PR
-- page, pipeline /records) have data out of the box. Times mirror what
-- PersonalRecordService.recalculateAllPRs() derives from the V2 activities
-- (largest-fitting distance per activity at its average pace), including the
-- previous-best values produced by chronological improvement, so a manual
-- recalculate yields the same rows. achieved_at copies the source activity's
-- start_time (V2 seeds those relative to NOW()).

INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 1, 4, 'RUN', 'FIVE_KM', 1500, 5.0, start_time, 1694, NOW() FROM activities WHERE id = 4
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 1, 2, 'RIDE', 'TEN_KM', 1593, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 2
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 1, 3, 'SWIM', 'ONE_KM', 1400, 1.0, start_time, NULL, NOW() FROM activities WHERE id = 3
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;

INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 2, 7, 'RUN', 'TEN_KM', 3000, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 7
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 2, 12, 'RUN', 'FIVE_KM', 1500, 5.0, start_time, 1636, NOW() FROM activities WHERE id = 12
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 2, 11, 'RIDE', 'TEN_KM', 1385, 10.0, start_time, 1543, NOW() FROM activities WHERE id = 11
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 2, 10, 'SWIM', 'ONE_KM', 1500, 1.0, start_time, NULL, NOW() FROM activities WHERE id = 10
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;

INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 13, 'RUN', 'HALF_MARATHON', 7076, 21.0975, start_time, NULL, NOW() FROM activities WHERE id = 13
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 14, 'RUN', 'TEN_KM', 2700, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 14
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 17, 'RUN', 'FIVE_KM', 1235, 5.0, start_time, 1500, NOW() FROM activities WHERE id = 17
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 16, 'WALK', 'FIVE_KM', 3600, 5.0, start_time, NULL, NOW() FROM activities WHERE id = 16
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 19, 'RIDE', 'TEN_KM', 1474, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 19
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 3, 20, 'SWIM', 'ONE_KM', 1350, 1.0, start_time, NULL, NOW() FROM activities WHERE id = 20
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;

INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 4, 21, 'RUN', 'TEN_KM', 3300, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 21
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 4, 23, 'RUN', 'FIVE_KM', 1600, 5.0, start_time, NULL, NOW() FROM activities WHERE id = 23
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 4, 22, 'RIDE', 'TEN_KM', 1600, 10.0, start_time, NULL, NOW() FROM activities WHERE id = 22
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
INSERT INTO personal_records (user_id, activity_id, activity_type, distance_type, time_in_seconds, distance, achieved_at, previous_time_in_seconds, created_at)
SELECT 4, 24, 'SWIM', 'ONE_KM', 1500, 1.0, start_time, NULL, NOW() FROM activities WHERE id = 24
ON CONFLICT (user_id, activity_type, distance_type) DO NOTHING;
