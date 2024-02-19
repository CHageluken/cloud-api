-- We provide our test suite with a second tenant that has some entities with data so that we can evaluate whether any
-- user authenticated within a different tenant context can access it (which should not be possible).
INSERT INTO tenants (name) VALUES ('multitenancy_test');

-- The tenant controller test uses a tenant 'limited' that has a user limit set
INSERT INTO tenants (name, user_limit) VALUES ('limited', 1);

-- Create test user for default (test) tenant
INSERT INTO users (auth_id, tenant_id) VALUES ('smartfloor-test', 1);

-- For tests, we want to be able to create the default test user for different tenants as well.
-- So, we relax the uniqueness constraint on the users table to a (auth_id, tenant_id) combination.
ALTER TABLE users DROP CONSTRAINT users_auth_id_key;
CREATE UNIQUE INDEX unique_auth_id_tenant_combination ON users (auth_id, tenant_id);