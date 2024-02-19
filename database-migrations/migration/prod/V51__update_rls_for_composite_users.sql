-- Due to the introduction of the composite user entity, it's now possible to authenticate a user who doesn't belong to
-- any tenant. Previously, our policy checks relied on the `current_setting('app.tenant_id')` to always return a value.
-- However, for authenticated composite users, this could lead to an error: `unrecognized configuration parameter` as
-- they won't have a `tenant_id` field. Instead, these users have a custom configuration parameter named
-- `composite_user_id`.

-- This migration addresses those changes by:
-- 1) Avoiding the `unrecognized configuration parameter` error. We achieve this by adding a flag to the
--    `current_setting('app.tenant_id', true)`. This ensures a safe attempt to fetch a potentially non-existent
--    configuration parameter, returning NULL if absent.
-- 2) Incorporating the `composite_user_id` setting in the relevant policies.
-- 3) Renaming all RLS policies that we modify. Their definition changes, so the `tenant_isolation` sequence doesn't
--    fit that well anymore.

-- Post-migration, we will have dual access scopes based on the authenticated user:
-- - One granting access to resources tied to a specific tenant.
-- - Another granting access to resources specific to a set of users (who might belong to different tenants).

-- Composite users will only be able to view tenants, but not modify them. We keep the names of those policies.
ALTER POLICY tenant_insert_isolation_policy ON tenants
    WITH CHECK (id::TEXT = current_setting('app.tenant_id', true));
ALTER POLICY tenant_update_isolation_policy ON tenants
    USING (id::TEXT = current_setting('app.tenant_id', true));
ALTER POLICY tenant_delete_isolation_policy ON tenants
    USING (id::TEXT = current_setting('app.tenant_id', true));

-- Direct users are allowed to access only users linked to their tenant.
-- Composite users are allowed to access only their sub-users.
ALTER POLICY user_tenant_isolation_policy ON users
    USING (
        tenant_id::TEXT = current_setting('app.tenant_id', true)
        OR
        composite_user_id::TEXT = current_setting('app.composite_user_id', true)
    );
ALTER POLICY user_tenant_isolation_policy ON users RENAME TO user_isolation_policy;

-- No need to change the definition of this policy, just its name.
ALTER POLICY user_info_tenant_isolation_policy ON user_info RENAME TO user_info_isolation_policy;

-- We update the definition and the name of this policy.
ALTER POLICY uwl_tenant_isolation_policy ON user_wearable_links
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
ALTER POLICY uwl_tenant_isolation_policy ON user_wearable_links RENAME TO uwl_isolation_policy;

-- Update definition and name.
ALTER POLICY floor_viewer_tenant_isolation_policy ON floor_viewers
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            WHERE u.id = floor_viewers.user_id
                AND (
                    u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                    OR
                    u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                )
        )
    );
ALTER POLICY floor_viewer_tenant_isolation_policy ON floor_viewers RENAME TO floor_viewer_isolation_policy;

-- Update definition and name.
ALTER POLICY frp_tenant_isolation_policy ON fall_risk_profiles
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
ALTER POLICY frp_tenant_isolation_policy ON fall_risk_profiles RENAME TO frp_isolation_policy;

-- Update definition and name.
-- Direct users have access to groups that belong to their tenant.
-- Composite users have access to groups that their sub-users are a part of.
ALTER POLICY group_tenant_isolation_policy ON groups
    USING (
        tenant_id::TEXT = current_setting('app.tenant_id', true)
        OR
        EXISTS(
            SELECT 1
            FROM users AS u
            INNER JOIN group_users AS gu ON gu.user_id = u.id
            AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            WHERE gu.group_id = groups.id
        )
    );
ALTER POLICY group_tenant_isolation_policy ON groups RENAME TO group_isolation_policy;

-- Update both definition and name.
ALTER POLICY group_manager_tenant_isolation_policy ON group_managers
    USING (
        (current_setting('app.tenant_id', true) IS NOT NULL
            AND
            EXISTS(
                SELECT 1
                FROM users AS u
                WHERE u.id = group_managers.user_id
                AND u.tenant_id::TEXT = current_setting('app.tenant_id', true)
            )
        )
        OR
        (current_setting('app.composite_user_id', true) IS NOT NULL AND false)
    );
ALTER POLICY group_manager_tenant_isolation_policy ON group_managers RENAME TO group_manager_isolation_policy;

