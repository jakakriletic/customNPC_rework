# M6.1 controller helpers; under dev/testworld so the controller fingerprints these inputs.
function Assert-JavaPrototype([bool]$Ok, [string]$Message) {
    if (-not $Ok) { throw $Message }
    Write-Host "OK $Message"
}
function Get-JavaPrototypeInputs([string]$Root) {
    # Callers first assert there is no foreign Java, so Gradle JVMs seen afterwards are ours.
    $before = @{}
    foreach ($p in @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction Stop)) { $before[[int]$p.ProcessId] = $true }
    & (Join-Path $Root 'dev.ps1') -GradleArgs @('javaPrototypePaths','--offline','--no-daemon','--console=plain') | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Could not export prototype paths.' }
    # --no-daemon with custom JVM args forks a single-use GradleDaemon that can outlive
    # the client briefly; wait for our own Gradle JVMs to exit before guarding workloads.
    $until = (Get-Date).AddSeconds(60)
    do {
        $left = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction Stop |
            Where-Object { -not $before.ContainsKey([int]$_.ProcessId) -and [string]$_.CommandLine -match $script:TujiJavaVzorec })
        if (-not $left.Count) { break }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    Assert-JavaPrototype ($left.Count -eq 0) 'own path-export Gradle JVMs exited'
    $cp = [IO.File]::ReadAllText((Join-Path $Root 'dev/build/m61/compile-classpath.txt')).Trim()
    $kept = @()
    foreach ($path in $cp.Split([IO.Path]::PathSeparator)) {
        # ForgeGradle's empty `api` source set exports an output directory that is never
        # created; javac ignores it, so only absent class directories may be dropped.
        if (-not $path.EndsWith('.jar', [StringComparison]::OrdinalIgnoreCase) -and -not (Test-Path -LiteralPath $path)) {
            Write-Host "OK absent empty class directory skipped: $path"
            continue
        }
        Assert-JavaPrototype (Test-Path -LiteralPath $path) "compiler input exists: $path"
        $kept += $path
    }
    return ($kept -join [IO.Path]::PathSeparator)
}
function Get-JavaPrototypeHash([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Missing input: $Path" }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}
function Test-JavaPrototypeJar([string]$Path, [string]$Entry) {
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try { return $null -ne $zip.GetEntry($Entry) } finally { $zip.Dispose() }
}
function Get-JavaPrototypeSingle([string]$Dir, [string]$Filter) {
    $files = @(Get-ChildItem -LiteralPath $Dir -Recurse -File -Filter $Filter)
    Assert-JavaPrototype ($files.Count -eq 1) "exactly one $Filter"
    return $files[0].FullName
}
function Start-JavaPrototypeProcess([string]$Executable, [string[]]$Arguments, [string]$WorkingDirectory, [string]$Log) {
    foreach ($arg in @($Executable,$Log) + $Arguments) {
        if ($arg -match '["\r\n]') { throw 'Unexpected quote/newline in process argument.' }
    }
    $quoted = @($Arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
    # Direct exe launch avoids cmd's 8191-character classpath limit. C# reader
    # callbacks can write logs without requiring PowerShell runspaces on IO threads.
    if (-not ('M61ProcessLog' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.IO;
using System.Text;
public static class M61ProcessLog {
    private static readonly ConcurrentDictionary<int,StreamWriter> logs = new ConcurrentDictionary<int,StreamWriter>();
    public static Process Start(string exe, string args, string work, string log) {
        ProcessStartInfo psi = new ProcessStartInfo();
        if (exe.EndsWith(".bat", StringComparison.OrdinalIgnoreCase)) {
            psi.FileName = Environment.GetEnvironmentVariable("ComSpec");
            psi.Arguments = "/d /s /c \"\"" + exe + "\" " + args + "\"";
        } else { psi.FileName = exe; psi.Arguments = args; }
        psi.WorkingDirectory=work; psi.UseShellExecute=false; psi.CreateNoWindow=true;
        psi.RedirectStandardInput=true; psi.RedirectStandardOutput=true; psi.RedirectStandardError=true;
        psi.StandardOutputEncoding=Encoding.UTF8; psi.StandardErrorEncoding=Encoding.UTF8;
        StreamWriter writer=new StreamWriter(new FileStream(log,FileMode.CreateNew,FileAccess.Write,FileShare.ReadWrite),new UTF8Encoding(false));
        writer.AutoFlush=true;
        Process p=new Process(); p.StartInfo=psi;
        DataReceivedEventHandler record=delegate(object sender, DataReceivedEventArgs e) {
            if(e.Data!=null) { lock(writer) { writer.WriteLine(e.Data); } }
        };
        p.OutputDataReceived+=record; p.ErrorDataReceived+=record;
        try { p.Start(); logs[p.Id]=writer; p.BeginOutputReadLine(); p.BeginErrorReadLine(); return p; }
        catch { writer.Dispose(); throw; }
    }
    public static void Finish(Process p) {
        p.WaitForExit(); // also drains asynchronous stdout/stderr readers
        StreamWriter writer;
        if(logs.TryRemove(p.Id,out writer)) { lock(writer) { writer.Dispose(); } }
    }
}
'@
    }
    return [M61ProcessLog]::Start($Executable,$quoted,$WorkingDirectory,$Log)
}
function Wait-JavaPrototypeProcess($Process, [int]$Seconds) {
    $until = (Get-Date).AddSeconds($Seconds)
    while (-not $Process.HasExited -and (Get-Date) -lt $until) {
        $foreign = @(Get-NavForeignJava -Roots @($Process.Id))
        if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    Assert-JavaPrototype $Process.HasExited 'prototype process completed before timeout'
    [M61ProcessLog]::Finish($Process)
    Assert-JavaPrototype ($Process.ExitCode -eq 0) 'prototype process exit 0'
}
function Read-JavaPrototypeLog([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    $stream = [IO.File]::Open($Path,'Open','Read','ReadWrite')
    $reader = New-Object IO.StreamReader($stream)
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
function Stop-JavaPrototypeProcess($Process) {
    if ($null -eq $Process) { return }
    if (-not $Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F 2>&1 | Out-Null
        if (-not $Process.WaitForExit(15000)) { throw 'Could not terminate owned prototype process tree.' }
    }
    [M61ProcessLog]::Finish($Process)
}
