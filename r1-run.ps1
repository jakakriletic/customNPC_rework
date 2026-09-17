# M2.2 - skriptirana reprodukcija R1 (NPC jaha NPC), merila E1-E6.
#
# Scenarij in razlaga: docs/scenariji/M2.2-R1.md
#
# Dve vzporedni progi hkrati v istem svetu in istem ticku:
#   proga M  8 nosilcev Z jahaci   - merjeno stanje
#   proga S  8 nosilcev BREZ njih  - kontrola
# Brez kontrole razlika ne pomeni nicesar.
#
# Vrstni red je pomemben: chunki se prisilno nalozijo in ogrejejo PREDEN se spawna
# krmilni NPC, ker njegova skripta zacne steti takoj ob spawnu. Brez tega bi se
# scenarij po 300 tickih ustavil sredi faze A in bi izgledal kot okvara AI
# (WorldServer.updateEntities:628-644, glej M2.1d).
#
# Zagon:
#     .\r1-run.ps1
#     .\r1-run.ps1 -AcceptEula
#     .\r1-run.ps1 -ChunkRadius 3     # sirsi obroc, ce NPC-ji zaidejo s proge

param([switch]$AcceptEula, [int]$ChunkRadius = 2, [int]$WarmupSeconds = 10,
      [int]$ScenarioTimeoutSec = 180)

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
    $outLog = Join-Path $audit 'm22-r1.log'
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

# "R1-MOUNT n=8 od=8 progaM=8 progaS=8 ..." -> [int[]](n, od, progaM, progaS)
function Read-Mount([string]$LogPath) {
    $m = [regex]::Match((Get-LogText $LogPath),
        'R1-MOUNT n=(\d+) od=(\d+) progaM=(\d+) progaS=(\d+)')
    if (-not $m.Success) { return @(-1, -1, -1, -1) }
    return @([int]$m.Groups[1].Value, [int]$m.Groups[2].Value,
             [int]$m.Groups[3].Value, [int]$m.Groups[4].Value)
}

# Vse vzorcne vrstice ene faze in proge, kot objekti.
function Read-Samples([string]$LogPath, [string]$Phase, [string]$Lane) {
    $pattern = 'R1-S faza=' + $Phase + ' tick=(\d+) proga=' + $Lane +
               ' navig=(\d+)/(\d+) prevozenoPovp=([\d.]+) prevozenoMax=([\d.]+)' +
               ' razpon=([\d.]+) doCiljaMin=([\d.]+) doCiljaPovp=([\d.]+)' +
               ' jahacev=(-?\d+) odstopMax=([\d.-]+)'
    $out = @()
    foreach ($m in [regex]::Matches((Get-LogText $LogPath), $pattern)) {
        $out += [pscustomobject]@{
            Tick         = [int]$m.Groups[1].Value
            Navig        = [int]$m.Groups[2].Value
            Skupaj       = [int]$m.Groups[3].Value
            PrevozenoAvg = [double]$m.Groups[4].Value
            PrevozenoMax = [double]$m.Groups[5].Value
            Razpon       = [double]$m.Groups[6].Value
            DoCiljaMin   = [double]$m.Groups[7].Value
            DoCiljaAvg   = [double]$m.Groups[8].Value
            Jahacev      = [int]$m.Groups[9].Value
            OdstopMax    = $m.Groups[10].Value
        }
    }
    return $out
}

