-- Bootstrap creates the fixed NOLOGIN privilege role; runtime login inherits it.
REVOKE ALL ON SCHEMA gapmap FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA gapmap FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA gapmap FROM PUBLIC;
-- These Supabase roles do not exist on ordinary local PostgreSQL installations.
DO $$
DECLARE api_role text;
BEGIN
  FOREACH api_role IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = api_role) THEN
      EXECUTE format('REVOKE ALL ON SCHEMA gapmap FROM %I', api_role);
      EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA gapmap FROM %I', api_role);
      EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA gapmap FROM %I', api_role);
    END IF;
  END LOOP;
END $$;
GRANT USAGE ON SCHEMA gapmap TO gapmap_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA gapmap TO gapmap_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA gapmap TO gapmap_runtime;
-- Runtime must not edit migration history.
REVOKE ALL ON gapmap.flyway_schema_history FROM gapmap_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA gapmap REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA gapmap REVOKE ALL ON SEQUENCES FROM PUBLIC;
-- Future migrations must grant runtime privileges explicitly for new objects.
