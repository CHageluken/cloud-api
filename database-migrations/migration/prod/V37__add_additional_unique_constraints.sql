-- Added as part of VIT-647: these problems were first noticed in this issue. That is, for the user_info table,
-- it was possible to have duplicate entries for the same user_id. This is because the user_id field did not contain
-- a unique constraint at the time.

-- This adds missing uniqueness constraints for child tables that are uniquely identified by (a combination of) their
-- parent table(s) primary key(s).
-- This is done to prevent duplicate entries in the child tables (see above example user_info).

-- Add a unique constraint to the application_access table
ALTER TABLE application_access ADD CONSTRAINT application_access_unique UNIQUE (application_id, user_id);

-- Add a unique constraint to the floor_viewers table
ALTER TABLE floor_viewers ADD CONSTRAINT floor_viewers_unique UNIQUE (floor_id, user_id);