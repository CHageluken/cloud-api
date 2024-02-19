-- This migration adds a column 'notes' that is used as a field for adding notes with additional user information.
-- See issue #408 for more information.
ALTER TABLE users
    ADD COLUMN notes TEXT DEFAULT '';
