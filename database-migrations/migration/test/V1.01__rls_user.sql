-- Create the "Row-Level Security" (RLS) user that is used by the application during operation.
-- Note that the migrations are ran using the main postgres user. This user can then no longer be used since RLS
-- does normally not apply to table owners and overriding this can be inconvenient for that user's rights.
DO
$do$
    BEGIN
        IF EXISTS (
                SELECT FROM pg_catalog.pg_roles
                WHERE  rolname = 'rls') THEN

            RAISE NOTICE 'Role "rls" already exists. Skipping.';
        ELSE
            CREATE ROLE rls LOGIN PASSWORD 'test_rls';
        END IF;
    END
$do$;