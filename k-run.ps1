<#
    k-run.ps1 - klientski merilni harness M2.4r (M5-K.0, D-033)

    Ena celica na zagon: razvojni klient (runClient) z igralcem v nacinu spectator, fiksna kamera,
    brez VSynca in view bobbinga, fiksna razdalja upodobitve in FOV, brez GUI-ja. Klient po
    navodilu sam ustvari svez FLAT svet (seme kot dev/testworld/server.properties), postavi
    prizorisce z istimi ukazi kot perf-run.ps1, se ogreje, 60 s belezi case slicic in se ugasne.
    Scenarij: docs/scenariji/M5-K.0-klient.md

    Uporaba:
        .\k-run.ps1 -Variant idle -N 200
        .\k-run.ps1 -Variant idle -N 50 -Warmup 10 -Measure 20      # hiter preizkus harnessa
        .\ponovitve-run.ps1 -Scenarij klient -Dodatno @('-Variant','idle','-N','200')

    Merila (K1-K8, z -PreveriPokrivalo K9, s -Pokrivalo K10, z -PreveriNapise K11, z -Napisi K12, z -Lod K15, s -Far K16, s -Posebni K17, s -Posebni in -RenderBox K18) so na koncu; izhodna koda 0 samo, ce so vsa zelena.

    -Jfr (M5-K.1): v merilnem oknu snema JFR klientskega JVM-ja (jcmd JFR.start, nastavitve 'profile')
    v audit\jfr\k-<varianta>-<N>-<stamp>.jfr in napise povzetek niti 'Client thread' z nativnimi
    vzorci (dev\tools\jfr-povzetek.js --native). Profiler stane CPU, zato zagon z -Jfr ni baseline.
    JFR v JDK 8 ujame le malo vzorcev render niti (vecino casa je v JNI), zato je za profil
    namenjen -Vzorci <ms>: harness med merjenjem vzorci sklad samo render niti (rework/diag/StackSampler)
    v <izhod>-skladi.txt, povzetek po fazah slicice napise dev\tools\skladi-povzetek.js. Tudi to ni baseline.
#>
param(
    [ValidateSet('idle', 'boj', 'skripte', 'nedosegljiva')][string]$Variant = 'idle',
    [int]$N = 200,
    [int]$Warmup = 30,
    [int]$Measure = 60,
    [int]$RenderDistance = 8,
    [int]$Fov = 70,
    [string]$JsonPath = '',
    [switch]$DovoliTuje,
    [switch]$Jfr,
    [int]$Vzorci = 0,
    # M5-K.0 je meril s skritim GUI-jem; v 1.12 to skrije tudi imena NPC-jev, zato je od M5-K.1 GUI viden.
    [switch]$SkrijGui,
    # M5-K K14b: RwHeadwearSkip za ta zagon (harness ga nastavi ob vstopu); -1 = iz CustomNpcs.cfg.
    [ValidateRange(-1, 1)][int]$Pokrivalo = -1,
    # M5-K K14b: po ogrevanju primerjaj piksle in globino z RwHeadwearSkip 0 in 1 (merilo K9).
    [switch]$PreveriPokrivalo,
    # M5-K K4: RwLabelCache za ta zagon; -1 = iz CustomNpcs.cfg.
    [ValidateRange(-1, 1)][int]$Napisi = -1,
    # M5-K K4: po ogrevanju primerjaj piksle in globino z RwLabelCache 0 in 1 (merilo K11).
    [switch]$PreveriNapise,
    # M5-K K5: RwLodStart za ta zagon (bloki, 0 = original); -1 = iz CustomNpcs.cfg (merilo K15).
    [ValidateRange(-1, 512)][int]$Lod = -1,
    # M5-V.6: RwFarView za ta zagon (0 = original, 2 = L2 + L3); -1 = iz CustomNpcs.cfg (merilo K16).
    [ValidateRange(-1, 2)][int]$Far = -1,
    # M5-V.6: RwImpostorStart za ta zagon (bloki, prag L2); -1 = iz CustomNpcs.cfg.
    [ValidateRange(-1, 512)][int]$ImpostorStart = -1,
    # M5-V.6: 1 = prizorisce "dalec": kamera 40 blokov nad tlemi, najblizja vrsta 36 blokov vodoravno, da je vecina
    # NPC-jev onkraj praga L2 (56) in vsi znotraj sledilnika (Chebyshev <= 60) in dometa upodobitve (RD 10: 80).
    [ValidateRange(0, 1)][int]$Dalec = 0,
    # M5-V.7: 1 = poleg mreze se 9 posebnih NPC-jev (RWV7_*: velik, pol-viden, neviden, brez hitboxa, jahac in
    # nosilec, ubit, morf, URL skin z lokalnega streznika z zamikom) v vrsti 4 bloke za mrezo; merilo K17.
    # Potrebuje -Dalec 1 in -Far 2.
    [ValidateRange(0, 1)][int]$Posebni = 0,
    # M5-K K12: RwRenderBox za ta zagon (0 = hitbox, 1 = okvir modela); -1 = iz CustomNpcs.cfg. S -Posebni 1 doda
    # NPC RWV7_Mec (diamantni mec v roki) in merilo K18 (okvir objame hitbox in pecen obris modela, domet brez hitboxa).
    [ValidateRange(-1, 1)][int]$RenderBox = -1
)

$ErrorActionPreference = 'Stop'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed  = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null
. (Join-Path $root 'meritve-lib.ps1')

$failures = @()
function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host "  OK       $What" } else { Write-Host "  PADLO    $What"; $script:failures += $What }
}

# Prizorisce mora biti isto kot v perf-run.ps1 (M2.4), sicer klient in streznik ne merita iste
# celice. Zato se generator ukazov in konstante mreze NE prepisejo, ampak preberejo iz perf-run.ps1
# (AST: samo definicije funkcij in prirejanja konstant, nic se ne izvede).
$perfAst = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $root 'perf-run.ps1'), [ref]$null, [ref]$null)
foreach ($ime in @('gridX', 'gridZ', 'gridW', 'bojGap', 'kontrolaAt')) {
    $a = $perfAst.Find({ param($x) $x -is [System.Management.Automation.Language.AssignmentStatementAst] -and "$($x.Left)" -eq ('$' + $ime) }, $false)
    if (-not $a) { throw "perf-run.ps1 nima konstante `$$ime" }
    . ([scriptblock]::Create($a.Extent.Text))
}
foreach ($ime in @('Get-GroupCommands', 'Get-FenceCommands', 'Get-SpawnCommands')) {
    $f = $perfAst.Find({ param($x) $x -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $x.Name -eq $ime }, $true)
    if (-not $f) { throw "perf-run.ps1 nima funkcije $ime" }
    . ([scriptblock]::Create($f.Extent.Text))
}

Step 1 'Parametri in priprava'
if ($N -lt 50 -or ($N % 50) -ne 0) { throw "N mora biti veckratnik 50 (mreza 25, boj ima dve polovici): $N" }
$zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'k-run'
Check 'zaklep scenarija (.scenarij.lock)' $true
$tuji0 = @(Get-TujiJava)
if ($tuji0.Count -gt 0 -and -not $DovoliTuje) {
    Check ('K0: pred zagonom ne tece tuj gradle build ali Minecraft - ' + ($tuji0 -join '; ')) $false
    throw 'Tuj java proces bi meril skupaj s klientom. Ustavi ga ali pozeni z -DovoliTuje (ni baseline).'
}
Check ('K0: pred zagonom ne tece tuj gradle build ali Minecraft' + $(if ($tuji0.Count) { ' (-DovoliTuje: ' + ($tuji0 -join '; ') + ')' } else { '' })) $true

$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwk-$Variant-$N"
$saves = Join-Path $run 'saves'
$save  = Join-Path $saves $world
# Svez svet vsak zagon (D-016: ponovitev v svezem svetu). Mapa je nasa, ustvari jo samo ta skripta.
if (Test-Path $save) { Remove-Item -Recurse -Force $save }
New-Item -ItemType Directory -Force -Path (Join-Path $save 'customnpcs') | Out-Null
Copy-Item -Path (Join-Path $seed 'customnpcs\*') -Destination (Join-Path $save 'customnpcs') -Recurse -Force
$nKlonov = @(Get-ChildItem (Join-Path $save 'customnpcs\clones\1') -Filter 'PERF_*.json').Count
Check ("fixture PERF_* namescene v svet ({0})" -f $nKlonov) ($nKlonov -ge 4)

# M5-V.7: posebni NPC-ji so kloni PERF_Idle z eno spremembo, zapisani samo v svez svet tega zagona (fixture
# dev\testworld ostane nespremenjen). Ime se zacne z RWV7_, harness steje izrise L2 po imenu (specialPrefix).
# Zamenjava: [iskani niz, nadomestek]; iskani niz z (?m) je regex, sicer dobeseden.
$skinPort = 18731
$skinUrl = "http://127.0.0.1:$skinPort/rwv7.png"
$posebniNpc = [ordered]@{
    'RWV7_Velik'    = @{ x = 49; zamenjaj = @(,@('(?m)^(\s*)"Size": 5,', '$1"Size": 15,')) }
    'RWV7_Polviden' = @{ x = 52; zamenjaj = @(,@('"NpcVisible": 0', '"NpcVisible": 2')) }
    'RWV7_Nevidni'  = @{ x = 55; zamenjaj = @(,@('"NpcVisible": 0', '"NpcVisible": 1')) }
    'RWV7_Kip'      = @{ x = 58; zamenjaj = @(,@('"IsStatue": 0b', '"IsStatue": 1b')) }
    'RWV7_Nosilec'  = @{ x = 61; zamenjaj = @() }
    'RWV7_Jahac'    = @{ x = 61; zamenjaj = @() }
    'RWV7_Mrtev'    = @{ x = 64; zamenjaj = @(,@('"RespawnTime": 20,', '"RespawnTime": 100000,')) }
    'RWV7_Morf'     = @{ x = 67; zamenjaj = @(,@('"NpcModelData": {', '"NpcModelData": {"EntityClass": "net.minecraft.entity.passive.EntityPig",')) }
    'RWV7_Url'      = @{ x = 70; zamenjaj = @(@('"UsingSkinUrl": 0b', '"UsingSkinUrl": 2b'), @('"SkinUrl": ""', ('"SkinUrl": "' + $skinUrl + '"'))) }
}
if ($RenderBox -ge 0) {
    # M5-K K12: predmet v roki sega cez roke (okvir HELD_HALF); samo s -RenderBox, da M5-V.7 (v7-posebni) ostane isti.
    $posebniNpc['RWV7_Mec'] = @{ x = 73; zamenjaj = @(,@('(?m)"Weapons": \[\s*\]', '"Weapons": [{"Slot": 0b, "id": "minecraft:diamond_sword", "Count": 1b, "Damage": 0s}]')) }
}
$zPosebni = $gridZ - 4
$skinProc = $null
$skinLog = ''
if ($Posebni -eq 1) {
    if ($Dalec -ne 1 -or $Far -ne 2) { throw '-Posebni 1 potrebuje -Dalec 1 in -Far 2' }
    $vzor = [System.IO.File]::ReadAllText((Join-Path $save 'customnpcs\clones\1\PERF_Idle.json'))
    foreach ($ime in $posebniNpc.Keys) {
        $t = $vzor.Replace('"Name": "PERF_Idle"', ('"Name": "' + $ime + '"'))
        foreach ($z in $posebniNpc[$ime].zamenjaj) {
            $novo = if ($z[0].StartsWith('(?m)')) { [regex]::Replace($t, $z[0], $z[1]) } else { $t.Replace($z[0], $z[1]) }
            if ($novo -eq $t) { throw "klon $ime : vzorec '$($z[0])' ni v PERF_Idle.json" }
            $t = $novo
        }
        [System.IO.File]::WriteAllText((Join-Path $save "customnpcs\clones\1\$ime.json"), $t, (New-Object System.Text.UTF8Encoding($false)))
    }
    Check ("M5-V.7: {0} posebnih klonov RWV7_* zapisanih v svet" -f $posebniNpc.Count) $true
    # Streznik skina odgovori sele po 4 s: peka impostorja mora naleteti na prenos v teku (merilo K17i).
    $skinLog = Join-Path $audit "k-$Variant-$N-$stamp-skin.txt"
    $node = Get-Command node -ErrorAction SilentlyContinue
    if (-not $node) { throw 'node ni na voljo (streznik skina za -Posebni)' }
    $skinProc = Start-Process -FilePath $node.Source -ArgumentList @(('"{0}"' -f (Join-Path $root 'dev\tools\skin-streznik.js')), $skinPort, ('"{0}"' -f $skinLog), 4000, 'FF00FF') -WindowStyle Hidden -PassThru
    $skinCas = (Get-Date).AddSeconds(10)
    while (((-not (Test-Path $skinLog)) -or ("$(Get-Content $skinLog -Raw -ErrorAction SilentlyContinue)" -notmatch 'POSLUSA')) -and (Get-Date) -lt $skinCas) { Start-Sleep -Milliseconds 200 }
    Check ("M5-V.7: lokalni streznik skina poslusa na 127.0.0.1:{0} (zamik 4000 ms)" -f $skinPort) ((Test-Path $skinLog) -and ("$(Get-Content $skinLog -Raw)" -match 'POSLUSA'))
}

# Kamera: juzno od mreze, nad njo, pogled proti severu (yaw 180) na sredino. NPC-ji morajo biti
# znotraj dometa sledilnika (64 blokov, tracker(64, 3, true)), sicer jih streznik klientu ne poslje.
$globina = if ($Variant -eq 'boj' -or $Variant -eq 'nedosegljiva') { [int](($N / 2) / $gridW) * 2 + $bojGap } else { [int]($N / $gridW) }
$zSredina = $gridZ + $globina / 2.0
$camZ = $gridZ + $globina + 6 + [math]::Round($globina * 0.5)
$camY = 4 + 8 + [math]::Round($globina * 0.6)
if ($Dalec -eq 1) {
    $camZ = $gridZ + $globina + 36
    $camY = 4 + 40
}
$camX = $gridX + ($gridW - 1) / 2.0 + 0.5
$pitch = [math]::Round([math]::Atan2($camY - 4, $camZ - $zSredina) * 180 / [math]::PI, 1)
$daljnji = [math]::Sqrt([math]::Pow($camZ - $gridZ, 2) + [math]::Pow($gridW / 2.0, 2) + [math]::Pow($camY - 4, 2))
Write-Host ("  kamera ({0}, {1}, {2}) yaw 180 pitch {3}; najdaljsi NPC {4:N1} blokov" -f $camX, $camY, $camZ, $pitch, $daljnji)
if ($Dalec -eq 1) {
    # Sledilnik je kvadrat (Chebyshev, samo x in z); vanilla domet upodobitve je 64 * RD/8 (1..2,5) za NPC z robom 1,0.
    $cheb = [math]::Max($camZ - $gridZ, $gridW / 2.0)
    $domet = 64 * [math]::Min(2.5, [math]::Max(1.0, $RenderDistance / 8.0))
    Check ("najbolj oddaljen NPC je znotraj dometa sledilnika (Chebyshev {0:N1} <= 60) in upodobitve ({1:N1} <= {2:N1} - 4)" -f $cheb, $daljnji, $domet) (($cheb -le 60) -and ($daljnji -le $domet - 4))
} else {
    Check ("najbolj oddaljen NPC je znotraj dometa sledilnika ({0:N1} <= 60)" -f $daljnji) ($daljnji -le 60)
}

$cmds = @()
foreach ($l in (Get-Content (Join-Path $seed 'perf-setup-commands.txt'))) {
    $t = $l.Trim(); if ($t -eq '' -or $t.StartsWith('#') -or $t.StartsWith('save-all')) { continue }
    # Kamera je spectator; s tem pravilom PlayerChunkMap spectatorju ne nalaga chunkov (streznik nima igralca, zato ga tam ne potrebuje).
    if ($t -match 'spectatorsGenerateChunks') { continue }
    $cmds += $t
}
$cmds += 'difficulty 0'
$cmds += @(Get-SpawnCommands $Variant $N)
$pricakovano = $N
if ($Variant -eq 'boj' -or $Variant -eq 'nedosegljiva') {
    # Frakciji 1 in 2 sovrazni nastavi PERF_Kontrola (kot v perf-run.ps1); tudi on je NPC.
    $cmds += ('noppes clone spawn PERF_Kontrola 1 {0}' -f $kontrolaAt)
    $pricakovano = $N + 1
}
# Boj: NPC-ji umirajo ze med umiritvijo (streznik to steje kot ranjene), zato je meja 90 % kot pri K4.
$pricakovanoMin = if ($Variant -eq 'boj' -or $Variant -eq 'nedosegljiva') { [int][math]::Floor($N * 0.9) } else { $pricakovano }
if ($Posebni -eq 1) {
    foreach ($ime in $posebniNpc.Keys) { $cmds += ('noppes clone spawn {0} 1 {1},4,{2}' -f $ime, $posebniNpc[$ime].x, $zPosebni) }
    $cmds += 'rwhitbox mount RWV7_Jahac RWV7_Nosilec'
    $cmds += 'kill @e[type=customnpcs:customnpc,name=RWV7_Mrtev]'
    $pricakovano += $posebniNpc.Count
    # Neviden NPC (NpcVisible 1) morda ni poslan klientu; K17a to preveri posebej.
    $pricakovanoMin += $posebniNpc.Count - 1
}
$cmds += 'rwdiag chunks on 1'

$out = if ($JsonPath -ne '') { $JsonPath -replace '\.json$', '-klient.json' } else { Join-Path $audit "k-$Variant-$N-$stamp-klient.json" }
$jobPath = Join-Path $audit "k-$Variant-$N-$stamp.job"
$job = @("world=$world", 'createSeed=customnpc-rework', "expectNpc=$pricakovano", "expectNpcMin=$pricakovanoMin",
         # Invariantna kultura: v sl-SI bi -f dal decimalne vejice in HarnessJob kamere ne bi prebral.
         [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, 'cam={0},{1},{2},180,{3}', $camX, $camY, $camZ, $pitch),
         "renderDistance=$RenderDistance", "fov=$Fov", ('hideGui={0}' -f [int][bool]$SkrijGui), "sampleMs=$Vzorci", "headwearSkip=$Pokrivalo", ('headwearCheck={0}' -f [int][bool]$PreveriPokrivalo), "labelCache=$Napisi", ('labelCheck={0}' -f [int][bool]$PreveriNapise), "lodStart=$Lod", "farView=$Far", "impostorStart=$ImpostorStart", "renderBox=$RenderBox", ('specialPrefix={0}' -f $(if ($Posebni -eq 1) { 'RWV7_' } else { '' })), ('settle={0}' -f (3 + [int]($N / 250))),
         "warmup=$Warmup", "measure=$Measure", ('out=' + ($out -replace '\\', '/')))
