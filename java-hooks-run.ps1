# Controller-only M6.2. Disposable dev or actual packaged Forge server: JS/Java hook trace, one run.
# -Package m63: same server, typed NpcScript probe (M6.3); m62 behaviour is unchanged by default.
# -Package m64: same server, per NPC/tab/generation loader, bytecode cache and atomic reload probe (M6.4).
# -Package m65: same server, error origins and B5 recovery; JS B5 against the original jar bytes (M6.5).
# -Package m66 [-ContextFixed]: nested ownership reproduction / fixed context acceptance (M6.6).
# -Package m68: existing server permissions and trusted-only capability boundary (M6.8).
# Same server setup as java-runtime-run.ps1 (M6.1, Q137-Q140); only the fixture and assertions differ.
param([ValidateSet('dev','packaged')][string]$Mode = 'dev',
      [ValidateSet('m62','m63','m64','m65','m66','m68')][string]$Package = 'm62',
      [switch]$ContextFixed)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$tag = $Package.ToUpperInvariant()
$prefix = Join-Path $root "audit/$Package-$Mode-$stamp"
# Not under dev/build: Gradle deletes stale task outputs there, and ForgeGradle declares
# runDir as a runServer output, so the copied config/EULA/world vanished before start (Q137).
$work = Join-Path $root "dev/run-$Package/hooks-$Mode-$stamp"
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
        if ($text -match "$tag FAILED|$tag DONE success=false|ScriptException|Encountered an unexpected exception") { throw 'Runtime fixture failed; see log.' }
        Update-M61Owned
        $foreign=@(Get-NavForeignJava -Roots (@([int]$proc.Id)+@($ownedJava.Keys)))
        if ($foreign.Count) { throw ('Foreign Java workload: '+($foreign -join '; ')) }
        Start-Sleep -Milliseconds 500
    }
    throw "Missing marker: $Marker"
}
try {
    $lock=Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo "$Package-$Mode"
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
    # Since M6.5 the JS container is the patched B5 class (src/patch); verify-package limits the changed
    # entries and the JS golden trace (m62) pins its behaviour. m65 still runs the original bytes.
    $originalJar=Join-Path $root $(if ($Mode -eq 'packaged') { 'CustomNPCs_1.12.2-(01Oct19).jar' } else { 'dev/libs/customnpcs-mapped-01Oct19.jar' })
    Assert-JavaPrototype (Test-JavaPrototypeJar $originalJar 'noppes/npcs/controllers/ScriptContainer.class') 'original JS container bytes available'
    $hashes[$originalJar]=Get-JavaPrototypeHash $originalJar
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
        # The dev compile classpath has no patch classes; M6.3 tabs extend NpcScript from runServer's output.
        $devCp=$cp; if ($Package -in @('m63','m64','m65','m66','m68')) { $devCp=$cp+';'+(Join-Path $root 'dev/build/classes/java/main') }
        [IO.File]::WriteAllText($classpathFile,$devCp,(New-Object Text.UTF8Encoding $false))
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
    $code=[IO.File]::ReadAllText((Join-Path $root $(switch ($Package) { 'm63' { 'dev/testworld/java-typed-control.js' } 'm64' { 'dev/testworld/java-reload-control.js' } 'm65' { 'dev/testworld/java-recovery-control.js' } 'm66' { 'dev/testworld/java-context-control.js' } 'm68' { 'dev/testworld/java-trust-control.js' } default { 'dev/testworld/java-hooks-control.js' } })))
    $code=$code.Replace('@@OUTPUT@@',($out.Replace('\','/') | ConvertTo-Json -Compress)).Replace('@@MODE@@',($Mode | ConvertTo-Json -Compress)).Replace('@@ORIGINAL@@',($originalJar.Replace('\','/') | ConvertTo-Json -Compress))
    $code=$code.Replace('@@CONTEXT_FIXED@@',([bool]$ContextFixed).ToString().ToLowerInvariant())
    $codePath=Join-Path $work 'hooks-control.js'; [IO.File]::WriteAllText($codePath,$code,(New-Object Text.UTF8Encoding $false))
    $clone=Join-Path $clones "${tag}_Control.json"
    Install-NavController (Join-Path $root 'dev/testworld/customnpcs/clones/1/PERF_Kontrola.json') $codePath $clone
    $cloneText=[IO.File]::ReadAllText($clone).Replace('"Name": "PERF_Kontrola"',"`"Name`": `"${tag}_Control`"")
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
    if ($Package -in @('m63','m64','m65','m66')) {
        $typedBase=if ($Mode -eq 'packaged') { $workspace } else { Join-Path $root 'dev/build/classes/java/main' }
        Assert-JavaPrototype ($(if ($Mode -eq 'packaged') { Test-JavaPrototypeJar $typedBase 'noppes/npcs/rework/script/NpcScript.class' } else { Test-Path -LiteralPath (Join-Path $typedBase 'noppes/npcs/rework/script/NpcScript.class') })) 'NpcScript on the compiler classpath'
    }
    Send-M61 "noppes clone spawn ${tag}_Control 1 0,4,0"
    Wait-M61 "$tag DONE success=true mode=$Mode" 180
    $text=Read-JavaPrototypeLog $log
    Assert-JavaPrototype ([regex]::Matches($text,"$tag DONE").Count -eq 1) 'probe ran exactly once'
    Assert-JavaPrototype ($text.Contains(('RWCORE coremod nalozen, runtimeDeobf=' + $(if($Mode -eq 'packaged') {'1'} else {'0'})))) 'expected coremod production/development mode'
    $proof=[IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert-JavaPrototype ($proof.success -eq $true -and $proof.mode -ceq $Mode) 'hook trace proof success'
    if ($Package -eq 'm68') {
        Assert-JavaPrototype (@($proof.checks).Count -eq 27 -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '27 existing-permission and trust-boundary assertions'
        foreach ($language in @('js','java')) {
            foreach ($suffix in @('ops-only-denies','permission-denies-upload','tool-denies','trusted-save','trusted-runs','denied-replacement-keeps-handler')) {
                $key="$language-$suffix"
                Assert-JavaPrototype (@($proof.checks | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "pinned trust contract $key"
            }
            Assert-JavaPrototype ($proof.traces.$language -ceq 'trusted') "$language authorized hook runs"
        }
        foreach ($key in @('default-grant-is-not-trust','disabled-permissions-bypass','non-op-reload-denied','op-reload-allowed','java-bounded-file-reflection-allowed','security-manager-unchanged','provider-config-restored')) {
            Assert-JavaPrototype (@($proof.checks | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "pinned trust limitation $key"
        }
        Assert-JavaPrototype ($proof.traces.deniedReloadResult -eq 0 -and $proof.traces.allowedReloadResult -eq 1 -and $proof.traces.javaAfterReload -ceq 'trusted') 'registered command manager rejects non-op and permits op reload'
        Assert-JavaPrototype ((Test-Path -LiteralPath "$out.capability.txt") -and ([IO.File]::ReadAllText("$out.capability.txt").Length -gt 0)) 'bounded Java capability file exists'
    } elseif ($Package -eq 'm66') {
        $expectedChecks=if ($ContextFixed) { 88 } else { 48 }
        Assert-JavaPrototype (@($proof.checks).Count -eq $expectedChecks -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) "$expectedChecks nested ownership assertions"
        Assert-JavaPrototype ($proof.expectedFixed -eq [bool]$ContextFixed -and @($proof.cases).Count -eq 8) 'all language pairs with return and throw'
        foreach ($case in $proof.cases) {
            Assert-JavaPrototype (@($case.observations).Count -eq 3) 'before, inner, after observations'
            foreach ($row in @($case.observations | Select-Object -First 2)) {
                Assert-JavaPrototype ($row.owner -ceq 'expected' -and $row.chest -ceq 'expected' -and $row.projectile -ceq 'expected' -and $row.projectileError -ceq '') 'binary consumers capture the executing owner'
            }
            $after=$case.observations[2]
            if ($ContextFixed) {
                Assert-JavaPrototype ($after.owner -ceq 'expected' -and $after.chest -ceq 'expected' -and $after.projectile -ceq 'expected' -and $after.projectileError -ceq '') 'previous owner restored after nested call'
            } else {
                Assert-JavaPrototype ($after.owner -ceq 'null' -and $after.chest -ceq 'null' -and $after.projectile -ceq 'empty' -and $after.projectileError.Contains('Can only be called during scripts')) 'baseline loss of chest/projectile ownership reproduced'
            }
        }
        if ($ContextFixed) {
            Assert-JavaPrototype (@($proof.extraCases).Count -eq 8) 'eight repeated/stopped nested calls with outer exception'
            foreach ($case in $proof.extraCases) {
                $expectedPhases=if ($case.id.Contains(':stopped-inner:')) { 'before,inner,after,after-second' } else { 'before,inner,after,inner,after-second' }
                Assert-JavaPrototype (($case.observations.phase -join ',') -ceq $expectedPhases) 'second nested call executes or returns from stopped-tab guard'
                foreach ($row in $case.observations) {
                    Assert-JavaPrototype ($row.owner -ceq 'expected' -and $row.chest -ceq 'expected' -and $row.projectile -ceq 'expected' -and $row.projectileError -ceq '') 'all repeated-call consumers retain their expected owner'
                }
                Assert-JavaPrototype ($case.outerConsole.Contains('m66 outer fixture')) 'outer exception reported'
            }
        }
    } elseif ($Package -eq 'm65') {
        Assert-JavaPrototype (@($proof.checks).Count -eq 17 -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '17 recovery/source map assertions'
        foreach ($key in @('current-caller-restored','current-top-level-js-cleared','current-top-level-java-cleared')) {
            Assert-JavaPrototype (@($proof.checks | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "pinned M6.6 recovery context contract: $key"
        }
        # Pinned here too, so a probe edit cannot silently weaken the expectation.
        $pinned=[ordered]@{ javaRuntime=''; javaRecovered='ok;'; javaMissing='tick;timer7;'; javaCompileFixed='p1;'
            oUnknown='tick;|;tick;interact;'; fUnknown='tick;|;interact;tick;interact;'; oErrored='boom;|;|;'; fErrored='boom;|;|;ok;' }
        foreach($key in $pinned.Keys) { Assert-JavaPrototype ($proof.traces.$key -ceq $pinned[$key]) "pinned M6.5 trace $key" }
        Assert-JavaPrototype ($proof.traces.fErroredConsole.Contains('Script source: m65/fe.js:3') -and -not $proof.traces.oErroredConsole.Contains('Script source:')) 'JS include source line only in the patched container'
    } elseif ($Package -eq 'm64') {
        Assert-JavaPrototype (@($proof.checks).Count -eq 15 -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) '15 reload/cache assertions'
        # Pinned here too, so a probe edit cannot silently weaken the expectation.
        $pinned=[ordered]@{ a='c1/1;c1/1;c2/2;c2/2;'; b='c1/1;c1/1;'; aReload='c1/1;c1/1;'; aGuiSave='c1/1;c1/1;'; c1='c1/1;v1;'; c2='c1/1;v2;'
            cBroken='c2/2;v2;'; cBrokenAgain='c3/3;v2;'; cFixed='c1/1;v3;'; d=''; aRestored='c1/1;c1/1;' }
        foreach($key in $pinned.Keys) { Assert-JavaPrototype ($proof.traces.$key -ceq $pinned[$key]) "pinned M6.4 trace $key" }
    } else {
    $hookChecks=if ($Package -eq 'm62') { 16 } else { 14 }
    Assert-JavaPrototype (@($proof.checks).Count -eq $hookChecks -and @($proof.checks | Where-Object { $_.ok -ne $true }).Count -eq 0) "$hookChecks hook trace assertions"
    if ($Package -eq 'm62') {
        foreach ($key in @('current-caller-restored','current-top-level-js-cleared','current-top-level-java-cleared')) {
            Assert-JavaPrototype (@($proof.checks | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "pinned M6.6 context contract: $key"
        }
    }
    # Golden strings are pinned here too, so a probe edit cannot silently weaken the expectation.
    $golden='1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;1:tick;2:tick;bus:tick;1:interact:false;2:interact:true;bus:interact:true;ret:true;1:timer:7;2:timer:7;bus:timer;'
    $javaTrace=if ($Package -eq 'm63') { $proof.traces.typed } else { $proof.traces.java }
    Assert-JavaPrototype ($proof.traces.js -ceq $golden -and $javaTrace -ceq $golden) 'JS and Java golden tab/bus/cancel trace'
    $reload='1:init:1;2:init;bus:init;1:tick;2:tick;bus:tick;'
    $javaReload=if ($Package -eq 'm63') { $proof.traces.typedReload } else { $proof.traces.javaReload }
    Assert-JavaPrototype ($proof.traces.jsReload -ceq $reload -and $javaReload -ceq $reload) 'JS and Java reload trace'
    if ($Package -eq 'm63') {
        Assert-JavaPrototype ($proof.traces.plain -ceq $golden) 'M6.2 plain Java class trace unchanged'
        Assert-JavaPrototype ($proof.traces.typedName -ceq 'm63:7') 'typed API call on the existing interface'
    }
    }
    Send-M61 'stop'
    Wait-M61Exit 180
    $proc=$null
    foreach($path in $hashes.Keys) { Assert-JavaPrototype ((Get-M61InputHash $path) -ceq $hashes[$path]) "unchanged protected input $path" }
    $condition=if ($Package -eq 'm68') { 'one packaged/dev Forge server; real packet entry and registered reload command; unconnected EntityPlayerMP transport and test permission provider; JS/Java deny and grant, bounded Java file/reflection capability; no GUI/login/plugin or CPU/heap sandbox claim; production permission/runtime code unchanged' } elseif ($Package -eq 'm66') { "one packaged/dev Forge server; real EventHooks with unspawned NPCs; binary chest/projectile ownership; ContextFixed=$([bool]$ContextFixed); no rendered GUI, impact events or async claim" } else { 'one functional server; real EventHooks with unspawned NPCs; JS and Java two-tab trace incl. bus order, cancel, timer, reload; Java error cases; no cost gate' }
    @{ success=$true; package=$Package; mode=$Mode; proof=$out; inputs=$hashes; compilerClasspath=[IO.File]::ReadAllText($classpathFile); fixtureHash=(Get-JavaPrototypeHash $codePath); condition=$condition } |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "$tag $Mode evidence: $prefix.json"
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
