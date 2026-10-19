# M5-K K3 (19. 10.): A/B oci iz display lista, 3 ponovitve na celico.
# o0 = original (RwEyesList 0), o1 = display list (1). Oci: 10 % render niti pri idle-200, 14 % pri idle-500 (M5-K.1).
# Pred vsako serijo 2 min brez jave in brez tujega procesa find (PID 22024; uporabnik: pocakaj, da konca).
# Vsaka serija v svojem procesu. Serija, ki pade na K0/K5/K8 (tuj proces, premaknjena kamera), se ponovi do trikrat.
$ErrorActionPreference = 'Continue'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = 'C:\Users\jakak\Desktop\customNPC_rework'
$audit = Join-Path $root 'audit'

$serije = @(
    @{ ime = 'idle-200-o0'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','200','-Oci','0'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-200-o1'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','200','-Oci','1'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-500-o0'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','500','-Oci','0'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-500-o1'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','500','-Oci','1'); exit `$LASTEXITCODE" })

function Cakaj-Mir {
    $mirnoOd = $null
    while ($true) {
        $java = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
        $prost = $true
        $zp = Join-Path $root '.scenarij.lock'
        if (Test-Path $zp) { try { $fs = [System.IO.File]::Open($zp, 'Open', 'ReadWrite', 'None'); $fs.Close() } catch { $prost = $false } }
        if (Get-Process -Id 22024 -ErrorAction SilentlyContinue) { $prost = $false }
        if ($java.Count -eq 0 -and $prost) {
            if (-not $mirnoOd) { $mirnoOd = Get-Date }
            if (((Get-Date) - $mirnoOd).TotalSeconds -ge 120) { return }
        } else { $mirnoOd = $null }
        Start-Sleep -Seconds 10
    }
}

foreach ($s in $serije) {
    for ($poskus = 1; $poskus -le 3; $poskus++) {
        Write-Host ("[{0}] {1} poskus {2}: cakam na mir" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $poskus)
        Cakaj-Mir
        $log = Join-Path $audit ("m5k3-{0}-p{1}.log" -f $s.ime, $poskus)
        # Start-Process v PS 5.1 argumentov s presledki ne navede, zato ukaz gre v svojo skripto.
        $ovoj = Join-Path $audit ("m5k3-{0}.ps1" -f $s.ime)
        Set-Content -Path $ovoj -Value $s.ukaz -Encoding ASCII
        Write-Host ("[{0}] {1}: zagon" -f (Get-Date -Format 'HH:mm:ss'), $s.ime)
        $p = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File', $ovoj) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        Write-Host ("[{0}] {1}: izhod {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $p.ExitCode)
        foreach ($l in @(Select-String -Path $log -Pattern 'idle-\d+: slicica')) { Write-Host ("    " + $l.Line.Trim()) }
        if (Select-String -Path $log -Pattern 'PADLO\s+K[058]' -Quiet) { continue }
        break
    }
}
Write-Host ("[{0}] KONEC" -f (Get-Date -Format 'HH:mm:ss'))
