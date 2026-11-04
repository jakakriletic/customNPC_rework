# M13.2b controller scenario; no workload is launched by the coding agent.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m132b-gui-$stamp"
$work = Join-Path $root "dev/run-m132b/$stamp"
$world = 'm132b-world'
$player = 'M132B_Player'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null; $clientLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|RWLINKED client not installed'

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
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwLinkedGroups=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwLinkedGroups=$Value`n", (New-Object Text.UTF8Encoding $false))
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
    [IO.File]::WriteAllLines($job, @('linkedGui=1', "phase=$Phase", "connect=$port", ('out=' + $proofPath.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
}
function Wait-Sync([string]$Marker) {
    $until = (Get-Date).AddSeconds(900)
    while ($true) {
        if ((Read-JavaPrototypeLog $clientLog).Contains("LINKEDGUI SYNC $Marker")) { return }
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
    Assert ($text.Contains('LINKEDGUI DONE success=true')) "$Phase client success marker"
    Assert (-not ($text -cmatch $bad)) "$Phase no linkage or ticking errors on the client"
    $proof = [IO.File]::ReadAllText($proofPath) | ConvertFrom-Json
    Assert ($proof.success -eq $true) "$Phase proof success"
    $rows = @($proof.checks)
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) "$Phase all client checks passed"
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($Keys | Sort-Object) -join '|')) "$Phase exact check key set ($($Keys.Count))"
    foreach ($row in $rows) { [void]$checks.Add("$Phase $($row.key)") }
    return $proof
}
function Write-Fixture([string]$Destination, [string]$Name) {
    $text = [IO.File]::ReadAllText((Join-Path $seed 'customnpcs/clones/1/M36_Stand.json'))
    $pattern = '(?m)^    "Name": "M36_Stand"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw 'Expected one root name in the seed' }
    $text = [regex]::Replace($text, $pattern, ('    "Name": "' + $Name + '"'))
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm132b-gui'
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
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('LGUI_A', 'LGUI_B', 'LGUI_C')) { Write-Fixture (Join-Path $clones "$name.json") $name }
    $linked = Join-Path $work "$world/customnpcs/linkednpcs"
    $lg1 = Join-Path $linked 'LG1.json'; $lg2 = Join-Path $linked 'LG2.json'; $prev = Join-Path $linked 'LG1.json_prev'

    Start-Phase 'p1-main' 1
    Assert ((Log).Contains('RWLINKED channel rwlinked registered')) 'channel registered with RwLinkedGroups=1'
    Start-Client 'main'
    Wait-Sync 'deop'
    $beforeDeny = @((Fingerprint $lg1), (Fingerprint $prev))
    Assert ($beforeDeny[0] -cne 'absent' -and $beforeDeny[1] -cne 'absent') 'group file and kept version saved by the GUI'
    $undoLines = [regex]::Matches((Log), 'RWLINKED gui player=\S+ undo group=LG1 done=1').Count
    Assert ($undoLines -eq 2) 'two undos through the GUI reached the server'
    Assert ([regex]::Matches((Log), 'RWLINKED gui player=\S+ categories group=LG1').Count -eq 3) 'three category changes through the GUI reached the server'
    Send "deop $player"
    Expect "De-opped $player" 'real player deopped'
    Wait-Sync 'denied'
    Assert ((Fingerprint $lg1) -ceq $beforeDeny[0]) 'denied requests left the group file unchanged'
    Assert ((Fingerprint $prev) -ceq $beforeDeny[1]) 'denied requests left the kept version unchanged'
    Assert ([regex]::Matches((Log), 'RWLINKED gui player=\S+ undo').Count -eq 2) 'no further undo was logged'
    Assert (-not (Log).Contains('RWLINKED undo group=')) 'level 2 command refused to the non-operator player'
    $keys = @('panel-probe', 'group-defaults', 'scripts-off-by-default', 'original-link-transfers', 'edit-reaches-member',
        'undo-enabled-after-edit', 'loaded-count', 'box-needs-apply', 'apply-saved', 'other-group-own-categories',
        'name-off-blocks-name', 'server-keeps-selection', 'undo-button', 'undo-reaches-members', 'name-on-restores-name',
        'second-undo-brings-back', 'final-selection', 'forged-set-denied', 'forged-undo-denied',
        'controls-disabled-without-rights', 'denied-shows-no-previous-group', 'connection-alive')
    $main = Finish-Client 'main' $keys
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p1-main'
    $group = [IO.File]::ReadAllText($lg1)
    Assert ($group -match '"RwCategories":\s*"appearance,stats,ai,inventory,advanced,scripts,transform"') 'file holds the selection made in the GUI'
    Assert (-not ([IO.File]::ReadAllText($lg2) -match 'RwCategories')) 'the other group stays at the default'
    $fileHash = Fingerprint $lg1; $prevHash = Fingerprint $prev

    [IO.File]::WriteAllText($opsPath, $opsText, (New-Object Text.UTF8Encoding $false))
    Start-Phase 'p2-restart' 1
    Start-Client 'verify'
    $verify = Finish-Client 'verify' @('restart-members', 'restart-selection', 'restart-undo', 'restart-other-group')
    Stop-Phase 'p2-restart'
    Assert ((Fingerprint $lg1) -ceq $fileHash) 'looking at the GUI after the restart changed nothing'

    Start-Phase 'p3-off' 0
    Assert (-not (Log).Contains('RWLINKED channel')) 'no channel with RwLinkedGroups=0'
    Start-Client 'off'
    $off = Finish-Client 'off' @('off-original-buttons', 'off-no-labels', 'off-no-reply')
    Stop-Phase 'p3-off'
    Assert (-not (Log).Contains('RWLINKED ')) 'no /rwlinked and no RWLINKED line at 0'
    Assert ((Fingerprint $lg1) -ceq $fileHash) 'group file bitwise unchanged at 0'
    Assert ((Fingerprint $prev) -ceq $prevHash) 'kept version bitwise unchanged at 0'

    Start-Phase 'p4-mixed' 0
    Assert (-not (Log).Contains('RWLINKED channel')) 'server without the channel'
    Set-Switch 1
    Start-Client 'mixed'
    $mixed = Finish-Client 'mixed' @('mixed-original-buttons', 'mixed-no-labels', 'mixed-no-reply')
    Assert ((Read-JavaPrototypeLog $clientLog).Contains('RWLINKED channel rwlinked registered')) 'client side had the channel'
    Stop-Phase 'p4-mixed'
    Assert ((Fingerprint $lg1) -ceq $fileHash) 'group file bitwise unchanged with a client at 1 and a server at 0'

    $all = @($main.screenshots) + @($verify.screenshots) + @($off.screenshots) + @($mixed.screenshots)
    foreach ($image in $all) {
        Assert ([IO.Path]::GetFileName($image) -ceq $image) 'local screenshot filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    Assert ($all.Count -eq 7) 'seven required screenshots'
    Unchanged
    @{success=$true; package='M13.2b'; checks=@($checks); inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      main=$main; verify=$verify; off=$off; mixed=$mixed;
      condition='one real dev client and dev dedicated server; four starts (edit, restart, both sides 0, client 1 with server 0); isolated flat world/config copy; regular mouse and key handlers; console takes the rights away; no packaged/integrated pair; no performance measurement'} |
      ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M132B evidence: $prefix.json ($($checks.Count) checks)"
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
