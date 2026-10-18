# M5-K.1 (18. 10.): profil render niti z vzorcevalnikom sklada (k-run -Vzorci 1), original.
# Vsaka serija v svojem procesu; pred vsako 5 min brez jave (uporabnik vmes gradi/igra ladja_mod).
# Serija, ki pade na K0/K8 (tuj proces), se ponovi do trikrat.
$ErrorActionPreference = 'Continue'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = 'C:\Users\jakak\Desktop\customNPC_rework'
$audit = Join-Path $root 'audit'

$serije = @(
    @{ ime = 'vz-smoke-idle-50'; ukaz = "& '$root\k-run.ps1' -Variant idle -N 50 -Warmup 10 -Measure 20 -Vzorci 1; exit `$LASTEXITCODE" },
    @{ ime = 'vz-idle-200'; ukaz = "& '$root\k-run.ps1' -Variant idle -N 200 -Vzorci 1; exit `$LASTEXITCODE" },
    @{ ime = 'vz-idle-500'; ukaz = "& '$root\k-run.ps1' -Variant idle -N 500 -Vzorci 1; exit `$LASTEXITCODE" },
    @{ ime = 'vz-boj-500';  ukaz = "& '$root\k-run.ps1' -Variant boj -N 500 -Vzorci 1; exit `$LASTEXITCODE" })

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
        $log = Join-Path $audit ("m5k1-{0}-p{1}.log" -f $s.ime, $poskus)
        # Start-Process v PS 5.1 argumentov s presledki ne navede, zato ukaz gre v svojo skripto.
        $ovoj = Join-Path $audit ("m5k1-{0}.ps1" -f $s.ime)
        Set-Content -Path $ovoj -Value $s.ukaz -Encoding ASCII
        Write-Host ("[{0}] {1}: zagon" -f (Get-Date -Format 'HH:mm:ss'), $s.ime)
        $p = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File', $ovoj) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        Write-Host ("[{0}] {1}: izhod {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $p.ExitCode)
        if (Select-String -Path $log -Pattern 'PADLO\s+K[08]' -Quiet) { continue }
        break
    }
}
Write-Host ("[{0}] KONEC" -f (Get-Date -Format 'HH:mm:ss'))
