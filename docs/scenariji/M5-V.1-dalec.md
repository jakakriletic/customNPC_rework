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

## M5-V.3: opis videza (V19, isti `-Far 1`)

Od M5-V.3 vnos nosi kratek id videza (D-045). Strežnik ob koncu ticka izračuna največ 64 opisov
(`FarLook`: podmnožica spawn NBT brez imena/naslova, 64-bitni hash), naslednji krog pošlje vnos z id-jem;
klient za neznan id pošlje `REQ`, strežnik odgovori z `LOOK` (GZIP opis + hash), klient hash preveri.
`rwfar` izpiše `videzov`, `izracunovVse`, `cakaVidez`, `nsVidez`, `zahtev`, `zavrnjenih`, `odgovorov` …;
JSON klienta `farVidezov`, `farVidezZahtev`, `farVidezOdgovorov`, `farVidezNapak`, `farBrezVideza`.
Čas opisov se od M5-V.3 šteje v `usNaTickNaIgralca` (V18).

| Merilo | Pogoj |
|---|---|
| V19 | strežnik: videzov v rabi **1** (vsi NPC-ji so kloni `PERF_Idle` z različnimi entitetami — kloni si delijo hash), izračunov ≥ pas, 0 zavrnjenih zahtev, 0 napak, 0 prevelikih opisov; klient: ≥ 1 opis in ≥ 1 odgovor, 0 napak (hash po razpakiranju enak poslanemu), **0 nadomestkov brez opisa** ob koncu merjenja |

Testa upravljalnika: `v3-far-24-64-5000`, `v3-far-10-64-1000` (`v-run.ps1 -Far 1`).

## M5-V.4: peka impostorjev (V20–V21, `-Far 1 -Impostor 1`)

Z `-Impostor 1` (samo skupaj s `-Far 1`) harness klientu nastavi `impostorCheck=1`. Klient speče vsak videz, ki ga
prejme po kanalu `rwfar` (`ImpostorBaker`, en videz na sličico, D-046), po ogrevanju pa `ImpostorCheck` za 8 testnih
videzov (prvi opis iz kanala, isti s petimi pravimi skini CNPC ter z velikostjo 10 in 2) primerja model in ploskev
impostorja pri 56 blokih za vseh 8 kotov. Klient izpiše vrstico `RWHARNESS-IMPOSTOR`; JSON klienta ima `impPeceno`,
`impVAtlasu`, `impBrezPeke`, `impNapak`, `impOdlozenih`, `impObrezanih`, `impBrezFbo`, `impPekaMsPovp`, `impPekaMsMax`,
`impPreverba`, `impVidezov`, `impObcutljivost`, `impSerije`, `impNapaka`.

| Merilo | Pogoj |
|---|---|
| V20 | ≥ 1 pečen videz, **0 nadomestkov z opisom brez peke**, 0 napak peke, framebuffer na voljo, nadomestki v klientu > 0 |
| V21 | za vsak od 8 videzov (združeno čez 8 kotov) IoU silhuete ≥ **0,70** in povprečna razlika barve v preseku ≤ **32**/255; občutljivost (enobarvni skin proti modelu osnove) > **64**; vseh 8 videzov izmerjenih. Merila so določena pred prvo meritvijo |

Testa upravljalnika: `v4-imp-24-64-5000`, `v4-imp-10-64-1000` (`v-run.ps1 -Far 1 -Impostor 1`).

## M5-V.5: izris L3 (V22–V24, `-Far 1 -Impostor 1 -Izris 1`)

Z `-Izris 1` (samo skupaj z `-Impostor 1`) klient nadomestke riše kot impostorje (`FarRender`, D-047): en
`BufferBuilder` na sličico v `RenderGlobal.renderEntities` pred `"blockentities"` (poseg coremoda
`RenderEntitiesPatch`), rezerva `RenderWorldLastEvent`. Prizorišče dobi **sondo**: en `PERF_Idle` naravnost pred
kamero na zunanjem robu pasu, `(vd − 1) · 16` blokov (pri vd 24 **368**, pri vd 10 144; Chebyshev = evklidska
razdalja, ker je `dx = 0`); N prizorišča je zato N + 1. Harness dobi `farDrawCheck=1` in `seamCheck=1`.

