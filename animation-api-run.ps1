# Controller-only M7.9 acceptance. Packaged server setup follows m7v/java-hooks; no parallel run.
# Entities=0 proves real absence (no optional mod on runtime OR compiler classpath).
param([ValidateSet(0,1)][int]$Entities=1,
      [string]$EntitiesJar=(Join-Path (Split-Path $PSScriptRoot -Parent) 'customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar'))
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp=Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix=Join-Path $root "audit/m79-e$Entities-$stamp"
$work=Join-Path $root "dev/run-m79/e$Entities-$stamp"
$run=Join-Path $root 'dev/run'
$lock=$null; $proc=$null; $exitCode=1
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
$hashes=@{}; $logs=@(); $log="$prefix-bootstrap.log"; $ownedJava=@{}
$linkage='NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError'
function Get-M79Hash([string]$Path) {
    if($Path -notmatch '[\\/]deobfedDeps[\\/](compile|provided)Dummy\.jar$') { return Get-JavaPrototypeHash $Path }
    $zip=[IO.Compression.ZipFile]::OpenRead($Path); $sha=[Security.Cryptography.SHA256]::Create()
    try {
        $parts=foreach($entry in @($zip.Entries | Sort-Object FullName)) {
            $s=$entry.Open(); try { $entry.FullName+'='+[BitConverter]::ToString($sha.ComputeHash($s)) } finally { $s.Dispose() }
        }
        return 'zip-content:'+($parts -join ';')
    } finally { $sha.Dispose(); $zip.Dispose() }
}
function Update-M79Owned {
    $snapshot=@(Get-CimInstance Win32_Process -ErrorAction Stop)
    $tree=@{ [int]$proc.Id=$true }; $grew=$true
    while($grew) {
        $grew=$false
        foreach($p in $snapshot) {
            if($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) {
                $tree[[int]$p.ProcessId]=$true; $grew=$true
            }
        }
    }
    foreach($p in $snapshot) { if($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $ownedJava[[int]$p.ProcessId]=$true } }
}
function Send-M79([string]$Command) { $proc.StandardInput.WriteLine($Command); $proc.StandardInput.Flush() }
function Wait-M79([string]$Marker,[int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while((Get-Date) -lt $until -and -not $proc.HasExited) {
        $text=Read-JavaPrototypeLog $log
        if($text.Contains($Marker)) { return $text }
        if($text -match "M79 FAILED|M79 DONE success=false|Encountered an unexpected exception|Exception ticking|$linkage") { throw 'Runtime fixture failed; see log.' }
        Update-M79Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    throw "Missing marker: $Marker"
}
function Start-M79([string]$Name) {
    $script:log="$prefix-$Name.log"; $script:logs+=$script:log
    $script:proc=Start-JavaPrototypeProcess $exe $arguments $work $script:log
    Wait-M79 'Done (' 1000 | Out-Null
}
function Stop-M79 {
    Send-M79 'stop'
    $until=(Get-Date).AddSeconds(180)
    while(-not $proc.HasExited -and (Get-Date) -lt $until) { Update-M79Owned; Start-Sleep -Milliseconds 500 }
    Assert-JavaPrototype $proc.HasExited 'own server stopped'
    [M61ProcessLog]::Finish($proc)
    Assert-JavaPrototype ($proc.ExitCode -eq 0) 'server exit 0'
    $until=(Get-Date).AddSeconds(120)
    do {
        $alive=@($ownedJava.Keys | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
        if(-not $alive.Count) { break }
        Start-Sleep -Milliseconds 500
    } while((Get-Date) -lt $until)
    Assert-JavaPrototype ($alive.Count -eq 0) 'own Java processes exited'
    $ownedJava.Clear(); $script:proc=$null
}
function Assert-M79Key($Proof,[string]$Key) {
    Assert-JavaPrototype (@($Proof.checks | Where-Object { $_.key -ceq $Key -and $_.ok -eq $true }).Count -eq 1) "pinned contract $Key"
}
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "m79-e$Entities"
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp=Get-JavaPrototypeInputs $root
    $jdk=Join-Path $root '.tools/jdk8'; $env:JAVA_HOME=$jdk; $env:PATH="$jdk/bin;$env:PATH"
    New-Item -ItemType Directory -Path $work | Out-Null
    $protected=@((Join-Path $root 'environment-lock.json'),(Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected+=@(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    if($Entities -eq 1) { $protected+=$EntitiesJar }
    foreach($path in $protected) { $hashes[$path]=Get-JavaPrototypeHash $path }
    if($Entities -eq 1) {
        Assert-JavaPrototype ($hashes[$EntitiesJar] -ceq '0DCDEC55A33BB03C2740D702C59AF8FC0039901CB9FB58D6215DAFA6BBD62149') 'pinned entities 0.7.0 jar'
    }
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $work
    $props=[IO.File]::ReadAllText((Join-Path $work 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'existing flat world settings'
    $clones=Join-Path $work 'world/customnpcs/clones/1'; New-Item -ItemType Directory -Path $clones -Force | Out-Null
    $workspace=Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    Assert-JavaPrototype (Test-JavaPrototypeJar $workspace 'noppes/npcs/rework/anim/Animators.class') 'controller packaged animation API exists'
    $cache=Join-Path $env:USERPROFILE '.gradle/caches'
    $forge=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraftforge/forge/1.12.2-14.23.5.2847') '*-universal.jar'
    $mc=Join-Path $cache 'minecraft/net/minecraft/minecraft_server/1.12.2/minecraft_server-1.12.2.jar'
    $srg=Join-Path $cache 'minecraft/net/minecraftforge/forge/1.12.2-14.23.5.2847/snapshot/20171003/forge-1.12.2-14.23.5.2847-srgBin.jar'
    $launch=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraft/launchwrapper/1.12') '*.jar'
    foreach($path in @($forge,$mc,$srg,$launch,$workspace)) { $hashes[$path]=Get-JavaPrototypeHash $path }
    Assert-JavaPrototype ($hashes[$forge] -ceq '29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A') 'pinned Forge 14.23.5.2847'
    Assert-JavaPrototype ($hashes[$mc] -ceq 'FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E') 'pinned Minecraft 1.12.2 server'
    $dependencies=@()
    foreach($path in $cp.Split(';')) {
        if(-not (Test-Path -LiteralPath $path -PathType Leaf) -or -not $path.EndsWith('.jar')) { continue }
        if((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or
           (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
           (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or
           (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class') -or
           (Test-JavaPrototypeJar $path 'dev/jakak/customnpcentities/entity/EntityAnimatedNpc.class')) { continue }
        $dependencies+=$path
    }
    $dependencies=@($dependencies | Select-Object -Unique)
    foreach($path in $cp.Split(';')) { if(Test-Path -LiteralPath $path -PathType Leaf) { $hashes[$path]=Get-M79Hash $path } }
    # Java compiles against the same packaged API and named SRG MC types as M6; entities jar is NOT required.
    $classpathFile=Join-Path $work 'compiler-classpath.txt'
    [IO.File]::WriteAllText($classpathFile,(@($workspace,$srg,$forge)+$dependencies -join ';'),(New-Object Text.UTF8Encoding $false))
    $cfg=Join-Path $work 'config/CustomNpcs.cfg'
    Assert-JavaPrototype (-not ([IO.File]::ReadAllText($cfg) -match '(?m)^RwJavaClasspath=')) 'config copy has no Java classpath'
    [IO.File]::AppendAllText($cfg,("`nRwJavaClasspath="+$classpathFile.Replace('\','/')+"`n"),(New-Object Text.UTF8Encoding $false))
    $mods=Join-Path $work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
    Copy-Item -LiteralPath $workspace -Destination $mods
    if($Entities -eq 1) { Copy-Item -LiteralPath $EntitiesJar -Destination $mods }
    $launchCp=(@($forge,$mc,$launch)+$dependencies | Select-Object -Unique) -join ';'
    $exe=Join-Path $jdk 'bin/java.exe'
    $arguments=@('-Xmx2G','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')
    $out="$prefix-proof.json"
    $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/m79-api-control.js')).Replace('@@ENTITIES@@',($Entities -eq 1).ToString().ToLowerInvariant()).Replace('@@OUTPUT@@',($out.Replace('\','/') | ConvertTo-Json -Compress))
    $codePath=Join-Path $work 'm79-control.js'; [IO.File]::WriteAllText($codePath,$code,(New-Object Text.UTF8Encoding $false))
    $clone=Join-Path $clones 'M79_Control.json'
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $cloneText=[IO.File]::ReadAllText($clone).Replace('"Name": "PERF_Kontrola"','"Name": "M79_Control"')
    [IO.File]::WriteAllText($clone,$cloneText,(New-Object Text.UTF8Encoding $false))
    Start-M79 'bootstrap'
    Send-M79 'setworldspawn 0 4 0'; Wait-M79 'Set the world spawn point to (0, 4, 0)' 30 | Out-Null
    Send-M79 'save-all flush'; Wait-M79 'Saved the world' 180 | Out-Null
    Stop-M79
    Start-M79 'run'
    Assert-JavaPrototype ((Read-JavaPrototypeLog $log).Contains('RWCORE coremod nalozen, runtimeDeobf=1')) 'SRG production coremod'
    if($Entities -eq 1) {
        $x=32
        foreach($mob in @('cave_bear','cave_bear','ice_golem')) {
            $before=([regex]::Matches((Read-JavaPrototypeLog $log),'Object successfully summoned')).Count
            Send-M79 "summon customnpcentities:$mob $x 4 0"
            $until=(Get-Date).AddSeconds(30)
            while(([regex]::Matches((Read-JavaPrototypeLog $log),'Object successfully summoned')).Count -le $before) {
                if((Get-Date) -gt $until) { throw "summon $mob failed" }; Start-Sleep -Milliseconds 300
            }
            $x+=32
        }
        Send-M79 'rwdiag chunks on 1'
        $text=Wait-M79 'RWDIAG-CHUNKS stanje=on' 30
        Assert-JavaPrototype ($text -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=3') 'three forced mobs'
    }
    Send-M79 'noppes clone spawn M79_Control 1 0,4,0'
    Wait-M79 "M79 DONE success=true entities=$(($Entities -eq 1).ToString().ToLowerInvariant())" 240 | Out-Null
    $proof=[IO.File]::ReadAllText($out) | ConvertFrom-Json
    $count=if($Entities -eq 1) {28} else {10}
    Assert-JavaPrototype ($proof.success -eq $true -and $proof.entities -eq ($Entities -eq 1) -and @($proof.checks).Count -eq $count -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) "$count successful probe checks"
    foreach($key in @('initially-no-queue-listener','ordinary-and-null-inert','ordinary-nbt-unchanged','availability-0','availability-1','js-golden','java-equals-js','tabs-healthy')) { Assert-M79Key $proof $key }
    if($Entities -eq 1) {
        foreach($key in @('two-bears-and-inert-golem','known-sorted-clips-0','known-sorted-clips-1','defensive-clips-0','defensive-clips-1','shared-handle-0','shared-handle-1','listener-only-for-pending','off-thread-mutations-rejected','fifo-js','fifo-java','raw-action-samples','hitbox-held','script-stop-golden','listener-detaches-after-stop','successful-play-clears-pending','no-resurrection-after-play','death-and-removal-detach-listener','dead-target-inert','tabs-still-healthy')) { Assert-M79Key $proof $key }
        Assert-JavaPrototype (($proof.sequences[0] -join ',') -ceq 'hurt,idle,hurt' -and ($proof.sequences[1] -join ',') -ceq 'hurt,idle,hurt') 'JS and Java FIFO hurt,idle,hurt'
        Assert-JavaPrototype ($proof.jsTrace -ceq 'true:true:true::0' -and $proof.javaTrace -ceq $proof.jsTrace) 'JS/Java stop golden'
        Assert-JavaPrototype (@($proof.samples[0]).Count -gt 10 -and @($proof.samples[1]).Count -gt 10 -and $proof.readTripletMicros -gt 0) 'per-tick samples and one read cost datum'
    } else {
        Assert-M79Key $proof 'optional-mod-actually-absent'; Assert-M79Key $proof 'no-listener-without-mod'
        Assert-JavaPrototype ($proof.jsTrace -ceq 'false:false:false:false:false:false' -and $proof.javaTrace -ceq $proof.jsTrace) 'absent JS/Java golden'
    }
    Stop-M79
    $all=($logs | ForEach-Object { Read-JavaPrototypeLog $_ }) -join "`n"
    Assert-JavaPrototype (-not ($all -match "$linkage|RWANIM incompatible|RWANIM queue cancelled")) 'no linkage or bridge errors'
    Assert-JavaPrototype (-not ($all -cmatch 'Exception ticking|Encountered an unexpected exception|Missing Mods|MissingModsException|DuplicateModsFound')) 'no ticking/loader errors'
    foreach($path in $hashes.Keys) { Assert-JavaPrototype ((Get-M79Hash $path) -ceq $hashes[$path]) "unchanged protected input $path" }
    @{success=$true;package='M7.9';entities=$Entities;proof=$out;logs=$logs;inputs=$hashes;fixtureHash=(Get-JavaPrototypeHash $codePath);
      condition='one packaged dedicated Forge server; JS and typed Java real timer dispatch; entities=1 live bears with per-tick raw-action/hitbox sampling, queue cancellation and lifecycle, one read cost batch; entities=0 no optional mod on runtime or compiler classpath; no client/rendering'} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M79 evidence: $prefix.json"
    $exitCode=0
} catch { Write-Host "FAIL $_" }
finally {
    if($null -ne $proc -and -not $proc.HasExited) {
        try { Send-M79 'stop'; if(-not $proc.WaitForExit(30000)) { Stop-JavaPrototypeProcess $proc } } catch { Stop-JavaPrototypeProcess $proc }
    }
    if($null -ne $proc -and $proc.HasExited) { Stop-JavaPrototypeProcess $proc }
    if($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode
