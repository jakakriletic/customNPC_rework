<#
    v-run.ps1 - odlocilni poskus M5-V.1 (D-034, D-035): prizorisce "dalec" na dediciranem strezniku
    s prikljucenim klientom.

    Ena celica na zagon. Dedicated server (runServer, lasten svet dev\run\rwv-dalec prek --world, svez
    vsak zagon) in razvojni klient (runClient, harness M5-K.0 v nacinu connect=) teceta hkrati.
    Prizorisce: N NPC-jev PERF_Idle v gruckah 5 x 5, enakomerno po povrsini v izseku +-35 stopinj
    pred kamero na razdalji 20..358 blokov (cela gruca znotraj Chebyshev 368 = (24 - 1) * 16), kamera
    12 blokov nad tlemi pri (0,5; 16; 3000,5), yaw 180, pitch 10 - dalec od obmocja spawna.
    Scenarij in razlaga: docs/scenariji/M5-V.1-dalec.md

    Potek: streznik -> ukazi determinizma -> 'rwdiag vd 24' (prizorisce se vedno spawna pri 24, nato se
    razdalja zmanjsa na celico, chunki onkraj se izpraznijo kot na pravem strezniku) -> varianta A
    'rwdiag track 368' -> klient se poveze -> gamemode 3 + tp -> cakanje na chunke -> spawn grucic ->
    'rwdiag vd <Vd>' -> 'rwdiag spawnnbt' -> 'say RWHARNESS-SCENA' -> klient steje NPC-je, se ogreje,
    meri; streznik ima 'rwdiag on' samo v klientovem merilnem oknu -> 'rwdiag dump'.

    Celice (plan M5-V.1): -Domet 64 (original, sledilnik CNPC) ali 368 (varianta A, s klientsko
    renderDistanceWeight 6), oboje pri -Vd 10 in 24, -N 1000 ali 5000. Klientski RD je enak -Vd.

    Uporaba:
        .\v-run.ps1 -Vd 24 -Domet 64 -N 1000
        .\ponovitve-run.ps1 -Scenarij dalec -Dodatno @('-Vd','24','-Domet','368','-N','5000')

    M5-V.2: -Far 1 (samo z -Domet 64) vklopi kanal rwfar: klient z RwFarView=1 poslje HELLO, streznik po
    postavitvi prizorisca dobi 'rwfar 1' in 'rwfar domet 384', v merilnem oknu se stevci rwfar ponastavijo.
    Merila V16-V18: register NPC-jev = loadedEntityList, nadomestkov v klientu toliko, kot je NPC-jev v pasu
    (Chebyshev (56, min(384, (vd - 1) * 16)], v gledanih chunkih), cas kode rwfar <= 100 us na tick na igralca.
    M5-V.3: V19 - opis videza: kloni PERF_Idle si delijo en videz, vsak NPC v pasu ima izracunan videz, klient
    pozna opis za vsak nadomestek (hash preverjen ob prejemu), brez zavrnjenih zahtev in napak.
    M5-V.4: -Impostor 1 (samo z -Far 1): klient spece impostorje (en videz na slicico) in po ogrevanju primerja
    piksle impostor - model pri 56 blokih (ImpostorCheck). V20: vsak nadomestek z opisom ima pecen videz, brez napak
    peke, framebuffer na voljo. V21: primerjava pikslov (IoU >= 0,70, razlika barve <= 32 za 8 videzov x 8 kotov,
    obcutljivost > 64).
    M5-V.5: -Izris 1 (samo z -Impostor 1): izris L3 (FarRender, poseg coremoda v RenderGlobal.renderEntities). Prizorisce
    dobi eno sondo PERF_Idle naravnost pred kamero na zunanjem robu pasu ((vd - 1) * 16 blokov). V22: v merjenju vsako
    slicico narisani vsi nadomestki onkraj sledilnika (64), nobeden izven frustuma ali brez peke, prek coremoda (brez
    rezerve). V23: po ogrevanju A/B pikslov sonde v pravi slicici (kamera usmerjena vanjo, FOV 10, izris vklopljen /
    izklopljen / vklopljen; razlika >= 1 piksel, kontrola 0). V24: po merjenju kamera hodi s hitrostjo hoje 48 blokov nazaj
    in nazaj; nobena slicica ne izgubi prej narisanega NPC-ja (ziv ali impostor), prehodi v obe smeri.

    Merila V0-V15 (in V16-V19 z -Far 1) so na koncu; izhodna koda 0 samo, ce so vsa zelena.
