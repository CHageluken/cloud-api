-- Rotation field allows a floor to specify how it should be rotated (in degrees) in clients
ALTER TABLE ONLY floors
    ADD COLUMN rotation double precision DEFAULT 0.0;

-- Set default value for orientationNorth field
ALTER TABLE only floors
    ALTER COLUMN orientation_north SET DEFAULT 0.0;
