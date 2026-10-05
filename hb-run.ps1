# M3.8 (R6) - scenarij nacinov hitboxa: original / solid / smart (velikost, scit).
# Zasnova: docs/01-ARHITEKTURA.md D-025, zahteva: docs/02-ZAHTEVE.md R6.
#
# Faza P (probe): '/rwhitbox probe A B 0.3' postavi B 0,3 bloka od A, izvede en trk po pravi kodi
#   (A.applyEntityCollision(B)) in izpise vodoravni hitrosti - deterministicno, brez AI.
# Faza N (vanilla pot): '/rwhitbox nearby A B 0.3' namesto enega trka poklice pravi vanilla
#   A.collideWithNearbyEntities() (izbira sosedov + collideWithEntity). Naravni premik v svetu ni
#   merilo: NPC-ja premika tudi lastni AI (wander proti sredini bloka), NoAI pa v 1.12 ustavi tudi
#   premik od potiska (5. 10., prvi zagon scenarija).
# Faza R (restart): nacin prezivi shranjevanje in ponovni zagon (NBT RwHitboxMode).
#
# Merila:
#   H0  global 0, oba original: vA = vB > 0 (vanilla; v0 = referenca)
#   H0b global 0, HB_A solid v NBT: se vedno vanilla (stikalo izklopljeno = original)
#   H1  global 1, A solid, B original: vA = 0, vB = v0
#   H2  smart, velikost 5 proti 15: vA/v0 = 2*27/28, vV/v0 = 2/28 (+-1 %)
#   H3  smart, scit: scit zaznan; brez scita 4/3 v0, s scitom 2/3 v0 (+-1 %)
#   H4  smart, enaka velikost: vA = vB = v0
#   H5  vanilla pot, A solid, B original: iz obeh strani vA = 0, vB = v0
#   H6  vanilla pot, oba original: vA = vB = v0
#   H7  po restartu: HB_A solid, HB_B original, HB_Velik in HB_Scit smart
#   H8  brez ERROR iz noppes.* in brez sesutja
#
# Zagon (iz seje Claude Code najprej: Remove-Item Env:NoDefaultCurrentDirectoryInExePath):
#     node dev\testworld\hb-fixture.js   (enkrat; fixture so v repozitoriju)
#     .\testworld.ps1
#     .\hb-run.ps1

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
    $outLog = Join-Path $audit ("m38-hitbox-{0}.log" -f $Tag)
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
        Start-Sleep -Seconds 1
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
function Probe($srv, [string]$A, [string]$B, [string]$Pot = 'probe') {
    Send-Command $srv ("rwhitbox {0} {1} {2} 0.3" -f $Pot, $A, $B)
    $script:nProbe++
    if (-not (Wait-ForCount $srv 'RWHITBOX-PROBE ' $script:nProbe 30)) { return $null }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), 'RWHITBOX-PROBE a=(\S+) b=(\S+) nacinA=(\w+) nacinB=(\w+) sirinaA=([\d.]+) sirinaB=([\d.]+) masaA=([\d.]+) masaB=([\d.]+) scitA=(\w+) scitB=(\w+) vA=([\d.]+) vB=([\d.]+)')
    $g = $m[$m.Count - 1].Groups
    $r = [pscustomobject]@{ A = $g[1].Value; B = $g[2].Value; NacinA = $g[3].Value; NacinB = $g[4].Value
        SirinaB = [double]$g[6].Value; ScitA = ($g[9].Value -eq 'true'); ScitB = ($g[10].Value -eq 'true')
        VA = [double]$g[11].Value; VB = [double]$g[12].Value }
    Write-Host ("  probe {0}({1}) / {2}({3}): vA={4:N6} vB={5:N6}" -f $r.A, $r.NacinA, $r.B, $r.NacinB, $r.VA, $r.VB)
    return $r
}
$script:nWhere = 0
function Where-Npc($srv, [string]$Ime) {
    Send-Command $srv ("rwhitbox where {0}" -f $Ime)
    $script:nWhere++
    if (-not (Wait-ForCount $srv 'RWHITBOX-WHERE ' $script:nWhere 30)) { return $null }
    $m = [regex]::Matches((Get-MarkerText $srv.Log), 'RWHITBOX-WHERE ime=(\S+) x=([-\d.]+) z=([-\d.]+)')
    $g = $m[$m.Count - 1].Groups
    return [pscustomobject]@{ X = [double]$g[2].Value; Z = [double]$g[3].Value }
}
function Razdalja($p, $q) { if ($null -eq $p -or $null -eq $q) { return [double]::NaN }; [math]::Sqrt(($p.X - $q.X) * ($p.X - $q.X) + ($p.Z - $q.Z) * ($p.Z - $q.Z)) }
function Blizu([double]$x, [double]$want, [double]$rel) { if ($want -eq 0) { return [math]::Abs($x) -lt 1e-9 }; return [math]::Abs($x - $want) -le [math]::Abs($want) * $rel }

