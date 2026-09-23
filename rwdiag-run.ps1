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
# M2.5a: posnetek ima tabelo najpocasnejsih tickov s kontekstom (nalaganje chunkov,
# autosave, stevilo NPC-jev). Merila S1-S4 preverijo, da je tabela tam, da meri iste ticke
# kot porazdelitev, in izpisejo pripis repa - prepada med p95 in p99 se ne sme vec
# ugibati. Glej razdelek M2.5a v docs/scenariji/M2.1-diag.md.
#
# Zagon:
#     .\rwdiag-run.ps1                 # 60 s merjenja, chunki prisilno nalozeni (obroc 1)
#     .\rwdiag-run.ps1 -Seconds 180    # daljse merjenje
#     .\rwdiag-run.ps1 -ChunkRadius 2  # sirsi obroc, ce NPC-ji veliko tavajo
#     .\rwdiag-run.ps1 -NoChunks       # namenoma brez pogoja, za primerjavo s starimi meritvami
#     .\rwdiag-run.ps1 -WarmupSeconds 0 # brez ogrevanja, ce hoces meriti tudi nalaganje chunkov
#     .\rwdiag-run.ps1 -AcceptEula     # prvic, ce dev\run\eula.txt se ni sprejet
#     .\rwdiag-run.ps1 -JsonPath audit\m21-p1.json  # zapis zagona na izbrano pot (M2.5c)
#
# Ponovitve po protokolu (tri in vec) pozene .\ponovitve-run.ps1, ki -JsonPath poda sam.

param([int]$Seconds = 60, [switch]$AcceptEula, [int]$ChunkRadius = 1, [switch]$NoChunks,
      [int]$WarmupSeconds = 10, [string]$JsonPath = '')

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$dumps  = Join-Path $run 'logs\rwdiag'
$seed   = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

# Zapis zagona za protokol ponovitev (M2.5c).
. (Join-Path $root 'meritve-lib.ps1')

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

