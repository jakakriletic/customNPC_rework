# M3.6 - zakasnitev napada pri NPC-ju, ki tava (prioriteta EntityAIAttackTarget proti
#        EntityAIWander), A/B med nacinoma /rwattack 0 in 1.
#
# Scenarij in razlaga: docs/scenariji/M3.6-napad-med-tavanjem.md
#
# Dve progi hkrati v istem svetu in istem ticku:
#   proga T  8 NPC-jev, ki tavajo   - merjeno stanje
#   proga S  8 NPC-jev, ki stojijo  - kontrola (cista reakcija napada)
# Trije krogi: ukaz setAttackTarget, 200 tickov opazovanja, vrnitev na start.
#
# A1  M36-FIND: 8 + 8 NPC-jev in oba cilja
# A2  vsaj 8 vzorcev NPC-jev, ki ob ukazu tavajo (sicer poskus ni izveden)
# A3  kontrola: vsi napadejo, najvecja zakasnitev <= 20 tickov
# A4  nacin 0: diagnoza - ali tavajoci zacnejo napad pozneje od kontrole (napaka ponovljena)
# A5  nacin 1: preverba - vsi tavajoci zacnejo napad najvec 10 tickov za kontrolo
# A6  brez ERROR iz noppes.* in brez "script errored"; scenarij pride do M36-SUM
#
# Zagon (najprej .\testworld.ps1, ce je svet od drugega scenarija):
#     .\m36-run.ps1              # nacin 0 = original, izpis audit\m36-napad-n0.log
#     .\m36-run.ps1 -Nacin 1     # popravek M3.6,       izpis audit\m36-napad-n1.log

param([switch]$AcceptEula, [int]$ChunkRadius = 2, [int]$WarmupSeconds = 10,
      [int]$ScenarioTimeoutSec = 180,
      [ValidateSet(0, 1)][int]$Nacin = 0)

