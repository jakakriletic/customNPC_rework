# M15: one dedicated server, two isolated clients, live reload and a fresh server process.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$dev = Join-Path $root 'dev'
$seed = Join-Path $dev 'run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$audit = Join-Path $root "audit/m15-creature-$stamp"
$work = Join-Path $dev "run-m15/$stamp"
$serverDir = Join-Path $work 'server'
$world = 'm15-world'
$jobs = @{}
$lock = $null
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false

function Check([bool]$ok, [string]$label) {
    if (-not $ok) { throw $label }
    Write-Host "OK $label"
}
function Write-Text([string]$file, [string]$value) { [IO.File]::WriteAllText($file, $value, $utf8) }
function Guard {
    $foreign = @(Get-NavForeignJava -Roots @($jobs.Values | ForEach-Object { $_.Process.Id }))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Wait-Log([string]$jobName, [string]$match, [int]$seconds = 240) {
    $job = $jobs[$jobName]
    $until = (Get-Date).AddSeconds($seconds)
    do {
        $log = Read-JavaPrototypeLog $job.Log
        if ($log.Contains($match)) { Check $true "$jobName $match"; return }
        if ($job.Process.HasExited -or $log -match 'Exception ticking|unexpected exception|NoClassDefFoundError|M15 CLIENT DONE .* false|Invalid creature ') {
            throw "$jobName failed before $match. See $($job.Log)"
        }
        Guard
        Start-Sleep -Milliseconds 400
    } while ((Get-Date) -lt $until)
    throw "$jobName timed out waiting for $match. See $($job.Log)"
}
function Start-JobProcess([string]$jobName, [string[]]$gradleArgs) {
    $logPath = "$audit-$jobName.log"
    $process = Start-JavaPrototypeProcess (Join-Path $dev 'gradlew.bat') $gradleArgs $dev $logPath
    $jobs[$jobName] = @{ Process = $process; Log = $logPath }
}
function Finish-JobProcess([string]$jobName) {
    $job = $jobs[$jobName]
    $until = (Get-Date).AddSeconds(180)
    while (-not $job.Process.HasExited -and (Get-Date) -lt $until) { Start-Sleep -Milliseconds 400 }
    Check $job.Process.HasExited "$jobName exited"
    [M61ProcessLog]::Finish($job.Process)
    Check ($job.Process.ExitCode -eq 0) "$jobName clean process exit"
    $jobs.Remove($jobName)
}
function Init-Run([string]$dir) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $seed 'config') -Destination (Join-Path $dir 'config') -Recurse
    $cfg = Join-Path $dir 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwAnimCore=.*\r?\n?', '')
    Write-Text $cfg ($text + "`nRwAnimCore=1`n")
}
function Start-Server([string]$jobName) {
    Start-JobProcess $jobName @('runServer', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$serverDir", "-PrwWorld=$world")
    Wait-Log $jobName 'Done ('
}
function Send([string]$jobName, [string]$command) {
    $proc = $jobs[$jobName].Process
    $proc.StandardInput.WriteLine($command)
    $proc.StandardInput.Flush()
}
function Start-Client([string]$name, [string]$player) {
    $dir = Join-Path $work $name
    Init-Run $dir
    $job = "$audit-$name.job"
    Write-Text $job ("creatures=1`nmode=$name`nconnect=$port`n")
    Start-JobProcess $name @('runClient', '--offline', '--no-daemon', '--console=plain', "-PrwRunDir=$dir",
        "-PrwHarness=$($job.Replace('\', '/'))", "-PrwUsername=$player")
    Wait-Log $name "M15 CLIENT READY $name"
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm15-creature'
    Guard
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Init-Run $serverDir
    Copy-Item -LiteralPath (Join-Path $seed 'eula.txt') -Destination $serverDir
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
    $props = [IO.File]::ReadAllText((Join-Path $seed 'server.properties'))
    Check ($props -match '(?m)^online-mode=false\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'isolated flat test world'
    $props = $props -replace '(?m)^server-port=.*$', "server-port=$port" -replace '(?m)^server-ip=.*$', 'server-ip=127.0.0.1' -replace '(?m)^level-name=.*$', "level-name=$world"
    Write-Text (Join-Path $serverDir 'server.properties') $props
    $firstPlayer = 'M15_First'
    $md5 = [Security.Cryptography.MD5]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$firstPlayer"))
    $md5[6] = ($md5[6] -band 0x0f) -bor 0x30
    $md5[8] = ($md5[8] -band 0x3f) -bor 0x80
    $hex = ($md5 | ForEach-Object { $_.ToString('x2') }) -join ''
    $uuid = $hex.Substring(0,8)+'-'+$hex.Substring(8,4)+'-'+$hex.Substring(12,4)+'-'+$hex.Substring(16,4)+'-'+$hex.Substring(20)
    Write-Text (Join-Path $serverDir 'ops.json') ('[{"uuid":"'+$uuid+'","name":"'+$firstPlayer+'","level":4,"bypassesPlayerLimit":false}]')
    $creatures = Join-Path $serverDir 'customnpcs/creatures'
    $troll = Join-Path $creatures 'epic_troll'
    $beetle = Join-Path $creatures 'copper_beetle'
    New-Item -ItemType Directory -Path $troll, $beetle -Force | Out-Null
    Write-Text (Join-Path $troll 'creature.json') '{"base":"troll","name":"Epic Troll"}'
    Write-Text (Join-Path $troll 'script.js') @'
function init(e) { e.npc.getDisplay().setTitle('Folder script ran'); e.npc.updateClient(); }
function animationEvent(e) {
    if (e.type == 'sound') { e.npc.getDisplay().setTitle('Folder event ran'); e.npc.updateClient(); }
}
'@
    $trollClips = Join-Path $troll 'animations.json'
    $firstClips = '{"animations":{"m15_roar":{"animation_length":4,"events":[{"t":0.5,"type":"sound","sound":"troll.roar"}]}}}'
    Write-Text $trollClips $firstClips
    Write-Text (Join-Path $beetle 'creature.json') '{"name":"Copper Beetle"}'
    $clone = [IO.File]::ReadAllText((Join-Path $dev 'testworld/customnpcs/clones/1/M36_Stand.json'))
    Write-Text (Join-Path $beetle 'clone.json') ($clone.Replace('"MaxHealth": 20', '"MaxHealth": 37'))
    Write-Text (Join-Path $beetle 'animations.json') '{"animations":{"idle":{"animation_length":1,"loop":true}}}'
    $assets = Join-Path $dev 'src/patch/resources/assets/customnpcs'
    Copy-Item -LiteralPath (Join-Path $assets 'rig/giant_spider.geo.json') -Destination (Join-Path $beetle 'model.geo.json')
    Copy-Item -LiteralPath (Join-Path $assets 'textures/rig/giant_spider.png') -Destination (Join-Path $beetle 'texture.png')
    # Reserve an invalid pack at startup to prove healthy packs still load.
    $broken = Join-Path $creatures 'broken'
    New-Item -ItemType Directory -Path $broken | Out-Null
    Write-Text (Join-Path $broken 'creature.json') '{"base":"troll"}'
    Start-Server 'server'
    Wait-Log 'server' 'creature broken: missing file animations.json'
    Rename-Item -LiteralPath $broken -NewName '_disabled'
    Rename-Item -LiteralPath (Join-Path $creatures '_disabled/creature.json') -NewName 'disabled.json'
    Send 'server' 'setworldspawn 0 4 0'
    Send 'server' 'gamerule spawnRadius 0'
    Send 'server' 'gamerule doMobSpawning false'
    Send 'server' 'rwcreature spawn epic_troll 0 4 5'
    Wait-Log 'server' 'RWRIG spawned Epic Troll (epic_troll)'
    Send 'server' 'rwcreature spawn copper_beetle 0 4 3'
    Wait-Log 'server' 'RWRIG spawned Copper Beetle (copper_beetle)'
    Start-Client 'first' $firstPlayer
    Start-Client 'late' 'M15_Late'
    Write-Text $trollClips '{"animations":[]}'
    Send 'server' 'rwcreature reload'
    Wait-Log 'server' 'animations.json must contain an animations object'
    Send 'server' 'rwrig info Epic'
    Wait-Log 'server' 'clips=backhand,death,hurt,idle,m15_roar'
    $updatedClips = '{"animations":{"m15_roar":{"animation_length":4},"m15_updated":{"animation_length":15,"events":[{"t":0.5,"type":"sound","sound":"troll.roar"}]}}}'
    Write-Text $trollClips $updatedClips
    Send 'server' 'rwcreature reload'
    Wait-Log 'server' 'Creature reload complete: 2 packs, revision 2.'
    Send 'server' 'rwrig play Epic m15_updated'
    Wait-Log 'server' 'RWRIG play ok=1/1'
    Wait-Log 'first' 'M15 CLIENT RELOAD first'
    Wait-Log 'late' 'M15 CLIENT RELOAD late'
    $fresh = Join-Path $creatures 'fresh_troll'
    New-Item -ItemType Directory -Path $fresh | Out-Null
    Write-Text (Join-Path $fresh 'creature.json') '{"base":"troll","name":"Fresh Troll"}'
    Write-Text (Join-Path $fresh 'animations.json') '{"animations":{}}'
    Send 'server' 'rwcreature deploy fresh_troll 2 4 5'
    Wait-Log 'server' 'RWRIG spawned Fresh Troll (fresh_troll)'
    Wait-Log 'late' 'M15 CLIENT DONE late true'
    Finish-JobProcess 'late'
    Send 'server' "replaceitem entity $firstPlayer slot.hotbar.0 customnpcs:npcwand"
    Wait-Log 'first' 'M15 CLIENT GUI first'
    Wait-Log 'first' 'M15 CLIENT DONE first true'
    Finish-JobProcess 'first'
    Send 'server' 'rwrig set Copper copper_beetle'
    Wait-Log 'server' 'RWRIG set ok=1/1'
    Send 'server' 'save-all flush'
    Wait-Log 'server' 'Saved the world'
    Send 'server' 'stop'
    Finish-JobProcess 'server'
    Start-Server 'restarted'
    Start-Client 'restart' 'M15_Restart'
    Wait-Log 'restart' 'M15 CLIENT DONE restart true'
    Finish-JobProcess 'restart'
    Send 'restarted' 'stop'
    Finish-JobProcess 'restarted'
    Write-Host "PASS M15: $audit"
} finally {
    foreach ($job in @($jobs.Values)) { Stop-JavaPrototypeProcess $job.Process }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
    if ($null -ne $lock) { $lock.Dispose() }
}