for ($i = 0; $i -lt $cmds.Count; $i++) { $job += ('cmd.{0}={1}' -f ($i + 1), $cmds[$i]) }
[System.IO.File]::WriteAllLines($jobPath, $job, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("  navodilo: {0} ({1} ukazov)" -f $jobPath, $cmds.Count)

Step 2 'Zagon klienta'
& (Join-Path $root 'prepare-assets.ps1') | ForEach-Object { Write-Host "    $_" }
$env:JAVA_HOME = Join-Path $root '.tools\jdk8'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$log = Join-Path $audit "k-$Variant-$N-$stamp.log"
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $env:ComSpec
$psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwHarness={0}" > "{1}" 2>&1' -f ($jobPath -replace '\\', '/'), $log)
$psi.WorkingDirectory = Join-Path $root 'dev'
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)
$casMeja = (Get-Date).AddSeconds(600 + $Warmup + $Measure)
$tujiMed = @{}
$jfrPot = ''
$jfrIzpis = ''
if ($Jfr) {
    $jfrDir = Join-Path $audit 'jfr'
    New-Item -ItemType Directory -Force -Path $jfrDir | Out-Null
    $jfrPot = Join-Path $jfrDir "k-$Variant-$N-$stamp.jfr"
}
while (-not $proc.HasExited) {
    if ((Get-Date) -gt $casMeja) {
        Write-Host '  ! klient se ni koncal pravocasno; ubijam drevo procesov'
        try { & taskkill.exe /PID $proc.Id /T /F 2>&1 | Out-Null } catch { }
        break
    }
    $tekst = if (Test-Path $log) { Get-Content $log -Raw -ErrorAction SilentlyContinue } else { '' }
    # Harness brez navodila se ne namesti in klient bi obvisel v glavnem meniju do casovne meje.
    if ("$tekst" -match 'RWHARNESS-NAPAKA (navodilo|namestitev)') {
        Write-Host '  ! harness se ni namestil; ubijam drevo procesov'
        try { & taskkill.exe /PID $proc.Id /T /F 2>&1 | Out-Null } catch { }
        break
    }
    if ("$tekst" -match 'RWHARNESS-FAZA (WARMUP|MEASURE)') {
        foreach ($t in @(Get-TujiJava (Get-DrevoProcesov @($proc.Id)))) { $tujiMed[$t] = $true }
    }
    if ($Jfr -and $jfrIzpis -eq '' -and "$tekst" -match 'RWHARNESS-FAZA MEASURE') {
        # Klientski JVM je GradleStart (streznik bi bil GradleStartServer) v drevesu nasega procesa.
        $drevo = Get-DrevoProcesov @($proc.Id)
        $javaPid = -1
        foreach ($p in @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue)) {
            if ($drevo.ContainsKey([int]$p.ProcessId) -and ("$($p.CommandLine)" -match 'GradleStart(\s|$)')) { $javaPid = [int]$p.ProcessId }
        }
        if ($javaPid -gt 0) {
            # Posnetek se konca pred koncem merjenja, zato ga JVM zapise, preden se klient ugasne.
            $ErrorActionPreference = 'Continue'
            $jfrIzpis = (& (Join-Path $env:JAVA_HOME 'bin\jcmd.exe') $javaPid JFR.start name=rwk settings=profile ("duration={0}s" -f ($Measure - 6)) ("filename={0}" -f $jfrPot) 2>&1 | Out-String)
            $ErrorActionPreference = 'Stop'
        } else { $jfrIzpis = 'ni klientskega java procesa' }
        Write-Host ("  JFR (PID {0}): {1}" -f $javaPid, $jfrIzpis.Trim())
    }
    Start-Sleep -Seconds $(if ($Jfr -and $jfrIzpis -eq '') { 1 } else { 5 })
}
if ($skinProc) {
    try { if (-not $skinProc.HasExited) { Stop-Process -Id $skinProc.Id -Force } } catch { }
}
$tekst = if (Test-Path $log) { Get-Content $log -Raw } else { '' }
foreach ($m in [regex]::Matches("$tekst", 'RWHARNESS-[A-Z]+[^\r\n]*')) { if ($m.Value -notmatch '^RWHARNESS-UKAZ') { Write-Host "  $($m.Value)" } }
$neuspeliUkazi = @([regex]::Matches("$tekst", 'RWHARNESS-UKAZ 0 ([^\r\n]+)') | ForEach-Object { $_.Groups[1].Value })

