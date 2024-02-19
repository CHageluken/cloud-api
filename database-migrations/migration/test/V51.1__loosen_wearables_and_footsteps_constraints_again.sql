-- The updated wearables and footsteps policies introduced with the prod migration V51 do NOT take into account wearable
-- groups. The reason is that leases are created by a lambda (which tracks wearable updates) based on wearable
-- groups. It is both redundant and impractical to use wearable groups to determine access to a wearable's footsteps,
-- because that way we cannot say when a wearable has been linked with a group and cannot filter out footsteps that were
-- made with the wearable prior to linking it to a group.
-- However. Most of our integration tests do not care about tenant-wearable leases and test completely unrelated things.
-- To keep the structure of those tests as closely to the original as possible, we loosen the constraints here by
-- allowing wearable and footstep lookups based either on leases or on wearable groups.
-- This migration also takes into account composite users.
ALTER POLICY wearable_select_isolation_policy ON wearables
    USING (
        (current_setting('app.tenant_id', true) IS NOT NULL
            AND (
                EXISTS(
                    SELECT 1
                    FROM tenant_wearable_leases AS twl
                    WHERE wearables.id = twl.wearable_id
                    AND twl.tenant_id::TEXT = current_setting('app.tenant_id', true)
                )
                OR
                EXISTS(
                    SELECT 1
                    FROM groups AS g
                    INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
                    WHERE g.tenant_id::TEXT = current_setting('app.tenant_id', true)
                    AND wearables.id = wgm.wearable_id
                )
            )
        )
        OR
        (current_setting('app.composite_user_id', true) IS NOT NULL
            AND (
                EXISTS(
                    SELECT 1
                    FROM users AS u
                    INNER JOIN tenant_wearable_leases AS twl ON u.tenant_id = twl.tenant_id
                    WHERE wearables.id = twl.wearable_id
                    AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                )
                OR
                EXISTS(
                    SELECT 1
                    FROM groups AS g
                    INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
                    INNER JOIN group_users AS gu ON gu.group_id = g.id
                    INNER JOIN users AS u ON u.id = gu.user_id
                    WHERE u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                    AND wearables.id = wgm.wearable_id
                )
            )
        )
    );

ALTER POLICY footstep_select_isolation_policy ON footsteps
    USING (
        (current_setting('app.tenant_id', true) IS NOT NULL
            AND (
                EXISTS(
                    SELECT 1
                    FROM tenant_wearable_leases AS twl
                    WHERE footsteps.wearable_id = twl.wearable_id
                    AND twl.tenant_id::TEXT = current_setting('app.tenant_id', true)
                    AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
                )
                OR
                EXISTS (
                    SELECT 1
                    FROM groups AS g
                    INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
                    WHERE g.tenant_id::TEXT = current_setting('app.tenant_id', true)
                    AND footsteps.wearable_id = wgm.wearable_id
                )
            )
        )
        OR
        (current_setting('app.composite_user_id', true) IS NOT NULL
            AND (
                EXISTS(
                    SELECT 1
                    FROM users AS u
                    INNER JOIN tenant_wearable_leases AS twl ON u.tenant_id = twl.tenant_id
                    WHERE footsteps.wearable_id = twl.wearable_id
                    AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                    AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
                )
                OR
                EXISTS (
                    SELECT 1
                    FROM groups AS g
                    INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
                    INNER JOIN group_users AS gu ON gu.group_id = g.id
                    INNER JOIN users AS u ON u.id = gu.user_id
                    WHERE u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                    AND footsteps.wearable_id = wgm.wearable_id
                )
            )
        )
    );