# M4.16 - branje markerjev LET-* iz loga. Skupno za .\letenje-run.ps1 (zagon v svetu)
# in .\letenje-samotest.ps1 (ista skripta krmilnika nad ponarejenim svetom v node), da
# se izpis skripte in branje harnessa ne moreta tiho razici (lekcija M2.3, 17. 9.).
#
# Dot-source:  . (Join-Path $PSScriptRoot 'letenje-lib.ps1')

# Proge v vrstnem redu skripte. D0 je pricakovana povprecna razdalja od izhodisca do
# cilja, izracunana iz letenje-setup-commands.txt (NPC se spawna na sredini bloka).
# Ce se ena stevilka v postavitvi ali skripti spremeni, merilo LT3 pade takoj.
$script:LetProge = @(
    [pscustomobject]@{ Id = 'Z';  Opis = 'leteci, zid cez vso sirino';      D0 = 16.36 }
    [pscustomobject]@{ Id = 'V';  Opis = 'leteci, L-zid (vogal)';           D0 = 12.51 }
    [pscustomobject]@{ Id = 'L';  Opis = 'leteci, zaprt labirint';          D0 = 10.56 }
    [pscustomobject]@{ Id = 'DF'; Opis = 'leteci, prosto 48 blokov';        D0 = 48.12 }
    [pscustomobject]@{ Id = 'DW'; Opis = 'kopenski, prosto 48 blokov';      D0 = 48.12 }
    [pscustomobject]@{ Id = 'P';  Opis = 'leteci, prosto 16 blokov (kontrola)'; D0 = 16.36 }
)
$script:LetFaze = @(
    [pscustomobject]@{ Id = 'A'; Opis = 'navigateTo en klic, hitrost 1.0';      Hitrost = 1.0 }
    [pscustomobject]@{ Id = 'B'; Opis = 'navigateTo osvezen vsakih 20 tickov';  Hitrost = 1.0 }
    [pscustomobject]@{ Id = 'C'; Opis = 'navigateTo en klic, hitrost 0.5';      Hitrost = 0.5 }
)
$script:LetNaProgo = 6
# 36 merjenih + 6 tarc; krmilnik se ob preverbi chunkov se ni spawnan.
$script:LetNpcSkupaj = 42
$script:LetTrajanjeFaze = 600
$script:LetToleranca = 0.30

# Skripta gre v polje Script minificirana enako kot vstavi-skripto.py (brez komentarjev,
# enojni narekovaji, ena vrstica). Skripte ne smejo imeti // ali " znotraj niza.
function Get-LetMinifiedScript([string]$JsPath) {
    $source = [IO.File]::ReadAllText($JsPath)
    $mini = [regex]::Replace($source, '//[^\n]*', '')
    return [regex]::Replace($mini.Replace('"', "'"), '\s+', ' ').Trim()
}

