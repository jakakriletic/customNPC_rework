# M2.7 - merila kakovosti navigacije, merila N1-N12.
#
# Scenarij in razlaga: docs/scenariji/M2.7-navigacija.md
#
# Sest velicin, izmerjenih na originalu, da se M4.10-M4.13 in M5.6 sploh smejo zaceti:
#   1 delez iskanj, ki vrnejo celo pot     sonda  (RWNAV-POMET delezCelih)
#   2 dolzina poti proti zracni razdalji   sonda  (razmerjeP50 / razmerjeP95)
#   3 cas skupine 8 NPC-jev do cilja       skripta (NAV-CAS prvi/mediana/zadnji)
#   4 razpon skupine na ozkem grlu         skripta (NAV-CAS razponGrloMax)
#   5 us na eno iskanje poti               sonda  (usP50 / usP95 / usMax)
#   6 iskanj poti na tick                  opazovalec (RWNAV-AI naTick)
#
# Dve progi hkrati v istem svetu in istem ticku:
#   proga G  8 kopenskih + zid z enimi vrati  - merjeno ozko grlo
#   proga O  8 kopenskih na odprtem           - kontrola, ali skupina sploh pride
#
# Vrstni red je pomemben: chunki se prisilno nalozijo in ogrejejo PREDEN se spawna
# krmilni NPC, ker njegova skripta zacne steti takoj ob spawnu (M2.1d).
#
# Zagon:
#     .\nav-run.ps1
#     .\nav-run.ps1 -AcceptEula
#     .\nav-run.ps1 -ChunkRadius 3

param([switch]$AcceptEula, [int]$ChunkRadius = 2, [int]$WarmupSeconds = 10,
      [int]$ScenarioTimeoutSec = 180, [int]$SweepRepeats = 3)

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$seed   = Join-Path $root 'dev\testworld'
$dumps  = Join-Path $run 'logs\rwdiag'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

$lanes     = @('G', 'O')
$laneOpis  = @{ G = 'zid z vrati'; O = 'odprto' }
$phases    = @('A', 'B')
$phaseOpis = @{ A = 'navigateTo en sam klic'; B = 'navigateTo osvezen' }

# Prizorisce; iste stevilke so v nav-control.js in nav-setup-commands.txt.
$goalY  = 4
$goalZ  = 78
$goalXG = -10
$goalXO = 15
$npcCount = 16

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
# lazno negativno (M0.7, dnevnik 26). Markerje stejemo samo nad konzolnim kanalom.
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
    $outLog = Join-Path $audit 'm27-nav.log'
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

# "NAV-SETUP progaG=8 progaO=8 vrataX=-10 zidZ=72 ciljZ=78"
function Read-Setup([string]$LogPath) {
    $m = [regex]::Match((Get-MarkerText $LogPath),
        'NAV-SETUP progaG=(\d+) progaO=(\d+) vrataX=(-?\d+) zidZ=(-?\d+) ciljZ=(-?\d+)')
    if (-not $m.Success) { return @{ G = -1; O = -1; VrataX = 0; ZidZ = 0; CiljZ = 0 } }
    return @{
        G = [int]$m.Groups[1].Value; O = [int]$m.Groups[2].Value
        VrataX = [int]$m.Groups[3].Value; ZidZ = [int]$m.Groups[4].Value
        CiljZ = [int]$m.Groups[5].Value
    }
}

