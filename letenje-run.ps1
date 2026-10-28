# M4.16 - scenarij letenja in dometa (D-038), merila LT1-LT9.
#
# Scenarij in razlaga: docs/scenariji/M4.16-letenje.md
#
# Sest vzporednih prog v istem svetu in istem ticku (zid, vogal, labirint, dalec za
# leteci in kopenski, prosta kontrola) in tri faze (en navigateTo, osvezen navigateTo,
# en navigateTo s polovicno hitrostjo). Brez produkcijske kode; meri original.
#
# Vsak zagon ima svoj svez svet dev\run\rwlet-<cas> (-PrwWorld): prvi zagon serverja
# pribije world spawn na 0 4 0 (sicer je spawn semena 750 blokov proc in 'fill' javi
# "outside of the world"), drugi zagon postavi prizorisce in meri. dev\run\world,
# server.properties in config se ne spreminjajo; config SHA se preveri na koncu (LT9).
#
# Zagon:
#     .\letenje-run.ps1
#     .\letenje-run.ps1 -JsonPath audit\m416-letenje-p1.json   # zapis zagona (M2.5c)
# Tri ponovitve:  .\ponovitve-run.ps1 -Scenarij letenje
#
# M4.10 (D-084): -Nadaljevanje 1 pred scenarijem poslje 'rwpath nadaljuj 1' (stikalo
# RwPathResume med tekom; config se ne spremeni) in po LET-SUM zapise stevce nadaljevanja.
# -Nadaljevanje 0 stikalo izrecno izklopi; brez parametra (-1) velja config (original).
#     .\ponovitve-run.ps1 -Scenarij letenje -Dodatno @('-Nadaljevanje','1')
#
# Q78: -TrajanjeFaze N (server ticki, veckratnik 20, privzeto 600 = M4.16) podaljsa vse tri
# faze; kopenski NPC (0,066 b/t) za 48 blokov proge DW potrebuje ~735 tickov. Odtis ima
# trajanjeFaze, zato se serije z razlicnim trajanjem ne zdruzijo.
#     .\ponovitve-run.ps1 -Scenarij letenje -Dodatno @('-Nadaljevanje','1','-TrajanjeFaze','1200')
#
# M4.17 (D-085): -VidnaCrta 1 pred scenarijem poslje 'rwpath vidno 1' (stikalo RwFlyLineOfSight
# med tekom; config se ne spremeni) in po LET-SUM zapise stevce preverb vidne crte; kombinira se
# z -Nadaljevanje. -VidnaCrta 0 stikalo izrecno izklopi; brez parametra (-1) velja config.
#     .\ponovitve-run.ps1 -Scenarij letenje -Dodatno @('-VidnaCrta','1')
#     .\ponovitve-run.ps1 -Scenarij letenje -Dodatno @('-VidnaCrta','1','-Nadaljevanje','1')

param([string]$JsonPath = '', [switch]$AcceptEula, [int]$ChunkRadius = 2, [int]$WarmupSeconds = 10,
      [int]$ScenarioTimeoutSec = 300, [ValidateSet(-1, 0, 1)][int]$Nadaljevanje = -1, [ValidateSet(-1, 0, 1)][int]$VidnaCrta = -1,
      [ValidateRange(200, 3000)][int]$TrajanjeFaze = 600)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed  = Join-Path $root 'dev\testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'letenje-lib.ps1')
if ($TrajanjeFaze % 20 -ne 0) { throw 'TrajanjeFaze mora biti veckratnik 20 server tickov.' }
$script:LetTrajanjeFaze = $TrajanjeFaze
New-Item -ItemType Directory -Force -Path $audit | Out-Null
$stamp  = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world  = "rwlet-$stamp"
$prefix = Join-Path $audit "m416-letenje-$stamp"
$failures = @()
$srv = $null
$lock = $null
$config = Join-Path $run 'config\CustomNpcs.cfg'

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}

