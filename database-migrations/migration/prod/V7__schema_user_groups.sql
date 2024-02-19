-- Table 'groups' will define groups that can contain one or more users, logically grouping them together according to a
-- shared purpose.
--
-- Examples of group names: 'Home 5', 'Team 1', etc.
-- We require group names to be unique for the same tenant.
CREATE TABLE groups (
    id bigserial,
    name character varying(255) NOT NULL,
    tenant_id bigint NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, name),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Join table 'group_users' will define the many-to-many relationship between groups and users that determines which
-- users belong to which groups.
CREATE TABLE group_users (
    group_id bigint,
    user_id bigint,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);
