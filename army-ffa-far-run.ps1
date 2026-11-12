# M11.11b: free-for-all march of 100 members to a target 600 blocks away in never generated terrain.
# No player, no diagnostic chunks; spawn moved away after the order, so only the army tickets load the world.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
$seed = Join-Path $root 'dev/testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $seed 'java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m1111-far-$stamp"
$work = Join-Path $root "dev/run-m1111far/$stamp"
$world = 'm1111far-world'
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
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm1111-far'
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

    # 2 100 members, order to a target 600 blocks away, then nothing keeps the world loaded but the army.
    Start-Phase 'p2-far' 1
    Send 'list'
    Expect 'players online' 'console ready p2'
    Send 'gamerule doMobSpawning false'
    $region = Join-Path $work "$world/region"
    $before = @(Get-ChildItem -LiteralPath $region -Filter '*.mca' -ErrorAction SilentlyContinue | ForEach-Object Name)
    Assert (-not ($before -contains 'r.0.1.mca')) 'target region z=512..1023 never generated before the order'
    for ($i = 0; $i -lt 100; $i++) {
        $x = -30 + (($i * 37) % 61)
        $z = -30 + (($i * 53) % 41)
        Send "noppes clone spawn FFA_M 1 $x,4,$z"
    }
    Send 'rwarmy create far faction:0'
    Expect 'RWARMY created far' 'army created'
    Send 'rwarmy assign far @e[name=FFA_M]'
    Expect 'RWARMY assigned far npcs=100 changed=100' 'all 100 assigned'
    Send 'rwarmy move far 0 4 600 freeforall 3 passive'
    Expect 'RWARMY move far dim=0 target=0.0,4.0,600.0' 'far target accepted'
    Expect 'RWARMY freeforall far members=100' 'free-for-all march started'
    # Spawn chunks no longer cover the army; save-all unloads every chunk without a ticket.
    Send 'setworldspawn 5000 4 5000'
    Expect 'Set the world spawn point to (5000, 4, 5000)' 'spawn moved away'
    Send 'save-all'
    Expect 'Saved the world' 'world saved after the spawn move' 120
    $state = ''; $last = ''
    $until = (Get-Date).AddSeconds(1500)
    while ((Get-Date) -lt $until) {
        Send 'rwarmy status far'
        Start-Sleep -Seconds 8
        $all = [regex]::Matches((Log), 'RWARMY order far state=(\w+)[^\r\n]*')
        if ($all.Count -gt 0) {
            $last = $all[$all.Count - 1].Value
            Write-Host ("  " + $last.Substring(0, [Math]::Min(260, $last.Length)))
            $state = $all[$all.Count - 1].Groups[1].Value
            if ($state -eq 'ARRIVED' -or $state -eq 'BLOCKED') { break }
        }
    }
    Assert ($state -eq 'ARRIVED') "march reached the far target ($last)"
    # M11.11e: mid-march (10+ legs, nobody arrived yet) the army walks in lanes, as wide as it stood, not a file.
    $mid = @([regex]::Matches((Log), 'RWARMY order far state=MOVING[^\r\n]* arrived=0 [^\r\n]* legs=(\d+) [^\r\n]* spread=(\d+)') |
        Where-Object { [int]$_.Groups[1].Value -ge 10 })
    Assert ($mid.Count -gt 0) 'a status sample in the middle of the march'
    $widths = @($mid | ForEach-Object { [int]$_.Groups[2].Value })
    Assert (($widths | Measure-Object -Minimum).Minimum -ge 30) ("mid-march width stays at least 30 blocks (" + ($widths -join ',') + ")")
    $arrived = [int][regex]::Match($last, 'arrived=(\d+)').Groups[1].Value
    Assert ($arrived -ge 98) "at least 98 of 100 hold their slot ($arrived)"
    $text = Log
    Assert ($text -match 'RWARMY tickets far chunks=\d+') 'army tickets held the march area'
    Assert ($text.Contains('RWARMY freeforall-slots far grounded at goal')) 'slots placed on real ground once the far goal loaded'
    $legs = [regex]::Matches($text, 'RWARMY freeforall-leg far ').Count
    Assert ($legs -ge 24) "route built in bounded legs through unloaded terrain ($legs)"
    $after = @(Get-ChildItem -LiteralPath $region -Filter '*.mca' | ForEach-Object Name)
    Assert ($after -contains 'r.0.1.mca') 'target region generated by the march'
    Stop-Phase 'p2-far'
    Unchanged
    @{success=$true; package='M11.11b'; checks=@($checks); logs=@(Get-ChildItem -Path "$prefix-*.log" | Select-Object -ExpandProperty Name);
      inputs=$hashes; harness=(Get-FileHash -LiteralPath $PSCommandPath).Hash; arrived=$arrived; legs=$legs; status=$last;
      condition='dev dedicated server, flat seed, no player; 100 members scattered over 61x41 blocks; goal 0,4,600 in never generated terrain, freeforall legion speed 3 passive; spawn moved to 5000,5000 and save-all after the order; RwArmyMaxMarches default 2; NpcNavRange=32; no diagnostic chunks; no teleport'} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M1111b evidence: $prefix.json"
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
