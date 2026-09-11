param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = Join-Path $PSScriptRoot '.tools\jdk8'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
if ($GradleArgs -contains 'runClient') { & (Join-Path $PSScriptRoot 'prepare-assets.ps1') }
Push-Location (Join-Path $PSScriptRoot 'dev')
try {
    if (-not $GradleArgs) { $GradleArgs = @('tasks', '--offline') }
    & .\gradlew.bat @GradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }
