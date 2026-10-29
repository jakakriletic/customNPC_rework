# Vanilla Properties.store rewrites its date and Hashtable entry order. Preserve
# every raw property record and the line endings; reject unfamiliar syntax.
function Get-NavGuiPropertiesContent([byte[]]$Bytes) {
    $text = [Text.Encoding]::GetEncoding(28591).GetString($Bytes)
    $date = '#(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun) (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) [0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} [A-Za-z0-9:+_-]+ [0-9]{4}'
    $pattern = '\A(#Minecraft server properties\r?\n)' + $date + '(?=\r?\n)'
    if (-not [regex]::IsMatch($text, $pattern)) { throw 'Unexpected server.properties header; refusing to normalize it.' }
    $normalized = [regex]::Replace($text, $pattern, '${1}#<Properties.store date>')
    $newline = [regex]::Match($text, '\r?\n').Value
    $lines = $normalized.Split([string[]]@($newline), [StringSplitOptions]::None)
    if ($lines.Count -lt 4 -or $lines[-1] -cne '') { throw 'Expected newline-terminated server properties.' }
    $keys = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    [string[]]$entries = $lines[2..($lines.Count - 2)]
    foreach ($line in $entries) {
        $record = [regex]::Match($line, '^([A-Za-z0-9._-]+)=')
        if ($line -match '[\r\n]' -or -not $record.Success) {
            throw 'Unexpected property record or line ending; refusing to normalize it.'
        }
        $key = $record.Groups[1].Value
        if (-not $keys.Add($key)) { throw 'Duplicate property key; refusing to normalize it.' }
        $ending = [regex]::Match($line, '(\\+)$')
        if ($ending.Success -and $ending.Value.Length % 2 -eq 1) {
            throw 'Continued property record; refusing to normalize it.'
        }
    }
    [Array]::Sort($entries, [StringComparer]::Ordinal)
    return $lines[0] + $newline + $lines[1] + $newline + ($entries -join $newline) + $newline
}

function Restore-NavGuiProperties([string]$Path, [byte[]]$Original) {
    $current = [IO.File]::ReadAllBytes($Path)
    if (-not [string]::Equals((Get-NavGuiPropertiesContent $current), (Get-NavGuiPropertiesContent $Original), [StringComparison]::Ordinal)) {
        throw 'server.properties records or line endings changed beyond the Properties.store date/order; preserving changed file.'
    }
    [IO.File]::WriteAllBytes($Path, $Original)
}
