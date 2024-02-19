-- This migration adds a column 'hidden' that is used as a marker/flag field to mark a certain FRP as hidden.
-- This indicates to any application that accesses the FRP that it should not be returned in any result set (by default).
-- By default, we do not mark FRPs as hidden. Marking an FRP as hidden can be useful for certain FRPs that may have been
-- invalid in terms of their underlying footsteps. This can happen when the footstep (data) selection algorithm
-- is not selective enough during the creation of a certain sequence of footsteps for a user.
ALTER TABLE fall_risk_profiles
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;