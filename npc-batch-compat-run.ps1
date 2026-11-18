<# Controller-only M14.3a native GL/lifecycle proof; -WholeRenderer selects M14.3b1
   frozen whole living-renderer fixtures instead; -Contexts selects M14.3b2
   M16/CNPC GUI/baker render-stage fixtures; -ProgramFault selects M14.3b4 injected
   compile/link/draw faults of the batch program with live original fallback and reload
   recovery. -UrlSkin selects M14.3b6 real loopback HTTP with latched PNG body and HTTP 404.
   -Mpm selects M14.3b7: CNPC MPM body parts, unknown CNPC renderers/models and the owner's
   real MorePlayerModels jar (pinned SHA256) loaded with its network paths disabled.
   One isolated dev client, no cost window. No mode installs or simulates OptiFine. #>
param([switch]$WholeRenderer, [switch]$Contexts, [switch]$ProgramFault, [switch]$UrlSkin, [switch]$Mpm)
$ErrorActionPreference = 'Stop'
if ([int]$WholeRenderer.IsPresent + [int]$Contexts.IsPresent + [int]$ProgramFault.IsPresent + [int]$UrlSkin.IsPresent + [int]$Mpm.IsPresent -gt 1) { throw 'Choose one scenario mode' }
$scene = $WholeRenderer -or $Contexts -or $Mpm
$mpmJar = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../ladja_mod/_packlab-multiplayer-20261031/mods/MorePlayerModels_1.12.2-(05Dec19).jar'))
$mpmHash = '5B17BE98531CCD526F4C341E5BD069BF8463D37AF11D324A707BD4745BE8BFCE'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = if ($Mpm) { 'm143b-mpm' } elseif ($UrlSkin) { 'm143b-url' } elseif ($ProgramFault) { 'm143b-fault' } elseif ($Contexts) { 'm143b-contexts' } elseif ($WholeRenderer) { 'm143b-scene' } else { 'm143a-native' }
$package = if ($Mpm) { 'M14.3b7' } elseif ($UrlSkin) { 'M14.3b6' } elseif ($ProgramFault) { 'M14.3b4' } elseif ($Contexts) { 'M14.3b2' } elseif ($WholeRenderer) { 'M14.3b1' } else { 'M14.3a' }
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
    if ($Mpm) {
        # Read-only copy of the owner's jar; the source stays untouched and is re-hashed at the end.
        Assert ((Fingerprint $mpmJar) -ceq $mpmHash) 'pinned MorePlayerModels 1.12.2-(05Dec19) jar'
        $hashes[$mpmJar] = $mpmHash
        New-Item -ItemType Directory -Path (Join-Path $work 'mods') | Out-Null
        Copy-Item -LiteralPath $mpmJar -Destination (Join-Path $work 'mods')
        $files = @(Get-ChildItem -LiteralPath (Join-Path $work 'mods') -Recurse -File)
        Assert ($files.Count -eq 1 -and (Fingerprint $files[0].FullName) -ceq $mpmHash) 'only the pinned MPM jar in isolated mods'
        # Network isolation, no rendering setting: MC/MPM snooper (MPM login analytics) and MPM update check off.
        $utf8 = New-Object Text.UTF8Encoding $false
        $options = Join-Path $work 'options.txt'
        $lines = @(); if (Test-Path -LiteralPath $options) { $lines = @([IO.File]::ReadAllLines($options) | Where-Object { $_ -notlike 'snooperEnabled:*' }) }
        [IO.File]::WriteAllLines($options, @($lines) + @('snooperEnabled:false'), $utf8)
        Assert (-not (Test-Path -LiteralPath (Join-Path $work 'config/MorePlayerModels.cfg'))) 'no inherited MPM config'
        [IO.File]::WriteAllLines((Join-Path $work 'config/MorePlayerModels.cfg'), @('EnableUpdateChecker=false'), $utf8)
    }
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    if ($UrlSkin) {
        # Retain source/condition evidence even when the pre-fix reproduction fails.
        @{package = $package; inputs = $hashes; proof = $out; log = $log} |
            ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.inputs.json" -Encoding UTF8
    }
    [IO.File]::WriteAllLines($job, @('npcBatchCompat=1', ('world=' + $tag), ('wholeRenderer=' + [int]$WholeRenderer.IsPresent),
        ('contexts=' + [int]$Contexts.IsPresent), ('programFault=' + [int]$ProgramFault.IsPresent),
        ('urlSkin=' + [int]$UrlSkin.IsPresent), ('mpm=' + [int]$Mpm.IsPresent),
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
    if ($Contexts) {
        $keys = @('ordinary-name', 'rig-idle', 'rig-walk', 'rig-hurt', 'rig-death', 'gui-ordinary', 'gui-rig',
            'bake-0', 'bake-45', 'bake-90', 'bake-135', 'bake-180', 'bake-225', 'bake-270', 'bake-315', 'ordinary-after-specials')
        $special = @($keys | Where-Object { $_ -cnotin @('ordinary-name', 'ordinary-after-specials') })
        $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('name-visible', 'pose-negative-control', 'uv-negative-control', 'recovery-baseline')
    }
    if ($ProgramFault) {
        $keys = @('compile-fault', 'compile-fault-later-frame', 'link-fault', 'draw-fault', 'draw-fault-later-frame', 'ordinary-after-faults')
        $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('compile-fault-state', 'compile-fault-no-retry',
            'live-world-original', 'live-world-original-gl', 'link-fault-state', 'draw-fault-state', 'draw-fault-no-retry', 'program-recovered')
        # Each injected fault reaches the real driver/runtime exactly once; no retry storm.
        foreach ($pair in @(@('RWBATCH shader compile failed', 1), @('RWBATCH program link failed', 1),
                @('RWBATCH draw failed: java.lang.IllegalStateException: test draw fault', 1), @('RWBATCH draw failed', 1))) {
            $count = ([regex]::Matches($text, [regex]::Escape($pair[0]))).Count
            Write-Host ("LOG '{0}' x{1}" -f $pair[0], $count)
            Assert ($count -eq $pair[1]) "log $($pair[0]) exactly $($pair[1])"
        }
    }
    if ($Mpm) {
        $keys = @('ordinary-name', 'eyes-off', 'parts-legs', 'parts-wings', 'parts-tail', 'parts-ears', 'parts-breasts',
            'foreign-renderer', 'foreign-model', 'classic-model', 'model-64x32', 'mpm-player-before', 'mpm-player-after',
            'ordinary-after-specials')
        $special = @($keys | Where-Object { $_ -like 'parts-*' }) + @('foreign-renderer', 'foreign-model', 'classic-model', 'model-64x32')
        $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('name-visible', 'pose-negative-control', 'uv-negative-control',
            'recovery-baseline', 'mpm-active', 'mpm-network-isolated')
        Assert ($text -match 'moreplayermodels') 'MPM present in client log'
        Assert (-not ($text -match 'google-analytics|nopapi\.nl|kodevelopment\.nl')) 'no MPM network endpoint in client log'
    }
    if ($UrlSkin) {
        $keys = @('url-pending', 'url-pending-later', 'url-loaded', 'ordinary-after-url', 'url-failed', 'ordinary-after-failure')
        $expected = @($keys) + @('pending-before', 'pending-after', 'pending-later-before', 'pending-later-after', 'body-held', 'loaded', 'failed', 'http-counts')
    }
    $checks = @($proof.checks)
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        if ($row.Count -eq 1) { Write-Host ("RESULT {0} ok={1} {2}" -f $key, $row[0].ok, $row[0].detail) }
    }
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143A DONE success=true')) 'compatibility proof success'
    $joins = if ($scene -or $ProgramFault -or $UrlSkin) { 1 } else { 2 }
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
        $native = if ($scene) { $row.key -cnotin $special } else { $row.key -like 'ordinary*' }
        if ($scene) {
            Assert ($row.eventsEqual -eq $true -and $row.visible -eq $true -and $row.featureVisible -eq $true) "whole renderer and visible feature $($row.key)"
            Assert ($row.offDraws -eq 0 -and $row.repeatDraws -eq 0) "original route at 0 $($row.key)"
            Assert ($row.draws -eq [int]$native -and ((-not $native) -or $row.fallbacks -eq 0)) "actual route $($row.key)"
            if ($Mpm -and -not $native) { Assert ($row.reasons -ceq 'model=1,') "model refusal reason $($row.key)" }
            if ($native) {
                $pixels = $row.pixels
                Assert ($pixels.union -gt 0 -and $pixels.maskDiff -eq 0 -and $pixels.alphaDiff -eq 0 -and $pixels.outsideDiff -eq 0 -and
                    $pixels.depthOver -eq 0 -and $pixels.maxDepth -le 0.000001 -and $pixels.over8 -eq 0 -and
                    $pixels.maxChannel -le 8 -and $pixels.within2 * 1000 -ge $pixels.union * 999) "Q475 numeric limits $($row.key)"
            }
            if ($Contexts) {
                $rig = $row.key -like 'rig-*' -or $row.key -ceq 'gui-rig'
                $fake = $row.key -like 'bake-*'
                Assert ($row.rigFrames -eq [int]$rig -and $row.offRigFrames -eq [int]$rig -and $row.repeatRigFrames -eq [int]$rig) "actual M16 route $($row.key)"
                Assert ($row.fakeEntity -eq $fake -and $row.worldRegistered -eq (-not $fake)) "entity membership $($row.key)"
                if ($fake -or $row.key -ceq 'gui-ordinary') {
                    Assert ($row.fallbacks -eq 1 -and $row.passFallbacks -eq 1) "pass refusal with complete original image $($row.key)"
                }
            }
        } else {
            Assert ($row.draws -eq [int]$native -and $row.fallbacks -eq [int](-not $native)) "actual route $($row.key)"
        }
        Assert ($row.exact -eq (-not $native)) "bitwise fallback $($row.key)"
        if ($UrlSkin) {
            Assert ($row.success -eq $true -and $row.referenceEqual -eq $true -and $row.visible -eq $true -and
                $row.eventsEqual -eq $true -and $row.glEqual -eq $true -and $row.skinControl -eq $true -and
                $row.events -ceq 'pre,layer,post,') "full original skin reference/events/GL $($row.key)"
            Assert ($row.offDraws -eq 0 -and $row.repeatDraws -eq 0) "original route at 0 $($row.key)"
            if (-not $native) { Assert ($row.reasons -ceq 'model=1,') "URL refusal reason $($row.key)" }
            else {
                $pixels = $row.pixels
                Assert ($pixels.union -gt 0 -and $pixels.maskDiff -eq 0 -and $pixels.alphaDiff -eq 0 -and $pixels.outsideDiff -eq 0 -and
                    $pixels.depthOver -eq 0 -and $pixels.maxDepth -le 0.000001 -and $pixels.over8 -eq 0 -and
                    $pixels.maxChannel -le 8 -and $pixels.within2 * 1000 -ge $pixels.union * 999) "Q475 recovery $($row.key)"
            }
        }
    }
    $client = $null
    Unchanged
    $condition = if ($Mpm) { 'one isolated integrated dev client/GPU with the owner''s real MorePlayerModels 1.12.2-(05Dec19) jar (pinned SHA256, snooper/update check/web API off); 14 complete living-renderer FBO A/B/A2 rows: default eyes on/off, five CNPC MPM body parts, a subclassed RenderCustomNpc, a subclassed ModelPlayerAlt, classic and 64x32 CNPC models, a real MPM player with wings drawn before/after the batched NPC; bitwise original for parts/unknown classes, Q475 for supported rows, event/layer/GL audit, ordinary recovery and negative controls; no OptiFine/shader, other optional mods, MPM GUI/animations, cost or packaged-client proof' }
        elseif ($UrlSkin) { 'one isolated integrated dev client/GPU; real loopback HTTP and original ImageDownloadAlt, body latched across two render callbacks, then successful PNG and separate HTTP 404; six complete living-renderer FBO A/B/A2 rows, bitwise URL fallback, known local image references, event/layer/GL audit and ordinary batch recovery; no downloader-field injection, M12 optimization, optional-mod, cost or packaged-client proof' }
        elseif ($ProgramFault) { 'one isolated integrated dev client/GPU; injected batch-program compile fault (first build), link fault and draw-time exception with program/VAO bound, each followed by a later frame; frozen FBO A/B/A2 bitwise original image and GL state, no rebuild until a real resource reload, live world NPC at switch 1 drawn by the original renderer while failed, recovery to the batch draw after reload; injected faults, not a real driver/OptiFine/shader failure; no cost proof' }
        elseif ($Contexts) { 'one isolated integrated dev client/GPU; 16 frozen FBO A/B/A2 rows, real M16 knight idle/walk/hurt/death route, actual CNPC GUI drawNpc with ordinary/rig entity, actual ImpostorBaker createEntity/drawModel at 8 angles; bitwise excluded routes and GL/event comparison, ordinary recovery and negative controls; not full atlas pipeline, GUI interaction, rig networking, live AI, L2 transition, cost or optional-mod proof' }
        elseif ($WholeRenderer) { 'one isolated integrated dev client/GPU; 25 frozen whole living-renderer fixtures in FBO A/B/A2, visible features, events/layer witness/name, Q475 and bitwise special fallback; no live AI motion, rig, URL, GUI/bake, L2, shadow, cost or optional-mod proof' }
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