-- Definition and name.
ALTER POLICY group_user_tenant_isolation_policy ON group_users
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            WHERE u.id = group_users.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );
ALTER POLICY group_user_tenant_isolation_policy ON group_users RENAME TO group_user_isolation_policy;

-- Definition and name.
ALTER POLICY test_result_tenant_isolation_policy ON test_results
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
ALTER POLICY test_result_tenant_isolation_policy ON test_results RENAME TO test_result_isolation_policy;

-- Just name.
ALTER POLICY test_survey_tenant_isolation_policy ON test_surveys RENAME TO test_survey_isolation_policy;
ALTER POLICY test_trial_tenant_isolation_policy ON test_trials RENAME TO test_trial_isolation_policy;

-- Definition and name.
ALTER POLICY user_measurement_tenant_isolation_policy ON user_measurements
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            WHERE u.id = user_measurements.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );
ALTER POLICY user_measurement_tenant_isolation_policy ON user_measurements RENAME TO user_measurement_isolation_policy;

-- Definition and name.
ALTER POLICY application_access_tenant_isolation_policy ON application_access
    USING (
        EXISTS(
            SELECT 1
            FROM users AS u
            WHERE u.id = application_access.user_id
            AND (
                u.tenant_id::TEXT = current_setting('app.tenant_id', true)
                OR
                u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );
ALTER POLICY application_access_tenant_isolation_policy ON application_access RENAME TO application_access_isolation_policy;

-- Definition and name.
-- For direct users, allow access to a wearable only if their tenant has an active or a terminated lease.
-- For composite users, allow access to a wearable only if the sub-users' tenants have an active or terminated lease
-- with that wearable.
ALTER POLICY wearable_tenant_isolation_policy ON wearables
    USING (
        (current_setting('app.tenant_id', true) IS NOT NULL
            AND
            EXISTS(
                SELECT 1
                FROM tenant_wearable_leases AS twl
                WHERE wearables.id = twl.wearable_id
                AND twl.tenant_id::TEXT = current_setting('app.tenant_id', true)
            )
        )
        OR
        (current_setting('app.composite_user_id', true) IS NOT NULL
            AND
            EXISTS(
                SELECT 1
                FROM users AS u
                INNER JOIN tenant_wearable_leases AS twl ON u.tenant_id = twl.tenant_id
                WHERE wearables.id = twl.wearable_id
                AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            )
        )
    );

ALTER POLICY wearable_tenant_isolation_policy ON wearables RENAME TO wearable_select_isolation_policy;

-- Just name.
ALTER POLICY wearable_tenant_insert_isolation_policy ON wearables RENAME TO wearable_insert_isolation_policy;
ALTER POLICY wearable_tenant_update_isolation_policy ON wearables RENAME TO wearable_update_isolation_policy;
ALTER POLICY wearable_tenant_delete_isolation_policy ON wearables RENAME TO wearable_delete_isolation_policy;

-- Definition and name.
-- For direct users, allow selection of footsteps that fall in the timeframe of their tenant's lease(s).
-- For composite users, allow selection of footsteps that fall in the timeframe of their sub-users' tenant's lease(s)
ALTER POLICY footstep_tenant_select_isolation_policy ON footsteps
    USING (
        EXISTS(
            SELECT 1
            FROM tenant_wearable_leases AS twl
            WHERE footsteps.wearable_id = twl.wearable_id
            AND twl.tenant_id::TEXT = current_setting('app.tenant_id', true)
            AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
        )
        OR
        EXISTS(
            SELECT 1
            FROM users AS u
            INNER JOIN tenant_wearable_leases AS twl ON u.tenant_id = twl.tenant_id
            WHERE footsteps.wearable_id = twl.wearable_id
            AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
            AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
        )
    );
ALTER POLICY footstep_tenant_select_isolation_policy ON footsteps RENAME TO footstep_select_isolation_policy;
-- Just name.
ALTER POLICY footstep_tenant_insert_isolation_policy ON footsteps RENAME TO footstep_insert_isolation_policy;
ALTER POLICY footstep_tenant_update_isolation_policy ON footsteps RENAME TO footstep_update_isolation_policy;
ALTER POLICY footstep_tenant_delete_isolation_policy ON footsteps RENAME TO footstep_delete_isolation_policy;