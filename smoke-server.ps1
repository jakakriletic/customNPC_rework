# M0.5 - dedicated server smoke test.
#
# Dokaze, da se dedicated server zazene s popravljenim dev runtime jarjem, da se
# NPC da ustvariti, da prezivi save in restart ter da se po restartu prebere z
# enako NBT vsebino.
#
# Postopek je namenoma skriptiran, ne rocen: vsi ukazi gredo na standardni vhod
# serverja, izpis pa v audit\m05-server-a.log in audit\m05-server-b.log, da ga
# lahko katerakoli seja ponovi in preveri.
#
# Zagon:  .\smoke-server.ps1 -AcceptEula
#
# -AcceptEula je obvezen ob prvem zagonu. Minecraft EULA je pravni dogovor z
# Mojangom; skripta ga zapise samo, ce to eksplicitno zahtevas s to zastavico.

param([switch]$AcceptEula)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run  = Join-Path $root 'dev\run'
$log  = Join-Path $root 'audit\m05-server-smoke.log'
New-Item -ItemType Directory -Force -Path (Join-Path $root 'audit') | Out-Null
Start-Transcript -Path $log -Force | Out-Null

$LEVEL = 'm05-smoke'
$SEED  = '20260914'

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

function Wait-ForMarker([string]$LogPath, [string]$Marker, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path $LogPath) {
            $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
            if ($c -and $c.Contains($Marker)) { return $true }
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

# Zazene 'gradlew runServer' tako, da ostane standardni vhod odprt cev.
# --no-daemon je nujen: le tako je System.in Gradla resnicni vhod procesa in ga
# JavaExec lahko poda strezniku.
function Start-DevServer([string]$Tag) {
    $outLog = Join-Path $root "audit\m05-server-$Tag.log"
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
    New-Item -ItemType Directory -Force -Path $run | Out-Null

    Step 1 'EULA'
    $eulaFile = Join-Path $run 'eula.txt'
    $eulaOk = (Test-Path $eulaFile) -and ((Get-Content $eulaFile -Raw) -match 'eula\s*=\s*true')
    if (-not $eulaOk) {
        if (-not $AcceptEula) {
            throw "dev\run\eula.txt ni sprejet. Minecraft EULA je pravni dogovor z Mojangom; potrdi ga z zagonom '.\smoke-server.ps1 -AcceptEula'."
        }
        Set-Content -Path $eulaFile -Value "# https://account.mojang.com/documents/minecraft_eula`r`neula=true" -Encoding ASCII
        Write-Host '  EULA zapisana na uporabnikovo eksplicitno zahtevo (-AcceptEula).'
    } else {
        Write-Host '  EULA je ze sprejeta.'
    }

    Step 2 'Deterministicen smoke svet'
    $worldDir = Join-Path $run $LEVEL
    if (Test-Path $worldDir) { Remove-Item $worldDir -Recurse -Force; Write-Host "  odstranjen star $LEVEL" }
    $props = @(
        "level-name=$LEVEL",
        "level-seed=$SEED",
        'level-type=FLAT',
        'generate-structures=false',
        'online-mode=false',
        'spawn-protection=0',
        'view-distance=6',
        'max-players=4',
        'allow-nether=false',
        'spawn-npcs=false',
        'spawn-animals=false',
        'spawn-monsters=false',
        'max-tick-time=-1'
    )
    Set-Content -Path (Join-Path $run 'server.properties') -Value $props -Encoding ASCII
    Write-Host "  level-name=$LEVEL  seed=$SEED  FLAT, brez watchdoga"

    # Zakaj trije zagoni in ne dva: v 1.12.2 CommandSummon javi
    # "Cannot summon the object out of the world" tudi takrat, ko ciljni chunk ni
    # nalozen, ne le pri neveljavnih koordinatah. Spawn superflat sveta lahko pade
    # do 256 blokov od izhodisca, zato chunk 0,0 ni nujno med spawn chunki. Prvi
    # zagon zato pribije world spawn na 0,5,0; po restartu so ti chunki zajamceno
    # nalozeni. Stranski ucinek je determinističen testni svet, ki ga rabi tudi M0.6.

    Step 3 'Zagon A: pribij world spawn na 0 5 0'
    $a = Start-DevServer 'a'
    Check 'server A je dosegel "Done ("'           (Wait-ForMarker $a.Log 'Done (' 900)
    if ($failures.Count -eq 0) {
        Check 'FML je nalozil CustomNPCs'          (Wait-ForMarker $a.Log 'customnpcs' 5)
        Send-Command $a 'setworldspawn 0 5 0'
        Check 'world spawn nastavljen'             (Wait-ForMarker $a.Log 'Set the world spawn point' 60)
        Send-Command $a 'save-all flush'
        Check 'svet shranjen (A)'                  (Wait-ForMarker $a.Log 'Saved the world' 180)
    }
    Check 'server A se je cisto ustavil'           (Stop-DevServer $a)

    Step 4 'Zagon B: summon NPC, preveri, shrani, stop'
    $b = Start-DevServer 'b'
    Check 'server B je dosegel "Done ("'           (Wait-ForMarker $b.Log 'Done (' 900)
    if ($failures.Count -eq 0) {
        Send-Command $b 'summon customnpcs:CustomNpc 0 5 0 {Name:"SmokeNPC"}'
        Check 'NPC ustvarjen'                      (Wait-ForMarker $b.Log 'Object successfully summoned' 60)
        Send-Command $b 'execute @e[type=customnpcs:CustomNpc] ~ ~ ~ say M05-NPC-PRESENT'
        Check 'NPC viden pred restartom'           (Wait-ForMarker $b.Log '[SmokeNPC] M05-NPC-PRESENT' 60)
        Send-Command $b 'save-all flush'
        Check 'svet shranjen (B)'                  (Wait-ForMarker $b.Log 'Saved the world' 180)
    }
    Check 'server B se je cisto ustavil'           (Stop-DevServer $b)

    Step 5 'Zagon C: restart, NPC mora se obstajati z enako NBT vsebino'
    $c = Start-DevServer 'c'
    Check 'server C je dosegel "Done ("'           (Wait-ForMarker $c.Log 'Done (' 900)
    if ($failures.Count -eq 0) {
        Send-Command $c 'execute @e[type=customnpcs:CustomNpc] ~ ~ ~ say M05-NPC-PRESENT'
        Check 'NPC je prezivel save + restart'     (Wait-ForMarker $c.Log '[SmokeNPC] M05-NPC-PRESENT' 120)
    }
    Check 'server C se je cisto ustavil'           (Stop-DevServer $c)

    Step 6 'Izid'
    if ($failures.Count -eq 0) {
        Write-Host 'M0.5 USPESNO: dedicated server se zazene, NPC prezivi save in restart.'
    } else {
        Write-Host 'M0.5 NEUSPESNO. Padle preverbe:'
        $failures | ForEach-Object { Write-Host "  - $_" }
    }
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'M0.5 NEUSPESNO. Izpis je v audit\m05-server-smoke.log ter audit\m05-server-a.log in -b.log'
}
finally {
    Write-Host ''
    Write-Host "===== KONEC $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ====="
    Stop-Transcript | Out-Null
}
