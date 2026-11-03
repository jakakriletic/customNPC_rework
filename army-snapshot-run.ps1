# M11.2 controller scenario: the army snapshot on the rwarmy channel, real dev client against a dev dedicated server
# (runServer) in an isolated run directory. Three starts of one world:
#   1 RwArmies=1  client: scene, clusters, limits, a real player's rights changed from this console; save
#   2 RwArmies=1  no client: the last known position survives the restart, nothing was served
#   3 RwArmies=0  client with RwArmies=1: no answer, no command, connection stays; register file not touched
# No performance measurement.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m112-snapshot-$stamp"
$work = Join-Path $root "dev/run-m112/$stamp"
$world = 'm112-world'
$player = 'M112_Player'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null; $clientLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|RWARMY client not installed'

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
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwArmies=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwArmies=$Value`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Switch) {
    Set-Switch $Switch
    $script:serverLog = "$prefix-$Name-server.log"
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
    Assert (-not ((Log) -cmatch $bad)) "$Name no linkage or ticking errors on the server"
    $script:server = $null
}
function Start-Client([string]$Phase) {
    $script:proofPath = "$prefix-$Phase.proof.json"
    $job = "$prefix-$Phase.job"
    [IO.File]::WriteAllLines($job, @('armySnapshot=1', "phase=$Phase", "connect=$port", ('out=' + $proofPath.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
}
# Waits for the client's next ARMYSNAP SYNC line, then runs the console command that changes its rights.
function Sync([string]$Marker, [string]$Command, [string]$Reply) {
    $until = (Get-Date).AddSeconds(600)
    while ($true) {
        if ((Read-JavaPrototypeLog $clientLog).Contains("ARMYSNAP SYNC $Marker")) { break }
        if ($client.HasExited) { throw "Client exited before sync $Marker; $clientLog" }
        if ($server.HasExited) { throw "Server exited before sync $Marker; $serverLog" }
        if ((Get-Date) -gt $until) { throw "Timeout before sync $Marker; $clientLog" }
        Guard; Start-Sleep -Milliseconds 500
    }
    Send $Command
    Expect $Reply "console: $Command"
}
function Finish-Client([string]$Phase, [string[]]$Keys) {
    $until = (Get-Date).AddSeconds(600)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $client.HasExited "$Phase client completed"
    [void][M61ProcessLog]::Finish($client)
    Assert ($client.ExitCode -eq 0) "$Phase client clean exit"
    $script:client = $null
    $text = Read-JavaPrototypeLog $clientLog
    Assert ($text.Contains('ARMYSNAP DONE success=true')) "$Phase client success marker"
    Assert (-not ($text -cmatch $bad)) "$Phase no linkage or ticking errors on the client"
    $proof = [IO.File]::ReadAllText($proofPath) | ConvertFrom-Json
    Assert ($proof.success -eq $true) "$Phase proof success"
    $rows = @($proof.checks)
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) "$Phase all client checks passed"
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($Keys | Sort-Object) -join '|')) "$Phase exact check key set ($($Keys.Count))"
    foreach ($row in $rows) { [void]$checks.Add("$Phase $($row.key)") }
    return $proof
}
function Write-Fixture([string]$Destination, [string]$Name, [string]$Army) {
    $text = [IO.File]::ReadAllText((Join-Path $seed 'customnpcs/clones/1/M36_Stand.json'))
    if ($text.Contains('"RwArmy"')) { throw 'Seed already has an army.' }
    $pattern = '(?m)^    "Name": "M36_Stand"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw 'Expected one root name in the seed' }
    $line = '    "Name": "' + $Name + '"'
    if ($Army) { $line = '    "RwArmy": "' + $Army + '",' + "`n" + $line }
    $text = [regex]::Replace($text, $pattern, $line)
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm112-snapshot'
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
    Assert (-not ([IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg')) -match '(?m)^RwArmies=(?!0\r?$)')) 'config copy has armies off'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    Assert ($props -match '(?m)^online-mode=false\s*$') 'existing local offline test mode'
    Assert ($props -match '(?m)^view-distance=10\s*$') 'view distance 10: the whole scene around the player is loaded'
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
    [IO.File]::WriteAllText((Join-Path $work 'ops.json'), ('[{"uuid":"' + $uuid + '","name":"' + $player + '","level":4,"bypassesPlayerLimit":false}]'), (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('ARMY_N', 'ARMY_S', 'ARMY_H')) { Write-Fixture (Join-Path $clones "$name.json") $name '' }
    Write-Fixture (Join-Path $clones 'ARMY_G.json') 'ARMY_G' 'ghost'
    $dat = Join-Path $work "$world/data/rwarmies.dat"

    # 1: real client, real rights
    Start-Phase 'p1-client' 1
    Assert ((Log).Contains('RWARMY channel rwarmy registered')) 'channel registered with RwArmies=1'
    Start-Client 'main'
    Sync 'deop' "deop $player" "De-opped $player"
    Sync 'grant-view' "rwarmy grant $player view north" 'RWARMY granted view north'
    Sync 'grant-command' "rwarmy grant $player command south" 'RWARMY granted command south'
    Sync 'revoke' "rwarmy revoke $player all south" 'RWARMY revoked all south'
    Sync 'away' "tp $player 5000 5 5000" "Teleported $player"
    $keys = @('orphan-name-on-npc', 'assigned-north', 'assigned-south', 'assigned-hidden', 'op-all-visible', 'op-command-everywhere', 'config-defaults',
        'north-two-clusters', 'north-main', 'north-straggler', 'north-last-is-main', 'south-below-threshold-is-main', 'empty-army', 'orphan-ignored',
        'request-answered-by-id', 'world-time', 'rate-limit-one-answer', 'rate-limit-first-served', 'malformed-rejected', 'over-limit-counted',
        'served-matches-client', 'client-rejects-nothing', 'command-shows-clusters', 'non-op-sees-own-only', 'owner-has-command', 'view-grant-visible',
        'view-is-not-command', 'view-sees-clusters', 'command-grant-visible', 'hidden-stays-hidden', 'revoke-applies-to-next', 'non-op-command-counts-visible',
        'unloaded-stays-listed', 'unloaded-keeps-last', 'last-seen-time', 'client-still-connected')
    $proof = Finish-Client 'main' $keys
    $last = [string]$proof.data.last
    Assert ($last -match '^0,\d+,\d+,\d+$') "last known position from the client: $last"
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p1-client'
    Assert (Test-Path -LiteralPath $dat) 'register file written'

    # 2: restart, no client
    Start-Phase 'p2-restart' 1
    Send 'rwarmy clusters north'
    Expect 'RWARMY clusters armies=4 radius=48 min=8 served=0 limited=0 rejected=0' 'after restart: nothing served without a request'
    Expect "RWARMY clusterarmy north access=command loaded=0 clusters=0 omitted=0 last=$last seen=" 'last known position survived the restart'
    Assert ((Log) -match 'RWARMY register loaded armies=4 skipped=0') 'register loaded without skipped entries'
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved after restart' 180
    Stop-Phase 'p2-restart'
    $datHash = Fingerprint $dat

    # 3: server without the switch, client with it
    Start-Phase 'p3-off' 0
    Assert (-not (Log).Contains('RWARMY channel')) 'no channel with RwArmies=0'
    Set-Switch 1
    Start-Client 'off'
    [void](Finish-Client 'off' @('off-no-answer', 'off-still-connected', 'off-no-command'))
    Assert ((Read-JavaPrototypeLog $clientLog).Contains('RWARMY channel rwarmy registered')) 'the client had the channel'
    Set-Switch 0
    Stop-Phase 'p3-off'
    Assert (-not (Log).Contains('RWARMY register loaded')) 'register not loaded with RwArmies=0'
    Assert ((Fingerprint $dat) -ceq $datHash) 'register file untouched with RwArmies=0'

    Unchanged
    @{success = $true; package = 'M11.2'; checks = @($checks); logs = @(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name); last = $last;
      inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; register = $datHash;
      condition = 'dev client (MCP runClient) against a dev dedicated server (runServer), isolated run directory with a copy of dev/run/config, RwArmies set in the copy; three starts of one flat world; one real player, operator at first, then rights changed from the server console; no packaged pair, no second player, no integrated server'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M112 evidence: $prefix.json ($($checks.Count) checks)"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client) { try { Stop-JavaPrototypeProcess $client } catch { Write-Host "Cleanup failure: $_"; $exitCode = 1 } }
    if ($null -ne $server) { try { if (-not $server.HasExited) { Send 'stop'; if (-not $server.WaitForExit(60000)) { Stop-JavaPrototypeProcess $server } }; Stop-JavaPrototypeProcess $server } catch { Write-Host "Cleanup failure: $_"; $exitCode = 1 } }
    foreach ($id in @($owned.Keys)) { if (Get-Process -Id $id -ErrorAction SilentlyContinue) { & taskkill.exe /PID $id /T /F 2>&1 | Out-Null; $exitCode = 1 } }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
