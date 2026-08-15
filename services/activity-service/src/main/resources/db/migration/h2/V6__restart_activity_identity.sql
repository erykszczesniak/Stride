-- H2-only counterpart of postgresql/V6: after the explicit-id sample inserts
-- (V3) the identity counter must be moved past the seeded ids, or the next
-- INSERT collides on the primary key. H2 identity columns don't advance on
-- explicit-id inserts and have no setval().
ALTER TABLE activities ALTER COLUMN id RESTART WITH 25;
