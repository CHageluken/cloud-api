-- Add a 'details' column to the user_measurements table.
-- This column will be used to store (optional) detailed, additional measurement information with the main measurement value.
-- For example: a POMA measurement may have a full POMA-B and POMA-G assessment, which can be stored as part of this column.
-- We use a default value of an empty JSON object (instead of NULL) to indicate that some user measurements do not have details.
ALTER TABLE user_measurements
    ADD COLUMN IF NOT EXISTS details jsonb default '{}'::jsonb;