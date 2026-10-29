# M4.15b controller scenario: server editor contract with both gates and no optional mod.
# Does not simulate client clicks or replace M4.15c. No performance measurement.
param([ValidateSet(0,1)][int]$Library = 1, [ValidateSet(0,1)][int]$Global = 1, [string]$BaritoneRoot = '')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev\run'
$seed = Join-Path $root 'dev\testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'nav-gui-properties-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwnavgui-$Library-$Global-$stamp"
$prefix = Join-Path $root "audit/m415b-contract-$Library-$Global-$stamp"
$srv = $null
$lock = $null
$failed = @()
$exitCode = 1
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$oldMod = $env:NPCB_MOD_DIR
$propertiesPath = Join-Path $run 'server.properties'
$propertiesBytes = $null
$propertiesSaves = 0

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
        if ([regex]::Matches((Get-Log), [regex]::Escape($Marker)).Count -ge $Count) { return $true }
        if ($srv.Proc.HasExited -or (Get-ServerSesutje (Get-Log))) { return $false }
        $foreign = @(Get-NavForeignJava -Roots @($srv.Proc.Id))
        if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $end)
    return $false
}
function Wait-Input {
    for ($i = 0; $i -lt 6; $i++) {
        $before = [regex]::Matches((Get-Log), 'players online').Count
        Send 'list'
        if (Wait-Marker 'players online' 5 ($before + 1)) { return }
    }
    throw 'Server stdin did not respond.'
}
function Stop-Server {
    if ($null -eq $srv -or $srv.Proc.HasExited) { return }
    Send 'rwdiag chunks off'
    Send 'stop'
    if (-not $srv.Proc.WaitForExit(180000)) {
        & taskkill.exe /PID $srv.Proc.Id /T /F 2>&1 | Out-Null
        throw 'Editor contract server did not stop cleanly.'
    }
    Check 'clean server exit' ($srv.Proc.ExitCode -eq 0)
    $script:propertiesSaves++
    [IO.File]::WriteAllBytes("$prefix-server.properties-after-$propertiesSaves", [IO.File]::ReadAllBytes($propertiesPath))
    Restore-NavGuiProperties $propertiesPath $propertiesBytes
    Check 'server property records/line endings unchanged, original bytes restored' $true
}
function Start-Server([string]$LogPath) {
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $optional = if ($Library -eq 1) { '-PnpcBaritoneDev' } else { '' }
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain {0} -PrwNavBackend={1} "-PrwWorld={2}" > "{3}" 2>&1' -f $optional, $Global, $world, $LogPath)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    return [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $LogPath }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'nav-gui'
    if (@(Get-NavForeignJava).Count) { throw 'Foreign Java workload before editor contract.' }
    $mod = Get-NavModPath $root $BaritoneRoot
    $runtimeHash = Get-NavRuntimeHash $mod
    $env:NPCB_MOD_DIR = $mod
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    # Without the dev classpath, an installed JAR must not masquerade as absent.
    if ($Library -eq 0) {
        $mods = Join-Path $run 'mods'
        if (Test-Path -LiteralPath $mods) {
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            foreach ($jar in @(Get-ChildItem -LiteralPath $mods -Recurse -Filter '*.jar')) {
                $zip = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
                try {
                    if ($zip.GetEntry('si/ladja/npcbaritone/api/NpcBaritone.class')) { throw 'Optional mod JAR present in no-library scenario.' }
                } finally { $zip.Dispose() }
            }
        }
    }
    $configs = @{}
    foreach ($name in @('CustomNpcs.cfg', 'npcbaritone.cfg', 'server.properties', 'eula.txt')) {
        $path = if ($name -in @('server.properties', 'eula.txt')) { Join-Path $run $name } else { Join-Path $run "config/$name" }
        $configs[$path] = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    }
    $propertiesBytes = [IO.File]::ReadAllBytes($propertiesPath)
    [IO.File]::WriteAllBytes("$prefix-server.properties-before", $propertiesBytes)
    $null = Get-NavGuiPropertiesContent $propertiesBytes
    if ([IO.File]::ReadAllText((Join-Path $run 'config/npcbaritone.cfg')) -notmatch 'B:largeEntities=false') { throw 'Expected unchanged largeEntities=false.' }
    $properties = [IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    if ($properties -notmatch '(?m)^level-type=FLAT' -or $properties -notmatch '(?m)^level-seed=customnpc-rework') { throw 'Expected existing flat testworld properties.' }
    if ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -notmatch 'eula=true') { throw 'Real EULA acceptance missing.' }
    $worldPath = Join-Path $run $world
    if (Test-Path -LiteralPath $worldPath) { throw 'Contract world already exists.' }
    $clones = Join-Path $worldPath 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier')) {
        $size = if ($name -eq 'Large') { 10 } else { 5 }
        $movement = if ($name -eq 'Fly') { 1 } elseif ($name -eq 'Swim') { 2 } else { 0 }
        Write-NavFixture (Join-Path $seed 'customnpcs/clones/1/LET_Walker.json') (Join-Path $clones "NAVGUI_$name.json") "NAVGUI_$name" $size $movement
    }
    $controller = Join-Path $clones 'NAVGUI_Control.json'
    Install-NavController (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') (Join-Path $seed 'nav-gui-control.js') $controller
    $text = [IO.File]::ReadAllText($controller).Replace('"Name": "PERF_Kontrola"', '"Name": "NAVGUI_Control"')
    [IO.File]::WriteAllText($controller, $text, (New-Object Text.UTF8Encoding $false))

    # Persist the origin spawn before spawning probes, as required by the unloaded-chunk finding.
    $srv = Start-Server "$prefix-bootstrap.log"
    if (-not (Wait-Marker 'Done (' 900)) { throw 'Contract bootstrap startup failed.' }
    Wait-Input
    Send 'setworldspawn 0 4 0'
    Check 'world spawn origin' (Wait-Marker 'Set the world spawn point to (0, 4, 0)')
    Send 'save-all flush'
    Check 'bootstrap saved' (Wait-Marker 'Saved the world' 180)
    Stop-Server
    $srv = $null
    if ($failed.Count) { throw 'Contract bootstrap failed.' }

    $srv = Start-Server "$prefix.log"
    if (-not (Wait-Marker 'Done (' 900)) { throw 'Contract server startup failed.' }
    Check 'isolated world' ((Get-Log) -match ('Preparing level "' + [regex]::Escape($world) + '"'))
    Wait-Input
    Send 'gamerule doMobSpawning false'
    Send 'time set 1000'
    Send 'weather clear'
    $i = 0
    foreach ($name in @('Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier')) {
        Send ("noppes clone spawn NAVGUI_$name 1 0,4,{0}" -f ($i * 8)); $i++
    }
    Send 'rwdiag chunks on 2'
    Check 'six actors in forced chunks' (Wait-Marker 'RWDIAG-CHUNKS stanje=on')
    Check 'chunk proof' ((Get-Log) -match 'RWDIAG-CHUNKS stanje=on chunki=\d+ tiketi=\d+ obroc=2 zavrnjeni=0 npc=6')
    if ($failed.Count) { throw 'Contract setup failed.' }
    Send 'noppes clone spawn NAVGUI_Control 1 0,4,48'
    Check 'all contract checks done' (Wait-Marker "NAVGUI DONE checks=97 failed=0 mod=$Library global=$Global" 120)
    $text = Get-Log
    $rows = @([regex]::Matches($text, 'NAVGUI CHECK i=(\d+) key=([\w-]+) ok=([01])'))
    Check '97 unique successful assertions' ($rows.Count -eq 97 -and @($rows | Where-Object { $_.Groups[3].Value -ne '1' }).Count -eq 0 -and
        @($rows | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique).Count -eq 97 -and
        @($rows | ForEach-Object { $_.Groups[2].Value } | Select-Object -Unique).Count -eq 97)
    Check 'no script or mod errors' ($text -notmatch 'script errored|ScriptException|(?m)^.*\bERROR\b.*(?:noppes\.|npcbaritone)')
    Stop-Server
    Check 'optional runtime unchanged' ((Get-NavRuntimeHash $mod) -eq $runtimeHash)
    foreach ($path in $configs.Keys) {
        Check "unchanged $([IO.Path]::GetFileName($path))" ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -eq $configs[$path])
    }
    $proof = Write-MeritevJson -Path "$prefix.json" -Paket 'M4.15b' -Scenarij "nav-gui-contract-$Library-$Global" `
        -Odtis @{ runtime = $runtimeHash; scene = (Get-FileHash (Join-Path $seed 'nav-gui-control.js')).Hash; cnpcConfig = $configs[(Join-Path $run 'config/CustomNpcs.cfg')]; npcbConfig = $configs[(Join-Path $run 'config/npcbaritone.cfg')] } `
        -Velicine @{ actors = 6; assertions = $rows.Count; library = $Library; global = $Global } -Uspeh ($failed.Count -eq 0) -Padle $failed
    Write-Host "Editor contract evidence: $proof"
    if (-not $failed.Count) { $exitCode = 0 }
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $srv -and -not $srv.Proc.HasExited) { try { Stop-Server } catch { Write-Host $_; $exitCode = 1 } }
    if ($null -ne $propertiesBytes -and ($null -eq $srv -or $srv.Proc.HasExited)) {
        try { Restore-NavGuiProperties $propertiesPath $propertiesBytes } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
    $env:NPCB_MOD_DIR = $oldMod
}
exit $exitCode
