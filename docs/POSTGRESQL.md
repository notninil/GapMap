# PostgreSQL preparation and migration runbook

The `supabase` profile disables Flyway and requires only runtime credentials. See
[SUPABASE-RUNTIME.md](SUPABASE-RUNTIME.md) for Windows launch commands and persistence checks.
Earlier verification notes at the end are historical snapshots, not current status.

Local preparation only: no Supabase database has been contacted and no data has
been transferred. Existing Spring Security, BCrypt, JWT cookies, entities,
repositories and APIs are retained. Spring Boot 3.4.5 and Java target 17 remain.

## Profiles

| Profile | Database | Schema management | Demo login / seed |
| --- | --- | --- | --- |
| default | Existing local MySQL settings | Hibernate update; Flyway off | Existing behavior |
| demo | Existing file-backed H2 | Hibernate update; Flyway off | Enabled |
| supabase | Explicit PostgreSQL JDBC URL | Separate Flyway job; runtime Hibernate validate | Both disabled |

Never combine demo and supabase. The Supabase profile rejects that combination
and missing/blank required settings before database initialization. It requires
verified TLS. Migration jobs use a distinct login outside the backend. Do not override its
safety settings in a deployment.

Required backend process environment variables (no credentials in source):

- `SUPABASE_DB_URL`: `jdbc:postgresql://<approved-host>:<port>/postgres?sslmode=verify-full&sslrootcert=<absolute-trusted-CA-path>`.
- `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`: restricted application login.
- `JWT_SECRET`: random deployment-specific secret, at least 32 characters.
- `TEACHER_INVITE_CODE`: deployment-specific invitation secret.
- `FRONTEND_ORIGIN`: exact HTTPS frontend origin.
- Optional `SUPABASE_DB_POOL_SIZE`: defaults to 5; size across all backend instances.

Migration credentials belong only in the separate Flyway process, never the runtime.
For the session pooler, use `gapmap_app.<project-ref>` as the runtime username.

Select the profile with `SPRING_PROFILES_ACTIVE=supabase` only after approval to
connect. `.env.example` documents names; Spring Boot does not automatically load
`.env`. Do not reuse its local demo secrets. The existing Compose stack remains
MySQL and now supplies the correct `DB_USER` variable.

Prefer a direct connection when network support allows, otherwise session
pooling. Avoid transaction pooling initially. Verify the actual project endpoint,
pooler username format, certificate chain and available connections before use.
The same approved endpoint is used by both migration and runtime logins.

## Administrative provisioning (not performed)

Review `docs/postgresql-bootstrap.sql` for a fresh, empty deployment. An
administrator must provision the roles and schema; application startup cannot
create schemas. Do not run the bootstrap unchanged over existing roles/schema.
Run reviewed provisioning in an explicit transaction. The bootstrap grants the custom
owner role to managed `postgres` so it can create the schema with that owner. It starts
the login roles as NOLOGIN; assign strong passwords with interactive psql `\password`
after committing provisioning, then enable LOGIN. Never reverse the ownership grant.

- `gapmap_migrator` owns the schema and objects.
- `gapmap_runtime` is a NOLOGIN privilege group.
- `gapmap_app` inherits runtime privileges only. It must not inherit ownership,
  superuser, DDL, role-management, database-creation or BYPASSRLS privileges.
- Exclude `gapmap` from exposed Data API schemas; disable the Data API if unused.
  Review inherited grants as well as explicit grants on the actual deployment.
- Migrations revoke application-schema access from PUBLIC and existing Supabase
  API roles. Runtime receives CRUD and sequence access, but not migration-history
  access or schema creation rights. New migrations must explicitly grant access
  to any new objects they introduce.

User-level authorization remains in Spring. The shared JDBC login does not
establish a Supabase Auth identity; do not assume `auth.uid()` RLS policies can
interpret existing application JWTs. No frontend Supabase SDK or API key is needed.

## Migrations

`src/main/resources/db/migration/postgresql/` contains:

1. `V1__application_schema.sql`: 15 application tables, 13 bigint identity keys,
   composite join-table keys, foreign keys and existing uniqueness constraints.
2. `V2__enum_checks_and_indexes.sql`: varchar enum checks and relationship/query
   indexes, preserving current nullable enum mappings.
3. `V3__private_schema_permissions.sql`: restrict exposure and grant runtime access.

PostgreSQL types replace MySQL auto_increment, bit, inline enum, datetime and
engine clauses. Instant values use timestamp(6) with time zone; scores retain
double precision. Foreign keys keep restrictive deletion behavior; existing JPA
cascades and archive rules remain responsible for application deletion semantics.

Flyway is disabled in every runtime profile. A separate migration job (or the explicit
disposable PostgreSQL test) enables it. Its PostgreSQL
location is isolated, clean is disabled, automatic baselining is disabled, and
schema auto-creation is disabled. A nonempty unmanaged schema must be reviewed,
not silently baselined. Never edit an already-applied migration; add a version.
No seed data or data-import SQL is included.

## Backup, rehearsal and cutover (future approved work)

1. Identify the authoritative source: current H2, any local MySQL database and
   the configured `gapmap_mysql` Docker volume. Do not assume demo data is the
   only data. No MySQL database/volume was connected to during preparation.
2. Stop writers and take a fresh database-native backup. For H2, copy the closed
   database while holding an exclusive file handle. Protect backup directories
   and exports as sensitive data and keep them outside Git.
