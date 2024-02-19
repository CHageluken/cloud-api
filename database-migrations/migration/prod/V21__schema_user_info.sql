CREATE TABLE user_info (
    user_id bigint NOT NULL,
    admission_diagnosis text DEFAULT '',
    secondary_diagnosis text DEFAULT '',
    relevant_medication text DEFAULT '',
    height int,
    weight int,
    age int,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
)