# Vstavi minificirano skripto v kopijo LET_Control.json. Vir je vedno
# dev\testworld\letenje-control.js, zato se fixture in skripta ne moreta razici.
#
# M4.10 (Q78): -TrajanjeFaze (server ticki, veckratnik 20) zamenja v minificirani skripti
# PHASE_LEN, zacetke faz B in C ter SUM_AT (razmik 4 skriptne ticke ostane). Pri 600 se
# skripta ne spremeni (merilni pogoj M4.16).
function Install-LetController([string]$CloneJson, [string]$JsPath, [string]$Destination, [int]$TrajanjeFaze = 600) {
    $mini = Get-LetMinifiedScript $JsPath
    if ($TrajanjeFaze -ne 600) {
        if ($TrajanjeFaze % 20 -ne 0) { throw 'TrajanjeFaze mora biti veckratnik 20 server tickov.' }
        $len = [int]($TrajanjeFaze / 10)
        $zamenjave = @(
            @('var PHASE_LEN = 60;', ("var PHASE_LEN = {0};" -f $len)),
            @('start: 70,', ("start: {0}," -f (6 + ($len + 4)))),
            @('start: 134,', ("start: {0}," -f (6 + 2 * ($len + 4)))),
            @('var SUM_AT = 198;', ("var SUM_AT = {0};" -f (6 + 3 * ($len + 4))))
        )
        foreach ($z in $zamenjave) {
            $n = [regex]::Matches($mini, [regex]::Escape($z[0])).Count
            if ($n -ne 1) { throw ("Skripta krmilnika: '{0}' se pojavi {1}-krat, pricakovano 1." -f $z[0], $n) }
            $mini = $mini.Replace($z[0], $z[1])
        }
    }
    $encoded = $mini.Replace('\', '\\').Replace('"', '\"')
    $clone = [IO.File]::ReadAllText($CloneJson)
    $pattern = '"Script"\s*:\s*"(?:\\.|[^"\\])*"'
    if ([regex]::Matches($clone, $pattern).Count -ne 1) { throw 'LET_Control mora imeti natanko eno skripto.' }
    $clone = [regex]::Replace($clone, $pattern, [Text.RegularExpressions.MatchEvaluator]{ param($m) '"Script": "' + $encoded + '"' })
    [IO.File]::WriteAllText($Destination, $clone, (New-Object Text.UTF8Encoding $false))
    return $mini.Length
}

$script:LetInv = [Globalization.CultureInfo]::InvariantCulture
function ConvertTo-LetDouble([string]$s) { return [double]::Parse($s, $script:LetInv) }

# Vsak /say iz skripte gre v log dvakrat (konzola in LogWriter, M0.7 dnevnik 26); stejemo
# samo konzolni kanal.
function Get-LetMarkerText([string]$Raw) {
    if ([string]::IsNullOrEmpty($Raw)) { return '' }
    return (($Raw -split "`n" | Where-Object { $_ -notmatch '\[FINE/CustomNPCs\]' }) -join "`n")
}

# 'LET-SETUP Z=6/da/16.36 V=6/da/12.51 ...' -> @{ Z = {N; Cilj; D0}; ... }; $null ce ga ni
function Read-LetSetup([string]$Text) {
    $m = [regex]::Match($Text, 'LET-SETUP((?: \w+=\d+/\w+/-?[\d.]+)+)')
    if (-not $m.Success) { return $null }
    $out = @{}
    foreach ($p in [regex]::Matches($m.Groups[1].Value, '(\w+)=(\d+)/(\w+)/(-?[\d.]+)')) {
        $out[$p.Groups[1].Value] = [pscustomobject]@{
            N = [int]$p.Groups[2].Value; Cilj = $p.Groups[3].Value; D0 = (ConvertTo-LetDouble $p.Groups[4].Value)
        }
    }
    return $out
}

# Kljuc 'A.Z' -> zacetek faze na progi.
function Read-LetStart([string]$Text) {
    $out = @{}
    $re = 'LET-START faza=(\w) proga=(\w+) N=(\d+) hitrost=([\d.]+) doCilja0=(-?[\d.]+) navig0=(\d+) cele0=(\d+)'
    foreach ($m in [regex]::Matches($Text, $re)) {
        $out[$m.Groups[1].Value + '.' + $m.Groups[2].Value] = [pscustomobject]@{
            N = [int]$m.Groups[3].Value; Hitrost = (ConvertTo-LetDouble $m.Groups[4].Value)
            D0 = (ConvertTo-LetDouble $m.Groups[5].Value)
            Navig0 = [int]$m.Groups[6].Value; Cele0 = [int]$m.Groups[7].Value
        }
    }
    return $out
}

# Vzorci ene faze in proge, po vrsti.
function Read-LetSamples([string]$Text, [string]$Faza, [string]$Proga) {
    $re = 'LET-S faza=' + $Faza + ' tick=(\d+) proga=' + $Proga + ' navig=(\d+)/(\d+) cele=(\d+)/\d+' +
          ' prispeli=(\d+)/\d+ doCiljaMin=(-?[\d.]+) doCiljaPovp=(-?[\d.]+) prevozenoMax=(-?[\d.]+)' +
          ' yMax=(-?[\d.]+) zastoj=(\d+)/\d+ dStarost=(-?\d+)/(-?\d+) gib=([\d.]+)/([\d.]+)'
    $out = @()
    foreach ($m in [regex]::Matches($Text, $re)) {
        $out += [pscustomobject]@{
            Tick = [int]$m.Groups[1].Value; Navig = [int]$m.Groups[2].Value; N = [int]$m.Groups[3].Value
            Cele = [int]$m.Groups[4].Value; Prispeli = [int]$m.Groups[5].Value
            DoCiljaMin = (ConvertTo-LetDouble $m.Groups[6].Value); DoCiljaPovp = (ConvertTo-LetDouble $m.Groups[7].Value)
            PrevozenoMax = (ConvertTo-LetDouble $m.Groups[8].Value); YMax = (ConvertTo-LetDouble $m.Groups[9].Value)
            Zastoj = [int]$m.Groups[10].Value
            DStarostMin = [int]$m.Groups[11].Value; DStarostMax = [int]$m.Groups[12].Value
            GibPovp = (ConvertTo-LetDouble $m.Groups[13].Value); GibMax = (ConvertTo-LetDouble $m.Groups[14].Value)
        }
    }
    return $out
}

# Kljuc 'A.Z' -> povzetek faze na progi.
function Read-LetSummary([string]$Text) {
    $out = @{}
    $re = 'LET-K faza=(\w) proga=(\w+) N=(\d+) prispeli=(\d+) casMed=(-?\d+) casMax=(-?\d+) konecNav=(\d+)' +
          ' drsenjeMax=(-?[\d.]+) drsenjeMed=(-?[\d.]+) hitrostN=(\d+) hitrost=(-?[\d.]+)' +
          ' doCiljaKonec=(-?[\d.]+) doCiljaKonecMin=(-?[\d.]+)'
    foreach ($m in [regex]::Matches($Text, $re)) {
        $out[$m.Groups[1].Value + '.' + $m.Groups[2].Value] = [pscustomobject]@{
            N = [int]$m.Groups[3].Value; Prispeli = [int]$m.Groups[4].Value
            CasMed = [int]$m.Groups[5].Value; CasMax = [int]$m.Groups[6].Value
            KonecNav = [int]$m.Groups[7].Value
            DrsenjeMax = (ConvertTo-LetDouble $m.Groups[8].Value); DrsenjeMed = (ConvertTo-LetDouble $m.Groups[9].Value)
            HitrostN = [int]$m.Groups[10].Value; Hitrost = (ConvertTo-LetDouble $m.Groups[11].Value)
            DoCiljaKonec = (ConvertTo-LetDouble $m.Groups[12].Value); DoCiljaKonecMin = (ConvertTo-LetDouble $m.Groups[13].Value)
        }
    }
    return $out
}

# Merila LT3, LT5-LT8 nad besedilom markerjev. Vrne seznam @{ Ime; Ok }; za samotest
# in zagon v svetu enako. LT1/LT2/LT4/LT9 so lastnost zagona in jih preveri letenje-run.ps1.
function Test-LetMerila([string]$Text) {
    $r = @()
    $setup = Read-LetSetup $Text
    $r += [pscustomobject]@{ Ime = 'LT3: vrstica LET-SETUP je berljiva'; Ok = ($null -ne $setup) }
    foreach ($p in $script:LetProge) {
        $s = if ($null -ne $setup) { $setup[$p.Id] } else { $null }
        $ok = ($null -ne $s) -and ($s.N -eq $script:LetNaProgo) -and ($s.Cilj -eq 'da') -and
              ([Math]::Abs($s.D0 - $p.D0) -le $script:LetToleranca)
        $opis = if ($null -ne $s) { "N={0} cilj={1} d0={2:N2}" -f $s.N, $s.Cilj, $s.D0 } else { 'manjka' }
        $r += [pscustomobject]@{ Ime = ("LT3: proga {0} je polna, cilj najden, geometrija d0={1:N2}+-{2} ({3})" -f $p.Id, $p.D0, $script:LetToleranca, $opis); Ok = $ok }
    }
    $start = Read-LetStart $Text
    $sum = Read-LetSummary $Text
    $minVzorcev = [int]($script:LetTrajanjeFaze / 20) - 5
    foreach ($f in $script:LetFaze) {
        $r += [pscustomobject]@{ Ime = ("LT1: faza {0} se je zacela in koncala" -f $f.Id); Ok = ($Text.Contains("LET-$($f.Id)-START") -and $Text.Contains("LET-$($f.Id)-END")) }
        foreach ($p in $script:LetProge) {
            $k = "$($f.Id).$($p.Id)"
            $sm = @(Read-LetSamples $Text $f.Id $p.Id)
            $r += [pscustomobject]@{ Ime = ("LT5: {0} ima vsaj {1} vzorcev (ima {2})" -f $k, $minVzorcev, $sm.Count); Ok = ($sm.Count -ge $minVzorcev) }
            $slabi = @()
            for ($i = 1; $i -lt $sm.Count; $i++) {
                if ($sm[$i].DStarostMin -lt 15 -or $sm[$i].DStarostMax -gt 25) { $slabi += $sm[$i].Tick }
            }
            $r += [pscustomobject]@{ Ime = ("LT6: {0} se tika (dStarost 15-25, odstopanj {1})" -f $k, $slabi.Count); Ok = ($sm.Count -ge 2 -and $slabi.Count -eq 0) }
            $s0 = $start[$k]
            $ok7 = ($null -ne $s0) -and ($null -ne $setup) -and ($null -ne $setup[$p.Id]) -and
                   ($s0.N -eq $script:LetNaProgo) -and ([Math]::Abs($s0.Hitrost - $f.Hitrost) -lt 0.001) -and
                   ([Math]::Abs($s0.D0 - $setup[$p.Id].D0) -le $script:LetToleranca)
            $o7 = if ($null -ne $s0) { "d0={0:N2} hitrost={1}" -f $s0.D0, $s0.Hitrost } else { 'manjka LET-START' }
            $r += [pscustomobject]@{ Ime = ("LT7: {0} se zacne na izhodiscu s pravo hitrostjo ({1})" -f $k, $o7); Ok = $ok7 }
            $r += [pscustomobject]@{ Ime = ("LT1: {0} ima povzetek LET-K" -f $k); Ok = ($null -ne $sum[$k] -and $sum[$k].N -eq $script:LetNaProgo) }
        }
    }
    # LT8: kontrola letenja (kot M2.3 L4). Ce pade, proge ne govorijo o oviri ali dometu,
    # ampak o tem, da letenje ne dela niti po prazni ravnini.
    $bp = @(Read-LetSamples $Text 'B' 'P')
    $maxP = 0.0; $minP = 1e9
    foreach ($s in $bp) {
        if ($s.PrevozenoMax -gt $maxP) { $maxP = $s.PrevozenoMax }
        if ($s.DoCiljaMin -lt $minP) { $minP = $s.DoCiljaMin }
    }
    $r += [pscustomobject]@{ Ime = ("LT8: kontrola P v fazi B prevozi >= 12 blokov (max {0:N2}) in pride na <= 3 bloke (min {1:N2})" -f $maxP, $minP); Ok = ($bp.Count -gt 0 -and $maxP -ge 12 -and $minP -le 3) }
    $r += [pscustomobject]@{ Ime = 'LT1: scenarij je prisel do LET-SUM'; Ok = ($Text -match 'LET-SUM Z=6 V=6 L=6 DF=6 DW=6 P=6') }
    return $r
}

# Velicine za zapis zagona (meritve-lib.ps1, protokol ponovitev M2.5c). Ravna imena
# '<faza>.<proga>.<velicina>'. Cas je cenzuriran s trajanjem faze; drsenje je 0, kadar
# nobena navigacija ni koncala (konecNav=0 to pove).
function Get-LetVelicine([string]$Text) {
    $v = @{}
    $start = Read-LetStart $Text
    $sum = Read-LetSummary $Text
    foreach ($f in $script:LetFaze) {
        foreach ($p in $script:LetProge) {
            $k = "$($f.Id).$($p.Id)"
            if ($null -ne $start[$k]) {
                $v["$k.navig0"] = $start[$k].Navig0
                $v["$k.cele0"] = $start[$k].Cele0
            }
            $s = $sum[$k]
            if ($null -eq $s) { continue }
            $v["$k.prispeli"] = $s.Prispeli
            $v["$k.casMed"] = $s.CasMed
            $v["$k.casMax"] = $s.CasMax
            $v["$k.konecNav"] = $s.KonecNav
            $v["$k.drsenjeMax"] = $s.DrsenjeMax
            $v["$k.drsenjeMed"] = $s.DrsenjeMed
            $v["$k.hitrost"] = $s.Hitrost
            $v["$k.doCiljaKonec"] = $s.DoCiljaKonec
        }
    }
    return $v
}

# Tabela za porocilo: ena vrstica na fazo in progo.
function Format-LetTabela([string]$Text) {
    $start = Read-LetStart $Text
    $sum = Read-LetSummary $Text
    $lines = @()
    $lines += '| faza | proga | navig0 | cele0 | prispeli | cas med/max | konecNav | drsenje max/med | hitrost (b/t) | do cilja konec |'
    $lines += '|---|---|---|---|---|---|---|---|---|---|'
    foreach ($f in $script:LetFaze) {
        foreach ($p in $script:LetProge) {
            $k = "$($f.Id).$($p.Id)"
            $a = $start[$k]; $s = $sum[$k]
            if ($null -eq $a -or $null -eq $s) { $lines += "| $($f.Id) | $($p.Id) | manjka | | | | | | | |"; continue }
            $lines += ('| {0} | {1} | {2}/6 | {3}/6 | {4}/6 | {5}/{6} | {7} | {8:N3}/{9:N3} | {10:N4} | {11:N2} |' -f `
                $f.Id, $p.Id, $a.Navig0, $a.Cele0, $s.Prispeli, $s.CasMed, $s.CasMax, $s.KonecNav,
                $s.DrsenjeMax, $s.DrsenjeMed, $s.Hitrost, $s.DoCiljaKonec)
        }
    }
    return $lines
}
