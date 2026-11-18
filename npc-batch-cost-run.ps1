<# Controller-only M14.2 cost pair. Exactly one unchanged K scene per switch value.
   Performance is data, while K1-K8/K20, identical inputs and conditions are required.
   No image readback, state audit or stack sampler runs in the measurement window. #>
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m142-batch-cost-$stamp"
$inputs = @{}
$cells = @()
$exitCode = 1

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Unchanged {
    foreach ($path in $inputs.Keys) {
        if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -cne $inputs[$path]) {
            throw "Changed measurement input $path"
        }
    }
    Write-Host "OK $($inputs.Count) unchanged measurement inputs"
}

try {
    $paths = @($PSCommandPath, (Join-Path $root 'k-run.ps1'), (Join-Path $root 'perf-run.ps1'),
        (Join-Path $root 'meritve-lib.ps1'), (Join-Path $root 'dev/build.gradle'),
        (Join-Path $root 'environment-lock.json'))
    $paths += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/src/patch/java') -Recurse -File | Select-Object -ExpandProperty FullName)
    $paths += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/testworld') -Recurse -File | Select-Object -ExpandProperty FullName)
    $environment = Get-Content -LiteralPath (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($artifact in $environment.artifacts.PSObject.Properties) {
        $path = Join-Path $root $artifact.Name
        Assert ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ceq $artifact.Value) "locked artifact $($artifact.Name)"
        $paths += $path
    }
    foreach ($path in $paths) { $inputs[$path] = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash }
    foreach ($mode in 0, 1) {
        Unchanged
        $out = "$prefix-$mode.json"
        # Same accepted M5-K.1 conditions and other rendering defaults, only RwNpcInstancing differs.
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'k-run.ps1') `
            -Variant idle -N 500 -Warmup 30 -Measure 60 -RenderDistance 8 -Fov 70 `
            -Pokrivalo 1 -Napisi 1 -Lod 0 -Far 2 -ImpostorStart 56 -RenderBox 1 `
            -AnimCore 0 -Instancing $mode -JsonPath $out |
            Tee-Object -FilePath "$prefix-$mode.log" | ForEach-Object { Write-Host $_ }
        Assert ($LASTEXITCODE -eq 0) "K harness passed at RwNpcInstancing=$mode"
        Unchanged
        $cell = Get-Content -LiteralPath $out -Raw | ConvertFrom-Json
        Assert ($cell.uspeh -eq $true -and $cell.padlih -eq 0) "valid cost cell $mode"
        Assert ($cell.odtis.rwNpcInstancing -eq $mode -and $cell.odtis.jfr -eq 0 -and
            $null -eq $cell.odtis.vzorci -and $cell.odtis.skritGui -eq 0 -and
            $cell.odtis.sirina -eq 854 -and $cell.odtis.visina -eq 480) "fixed conditions and no profiler $mode"
        $cells += $cell
    }
    $offConditions = $cells[0].odtis | Select-Object * -ExcludeProperty rwNpcInstancing | ConvertTo-Json -Compress
    $onConditions = $cells[1].odtis | Select-Object * -ExcludeProperty rwNpcInstancing | ConvertTo-Json -Compress
    Assert ($offConditions -ceq $onConditions) 'only candidate switch differs'
    $proof = [ordered]@{
        success = $true
        package = 'M14.2'
        condition = 'one 0/1 idle-500 pair, M5-K.1 settings, 30+60 seconds, performance data only'
        inputs = $inputs
        cells = $cells
        p95ChangePercent = 100 * ($cells[1].velicine.'frame.p95' / $cells[0].velicine.'frame.p95' - 1)
    }
    $proof | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M14.2 cost evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
}
exit $exitCode
