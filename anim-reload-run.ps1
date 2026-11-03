# P7c controller scenario: /npcanim reload on a real dev client with the pinned customnpcentities 0.7.0
# in an isolated run directory (RwAnimHooks=1 in its config copy), against an integrated or a dedicated
# dev server. Client and server share the run directory, so the harness writes the override files where
# the server reads them; a client only ever gets them over the rwanim channel.
param([ValidateSet('integrated','dedicated')][string]$Case='dedicated',
      [string]$EntitiesJar=(Join-Path (Split-Path $PSScriptRoot -Parent) 'customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar'))
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/p7c-reload-$Case-$stamp"
$work = Join-Path $root "dev/run-p7c/$Case-$stamp"
$world = 'p7c-world'
$player = 'P7C_Player'
$dedicated = $Case -eq 'dedicated'
$server = $null; $client = $null; $lock = $null; $exitCode = 1; $serverLog = $null
$hashes = @{}; $owned = @{}
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$bad = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|Exception ticking|Encountered an unexpected exception|MissingModsException|DuplicateModsFound|NI uspel|RWANIM pose blending stopped|RWANIM incompatible|NPCANIM reload failed|NPCANIM could not|NPCANIM client could not'

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
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
function Unchanged { foreach ($path in $hashes.Keys) { Assert ((Fingerprint $path) -ceq $hashes[$path]) "unchanged $path" } }
function Send([string]$Text) { $server.StandardInput.WriteLine($Text); $server.StandardInput.Flush() }
function Wait-Marker([string]$Marker, [int]$Seconds) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Read-JavaPrototypeLog $serverLog
        if ($text.Contains($Marker)) { return }
        if ($server.HasExited -or $text -cmatch $bad) { throw "Server failed before $Marker; $serverLog" }
        Guard; Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw "Timeout waiting for $Marker; $serverLog"
}
function Wait-Exit($Proc, [string]$Label, [int]$Seconds) {
    $until = (Get-Date).AddSeconds($Seconds)
    while (-not $Proc.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 500 }
    Assert $Proc.HasExited "$Label completed before timeout"
    [M61ProcessLog]::Finish($Proc)
    Assert ($Proc.ExitCode -eq 0) "$Label clean process exit"
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "p7c-$Case"
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $protected = @($EntitiesJar, (Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'), (Join-Path $run 'server.properties'), (Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert ($hashes[$EntitiesJar] -ceq '0DCDEC55A33BB03C2740D702C59AF8FC0039901CB9FB58D6215DAFA6BBD62149') 'pinned entities 0.7.0 jar'
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Copy-Item -LiteralPath (Join-Path $run 'eula.txt') -Destination $work
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'config/customnpcs/animations'))) 'no override files in the config copy'
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $cfgText = [IO.File]::ReadAllText($cfg)
    Assert (-not ($cfgText -match '(?m)^RwAnimHooks=(?!0\r?$)')) 'config copy has no animation hook switch'
    $cfgText = [regex]::Replace($cfgText, '(?m)^RwAnimHooks=0\r?\n?', '')
    $cfgText += "`nRwAnimHooks=1`n"
    [IO.File]::WriteAllText($cfg, $cfgText, (New-Object Text.UTF8Encoding $false))
    $mods = Join-Path $work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
    Copy-Item -LiteralPath $EntitiesJar -Destination $mods
    $port = 0
    if ($dedicated) {
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
        [IO.File]::WriteAllText((Join-Path $work 'ops.json'), ('[{"uuid":"' + $uuid + '","name":"' + $player + '","level":4,"bypassesPlayerLimit":false}]'), (New-Object Text.UTF8Encoding $false))
        $serverLog = "$prefix-server.log"
        $server = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runServer', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwWorld=$world") (Join-Path $root 'dev') $serverLog
        Wait-Marker 'Done (' 300
    }
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $lines = @('animReload=1', "world=$world", ('out=' + $out.Replace('\', '/')))
    if ($dedicated) { $lines += "connect=$port" }
    [IO.File]::WriteAllLines($job, $lines, (New-Object Text.UTF8Encoding $false))
    $clientLog = "$prefix-client.log"
    $client = Start-JavaPrototypeProcess (Join-Path $root 'dev/gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player") (Join-Path $root 'dev') $clientLog
    Wait-Exit $client 'client' 1200
    $client = $null
    if ($dedicated) {
        Send 'stop'
        Wait-Exit $server 'server' 180
        $server = $null
    }
    $text = Read-JavaPrototypeLog $clientLog
    Assert ($text.Contains('P7C DONE success=true')) 'client success marker'
    Assert ($text.Contains('poseg v dev.jakak.customnpcentities.entity.EntityAnimatedNpc uspel')) 'client entity class instrumented at load'
    Assert (-not ($text -cmatch $bad)) 'no linkage, ticking or patch errors on the client'
    Assert ($text.Contains('NPCANIM client refused revision')) 'the refusal really happened on the client'
    if ($dedicated) {
        $serverText = Read-JavaPrototypeLog $serverLog
        Assert ($serverText.Contains('poseg v dev.jakak.customnpcentities.entity.EntityAnimatedNpc uspel')) 'server entity class instrumented at load'
        Assert (-not ($serverText -cmatch $bad)) 'no linkage, ticking or patch errors on the server'
        Assert ($serverText.Contains("NPCANIM disconnecting $player")) 'the late-join refusal really disconnected the player'
    }
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true) 'proof success'
    $keys = @('classes-patched','jar-baseline','client-not-reloaded','status-initial',
        'r1-commit','r1-client-revision','r1-existing-action-stopped','r1-client-library','r1-client-sample','r1-jar-cache-untouched','r1-rendered','r1-server-library','r1-new-clip-index','r1-server-event',
        'invalid-reports-path','invalid-keeps-revision','invalid-keeps-action','traversal-refused','unknown-refused','invalid-status',
        'refusal-aborts','refusal-status','refusal-server-unchanged',
        'r2-commit','r2-client-library','r2-removed-on-server','r2-index-moved','spawn-same-snapshot','spawn-server-library','repeat-same-hash',
        'cleanup-commit','cleanup-client-jar','cleanup-server-jar')
    if ($dedicated) { $keys += @('disconnect-clears','late-join-refusal-disconnects','late-join-state','late-join-library','late-join-index','retrack-index') }
    else { $keys += 'r1-sides-separate' }
    $rows = @($proof.checks)
    Assert ($rows.Count -eq $keys.Count) "exactly $($keys.Count) checks"
    Assert (@($rows | Where-Object { $_.ok -ne $true }).Count -eq 0) 'all checks passed'
    Assert ((@($rows.key | Sort-Object) -join '|') -ceq (@($keys | Sort-Object) -join '|')) 'exact check key set'
    Assert (@($proof.screenshots).Count -eq 2) 'two screenshots'
    foreach ($image in $proof.screenshots) {
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'config/customnpcs/animations/cave_bear.animation.json'))) 'override file removed by cleanup'
    Unchanged
    @{success = $true; package = 'P7c'; case = $Case; proof = $out; clientLog = $clientLog; serverLog = $serverLog; inputs = $hashes; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; data = $proof.data;
      condition = 'one dev client (MCP runClient, isolated run dir) with customnpcentities 0.7.0 pinned jar in mods, classes instrumented in memory at load, RwAnimHooks=1 in the config copy; integrated or dev dedicated server (runServer); server read only through /npcanim and /cne; no packaged client'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "P7C evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client) { try { Stop-JavaPrototypeProcess $client } catch { Write-Host "Cleanup failure: $_"; $exitCode = 1 } }
    if ($null -ne $server) { try { if (-not $server.HasExited) { Send 'stop'; if (-not $server.WaitForExit(30000)) { Stop-JavaPrototypeProcess $server } }; Stop-JavaPrototypeProcess $server } catch { Write-Host "Cleanup failure: $_"; $exitCode = 1 } }
    foreach ($id in @($owned.Keys)) { if (Get-Process -Id $id -ErrorAction SilentlyContinue) { & taskkill.exe /PID $id /T /F 2>&1 | Out-Null; $exitCode = 1 } }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
