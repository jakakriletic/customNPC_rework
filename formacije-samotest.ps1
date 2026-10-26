# Controller-only static/contract checks; does not launch Gradle or Minecraft.
$ErrorActionPreference = 'Stop'
$tokens = $null
$errors = $null
$path = Join-Path $PSScriptRoot 'formacije-run.ps1'
$ast = [Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors)
if ($errors.Count) { throw ($errors | Out-String) }
# Exercise the actual production log readers, without evaluating the runner's main body.
foreach ($name in @('Get-Log', 'Read-Arrival', 'Read-FinalSquad', 'Install-Controller', 'Read-FormationTrace')) {
    $fn = $ast.FindAll({ param($a) $a -is [Management.Automation.Language.FunctionDefinitionAst] -and $a.Name -eq $name }, $true)
    if (@($fn).Count -ne 1) { throw "Missing or duplicate function: $name" }
    Invoke-Expression $fn.Extent.Text
}
# Regression for the controller's first A/B attempt: the stdout file is still
# held open by cmd.exe. Exercise the actual reader against an open writer, then
# append and read again so a stale snapshot cannot satisfy the contract.
$tempLog = [IO.Path]::GetTempFileName()
$writer = $null
try {
    $writer = [IO.File]::Open($tempLog, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::ReadWrite)
    $bytes = [Text.Encoding]::UTF8.GetBytes('server starting')
    $writer.Write($bytes, 0, $bytes.Length)
    $writer.Flush()
    if ((Get-Log $tempLog) -cne 'server starting') { throw 'Open-writer log read failed.' }
    $bytes = [Text.Encoding]::UTF8.GetBytes("`nDone (")
    $writer.Write($bytes, 0, $bytes.Length)
    $writer.Flush()
    if ((Get-Log $tempLog) -cne "server starting`nDone (") { throw 'Log reader missed appended marker.' }
} finally {
    if ($null -ne $writer) { $writer.Dispose() }
    Remove-Item -LiteralPath $tempLog -Force
}
if ((Get-Log $tempLog) -cne '') { throw 'Missing log should return empty text.' }
$arrival = Read-Arrival 'NAV-CAS faza=B proga=G prispelo=6/8 prviTick=20 medianaTick=100 zadnjiTick=220' 'G'
if ($arrival.arrived -ne 6 -or $arrival.count -ne 8 -or $arrival.last -ne 220) { throw 'Arrival parser mismatch.' }
$missingRejected = $false
try { $null = Read-Arrival '' 'G' } catch { $missingRejected = $true }
if (-not $missingRejected) { throw 'Missing arrival was accepted.' }
$line = 'RWSQUAD konec enota=1 ime=NAV_WalkG faza=DONE iskanj=12 obnov=2 obtical=3 neporavnani=4 cilj_nedosegljiv=false'
$end = Read-FinalSquad $line 'G'
if ($end.iskanj -ne '12' -or $end.neporavnani -ne '4') { throw 'End parser lost real-search or failure data.' }
if ($null -ne (Read-FinalSquad $line 'O')) { throw 'Wrong lane accepted.' }
if ($null -ne (Read-FinalSquad ($line + "`n" + $line) 'G')) { throw 'Duplicate end marker accepted.' }
$seed = Join-Path $PSScriptRoot 'dev\testworld'
$tempClone = [IO.Path]::GetTempFileName()
try {
    foreach ($variant in @('ordinary', 'trace', 'recovery')) {
        $Trace = $variant -ne 'ordinary'
        $TraceRecovery = $variant -eq 'recovery'
        foreach ($mode in @('NAVIGATE', 'FORMACIJA')) {
            Install-Controller $tempClone $mode
            $clone = [IO.File]::ReadAllText($tempClone)
            $scriptValue = [regex]::Match($clone, '"Script"\s*:\s*("(?:\\.|[^"\\])*")')
            if (-not $scriptValue.Success) { throw 'Generated clone has no readable script string.' }
            $decoded = $scriptValue.Groups[1].Value | ConvertFrom-Json
            if (-not $decoded.Contains("var NAV_MODE = '$mode';")) { throw 'Generated clone has wrong mode.' }
            if (-not $decoded.Contains(".split('\n')")) { throw 'Newline escape was damaged during SNBT embedding.' }
            $hasTrace = $decoded.Contains('var formTraceTimer = 414;')
            if ($hasTrace -ne ($Trace -and $mode -eq 'FORMACIJA')) { throw 'Trace leaked into the reference or ordinary acceptance input.' }
            $hasRecovery = $decoded.Contains('var formTraceRecovery = true;')
            if ($hasRecovery -ne ($TraceRecovery -and $mode -eq 'FORMACIJA')) { throw 'Recovery diagnostic leaked into another mode.' }
        }
    }
} finally { Remove-Item -LiteralPath $tempClone -Force }
# Complete synthetic trace, including known post-DONE drift. The parser must
# validate coverage/data integrity without judging a drifted member successful.
$traceLines = @('FORM-TRACE start squads=2 members=16 through=560 denseThrough=80 stride=20')
$sampleTicks = @(1..80) + @(100..560 | Where-Object { $_ % 20 -eq 0 })
foreach ($lane in @('G', 'O')) {
    foreach ($tick in $sampleTicks) {
        for ($i = 0; $i -lt 8; $i++) {
            $traceLines += "FORM-D proga=$lane tick=$tick i=$i uuid=$lane-$i age=$($tick + 1000) plannerTick=38 faza=DONE finished=true pos=0.5,4,79.055 motion=0,0,0 input=0,0 aiSpeed=0 helper=WAIT helperSpeed=0 helperTarget=0.5,4,78.5 target=0.5,4,78.5 final=0.5,4,78.5 slot=1,-1 mode=0 speed=0 error=0.3 settled=true trailing=false excused=false lagTicks=0 pathUntil=0 noPath=true pathIndex=-1 pathLength=0 pathNext=none pathEnd=none anchor=0.5,4,78.5 anchorSpeed=0 yaw=0 remaining=0 searches=0 stuck=0"
        }
    }
}
$traceLines += 'FORM-TRACE end tick=560'
$syntheticTrace = $traceLines -join "`n"
$coverage = Read-FormationTrace $syntheticTrace
if (-not $coverage.ok -or $coverage.count -ne 1664) { throw ('Complete trace rejected: ' + ($coverage.issues -join ', ')) }
foreach ($broken in @(
        (($traceLines | Where-Object { $_ -notmatch 'proga=G tick=1 i=0 ' }) -join "`n"),
        ($syntheticTrace + "`n" + $traceLines[1]),
        $syntheticTrace.Replace('proga=G tick=1 i=0 uuid=G-0 age=1001', 'proga=G tick=1 i=0 uuid=G-0 age=999'),
        $syntheticTrace.Replace('pos=0.5,4,79.055', 'pos=NaN,4,79.055'),
        $syntheticTrace.Replace('motion=0,0,0', 'motion=broken'),
        $syntheticTrace.Replace('FORM-TRACE end tick=560', 'FORM-TRACE end tick=559'),
        $syntheticTrace.Replace('uuid=O-7 ', 'uuid=G-0 '))) {
    if ((Read-FormationTrace $broken).ok) { throw 'Incomplete or corrupt diagnostic trace accepted.' }
}
if ((Read-FormationTrace '').ok) { throw 'Missing diagnostic trace accepted.' }
& node (Join-Path $PSScriptRoot 'dev\testworld\preveri-formacije-markerje.js')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$tempTrace = [IO.Path]::GetTempFileName()
try {
    & node (Join-Path $PSScriptRoot 'dev\testworld\preveri-formacije-trace.js') $tempTrace
    if ($LASTEXITCODE -ne 0) { throw 'Diagnostic overlay contract failed.' }
    $actualOverlay = Read-FormationTrace ([IO.File]::ReadAllText($tempTrace))
    if (-not $actualOverlay.ok) { throw ('Actual overlay output rejected: ' + ($actualOverlay.issues -join ', ')) }
    & node (Join-Path $PSScriptRoot 'dev\testworld\preveri-formacije-trace.js') $tempTrace recovery
    if ($LASTEXITCODE -ne 0) { throw 'Recovery diagnostic overlay contract failed.' }
    $denseTrace = [IO.File]::ReadAllText($tempTrace)
    $denseCoverage = Read-FormationTrace $denseTrace -Recovery
    if (-not $denseCoverage.ok -or $denseCoverage.count -ne 8960) { throw ('Recovery overlay output rejected: ' + ($denseCoverage.issues -join ', ')) }
    if ((Read-FormationTrace $denseTrace).ok -or (Read-FormationTrace $syntheticTrace -Recovery).ok) { throw 'Diagnostic versions were confused.' }
    $denseMember = [regex]::Match($denseTrace, 'FORM-D proga=G tick=99 i=0 [^\r\n]+').Value
    $denseAbsent = [regex]::Match($denseTrace, 'FORM-D proga=G tick=99 i=1 [^\r\n]+').Value
    if (-not $denseMember -or -not $denseAbsent) { throw 'Recovery fixture is missing identity cases.' }
    foreach ($broken in @(
            ([regex]::Replace($denseTrace, '(?m)^FORM-D proga=G tick=99 i=0 [^\r\n]+\r?\n', '')),
            ($denseTrace + "`n" + $denseMember),
            $denseTrace.Replace($denseMember, $denseMember.Replace('recoveryId=', 'absentId=')),
            $denseTrace.Replace($denseMember, $denseMember.Replace('recoveryFinished=false', 'recoveryFinished=invalid')),
            $denseTrace.Replace($denseMember, $denseMember.Replace('lastPathTarget=0.500000,78.500000', 'lastPathTarget=broken')),
            $denseTrace.Replace($denseMember, $denseMember.Replace('byLost=0', 'byLost=1')),
            $denseTrace.Replace($denseAbsent, $denseAbsent.Replace('recoveryId=0 recoveryIndex=-1', 'recoveryId=0 recoveryIndex=0')),
            $denseTrace.Replace($denseAbsent, $denseAbsent.Replace('navId=0 recoveryId=0', 'navId=1 recoveryId=0')))) {
        if ((Read-FormationTrace $broken -Recovery).ok) { throw 'Incomplete or corrupt recovery diagnostic accepted.' }
    }
} finally { Remove-Item -LiteralPath $tempTrace -Force }
Write-Host 'M4.14b: PowerShell parsing and actual log readers pass.'
exit 0
