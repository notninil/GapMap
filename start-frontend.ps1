[CmdletBinding()]
param([switch]$CheckOnly)
. (Join-Path $PSScriptRoot 'local-startup-common.ps1')
$frontend = Join-Path $PSScriptRoot 'gapmap-frontend'
foreach ($path in @($LocalConfig.Certificate, $LocalConfig.PrivateKey, $LocalConfig.HTTPSCA, (Join-Path $frontend 'package.json'))) { Assert-LocalFile $path }
if (-not (Test-Path (Join-Path $frontend 'node_modules\vite'))) { throw 'Frontend dependencies missing. Run npm ci in gapmap-frontend.' }
Get-Command npm.cmd,node.exe -ErrorAction Stop | Out-Null
Assert-FreePort 5173
# Explicit CA, hostname verification, and no certificate bypass.
$healthCheck = @'
const https = require('node:https');
const fs = require('node:fs');
const req = https.get('https://127.0.0.1:8443/api/health', {
  ca: fs.readFileSync(process.argv[1]), rejectUnauthorized: true, timeout: 5000
}, res => { res.resume(); process.exitCode = res.statusCode === 200 ? 0 : 1; });
req.on('timeout', () => req.destroy());
req.on('error', () => { process.exitCode = 1; });
'@
& node.exe -e $healthCheck $LocalConfig.HTTPSCA
if ($LASTEXITCODE -ne 0) { throw 'Backend HTTPS health check failed. Start .\start-backend.ps1 first, wait for Spring Boot to finish, then retry. Check certificate validity and CA paths if it is already running.' }
if ($CheckOnly) { Write-Host 'Frontend preflight passed, including verified backend HTTPS. No Vite started.'; return }
$variables = @{
    GAPMAP_HTTPS_CERT = $LocalConfig.Certificate
    GAPMAP_HTTPS_KEY = $LocalConfig.PrivateKey
    GAPMAP_HTTPS_CA = $LocalConfig.HTTPSCA
    API_PROXY_TARGET = 'https://127.0.0.1:8443'
    NODE_TLS_REJECT_UNAUTHORIZED = $null
}
Write-Host 'Vite will print https://localhost:5173 when ready. Stop with Ctrl+C.'
Invoke-LocalProcess $variables $frontend { & npm.cmd run dev -- --host localhost --port 5173 --strictPort }
