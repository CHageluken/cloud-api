-- Defines a table for storing user info upon update. The table contains all fields of the user_info table,
-- with the addition of a timestamp end_time.

CREATE TABLE user_info_history (
    user_id bigint NOT NULL,
    end_time timestamp NOT NULL,
    admission_diagnosis text DEFAULT '',
    secondary_diagnosis text DEFAULT '',
    relevant_medication text DEFAULT '',
    height int,
    weight int,
    age int,
    gender TEXT DEFAULT '',
    orthosis TEXT DEFAULT '',
    shoes TEXT DEFAULT '',
    walking_aid TEXT DEFAULT ''
);

ALTER TABLE ONLY user_info_history
    ADD CONSTRAINT user_info_history_pkey PRIMARY KEY (user_id, end_time);

ALTER TABLE ONLY user_info_history
    ADD CONSTRAINT FK_user_info_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;