# M2.4 - merilne obremenitve: 50 / 200 / 500 NPC-jev, loceno idle / boj / skripte.
# Scenarij in razlaga: docs/scenariji/M2.4-obremenitve.md
#
# Zakaj: vse dosedanje meritve (M2.1, M2.7) so imele 8-17 NPC-jev. Performance cilji
# projekta (README, M5) pa govorijo o stotinah. Brez tega scenarija M2.6 (baseline) nima
# obremenitve, pod katero bi meril, in nobena optimizacija v M5 nima stevilke 'pred'.
#
# Kaj naredi:
#   zagon A  pribije world spawn na 0 4 0 (isti razlog kot nav-run.ps1)
#   zagon B  postavi svet (perf-setup-commands.txt), nato za vsako celico (varianta x N):
#            1 'noppes slay npcs' in spawn N NPC-jev z 'noppes clone grid'
#            2 PERF_Kontrola: frakciji 1/2 sovrazni, presteje NPC-je, se odstrani
#            3 'rwdiag chunks on' (pogoj meritve M2.1d), ogrevanje, 'rwdiag on'
#            4 merjenje, 'rwdiag dump', 'rwdiag off'
#            5 PERF_Kontrola se enkrat: dokaz, da je obremenitev tekla ves cas
#            6 merila P1-P8 in zapis celice (meritve-lib.ps1)
#
# Celice tecejo v enem serverju, po vrsti: varianta za varianto, N narascajoce. Vrstni red
# je del odtisa; ponovitve (ponovitve-run.ps1) so primerljive samo z enakim vrstnim redom.
#
# Render (frame time klienta) tu NI: dedicated server nima izrisa. Glej scenarij.
#
# Zagon:
#     .\perf-run.ps1                                   # vseh 9 celic po protokolu (~70 min)
#     .\perf-run.ps1 -Seconds 60 -WarmupSeconds 20     # hitra preverba scenarija (~20 min)
#     .\perf-run.ps1 -Variants boj -Counts 200         # ena celica
#     .\ponovitve-run.ps1 -Scenarij perf -Dodatno @('-Variants','boj','-Counts','200')
#     .\perf-run.ps1 -Razprseno                        # spawn po 5 NPC-jev, ~7 tickov narazen (M2.6)
#     .\baseline-run.ps1                               # M2.6: tri ponovitve vseh celic in baseline
#     .\perf-run.ps1 -DovoliTuje                       # P9 samo opozori (hitra preverba ob buildu)
#     .\perf-run.ps1 -Variants idle -Counts 500 -RwTarget 1   # M5-S S1: A/B predzavrnitve tarc (2 = S1 + S2)
#
# -RwTarget N (M5-S S1): po postavitvi sveta poslje '/rwtarget N'; odtis dobi kljuc rwtarget,
# celica velicini rwtarget.predzavrnjenih in rwtarget.zozenih (S2, nacin 2). Brez parametra (-1) se stikala ne dotakne in odtis
# ostane enak baselinu M2.6.
#
# -Jfr (M5-S P1, 8. 10.): v merilnem oknu vsake celice snema JFR (jcmd JFR.start, nastavitve
# 'profile') v audit\jfr\<stamp>-<varianta>-<N>.jfr; po ustavitvi serverja za vsak posnetek
# napise povzetek (dev\tools\jfr-povzetek.js, .md poleg .jfr). Profiler stane CPU in alokacije,
# zato celica dobi v odtis jfr=1 in ni primerljiva z baselinom ali A/B brez profila.
#     .\perf-run.ps1 -Variants idle,boj -Counts 500 -RwTarget 2 -Jfr
#
# -RwPath N (M5-S S14 8. 10., od M5.11 9. 10. S14b): po postavitvi sveta poslje '/rwpath N'
# (1 = pomnjenje tipa vozlisca v sledenju poti). Odtis dobi kljuc rwpath, celica velicine rwpath.*
# (klici, ocene tipa vozlisca, izracuni, primerjave, neujemanja). Nacin 2 = preverba: merilo R1
# (vsaj ena primerjava) in R2 (nic neujemanj); nacin 2 stane dvojno, zato ni za A/B. Brez
# parametra (-1) se stikala ne dotakne. Do 9. 10. sta bila 1/2 S14 (odstranjen), S14b pa 3/4.
#     .\perf-run.ps1 -Variants boj -Counts 500 -RwTarget 2 -RwPath 2   # dokaz enakosti v svetu
#
# -RwPathCas (M5.10, 9. 10.; zahteva -RwPath): poslje se '/rwpath cas 1', ki izmeri ns celega
# vanilla pathFollow in vsakega kandidata (isDirectPathBetweenPoints) ter presteje kandidate na
# pathFollow. Celica dobi rwpath.sledenje.delez (delez povprecnega ticka v pathFollow, %),
# rwpath.kandidat.delez, rwpath.sledenjNaTick, rwpath.kandidatovNaTick, rwpath.kandidat.usNaKlic
# in histogram kandidatov; merilo M1 (meritev je stela). Odtis dobi rwpathCas=1 (dva klica
# nanoTime na klic - ni primerljivo z zagoni brez merjenja).
#     .\perf-run.ps1 -Variants boj -Counts 500 -RwTarget 2 -RwPath 0 -RwPathCas
#
# -RwPathAB a,b (M5.11, parameter -RwPathABNiz z vzdevkom -RwPathAB; zahteva -RwPathCas): A/B nacinov sledenja poti v ISTEM boju. Merilno okno
# se razdeli na okna po -RwPathOkno sekund (privzeto 15), nacini se izmenjujejo a,b,a,b,... (stevilo
# oken mora biti veckratnik stevila nacinov); streznik steje ticke, pathFollow in kandidate loceno po
# nacinu (poNacinu v odgovoru /rwpath). Celica dobi rwpath.ab.<nacin>.* in razmerje us na kandidata
# (drugi / prvi); merili AB1 (vsak nacin je tekel in imel kandidate) in AB2 (kandidatov na tick se
# med nacinoma razlikuje najvec 10 %, pogoj veljavnosti M5.0). MSPT celice je mesanica nacinov.
#     .\perf-run.ps1 -Variants boj -Counts 500 -RwTarget 2 -RwPathAB 0,1 -RwPathCas
#
# -RwCollide N (M5.12, 9. 10.): po postavitvi sveta poslje '/rwcollide N' (onCollide brez opazovalca:
# 1 = preskok, 2 = preverba). Odtis dobi kljuc rwcollide, celica velicine rwcollide.* (klici,
# preskoceni, brez opazovalca, dogodki brez opazovalca). Merilo K1: v nacinu 1/2 je odlocitev tekla.
# V nacinu 1 po meritvi se preverba poslusalca: '/rwcollide poslusalec 1' - preskok se mora ustaviti
# in poslusalec mora dobiti dogodke (K2), po '/rwcollide poslusalec 0' se preskok nadaljuje (K3).
#     .\perf-run.ps1 -Variants idle -Counts 500 -RwTarget 2 -RwCollide 1
#
# Varianta 'nedosegljiva' (M5.9, 10. 10.): kot 'boj' (A frakcija 1, B frakcija 2, N/2 + N/2), le da
# je okoli skupine B pas ograje (minecraft:fence - v 1.12.2 je to hrastova ograja), debel 3 bloke. Ograja je pathfindingu zaprta
# (PathNodeType.FENCE), visoka pa 1,5 bloka, zato vodoravni zarek med ocmi NPC-jev (~1,62) gre nad
# njo: tarca je vidna (fixtura ima DirectLOS 1b) in nedosegljiva hkrati. Obroc in ne vrsta, ker bi
# dolga vrsta NPC-jem blizu konca dala pot okoli; debela 3 bloke, ker obe skupini prideta do ograje
# in bi se cez eno vrsto udarili (doseg 2). Merili U1 (NPC-ji imajo tarco) in U2 (nihce ni
# ranjen - tarca res ni dosegljiva). Prizorisce se pred vsakim spawnom pocisti ('fill ... air' na
# y = 4), zato ograja ne ostane za naslednjo celico.
#     .\perf-run.ps1 -Variants nedosegljiva -Counts 500 -RwTarget 2 -RwPath 0 -RwPathIskanje
#
# -RwPathIskanje (M5.9; zahteva -RwPath): poslje se '/rwpath iskanje 1'; navigator izmeri in
# razvrsti vsako iskanje poti (getPathToPos, skozi katerega gredo vsi klici). Celica dobi
# rwpath.isk.iskanjNaTick, rwpath.isk.usNaIskanje, rwpath.isk.msNaTick, rwpath.isk.delez (% ticka),
# rwpath.isk.maxUs in razvrstitev izidov: rwpath.isk.pomnilnik (vanilla je vrnila obstojeco pot brez
# iskanja), rwpath.isk.brezPoti, rwpath.isk.celih, rwpath.isk.delnih z povprecno razdaljo zadnje
# tocke od cilja. Merilo U3 (merjenje je stelo). Odtis dobi rwpathIskanje=1 (dva klica nanoTime na
# iskanje).
#
# -RwBlink N (M5.13, 10. 10.): po postavitvi sveta poslje '/rwblink N' (prejemniki utripa oci:
# 1 = iskanje po world.playerEntities, 2 = preverba). Odtis dobi kljuc rwblink, celica velicine
# rwblink.* (iskanj, igralcev, prejemnikov, chunkov, primerjav, neujemanj). Merilo B1: v nacinu 1/2
# je iskanje teklo. Nacin 2 = preverba: B2 (vsaj ena primerjava) in B3 (nic neujemanj); nacin 2
# stane dvojno in ni za A/B. V nacinu 1/2 se po merjenju pozene se '/rwblink poskus 64': ker na
# dediciranem strezniku ni igralcev, je seznam prejemnikov vedno prazen in B2/B3 sama nista dokaz,
# zato poskus primerja vanilla poizvedbo in obhod seznama entitet na NPC-jih (64 x 5 kvadrov,
# brez posiljanja) - merili B4 (primerjave so tekle) in B5 (nic neujemanj).
#     .\perf-run.ps1 -Variants idle -Counts 500 -RwTarget 2 -RwBlink 2   # dokaz enakosti v svetu
#
# -RwBlinkCas (M5.13; zahteva -RwBlink): poslje se '/rwblink cas 1', ki izmeri ns okoli iskanja
# prejemnikov. Celica dobi rwblink.usNaIskanje, rwblink.msNaTick, rwblink.delez (delez povprecnega
# ticka), rwblink.iskanjNaTick in rwblink.chunkovNaIskanje. Odtis dobi rwblinkCas=1.
#
# -RwBlinkAB a,b (M5.13, parameter -RwBlinkABNiz z vzdevkom -RwBlinkAB; zahteva -RwBlinkCas): A/B
# nacinov iskanja prejemnikov v ISTEM zagonu, po vzoru M5.11. Merilno okno se razdeli na okna po
# -RwBlinkOkno sekund (privzeto 15), nacini se izmenjujejo a,b,a,b,...; streznik steje ticke in
# iskanja loceno po nacinu (poNacinu v odgovoru /rwblink). Celica dobi rwblink.ab.<nacin>.* in
# razmerje us na iskanje (drugi / prvi); merili AB3 (vsak nacin je tekel in imel iskanja) in AB4
# (iskanj na tick se med nacinoma razlikuje najvec 10 % - utrip je nakljucen, zato je to pogoj
# veljavnosti primerjave). Hkratni -RwPathAB in -RwBlinkAB nista dovoljena. MSPT celice je
# mesanica nacinov.
#     .\perf-run.ps1 -Variants idle -Counts 500 -RwTarget 2 -RwBlinkAB 0,1 -RwBlinkCas
#
# M2.6b: scenarij drzi zaklep .scenarij.lock v korenu (drug zagon v isti mapi takoj pade),
# pred zagonom in vsakih 5 s med celico preveri, da ne tece tuj gradle build ali Minecraft
# (merilo P9), in ob sesutju serverja takoj konca z vzrokom iz loga.
#
# Pred zagonom: .\testworld.ps1 (svez svet). Ta scenarij svet spremeni (pobije fixture M0.6)
# in za seboj pusti oznako dev\run\world\rework-scenarij.txt.

