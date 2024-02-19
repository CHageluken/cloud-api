CREATE TABLE fall_events (
                             wearable_id character varying(255) NOT NULL,
                             timestamp timestamp without time zone NOT NULL,
                             floor_id bigserial,
                             confirmed boolean
);

ALTER TABLE ONLY fall_events
    ADD CONSTRAINT fall_events_pkey PRIMARY KEY (wearable_id, timestamp);

ALTER TABLE ONLY fall_events
    ADD CONSTRAINT FK_fall_events FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE CASCADE;


