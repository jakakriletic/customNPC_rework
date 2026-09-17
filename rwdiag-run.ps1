# M2.1 - skriptiran preizkus instrumentacije (merila D4-D7).
#
# Zakaj skriptirano: rocno tipkanje ukazov v gradle konzolo je 15. 9. ze dvakrat
# tiho odpovedalo - ukaz ni prisel do serverja, v logu ni bilo nicesar in izgledalo
# je kot napaka moda. Isti mehanizem kot testworld-run.ps1 in smoke-server.ps1:
# ukazi gredo na standardni vhod procesa, izpis pa v audit\m21-rwdiag.log.
#
# M2.1d: pred merjenjem skripta prisilno nalozi chunke z NPC-ji. Brez tega vanilla neha
# posodabljati entitete 300 tickov po nalaganju sveta (WorldServer.updateEntities, :628-644)
# in meritev meri prvih 15 sekund, nato prazen tek. Glej docs/scenariji/M2.1-diag.md.
#
# Zagon:
#     .\rwdiag-run.ps1                 # 60 s merjenja, chunki prisilno nalozeni (obroc 1)
#     .\rwdiag-run.ps1 -Seconds 180    # daljse merjenje
#     .\rwdiag-run.ps1 -ChunkRadius 2  # sirsi obroc, ce NPC-ji veliko tavajo
#     .\rwdiag-run.ps1 -NoChunks       # namenoma brez pogoja, za primerjavo s starimi meritvami
#     .\rwdiag-run.ps1 -WarmupSeconds 0 # brez ogrevanja, ce hoces meriti tudi nalaganje chunkov
#     .\rwdiag-run.ps1 -AcceptEula     # prvic, ce dev\run\eula.txt se ni sprejet

param([int]$Seconds = 60, [switch]$AcceptEula, [int]$ChunkRadius = 1, [switch]$NoChunks,
      [int]$WarmupSeconds = 10)

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$dumps  = Join-Path $run 'logs\rwdiag'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

# Vsak odgovor ukaza /rwdiag se v logu pojavi DVAKRAT: enkrat kot konzolni odgovor
# ([minecraft/DedicatedServer]) in enkrat prek LogWriterja ([FINE/CustomNPCs]).
# Steti markerje nad celim logom zato pomeni dvojne zadetke: 17. 9. je D7b padel
# lazno, ker je bil "drugi posnetek" v resnici dvojnik prvega. Markerje odslej
# stejemo samo nad konzolnim kanalom.
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

