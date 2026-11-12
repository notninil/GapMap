[CmdletBinding()]
param([switch]$Update)
. (Join-Path $PSScriptRoot 'local-startup-common.ps1')
$directory = $LocalConfig.SecretsDirectory
$credentialPath = Join-Path $directory 'credentials.clixml'
$connectionPath = Join-Path $directory 'connection.clixml'
if ((Test-Path -LiteralPath $credentialPath) -and -not $Update) {
    throw 'Credentials already exist. Use -Update only when you intend to replace saved values.'
}
# Export-Clixml uses Windows DPAPI CurrentUser for SecureString values.
$values = @{}
try {
    foreach ($name in @('SUPABASE_DB_PASSWORD','JWT_SECRET','TEACHER_INVITE_CODE')) {
        $mode = Read-Host "$name input: C = clipboard, E = existing process environment, M = masked prompt"
        $plain = $null
        if ($mode -eq 'C') {
            $secure = Read-LocalClipboardSecret $name
        } elseif ($mode -eq 'E') {
            $plain = [Environment]::GetEnvironmentVariable($name, 'Process')
            if ([string]::IsNullOrWhiteSpace($plain)) { throw "Process environment has no $name." }
            $secure = ConvertTo-SecureString -String $plain -AsPlainText -Force
        } elseif ($mode -eq 'M') {
            $secure = Read-Host "Enter ORIGINAL $name" -AsSecureString
        } else { throw 'Choose C, E, or M. No credentials were changed.' }
        $plain = $null
        if ($secure.Length -eq 0 -or ($name -eq 'JWT_SECRET' -and $secure.Length -lt 32)) {
            throw "Invalid $name length. JWT requires at least 32 characters. No credentials were changed."
        }
        $values[$name] = $secure
    }
    if (Test-Path -LiteralPath $connectionPath) {
        $connection = Import-Clixml -LiteralPath $connectionPath
    } else {
        $connection = @{
            PoolerHost = (Read-Host 'Exact existing Supabase SESSION pooler hostname (no URL or port)').Trim()
            ProjectRef = (Read-Host 'Existing Supabase project reference').Trim()
        }
    }
    if ($connection.PoolerHost -notmatch '^aws-[a-zA-Z0-9.-]+\.pooler\.supabase\.com$' -or $connection.ProjectRef -notmatch '^[a-z0-9]+$') {
        throw 'Invalid session pooler hostname or project reference. Copy existing values from Supabase Connect.'
    }
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetOwner($sid)
    $acl.SetAccessRuleProtection($true, $false)
    $rule = New-Object Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $directory -AclObject $acl
    $temp = Join-Path $directory ([IO.Path]::GetRandomFileName())
    try {
        $values | Export-Clixml -LiteralPath $temp
        $check = Import-Clixml -LiteralPath $temp
        foreach ($name in $values.Keys) {
            if ($check[$name] -isnot [Security.SecureString] -or $check[$name].Length -ne $values[$name].Length) { throw 'Credential encryption verification failed.' }
            if (([PSCredential]::new('verify', $check[$name])).GetNetworkCredential().Password -cne
                ([PSCredential]::new('verify', $values[$name])).GetNetworkCredential().Password) {
                throw 'Credential encryption round trip failed.'
            }
        }
        $connection | Export-Clixml -LiteralPath $connectionPath
        Move-Item -LiteralPath $temp -Destination $credentialPath -Force
    } finally { if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp } }
    Write-Host 'Credentials encrypted and verified for this Windows account. No secret values were displayed.'
} finally {
    $plain = $null
    if (Get-Variable check -ErrorAction SilentlyContinue) {
        foreach ($value in $check.Values) { $value.Dispose() }
    }
    foreach ($value in $values.Values) { $value.Dispose() }
    $values.Clear()
}
