# M4.15b import baseline. Controller only; no Minecraft, Gradle or background process.
# Run once BEFORE changing these restored classes. This proves source fidelity,
# not GUI interaction or packet behaviour in a running game.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'meritve-lib.ps1')
$imports = @(
    'noppes/npcs/PacketHandlerServer.java',
    'noppes/npcs/client/gui/mainmenu/GuiNpcAI.java',
    'noppes/npcs/client/gui/SubGuiNpcMovement.java'
)
$fingerprint = @{}
foreach ($relative in $imports) {
    $original = Join-Path $PSScriptRoot ('dev/reference-src/' + $relative)
    $restored = Join-Path $PSScriptRoot ('dev/src/patch/java/' + $relative)
    $before = (Get-FileHash -LiteralPath $original -Algorithm SHA256).Hash
    $after = (Get-FileHash -LiteralPath $restored -Algorithm SHA256).Hash
    if ($before -ne $after) { throw "Import differs from reference: $relative" }
    $fingerprint[$relative] = $after
    Write-Host "OK exact original source: $relative ($after)"
}
$jar = Join-Path $PSScriptRoot 'CustomNPCs_1.12.2-(01Oct19).jar'
$jarHash = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
if ($jarHash -ne 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') {
    throw 'Original binary hash changed.'
}
$fingerprint['originalJar'] = $jarHash
$proof = Join-Path $PSScriptRoot ('audit/m415b-import-' + (Get-Date -Format 'yyyy-MM-dd-HHmmss') + '.json')
$null = Write-MeritevJson -Path $proof -Paket 'M4.15b' -Scenarij 'original-import' `
    -Odtis $fingerprint -Velicine @{ exactSources = $imports.Count; originalBinary = 1 }
Write-Host "PASS: 3 exact source imports and unchanged original binary. Evidence: $proof"
exit 0
