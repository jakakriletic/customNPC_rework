# Controller-only, no Minecraft or build. Validate the evaluator before the costly 4 h run.
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'dev/testworld/m104-soak-lib.ps1')
$checks=0
function Check-M104([bool]$Ok,[string]$Message){Assert-M104 $Ok $Message;$script:checks++;Write-Host "OK $Message"}
function Good-M104 {
    $items=@()
    for($i=0;$i -le 48;$i++) {
        $hour=if($i -eq 0){0}elseif($i%12 -eq 0){3600}else{($i%12)*300}
        $items+=[pscustomobject]@{success=$true;seq=$i;elapsedSeconds=$i*300;hourSeconds=$hour;alive=200;moved=80;damaged=80;timers=40;
            minDistance=100;minTimerDelta=290;heap=100000000;threads=20;gcBefore=$i;gcAfter=$i+1;queue=0;active=0;writes=$i*15;writesCompleted=$i*15+1;persistedStep=$i*300;ticks=72000;p95ns=10000000}
    }
    return ,$items
}
function Reject-M104([string]$Label,[scriptblock]$Mutation,[string]$Reason) {
    $items=Good-M104; & $Mutation $items
    $caught='';try{$null=Test-M104Samples $items}catch{$caught=[string]$_}
    Check-M104 ($caught -like "*$Reason*") "$Label rejected for expected reason: $Reason"
}
try {
    foreach($file in @('m104-soak-run.ps1','m104-soak-samotest.ps1','dev/testworld/m104-soak-lib.ps1')) {
        $tokens=$null;$errors=$null
        $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $root $file),[ref]$tokens,[ref]$errors)
        Check-M104 ($errors.Count -eq 0) "PowerShell syntax $file $errors"
    }
    & (Join-Path $root '.tools/jdk8/bin/jjs.exe') (Join-Path $root 'dev/testworld/m104-soak-syntax.js') -- (Join-Path $root 'dev/testworld/m104-soak-control.js')
    Check-M104 ($LASTEXITCODE -eq 0) 'Exact runtime Nashorn compiler accepts fixture'
    & node (Join-Path $root 'dev/testworld/m104-controller-budget.mjs')
    Check-M104 ($LASTEXITCODE -eq 0) 'Controller budget: fixed 4 h plus explicit preparation allowance; other operations unchanged'
    $good=Good-M104;$result=Test-M104Samples $good
    Check-M104 ($result.elapsedSeconds -eq 14400 -and $result.p95FirstMs -eq 10) 'Valid full 4 h fixture accepted'
    Reject-M104 'Missing sample' {param($x) $x[48].seq=47} 'identity'
    Reject-M104 'Short run' {param($x) $x[48].elapsedSeconds=14399} 'coverage'
    Reject-M104 'Late save' {param($x) $x[20].elapsedSeconds=6061} 'cadence'
    Reject-M104 'Incomplete hour' {param($x) $x[12].hourSeconds=3500} 'coverage'
    Reject-M104 'Missing workers' {param($x) $x[8].alive=199} 'workload'
    Reject-M104 'Patrol stopped' {param($x) $x[8].moved=79} 'workload'
    Reject-M104 'Combat stopped' {param($x) $x[8].damaged=79} 'workload'
    Reject-M104 'Timer stopped' {param($x) $x[8].timers=39} 'workload'
    Reject-M104 'GC absent' {param($x) $x[8].gcAfter=$x[8].gcBefore} 'full GC'
    Reject-M104 'Queued save' {param($x) $x[8].queue=1} 'pending'
    Reject-M104 'Executing save' {param($x) $x[8].active=1} 'pending'
    Reject-M104 'No new writes' {param($x) $x[8].writes=$x[7].writes;$x[8].writesCompleted=$x[7].writesCompleted;$x[8].persistedStep=$x[7].persistedStep} 'progress'
    Reject-M104 'Write not completed' {param($x) $x[8].writesCompleted=0} 'persisted'
    Reject-M104 'Stale file' {param($x) $x[8].persistedStep=0} 'persisted'
    Reject-M104 'Missing metric' {param($x) $x[8].heap=$null} 'Missing'
    Reject-M104 'Nonfinite metric' {param($x) $x[8].heap=[double]::NaN} 'nonfinite'
    Reject-M104 'Heap trend' {param($x) foreach($i in 37..48){$x[$i].heap=110000001}} 'heap grew'
    Reject-M104 'Heap spike' {param($x) $x[48].heap=110000001} 'heap grew'
    Reject-M104 'Thread trend' {param($x) foreach($i in 37..48){$x[$i].threads=23}} 'Thread count'
    Reject-M104 'Thread spike' {param($x) $x[24].threads=25} 'Thread count'
    Reject-M104 'MSPT trend' {param($x) $x[48].p95ns=11000001} 'p95 MSPT'
    Reject-M104 'No tick data' {param($x) $x[12].ticks=0} 'histogram'
    $boundary=Good-M104
    foreach($i in 37..48){$boundary[$i].heap=110000000;$boundary[$i].threads=22};$boundary[48].p95ns=11000000
    $null=Test-M104Samples $boundary;Check-M104 $true 'Exact permitted boundaries accepted'
    Test-M104Log '[Server thread/INFO] Attempting connection with missing mods';Check-M104 $true 'Normal FML INFO accepted'
    $launcher=@('[02:31:31] [main/ERROR] [FML]: Apache Maven library folder was not in the format expected. Using default libraries directory.',
        '[02:31:31] [main/ERROR] [FML]: Full: C:\Users\x\.gradle\caches\modules-2\files-2.1\org.apache.maven\maven-artifact\3.5.3\7dc7\maven-artifact-3.5.3.jar',
        '[02:31:31] [main/ERROR] [FML]: Trimmed: c:/users/x/.gradle/caches/modules-2/files-2.1/org.apache.maven/maven-artifact/3.5.3/',
        '[02:31:32] [main/ERROR] [FML]: FML appears to be missing any signature data. This is not a good thing')
    Test-M104Log (($launcher+'[02:31:38] [Server thread/INFO] Done') -join "`r`n");Check-M104 $true 'Four fixed FML launcher lines accepted (Q239)'
    foreach($bad in @('[02:31:31] [main/ERROR] [FML]: Some other loader failure',($launcher[3]+"`r`n[02:40:00] [Server thread/ERROR] [FML]: x"),($launcher[0]+' extra'),'[Server thread/ERROR] write failed','M104 FAILED X',"`tat noppes.npcs.X.x(X.java:1)",'NoSuchMethodError','OutOfMemoryError')){
        $caught=$false;try{Test-M104Log $bad}catch{$caught=$true};Check-M104 $caught "Bad log rejected: $bad"
    }
    $stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss'
    @{success=$true;checks=$checks;scope='synthetic evaluator and compile only, no Minecraft'}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $root "audit/m104-samotest-$stamp.json") -Encoding UTF8
    Write-Host "M104 harness PASS checks=$checks";exit 0
}catch{Write-Host "M104 harness FAIL checks=$checks $_";exit 1}
