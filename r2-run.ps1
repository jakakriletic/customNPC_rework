# M2.3 - skriptirana reprodukcija R2 (leteci NPC in ovira), merila L1-L8.
#
# Scenarij in razlaga: docs/scenariji/M2.3-R2.md
#
# Tri vzporedne proge hkrati v istem svetu in istem ticku:
#   proga F  6 letecih   + zid     - merjeno stanje
#   proga W  6 kopenskih + isti zid - pove, ali je zid sploh ovira
#   proga P  6 letecih   brez ovire - pove, ali letenje deluje brez ovire
# Brez obeh kontrol razlika ne pomeni nicesar.
#
# Vrstni red je pomemben: chunki se prisilno nalozijo in ogrejejo PREDEN se spawna
# krmilni NPC, ker njegova skripta zacne steti takoj ob spawnu. Brez tega bi se
# scenarij po 300 tickih ustavil sredi faze A in bi izgledal kot okvara AI
# (WorldServer.updateEntities:628-644, glej M2.1d).
#
# Zagon:
#     .\r2-run.ps1
#     .\r2-run.ps1 -AcceptEula
#     .\r2-run.ps1 -ChunkRadius 3     # sirsi obroc, ce NPC-ji zaidejo s proge

param([switch]$AcceptEula, [int]$ChunkRadius = 2, [int]$WarmupSeconds = 10,
      [int]$ScenarioTimeoutSec = 180)

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$seed   = Join-Path $root 'dev\testworld'
$dumps  = Join-Path $run 'logs\rwdiag'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

$lanes      = @('F', 'W', 'P')
$laneOpis   = @{ F = 'leteci + zid'; W = 'kopenski + zid'; P = 'leteci, prosto' }
$phases     = @('A', 'B', 'C', 'D')
$phaseOpis  = @{ A = 'navigateTo en sam klic'; B = 'navigateTo osvezen'; C = 'setAttackTarget'; D = 'navigateTo osvezen, izhodisce pol bloka izven mreze' }

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

# Vsak odgovor ukaza /rwdiag se v logu pojavi DVAKRAT: enkrat kot konzolni odgovor
# ([minecraft/DedicatedServer]) in enkrat prek LogWriterja ([FINE/CustomNPCs]).
# Steti markerje nad celim logom zato pomeni dvojne zadetke; 17. 9. je tako D7b padel
# lazno negativno. Markerje stejemo samo nad konzolnim kanalom (M0.7, dnevnik 26).
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
        if ($Srv.Proc.HasExited) {
            Start-Sleep -Milliseconds 500
            $n = ([regex]::Matches((Get-MarkerText $Srv.Log), [regex]::Escape($Marker))).Count
            if ($n -ge $Count) { return $true }
            Write-Host ("  ! proces se je koncal, preden se je pojavil marker '{0}'" -f $Marker)
            return $false
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host ("  ! timeout {0} s pri cakanju na '{1}'" -f $TimeoutSec, $Marker)
            return $false
        }
        Start-Sleep -Seconds 1
    }
}

function Wait-ForMarker($Srv, [string]$Marker, [int]$TimeoutSec) {
    return (Wait-ForCount $Srv $Marker 1 $TimeoutSec)
}

function Start-DevServer {
    $outLog = Join-Path $audit 'm23-r2.log'
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

function Send-File($srv, [string]$Path) {
    $sent = 0
    foreach ($line in (Get-Content $Path)) {
        $t = $line.Trim()
        if ($t -eq '' -or $t.StartsWith('#')) { continue }
        Send-Command $srv $t
        $sent++
    }
    Write-Host ("  poslanih ukazov: {0}" -f $sent)
    return $sent
}

function Stop-DevServer($srv, [int]$TimeoutSec = 180) {
    Send-Command $srv 'stop'
    if (-not $srv.Proc.WaitForExit($TimeoutSec * 1000)) {
        Write-Host '  ! server se ni ustavil sam; ubijam proces'
        try { $srv.Proc.Kill() } catch { }
        return $false
    }
    return $true
}

$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}

