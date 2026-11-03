# M11.5 controller scenario. Four isolated starts: setup, long march, resume, switch off.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m115-army-$stamp"
$work = Join-Path $root "dev/run-m115/$stamp"
$world = 'm115-world'
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
        if ($text -match 'ARMYLONG DONE phase=\w+ checks=\d+ failed=[1-9]') {
            throw "Army assertions failed before '$Marker'; $serverLog"
        }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before '$Marker'; $serverLog" }
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
# entitydata with an empty tag fails and prints the whole NPC tag: a read-only NBT probe.
function Npc-Tag([string]$Name) {
    $before = [regex]::Matches((Log), 'The data tag did not change').Count
    Send "entitydata @e[name=$Name,c=1] {}"
    $until = (Get-Date).AddSeconds(30)
    do {
        $lines = @((Log) -split "`n" | Where-Object { $_.Contains('The data tag did not change') })
        if ($lines.Count -gt $before) { return $lines[$lines.Count - 1] }
        Start-Sleep -Milliseconds 300
    } while ((Get-Date) -lt $until)
    throw "No NBT probe reply for $Name"
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
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm115-army'
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
    Assert (-not ($sceneCfg -match '(?m)^NpcNavRange=(?!32\r?$)')) 'navigation range 32'
    Assert (-not ($sceneCfg -match '(?m)^RwPathResume=(?!0\r?$)')) 'individual path continuation stays off'
    Assert (-not ($sceneCfg -match '(?m)^RwNavBackend=(?!0\r?$)')) 'vanilla navigation backend'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones 'LONG_M.json') 'LONG_M' 'M36_Stand'
    foreach ($name in @('LONG_Ctrl', 'LONG_Resume')) {
        $dest = Join-Path $clones "$name.json"
        Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'army-march-control.js') $dest
        Write-Fixture $dest $dest $name 'PERF_Kontrola'
    }
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1 bootstrap (the same flat seed and config copy as M11.1; no changed production environment).
    Start-Phase 'p1-bootstrap' 1
    Send 'list'
    Expect 'players online' 'console ready'
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'spawn at origin'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    # 2: 32 clustered members; 240-block goal is 7.5 times NpcNavRange.
    Start-Phase 'p2-start' 1
    Send 'list'
    Expect 'players online' 'console ready p2'
    Send 'gamerule doMobSpawning false'
    for ($i = 0; $i -lt 32; $i++) {
        $x = -7 + ($i % 8) * 2
        $z = -7 + [Math]::Floor($i / 8) * 2
        Send "noppes clone spawn LONG_M 1 $x,4,$z"
    }
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'forced scene chunks (Q308)'
    Assert ((Log) -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=32') '32 workers forced before the controller starts'
    Send 'noppes clone spawn LONG_Ctrl 1 0,4,10'
    Expect 'ARMYLONG DONE phase=setup checks=36 failed=0' '32 assignments and durable 240-block order' 120
    Expect 'RWARMY march north companies=4 members=32 paths=1 size=8' 'joined army uses four companies and one path' 180
    Expect 'ARMYLONG MID members=32' 'all 32 have passed 35 blocks over several waypoints' 240
    Assert (-not (Log).Contains('ARMYLONG FAILURE')) 'no blocked march before restart'
    # Restart after several legs, before reaching the goal. No new move on resume.
    Send 'rwarmy status north'
    Expect 'RWARMY order north state=MOVING dim=0 target=0.0,4.0,240.0 shape=LEGIJA company=8' 'final intent is not overwritten by waypoints'
    Assert (-not (Log).Contains('RWARMY order north state=ARRIVED')) 'restart is during movement'
    Stop-Phase 'p2-start'
    Assert (Test-Path -LiteralPath $dat) 'intent saved in data/rwarmies.dat'

    # 3: no new move command. Resume from saved intent; only observe actual positions.
    Start-Phase 'p3-resume' 1
    Send 'list'
    Expect 'players online' 'console ready p3'
    Expect 'RWARMY register loaded armies=1 skipped=0' 'saved register and order load without errors'
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'resume chunks forced'
    Send 'noppes clone spawn LONG_Resume 1 0,4,10'
    Expect 'ARMYLONG DONE phase=resume checks=39 failed=0' 'restart reaches 240: all 32 walk over 200 blocks to distinct slots' 600
    $p3 = Log
    Assert ([regex]::Matches($p3, 'ARMYLONG POSITION uuid=').Count -eq 32) '32 unique member position reports'
    Assert (@([regex]::Matches($p3, 'ARMYLONG POSITION uuid=([a-f0-9-]+)') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique).Count -eq 32) 'all reported UUIDs unique'
    Send 'rwarmy status north'
    Expect 'RWARMY order north state=ARRIVED' 'arrival only at final goal'
    $allLogs = $p3 + (Read-JavaPrototypeLog "$prefix-p2-start.log")
    $waypoints = [regex]::Matches($allLogs, 'RWARMY waypoint north [^\r\n]* manhattan=([^ ]+) range=([^ ]+) final=(true|false)')
    Assert ($waypoints.Count -ge 8) 'at least eight real bounded anchor path requests'
    foreach ($w in $waypoints) {
        $distance = [double]::Parse($w.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
        $range = [double]::Parse($w.Groups[2].Value, [Globalization.CultureInfo]::InvariantCulture)
        Assert ($distance -le 24.000001 -and $distance -le $range - 7.999999) 'waypoint distance bounded by navigation range minus margin'
    }
    Assert ($allLogs.Contains('final=true') -and $allLogs.Contains('final=false')) 'both intermediate and final waypoints observed'
    # A target buried inside solid stone must BLOCK, never arrive or fabricate a straight path.
    # A fixture NPC lets the diagnostic loader generate the entire fill area; it is not an army member.
    Send 'noppes clone spawn LONG_M 1 0,4,270'
    Send 'rwdiag chunks on 1'
    Send 'fill -16 4 256 16 14 285 minecraft:stone'
    Expect '10890 blocks filled' 'solid obstacle fixture'
    Send 'rwarmy move north 0 4 268 legion 3'
    Expect 'RWARMY move north' 'blocked target command accepted as intent'
    Expect 'RWARMY order north state=BLOCKED' 'incomplete real path fails closed' 180
    Send 'rwarmy stop north'
    Expect 'RWARMY cancelled north' 'stop after blocked target'
    Stop-Phase 'p3-resume'
    $datHash = Fingerprint $dat

    # 4 equality at 0: no army runtime or command; the saved order file stays bit-identical.
    Start-Phase 'p4-off' 0
    Send 'list'
    Expect 'players online' 'console ready p4'
    Send 'rwarmy status north'
    Expect 'Unknown command' 'no army command at 0'
    Assert (-not (Log).Contains('RWARMY register loaded')) 'register not loaded at 0'
    Send 'rwsquad status'
    Expect 'RWSQUAD ni aktivnih enot' 'no transient companies or listeners resume at 0'
    Stop-Phase 'p4-off'
    Assert ((Fingerprint $dat) -ceq $datHash) 'saved order file untouched at 0'
    Unchanged
    @{success=$true; package='M11.5'; checks=@($checks); logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash; control=(Get-FileHash -LiteralPath (Join-Path $seed 'army-march-control.js')).Hash;
      condition='dev dedicated server; isolated original flat config; 32 ground members, company size 8; target 0,4,240 legion speed3, NpcNavRange=32, individual resume/backend off; restart after all members pass z35 while MOVING; all walk >=200 to <=12 from goal, separation >=1; bounded requests <=min(24,range-8); blocked stone target; no teleport or performance repetition; diagnostic chunk tickets only'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M115 evidence: $prefix.json"
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
