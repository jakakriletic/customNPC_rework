# Obnovi razvojno okolje na tej delovni postaji (M0.2r).
# Okolje je bilo prvotno na drugem racunalniku; dev/libs, dev/baseline in .tools
# so v .gitignore, zato jih je treba tukaj sestaviti znova.
#
# Zagon iz korena mape:   .\obnovi-okolje.ps1
# Vse gre tudi v log:     audit\obnova-okolja.log
#
# Gradle koraki najprej poskusijo --offline. Ce lokalni Gradle cache na tej
# postaji ne obstaja (nova delovna postaja), se isti korak ponovi brez te
# zastavice, da se odvisnosti prenesejo. To je edina razlika proti postopku v
# OKOLJE.md in je zabelezena v logu.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$log  = Join-Path $root 'audit\obnova-okolja.log'
New-Item -ItemType Directory -Force -Path (Join-Path $root 'audit') | Out-Null
Start-Transcript -Path $log -Force | Out-Null

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

# Zunanji programi (java, gradlew) pisejo tudi na stderr. Pri
# $ErrorActionPreference = 'Stop' PowerShell vsako vrstico stderr spremeni v
# terminating error, zato se okoli vsakega native klica zacasno preklopi na
# 'Continue'; uspeh se presoja po $LASTEXITCODE, ne po stderr.
function NativeRun {
    param([string]$Exe, [string[]]$ExeArgs)
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Exe @ExeArgs 2>&1 | ForEach-Object { Write-Host $_ }
    } finally { $ErrorActionPreference = $old }
}

# Pozene gradlew z --offline; ob neuspehu ponovi brez njega.
function Gradle([string[]]$taskArgs) {
    Write-Host ">>> gradlew $($taskArgs -join ' ') --offline"
    NativeRun '.\gradlew.bat' ($taskArgs + '--offline')
    if ($LASTEXITCODE -eq 0) { return }
    Write-Host ""
    Write-Host "!!! offline razlicica je padla (exit $LASTEXITCODE); ponavljam z omrezjem"
    Write-Host ">>> gradlew $($taskArgs -join ' ')"
    NativeRun '.\gradlew.bat' $taskArgs
    if ($LASTEXITCODE -ne 0) { throw "gradlew $($taskArgs -join ' ') exit $LASTEXITCODE" }
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

    Step 1 'Java 8'
    if (-not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        throw "Manjka $env:JAVA_HOME\bin\javac.exe"
    }
    NativeRun "$env:JAVA_HOME\bin\java.exe" @('-version')

    Step 2 'Preverjanje artefaktov proti environment-lock.json'
    $lock = Get-Content (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($p in $lock.artifacts.PSObject.Properties) {
        $f = Join-Path $root $p.Name
        if (-not (Test-Path $f)) { Write-Host ("MANJKA   {0}" -f $p.Name); continue }
        $h = (Get-FileHash $f -Algorithm SHA256).Hash
        $ok = if ($h -eq $p.Value) { 'OK      ' } else { 'RAZLIKA ' }
        Write-Host ("{0} {1}" -f $ok, $p.Name)
        if ($h -ne $p.Value) { Write-Host ("           dobljeno {0}" -f $h) }
    }

    Push-Location (Join-Path $root 'dev')
    try {
        Step 3 'setupDecompWorkspace'
        Gradle @('setupDecompWorkspace')

        Step 4 'SRG -> MCP remap originala'
        Gradle @('exportMappedOriginal', '-PremapOriginal')

        Step 5 'Testi: original + obnovljena koda'
        Gradle @('testOriginal', 'test')

        Step 6 'buildPatchedMod'
        Gradle @('buildPatchedMod')
    } finally { Pop-Location }

    Step 7 'verify-package.ps1'
    & (Join-Path $root 'verify-package.ps1') | ForEach-Object { Write-Host $_ }

    Step 8 'Koncano'
    Write-Host 'OBNOVA USPESNA. Naslednji korak je M0.5 (runServer smoke).'
}
catch {
    Write-Host ''
    Write-Host "NAPAKA: $_"
    Write-Host 'OBNOVA NEUSPESNA. Celoten izpis je v audit\obnova-okolja.log'
}
finally {
    Write-Host ''
    Write-Host "===== KONEC $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ====="
    Stop-Transcript | Out-Null
}
