# M0.6 - skriptiran zagon scenarija testnega sveta.
#
# Zakaj skriptirano: rocno lepljenje dev\testworld\setup-commands.txt v gradle
# konzolo je 15. 9. tiho odpovedalo - v logu ni bilo nobenega 'clone spawn', zato
# je 'execute @e[tag=testworld]' nasel nic in je scenarij izgledal, kot da svet ne
# dela. Ukazi zdaj gredo na standardni vhod serverja, izpis pa v audit\m06-*.log.
#
# Mehanizem zagona je enak kot v smoke-server.ps1 (M0.5): --no-daemon je nujen,
# ker le tako Gradlov System.in postane resnicni vhod procesa.
#
# Zagon:
#     .\testworld-run.ps1              # postavi svet in pozene cel scenarij
#     .\testworld-run.ps1 -SkipSeed    # obdrzi svet, a osvezi fixture NPC-je
#     .\testworld-run.ps1 -AcceptEula  # prvic, ce dev\run\eula.txt se ni sprejet

param([switch]$SkipSeed, [switch]$AcceptEula)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

$expected = @('T_Stand','T_Wander','T_Flyer','T_Carrier','T_Rider','T_Follower','T_Trader','T_Scripted')

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

# Cakanje se prekine takoj, ko se proces konca. Brez tega je 15. 9. skripta visela
# 900 s: Gradle je server preskocil ('Task :runServer UP-TO-DATE'), build se je
# koncal v 21 s, marker pa seveda ni nikoli prisel.
function Wait-ForCount($Srv, [string]$Marker, [int]$Count, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        $n = ([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count
        if ($n -ge $Count) { return $true }
        if ($Srv.Proc.HasExited) {
            Start-Sleep -Milliseconds 500   # zadnji zapis v log
            $n = ([regex]::Matches((Get-LogText $Srv.Log), [regex]::Escape($Marker))).Count
            if ($n -ge $Count) { return $true }
            Write-Host ("  ! proces se je koncal, preden se je pojavil marker '{0}'" -f $Marker)
            if ((Get-LogText $Srv.Log).Contains('runServer UP-TO-DATE')) {
                Write-Host '  ! Gradle je runServer preskocil kot UP-TO-DATE.'
                Write-Host '    Potrebuje popravek v dev/build.gradle: outputs.upToDateWhen { false }'
            }
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

function Count-Marker([string]$LogPath, [string]$Marker) {
    return ([regex]::Matches((Get-LogText $LogPath), [regex]::Escape($Marker))).Count
}

function Start-DevServer([string]$Tag) {
    $outLog = Join-Path $audit "m06-testworld-$Tag.log"
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

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'EULA'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) {
            throw "dev\run\eula.txt ni sprejet. Potrdi ga z zagonom '.\testworld-run.ps1 -AcceptEula'."
        }
        New-Item -ItemType Directory -Force -Path $run | Out-Null
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
    } else {
        Write-Host '  EULA je ze sprejeta.'
    }

    Step 2 'Seme testnega sveta'
    # -SkipSeed obdrzi svet, a fixture datoteke se vseeno osvezijo. Brez tega je
    # 15. 9. scenarij tekel proti stari clone shrambi v svetu in nov marker
    # TW-SCRIPT-TICK-10 se ni mogel pojaviti, ker je bila tam se skripta s pragom 200.
    if ($SkipSeed) {
        Write-Host '  -SkipSeed: svet ostane, fixture NPC-ji se osvezijo.'
        & (Join-Path $root 'testworld.ps1') -KeepWorld
    } else {
        & (Join-Path $root 'testworld.ps1')
    }

    $setupFile = Join-Path $root 'dev\testworld\setup-commands.txt'
    $commands = @(Get-Content $setupFile |
        Where-Object { $_.Trim() -ne '' -and -not $_.TrimStart().StartsWith('#') })
    Check 'setup-commands.txt ima ukaze' ($commands.Count -gt 0)

    # Zakaj trije zagoni: v 1.12.2 spawn superflat sveta lahko pade dalec od
    # izhodisca, konzolni ukazi pa tecejo na (0,0,0). Prvi zagon zato pribije
    # world spawn na 0 4 0; sele po restartu je chunk 0,0 zajamceno nalozen in
    # 'fill' ter 'clone spawn' zagotovo delujeta.

    Step 3 'Zagon A: pribij world spawn na 0 4 0'
    $a = Start-DevServer 'a'
    Check 'server A je dosegel "Done ("'      (Wait-ForMarker $a 'Done (' 900)
    if ($failures.Count -eq 0) {
        Send-Command $a 'setworldspawn 0 4 0'
        Check 'world spawn nastavljen'        (Wait-ForMarker $a 'Set the world spawn point' 60)
        Send-Command $a 'save-all flush'
        Check 'svet shranjen (A)'             (Wait-ForMarker $a 'Saved the world' 180)
    }
    Check 'server A se je cisto ustavil'      (Stop-DevServer $a)
    if ($failures.Count -gt 0) { throw "Zagon A ni uspel; nadaljnji zagoni nimajo smisla. Glej $($a.Log)" }

    Step 4 'Zagon B: postavi prizorisce in NPC-je'
    $b = Start-DevServer 'b'
    Check 'server B je dosegel "Done ("'      (Wait-ForMarker $b 'Done (' 900)
    if ($failures.Count -eq 0) {
        foreach ($c in $commands) { Send-Command $b $c }
        # 'noppes clone spawn' ob uspehu ne izpise nicesar (CmdClone.java:112-170),
        # zato je edini dokaz spawna stetje TW-OK nize spodaj. Preverimo pa, da ni
        # javil napake za neznano ime ali manjkajoco lokacijo.
        Start-Sleep -Seconds 3
        Check 'noben clone spawn ni javil napake' (
            -not (Get-LogText $b.Log).Contains('Unknown npc') -and
            -not (Get-LogText $b.Log).Contains('Location needed'))
        Send-Command $b 'noppes clone list 1'
        Check 'clone list izpise vseh 8 imen' (Wait-ForMarker $b '--- Stored NPCs ---' 60)
        Send-Command $b 'execute @e[tag=testworld] ~ ~ ~ say TW-OK'
        Check 'TW-OK 8x pred restartom'       (Wait-ForCount $b 'TW-OK' 8 60)
        # natanko 8, ne vec: podvojeni NPC-ji pomenijo, da prizorisce ni bilo pocisceno
        Check 'natanko 8 NPC-jev, brez podvojenih' ((Count-Marker $b.Log 'TW-OK') -eq 8)
        Check 'skripta na T_Scripted tece'    (Wait-ForMarker $b 'TW-SCRIPT-OK' 60)
        Send-Command $b 'save-all flush'
        Check 'svet shranjen (B)'             (Wait-ForMarker $b 'Saved the world' 180)
    }
    Check 'server B se je cisto ustavil'      (Stop-DevServer $b)
    if ($failures.Count -gt 0) { throw "Zagon B ni uspel; restart nima smisla. Glej $($b.Log)" }

    Step 5 'Zagon C: restart, prizorisce mora prezivet'
    $c2 = Start-DevServer 'c'
    Check 'server C je dosegel "Done ("'      (Wait-ForMarker $c2 'Done (' 900)
    if ($failures.Count -eq 0) {
        Send-Command $c2 'execute @e[tag=testworld] ~ ~ ~ say TW-OK'
        Check 'TW-OK 8x po restartu'          (Wait-ForCount $c2 'TW-OK' 8 120)
        Check 'natanko 8 NPC-jev po restartu'  ((Count-Marker $c2.Log 'TW-OK') -eq 8)
        Check 'skripta tece tudi po restartu' (Wait-ForMarker $c2 'TW-SCRIPT-OK' 60)
        # 'tick' hook se sprozi vsak 10. tick (EntityNPCInterface:358), torej 2x na
        # sekundo. 10 klicev je ~5 s sveta; to dokaze, da scripting tece naprej in
        # ne le ob inicializaciji.
        Check 'script tick hook tece'         (Wait-ForMarker $c2 'TW-SCRIPT-TICK-10' 90)
    }
    Check 'server C se je cisto ustavil'      (Stop-DevServer $c2)

    Step 6 'Merila W1-W8'
    & (Join-Path $root 'verify-testworld.ps1') -LogPath $b.Log
    $vb = $LASTEXITCODE
    & (Join-Path $root 'verify-testworld.ps1') -LogPath $c2.Log -Restart
    $vc = $LASTEXITCODE
    Check 'verify-testworld (zagon B)' ($vb -eq 0)
    Check 'verify-testworld (zagon C)' ($vc -eq 0)

    Step 7 'Izid'
    Write-Host ("  TW-OK       B={0}  C={1}" -f (Count-Marker $b.Log 'TW-OK'), (Count-Marker $c2.Log 'TW-OK'))
    Write-Host ("  TW-SCRIPT-OK B={0}  C={1}" -f (Count-Marker $b.Log 'TW-SCRIPT-OK'), (Count-Marker $c2.Log 'TW-SCRIPT-OK'))
    Write-Host ("  TW-SCRIPT-TICK-10 C={0}" -f (Count-Marker $c2.Log 'TW-SCRIPT-TICK-10'))
    if ($failures.Count -eq 0) {
        Write-Host ''
        Write-Host 'M0.6 USPESNO: testni svet se postavi, NPC-ji in skripta prezivijo restart.'
        exit 0
    }
    Write-Host ''
    Write-Host 'M0.6 NEUSPESNO. Padle preverbe:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    Write-Host 'Izpisi: audit\m06-testworld-a.log, -b.log, -c.log'
    exit 1
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M0.6 NEUSPESNO. Izpisi so v audit\m06-testworld-*.log'
    exit 1
}
