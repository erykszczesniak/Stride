-- Postgres-only counterpart of h2/V6: continue the activities id sequence
-- after the fixed sample ids inserted by V3.
SELECT setval('activities_id_seq', 24);