# "R2-SETUP progaF=6 progaW=6 progaP=6 ciljF=da ciljW=da ciljP=da"
#   -> @{F=6; W=6; P=6; CiljF='da'; CiljW='da'; CiljP='da'}; ce ni zadetka, F=-1
function Read-Setup([string]$LogPath) {
    $m = [regex]::Match((Get-MarkerText $LogPath),
        'R2-SETUP progaF=(\d+) progaW=(\d+) progaP=(\d+) ciljF=(\w+) ciljW=(\w+) ciljP=(\w+)')
    if (-not $m.Success) { return @{ F = -1; W = -1; P = -1; CiljF = '?'; CiljW = '?'; CiljP = '?' } }
    return @{
        F = [int]$m.Groups[1].Value; W = [int]$m.Groups[2].Value; P = [int]$m.Groups[3].Value
        CiljF = $m.Groups[4].Value; CiljW = $m.Groups[5].Value; CiljP = $m.Groups[6].Value
    }
}

# Vse vzorcne vrstice ene faze in proge, kot objekti.
function Read-Samples([string]$LogPath, [string]$Phase, [string]$Lane) {
    $pattern = 'R2-S faza=' + $Phase + ' tick=(\d+) proga=' + $Lane +
               ' navig=(\d+)/(\d+) cele=(\d+)/\d+' +
               ' prevozenoPovp=(-?[\d.]+) prevozenoMax=(-?[\d.]+)' +
               ' yPovp=(-?[\d.]+) yMax=(-?[\d.]+)' +
               ' cezOviro=(\d+)/\d+' +
               ' doCiljaMin=(-?[\d.]+) doCiljaPovp=(-?[\d.]+)' +
               ' razpon=(-?[\d.]+) zastoj=(\d+)/\d+' +
               ' dStarost=(-?\d+)/(-?\d+) gib=([\d.]+)/([\d.]+)'
    $out = @()
    foreach ($m in [regex]::Matches((Get-MarkerText $LogPath), $pattern)) {
        $out += [pscustomobject]@{
            Tick         = [int]$m.Groups[1].Value
            Navig        = [int]$m.Groups[2].Value
            Skupaj       = [int]$m.Groups[3].Value
            Cele         = [int]$m.Groups[4].Value
            PrevozenoAvg = [double]$m.Groups[5].Value
            PrevozenoMax = [double]$m.Groups[6].Value
            YAvg         = [double]$m.Groups[7].Value
            YMax         = [double]$m.Groups[8].Value
            CezOviro     = [int]$m.Groups[9].Value
            DoCiljaMin   = [double]$m.Groups[10].Value
            DoCiljaAvg   = [double]$m.Groups[11].Value
            Razpon       = [double]$m.Groups[12].Value
            Zastoj       = [int]$m.Groups[13].Value
            DStarostMin  = [int]$m.Groups[14].Value
            DStarostMax  = [int]$m.Groups[15].Value
            GibAvg       = [double]$m.Groups[16].Value
            GibMax       = [double]$m.Groups[17].Value
        }
    }
    return $out
}

# Najvecji cezOviro v celi fazi, ne samo na koncu: NPC lahko oviro prestopi in se vrne.
function Max-CezOviro($Samples) {
    $max = 0
    foreach ($s in $Samples) { if ($s.CezOviro -gt $max) { $max = $s.CezOviro } }
    return $max
}

function Max-Prevozeno($Samples) {
    $max = 0.0
    foreach ($s in $Samples) { if ($s.PrevozenoMax -gt $max) { $max = $s.PrevozenoMax } }
    return $max
}

# Najblizji priblizek cilju v celi fazi. 'Premaknil se je' in 'prisel je' nista isto:
# NPC, ki 12 blokov zaide vstran, bi zadostil prvemu in ne drugemu.
function Min-DoCilja($Samples) {
    $min = 1e9
    foreach ($s in $Samples) { if ($s.DoCiljaMin -lt $min) { $min = $s.DoCiljaMin } }
    if ($min -ge 1e9) { return -1 }
    return $min
}

# Najmanjsi prirastek starosti med dvema vzorcema. Prvi vzorec faze se izpusti: njegov
# prirastek meri razmik med fazama, ne merilnega intervala.
# -1 pomeni 'ni podatka', ne 'entiteta ne tika'.
function Min-DStarost($Samples) {
    if ($Samples.Count -lt 2) { return -1 }
    $min = [int]::MaxValue
    for ($i = 1; $i -lt $Samples.Count; $i++) {
        if ($Samples[$i].DStarostMin -lt $min) { $min = $Samples[$i].DStarostMin }
    }
    return $min
}

