# 04-REGISTRI — živi registri projekta

Sem se **dodajajo vrstice** med delom (glej `05-SEJA-PROTOKOL.md` §3). Do 2026-10-19 so bili
to razdelki v `04-STANJE.md`; premaknjeni so dobesedno, da jih nova seja ne bere ob vsakem
začetku. Ne preberi jih na začetku seje: išči z `grep` (npr. po imenu stikala `Rw…`).

- [Sprejete spremembe obnašanja](#sprejete-spremembe-obnašanja) — vsako stikalo in njegova privzeta vrednost
- [Prevzeta koda](#prevzeta-koda) — tuja koda z izvorom in licenco
- [Meritve](#meritve) — kazalo meritev (polni zapisi v `meritve/`)

---

## Prevzeta koda

| Datum | Kaj | Izvor | Licenca | Kje v našem drevesu |
|---|---|---|---|---|
| — | — | — | — | — |

Nič prevzetega. `NbtJson` je napisan na novo; format posnema original, koda ne.

---

## Sprejete spremembe obnašanja

| Datum | Sprememba | Zahteva / bug | Stikalo | Privzeto |
|---|---|---|---|---|
| 2026-10-19 | Klient: ime in naslov NPC-ja se od drugega izrisa istega niza (barva, pisava) predvajata iz display lista namesto znak za znakom v takojšnjem načinu; isti ukazi GL z istimi vrednostmi, preverba 0 razlik pikslov. Original za prvi izris, podrazrede `FontRenderer`, OptiFine, `§k`/`§n`/`§m` | M5-K K4, D-032 | `RwLabelCache` | **1** (D-032) |
| 2026-10-14 | `World.getCollisionBoxes` preskoči poizvedbo entitet za trke, kadar v svetu nihče ne more prispevati škatle in premikajoča se entiteta ne prepisuje `getCollisionBox(Entity)`; isti izid | M5.16, D-031, D-032 | `RwMoveBoxes` | **1** (D-032) |
| 2026-10-10 | NPC bere svoje ključe `EntityDataManager` iz senčnih polj (S16); isti izid | M5.14, D-032 | `RwDataShadow` | **1** (D-032) |
| 2026-10-10 | Iskanje poti oceni tip vozlišča vsake pozicije enkrat na iskanje (S14c); isti izid | M5.17, D-032 | `RwPathSearchMemo` | **1** (D-032) |
| 2026-10-10 | NPC, ki do tarče nima poti, 20 tickov ne poskuša znova (S5); **ni** enak izid — do 20 tickov zamika | M5.6, D-007, Q21 | `RwPathNegCache` | **1 od 16. 10.** (D-037; 0 = original) |
| 2026-10-10 | Prejemniki paketa utripa oči se poiščejo z obhodom `world.playerEntities` namesto pregleda ~450 chunkov (S7); isti prejemniki | M5.13, D-032 | `RwBlinkRecipients` | **1** (D-032) |
| 2026-10-09 | `onCollide` NPC-ja brez skript preskoči poizvedbo, ko nihče ne posluša `CollideEvent` (S4); isti izid | M5.12, D-032 | `RwCollideSkip` | **1** (D-032) |
| 2026-10-09 | Sledenje poti: ocena tipa vozlišča se v enem klicu `isDirectPathBetweenPoints` pomni po poziciji (S14b); isti izid | M5.11, D-032 | `RwPathFollowCache` | **1** (D-032, Q17) |
| 2026-10-07 | Iskalnik tarč: predzavrnitev pred raytraceom (S1) in poizvedba samo po igralcih (S2); iste tarče | M5-S, D-027 | `RwTargetPrefilter` | **2** (izjema od D-007, Q13) |
| 2026-10-07 | NPC nosilec z NPC posadko sprejme potisk z maso nosilec + jahači | R6 × R1, D-028 | `RwHitbox` | 0 (vanilla) |
| 2026-09-11 | Long array vrednosti se berejo z `getLong()` namesto `getByte()` | R9-a | ne | popravljeno |
| 2026-09-11 | Prazen `[B;]`/`[I;]`/`[L;]` obdrži svoj tip namesto da postane prazen seznam | R9-b2 | ne | popravljeno |
| 2026-09-11 | `NBTTagList` bytov/intov/longov se ne pretvori več v array tip | R9-b | ne | popravljeno |
| 2026-09-11 | Nizi obdržijo vodilne presledke in prelome vrstic | R9-d | ne | popravljeno |
| 2026-09-11 | Niz, ki se konča z `\`, `NaN`, `Infinity` in prazen ključ se preberejo namesto da vržejo | R9-e, e2, e3 | ne | popravljeno |
| 2026-09-11 | Parser ne rekurzira več na ključ; namesto `StackOverflowError` je omejitev globine 512 z jasno napako | R9-e4 | ne | popravljeno |
| 2026-09-11 | Prazen ključ se zapiše kot `"": vrednost` namesto da se izpusti | R9-e3 | ne | popravljeno |
| 2026-09-11 | `SaveFile` po pisanju eksplicitno flusha | — | ne | popravljeno |
| 2026-09-11 | Clone JSON se zapiše, sinhronizira in validira pred atomsko zamenjavo | B1 / R9-f | ne | varni zapis |
| 2026-09-11 | Player JSON se zapiše in validira pred zamenjavo; stari ostane ob napaki | B1 | ne | varni zapis |
| 2026-09-11 | Dialog, quest, linked NPC in trader market JSON se zapišejo neposredno skozi validirano atomsko zamenjavo | B1 | ne | varni zapis |
| 2026-09-11 | World controllerji, klientovi preseti in schematiki uporabljajo validiran atomski zapis stisnjenega NBT | B1 | ne | varni zapis |
| 2026-09-11 | Player save vrsta je vezana na world sejo in se izprazni pred resetom server globalov | B2 | ne | varen lifecycle |
| 2026-09-15 | Nov ukaz `/rwdiag` in zbiralnik meritev `rework/diag` | M2.1 | da — `/rwdiag on\|off`, `-Drwdiag=on` | izklopljeno |
| 2026-09-24 | Nosilec-NPC z jahačem-NPC obdrži svojo pot in move helper (jahač mu ju ne prepiše več vsak tick); nov ukaz `/rwmount` | R1, M3.3 | da — config `RwMountSteering` 0/1/2, `/rwmount` | 0 = original |
| 2026-09-24 | Jahač na nosilcu-NPC ne zažene sledenja/poti premikanja, ko krmili nosilec; `tpTo` jahača teleportira nosilca | R1, M3.4 | da — isti `RwMountSteering` | 0 = original |
| 2026-09-24 | NPC po sestopu z nosilca dobi nazaj polno višino hitboxa (`updateHitbox` v `dismountRidingEntity`) | R1, M3.5 | da — isti `RwMountSteering` | 0 = original |
| 2026-09-24 | NPC brez projektila, ki tava ali hodi po poti, napad začne takoj (napad dobi prioriteto pred gibalnim taskom); nov ukaz `/rwattack` | M3.6 | da — config `RwAttackPriority` 0/1, `/rwattack` | 0 = original |
| 2026-10-05 | Iskalnik tarč zavrne kandidata, do katerega NPC gotovo ni sovražen, pred raytraceom vidnosti (izbrane tarče enake); nov ukaz `/rwtarget` | M5-S S1 | da — config `RwTargetPrefilter` 0/1, `/rwtarget` | 0 = original |
| 2026-09-18 | Nov ukaz `/rwsquad` in `FormationApi`: formacije; ob koncu nastavijo dom in orientacijo članov | M4.14, D-021 | da — sam ukaz; `brezsidra`; `-Drwformation=off` | brez ukaza nič |

Vse zgornje so popravki tihe izgube podatkov, zato so brez stikala in privzeto vklopljene.
Format datotek se ne spremeni, zato ni migracije. Izjema je zadnji stolpec pri praznem
ključu: tam original ni znal zapisati berljive datoteke, mi jo znamo, original pa jo zna
prebrati.

---

## Meritve

| Datum | Scenarij | Rezultat | Zapis |
|---|---|---|---|
| 2026-10-21 | M5-K K5 `RwLodStart` (L1: brez oči, ploščato pokrivalo, brez imena onkraj praga): A/B idle-200 in idle-500, prag 0 / 32 blokov (3 + 3, nočni upravljalnik, K0–K8 in K15 zeleno v 12 zagonih; K15 delež L1 0,0 % / 60,6 % = geometrija) | p50 idle-500 11,464 → **8,691 ms (−24,2 %)**, p95 −21,0 %, p99 −21,2 %, FPS 84,5 → 110,7, razponi ločeni; idle-200 p50 6,423 → 6,454 (+0,5 %, prekrivanje) → v kodi, **privzeto 0** (spremeni videz, D-039) | [zapis](meritve/2026-10-21-M5-K-K5-lod-L1.md) |
| 2026-10-21 | M5-K K3 `RwEyesList` (oči iz display lista): A/B idle-200 in idle-500 (3 + 3, nočni upravljalnik, K0–K8 zeleno v 12 zagonih); enakost pikslov iz seje 97 (`EyesCheck`, 0 razlik) | p50 idle-200 6,451 → 6,644 ms (**+3,0 %**, razpona ločena v slabšo smer), idle-500 11,435 → 11,754 (+2,8 %, prekrivanje), p95 +1,7 / +3,3 %, p99 idle-500 +6,3 % (ločena, slabše) → **zavrženo**, privzeto ostane 0 | [zapis](meritve/2026-10-21-M5-K-K3-oci-display-list.md) |
| 2026-10-19 | M5-K K4 `RwLabelCache`: preverba pikslov v klientu (`LabelCheck`, idle-200, 174 napisov v dometu: ime testnega sveta; barvne kode, krepko, ležeče, šumniki; `§k` po originalu), A/B idle-200 in idle-50 (3 + 3) | enakost **0 razlik** barve in globine (drug naslov spremeni 2.817 pikslov); p50 idle-200 6,556 → **6,100 ms (−7,0 %)**, idle-50 2,164 → **2,025 (−6,4 %)**, p95 −6,1 / −7,6 %, razponi ločeni → privzeto 1 (D-032) | [zapis](meritve/2026-10-19-M5-K-K4-napisi-display-list.md) |
| 2026-10-19 | M5-K K14b `RwHeadwearSkip`: preverba pikslov v klientu (`HeadwearCheck`, 7 serij × 200 NPC: skin, vzorec, 5 pravih skinov), A/B idle-500 in idle-200 (3 + 3 × original / z robom / brez roba) | enakost **0 razlik** barve in globine v vseh serijah; p50 idle-500 11,493 → **8,595 ms (−25 %)**, idle-200 6,521 → **5,317 (−18 %)**, razponi ločeni; različica z robom (strogo enaka) v šumu → zavržena; privzeto 0, Q30 | [zapis](meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md) |
| 2026-10-19 | M5-K K14 pokrivalo v enem display listu (`RwHeadwearList` 0/1/2, poskus, odstranjen): enakost pikslov v klientu (`HeadwearCheck`, skin + testna tekstura), A/B idle-500 in idle-200 (3 + 3), zgornja meja `HeadWearType=0` (3), profil z enim listom | enakost **0 razlik** barve in globine; p50 idle-500 11,589 → 11,423 ms (razpona se prekrivata), idle-200 6,538 → 6,590 → **zavrženo**; profil: pokrivalo še vedno 26,1 % (čas je v 816 ogliščih na NPC, ne v klicih); zgornja meja brez 6 delov idle-500 p50 **−23 %** (8,907 ms); 41 % od 239 skinov CNPC ima povsem prozorno pokrivalo → K14b | [zapis](meritve/2026-10-19-M5-K-K14-pokrivalo-en-list.md) |
| 2026-10-18 | M5-K.1 baseline klienta (original, GUI viden, RD 8, 30 + 60 s, 3 ponovitve) idle-50/200/500, boj-200/500 + profil render niti (`StackSampler` 1 ms) | p50 2,153 / 6,570 / 11,571 / 5,782 / 12,551 ms, p95 2,425 / 7,103 / 13,033 / 6,476 / 14,272; razpon p50/p95 ≤ 9,2 %; profil: NPC-ji ~90 % render niti, model 31–38 %, pokrivalo 19–26 %, imena 20 % (idle-200), oči 10–14 %; JFR: 38 vzorcev render niti v 14 s (neuporaben) | [zapis](meritve/2026-10-18-M5-K.1-baseline-profil.md) |
| 2026-10-14/15 | M5.8 lestvica kapacitete (kratke celice 60 + 120 s, ena ponovitev), O = vsa stikala 0, S = privzeta po D-032; idle, boj, nedosegljiva | p95 ≤ 25 ms obkroženo: idle O 350–400 / S 3500–4000, boj O 1000–1500 / S 1500–2000, nedosegljiva O 500–600 / S 700–800; ≤ 50 ms: idle O 500–600 / S 7000–8000, boj O 1500–2000 / S 2500–3000, nedosegljiva O 800–1000 / S 1200–1400. Boj pri istem N −38 %. Meja prisilnih chunkov 400 → 2000 (P4c). **Potrditev K3 idle** (3 ponovitve, 120 + 300 s): O 350 = 20,97 / 400 = 27,26, S 3500 = 24,64 / 4000 = 28,31 ms p95, razponi ločeni → 25 ms: **350 → 3500 (10×)** | [zapis](meritve/2026-10-15-M5.8-lestvica.md) |
| 2026-10-14 | M5.16 `RwMoveBoxes`: enakost (način 2, idle + boj), A/B idle-500 0/1 (**5 + 5** ponovitev), A/B boj-500 0/1 (3 + 3), `RwTarget 2` | enakost **0 neujemanj / 945.162 primerjav** (boj; v idle poizvedba ne najde nikogar v 1,25 M klicih); idle-500 MSPT povp **2,787 → 2,540 ms (−8,9 %)**, p50 2,556 → 2,294, p95 4,194 → **3,867**, p99 4,850 → 4,719, µs/NPC 5,47 → 4,98, alokacija 20,7 → 20,0 MB/s — vse z ločenima razponoma; 100 % klicev preskočenih (505 na tick); boj-500 brez ločitve in brez regresije | [zapis](meritve/2026-10-14-M5.16-trki-brez-poizvedbe.md) |
| 2026-10-10 | M5.14 S16 `RwDataShadow`: enakost (način 2, idle + boj) in A/B 0/1 (3 + 3 × dve varianti), `RwTarget 2` | enakost **0 neujemanj / 52,0 M primerjav**; ~20 branj na NPC na tick; idle-500 MSPT povp **3,023 → 2,863 ms (−5,3 %)**, p95 4,588 → **4,325**, µs/NPC 5,92 → 5,60 (p99 se ne loči); boj-500 mediana −15 %, a v šumu | [zapis](meritve/2026-10-10-M5.14-sencna-polja.md) |
| 2026-10-10 | M5.17 S14c `RwPathSearchMemo`: enakost (način 2), cena iskanja (1 + 1) in A/B celih zagonov (3 + 3 × dve varianti), `RwTarget 2` | enakost **0 neujemanj / 239,5 M primerjav ocen**; 184,0 → **144,9 µs** na iskanje (−21 %), zadetkov 52,3 %; `nedosegljiva`-500 MSPT povp **17,959 → 15,253 ms (−15 %)**, p95 22,02 → 18,87, alokacija 291,5 → **162,8 MB/s**; `boj`-500 nevtralen (9,064 → 9,424, v razponu obeh vej) | [zapis](meritve/2026-10-10-M5.17-pomnjenje-med-iskanjem.md) |
| 2026-10-10 | M5.6 S5 `RwPathNegCache`: preverba (3 pravila) in A/B 0/1 (3 + 3), `nedosegljiva`-500 | preverba: neujemanj **58,6 %** (samo `null`, 20 tickov) oz. 66,8 % (tudi delne poti), pri 4 tickih **0 preskokov**; A/B: MSPT povp **17,854 → 14,462 ms (−19 %)**, p95 22,02 → 18,35, alokacija 292,5 → **135,2 MB/s**, ticki > 25 ms 45 → 10, preskočenih 17,46 iskanj na tick | [zapis](meritve/2026-10-10-M5.6-negativni-predpomnilnik.md) |
| 2026-10-10 | M5.9 scenarij nedosegljive tarče: celici `nedosegljiva`-500 in `boj`-500 v istem zagonu, `RwTarget 2`, `RwPath 0` + merjenje iskanja | iskanje poti **2,45 % ticka (boj) proti 51,18 % (nedosegljiva)**; eno iskanje 3,5 proti **180,5 µs**; MSPT povp 9,23 → **17,73 ms**, p95 10,75 → 22,02; alokacija 91 → 293 MB/s; celih poti 219.335 proti **0**, delnih 0 proti 199.402; pri 50 NPC-jih 71,1 % ticka | [zapis](meritve/2026-10-10-M5.9-nedosegljiva-tarca.md) |
| 2026-10-10 | M5.13 S7 `RwBlinkRecipients`: enakost (način 2 in `poskus`), A/B iskanja v istem zagonu 0/1, A/B celih zagonov 0/1 (3 + 3), idle-500, `RwTarget 2` | enakost **0 neujemanj** (3.939 + 388 primerjav načina 2, 4× 320 primerjav `poskus`); iskanje prejemnikov **55,42 → 0,12 µs** (−0,167 ms/tick, razpon iskanj 0,2 %); celi zagoni MSPT povp **2,824 → 2,570 ms**, p50 2,621 → 2,359, p95 4,325 → **3,932**, p99 4,719 → 4,325, µs/NPC 5,54 → 5,05 — vse nad šumom; alokacija nespremenjena; 8,22 M manj pregledanih chunkov na 300 s | [zapis](meritve/2026-10-10-M5.13-prejemniki-utripa-oci.md) |
| 2026-10-09 | M5.12 S4 `RwCollideSkip`: skripte-500 način 2; idle-500 A/B 0/1 (3 + 3), `RwTarget 2` | skripte: 0 brez opazovalca / 161.000; idle-500 p95 5,24 → **3,93 ms**, p99 6,16 → **4,13**, povp 3,53 → 3,02 (v šumu A), µs/NPC 6,85 → 5,84; K2/K3 (poslušalec ustavi preskok) zelena | [zapis](meritve/2026-10-09-M5.12-oncollide-brez-opazovalca.md) |
| 2026-10-09 | M5.11 S14b, boj-500, `RwTarget 2`: enakost (2×), A/B v istem boju 0/S14b/S14 (3 ponovitve), A/B celih zagonov 0/1 (3 + 3) | enakost **0 / 1.245.605**; v istem boju µs/kandidat 0,37–0,41×, pathFollow −2,9…−3,5 ms/tick, S14 v šumu (odstranjen); celi zagoni MSPT povp **8,73 → 5,88 ms**, p95 9,96 → 7,21, alokacija 89 → 58 MB/s | [zapis](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md) |
| 2026-10-09 | M5.10 delež sledenja poti, boj-500, `RwTarget 2`, `RwPath 0` + merjenje, 3 ponovitve | `pathFollow` **47,7–53,6 %** povprečnega ticka (mediana 4,08 od 8,55 ms); 262 `pathFollow` in 251 kandidatov na tick (razpon 1,4 / 1,1 %), 0 ali 1 kandidat na `pathFollow`, vsi prosti; µs na kandidata 15,8–19,7 (razpon 24,5 %), MSPT razpon 14 % | [zapis](meritve/2026-10-09-M5.10-delez-sledenja-poti.md) |
| 2026-10-08 | M5-S S14 `RwPathFollowCache`, boj-500: enakost (način 2) in A/B 0/1 | enakost **0 neujemanj / 616.276**; A povp 8,96 ms (razpon 0,30), B 8,84 / 7,85 (3. ponovitev neveljavna, P9) — **nedokazano**, stikalo ostane 0 | [zapis](meritve/2026-10-08-M5-S14-sledenje-poti.md) |
| 2026-10-08 | M5-S P1 profil JFR, idle/boj/skripte × 500, `RwTarget 2`, ena ponovitev | boj-500: **48 %** CPU strežniške niti v `pathFollow` → `isDirectPathBetweenPoints` (27 % hash iskanje chunka), iskanje poti 2 %; idle-500: `Entity.move` 29 %, `onCollide` 12 %, `EntityDataManager.get` 12 %, utrip oči 6,5 %, iskalnik tarč 1,8 %; alokacija ~55 % vanilla `AxisAlignedBB` | [zapis](meritve/2026-10-08-M5-S-P1-profil-jfr.md) |
| 2026-10-07 | M5-S S2 A/B `RwTargetPrefilter` 1/2, idle-500 in boj-500, 3 ponovitve | idle-500: MSPT p95 10,22 → **5,24** ms, p99 11,27 → **5,51**, µs/NPC 9,74 → **6,60**, ticki >10 ms 370 → **6**, alokacija nespremenjena; boj-500 nevtralen (`zozenih = 0`) | [zapis](meritve/2026-10-07-M5-S2-poizvedba-igralci.md) |
| 2026-10-07 | M3.9 solid × jahanje, `m39-run.ps1` | J1–J6, P1–P4 zelena: jahan nosilec 0 v vseh načinih (J4 solid proti jahanemu 0/0 → Q15); 8/8 hodečih mimo NPC-ja na poti brez teleporta; solid ob stiku 0 proti 0,0219 bloka/tick | [zapis](meritve/2026-10-07-M3.9-solid-jahanje.md) |
| 2026-10-06 | M5-S S1 A/B `RwTargetPrefilter` 0/1, idle-500 in boj-500, 3 ponovitve | idle-500: alokacija 342,4 → **21** MB/s, MSPT p95 41,9 → **9,96** ms, µs/NPC 29,0 → **9,31**, ticki >25 ms 2.016 → **6**; boj-500 nevtralen | [zapis](meritve/2026-10-06-M5-S1-predzavrnitev-tarc.md) |
| 2026-09-11 | NBT↔JSON round-trip, 4000 naključnih struktur | original 1598 napak (40 %), novi 0 | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON zapis, 1500 ključev (68 KB) | 235,3 ms → 2,9 ms (81×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON branje, 1500 ključev | 2386,3 ms → 6,5 ms (367×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-24 | M3.2 faza C: `navigateTo` jahačem proge M | navig nosilca = navig jahača 20/20; M na cilju (0,64) v ~180 tickih, 2,8× hitreje od S | [zapis](meritve/2026-09-24-M3.2-R1-diagnoza.md) |
| 2026-10-05 | M3.6 ponovni zagon (doseg ločen), način 0 ×2 in 1 | tavajoči v dosegu: način 0 združeno 8 vzorcev, mediana **25**, max 30, 6/8 nad kontrolo; način 1 10 vzorcev, vsi **10**; kontrola 10; A5 zelen, P2 potrjen | [zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md) §6 |
| 2026-09-24 | M3.6 napad med tavanjem, `m36-run` način 0 in 1 | tavajoči mediana/max: način 0 **30/120** tickov (2 brez napada), način 1 **10/50**; kontrola 10/10; A5 pade na 1 vzorcu (hipoteza `aggroRange`) | [zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md) |
| 2026-09-24 | M3.5 E9: višina jahačev po sestopu, `r1-run` način 0 in 1 | način 0: 1,463 (ostane × 0,77); način 1: 1,900 (vrne se) | [zapis](meritve/2026-09-24-M3.5-hitbox.md) |
| 2026-09-15 | M2.1 prvi posnetek: 8 NPC-jev, 61 s, brez igralca | MSPT p50 0,16 ms / p95 1,21 ms; `npc.per.tick` p50 = 0 | [zapis](meritve/2026-09-15-M2.1-prvi-posnetek.md) |
| 2026-09-17 | M2.2 R1: 8 nosilcev z jahači proti 8 brez, dve fazi, 945 tickov | proga M `isNavigating` **0/8 ves čas**, prevozeno 0,06 bloka; kontrola 8/8 in 22,71 bloka | [zapis](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| 2026-09-17 | poraba pri 27 dejavnih NPC-jih (ni baseline) | `npc.update.window` 207,3 µs/NPC; MSPT p95 10,75 ms, p99 115,3 ms | [zapis](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| 2026-09-17 | M2.3 R2 faza A: 6 letečih + zid, 6 kopenskih + isti zid, 6 letečih prosto | leteči čez zid **0/6**, prevozeno 7,95 in `cele=0/6`; kopenski 6,93; leteči brez ovire 16,27 in `cele=6/6` | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-17 | M2.3 fazi B in C — **neveljavni** (pojav P1) | 12 letečih NPC-jev 0,00 bloka v 900 tickih pri `navig=6/6` | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-17 | M2.3 ponovitev z `dStarost` in `gib` (12:37) | `dStarost` 20 povsod, `gib` točno 0 v zmrznjenih progah proti 0,2306 v fazi A — dve razlagi od treh ovrženi | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-17 | M2.3 tretji zagon (13:49), popravljen reset + faza D | zmrznitve ni nikjer; proga F čez zid 0/6 pri enem `navigateTo`, **6/6 ob osveženi poti** in `cele=6/6` pri vanilla AI; P1 neodločen (faza D padla na vhodnem pogoju) | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-21 | M2.3 četrti zagon (07:45), postavitev na začetku faze | **L1–L12 zelena**; faza D z z = −16,0 prevozi 15,93 pri `gib` 0,2734 → **P1 OVRŽEN**; faza C proge P prvič veljavna (16,34) | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-18 | M2.5c: tri ponovitve scenarija M2.7, vsaka svež svet (2,6 / 2,1 / 2,0 min) | T1–T6 zelena; **46 veličin od 59 z razponom nič**, šumna samo veličina 5 (do 114 %) | [zapis](meritve/2026-09-18-M2.5c-ponovitve-nav.md) |
| 2026-09-17 | poraba pri 21 NPC-jih med M2.3 (ni baseline) | `npc.update.window` 162,5 µs/NPC; MSPT p50 0,84 ms, p95 8,91 ms, p99 102,8 ms | [zapis](meritve/2026-09-17-M2.3-R2-reprodukcija.md) |
| 2026-09-21 | M2.7b, dva zagona nav (12:23 in 12:28) | razdelitev se sešteje (`prviN=8`, `ponN=56` povsod), vse razen časa identično; ogreta `O.ponP50.poA` pa 254,0 proti 42,0 µs — **šum je med pometanji, ne znotraj njih** | [zapis](meritve/2026-09-21-M2.7b-ogrevanje.md) |
| 2026-09-21 | `rwdiag-run.ps1` 12:55 — **neveljaven** | `world.npc.loaded = 17` (NAV svet, brez `T_Scripted`); S1–S7 še vedno niso ovrednotena | [zapis](meritve/2026-09-21-M2.7b-ogrevanje.md) |
| — | baseline MSPT še ni izmerjen (M2.6) | — | — |

Meritve so tekle na OpenJDK 21 v oblačnem okolju, ne na Javi 8. Ponovitev na Javi 8 je odprta.

