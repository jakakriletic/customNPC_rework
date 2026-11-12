# M16 controller scenario: the built-in animation core on a real integrated dev client in an isolated
# run directory (RwAnimCore=1 in its config copy, no customnpcentities). NPCs summoned with only NBT RwRig
# are drawn as rigged models, clips reach the client, a data-only hit event hurts a zombie, rig and own
# clips survive the clone NBT round trip. Two PNGs. No dedicated server.
param([string]$Models='steve,legionary,humanoid,knight,skeleton_warrior,ogre,troll,warg,giant_spider,cave_bear',
      [string]$Legion=(Join-Path $PSScriptRoot 'rig_src/examples/legionary.rig.json'),
      [string]$LegionSkin=(Join-Path $PSScriptRoot 'rig_src/steve/preview_skin.png'),
      [string]$Package=(Join-Path $PSScriptRoot 'rig_src/examples/knight_boss.rig.json'))
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m16-rig-$stamp"
$work = Join-Path $root "dev/run-m16/$stamp"
$world = 'm16-world'
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
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm16-rig'
    Guard
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $cfgText = [IO.File]::ReadAllText($cfg)
    $cfgText = [regex]::Replace($cfgText, '(?m)^RwAnimCore=\d+\r?\n?', '')
    $cfgText += "`nRwAnimCore=1`n"
    [IO.File]::WriteAllText($cfg, $cfgText, (New-Object Text.UTF8Encoding $false))
    Assert (@([regex]::Matches($cfgText, '(?m)^RwAnimCore=')).Count -eq 1) 'config copy has exactly one RwAnimCore=1'
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'mods'))) 'no optional entities mod'
    # The legionary package names this skin; the run's customnpcs/ folder is a resource pack of CustomNPCs.
    $skinDir = Join-Path $work 'customnpcs/assets/customnpcs/textures/entity/humanmale'
    New-Item -ItemType Directory -Path $skinDir -Force | Out-Null
    Copy-Item -LiteralPath $LegionSkin -Destination (Join-Path $skinDir 'legionary.png')
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    [IO.File]::WriteAllLines($job, @('rigCore=1', "world=$world", "models=$Models", ('package=' + $Package.Replace('\', '/')), ('legion=' + $Legion.Replace('\', '/')), ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
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
    Assert ($text.Contains('M16 DONE success=true')) 'client success marker'
    Assert (-not ($text -match "$linkage|RWRIG cannot|RWRIG model|RWRIG client not installed|M16 DONE success=false")) 'no linkage or rig load errors'
    Assert (-not ($text -cmatch 'Exception ticking|Encountered an unexpected exception|MissingModsException')) 'no ticking/loader errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true) 'proof success'
    $keys = @('server-rigs','client-rigs','client-meshes','rig-frames','client-clip-synced','hit-event-damage','own-clips-valid',
        'clone-keeps-rig','clone-keeps-own-clips','own-clip-plays','own-clip-synced','package-applied','package-own-clip','js-attack-clips','js-attack-damage',
        'legion-shield-advance','legion-shield-on-client','legion-attack','legion-damage')
    $rows = @($proof.checks)
    Assert ($rows.Count -eq $keys.Count) "exactly $($keys.Count) checks"
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) 'all checks passed'
    Assert (@($proof.screenshots).Count -eq 5) 'five screenshots'
    foreach ($image in $proof.screenshots) {
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    $client = $null
    Unchanged
    @{success = $true; package = 'M16'; proof = $out; log = $log; inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; data = $proof.data; models = $Models;
      condition = 'one integrated dev client (MCP runClient, isolated run dir), RwAnimCore=1 in the config copy, no customnpcentities; NPCs summoned with NBT RwRig only'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M16 evidence: $prefix.json"
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