#>
param(
    [ValidateSet(10, 24)][int]$Vd = 24,
    [ValidateSet(64, 368)][int]$Domet = 64,
    [ValidateSet(1000, 5000)][int]$N = 1000,
    # M5-V.2: 1 = nadomestki oddaljenih NPC-jev (kanal rwfar); 0 = original (M5-V.1).
    [ValidateSet(0, 1)][int]$Far = 0,
    # M5-V.4: 1 = peka impostorjev in primerjava pikslov impostor - model (samo z -Far 1).
    [ValidateSet(0, 1)][int]$Impostor = 0,
    # M5-V.5: 1 = izris L3 in preverbi V22-V24 (samo z -Impostor 1).
    [ValidateSet(0, 1)][int]$Izris = 0,
    [int]$Warmup = 30,
    [int]$Measure = 60,
    [int]$Fov = 70,
    [string]$JsonPath = '',
    [switch]$DovoliTuje,
    # ponovitve-run.ps1 ga lahko poda; EULA mora biti ze sprejeta v dev\run\eula.txt.
    [switch]$AcceptEula
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
$inv = [System.Globalization.CultureInfo]::InvariantCulture

# --- geometrija prizorisca (deterministicna) ---
$camX = 0.5; $camY = 16.0; $camZ = 3000.5; $yaw = 180; $pitch = 10
$tla = 4
$rMin = 20.0; $rMax = 358.0; $polKota = 35.0 * [math]::PI / 180.0
$phi = 0.6180339887498949
$gruc = [int]($N / 25)
$gruce = New-Object System.Collections.ArrayList
for ($k = 0; $k -lt $gruc; $k++) {
    $r = [math]::Sqrt($rMin * $rMin + ($rMax * $rMax - $rMin * $rMin) * ($k + 0.5) / $gruc)
    $ok = $false
    for ($t = 0; $t -lt 40 -and -not $ok; $t++) {
        $f = (($k * $phi) + $t * 0.025) % 1.0
        $th = ($f - 0.5) * 2.0 * $polKota
        $bx = [int][math]::Floor($camX + $r * [math]::Sin($th))
        $bz = [int][math]::Floor($camZ - $r * [math]::Cos($th))
        $prosto = $true
        foreach ($c in $gruce) { if ([math]::Max([math]::Abs($c[0] - $bx), [math]::Abs($c[1] - $bz)) -lt 6) { $prosto = $false; break } }
        if ($prosto) { [void]$gruce.Add(@($bx, $bz)); $ok = $true }
    }
    if (-not $ok) { throw "gruce $k ni mogoce postaviti brez prekrivanja" }
}
# Polozaji NPC-jev: sredina bloka (CmdClone.grid postavi NPC na x + i, z + j).
$npcPos = New-Object System.Collections.ArrayList
foreach ($c in $gruce) { for ($i = -2; $i -le 2; $i++) { for ($j = -2; $j -le 2; $j++) { [void]$npcPos.Add(@(($c[0] + $i + 0.5), ($c[1] + $j + 0.5))) } } }
# M5-V.5: sonda - en NPC naravnost pred kamero (yaw 180 = -Z) na zunanjem robu pasu rwfar, Chebyshev (vd - 1) * 16.
$sonda = $null
if ($Izris -eq 1) {
    $sonda = @([int][math]::Floor($camX), [int][math]::Floor($camZ - ($Vd - 1) * 16))
    foreach ($c in $gruce) { if ([math]::Max([math]::Abs($c[0] - $sonda[0]), [math]::Abs($c[1] - $sonda[1])) -lt 6) { throw 'sonda se prekriva z gruco' } }
    [void]$npcPos.Add(@(($sonda[0] + 0.5), ($sonda[1] + 0.5)))
}
$nVseh = $npcPos.Count
function Get-Sledenih([int]$R) {
    $n = 0
    foreach ($p in $npcPos) { if (([math]::Abs($p[0] - $camX) -le $R) -and ([math]::Abs($p[1] - $camZ) -le $R)) { $n++ } }
    return $n
}
function Get-Nalozenih([int]$Razdalja) {
    $pcx = [math]::Floor($camX / 16); $pcz = [math]::Floor($camZ / 16); $n = 0
    foreach ($p in $npcPos) {
        if (([math]::Abs([math]::Floor($p[0] / 16) - $pcx) -le $Razdalja) -and ([math]::Abs([math]::Floor($p[1] / 16) - $pcz) -le $Razdalja)) { $n++ }
    }
    return $n
}
# Sledilnik: |dx|, |dz| <= min(domet, (vd - 1) * 16) (EntityTrackerEntry.isVisibleTo); NPC tocno na
# meji je lahko v ali izven, zato meja +-1 blok.
$Rsl = [math]::Min($Domet, ($Vd - 1) * 16)
$slMin = Get-Sledenih ($Rsl - 1)
$slMax = Get-Sledenih ($Rsl + 1)
$nal24 = Get-Nalozenih 24
$nalVd = Get-Nalozenih $Vd
$daljnji = 0.0; $najblizji = 1e9
foreach ($p in $npcPos) { $d = [math]::Sqrt([math]::Pow($p[0] - $camX, 2) + [math]::Pow($p[1] - $camZ, 2)); $daljnji = [math]::Max($daljnji, $d); $najblizji = [math]::Min($najblizji, $d) }
if ($Far -eq 1 -and $Domet -ne 64) { throw '-Far 1 je mogoc samo z -Domet 64 (pas rwfar se zacne pri sledilniku).' }
if ($Impostor -eq 1 -and $Far -ne 1) { throw '-Impostor 1 potrebuje -Far 1 (opis videza pride po kanalu rwfar).' }
if ($Izris -eq 1 -and $Impostor -ne 1) { throw '-Izris 1 potrebuje -Impostor 1 (izris L3 rise pecene impostorje).' }
# M5-V.2: pas rwfar (FarBand): (min(64, meja) - 8, min(RwFarRange 384, zeleni domet klienta RD * 16, meja)], meja = (vd - 1) * 16,
# samo NPC-ji v chunkih, ki jih igralec gleda (kvadrat vd chunkov). NPC tocno na meji je lahko v ali izven, zato +-1 blok.
$farMeja = ($Vd - 1) * 16
$farNotr = [math]::Min(64, $farMeja) - 8
$farZun = [math]::Min([math]::Min(384, $Vd * 16), $farMeja)
function Get-Pas([double]$Lo, [double]$Hi) {
    $pcx = [math]::Floor($camX / 16); $pcz = [math]::Floor($camZ / 16); $n = 0
    foreach ($p in $npcPos) {
        $c = [math]::Max([math]::Abs($p[0] - $camX), [math]::Abs($p[1] - $camZ))
        $vChunku = ([math]::Abs([math]::Floor($p[0] / 16) - $pcx) -le $Vd) -and ([math]::Abs([math]::Floor($p[1] / 16) - $pcz) -le $Vd)
        if ($vChunku -and $c -gt $Lo -and $c -le $Hi) { $n++ }
    }
    return $n
}
$pasMin = Get-Pas ($farNotr + 1) ($farZun - 1)
$pasMax = Get-Pas ($farNotr - 1) ($farZun + 1)
# M5-V.5: narisani impostorji so nadomestki onkraj sledilnika (64); med (64 - 8, 64] ima klient zivo entiteto (siv).
$izrMin = Get-Pas ([math]::Min(64, $farMeja) + 1) ($farZun - 1)
$izrMax = Get-Pas ([math]::Min(64, $farMeja) - 1) ($farZun + 1)
$sivMin = Get-Pas ($farNotr + 1) ([math]::Min(64, $farMeja) - 1)
$sivMax = Get-Pas ($farNotr - 1) ([math]::Min(64, $farMeja) + 1)
$renderWeight = if ($Domet -gt 64) { 6.0 } else { 0.0 }
$pricWeight = if ($renderWeight -gt 0) { $renderWeight } else { [math]::Min(2.5, [math]::Max(1.0, $Vd / 8.0)) }

Step 1 'Parametri in priprava'
Write-Host ("  celica: vd {0}, domet {1} (sledilnik {2}), N {3} v {4} gruckah, NPC od {5:N1} do {6:N1} blokov" -f $Vd, $Domet, $Rsl, $N, $gruc, $najblizji, $daljnji)
if ($Far -eq 1) { Write-Host ("  rwfar: pas ({0}, {1}], pricakovanih nadomestkov {2}..{3}" -f $farNotr, $farZun, $pasMin, $pasMax) }
if ($Izris -eq 1) { Write-Host ("  izris L3: sonda pri ({0}, {1}), pricakovanih impostorjev {2}..{3}, zivih v pasu {4}..{5}" -f ($sonda[0] + 0.5), ($sonda[1] + 0.5), $izrMin, $izrMax, $sivMin, $sivMax) }
Write-Host ("  pricakovano: nalozenih pri 24 {0}, pri {1} {2}; v klientu (sledenih) {3}..{4}" -f $nal24, $Vd, $nalVd, $slMin, $slMax)
Check ("V0a: vsi NPC-ji prizorisca so znotraj 24 chunkov ({0} od {1})" -f $nal24, $nVseh) ($nal24 -eq $nVseh)
$zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'v-run'
Check 'zaklep scenarija (.scenarij.lock)' $true
$tuji0 = @(Get-TujiJava)
if ($tuji0.Count -gt 0 -and -not $DovoliTuje) {
    Check ('V0: pred zagonom ne tece tuj gradle build ali Minecraft - ' + ($tuji0 -join '; ')) $false
    throw 'Tuj java proces bi meril skupaj s serverjem in klientom. Ustavi ga ali pozeni z -DovoliTuje (ni baseline).'
}
Check ('V0: pred zagonom ne tece tuj gradle build ali Minecraft' + $(if ($tuji0.Count) { ' (-DovoliTuje: ' + ($tuji0 -join '; ') + ')' } else { '' })) $true

$eulaFile = Join-Path $run 'eula.txt'
if (-not ((Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true'))) { throw 'dev\run\eula.txt ni sprejet (glej perf-run.ps1 -AcceptEula).' }
$props = Join-Path $run 'server.properties'
if (-not (Test-Path $props)) { throw 'dev\run\server.properties ne obstaja. Pozeni najprej .\testworld.ps1.' }
$port = 25565
$pl = @(Get-Content $props | Where-Object { $_ -match '^server-port\s*=' })
if ($pl.Count) { $port = [int](($pl[0] -split '=', 2)[1].Trim()) }
$offline = @(Get-Content $props | Where-Object { $_ -match '^online-mode\s*=\s*false' }).Count -gt 0
Check ("server.properties: online-mode=false (dev klient nima prijave), vrata {0}" -f $port) $offline
if (-not $offline) { throw 'online-mode mora biti false.' }

$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = if ($Izris -eq 1) { "v-$Vd-$Domet-$N-izris-$stamp" } elseif ($Impostor -eq 1) { "v-$Vd-$Domet-$N-imp-$stamp" } elseif ($Far -eq 1) { "v-$Vd-$Domet-$N-far-$stamp" } else { "v-$Vd-$Domet-$N-$stamp" }
$svet = 'rwv-dalec'
$svetDir = Join-Path $run $svet
# Svez svet vsak zagon (D-016). Mapa je nasa (predpona rwv-), ustvari jo samo ta skripta.
if (Test-Path $svetDir) { Remove-Item -Recurse -Force $svetDir }
New-Item -ItemType Directory -Force -Path (Join-Path $svetDir 'customnpcs') | Out-Null
Copy-Item -Path (Join-Path $seed 'customnpcs\*') -Destination (Join-Path $svetDir 'customnpcs') -Recurse -Force
$nKlonov = @(Get-ChildItem (Join-Path $svetDir 'customnpcs\clones\1') -Filter 'PERF_*.json').Count
Check ("fixture PERF_* namescene v svet ({0})" -f $nKlonov) ($nKlonov -ge 4)

$env:JAVA_HOME = Join-Path $root '.tools\jdk8'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

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
function Send-Command($srv, [string]$Cmd) {
    $srv.Proc.StandardInput.WriteLine($Cmd)
    $srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}
function Wait-ForCount($srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        if (([regex]::Matches((Get-MarkerText $srv.Log), [regex]::Escape($Marker))).Count -ge $Count) { return $true }
        $sesutje = Get-ServerSesutje (Get-LogText $srv.Log)
        if ($sesutje -ne '') { throw ("Server se je sesul (cakal na '{0}'): {1}" -f $Marker, $sesutje) }
        if ($srv.Proc.HasExited -or (Get-Date) -ge $deadline) { return $false }
        Start-Sleep -Milliseconds 500
    }
}
# Poslje ukaz in vrne prvo novo vrstico z markerjem ('' ob timeoutu).
function Send-Query($srv, [string]$Cmd, [string]$Marker, [int]$TimeoutSec = 20) {
    $pred = ([regex]::Matches((Get-MarkerText $srv.Log), [regex]::Escape($Marker) + '[^\r\n]*')).Count
    Send-Command $srv $Cmd
    if (-not (Wait-ForCount $srv $Marker ($pred + 1) $TimeoutSec)) { return '' }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), [regex]::Escape($Marker) + '[^\r\n]*')
    return $m[$pred].Value
}
function Get-Polje([string]$Vrstica, [string]$Kljuc) {
    $m = [regex]::Match($Vrstica, '\b' + [regex]::Escape($Kljuc) + '=(-?\d+)')
    if ($m.Success) { return [int]$m.Groups[1].Value } else { return -1 }
}
function Stop-Drevo($proc) { try { & taskkill.exe /PID $proc.Id /T /F 2>&1 | Out-Null } catch { } }

$srvLog = Join-Path $audit "$tag-streznik.log"
$kliLog = Join-Path $audit "$tag-klient.log"
$out = if ($JsonPath -ne '') { $JsonPath -replace '\.json$', '-klient.json' } else { Join-Path $audit "$tag-klient.json" }
$srv = $null
$cli = $null
$tujiMed = @{}
$spawnNbt = ''
$vdKonec = ''
$dumpJson = ''
$igralec = ''
$chunkiPred = -1
$chunki24 = -1
$npc24 = -1
$npcVd = -1
$cakanjeChunkov = -1.0
$farVklop = ''
$farKonec = ''
try {
    Step 2 'Zagon streznika'
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain "-PrwWorld={0}" > "{1}" 2>&1' -f $svet, $srvLog)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    $srv = [pscustomobject]@{ Proc = [System.Diagnostics.Process]::Start($psi); Log = $srvLog }
    $gor = Wait-ForCount $srv 'Done (' 1 600
    Check 'V1a: streznik je zagnan (Done)' $gor
    if (-not $gor) { throw 'Streznik se ni zagnal.' }
    # --world mora res izbrati nas svet; sicer bi prizorisce pisalo v dev\run\world (testni svet).
    # level.dat nastane sele ob prvem shranjevanju; dokaz sta vrstica 'Preparing level' in session.lock.
    $nasSvet = ((Get-LogText $srvLog) -match ('Preparing level "' + [regex]::Escape($svet) + '"')) -and
               (Test-Path (Join-Path $svetDir 'session.lock'))
    Check ("V1b: streznik tece v svetu {0} (Preparing level, session.lock), ne v dev\run\world" -f $svet) $nasSvet
    if (-not $nasSvet) { throw 'Streznik ni v svetu iz --world; prizorisca ne postavljam.' }
    # M5.13: prvi ukaz po zagonu se lahko izgubi; vhod se ogreje z 'list'.
    $primed = $false
    for ($i = 0; $i -lt 6 -and -not $primed; $i++) {
        $pred = ([regex]::Matches((Get-MarkerText $srvLog), 'players online')).Count
        Send-Command $srv 'list'
        $primed = Wait-ForCount $srv 'players online' ($pred + 1) 5
    }
    Check 'vhod streznika odgovarja (list)' $primed
    foreach ($l in (Get-Content (Join-Path $seed 'perf-setup-commands.txt'))) {
        $t = $l.Trim(); if ($t -eq '' -or $t.StartsWith('#') -or $t.StartsWith('save-all')) { continue }
        # Spectator mora nalagati chunke (igralec je edini vir nalaganja prizorisca).
        if ($t -match 'spectatorsGenerateChunks') { continue }
        Send-Command $srv $t
    }
    Send-Command $srv 'difficulty 0'
    $vdOdg = Send-Query $srv 'rwdiag vd 24' 'RWDIAG-VD'
    Check ("V1c: razdalja 24 za spawn ({0})" -f $vdOdg) ((Get-Polje $vdOdg 'vd') -eq 24)
    $chunkiPred = Get-Polje $vdOdg 'chunki'
    $trOdg = if ($Domet -ne 64) { Send-Query $srv ("rwdiag track {0}" -f $Domet) 'RWDIAG-TRACK' } else { Send-Query $srv 'rwdiag track' 'RWDIAG-TRACK' }
    $trOk = if ($Domet -ne 64) { ($trOdg -match ('domet={0}\b' -f $Domet)) -and ((Get-Polje $trOdg 'registracij') -gt 0) } else { $trOdg -match 'domet=original' }
    Check ("V1d: domet sledilnika NPC-jev ({0})" -f $trOdg) $trOk

    Step 3 'Zagon klienta in vstop'
    $jobPath = Join-Path $audit "$tag.job"
    $job = @('world=rwv-klient', ("connect=127.0.0.1:{0}" -f $port), "expectNpc=$slMax", "expectNpcMin=$slMin",
             [string]::Format($inv, 'cam={0},{1},{2},{3},{4}', $camX, $camY, $camZ, $yaw, $pitch),
             "renderDistance=$Vd", "fov=$Fov", 'hideGui=0', [string]::Format($inv, 'renderWeight={0}', $renderWeight),
             'settle=5', "warmup=$Warmup", "measure=$Measure", ('out=' + ($out -replace '\\', '/')))
    if ($Far -eq 1) { $job += 'farView=1' }
    if ($Impostor -eq 1) { $job += 'impostorCheck=1' }
    if ($Izris -eq 1) { $job += 'farDrawCheck=1'; $job += 'seamCheck=1' }
    [System.IO.File]::WriteAllLines($jobPath, $job, (New-Object System.Text.UTF8Encoding($false)))
    & (Join-Path $root 'prepare-assets.ps1') | ForEach-Object { Write-Host "    $_" }
    $psc = New-Object System.Diagnostics.ProcessStartInfo
    $psc.FileName = $env:ComSpec
    $psc.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwHarness={0}" > "{1}" 2>&1' -f ($jobPath -replace '\\', '/'), $kliLog)
    $psc.WorkingDirectory = Join-Path $root 'dev'
    $psc.UseShellExecute = $false
    $psc.CreateNoWindow = $true
    $cli = [System.Diagnostics.Process]::Start($psc)
    $meja = (Get-Date).AddSeconds(420)
    while ($igralec -eq '' -and -not $cli.HasExited -and (Get-Date) -lt $meja) {
        $m = [regex]::Match((Get-LogText $kliLog), 'RWHARNESS-IGRALEC (\S+)')
        if ($m.Success) { $igralec = $m.Groups[1].Value } else { Start-Sleep -Seconds 2 }
        if ((Get-LogText $kliLog) -match 'RWHARNESS-NAPAKA') { break }
    }
    Check ("V2: klient je vstopil na streznik ({0})" -f $(if ($igralec) { $igralec } else { 'ni vstopa' })) ($igralec -ne '')
    if ($igralec -eq '') { throw 'Klient ni vstopil.' }
    Send-Command $srv ("gamemode 3 {0}" -f $igralec)
    Send-Command $srv ([string]::Format($inv, 'tp {0} {1} {2} {3} {4} {5}', $igralec, $camX, $camY, $camZ, $yaw, $pitch))

    Step 4 'Chunki in prizorisce'
    # Igralec pri razdalji 24 nalozi 49 x 49 chunkov okoli sebe. Skupno stevilo ni merilo: chunki okoli
    # mesta vstopa (blizu spawna) se po teleportu izpraznijo, zato steje FarProbe okoliIgralcev.
    $cilj = 49 * 49
    $t0 = Get-Date
    $meja = $t0.AddSeconds(300)
    do {
        Start-Sleep -Seconds 3
        $vdOdg = Send-Query $srv 'rwdiag vd' 'RWDIAG-VD'
        $chunki24 = Get-Polje $vdOdg 'okoliIgralcev'
    } while ($chunki24 -lt $cilj -and (Get-Date) -lt $meja)
    $cakanjeChunkov = ((Get-Date) - $t0).TotalSeconds
    Check ("V3: chunki okoli igralca pri 24 nalozeni ({0} >= {1} = 49 x 49, pred vstopom skupaj {2}, {3:N0} s; {4})" -f $chunki24, $cilj, $chunkiPred, $cakanjeChunkov, $vdOdg) ($chunki24 -ge $cilj)
    foreach ($c in $gruce) { Send-Command $srv ('noppes clone grid PERF_Idle 1 5 5 {0},{1},{2}' -f ($c[0] - 2), $tla, ($c[1] - 2)) }
    if ($sonda) { Send-Command $srv ('noppes clone grid PERF_Idle 1 1 1 {0},{1},{2}' -f $sonda[0], $tla, $sonda[1]) }
    $meja = (Get-Date).AddSeconds(180)
    do {
        Start-Sleep -Seconds 2
        $npc24 = Get-Polje (Send-Query $srv 'rwdiag vd' 'RWDIAG-VD') 'npc'
    } while ($npc24 -lt $nVseh -and (Get-Date) -lt $meja)
    Check ("V4: na strezniku je {0} NPC-jev pri razdalji 24 (pricakovano {1})" -f $npc24, $nVseh) ($npc24 -eq $nVseh)
    if ($Vd -ne 24) {
        $vdOdg = Send-Query $srv ("rwdiag vd {0}" -f $Vd) 'RWDIAG-VD'
        Write-Host ("  {0}" -f $vdOdg)
    }
    # Pri manjsi razdalji rwdiag vd chunke onkraj izprazni takoj (D-042: vanilla vrsta jih ob robu nakljucno
    # obdrzi); entitete izginejo iz seznama v naslednjem ticku.
    $meja = (Get-Date).AddSeconds(120)
    do {
        Start-Sleep -Seconds 2
        $vdOdg = Send-Query $srv 'rwdiag vd' 'RWDIAG-VD'
        $npcVd = Get-Polje $vdOdg 'npc'
    } while ($npcVd -ne $nalVd -and (Get-Date) -lt $meja)
    Check ("V5: nalozenih NPC-jev pri razdalji {0}: {1} (pricakovano iz chunkov {2}); {3}" -f $Vd, $npcVd, $nalVd, $vdOdg) (($npcVd -eq $nalVd) -and ((Get-Polje $vdOdg 'vd') -eq $Vd) -and ((Get-Polje $vdOdg 'sledilnikMeja') -eq (($Vd - 1) * 16)))
    $spawnNbt = Send-Query $srv 'rwdiag spawnnbt' 'RWDIAG-SPAWNNBT'
    Check ("V6: velikost spawn NBT nalozenih NPC-jev ({0})" -f $spawnNbt) ((Get-Polje $spawnNbt 'n') -eq $nalVd)
    if ($Far -eq 1) {
        [void](Send-Query $srv 'rwfar domet 384' 'RWFAR nacin=')
        $farVklop = Send-Query $srv 'rwfar 1' 'RWFAR nacin='
        Check ("V16a: rwfar vklopljen, klient je poslal HELLO ({0})" -f $farVklop) (((Get-Polje $farVklop 'nacin') -eq 1) -and ((Get-Polje $farVklop 'domet') -eq 384) -and ((Get-Polje $farVklop 'igralcev') -eq 1))
    }
    Send-Command $srv 'say RWHARNESS-SCENA'

    Step 5 'Merjenje'
    $meja = (Get-Date).AddSeconds(600 + $Warmup + $Measure)
    $rwOn = $false
    $rwDump = $false
    $izhod = $false
    while (-not $cli.HasExited) {
        if ((Get-Date) -gt $meja) { Write-Host '  ! klient se ni koncal pravocasno; ubijam drevo procesov'; Stop-Drevo $cli; break }
        $kt = Get-LogText $kliLog
        if ("$kt" -match 'RWHARNESS-FAZA (WARMUP|MEASURE)') {
            $nasi = Get-DrevoProcesov @($cli.Id, $srv.Proc.Id)
            foreach ($t in @(Get-TujiJava $nasi)) { $tujiMed[$t] = $true }
        }
        if (-not $rwOn -and "$kt" -match 'RWHARNESS-FAZA MEASURE') {
            Send-Command $srv 'rwdiag on'
            if ($Far -eq 1) { Send-Command $srv 'rwfar reset' }
            $rwOn = $true
        }
        # M5-V.5: s -Izris 1 sledi merjenju faza SEAM (kamera hodi); streznik se prebere ob njenem zacetku.
        if ($rwOn -and -not $rwDump -and "$kt" -match 'RWHARNESS-FAZA (SEAM|DONE)') {
            $dumpOdg = Send-Query $srv ("rwdiag dump {0}" -f $tag) 'RWDIAG-DUMP' 30
            Send-Command $srv 'rwdiag off'
            $rwDump = $true
            $m = [regex]::Match($dumpOdg, 'RWDIAG-DUMP (\S+\.txt)')
            if ($m.Success) {
                $rel = $m.Groups[1].Value.Trim()
                $p = if ([System.IO.Path]::IsPathRooted($rel)) { $rel } else { Join-Path $run $rel }
                $dumpJson = $p -replace '\.txt$', '.json'
            }
            $vdKonec = Send-Query $srv 'rwdiag vd' 'RWDIAG-VD'
            if ($Far -eq 1) { $farKonec = Send-Query $srv 'rwfar' 'RWFAR nacin=' }
        }
        if ($rwDump -and -not $izhod -and "$kt" -match 'RWHARNESS-FAZA DONE') {
            # M5-V.2: klient po DONE caka povezan (stevci na igralca so brani, ko je se prijavljen).
            Send-Command $srv 'say RWHARNESS-IZHOD'
            $izhod = $true
        }
        if ("$kt" -match 'RWHARNESS-NAPAKA') {
            # Harness po napaki zapise JSON in se ugasne sam; pocakamo ga.
            Start-Sleep -Seconds 2
        }
        Start-Sleep -Seconds 1
    }
    if (-not $cli.HasExited) { [void]$cli.WaitForExit(60000) }
} finally {
    if ($cli -and -not $cli.HasExited) { Stop-Drevo $cli }
    if ($srv -and -not $srv.Proc.HasExited) {
        try { Send-Command $srv 'stop' } catch { }
        if (-not $srv.Proc.WaitForExit(180000)) { Write-Host '  ! streznik se ni ustavil sam; ubijam drevo procesov'; Stop-Drevo $srv.Proc }
    }
}

Step 6 'Merila'
$kt = Get-LogText $kliLog
foreach ($m in [regex]::Matches("$kt", 'RWHARNESS-[A-Z]+[^\r\n]*')) { Write-Host "  $($m.Value)" }
$r = $null
if (Test-Path $out) { $r = Get-Content $out -Raw | ConvertFrom-Json }
Check ("V7: klient je zapisal rezultat in prisel do konca meritve ({0})" -f $(if ($r) { "faza $($r.faza), napak $(@($r.napake).Count) $(@($r.napake) -join '; ')" } else { 'ni zapisa' })) (($null -ne $r) -and ($r.faza -eq 'MEASURE') -and (@($r.napake).Count -eq 0))
$d = $null
if ($dumpJson -ne '' -and (Test-Path $dumpJson)) { $d = Get-Content $dumpJson -Raw | ConvertFrom-Json }
$tick = $null
if ($d) { foreach ($x in $d.distributions) { if ($x.name -eq 'server.tick.ns') { $tick = $x } } }
if ($r) {
    Check ("V8: v klientu je {0} NPC-jev (sledenih po geometriji {1}..{2}, sledilnik {3})" -f $r.npcVKlientu, $slMin, $slMax, $Rsl) (($r.npcVKlientu -ge $slMin) -and ($r.npcVKlientu -le $slMax))
    # Merilo veljavnosti (plan M5-V.1): vidnih NPC-jev je res toliko, kot jih klient ima.
    Check ("V9: upodobljenih entitet min {0} / max {1} v {2} vzorcih (prag {3} = NPC-ji v klientu)" -f $r.upodobljenoMin, $r.upodobljenoMax, $r.upodobljenoVzorcev, $r.npcVKlientu) (($r.upodobljenoVzorcev -ge [int]($Measure * 0.8)) -and ($r.upodobljenoMin -ge $r.npcVKlientu))
    Check ("V10: kamera fiksna (odmik {0} blokov, {1} stopinj)" -f $r.kameraOdmikPoz, $r.kameraOdmikKot) (($r.kameraOdmikPoz -le 0.01) -and ($r.kameraOdmikKot -le 0.01))
    Check ("V11: nastavitve: vsync {0}, limit {1}, RD {2}, FOV {3}, renderDistanceWeight {4} (pricakovano {5}), ponastavitev {6} / v merjenju {7}" -f $r.vsync, $r.limitFramerate, $r.renderDistance, $r.fov, $r.renderWeight, $pricWeight, $r.renderWeightPonastavitev, $r.renderWeightPonastavitevMeritev) (($r.renderWeightPonastavitevMeritev -eq 0) -and ($r.vsync -eq 0) -and ($r.limitFramerate -ge 260) -and ($r.renderDistance -eq $Vd) -and ([math]::Abs($r.fov - $Fov) -lt 0.01) -and ([math]::Abs($r.renderWeight - $pricWeight) -lt 0.001))
    # D-041: FPS je izmerjena velikost (varianta A pri 5000 je lahko pod 10/s), ne merilo. Veljavnost je dovolj
    # vzorcev za p99 / 1 % low (>= 300 slicic = >= 3 nad p99) in klientski tick, ki ni zastal (>= 15/s).
    if ($r.slicic -lt 10 * $Measure) { Write-Host ("  opomba: {0:N1} slicic/s je pod 10/s; to je izmerjena cena celice, ne napaka harnessa" -f ($r.slicic / $Measure)) }
    Check ("V12: {0} slicic in {1} klientskih tickov v {2:N1} s (najmanj 300 slicic, 15 tickov/s)" -f $r.slicic, $r.klientTickov, $r.sekundMerjenja) (($r.slicic -ge 300) -and ($r.klientTickov -ge 15 * $Measure) -and ([math]::Abs($r.sekundMerjenja - $Measure) -le 2))
    Check ("V13: bajti s streznika izmerjeni ({0} B v merjenju, {1:N0} B/s; pred merjenjem {2} B)" -f $r.bajtovMeritev, $r.bajtovNaS, $r.bajtovPredMeritvijo) (($r.bajtovMeritev -ge 0) -and ($r.bajtovPredMeritvijo -gt 0))
}
Check ("V14: streznik je zapisal posnetek merilnega okna ({0}, tickov {1})" -f $dumpJson, $(if ($tick) { $tick.count } else { 0 })) (($null -ne $tick) -and ($tick.count -ge 15 * ($Measure - 4)))
Check ("V15: med ogrevanjem in merjenjem ni tekel tuj gradle build ali Minecraft" + $(if ($tujiMed.Count) { ' - ' + (@($tujiMed.Keys) -join '; ') } else { '' })) ($tujiMed.Count -eq 0 -or $DovoliTuje)
$farUs = -1.0
$farBs = -1.0
if ($Far -eq 1) {
    $mu = [regex]::Match($farKonec, 'usNaTickNaIgralca=([0-9.]+)')
    if ($mu.Success) { $farUs = [double]::Parse($mu.Groups[1].Value, $inv) }
    $mp = [regex]::Match($farKonec, 'poIgralcih=[^:;\s]+:(\d+):(\d+)')
    $farPas = if ($mp.Success) { [int]$mp.Groups[1].Value } else { -1 }
    $farZnanih = if ($mp.Success) { [int]$mp.Groups[2].Value } else { -1 }
    Check ("V16: register NPC-jev rwfar = loadedEntityList ({0} / {1}); {2}" -f (Get-Polje $farKonec 'registra'), (Get-Polje $farKonec 'npcVSvetu'), $farKonec) (($farKonec -ne '') -and ((Get-Polje $farKonec 'registra') -eq (Get-Polje $farKonec 'npcVSvetu')) -and ((Get-Polje $farKonec 'npcVSvetu') -eq $npcVd))
    if ($r) {
        $farBs = if ($r.sekundMerjenja -gt 0) { $r.farBajtovMeritev / $r.sekundMerjenja } else { -1.0 }
        Check ("V17: nadomestkov v klientu ob zacetku / koncu merjenja {0} / {1}, streznik pas {2} / znanih {3} (pricakovano {4}..{5}); HELLO {6}" -f $r.farVKlientuZacetek, $r.farVKlientu, $farPas, $farZnanih, $pasMin, $pasMax, $r.farHello) (($r.farVKlientuZacetek -ge $pasMin) -and ($r.farVKlientuZacetek -le $pasMax) -and ($r.farVKlientu -ge $pasMin) -and ($r.farVKlientu -le $pasMax) -and ($farPas -eq $r.farVKlientu) -and ($farZnanih -eq $r.farVKlientu) -and ($r.farHello -ge 1))
    }
    Check ("V18: cas kode rwfar {0} us na tick na igralca (cilj <= 100 = 0,1 ms, D-043; od M5-V.3 z opisi videza); rwfar {1:N1} B/s v merjenju" -f $farUs, $farBs) (($farUs -ge 0) -and ($farUs -le 100.0))
    # M5-V.3: kloni istega NPC-ja (noppes clone grid PERF_Idle) imajo en videz; vsak nadomestek ima znan opis.
    $vVidezov = Get-Polje $farKonec 'videzov'
    $vIzracunov = Get-Polje $farKonec 'izracunovVse'
    $vZavrnjenih = Get-Polje $farKonec 'zavrnjenih'
    $vNapak = Get-Polje $farKonec 'napakVidez'
    $vPrevelikih = Get-Polje $farKonec 'prevelikih'
    $vCaka = Get-Polje $farKonec 'cakaVidez'
    $mNs = [regex]::Match($farKonec, '\bnsVidez=(\d+)'); $vNs = if ($mNs.Success) { [long]$mNs.Groups[1].Value } else { -1 }
    if ($r) {
        Check ("V19: videz - streznik videzov v rabi {0} (kloni -> 1), izracunov {1} (>= pas {2}), caka {3}, zavrnjenih {4}, napak {5}, prevelikih {6}, ns opisov v merjenju {7}; klient opisov {8}, zahtevanih id {9}, odgovorov {10}, napak {11}, nadomestkov brez opisa {12}" -f $vVidezov, $vIzracunov, $farPas, $vCaka, $vZavrnjenih, $vNapak, $vPrevelikih, $vNs, $r.farVidezov, $r.farVidezZahtev, $r.farVidezOdgovorov, $r.farVidezNapak, $r.farBrezVideza) (($vVidezov -eq 1) -and ($vIzracunov -ge $farPas) -and ($farPas -gt 0) -and ($vZavrnjenih -eq 0) -and ($vNapak -eq 0) -and ($vPrevelikih -eq 0) -and ($r.farVidezov -ge 1) -and ($r.farVidezOdgovorov -ge 1) -and ($r.farVidezNapak -eq 0) -and ($r.farBrezVideza -eq 0))
    }
}
if ($Impostor -eq 1 -and $r) {
    # M5-V.4: peka (vsi videzi nadomestkov speceni, brez napak) in primerjava pikslov impostor - model pri 56 blokih.
    Check ('V20: peka impostorjev - pecenih {0}, v atlasu {1}, nadomestkov brez peke {2}, napak {3}, odlozenih {4}, obrezanih {5}, brez FBO {6}; peka povp. {7:N2} ms, najvec {8:N2} ms' -f $r.impPeceno, $r.impVAtlasu, $r.impBrezPeke, $r.impNapak, $r.impOdlozenih, $r.impObrezanih, $r.impBrezFbo, $r.impPekaMsPovp, $r.impPekaMsMax) (($r.impPeceno -ge 1) -and ($r.impBrezPeke -eq 0) -and ($r.impNapak -eq 0) -and ($r.impBrezFbo -eq 0) -and ($r.farVKlientu -gt 0))
    Check ('V21: piksli impostor - model pri 56 blokih (8 videzov x 8 kotov, IoU >= 0,70, razlika <= 32, obcutljivost > 64): preverba {0}, videzov {1}, obcutljivost {2:N1}; {3} {4}' -f $r.impPreverba, $r.impVidezov, $r.impObcutljivost, $r.impSerije, $r.impNapaka) ($r.impPreverba -eq 1)
}
if ($Izris -eq 1 -and $r) {
    # M5-V.5: izris L3 v merilnem oknu, vidnost sonde na robu pasu, siv ob prehodu cez mejo sledilnika.
    Check ('V22: izris L3 - poseg coremoda {0}, slicic z izrisom {1} od {2}, rezerva {3}; narisanih min/max {4}/{5} (pricakovano {6}..{7}), zivih (siv) max {8} ({9}..{10}), izven frustuma {11}, brez peke {12}; najdlje {13:N1} blokov; izris povp. {14:N1} us, najvec {15:N1} us' -f $r.farPoseg, $r.farIzrisSlicic, $r.slicic, $r.farIzrisRezerva, $r.farIzrisNarisanihMin, $r.farIzrisNarisanihMax, $izrMin, $izrMax, $r.farIzrisZivihMax, $sivMin, $sivMax, $r.farIzrisIzvenFrustumaMax, $r.farIzrisBrezPekeMax, $r.farIzrisNajdlje, $r.farIzrisUsPovp, $r.farIzrisUsMax) (($r.farPoseg -eq 1) -and ($r.farIzrisRezerva -eq 0) -and ($r.farIzrisSlicic -ge $r.slicic - 2) -and ($r.farIzrisNarisanihMin -eq $r.farIzrisNarisanihMax) -and ($r.farIzrisNarisanihMin -ge $izrMin) -and ($r.farIzrisNarisanihMax -le $izrMax) -and ($r.farIzrisZivihMax -ge $sivMin) -and ($r.farIzrisZivihMax -le $sivMax) -and ($r.farIzrisNarisanihMin + $r.farIzrisZivihMax -eq $r.farVKlientu) -and ($r.farIzrisIzvenFrustumaMax -eq 0) -and ($r.farIzrisBrezPekeMax -eq 0))
    Check ('V23: sonda vidna v pravi slicici - preverba {0}, razdalja {1:N1} (rob pasu {2}), spremenjenih pikslov {3}, najvecja razlika {4}, kontrola {5}, poskusov {6}; {7}' -f $r.dalecPreverba, $r.dalecRazdalja, $farMeja, $r.dalecSpremenjenih, $r.dalecNajvecRazlika, $r.dalecKontrola, $r.dalecPoskusov, $r.dalecOpis) (($r.dalecPreverba -eq 1) -and ($r.dalecRazdalja -ge $farMeja - 0.5))
    Check ('V24: prehod cez mejo sledilnika - preverba {0}; {1}' -f $r.sivPreverba, $r.sivOpis) ($r.sivPreverba -eq 1)
}
if ($vdKonec -ne '') { Write-Host ("  ob koncu: {0}" -f $vdKonec) }

if ($r) {
    $msP = { param($ns) [math]::Round($ns / 1000000.0, 3) }
    Write-Host ''
    Write-Host ("  vd {0} domet {1} N {2}: slicica p50 {3:N2} / p95 {4:N2} / p99 {5:N2} ms, FPS {6:N1}, 1 % low {7:N1}; klientski tick p50 {8:N2} / p95 {9:N2} ms; {10:N0} B/s; MSPT p50 {11} / p95 {12} / p99 {13} ms; spawn NBT {14}" -f `
        $Vd, $Domet, $N, $r.frameMsP50, $r.frameMsP95, $r.frameMsP99, $r.fps, $r.fpsLow1, $r.klientTickMsP50, $r.klientTickMsP95, $r.bajtovNaS,
        $(if ($tick) { & $msP $tick.p50 } else { '-' }), $(if ($tick) { & $msP $tick.p95 } else { '-' }), $(if ($tick) { & $msP $tick.p99 } else { '-' }), $spawnNbt)
    if ($JsonPath -ne '') {
        $odtis = @{ vd = $Vd; domet = $Domet; npc = $N; gruc = $gruc; nalozenih = $nalVd; sledenihMin = $slMin; sledenihMax = $slMax;
                    ogrevanje = $Warmup; sekund = $Measure; fov = $Fov; renderWeight = $pricWeight;
                    kamera = [string]::Format($inv, '{0},{1},{2},{3},{4}', $camX, $camY, $camZ, $yaw, $pitch); sirina = $r.sirina; visina = $r.visina }
        $vel = @{ 'frame.p50' = $r.frameMsP50; 'frame.p95' = $r.frameMsP95; 'frame.p99' = $r.frameMsP99; 'frame.max' = $r.frameMsMax;
                  'fps' = $r.fps; 'fps.low1' = $r.fpsLow1; 'klient.tick.p50' = $r.klientTickMsP50; 'klient.tick.p95' = $r.klientTickMsP95;
                  'bajti.s' = $r.bajtovNaS; 'bajti.pred' = $r.bajtovPredMeritvijo; 'npc.klient' = $r.npcVKlientu; 'upodobljeno.min' = $r.upodobljenoMin;
                  'gc.ms' = $r.gcMs; 'spawnnbt.p50' = (Get-Polje $spawnNbt 'p50'); 'spawnnbt.max' = (Get-Polje $spawnNbt 'max'); 'npc.streznik' = $npcVd }
        if ($Far -eq 1) { $odtis['far'] = 1; $odtis['pasMin'] = $pasMin; $odtis['pasMax'] = $pasMax }
        if ($Far -eq 1) { $vel['far.klient'] = $r.farVKlientu; $vel['far.us.tick'] = $farUs; $vel['far.bajti.s'] = $farBs; $vel['far.bajtov'] = $r.farBajtov
                          $vel['far.videzov'] = $vVidezov; $vel['far.izracunov'] = $vIzracunov; $vel['far.klient.opisov'] = $r.farVidezov; $vel['far.klient.zahtev'] = $r.farVidezZahtev }
        if ($Izris -eq 1) { $odtis['izris'] = 1; $odtis['sonda'] = $farMeja; $vel['izris.narisanih'] = $r.farIzrisNarisanihMax; $vel['izris.us.povp'] = $r.farIzrisUsPovp; $vel['izris.us.max'] = $r.farIzrisUsMax
                            $vel['dalec.spremenjenih'] = $r.dalecSpremenjenih; $vel['dalec.razlika'] = $r.dalecNajvecRazlika; $vel['siv.slicic'] = $r.sivSlicic; $vel['siv.prehodov'] = $r.sivZivDoImpostorja + $r.sivImpostorDoZivega }
        if ($Impostor -eq 1) { $odtis['impostor'] = 1; $vel['imp.peceno'] = $r.impPeceno; $vel['imp.atlas'] = $r.impVAtlasu; $vel['imp.peka.ms.povp'] = $r.impPekaMsPovp; $vel['imp.peka.ms.max'] = $r.impPekaMsMax; $vel['imp.obcutljivost'] = $r.impObcutljivost }
        if ($tick) { $vel['mspt.p50'] = & $msP $tick.p50; $vel['mspt.p95'] = & $msP $tick.p95; $vel['mspt.p99'] = & $msP $tick.p99; $vel['mspt.povp'] = & $msP ($tick.sum / [math]::Max(1, $tick.count)) }
        $null = Write-MeritevJson -Path $JsonPath -Paket $(if ($Izris -eq 1) { 'M5-V.5' } elseif ($Impostor -eq 1) { 'M5-V.4' } elseif ($Far -eq 1) { 'M5-V.3' } else { 'M5-V.1' }) -Scenarij ("dalec vd{0}-d{1}-{2}{3}" -f $Vd, $Domet, $N, $(if ($Izris -eq 1) { '-izris' } elseif ($Impostor -eq 1) { '-imp' } elseif ($Far -eq 1) { '-far' } else { '' })) -Odtis $odtis -Velicine $vel -Uspeh ($failures.Count -eq 0) -Padle $failures
    }
}
$zaklep.Close()
Write-Host ''
if ($failures.Count -gt 0) { Write-Host ("M5-V.1 PADLO: {0} meril" -f $failures.Count); exit 1 }
Write-Host $(if ($Izris -eq 1) { 'M5-V.5 V0-V24 USPESNO.' } elseif ($Impostor -eq 1) { 'M5-V.4 V0-V21 USPESNO.' } elseif ($Far -eq 1) { 'M5-V.3 V0-V19 USPESNO.' } else { 'M5-V.1 V0-V15 USPESNO.' })
exit 0
