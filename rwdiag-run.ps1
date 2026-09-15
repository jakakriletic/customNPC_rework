# M2.1 - skriptiran preizkus instrumentacije (merila D4-D7).
#
# Zakaj skriptirano: rocno tipkanje ukazov v gradle konzolo je 15. 9. ze dvakrat
# tiho odpovedalo - ukaz ni prisel do serverja, v logu ni bilo nicesar in izgledalo
# je kot napaka moda. Isti mehanizem kot testworld-run.ps1 in smoke-server.ps1:
# ukazi gredo na standardni vhod procesa, izpis pa v audit\m21-rwdiag.log.
#
# Zagon:
#     .\rwdiag-run.ps1                 # 60 s merjenja
#     .\rwdiag-run.ps1 -Seconds 180    # daljse merjenje
#     .\rwdiag-run.ps1 -AcceptEula     # prvic, ce dev\run\eula.txt se ni sprejet

param([int]$Seconds = 60, [switch]$AcceptEula)

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
    $m = [regex]::Matches((Get-LogText $LogPath), 'RWDIAG-OK ticki=(\d+) npc=(\d+)')
    if ($m.Count -lt $Nth) { return @(-1, -1) }
    $hit = $m[$Nth - 1]
    return @([int]$hit.Groups[1].Value, [int]$hit.Groups[2].Value)
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
        Write-Host 'M2.1 D4-D7 USPESNO: instrumentacija se vklopi, meri, zapise posnetek in se izklopi.'
        Write-Host ("Posnetki: {0}" -f $dumps)
        Write-Host ("Izpis:    {0}" -f $s.Log)
        exit 0
    }
    Write-Host ''
    Write-Host 'M2.1 D4-D7 NEUSPESNO. Padle preverbe:'
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
