# Controller-only, no Minecraft or build. Q438: validate the heap evaluator before the long runs.
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'dev/testworld/m104-soak-lib.ps1')
$checks=0
function Check-M104([bool]$Ok,[string]$Message){Assert-M104 $Ok $Message;$script:checks++;Write-Host "OK $Message"}
function Good-Heap([int]$Count,[double]$MbPerSample) {
    $items=@()
    for($i=0;$i -le $Count;$i++) {
        $items+=[pscustomobject]@{success=$true;seq=$i;elapsedSeconds=$i*300;alive=200;moved=80;damaged=80;timers=40;heap=[long]((200+$i*$MbPerSample)*1048576);
            gcBefore=$i;gcAfter=$i+1;queue=0;active=0;maxDrift=6;peakDrift=6;recalls=0;forcedChunks=100;loadedChunks=300;combatEntries=1000;threads=36;p95ns=3000000}
    }
    return ,$items
}
try {
    foreach($file in @('m104-heap-run.ps1','m104-heap-samotest.ps1','dev/testworld/m104-soak-lib.ps1')) {
        $tokens=$null;$errors=$null
        $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $root $file),[ref]$tokens,[ref]$errors)
        Check-M104 ($errors.Count -eq 0) "PowerShell syntax $file $errors"
    }
    # PowerShell names are case-insensitive: a script variable sharing a typed parameter's name fails at run start.
    $ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $root 'm104-heap-run.ps1'),[ref]$tokens,[ref]$errors)
    $params=@($ast.ParamBlock.Parameters|ForEach-Object{$_.Name.VariablePath.UserPath})
    $clash=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.AssignmentStatementAst] -and $n.Left -is [Management.Automation.Language.VariableExpressionAst]},$true)|Where-Object{$params -contains ($_.Left.VariablePath.UserPath -replace '^script:','')})
    Check-M104 ($params.Count -eq 3 -and $clash.Count -eq 0) "No assignment to a run parameter: $($clash -join '; ')"
    $control=Join-Path $root 'dev/testworld/m104-soak-control.js'
    & (Join-Path $root '.tools/jdk8/bin/jjs.exe') (Join-Path $root 'dev/testworld/m104-soak-syntax.js') -- $control
    Check-M104 ($LASTEXITCODE -eq 0) 'Runtime Nashorn compiler accepts fixture'
    Check-M104 (([regex]::Matches([IO.File]::ReadAllText($control),'var LOCKED = false;')).Count -eq 1) 'Fixture has exactly one lock switch, off'
    Check-M104 (([regex]::Matches([IO.File]::ReadAllText($control),'var COMBAT_CAP = 0;')).Count -eq 1) 'Fixture has exactly one combat cap switch, off'
    Check-M104 (Test-Path -LiteralPath (Join-Path $root '.tools/jdk8/bin/jcmd.exe')) 'Pinned JDK has jcmd'
    # The pinned line must follow the code, otherwise the first server start fails only after minutes.
    $source=[IO.File]::ReadAllText((Join-Path $root 'dev/src/patch/java/noppes/npcs/CustomNpcs.java'))
    $found=New-Object 'System.Collections.Generic.List[string]'
    foreach($m in [regex]::Matches($source,'@ConfigProp[^\r\n]*\r?\n\s*public static \w+ (Rw\w+) = "?([^";]*)"?;')){$found.Add($m.Groups[1].Value+'='+$m.Groups[2].Value)}
    $found.Sort([StringComparer]::Ordinal)
    Check-M104 ($found.Count -eq 38) "38 Rw switches in code, found $($found.Count)"
    Check-M104 (($found -join ' ') -ceq $script:M104HeapDefaults) 'Pinned defaults equal the code'
    $flat=Test-M104HeapSamples (Good-Heap 24 0) 24 $true
    Check-M104 ([Math]::Abs($flat.slopeMbPerHour) -lt 0.01) 'Flat heap gives slope 0'
    $grow=Test-M104HeapSamples (Good-Heap 12 2.2) 12 $false
    Check-M104 ([Math]::Abs($grow.slopeMbPerHour-26.4) -lt 0.05) 'P1 rate 2.2 MB per sample gives 26.4 MB/h'
    Check-M104 ($grow.slopeMbPerHour -gt $script:M104HeapLockedSlope) 'P1 rate is over the locked limit'
    foreach($case in @(@('Short run',{param($x) $x[12].seq=11},'identity'),@('Late sample',{param($x) $x[6].elapsedSeconds=1861},'cadence'),
        @('Stopped patrol',{param($x) $x[6].moved=79},'workload'),@('No full GC',{param($x) $x[6].gcAfter=$x[6].gcBefore},'full GC'),@('Left the field',{param($x) $x[6].maxDrift=17},'locked field'),
        @('Excursion hidden by recall',{param($x) $x[6].peakDrift=17},'locked field'))) {
        $items=Good-Heap 12 0;$mutate=$case[1];& $mutate $items;$caught='';try{$null=Test-M104HeapSamples $items 12 $true}catch{$caught=[string]$_}
        Check-M104 ($caught -like "*$($case[2])*") "$($case[0]) rejected"
    }
    $a=Read-M104Histogram "1234:`n num     #instances         #bytes  class name`n----`n   1:        10        1000  [C`n   2:         5         500  java.lang.String`n"
    $b=Read-M104Histogram "1235:`n   1:        30        4000  [C`n   2:         1         100  net.minecraft.A`nTotal  31  4100`n"
    $d=Compare-M104Histogram $a $b
    Check-M104 ($d[0].class -ceq '[C' -and $d[0].bytes -eq 3000 -and $d[0].instances -eq 20 -and $d[-1].class -ceq 'java.lang.String' -and $d[-1].bytes -eq -500) 'Histogram diff'
    Write-Host "M104 heap harness PASS checks=$checks";exit 0
}catch{Write-Host "M104 heap harness FAIL checks=$checks $_";exit 1}
