<#
    fixture-run.ps1 - M0.7: samodejna izdelava quest in dialog fixtura (vrstici IC1, IC2).

    Zakaj skripta in ne GUI: 'noppes quest/dialog' zna samo start, finish in reload, ne
    create. NBT formata ne pisemo na roko, ker to protokol prepoveduje. Zato objekta
    ustvari MOD SAM prek svojega scripting API-ja (IQuestCategory.create(), Quest.save()),
    skripta pa samo pozene server, spawna krmilnika in preveri izid. Datoteka, ki nastane,
    gre skozi isto save pot kot tista iz GUI-ja.

    Trije zagoni, razlog je isti kot pri M0.5:
      A  pribije world spawn na 0 4 0, da so chunki pri 0,0 zajamceno nalozeni
      B  spawna T_Trader in TW_Control; krmilnik ustvari quest + dialog in ga pripne
      C  restart: fixture se prebere Z DISKA in krmilnik javi 'nov=0' -> round-trip dokaz

    Predpogoj: v dev\testworld\customnpcs\clones\1\TW_Control.json mora biti vlozena
    skripta tw-fixture.js. To naredi seja z
        python3 dev/testworld/vstavi-skripto.py dev/testworld/tw-fixture.js dev/testworld/customnpcs/clones/1/TW_Control.json
    in rezultat commita. Ta skripta samo preveri, da je vlozena.

    Zagon:
        .\fixture-run.ps1                 # svez svet iz semena (priporoceno)
        .\fixture-run.ps1 -KeepWorld      # obdrzi obstojeci svet
        .\fixture-run.ps1 -AcceptEula     # prvic, ce dev\run\eula.txt se ni sprejet
        .\fixture-run.ps1 -NoSeedUpdate   # ne kopiraj rezultata nazaj v dev\testworld
#>
param([switch]$KeepWorld, [switch]$AcceptEula, [switch]$NoSeedUpdate)

$ErrorActionPreference = 'Stop'
$root  = $PSScriptRoot
$run   = Join-Path $root 'dev\run'
$audit = Join-Path $root 'audit'
$seed  = Join-Path $root 'dev\testworld'
New-Item -ItemType Directory -Force -Path $audit | Out-Null

function Step($n, $t) { Write-Host ''; Write-Host "===== $n : $t =====" }

$failures = @()
function Check([string]$What, [bool]$Ok) {
    if ($Ok) { Write-Host ("  OK       {0}" -f $What) }
    else     { Write-Host ("  NAPAKA   {0}" -f $What); $script:failures += $What }
}

function Get-LogText([string]$LogPath) {
    if (-not (Test-Path $LogPath)) { return '' }
    $c = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
    if ($null -eq $c) { return '' }
    return $c
}

function Wait-ForMarker($Srv, [string]$Marker, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ($true) {
        if ((Get-LogText $Srv.Log) -match [regex]::Escape($Marker)) { return $true }
        if ($Srv.Proc.HasExited) {
            Start-Sleep -Milliseconds 500
            if ((Get-LogText $Srv.Log) -match [regex]::Escape($Marker)) { return $true }
            Write-Host ("  ! proces se je koncal pred markerjem '{0}'" -f $Marker)
            return $false
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host ("  ! timeout {0} s pri cakanju na '{1}'" -f $TimeoutSec, $Marker)
            return $false
        }
        Start-Sleep -Seconds 1
    }
}

function Start-DevServer([string]$Tag) {
    $outLog = Join-Path $audit ("m07-fixture-{0}.log" -f $Tag)
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName         = $env:ComSpec
    $psi.Arguments        = "/c gradlew.bat runServer --offline --no-daemon --console=plain > `"$outLog`" 2>&1"
    $psi.WorkingDirectory = Join-Path $root 'dev'
    $psi.UseShellExecute  = $false
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow   = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    return [pscustomobject]@{ Proc = $proc; Log = $outLog }
}

function Send-Command($srv, [string]$Cmd) {
    $srv.Proc.StandardInput.WriteLine($Cmd)
    $srv.Proc.StandardInput.Flush()
    Start-Sleep -Milliseconds 400
}

function Stop-DevServer($srv, [int]$TimeoutSec = 180) {
    Send-Command $srv 'stop'
    if (-not $srv.Proc.WaitForExit($TimeoutSec * 1000)) {
        Write-Host '  ! server se ni ustavil sam; ubijam proces'
        try { $srv.Proc.Kill() } catch { }
        return $false
    }
    return $true
}

# Skripta krmilnika je vir resnice v .js, v clone JSON pa jo vstavi
# dev\testworld\vstavi-skripto.py (M2.2). Ta skripta zato NE minificira nicesar - samo
# preveri, da je vstavljena, in fixture prekopira v svet, tocno kot to dela r1-run.ps1.
# Dve implementaciji minifikacije sta bili 17. 9. vzrok za 'script errored': PowerShell
# razlicica je pobrisala samo cele komentarjske vrstice, zato je komentar na koncu vrstice
# po strnitvi zakomentiral preostanek skripte.
function Install-ControlClone {
    $src = Join-Path $seed 'customnpcs\clones\1\TW_Control.json'
    if (-not (Test-Path $src)) { throw "Manjka $src" }

    $json = Get-Content $src -Raw
    if ($json -match '@@SCRIPT@@') {
        throw ("V $src je se oznaka @@SCRIPT@@. Skripto vstavi z:`n" +
               "  python3 dev/testworld/vstavi-skripto.py dev/testworld/tw-fixture.js $src")
    }
    $m = [regex]::Match($json, '"Script":\s*"(.*?)",\r?\n', [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if (-not $m.Success -or $m.Groups[1].Value.Length -lt 200) {
        throw "V $src ni vlozene skripte (polje Script je prazno ali prekratko)."
    }

    $dst = Join-Path $run 'world\customnpcs\clones\1\TW_Control.json'
    New-Item -ItemType Directory -Force -Path (Split-Path $dst) | Out-Null
    Copy-Item $src -Destination $dst -Force
    Write-Host ("  TW_Control.json kopiran v svet ({0} znakov skripte)" -f $m.Groups[1].Value.Length)
    return $dst
}

# --- 0: priprava ------------------------------------------------------------

Step 0 'Priprava sveta'

if (-not (Test-Path (Join-Path $run 'eula.txt'))) {
    if (-not $AcceptEula) { throw "dev\run\eula.txt ni sprejet. Pozeni '.\fixture-run.ps1 -AcceptEula'." }
    New-Item -ItemType Directory -Force -Path $run | Out-Null
    Set-Content -Path (Join-Path $run 'eula.txt') -Value 'eula=true' -Encoding ASCII
    Write-Host '  EULA zapisana na izrecno zahtevo (-AcceptEula).'
}

if ($KeepWorld) {
    Write-Host '  -KeepWorld: svet ostane, fixture NPC-ji se osvezijo.'
    & (Join-Path $root 'testworld.ps1') -KeepWorld
} else {
    & (Join-Path $root 'testworld.ps1')
}

# Brez command blokov ni izpisa /say in nobenega TW-FIX markerja.
$propsFile = Join-Path $run 'server.properties'
if (Test-Path $propsFile) {
    $props = Get-Content $propsFile -Raw
    if ($props -notmatch '(?m)^enable-command-block\s*=\s*true') {
        if ($props -match '(?m)^enable-command-block\s*=') {
            $props = $props -replace '(?m)^enable-command-block\s*=.*', 'enable-command-block=true'
        } else {
            $props += "`nenable-command-block=true"
        }
        Set-Content -Path $propsFile -Value $props -Encoding ASCII
        Write-Host '  enable-command-block popravljen na true'
    }
}

# Prazna kategorija TW: QuestController/DialogController ob zagonu naredita kategorijo iz
# imena mape. To je edina stvar, ki jo pripravimo mi - in je mapa, ne format.
foreach ($d in @('world\customnpcs\quests\TW', 'world\customnpcs\dialogs\TW')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $run $d) | Out-Null
}
Check 'prazni kategoriji TW sta pripravljeni' (
    (Test-Path (Join-Path $run 'world\customnpcs\quests\TW')) -and
    (Test-Path (Join-Path $run 'world\customnpcs\dialogs\TW')))

