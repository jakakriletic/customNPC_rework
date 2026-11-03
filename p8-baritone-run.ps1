# P8: Baritone cost at N=2000 on the wide grid plus live heap after full GC (data, not a
# capacity/default-on claim). Same library, configs, fixtures and windows as M4.15a; only the
# grid width (50, so U1 can be met at N=2000, Q292) and the live-heap read-out (Q293) are new.
param([Parameter(Mandatory = $true)][ValidateSet(0, 1)][int]$Backend)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'testworld.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Fresh perf world setup failed.' }
& (Join-Path $PSScriptRoot 'perf-run.ps1') -Variants @('nedosegljiva', 'boj') -Counts @('2000') `
    -WarmupSeconds 60 -Seconds 120 -NavBackend $Backend -MrezaSirina 50 -ZiviHeap
exit $LASTEXITCODE
