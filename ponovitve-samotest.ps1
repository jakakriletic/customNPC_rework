# M2.5c - samotest protokola ponovitev.
#
# Zakaj obstaja: .\ponovitve-run.ps1 je edino mesto, kjer se iz vec zagonov naredi ena
# trditev ("razpon te velicine je X"). Ce se ta racun tiho pokvari, se to ne pozna kot
# napaka, ampak kot napacna stevilka v dokumentaciji - najdrazja vrsta napake v tem
# projektu. Merilna skripta za preizkus potrebuje Minecraft in petnajst minut, zato tu
# namesto nje tece ponarejeni scenarij, ki zapise vnaprej znane stevilke.
#
# Kaj preverja (vsak primer je ena trditev protokola):
#   A  tri ponovitve z enakim odtisom -> T1-T6 zelena, izhodna koda 0
#   B  velicina, ki se med ponovitvami premakne cez prag -> oznaka SUMNA
#   C  velicina, ki se ne premakne -> oznaka 'enaka', razpon 0
#   D  spremenjen odtis (kot ob pozabljenem resetu sveta) -> T4 pade, koda 1
#   E  dve ponovitvi -> T1 pade, koda 1
#   F  padel zagon (izhodna koda 1) -> T2 pade, koda 1
#   G  manjkajoca velicina v enem zagonu -> T5 pade, koda 1
#
# Zagon:  .\ponovitve-samotest.ps1
# Ne potrebuje Minecrafta, sveta ne resetira in v audit\ nic ne zapise.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

$psExe = (Get-Process -Id $PID).Path
if ([string]::IsNullOrWhiteSpace($psExe)) { $psExe = 'powershell.exe' }

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("m25c-samotest-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

$failures = @()
function Trditev([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host "  OK       $What" }
    else     { Write-Host "  PADLO    $What"; $script:failures += $What }
}

# Ponarejeni scenarij: vsak zagon prebere svojo zaporedno stevilko iz stevca v mapi in
# zapise vrednosti iz tabele, ki mu jo poda samotest. Tako je vsaka ponovitev drugacna
# na tocno tak nacin, kot ga primer hoce preizkusiti.
$fakeBody = @'
param([string]$JsonPath)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'meritve-lib.ps1')

# Nacrt pride po okoljski spremenljivki, ne po argumentu: argumenti merilnih skript so
# del pogodbe s protokolom in samotest je ne sme siriti samo zase.
$nacrtPot = $env:M25C_NACRT
if ([string]::IsNullOrWhiteSpace($nacrtPot)) { throw 'M25C_NACRT ni nastavljen.' }

$stanje = Join-Path (Split-Path -Parent $nacrtPot) 'stevec.txt'
$n = 0
if (Test-Path $stanje) { $n = [int](Get-Content $stanje -Raw).Trim() }
$n = $n + 1
Set-Content -Path $stanje -Value $n -Encoding ASCII

$nacrt = (Get-Content $nacrtPot -Raw | ConvertFrom-Json)
$tek   = $nacrt.zagoni[$n - 1]

$odtis = @{}
foreach ($p in $tek.odtis.PSObject.Properties)    { $odtis[$p.Name] = $p.Value }
$vel = @{}
foreach ($p in $tek.velicine.PSObject.Properties) { $vel[$p.Name] = [double]$p.Value }

$null = Write-MeritevJson -Path $JsonPath -Paket $nacrt.paket -Scenarij 'samotest' `
                          -Odtis $odtis -Velicine $vel -Uspeh ([bool]$tek.uspeh)
Write-Host ("ponarejeni zagon {0}: {1}" -f $n, $JsonPath)
exit ([int]$tek.koda)
'@

$fake = Join-Path $root 'ponovitve-samotest-scenarij.ps1'
Set-Content -Path $fake -Value $fakeBody -Encoding UTF8

# Pozene ponovitve-run.ps1 nad ponarejenim scenarijem in vrne (koda, izpis).
function Invoke-Protokol {
    param([object[]]$Zagoni, [int]$Ponovitev = 3, [double]$PragSum = 0.20, [string]$Paket = 'M2.7')

    $primer = Join-Path $tmp ("primer-" + [Guid]::NewGuid().ToString('N').Substring(0, 6))
    New-Item -ItemType Directory -Force -Path $primer | Out-Null
    $nacrt = Join-Path $primer 'nacrt.json'
    $json = (@{ paket = $Paket; zagoni = $Zagoni } | ConvertTo-Json -Depth 8)
    [System.IO.File]::WriteAllText($nacrt, $json, (New-Object System.Text.UTF8Encoding($false)))

    $out = Join-Path $primer 'izpis.txt'
    $argumenti = @('-NoProfile', '-ExecutionPolicy', 'Bypass',
                   '-File', ('"{0}"' -f (Join-Path $root 'ponovitve-run.ps1')),
                   '-Skripta', ('"{0}"' -f $fake),
                   '-Paket', $Paket,
                   '-IzhodnaMapa', ('"{0}"' -f $primer),
                   '-Ponovitev', $Ponovitev,
                   '-PragSum', $PragSum,
                   '-BrezResetaSveta',
                   '-Vztrajaj')
    $env:M25C_NACRT = $nacrt
    $p = Start-Process -FilePath $psExe -ArgumentList $argumenti -PassThru -Wait -NoNewWindow `
                       -RedirectStandardOutput $out -RedirectStandardError ($out + '.err')
    $txt = ''
    if (Test-Path $out) { $txt = (Get-Content $out -Raw) }
    return [pscustomobject]@{ Koda = $p.ExitCode; Izpis = $txt; Mapa = $primer }
}

