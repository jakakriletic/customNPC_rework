# Q459 controller-only scenario: removing a Linked NPC group deletes its file with RwLinkedGroups=1 (original bug Q393).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/q459-remove-$stamp"
$work = Join-Path $root "dev/run-q459/$stamp"
$world = 'q459-world'
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
function Set-Config([int]$Groups) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwLinkedGroups)=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwLinkedGroups=$Groups`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Groups) {
    Set-Config $Groups
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
function Write-Controller([string]$Mode, [string]$Name, [string]$Clones) {
    $code = Join-Path $work "linked-remove-control-$Mode.js"
    [IO.File]::WriteAllText($code, "var MODE = '$Mode';`n" + [IO.File]::ReadAllText((Join-Path $seed 'linked-remove-control.js')), (New-Object Text.UTF8Encoding $false))
    $dest = Join-Path $Clones "$Name.json"
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') $code $dest
    $text = [IO.File]::ReadAllText($dest)
    $pattern = '(?m)^    "Name": "PERF_Kontrola"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $dest" }
    [IO.File]::WriteAllText($dest, [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"'), (New-Object Text.UTF8Encoding $false))
}
function Run-Controller([string]$Mode, [string]$Z) {
    Send "noppes clone spawn LINKRM_$Mode 1 20.5,4,$Z"
    # Without a player the server stops entity updates after 300 ticks; forced chunks keep them going (Q308).
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on' "$Mode forced chunks (Q308)"
    Expect "LINKEDRM DONE mode=$Mode " "$Mode controller finished" 180
    $text = Log
    $m = [regex]::Match($text, "LINKEDRM DONE mode=$Mode checks=(\d+) failed=(\d+)")
    Assert ($m.Success -and [int]$m.Groups[2].Value -eq 0 -and [int]$m.Groups[1].Value -gt 0) "$Mode all script checks pass ($($m.Groups[1].Value))"
    Assert (-not ($text -match 'LINKEDRM CHECK [^\r\n]*ok=0')) "$Mode no failed check"
    return [int]$m.Groups[1].Value
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'q459-remove'
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
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^RwLinkedGroups=(?!0\r?$)')) 'config copy has RwLinkedGroups at its default'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($mode in @('seed', 'offafter', 'on', 'onafter')) { Write-Controller $mode "LINKRM_$mode" $clones }
    $linkedDir = Join-Path $work "$world/customnpcs/linkednpcs"
    $results = @{}

    # Switch 0: reproduce the original bug.
    Start-Phase 'p1-seed' 0
    Send 'gamerule doMobSpawning false'
    Send 'setworldspawn 20 4 0'
    Expect 'Set the world spawn point to (20, 4, 0)' 'spawn chunks cover the controller'
    $results.seed = Run-Controller 'seed' '-10.5'
    Stop-Phase 'p1-seed'
    Assert (Test-Path -LiteralPath (Join-Path $linkedDir 'q459off.json')) 'switch 0: removed group file still on disk (Q393 reproduced)'
    Assert (Test-Path -LiteralPath (Join-Path $linkedDir 'q459keep.json')) 'other group file on disk'
    # A kept version for the switch-1 removal; undo writes it in the same form.
    Copy-Item -LiteralPath (Join-Path $linkedDir 'q459on.json') -Destination (Join-Path $linkedDir 'q459on.json_prev')
    $keepHash = Fingerprint (Join-Path $linkedDir 'q459keep.json')

    Start-Phase 'p2-offafter' 0
    $results.offafter = Run-Controller 'offafter' '-12.5'
    Stop-Phase 'p2-offafter'
    Assert (Test-Path -LiteralPath (Join-Path $linkedDir 'q459off.json')) 'switch 0: removed group came back after restart'

    # Switch 1: the fix.
    Start-Phase 'p3-on' 1
    $results.on = Run-Controller 'on' '-14.5'
    Stop-Phase 'p3-on'
    Assert (-not (Test-Path -LiteralPath (Join-Path $linkedDir 'q459on.json'))) 'switch 1: group file deleted'
    Assert (-not (Test-Path -LiteralPath (Join-Path $linkedDir 'q459on.json_prev'))) 'switch 1: kept version deleted'
    Assert ((Fingerprint (Join-Path $linkedDir 'q459keep.json')) -ceq $keepHash) 'other group file bitwise unchanged'

    Start-Phase 'p4-onafter' 1
    $results.onafter = Run-Controller 'onafter' '-16.5'
    Stop-Phase 'p4-onafter'
    Assert (-not (Test-Path -LiteralPath (Join-Path $linkedDir 'q459on.json'))) 'switch 1: group file still absent after restart'
    Unchanged
    @{success=$true; package='Q459'; checks=@($checks); scriptChecks=$results;
      files=@(Get-ChildItem -LiteralPath $linkedDir | Select-Object -ExpandProperty Name);
      logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      control=(Get-FileHash -LiteralPath (Join-Path $seed 'linked-remove-control.js')).Hash;
      condition='dev dedicated, flat, four starts of one world, no player; groups added and removed through LinkedNpcController.addData/removeData (the LinkedRemove packet path) from a controller NPC script; RwLinkedGroups 0, 0, 1, 1'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "Q459 evidence: $prefix.json"
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
