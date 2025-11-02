-- Dodanie kolumn latitude i longitude dla adresów odbioru i dostawy w harmonogramach
ALTER TABLE schedules
ADD COLUMN pickup_latitude DOUBLE PRECISION,
ADD COLUMN pickup_longitude DOUBLE PRECISION,
ADD COLUMN dropoff_latitude DOUBLE PRECISION,
ADD COLUMN dropoff_longitude DOUBLE PRECISION;

-- Dodanie indeksów dla wydajniejszych zapytań przestrzennych (opcjonalne)
CREATE INDEX idx_schedules_pickup_location ON schedules(pickup_latitude, pickup_longitude) WHERE pickup_latitude IS NOT NULL;
CREATE INDEX idx_schedules_dropoff_location ON schedules(dropoff_latitude, dropoff_longitude) WHERE dropoff_latitude IS NOT NULL;

-- Komentarze do kolumn
COMMENT ON COLUMN schedules.pickup_latitude IS 'Szerokość geograficzna punktu odbioru';
COMMENT ON COLUMN schedules.pickup_longitude IS 'Długość geograficzna punktu odbioru';
COMMENT ON COLUMN schedules.dropoff_latitude IS 'Szerokość geograficzna punktu dostawy';
COMMENT ON COLUMN schedules.dropoff_longitude IS 'Długość geograficzna punktu dostawy';