# cmd.exe drzi stdout odprt za pisanje ves cas zagona; branje mora dovoliti souporabo.
function Get-Log([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
    $reader = $null
    try {
        $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8, $true)
        return $reader.ReadToEnd()
    } finally {
        if ($null -ne $reader) { $reader.Dispose() } else { $stream.Dispose() }
    }
}

function Wait-Marker($Server, [string]$Marker, [int]$Count = 1, [int]$Seconds = 30) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $log = Get-LetMarkerText (Get-Log $Server.Log)
        if ([regex]::Matches($log, [regex]::Escape($Marker)).Count -ge $Count) { return $true }
        $crash = Get-ServerSesutje $log
        if ($crash) { throw "Server se je sesul med cakanjem na ${Marker}: $crash" }
        if ($Server.Proc.HasExited) { return $false }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    Write-Host ("  ! timeout {0} s pri cakanju na '{1}'" -f $Seconds, $Marker)
    return $false
}

function Send-Command($Server, [string]$Command) {
    $Server.Proc.StandardInput.WriteLine($Command)
    $Server.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}

function Stop-Server($Server) {
    try { Send-Command $Server 'stop' } catch { }
    if ($Server.Proc.WaitForExit(180000)) { return ($Server.Proc.ExitCode -eq 0) }
    & taskkill.exe /PID $Server.Proc.Id /T /F 2>&1 | Out-Null
    return $false
}

function Start-Server([string]$Tag) {
    $path = "$prefix-$Tag.log"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain "-PrwWorld={0}" > "{1}" 2>&1' -f $world, $path)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    # Rocaj se obdrzi tudi, ce zagon pade, da ga finally zanesljivo pocisti.
    $script:srv = [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $path }
    $done = Wait-Marker $script:srv 'Done (' 1 900
    # 27. 9. (M7.6): ':extractNatives' pade, ce prejsnji JVM se drzi DLL-je; enkrat znova.
    if ((-not $done) -and ((Get-Log $path) -match ':extractNatives')) {
        Write-Host '  ! gradle :extractNatives je padel (zaklenjeni natives prejsnjega JVM-a); cez 20 s se enkrat'
        Start-Sleep -Seconds 20
        $script:srv = [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $path }
        $done = Wait-Marker $script:srv 'Done (' 1 900
    }
    if (-not $done) { throw "Server $Tag se ni zagnal: $path" }
    if ((Get-Log $path) -notmatch ('Preparing level "' + [regex]::Escape($world) + '"')) {
        throw 'Server ni izbral loceno mapo sveta scenarija.'
    }
    $ready = $false
    for ($i = 0; $i -lt 6 -and -not $ready; $i++) {
        $before = [regex]::Matches((Get-Log $path), 'players online').Count
        Send-Command $script:srv 'list'
        $ready = Wait-Marker $script:srv 'players online' ($before + 1) 5
    }
    if (-not $ready) { throw 'Server ne potrdi ukaza list (standardni vhod).' }
    return $script:srv
}

