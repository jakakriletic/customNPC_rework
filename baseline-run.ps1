# M2.6 - baseline originala: vse celice M2.4 po protokolu, v ponovitvah, z GC in alokacijami.
# Scenarij in razlaga: docs/scenariji/M2.6-baseline.md
#
# Zakaj posebna skripta in ne ponovitve-run.ps1: ta ponovi ENO celico. Baseline ima devet
# celic; ce bi vsaka tekla v svojem serverju, bi bil zagon 27 zagonov serverja in trikrat
# daljsi. Ena ponovitev je zato en cel .\perf-run.ps1 (vse celice, en server) v svezem
# svetu (D-016); ta skripta pozene N ponovitev in jih zdruzi po celicah.
#
# Protokol (docs/01-ARHITEKTURA.md, razdelek 7): 2 min ogrevanja, 5 min merjenja,
# vsaj 3 ponovitve. Privzeto trajanje: 3 x ~70 min = ~3,5 ure.
#
# Zagon:
#     .\baseline-run.ps1                          # po protokolu
#     .\baseline-run.ps1 -Razprseno               # spawn razprsen (glej scenarij, odlocitev D-017)
#     .\baseline-run.ps1 -Ponovitev 2 -Seconds 60 -WarmupSeconds 20   # preverba skripte, ~45 min
#     .\baseline-run.ps1 -Privzeto 1 -Primerjaj audit\m26-baseline-2026-10-05-1426.json   # M10.2
#
# M10.2 (D-096, Q242): -Privzeto 1 postavi ORG_GRADLE_PROJECT_rwPrivzeto=1 kot matrika-run.ps1, zato
# runServer tece s stikali Rw* na privzetih vrednostih kode; vsaka ponovitev mora imeti v logu
# serverja vrstico 'RWPRIVZETO 1 ' (B4). -Primerjaj doda primerjavo z obstojecim baselinom:
# odtis celic (B5) in alarm p95 MSPT (mediana +5 % in razpona locena). Alarm je podatek za
# sodbo paketa, ne izhodna koda.
#
# Izid:
#     audit\m26-baseline-<cas>\p<i>\<varianta>-<N>.json   zapisi celic vseh ponovitev
#     audit\m26-baseline-<cas>.json                        mediane in razponi (za A/B v M3-M5)
#     docs\meritve\baseline-<datum>.md                     tabela za cloveka

param([int]$Ponovitev = 3, [int]$Seconds = 300, [int]$WarmupSeconds = 120, [switch]$Razprseno,
      [string[]]$Variants = @('idle', 'boj', 'skripte'), [Alias('Counts')][string[]]$CountsIn = @('50', '200', '500'),
      [switch]$Vztrajaj, [switch]$AcceptEula,
      [ValidateSet(-1, 0, 1)][int]$Privzeto = -1, [string]$Primerjaj = '')

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$audit = Join-Path $root 'audit'
. (Join-Path $root 'meritve-lib.ps1')

# Velicine, ki gredo v tabelo baseline. Vse ostale so v .json.
$tabela = @(
    @{ K = 'mspt.p50';          Ime = 'MSPT p50' },
    @{ K = 'mspt.p95';          Ime = 'MSPT p95' },
    @{ K = 'mspt.p99';          Ime = 'MSPT p99' },
    @{ K = 'mspt.brezSave.p99'; Ime = 'p99 brez save' },
    @{ K = 'mspt.max';          Ime = 'max' },
    @{ K = 'npc.us';            Ime = 'us/NPC' },
    @{ K = 'ticki.nad50ms';     Ime = 'ticki >50 ms' },
    @{ K = 'gc.msNaS';          Ime = 'GC ms/s' },
    @{ K = 'alok.MBnaS';        Ime = 'alok. MB/s' }
)
# Odtis, ki se med ponovitvami ne sme razlikovati (D-016). 'celica' in 'celic' sta del
# odtisa, ker je vrstni red celic v istem serverju del pogojev meritve.
$odtisKljuci = @('razprseno', 'varianta', 'npc', 'sekund', 'ogrevanje', 'obroc', 'celica', 'celic', 'mrezaX', 'mrezaZ', 'mrezaSirina')

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }
$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}
function F([double]$v) {
    if ([double]::IsNaN($v)) { return 'n/a' }
    if ([Math]::Abs($v) -ge 100) { return ('{0:N0}' -f $v) }
    if ([Math]::Abs($v) -ge 10) { return ('{0:N1}' -f $v) }
    return ('{0:N2}' -f $v)
}

