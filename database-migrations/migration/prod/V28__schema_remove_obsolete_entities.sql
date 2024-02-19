-- We are no longer offering fall detection.
TRUNCATE TABLE fall_events;
DROP TABLE fall_events;

-- We are no longer using the concept of sessions.
TRUNCATE TABLE session_users;
DROP TABLE session_users;

TRUNCATE TABLE sessions;
DROP TABLE sessions;

-- We are no longer persisting (raw) IMU events.
TRUNCATE TABLE imu_events;
DROP TABLE imu_events;