# Najvecje gibanje v celi fazi. Nic pri mirujoci progi pomeni, da moveHelper ni dodal
# gibanja; vec kot nic pomeni, da je gibanje bilo in ga je pojel move().
function Max-Gib($Samples) {
    $max = 0.0
    foreach ($s in $Samples) { if ($s.GibMax -gt $max) { $max = $s.GibMax } }
    return $max
}

function Format-Lane([string]$Phase, [string]$Lane, $Samples) {
    if ($Samples.Count -eq 0) { return ("  faza {0} proga {1}: NI VZORCEV" -f $Phase, $Lane) }
    $last = $Samples[$Samples.Count - 1]
    return ("  faza {0} proga {1} ({2,-14}): vzorcev={3,2} | konec: navig={4}/{5} cele={6}/{5} cezOviro={7}/{5} zastoj={8}/{5} prevozeno(povp/max)={9}/{10} y(povp/max)={11}/{12} doCilja(min/povp)={13}/{14} razpon={15} | dStarost(min)={16} gib(max)={17}" -f `
        $Phase, $Lane, $laneOpis[$Lane], $Samples.Count,
        $last.Navig, $last.Skupaj, $last.Cele, $last.CezOviro, $last.Zastoj,
        $last.PrevozenoAvg, $last.PrevozenoMax, $last.YAvg, $last.YMax,
        $last.DoCiljaMin, $last.DoCiljaAvg, $last.Razpon,
        (Min-DStarost $Samples), (Max-Gib $Samples))
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA in fixture'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\r2-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # Merilni kanal scenarija je '/say' iz skripte krmilnika. NPCWrapper.executeCommand
    # (reference-src/.../wrapper/NPCWrapper.java:201) vrze CustomNPCsException, ce command
    # bloki niso vklopljeni. Brez te preverbe bi scenarij padel brez ene same vrstice R2-*,
    # kar izgleda kot okvara skripte, ne kot napacna nastavitev serverja.
    $propsFile = Join-Path $run 'server.properties'
    if (Test-Path $propsFile) {
        $props = @(Get-Content $propsFile)
        if ($props -match '^enable-command-block\s*=\s*true') {
            Write-Host '  enable-command-block=true'
        } else {
            if ($props -match '^enable-command-block\s*=') {
                $props = $props -replace '^enable-command-block\s*=.*', 'enable-command-block=true'
            } else {
                $props += 'enable-command-block=true'
            }
            Set-Content -Path $propsFile -Value $props -Encoding ASCII
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa R2-*)'
        }
    } else {
        Write-Host '  ! dev\run\server.properties se ne obstaja; ustvari ga prvi zagon.'
        Write-Host '    Ce padejo vse preverbe R2-*, preveri enable-command-block in pozeni znova.'
    }

    # Fixture se kopirajo v dev\run\world, server pa nalozi svet, ki ga imenuje level-name
    # v dev\run\server.properties. Ce se to dvoje razide, server zazene DRUG svet: vsak
    # 'noppes clone spawn' javi "Could not find clone file", v svetu ni NPC-jev, scenarij
    # pa nato deset minut caka na markerje, ki jih ne bo. 17. 9. je to stalo cel zagon -
    # server.properties je bil od smoke testa (level-name=m05-smoke). Popravek ene vrstice
    # ne bi zadoscal: smoke test prepise cel server.properties (drug seed, druga vidna
    # razdalja, druga tezavnost), zato scenarij raje pade takoj in pove, kaj pognati.
    function Get-Prop([string]$File, [string]$Key) {
        $line = @(Get-Content $File | Where-Object { $_ -match ('^' + [regex]::Escape($Key) + '\s*=') })
        if ($line.Count -eq 0) { return '' }
        return ($line[0] -replace ('^' + [regex]::Escape($Key) + '\s*=\s*'), '').Trim()
    }
    $seedProps = Join-Path $seed 'server.properties'
    if ((Test-Path $propsFile) -and (Test-Path $seedProps)) {
        $mismatch = @()
        foreach ($key in @('level-name', 'level-seed')) {
            $want = Get-Prop $seedProps $key
            $have = Get-Prop $propsFile $key
            if ($want -ne $have) { $mismatch += ("{0} je '{1}', pricakovano '{2}'" -f $key, $have, $want) }
        }
        if ($mismatch.Count -eq 0) {
            Check ("dev\run\server.properties je od testnega sveta (level-name={0})" -f (Get-Prop $propsFile 'level-name')) $true
        } else {
            Check ("dev\run\server.properties ni od testnega sveta - " + ($mismatch -join '; ')) $false
            throw "server.properties je od drugega scenarija (najbrz smoke test). Pozeni najprej .\testworld.ps1, nato ta scenarij."
        }
    }

    # Fixture morajo biti v svetu, preden se server zazene: ServerCloneController jih
    # nalozi ob zagonu. Kopiramo jih, da scenarij ne zahteva ponovne postavitve sveta.
    $srcClones = Join-Path $seed 'customnpcs\clones\1'
    $dstClones = Join-Path $run  'world\customnpcs\clones\1'
    New-Item -ItemType Directory -Force -Path $dstClones | Out-Null
    $copied = 0
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'R2_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("stiri R2 fixture datoteke so v svetu (kopiranih {0})" -f $copied) ($copied -eq 4)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones" }

    Step 2 'Zagon serverja'
    $s = Start-DevServer
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    Step 3 'Prizorisce in fixture NPC-ji'
    $null = Send-File $s (Join-Path $seed 'r2-setup-commands.txt')
    Start-Sleep -Seconds 3

    Step 4 'L2: pogoj meritve (M2.1d)'
    Send-Command $s ("rwdiag chunks on {0}" -f $ChunkRadius)
    Check 'ukaz chunks odgovori' (Wait-ForMarker $s 'RWDIAG-CHUNKS' 30)
    $chunks = [regex]::Match((Get-MarkerText $s.Log),
        'RWDIAG-CHUNKS stanje=on chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+) npc=(\d+)')
    Check 'L2: chunki so prisilno nalozeni' ($chunks.Success -and ([int]$chunks.Groups[1].Value -gt 0))
    if ($chunks.Success) {
        Write-Host ("  chunki={0} tiketi={1} zavrnjeni={2} npc={3}" -f `
            $chunks.Groups[1].Value, $chunks.Groups[2].Value, $chunks.Groups[3].Value, $chunks.Groups[4].Value)
        Check ("L2: noben chunk ni bil zavrnjen (zavrnjeni={0})" -f $chunks.Groups[3].Value) ([int]$chunks.Groups[3].Value -eq 0)
        # 6 F + 6 W + 6 P + 3 cilji = 21. Krmilnik se ni spawnan.
        Check ("L2: v svetu je 21 R2 NPC-jev (najdenih {0})" -f $chunks.Groups[4].Value) ([int]$chunks.Groups[4].Value -eq 21)
    }
    Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
    Start-Sleep -Seconds $WarmupSeconds
    Send-Command $s 'rwdiag on'
    Check 'merjenje vklopljeno' (Wait-ForMarker $s 'RWDIAG vklopljen' 30)

    Step 5 'Scenarij'
    # Sele zdaj: skripta krmilnika zacne steti ob spawnu.
    Send-Command $s 'noppes clone spawn R2_Control 1 0,4,-8'
    Check 'L1: krmilnik se je oglasil (R2-INIT)' (Wait-ForMarker $s 'R2-INIT' 60)
    Check 'L3: proge so sestavljene (R2-SETUP)' (Wait-ForMarker $s 'R2-SETUP ' 60)
    $setup = Read-Setup $s.Log
    Check ("L3: vse tri proge so polne (F={0} W={1} P={2})" -f $setup.F, $setup.W, $setup.P) `
        (($setup.F -eq 6) -and ($setup.W -eq 6) -and ($setup.P -eq 6))
    Check ("L3: vsi trije cilji so najdeni (F={0} W={1} P={2})" -f $setup.CiljF, $setup.CiljW, $setup.CiljP) `
        (($setup.CiljF -eq 'da') -and ($setup.CiljW -eq 'da') -and ($setup.CiljP -eq 'da'))

    foreach ($ph in $phases) {
        Check ("faza {0} se je zacela ({1})" -f $ph, $phaseOpis[$ph]) (Wait-ForMarker $s ("R2-{0}-START" -f $ph) $ScenarioTimeoutSec)
        Check ("faza {0} se je koncala" -f $ph) (Wait-ForMarker $s ("R2-{0}-END" -f $ph) $ScenarioTimeoutSec)
    }
    Check 'L1: scenarij je prisel do konca (R2-SUM)' (Wait-ForMarker $s 'R2-SUM' 60)

    Step 6 'Posnetek meritve in ustavitev'
    Send-Command $s 'rwdiag dump m23-r2'
    Check 'posnetek zapisan' (Wait-ForMarker $s 'RWDIAG-DUMP ' 60)
    Send-Command $s 'rwdiag off'
    Send-Command $s 'rwdiag chunks off'
    Check 'ticketi sproscen' (Wait-ForCount $s 'RWDIAG-CHUNKS stanje=off' 1 30)
    Check 'server se je cisto ustavil' (Stop-DevServer $s)

    Step 7 'L1: nobene napake iz moda'
    $log = Get-LogText $s.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'brez "script errored"' (-not $log.Contains('script errored'))

    Step 8 'L4-L11: izid po fazah in progah'
    $result = @{}
    $lines  = @()
    foreach ($ph in $phases) {
        $lines += ("faza {0} - {1}" -f $ph, $phaseOpis[$ph])
        foreach ($lane in $lanes) {
            $sm = @(Read-Samples $s.Log $ph $lane)
            $result["$ph$lane"] = $sm
            $lines += (Format-Lane $ph $lane $sm)
        }
        $lines += ''
    }
    $lines | ForEach-Object { Write-Host $_ }

    foreach ($ph in $phases) {
        foreach ($lane in $lanes) {
            $key = "$ph$lane"
            Check ("L6: faza/proga {0} ima vsaj 20 vzorcev (ima {1})" -f $key, $result[$key].Count) ($result[$key].Count -ge 20)
        }
    }

    # L4: kontrola letenja. Ce pade, meritev ne govori o oviri, ampak o tem, da letenje
    # ne deluje niti po prazni ravnini - drugacna ugotovitev in drugacen popravek.
    $bp = $result['BP']
    if ($bp.Count -gt 0) {
        $maxP = Max-Prevozeno $bp
        $minP = Min-DoCilja $bp
        Check ("L4a: proga P v fazi B prevozi vsaj 12 od 16 blokov (prevozenoMax={0})" -f $maxP) ($maxP -ge 12)
        Check ("L4b: proga P v fazi B pride do cilja, do 3 bloke (doCiljaMin={0})" -f $minP) (($minP -ge 0) -and ($minP -le 3))
    } else {
        Check 'L4: proga P ima vzorce v fazi B' $false
    }

    # L5: kontrola ovire. Ce pade, zid ni ovira in proga F ne dokazuje nicesar; takrat je
    # treba podaljsati zid, ne razlagati izida.
    $cezW = 0
    foreach ($ph in $phases) {
        $c = Max-CezOviro $result["${ph}W"]
        if ($c -gt $cezW) { $cezW = $c }
    }
    Check ("L5: kopenska proga W ne pride cez zid v nobeni fazi (najvec {0}/6)" -f $cezW) ($cezW -eq 0)

    # L9: entitete se morajo tikati. Ce se ne, izid faze ne govori o navigaciji, ampak o
    # tem, da World.updateEntity teh entitet ne poklice - drugacna ugotovitev in drugacen
    # popravek. Pri prvem zagonu (17. 9.) te preverbe ni bilo in faza B je ostala nejasna.
    foreach ($ph in $phases) {
        foreach ($lane in $lanes) {
            $key = "$ph$lane"
            $d = Min-DStarost $result[$key]
            Check ("L9: faza/proga {0} se tika (dStarost min={1}, pricakovano 20)" -f $key, $d) `
                  (($d -ge 15) -and ($d -le 25))
        }
    }

    # L10: preverba samega merilnika gibanja na znanem primeru. Proga P v fazi A je edina,
    # za katero je ze izmerjeno, da se premika; ce je tam gib 0, je pokvarjeno merjenje in
    # ne NPC.
    $gibAP = Max-Gib $result['AP']
    Check ("L10: proga P v fazi A ima izmerjeno gibanje (gibMax={0})" -f $gibAP) ($gibAP -gt 0)

    # Diagnostika zmrznitve: tabela za vse pare, ki se niso premaknili.
    $mrtvi = @()
    foreach ($ph in $phases) {
        foreach ($lane in $lanes) {
            $key = "$ph$lane"
            if ($result[$key].Count -eq 0) { continue }
            if ((Max-Prevozeno $result[$key]) -lt 0.05) {
                $mrtvi += ("  {0}: dStarost(min)={1} gib(max)={2} -> {3}" -f $key,
                    (Min-DStarost $result[$key]), (Max-Gib $result[$key]),
                    $(if ((Min-DStarost $result[$key]) -lt 15) { 'entiteta se ne tika' }
                      elseif ((Max-Gib $result[$key]) -le 0) { 'tika se, moveHelper ne doda gibanja' }
                      else { 'gibanje je, a ga move() poje' }))
            }
        }
    }
    $head += ''
    $head += '## Razsodba o P1'
    $head += ''
    $head += $p1
    if ($mrtvi.Count -gt 0) {
        Write-Host ''
        Write-Host 'Diagnostika: proge, ki se niso premaknile'
        $mrtvi | ForEach-Object { Write-Host $_ }
    }

    # L11 in razsodba o P1. Faza D se od faze B razlikuje v eni sami stvari: NPC zacne pol
    # bloka izven mreze po z. Ce je to vzrok zmrznitve iz zagonov 12:17 in 12:37, mora biti
    # proga P v fazi B ziva in v fazi D mrtva, kopenska proga W pa ziva v obeh - slednja je
    # kontrola, ker EntityMoveHelper praga d3 > 0,5 nima.
    $bpP = Max-Prevozeno $result['BP']
    $dpP = Max-Prevozeno $result['DP']
    $dwW = Max-Prevozeno $result['DW']
    $gdW = Max-Gib $result['DW']
    Check ("L11: kontrola P1 - kopenska proga W se v fazi D premika (prevozenoMax={0}, gibMax={1})" -f $dwW, $gdW) `
          (($dwW -gt 0.05) -and ($gdW -gt 0))

    if ($bpP -ge 12 -and $dpP -lt 0.05) {
        $p1 = "P1 POTRJEN: proga P na mrezi prevozi {0}, izven mreze {1}. Pol bloka odloci." -f $bpP, $dpP
    } elseif ($bpP -ge 12 -and $dpP -ge 12) {
        $p1 = "P1 OVRZEN: proga P se premika na mrezi ({0}) in izven nje ({1}); vzrok zmrznitve je bil nekaj drugega." -f $bpP, $dpP
    } elseif ($bpP -lt 12) {
        $p1 = "P1 NEODLOCEN: proga P se ne premika niti v fazi B ({0}); popravek resetiranja ni zalegel." -f $bpP
    } else {
        $p1 = "P1 NEODLOCEN: B={0} D={1}" -f $bpP, $dpP
    }
    Write-Host ''
    Write-Host $p1

    # L8: cele je poslan za vsako progo in fazo - to je zadetek regexa, ne vrednost.
    $celeOk = $true
    foreach ($ph in $phases) { foreach ($lane in $lanes) { if ($result["$ph$lane"].Count -eq 0) { $celeOk = $false } } }
    Check 'L8: cele=b/N je poslan za vseh devet nizov' $celeOk

    Step 9 'Porocilo'
    $stamp  = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $report = Join-Path $audit ("m23-r2-{0}.md" -f $stamp)
    $head = @()
    $head += ("# M2.3 - reprodukcija R2, zagon {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $head += ''
    $head += 'Scenarij: `docs/scenariji/M2.3-R2.md`. Proge: F = leteci + zid, W = kopenski + zid, P = leteci brez ovire.'
    $head += ''
    $head += ('Merila: {0}' -f $(if ($failures.Count -eq 0) { 'L1-L11 zelena' } else { ("padlo {0}" -f $failures.Count) }))
    $head += ''
    $head += '```'
    $head += $lines
    $head += '```'
    if ($mrtvi.Count -gt 0) {
        $head += ''
        $head += '## Proge, ki se niso premaknile'
        $head += ''
        $head += '```'
        $head += $mrtvi
        $head += '```'
    }
    if ($failures.Count -gt 0) {
        $head += ''
        $head += '## Padle preverbe'
        $head += ''
        $failures | ForEach-Object { $head += ("- {0}" -f $_) }
    }
    Set-Content -Path $report -Value $head -Encoding UTF8
    Write-Host ("  porocilo: {0}" -f $report)

    Step 10 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M2.3 L1-L11 USPESNO: reprodukcija je veljavna. Stevilke zgoraj gredo v docs/meritve/.'
        Write-Host ("Izpis:    {0}" -f $s.Log)
        Write-Host ("Posnetek: {0}" -f $dumps)
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.3 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $s.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.3 NEUSPESNO. Izpis je v audit\m23-r2.log'
    exit 1
}
