-- Adds a timestamp to the users table, which indicates when a user was archived.
-- The field is not a boolean, since we also want to keep users archived for a while, to avoid abusing the feature.
-- Archived users cannot be updated, linked or used for any measurements. Their data is read-only (until unarchived).
ALTER TABLE users
    ADD COLUMN archived_at timestamp without time zone;