param(
    [Parameter(Mandatory = $true)][string]$ForgeDirectory,
    [Parameter(Mandatory = $true)][string]$Bundle
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$work = Join-Path $root "dev/run-varied-bundle/$stamp"
$log = Join-Path $root "audit/varied-bundle-$stamp.log"
$server = $null; $lock = $null

function Wait-Log([string]$marker) {
    $until = (Get-Date).AddSeconds(140)
    do {
        $text = Read-JavaPrototypeLog $log
        if ($text.Contains($marker)) { return }
        if ($server.HasExited -or $text -match 'NoClassDefFoundError|NoSuchMethodError|DuplicateModsFoundException|Encountered an unexpected exception') {
            throw "Bundled server failed before '$marker'. See $log"
        }
        Start-Sleep -Milliseconds 350
    } while ((Get-Date) -lt $until)
    throw "Missing '$marker' in $log"
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'varied-bundle'
    if (@(Get-NavForeignJava).Count) { throw 'A Minecraft or Gradle workload is already running.' }
    $forge = Get-ChildItem -LiteralPath $ForgeDirectory -Filter 'forge-1.12.2-*.jar' -File | Sort-Object Name -Descending | Select-Object -First 1
    if (!$forge) { throw 'Forge 1.12.2 server jar is missing.' }
    New-Item -ItemType Directory -Path (Join-Path $work 'mods'), (Join-Path $work 'config') -Force | Out-Null
    New-Item -ItemType Junction -Path (Join-Path $work 'libraries') -Target (Join-Path $forge.DirectoryName 'libraries') | Out-Null
    Copy-Item -LiteralPath @($forge.FullName, (Join-Path $forge.DirectoryName 'minecraft_server.1.12.2.jar'),
        (Join-Path $root 'dev/run/eula.txt')) -Destination $work
    Copy-Item -LiteralPath $Bundle -Destination (Join-Path $work 'mods')
    [IO.File]::WriteAllText((Join-Path $work 'config/CustomNpcs.cfg'), "RwCommandStaff=1`n")
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    [IO.File]::WriteAllLines((Join-Path $work 'server.properties'), @('online-mode=false', 'server-ip=127.0.0.1',
        "server-port=$port", 'level-name=proof', 'level-type=FLAT', 'level-seed=customnpc-rework', 'view-distance=10'))
    $server = Start-JavaPrototypeProcess (Join-Path $root '.tools/jdk8/bin/java.exe') @('-Xmx2G', '-jar', $forge.Name, 'nogui') $work $log
    Wait-Log 'Done ('
    $modLog = Read-JavaPrototypeLog (Join-Path $work 'logs/debug.log')
    foreach ($mod in 'noppes.npcs.CustomNpcs', 'noppes.vc.VariedCommodities') {
        if (!$modLog.Contains("($mod) - loading")) { throw "$mod was not discovered. See $log" }
    }
    $server.StandardInput.WriteLine('setworldspawn 0 4 0')
    $server.StandardInput.WriteLine('gamerule spawnRadius 0')
    $server.StandardInput.WriteLine('setblock 0 4 0 variedcommodities:crate'); $server.StandardInput.Flush()
    Wait-Log 'Block placed'
    foreach ($item in 'variedcommodities:ancient_coin', 'customnpcs:command_staff') {
        $server.StandardInput.WriteLine(('summon minecraft:item 0 4 0 {{Item:{{id:"{0}",Count:1b}}}}' -f $item))
        $server.StandardInput.WriteLine(('testfor @e[type=item] {{Item:{{id:"{0}"}}}}' -f $item))
    }
    $server.StandardInput.WriteLine('save-all flush'); $server.StandardInput.WriteLine('stop'); $server.StandardInput.Flush()
    if (!$server.WaitForExit(90000)) { throw "Server did not exit. See $log" }
    [M61ProcessLog]::Finish($server)
    $text = Read-JavaPrototypeLog $log
    if ($server.ExitCode -ne 0 -or [regex]::Matches($text, 'Found item\.').Count -lt 2) { throw "Item registration failed. See $log" }
    Write-Output "PASS one JAR, both mods and commodity block/items loaded. $log"
} finally {
    if ($null -ne $server) { Stop-JavaPrototypeProcess $server }
    if ($null -ne $lock) { $lock.Dispose() }
}
