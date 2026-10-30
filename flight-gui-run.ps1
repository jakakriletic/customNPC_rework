# P6 controller scenario: RwFlightMode button in the AI movement editor. Normal wand/AI clicks,
# server observations, clones, then a second client JVM loading the same world.
# No changes to existing worlds/configs.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwflight-gui-$stamp"
$prefix = Join-Path $root "audit/p6-$stamp"
$lock = $null
$client = $null
$exitCode = 1
$proofs = @()
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$hashes = @{}

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Fingerprint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Not a regular file: $Path" }
    return ('sha256:' + (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash)
}
function Guard {
    $owned = @()
    if ($null -ne $client) { $owned += $client.Id }
    $foreign = @(Get-NavForeignJava -Roots $owned)
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged {
    foreach ($path in $hashes.Keys) { Assert ((Fingerprint $path) -ceq $hashes[$path]) "unchanged $path" }
}
function Start-Client([string]$Pass) {
    $out = "$prefix-$Pass.json"
    $job = "$prefix-$Pass.job"
    $log = "$prefix-$Pass-client.log"
    [IO.File]::WriteAllLines($job, @('flightGui=1', "world=$world", "pass=$Pass", ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwHarness={0}" > "{1}" 2>&1' -f $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $script:client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(780)
    while (-not $client.HasExited -and (Get-Date) -lt $until) {
        Guard
        Start-Sleep -Seconds 1
    }
    Assert $client.HasExited "client $Pass completed before timeout"
    Assert ($client.ExitCode -eq 0) "client $Pass clean process exit"
    Unchanged
    $text = [IO.File]::ReadAllText($log)
    Assert ($text.Contains("FLGUI DONE success=true pass=$Pass")) "client $Pass success marker"
    Assert ($text -notmatch 'script errored|ScriptException|NoClassDefFoundError|FLGUI DONE success=false') "client $Pass no runtime errors"
    Assert (Test-Path -LiteralPath $out) "client $Pass proof exists"
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true -and $proof.pass -ceq $Pass) "client $Pass proof identity"
    $rounds = 6
    # Rounds with the flight button visible: all edit rounds; verify skips the two ground NPCs.
    $shown = if ($Pass -eq 'edit') { @(0, 1, 2, 3, 4, 5) } else { @(1, 2, 4, 5) }
    Assert ($proof.rounds -eq $rounds) "client $Pass completed $rounds rounds"
    Assert (@($proof.assertions).Count -ge 1) "client $Pass has assertions"
    Assert (@($proof.assertions | Where-Object { $_.ok -ne $true }).Count -eq 0) "client $Pass all assertions passed"
    Assert (@($proof.assertions.key | Select-Object -Unique).Count -eq @($proof.assertions).Count) "client $Pass unique assertion keys"
    foreach ($r in 0..($rounds - 1)) {
        $suffixes = @('gui-movement','gui-present','pending','ai-parent','closed','server')
        if ($shown -contains $r) { $suffixes += @('gui-mode','gui-label','gui-enabled','gui-bounds','gui-pending') }
        foreach ($suffix in $suffixes) {
            Assert (@($proof.assertions | Where-Object { $_.key -ceq "r$r-$suffix" -and $_.ok -eq $true }).Count -eq 1) "client $Pass r$r-$suffix exists once"
        }
    }
    if ($Pass -eq 'edit') {
        foreach ($key in @('r3-gui-shown', 'r5-gui-hidden', 'clone0', 'clone1', 'clone2', 'clone0-new-uuid', 'clone1-new-uuid', 'clone2-new-uuid')) {
            Assert (@($proof.assertions | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "client $Pass $key exists once"
        }
    }
    Assert (@($proof.screenshots).Count -eq $shown.Count) "client $Pass screenshots count"
    foreach ($image in $proof.screenshots) {
        Assert ([IO.Path]::GetFileName($image) -ceq $image) 'screenshot local filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    Assert (@($proof.uuids.psobject.Properties).Count -eq 6) "client $Pass six UUID records"
    $ids = @()
    foreach ($i in 0..2) {
        foreach ($suffix in @('', '_Clone')) {
            $name = "FLGUI_$i$suffix"
            $id = $proof.uuids.$name
            Assert ($id -cmatch '^[0-9a-f-]{36}$') "client $Pass UUID $name"
            $ids += $id
        }
    }
    Assert (@($ids | Select-Object -Unique).Count -eq 6) "client $Pass six distinct entity UUIDs"
    $script:proofs += $proof
    $script:client = $null
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'flight-gui'
    Guard
    foreach ($name in @('config/CustomNpcs.cfg', 'config/npcbaritone.cfg', 'server.properties', 'eula.txt', 'options.txt')) {
        $path = Join-Path $run $name
        $hashes[$path] = Fingerprint $path
    }
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $worldPath = Join-Path $run "saves/$world"
    Assert (-not (Test-Path -LiteralPath $worldPath)) 'fresh isolated world'
    $clones = Join-Path $worldPath 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    # FLGUI_0 flying/Original, FLGUI_1 ground/Original, FLGUI_2 flying/MOMENTUM (set only by NBT).
    foreach ($i in 0..2) {
        $fixture = Join-Path $clones "FLGUI_$i.json"
        $movement = if ($i -eq 1) { 0 } else { 1 }
        Write-NavFixture (Join-Path $root 'dev/testworld/customnpcs/clones/1/LET_Walker.json') $fixture "FLGUI_$i" 5 $movement
        $text = [IO.File]::ReadAllText($fixture).Replace('"RwNavBackend": 1,', '')
        if ($i -eq 2) { $text = $text.Insert($text.IndexOf('{') + 1, '"RwFlightMode": 2,') }
        Assert ($text -match '"MovingState": 0' -and $text -match ('"MovementType": ' + $movement) -and $text -notmatch '"RwHitboxMode"|"RwNavBackend"') "standing fixture FLGUI_$i"
        Assert (($text -match '"RwFlightMode"') -eq ($i -eq 2)) "flight mode fixture FLGUI_$i"
        [IO.File]::WriteAllText($fixture, $text, (New-Object Text.UTF8Encoding $false))
    }
    foreach ($pass in @('edit', 'verify')) {
        Start-Client $pass
        Assert (Test-Path -LiteralPath (Join-Path $worldPath 'level.dat')) "world persisted after $pass"
    }
    foreach ($name in @($proofs[0].uuids.psobject.Properties.Name)) {
        Assert ($proofs[0].uuids.$name -ceq $proofs[1].uuids.$name) "same UUID after JVM/world restart: $name"
    }
    Unchanged
    $null = Write-MeritevJson -Path "$prefix.json" -Paket 'P6' -Scenarij 'flight-gui-save-restart-clone' `
        -Odtis @{ harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; cnpcConfig = $hashes[(Join-Path $run 'config/CustomNpcs.cfg')]; options = $hashes[(Join-Path $run 'options.txt')] } `
        -Velicine @{ assertions = (@($proofs[0].assertions).Count + @($proofs[1].assertions).Count); screenshots = 10; actors = 6; restarts = 1 } -Uspeh $true -Padle @()
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
