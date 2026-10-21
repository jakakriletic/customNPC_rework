param(
    [ValidateSet('Desktop','Test','Lease','Job')][string]$Mode,
    [string]$SpecFile = '',
    [string]$LeaseName = '',
    [switch]$KeepAwake,
    [int]$TargetPid = 0
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8
if ($Mode -eq 'Job') {
    # The target and every later descendant join a kill-on-close job. Orphans whose parent
    # already exited stay members, so closing the job stops the whole AI process tree.
    try {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class OvernightJob {
    [StructLayout(LayoutKind.Sequential)] struct Basic { public long a; public long b; public uint flags; public UIntPtr c; public UIntPtr d; public uint e; public UIntPtr f; public uint g; public uint h; }
    [StructLayout(LayoutKind.Sequential)] struct Io { public ulong a; public ulong b; public ulong c; public ulong d; public ulong e; public ulong f; }
    [StructLayout(LayoutKind.Sequential)] struct Extended { public Basic basic; public Io io; public UIntPtr a; public UIntPtr b; public UIntPtr c; public UIntPtr d; }
    [DllImport("kernel32.dll", SetLastError=true)] static extern IntPtr CreateJobObject(IntPtr attributes, string name);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job, int type, ref Extended info, uint length);
    [DllImport("kernel32.dll", SetLastError=true)] static extern IntPtr OpenProcess(uint access, bool inherit, int pid);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);
    [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr h);
    public static IntPtr Attach(int pid) {
        var job = CreateJobObject(IntPtr.Zero, null);
        if (job == IntPtr.Zero) throw new Exception("CreateJobObject " + Marshal.GetLastWin32Error());
        var info = new Extended(); info.basic.flags = 0x2000;
        if (!SetInformationJobObject(job, 9, ref info, (uint)Marshal.SizeOf(info))) { var e = Marshal.GetLastWin32Error(); CloseHandle(job); throw new Exception("SetInformationJobObject " + e); }
        var process = OpenProcess(0x0101, false, pid);
        if (process == IntPtr.Zero) { var e = Marshal.GetLastWin32Error(); CloseHandle(job); throw new Exception("OpenProcess " + e); }
        try { if (!AssignProcessToJobObject(job, process)) { var e = Marshal.GetLastWin32Error(); CloseHandle(job); throw new Exception("AssignProcessToJobObject " + e); } }
        finally { CloseHandle(process); }
        return job;
    }
}
'@
        $job = [OvernightJob]::Attach($TargetPid)
    } catch { [Console]::WriteLine('NOJOB ' + $_.Exception.Message); exit 0 }
    [Console]::WriteLine('READY')
    [Console]::Out.Flush()
    # EOF on parent exit also closes the job; this helper started before the target joined it.
    [void][Console]::ReadLine()
    [void][OvernightJob]::CloseHandle($job)
    exit 0
}
if ($Mode -eq 'Lease') {
    $mutex = New-Object System.Threading.Mutex($false, $LeaseName)
    $owned = $false
    try {
        try { $owned = $mutex.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $owned = $true }
        if (-not $owned) { [Console]::WriteLine('BUSY'); exit 2 }
        if ($KeepAwake) {
            Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class OvernightAwake {
    [DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint flags);
}
'@
            if ([OvernightAwake]::SetThreadExecutionState([uint32]2147483651) -eq 0) { throw 'SetThreadExecutionState failed' }
        }
        [Console]::WriteLine('READY')
        [Console]::Out.Flush()
        # EOF on parent exit also releases the OS lease. No persistent power setting is changed.
        [void][Console]::ReadLine()
    } finally {
        if ($owned) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
    exit 0
}
if ($Mode -eq 'Desktop') {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class OvernightDesktop {
    [StructLayout(LayoutKind.Sequential)] public struct LastInput { public uint size; public uint time; }
    [DllImport("user32.dll")] static extern bool GetLastInputInfo(ref LastInput value);
    [DllImport("user32.dll", SetLastError=true)] static extern IntPtr OpenInputDesktop(uint flags, bool inherit, uint access);
    [DllImport("user32.dll", CharSet=CharSet.Unicode, SetLastError=true)] static extern bool GetUserObjectInformation(IntPtr h, int n, StringBuilder text, int length, out int needed);
    [DllImport("user32.dll")] static extern bool CloseDesktop(IntPtr h);
    [DllImport("kernel32.dll")] public static extern uint WTSGetActiveConsoleSessionId();
    public static uint[] Input() {
        var v = new LastInput(); v.size = (uint)Marshal.SizeOf(v);
        if (!GetLastInputInfo(ref v)) throw new Exception("GetLastInputInfo failed");
        return new uint[] { v.time, unchecked((uint)Environment.TickCount - v.time) };
    }
    public static string Desktop() {
        var h = OpenInputDesktop(0, false, 1);
        if (h == IntPtr.Zero) return "unavailable";
        try { int n; var b = new StringBuilder(256); return GetUserObjectInformation(h, 2, b, 512, out n) ? b.ToString() : "unknown"; }
        finally { CloseDesktop(h); }
    }
}
'@
    $inputState = [OvernightDesktop]::Input()
    $cpu = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter "Name='_Total'" -ErrorAction Stop
    [ordered]@{
        lastInputTick = [long]$inputState[0]
        idleSeconds = [math]::Floor($inputState[1] / 1000)
        desktop = [OvernightDesktop]::Desktop()
        consoleSession = [long][OvernightDesktop]::WTSGetActiveConsoleSessionId()
        session = (Get-Process -Id $PID).SessionId
        javaProcesses = @(Get-Process -Name java,javaw -ErrorAction SilentlyContinue).Count
        cpuPercent = [double]$cpu.PercentProcessorTime
    } | ConvertTo-Json -Compress
    exit 0
}
if ($SpecFile -eq '') { throw 'SpecFile is required' }
$spec = Get-Content -LiteralPath $SpecFile -Raw -Encoding UTF8 | ConvertFrom-Json
Set-Location -LiteralPath $spec.cwd
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue
$parameters = @{}
foreach ($property in $spec.test.parameters.PSObject.Properties) {
    $value = $property.Value
    if ($value -is [System.Array]) { $value = [string[]]$value }
    $parameters[$property.Name] = $value
}
# The script path and parameter dictionary are data; no generated PowerShell is evaluated.
$global:LASTEXITCODE = 0
try {
    & $spec.test.script @parameters
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
