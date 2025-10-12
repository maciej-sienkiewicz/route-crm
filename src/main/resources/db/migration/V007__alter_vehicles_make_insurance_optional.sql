-- V007__alter_vehicles_make_insurance_optional.sql

-- Make insurance fields nullable
ALTER TABLE vehicles
    ALTER COLUMN insurance_policy_number DROP NOT NULL;

ALTER TABLE vehicles
    ALTER COLUMN insurance_valid_until DROP NOT NULL;

ALTER TABLE vehicles
    ALTER COLUMN insurance_insurer DROP NOT NULL;

-- Make technical inspection fields nullable
ALTER TABLE vehicles
    ALTER COLUMN technical_inspection_valid_until DROP NOT NULL;

ALTER TABLE vehicles
    ALTER COLUMN technical_inspection_station DROP NOT NULL;

COMMENT ON COLUMN vehicles.insurance_policy_number IS 'Insurance policy number (optional - vehicle can be registered without insurance documents)';
COMMENT ON COLUMN vehicles.technical_inspection_valid_until IS 'Technical inspection valid until date (optional - allows for registration before inspection)';