- **V23 (po ogrevanju):** harness kamero usmeri v sondo in nastavi FOV **10** (pri 480 vrsticah je NPC na 368 blokih
  pri FOV 70 ~1 × 2 piksla, pod mejo zanesljive rasterizacije; pri 10 ~7 × 14). Megla je odvisna od razdalje, ne od
  FOV, zato ostane igrina (pri 368 od 384 blokov ~83 % megle). Po 20 sličicah prebere okno okoli pravokotnika sonde
  (+3 px) v treh zaporednih sličicah: A z izrisom L3, B brez, C z njim. Nato FOV in kot nazaj, merjenje 2 s pozneje.
  Sonda je najbolj oddaljen nadomestek, ki je še vsaj 12 blokov pred koncem megle (`RD · 16`): pri vd 10 je
  diagonalni NPC gruče na 168,8 bloka za koncem megle (160) in povsem v njeni barvi, neviden tudi v originalu
  (prvi zagon 2026-10-21: 0 spremenjenih pikslov). Meja pri vd 24 je 372 ≥ 368, merilo ostane nespremenjeno.
- **V22 (merjenje):** vsako sličico isti izris; števci `FarRender` od začetka merjenja.
- **V24 (po merjenju):** JSON merjenja se zgradi ob koncu merjenja, klient 5 s miruje (skripta takrat prebere
  `rwdiag dump`, `rwdiag vd` in `rwfar` — merilno okno strežnika ostane mirno), nato faza `SEAM`: kamera se s
  **hitrostjo hoje** (0,2158 bloka/tick) premakne 48 blokov nazaj in nazaj na začetek (447 tickov). Vsako sličico se
  zbere množica narisanih NPC-jev (živa entiteta prek `RenderLivingEvent.Post`, impostor iz `FarRender.DRAWN_IDS`).

JSON klienta: `farPoseg`, `farIzrisSlicic`, `farIzrisRezerva`, `farIzrisNarisanihMin/Max`, `farIzrisZivihMax`,
`farIzrisIzvenFrustumaMax`, `farIzrisBrezPekeMax`, `farIzrisNajdlje`, `farIzrisUsPovp/Max`, `dalecPreverba`,
`dalecRazdalja`, `dalecSpremenjenih`, `dalecNajvecRazlika`, `dalecKontrola`, `dalecPoskusov`, `dalecOpis`,
`sivPreverba`, `sivSlicic`, `sivVrzeli`, `sivZivDoImpostorja`, `sivImpostorDoZivega`, `sivDvojnih`, `sivIzPasu`,
`sivIzvenFrustuma`, `sivNajvecNarisanih`, `sivOpis`. Vrstice dnevnika `RWHARNESS-DALEC`, `RWHARNESS-SIV`.

| Merilo | Pogoj |
|---|---|
| V22 | poseg coremoda uspel (`farPoseg = 1`), 0 sličic rezerve, izris v vsaki sličici merjenja (≥ sličic − 2); narisanih min = max, v mejah nadomestkov v pasu `(64, rob]` (±1 blok); živih (šiv) v mejah `(56, 64]`; narisani + živi = nadomestki v klientu; 0 izven frustuma, 0 brez pečenega videza |
| V23 | sonda je najbolj oddaljen nadomestek, razdalja ≥ rob pasu − 0,5 (vd 24: **≥ 367,5**); A ≠ B v ≥ **1** pikslu za ≥ 3/255 v kakšnem kanalu; kontrola A = C (0 pikslov; sicer do 5 poskusov) |
| V24 | 0 vrzeli (NPC, narisan v prejšnji sličici, v tej ni narisan ne živ ne kot impostor; izvzeto: ni ga več nikjer in je bil ≤ 4 bloke od zunanjega roba pasu, ali živ izven frustuma), 0 dvojnih (živ in impostor v isti sličici), ≥ 1 prehod živ → impostor in ≥ 1 impostor → živ |

