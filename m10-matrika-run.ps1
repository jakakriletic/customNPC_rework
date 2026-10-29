# Controller-only M10.1c server close-out (D-096, Q215-Q218, Q221-Q224; harness of M10.1a/b). Server A/B matrix: the same flat world, the same JS
# control script and the same procedure run twice on a packaged Forge 1.12.2 dedicated server:
#   O = distribution CustomNPCs_1.12.2-(01Oct19).jar (never placed in dev/run/mods or dev/libs),
#   K = workspace jar with code defaults (-Drwprivzeto=1, given to both arms; O ignores it).
# Phases per arm: bootstrap (spawn point) -> run (fixtures and rows) -> real save/stop/start -> verify.
# Without a player nothing ticks after 300 ticks, and O has no /rwdiag, so the control NPCs use the
# original CNPC ChunkLoader job (NpcJob 8) in both arms. Rows: IN3 IN6 IN7 IN8 IA1 IA2 IA3 IA5 IS3 IS8
# IL5 IL6; IC2-IC4 and IO3 need a real player connection and are the client part M10.1d (Q217, Q224).
# IN3: the set of source keys that differ in the clone is compared O = K (the original re-rolls eye colours).
# IN6: derived ModelPartConfig Trans* values are masked (recomputed on every load, Q222).
# O = K: Eyes.Color is a fresh draw from the 23-colour palette per entity in both jars, so it is compared
# as "in the palette", not as a value (Q223).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m101c-matrika-$stamp"
$run = Join-Path $root 'dev/run'
$lock = $null; $proc = $null; $exitCode = 1
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
$hashes = @{}
$logs = @()
$log = "$prefix-start.log"
$failPattern = 'M10 FAILED|M10 \w+ DONE success=false|Encountered an unexpected exception|Exception ticking|NoSuchMethodError|NoSuchFieldError|AbstractMethodError|NoClassDefFoundError'
# Keys positional by definition plus keys the register explains: K-only Rw* NPC keys (RwHitboxMode,
# RwNavBackend: M3.8/M4.15 switches, 04-REGISTRI). Anything else that differs between O and K fails.
$positional = @('Pos','Motion','Rotation','UUIDMost','UUIDLeast','StartPosNew','FallDistance','OnGround','Fire','Air','PortalCooldown','Dimension')
function Get-M10InputHash([string]$Path) {
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
function Update-M10Owned {
    $snapshot=@(Get-CimInstance Win32_Process -ErrorAction Stop)
    $tree=@{ [int]$proc.Id=$true }; $grew=$true
    while ($grew) {
        $grew=$false
        foreach($p in $snapshot) { if($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId]=$true; $grew=$true } }
    }
    foreach($p in $snapshot) { if($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $ownedJava[[int]$p.ProcessId]=$true } }
}
function Wait-M10Owned([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    do {
        $alive=@($ownedJava.Keys | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
        if (-not $alive.Count) { $ownedJava.Clear(); return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw ('Own Java processes still running: '+($alive -join ', '))
}
function Wait-M10Exit([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while (-not $proc.HasExited -and (Get-Date) -lt $until) {
        Update-M10Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    Assert-JavaPrototype $proc.HasExited 'server process completed before timeout'
    [M61ProcessLog]::Finish($proc)
    Assert-JavaPrototype ($proc.ExitCode -eq 0) 'server process exit 0'
    Wait-M10Owned 120
}
function Send-M10([string]$Command) { $proc.StandardInput.WriteLine($Command); $proc.StandardInput.Flush() }
function Wait-M10([string]$Marker, [int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $until -and -not $proc.HasExited) {
        $text=Read-JavaPrototypeLog $log
        if ($text.Contains($Marker)) { return $text }
        if ($text -cmatch $failPattern) { throw 'Runtime fixture failed; see log.' }
        Update-M10Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    throw "Missing marker: $Marker"
}
function Start-M10([string]$Arm, [string]$Name) {
    $script:log="$prefix-$Arm-$Name.log"; $script:logs += $script:log
    $script:proc=Start-JavaPrototypeProcess $exe $arguments $script:work $script:log
    Wait-M10 'Done (' 1000 | Out-Null
}
function Stop-M10 {
    Send-M10 'save-all flush'
    Wait-M10 'Saved the world' 180 | Out-Null
    Send-M10 'stop'
    Wait-M10Exit 180
    $script:proc=$null
}
function Install-M10Control([string]$Clones, [string]$Phase, [string]$Out) {
    $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/m10-matrika-control.js'))
    $code=$code.Replace('@@PHASE@@',($Phase | ConvertTo-Json -Compress)).Replace('@@OUTPUT@@',($Out.Replace('\','/') | ConvertTo-Json -Compress))
    $codePath=Join-Path $script:work "m10-$Phase.js"; [IO.File]::WriteAllText($codePath,$code,(New-Object Text.UTF8Encoding $false))
    $name="M10_$Phase"
    $clone=Join-Path $Clones "$name.json"
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $text=[IO.File]::ReadAllText($clone)
    Assert-JavaPrototype ($text.Contains('"Name": "PERF_Kontrola"') -and $text.Contains('"NpcJob": 0,')) 'control clone template fields'
    # Original ChunkLoader job: forces the control chunks; ChunkPlayerLastSeen in 2100 keeps the ticket.
    $text=$text.Replace('"Name": "PERF_Kontrola"',"`"Name`": `"$name`"").Replace('"NpcJob": 0,','"NpcJob": 8, "ChunkPlayerLastSeen": 4102444800000L,')
    [IO.File]::WriteAllText($clone,$text,(New-Object Text.UTF8Encoding $false))
    return $codePath
}
function Get-M10Failed($Proof) { return @($Proof.checks | Where-Object { $_.ok -ne $true } | ForEach-Object { "$($_.key)=$($_.detail)" }) }
function Get-M10MapDiff($A, $B, [string[]]$Skip) {
    $keys=@(@($A.PSObject.Properties.Name)+@($B.PSObject.Properties.Name) | Sort-Object -Unique)
    return @($keys | Where-Object { $_ -notin $Skip -and $A.$_ -cne $B.$_ })
}
# IN6 (Q222): ModelPartConfig Trans* are derived; ModelDataShared.readFromNBT overwrites them through
# updateTransate on every load (original 01Oct19), so a fresh NPC (0) and its reloaded NBT differ there.
function Get-M10Underived($Map) {
    $copy=[pscustomobject]@{}
    foreach($p in $Map.PSObject.Properties) {
        $v=$p.Value; if ($p.Name -ceq 'NpcModelData') { $v=([string]$v) -replace '("Trans[XYZ]"): -?[0-9.]+(E-?[0-9]+)?f','$1: derived' }
        $copy | Add-Member -NotePropertyName $p.Name -NotePropertyValue $v
    }
    return $copy
}
# O = K (Q223): ModelEyeData() sets Eyes.Color with new Random().nextInt(23) from this palette (same
# constructor in 01Oct19 and the workspace); a colour outside it still counts as a difference.
$eyePalette=@(8368696,16247203,10526975,10987431,10791096,4210943,14188339,11685080,6724056,15066419,8375321,15892389,10066329,5013401,8339378,3361970,6704179,6717235,10040115,16445005,6085589,4882687,55610)
function Get-M10EyeRolled($Map) {
    $copy=[pscustomobject]@{}
    foreach($p in $Map.PSObject.Properties) {
        $v=$p.Value
        if ($p.Name -ceq 'NpcModelData') {
            $v=[regex]::Replace([string]$v,'("Eyes": \{[^}]*"Color": )(-?[0-9]+)',{ param($m) if([int]$m.Groups[2].Value -in $eyePalette){ $m.Groups[1].Value+'palette' } else { $m.Value } })
        }
        $copy | Add-Member -NotePropertyName $p.Name -NotePropertyValue $v
    }
    return $copy
}
function Invoke-M10Arm([string]$Arm, [string]$Jar) {
    $script:work=Join-Path $root "dev/run-m10/$Arm-$stamp"
    New-Item -ItemType Directory -Path $script:work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $script:work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $script:work
    $clones=Join-Path $script:work 'world/customnpcs/clones/1'
    New-Item -ItemType Directory -Path $clones -Force | Out-Null
    $mods=Join-Path $script:work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
    Copy-Item -LiteralPath $Jar -Destination $mods
    Assert-JavaPrototype (@(Get-ChildItem -LiteralPath $mods -File).Count -eq 1) "$Arm has exactly one mod jar"
    $runOut="$prefix-$Arm-run.json"; $verifyOut="$prefix-$Arm-verify.json"
    $runCode=Install-M10Control $clones 'run' $runOut
    $verifyCode=Install-M10Control $clones 'verify' $verifyOut

    # 1) Bootstrap: pin the origin spawn so the fixture area stays in the spawn chunks.
    Start-M10 $Arm 'bootstrap'
    Send-M10 'setworldspawn 0 4 0'
    Wait-M10 'Set the world spawn point to (0, 4, 0)' 30 | Out-Null
    Stop-M10

    # 2) Run: the control (ChunkLoader job) must be in the world before the 300-tick no-player limit.
    Start-M10 $Arm 'run'
    Send-M10 'noppes clone spawn M10_run 1 0,4,0'
    $text=Wait-M10 'M10 DIM uuid=' 600
    $uuid=[regex]::Match($text,'M10 DIM uuid=([0-9a-f-]{36})').Groups[1].Value
    Assert-JavaPrototype ($uuid.Length -eq 36) "$Arm IA5 NPC uuid"
    Send-M10 "forge setdimension $uuid -1 8 128 8"
    $text=Wait-M10 'M10 RUN DONE success=' 600
    $runProof=[IO.File]::ReadAllText($runOut) | ConvertFrom-Json
    Assert-JavaPrototype ($runProof.success -eq $true -and $runProof.phase -ceq 'run') ("$Arm run proof success; failed: "+((Get-M10Failed $runProof) -join '; '))
    Assert-JavaPrototype (@($runProof.checks).Count -eq 13) "$Arm 13 run assertions"
    # IL5: with no player and no forced chunk Forge unloads the nether; wait for it (data, compared O = K).
    $unloaded=$false; $until=(Get-Date).AddSeconds(90)
    while (-not $unloaded -and (Get-Date) -lt $until) { $unloaded=(Read-JavaPrototypeLog $log) -match 'Unloading dimension -1'; if(-not $unloaded) { Start-Sleep -Milliseconds 500 } }
    Stop-M10
    # IL6: after stop the clone file written in run is complete and parseable (B1 in K, plain write in O).
    $in3=Join-Path $clones 'M10_IN3.json'
    Assert-JavaPrototype ((Test-Path -LiteralPath $in3) -and @(Get-ChildItem -LiteralPath $clones -Filter '*.tmp').Count -eq 0) "$Arm IL6 clone file complete after stop"

    # 3) Verify after a real restart.
    Start-M10 $Arm 'verify'
    Send-M10 'noppes clone spawn M10_verify 1 0,4,0'
    Wait-M10 'M10 VERIFY DONE success=' 600 | Out-Null
    $verifyProof=[IO.File]::ReadAllText($verifyOut) | ConvertFrom-Json
    Assert-JavaPrototype ($verifyProof.success -eq $true -and $verifyProof.phase -ceq 'verify') ("$Arm verify proof success; failed: "+((Get-M10Failed $verifyProof) -join '; '))
    Assert-JavaPrototype (@($verifyProof.checks).Count -eq 9) "$Arm 9 verify assertions"
    Stop-M10
    $skip=@($positional)+@($runProof.volatile)
    $in6=@(Get-M10MapDiff (Get-M10Underived $runProof.aMap) (Get-M10Underived $verifyProof.aMap) $skip)
    Assert-JavaPrototype ($in6.Count -eq 0) ("$Arm IN6 NBT of M10_A equal after restart; differs: "+($in6 -join ','))
    $armLogs=($logs | Where-Object { $_ -like "*-$Arm-*" } | ForEach-Object { Read-JavaPrototypeLog $_ }) -join "`n"
    Assert-JavaPrototype (-not ($armLogs -cmatch '\tat noppes\.|Exception ticking|Encountered an unexpected exception')) "$Arm no noppes stack trace or ticking exception in any start"
    if ($Arm -eq 'K') { Assert-JavaPrototype ($armLogs.Contains('RWCORE coremod nalozen, runtimeDeobf=1') -and $armLogs.Contains('RWPRIVZETO 1 ')) 'K: rework coremod in SRG mode and code defaults restored' }
    else { Assert-JavaPrototype (-not $armLogs.Contains('RWCORE')) 'O: no rework code loaded' }
    Write-Host "OK arm $Arm volatile=[$(@($runProof.volatile) -join ',')] unloaded=$unloaded hits=$($runProof.hits | ConvertTo-Json -Compress)"
    return @{ arm=$Arm; run=$runProof; verify=$verifyProof; unloaded=$unloaded; work=$script:work; fixtures=@{ run=(Get-JavaPrototypeHash $runCode); verify=(Get-JavaPrototypeHash $verifyCode) } }
}
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm101c-matrika'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp=Get-JavaPrototypeInputs $root
    $jdk=Join-Path $root '.tools/jdk8'
    $env:JAVA_HOME=$jdk; $env:PATH="$jdk/bin;$env:PATH"
    $original=Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'
    $protected=@((Join-Path $root 'environment-lock.json'),$original,(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path]=Get-JavaPrototypeHash $path }
    Assert-JavaPrototype ($hashes[$original] -ceq 'CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1') 'pinned distribution jar (environment-lock)'
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    $props=[IO.File]::ReadAllText((Join-Path $run 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'existing flat world settings'
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
        if ((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or
            (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
            (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or
            (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class')) { continue }
        $dependencies += $path
    }
    $dependencies=@($dependencies | Select-Object -Unique)
    foreach($path in $cp.Split(';')) { if(Test-Path -LiteralPath $path -PathType Leaf) { $hashes[$path]=Get-M10InputHash $path } }
    $launchCp=(@($forge,$mc,$launch)+$dependencies | Select-Object -Unique) -join ';'
    $exe=Join-Path $jdk 'bin/java.exe'
    # Same JVM arguments in both arms (Q216); the original has no ConfigDefaults and ignores the property.
    $arguments=@('-Xmx2G','-Drwprivzeto=1','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')

    $o=Invoke-M10Arm 'O' $original
    $k=Invoke-M10Arm 'K' $workspace

    # O = K: every row verdict and the normalized NBT of the still NPC; K-only Rw* keys are registered.
    $volatile=@(@($o.run.volatile)+@($k.run.volatile) | Sort-Object -Unique)
    foreach($phase in @('run','verify')) {
        $ot=$o[$phase].trace; $kt=$k[$phase].trace
        foreach($name in @($ot.PSObject.Properties.Name)) { Assert-JavaPrototype ([string]$ot.$name -ceq [string]$kt.$name) "O = K $phase trace $name ($($ot.$name))" }
        Assert-JavaPrototype (@($ot.PSObject.Properties.Name).Count -eq @($kt.PSObject.Properties.Name).Count) "O = K $phase trace keys"
        $diff=@(Get-M10MapDiff (Get-M10EyeRolled $o[$phase].aMap) (Get-M10EyeRolled $k[$phase].aMap) (@($positional)+$volatile))
        $unexplained=@($diff | Where-Object { -not ($_ -cmatch '^Rw' -and $null -eq $o[$phase].aMap.$_) })
        Assert-JavaPrototype ($unexplained.Count -eq 0) ("O = K $phase NBT of M10_A except registered K-only Rw keys; unexplained: "+($unexplained -join ','))
        Write-Host "OK $phase K-only registered keys: $(@($diff) -join ',')"
    }
    Assert-JavaPrototype ($o.unloaded -eq $k.unloaded) "O = K IL5 nether unload observed ($($o.unloaded))"
    foreach($path in $hashes.Keys) { Assert-JavaPrototype ((Get-M10InputHash $path) -ceq $hashes[$path]) "unchanged protected input $path" }
    $condition='two arms on fresh flat worlds, one packaged Forge 14.23.5.2847 dedicated server each, no player; O = distribution 01Oct19 jar, K = workspace jar with -Drwprivzeto=1 (both arms get the property); same JS control (original CNPC API only) kept ticking by the original ChunkLoader job; bootstrap, run, save/stop/start, verify; nether via forge setdimension; no client'
    @{ success=$true; package='M10.1c'; O=$o; K=$k; volatile=$volatile; logs=$logs; inputs=$hashes; condition=$condition } |
        ConvertTo-Json -Depth 10 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M10.1c evidence: $prefix.json"
    $exitCode=0
} catch { Write-Host "FAIL $_" }
finally {
    if($null -ne $proc -and -not $proc.HasExited) {
        try { Send-M10 'stop'; if(-not $proc.WaitForExit(30000)) { Stop-JavaPrototypeProcess $proc } } catch { Stop-JavaPrototypeProcess $proc }
    }
    if($null -ne $proc -and $proc.HasExited) { Stop-JavaPrototypeProcess $proc }
    if($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode
