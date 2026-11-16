<# M14.1b controller-only GPU baseline. No world or cost window. A verified mapped-original
   ModelScaleRenderer is compared to the VBO candidate at the ordinary CNPC part boundary.
   Launches exactly one isolated dev client; never modifies dev/run, original jars or locks. #>
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m141b-vbo-$stamp"
$work = Join-Path $root "dev/run-m141b/$stamp"
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
    foreach ($path in $hashes.Keys) { Assert ((Fingerprint $path) -ceq $hashes[$path]) "unchanged $path" }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm141b-vbo'
    Guard
    $environment = Get-Content -LiteralPath (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($artifact in $environment.artifacts.PSObject.Properties) {
        $path = Join-Path $root $artifact.Name
        Assert ((Fingerprint $path) -ceq $artifact.Value) "locked artifact $($artifact.Name)"
        $hashes[$path] = Fingerprint $path
    }
    $reference = Join-Path $root 'dev/reference-src/noppes/npcs/client/model/ModelScaleRenderer.java'
    $patch = Join-Path $root 'dev/src/patch/java/noppes/npcs/client/model/ModelScaleRenderer.java'
    Assert ((Fingerprint $reference) -cne (Fingerprint $patch)) 'candidate differs from neutral original'
    $protected = @((Join-Path $root 'environment-lock.json'), (Join-Path $run 'options.txt'), $reference, $patch, $PSCommandPath,
        (Join-Path $root 'dev/src/patch/java/noppes/npcs/rework/diag/ModelVboCheck.java'),
        (Join-Path $root 'dev/src/patch/java/noppes/npcs/rework/render/ModelVbo.java'),
        (Join-Path $root 'dev/src/patch/java/noppes/npcs/rework/render/ModelVboGeometry.java'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path] = Fingerprint $path }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $root 'audit') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    Assert (-not (Test-Path -LiteralPath (Join-Path $work 'mods'))) 'no optional rendering mods'
    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    $mapped = Join-Path $root 'dev/libs/customnpcs-mapped-01Oct19.jar'
    [IO.File]::WriteAllLines($job, @('modelVboCheck=1', ('mappedJar=' + $mapped.Replace('\', '/')),
        ('out=' + $out.Replace('\', '/'))), (New-Object Text.UTF8Encoding $false))
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(900)
    while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
    Assert $client.HasExited 'client completed before timeout'
    Assert ($client.ExitCode -eq 0) 'clean client process exit'
    $text = [IO.File]::ReadAllText($log)
    Assert ($text.Contains('M141B DONE success=true')) 'baseline success marker'
    Assert (-not ($text -match 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|M141B DONE success=false|RWHARNESS-NAPAKA')) 'no linkage/harness errors'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true) 'proof success'
    Assert ($proof.oracleClassLoaderIndependent -eq $true) 'independent original oracle'
    Assert ($proof.modelFixtures -eq 3 -and $proof.distinctParts -eq 31 -and $proof.comparisons -eq 93 -and $proof.nonemptyPartDraws -eq 279) 'exact fixture and draw counts'
    Assert ($proof.width -eq 256 -and $proof.height -eq 256 -and $proof.glErrors -eq 0) 'fixed FBO and no GL errors'
    Assert ($proof.wrongPoseRejected -eq $true -and $proof.wrongUvRejected -eq $true) 'both negative controls rejected'
    $rows = @($proof.rows)
    Assert ($rows.Count -eq 93 -and @($rows.key | Select-Object -Unique).Count -eq 93) '93 distinct comparisons'
    foreach ($m in 0..2) {
        $partCount = if ($m -lt 2) { 12 } else { 7 }
        foreach ($p in 0..($partCount - 1)) {
            foreach ($pose in 0..2) {
                $key = "model$m-part$p-pose$pose"
                $row = @($rows | Where-Object { $_.key -ceq $key })
                Assert ($row.Count -eq 1 -and $row[0].equal -eq $true -and $row[0].repeatEqual -eq $true -and
                    $row[0].offEqual -eq $true -and $row[0].vboDraws -eq 1 -and $row[0].rgbaEqual -eq $true -and $row[0].depthEqual -eq $true -and $row[0].stateEqual -eq $true -and
                    $row[0].covered -gt 0 -and $row[0].cubes -eq 1 -and $row[0].listsValid -eq $true -and
                    $row[0].rgbaSha256 -cmatch '^[0-9a-f]{64}$' -and $row[0].depthSha256 -cmatch '^[0-9a-f]{64}$') $key
            }
        }
    }
    Assert ($proof.outsideFallback -eq $true -and $proof.mutationFallback -eq $true -and $proof.programFallback -eq $true) 'outside context, mutation and active program fallback'
    Assert ($proof.reloadEqual -eq $true -and $proof.liveAfterRelease -eq 0 -and $proof.uploads -gt 0 -and $proof.deletes -eq $proof.uploads) 'reload rebuild, balanced GPU resources'
    Assert (@($proof.screenshots).Count -eq 6) 'six baseline PNGs'
    foreach ($image in $proof.screenshots) {
        Assert ((Test-Path -LiteralPath $image) -and (Get-Item -LiteralPath $image).Length -gt 100) "PNG $image"
    }
    $client = $null
    Unchanged
    @{success = $true; package = 'M14.1b'; proof = $out; log = $log; inputs = $hashes;
      condition = 'one isolated dev client, menu GL context, 31 ordinary CNPC parts in 3 frozen poses, original/patch/original; no NPC world or cost run'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M14.1b evidence: $prefix.json"
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
