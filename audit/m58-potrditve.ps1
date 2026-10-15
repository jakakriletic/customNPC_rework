# M5.8 (15. 10.): potrditev pragov p95 <= 25 ms (K3). Za vsak par N ob pragu ena serija
# ponovitve-run.ps1 (3 ponovitve, polno okno 120 + 300 s, svez svet pred vsako), vsaka v svojem procesu.
$ErrorActionPreference = 'Continue'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = 'C:\Users\jakak\Desktop\customNPC_rework'
$audit = Join-Path $root 'audit'
# Ne $S: PowerShell ne loci velikih crk, zancna $s bi ga povozila (15. 10. se je to zgodilo).
$stikalaS = "'-RwTarget','2','-RwPath','1','-RwPathMemo','1','-RwCollide','1','-RwBlink','1','-RwData','1','-RwMove','1'"
# 15. 10. narejeno (T1-T6 zelena): S-idle 3500 in 4000, O-idle 350 in 400. S-boj 1500 je bil
# prekinjen po 1/3 na zahtevo uporabnika in se pozene znova.
$serije = @(
    @{ ime = 'S-boj-1500';  v = 'boj';  n = 1500; s = $true },
    @{ ime = 'S-boj-2000';  v = 'boj';  n = 2000; s = $true },
    @{ ime = 'O-boj-1000';  v = 'boj';  n = 1000; s = $false },
    @{ ime = 'O-boj-1500';  v = 'boj';  n = 1500; s = $false },
    @{ ime = 'S-ned-700';   v = 'nedosegljiva'; n = 700; s = $true },
    @{ ime = 'S-ned-800';   v = 'nedosegljiva'; n = 800; s = $true },
    @{ ime = 'O-ned-500';   v = 'nedosegljiva'; n = 500; s = $false },
    @{ ime = 'O-ned-600';   v = 'nedosegljiva'; n = 600; s = $false })

function Cakaj-Mir {
    $mirnoOd = $null
    while ($true) {
        $java = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
        $prost = $true
        $zp = Join-Path $root '.scenarij.lock'
        if (Test-Path $zp) { try { $fs = [System.IO.File]::Open($zp, 'Open', 'ReadWrite', 'None'); $fs.Close() } catch { $prost = $false } }
        if ($java.Count -eq 0 -and $prost) {
            if (-not $mirnoOd) { $mirnoOd = Get-Date }
            if (((Get-Date) - $mirnoOd).TotalSeconds -ge 300) { return }
        } else { $mirnoOd = $null }
        Start-Sleep -Seconds 15
    }
}

foreach ($s in $serije) {
    for ($poskus = 1; $poskus -le 3; $poskus++) {
        Write-Host ("[{0}] {1} poskus {2}: cakam na mir" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $poskus)
        Cakaj-Mir
        $dod = "'-Variants','{0}','-Counts','{1}'" -f $s.v, $s.n
        if ($s.s) { $dod = $dod + ',' + $stikalaS }
        $ukaz = "& '{0}' -Scenarij perf -Ponovitev 3 -Dodatno @({1}); exit `$LASTEXITCODE" -f (Join-Path $root 'ponovitve-run.ps1'), $dod
        $log = Join-Path $audit ("m58-pot-{0}-p{1}.log" -f $s.ime, $poskus)
        Write-Host ("[{0}] {1}: {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $ukaz)
        # Start-Process v PS 5.1 argumentov s presledki ne navede, zato ukaz gre v svojo skripto.
        $ovoj = Join-Path $audit ("m58-pot-{0}.ps1" -f $s.ime)
        Set-Content -Path $ovoj -Value $ukaz -Encoding ASCII
        $p = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File', $ovoj) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        Write-Host ("[{0}] {1}: izhod {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $p.ExitCode)
        if (Select-String -Path $log -Pattern 'NAPAKA\s+P9' -Quiet) { continue }
        break
    }
}
Write-Host ("[{0}] KONEC" -f (Get-Date -Format 'HH:mm:ss'))
