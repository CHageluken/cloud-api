-- Migrate notes field from user_info to users table
ALTER TABLE user_info ADD COLUMN notes text;
UPDATE user_info SET notes = users.notes FROM users WHERE user_info.user_id = users.id;
ALTER TABLE users DROP COLUMN notes;