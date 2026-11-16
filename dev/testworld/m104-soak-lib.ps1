# Fixed acceptance rules from D-096, operational choices Q237. Shared with negative selftests.
function Assert-M104([bool]$Ok,[string]$Message) { if(-not $Ok){throw $Message} }
function Get-M104Median($Values) {
    $v=@($Values|Sort-Object); Assert-M104 ($v.Count -gt 0) 'Missing median inputs'
    if($v.Count%2){return [double]$v[[int][Math]::Floor($v.Count/2)]}
    return ([double]$v[$v.Count/2-1]+[double]$v[$v.Count/2])/2
}
function Test-M104Samples($Samples) {
    $all=@($Samples)
    Assert-M104 ($all.Count -eq 49) 'Exactly 49 ordered samples required (0 + 48 saves)'
    for($i=0;$i -lt 49;$i++) {
        $v=$all[$i]
        foreach($key in @('seq','elapsedSeconds','hourSeconds','alive','moved','damaged','timers','minDistance','minTimerDelta','heap','threads','gcBefore','gcAfter','queue','active','writes','writesCompleted','persistedStep','ticks','p95ns')) {
            Assert-M104 ($null -ne $v.$key -and -not [double]::IsNaN([double]$v.$key) -and -not [double]::IsInfinity([double]$v.$key)) "Missing/nonfinite sample $i field $key"
        }
        Assert-M104 ($v.success -eq $true -and $v.seq -eq $i) "Sample $i identity/success"
        Assert-M104 ($v.alive -eq 200 -and $v.moved -eq 80 -and $v.damaged -eq 80 -and $v.timers -eq 40 -and $v.minDistance -gt 1 -and $v.minTimerDelta -gt 0) "Sample $i workload inactive"
        Assert-M104 ($v.heap -gt 0 -and $v.threads -gt 0 -and $v.gcAfter -gt $v.gcBefore) "Sample $i lacks completed full GC"
        Assert-M104 ($v.queue -eq 0 -and $v.active -eq 0) "Sample $i pending writes"
        Assert-M104 ($v.writesCompleted -ge $v.writes+1 -and $v.persistedStep -eq $v.writes*20) "Sample $i async writes not persisted"
        if($i -gt 0) {
            Assert-M104 ($v.elapsedSeconds -ge $i*300 -and $v.elapsedSeconds -le $i*300+60) "Sample $i wall-clock coverage/cadence"
            Assert-M104 ($v.writes -gt $all[$i-1].writes) "Sample $i no async write progress"
            Assert-M104 ($v.ticks -gt 0 -and $v.p95ns -gt 0) "Sample $i no tick histogram"
            if($i%12 -eq 0){Assert-M104 ($v.hourSeconds -ge 3590 -and $v.hourSeconds -le 3660 -and $v.ticks -ge 36000) "Hour $($i/12) missing coverage"}
        }
    }
    $first=@($all[1..12]);$last=@($all[37..48])
    $heapFirst=Get-M104Median $first.heap;$heapLast=Get-M104Median $last.heap
    $firstMax=($first.heap|Measure-Object -Maximum).Maximum;$lastMax=($last.heap|Measure-Object -Maximum).Maximum
    Assert-M104 ($heapLast -le $heapFirst*1.10 -and $lastMax -le $firstMax*1.10) 'Post-full-GC heap grew over 10 percent'
    $threadMin=($all[1..48].threads|Measure-Object -Minimum).Minimum
    $threadMax=($all[1..48].threads|Measure-Object -Maximum).Maximum
    $threadFirst=($first.threads|Measure-Object -Maximum).Maximum;$threadLast=($last.threads|Measure-Object -Maximum).Maximum
    Assert-M104 ($threadMax-$threadMin -le 4 -and $threadLast -le $threadFirst+2) 'Thread count unstable'
    Assert-M104 ($all[48].p95ns -le $all[12].p95ns*1.10) 'Last-hour p95 MSPT grew over 10 percent'
    return @{heapMedianFirst=$heapFirst;heapMedianLast=$heapLast;heapMaxFirst=$firstMax;heapMaxLast=$lastMax;
        threadMin=$threadMin;threadMax=$threadMax;p95FirstMs=$all[12].p95ns/1e6;p95LastMs=$all[48].p95ns/1e6;elapsedSeconds=$all[48].elapsedSeconds}
}
function Test-M104Log([string]$Text) {
    # Case-sensitive: FML's normal INFO "Attempting connection with missing mods" is harmless.
    # Q239: every packaged launch from the Gradle cache prints exactly these four main-thread
    # FML launcher lines before any mod loads; only they are removed, any other ERROR still fails.
    $Text=$Text -creplace '(?m)^\[[0-9:]+\] \[main/ERROR\] \[FML\]: (Apache Maven library folder was not in the format expected\. Using default libraries directory\.|Full: [^\r\n]*[\\/]maven-artifact-3\.5\.3\.jar|Trimmed: [^\r\n]*/org\.apache\.maven/maven-artifact/3\.5\.3/|FML appears to be missing any signature data\. This is not a good thing)\r?$',''
    Assert-M104 ($Text -cnotmatch 'M104 FAILED|/ERROR\]|\bat noppes\.|script errored|Exception ticking|Encountered an unexpected exception|NoSuchMethodError|NoSuchFieldError|AbstractMethodError|NoClassDefFoundError|OutOfMemoryError') 'Error/exception in soak log'
}
# Q438: short diagnostic after the P1 soak. Slope of post-full-GC heap, histogram diff. Not the 4 h acceptance.
$script:M104HeapDefaults='RwAnimCore=0 RwAnimHooks=0 RwArmies=0 RwArmyClusterMin=8 RwArmyClusterRadius=48 RwArmyCompany=8 RwArmyMaxMarches=2 RwAttackPriority=0 RwBlinkRecipients=1 RwCollideSkip=1 RwCombatEntryCap=0 RwCommandStaff=0 RwDataShadow=1 RwFarRange=384 RwFarShaders=0 RwFarView=2 RwFlyLineOfSight=0 RwHeadwearSkip=1 RwHitbox=0 RwHitboxShieldItems= RwHitboxShieldWeight=200 RwImpostorAtlas=2048 RwImpostorStart=56 RwJavaClasspath= RwLabelCache=1 RwLinkedBudget=50 RwLinkedGroups=0 RwLinkedOwnFields=0 RwLodStart=0 RwMountSteering=0 RwMoveBoxes=1 RwNavBackend=0 RwPathFollowCache=1 RwPathNegCache=1 RwPathResume=0 RwPathSearchMemo=1 RwRenderBox=1 RwTargetPrefilter=2'
$script:M104HeapLockedDrift=16
$script:M104HeapLockedSlope=2.65
function Test-M104HeapSamples($Samples,[int]$Count,[bool]$Locked) {
    $all=@($Samples)
    Assert-M104 ($all.Count -eq $Count+1) 'Exactly one sample 0 plus the requested samples'
    for($i=0;$i -le $Count;$i++) {
        $v=$all[$i]
        Assert-M104 ($v.success -eq $true -and $v.seq -eq $i) "Sample $i identity/success"
        Assert-M104 ($v.alive -eq 200 -and $v.moved -eq 80 -and $v.damaged -eq 80 -and $v.timers -eq 40) "Sample $i workload inactive"
        Assert-M104 ($v.heap -gt 0 -and $v.gcAfter -gt $v.gcBefore) "Sample $i lacks completed full GC"
        Assert-M104 ($v.queue -eq 0 -and $v.active -eq 0) "Sample $i pending writes"
        if($i -gt 0){Assert-M104 ($v.elapsedSeconds -ge $i*300 -and $v.elapsedSeconds -le $i*300+60) "Sample $i wall-clock coverage/cadence"}
        if($Locked){Assert-M104 ($null -ne $v.peakDrift -and $v.maxDrift -le $script:M104HeapLockedDrift -and $v.peakDrift -le $script:M104HeapLockedDrift) "Sample $i left the locked field"}
    }
    # Samples 0-2 hold the JVM/thread ramp seen in the P1 soak (threads 12 -> 36); fixed before the run.
    $fit=@($all[3..$Count]);$n=$fit.Count
    $sx=0.0;$sy=0.0;foreach($v in $fit){$sx+=[double]$v.elapsedSeconds/3600;$sy+=[double]$v.heap/1048576}
    $mx=$sx/$n;$my=$sy/$n;$sxx=0.0;$sxy=0.0
    foreach($v in $fit){$dx=[double]$v.elapsedSeconds/3600-$mx;$sxx+=$dx*$dx;$sxy+=$dx*([double]$v.heap/1048576-$my)}
    $slope=$sxy/$sxx;$sse=0.0
    foreach($v in $fit){$e=[double]$v.heap/1048576-($my+$slope*([double]$v.elapsedSeconds/3600-$mx));$sse+=$e*$e}
    return @{slopeMbPerHour=$slope;residualMb=[Math]::Sqrt($sse/($n-2));firstMb=[double]$fit[0].heap/1048576;lastMb=[double]$fit[$n-1].heap/1048576;
        maxDrift=($all.maxDrift|Measure-Object -Maximum).Maximum;peakDrift=($all.peakDrift|Measure-Object -Maximum).Maximum;firstChunks=$fit[0].forcedChunks;lastChunks=$fit[$n-1].forcedChunks;
        firstLoaded=$fit[0].loadedChunks;lastLoaded=$fit[$n-1].loadedChunks;threadsLast=$fit[$n-1].threads;firstCombatEntries=$fit[0].combatEntries;lastCombatEntries=$fit[$n-1].combatEntries;recallsLast=$fit[$n-1].recalls;p95LastMs=[double]$fit[$n-1].p95ns/1e6}
}
function Read-M104Histogram([string]$Text) {
    $map=New-Object Collections.Hashtable # Case-sensitive: class names differ by case only in obfuscated libraries.
    foreach($m in [regex]::Matches($Text,'(?m)^\s*\d+:\s+(\d+)\s+(\d+)\s+(\S+)')){$map[$m.Groups[3].Value]=@([long]$m.Groups[1].Value,[long]$m.Groups[2].Value)}
    Assert-M104 ($map.Count -gt 0) 'Empty class histogram'
    return $map
}
function Compare-M104Histogram($First,$Last,[int]$Top=40) {
    $rows=@()
    foreach($name in @($First.Keys)+@($Last.Keys)|Select-Object -Unique) {
        $a=if($First.ContainsKey($name)){$First[$name]}else{@(0,0)};$b=if($Last.ContainsKey($name)){$Last[$name]}else{@(0,0)}
        $rows+=[pscustomobject]@{class=$name;instances=$b[0]-$a[0];bytes=$b[1]-$a[1];lastInstances=$b[0];lastBytes=$b[1]}
    }
    return @($rows|Sort-Object -Property bytes -Descending|Select-Object -First $Top)
}