$srv = $null
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Step 1 'Priprava'
    $zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'hb-run'
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
    Check 'fixture HB_* so v svetu' (@(Get-ChildItem $clones -Filter 'HB_*.json' -ErrorAction SilentlyContinue).Count -eq 4)
    if ($failures.Count) { throw 'Priprava ni uspela (fixture: node dev\testworld\hb-fixture.js, nato .\testworld.ps1).' }

    Step 2 'Zagon 1: postavitev in probe'
    $srv = Start-DevServer '1'
    Check 'server je dosegel "Done ("' (Wait-ForCount $srv 'Done (' 1 900)
    if ($failures.Count) { throw "Server se ni zagnal. Glej $($srv.Log)" }
    Set-Content -Path $scenMark -Value ("hb-run {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm')) -Encoding ASCII
    Send-Command $srv 'setworldspawn 0 4 0'
    Send-Command $srv 'noppes slay npcs 2000'
    Start-Sleep -Seconds 2
    Send-Command $srv 'noppes clone spawn HB_A 1 8,4,8'
    Send-Command $srv 'noppes clone spawn HB_B 1 8,4,14'
    Send-Command $srv 'noppes clone spawn HB_Velik 1 8,4,20'
    Send-Command $srv 'noppes clone spawn HB_Scit 1 8,4,26'
    Start-Sleep -Seconds 2
    Send-Command $srv 'rwdiag chunks on 1'
    Check 'NPC-ji pod prisilno nalozenimi chunki' (Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' 1 60)
    Start-Sleep -Seconds 3

    Send-Command $srv 'rwhitbox global 0'
    $p0 = Probe $srv 'HB_A' 'HB_B'
    $v0 = if ($null -ne $p0) { $p0.VA } else { 0 }
    Check ("H0: global 0, original: vA = vB = {0:N6} > 0" -f $v0) (($null -ne $p0) -and $v0 -gt 0 -and (Blizu $p0.VB $v0 1e-6))
    Send-Command $srv 'rwhitbox solid HB_A'
    $p0b = Probe $srv 'HB_A' 'HB_B'
    Check 'H0b: global 0, HB_A solid v NBT: se vedno vanilla' (($null -ne $p0b) -and (Blizu $p0b.VA $v0 1e-6) -and (Blizu $p0b.VB $v0 1e-6) -and $p0b.NacinA -eq 'original')

    Send-Command $srv 'rwhitbox global 1'
    $p1 = Probe $srv 'HB_A' 'HB_B'
    Check 'H1: solid A se ne premakne, B odrine kot vanilla' (($null -ne $p1) -and $p1.NacinA -eq 'solid' -and $p1.VA -eq 0 -and (Blizu $p1.VB $v0 1e-6))
    $p1r = Probe $srv 'HB_B' 'HB_A'
    Check 'H1: obratno (B klice trk s solid A)' (($null -ne $p1r) -and $p1r.VB -eq 0 -and (Blizu $p1r.VA $v0 1e-6))

    Send-Command $srv 'rwhitbox smart HB_A'
    Send-Command $srv 'rwhitbox smart HB_B'
    Send-Command $srv 'rwhitbox smart HB_Velik'
    Send-Command $srv 'rwhitbox smart HB_Scit'
    $p2 = Probe $srv 'HB_A' 'HB_Velik'
    Check ("H2: smart 5 proti 15 (sirina {0}): mali {1:N4} v0, velik {2:N4} v0" -f $p2.SirinaB, ($p2.VA / $v0), ($p2.VB / $v0)) `
        (($null -ne $p2) -and (Blizu $p2.VA ($v0 * 54.0 / 28.0) 0.01) -and (Blizu $p2.VB ($v0 * 2.0 / 28.0) 0.01))
    $p3 = Probe $srv 'HB_B' 'HB_Scit'
    Check ("H3: scit zaznan (scitB={0}); brez {1:N4} v0, s scitom {2:N4} v0" -f $p3.ScitB, ($p3.VA / $v0), ($p3.VB / $v0)) `
        (($null -ne $p3) -and $p3.ScitB -and (-not $p3.ScitA) -and (Blizu $p3.VA ($v0 * 4.0 / 3.0) 0.01) -and (Blizu $p3.VB ($v0 * 2.0 / 3.0) 0.01))
    $p4 = Probe $srv 'HB_A' 'HB_B'
    Check 'H4: smart, enaka velikost = vanilla' (($null -ne $p4) -and (Blizu $p4.VA $v0 1e-6) -and (Blizu $p4.VB $v0 1e-6))

    Step 3 'Vanilla pot trka (collideWithNearbyEntities)'
    Send-Command $srv 'rwhitbox solid HB_A'
    Send-Command $srv 'rwhitbox original HB_B'
    $n1 = Probe $srv 'HB_A' 'HB_B' 'nearby'
    $n2 = Probe $srv 'HB_B' 'HB_A' 'nearby'
    Check 'H5: vanilla pot, solid A: A 0, B v0 iz obeh strani' (($null -ne $n1) -and ($null -ne $n2) -and $n1.VA -eq 0 -and (Blizu $n1.VB $v0 1e-6) -and $n2.VB -eq 0 -and (Blizu $n2.VA $v0 1e-6))
    Send-Command $srv 'rwhitbox original HB_A'
    $n3 = Probe $srv 'HB_A' 'HB_B' 'nearby'
    Check 'H6: vanilla pot, oba original: vA = vB = v0' (($null -ne $n3) -and (Blizu $n3.VA $v0 1e-6) -and (Blizu $n3.VB $v0 1e-6))

    Send-Command $srv 'rwhitbox solid HB_A'
    Send-Command $srv 'rwhitbox smart HB_Velik'
    Send-Command $srv 'save-all flush'
    Check 'svet shranjen' (Wait-ForCount $srv 'Saved the world' 1 120)
    Check 'server 1 se je cisto ustavil' (Stop-DevServer $srv)
    $log1 = Get-LogText $srv.Log

    Step 4 'Zagon 2: nacin po restartu'
    $srv = Start-DevServer '2'
    Check 'server 2 je dosegel "Done ("' (Wait-ForCount $srv 'Done (' 1 900)
    Send-Command $srv 'rwdiag chunks on 1'
    $null = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' 1 60
    Start-Sleep -Seconds 2
    Send-Command $srv 'rwhitbox status HB_'
    $okS = Wait-ForCount $srv 'RWHITBOX global=' 1 30
    $m = [regex]::Match((Get-MarkerText $srv.Log), 'RWHITBOX global=(\d) ujemanj=(\d+) original=(\d+) solid=(\d+) smart=(\d+)')
    Check ("H7: po restartu {0}" -f $m.Value) ($okS -and $m.Success -and [int]$m.Groups[2].Value -eq 4 -and [int]$m.Groups[3].Value -eq 1 -and [int]$m.Groups[4].Value -eq 1 -and [int]$m.Groups[5].Value -eq 2)
    Send-Command $srv 'rwdiag chunks off'
    Check 'server 2 se je cisto ustavil' (Stop-DevServer $srv)
    $log = $log1 + (Get-LogText $srv.Log)
    $err = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'H8: brez ERROR vrstic iz noppes.*' ($err.Count -eq 0)
    if ($err.Count) { $err | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }

    Write-Host ''
    if ($failures.Count -eq 0) { Write-Host 'M3.8 USPESNO: H0-H8 zelena.'; exit 0 }
    Write-Host 'M3.8 NEUSPESNO:'; $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    if ($null -ne $srv) { $null = Stop-DevServer $srv }
    Write-Host 'M3.8 NEUSPESNO.'
    exit 1
}
