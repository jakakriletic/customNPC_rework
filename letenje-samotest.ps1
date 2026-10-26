# M4.16 - samotest harnessa letenja brez Minecrafta.
#
# Namesti skripto krmilnika v zacasno kopijo LET_Control.json natanko tako kot
# .\letenje-run.ps1, jo pozene nad ponarejenim svetom (dev\testworld\preveri-letenje.js,
# node) in izpis prebere z istimi funkcijami (letenje-lib.ps1) in merili LT1, LT3, LT5-LT8.
# Ujame neujemanje med skripto, postavitvijo, fixture in branjem; zagona v svetu NE
# nadomesti in o obnasanju NPC-jev ne pove nicesar.
#
# Zagon:  .\letenje-samotest.ps1

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$seed = Join-Path $root 'dev\testworld'
. (Join-Path $root 'letenje-lib.ps1')
$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}

$tmp = Join-Path ([IO.Path]::GetTempPath()) ('m416-samotest-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$exitCode = 1
try {
    $node = Get-Command node -ErrorAction SilentlyContinue
    if ($null -eq $node) { throw 'node ni na voljo; samotest ga potrebuje.' }
    & $node.Source --check (Join-Path $seed 'letenje-control.js')
    Check 'skripta krmilnika je sintakticno veljavna (node --check)' ($LASTEXITCODE -eq 0)

    $src = Get-Content -Raw (Join-Path $seed 'letenje-control.js')
    $strings = [regex]::Matches(($src -replace '(?m)^\s*//.*$', ''), "'[^'\r\n]*'") | ForEach-Object { $_.Value }
    Check 'nobena nizovna konstanta nima // ali " (minifikacija bi ju pokvarila)' (@($strings | Where-Object { $_.Contains('//') -or $_.Contains('"') }).Count -eq 0)

    # Clone datoteke so NBT v JSON-u (0b, 1.0f), zato regex in ne ConvertFrom-Json.
    foreach ($n in @('LET_Control', 'LET_Flyer', 'LET_Walker', 'LET_Target')) {
        $j = [IO.File]::ReadAllText((Join-Path $seed "customnpcs\clones\1\$n.json"))
        Check ("fixture {0} ima ime {0}" -f $n) ($j -match ('(?m)^    "Name": "' + $n + '",'))
        $tag = 'm416' + $n.Substring(4).ToLowerInvariant()
        Check ("fixture {0} ima oznako {1}" -f $n, $tag) ($j -match ('"Tags": \[[^\]]*"' + $tag + '"'))
        $mt = if ($n -eq 'LET_Flyer') { 1 } else { 0 }
        Check ("fixture {0} ima MovementType {1} in MovingState 0" -f $n, $mt) (($j -match ('"MovementType": ' + $mt + ',')) -and ($j -match '"MovingState": 0,'))
    }

    $dest = Join-Path $tmp 'LET_Control.json'
    $len = Install-LetController (Join-Path $seed 'customnpcs\clones\1\LET_Control.json') (Join-Path $seed 'letenje-control.js') $dest
    $inst = [regex]::Match([IO.File]::ReadAllText($dest), '"Script"\s*:\s*"((?:\\.|[^"\\])*)"')
    $back = if ($inst.Success) { $inst.Groups[1].Value.Replace('\"', '"').Replace('\\', '\') } else { '' }
    Check ("namescena skripta je enaka minificiranemu viru ({0} znakov)" -f $len) ($back -eq (Get-LetMinifiedScript (Join-Path $seed 'letenje-control.js')))

    $outPath = Join-Path $tmp 'izpis.txt'
    & $node.Source (Join-Path $seed 'preveri-letenje.js') $dest (Join-Path $seed 'letenje-setup-commands.txt') > $outPath
    Check 'ponarejen svet je skripto pognal do konca' ($LASTEXITCODE -eq 0)
    $text = Get-LetMarkerText ([IO.File]::ReadAllText($outPath))
    Write-Host ("  izpis: {0} vrstic" -f @($text -split "`n").Count)

    foreach ($c in (Test-LetMerila $text)) { Check $c.Ime $c.Ok }
    $vel = Get-LetVelicine $text
    $pricakovano = $script:LetFaze.Count * $script:LetProge.Count * 10
    Check ("zapis zagona ima vseh {0} velicin (ima {1})" -f $pricakovano, $vel.Count) ($vel.Count -eq $pricakovano)
    # Mocki znane vrednosti: prost let po ravni crti pride, cilj 48 blokov nima poti.
    Check 'mock: proga P v fazi A prispe 6/6' ($vel['A.P.prispeli'] -eq 6)
    Check 'mock: proga DF nima poti (navig0 0/6)' ($vel['A.DF.navig0'] -eq 0)
    # Drsenje mocka je 0,5 bloka v 50 tickih; vzorec na 10 tickov ga ujame z zamikom do
    # 10 tickov (0,1 bloka), zato je merjena vrednost spodnja meja med 0,4 in 0,5.
    $dr = $vel['A.P.drsenjeMax']
    Check ("mock: drsenje po koncu poti je izmerjeno (0,4-0,5, je {0})" -f $dr) (($dr -ge 0.39) -and ($dr -le 0.501))
    $razmerje = if ($vel['A.P.hitrost'] -gt 0) { $vel['C.P.hitrost'] / $vel['A.P.hitrost'] } else { 0 }
    Check ("mock: hitrost faze C je priblizno polovica faze A (razmerje {0:N3})" -f $razmerje) (($razmerje -ge 0.45) -and ($razmerje -le 0.6))
    Format-LetTabela $text | ForEach-Object { Write-Host $_ }

    if ($failures.Count -eq 0) { Write-Host ''; Write-Host 'M4.16 SAMOTEST USPESEN.'; $exitCode = 0 }
    else { Write-Host ''; Write-Host ("M4.16 SAMOTEST NEUSPESEN: {0} padlih preverb." -f $failures.Count) }
}
catch {
    Write-Host "NAPAKA: $_"
    $exitCode = 1
}
finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}
exit $exitCode
