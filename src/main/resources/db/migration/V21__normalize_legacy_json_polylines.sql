-- The V5/V6 sample swims stored raw JSON coordinate arrays in
-- route_polyline; every other row uses Google's encoded format.
-- Normalize the legacy rows so all clients see one format (the web,
-- backend GeoUtils and iOS keep their JSON fallbacks for safety).
-- Content-keyed so the statements are idempotent and no-op on
-- databases where the rows were already converted or never seeded.

UPDATE activities
SET route_polyline = 'sbx}Hs{d_C'
WHERE route_polyline = '[[52.2297,21.0017]]';

UPDATE activities
SET route_polyline = 'clmwFrosbMgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKgEoKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnKfEnK'
WHERE route_polyline = '[[40.7061,-73.9969],[40.7071,-73.9949],[40.7081,-73.9929],[40.7091,-73.9909],[40.7101,-73.9889],[40.7111,-73.9869],[40.7121,-73.9849],[40.7131,-73.9829],[40.7141,-73.9809],[40.7151,-73.9789],[40.7161,-73.9769],[40.7171,-73.9749],[40.7181,-73.9729],[40.7191,-73.9709],[40.7201,-73.9689],[40.7211,-73.9669],[40.7221,-73.9649],[40.7231,-73.9629],[40.7241,-73.9609],[40.7231,-73.9629],[40.7221,-73.9649],[40.7211,-73.9669],[40.7201,-73.9689],[40.7191,-73.9709],[40.7181,-73.9729],[40.7171,-73.9749],[40.7161,-73.9769],[40.7151,-73.9789],[40.7141,-73.9809],[40.7131,-73.9829],[40.7121,-73.9849],[40.7111,-73.9869],[40.7101,-73.9889],[40.7091,-73.9909],[40.7081,-73.9929],[40.7071,-73.9949],[40.7061,-73.9969]]';

UPDATE activities
SET route_polyline = 'km|wF~nnbM'
WHERE route_polyline = '[[40.7831,-73.9712]]';
