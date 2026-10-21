# M5-V.1 — odločilni poskus in prizorišče "daleč"

Skripta: [`v-run.ps1`](../../v-run.ps1) (ena celica na zagon), ponovitve:
`.\ponovitve-run.ps1 -Scenarij dalec -Dodatno @('-Vd','24','-Domet','368','-N','5000')`.
Plan: [03-FAZE §M5-V](../03-FAZE.md), raziskava [M5-V.0](../raziskave/M5-V.0-daljna-vidnost.md) §7, D-034, D-035, D-040.

## Namen

Izmeri ceno **variante A** (dvig dometa sledilnika NPC-jev s 64 na 368 blokov) proti originalu na
**dediciranem strežniku s priključenim klientom** pri `view-distance` 10 in 24, da so številke, s katerimi
se varianta B (M5-V.2–V.8) primerja, izmerjene in ne ocenjene. Ločeno izmeri vanilla ceno razdalje
(10 → 24) od cene vidnosti.

## Celice

| parameter | vrednosti | pomen |
|---|---|---|
| `-Vd` | 10, 24 | `view-distance` strežnika in RD klienta |
| `-Domet` | 64, 368 | 64 = original (`tracker(64, 3, true)`), 368 = varianta A (`/rwdiag track 368`, klientski `renderDistanceWeight` 6) |
| `-N` | 1000, 5000 | NPC-jev v prizorišču |

Domet sledilnika je vedno `min(domet, (vd − 1) · 16)`: varianta A pri `vd 10` je 144 blokov.

## Prizorišče

- Svež svet `dev\run\rwv-dalec` vsak zagon (`runServer --world`, `-PrwWorld`), FLAT s semenom
  `customnpc-rework` iz `dev\run\server.properties`; `server.properties` in `dev\run\world` se ne spremenita.
- Kamera (spectator) pri (0,5; 16; 3000,5), yaw 180, pitch 10, FOV 70 — daleč od območja spawna (0, 0).
- N NPC-jev `PERF_Idle` v gručah 5 × 5 (`noppes clone grid … 1 5 5`), N / 25 gruč, enakomerno po površini
  izseka ±35° pred kamero na razdalji 20..358 blokov (polmer `sqrt(r²min + (r²max − r²min)(k + ½)/K)`, kot
  zlati rez; gruče se ne prekrivajo, Chebyshev ≥ 6). Vsi NPC-ji so znotraj 368 blokov (Chebyshev) in 24 chunkov.
- Prizorišče se vedno spawna pri razdalji 24 (`/rwdiag vd 24`), nato se razdalja zmanjša na celico
  (`/rwdiag vd 10`, `PlayerList.setViewDistance`, kot ob spremembi nastavitve v vanilli: posodobi
  `PlayerChunkMap` in mejo sledilnika). Chunki onkraj se izpraznijo kot na pravem strežniku, kjer NPC-ji
  obstajajo v svetu, a so naloženi samo do `view-distance`.

Pričakovano po geometriji (izračun v skripti):

| N | naloženih pri 24 | pri 10 | sledenih 64 | sledenih 144 | sledenih 368 |
|---|---|---|---|---|---|
| 1000 | 1000 | 230 | 25 | 195..200 | 1000 |
| 5000 | 5000 | 1245 | 160..180 | 920..940 | 5000 |

## Potek

1. Strežnik (`runServer`, svet iz `--world`), ogrevanje vhoda z `list`, ukazi determinizma iz
   `perf-setup-commands.txt` (brez `spectatorsGenerateChunks false`), `difficulty 0`, `rwdiag vd 24`,
   pri A `rwdiag track 368` (pred spawnom: domet se prebere ob začetku sledenja).
2. Klient (`runClient`, harness z `connect=127.0.0.1:<vrata>`) vstopi in izpiše `RWHARNESS-IGRALEC`;
   skripta pošlje `gamemode 3` in `tp`, počaka 49 × 49 chunkov, spawna gruče, zmanjša razdaljo na celico,
   izpiše `rwdiag spawnnbt` in pošlje `say RWHARNESS-SCENA`.
3. Klient prešteje NPC-je (5 s umiritve), se ogreje 30 s in meri 60 s. V istem oknu ima strežnik
   `rwdiag on` (skripta ga vklopi ob `RWHARNESS-FAZA MEASURE`, izklopi ob `FAZA DONE`, zamik ~1 s).

## Velikosti

- **Klient:** čas sličice p50/p95/p99/max, FPS, 1 % low; čas klientskega ticka (START..END
  `ClientTickEvent`) p50/p95; žični bajti s strežnika (števec na začetku Netty cevovoda, pred
  razpakiranjem) v merilnem oknu (B/s) in do začetka ogrevanja (teren + spawn).
- **Strežnik:** MSPT z igralcem (`server.tick.ns`, p50/p95/p99/povprečje), naloženi NPC-ji, velikost
  dodatnih podatkov spawna (`writeSpawnData`, GZIP NBT) naloženih NPC-jev (n, min, p50, p95, max, vsota).

