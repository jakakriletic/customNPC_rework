# M13.4 controller scenario; no workload is launched by the coding agent.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m134-cloner-$stamp"
$work = Join-Path $root "dev/run-m134/$stamp"
$world = 'm134-world'
$player = 'M134_Player'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null; $clientLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|Error executing task|script errored|ScriptException'
$T1 = 'customnpcs:textures/entity/humanfemale/magestephanie.png'
$T2 = 'customnpcs:textures/entity/humanfemale/doctorstephanie.png'
$STEVE = 'customnpcs:textures/entity/humanmale/steve.png'

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
    $roots = @(); if ($null -ne $server) { $roots += $server.Id }; if ($null -ne $client) { $roots += $client.Id }
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
function Expect([string]$Marker, [string]$Text, [int]$Seconds = 60) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $log = Log
        if ($log.Contains($Marker)) { Assert $true $Text; return }
        if ($server.HasExited -or $log -cmatch $bad) { throw "Server failed before '$Marker'; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw "Timeout waiting for '$Marker'; $serverLog"
}
function Set-Switch([int]$Value) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwLinkedGroups|RwLinkedOwnFields)=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwLinkedGroups=$Value`nRwLinkedOwnFields=$Value`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Switch) {
    Set-Switch $Switch
    $script:serverLog = "$prefix-$Name-server.log"
    $script:server = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runServer', '--offline', '--no-daemon', '--console=plain', '-Pm134Probe', "-PrwRunDir=$work", "-PrwWorld=$world") (Join-Path $root 'dev') $serverLog
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
    Assert (-not ((Log) -cmatch $bad)) "$Name no linkage, task or ticking errors on the server"
    $script:server = $null
}
function Start-Client([string]$Phase) {
    $script:proofPath = "$prefix-$Phase.proof.json"
    $job = "$prefix-$Phase.job"
    [IO.File]::WriteAllLines($job, @('linkedCloner=1', ('gate=' + $gatePath.Replace('\', '/')), "phase=$Phase", "connect=$port", ('out=' + $proofPath.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
}
function Wait-Sync([string]$Marker) {
    $until = (Get-Date).AddSeconds(900)
    while ($true) {
        if ((Read-JavaPrototypeLog $clientLog).Contains("LINKEDCLONER SYNC $Marker")) { return }
        if ($client.HasExited) { throw "Client exited before sync $Marker; $clientLog" }
        if ($server.HasExited) { throw "Server exited before sync $Marker; $serverLog" }
        if ((Get-Date) -gt $until) { throw "Timeout before sync $Marker; $clientLog" }
        Guard; Start-Sleep -Milliseconds 500
    }
}
function Finish-Client([string]$Phase, [string[]]$Keys) {
    $until = (Get-Date).AddSeconds(600)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $client.HasExited "$Phase client completed"
    [void][M61ProcessLog]::Finish($client)
    Assert ($client.ExitCode -eq 0) "$Phase client clean exit"
    $script:client = $null
    $text = Read-JavaPrototypeLog $clientLog
    Write-Host ("INFO $Phase join retries " + ([regex]::Matches($text, 'LINKEDCLONER JOIN retry')).Count)
    Assert ($text.Contains('LINKEDCLONER DONE success=true')) "$Phase client success marker"
    Assert (-not ($text -cmatch $bad)) "$Phase no linkage or ticking errors on the client"
    $proof = [IO.File]::ReadAllText($proofPath) | ConvertFrom-Json
    Assert ($proof.success -eq $true) "$Phase proof success"
    $rows = @($proof.checks)
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) "$Phase all client checks passed"
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($Keys | Sort-Object) -join '|')) "$Phase exact check key set ($($Keys.Count))"
    foreach ($row in $rows) { [void]$checks.Add("$Phase $($row.key)") }
    return $proof
}

function Gate([string]$Text) { [IO.File]::AppendAllText($gatePath, "$Text`n", (New-Object Text.UTF8Encoding $false)) }
function Write-Fixture([string]$Destination, [string]$Name, [string]$Texture) {
    $text = [IO.File]::ReadAllText((Join-Path $seed 'customnpcs/clones/1/M36_Stand.json'))
    $text = $text.Replace('"Name": "M36_Stand"', ('"Name": "' + $Name + '"')).Replace($STEVE, $Texture)
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Probe([string]$Marker, [string]$Gate) {
    Expect ("CLONERPROBE " + $Marker) ("probe " + $Marker) 180
    Assert (-not ((Log) -match 'CLONERPROBE CHECK [^\r\n]*ok=0')) 'no failed probe check'
    Gate $Gate
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm134-cloner'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'), (Join-Path $run 'server.properties'), (Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $seed 'customnpcs/clones/1') -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Force -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    $cfgText = [IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg'))
    Assert (-not ($cfgText -match '(?m)^RwLinkedGroups=(?!0\r?$)')) 'config copy has groups off'
    Assert (-not ($cfgText -match '(?m)^RwLinkedOwnFields=(?!0\r?$)')) 'config copy has own fields off'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    Assert ($props -match '(?m)^online-mode=false\s*$') 'existing local offline test mode'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    # Offline UUID = Java UUID.nameUUIDFromBytes("OfflinePlayer:"+name), no profile lookup.
    $md5 = [Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$player"))
    $md5[6] = ($md5[6] -band 0x0f) -bor 0x30; $md5[8] = ($md5[8] -band 0x3f) -bor 0x80
    $hex = ($md5 | ForEach-Object { $_.ToString('x2') }) -join ''
    $uuid = $hex.Substring(0, 8) + '-' + $hex.Substring(8, 4) + '-' + $hex.Substring(12, 4) + '-' + $hex.Substring(16, 4) + '-' + $hex.Substring(20, 12)
    $opsPath = Join-Path $work 'ops.json'
    $opsText = '[{"uuid":"' + $uuid + '","name":"' + $player + '","level":4,"bypassesPlayerLimit":false}]'
    [IO.File]::WriteAllText($opsPath, $opsText, (New-Object Text.UTF8Encoding $false))

    $gatePath = Join-Path $work 'gates.txt'
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    $localClones = Join-Path $work 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones,$localClones | Out-Null
    Write-Fixture (Join-Path $clones 'LCGuard.json') 'LCGuard' $STEVE
    Write-Fixture (Join-Path $clones 'LCPlain.json') 'LCPlain' $STEVE
    Write-Fixture (Join-Path $localClones 'LCGuard.json') 'LCGuard' $T2
    $probeClone = Join-Path $clones 'LCProbe.json'
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'linked-cloner-control.js') $probeClone
    $probeText = [IO.File]::ReadAllText($probeClone).Replace('"Name": "PERF_Kontrola"', '"Name": "LCProbe"')
    [IO.File]::WriteAllText($probeClone, $probeText, (New-Object Text.UTF8Encoding $false))
    $group = Join-Path $work "$world/customnpcs/linkednpcs/LCGuard.json"
    $previous = $group + '_prev'
    $forbidden = Join-Path $work "$world/customnpcs/linkednpcs/LCForbidden.json"
    Start-Phase 'p1-main' 1
    Send 'setworldspawn 0 4 0'
    Send 'gamerule doMobSpawning false'
    Send 'noppes clone spawn LCProbe 1 0.5,4,0.5'
    Start-Client 'main'
    Wait-Sync 'first'
    Expect 'CLONERPROBE evaluated' 'probe script evaluated with player connected' 30
    Expect 'CLONERPROBE ready' 'probe ticks with player connected' 30
    Assert (Test-Path -LiteralPath $group) 'new group file created from first clone'
    $initial = Fingerprint $group
    Gate 'first'
    Wait-Sync 'local'
    Assert ((Fingerprint $group) -ceq $initial) 'local linked placement did not overwrite the existing group'
    Assert (-not (Test-Path -LiteralPath $previous)) 'local placement did not use undo'
    Gate 'local'
    Wait-Sync 'far-before'
    $beforeFar = Fingerprint $group
    Gate 'far-before'
    Wait-Sync 'far'
    Expect 'CLONERPROBE snapshot x=1002 ' 'remote own-field snapshot recorded before leaving' 30
    Assert ((Fingerprint $group) -ceq $beforeFar) 'server linked placement did not overwrite existing group'
    Assert (-not (Test-Path -LiteralPath $previous)) 'server placement did not use undo'
    Gate 'far'
    Wait-Sync 'unload'
    Probe 'unloaded1' 'unloaded1'
    Wait-Sync 'edited-unloaded'
    Probe 'edited1' 'edited1'
    Expect ('RWLINKED update player=' + $player + ' Group LCGuard: updated 1 loaded NPCs; pending 0.') 'notification reports one actual loaded recipient'
    Wait-Sync 'loaded1'
    Probe 'loaded1' 'loaded1'
    Wait-Sync 'unload2'
    Probe 'unloaded2' 'unloaded2'
    Wait-Sync 'restart-ready'
    Probe 'restart-ready' 'restart-ready'
    Wait-Sync 'deop'
    $beforeDenied = Fingerprint $group
    $prevDenied = Fingerprint $previous
    Send "deop $player"
    Expect "De-opped $player" 'player deopped with cloner open'
    Gate 'deopped'
    Wait-Sync 'denied'
    Probe 'denied-done' 'denied-done'
    Assert ((Fingerprint $group) -ceq $beforeDenied -and (Fingerprint $previous) -ceq $prevDenied) 'deopped placements preserved group and undo bitwise'
    Assert (-not (Test-Path -LiteralPath $forbidden)) 'forged placement did not create a group'
    $lines = [regex]::Matches((Log), 'RWLINKED cloner player=' + $player + ' group=LCGuard created=(true|false)')
    Assert ((($lines | ForEach-Object { $_.Groups[1].Value }) -join '|') -ceq 'true|false|false') 'exact three linked placements (new, local existing, server existing)'
    $main = Finish-Client 'main' @('linked-controls-600','new-group-feedback','linked-controls-604','local-clone-existing-group',
        'plain-clone','linked-controls-1002','far-clone','loaded-categories','update-notification','unloaded-member-updated',
        'second-edit','stale-screen-denied','forged-spawn-denied')
    Send 'save-all flush'
    Expect 'Saved the world' 'world flushed before restart' 180
    Stop-Phase 'p1-main'
    Assert ((Fingerprint $group) -ceq $beforeDenied) 'stop did not rewrite group data'
    [IO.File]::WriteAllText($opsPath, $opsText, (New-Object Text.UTF8Encoding $false))
    Start-Phase 'p2-verify' 1
    Start-Client 'verify'
    Wait-Sync 'restart-loaded'
    Probe 'restart-done' 'restart-done'
    $verify = Finish-Client 'verify' @('restart-near','restart-unloaded-member')
    Stop-Phase 'p2-verify'
    Assert ((Fingerprint $group) -ceq $beforeDenied) 'restart/viewing did not rewrite group data'
    $offHash = Fingerprint $group
    $offPrev = Fingerprint $previous
    Start-Phase 'p3-off' 0
    Assert (-not (Log).Contains('RWLINKED channel')) 'channel absent at 0'
    Start-Client 'off'
    $off = Finish-Client 'off' @('off-original-gui','off-no-reply','off-original-spawn')
    Stop-Phase 'p3-off'
    Assert ((Fingerprint $group) -ceq $offHash -and (Fingerprint $previous) -ceq $offPrev) 'group and undo bitwise unchanged at 0'
    Start-Phase 'p4-mixed' 0
    Assert (-not (Log).Contains('RWLINKED channel')) 'mixed server channel absent'
    Set-Switch 1
    Start-Client 'mixed'
    $mixed = Finish-Client 'mixed' @('mixed-original-gui','mixed-no-reply','mixed-original-spawn')
    Stop-Phase 'p4-mixed'
    Assert ((Fingerprint $group) -ceq $offHash -and (Fingerprint $previous) -ceq $offPrev) 'mixed version preserved group and undo'
    $all = @($main.screenshots) + @($verify.screenshots) + @($off.screenshots) + @($mixed.screenshots)
    foreach ($img in $all) {
        Assert ([IO.Path]::GetFileName($img) -ceq $img) 'local screenshot filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $img
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $img"
    }
    Assert ($all.Count -eq 9) 'nine required screenshots'
    Unchanged
    @{success=$true; package='M13.4'; checks=@($checks); inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      main=$main; verify=$verify; off=$off; mixed=$mixed;
      condition='one real dev client/dedicated; four starts; local and server clones, actual provider-null remote chunk without forced chunks/tickets; probe in vanilla spawn chunks; own fields and UUID read server-side; GUI edits and network notifications; deop while cloner open; off/mixed original placement; transfer CPU once as data, no MSPT benchmark'} |
        ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M134 evidence: $prefix.json ($($checks.Count) checks)"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client) { try { Stop-JavaPrototypeProcess $client } catch { Write-Host "Cleanup failure: $_"; $exitCode=1 } }
    if ($null -ne $server) { try { if (-not $server.HasExited) { Send 'stop'; if (-not $server.WaitForExit(60000)) { Stop-JavaPrototypeProcess $server } }; Stop-JavaPrototypeProcess $server } catch { Write-Host "Cleanup failure: $_"; $exitCode=1 } }
    foreach ($id in @($owned.Keys)) { if (Get-Process -Id $id -ErrorAction SilentlyContinue) { & taskkill.exe /PID $id /T /F 2>&1 | Out-Null; $exitCode=1 } }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode=1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode


