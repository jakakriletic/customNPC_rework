<#
    M4.14b: A/B on the unchanged M2.7 lanes, separate copies of one fresh world.
    Controller owns all launches. No build, config edit or replacement of dev/run/world.
    Phase A is navigateTo once on both sides; phase B is refreshed script / FormationApi.
    F1-F12 are a separate smoke pass and are NOT certified by this A/B runner.
    TraceRecovery is a separate diagnostic: all ticks plus navigation/recovery identities.
    TraceNavigator adds read-only vanilla timeout/update state; never acceptance proof.
    P4 (Q257): FA1/2 are judged by the 8 unique final slots and FA4 by the largest lag of a
    non-excused member behind its commanded target (RWSQUAD zaostanek_max <= LAG_STOP + 1 = 6).
    The original common-goal and diameter values stay in the report as NOTE rows, because a
    legion(8,4) cannot meet them by construction (slots 3 blocks off the goal, diameter 6.325).
    P4 also keeps the M4.14d guards: G DONE <= 246 ticks and G stuck events <= 6 (Q65).
#>
param([int]$TimeoutSec = 240, [switch]$Trace, [switch]$TraceRecovery, [switch]$TraceNavigator, [switch]$P4)

$ErrorActionPreference = 'Stop'
if ($TraceNavigator) { $TraceRecovery = $true }
if ($TraceRecovery) { $Trace = $true }
$root = $PSScriptRoot
$run = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed = Join-Path $root 'dev\testworld'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'formacije-navigacija-lib.ps1')
New-Item -ItemType Directory -Force -Path $audit | Out-Null
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$base = "rwform-$stamp-base"
$reportKind = if ($P4) { 'p4-formacije' } elseif ($TraceNavigator) { 'm414b-navigator' } elseif ($TraceRecovery) { 'm414b-recovery' } elseif ($Trace) { 'm414b-trace' } else { 'm414b' }
$prefix = Join-Path $audit "$reportKind-$stamp"
$failures = @()
$checks = @()
$arms = @{}
$srv = $null
$lock = $null
$config = Join-Path $run 'config\CustomNpcs.cfg'
$configHash = (Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash

function Check([string]$Name, [bool]$Ok) {
    $script:checks += [pscustomobject]@{ name = $Name; ok = $Ok }
    Write-Host ("{0} {1}" -f $(if ($Ok) { 'OK' } else { 'FAIL' }), $Name)
    if (-not $Ok) { $script:failures += $Name }
}
# P4 (Q257): an original criterion that the formation geometry cannot meet is recorded, not gated.
function Note([string]$Name, [bool]$Ok) {
    if (-not $P4) { Check $Name $Ok; return }
    $script:checks += [pscustomobject]@{ name = "NOTE $Name"; ok = $Ok; note = $true }
    Write-Host ("NOTE {0} {1}" -f $(if ($Ok) { 'met' } else { 'not met' }), $Name)
}
function Get-Log([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return '' }
    # cmd.exe keeps stdout open for writing throughout the server run. ReadAllText
    # opens with FileShare.Read and conflicts with that writer on Windows.
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
    $reader = $null
    try {
        $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8, $true)
        return $reader.ReadToEnd()
    } finally {
        if ($null -ne $reader) { $reader.Dispose() } else { $stream.Dispose() }
    }
}
function Wait-Marker($Server, [string]$Marker, [int]$Count = 1, [int]$Seconds = 30) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $log = Get-Log $Server.Log
        if ([regex]::Matches($log, [regex]::Escape($Marker)).Count -ge $Count) { return $true }
        $crash = Get-ServerSesutje $log
        if ($crash) { throw "Server crash while waiting for ${Marker}: $crash" }
        if ($Server.Proc.HasExited) { return $false }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    return $false
}
function Send-Command($Server, [string]$Command) {
    $Server.Proc.StandardInput.WriteLine($Command)
    $Server.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 300
}
function Stop-Server($Server) {
    try { Send-Command $Server 'stop' } catch { }
    if ($Server.Proc.WaitForExit(180000)) { return ($Server.Proc.ExitCode -eq 0) }
    & taskkill.exe /PID $Server.Proc.Id /T /F 2>&1 | Out-Null
    return $false
}
function Start-Server([string]$World, [string]$Tag) {
    $path = "$prefix-$Tag.log"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.Arguments = ('/c gradlew.bat runServer --offline --no-daemon --console=plain "-PrwWorld={0}" -PrwPrivzeto=1 > "{1}" 2>&1' -f $World, $path)
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    # Retain the handle even if readiness fails, so finally always owns cleanup.
    $script:srv = [pscustomobject]@{ Proc = [Diagnostics.Process]::Start($psi); Log = $path }
    if (-not (Wait-Marker $script:srv 'Done (' 1 900)) { throw "Server $Tag failed to start: $path" }
    if ((Get-Log $path) -notmatch ('Preparing level "' + [regex]::Escape($World) + '"')) {
        throw 'Server did not select the isolated scenario world.'
    }
    if ((Get-Log $path) -notmatch 'RWPRIVZETO 1 ') { throw 'Code defaults were not applied.' }
    $ready = $false
    for ($i = 0; $i -lt 6 -and -not $ready; $i++) {
        $before = [regex]::Matches((Get-Log $path), 'players online').Count
        Send-Command $script:srv 'list'
        $ready = Wait-Marker $script:srv 'players online' ($before + 1) 5
    }
    if (-not $ready) { throw 'Server input did not acknowledge list.' }
    return $script:srv
}
function Install-Controller([string]$Destination, [string]$Mode) {
    $source = [IO.File]::ReadAllText((Join-Path $seed 'nav-control.js'))
    if ($Mode -eq 'FORMACIJA') { $source = $source.Replace("var NAV_MODE = 'NAVIGATE';", "var NAV_MODE = 'FORMACIJA';") }
    if ($Trace -and $Mode -eq 'FORMACIJA') {
        if ($TraceRecovery) { $source += "`nvar formTraceRecovery = true;" }
        $source += "`n" + [IO.File]::ReadAllText((Join-Path $seed 'formacije-trace.js'))
        if ($TraceNavigator) {
            $source += "`n" + [IO.File]::ReadAllText((Join-Path $seed 'formacije-navigacija-trace.js'))
        }
    }
    $mini = [regex]::Replace($source, '//[^\n]*', '')
    $mini = [regex]::Replace($mini.Replace('"', "'"), '\s+', ' ').Trim()
    $encoded = $mini.Replace('\', '\\').Replace('"', '\"')
    $clone = [IO.File]::ReadAllText((Join-Path $seed 'customnpcs\clones\1\NAV_Control.json'))
    $pattern = '"Script"\s*:\s*"(?:\\.|[^"\\])*"'
    if ([regex]::Matches($clone, $pattern).Count -ne 1) { throw 'NAV_Control must have exactly one script.' }
    $clone = [regex]::Replace($clone, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) '"Script": "' + $encoded + '"' })
    [IO.File]::WriteAllText($Destination, $clone, (New-Object Text.UTF8Encoding $false))
}
function Read-Arrival([string]$Log, [string]$Lane) {
    $m = [regex]::Match($Log, ('NAV-CAS faza=B proga={0} prispelo=(\d+)/(\d+) prviTick=(-?\d+) medianaTick=(-?\d+) zadnjiTick=(-?\d+)' -f $Lane))
    if (-not $m.Success) { throw "Missing arrival summary for $Lane" }
    return [pscustomobject]@{ arrived = [int]$m.Groups[1].Value; count = [int]$m.Groups[2].Value; last = [int]$m.Groups[5].Value }
}
function Read-FinalSquad([string]$Log, [string]$Lane) {
    $pattern = 'RWSQUAD konec enota=\d+ ime=NAV_Walk' + $Lane + ' [^\r\n]*'
    $m = [regex]::Matches($Log, $pattern)
    if ($m.Count -ne 1) { return $null }
    $values = @{}
    foreach ($pair in [regex]::Matches($m[0].Value, '(\w+)=([^\s]+)')) { $values[$pair.Groups[1].Value] = $pair.Groups[2].Value }
    return $values
}
function Read-FormationTrace([string]$Log, [switch]$Recovery) {
    # Fail closed: a timer, reflection or logger failure must not resemble a
    # stationary member. Never use this diagnostic run as a timing comparison.
    $issues = @()
    $records = @{}
    $ids = @{}
    $start = if ($Recovery) { 'denseThrough=560 stride=1 recovery=true' } else { 'denseThrough=80 stride=20' }
    if ([regex]::Matches($Log, ('FORM-TRACE start squads=2 members=16 through=560 ' + [regex]::Escape($start) + '(?=\r?$)') , [Text.RegularExpressions.RegexOptions]::Multiline).Count -ne 1) {
        $issues += 'missing or duplicate trace start'
    }
    if ([regex]::Matches($Log, 'FORM-TRACE end tick=560\b').Count -ne 1) { $issues += 'missing or duplicate trace end' }
    $rows = [regex]::Matches($Log, 'FORM-D proga=(G|O) tick=(\d+) i=(\d+) ([^\r\n]+)')
    foreach ($row in $rows) {
        $lane = $row.Groups[1].Value
        $tick = [int]$row.Groups[2].Value
        $index = [int]$row.Groups[3].Value
        $key = "$lane/$tick/$index"
        if ($records.ContainsKey($key)) { $issues += "duplicate $key"; continue }
        $values = @{}
        foreach ($pair in [regex]::Matches($row.Groups[4].Value, '(\w+)=([^\s]+)')) {
            $values[$pair.Groups[1].Value] = $pair.Groups[2].Value
        }
        foreach ($required in @('uuid', 'age', 'plannerTick', 'faza', 'finished', 'pos', 'motion', 'input', 'aiSpeed',
                'helper', 'helperSpeed', 'helperTarget', 'target', 'final', 'slot', 'mode', 'speed', 'error', 'settled',
                'trailing', 'excused', 'lagTicks', 'pathUntil', 'noPath', 'pathIndex', 'pathLength', 'pathNext',
                'pathEnd', 'anchor', 'anchorSpeed', 'yaw', 'remaining', 'searches', 'stuck')) {
            if (-not $values.ContainsKey($required)) { $issues += "missing $required in $key" }
        }
        if ($values.faza -notin @('FORMING', 'MARCHING', 'SETTLING', 'DONE') -or $values.helper -notin @('WAIT', 'MOVE_TO', 'STRAFE', 'JUMPING')) {
            $issues += "invalid state in $key"
        }
        $decimal = '-?\d+(?:\.\d+)?'
        foreach ($vector in @('pos', 'motion', 'helperTarget', 'target', 'final', 'anchor')) {
            if ($values[$vector] -notmatch "^$decimal,$decimal,$decimal`$") { $issues += "invalid $vector in $key" }
        }
        foreach ($vector in @('input', 'slot')) {
            if ($values[$vector] -notmatch "^$decimal,$decimal`$") { $issues += "invalid $vector in $key" }
        }
        foreach ($scalar in @('aiSpeed', 'helperSpeed', 'speed', 'error', 'anchorSpeed', 'yaw', 'remaining')) {
            if ($values[$scalar] -notmatch "^$decimal`$") { $issues += "invalid $scalar in $key" }
        }
        foreach ($integer in @('age', 'plannerTick', 'mode', 'lagTicks', 'pathUntil', 'pathIndex', 'pathLength', 'searches', 'stuck')) {
            if ($values[$integer] -notmatch '^-?\d+$') { $issues += "invalid $integer in $key" }
        }
        foreach ($boolean in @('finished', 'settled', 'trailing', 'excused', 'noPath')) {
            if ($values[$boolean] -notin @('true', 'false')) { $issues += "invalid $boolean in $key" }
        }
        foreach ($vector in @('pathNext', 'pathEnd')) {
            if ($values[$vector] -ne 'none' -and $values[$vector] -notmatch "^$decimal,$decimal,$decimal`$") {
                $issues += "invalid $vector in $key"
            }
        }
        if ($Recovery) {
            foreach ($integer in @('recoveryIndex', 'lastPathTick')) {
                if ($values[$integer] -notmatch '^-?\d+$') { $issues += "invalid $integer in $key" }
            }
            foreach ($integer in @('navId', 'recoveryId', 'recoveryLength', 'trailUntil', 'byInterval', 'byMove', 'byLost')) {
                if ($values[$integer] -notmatch '^\d+$') { $issues += "invalid $integer in $key" }
            }
            if ($values.recoveryFinished -notin @('true', 'false')) { $issues += "invalid recoveryFinished in $key" }
            if ($values.lastPathTarget -notmatch "^$decimal,$decimal`$") { $issues += "invalid lastPathTarget in $key" }
            if ($values.recoveryEnd -ne 'none' -and $values.recoveryEnd -notmatch "^$decimal,$decimal,$decimal`$") {
                $issues += "invalid recoveryEnd in $key"
            }
            if ($values.navId -match '^\d+$') {
                if (($values.navId -eq '0') -ne ($values.pathIndex -eq '-1')) { $issues += "inconsistent navId in $key" }
            }
            if ($values.recoveryId -eq '0' -and ($values.recoveryIndex -ne '-1' -or $values.recoveryLength -ne '0' -or $values.recoveryFinished -ne 'true' -or $values.recoveryEnd -ne 'none')) {
                $issues += "inconsistent absent recovery in $key"
            }
            if ($values.byInterval -match '^\d+$' -and $values.byMove -match '^\d+$' -and $values.byLost -match '^\d+$' -and $values.searches -match '^\d+$') {
                if ([long]$values.byInterval + [long]$values.byMove + [long]$values.byLost -ne [long]$values.searches) {
                    $issues += "search reason sum mismatch in $key"
                }
            }
        }
        if ($row.Groups[4].Value -match '\b(?:NaN|Infinity|undefined|null)\b') { $issues += "non-numeric or missing value in $key" }
        $memberKey = "$lane/$index"
        if ($ids.ContainsKey($memberKey) -and $ids[$memberKey] -ne $values.uuid) { $issues += "member changed in $key" }
        $ids[$memberKey] = $values.uuid
        $records[$key] = $values
    }
    $ticks = if ($Recovery) { @(1..560) } else { @(1..80) + @(100..560 | Where-Object { $_ % 20 -eq 0 }) }
    foreach ($lane in @('G', 'O')) {
        for ($index = 0; $index -lt 8; $index++) {
            $firstAge = $null
            foreach ($tick in $ticks) {
                $key = "$lane/$tick/$index"
                if (-not $records.ContainsKey($key)) { $issues += "missing $key"; continue }
                $age = 0
                if (-not [int]::TryParse($records[$key].age, [ref]$age)) { continue }
                if ($null -eq $firstAge) { $firstAge = $age - $tick }
                if ($age - $tick -ne $firstAge) { $issues += "member did not tick in $key" }
            }
        }
    }
    if ($records.Count -ne 16 * $ticks.Count) { $issues += 'unexpected trace record count' }
    if (@($ids.Values | Select-Object -Unique).Count -ne 16) { $issues += 'expected sixteen distinct members' }
    return [pscustomobject]@{ ok = $issues.Count -eq 0; count = $records.Count; issues = $issues }
}

