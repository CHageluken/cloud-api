-- This migration adds a column 'notes' that is used as a field for marking FRPs with notes such as '4m' for example.
-- Can be used to indicate whether the FRP originated from the "regular" continuous footstep segmentation or if
-- they were added by some other process.
ALTER TABLE fall_risk_profiles
    ADD COLUMN notes TEXT DEFAULT '';