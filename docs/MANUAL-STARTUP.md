# Manual Windows startup

Run setup and launchers as your normal Windows account (Windows PowerShell 5.1).
No launcher runs automatically. Nothing here builds, migrates, seeds, or starts
the application until you explicitly run a launcher without `-CheckOnly`.

## One-time setup

If PowerShell reports that scripts are disabled, run this once in your own terminal:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

An organizational policy may override this; use your administrator's approved
script policy in that case. Administrator elevation is not otherwise required.

From the project root:

```powershell
.\setup-local-secrets.ps1
.\start-backend.ps1 -CheckOnly
```

For each secret, choose `C` (clipboard), `E` (an existing environment value in
this terminal), or `M` (masked typing). For the JWT secret, use `C` to import
the **entire original secret** from your existing secure source. Copy each value
only when its prompt appears. Clipboard import immediately clears the current
clipboard. It does not manage Windows clipboard history or a password manager's
history; avoid clipboard history/sync for secrets. Do not paste secrets into chat,
PowerShell commands, or a transcribed terminal. No secret is generated or rotated.
JWT length must be at least 32 characters; the value is never displayed.

Enter the existing **session** pooler hostname and project reference from Supabase
Connect. Do not import `flyway.conf`: it contains migration credentials rather
than the restricted runtime login. Setup constructs `gapmap_app.<project-ref>` on
port 5432. It does not connect to Supabase. Host/reference are reused on updates;
to change these non-secret settings, edit the external `connection.clixml` values.

Secrets are Windows DPAPI CurrentUser-encrypted SecureStrings in
`D:\GapMap-Secrets\runtime\credentials.clixml`; non-secret host/reference are in
`connection.clixml` beside it. The dedicated runtime directory's ACL permits your
account only. Setup checks an encryption/decryption round trip before replacing
the saved credential file. The same Windows account and machine must run launchers.
Existing credentials cannot be overwritten without `-Update`.

Certificate and CA locations can be adjusted in `local-startup.psd1`. It contains
only non-secret paths. Keep its SecretsDirectory outside the repository. HTTPS
keys and credentials are never copied into the project.

## Every day

Terminal 1, from the project root:

```powershell
.\start-backend.ps1
```

Wait for Spring Boot startup to finish. Terminal 2, from the project root:

```powershell
.\start-frontend.ps1
```

Open **https://localhost:5173** when Vite reports ready. Press **Ctrl+C** in each
terminal to stop its server. Launchers use their own locations, so an absolute
script path also works from another directory. Environment changes are temporary
and restored when the command exits; no machine-wide variables are saved. Secrets
are decrypted only by the backend launcher and passed through process environment,
never command-line arguments. The Java process necessarily holds them in memory.

## Updates and troubleshooting

- Changed credentials: run `.\setup-local-secrets.ps1 -Update`, supply all three
  intended values, then restart the backend. Preserve the original JWT unless you
  intend to invalidate existing sessions. Do not rerun setup just to restart.
- Preflight only: `.\start-backend.ps1 -CheckOnly` validates files, port and saved
  credentials without Java. `.\start-frontend.ps1 -CheckOnly` requires an already
  running backend and performs a verified HTTPS GET to `/api/health` without Vite.
- Missing/decryption-failed credentials: run setup in your normal Windows account.
  Sandbox impersonation may not support DPAPI; no actual credentials were imported
  during implementation. After setup succeeds, backend `-CheckOnly` verifies the
  saved file under your account without printing values.
- Missing JAR: stop the backend first. In a fresh terminal without remote database
  profile/environment settings, run `cd gapmap-backend`, then `mvn -B package`.
  No automatic build is attempted, so a running JAR is never overwritten.
- Missing frontend dependencies: run `npm ci` in `gapmap-frontend`.
- Occupied port: stop the existing owner of 8443 or 5173; launchers do not kill it
  or silently choose another port.
- Backend unreachable: start it first and wait for successful startup. Check its
  error output, certificate expiry/trust and configured CA paths. TLS verification
  stays enabled; do not bypass certificate warnings.
- Database connection failure: verify saved session pooler hostname/reference,
  runtime password, network and database CA. Do not substitute the migration role,
  enable Flyway, or relax `sslmode=verify-full`.

No application/authentication code, Vite configuration, branch, commit, push or PR
is changed by these scripts. Keep these local files uncommitted as requested.
