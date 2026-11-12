# Synthetic tests only. Never reads or changes the actual Windows clipboard.
$ErrorActionPreference = 'Stop'
. (Join-Path (Split-Path $PSScriptRoot -Parent) 'local-startup-common.ps1')
function Get-Clipboard { param([switch]$Raw, [string]$ErrorAction) return $script:dummyClipboard }
function Clear-LocalClipboard {
    $script:clearCount++
    if ($script:failClear) { throw 'Synthetic clearing failure' }
    $script:dummyClipboard = $null
}
$script:failClear = $false
$script:clearCount = 0
foreach ($empty in @($null, '', ' ', "`r`n")) {
    $script:dummyClipboard = $empty
    $rejected = $false
    try { $unexpected = Read-LocalClipboardSecret 'TEST_SECRET' }
    catch { $rejected = $_.Exception.Message -like 'No text is available on the clipboard*' }
    if (-not $rejected) { throw 'Empty/non-text clipboard was not usefully rejected.' }
}
if ($script:clearCount -ne 0) { throw 'Empty clipboard attempted credential import/clearing.' }
Write-Output 'PASS: null, empty, whitespace and newline-only clipboard rejected.'
foreach ($dummy in @(('0123456789abcdef' * 4), "  synthetic value `r`n")) {
    $script:dummyClipboard = $dummy
    $secret = Read-LocalClipboardSecret 'TEST_SECRET'
    try {
        if (([PSCredential]::new('test', $secret)).GetNetworkCredential().Password -cne $dummy) {
            throw 'Synthetic value was modified or truncated.'
        }
        if ($null -ne $script:dummyClipboard) { throw 'Clipboard was not cleared after import.' }
    } finally { $secret.Dispose() }
}
if ($script:clearCount -ne 2) { throw 'Incorrect successful-import clearing count.' }
Write-Output 'PASS: full 64-character value and whitespace/newline preserved; clipboard cleared.'
$script:dummyClipboard = 'synthetic failure case'
$script:failClear = $true
$rejected = $false
try { $unexpected = Read-LocalClipboardSecret 'TEST_SECRET' }
catch { $rejected = $_.Exception.Message -eq 'Synthetic clearing failure' }
if (-not $rejected) { throw 'Clipboard clearing failure was ignored.' }
Write-Output 'PASS: clearing failure stops import.'
