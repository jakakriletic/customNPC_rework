# Controller-only M6.1 language, diagnostics, real absent-provider and timing experiment.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$lock = $null
$proc = $null
$exitCode = 1
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$prefix = Join-Path $root "audit/m61-compiler-$stamp"
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm61-compiler'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no foreign Java workload'
    $cp = Get-JavaPrototypeInputs $root
    $jdk = Join-Path $root '.tools/jdk8'
    $classes = Join-Path $root "dev/build/m61/comparison-$stamp"
    New-Item -ItemType Directory -Path $classes | Out-Null
    $main = Join-Path $root 'dev/build/classes/java/main'
    Assert-JavaPrototype (Test-Path -LiteralPath (Join-Path $main 'noppes/npcs/rework/script/JdkScriptCompiler.class')) 'controller build produced compiler'
    $compileCp = "$main;$cp"
    $comparisonSource = Join-Path $root 'dev/src/prototype/java/noppes/npcs/rework/script/prototype/CompilerComparison.java'
    $proc = Start-JavaPrototypeProcess (Join-Path $jdk 'bin/javac.exe') @('-encoding','UTF-8','-source','8','-target','8','-proc:none','-cp',$compileCp,'-d',$classes,$comparisonSource) $root "$prefix-build-comparison.log"
    Wait-JavaPrototypeProcess $proc 120
    $proc = $null
    $artifacts = @{
        'janino' = '5699878C31E71C9A94CF472F95A2DAD52CA7B0449C099276B8BD7E992A43595B'
        'commons-compiler' = '17EA25D860225F4B59D0A512B4995F2E4711BDAE5E15736CBCD5F6CE4DDA8F7C'
    }
    $paths = @{}
    $lib = Join-Path $root 'dev/libs/m61'
    New-Item -ItemType Directory -Force -Path $lib | Out-Null
    foreach ($name in $artifacts.Keys) {
        $path = Join-Path $lib "$name-3.1.12.jar"
        if (-not (Test-Path -LiteralPath $path)) {
            Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/codehaus/janino/$name/3.1.12/$name-3.1.12.jar" -OutFile $path
        }
        Assert-JavaPrototype ((Get-JavaPrototypeHash $path) -ceq $artifacts[$name]) "pinned $name 3.1.12"
        $paths[$name] = $path
    }
    # A copy of the real JRE, outside its JDK parent: merely using jre/bin/java in
    # the original JDK still lets ToolProvider discover ../lib/tools.jar.
    $jre = Join-Path $root "dev/build/m61/isolated-jre-$stamp"
    Copy-Item -LiteralPath (Join-Path $jdk 'jre') -Destination $jre -Recurse
    $proofs = @()
    foreach ($backend in @('javac','janino','absent')) {
        $java = if ($backend -eq 'absent') { Join-Path $jre 'bin/java.exe' } else { Join-Path $jdk 'bin/java.exe' }
        $out = "$prefix-$backend.json"
        $argsList = @('-cp',"$classes;$compileCp",'noppes.npcs.rework.script.prototype.CompilerComparison',$backend,$out,$paths['janino'],$paths['commons-compiler'])
        $proc = Start-JavaPrototypeProcess $java $argsList $root "$prefix-$backend.log"
        Wait-JavaPrototypeProcess $proc 180
        $proc = $null
        $proof = [IO.File]::ReadAllText($out) | ConvertFrom-Json
        Assert-JavaPrototype ($proof.success -eq $true -and $proof.backend -ceq $backend) "$backend proof success"
        if ($backend -ne 'absent') {
            Assert-JavaPrototype (@($proof.cases).Count -eq 6) "$backend six language fixtures"
            Assert-JavaPrototype (@($proof.warmBasicNanos).Count -eq 5 -and $proof.coldBasicNanos -gt 0) "$backend cold plus five warm timings"
        }
        $proofs += $out
    }
    $inputs = @{}
    foreach ($path in @($cp.Split(';')) + @($comparisonSource,(Join-Path $root 'dev/src/patch/java/noppes/npcs/rework/script/JdkScriptCompiler.java'))) {
        if (Test-Path -LiteralPath $path -PathType Leaf) { $inputs[$path] = Get-JavaPrototypeHash $path }
    }
    @{ success=$true; proofs=$proofs; artifacts=$artifacts; inputs=$inputs; condition='one fresh JVM per provider; cold first Basic; five fresh warm Basic compilations; no game workload' } |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$prefix.json" -Encoding UTF8
    Write-Host "M61 compiler evidence: $prefix.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    try { Stop-JavaPrototypeProcess $proc } catch { Write-Host "FAIL $_"; $exitCode=1 }
    if ($null -ne $lock) { $lock.Dispose() }
    $env:JAVA_HOME=$oldJava; $env:PATH=$oldPath
}
exit $exitCode
