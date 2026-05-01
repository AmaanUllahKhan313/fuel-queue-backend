-- Seed fuel stations (Pune / Pimpri-Chinchwad area)
INSERT INTO fuel_stations (name, address, latitude, longitude, geofence_radius_meters, active)
VALUES
  ('HP Petrol Pump Pimpri',          'Pimpri Road, Pimpri',              18.6298, 73.7997, 80.0, TRUE),
  ('Indian Oil Chinchwad',           'Chinchwad Station Road, Pune',     18.6402, 73.8050, 80.0, TRUE),
  ('Bharat Petroleum Akurdi',        'Akurdi, Pune',                     18.6481, 73.7694, 80.0, TRUE),
  ('HP Petrol Pump Nigdi',           'Nigdi Pradhikaran, Pune',          18.6587, 73.7729, 80.0, TRUE),
  ('Indian Oil Wakad',               'Wakad, Pune',                      18.5984, 73.7609, 80.0, TRUE),
  ('Reliance Petrol Pump Hinjewadi', 'Hinjewadi Phase 1, Pune',          18.5912, 73.7389, 80.0, TRUE);
