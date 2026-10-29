# M4.3: opt-in only in disposable scenario clone copies. Never edits seed/config.
function Write-HoverFixture([string]$Source, [string]$Destination, [int]$Mode) {
    if ($Mode -eq -1) {
        Copy-Item -LiteralPath $Source -Destination $Destination
        return
    }
    $clone = [IO.File]::ReadAllText($Source)
    if ($Mode -ge 0) {
        if ($Mode -notin @(0, 1, 2)) { throw 'Invalid RwFlightMode fixture selection.' }
        if ($clone -match '"RwFlightMode"') { throw 'Seed already has RwFlightMode; inspect instead of overwriting.' }
        $start = $clone.IndexOf('{')
        if ($start -lt 0) { throw 'Missing root clone compound.' }
        $clone = $clone.Insert($start + 1, ('"RwFlightMode": {0},' -f $Mode))
    }
    [IO.File]::WriteAllText($Destination, $clone, (New-Object Text.UTF8Encoding $false))
}

# Installed script reads the actual saved selection and runtime helper outside measurement.
# Works in both LET_Control and PERF_Kontrola, without changing their baseline scripts.
function Add-HoverProof([string]$Path, [string]$Anchor, [string]$NpcExpression, [int]$Mode) {
    $clone = [IO.File]::ReadAllText($Path)
    $pattern = '"Script"\s*:\s*"((?:\\.|[^"\\])*)"'
    $matches = [regex]::Matches($clone, $pattern)
    if ($matches.Count -ne 1) { throw 'Expected exactly one controller Script.' }
    $code = ConvertFrom-Json ('"' + $matches[0].Groups[1].Value + '"')
    if ([regex]::Matches($code, [regex]::Escape($Anchor)).Count -ne 1) { throw 'Hover proof anchor is not unique.' }
    $proof = @'
var hAll = __NPC__.getWorld().getAllEntities(2), hFly = 0, hMode = 0, hHelper = 0, hOther = 0;
for (var hi = 0; hi < hAll.length; hi++) {
    var hn = hAll[hi], hmc = hn.getMCEntity();
    var hh = String(hmc.getMoveHelper().getClass().getName()) === 'noppes.npcs.rework.movement.HoverMoveHelper';
    var hname = String(hn.getName());
    if (hname === 'LET_Flyer' || hname === 'PERF_Leteci' || hname === 'PERF_LetA' || hname === 'PERF_LetB') {
        hFly++;
        if (hmc.getRwFlightMode().id() === __MODE__) { hMode++; }
        if (hh) { hHelper++; }
    } else if (hh) { hOther++; }
}
__NPC__.executeCommand('/say HOVER-PROOF mode=__MODE__ flyers=' + hFly + ' selected=' + hMode + ' helpers=' + hHelper + ' otherHover=' + hOther);
'@
    $proof = [regex]::Replace($proof, '\s+', ' ').Replace('__NPC__', $NpcExpression).Replace('__MODE__', [string]$Mode)
    $code = $code.Replace($Anchor, $Anchor + ' ' + $proof)
    $encoded = $code.Replace('\', '\\').Replace('"', '\"')
    $clone = [regex]::Replace($clone, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) '"Script": "' + $encoded + '"' })
    [IO.File]::WriteAllText($Path, $clone, (New-Object Text.UTF8Encoding $false))
}

function Test-HoverProof([string]$Text, [int]$Mode, [int]$Flyers, [int]$Minimum = 1) {
    $proofs = [regex]::Matches($Text, 'HOVER-PROOF mode=(\d+) flyers=(\d+) selected=(\d+) helpers=(\d+) otherHover=(\d+)')
    if ($proofs.Count -lt $Minimum) { return $false }
    $proofs = @($proofs | Select-Object -Last $Minimum)
    $helpers = if ($Mode -eq 1) { $Flyers } else { 0 }
    foreach ($p in $proofs) {
        if ([int]$p.Groups[1].Value -ne $Mode -or [int]$p.Groups[2].Value -ne $Flyers -or
            [int]$p.Groups[3].Value -ne $Flyers -or [int]$p.Groups[4].Value -ne $helpers -or
            [int]$p.Groups[5].Value -ne 0) { return $false }
    }
    return $true
}
