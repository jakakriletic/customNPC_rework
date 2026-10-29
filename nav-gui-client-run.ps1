# M4.15c controller scenario. Two sequential client JVMs; real save/restart.
# Dedicated mode deliberately uses server library=1, client library=0.
param([ValidateSet('integrated', 'dedicated')][string]$Mode = 'integrated',
      [ValidateSet(0,1)][int]$Library = 1, [string]$BaritoneRoot = '')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'nav-gui-properties-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwnavgui-client-$Mode-$Library-$stamp"
$prefix = Join-Path $root "audit/m415c-$Mode-$Library-$stamp"
$serverLibrary = if ($Mode -eq 'dedicated') { 1 } else { $Library }
$clientLibrary = if ($Mode -eq 'dedicated') { 0 } else { $Library }
$server = $null
$client = $null
$lock = $null
$propertiesBytes = $null
$propertiesPath = Join-Path $run 'server.properties'
$exitCode = 1
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$oldMod = $env:NPCB_MOD_DIR
$proofs = @()
$configHashes = @{}
$optionsHash = $null

function Get-OptionsFingerprint {
    $path = Join-Path $run 'options.txt'
    # A fresh client legitimately uses defaults without this file (GameSettings.loadOptions).
    # Record absence explicitly; creation, deletion or byte changes must still fail.
    if (-not (Test-Path -LiteralPath $path)) { return 'absent' }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'options.txt is not a regular file' }
    return ('sha256:' + (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash)
}

