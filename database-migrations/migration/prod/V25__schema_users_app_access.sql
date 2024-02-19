CREATE TABLE applications (
    id bigserial,
    name text NOT NULL,
    priority int NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name),
    UNIQUE (priority)
);

CREATE TABLE application_access (
   application_id bigint NOT NULL,
   user_id bigint NOT NULL,
   FOREIGN KEY (application_id) REFERENCES applications(id) ON UPDATE CASCADE ON DELETE CASCADE,
   FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO applications (name, priority) VALUES ('frp', 1);
INSERT INTO applications (name, priority) VALUES('rehabilitation', 2);
INSERT INTO applications (name, priority) VALUES('activity', 3);