# Controller-only. No Gradle/Minecraft; exercises the actual overlay and readers.
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'formacije-samotest.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
. (Join-Path $PSScriptRoot 'formacije-navigacija-lib.ps1')
$tokens = $null
$errors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'formacije-run.ps1'), [ref]$tokens, [ref]$errors)
if ($errors.Count) { throw ($errors | Out-String) }
foreach ($name in @('Read-FormationTrace', 'Install-Controller')) {
    $fn = $ast.FindAll({ param($a) $a -is [Management.Automation.Language.FunctionDefinitionAst] -and $a.Name -eq $name }, $true)
    if (@($fn).Count -ne 1) { throw "Missing or duplicate function: $name" }
    Invoke-Expression $fn.Extent.Text
}
$libAst = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'formacije-navigacija-lib.ps1'), [ref]$tokens, [ref]$errors)
if ($errors.Count) { throw ($errors | Out-String) }
$seed = Join-Path $PSScriptRoot 'dev\testworld'
$tempClone = [IO.Path]::GetTempFileName()
$tempTrace = [IO.Path]::GetTempFileName()
try {
    foreach ($TraceNavigator in @($false, $true)) {
        $Trace = $true
        $TraceRecovery = $true
        foreach ($mode in @('NAVIGATE', 'FORMACIJA')) {
            Install-Controller $tempClone $mode
            $clone = [IO.File]::ReadAllText($tempClone)
            $match = [regex]::Match($clone, '"Script"\s*:\s*("(?:\\.|[^"\\])*")')
            $decoded = $match.Groups[1].Value | ConvertFrom-Json
            if ($decoded.Contains('var formNavClass =') -ne ($TraceNavigator -and $mode -eq 'FORMACIJA')) {
                throw 'Navigator diagnostic leaked into an existing input.'
            }
        }
    }
    & node (Join-Path $seed 'preveri-formacije-trace.js') $tempTrace navigator
    if ($LASTEXITCODE -ne 0) { throw 'Navigator overlay contract failed.' }
    $trace = [IO.File]::ReadAllText($tempTrace)
    $result = Read-NavigationTrace $trace
    if (-not $result.ok -or $result.count -ne 8960) { throw ('Navigator coverage failed: ' + ($result.issues -join ', ')) }
    $events = @($result.invalidations)
    if ($events.Count -ne 1 -or $events[0].lane -ne 'G' -or $events[0].tick -ne 100 -or
            $events[0].member -ne 0 -or -not $events[0].timeoutResetSignature) {
        throw 'Synthetic invalidation/reset signature was not reported correctly.'
    }
    $row = [regex]::Match($trace, 'FORM-D proga=G tick=100 i=0 [^\r\n]+').Value
    foreach ($broken in @(
            $trace.Replace($row, $row.Replace('navTimeoutMs=', 'missingTimeout=')),
            $trace.Replace($row, $row.Replace('navTimeoutMs=0', 'navTimeoutMs=-1')),
            $trace.Replace($row, $row.Replace('navTimeoutLimit=0.000000', 'navTimeoutLimit=NaN')),
            $trace.Replace($row, $row.Replace('navTimeoutLastMs=154999', 'navTimeoutLastMs=999999999')),
            $trace.Replace($row, $row.Replace('navTicks=1100', 'navTicks=1102')),
            $trace.Replace($row, $row.Replace('navCheckTick=900', 'navCheckTick=1200')),
            $trace.Replace($row, $row.Replace('navUpdate=false', 'navUpdate=invalid')),
            $trace.Replace($row, $row.Replace('navClass=noppes.npcs.rework.nav.RwPathNavigateGround', 'navClass=other.Navigator')),
            $trace.Replace($row, $row.Replace('navTarget=0.000000,4.000000,78.000000', 'navTarget=broken')))) {
        if ($broken -eq $trace) { throw 'Broken fixture did not mutate the intended field.' }
        if ((Read-NavigationTrace $broken).ok) { throw 'Corrupt navigator trace accepted.' }
    }
    # Loss without the three-field reset must remain unattributed, not inferred as timeout.
    $unattributed = $trace.Replace($row, $row.Replace('navTimeoutMs=0', 'navTimeoutMs=500'))
    $unknown = Read-NavigationTrace $unattributed
    if (-not $unknown.ok -or $unknown.invalidations[0].timeoutResetSignature) { throw 'Unattributed loss mislabeled as a timeout reset.' }
    # D-075: a private read failure keeps the controller alive and fails the evidence closed.
    & node (Join-Path $seed 'preveri-formacije-trace.js') $tempTrace navigator-error
    if ($LASTEXITCODE -ne 0) { throw 'Navigator read-failure contract failed.' }
    $failed = Read-NavigationTrace ([IO.File]::ReadAllText($tempTrace))
    if ($failed.ok -or @($failed.errors).Count -ne 3 -or $failed.errors[0] -notmatch '^FORM-NAVERR step=timeoutLimit ') {
        throw 'Navigator read failure was not reported fail-closed.'
    }
    & node (Join-Path $seed 'preveri-formacije-trace.js') $tempTrace recovery
    if ($LASTEXITCODE -ne 0) { throw 'Existing recovery overlay failed.' }
    if ((Read-NavigationTrace ([IO.File]::ReadAllText($tempTrace))).ok) { throw 'Old recovery trace accepted as navigator evidence.' }
} finally {
    Remove-Item -LiteralPath $tempClone -Force
    Remove-Item -LiteralPath $tempTrace -Force
}
Write-Host 'M4.14b navigator diagnostic contracts pass; no Minecraft validation.'
exit 0