# Prebere vrstico RWDIAG-SAVE iz zapisanega posnetka (.txt):
#   "RWDIAG-SAVE izlocenih=1 brezKonteksta=0 ostalo=1199 p99vsi=0.705 p99brez=0.700 maxVsi=87.000 maxBrez=0.700"
# Vrne psobject ali $null, ce vrstice ni (npr. posnetek brez enega samega ticka).
function Read-SaveLine([string]$Path) {
    if (-not (Test-Path $Path)) { return $null }
    $text = Get-Content $Path -Raw -ErrorAction SilentlyContinue
    if ($null -eq $text) { return $null }
    $m = [regex]::Match($text, '(?m)^RWDIAG-SAVE izlocenih=(\d+) brezKonteksta=(\d+) ostalo=(\d+)' +
                               '(?: p99vsi=([\d.]+) p99brez=([\d.]+) maxVsi=([\d.]+) maxBrez=([\d.]+))?\s*$')
    if (-not $m.Success) { return $null }
    return [pscustomobject]@{
        Excluded  = [long]$m.Groups[1].Value
        NoContext = [long]$m.Groups[2].Value
        Kept      = [long]$m.Groups[3].Value
        P99All    = if ($m.Groups[4].Success) { [double]$m.Groups[4].Value } else { [double]::NaN }
        P99Clean  = if ($m.Groups[5].Success) { [double]$m.Groups[5].Value } else { [double]::NaN }
        MaxAll    = if ($m.Groups[6].Success) { [double]$m.Groups[6].Value } else { [double]::NaN }
        MaxClean  = if ($m.Groups[7].Success) { [double]$m.Groups[7].Value } else { [double]::NaN }
    }
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

# Prebere tabelo najpocasnejsih tickov iz zapisanega posnetka (.txt).
#   "najpocasnejsi ticki (16 od 1228)"
#   "1   900   43.2   269.019   8   0   0   1"
#   "ticki nad ravnijo: 10ms=14 25ms=14 50ms=14 100ms=1"
# Vrne psobject z Kept/Recorded/Rows/Levels ali $null, ce tabele v posnetku ni.
function Read-SlowTable([string]$Path) {
    if (-not (Test-Path $Path)) { return $null }
    $text = Get-Content $Path -Raw -ErrorAction SilentlyContinue
    if ($null -eq $text) { return $null }
    $head = [regex]::Match($text, '(?m)^najpocasnejsi ticki \((\d+) od (\d+)\)\s*$')
    if (-not $head.Success) { return $null }
    $rows = @()
    foreach ($m in [regex]::Matches($text,
            '(?m)^(\d+)\s+(\d+|-)\s+([\d.]+)\s+([\d.]+)\s+(\d+|-)\s+(\d+|-)\s+(\d+|-)\s+(\d+|-)\s*$')) {
        $rows += [pscustomobject]@{
            Rank    = [int]$m.Groups[1].Value
            Tick    = $m.Groups[2].Value
            AtSec   = [double]$m.Groups[3].Value
            Ms      = [double]$m.Groups[4].Value
            Npc     = $m.Groups[5].Value
            ChunkIn = $m.Groups[6].Value
            ChunkOut= $m.Groups[7].Value
            Save    = $m.Groups[8].Value
        }
    }
    $levels = @{}
    $lv = [regex]::Match($text, '(?m)^ticki nad ravnijo:(.*)$')
    if ($lv.Success) {
        foreach ($one in [regex]::Matches($lv.Groups[1].Value, '(\d+)ms=(\d+)')) {
            $levels[[int]$one.Groups[1].Value] = [long]$one.Groups[2].Value
        }
    }
    return [pscustomobject]@{
        Kept     = [int]$head.Groups[1].Value
        Recorded = [long]$head.Groups[2].Value
        Rows     = @($rows | Sort-Object Rank)
        Levels   = $levels
    }
}

# Vrne vrednost stolpca kot stevilo; "-" (neznan kontekst) je -1.
function Get-ColumnNumber($value) {
    if ($value -eq '-') { return -1 }
    return [long]$value
}

# server.tick.ns je v tabeli izpisan v milisekundah z decimalkami ("server.tick.ns (ms)"),
# zato ga Read-Distribution (cela stevila) ne prebere. Vrne [double[]](n, min, povp, p50,
# p95, p99, max) v ms ali $null.
function Read-TickMillis([string]$LogPath) {
    $pattern = '(?m)^\s*server\.tick\.ns \(ms\)\s+(\d+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*$'
    $m = [regex]::Matches((Get-LogText $LogPath), $pattern)
    if ($m.Count -eq 0) { return $null }
    $hit = $m[0]
    return @([double]$hit.Groups[1].Value, [double]$hit.Groups[2].Value, [double]$hit.Groups[3].Value,
             [double]$hit.Groups[4].Value, [double]$hit.Groups[5].Value, [double]$hit.Groups[6].Value,
             [double]$hit.Groups[7].Value)
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

    # 23. 9.: rwdiag-run je tekel za nav-run in je dve minuti cakal na TW-SCRIPT-OK.
    # nav-run pusti v svetu oznako; z njo scenarij pade takoj, brez zagona serverja.
    $scenMark = Join-Path $run 'world\rework-scenarij.txt'
    if (Test-Path $scenMark) {
        $prej = (Get-Content $scenMark -Raw).Trim()
        Check ("dev\run\world je testni svet (oznaka pravi: {0})" -f $prej) $false
        throw ("dev\run\world je ze uporabil scenarij '{0}'. Pozeni .\testworld.ps1 in .\testworld-run.ps1, sele potem ta scenarij." -f $prej)
    }

    # Pade takoj, ce dev\run ni od testnega sveta. Zagon 21. 9. ob 12:55 je to potreboval:
    # pred njim je tekel .\nav-run.ps1 in v svetu je ostalo 17 NAV NPC-jev namesto 21
    # fixture NPC-jev, brez T_Scripted. Merilo TW-SCRIPT-OK je zato padlo cele dve minuti
    # pozneje in ni povedalo, zakaj - meritev pa je bila takrat ze posneta nad napacnim
    # svetom. Isti prijem ima r2-run.ps1 od 17. 9.
    function Get-Prop([string]$Path, [string]$Key) {
        if (-not (Test-Path $Path)) { return "" }
        $line = @(Get-Content $Path | Where-Object { $_ -match ("^" + [regex]::Escape($Key) + "\s*=") })
        if ($line.Count -eq 0) { return "" }
        return ($line[0] -replace ('^' + [regex]::Escape($Key) + '\s*=\s*'), '').Trim()
    }
    $propsFile = Join-Path $run 'server.properties'
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
            Check ("dev\run\server.properties ni od testnega sveta - " + ($mismatch -join "; ")) $false
            throw "server.properties je od drugega scenarija. Pozeni najprej .\testworld.ps1, nato ta scenarij."
        }
    }

    $s = Start-DevServer 'm21'
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    # D4: svet mora tecti kot prej. Testni svet ima skripto na T_Scripted, ki se
    # oglasi ob nalaganju - ce je z novim jarjem kaj narobe, se to pozna tu.
    $twOk = Wait-ForMarker $s 'TW-SCRIPT-OK' 120
    Check 'testni svet in scripting tecejo (TW-SCRIPT-OK)' $twOk
    if (-not $twOk) {
        # 21. 9. ob 12:55: pred tem je tekel .\nav-run.ps1 in v svetu je bilo 17 NAV
        # NPC-jev namesto 21 fixture NPC-jev, torej brez T_Scripted. Brez tega izhoda bi
        # scenarij se minuto meril naprej in posnel meritev nad napacnim svetom - ta bi
        # izgledala povsem verodostojno. Ista vrsta napake kot 17. 9. pri r2-run.ps1.
        Stop-DevServer $s
        throw ("V svetu ni T_Scripted (marker TW-SCRIPT-OK ga ni). Najpogostejsi vzrok: " +
               "dev\run\world je pustil drug scenarij (npr. .\nav-run.ps1 ali .\r2-run.ps1). " +
               "Pozeni .\testworld.ps1 in nato .\testworld-run.ps1, sele potem ta scenarij.")
    }

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

    # M2.5a: pripis repa porazdelitve. Porazdelitev pove, KOLIKO tickov je pocasnih;
    # tabela pove, KATERI so bili in kaj je v njih teklo. Prva veljavna meritev (15. 9.)
    # ima p95 = 1,6 ms in p99 = 81,8 ms; brez pripisa se ta prepad lahko samo ugiba.
    Step '5c' 'S1-S4: pripis najpocasnejsih tickov'
    $dumpFiles = @(Get-ChildItem -Path $dumps -Filter '*m21-prvi*.txt' -ErrorAction SilentlyContinue |
                   Sort-Object LastWriteTime -Descending)
    $slow = $null
    if ($dumpFiles.Count -gt 0) { $slow = Read-SlowTable $dumpFiles[0].FullName }
    $ms = Read-TickMillis $s.Log
    if ($null -eq $slow) {
        Check 'S1: posnetek ima tabelo najpocasnejsih tickov' $false
    } else {
        Check ("S1: tabela ima vrstice (hranjenih={0} od {1} tickov)" -f $slow.Kept, $slow.Recorded) ($slow.Rows.Count -gt 0)
        $expected = [long]($first[0] * 0.95)
        Check ("S1b: tabela je iz iste meritve (zabelezenih={0}, ticki={1})" -f $slow.Recorded, $first[0]) ($slow.Recorded -ge $expected)

        if ($null -eq $ms) {
            Check 'S2: server.tick.ns je berljiv iz tabele posnetka' $false
        } elseif ($slow.Rows.Count -eq 0) {
            Check 'S2: tabela nima vrstice, ki bi jo primerjali z max porazdelitve' $false
        } else {
            # Isti tick mora biti max porazdelitve in prva vrstica tabele. Ce nista, vsak
            # od njiju meri nekaj drugega in pripis ne dokazuje nicesar.
            $topMs = $slow.Rows[0].Ms
            $diff  = [math]::Abs($topMs - $ms[6])
            Check ("S2: najpocasnejsi tick v tabeli = max porazdelitve ({0:N3} vs {1:N3} ms)" -f $topMs, $ms[6]) ($diff -le 0.002)
        }

        if ($slow.Levels.Count -eq 0) {
            Check 'S3: posnetek poroca ticke nad ravnijo proracuna' $false
        } else {
            $levelKeys = @($slow.Levels.Keys | Sort-Object)
            $monotone = $true
            for ($i = 1; $i -lt $levelKeys.Count; $i++) {
                if ($slow.Levels[$levelKeys[$i]] -gt $slow.Levels[$levelKeys[$i - 1]]) { $monotone = $false }
            }
            $summary = ($levelKeys | ForEach-Object { "{0}ms={1}" -f $_, $slow.Levels[$_] }) -join ' '
            Check ("S3: ticki nad ravnijo so poroceni in dosledni ({0})" -f $summary) $monotone
        }

        # Pripis: kaj je teklo v tickih, ki so presegli cel proracun (50 ms).
        $overBudget = @($slow.Rows | Where-Object { $_.Ms -ge 50.0 })
        $withSave   = @($overBudget | Where-Object { (Get-ColumnNumber $_.Save) -gt 0 })
        $withChunks = @($overBudget | Where-Object { (Get-ColumnNumber $_.ChunkIn) -gt 0 })
        $plain      = @($overBudget | Where-Object {
                          ((Get-ColumnNumber $_.Save) -le 0) -and ((Get-ColumnNumber $_.ChunkIn) -le 0) })
        $attribution = "  pripis tickov nad 50 ms: skupaj {0}, z autosave {1}, z nalaganjem chunkov {2}, brez obojega {3}"
        Write-Host ($attribution -f $overBudget.Count, $withSave.Count, $withChunks.Count, $plain.Count)

        # S4 ne trdi, kaj je vzrok - trdi le, da rep ni izgubljen. Ce ima porazdelitev
        # dolg rep (p99 vsaj 5x p95), mora biti ta rep viden tudi v tabeli, sicer pripisa
        # ni mogoce narediti in meritev je treba ponoviti z vecjo tabelo.
        if (($null -ne $ms) -and ($slow.Rows.Count -gt 0)) {
            if ($ms[5] -ge (5.0 * $ms[4])) {
                $tailRows = @($slow.Rows | Where-Object { $_.Ms -ge (0.9 * $ms[5]) })
                Check ("S4: rep (p95={0:N3} -> p99={1:N3} ms) je viden v tabeli ({2} vrstic)" -f $ms[4], $ms[5], $tailRows.Count) ($tailRows.Count -gt 0)
            } else {
                Write-Host ("  OK       S4: porazdelitev nima prepada (p95={0:N3} ms, p99={1:N3} ms), pripis ni potreben" -f $ms[4], $ms[5])
            }
        }

        Write-Host '  najpocasnejsi ticki (do 10 vrstic):'
        $rowFormat = "    #{0,-3} tick={1,-8} ob={2,7:N1} s  {3,10:N3} ms  npc={4,-4} chunk+={5,-4} chunk-={6,-4} save={7}"
        $slow.Rows | Select-Object -First 10 | ForEach-Object {
            Write-Host ($rowFormat -f $_.Rank, $_.Tick, $_.AtSec, $_.Ms, $_.Npc, $_.ChunkIn, $_.ChunkOut, $_.Save)
        }
    }

    # M2.5b: autosave ni cena NPC-jev. Tece vsakih 900 tickov (MinecraftServer.tick():762)
    # in je bil v vsakem dosedanjem zagonu najpocasnejsi tick meritve (58 / 74 / 87 / 124 ms).
    # Posnetek ga zato izloci v svojo porazdelitev, ne pa izbrise: server ta cas res porabi.
    # Merili S5 in S6 preverita, da izlocitev nic ne izgubi in da rep res pripada autosave.
    Step '5d' 'S5-S7: izlocitev autosave ticka'
    $save = $null
    if ($dumpFiles.Count -gt 0) { $save = Read-SaveLine $dumpFiles[0].FullName }
    if ($null -eq $save) {
        Check 'S5: posnetek loci ticke z autosave (vrstica RWDIAG-SAVE)' $false
    } else {
        Write-Host ("  izloceni={0} brezKonteksta={1} ostalo={2}" -f $save.Excluded, $save.NoContext, $save.Kept)
        $sum = $save.Excluded + $save.NoContext + $save.Kept
        $total = if ($null -ne $ms) { [long]$ms[0] } else { -1 }
        Check ("S5: nobene meritve ne izgubimo ({0}+{1}+{2} = {3}, porazdelitev n={4})" -f `
                $save.Excluded, $save.NoContext, $save.Kept, $sum, $total) `
              (($total -ge 0) -and ($sum -eq $total))

        if ([double]::IsNaN($save.P99Clean)) {
            Write-Host '  OK       S6: v meritvi ni bilo ticka brez autosave, primerjave repa ni'
        } else {
            Check ("S6: rep brez autosave ni daljsi od celega (p99 {0:N3} -> {1:N3} ms, max {2:N3} -> {3:N3} ms)" -f `
                    $save.P99All, $save.P99Clean, $save.MaxAll, $save.MaxClean) `
                  (($save.P99Clean -le ($save.P99All + 0.001)) -and ($save.MaxClean -le ($save.MaxAll + 0.001)))

            # S7 je navzkrizna preverba dveh neodvisnih mehanizmov: ce je autosave tisti,
            # ki potegne max cez proracun, mora biti prva vrstica tabele najpocasnejsih
            # tickov oznacena s save>0. Ce ni, si porazdelitev in tabela nasprotujeta.
            if (($save.MaxAll -ge 50.0) -and ($save.MaxClean -lt 50.0) -and ($null -ne $slow) -and ($slow.Rows.Count -gt 0)) {
                Check ("S7: najpocasnejsi tick je autosave (save={0})" -f $slow.Rows[0].Save) `
                      ((Get-ColumnNumber $slow.Rows[0].Save) -gt 0)
            } else {
                Write-Host '  OK       S7: max ne pade pod proracun z izlocitvijo autosave; pripis ostane pri S2'
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

    Step '8b' 'Zapis zagona za protokol ponovitev (M2.5c)'
    # Odtis: pod kaksnimi pogoji je meritev nastala (dolzina merjenja, obroc chunkov,
    # stevilo NPC-jev). Velicine: MSPT porazdelitev in pripis repa. Ponovitve zdruzi
    # .\ponovitve-run.ps1; razpon velicine cez ponovitve je merilni sum.
    $stampJ = Get-Date -Format 'yyyy-MM-dd-HHmm'
    if ($JsonPath -eq '') { $JsonPath = Join-Path $audit ("m21-rwdiag-{0}.json" -f $stampJ) }

    $odtis = @{
        sekund      = $Seconds
        ogrevanje   = $WarmupSeconds
        obroc       = $(if ($NoChunks) { -1 } else { $ChunkRadius })
        brezChunkov = $(if ($NoChunks) { 1 } else { 0 })
        # NE $first[1]: "RWDIAG-OK ... npc=" so KUMULATIVNI NPC-ticki (20910 pri 17 NPC-jih
        # cez 1229 tickov) in se med ponovitvami razlikujejo za nekaj tickov. V odtisu bi
        # to pomenilo, da serija pade na merilu T4 (odtis mora biti enak), ceprav je bil
        # svet isti. Stevilo NPC-jev pove vrstica RWDIAG-CHUNKS.
        npc         = $(if ($null -ne $chunks) { $chunks[2] } else { -1 })
    }

    # npcTickov je velicina in ne odtis: med ponovitvami se razlikuje za nekaj tickov,
    # v odtisu pa bi to podrlo merilo T4 (odtis mora biti enak v vseh ponovitvah).
    $vel = @{ ticki = $first[0]; npcTickov = $first[1] }
    if ($null -ne $ms) {
        $vel['mspt.n']    = $ms[0]
        $vel['mspt.min']  = $ms[1]
        $vel['mspt.povp'] = $ms[2]
        $vel['mspt.p50']  = $ms[3]
        $vel['mspt.p95']  = $ms[4]
        $vel['mspt.p99']  = $ms[5]
        $vel['mspt.max']  = $ms[6]
    }
    if ($null -ne $perTick) { $vel['npc.per.tick.p50'] = $perTick[3]; $vel['npc.per.tick.max'] = $perTick[6] }
    if ($null -ne $gap)     { $vel['npc.tick.gap.max'] = $gap[6] }
    if ($null -ne $forced)  { $vel['world.chunks.forced.p50'] = $forced[3] }
    if ($null -ne $slow) {
        $vel['slow.hranjenih']   = $slow.Kept
        $vel['slow.zabelezenih'] = $slow.Recorded
        if ($slow.Rows.Count -gt 0) { $vel['slow.maxMs'] = $slow.Rows[0].Ms }
    }
    if ($null -ne $save) {
        $vel['save.izlocenih'] = $save.Excluded
        $vel['save.ostalo']    = $save.Kept
        if (-not [double]::IsNaN($save.P99All))   { $vel['save.p99vsi']  = $save.P99All }
        if (-not [double]::IsNaN($save.P99Clean)) { $vel['save.p99brez'] = $save.P99Clean }
        if (-not [double]::IsNaN($save.MaxAll))   { $vel['save.maxVsi']  = $save.MaxAll }
        if (-not [double]::IsNaN($save.MaxClean)) { $vel['save.maxBrez'] = $save.MaxClean }
    }

    $zapisPot = Write-MeritevJson -Path $JsonPath -Paket 'M2.1' -Scenarij 'rwdiag' `
                    -Odtis $odtis -Velicine $vel -Uspeh ($failures.Count -eq 0) -Padle $failures
    Write-Host ("  zapis zagona: {0} ({1} velicin)" -f $zapisPot, $vel.Count)

    Step 9 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M2.1 D4-D7, C1-C6 in S1-S4 USPESNO: pogoj meritve drzi, NPC-ji tikajo ves cas in rep porazdelitve je pripisan.'
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
