CREATE TABLE composite_users (
    id bigserial,
    auth_id character varying(255) NOT NULL,
    UNIQUE (auth_id),
    PRIMARY KEY (id)
);

ALTER TABLE users
    ADD COLUMN composite_user_id bigint,
    ADD CONSTRAINT FK_composite_user FOREIGN KEY (composite_user_id) REFERENCES composite_users(id) ON DELETE SET NULL ON UPDATE CASCADE;