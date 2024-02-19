-- This function expresses the constraint that a wearable can only be leased by one tenant at a time
CREATE FUNCTION check_tenant_wearable_leases_no_overlap(
    new_id bigint,
    new_wearable_id character varying(255),
    new_begin_time timestamp without time zone,
    new_end_time timestamp without time zone
) returns boolean as
$$
BEGIN
    RETURN NOT EXISTS(
            SELECT 1
            FROM tenant_wearable_leases twl
            WHERE twl.wearable_id = new_wearable_id
              AND (COALESCE(id, 0) <> COALESCE(twl.id, 0))
              AND (new_begin_time, new_end_time) OVERLAPS (twl.begin_time, twl.end_time)
        );
END;
$$ LANGUAGE plpgsql;

-- The tenant_wearable_leases table tracks the current lease for a wearable.
-- It answers the question: Which tenant (customer/organization) is currently leasing the wearable?
-- We have a separate id column as the primary key to stay consistent with user_wearable_links.
CREATE TABLE tenant_wearable_leases
(
    id          bigserial,
    tenant_id   bigint                 NOT NULL,
    wearable_id character varying(255) NOT NULL,
    begin_time  timestamp without time zone DEFAULT NOW(),
    end_time    timestamp without time zone,
    PRIMARY KEY (id),
    FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    FOREIGN KEY (wearable_id) REFERENCES wearables (id) ON DELETE CASCADE,
    -- A wearable can only be leased by one tenant at a time
    CONSTRAINT chk_tenant_wearable_leases_no_overlap CHECK (
        check_tenant_wearable_leases_no_overlap(id, wearable_id, begin_time, end_time)
        )
);

-- Add all the existing wearable tenant leases to the new table
INSERT INTO tenant_wearable_leases (tenant_id, wearable_id, begin_time, end_time)
SELECT w.tenant_id, w.id, NOW(), NULL
FROM wearables w;

-- Keeping the tenant id for a wearable is no longer necessary.
-- Instead, we can get the current lease from the tenant_wearable_leases table.
ALTER TABLE wearables
    DROP COLUMN tenant_id;