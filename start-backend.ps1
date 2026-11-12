[CmdletBinding()]
param([switch]$CheckOnly)
. (Join-Path $PSScriptRoot 'local-startup-common.ps1')
$backend = Join-Path $PSScriptRoot 'gapmap-backend'
$jar = Join-Path $backend 'target\gapmap-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'JAR missing. With backend stopped, build in a fresh terminal: cd gapmap-backend; mvn -B package' }
Assert-LocalFile $LocalConfig.DatabaseCA
Assert-LocalFile $LocalConfig.Certificate
Assert-LocalFile $LocalConfig.PrivateKey
Get-Command java.exe -ErrorAction Stop | Out-Null
Assert-FreePort 8443
$connectionPath = Join-Path $LocalConfig.SecretsDirectory 'connection.clixml'
Assert-LocalFile $connectionPath
$connection = Import-Clixml -LiteralPath $connectionPath
if ($connection.PoolerHost -notmatch '^aws-[a-zA-Z0-9.-]+\.pooler\.supabase\.com$' -or $connection.ProjectRef -notmatch '^[a-z0-9]+$') { throw 'Invalid saved session pooler configuration.' }
$secrets = Get-RuntimeSecrets
$variables = @{
    SPRING_PROFILES_ACTIVE = 'supabase,local-https'
    FRONTEND_ORIGIN = 'https://localhost:5173'
    SUPABASE_DB_URL = ('jdbc:postgresql://' + $connection.PoolerHost + ':5432/postgres?sslmode=verify-full&sslrootcert=' + [Uri]::EscapeDataString($LocalConfig.DatabaseCA.Replace('\','/')))
    SUPABASE_DB_USER = ('gapmap_app.' + $connection.ProjectRef)
    SUPABASE_DB_POOL_SIZE = '5'
    GAPMAP_HTTPS_CERT = $LocalConfig.Certificate
    GAPMAP_HTTPS_KEY = $LocalConfig.PrivateKey
    SPRING_FLYWAY_ENABLED = 'false'
    SPRING_SQL_INIT_MODE = 'never'
    SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
    APP_SEED = 'false'
    SEED_DATA = 'false'
    APP_DEMO_LOGIN = 'false'
    APP_COOKIE_SECURE = 'true'
    SERVER_PORT = '8443'
    SERVER_ADDRESS = '127.0.0.1'
    SERVER_SSL_ENABLED = 'true'
    SPRING_APPLICATION_JSON = $null
    SPRING_CONFIG_LOCATION = $null
    SPRING_CONFIG_ADDITIONAL_LOCATION = $null
    JAVA_TOOL_OPTIONS = $null
    JDK_JAVA_OPTIONS = $null
    _JAVA_OPTIONS = $null
    SUPABASE_MIGRATION_USER = $null
    SUPABASE_MIGRATION_PASSWORD = $null
    SPRING_FLYWAY_USER = $null
    SPRING_FLYWAY_PASSWORD = $null
    FLYWAY_PASSWORD = $null
}
try {
    if ($CheckOnly) { Write-Host 'Backend preflight passed, including DPAPI credential loading. No server started.'; return }
    foreach ($name in $secrets.Keys) { $variables[$name] = ([PSCredential]::new('unused', $secrets[$name])).GetNetworkCredential().Password }
    Write-Host 'Starting backend at https://localhost:8443. Stop with Ctrl+C.'
    Invoke-LocalProcess $variables $backend { & java.exe -jar $jar }
} finally {
    foreach ($value in $secrets.Values) { $value.Dispose() }
    $variables.Clear()
}
