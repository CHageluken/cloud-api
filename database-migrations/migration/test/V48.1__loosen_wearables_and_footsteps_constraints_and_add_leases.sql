-- The new wearables and footsteps policy introduced with the prod migration V48 do NOT take into account wearable
-- groups. The reason is that leases are created by a lambda (which tracks wearable updates) based on wearable
-- groups. It is both redundant and impractical to use wearable groups to determine access to a wearable's footsteps,
-- because that way we cannot say when a wearable has been linked with a group and cannot filter out footsteps that were
-- made with the wearable prior to linking it to a group.
-- However. Most of our integration tests do not care about tenant-wearable leases and test completely unrelated things.
-- To keep the structure of those tests as closely to the original as possible, we loosen the constraints here by
-- allowing wearable and footstep lookups based either on leases or on wearable groups.
ALTER POLICY wearable_tenant_isolation_policy ON wearables
    USING (
        EXISTS(
            SELECT 1
            FROM tenant_wearable_leases AS twl
            WHERE wearables.id = twl.wearable_id
            AND twl.tenant_id::TEXT = current_setting('app.tenant_id')
        )
        OR
        EXISTS (
            SELECT 1
            FROM groups AS g
            INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
            WHERE g.tenant_id::TEXT = current_setting('app.tenant_id')
            AND wearables.id = wgm.wearable_id
        )
    );

ALTER POLICY footstep_tenant_select_isolation_policy ON footsteps
    USING (
        EXISTS(
            SELECT 1
            FROM tenant_wearable_leases AS twl
            WHERE footsteps.wearable_id = twl.wearable_id
            AND twl.tenant_id::TEXT = current_setting('app.tenant_id')
            AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
        )
        OR
        EXISTS (
            SELECT 1
            FROM groups AS g
            INNER JOIN wearable_group_members AS wgm ON g.wearable_group_id = wgm.wearable_group_id
            WHERE g.tenant_id::TEXT = current_setting('app.tenant_id')
            AND footsteps.wearable_id = wgm.wearable_id
        )
    );

-- To make the setup for cross-tenant integration tests easier, we add a new wearable and leases to it through a
-- migration.
INSERT INTO wearables (id) VALUES ('heelable_51');
-- We create leases for tenants 2 and 3, with the new wearable.
-- Tenant 2's lease has an end time.
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2023-09-01 10:00:00.000000', '2023-09-04 10:00:00.000000', 2, 'heelable_51');
-- Tenant 3's lease is ongoing.
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2023-09-04 10:01:00.000000', null, 3, 'heelable_51');
-- Additionally, create tenant leases for the default tenant and any wearables created by migrations (h_37, h_48, ect.).
-- That way, old tests will still comply with the new RLS policies.
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2020-09-01 10:00:00.000000', null, 1, 'heelable_37');
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2020-09-01 10:00:00.000000', null, 1, 'heelable_48');
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2020-09-01 10:00:00.000000', null, 1, 'EFS33rdFF20');
INSERT INTO tenant_wearable_leases (begin_time, end_time, tenant_id, wearable_id)
    VALUES ('2020-09-01 10:00:00.000000', null, 1, 'heelable_50');