$clone = Install-ControlClone
Check 'TW_Control.json z vlozeno skripto je v svetu' (Test-Path $clone)
if ($failures.Count -gt 0) { exit 1 }

# --- A: pribij world spawn --------------------------------------------------

Step A 'Zagon A - world spawn'
$a = Start-DevServer 'a'
Check 'A: server je dosegel "Done ("' (Wait-ForMarker $a 'Done (' 900)
if ($failures.Count -gt 0) { Stop-DevServer $a | Out-Null; exit 1 }
Send-Command $a 'setworldspawn 0 4 0'
Check 'A: world spawn je pribit' (Wait-ForMarker $a 'Set the world spawn point' 60)
Send-Command $a 'save-all flush'
Check 'A: svet je shranjen' (Wait-ForMarker $a 'Saved the world' 120)
Check 'A: server se je ustavil sam' (Stop-DevServer $a)

# --- B: ustvari fixture -----------------------------------------------------

Step B 'Zagon B - krmilnik ustvari quest in dialog'
$b = Start-DevServer 'b'
Check 'B: server je dosegel "Done ("' (Wait-ForMarker $b 'Done (' 900)
if ($failures.Count -gt 0) { Stop-DevServer $b | Out-Null; exit 1 }

Send-Command $b 'noppes clone spawn T_Trader 1 8,4,8'
Start-Sleep -Seconds 2
Send-Command $b 'noppes clone spawn TW_Control 1 0,4,-4'

Check 'F1: krmilnik se je oglasil (TW-FIX-INIT)' (Wait-ForMarker $b 'TW-FIX-INIT' 90)
$logB = Get-LogText $b.Log
Check 'F2: v zagonu B ni TW-FIX-ERROR' ($logB -notmatch 'TW-FIX-ERROR')
if ($logB -match 'TW-FIX-ERROR (.*)') { Write-Host ("  napaka skripte: {0}" -f $Matches[1]) }

Check 'F3: quest je ustvarjen' (Wait-ForMarker $b 'TW-FIX-QUEST' 60)
Check 'F4: dialog je ustvarjen' (Wait-ForMarker $b 'TW-FIX-DIALOG' 60)
Check 'F5: dialog je pripet na T_Trader' (Wait-ForMarker $b 'TW-FIX-ATTACH' 60)
Check 'F6: krmilnik je konal (TW-FIX-DONE)' (Wait-ForMarker $b 'TW-FIX-DONE' 60)

$logB  = Get-LogText $b.Log
$mq    = [regex]::Match($logB, 'TW-FIX-QUEST id=(-?\d+) ime=(\S+) tip=(\d+) nov=(\d)')
$md    = [regex]::Match($logB, 'TW-FIX-DIALOG id=(-?\d+) ime=(\S+) quest=(-?\d+) nov=(\d)')
$ma    = [regex]::Match($logB, 'TW-FIX-ATTACH npc=T_Trader slot=0 dialog=(-?\d+)')
Check 'F3a: quest ima veljaven id in ime TW_Quest' ($mq.Success -and [int]$mq.Groups[1].Value -ge 0 -and $mq.Groups[2].Value -eq 'TW_Quest')
Check 'F3b: quest je tipa 5 (manual)' ($mq.Success -and $mq.Groups[3].Value -eq '5')
Check 'F4a: dialog ima veljaven id in ime TW_Dialog' ($md.Success -and [int]$md.Groups[1].Value -ge 0 -and $md.Groups[2].Value -eq 'TW_Dialog')
Check 'F4b: dialog kaze na ustvarjeni quest' ($mq.Success -and $md.Success -and $md.Groups[3].Value -eq $mq.Groups[1].Value)
Check 'F5a: pripeti dialog je tisti, ki smo ga ustvarili' ($ma.Success -and $md.Success -and $ma.Groups[1].Value -eq $md.Groups[1].Value)
if ($mq.Success) { Write-Host ("  quest id={0} dialog id={1}" -f $mq.Groups[1].Value, $md.Groups[1].Value) }

