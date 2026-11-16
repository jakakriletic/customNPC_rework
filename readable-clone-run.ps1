# M16.6 acceptance. Launched only by the overnight controller; owns the isolated client/server pair.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m166-readable-$stamp"
$work = Join-Path $root "dev/run-m166/$stamp"
$world = 'm166-world'
$player = 'M166_Player'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null; $clientLog = $null
$hashes = @{}; $owned = @{}; $checks = New-Object Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|Error executing task|script errored|ScriptException'

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    [void]$checks.Add($Text); Write-Host "OK $Text"
}
function Fingerprint([string]$Path) { if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }; return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash }
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
function Expect([string]$Marker,[int]$Seconds = 180) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Log
        if ($text.Contains($Marker)) { Assert $true "server marker $Marker"; return }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before $Marker; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw "Timeout before $Marker; $serverLog"
}
function Set-Switch([int]$Value) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwReadableClones|RwAnimCore)=.*\r?\n?', '')
    # The rig core stays 1 on both sides in all cells; only the clone disk writer switches.
    [IO.File]::WriteAllText($cfg, $text + "`nRwAnimCore=1`nRwReadableClones=$Value`n", $utf8)
}
function Start-Phase([string]$Phase,[int]$Value) {
    Set-Switch $Value
    $script:serverLog = "$prefix-$Phase-server.log"
    $script:server = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runServer','--offline','--no-daemon','--console=plain',"-PrwRunDir=$work", "-PrwWorld=$world") (Join-Path $root 'dev') $serverLog
    Expect 'Done (' 600
    Assert ((Log).Contains('Preparing level "' + $world + '"')) "$Phase isolated world"
    Send 'gamerule doMobSpawning false'
    Send 'setworldspawn 0 4 0'
    if ($Phase -eq 'off') { Send 'noppes clone spawn ReadableSeed 1 2,3,3' }
    Send "noppes clone spawn RCProbe_$Phase 1 0,3,8"
}
function Stop-Phase([string]$Phase) {
    Send 'save-all flush'; Expect 'Saved the world'
    Send 'stop'
    $until = (Get-Date).AddSeconds(180)
    while (-not $server.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $server.HasExited "$Phase server stopped"
    [void][M61ProcessLog]::Finish($server)
    Assert ($server.ExitCode -eq 0) "$Phase server clean exit"
    Assert (-not ((Log) -cmatch $bad)) "$Phase no server linkage/ticking/script faults"
    $script:server = $null
}
function Client-Phase([string]$Phase,[int]$Value,[string[]]$ClientKeys,[string[]]$ServerKeys) {
    $proofPath = "$prefix-$Phase.proof.json"; $job = "$prefix-$Phase.job"
    $script:gatePath = Join-Path $work "$Phase-gate.txt"
    [IO.File]::WriteAllLines($job,@('readableClone=1',"phase=$Phase","switch=$Value","connect=$port",('gate='+$gatePath.Replace('\','/')),('out='+$proofPath.Replace('\','/'))),$utf8)
    $script:clientLog = "$prefix-$Phase-client.log"
    $script:client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient','--offline','--no-daemon','--console=plain',"-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\','/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
    $until = (Get-Date).AddSeconds(900)
    do {
        $text = Read-JavaPrototypeLog $clientLog
        if ($text.Contains("READABLECLIENT SYNC $Phase")) { break }
        if ($client.HasExited -or $server.HasExited -or $text -cmatch $bad) { throw "$Phase failed before sync; $clientLog" }
        if ((Get-Date) -gt $until) { throw "$Phase client timeout; $clientLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ($true)
    [IO.File]::WriteAllText($gatePath,"run`n",$utf8)
    Expect "READABLEPROBE DONE phase=$Phase "
    $rows = [regex]::Matches((Log),'READABLEPROBE CHECK key=(\S+) ok=(\d) detail=([^\r\n]*)')
    Assert (($rows.Count -eq $ServerKeys.Count) -and (@($rows | Where-Object { $_.Groups[2].Value -ne '1' }).Count -eq 0)) "$Phase all server assertions ($($rows.Count))"
    Assert ((@($rows | ForEach-Object { $_.Groups[1].Value } | Sort-Object) -join '|') -ceq (@($ServerKeys | Sort-Object) -join '|')) "$Phase exact server assertion keys"
    foreach ($row in $rows) { [void]$checks.Add("$Phase server $($row.Groups[1].Value)") }
    [IO.File]::AppendAllText($gatePath,"done`n",$utf8)
    $until = (Get-Date).AddSeconds(240)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $client.HasExited "$Phase client completed"
    [void][M61ProcessLog]::Finish($client)
    Assert ($client.ExitCode -eq 0) "$Phase client clean exit"
    $script:client = $null
    $text = Read-JavaPrototypeLog $clientLog
    Assert ($text.Contains('READABLECLIENT DONE success=true') -and -not ($text -cmatch $bad)) "$Phase client success, no linkage/ticking/script faults"
    $proof = [IO.File]::ReadAllText($proofPath) | ConvertFrom-Json
    Assert ($proof.success -eq $true -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) "$Phase proof success"
    Assert ((@($proof.checks.key | Sort-Object) -join '|') -ceq (@($ClientKeys | Sort-Object) -join '|')) "$Phase exact client assertion keys"
    foreach ($row in $proof.checks) { [void]$checks.Add("$Phase client $($row.key)") }
    foreach ($img in $proof.screenshots) {
        Assert ([IO.Path]::GetFileName($img) -ceq $img) 'local screenshot filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $img
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $img"
    }
    return $proof
}
function Install-Probe([string]$Phase) {
    $code = Join-Path $work "$Phase-control.js"
    $gate = (Join-Path $work "$Phase-gate.txt").Replace('\','/').Replace("'","\'")
    [IO.File]::WriteAllText($code, "var PHASE='$Phase'; var GATE='$gate';`n" + [IO.File]::ReadAllText((Join-Path $seed 'readable-clone-control.js')), $utf8)
    $dest = Join-Path $clones "RCProbe_$Phase.json"
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') $code $dest
    $text = [IO.File]::ReadAllText($dest).Replace('"Name": "PERF_Kontrola"', '"Name": "RCProbe_' + $Phase + '"')
    [IO.File]::WriteAllText($dest,$text,$utf8)
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm166-readable'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $protected = @((Join-Path $root 'environment-lock.json'),(Join-Path $run 'options.txt'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $seed 'customnpcs/clones/1') -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$' -and $props -match '(?m)^online-mode=false\s*$') 'existing flat local offline scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0); $listener.Start(); $port=$listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'),$props,$utf8)
    $md5 = [Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$player"))
    $md5[6]=($md5[6] -band 0x0f) -bor 0x30; $md5[8]=($md5[8] -band 0x3f) -bor 0x80
    $hex=($md5 | ForEach-Object { $_.ToString('x2') }) -join ''
    $uuid=$hex.Substring(0,8)+'-'+$hex.Substring(8,4)+'-'+$hex.Substring(12,4)+'-'+$hex.Substring(16,4)+'-'+$hex.Substring(20,12)
    [IO.File]::WriteAllText((Join-Path $work 'ops.json'),('[{"uuid":"'+$uuid+'","name":"'+$player+'","level":4,"bypassesPlayerLimit":false}]'),$utf8)
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    $localClones = Join-Path $work 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones,$localClones | Out-Null
    foreach ($phase in @('off','on','verify','back')) { Install-Probe $phase }
    $sourcePath = Join-Path $clones 'ReadableSeed.json'
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'readable-clone-source.js') $sourcePath
    $text=[IO.File]::ReadAllText($sourcePath).Replace('"Name": "PERF_Kontrola"','"Name": "ReadableSeed"')
    $clips='{"animations":{"rc_probe":{"animation_length":1,"loop":true,"bones":{}}}}'
    $rig='"RwRig": {"Model": "knight", "Clips": "'+$clips.Replace('\','\\').Replace('"','\"')+'"},'
    $text=$text.Insert($text.IndexOf('{')+1,"`n    $rig")
    [IO.File]::WriteAllText($sourcePath,$text,$utf8)
    Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $clones 'RC_upgrade.json')
    $seedHash=Fingerprint $sourcePath; $upgradeHash=Fingerprint (Join-Path $clones 'RC_upgrade.json')
    $clientWrite=@('switch','local-roundtrip','local-format','local-list')
    $serverWrite=@('switch','source-rig-script','server-clonesave-roundtrip','server-format','command-add-roundtrip','command-spawn-rig','clone-list-no-artifacts')
    Start-Phase 'off' 0
    $off=Client-Phase 'off' 0 $clientWrite $serverWrite
    Stop-Phase 'off'
    Start-Phase 'on' 1
    $on=Client-Phase 'on' 1 ($clientWrite+@('conversion')) ($serverWrite+@('legacy-read-no-migration','legacy-overwrite-backup','legacy-upgrade-roundtrip','backup-not-replaced'))
    Stop-Phase 'on'
    Assert ((Fingerprint (Join-Path $clones 'RC_upgrade.json.v0.bak')) -ceq $upgradeHash) 'backup exactly original legacy fixture'
    $serverNewHash=Fingerprint (Join-Path $clones 'RC_onServer.json')
    $localNewHash=Fingerprint (Join-Path $localClones 'RC_onLocal.json')
    Start-Phase 'verify' 1
    $verify=Client-Phase 'verify' 1 @('switch','restart-local-read','local-placement','server-placement') @('switch','source-rig-script','restart-server-read','local-placement-rig-script','server-placement-rig-script','clone-list-no-artifacts')
    Stop-Phase 'verify'
    Assert ((Fingerprint (Join-Path $clones 'RC_onServer.json')) -ceq $serverNewHash -and (Fingerprint (Join-Path $localClones 'RC_onLocal.json')) -ceq $localNewHash) 'restart and both clone tab placements never rewrite readable source files'
    Start-Phase 'back' 0
    $back=Client-Phase 'back' 0 $clientWrite ($serverWrite+@('readable-at-zero','legacy-reexport','readable-command-spawn-at-zero'))
    Stop-Phase 'back'
    Assert ((Fingerprint (Join-Path $clones 'RC_onServer.json')) -ceq $serverNewHash -and (Fingerprint (Join-Path $localClones 'RC_onLocal.json')) -ceq $localNewHash) 'readable files unchanged after reading at zero'
    Assert ((Fingerprint $sourcePath) -ceq $seedHash) 'legacy seed never migrated by reading/spawning'
    Assert ((Fingerprint (Join-Path $clones 'RC_upgrade.json.v0.bak')) -ceq $upgradeHash) 'backup unchanged through all restarts'
    Unchanged
    @{success=$true; package='M16.6b'; checks=@($checks); inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash;
      off=$off; on=$on; verify=$verify; back=$back;
      condition='one dev client/dedicated pair, four server and client JVM starts; RwReadableClones=0/1/1/0; RwAnimCore=1 fixed; real local and server CloneSave and clone tab placement; exact typed cleaned-input comparison; actual clone commands; rig and script execution server-side; one conversion time/size datum, no per-tick cost measurement'} |
      ConvertTo-Json -Depth 15 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M166 evidence: $prefix.json ($($checks.Count) checks)"
    $exitCode=0
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
