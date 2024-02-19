-- Table "floor-groups" tracks the access of groups to floors

CREATE TABLE floor_groups (
    group_id bigserial,
    floor_id bigserial,

    FOREIGN KEY (group_id) REFERENCES groups(id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE floor_groups
    ADD PRIMARY KEY(group_id, floor_id);

ALTER TABLE floor_groups
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY floor_groups_isolation_policy ON floor_groups
    USING (EXISTS (
                SELECT 1
                FROM group_users AS gu
                INNER JOIN users AS u ON gu.user_id = u.id
                INNER JOIN groups AS g ON gu.group_id = g.id
                WHERE gu.group_id = floor_groups.group_id
                AND (
                    g.tenant_id::TEXT = current_setting('app.tenant_id', true)
                    OR
                    u.composite_user_id::TEXT = current_setting('app.composite_user_id', true)
                    )

                UNION

                SELECT 1
                FROM group_managers AS gm
                INNER JOIN users AS u ON gm.user_id = u.id
                INNER JOIN groups AS g ON gm.group_id = g.id
                WHERE gm.group_id = floor_groups.group_id
                AND g.tenant_id::TEXT = current_setting('app.tenant_id', true)
    ));
