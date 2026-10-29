# M4.15b compilable import baseline. Controller only; no launches.
# v1 remains the historical byte-exact import check; this ID allows only the
# two documented compilation repairs, with the reference sources pinned.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'meritve-lib.ps1')
$imports = @(
    @{ Path = 'noppes/npcs/PacketHandlerServer.java'; Hash = '72A0351B87898CA7AFFFB973A4B182A5ABCD96B331099239C4A1357B64EA5C31'; From = 'Set names = compound.getKeySet();'; To = 'Set<String> names = compound.getKeySet();'; Count = 1 },
    @{ Path = 'noppes/npcs/client/gui/mainmenu/GuiNpcAI.java'; Hash = '884EAA6E7BF7A27DD3633C725755B8158A354B528205677E96D2F3839BE3C1CB'; From = 'textfield.id'; To = 'textfield.getId()'; Count = 1 },
    @{ Path = 'noppes/npcs/client/gui/SubGuiNpcMovement.java'; Hash = 'CC5FFB3A4C709F3FA82A10C8A98272114FFAA353C28389A0AA8A065F892E19CC'; From = 'textfield.id'; To = 'textfield.getId()'; Count = 6 }
)
$fingerprint = @{}
foreach ($import in $imports) {
    $original = Join-Path $PSScriptRoot ('dev/reference-src/' + $import.Path)
    $restored = Join-Path $PSScriptRoot ('dev/src/patch/java/' + $import.Path)
    $hash = (Get-FileHash -LiteralPath $original -Algorithm SHA256).Hash
    if ($hash -cne $import.Hash) { throw "Reference changed: $($import.Path)" }
    $source = [IO.File]::ReadAllText($original).Replace("`r`n", "`n")
    $actual = [IO.File]::ReadAllText($restored).Replace("`r`n", "`n")
    $count = [regex]::Matches($source, [regex]::Escape($import.From)).Count
    if ($count -ne $import.Count) { throw "Unexpected repair count: $($import.Path) ($count)" }
    $expected = $source.Replace($import.From, $import.To)
    if (-not [string]::Equals($expected, $actual, [StringComparison]::Ordinal)) {
        throw "Import differs beyond compilation repairs: $($import.Path)"
    }
    $fingerprint[$import.Path] = (Get-FileHash -LiteralPath $restored -Algorithm SHA256).Hash
    Write-Host "OK original source plus $count compilation repairs: $($import.Path)"
}
$jar = Join-Path $PSScriptRoot 'CustomNPCs_1.12.2-(01Oct19).jar'
$jarHash = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
if ($jarHash -cne 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') {
    throw 'Original binary hash changed.'
}
$fingerprint['originalJar'] = $jarHash
$proof = Join-Path $PSScriptRoot ('audit/m415b-import-v2-' + (Get-Date -Format 'yyyy-MM-dd-HHmmss') + '.json')
$null = Write-MeritevJson -Path $proof -Paket 'M4.15b' -Scenarij 'compilable-original-import' `
    -Odtis $fingerprint -Velicine @{ restoredSources = $imports.Count; compilationRepairs = 8; originalBinary = 1 }
Write-Host "PASS: 3 original sources with only 8 compilation repairs; original binary unchanged. Evidence: $proof"
exit 0
