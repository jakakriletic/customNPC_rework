# M11.1b controller scenario: rights of /rwarmy on a dev dedicated server (runServer) in an isolated run directory.
# Two scripted controller NPCs drive the real command handler with stand-in players (army-perm-control.js).
# Four starts of the same world:
#   1 RwArmies=1  bootstrap: world spawn at the origin so the probes sit in spawn chunks
#   2 RwArmies=1  rights matrix, grant, revoke, operator list, script API, save
#   3 RwArmies=1  restart: grants and revocations survive, a renamed player keeps the grant
#   4 RwArmies=0  no command; the register file is not touched
# No client, no performance measurement.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m111b-perm-$stamp"
$work = Join-Path $root "dev/run-m111/b-$stamp"
$world = 'm111b-world'
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
function Write-Fixture([string]$Source, [string]$Destination, [string]$Name, [string]$Old) {
    $text = [IO.File]::ReadAllText($Source)
    if ($text.Contains('"RwArmy"')) { throw 'Seed already has an army.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
# Waits for a controller part, then requires its count of unique successful checks; failed ones are listed.
function Expect-Part([string]$Part, [int]$MinChecks) {
    $marker = "ARMYPERM DONE part=$Part"
    if (-not (Wait-Marker $marker 120)) { throw "No result from controller $Part" }
    $text = Log
    $rows = @([regex]::Matches($text, 'ARMYPERM CHECK i=(\d+) key=([\w-]+) ok=([01])'))
    foreach ($row in $rows) { if ($row.Groups[3].Value -ne '1') { Write-Host ($text -split "`n" | Where-Object { $_.Contains("key=$($row.Groups[2].Value) ok=0") }) } }
    $fail = @($rows | Where-Object { $_.Groups[3].Value -ne '1' })
    $keys = @($rows | ForEach-Object { $_.Groups[2].Value } | Select-Object -Unique)
    Assert ($fail.Count -eq 0) "$Part no failed rights check ($($fail.Count) failed)"
    Assert ($rows.Count -ge $MinChecks -and $keys.Count -eq $rows.Count) "$Part $($rows.Count) unique rights checks"
    foreach ($row in $rows) { [void]$checks.Add("$Part $($row.Groups[2].Value)") }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm111b-perm'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'server.properties'), (Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $seed 'customnpcs/clones/1') -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Force -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^RwArmies=(?!0\r?$)')) 'config copy has armies off'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    Assert ($props -match '(?m)^online-mode=false\s*$') 'offline mode: op needs no network'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('PERM_N', 'PERM_S', 'PERM_W', 'PERM_F')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "$name.json") $name 'M36_Stand'
    }
    foreach ($name in @('PERM_Main', 'PERM_Restart')) {
        $controller = Join-Path $clones "$name.json"
        Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'army-perm-control.js') $controller
        Write-Fixture $controller $controller $name 'PERF_Kontrola'
    }
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1: bootstrap
    Start-Phase 'p1-bootstrap' 1
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'world spawn at the origin'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 180
    Stop-Phase 'p1-bootstrap'

    # 2: rights matrix. Forced chunks keep the controller ticking without a player (Q308).
    Start-Phase 'p2-rights' 1
    Send 'gamerule doMobSpawning false'
    foreach ($spawn in @('PERM_N 1 2,4,2', 'PERM_N 1 4,4,2', 'PERM_S 1 6,4,2', 'PERM_W 1 2,4,6', 'PERM_F 1 4,4,6')) {
        Send "noppes clone spawn $spawn"
    }
    Start-Sleep -Seconds 2
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'chunks of the fixtures forced'
    Send 'noppes clone spawn PERM_Main 1 0,4,10'
    Expect-Part 'PERM_Main' 100
    Send 'rwarmy grants'
    Expect 'RWARMY grants count=4' 'console sees all four remaining grant records'
    Send 'rwdiag chunks off'
    Expect 'RWDIAG-CHUNKS stanje=off' 'forced chunks released'
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p2-rights'
    Assert (Test-Path -LiteralPath $dat) 'register file data/rwarmies.dat written'

    # 3: restart keeps grants
    Start-Phase 'p3-restart' 1
    Send 'rwarmy list'
    Expect 'RWARMY list armies=6' 'six armies after restart'
    Assert ((Log) -match 'RWARMY register loaded armies=6 skipped=0') 'register with grants loaded without skipped entries'
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'chunks forced after restart'
    Send 'noppes clone spawn PERM_Restart 1 0,4,10'
    Expect-Part 'PERM_Restart' 10
    Send 'rwdiag chunks off'
    Expect 'RWDIAG-CHUNKS stanje=off' 'forced chunks released after restart'
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved after restart' 180
    Stop-Phase 'p3-restart'
    $datHash = Fingerprint $dat

    # 4: switch off
    Start-Phase 'p4-off' 0
    Send 'rwarmy list'
    Expect 'Unknown command' 'no /rwarmy with RwArmies=0'
    Assert (-not (Log).Contains('RWARMY register loaded')) 'register not loaded with RwArmies=0'
    Stop-Phase 'p4-off'
    Assert ((Fingerprint $dat) -ceq $datHash) 'register file untouched with RwArmies=0'

    Unchanged
    @{success = $true; package = 'M11.1b'; checks = @($checks); logs = @(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; control = (Get-FileHash -LiteralPath (Join-Path $seed 'army-perm-control.js')).Hash; register = $datHash;
      condition = 'dev dedicated server (runServer), isolated run directory with a copy of dev/run/config, RwArmies set in the copy; four starts of one flat world; stand-in players through the real command handler (FakePlayer as entity, level -1 = not an operator, level 2 = operator), one real entry of the operator list; no client'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M111B evidence: $prefix.json ($($checks.Count) checks)"
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
