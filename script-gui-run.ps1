# M6.12 controller scenario: existing NPC script GUI with Java (Q184-Q187). One disposable run
# directory with a copy of dev/run/config plus the real RwJavaClasspath key, a fresh world, an
# edit client JVM (JS -> Java -> JS -> no compiler -> Java, clone) and a second verify JVM.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$run = Join-Path $root 'dev/run'
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$world = "rwm612-gui-$stamp"
$prefix = Join-Path $root "audit/m612-$stamp"
# Not under dev/build: Gradle deletes stale outputs there (Q137).
$work = Join-Path $root "dev/run-m612/gui-$stamp"
$lock = $null
$client = $null
$exitCode = 1
$proofs = @()
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$hashes = @{}
$utf8 = New-Object Text.UTF8Encoding $false

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Fingerprint([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 'absent' }
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Not a regular file: $Path" }
    return ('sha256:' + (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash)
}
function Guard {
    $owned = @()
    if ($null -ne $client) { $owned += $client.Id }
    $foreign = @(Get-NavForeignJava -Roots $owned)
    if ($foreign.Count) { throw ('Foreign Java workload: ' + ($foreign -join '; ')) }
}
function Unchanged {
    foreach ($path in $hashes.Keys) { Assert ((Fingerprint $path) -ceq $hashes[$path]) "unchanged $path" }
}
function Wait-NoForeign([int]$Seconds) {
    # --no-daemon may leave a single-use Gradle JVM for a moment after the client exits.
    $until = (Get-Date).AddSeconds($Seconds)
    do {
        if (@(Get-NavForeignJava).Count -eq 0) { return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $until)
    Guard
}
function Start-Client([string]$Pass) {
    $out = "$prefix-$Pass.json"
    $job = "$prefix-$Pass.job"
    $log = "$prefix-$Pass-client.log"
    [IO.File]::WriteAllLines($job, @('scriptGui=1', "world=$world", "pass=$Pass", ('out=' + $out.Replace('\', '/'))), $utf8)
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $env:ComSpec
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.Arguments = ('/c gradlew.bat runClient --offline --no-daemon --console=plain "-PrwRunDir={0}" "-PrwHarness={1}" > "{2}" 2>&1' -f $work.Replace('\', '/'), $job.Replace('\', '/'), $log)
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $script:client = [Diagnostics.Process]::Start($psi)
    $until = (Get-Date).AddSeconds(1200)
    while (-not $client.HasExited -and (Get-Date) -lt $until) {
        Guard
        Start-Sleep -Seconds 1
    }
    Assert $client.HasExited "client $Pass completed before timeout"
    Assert ($client.ExitCode -eq 0) "client $Pass clean process exit"
    $script:client = $null
    Wait-NoForeign 60
    Unchanged
    $text = [IO.File]::ReadAllText($log)
    Assert ($text.Contains("M612 DONE success=true pass=$Pass")) "client $Pass success marker"
    # Rounds 1 and 4 provoke Java tab errors on purpose; any JS engine error or linkage failure is a FAIL.
    Assert ($text -notmatch 'ScriptException|NoClassDefFoundError|NoSuchMethodError|M612 DONE success=false') "client $Pass no JS/linkage errors"
    Assert (Test-Path -LiteralPath $out) "client $Pass proof exists"
    $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
    Assert ($proof.success -eq $true -and $proof.pass -ceq $Pass) "client $Pass proof identity"
    $rounds = if ($Pass -eq 'edit') { 6 } else { 2 }
    Assert ($proof.rounds -eq $rounds) "client $Pass completed $rounds rounds"
    Assert (@($proof.assertions | Where-Object { $_.ok -ne $true }).Count -eq 0) "client $Pass all assertions passed"
    Assert (@($proof.assertions.key | Select-Object -Unique).Count -eq @($proof.assertions).Count) "client $Pass unique assertion keys"
    $required = @('config-classpath','language-registered','world-existence','integrated-player','default-language','include-loaded')
    foreach ($r in 0..($rounds - 1)) {
        $required += @('uuid','gui-languages','gui-language','pending-language','pending-enabled','pending-tab','closed','server-saved','ran' | ForEach-Object { "r$r-$_" })
        if ($r -gt 0) { $required += "r$r-gui-kept-source" }
    }
    if ($Pass -eq 'edit') {
        $required += @('r1-server-console','r4-server-console','r2-gui-console','r5-gui-console','r2-script-list','r2-script-list-closed','r3-script-list','r3-script-list-closed','clone-saved','clone-ran')
    }
    foreach ($key in $required) {
        Assert (@($proof.assertions | Where-Object { $_.key -ceq $key -and $_.ok -eq $true }).Count -eq 1) "client $Pass $key exists once"
    }
    Assert (@($proof.screenshots).Count -eq $rounds) "client $Pass screenshots count"
    foreach ($image in $proof.screenshots) {
        Assert ([IO.Path]::GetFileName($image) -ceq $image) 'screenshot local filename'
        $path = Join-Path (Join-Path $root 'audit/screenshots') $image
        Assert ((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1000) "screenshot $image"
    }
    foreach ($name in @('M612_A', 'M612_A_Clone')) { Assert ($proof.uuids.$name -cmatch '^[0-9a-f-]{36}$') "client $Pass UUID $name" }
    Assert ($proof.uuids.M612_A -cne $proof.uuids.M612_A_Clone) "client $Pass clone has its own UUID"
    $script:proofs += $proof
}

try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm612-gui'
    Guard
    foreach ($name in @('config/CustomNpcs.cfg', 'config/npcbaritone.cfg', 'server.properties', 'eula.txt', 'options.txt')) {
        $path = Join-Path $run $name
        $hashes[$path] = Fingerprint $path
    }
    Assert (-not ([IO.File]::ReadAllText((Join-Path $run 'config/CustomNpcs.cfg')) -match '(?m)^RwJavaClasspath=\S')) 'shared dev config keeps Java unlisted (empty RwJavaClasspath)'
    $env:JAVA_HOME = Join-Path $root '.tools/jdk8'
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    New-Item -ItemType Directory -Path $work | Out-Null
    Copy-Item -LiteralPath (Join-Path $run 'config') -Destination (Join-Path $work 'config') -Recurse
    if (Test-Path -LiteralPath (Join-Path $run 'options.txt')) { Copy-Item -LiteralPath (Join-Path $run 'options.txt') -Destination $work }
    # Same exported API classpath as the IDE template (M6.10): patch output before mapped dependencies.
    $classpathFile = Join-Path $work 'compiler-classpath.txt'
    & (Join-Path $root 'dev.ps1') -GradleArgs @('exportJavaTemplateClasspath','--offline','--no-daemon','--console=plain',"-PjavaTemplateOutput=$($classpathFile.Replace('\','/'))") | Out-Host
    Assert ($LASTEXITCODE -eq 0) 'exported Java API classpath'
    Wait-NoForeign 60
    $classpath = [IO.File]::ReadAllText($classpathFile).Trim()
    Assert ($classpath.Length -gt 0) 'non-empty compiler classpath'
    $cfg = Join-Path $work 'config/CustomNpcs.cfg'
    $cfgText = [IO.File]::ReadAllText($cfg)
    Assert (-not ($cfgText -match '(?m)^RwJavaClasspath=')) 'config copy has no RwJavaClasspath yet'
    # Java tabs and the language entry read the compiler classpath through the real config key.
    [IO.File]::AppendAllText($cfg, ("`nRwJavaClasspath=" + $classpathFile.Replace('\', '/') + "`n"), $utf8)
    $worldPath = Join-Path $work "saves/$world"
    Assert (-not (Test-Path -LiteralPath $worldPath)) 'fresh isolated world'
    $clones = Join-Path $worldPath 'customnpcs/clones/1'
    New-Item -ItemType Directory -Force -Path $clones | Out-Null
    $fixture = Join-Path $clones 'M612_A.json'
    Write-NavFixture (Join-Path $root 'dev/testworld/customnpcs/clones/1/LET_Walker.json') $fixture 'M612_A' 5 0
    $text = [IO.File]::ReadAllText($fixture).Replace('"RwNavBackend": 1,', '')
    Assert ([regex]::Matches($text, '"ScriptLanguage": ""').Count -eq 1 -and $text -match '"ScriptEnabled": 0b' -and $text -match '"Scripts": \[\s*\]') 'unscripted fixture'
    # A new NPC stores DataScript's default ECMAScript; the harness re-checks that default in-game.
    $text = $text.Replace('"ScriptLanguage": ""', '"ScriptLanguage": "ECMAScript"')
    [IO.File]::WriteAllText($fixture, $text, $utf8)
    $include = Join-Path $worldPath 'customnpcs/scripts/java/m612/helper.java'
    New-Item -ItemType Directory -Force -Path (Split-Path $include) | Out-Null
    [IO.File]::WriteAllText($include, "package m612;`n`npublic final class Helper {`n    public static String tag() { return `"helper`"; }`n}`n", $utf8)
    foreach ($pass in @('edit', 'verify')) {
        Start-Client $pass
        Assert (Test-Path -LiteralPath (Join-Path $worldPath 'level.dat')) "world persisted after $pass"
    }
    foreach ($name in @('M612_A', 'M612_A_Clone')) {
        Assert ($proofs[0].uuids.$name -ceq $proofs[1].uuids.$name) "same UUID after JVM/world restart: $name"
    }
    Unchanged
    $null = Write-MeritevJson -Path "$prefix.json" -Paket 'M6.12' -Scenarij 'script-gui-language-restart-clone' `
        -Odtis @{ harness = (Get-FileHash -LiteralPath $PSCommandPath).Hash; cnpcConfig = $hashes[(Join-Path $run 'config/CustomNpcs.cfg')]; classpath = (Fingerprint $classpathFile) } `
        -Velicine @{ assertions = (@($proofs[0].assertions).Count + @($proofs[1].assertions).Count); screenshots = 8; actors = 2; restarts = 1 } -Uspeh $true -Padle @()
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    if ($null -ne $client -and -not $client.HasExited) { & taskkill.exe /PID $client.Id /T /F 2>&1 | Out-Null; $exitCode = 1 }
    try { Unchanged } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
exit $exitCode
