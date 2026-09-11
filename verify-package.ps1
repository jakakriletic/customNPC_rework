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
try {
    $checked = 0
    $changed = @()
    foreach ($entry in $original.Entries) {
        if ($entry.FullName.EndsWith('/')) { continue }
        $other = $candidate.GetEntry($entry.FullName)
        if (-not $other) { throw "Missing original entry: $($entry.FullName)" }
        if ((Get-EntryHash $entry) -ne (Get-EntryHash $other)) {
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
    Write-Output "PASS: $checked original entries checked; only $($changed.Count) explicitly compiled class entries changed."
    $changed | ForEach-Object { Write-Output "  $_" }
} finally { $candidate.Dispose(); $original.Dispose() }
