-- To be able to record fall incidents, we add a "fall incident" measurement type to the user measurement types table.
INSERT INTO user_measurement_types (name, description)
VALUES ('FALL_INCIDENT', 'A record of when the recorded user has fallen.')