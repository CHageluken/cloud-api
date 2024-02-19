-- For all tables defined before this point, grant select, insert, update, delete to rls
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rls;
GRANT SELECT, UPDATE, USAGE ON ALL SEQUENCES IN SCHEMA public TO rls;

-- For all tables defined after this point, grant select, insert, update, delete to rls
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rls;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, UPDATE, USAGE ON SEQUENCES TO rls;