# Vse vzorcne vrstice ene faze in proge, kot objekti.
function Read-Samples([string]$LogPath, [string]$Phase, [string]$Lane) {
    $pattern = 'NAV-S faza=' + $Phase + ' tick=(\d+) proga=' + $Lane +
               ' navig=(\d+)/(\d+) cele=(\d+)/\d+ prispelo=(\d+)/\d+' +
               ' cez=(\d+)/\d+ cezDalec=(\d+)/\d+ priGrlu=(\d+)/\d+' +
               ' prevozenoPovp=([\d.]+) prevozenoMax=([\d.]+)' +
               ' doCiljaMin=([\d.]+) doCiljaPovp=([\d.]+)' +
               ' razpon=([\d.]+) razponGrlo=([\d.]+)' +
               ' zastoj=(\d+)/\d+ dStarost=(-?\d+)/(-?\d+) gib=([\d.]+)/([\d.]+)'
    $out = @()
    foreach ($m in [regex]::Matches((Get-MarkerText $LogPath), $pattern)) {
        $out += [pscustomobject]@{
            Tick         = [int]$m.Groups[1].Value
            Navig        = [int]$m.Groups[2].Value
            Skupaj       = [int]$m.Groups[3].Value
            Cele         = [int]$m.Groups[4].Value
            Prispelo     = [int]$m.Groups[5].Value
            Cez          = [int]$m.Groups[6].Value
            CezDalec     = [int]$m.Groups[7].Value
            PriGrlu      = [int]$m.Groups[8].Value
            PrevozenoAvg = [double]$m.Groups[9].Value
            PrevozenoMax = [double]$m.Groups[10].Value
            DoCiljaMin   = [double]$m.Groups[11].Value
            DoCiljaAvg   = [double]$m.Groups[12].Value
            Razpon       = [double]$m.Groups[13].Value
            RazponGrlo   = [double]$m.Groups[14].Value
            Zastoj       = [int]$m.Groups[15].Value
            DStarostMin  = [int]$m.Groups[16].Value
            DStarostMax  = [int]$m.Groups[17].Value
            GibAvg       = [double]$m.Groups[18].Value
            GibMax       = [double]$m.Groups[19].Value
        }
    }
    return $out
}

# "NAV-CAS faza=B proga=G prispelo=8/8 prviTick=180 medianaTick=260 zadnjiTick=430
#  razponGrloMax=7.30 razponMax=9.12 cezDalecMax=0"
function Read-Cas([string]$LogPath, [string]$Phase, [string]$Lane) {
    $m = [regex]::Match((Get-MarkerText $LogPath),
        'NAV-CAS faza=' + $Phase + ' proga=' + $Lane +
        ' prispelo=(\d+)/(\d+) prviTick=(-?\d+) medianaTick=(-?\d+) zadnjiTick=(-?\d+)' +
        ' razponGrloMax=([\d.]+) razponMax=([\d.]+) cezDalecMax=(\d+)')
    if (-not $m.Success) { return $null }
    return [pscustomobject]@{
        Prispelo    = [int]$m.Groups[1].Value
        Skupaj      = [int]$m.Groups[2].Value
        Prvi        = [int]$m.Groups[3].Value
        Mediana     = [int]$m.Groups[4].Value
        Zadnji      = [int]$m.Groups[5].Value
        RazponGrlo  = [double]$m.Groups[6].Value
        RazponMax   = [double]$m.Groups[7].Value
        CezDalecMax = [int]$m.Groups[8].Value
    }
}

# Skupni del vrstice sonde; isti zapis ima pometanje (RWNAV-POMET) in skupna sonda
# (RWNAV-SONDA), ker ju pise ista koda (NavProbe.markerLine).
#
# Namenoma funkcija in ne spremenljivka: ce bi bila spremenljivka in bi jo kdo preimenoval
# ali premaknil za funkcije, ki jo uporabljajo, bi se vzorec tiho sestavil iz praznega niza,
# regex ne bi nasel nicesar in merilo bi padlo z 'sonda ni pometla' namesto z napako v
# skripti. Klic neobstojece funkcije pade takoj in pove, kaj je narobe.
function Get-SondaVzorec {
    return (
    ' pometanj=(\d+) cilj=(\S+) iskanj=(\d+) celih=(\d+) delnih=(\d+) brezPoti=(\d+)' +
    ' preblizu=(\d+) ponovitev=(\d+) delezCelih=([\d.]+)' +
    ' razmerjeN=(\d+) razmerjeP50=([\d.]+) razmerjeP95=([\d.]+)' +
    ' dosegN=(\d+) dosegP50=([\d.]+) dosegP05=([\d.]+)' +
    ' usP50=([\d.]+) usP95=([\d.]+) usMax=([\d.]+)')
}

function New-Sonda($m) {
    return [pscustomobject]@{
        Pometanj    = [int]$m.Groups[1].Value
        Cilj        = $m.Groups[2].Value
        Iskanj      = [int]$m.Groups[3].Value
        Celih       = [int]$m.Groups[4].Value
        Delnih      = [int]$m.Groups[5].Value
        BrezPoti    = [int]$m.Groups[6].Value
        Preblizu    = [int]$m.Groups[7].Value
        Ponovitev   = [int]$m.Groups[8].Value
        DelezCelih  = [double]$m.Groups[9].Value
        RazmerjeN   = [int]$m.Groups[10].Value
        RazmerjeP50 = [double]$m.Groups[11].Value
        RazmerjeP95 = [double]$m.Groups[12].Value
        DosegN      = [int]$m.Groups[13].Value
        DosegP50    = [double]$m.Groups[14].Value
        DosegP05    = [double]$m.Groups[15].Value
        UsP50       = [double]$m.Groups[16].Value
        UsP95       = [double]$m.Groups[17].Value
        UsMax       = [double]$m.Groups[18].Value
        Npc         = -1
        Preskocenih = -1
        NaTleh      = -1
        Predpona    = '-'
        ChunkiForced = -1
    }
}

