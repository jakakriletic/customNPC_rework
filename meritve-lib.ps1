# meritve-lib.ps1 - skupni zapis merilnega zagona (M2.5c).
#
# Zakaj obstaja: protokol meritev (docs/01-ARHITEKTURA.md, razdelek 7) zahteva vsaj tri
# ponovitve. Dokler je edini izdelek zagona formatirana tabela v .md, je zdruzevanje
# ponovitev branje lastnega izpisa z regexom - in vsaka sprememba sirine stolpca tiho
# pokvari agregat. Zato vsak merilni zagon poleg porocila zapise se strojno berljiv
# zapis: ime velicine -> stevilka. Ponovitve zdruzuje .\ponovitve-run.ps1.
#
# Zapis ima dva dela in razlika med njima je bistvo protokola:
#
#   odtis     pogoji, pod katerimi je meritev nastala (geometrija prizorisca, stevilo
#             NPC-jev, parametri zagona). Med ponovitvami se NE sme razlikovati; ce se,
#             ponovitve niso ponovitve istega poskusa in jih ni dovoljeno zdruzevati.
#             Prav to ujame pozabljen reset sveta: drugi zagon v istem svetu ima NPC-je
#             prejsnjega.
#
#   velicine  kar se meri. Med ponovitvami se sme razlikovati; razpon te razlike je
#             merilni sum in je edini prag, nad katerim sme kasnejsi A/B sploh trditi,
#             da je nekaj boljse (05-SEJA-PROTOKOL.md, razdelek "Meritve").
#
# Dot-source iz merilne skripte:
#     . (Join-Path $PSScriptRoot 'meritve-lib.ps1')

$script:MeritevShema = 1

# Zapise zapis zagona. $Odtis in $Velicine sta ravni slovarji ime -> vrednost;
# gnezdenja namenoma ni, ker se zdruzevanje po ponovitvah dela po imenu kljuca.
function Write-MeritevJson {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Paket,
        [Parameter(Mandatory = $true)][string]$Scenarij,
        [Parameter(Mandatory = $true)][hashtable]$Odtis,
        [Parameter(Mandatory = $true)][hashtable]$Velicine,
        [bool]$Uspeh = $true,
        [string[]]$Padle = @()
    )

    $o = [ordered]@{}
    foreach ($k in ($Odtis.Keys | Sort-Object)) { $o[[string]$k] = $Odtis[$k] }
    $v = [ordered]@{}
    foreach ($k in ($Velicine.Keys | Sort-Object)) { $v[[string]$k] = $Velicine[$k] }

    $zapis = [ordered]@{
        shema    = $script:MeritevShema
        paket    = $Paket
        scenarij = $Scenarij
        zagon    = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        uspeh    = $Uspeh
        padlih   = $Padle.Count
        padle    = @($Padle)
        odtis    = $o
        velicine = $v
    }

    $json = ($zapis | ConvertTo-Json -Depth 6)
    $json = $json -replace "`r`n", "`n"
    $dir = Split-Path -Parent $Path
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    # .gitattributes: *.json je eol=lf, zato brez BOM in brez CRLF.
    [System.IO.File]::WriteAllText($Path, ($json + "`n"), (New-Object System.Text.UTF8Encoding($false)))
    return $Path
}

# Prebere zapis zagona in preveri, da je res zapis merilnega zagona te sheme.
# Ob napaki vrze; klicatelj naj napako pripise konkretnemu zagonu.
function Read-MeritevJson {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path $Path)) { throw "zapisa ni: $Path" }
    $raw = Get-Content $Path -Raw -ErrorAction Stop
    if ([string]::IsNullOrWhiteSpace($raw)) { throw "zapis je prazen: $Path" }
    $z = $raw | ConvertFrom-Json
    if ($null -eq $z.shema)    { throw "zapis nima polja 'shema': $Path" }
    if ([int]$z.shema -ne $script:MeritevShema) {
        throw ("zapis je sheme {0}, ta skripta pozna {1}: {2}" -f $z.shema, $script:MeritevShema, $Path)
    }
    if ($null -eq $z.velicine) { throw "zapis nima polja 'velicine': $Path" }
    if ($null -eq $z.odtis)    { throw "zapis nima polja 'odtis': $Path" }
    return $z
}

# PSCustomObject (tak pride iz ConvertFrom-Json) -> urejen slovar ime -> vrednost.
# Windows PowerShell 5.1 nima ConvertFrom-Json -AsHashtable, zato rocno.
function ConvertTo-Slovar {
    param($Obj)
    $h = [ordered]@{}
    if ($null -eq $Obj) { return $h }
    foreach ($p in $Obj.PSObject.Properties) { $h[$p.Name] = $p.Value }
    return $h
}

function Get-Mediana {
    param([double[]]$Values)
    if ($null -eq $Values -or $Values.Count -eq 0) { return [double]::NaN }
    $s = @($Values | Sort-Object)
    $n = $s.Count
    if ($n % 2 -eq 1) { return [double]$s[[int](($n - 1) / 2)] }
    return ([double]$s[$n / 2 - 1] + [double]$s[$n / 2]) / 2.0
}

# Sum ene velicine cez ponovitve. Relativni razpon je razpon / |mediana|; ce je mediana
# nic, razmerje ne obstaja in se namenoma ne izracuna nadomestka - 'n/a' pove resnico,
# izmisljena stevilka pa ne.
function Get-Sum {
    param([double[]]$Values)
    $n = @($Values).Count
    $min = ($Values | Measure-Object -Minimum).Minimum
    $max = ($Values | Measure-Object -Maximum).Maximum
    $med = Get-Mediana $Values
    $razpon = [double]$max - [double]$min
    $rel = [double]::NaN
    if ($med -ne 0) { $rel = [Math]::Abs($razpon / $med) }
    return [pscustomobject]@{
        N       = $n
        Min     = [double]$min
        Max     = [double]$max
        Mediana = $med
        Razpon  = $razpon
        Rel     = $rel
    }
}
