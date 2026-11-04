# M13.5 controller scenario (exit proof of M13); no workload is launched by the coding agent.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m135-hundred-$stamp"
$work = Join-Path $root "dev/run-m135/$stamp"
$world = 'm135-world'
$player = 'M135_Player'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null; $clientLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object System.Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|Error executing task'
$T1 = 'customnpcs:textures/entity/humanfemale/magestephanie.png'
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
    Assert (-not ((Log) -cmatch $bad)) "$Name no linkage, task or ticking errors on the server"
    $script:server = $null
}
function Start-Client([string]$Phase) {
    $script:proofPath = "$prefix-$Phase.proof.json"
    $job = "$prefix-$Phase.job"
    [IO.File]::WriteAllLines($job, @('linkedHundred=1', "phase=$Phase", "connect=$port", ('out=' + $proofPath.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
}
function Finish-Client([string]$Phase, [string[]]$Keys) {
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $client.HasExited "$Phase client completed"
    [void][M61ProcessLog]::Finish($client)
    Assert ($client.ExitCode -eq 0) "$Phase client clean exit"
    $script:client = $null
    $text = Read-JavaPrototypeLog $clientLog
    Assert ($text.Contains('LINKEDHUNDRED DONE success=true')) "$Phase client success marker"
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
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one match of $pattern in the seed" }
    if ([regex]::Matches($text, ('(?m)^    "Texture": "' + [regex]::Escape($STEVE) + '"')).Count -ne 1) { throw 'Expected the steve skin in the seed' }
    $text = [regex]::Replace($text, $pattern, ('    "Name": "' + $Name + '"'))
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Write-Group([string]$Destination, [string]$Name, [string]$Texture) {
    # The form the server writes: save() rewrites every group (original behaviour), so a different seed form would change bits.
    $text = "{`n    `"NPCData`": {`n        `"Texture`": `"$Texture`"`n    },`n    `"LinkedName`": `"$Name`"`n}`n"
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Wand-Lines { return [regex]::Matches((Log), 'RWLINKED wand player=' + $player + ' op=(\w+) group=(\S*) done=(\d+) asked=(\d+)') }

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm135-hundred'
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
    Assert (-not ($cfgText -match '(?m)^RwLinkedBudget=')) 'transfer budget at its default'
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
    Write-Fixture (Join-Path $clones 'WGuard.json') 'WGuard'
    Write-Fixture (Join-Path $clones 'WCook.json') 'WCook'
    $linked = Join-Path $work "$world/customnpcs/linkednpcs"
    New-Item -ItemType Directory -Force -Path $linked | Out-Null
    $lw1 = Join-Path $linked 'LW1.json'; $lw2 = Join-Path $linked 'LW2.json'; $prev = "$lw1`_prev"
    Write-Group $lw1 'LW1' $T1
    Write-Group $lw2 'LW2' $STEVE
    $lw1Seed = Fingerprint $lw1; $lw2Seed = Fingerprint $lw2

    Start-Phase 'p1-main' 1
    Assert ((Log).Contains('RWLINKED channel rwlinked registered')) 'channel registered with RwLinkedGroups=1'
    Start-Client 'main'
    $keys = @('rows-from-server', 'placed-before-list', 'one-selection-100', 'link-dialog-group', 'link-reply-100', 'link-reaches-100',
        'group-and-count-shown', 'edit-member', 'update-notification-99', 'change-reaches-100')
    $main = Finish-Client 'main' $keys
    $lines = @(Wand-Lines | ForEach-Object { $_.Groups[1].Value + ':' + $_.Groups[2].Value + ':' + $_.Groups[3].Value + ':' + $_.Groups[4].Value })
    Assert (($lines -join '|') -ceq 'LINK:LW1:100:100') ('one wand action linked the 100: ' + ($lines -join '|'))
    Assert ((Log).Contains("RWLINKED update player=$player Group LW1: updated 99 loaded NPCs; pending 0")) 'server reported the edit reaching the other 99'
    Send 'save-all flush'
    Expect 'Saved the world' 'world saved' 180
    Stop-Phase 'p1-main'
    $lw1Text = [IO.File]::ReadAllText($lw1)
    Assert ((Fingerprint $lw1) -cne $lw1Seed -and $lw1Text.Contains('"Title": "M135 Edit"')) 'the edit was saved to the group file'
    Assert ((Fingerprint $prev) -ceq $lw1Seed -and (Fingerprint $lw2) -ceq $lw2Seed) 'previous version kept for undo bitwise; other group bitwise unchanged'
    $groupHashes = @((Fingerprint $lw1), (Fingerprint $prev), (Fingerprint $lw2))

    [IO.File]::WriteAllText($opsPath, $opsText, (New-Object Text.UTF8Encoding $false))
    Start-Phase 'p2-off' 0
    Assert (-not (Log).Contains('RWLINKED channel')) 'no channel with RwLinkedGroups=0'
    Start-Client 'off'
    $off = Finish-Client 'off' @('off-original-list', 'off-state-kept')
    Stop-Phase 'p2-off'
    Assert (-not (Log).Contains('RWLINKED ')) 'no RWLINKED line at 0'
    Assert ((@((Fingerprint $lw1), (Fingerprint $prev), (Fingerprint $lw2)) -join '|') -ceq ($groupHashes -join '|')) 'group files bitwise unchanged at 0'

    $all = @($main.screenshots) + @($off.screenshots)
    foreach ($image in $all) {
        Assert ([IO.Path]::GetFileName($image) -ceq $image) 'local screenshot filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    Assert ($all.Count -eq 5) 'five required screenshots'
    Unchanged
    @{success=$true; package='M13.5'; checks=@($checks); inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      main=$main; off=$off;
      condition='one real dev client and dev dedicated server; two starts (switch 1, then both sides 0); isolated flat world/config copy; 100 members placed with noppes clone grid before the list opens, 5 distractors and 2 same-name NPCs past the 32 block radius; regular mouse and key handlers; no packaged/integrated pair; no performance measurement'} |
      ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M135 evidence: $prefix.json ($($checks.Count) checks)"
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
