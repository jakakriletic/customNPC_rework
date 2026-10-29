# Controller-owned M6.10 acceptance; one IDE compile, one good and one bad CLI corpus.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$work = Join-Path $root "dev/build/m610/template $stamp"
$prefix = Join-Path $root "audit/m610-template-$stamp"
$lock = $null
$proc = $null
$exitCode = 1
$oldRoot = $env:CNPC_REWORK_ROOT
function Quote-M610([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
function Invoke-M610([string]$Driver, [string[]]$Arguments, [string]$Tag, [int]$Expected) {
    $script:proc = Start-JavaPrototypeProcess (Join-Path $PSHOME 'powershell.exe') (@('-NoProfile','-ExecutionPolicy','Bypass','-File',$Driver) + $Arguments) $root "$prefix-$Tag.log"
    $until = (Get-Date).AddSeconds(600)
    while (-not $script:proc.HasExited -and (Get-Date) -lt $until) {
        Assert-M610NoGame
        if (@(Get-NavForeignJava -Roots @($script:proc.Id)).Count) { throw 'Competing Java workload' }
        Start-Sleep -Milliseconds 500
    }
    Assert-JavaPrototype $script:proc.HasExited "$Tag completed"
    [M61ProcessLog]::Finish($script:proc)
    Assert-JavaPrototype ($script:proc.ExitCode -eq $Expected) "$Tag exit $Expected"
    $script:proc = $null
    $log = Read-JavaPrototypeLog "$prefix-$Tag.log"
    Assert-JavaPrototype ($log -notmatch ':runClient|:runServer|Starting minecraft|MinecraftForge v|:buildPatchedMod') "$Tag no game/buildPatchedMod task"
}
function Assert-M610NoGame {
    $games = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction Stop |
        Where-Object { [string]$_.CommandLine -match 'net\.minecraft\.launchwrapper\.Launch|GradleStart(Server)?(\s|$)|net\.minecraft\.server\.MinecraftServer' })
    if ($games.Count) { throw 'Minecraft JVM during offline template scenario' }
}
function Get-M610Inputs {
    $hashes = [ordered]@{}
    foreach ($dir in @('dev/src/patch','dev/src/scriptTemplate','scripts/java/template')) {
        foreach ($f in @(Get-ChildItem -LiteralPath (Join-Path $root $dir) -Recurse -File | Sort-Object FullName)) {
            $relative = $f.FullName.Substring($root.Length + 1).Replace('\','/')
            if ($relative -match '/(\.local|\.gradle|build|\.idea|\.settings|bin)/' -or $f.Name -match '\.(iml|ipr|iws)$|^\.(classpath|project)$') { continue }
            $hashes[$relative] = Get-JavaPrototypeHash $f.FullName
        }
    }
    foreach ($relative in @('dev/build.gradle','dev/libs/customnpcs-mapped-01Oct19.jar','java-template-run.ps1','dev/testworld/java-template-inputs.json')) {
        $hashes[$relative] = Get-JavaPrototypeHash (Join-Path $root $relative)
    }
    return $hashes
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm610-template'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no competing Java workload'
    Assert-M610NoGame
    # scripts/ is outside the controller's original fingerprintPaths. These canonical
    # source pins live in dev/testworld (fingerprinted), so a template edit cannot reuse a pass.
    $pins = [IO.File]::ReadAllText((Join-Path $root 'dev/testworld/java-template-inputs.json')) | ConvertFrom-Json
    foreach ($pin in $pins.PSObject.Properties) {
        $source = [IO.File]::ReadAllText((Join-Path $root $pin.Name)).Replace("`r`n", "`n")
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $actual = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($source)))).Replace('-','').ToLowerInvariant() }
        finally { $sha.Dispose() }
        Assert-JavaPrototype ($actual -ceq $pin.Value) "fingerprinted template pin $($pin.Name)"
    }
    $before = Get-M610Inputs
    $templateNames = @($before.Keys | Where-Object { $_.StartsWith('scripts/java/template/') } | Sort-Object)
    $pinnedNames = @($pins.PSObject.Properties.Name | Sort-Object)
    Assert-JavaPrototype (($templateNames -join "`n") -ceq ($pinnedNames -join "`n")) 'all template inputs pinned, no extra or missing files'
    $package = Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    $packageHash = if (Test-Path -LiteralPath $package) { Get-JavaPrototypeHash $package } else { $null }
    New-Item -ItemType Directory -Path $work | Out-Null
    # Copy only source-controlled template inputs; leave any developer's local IDE state alone.
    foreach ($relative in @($before.Keys | Where-Object { $_.StartsWith('scripts/java/template/') })) {
        $destination = Join-Path $work $relative.Substring('scripts/java/template/'.Length)
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
        Copy-Item -LiteralPath (Join-Path $root $relative) -Destination $destination
    }
    $cpFile = Join-Path $work '.local/api-classpath.txt'
    $driver = Join-Path $work 'prepare-driver.ps1'
    $arguments = @('exportJavaTemplateClasspath','scriptTemplateClasses','--offline','--no-daemon','--console=plain',"-PjavaTemplateOutput=$cpFile")
    $literal = @($arguments | ForEach-Object { Quote-M610 $_ }) -join ','
    $body = '$ErrorActionPreference = ''Stop''; & ' + (Quote-M610 (Join-Path $root 'dev.ps1')) + ' -GradleArgs @(' + $literal + ')'
    [IO.File]::WriteAllText($driver,$body,(New-Object Text.UTF8Encoding($false)))
    $watch = [Diagnostics.Stopwatch]::StartNew()
    Invoke-M610 $driver @() 'prepare' 0
    $api = [IO.File]::ReadAllText($cpFile).Trim()
    $paths = $api.Split([IO.Path]::PathSeparator)
    Assert-JavaPrototype ([IO.Path]::GetFullPath($paths[0]) -ieq [IO.Path]::GetFullPath((Join-Path $root 'dev/build/classes/java/main'))) 'patched classes first in exported API'
    foreach ($path in $paths) { Assert-JavaPrototype (Test-Path -LiteralPath $path) "exported API input exists: $path" }
    $env:CNPC_REWORK_ROOT = $root
    Invoke-M610 (Join-Path $work 'check.ps1') @('-Ide') 'valid-ide' 0
    Invoke-M610 (Join-Path $work 'check.ps1') @('-Manifest','examples/invalid/scripts.json','-Report','.local/invalid.json') 'invalid' 1
    $iml = @(Get-ChildItem -LiteralPath $work -Filter '*.iml' -File)
    Assert-JavaPrototype ($iml.Count -eq 1) 'one IntelliJ module generated'
    $idea = [IO.File]::ReadAllText($iml[0].FullName)
    Assert-JavaPrototype ($idea.Contains('customnpcs-mapped-01Oct19.jar') -and $idea.Contains('src/main/java')) 'IntelliJ autocomplete model uses actual API and source root'
    [xml]$eclipse = [IO.File]::ReadAllText((Join-Path $work '.classpath'))
    $libraries = @($eclipse.classpath.classpathentry | Where-Object { $_.kind -ceq 'lib' } | ForEach-Object { [string]$_.path })
    Assert-JavaPrototype (@($libraries | Where-Object { $_.Replace('\','/') -ceq $paths[0].Replace('\','/') }).Count -eq 1) 'Eclipse autocomplete model includes patched API classes'
    $prefs = [IO.File]::ReadAllText((Join-Path $work '.settings/org.eclipse.jdt.core.prefs'))
    Assert-JavaPrototype ($prefs.Contains('org.eclipse.jdt.core.compiler.source=1.8') -and $prefs.Contains('org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.8')) 'Eclipse Java 8 source and target'
    $toolCp = (Join-Path $root 'dev/build/classes/java/scriptTemplate') + [IO.Path]::PathSeparator + $api
    $proc = Start-JavaPrototypeProcess (Join-Path $root '.tools/jdk8/bin/java.exe') @('-cp',$toolCp,'local.cnpctemplate.TemplateAcceptance',$work,"$prefix-contract.json") $root "$prefix-contract.log"
    Wait-JavaPrototypeProcess $proc 180
    $proc = $null
    $watch.Stop()
    $contract = [IO.File]::ReadAllText("$prefix-contract.json") | ConvertFrom-Json
    Assert-JavaPrototype ($contract.success -and $contract.hooks -eq 4 -and $contract.invalidTabs -eq 3) 'shared compiler/binder and diagnostics acceptance'
    $after = Get-M610Inputs
    Assert-JavaPrototype (($before | ConvertTo-Json -Compress) -ceq ($after | ConvertTo-Json -Compress)) 'source/API/tool/template inputs unchanged'
    if ($null -ne $packageHash) { Assert-JavaPrototype ((Get-JavaPrototypeHash $package) -ceq $packageHash) 'packaged mod unchanged' }
    Assert-M610NoGame
    @{success=$true;contract=$contract;commandMillis=$watch.Elapsed.TotalMilliseconds;inputs=$before;classpath=$api;
      classpathHash=(Get-JavaPrototypeHash $cpFile);work=$work;condition='one offline scenario in copied path with spaces; actual Gradle IDE models/compile and CLI exits 0/1; no game or production code change'} |
        ConvertTo-Json -Depth 10 | Set-Content -LiteralPath "$prefix-proof.json" -Encoding UTF8
    Write-Host "M610 evidence: $prefix-proof.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    $env:CNPC_REWORK_ROOT = $oldRoot
    try { Stop-JavaPrototypeProcess $proc } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $exitCode
