# Windows PowerShell 5.1 compatible. Never dot-source the launchers themselves.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$LocalConfig = Import-PowerShellDataFile (Join-Path $PSScriptRoot 'local-startup.psd1')
$secretRoot = [IO.Path]::GetFullPath($LocalConfig.SecretsDirectory).TrimEnd('\')
$repoRoot = [IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
if ($secretRoot.Equals($repoRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $secretRoot.StartsWith($repoRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'SecretsDirectory must be outside the Git repository.'
}
function Assert-LocalFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing file: $Path" }
}
function Clear-LocalClipboard {
    # EmptyClipboard clears the clipboard itself; Set-Clipboard -Value '' can
    # pass null to Windows PowerShell 5.1's underlying text API.
    if (-not ('GapMap.LocalClipboard' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace GapMap {
    public static class LocalClipboard {
        [DllImport("user32.dll", SetLastError = true)]
        public static extern bool OpenClipboard(IntPtr owner);
        [DllImport("user32.dll", SetLastError = true)]
        public static extern bool EmptyClipboard();
        [DllImport("user32.dll", SetLastError = true)]
        public static extern bool CloseClipboard();
    }
}
'@
    }
    $opened = $false
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        if ([GapMap.LocalClipboard]::OpenClipboard([IntPtr]::Zero)) { $opened = $true; break }
        Start-Sleep -Milliseconds 50
    }
    if (-not $opened) { throw 'Clipboard is busy and could not be cleared. Clear it manually and retry setup. No credentials were saved.' }
    try {
        if (-not [GapMap.LocalClipboard]::EmptyClipboard()) {
            throw 'Clipboard could not be cleared. Clear it manually and retry setup. No credentials were saved.'
        }
    } finally { [void][GapMap.LocalClipboard]::CloseClipboard() }
}
function Read-LocalClipboardSecret([string]$Name) {
    $text = $null
    $secret = $null
    try {
        try { $text = Get-Clipboard -Raw -ErrorAction Stop }
        catch { throw "Could not read clipboard text for $Name. Copy the original value and retry setup." }
        if ([string]::IsNullOrWhiteSpace($text)) {
            throw "No text is available on the clipboard for $Name. Copy the original secret, then retry setup. No credentials were saved."
        }
        # Preserve every character, including whitespace and any trailing newline.
        $secret = ConvertTo-SecureString -String $text -AsPlainText -Force
        Clear-LocalClipboard
        return $secret
    } catch {
        if ($null -ne $secret) { $secret.Dispose() }
        throw
    } finally { $text = $null }
}
function Assert-FreePort([int]$Port) {
    $listeners = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    if ($listeners | Where-Object Port -eq $Port) { throw "Port $Port is occupied. Stop its existing server before retrying." }
}
function Get-RuntimeSecrets {
    $path = Join-Path $LocalConfig.SecretsDirectory 'credentials.clixml'
    Assert-LocalFile $path
    try { $saved = Import-Clixml -LiteralPath $path } catch { throw 'Cannot decrypt credentials. Run setup-local-secrets.ps1 as the Windows account that will launch GapMap.' }
    foreach ($name in @('SUPABASE_DB_PASSWORD','JWT_SECRET','TEACHER_INVITE_CODE')) {
        if ($saved[$name] -isnot [Security.SecureString] -or $saved[$name].Length -eq 0) { throw "Missing saved $name. Run setup-local-secrets.ps1." }
    }
    if ($saved.JWT_SECRET.Length -lt 32) { throw 'Saved JWT secret must have at least 32 characters. Import the original full secret again.' }
    return $saved
}
function Invoke-LocalProcess([hashtable]$Variables, [string]$Directory, [scriptblock]$Command) {
    $previous = @{}
    foreach ($name in $Variables.Keys) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
    Push-Location -LiteralPath $Directory
    try {
        foreach ($name in $Variables.Keys) { [Environment]::SetEnvironmentVariable($name, $Variables[$name], 'Process') }
        & $Command
        if ($LASTEXITCODE -ne 0) { throw "Server exited with code $LASTEXITCODE. Review the server error above." }
    } finally {
        foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
        Pop-Location
        $Variables.Clear()
    }
}
