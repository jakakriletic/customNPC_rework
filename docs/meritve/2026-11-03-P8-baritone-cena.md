# P8 — cena Baritone pri 2000 NPC-jih (široka mreža, živi heap)

2026-11-03 · seja 206 · poskus 1 · **sprejet kot podatek** na `dbca5966…`. Izhodišče `82b7af8` (P7c).
Paket po Q248/Q92 (privzeto `RwNavBackend=0`, brez cenovne meje) in Q292–Q294. Produkcijska koda ni spremenjena.

## Kaj je novo

- `perf-run.ps1 -MrezaSirina 50` (privzeto 25, vse prejšnje celice nespremenjene): pri N=2000 ima vsaka skupina
  20 vrst, enako globino kot N=1000 pri širini 25. M4.15a je pri 2000 nedosegljivih padla na U1 (881/800 < 1000)
  na obeh straneh, ker je pri širini 25 tarčo sploh lahko imelo le ~800 NPC-jev (vrste izven dosega druge skupine).
  Vzrok je bila geometrija; U1 ostane nespremenjen (Q292).
- `-ZiviHeap`: po oknu `jcmd GC.class_histogram` (JDK 8 pred tem naredi poln GC) → `heap.zivMB` in delež razredov
  `si.ladja.npcbaritone.*` (`heap.npcbMB`); surov histogram ob JSON celice (`-histo.txt`). Samo podatek (Q293).
- `p8-baritone-run.ps1 -Backend 0|1`: svež svet, nedosegljiva + boj pri 2000, 60 s + 120 s, ista knjižnica,
  configi in merila P1–P9/U0–U2/NB1–NB5 kot M4.15a. Po Q92/Q250 ena ponovitev.

## Controller dokazi (oba exit 0, vsa merila OK)

| ID | Konec UTC | Celice |
|---|---|---|
| p8-off-v1 | 07:34:12 | `audit/m24-perf-{nedosegljiva,boj}-2000-2026-11-03-0826.json`, `m24-perf-2026-11-03-0826.md` |
| p8-on-v1 | 07:43:46 | `audit/m24-perf-{nedosegljiva,boj}-2000-2026-11-03-0835.json`, `m24-perf-2026-11-03-0835.md` |

Runtime knjižnice `141FFF9B46D83B7F…` (v M4.15a `1FC8E4D3B7E0C195…`: sosednji repo je bil vmes spremenjen in
zgrajen izven tega projekta; Q294), `npcbaritone.cfg` `27451787…` enak kot v M4.15a; NB4 enak v vseh celicah.

## Rezultati 0 → 1 (ms; ena ponovitev, brez šumnega pasu)

| Prizor 2000 | U1 tarča | TPS | MSPT povp. | p50 | p95 | p99 | µs/NPC | GC ms/s | Alok. MB/s | živi heap MB | npcb MB |
|---|---|---|---|---|---|---|---|---|---|---|---|
| nedosegljiva | 1341 → 1418 (≥1000), ranjenih 0/0 | 11,38 → 15,12 | 87,55 → 65,77 | 73,4 → 43,0 | 151,0 → 138,4 | 163,6 → 151,0 | 43,59 → 32,49 | 11,72 → 7,56 | 720 → 578 | 231 → 388 | 0,01 → 21,3 |
| boj | 2000/2000 obe | 20,17 → 20,00 | 43,89 → 41,46 | 41,9 → 38,8 | 55,6 → 52,4 | 58,7 → 60,8 | 21,77 → 20,24 | 2,91 → 6,82 | 360 → 384 | 257 → 706 | 0,01 → 43,5 |

Stran 0 pri nedosegljivih je nasičena (p50 > 50 ms), stran 1 ne. Boj je pri širini 50 drugačna geometrija kot v
M4.15a (širina 25: 46,95 → 38,10 ms), zato je primerjava z M4.15a samo okvirna; smer je enaka (1 nižje povprečje,
p99 pri boju tu +2,1 ms slabši).

Knjižnica pri 1 (µs, vsota glavne niti na tick): nedosegljiva glavna nit p50/p95/p99/max 3096/3710/4881/10659,
iskanje p50/p95 1343/5432, vrsta p95 92844, iskanj/končanih/neuspelih/zavrnjenih 94591/94603/90972/20092;
boj 9700/10678/18744/23794, iskanje 34/65, vrsta p95 8, 326468/326468/1/0. Posnetek p95 2 µs, chunkov p50 36.
Pri nedosegljivih so zavrnjena iskanja (20092) prvič neničelna: vrsta knjižnice je polna; poti do zaprte tarče ni,
zato to ne spremeni izida (U2 ranjenih 0).

## Pomnilnik (Q293)

Živi heap je merjen po celici v **istem** JVM (boj teče za nedosegljivimi), zato je boj kumulativen.
Pri 1 je razlika 157 MB po prvi in 449 MB po drugi celici; razredi knjižnice so samo 21/43 MB, ostalo so
`int[]`, `HashMap$Node`, `char[]`/`String`. `npcbaritone.core.api.Settings$Setting`: 426 213 → 852 213 primerkov
(~213 na doslej ustvarjenega NPC-ja, 2000 nato 4000). To se ujema s kopijo nastavitev na NPC, ki ostane po
odstranitvi NPC-jev prve celice; adapter ob odstranitvi kliče `NpcBaritone.detach`. Zadrževanje ni dokazano z
analizo korenov (brez heap dumpa) in je zabeleženo kot odprta omejitev knjižnice, ne mod napaka (Q294).

## Odločitve

- **Q292** — širša mreža (50) samo za P8; vzrok padca U1 v M4.15a je geometrija, merilo U1 ostane.
- **Q293** — živi heap je podatek brez meje (privzeto 0, Q92).
- **Q294** — knjižnica se ne spreminja (zunanji repo z tujimi neoddanimi spremembami in lastno gradnjo);
  `RwNavBackend` ostane privzeto 0, cena brez meje po Q92. P8 zapre odprti cenovni meji M4.15a (2000 nedosegljivih,
  skupni pomnilnik) kot izmerjena podatka; možno zadrževanje nastavitev ostane omejitev.

## Omejitve

Ena ponovitev, en stroj, dev strežnik; kapaciteta in stabilnih 20 TPS nista obljubljena. Živi heap po celicah
istega JVM; zadrževanje `Settings$Setting` ni potrjeno s heap dumpom. Uspešnih unit/build/package ne ponavljamo
(produkcija nespremenjena, Q90).
