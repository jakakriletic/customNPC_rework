# M16.7b: a creature pack clone.json in the readable clone format, written by the server itself,
# loads on reload and at startup, spawns with its edited values; an unknown readable version is refused.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$dev = Join-Path $root 'dev'
$seed = Join-Path $dev 'run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$audit = Join-Path $root "audit/m167-creature-readable-$stamp"
$work = Join-Path $dev "run-m167/$stamp"
$world = 'm167-world'
$jobs = @{}
$checks = New-Object Collections.ArrayList
$lock = $null
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false

function Check([bool]$ok, [string]$label) {
    if (-not $ok) { throw $label }
    [void]$checks.Add($label)
    Write-Host "OK $label"
}
function Write-Text([string]$file, [string]$value) { [IO.File]::WriteAllText($file, $value, $utf8) }
function Hash([string]$file) { (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
function Guard {
    $foreign = @(Get-NavForeignJava -Roots @($jobs.Values | ForEach-Object { $_.Process.Id }))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Wait-Log([string]$jobName, [string]$match, [int]$seconds = 240) {
    $job = $jobs[$jobName]
    $until = (Get-Date).AddSeconds($seconds)
    do {
        $log = Read-JavaPrototypeLog $job.Log
        if ($log.Contains($match)) { Check $true "$jobName $match"; return $log }
        if ($job.Process.HasExited -or $log -match 'Exception ticking|unexpected exception|NoClassDefFoundError|Invalid creature ') {
            throw "$jobName failed before $match. See $($job.Log)"
        }
        Guard
        Start-Sleep -Milliseconds 400
    } while ((Get-Date) -lt $until)
    throw "$jobName timed out waiting for $match. See $($job.Log)"
}
function Start-Server([string]$jobName) {
    $logPath = "$audit-$jobName.log"
    $process = Start-JavaPrototypeProcess (Join-Path $dev 'gradlew.bat') @('runServer', '--offline', '--no-daemon', '--console=plain',
        "-PrwRunDir=$work", "-PrwWorld=$world") $dev $logPath
    $jobs[$jobName] = @{ Process = $process; Log = $logPath }
    [void](Wait-Log $jobName 'Done (')
}
function Stop-Server([string]$jobName) {
    Send $jobName 'save-all flush'
    [void](Wait-Log $jobName 'Saved the world')
    Send $jobName 'stop'
    $job = $jobs[$jobName]
    $until = (Get-Date).AddSeconds(180)
    while (-not $job.Process.HasExited -and (Get-Date) -lt $until) { Start-Sleep -Milliseconds 400 }
    Check $job.Process.HasExited "$jobName exited"
    [M61ProcessLog]::Finish($job.Process)
    Check ($job.Process.ExitCode -eq 0) "$jobName clean process exit"
    $jobs.Remove($jobName)
}
function Send([string]$jobName, [string]$command) {
    $proc = $jobs[$jobName].Process
    $proc.StandardInput.WriteLine($command)
    $proc.StandardInput.Flush()
}
function Keep-Ticking([string]$jobName) {
    # Without a player the server stops entity updates after 300 ticks; forced chunks keep the
    # pack script ticking so its init line prints (Q308, Q486).
    Send $jobName 'rwdiag chunks on 1'
    [void](Wait-Log $jobName 'RWDIAG-CHUNKS stanje=on')
}
function New-Pack([string]$dir, [string]$clone) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    Write-Text (Join-Path $dir 'creature.json') '{"base":"troll"}'
    Write-Text (Join-Path $dir 'animations.json') '{"animations":{}}'
    # The pack script replaces clone script tabs; it prints what the spawned NPC really got.
    Write-Text (Join-Path $dir 'script.js') @'
function init(e) {
    var System = Java.type('java.lang.System');
    System.out.println('M167 PACK ' + e.npc.getName() + ' max=' + e.npc.getStats().getMaxHealth() + ' x=' + Math.floor(e.npc.getX()));
}
'@
    Write-Text (Join-Path $dir 'clone.json') $clone
}
function Replace-Once([string]$text, [string]$pattern, [string]$replacement, [string]$label) {
    $found = [regex]::Matches($text, $pattern).Count
    Check ($found -eq 1) "readable source has exactly one $label ($found)"
    return [regex]::Replace($text, $pattern, $replacement)
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm167-creature-readable'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $seed 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $seed 'eula.txt') -Destination $work
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^(RwAnimCore|RwReadableClones)=.*\r?\n?', '')
    Write-Text $cfg ($text + "`nRwAnimCore=1`nRwReadableClones=1`n")
    $props = [IO.File]::ReadAllText((Join-Path $seed 'server.properties'))
    Check ($props -match '(?m)^online-mode=false\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'isolated flat test world'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port" -replace '(?m)^server-ip=.*$', 'server-ip=127.0.0.1' -replace '(?m)^level-name=.*$', "level-name=$world"
    Write-Text (Join-Path $work 'server.properties') $props

    $creatures = Join-Path $work 'customnpcs/creatures'
    $legacy = [IO.File]::ReadAllText((Join-Path $dev 'testworld/customnpcs/clones/1/M36_Stand.json'))
    $legacy = $legacy.Replace('"MaxHealth": 20', '"MaxHealth": 37').Replace('"Name": "M36_Stand"', '"Name": "LegacyBeetle"')
    Check (-not $legacy.Contains('customnpcs-readable-clone') -and $legacy.Contains('"MaxHealth": 37') -and $legacy.Contains('"Name": "LegacyBeetle"')) 'legacy control clone prepared'
    New-Pack (Join-Path $creatures 'legacy_beetle') $legacy

    Start-Server 'server'
    Send 'server' 'setworldspawn 0 4 0'
    Send 'server' 'gamerule spawnRadius 0'
    Send 'server' 'gamerule doMobSpawning false'
    Send 'server' 'rwcreature spawn legacy_beetle 0 4 3'
    [void](Wait-Log 'server' 'RWRIG spawned LegacyBeetle (legacy_beetle).')
    Keep-Ticking 'server'
    [void](Wait-Log 'server' 'M167 PACK LegacyBeetle max=37 x=0')

    # The server writes the readable clone itself (RwReadableClones=1), so the pack uses the real format.
    Send 'server' 'noppes clone add LegacyBeetle 1 RB_Source'
    $source = Join-Path $work "$world/customnpcs/clones/1/RB_Source.json"
    $until = (Get-Date).AddSeconds(60)
    while (-not (Test-Path -LiteralPath $source) -and (Get-Date) -lt $until) { Guard; Start-Sleep -Milliseconds 400 }
    Start-Sleep -Seconds 1
    Check (Test-Path -LiteralPath $source) 'server wrote the clone file'
    $readable = [IO.File]::ReadAllText($source, [Text.Encoding]::UTF8)
    Check ($readable -match '"\$format"\s*:\s*"customnpcs-readable-clone"' -and $readable -match '"version"\s*:\s*1\b') 'server clone is readable format version 1'
    $readable = Replace-Once $readable '("name"\s*:\s*)"LegacyBeetle"' '$1"ReadableBeetle"' 'name LegacyBeetle'
    $readable = Replace-Once $readable '("maxHealth"\s*:\s*)37\b' '${1}41' 'maxHealth 37'
    $readablePack = Join-Path $creatures 'readable_beetle'
    New-Pack $readablePack $readable
    $readableHash = Hash (Join-Path $readablePack 'clone.json')
    $badPack = Join-Path $creatures 'bad_version'
    New-Pack $badPack '{"$format":"customnpcs-readable-clone","version":99}'

    Send 'server' 'rwcreature reload'
    $log = Wait-Log 'server' 'Creature reload refused: RWRIG creature bad_version:'
    Check (-not $log.Contains('Creature reload complete')) 'unknown readable version refuses the whole reload, no legacy fallback'
    Remove-Item -LiteralPath $badPack -Recurse -Force
    Send 'server' 'rwcreature reload'
    [void](Wait-Log 'server' 'Creature reload complete: 2 packs, revision')
    Send 'server' 'rwcreature spawn readable_beetle 2 4 3'
    [void](Wait-Log 'server' 'RWRIG spawned ReadableBeetle (readable_beetle).')
    [void](Wait-Log 'server' 'M167 PACK ReadableBeetle max=41 x=2')
    Stop-Server 'server'
    Check ((Hash (Join-Path $readablePack 'clone.json')) -ceq $readableHash) 'reload and spawn never rewrite the pack clone.json'

    Start-Server 'restarted'
    $log = Read-JavaPrototypeLog $jobs['restarted'].Log
    Check (-not $log.Contains('RWRIG creature ')) 'both packs load at startup without errors'
    Send 'restarted' 'rwcreature spawn readable_beetle 4 4 3'
    [void](Wait-Log 'restarted' 'RWRIG spawned ReadableBeetle (readable_beetle).')
    Keep-Ticking 'restarted'
    [void](Wait-Log 'restarted' 'M167 PACK ReadableBeetle max=41 x=4')
    Send 'restarted' 'rwcreature spawn legacy_beetle 6 4 3'
    [void](Wait-Log 'restarted' 'M167 PACK LegacyBeetle max=37 x=6')
    Stop-Server 'restarted'
    Check ((Hash (Join-Path $readablePack 'clone.json')) -ceq $readableHash) 'restart never rewrites the pack clone.json'
    [ordered]@{ result = 'PASS'; checks = @($checks); readableHash = $readableHash } |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$audit.json" -Encoding UTF8
    Write-Host "PASS M16.7b: $audit ($($checks.Count) checks)"
} finally {
    foreach ($job in @($jobs.Values)) { Stop-JavaPrototypeProcess $job.Process }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
    if ($null -ne $lock) { $lock.Dispose() }
}
