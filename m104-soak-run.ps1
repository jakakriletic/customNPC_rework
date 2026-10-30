# Controller-only M10.4: one uninterrupted 4 h packaged synthetic mixed load (D-096).
param([ValidateSet(1800)][int]$SetupAllowanceSeconds=1800)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
. (Join-Path $root 'dev/testworld/m104-soak-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix=Join-Path $root "audit/m104-soak-$stamp"
$work=Join-Path $root "dev/run-m104/$stamp"
$run=Join-Path $root 'dev/run'
$utf8=New-Object Text.UTF8Encoding $false
$proc=$null;$lock=$null;$exitCode=1;$failure='';$hashes=@{};$samples=@();$summary=$null;$logs=@()
$oldJava=$env:JAVA_HOME;$oldPath=$env:PATH
function Guard-M104 {
    $roots=@();if($null -ne $proc){$roots+= [int]$proc.Id}
    $foreign=@(Get-NavForeignJava -Roots $roots)
    Assert-M104 ($foreign.Count -eq 0) ('Foreign Java workload: '+($foreign -join '; '))
}
function Send-M104([string]$Text){$proc.StandardInput.WriteLine($Text);$proc.StandardInput.Flush()}
function Wait-M104([string]$Marker,[int]$Seconds=180,[int]$Offset=0) {
    $until=[DateTime]::UtcNow.AddSeconds($Seconds)
    while([DateTime]::UtcNow -lt $until) {
        $text=Read-JavaPrototypeLog $log;Test-M104Log $text
        if($text.Length -gt $Offset -and $text.Substring($Offset).Contains($Marker)){return}
        Assert-M104 (-not $proc.HasExited) "Server exited before $Marker"
        Guard-M104;Start-Sleep -Milliseconds 1000
    }
    throw "Timeout waiting for $Marker; $log"
}
function Save-M104 {
    $offset=(Read-JavaPrototypeLog $log).Length
    Send-M104 'save-all flush';Wait-M104 'Saved the world' 60 $offset
}
function Stop-M104 {
    Save-M104;Send-M104 'stop'
    $until=[DateTime]::UtcNow.AddSeconds(180)
    while(-not $proc.HasExited -and [DateTime]::UtcNow -lt $until){Guard-M104;Start-Sleep -Milliseconds 1000}
    Assert-M104 $proc.HasExited 'Clean server stop before timeout'
    [M61ProcessLog]::Finish($proc);Assert-M104 ($proc.ExitCode -eq 0) 'Server exit 0'
    Test-M104Log (Read-JavaPrototypeLog $log);$script:proc=$null;Guard-M104
}
function Start-M104([string]$Phase) {
    Guard-M104;$script:log="$prefix-$Phase.log";$script:logs+=$log
    $script:proc=Start-JavaPrototypeProcess $exe $arguments $work $log
    Wait-M104 'Done (' 1000
    $text=Read-JavaPrototypeLog $log
    Assert-M104 ($text.Contains('RWCORE coremod nalozen, runtimeDeobf=1')) 'Packaged candidate SRG coremod active'
    Assert-M104 ($text -cmatch 'RWPRIVZETO 1 spremenjenih=\d+ stikal=27 ') 'Code defaults applied'
    $defaults='RwAnimHooks=0 RwAttackPriority=0 RwBlinkRecipients=1 RwCollideSkip=1 RwDataShadow=1 RwFarRange=384 RwFarShaders=0 RwFarView=2 RwFlyLineOfSight=0 RwHeadwearSkip=1 RwHitbox=0 RwHitboxShieldItems= RwHitboxShieldWeight=200 RwImpostorAtlas=2048 RwImpostorStart=56 RwJavaClasspath= RwLabelCache=1 RwLodStart=0 RwMountSteering=0 RwMoveBoxes=1 RwNavBackend=0 RwPathFollowCache=1 RwPathNegCache=1 RwPathResume=0 RwPathSearchMemo=1 RwRenderBox=1 RwTargetPrefilter=2'
    Assert-M104 ($text.Contains('stikal=27 '+$defaults)) 'Exact pinned runtime defaults'
}
function Request-M104([int]$Seq) {
    $offset=(Read-JavaPrototypeLog $log).Length
    $tmp=Join-Path $work 'request.tmp';$target=Join-Path $work 'request.json'
    [IO.File]::WriteAllText($tmp,(@{seq=$Seq}|ConvertTo-Json -Compress),$utf8)
    Move-Item -LiteralPath $tmp -Destination $target -Force
    Wait-M104 "M104 SAMPLE $Seq success=true" 30 $offset
    $proof=[IO.File]::ReadAllText("$prefix-sample-$Seq.json")|ConvertFrom-Json
    Assert-M104 ($proof.success -eq $true -and $proof.seq -eq $Seq) "Sample $Seq proof identity"
    $script:samples+= $proof
}
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm104-soak'
    Guard-M104
    $jdk=Join-Path $root '.tools/jdk8';$env:JAVA_HOME=$jdk;$env:PATH="$jdk/bin;$env:PATH"
    $cp=Get-JavaPrototypeInputs $root
    $workspace=Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    $original=Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'
    $protected=@($workspace,$original,(Join-Path $root 'environment-lock.json'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected+=@(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File|Select-Object -ExpandProperty FullName)
    $options=Join-Path $run 'options.txt';$hadOptions=Test-Path -LiteralPath $options;if($hadOptions){$protected+=$options}
    foreach($path in $protected){$hashes[$path]=Get-JavaPrototypeHash $path}
    Assert-M104 ($hashes[$original] -ceq 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') 'Pinned original input'
    Assert-M104 (Test-JavaPrototypeJar $workspace 'noppes/npcs/rework/data/WorldSaveSession.class') 'Existing packaged candidate'
    Assert-M104 ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'Existing real EULA acceptance'
    $props=[IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert-M104 ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'Same flat world conditions'
    Assert-M104 (-not (Test-Path -LiteralPath $work)) 'Fresh isolated work directory'
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination $work -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $work
    New-Item -ItemType Directory -Path (Join-Path $work 'mods') | Out-Null
    Copy-Item -LiteralPath $workspace -Destination (Join-Path $work 'mods/customnpcs.jar')
    Assert-M104 ((Get-JavaPrototypeHash (Join-Path $work 'mods/customnpcs.jar')) -ceq $hashes[$workspace]) 'Installed jar byte identical'
    $clones=Join-Path $work 'world/customnpcs/clones/1';New-Item -ItemType Directory -Path $clones -Force | Out-Null
    $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/m104-soak-control.js'))
    $code=$code.Replace('@@OUTPUT@@',("$prefix-sample".Replace('\','/')|ConvertTo-Json -Compress)).Replace('@@REQUEST@@',((Join-Path $work 'request.json').Replace('\','/')|ConvertTo-Json -Compress))
    $codePath=Join-Path $work 'control.js';[IO.File]::WriteAllText($codePath,$code,$utf8)
    $clone=Join-Path $clones 'M104.json'
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $text=[IO.File]::ReadAllText($clone)
    Assert-M104 ($text.Contains('"Name": "PERF_Kontrola"') -and $text.Contains('"NpcJob": 0,')) 'Expected controller template'
    $text=$text.Replace('"Name": "PERF_Kontrola"','"Name": "M104"').Replace('"NpcJob": 0,','"NpcJob": 8, "ChunkPlayerLastSeen": 4102444800000L,')
    [IO.File]::WriteAllText($clone,$text,$utf8)
    $cache=Join-Path $env:USERPROFILE '.gradle/caches'
    $forge=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraftforge/forge/1.12.2-14.23.5.2847') '*-universal.jar'
    $mc=Join-Path $cache 'minecraft/net/minecraft/minecraft_server/1.12.2/minecraft_server-1.12.2.jar'
    $launch=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraft/launchwrapper/1.12') '*.jar'
    foreach($path in @($forge,$mc,$launch)){$hashes[$path]=Get-JavaPrototypeHash $path}
    Assert-M104 ($hashes[$forge] -ceq '29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A') 'Pinned Forge'
    Assert-M104 ($hashes[$mc] -ceq 'FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E') 'Pinned Minecraft'
    $deps=@()
    foreach($path in $cp.Split(';')) {
        if(-not (Test-Path -LiteralPath $path -PathType Leaf) -or -not $path.EndsWith('.jar')){continue}
        $hashes[$path]=Get-JavaPrototypeHash $path
        if((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
           (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class')){continue}
        $deps+=$path
    }
    $launchCp=(@($forge,$mc,$launch)+$deps|Select-Object -Unique)-join ';'
    $exe=Join-Path $jdk 'bin/java.exe'
    # Pin collector for synchronous full GC and identical first/last-hour measurement conditions.
    $arguments=@('-Xms2G','-Xmx2G','-XX:+UseParallelGC','-Drwprivzeto=1','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')
    Start-M104 'bootstrap';Send-M104 'setworldspawn 0 4 0';Wait-M104 'Set the world spawn point to (0, 4, 0)' 30;Stop-M104
    Start-M104 'soak';Send-M104 'noppes clone spawn M104 1 0,4,0';Wait-M104 'M104 READY workers=200' 180
    Send-M104 'rwdiag chunks on 1';Wait-M104 'RWDIAG-CHUNKS stanje=on' 30
    # Two-minute warmup; no accepted short mode. A failed warmup cannot be a passing soak.
    $warm=[Diagnostics.Stopwatch]::StartNew()
    while($warm.Elapsed.TotalSeconds -lt 120){Guard-M104;Test-M104Log (Read-JavaPrototypeLog $log);Assert-M104 (-not $proc.HasExited) 'Alive during warmup';Start-Sleep -Seconds 2}
    Save-M104;Request-M104 0
    $clock=[Diagnostics.Stopwatch]::StartNew()
    for($seq=1;$seq -le 48;$seq++) {
        while($clock.Elapsed.TotalSeconds -lt $seq*300) {
            Guard-M104;Test-M104Log (Read-JavaPrototypeLog $log);Assert-M104 (-not $proc.HasExited) 'Alive during soak';Start-Sleep -Seconds 2
        }
        Save-M104;Request-M104 $seq
        Write-Host "M104 progress $seq/48 elapsed=$([int]$clock.Elapsed.TotalSeconds)s heap=$($samples[-1].heap) threads=$($samples[-1].threads)"
    }
    $summary=Test-M104Samples $samples
    Stop-M104
    foreach($path in $hashes.Keys){Assert-M104 ((Get-JavaPrototypeHash $path) -ceq $hashes[$path]) "Protected input changed: $path"}
    Assert-M104 ((Test-Path -LiteralPath $options) -eq $hadOptions) 'Protected options presence'
    $exitCode=0
}catch{$failure=[string]$_;Write-Host "FAIL $failure"}
finally {
    if($null -ne $proc){try{if(-not $proc.HasExited){Send-M104 'stop';if(-not $proc.WaitForExit(30000)){Stop-JavaPrototypeProcess $proc}};[M61ProcessLog]::Finish($proc)}catch{Stop-JavaPrototypeProcess $proc; $exitCode=1}}
    @{success=($exitCode -eq 0);package='M10.4';error=$failure;samples=$samples;summary=$summary;logs=$logs;inputs=$hashes;scenario=$work;
        limits=@('synthetic dedicated world; Q1 absent','200 workers plus one observer; no clients or entities mod','original code-default JS scripting, no Java workload at empty compiler classpath','full GC every 5 min is part of the fixed measurement; p95 from complete hourly histograms')}|
        ConvertTo-Json -Depth 15|Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    if($null -ne $lock){$lock.Dispose()};$env:JAVA_HOME=$oldJava;$env:PATH=$oldPath
}
Write-Host "M10.4 evidence: $prefix.json"
exit $exitCode
