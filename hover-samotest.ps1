# M4.3 harness-only checks. The controller runs this; no Minecraft/build here.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'hover-lib.ps1')
. (Join-Path $PSScriptRoot 'letenje-lib.ps1')
$tmp = Join-Path ([IO.Path]::GetTempPath()) ('hover-harness-' + [Guid]::NewGuid().ToString('N'))
$failures = @()
function Check([string]$Name, [bool]$Ok) {
    if ($Ok) { Write-Host "OK $Name" } else { $script:failures += $Name; Write-Host "FAIL $Name" }
}
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    foreach ($scriptName in @('hover-lib.ps1', 'hover-samotest.ps1', 'hover-cost-run.ps1', 'letenje-run.ps1', 'perf-run.ps1')) {
        $tokens = $null
        $parseErrors = $null
        $null = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $scriptName), [ref]$tokens, [ref]$parseErrors)
        Check "$scriptName parses" ($parseErrors.Count -eq 0)
        $parseErrors | ForEach-Object { Write-Host $_ }
    }
    $seed = Join-Path $PSScriptRoot 'dev/testworld'
    $flyer = Join-Path $seed 'customnpcs/clones/1/LET_Flyer.json'
    $before = (Get-FileHash -LiteralPath $flyer -Algorithm SHA256).Hash
    $dest = Join-Path $tmp 'flyer.json'
    foreach ($mode in @(-1, 0, 1, 2)) {
        Write-HoverFixture $flyer $dest $mode
        $text = [IO.File]::ReadAllText($dest)
        if ($mode -eq -1) { Check 'default fixture is byte-identical' ((Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash -eq $before) }
        else {
            Check "mode $mode is a root int" ($text.StartsWith(('{{"RwFlightMode": {0},' -f $mode)))
            Check "mode $mode leaves every original byte unchanged" ($text.Replace(('"RwFlightMode": {0},' -f $mode), '') -ceq [IO.File]::ReadAllText($flyer))
        }
    }
    Check 'seed unchanged' ((Get-FileHash -LiteralPath $flyer -Algorithm SHA256).Hash -eq $before)
    $rejected = $false
    try { Write-HoverFixture $dest (Join-Path $tmp 'bad.json') 1 } catch { $rejected = $true }
    Check 'refuses an already selected seed' $rejected

    $control = Join-Path $tmp 'LET_Control.json'
    $null = Install-LetController (Join-Path $seed 'customnpcs/clones/1/LET_Control.json') (Join-Path $seed 'letenje-control.js') $control
    Add-HoverProof $control 'rememberHome();' 'npc' 1
    $clone = [IO.File]::ReadAllText($control)
    $m = [regex]::Match($clone, '"Script"\s*:\s*("(?:\\.|[^"\\])*")')
    $code = ConvertFrom-Json $m.Groups[1].Value
    Check 'proof installed once and original setup still present' ($code.Contains('rememberHome();') -and [regex]::Matches($code, 'HOVER-PROOF').Count -eq 1)
    $jsPath = Join-Path $tmp 'control.js'
    [IO.File]::WriteAllText($jsPath, $code, (New-Object Text.UTF8Encoding $false))
    & node --check $jsPath
    Check 'injected LET JavaScript parses' ($LASTEXITCODE -eq 0)
    $perf = Join-Path $tmp 'PERF_Kontrola.json'
    Copy-Item -LiteralPath (Join-Path $seed 'customnpcs/clones/1/PERF_Kontrola.json') -Destination $perf
    Add-HoverProof $perf 'var w = e.npc.getWorld().getTempdata();' 'e.npc' 1
    $clone = [IO.File]::ReadAllText($perf)
    $m = [regex]::Match($clone, '"Script"\s*:\s*("(?:\\.|[^"\\])*")')
    [IO.File]::WriteAllText($jsPath, (ConvertFrom-Json $m.Groups[1].Value), (New-Object Text.UTF8Encoding $false))
    & node --check $jsPath
    Check 'injected PERF JavaScript parses' ($LASTEXITCODE -eq 0)

    $good = 'HOVER-PROOF mode=1 flyers=30 selected=30 helpers=30 otherHover=0'
    Check 'valid runtime proof accepted' (Test-HoverProof $good 1 30)
    foreach ($bad in @('', $good.Replace('mode=1', 'mode=0'), $good.Replace('flyers=30', 'flyers=29'),
            $good.Replace('selected=30', 'selected=0'), $good.Replace('helpers=30', 'helpers=0'),
            $good.Replace('otherHover=0', 'otherHover=6'))) {
        Check 'missing/corrupted runtime proof rejected' (-not (Test-HoverProof $bad 1 30))
    }
    Check 'cost requires before and after proofs' (-not (Test-HoverProof $good 1 30 2))
    Check 'two cost proofs accepted' (Test-HoverProof ($good + "`n" + $good) 1 30 2)
} finally {
    # Only the verified temporary directory created by this script is removed.
    $resolved = [IO.Path]::GetFullPath($tmp)
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
        -not ([IO.Path]::GetFileName($resolved)).StartsWith('hover-harness-')) { throw 'Unsafe temporary path.' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
if ($failures.Count) { throw ('Hover harness failed: ' + ($failures -join '; ')) }
Write-Host 'M4.3 HOVER HARNESS PASS'
