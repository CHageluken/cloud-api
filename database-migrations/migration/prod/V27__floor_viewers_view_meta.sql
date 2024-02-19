-- This boolean flag field indicates whether a user is allowed to view "metadata" about a floor.
-- An example of this metadata is tag information generated from walking on the floor (combined with RSSI values).
ALTER TABLE floor_viewers
    ADD COLUMN view_metadata boolean DEFAULT false;