# M4.15a: actual eligibility, rider transitions and task rebuild, outside cost measurement.
param([string]$BaritoneRoot = '')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed = Join-Path $root 'dev\testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwnavc-$stamp"
$prefix = Join-Path $audit "m415a-compat-$stamp"
$srv = $null
$lock = $null
$failed = @()
$exitCode = 1

function Check([string]$Name, [bool]$Ok) {
    if ($Ok) { Write-Host "OK $Name" } else { Write-Host "FAIL $Name"; $script:failed += $Name }
}
function Get-Log {
    if ($null -eq $srv -or -not (Test-Path -LiteralPath $srv.Log)) { return '' }
    $stream = [IO.File]::Open($srv.Log, 'Open', 'Read', 'ReadWrite')
    $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8, $true)
    try { return (($reader.ReadToEnd() -split "`n" | Where-Object { $_ -notmatch '\[FINE/CustomNPCs\]' }) -join "`n") }
    finally { $reader.Dispose() }
}
function Send([string]$Command) {
    $srv.Proc.StandardInput.WriteLine($Command)
    $srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}
function Wait-Marker([string]$Marker, [int]$Seconds = 60, [int]$Count = 1) {
    $end = (Get-Date).AddSeconds($Seconds)
    do {
        $text = Get-Log
        if ([regex]::Matches($text, [regex]::Escape($Marker)).Count -ge $Count) { return $true }
        if ($srv.Proc.HasExited -or (Get-ServerSesutje $text)) { return $false }
        # The server's own process tree is the only permitted Java workload.
        $foreign = @(Get-NavForeignJava -Roots @($srv.Proc.Id))
        if ($foreign.Count) { throw ('Foreign Java workload during compatibility proof: ' + ($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $end)
    return $false
}
function Wait-Input {
    $ready = $false
    for ($i = 0; $i -lt 6 -and -not $ready; $i++) {
        $before = [regex]::Matches((Get-Log), 'players online').Count
        Send 'list'
        $ready = Wait-Marker 'players online' 5 ($before + 1)
    }
    if (-not $ready) { throw 'Server stdin did not respond.' }
}
function Stop-Server {
    if ($null -eq $srv -or $srv.Proc.HasExited) { return }
    # Release diagnostic tickets before world unload, also on a failed setup.
    Send 'rwdiag chunks off'
    Send 'stop'
    if (-not $srv.Proc.WaitForExit(180000)) {
        & taskkill.exe /PID $srv.Proc.Id /T /F 2>&1 | Out-Null
        throw 'Compatibility server did not stop cleanly.'
    }
    Check 'clean server exit' ($srv.Proc.ExitCode -eq 0)
}
function Start-Server([string]$LogPath) {
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain -PnpcBaritoneDev -PrwNavBackend=1 "-PrwWorld={0}" > "{1}" 2>&1' -f $world, $LogPath)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    return [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $LogPath }
}

try {
    New-Item -ItemType Directory -Force -Path $audit | Out-Null
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'nav-compat'
    $foreign = @(Get-NavForeignJava)
    if ($foreign.Count) { throw ('Foreign Java workload before compatibility proof: ' + ($foreign -join '; ')) }
    $mod = Get-NavModPath $root $BaritoneRoot
    $runtimeHash = Get-NavRuntimeHash $mod
    $env:NPCB_MOD_DIR = $mod
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $configs = @{}
    foreach ($name in @('CustomNpcs.cfg', 'npcbaritone.cfg')) {
        $configs[$name] = (Get-FileHash -LiteralPath (Join-Path $run "config\$name") -Algorithm SHA256).Hash
    }
    # Fixed, already present measurement conditions; do not silently enable large-entity mode.
    if ([IO.File]::ReadAllText((Join-Path $run 'config\npcbaritone.cfg')) -notmatch 'B:largeEntities=false') { throw 'This probe expects the existing largeEntities=false config.' }
    if ([IO.File]::ReadAllText((Join-Path $run 'config\CustomNpcs.cfg')) -notmatch '(?m)^RwMountSteering=0\s*$') { throw 'This probe expects the existing original mount mode 0 (D-019 hitbox history).' }
    $properties = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    if ($properties -notmatch '(?m)^level-type=FLAT' -or $properties -notmatch '(?m)^level-seed=customnpc-rework' -or
        $properties -notmatch '(?m)^enable-command-block=true') { throw 'Expected testworld flat/seed/command-block properties.' }
    if ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -notmatch 'eula=true') { throw 'Real EULA acceptance missing.' }
    $worldPath = Join-Path $run $world
    if (Test-Path -LiteralPath $worldPath) { throw 'Probe world already exists.' }
    $clones = Join-Path $worldPath 'customnpcs\clones\1'
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier')) {
        $size = if ($name -eq 'Large') { 10 } else { 5 }
        $movement = if ($name -eq 'Fly') { 1 } elseif ($name -eq 'Swim') { 2 } else { 0 }
        Write-NavFixture (Join-Path $seed 'customnpcs\clones\1\LET_Walker.json') (Join-Path $clones "NAVC_$name.json") "NAVC_$name" $size $movement
    }
    $controller = Join-Path $clones 'NAVC_Control.json'
    Install-NavController (Join-Path $seed 'customnpcs\clones\1\PERF_Kontrola.json') (Join-Path $seed 'nav-compat-control.js') $controller
    $text = [IO.File]::ReadAllText($controller).Replace('"Name": "PERF_Kontrola"', '"Name": "NAVC_Control"')
    [IO.File]::WriteAllText($controller, $text, (New-Object Text.UTF8Encoding $false))

    # As in letenje-run/perf-run: setworldspawn alone does not load the new spawn
    # region. CmdClone.spawn ignores World.spawnEntity's false result for unloaded
    # chunks. Persist the origin and restart before spawning any probe actor.
    $srv = Start-Server "$prefix-bootstrap.log"
    if (-not (Wait-Marker 'Done (' 900)) { throw 'Probe bootstrap startup failed.' }
    Wait-Input
    Send 'setworldspawn 0 4 0'
    Check 'probe world spawn set to origin' (Wait-Marker 'Set the world spawn point to (0, 4, 0)')
    Send 'save-all flush'
    Check 'probe world saved before restart' (Wait-Marker 'Saved the world' 180)
    Stop-Server
    $srv = $null
    if ($failed.Count) { throw 'Probe bootstrap failed.' }

    $srv = Start-Server "$prefix.log"
    if (-not (Wait-Marker 'Done (' 900)) { throw 'Probe server startup failed.' }
    Check 'isolated world selected' ((Get-Log) -match ('Preparing level "' + [regex]::Escape($world) + '"'))
    Wait-Input
    Send 'setworldspawn 0 4 0'
    Send 'gamerule doMobSpawning false'
    Send 'time set 1000'
    Send 'weather clear'
    $i = 0
    foreach ($name in @('Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier')) {
        Send ("noppes clone spawn NAVC_$name 1 0,4,{0}" -f ($i * 8))
        $i++
    }
    Send 'rwdiag chunks on 2'
    Check 'six probe NPCs have forced chunks without rejection' (Wait-Marker 'RWDIAG-CHUNKS stanje=on')
    Check 'chunk count proof' ((Get-Log) -match 'RWDIAG-CHUNKS stanje=on chunki=\d+ tiketi=\d+ obroc=2 zavrnjeni=0 npc=6')
    if ($failed.Count) { throw 'Probe setup failed.' }
    Send 'noppes clone spawn NAVC_Control 1 0,4,48'
    Check 'probe controller completed' (Wait-Marker 'NAVC DONE count=6' 120)
    $text = Get-Log
    Check 'C1-C4: exact eligibility, real large hitbox, arrivals, dismount/rebuild' (Test-NavCompatibility $text)
    Check 'no script or mod errors' ($text -notmatch 'script errored|ScriptException|(?m)^.*\bERROR\b.*(?:noppes\.|npcbaritone)')
    Check 'optional runtime unchanged' ((Get-NavRuntimeHash $mod) -eq $runtimeHash)
    foreach ($name in $configs.Keys) {
        Check "config $name unchanged" ((Get-FileHash -LiteralPath (Join-Path $run "config\$name") -Algorithm SHA256).Hash -eq $configs[$name])
    }
    Send 'rwdiag chunks off'
    Stop-Server
    $result = Write-MeritevJson -Path "$prefix.json" -Paket 'M4.15a' -Scenarij 'nav-compat' `
        -Odtis @{ runtime = $runtimeHash; cnpcConfig = $configs['CustomNpcs.cfg']; npcbConfig = $configs['npcbaritone.cfg']; scene = 'nav-compat-v1' } `
        -Velicine @{ actors = 6; rows = 18; valid = [int](Test-NavCompatibility $text) } -Uspeh ($failed.Count -eq 0) -Padle $failed
    Write-Host "Compatibility evidence: $result"
    if (-not $failed.Count) { $exitCode = 0 }
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $srv -and -not $srv.Proc.HasExited) { try { Stop-Server } catch { Write-Host $_; $exitCode = 1 } }
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $exitCode
