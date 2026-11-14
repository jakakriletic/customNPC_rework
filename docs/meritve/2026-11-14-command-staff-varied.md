# Command Staff in Varied Commodities — 14. 11. 2026

**Paket.** `bundle-varied.ps1` doda vse razrede in vire iz ene od dveh izvirnih izdaj
Varied Commodities (21Nov19, 31Mar23). Forge dobi obe deklaraciji moda in sam zgradi
predpomnilnik. ID-ji, formati shranjenih predmetov in bloki ostanejo pod `variedcommodities`.
JAR vsebuje avtorstvo, licenco CC BY-NC 3.0 in izvorno zgoščeno vrednost. Binarka nastane lokalno.

**Preverba paketa.** Forge 14.23.5.2860 dedicated z eno datoteko v `mods/`: oba moda sta se
naložila pri obeh izdajah, postavljen je bil `variedcommodities:crate`, na tleh sta bila najdena
`variedcommodities:ancient_coin` in `customnpcs:command_staff` (`varied-bundle-2026-11-14-150232`
in `varied-bundle-2026-11-14-150241`). Build, unit in primerjava z izvirnim JAR-om so zeleni.

**Palica.** `command-staff-run.ps1` je preveril izbor štirih izmed petih NPC-jev, menijske
načine, sledenje, držanje tal proti oddaljenemu nasprotniku, zasledovanje v Attack, prihod v
ločene položaje, zapis načinov in vrnitev prvotnega AI-ja. Test tudi v obeh reobf paketih na
Forge 2860 (`staff-bundled-2026-11-14-150521`, `staff-bundled2019-2026-11-14-150613`),
vključno z naloženima modeloma žetona in palice.
**Popravek izbora in konjenica.** Dev Forge in kombinirani 31Mar23 JAR na Forge 2860
(`staff-2026-11-14-172307`, `staff-bundled-2026-11-14-selection`) sta preverila izbor po
spustu, štiri pešce v ločenih položajih in pot klona Konjenik. Konjenik se ustvari v sedlu,
krog ga izbere pri vodoravnem pogledu in do cilja pride na konju. Scenarij kliče obdelavo
stanja tipk znotraj Forge klienta. Unit, build in preverba paketa so zeleni.

**Premikanje konjenice po stopnici.** V `staff-cavalry-slope-before` je do zgornjega dela
proge prišlo 3/4 konjenikov. Vanilla je preverjala višino vozlišča glede na sedečega jezdeca,
ki je bil več kot blok nad tlemi. Ukaz za premik in sledenje zdaj načrtuje in sledi poti
s konjevo višino in širino. Konj pred ostrim zavojem upočasni. V dev Forge (`staff-cavalry-slope-fix1`)
in kombiniranem 31Mar23 JAR-u (`staff-cavalry-slope-jar`) so 4/4 prečkali stopnico,
obdržali razmik vsaj 1,4 bloka in ostali na mestu še 45 tickov po prihodu.

**Odrivanje konjenice.** Vanilla pri navadnem trku ne odrine konja, ki nosi jezdeca.
Na strežniku prekrivajoči se hitboxi konj z NPC-jezdeci sprožijo omejen vodoravni potisk.
Pri identičnih koordinatah dobi vsak par nasprotni smeri. To velja tudi za mirujoče
jezdece in pri `RwHitbox=0`. Preizkus `staff-mounted-stack-before` je po 140 tickih
izmeril največ 1,396 bloka prekrivanja pri osmih konjenikih na eni točki. Kombinirani JAR
(`staff-mounted-stack-jar`) je v istem scenariju izmeril 0,000. Vseh osem je ostalo v sedlu,
štirje na progi s stopnico pa so prišli v formacijo in obdržali položaj.

**Meje.** Preizkus je singleplayer v pripravljeni progi. Velike skupine, razgiban teren
na uporabnikovem zemljevidu in migracija produkcijskih svetov ostajajo nepreverjeni.
