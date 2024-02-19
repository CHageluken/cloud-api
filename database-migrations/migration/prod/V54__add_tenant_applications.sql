-- Table "tenant_applications" tracks the access of groups to floors

CREATE TABLE tenant_applications (
   application_id bigint NOT NULL,
   tenant_id bigint NOT NULL,
   PRIMARY KEY (application_id, tenant_id),
   FOREIGN KEY (application_id) REFERENCES applications(id) ON UPDATE CASCADE ON DELETE CASCADE,
   FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE tenant_applications
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY application_tenant_applications_isolation_policy ON tenant_applications
    USING (
        EXISTS(
            SELECT 1
            FROM tenants AS t
            WHERE t.id = tenant_applications.tenant_id
            AND t.id::TEXT = current_setting('app.tenant_id', true)
        )
        OR
        EXISTS(
            SELECT 1
            FROM users AS u
            INNER JOIN tenants AS t ON t.id = u.tenant_id
            WHERE t.id = tenant_applications.tenant_id
            AND u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
        )
    );

INSERT INTO tenant_applications (tenant_id, application_id)
	SELECT u.tenant_id, a.application_id
    FROM application_access as a
    LEFT JOIN users as u ON u.id = a.user_id
    GROUP BY u.tenant_id, a.application_id;

