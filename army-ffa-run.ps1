# M11.11 controller scenario: free-for-all march of 200 scattered members over an obstacle course
# (wall with a 3-wide gap, stepped hill, pillars, pond), one member walled in, restart mid-march.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m1111-ffa-$stamp"
$work = Join-Path $root "dev/run-m1111/$stamp"
$world = 'm1111-world'
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
function Send([string]$Text) { $server.StandardInput.WriteLine($Text); $server.StandardInput.Flush(); Start-Sleep -Milliseconds 300 }
function Wait-Marker([string]$Marker, [int]$Seconds = 60) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Log
        if ($text.Contains($Marker)) { return $true }
        if ($text.Contains('ARMYFFA FAILURE')) { throw "Army failed before '$Marker'; $serverLog" }
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
function Write-Fixture([string]$Source, [string]$Destination, [string]$Name, [string]$Old) {
    $text = [IO.File]::ReadAllText($Source)
    if ($text.Contains('"RwArmy"')) { throw 'Seed already has an army.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Done-Ok([string]$Phase) {
    $m = [regex]::Match((Log), "ARMYFFA DONE phase=$Phase checks=(\d+) failed=(\d+)")
    Assert ($m.Success -and $m.Groups[2].Value -eq '0') "controller $Phase checks=$($m.Groups[1].Value) failed=$($m.Groups[2].Value)"
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm1111-ffa'
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
    Assert (-not ($sceneCfg -match '(?m)^NpcNavRange=(?!32\r?$)')) 'navigation range 32'
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
    Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones 'FFA_M.json') 'FFA_M' 'M36_Stand'
    Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones 'FFA_Box.json') 'FFA_Box' 'M36_Stand'
    foreach ($name in @('FFA_Ctrl', 'FFA_Resume')) {
        $dest = Join-Path $clones "$name.json"
        Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'army-ffa-control.js') $dest
        Write-Fixture $dest $dest $name 'PERF_Kontrola'
    }
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1 bootstrap
    Start-Phase 'p1-bootstrap' 1
    Send 'list'
    Expect 'players online' 'console ready'
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'spawn at origin'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    # 2 course, 200 members, start, restart mid-march.
    Start-Phase 'p2-start' 1
    Send 'list'
    Expect 'players online' 'console ready p2'
    Send 'gamerule doMobSpawning false'
    # Wall across the course with one 3-wide gap far to the side.
    Send 'fill -50 4 30 50 6 30 minecraft:stone'
    Send 'fill 18 4 30 20 6 30 minecraft:air'
    # Stepped hill, one block per step.
    Send 'fill -50 4 45 50 4 60 minecraft:stone'
    Send 'fill -50 5 48 50 5 57 minecraft:stone'
    Send 'fill -50 6 51 50 6 54 minecraft:stone'
    # Pillars.
    foreach ($z in @(66, 71, 76)) { for ($x = -45; $x -le 45; $x += 6) { $px = $x + (($z / 5) % 3); Send "fill $px 4 $z $px 6 $z minecraft:cobblestone" } }
    # One-block deep pond.
    Send 'fill -15 3 84 15 3 90 minecraft:water'
    # Closed box for one member: spawn it first, then build the box around it.
    Send 'noppes clone spawn FFA_Box 1 60,4,0'
    Start-Sleep -Seconds 2
    Send 'fill 58 3 -2 62 7 2 minecraft:stone 0 hollow'
    Expect '98 blocks filled' 'box closed around one member'
    Expect 'blocks filled' 'obstacle course built'
    for ($i = 0; $i -lt 199; $i++) {
        $x = -45 + (($i * 37) % 91)
        $z = -30 + (($i * 53) % 41)
        Send "noppes clone spawn FFA_M 1 $x,4,$z"
    }
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'forced scene chunks (Q308)'
    Send 'noppes clone spawn FFA_Ctrl 1 0,4,-40'
    Expect 'ARMYFFA DONE phase=setup' 'setup finished' 180
    Done-Ok 'setup'
    Expect 'RWARMY freeforall ffa members=200' 'free-for-all march started with 200 members' 120
    Expect 'ARMYFFA MID' 'half of the army is past the wall gap while MOVING' 900
    Send 'rwarmy status ffa'
    Expect 'mode=freeforall members=200' 'status reports free-for-all counts'
    Stop-Phase 'p2-start'
    Assert (Test-Path -LiteralPath $dat) 'intent saved in data/rwarmies.dat'

    # 3 no new command: resume from saved intent and finish.
    Start-Phase 'p3-resume' 1
    Send 'list'
    Expect 'players online' 'console ready p3'
    Expect 'RWARMY register loaded armies=1 skipped=0' 'saved register and order load'
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'resume chunks forced'
    Send 'noppes clone spawn FFA_Resume 1 0,4,-40'
    Expect 'ARMYFFA DONE phase=resume' 'march finished after restart' 1800
    $p3 = Log
    Send 'rwarmy status ffa'
    Start-Sleep -Seconds 2
    Done-Ok 'resume'
    Assert ($p3.Contains('RWARMY freeforall-end ffa')) 'end summary logged'
    Assert ($p3.Contains('RWARMY freeforall-blocked ffa')) 'walled-in member reported blocked'
    $all = $p3 + (Read-JavaPrototypeLog "$prefix-p2-start.log")
    Assert (-not ($all -match 'RWARMY march ffa companies=')) 'no companies were used'
    Stop-Phase 'p3-resume'
    Unchanged
    @{success=$true; package='M11.11'; checks=@($checks); logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash; control=(Get-FileHash -LiteralPath (Join-Path $seed 'army-ffa-control.js')).Hash;
      condition='dev dedicated server, flat seed, 199 members scattered over 91x41 blocks plus one walled in; course: wall z30 with a 3-wide gap at x18-20, stepped hill z45-60, pillars z66-76, pond z84-90; goal 0,4,115 freeforall legion speed 3 engage; restart after 100 members passed the wall; NpcNavRange=32; diagnostic chunk tickets only; no teleport'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M1111 evidence: $prefix.json"
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
