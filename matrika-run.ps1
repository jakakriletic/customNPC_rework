<#
    matrika-run.ps1 - M0.7 prehod 1 integracijske matrike, en zagon.

    Scenarij in pomen vrstic: docs\scenariji\M0.7-integracijska-matrika.md

    Skripta NE pozene nicesar novega. Je orkestrator: po vrsti pozene ze obstojece
    preverbe, ujame njihove izhodne kode in napise porocilo v
    audit\m07-matrika-<datum>.md, da rezultat ni odvisen od tega, kaj je kdo videl
    v konzoli.

    Privzeto se ustavi ob prvi padli stopnji. To je namerno: matrika ni nikoli
    zelena po opustitvi, in nadaljnje stopnje nad pokvarjenim jarjem merijo smeti.

    Zagon:
        .\matrika-run.ps1                 # cel prehod 1
        .\matrika-run.ps1 -SkipBuild      # jar je ze svez, preskoci build
        .\matrika-run.ps1 -AcceptEula     # prvic, ce dev\run\eula.txt se ni sprejet
        .\matrika-run.ps1 -ContinueOnFail # pozeni vse stopnje kljub padcu
        .\matrika-run.ps1 -Only r1        # samo ena stopnja (build,package,testworld,rwdiag,r1,r2)
        .\matrika-run.ps1 -Privzeto 1     # M5.Z: stikala Rw* na privzetih vrednostih kode (D-055)

    M5.Z (D-055): dev\run\config\CustomNpcs.cfg drzi vsa stikala Rw* na 0 (merilni pogoj
    originala). -Privzeto 1 postavi okoljsko spremenljivko ORG_GRADLE_PROJECT_rwPrivzeto=1,
    ki jo podedujejo vsi zagoni serverja v stopnjah; mod tedaj stikala vrne na vrednosti iz
    kode, ne da bi pisal config. -Privzeto 0 samo izpise vrednosti iz configa. Vsaka stopnja z
    zagonom serverja mora imeti v logu vrstico 'RWPRIVZETO <n>', sicer je rdeca - sicer bi
    stopnja tiho tekla v drugem nacinu, kot ga porocilo navaja.
#>
param(
    [switch]$SkipBuild,
    [switch]$AcceptEula,
    [switch]$ContinueOnFail,
    [string[]]$Only,
    [ValidateSet(-1, 0, 1)][int]$Privzeto = -1
)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$audit = Join-Path $root 'audit'
if (-not (Test-Path $audit)) { New-Item -ItemType Directory -Path $audit | Out-Null }

$stamp  = Get-Date -Format 'yyyy-MM-dd-HHmm'
$report = Join-Path $audit ("m07-matrika-{0}.md" -f $stamp)

$prejsnjiPrivzeto = $env:ORG_GRADLE_PROJECT_rwPrivzeto
if ($Privzeto -ge 0) { $env:ORG_GRADLE_PROJECT_rwPrivzeto = [string]$Privzeto }
else { Remove-Item Env:ORG_GRADLE_PROJECT_rwPrivzeto -ErrorAction SilentlyContinue }
$nacin = switch ($Privzeto) { 1 { 'privzete vrednosti kode (-Privzeto 1)' } 0 { 'config dev\run (-Privzeto 0)' } default { 'config dev\run (brez preglasitve)' } }

# --- kontekst zagona ---------------------------------------------------------

function Get-GitInfo {
    try {
        $c = (& git -C $root rev-parse --short HEAD 2>$null)
        $d = (& git -C $root status --porcelain 2>$null)
        if ($d) { return "$c (delovno drevo ni cisto)" }
        return $c
    } catch { return 'ni git repozitorija' }
}

function Get-JarHash([string]$Path) {
    if (-not (Test-Path $Path)) { return 'jar ne obstaja' }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLower()
}

$jarPath = Join-Path $root 'dev\build\libs\CustomNPCs_1.12.2-01Oct19-workspace.jar'

# --- definicija stopenj ------------------------------------------------------
# Vsaka stopnja pove, katere vrstice matrike pokriva. Ce se to spremeni, se
# spremeni tudi tabela v docs\scenariji\M0.7-integracijska-matrika.md.

$stages = @(
    @{ Key='build';     Naziv='Gradle build';            Vrstice='(pogoj)';                 Log='m07-build.log'; Logi=@();
       Akcija={ & (Join-Path $root 'dev.ps1') build } },
    @{ Key='package';   Naziv='verify-package';          Vrstice='(pogoj)';                 Log='m07-package.log'; Logi=@();
       Akcija={ & (Join-Path $root 'verify-package.ps1') } },
    @{ Key='testworld'; Naziv='M0.6 testni svet W1-W8';  Vrstice='IN1, IN2, IN4, IN6, IS1, IL1'; Log='m06-testworld-c.log';
       Logi=@('m06-testworld-a.log', 'm06-testworld-b.log', 'm06-testworld-c.log');
       Akcija={ if ($AcceptEula) { & (Join-Path $root 'testworld-run.ps1') -AcceptEula } else { & (Join-Path $root 'testworld-run.ps1') } } },
    @{ Key='rwdiag';    Naziv='M2.1 diagnostika D1-D7, C1-C6'; Vrstice='IL7';               Log='m21-rwdiag.log'; Logi=@('m21-rwdiag.log');
       Akcija={ if ($AcceptEula) { & (Join-Path $root 'rwdiag-run.ps1') -AcceptEula } else { & (Join-Path $root 'rwdiag-run.ps1') } } },
    @{ Key='r1';        Naziv='M2.2 reprodukcija R1 E1-E6'; Vrstice='IA7';                  Log='m22-r1.log'; Logi=@('m22-r1.log');
       Akcija={ if ($AcceptEula) { & (Join-Path $root 'r1-run.ps1') -AcceptEula } else { & (Join-Path $root 'r1-run.ps1') } } },
    @{ Key='r2';        Naziv='M2.3 reprodukcija R2 L1-L8'; Vrstice='IA4 (zid)';            Log='m23-r2.log'; Logi=@('m23-r2.log');
       Akcija={ if ($AcceptEula) { & (Join-Path $root 'r2-run.ps1') -AcceptEula } else { & (Join-Path $root 'r2-run.ps1') } } }
)

if ($SkipBuild) { $stages = $stages | Where-Object { $_.Key -ne 'build' } }
if ($Only)      { $stages = $stages | Where-Object { $Only -contains $_.Key } }
if (-not $stages) { throw "Nobena stopnja ni izbrana. Veljavni kljuci: build, package, testworld, rwdiag, r1, r2." }

# --- izvedba -----------------------------------------------------------------

Write-Host ''
Write-Host '=== M0.7 prehod 1 integracijske matrike ==='
Write-Host ("Zagon:  {0}" -f $stamp)
Write-Host ("Commit: {0}" -f (Get-GitInfo))
Write-Host ("Stopnje: {0}" -f (($stages | ForEach-Object { $_.Key }) -join ', '))
Write-Host ("Stikala: {0}" -f $nacin)
Write-Host ''

$results = @()
$aborted = $false

foreach ($stage in $stages) {
    if ($aborted) {
        $results += [pscustomobject]@{ Key=$stage.Key; Naziv=$stage.Naziv; Vrstice=$stage.Vrstice;
                                       Izid='preskoceno'; Koda=$null; Sekunde=0; Log=$stage.Log; Napaka='prejsnja stopnja je padla' }
        continue
    }

    Write-Host ("--- {0} ..." -f $stage.Naziv)
    $t0 = Get-Date
    $code = 0
    $err  = ''
    $out  = Join-Path $audit ("m07-{0}-stdout.log" -f $stage.Key)

    try {
        # Tee-Object v PS 5.1 zapise UTF-16 in log je potem nebereljiv v git diffu in grepu.
        if (Test-Path $out) { Remove-Item $out -Force }
        & $stage.Akcija *>&1 | ForEach-Object {
            $line = [string]$_
            Write-Host $line
            [System.IO.File]::AppendAllText($out, $line + [Environment]::NewLine, [System.Text.Encoding]::UTF8)
        }
        if ($null -ne $LASTEXITCODE) { $code = $LASTEXITCODE }
    }
    catch {
        $code = 1
        $err  = "$_"
        $err | Out-File -FilePath $out -Append -Encoding UTF8
    }

    $sec  = [int]((Get-Date) - $t0).TotalSeconds

    # M5.Z: dokaz nacina stikal v vsakem logu serverja te stopnje; kopija loga ostane po zagonu.
    $kopije = @()
    foreach ($ime in $stage.Logi) {
        $pot = Join-Path $audit $ime
        if (-not (Test-Path $pot) -or (Get-Item $pot).LastWriteTime -lt $t0) {
            if ($Privzeto -ge 0) { $code = 1; $err += (" log {0} ni iz tega zagona;" -f $ime) }
            continue
        }
        $kopija = ("m07-matrika-{0}-{1}" -f $stamp, $ime)
        Copy-Item $pot (Join-Path $audit $kopija) -Force
        $kopije += $kopija
        if ($Privzeto -ge 0) {
            $vrstica = Select-String -LiteralPath $pot -SimpleMatch ("RWPRIVZETO {0} " -f $Privzeto) | Select-Object -First 1
            if (-not $vrstica) {
                $code = 1
                $err += (" v {0} ni vrstice 'RWPRIVZETO {1}' (stikala niso v zahtevanem nacinu);" -f $ime, $Privzeto)
            } else {
                Write-Host ("  {0}: {1}" -f $ime, ($vrstica.Line -replace '^.*(RWPRIVZETO)', '$1'))
            }
        }
    }
    $izid = if ($code -eq 0) { 'zeleno' } else { 'rdece' }

    $logOpis = if ($kopije.Count -gt 0) { ($kopije -join ', ') } else { $stage.Log }
    $results += [pscustomobject]@{ Key=$stage.Key; Naziv=$stage.Naziv; Vrstice=$stage.Vrstice;
                                   Izid=$izid; Koda=$code; Sekunde=$sec; Log=$logOpis; Napaka=$err }

    Write-Host ("--- {0}: {1} ({2} s)" -f $stage.Naziv, $izid.ToUpper(), $sec)
    Write-Host ''

    if ($code -ne 0 -and -not $ContinueOnFail) { $aborted = $true }
}

# --- porocilo ----------------------------------------------------------------

# @() je nujen: v PS 5.1 vrne .Count na praznem rezultatu nic, ne 0, in -f izpise prazen niz.
$zeleno = @($results | Where-Object { $_.Izid -eq 'zeleno' }).Count
$rdece  = @($results | Where-Object { $_.Izid -eq 'rdece' }).Count
$presk  = @($results | Where-Object { $_.Izid -eq 'preskoceno' }).Count

$lines = @()
$lines += "# M0.7 prehod 1 - $stamp"
$lines += ''
$lines += '| | |'
$lines += '|---|---|'
$lines += ("| Zagon | $stamp |")
$lines += ("| Commit | {0} |" -f (Get-GitInfo))
$lines += ("| Jar SHA-256 | ``{0}`` |" -f (Get-JarHash $jarPath))
$lines += ("| Stikala | {0} |" -f $nacin)
$lines += ("| Izid | {0} zeleno, {1} rdece, {2} preskoceno |" -f $zeleno, $rdece, $presk)
$lines += ''
$lines += '## Stopnje'
$lines += ''
$lines += '| Stopnja | Vrstice matrike | Izid | Koda | Trajanje | Log |'
$lines += '|---|---|---|---|---|---|'
foreach ($r in $results) {
    $lines += ("| {0} | {1} | **{2}** | {3} | {4} s | ``audit\{5}`` |" -f `
        $r.Naziv, $r.Vrstice, $r.Izid, $(if ($null -eq $r.Koda) { '-' } else { $r.Koda }), $r.Sekunde, $r.Log)
}
$lines += ''
foreach ($r in ($results | Where-Object { $_.Napaka })) {
    $lines += ("**Napaka v stopnji {0}:** {1}" -f $r.Naziv, $r.Napaka)
    $lines += ''
}
$lines += '## Vrstice, ki jih ta prehod ne pokriva'
$lines += ''
$lines += 'Prehod 1 je samo server. Vse vrstice tipa **R** (klient, GUI, dva racunalnika) ostanejo'
$lines += '`preskoceno`, ne `zeleno`. To so IN5, IN9, IN10, IA6, IC1-IC6, IK1-IK5, IO1, IO2 in IO4.'
$lines += 'Za njihovo izvedbo glej prehod 2 v `docs\scenariji\M0.7-integracijska-matrika.md`.'
$lines += ''
$lines += 'Vrstica, ki ni bila izvedena, se ne steje za uspeh.'

[System.IO.File]::WriteAllLines($report, $lines, (New-Object System.Text.UTF8Encoding($false)))

if ($null -ne $prejsnjiPrivzeto) { $env:ORG_GRADLE_PROJECT_rwPrivzeto = $prejsnjiPrivzeto }
else { Remove-Item Env:ORG_GRADLE_PROJECT_rwPrivzeto -ErrorAction SilentlyContinue }

Write-Host '=== POVZETEK ==='
$results | Format-Table Naziv, Izid, Koda, Sekunde -AutoSize | Out-Host
Write-Host ("Porocilo: {0}" -f $report)
Write-Host ''

if ($rdece -gt 0) {
    Write-Host 'PREHOD 1 NEUSPESEN. Poglej porocilo in pripadajoc log.'
    exit 1
}
if ($presk -gt 0) {
    Write-Host 'PREHOD 1 NEPOPOLN - nekatere stopnje niso tekle.'
    exit 1
}
Write-Host 'PREHOD 1 USPESEN - vse stopnje zelene.'
exit 0
