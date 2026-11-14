param(
    [string]$RunName = ('staff-' + (Get-Date -Format 'yyyy-MM-dd-HHmmss')),
    [string]$Bundle,
    [string]$MinecraftInstall = (Join-Path $env:USERPROFILE 'curseforge/minecraft/Install')
)

$root = $PSScriptRoot
$dev = Join-Path $root 'dev'
$ErrorActionPreference = 'Stop'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $dev 'testworld/java-compiler-lib.ps1')
$work = Join-Path $dev "run-command-staff/$RunName"
$log = Join-Path $root "audit/$RunName.log"
$job = Join-Path $work 'staff.job'
$client = $null
$lock = $null
$oldJava = $env:JAVA_HOME; $oldPath = $env:PATH
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'command-staff'
    if (@(Get-NavForeignJava).Count) { throw 'A Minecraft or Gradle workload is already running.' }
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $dev 'run/config') -Destination (Join-Path $work 'config') -Recurse
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $text = [regex]::Replace([IO.File]::ReadAllText($cfg), '(?m)^RwCommandStaff=.*\r?\n?', '')
    [IO.File]::WriteAllText($cfg, $text + "`nRwCommandStaff=1`n")
    $jobLines = @('commandStaff=1', 'world=command-staff-proof')
    $jobLines += ('cavalryClone=' + ((Join-Path $root 'examples/clones/1/Konjenik.json').Replace('\','/')))
    if ($Bundle) {
        $jobLines += 'bundledVaried=1'
        $mods = New-Item -ItemType Directory -Path (Join-Path $work 'mods')
        Copy-Item -LiteralPath $Bundle -Destination $mods.FullName
        $forgeId = 'forge-14.23.5.2860'
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
        [IO.File]::WriteAllLines($job, [string[]]$jobLines)
        $client = Start-JavaPrototypeProcess (Join-Path $env:JAVA_HOME 'bin/java.exe') @('-Xmx2G', "-Djava.library.path=$natives",
            "-Drwharness=$job", '-cp', ($classpath -join ';'), $forge.mainClass, '--username', 'StaffTester',
            '--version', $forgeId, '--gameDir', $work, '--assetsDir', $assets, '--assetIndex', $base.assetIndex.id,
            '--uuid', '00000000000000000000000000000000', '--accessToken', 'offline', '--userType', 'legacy',
            '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker', '--versionType', 'Forge') $work $log
    } else {
        [IO.File]::WriteAllLines($job, [string[]]$jobLines)
        $client = Start-JavaPrototypeProcess (Join-Path $dev 'gradlew.bat') @('runClient', '--offline', '--no-daemon', '--console=plain',
            "-PrwRunDir=$work", "-PrwHarness=$($job.Replace('\','/'))", '-PrwUsername=StaffTester') $dev $log
    }
    $end = (Get-Date).AddMinutes(5)
    while (!$client.HasExited -and (Get-Date) -lt $end) {
        if (@(Get-NavForeignJava -Roots @($client.Id)).Count) { throw 'A foreign Minecraft or Gradle workload started.' }
        Start-Sleep -Milliseconds 500
    }
    if (!$client.HasExited) { throw "Staff test timed out. See $log" }
    [M61ProcessLog]::Finish($client)
    $text = Read-JavaPrototypeLog $log
    if ($client.ExitCode -ne 0 -or !$text.Contains('STAFF DONE true') -or $text -match 'Exception ticking|NoSuchMethodError|NoClassDefFoundError|Model definition for location rwcommandstaff') {
        throw "Command Staff test failed. See $log"
    }
    Select-String -LiteralPath $log -SimpleMatch 'STAFF CHECK' | ForEach-Object { $_.Line }
    Write-Output "PASS Command Staff $log"
} finally {
    if ($null -ne $client) { Stop-JavaPrototypeProcess $client }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava; $env:PATH = $oldPath
}