# T_Trader s pripetim dialogom nazaj v clone shrambo, da fixture ne zivi samo v svetu.
# CmdClone.add primerja args[0] z display imenom in isce 80 blokov okoli posiljatelja
# (konzola = 0,0,0), zato gre ime, ne selektor.
Send-Command $b 'noppes clone add T_Trader 1 T_Trader'
Start-Sleep -Seconds 2
Send-Command $b 'save-all flush'
Check 'B: svet je shranjen' (Wait-ForMarker $b 'Saved the world' 120)
Check 'B: server se je ustavil sam' (Stop-DevServer $b)

$qDir = Join-Path $run 'world\customnpcs\quests\TW'
$dDir = Join-Path $run 'world\customnpcs\dialogs\TW'
$qFiles = @(Get-ChildItem -Path $qDir -Filter '*.json' -ErrorAction SilentlyContinue)
$dFiles = @(Get-ChildItem -Path $dDir -Filter '*.json' -ErrorAction SilentlyContinue)
Check ("F7: quest datoteka je na disku (najdenih {0})" -f $qFiles.Count)  ($qFiles.Count -ge 1)
Check ("F8: dialog datoteka je na disku (najdenih {0})" -f $dFiles.Count) ($dFiles.Count -ge 1)

# --- C: round-trip z diska --------------------------------------------------

Step C 'Zagon C - fixture se prebere z diska'
$c = Start-DevServer 'c'
Check 'C: server je dosegel "Done ("' (Wait-ForMarker $c 'Done (' 900)
if ($failures.Count -gt 0) { Stop-DevServer $c | Out-Null; exit 1 }

Send-Command $c 'noppes clone spawn TW_Control 1 0,4,-6'
Check 'C: krmilnik se je oglasil' (Wait-ForMarker $c 'TW-FIX-INIT' 90)
Check 'C: quest je najden' (Wait-ForMarker $c 'TW-FIX-QUEST' 60)
Check 'C: dialog je najden' (Wait-ForMarker $c 'TW-FIX-DIALOG' 60)

$logC = Get-LogText $c.Log
$cq = [regex]::Match($logC, 'TW-FIX-QUEST id=(-?\d+) ime=(\S+) tip=(\d+) nov=(\d)')
$cd = [regex]::Match($logC, 'TW-FIX-DIALOG id=(-?\d+) ime=(\S+) quest=(-?\d+) nov=(\d)')
Check 'F9: quest je prisel Z DISKA, ne na novo (nov=0)'  ($cq.Success -and $cq.Groups[4].Value -eq '0')
Check 'F10: dialog je prisel Z DISKA, ne na novo (nov=0)' ($cd.Success -and $cd.Groups[4].Value -eq '0')
Check 'F11: quest ima po restartu isti id' ($cq.Success -and $mq.Success -and $cq.Groups[1].Value -eq $mq.Groups[1].Value)
Check 'F12: dialog po restartu se vedno kaze na quest' ($cd.Success -and $cd.Groups[3].Value -eq $cq.Groups[1].Value)
Check 'F13: v zagonu C ni "Error loading"' ($logC -notmatch 'Error loading')
$npcErr = [regex]::Matches($logC, '(?m)^.*(ERROR|Exception).*noppes\..*$')
Check ("F14: v zagonu C ni napak iz noppes.* (najdenih {0})" -f $npcErr.Count) ($npcErr.Count -eq 0)
Check 'C: server se je ustavil sam' (Stop-DevServer $c)

