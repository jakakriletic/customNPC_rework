# Uses the repository's pinned Gradle/JDK 8; never launches Minecraft or installs scripts.
param([string]$Manifest = 'scripts.json', [string]$Report = '.local/compile.json', [switch]$Ide)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
# A copied template can point to its owning checkout without storing a machine path in git.
if ($env:CNPC_REWORK_ROOT) { $root = [IO.Path]::GetFullPath($env:CNPC_REWORK_ROOT) }
$driver = Join-Path $root 'dev.ps1'
if (-not (Test-Path -LiteralPath $driver)) { throw 'Set CNPC_REWORK_ROOT to the prepared CustomNPC rework checkout.' }
$tasks = if ($Ide) { @('compileJava','idea','eclipse','compileScripts') } else { @('compileScripts') }
& $driver -GradleArgs (@('-p',$PSScriptRoot) + $tasks + @('--offline','--no-daemon','--console=plain',
    "-PscriptsManifest=$Manifest", "-PscriptsReport=$Report"))
