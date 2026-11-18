<# Controller-only M14.3b3 live L1/L2 transition proof. One isolated integrated dev client,
   two real NPCs (ordinary batch candidate and an armored, refused one), camera walked frame by
   frame across RwImpostorStart=24 and back (hysteresis 4) after a baking warmup, at
   RwNpcInstancing 0/1/0. Each NPC must be drawn exactly once per frame (model or impostor).
   v2 (Q508): every frame the screen depth inside each NPC's projected box must also show it
   (empty-box negative control), with the world framebuffer bound and render pass 0.
   No cost window, no optional rendering mods, no OptiFine. #>
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = 'm143b-l2'
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
    [IO.File]::WriteAllLines($job, @('npcBatchL2=1', ('world=' + $tag), ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" -PrwSet=RwNpcInstancing=0 > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1500)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
    Assert $client.HasExited 'client completed before timeout'
    Assert ($client.ExitCode -eq 0) 'clean client process exit'
    $text = [IO.File]::ReadAllText($log)
    Assert (-not ($text -match "NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|RWHARNESS-NAPAKA|Couldn't render entity")) 'no linkage/harness/render errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    $expected = @('live-npcs', 'warm-hook-gl', 'warm-baked')
    foreach ($p in @('A', 'B', 'A2')) {
        $expected += "$p-hook-gl"
        foreach ($n in @('plain', 'armor')) { $expected += @("$p-$n-exactly-once", "$p-$n-threshold", "$p-$n-route", "$p-$n-visible") }
        $expected += "$p-visible-control"
    }
    $expected += @('route-sequence-A-B-A2', 'screenshots')
    $checks = @($proof.checks)
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        if ($row.Count -eq 1) { Write-Host ("RESULT {0} ok={1} {2}" -f $key, $row[0].ok, $row[0].detail) }
    }
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143L DONE success=true')) 'L2 transition proof success'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        Assert ($row.Count -eq 1 -and $row[0].ok -eq $true) $key
    }
    Assert ($proof.start -eq 24 -and $proof.hysteresis -eq 4) 'threshold and hysteresis'
    $passes = @($proof.passes)
    Assert ($passes.Count -eq 4 -and (($passes.pass) -join ',') -ceq 'warm,A,B,A2') 'four passes in order'
    Assert ((($passes.instancing) -join ',') -ceq '1,0,1,0') 'switch per pass'
    foreach ($p in $passes | Select-Object -Skip 1) {
        Assert ($p.glErrors -eq 0 -and $p.hookFrames -eq $p.frames -and $p.waited -eq 0) "hook and GL $($p.pass)"
        foreach ($n in @('plain', 'armor')) {
            $s = $p.$n
            Assert ($s.bad -eq 0 -and $s.offeredBad -eq 0 -and $s.expectedBad -eq 0 -and $s.unbaked -eq 0 -and
                $s.model + $s.impostor -eq $p.frames -and $s.model -gt 0 -and $s.impostor -gt 0) "exactly once $($p.pass) $n"
            Assert ($s.invisible -eq 0 -and $s.fboBad -eq 0 -and $s.passBad -eq 0 -and $s.minCover -ge 0.2) "visible $($p.pass) $n"
            Assert ($s.enters -eq 1 -and $s.leaves -eq 1 -and $s.bandModel -gt 0 -and $s.bandImpostor -gt 0 -and
                $s.enterDist -gt 24 -and $s.enterPrev -le 24 -and $s.leaveDist -le 20 -and $s.leavePrev -gt 20) "threshold $($p.pass) $n"
            if ($p.instancing -eq 0) {
                Assert ($s.batch -eq 0 -and $s.fallbacks -eq 0) "original route $($p.pass) $n"
            } elseif ($n -ceq 'plain') {
                Assert ($s.batch -eq $s.model -and $s.fallbacks -eq 0 -and $s.batchBad -eq 0) "batch route $($p.pass) $n"
            } else {
                Assert ($s.batch -eq 0 -and $s.fallbacks -eq $s.model -and $s.modelRefusals -eq $s.model -and $s.batchBad -eq 0) "refused route $($p.pass) $n"
            }
        }
    }
    $shots = @($proof.screenshots)
    Assert ($shots.Count -eq 6) 'six transition screenshots'
    foreach ($s in $shots) { Assert (Test-Path -LiteralPath (Join-Path $root "audit/$s")) "screenshot $s" }
    $client = $null
    Unchanged
    $condition = 'one isolated integrated dev client/GPU, FLAT world, two standing NPCs (ordinary and armored), RwFarView=2, RwImpostorStart=24, camera walked 8->36->8 blocks in 0.1 block steps per frame after a live bake at 1, passes RwNpcInstancing 0/1/0; exactly-once model/impostor per frame with screen-depth visibility and empty-box control, threshold/hysteresis, batch/refused route and identical route sequence; no L2/L3 handoff, beyond-render-range L2, moving/fighting NPC, cost, OptiFine/shader or optional-mod proof'
    @{success = $true; package = 'M14.3b3'; proof = $out; log = $log; screenshots = $shots; inputs = $hashes; condition = $condition} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M14.3b3 evidence: $prefix.json"
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
