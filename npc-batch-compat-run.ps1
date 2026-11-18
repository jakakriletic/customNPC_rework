<# Controller-only M14.3a native GL/lifecycle proof; -WholeRenderer selects M14.3b1
   frozen whole living-renderer fixtures instead. One isolated dev client, no cost
   window. Neither mode installs or simulates OptiFine. #>
param([switch]$WholeRenderer)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = if ($WholeRenderer) { 'm143b-scene' } else { 'm143a-native' }
$package = if ($WholeRenderer) { 'M14.3b1' } else { 'M14.3a' }
$prefix = Join-Path $root "audit/$tag-$stamp"
$work = Join-Path $root "dev/run-$tag/$stamp"
$lock = $null
$client = $null
$exitCode = 1
$hashes = @{}
$owned = @{}
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Fingerprint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Guard {
    $roots = @(); if ($null -ne $client) { $roots += $client.Id }
    $snapshot = @(Get-CimInstance Win32_Process)
    $tree = @{}; foreach ($id in @($roots) + @($owned.Keys)) { $tree[[int]$id] = $true }
    $grew = $true
    while ($grew) {
        $grew = $false
        foreach ($p in $snapshot) {
            if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) {
                $tree[[int]$p.ProcessId] = $true; $grew = $true
            }
        }
    }
    foreach ($p in $snapshot) {
        if ($p.Name -in @('java.exe', 'javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true }
    }
    $foreign = @(Get-NavForeignJava -Roots (@($roots) + @($owned.Keys)))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged {
    foreach ($path in $hashes.Keys) {
        if ((Fingerprint $path) -cne $hashes[$path]) { throw "Changed protected input: $path" }
    }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo $tag
    Guard
    $environment = Get-Content -LiteralPath (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($artifact in $environment.artifacts.PSObject.Properties) {
        $path = Join-Path $root $artifact.Name
        Assert ((Fingerprint $path) -ceq $artifact.Value) "locked artifact $($artifact.Name)"
        $hashes[$path] = Fingerprint $path
    }
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'), $PSCommandPath,
        (Join-Path $root 'meritve-lib.ps1'), (Join-Path $root 'nav-meritve-lib.ps1'), (Join-Path $root 'dev/build.gradle'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/src') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $root 'audit') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'mods'))) 'no optional rendering mods (native test only)'
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    [IO.File]::WriteAllLines($job, @('npcBatchCompat=1', ('world=' + $tag), ('wholeRenderer=' + [int]$WholeRenderer.IsPresent),
        ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" -PrwSet=RwNpcInstancing=0 > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
    Assert $client.HasExited 'client completed before timeout'
    Assert ($client.ExitCode -eq 0) 'clean client process exit'
    $text = [IO.File]::ReadAllText($log)
    Assert (-not ($text -match "NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|RWHARNESS-NAPAKA|Couldn't render entity")) 'no linkage/harness/render errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    # Check the full contract, not just the harness success flag. False rows are retained
    # in the proof/log for diagnosis of the unchanged pre-fix production code.
    $keys = @('ordinary', 'foreign-vao-same-frame', 'foreign-program-same-frame', 'extra-light', 'extra-texture',
        'child-model', 'outside-world-pass', 'ordinary-after-fallbacks', 'ordinary-after-reload', 'ordinary-after-world-rejoin')
    $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('reload-has-live-resources', 'reload-off', 'reload-off-gl',
        'reload-program-rebuilt', 'unload-has-live-resources', 'world-unload-off', 'world-unload-off-gl', 'rejoined-world')
    if ($WholeRenderer) {
        $keys = @('ordinary-name', 'walk', 'attack', 'bow-pose', 'sneak', 'skin-alex', 'tint', 'global-scale', 'part-scale',
            'dark-lightmap', 'block-lightmap', 'fog-linear', 'fog-exp', 'fog-exp2', 'hurt', 'death', 'overlay',
            'armor', 'armor-glint', 'held-item', 'riding', 'morph-zombie', 'outline', 'cancel-pre', 'ordinary-after-specials')
        $special = @('hurt', 'death', 'overlay', 'armor', 'armor-glint', 'held-item', 'riding', 'morph-zombie', 'outline', 'cancel-pre')
        $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('name-visible', 'pose-negative-control', 'uv-negative-control', 'recovery-baseline')
    }
    $checks = @($proof.checks)
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        if ($row.Count -eq 1) { Write-Host ("RESULT {0} ok={1} {2}" -f $key, $row[0].ok, $row[0].detail) }
    }
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143A DONE success=true')) 'compatibility proof success'
    $joins = if ($WholeRenderer) { 1 } else { 2 }
    Assert ($proof.joins -eq $joins -and $proof.width -eq 256 -and $proof.height -eq 256) 'real world joins and fixed FBO'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        Assert ($row.Count -eq 1 -and $row[0].ok -eq $true) $key
    }
    Assert (@($proof.rows).Count -eq $keys.Count) 'all image/state rows'
    Assert (@($proof.rows.key | Select-Object -Unique).Count -eq $keys.Count) 'distinct image/state rows'
    foreach ($key in $keys) { Assert (@($proof.rows | Where-Object { $_.key -ceq $key }).Count -eq 1) "image row $key" }
    foreach ($row in $proof.rows) {
        Assert ($row.repeatEqual -eq $true -and $row.stateEqual -eq $true -and $row.imageEqual -eq $true) "A/B/A2 $($row.key)"
        $native = if ($WholeRenderer) { $row.key -cnotin $special } else { $row.key -like 'ordinary*' }
        if ($WholeRenderer) {
            Assert ($row.eventsEqual -eq $true -and $row.visible -eq $true -and $row.featureVisible -eq $true) "whole renderer and visible feature $($row.key)"
            Assert ($row.offDraws -eq 0 -and $row.repeatDraws -eq 0) "original route at 0 $($row.key)"
            Assert ($row.draws -eq [int]$native -and ((-not $native) -or $row.fallbacks -eq 0)) "actual route $($row.key)"
            if ($native) {
                $pixels = $row.pixels
                Assert ($pixels.union -gt 0 -and $pixels.maskDiff -eq 0 -and $pixels.alphaDiff -eq 0 -and $pixels.outsideDiff -eq 0 -and
                    $pixels.depthOver -eq 0 -and $pixels.maxDepth -le 0.000001 -and $pixels.over8 -eq 0 -and
                    $pixels.maxChannel -le 8 -and $pixels.within2 * 1000 -ge $pixels.union * 999) "Q475 numeric limits $($row.key)"
            }
        } else {
            Assert ($row.draws -eq [int]$native -and $row.fallbacks -eq [int](-not $native)) "actual route $($row.key)"
        }
        Assert ($row.exact -eq (-not $native)) "bitwise fallback $($row.key)"
    }
    $client = $null
    Unchanged
    $condition = if ($WholeRenderer) { 'one isolated integrated dev client/GPU; 25 frozen whole living-renderer fixtures in FBO A/B/A2, visible features, events/layer witness/name, Q475 and bitwise special fallback; no live AI motion, rig, URL, GUI/bake, L2, shadow, cost or optional-mod proof' }
        else { 'one isolated integrated dev client, ordinary main model, frozen FBO A/B/A2, native foreign program/VAO in same frame, real reload and world leave/rejoin at switch 0; no cost or optional-mod proof' }
    @{success = $true; package = $package; proof = $out; log = $log; inputs = $hashes; condition = $condition} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "$package evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
