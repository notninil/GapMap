# Trusted localhost HTTPS on Windows

Local certificate setup is complete. Supabase backend startup is NOT performed.

| Endpoint | URL |
| --- | --- |
| React/Vite | https://localhost:5173 |
| Spring Boot | https://localhost:8443 |
| Vite `/api` upstream | https://127.0.0.1:8443 |

The Axios client already uses relative `/api` URLs and credentials. Vite forwards
these requests over verified HTTPS to Spring Boot, preserving secure cookies.
Use `localhost` consistently in the browser, not a mixture of localhost and IPs.
No CORS wildcard or certificate-verification bypass is introduced.

## Certificates (outside Git)

Created with official mkcert v1.4.4:

- `D:\GapMap-Secrets\https\localhost.pem`: localhost server certificate.
- `D:\GapMap-Secrets\https\localhost-key.pem`: private key; do not share.
- `D:\GapMap-Secrets\https\ca\rootCA.pem`: local CA certificate.
- `D:\GapMap-Secrets\https\ca\rootCA-key.pem`: CA private key; never share or commit.

The certificate covers localhost, 127.0.0.1, and ::1. The CA was installed in the
current Windows user's Trusted Root Certification Authorities store. Edge/Chrome
should trust it after restart; Firefox may need the public CA imported into its
own certificate store. Never bypass a browser certificate warning.
Do not use this development CA/certificate in production. Store the secrets folder
on a user-restricted local drive. Keep the Supabase database CA separate; the local
CA is for localhost only. Private-key extensions are also ignored by Git defensively.

For a new developer machine, first obtain the Windows mkcert binary from the official
release linked below and save it as `D:\GapMap-Secrets\https\mkcert.exe`. Create the
directory if needed. Run the creation/trust commands below as that Windows user.
These steps were already executed on the original development machine; do not
regenerate existing keys casually or copy another developer's CA private key.

```powershell
$tlsDir = 'D:\GapMap-Secrets\https'
New-Item -ItemType Directory -Path $tlsDir -Force | Out-Null
$env:CAROOT = Join-Path $tlsDir 'ca'
& "$tlsDir\mkcert.exe" -cert-file "$tlsDir\localhost.pem" -key-file "$tlsDir\localhost-key.pem" localhost 127.0.0.1 ::1
if ($LASTEXITCODE -ne 0) { throw 'Certificate creation failed' }
certutil -user -addstore Root "$env:CAROOT\rootCA.pem"
if ($LASTEXITCODE -ne 0) { throw 'CA trust installation failed' }
```

## Frontend launch (future command)

In a fresh PowerShell terminal:

```powershell
Set-Location 'D:\Downloads\GapMap_Full_Application\gapmap\gapmap-frontend'
$env:GAPMAP_HTTPS_CERT = 'D:\GapMap-Secrets\https\localhost.pem'
$env:GAPMAP_HTTPS_KEY = 'D:\GapMap-Secrets\https\localhost-key.pem'
$env:GAPMAP_HTTPS_CA = 'D:\GapMap-Secrets\https\ca\rootCA.pem'
$env:API_PROXY_TARGET = 'https://127.0.0.1:8443'
npm run dev -- --host localhost --port 5173 --strictPort
```

Vite verifies the upstream using an HTTPS agent with this CA. `secure: true`
and `rejectUnauthorized: true` remain enabled. Missing TLS files, a partial
certificate/key pair, or HTTPS mode without an HTTPS upstream/CA fails startup.
The strict port keeps the origin exactly `https://localhost:5173`.

## Backend launch (prepared only; do not execute yet)

Follow the full build, database TLS, masked runtime-secret prompts, and launch block
in [SUPABASE-RUNTIME.md](SUPABASE-RUNTIME.md). It now selects
`supabase,local-https`, sets the local certificate paths, and fixes
`FRONTEND_ORIGIN=https://localhost:5173`.

The `local-https` profile binds Spring Boot to loopback 127.0.0.1:8443 and loads
the PEM certificate/key directly. No keystore password, Java truststore edit, or
HTTP reverse proxy is needed. Database JDBC independently retains `verify-full`
and its separate database CA path. Flyway remains disabled and Hibernate validates.

After separately authorized startup, open https://localhost:5173 and check
https://localhost:5173/api/health. Expect 200 and `demoLogin=false`. Browser sign-in
cookies remain `Secure; HttpOnly; SameSite=Strict; Path=/api`.
No account creation or database writes are part of HTTPS verification.

## Local checks without Spring Boot or Supabase

In the frontend terminal with the HTTPS variables above:

```powershell
node scripts/check-local-https.mjs
npm run build
```

The check briefly serves an HTTPS mock API on 8443 and Vite on 5173, checks the
verified frontend-to-backend TLS path and cookie attributes, then closes both.
It uses a synthetic non-authentication cookie and never contacts a database.
Stop other processes using those ports before running it.

## Existing HTTP H2 demo

Use fresh terminals without the HTTPS/Supabase environment variables:

```powershell
# Backend terminal (existing file-backed demo; unchanged behavior):
Set-Location 'D:\Downloads\GapMap_Full_Application\gapmap\gapmap-backend'
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

```powershell
# Frontend terminal:
Set-Location 'D:\Downloads\GapMap_Full_Application\gapmap\gapmap-frontend'
npm run dev
```

This retains HTTP localhost:5173 -> localhost:8080 and the existing H2 settings.
Do not combine `demo` and `supabase`.

References:
- https://github.com/FiloSottile/mkcert
- https://github.com/FiloSottile/mkcert/releases/tag/v1.4.4
- https://vite.dev/config/server-options
- https://docs.spring.io/spring-boot/3.4/how-to/webserver.html
