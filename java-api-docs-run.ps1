# Controller-owned M6.9 acceptance. No Minecraft, runtime scripts or production package changes.
param()
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
. (Join-Path $root 'meritve-lib.ps1')
. (Join-Path $root 'nav-meritve-lib.ps1')
. (Join-Path $root 'dev/testworld/java-compiler-lib.ps1')
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmmss'
$work = Join-Path $root "dev/build/m69/$stamp"
$prefix = Join-Path $root "audit/m69-api-$stamp"
$verified = Join-Path $work 'verified'
$lock = $null
$proc = $null
$exitCode = 1
function Get-M69SourceHashes {
    $hashes = [ordered]@{}
    foreach ($dir in @('dev/src/patch','dev/src/apiDocs')) {
        foreach ($f in @(Get-ChildItem -LiteralPath (Join-Path $root $dir) -Recurse -File | Sort-Object FullName)) {
            $relative = $f.FullName.Substring($root.Length + 1).Replace('\','/')
            $hashes[$relative] = Get-JavaPrototypeHash $f.FullName
        }
    }
    foreach ($relative in @('dev/build.gradle','dev/libs/customnpcs-mapped-01Oct19.jar')) {
        $hashes[$relative] = Get-JavaPrototypeHash (Join-Path $root $relative)
    }
    return $hashes
}
try {
    $lock = Enter-ScenarijZaklep -Pot (Join-Path $root '.scenarij.lock') -Kdo 'm69-api-docs'
    Assert-JavaPrototype (@(Get-NavForeignJava).Count -eq 0) 'no competing Java workload'
    New-Item -ItemType Directory -Path $work | Out-Null
    $before = Get-M69SourceHashes
    $package = Join-Path $root 'dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar'
    $packageHash = if (Test-Path -LiteralPath $package) { Get-JavaPrototypeHash $package } else { $null }
    $driver = Join-Path $work 'driver.ps1'
    function Quote-M69([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
    $arguments = @('verifyApiDocs','generateApiDocs','--offline','--no-daemon','--console=plain',
        "-PapiDocsVerifiedOutput=$verified", "-PapiDocsWork=$work")
    $literal = @($arguments | ForEach-Object { Quote-M69 $_ }) -join ','
    $body = '$ErrorActionPreference = ''Stop''; try { & ' + (Quote-M69 (Join-Path $root 'dev.ps1')) +
        ' -GradleArgs @(' + $literal + '); exit 0 } catch { Write-Host $_; exit 1 }'
    [IO.File]::WriteAllText($driver, $body, (New-Object Text.UTF8Encoding($false)))
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $proc = Start-JavaPrototypeProcess (Join-Path $PSHOME 'powershell.exe') @('-NoProfile','-ExecutionPolicy','Bypass','-File',$driver) $root "$prefix.log"
    Wait-JavaPrototypeProcess $proc 600
    $watch.Stop()
    $proc = $null
    $proof = [IO.File]::ReadAllText((Join-Path $work 'proof.json')) | ConvertFrom-Json
    Assert-JavaPrototype ($proof.success -and $proof.checks -gt 100 -and $proof.hooks -eq 16) 'independent symbol/reflection and compiled-example acceptance'
    $outputs = [ordered]@{}
    foreach ($file in @('API.json','API.md','HOOKS.md')) {
        $actual = Join-Path $root "docs/api/$file"
        $sha = Get-JavaPrototypeHash $actual
        Assert-JavaPrototype ($sha -ceq (Get-JavaPrototypeHash (Join-Path $verified $file))) "$file identical in independent JVMs and reversed discovery"
        $bytes = [IO.File]::ReadAllBytes($actual)
        Assert-JavaPrototype (-not ($bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191)) "$file UTF-8 without BOM"
        Assert-JavaPrototype (-not ([IO.File]::ReadAllText($actual).Contains("`r"))) "$file canonical LF"
        $outputs[$file] = $sha
    }
    $json = [IO.File]::ReadAllText((Join-Path $root 'docs/api/API.json')) | ConvertFrom-Json
    Assert-JavaPrototype ($json.signatureFingerprint -ceq $proof.signatureFingerprint) 'same signature fingerprint in proof and published JSON'
    $after = Get-M69SourceHashes
    Assert-JavaPrototype (($before | ConvertTo-Json -Compress) -ceq ($after | ConvertTo-Json -Compress)) 'production, tool sources and API binary unchanged by generation'
    if ($null -ne $packageHash) { Assert-JavaPrototype ((Get-JavaPrototypeHash $package) -ceq $packageHash) 'packaged mod not changed' }
    $log = Read-JavaPrototypeLog "$prefix.log"
    Assert-JavaPrototype ($log -notmatch ':runClient|:runServer|Starting minecraft|MinecraftForge v|:buildPatchedMod|:compileScripts') 'no game or script execution task'
    @{success=$true;acceptance=$proof;outputs=$outputs;commandMillis=$watch.Elapsed.TotalMilliseconds;inputs=$before;
      condition='one documentation acceptance; independent JVM byte equality; 16 compile-only examples; no production code or runtime change'} |
        ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$prefix-proof.json" -Encoding UTF8
    Write-Host "M69 evidence: $prefix-proof.json"
    $exitCode = 0
} catch { Write-Host "FAIL $_" }
finally {
    try { Stop-JavaPrototypeProcess $proc } catch { Write-Host "FAIL $_"; $exitCode = 1 }
    if ($null -ne $lock) { $lock.Dispose() }
}
exit $exitCode