# --- D: prenos v seme -------------------------------------------------------

Step D 'Prenos fixtura v dev\testworld'
if ($NoSeedUpdate) {
    Write-Host '  -NoSeedUpdate: seme ostane nespremenjeno.'
} else {
    foreach ($pair in @(@('quests\TW', $qDir), @('dialogs\TW', $dDir))) {
        $dst = Join-Path $seed ('customnpcs\' + $pair[0])
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item (Join-Path $pair[1] '*.json') -Destination $dst -Force
    }
    # Iz na novo shranjenega T_Trader.json prenesemo SAMO blok NPCDialogOptions.
    # Cela datoteka bi v seme prinesla se runtime sum (Motion, OnGround, Air, Fire,
    # trader polja), kar bi tiho spremenilo fixture in s tem primerljivost meritev.
    $srcTrader = Join-Path $run  'world\customnpcs\clones\1\T_Trader.json'
    $dstTrader = Join-Path $seed 'customnpcs\clones\1\T_Trader.json'
    if ((Test-Path $srcTrader) -and (Test-Path $dstTrader)) {
        $sl = [System.Text.RegularExpressions.RegexOptions]::Singleline
        $pat = '"NPCDialogOptions": \[.*?\r?\n    \],\r?\n'
        $blockNew = [regex]::Match((Get-Content $srcTrader -Raw), $pat, $sl)
        $dstText  = Get-Content $dstTrader -Raw
        $blockOld = [regex]::Match($dstText, $pat, $sl)
        if ($blockNew.Success -and $blockOld.Success) {
            if ($blockNew.Value -ne $blockOld.Value) {
                $merged = $dstText.Substring(0, $blockOld.Index) + $blockNew.Value +
                          $dstText.Substring($blockOld.Index + $blockOld.Length)
                [System.IO.File]::WriteAllText($dstTrader, $merged, (New-Object System.Text.UTF8Encoding($false)))
                Write-Host '  T_Trader.json v semenu: blok NPCDialogOptions posodobljen'
            } else {
                Write-Host '  T_Trader.json v semenu je ze pripet na ta dialog'
            }
        } else {
            Write-Host '  ! bloka NPCDialogOptions ni bilo mogoce prebrati; T_Trader.json v semenu ostaja nespremenjen'
        }
    }
    Check 'F17: T_Trader v semenu ima pripet dialog' (
        (Get-Content $dstTrader -Raw) -match '"Title": "TW_Dialog"')
    $sq = @(Get-ChildItem (Join-Path $seed 'customnpcs\quests\TW')  -Filter '*.json' -ErrorAction SilentlyContinue)
    $sd = @(Get-ChildItem (Join-Path $seed 'customnpcs\dialogs\TW') -Filter '*.json' -ErrorAction SilentlyContinue)
    Check ("F15: seme ima quest fixture ({0})" -f $sq.Count)  ($sq.Count -ge 1)
    Check ("F16: seme ima dialog fixture ({0})" -f $sd.Count) ($sd.Count -ge 1)
}

# --- povzetek ---------------------------------------------------------------

Write-Host ''
if ($failures.Count -eq 0) {
    Write-Host 'FIXTURE USPESNO. Merila F1-F17 veljajo.'
    Write-Host 'Vrstici IC1 in IC2 integracijske matrike nista vec blokirani na GUI.'
    Write-Host ("Logi: audit\m07-fixture-a.log, -b.log, -c.log")
    exit 0
}
Write-Host 'FIXTURE NEUSPESNO. Padle preverbe:'
$failures | ForEach-Object { Write-Host "  - $_" }
Write-Host ("Logi: audit\m07-fixture-a.log, -b.log, -c.log")
exit 1
