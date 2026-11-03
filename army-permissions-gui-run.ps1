# M11.8b controller scenario; no workload is launched by the coding agent.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m118b-gui-$stamp"
$work = Join-Path $root "dev/run-m118b/$stamp"
$world = 'm118b-world'
$player = 'M118B_Player'
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
    [IO.File]::WriteAllLines($job, @('armyPerm=1', "phase=$Phase", "connect=$port", ('out=' + $proofPath.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
}
# Waits for the client's next ARMYPERM SYNC line, then runs the console command that changes its rights.
function Sync([string]$Marker, [string]$Command, [string]$Reply) {
    $until = (Get-Date).AddSeconds(600)
    while ($true) {
        if ((Read-JavaPrototypeLog $clientLog).Contains("ARMYPERM SYNC $Marker")) { break }
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
    Assert ($text.Contains('ARMYPERM DONE success=true')) "$Phase client success marker"
    Assert (-not ($text -cmatch $bad)) "$Phase no linkage or ticking errors on the client"
    $proof = [IO.File]::ReadAllText($proofPath) | ConvertFrom-Json
    Assert ($proof.success -eq $true) "$Phase proof success"
    $rows = @($proof.checks)
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) "$Phase all client checks passed"
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($Keys | Sort-Object) -join '|')) "$Phase exact check key set ($($Keys.Count))"
    foreach ($row in $rows) { [void]$checks.Add("$Phase $($row.key)") }
    return $proof
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm118b-gui'
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
    # Deterministic offline profiles: one real client and five previously seen local profiles.
    function Offline-UUID([string]$Name) {
        $hasher = [Security.Cryptography.MD5]::Create()
        try { $bytes = $hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Name")) } finally { $hasher.Dispose() }
        $bytes[6] = ($bytes[6] -band 0x0f) -bor 0x30; $bytes[8] = ($bytes[8] -band 0x3f) -bor 0x80
        $hex = ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
        return $hex.Substring(0,8)+'-'+$hex.Substring(8,4)+'-'+$hex.Substring(12,4)+'-'+$hex.Substring(16,4)+'-'+$hex.Substring(20,12)
    }
    $uuid = Offline-UUID $player
    [IO.File]::WriteAllText((Join-Path $work 'ops.json'), ('[{"uuid":"'+$uuid+'","name":"'+$player+'","level":4,"bypassesPlayerLimit":false}]'), (New-Object Text.UTF8Encoding $false))
    $profiles = @('M118B_Ally','M118B_Z1','M118B_Z2','M118B_Z3','M118B_Z4') | ForEach-Object {
        @{name=$_; uuid=(Offline-UUID $_); expiresOn='2100-01-01 00:00:00 +0000'}
    }
    [IO.File]::WriteAllText((Join-Path $work 'usercache.json'), (ConvertTo-Json -InputObject @($profiles)), (New-Object Text.UTF8Encoding $false))
    $dat = Join-Path $work "$world/data/rwarmies.dat"
    $opsText = [IO.File]::ReadAllText((Join-Path $work 'ops.json'))
    Start-Phase 'p1-main' 1
    Assert ((Log).Contains('RWARMY channel rwarmy registered')) 'channel registered with RwArmies=1'
    Start-Client 'main'
    Sync 'deop' "deop $player" "De-opped $player"
    Sync 'remove-owner' 'rwarmy remove alpha' 'RWARMY removed alpha'
    $keys = @('tab-players','tab-players-list','tab-layout','needs-player','players-page','find-player','find-result','all-players',
      'select-ally','view-selected','command-all','command-none','view-none','view-all','command-selected','revoke-army','unknown-army',
      'checkbox-grants','checkbox-revoke','select-self','self-refused','forged-no-staff','reopen-players','reopen-players-list',
      'reselect-ally','deop-own-only','deop-foreign-hidden','batch-stop-on-denial','clear-stale-selection','owner-command-selected','owner-command-all','owner-command-none',
      'owner-view-all','non-owner-no-delegation','connection-alive')
    $main = Finish-Client 'main' $keys
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p1-main'
    Assert (Test-Path -LiteralPath $dat) 'register saved'
    [IO.File]::WriteAllText((Join-Path $work 'ops.json'), $opsText, (New-Object Text.UTF8Encoding $false))
    Start-Phase 'p2-restart' 1
    Start-Client 'verify'
    $verify = Finish-Client 'verify' @('restart-players','restart-players-list','restart-grants')
    Stop-Phase 'p2-restart'
    $datHash = Fingerprint $dat
    Start-Phase 'p3-off' 0
    Assert (-not (Log).Contains('RWARMY channel')) 'no channel with RwArmies=0'
    Start-Client 'off'
    [void](Finish-Client 'off' @('off-no-gui','off-no-reply','off-item'))
    Stop-Phase 'p3-off'
    Assert (-not (Log).Contains('RWARMY register loaded')) 'register not loaded at 0'
    Assert ((Fingerprint $dat) -ceq $datHash) 'register bitwise unchanged at 0'
    foreach ($proof in @($main,$verify)) {
        foreach ($image in @($proof.screenshots)) {
            Assert ([IO.Path]::GetFileName($image) -ceq $image) 'local screenshot filename'
            $path = Join-Path (Join-Path $root 'audit/screenshots') $image
            Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
        }
    }
    Assert (@($main.screenshots).Count -eq 4 -and @($verify.screenshots).Count -eq 1) 'five required screenshots'
    Unchanged
    @{success=$true; package='M11.8b'; checks=@($checks); inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      register=$datHash; main=$main; verify=$verify;
      condition='one real dev client and dev dedicated server; cached offline grantee; three starts (permissions, restart, switch 0); isolated world/config copy; real mouse/key handlers and channel; console deop/remove checks live authority; no packaged/integrated pair; no performance measurement'} |
      ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M118B evidence: $prefix.json ($($checks.Count) checks)"
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