$ErrorActionPreference = 'Stop'
$root   = $PSScriptRoot
$run    = Join-Path $root 'dev\run'
$audit  = Join-Path $root 'audit'
$seed   = Join-Path $root 'dev\testworld'
$dumps  = Join-Path $run 'logs\rwdiag'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        $n = ([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count
        if ($n -ge $Count) { return $true }
        if ($Srv.Proc.HasExited) {
            Start-Sleep -Milliseconds 500
            $n = ([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count
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
    $outLog = Join-Path $audit "m36-napad-n$Nacin.log"
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

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA in fixture'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\m36-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # Merilni kanal scenarija je '/say' iz skripte krmilnika. NPCWrapper.executeCommand
    # (reference-src/.../wrapper/NPCWrapper.java:201) vrze CustomNPCsException, ce command
    # bloki niso vklopljeni, in NoppesUtilServer.runCommand (:226) samo zapise opozorilo.
    # Brez te preverbe bi scenarij padel brez ene same vrstice M36-*, kar izgleda kot okvara
    # skripte, ne kot napacna nastavitev serverja.
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
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa M36-*)'
        }
    } else {
        Write-Host '  ! dev\run\server.properties se ne obstaja; ustvari ga prvi zagon.'
        Write-Host '    Ce padejo vse preverbe M36-*, preveri enable-command-block in pozeni znova.'
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
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'M36_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("stiri M36 fixture datoteke so v svetu (kopiranih {0})" -f $copied) ($copied -eq 4)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones" }

    Step 2 'Zagon serverja'
    $s = Start-DevServer
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    Step 3 'Prizorisce in fixture NPC-ji'
    $null = Send-File $s (Join-Path $seed 'm36-setup-commands.txt')
    Start-Sleep -Seconds 3

    Step 4 'Pogoj meritve (M2.1d) in nacin'
    Send-Command $s ("rwdiag chunks on {0}" -f $ChunkRadius)
    Check 'ukaz chunks odgovori' (Wait-ForMarker $s 'RWDIAG-CHUNKS' 30)
    $chunks = [regex]::Match((Get-LogText $s.Log),
        'RWDIAG-CHUNKS stanje=on chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+) npc=(\d+)')
    Check 'chunki so prisilno nalozeni' ($chunks.Success -and ([int]$chunks.Groups[1].Value -gt 0))
    if ($chunks.Success) {
        Check ("noben chunk ni bil zavrnjen (zavrnjeni={0})" -f $chunks.Groups[3].Value) ([int]$chunks.Groups[3].Value -eq 0)
        # 8 tavajocih + 8 stojecih + 2 cilja = 18. Krmilnik se ni spawnan.
        Check ("v svetu je 18 M36 NPC-jev (najdenih {0})" -f $chunks.Groups[4].Value) ([int]$chunks.Groups[4].Value -eq 18)
    }
    # Nacin se poslje vedno, tudi 0: prioritete so del AI, ki se sestavi ob spawnu, in
    # ukaz ob spremembi AI vseh NPC-jev sestavi znova (CommandRwAttack). Izpis potrdi nacin.
    Send-Command $s "rwattack $Nacin"
    Check ("M3.6: nacin nastavljen (RWATTACK nacin={0})" -f $Nacin) (Wait-ForMarker $s "RWATTACK nacin=$Nacin" 30)
    Write-Host ("  ogrevanje {0} s (NPC-ji proge T medtem ze tavajo)" -f $WarmupSeconds)
    Start-Sleep -Seconds $WarmupSeconds

    Step 5 'Scenarij'
    Send-Command $s 'noppes clone spawn M36_Control 1 74,4,30'
    Check 'krmilnik se je oglasil (M36-INIT)' (Wait-ForMarker $s 'M36-INIT' 60)
    Check 'A1: krmilnik je nasel NPC-je (M36-FIND)' (Wait-ForMarker $s 'M36-FIND ' 60)
    $find = [regex]::Match((Get-LogText $s.Log), 'M36-FIND tava=(\d+) stoji=(\d+) ciljT=(\w+) ciljS=(\w+)')
    Check ("A1: 8 tavajocih, 8 stojecih, oba cilja ({0})" -f $find.Value) `
        ($find.Success -and [int]$find.Groups[1].Value -eq 8 -and [int]$find.Groups[2].Value -eq 8 -and
         $find.Groups[3].Value -eq 'da' -and $find.Groups[4].Value -eq 'da')
    Check 'A6: scenarij je prisel do konca (M36-SUM)' (Wait-ForMarker $s 'M36-SUM' $ScenarioTimeoutSec)

    Step 6 'Ustavitev'
    Send-Command $s 'rwdiag chunks off'
    Check 'ticketi sproscen' (Wait-ForCount $s 'RWDIAG-CHUNKS stanje=off' 1 30)
    Check 'server se je cisto ustavil' (Stop-DevServer $s)

    Step 7 'A6: nobene napake iz moda'
    $log = Get-LogText $s.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'brez "script errored"' (-not $log.Contains('script errored'))

    Step 8 'Izid po krogih in A2-A5'
    foreach ($m in [regex]::Matches($log, 'M36-NAPAD krog=\d+ navigT=\d+/\d+ navigS=\d+/\d+')) { Write-Host ("  {0}" -f $m.Value) }
    foreach ($m in [regex]::Matches($log, 'M36-R krog=\d+ proga=\w n=\d+ tavajocih=\d+ lat=[-t\d,]+')) { Write-Host ("  {0}" -f $m.Value) }
    $pat = 'M36-LAT proga={0} vzorcev=(\d+) tavajocih=(\d+) nikoli=(\d+) latTavMed=(-?[\d.]+) latTavMax=(-?\d+) latOstaliMed=(-?[\d.]+) latOstaliMax=(-?\d+)'
    $lt = [regex]::Match($log, ($pat -f 'T'))
    $ls = [regex]::Match($log, ($pat -f 'S'))
    if (-not ($lt.Success -and $ls.Success)) {
        Check 'scenarij je izpisal M36-LAT za obe progi' $false
    } else {
        Write-Host ("  {0}" -f $lt.Value)
        Write-Host ("  {0}" -f $ls.Value)
        $tTav = [int]$lt.Groups[2].Value; $tNikoli = [int]$lt.Groups[3].Value
        $tTavMed = [double]$lt.Groups[4].Value; $tTavMax = [int]$lt.Groups[5].Value
        $sNikoli = [int]$ls.Groups[3].Value
        $sMax = [math]::Max([int]$ls.Groups[5].Value, [int]$ls.Groups[7].Value)
        Check ("A2: vsaj 8 vzorcev NPC-jev, ki ob ukazu tavajo (ima {0})" -f $tTav) ($tTav -ge 8)
        Check ("A3: kontrola - vsi napadejo (nikoli={0})" -f $sNikoli) ($sNikoli -eq 0)
        Check ("A3: kontrola - najvecja zakasnitev <= 20 tickov (je {0})" -f $sMax) (($sMax -ge 0) -and ($sMax -le 20))
        Write-Host ''
        Write-Host ("  zakasnitev tavajocih: mediana {0}, max {1} tickov, brez napada {2} | kontrola max {3}" -f $tTavMed, $tTavMax, $tNikoli, $sMax)
        if ($Nacin -eq 0) {
            # Diagnoza, ne preverba: oba izida sta veljavna rezultata poskusa.
            if ($tTavMed -gt ($sMax + 10) -or $tNikoli -gt 0) {
                Write-Host '  A4 (nacin 0): tavajoci zacnejo napad pozneje od kontrole - napaka originala PONOVLJENA.'
            } else {
                Write-Host '  A4 (nacin 0): tavajoci napadejo tako hitro kot kontrola - napaka se NI ponovila.'
            }
        } else {
            Check ("A5: vsi tavajoci napadejo (brez napada {0})" -f $tNikoli) ($tNikoli -eq 0)
            Check ("A5: tavajoci zacnejo napad najvec 10 tickov za kontrolo (max {0} proti {1})" -f $tTavMax, $sMax) `
                (($tTavMax -ge 0) -and ($tTavMax -le ($sMax + 10)))
        }
    }

    Step 9 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host ("M3.6 (nacin {0}) USPESNO. Stevilke zgoraj gredo v docs/meritve/." -f $Nacin)
        Write-Host ("Izpis: {0}" -f $s.Log)
        exit 0
    }
    Write-Host ''
    Write-Host 'M3.6 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $s.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host ("M3.6 NEUSPESNO. Izpis je v audit\m36-napad-n{0}.log" -f $Nacin)
    exit 1
}
