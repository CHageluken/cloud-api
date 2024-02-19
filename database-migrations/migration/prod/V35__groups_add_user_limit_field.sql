-- Add a field for limiting the number of users a group can have. Leaving the field as 'null' does not set a limit to the amount of users.
ALTER TABLE groups
    ADD COLUMN user_limit int;