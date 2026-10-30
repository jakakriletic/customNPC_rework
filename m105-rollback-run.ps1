# Controller-only M10.5, D-096. Real packaged jar swaps on a disposable synthetic world.
# Never modifies dev/run, the distribution jar, source, or the owner's world.
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix=Join-Path $root "audit/m105-rollback-$stamp"
$scenario=Join-Path $root "dev/run-m105/$stamp"
$work=Join-Path $scenario 'install'
$backup=Join-Path $scenario 'backup-before-candidate'
$candidateBackup=Join-Path $scenario 'backup-candidate'
$restored=Join-Path $scenario 'restored'
$run=Join-Path $root 'dev/run'
$utf8=New-Object Text.UTF8Encoding $false
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
$lock=$null; $proc=$null; $exitCode=1; $hashes=@{}; $logs=@(); $proofs=@(); $failure=''
$failPattern='M105 FAILED|M105 [A-Z-]+ DONE success=false|Exception ticking|Encountered an unexpected exception|NoSuchMethodError|NoSuchFieldError|AbstractMethodError|NoClassDefFoundError'
$stateItems=@('world','config','mods','server.properties','eula.txt')
# Pin every runtime switch to the same code defaults as attempt 1. rwprivzeto=1 also
# resets the String RwJavaClasspath to empty, so use a fresh O config (no Rw keys)
# and report actual values with rwprivzeto=0. Only the compiler fixture is configured.
$defaults='RwAnimHooks=0 RwAttackPriority=0 RwBlinkRecipients=1 RwCollideSkip=1 RwDataShadow=1 RwFarRange=384 RwFarShaders=0 RwFarView=2 RwFlyLineOfSight=0 RwHeadwearSkip=1 RwHitbox=0 RwHitboxShieldItems= RwHitboxShieldWeight=200 RwImpostorAtlas=2048 RwImpostorStart=56 RwJavaClasspath=@@CLASSPATH@@ RwLabelCache=1 RwLodStart=0 RwMountSteering=0 RwMoveBoxes=1 RwNavBackend=0 RwPathFollowCache=1 RwPathNegCache=1 RwPathResume=0 RwPathSearchMemo=1 RwRenderBox=1 RwTargetPrefilter=2'
function Guard-M105 {
    $roots=@(); if($null -ne $proc){$roots+= [int]$proc.Id}
    $foreign=@(Get-NavForeignJava -Roots $roots)
    if($foreign.Count){throw ('Foreign Java workload: '+($foreign -join '; '))}
}
function Send-M105([string]$Text){$proc.StandardInput.WriteLine($Text);$proc.StandardInput.Flush()}
function Wait-M105([string]$Marker,[int]$Seconds=600){
    $until=(Get-Date).AddSeconds($Seconds)
    while((Get-Date)-lt $until){
        $text=Read-JavaPrototypeLog $log
        if($text -cmatch $failPattern){throw "Runtime failure; $log"}
        if($text.Contains($Marker)){return}
        if($proc.HasExited){throw "Server exited before $Marker; $log"}
        Guard-M105;Start-Sleep -Milliseconds 500
    }
    throw "Timeout waiting for $Marker; $log"
}
function Start-M105([string]$Arm,[string]$Phase){
    Guard-M105
    $script:log="$prefix-$Phase.log";$script:logs+=$script:log
    $script:proc=Start-JavaPrototypeProcess $exe $arguments $work $script:log
    Wait-M105 'Done (' 1000
    $text=Read-JavaPrototypeLog $log
    if($Arm -eq 'K'){
        Assert-JavaPrototype ($text.Contains('RWCORE coremod nalozen, runtimeDeobf=1')) "$Phase packaged K in SRG mode"
        $expectedDefaults='RWPRIVZETO 0 spremenjenih=0 stikal=27 '+$defaults.Replace('@@CLASSPATH@@',$classpath.Replace('\','/'))
        Assert-JavaPrototype ($text.Contains($expectedDefaults)) "$Phase all 26 switches at pinned code defaults, compiler fixture retained"
    }else{Assert-JavaPrototype (-not $text.Contains('RWCORE')) "$Phase distribution O without rework"}
}
function Stop-M105 {
    Send-M105 'save-all flush';Wait-M105 'Saved the world' 180;Send-M105 'stop'
    $until=(Get-Date).AddSeconds(180)
    while(-not $proc.HasExited -and (Get-Date)-lt $until){Guard-M105;Start-Sleep -Milliseconds 500}
    Assert-JavaPrototype $proc.HasExited 'clean stop before timeout'
    [M61ProcessLog]::Finish($proc)
    Assert-JavaPrototype ($proc.ExitCode -eq 0) 'server exit 0'
    $script:proc=$null;Guard-M105
}
function Assert-M105Stopped {
    Assert-JavaPrototype ($null -eq $proc) 'state operations only with stopped server'
    Guard-M105
}
function Set-M105Jar([string]$Jar){
    Assert-M105Stopped
    $target=[IO.Path]::GetFullPath((Join-Path $work 'mods/customnpcs.jar'))
    $scope=[IO.Path]::GetFullPath($scenario).TrimEnd('\')+'\'
    Assert-JavaPrototype ($target.StartsWith($scope,[StringComparison]::OrdinalIgnoreCase)) 'jar swap confined to disposable scenario'
    Copy-Item -LiteralPath $Jar -Destination $target -Force
    Assert-JavaPrototype ((Get-JavaPrototypeHash $target) -ceq (Get-JavaPrototypeHash $Jar)) 'installed jar byte-identical to selected input'
    Assert-JavaPrototype (@(Get-ChildItem -LiteralPath (Join-Path $work 'mods') -File).Count -eq 1) 'exactly one mod jar'
}
function Copy-M105State([string]$Source,[string]$Target){
    Assert-M105Stopped
    Assert-JavaPrototype (-not (Test-Path -LiteralPath $Target)) 'new backup/restore directory, no overwrite'
    New-Item -ItemType Directory -Path $Target | Out-Null
    foreach($name in $stateItems){Copy-Item -LiteralPath (Join-Path $Source $name) -Destination $Target -Recurse}
}
function Get-M105Manifest([string]$State){
    $manifest=@{};$base=[IO.Path]::GetFullPath($State).TrimEnd('\')+'\'
    foreach($name in $stateItems){
        $path=Join-Path $State $name
        foreach($file in @(Get-ChildItem -LiteralPath $path -Recurse -File)){
            if($file.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Reparse point in rollback state'}
            $manifest[$file.FullName.Substring($base.Length).Replace('\','/')]=Get-JavaPrototypeHash $file.FullName
        }
    }
    return $manifest
}
function Assert-M105Manifest($A,$B,[string]$Label){
    $diff=@(@($A.Keys)+@($B.Keys)|Sort-Object -Unique|Where-Object {$A[$_] -cne $B[$_]})
    Assert-JavaPrototype ($A.Count -gt 0 -and $A.Count -eq $B.Count -and $diff.Count -eq 0) "$Label exact file set and SHA256 bytes ($($A.Count) files); differences=$($diff -join ',')"
}
function Install-M105Controls {
    $clones=Join-Path $work 'world/customnpcs/clones/1'
    New-Item -ItemType Directory -Path $clones -Force | Out-Null
    foreach($phase in @('seed','candidate','candidate-restart','original','original-restart','restored')){
        $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/m105-rollback-control.js'))
        foreach($pair in @(@('@@PHASE@@',$phase),@('@@OUTPUT@@',"$prefix-$phase.json"),@('@@SEED@@',"$prefix-seed.json"),@('@@CANDIDATE@@',"$prefix-candidate.json"))){
            $code=$code.Replace($pair[0],($pair[1].Replace('\','/')|ConvertTo-Json -Compress))
        }
        $codePath=Join-Path $scenario "$phase.js";[IO.File]::WriteAllText($codePath,$code,$utf8)
        $clone=Join-Path $clones "M105_$phase.json"
        Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
        $text=[IO.File]::ReadAllText($clone)
        Assert-JavaPrototype ($text.Contains('"Name": "PERF_Kontrola"') -and $text.Contains('"NpcJob": 0,')) 'expected original control template'
        # Same original ChunkLoader job as M10.1c; no player needed to keep controls ticking.
        $text=$text.Replace('"Name": "PERF_Kontrola"',"`"Name`": `"M105_$phase`"").Replace('"NpcJob": 0,','"NpcJob": 8, "ChunkPlayerLastSeen": 4102444800000L,')
        [IO.File]::WriteAllText($clone,$text,$utf8)
    }
}
function Invoke-M105Phase([string]$Arm,[string]$Phase){
    Start-M105 $Arm $Phase
    Send-M105 "noppes clone spawn M105_$Phase 1 0,4,0"
    Wait-M105 ("M105 "+$Phase.ToUpperInvariant()+' DONE success=true') 180
    $proof=[IO.File]::ReadAllText("$prefix-$Phase.json")|ConvertFrom-Json
    Assert-JavaPrototype ($proof.success -eq $true -and $proof.phase -ceq $Phase) "$Phase proof identity/success"
    $keys=@('runtime-arm')
    if($Phase -eq 'seed'){$keys+='seed-fixture'}else{
        $keys+=@('npc-set','uuid:RB_Base','state:RB_Base','quest-dialog-readable','playerdata-readable-equal')
        if($Phase -notin @('candidate','restored')){$keys+=@('uuid:RB_Flight','state:RB_Flight','uuid:RB_Java','state:RB_Java')}
        if($Phase -eq 'restored'){$keys+='pre-candidate-restored'}else{$keys+=@('rework-nbt','java-source-preserved','java-execution')}
        if($Phase -eq 'candidate'){$keys+='candidate-fixture'}
    }
    Assert-JavaPrototype (@($proof.checks).Count -eq $keys.Count -and @($proof.checks|Where-Object {$_.ok -ne $true}).Count -eq 0) "$Phase all $($keys.Count) assertions passed"
    Assert-JavaPrototype ((@($proof.checks.key|Sort-Object)-join '|') -ceq (@($keys|Sort-Object)-join '|')) "$Phase exact assertion key set"
    # Prove queued playerdata was actually persisted before proceeding/stop (not only in memory).
    if($Phase -in @('seed','candidate')){
        $player=Join-Path $work 'world/customnpcs/playerdata/2978a942-7037-49f8-954e-32402847b105.json'
        $until=(Get-Date).AddSeconds(30);$saved=$false
        do{
            if(Test-Path -LiteralPath $player){
                $text=Read-JavaPrototypeLog $player
                $saved=$text.Contains('M105_Player') -and ($Phase -eq 'seed' -or $text.Contains('1234567890123'))
            }
            if(-not $saved){Guard-M105;Start-Sleep -Milliseconds 200}
        }while(-not $saved -and (Get-Date)-lt $until)
        Assert-JavaPrototype $saved "$Phase playerdata written to disk"
    }
    $script:proofs+=$proof
    Stop-M105
}
try{
    # Parse the controller-owned scenario before any JVM launch; no separate build is necessary.
    $tokens=$null;$errors=$null
    $null=[Management.Automation.Language.Parser]::ParseFile($PSCommandPath,[ref]$tokens,[ref]$errors)
    Assert-JavaPrototype ($errors.Count -eq 0) 'PowerShell scenario parses'
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm105-rollback'
    Guard-M105
    $jdk=Join-Path $root '.tools/jdk8';$env:JAVA_HOME=$jdk;$env:PATH="$jdk/bin;$env:PATH"
    $cp=Get-JavaPrototypeInputs $root
    $original=Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'
    $workspace=Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    $protected=@($original,$workspace,(Join-Path $root 'environment-lock.json'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected+=@(Get-ChildItem -LiteralPath (Join-Path $run 'config') -File -Recurse|Select-Object -ExpandProperty FullName)
    $options=Join-Path $run 'options.txt';$optionsExisted=Test-Path -LiteralPath $options
    if($optionsExisted){$protected+=$options}
    foreach($path in $protected){$hashes[$path]=Get-JavaPrototypeHash $path}
    Assert-JavaPrototype (Test-JavaPrototypeJar $workspace 'noppes/npcs/rework/script/NpcScript.class') 'existing packaged candidate contains Java NPC scripting'
    Assert-JavaPrototype ($hashes[$original] -ceq 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') 'pinned distribution original'
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    $props=[IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'existing flat world conditions'
    Assert-JavaPrototype (-not (Test-Path -LiteralPath $scenario)) 'fresh isolated scenario'
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination $work -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $work
    New-Item -ItemType Directory -Path (Join-Path $work 'mods') | Out-Null
    foreach($kind in @('quests','dialogs')){New-Item -ItemType Directory -Path (Join-Path $work "world/customnpcs/$kind/M105") -Force | Out-Null}
    Install-M105Controls
    $cache=Join-Path $env:USERPROFILE '.gradle/caches'
    $forge=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraftforge/forge/1.12.2-14.23.5.2847') '*-universal.jar'
    $mc=Join-Path $cache 'minecraft/net/minecraft/minecraft_server/1.12.2/minecraft_server-1.12.2.jar'
    $srg=Join-Path $cache 'minecraft/net/minecraftforge/forge/1.12.2-14.23.5.2847/snapshot/20171003/forge-1.12.2-14.23.5.2847-srgBin.jar'
    $launch=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraft/launchwrapper/1.12') '*.jar'
    foreach($path in @($forge,$mc,$srg,$launch)){$hashes[$path]=Get-JavaPrototypeHash $path}
    Assert-JavaPrototype ($hashes[$forge] -ceq '29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A') 'pinned Forge universal'
    Assert-JavaPrototype ($hashes[$mc] -ceq 'FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E') 'pinned Minecraft server'
    $dependencies=@()
    foreach($path in $cp.Split(';')){
        if(-not (Test-Path -LiteralPath $path -PathType Leaf) -or -not $path.EndsWith('.jar')){continue}
        $hashes[$path]=Get-JavaPrototypeHash $path
        if((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
            (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class')){continue}
        $dependencies+=$path
    }
    $dependencies=@($dependencies|Select-Object -Unique)
    $launchCp=(@($forge,$mc,$launch)+$dependencies|Select-Object -Unique)-join ';'
    $classpath=Join-Path $scenario 'compiler-classpath.txt'
    [IO.File]::WriteAllText($classpath,(@($workspace,$srg,$forge)+$dependencies -join ';'),$utf8)
    $exe=Join-Path $jdk 'bin/java.exe'
    $arguments=@('-Xmx2G','-Drwprivzeto=0','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')
    Set-M105Jar $original
    Start-M105 'O' 'bootstrap'
    Send-M105 'setworldspawn 0 4 0';Wait-M105 'Set the world spawn point to (0, 4, 0)' 30;Stop-M105
    Invoke-M105Phase 'O' 'seed'
    # Backup is taken only after a complete clean stop, including all world/CNPC data, config and jar.
    $before=Get-M105Manifest $work
    Copy-M105State $work $backup
    $backupManifest=Get-M105Manifest $backup
    Assert-M105Manifest $before $backupManifest 'backup before candidate'
    $backupManifest|ConvertTo-Json -Depth 5|Set-Content -LiteralPath "$prefix-backup.manifest.json" -Encoding UTF8
    $cfg=Join-Path $work 'config/CustomNpcs.cfg'
    $text=[IO.File]::ReadAllText($cfg)
    Assert-JavaPrototype ($text -cnotmatch '(?m)^Rw\w+=') 'O config has no rework overrides; K will generate code defaults'
    [IO.File]::WriteAllText($cfg,($text+"`nRwJavaClasspath="+$classpath.Replace('\','/')+"`n"),$utf8)
    Set-M105Jar $workspace
    Invoke-M105Phase 'K' 'candidate'
    Invoke-M105Phase 'K' 'candidate-restart'
    # Retain candidate state before the original gets a chance to discard unknown NPC keys.
    Copy-M105State $work $candidateBackup
    $candidateManifest=Get-M105Manifest $candidateBackup
    Assert-M105Manifest (Get-M105Manifest $work) $candidateManifest 'candidate recovery copy'
    Set-M105Jar $original
    Invoke-M105Phase 'O' 'original'
    Invoke-M105Phase 'O' 'original-restart'
    # Full rollback: restore the stopped pre-candidate installation into a new directory.
    Copy-M105State $backup $restored
    $restoredManifest=Get-M105Manifest $restored
    Assert-M105Manifest $backupManifest $restoredManifest 'restored before any launch'
    $restoredManifest|ConvertTo-Json -Depth 5|Set-Content -LiteralPath "$prefix-restored.manifest.json" -Encoding UTF8
    $work=$restored
    Invoke-M105Phase 'O' 'restored'
    Assert-M105Manifest $backupManifest (Get-M105Manifest $backup) 'immutable pre-candidate backup'
    Assert-M105Manifest $candidateManifest (Get-M105Manifest $candidateBackup) 'immutable candidate recovery copy'
    $combined=($logs|ForEach-Object {Read-JavaPrototypeLog $_})-join "`n"
    Assert-JavaPrototype ($combined -cnotmatch '\tat noppes\.|script errored|Exception ticking|Encountered an unexpected exception|NoSuchMethodError|NoSuchFieldError|AbstractMethodError|NoClassDefFoundError') 'no CNPC stack, script error log, linkage or ticking exception in all starts'
    foreach($path in $hashes.Keys){Assert-JavaPrototype ((Get-JavaPrototypeHash $path) -ceq $hashes[$path]) "protected input unchanged: $path"}
    Assert-JavaPrototype ((Test-Path -LiteralPath $options) -eq $optionsExisted) 'main options presence unchanged'
    $exitCode=0
}catch{$failure=[string]$_;Write-Host "FAIL $failure"}
finally{
    if($null -ne $proc){
        try{if(-not $proc.HasExited){Send-M105 'stop';if(-not $proc.WaitForExit(30000)){Stop-JavaPrototypeProcess $proc}};[M61ProcessLog]::Finish($proc)}
        catch{Write-Host "Cleanup failure: $_";Stop-JavaPrototypeProcess $proc;$exitCode=1}
    }
    @{success=($exitCode -eq 0);package='M10.5';error=$failure;proofs=$proofs;logs=$logs;inputs=$hashes;scenario=$scenario;
      limits=@('synthetic world, no user modpack Q1','synthetic offline CNPC playerdata, no login or GUI','original drops RwFlightMode/RwHitboxMode on save; Java source remains inert','full restore loses candidate-era progress; original timer bug B3 returns')}|
        ConvertTo-Json -Depth 12|Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    if($null -ne $lock){$lock.Dispose()}
    $env:JAVA_HOME=$oldJava;$env:PATH=$oldPath
}
Write-Host "M10.5 evidence: $prefix.json"
exit $exitCode
