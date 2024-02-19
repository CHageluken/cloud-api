-- Create table 'footstep_positions' that will contain (x,y) locations (position of the foot on the floor) for footsteps for which a position could be determined.
CREATE TABLE footstep_positions (
    footstep_id bigserial,
    x double precision NOT NULL,
    y double precision NOT NULL,
    PRIMARY KEY (footstep_id)
);

-- Add foreign key constraint on field 'footstep_id' that references 'id' field in table 'footsteps' (shared primary key).
ALTER TABLE ONLY footstep_positions
    ADD CONSTRAINT FK_footstep_position FOREIGN KEY (footstep_id) REFERENCES footsteps(id) ON UPDATE CASCADE ON DELETE CASCADE;
