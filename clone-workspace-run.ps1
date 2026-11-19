# M16.7c1 dedicated acceptance; launched only by the controller.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$dev = Join-Path $root 'dev'
$seed = Join-Path $dev 'run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$audit = Join-Path $root "audit/m167c1-workspace-$stamp"
$work = Join-Path $dev "run-m167c1/$stamp"
$world = 'm167c1-world'
$server = $null; $serverLog = $null; $lock = $null
$owned = @{}; $checks = New-Object Collections.ArrayList
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|Error executing task|script errored|ScriptException|WORKSPACEPROBE ERROR|RWJAVA.*failed'
function Check([bool]$ok, [string]$label) {
    if (-not $ok) { throw $label }
    [void]$checks.Add($label); Write-Host "OK $label"
}
function Write-Text([string]$file, [string]$value) { [IO.File]::WriteAllText($file, $value, $utf8) }
function Hash([string]$file) { (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
function Guard {
    $roots = @($owned.Keys); if ($null -ne $server) { $roots += $server.Id }
    $snapshot = @(Get-CimInstance Win32_Process -ErrorAction Stop)
    $tree = @{}; foreach ($id in $roots) { $tree[[int]$id] = $true }
    $grew = $true
    while ($grew) {
        $grew = $false
        foreach ($p in $snapshot) { if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId] = $true; $grew = $true } }
    }
    foreach ($p in $snapshot) { if ($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true } }
    $foreign = @(Get-NavForeignJava -Roots @($tree.Keys))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Log { Read-JavaPrototypeLog $serverLog }
function Send([string]$command) { $server.StandardInput.WriteLine($command); $server.StandardInput.Flush() }
function Expect([string]$marker, [int]$seconds = 240) {
    $until = (Get-Date).AddSeconds($seconds)
    do {
        $log = Log
        if ($server.HasExited -or $log -match $bad) { throw "Failed before $marker. See $serverLog" }
        if ($log.Contains($marker)) { Check $true $marker; return }
        Guard; Start-Sleep -Milliseconds 400
    } while ((Get-Date) -lt $until)
    throw "Timed out waiting for $marker. See $serverLog"
}
function Start-Phase([string]$phase, [int]$readable) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwAnimCore|RwReadableClones|RwJavaClasspath|EnableScripting)=.*\r?\n?', '')
    Write-Text $cfg ($text + "`nRwAnimCore=1`nRwReadableClones=$readable`nRwJavaClasspath=auto`nEnableScripting=true`n")
    $code = "var PHASE = '$phase';`n" + [IO.File]::ReadAllText((Join-Path $dev 'testworld/clone-workspace-probe.js'))
    $source = [IO.File]::ReadAllText((Join-Path $dev 'testworld/customnpcs/clones/1/M36_Stand.json'))
    $source = $source.Replace('"Name": "M36_Stand"', '"Name": "WSProbe"').Replace('"ScriptLanguage": ""', '"ScriptLanguage": "ECMAScript"').Replace('"ScriptEnabled": 0b', '"ScriptEnabled": 1b')
    $source = $source.Replace('"SpawnCycle": 0', '"SpawnCycle": 3')
    $pattern = '"Scripts":\s*\[\s*\]'
    Check ([regex]::Matches($source, $pattern).Count -eq 1) 'exactly one empty probe script list in seed'
    # Legacy CNPC strings escape only backslash/quote; CR/LF remain literal.
    # JSON's \n or \u0027 would be read as the literal letters n/u0027 by NbtJson.
    $scriptNbt = '"' + $code.Replace('\', '\\').Replace('"', '\"') + '"'
    $replacement = '"Scripts": [{"Script":' + $scriptNbt + ',"ScriptList":[],"Console":[]}]'
    # MatchEvaluator preserves literal dollar signs in the source code.
    $source = [regex]::Replace($source, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) $replacement })
    Write-Text (Join-Path $work "$world/customnpcs/clones/1/WSProbe.json") $source
    $script:serverLog = "$audit-$phase.log"
    $script:server = Start-JavaPrototypeProcess (Join-Path $dev 'gradlew.bat') @('runServer','--offline','--no-daemon','--console=plain',"-PrwRunDir=$work", "-PrwWorld=$world") $dev $serverLog
    Expect 'Done (' 600
    Check ((Log).Contains('Preparing level "' + $world + '"')) "$phase isolated world loaded"
    Send 'gamerule doMobSpawning false'
    Send 'setworldspawn 0 4 0'
    Send 'summon minecraft:pig 0 4 4 {NoAI:1b}'
    Send 'noppes clone spawn WSProbe 1 0,4,8'
    # Q547/Q486: the diagnostic loader scans existing NPCs; an empty world cannot
    # receive a ticket. Queue the probe first, then enable ticking for its chunk.
    Send 'rwdiag chunks on 1'
    Expect 'RWDIAG-CHUNKS stanje=on'
    Expect "WORKSPACEPROBE DONE phase=$phase " 600
    $log = Log
    $done = [regex]::Match($log, "WORKSPACEPROBE DONE phase=$phase checks=(\d+) failed=(\d+)")
    $rows = [regex]::Matches($log, "WORKSPACEPROBE CHECK phase=$phase key=(\S+) ok=(\d)")
    # Keep the single verify key as an array; otherwise += concatenates strings.
    $expectedKeys = @(if ($phase -eq 'verify') { 'switch' } else {
        @('switch','bad-id-refused','bad-tab-refused','bad-name-refused','multi-selector-refused','non-npc-refused','level2-writer-refused',
          'unpack-live-command','unpack-live-clean-snapshot','pack-live-command','pack-live-exact')
    })
    $ids = if ($phase -eq 'verify') { @('off_java','off_js','on_java','on_js') } else { @("${phase}_java", "${phase}_js") }
    foreach ($id in $ids) {
        $expectedKeys += @("$id-live-language-tabs-rig", "$id-first-tab-runs", "$id-second-tab-runs")
        if ($phase -ne 'verify') {
            foreach ($suffix in @('unpack-command','typed-unpack','exact-code-bytes','pack-command','typed-pack','output-format','source-unchanged','no-overwrite','spawn-command')) { $expectedKeys += "$id-$suffix" }
        }
        Expect "WORKSPACERAN edited_$id tab=0 name=WSPlaced_$id"
        Expect "WORKSPACERAN original_$id tab=1 name=WSPlaced_$id"
    }
    Check ($done.Success -and [int]$done.Groups[2].Value -eq 0 -and [int]$done.Groups[1].Value -eq $expectedKeys.Count) "$phase exact successful assertion count"
    Check ($rows.Count -eq $expectedKeys.Count -and @($rows | Where-Object { $_.Groups[2].Value -ne '1' }).Count -eq 0) "$phase all checks pass"
    Check ((@($rows | ForEach-Object { $_.Groups[1].Value } | Sort-Object) -join '|') -ceq (@($expectedKeys | Sort-Object) -join '|')) "$phase exact assertion keys"
    foreach ($row in $rows) { [void]$checks.Add("$phase $($row.Groups[1].Value)") }
}
function Stop-Phase([string]$phase) {
    # The probe is a controller, not a fixture that may re-run setup after restart.
    Send 'kill @e[type=customnpcs:customnpc,name=WSProbe]'
    Send 'save-all flush'; Expect 'Saved the world'
    Send 'stop'
    $until = (Get-Date).AddSeconds(180)
    while (-not $server.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 400 }
    Check $server.HasExited "$phase process exited"
    [void][M61ProcessLog]::Finish($server)
    Check ($server.ExitCode -eq 0 -and -not ((Log) -match $bad)) "$phase clean exit without script/ticking/linkage faults"
    $script:server = $null
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm167c1-workspace'
    Guard
    $protected = @{}
    foreach ($file in @('environment-lock.json','CustomNPCs_1.12.2-(01Oct19).jar','dev/testworld/customnpcs/clones/1/M36_Stand.json','dev/testworld/clone-workspace-probe.js')) {
        $path = Join-Path $root $file; $protected[$path] = Hash $path
    }
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $seed 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $seed 'eula.txt') -Destination $work
    New-Item -ItemType Directory -Path (Join-Path $work "$world/customnpcs/clones/1") -Force | Out-Null
    foreach ($language in @('java','ecmascript')) {
        $ext = if ($language -eq 'java') { 'java' } else { 'js' }
        $scripts = Join-Path $work "$world/customnpcs/scripts/$language"
        New-Item -ItemType Directory -Path $scripts -Force | Out-Null
        Write-Text (Join-Path $scripts "workspace-helper.$ext") ''
    }
    $props = [IO.File]::ReadAllText((Join-Path $seed 'server.properties'))
    Check ($props -match '(?m)^online-mode=false\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'isolated flat offline seed'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port" -replace '(?m)^server-ip=.*$', 'server-ip=127.0.0.1' -replace '(?m)^level-name=.*$', "level-name=$world"
    Write-Text (Join-Path $work 'server.properties') $props
    Start-Phase 'off' 0
    Stop-Phase 'off'
    Start-Phase 'on' 1
    Stop-Phase 'on'
    $workspaces = @{}
    foreach ($file in @(Get-ChildItem -LiteralPath (Join-Path $work 'customnpcs/creatures') -Recurse -File)) { $workspaces[$file.FullName] = Hash $file.FullName }
    Start-Phase 'verify' 1
    Stop-Phase 'verify'
    foreach ($file in $workspaces.Keys) { Check ((Hash $file) -ceq $workspaces[$file]) "restart did not rewrite $file" }
    foreach ($file in $protected.Keys) { Check ((Hash $file) -ceq $protected[$file]) "protected input unchanged: $file" }
    [ordered]@{ result='PASS'; checks=@($checks); work=$work; world=$world; port=$port } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$audit.json" -Encoding UTF8
    Write-Host "PASS M16.7c1: $audit ($($checks.Count) checks)"
} finally {
    if ($null -ne $server) { Stop-JavaPrototypeProcess $server }
    $env:JAVA_HOME = $oldJava; $env:PATH = $oldPath
    if ($null -ne $lock) { $lock.Dispose() }
}
