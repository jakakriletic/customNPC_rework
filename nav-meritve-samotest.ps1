# Controller-owned harness validation; never launches Minecraft or Gradle.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'meritve-lib.ps1')
. (Join-Path $PSScriptRoot 'nav-meritve-lib.ps1')
function Assert([bool]$Ok, [string]$What) { if (-not $Ok) { throw $What }; Write-Host "OK $What" }
# Deterministic startup race: the owned server Java child is absent from the first
# enumeration and present in the second. The legacy two-read guard misclassifies it.
$wrapper = [pscustomobject]@{ ProcessId = 102; ParentProcessId = 101; Name = 'java.exe'; CommandLine = 'GradleWrapperMain' }
$server = [pscustomobject]@{ ProcessId = 103; ParentProcessId = 102; Name = 'java.exe'; CommandLine = 'GradleStartServer' }
$foreignJava = [pscustomobject]@{ ProcessId = 203; ParentProcessId = 201; Name = 'javaw.exe'; CommandLine = 'net.minecraft.client.main.Main' }
$script:navSnapshots = New-Object System.Collections.Queue
function Get-CimInstance {
    param($ClassName, $Filter, $ErrorAction)
    if ($ClassName -ne 'Win32_Process') { throw 'Unexpected process query.' }
    if ($script:navSnapshots.Count -eq 0) { throw 'Unexpected extra process enumeration.' }
    return $script:navSnapshots.Dequeue()
}
try {
    $script:navSnapshots.Enqueue(@($wrapper, $foreignJava))
    $script:navSnapshots.Enqueue(@($wrapper, $server, $foreignJava))
    $legacy = @(Get-TujiJava (Get-DrevoProcesov @(101)))
    Assert (@($legacy | Where-Object { $_ -like '103:*' }).Count -eq 1) 'reproduce split-snapshot false foreign child'
    $script:navSnapshots.Enqueue(@($wrapper, $server, $foreignJava))
    $actual = @(Get-NavForeignJava -Roots @(101))
    Assert ($script:navSnapshots.Count -eq 0 -and $actual.Count -eq 1 -and $actual[0] -like '203 (parent 201):*') 'one snapshot excludes owned descendants and rejects foreign Java'
    $script:navSnapshots.Enqueue(@($wrapper, $server))
    Assert (@(Get-NavForeignJava -Roots @(101)).Count -eq 0) 'own server tree is permitted'
    $script:navSnapshots.Enqueue(@($wrapper, $server))
    Assert (@(Get-NavForeignJava -Roots @(301)).Count -eq 2) 'previous launch tree is foreign to a new launch'
    $script:navSnapshots.Enqueue(@($wrapper, $server))
    Assert (@(Get-NavForeignJava).Count -eq 2) 'preflight rejects all existing Gradle or Minecraft Java'
    $threw = $false
    try { $null = Get-NavForeignJava } catch { $threw = $true }
    Assert $threw 'process enumeration failure is fatal'
} finally { Remove-Item Function:\Get-CimInstance }
$temp = Join-Path $PSScriptRoot ('audit\m415a-harness-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $temp | Out-Null
$seed = Join-Path $PSScriptRoot 'dev\testworld\customnpcs\clones\1\LET_Walker.json'
$seedHash = (Get-FileHash -LiteralPath $seed).Hash
$clone = Join-Path $temp 'large.json'
Write-NavFixture $seed $clone 'NAVC_Large' 10 1
$text = [IO.File]::ReadAllText($clone)
Assert ($text -match '"RwNavBackend": 1' -and $text -match '"Name": "NAVC_Large"' -and $text -match '"Size": 10' -and $text -match '"pSize": 5' -and $text -match '"MovementType": 1') 'only intended clone fields changed'
Assert ((Get-FileHash -LiteralPath $seed).Hash -eq $seedHash) 'seed preserved'
$threw = $false
try { Write-NavFixture $clone (Join-Path $temp 'duplicate.json') } catch { $threw = $true }
Assert $threw 'refuse an existing selection'
$controller = Join-Path $temp 'controller.json'
Install-NavController (Join-Path $PSScriptRoot 'dev\testworld\customnpcs\clones\1\PERF_Kontrola.json') `
    (Join-Path $PSScriptRoot 'dev\testworld\nav-compat-control.js') $controller
$out = & node (Join-Path $PSScriptRoot 'dev\testworld\preveri-nav-compat.js') $controller (Join-Path $PSScriptRoot 'dev\testworld\nav-compat-control.js')
Assert ($LASTEXITCODE -eq 0) 'clone-format roundtrip, legacy JSON bug reproduction and all controller phases'
$proof = $out -join "`n"
[IO.File]::WriteAllText((Join-Path $temp 'markers.log'), $proof)
Assert (Test-NavCompatibility $proof) 'exact valid 18-row proof accepted'
Assert (-not (Test-NavCompatibility ($proof.Replace('selected=1', 'selected=0')))) 'reject absent selection'
Assert (-not (Test-NavCompatibility ($proof.Replace('width=1.200', 'width=0.600')))) 'reject large fixture with normal hitbox'
Assert (-not (Test-NavCompatibility ($proof.Replace('height=1.900', 'height=1.800')))) 'reject interface initial height in place of humanoid model height'
Assert (-not (Test-NavCompatibility ($proof.Replace('height=3.800', 'height=3.600')))) 'reject wrong scaled large height with unchanged tolerance'
Assert (-not (Test-NavCompatibility ($proof.Replace('height=1.463', 'height=1.900')))) 'reject missing original-mode riding hitbox history'
Assert (-not (Test-NavCompatibility ($proof.Replace('name=Large selected=1 attached=0', 'name=Large selected=1 attached=1')))) 'reject unsupported attachment'
Assert (-not (Test-NavCompatibility ($proof.Replace('movement=2', 'movement=0')))) 'reject wrong movement'
Assert (-not (Test-NavCompatibility ($proof.Replace('distance=0.000', 'distance=9.000')))) 'reject failed arrival'
Assert (-not (Test-NavCompatibility (($out | Select-Object -Skip 1) -join "`n"))) 'reject missing row'
Assert (-not (Test-NavCompatibility ($proof + "`n" + $out[0]))) 'reject duplicate row'
$status = Read-NavStatus 'RWNAV global=1 mod=true ime=PERF_ ujemanj=500 izbranih=500 pripetih=500'
Assert (Test-NavStatus $status 1 500 500) 'actual backend parser accepts intended cohort'
Assert (-not (Test-NavStatus $status 0 500 500)) 'global mode is checked'
Assert (-not (Test-NavStatus $status 1 1000 1000)) 'cohort count is checked'
$status.Attached = 0
Assert (-not (Test-NavStatus $status 1 500 500)) 'selected alone does not prove attachment'
$perf = Read-NavPerf 'NPCB-PERF iskanj=100 koncanih=90 zavrnjenih=3 neuspelih=7 deljenih=8 | iskanje us p50=11 p95=22 | vrsta us p50=33 p95=44 | posnetek us p50=55 p95=66 (chunkov p50=9) | glavna nit us/tick p50=77 p95=88 p99=99 max=111 (tickov 2400)'
Assert ($perf.Count -eq 17 -and $perf['searches'] -eq 100 -and $perf['shared'] -eq 8 -and $perf['snapshot.us.p95'] -eq 66 -and $perf['main.us.p95'] -eq 88 -and $perf['ticks'] -eq 2400) 'library counters use the correct fields'
Assert ($null -eq (Read-NavPerf 'NPCB-PERF broken')) 'reject unrecognised counters'
foreach ($path in @('perf-run.ps1', 'nav-meritve-lib.ps1', 'nav-meritve-samotest.ps1', 'nav-lestvica-run.ps1', 'nav-compat-run.ps1')) {
    $tokens = $null; $errors = $null
    $null = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $path), [ref]$tokens, [ref]$errors)
    Assert ($errors.Count -eq 0) "PowerShell syntax: $path"
}
Write-Host "M4.15a harness PASS: $temp (mocked contract, not Minecraft behaviour)"
exit 0
