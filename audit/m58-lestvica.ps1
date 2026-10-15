# M5.8 lestvica: boj in nedosegljiva, O in S. Vsaka serija v svojem procesu, svez svet pred vsako.
$ErrorActionPreference = 'Continue'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = 'C:\Users\jakak\Desktop\customNPC_rework'
$audit = Join-Path $root 'audit'
$S = @('-RwTarget','2','-RwPath','1','-RwPathMemo','1','-RwCollide','1','-RwBlink','1','-RwData','1','-RwMove','1')
$serije = @(
    @{ ime = 'S-boj';  arg = @('-Variants','boj','-Counts','1000,1500,2000,2500,3000,3500') + $S },
    @{ ime = 'O-boj';  arg = @('-Variants','boj','-Counts','1000,1500,2000,2500,3000') },
    @{ ime = 'S-ned';  arg = @('-Variants','nedosegljiva','-Counts','500,600,700,800,1000,1200,1400') + $S },
    @{ ime = 'O-ned';  arg = @('-Variants','nedosegljiva','-Counts','400,500,600,700,800,1000,1200') },
    @{ ime = 'S-idle'; arg = @('-Variants','idle','-Counts','5000,6000,7000,8000') + $S })

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
        Write-Host ("[{0}] {1}: testworld" -f (Get-Date -Format 'HH:mm:ss'), $s.ime)
        $p = Start-Process powershell.exe -ArgumentList (@('-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $root 'testworld.ps1'))) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput (Join-Path $audit ("m58-{0}-testworld.log" -f $s.ime)) -RedirectStandardError (Join-Path $audit ("m58-{0}-testworld.err" -f $s.ime))
        if ($p.ExitCode -ne 0) { Write-Host "  testworld izhod $($p.ExitCode)"; continue }
        $log = Join-Path $audit ("m58-{0}-p{1}.log" -f $s.ime, $poskus)
        Write-Host ("[{0}] {1}: perf-run {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, ($s.arg -join ' '))
        $a = @('-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $root 'perf-run.ps1'),'-Seconds','120','-WarmupSeconds','60') + $s.arg
        $p = Start-Process powershell.exe -ArgumentList $a -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        Write-Host ("[{0}] {1}: izhod {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $p.ExitCode)
        if (Select-String -Path $log -Pattern 'NAPAKA\s+P9' -Quiet) { continue }
        break
    }
}
Write-Host ("[{0}] KONEC" -f (Get-Date -Format 'HH:mm:ss'))
