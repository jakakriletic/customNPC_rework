# M2.4 - merilne obremenitve: 50 / 200 / 500 NPC-jev, loceno idle / boj / skripte.
# Scenarij in razlaga: docs/scenariji/M2.4-obremenitve.md
#
# Zakaj: vse dosedanje meritve (M2.1, M2.7) so imele 8-17 NPC-jev. Performance cilji
# projekta (README, M5) pa govorijo o stotinah. Brez tega scenarija M2.6 (baseline) nima
# obremenitve, pod katero bi meril, in nobena optimizacija v M5 nima stevilke 'pred'.
#
# Kaj naredi:
#   zagon A  pribije world spawn na 0 4 0 (isti razlog kot nav-run.ps1)
#   zagon B  postavi svet (perf-setup-commands.txt), nato za vsako celico (varianta x N):
#            1 'noppes slay npcs' in spawn N NPC-jev z 'noppes clone grid'
#            2 PERF_Kontrola: frakciji 1/2 sovrazni, presteje NPC-je, se odstrani
#            3 'rwdiag chunks on' (pogoj meritve M2.1d), ogrevanje, 'rwdiag on'
#            4 merjenje, 'rwdiag dump', 'rwdiag off'
#            5 PERF_Kontrola se enkrat: dokaz, da je obremenitev tekla ves cas
#            6 merila P1-P8 in zapis celice (meritve-lib.ps1)
#
# Celice tecejo v enem serverju, po vrsti: varianta za varianto, N narascajoce. Vrstni red
# je del odtisa; ponovitve (ponovitve-run.ps1) so primerljive samo z enakim vrstnim redom.
#
# Render (frame time klienta) tu NI: dedicated server nima izrisa. Glej scenarij.
#
# Zagon:
#     .\perf-run.ps1                                   # vseh 9 celic po protokolu (~70 min)
#     .\perf-run.ps1 -Seconds 60 -WarmupSeconds 20     # hitra preverba scenarija (~20 min)
#     .\perf-run.ps1 -Variants boj -Counts 200         # ena celica
#     .\ponovitve-run.ps1 -Scenarij perf -Dodatno @('-Variants','boj','-Counts','200')
#     .\perf-run.ps1 -Razprseno                        # spawn po 5 NPC-jev, ~7 tickov narazen (M2.6)
#     .\baseline-run.ps1                               # M2.6: tri ponovitve vseh celic in baseline
#
# Pred zagonom: .\testworld.ps1 (svez svet). Ta scenarij svet spremeni (pobije fixture M0.6)
# in za seboj pusti oznako dev\run\world\rework-scenarij.txt.

param([string[]]$Variants = @('idle', 'boj', 'skripte'), [string[]]$Counts = @('50', '200', '500'),
      [int]$Seconds = 300, [int]$WarmupSeconds = 120, [int]$ChunkRadius = 1,
      [switch]$AcceptEula, [string]$JsonPath = '', [switch]$Razprseno, [string]$SerijaDir = '')

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$seed   = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

. (Join-Path $root 'meritve-lib.ps1')

# Prizorisce; iste stevilke so opisane v perf-setup-commands.txt in v scenariju.
$gridX     = 48      # zahodni rob mreze
$gridZ     = -12     # severni rob mreze
$gridW     = 25      # NPC-jev v vrsti (os x); N mora biti veckratnik 2*gridW = 50
$bojGap    = 4       # prazne vrste med skupinama A in B (AggroRange 16 jih pokrije)
$kontrolaAt = '60,4,30'
$znaneVariante = @('idle', 'boj', 'skripte')
# -Razprseno: skupina po 5 NPC-jev na ukaz, ukazi ~7 tickov narazen. 7 je tuje 10, zato
# zaporedni ukazi padejo v vse faze 'ticksExisted % 10' (EntityNPCInterface.java:358).
$razKos   = 5
$razPavza = 50       # ms poleg 300 ms v Send-Command; skupaj ~350 ms = ~7 tickov

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

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

function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        $n = ([regex]::Matches((Get-MarkerText $Srv.Log), [regex]::Escape($Marker))).Count
        if ($n -ge $Count) { return $true }
        if ($Srv.Proc.HasExited) {
            Write-Host ("  ! proces se je koncal, preden se je pojavil marker '{0}' (#{1})" -f $Marker, $Count)
            return $false
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host ("  ! timeout {0} s pri cakanju na '{1}' (#{2})" -f $TimeoutSec, $Marker, $Count)
            return $false
        }
        Start-Sleep -Seconds 1
    }
}

function Wait-ForMarker($Srv, [string]$Marker, [int]$TimeoutSec) {
    return (Wait-ForCount $Srv $Marker 1 $TimeoutSec)
}

function Start-DevServer([string]$Tag = '') {
    $name = if ($Tag -eq '') { 'm24-perf.log' } else { ('m24-perf-{0}.log' -f $Tag) }
    $outLog = Join-Path $audit $name
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
}

function Stop-DevServer($srv, [int]$TimeoutSec = 180) {
    if ($srv.Proc.HasExited) { return $false }
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

function Get-Prop([string]$File, [string]$Key) {
    $line = @(Get-Content $File | Where-Object { $_ -match ('^' + [regex]::Escape($Key) + '\s*=') })
    if ($line.Count -eq 0) { return '' }
    return ($line[0] -replace ('^' + [regex]::Escape($Key) + '\s*=\s*'), '').Trim()
}

# Zadnji izpis PERF-KONTROLA -> psobject; $null, ce ga ni.
function Read-Kontrola([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath),
        'PERF-KONTROLA idle=(\d+) bojA=(\d+) bojB=(\d+) skripte=(\d+) drugi=(\d+) cilj=(\d+) ranjenih=(\d+) skriptTickov=(\d+)')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    return [pscustomobject]@{
        Idle = [int]$g[1].Value; BojA = [int]$g[2].Value; BojB = [int]$g[3].Value
        Skripte = [int]$g[4].Value; Drugi = [int]$g[5].Value; Cilj = [int]$g[6].Value
        Ranjenih = [int]$g[7].Value; SkriptTickov = [long]$g[8].Value
    }
}

function Read-Frakcije([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath), 'PERF-FRAKCIJE a>b=(\w+) b>a=(\w+)')
    if ($m.Count -eq 0) { return $false }
    $g = $m[$m.Count - 1].Groups
    return ($g[1].Value -eq 'true' -and $g[2].Value -eq 'true')
}

# Zadnji odgovor 'rwdiag chunks ...' -> psobject; $null, ce ga ni.
function Read-Chunks([string]$LogPath) {
    $t = Get-MarkerText $LogPath
    $m = [regex]::Matches($t, 'RWDIAG-CHUNKS stanje=(\w+) chunki=(\d+) tiketi=(\d+)(?: obroc=\d+ zavrnjeni=(\d+))?(?: npc=(\d+))?')
    if ($m.Count -eq 0) { return $null }
    $g = $m[$m.Count - 1].Groups
    return [pscustomobject]@{
        Stanje = $g[1].Value; Chunki = [int]$g[2].Value; Tiketi = [int]$g[3].Value
        Zavrnjeni = $(if ($g[4].Success) { [int]$g[4].Value } else { -1 })
        Npc = $(if ($g[5].Success) { [int]$g[5].Value } else { -1 })
    }
}

# Zadnji 'RWDIAG-DUMP logs\rwdiag\x.txt' -> absolutna pot do .json posnetka.
function Read-DumpJsonPath([string]$LogPath) {
    $m = [regex]::Matches((Get-MarkerText $LogPath), 'RWDIAG-DUMP (\S+\.txt)')
    if ($m.Count -eq 0) { return $null }
    $rel = $m[$m.Count - 1].Groups[1].Value.Trim()
    $p = if ([System.IO.Path]::IsPathRooted($rel)) { $rel } else { Join-Path $run $rel }
    return ($p -replace '\.txt$', '.json')
}

function Get-Dist($Dump, [string]$Name) {
    foreach ($d in $Dump.distributions) { if ($d.name -eq $Name) { return $d } }
    return $null
}

function Get-Counter($Dump, [string]$Name) {
    foreach ($c in $Dump.counters) { if ($c.name -eq $Name) { return $c } }
    return $null
}

function Ms([double]$ns) { return [math]::Round($ns / 1000000.0, 3) }

