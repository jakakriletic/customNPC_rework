# M13.2 controller-only scenario: group categories, scripts, undo, world reload and the transfer budget.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m132-groups-$stamp"
$work = Join-Path $root "dev/run-m132/$stamp"
$world = 'm132-world'
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
function Set-Config([int]$Groups, [int]$Own) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwLinkedGroups|RwLinkedBudget|RwLinkedOwnFields)=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwLinkedGroups=$Groups`nRwLinkedOwnFields=$Own`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Groups, [int]$Own) {
    Set-Config $Groups $Own
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
    if ($text.Contains('"LinkedNpcName": "m13')) { throw 'Seed already linked.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Write-Controller([string]$Mode, [string]$Name, [string]$Clones) {
    $code = Join-Path $work "linked-groups-control-$Mode.js"
    [IO.File]::WriteAllText($code, "var MODE = '$Mode';`n" + [IO.File]::ReadAllText((Join-Path $seed 'linked-groups-control.js')), (New-Object Text.UTF8Encoding $false))
    $dest = Join-Path $Clones "$Name.json"
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') $code $dest
    Write-Fixture $dest $dest $Name 'PERF_Kontrola'
}
function Spawn-Workers([string]$Mode, [int]$Z) {
    Send "noppes clone spawn M132A_$Mode 1 0.5,4,$Z.5"
    Send "noppes clone spawn M132B_$Mode 1 40.5,4,$Z.5"
    Send "noppes clone spawn M132C_$Mode 1 20.5,4,30.5"
    # Without a player the server stops entity updates after 300 ticks; forced chunks keep them going (Q308).
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' "$Mode forced chunks (Q308)"
}
function Expect-Done([string]$Mode, [int]$Seconds = 180) {
    Expect "LINKED2 DONE mode=$Mode " "$Mode controller finished" $Seconds
    $text = Log
    $m = [regex]::Match($text, "LINKED2 DONE mode=$Mode checks=(\d+) failed=(\d+)")
    Assert ($m.Success -and [int]$m.Groups[2].Value -eq 0 -and [int]$m.Groups[1].Value -gt 0) "$Mode all script checks pass ($($m.Groups[1].Value))"
    Assert (-not ($text -match 'LINKED2 CHECK [^\r\n]*ok=0')) "$Mode no failed check"
    return [int]$m.Groups[1].Value
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm132-groups'
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
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^(RwLinkedOwnFields|RwLinkedGroups)=(?!0\r?$)')) 'config copy has the linked switches at their default'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('M132A_off', 'M132B_off', 'M132C_off', 'M132A_on', 'M132B_on', 'M132C_on', 'M132W')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "$name.json") $name 'M36_Stand'
    }
    Write-Controller 'off' 'LINK2_CtrlOff' $clones
    Write-Controller 'on' 'LINK2_CtrlOn' $clones
    Write-Controller 'reload' 'LINK2_CtrlReload' $clones
    Write-Controller 'budget' 'LINK2_CtrlBudget' $clones
    $linkedDir = Join-Path $work "$world/customnpcs/linkednpcs"

    Start-Phase 'p1-bootstrap' 0 0
    Send 'setworldspawn 20 4 0'
    Expect 'Set the world spawn point to (20, 4, 0)' 'spawn chunks cover the linked NPCs'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    Start-Phase 'p2-off' 0 0
    Send 'gamerule doMobSpawning false'
    Send 'gamerule doDaylightCycle false'
    Spawn-Workers 'off' 0
    Send 'noppes clone spawn LINK2_CtrlOff 1 20.5,4,-10.5'
    $offChecks = Expect-Done 'off'
    Stop-Phase 'p2-off'
    $offFile = [IO.File]::ReadAllText((Join-Path $linkedDir 'm132off.json'))
    Assert (-not $offFile.Contains('RwCategories')) 'switch 0: group file has no categories key'
    Assert (-not $offFile.Contains('ScriptEnabled')) 'switch 0: group file has no scripts'
    Assert (-not (Test-Path -LiteralPath (Join-Path $linkedDir 'm132off.json_prev'))) 'switch 0: no kept version'

    Start-Phase 'p3-on' 1 0
    Spawn-Workers 'on' 10
    Send 'noppes clone spawn LINK2_CtrlOn 1 20.5,4,-12.5'
    $onChecks = Expect-Done 'on'
    $p3 = Log
    Assert ($p3.Contains('RWLINKED categories group=m132on now=appearance,stats')) 'command reply in the log'
    Send 'save-all flush'
    Expect 'Saved the world' 'on phase saved' 120
    Stop-Phase 'p3-on'
    Assert (Test-Path -LiteralPath (Join-Path $linkedDir 'm132on.json_prev')) 'kept version on disk'
    Assert ([IO.File]::ReadAllText((Join-Path $linkedDir 'm132on.json_prev')).Contains('ScriptEnabled')) 'kept version holds the scripts'

    # Change the group while the server is down; only the load-time merge can apply it.
    $groupFile = Join-Path $linkedDir 'm132on.json'
    $group = [IO.File]::ReadAllText($groupFile)
    Assert ($group -match '"RwCategories":\s*"stats"') 'group file holds the categories'
    Assert ([regex]::Matches($group, '"G_name2"').Count -eq 1) 'saved group holds the name once'
    Assert ([regex]::Matches($group, '"MaxHealth":\s*88').Count -eq 1) 'saved group holds the health once'
    $group = $group.Replace('"G_name2"', '"G_file"')
    $group = [regex]::Replace($group, '"MaxHealth":\s*88', '"MaxHealth": 99')
    [IO.File]::WriteAllText($groupFile, $group, (New-Object Text.UTF8Encoding $false))

    Start-Phase 'p4-reload' 1 1
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'reload forced chunks (Q308)'
    Send 'noppes clone spawn LINK2_CtrlReload 1 20.5,4,-14.5'
    $reloadChecks = Expect-Done 'reload'
    Stop-Phase 'p4-reload'

    Start-Phase 'p5-budget' 1 1
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'budget forced chunks (Q308)'
    Send 'noppes clone spawn LINK2_CtrlBudget 1 20.5,4,-16.5'
    $budgetChecks = Expect-Done 'budget' 300
    $budgetData = [regex]::Match((Log), 'LINKED2 DATA budget [^\r\n]*').Value
    Assert ($budgetData.Length -gt 0) 'budget data logged'
    Write-Host $budgetData
    Stop-Phase 'p5-budget'
    Unchanged
    @{success=$true; package='M13.2'; checks=@($checks); scriptChecks=@{off=$offChecks; on=$onChecks; reload=$reloadChecks; budget=$budgetChecks};
      budget=$budgetData;
      logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      control=(Get-FileHash -LiteralPath (Join-Path $seed 'linked-groups-control.js')).Hash;
      condition='dev dedicated, flat, spawn chunks plus harness forced chunks (Q308); three standing M36_Stand NPCs (two with paths, one without), then 200 members spawned in one tick; linked and saved through LinkedNpcController.loadNpcData/saveNpcData (the LinkedSet/NpcMenuClose paths), /rwlinked from a script at command block level; RwLinkedGroups 0 then 1, RwLinkedOwnFields 0 then 1 (set by the script at age 420), group file edited offline before a world reload; no player, no GUI'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M132 evidence: $prefix.json"
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
