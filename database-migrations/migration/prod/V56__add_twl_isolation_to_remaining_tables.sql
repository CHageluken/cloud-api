-- This migration extends the policies of the tables `user_wearable_links`, `fall_risk_profiles` and `test_results` in
-- such a way that they take `tenant_wearable_leases` into account. This restricts access to UWLs, FRPs and test results
-- that have been performed with a wearable that has never been leased by the current tenant / sub-users' tenants.
-- See VIT-1329 for more info.

-- Access to a UWL is allowed when:
-- 1. The UWL belongs to a user which is part of the current tenant / the current CU's sub-users
-- 2. The wearable of the UWL is accessible. This is the case if:
-- 2.1. TWLs are associated with the tenant of users (which are filtered in step 1)
-- 2.2. UWLs and TWLs concern the same wearable
-- 2.3. UWLs fall under the time frame of TWLs
ALTER POLICY uwl_isolation_policy ON user_wearable_links
    USING (
        EXISTS(
            SELECT 1
            FROM users as u
            INNER JOIN tenant_wearable_leases AS twl ON twl.tenant_id = u.tenant_id
            WHERE u.id = user_wearable_links.user_id
            AND user_wearable_links.wearable_id = twl.wearable_id
            AND user_wearable_links.begin_time >= twl.begin_time
            -- If the TWL has an end time, make sure the UWL's end time is earlier
            AND (
                CASE
                    WHEN twl.end_time IS NULL THEN true
                    ELSE user_wearable_links.end_time <= twl.end_time
                END
            )
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );

-- Access to an FRP is allowed when it is created during a UWL that is accessible. The previous policy already takes
-- care of restricting access to UWLs, but we do that here as well.
ALTER POLICY frp_isolation_policy ON fall_risk_profiles
    USING (
        EXISTS(
            SELECT 1
            FROM user_wearable_links AS uwl
            INNER JOIN users AS u ON u.id = uwl.user_id
            INNER JOIN tenant_wearable_leases AS twl ON twl.tenant_id = u.tenant_id
            WHERE uwl.wearable_id = twl.wearable_id
            AND uwl.begin_time >= twl.begin_time
            AND (
                CASE
                    WHEN twl.end_time IS NULL THEN true
                    ELSE uwl.end_time <= twl.end_time
                END
            )
            AND uwl.wearable_id = fall_risk_profiles.wearable_id
            AND fall_risk_profiles.created_at BETWEEN uwl.begin_time AND uwl.end_time
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                )
        )
    );

-- The test results should either be associated to no wearables, or to ones that
-- have been (or still are) leased by the user's tenant.
-- The test results should also fall under the time window of the leases.
ALTER POLICY test_result_isolation_policy ON test_results
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            INNER JOIN tenant_wearable_leases AS twl ON twl.tenant_id = u.tenant_id
            WHERE u.id = test_results.user_id
            AND (
                test_results.wearable_id IS NULL
                OR (
                    twl.wearable_id = test_results.wearable_id
                    AND test_results.begin_time >= twl.begin_time
                    AND test_results.end_time <= COALESCE(twl.end_time, NOW())
                )
              )
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );
