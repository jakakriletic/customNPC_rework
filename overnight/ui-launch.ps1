param(
    [ValidateSet('Open','Login')][string]$Mode,
    [string]$Url,
    [string]$ConfigFile,
    [ValidatePattern('^[a-z0-9-]+$')][string]$ProfileId
)
$ErrorActionPreference = 'Stop'
if ($Mode -eq 'Open') {
    if ($Url -notmatch '^http://127\.0\.0\.1:\d+$') { throw 'Neveljaven lokalni URL' }
    Start-Process $Url
} else {
    $runner = Join-Path (Split-Path $PSScriptRoot -Parent) 'nocni-run.ps1'
    if ($runner.Contains('"') -or $ConfigFile.Contains('"')) { throw 'Neveljavna pot' }
    Start-Process powershell.exe -WindowStyle Normal -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $runner + '"'), 'login', $ProfileId, '--config', ('"' + $ConfigFile + '"'))
}
