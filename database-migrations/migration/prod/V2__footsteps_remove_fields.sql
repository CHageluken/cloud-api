-- Drop fields that are no longer included in footstep events.
ALTER TABLE footsteps
DROP COLUMN acceleration_avg_x,
DROP COLUMN acceleration_extreme_x,
DROP COLUMN acceleration_avg_y,
DROP COLUMN acceleration_extreme_y,
DROP COLUMN acceleration_avg_z,
DROP COLUMN acceleration_extreme_z,
DROP COLUMN acceleration_rms_z,
DROP COLUMN airtime,
DROP COLUMN orientation_x,
DROP COLUMN orientation_y,
DROP COLUMN orientation_z,
DROP COLUMN orientation_heading;


