<#
    sled-run.ps1 - M5.Z: sled skript in timerjev, original proti privzetim vrednostim stikal.

    Scenarij in merila: docs\scenariji\M5.Z-sled.md

    Izhodni kriterij M5 zahteva "timerji in script trace se ujemajo z referenco" (PLAN_IMPLEMENTACIJE
    pogl. 6: "razen dokumentiranih bugfixov"). Referenca je original: isti jar s stikali Rw* iz
    dev\run\config\CustomNpcs.cfg (vsa 0, -PrwPrivzeto=0). Kandidat so privzete vrednosti kode
    (-PrwPrivzeto=1, D-055). Oba kraka teceta na svezi kopiji istega sveta; skripta na SLED_Skripta
    (dev\testworld\sled-skripta.js) zapise sled dogodkov init/tick/collide/timer s starostjo NPC-ja,
    sled faze 1 (svez NPC) in faze 2 (po restartu) pa mora biti v obeh krakih enaka znak za znakom.

    Ni meritev: cas se ne meri, zato zaklep scenarija zadosca in tuji java procesi niso ovira.

    Zagon:
        .\sled-run.ps1
        .\sled-run.ps1 -TimeoutSec 240    # vec casa za fazo, ce je stroj pocasen
#>
param([int]$TimeoutSec = 180)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed  = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null
. (Join-Path $root 'meritve-lib.ps1')

$stamp  = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$report = Join-Path $audit ("m5z-sled-{0}.md" -f $stamp)
$osnova = 'rwz-sled-osnova'
$svet   = 'rwz-sled'

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

