-- Define wearable groups table
-- This table will be used to group wearables together. It is based on the thing groups that we keep in AWS IoT.
-- Each thing group name should directly map to a wearable group name.
-- The goal is to (at least initially) only store the child thing groups ("leaf nodes" of the thing group hierarchy)
-- in this table, i.e. the ones that contain the things (wearables).
CREATE TABLE wearable_groups (
    id serial PRIMARY KEY,
    name varchar(255) NOT NULL
);

-- Define wearable group members table
CREATE TABLE wearable_group_members (
    wearable_id text NOT NULL,
    wearable_group_id integer NOT NULL,
    PRIMARY KEY (wearable_id, wearable_group_id),
    FOREIGN KEY (wearable_id) REFERENCES wearables(id),
    FOREIGN KEY (wearable_group_id) REFERENCES wearable_groups(id)
);

-- Add wearable group id to (user) groups table
ALTER TABLE groups ADD COLUMN wearable_group_id bigint;
ALTER TABLE groups ADD FOREIGN KEY (wearable_group_id) REFERENCES wearable_groups(id);