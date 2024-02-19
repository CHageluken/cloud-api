-- Add a field for limiting the number of users a tenant can have. Leaving the field as 'null' does not set a limit to the amount of users.
ALTER TABLE tenants
    ADD COLUMN user_limit int;