$prejsnjiPrivzeto = $env:ORG_GRADLE_PROJECT_rwPrivzeto
if ($Privzeto -ge 0) { $env:ORG_GRADLE_PROJECT_rwPrivzeto = [string]$Privzeto }
else { Remove-Item Env:ORG_GRADLE_PROJECT_rwPrivzeto -ErrorAction SilentlyContinue }

try {
    Step 1 'Priprava'
    Write-Host ("  stikala: {0}" -f $(switch ($Privzeto) { 1 { 'privzete vrednosti kode (-Privzeto 1)' } 0 { 'config dev\run (-Privzeto 0)' } default { 'config dev\run (brez preglasitve)' } }))
    $ref = $null
    if ($Primerjaj -ne '') {
        $refPot = if ([System.IO.Path]::IsPathRooted($Primerjaj)) { $Primerjaj } else { Join-Path $root $Primerjaj }
        Check ("primerjalni baseline obstaja ({0})" -f $Primerjaj) (Test-Path $refPot)
        if ($failures.Count) { throw 'Manjka primerjalni baseline.' }
        $ref = Get-Content -Raw -LiteralPath $refPot | ConvertFrom-Json
    }
    # M2.6b: 27. 9. je drug baseline (zapis -1231) stekel med prvim (-1230). Lasten zaklep,
    # ker perf-run.ps1 med ponovitvami svoj zaklep sprosti (vmes tece testworld.ps1).
    $zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.baseline.lock') -Kdo 'baseline-run'
    Check 'zaklep baselina (.baseline.lock)' $true
    $tuji0 = @(Get-TujiJava)
    Check ("P9: pred zagonom ne tece tuj gradle build ali Minecraft" + $(if ($tuji0.Count) { ' - ' + ($tuji0 -join '; ') } else { '' })) ($tuji0.Count -eq 0)
    if ($failures.Count) { throw 'Baseline se ne zacne ob tujem java procesu (meril bi ga skupaj s serverjem).' }
    # '-Counts 50,500' iz ukazne vrstice (-File) pride kot en niz; brez tega bi bil 50500.
    $Variants = @($Variants | ForEach-Object { "$_" -split ',' } | ForEach-Object { $_.Trim().ToLower() } | Where-Object { $_ -ne '' })
    $Counts = [int[]]@($CountsIn | ForEach-Object { "$_" -split ',' } | Where-Object { $_.Trim() -ne '' } | ForEach-Object { [int]$_.Trim() } | Sort-Object)
    if ($Ponovitev -lt 1) { throw 'Ponovitev mora biti vsaj 1.' }
    if ($Ponovitev -lt 3) { Write-Host ("  OPOZORILO {0} ponovitev je manj od protokola (3); izid ni baseline, samo preverba." -f $Ponovitev) }
    $psExe = (Get-Process -Id $PID).Path
    if ([string]::IsNullOrWhiteSpace($psExe)) { $psExe = 'powershell.exe' }
    $testworld = Join-Path $root 'testworld.ps1'
    $perf      = Join-Path $root 'perf-run.ps1'
    Check 'testworld.ps1 in perf-run.ps1 obstajata' ((Test-Path $testworld) -and (Test-Path $perf))
    if ($failures.Count) { throw 'Manjka skripta.' }
    $cells = @(); foreach ($v in $Variants) { foreach ($n in ($Counts | Sort-Object)) { $cells += ('{0}-{1}' -f $v, $n) } }
    $stamp = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $serija = Join-Path $audit ("m26-baseline-{0}" -f $stamp)
    New-Item -ItemType Directory -Force -Path $serija | Out-Null
    $min = [math]::Ceiling($Ponovitev * ($cells.Count * ($Seconds + $WarmupSeconds + 30) + 300) / 60)
    Write-Host ("  {0} ponovitev x {1} celic, ogrevanje {2} s, merjenje {3} s, spawn {4}; ocena ~{5} min" -f `
        $Ponovitev, $cells.Count, $WarmupSeconds, $Seconds, $(if ($Razprseno) { 'razprsen' } else { 'naenkrat' }), $min)
    Write-Host ("  serija: {0}" -f $serija)

    $zagoni = @()
    for ($i = 1; $i -le $Ponovitev; $i++) {
        Step ("2.$i") ("Ponovitev {0} od {1}" -f $i, $Ponovitev)
        Write-Host '  reset sveta (.\testworld.ps1) ...'
        & $testworld | ForEach-Object { Write-Host "    $_" }
        $dir = Join-Path $serija ("p{0}" -f $i)
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $log = Join-Path $serija ("p{0}.log" -f $i)
        $args2 = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $perf),
                   '-Seconds', $Seconds, '-WarmupSeconds', $WarmupSeconds,
                   '-Variants', ($Variants -join ','), '-Counts', (($Counts | Sort-Object) -join ','),
                   '-SerijaDir', ('"{0}"' -f $dir))
        if ($Razprseno) { $args2 += '-Razprseno' }
        if ($AcceptEula) { $args2 += '-AcceptEula' }
        $t0 = Get-Date
        $p = Start-Process -FilePath $psExe -ArgumentList $args2 -PassThru -Wait -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        $minut = ((Get-Date) - $t0).TotalMinutes
        Write-Host ("  izhodna koda {0}, {1:N1} min, izpis {2}" -f $p.ExitCode, $minut, $log)
        $srvLog = Join-Path $audit 'm24-perf.log'
        $privzetoVrstica = ''
        if ((Test-Path $srvLog) -and (Get-Item $srvLog).LastWriteTime -ge $t0) {
            Copy-Item $srvLog (Join-Path $serija ("p{0}-server.log" -f $i)) -Force
            if ($Privzeto -ge 0) {
                $v = Select-String -LiteralPath $srvLog -SimpleMatch ("RWPRIVZETO {0} " -f $Privzeto) | Select-Object -First 1
                if ($v) { $privzetoVrstica = ($v.Line -replace '^.*(RWPRIVZETO)', '$1'); Write-Host ("  {0}" -f $privzetoVrstica) }
            }
        }
        $zagoni += [pscustomobject]@{ I = $i; Koda = $p.ExitCode; Dir = $dir; Privzeto = $privzetoVrstica }
        if ($p.ExitCode -ne 0 -and -not $Vztrajaj) {
            Write-Host '  Ponovitev je padla; naslednje so izpuscene (-Vztrajaj jih vseeno pozene).'
            break
        }
    }

    Step 3 'B1-B3: ponovitve in odtis'
    foreach ($z in $zagoni) { Check ("B1: ponovitev {0} je uspela (koda {1})" -f $z.I, $z.Koda) ($z.Koda -eq 0) }
    Check ("B1: ponovitev je {0} (zahtevano {1})" -f $zagoni.Count, $Ponovitev) ($zagoni.Count -eq $Ponovitev)
    if ($Privzeto -ge 0) {
        foreach ($z in $zagoni) { Check ("B4: log serverja ponovitve {0} ima 'RWPRIVZETO {1} '" -f $z.I, $Privzeto) ($z.Privzeto -ne '') }
    }

    $zdruzeno = [ordered]@{}
    foreach ($c in $cells) {
        $zapisi = @()
        foreach ($z in $zagoni) {
            $pot = Join-Path $z.Dir ("{0}.json" -f $c)
            try { $zapisi += (Read-MeritevJson $pot) } catch { Check ("B2: celica {0} v ponovitvi {1} ima zapis - {2}" -f $c, $z.I, "$_") $false }
        }
        if ($zapisi.Count -eq 0) { continue }
        Check ("B2: celica {0} ima zapis v vseh {1} ponovitvah in vsi so uspesni" -f $c, $zagoni.Count) `
            (($zapisi.Count -eq $zagoni.Count) -and (@($zapisi | Where-Object { -not $_.uspeh }).Count -eq 0))
        # B3: isti poskus. Odtis se primerja kljuc po kljuc s prvo ponovitvijo.
        $o0 = ConvertTo-Slovar $zapisi[0].odtis
        $razlike = @()
        foreach ($zp in $zapisi) {
            $o = ConvertTo-Slovar $zp.odtis
            foreach ($k in $odtisKljuci) { if ("$($o[$k])" -ne "$($o0[$k])") { $razlike += ("{0}: {1} proti {2}" -f $k, $o[$k], $o0[$k]) } }
        }
        Check ("B3: celica {0} ima v vseh ponovitvah enak odtis" -f $c) ($razlike.Count -eq 0)
        if ($razlike.Count) { $razlike | Select-Object -First 3 | ForEach-Object { Write-Host "      $_" } }

        $vel = [ordered]@{}
        $imena = @(); foreach ($zp in $zapisi) { $imena += (ConvertTo-Slovar $zp.velicine).Keys }
        foreach ($ime in ($imena | Sort-Object -Unique)) {
            $vals = @(); foreach ($zp in $zapisi) { $h = ConvertTo-Slovar $zp.velicine; if ($h.Contains($ime)) { $vals += [double]$h[$ime] } }
            if ($vals.Count -eq 0) { continue }
            $s = Get-Sum $vals
            $vel[$ime] = [ordered]@{ n = $s.N; mediana = $s.Mediana; min = $s.Min; max = $s.Max; razpon = $s.Razpon
                                     rel = $(if ([double]::IsNaN($s.Rel)) { $null } else { [math]::Round($s.Rel, 4) }) }
        }
        $zdruzeno[$c] = [ordered]@{ odtis = $o0; velicine = $vel; nasiceno = $(if ($vel.Contains('nasiceno')) { $vel['nasiceno'].max } else { 0 }) }
    }

    $primerjava = [ordered]@{}
    if ($null -ne $ref) {
        Step '3b' ("B5 in alarm p95: primerjava z {0}" -f $Primerjaj)
        foreach ($c in $zdruzeno.Keys) {
            $rc = $ref.celice.$c
            if ($null -eq $rc) { Check ("B5: celica {0} je v primerjalnem baselinu" -f $c) $false; continue }
            $ro = ConvertTo-Slovar $rc.odtis; $ko = $zdruzeno[$c].odtis
            $razlike = @(); foreach ($k in $odtisKljuci) { if ("$($ko[$k])" -ne "$($ro[$k])") { $razlike += ("{0}: {1} proti {2}" -f $k, $ko[$k], $ro[$k]) } }
            Check ("B5: celica {0} ima enak odtis kot primerjalni baseline" -f $c) ($razlike.Count -eq 0)
            if ($razlike.Count) { $razlike | Select-Object -First 3 | ForEach-Object { Write-Host "      $_" } }
            $k95 = $zdruzeno[$c].velicine['mspt.p95']; $r95 = $rc.velicine.'mspt.p95'
            if ($null -eq $k95 -or $null -eq $r95) { continue }
            $rel = if ([double]$r95.mediana -gt 0) { ([double]$k95.mediana / [double]$r95.mediana) - 1 } else { [double]::NaN }
            $nad5 = ($rel -gt 0.05); $loceno = ([double]$k95.min -gt [double]$r95.max)
            $alarm = ($nad5 -and $loceno)
            $primerjava[$c] = [ordered]@{ kandidat = $k95.mediana; kMin = $k95.min; kMax = $k95.max; baseline = $r95.mediana
                                          bMin = $r95.min; bMax = $r95.max; rel = [math]::Round($rel, 4); nad5 = $nad5; loceno = $loceno; alarm = $alarm }
            Write-Host ("  {0,-12} p95 {1} ({2}-{3}) proti {4} ({5}-{6}): {7:+0.0;-0.0} %{8}" -f $c, (F $k95.mediana), (F $k95.min), (F $k95.max),
                (F $r95.mediana), (F $r95.min), (F $r95.max), (100 * $rel), $(if ($alarm) { '  ALARM' } elseif ($nad5) { '  nad 5 %, razpona se prekrivata' } else { '' }))
        }
    }

    Step 4 'Zapis baseline'
    $jsonPot = Join-Path $audit ("m26-baseline-{0}.json" -f $stamp)
    $out = [ordered]@{ shema = 1; paket = $(if ($Privzeto -eq 1) { 'M10.2' } else { 'M2.6' }); privzeto = $Privzeto; primerjaj = $Primerjaj; primerjava = $primerjava; zagon = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'); ponovitev = $zagoni.Count
                       sekund = $Seconds; ogrevanje = $WarmupSeconds; razprseno = $(if ($Razprseno) { 1 } else { 0 })
                       uspeh = ($failures.Count -eq 0); padle = @($failures); celice = $zdruzeno }
    [System.IO.File]::WriteAllText($jsonPot, ((($out | ConvertTo-Json -Depth 8) -replace "`r`n", "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ("  {0}" -f $jsonPot)

    $datum = Get-Date -Format 'yyyy-MM-dd'
    $mdIme = if ($Privzeto -eq 1) { 'baseline-kandidat' } else { 'baseline' }
    $md = Join-Path $root ("docs\meritve\{0}-{1}.md" -f $mdIme, $datum)
    if (Test-Path $md) { $md = Join-Path $root ("docs\meritve\{0}-{1}.md" -f $mdIme, $stamp) }
    $L = @()
    $L += ("# Baseline originala (M2.6), {0}" -f $datum)
    $L += ''
    $L += ('{0} ponovitev, ogrevanje {1} s, merjenje {2} s, spawn {3}. Vsaka ponovitev je cel `.\perf-run.ps1` v svezem svetu. Strojni zapis: `audit/m26-baseline-{4}.json`.' -f `
        $zagoni.Count, $WarmupSeconds, $Seconds, $(if ($Razprseno) { 'razprsen' } else { 'naenkrat' }), $stamp)
    $L += ''
    $L += ('Merila B1-B3 in P1-P9 v vseh ponovitvah: ' + $(if ($failures.Count -eq 0) { '**zelena**' } else { '**PADLA**: ' + ($failures -join '; ') }))
    $L += ''
    $L += 'Celica je `mediana` (razpon min-max). **Razpon je merilni sum:** A/B sme trditi izboljsavo samo, ce je razlika vecja od njega (05-SEJA-PROTOKOL.md).'
    $L += ''
    $L += ('| celica | ' + (($tabela | ForEach-Object { $_.Ime }) -join ' | ') + ' |')
    $L += ('|---|' + (($tabela | ForEach-Object { '---:' }) -join '|') + '|')
    foreach ($c in $zdruzeno.Keys) {
        $cel = @()
        foreach ($t in $tabela) {
            $x = $zdruzeno[$c].velicine[$t.K]
            if ($null -eq $x) { $cel += '-' }
            elseif ($x.n -gt 1) { $cel += ('{0} ({1}-{2})' -f (F $x.mediana), (F $x.min), (F $x.max)) }
            else { $cel += (F $x.mediana) }
        }
        $ime = $c; if ($zdruzeno[$c].nasiceno -gt 0) { $ime += ' *nasiceno*' }
        $L += ('| {0} | {1} |' -f $ime, ($cel -join ' | '))
    }
    $L += ''
    $L += 'MSPT v ms. GC ms/s: cas zbiranja smeti na sekundo meritve. Alok. MB/s: alokacije server niti.'
    if ($primerjava.Count) {
        $L += ''
        $L += ('Primerjava p95 MSPT z `{0}` (alarm: mediana +5 % in razpona locena):' -f $Primerjaj)
        $L += ''
        $L += '| celica | kandidat | baseline | razlika | alarm |'
        $L += '|---|---:|---:|---:|---|'
        foreach ($c in $primerjava.Keys) {
            $x = $primerjava[$c]
            $L += ('| {0} | {1} ({2}-{3}) | {4} ({5}-{6}) | {7:+0.0;-0.0} % | {8} |' -f $c, (F $x.kandidat), (F $x.kMin), (F $x.kMax), (F $x.baseline), (F $x.bMin), (F $x.bMax), (100 * $x.rel), $(if ($x.alarm) { '**da**' } else { 'ne' }))
        }
    }
    [System.IO.File]::WriteAllText($md, (($L -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    $L | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
    Write-Host ("Baseline: {0}" -f $md)
    if ($failures.Count -eq 0) { Write-Host 'M2.6 USPESNO.'; exit 0 }
    Write-Host 'M2.6 NEUSPESNO. Padle preverbe:'; $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.6 NEUSPESNO.'
    exit 1
}
finally {
    if ($null -ne $prejsnjiPrivzeto) { $env:ORG_GRADLE_PROJECT_rwPrivzeto = $prejsnjiPrivzeto }
    else { Remove-Item Env:ORG_GRADLE_PROJECT_rwPrivzeto -ErrorAction SilentlyContinue }
}
