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

# --- Zascita zagona (M2.6b, 5. 10.) ---------------------------------------------------
#
# 27. 9. je baseline M2.6 padel sredi meritve: server je ob prvem udarcu v celici boj-50
# vrgel NoClassDefFoundError za razred iz originalnega jarja, ob ustavitvi se za
# SquadManager. Ob 12:31 je v isti mapi stekel se drugi baseline zagon; skripte tega niso
# preprecile. Spodnje funkcije poskrbijo, da (1) v isti mapi tece samo en scenarij,
# (2) meritev pade, ce med njo tece tuj gradle build ali Minecraft, in (3) sesut server
# takoj ustavi cakanje z vzrokom, namesto da preverbe berejo star odgovor.

# Izkljucen zaklep za vec ur trajajoc scenarij. Vrne odprt FileStream; dokler je odprt, ga
# drug proces ne more odpreti. Ob koncu procesa (tudi ob padcu ali kill) ga sprosti OS.
function Enter-ScenarijZaklep {
    param([Parameter(Mandatory = $true)][string]$Pot, [Parameter(Mandatory = $true)][string]$Kdo)
    $dir = Split-Path -Parent $Pot
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    try {
        $fs = [System.IO.File]::Open($Pot, 'OpenOrCreate', 'ReadWrite', 'None')
    } catch {
        throw ("V tej mapi ze tece drug scenarij (zaklep {0} je zaseden). Pocakaj, da se konca, ali ga ustavi; dva zagona hkrati si delita dev\run in jar." -f $Pot)
    }
    $txt = [System.Text.Encoding]::ASCII.GetBytes(("{0} pid={1} {2}`r`n" -f $Kdo, $PID, (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')))
    $fs.SetLength(0); $fs.Write($txt, 0, $txt.Length); $fs.Flush()
    return $fs
}

# $true, ce zaklep trenutno drzi drug proces (za skripte, ki zaklepa ne vzamejo, a ne smejo
# teci med scenarijem, npr. testworld.ps1).
function Test-ScenarijZaklep {
    param([Parameter(Mandatory = $true)][string]$Pot)
    if (-not (Test-Path $Pot)) { return $false }
    try { $fs = [System.IO.File]::Open($Pot, 'Open', 'ReadWrite', 'None'); $fs.Close(); return $false }
    catch { return $true }
}

# Ukazna vrstica, po kateri je java proces gradle build ali Minecraft (server, klient, dev).
$script:TujiJavaVzorec = 'GradleDaemon|GradleWrapperMain|GradleMain|GradleStart|launchwrapper|net\.minecraft'

# PID-i procesa $Koren in vseh njegovih potomcev.
function Get-DrevoProcesov {
    param([int[]]$Koren)
    $vsi = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Select-Object ProcessId, ParentProcessId)
    $drevo = @{}
    $vrsta = New-Object System.Collections.Queue
    foreach ($k in $Koren) { if (-not $drevo.ContainsKey($k)) { $drevo[$k] = $true; $vrsta.Enqueue($k) } }
    while ($vrsta.Count -gt 0) {
        $x = $vrsta.Dequeue()
        foreach ($p in $vsi) {
            $id = [int]$p.ProcessId
            if (([int]$p.ParentProcessId -eq $x) -and -not $drevo.ContainsKey($id)) { $drevo[$id] = $true; $vrsta.Enqueue($id) }
        }
    }
    return $drevo
}

# Tuji java procesi (gradle build ali Minecraft), ki niso v $Nasi (slovar PID -> $true).
# Vrne seznam nizov 'pid: zacetek ukazne vrstice'.
function Get-TujiJava {
    param([hashtable]$Nasi = @{})
    $out = @()
    foreach ($p in @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue)) {
        if ($Nasi.ContainsKey([int]$p.ProcessId)) { continue }
        $cmd = "$($p.CommandLine)"
        if ($cmd -notmatch $script:TujiJavaVzorec) { continue }
        $out += ('{0}: {1}' -f $p.ProcessId, $cmd.Substring(0, [Math]::Min(160, $cmd.Length)))
    }
    return $out
}

# Opis sesutja serverja iz loga ('' ce ga ni): vrstica s crash reportom in prvi vzrok.
function Get-ServerSesutje {
    param([string]$LogText)
    if ([string]::IsNullOrEmpty($LogText)) { return '' }
    $m = [regex]::Match($LogText, 'Encountered an unexpected exception|---- Minecraft Crash Report ----|This crash report has been saved to:')
    if (-not $m.Success) { return '' }
    $rep = [regex]::Match($LogText, 'This crash report has been saved to: (\S+)')
    $vzrok = [regex]::Match($LogText.Substring($m.Index), 'Caused by: ([^\r\n]+)')
    $opis = if ($vzrok.Success) { $vzrok.Groups[1].Value.Trim() } else { $m.Value }
    if ($rep.Success) { $opis += (' (crash report: {0})' -f $rep.Groups[1].Value) }
    return $opis
}
