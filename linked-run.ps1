# M13.1 controller-only scenario: two linked NPCs at different places, original merge at 0, own fields at 1, world reload.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m131-linked-$stamp"
$work = Join-Path $root "dev/run-m131/$stamp"
$world = 'm131-world'
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
function Set-Config([int]$Linked) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwArmies|RwLinkedOwnFields)=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwArmies=1`nRwLinkedOwnFields=$Linked`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Linked) {
    Set-Config $Linked
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
    if ($text.Contains('"LinkedNpcName": "m131')) { throw 'Seed already linked.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Write-Controller([string]$Mode, [string]$Name, [string]$Clones) {
    $code = Join-Path $work "linked-control-$Mode.js"
    [IO.File]::WriteAllText($code, "var MODE = '$Mode';`n" + [IO.File]::ReadAllText((Join-Path $seed 'linked-control.js')), (New-Object Text.UTF8Encoding $false))
    $dest = Join-Path $Clones "$Name.json"
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') $code $dest
    Write-Fixture $dest $dest $Name 'PERF_Kontrola'
}
function Expect-Done([string]$Mode, [int]$Seconds = 180) {
    Expect "LINKED DONE mode=$Mode " "$Mode controller finished" $Seconds
    $text = Log
    $m = [regex]::Match($text, "LINKED DONE mode=$Mode checks=(\d+) failed=(\d+)")
    Assert ($m.Success -and [int]$m.Groups[2].Value -eq 0 -and [int]$m.Groups[1].Value -gt 0) "$Mode all script checks pass ($($m.Groups[1].Value))"
    Assert (-not ($text -match 'LINKED CHECK [^\r\n]*ok=0')) "$Mode no failed check"
    return [int]$m.Groups[1].Value
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm131-linked'
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
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^RwLinkedOwnFields=(?!0\r?$)')) 'config copy has the linked switch at its default'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('M131A_off', 'M131B_off', 'M131A_on', 'M131B_on')) {
        Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones "$name.json") $name 'M36_Stand'
    }
    Write-Controller 'off' 'LINK_CtrlOff' $clones
    Write-Controller 'on' 'LINK_CtrlOn' $clones
    Write-Controller 'reload' 'LINK_CtrlReload' $clones
    $linkedDir = Join-Path $work "$world/customnpcs/linkednpcs"

    Start-Phase 'p1-bootstrap' 0
    Send 'setworldspawn 20 4 0'
    Expect 'Set the world spawn point to (20, 4, 0)' 'spawn chunks cover both linked NPCs'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    Start-Phase 'p2-off' 0
    Send 'gamerule doMobSpawning false'
    Send 'gamerule doDaylightCycle false'
    Send 'noppes clone spawn M131A_off 1 0.5,4,0.5'
    Send 'noppes clone spawn M131B_off 1 40.5,4,0.5'
    # Without a player the server stops entity updates after 300 ticks; forced chunks keep them going (Q308).
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'off forced chunks (Q308)'
    Send 'noppes clone spawn LINK_CtrlOff 1 20.5,4,-10.5'
    $offChecks = Expect-Done 'off'
    $p2 = Log
    # Q384/Q386: at 0 the member takes the edited NPC's orientation and path index; its home goes to its own first path point.
    Assert ($p2.Contains('key=off-B-orientation-overwritten ok=1') -and $p2.Contains('key=off-B-pathpos-overwritten ok=1') -and $p2.Contains('key=off-B-home-not-A ok=1')) 'original merge reproduced: member takes the edited NPC orientation and path index'
    Assert ($p2.Contains('key=B-army-kept ok=1') -and $p2.Contains('key=B-navbackend-kept ok=1')) 'RwArmy and RwNavBackend are not group data at 0'
    $offMove = [regex]::Match($p2, 'LINKED DATA moved mode=off [^\r\n]*').Value
    Stop-Phase 'p2-off'
    Assert (Test-Path -LiteralPath (Join-Path $linkedDir 'm131off.json')) 'off group saved'

    Start-Phase 'p3-on' 1
    Send 'noppes clone spawn M131A_on 1 0.5,4,10.5'
    Send 'noppes clone spawn M131B_on 1 40.5,4,10.5'
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'on forced chunks (Q308)'
    Send 'noppes clone spawn LINK_CtrlOn 1 20.5,4,-12.5'
    $onChecks = Expect-Done 'on'
    $p3 = Log
    Assert ($p3.Contains('key=on-B-home-own ok=1') -and $p3.Contains('key=on-B-pathpos-own ok=1')) 'member keeps its home and path index at 1'
    Assert ($p3.Contains('key=shared-name-B ok=1') -and $p3.Contains('key=shared-health-B ok=1')) 'shared data still reaches the other loaded member at 1'
    $onMove = [regex]::Match($p3, 'LINKED DATA moved mode=on [^\r\n]*').Value
    Send 'save-all flush'
    Expect 'Saved the world' 'on phase saved' 120
    Stop-Phase 'p3-on'

    # Rename the group while the server is down; only the load-time merge (readEntityFromNBT) can apply it.
    $groupFile = Join-Path $linkedDir 'm131on.json'
    $group = [IO.File]::ReadAllText($groupFile)
    Assert ([regex]::Matches($group, '"M131Shared_on"').Count -eq 1) 'saved group holds the shared name once'
    [IO.File]::WriteAllText($groupFile, $group.Replace('"M131Shared_on"', '"M131File_on"'), (New-Object Text.UTF8Encoding $false))

    Start-Phase 'p4-reload' 1
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' 'reload forced chunks (Q308)'
    Send 'noppes clone spawn LINK_CtrlReload 1 20.5,4,-14.5'
    $reloadChecks = Expect-Done 'reload'
    $p4 = Log
    Assert ($p4.Contains('key=reload-group-name-applied-B ok=1')) 'load-time merge applied the edited group file'
    Assert ($p4.Contains('key=reload-B-home-own ok=1') -and $p4.Contains('key=reload-B-army-kept ok=1')) 'own fields and entity tags kept after world reload'
    Stop-Phase 'p4-reload'
    Unchanged
    @{success=$true; package='M13.1'; checks=@($checks); scriptChecks=@{off=$offChecks; on=$onChecks; reload=$reloadChecks};
      movement=@{off=$offMove; on=$onMove};
      logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      control=(Get-FileHash -LiteralPath (Join-Path $seed 'linked-control.js')).Hash;
      condition='dev dedicated, flat, spawn chunks plus harness forced chunks (Q308); two standing M36_Stand NPCs 40 blocks apart per phase, linked through LinkedNpcController.addData/loadNpcData and saved with saveNpcData (the LinkedSet/NpcMenuClose paths), RwArmies=1 for one RwArmy tag; RwLinkedOwnFields 0 then 1; group file edited offline before a world reload; no player, no GUI'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M131 evidence: $prefix.json"
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
