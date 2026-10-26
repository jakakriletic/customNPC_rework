# Pure readers, shared by the controller runner and its contract test.
function Read-NavigationTrace([string]$Log) {
    $base = Read-FormationTrace $Log -Recovery
    $issues = [Collections.Generic.List[string]]::new()
    foreach ($issue in $base.issues) { $issues.Add($issue) }
    $records = @{}
    $events = [Collections.Generic.List[object]]::new()
    $decimal = '-?\d+(?:\.\d+)?'
    # D-075: an overlay read failure is reported verbatim and always fails closed.
    $errors = @([regex]::Matches($Log, 'FORM-NAVERR [^\r\n]+') | ForEach-Object { $_.Value.Trim() })
    foreach ($err in $errors) { $issues.Add("navigator read failed: $err") }
    foreach ($line in [regex]::Matches($Log, 'FORM-D proga=(G|O) tick=(\d+) i=(\d+) ([^\r\n]+)')) {
        $row = @{}
        foreach ($pair in [regex]::Matches($line.Groups[4].Value, '(\w+)=([^\s]+)')) {
            $row[$pair.Groups[1].Value] = $pair.Groups[2].Value
        }
        $key = "$($line.Groups[1].Value)/$($line.Groups[2].Value)/$($line.Groups[3].Value)"
        $valid = $true
        foreach ($name in @('sampleMs', 'navTicks', 'navCheckTick', 'navTimeoutMs', 'navTimeoutLastMs', 'navUpdateWorldTick')) {
            $number = 0L
            if ($row[$name] -notmatch '^\d+$' -or -not [long]::TryParse($row[$name], [ref]$number)) {
                $issues.Add("invalid $name in $key"); $valid = $false
            }
        }
        foreach ($name in @('navCheckPos', 'navTimeoutNode', 'navTarget')) {
            if ($name -eq 'navTarget' -and $row[$name] -eq 'none') { continue }
            if ($row[$name] -notmatch "^$decimal,$decimal,$decimal`$") {
                $issues.Add("invalid $name in $key"); $valid = $false
            }
        }
        if ($row.navTimeoutLimit -notmatch '^\d+(?:\.\d+)?$' -or
                $row.navUpdate -notin @('true', 'false') -or
                $row.navClass -notmatch '^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+$') {
            $issues.Add("invalid navigator state in $key"); $valid = $false
        }
        if ($valid) {
            if ([long]$row.navCheckTick -gt [long]$row.navTicks -or
                    [long]$row.navTimeoutLastMs -gt [long]$row.sampleMs) {
                $issues.Add("inconsistent navigator clock in $key")
            }
            $records[$key] = $row
        }
    }
    foreach ($lane in @('G', 'O')) {
        foreach ($i in 0..7) {
            foreach ($tick in 2..560) {
                $key = "$lane/$tick/$i"
                $before = $records["$lane/$($tick - 1)/$i"]
                $after = $records[$key]
                if ($null -eq $before -or $null -eq $after) { continue }
                if ([long]$after.navTicks - [long]$before.navTicks -ne 1 -or
                        [long]$after.sampleMs -lt [long]$before.sampleMs -or
                        $after.navClass -ne $before.navClass) {
                    $issues.Add("navigator changed or observation clock discontinuity in $key")
                }
                # Report state transitions without asserting an unobserved caller.
                if ($before.navId -ne '0' -and [int]$before.pathIndex -lt [int]$before.pathLength -and
                        $after.navId -eq '0') {
                    $reset = [double]::Parse($after.navTimeoutLimit, [Globalization.CultureInfo]::InvariantCulture) -eq 0 -and
                        $after.navTimeoutMs -eq '0' -and $after.navTimeoutNode -match '^0(?:\.0+)?,0(?:\.0+)?,0(?:\.0+)?$' -and
                        ([long]$before.navTimeoutMs -gt 0 -or
                            [double]::Parse($before.navTimeoutLimit, [Globalization.CultureInfo]::InvariantCulture) -gt 0)
                    $events.Add([pscustomobject]@{ lane = $lane; tick = $tick; member = $i;
                        timeoutResetSignature = $reset; before = $before; after = $after })
                }
            }
        }
    }
    if ($records.Count -ne 8960) { $issues.Add('expected 8960 complete navigator records') }
    return [pscustomobject]@{ ok = $issues.Count -eq 0; count = $records.Count;
        issues = @($issues.ToArray()); errors = $errors; invalidations = @($events.ToArray()) }
}
