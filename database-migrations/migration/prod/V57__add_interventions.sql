-- We want to learn how various interventions methods affect the fall risk of clients. This migration
-- introduces the means to do so by creating the tables:
--
-- `intervention_types` - For storing the 4 intervention types - 'Regular exercise', 'Fall prevention program',
--      'Individual physiotherapy' and 'Other';
-- `fall_prevention_programs` - For storing the different fall prevention programs;
-- `interventions` - For storing the intervention entries. An intervention has a begin date and, optionally, an
--      end date. It also refers to the previous two tables.

CREATE TABLE intervention_types (
    id smallserial,
    name character varying(255) not null,
    PRIMARY KEY (id)
);

CREATE TABLE fall_prevention_programs (
    id smallserial,
    name character varying(255) not null,
    PRIMARY KEY (id)
);

CREATE TABLE interventions (
    id bigserial,
    user_id bigint NOT NULL,
    intervention_type_id smallint NOT NULL,
    begin_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone DEFAULT NULL, -- an intervention might be ongoing
    deleted boolean NOT NULL DEFAULT FALSE,
    fall_prevention_program_id smallint DEFAULT NULL, -- not every intervention is a fall prevention program
    other_program character varying(255) DEFAULT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (intervention_type_id) REFERENCES intervention_types(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (fall_prevention_program_id) REFERENCES fall_prevention_programs(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Populate first two tables
INSERT INTO intervention_types (name) VALUES ('Regular exercise');
INSERT INTO intervention_types (name) VALUES ('Fall prevention program');
INSERT INTO intervention_types (name) VALUES ('Individual physiotherapy');
INSERT INTO intervention_types (name) VALUES ('Other');

INSERT INTO fall_prevention_programs (name) VALUES ('Vallen Verleden Tijd');
INSERT INTO fall_prevention_programs (name) VALUES ('In Balans');
INSERT INTO fall_prevention_programs (name) VALUES ('Otago');
INSERT INTO fall_prevention_programs (name) VALUES ('Thuis Onbezorgd Mobiel');
INSERT INTO fall_prevention_programs (name) VALUES ('Stevig Staan');
INSERT INTO fall_prevention_programs (name) VALUES ('Minder Vallen Door Meer Bewegen');
INSERT INTO fall_prevention_programs (name) VALUES ('Zicht op Evenwicht');

-- Interventions policy
ALTER TABLE interventions ENABLE ROW LEVEL SECURITY;
CREATE POLICY interventions_isolation_policy ON interventions
    USING (EXISTS (
        SELECT 1
        FROM users AS u
        WHERE u.id = interventions.user_id
        AND (
            u.tenant_id::TEXT = current_setting('app.tenant_id', true)
            OR
            u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
        )
    ));