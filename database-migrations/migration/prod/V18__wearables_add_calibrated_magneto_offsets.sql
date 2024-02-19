--Add calibrated magneto offset values for the magnetometer's X and Z axes to the wearables table
ALTER TABLE wearables
    ADD COLUMN magneto_offset_x double precision DEFAULT -1.0,
    ADD COLUMN magneto_offset_z double precision DEFAULT -1.0;