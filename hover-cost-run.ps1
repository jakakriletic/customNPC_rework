# Q92: one measurement per workload, data only; preserve the existing perf conditions.
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'testworld.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Fresh perf world setup failed.' }
& (Join-Path $PSScriptRoot 'perf-run.ps1') -Variants @('leteci', 'leteci-boj') -Counts 200 -FlightMode 1
exit $LASTEXITCODE
