# M11.9 controller-only scenario: water, gap, throat, trapped member, casualties/departure, restart and off.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m119-army-$stamp"
$work = Join-Path $root "dev/run-m119/$stamp"
$world = 'm119-world'
$server = $null; $lock = $null; $exitCode = 1; $serverLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|script errored|ScriptException'

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    [void]$checks.Add($Text)
    Write-Host "OK $Text"
}
function Fingerprint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Guard {
    $roots = @(); if ($null -ne $server) { $roots += $server.Id }
    $snapshot = @(Get-CimInstance Win32_Process)
    $tree = @{}; foreach ($id in @($roots) + @($owned.Keys)) { $tree[[int]$id] = $true }
    $grew = $true
    while ($grew) { $grew = $false; foreach ($p in $snapshot) { if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId] = $true; $grew = $true } } }
    foreach ($p in $snapshot) { if ($p.Name -in @('java.exe', 'javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true } }
    $foreign = @(Get-NavForeignJava -Roots (@($roots) + @($owned.Keys)))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged { foreach ($path in $hashes.Keys) { if ((Fingerprint $path) -cne $hashes[$path]) { throw "changed $path" } } }
function Log { return (Read-JavaPrototypeLog $serverLog) }
function Send([string]$Text) { $server.StandardInput.WriteLine($Text); $server.StandardInput.Flush(); Start-Sleep -Milliseconds 400 }
function Wait-Marker([string]$Marker, [int]$Seconds = 60) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Log
        if ($text.Contains($Marker)) { return $true }
        if ($server.HasExited -or $text -cmatch $bad -or $text.Contains('ARMYEDGE DONE phase=terrain')) { throw "Server failed before '$Marker'; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    return $false
}
function Expect([string]$Marker, [string]$Text, [int]$Seconds = 60) { Assert (Wait-Marker $Marker $Seconds) $Text }
function Set-Switch([int]$Value) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwArmies=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwArmies=$Value`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Switch) {
    Set-Switch $Switch
    $script:serverLog = "$prefix-$Name.log"
    $script:server = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runServer', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwWorld=$world") (Join-Path $root 'dev') $serverLog
    Expect 'Done (' "$Name server started" 600
    Assert ((Log).Contains('Preparing level "' + $world + '"')) "$Name isolated world"
}
function Stop-Phase([string]$Name) {
    Send 'stop'
    $until = (Get-Date).AddSeconds(180)
    while (-not $server.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $server.HasExited "$Name server stopped"
    [M61ProcessLog]::Finish($server)
    Assert ($server.ExitCode -eq 0) "$Name clean exit"
    Assert (-not ((Log) -cmatch $bad)) "$Name no linkage, ticking or script errors"
    $script:server = $null
}
function Write-Fixture([string]$Source, [string]$Destination, [string]$Name, [string]$Old) {
    $text = [IO.File]::ReadAllText($Source)
    if ($text.Contains('"RwArmy"')) { throw 'Seed already has an army.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm119-army'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'server.properties'), (Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $seed 'customnpcs/clones/1') -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^RwArmies=(?!0\r?$)')) 'config copy has armies off'
    $sceneCfg = [IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg'))
    Assert (-not ($sceneCfg -match '(?m)^RwArmyClusterRadius=(?!48\r?$)')) 'default cluster radius 48'
    Assert (-not ($sceneCfg -match '(?m)^RwArmyCompany=(?!8\r?$)')) 'default company size 8'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('water', 'gap', 'throat', 'trap', 'loss')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "EDGE_$name.json") "EDGE_$name" 'M36_Stand'
    }
    foreach ($name in @('EDGE_Ctrl', 'EDGE_Resume')) {
        $dest = Join-Path $clones "$name.json"
        Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'army-edge-control.js') $dest
        Write-Fixture $dest $dest $name 'PERF_Kontrola'
    }
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # Bootstrap before moving spawn: the next start prepares all five scenes, x=-16..208.
    Start-Phase 'p1-bootstrap' 1
    Send 'setworldspawn 96 4 0'
    Expect 'Set the world spawn point to (96, 4, 0)' 'spawn covers all edge scenes'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    Start-Phase 'p2-edges' 1
    Send 'gamerule doMobSpawning false'
    Send 'gamerule doDaylightCycle false'
    Send 'weather clear'
    # Shallow one-block water with its natural dirt bed; no invisible bridge.
    Send 'fill -16 3 7 16 3 10 minecraft:water'
    # Four-block-deep void cut, wider than any route within NpcNavRange; no way across.
    Send 'fill 16 0 7 80 3 10 minecraft:air'
    # Five-block-long two-block-wide throat in a four-block-high wall.
    Send 'fill 64 4 8 128 7 12 minecraft:stone'
    Send 'fill 95 4 8 96 7 12 minecraft:air'
    # Enclose exactly one of the trap company's eight members before its order.
    Send 'fill 140 4 -4 142 6 -2 minecraft:stone'
    Send 'fill 141 4 -3 141 5 -3 minecraft:air'
    $names = @('water', 'gap', 'throat', 'trap', 'loss')
    $centres = @(0, 48, 96, 144, 192)
    for ($j = 0; $j -lt $names.Count; $j++) {
        for ($i = 0; $i -lt 8; $i++) {
            $x = $centres[$j] - 3 + ($i % 4) * 2 + 0.5
            $z = -3 + [Math]::Floor($i / 4) * 2 + 0.5
            $point = [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0},4,{1}', $x, $z)
            Send "noppes clone spawn EDGE_$($names[$j]) 1 $point"
        }
    }
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'forced chunks (Q308); not a ticket cost measurement'
    Assert ((Log) -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=40') 'exactly forty workers before the controller'
    Send 'noppes clone spawn EDGE_Ctrl 1 0,4,-10'
    Expect 'ARMYEDGE MID restart checks=' 'all terrain checks, losses and stable arrival complete; returning while MOVING' 240
    $p2 = Log
    $mid = [regex]::Match($p2, 'ARMYEDGE MID restart checks=(\d+) failed=0 [^\r\n]*state=MOVING')
    Assert ($mid.Success) 'all script checks pass before restart'
    Assert (-not ($p2 -match 'ARMYEDGE CHECK [^\r\n]*ok=0')) 'no failed check hidden by a final marker'
    foreach ($name in $names) {
        $want = if ($name -in @('gap', 'trap')) { 'BLOCKED' } else { 'ARRIVED' }
        Assert ($p2 -match ('ARMYEDGE METRIC army=' + $name + ' state=' + $want)) "$name expected terminal state and position metrics"
    }
    Assert (([regex]::Matches($p2, 'ARMYEDGE POSITION army=loss uuid=')).Count -eq 5) 'five survivor positions recorded'
    Assert ($p2.Contains('ARMYEDGE LOSS killed=2 left=1')) 'real damage and unassignment recorded during movement'
    Assert ($p2.Contains('key=arrival-stays-terminal ok=1')) 'arrival held for one hundred ticks'
    # No command is issued after the saved return order, including after restart.
    Stop-Phase 'p2-edges'
    Assert (Test-Path -LiteralPath $dat) 'army states and current return order saved'

    Start-Phase 'p3-reload' 1
    Expect 'RWARMY register loaded armies=5 skipped=0' 'five army records load'
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'same loaded scene after world reload'
    Send 'noppes clone spawn EDGE_Resume 1 0,4,-10'
    Expect 'ARMYEDGE DONE phase=resume checks=' 'saved return order reaches its goal with five survivors' 240
    $p3 = Log
    Assert ($p3 -match 'ARMYEDGE DONE phase=resume checks=\d+ failed=0') 'all reload checks pass'
    Assert (-not ($p3 -match 'ARMYEDGE CHECK [^\r\n]*ok=0')) 'no reload assertion failed'
    Assert (([regex]::Matches($p3, 'ARMYEDGE RELOADPOSITION uuid=')).Count -eq 5) 'five physical survivor positions after reload'
    Assert ($p3.Contains('key=loss-no-respawn ok=1')) 'deaths and departure persist after reload'
    Assert (-not $p3.Contains('RWARMY tickets stale-released')) 'no stale army ticket survived a clean stop'
    Stop-Phase 'p3-reload'
    $datHash = Fingerprint $dat

    Start-Phase 'p4-off' 0
    Send 'rwarmy status loss'
    Expect 'Unknown command' 'no army command at zero'
    Assert (-not (Log).Contains('RWARMY register loaded') -and -not (Log).Contains('RWARMY tickets')) 'no army runtime at zero'
    Send 'rwsquad status'
    Expect 'RWSQUAD ni aktivnih enot' 'no revived transient companies at zero'
    Stop-Phase 'p4-off'
    Assert ((Fingerprint $dat) -ceq $datHash) 'saved register byte-identical at zero'
    Unchanged
    @{success=$true; package='M11.9'; checks=@($checks); scriptChecksBeforeReload=[int]$mid.Groups[1].Value;
      logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      control=(Get-FileHash -LiteralPath (Join-Path $seed 'army-edge-control.js')).Hash;
      condition='dev dedicated, five isolated eight-member armies; default company=8, RwArmies=1, legion speed3 passive; natural shallow water, void cut, two-block throat, one caged member; two real permanent deaths and one unassignment while MOVING; arrival stable 100 ticks; return speed1 and world restart without reissue; byte equality at 0; harness chunk tickets, no performance measurement, no worker teleport or healing'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M119 evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $server) { try { if (-not $server.HasExited) { Send 'stop'; if (-not $server.WaitForExit(60000)) { Stop-JavaPrototypeProcess $server } }; Stop-JavaPrototypeProcess $server } catch { Write-Host "Cleanup failure: $_"; $exitCode = 1 } }
    foreach ($id in @($owned.Keys)) { if (Get-Process -Id $id -ErrorAction SilentlyContinue) { & taskkill.exe /PID $id /T /F 2>&1 | Out-Null; $exitCode = 1 } }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
