-- Create the "Row-Level Security" (RLS) user that is used by the application during operation.
-- Note that the migrations are ran using the main postgres user. This user can then no longer be used since RLS
-- does normally not apply to table owners and overriding this can be inconvenient for that user's rights.
CREATE USER rls WITH PASSWORD 'dev_rls';

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rls;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, UPDATE, USAGE ON SEQUENCES TO rls;