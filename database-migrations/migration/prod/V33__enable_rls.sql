-- Enable row(-level) security on a subset of our schema to enforce tenant isolation at the database level.
-- The policies are comparing the tenant id to a configurable application tenant id parameter.
-- This parameter should be set for every connection to the database.

-- Note: in some policies, we look up the foreign key s.t. we can check the tenant id of the parent table.
-- We could also do without the additional tenant id check.
-- But then we would assume row-level security to be enabled on the parent table.
-- For some clear parent-child relationships, such as footstep_positions, this assumption is fine.
-- For others, such as fall_risk_profiles, this assumption is insufficient.
-- In those cases, we'd rather be explicit. That is, even if RLS is no longer enabled on the referred table,
-- we would still not return any records for the table.

-- Note: The below exceptional policies are for tenants. For tenants, we want to allow selection of all tenants but
-- modification of only the tenant row matching the configured tenant.

-- Note: We currently do not define policies for the following entities: wearables, footsteps and footstep positions.
-- For wearables, we can not easily determine which tenant should be able to access what wearable due to the possibility
-- of wearable migration between tenants (see VIT-587).
-- For footsteps and (its related entity) footstep positions, we rely on the wearable for determining tenancy,
-- which hence suffers from the same indetermination issue for tenancy as wearables.
-- These entities will be restricted in a follow-up implementation of RLS policies, requiring VIT-595 to be implemented
-- first.

ALTER TABLE tenants
    ENABLE ROW LEVEL SECURITY;

-- Allow all rows to be visible in select statements so that we can always look up the (current) tenant
-- We currently also allow the looking up of tenants other than the one configured because of wearable lookups (see VIT-587)
CREATE POLICY tenant_select_isolation_policy ON tenants
    FOR SELECT
    USING (true);

CREATE POLICY tenant_insert_isolation_policy ON tenants
    FOR INSERT
    WITH CHECK (id::TEXT = current_setting('app.tenant_id'));

CREATE POLICY tenant_update_isolation_policy ON tenants
    FOR UPDATE
    USING (id::TEXT = current_setting('app.tenant_id'));

CREATE POLICY tenant_delete_isolation_policy ON tenants
    FOR DELETE
    USING (id::TEXT = current_setting('app.tenant_id'));

ALTER TABLE users
    ENABLE ROW LEVEL SECURITY;

-- We allow the user's auth id to be set so that we can allow tenant id look ups for the authenticated user.
-- This so that we can then configure the tenant id based on the result of this look up.
CREATE POLICY user_tenant_isolation_policy ON users
    USING (tenant_id::TEXT = current_setting('app.tenant_id'));

ALTER TABLE user_info
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_info_tenant_isolation_policy ON user_info
    USING (EXISTS(
        SELECT 1
        FROM users AS u
        WHERE u.id = user_info.user_id
    ));

ALTER TABLE user_wearable_links
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY uwl_tenant_isolation_policy ON user_wearable_links
    USING (EXISTS(SELECT 1
                  FROM users as u
                  WHERE u.id = user_wearable_links.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));

ALTER TABLE floor_viewers
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY floor_viewer_tenant_isolation_policy ON floor_viewers
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = floor_viewers.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));

ALTER TABLE fall_risk_profiles
    ENABLE ROW LEVEL SECURITY;

-- Look up all the user-wearable links of users of the currently configured tenant.
-- Filter on the user-wearable links for which the fall risk profiles fall within their window.
-- If they exist, then we return the fall risk profiles within their window as part of the result set.
-- This since it implies that these particular fall risk profiles belong to the currently configured tenant.
CREATE POLICY frp_tenant_isolation_policy ON fall_risk_profiles
    FOR SELECT
    USING (EXISTS(
        SELECT 1
        FROM user_wearable_links AS uwl
        INNER JOIN users AS u ON u.id = uwl.user_id
            AND u.tenant_id::TEXT = current_setting('app.tenant_id')
        WHERE uwl.wearable_id = fall_risk_profiles.wearable_id
          AND fall_risk_profiles.created_at BETWEEN uwl.begin_time AND uwl.end_time
    ));

ALTER TABLE groups
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY group_tenant_isolation_policy ON groups
    USING (tenant_id::TEXT = current_setting('app.tenant_id'));

ALTER TABLE group_managers
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY group_manager_tenant_isolation_policy ON group_managers
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = group_managers.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));

ALTER TABLE group_users
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY group_user_tenant_isolation_policy ON group_users
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = group_users.user_id
    ));

ALTER TABLE test_results
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY test_result_tenant_isolation_policy ON test_results
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = test_results.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));

ALTER TABLE test_surveys
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY test_survey_tenant_isolation_policy ON test_surveys
    USING (EXISTS(SELECT 1
                  FROM test_results AS tr
                  WHERE tr.id = test_surveys.test_id
    ));

ALTER TABLE test_trials
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY test_trial_tenant_isolation_policy ON test_trials
    USING (EXISTS(SELECT 1
                  FROM test_results AS tr
                  WHERE tr.id = test_trials.test_id));

ALTER TABLE user_measurements
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_measurement_tenant_isolation_policy ON user_measurements
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = user_measurements.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));

ALTER TABLE application_access
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY application_access_tenant_isolation_policy ON application_access
    USING (EXISTS(SELECT 1
                  FROM users AS u
                  WHERE u.id = application_access.user_id
                    AND u.tenant_id::TEXT = current_setting('app.tenant_id')
    ));