# Ena skupina (ime, N NPC-jev od vrste z0 naprej) kot ukazi. Brez -Razprseno en sam
# 'clone grid'; z -Razprseno kosi po $razKos NPC-jev, vsak na svojem mestu mreze.
function Get-GroupCommands([string]$Ime, [int]$N, [int]$Z0) {
    $rows = [int]($N / $gridW)
    if (-not $Razprseno) {
        return @('noppes clone grid {0} 1 {1} {2} {3},4,{4}' -f $Ime, $gridW, $rows, $gridX, $Z0)
    }
    $out = @()
    for ($r = 0; $r -lt $rows; $r++) {
        for ($c = 0; $c -lt ($gridW / $razKos); $c++) {
            $out += ('noppes clone grid {0} 1 {1} 1 {2},4,{3}' -f $Ime, $razKos, ($gridX + $c * $razKos), ($Z0 + $r))
        }
    }
    return $out
}

# Ukazi za spawn ene celice. 'noppes clone grid <ime> 1 <sirina x> <vrstic z> x,y,z'
# (CmdClone.grid: zanka x < args[2], z < args[3]; NPC stoji na prvem polnem bloku).
function Get-SpawnCommands([string]$Variant, [int]$N) {
    $cmds = @()
    if ($Variant -eq 'boj') {
        $rows = [int](($N / 2) / $gridW)
        $zB = $gridZ + $rows + $bojGap
        $cmds += Get-GroupCommands 'PERF_BojA' ($N / 2) $gridZ
        $cmds += Get-GroupCommands 'PERF_BojB' ($N / 2) $zB
    } else {
        $ime = if ($Variant -eq 'idle') { 'PERF_Idle' } else { 'PERF_Skripte' }
        $cmds += Get-GroupCommands $ime $N $gridZ
    }
    return $cmds
}

