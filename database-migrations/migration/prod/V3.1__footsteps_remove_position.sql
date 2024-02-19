-- Forgot to remove 2d (x,y) position fields from footsteps table schema in V2 migration, added as a (this) separate migration instead (as #233 with V2 migration had already been deployed).
ALTER TABLE ONLY footsteps
    DROP COLUMN position_x,
    DROP COLUMN position_y;