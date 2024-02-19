CREATE TABLE imu_events (
    wearable_id character varying(255) NOT NULL,
    timestamp timestamp without time zone NOT NULL,
    accel_x float8[],
    accel_y float8[],
    accel_z float8[],
    gyro_x float8[],
    gyro_y float8[],
    gyro_z float8[],
    magneto_x float8[],
    magneto_y float8[],
    magneto_z float8[],
    PRIMARY KEY (wearable_id, timestamp),
    FOREIGN KEY (wearable_id) REFERENCES wearables(id) ON UPDATE CASCADE ON DELETE CASCADE
)