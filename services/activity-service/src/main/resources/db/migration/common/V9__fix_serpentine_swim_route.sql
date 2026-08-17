-- The seeded "Serpentine Swim" (activity 24) route drifted across Hyde Park's
-- lawns north of the lake. Replace it with a two-lap buoy course inside the
-- western basin of the Serpentine, derived from the OSM water polygon
-- (55 points, ~1.25 km, every point >30 m from the banks / island).
-- The point count is unchanged, so the previously generated point_timestamps
-- and elevation_profile remain aligned 1:1. Content-keyed for idempotence.
UPDATE activities
SET route_polyline = 'qvjyHno`@P_AN}@H{@?eAAcA@eAFcABaADcABcABcADcAAcADbA?bA?dAAbAAbACdAEbAC~@AbA?dAQ`ASz@Uz@P}@P}@L}@?eAAcABcADcADcA@cABcABcA@cAAcABhA?hAChAAhAChAEhAEfA?hAAhAKfAS`AU`AH_@CHAJ'
WHERE id = 24
  AND route_polyline = 'gqjyHrj_@Wr@Sv@Ox@I|@E~@G|@Mz@Yp@a@^e@Te@Tc@Z_@d@]h@[l@Wr@Qv@M|@I|@I|@St@]h@e@Ze@Pc@Tc@^]h@Wp@Ut@Ox@K|@C~@A~@C~@Oz@[j@e@Xe@Hg@Fg@Bg@Ag@Ge@Sc@]]g@[k@Uu@I_AF}@Ru@Zm@\m@Vq@Tu@';