function Show-Lane([string]$Phase, [string]$Lane, $Samples) {
    if ($Samples.Count -eq 0) {
        Write-Host ("  faza {0} proga {1}: NI VZORCEV" -f $Phase, $Lane)
        return
    }
    $last = $Samples[$Samples.Count - 1]
    Write-Host ("  faza {0} proga {1}: vzorcev={2} | konec: navig={3}/{4} prevozeno(povp/max)={5}/{6} razpon={7} doCilja(min/povp)={8}/{9} jahacev={10} odstop={11}" -f `
        $Phase, $Lane, $Samples.Count, $last.Navig, $last.Skupaj,
        $last.PrevozenoAvg, $last.PrevozenoMax, $last.Razpon,
        $last.DoCiljaMin, $last.DoCiljaAvg, $last.Jahacev, $last.OdstopMax)
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA in fixture'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\r1-run.ps1 -AcceptEula'." }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    }

    # Merilni kanal scenarija je '/say' iz skripte krmilnika. NPCWrapper.executeCommand
    # (reference-src/.../wrapper/NPCWrapper.java:201) vrze CustomNPCsException, ce command
    # bloki niso vklopljeni, in NoppesUtilServer.runCommand (:226) samo zapise opozorilo.
    # Brez te preverbe bi scenarij padel brez ene same vrstice R1-*, kar izgleda kot okvara
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
            Write-Host '  enable-command-block je bil popravljen na true (brez tega ni izpisa R1-*)'
        }
    } else {
        Write-Host '  ! dev\run\server.properties se ne obstaja; ustvari ga prvi zagon.'
        Write-Host '    Ce padejo vse preverbe R1-*, preveri enable-command-block in pozeni znova.'
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
    foreach ($f in (Get-ChildItem -Path $srcClones -Filter 'R1_*.json')) {
        Copy-Item $f.FullName -Destination $dstClones -Force
        $copied++
    }
    Check ("stiri R1 fixture datoteke so v svetu (kopiranih {0})" -f $copied) ($copied -eq 4)
    if ($failures.Count -gt 0) { throw "Fixture manjkajo v $srcClones" }

    Step 2 'Zagon serverja'
    $s = Start-DevServer
    Check 'server je dosegel "Done ("' (Wait-ForMarker $s 'Done (' 900)
    if ($failures.Count -gt 0) { throw "Server se ni zagnal. Glej $($s.Log)" }

    Step 3 'Prizorisce in fixture NPC-ji'
    $null = Send-File $s (Join-Path $seed 'r1-setup-commands.txt')
    Start-Sleep -Seconds 3

    Step 4 'E4: pogoj meritve (M2.1d)'
    Send-Command $s ("rwdiag chunks on {0}" -f $ChunkRadius)
    Check 'ukaz chunks odgovori' (Wait-ForMarker $s 'RWDIAG-CHUNKS' 30)
    $chunks = [regex]::Match((Get-LogText $s.Log),
        'RWDIAG-CHUNKS stanje=on chunki=(\d+) tiketi=(\d+) obroc=\d+ zavrnjeni=(\d+) npc=(\d+)')
    Check 'E4: chunki so prisilno nalozeni' ($chunks.Success -and ([int]$chunks.Groups[1].Value -gt 0))
    if ($chunks.Success) {
        Write-Host ("  chunki={0} tiketi={1} zavrnjeni={2} npc={3}" -f `
            $chunks.Groups[1].Value, $chunks.Groups[2].Value, $chunks.Groups[3].Value, $chunks.Groups[4].Value)
        Check ("E4: noben chunk ni bil zavrnjen (zavrnjeni={0})" -f $chunks.Groups[3].Value) ([int]$chunks.Groups[3].Value -eq 0)
        # 8 nosilcev M + 8 jahacev + 8 nosilcev S + 2 cilja = 26. Krmilnik se ni spawnan.
        Check ("E4: v svetu je 26 R1 NPC-jev (najdenih {0})" -f $chunks.Groups[4].Value) ([int]$chunks.Groups[4].Value -eq 26)
    }
    Write-Host ("  ogrevanje {0} s" -f $WarmupSeconds)
    Start-Sleep -Seconds $WarmupSeconds
    Send-Command $s 'rwdiag on'
    Check 'merjenje vklopljeno' (Wait-ForMarker $s 'RWDIAG vklopljen' 30)

    Step 5 'Scenarij'
    # Sele zdaj: skripta krmilnika zacne steti ob spawnu.
    Send-Command $s 'noppes clone spawn R1_Control 1 0,4,4'
    Check 'E3: krmilnik se je oglasil (R1-INIT)' (Wait-ForMarker $s 'R1-INIT' 60)
    Check 'E1: mount je izveden (R1-MOUNT)' (Wait-ForMarker $s 'R1-MOUNT ' 60)
    $mount = Read-Mount $s.Log
    Check ("E1: vseh 8 jahacev je na nosilcu (n={0} od={1})" -f $mount[0], $mount[1]) ($mount[0] -eq 8)
    Check ("E1: progi sta polni (M={0} S={1})" -f $mount[2], $mount[3]) (($mount[2] -eq 8) -and ($mount[3] -eq 8))

    Check 'faza A se je zacela' (Wait-ForMarker $s 'R1-A-START' 60)
    Check 'faza A se je koncala' (Wait-ForMarker $s 'R1-A-END' $ScenarioTimeoutSec)
    Check 'faza B se je zacela' (Wait-ForMarker $s 'R1-B-START' 60)
    Check 'E3: scenarij je prisel do konca (R1-SUM)' (Wait-ForMarker $s 'R1-SUM' $ScenarioTimeoutSec)

    Step 6 'Posnetek meritve in ustavitev'
    Send-Command $s 'rwdiag dump m22-r1'
    Check 'posnetek zapisan' (Wait-ForMarker $s 'RWDIAG-DUMP ' 60)
    Send-Command $s 'rwdiag off'
    Send-Command $s 'rwdiag chunks off'
    Check 'ticketi sproscen' (Wait-ForCount $s 'RWDIAG-CHUNKS stanje=off' 1 30)
    Check 'server se je cisto ustavil' (Stop-DevServer $s)

    Step 7 'E3: nobene napake iz moda'
    $log = Get-LogText $s.Log
    $modErrors = @([regex]::Matches($log, '(?m)^.*\bERROR\b.*noppes\..*$') | ForEach-Object { $_.Value })
    Check 'brez ERROR vrstic iz noppes.*' ($modErrors.Count -eq 0)
    if ($modErrors.Count -gt 0) { $modErrors | Select-Object -First 5 | ForEach-Object { Write-Host "      $_" } }
    Check 'brez "script errored"' (-not $log.Contains('script errored'))

    Step 8 'E5 in E6: izid po fazah in progah'
    $result = @{}
    foreach ($ph in @('A', 'B')) {
        foreach ($lane in @('M', 'S')) {
            $sm = @(Read-Samples $s.Log $ph $lane)
            $result["$ph$lane"] = $sm
            Show-Lane $ph $lane $sm
        }
    }
    foreach ($key in @('AM', 'AS', 'BM', 'BS')) {
        Check ("E5: faza/proga {0} ima vsaj 20 vzorcev (ima {1})" -f $key, $result[$key].Count) ($result[$key].Count -ge 20)
    }
    if ($result['AS'].Count -gt 0) {
        $lastAS = $result['AS'][$result['AS'].Count - 1]
        Check ("E2: kontrolna proga se je premaknila vsaj 20 blokov (prevozenoMax={0})" -f $lastAS.PrevozenoMax) ($lastAS.PrevozenoMax -ge 20)
    } else {
        Check 'E2: kontrolna proga ima vzorce' $false
    }

    Write-Host ''
    Write-Host 'E6: razlika M proti S (konec vsake faze) - to je ugotovitev, ne absolutne vrednosti:'
    foreach ($ph in @('A', 'B')) {
        $m = $result["${ph}M"]; $sS = $result["${ph}S"]
        if ($m.Count -eq 0 -or $sS.Count -eq 0) { continue }
        $lm = $m[$m.Count - 1]; $ls = $sS[$sS.Count - 1]
        Write-Host ("  faza {0}  navig M={1}/{2} S={3}/{4} | prevozenoMax M={5} S={6} | razpon M={7} S={8} | doCiljaMin M={9} S={10}" -f `
            $ph, $lm.Navig, $lm.Skupaj, $ls.Navig, $ls.Skupaj,
            $lm.PrevozenoMax, $ls.PrevozenoMax, $lm.Razpon, $ls.Razpon,
            $lm.DoCiljaMin, $ls.DoCiljaMin)
    }

    Step 9 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M2.2 E1-E6 USPESNO: reprodukcija je veljavna. Stevilke zgoraj gredo v docs/meritve/.'
        Write-Host ("Izpis:    {0}" -f $s.Log)
        Write-Host ("Posnetek: {0}" -f $dumps)
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.2 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host ("Izpis: {0}" -f $s.Log)
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M2.2 NEUSPESNO. Izpis je v audit\m22-r1.log'
    exit 1
}
