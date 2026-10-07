# M3.9 - preverba solid x jahanje (R6 x R1): nosilec z jahacem in NPC na poti drugega NPC-ja.
# Zasnova: docs/01-ARHITEKTURA.md D-025, paket: docs/03-FAZE.md M3.9.
#
# Faza J (probe, kot hb-run.ps1): '/rwhitbox probe A B 0.3' en trk po pravi kodi, z nosilcem,
#   na katerem sedi jahac ('/rwhitbox mount'). Vanilla (Entity.applyEntityCollision) nosilcu
#   s potnikom potiska ne doda (isBeingRidden) - RwHitbox to ohrani.
# Faza P (pot): hodeci se spawna na cilju (x 16,5), tp ga postavi na start (x 2,5), CNPC-jev
#   EntityAIReturn ga po lastni poti vrne na cilj - 14 blokov naravnost skozi NPC-ja na x 8,5.
#   Proge 1-3 original, 4-6 solid: NPC na poti tava (+-1 blok, zato je stik nakljucen).
#   Progi 7, 8 (original, solid): NPC na poti stoji (MovingState 0) - stik od spredaj zagotovljen,
#   najslabsi primer za zatik; potisk zavrne ze original (addVelocity samo pri isWalking).
#   '/rwhitbox track' 800 tickov sledi vsak tick: skok > 1 blok v ticku
#   pomeni, da je EntityAIReturn obupal in NPC-ja teleportiral (zataknjen); stik = hitboxa se prekrivata (trk tece). P3 primerja premik samo ob stiku.
# Svet: prvi zagon pribije world spawn na 0 4 0 (kot testworld-run.ps1), sicer chunki prog niso
#   nalozeni in 'noppes clone spawn' tiho ne naredi nic (7. 10.: proga 1 in 2 sta manjkali).
#
# Merila:
#   J1  global 1, nosilec original z jahacem proti HB_B original: nosilec 0, HB_B v0 (vanilla)
#   J2  nosilec solid z jahacem: nosilec 0, HB_B v0
#   J3  jahac proti lastnemu nosilcu: 0 / 0 (isRidingSameEntity)
#   J4  nosilec original z jahacem proti solid HB_A: napoved kode 0 / 0 - nihce se ne odrine
#   J5  nosilec smart z jahacem proti smart HB_Velik: napoved 0 / 2/28 v0
#   J6  po sestopu: nosilec solid proti HB_B: 0 / v0 (navaden solid), jahac brez nosilca
#   P1  vseh osem hodecih na cilju (< 1,5 bloka; EntityAIReturn se ustavi v dosegu 1) po 40 s,
#       brez teleporta (skok <= 1 na tick)
#   P2  stik je bil: na progah 7, 8 in vsaj na eni tavajoci progi vsakega nacina
#   P3  tavajoci solid NPC se ob stiku premakne manj na tick stika kot tavajoci original;
#       stojeca (7, 8) ob stiku 0
#   P4  brez ERROR iz noppes.* in brez sesutja
#
# Zagon (iz seje Claude Code najprej: Remove-Item Env:NoDefaultCurrentDirectoryInExePath):
#     node dev\testworld\hb-fixture.js   (enkrat; fixture so v repozitoriju)
#     .\testworld.ps1
#     .\m39-run.ps1

param([switch]$AcceptEula)
$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
New-Item -ItemType Directory -Force -Path $audit | Out-Null
. (Join-Path $root 'meritve-lib.ps1')

$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) } else { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}
function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }
function Get-LogText([string]$p) { if (-not (Test-Path $p)) { return '' }; $c = Get-Content $p -Raw -ErrorAction SilentlyContinue; if ($null -eq $c) { '' } else { $c } }
function Get-MarkerText([string]$p) { ((Get-LogText $p) -split "`n" | Where-Object { $_ -notmatch '\[FINE/CustomNPCs\]' -and $_ -notmatch 'CustomNPCs\]\[noppes' }) -join "`n" }

