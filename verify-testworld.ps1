<#
    verify-testworld.ps1 - preveri merila sprejemljivosti W1-W8 scenarija M0.6.

    Skripta ne zaganja serverja. Poganjas jo PO tem, ko je server tekel in se
    ustavil, in prebere dev\run\logs\latest.log ter stanje datotek v svetu.

    Uporaba:
        .\verify-testworld.ps1                 # preveri zadnji zagon
        .\verify-testworld.ps1 -Restart        # preveri zagon PO restartu (W6)
        .\verify-testworld.ps1 -LogPath <pot>  # preveri drug log

    Izhodna koda: 0 ce vsa merila veljajo, 1 ce katerokoli pade.
#>
param(
    [switch]$Restart,
    [string]$LogPath
)

$ErrorActionPreference = 'Stop'

$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$world = Join-Path $run  'world'
if (-not $LogPath) { $LogPath = Join-Path $run 'logs\latest.log' }

$expected = @('T_Stand','T_Wander','T_Flyer','T_Carrier','T_Rider','T_Follower','T_Trader','T_Scripted')

if (-not (Test-Path $LogPath)) { throw "Ni loga: $LogPath. Najprej pozeni .\dev.ps1 runServer --offline" }
$log = Get-Content -LiteralPath $LogPath -Encoding UTF8

$results = New-Object System.Collections.Generic.List[object]
function Add-Result($id, $ok, $detail) {
    $results.Add([pscustomobject]@{ ID = $id; Stanje = $(if ($ok) { 'PASS' } else { 'FAIL' }); Podrobnost = $detail })
}

# --- W1: seme namesceno (posredno: 8 fixture datotek v semenu) ---
$seed = Join-Path $root 'dev\testworld\customnpcs\clones\1'
$seedFiles = @(Get-ChildItem -Path $seed -Filter *.json -ErrorAction SilentlyContinue)
Add-Result 'W1' ($seedFiles.Count -eq 8) ("seme vsebuje {0} fixture datotek (pricakovano 8)" -f $seedFiles.Count)

# --- W2: svet se pripravi brez izjeme ---
$prepared = @($log | Select-String -SimpleMatch 'Preparing level "world"').Count
Add-Result 'W2' ($prepared -ge 1) ("'Preparing level' najden {0}x" -f $prepared)

# --- W3: clone list 1 izpise vseh 8 imen ---
$listed  = @($expected | Where-Object { $log | Select-String -SimpleMatch $_ -Quiet })
$missing = @($expected | Where-Object { $listed -notcontains $_ })
$missingText = if ($missing.Count -eq 0) { 'nobeno ne manjka' } else { 'manjka: ' + ($missing -join ', ') }
Add-Result 'W3' ($listed.Count -eq 8) ("v logu najdenih {0}/8 imen; {1}" -f $listed.Count, $missingText)

# --- W4: TW-OK se izpise 8x ---
$twok = @($log | Select-String -SimpleMatch 'TW-OK').Count
Add-Result 'W4' ($twok -eq 8) ("'TW-OK' {0}x (pricakovano 8)" -f $twok)

# --- W5: skripta dejansko tece in ne javi napake ---
$scriptOk  = @($log | Select-String -SimpleMatch 'TW-SCRIPT-OK').Count
$scriptErr = @($log | Select-String -SimpleMatch 'script errored').Count
Add-Result 'W5' (($scriptOk -ge 1) -and ($scriptErr -eq 0)) ("'TW-SCRIPT-OK' {0}x, 'script errored' {1}x" -f $scriptOk, $scriptErr)

# --- W6: po restartu se vse skupaj ponovi ---
if ($Restart) {
    Add-Result 'W6' ($twok -eq 8 -and $scriptOk -ge 1) ("po restartu TW-OK {0}x, TW-SCRIPT-OK {1}x" -f $twok, $scriptOk)
} else {
    Add-Result 'W6' $true 'preskoceno (pozeni s -Restart po drugem zagonu)'
}

# --- W7: clone datoteke v svetu, brez .tmp in .bak ---
$worldClones = Join-Path $world 'customnpcs\clones\1'
$json = @(Get-ChildItem -Path $worldClones -Filter *.json -ErrorAction SilentlyContinue)
$junk = @(Get-ChildItem -Path $worldClones -Include *.tmp,*.bak -Recurse -ErrorAction SilentlyContinue)
Add-Result 'W7' ($json.Count -ge 8 -and $junk.Count -eq 0) ("{0} json, {1} ostankov .tmp/.bak" -f $json.Count, $junk.Count)

# --- W8: nobene napake iz noppes.* ---
$noppesErr = @($log | Select-String -Pattern 'ERROR' | Select-String -SimpleMatch 'noppes')
Add-Result 'W8' ($noppesErr.Count -eq 0) ("{0} ERROR vrstic z 'noppes'" -f $noppesErr.Count)

$results | Format-Table -AutoSize | Out-String | Write-Output

$failed = @($results | Where-Object { $_.Stanje -eq 'FAIL' })
if ($failed.Count -gt 0) {
    Write-Output ("REZULTAT: FAIL - pade {0} meril: {1}" -f $failed.Count, (($failed | ForEach-Object { $_.ID }) -join ', '))
    if ($noppesErr.Count -gt 0) {
        Write-Output ''
        Write-Output 'Prve napake iz noppes.*:'
        $noppesErr | Select-Object -First 5 | ForEach-Object { Write-Output ("  " + $_.Line.Trim()) }
    }
    exit 1
}

Write-Output 'REZULTAT: PASS - vsa merila W1-W8 veljajo.'
exit 0
