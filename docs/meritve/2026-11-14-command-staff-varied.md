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
Vhod pokliče isto obdelavo klikov kot Forge MouseEvent.

**Meje.** Preizkus palice je singleplayer z naloženim ravnim svetom in štirimi talnimi NPC-ji.
Velike skupine, zahteven teren in migracija produkcijskih svetov ostajajo nepreverjeni.
