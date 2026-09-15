<#
    testworld.ps1 — postavi ponovljiv testni svet (M0.6)

    Arhivira obstojeci svet v dev/run/_worlds/world-<datum> in namesti seme
    iz dev/testworld. Nic ne brise; star svet ostane na disku.

    Uporaba:
        .\testworld.ps1            # arhiviraj in namesti
        .\testworld.ps1 -KeepWorld # samo osvezi customnpcs fixture, svet pusti pri miru
#>
param([switch]$KeepWorld)

$ErrorActionPreference = 'Stop'

$root     = $PSScriptRoot
$run      = Join-Path $root 'dev\run'
$world    = Join-Path $run  'world'
$fixtures = Join-Path $root 'dev\testworld'

if (-not (Test-Path $fixtures)) { throw "Ni mape s semenom: $fixtures" }
if (-not (Test-Path $run))      { New-Item -ItemType Directory -Path $run | Out-Null }

# Server ne sme teci: zaklenjen session.lock pomeni, da svet se uporablja.
$lock = Join-Path $world 'session.lock'
if (Test-Path $lock) {
    try {
        $fs = [System.IO.File]::Open($lock, 'Open', 'ReadWrite', 'None')
        $fs.Close()
    } catch {
        throw "Svet je zaklenjen. Najprej ustavi server z ukazom 'stop' v konzoli."
    }
}

if (-not $KeepWorld -and (Test-Path $world)) {
    $stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
    $archive = Join-Path $run '_worlds'
    if (-not (Test-Path $archive)) { New-Item -ItemType Directory -Path $archive | Out-Null }
    $dest = Join-Path $archive "world-$stamp"
    Move-Item -Path $world -Destination $dest
    Write-Host "Star svet arhiviran v: $dest"
}

Copy-Item -Path (Join-Path $fixtures 'server.properties') -Destination (Join-Path $run 'server.properties') -Force
Write-Host "Namescen server.properties (superflat, fiksni seed, brez posasti)."

$targetCnpc = Join-Path $world 'customnpcs'
if (-not (Test-Path $targetCnpc)) { New-Item -ItemType Directory -Path $targetCnpc -Force | Out-Null }
Copy-Item -Path (Join-Path $fixtures 'customnpcs\*') -Destination $targetCnpc -Recurse -Force

$clones = Get-ChildItem -Path (Join-Path $targetCnpc 'clones\1') -Filter *.json -ErrorAction SilentlyContinue
Write-Host ("Namescenih fixture NPC-jev: {0}" -f $clones.Count)
foreach ($c in $clones) { Write-Host ("  - {0}" -f $c.BaseName) }

$eula = Join-Path $run 'eula.txt'
if (-not (Test-Path $eula)) {
    Write-Host "Opozorilo: eula.txt ne obstaja. Server ga bo ustvaril ob prvem zagonu in se ustavil."
}

Write-Host ""
Write-Host "Naslednji korak:"
Write-Host "  .\dev.ps1 runServer --offline"
Write-Host "  nato prilepi dev\testworld\setup-commands.txt v server konzolo"
