<# Controller-only M14.3b8: the packaged (SRG) CNPC jar in a real Forge 1.12.2-14.23.5.2860
   client from the owner's CurseForge install, with the owner's real OptiFine 1.12.2 HD U G5 jar
   (pinned SHA256), without a shader pack (-Shader none) or with BSL v7.1.04 (-Shader bsl).
   NpcBatchCompatHarness optifine=1 proves: OptiFine and the shader state are really loaded,
   RwNpcInstancing=1 never builds a batch program, A/B/A2 FBO images and GL state are bitwise
   original, and the living world at switch 1 draws only the original. Owner files are only
   read and re-hashed at the end. One isolated run directory, no cost window. #>
param([ValidateSet('none', 'bsl')][string]$Shader = 'none',
    [string]$MinecraftInstall = (Join-Path $env:USERPROFILE 'curseforge/minecraft/Install'))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = $PSScriptRoot
$dev = Join-Path $root 'dev'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$instances = Join-Path (Split-Path $MinecraftInstall -Parent) 'Instances'
$optifineJar = Join-Path $instances 'RLCraft Dregora/mods/OptiFine_1.12.2_HD_U_G5.jar'
$optifineHash = '3B0006797771FEB97F2D0D2908AE7C9A78CEFB2E730AF1C2E85D08A839BA271E'
$packName = 'BSL_v7.1.04.zip'
$pack = Join-Path $instances "Giga Advantura/shaderpacks/$packName"
$packHash = '03C6E5CC83A7843124F9C7C1FC8D3083B1DF17DB6B0C1AAF22581F45E0719B83'
$workspace = Join-Path $dev 'build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
$forgeId = 'forge-14.23.5.2860'
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m143b-optifine-$Shader-$stamp"
$work = Join-Path $dev "run-m143b-optifine/$Shader-$stamp"
$lock = $null
$client = $null
$hashes = @{}
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Unchanged {
    foreach ($path in $hashes.Keys) {
        Assert ((Get-JavaPrototypeHash $path) -ceq $hashes[$path]) "unchanged input $path"
    }
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm143b-optifine'
    if (@(Get-NavForeignJava).Count) { throw 'A Minecraft or Gradle workload is already running.' }
    Assert ((Get-JavaPrototypeHash $optifineJar) -ceq $optifineHash) 'pinned OptiFine 1.12.2 HD U G5 jar'
    $hashes[$optifineJar] = $optifineHash
    if ($Shader -eq 'bsl') {
        Assert ((Get-JavaPrototypeHash $pack) -ceq $packHash) "pinned shader pack $packName"
        $hashes[$pack] = $packHash
    }
    # The controller's build produced the packaged jar from the current sources.
    $jarTime = (Get-Item -LiteralPath $workspace).LastWriteTimeUtc
    $newest = @(Get-ChildItem -LiteralPath (Join-Path $dev 'src/patch') -Recurse -File | Sort-Object LastWriteTimeUtc -Descending)[0]
    Assert ($jarTime -gt $newest.LastWriteTimeUtc) "packaged jar newer than sources ($($newest.Name))"
    Assert (Test-JavaPrototypeJar $workspace 'noppes/npcs/rework/diag/NpcBatchCompatHarness.class') 'packaged jar contains the harness'
    $hashes[$workspace] = Get-JavaPrototypeHash $workspace
    $hashes[$PSCommandPath] = Get-JavaPrototypeHash $PSCommandPath

    Assert (-not (Test-Path -LiteralPath $work)) 'fresh isolated run directory'
    New-Item -ItemType Directory -Path (Join-Path $work 'mods') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $root 'audit') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $dev 'run/config') -Destination (Join-Path $work 'config') -Recurse
    Copy-Item -LiteralPath $workspace -Destination (Join-Path $work 'mods')
    Copy-Item -LiteralPath $optifineJar -Destination (Join-Path $work 'mods')
    $mods = @(Get-ChildItem -LiteralPath (Join-Path $work 'mods') -Recurse -File)
    Assert ($mods.Count -eq 2) 'only the packaged CNPC jar and OptiFine in isolated mods'
    $utf8 = New-Object Text.UTF8Encoding $false
    # No rendering setting: no snooper data, no OptiFine cape download for the test player.
    [IO.File]::WriteAllLines((Join-Path $work 'options.txt'), @('snooperEnabled:false'), $utf8)
    [IO.File]::WriteAllLines((Join-Path $work 'optionsof.txt'), @('ofShowCapes:false'), $utf8)
    if ($Shader -eq 'bsl') {
        New-Item -ItemType Directory -Path (Join-Path $work 'shaderpacks') | Out-Null
        Copy-Item -LiteralPath $pack -Destination (Join-Path $work 'shaderpacks')
        [IO.File]::WriteAllLines((Join-Path $work 'optionsshaders.txt'), @("shaderPack=$packName"), $utf8)
    } else {
        [IO.File]::WriteAllLines((Join-Path $work 'optionsshaders.txt'), @('shaderPack=OFF'), $utf8)
    }

    $out = "$prefix.proof.json"
    $job = "$prefix.job"
    $log = "$prefix-client.log"
    [IO.File]::WriteAllLines($job, @('npcBatchCompat=1', 'optifine=1', 'world=m143b-optifine',
        ('shaderPack=' + [int]($Shader -eq 'bsl')), 'shaderName=BSL_v7.1.04', 'optifineVersion=OptiFine_1.12.2_HD_U_G5',
        ('out=' + $out.Replace('\', '/'))), $utf8)
    @{package = 'M14.3b8'; shader = $Shader; inputs = $hashes; proof = $out; log = $log} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$prefix.inputs.json" -Encoding UTF8

    $base = Get-Content -LiteralPath (Join-Path $MinecraftInstall 'versions/1.12.2/1.12.2.json') -Raw | ConvertFrom-Json
    $forge = Get-Content -LiteralPath (Join-Path $MinecraftInstall "versions/$forgeId/$forgeId.json") -Raw | ConvertFrom-Json
    $libraries = [ordered]@{}
    foreach ($entry in @($forge.libraries) + @($base.libraries)) {
        $group, $artifact, $version = $entry.name.Split(':')
        $key = "$group/$artifact"
        if ($libraries.Contains($key) -or ($entry.rules.Count -eq 1 -and $entry.rules[0].os.name -eq 'osx')) { continue }
        $relative = $entry.downloads.artifact.path
        if (!$relative) { continue }
        $libraries[$key] = (Resolve-Path -LiteralPath (Join-Path $MinecraftInstall "libraries/$relative")).ProviderPath
    }
    $classpath = @($libraries.Values) + (Join-Path $MinecraftInstall 'versions/1.12.2/1.12.2.jar')
    $natives = (Resolve-Path -LiteralPath (Join-Path $MinecraftInstall "natives/$forgeId")).ProviderPath
    $assets = (Resolve-Path -LiteralPath (Join-Path $MinecraftInstall 'assets')).ProviderPath
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $client = Start-JavaPrototypeProcess (Join-Path $env:JAVA_HOME 'bin/java.exe') @('-Xmx3G', "-Djava.library.path=$natives",
        "-Drwharness=$($job.Replace('\', '/'))", '-cp', ($classpath -join ';'), $forge.mainClass, '--username', 'OptiFineTester',
        '--version', $forgeId, '--gameDir', $work, '--assetsDir', $assets, '--assetIndex', $base.assetIndex.id,
        '--uuid', '00000000000000000000000000000000', '--accessToken', 'offline', '--userType', 'legacy',
        '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker', '--versionType', 'Forge') $work $log
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) {
        if (@(Get-NavForeignJava -Roots @($client.Id)).Count) { throw 'A foreign Minecraft or Gradle workload started.' }
        Start-Sleep -Milliseconds 500
    }
    Assert $client.HasExited 'client completed before timeout'
    [M61ProcessLog]::Finish($client)
    Assert ($client.ExitCode -eq 0) 'clean client process exit'
    $text = Read-JavaPrototypeLog $log
    Assert ($text.Contains('RWCORE coremod nalozen, runtimeDeobf=1')) 'packaged coremod in production SRG mode'
    Assert ($text.Contains('OptiFine_1.12.2_HD_U_G5')) 'OptiFine G5 in client log'
    foreach ($line in @(Select-String -LiteralPath $log -Pattern 'RWBATCH|\[Shaders\] (Loaded|Shader|Program)' | Select-Object -First 20)) { Write-Host "LOG $($line.Line)" }
    Assert (-not ($text -match 'NoSuchMethodError|NoSuchFieldError|AbstractMethodError|IncompatibleClassChangeError|NoClassDefFoundError|VerifyError|ClassCastException|RWHARNESS-NAPAKA|Couldn''t render entity|Exception ticking|OpenGL error')) 'no linkage/render/OpenGL errors in log'
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    $checks = @($proof.checks)
    foreach ($c in $checks) { Write-Host ("RESULT {0} ok={1} {2}" -f $c.key, $c.ok, $c.detail) }
    $rowKeys = @('optifine-ordinary', 'optifine-ordinary-after-live')
    $expected = @('optifine-loaded', 'shader-pack', 'live-world-original', 'live-world-original-gl', 'live-0-image', 'live-1-image',
        'optifine-restore-gl', 'optifine-restore-gl-after-live') +
        @($rowKeys | ForEach-Object { $_, "$_-gl", "$_-gate" })
    Assert ($checks.Count -eq $expected.Count -and @($checks.key | Select-Object -Unique).Count -eq $expected.Count) 'exact distinct check count'
    foreach ($key in $expected) {
        $row = @($checks | Where-Object { $_.key -ceq $key })
        Assert ($row.Count -eq 1 -and $row[0].ok -eq $true) $key
    }
    Assert ($proof.success -eq $true -and $proof.error -ceq '' -and $text.Contains('M143A DONE success=true')) 'OptiFine proof success'
    Assert ($proof.joins -eq 1 -and $proof.width -eq 256 -and $proof.height -eq 256) 'real world join and fixed FBO'
    Assert (@($proof.rows).Count -eq $rowKeys.Count) 'all image/state rows'
    foreach ($row in $proof.rows) {
        Assert ($rowKeys -ccontains $row.key) "known row $($row.key)"
        Assert ($row.repeatEqual -eq $true -and $row.stateEqual -eq $true -and $row.imageEqual -eq $true) "bitwise A/B/A2 $($row.key)"
        Assert ($row.draws -eq 0 -and $row.fallbacks -eq 1) "original route at switch 1 $($row.key)"
    }
    foreach ($s in @('live-0', 'live-1')) { Assert (Test-Path -LiteralPath "$prefix-$s.png") "live image $s" }
    Unchanged
    Write-Host "PASS M14.3b8 OptiFine shader=$Shader $out"
} finally {
    if ($null -ne $client) { Stop-JavaPrototypeProcess $client }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava; $env:PATH = $oldPath
}
