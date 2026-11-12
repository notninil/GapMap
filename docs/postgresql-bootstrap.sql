-- Reviewed administrative bootstrap for a NEW, EMPTY application schema only.
-- NOT an automatic migration. Do not run during local preparation.
-- No passwords are stored here. Provision login passwords through a secret-safe
-- administrative channel before use. Runtime must never receive owner membership.
CREATE ROLE gapmap_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
CREATE ROLE gapmap_migrator NOLOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
CREATE ROLE gapmap_app NOLOGIN INHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
-- Managed postgres is not a superuser: allow it to administer the custom owner.
-- Never grant postgres TO either application role.
GRANT gapmap_migrator TO postgres WITH ADMIN OPTION;
GRANT gapmap_runtime TO gapmap_app;
CREATE SCHEMA gapmap AUTHORIZATION gapmap_migrator;
REVOKE ALL ON SCHEMA gapmap FROM PUBLIC;
-- Do not add gapmap to exposed Data API schemas. Disable unused Data API access.
-- After committing reviewed provisioning, use psql \password for each login role,
-- then ALTER ROLE gapmap_migrator LOGIN; ALTER ROLE gapmap_app LOGIN;
