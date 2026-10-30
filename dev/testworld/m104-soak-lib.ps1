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
