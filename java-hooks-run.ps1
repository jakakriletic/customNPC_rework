# Controller-only M6.2. Disposable dev or actual packaged Forge server: JS/Java hook trace, one run.
# Same server setup as java-runtime-run.ps1 (M6.1, Q137-Q140); only the fixture and assertions differ.
param([ValidateSet('dev','packaged')][string]$Mode = 'dev')
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m62-$Mode-$stamp"
# Not under dev/build: Gradle deletes stale task outputs there, and ForgeGradle declares
# runDir as a runServer output, so the copied config/EULA/world vanished before start (Q137).
$work = Join-Path $root "dev/run-m62/hooks-$Mode-$stamp"
$run = Join-Path $root 'dev/run'
$lock = $null; $proc = $null; $exitCode = 1
$oldJava=$env:JAVA_HOME; $oldPath=$env:PATH
$hashes = @{}
$log = "$prefix.log"
# ForgeGradle rewrites its empty deobfedDeps dummy jars on every Gradle start with a new zip
# timestamp. Compare their entry names and bytes instead; every other input stays file-SHA.
function Get-M61InputHash([string]$Path) {
    if ($Path -notmatch '[\\/]deobfedDeps[\\/](compile|provided)Dummy\.jar$') { return Get-JavaPrototypeHash $Path }
    $zip=[IO.Compression.ZipFile]::OpenRead($Path); $sha=[Security.Cryptography.SHA256]::Create()
    try {
        $parts=foreach($entry in @($zip.Entries | Sort-Object FullName)) {
            $s=$entry.Open(); try { $entry.FullName+'='+[BitConverter]::ToString($sha.ComputeHash($s)) } finally { $s.Dispose() }
        }
        return 'zip-content:'+($parts -join ';')
    } finally { $sha.Dispose(); $zip.Dispose() }
}
# org.gradle.jvmargs forces a single-use Gradle daemon under --no-daemon. It outlives the
# gradlew client for a moment as an orphan, so the next start saw it as foreign (Q140).
# Remember every Java descendant of our own start and wait for exactly those to exit.
$ownedJava=@{}
function Update-M61Owned {
    $snapshot=@(Get-CimInstance Win32_Process -ErrorAction Stop)
    $tree=@{ [int]$proc.Id=$true }; $grew=$true
    while ($grew) {
        $grew=$false
        foreach($p in $snapshot) { if($tree.ContainsKey([int]$p.ParentProcessId) -and -not $tree.ContainsKey([int]$p.ProcessId)) { $tree[[int]$p.ProcessId]=$true; $grew=$true } }
    }
    foreach($p in $snapshot) { if($p.Name -in @('java.exe','javaw.exe') -and $tree.ContainsKey([int]$p.ProcessId)) { $ownedJava[[int]$p.ProcessId]=$true } }
}
function Wait-M61Owned([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    do {
        $alive=@($ownedJava.Keys | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
        if (-not $alive.Count) { $ownedJava.Clear(); return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    throw ('Own Java processes still running: '+($alive -join ', '))
}
function Wait-M61Exit([int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while (-not $proc.HasExited -and (Get-Date) -lt $until) {
        Update-M61Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    Assert-JavaPrototype $proc.HasExited 'prototype process completed before timeout'
    [M61ProcessLog]::Finish($proc)
    Assert-JavaPrototype ($proc.ExitCode -eq 0) 'prototype process exit 0'
    Wait-M61Owned 120
}
function Send-M61([string]$Command) {
    $proc.StandardInput.WriteLine($Command); $proc.StandardInput.Flush()
}
function Wait-M61([string]$Marker, [int]$Seconds) {
    $until=(Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $until -and -not $proc.HasExited) {
        $text=Read-JavaPrototypeLog $log
        if ($text.Contains($Marker)) { return }
        # The probe provokes Java tab errors on purpose, so 'script errored' is expected here.
        if ($text -match 'M62 FAILED|M62 DONE success=false|ScriptException|Encountered an unexpected exception') { throw 'Runtime fixture failed; see log.' }
        Update-M61Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    throw "Missing marker: $Marker"
}
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "m62-$Mode"
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp=Get-JavaPrototypeInputs $root
    $jdk=Join-Path $root '.tools/jdk8'
    $env:JAVA_HOME=$jdk; $env:PATH="$jdk/bin;$env:PATH"
    New-Item -ItemType Directory -Path $work | Out-Null
    $protected=@((Join-Path $root 'environment-lock.json'),(Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'),(Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt'))
    $protected += @(Get-ChildItem -LiteralPath (Join-Path $run 'config') -Recurse -File | Select-Object -ExpandProperty FullName)
    foreach ($path in $protected) { $hashes[$path]=Get-JavaPrototypeHash $path }
    Assert-JavaPrototype ([IO.File]::ReadAllText((Join-Path $run 'eula.txt')) -match '(?m)^eula=true') 'existing real EULA acceptance'
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath (Join-Path $run 'server.properties'),(Join-Path $run 'eula.txt') -Destination $work
    $props=[IO.File]::ReadAllText((Join-Path $work 'server.properties'))
    Assert-JavaPrototype ($props -match '(?m)^level-name=world\s*$' -and $props -match '(?m)^level-type=FLAT\s*$') 'existing flat world settings'
    $clones=Join-Path $work 'world/customnpcs/clones/1'
    New-Item -ItemType Directory -Path $clones -Force | Out-Null
    $classpathFile=Join-Path $work 'compiler-classpath.txt'
    $out="$prefix-proof.json"
    $dependencies=@()
    $workspace=Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    Assert-JavaPrototype (Test-Path -LiteralPath $workspace) 'controller packaged mod exists'
    # The JS container stays byte-for-byte original; DataScript is the patched M6.2 edge.
    $original=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar'))
    $candidate=[IO.Compression.ZipFile]::OpenRead($workspace)
    try {
        foreach($entry in @('noppes/npcs/controllers/ScriptContainer.class')) {
            $sha=[Security.Cryptography.SHA256]::Create()
            $a=$original.GetEntry($entry).Open(); $b=$candidate.GetEntry($entry).Open()
            try { Assert-JavaPrototype ([BitConverter]::ToString($sha.ComputeHash($a)) -ceq [BitConverter]::ToString($sha.ComputeHash($b))) "unchanged JS container $entry" }
            finally { $a.Dispose(); $b.Dispose(); $sha.Dispose() }
        }
    } finally { $original.Dispose(); $candidate.Dispose() }
    if ($Mode -eq 'packaged') {
        $cache=Join-Path $env:USERPROFILE '.gradle/caches'
        $forge=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraftforge/forge/1.12.2-14.23.5.2847') '*-universal.jar'
        $mc=Join-Path $cache 'minecraft/net/minecraft/minecraft_server/1.12.2/minecraft_server-1.12.2.jar'
        $srg=Join-Path $cache 'minecraft/net/minecraftforge/forge/1.12.2-14.23.5.2847/snapshot/20171003/forge-1.12.2-14.23.5.2847-srgBin.jar'
        $launch=Get-JavaPrototypeSingle (Join-Path $cache 'modules-2/files-2.1/net.minecraft/launchwrapper/1.12') '*.jar'
        foreach($path in @($forge,$mc,$srg,$launch,$workspace)) { $hashes[$path]=Get-JavaPrototypeHash $path }
        Assert-JavaPrototype ($hashes[$forge] -ceq '29A7372B5801C2EA01ACFFA8B238256D131D770BCD18148D6F2D5C2A40BC6A6A') 'pinned Forge 14.23.5.2847 universal'
        Assert-JavaPrototype ($hashes[$mc] -ceq 'FE1F9274E6DAD9191BF6E6E8E36EE6EBC737F373603DF0946AAFCDED0D53167E') 'pinned Minecraft 1.12.2 server'
        Assert-JavaPrototype (Test-JavaPrototypeJar $srg 'net/minecraft/entity/EntityCreature.class') 'named SRG compiler dependency'
        foreach($path in $cp.Split(';')) {
            if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or -not $path.EndsWith('.jar')) { continue }
            # No mapped mod, dev Forge/MC, source jars or optional mod API on the real runtime path.
            if ((Test-JavaPrototypeJar $path 'noppes/npcs/CustomNpcs.class') -or
                (Test-JavaPrototypeJar $path 'net/minecraft/server/MinecraftServer.class') -or
                (Test-JavaPrototypeJar $path 'net/minecraftforge/fml/common/Loader.class') -or
                (Test-JavaPrototypeJar $path 'si/ladja/npcbaritone/api/NpcBaritone.class')) { continue }
            $dependencies += $path
        }
        $dependencies=@($dependencies | Select-Object -Unique)
        # srgBin has no FML eventhandler classes (Event.setCanceled); the runtime takes them from the universal jar.
        Assert-JavaPrototype (Test-JavaPrototypeJar $forge 'net/minecraftforge/fml/common/eventhandler/Event.class') 'Forge Event in universal jar'
        [IO.File]::WriteAllText($classpathFile,(@($workspace,$srg,$forge)+$dependencies -join ';'),(New-Object Text.UTF8Encoding $false))
        $mods=Join-Path $work 'mods'; New-Item -ItemType Directory -Path $mods | Out-Null
        Copy-Item -LiteralPath $workspace -Destination $mods
        $launchCp=(@($forge,$mc,$launch)+$dependencies | Select-Object -Unique) -join ';'
        $exe=Join-Path $jdk 'bin/java.exe'
        $arguments=@('-Xmx2G','-cp',$launchCp,'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLServerTweaker','nogui')
        $working=$work
    } else {
        [IO.File]::WriteAllText($classpathFile,$cp,(New-Object Text.UTF8Encoding $false))
        $exe=Join-Path $root 'dev/gradlew.bat'
        # gradlew.bat requires cmd; Start-JavaPrototypeProcess already provides it.
        $arguments=@('runServer','--offline','--no-daemon','--console=plain',"-PrwRunDir=$work")
        $working=Join-Path $root 'dev'
    }
    foreach($path in $cp.Split(';')) { if(Test-Path -LiteralPath $path -PathType Leaf) { $hashes[$path]=Get-M61InputHash $path } }
    # Java tabs read the compiler classpath through the real config key (RwJavaClasspath).
    $cfg=Join-Path $work 'config/CustomNpcs.cfg'
    Assert-JavaPrototype (-not ([IO.File]::ReadAllText($cfg) -match '(?m)^RwJavaClasspath=')) 'config copy has no RwJavaClasspath yet'
    [IO.File]::AppendAllText($cfg,("`nRwJavaClasspath="+$classpathFile.Replace('\','/')+"`n"),(New-Object Text.UTF8Encoding $false))
    $code=[IO.File]::ReadAllText((Join-Path $root 'dev/testworld/java-hooks-control.js'))
    $code=$code.Replace('@@OUTPUT@@',($out.Replace('\','/') | ConvertTo-Json -Compress)).Replace('@@MODE@@',($Mode | ConvertTo-Json -Compress))
    $codePath=Join-Path $work 'hooks-control.js'; [IO.File]::WriteAllText($codePath,$code,(New-Object Text.UTF8Encoding $false))
    $clone=Join-Path $clones 'M62_Control.json'
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $cloneText=[IO.File]::ReadAllText($clone).Replace('"Name": "PERF_Kontrola"','"Name": "M62_Control"')
    [IO.File]::WriteAllText($clone,$cloneText,(New-Object Text.UTF8Encoding $false))
    # A fresh flat world spawns far from 0,0, so the control NPC would sit in a non-ticking
    # chunk. Pin and save the origin spawn in a bootstrap start first (nav-gui-run pattern).
    $log="$prefix-bootstrap.log"
    $proc=Start-JavaPrototypeProcess $exe $arguments $working $log
    Wait-M61 'Done (' 1000
    Send-M61 'list'
    Wait-M61 'players online' 30
    Send-M61 'setworldspawn 0 4 0'
    Wait-M61 'Set the world spawn point to (0, 4, 0)' 30
    Send-M61 'save-all flush'
    Wait-M61 'Saved the world' 180
    Send-M61 'stop'
    Wait-M61Exit 180
    $proc=$null
    $log="$prefix.log"
    $proc=Start-JavaPrototypeProcess $exe $arguments $working $log
    Wait-M61 'Done (' 1000
    Send-M61 'list'
    Wait-M61 'players online' 30
    Send-M61 'noppes clone spawn M62_Control 1 0,4,0'
    Wait-M61 "M62 DONE success=true mode=$Mode" 180
    $text=Read-JavaPrototypeLog $log
    Assert-JavaPrototype ([regex]::Matches($text,'M62 DONE').Count -eq 1) 'probe ran exactly once'
    Assert-JavaPrototype ($text.Contains(('RWCORE coremod nalozen, runtimeDeobf=' + $(if($Mode -eq 'packaged') {'1'} else {'0'})))) 'expected coremod production/development mode'
    $proof=[IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert-JavaPrototype ($proof.success -eq $true -and $proof.mode -ceq $Mode) 'hook trace proof success'
    Assert-JavaPrototype (@($proof.checks).Count -eq 14 -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '14 hook trace assertions'
    # Golden strings are pinned here too, so a probe edit cannot silently weaken the expectation.
    $golden='1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;1:tick;2:tick;bus:tick;1:interact:false;2:interact:true;bus:interact:true;ret:true;1:timer:7;2:timer:7;bus:timer;'
    Assert-JavaPrototype ($proof.traces.js -ceq $golden -and $proof.traces.java -ceq $golden) 'JS and Java golden tab/bus/cancel trace'
    $reload='1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;'
    Assert-JavaPrototype ($proof.traces.jsReload -ceq $reload -and $proof.traces.javaReload -ceq $reload) 'JS and Java reload trace'
    Send-M61 'stop'
    Wait-M61Exit 180
    $proc=$null
    foreach($path in $hashes.Keys) { Assert-JavaPrototype ((Get-M61InputHash $path) -ceq $hashes[$path]) "unchanged protected input $path" }
    @{ success=$true; mode=$Mode; proof=$out; inputs=$hashes; compilerClasspath=[IO.File]::ReadAllText($classpathFile); fixtureHash=(Get-JavaPrototypeHash $codePath); condition='one functional server; real EventHooks with unspawned NPCs; JS and Java two-tab trace incl. bus order, cancel, timer, reload; Java error cases; no cost gate' } |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M62 $Mode evidence: $prefix.json"
    $exitCode=0
} catch { Write-Host "FAIL $_" }
finally {
    if($null -ne $proc -and -not $proc.HasExited) {
        try { Send-M61 'stop'; if(-not $proc.WaitForExit(30000)) { Stop-JavaPrototypeProcess $proc } } catch { Stop-JavaPrototypeProcess $proc }
    }
    if($null -ne $proc -and $proc.HasExited) { Stop-JavaPrototypeProcess $proc }
    if($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode
