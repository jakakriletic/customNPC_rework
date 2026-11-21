<# M12.1: controller-only original URL pipeline baseline. One cell, cold JVM then
   warm restart in the SAME isolated directory against the SAME URLs/HTTP fixture.
   No change to owner mods, original binaries, environment lock or production defaults.
   -Cell faults separates failures/size sensitivity from the four load measurements. #>
param([ValidateSet('unique50','unique200','unique500','same500','faults')][string]$Cell = 'unique50')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m121-$Cell-$stamp"
$work = Join-Path $root "dev/run-m121-$Cell/$stamp"
$fixture = "$prefix-fixture"
$lock = $null; $client = $null; $http = $null; $exitCode = 1
$hashes = @{}; $owned = @{}
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
$utf8 = New-Object Text.UTF8Encoding $false
$n = if ($Cell -eq 'faults') { 15 } elseif ($Cell -eq 'unique50') { 50 } elseif ($Cell -eq 'unique200') { 200 } else { 500 }
$unique = if ($Cell -eq 'same500') { 1 } else { $n }
function Assert([bool]$Ok, [string]$Text) { if (-not $Ok) { throw $Text }; Write-Host "OK $Text" }
function Hash([string]$Path) { if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }; return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash }
function Unchanged { foreach ($p in $hashes.Keys) { if ((Hash $p) -cne $hashes[$p]) { throw "Changed protected input: $p" } } }
function NoOptionalMods {
    # Forge creates mods/ on first startup. Keep the same empty-mod condition
    # across the cold/warm pair without rejecting that generated directory.
    $modsPath = Join-Path $work 'mods'
    $modFiles = @()
    if (Test-Path -LiteralPath $modsPath) {
        Assert (Test-Path -LiteralPath $modsPath -PathType Container) 'mods path is a directory'
        $modFiles = @(Get-ChildItem -LiteralPath $modsPath -Recurse -File -Force)
    }
    Assert ($modFiles.Count -eq 0) 'no optional mod files (including version subdirectories)'
}
function Guard {
    $snapshot = @(Get-CimInstance Win32_Process)
    $roots = @($owned.Keys); if ($null -ne $client) { $roots += $client.Id }
    $tree = @{}; foreach ($id in $roots) { $tree[[int]$id] = $true }
    $grew = $true
    while ($grew) {
        $grew = $false
        foreach ($p in $snapshot) {
            if ($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) {
                $tree[[int]$p.ProcessId] = $true; $grew = $true
            }
        }
    }
    foreach ($p in $snapshot) { if ($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $owned[[int]$p.ProcessId] = $true } }
    $foreign = @(Get-NavForeignJava -Roots (@($roots) + @($owned.Keys)))
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "m121-$Cell"
    Guard
    $environment = Get-Content -LiteralPath (Join-Path $root 'environment-lock.json') -Raw | ConvertFrom-Json
    foreach ($a in $environment.artifacts.PSObject.Properties) {
        $p = Join-Path $root $a.Name; Assert ((Hash $p) -ceq $a.Value) "locked artifact $($a.Name)"; $hashes[$p] = Hash $p
    }
    $protected = @($PSCommandPath,(Join-Path $root 'dev/tools/url-skin-fixture.js'),(Join-Path $root 'dev/build.gradle'),
        (Join-Path $root 'environment-lock.json'),(Join-Path $root 'dev/run/options.txt'),
        (Join-Path $root 'meritve-lib.ps1'),(Join-Path $root 'nav-meritve-lib.ps1'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/src') -Recurse -File | Select-Object -ExpandProperty FullName)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $root 'dev/run/config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($p in $protected) { $hashes[$p] = Hash $p }
    # A dedicated-server workspace may have no client options. Freeze one seed for
    # both JVMs; the Java harness applies and verifies the measured settings.
    # Never borrow another installation's settings or write to dev/run.
    $optionsSource = Join-Path $root 'dev/run/options.txt'
    $optionsSeed = [byte[]]@()
    if (Test-Path -LiteralPath $optionsSource) { $optionsSeed = [IO.File]::ReadAllBytes($optionsSource) }
    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated client/cache directory'
    New-Item -ItemType Directory -Path $work,$fixture -Force | Out-Null
    # The fixture itself is asynchronous Node, so its concurrency never creates Java downloader threads.
    $node = (Get-Command node -ErrorAction Stop).Source
    $hashes[$node] = Hash $node
    $http = Start-Process -FilePath $node -ArgumentList @(('"' + (Join-Path $root 'dev/tools/url-skin-fixture.js') + '"'),('"' + $fixture + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$prefix-http.log" -RedirectStandardError "$prefix-http-error.log"
    $until = (Get-Date).AddSeconds(30)
    while (-not (Test-Path -LiteralPath (Join-Path $fixture 'ready.json')) -and -not $http.HasExited -and (Get-Date) -lt $until) { Start-Sleep -Milliseconds 200 }
    Assert (-not $http.HasExited -and (Test-Path -LiteralPath (Join-Path $fixture 'ready.json'))) 'loopback fixture ready'
    $ready = Get-Content -LiteralPath (Join-Path $fixture 'ready.json') -Raw | ConvertFrom-Json
    $base = "http://127.0.0.1:$($ready.port)"
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0)
    $listener.Start(); $refusedPort = $listener.LocalEndpoint.Port; $listener.Stop()
    $manifest = Get-Content -LiteralPath (Join-Path $fixture 'manifest.json') -Raw | ConvertFrom-Json
    foreach ($a in $manifest.PSObject.Properties) {
        $p = Join-Path $fixture ($a.Name + '.png')
        Assert ((Hash $p) -ieq $a.Value.sha256 -and (Get-Item -LiteralPath $p).Length -eq $a.Value.bytes) "fixture hash/size $($a.Name)"
        $hashes[$p] = Hash $p
    }
    Assert ($manifest.bigbody.bytes -gt 8388608) 'encoded-byte negative control exceeds 8 MiB'
    $runs = @()
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    foreach ($restart in @('cold','warm')) {
        Guard; Unchanged
        # Copy only settings; the warm JVM retains the entire previous cache/world tree.
        Copy-Item -LiteralPath (Join-Path $root 'dev/run/config') -Destination $work -Recurse -Force
        $optionsPath = Join-Path $work 'options.txt'
        [IO.File]::WriteAllBytes($optionsPath, $optionsSeed)
        $optionsInput = @{source=$optionsSource; sourceHash=$hashes[$optionsSource]; seedHash=(Hash $optionsPath); seedBytes=$optionsSeed.Length}
        NoOptionalMods
        $proofPath = "$prefix-$restart.proof.json"; $job = "$prefix-$restart.job"; $log = "$prefix-$restart-client.log"
        $httpBefore = @(Get-Content -LiteralPath (Join-Path $fixture 'http.jsonl') -ErrorAction SilentlyContinue).Count
        $cacheBefore = @()
        $cachePath = Join-Path $work 'customnpcs/cache'
        if (Test-Path -LiteralPath $cachePath) { $cacheBefore = @(Get-ChildItem -LiteralPath $cachePath -Recurse -File | ForEach-Object { @{path=$_.FullName; hash=(Hash $_.FullName); bytes=$_.Length} }) }
        [IO.File]::WriteAllLines($job,@('urlSkinBaseline=1','world=m121',('cell='+$Cell),('n='+$n),('restart='+$restart),
            ('base='+$base),('refusedBase=http://127.0.0.1:'+$refusedPort),('fixture='+$fixture.Replace('\','/')),('out='+$proofPath.Replace('\','/'))),$utf8)
        @{package='M12.1'; cell=$Cell; restart=$restart; n=$n; base=$base; fixture=$manifest; inputs=$hashes;
            cacheBefore=$cacheBefore; ready=$ready; work=$work; optionsInput=$optionsInput; conditions='30s scene/JVM warmup; original ImageDownloadAlt, MD5 keys, null cacheFile; 1280x720 RD10 FOV70 GUI2 VSync off frame cap260; fixed camera 0,70,-32 yaw0/pitch0; client-only static grid in sky; 20s load + 10s steady; no profiler/readback/image IO in timed windows; two field reads until upload through transparent delegate; 250ms telemetry; estimates are not physical VRAM'} |
            ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix-$restart.inputs.json" -Encoding UTF8
        $psi = New-Object Diagnostics.ProcessStartInfo
        $psi.FileName = $env:ComSpec; $psi.WorkingDirectory = Join-Path $root 'dev'
        $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" > "{2}" 2>&1' -f $work.Replace('\','/'),$job.Replace('\','/'),$log)
        $psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
        $client = [Diagnostics.Process]::Start($psi)
        $until = (Get-Date).AddSeconds(900)
        while (-not $client.HasExited -and (Get-Date) -lt $until) { Guard; Start-Sleep -Seconds 1 }
        Assert $client.HasExited "$restart client finished before hang guard"
        Assert ($client.ExitCode -eq 0) "$restart clean client exit"
        NoOptionalMods
        $text = [IO.File]::ReadAllText($log)
        Assert (-not ($text -match "NoSuchMethodError|NoSuchFieldError|AbstractMethodError|NoClassDefFoundError|VerifyError|RWHARNESS-NAPAKA|Couldn't render entity")) "$restart no linkage/render/harness failure"
        $proof = Get-Content -LiteralPath $proofPath -Raw | ConvertFrom-Json
        Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M121 DONE success=true')) "$restart proof success"
        $expected = @('warm-visible','conditions','visible','dedup','frames','threads-ended','no-gl-error')
        if ($Cell -ne 'faults') { $expected += 'all-gpu'; if ($Cell -ne 'same500') { $expected += 'thread-fanout' } }
        else { $expected += @('unbounded-headers','unbounded-body','unbounded-trickle') }
        $failed = @('missing','invalid','headers','body','trickle','redirect','refused')
        Assert (@($proof.urls).Count -eq $unique) "$restart exact URL count"
        for ($i=0; $i -lt $unique; $i++) {
            $r = $proof.urls[$i]; $expected += "state-$i"
            Assert ($r.cacheFileNull -eq $true -and $r.threadName -like 'Texture Downloader #*') "$restart original downloader $i"
            if ($r.mode -notin $failed) {
                $expected += "pixels-$i"
                Assert ($r.state -ceq 'uploaded' -and $r.gpuBindMs -gt 0 -and $r.uploadNs -gt 0) "$restart actual GPU upload $i"
                Assert ($r.retainedImageBytes -eq $r.width*$r.height*4 -and $r.retainedParserBytes -eq $r.retainedImageBytes -and $r.parserAliasesImage -eq $true) "$restart retained raster alias $i"
            }
            if ($Cell -eq 'faults' -and $r.mode -ne 'bigbody') { $expected += "image-$i"; Assert ($r.imageComparison.success -eq $true) "$restart local skin/Steve image reference $i" }
        }
        Assert (@($proof.checks).Count -eq $expected.Count -and @($proof.checks.key | Select-Object -Unique).Count -eq $expected.Count) "$restart exact check count"
        foreach ($key in $expected) { $c = @($proof.checks | Where-Object { $_.key -ceq $key }); Assert ($c.Count -eq 1 -and $c[0].ok -eq $true) "$restart $key" }
        $httpRows = @(Get-Content -LiteralPath (Join-Path $fixture 'http.jsonl') | Select-Object -Skip $httpBefore | ForEach-Object { $_ | ConvertFrom-Json })
        $startRows = @($httpRows | Where-Object { $_.event -ceq 'start' })
        $endRows = @($httpRows | Where-Object { $_.event -ceq 'end' })
        # JDK may follow the redirect loop up to its own limit; report that separately.
        $ordinary = @($startRows | Where-Object { $_.url -notlike '*/redirect/*' })
        $wanted = if ($Cell -eq 'faults') { $unique - 2 } else { $unique }
        Assert ($ordinary.Count -eq $wanted -and @($ordinary.url | Select-Object -Unique).Count -eq $wanted) "$restart one HTTP attempt per original non-redirect URL (same500 deduplicated)"
        Assert ($startRows.Count -eq $endRows.Count) "$restart all HTTP responses/sockets closed"
        if ($Cell -eq 'faults') {
            foreach ($mode in @('headers','body','trickle')) {
                $start = @($startRows | Where-Object { $_.url -like "*/$mode/*" })
                $close = @($httpRows | Where-Object { $_.event -ceq 'fixture-close' -and $_.url -like "*/$mode/*" })
                Assert ($start.Count -eq 1 -and $close.Count -eq 1 -and ($close[0].time - $start[0].time) -ge 30000) "$restart fixture cleanup only after 30s $mode"
            }
            $headersEnd = @($endRows | Where-Object { $_.url -like '*/headers/*' })
            Assert ($headersEnd.Count -eq 1 -and $headersEnd[0].status -eq 503 -and $headersEnd[0].bytes -eq 0 -and $headersEnd[0].elapsed -ge 30000) "$restart headers cleanup closes with complete empty 503 (no JDK EOF retry)"
            Assert (@($startRows | Where-Object { $_.url -like '*/redirect/*' }).Count -gt 1) "$restart original redirect retry sensitivity"
        }
        $httpRows | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix-$restart.http.json" -Encoding UTF8
        $runs += @{restart=$restart; proof=$proofPath; log=$log; inputs="$prefix-$restart.inputs.json"; http="$prefix-$restart.http.json";
            requests=$startRows.Count; bytes=($endRows | Measure-Object -Property bytes -Sum).Sum; peak=($httpRows | Measure-Object -Property peak -Maximum).Maximum}
        $client = $null; Unchanged
    }
    # Warm is a new JVM with the same cache directory and URLs: legacy must still redownload.
    Assert ($runs[0].requests -eq $runs[1].requests) 'original warm restart request count equals cold'
    @{success=$true; package='M12.1'; cell=$Cell; runs=$runs; inputs=$hashes; note='instrumented synthetic baseline, not owner-world measurement or optimized pipeline acceptance'} |
        ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    if ($null -ne $http -and -not $http.HasExited) {
        [IO.File]::WriteAllText((Join-Path $fixture 'stop'),'stop',$utf8)
        if (-not $http.WaitForExit(10000)) { Stop-Process -Id $http.Id -Force; $exitCode = 1 }
    }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava; $env:PATH = $oldPath
}
exit $exitCode
