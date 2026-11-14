param(
    [Parameter(Mandatory = $true)][string]$VariedJar,
    [string]$ReworkJar = (Join-Path $PSScriptRoot 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$release = @{
    'E8678097564A2758E0B93622E73C2567BB938368820F6AA4E64EBC47137E5BB5' = '21Nov19'
    'BB1AFC496B25E2AFDA523FFC02E3883B4F5A8E6C852C6D5818BD2D82AFAE6016' = '31Mar23'
}
$source = (Resolve-Path -LiteralPath $VariedJar).ProviderPath
$candidate = (Resolve-Path -LiteralPath $ReworkJar).ProviderPath
$hash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$version = $release[$hash]
if (!$version) { throw 'Supply the original 1.12.2 Varied Commodities release from 21 Nov 2019 or 31 Mar 2023.' }
$output = Join-Path (Split-Path -Parent $candidate) "CustomNPCs-Rework-1.12.2-with-VariedCommodities-$version.jar"
if ($output -eq $candidate -or $output -eq $source) { throw 'The output must be distinct from the input jars.' }
$temporary = "$output.$([guid]::NewGuid().ToString('N')).tmp"
$backup = "$temporary.bak"
$npc = $null; $varied = $null; $bundle = $null
$utf8 = [Text.UTF8Encoding]::new($false)
# Forge-generated caches describe the original individual jars. Forge rescans the combined jar.
$forgeCache = @('META-INF/fml_cache_annotation.json', 'META-INF/fml_cache_class_versions.json')

function Copy-Entry($Zip, $Entry, $Target) {
    $dest = $Target.CreateEntry($Entry.FullName)
    $inputStream = $Zip.GetEntry($Entry.FullName).Open()
    try {
        $outputStream = $dest.Open()
        try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose() }
    } finally { $inputStream.Dispose() }
}
function Read-Entry($Zip, [string]$Name) {
    $entry = $Zip.GetEntry($Name)
    if ($null -eq $entry) { throw "Missing $Name" }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
function Write-Entry($Zip, [string]$Name, [string]$Text) {
    $writer = [IO.StreamWriter]::new($Zip.CreateEntry($Name).Open(), $utf8)
    try { $writer.Write($Text) } finally { $writer.Dispose() }
}

try {
    $npc = [IO.Compression.ZipFile]::OpenRead($candidate)
    $varied = [IO.Compression.ZipFile]::OpenRead($source)
    if (!$npc.GetEntry('noppes/npcs/rework/core/RwCoreMod.class') -or $npc.GetEntry('noppes/vc/VariedCommodities.class')) {
        throw 'Supply the built CustomNPCs Rework jar before Varied Commodities has been bundled.'
    }
    $descriptions = @(Read-Entry $npc 'mcmod.info' | ConvertFrom-Json)
    $descriptions += @(Read-Entry $varied 'mcmod.info' | ConvertFrom-Json)
    $bundle = [IO.Compression.ZipFile]::Open($temporary, [IO.Compression.ZipArchiveMode]::Create)
    $names = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($entry in $npc.Entries) {
        if ($entry.FullName.EndsWith('/') -or $entry.FullName -ceq 'mcmod.info' -or $entry.FullName -cin $forgeCache) { continue }
        if (!$names.Add($entry.FullName)) { throw "Duplicate rework entry $($entry.FullName)" }
        Copy-Entry $npc $entry $bundle
    }
    $copied = 0
    foreach ($entry in $varied.Entries) {
        if ($entry.FullName.EndsWith('/') -or $entry.FullName -ceq 'mcmod.info' -or
                $entry.FullName -ceq 'META-INF/MANIFEST.MF' -or $entry.FullName -cin $forgeCache) { continue }
        if (!$names.Add($entry.FullName)) { throw "Overlapping jar entry $($entry.FullName)" }
        Copy-Entry $varied $entry $bundle
        $copied++
    }
    Write-Entry $bundle 'mcmod.info' (ConvertTo-Json -InputObject @($descriptions) -Depth 20)
    Write-Entry $bundle 'META-INF/Varied-Commodities-NOTICE.txt' @"
Varied Commodities for Minecraft 1.12.2 ($version) by Noppes.
Source: https://www.curseforge.com/minecraft/mc-mods/varied-commodities
License: Creative Commons Attribution-NonCommercial 3.0 Unported
https://creativecommons.org/licenses/by-nc/3.0/
The original classes and resources are included unchanged in this collection.
The Forge metadata is combined with the CustomNPCs Rework metadata. Forge regenerates its scan caches.
Original release SHA-256: $hash
"@
    $bundle.Dispose(); $bundle = $null
    if ([IO.File]::Exists($output)) {
        [IO.File]::Replace($temporary, $output, $backup)
        [IO.File]::Delete($backup)
    }
    else { [IO.File]::Move($temporary, $output) }
    Write-Output "Bundled $copied Varied Commodities entries into $output"
} finally {
    if ($null -ne $bundle) { $bundle.Dispose() }
    if ($null -ne $varied) { $varied.Dispose() }
    if ($null -ne $npc) { $npc.Dispose() }
    if ([IO.File]::Exists($temporary)) { [IO.File]::Delete($temporary) }
}
