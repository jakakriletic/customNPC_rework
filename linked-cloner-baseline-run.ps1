# M13.4 import-only proof. Retire this ID after the first functional GUI edit.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$relative = 'noppes/npcs/client/gui/GuiNpcMobSpawner.java'
$reference = Join-Path $root "dev/reference-src/$relative"
$restored = Join-Path $root "dev/src/patch/java/$relative"
$a = (Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash
$b = (Get-FileHash -LiteralPath $restored -Algorithm SHA256).Hash
if ($a -cne $b) { throw 'GuiNpcMobSpawner import differs from reference-src' }
$baseline = (Get-FileHash -LiteralPath (Join-Path $root 'CustomNPCs_1.12.2-(01Oct19).jar') -Algorithm SHA256).Hash
if ($baseline -ine 'cafacade45fb2aa6ac52956487889d0f4a9d4f6a1682ea28a1f7aca1ba100fa1') { throw 'Original jar changed' }
$path = Join-Path $root ('audit/m134-cloner-baseline-' + (Get-Date -Format 'yyyy-MM-dd-HHmmss') + '.json')
@{success=$true; package='M13.4 import baseline'; source=$relative; referenceSha256=$a; restoredSha256=$b;
  originalJarSha256=$baseline; condition='byte-identical source import; GUI entry points checked by testOriginal and test; no Minecraft launch'} |
  ConvertTo-Json | Set-Content -LiteralPath $path -Encoding UTF8
Write-Host "PASS M13.4 byte-identical import; evidence $path"