param([string[]]$Variants = @('idle', 'boj', 'skripte'), [Alias('Counts')][string[]]$CountsIn = @('50', '200', '500'),
      [int]$Seconds = 300, [int]$WarmupSeconds = 120, [int]$ChunkRadius = 1,
      [switch]$AcceptEula, [string]$JsonPath = '', [switch]$Razprseno, [string]$SerijaDir = '',
      [switch]$DovoliTuje, [int]$RwTarget = -1, [switch]$Jfr, [int]$RwPath = -1,
      [switch]$RwPathCas, [Alias('RwPathAB')][string]$RwPathABNiz = '', [int]$RwPathOkno = 15, [int]$RwCollide = -1,
      [switch]$RwPathIskanje, [int]$RwBlink = -1, [switch]$RwBlinkCas, [Alias('RwBlinkAB')][string]$RwBlinkABNiz = '', [int]$RwBlinkOkno = 15)

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$seed   = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

. (Join-Path $root 'meritve-lib.ps1')

# Prizorisce; iste stevilke so opisane v perf-setup-commands.txt in v scenariju.
$gridX     = 48      # zahodni rob mreze
$gridZ     = -12     # severni rob mreze
$gridW     = 25      # NPC-jev v vrsti (os x); N mora biti veckratnik 2*gridW = 50
$bojGap    = 4       # prazne vrste med skupinama A in B (AggroRange 16 jih pokrije)
$kontrolaAt = '60,4,30'
$znaneVariante = @('idle', 'boj', 'skripte', 'nedosegljiva')
# -Razprseno: skupina po 5 NPC-jev na ukaz, ukazi ~7 tickov narazen. 7 je tuje 10, zato
# zaporedni ukazi padejo v vse faze 'ticksExisted % 10' (EntityNPCInterface.java:358).
$razKos   = 5
$razPavza = 50       # ms poleg 300 ms v Send-Command; skupaj ~350 ms = ~7 tickov

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

# Odgovor /rwdiag je v logu dvakrat (konzola in [FINE/CustomNPCs]); stejemo samo konzolo.
function Get-MarkerText([string]$LogPath) {
    $t = Get-LogText $LogPath
    if ($t -eq '') { return '' }
    return (($t -split "`n" | Where-Object { $_ -notmatch '\[FINE/CustomNPCs\]' }) -join "`n")
}

function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        $n = ([regex]::Matches((Get-MarkerText $Srv.Log), [regex]::Escape($Marker))).Count
        if ($n -ge $Count) { return $true }
        # M2.6b: sesut server ne odgovori nikoli vec; cakanje do timeouta bi le skrilo vzrok.
        $sesutje = Get-ServerSesutje (Get-LogText $Srv.Log)
        if ($sesutje -ne '') { throw ("Server se je sesul (cakal na '{0}'): {1}" -f $Marker, $sesutje) }
        if ($Srv.Proc.HasExited) {
            Write-Host ("  ! proces se je koncal, preden se je pojavil marker '{0}' (#{1})" -f $Marker, $Count)
            return $false
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host ("  ! timeout {0} s pri cakanju na '{1}' (#{2})" -f $TimeoutSec, $Marker, $Count)
            return $false
        }
        Start-Sleep -Seconds 1
    }
}

function Wait-ForMarker($Srv, [string]$Marker, [int]$TimeoutSec) {
    return (Wait-ForCount $Srv $Marker 1 $TimeoutSec)
}

# M5-S S1: poslje ukaz rwtarget in vrne stevec predzavrnitev iz novega odgovora (-1 ob napaki).
# Steje odgovore pred ukazom, ker ni znano, ali LogWriter vrstico podvoji.
function Send-RwTarget($Srv, [string]$Cmd) {
    $pred = ([regex]::Matches((Get-MarkerText $Srv.Log), 'RWTARGET nacin=')).Count
    Send-Command $Srv $Cmd
    $ok = Wait-ForCount $Srv 'RWTARGET nacin=' ($pred + 1) 60
    Check ("ukaz '{0}' odgovori" -f $Cmd) $ok
    if (-not $ok) { return -1 }
    $m = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWTARGET nacin=\d+ .*? predzavrnjenih=(\d+)')
    if ($m.Count -eq 0) { return -1 }
    # M5-S S2: stevec poizvedb samo po igralcih (nacin 2); starejsi build ga ne izpise.
    $z = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWTARGET nacin=\d+ .*? zozenih=(\d+)')
    $script:rwZozenih = if ($z.Count -gt 0) { [long]$z[$z.Count - 1].Groups[1].Value } else { -1 }
    return [long]$m[$m.Count - 1].Groups[1].Value
}

# M5-S S14 / M5.11: ukaz /rwpath; vrne slovar stevcev iz zadnjega odgovora ($null, ce ni odgovora).
function Send-RwPath($Srv, [string]$Cmd) {
    $pred = ([regex]::Matches((Get-MarkerText $Srv.Log), 'RWPATH nacin=')).Count
    Send-Command $Srv $Cmd
    $ok = Wait-ForCount $Srv 'RWPATH nacin=' ($pred + 1) 60
    Check ("ukaz '{0}' odgovori" -f $Cmd) $ok
    if (-not $ok) { return $null }
    $m = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWPATH nacin=(\d+) .*? klicev=(\d+) ocen=(\d+) izracunov=(\d+) primerjav=(\d+) neujemanj=(\d+)')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    $r = @{ nacin = [int]$g[1].Value; klicev = [long]$g[2].Value; ocen = [long]$g[3].Value
            izracunov = [long]$g[4].Value; primerjav = [long]$g[5].Value; neujemanj = [long]$g[6].Value }
    # M5.10: merjenje casa (starejsi build teh polj ne izpise)
    $c = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWPATH nacin=\d+ .*? cas=(\d) tickov=(-?\d+) sledenj=(\d+) sledenjNs=(\d+) kandidatov=(\d+) kandidatNs=(\d+) prostih=(\d+) histKand=([\d/]+)')
    if ($c.Count -gt 0) {
        $h = $c[$c.Count - 1].Groups
        $r['cas'] = [int]$h[1].Value; $r['tickov'] = [long]$h[2].Value; $r['sledenj'] = [long]$h[3].Value
        $r['sledenjNs'] = [long]$h[4].Value; $r['kandidatov'] = [long]$h[5].Value; $r['kandidatNs'] = [long]$h[6].Value
        $r['prostih'] = [long]$h[7].Value; $r['histKand'] = $h[8].Value
    }
    # M5.9: stevci iskanja poti (starejsi build teh polj ne izpise)
    $i = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWPATH nacin=\d+ .*? isk=(\d) iskKlicev=(\d+) iskNs=(\d+) iskMaxNs=(\d+) iskPomnilnik=(\d+) iskBrezPoti=(\d+) iskCelih=(\d+) iskDelnih=(\d+) iskDelnaRazdalja=([\d,.]+)')
    if ($i.Count -gt 0) {
        $q = $i[$i.Count - 1].Groups
        $r['isk'] = [int]$q[1].Value; $r['iskKlicev'] = [long]$q[2].Value; $r['iskNs'] = [long]$q[3].Value
        $r['iskMaxNs'] = [long]$q[4].Value; $r['iskPomnilnik'] = [long]$q[5].Value
        $r['iskBrezPoti'] = [long]$q[6].Value; $r['iskCelih'] = [long]$q[7].Value
        $r['iskDelnih'] = [long]$q[8].Value
        $r['iskDelnaRazdalja'] = [double]($q[9].Value -replace ',', '.')
    }
    # M5.11: stevci po nacinu "m:tickov:sledenj:sledenjNs:kandidatov:kandidatNs;..."
    $pn = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWPATH nacin=\d+ .*? poNacinu=([\d:;]*)')
    if ($pn.Count -gt 0) {
        $po = @{}
        foreach ($del in ($pn[$pn.Count - 1].Groups[1].Value -split ';')) {
            if ($del -eq '') { continue }
            $x = $del -split ':'
            $po[[int]$x[0]] = @{ tickov = [long]$x[1]; sledenj = [long]$x[2]; sledenjNs = [long]$x[3]; kandidatov = [long]$x[4]; kandidatNs = [long]$x[5] }
        }
        $r['poNacinu'] = $po
    }
    return $r
}

# M5.12: ukaz /rwcollide; vrne slovar stevcev iz odgovora ($null, ce ni odgovora).
function Send-RwCollide($Srv, [string]$Cmd) {
    $pred = ([regex]::Matches((Get-MarkerText $Srv.Log), 'RWCOLLIDE nacin=')).Count
    Send-Command $Srv $Cmd
    $ok = Wait-ForCount $Srv 'RWCOLLIDE nacin=' ($pred + 1) 60
    Check ("ukaz '{0}' odgovori" -f $Cmd) $ok
    if (-not $ok) { return $null }
    $m = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWCOLLIDE nacin=(\d+) .*? klicev=(\d+) preskocenih=(\d+) brezOpazovalca=(\d+) dogodkovBrezOpazovalca=(\d+) poslusalec=(\d) poslusalecDogodkov=(\d+) opazovalec=(\d)')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    return @{ nacin = [int]$g[1].Value; klicev = [long]$g[2].Value; preskocenih = [long]$g[3].Value
              brezOpazovalca = [long]$g[4].Value; dogodkovBrezOpazovalca = [long]$g[5].Value
              poslusalec = [int]$g[6].Value; poslusalecDogodkov = [long]$g[7].Value; opazovalec = [int]$g[8].Value }
}

# M5.13: ukaz /rwblink; vrne slovar stevcev iz odgovora ($null, ce ni odgovora).
function Send-RwBlink($Srv, [string]$Cmd) {
    $pred = ([regex]::Matches((Get-MarkerText $Srv.Log), 'RWBLINK nacin=')).Count
    Send-Command $Srv $Cmd
    $ok = Wait-ForCount $Srv 'RWBLINK nacin=' ($pred + 1) 60
    Check ("ukaz '{0}' odgovori" -f $Cmd) $ok
    if (-not $ok) { return $null }
    $m = [regex]::Matches((Get-MarkerText $Srv.Log), 'RWBLINK nacin=(\d+) .*? iskanj=(\d+) igralcev=(\d+) prejemnikov=(\d+) chunkov=(\d+) primerjav=(\d+) neujemanj=(\d+) cas=(\d) tickov=(-?\d+) poNacinu=([\d:;]*)')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    $r = @{ nacin = [int]$g[1].Value; iskanj = [long]$g[2].Value; igralcev = [long]$g[3].Value
            prejemnikov = [long]$g[4].Value; chunkov = [long]$g[5].Value; primerjav = [long]$g[6].Value
            neujemanj = [long]$g[7].Value; cas = [int]$g[8].Value; tickov = [long]$g[9].Value }
    $po = @{}
    foreach ($del in ($g[10].Value -split ';')) {
        if ($del -eq '') { continue }
        $x = $del -split ':'
        $po[[int]$x[0]] = @{ tickov = [long]$x[1]; iskanj = [long]$x[2]; ns = [long]$x[3]; prejemnikov = [long]$x[4] }
    }
    $r['poNacinu'] = $po
    return $r
}

# M5-S P1: PID java procesa serverja (GradleStartServer) v drevesu nasega zagona.
function Get-ServerJavaPid {
    $drevo = Get-DrevoProcesov @($srv.Proc.Id)
    foreach ($p in @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue)) {
        if ($drevo.ContainsKey([int]$p.ProcessId) -and ("$($p.CommandLine)" -match 'GradleStartServer')) { return [int]$p.ProcessId }
    }
    return -1
}

# M5-S P1: jcmd ukaz serverju; vrne izpis kot en niz.
function Invoke-Jcmd([int]$JavaPid, [string[]]$JcmdArgs) {
    $jcmd = Join-Path $env:JAVA_HOME 'bin\jcmd.exe'
    # Stderr nativnega ukaza bi pod 'Stop' postal koncna napaka; izpis preverimo sami.
    $ErrorActionPreference = 'Continue'
    return ((& $jcmd $JavaPid @JcmdArgs 2>&1 | Out-String))
}