function Start-DevServer([string]$Tag) {
    $outLog = Join-Path $audit ("m39-{0}.log" -f $Tag)
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = "/c gradlew.bat runServer --offline --no-daemon --console=plain > `"$outLog`" 2>&1"
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    return [pscustomobject]@{ Proc = [System.Diagnostics.Process]::Start($psi); Log = $outLog }
}
function Send-Command($srv, [string]$Cmd) { $srv.Proc.StandardInput.WriteLine($Cmd); $srv.Proc.StandardInput.Flush(); Start-Sleep -Milliseconds 300 }
function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        if (([regex]::Matches((Get-MarkerText $Srv.Log), [regex]::Escape($Marker))).Count -ge $Count) { return $true }
        $s = Get-ServerSesutje (Get-LogText $Srv.Log)
        if ($s -ne '') { throw ("Server se je sesul (cakal na '{0}'): {1}" -f $Marker, $s) }
        if ($Srv.Proc.HasExited) { Write-Host ("  ! proces se je koncal pred '{0}'" -f $Marker); return $false }
        if ((Get-Date) -ge $deadline) { Write-Host ("  ! timeout pri '{0}' (#{1})" -f $Marker, $Count); return $false }
        Start-Sleep -Milliseconds 200
    }
}
function Stop-DevServer($srv) {
    if ($srv.Proc.HasExited) { return $true }
    Send-Command $srv 'stop'
    if (-not $srv.Proc.WaitForExit(180000)) {
        try { & taskkill.exe /PID $srv.Proc.Id /T /F 2>&1 | Out-Null } catch { }
        return $false
    }
    return $true
}

$script:nProbe = 0
function Probe($srv, [string]$A, [string]$B) {
    Send-Command $srv ("rwhitbox probe {0} {1} 0.3" -f $A, $B)
    $script:nProbe++
    if (-not (Wait-ForCount $srv 'RWHITBOX-PROBE ' $script:nProbe 30)) { return $null }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), 'RWHITBOX-PROBE a=(\S+) b=(\S+) nacinA=(\w+) nacinB=(\w+) .*? vA=([\d.]+) vB=([\d.]+)')
    $g = $m[$m.Count - 1].Groups
    $r = [pscustomobject]@{ A = $g[1].Value; B = $g[2].Value; NacinA = $g[3].Value; NacinB = $g[4].Value; VA = [double]$g[5].Value; VB = [double]$g[6].Value }
    Write-Host ("  probe {0}({1}) / {2}({3}): vA={4:N6} vB={5:N6}" -f $r.A, $r.NacinA, $r.B, $r.NacinB, $r.VA, $r.VB)
    return $r
}
$script:nMove = @{}
function Move-Npc($srv, [string]$Cmd) {
    $kind = ($Cmd -split ' ')[0].ToUpperInvariant()
    Send-Command $srv ("rwhitbox {0}" -f $Cmd)
    $script:nMove[$kind] = 1 + $(if ($script:nMove.ContainsKey($kind)) { $script:nMove[$kind] } else { 0 })
    if (-not (Wait-ForCount $srv ("RWHITBOX-{0} " -f $kind) $script:nMove[$kind] 30)) { return $false }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), ('RWHITBOX-{0} ime=(\S+) ok=(\w+)' -f $kind))
    return ($m[$m.Count - 1].Groups[2].Value -eq 'true')
}
$script:nWhere = 0
function Get-Polozaji($srv, [int]$Stevilo) {
    Send-Command $srv 'rwhitbox where M39_'
    $script:nWhere += $Stevilo
    if (-not (Wait-ForCount $srv 'RWHITBOX-WHERE ' $script:nWhere 30)) { return $null }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), 'RWHITBOX-WHERE ime=(\S+) x=([-\d.]+) z=([-\d.]+)')
    $p = @{}
    for ($i = $m.Count - $Stevilo; $i -lt $m.Count; $i++) {
        $p[$m[$i].Groups[1].Value] = [pscustomobject]@{ X = [double]$m[$i].Groups[2].Value; Z = [double]$m[$i].Groups[3].Value }
    }
    return $p
}
function Razdalja($p, $q) { if ($null -eq $p -or $null -eq $q) { return [double]::NaN }; [math]::Sqrt(($p.X - $q.X) * ($p.X - $q.X) + ($p.Z - $q.Z) * ($p.Z - $q.Z)) }
function Blizu([double]$x, [double]$want, [double]$rel) { if ($want -eq 0) { return [math]::Abs($x) -lt 1e-9 }; return [math]::Abs($x - $want) -le [math]::Abs($want) * $rel }

$proge = @(1..8 | ForEach-Object {
    [pscustomobject]@{ N = $_; Z = 34 + 6 * $_; Nacin = $(if ($_ -le 3 -or $_ -eq 7) { 'original' } else { 'solid' }); Stoji = ($_ -ge 7) } })
$vseM39 = 18

$srv = $null
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Step 1 'Priprava'
    $zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm39-run'
    $tuji = @(Get-TujiJava)
    Check ("pred zagonom ne tece tuj gradle build ali Minecraft" + $(if ($tuji.Count) { ' - ' + ($tuji -join '; ') } else { '' })) ($tuji.Count -eq 0)
    $eula = Join-Path $run 'eula.txt'
    if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni z -AcceptEula." }
        Set-Content -Path $eula -Value 'eula=true' -Encoding ASCII
    }
    $scenMark = Join-Path $run 'world\rework-scenarij.txt'
    if (Test-Path $scenMark) { throw ("Svet je ze uporabil scenarij '{0}'. Pozeni najprej .\testworld.ps1." -f (Get-Content $scenMark -Raw).Trim()) }
    $clones = Join-Path $run 'world\customnpcs\clones\1'
    Check 'fixture HB_* in M39_* so v svetu' (@(Get-ChildItem $clones -Filter 'M39_*.json' -ErrorAction SilentlyContinue).Count -eq $vseM39 -and (Test-Path (Join-Path $clones 'HB_Velik.json')))
    if ($failures.Count) { throw 'Priprava ni uspela (fixture: node dev\testworld\hb-fixture.js, nato .\testworld.ps1).' }

    Step 2 'Zagon 0: pribij world spawn na 0 4 0'
    $srv = Start-DevServer 'spawn'
    Check 'server 0 je dosegel "Done ("' (Wait-ForCount $srv 'Done (' 1 900)
    if ($failures.Count) { throw "Server se ni zagnal. Glej $($srv.Log)" }
    Set-Content -Path $scenMark -Value ("m39-run {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm')) -Encoding ASCII
    Send-Command $srv 'setworldspawn 0 4 0'
    Check 'world spawn nastavljen' (Wait-ForCount $srv 'Set the world spawn point' 1 60)
    Send-Command $srv 'save-all flush'
    $null = Wait-ForCount $srv 'Saved the world' 1 120
    Check 'server 0 se je cisto ustavil' (Stop-DevServer $srv)
    $log0 = Get-LogText $srv.Log

    Step 3 'Zagon 1: postavitev'
    $srv = Start-DevServer 'run'
    Check 'server je dosegel "Done ("' (Wait-ForCount $srv 'Done (' 1 900)
    if ($failures.Count) { throw "Server se ni zagnal. Glej $($srv.Log)" }
    Send-Command $srv 'noppes slay npcs 2000'
    Start-Sleep -Seconds 2
    Send-Command $srv 'noppes clone spawn HB_A 1 8,4,8'
    Send-Command $srv 'noppes clone spawn HB_B 1 8,4,14'
    Send-Command $srv 'noppes clone spawn HB_Velik 1 8,4,20'
    Send-Command $srv 'noppes clone spawn M39_Nos 1 20,4,8'
    Send-Command $srv 'noppes clone spawn M39_Jah 1 20,4,12'
    foreach ($p in $proge) {
        Send-Command $srv ("noppes clone spawn M39_S{0} 1 8,4,{1}" -f $p.N, $p.Z)
        Send-Command $srv ("noppes clone spawn M39_H{0} 1 16,4,{1}" -f $p.N, $p.Z)
    }
    Start-Sleep -Seconds 2
    Send-Command $srv 'rwdiag chunks on 1'
    Check 'NPC-ji pod prisilno nalozenimi chunki' (Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' 1 60)
    Start-Sleep -Seconds 3
    $pol0 = Get-Polozaji $srv $vseM39
    Check ("vseh {0} NPC-jev M39_* nalozenih" -f $vseM39) ($null -ne $pol0)
    if ($failures.Count) { throw 'Postavitev ni uspela.' }

    Step 4 'Nosilec z jahacem (probe)'
    Send-Command $srv 'rwhitbox global 1'
    Check 'jahac sede na nosilca' (Move-Npc $srv 'mount M39_Jah M39_Nos')
    Start-Sleep -Seconds 1
    $p0 = Probe $srv 'HB_A' 'HB_B'
    $v0 = if ($null -ne $p0) { $p0.VA } else { 0 }
    Check ("referenca: oba original brez jahanja v0 = {0:N6}" -f $v0) (($null -ne $p0) -and $v0 -gt 0 -and (Blizu $p0.VB $v0 1e-6))
    $j1 = Probe $srv 'M39_Nos' 'HB_B'
    Check 'J1: nosilec original z jahacem: nosilec 0, HB_B v0 (vanilla)' (($null -ne $j1) -and $j1.VA -eq 0 -and (Blizu $j1.VB $v0 1e-6))
    Send-Command $srv 'rwhitbox solid M39_Nos'
    $j2 = Probe $srv 'M39_Nos' 'HB_B'
    $j2r = Probe $srv 'HB_B' 'M39_Nos'
    Check 'J2: nosilec solid z jahacem: nosilec 0, HB_B v0 (iz obeh strani)' (($null -ne $j2) -and ($null -ne $j2r) -and $j2.VA -eq 0 -and (Blizu $j2.VB $v0 1e-6) -and $j2r.VB -eq 0 -and (Blizu $j2r.VA $v0 1e-6))
    Send-Command $srv 'rwhitbox solid M39_Jah'
    $j3 = Probe $srv 'M39_Jah' 'M39_Nos'
    Check 'J3: jahac proti lastnemu nosilcu: 0 / 0' (($null -ne $j3) -and $j3.VA -eq 0 -and $j3.VB -eq 0)
    Send-Command $srv 'rwhitbox original M39_Nos'
    Send-Command $srv 'rwhitbox original M39_Jah'
    Send-Command $srv 'rwhitbox solid HB_A'
    $j4 = Probe $srv 'M39_Nos' 'HB_A'
    Check 'J4: nosilec original z jahacem proti solid HB_A: 0 / 0 (nihce se ne odrine - ugotovitev)' (($null -ne $j4) -and $j4.VA -eq 0 -and $j4.VB -eq 0)
    Send-Command $srv 'rwhitbox smart M39_Nos'
    Send-Command $srv 'rwhitbox smart HB_Velik'
    $j5 = Probe $srv 'M39_Nos' 'HB_Velik'
    Check ("J5: nosilec smart z jahacem proti smart HB_Velik: 0 / {0:N4} v0 (napoved 0,0714)" -f $(if ($null -ne $j5) { $j5.VB / $v0 } else { 0 })) `
        (($null -ne $j5) -and $j5.VA -eq 0 -and (Blizu $j5.VB ($v0 * 2.0 / 28.0) 0.01))
    Check 'jahac sestopi' (Move-Npc $srv 'dismount M39_Jah')
    Send-Command $srv 'rwhitbox solid M39_Nos'
    $j6 = Probe $srv 'M39_Nos' 'HB_B'
    Check 'J6: po sestopu nosilec solid proti HB_B: 0 / v0' (($null -ne $j6) -and $j6.VA -eq 0 -and (Blizu $j6.VB $v0 1e-6))

    Step 5 'NPC na poti drugega NPC-ja'
    foreach ($p in $proge) { Send-Command $srv ("rwhitbox {0} M39_S{1}" -f $p.Nacin, $p.N) }
    Start-Sleep -Seconds 3
    foreach ($p in $proge) { Send-Command $srv ("tp @e[name=M39_H{0}] 2.5 4 {1}.5" -f $p.N, $p.Z) }
    Send-Command $srv 'rwhitbox track M39_ 800'
    Check 'sledenje 40 s zacelo' (Wait-ForCount $srv ("RWHITBOX-TRACK-ZACETEK n={0} " -f $vseM39) 1 30)
    Check 'sledenje koncano' (Wait-ForCount $srv 'RWHITBOX-TRACK-KONEC ' 1 120)
    $sled = @{}
    foreach ($m in [regex]::Matches((Get-MarkerText $srv.Log), 'RWHITBOX-TRACK ime=(\S+) premik=([\d.]+) skok=([\d.]+) razdalja=([\d.]+) stik=(\d+) premikObStiku=([\d.]+) x=([-\d.]+) z=([-\d.]+)')) {
        $g = $m.Groups
        $sled[$g[1].Value] = [pscustomobject]@{ Premik = [double]$g[2].Value; Skok = [double]$g[3].Value; Razdalja = [double]$g[4].Value
            Stik = [int]$g[5].Value; PremikObStiku = [double]$g[6].Value; X = [double]$g[7].Value; Z = [double]$g[8].Value }
    }
    $okPrihod = $true
    foreach ($p in $proge) {
        $h = $sled[("M39_H{0}" -f $p.N)]; $s = $sled[("M39_S{0}" -f $p.N)]
        if ($null -eq $h -or $null -eq $s) { $okPrihod = $false; continue }
        $doCilja = Razdalja $h ([pscustomobject]@{ X = 16.5; Z = $p.Z + 0.5 })
        $vrsta = if ($p.Stoji) { 'stoji' } else { 'tava' }
        Write-Host ("  proga {0} ({1}, {2}): hodeci do cilja {3:N3}, skok {4:N3}/tick, stik {5} tickov, najmanjsa razdalja {6:N3}; NPC na poti: premik {7:N3}, ob stiku {8:N3}" -f `
            $p.N, $p.Nacin, $vrsta, $doCilja, $h.Skok, $h.Stik, $h.Razdalja, $s.Premik, $s.PremikObStiku)
        if ($doCilja -ge 1.5 -or $h.Skok -gt 1.0) { $okPrihod = $false }
    }
    Check 'P1: vseh osem hodecih na cilju (< 1,5) brez teleporta (skok <= 1,0 na tick)' $okPrihod
    function Stik([int[]]$N) {
        $t = 0; $m = 0.0
        foreach ($i in $N) { $x = $sled[("M39_S{0}" -f $i)]; if ($null -ne $x) { $t += $x.Stik; $m += $x.PremikObStiku } }
        return [pscustomobject]@{ Tickov = $t; Premik = $m; NaTick = $(if ($t -gt 0) { $m / $t } else { [double]::NaN }) }
    }
    $tOrg = Stik @(1, 2, 3); $tSol = Stik @(4, 5, 6); $s7 = $sled['M39_S7']; $s8 = $sled['M39_S8']
    $okStik = ($null -ne $s7) -and ($null -ne $s8) -and $s7.Stik -gt 0 -and $s8.Stik -gt 0 -and $tOrg.Tickov -gt 0 -and $tSol.Tickov -gt 0
    Check ("P2: stik na progah 7 in 8 ter na tavajocih (original {0}, solid {1} tickov)" -f $tOrg.Tickov, $tSol.Tickov) $okStik
    $okPremik = $okStik -and ($tSol.NaTick -lt $tOrg.NaTick) -and $s7.PremikObStiku -eq 0 -and $s8.PremikObStiku -eq 0
    Check ("P3: tavajoci ob stiku na tick: solid {0:N4} < original {1:N4}; stojeca 0" -f $tSol.NaTick, $tOrg.NaTick) $okPremik

    Send-Command $srv 'rwdiag chunks off'
    Check 'server se je cisto ustavil' (Stop-DevServer $srv)
    $log = $log0 + (Get-LogText $srv.Log)
    $err = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'P4: brez ERROR vrstic iz noppes.*' ($err.Count -eq 0)
    if ($err.Count) { $err | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }

    Write-Host ''
    if ($failures.Count -eq 0) { Write-Host 'M3.9 USPESNO: J1-J6 in P1-P4 zelena.'; exit 0 }
    Write-Host 'M3.9 NEUSPESNO:'; $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    if ($null -ne $srv) { $null = Stop-DevServer $srv }
    Write-Host 'M3.9 NEUSPESNO.'
    exit 1
}