function Read-Live([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    $stream = [IO.File]::Open($Path, 'Open', 'Read', 'ReadWrite')
    $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8, $true)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Send([string]$Text) {
    $server.Proc.StandardInput.WriteLine($Text)
    $server.Proc.StandardInput.Flush()
}
function Send-Setup([string]$Name, [string]$Text) {
    if ($Name -cnotmatch '^[A-Za-z0-9_]+$') { throw 'Invalid setup player name' }
    # Vanilla execute retains console permissions through CommandSenderWrapper.
    # Unlike a raw console sender, its feedback does not lazily persist the missing
    # broadcast-console-to-ops property. Coordinates/targets and game rules stay identical.
    Send "execute $Name ~ ~ ~ $Text"
}
function Guard {
    $owned = @()
    if ($null -ne $server) { $owned += $server.Proc.Id }
    if ($null -ne $client) { $owned += $client.Id }
    $foreign = @(Get-NavForeignJava -Roots $owned)
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Wait-Server([string]$Marker, [int]$Seconds = 120) {
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Read-Live $server.Log
        if ($text.Contains($Marker)) { return }
        if ($server.Proc.HasExited -or (Get-ServerSesutje $text)) { throw "Server stopped before $Marker" }
        Guard
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw "Server timeout: $Marker"
}
function Start-Server([string]$Pass) {
    $log = "$prefix-$Pass-server.log"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain -PnpcBaritoneDev -PrwNavBackend=1 "-PrwWorld={0}" > "{1}" 2>&1' -f $world, $log)
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    $script:server = [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $log }
    Wait-Server 'Done (' 900
    Send 'list'
    Wait-Server 'players online'
    Assert ((Read-Live $log).Contains('Preparing level "' + $world + '"')) 'dedicated isolated world'
}
function Stop-Server {
    if ($null -eq $server -or $server.Proc.HasExited) { return }
    Send 'stop'
    if (-not $server.Proc.WaitForExit(180000)) { throw 'Dedicated server failed clean stop' }
    Assert ($server.Proc.ExitCode -eq 0) 'dedicated clean stop'
    [IO.File]::WriteAllBytes(($server.Log + '.properties-after'), [IO.File]::ReadAllBytes($propertiesPath))
    Restore-NavGuiProperties $propertiesPath $propertiesBytes
}
function Start-Client([string]$Pass) {
    $out = "$prefix-$Pass.json"
    $job = "$prefix-$Pass.job"
    $lines = @('navGui=1', "world=$world", "pass=$Pass", "serverLibrary=$serverLibrary", "clientLibrary=$clientLibrary", ('out=' + $out.Replace('\', '/')))
    if ($Mode -eq 'dedicated') { $lines += "connect=$port" }
    [IO.File]::WriteAllLines($job, $lines, (New-Object Text.UTF8Encoding $false))
    $optional = if ($clientLibrary -eq 1) { '-PnpcBaritoneDev' } else { '' }
    # For dedicated the local override disagrees with the server, proving server authority.
    $global = if ($Mode -eq 'dedicated') { 0 } else { 1 }
    $log = "$prefix-$Pass-client.log"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain {0} -PrwNavBackend={1} "-PrwHarness={2}" > "{3}" 2>&1' -f $optional, $global, $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $script:client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1200)
    $prepared = $false
    while (-not $client.HasExited -and (Get-Date) -lt $until) {
        $text = Read-Live $log
        if ($Mode -eq 'dedicated' -and -not $prepared) {
            $match = [regex]::Match($text, 'NAVGUI-CLIENT PLAYER ([A-Za-z0-9_]+)')
            if ($match.Success) {
                $name = $match.Groups[1].Value
                # Confined to this scenario's fresh world. Restore ops.json bytes in finally.
                Send-Setup $name "op $name"
                Send-Setup $name 'setworldspawn 0 4 0'
                Send-Setup $name "gamemode 1 $name"
                Send-Setup $name "tp $name 0 4 2 180 0"
                Send-Setup $name "replaceitem entity $name slot.hotbar.0 customnpcs:npcwand"
                Send-Setup $name 'gamerule doMobSpawning false'
                Send-Setup $name 'time set 1000'
                Start-Sleep -Seconds 5
                if ($Pass -eq 'edit') { Send 'noppes clone spawn NAVGUI_Click 1 0,4,0' }
                Send 'say NAVGUI-CLIENT-SCENE'
                $prepared = $true
            }
        }
        Guard
        Start-Sleep -Seconds 1
    }
    Assert $client.HasExited "client $Pass completed before timeout"
    Assert ($client.ExitCode -eq 0) "client $Pass clean process exit"
    Assert ((Get-OptionsFingerprint) -ceq $optionsHash) "client $Pass options existence and bytes unchanged"
    $text = Read-Live $log
    Assert ($text.Contains("NAVGUI-CLIENT DONE success=true pass=$Pass")) "client $Pass success marker"
    Assert ($text -notmatch 'script errored|ScriptException|NoClassDefFoundError|NAVGUI-CLIENT DONE success=false') "client $Pass no runtime errors"
    Assert (Test-Path -LiteralPath $out) "client $Pass proof exists"
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true -and $proof.pass -eq $Pass -and $proof.dedicated -eq ($Mode -eq 'dedicated')) "client $Pass proof identity"
    $expected = if ($Pass -eq 'edit') { 57 } else { if ($Mode -eq 'integrated') { 12 } else { 11 } }
    if ($Pass -eq 'edit' -and $Mode -eq 'integrated') { $expected++ }
    Assert (@($proof.assertions).Count -eq $expected) "client $Pass exactly $expected assertions"
    Assert (@($proof.assertions | Where-Object { $_.ok -ne $true }).Count -eq 0) "client $Pass all assertions passed"
    Assert (@($proof.assertions.key | Select-Object -Unique).Count -eq $expected) "client $Pass assertion keys unique"
    $expectedImages = if ($Pass -eq 'edit') { 9 } else { 1 }
    Assert (@($proof.screenshots).Count -eq $expectedImages) "client $Pass screenshots count"
    foreach ($image in $proof.screenshots) {
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    $script:proofs += $proof
    $script:client = $null
}

$opsPath = Join-Path $run 'ops.json'
$opsBytes = $null
$opsExisted = Test-Path -LiteralPath $opsPath
$opsCaptured = $false
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'nav-gui-client'
    Guard
    $mod = Get-NavModPath $root $BaritoneRoot
    $runtimeHash = Get-NavRuntimeHash $mod
    $env:NPCB_MOD_DIR = $mod
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($jar in @(Get-ChildItem -LiteralPath (Join-Path $run 'mods') -Recurse -Filter '*.jar')) {
        $zip = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try { Assert ($null -eq $zip.GetEntry('si/ladja/npcbaritone/api/NpcBaritone.class')) 'no installed optional mod masquerading as absent' }
        finally { $zip.Dispose() }
    }
    foreach ($name in @('config/CustomNpcs.cfg', 'config/npcbaritone.cfg', 'server.properties', 'eula.txt')) {
        $path = Join-Path $run $name
        $configHashes[$path] = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    }
    $optionsHash = Get-OptionsFingerprint
    Write-Host "OPTIONS initial=$optionsHash"
    if ($Mode -eq 'dedicated') {
        $propertiesBytes = [IO.File]::ReadAllBytes($propertiesPath)
        [IO.File]::WriteAllBytes("$prefix.properties-before", $propertiesBytes)
        $null = Get-NavGuiPropertiesContent $propertiesBytes
        $settings = [IO.File]::ReadAllText($propertiesPath)
        Assert ($settings -match '(?m)^level-type=FLAT' -and $settings -match '(?m)^level-seed=customnpc-rework') 'unchanged flat world properties'
        Assert ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match 'eula=true') 'existing real EULA acceptance'
        $m = [regex]::Match($settings, '(?m)^server-port=(\d+)')
        Assert $m.Success 'server port from existing properties'
        $port = [int]$m.Groups[1].Value
        if ($opsExisted) { $opsBytes = [IO.File]::ReadAllBytes($opsPath) }
        $opsCaptured = $true
        if ($opsExisted) { [IO.File]::WriteAllBytes("$prefix.ops-before", $opsBytes) }
        else { [IO.File]::WriteAllText("$prefix.ops-absent-before", 'absent') }
    }
    $worldPath = if ($Mode -eq 'integrated') { Join-Path $run "saves/$world" } else { Join-Path $run $world }
    Assert (-not (Test-Path -LiteralPath $worldPath)) 'fresh isolated world'
    $clones = Join-Path $worldPath 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    $fixture = Join-Path $clones 'NAVGUI_Click.json'
    Write-NavFixture (Join-Path $root 'dev/testworld/customnpcs/clones/1/LET_Walker.json') $fixture 'NAVGUI_Click' 5 0
    $text = [IO.File]::ReadAllText($fixture).Replace('"RwNavBackend": 1,', '"RwNavBackend": 0,')
    Assert ($text -match '"MovingState": 0') 'standing actor fixture'
    [IO.File]::WriteAllText($fixture, $text, (New-Object Text.UTF8Encoding $false))
    foreach ($pass in @('edit', 'verify')) {
        if ($Mode -eq 'dedicated') { Start-Server $pass }
        Start-Client $pass
        if ($Mode -eq 'dedicated') { Stop-Server; $server = $null }
        Assert (Test-Path -LiteralPath (Join-Path $worldPath 'level.dat')) "world persisted after $pass"
    }
    Assert ($proofs.Count -eq 2 -and $proofs[0].uuid -match '^[0-9a-f-]{36}$' -and $proofs[0].uuid -eq $proofs[1].uuid) 'same NPC UUID survives world/JVM restart'
    Assert ((Get-NavRuntimeHash $mod) -eq $runtimeHash) 'optional runtime unchanged'
    Assert ((Get-OptionsFingerprint) -ceq $optionsHash) 'options existence and bytes unchanged'
    foreach ($path in $configHashes.Keys) { Assert ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -eq $configHashes[$path]) "unchanged $([IO.Path]::GetFileName($path))" }
    $null = Write-MeritevJson -Path "$prefix.json" -Paket 'M4.15c' -Scenarij "$Mode-$Library" `
        -Odtis @{ runtime = $runtimeHash; harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; cnpcConfig = $configHashes[(Join-Path $run 'config/CustomNpcs.cfg')]; npcbConfig = $configHashes[(Join-Path $run 'config/npcbaritone.cfg')]; options = $optionsHash } `
        -Velicine @{ assertions = @($proofs[0].assertions).Count + @($proofs[1].assertions).Count; screenshots = 10; uuid = $proofs[0].uuid; serverLibrary = $serverLibrary; clientLibrary = $clientLibrary } -Uspeh $true -Padle @()
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    if ($null -ne $server -and -not $server.Proc.HasExited) {
        try { Stop-Server } catch { Write-Host "FAIL $_"; & taskkill.exe /PID $server.Proc.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    }
    if ($Mode -eq 'dedicated' -and $null -ne $propertiesBytes) {
        try {
            Restore-NavGuiProperties $propertiesPath $propertiesBytes
        } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    }
    # An unrelated properties failure must not prevent restoration of temporary op grants.
    if ($Mode -eq 'dedicated' -and $opsCaptured) {
        try {
            if ($opsExisted) { [IO.File]::WriteAllBytes($opsPath, $opsBytes) }
            elseif (Test-Path -LiteralPath $opsPath) { Remove-Item -LiteralPath $opsPath }
        } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
    $env:NPCB_MOD_DIR = $oldMod
}
exit $exitCode