3. Rehearse restoration. Export all 15 tables, recording per-table counts and
   checksums. Preserve IDs, password hashes, token versions, ownership, historical
   results, reset-token timestamps and all relationship rows.
4. Apply migrations to an empty disposable PostgreSQL database first. Confirm
   Hibernate validation, permissions, and the full workflow suite.
5. Transform exported data rather than executing the MySQL schema/dump verbatim.
   Check email and join-code casing/duplicates, enum values, UTF-8, UTC timestamp
   interpretation, nulls, lengths, scores and referential integrity.
6. Import parents before children and join rows; topics precede prerequisite
   relationships. Preserve explicit IDs with BY DEFAULT identity columns. Reset
   every identity sequence beyond the imported maximum, handling empty tables.
7. Reconcile every table and verify the next inserted ID before any cutover.
8. After separate approval, repeat in staging and run real HTTP/frontend checks.
   Freeze writes for final transfer and retain the original database and app
   configuration. Once target writes occur, rollback needs reconciliation; merely
   switching the URL can lose those writes.

## Local verification

Submission verification on 12 November 2026 (issue #3):

| Check | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `mvn -B clean package` (H2/default suite and build) | 31 | 0 | 0 | 0 |
| `mvn -B -Ppostgres-tests test` (includes 12 PostgreSQL workflow tests) | 43 | 0 | 0 | 0 |

The React production build and local HTTPS mock API proxy/cookie check also passed.
These are separate runs: the PostgreSQL profile repeats the default tests. The tests
use local H2 and disposable Docker PostgreSQL, never the hosted database. The hosted
V1–V3 migration, 15 tables, verified runtime TLS and CRUD permissions were reported
by the project owner; this submission did not independently query Supabase.
Remote browser registration/persistence across restart, teacher access, enrollment,
assessment scoring/progress and full incorrect-answer recommendation scenarios still
need recorded manual verification. No public deployment is claimed. The bootstrap SQL
has been reviewed but the local workflow test uses its own disposable role bootstrap;
it does not establish managed Supabase administrative provisioning permissions.

From `gapmap-backend`:

```powershell
mvn clean test
mvn -Ppostgres-tests test-compile
# Requires a working local Docker engine; downloads postgres:17-alpine:
mvn -Ppostgres-tests test
# Existing demo (file-backed database; back it up before starting):
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The opt-in PostgreSQL suite reuses all 11 workflow tests and adds a migration/table
visibility/permission test. A disposable Testcontainers database provisions
separate migration and runtime logins, runs Flyway, validates Hibernate mappings,
then exercises real Spring Security and repository transactions. No Docker-based
tests are silently skipped: selecting the profile without Docker fails.

Only this disposable test overrides TLS URLs, secure cookies and seeding to allow
local fixtures. Deployment defaults remain strict. The test bootstrap contains
synthetic container-only passwords and must never be used on a real deployment.
Future TLS/pooler and remote grant checks still require staging verification.

Acceptance checklist:

- Existing H2 tests, startup, health and authenticated endpoints still work.
- PostgreSQL migrations succeed and second startup changes no schema.
- Concurrent retries save one attempt; invalid submissions roll back.
- Login/reset/logout, token invalidation, ownership and enrollment checks pass.
- Assessment CRUD/archive, scoring, reports, recommendations and mini-quizzes pass.
- Runtime cannot create/drop tables or access Flyway history; API roles cannot
  access the application schema. Verify actual inherited privileges too.
- Imported row counts, relationships, IDs, hashes, timestamps and results match.
- Verified TLS, pool limits, HTTPS cookies and secret-safe logging are confirmed.

## Historical Phase 3 preparation results — 12 November 2026

- Fresh stopped H2 backup: `verification-backups/pre-postgresql-20261112-024313/`;
  both database files copied; main-file SHA-256 matched the source.
- `mvn clean test`: 30 passed, 0 failed, 0 errors, 0 skipped. The original 26 tests
  remain; four new tests cover required settings, TLS, separate logins, profile
  conflicts and JWT length.
- `mvn -Ppostgres-tests test-compile`: passed. This compiles PostgreSQL tests;
  it does not execute them or prove migrations run successfully.
- Docker, psql and pg_ctl were not found on PATH. PostgreSQL execution has not
  been performed. No PostgreSQL test-pass claim is made.
- Demo smoke check used an isolated in-memory H2 database: startup succeeded,
  health and authenticated student dashboard returned 200, and unauthenticated
  profile access returned 401. The verification server was stopped afterward.
  The existing file-backed H2 database still matches its fresh backup checksum.
- Maven uses JDK 25, compiling for release 17. Java 17 runtime verification remains
  outstanding; no Java or Spring Boot upgrade was made.
- An inherited DEBUG environment produces verbose H2 test request logs containing
  synthetic fixture credentials. Supabase logging explicitly raises Spring and
  Hibernate to INFO; keep real secrets out of request logs and test artifacts.

The earlier count of 30 test methods included four workflow helpers. Before this
implementation there were 26 annotated tests, exactly matching Surefire. There
were then 30 real default-suite tests because four configuration tests were added.
The current suite has 31 tests after adding the runtime/migration credential regression.

References:
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://supabase.com/docs/guides/api/securing-your-api
- https://www.postgresql.org/docs/current/ddl-identity-columns.html
