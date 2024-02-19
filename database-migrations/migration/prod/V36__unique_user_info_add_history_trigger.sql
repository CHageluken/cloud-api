-- Added as part of VIT-647: we noted various issues with updating user info and keeping user info history at the
-- application-level.

-- Make user id unique for user info to avoid having multiple rows for the same user (since it's a 1:0..1 relationship)
ALTER TABLE user_info ADD CONSTRAINT user_info_user_id_unique UNIQUE (user_id);

-- Database trigger that creates a new row in user_info_history when a row in user_info is updated
-- The row in user_info_history contains the previous user info values of the user
-- This avoids issues at the application-level where concurrent requests may overwrite each other's changes before they
-- are saved (as history entries) to the database
-- Downside is that this trigger needs to be updated along with any user_info and user_info_history schema changes
CREATE OR REPLACE FUNCTION user_info_history_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS(SELECT * FROM user_info_history WHERE user_id=NEW.user_id AND end_time=NOW()) THEN
        INSERT INTO user_info_history (user_id, end_time, admission_diagnosis, secondary_diagnosis, relevant_medication, height, weight, age, gender, orthosis, shoes, walking_aid)
        VALUES (OLD.user_id, NOW(), OLD.admission_diagnosis, OLD.secondary_diagnosis, OLD.relevant_medication, OLD.height, OLD.weight, OLD.age, OLD.gender, OLD.orthosis, OLD.shoes, OLD.walking_aid);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_info_history_trigger
AFTER UPDATE ON user_info
FOR EACH ROW
EXECUTE FUNCTION user_info_history_trigger();