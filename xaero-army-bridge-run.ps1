# M11.7b: controller-owned test/build/package of the external map adapter, no Minecraft launch.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$project = [IO.Path]::GetFullPath((Join-Path $root '../advantura_estetika_mod/xaero-winter-storm'))
$snapshot = Join-Path $root 'integrations/xaero-fleet-command'
$manifestPath = Join-Path $snapshot 'inputs.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$report = Join-Path $root ("audit/" + ($manifest.package -replace '\.', '').ToLower() + "-bridge-$stamp.json")
$oldJava = $env:JAVA_HOME

function Assert([bool]$Ok, [string]$Text) {
    if (-not $Ok) { throw $Text }
    Write-Host "OK $Text"
}
function Check-Inputs {
    foreach ($inputFile in $manifest.files) {
        $path = Join-Path $project $inputFile.path
        Assert (Test-Path -LiteralPath $path -PathType Leaf) "input exists: $($inputFile.path)"
        Assert ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -eq $inputFile.sha256) "input SHA256: $($inputFile.path)"
        if ($inputFile.sourceCopy) {
            Assert ((Get-FileHash -LiteralPath (Join-Path $snapshot $inputFile.path) -Algorithm SHA256).Hash -eq $inputFile.sha256) "committed source copy: $($inputFile.path)"
        }
    }
    $expected = @($manifest.files | Where-Object { $_.path -like 'src/*' } | ForEach-Object path | Sort-Object)
    $actual = @(Get-ChildItem -LiteralPath (Join-Path $project 'src') -File -Recurse | ForEach-Object { $_.FullName.Substring($project.Length+1).Replace('\','/') } | Sort-Object)
    Assert (-not (Compare-Object $expected $actual)) 'source tree exactly matches frozen inputs (no unrecorded code)'
}

try {
    Check-Inputs
    # The v0.1.8-winter build uses Gradle 8.12 (Java 21) and a separate Java 8 toolchain.
    $env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jre-21.0.11.10-hotspot'
    Assert (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe')) 'Gradle Java 21 runtime exists'
    Assert (Test-Path -LiteralPath 'C:/Program Files/Java/jdk1.8.0_202/bin/javac.exe') 'configured Minecraft JDK 8 exists'
    Push-Location -LiteralPath $project
    try {
        # RFG's shaded Download predicate checks its legacy Fernflower output before
        # RFG can skip that task for Minecraft 1.12.2. Offline mode throws when the
        # unused legacy file is absent. Keep the pinned build inputs, allow normal
        # dependency resolution, and retain a stack trace for any actual build failure.
        & .\gradlew.bat test build --no-daemon --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "External test/build failed (Gradle exit $LASTEXITCODE); inspect the controller log." }
        Write-Host 'OK external unit/regression tests and production reobfuscated build passed'
    } finally { Pop-Location }
    Check-Inputs
    $results = @(Get-ChildItem -LiteralPath (Join-Path $project 'build/test-results/test') -Filter 'TEST-*.xml' -File)
    Assert ($results.Count -ge 15) 'test reports include existing fleet/winter regressions and new army tests'
    $total=0; $suites=@()
    foreach ($file in $results) {
        [xml]$xml=Get-Content -LiteralPath $file.FullName -Raw
        Assert (([int]$xml.testsuite.failures -eq 0) -and ([int]$xml.testsuite.errors -eq 0)) "JUnit passed: $($xml.testsuite.name)"
        Assert ([int]$xml.testsuite.skipped -eq 0) "JUnit has no skipped tests: $($xml.testsuite.name)"
        $total += [int]$xml.testsuite.tests
        $suites += [pscustomobject]@{name=$xml.testsuite.name; tests=[int]$xml.testsuite.tests; sha256=(Get-FileHash -LiteralPath $file.FullName).Hash}
    }
    foreach ($required in @('si.ladja.xaerofleet.integration.CnpcArmyBridgeTest','si.ladja.xaerofleet.network.ArmyPacketTest','si.ladja.xaerofleet.client.ArmyClientTest')) {
        Assert (@($suites | Where-Object name -eq $required).Count -eq 1) "new suite executed: $required"
    }
    $jar = Join-Path $project ('build/libs/' + $manifest.artifact)
    Assert (Test-Path -LiteralPath $jar) "release $($manifest.artifact) exists (not the dev jar)"
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in @('si/ladja/xaerofleet/integration/CnpcArmyBridge.class','si/ladja/xaerofleet/integration/ArmyGround.class',
                'si/ladja/xaerofleet/client/ArmyMapLayer.class','si/ladja/xaerofleet/client/StormMapLayer.class',
                'si/ladja/xaerofleet/integration/DavincisFleetBridge.class','si/ladja/xaerofleet/storm/StormService.class')) {
            Assert ($null -ne $zip.GetEntry($entry)) "release contains $entry"
        }
        Assert (@($zip.Entries | Where-Object { $_.FullName -like 'noppes/*' -or $_.FullName -like '*/qa/*' -or $_.FullName -like '*Test.class' }).Count -eq 0) 'release embeds no CNPC, QA or test classes'
        $reader=New-Object IO.StreamReader($zip.GetEntry('mcmod.info').Open())
        try { $info=$reader.ReadToEnd() } finally { $reader.Dispose() }
        $wire = if ($manifest.wire) { $manifest.wire } else { '0.6.0-armies' }
        Assert ($info.Contains($wire)) "release metadata has wire compatibility version $wire"
    } finally { $zip.Dispose() }
    $proof=[ordered]@{package=$manifest.package; result='PASS'; sourceManifestSha256=(Get-FileHash -LiteralPath $manifestPath).Hash;
        sourceFiles=$manifest.files; jar=$jar; jarSha256=(Get-FileHash -LiteralPath $jar).Hash; tests=$total; suites=$suites;
        limitations=@('No live Minecraft or manual map validation in this build scenario','No production runtime reflection/ground-height proof','CNPC source unchanged; no repeat of M11.7a')}
    $proof | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $report -Encoding UTF8
    Write-Host "M117B PASS: $total tests; proof $report"
} finally { $env:JAVA_HOME=$oldJava }
