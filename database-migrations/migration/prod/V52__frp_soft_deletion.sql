CREATE TABLE fall_risk_profile_removal_reasons (
   id smallint NOT NULL UNIQUE CHECK (id >= 1),
   reason character varying(30) NOT NULL
);

INSERT INTO fall_risk_profile_removal_reasons (id, reason) VALUES(1, 'Walking protocol not followed');
INSERT INTO fall_risk_profile_removal_reasons (id, reason) VALUES(2, 'Unnecessary measurement');
INSERT INTO fall_risk_profile_removal_reasons (id, reason) VALUES(3, 'Problem with sensor');
INSERT INTO fall_risk_profile_removal_reasons (id, reason) VALUES(4, 'Problem with web-application');
INSERT INTO fall_risk_profile_removal_reasons (id, reason) VALUES(5, 'Other');

CREATE TABLE fall_risk_profile_removals (
    id bigserial,
    fall_risk_profile_id bigint NOT NULL,
    reason_for_removal smallint NOT NULL,
    specification_other character varying(255) default '',
    deleted_at timestamp without time zone DEFAULT NOW(),
    deleted_by bigint NOT NULL,
    PRIMARY KEY(id)
);

ALTER TABLE ONLY fall_risk_profile_removals
    ADD constraint fk_deleted_by
    FOREIGN KEY(deleted_by) REFERENCES users(id)
    ON UPDATE CASCADE;

ALTER TABLE ONLY fall_risk_profile_removals
    ADD constraint fk_reason_id
    FOREIGN KEY(reason_for_removal)
    REFERENCES fall_risk_profile_removal_reasons(id)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY fall_risk_profile_removals
    ADD constraint fk_fall_risk_profile_id
    FOREIGN KEY(fall_risk_profile_id)
    REFERENCES fall_risk_profiles(id)
    ON UPDATE CASCADE ON DELETE CASCADE;
