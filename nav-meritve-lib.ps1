# M4.15a: measurement helpers only. No edits to the seed, config or optional library.
function Get-NavForeignJava {
    param([int[]]$Roots = @())
    # Use ONE enumeration for ancestry and Java classification. A child starting
    # between two separate enumerations can otherwise look like a foreign process.
    # Failure to read the process list is fatal, not an empty/clean observation.
    $snapshot = @(Get-CimInstance Win32_Process -ErrorAction Stop)
    $owned = @{}
    $pending = New-Object System.Collections.Queue
    foreach ($rootId in $Roots) { $owned[$rootId] = $true; $pending.Enqueue($rootId) }
    while ($pending.Count -gt 0) {
        $parentId = $pending.Dequeue()
        foreach ($process in $snapshot) {
            $processId = [int]$process.ProcessId
            if ([int]$process.ParentProcessId -eq $parentId -and -not $owned.ContainsKey($processId)) {
                $owned[$processId] = $true
                $pending.Enqueue($processId)
            }
        }
    }
    foreach ($process in $snapshot) {
        if ($process.Name -notin @('java.exe', 'javaw.exe') -or $owned.ContainsKey([int]$process.ProcessId)) { continue }
        $command = [string]$process.CommandLine
        if ($command -match $script:TujiJavaVzorec) {
            '{0} (parent {1}): {2}' -f $process.ProcessId, $process.ParentProcessId, $command.Substring(0, [Math]::Min(160, $command.Length))
        }
    }
}

function Get-NavModPath([string]$Root, [string]$LibraryRoot = '') {
    $candidates = if ($LibraryRoot) { @($LibraryRoot) } else {
        @('..\barittone_for_npc_rework\barritone_converted_for_entities', '..\barittone_for_npc_rework')
    }
    foreach ($candidate in $candidates) {
        if (-not [IO.Path]::IsPathRooted($candidate)) { $candidate = Join-Path $Root $candidate }
        $mod = [IO.Path]::GetFullPath((Join-Path $candidate 'mod'))
        if (Test-Path -LiteralPath (Join-Path $mod 'build\classes\java\main\si\ladja\npcbaritone\api\NpcBaritone.class')) { return $mod }
    }
    throw 'Optional npcbaritone compiled runtime is absent; do not substitute an API jar or build it during measurement.'
}

function Get-NavRuntimeHash([string]$Mod) {
    $lines = @()
    foreach ($dir in @('build\classes\java\main', 'build\resources\main')) {
        $path = Join-Path $Mod $dir
        if (-not (Test-Path -LiteralPath $path)) { throw "Missing runtime input: $path" }
        foreach ($file in @(Get-ChildItem -LiteralPath $path -Recurse -File | Sort-Object FullName)) {
            $lines += $dir + '/' + $file.FullName.Substring($path.Length).Replace('\', '/') + ':' + (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
        }
    }
    $hash = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes(($lines -join "`n"))))).Replace('-', '') }
    finally { $hash.Dispose() }
}

function Write-NavFixture([string]$Source, [string]$Destination, [string]$Name = '', [int]$Size = 0, [int]$Movement = -1) {
    $text = [IO.File]::ReadAllText($Source)
    if ($text -match '"RwNavBackend"') { throw 'Seed already selects a navigation backend.' }
    $at = $text.IndexOf('{')
    if ($at -lt 0) { throw 'Missing clone root compound.' }
    $text = $text.Insert($at + 1, '"RwNavBackend": 1,')
    foreach ($change in @(@('Name', $Name, '"[^"]*"'), @('Size', $Size, '\d+'), @('MovementType', $Movement, '\d+'))) {
        $key = $change[0]; $value = $change[1]
        if (($key -eq 'Name' -and $value -eq '') -or ($key -eq 'Size' -and $value -eq 0) -or ($key -eq 'MovementType' -and $value -eq -1)) { continue }
        $pattern = '"' + $key + '"\s*:\s*' + $change[2]
        # Attribute compounds also contain Name; only replace the root display name.
        if ($key -eq 'Name') { $pattern = '(?m)^    ' + $pattern }
        if ([regex]::Matches($text, $pattern).Count -ne 1) { throw "Expected one clone $key." }
        if ($key -eq 'Name') { $value = '"' + $value + '"' }
        $replacement = '"{0}": {1}' -f $key, $value
        if ($key -eq 'Name') { $replacement = '    ' + $replacement }
        $text = [regex]::Replace($text, $pattern, $replacement)
    }
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}

function Install-NavController([string]$Source, [string]$CodePath, [string]$Destination) {
    $text = [IO.File]::ReadAllText($Source)
    $pattern = '"Script"\s*:\s*"(?:\\.|[^"\\])*"'
    if ([regex]::Matches($text, $pattern).Count -ne 1) { throw 'Expected one controller Script.' }
    # Clone files use NbtJson's legacy string format, NOT JSON string escapes.
    # readQuoted consumes the character after any backslash literally: JSON \n
    # becomes n and \u0027 becomes u0027. Keep raw line breaks (including those
    # terminating // comments), and escape only backslashes and double quotes.
    $code = [IO.File]::ReadAllText($CodePath).Replace('\', '\\').Replace('"', '\"')
    $text = [regex]::Replace($text, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) '"Script": "' + $code + '"' })
    [IO.File]::WriteAllText($Destination, $text, (New-Object Text.UTF8Encoding $false))
}

function Read-NavStatus([string]$Text) {
    $matches = [regex]::Matches($Text, 'RWNAV global=(\d) mod=(\w+) ime=(\S*) ujemanj=(\d+) izbranih=(\d+) pripetih=(\d+)')
    if (-not $matches.Count) { return $null }
    $g = $matches[$matches.Count - 1].Groups
    return @{ Global = [int]$g[1].Value; Mod = $g[2].Value; Prefix = $g[3].Value; Count = [int]$g[4].Value; Selected = [int]$g[5].Value; Attached = [int]$g[6].Value }
}

function Test-NavStatus($Status, [int]$Global, [int]$Count, [int]$Attached) {
    return $null -ne $Status -and $Status.Global -eq $Global -and $Status.Mod -eq 'true' -and
        $Status.Prefix -eq 'PERF_' -and $Status.Count -eq $Count -and $Status.Selected -eq $Count -and $Status.Attached -eq $Attached
}

function Read-NavPerf([string]$Text) {
    $matches = [regex]::Matches($Text, 'NPCB-PERF iskanj=(\d+) \S+=(\d+) \S+=(\d+) \S+=(\d+) \S+=(\d+) \| iskanje \S+ p50=(\d+) p95=(\d+) \| vrsta \S+ p50=(\d+) p95=(\d+) \| posnetek \S+ p50=(\d+) p95=(\d+) \(chunkov p50=(\d+)\) \| glavna nit \S+ p50=(\d+) p95=(\d+) p99=(\d+) max=(\d+) \(tickov (\d+)\)')
    if (-not $matches.Count) { return $null }
    $g = $matches[$matches.Count - 1].Groups
    $keys = @('searches', 'completed', 'rejected', 'failed', 'shared', 'search.us.p50', 'search.us.p95', 'queue.us.p50', 'queue.us.p95', 'snapshot.us.p50', 'snapshot.us.p95', 'snapshot.chunks.p50', 'main.us.p50', 'main.us.p95', 'main.us.p99', 'main.us.max', 'ticks')
    $result = @{}
    for ($i = 0; $i -lt $keys.Count; $i++) { $result[$keys[$i]] = [long]$g[$i + 1].Value }
    return $result
}

function Test-NavCompatibility([string]$Text) {
    $rows = [regex]::Matches($Text, 'NAVC phase=(mounted|dismounted|rebuilt) name=(Normal|Large|Fly|Swim|Rider|Carrier) selected=(\d+) attached=(\d+) movement=(\d+) riding=(\d+) carrying=(\d+) width=([\d.]+) height=([\d.]+) distance=([\d.]+)')
    # Duplicate/missing rows are errors, not a reason to pick a favourable last sample.
    if ($rows.Count -ne 18 -or $Text -notmatch 'NAVC DONE count=6') { return $false }
    foreach ($phase in @('mounted', 'dismounted', 'rebuilt')) {
        foreach ($name in @('Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier')) {
            $row = @($rows | Where-Object { $_.Groups[1].Value -eq $phase -and $_.Groups[2].Value -eq $name })
            if ($row.Count -ne 1) { return $false }
            $g = $row[0].Groups
            $attached = if ($name -eq 'Normal' -or ($name -in @('Rider', 'Carrier') -and $phase -ne 'mounted')) { 1 } else { 0 }
            $movement = if ($name -eq 'Fly') { 1 } elseif ($name -eq 'Swim') { 2 } else { 0 }
            $riding = if ($name -eq 'Rider' -and $phase -eq 'mounted') { 1 } else { 0 }
            $carrying = if ($name -eq 'Carrier' -and $phase -eq 'mounted') { 1 } else { 0 }
            if ([int]$g[3].Value -ne 1 -or [int]$g[4].Value -ne $attached -or [int]$g[5].Value -ne $movement -or
                [int]$g[6].Value -ne $riding -or [int]$g[7].Value -ne $carrying) { return $false }
            $w = [double]::Parse($g[8].Value, [Globalization.CultureInfo]::InvariantCulture)
            $h = [double]::Parse($g[9].Value, [Globalization.CultureInfo]::InvariantCulture)
            # LET_Walker is EntityCustomNpc, whose unscaled humanoid baseHeight is
            # 1.9 (EntityCustomNpc.updateHitbox), not the interface's initial 1.8.
            # Original mount mode 0 retains the 0.77 riding height after dismount
            # and updateTasks (RiderState.hitboxFollowsRiding; D-019). No reset or
            # mount switch is injected by this probe. Keep exact per-actor sizes.
            $expectedWidth = if ($name -eq 'Large') { 1.2 } else { 0.6 }
            $expectedHeight = if ($name -eq 'Large') { 3.8 } elseif ($name -eq 'Rider') { 1.9 * 0.77 } else { 1.9 }
            if ([Math]::Abs($w - $expectedWidth) -gt 0.01 -or [Math]::Abs($h - $expectedHeight) -gt 0.01) { return $false }
            if ($phase -eq 'rebuilt' -and $name -in @('Normal', 'Large')) {
                if ([double]::Parse($g[10].Value, [Globalization.CultureInfo]::InvariantCulture) -gt 2) { return $false }
            }
        }
    }
    return $true
}
