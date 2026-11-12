# Phase 4: Supabase runtime launch and persistence checks

Prepared only. Do not run the launch or remote account checks until separately authorized.
No runtime password or other deployment secret belongs in this repository.

## Runtime configuration

The `supabase` profile uses `gapmap_app`, schema `gapmap`, verified TLS,
Hibernate `validate`, no SQL initialization, no Flyway execution, no seeds,
no demo login, and secure HTTP-only cookies. Migration secrets are unnecessary.
The local PostgreSQL test profile explicitly enables Flyway for its disposable container.
The default and H2 `demo` configurations are unchanged.

Use the existing session pooler endpoint on port 5432. Its username is
`gapmap_app.<project-ref>`; the database SQL role remains `gapmap_app`.
Keep the trusted PEM CA outside Git, for example `D:\GapMap-Secrets\ca.crt`.
Windows Java needs its actual Windows path, not Docker's `/certs/ca.crt`.
The URL below encodes the path, uses pgJDBC's default certificate-verifying factory,
and requires both a trusted chain and a matching server hostname. Do not select a
nonvalidating SSL factory or downgrade to `sslmode=require`.

## Exact PowerShell preparation and launch

Use a new, non-transcribed PowerShell session. Build before setting the remote profile.
The first block builds/tests locally; it does not launch the standalone backend.

```powershell
Set-Location 'D:\Downloads\GapMap_Full_Application\gapmap\gapmap-backend'
mvn -B package
if ($LASTEXITCODE -ne 0) { throw 'Build/tests failed' }
```

The next block is a future launch command; it has NOT been executed.
Enter secrets only at masked prompts. The JWT secret must be a stable, password-manager
generated random value of at least 32 characters, reused across restarts/instances.
The frontend origin must exactly match the HTTPS browser origin. Local HTTPS is
configured in [LOCAL-HTTPS.md](LOCAL-HTTPS.md); database TLS remains separate from HTTP-server TLS.

```powershell
Set-Location 'D:\Downloads\GapMap_Full_Application\gapmap\gapmap-backend'
$ErrorActionPreference = 'Stop'
$caPath = (Resolve-Path -LiteralPath (Read-Host 'Trusted CA PEM file path')).Path
$poolerHost = Read-Host 'Exact session pooler hostname from Supabase Connect'
$projectRef = Read-Host 'Supabase project reference'
$env:SUPABASE_DB_URL = 'jdbc:postgresql://' + $poolerHost + ':5432/postgres?sslmode=verify-full&sslrootcert=' + [Uri]::EscapeDataString($caPath.Replace('\', '/'))
$env:SUPABASE_DB_USER = 'gapmap_app.' + $projectRef
$env:SUPABASE_DB_POOL_SIZE = '5'
$env:FRONTEND_ORIGIN = 'https://localhost:5173'
$env:GAPMAP_HTTPS_CERT = 'D:\GapMap-Secrets\https\localhost.pem'
$env:GAPMAP_HTTPS_KEY = 'D:\GapMap-Secrets\https\localhost-key.pem'
$env:SPRING_PROFILES_ACTIVE = 'supabase,local-https'
$env:SPRING_FLYWAY_ENABLED = 'false'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:SPRING_SQL_INIT_MODE = 'never'
Remove-Item Env:SUPABASE_MIGRATION_USER, Env:SUPABASE_MIGRATION_PASSWORD, Env:SPRING_FLYWAY_USER, Env:SPRING_FLYWAY_PASSWORD, Env:FLYWAY_PASSWORD -ErrorAction SilentlyContinue

function Read-ProcessSecret([string]$Name, [string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $credential = [PSCredential]::new('unused', $secure)
    [Environment]::SetEnvironmentVariable($Name, $credential.GetNetworkCredential().Password, 'Process')
    $secure.Dispose()
}

try {
    Read-ProcessSecret 'SUPABASE_DB_PASSWORD' 'Runtime database password'
    Read-ProcessSecret 'JWT_SECRET' 'Stable JWT secret (at least 32 characters)'
    Read-ProcessSecret 'TEACHER_INVITE_CODE' 'Teacher invitation secret'
    java -jar .\target\gapmap-1.0.0.jar
    if ($LASTEXITCODE -ne 0) { throw 'Backend exited with an error' }
}
finally {
    Remove-Item Env:SUPABASE_DB_PASSWORD, Env:JWT_SECRET, Env:TEACHER_INVITE_CODE -ErrorAction SilentlyContinue
}
```

Environment variables are process-local and necessarily readable by the application;
masked prompts prevent plaintext history entries, not access by privileged processes.
Close this dedicated shell afterward. For deployment, inject runtime secrets from the
hosting secret manager. Never load the Flyway credential file into the backend.
Inspect startup for only `supabase,local-https` profiles, schema validation success, and no
Flyway migration or seed execution. Stop on failure; do not relax TLS or enable DDL.

## Future registration/login persistence checks

These tests create one account and therefore require separate authorization.
Use a dedicated approved test identity; never reuse a real user's password.
Use the HTTPS frontend/reverse-proxy origin so `Secure; HttpOnly; SameSite=Strict`
cookies function normally. Do not disable secure cookies for this profile.

1. `GET /api/health` returns 200 with `demoLogin=false`.
2. `GET /api/auth/me` without a session is rejected (401).
3. Register a new student using `POST /api/auth/register` with JSON fields
   `name`, `email`, `password`, and `role: "STUDENT"`. Use 8–72 password characters
   and at most 72 UTF-8 bytes. Expect 200, a numeric user ID, no password/hash in the
   response, and the secure `gapmap_session` cookie.
4. `GET /api/auth/me` with that cookie returns the same ID/email/role.
5. `POST /api/auth/logout` returns 204; `/api/auth/me` is rejected afterward.
6. `POST /api/auth/login` with a wrong password returns 401; the correct password
   returns 200 and the original ID. Re-registering the same email must be rejected.
7. Stop and relaunch the backend using the same JWT secret and settings. With a fresh
   browser session, log in again and confirm the same ID; no seeds or DDL should run.
8. Optionally use an approved read-only SQL client to check only the test user's ID,
   role, and that `password_hash` has a BCrypt prefix. Do not select/display the hash,
   log request bodies, or export cookies. Never publish the test password or token.
9. Keep the test record until cleanup is separately authorized. Do not delete rows or
   change existing accounts as part of this preparation.

All fifteen tables stay under the existing database grants; no schema migrations,
data imports, account creation, or persistence checks were performed during preparation.

References:
- https://jdbc.postgresql.org/documentation/ssl/
- https://supabase.com/docs/guides/database/connecting-to-postgres
