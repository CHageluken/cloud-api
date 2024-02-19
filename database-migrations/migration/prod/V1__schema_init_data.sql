-- SMARTFLOOR SCHEMA

--
-- TABLE DEFINITIONS
--

CREATE TABLE tenants (
    id bigserial,
    name character varying(255) NOT NULL,
    rep_user_id bigint
);

CREATE TABLE users (
    id bigserial,
    auth_id character varying(255) NOT NULL,
    tenant_id bigint NOT NULL,
    UNIQUE (auth_id)
);

CREATE TABLE wearables (
    id character varying(255) NOT NULL,
    tenant_id bigint NOT NULL
);

CREATE TABLE user_wearable_links (
    id bigserial,
    user_id bigint NOT NULL,
    wearable_id character varying(255) NOT NULL,
    begin_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    side integer
);

CREATE TABLE floors (
    id bigserial,
    max_x integer NOT NULL,
    max_y integer NOT NULL,
    name character varying(255) NOT NULL,
    orientation_north double precision NOT NULL
);

CREATE TABLE footsteps (
    id bigserial,
    wearable_id character varying(255) NOT NULL,
    timestamp timestamp without time zone NOT NULL,
    floor_id bigint NOT NULL,
    position_x double precision,
    position_y double precision,
    acceleration_avg_x double precision,
    acceleration_extreme_x double precision,
    acceleration_avg_y double precision,
    acceleration_extreme_y double precision,
    acceleration_avg_z double precision,
    acceleration_extreme_z double precision,
    acceleration_rms_z double precision,
    airtime integer,
    orientation_x integer,
    orientation_y integer,
    orientation_z integer,
    orientation_heading integer
);

CREATE TABLE sessions (
    id bigserial,
    begin_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    created_by bigint NOT NULL,
    name character varying(255)
);

CREATE TABLE session_users (
    session_id bigint NOT NULL,
    user_id bigint NOT NULL
);

--
-- PRIMARY KEY CONSTRAINTS
--

ALTER TABLE ONLY tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY wearables
    ADD CONSTRAINT wearables_pkey PRIMARY KEY (id);

ALTER TABLE ONLY user_wearable_links
    ADD CONSTRAINT user_wearable_links_pkey PRIMARY KEY (id);

ALTER TABLE ONLY floors
    ADD CONSTRAINT floors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY footsteps
    ADD CONSTRAINT footsteps_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY session_users
    ADD CONSTRAINT session_users_pkey PRIMARY KEY (session_id, user_id);

--
-- FOREIGN KEY constraints
--

ALTER TABLE ONLY user_wearable_links
    ADD CONSTRAINT FK_uwlink_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY users
    ADD CONSTRAINT FK_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY sessions
    ADD CONSTRAINT FK_session_created_by_user FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY session_users
    ADD CONSTRAINT FK_user_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY wearables
    ADD CONSTRAINT FK_wearable_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY user_wearable_links
    ADD CONSTRAINT FK_uwlink_wearable FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY tenants
    ADD CONSTRAINT FK_tenant_representative_user FOREIGN KEY (rep_user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY footsteps
    ADD CONSTRAINT FK_footstep_wearable FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY footsteps
    ADD CONSTRAINT FK_footstep_floor FOREIGN KEY (floor_id) REFERENCES floors(id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY session_users
    ADD CONSTRAINT FK_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;

--
-- INDICES
--

CREATE UNIQUE INDEX ON footsteps (wearable_id DESC, timestamp DESC);

--
-- SEED DATA
--
INSERT INTO tenants (name, rep_user_id) VALUES ('smartfloor', NULL);