# Vsa pometanja po vrsti, kot so bila pognana.
function Read-Pometi([string]$LogPath) {
    $pattern = 'RWNAV-POMET' + (Get-SondaVzorec) +
               ' npc=(\d+) preskocenih=(\d+) naTleh=(\d+) predpona=(\S+) chunkiForced=(\d+)'
    $out = @()
    foreach ($m in [regex]::Matches((Get-MarkerText $LogPath), $pattern)) {
        $s = New-Sonda $m
        $s.Npc          = [int]$m.Groups[19].Value
        $s.Preskocenih  = [int]$m.Groups[20].Value
        $s.NaTleh       = [int]$m.Groups[21].Value
        $s.Predpona     = $m.Groups[22].Value
        $s.ChunkiForced = [int]$m.Groups[23].Value
        $out += $s
    }
    return $out
}

# Zadnja vrstica skupne sonde: vsota vseh pometanj te meritve.
function Read-Sonda([string]$LogPath) {
    $ms = [regex]::Matches((Get-MarkerText $LogPath), 'RWNAV-SONDA' + (Get-SondaVzorec))
    if ($ms.Count -eq 0) { return $null }
    return (New-Sonda $ms[$ms.Count - 1])
}

# "RWNAV-AI novihPoti=16 tickov=40 naTick=0.400 naTickP95=2 naTickMax=2
#  navigirajoP50=16 navigirajoMax=16"
function Read-NavAi([string]$LogPath) {
    $ms = [regex]::Matches((Get-MarkerText $LogPath),
        'RWNAV-AI novihPoti=(\d+) tickov=(\d+) naTick=([\d.]+) naTickP95=(\d+) naTickMax=(\d+)' +
        ' navigirajoP50=(\d+) navigirajoMax=(\d+)')
    if ($ms.Count -eq 0) { return $null }
    $m = $ms[$ms.Count - 1]
    return [pscustomobject]@{
        NovihPoti    = [int]$m.Groups[1].Value
        Tickov       = [int]$m.Groups[2].Value
        NaTick       = [double]$m.Groups[3].Value
        NaTickP95    = [int]$m.Groups[4].Value
        NaTickMax    = [int]$m.Groups[5].Value
        NavigirajoP50 = [int]$m.Groups[6].Value
        NavigirajoMax = [int]$m.Groups[7].Value
    }
}

# Vrstica RWDIAG-CHUNKS ima dve obliki: ob uspehu "stanje=on ... obroc=N zavrnjeni=N npc=N",
# ob zavrnitvi pa "stanje=off chunki=0 tiketi=0 npc=0 razlog=...". Bralnik, ki pozna samo
# prvo, ob neuspehu ne izpise nicesar in merilo pove "chunki niso nalozeni", ne pa tistega,
# kar dejansko manjka: NPC-jev v svetu. Zagon 17. 9. je bil prav tak.
function Read-Chunks([string]$LogPath) {
    $t = Get-MarkerText $LogPath
    $m = [regex]::Match($t, 'RWDIAG-CHUNKS stanje=(\w+) chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+) npc=(\d+)')
    if ($m.Success) {
        return [pscustomobject]@{ Stanje = $m.Groups[1].Value; Chunki = [int]$m.Groups[2].Value
            Tiketi = [int]$m.Groups[3].Value; Zavrnjeni = [int]$m.Groups[4].Value; Npc = [int]$m.Groups[5].Value
            Razlog = '' }
    }
    $m = [regex]::Match($t, 'RWDIAG-CHUNKS stanje=(\w+) chunki=(\d+) tiketi=(\d+) npc=(\d+) razlog=(.*)')
    if ($m.Success) {
        return [pscustomobject]@{ Stanje = $m.Groups[1].Value; Chunki = [int]$m.Groups[2].Value
            Tiketi = [int]$m.Groups[3].Value; Zavrnjeni = -1; Npc = [int]$m.Groups[4].Value
            Razlog = $m.Groups[5].Value.Trim() }
    }
    return $null
}

