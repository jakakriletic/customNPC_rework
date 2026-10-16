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

    Merila (K1-K8) so na koncu; izhodna koda 0 samo, ce so vsa zelena.
#>
param(
    [ValidateSet('idle', 'boj', 'skripte', 'nedosegljiva')][string]$Variant = 'idle',
    [int]$N = 200,
    [int]$Warmup = 30,
    [int]$Measure = 60,
    [int]$RenderDistance = 8,
    [int]$Fov = 70,
    [string]$JsonPath = '',
    [switch]$DovoliTuje
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

# Kamera: juzno od mreze, nad njo, pogled proti severu (yaw 180) na sredino. NPC-ji morajo biti
# znotraj dometa sledilnika (64 blokov, tracker(64, 3, true)), sicer jih streznik klientu ne poslje.
$globina = if ($Variant -eq 'boj' -or $Variant -eq 'nedosegljiva') { [int](($N / 2) / $gridW) * 2 + $bojGap } else { [int]($N / $gridW) }
$zSredina = $gridZ + $globina / 2.0
$camZ = $gridZ + $globina + 6 + [math]::Round($globina * 0.5)
$camY = 4 + 8 + [math]::Round($globina * 0.6)
$camX = $gridX + ($gridW - 1) / 2.0 + 0.5
$pitch = [math]::Round([math]::Atan2($camY - 4, $camZ - $zSredina) * 180 / [math]::PI, 1)
$daljnji = [math]::Sqrt([math]::Pow($camZ - $gridZ, 2) + [math]::Pow($gridW / 2.0, 2) + [math]::Pow($camY - 4, 2))
Write-Host ("  kamera ({0}, {1}, {2}) yaw 180 pitch {3}; najdaljsi NPC {4:N1} blokov" -f $camX, $camY, $camZ, $pitch, $daljnji)
Check ("najbolj oddaljen NPC je znotraj dometa sledilnika ({0:N1} <= 60)" -f $daljnji) ($daljnji -le 60)

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
$cmds += 'rwdiag chunks on 1'

$out = if ($JsonPath -ne '') { $JsonPath -replace '\.json$', '-klient.json' } else { Join-Path $audit "k-$Variant-$N-$stamp-klient.json" }
$jobPath = Join-Path $audit "k-$Variant-$N-$stamp.job"
$job = @("world=$world", 'createSeed=customnpc-rework', "expectNpc=$pricakovano", "expectNpcMin=$pricakovanoMin",
         # Invariantna kultura: v sl-SI bi -f dal decimalne vejice in HarnessJob kamere ne bi prebral.
         [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, 'cam={0},{1},{2},180,{3}', $camX, $camY, $camZ, $pitch),
         "renderDistance=$RenderDistance", "fov=$Fov", ('settle={0}' -f (3 + [int]($N / 250))),
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
    Start-Sleep -Seconds 5
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
    Check ("K6: nastavitve: vsync {0}, limit {1}, RD {2}, FOV {3}" -f $r.vsync, $r.limitFramerate, $r.renderDistance, $r.fov) (($r.vsync -eq 0) -and ($r.limitFramerate -ge 260) -and ($r.renderDistance -eq $RenderDistance) -and ([math]::Abs($r.fov - $Fov) -lt 0.01))
    Check ("K7: {0} slicic v {1:N1} s" -f $r.slicic, $r.sekundMerjenja) (($r.slicic -ge 10 * $Measure) -and ([math]::Abs($r.sekundMerjenja - $Measure) -le 2))
}
Check ("K8: med ogrevanjem in merjenjem ni tekel tuj gradle build ali Minecraft" + $(if ($tujiMed.Count) { ' - ' + (@($tujiMed.Keys) -join '; ') } else { '' })) ($tujiMed.Count -eq 0 -or $DovoliTuje)

if ($r) {
    Write-Host ''
    Write-Host ("  {0}-{1}: slicica p50 {2:N2} / p95 {3:N2} / p99 {4:N2} / max {5:N1} ms, FPS {6:N1}, 1 % low {7:N1}, 0,1 % low {8:N1}, GC {9} ({10} ms), integrirani streznik {11:N2} ms/tick" -f `
        $Variant, $N, $r.frameMsP50, $r.frameMsP95, $r.frameMsP99, $r.frameMsMax, $r.fps, $r.fpsLow1, $r.fpsLow01, $r.gcStevilo, $r.gcMs, $r.integriraniStreznikMsPovp)
    if ($JsonPath -ne '') {
        $odtis = @{ varianta = $Variant; npc = $N; ogrevanje = $Warmup; sekund = $Measure; rd = $RenderDistance; fov = $Fov;
                    kamera = [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, '{0},{1},{2},180,{3}', $camX, $camY, $camZ, $pitch); sirina = $r.sirina; visina = $r.visina }
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
