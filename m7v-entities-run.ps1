# Controller-only M7.V (D-095, Q193). One actual packaged Forge 1.12.2 server with the rework jar and
# customnpcentities 0.7.0 (built against CNPC 05Jul20) in mods/: the entities mod links against the
# 01Oct19-based rework, all five mobs spawn, every clip of the four animated mobs plays (sampled each
# tick: clip, age, hitbox), then a real save/stop/start keeps identity, hitbox and NBT and replays a clip.
# No production code; server setup is the java-hooks-run.ps1 packaged path (M6.1, Q137-Q140).
param([string]$EntitiesJar = (Join-Path (Split-Path $PSScriptRoot -Parent) 'customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar'))
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m7v-entities-$stamp"
$work = Join-Path $root "dev/run-m7v/entities-$stamp"
$run = Join-Path $root 'dev/run'
$lock = $null; $proc = $null; $exitCode = 1
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
$hashes = @{}
$logs = @()
$log = "$prefix-bootstrap.log"
# Linkage across CNPC versions fails as one of these; any of them in any start fails the package.
$linkage = 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError'
# ForgeGradle rewrites its empty deobfedDeps dummy jars on every Gradle start (Q140 helper, as in m62).
function Get-M7VInputHash([string]$Path) {
    if ($Path -notmatch '[\\/]deobfedDeps[\\/](compile|provided)Dummy\.jar$') { return Get-JavaPrototypeHash $Path }
    $zip=[IO.Compression.ZipFile]::OpenRead($Path); $sha=[Security.Cryptography.SHA256]::Create()
    try {
        $parts=foreach($entry in @($zip.Entries | Sort-Object FullName)) {
            $s=$entry.Open(); try { $entry.FullName+'='+[BitConverter]::ToString($sha.ComputeHash($s)) } finally { $s.Dispose() }
        }
        return 'zip-content:'+($parts -join ';')
    } finally { $sha.Dispose(); $zip.Dispose() }
}
$ownedJava=@{}
function Update-M7VOwned {
    $snapshot=@(Get-CimInstance Win32_Process -ErrorAction Stop)
    $tree=@{ [int]$proc.Id=$true }; $grew=$true
    while ($grew) {
        $grew=$false
        foreach($p in $snapshot) { if($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId]=$true; $grew=$true } }
    }
    foreach($p in $snapshot) { if($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $ownedJava[[int]$p.ProcessId]=$true } }
}
function Wait-M7VOwned([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    do {
        $alive=@($ownedJava.Keys | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
        if (-not $alive.Count) { $ownedJava.Clear(); return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw ('Own Java processes still running: '+($alive -join ', '))
}
function Wait-M7VExit([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while (-not $proc.HasExited -and (Get-Date) -lt $until) {
        Update-M7VOwned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    Assert-JavaPrototype $proc.HasExited 'server process completed before timeout'
    [M61ProcessLog]::Finish($proc)
    Assert-JavaPrototype ($proc.ExitCode -eq 0) 'server process exit 0'
    Wait-M7VOwned 120
}
function Send-M7V([string]$Command) {
    $proc.StandardInput.WriteLine($Command); $proc.StandardInput.Flush()
}
function Wait-M7V([string]$Marker, [int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $until -and -not $proc.HasExited) {
        $text=Read-JavaPrototypeLog $log
        if ($text.Contains($Marker)) { return $text }
        if ($text -match "M7V FAILED|M7V \w+ DONE success=false|Encountered an unexpected exception|Exception ticking|$linkage") { throw 'Runtime fixture failed; see log.' }
        Update-M7VOwned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    throw "Missing marker: $Marker"
}
function Start-M7V([string]$Name) {
    $script:log="$prefix-$Name.log"; $script:logs += $script:log
    $script:proc=Start-JavaPrototypeProcess $exe $arguments $work $script:log
    Wait-M7V 'Done (' 1000 | Out-Null
    Send-M7V 'list'
    Wait-M7V 'players online' 30 | Out-Null
}
function Stop-M7V {
    Send-M7V 'stop'
    Wait-M7VExit 180
    $script:proc=$null
}
function Install-M7VControl([string]$Phase, [string]$Out) {
    $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/m7v-entities-control.js'))
    $code=$code.Replace('@@PHASE@@',($Phase | ConvertTo-Json -Compress)).Replace('@@OUTPUT@@',($Out.Replace('\','/') | ConvertTo-Json -Compress))
    $codePath=Join-Path $work "m7v-$Phase.js"; [IO.File]::WriteAllText($codePath,$code,(New-Object Text.UTF8Encoding $false))
    $name="M7V_$Phase"
    $clone=Join-Path $clones "$name.json"
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $cloneText=[IO.File]::ReadAllText($clone).Replace('"Name": "PERF_Kontrola"',"`"Name`": `"$name`"")
    [IO.File]::WriteAllText($clone,$cloneText,(New-Object Text.UTF8Encoding $false))
    return $codePath
}
function Get-M7VCheck($Proof, [string]$Key) { return @($Proof.checks | Where-Object { $_.key -ceq $Key -and $_.ok -eq $true }).Count -eq 1 }
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm7v-entities'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp=Get-JavaPrototypeInputs $root
    $jdk=Join-Path $root '.tools/jdk8'
    $env:JAVA_HOME=$jdk; $env:PATH="$jdk/bin;$env:PATH"
    New-Item -ItemType Directory -Path $work | Out-Null
    $protected=@((Join-Path $root 'environment-lock.json'),(Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'),$EntitiesJar)
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path]=Get-JavaPrototypeHash $path }
    # The 0.7.0 release jar inspected by javap in M7.0 (build/libs and final_versions are byte-identical).
    Assert-JavaPrototype ($hashes[$EntitiesJar] -ceq '0DCDEC55A33BB03C2740D702C59AF8FC0039901CB9FB58D6215DAFA6BBD62149') 'pinned customnpcentities 0.7.0 release jar'
    Assert-JavaPrototype (Test-JavaPrototypeJar $EntitiesJar 'dev/jakak/customnpcentities/entity/EntityAnimatedNpc.class') 'entities mod classes present'
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $work
    $props=[IO.File]::ReadAllText((Join-Path $work 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'existing flat world settings'
    $clones=Join-Path $work 'world/customnpcs/clones/1'
    New-Item -ItemType Directory -Path $clones -Force | Out-Null
    $workspace=Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    Assert-JavaPrototype (Test-Path -LiteralPath $workspace) 'controller packaged mod exists'
    $cache=Join-Path $env:USERPROFILE '.gradle/caches'
    $forge=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraftforge/forge/1.12.2-14.23.5.2847') '*-universal.jar'
    $mc=Join-Path $cache 'minecraft/net/minecraft/minecraft_server/1.12.2/minecraft_server-1.12.2.jar'
    $launch=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraft/launchwrapper/1.12') '*.jar'
    foreach($path in @($forge,$mc,$launch,$workspace)) { $hashes[$path]=Get-JavaPrototypeHash $path }
    Assert-JavaPrototype ($hashes[$forge] -ceq '29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A') 'pinned Forge 14.23.5.2847 universal'
    Assert-JavaPrototype ($hashes[$mc] -ceq 'FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E') 'pinned Minecraft 1.12.2 server'
    $dependencies=@()
    foreach($path in $cp.Split(';')) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or -not $path.EndsWith('.jar')) { continue }
        # No mapped mod, dev Forge/MC or optional mod API on the real runtime path.
        if ((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or
            (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
            (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or
            (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class')) { continue }
        $dependencies += $path
    }
    $dependencies=@($dependencies | Select-Object -Unique)
    foreach($path in $cp.Split(';')) { if(Test-Path -LiteralPath $path -PathType Leaf) { $hashes[$path]=Get-M7VInputHash $path } }
    $mods=Join-Path $work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
    Copy-Item -LiteralPath $workspace,$EntitiesJar -Destination $mods
    $launchCp=(@($forge,$mc,$launch)+$dependencies | Select-Object -Unique) -join ';'
    $exe=Join-Path $jdk 'bin/java.exe'
    $arguments=@('-Xmx2G','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')
    $runOut="$prefix-run.json"; $verifyOut="$prefix-verify.json"
    $runCode=Install-M7VControl 'run' $runOut
    $verifyCode=Install-M7VControl 'verify' $verifyOut

    # 1) Bootstrap: pin the origin spawn so the fixture area stays in loaded spawn chunks (nav-gui-run pattern).
    Start-M7V 'bootstrap'
    Send-M7V 'setworldspawn 0 4 0'
    Wait-M7V 'Set the world spawn point to (0, 4, 0)' 30 | Out-Null
    Send-M7V 'save-all flush'
    Wait-M7V 'Saved the world' 180 | Out-Null
    Stop-M7V

    # 2) Run: the entities mod command exists, five mobs spawn, every clip plays.
    Start-M7V 'run'
    $text=Read-JavaPrototypeLog $log
    Assert-JavaPrototype ($text.Contains('RWCORE coremod nalozen, runtimeDeobf=1')) 'rework coremod in production (SRG) mode'
    Send-M7V 'cne list'
    Wait-M7V 'No CustomNPC Entities mob within 32 blocks' 30 | Out-Null
    $x=40
    foreach($mob in @('cave_bear','giant_spider','troll','warg','ice_golem')) {
        $before=([regex]::Matches((Read-JavaPrototypeLog $log),'Object successfully summoned')).Count
        Send-M7V "summon customnpcentities:$mob $x 4 0"
        $until=(Get-Date).AddSeconds(30)
        while (([regex]::Matches((Read-JavaPrototypeLog $log),'Object successfully summoned')).Count -le $before) {
            if ((Get-Date) -gt $until) { throw "summon $mob failed" }
            Start-Sleep -Milliseconds 300
        }
        Write-Host "OK summoned customnpcentities:$mob at $x 4 0"
        $x+=16
    }
    # No player joins: force the NPC chunks so entities keep ticking after 300 ticks (DiagChunkLoader, M2.1d).
    Send-M7V 'rwdiag chunks on 1'
    $text=Wait-M7V 'RWDIAG-CHUNKS stanje=on' 30
    Assert-JavaPrototype ($text -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=5') 'five entities-mod NPCs loaded and chunk-forced'
    Send-M7V 'noppes clone spawn M7V_run 1 0,4,0'
    Wait-M7V 'M7V RUN DONE success=true' 600 | Out-Null
    $runProof=[IO.File]::ReadAllText($runOut) | ConvertFrom-Json
    Assert-JavaPrototype ($runProof.success -eq $true -and $runProof.phase -ceq 'run') 'run proof success'
    $expectedClips=[ordered]@{ cave_bear=9; giant_spider=9; troll=11; warg=8 }
    Assert-JavaPrototype (@($runProof.checks).Count -eq 19 -and @($runProof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '19 run assertions'
    Assert-JavaPrototype ((Get-M7VCheck $runProof 'five-mobs') -and (Get-M7VCheck $runProof 'golem-class') -and (Get-M7VCheck $runProof 'all-lanes-finished')) 'pinned: five mobs, golem class, all lanes finished'
    $played=0
    foreach($id in $expectedClips.Keys) {
        foreach($suffix in @('hitbox-is-mob-def','idle-at-start','clips','hitbox-after')) { Assert-JavaPrototype (Get-M7VCheck $runProof "$id-$suffix") "pinned run contract $id-$suffix" }
        $lane=@($runProof.lanes | Where-Object { $_.id -ceq $id })
        Assert-JavaPrototype ($lane.Count -eq 1 -and @($lane[0].results).Count -eq $expectedClips[$id]) "$id played all $($expectedClips[$id]) clips"
        foreach($c in $lane[0].results) {
            # Pinned here too, so a probe edit cannot silently weaken the expectation.
            Assert-JavaPrototype ($c.played -eq $true -and $c.ended -eq $true -and $c.wrongClip -eq 0 -and $c.hitboxBad -eq 0 -and $c.playingSamples -gt 0 -and
                $c.maxAge -ge ($c.lengthTicks-2) -and $c.maxAge -le $c.lengthTicks -and ($c.endTick-$c.startTick) -le ($c.lengthTicks+2)) "$id/$($c.name): played $($c.maxAge)/$($c.lengthTicks) ticks, hitbox held on $($c.samples) samples"
            $played++
        }
    }
    Assert-JavaPrototype ($played -eq 37) 'all 37 clips of the four animated mobs played'
    $mobsBefore=@($runProof.mobs)
    Send-M7V 'save-all flush'
    Wait-M7V 'Saved the world' 180 | Out-Null
    Stop-M7V

    # 3) Verify after a real restart: same UUIDs and classes, hitbox from the NBT load path, saved anim NBT, replay.
    Start-M7V 'verify'
    Send-M7V 'rwdiag chunks on 1'
    $text=Wait-M7V 'RWDIAG-CHUNKS stanje=on' 30
    Assert-JavaPrototype ($text -match 'RWDIAG-CHUNKS stanje=on [^\r\n]* npc=5') 'five entities-mod NPCs reloaded (run control despawned)'
    Send-M7V 'noppes clone spawn M7V_verify 1 0,4,0'
    Wait-M7V 'M7V VERIFY DONE success=true' 300 | Out-Null
    $verifyProof=[IO.File]::ReadAllText($verifyOut) | ConvertFrom-Json
    Assert-JavaPrototype ($verifyProof.success -eq $true -and $verifyProof.phase -ceq 'verify') 'verify proof success'
    Assert-JavaPrototype (@($verifyProof.checks).Count -eq 23 -and @($verifyProof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '23 verify assertions'
    foreach($id in $expectedClips.Keys) {
        foreach($suffix in @('hitbox-is-mob-def','idle-at-start','nbt-anim-cooldowns','clips','hitbox-after')) { Assert-JavaPrototype (Get-M7VCheck $verifyProof "$id-$suffix") "pinned verify contract $id-$suffix" }
    }
    $mobsAfter=@($verifyProof.mobs)
    Assert-JavaPrototype ($mobsAfter.Count -eq 5) 'five mobs after restart'
    foreach($m in $mobsBefore) {
        $same=@($mobsAfter | Where-Object { $_.uuid -ceq $m.uuid -and $_.cls -ceq $m.cls -and $_.id -ceq $m.id })
        Assert-JavaPrototype ($same.Count -eq 1 -and [math]::Abs($same[0].width-$m.width) -lt 1e-4 -and [math]::Abs($same[0].height-$m.height) -lt 1e-4) "$($m.id) $($m.uuid) survived save/restart with hitbox $($m.width)x$($m.height)"
    }
    Stop-M7V

    $all=($logs | ForEach-Object { Read-JavaPrototypeLog $_ }) -join "`n"
    Assert-JavaPrototype (-not ($all -match $linkage)) 'no linkage errors in any start'
    # Case-sensitive: FML logs the normal INFO line "Attempting connection with missing mods [...]" on every start.
    Assert-JavaPrototype (-not ($all -cmatch 'Exception ticking|Encountered an unexpected exception|Missing Mods|MissingModsException|DuplicateModsFound')) 'no ticking/loader errors in any start'
    foreach($path in $hashes.Keys) { Assert-JavaPrototype ((Get-M7VInputHash $path) -ceq $hashes[$path]) "unchanged protected input $path" }
    $condition='one packaged Forge 14.23.5.2847 dedicated server, no player; rework workspace jar + customnpcentities 0.7.0 (05Jul20 build) in mods; DiagChunkLoader keeps NPC chunks ticking; JS control NPC samples every tick via CNPC API and entities-mod public members; bootstrap, run, real save/stop/start verify; no client, rendering or sounds'
    @{ success=$true; package='M7.V'; run=$runOut; verify=$verifyOut; logs=$logs; inputs=$hashes; fixtureHashes=@{ run=(Get-JavaPrototypeHash $runCode); verify=(Get-JavaPrototypeHash $verifyCode) }; condition=$condition } |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M7V evidence: $prefix.json"
    $exitCode=0
} catch { Write-Host "FAIL $_" }
finally {
    if($null -ne $proc -and -not $proc.HasExited) {
        try { Send-M7V 'stop'; if(-not $proc.WaitForExit(30000)) { Stop-JavaPrototypeProcess $proc } } catch { Stop-JavaPrototypeProcess $proc }
    }
    if($null -ne $proc -and $proc.HasExited) { Stop-JavaPrototypeProcess $proc }
    if($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode
