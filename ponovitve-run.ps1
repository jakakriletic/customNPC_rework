# M2.5c - protokol ponovitev.
#
# Scenarij in razlaga: docs/scenariji/M2.5c-ponovitve.md
#
# Zakaj: en zagon da eno stevilko, ne meritve. Tabela M2.7 (17. 9.) je nastala iz enega
# samega zagona in zato nima razpona - o njej se ne da reci, ali je razlika 1,151 proti
# 1,180 izboljsava ali sum. Protokol (docs/01-ARHITEKTURA.md, razdelek 7) zahteva vsaj
# tri ponovitve; ta skripta jih pozene in zdruzi.
#
# Kaj naredi ena ponovitev:
#   1 resetira svet (.\testworld.ps1 arhivira starega in namesti seme)
#   2 pozene merilno skripto kot svoj proces, z izrecno potjo do zapisa zagona
#   3 prebere zapis (meritve-lib.ps1) in ga da na kup
#
# Reset sveta ni udobje, ampak pogoj. Svet v dev\run\world se med zagoni ohrani z vsemi
# NPC-ji vred; drugi zagon nav-run.ps1 brez reseta ima v svetu 32 NPC-jev namesto 16 in
# meri nekaj drugega. Merilo T4 (odtis) to ujame tudi, ce reset kdaj odpove.
#
# Zagon:
#     .\ponovitve-run.ps1                            # nav, tri ponovitve
#     .\ponovitve-run.ps1 -Scenarij rwdiag
#     .\ponovitve-run.ps1 -Ponovitev 5 -PragSum 0.15
#     .\ponovitve-run.ps1 -BrezResetaSveta           # namenoma brez reseta (za primerjavo)
#     .\ponovitve-run.ps1 -Dodatno @('-Seconds','180')
#
# Skripta sama NE zazene Minecrafta; to naredi merilna skripta, ki jo poklice.

param(
    [ValidateSet('nav', 'rwdiag')][string]$Scenarij = 'nav',
    [int]$Ponovitev = 3,
    [double]$PragSum = 0.20,
    [switch]$BrezResetaSveta,
    [switch]$Vztrajaj,
    [switch]$AcceptEula,
    [string[]]$Dodatno = @(),
    # Tri stikala za preizkus samega protokola brez Minecrafta (.\ponovitve-samotest.ps1):
    # z njimi se ponovitve poganjajo nad poljubno skripto, ki zna zapisati zapis zagona,
    # izhod pa gre v zacasno mapo namesto v audit\.
    [string]$Skripta = '',
    [string]$Paket = '',
    [string]$IzhodnaMapa = ''
)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$audit = if ($IzhodnaMapa -ne '') { $IzhodnaMapa } else { Join-Path $root 'audit' }
New-Item -ItemType Directory -Force -Path $audit | Out-Null

. (Join-Path $root 'meritve-lib.ps1')

$znani = @{
    nav    = [pscustomobject]@{ Skripta = 'nav-run.ps1';    Paket = 'M2.7';  Predpona = 'm27-nav' }
    rwdiag = [pscustomobject]@{ Skripta = 'rwdiag-run.ps1'; Paket = 'M2.1';  Predpona = 'm21-rwdiag' }
}
$izbrani = $znani[$Scenarij]
if ($Skripta -ne '') {
    $izbrani = [pscustomobject]@{
        Skripta  = $Skripta
        Paket    = $(if ($Paket -ne '') { $Paket } else { $izbrani.Paket })
        Predpona = [System.IO.Path]::GetFileNameWithoutExtension($Skripta)
    }
}

$failures = @()
function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host "  OK       $What" }
    else     { Write-Host "  PADLO    $What"; $script:failures += $What }
}

# Vrne pot do gostiteljskega powershella. Podproces mora teci v istem gostitelju kot ta
# skripta: merilne skripte so pisane za Windows PowerShell 5.1 in se v drugem gostitelju
# lahko obnasajo drugace, kar bi se pokazalo kot 'sum', ki to ni.
function Get-PsExe {
    $p = (Get-Process -Id $PID).Path
    if ([string]::IsNullOrWhiteSpace($p)) { return 'powershell.exe' }
    return $p
}

function Format-Stevilka([double]$v) {
    if ([double]::IsNaN($v)) { return 'n/a' }
    if ([Math]::Abs($v) -ge 1000) { return ('{0:N0}' -f $v) }
    if ([Math]::Abs($v - [Math]::Round($v)) -lt 0.0005) { return ('{0:N0}' -f $v) }
    return ('{0:N3}' -f $v)
}

