CREATE TABLE user_measurement_types (
    id bigserial,
    name text NOT NULL,
    description text DEFAULT '',
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE user_measurements (
    id bigserial,
    type_id int NOT NULL,
    user_id bigint NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    recorded_by bigint NOT NULL,
    created_at timestamp without time zone DEFAULT NOW(),
    value float NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (type_id) REFERENCES test_types(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

INSERT INTO user_measurement_types (name, description) VALUES ('POMA', 'Performance-Oriented Mobility Assessment score.');