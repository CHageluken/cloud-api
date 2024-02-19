-- Too many tests are breaking with the introduction of migration V56. Therefore, for tests, we revert the affected
-- tables' policies to their previous state.

ALTER POLICY uwl_isolation_policy ON user_wearable_links
    USING (
        EXISTS(
            SELECT 1
            FROM users as u
            WHERE u.id = user_wearable_links.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );

ALTER POLICY test_result_isolation_policy ON test_results
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            WHERE u.id = test_results.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );

ALTER POLICY frp_isolation_policy ON fall_risk_profiles
    USING (
        EXISTS(
            SELECT 1
            FROM user_wearable_links AS uwl
            INNER JOIN users AS u ON u.id = uwl.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
            WHERE uwl.wearable_id = fall_risk_profiles.wearable_id
            AND fall_risk_profiles.created_at BETWEEN uwl.begin_time AND uwl.end_time
        )
    );
