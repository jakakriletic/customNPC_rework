<# Controller-only M14.3b5 real path AI/combat and same-frame live-pose A/B/A2; no cost window. #>
param([switch]$DepthProbe, [switch]$DepthRegression)
$ErrorActionPreference = 'Stop'
if ($DepthProbe -and $DepthRegression) { throw 'Choose one depth mode' }
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = 'm143b-motion'
if ($DepthProbe) { $tag = 'm143b-depth' }
if ($DepthRegression) { $tag = 'm143b-depth-regression' }
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
    [IO.File]::WriteAllLines($job, @('npcBatchMotion=1', ('depthProbe=' + [int]$DepthProbe.IsPresent),
        ('depthRegression=' + [int]$DepthRegression.IsPresent), ('world=' + $tag), ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
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
    if ($proof.error) { Write-Host ("PROOF ERROR " + $proof.error) }
    $failedRows = @((@($proof.rows) + @($proof.probes)) | Where-Object { $_.success -ne $true })
    if ($failedRows.Count) {
        Write-Host ("FAILED IMAGE ROWS " + $failedRows.Count)
        foreach ($row in @($failedRows | Select-Object -First 8)) {
            Write-Host ("FAILED tick={0} stage={1} category={2} pixels={3}" -f $row.tick, $row.stage, $row.category,
                ($row.pixels | ConvertTo-Json -Compress))
        }
        $diagnosed = @($failedRows | Where-Object { $null -ne $_.diagnostic })
        if ($diagnosed.Count) {
            Write-Host ("FIRST FAILURE DIAGNOSTIC " + ($diagnosed[0].diagnostic |
                Select-Object vendor, renderer, version, depthPixels, depthFile, inputs, isolatedParts, vertexWitnessEqual |
                ConvertTo-Json -Depth 8 -Compress))
        }
    }
    if ($DepthProbe) {
        # Diagnostic completion is NOT visual acceptance. Require an actual retained
        # Q475 failure with raw depths and all twelve isolated parts for investigation.
        Assert ($proof.diagnosticOnly -eq $true -and $proof.success -eq $true -and $proof.error -ceq '') 'diagnostic reproduction completed (not package acceptance)'
        $checks = @($proof.checks)
        $expected = @('live-npc', 'diagnostic-inputs-valid', 'depth-failure-reproduced')
        Assert ($checks.Count -eq 3 -and @($checks.key | Select-Object -Unique).Count -eq 3) 'distinct diagnostic check count'
        foreach ($key in $expected) {
            $check = @($checks | Where-Object { $_.key -ceq $key })
            Assert ($check.Count -eq 1 -and $check[0].ok -eq $true) $key
        }
        Assert (@($proof.rows).Count -ge 1 -and @($proof.rows).Count -le 146) 'bounded synthetic search'
        $failure = @($proof.rows | Where-Object { $_.pixels.depthOver -gt 0 })
        Assert ($failure.Count -ge 1 -and $failure[0].success -eq $false) 'Q475 depth failure retained'
        $d = $failure[0].diagnostic
        Assert ($null -ne $d -and @($d.isolatedParts).Count -eq 12 -and @($d.depthPixels).Count -gt 0) 'raw failure and isolated parts captured'
        Assert ((Test-Path -LiteralPath $d.depthFile) -and (Get-Item -LiteralPath $d.depthFile).Length -eq (8 + 256 * 256 * 8)) 'complete raw A/B float depths'
        foreach ($part in @($d.isolatedParts)) {
            Assert ($part.repeatEqual -eq $true -and $part.draws -eq 1 -and $part.gl -eq 0 -and
                $part.visibleParts -eq $part.expectedVisibleParts -and $part.visibleParts -le 1) "actual single-part isolation $($part.part)"
            if ($part.expectedVisibleParts -eq 0) { Assert ($part.pixels.union -eq 0) "hidden isolated part $($part.part) is empty" }
        }
        Assert (@($d.isolatedParts | Where-Object { $_.pixels.depthOver -gt 0 -and $_.visibleParts -eq 1 }).Count -gt 0) 'depth failure localized to a truly isolated visible part'
        Assert ($d.vertexWitnessEqual -eq $true) 'vertex capture leaves candidate image, route and state bitwise unchanged'
        $transforms = @($d.vertexTransforms)
        $visibleParts = @($d.isolatedParts | Where-Object { $_.expectedVisibleParts -eq 1 })
        Assert ($transforms.Count -eq $visibleParts.Count -and @($transforms.part | Select-Object -Unique).Count -eq $transforms.Count) 'native-stack feedback for every visible part'
        foreach ($t in $transforms) {
            Assert ($t.gl -eq 0 -and $t.vertices -gt 0 -and $t.basisInputs -eq 4 -and
                @($t.inputs).Count -eq ($t.vertices + 4) * 4 -and @($t.outputs).Count -eq ($t.vertices + 4) * 16) "complete vertex feedback part $($t.part)"
            $finite = $true
            foreach ($value in @($t.outputs)) { $finite = $finite -and -not [float]::IsNaN($value) -and -not [float]::IsInfinity($value) }
            Assert $finite "finite vertex feedback part $($t.part)"
            $nonempty = $true
            for ($v = 0; $v -lt $t.vertices; $v++) {
                $nonempty = $nonempty -and $t.outputs[$v * 16 + 3] -eq 1
            }
            Assert $nonempty "nonempty orthographic ftransform feedback part $($t.part)"
            Write-Host ("VERTEX part={0} fixed/builtin={1} fixed/uniform={2} fixed/split={3}" -f $t.part,
                ($t.builtinVsFixed | ConvertTo-Json -Compress), ($t.uniformVsFixed | ConvertTo-Json -Compress), ($t.splitVsFixed | ConvertTo-Json -Compress))
        }
    } elseif ($DepthRegression) {
        Assert ($proof.diagnosticOnly -eq $false -and $proof.depthRegression -eq $true -and
            $proof.success -eq $true -and $proof.error -ceq '') 'deterministic depth regression completed'
        $checks = @($proof.checks)
        $expected = @('live-npc', 'old-transform-rejected', 'regression-inputs-valid', 'depth-regression')
        Assert ($checks.Count -eq 4 -and @($checks.key | Select-Object -Unique).Count -eq 4) 'distinct regression check count'
        foreach ($key in $expected) {
            $check = @($checks | Where-Object { $_.key -ceq $key })
            Assert ($check.Count -eq 1 -and $check[0].ok -eq $true) $key
        }
        $old = $proof.legacyControl
        Assert ($old.success -eq $false -and $old.repeatEqual -eq $true -and $old.eventsEqual -eq $true -and
            $old.stateEqual -eq $true -and $old.glEqual -eq $true -and $old.draws -eq 1 -and $old.fallbacks -eq 0 -and
            $old.pixels.depthOver -gt 0 -and $old.inputs.floatBits.prevLimbSwingAmount -eq 1057629362) 'old shader fails Q475 on known pose'
        $rows = @($proof.rows)
        Assert ($rows.Count -eq 146 -and @($rows.index | Select-Object -Unique).Count -eq 146) 'all 146 deterministic poses retained'
        foreach ($row in $rows) {
            $p = $row.pixels
            Assert ($row.success -eq $true -and $row.repeatEqual -eq $true -and $row.eventsEqual -eq $true -and
                $row.stateEqual -eq $true -and $row.glEqual -eq $true -and $row.imageEqual -eq $true -and
                $row.draws -eq 1 -and $row.offDraws -eq 0 -and $row.repeatDraws -eq 0 -and $row.fallbacks -eq 0 -and
                $p.union -gt 0 -and $p.maskDiff -eq 0 -and $p.alphaDiff -eq 0 -and $p.outsideDiff -eq 0 -and
                $p.depthOver -eq 0 -and $p.maxDepth -le 0.000001 -and $p.maxChannel -le 8 -and $p.over8 -eq 0 -and
                $p.within2 * 1000 -ge $p.union * 999) "unchanged Q475 and candidate route pose $($row.index)"
        }
        $known = @($rows | Where-Object index -eq 8)[0]
        Assert (($known.inputs | ConvertTo-Json -Compress -Depth 4) -ceq
            ($old.inputs | ConvertTo-Json -Compress -Depth 4)) 'old and corrected shader inputs exactly equal'
        foreach ($variant in @('old-control', 'fixed-control')) {
            foreach ($suffix in @('A', 'B', 'diff')) {
                $file = "$prefix-$variant-$suffix.png"
                Assert ((Test-Path -LiteralPath $file) -and (Get-Item -LiteralPath $file).Length -gt 100) "$variant $suffix PNG"
            }
        }
        Assert ($proof.frames -eq 0 -and @($proof.probes).Count -eq 0) 'synthetic regression is separate from live AI proof'
    } else {
    Assert ($proof.diagnosticOnly -eq $false) 'live proof mode'
    $expected = @('live-npc', 'pose-control', 'uv-control', 'server-navigation', 'server-combat',
        'samples-walk', 'samples-swing', 'samples-hurt', 'samples-recovery', 'all-images', 'walk-partial-sweep', 'negative-controls',
        'live-world-route', 'live-world-gl', 'screenshots')
    $checks = @($proof.checks)
    foreach ($row in $checks) { Write-Host ("RESULT {0} ok={1} {2}" -f $row.key, $row.ok, $row.detail) }
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143M DONE success=true')) 'motion proof success'
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        Assert ($row.Count -eq 1 -and $row[0].ok -eq $true) $key
    }
    $server = $proof.server
    Assert ($server.ticks -eq 641 -and $server.walkSpan -ge 4 -and $server.pathTicks -ge 20 -and $server.opponentAiEnabledAt -eq 401) 'real server navigation and combat stages'
    Assert ($server.swings -ge 3 -and $server.hurts -ge 3 -and $server.actorAfter -gt 0 -and $server.enemyAfter -gt 0 -and
        $server.actorBefore -gt $server.actorAfter -and $server.enemyBefore -gt $server.enemyAfter) 'both combatants really deal damage'
    Assert ($proof.frames -ge 100 -and $proof.badFrames -eq 0 -and $proof.glErrors -eq 0) 'live world original/candidate route and GL'
    $rows = @($proof.rows)
    $probes = @($proof.probes)
    Assert ($rows.Count -ge 38 -and @($rows.tick | Select-Object -Unique).Count -eq $rows.Count) 'distinct real client ticks sampled'
    $walkRows = @($rows | Where-Object { $_.category -ceq 'walk' })
    Assert ($probes.Count -eq $walkRows.Count * 11) 'eleven additional interpolation probes for every live walking sample'
    foreach ($walkRow in $walkRows) {
        $group = @($probes | Where-Object { $_.tick -eq $walkRow.tick })
        Assert ($group.Count -eq 11 -and @($group.partial | Select-Object -Unique).Count -eq 11) "distinct interpolation probes tick $($walkRow.tick)"
        foreach ($partial in @(0, 0.099999666, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1)) {
            Assert (@($group | Where-Object { [Math]::Abs($_.partial - $partial) -lt 0.00000001 }).Count -eq 1) "probe partial $partial tick $($walkRow.tick)"
        }
    }
    foreach ($row in (@($rows) + @($probes))) {
        $key = "tick $($row.tick) $($row.category)"
        Assert ($row.success -eq $true -and $row.repeatEqual -eq $true -and $row.eventsEqual -eq $true -and
            $row.imageEqual -eq $true -and $row.stateEqual -eq $true -and $row.glEqual -eq $true) "A/B/A2 $key"
        Assert ($row.offDraws -eq 0 -and $row.repeatDraws -eq 0 -and $row.partial -ge 0 -and $row.partial -le 1) "original passes/partial $key"
        Assert ($null -ne $row.inputs.floatBits -and $row.inputs.ticks -eq $row.tick -and $row.inputs.partial -eq $row.partial) "recorded pose inputs $key"
        $exact = $row.hurt -gt 0
        Assert ($row.exact -eq $exact -and $row.draws -eq [int](-not $exact) -and ($exact -or $row.fallbacks -eq 0)) "actual live route $key"
        $p = $row.pixels
        Assert ($p.union -gt 0 -and $p.maskDiff -eq 0 -and $p.alphaDiff -eq 0 -and $p.outsideDiff -eq 0 -and
            $p.depthOver -eq 0 -and $p.maxDepth -le 0.000001 -and $p.maxChannel -le 8 -and
            $p.over8 -eq 0 -and $p.within2 * 1000 -ge $p.union * 999) "Q475 $key"
        if ($exact) { Assert ($p.maxChannel -eq 0 -and $p.maxDepth -eq 0) "bitwise hurt fallback $key" }
    }
    foreach ($pair in @(@('walk', 20), @('swing', 4), @('hurt', 4), @('recovery', 10))) {
        $category = $pair[0]
        $group = @($rows | Where-Object { $_.category -ceq $category })
        Assert ($group.Count -ge $pair[1]) "actual $category sample count"
        foreach ($suffix in @('A', 'B', 'diff')) {
            $file = "$prefix-$category-$suffix.png"
            Assert ((Test-Path -LiteralPath $file) -and (Get-Item -LiteralPath $file).Length -gt 100) "FBO $category $suffix PNG"
        }
    }
    $negative = @($rows | Where-Object { $_.category -ceq 'walk' } | Select-Object -First 1)
    Assert ($negative.Count -eq 1 -and $negative[0].poseControl -eq $true -and $negative[0].uvControl -eq $true) 'live negative controls'
    Assert (@($proof.screenshots).Count -eq 4 -and @($proof.screenshots | Select-Object -Unique).Count -eq 4) 'four distinct world PNG'
    foreach ($name in $proof.screenshots) {
        $file = Join-Path (Split-Path $prefix -Parent) $name
        Assert ((Test-Path -LiteralPath $file) -and (Get-Item -LiteralPath $file).Length -gt 100) "world PNG $name"
    }
    }
    $client = $null
    Unchanged
    $condition = 'one isolated integrated dev client/GPU; lazy render listener prepared at switch 0 before observed interval; 240 server ticks of production path AI, 320 combat ticks against a live vindicator (160 NoAI recipient then 160 opponent AI), 80 recovery ticks; no NPC positions/limb/swing/hurt fields assigned after spawn; 256x256 FBO whole living-renderer A/B/A2 on actual networked poses/event partialTicks plus eleven mandatory partial probes per walking sample, Q475 ordinary and bitwise hurt fallback, per-frame world Pre/Post/batch route, four categories of FBO/world PNG; clock blink held open only inside comparison; no cost, optional-mod, URL, MPM, death, tracking edge or multiplayer proof'
    if ($DepthProbe) { $condition = 'DIAGNOSTIC ONLY: one isolated integrated dev client/GPU, no patrol/combat simulation; at most 146 synthetic v4-neighbourhood poses, changed client fields restored, 256x256 FBO original/candidate/original, unchanged Q475; success requires retained failing depth image, raw depths, twelve single-part comparisons (showModel, no layers/name), and native-stack transform feedback of ftransform/builtin MVP/uniform clip/split products with four basis inputs. Feedback must leave the full candidate image/state/route bitwise unchanged. Probe shader is diagnostic, not the production shader. NOT live AI or package acceptance.' }
    if ($DepthRegression) { $condition = 'DETERMINISTIC REGRESSION: same v4-neighbourhood inputs as depth-v2, 146 corrected-production whole-renderer A/B/A2 images must pass unchanged Q475; old matrix-product shader must fail at prevAmount=0.5396224 on exactly identical inputs, with a real candidate draw and valid state/events/repeat; six control PNG. No live AI or cost proof in this mode.' }
    @{success = $true; diagnosticOnly = $DepthProbe.IsPresent; depthRegression = $DepthRegression.IsPresent; package = 'M14.3b5'; proof = $out; log = $log; inputs = $hashes;
        condition = $condition} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M14.3b5 evidence: $prefix.json"
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
