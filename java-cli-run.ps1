# Controller-owned M6.7 CLI acceptance. No game launch, one valid and one invalid corpus, no repetitions.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$lock = $null
$proc = $null
$exitCode = 1
$oldJavaOptions = $env:JAVA_TOOL_OPTIONS
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m67-cli-$stamp"
$work = Join-Path $root "dev/build/m67/corpus $stamp"
function Wait-M67Process($Process, [int]$Expected) {
    $until = (Get-Date).AddSeconds(180)
    while (-not $Process.HasExited -and (Get-Date) -lt $until) {
        if (@(Get-NavForeignJava -Roots @($Process.Id)).Count -ne 0) { throw 'Competing Java workload during CLI command' }
        $games = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction Stop |
            Where-Object { [string]$_.CommandLine -match 'net\.minecraft\.launchwrapper\.Launch|GradleStart(Server)?(\s|$)|net\.minecraft\.server\.MinecraftServer' })
        if ($games.Count -ne 0) { throw 'Minecraft JVM during compile-only scenario' }
        Start-Sleep -Milliseconds 250
    }
    Assert-JavaPrototype $Process.HasExited 'CLI command completed before timeout'
    [M61ProcessLog]::Finish($Process)
    Assert-JavaPrototype ($Process.ExitCode -eq $Expected) "CLI process exit $Expected"
}
function Quote-M67Literal([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm67-cli'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp = Get-JavaPrototypeInputs $root
    $main = Join-Path $root 'dev/build/classes/java/main'
    Assert-JavaPrototype (Test-Path -LiteralPath (Join-Path $main 'noppes/npcs/rework/script/CompileScripts.class')) 'controller built CLI'
    New-Item -ItemType Directory -Path $work | Out-Null
    $fixture = Join-Path $root 'dev/testworld/java-cli'
    Copy-Item -Path (Join-Path $fixture '*.java') -Destination $work
    $inputs = @{}
    foreach ($file in @(Get-ChildItem -LiteralPath $fixture -File)) { $inputs[$file.Name] = Get-JavaPrototypeHash $file.FullName }
    $processor = Join-Path $work 'processor'
    New-Item -ItemType Directory -Path $processor | Out-Null
    $proc = Start-JavaPrototypeProcess (Join-Path $root '.tools/jdk8/bin/javac.exe') @('-encoding','UTF-8','-source','8','-target','8','-proc:none','-d',$processor,(Join-Path $work 'TripwireProcessor.java')) $root "$prefix-processor.log"
    Wait-JavaPrototypeProcess $proc 60
    $proc = $null
    $services = Join-Path $processor 'META-INF/services'
    New-Item -ItemType Directory -Path $services -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $services 'javax.annotation.processing.Processor'),'TripwireProcessor',(New-Object Text.UTF8Encoding($false)))
    $classpathFile = Join-Path $work 'classpath.txt'
    [IO.File]::WriteAllText($classpathFile,"$processor;$main;$cp",(New-Object Text.UTF8Encoding($false)))
    $valid = @{schema=1;tabs=@(
        @{id='1';files=@('Main.java','Helper.java','Tripwire.java')},
        @{id='2';files=@('Main.java','Helper.java','Tripwire.java')},
        @{id='3';files=@('Plain.java','Tripwire.java')}
    )}
    $invalid = @{schema=1;tabs=@(
        @{id='include';files=@('Main.java','BrokenHelper.java','Tripwire.java')},
        @{id='type';files=@('Wrong.java')},
        @{id='syntax';files=@('Syntax.java')}
    )}
    $proofs = @()
    foreach ($variant in @('valid','invalid')) {
        $manifest = Join-Path $work "$variant.json"
        $corpus = if ($variant -eq 'valid') { $valid } else { $invalid }
        $corpus | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifest -Encoding UTF8
        $report = "$prefix-$variant-report.json"
        $marker = Join-Path $work "$variant-EXECUTED.marker"
        # Both javac and the plain CLI JVM inherit this property. A script initializer,
        # constructor or service-discovered processor writes it if compilation executes them.
        $env:JAVA_TOOL_OPTIONS = "-Drw.m67.marker=`"$marker`""
        $driver = Join-Path $work "$variant-driver.ps1"
        $commandArgs = @('compileScripts','--offline','--no-daemon','--console=plain',"-PscriptsManifest=$manifest","-PscriptsReport=$report","-PscriptsClasspathFile=$classpathFile")
        $literals = @($commandArgs | ForEach-Object { Quote-M67Literal $_ }) -join ','
        $body = '$ErrorActionPreference = ''Stop''; try { & ' + (Quote-M67Literal (Join-Path $root 'dev.ps1')) + ' -GradleArgs @(' + $literals + '); exit 0 } catch { Write-Host $_; exit 1 }'
        [IO.File]::WriteAllText($driver,$body,(New-Object Text.UTF8Encoding($false)))
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $proc = Start-JavaPrototypeProcess (Join-Path $PSHOME 'powershell.exe') @('-NoProfile','-ExecutionPolicy','Bypass','-File',$driver) $root "$prefix-$variant.log"
        $expected = if ($variant -eq 'valid') { 0 } else { 1 }
        Wait-M67Process $proc $expected
        $watch.Stop()
        $proc = $null
        $env:JAVA_TOOL_OPTIONS = $oldJavaOptions
        $r = [IO.File]::ReadAllText($report) | ConvertFrom-Json
        Assert-JavaPrototype ($r.schema -eq 1 -and $r.operation -ceq 'compile-only' -and @($r.tabs).Count -eq 3) "$variant report schema and three tabs"
        Assert-JavaPrototype (@($r.problems).Count -eq 0) "$variant valid input manifest"
        Assert-JavaPrototype ($r.success -eq ($variant -eq 'valid')) "$variant aggregate success"
        Assert-JavaPrototype (-not (Test-Path -LiteralPath $marker)) "$variant no script/constructor/processor execution"
        $classFiles = @(Get-ChildItem -LiteralPath $work -Recurse -File -Filter '*.class')
        Assert-JavaPrototype ($classFiles.Count -eq 1 -and $classFiles[0].FullName -ceq (Join-Path $processor 'TripwireProcessor.class')) "$variant no emitted script class files"
        Assert-JavaPrototype ($r.elapsedNanos -gt 0) "$variant compilation time recorded"
        foreach ($tab in @($r.tabs)) {
            Assert-JavaPrototype ($tab.inputFingerprint -cmatch '^[a-f0-9]{64}$') "$variant tab $($tab.id) input fingerprint"
            Assert-JavaPrototype ($tab.success -eq ($variant -eq 'valid')) "$variant tab $($tab.id) expected result"
            if ($variant -eq 'valid') {
                Assert-JavaPrototype (@($tab.classes).Count -ge 2 -and @($tab.problems | Where-Object { $_.kind -eq 'ERROR' }).Count -eq 0) "valid tab $($tab.id) typed API compiled"
            } else {
                Assert-JavaPrototype (@($tab.classes).Count -eq 0) "invalid tab $($tab.id) no partial bytecode"
                $file = switch ($tab.id) { 'include' {'BrokenHelper.java'} 'type' {'Wrong.java'} 'syntax' {'Syntax.java'} }
                $line = switch ($tab.id) { 'include' {5} 'type' {3} 'syntax' {3} }
                $errors = @($tab.problems | Where-Object { $_.kind -ceq 'ERROR' -and $_.file -ceq $file -and $_.line -eq $line -and $_.column -gt 0 -and $_.code })
                Assert-JavaPrototype ($errors.Count -gt 0) "invalid tab $($tab.id) original file, line, column, compiler code"
                if ($tab.id -eq 'include') { Assert-JavaPrototype ($errors[0].origin -ceq 'include BrokenHelper.java') 'include source map' }
            }
        }
        $log = Read-JavaPrototypeLog "$prefix-$variant.log"
        Assert-JavaPrototype ($log -notmatch ':runClient|:runServer|Starting minecraft|MinecraftForge v') "$variant no game task or bootstrap"
        $proofs += @{variant=$variant;report=$report;commandMillis=$watch.Elapsed.TotalMilliseconds;compileNanos=$r.elapsedNanos;tabs=@($r.tabs).Count;exit=$expected}
    }
    foreach ($file in @(Get-ChildItem -LiteralPath $fixture -File)) { Assert-JavaPrototype ((Get-JavaPrototypeHash $file.FullName) -ceq $inputs[$file.Name]) "unchanged fixture $($file.Name)" }
    @{success=$true;proofs=$proofs;inputs=$inputs;condition='one CLI command per corpus; existing controller build; no Minecraft, scripts or processors executed; three separate tabs per corpus'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M67 CLI evidence: $prefix.json"
    $exitCode=0
} catch { Write-Host "FAIL $_" }
finally {
    $env:JAVA_TOOL_OPTIONS = $oldJavaOptions
    try { Stop-JavaPrototypeProcess $proc } catch { Write-Host "FAIL $_"; $exitCode=1 }
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $exitCode