function Start-DevServer([string]$Tag) {
    $outLog = Join-Path $audit "m21-rwdiag.log"
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName         = $env:ComSpec
    $psi.Arguments        = "/c gradlew.bat runServer --offline --no-daemon --console=plain > `"$outLog`" 2>&1"
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute  = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow   = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    return [pscustomobject]@{ Proc = $proc; Log = $outLog; Tag = $Tag }
}

function Send-Command($srv, [string]$Cmd) {
    Write-Host ("  > {0}" -f $Cmd)
    $srv.Proc.StandardInput.WriteLine($Cmd)
    $srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 400
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

# "RWDIAG-OK ticki=1234 npc=5678" -> [int[]](ticki, npc); ce ni zadetka, (-1,-1)
function Read-OkMarker([string]$LogPath, [int]$Nth) {
    $m = [regex]::Matches((Get-MarkerText $LogPath), 'RWDIAG-OK ticki=(\d+) npc=(\d+)')
    if ($m.Count -lt $Nth) { return @(-1, -1) }
    $hit = $m[$Nth - 1]
    return @([int]$hit.Groups[1].Value, [int]$hit.Groups[2].Value)
}

# "RWDIAG-CHUNKS stanje=on chunki=12 tiketi=1 obroc=1 zavrnjeni=0 npc=8"
#   -> [int[]](chunki, tiketi, npc, zavrnjeni); ce ni zadetka, (-1,-1,-1,-1)
function Read-ChunkMarker([string]$LogPath, [int]$Nth) {
    # Get-MarkerText, ne Get-LogText: odgovor ukaza je v logu dvakrat (glej Get-MarkerText).
    $m = [regex]::Matches((Get-MarkerText $LogPath),
        'RWDIAG-CHUNKS stanje=\w+ chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+)(?: npc=(\d+))?')
    if ($m.Count -eq 0) { return @(-1, -1, -1, -1) }
    # $Nth = 0 pomeni "zadnji zadetek"; sicer n-ti po vrsti.
    if ($Nth -le 0) { $hit = $m[$m.Count - 1] }
    elseif ($m.Count -lt $Nth) { return @(-1, -1, -1, -1) }
    else { $hit = $m[$Nth - 1] }
    $npc = if ($hit.Groups[4].Success) { [int]$hit.Groups[4].Value } else { -1 }
    return @([int]$hit.Groups[1].Value, [int]$hit.Groups[2].Value, $npc, [int]$hit.Groups[3].Value)
}

# "diag.chunks.added   3   0.002   -   -" -> 3; ce stevca ni v tabeli, je 0.
function Read-Counter([string]$LogPath, [string]$Name) {
    $pattern = '(?m)^\s*' + [regex]::Escape($Name) + '\s+(\d+)\s+'
    $m = [regex]::Matches((Get-LogText $LogPath), $pattern)
    if ($m.Count -eq 0) { return 0 }
    return [long]$m[$m.Count - 1].Groups[1].Value
}

# Prebere vrstico porazdelitve iz tabele posnetka v logu:
#   "npc.per.tick   1229   0   1   0   8   8   8" -> [long[]](n, min, povp, p50, p95, p99, max)
function Read-Distribution([string]$LogPath, [string]$Name) {
    $pattern = '(?m)^\s*' + [regex]::Escape($Name) + '\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*$'
    $m = [regex]::Matches((Get-LogText $LogPath), $pattern)
    if ($m.Count -eq 0) { return $null }
    $hit = $m[$m.Count - 1]
    return @([long]$hit.Groups[1].Value, [long]$hit.Groups[2].Value, [long]$hit.Groups[3].Value,
             [long]$hit.Groups[4].Value, [long]$hit.Groups[5].Value, [long]$hit.Groups[6].Value,
             [long]$hit.Groups[7].Value)
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\rwdiag-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    } else {
        Write-Host '  EULA je ze sprejeta.'
    }

    # Stare posnetke pobrisemo, da merilo D6 ne more biti zeleno zaradi prejsnjega zagona.
    Step 2 'Priprava'
    if (Test-Path $dumps) { Remove-Item (Join-Path $dumps '*') -Force -ErrorAction SilentlyContinue }
    Write-Host ("  mapa posnetkov: {0}" -f $dumps)
    Write-Host ("  trajanje merjenja: {0} s" -f $Seconds)

    Step 3 'Zagon serverja'
    $s = Start-DevServer 'm21'
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    # D4: svet mora tecti kot prej. Testni svet ima skripto na T_Scripted, ki se
    # oglasi ob nalaganju - ce je z novim jarjem kaj narobe, se to pozna tu.
    Check 'testni svet in scripting tecejo (TW-SCRIPT-OK)' (Wait-ForMarker $s 'TW-SCRIPT-OK' 120)

    Step 4 'D7a: pred vklopom mora biti izklopljeno'
    Send-Command $s 'rwdiag status'
    Check 'status odgovori' (Wait-ForMarker $s 'RWDIAG stanje=' 30)
    Check 'privzeto je izklopljeno' ((Get-LogText $s.Log).Contains('RWDIAG stanje=off'))

    # M2.1d: brez tega meritev po 300 tickih meri prazen tek. Chunki se prisilno nalozijo
    # PRED vklopom merjenja, da je pogoj izpolnjen ze ob prvem vzorcu.
    Step '4b' 'C1-C2: pogoj meritve (prisilno nalozeni chunki)'
    if (-not $NoChunks) {
        Send-Command $s ("rwdiag chunks on {0}" -f $ChunkRadius)
        Check 'ukaz chunks odgovori' (Wait-ForMarker $s 'RWDIAG-CHUNKS' 30)
        $chunks = Read-ChunkMarker $s.Log 1
        Check ("C1: chunki so prisilno nalozeni (chunki={0}, tiketi={1}, npc={2})" -f $chunks[0], $chunks[1], $chunks[2]) ($chunks[0] -gt 0)
        Check ("C2: noben chunk ni bil zavrnjen (zavrnjeni={0})" -f $chunks[3]) ($chunks[3] -eq 0)
        Check 'ukaz chunks ni javil napake' (-not (Get-LogText $s.Log).Contains('RWDIAG-CHUNKS-NAPAKA'))
        # Ogrevanje: prisilno nalaganje lahko sprozi nalaganje chunkov, to pa je delo na
        # server niti in bi se steto v meritev. Prva veljavna meritev (15. 9. 14:07) je
        # imela p95 1,6 ms in p99 81,8 ms - prepad, ki ga je treba lociti od dela NPC-jev.
        if ($WarmupSeconds -gt 0) {
            Write-Host ("  ogrevanje {0} s, da se nalaganje chunkov ne steje v meritev" -f $WarmupSeconds)
            Start-Sleep -Seconds $WarmupSeconds
        }
    } else {
        Write-Host '  preskoceno (-NoChunks): meritev bo merila prazen tek po 300 tickih'
    }

    Step 5 ("D5: merjenje {0} s" -f $Seconds)
    Send-Command $s 'rwdiag on'
    Check 'vklop potrjen' (Wait-ForMarker $s 'RWDIAG vklopljen' 30)
    Start-Sleep -Seconds $Seconds
    Send-Command $s 'rwdiag dump m21-prvi'
    Check 'prvi posnetek zapisan' (Wait-ForMarker $s 'RWDIAG-DUMP ' 60)
    Check 'prvi posnetek ima izid'  (Wait-ForMarker $s 'RWDIAG-OK ' 60)
    $first = Read-OkMarker $s.Log 1
    Check ("stevilo tickov je vecje od 0 (ticki={0})" -f $first[0]) ($first[0] -gt 0)
    Check ("NPC-ji so tikali (npc={0})" -f $first[1])               ($first[1] -gt 0)
    Check 'zapis ni javil napake' (-not (Get-LogText $s.Log).Contains('RWDIAG-NAPAKA'))

    # M2.1d merila. Brez njih je zelen zagon se vedno lahko meritev praznega teka: stevci
    # rastejo, samo NPC-ji ne tikajo. Prav to se je zgodilo 15. 9. trikrat zapored.
    Step '5b' 'C3-C5: NPC-ji morajo tikati ves cas meritve'
    $perTick = Read-Distribution $s.Log 'npc.per.tick'
    $gap     = Read-Distribution $s.Log 'npc.tick.gap'
    $forced  = Read-Distribution $s.Log 'world.chunks.forced'
    if (($null -eq $perTick) -or ($null -eq $gap)) {
        Check 'tabela posnetka je berljiva iz loga' $false
    } else {
        Check ("C3: npc.per.tick p50 > 0 (p50={0} min={1} max={2})" -f $perTick[3], $perTick[1], $perTick[6]) ($perTick[3] -gt 0)
        Check ("C4: npc.tick.gap max = 1 (max={0} n={1})" -f $gap[6], $gap[0]) ($gap[6] -eq 1)
        $wanted = [int]($first[0] * 0.95)
        Check ("C4b: gap ima vzorec za skoraj vsak tick (n={0}, pricakovano vsaj {1})" -f $gap[0], $wanted) ($gap[0] -ge $wanted)
    }
    if (-not $NoChunks) {
        if ($null -eq $forced) {
            Check 'C5: world.chunks.forced je v posnetku' $false
        } else {
            # Merilo je min > 0, ne min = max. Nabor sme med meritvijo RASTI - osvezitev
            # doda chunk, kadar NPC zatava - kar je delo zbiralnika, ne napaka. Padec na 0
            # bi bil napaka in ga min ujame. (15. 9.: prvo merilo je bilo min = max in bi
            # zavrglo prvo veljavno meritev projekta zaradi treh dodanih chunkov.)
            Check ("C5: pogoj meritve je veljal ves cas (min={0} max={1})" -f $forced[1], $forced[6]) ($forced[1] -gt 0)
            $added = Read-Counter $s.Log 'diag.chunks.added'
            if ($added -gt $forced[1]) {
                Write-Host ("  OPOZORILO obroc je premajhen za ta scenarij: dodanih {0} chunkov na zacetnih {1}; ponovi z -ChunkRadius {2}" -f $added, $forced[1], ($ChunkRadius + 1))
            } else {
                Write-Host ("  OK       obroc zadosca (dodanih {0} chunkov na zacetnih {1})" -f $added, $forced[1])
            }
        }
    }

    # Med prvim posnetkom in izklopom ticki se vedno tecejo, zato se drugi posnetek
    # naredi takoj po izklopu, tretji pa 10 s kasneje; primerjata se drugi in tretji.
    Step 6 'D7b: po izklopu se ticki ne smejo vec nabirati'
    Send-Command $s 'rwdiag off'
    Check 'izklop potrjen' (Wait-ForMarker $s 'RWDIAG izklopljen' 30)
    Send-Command $s 'rwdiag dump m21-ob-izklopu'
    Check 'drugi posnetek zapisan' (Wait-ForCount $s 'RWDIAG-OK ' 2 60)
    $second = Read-OkMarker $s.Log 2
    Start-Sleep -Seconds 10
    Send-Command $s 'rwdiag dump m21-po-izklopu'
    Check 'tretji posnetek zapisan' (Wait-ForCount $s 'RWDIAG-OK ' 3 60)
    $third = Read-OkMarker $s.Log 3
    Check ("ticki so med merjenjem rasli ({0} -> {1})" -f $first[0], $second[0]) ($second[0] -ge $first[0])
    Check ("ticki mirujejo po izklopu ({0} -> {1})" -f $second[0], $third[0]) ($third[0] -eq $second[0])
    Check ("npc miruje po izklopu ({0} -> {1})" -f $second[1], $third[1])     ($third[1] -eq $second[1])

    # C6: ticketi se morajo dati sprostiti, sicer bi se zapisali v forcedchunks.dat in bi
    # naslednji zagon tiho tekel pod drugacnim pogojem meritve.
    if (-not $NoChunks) {
        Step '6b' 'C6: sprostitev ticketov'
        Send-Command $s 'rwdiag chunks off'
        Check 'izklop chunkov potrjen' (Wait-ForCount $s 'RWDIAG-CHUNKS stanje=off' 1 30)
        $after = Read-ChunkMarker $s.Log 0
        Check ("C6: po izklopu ni prisilno nalozenih chunkov (chunki={0} tiketi={1})" -f $after[0], $after[1]) (($after[0] -eq 0) -and ($after[1] -eq 0))
    }

    Check 'server se je cisto ustavil' (Stop-DevServer $s)

    Step 7 'D4: nobene napake iz moda'
    $log = Get-LogText $s.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'brez "script errored"' (-not $log.Contains('script errored'))

    Step 8 'D6: zapisani posnetki'
    $txt  = @(Get-ChildItem -Path $dumps -Filter '*.txt'  -ErrorAction SilentlyContinue)
    $json = @(Get-ChildItem -Path $dumps -Filter '*.json' -ErrorAction SilentlyContinue)
    Check ("tri .txt datoteke (najdenih {0})"  -f $txt.Count)  ($txt.Count  -eq 3)
    Check ("tri .json datoteke (najdenih {0})" -f $json.Count) ($json.Count -eq 3)
    Check 'nobena datoteka ni prazna' ((@($txt + $json) | Where-Object { $_.Length -le 0 }).Count -eq 0)

    if ($txt.Count -gt 0) {
        Write-Host ''
        Write-Host ("--- {0} ---" -f $txt[0].Name)
        Get-Content $txt[0].FullName | ForEach-Object { Write-Host "  $_" }
    }

    Step 9 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M2.1 D4-D7 in C1-C6 USPESNO: pogoj meritve drzi in NPC-ji tikajo ves cas.'
        Write-Host ("Posnetki: {0}" -f $dumps)
        Write-Host ("Izpis:    {0}" -f $s.Log)
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.1 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $s.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.1 D4-D7 NEUSPESNO. Izpis je v audit\m21-rwdiag.log'
    exit 1
}
