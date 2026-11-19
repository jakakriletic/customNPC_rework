# M16.7c2: controller-owned dedicated server, two clients, edit/deploy and restart.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot; $dev = Join-Path $root 'dev'; $seed = Join-Path $dev 'run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$audit = Join-Path $root "audit/m167c2-live-$stamp"
$work = Join-Path $dev "run-m167c2/$stamp"
$serverDir = Join-Path $work 'server'; $world = 'm167c2-world'
$signal = Join-Path $work 'step.txt'
$jobs = @{}; $owned = @{}; $checks = New-Object Collections.ArrayList; $lock = $null
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|Error executing task|script errored|ScriptException|WSLIVE ERROR|WSLIVE CLIENT DONE .*ok=false|RWJAVA.*failed|OpenGL Error'
function Check([bool]$ok, [string]$key) {
    if (-not $ok) { throw $key }
    [void]$checks.Add($key); Write-Host "OK $key"
}
function Write-Text([string]$file, [string]$text) { [IO.File]::WriteAllText($file,$text,$utf8) }
function Hash([string]$file) { (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
function Guard {
    $roots = @($owned.Keys) + @($jobs.Values | ForEach-Object { $_.Process.Id })
    $snapshot = @(Get-CimInstance Win32_Process -ErrorAction Stop); $tree = @{}
    foreach ($processId in $roots) { $tree[[int]$processId] = $true }
    $grew = $true
    while ($grew) {
        $grew = $false
        foreach ($p in $snapshot) { if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId] = $true; $grew = $true } }
    }
    foreach ($p in $snapshot) { if ($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true } }
    $foreign = @(Get-NavForeignJava -Roots @($tree.Keys))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Log([string]$name) { Read-JavaPrototypeLog $jobs[$name].Log }
function Expect([string]$name, [string]$marker, [int]$seconds = 240) {
    $until = (Get-Date).AddSeconds($seconds)
    do {
        $log = Log $name
        if ($log -match $bad) { throw "$name failed before $marker. See $($jobs[$name].Log)" }
        if ($log.Contains($marker)) { Check $true "$name $marker"; return }
        if ($jobs[$name].Process.HasExited) { throw "$name exited before $marker" }
        Guard; Start-Sleep -Milliseconds 400
    } while ((Get-Date) -lt $until)
    throw "$name timeout before $marker. See $($jobs[$name].Log)"
}
function Send([string]$name, [string]$command) { $jobs[$name].Process.StandardInput.WriteLine($command); $jobs[$name].Process.StandardInput.Flush() }
function Start-ProcessJob([string]$name, [string[]]$gradleArgs) {
    $path = "$audit-$name.log"
    $p = Start-JavaPrototypeProcess (Join-Path $dev 'gradlew.bat') $gradleArgs $dev $path
    $jobs[$name] = @{ Process=$p; Log=$path }
}
function Finish([string]$name) {
    $job = $jobs[$name]; $until = (Get-Date).AddSeconds(180)
    while (-not $job.Process.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 400 }
    Check $job.Process.HasExited "$name process exited"
    [void][M61ProcessLog]::Finish($job.Process)
    Check ($job.Process.ExitCode -eq 0 -and -not ((Log $name) -match $bad)) "$name clean exit and no script/linkage/ticking/GL faults"
    $jobs.Remove($name)
}
function Init-Run([string]$dir) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $seed 'config') -Destination (Join-Path $dir 'config') -Recurse
    $cfg = Join-Path $dir 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwAnimCore|RwJavaClasspath|EnableScripting)=.*\r?\n?', '')
    Write-Text $cfg ($text + "`nRwAnimCore=1`nRwJavaClasspath=auto`nEnableScripting=true`n")
}
function Start-Server([string]$phase) {
    $code = "var PHASE = '$phase'; var SIGNAL = " + (ConvertTo-Json -InputObject $signal -Compress) + ";`n" + [IO.File]::ReadAllText((Join-Path $dev 'testworld/clone-workspace-live-probe.js'))
    $source = [IO.File]::ReadAllText((Join-Path $dev 'testworld/customnpcs/clones/1/M36_Stand.json'))
    $source = $source.Replace('"Name": "M36_Stand"','"Name": "WSLiveProbe"').Replace('"ScriptLanguage": ""','"ScriptLanguage": "ECMAScript"').Replace('"ScriptEnabled": 0b','"ScriptEnabled": 1b').Replace('"SpawnCycle": 0','"SpawnCycle": 3')
    $pattern = '"Scripts":\s*\[\s*\]'
    Check ([regex]::Matches($source,$pattern).Count -eq 1) "$phase exact seed script list"
    $escaped = '"' + $code.Replace('\','\\').Replace('"','\"') + '"'
    $replacement = '"Scripts":[{"Script":' + $escaped + ',"ScriptList":[],"Console":[]}]'
    $source = [regex]::Replace($source,$pattern,[Text.RegularExpressions.MatchEvaluator]{param($m) $replacement})
    Write-Text (Join-Path $serverDir "$world/customnpcs/clones/1/WSLiveProbe.json") $source
    Start-ProcessJob $phase @('runServer','--offline','--no-daemon','--console=plain',"-PrwRunDir=$serverDir","-PrwWorld=$world")
    Expect $phase 'Done (' 600
    Check ((Log $phase).Contains('Preparing level "' + $world + '"')) "$phase isolated world"
    Send $phase 'gamerule doMobSpawning false'; Send $phase 'gamerule spawnRadius 0'; Send $phase 'setworldspawn 0 4 0'
    Send $phase 'noppes clone spawn WSLiveProbe 1 0,4,10'
    Send $phase 'rwdiag chunks on 1'; Expect $phase 'RWDIAG-CHUNKS stanje=on'
}
function Stop-Server([string]$phase) {
    Send $phase 'kill @e[type=customnpcs:customnpc,name=WSLiveProbe]'
    Send $phase 'save-all flush'; Expect $phase 'Saved the world'
    Send $phase 'stop'; Finish $phase
}
function Start-Client([string]$mode, [string]$serverName) {
    $dir = Join-Path $work $mode; Init-Run $dir
    $player = if ($mode -like 'restart*') { 'WSL_Restart' } else { "WSL_$mode" }
    $job = "$audit-$mode.job"
    Write-Text $job ("cloneWorkspace=1`nmode=$mode`nconnect=$port`nshots=$($audit.Replace('\','/'))`n")
    Start-ProcessJob $mode @('runClient','--offline','--no-daemon','--console=plain',"-PrwRunDir=$dir","-PrwHarness=$($job.Replace('\','/'))","-PrwUsername=$player")
    Expect $mode "WSLIVE CLIENT CONNECTED $mode" 600
    Send $serverName "gamemode spectator $player"; Send $serverName "tp $player -5.0 4.0 -8.0 0 8"
}
function Check-Probe([string]$phase, [string[]]$keys) {
    Expect $phase "WSLIVE DONE phase=$phase " 600
    $log = Log $phase; $done = [regex]::Match($log,"WSLIVE DONE phase=$phase checks=(\d+) failed=(\d+)")
    $rows = @([regex]::Matches($log,"WSLIVE CHECK phase=$phase key=(\S+) ok=(\d)"))
    Check ($done.Success -and [int]$done.Groups[1].Value -eq $keys.Count -and [int]$done.Groups[2].Value -eq 0) "$phase exact probe assertion count"
    Check ($rows.Count -eq $keys.Count -and @($rows | Where-Object { $_.Groups[2].Value -ne '1' }).Count -eq 0) "$phase all probe assertions pass"
    Check ((@($rows | ForEach-Object { $_.Groups[1].Value } | Sort-Object) -join '|') -ceq (@($keys | Sort-Object) -join '|')) "$phase exact probe assertion keys"
    foreach ($row in $rows) { [void]$checks.Add("$phase $($row.Groups[1].Value)") }
    foreach ($rev in @('v1','v2')) { foreach ($language in @('Java','JS')) {
        Expect $phase "WSLIVE RAN $language $rev tab=0 name=WS_"
    } }
    foreach ($language in @('Java','JS')) { Expect $phase "WSLIVE RAN $language v1 tab=1 name=WS_" }
}
function Check-Client([string]$mode) {
    Expect $mode "WSLIVE CLIENT DONE mode=$mode " 600
    $log = Log $mode; $rows = @([regex]::Matches($log,"WSLIVE CLIENT CHECK mode=$mode key=(\S+) ok=1"))
    $done = [regex]::Match($log,"WSLIVE CLIENT DONE mode=$mode checks=(\d+) ok=true")
    $expected = if ($mode -like 'restart*') { 12 } else { 20 }
    Check ($done.Success -and [int]$done.Groups[1].Value -eq $expected -and $rows.Count -eq $expected) "$mode exact client checks"
    Check (@($rows | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique).Count -eq $expected) "$mode unique client checks"
    foreach ($row in $rows) { [void]$checks.Add("$mode $($row.Groups[1].Value)") }
    Finish $mode
}
function Field-Keys([string[]]$prefixes) {
    foreach ($key in $prefixes) { foreach ($suffix in @('language-tabs-includes-enabled','rig-bindings-clips','health-position-start','scripts-run-or-disabled')) { "$key-$suffix" } }
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm167c2-live'
    Guard
    $protected = @{}
    foreach ($file in @('environment-lock.json','CustomNPCs_1.12.2-(01Oct19).jar','dev/testworld/customnpcs/clones/1/M36_Stand.json','dev/testworld/clone-workspace-live-probe.js')) { $p = Join-Path $root $file; $protected[$p] = Hash $p }
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Init-Run $serverDir
    Copy-Item -LiteralPath (Join-Path $seed 'eula.txt') -Destination $serverDir
    New-Item -ItemType Directory -Path (Join-Path $serverDir "$world/customnpcs/clones/1") -Force | Out-Null
    foreach ($language in @('java','ecmascript')) {
        $dir = Join-Path $serverDir "$world/customnpcs/scripts/$language"; New-Item -ItemType Directory -Path $dir -Force | Out-Null
        if ($language -eq 'java') { Write-Text (Join-Path $dir 'live-helper.java') 'public final class LiveHelper { public static String value(String v) { return v; } }' }
        else { Write-Text (Join-Path $dir 'live-helper.js') 'function liveHelper(e, key, value) { e.npc.getStoreddata().put(key, value); }' }
    }
    $props = [IO.File]::ReadAllText((Join-Path $seed 'server.properties'))
    Check ($props -match '(?m)^online-mode=false\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'isolated flat offline seed'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0); $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$',"server-port=$port" -replace '(?m)^server-ip=.*$','server-ip=127.0.0.1' -replace '(?m)^level-name=.*$',"level-name=$world"
    Write-Text (Join-Path $serverDir 'server.properties') $props
    Start-Server 'live'; Expect 'live' 'WSLIVE READY' 600
    Start-Client 'first' 'live'; Expect 'first' 'WSLIVE CLIENT READY first' 600
    Start-Client 'late' 'live'; Expect 'late' 'WSLIVE CLIENT READY late' 600
    Write-Text $signal 'edit'
    $liveKeys = @('loaded-workspace-ids','workspace-id-not-model','wire-has-only-model-assets','spawn-a_rig--3','spawn-b_plain-0','spawn-c_disabled-3','spawn-z_model-6','initial-read-spawn-no-rewrite','spawn-a_rig-9','spawn-before-reload-uses-snapshot','spawn-b_plain--9','fresh-identity-startpos','ordinary-m15-replaces-clone-rig-and-scripts','bad-clips-keeps-entire-snapshot','bad-reload-keeps-entire-model-snapshot','bad-deploy-no-spawn','spawn-a_rig--12','bad-reload-keeps-clone-snapshot','reload-spawn-never-rewrites-workspaces')
    $liveKeys += @(Field-Keys @('initial-rig-v1','initial-plain-v1','initial-disabled-v1','old-rig-v1','old-plain-v1','disabled-v1','new-rig-v2','new-plain-v2'))
    Check-Probe 'live' $liveKeys
    Check-Client 'first'; Check-Client 'late'
    $disk = @{}
    foreach ($file in @(Get-ChildItem -LiteralPath (Join-Path $serverDir 'customnpcs/creatures') -Recurse -File)) { $disk[$file.FullName] = Hash $file.FullName }
    Stop-Server 'live'
    Start-Server 'restart'
    Expect 'restart' 'creature broken_start: Unknown clone workspace format/version'
    $restartKeys = @('startup-skips-broken-keeps-healthy','spawn-a_rig--15','spawn-b_plain--18')
    $restartKeys += @(Field-Keys @('restart-old-rig-v1','restart-old-plain-v1','restart-disabled-v1','restart-existing-rig-v2','restart-existing-plain-v2','restart-new-rig-v2','restart-new-plain-v2'))
    Check-Probe 'restart' $restartKeys
    Start-Client 'restart-client' 'restart'
    Check-Client 'restart-client'
    Stop-Server 'restart'
    foreach ($file in $disk.Keys) { Check ((Hash $file) -ceq $disk[$file]) "restart/spawn did not rewrite $file" }
    foreach ($file in $protected.Keys) { Check ((Hash $file) -ceq $protected[$file]) "protected input unchanged: $file" }
    [ordered]@{result='PASS'; checks=@($checks); work=$work; port=$port; screenshots=@(Get-ChildItem -Path "$audit-*.png" | ForEach-Object FullName)} | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$audit.json" -Encoding UTF8
    Write-Host "PASS M16.7c2: $audit ($($checks.Count) checks)"
} finally {
    foreach ($job in @($jobs.Values)) { Stop-JavaPrototypeProcess $job.Process }
    $env:JAVA_HOME = $oldJava; $env:PATH = $oldPath
    if ($null -ne $lock) { $lock.Dispose() }
}