try {
    Step 1 'Pogoji'
    $skripta = $izbrani.Skripta
    if (-not [System.IO.Path]::IsPathRooted($skripta)) { $skripta = Join-Path $root $skripta }
    Check ("merilna skripta obstaja ({0})" -f (Split-Path -Leaf $skripta)) (Test-Path $skripta)

    $testworld = Join-Path $root 'testworld.ps1'
    if (-not $BrezResetaSveta) {
        Check 'testworld.ps1 obstaja (reset sveta)' (Test-Path $testworld)
    } else {
        Write-Host '  ! -BrezResetaSveta: svet se med ponovitvami NE resetira.'
        Write-Host '    Ponovitve bodo primerljive samo, ce scenarij sam pocisti za sabo.'
    }

    # Protokol zahteva vsaj tri ponovitve. Manj ni "hitra razlicica", ampak druga trditev:
    # iz dveh zagonov se razpona ne da lociti od ene odstopajoce meritve.
    Check ("T1: vsaj tri ponovitve (zahtevanih {0})" -f $Ponovitev) ($Ponovitev -ge 3)
    if ($failures.Count -gt 0) { throw 'Pogoji za protokol ponovitev niso izpolnjeni.' }

    $psExe = Get-PsExe
    Write-Host ("  gostitelj: {0}" -f $psExe)
    Write-Host ("  scenarij:  {0} ({1})" -f $Scenarij, $izbrani.Paket)

    $stamp   = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $zagoni  = @()

    for ($i = 1; $i -le $Ponovitev; $i++) {
        Step ("2.$i") ("Ponovitev {0} od {1}" -f $i, $Ponovitev)

        if (-not $BrezResetaSveta) {
            Write-Host '  reset sveta (.\testworld.ps1) ...'
            & $testworld | ForEach-Object { Write-Host "    $_" }
            if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE -and $LASTEXITCODE -gt 0) {
                throw "testworld.ps1 je vrnil $LASTEXITCODE; brez cistega sveta ponovitev nima smisla."
            }
        }

        $jsonPath = Join-Path $audit ("{0}-{1}-p{2}.json" -f $izbrani.Predpona, $stamp, $i)
        $runLog   = Join-Path $audit ("m25c-{0}-{1}-p{2}.log" -f $Scenarij, $stamp, $i)

        $argumenti = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $skripta),
                       '-JsonPath', ('"{0}"' -f $jsonPath))
        if ($AcceptEula) { $argumenti += '-AcceptEula' }
        if ($Dodatno.Count -gt 0) { $argumenti += $Dodatno }

        $zacetek = Get-Date
        Write-Host ("  zagon: {0} {1}" -f $izbrani.Skripta, ($Dodatno -join ' '))
        $p = Start-Process -FilePath $psExe -ArgumentList $argumenti -PassThru -Wait -NoNewWindow `
                           -RedirectStandardOutput $runLog -RedirectStandardError ($runLog + '.err')
        $trajanje = (Get-Date) - $zacetek
        $koda = $p.ExitCode
        Write-Host ("  izhodna koda {0}, trajanje {1:N1} min, izpis {2}" -f $koda, $trajanje.TotalMinutes, $runLog)

        $zapis = $null
        $napaka = ''
        try { $zapis = Read-MeritevJson $jsonPath } catch { $napaka = "$_" }

        $zagoni += [pscustomobject]@{
            Stevilka = $i
            Koda     = $koda
            Minut    = $trajanje.TotalMinutes
            Json     = $jsonPath
            Log      = $runLog
            Zapis    = $zapis
            Napaka   = $napaka
        }

        if ($koda -ne 0 -and -not $Vztrajaj) {
            Write-Host ''
            Write-Host ("  Ponovitev {0} je padla. Naslednje ponovitve so izpuscene (-Vztrajaj jih vseeno pozene)." -f $i)
            break
        }
    }

    Step 3 'T2-T3: zagoni in njihovi zapisi'
    foreach ($z in $zagoni) {
        Check ("T2: ponovitev {0} je uspela (izhodna koda {1})" -f $z.Stevilka, $z.Koda) ($z.Koda -eq 0)
        if ($z.Napaka -ne '') {
            Check ("T3: ponovitev {0} je pustila zapis - {1}" -f $z.Stevilka, $z.Napaka) $false
        } else {
            Check ("T3: ponovitev {0} ima zapis ({1} velicin)" -f $z.Stevilka, (ConvertTo-Slovar $z.Zapis.velicine).Count) $true
            if ($z.Zapis.paket -ne $izbrani.Paket) {
                Check ("T3: ponovitev {0} je zapis paketa {1}, pricakovan {2}" -f $z.Stevilka, $z.Zapis.paket, $izbrani.Paket) $false
            }
            if (-not $z.Zapis.uspeh) {
                Check ("T2: ponovitev {0} se je oznacila kot neuspesna ({1} padlih meril)" -f $z.Stevilka, $z.Zapis.padlih) $false
            }
        }
    }

    $veljavni = @($zagoni | Where-Object { $_.Koda -eq 0 -and $null -ne $_.Zapis })
    if ($veljavni.Count -eq 0) { throw 'Noben zagon ni dal veljavnega zapisa; zdruzevati ni cesa.' }

    Step 4 'T4: odtis prizorisca je v vseh ponovitvah enak'
    # To merilo je razlog, zakaj je odtis locen od velicin. Ce se med ponovitvami spremeni
    # geometrija ali stevilo NPC-jev, so stevilke iz razlicnih poskusov in mediana cez njih
    # je povprecje jabolk in hrusk.
    $osnova = ConvertTo-Slovar $veljavni[0].Zapis.odtis
    $odtisOk = $true
    foreach ($z in $veljavni) {
        $tu = ConvertTo-Slovar $z.Zapis.odtis
        foreach ($k in $osnova.Keys) {
            $a = [string]$osnova[$k]
            $b = [string]$tu[$k]
            if ($a -ne $b) {
                Check ("T4: ponovitev {0} ima drugacen odtis: {1} je '{2}', v prvi '{3}'" -f $z.Stevilka, $k, $b, $a) $false
                $odtisOk = $false
            }
        }
    }
    if ($odtisOk) {
        $opis = (($osnova.Keys | ForEach-Object { "{0}={1}" -f $_, $osnova[$_] }) -join ' ')
        Check ("T4: odtis je enak v vseh {0} ponovitvah ({1})" -f $veljavni.Count, $opis) $true
    }

    Step 5 'T5: vsaka velicina ima vrednost v vseh ponovitvah'
    $imena = @()
    foreach ($z in $veljavni) {
        foreach ($k in (ConvertTo-Slovar $z.Zapis.velicine).Keys) {
            if ($imena -notcontains $k) { $imena += $k }
        }
    }
    $imena = @($imena | Sort-Object)
    $vrednosti = @{}
    $manjkajo  = @()
    foreach ($ime in $imena) {
        $v = @()
        foreach ($z in $veljavni) {
            $s = ConvertTo-Slovar $z.Zapis.velicine
            if ($s.Contains($ime) -and $null -ne $s[$ime]) { $v += [double]$s[$ime] }
        }
        $vrednosti[$ime] = $v
        if ($v.Count -ne $veljavni.Count) { $manjkajo += ("{0} ({1}/{2})" -f $ime, $v.Count, $veljavni.Count) }
    }
    if ($manjkajo.Count -eq 0) {
        Check ("T5: vseh {0} velicin je izmerjenih v vseh ponovitvah" -f $imena.Count) $true
    } else {
        Check ("T5: {0} velicin ni v vseh ponovitvah: {1}" -f $manjkajo.Count, ($manjkajo -join ', ')) $false
    }

    Step 6 'T6: sumni pas'
    # Ta korak nicesar ne razglasi za dobro ali slabo. Izracuna, koliko se velicina
    # premakne, ko se NE spremeni nic - in to je prag, ki ga mora kasnejsi A/B preseci.
    $vrstice = @()
    $glava = ('{0,-34}' -f 'velicina')
    for ($i = 1; $i -le $veljavni.Count; $i++) { $glava += ('{0,10}' -f ("p" + $i)) }
    $glava += ('{0,10}{1,10}{2,9}  {3}' -f 'mediana', 'razpon', 'rel', 'oznaka')
    $vrstice += $glava
    $vrstice += ('-' * $glava.Length)

    $stEnakih = 0; $stStabilnih = 0; $stSumnih = 0
    $sumne = @()
    foreach ($ime in $imena) {
        $v = $vrednosti[$ime]
        if ($v.Count -eq 0) { continue }
        $s = Get-Sum $v
        $oznaka = 'stabilna'
        if ($s.Razpon -eq 0) { $oznaka = 'enaka'; $stEnakih++ }
        elseif ((-not [double]::IsNaN($s.Rel)) -and $s.Rel -le $PragSum) { $stStabilnih++ }
        else { $oznaka = 'SUMNA'; $stSumnih++; $sumne += $ime }

        $vrstica = ('{0,-34}' -f $ime)
        foreach ($x in $v) { $vrstica += ('{0,10}' -f (Format-Stevilka $x)) }
        for ($k = $v.Count; $k -lt $veljavni.Count; $k++) { $vrstica += ('{0,10}' -f '-') }
        $relTxt = if ([double]::IsNaN($s.Rel)) { 'n/a' } else { ('{0:P1}' -f $s.Rel) }
        $vrstica += ('{0,10}{1,10}{2,9}  {3}' -f (Format-Stevilka $s.Mediana), (Format-Stevilka $s.Razpon), $relTxt, $oznaka)
        $vrstice += $vrstica
    }
    $vrstice | ForEach-Object { Write-Host $_ }

    Check ("T6: sumni pas je izracunan ({0} velicin: {1} enakih, {2} stabilnih, {3} sumnih pri pragu {4:P0})" -f `
            $imena.Count, $stEnakih, $stStabilnih, $stSumnih, $PragSum) ($imena.Count -gt 0)

    Step 7 'Porocilo'
    $report = Join-Path $audit ("m25c-{0}-{1}.md" -f $Scenarij, $stamp)
    $head = @()
    $head += ("# M2.5c - ponovitve scenarija {0} ({1}), {2}" -f $Scenarij, $izbrani.Paket, (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $head += ''
    $head += 'Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in'
    $head += ('svez svet ({0}).' -f $(if ($BrezResetaSveta) { 'reset IZKLJUCEN z -BrezResetaSveta' } else { 'reset z .\testworld.ps1' }))
    $head += ''
    $head += ('Merila: {0}' -f $(if ($failures.Count -eq 0) { 'T1-T6 zelena' } else { ("padlo {0}" -f $failures.Count) }))
    $head += ''
    $head += '## Zagoni'
    $head += ''
    $head += '| # | izhodna koda | minut | zapis |'
    $head += '|---|---|---|---|'
    foreach ($z in $zagoni) {
        $head += ('| {0} | {1} | {2:N1} | `{3}` |' -f $z.Stevilka, $z.Koda, $z.Minut, (Split-Path -Leaf $z.Json))
    }
    $head += ''
    $head += '## Odtis (mora biti enak v vseh ponovitvah)'
    $head += ''
    $head += '```'
    foreach ($k in $osnova.Keys) { $head += ("{0,-20} {1}" -f $k, $osnova[$k]) }
    $head += '```'
    $head += ''
    $head += '## Sumni pas'
    $head += ''
    $head += ('Prag: relativni razpon <= {0:P0}. Velicina, oznacena kot SUMNA, se med ponovitvami' -f $PragSum)
    $head += 'premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca'
    $head += '`razpon` - manjsa razlika je sum, ne izboljsava.'
    $head += ''
    $head += '```'
    $head += $vrstice
    $head += '```'
    if ($sumne.Count -gt 0) {
        $head += ''
        $head += '### Sumne velicine'
        $head += ''
        foreach ($ime in $sumne) {
            $s = Get-Sum $vrednosti[$ime]
            $head += ('- `{0}`: mediana {1}, razpon {2} ({3})' -f $ime, (Format-Stevilka $s.Mediana), `
                      (Format-Stevilka $s.Razpon), $(if ([double]::IsNaN($s.Rel)) { 'mediana je 0, razmerje ne obstaja' } else { '{0:P1}' -f $s.Rel }))
        }
    }
    if ($failures.Count -gt 0) {
        $head += ''
        $head += '## Padle preverbe'
        $head += ''
        $failures | ForEach-Object { $head += ("- {0}" -f $_) }
    }
    Set-Content -Path $report -Value $head -Encoding UTF8
    Write-Host ("  porocilo: {0}" -f $report)

    Step 8 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host ("M2.5c T1-T6 USPESNO: {0} ponovitev, tabela ima razpon. Stevilke gredo v docs/meritve/." -f $veljavni.Count)
        if ($stSumnih -gt 0) {
            Write-Host ("Pozor: {0} velicin je sumnih; na njih A/B potrebuje razliko, vecjo od razpona." -f $stSumnih)
        }
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.5c NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.5c NEUSPESNO.'
    exit 1
}