Step 3 'Merila'
$r = $null
if (Test-Path $out) { $r = Get-Content $out -Raw | ConvertFrom-Json }
Check ("K1: klient je zapisal rezultat in prisel do konca meritve ({0})" -f $(if ($r) { "faza $($r.faza), napak $(@($r.napake).Count)" } else { 'ni zapisa' })) (($null -ne $r) -and ($r.faza -eq 'MEASURE') -and (@($r.napake).Count -eq 0))
# 'fill' odgovori 0, kadar ni bilo kaj spremeniti (ciscenje praznega pasu); vse ostalo mora uspeti.
$resniNeuspeli = @($neuspeliUkazi | Where-Object { $_ -notmatch '^fill .* minecraft:air$' })
Check ("K2: vsi ukazi prizorisca so uspeli" + $(if ($resniNeuspeli.Count) { ' - ' + ($resniNeuspeli -join '; ') } else { '' })) ($resniNeuspeli.Count -eq 0)
if ($r) {
    Check ("K3: v klientu je {0} NPC-jev (pricakovano {1}..{2})" -f $r.npcVKlientu, $pricakovanoMin, $pricakovano) (($r.npcVKlientu -ge $pricakovanoMin) -and ($r.npcVKlientu -le $pricakovano))
    # Idle in skripte stojijo na mestu: vsak vzorec mora upodobiti vse. Boj se razbezi, zato 90 %.
    $dovoljVzorcev = $r.upodobljenoVzorcev -ge [int]($Measure * 0.8)
    if ($Variant -eq 'idle' -or $Variant -eq 'skripte') {
        Check ("K4: upodobljenih entitet min {0} / max {1} v {2} vzorcih (prag {3})" -f $r.upodobljenoMin, $r.upodobljenoMax, $r.upodobljenoVzorcev, $N) ($dovoljVzorcev -and ($r.upodobljenoMin -ge $N))
    } else {
        # Boj: borci umirajo in se razbezijo; upodobljeni se primerjajo z zivimi v istem vzorcu.
        Check ("K4: upodobljenih min {0} / max {1}, zivih min {2}, delez upodobljenih min {3:P1} v {4} vzorcih (prag 90 %)" -f $r.upodobljenoMin, $r.upodobljenoMax, $r.zivihMin, $r.upodobljenoDelezMin, $r.upodobljenoVzorcev) ($dovoljVzorcev -and ($r.upodobljenoDelezMin -ge 0.9))
    }
    Check ("K5: kamera fiksna (odmik {0} blokov, {1} stopinj)" -f $r.kameraOdmikPoz, $r.kameraOdmikKot) (($r.kameraOdmikPoz -le 0.01) -and ($r.kameraOdmikKot -le 0.01))
    Check ("K6: nastavitve: vsync {0}, limit {1}, RD {2}, FOV {3}, skrit GUI {4}" -f $r.vsync, $r.limitFramerate, $r.renderDistance, $r.fov, $r.hideGui) (($r.vsync -eq 0) -and ($r.limitFramerate -ge 260) -and ($r.renderDistance -eq $RenderDistance) -and ([math]::Abs($r.fov - $Fov) -lt 0.01) -and ($r.hideGui -eq [int][bool]$SkrijGui))
    Check ("K7: {0} slicic v {1:N1} s" -f $r.slicic, $r.sekundMerjenja) (($r.slicic -ge 10 * $Measure) -and ([math]::Abs($r.sekundMerjenja - $Measure) -le 2))
}
Check ("K8: med ogrevanjem in merjenjem ni tekel tuj gradle build ali Minecraft" + $(if ($tujiMed.Count) { ' - ' + (@($tujiMed.Keys) -join '; ') } else { '' })) ($tujiMed.Count -eq 0 -or $DovoliTuje)
if ($r -and $Pokrivalo -ge 0) {
    # Vsi NPC-ji fixture imajo isti skin (SimpleTexture): z 1 vsak izris brez vrnitve na original, ena analiza na renderer.
    Check ("K10: RwHeadwearSkip {0} (zahtevano {1}), izrisov K14b {2}, na original {3} + {4} (stanje), analiz {5}, delov nic/nekaj/vse {6}/{7}/{8}" -f $r.rwHeadwearSkip, $Pokrivalo, $r.pokrivaloIzrisov, $r.pokrivaloOriginal, $r.pokrivaloOriginalStanje, $r.pokrivaloAnaliz, $r.pokrivaloDelovNic, $r.pokrivaloDelovNekaj, $r.pokrivaloDelovVse) (($r.rwHeadwearSkip -eq $Pokrivalo) -and ($PreveriPokrivalo -or ((($Pokrivalo -eq 0) -eq ($r.pokrivaloIzrisov -eq 0)) -and ($r.pokrivaloOriginal -eq 0) -and ($r.pokrivaloOriginalStanje -eq 0))))
}
if ($r -and $Napisi -ge 0) {
    # Prvi izris niza je original, drugi posname list; v 60 s so skoraj vsi izrisi z listom.
    Check ("K12: RwLabelCache {0} (zahtevano {1}), napisov z listom {2}, original {3}, prevodov {4}" -f $r.rwLabelCache, $Napisi, $r.napisiZListom, $r.napisiOriginal, $r.napisiPrevodov) (($r.rwLabelCache -eq $Napisi) -and ($PreveriNapise -or (($Napisi -eq 0) -eq ($r.napisiZListom -eq 0))))
}
if ($r -and $PreveriNapise) {
    foreach ($ser in ("$($r.napisiSerije)" -split ' \| ')) { Write-Host ("    napisi {0}" -f $ser) }
    Check ("K11: napisi bitno enaki z RwLabelCache 0 in 1 ({0} NPC, {1} v dometu imen; ime testnega sveta, barvne kode in sumniki, koda k po originalu), drug naslov spremeni {2} pikslov {3}" -f $r.napisiNpc, $r.napisiVDometu, $r.napisiObcutljivost, $r.napisiNapaka) ($r.napisiPreverba -eq 1)
}
if ($r -and $Lod -ge 0) {
    # Delez NPC-jev na stopnji L1 iz geometrije prizorisca (kot Lod.far: getDistanceSq od nog NPC-ja,
    # y = 4, sredina bloka mreze, do kamere); idle/skripte imajo eno mrezo od $gridZ naprej.
    $vsota = [double]$r.lodDalec + [double]$r.lodBlizu
    $delez = if ($vsota -gt 0) { $r.lodDalec / $vsota } else { 0 }
    $ok = ($r.rwLodStart -eq $Lod)
    if ($Lod -eq 0) {
        $ok = $ok -and ($vsota -eq 0)
        $opis = 'brez odlocitev L1'
    } else {
        $ok = $ok -and ($vsota -gt 0)
        $opis = 'delez L1 {0:P1}' -f $delez
        if ($Variant -eq 'idle' -or $Variant -eq 'skripte') {
            $dalec = 0
            for ($vr = 0; $vr -lt [int]($N / $gridW); $vr++) {
                for ($st = 0; $st -lt $gridW; $st++) {
                    $dx = $gridX + $st + 0.5 - $camX; $dy = 4 - $camY; $dz = $gridZ + $vr + 0.5 - $camZ
                    if (($dx * $dx + $dy * $dy + $dz * $dz) -gt ($Lod * $Lod)) { $dalec++ }
                }
            }
            $pric = $dalec / $N
            $ok = $ok -and ([math]::Abs($delez - $pric) -le 0.05)
            $opis += (', pricakovano {0:P1} ({1} od {2}, +-5 tock)' -f $pric, $dalec, $N)
        }
    }
    Check ("K15: RwLodStart {0} (zahtevano {1}), odlocitev pokrivala dalec/blizu {2}/{3}, {4}" -f $r.rwLodStart, $Lod, $r.lodDalec, $r.lodBlizu, $opis) $ok
}
if ($r -and $Far -ge 0) {
    # M5-V.6: stevilo NPC-jev v L2 iz geometrije prizorisca (kot FarRender: getDistanceSq od nog NPC-ja, y = 4,
    # sredina bloka mreze, do kamere = polozaja igralca); staticni NPC-ji, histereza ne vpliva. Vsi so v dometu
    # upodobitve (K4), zato izven dometa 0.
    $prag = if ($ImpostorStart -ge 0) { [math]::Max(8, [math]::Min(512, $ImpostorStart)) } else { [int]$r.rwImpostorStart }
    $ok = ($r.rwFarView -eq $Far)
    if ($Far -lt 2) {
        $ok = $ok -and ($r.l2NarisanihMax -le 0) -and ($r.l2Preklicev -eq 0)
        $opis = 'brez L2'
    } else {
        $opis = 'L2 na slicico min/max {0}/{1}, izven dometa {2}, preklicev {3}, brez peke v oknu {4}, videzov {5}, napak {6}' -f $r.l2NarisanihMin, $r.l2NarisanihMax, $r.l2IzvenDometaMax, $r.l2Preklicev, $r.l2BrezPeke, $r.l2Videzov, $r.l2VidezNapak
        # M5-V.7 (-Posebni): NPC brez hitboxa (RWV7_Kip) je izven vanilla dometa upodobitve, zato natanko 1;
        # z RwRenderBox 1 (M5-K K12) ima domet, kot bi ga imel s hitboxom, in je preklican L2 kot ostali.
        $izvenPric = if ($Posebni -eq 1 -and $r.rwRenderBox -ne 1) { 1 } else { 0 }
        $ok = $ok -and ($r.l2BrezPeke -eq 0) -and ($r.l2VidezNapak -eq 0) -and ($r.l2IzvenDometaMax -eq $izvenPric) -and ($r.farPoseg -eq 1) -and ($r.farIzrisRezerva -eq 0)
        if ($Variant -eq 'idle' -or $Variant -eq 'skripte') {
            $vL2 = 0
            for ($vr = 0; $vr -lt [int]($N / $gridW); $vr++) {
                for ($st = 0; $st -lt $gridW; $st++) {
                    $dx = $gridX + $st + 0.5 - $camX; $dy = 4 - $camY; $dz = $gridZ + $vr + 0.5 - $camZ
                    if (($dx * $dx + $dy * $dy + $dz * $dz) -gt ($prag * $prag)) { $vL2++ }
                }
            }
            # M5-V.7: L2 posebnih NPC-jev na slicico: velik, pol-viden, kip (izven dometa), morf, URL = 5
            # (neviden, jahac, nosilec in ubit ostanejo model ali niso narisani).
            if ($Posebni -eq 1) { $vL2 += 5 }
            # M5-K K12: RWV7_Mec stoji onkraj praga.
            if ($Posebni -eq 1 -and $RenderBox -ge 0) { $vL2 += 1 }
            $tol = [math]::Max(2, [int][math]::Ceiling($N * 0.02))
            $ok = $ok -and ($vL2 -gt 0) -and ([math]::Abs($r.l2NarisanihMin - $vL2) -le $tol) -and ([math]::Abs($r.l2NarisanihMax - $vL2) -le $tol)
            $opis += (', pricakovano {0} od {1} (+-{2}, prag {3})' -f $vL2, $N, $tol, $prag)
        }
    }
    Check ("K16: RwFarView {0} (zahtevano {1}), RwImpostorStart {2}, {3}" -f $r.rwFarView, $Far, $r.rwImpostorStart, $opis) $ok
}
if ($Posebni -eq 1) {
    # M5-V.7: posebni NPC-ji v merilnem oknu; stevci ponujen/preklican/polViden/izvenDometa/jahanje/jahanjePreklican@RGB.
    $sp = @{}
    if ($r) {
        foreach ($del in ("$($r.posebni)" -split ';')) {
            if ($del -match '^(RWV7_\w+)=(\d+)/(\d+)/(\d+)/(\d+)/(\d+)/(\d+)@([0-9A-F]{6}|-)$') {
                $sp[$Matches[1]] = @{ pon = [long]$Matches[2]; prek = [long]$Matches[3]; pol = [long]$Matches[4]; izven = [long]$Matches[5]; jah = [long]$Matches[6]; jahPrek = [long]$Matches[7]; rgb = $Matches[8] }
            }
        }
        Write-Host ("    posebni: {0}" -f $r.posebni)
    }
    function SpOk([string]$Ime, [scriptblock]$Pogoj) { if (-not $sp.ContainsKey($Ime)) { return $false }; return [bool](& $Pogoj $sp[$Ime]) }
    # Impostor (L2): preklican model v vsaki ponujeni slicici (toleranca 2) ali narisan izven vanilla dometa.
    $impostor = { param($c) (($c.prek -gt 0) -and ($c.prek -ge $c.pon - 2)) -or (($c.pon -eq 0) -and ($c.izven -gt 0)) }
    $model = { param($c) ($c.prek -eq 0) -and ($c.izven -eq 0) }
    Check ("K17a: vseh {0} posebnih NPC-jev je v svetu klienta ({1})" -f $posebniNpc.Count, (@($sp.Keys | Sort-Object) -join ', ')) ($sp.Count -eq $posebniNpc.Count)
    Check 'K17b: RWV7_Velik (Size 15) je impostor' (SpOk 'RWV7_Velik' $impostor)
    Check 'K17c: RWV7_Polviden (NpcVisible 2) je pol-prozoren impostor' (SpOk 'RWV7_Polviden' { param($c) (& $impostor $c) -and ($c.pol -gt 0) })
    Check 'K17d: RWV7_Nevidni (NpcVisible 1) ni impostor' (SpOk 'RWV7_Nevidni' $model)
    if ($r -and $r.rwRenderBox -eq 1) {
        # M5-K K12: z okvirjem modela je NPC brez hitboxa v dometu vanille in preklican L2 (kot NPC s hitboxom).
        Check 'K17e: RWV7_Kip (brez hitboxa, RwRenderBox 1) je v dometu vanille in impostor L2' (SpOk 'RWV7_Kip' { param($c) (& $impostor $c) -and ($c.pon -gt 0) -and ($c.izven -eq 0) })
    } else {
        Check 'K17e: RWV7_Kip (brez hitboxa, izven vanilla dometa) je impostor do konca megle' (SpOk 'RWV7_Kip' { param($c) ($c.pon -eq 0) -and ($c.izven -gt 0) })
    }
    Check 'K17f: RWV7_Jahac in RWV7_Nosilec jahata in ostaneta model' ((SpOk 'RWV7_Jahac' { param($c) ($c.jah -gt 0) -and ($c.jah -eq $c.pon) -and (& $model $c) }) -and (SpOk 'RWV7_Nosilec' { param($c) ($c.jah -gt 0) -and ($c.jah -eq $c.pon) -and (& $model $c) }))
    Check 'K17g: RWV7_Mrtev (ubit) ni impostor' (SpOk 'RWV7_Mrtev' $model)
    Check 'K17h: RWV7_Morf (model prasica) je impostor' (SpOk 'RWV7_Morf' $impostor)
    # Skin z URL-ja: peka je cakala na prenos (impPrenosCakanj > 0), pecen okvir ima barvo prenesenega skina (magenta).
    $urlRgb = if ($sp.ContainsKey('RWV7_Url')) { $sp['RWV7_Url'].rgb } else { '-' }
    $magenta = $false
    if ($urlRgb -match '^[0-9A-F]{6}$') {
        $cr = [Convert]::ToInt32($urlRgb.Substring(0, 2), 16); $cg = [Convert]::ToInt32($urlRgb.Substring(2, 2), 16); $cb = [Convert]::ToInt32($urlRgb.Substring(4, 2), 16)
        $magenta = ($cr -ge 2 * $cg + 40) -and ($cb -ge 2 * $cg + 40)
    }
    $zahtev = if ($skinLog -and (Test-Path $skinLog)) { @(Get-Content $skinLog | Where-Object { $_ -match ' GET /rwv7.png ' }).Count } else { 0 }
    Check ("K17i: RWV7_Url je impostor s prenesenim skinom (barva okvirja {0}, cakanj na prenos {1}, zahtev streznika {2})" -f $urlRgb, $(if ($r) { $r.impPrenosCakanj } else { '-' }), $zahtev) ((SpOk 'RWV7_Url' $impostor) -and $magenta -and ($null -ne $r) -and ($r.impPrenosCakanj -gt 0) -and ($zahtev -ge 1))
    if ($r) {
        Check ("K17j: brez OptiFine je zdruzljivost neaktivna (optifine {0}, sencila {1}, izklopljenih slicic {2}, sencnih preskokov {3})" -f $r.farOptifine, $r.farSencila, $r.farSencilaIzklop, $r.farSencniPreskok) (($r.farOptifine -eq 0) -and ($r.farSencila -eq 0) -and ($r.farSencilaIzklop -eq 0) -and ($r.farSencniPreskok -eq 0))
    }
}
if ($Posebni -eq 1 -and $RenderBox -ge 0) {
    # M5-K K12: okvir upodobitve ob koncu merjenja (RenderBoxAudit): npc, niVsebuje, izvenDometa, brezHitboxa, videzov,
    # videzIzven (presezek pecenega obrisa modela cez okvir > 0,05 x Size/5), najvecPresega; posebni domet/vsebuje/presega.
    $ok = @{}
    $okSp = @{}
    if ($r) {
        foreach ($del in ("$($r.okvir)" -split ';')) {
            if ($del -match '^(RWV7_\w+)=([01])/([01])/(-|-?[0-9.]+)$') { $okSp[$Matches[1]] = @{ domet = [int]$Matches[2]; vsebuje = [int]$Matches[3]; presega = $Matches[4] } }
            elseif ($del -match '^(\w+)=(-|-?[0-9.]+)$') { $ok[$Matches[1]] = $Matches[2] }
        }
        Write-Host ("    okvir: {0}" -f $r.okvir)
    }
    function OkNum([string]$K) { if ($ok.ContainsKey($K) -and $ok[$K] -ne '-') { return [double]::Parse($ok[$K], [System.Globalization.CultureInfo]::InvariantCulture) }; return [double]::NaN }
    function SpPresega([string]$Ime) { if ($okSp.ContainsKey($Ime) -and $okSp[$Ime].presega -ne '-') { return [double]::Parse($okSp[$Ime].presega, [System.Globalization.CultureInfo]::InvariantCulture) }; return [double]::NaN }
    $sObris = @('RWV7_Velik', 'RWV7_Polviden', 'RWV7_Kip', 'RWV7_Morf', 'RWV7_Url', 'RWV7_Mec')
    $brezObrisa = @($sObris | Where-Object { [double]::IsNaN((SpPresega $_)) })
    Check ("K18a: RwRenderBox {0} (zahtevano {1}), NPC-jev {2}, brez hitboxa {3}" -f $(if ($r) { $r.rwRenderBox } else { '-' }), $RenderBox, (OkNum 'npc'), (OkNum 'brezHitboxa')) (($null -ne $r) -and ($r.rwRenderBox -eq $RenderBox) -and ((OkNum 'npc') -ge $pricakovanoMin) -and ((OkNum 'brezHitboxa') -ge 1))
    Check ("K18b: okvir upodobitve objame hitbox pri vseh NPC-jih (ne objame {0})" -f (OkNum 'niVsebuje')) ((OkNum 'niVsebuje') -eq 0)
    Check ("K18c: pecen obris modela za {0} posebnih NPC-jev (brez obrisa: {1}), videzov skupaj {2}" -f ($sObris.Count - $brezObrisa.Count), ($brezObrisa -join ', '), (OkNum 'videzov')) ($brezObrisa.Count -eq 0)
    $kip = if ($okSp.ContainsKey('RWV7_Kip')) { $okSp['RWV7_Kip'] } else { $null }
    $obrisi = ($sObris | ForEach-Object { '{0} {1}' -f ($_ -replace '^RWV7_', ''), $(if ($okSp.ContainsKey($_)) { $okSp[$_].presega } else { '-' }) }) -join ', '
    if ($RenderBox -eq 1) {
        Check ("K18d: z okvirjem modela je vsak sledeni NPC v dometu upodobitve (izven {0}; Kip domet {1})" -f (OkNum 'izvenDometa'), $(if ($kip) { $kip.domet } else { '-' })) (((OkNum 'izvenDometa') -eq 0) -and ($null -ne $kip) -and ($kip.domet -eq 1))
        Check ("K18e: pecen obris ne sega cez okvir (cez mejo {0}, najvec {1} bloka; {2})" -f (OkNum 'videzIzven'), (OkNum 'najvecPresega'), $obrisi) (((OkNum 'videzIzven') -eq 0) -and ((OkNum 'videzov') -ge $sObris.Count))
    } else {
        # Obcutljivost: hitbox originala je ozji od rok (0,3 proti ~0,47), crta NPC-ja brez hitboxa pa skoraj nic.
        Check ("K18d: brez okvirja modela je NPC brez hitboxa izven dometa (izven {0}; Kip domet {1})" -f (OkNum 'izvenDometa'), $(if ($kip) { $kip.domet } else { '-' })) (((OkNum 'izvenDometa') -ge 1) -and ($null -ne $kip) -and ($kip.domet -eq 0))
        Check ("K18e: obcutljivost - obris modela sega cez hitbox originala (cez mejo {0}, Kip {1} bloka; {2})" -f (OkNum 'videzIzven'), $(if ($kip) { $kip.presega } else { '-' }), $obrisi) (((OkNum 'videzIzven') -ge 1) -and ((SpPresega 'RWV7_Kip') -gt 0.3))
    }
}
if ($r -and $PreveriPokrivalo) {
    foreach ($ser in ("$($r.pokrivaloSerije)" -split ' \| ')) { Write-Host ("    pokrivalo {0}" -f $ser) }
    Check ("K9: slika in globina pokrivala bitno enaki z RwHeadwearSkip 0 in 1 ({0} NPC; skin, vzorec, 5 pravih skinov), vzorec spremeni {1} pikslov {2}" -f $r.pokrivaloNpc, $r.pokrivaloObcutljivost, $r.pokrivaloNapaka) ($r.pokrivaloPreverba -eq 1)
}
if ($Jfr) {
    $jfrOk = ($jfrIzpis -match 'Started recording') -and (Test-Path $jfrPot) -and ((Get-Item $jfrPot).Length -gt 0)
    Check ("J1: JFR posnetek render niti zapisan ({0})" -f $jfrPot) $jfrOk
    if ($jfrOk) {
        $md = $jfrPot -replace '\.jfr$', '.md'
        $ErrorActionPreference = 'Continue'
        & node (Join-Path $root 'dev\tools\jfr-povzetek.js') $jfrPot --nit 'Client thread' --native --top 30 --md $md 2>&1 | Out-Null
        $ErrorActionPreference = 'Stop'
        Check ("J2: povzetek JFR ({0})" -f $md) (Test-Path $md)
    }
}
if ($Vzorci -gt 0) {
    $skladi = $out -replace '\.json$', '-skladi.txt'
    $nVz = if ($r) { [int]$r.vzorcevSklada } else { 0 }
    # Ob 1 ms bi bilo 1000/s; getStackTrace potrebuje varno tocko, zato je vsaj 100/s dovolj za delez +-1 %.
    Check ("J3: vzorci sklada render niti ({0}, {1:N0} na s, {2:N1} us na vzorec)" -f $nVz, ($nVz / [math]::Max(1, $Measure)), $(if ($r) { $r.vzorcevalnikUsNaVzorec } else { 0 })) ((Test-Path $skladi) -and ($nVz -ge 100 * $Measure))
    if (Test-Path $skladi) {
        $ErrorActionPreference = 'Continue'
        & node (Join-Path $root 'dev\tools\skladi-povzetek.js') $skladi --md ($skladi -replace '\.txt$', '.md') 2>&1 | Out-Null
        $ErrorActionPreference = 'Stop'
    }
}