try {
    $env:JAVA_HOME = Join-Path $root '.tools\jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'formacije-run'
    if ((Get-Log (Join-Path $run 'eula.txt')) -notmatch 'eula\s*=\s*true') { throw 'EULA has not been accepted.' }
    $props = Get-Log (Join-Path $run 'server.properties')
    if ($props -notmatch '(?m)^enable-command-block\s*=\s*true' -or
        $props -notmatch '(?m)^level-type\s*=\s*FLAT' -or $props -notmatch '(?m)^level-seed\s*=\s*customnpc-rework') {
        throw 'Expected the existing superflat test configuration; no settings have been changed.'
    }
    if (Test-Path -LiteralPath (Join-Path $run $base)) { throw 'Scenario world already exists.' }
    $srv = Start-Server $base 'base'
    Send-Command $srv 'setworldspawn 0 4 0'
    Check 'base: spawn acknowledged' (Wait-Marker $srv 'Set the world spawn point')
    Send-Command $srv 'save-all flush'
    Check 'base: save acknowledged' (Wait-Marker $srv 'Saved the world' 1 180)
    Check 'base: clean stop' (Stop-Server $srv)
    $srv = $null
    if ($failures.Count) { throw 'Base world setup failed.' }

    foreach ($mode in @('NAVIGATE', 'FORMACIJA')) {
        $world = "rwform-$stamp-$($mode.ToLowerInvariant())"
        $dest = Join-Path $run $world
        if (Test-Path -LiteralPath $dest) { throw 'Scenario world already exists.' }
        Copy-Item -LiteralPath (Join-Path $run $base) -Destination $dest -Recurse
        $clones = Join-Path $dest 'customnpcs\clones\1'
        New-Item -ItemType Directory -Force -Path $clones | Out-Null
        foreach ($name in @('NAV_WalkG.json', 'NAV_WalkO.json')) {
            Copy-Item -LiteralPath (Join-Path $seed "customnpcs\clones\1\$name") -Destination $clones
        }
        Install-Controller (Join-Path $clones 'NAV_Control.json') $mode
        $srv = Start-Server $world $mode
        foreach ($line in (Get-Content -Encoding UTF8 (Join-Path $seed 'nav-setup-commands.txt'))) {
            if ($line.Trim() -and -not $line.Trim().StartsWith('#')) { Send-Command $srv $line.Trim() }
        }
        $fills = @([regex]::Matches((Get-Log $srv.Log), '(\d+) blocks filled') | ForEach-Object { [int]$_.Groups[1].Value })
        Check "$mode : full wall and two-block door placed" (($fills | Measure-Object -Maximum).Maximum -ge 100 -and $fills -contains 2)
        Send-Command $srv 'rwdiag chunks on 2'
        if (-not (Wait-Marker $srv 'RWDIAG-CHUNKS stanje=on')) { throw 'Chunk loader did not respond.' }
        $chunks = [regex]::Match((Get-Log $srv.Log), 'RWDIAG-CHUNKS stanje=on[^\r\n]*')
        Check "$mode : 16 forced NPCs and no rejected chunks" ($chunks.Success -and $chunks.Value -match 'npc=16\b' -and $chunks.Value -match 'zavrnjeni=0\b')
        if ($failures.Count) { throw 'Scene preconditions failed; no measurement collected.' }
        Start-Sleep -Seconds 10
        Send-Command $srv 'rwdiag on'
        if (-not (Wait-Marker $srv 'RWDIAG vklopljen')) { throw 'Diagnostic collector did not start.' }
        Send-Command $srv 'noppes clone spawn NAV_Control 1 0,4,56'
        if (-not (Wait-Marker $srv "FORM-MODE $mode" 1 60)) { throw 'Generated controller did not initialize in requested mode.' }
        Check "$mode : scene geometry/count" (Wait-Marker $srv 'NAV-SETUP progaG=8 progaO=8 vrataX=-10 zidZ=72 ciljZ=78' 1 60)
        if (-not (Wait-Marker $srv 'NAV-SUM progaG=8 progaO=8' 1 $TimeoutSec)) { throw "Scenario $mode did not finish." }
        Send-Command $srv 'rwdiag dump m414b'
        Check "$mode : diagnostic dump" (Wait-Marker $srv 'RWDIAG-DUMP ')
        Send-Command $srv 'rwdiag off'
        Send-Command $srv 'rwdiag chunks off'
        Check "$mode : clean stop" (Stop-Server $srv)
        $logPath = $srv.Log
        $srv = $null
        $custom = Get-Log (Join-Path $run 'logs\CustomNPCs-latest.log')
        [IO.File]::WriteAllText("$prefix-$mode-customnpcs.log", $custom)
        $raw = Get-Log $logPath
        # Command output is also echoed through LogWriter. Deduplicate script observations.
        $log = (($raw -split "`n" | Where-Object { $_ -notmatch '\[FINE/CustomNPCs\]' }) -join "`n")
        Check "$mode : no script/mod exception" (($raw + $custom) -notmatch 'script errored|ScriptException|Exception in server tick loop|ConcurrentModificationException')
        foreach ($lane in @('G', 'O')) {
            $samples = [regex]::Matches($log, ('NAV-S faza=B tick=\d+ proga={0}[^\r\n]*dStarost=(-?\d+)/(-?\d+)' -f $lane))
            Check "$mode/$lane : at least 20 samples" ($samples.Count -ge 20)
            $badAge = @($samples | Where-Object { [int]$_.Groups[1].Value -lt 15 -or [int]$_.Groups[2].Value -gt 25 })
            Check "$mode/$lane : entities tick every sample" ($samples.Count -gt 0 -and $badAge.Count -eq 0)
            $arms["$mode-$lane"] = Read-Arrival $log $lane
        }
        $arms["$mode-log"] = $log
        $arms["$mode-custom"] = $custom
    }

    foreach ($lane in @('G', 'O')) {
        $pattern = 'NAV-CAS faza=A proga=' + $lane + '[^\r\n]*'
        $a0 = [regex]::Match($arms['NAVIGATE-log'], $pattern)
        $a1 = [regex]::Match($arms['FORMACIJA-log'], $pattern)
        Check "control A/$lane : identical phase-A non-timing summary" ($a0.Success -and $a1.Success -and $a0.Value.Trim() -eq $a1.Value.Trim())
    }
    $b = $arms['FORMACIJA-log']
    if ($Trace) {
        $traceResult = Read-FormationTrace $arms['FORMACIJA-custom'] -Recovery:$TraceRecovery
        Check 'trace: complete unique member samples with continuous ages' $traceResult.ok
        $traceResult | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 "$prefix-trace-coverage.json"
        if ($TraceNavigator) {
            $navigation = Read-NavigationTrace $arms['FORMACIJA-custom']
            Check 'navigator: complete private state and monotonic observations' $navigation.ok
            foreach ($err in @($navigation.errors)) { Check "navigator read: $err" $false }
            $navigation | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 "$prefix-navigator.json"
        }
    }
    Note ('FA1: original common-goal metric G = 8/8 (observed {0})' -f $arms['FORMACIJA-G'].arrived) ($arms['FORMACIJA-G'].arrived -eq 8)
    Note ('FA2: original common-goal metric O = 8/8 (observed {0})' -f $arms['FORMACIJA-O'].arrived) ($arms['FORMACIJA-O'].arrived -eq 8)
    foreach ($lane in @('G', 'O')) {
        $slots = [regex]::Match($b, ('FORM-CAS proga={0} mesta=(\d+)/8 vsiTick=(-?\d+)' -f $lane))
        Check "FA1/2-slot $lane : 8 unique final slots simultaneously occupied" ($slots.Success -and [int]$slots.Groups[1].Value -eq 8)
        if ($lane -eq 'O') {
            $last = if ($slots.Success) { [int]$slots.Groups[2].Value } else { -1 }
            $ref = $arms['NAVIGATE-O']
            Check 'reference: O = 8/8 with valid last-arrival time' ($ref.arrived -eq 8 -and $ref.last -gt 0)
            Check 'FA3: all slots O by historical 220 * 1.25 = 275 ticks' ($last -ge 0 -and $last -le 275)
            Check 'FA3-live: all slots O within +25% of current script reference' ($last -ge 0 -and $ref.last -gt 0 -and $last -le 1.25 * $ref.last)
        }
        $march = [regex]::Matches($b, ('FORM-S proga={0} tick=\d+ mesta=\d+/8 napakaMax=[\d.]+ razpon=([\d.]+) [^\r\n]*faza=MARCHING\b' -f $lane))
        $max = ($march | ForEach-Object { [double]::Parse($_.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture) } | Measure-Object -Maximum).Maximum
        Note "FA4 $lane : marching diameter <= 6 (observed $max)" ($march.Count -gt 0 -and $max -le 6)
        $end = Read-FinalSquad $arms['FORMACIJA-custom'] $lane
        if ($P4) {
            $lag = if ($null -ne $end -and $end.ContainsKey('zaostanek_max')) { [double]::Parse($end.zaostanek_max, [Globalization.CultureInfo]::InvariantCulture) } else { -1 }
            Check "FA4-lag $lane : largest non-excused member lag while marching <= LAG_STOP + 1 = 6 (observed $lag)" ($lag -ge 0 -and $lag -le 6)
            if ($lane -eq 'G') {
                $doneTick = if ($null -ne $end) { [int]$end.tick } else { -1 }
                $stuck = if ($null -ne $end) { [int]$end.obtical } else { -1 }
                Check "guard G : DONE tick <= 246 (observed $doneTick)" ($doneTick -gt 0 -and $doneTick -le 246)
                Check "guard G : stuck events <= 6 (observed $stuck)" ($stuck -ge 0 -and $stuck -le 6)
            }
        }
        Check "FA6 $lane : one DONE record, no unreachable goal or unsettled members" ($null -ne $end -and $end.faza -eq 'DONE' -and $end.cilj_nedosegljiv -eq 'false' -and $end.neporavnani -eq '0')
        # Squad.status currently omits the initial leader search. Include that known call
        # explicitly in the reported real-search count; do not use Path identity counts.
        $actual = if ($null -ne $end) { 1 + [int]$end.iskanj } else { -1 }
        $limit = if ($null -ne $end) { 1 + [int]$end.obnov + [int]$end.obtical } else { -1 }
        Check "FA5 $lane : real searches $actual <= 1 + replans + stuck events ($limit)" ($null -ne $end -and $actual -le $limit)
    }
} catch {
    Check ("harness: {0}" -f $_) $false
} finally {
    if ($null -ne $srv) { Check 'finally: server stopped' (Stop-Server $srv) }
    Check 'config SHA256 unchanged' ((Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash -eq $configHash)
    if ($null -ne $lock) { $lock.Dispose() }
    $result = [ordered]@{ package = 'M4.14b'; stage = $(if ($TraceNavigator) { 'AB-NAVIGATOR' } elseif ($TraceRecovery) { 'AB-RECOVERY' } elseif ($Trace) { 'AB-TRACE' } else { 'AB' }); stamp = $stamp; checks = $checks; failures = $failures; arms = $arms }
    $result | ConvertTo-Json -Depth 9 | Set-Content -Encoding UTF8 "$prefix.json"
    $table = @('# M4.14b A/B', '', 'F1-F12 are pending; this report covers only FA1-FA6.', '', '| Check | Result |', '|---|---|')
    if ($Trace) { $table = @('# M4.14b diagnostic trace (not acceptance/performance proof)', '', 'All original FA checks are retained. F1-F12 are pending.', '', '| Check | Result |', '|---|---|') }
    foreach ($check in $checks) { $table += ('| {0} | {1} |' -f $check.name, $(if ($check.ok) { 'OK' } elseif ($check.note) { 'not met (NOTE, Q257)' } else { '**FAIL**' })) }
    $table | Set-Content -Encoding UTF8 "$prefix.md"
    Write-Host "Report: $prefix.md"
}
if ($failures.Count) { exit 1 }
exit 0