function Start-DevServer([string]$Tag = '') {
    $name = if ($Tag -eq '') { 'm24-perf.log' } else { ('m24-perf-{0}.log' -f $Tag) }
    $outLog = Join-Path $audit $name
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName         = $env:ComSpec
    $psi.Arguments        = "/c gradlew.bat runServer --offline --no-daemon --console=plain > `"$outLog`" 2>&1"
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute  = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow   = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    return [pscustomobject]@{ Proc = $proc; Log = $outLog }
}

function Send-Command($srv, [string]$Cmd) {
    $srv.Proc.StandardInput.WriteLine($Cmd)
    $srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}

# M5.13 (10. 10.): prvi ukaz po zagonu serverja se lahko izgubi. Vhod gre PowerShell -> cmd.exe
# -> gradlew.bat -> gradle (ki zaradi org.gradle.jvmargs tudi z --no-daemon forka build JVM) ->
# javaexec serverja; gradlov posredovalnik vhoda vrstico, zapisano preden se prikljuci, pogoltne,
# do serverja pride samo konec vrstice in ta odgovori "Unknown command". Zato se vhod po zagonu
# ogreje z 'list', dokler server ne odgovori; vse naslednje vrstice pridejo.
function Prime-ServerInput($srv, [int]$Tries = 6, [int]$TimeoutSec = 5) {
    $marker = 'players online'
    for ($i = 0; $i -lt $Tries; ++$i) {
        $pred = ([regex]::Matches((Get-MarkerText $srv.Log), $marker)).Count
        Send-Command $srv 'list'
        if (Wait-ForCount $srv $marker ($pred + 1) $TimeoutSec) { return $true }
    }
    return $false
}

function Send-File($srv, [string]$Path) {
    $sent = 0
    foreach ($line in (Get-Content $Path)) {
        $t = $line.Trim()
        if ($t -eq '' -or $t.StartsWith('#')) { continue }
        Send-Command $srv $t
        $sent++
    }
    Write-Host ("  poslanih ukazov: {0}" -f $sent)
}

function Stop-DevServer($srv, [int]$TimeoutSec = 180) {
    if ($srv.Proc.HasExited) { return $false }
    Send-Command $srv 'stop'
    if (-not $srv.Proc.WaitForExit($TimeoutSec * 1000)) {
        Write-Host '  ! server se ni ustavil sam; ubijam proces in njegove potomce'
        # cmd.exe -> gradle -> java serverja: Kill() bi ubil samo cmd, java bi ostala
        # (zaseden svet, port in CPU v naslednji ponovitvi).
        try { & taskkill.exe /PID $srv.Proc.Id /T /F 2>&1 | Out-Null } catch { }
        try { if (-not $srv.Proc.HasExited) { $srv.Proc.Kill() } } catch { }
        return $false
    }
    return $true
}

$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}

function Get-Prop([string]$File, [string]$Key) {
    $line = @(Get-Content $File | Where-Object { $_ -match ('^' + [regex]::Escape($Key) + '\s*=') })
    if ($line.Count -eq 0) { return '' }
    return ($line[0] -replace ('^' + [regex]::Escape($Key) + '\s*=\s*'), '').Trim()
}

# Zadnji izpis PERF-KONTROLA -> psobject; $null, ce ga ni.
function Read-Kontrola([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath),
        'PERF-KONTROLA idle=(\d+) bojA=(\d+) bojB=(\d+) skripte=(\d+) drugi=(\d+) cilj=(\d+) ranjenih=(\d+) skriptTickov=(\d+)')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    return [pscustomobject]@{
        Idle = [int]$g[1].Value; BojA = [int]$g[2].Value; BojB = [int]$g[3].Value
        Skripte = [int]$g[4].Value; Drugi = [int]$g[5].Value; Cilj = [int]$g[6].Value
        Ranjenih = [int]$g[7].Value; SkriptTickov = [long]$g[8].Value
    }
}

function Read-Frakcije([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath), 'PERF-FRAKCIJE a>b=(\w+) b>a=(\w+)')
    if ($m.Count -eq 0) { return $false }
    $g = $m[$m.Count - 1].Groups
    return ($g[1].Value -eq 'true' -and $g[2].Value -eq 'true')
}

# Zadnji odgovor 'rwdiag chunks ...' -> psobject; $null, ce ga ni.
function Read-Chunks([string]$LogPath) {
    $t = Get-MarkerText $LogPath
    $m = [regex]::Matches($t, 'RWDIAG-CHUNKS stanje=(\w+) chunki=(\d+) tiketi=(\d+)(?: obroc=\d+ zavrnjeni=(\d+))?(?: npc=(\d+))?')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    return [pscustomobject]@{
        Stanje = $g[1].Value; Chunki = [int]$g[2].Value; Tiketi = [int]$g[3].Value
        Zavrnjeni = $(if ($g[4].Success) { [int]$g[4].Value } else { -1 })
        Npc = $(if ($g[5].Success) { [int]$g[5].Value } else { -1 })
    }
}

# Zadnji 'RWDIAG-DUMP logs\rwdiag\x.txt' -> absolutna pot do .json posnetka.
function Read-DumpJsonPath([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath), 'RWDIAG-DUMP (\S+\.txt)')
    if ($m.Count -eq 0) { return $null }
    $rel = $m[$m.Count - 1].Groups[1].Value.Trim()
    $p = if ([System.IO.Path]::IsPathRooted($rel)) { $rel } else { Join-Path $run $rel }
    return ($p -replace '\.txt$', '.json')
}

function Get-Dist($Dump, [string]$Name) {
    foreach ($d in $Dump.distributions) { if ($d.name -eq $Name) { return $d } }
    return $null
}

function Get-Counter($Dump, [string]$Name) {
    foreach ($c in $Dump.counters) { if ($c.name -eq $Name) { return $c } }
    return $null
}

function Ms([double]$ns) { return [math]::Round($ns / 1000000.0, 3) }

# M2.6b (P9): cakanje, med katerim se vsakih 5 s preveri, ali je stekel tuj java proces
# (gradle build ali Minecraft). Get-Process je poceni; ukazno vrstico (CIM) beremo samo
# za PID, ki ga v tej celici se nismo videli. $script:nasiPid je drevo nasega serverja.
$script:nasiPid = @{}
$script:znaniPid = @{}
$script:tujiVCelici = @()
function Wait-SPreverbo([int]$Sekund) {
    $konec = (Get-Date).AddSeconds($Sekund)
    while ($true) {
        foreach ($p in @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)) {
            if ($script:nasiPid.ContainsKey($p.Id) -or $script:znaniPid.ContainsKey($p.Id)) { continue }
            $script:znaniPid[$p.Id] = $true
            $tuj = @(Get-TujiJava -Nasi $script:nasiPid | Where-Object { $_ -like ('{0}:*' -f $p.Id) })
            if ($tuj.Count -gt 0) {
                $script:tujiVCelici += $tuj
                Write-Host ("  ! tuj java proces med celico: {0}" -f $tuj[0])
            }
        }
        $ostane = ($konec - (Get-Date)).TotalSeconds
        if ($ostane -le 0) { break }
        Start-Sleep -Seconds ([int][Math]::Min(5, [Math]::Ceiling($ostane)))
    }
}

# Ena skupina (ime, N NPC-jev od vrste z0 naprej) kot ukazi. Brez -Razprseno en sam
# 'clone grid'; z -Razprseno kosi po $razKos NPC-jev, vsak na svojem mestu mreze.
function Get-GroupCommands([string]$Ime, [int]$N, [int]$Z0) {
    $rows = [int]($N / $gridW)
    if (-not $Razprseno) {
        return @('noppes clone grid {0} 1 {1} {2} {3},4,{4}' -f $Ime, $gridW, $rows, $gridX, $Z0)
    }
    $out = @()
    for ($r = 0; $r -lt $rows; $r++) {
        for ($c = 0; $c -lt ($gridW / $razKos); $c++) {
            $out += ('noppes clone grid {0} 1 {1} 1 {2},4,{3}' -f $Ime, $razKos, ($gridX + $c * $razKos), ($Z0 + $r))
        }
    }
    return $out
}

# M5.9 (varianta 'nedosegljiva'): ograja okoli skupine B, da do nje ni poti. Ograja je
# pathfindingu vedno zaprta (PathNodeType.FENCE), visoka pa je 1,5 bloka, zato vodoravni zarek
# med ocmi NPC-jev (visina ~1,62) gre nad njo in skupini se vidita - tarca je torej vidna
# (DirectLOS 1b v fixturi) in nedosegljiva hkrati. Obroc (ne le vrsta) zato, da obhoda ni:
# dolga vrsta bi NPC-jem blizu konca dala pot okoli (domet iskanja je followRange 32).
function Get-FenceCommands([int]$Rows, [int]$ZB, [string]$Block) {
    # Pas je debel 3 bloke, ne ena vrsta. Zakaj (10. 10., dva padla poskusa U2): obe skupini
    # prideta do ograje z svoje strani, zato sta na koncu samo 2 bloka narazen in udarec gre
    # skozi njo (doseg udarca 2 bloka; odmik ob spawnu ne pomaga, ker se premikata). Pri pasu
    # debeline 3 je najblizja mozna razdalja napadalec-tarca 4 bloke, torej izven dosega.
    $x1 = $gridX - 3
    $x2 = $gridX + $gridW + 2
    $z1 = $ZB - 3
    $z2 = $ZB + $Rows + 2
    return @(
        ('fill {0} 4 {1} {2} 4 {3} {4}' -f $x1, $z1, $x2, ($z1 + 2), $Block),
        ('fill {0} 4 {1} {2} 4 {3} {4}' -f $x1, ($z2 - 2), $x2, $z2, $Block),
        ('fill {0} 4 {1} {2} 4 {3} {4}' -f $x1, $z1, ($x1 + 2), $z2, $Block),
        ('fill {0} 4 {1} {2} 4 {3} {4}' -f ($x2 - 2), $z1, $x2, $z2, $Block)
    )
}

# Ukazi za spawn ene celice. 'noppes clone grid <ime> 1 <sirina x> <vrstic z> x,y,z'
# (CmdClone.grid: zanka x < args[2], z < args[3]; NPC stoji na prvem polnem bloku).
function Get-SpawnCommands([string]$Variant, [int]$N) {
    $cmds = @()
    # M5.9: prizorisce se pred vsakim spawnom pocisti (y = 4 je nad povrsino superflata, zato je
    # tam samo to, kar postavi scenarij - ograja variante 'nedosegljiva'). 'fill' ne premakne entitet.
    $cmds += ('fill {0} 4 {1} {2} 4 {3} minecraft:air' -f ($gridX - 4), ($gridZ - 4), ($gridX + $gridW + 3), ($gridZ + 32))
    if ($Variant -eq 'boj' -or $Variant -eq 'nedosegljiva') {
        $rows = [int](($N / 2) / $gridW)
        $zB = $gridZ + $rows + $bojGap
        if ($Variant -eq 'nedosegljiva') { $cmds += Get-FenceCommands $rows $zB 'minecraft:fence' }
        $cmds += Get-GroupCommands 'PERF_BojA' ($N / 2) $gridZ
        $cmds += Get-GroupCommands 'PERF_BojB' ($N / 2) $zB
    } else {
        $ime = if ($Variant -eq 'idle') { 'PERF_Idle' } else { 'PERF_Skripte' }
        $cmds += Get-GroupCommands $ime $N $gridZ
    }
    return $cmds
}

$srv = $null
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'Parametri in priprava'
    # M2.6b: en scenarij na mapo. Zaklep ostane odprt do konca procesa. Ne v dev\run:
    # ForgeGradle prijavi dev\run kot izhod runServer in Gradle bi zaklenjeno datoteko hashiral.
    $zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'perf-run'
    Check 'zaklep scenarija (.scenarij.lock)' $true
    $tuji0 = @(Get-TujiJava)
    if ($tuji0.Count -gt 0 -and $DovoliTuje) {
        Write-Host ("  OPOZORILO P9: tece tuj gradle build ali Minecraft (-DovoliTuje): {0}" -f ($tuji0 -join '; '))
    } else {
        Check ("P9: pred zagonom ne tece tuj gradle build ali Minecraft" + $(if ($tuji0.Count) { ' - ' + ($tuji0 -join '; ') } else { '' })) ($tuji0.Count -eq 0)
        if ($tuji0.Count -gt 0) { throw 'Tuj java proces bi meril skupaj s serverjem ali prepisal jar. Ustavi ga ali pozeni z -DovoliTuje (ni baseline).' }
    }
    # PowerShell pri klicu iz drugega procesa (ponovitve-run.ps1) poda '-Variants boj,idle'
    # kot en niz; razbijemo ga tu, da se obnasa enako kot seznam.
    $Variants = @($Variants | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().ToLower() } | Where-Object { $_ -ne '' })
    # Isto za stevila: '-Counts 50,200' iz -File pride kot en niz.
    $Counts = [int[]]@($CountsIn | ForEach-Object { "$_" -split ',' } | Where-Object { $_.Trim() -ne '' } | ForEach-Object { [int]$_.Trim() } | Sort-Object)
    foreach ($v in $Variants) {
        if ($znaneVariante -notcontains $v) { throw ("Neznana varianta '{0}'. Znane: {1}" -f $v, ($znaneVariante -join ', ')) }
    }
    foreach ($n in $Counts) {
        if ($n -le 0 -or ($n % (2 * $gridW)) -ne 0) { throw ("Stevilo NPC-jev {0} ni pozitiven veckratnik {1}." -f $n, (2 * $gridW)) }
        if ($n -gt 1000) { throw "Vec kot 1000 NPC-jev ni predvideno (mreza bi segla cez prostor scenarija)." }
    }
    $cells = @()
    foreach ($v in $Variants) { foreach ($n in $Counts) { $cells += [pscustomobject]@{ Varianta = $v; N = $n } } }
    if ($JsonPath -ne '' -and $cells.Count -ne 1) { throw '-JsonPath je dovoljen samo za eno celico (ena varianta, eno stevilo).' }
    # -File poda argumente kot nize, zato seznam nacinov pride kot "0,3" in se razcepi tu.
    [int[]]$RwPathAB = @($RwPathABNiz -split '[,\s]+' | Where-Object { $_.Trim() -ne '' } | ForEach-Object { [int]$_.Trim() })
    if ($RwPathAB.Count -gt 0) {
        if ($RwPathAB.Count -lt 2) { throw '-RwPathAB potrebuje vsaj dva nacina (npr. 0,3).' }
        if (-not $RwPathCas) { throw '-RwPathAB zahteva -RwPathCas (primerjajo se merjeni stevci po nacinu).' }
        if ($RwPath -lt 0) { $RwPath = $RwPathAB[0] }
        if ($RwPath -ne $RwPathAB[0]) { throw '-RwPath mora biti enak prvemu nacinu v -RwPathAB.' }
        if ($RwPathOkno -lt 1) { throw '-RwPathOkno mora biti vsaj 1 s.' }
        $oknaAB = [math]::Floor($Seconds / $RwPathOkno)
        if (($oknaAB -lt $RwPathAB.Count) -or (($oknaAB % $RwPathAB.Count) -ne 0)) {
            throw ("-Seconds {0} / -RwPathOkno {1} = {2} oken; potreben je veckratnik stevila nacinov ({3})." -f $Seconds, $RwPathOkno, $oknaAB, $RwPathAB.Count)
        }
    }
    if ($RwPathCas -and $RwPath -lt 0) { throw '-RwPathCas zahteva -RwPath N (nacin, v katerem se meri; za M5.10 -RwPath 0).' }
    [int[]]$RwBlinkAB = @($RwBlinkABNiz -split '[,\s]+' | Where-Object { $_.Trim() -ne '' } | ForEach-Object { [int]$_.Trim() })
    $oknaBlinkAB = 0
    if ($RwBlinkAB.Count -gt 0) {
        if ($RwPathAB.Count -gt 0) { throw '-RwBlinkAB in -RwPathAB se ne moreta izmenjevati v istem zagonu.' }
        if ($RwBlinkAB.Count -lt 2) { throw '-RwBlinkAB potrebuje vsaj dva nacina (npr. 0,1).' }
        if (-not $RwBlinkCas) { throw '-RwBlinkAB zahteva -RwBlinkCas (primerjajo se merjeni stevci po nacinu).' }
        if ($RwBlink -lt 0) { $RwBlink = $RwBlinkAB[0] }
        if ($RwBlink -ne $RwBlinkAB[0]) { throw '-RwBlink mora biti enak prvemu nacinu v -RwBlinkAB.' }
        if ($RwBlinkOkno -lt 1) { throw '-RwBlinkOkno mora biti vsaj 1 s.' }
        $oknaBlinkAB = [math]::Floor($Seconds / $RwBlinkOkno)
        if (($oknaBlinkAB -lt $RwBlinkAB.Count) -or (($oknaBlinkAB % $RwBlinkAB.Count) -ne 0)) {
            throw ("-Seconds {0} / -RwBlinkOkno {1} = {2} oken; potreben je veckratnik stevila nacinov ({3})." -f $Seconds, $RwBlinkOkno, $oknaBlinkAB, $RwBlinkAB.Count)
        }
    }
    if ($RwBlinkCas -and $RwBlink -lt 0) { throw '-RwBlinkCas zahteva -RwBlink N (nacin, v katerem se meri).' }
    if ($RwPathIskanje -and $RwPath -lt 0) { throw '-RwPathIskanje zahteva -RwPath N (za M5.9 -RwPath 0).' }
    $perCell = $WarmupSeconds + $Seconds + 25
    Write-Host ("  celice: {0}" -f (($cells | ForEach-Object { '{0}/{1}' -f $_.Varianta, $_.N }) -join ', '))
    Write-Host ("  ogrevanje {0} s, merjenje {1} s, obroc {2}; ocena trajanja ~{3} min" -f `
        $WarmupSeconds, $Seconds, $ChunkRadius, [math]::Ceiling(($cells.Count * $perCell + 240) / 60))

    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\perf-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # PERF_Kontrola izpisuje z '/say' prek executeCommand, ki brez command blokov vrze
    # CustomNPCsException (isti razlog kot v nav-run.ps1).
    $propsFile = Join-Path $run 'server.properties'
    $seedProps = Join-Path $seed 'server.properties'
    if (Test-Path $propsFile) {
        $props = @(Get-Content $propsFile)
        if (-not ($props -match '^enable-command-block\s*=\s*true')) {
            if ($props -match '^enable-command-block\s*=') {
                $props = $props -replace '^enable-command-block\s*=.*', 'enable-command-block=true'
            } else {
                $props += 'enable-command-block=true'
            }
            Set-Content -Path $propsFile -Value $props -Encoding ASCII
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa PERF-*)'
        }
        if (Test-Path $seedProps) {
            $mismatch = @()
            foreach ($key in @('level-name', 'level-seed')) {
                $want = Get-Prop $seedProps $key
                $have = Get-Prop $propsFile $key
                if ($want -ne $have) { $mismatch += ("{0} je '{1}', pricakovano '{2}'" -f $key, $have, $want) }
            }
            Check ("dev\run\server.properties je od testnega sveta" + $(if ($mismatch.Count) { ' - ' + ($mismatch -join '; ') } else { '' })) ($mismatch.Count -eq 0)
            if ($mismatch.Count -gt 0) { throw "server.properties je od drugega scenarija. Pozeni najprej .\testworld.ps1, nato ta scenarij." }
        }
    } else {
        throw 'dev\run\server.properties ne obstaja. Pozeni najprej .\testworld.ps1.'
    }

    # Svet, ki ga je ze uporabil drug scenarij, ima druge NPC-je in druge bloke.
    $scenMark = Join-Path $run 'world\rework-scenarij.txt'
    if (Test-Path $scenMark) {
        $prej = (Get-Content $scenMark -Raw).Trim()
        Check ("svet je svez (oznaka pravi: {0})" -f $prej) $false
        throw ("dev\run\world je ze uporabil scenarij '{0}'. Pozeni najprej .\testworld.ps1, nato ta scenarij." -f $prej)
    }
    Check 'svet ni oznacen kot uporabljen' $true

    $srcClones = Join-Path $seed 'customnpcs\clones\1'
    $dstClones = Join-Path $run  'world\customnpcs\clones\1'
    New-Item -ItemType Directory -Force -Path $dstClones | Out-Null
    $copied = 0
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'PERF_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("pet PERF fixture datotek je v svetu (kopiranih {0})" -f $copied) ($copied -eq 5)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones. Pozeni 'python3 dev/testworld/perf-fixture.py'." }

    Step 2 'Zagon A: pribij world spawn na 0 4 0'
    $a = Start-DevServer 'a'
    Check 'server A je dosegel "Done ("' (Wait-ForMarker $a 'Done (' 900)
    if ($failures.Count -eq 0) {
        Check 'standardni vhod serverja A odgovarja (ogrevanje z "list")' (Prime-ServerInput $a)
    }
    if ($failures.Count -eq 0) {
        Send-Command $a 'setworldspawn 0 4 0'
        Check 'world spawn nastavljen' (Wait-ForMarker $a 'Set the world spawn point' 60)
        Send-Command $a 'save-all flush'
        Check 'svet shranjen (A)' (Wait-ForMarker $a 'Saved the world' 180)
    }
    Check 'server A se je cisto ustavil' (Stop-DevServer $a)
    if ($failures.Count -gt 0) { throw "Zagon A ni uspel. Glej $($a.Log)" }

    Step 3 'Zagon B: postavitev sveta'
    $srv = Start-DevServer
    Check 'server B je dosegel "Done ("' (Wait-ForMarker $srv 'Done (' 900)
    if ($failures.Count -eq 0) {
        Check 'standardni vhod serverja B odgovarja (ogrevanje z "list")' (Prime-ServerInput $srv)
    }
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($srv.Log)" }
    $script:nasiPid = Get-DrevoProcesov @($srv.Proc.Id)
    Send-File $srv (Join-Path $seed 'perf-setup-commands.txt')
    Check 'svet shranjen po postavitvi' (Wait-ForMarker $srv 'Saved the world' 120)
    if ($RwTarget -ge 0) {
        $null = Send-RwTarget $srv ("rwtarget {0}" -f $RwTarget)
        Check ("stikalo RwTarget je {0}" -f $RwTarget) ((Get-MarkerText $srv.Log) -match ("RWTARGET nacin={0} " -f $RwTarget))
    }
    if ($RwCollide -ge 0) {
        $rc = Send-RwCollide $srv ("rwcollide {0}" -f $RwCollide)
        Check ("stikalo RwCollide je {0}" -f $RwCollide) (($null -ne $rc) -and ($rc.nacin -eq $RwCollide))
    }
    if ($RwBlink -ge 0) {
        $rb = Send-RwBlink $srv ("rwblink {0}" -f $RwBlink)
        Check ("stikalo RwBlink je {0}" -f $RwBlink) (($null -ne $rb) -and ($rb.nacin -eq $RwBlink))
        if ($RwBlinkCas) {
            $rb = Send-RwBlink $srv 'rwblink cas 1'
            Check 'merjenje prejemnikov utripa vklopljeno (rwblink cas 1)' (($null -ne $rb) -and ($rb['cas'] -eq 1))
        }
    }
    if ($RwPath -ge 0) {
        $rp = Send-RwPath $srv ("rwpath {0}" -f $RwPath)
        Check ("stikalo RwPath je {0}" -f $RwPath) (($null -ne $rp) -and ($rp.nacin -eq $RwPath))
        if ($RwPathCas) {
            $rp = Send-RwPath $srv 'rwpath cas 1'
            Check 'merjenje sledenja poti vklopljeno (rwpath cas 1)' (($null -ne $rp) -and ($rp['cas'] -eq 1))
        }
        if ($RwPathIskanje) {
            $rp = Send-RwPath $srv 'rwpath iskanje 1'
            Check 'merjenje iskanja poti vklopljeno (rwpath iskanje 1)' (($null -ne $rp) -and ($rp['isk'] -eq 1))
        }
    }

    # Pobijanje fixtur M0.6. Vrstni red je bistven: 'noppes slay npcs' samo oznaci isDead,
    # odstrani pa jih sele updateEntities - ta pa brez igralca in brez prisilno nalozenih
    # chunkov 300 tickov po nalaganju neha teci (M2.1d). Zato najprej chunki (svet se
    # zbudi), nato slay. Ce v svetu ni NPC-jev (samo .\testworld.ps1, brez
    # .\testworld-run.ps1), 'chunks on' odgovori stanje=off in ni kaj pobiti.
    # Chunki ostanejo vklopljeni do konca: brez njih bi se svet med celicami spet ustavil
    # in pobiti NPC-ji prejsnje celice bi ostali v loadedEntityList.
    $nKontrola = 0; $nChunks = 0; $nOn = 0; $nOff = 0; $nDump = 0
    Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
    $nChunks++
    $okCh = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60
    Check 'ukaz chunks odgovori (ciscenje)' $okCh
    $ch = if ($okCh) { Read-Chunks $srv.Log } else { $null }
    Write-Host ("  pred ciscenjem: stanje={0} npc={1}" -f $ch.Stanje, $ch.Npc)
    Send-Command $srv 'noppes slay npcs 2000'
    Start-Sleep -Seconds 5
    Set-Content -Path $scenMark -Value ("perf-run {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm')) -Encoding ASCII
    $rows = @()
    $jfrPosnetki = @()
    $stamp = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $idx = 0
    foreach ($cell in $cells) {
        $idx++
        $v = $cell.Varianta; $N = $cell.N
        $cellFailStart = $failures.Count
        $script:znaniPid = @{}
        $script:tujiVCelici = @()
        Step ('4.{0}' -f $idx) ("celica {0}/{1}: {2}, {3} NPC-jev" -f $idx, $cells.Count, $v, $N)

        # M2.6b (5. 10.): 'noppes slay npcs' brez stevila pobije samo NPC-je v radiju 120 blokov
        # od konzole (0,4,0; CmdSlay). V boj-500 se jih je nekaj razbezalo dlje (82 prisilnih
        # chunkov) in 4 so ostali v celici skripte-50. Zato velik radij in preverba, da je svet
        # pred spawnom res prazen (rwdiag chunks steje nalozene NPC-je).
        $prazno = $false
        for ($poskus = 1; ($poskus -le 3) -and (-not $prazno); $poskus++) {
            Send-Command $srv 'noppes slay npcs 2000'
            Start-Sleep -Seconds 3
            Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
            $nChunks++
            $okP = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60
            $chP = if ($okP) { Read-Chunks $srv.Log } else { $null }
            $prazno = ($null -ne $chP) -and ($chP.Npc -eq 0)
            if (-not $prazno) { Write-Host ("  ciscenje: po poskusu {0} ostalo npc={1}" -f $poskus, $chP.Npc) }
        }
        Check ("P0: svet je pred spawnom prazen (npc=0)") $prazno
        $spawnCmds = @(Get-SpawnCommands $v $N)
        if ($Razprseno) {
            Write-Host ("  razprseni spawn: {0} ukazov po {1} NPC-jev, ~{2} s" -f $spawnCmds.Count, $razKos, [math]::Ceiling($spawnCmds.Count * 0.35))
            foreach ($c in $spawnCmds) { Send-Command $srv $c; Start-Sleep -Milliseconds $razPavza }
        } else {
            foreach ($c in $spawnCmds) { Write-Host "  > $c"; Send-Command $srv $c }
        }
        Start-Sleep -Seconds 3

        # M5.9 / U0: ograja mora biti res v svetu. Prvi poskusi 10. 10. so tiho merili navaden
        # boj, ker se blok v 1.12.2 imenuje minecraft:fence in ne oak_fence - 'fill' je odgovoril
        # "There is no such block", scenarij pa tega ni gledal. Preveri se vsaka od stirih stran
        # pasu in da je notranjost (polje skupine B) prosta.
        if ($v -eq 'nedosegljiva') {
            $uRows = [int](($N / 2) / $gridW)
            $uZB = $gridZ + $uRows + $bojGap
            $tocke = @(
                @{ x = ($gridX - 3);            z = ($uZB - 3);            blok = 'minecraft:fence'; kaj = 'severna stran' },
                @{ x = ($gridX - 3);            z = ($uZB + $uRows + 2);   blok = 'minecraft:fence'; kaj = 'juzna stran' },
                @{ x = ($gridX + $gridW + 2);  z = ($uZB - 1);            blok = 'minecraft:fence'; kaj = 'vzhodna stran' },
                @{ x = ($gridX - 1);           z = ($uZB - 1);            blok = 'minecraft:fence'; kaj = 'zapadna stran' },
                @{ x = $gridX;                 z = $uZB;                  blok = 'minecraft:air';   kaj = 'notranjost prosta' }
            )
            $uOk = $true
            foreach ($t in $tocke) {
                $pred = ([regex]::Matches((Get-MarkerText $srv.Log), 'Successfully found the block')).Count
                Send-Command $srv ('testforblock {0} 4 {1} {2}' -f $t.x, $t.z, $t.blok)
                if (-not (Wait-ForCount $srv 'Successfully found the block' ($pred + 1) 30)) {
                    Write-Host ("  ! ograja: {0} ({1},4,{2}) ni {3}" -f $t.kaj, $t.x, $t.z, $t.blok)
                    $uOk = $false
                }
            }
            Check ("U0: pas ograje je v svetu (stiri strani in prosta notranjost pri z={0})" -f $uZB) $uOk
        }

        # P2: pogoj meritve (M2.1d) in hkrati neodvisno stetje NPC-jev. PRED krmilnikom:
        # ce je svet ustavljen (prva celica), PERF_Kontrola brez tega ne bi nikoli tiknil.
        Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
        $nChunks++
        # Brez odgovora se ne bere zadnja vrstica: ta je od prejsnje celice (27. 9. je tako
        # 'npc=500' iz idle-500 izgledal kot neociscen svet, v resnici je server ze padel).
        $okCh = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60
        Check 'ukaz chunks odgovori' $okCh
        $ch = if ($okCh) { Read-Chunks $srv.Log } else { $null }
        $chOk = ($null -ne $ch) -and ($ch.Stanje -eq 'on') -and ($ch.Npc -eq $N) -and ($ch.Zavrnjeni -eq 0)
        Check ("P2: chunki prisilno nalozeni, npc={0} (pricakovano {1}), chunki={2}, zavrnjeni={3}" -f `
            $ch.Npc, $N, $ch.Chunki, $ch.Zavrnjeni) $chOk

        # P1: prava obremenitev je v svetu (in samo ta).
        Send-Command $srv ('noppes clone spawn PERF_Kontrola 1 {0}' -f $kontrolaAt)
        $nKontrola++
        $okK = Wait-ForCount $srv 'PERF-KONTROLA idle=' $nKontrola 90
        Check 'P1: PERF_Kontrola se je oglasil (zacetek)' $okK
        if (-not $okK) { throw 'PERF_Kontrola se ni oglasil; brez tega ni dokaza, kaj je v svetu.' }
        $k0 = Read-Kontrola $srv.Log
        $want = @{ Idle = 0; BojA = 0; BojB = 0; Skripte = 0 }
        if ($v -eq 'idle')    { $want.Idle = $N }
        if ($v -eq 'skripte') { $want.Skripte = $N }
        if ($v -eq 'boj' -or $v -eq 'nedosegljiva') { $want.BojA = $N / 2; $want.BojB = $N / 2 }
        $spawnOk = ($k0.Idle -eq $want.Idle) -and ($k0.BojA -eq $want.BojA) -and ($k0.BojB -eq $want.BojB) -and `
                   ($k0.Skripte -eq $want.Skripte) -and ($k0.Drugi -eq 0)
        Check ("P1: v svetu je natanko obremenitev celice (idle={0} bojA={1} bojB={2} skripte={3} drugi={4})" -f `
            $k0.Idle, $k0.BojA, $k0.BojB, $k0.Skripte, $k0.Drugi) $spawnOk
        if ($v -eq 'boj' -or $v -eq 'nedosegljiva') { Check 'P1: frakciji 1 in 2 sta sovrazni druga drugi' (Read-Frakcije $srv.Log) }
        # Krmilnik se odstrani sam; pocakamo, da ga ni vec, preden se zacne ogrevanje.
        Start-Sleep -Seconds 2

        if ($WarmupSeconds -gt 0) {
            Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
            Wait-SPreverbo $WarmupSeconds
        }

        if ($RwTarget -ge 0) { $null = Send-RwTarget $srv 'rwtarget reset' }
        # M5.11: A/B v istem boju zacne vsako celico v prvem nacinu (prejsnja je lahko koncala v drugem).
        if ($RwPathAB.Count -gt 0) { $null = Send-RwPath $srv ("rwpath {0}" -f $RwPathAB[0]) }
        if ($RwPath -ge 0) { $null = Send-RwPath $srv 'rwpath reset' }
        if ($RwCollide -ge 0) { $null = Send-RwCollide $srv 'rwcollide reset' }
        # M5.13: A/B v istem zagonu zacne vsako celico v prvem nacinu.
        if ($RwBlinkAB.Count -gt 0) { $null = Send-RwBlink $srv ("rwblink {0}" -f $RwBlinkAB[0]) }
        if ($RwBlink -ge 0) { $null = Send-RwBlink $srv 'rwblink reset' }
        Send-Command $srv 'rwdiag on'
        $nOn++
        Check 'merjenje vklopljeno' (Wait-ForCount $srv 'RWDIAG vklopljen' $nOn 60)
        $jfrPot = ''
        if ($Jfr) {
            $javaPid = Get-ServerJavaPid
            Check ("J1: najden java proces serverja (PID {0})" -f $javaPid) ($javaPid -gt 0)
            $jfrDir = Join-Path $audit 'jfr'
            New-Item -ItemType Directory -Force -Path $jfrDir | Out-Null
            $jfrPot = Join-Path $jfrDir ("{0}-{1}-{2}.jfr" -f $stamp, $v, $N)
            $izJ = Invoke-Jcmd $javaPid @('JFR.start', ("name=rw{0}" -f $idx), 'settings=profile', ("filename={0}" -f $jfrPot))
            Check 'J1: JFR snemanje se je zacelo' ($izJ -match 'Started recording')
            if ($izJ -notmatch 'Started recording') { Write-Host ("      {0}" -f $izJ.Trim()) }
        }
        Write-Host ("  merjenje {0} s" -f $Seconds)
        if ($RwPathAB.Count -gt 0) {
            Write-Host ("  A/B sledenja poti: {0} oken po {1} s, nacini {2}" -f $oknaAB, $RwPathOkno, ($RwPathAB -join ','))
            for ($ok = 0; $ok -lt $oknaAB; $ok++) {
                if ($ok -gt 0) { $null = Send-RwPath $srv ("rwpath {0}" -f $RwPathAB[$ok % $RwPathAB.Count]) }
                Wait-SPreverbo $RwPathOkno
            }
            $ostanek = $Seconds - $oknaAB * $RwPathOkno
            if ($ostanek -gt 0) { Wait-SPreverbo $ostanek }
        } elseif ($RwBlinkAB.Count -gt 0) {
            Write-Host ("  A/B prejemnikov utripa: {0} oken po {1} s, nacini {2}" -f $oknaBlinkAB, $RwBlinkOkno, ($RwBlinkAB -join ','))
            for ($ok = 0; $ok -lt $oknaBlinkAB; $ok++) {
                if ($ok -gt 0) { $null = Send-RwBlink $srv ("rwblink {0}" -f $RwBlinkAB[$ok % $RwBlinkAB.Count]) }
                Wait-SPreverbo $RwBlinkOkno
            }
            $ostanek = $Seconds - $oknaBlinkAB * $RwBlinkOkno
            if ($ostanek -gt 0) { Wait-SPreverbo $ostanek }
        } else {
            Wait-SPreverbo $Seconds
        }
        if ($Jfr -and $javaPid -gt 0) {
            $izJ = Invoke-Jcmd $javaPid @('JFR.stop', ("name=rw{0}" -f $idx))
            $jfrOk = ($izJ -match 'Stopped recording') -and (Test-Path $jfrPot) -and ((Get-Item $jfrPot).Length -gt 0)
            Check ("J2: JFR posnetek zapisan ({0})" -f $jfrPot) $jfrOk
            if ($jfrOk) { $jfrPosnetki += $jfrPot }
        }
        $tag = 'm24-{0}-{1}' -f $v, $N
        Send-Command $srv ("rwdiag dump {0}" -f $tag)
        $nDump++
        Check 'posnetek zapisan' (Wait-ForCount $srv 'RWDIAG-DUMP ' $nDump 120)
        $predzavrnjenih = -1
        if ($RwTarget -ge 0) {
            $predzavrnjenih = Send-RwTarget $srv 'rwtarget'
            Write-Host ("  RwTarget={0}: predzavrnjenih kandidatov med merjenjem {1}, zozenih poizvedb {2}" -f $RwTarget, $predzavrnjenih, $script:rwZozenih)
        }
        $rwPathStevci = $null
        if ($RwPath -ge 0) {
            $rwPathStevci = Send-RwPath $srv 'rwpath'
            if ($null -ne $rwPathStevci) {
                Write-Host ("  RwPath={0}: klicev {1}, ocen tipa vozlisca {2}, izracunov {3}, primerjav {4}, neujemanj {5}" -f `
                    $RwPath, $rwPathStevci.klicev, $rwPathStevci.ocen, $rwPathStevci.izracunov, $rwPathStevci.primerjav, $rwPathStevci.neujemanj)
                if ($RwPath -eq 2) {
                    Check ("R1: preverba je primerjala original in pomnjenje ({0} primerjav)" -f $rwPathStevci.primerjav) ($rwPathStevci.primerjav -gt 0)
                    Check ("R2: nobenega neujemanja ({0})" -f $rwPathStevci.neujemanj) ($rwPathStevci.neujemanj -eq 0)
                }
                if ($RwPathCas) {
                    $okCas = ($rwPathStevci['cas'] -eq 1) -and ($rwPathStevci['tickov'] -gt 0) -and ($rwPathStevci['sledenj'] -gt 0)
                    Check ("M1: merjenje sledenja poti je stelo ({0} pathFollow v {1} tickih)" -f $rwPathStevci['sledenj'], $rwPathStevci['tickov']) $okCas
                    if ($okCas) {
                        Write-Host ("  sledenje poti: {0} pathFollow, {1} kandidatov ({2} prostih), {3:N1} ms + {4:N1} ms kandidati v {5} tickih; histogram kandidatov {6}" -f `
                            $rwPathStevci['sledenj'], $rwPathStevci['kandidatov'], $rwPathStevci['prostih'], ($rwPathStevci['sledenjNs'] / 1e6), `
                            ($rwPathStevci['kandidatNs'] / 1e6), $rwPathStevci['tickov'], $rwPathStevci['histKand'])
                    }
                }
            }
        }
        $rwBlinkStevci = $null
        $rwBlinkPoskus = $null
        if ($RwBlink -ge 0) {
            $rwBlinkStevci = Send-RwBlink $srv 'rwblink'
            if ($null -ne $rwBlinkStevci) {
                Write-Host ("  RwBlink={0}: iskanj {1}, prejemnikov {2}, chunkov originala {3}, primerjav {4}, neujemanj {5}" -f $RwBlink, $rwBlinkStevci.iskanj, $rwBlinkStevci.prejemnikov, $rwBlinkStevci.chunkov, $rwBlinkStevci.primerjav, $rwBlinkStevci.neujemanj)
                if ($RwBlink -gt 0) {
                    Check ("B1: iskanje prejemnikov je teklo ({0} iskanj)" -f $rwBlinkStevci.iskanj) ($rwBlinkStevci.iskanj -gt 0)
                }
                if ($RwBlink -eq 2) {
                    Check ("B2: preverba je primerjala original in playerEntities ({0} primerjav)" -f $rwBlinkStevci.primerjav) ($rwBlinkStevci.primerjav -gt 0)
                    Check ("B3: nobenega neujemanja ({0})" -f $rwBlinkStevci.neujemanj) ($rwBlinkStevci.neujemanj -eq 0)
                }
            }
            # B4/B5: preverba enakosti v svetu na NPC-jih (na dediciranem strezniku ni igralcev,
            # zato je seznam prejemnikov vedno prazen in B2/B3 sama nista dokaz). Izven merilnega
            # okna, ker poizvedba originala stane.
            if ($RwBlink -gt 0) {
                $predP = ([regex]::Matches((Get-MarkerText $srv.Log), 'RWBLINK-POSKUS ')).Count
                Send-Command $srv 'rwblink poskus 64'
                $okP = Wait-ForCount $srv 'RWBLINK-POSKUS ' ($predP + 1) 120
                Check 'ukaz rwblink poskus odgovori' $okP
                if ($okP) {
                    $mp = [regex]::Matches((Get-MarkerText $srv.Log), 'RWBLINK-POSKUS primerjav=(\d+) skupaj=(\d+) neujemanj=(\d+)')
                    $gp = $mp[$mp.Count - 1].Groups
                    $rwBlinkPoskus = @{ primerjav = [long]$gp[1].Value; neujemanj = [long]$gp[3].Value }
                    Check ("B4: poskus je primerjal poizvedbi v svetu ({0} primerjav)" -f $gp[1].Value) ([long]$gp[1].Value -gt 0)
                    Check ("B5: poskus brez neujemanj ({0})" -f $gp[3].Value) ([long]$gp[3].Value -eq 0)
                }
            }
        }
        $rwCollideStevci = $null
        if ($RwCollide -ge 0) {
            $rwCollideStevci = Send-RwCollide $srv 'rwcollide'
            if ($null -ne $rwCollideStevci) {
                Write-Host ("  RwCollide={0}: klicev {1}, preskocenih {2}, brez opazovalca {3}, dogodkov brez opazovalca {4}" -f `
                    $RwCollide, $rwCollideStevci.klicev, $rwCollideStevci.preskocenih, $rwCollideStevci.brezOpazovalca, $rwCollideStevci.dogodkovBrezOpazovalca)
                if ($RwCollide -gt 0) {
                    Check ("K1: odlocitev onCollide je tekla ({0} klicev)" -f $rwCollideStevci.klicev) ($rwCollideStevci.klicev -gt 0)
                }
            }
            if ($RwCollide -eq 1) {
                # K2/K3: preskok mora videti poslusalca takoj (registracija med tekom).
                $r1 = Send-RwCollide $srv 'rwcollide poslusalec 1'
                Wait-SPreverbo 5
                $r2 = Send-RwCollide $srv 'rwcollide'
                $okK2 = ($null -ne $r1) -and ($null -ne $r2) -and ($r1.opazovalec -eq 1) -and ($r2.preskocenih -eq $r1.preskocenih) -and ($r2.poslusalecDogodkov -gt 0)
                Check ("K2: s poslusalcem ni preskoka ({0} -> {1}) in poslusalec dobi dogodke ({2})" -f $r1.preskocenih, $r2.preskocenih, $r2.poslusalecDogodkov) $okK2
                $r3 = Send-RwCollide $srv 'rwcollide poslusalec 0'
                Wait-SPreverbo 5
                $r4 = Send-RwCollide $srv 'rwcollide'
                $okK3 = ($null -ne $r3) -and ($null -ne $r4) -and ($r3.opazovalec -eq 0) -and ($r4.preskocenih -gt $r3.preskocenih)
                Check ("K3: po odjavi se preskok nadaljuje ({0} -> {1})" -f $r3.preskocenih, $r4.preskocenih) $okK3
            }
        }
        Send-Command $srv 'rwdiag off'
        $nOff++
        Check 'merjenje izklopljeno' (Wait-ForCount $srv 'RWDIAG izklopljen' $nOff 60)

        Send-Command $srv ('noppes clone spawn PERF_Kontrola 1 {0}' -f $kontrolaAt)
        $nKontrola++
        $okK = Wait-ForCount $srv 'PERF-KONTROLA idle=' $nKontrola 120
        Check 'PERF_Kontrola se je oglasil (konec)' $okK
        $k1 = if ($okK) { Read-Kontrola $srv.Log } else { $null }

        # --- posnetek ---
        $dumpPath = Read-DumpJsonPath $srv.Log
        $dump = $null
        if (($null -ne $dumpPath) -and (Test-Path $dumpPath)) {
            $dump = Get-Content $dumpPath -Raw | ConvertFrom-Json
        }
        Check ("posnetek je berljiv ({0})" -f $dumpPath) ($null -ne $dump)

        $vel = @{}
        $sat = $false
        if ($null -ne $dump) {
            $tick   = Get-Dist $dump 'server.tick.ns'
            $nosave = Get-Dist $dump 'server.tick.ns.nosave'
            $per    = Get-Dist $dump 'npc.per.tick'
            $gap    = Get-Dist $dump 'npc.tick.gap'
            $loaded = Get-Dist $dump 'world.npc.loaded'
            $killed = Get-Dist $dump 'world.npc.killed'
            $forced = Get-Dist $dump 'world.chunks.forced'
            $upd    = Get-Counter $dump 'npc.update.window'

            $vel['ticki'] = [long]$dump.ticks
            if ($dump.elapsedMillis -gt 0) { $vel['tps'] = [math]::Round($dump.ticks * 1000.0 / $dump.elapsedMillis, 2) }
            if ($null -ne $tick) {
                $vel['mspt.povp'] = Ms ($tick.sum / [math]::Max(1, $tick.count))
                $vel['mspt.p50']  = Ms $tick.p50
                $vel['mspt.p95']  = Ms $tick.p95
                $vel['mspt.p99']  = Ms $tick.p99
                $vel['mspt.max']  = Ms $tick.max
                # Nasicenje: server ne dohaja 20 tickov na sekundo. To je veljaven rezultat
                # (03-FAZE.md, tveganja M2), ne napaka scenarija.
                $sat = ($tick.p50 -gt 50000000)
            }
            if ($null -ne $nosave) { $vel['mspt.brezSave.p99'] = Ms $nosave.p99; $vel['mspt.brezSave.max'] = Ms $nosave.max }
            if (($null -ne $upd) -and ($upd.count -gt 0)) {
                $vel['npc.us'] = [math]::Round($upd.nanos / 1000.0 / $upd.count, 2)
            }
            # M2.6: pomnilnik in GC (JvmProbe). Brez teh vrstic jar ne vsebuje M2.6.
            $gc     = Get-Counter $dump 'jvm.gc'
            $gcOld  = Get-Counter $dump 'jvm.gc.old'
            $alloc  = Get-Counter $dump 'jvm.alloc.server'
            $allocOk= Get-Counter $dump 'jvm.alloc.podprto'
            $oldPo  = Get-Counter $dump 'jvm.heap.old.poGc'
            $hMax   = Get-Counter $dump 'jvm.heap.max'
            # Posnetek izpusti stevce z niclo (DiagSnapshot). jvm.heap.max in jvm.alloc.server sta
            # pri M2.6 jarju vedno vecja od nic; jvm.gc in jvm.gc.old manjkata, ce v meritvi ni
            # bilo zbirke (5. 10.: 30-sekundna preverba), kar pomeni 0, ne manjkajoce meritve.
            Check 'P8: posnetek ima meritve JVM (jvm.heap.max, jvm.alloc.server)' (($null -ne $hMax) -and ($null -ne $alloc))
            if (($null -ne $hMax) -and ($dump.elapsedMillis -gt 0)) {
                $sek = $dump.elapsedMillis / 1000.0
                $gcN = if ($null -ne $gc) { [long]$gc.count } else { 0L }
                $gcNs = if ($null -ne $gc) { [double]$gc.nanos } else { 0.0 }
                $vel['gc.zbirk'] = $gcN
                $vel['gc.ms'] = [math]::Round($gcNs / 1000000.0, 0)
                $vel['gc.msNaS'] = [math]::Round($gcNs / 1000000.0 / $sek, 2)
                $vel['gc.old.zbirk'] = $(if ($null -ne $gcOld) { [long]$gcOld.count } else { 0L })
                $vel['gc.old.ms'] = $(if ($null -ne $gcOld) { [math]::Round($gcOld.nanos / 1000000.0, 0) } else { 0 })
                if (($null -ne $alloc) -and ($null -ne $allocOk) -and ($allocOk.count -eq 1)) {
                    $vel['alok.MBnaS'] = [math]::Round($alloc.count / 1048576.0 / $sek, 1)
                    if ($dump.ticks -gt 0) { $vel['alok.KBnaTick'] = [math]::Round($alloc.count / 1024.0 / $dump.ticks, 1) }
                } else {
                    Write-Host '  OPOZORILO JVM ne podpira stetja alokacij po niti; alok.* manjka'
                }
                if (($null -ne $oldPo) -and ($oldPo.count -gt 0)) { $vel['heap.old.poGcMB'] = [math]::Round($oldPo.count / 1048576.0, 0) }
                if ($null -ne $hMax) { $vel['heap.maxMB'] = [math]::Round($hMax.count / 1048576.0, 0) }
            }
            foreach ($lv in $dump.slowTicks.levels) { $vel[('ticki.nad{0}ms' -f [int]($lv.ns / 1000000))] = [long]$lv.count }
            if ($null -ne $per) { $vel['npc.per.tick.p50'] = [long]$per.p50 }

            # P3: vsak NPC tika vsak tick.
            $p3 = ($null -ne $per) -and ($null -ne $gap) -and ($per.p50 -eq $N) -and ($gap.max -eq 1)
            Check ("P3: vsi NPC-ji tikajo vsak tick (npc.per.tick p50={0}, gap max={1})" -f $per.p50, $gap.max) $p3
            # P4: stevilo NPC-jev se med meritvijo ni spremenilo.
            $p4 = ($null -ne $loaded) -and ($loaded.min -eq $N) -and ($loaded.max -eq $N) -and `
                  ($null -ne $killed) -and ($killed.max -eq 0)
            Check ("P4: NPC-jev ves cas {0} (loaded min={1} max={2}, killed max={3})" -f $N, $loaded.min, $loaded.max, $killed.max) $p4
            Check ("P4b: pogoj meritve je veljal ves cas (world.chunks.forced min={0})" -f $forced.min) (($null -ne $forced) -and ($forced.min -gt 0))
        }

        # P5: obremenitev je bila res obremenitev celice.
        if ($null -ne $k1) {
            if ($v -eq 'idle') {
                Check ("P5: idle NPC-ji se niso borili (cilj={0}, ranjenih={1})" -f $k1.Cilj, $k1.Ranjenih) (($k1.Cilj -eq 0) -and ($k1.Ranjenih -eq 0))
            }
            if ($v -eq 'boj') {
                Check ("P5: boj je tekel (s ciljem {0} od {1}, ranjenih {2})" -f $k1.Cilj, $N, $k1.Ranjenih) (($k1.Cilj -ge ($N / 2)) -and ($k1.Ranjenih -gt 0))
                $vel['boj.sCiljem'] = $k1.Cilj
                $vel['boj.ranjenih'] = $k1.Ranjenih
            }
            if ($v -eq 'nedosegljiva') {
                # U1: tarca je vidna (NPC-ji jo imajo za cilj). U2: ni dosegljiva (nihce ni ranjen).
                Check ("U1: NPC-ji imajo tarco (s ciljem {0} od {1})" -f $k1.Cilj, $N) ($k1.Cilj -ge ($N / 2))
                Check ("U2: tarca ni dosegljiva (ranjenih {0}, pricakovano 0)" -f $k1.Ranjenih) ($k1.Ranjenih -eq 0)
                $vel['boj.sCiljem'] = $k1.Cilj
                $vel['boj.ranjenih'] = $k1.Ranjenih
            }
            if ($v -eq 'skripte') {
                $delta = $k1.SkriptTickov - $k0.SkriptTickov
                $wanted = if ($null -ne $dump) { [long](0.9 * $N * $dump.ticks / 10) } else { 1 }
                Check ("P5: skripte so tekle ves cas (skriptnih tickov {0}, pricakovano vsaj {1})" -f $delta, $wanted) ($delta -ge $wanted)
                $vel['skripte.tickov'] = $delta
            }
        }

        # P9: med ogrevanjem in merjenjem ni tekel tuj gradle build ali Minecraft.
        Wait-SPreverbo 0
        $tujiC = @($script:tujiVCelici | Select-Object -Unique)
        if ($tujiC.Count -gt 0 -and $DovoliTuje) {
            Write-Host ("  OPOZORILO P9: med celico je tekel tuj java proces (-DovoliTuje): {0}" -f ($tujiC -join '; '))
        } else {
            Check ("P9: med celico ni tekel tuj gradle build ali Minecraft" + $(if ($tujiC.Count) { ' - ' + ($tujiC -join '; ') } else { '' })) ($tujiC.Count -eq 0)
        }

        # P6: nobene napake iz moda ali skript v tej celici; P7 pomnilnik.
        $log = Get-LogText $srv.Log
        $oom = $log.Contains('OutOfMemoryError')
        Check 'P7: brez OutOfMemoryError' (-not $oom)
        if ($sat) { Write-Host ("  NASICENO   MSPT p50 = {0} ms > 50 ms: server ne dohaja 20 TPS pri {1} NPC-jih ({2})" -f $vel['mspt.p50'], $N, $v) }

        $odtis = @{ razprseno = $(if ($Razprseno) { 1 } else { 0 }); varianta = $v; npc = $N; sekund = $Seconds; ogrevanje = $WarmupSeconds; obroc = $ChunkRadius
                    celica = $idx; celic = $cells.Count; mrezaX = $gridX; mrezaZ = $gridZ; mrezaSirina = $gridW }
        if ($RwTarget -ge 0) {
            $odtis['rwtarget'] = $RwTarget
            $vel['rwtarget.predzavrnjenih'] = $predzavrnjenih
            $vel['rwtarget.zozenih'] = $script:rwZozenih
        }
        if ($Jfr) { $odtis['jfr'] = 1 }
        if ($RwPath -ge 0) {
            $odtis['rwpath'] = $RwPath
            if ($null -ne $rwPathStevci) {
                foreach ($k in @('klicev', 'ocen', 'izracunov', 'primerjav', 'neujemanj')) { $vel['rwpath.' + $k] = $rwPathStevci[$k] }
            }
        }
        if ($RwCollide -ge 0) {
            $odtis['rwcollide'] = $RwCollide
            if ($null -ne $rwCollideStevci) {
                foreach ($k in @('klicev', 'preskocenih', 'brezOpazovalca', 'dogodkovBrezOpazovalca')) { $vel['rwcollide.' + $k] = $rwCollideStevci[$k] }
            }
        }
        if ($RwPathIskanje) {
            $odtis['rwpathIskanje'] = 1
            if (($null -ne $rwPathStevci) -and ($rwPathStevci['tickov'] -gt 0) -and ($null -ne $rwPathStevci['iskKlicev'])) {
                $ti = [double]$rwPathStevci['tickov']
                $kl = [long]$rwPathStevci['iskKlicev']
                $vel['rwpath.isk.klicev'] = $kl
                $vel['rwpath.isk.naTick'] = [math]::Round($kl / $ti, 2)
                $vel['rwpath.isk.msNaTick'] = [math]::Round($rwPathStevci['iskNs'] / $ti / 1e6, 3)
                $vel['rwpath.isk.maxUs'] = [math]::Round($rwPathStevci['iskMaxNs'] / 1000.0, 1)
                $vel['rwpath.isk.pomnilnik'] = $rwPathStevci['iskPomnilnik']
                $vel['rwpath.isk.brezPoti'] = $rwPathStevci['iskBrezPoti']
                $vel['rwpath.isk.celih'] = $rwPathStevci['iskCelih']
                $vel['rwpath.isk.delnih'] = $rwPathStevci['iskDelnih']
                $vel['rwpath.isk.delnaRazdalja'] = $rwPathStevci['iskDelnaRazdalja']
                $isci = $kl - [long]$rwPathStevci['iskPomnilnik']
                $vel['rwpath.isk.iskanj'] = $isci
                $vel['rwpath.isk.iskanjNaTick'] = [math]::Round($isci / $ti, 2)
                if ($isci -gt 0) { $vel['rwpath.isk.usNaIskanje'] = [math]::Round($rwPathStevci['iskNs'] / [double]$isci / 1000.0, 1) }
                if (($null -ne $vel['mspt.povp']) -and ($vel['mspt.povp'] -gt 0)) {
                    $vel['rwpath.isk.delez'] = [math]::Round(100.0 * $vel['rwpath.isk.msNaTick'] / $vel['mspt.povp'], 2)
                }
                Check ("U3: merjenje iskanja poti je stelo ({0} klicev v {1} tickih)" -f $kl, $rwPathStevci['tickov']) ($kl -gt 0)
                Write-Host ("  M5.9: iskanj {0} ({1}/tick, {2} us na iskanje), iz pomnilnika {3}, brez poti {4}, celih {5}, delnih {6} (povp. razdalja {7}); {8} ms/tick = {9} % ticka" -f $isci, $vel['rwpath.isk.iskanjNaTick'], $vel['rwpath.isk.usNaIskanje'], $vel['rwpath.isk.pomnilnik'], $vel['rwpath.isk.brezPoti'], $vel['rwpath.isk.celih'], $vel['rwpath.isk.delnih'], $vel['rwpath.isk.delnaRazdalja'], $vel['rwpath.isk.msNaTick'], $vel['rwpath.isk.delez'])
            }
        }
        if ($RwBlink -ge 0) {
            $odtis['rwblink'] = $RwBlink
            if ($null -ne $rwBlinkStevci) {
                foreach ($k in @('iskanj', 'igralcev', 'prejemnikov', 'chunkov', 'primerjav', 'neujemanj')) { $vel['rwblink.' + $k] = $rwBlinkStevci[$k] }
            }
            if ($null -ne $rwBlinkPoskus) {
                $vel['rwblink.poskus.primerjav'] = $rwBlinkPoskus.primerjav
                $vel['rwblink.poskus.neujemanj'] = $rwBlinkPoskus.neujemanj
            }
        }
        if ($RwBlinkCas) {
            $odtis['rwblinkCas'] = 1
            if (($null -ne $rwBlinkStevci) -and ($rwBlinkStevci['tickov'] -gt 0) -and ($rwBlinkStevci['iskanj'] -gt 0)) {
                $tb = [double]$rwBlinkStevci['tickov']
                $nsb = 0.0
                foreach ($m in $rwBlinkStevci['poNacinu'].Keys) { $nsb += $rwBlinkStevci['poNacinu'][$m].ns }
                $vel['rwblink.tickov'] = $rwBlinkStevci['tickov']
                $vel['rwblink.iskanjNaTick'] = [math]::Round($rwBlinkStevci['iskanj'] / $tb, 3)
                $vel['rwblink.chunkovNaIskanje'] = [math]::Round($rwBlinkStevci['chunkov'] / [double]$rwBlinkStevci['iskanj'], 1)
                $vel['rwblink.usNaIskanje'] = [math]::Round($nsb / [double]$rwBlinkStevci['iskanj'] / 1000.0, 2)
                $vel['rwblink.msNaTick'] = [math]::Round($nsb / $tb / 1e6, 4)
                if (($null -ne $vel['mspt.povp']) -and ($vel['mspt.povp'] -gt 0)) {
                    $vel['rwblink.delez'] = [math]::Round(100.0 * $vel['rwblink.msNaTick'] / $vel['mspt.povp'], 2)
                }
                Write-Host ("  M5.13: iskanje prejemnikov {0} us na iskanje, {1} iskanj/tick = {2} ms/tick ({3} % povprecnega ticka)" -f $vel['rwblink.usNaIskanje'], $vel['rwblink.iskanjNaTick'], $vel['rwblink.msNaTick'], $vel['rwblink.delez'])
            }
        }
        if ($RwBlinkAB.Count -gt 0) {
            $odtis['rwblinkAB'] = ($RwBlinkAB -join ',')
            $odtis['rwblinkOkno'] = $RwBlinkOkno
            $pob = if ($null -ne $rwBlinkStevci) { $rwBlinkStevci['poNacinu'] } else { $null }
            $abOkB = $null -ne $pob
            if ($abOkB) { foreach ($m in $RwBlinkAB) { if (-not $pob.ContainsKey($m) -or ($pob[$m].tickov -le 0) -or ($pob[$m].iskanj -le 0)) { $abOkB = $false } } }
            Check ("AB3: vsak nacin ({0}) je tekel in imel iskanja" -f ($RwBlinkAB -join ',')) $abOkB
            if ($abOkB) {
                $intenz = @()
                foreach ($m in $RwBlinkAB) {
                    $tm = [double]$pob[$m].tickov
                    $pre = 'rwblink.ab.{0}.' -f $m
                    $vel[$pre + 'tickov'] = $pob[$m].tickov
                    $vel[$pre + 'iskanj'] = $pob[$m].iskanj
                    $vel[$pre + 'iskanjNaTick'] = [math]::Round($pob[$m].iskanj / $tm, 3)
                    $vel[$pre + 'usNaIskanje'] = [math]::Round($pob[$m].ns / [double]$pob[$m].iskanj / 1000.0, 2)
                    $vel[$pre + 'msNaTick'] = [math]::Round($pob[$m].ns / $tm / 1e6, 4)
                    $intenz += $pob[$m].iskanj / $tm
                    Write-Host ("  AB nacin {0}: {1} tickov, {2} iskanj/tick, {3} us na iskanje, {4} ms/tick" -f $m, $pob[$m].tickov, $vel[$pre + 'iskanjNaTick'], $vel[$pre + 'usNaIskanje'], $vel[$pre + 'msNaTick'])
                }
                $aB = $RwBlinkAB[0]; $bB = $RwBlinkAB[1]
                $vel['rwblink.ab.usRazmerje'] = [math]::Round($vel[('rwblink.ab.{0}.usNaIskanje' -f $bB)] / $vel[('rwblink.ab.{0}.usNaIskanje' -f $aB)], 3)
                $vel['rwblink.ab.msNaTickRazlika'] = [math]::Round($vel[('rwblink.ab.{0}.msNaTick' -f $bB)] - $vel[('rwblink.ab.{0}.msNaTick' -f $aB)], 4)
                $iMin = ($intenz | Measure-Object -Minimum).Minimum; $iMax = ($intenz | Measure-Object -Maximum).Maximum
                $vel['rwblink.ab.iskanjNaTick.razpon'] = [math]::Round(100.0 * ($iMax - $iMin) / $iMin, 1)
                Check ("AB4: iskanj na tick med nacini v 10 % (razpon {0} %)" -f $vel['rwblink.ab.iskanjNaTick.razpon']) ($vel['rwblink.ab.iskanjNaTick.razpon'] -le 10.0)
                Write-Host ("  AB: us na iskanje {0}/{1} = {2}, {3} ms/tick razlike" -f $bB, $aB, $vel['rwblink.ab.usRazmerje'], $vel['rwblink.ab.msNaTickRazlika'])
            }
        }
        if ($RwPathAB.Count -gt 0) {
            $odtis['rwpathAB'] = ($RwPathAB -join ',')
            $odtis['rwpathOkno'] = $RwPathOkno
            $po = if ($null -ne $rwPathStevci) { $rwPathStevci['poNacinu'] } else { $null }
            $abOk = $null -ne $po
            if ($abOk) { foreach ($m in $RwPathAB) { if (-not $po.ContainsKey($m) -or ($po[$m].tickov -le 0) -or ($po[$m].kandidatov -le 0)) { $abOk = $false } } }
            Check ("AB1: vsak nacin ({0}) je tekel in imel kandidate" -f ($RwPathAB -join ',')) $abOk
            if ($abOk) {
                $knt = @()
                foreach ($m in $RwPathAB) {
                    $t = [double]$po[$m].tickov
                    $p = 'rwpath.ab.{0}.' -f $m
                    $vel[$p + 'tickov'] = $po[$m].tickov
                    $vel[$p + 'kandidatov'] = $po[$m].kandidatov
                    $vel[$p + 'kandidatovNaTick'] = [math]::Round($po[$m].kandidatov / $t, 2)
                    $vel[$p + 'sledenjNaTick'] = [math]::Round($po[$m].sledenj / $t, 2)
                    $vel[$p + 'kandidat.usNaKlic'] = [math]::Round($po[$m].kandidatNs / [double]$po[$m].kandidatov / 1000.0, 3)
                    $vel[$p + 'kandidat.msNaTick'] = [math]::Round($po[$m].kandidatNs / $t / 1e6, 3)
                    $vel[$p + 'sledenje.msNaTick'] = [math]::Round($po[$m].sledenjNs / $t / 1e6, 3)
                    $knt += $po[$m].kandidatov / $t
                    Write-Host ("  AB nacin {0}: {1} tickov, {2} kandidatov/tick, {3} us/kandidat, pathFollow {4} ms/tick" -f `
                        $m, $po[$m].tickov, $vel[$p + 'kandidatovNaTick'], $vel[$p + 'kandidat.usNaKlic'], $vel[$p + 'sledenje.msNaTick'])
                }
                $a = $RwPathAB[0]; $b = $RwPathAB[1]
                $vel['rwpath.ab.kandidat.usRazmerje'] = [math]::Round($vel[('rwpath.ab.{0}.kandidat.usNaKlic' -f $b)] / $vel[('rwpath.ab.{0}.kandidat.usNaKlic' -f $a)], 3)
                $vel['rwpath.ab.sledenje.msRazlika'] = [math]::Round($vel[('rwpath.ab.{0}.sledenje.msNaTick' -f $b)] - $vel[('rwpath.ab.{0}.sledenje.msNaTick' -f $a)], 3)
                $kMin = ($knt | Measure-Object -Minimum).Minimum; $kMax = ($knt | Measure-Object -Maximum).Maximum
                $vel['rwpath.ab.kandidatovNaTick.razpon'] = [math]::Round(100.0 * ($kMax - $kMin) / $kMin, 1)
                Check ("AB2: kandidatov na tick med nacini v 10 % (razpon {0} %)" -f $vel['rwpath.ab.kandidatovNaTick.razpon']) ($vel['rwpath.ab.kandidatovNaTick.razpon'] -le 10.0)
                Write-Host ("  AB: us na kandidata {0}/{1} = {2}, pathFollow {3} ms/tick razlike" -f $b, $a, $vel['rwpath.ab.kandidat.usRazmerje'], $vel['rwpath.ab.sledenje.msRazlika'])
            }
        }
        if ($RwPathCas) {
            $odtis['rwpathCas'] = 1
            # M5.10: delez ticka iz lastnega stevca tickov (od 'rwpath reset' do branja), da okno
            # stevcev ne rabi sovpadati z oknom rwdiag; povprecen tick iz rwdiag (mspt.povp).
            if (($null -ne $rwPathStevci) -and ($rwPathStevci['tickov'] -gt 0) -and ($rwPathStevci['sledenj'] -gt 0)) {
                $t = [double]$rwPathStevci['tickov']
                $vel['rwpath.tickov'] = $rwPathStevci['tickov']
                $vel['rwpath.sledenj'] = $rwPathStevci['sledenj']
                $vel['rwpath.kandidatov'] = $rwPathStevci['kandidatov']
                $vel['rwpath.prostih'] = $rwPathStevci['prostih']
                $vel['rwpath.sledenjNaTick'] = [math]::Round($rwPathStevci['sledenj'] / $t, 1)
                $vel['rwpath.kandidatovNaTick'] = [math]::Round($rwPathStevci['kandidatov'] / $t, 1)
                $vel['rwpath.kandidatovNaSledenje'] = [math]::Round($rwPathStevci['kandidatov'] / [double]$rwPathStevci['sledenj'], 3)
                $vel['rwpath.sledenje.msNaTick'] = [math]::Round($rwPathStevci['sledenjNs'] / $t / 1e6, 3)
                $vel['rwpath.kandidat.msNaTick'] = [math]::Round($rwPathStevci['kandidatNs'] / $t / 1e6, 3)
                $vel['rwpath.sledenje.usNaKlic'] = [math]::Round($rwPathStevci['sledenjNs'] / [double]$rwPathStevci['sledenj'] / 1000.0, 2)
                if ($rwPathStevci['kandidatov'] -gt 0) {
                    $vel['rwpath.kandidat.usNaKlic'] = [math]::Round($rwPathStevci['kandidatNs'] / [double]$rwPathStevci['kandidatov'] / 1000.0, 2)
                }
                $hi = 0
                foreach ($x in ($rwPathStevci['histKand'] -split '/')) { $vel[('rwpath.hist.{0}' -f $hi)] = [long]$x; $hi++ }
                if (($null -ne $vel['mspt.povp']) -and ($vel['mspt.povp'] -gt 0)) {
                    $vel['rwpath.sledenje.delez'] = [math]::Round(100.0 * $vel['rwpath.sledenje.msNaTick'] / $vel['mspt.povp'], 1)
                    $vel['rwpath.kandidat.delez'] = [math]::Round(100.0 * $vel['rwpath.kandidat.msNaTick'] / $vel['mspt.povp'], 1)
                    Write-Host ("  M5.10: pathFollow {0} ms/tick = {1} % povprecnega ticka ({2} ms), od tega kandidati {3} %" -f `
                        $vel['rwpath.sledenje.msNaTick'], $vel['rwpath.sledenje.delez'], $vel['mspt.povp'], $vel['rwpath.kandidat.delez'])
                }
            }
        }
        $cellFails = @()
        if ($failures.Count -gt $cellFailStart) { $cellFails = @($failures[$cellFailStart..($failures.Count - 1)]) }
        $vel['nasiceno'] = $(if ($sat) { 1 } else { 0 })
        $jp = if ($JsonPath -ne '') { $JsonPath }
              elseif ($SerijaDir -ne '') { Join-Path $SerijaDir ("{0}-{1}.json" -f $v, $N) }
              else { Join-Path $audit ("m24-perf-{0}-{1}-{2}.json" -f $v, $N, $stamp) }
        $null = Write-MeritevJson -Path $jp -Paket 'M2.4' -Scenarij ("perf-{0}-{1}" -f $v, $N) `
                    -Odtis $odtis -Velicine $vel -Uspeh ($cellFails.Count -eq 0) -Padle $cellFails
        Write-Host ("  zapis celice: {0}" -f $jp)
        $rows += [pscustomobject]@{ Varianta = $v; N = $N; Vel = $vel; Uspeh = ($cellFails.Count -eq 0); Nasiceno = $sat }

        if ($oom -or $srv.Proc.HasExited) {
            Write-Host '  ! server je brez pomnilnika ali se je ustavil; preostale celice se ne pozenejo'
            break
        }
    }

    Step 5 'Konec in napake moda'
    Send-Command $srv 'rwdiag chunks off'
    $nChunks++
    $null = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60
    $log = Get-LogText $srv.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'P6: brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'P6: brez "script errored"' (-not $log.Contains('script errored'))
    Check 'server se je cisto ustavil' (Stop-DevServer $srv)

    # M5-S P1: povzetki JFR sele zdaj, da 'jfr print' ne tece med merjenjem naslednje celice.
    foreach ($jp in $jfrPosnetki) {
        $md = [System.IO.Path]::ChangeExtension($jp, '.md')
        $ErrorActionPreference = 'Continue'
        & node (Join-Path $root 'dev\tools\jfr-povzetek.js') $jp --md $md > $null 2>&1
        $ErrorActionPreference = 'Stop'
        Check ("J3: povzetek JFR {0}" -f $md) (($LASTEXITCODE -eq 0) -and (Test-Path $md))
    }

    Step 6 'Porocilo'
    $report = Join-Path $audit ("m24-perf-{0}.md" -f $stamp)
    $L = @()
    $L += ("# M2.4 - merilne obremenitve, zagon {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $L += ''
    $L += 'Scenarij: `docs/scenariji/M2.4-obremenitve.md`. Ogrevanje {0} s, merjenje {1} s, obroc chunkov {2}, spawn {3}.' -f $WarmupSeconds, $Seconds, $ChunkRadius, $(if ($Razprseno) { 'razprsen' } else { 'naenkrat' })
    $L += ''
    $L += ("Merila: " + $(if ($failures.Count -eq 0) { 'P1-P9 zelena' } else { 'PADLA: ' + ($failures -join '; ') }))
    $L += ''
    $L += '| varianta | NPC | TPS | MSPT povp | p50 | p95 | p99 | max | p99 brez save | us/NPC update | ticki >50 ms | GC ms/s | GC stare | alok. MB/s | stanje |'
    $L += '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|'
    foreach ($r in $rows) {
        $x = $r.Vel
        $st = if (-not $r.Uspeh) { 'merila padla' } elseif ($r.Nasiceno) { 'nasiceno' } else { 'ok' }
        $L += ('| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} | {11} | {12} | {13} | {14} |' -f `
            $r.Varianta, $r.N, $x['tps'], $x['mspt.povp'], $x['mspt.p50'], $x['mspt.p95'], $x['mspt.p99'],
            $x['mspt.max'], $x['mspt.brezSave.p99'], $x['npc.us'], $x['ticki.nad50ms'],
            $x['gc.msNaS'], $x['gc.old.zbirk'], $x['alok.MBnaS'], $st)
    }
    $L += ''
    $L += 'MSPT v ms. Posnetki: `dev/run/logs/rwdiag/*m24-*`; zapisi celic: `audit/m24-perf-<varianta>-<N>-' + $stamp + '.json`.'
    [System.IO.File]::WriteAllText($report, (($L -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ''
    $L | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
    Write-Host ("Porocilo: {0}" -f $report)

    if ($failures.Count -eq 0) {
        Write-Host 'M2.4 USPESNO: vse celice so merile, kar pravijo, da merijo.'
        exit 0
    }
    Write-Host 'M2.4 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $srv.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    if ($null -ne $srv) { $null = Stop-DevServer $srv }
    Write-Host 'M2.4 NEUSPESNO. Izpis je v audit\m24-perf.log'
    exit 1
}