## Merila

| ID | merilo |
|---|---|
| V0 | pred zagonom ne teče tuj gradle/Minecraft; vsi NPC-ji prizorišča so znotraj 24 chunkov |
| V1 | strežnik zagnan, res v svetu `rwv-dalec` (sicer se prizorišče ne postavi), razdalja 24, domet sledilnika kot zahtevano |
| V2 | klient vstopil |
| V3 | pri 24 je okoli igralca naloženih vseh 49 × 49 chunkov (`okoliIgralcev`; skupno število ni merilo, ker se chunki okoli mesta vstopa po teleportu izpraznijo) |
| V4 | na strežniku je N NPC-jev |
| V5 | po zmanjšanju razdalje je naloženih natanko toliko, kot jih je v chunkih znotraj razdalje; meja sledilnika `(vd − 1) · 16` |
| V6 | `spawnnbt` je izmeril vse naložene |
| V7 | klient prišel do konca meritve brez napak |
| V8 | v klientu je toliko NPC-jev, kot jih po geometriji sledilnika pričakujemo (±1 blok na meji) |
| V9 | **veljavnost:** upodobljenih entitet ≥ NPC-jev v klientu v vsakem vzorcu (vidnih je res N) |
| V10 | kamera fiksna |
| V11 | vsync 0, brez omejitve sličic, RD = vd, FOV, `renderDistanceWeight` (A: 6, original: `clamp(RD/8, 1, 2,5)`); pri A harness utež ponovno nastavi vsak tick, ker jo vsak `loadRenderers` vrne na clamp, in v merjenju ne sme biti nobene ponastavitve |
| V12 | **veljavnost vzorca:** ≥ 300 sličic (≥ 3 vzorci nad p99 in v 1 % low) in ≥ 15 klientskih tickov/s v 60 ± 2 s. FPS sam je izmerjena velikost, ne merilo (D-041): prvotni prag 10 sličic/s je zavrnil celico 24/368/5000 pri 6 FPS, kar je ravno cena, ki jo poskus meri |
| V13 | bajti izmerjeni |
| V14 | strežnik zapisal posnetek merilnega okna |
| V15 | med ogrevanjem in merjenjem ni tekel tuj java proces |

## Omejitve

- Strežnik in klient tečeta na istem računalniku (DESKTOP-SN494C0) in si delita CPU; to velja za vse
  celice enako, absolutni MSPT in čas sličice nista primerljiva z meritvami brez drugega procesa.
- `rwdiag on` doda instrumentacijo (kot v vseh meritvah M2.4/M5-S).
- Bajti so žični (stisnjeni) in vsebujejo vse pakete (teren, NPC-ji, ključ sledenja); idle NPC-ji
  skoraj ne pošiljajo gibanja, zato je okno idle predvsem ključ obstoja.
- `track` velja za entitete, ki se začnejo slediti po ukazu; zato pred spawnom.

## M5-V.2: `-Far 1` (kanal `rwfar`)

Ista celica (samo `-Domet 64`) z vklopljenimi nadomestki: klient dobi v navodilu `farView=1`
(`CustomNpcs.RwFarView=1` ob vstopu → `HELLO` z dometom `RD · 16`), strežnik po postavitvi prizorišča
`rwfar domet 384` in `rwfar 1`, ob začetku merjenja `rwfar reset`, ob koncu `rwfar`. Pas
(`FarBand`): Chebyshev `(min(64, (vd−1)·16) − 8, min(384, RD·16, (vd−1)·16)]` v chunkih, ki jih igralec
gleda; pričakovano število skripta izračuna iz geometrije (±1 blok na mejah).

| Merilo | Pogoj |
|---|---|
| V16a | po `rwfar 1`: `nacin=1`, `domet=384`, en igralec s `HELLO` |
| V16 | register NPC-jev `rwfar` = NPC-ji v `loadedEntityList` = naloženi po V5 |
| V17 | nadomestkov v klientu ob začetku in koncu merjenja v pričakovanem razponu; strežnikov pas in število znanih id-jev enako klientovi shrambi; `HELLO` ≥ 1 |
| V18 | čas kode `rwfar` ≤ 100 µs na tick na igralca v merilnem oknu (strežniški cilj +≤ 0,1 ms, D-043); B/s kanala se izpiše |

Klient po koncu merjenja (`RWHARNESS-FAZA DONE`, JSON zapisan) ostane povezan, dokler skripta po branju
`rwdiag vd` in `rwfar` ne pošlje `say RWHARNESS-IZHOD` (največ 120 s); števci na igralca in naloženi chunki
se tako berejo, ko je igralec še prijavljen.

Primerjava z originalom (A/B, tri ponovitve) je M5-V.8; `-Far 1` v M5-V.2 preveri kanal od konca do konca.