$exitCode = 1
try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'Pogoji'
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'letenje-run'
    $tuji0 = @(Get-TujiJava)
    Check ("ne tece noben tuj gradle/Minecraft ({0})" -f $(if ($tuji0.Count) { $tuji0 -join '; ' } else { '-' })) ($tuji0.Count -eq 0)
    $eula = Join-Path $run 'eula.txt'
    if ((Get-Log $eula) -notmatch 'eula\s*=\s*true') {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni z -AcceptEula." }
        Set-Content -Path $eula -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
    }
    $props = Get-Log (Join-Path $run 'server.properties')
    $propsOk = ($props -match '(?m)^enable-command-block\s*=\s*true') -and ($props -match '(?m)^level-type\s*=\s*FLAT') -and
               ($props -match '(?m)^level-seed\s*=\s*customnpc-rework')
    Check 'server.properties je od testnega sveta (FLAT, seme customnpc-rework, command bloki)' $propsOk
    $configHash = (Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash
    Check 'config dev\run\config\CustomNpcs.cfg obstaja' ([bool]$configHash)
    $cfg = Get-Log $config
    $rw = @([regex]::Matches($cfg, '(?m)^\s*[IBSD]?:?(Rw\w+)=(\S*)') | ForEach-Object { '{0}={1}' -f $_.Groups[1].Value, $_.Groups[2].Value })
    Write-Host ("  stikala configa: {0}" -f ($rw -join ' '))
    $node = Get-Command node -ErrorAction SilentlyContinue
    if ($null -ne $node) {
        & $node.Source --check (Join-Path $seed 'letenje-control.js')
        Check 'skripta krmilnika je sintakticno veljavna (node --check)' ($LASTEXITCODE -eq 0)
    }
    if (Test-Path -LiteralPath (Join-Path $run $world)) { throw 'Svet scenarija ze obstaja.' }
    if ($failures.Count) { throw 'Pogoji zagona niso izpolnjeni; meritve ni.' }

    Step 2 'Zagon A: svez svet, world spawn na 0 4 0'
    $srv = Start-Server 'a'
    Send-Command $srv 'setworldspawn 0 4 0'
    Check 'world spawn nastavljen' (Wait-Marker $srv 'Set the world spawn point')
    Send-Command $srv 'save-all flush'
    Check 'svet shranjen (A)' (Wait-Marker $srv 'Saved the world' 1 180)
    Check 'server A se je cisto ustavil' (Stop-Server $srv)
    $srv = $null
    if ($failures.Count) { throw 'Zagon A ni uspel.' }

    $clones = Join-Path $run "$world\customnpcs\clones\1"
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    foreach ($name in @('LET_Flyer.json', 'LET_Walker.json', 'LET_Target.json')) {
        Copy-Item -LiteralPath (Join-Path $seed "customnpcs\clones\1\$name") -Destination $clones
    }
    $scriptLen = Install-LetController (Join-Path $seed 'customnpcs\clones\1\LET_Control.json') `
                     (Join-Path $seed 'letenje-control.js') (Join-Path $clones 'LET_Control.json') $TrajanjeFaze
    Write-Host ("  krmilnik namescen ({0} znakov skripte, faza {1} server tickov)" -f $scriptLen, $TrajanjeFaze)

    Step 3 'Zagon B: prizorisce'
    $srv = Start-Server 'b'
    $sent = 0
    foreach ($line in (Get-Content -Encoding UTF8 (Join-Path $seed 'letenje-setup-commands.txt'))) {
        $t = $line.Trim()
        if ($t -and -not $t.StartsWith('#')) { Send-Command $srv $t; $sent++ }
    }
    Write-Host ("  poslanih ukazov: {0}" -f $sent)
    Check 'svet shranjen po postavitvi' (Wait-Marker $srv 'Saved the world' 1 120)
    $fills = @([regex]::Matches((Get-LetMarkerText (Get-Log $srv.Log)), '(\d+) blocks filled') | ForEach-Object { [int]$_.Groups[1].Value })
    Write-Host ("  fill: {0}" -f ($fills -join ', '))
    # Zid Z 73x4=292, vogal 20x16=320 in 10x16=160, tri stene labirinta po 10x3=30; lupina
    # labirinta zamenja samo bloke, ki niso ze kamen, zato je njena stevilka samo > 0.
    $fillsOk = ($fills.Count -eq 7) -and ($fills[0] -eq 292) -and ($fills[1] -eq 320) -and ($fills[2] -eq 160) -and
               ($fills[3] -gt 0) -and ($fills[4] -eq 30) -and ($fills[5] -eq 30) -and ($fills[6] -eq 30)
    Check ("LT4: prizorisce je postavljeno (7 ukazov fill, {0} blokov)" -f (($fills | Measure-Object -Sum).Sum)) $fillsOk
    Check 'LT4: nobena postavitev ni zunaj sveta' (-not ((Get-Log $srv.Log) -match 'outside of the world'))

    Step 4 'LT2: pogoj meritve (M2.1d)'
    Send-Command $srv ("rwdiag chunks on {0}" -f $ChunkRadius)
    Check 'ukaz chunks odgovori' (Wait-Marker $srv 'RWDIAG-CHUNKS stanje=on')
    $chunks = [regex]::Match((Get-LetMarkerText (Get-Log $srv.Log)), 'RWDIAG-CHUNKS stanje=on chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+) npc=(\d+)')
    $nChunk = if ($chunks.Success) { [int]$chunks.Groups[1].Value } else { -1 }
    $nZavr  = if ($chunks.Success) { [int]$chunks.Groups[3].Value } else { -1 }
    $nNpc   = if ($chunks.Success) { [int]$chunks.Groups[4].Value } else { -1 }
    Check ("LT2: chunki prisilno nalozeni (chunki={0})" -f $nChunk) ($nChunk -gt 0)
    Check ("LT2: noben chunk ni zavrnjen (zavrnjeni={0})" -f $nZavr) ($nZavr -eq 0)
    Check ("LT2: v svetu je {0} NPC-jev scenarija (najdenih {1})" -f $script:LetNpcSkupaj, $nNpc) ($nNpc -eq $script:LetNpcSkupaj)
    if ($failures.Count) { throw 'Pogoj meritve ni izpolnjen; meritve ni.' }
    Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
    Start-Sleep -Seconds $WarmupSeconds
    Send-Command $srv 'rwdiag on'
    Check 'merjenje vklopljeno' (Wait-Marker $srv 'RWDIAG vklopljen')
    $nadVrstica = ''
    $vidVrstica = ''
    if (($Nadaljevanje -ge 0) -or ($VidnaCrta -ge 0)) { Send-Command $srv 'rwpath reset' }
    if ($VidnaCrta -ge 0) {
        Send-Command $srv ("rwpath vidno {0}" -f $VidnaCrta)
        Check ("M4.17: stikalo RwFlyLineOfSight={0} je vklopljeno med tekom" -f $VidnaCrta) `
            (Wait-Marker $srv (" vidno={0} " -f $VidnaCrta) 1 30)
    }
    if ($Nadaljevanje -ge 0) {
        Send-Command $srv ("rwpath nadaljuj {0}" -f $Nadaljevanje)
        Check ("M4.10: stikalo RwPathResume={0} je vklopljeno med tekom" -f $Nadaljevanje) `
            (Wait-Marker $srv (" nadaljuj={0} " -f $Nadaljevanje) 1 30)
    }

    Step 5 'Scenarij'
    Send-Command $srv 'noppes clone spawn LET_Control 1 0,4,-40'
    Check 'LT1: krmilnik se je oglasil (LET-INIT)' (Wait-Marker $srv 'LET-INIT' 1 60)
    Check 'LT3: proge so sestavljene (LET-SETUP)' (Wait-Marker $srv 'LET-SETUP ' 1 60)
    foreach ($f in $script:LetFaze) {
        Check ("faza {0} se je zacela ({1})" -f $f.Id, $f.Opis) (Wait-Marker $srv ("LET-{0}-START" -f $f.Id) 1 $ScenarioTimeoutSec)
        Check ("faza {0} se je koncala" -f $f.Id) (Wait-Marker $srv ("LET-{0}-END" -f $f.Id) 1 $ScenarioTimeoutSec)
    }
    Check 'LT1: scenarij je prisel do konca (LET-SUM)' (Wait-Marker $srv 'LET-SUM' 1 60)
    if (($Nadaljevanje -ge 0) -or ($VidnaCrta -ge 0)) {
        $pred = [regex]::Matches((Get-LetMarkerText (Get-Log $srv.Log)), 'RWPATH nacin=').Count
        Send-Command $srv 'rwpath'
        Check 'M4.10/M4.17: stevci stikal poti zapisani' (Wait-Marker $srv 'RWPATH nacin=' ($pred + 1) 30)
    }
    if ($VidnaCrta -ge 0) {
        $vid = [regex]::Matches((Get-LetMarkerText (Get-Log $srv.Log)), 'vidno=(\d) (vidKlicev=(\d+) vidDalec=\d+ vidZaprtih=\d+ vidProstih=\d+)')
        if ($vid.Count -gt 0) {
            $zadnjiV = $vid[$vid.Count - 1]
            $vidVrstica = ('vidno={0} {1}' -f $zadnjiV.Groups[1].Value, $zadnjiV.Groups[2].Value)
            Write-Host ("  {0}" -f $vidVrstica)
            Check ("M4.17: stikalo je bilo do konca {0} ({1})" -f $VidnaCrta, $vidVrstica) `
                ($zadnjiV.Groups[1].Value -eq [string]$VidnaCrta)
            if ($VidnaCrta -eq 1) {
                Check ("M4.17: preverba vidne crte je tekla ({0} klicev)" -f $zadnjiV.Groups[3].Value) ([long]$zadnjiV.Groups[3].Value -gt 0)
            }
        } else {
            Check 'M4.17: vrstica stevcev vidne crte je berljiva' $false
        }
    }
    if ($Nadaljevanje -ge 0) {
        $nad = [regex]::Matches((Get-LetMarkerText (Get-Log $srv.Log)), 'nadaljuj=(\d) (nadZacetih=\S+(?: nad\w+=\S+)*)')
        if ($nad.Count -gt 0) {
            $zadnji = $nad[$nad.Count - 1]
            $nadVrstica = ('nadaljuj={0} {1}' -f $zadnji.Groups[1].Value, $zadnji.Groups[2].Value)
            Write-Host ("  {0}" -f $nadVrstica)
            Check ("M4.10: stikalo je bilo do konca {0} ({1})" -f $Nadaljevanje, $nadVrstica) `
                ($zadnji.Groups[1].Value -eq [string]$Nadaljevanje)
        } else {
            Check 'M4.10: vrstica stevcev nadaljevanja je berljiva' $false
        }
    }

    Step 6 'Posnetek meritve in ustavitev'
    Send-Command $srv 'rwdiag dump m416-letenje'
    Check 'posnetek zapisan' (Wait-Marker $srv 'RWDIAG-DUMP ' 1 60)
    Send-Command $srv 'rwdiag off'
    Send-Command $srv 'rwdiag chunks off'
    Check 'server B se je cisto ustavil' (Stop-Server $srv)
    $logPath = $srv.Log
    $srv = $null

    Step 7 'LT1: nobene napake iz moda ali skripte'
    $raw = Get-Log $logPath
    $text = Get-LetMarkerText $raw
    $custom = Get-Log (Join-Path $run 'logs\CustomNPCs-latest.log')
    [IO.File]::WriteAllText("$prefix-customnpcs.log", $custom)
    $modErrors = @([regex]::Matches($raw, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check ("LT1: brez ERROR vrstic iz noppes.* ({0})" -f $modErrors.Count) ($modErrors.Count -eq 0)
    $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" }
    Check 'LT1: brez napake skripte ali izjeme v ticku' (($raw + $custom) -notmatch 'script errored|ScriptException|Exception in server tick loop|ConcurrentModificationException')
    $tujiMed = @(Get-TujiJava)
    Check ("ne tece noben tuj gradle/Minecraft po zagonu ({0})" -f $(if ($tujiMed.Count) { $tujiMed -join '; ' } else { '-' })) ($tujiMed.Count -eq 0)

    Step 8 'LT3, LT5-LT8: veljavnost meritve'
    foreach ($c in (Test-LetMerila $text)) { Check $c.Ime $c.Ok }

    Step 9 'LT9: merilni pogoji nespremenjeni'
    $configHash2 = (Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash
    Check 'LT9: config SHA-256 je pred in po zagonu enak' ($configHash -eq $configHash2)

    Step 10 'Izid po fazah in progah'
    $tabela = Format-LetTabela $text
    $tabela | ForEach-Object { Write-Host $_ }

    Step 11 'Porocilo in zapis zagona'
    $head = @()
    $head += ("# M4.16 - scenarij letenja in dometa, zagon {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm'))
    $head += ''
    $head += 'Scenarij: `docs/scenariji/M4.16-letenje.md`. Svet: `dev/run/' + $world + '`. ' + $(if ($Nadaljevanje -ge 0) { ('Config dev/run, med tekom RwPathResume={0} (M4.10).' -f $Nadaljevanje) } elseif ($VidnaCrta -ge 0) { 'Config dev/run.' } else { 'Original (config dev/run, vsa stikala Rw* kot v configu).' })
    $head += ''
    $head += ('Merila: {0}' -f $(if ($failures.Count -eq 0) { 'LT1-LT9 zelena' } else { ("padlo {0}" -f $failures.Count) }))
    $head += ''
    foreach ($p in $script:LetProge) { $head += ("- proga **{0}**: {1}" -f $p.Id, $p.Opis) }
    foreach ($f in $script:LetFaze) { $head += ("- faza **{0}**: {1}" -f $f.Id, $f.Opis) }
    $head += ''
    $head += $tabela
    $head += ''
    $head += ('Fill: {0}; chunki={1}, NPC={2}; config SHA-256 {3}; stikala: {4}' -f ($fills -join ', '), $nChunk, $nNpc, $configHash, ($rw -join ' '))
    if ($nadVrstica) { $head += ''; $head += ('Stevci M4.10: {0}' -f $nadVrstica) }
    if ($VidnaCrta -ge 0) { $head += ''; $head += ('Med tekom RwFlyLineOfSight={0} (M4.17). Stevci: {1}' -f $VidnaCrta, $vidVrstica) }
    if ($failures.Count -gt 0) {
        $head += ''
        $head += '## Padle preverbe'
        $head += ''
        $failures | ForEach-Object { $head += ("- {0}" -f $_) }
    }
    Set-Content -Path "$prefix.md" -Value $head -Encoding UTF8
    Write-Host ("  porocilo: {0}.md" -f $prefix)

    if ($JsonPath -eq '') { $JsonPath = "$prefix.json" }
    $setup = Read-LetSetup $text
    $odtis = @{
        prizorisce = 'm416-v1'
        proge = (($script:LetProge | ForEach-Object { $_.Id }) -join ',')
        naProgo = $script:LetNaProgo
        npc = $nNpc
        fillBlokov = (($fills | Measure-Object -Sum).Sum - $fills[3])
        trajanjeFaze = $script:LetTrajanjeFaze
        obroc = $ChunkRadius
        ogrevanje = $WarmupSeconds
        config = $configHash
    }
    if ($Nadaljevanje -ge 0) { $odtis['nadaljuj'] = $Nadaljevanje }
    if ($VidnaCrta -ge 0) { $odtis['vidno'] = $VidnaCrta }
    if ($null -ne $setup) {
        foreach ($p in $script:LetProge) { if ($null -ne $setup[$p.Id]) { $odtis["d0.$($p.Id)"] = ('{0:N2}' -f $setup[$p.Id].D0) } }
    }
    $vel = Get-LetVelicine $text
    $zapis = Write-MeritevJson -Path $JsonPath -Paket 'M4.16' -Scenarij 'letenje' -Odtis $odtis -Velicine $vel `
                               -Uspeh ($failures.Count -eq 0) -Padle $failures
    Write-Host ("  zapis zagona: {0} ({1} velicin)" -f $zapis, $vel.Count)

    Step 12 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M4.16 LT1-LT9 USPESNO: meritev je veljavna. Stevilke gredo v docs/meritve/.'
        $exitCode = 0
    } else {
        Write-Host ''
        Write-Host 'M4.16 NEUSPESNO. Padle preverbe:'
        $failures | ForEach-Object { Write-Host "  - $_" }
    }
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M4.16 NEUSPESNO.'
    $exitCode = 1
}
finally {
    if ($null -ne $srv) { try { Stop-Server $srv | Out-Null } catch { } }
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $exitCode