# Koliko blokov je postavil 'fill'. Vanilla odgovori "N blocks filled", ob nenalozenem
# obmocju pa "Cannot place blocks outside of the world" - in prav to se je 17. 9. zgodilo
# zidu, medtem ko je scenarij mirno tekel naprej proti prazni sceni.
function Read-FillBlocks([string]$LogPath) {
    $max = 0
    foreach ($m in [regex]::Matches((Get-MarkerText $LogPath), '(\d+) blocks filled')) {
        $n = [int]$m.Groups[1].Value
        if ($n -gt $max) { $max = $n }
    }
    return $max
}

# Najmanjsi prirastek starosti med dvema vzorcema. Prvi vzorec faze se izpusti: njegov
# prirastek meri razmik med fazama, ne merilnega intervala. -1 pomeni 'ni podatka'.
function Min-DStarost($Samples) {
    if ($Samples.Count -lt 2) { return -1 }
    $min = [int]::MaxValue
    for ($i = 1; $i -lt $Samples.Count; $i++) {
        if ($Samples[$i].DStarostMin -lt $min) { $min = $Samples[$i].DStarostMin }
    }
    return $min
}

function Max-Of($Samples, [string]$Field) {
    $max = 0.0
    foreach ($s in $Samples) { if ($s.$Field -gt $max) { $max = $s.$Field } }
    return $max
}

function Format-Lane([string]$Phase, [string]$Lane, $Samples) {
    if ($Samples.Count -eq 0) { return ("  faza {0} proga {1}: NI VZORCEV" -f $Phase, $Lane) }
    $last = $Samples[$Samples.Count - 1]
    return ("  faza {0} proga {1} ({2,-12}): vzorcev={3,2} | konec: navig={4}/{5} cele={6}/{5} prispelo={7}/{5} cez={8}/{5} zastoj={9}/{5} prevozeno(povp/max)={10}/{11} doCilja(min/povp)={12}/{13} razpon={14} razponGrlo(max)={15} | dStarost(min)={16} gib(max)={17}" -f `
        $Phase, $Lane, $laneOpis[$Lane], $Samples.Count,
        $last.Navig, $last.Skupaj, $last.Cele, $last.Prispelo, $last.Cez, $last.Zastoj,
        $last.PrevozenoAvg, $last.PrevozenoMax, $last.DoCiljaMin, $last.DoCiljaAvg,
        $last.Razpon, (Max-Of $Samples 'RazponGrlo'),
        (Min-DStarost $Samples), (Max-Of $Samples 'GibMax'))
}

function Format-Sonda($S) {
    if ($null -eq $S) { return '  (ni vrstice sonde)' }
    return ("  cilj={0,-10} npc={1,2} naTleh={2,2} | iskanj={3,2} celih={4,2} delnih={5,2} brezPoti={6,2} delez={7:N3} | razmerje p50/p95={8:N3}/{9:N3} (n={10}) | doseg p50/p05={11:N3}/{12:N3} | us p50/p95/max={13:N1}/{14:N1}/{15:N1}" -f `
        $S.Cilj, $S.Npc, $S.NaTleh, $S.Iskanj, $S.Celih, $S.Delnih, $S.BrezPoti,
        $S.DelezCelih, $S.RazmerjeP50, $S.RazmerjeP95, $S.RazmerjeN,
        $S.DosegP50, $S.DosegP05, $S.UsP50, $S.UsP95, $S.UsMax)
}

