-- Enables RLS for wearables and footsteps.

-- Policy for wearables.
ALTER TABLE wearables
    ENABLE ROW LEVEL SECURITY;
-- Allow tenants with an active or terminated lease with a wearable to look it up.
CREATE POLICY wearable_tenant_isolation_policy ON wearables
    FOR SELECT
    USING (
        EXISTS(
            SELECT 1
            FROM tenant_wearable_leases AS twl
            WHERE wearables.id = twl.wearable_id
            AND twl.tenant_id::TEXT = current_setting('app.tenant_id')
        )
    );
-- All other operations do not matter, as we do not expose any wearable related endpoints for them.
CREATE POLICY wearable_tenant_insert_isolation_policy ON wearables
FOR INSERT
WITH CHECK (true);
CREATE POLICY wearable_tenant_update_isolation_policy ON wearables
FOR UPDATE
USING (true);
CREATE POLICY wearable_tenant_delete_isolation_policy ON wearables
FOR DELETE
USING (true);

-- Policy for footsteps.
ALTER TABLE footsteps
    ENABLE ROW LEVEL SECURITY;
-- Allow selection of footsteps that fall in the timeframe of a tenant's lease(s).
CREATE POLICY footstep_tenant_select_isolation_policy ON footsteps
    FOR SELECT
    USING (
        EXISTS(
            SELECT 1
            FROM tenant_wearable_leases AS twl
            WHERE footsteps.wearable_id = twl.wearable_id
            AND twl.tenant_id::TEXT = current_setting('app.tenant_id')
            AND footsteps.timestamp BETWEEN twl.begin_time AND COALESCE(twl.end_time, NOW())
        )
    );
-- Similar to wearables, creating, updating and deleting footsteps is not managed through endpoints,
-- so there is no need for constraints.
CREATE POLICY footstep_tenant_insert_isolation_policy ON footsteps
    FOR INSERT
    WITH CHECK (true);
CREATE POLICY footstep_tenant_update_isolation_policy ON footsteps
    FOR UPDATE
    USING (true);
CREATE POLICY footstep_tenant_delete_isolation_policy ON footsteps
    FOR DELETE
    USING (true);
