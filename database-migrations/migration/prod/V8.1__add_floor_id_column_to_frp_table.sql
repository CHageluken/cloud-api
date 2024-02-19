-- This migration adds a column 'floor_id' that is used to in the foreign key relationship with the Floor entity.
ALTER TABLE fall_risk_profiles
    ADD COLUMN floor_id bigint,
    ADD CONSTRAINT floorfk FOREIGN KEY (floor_id) REFERENCES floors(id) ON UPDATE CASCADE ON DELETE RESTRICT;
