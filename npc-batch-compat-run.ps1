<# M14.3a controller-only native GL boundary/lifecycle proof. One isolated dev client,
   real world reload/rejoin, no cost window. This does not install or simulate OptiFine. #>
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m143a-native-$stamp"
$work = Join-Path $root "dev/run-m143a/$stamp"
$lock = $null
$client = $null
$exitCode = 1
$hashes = @{}
$owned = @{}
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Fingerprint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Guard {
    $roots = @(); if ($null -ne $client) { $roots += $client.Id }
    $snapshot = @(Get-CimInstance Win32_Process)
    $tree = @{}; foreach ($id in @($roots) + @($owned.Keys)) { $tree[[int]$id] = $true }
    $grew = $true
    while ($grew) {
        $grew = $false
        foreach ($p in $snapshot) {
            if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) {
                $tree[[int]$p.ProcessId] = $true; $grew = $true
            }
        }
    }
    foreach ($p in $snapshot) {
        if ($p.Name -in @('java.exe', 'javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true }
    }
    $foreign = @(Get-NavForeignJava -Roots (@($roots) + @($owned.Keys)))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged {
    foreach ($path in $hashes.Keys) {
        if ((Fingerprint $path) -cne $hashes[$path]) { throw "Changed protected input: $path" }
    }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm143a-native'
    Guard
    $environment = Get-Content -LiteralPath (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($artifact in $environment.artifacts.PSObject.Properties) {
        $path = Join-Path $root $artifact.Name
        Assert ((Fingerprint $path) -ceq $artifact.Value) "locked artifact $($artifact.Name)"
        $hashes[$path] = Fingerprint $path
    }
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'), $PSCommandPath,
        (Join-Path $root 'meritve-lib.ps1'), (Join-Path $root 'nav-meritve-lib.ps1'), (Join-Path $root 'dev/build.gradle'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/src') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $root 'audit') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'mods'))) 'no optional rendering mods (native test only)'
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    [IO.File]::WriteAllLines($job, @('npcBatchCompat=1', 'world=m143a-native',
        ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" -PrwSet=RwNpcInstancing=0 > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
    Assert $client.HasExited 'client completed before timeout'
    Assert ($client.ExitCode -eq 0) 'clean client process exit'
    $text = [IO.File]::ReadAllText($log)
    Assert (-not ($text -match 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|RWHARNESS-NAPAKA')) 'no linkage/harness errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    # Check the full contract, not just the harness success flag. False rows are retained
    # in the proof/log for diagnosis of the unchanged pre-fix production code.
    $keys = @('ordinary', 'foreign-vao-same-frame', 'foreign-program-same-frame', 'extra-light', 'extra-texture',
        'child-model', 'outside-world-pass', 'ordinary-after-fallbacks', 'ordinary-after-reload', 'ordinary-after-world-rejoin')
    $expected = @($keys) + @($keys | ForEach-Object { "$_-gl" }) + @('reload-has-live-resources', 'reload-off', 'reload-off-gl',
        'reload-program-rebuilt', 'unload-has-live-resources', 'world-unload-off', 'world-unload-off-gl', 'rejoined-world')
    $checks = @($proof.checks)
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        if ($row.Count -eq 1) { Write-Host ("RESULT {0} ok={1} {2}" -f $key, $row[0].ok, $row[0].detail) }
    }
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143A DONE success=true')) 'native compatibility proof success'
    Assert ($proof.joins -eq 2 -and $proof.width -eq 256 -and $proof.height -eq 256) 'real world rejoin and fixed FBO'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        Assert ($row.Count -eq 1 -and $row[0].ok -eq $true) $key
    }
    Assert (@($proof.rows).Count -eq $keys.Count) 'all image/state rows'
    foreach ($row in $proof.rows) {
        Assert ($row.repeatEqual -eq $true -and $row.stateEqual -eq $true -and $row.imageEqual -eq $true) "A/B/A2 $($row.key)"
        $native = $row.key -like 'ordinary*'
        Assert ($row.draws -eq [int]$native -and $row.fallbacks -eq [int](-not $native)) "actual route $($row.key)"
        Assert ($row.exact -eq (-not $native)) "bitwise fallback $($row.key)"
    }
    $client = $null
    Unchanged
    @{success = $true; package = 'M14.3a'; proof = $out; log = $log; inputs = $hashes;
      condition = 'one isolated integrated dev client, ordinary main model, frozen FBO A/B/A2, native foreign program/VAO in same frame, real reload and world leave/rejoin at switch 0; no cost or optional-mod proof'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M14.3a evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
