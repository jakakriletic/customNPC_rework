# Controller preflight only. No Minecraft and no measurements.
$ErrorActionPreference='Stop'
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
try {
    foreach($name in @('m10-client-run.ps1','m10-client-compile.ps1')) {
        $parseTokens=$null; $parseErrors=$null
        $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $name),[ref]$parseTokens,[ref]$parseErrors)
        if($parseErrors.Count) { throw ($parseErrors -join "`n") }
    }
    & (Join-Path $PSScriptRoot 'dev.ps1') -GradleArgs @('releaseHarnessJar','--offline')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $path=Join-Path $PSScriptRoot 'dev/build/libs/m10-release-harness.jar'
    $zip=[IO.Compression.ZipFile]::OpenRead($path)
    try {
        $classes=@($zip.Entries | Where-Object {$_.FullName.EndsWith('.class')})
        if($classes.Count -lt 3) { throw 'Missing independent harness classes' }
        if(@($classes | Where-Object {$_.FullName -cnotmatch '^local/m10/'}).Count) { throw 'Production classes leaked into test mod' }
        foreach($name in @('local/m10/ReleaseHarness.class','local/m10/ReleaseClient.class')) {
            if($null -eq $zip.GetEntry($name)) { throw "Missing $name" }
        }
    } finally { $zip.Dispose() }
    Write-Host 'M101D COMPILE PASS: PowerShell parsed, independent harness compiled, jar contains only local/m10 classes.'
} finally { $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath }
