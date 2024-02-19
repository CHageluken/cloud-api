CREATE TABLE fall_risk_profiles (
    id bigserial,
    wearable_id character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT NOW(),
    begin_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    walking_speed double precision NOT NULL,
    step_length double precision NOT NULL,
    step_frequency double precision NOT NULL,
    rms_vertical_accel double precision,
    PRIMARY KEY (id)
);

-- Add foreign key constraint on field 'wearable_id' that references 'id' field in table 'wearables'.
ALTER TABLE ONLY fall_risk_profiles
    ADD CONSTRAINT FK_wearable_id FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE RESTRICT;