function Invoke-Sweep($srv, [string]$Lane) {
    $gx = if ($Lane -eq 'G') { $goalXG } else { $goalXO }
    $prefix = if ($Lane -eq 'G') { 'NAV_WalkG' } else { 'NAV_WalkO' }
    Send-Command $srv ("rwdiag nav {0} {1} {2} {3} 32 {4}" -f $gx, $goalY, $goalZ, $SweepRepeats, $prefix)
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA in fixture'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\nav-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # Merilni kanal scenarija je '/say' iz skripte krmilnika. NPCWrapper.executeCommand
    # vrze CustomNPCsException, ce command bloki niso vklopljeni; brez te preverbe bi
    # scenarij padel brez ene same vrstice NAV-*, kar izgleda kot okvara skripte.
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
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa NAV-*)'
        }
    } else {
        Write-Host '  ! dev\run\server.properties se ne obstaja; ustvari ga prvi zagon.'
    }

    # Fixture se kopirajo v dev\run\world, server pa nalozi svet, ki ga imenuje level-name
    # v dev\run\server.properties. Ce se to dvoje razide, server zazene DRUG svet in
    # scenarij caka na markerje, ki jih ne bo (17. 9., dnevnik 32).
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

    $srcClones = Join-Path $seed 'customnpcs\clones\1'
    $dstClones = Join-Path $run  'world\customnpcs\clones\1'
    New-Item -ItemType Directory -Force -Path $dstClones | Out-Null
    $copied = 0
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'NAV_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("tri NAV fixture datoteke so v svetu (kopiranih {0})" -f $copied) ($copied -eq 3)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones" }

    Step 2 'Zagon serverja'
    $s = Start-DevServer
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    Step 3 'Prizorisce in fixture NPC-ji'
    $null = Send-File $s (Join-Path $seed 'nav-setup-commands.txt')
    Start-Sleep -Seconds 3

    Step 4 'N2: prizorisce in pogoj meritve (M2.1d)'
    # Zid najprej: ce ga ni, sta ozko grlo in vse, kar iz njega sledi, izmisljena.
    $filled = Read-FillBlocks $s.Log
    Check ("N2: zid je postavljen ({0} blokov)" -f $filled) ($filled -ge 100)
    if ($filled -lt 100) {
        throw "Zid ni bil postavljen. Ce log pravi 'Cannot place blocks outside of the world', obmocje ni nalozeno - preveri, da je v nav-setup-commands.txt ukaz 'setworldspawn 0 4 0'."
    }
    Send-Command $s ("rwdiag chunks on {0}" -f $ChunkRadius)
    Check 'ukaz chunks odgovori' (Wait-ForMarker $s 'RWDIAG-CHUNKS' 30)
    $chunks = Read-Chunks $s.Log
    if ($null -ne $chunks) {
        Write-Host ("  stanje={0} chunki={1} tiketi={2} zavrnjeni={3} npc={4} {5}" -f `
            $chunks.Stanje, $chunks.Chunki, $chunks.Tiketi, $chunks.Zavrnjeni, $chunks.Npc, $chunks.Razlog)
        # Brez NPC-jev nima smisla cakati na scenarij: krmilnik se ne bo oglasil in vsaka
        # faza bo iztekla po 180 s. Zagon 17. 9. je tako porabil deset minut za nic.
        Check ("N2: v svetu je {0} NAV NPC-jev (najdenih {1})" -f $npcCount, $chunks.Npc) ($chunks.Npc -eq $npcCount)
        if ($chunks.Npc -eq 0) {
            throw "V svetu ni NPC-jev - 'noppes clone spawn' ni nicesar postavil. Najpogostejsi vzrok: obmocje scenarija ni nalozeno (manjka 'setworldspawn 0 4 0') ali fixture NAV_*.json niso v dev\run\world\customnpcs\clones\1."
        }
        Check 'N2: chunki so prisilno nalozeni' ($chunks.Chunki -gt 0)
        Check ("N2: noben chunk ni bil zavrnjen (zavrnjeni={0})" -f $chunks.Zavrnjeni) ($chunks.Zavrnjeni -le 0)
    } else {
        Check 'N2: vrstica RWDIAG-CHUNKS je berljiva' $false
    }
    Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
    Start-Sleep -Seconds $WarmupSeconds
    Send-Command $s 'rwdiag on'
    Check 'merjenje vklopljeno' (Wait-ForMarker $s 'RWDIAG vklopljen' 30)

    Step 5 'Sonda na startni crti (velicine 1, 2, 5)'
    # Prvo pometanje mora biti PRED krmilnikom: takrat vsi NPC-ji stojijo na startu in
    # merjene poti so primerljive med sabo. Po zacetku scenarija vsak stoji drugje.
    foreach ($lane in $lanes) { Invoke-Sweep $s $lane }
    Check 'sonda je odgovorila dvakrat' (Wait-ForCount $s 'RWNAV-POMET ' 2 60)

    Step 6 'Scenarij, faza A'
    Send-Command $s 'noppes clone spawn NAV_Control 1 0,4,56'
    Check 'N1: krmilnik se je oglasil (NAV-INIT)' (Wait-ForMarker $s 'NAV-INIT' 60)
    Check 'N1: progi sta sestavljeni (NAV-SETUP)' (Wait-ForMarker $s 'NAV-SETUP ' 60)
    $setup = Read-Setup $s.Log
    Check ("N3: obe progi sta polni (G={0} O={1})" -f $setup.G, $setup.O) `
        (($setup.G -eq 8) -and ($setup.O -eq 8))
    Check ("N3: prizorisce se ujema s skripto (vrataX={0} zidZ={1} ciljZ={2})" -f $setup.VrataX, $setup.ZidZ, $setup.CiljZ) `
        (($setup.VrataX -eq $goalXG) -and ($setup.CiljZ -eq $goalZ))
    Check 'faza A se je zacela' (Wait-ForMarker $s 'NAV-A-START' $ScenarioTimeoutSec)
    Check 'faza A se je koncala' (Wait-ForMarker $s 'NAV-A-END' $ScenarioTimeoutSec)

    Step 7 'Sonda tam, kjer je faza A obstala'
    # Drugo pometanje je najbolj poveden podatek za M4.10: NPC-ji stojijo na koncu delne
    # poti in vprasanje je, ali bi novo iskanje od tam naslo celo pot.
    foreach ($lane in $lanes) { Invoke-Sweep $s $lane }
    Check 'sonda je odgovorila stirikrat' (Wait-ForCount $s 'RWNAV-POMET ' 4 60)

    Step 8 'Scenarij, faza B'
    Check 'faza B se je zacela' (Wait-ForMarker $s 'NAV-B-START' $ScenarioTimeoutSec)
    Check 'faza B se je koncala' (Wait-ForMarker $s 'NAV-B-END' $ScenarioTimeoutSec)
    Check 'N1: scenarij je prisel do konca (NAV-SUM)' (Wait-ForMarker $s 'NAV-SUM' 60)

    Step 9 'Posnetek meritve in ustavitev'
    Send-Command $s 'rwdiag dump m27-nav'
    Check 'posnetek zapisan' (Wait-ForMarker $s 'RWDIAG-DUMP ' 60)
    Send-Command $s 'rwdiag off'
    Send-Command $s 'rwdiag chunks off'
    Check 'ticketi sproscen' (Wait-ForCount $s 'RWDIAG-CHUNKS stanje=off' 1 30)
    Check 'server se je cisto ustavil' (Stop-DevServer $s)

    Step 10 'N1: nobene napake iz moda'
    $log = Get-LogText $s.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'brez "script errored"' (-not $log.Contains('script errored'))

    Step 11 'N4-N7: izid po fazah in progah'
    $result = @{}
    $cas    = @{}
    $lines  = @()
    foreach ($ph in $phases) {
        $lines += ("faza {0} - {1}" -f $ph, $phaseOpis[$ph])
        foreach ($lane in $lanes) {
            $sm = @(Read-Samples $s.Log $ph $lane)
            $result["$ph$lane"] = $sm
            $cas["$ph$lane"] = Read-Cas $s.Log $ph $lane
            $lines += (Format-Lane $ph $lane $sm)
        }
        $lines += ''
    }
    $lines | ForEach-Object { Write-Host $_ }

    foreach ($ph in $phases) {
        foreach ($lane in $lanes) {
            $key = "$ph$lane"
            Check ("N4: faza/proga {0} ima vsaj 20 vzorcev (ima {1})" -f $key, $result[$key].Count) `
                ($result[$key].Count -ge 20)
            # N5: entitete se morajo tikati. Ce se ne, izid ne govori o navigaciji, ampak
            # o tem, da World.updateEntity teh entitet ne poklice (pojav P1 iz M2.3).
            $d = Min-DStarost $result[$key]
            Check ("N5: faza/proga {0} se tika (dStarost min={1}, pricakovano 20)" -f $key, $d) `
                (($d -ge 15) -and ($d -le 25))
        }
    }

    # N6: kontrola. Ce skupina na odprtem ne pride 14 blokov dalec, meritev ne govori o
    # ozkem grlu, ampak o tem, da skupinska navigacija sploh ne deluje.
    $bo = $cas['BO']
    if ($null -ne $bo) {
        Check ("N6: kontrolna proga O v fazi B pride do cilja (prispelo={0}/{1})" -f $bo.Prispelo, $bo.Skupaj) `
            ($bo.Prispelo -ge 6)
    } else {
        Check 'N6: proga O ima vrstico NAV-CAS v fazi B' $false
    }

    # N7: grlo mora biti edina pot. Ce je kdo prisel za zid dalec od vrat, je zid obsel
    # in stevilki 3 in 4 ne govorita o ozkem grlu.
    foreach ($ph in $phases) {
        $c = $cas["${ph}G"]
        if ($null -ne $c) {
            Check ("N7: v fazi {0} nihce ni obsel zidu (cezDalecMax={1})" -f $ph, $c.CezDalecMax) `
                ($c.CezDalecMax -eq 0)
        }
    }

    Step 12 'N8-N12: sonda in opazovalec'
    $pometi = @(Read-Pometi $s.Log)
    $sonda  = Read-Sonda $s.Log
    $navAi  = Read-NavAi $s.Log
    Check ("N8: sonda je pometla stirikrat (najdenih {0})" -f $pometi.Count) ($pometi.Count -eq 4)
    foreach ($p in $pometi) {
        Write-Host (Format-Sonda $p)
        # N9: navzkrizna preverba samega merilnika. Iskanje mora pasti v natanko eno
        # vedro; ce se vedra ne sestejejo, je pokvarjena sonda in ne navigacija.
        Check ("N9: vedra se sestejejo za {0} (celih {1} + delnih {2} + brezPoti {3} = iskanj {4})" -f `
            $p.Predpona, $p.Celih, $p.Delnih, $p.BrezPoti, $p.Iskanj) `
            (($p.Celih + $p.Delnih + $p.BrezPoti) -eq $p.Iskanj)
        Check ("N8: pometanje {0} je merilo 8 NPC-jev na tleh (npc={1} naTleh={2} chunki={3})" -f `
            $p.Predpona, $p.Npc, $p.NaTleh, $p.ChunkiForced) `
            (($p.Npc -eq 8) -and ($p.NaTleh -eq 8) -and ($p.ChunkiForced -gt 0))
        Check ("N12: pometanje {0} ima izmerjen cas (usP50={1})" -f $p.Predpona, $p.UsP50) ($p.UsP50 -gt 0)
    }

    # N10: znan primer. Na odprtem, 14 blokov, znotraj NpcNavRange (32) mora skoraj vsako
    # iskanje vrniti celo pot. Ce ne, je pokvarjena sonda ali prizorisce - ne pathfinder.
    $oStart = $null
    foreach ($p in $pometi) { if ($p.Predpona -eq 'NAV_WalkO') { $oStart = $p; break } }
    if ($null -ne $oStart) {
        Check ("N10: na odprtem s startne crte je skoraj vsaka pot cela (delez={0})" -f $oStart.DelezCelih) `
            ($oStart.DelezCelih -ge 0.9)
    } else {
        Check 'N10: prvo pometanje proge O obstaja' $false
    }

    if ($null -ne $sonda) { Write-Host ''; Write-Host '  skupna sonda:'; Write-Host (Format-Sonda $sonda) }
    Check 'N8: skupna vrstica sonde je v posnetku' ($null -ne $sonda)

    # N11: sesta velicina. Brez nje je 'iskanj na tick' nic po pomoti in ne po meritvi.
    if ($null -ne $navAi) {
        Write-Host ("  opazovalec: novihPoti={0} cez {1} tickov, naTick={2} (p95={3}, max={4}), navigira p50={5}" -f `
            $navAi.NovihPoti, $navAi.Tickov, $navAi.NaTick, $navAi.NaTickP95, $navAi.NaTickMax, $navAi.NavigirajoP50)
        Check ("N11: opazovalec je videl vsaj eno novo pot (novihPoti={0})" -f $navAi.NovihPoti) ($navAi.NovihPoti -gt 0)
    } else {
        Check 'N11: vrstica RWNAV-AI je v posnetku' $false
    }

    Step 13 'Sest velicin (izhodiscna tabela M2.7)'
    $tabela = @()
    $tabela += 'velicina                                  proga G (grlo)        proga O (odprto)'
    $tabela += '---------------------------------------------------------------------------------'
    $pG = @($pometi | Where-Object { $_.Predpona -eq 'NAV_WalkG' })
    $pO = @($pometi | Where-Object { $_.Predpona -eq 'NAV_WalkO' })
    function Val($arr, [int]$i, [string]$field, [string]$fmt) {
        if ($arr.Count -le $i) { return '-' }
        return ($fmt -f $arr[$i].$field)
    }
    $tabela += ('1 delez celih poti (start)                {0,-21} {1}' -f (Val $pG 0 'DelezCelih' '{0:N3}'), (Val $pO 0 'DelezCelih' '{0:N3}'))
    $tabela += ('1 delez celih poti (po fazi A)            {0,-21} {1}' -f (Val $pG 1 'DelezCelih' '{0:N3}'), (Val $pO 1 'DelezCelih' '{0:N3}'))
    $tabela += ('2 razmerje dolzine p50 (start)            {0,-21} {1}' -f (Val $pG 0 'RazmerjeP50' '{0:N3}'), (Val $pO 0 'RazmerjeP50' '{0:N3}'))
    $tabela += ('2 razmerje dolzine p95 (start)            {0,-21} {1}' -f (Val $pG 0 'RazmerjeP95' '{0:N3}'), (Val $pO 0 'RazmerjeP95' '{0:N3}'))
    $tabela += ('5 us na iskanje p50 (start)               {0,-21} {1}' -f (Val $pG 0 'UsP50' '{0:N1}'), (Val $pO 0 'UsP50' '{0:N1}'))
    $tabela += ('5 us na iskanje p95 (start)               {0,-21} {1}' -f (Val $pG 0 'UsP95' '{0:N1}'), (Val $pO 0 'UsP95' '{0:N1}'))
    foreach ($ph in $phases) {
        $g = $cas["${ph}G"]; $o = $cas["${ph}O"]
        $gt = if ($null -eq $g) { '-' } else { ("{0}/{1} ob {2}/{3}/{4}" -f $g.Prispelo, $g.Skupaj, $g.Prvi, $g.Mediana, $g.Zadnji) }
        $ot = if ($null -eq $o) { '-' } else { ("{0}/{1} ob {2}/{3}/{4}" -f $o.Prispelo, $o.Skupaj, $o.Prvi, $o.Mediana, $o.Zadnji) }
        $tabela += ('3 prispelo ob prvi/mediana/zadnji, faza {0}  {1,-19} {2}' -f $ph, $gt, $ot)
    }
    foreach ($ph in $phases) {
        $g = $cas["${ph}G"]; $o = $cas["${ph}O"]
        $gt = if ($null -eq $g) { '-' } else { ('{0:N2}' -f $g.RazponGrlo) }
        $ot = if ($null -eq $o) { '-' } else { ('{0:N2}' -f $o.RazponGrlo) }
        $tabela += ('4 razpon skupine pri grlu, faza {0}         {1,-21} {2}' -f $ph, $gt, $ot)
    }
    if ($null -ne $navAi) {
        $tabela += ('6 iskanj na tick (obe progi skupaj)       {0:N3} (p95 {1}, max {2}), navigira p50 {3}' -f `
            $navAi.NaTick, $navAi.NaTickP95, $navAi.NaTickMax, $navAi.NavigirajoP50)
    }
    $tabela | ForEach-Object { Write-Host $_ }

    Step 14 'Porocilo'
    $stamp  = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $report = Join-Path $audit ("m27-nav-{0}.md" -f $stamp)
    $head = @()
    $head += ("# M2.7 - merila kakovosti navigacije, zagon {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $head += ''
    $head += 'Scenarij: `docs/scenariji/M2.7-navigacija.md`. Progi: G = zid z enimi vrati, O = odprto.'
    $head += ''
    $head += ('Merila: {0}' -f $(if ($failures.Count -eq 0) { 'N1-N12 zelena' } else { ("padlo {0}" -f $failures.Count) }))
    $head += ''
    $head += '## Sest velicin'
    $head += ''
    $head += '```'
    $head += $tabela
    $head += '```'
    $head += ''
    $head += '## Po fazah in progah'
    $head += ''
    $head += '```'
    $head += $lines
    $head += '```'
    $head += ''
    $head += '## Pometanja sonde'
    $head += ''
    $head += '```'
    foreach ($p in $pometi) { $head += (Format-Sonda $p) }
    if ($null -ne $sonda) { $head += ''; $head += 'skupna sonda:'; $head += (Format-Sonda $sonda) }
    $head += '```'
    if ($failures.Count -gt 0) {
        $head += ''
        $head += '## Padle preverbe'
        $head += ''
        $failures | ForEach-Object { $head += ("- {0}" -f $_) }
    }
    Set-Content -Path $report -Value $head -Encoding UTF8
    Write-Host ("  porocilo: {0}" -f $report)

    Step 15 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M2.7 N1-N12 USPESNO: izhodiscna tabela je veljavna. Stevilke gredo v docs/meritve/.'
        Write-Host ("Izpis:    {0}" -f $s.Log)
        Write-Host ("Posnetek: {0}" -f $dumps)
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.7 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $s.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.7 NEUSPESNO. Izpis je v audit\m27-nav.log'
    exit 1
}
