-- Add constraint for gender format

ALTER TABLE user_info_history
    ADD CONSTRAINT user_info_history_gender_format CHECK (gender in ('','m','f','x'));

ALTER TABLE user_info
    ADD CONSTRAINT user_info_gender_format CHECK (gender in ('','m','f','x'));
