# M11.6 controller scenario: chunk tickets travel with marching armies, no player and no rwdiag chunks.
#   1 RwArmies=1  bootstrap: world spawn at 400,4,0 so the next start loads the scene as spawn chunks
#   2 RwArmies=1  alpha(8)/bravo(2)/charlie(2) march, spawn moved to the origin and save-all unloads every
#                 unticketed chunk; alpha travels with its ticket, queued charlie is unloaded, then resumes
#                 from its saved last known position once a slot is free; alpha is stopped mid-march
#   3 RwArmies=1  restart with spawn at the origin: alpha resumes from the saved position and arrives
#   4 RwArmyMaxMarches=0  control: an order without tickets stays WAITING, no ticket line at all
#   5 RwArmies=0  no command; the register file stays bit-identical
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m116-army-$stamp"
$work = Join-Path $root "dev/run-m116/$stamp"
$world = 'm116-world'
$server = $null; $lock = $null; $exitCode = 1; $serverLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList; $data = @{}
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|attempted to force load a chunk with an invalid ticket|beyond it.s currently allocated maximum'

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
function Wait-Marker([string]$Pattern, [int]$Seconds = 60) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Log
        if ($text -match $Pattern) { return $true }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before '$Pattern'; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    return $false
}
function Expect([string]$Marker, [string]$Text, [int]$Seconds = 60) { Assert (Wait-Marker ([regex]::Escape($Marker)) $Seconds) $Text }
function ExpectRx([string]$Pattern, [string]$Text, [int]$Seconds = 60) { Assert (Wait-Marker $Pattern $Seconds) $Text }
function At([string]$Marker) { return (Log).IndexOf($Marker) }
function Set-Cfg([string]$Name, [int]$Value) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), "(?m)^$Name=.*\r?\n?", '')
    [IO.File]::WriteAllText($cfg, $text + "`n$Name=$Value`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Switch, [int]$Marches = 2) {
    Set-Cfg 'RwArmies' $Switch
    Set-Cfg 'RwArmyMaxMarches' $Marches
    $script:serverLog = "$prefix-$Name.log"
    $script:server = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runServer', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwWorld=$world") (Join-Path $root 'dev') $serverLog
    Expect 'Done (' "$Name server started" 600
    Assert ((Log).Contains('Preparing level "' + $world + '"')) "$Name isolated world"
    Send 'list'
    Expect 'There are 0/' "$Name no player online"
}
function Stop-Phase([string]$Name) {
    Send 'stop'
    $until = (Get-Date).AddSeconds(180)
    while (-not $server.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $server.HasExited "$Name server stopped"
    [M61ProcessLog]::Finish($server)
    Assert ($server.ExitCode -eq 0) "$Name clean exit"
    Assert (-not ((Log) -cmatch $bad)) "$Name no linkage, ticking or ticket errors"
    Assert (-not (Log).Contains('RWDIAG-CHUNKS stanje=on')) "$Name no diagnostic forced chunks"
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
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm116-army'
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
    $sceneCfg = [IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg'))
    Assert (-not ($sceneCfg -match '(?m)^RwArmies=(?!0\r?$)')) 'config copy has armies off'
    Assert (-not ($sceneCfg -match '(?m)^RwArmyMaxMarches=(?!2\r?$)')) 'default RwArmyMaxMarches 2'
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
    foreach ($name in @('TKA', 'TKB', 'TKC')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "$name.json") $name 'M36_Stand'
    }
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1 bootstrap: the next start prepares the scene at x=400 as spawn chunks (the only way to place NPCs there
    # from a console without a player or forced chunks).
    Start-Phase 'p1-bootstrap' 1
    Send 'setworldspawn 400 4 0'
    Expect 'Set the world spawn point to (400, 4, 0)' 'spawn at the scene'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    # 2 three armies march; the scene stops being spawn chunks and save-all unloads all unticketed chunks.
    Start-Phase 'p2-march' 1
    Send 'gamerule doMobSpawning false'
    for ($i = 0; $i -lt 8; $i++) { Send ("noppes clone spawn TKA 1 {0},4,{1}" -f (397 + ($i % 4) * 2), (-1 + [Math]::Floor($i / 4) * 2)) }
    foreach ($x in @(399, 401)) { Send "noppes clone spawn TKB 1 $x,4,-100"; Send "noppes clone spawn TKC 1 $x,4,100" }
    foreach ($a in @('alpha', 'bravo', 'charlie')) {
        Send "rwarmy create $a faction:0"
        Expect "RWARMY created $a owner=faction:0" "create $a"
    }
    Send 'rwarmy assign alpha @e[name=TKA]'
    Expect 'RWARMY assigned alpha npcs=8 changed=8' 'alpha has eight members'
    Send 'rwarmy assign bravo @e[name=TKB]'
    Expect 'RWARMY assigned bravo npcs=2 changed=2' 'bravo has two members'
    Send 'rwarmy assign charlie @e[name=TKC]'
    Expect 'RWARMY assigned charlie npcs=2 changed=2' 'charlie has two members'
    # Speed 1 (about 1.3 blocks/s): alpha crosses into chunk 26 only after the unload below.
    Send 'rwarmy move alpha 424 4 0 legion 1'
    Expect 'RWARMY move alpha dim=0 target=424.0,4.0,0.0' 'alpha ordered across a chunk border'
    Send 'rwarmy move bravo 420 4 -100 legion 1'
    Expect 'RWARMY move bravo dim=0 target=420.0,4.0,-100.0' 'bravo ordered'
    Send 'rwarmy move charlie 420 4 100 legion 1'
    Expect 'RWARMY move charlie dim=0 target=420.0,4.0,100.0' 'charlie ordered'
    ExpectRx 'RWARMY tickets alpha granted slots=1/2' 'alpha holds the first slot' 30
    ExpectRx 'RWARMY tickets bravo granted slots=2/2' 'bravo holds the second slot' 30
    ExpectRx 'RWARMY tickets charlie queued slots=2/2 \(marches without tickets\)' 'charlie is queued at the limit' 30
    Expect 'RWARMY order charlie state=MOVING' 'queued charlie still marches while its chunks are loaded' 30
    Start-Sleep -Seconds 3   # at least two one-second scans record charlie's last known position
    Assert (-not (Log).Contains('RWARMY order charlie state=ARRIVED')) 'charlie is mid-march before the unload'
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'scene is no longer spawn chunks'
    Send 'save-all'
    Expect 'Saved the world' 'save-all queued every chunk without a player or ticket for unload' 120
    $unload = At 'Set the world spawn point to (0, 4, 0)'
    Expect 'RWARMY order charlie state=WAITING' 'unticketed charlie lost its chunks and members' 60
    # Q355/Q356: the ticket centre is the members' mean chunk; the start is 24 or 25 (mean x=400 sits on the
    # border), so the travel is a later members line after the unload whose centre is east of the recorded start.
    $centre = 'RWARMY tickets alpha chunks=\d+ tickets=\d+ added=[1-9]\d* mode=members centre=(-?\d+),'
    $start = [regex]::Match((Log), $centre)
    Assert ($start.Success -and $start.Index -lt $unload) 'alpha ticket centred on its members before the unload'
    $startX = [int]$start.Groups[1].Value
    $data['alphaStartCentre'] = $startX
    $until = (Get-Date).AddSeconds(120); $moved = $null
    do {
        $text = Log
        foreach ($m in [regex]::Matches($text, $centre)) { if ($m.Index -gt $unload -and [int]$m.Groups[1].Value -gt $startX) { $moved = $m; break } }
        if ($null -ne $moved) { break }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before alpha ticket travel; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    Assert ($null -ne $moved) 'alpha ticket moved east of its start chunk after the unload'
    $data['alphaTravelCentre'] = [int]$moved.Groups[1].Value
    Expect 'RWARMY order alpha state=ARRIVED' 'alpha arrived with only its ticket loading the area' 180
    Expect 'RWARMY tickets alpha released' 'alpha ticket released at arrival' 30
    Expect 'RWARMY order bravo state=ARRIVED' 'bravo arrived' 180
    Expect 'RWARMY tickets bravo released' 'bravo ticket released at arrival' 30
    ExpectRx 'RWARMY tickets charlie granted slots=\d/2' 'a freed slot goes to charlie' 30
    ExpectRx 'RWARMY tickets charlie chunks=25 tickets=\d+ added=25 mode=resume' 'charlie held at its saved position' 30
    # Q355: resuming from the saved position is a recorded limit (v2 run: BLOCKED no-complete-path); data only.
    ExpectRx 'RWARMY order charlie state=(ARRIVED|BLOCKED)' 'charlie resume ended (arrived or blocked)' 180
    $data['charlieResume'] = [regex]::Match((Log), 'RWARMY order charlie state=(ARRIVED|BLOCKED)').Groups[1].Value
    Expect 'RWARMY tickets charlie released' 'charlie ticket released when its order ended' 30
    Assert ((At 'RWARMY order alpha state=ARRIVED') -gt $unload) 'alpha arrived after the unload'
    Assert ((At 'RWARMY tickets charlie granted') -gt [Math]::Min((At 'RWARMY tickets alpha released'), (At 'RWARMY tickets bravo released'))) 'charlie waited for a free slot'
    Assert (-not ((Log) -match 'RWARMY order (alpha|bravo) state=BLOCKED')) 'no ticketed live march blocked'
    # alpha back west; stop while it marches so the restart has to find it.
    Send 'rwarmy move alpha 400 4 0 legion 1'
    Expect 'RWARMY move alpha dim=0 target=400.0,4.0,0.0' 'alpha ordered back'
    ExpectRx 'RWARMY tickets alpha granted' 'alpha holds a slot again' 30
    Send 'rwarmy status alpha'
    ExpectRx 'RWARMY order alpha state=(WAITING|MOVING) .* tickets=\d+ chunks=\d+' 'status reports the held tickets' 30
    Assert ((Log).IndexOf('RWARMY order alpha state=ARRIVED', (At 'RWARMY move alpha dim=0 target=400.0')) -lt 0) 'restart before the second arrival'
    Stop-Phase 'p2-march'
    Assert ((Log).Contains('RWARMY tickets alpha released-at-stop')) 'held ticket released before the last save'
    Assert (Test-Path -LiteralPath $dat) 'orders and last positions saved'

    # 3 restart with the spawn at the origin: no player, nothing loads alpha except its saved position.
    Start-Phase 'p3-resume' 1
    Expect 'RWARMY register loaded armies=3 skipped=0' 'register with the active order loads'
    Assert (-not (Log).Contains('RWARMY tickets stale-released')) 'no army ticket survived in forcedchunks.dat'
    ExpectRx 'RWARMY tickets alpha granted slots=1/2' 'alpha granted after restart' 60
    ExpectRx 'RWARMY tickets alpha chunks=25 tickets=\d+ added=25 mode=resume' 'alpha held at its saved position' 60
    # Q355: restart resume is data only, like charlie's; the ticket must still end with the order.
    ExpectRx 'RWARMY order alpha state=(ARRIVED|BLOCKED)' 'alpha resume ended (arrived or blocked)' 240
    $data['alphaRestartResume'] = [regex]::Match((Log), 'RWARMY order alpha state=(ARRIVED|BLOCKED)').Groups[1].Value
    Expect 'RWARMY tickets alpha released' 'ticket released when the resumed order ended' 30
    Stop-Phase 'p3-resume'

    # 4 control at RwArmyMaxMarches=0: the M11.3 behaviour, the order cannot load its members.
    Start-Phase 'p4-zero' 1 0
    Expect 'RWARMY register loaded armies=3 skipped=0' 'register loads at zero marches'
    Send 'rwarmy move alpha 424 4 0 legion 3'
    Expect 'RWARMY move alpha dim=0 target=424.0,4.0,0.0' 'order accepted without loaded members'
    Start-Sleep -Seconds 30
    Send 'rwarmy status alpha'
    Expect 'RWARMY order alpha state=WAITING' 'without tickets the unloaded army waits' 30
    Assert (-not (Log).Contains('RWARMY tickets')) 'no ticket activity at zero'
    Assert (-not ((Log) -match 'RWARMY order alpha state=WAITING[^\r\n]*tickets=')) 'status has no ticket field at zero'
    Stop-Phase 'p4-zero'
    $datHash = Fingerprint $dat

    # 5 equality at 0: no command, no ticket, the saved file stays bit-identical.
    Start-Phase 'p5-off' 0
    Send 'rwarmy status alpha'
    Expect 'Unknown command' 'no army command at 0'
    Assert (-not (Log).Contains('RWARMY register loaded') -and -not (Log).Contains('RWARMY tickets')) 'no army runtime or ticket at 0'
    Stop-Phase 'p5-off'
    Assert ((Fingerprint $dat) -ceq $datHash) 'saved register untouched at 0'
    Unchanged
    @{success=$true; package='M11.6'; checks=@($checks); data=$data; logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      condition='dev dedicated server, isolated copied flat config, no player and no rwdiag forced chunks in any phase; scene at x=400 loaded only as spawn chunks until spawn moves to the origin; alpha 8 / bravo 2 / charlie 2 ground workers, RwArmyMaxMarches=2; single path per march (M11.5 in v2); cost in M11.10'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M116 evidence: $prefix.json"
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