$srv = $null
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'Parametri in priprava'
    # PowerShell pri klicu iz drugega procesa (ponovitve-run.ps1) poda '-Variants boj,idle'
    # kot en niz; razbijemo ga tu, da se obnasa enako kot seznam.
    $Variants = @($Variants | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().ToLower() } | Where-Object { $_ -ne '' })
    # Isto za stevila: '-Counts 50,200' iz -File pride kot en niz.
    $Counts   = @($Counts | ForEach-Object { "$_" -split ',' } | Where-Object { $_.Trim() -ne '' } | ForEach-Object { [int]$_.Trim() } | Sort-Object)
    foreach ($v in $Variants) {
        if ($znaneVariante -notcontains $v) { throw ("Neznana varianta '{0}'. Znane: {1}" -f $v, ($znaneVariante -join ', ')) }
    }
    foreach ($n in $Counts) {
        if ($n -le 0 -or ($n % (2 * $gridW)) -ne 0) { throw ("Stevilo NPC-jev {0} ni pozitiven veckratnik {1}." -f $n, (2 * $gridW)) }
        if ($n -gt 1000) { throw "Vec kot 1000 NPC-jev ni predvideno (mreza bi segla cez prostor scenarija)." }
    }
    $cells = @()
    foreach ($v in $Variants) { foreach ($n in $Counts) { $cells += [pscustomobject]@{ Varianta = $v; N = $n } } }
    if ($JsonPath -ne '' -and $cells.Count -ne 1) { throw '-JsonPath je dovoljen samo za eno celico (ena varianta, eno stevilo).' }
    $perCell = $WarmupSeconds + $Seconds + 25
    Write-Host ("  celice: {0}" -f (($cells | ForEach-Object { '{0}/{1}' -f $_.Varianta, $_.N }) -join ', '))
    Write-Host ("  ogrevanje {0} s, merjenje {1} s, obroc {2}; ocena trajanja ~{3} min" -f `
        $WarmupSeconds, $Seconds, $ChunkRadius, [math]::Ceiling(($cells.Count * $perCell + 240) / 60))

    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\perf-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # PERF_Kontrola izpisuje z '/say' prek executeCommand, ki brez command blokov vrze
    # CustomNPCsException (isti razlog kot v nav-run.ps1).
    $propsFile = Join-Path $run 'server.properties'
    $seedProps = Join-Path $seed 'server.properties'
    if (Test-Path $propsFile) {
        $props = @(Get-Content $propsFile)
        if (-not ($props -match '^enable-command-block\s*=\s*true')) {
            if ($props -match '^enable-command-block\s*=') {
                $props = $props -replace '^enable-command-block\s*=.*', 'enable-command-block=true'
            } else {
                $props += 'enable-command-block=true'
            }
            Set-Content -Path $propsFile -Value $props -Encoding ASCII
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa PERF-*)'
        }
        if (Test-Path $seedProps) {
            $mismatch = @()
            foreach ($key in @('level-name', 'level-seed')) {
                $want = Get-Prop $seedProps $key
                $have = Get-Prop $propsFile $key
                if ($want -ne $have) { $mismatch += ("{0} je '{1}', pricakovano '{2}'" -f $key, $have, $want) }
            }
            Check ("dev\run\server.properties je od testnega sveta" + $(if ($mismatch.Count) { ' - ' + ($mismatch -join '; ') } else { '' })) ($mismatch.Count -eq 0)
            if ($mismatch.Count -gt 0) { throw "server.properties je od drugega scenarija. Pozeni najprej .\testworld.ps1, nato ta scenarij." }
        }
    } else {
        throw 'dev\run\server.properties ne obstaja. Pozeni najprej .\testworld.ps1.'
    }

    # Svet, ki ga je ze uporabil drug scenarij, ima druge NPC-je in druge bloke.
    $scenMark = Join-Path $run 'world\rework-scenarij.txt'
    if (Test-Path $scenMark) {
        $prej = (Get-Content $scenMark -Raw).Trim()
        Check ("svet je svez (oznaka pravi: {0})" -f $prej) $false
        throw ("dev\run\world je ze uporabil scenarij '{0}'. Pozeni najprej .\testworld.ps1, nato ta scenarij." -f $prej)
    }
    Check 'svet ni oznacen kot uporabljen' $true

    $srcClones = Join-Path $seed 'customnpcs\clones\1'
    $dstClones = Join-Path $run  'world\customnpcs\clones\1'
    New-Item -ItemType Directory -Force -Path $dstClones | Out-Null
    $copied = 0
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'PERF_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("pet PERF fixture datotek je v svetu (kopiranih {0})" -f $copied) ($copied -eq 5)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones. Pozeni 'python3 dev/testworld/perf-fixture.py'." }

    Step 2 'Zagon A: pribij world spawn na 0 4 0'
    $a = Start-DevServer 'a'
    Check 'server A je dosegel "Done ("' (Wait-ForMarker $a 'Done (' 900)
    if ($failures.Count -eq 0) {
        Send-Command $a 'setworldspawn 0 4 0'
        Check 'world spawn nastavljen' (Wait-ForMarker $a 'Set the world spawn point' 60)
        Send-Command $a 'save-all flush'
        Check 'svet shranjen (A)' (Wait-ForMarker $a 'Saved the world' 180)
    }
    Check 'server A se je cisto ustavil' (Stop-DevServer $a)
    if ($failures.Count -gt 0) { throw "Zagon A ni uspel. Glej $($a.Log)" }

    Step 3 'Zagon B: postavitev sveta'
    $srv = Start-DevServer
    Check 'server B je dosegel "Done ("' (Wait-ForMarker $srv 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($srv.Log)" }
    Send-File $srv (Join-Path $seed 'perf-setup-commands.txt')
    Check 'svet shranjen po postavitvi' (Wait-ForMarker $srv 'Saved the world' 120)

    # Pobijanje fixtur M0.6. Vrstni red je bistven: 'noppes slay npcs' samo oznaci isDead,
    # odstrani pa jih sele updateEntities - ta pa brez igralca in brez prisilno nalozenih
    # chunkov 300 tickov po nalaganju neha teci (M2.1d). Zato najprej chunki (svet se
    # zbudi), nato slay. Ce v svetu ni NPC-jev (samo .\testworld.ps1, brez
    # .\testworld-run.ps1), 'chunks on' odgovori stanje=off in ni kaj pobiti.
    # Chunki ostanejo vklopljeni do konca: brez njih bi se svet med celicami spet ustavil
    # in pobiti NPC-ji prejsnje celice bi ostali v loadedEntityList.
    $nKontrola = 0; $nChunks = 0; $nOn = 0; $nOff = 0; $nDump = 0
    Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
    $nChunks++
    Check 'ukaz chunks odgovori (ciscenje)' (Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60)
    $ch = Read-Chunks $srv.Log
    Write-Host ("  pred ciscenjem: stanje={0} npc={1}" -f $ch.Stanje, $ch.Npc)
    Send-Command $srv 'noppes slay npcs'
    Start-Sleep -Seconds 5
    Set-Content -Path $scenMark -Value ("perf-run {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm')) -Encoding ASCII
    $rows = @()
    $stamp = Get-Date -Format 'yyyy-MM-dd-HHmm'
    $idx = 0
    foreach ($cell in $cells) {
        $idx++
        $v = $cell.Varianta; $N = $cell.N
        $cellFailStart = $failures.Count
        Step ('4.{0}' -f $idx) ("celica {0}/{1}: {2}, {3} NPC-jev" -f $idx, $cells.Count, $v, $N)

        Send-Command $srv 'noppes slay npcs'
        Start-Sleep -Seconds 3
        $spawnCmds = @(Get-SpawnCommands $v $N)
        if ($Razprseno) {
            Write-Host ("  razprseni spawn: {0} ukazov po {1} NPC-jev, ~{2} s" -f $spawnCmds.Count, $razKos, [math]::Ceiling($spawnCmds.Count * 0.35))
            foreach ($c in $spawnCmds) { Send-Command $srv $c; Start-Sleep -Milliseconds $razPavza }
        } else {
            foreach ($c in $spawnCmds) { Write-Host "  > $c"; Send-Command $srv $c }
        }
        Start-Sleep -Seconds 3

        # P2: pogoj meritve (M2.1d) in hkrati neodvisno stetje NPC-jev. PRED krmilnikom:
        # ce je svet ustavljen (prva celica), PERF_Kontrola brez tega ne bi nikoli tiknil.
        Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
        $nChunks++
        Check 'ukaz chunks odgovori' (Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60)
        $ch = Read-Chunks $srv.Log
        $chOk = ($null -ne $ch) -and ($ch.Stanje -eq 'on') -and ($ch.Npc -eq $N) -and ($ch.Zavrnjeni -eq 0)
        Check ("P2: chunki prisilno nalozeni, npc={0} (pricakovano {1}), chunki={2}, zavrnjeni={3}" -f `
            $ch.Npc, $N, $ch.Chunki, $ch.Zavrnjeni) $chOk

        # P1: prava obremenitev je v svetu (in samo ta).
        Send-Command $srv ('noppes clone spawn PERF_Kontrola 1 {0}' -f $kontrolaAt)
        $nKontrola++
        $okK = Wait-ForCount $srv 'PERF-KONTROLA idle=' $nKontrola 90
        Check 'P1: PERF_Kontrola se je oglasil (zacetek)' $okK
        if (-not $okK) { throw 'PERF_Kontrola se ni oglasil; brez tega ni dokaza, kaj je v svetu.' }
        $k0 = Read-Kontrola $srv.Log
        $want = @{ Idle = 0; BojA = 0; BojB = 0; Skripte = 0 }
        if ($v -eq 'idle')    { $want.Idle = $N }
        if ($v -eq 'skripte') { $want.Skripte = $N }
        if ($v -eq 'boj')     { $want.BojA = $N / 2; $want.BojB = $N / 2 }
        $spawnOk = ($k0.Idle -eq $want.Idle) -and ($k0.BojA -eq $want.BojA) -and ($k0.BojB -eq $want.BojB) -and `
                   ($k0.Skripte -eq $want.Skripte) -and ($k0.Drugi -eq 0)
        Check ("P1: v svetu je natanko obremenitev celice (idle={0} bojA={1} bojB={2} skripte={3} drugi={4})" -f `
            $k0.Idle, $k0.BojA, $k0.BojB, $k0.Skripte, $k0.Drugi) $spawnOk
        if ($v -eq 'boj') { Check 'P1: frakciji 1 in 2 sta sovrazni druga drugi' (Read-Frakcije $srv.Log) }
        # Krmilnik se odstrani sam; pocakamo, da ga ni vec, preden se zacne ogrevanje.
        Start-Sleep -Seconds 2

        if ($WarmupSeconds -gt 0) {
            Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
            Start-Sleep -Seconds $WarmupSeconds
        }

        Send-Command $srv 'rwdiag on'
        $nOn++
        Check 'merjenje vklopljeno' (Wait-ForCount $srv 'RWDIAG vklopljen' $nOn 60)
        Write-Host ("  merjenje {0} s" -f $Seconds)
        Start-Sleep -Seconds $Seconds
        $tag = 'm24-{0}-{1}' -f $v, $N
        Send-Command $srv ("rwdiag dump {0}" -f $tag)
        $nDump++
        Check 'posnetek zapisan' (Wait-ForCount $srv 'RWDIAG-DUMP ' $nDump 120)
        Send-Command $srv 'rwdiag off'
        $nOff++
        Check 'merjenje izklopljeno' (Wait-ForCount $srv 'RWDIAG izklopljen' $nOff 60)

        Send-Command $srv ('noppes clone spawn PERF_Kontrola 1 {0}' -f $kontrolaAt)
        $nKontrola++
        $okK = Wait-ForCount $srv 'PERF-KONTROLA idle=' $nKontrola 120
        Check 'PERF_Kontrola se je oglasil (konec)' $okK
        $k1 = if ($okK) { Read-Kontrola $srv.Log } else { $null }

        # --- posnetek ---
        $dumpPath = Read-DumpJsonPath $srv.Log
        $dump = $null
        if (($null -ne $dumpPath) -and (Test-Path $dumpPath)) {
            $dump = Get-Content $dumpPath -Raw | ConvertFrom-Json
        }
        Check ("posnetek je berljiv ({0})" -f $dumpPath) ($null -ne $dump)

        $vel = @{}
        $sat = $false
        if ($null -ne $dump) {
            $tick   = Get-Dist $dump 'server.tick.ns'
            $nosave = Get-Dist $dump 'server.tick.ns.nosave'
            $per    = Get-Dist $dump 'npc.per.tick'
            $gap    = Get-Dist $dump 'npc.tick.gap'
            $loaded = Get-Dist $dump 'world.npc.loaded'
            $killed = Get-Dist $dump 'world.npc.killed'
            $forced = Get-Dist $dump 'world.chunks.forced'
            $upd    = Get-Counter $dump 'npc.update.window'

            $vel['ticki'] = [long]$dump.ticks
            if ($dump.elapsedMillis -gt 0) { $vel['tps'] = [math]::Round($dump.ticks * 1000.0 / $dump.elapsedMillis, 2) }
            if ($null -ne $tick) {
                $vel['mspt.povp'] = Ms ($tick.sum / [math]::Max(1, $tick.count))
                $vel['mspt.p50']  = Ms $tick.p50
                $vel['mspt.p95']  = Ms $tick.p95
                $vel['mspt.p99']  = Ms $tick.p99
                $vel['mspt.max']  = Ms $tick.max
                # Nasicenje: server ne dohaja 20 tickov na sekundo. To je veljaven rezultat
                # (03-FAZE.md, tveganja M2), ne napaka scenarija.
                $sat = ($tick.p50 -gt 50000000)
            }
            if ($null -ne $nosave) { $vel['mspt.brezSave.p99'] = Ms $nosave.p99; $vel['mspt.brezSave.max'] = Ms $nosave.max }
            if (($null -ne $upd) -and ($upd.count -gt 0)) {
                $vel['npc.us'] = [math]::Round($upd.nanos / 1000.0 / $upd.count, 2)
            }
            # M2.6: pomnilnik in GC (JvmProbe). Brez teh vrstic jar ne vsebuje M2.6.
            $gc     = Get-Counter $dump 'jvm.gc'
            $gcOld  = Get-Counter $dump 'jvm.gc.old'
            $alloc  = Get-Counter $dump 'jvm.alloc.server'
            $allocOk= Get-Counter $dump 'jvm.alloc.podprto'
            $oldPo  = Get-Counter $dump 'jvm.heap.old.poGc'
            $hMax   = Get-Counter $dump 'jvm.heap.max'
            Check 'P8: posnetek ima meritve JVM (jvm.gc, jvm.alloc.server)' (($null -ne $gc) -and ($null -ne $alloc))
            if (($null -ne $gc) -and ($dump.elapsedMillis -gt 0)) {
                $sek = $dump.elapsedMillis / 1000.0
                $vel['gc.zbirk'] = [long]$gc.count
                $vel['gc.ms'] = [math]::Round($gc.nanos / 1000000.0, 0)
                $vel['gc.msNaS'] = [math]::Round($gc.nanos / 1000000.0 / $sek, 2)
                if ($null -ne $gcOld) { $vel['gc.old.zbirk'] = [long]$gcOld.count; $vel['gc.old.ms'] = [math]::Round($gcOld.nanos / 1000000.0, 0) }
                if (($null -ne $alloc) -and ($null -ne $allocOk) -and ($allocOk.count -eq 1)) {
                    $vel['alok.MBnaS'] = [math]::Round($alloc.count / 1048576.0 / $sek, 1)
                    if ($dump.ticks -gt 0) { $vel['alok.KBnaTick'] = [math]::Round($alloc.count / 1024.0 / $dump.ticks, 1) }
                } else {
                    Write-Host '  OPOZORILO JVM ne podpira stetja alokacij po niti; alok.* manjka'
                }
                if (($null -ne $oldPo) -and ($oldPo.count -gt 0)) { $vel['heap.old.poGcMB'] = [math]::Round($oldPo.count / 1048576.0, 0) }
                if ($null -ne $hMax) { $vel['heap.maxMB'] = [math]::Round($hMax.count / 1048576.0, 0) }
            }
            foreach ($lv in $dump.slowTicks.levels) { $vel[('ticki.nad{0}ms' -f [int]($lv.ns / 1000000))] = [long]$lv.count }
            if ($null -ne $per) { $vel['npc.per.tick.p50'] = [long]$per.p50 }

            # P3: vsak NPC tika vsak tick.
            $p3 = ($null -ne $per) -and ($null -ne $gap) -and ($per.p50 -eq $N) -and ($gap.max -eq 1)
            Check ("P3: vsi NPC-ji tikajo vsak tick (npc.per.tick p50={0}, gap max={1})" -f $per.p50, $gap.max) $p3
            # P4: stevilo NPC-jev se med meritvijo ni spremenilo.
            $p4 = ($null -ne $loaded) -and ($loaded.min -eq $N) -and ($loaded.max -eq $N) -and `
                  ($null -ne $killed) -and ($killed.max -eq 0)
            Check ("P4: NPC-jev ves cas {0} (loaded min={1} max={2}, killed max={3})" -f $N, $loaded.min, $loaded.max, $killed.max) $p4
            Check ("P4b: pogoj meritve je veljal ves cas (world.chunks.forced min={0})" -f $forced.min) (($null -ne $forced) -and ($forced.min -gt 0))
        }

        # P5: obremenitev je bila res obremenitev celice.
        if ($null -ne $k1) {
            if ($v -eq 'idle') {
                Check ("P5: idle NPC-ji se niso borili (cilj={0}, ranjenih={1})" -f $k1.Cilj, $k1.Ranjenih) (($k1.Cilj -eq 0) -and ($k1.Ranjenih -eq 0))
            }
            if ($v -eq 'boj') {
                Check ("P5: boj je tekel (s ciljem {0} od {1}, ranjenih {2})" -f $k1.Cilj, $N, $k1.Ranjenih) (($k1.Cilj -ge ($N / 2)) -and ($k1.Ranjenih -gt 0))
                $vel['boj.sCiljem'] = $k1.Cilj
                $vel['boj.ranjenih'] = $k1.Ranjenih
            }
            if ($v -eq 'skripte') {
                $delta = $k1.SkriptTickov - $k0.SkriptTickov
                $wanted = if ($null -ne $dump) { [long](0.9 * $N * $dump.ticks / 10) } else { 1 }
                Check ("P5: skripte so tekle ves cas (skriptnih tickov {0}, pricakovano vsaj {1})" -f $delta, $wanted) ($delta -ge $wanted)
                $vel['skripte.tickov'] = $delta
            }
        }

        # P6: nobene napake iz moda ali skript v tej celici; P7 pomnilnik.
        $log = Get-LogText $srv.Log
        $oom = $log.Contains('OutOfMemoryError')
        Check 'P7: brez OutOfMemoryError' (-not $oom)
        if ($sat) { Write-Host ("  NASICENO   MSPT p50 = {0} ms > 50 ms: server ne dohaja 20 TPS pri {1} NPC-jih ({2})" -f $vel['mspt.p50'], $N, $v) }

        $odtis = @{ razprseno = $(if ($Razprseno) { 1 } else { 0 }); varianta = $v; npc = $N; sekund = $Seconds; ogrevanje = $WarmupSeconds; obroc = $ChunkRadius
                    celica = $idx; celic = $cells.Count; mrezaX = $gridX; mrezaZ = $gridZ; mrezaSirina = $gridW }
        $cellFails = @()
        if ($failures.Count -gt $cellFailStart) { $cellFails = @($failures[$cellFailStart..($failures.Count - 1)]) }
        $vel['nasiceno'] = $(if ($sat) { 1 } else { 0 })
        $jp = if ($JsonPath -ne '') { $JsonPath }
              elseif ($SerijaDir -ne '') { Join-Path $SerijaDir ("{0}-{1}.json" -f $v, $N) }
              else { Join-Path $audit ("m24-perf-{0}-{1}-{2}.json" -f $v, $N, $stamp) }
        $null = Write-MeritevJson -Path $jp -Paket 'M2.4' -Scenarij ("perf-{0}-{1}" -f $v, $N) `
                    -Odtis $odtis -Velicine $vel -Uspeh ($cellFails.Count -eq 0) -Padle $cellFails
        Write-Host ("  zapis celice: {0}" -f $jp)
        $rows += [pscustomobject]@{ Varianta = $v; N = $N; Vel = $vel; Uspeh = ($cellFails.Count -eq 0); Nasiceno = $sat }

        if ($oom -or $srv.Proc.HasExited) {
            Write-Host '  ! server je brez pomnilnika ali se je ustavil; preostale celice se ne pozenejo'
            break
        }
    }

    Step 5 'Konec in napake moda'
    Send-Command $srv 'rwdiag chunks off'
    $nChunks++
    $null = Wait-ForCount $srv 'RWDIAG-CHUNKS stanje=' $nChunks 60
    $log = Get-LogText $srv.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'P6: brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'P6: brez "script errored"' (-not $log.Contains('script errored'))
    Check 'server se je cisto ustavil' (Stop-DevServer $srv)

    Step 6 'Porocilo'
    $report = Join-Path $audit ("m24-perf-{0}.md" -f $stamp)
    $L = @()
    $L += ("# M2.4 - merilne obremenitve, zagon {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $L += ''
    $L += 'Scenarij: `docs/scenariji/M2.4-obremenitve.md`. Ogrevanje {0} s, merjenje {1} s, obroc chunkov {2}, spawn {3}.' -f $WarmupSeconds, $Seconds, $ChunkRadius, $(if ($Razprseno) { 'razprsen' } else { 'naenkrat' })
    $L += ''
    $L += ("Merila: " + $(if ($failures.Count -eq 0) { 'P1-P8 zelena' } else { 'PADLA: ' + ($failures -join '; ') }))
    $L += ''
    $L += '| varianta | NPC | TPS | MSPT povp | p50 | p95 | p99 | max | p99 brez save | us/NPC update | ticki >50 ms | GC ms/s | GC stare | alok. MB/s | stanje |'
    $L += '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|'
    foreach ($r in $rows) {
        $x = $r.Vel
        $st = if (-not $r.Uspeh) { 'merila padla' } elseif ($r.Nasiceno) { 'nasiceno' } else { 'ok' }
        $L += ('| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} | {11} | {12} | {13} | {14} |' -f `
            $r.Varianta, $r.N, $x['tps'], $x['mspt.povp'], $x['mspt.p50'], $x['mspt.p95'], $x['mspt.p99'],
            $x['mspt.max'], $x['mspt.brezSave.p99'], $x['npc.us'], $x['ticki.nad50ms'],
            $x['gc.msNaS'], $x['gc.old.zbirk'], $x['alok.MBnaS'], $st)
    }
    $L += ''
    $L += 'MSPT v ms. Posnetki: `dev/run/logs/rwdiag/*m24-*`; zapisi celic: `audit/m24-perf-<varianta>-<N>-' + $stamp + '.json`.'
    [System.IO.File]::WriteAllText($report, (($L -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ''
    $L | ForEach-Object { Write-Host "  $_" }
    Write-Host ''
    Write-Host ("Porocilo: {0}" -f $report)

    if ($failures.Count -eq 0) {
        Write-Host 'M2.4 USPESNO: vse celice so merile, kar pravijo, da merijo.'
        exit 0
    }
    Write-Host 'M2.4 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $srv.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    if ($null -ne $srv) { $null = Stop-DevServer $srv }
    Write-Host 'M2.4 NEUSPESNO. Izpis je v audit\m24-perf.log'
    exit 1
}
