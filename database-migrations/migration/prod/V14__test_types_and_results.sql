CREATE TABLE test_types (
    id bigserial,
    name character varying(255) not null,
    PRIMARY KEY (id)
);

CREATE TABLE test_survey_types (
    id bigserial,
    name character varying(255) not null,
    PRIMARY KEY (id)
);

CREATE TABLE test_results (
    id bigserial,
    type int not null,
    user_id bigint not null,
    begin_time timestamp without time zone not null,
    end_time timestamp without time zone not null,
    remarks text default '',
    additional_info jsonb default '{}'::jsonb,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (type) REFERENCES test_types(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE test_trials (
    test_id bigserial,
    trial_nr int not null,
    begin_time timestamp without time zone not null,
    end_time timestamp without time zone not null,
    PRIMARY KEY (test_id, trial_nr),
    FOREIGN KEY (test_id) REFERENCES test_results(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE test_surveys (
    test_id bigserial,
    type int not null,
    content jsonb DEFAULT '{}'::jsonb,
    PRIMARY KEY (test_id, type),
    FOREIGN KEY (test_id) REFERENCES test_results(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (type) REFERENCES test_survey_types(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

/* Seed test types table with rehabilitation test types. */
INSERT INTO test_types (name) VALUES ('Berg Balance Scale');
INSERT INTO test_types (name) VALUES ('Six Minute Walking');
INSERT INTO test_types (name) VALUES ('Ten Meter Walking');
INSERT INTO test_types (name) VALUES ('Timed Up n Go');

/* Seed test survey types table with rehabilitation survey types. */
INSERT INTO test_survey_types (name) VALUES ('Borg-RPE');
INSERT INTO test_survey_types (name) VALUES ('FAC');
INSERT INTO test_survey_types (name) VALUES ('FES-I');
INSERT INTO test_survey_types (name) VALUES ('NPRS');
INSERT INTO test_survey_types (name) VALUES ('NRS');
