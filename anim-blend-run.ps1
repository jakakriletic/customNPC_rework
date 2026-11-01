# P7b controller scenario: IAnimator.blendTo on a real integrated dev client with the pinned
# customnpcentities 0.7.0 in an isolated run directory (RwAnimHooks=1 in its config copy).
# Two cave bears: A switches with play, B with blendTo; numeric poses of the patched
# AnimatedModel.buildPose are the primary proof, one PNG supplements. No dedicated server.
param([string]$EntitiesJar=(Join-Path (Split-Path $PSScriptRoot -Parent) 'customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar'))
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/p7b-blend-$stamp"
$work = Join-Path $root "dev/run-p7b/$stamp"
$world = 'p7b-world'
$lock = $null
$client = $null
$exitCode = 1
$hashes = @{}
$owned = @{}
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$linkage = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError'

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
    while ($grew) { $grew = $false; foreach ($p in $snapshot) { if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId] = $true; $grew = $true } } }
    foreach ($p in $snapshot) { if ($p.Name -in @('java.exe', 'javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true } }
    $foreign = @(Get-NavForeignJava -Roots (@($roots) + @($owned.Keys)))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged {
    foreach ($path in $hashes.Keys) { Assert ((Fingerprint $path) -ceq $hashes[$path]) "unchanged $path" }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'p7b-blend'
    Guard
    $protected = @($EntitiesJar, (Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert ($hashes[$EntitiesJar] -ceq '0DCDEC55A33BB03C2740D702C59AF8FC0039901CB9FB58D6215DAFA6BBD62149') 'pinned entities 0.7.0 jar'
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $cfgText = [IO.File]::ReadAllText($cfg)
    Assert (-not ($cfgText -match '(?m)^RwAnimHooks=(?!0\r?$)')) 'config copy has no animation hook switch'
    $cfgText = [regex]::Replace($cfgText, '(?m)^RwAnimHooks=0\r?\n?', '')
    $cfgText += "`nRwAnimHooks=1`n"
    [IO.File]::WriteAllText($cfg, $cfgText, (New-Object Text.UTF8Encoding $false))
    Assert (@([regex]::Matches($cfgText, '(?m)^RwAnimHooks=')).Count -eq 1) 'config copy has exactly one RwAnimHooks=1'
    $mods = Join-Path $work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
    Copy-Item -LiteralPath $EntitiesJar -Destination $mods
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    [IO.File]::WriteAllLines($job, @('animBlend=1', "world=$world", ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
    Assert $client.HasExited 'client completed before timeout'
    Assert ($client.ExitCode -eq 0) 'client clean process exit'
    $text = [IO.File]::ReadAllText($log)
    Assert ($text.Contains('P7B DONE success=true')) 'client success marker'
    Assert ($text.Contains('poseg v dev.jakak.customnpcentities.anim.client.AnimatedModel uspel')) 'client model class instrumented at load'
    Assert ($text.Contains('poseg v dev.jakak.customnpcentities.entity.EntityAnimatedNpc uspel')) 'entity class instrumented at load'
    Assert (-not ($text -match "$linkage|RWANIM pose blending stopped|RWANIM incompatible|NI uspel|P7B DONE success=false")) 'no linkage, blend or patch errors'
    Assert (-not ($text -cmatch 'Exception ticking|Encountered an unexpected exception|MissingModsException|DuplicateModsFound')) 'no ticking/loader errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true) 'proof success'
    $keys = @('blend-class-patched','journal-class-patched','bears-rendered','off-thread-false','bad-args-false','zero-ticks-plays','absent-handle-false',
        'server-c1-roar','server-c1-switch','server-c2-roar','server-c2-swipe','server-c2-maul','server-c3-roar','server-c3-charge',
        'c1-frames','c1-previous-was-roar','c1-from-is-last-drawn','c1-play-cuts','c1-blend-continuous','c1-linear','c1-mid','c1-end',
        'c2-frames','c2-swipe-blended-until-replaced','c2-maul-from-blended-pose','c2-linear','c3-blend-before-death','c3-death-unblended','messages')
    $rows = @($proof.checks)
    Assert ($rows.Count -eq $keys.Count) "exactly $($keys.Count) checks"
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) 'all checks passed'
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($keys | Sort-Object) -join '|')) 'exact check key set'
    Assert (@($proof.screenshots).Count -eq 1) 'one screenshot'
    foreach ($image in $proof.screenshots) {
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    $client = $null
    Unchanged
    @{success = $true; package = 'P7b'; proof = $out; log = $log; inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; data = $proof.data;
      condition = 'one integrated dev client (MCP runClient, isolated run dir) with customnpcentities 0.7.0 pinned jar in mods (classes instrumented in memory at load), RwAnimHooks=1 in the config copy; two cave bears, play vs blendTo; numeric poses from the patched AnimatedModel.buildPose; no dedicated server, no packaged client'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "P7B evidence: $prefix.json"
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