Testa upravljalnika: `v5-izris-24-64-5000`, `v5-izris-10-64-1000` (`v-run.ps1 -Far 1 -Impostor 1 -Izris 1`).

**Omejitev V24:** prekrivanje pasu je 8 blokov, kadenca strežnika ~1 Hz. Pri hoji (4,3 bloka/s) nadomestek pride
~1,9 s pred uničenjem žive entitete; pri hitrosti nad ~8 blokov/s (letenje v ustvarjalnem načinu 10,9, gledalec) lahko
NPC na meji izgine za do ~1 s. Scenarij meri hojo; hitrejše premikanje je kandidat za M5-V.8 (4 Hz v bližnjem pasu,
raziskava M5-V.0 §5.1).

## M5-V.8: sprejemni A/B (V25–V26, `-Sprejem 1 -Far 0|1|2`)

Sprejem D-035 (plan M5-V.8): celice M5-V.1 z dometom 64 (vd 10 / 24 × N 1.000 / 5.000) v treh variantah na **istem**
prizorišču, tri ponovitve vsaka (`ponovitve-run.ps1 -Scenarij sprejem`, paket `M5-V.8`, predpona `m5v8-sprejem`).

| `-Far` | strežnik | klient `RwFarView` | kaj se riše onkraj 56 / 64 blokov |
|---|---|---|---|
| 0 | brez `rwfar` | 0 | original: sledeni modeli do 64, dlje nič |
| 1 | `rwfar 1`, domet 384 | 1 | modeli do 64, L3 impostorji 64..`(vd − 1)·16` |
| 2 | `rwfar 1`, domet 384 | 2, `RwImpostorStart` 56 | modeli do 56, L2 56..64, L3 dlje |

- Prizorišče je v vseh treh variantah enako: gruče M5-V.1 **in** sonda na `(vd − 1)·16` (N + 1 NPC-jev; pri M5-V.1 sonde
  ni bilo, zato so izhodišče te celice in ne stare številke).
- Merilno okno (30 s ogrevanja, 60 s merjenja, kamera fiksna) je enako kot pri M5-V.1. Pri `-Far 1/2` je pred merjenjem
  A/B pikslov sonde (V23, kamera se po njem vrne in 2 s miruje), po merjenju pa hoja V24.
- **V25 (vse variante):** po merjenju in 5 s mirovanja kamera hodi s hitrostjo hoje 48 blokov nazaj in nazaj (≈ 22 s);
  harness zapiše žične bajte (`hojaBajtovNaS`) in koristne bajte `rwfar` (`hojaFarBajtovNaS`). Merilo veljavnosti:
  hoja izvedena, ≥ 20 s. Cilj D-035 (hoja ≤ 20 KB/s, idle ≤ 1 KB/s na igralca) se presodi v zapisu meritve iz A/B.
- **V26 (vse variante):** po hoji kamera z mesta merjenja pogleda v NPC, ki je vodoravno najbližje 64 / 128 / 256 / 368
  blokom (samo cilji znotraj `(vd − 1)·16`: pri vd 10 samo 64 in 128), in shrani sličico pri FOV 70 in FOV 10
  (`…-klient-posnetek-<d>-fov<f>.png` ob JSON-u). Merilo: vsi posnetki zapisani.
- Pri `-Far 2` V24 šteje tudi impostorje L2 kot narisane (`FarRender.DRAWN_IDS` od M5-V.8 vsebuje L2), zato preveri
  tudi mejo model ↔ L2 pri 56/52 blokih.
- Ciljne vrednosti (sličica p95 +≤ 1,0 ms pri 5.000, MSPT oziroma čas `rwfar` ≤ 100 µs na tick na igralca, omrežje,
  VRAM ≤ 32 MB) niso merila zagona, ampak sprejema; presodijo se iz median treh ponovitev.
