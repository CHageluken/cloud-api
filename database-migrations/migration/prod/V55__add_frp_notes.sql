-- A table for storing notes related to fall risk profiles.

CREATE TABLE fall_risk_profile_notes (
    id bigserial,
    fall_risk_profile_id bigint NOT NULL,
    value character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT NOW(),
    updated_at timestamp without time zone,
    created_by bigint NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (fall_risk_profile_id) REFERENCES fall_risk_profiles(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE fall_risk_profile_notes
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY frp_notes_isolation_policy ON fall_risk_profile_notes
    USING (
        EXISTS(
            SELECT 1
            FROM user_wearable_links AS uwl
            INNER JOIN users AS u ON u.id = uwl.user_id
            INNER JOIN fall_risk_profiles AS frp ON frp.wearable_id = uwl.wearable_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                )
            WHERE frp.created_at BETWEEN uwl.begin_time AND uwl.end_time
            AND fall_risk_profile_notes.fall_risk_profile_id = frp.id
        )
    );
