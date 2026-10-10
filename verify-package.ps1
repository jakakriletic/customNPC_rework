$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$originalPath = Join-Path $PSScriptRoot 'CustomNPCs_1.12.2-(01Oct19).jar'
$candidatePath = Join-Path $PSScriptRoot 'dev\build\libs\CustomNPCs_1.12.2-01Oct19-workspace.jar'
$allowed = @(Get-ChildItem (Join-Path $PSScriptRoot 'dev\build\classes\java\main') -Recurse -Filter '*.class' | ForEach-Object {
    $_.FullName.Substring((Join-Path $PSScriptRoot 'dev\build\classes\java\main').Length + 1).Replace('\', '/')
})
$original = [System.IO.Compression.ZipFile]::OpenRead($originalPath)
$candidate = [System.IO.Compression.ZipFile]::OpenRead($candidatePath)
function Get-EntryHash($entry) {
    $stream = $entry.Open()
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { [BitConverter]::ToString($sha.ComputeHash($stream)) }
    finally { $sha.Dispose(); $stream.Dispose() }
}
function Get-EntryText($entry) {
    $stream = $entry.Open()
    $reader = New-Object System.IO.StreamReader($stream)
    try { $reader.ReadToEnd() } finally { $reader.Dispose(); $stream.Dispose() }
}
# M5.15 (D-031): manifest je edini vnos originala, ki ni bitna kopija - jar je od M5.15 hkrati
# coremod in mod. Razlika mora biti NATANKO ta dva vnosa; vsaka druga sprememba manifesta je napaka.
$manifestEntry = 'META-INF/MANIFEST.MF'
$manifestAllowed = @{ 'FMLCorePlugin' = 'noppes.npcs.rework.core.RwCoreMod'; 'FMLCorePluginContainsFMLMod' = 'true' }
function Get-ManifestMap([string]$text) {
    $map = @{}
    foreach ($line in ($text -split "`r?`n")) {
        if ($line.Trim() -eq '') { continue }
        $i = $line.IndexOf(':')
        if ($i -lt 1) { throw "Manifest line without a key: $line" }
        $map[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim()
    }
    return $map
}
function Test-ManifestChange($originalEntry, $candidateEntry) {
    $a = Get-ManifestMap (Get-EntryText $originalEntry)
    $b = Get-ManifestMap (Get-EntryText $candidateEntry)
    foreach ($k in $a.Keys) {
        if ($b[$k] -ne $a[$k]) { throw "Manifest entry changed or removed: $k ('$($a[$k])' -> '$($b[$k])')" }
    }
    foreach ($k in $b.Keys) {
        if (-not $a.ContainsKey($k)) {
            if (-not $manifestAllowed.ContainsKey($k)) { throw "Unexpected new manifest entry: $k" }
            if ($b[$k] -ne $manifestAllowed[$k]) { throw "Manifest entry $k has unexpected value: $($b[$k])" }
        }
    }
    $missing = @($manifestAllowed.Keys | Where-Object { -not $b.ContainsKey($_) })
    if ($missing.Count -gt 0) { throw "Manifest is missing the coremod entries: $($missing -join ', ')" }
}

try {
    $checked = 0
    $changed = @()
    foreach ($entry in $original.Entries) {
        if ($entry.FullName.EndsWith('/')) { continue }
        $other = $candidate.GetEntry($entry.FullName)
        if (-not $other) { throw "Missing original entry: $($entry.FullName)" }
        if ((Get-EntryHash $entry) -ne (Get-EntryHash $other)) {
            if ($entry.FullName -eq $manifestEntry) {
                Test-ManifestChange $entry $other
                $changed += ($entry.FullName + ' (coremod vnosa, preverjeno)')
                $checked++
                continue
            }
            if ($entry.FullName -notin $allowed) { throw "Unexpected changed entry: $($entry.FullName)" }
            $changed += $entry.FullName
        }
        $checked++
    }
    foreach ($entry in $candidate.Entries) {
        if (-not $entry.FullName.EndsWith('/') -and -not $original.GetEntry($entry.FullName) -and $entry.FullName -notin $allowed) {
            throw "Unexpected additional entry: $($entry.FullName)"
        }
    }
    Write-Output "PASS: $checked original entries checked; only $($changed.Count) expected entries changed (razredi + manifest coremoda)."
    $changed | ForEach-Object { Write-Output "  $_" }
} finally { $candidate.Dispose(); $original.Dispose() }
