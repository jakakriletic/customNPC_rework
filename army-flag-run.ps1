# M11.11d: move one map flag only. One army in two groups 150 blocks apart; moveflag on the far group splits it
# off into its own army and moves it, the near group stays where it is with no order.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m1111-flag-$stamp"
$work = Join-Path $root "dev/run-m1111flag/$stamp"
$world = 'm1111flag-world'
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
function Send([string]$Text) { $server.StandardInput.WriteLine($Text); $server.StandardInput.Flush(); Start-Sleep -Milliseconds 300 }
function Wait-Marker([string]$Marker, [int]$Seconds = 60) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Log
        if ($text.Contains($Marker)) { return $true }
        if ($text.Contains('ARMYFFA FAILURE')) { throw "Army failed before '$Marker'; $serverLog" }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before '$Marker'; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    return $false
}
function Expect([string]$Marker, [string]$Text, [int]$Seconds = 60) { Assert (Wait-Marker $Marker $Seconds) $Text }
function Set-Switch([int]$Value) {
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwArmies=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwArmies=$Value`n", (New-Object Text.UTF8Encoding $false))
}
function Start-Phase([string]$Name, [int]$Switch) {
    Set-Switch $Switch
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
    if ($text.Contains('"RwArmy"')) { throw 'Seed already has an army.' }
    $pattern = '(?m)^    "Name": "' + [regex]::Escape($Old) + '"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one root name in $Source" }
    $text = [regex]::Replace($text, $pattern, '    "Name": "' + $Name + '"')
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}
function Done-Ok([string]$Phase) {
    $m = [regex]::Match((Log), "ARMYFFA DONE phase=$Phase checks=(\d+) failed=(\d+)")
    Assert ($m.Success -and $m.Groups[2].Value -eq '0') "controller $Phase checks=$($m.Groups[1].Value) failed=$($m.Groups[2].Value)"
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm1111-flag'
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
    $sceneCfg = [IO.File]::ReadAllText((Join-Path $work 'config/CustomNpcs.cfg'))
    Assert (-not ($sceneCfg -match '(?m)^NpcNavRange=(?!32\r?$)')) 'navigation range 32'
    Assert (-not ($sceneCfg -match '(?m)^RwNavBackend=(?!0\r?$)')) 'vanilla navigation backend'
    $props = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert ($props -match '(?m)^level-type=FLAT\s*$') 'existing flat scene'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port"
    $props = $props -replace '(?m)^level-name=.*$', "level-name=$world"
    [IO.File]::WriteAllText((Join-Path $work 'server.properties'), $props, (New-Object Text.UTF8Encoding $false))
    $clones = Join-Path $work "$world/customnpcs/clones/1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    Write-Fixture (Join-Path $seed 'customnpcs/clones/1/M36_Stand.json') (Join-Path $clones 'FFA_M.json') 'FFA_M' 'M36_Stand'

    # 1 bootstrap
    Start-Phase 'p1-bootstrap' 1
    Send 'list'
    Expect 'players online' 'console ready'
    Send 'setworldspawn 0 4 0'
    Expect 'Set the world spawn point to (0, 4, 0)' 'spawn at origin'
    Send 'save-all flush'
    Expect 'Saved the world' 'bootstrap saved' 120
    Stop-Phase 'p1-bootstrap'

    # 2 two groups of one army, one flag moved
    Start-Phase 'p2-flag' 1
    Send 'list'
    Expect 'players online' 'console ready p2'
    Send 'gamerule doMobSpawning false'
    for ($i = 0; $i -lt 12; $i++) { Send ("noppes clone spawn FFA_M 1 {0},4,{1}" -f (-6 + ($i % 4) * 3), (-3 + [Math]::Floor($i / 4) * 3)) }
    for ($i = 0; $i -lt 10; $i++) { Send ("noppes clone spawn FFA_M 1 {0},4,{1}" -f (144 + ($i % 4) * 3), (-3 + [Math]::Floor($i / 4) * 3)) }
    Send 'rwarmy create flag faction:0'
    Expect 'RWARMY created flag' 'army created'
    Send 'rwarmy assign flag @e[name=FFA_M]'
    Expect 'RWARMY assigned flag npcs=22 changed=22' 'all 22 in one army'
    Send 'rwarmy clusters flag'
    Expect 'RWARMY cluster flag main' 'army shows its flags'
    Assert ((Log) -match 'RWARMY cluster flag \w+ dim=0 at 14\d ') 'the far group is its own flag near x=148'
    Send 'rwarmy moveflag flag 300 300 150 4 60'
    Expect 'that flag of flag is gone' 'a position without a flag is refused, nothing split'
    Send 'rwarmy moveflag flag 148 0 150 4 60 freeforall 3 passive'
    Expect 'RWARMY split flag -> flag-2 members=10' 'only the 10 of the far flag are split off and moved'
    $state = ''; $last = ''
    $until = (Get-Date).AddSeconds(300)
    while ((Get-Date) -lt $until) {
        Send 'rwarmy status flag-2'
        Start-Sleep -Seconds 5
        $all = [regex]::Matches((Log), 'RWARMY order flag-2 state=(\w+)[^\r\n]*')
        if ($all.Count -gt 0) { $last = $all[$all.Count - 1].Value; $state = $all[$all.Count - 1].Groups[1].Value; if ($state -eq 'ARRIVED' -or $state -eq 'BLOCKED') { break } }
    }
    Assert ($state -eq 'ARRIVED') "the split flag arrives ($last)"
    Assert ([int][regex]::Match($last, 'arrived=(\d+)').Groups[1].Value -eq 10) 'all 10 of the flag hold their slot'
    Send 'rwarmy status flag'
    Expect 'RWARMY order flag state=NONE' 'the other flag got no order'
    $before = [regex]::Matches((Log), 'RWARMY cluster flag main').Count
    Send 'rwarmy clusters flag'
    $until = (Get-Date).AddSeconds(30)
    while ([regex]::Matches((Log), 'RWARMY cluster flag main').Count -le $before -and (Get-Date) -lt $until) { Start-Sleep -Milliseconds 300 }
    $main = [regex]::Matches((Log), 'RWARMY cluster flag main dim=0 at (-?\d+) (-?\d+) (-?\d+) size=(\d+)')
    $m = $main[$main.Count - 1]
    Assert ([int]$m.Groups[4].Value -eq 12 -and [Math]::Abs([int]$m.Groups[1].Value) -le 4 -and [Math]::Abs([int]$m.Groups[3].Value) -le 4) "the 12 near members stayed ($($m.Value))"
    Send 'rwarmy moveflag flag-2 150 60 150 4 70 freeforall 3 passive'
    Expect 'RWARMY move flag-2 dim=0 target=150.0,4.0,70.0' 'a flag that is the whole army is an ordinary move'
    Assert (-not (Log).Contains('flag-2 -> flag-2-2')) 'no split of a whole army'
    Stop-Phase 'p2-flag'
    Unchanged
    @{success=$true; package='M11.11d'; checks=@($checks); logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash; status=$last;
      condition='dev dedicated server, flat seed, spawn chunks; one army of 22 in two groups 150 blocks apart; moveflag on the far group'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M1111d evidence: $prefix.json"
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
