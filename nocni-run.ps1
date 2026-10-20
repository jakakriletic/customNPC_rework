param([Parameter(ValueFromRemainingArguments=$true)][string[]]$RunnerArgs)
$ErrorActionPreference = 'Stop'
& node (Join-Path $PSScriptRoot 'overnight\runner.mjs') @RunnerArgs
exit $LASTEXITCODE
