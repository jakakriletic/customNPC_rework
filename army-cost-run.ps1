# M11.10 (Q374-Q376): cost of a 200 NPC army march on the M2.6 fixture and conditions, one repetition (RwArmies stays 0).
#   pair     idle-200 control + pohod-200 in one server: RwArmies=1, RwArmyMaxMarches=0, rwdiag forced chunks
#   tickets  pohod-200: RwArmyMaxMarches=2, spawn moved away, no rwdiag chunks; only M11.6 tickets load the world
# The switches go into dev/run/config for this run only; the original bytes are restored and verified afterwards.
param([Parameter(Mandatory = $true)][ValidateSet('pair', 'tickets')][string]$Cell)
$ErrorActionPreference = 'Stop'
$cfg = Join-Path $PSScriptRoot 'dev/run/config/CustomNpcs.cfg'
& (Join-Path $PSScriptRoot 'testworld.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Fresh perf world setup failed.' }
$original = [IO.File]::ReadAllBytes($cfg)
$code = 1
try {
    $text = [IO.File]::ReadAllText($cfg)
    foreach ($name in @('RwArmies', 'RwArmyMaxMarches', 'RwArmyCompany')) { $text = [regex]::Replace($text, "(?m)^$name=.*\r?\n?", '') }
    $marches = if ($Cell -eq 'tickets') { 2 } else { 0 }
    [IO.File]::WriteAllText($cfg, $text + "`nRwArmies=1`nRwArmyMaxMarches=$marches`nRwArmyCompany=8`n", (New-Object Text.UTF8Encoding $false))
    if ($Cell -eq 'tickets') {
        & (Join-Path $PSScriptRoot 'perf-run.ps1') -Variants @('pohod') -Counts @('200') -WarmupSeconds 60 -Seconds 180 -PohodBrezChunkov
    } else {
        & (Join-Path $PSScriptRoot 'perf-run.ps1') -Variants @('idle', 'pohod') -Counts @('200') -WarmupSeconds 60 -Seconds 180
    }
    $code = $LASTEXITCODE
} finally {
    [IO.File]::WriteAllBytes($cfg, $original)
}
if ((Compare-Object $original ([IO.File]::ReadAllBytes($cfg))).Count -ne 0) { throw 'CustomNpcs.cfg was not restored.' }
exit $code