if ($r) {
    Write-Host ''
    Write-Host ("  {0}-{1}: slicica p50 {2:N2} / p95 {3:N2} / p99 {4:N2} / max {5:N1} ms, FPS {6:N1}, 1 % low {7:N1}, 0,1 % low {8:N1}, GC {9} ({10} ms), integrirani streznik {11:N2} ms/tick" -f `
        $Variant, $N, $r.frameMsP50, $r.frameMsP95, $r.frameMsP99, $r.frameMsMax, $r.fps, $r.fpsLow1, $r.fpsLow01, $r.gcStevilo, $r.gcMs, $r.integriraniStreznikMsPovp)
    if ($JsonPath -ne '') {
        $odtis = @{ varianta = $Variant; npc = $N; ogrevanje = $Warmup; sekund = $Measure; rd = $RenderDistance; fov = $Fov;
                    kamera = [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, '{0},{1},{2},180,{3}', $camX, $camY, $camZ, $pitch); sirina = $r.sirina; visina = $r.visina; jfr = [int][bool]$Jfr; skritGui = [int][bool]$SkrijGui }
        # Samo, ce je vklopljen: baseline M5-K.1 (18. 10.) kljuca nima, odtis ostane primerljiv.
        if ($Vzorci -gt 0) { $odtis['vzorci'] = $Vzorci }
        if ($Pokrivalo -ge 0) { $odtis['rwHeadwearSkip'] = $Pokrivalo }
        if ($Napisi -ge 0) { $odtis['rwLabelCache'] = $Napisi }
        if ($Lod -ge 0) { $odtis['rwLodStart'] = $Lod }
        if ($Far -ge 0) { $odtis['rwFarView'] = $Far }
        if ($ImpostorStart -ge 0) { $odtis['rwImpostorStart'] = $ImpostorStart }
        if ($Dalec -ne 0) { $odtis['dalec'] = $Dalec }
        if ($Posebni -ne 0) { $odtis['posebni'] = $Posebni }
        if ($RenderBox -ge 0) { $odtis['rwRenderBox'] = $RenderBox }
        $vel = @{ 'frame.p50' = $r.frameMsP50; 'frame.p95' = $r.frameMsP95; 'frame.p99' = $r.frameMsP99; 'frame.max' = $r.frameMsMax;
                  'fps' = $r.fps; 'fps.low1' = $r.fpsLow1; 'fps.low01' = $r.fpsLow01; 'gc.stevilo' = $r.gcStevilo; 'gc.ms' = $r.gcMs;
                  'upodobljeno.min' = $r.upodobljenoMin; 'streznik.ms' = $r.integriraniStreznikMsPovp }
        $null = Write-MeritevJson -Path $JsonPath -Paket 'M5-K.0' -Scenarij ("klient {0}-{1}" -f $Variant, $N) -Odtis $odtis -Velicine $vel -Uspeh ($failures.Count -eq 0) -Padle $failures
    }
}
$zaklep.Close()
Write-Host ''
if ($failures.Count -gt 0) { Write-Host ("M5-K.0 PADLO: {0} meril" -f $failures.Count); exit 1 }
Write-Host 'M5-K.0 K1-K8 USPESNO.'
exit 0
