# Controller-only regression for the date/order guard. No Java, build or Minecraft.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'nav-gui-properties-lib.ps1')
$temp = Join-Path ([IO.Path]::GetTempPath()) ('nav-gui-properties-' + [guid]::NewGuid() + '.properties')
$encoding = [Text.Encoding]::GetEncoding(28591)
$original = $encoding.GetBytes("#Minecraft server properties`r`n#Thu Oct 29 15:47:54 CET 2026`r`nlevel-seed=customnpc-rework`r`nview-distance=10`r`n")
$dateOnly = $encoding.GetString($original).Replace('15:47:54', '16:00:00')
$checks = 0
function Assert([bool]$Ok, [string]$Message) {
    if (-not $Ok) { throw $Message }
    $script:checks++
    Write-Host "OK $Message"
}
try {
    $reordered = $dateOnly.Replace("level-seed=customnpc-rework`r`nview-distance=10", "view-distance=10`r`nlevel-seed=customnpc-rework")
    foreach ($allowed in @($dateOnly, $reordered)) {
        [IO.File]::WriteAllBytes($temp, $encoding.GetBytes($allowed))
        Restore-NavGuiProperties $temp $original
        Assert ([Convert]::ToBase64String([IO.File]::ReadAllBytes($temp)) -ceq [Convert]::ToBase64String($original)) 'date/order rewrite restored byte for byte'
    }
    foreach ($changed in @(
        $dateOnly.Replace('view-distance=10', 'view-distance=9'),
        $dateOnly.Replace('customnpc-rework', 'different-seed'),
        ($dateOnly + "extra-key=true`r`n"),
        $dateOnly.Replace("view-distance=10`r`n", ''),
        $dateOnly.Replace('#Minecraft server properties', '#Unexpected header'),
        $dateOnly.Replace('Thu Oct 29', 'invalid date'),
        $dateOnly.Replace("`r`n", "`n"),
        ($dateOnly + "view-distance=10`r`n"),
        $dateOnly.Replace('view-distance=10', 'View-distance=10'),
        $dateOnly.Replace('view-distance=10', 'view-distance=010'),
        $dateOnly.Replace('view-distance=10', 'view-distance=10 '),
        ($dateOnly + "#extra comment`r`n"),
        $dateOnly.Replace('view-distance=10', 'view-distance=10\'),
        $dateOnly.Replace("view-distance=10`r`n", "view-distance=10`n"),
        ($dateOnly + "`r`n"),
        $dateOnly.TrimEnd([char[]]"`r`n")
    )) {
        $bytes = $encoding.GetBytes($changed)
        [IO.File]::WriteAllBytes($temp, $bytes)
        $rejected = $false
        try { Restore-NavGuiProperties $temp $original } catch { $rejected = $true }
        Assert $rejected 'property record or line-ending change rejected'
        Assert ([Convert]::ToBase64String([IO.File]::ReadAllBytes($temp)) -ceq [Convert]::ToBase64String($bytes)) 'rejected file preserved byte for byte'
    }
    Write-Host "PASS $checks properties guard assertions"
} finally {
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp }
}
