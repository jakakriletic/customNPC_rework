# M4.15a: one M5.8 coarse ladder per side (data, not a capacity/default-on claim).
param([Parameter(Mandatory = $true)][ValidateSet(0, 1)][int]$Backend)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'testworld.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Fresh perf world setup failed.' }
& (Join-Path $PSScriptRoot 'perf-run.ps1') -Variants @('boj', 'nedosegljiva') -Counts @('500', '1000', '2000') `
    -WarmupSeconds 60 -Seconds 120 -NavBackend $Backend
exit $LASTEXITCODE
