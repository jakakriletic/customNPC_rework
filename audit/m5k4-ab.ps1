# M5-K K4 (19. 10.): A/B napisov iz display lista, 3 ponovitve na celico.
# n0 = original (RwLabelCache 0), n1 = display list (1). idle-200: 174 napisov v dometu, idle-50: vsi.
# Vsaka serija v svojem procesu; pred vsako 2 min brez jave. Serija, ki pade na K0/K8, se ponovi do trikrat.
$ErrorActionPreference = 'Continue'
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$root  = 'C:\Users\jakak\Desktop\customNPC_rework'
$audit = Join-Path $root 'audit'

$serije = @(
    @{ ime = 'idle-200-n0'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','200','-Napisi','0'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-200-n1'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','200','-Napisi','1'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-50-n0'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','50','-Napisi','0'); exit `$LASTEXITCODE" },
    @{ ime = 'idle-50-n1'; ukaz = "& '$root\ponovitve-run.ps1' -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','50','-Napisi','1'); exit `$LASTEXITCODE" })

function Cakaj-Mir {
    $mirnoOd = $null
    while ($true) {
        $java = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
        $prost = $true
        $zp = Join-Path $root '.scenarij.lock'
        if (Test-Path $zp) { try { $fs = [System.IO.File]::Open($zp, 'Open', 'ReadWrite', 'None'); $fs.Close() } catch { $prost = $false } }
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
        $log = Join-Path $audit ("m5k4-{0}-p{1}.log" -f $s.ime, $poskus)
        # Start-Process v PS 5.1 argumentov s presledki ne navede, zato ukaz gre v svojo skripto.
        $ovoj = Join-Path $audit ("m5k4-{0}.ps1" -f $s.ime)
        Set-Content -Path $ovoj -Value $s.ukaz -Encoding ASCII
        Write-Host ("[{0}] {1}: zagon" -f (Get-Date -Format 'HH:mm:ss'), $s.ime)
        $p = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File', $ovoj) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError ($log + '.err')
        Write-Host ("[{0}] {1}: izhod {2}" -f (Get-Date -Format 'HH:mm:ss'), $s.ime, $p.ExitCode)
        foreach ($l in @(Select-String -Path $log -Pattern 'idle-\d+: slicica')) { Write-Host ("    " + $l.Line.Trim()) }
        if (Select-String -Path $log -Pattern 'PADLO\s+K[08]' -Quiet) { continue }
        break
    }
}
Write-Host ("[{0}] KONEC" -f (Get-Date -Format 'HH:mm:ss'))
