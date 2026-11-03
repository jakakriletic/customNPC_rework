# M11.1 controller scenario: army register, RwArmy on NPCs, /rwarmy and ArmyApi on a dev dedicated server
# (runServer) in an isolated run directory. Four starts of the same world:
#   1 RwArmies=1  bootstrap: world spawn at the origin so the probes sit in spawn chunks
#   2 RwArmies=1  create/assign/remove by command, script API by a controller NPC, save
#   3 RwArmies=1  restart: register and memberships survive
#   4 RwArmies=0  no command; RwArmy on the NPC kept unchanged; the register file is not touched
# No client, no performance measurement.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m111-army-$stamp"
$work = Join-Path $root "dev/run-m111/$stamp"
$world = 'm111-world'
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
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm111-army'
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
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('ARMY_A', 'ARMY_B', 'ARMY_C', 'ARMY_D')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "$name.json") $name 'M36_Stand'
    }
    $controller = Join-Path $clones 'ARMY_Ctrl.json'
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'army-control.js') $controller
    Write-Fixture $controller $controller 'ARMY_Ctrl' 'PERF_Kontrola'
    # Offline UUID of a player who never joins = UUID.nameUUIDFromBytes("OfflinePlayer:"+name).
    $md5 = [Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes('OfflinePlayer:M111_Owner'))
    $md5[6] = ($md5[6] -band 0x0f) -bor 0x30; $md5[8] = ($md5[8] -band 0x3f) -bor 0x80
    $hex = ($md5 | ForEach-Object { $_.ToString('x2') }) -join ''
    $owner = $hex.Substring(0, 8) + '-' + $hex.Substring(8, 4) + '-' + $hex.Substring(12, 4) + '-' + $hex.Substring(16, 4) + '-' + $hex.Substring(20, 12)
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1: bootstrap
    Start-Phase 'p1-bootstrap' 1
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'world spawn at the origin'
    Send 'rwarmy list'
    Expect 'RWARMY list armies=0' 'command present with RwArmies=1, empty register'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 180
    Stop-Phase 'p1-bootstrap'

    # 2: commands and script API
    Start-Phase 'p2-create' 1
    Send 'gamerule doMobSpawning false'
    foreach ($spawn in @('ARMY_A 1 2,4,2', 'ARMY_A 1 4,4,2', 'ARMY_A 1 6,4,2', 'ARMY_B 1 2,4,6', 'ARMY_C 1 4,4,6', 'ARMY_C 1 6,4,6', 'ARMY_D 1 8,4,6')) {
        Send "noppes clone spawn $spawn"
    }
    Start-Sleep -Seconds 2
    $before = Npc-Tag 'ARMY_B'
    Assert (-not $before.Contains('RwArmy')) 'fresh NPC has no RwArmy key'
    # Without a player WorldServer stops updating entities 300 ticks after start, so the controller
    # spawned below would never tick (attempt-1 run: TotalTicksAlive=0). A forced chunk keeps them ticking.
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'chunks of the fixtures forced'
    Assert ((Log) -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=7') 'seven fixture NPCs in forced chunks'
    Send "rwarmy create alpha uuid:$owner"
    Expect "RWARMY created alpha owner=uuid:$owner" 'create alpha owned by a player UUID'
    Send 'rwarmy create beta faction:2'
    Expect 'RWARMY created beta owner=faction:2 (Aggressive)' 'create beta owned by faction 2'
    Send 'rwarmy create Alpha faction:0'
    Expect 'RWARMY create Alpha refused: EXISTS' 'duplicate name refused without case'
    Send 'rwarmy create nobody'
    Expect 'RWARMY create needs an owner' 'console create without owner refused'
    Send 'rwarmy create gamma faction:999'
    Expect 'RWARMY unknown faction 999' 'unknown faction refused'
    Send 'rwarmy create bad:name faction:0'
    Expect "RWARMY invalid name 'bad:name'" 'invalid name refused'
    Send 'rwarmy create delta faction:0'
    Expect 'RWARMY created delta owner=faction:0' 'create delta'
    Send 'rwarmy assign alpha ARMY_A 50'
    Expect 'RWARMY assigned alpha npcs=3 changed=3' 'assign three NPCs by name and range'
    Send 'rwarmy assign beta @e[name=ARMY_B]'
    Expect 'RWARMY assigned beta npcs=1 changed=1' 'assign by entity selector'
    Send 'rwarmy assign BETA @e[name=ARMY_B]'
    Expect 'RWARMY assigned beta npcs=1 changed=0' 'repeat assign changes nothing'
    Send 'rwarmy assign nosuch ARMY_A 50'
    Expect 'RWARMY unknown army nosuch' 'unknown army refused'
    Send 'rwarmy assign delta ARMY_D 50'
    Expect 'RWARMY assigned delta npcs=1 changed=1' 'assign delta'
    Send 'noppes clone spawn ARMY_Ctrl 1 0,4,10'
    Expect 'ARMYAPI DONE checks=16 failed=0' 'script API: 16 checks passed' 120
    $rows = @([regex]::Matches((Log), 'ARMYAPI CHECK i=(\d+) key=([\w-]+) ok=([01])'))
    Assert ($rows.Count -eq 16 -and @($rows | Where-Object { $_.Groups[3].Value -ne '1' }).Count -eq 0 -and
        @($rows | ForEach-Object { $_.Groups[2].Value } | Select-Object -Unique).Count -eq 16) '16 unique successful API assertions'
    Send 'rwdiag chunks off'
    Expect 'RWDIAG-CHUNKS stanje=off' 'forced chunks released'
    Send 'rwarmy remove delta'
    Expect 'RWARMY removed delta cleared=1' 'remove clears the loaded member'
    Assert (-not (Npc-Tag 'ARMY_D').Contains('RwArmy')) 'removed army leaves no key on its NPC'
    Send 'rwarmy unassign ARMY_C 50'
    Expect 'RWARMY unassigned npcs=2 changed=2' 'unassign by name'
    Send 'rwarmy assign api_army ARMY_C 50'
    Expect 'RWARMY assigned api_army npcs=2 changed=2' 'reassign by name'
    Send 'rwarmy list'
    Expect 'RWARMY list armies=3' 'three armies listed'
    $text = Log
    Assert ($text.Contains("RWARMY army alpha owner=uuid:$owner loaded=3")) 'alpha has 3 loaded members'
    Assert ($text.Contains('RWARMY army beta owner=faction:2 (Aggressive) loaded=1')) 'beta has 1 loaded member'
    Assert ($text.Contains('RWARMY army api_army owner=faction:1 (Neutral) loaded=2')) 'api_army has 2 loaded members'
    Assert ((Npc-Tag 'ARMY_B').Contains('RwArmy:"beta"')) 'NPC tag stores the army name'
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p2-create'
    Assert (Test-Path -LiteralPath $dat) 'register file data/rwarmies.dat written'
    $datHash = Fingerprint $dat

    # 3: restart keeps register and memberships
    Start-Phase 'p3-restart' 1
    Send 'rwarmy list'
    Expect 'RWARMY register loaded armies=3 skipped=0' 'register loaded from disk after restart'
    Expect 'RWARMY list armies=3' 'three armies after restart'
    $text = Log
    Assert ($text.Contains("RWARMY army alpha owner=uuid:$owner loaded=3")) 'alpha members kept after restart'
    Assert ($text.Contains('RWARMY army beta owner=faction:2 (Aggressive) loaded=1')) 'beta member kept after restart'
    Assert ($text.Contains('RWARMY army api_army owner=faction:1 (Neutral) loaded=2')) 'api_army members kept after restart'
    Assert (-not $text.Contains('RWARMY army delta')) 'removed army stays removed'
    Send 'rwarmy info alpha'
    Expect 'RWARMY member ARMY_A dim=0 at' 'info lists members with position'
    Stop-Phase 'p3-restart'
    Assert ((Fingerprint $dat) -ceq $datHash) 'unchanged register not rewritten'

    # 4: switch off
    Start-Phase 'p4-off' 0
    Send 'rwarmy list'
    Expect 'Unknown command' 'no /rwarmy with RwArmies=0'
    Assert (-not (Log).Contains('RWARMY register loaded')) 'register not loaded with RwArmies=0'
    Assert ((Npc-Tag 'ARMY_B').Contains('RwArmy:"beta"')) 'RwArmy kept on the NPC with RwArmies=0'
    Assert (-not (Npc-Tag 'ARMY_D').Contains('RwArmy')) 'NPC without army still has no key with RwArmies=0'
    Stop-Phase 'p4-off'
    Assert ((Fingerprint $dat) -ceq $datHash) 'register file untouched with RwArmies=0'

    Unchanged
    @{success = $true; package = 'M11.1'; checks = @($checks); logs = @(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; control = (Get-FileHash -LiteralPath (Join-Path $seed 'army-control.js')).Hash; register = $datHash;
      condition = 'dev dedicated server (runServer), isolated run directory with a copy of dev/run/config, RwArmies set in the copy; four starts of one flat world; console commands and one scripted controller NPC (chunks forced by rwdiag during the API checks); no client'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M111 evidence: $prefix.json ($($checks.Count) checks)"
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
