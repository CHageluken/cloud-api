CREATE TABLE floor_viewers (
    user_id bigint NOT NULL,
    floor_id bigint NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON UPDATE CASCADE ON DELETE CASCADE
)