function Zagon {
    param([hashtable]$Odtis, [hashtable]$Velicine, [int]$Koda = 0, [bool]$Uspeh = $true)
    return @{ odtis = $Odtis; velicine = $Velicine; koda = $Koda; uspeh = $Uspeh }
}

try {
    $odtisA = @{ npc = 16; vrataX = -10; zidZ = 72 }

    Write-Host ''
    Write-Host '===== A, B, C : tri ponovitve, stabilna in sumna velicina ====='
    # 'mirna' se ne premakne; 'majhna' se premakne za 2 % (pod pragom); 'divja' za 100 %.
    $r = Invoke-Protokol @(
        (Zagon $odtisA @{ mirna = 5;   majhna = 100; divja = 10 }),
        (Zagon $odtisA @{ mirna = 5;   majhna = 101; divja = 20 }),
        (Zagon $odtisA @{ mirna = 5;   majhna = 102; divja = 30 })
    )
    Trditev ("A: tri skladne ponovitve koncajo z 0 (koda {0})" -f $r.Koda) ($r.Koda -eq 0)
    Trditev 'A: T1 zelena' ($r.Izpis -match 'OK\s+T1:')
    Trditev 'A: T4 zelena (odtis enak)' ($r.Izpis -match 'OK\s+T4:')
    Trditev 'A: T5 zelena (vse velicine povsod)' ($r.Izpis -match 'OK\s+T5:')
    Trditev 'C: nespremenjena velicina je oznacena kot enaka' ($r.Izpis -match '(?m)^mirna\s.*\senaka\s*$')
    Trditev 'B: 2 % premika je se stabilno' ($r.Izpis -match '(?m)^majhna\s.*\sstabilna\s*$')
    Trditev 'B: 100 % premika je SUMNA' ($r.Izpis -match '(?m)^divja\s.*\sSUMNA\s*$')
    Trditev 'B: porocilo nasteje sumno velicino' `
        ((@(Get-ChildItem -Path $r.Mapa -Filter 'm25c-*.md') | ForEach-Object { Get-Content $_.FullName -Raw }) -match 'divja')

    Write-Host ''
    Write-Host '===== D : spremenjen odtis (pozabljen reset sveta) ====='
    # Drugi zagon ima 32 NPC-jev namesto 16 - natanko to se zgodi, ce svet ni resetiran.
    $r = Invoke-Protokol @(
        (Zagon $odtisA @{ mirna = 5 }),
        (Zagon @{ npc = 32; vrataX = -10; zidZ = 72 } @{ mirna = 5 }),
        (Zagon $odtisA @{ mirna = 5 })
    )
    Trditev ("D: razlicen odtis pomeni neuspeh (koda {0})" -f $r.Koda) ($r.Koda -eq 1)
    Trditev 'D: T4 pade in imenuje kljuc' ($r.Izpis -match "PADLO\s+T4:.*npc je '32'")

    Write-Host ''
    Write-Host '===== E : premalo ponovitev ====='
    $r = Invoke-Protokol -Ponovitev 2 -Zagoni @(
        (Zagon $odtisA @{ mirna = 5 }),
        (Zagon $odtisA @{ mirna = 5 })
    )
    Trditev ("E: dve ponovitvi pomenita neuspeh (koda {0})" -f $r.Koda) ($r.Koda -eq 1)
    Trditev 'E: T1 pade' ($r.Izpis -match 'PADLO\s+T1:')

    Write-Host ''
    Write-Host '===== F : padel zagon ====='
    $r = Invoke-Protokol @(
        (Zagon $odtisA @{ mirna = 5 }),
        (Zagon $odtisA @{ mirna = 5 } 1 $false),
        (Zagon $odtisA @{ mirna = 5 })
    )
    Trditev ("F: padel zagon pomeni neuspeh (koda {0})" -f $r.Koda) ($r.Koda -eq 1)
    Trditev 'F: T2 pade in imenuje ponovitev' ($r.Izpis -match 'PADLO\s+T2: ponovitev 2')

    Write-Host ''
    Write-Host '===== G : manjkajoca velicina ====='
    $r = Invoke-Protokol @(
        (Zagon $odtisA @{ mirna = 5; divja = 10 }),
        (Zagon $odtisA @{ mirna = 5 }),
        (Zagon $odtisA @{ mirna = 5; divja = 10 })
    )
    Trditev ("G: manjkajoca velicina pomeni neuspeh (koda {0})" -f $r.Koda) ($r.Koda -eq 1)
    Trditev 'G: T5 pade in imenuje velicino' ($r.Izpis -match 'PADLO\s+T5:.*divja \(2/3\)')

    Write-Host ''
    if ($failures.Count -eq 0) {
        Write-Host ("M2.5c samotest USPESNO: {0} trditev, protokol steje pravilno." -f 16)
        exit 0
    }
    Write-Host 'M2.5c samotest NEUSPESNO. Padle trditve:'
    $failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
finally {
    Remove-Item -Path $fake -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