$failures = @()
$rezultati = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
    $script:rezultati += ("| {0} | {1} |" -f $(if ($Ok) { 'OK' } else { '**NAPAKA**' }), $What)
}

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$Sec) {
    $deadline = (Get-Date).AddSeconds($Sec)
    while ($true) {
        $t = Get-LogText $Srv.Log
        if (([regex]::Matches($t, [regex]::Escape($Marker))).Count -ge $Count) { return $true }
        $sesutje = Get-ServerSesutje $t
        if ($sesutje -ne '') { throw ("Server se je sesul (cakal na '{0}'): {1}" -f $Marker, $sesutje) }
        if ($Srv.Proc.HasExited) {
            Start-Sleep -Milliseconds 500
            return (([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count -ge $Count)
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host ("  ! timeout {0} s pri cakanju na '{1}'" -f $Sec, $Marker)
            return $false
        }
        Start-Sleep -Milliseconds 500
    }
}

function Start-DevServer([string]$World, [string]$Tag, [int]$Privzeto) {
    $outLog = Join-Path $audit ("m5z-sled-{0}.log" -f $Tag)
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    $extra = if ($Privzeto -ge 0) { (' -PrwPrivzeto={0}' -f $Privzeto) } else { '' }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName         = $env:ComSpec
    $psi.Arguments        = ('/c gradlew.bat runServer --offline --no-daemon --console=plain "-PrwWorld={0}"{1} > "{2}" 2>&1' -f $World, $extra, $outLog)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute  = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow   = $true
    $srv = [pscustomobject]@{ Proc = [System.Diagnostics.Process]::Start($psi); Log = $outLog; Tag = $Tag }
    if (-not (Wait-ForCount $srv 'Done (' 1 900)) { throw ("Server {0} se ni zagnal. Glej {1}" -f $Tag, $outLog) }
    $nas = ((Get-LogText $outLog) -match ('Preparing level "' + [regex]::Escape($World) + '"')) -and
           (Test-Path (Join-Path (Join-Path $run $World) 'session.lock'))
    Check ("{0}: server tece v svetu {1}, ne v dev\run\world" -f $Tag, $World) $nas
    if (-not $nas) { throw 'Server ni v svetu iz --world; ne nadaljujem.' }
    # Prvi ukaz po zagonu se lahko izgubi (M5.13); vhod se ogreje z 'list'.
    $ok = $false
    for ($i = 0; $i -lt 6 -and -not $ok; $i++) {
        $pred = ([regex]::Matches((Get-LogText $outLog), 'players online')).Count
        Send-Command $srv 'list'
        $ok = Wait-ForCount $srv 'players online' ($pred + 1) 5
    }
    if (-not $ok) { throw ("Vhod serverja {0} ne odgovarja." -f $Tag) }
    return $srv
}

function Send-Command($Srv, [string]$Cmd) {
    $Srv.Proc.StandardInput.WriteLine($Cmd)
    $Srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}

function Stop-DevServer($Srv) {
    try { Send-Command $Srv 'stop' } catch { }
    if (-not $Srv.Proc.WaitForExit(180 * 1000)) {
        Write-Host '  ! server se ni ustavil sam; ubijam drevo procesov'
        try { & taskkill.exe /PID $Srv.Proc.Id /T /F 2>&1 | Out-Null } catch { }
        return $false
    }
    return $true
}

# Zadnja vrstica z markerjem po poslanem ukazu ('' ob timeoutu).
function Send-Query($Srv, [string]$Cmd, [string]$Marker, [int]$Sec = 30) {
    $pred = ([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count
    Send-Command $Srv $Cmd
    if (-not (Wait-ForCount $Srv $Marker ($pred + 1) $Sec)) { return '' }
    $m = [regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker) + '[^\r\n]*')
    return $m[$m.Count - 1].Value
}

# Sled faze iz loga: deli SLED-D po vrsti in vrstica SLED-KONEC.
function Read-Sled([string]$LogPath, [int]$Faza) {
    $t = Get-LogText $LogPath
    $deli = @{}
    foreach ($m in [regex]::Matches($t, ('SLED-D faza={0} del=(\d+) ([^\r\n]*)' -f $Faza))) {
        $deli[[int]$m.Groups[1].Value] = $m.Groups[2].Value.Trim()
    }
    $k = [regex]::Match($t, ('SLED-KONEC faza={0} dogodkov=(\d+) delov=(\d+) razmiki7=(\S+)' -f $Faza))
    $sled = @()
    for ($i = 0; $i -lt $deli.Count; $i++) {
        if ($deli.ContainsKey($i)) { $sled += ($deli[$i] -split ' ') } else { $sled += "MANJKA-DEL-$i" }
    }
    return [pscustomobject]@{
        Konec    = $k.Success
        Dogodkov = $(if ($k.Success) { [int]$k.Groups[1].Value } else { -1 })
        Delov    = $(if ($k.Success) { [int]$k.Groups[2].Value } else { -1 })
        Razmiki  = $(if ($k.Success) { $k.Groups[3].Value } else { '' })
        DeliN    = $deli.Count
        Sled     = $sled
    }
}

function Copy-World([string]$From, [string]$To) {
    $dst = Join-Path $run $To
    if (Test-Path $dst) { Remove-Item -Recurse -Force $dst }
    Copy-Item -Path (Join-Path $run $From) -Destination $dst -Recurse -Force
    $lock = Join-Path $dst 'session.lock'
    if (Test-Path $lock) { Remove-Item -Force $lock }
}

$zaklep = $null
$srv = $null
$kraki = @{}
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    # Okolje matrike (-Privzeto) ne sme vplivati na ta scenarij; krak doloci -PrwPrivzeto.
    Remove-Item Env:ORG_GRADLE_PROJECT_rwPrivzeto -ErrorAction SilentlyContinue

    Step 0 'Pogoji'
    $zaklep = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'sled-run'
    $eula = Join-Path $run 'eula.txt'
    if (-not ((Test-Path $eula) -and ((Get-Content $eula -Raw) -match 'eula\s*=\s*true'))) {
        throw 'dev\run\eula.txt ni sprejet (glej testworld-run.ps1 -AcceptEula).'
    }
    $props = Join-Path $run 'server.properties'
    if (-not (Test-Path $props)) { throw 'dev\run\server.properties ne obstaja. Pozeni najprej .\testworld.ps1.' }
    $p = Get-Content $props -Raw
    # Merilni kanal je '/say' iz skripte; brez command blokov ga NPCWrapper.executeCommand zavrne (M2.2).
    Check 'server.properties: enable-command-block=true' ($p -match '(?m)^enable-command-block\s*=\s*true')
    Check 'server.properties: level-type=FLAT, seed testnega sveta' (($p -match '(?m)^level-type\s*=\s*FLAT') -and ($p -match '(?m)^level-seed\s*=\s*customnpc-rework'))
    $klon = Join-Path $seed 'customnpcs\clones\1'
    $fixture = @(Get-ChildItem $klon -Filter 'SLED_*.json')
    Check ("fixture SLED_Skripta in SLED_Sosed obstajata ({0})" -f $fixture.Count) ($fixture.Count -eq 2)
    $skriptaJson = Get-Content (Join-Path $klon 'SLED_Skripta.json') -Raw
    Check 'skripta je vlozena v SLED_Skripta.json (vstavi-skripto.py)' (($skriptaJson -match 'SLED-KONEC') -and ($skriptaJson -notmatch '@@SCRIPT@@'))
    if ($failures.Count -gt 0) { throw 'Pogoji niso izpolnjeni.' }

    Step 1 'Osnovni svet: world spawn na 0 4 0'
    # Spawn superflat sveta lahko pade dalec od izhodisca (M0.6); chunk 0,0 je zajamceno nalozen
    # sele po restartu s pribitim spawnom. Oba kraka dobita kopijo istega osnovnega sveta.
    $osnovaDir = Join-Path $run $osnova
    if (Test-Path $osnovaDir) { Remove-Item -Recurse -Force $osnovaDir }
    $srv = Start-DevServer $osnova 'osnova' -1
    Send-Command $srv 'setworldspawn 0 4 0'
    Check 'osnova: world spawn nastavljen' (Wait-ForCount $srv 'Set the world spawn point' 1 60)
    Send-Command $srv 'save-all flush'
    Check 'osnova: svet shranjen' (Wait-ForCount $srv 'Saved the world' 1 180)
    Check 'osnova: server se je cisto ustavil' (Stop-DevServer $srv)
    $srv = $null
    if ($failures.Count -gt 0) { throw 'Osnovni svet ni nastal.' }

    foreach ($krak in @(0, 1)) {
        Step ("2.{0}" -f $krak) ("Krak {0}: {1}" -f $krak, $(if ($krak -eq 0) { 'original (stikala iz configa)' } else { 'privzete vrednosti kode' }))
        Copy-World $osnova $svet
        $dstKlon = Join-Path $run "$svet\customnpcs\clones\1"
        New-Item -ItemType Directory -Force -Path $dstKlon | Out-Null
        foreach ($f in $fixture) { Copy-Item $f.FullName -Destination $dstKlon -Force }

        # --- faza 1: svez NPC ---
        $srv = Start-DevServer $svet ("k{0}-a" -f $krak) $krak
        $logA = $srv.Log
        $priv = [regex]::Match((Get-LogText $logA), ('RWPRIVZETO {0} [^\r\n]*' -f $krak))
        Check ("k{0}: stikala v zahtevanem nacinu ({1})" -f $krak, $(if ($priv.Success) { $priv.Value } else { 'ni vrstice RWPRIVZETO' })) $priv.Success
        foreach ($c in @('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false', 'gamerule doMobSpawning false',
                         'gamerule randomTickSpeed 0', 'gamerule mobGriefing false', 'time set 6000', 'weather clear 1000000',
                         'difficulty 0', 'kill @e[type=!player]')) {
            Send-Command $srv $c
        }
        # Sosed najprej: rwdiag chunks potrebuje NPC za tiket; skripta zacne sled ob svojem spawnu.
        Send-Command $srv 'noppes clone spawn SLED_Sosed 1 1,4,0'
        $chunks = Send-Query $srv 'rwdiag chunks on 1' 'RWDIAG-CHUNKS stanje=on'
        Check ("k{0}: chunki prizorisca prisilno nalozeni ({1})" -f $krak, $chunks) ($chunks -match 'npc=1\b')
        Send-Command $srv 'noppes clone spawn SLED_Skripta 1 0,4,0'
        Check ("k{0}: faza 1 se je zacela (SLED-INIT faza=1)" -f $krak) (Wait-ForCount $srv 'SLED-INIT faza=1' 1 60)
        Check ("k{0}: faza 1 je koncana (SLED-KONEC faza=1)" -f $krak) (Wait-ForCount $srv 'SLED-KONEC faza=1' 1 $TimeoutSec)
        $col = Send-Query $srv 'rwcollide' 'RWCOLLIDE nacin='
        Send-Command $srv 'save-all flush'
        Check ("k{0}: svet shranjen po fazi 1" -f $krak) (Wait-ForCount $srv 'Saved the world' 1 180)
        Check ("k{0}: server faze 1 se je cisto ustavil" -f $krak) (Stop-DevServer $srv)
        $srv = $null

        # --- faza 2: restart ---
        $srv = Start-DevServer $svet ("k{0}-b" -f $krak) $krak
        $logB = $srv.Log
        Check ("k{0}: po restartu stikala v zahtevanem nacinu" -f $krak) ((Get-LogText $logB) -match ('RWPRIVZETO {0} ' -f $krak))
        $null = Send-Query $srv 'rwdiag chunks on 1' 'RWDIAG-CHUNKS stanje=on'
        Check ("k{0}: faza 2 se je zacela (SLED-INIT faza=2)" -f $krak) (Wait-ForCount $srv 'SLED-INIT faza=2' 1 60)
        Check ("k{0}: faza 2 je koncana (SLED-KONEC faza=2)" -f $krak) (Wait-ForCount $srv 'SLED-KONEC faza=2' 1 $TimeoutSec)
        Check ("k{0}: server faze 2 se je cisto ustavil" -f $krak) (Stop-DevServer $srv)
        $srv = $null

        $s1 = Read-Sled $logA 1
        $s2 = Read-Sled $logB 2
        $napake = ([regex]::Matches((Get-LogText $logA) + (Get-LogText $logB), 'script errored|ScriptException|CustomNPCsException')).Count
        Check ("k{0}: brez napak skripte v logu ({1})" -f $krak, $napake) ($napake -eq 0)
        Check ("k{0}: sled faze 1 je cela (delov {1}/{2}, dogodkov {3}/{4})" -f $krak, $s1.DeliN, $s1.Delov, $s1.Sled.Count, $s1.Dogodkov) `
            ($s1.Konec -and $s1.DeliN -eq $s1.Delov -and $s1.Sled.Count -eq $s1.Dogodkov)
        Check ("k{0}: sled faze 2 je cela (delov {1}/{2}, dogodkov {3}/{4})" -f $krak, $s2.DeliN, $s2.Delov, $s2.Sled.Count, $s2.Dogodkov) `
            ($s2.Konec -and $s2.DeliN -eq $s2.Delov -and $s2.Sled.Count -eq $s2.Dogodkov)
        $j1 = ($s1.Sled -join ' ')
        # Vsebina faze 1: vsi timerji, ukazi iz callbackov in collide s sosedom.
        $vsebina = @('^I@', ' T1@', ' T2@', ' T3@', ' T4@', ' R3@', ' X=da@', ' H2=false@', ' F1@', ' S3=true@', ' S3b=false@',
                     ' H1=false,H7=true@', ' K@', ' CSLED_Sosed@')
        $manjka = @($vsebina | Where-Object { $j1 -notmatch $_ })
        Check ("k{0}: faza 1 vsebuje vse vrste dogodkov{1}" -f $krak, $(if ($manjka.Count) { ' - manjka ' + ($manjka -join ',') } else { '' })) ($manjka.Count -eq 0)
        $j2 = ($s2.Sled -join ' ')
        Check ("k{0}: timer 7 je prezivel restart (H7=true)" -f $krak) ($j2 -match '(^| )H7=true( |$)')
        $r = @($s2.Razmiki -split ',' | Where-Object { $_ -match '^\d+$' } | ForEach-Object { [int]$_ })
        Check ("k{0}: timer 7 po restartu proza v enakih razmikih ({1})" -f $krak, $s2.Razmiki) (($r.Count -ge 3) -and (@($r | Select-Object -Unique).Count -eq 1))
        $kraki[$krak] = [pscustomobject]@{ S1 = $s1; S2 = $s2; Collide = $col; Priv = $(if ($priv.Success) { $priv.Value } else { '' }); Razmik = $(if ($r.Count) { $r[0] } else { -1 }) }
    }

    Step 3 'Primerjava krakov'
    $k0 = $kraki[0]; $k1 = $kraki[1]
    foreach ($faza in @(1, 2)) {
        $a = if ($faza -eq 1) { $k0.S1.Sled } else { $k0.S2.Sled }
        $b = if ($faza -eq 1) { $k1.S1.Sled } else { $k1.S2.Sled }
        $prva = -1
        for ($i = 0; $i -lt [Math]::Max($a.Count, $b.Count); $i++) {
            $x = if ($i -lt $a.Count) { $a[$i] } else { '<konec>' }
            $y = if ($i -lt $b.Count) { $b[$i] } else { '<konec>' }
            if ($x -ne $y) { $prva = $i; break }
        }
        $opis = if ($prva -lt 0) { ("{0} dogodkov" -f $a.Count) } else { ("prva razlika pri dogodku {0}: original '{1}', privzeto '{2}'" -f $prva, $(if ($prva -lt $a.Count) { $a[$prva] } else { '<konec>' }), $(if ($prva -lt $b.Count) { $b[$prva] } else { '<konec>' })) }
        Check ("Z{0}: sled faze {0} je v obeh krakih enaka ({1})" -f $faza, $opis) (($prva -lt 0) -and ($a.Count -gt 0))
    }
    Check ("Z3: razmiki timerja 7 po restartu so enaki (original {0}, privzeto {1})" -f $k0.S2.Razmiki, $k1.S2.Razmiki) ($k0.S2.Razmiki -eq $k1.S2.Razmiki)
    # Dokaz, da je krak 1 res tekel z drugo kodo poti: SLED_Sosed nima skript, zato S4 (RwCollideSkip=1)
    # njegovo poizvedbo preskoci; v originalu (nacin 0) se ne preskoci nic.
    $p0 = [regex]::Match($k0.Collide, 'nacin=(\d+).*preskocenih=(\d+)')
    $p1 = [regex]::Match($k1.Collide, 'nacin=(\d+).*preskocenih=(\d+)')
    Check ("Z4: original ne preskoci collide poizvedb ({0})" -f $k0.Collide) ($p0.Success -and $p0.Groups[1].Value -eq '0' -and $p0.Groups[2].Value -eq '0')
    Check ("Z4: privzeto preskoci poizvedbo soseda brez skripte ({0})" -f $k1.Collide) ($p1.Success -and $p1.Groups[1].Value -eq '1' -and [int]$p1.Groups[2].Value -gt 0)
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    $failures += "izjema: $_"
    $rezultati += ("| **NAPAKA** | izjema: {0} |" -f $_)
}
finally {
    if ($null -ne $srv) { $null = Stop-DevServer $srv }
    if ($null -ne $zaklep) { $zaklep.Close() }
}

# --- porocilo ----------------------------------------------------------------
$git = try { (& git -C $root rev-parse --short HEAD 2>$null) } catch { '?' }
$lines = @()
$lines += "# M5.Z sled skript in timerjev - $stamp"
$lines += ''
$lines += ('Scenarij: `docs/scenariji/M5.Z-sled.md`. Commit {0}. Izid: **{1}**.' -f $git, $(if ($failures.Count -eq 0) { 'USPESNO' } else { 'NEUSPESNO' }))
$lines += ''
$lines += '| Izid | Preverba |'
$lines += '|---|---|'
$lines += $rezultati
foreach ($krak in @(0, 1)) {
    if (-not $kraki.ContainsKey($krak)) { continue }
    $k = $kraki[$krak]
    $lines += ''
    $lines += ("## Krak {0}" -f $krak)
    $lines += ''
    $lines += ('- {0}' -f $k.Priv)
    $lines += ('- {0}' -f $k.Collide)
    $lines += ''
    $lines += '```'
    $lines += ('faza 1: ' + ($k.S1.Sled -join ' '))
    $lines += ('faza 2: ' + ($k.S2.Sled -join ' ') + ' | razmiki7=' + $k.S2.Razmiki)
    $lines += '```'
}
[System.IO.File]::WriteAllLines($report, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ''
Write-Host ("Porocilo: {0}" -f $report)

if ($failures.Count -eq 0) {
    Write-Host 'M5.Z SLED USPESNO: sled skript in timerjev je z privzetimi stikali enaka originalu.'
    exit 0
}
Write-Host 'M5.Z SLED NEUSPESNO:'
$failures | ForEach-Object { Write-Host "  - $_" }
exit 1
