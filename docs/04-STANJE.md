# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki se spreminja vsako sejo.** Vsaka seja ga na koncu posodobi.
Če seja tega ne naredi, je naslednja seja slepa.

---

## Trenutno stanje

| | |
|---|---|
| Zadnja posodobitev | **2026-10-10** (seje 79-81. **Seja 81: M5.6/S5** — negativni predpomnilnik iskanja poti: v prizorišču nedosegljive tarče MSPT **17,85 → 14,46 ms (−19 %)**, alokacija 292 → 135 MB/s, a **ni enakovreden originalu** (59 % preskokov bi original odgovoril drugače) → privzeto 0 in **Q21**; nov kandidat brez spremembe obnašanja: M5.17 (S14c). **Seja 80: M5.9 zaključen** — scenarij nedosegljive tarče (varianta `nedosegljiva`, pas ograje: tarča vidna, brez poti) in instrumentacija iskanja poti: iskanje je **2,45 % ticka pri boju proti 51,18 % pri nedosegljivi tarči**, eno iskanje 3,5 proti **180,5 µs** (vanilla A* porabi ves proračun 200 vozlišč), MSPT 9,23 → **17,73 ms**, alokacija 91 → 293 MB/s → **M5.2, M5.6 in S5 dobijo podlago**. Prej seja 79: **M5.13 zaključen** — S7, prejemniki utripa oči po `world.playerEntities`: iskanje 55,42 → **0,12 µs**, idle-500 celi zagoni MSPT povp 2,824 → **2,570 ms**, p95 4,325 → **3,932**, p99 4,719 → 4,325 — vse nad šumom; enakost 0 neujemanj (4.327 primerjav v svetu, 200.000 leg v testu); `RwBlinkRecipients` privzeto 1 (D-032). Ob tem odpravljena blokada scenarijev: **prvi ukaz po zagonu serverja se je izgubljal** (gradlov posredovalnik vhoda), zdaj se vhod ogreje z `list` — Q18, Q19, Q20 odprta. Prej seja 78: **M5.12 zaključen** — S4, `onCollide` brez opazovalca preskoči poizvedbo: idle-500 p95 5,24 → **3,93 ms**, p99 6,16 → **4,13**, povp 3,53 → 3,02 (v šumu A); enakost po kodi + test + poslušalec v svetu (K2/K3); `RwCollideSkip` privzeto 1 (D-032). Prej seja 77: Q17 → **D-032**, optimizacija z dokazano enakim izidom in izmerjeno koristjo je privzeto vklopljena, `RwPathFollowCache` privzeto 1. **M5.11 zaključen** — S14b, pomnjenje tipa vozlišča v sledenju poti: boj-500 MSPT povp **8,73 → 5,88 ms (−33 %)**, p95 9,96 → 7,21, alokacija 89 → 58 MB/s, enakost 0 / 1,25 M primerjav; S14 v istem boju v šumu in odstranjen; stikalo `RwPathFollowCache` preštevilčeno (1 = S14b), privzeto 1 (D-032). Prej seja 76: **M5.10 zaključen** — neposredno izmerjeno: `pathFollow` je v boju-500 **47,7–53,6 %** ticka (prag 25 % za M5.11 presežen); na `pathFollow` vedno 0 ali 1 kandidat; šum boja je v delu na klic, ne v številu klicev. Prej seja 75: **M5.0 zaključen** — raziskava preostanka M5: boj je edina celica brez napredka, njegov strošek je ocena tipa vozlišča v vanilla sledenju poti (~1.110 branj na klic, model se ujema), lock in iskanje poti nista ozko grlo; plan M5 prepisan (D-030), Q16. Prej seja 74: **D-029** — vsak milestone se začne z raziskavo izvedbe M?.0, plan ni dogma; M5-S S14 v kodi, enakost dokazana 0/616.276, A/B nedokazan. Prej seja 73: **M5-S P1 profil JFR** — boj-500: 48 % CPU strežniške niti v vanilla `pathFollow` → `isDirectPathBetweenPoints` (27 % hash iskanje chunka), iskanje poti 2 %; idle: `Entity.move` 29 %, `onCollide` 12 %, utrip oči 6 %; nov kandidat S14, vrstni red S14 → S4 → S7. Prej seja 72: odločitvi Q13 → D-027, `RwTargetPrefilter` privzeto 2; Q15 → D-028, nosilec z NPC posadko sprejme potisk. Prej seja 71: **M5-S S2 zaključen** — poizvedba iskalnika tarč samo po igralcih, `RwTargetPrefilter=2`: idle-500 p95 10,2 → 5,2 ms, boj nevtralen) |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; znana omejitev: zadnji prihod na skupni cilj 2–6 s kasneje. Izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Trenutni milestone | **M3 — jedro entitete**: M3.1–M3.5 zaključeni (**M3.5 zaključen 24. 9.:** E9 v svetu — način 0 po sestopu 1,463, način 1 1,900). **M3.6 preusmerjen in v kodi 24. 9.:** premisa o mutex bitih ovržena (D-020), popravljena je prioriteta napada pred tavanjem pod stikalom `RwAttackPriority`. **M3.6 zaključen 5. 10.:** način 1 zelen (tavajoči v dosegu 10/10 vzorcev z zakasnitvijo 10 = kontrola), način 0 napako ponovi (združeno 6/8 nad kontrolo, mediana 25), hipoteza P2 (`aggroRange`) potrjena. M3.7 in M3.8 zaključena 5. 10. R1 popravljen pod stikalom `RwMountSteering=1` (M3.3). M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **M5.17 (S14c)** — pomnjenje tipa vozlišča med celim iskanjem poti (brez spremembe obnašanja), nato M5.2 po odgovoru na Q21; vrstni red v `03-FAZE.md` (D-030), [raziskava M5.0](raziskave/M5.0-preostanek-performance.md), [M5.11](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md). Q17 odločeno → D-032. **M4.0** je lahko vmes (raziskava, brez merjenja). Vzporedno: M3.8b GUI v klientu (z igralcem). Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Prevedljivih razredov | 35 (14 v `rework/diag`, vsi prevedeni 21. 9. v seji) — prejšnjih 21 + 14 v `rework/diag` (M2.1d doda `DiagChunkPlan` in `DiagChunkLoader`, M2.5a `SlowTicks`, **M2.7a `NavProbe` in `NavSweep`**) |
| Testi | 31 primerjalnih v obeh načinih + 16 za varne writerje/session/fault injection + **90 za instrumentacijo** (61 + **27** `NavProbeTest` + 2 za vrstico opazovalca); zeleni, zadnjič prevedeni in pognani v seji **21. 9.** (D-014; 27/27 `NavProbeTest`). Dodatno 13 preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.\ponovitve-samotest.ps1`, M2.5c, zelene 18. 9. v oblačnem PowerShellu) |
| Odprti pojavi | **nobenega blokirnega.** P1 je 21. 9. **ovržen** z meritvijo: faza D je začela natanko na z = −16,0 in leteči NPC-ji so se premikali že v prvem vzorcu (`gib = 0,1183`, prevozili 15,93). Hipoteza `pathFollow` 0,45 proti `FlyingMoveHelper` 0,5 je padla. Ostane **R-P1b**: stara zmrznitev je zahtevala postavitev izven mreže **in** 40 tickov mirovanja pred `navigateTo`; recept je zapisan, poskus (faza E) se požene šele, če ga M4 potrebuje |
| Ugotovitev M2.3 (21. 9.) | **R2 v tem prizorišču ni okvara letenja, ampak zastarela delna pot.** En sam `navigateTo` da pot, ki se konča pred oviro (`cele = 0/6`) in je nič ne zamenja; osvežena pot (faza B) in vanilla AI (faza C) isto skupino spravita čez. Popravek v M4 mora osveževati pot, ne spreminjati move helperja |
| Zagoni 23. 9. | **`nav-run` N1–N15 zelena** (z ogrevanjem: začetno pometanje G/O 2.903 / 4.343 µs, po fazi A 13.173 / 16.572 µs — razlika je ponovljiva, ne šum; A/B primerjati samo začetna pometanja). R2 ponovljen: faza A G 1/8, faza B 6/8. **`rwdiag-run` zelen, 0 padlih** (S1–S7 prvič ovrednotena v svetu): MSPT p50/p95/p99 = 0,54/1,15/2,36 ms, max 58,9 ms; brez autosave ticka max 52,7 ms, p99 2,16 ms; 8 NPC-jev tika vsak tick. Dodana oznaka `dev\run\world\rework-scenarij.txt`: nav-run jo zapiše, nav-run in rwdiag-run ob njej takoj padeta (dvakrat se je zgodilo, da je scenarij tekel na napačnem svetu). Vrstni red: testworld → testworld-run → rwdiag-run → nav-run zadnji |
| Ogrevanje (M2.7b, 21. 9.) | **Peta veličina potrebuje ogrevanje iskalnika, tako kot meritev potrebuje ogrevanje chunkov.** Dva zagona pet minut narazen: `usSkupaj` zadnjih dveh pometanj 9.069 / 18.076 µs proti 4.579 / 2.939 µs. Več vzorcev tega ne reši — vseh 56 ogretih vzorcev enega pometanja si deli isto stanje JVM-a. [zapis](meritve/2026-09-21-M2.7b-ogrevanje.md) |
| Formacije (M4.14a) | koda iz 18. 9. **integrirana 27. 9.** na M3.6 (odločitev **D-021**, prej lokalno D-017); +15 razredov v `rework/formation`, 21 testov; v svetu še ne pognana |
| Šumni pas (M2.5c, 18. 9.) | **46 veličin od 59 ima razpon nič** čez tri ponovitve; vse, kar opisuje vedenje skupine in kakovost poti, je deterministično do enega ticka. Šumna je samo veličina 5 (µs na iskanje), do **114 %** — zato A/B na ceni iskanja (M4.11, M5.6) do M2.7b ni merljiv. [zapis](meritve/2026-09-18-M2.5c-ponovitve-nav.md) |
| Odprta vprašanja iz M2.7 | dva NPC-ja od osmih na progi z grlom ne prispeta niti ob osveženi poti (zamašek ali `canNavigate()`, loči M3.1); najpočasnejši tick meritve (232 ms) ni bil autosave, ampak tick s pathfindingom |
| Blokade | Q1 je 17. 9. zabeležena kot **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6–Q8, Q10 in Q12 odprta; specifična R9 forenzika je po navodilu uporabnika odložena, ne blokirana |
| Omejitev orodij | **spremenjeno 5. 10.**: seja teče na uporabnikovem Windows računalniku (DESKTOP-SN494C0) s PowerShellom, Gradlom in JDK 8 — **sama poganja build, `verify-package` in scenarije v svetu**. Pred zagonom scenarija odstrani `NoDefaultCurrentDirectoryInExePath` (glej `OKOLJE.md`). Python ni nameščen (node je). Starejši opis (linuxova lupina brez Gradla, 17. 9.) velja za oblačne seje |

---

## Napredek po milestonih

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **zaključeno** | M0.1–M0.7 narejeno (+ M0.2r obnova okolja); M0.8 zabeležen kot blokada z opisanim vplivom |
| M1 Integriteta podatkov | **zaključeno** | M1.1–M1.3, M1.5, M1.6 in M1.9 narejeni; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | **zaključeno 5. 10.** (razen M2.4r render, prestavljen v M5-K kot klientski harness) | M2.1–M2.7 preverjeni v svetu; **baseline M2.6 zelen 5. 10.** (3 ponovitve × 9 celic, B1–B3 in P0–P9) — [baseline](meritve/baseline-2026-10-05.md) |
| M3 Jedro entitete | **v teku** — M3.1–M3.5 zaključeni (vzrok R1 potrjen, R1 popravljen pod stikalom, regresija zelena v načinih 0/1/2, hitbox po sestopu); M3.6 zaključen 5. 10. (D-020, `RwAttackPriority`), M3.7 zaključen brez spremembe, M3.8 zaključen (`RwHitbox`), M3.8b strežniško zelen 7. 10. (GUI v klientu odprt), **M3.9 zaključen 7. 10.** (Q15 odprto) | analiza narejena in **reprodukcija R1 obstaja**; glej R1 in R6 |
| M4 Gibanje | **M4.14a v kodi** (izven vrstnega reda, na zahtevo uporabnika) | R2 analiza narejena; formacije (M4.14) prevedene in testirane v simulaciji, v svetu še ne — [zasnova](06-FORMACIJE.md), [scenarij](scenariji/M4.14-formacije.md) |
| M5 Performance | **M5.0 zaključen 8. 10.** (plan prepisan, D-030); **M5.10 zaključen 9. 10.** (`pathFollow` 50 % ticka boja-500 → M5.11 se izvede); **M5.11 zaključen 9. 10.** (S14b: boj-500 MSPT povp 8,73 → 5,88 ms, p50 5,8 proti baselineu 8,9; S14 odstranjen; privzeto 1 po D-032); **M5.12 zaključen 9. 10.** (S4: idle-500 p95 5,24 → 3,93 ms, p99 6,16 → 4,13; privzeto 1); **M5.13 zaključen 10. 10.** (S7: iskanje prejemnikov 55,42 → 0,12 µs, idle-500 MSPT povp 2,824 → 2,570 ms, p95 4,325 → 3,932, vse nad šumom; privzeto 1); **M5.9 zaključen 10. 10.** (nedosegljiva tarča: iskanje poti 51 % ticka proti 2,45 % v boju → M5.2/M5.6/S5 dobijo podlago); **M5.6/S5 zaključen 10. 10.** (−19 % MSPT v tem prizorišču, a sprememba obnašanja → privzeto 0, Q21); **S1 zaključen 6. 10.** (`RwTargetPrefilter`: idle-500 alokacija 342 → 21 MB/s, MSPT p95 41,9 → 9,96 ms, boj-500 nevtralen); **S2 zaključen 7. 10.** (način 2: p95 → 5,24 ms, µs/NPC 9,7 → 6,6, ticki >10 ms 370 → 6, boj nevtralen); **profil JFR 8. 10.** (nov vrstni red S14 → S4 → S7; S3 in S8 umaknjena) | del že pokrit z M1.3, glej meritve; razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | ostanejo script API/hooki, uskladitev verzije CNPC, reload; [plan](08-PREGLED-IN-PLAN.md) |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

### M1 po paketih

| ID | Paket | Stanje |
|---|---|---|
| M1.1 | Karakterizacijski testi, ki dokumentirajo napake | **narejeno** |
| M1.2 | `javap` verifikacija sumljive logike | **narejeno** — dve hipotezi ovrženi |
| M1.3 | Nov tipno varen NBT↔JSON serializer + fuzz testi | **narejeno** |
| M1.4 | Bralnik za že pokvarjene datoteke, popravek tipov kjer je mogoče | **odloženo** — ni vhodnih datotek, R9 je minoren |
| M1.5 | `SafeFileWriter` + preklop vseh controllerjev (B1) | **narejeno** — clone, player, sinhroni JSON in stisnjeni NBT |
| M1.6 | Lifecycle asinhronih zapisov (B2) | **narejeno** — session executor, zajeta pot/snapshot, drain pred resetom |
| M1.7 | Verzioniranje `SaveFormat` + migracija | **ni več nujno za R9** — format nespremenjen |
| M1.8 | `.\dev.ps1 auditClones` | **odloženo** — brez konkretnih poškodovanih datotek |
| M1.9 | Fault injection testi | **narejeno** — 9 determinističnih odpovednih scenarijev |

### M2 po paketih

| ID | Paket | Stanje |
|---|---|---|
| M2.1a | `rework/diag` jedro + zbiralnik na Forge dogodkih + ukaz `/rwdiag` | **zaključeno** — D1–D7 zelena, `.\rwdiag-run.ps1` |
| M2.1b | Klicna mesta za pot, skripte in AI taske | **čaka na prenos** `EntityNPCInterface`/`ai` (M3.1) in `ScriptContainer` (M5.1) |
| M2.1c | Števci za razčiščenje `npc.per.tick` = 0 | **zaključeno** — vzrok imenovan in dokazan, glej meritev |
| M2.1d | Pogoj meritve: `ForgeChunkManager` ticket za chunke z merjenimi NPC-ji | **zaključeno** — C1–C6 zelena v svetu, prva veljavna meritev obstaja |
| M2.2 | Reprodukcija R1 (8 jahačev na 8 nosilcih + kontrolna skupina brez jahačev) | **zaključeno** — E1–E6 zelena, R1 reproduciran; [meritev](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| M2.3 | Reprodukcija R2 (leteči NPC in ovira) | **zaključeno 21. 9. — L1–L12 zelena v štirih fazah, R2 reproduciran in obhod izmerjen, P1 ovržen**. Štirje zagoni; odprt ostane samo R-P1b. [meritev](meritve/2026-09-17-M2.3-R2-reprodukcija.md), [scenarij](scenariji/M2.3-R2.md) |
| M2.5a | Pripis počasnih tickov: `SlowTicks` + merila S1–S4 v `rwdiag-run.ps1` | **koda in testi narejeni** (22 testov, prevedeno v seji 16. 9.); čaka na prvi zagon v svetu |
| M2.4 | Merilni scenariji 50 / 200 / 500 NPC-jev | **zaključeno 23. 9.** — P1–P7 zelena v vseh 9 celicah (preverba 20+60 s); [meritev](meritve/2026-09-23-M2.4-obremenitve-preverba.md), [scenarij](scenariji/M2.4-obremenitve.md). Render (M2.4r) odprt |
| M2.5 | Merilni protokol kot skripta | **zaključeno** — M2.5a (pripis počasnih tickov), M2.5b (izločitev autosave ticka) in M2.5c (protokol ponovitev) narejeni; vsi trije čakajo na zagon v svetu |
| M2.5b | Izločitev autosave ticka: `server.tick.ns.nosave` + merila S5–S7 | **koda in testi narejeni** (6 testov, prevedeno v seji); čaka na prvi zagon v svetu |
| M2.5c | **Protokol ponovitev**: zapis zagona (`meritve-lib.ps1`), združevanje N zagonov v svežem svetu in šumni pas (`ponovitve-run.ps1`), merila T1–T6 | **zaključeno 18. 9. — serija pognana, T1–T6 zelena**; tabela M2.7 ima razpon; [scenarij](scenariji/M2.5c-ponovitve.md), [meritev](meritve/2026-09-18-M2.5c-ponovitve-nav.md) |
| M2.7b | Več vzorcev za veličino 5 (µs na iskanje) | **pognano dvakrat 21. 9.; razdelitev na `prvi*`/`pon*` in `usSkupaj` delujeta, več vzorcev pa šuma ni zaprlo** — vzrok je stanje JVM-a med pometanji. Dodano **ogrevanje iskalnika** (`-OgrevalnihPometanj 2` + `rwdiag reset`) in merilo **N15**; N14 prepisan iz časovnega praga v strukturno invarianto. Čaka na ponovni zagon in serijo. [meritev](meritve/2026-09-21-M2.7b-ogrevanje.md), [scenarij](scenariji/M2.7-navigacija.md) |
| M2.6 | Baseline originala | **zaključeno 5. 10.** — 3 ponovitve × 9 celic, B1–B3 in P0–P9 zelena; [tabela](meritve/baseline-2026-10-05.md), [strojni zapis](meritve/2026-10-05-M2.6-baseline.json). Pred tem M2.6b (zaščita zagona) in popravek čiščenja (slay radij 120 → 2000, P0) |
| M2.7 | **Merila kakovosti navigacije** (šest veličin, izmerjenih na originalu) | **zaključeno 17. 9. — N1–N12 zelena, tabela obstaja**; [meritev](meritve/2026-09-17-M2.7-navigacija-baseline.md), [scenarij](scenariji/M2.7-navigacija.md). Vhodni pogoj za M4.10 je dopolnjen: potrebno je prizorišče z razdaljo čez `NpcNavRange` |

---

## Dnevnik sej

### 2026-10-10 (81) — M5.6 / S5: negativni predpomnilnik iskanja poti

**Paket:** M5.6 (polovica: S5) · **Stanje:** končano, odločitev pri uporabniku (Q21)

**Narejeno:**
- `NegativePathCache` (`rework/nav`) + spomin na zadnje neuspelo iskanje v navigatorju
  (`RwPathNavigateGround`, ena reža, brez alokacije): po iskanju brez cele poti isti NPC za (skoraj)
  isto tarčo `ttl` tickov ne išče — `getPathToPos` vrne `null`. Stikalo `RwPathNegCache` (0/1/2),
  ukazi `/rwpath neg|negttl|negtol|negdelne`, `perf-run -RwPathNeg N [-RwPathNegTtl T] [-RwPathNegDelne]`
  z merili N1/N2, test `NegativePathCacheTest` (5). Build zelen (test 250), `verify-package` PASS (65).
- **Velikost spremembe obnašanja (način 2, `nedosegljiva`-500):** neuspeh = `null` ali delna pot,
  20 tickov → 195.402 primerjav, **66,8 %** neujemanj; neuspeh = samo `null`, 20 tickov → 106.996
  primerjav, **58,6 %**; samo `null`, 4 ticki → **0 preskokov** (NPC-ji isto tarčo iščejo na ~7 tickov,
  ne na 3). Trajanja, pri katerem bi bil S5 »skoraj enak«, torej ni.
- **A/B (3 + 3, `nedosegljiva`-500, pravilo samo `null`, 20 tickov):** MSPT povp **17,854 → 14,462 ms
  (−19 %)**, p50 17,826 → 14,418, p95 22,02 → **18,35**, p99 24,64 → 20,45, µs/NPC 35,57 → 28,79,
  alokacija 292,5 → **135,2 MB/s**, ticki > 25 ms 45 → 10; preskočenih 17,46 iskanj na tick (35 %).
  Najslabša ponovitev B je boljša od najboljše A pri vseh štirih veličinah ticka.
  [zapis](meritve/2026-10-10-M5.6-negativni-predpomnilnik.md)

**Ugotovitve:**
- S5 odstrani petino ticka in polovico alokacij v pathološkem prizorišču, a **ni enakovreden
  originalu** → po D-007 privzeto 0 in **Q21** za uporabnika (priporočilo: da, ker je drenjanje ob
  oviri tudi vizualno slabše).
- Napoved M5.0, da ponovitve poganja `shouldExecute` (vsak 3. tick), je popravljena: prevladuje
  `updateTask` → `tryMoveToEntityLiving` vsakih 4–10 tickov, ker AI task **teče** z delno potjo.
- Nov kandidat **M5.17 (S14c)**, ki obnašanja **ne** spremeni: S14b pomni ocene tipa vozlišča samo med
  `isDirectPathBetweenPoints`, isto pomnjenje čez cel `findPath` pa napada 190 µs na iskanje
  neposredno. Dodan v `03-FAZE.md`.
- Druga polovica M5.6 (deljenje poti med NPC-ji) ostaja odprta; po S5 in S14c bo imela manjšo osnovo.

**Spremembe obnašanja:** S5 je sprememba obnašanja pod stikalom, privzeto 0 (D-007).
**Meritve:** M5.6 (tabela Meritve).

**Naslednja seja:** M5.17 (S14c) ali M5.2 (proračun iskanj) po odgovoru na Q21.

### 2026-10-10 (80) — M5.9: scenarij nedosegljive tarče

**Paket:** M5.9 · **Stanje:** končano

**Narejeno:**
- Nova varianta `perf-run.ps1 -Variants nedosegljiva`: kot `boj`, le da je okoli skupine B **pas ograje**
  (`minecraft:fence`), debel 3 bloke. Ograja je pathfindingu zaprta (`PathNodeType.FENCE`), visoka pa
  1,5 bloka, zato žarek med očmi NPC-jev (~1,62) gre nad njo — tarča je **vidna in nedosegljiva** hkrati.
  Merila U0 (ograja je res v svetu, `testforblock` štirih strani in proste notranjosti), U1 (NPC-ji imajo
  tarčo), U2 (nihče ni ranjen), U3 (merjenje je štelo).
- Instrumentacija `PathSearch` (`rework/nav`) + `RwPathNavigateGround.getPathToPos`: izmeri in razvrsti
  **vsak** klic iskanja poti (vsi klici gredo skozi `getPathToPos`) — *iz obstoječe poti* / *brez poti* /
  *cela* / *delna*. Ukaz `/rwpath iskanje 1`, `perf-run -RwPathIskanje`, test `PathSearchTest` (3).
  Build zelen (test 245), `verify-package` PASS (65).
- **Izid (ena celica na varianto v istem zagonu, 500 NPC-jev, 300 s):** iskanje poti je **2,45 % ticka pri
  `boj` in 51,18 % pri `nedosegljiva`**; eno iskanje **3,5 proti 180,5 µs**; MSPT povp 9,23 → **17,73 ms**,
  p95 10,75 → 22,02; alokacija 91 → **292,6 MB/s**; celih poti 219.335 proti **0**.
  Pri 50 NPC-jih 71,1 % ticka. [zapis](meritve/2026-10-10-M5.9-nedosegljiva-tarca.md)
- Razlaga je v vanilla kodi **[K]**: `PathFinder.findPath` se ustavi po **200 iteracijah** A\*, zato
  neuspelo iskanje porabi ves proračun, uspelo pa konča takoj (cilj je sosednji blok). `null` vrne, ko ni
  našel nič bližjega od začetka, sicer delno pot.

**Ugotovitve:**
- **M5.2, M5.6 in S5 dobijo podlago** (pogoj iz M5.0 §5 je izpolnjen: 51 % ni šum). **S5 (negativni
  predpomnilnik) je najcenejši prvi korak**; je pa sprememba obnašanja → po D-007 stikalo in privzeto
  original (D-032 tu ne velja).
- Celico `nedosegljiva` je treba vključiti v M5.8 (kapaciteta brez nje je optimistična).
- Scenarij je imel tri padle zasnove, vse zapisane v `M2.4-obremenitve.md`: ograja tesno ob B (udarec
  skozi njo, doseg 2), odmik 3 bloke (obe skupini prideta do ograje in sta spet 2 bloka narazen) in
  **napačno ime bloka** (`minecraft:oak_fence` je ime iz 1.13; v 1.12.2 je `minecraft:fence`). Tretja je
  pol ure tiho merila navaden boj — zato je zdaj merilo U0, ki ograjo preveri v svetu.

**Spremembe obnašanja:** nobene (samo merjenje; privzeto izklopljeno). **Meritve:** M5.9 (tabela Meritve).

**Naslednja seja:** M5.6/S5 (negativni predpomnilnik poti) ali M4.0 (raziskava M4).

### 2026-10-10 (79) — M5.13: S7, prejemniki utripa oči po `playerEntities`

**Paket:** M5.13 · **Stanje:** končano

**Narejeno:**
- `ModelEyeData` prevzet v `src/patch` kot bitna kopija (ločen commit, `ModelEyeDataBaselineTest` (4) teče
  tudi v `:testOriginal` proti nespremenjenemu binarju).
- `AssociatedPlayers` (`rework/net`): prejemniki paketa `EYE_BLINK` se poiščejo z obhodom
  `world.playerEntities` z istim izborom kot vanilla poizvedba (`NOT_SPECTATING` = `!isSpectator()`,
  `addedToChunk` + `isChunkLoaded`, presek škatel), namesto pregleda **450 chunkov** na iskanje. Telo
  pošiljanja je nespremenjeno prepisano iz originala; `Server.java` ni prevzet (CFR opozorilo na
  `writeNBT`/`readNBT` — prevod celotne mrežne plasti zaradi ene vrstice je nesorazmerno tveganje).
  `RwWorldAccess` dobi `isChunkLoaded` (isti vzorec kot `pathListener`, M3.1).
- Stikalo `RwBlinkRecipients` (0/1/2), ukaz `/rwblink` (način, števci, `cas`, `poskus`),
  `perf-run -RwBlink N`, `-RwBlinkCas`, `-RwBlinkAB a,b` z merili B1–B5, AB3, AB4.
  Build zelen (test 242), `verify-package` PASS (65).
- **Enakost:** `AssociatedPlayersTest` (6) na **200.000 naključnih legah** preveri tisti del dokaza, ki ga ni
  mogoče prebrati iz ene vrstice — da je vanilla izbor chunkov in y-rezov konservativen (meje chunkov
  ±0,001, lege pod y = 0 in nad 255); model vanille je prepisan iz vira Forge 14.23.5.2847. V svetu:
  način 2 pri idle-500 **3.939 primerjav, 0 neujemanj**, `/rwblink poskus` 4 × **320 primerjav, 0 neujemanj**.
- **A/B iskanja v istem zagonu** (izmenjujoča se okna po 15 s, kot M5.11): iskanje prejemnikov
  **55,42 → 0,12 µs** (0,002×), iz ticka izgine **0,167 ms** = 5,7 % ticka; razpon iskanj na tick med
  načinoma 0,2 % (AB4). Ujema se z napovedjo profila JFR (6,0–6,5 %).
- **A/B celih zagonov** (3 + 3, idle-500, `RwTarget 2`): MSPT povp **2,824 → 2,570 ms**, p50 2,621 → 2,359,
  p95 4,325 → **3,932**, p99 4,719 → 4,325, µs/NPC 5,54 → 5,05, alokacija nespremenjena (20,7 MB/s).
  Najslabša ponovitev B je boljša od najboljše A pri **vseh štirih** veličinah ticka — prvi paket M5, kjer
  ločitev drži povsod. [zapis](meritve/2026-10-10-M5.13-prejemniki-utripa-oci.md)
- Po D-032 privzeto `RwBlinkRecipients=1`; `dev/run` config ostane izrecno 0.

**Blokada paketa (najdena in popravljena):**
- Scenarij trikrat ni mogel niti postaviti sveta: **prvi ukaz po zagonu serverja se izgubi**. Vhod gre
  PowerShell → `cmd.exe` → `gradlew.bat` → gradle (ki zaradi `org.gradle.jvmargs` tudi z `--no-daemon`
  forka build JVM) → javaexec serverja; gradlov posredovalnik vhoda vrstico, zapisano preden se priključi,
  pogoltne — do serverja pride samo konec vrstice in ta odgovori `Unknown command`. Zato `perf-run` po
  `Done (` ogreje vhod z `list`, dokler server ne odgovori (prvi poskus pade po 5 s, drugi uspe).
  Prej je uspevalo zato, ker je bil zapis vedno dovolj pozen; danes se server zažene v 0,9 s.
- `-RwPathAB`/`-RwBlinkAB 0,1` brez narekovajev PowerShell poda kot niz `"0 1"`, zato se argument zdaj deli
  tudi po presledku (prej bi `-RwPathAB 0,1` padel na pretvorbi v `Int32`).
- Isti popravek še ni v ostalih desetih scenarijskih skriptah → **Q18**.

**Ugotovitve:**
- 3,01 iskanja na tick pri 500 NPC-jih se ujema s kodo: po utripu je še 20 tickov premora, zato je perioda
  140 + ~26 tickov (500/166 = 3,01), ne 500/140.
- Alokacija se ne spremeni: strošek je iskanje po chunkih, ne smeti (enako kot pri S4).
- Utrip oči ni več kandidat; preostanek idle-500 je vanilla `Entity.move` (M5.15/M5.16) in
  `EntityDataManager` (M5.14).
- Česa meritev ne pokrije: neprazen seznam prejemnikov v svetu (scenarij nima igralcev) → **Q20**.

**Spremembe obnašanja:** nobene (isti prejemniki; privzeto 1 po D-032). **Meritve:** M5.13 (tabela Meritve).

**Naslednja seja:** M5.9 (scenarij nedosegljive tarče) ali M4.0 (raziskava M4).

### 2026-10-09 (78) — M5.12: S4, `onCollide` brez opazovalca

**Paket:** M5.12 · **Stanje:** končano

**Narejeno:**
- `CollideSkip` (`rework/entity`): NPC brez vklopljenih skript preskoči poizvedbo v `onCollide`, ko na
  `WrapperNpcAPI.EVENT_BUS` ni poslušalca za `CollideEvent` (preverjeno ob vsakem klicu prek `ListenerList`).
  Stikalo `RwCollideSkip` (0/1/2), ukaz `/rwcollide` s poskusnim poslušalcem, `perf-run -RwCollide N` (K1–K3),
  `CollideSkipTest` (3). Build zelen (263), `verify-package` PASS (64).
- Skripte-500, način 2: 161.000 odločitev, nobena brez opazovalca (NPC-ji s skriptami ostanejo original).
- Idle-500 A/B (3 + 3): p95 5,24 → **3,93 ms**, p99 6,16 → **4,13**, povp 3,53 → 3,02, µs/NPC 6,85 → 5,84;
  K2/K3 zelena v vseh treh ponovitvah. [zapis](meritve/2026-10-09-M5.12-oncollide-brez-opazovalca.md)
- Po D-032 privzeto `RwCollideSkip=1`; `dev/run` config izrecno 0.

**Ni narejeno in zakaj:**
- NPC-ji s skriptami (skripte ~15 % CPU v `onCollide`) niso pokriti: potreben bi bil vpogled v `ScriptContainer`
  (ali skripta ima `collide`) in obravnava `onNPCInit` po ponovnem nalaganju → kandidat S4b, ni v planu.

**Ugotovitve:**
- Povprečje −14 % se ujema s profilom (12 %), a je v razponu A (A je bil v tej seriji šumen, 20 %); repa sta jasno
  nad šumom.
- Dvakrat P9 zaradi Gradle builda `ladja_mod`; zaganjalnik je v istem procesu podedoval `$LASTEXITCODE` padle
  serije (lažen padec `testworld.ps1`) → serije zdaj vsaka v svojem procesu.

**Spremembe obnašanja:** nobene (isti izid; privzeto 1 po D-032). **Meritve:** M5.12 (tabela Meritve).

**Naslednja seja:** M5.13 (S7) ali M4.0 (raziskava M4).

### 2026-10-09 (77) — M5.11: S14b, pomnjenje tipa vozlišča v sledenju poti

**Paket:** M5.11 · **Stanje:** končano

**Narejeno:**
- `RwWalkNodeProcessor` (node procesor vsakega kopenskega NPC-ja, `RwPathNavigateGround.getPathFinder` kot vanilla) si
  med enim klicem `isDirectPathBetweenPoints` zapomni `getPathNodeType(x, y, z)` po poziciji (`NodeTypeMemo`); vanilla
  metoda se kliče nespremenjena. Stikalo `RwPathFollowCache` 1 = S14b, 2 = preverba, privzeto 0.
- Števci po načinu in ticki v vsakem načinu (`/rwpath ... poNacinu=`), `perf-run.ps1 -RwPathAB a,b -RwPathOkno N`
  (A/B v istem boju, merili AB1/AB2). Testi `NodeTypeMemoTest` (6), `PathFollowCacheTest` (+2).
- Enakost v svetu dvakrat: 618.004 in 627.601 primerjav, **0 neujemanj**; 3,03 ocene na izračun (model 3,07).
- A/B v istem boju (0/S14b/S14, okna 20 s, 3 ponovitve): µs na kandidata 0,37–0,41× originala, pathFollow
  −2,9 do −3,5 ms/tick (razpon originala 0,73); S14 −0,2 do −0,95 ms, v šumu.
- **S14 odstranjen** (`MemoBlockAccess`, `ChunkMemo`, `ChunkMemoTest`, prepis vanilla metode), načini preštevilčeni.
- A/B celih zagonov na končni kodi (3 + 3, `ab-boj.js` pogoj izpolnjen, odmik klicev ≤ 1,9 %): MSPT povp
  **8,73 → 5,88 ms**, p50 8,65 → 5,77, p95 9,96 → 7,21, µs/NPC 17,3 → 11,6, alokacija 89 → 58 MB/s.
  Build zelen (260), `verify-package` PASS (64). [zapis](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md)

**Ugotovitve:**
- Boj-500, edina celica brez napredka po M5.0, je zdaj p50 5,8 ms proti 8,9 v baselineu M2.6.
- Šum boja se z S14b skoraj izgubi (razpon MSPT povp 0,52 → 0,07 ms): del na klic, ki ga je M5.10 označil za
  vir šuma, so bile ponovljene ocene [H].
- `pathFollow` ostane 34,5 % ticka; naslednji vzvod je S15 (M4.13, sprememba gibanja) ali S15b.
- Zaganjalnik je 4 ure čakal na obvisel `ladja_mod` proces (brez okna); uporabnik ga je dovolil ustaviti.

**Spremembe obnašanja:** nobene (S14b da isti izid; privzeto 0). **Meritve:** M5.11 (tabela Meritve).

**Odločitev uporabnika v isti seji:** Q17 → **D-032** — optimizacija z dokazano enakim izidom in izmerjeno koristjo
je privzeto vklopljena (D-007 velja še za spremembe obnašanja); `RwPathFollowCache` privzeto 1, `dev/run` config
ostane izrecno 0.

**Naslednja seja:** M5.12 (S4) ali M4.0 (raziskava M4).

### 2026-10-09 (76) — M5.10: delež sledenja poti v boju, neposredno

**Paket:** M5.10 · **Stanje:** končano

**Narejeno:**
- `RwPathNavigateGround`: `pathFollow` prepisan samo za merjenje (vedno `super`), ns okoli `pathFollow` in vsakega
  kandidata `isDirectPathBetweenPoints` v katerem koli načinu, histogram kandidatov na `pathFollow` (0…5+);
  `PathFollowCache` števci, `/rwpath cas 0|1` (privzeto izklopljeno), odgovor ima še ticke od `reset`.
  `PathFollowCacheTest` (3). Build zelen (`test` 225, `testOriginal` 31; test izključen iz `testOriginal` kot
  `ChunkMemoTest`), `verify-package` PASS (64).
- `perf-run.ps1 -RwPathCas`: delež povprečnega ticka v `pathFollow`/kandidatih, klici na tick, µs na klic,
  `rwpath.hist.0`–`5`, merilo M1, odtis `rwpathCas`. `dev/tools/ab-boj.js`: pogoj veljavnosti A/B boja.
- Serija boj-500 × 3 (`RwTarget 2`, `RwPath 0`), T1–T6 in P1–P9 zelena.
  [zapis](meritve/2026-10-09-M5.10-delez-sledenja-poti.md)

**Ugotovitve:**
- `pathFollow` = **47,7 / 53,6 / 50,0 %** povprečnega ticka (4,0–4,9 ms od 8,0–9,2), skoraj ves v kandidatih.
  JFR (48 %) je imel prav za celoto, precenil je le iskanje chunka znotraj klica (S14).
- Na `pathFollow` vedno **0 ali 1 kandidat** in vsak uspe → napoved 5–8× ponovljenih ocen pri več kandidatih v
  boju ne velja; S14b zadošča za en klic.
- Klici na tick se med ponovitvami razlikujejo za 1,1–1,4 %, MSPT za 14 %, µs na kandidata za 24,5 % (p2 ima tudi
  največ alokacije) → šum boja je v **delu na klic** (geometrija boja, [H]), pogoj ±10 % klicev iz M5.0 je nujen,
  ne zadosten. A/B v M5.11 zato pri istem boju (preklapljanje načina med tekom, števci po načinu).
- Prvi poskus serije je P9 zavrnil (vzporedni testi `ladja_mod`); zaganjalnik je nato čakal 5 min mirnega stroja.

**Spremembe obnašanja:** nobene (merjenje privzeto izklopljeno, `pathFollow` vedno `super`).

**Meritve:** M5.10 (tabela Meritve).

**Naslednja seja:** M5.11 — S14b z A/B pri istem boju (ali M4.0, raziskava M4).

### 2026-10-08 (75) — M5.0: raziskava preostanka M5

**Paket:** M5.0 (D-029) · **Stanje:** končano

**Narejeno:**
- [Zapis raziskave](raziskave/M5.0-preostanek-performance.md) z oznakami [K]/[M]/[H]/[W]: primerjava z
  baselineom, analiza vanilla sledenja poti, idle/skripte po profilu, kritika M5.1–M5.7 in S-seznama.
- `dev/tools/model-sledenje-poti.js`: model zank vanilla `isDirectPathBetweenPoints` (ravna tla,
  NPC 0,6 × 1,9) — ~1.100–1.140 branj blokov na klic, izmerjeno 1.110 (S14).
- Plan M5 prepisan (`03-FAZE.md`, D-030), R5 v `02-ZAHTEVE.md` dopolnjen s stanjem po meritvah, M4.13 dobi
  vhod S15, M6.6 prevzame M5.1. Q16.

**Ugotovitve:**
- Idle-500 p95 44,0 → 5,1 ms proti baselineu; **boj-500 je v razponu baselinea** (p50 7,3–8,9 proti 8,9 ms),
  torej edina celica brez napredka.
- ~90 % branj v `isDirectPathBetweenPoints` je prvo okno (3 × 3 stolpcev s kvadrom 3 × 2 × 3, ker vanilla
  podaljšano širino poda tudi v oceno kvadra). Isto mesto se oceni ~3× na klic in 5–8× na `pathFollow` z več
  kandidati → S14b. ~56 % alokacij boja (`BlockPos` v `getPathNodeTypeRaw`/`getPathNodeType`) je tu.
- Vanilla 1.21.1 (lokalni viri Forge 52.1.14) kopenskim mobom med sledenjem poti ne bere blokov
  (`followThePath` po razdalji in smeri, `canMoveDirectly` = false) in ima `PathTypeCache` (4.096 mest,
  invalidacija ob spremembi bloka). Pri nas bi bilo prvo sprememba gibanja → M4.13.
- Boj A/B je šumen zaradi poteka boja (baseline razpon p50 20 %, S14 B2 alokacija 36,7 proti 90 MB/s); nov
  pogoj veljavnosti: klici sledenja poti v obeh vejah ±10 %.
- `onCollide` v idle je skoraj samo poizvedba (dogodki ~2 % alokacij) → premisa S4 drži. Vanilla
  `Entity.move` (26–29 %) brez vanilla mixinov ni dosegljiv (Q16); Lithium ga za novejše verzije pospeši,
  za 1.12 portov ni.

**Spremembe obnašanja:** nobene. **Meritve:** nobene nove (model je izračun, ne meritev).

**Odločitev uporabnika v isti seji:** Q16 → **D-031** — vanilla/Forge posegi so dovoljeni, če so zapakirani v CNPC
jar (lasten coremod z ASM; MixinBooter kot zunanji mod in vgrajen Mixin zavrnjena zaradi odvisnosti oz. trkov v
modpackih). Dodana paketa M5.15 (infrastruktura coremoda) in M5.16 (`Entity.move` poizvedba entitet za trke).
Pričakovan dobitek M5.16 je zmeren (idle do ~12 % CPU strežniške niti, ki je že ~3 ms/tick), zato je za S14b, S4, S7.

**Naslednja seja:** M5.10 (meritev sledenja poti v boju), ali M4.0 (raziskava M4).

### 2026-10-08 (74) — M5-S S14 (delno) in D-029: raziskava pred vsakim milestonom

**Paket:** M5-S S14 · **Stanje:** delno — koda in dokaz enakosti narejena, učinek nedokazan

**Narejeno:**
- `rework/nav`: `RwPathNavigateGround` (navigator vsakega kopenskega NPC-ja, pri 0 kliče `super`),
  `MemoBlockAccess`, `ChunkMemo`, `PathFollowCache`, `/rwpath [0|1|2|reset]`; stikalo
  `RwPathFollowCache` (privzeto 0). `ChunkMemoTest` (4), build 253 testov zelen, `verify-package` PASS (64).
- `perf-run.ps1 -RwPath N` (merili R1/R2 za način 2).
- **Enakost v svetu:** boj-500, način 2: 616.276 primerjav, **0 neujemanj**.
- **A/B boj-500:** A 8,96 ms povp (razpon 0,30); B 8,84 in 7,85, tretja ponovitev B neveljavna (P9:
  med celico je tekel uporabnikov `ladja_mod`). Izid **nedokazano**, stikalo ostane 0.
  [zapis](meritve/2026-10-08-M5-S14-sledenje-poti.md)
- **D-029** (odločitev uporabnika): pred vsakim milestonom paket M?.0 — raziskava izvedbe (koda,
  meritve, zunanji viri), ki lahko pakete spremeni, zavrže ali doda; plan je delno nastal s šibkejšimi
  AI modeli. `05-SEJA-PROTOKOL.md` §1a, `03-FAZE.md` (pravilo, M4.0, M5.0), README.

**Ugotovitve:**
- Profil JFR je strošek iskanja chunka precenil; pravi strošek je ~1.110 branj blokov na klic
  (prekrivajoča se okna `isSafeToStandAt`) → kandidat S14b.
- Uporabnik je dovolil, da seja med meritvijo zapre tuje java procese, nato je zagon zaustavil;
  serija B ni bila ponovljena in noben proces ni bil zaprt.

**Spremembe obnašanja:** nobene (privzeto 0).

**Meritve:** S14 enakost in delni A/B (tabela Meritve).

**Naslednja seja:** M5.0 — raziskava preostanka M5 (S14/S14b, M5.1–M5.7, S4, S7), po D-029.

### 2026-10-08 (73) — M5-S P1: profil JFR strežniške niti pri 500 NPC-jih

**Paket:** M5-S P1 (manjkajoči merilni korak iz [09](09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** končano

**Narejeno:**
- `perf-run.ps1 -Jfr`: v merilnem oknu vsake celice `jcmd JFR.start settings=profile` / `JFR.stop`
  (posnetek v `audit/jfr/`, ni v gitu), merila J1–J3, odtis `jfr = 1`. Povzetki šele po ustavitvi
  serverja, da `jfr print` ne moti naslednje celice.
- `dev/tools/jfr-povzetek.js`: CPU vzorci strežniške niti (lastni, po vrstici, vključni, po prvem
  `noppes` okvirju), alokacije po razredu in klicnem mestu; `--klicatelji <metoda>` za verige.
- Zagon idle/boj/skripte × 500, `RwTarget 2`: P1–P9 in J1–J3 zelena.
  [zapis](meritve/2026-10-08-M5-S-P1-profil-jfr.md)

**Ugotovitve:**
- Profil meritve ne pokvari (idle p95 5,11 proti 5,24 brez profila, v razponu); ocena alokacij iz JFR
  (20,6 / 93,4 MB/s) se ujema z `jvm.alloc.server` (20,7 / 93,6).
- **boj-500:** 48,3 % CPU v vanilla `pathFollow` → `isDirectPathBetweenPoints`, od tega 27,3 % v
  `ChunkProviderServer.getLoadedChunk` (hash za vsak blok). Iskanje poti 2 %, iskalnik tarč 0 %.
- **idle-500:** `Entity.move` 29 % (vanilla, tudi stoječi NPC), `onCollide` 12 %, `EntityDataManager.get`
  11,6 %, utrip oči (`sendAssociatedData`, AABB 160 blokov) 6,5 %, iskalnik tarč 1,8 %.
  Alokacija ~1 MB/tick: ~55 % vanilla `AxisAlignedBB` iz `Entity.move`, CNPC ~8 %.
- `HashMap$TreeNode.root` (9 % pri idle) pod `EntityDataManager.getEntry` je pripis JIT-a (drevesni koš
  pri majhnih `Integer` ključih ni mogoč).
- Nov kandidat **S14** v 09; S3 in S8 umaknjena; M5.6/S5 imata za boj nižjo prioriteto.
- Hipoteza (nepreverjeno): višji p95 skripte proti idle (9,4 proti 5,1 ms) ni v `ScriptContainer` (1,8 %).

**Spremembe obnašanja:** nobene (orodje in stikalo scenarija).

**Meritve:** M5-S P1 profil (tabela Meritve).

**Naslednja seja:** M5-S S14 (koda + test enakosti blokov, nato A/B boj-500) ali M3.8b GUI v klientu.

### 2026-10-07 (72) — Odločitvi Q13 in Q15

**Paket:** odločitvi uporabnika (Q13 → D-027, Q15 → D-028) · **Stanje:** končano

**Narejeno:**
- **D-027:** `RwTargetPrefilter` privzeto 2 (`CustomNpcs`). `dev/run/config` (ni v gitu) ima izrecno 0,
  zato scenariji in baseline še vedno merijo original; A/B jih nastavi z `-RwTarget`.
- **D-028:** `RwHitbox.acceptsPush` / `npcCrewOnly` / masa s posadko. Nosilec z igralcem in trk dveh
  originalnih NPC-jev ostaneta vanilla.
- `m39-run.ps1`: J4 in J5 z novimi pričakovanji (J4: nosilec v0, solid 0; J5: delež po masi nosilec +
  jahač iz izpisa probe — jahač med jahanjem ima manjši hitbox, masa 0,527 namesto 0,684). Tavajočih prog
  10 + 10 namesto 3 + 3: zagon s 3 + 3 je enkrat ostal brez stika (P2), ker NPC na poti tava.
- Regresija: `hb-run` H0–H8 zelen; `m39-run` **J1–J6 in P1–P4 zelena** (22 hodečih na cilju brez teleporta,
  stik tavajočih original 100 / solid 299 tickov, premik ob stiku na tick solid 0,0004 proti original 0,0158).

**Spremembe obnašanja:** D-027 (privzeto 2, iste tarče), D-028 (pod `RwHitbox=1`).

**Meritve:** nobene nove (regresija).

**Naslednja seja:** GUI M3.8b v klientu, nato S3/S4 ali profil JFR preostale alokacije.

### 2026-10-07 (71) — M5-S S2: poizvedba iskalnika tarč samo po igralcih

**Paket:** M5-S S2 ([09](09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** končano

**Narejeno:**
- `TargetPrefilter.scanClass` + `onlyPlayers`, klic v `EntityAIClosestTarget.shouldExecute` po metu RNG;
  nov način `RwTargetPrefilter=2` (S1 + S2), `/rwtarget 2`, števec `zozenih`. Izčrpen test ekvivalence
  (3 testi), build zelen, `verify-package` PASS (64). Commit `3b712a8`.
- A/B v svetu (način 1 proti 2, idle-500 in boj-500, po 3 ponovitve, T1–T6 in P1–P9 zelena).
  [meritev](meritve/2026-10-07-M5-S2-poizvedba-igralci.md)
- **idle-500:** p95 10,22 → 5,24 ms, p99 11,27 → 5,51, povp 4,94 → 3,41, µs/NPC 9,74 → 6,60, ticki >10 ms
  370 → 6; alokacija nespremenjena (21 MB/s). **boj-500:** `zozenih = 0` (bojni NPC-ji imajo
  `AttackOtherFactions`), vse v šumu.

**Ugotovitve:**
- Pogoj je strožji od predloga v 09: prazen seznam sovražnih frakcij ne zadošča, ker bi original pri NPC-ju
  brez frakcije vrgel izjemo; S2 velja samo brez `AttackOtherFactions` (privzeto).
- Preostala alokacija idle-500 (~1 MB/tick) ni iz iskalnika tarč.
- Serija je čakala ~4 ure na uporabnikov `ladja_mod`; na zahtevo uporabnika je bil njegov Minecraft ustavljen.
  Prvi poskus idle A je P9 zavrnil (uporabnikov gradle build med celico), ponovljen.

**Spremembe obnašanja:** nobene (privzeto 0; način 2 izbere iste tarče). **Meritve:** M5-S S2 (tabela Meritve).

**Odprto za uporabnika:** Q13 (zdaj: privzeto način 2?), Q15.

**Naslednja seja:** GUI M3.8b v klientu, odločitev Q13/Q15, nato S3/S4 ali profil JFR.

### 2026-10-07 (70) — M3.9: preverba solid × jahanje

**Paket:** M3.9 · **Stanje:** končano (igralec preverjen iz kode in s testom, ne v klientu)

**Narejeno:**
- Diagnostika v `/rwhitbox`: `mount`, `dismount`, `track` (nov razred `rework/entity/HitboxTrack`,
  sledilnik na vsak tick; ob ustavitvi strežnika se počisti). Test `igralecDobiNajvecDvakratVanilla`.
- Scenarij `m39-run.ps1` (fixture `M39_*` v `hb-fixture.js`): **J1–J6 in P1–P4 zelena**.
  [zapis](meritve/2026-10-07-M3.9-solid-jahanje.md)
- Nosilec z jahačem: vanilla mu potiska ne doda (`isBeingRidden`), `RwHitbox` to ohrani v vseh načinih;
  po sestopu se nosilec obnaša kot navaden solid.
- NPC na poti: vseh 8 hodečih pride mimo (brez teleporta), tavajoči solid NPC se ob stiku ne premakne
  (0 proti 0,0219 bloka na tick stika pri originalu).

**Ugotovitve:**
- **Q15:** jahan nosilec in solid NPC se ob trku ne odrineta nikamor (J4: 0 / 0) — konjenica se lahko
  prekriva s solid pehoto. Pri SMART se izgubi delež nosilca.
- NPC, ki stoji (`MovingState 0`), je potisku zaprt že v originalu; hodeči je v stiku z njim od spredaj
  ~6,5 s, preden zdrsne mimo — v obeh načinih enako (obstoječe obnašanje).
- Prva dva zagona neveljavna: proge izven naloženih chunkov (scenarij zdaj sam pribije world spawn) in
  vzorčenje na sekundo pregrobo (zato `track`). Med sejo je trikrat tekel uporabnikov `ladja_mod`;
  P9 je zagon vsakič zavrnil, počakano.

**Spremembe obnašanja:** nobene (samo diagnostični podukazi). **Meritve:** M3.9 (tabela Meritve).

**Odprto za uporabnika:** Q15.

**Naslednja seja:** GUI M3.8b v klientu z igralcem (tudi potisk med dvema solid NPC-jema), odločitev
Q15, nato M3.10 ali M5-S S2.

### 2026-10-07 (69) — M3.8b: regresija `hb-run` na kodi z GUI-jem

**Paket:** M3.8b · **Stanje:** delno — strežniški del zelen, GUI v klientu ni preverjen

**Narejeno:**
- Ob začetku seje je tekel uporabnikov build `ladja_mod` (gradle); počakano, da je končal (~1 min), nato
  `.	estworld.ps1` in `.hb-run.ps1`: **H0–H8 zelena**, vse hitrosti enake zagonu 5. 10.; po restartu
  original=1 solid=1 smart=2, brez ERROR iz `noppes.*`. [zapis](meritve/2026-10-05-M3.8-hitbox-nacini.md)

**Ni narejeno in zakaj:**
- GUI v klientu (Display → Hitbox → Solid, `/rwhitbox status`, ponovno odpri GUI) potrebuje igralca v
  klientu; ostane za uporabnika ali sejo z računalniškim upravljanjem, skupaj z M3.9.

**Spremembe obnašanja:** nobene. **Meritve:** nobene nove (regresija).

**Naslednja seja:** GUI M3.8b + M3.9 v klientu, ali naslednji strežniški paket M5-S (S2 kratki stik
»ni možne tarče« ali profil JFR za boj, [09](09-PERFORMANCE-RAZISKAVA.md)).

### 2026-10-06 (68) — Nova zahteva R10 kot zadnja faza M11 (samo plan)

**Paket:** plan · **Stanje:** končano

**Narejeno:**
- Uporabnik je teoretično vprašal, ali bi se dalo vojsko (npr. 200 istoimenskih NPC-jev) prikazati
  na zemljevidu kot eno oznako in jo s klikom poslati na točko, kot pri ladjah. Ocena: izvedljivo,
  osnova 3–5 sej po M4.14b, polna verzija (dolg pohod, neaktivni chunki) še 5–10.
- Na željo uporabnika zapisano kot **zadnja faza M11** (D-026): R10 v `02-ZAHTEVE.md`, M11.1–M11.8
  v `03-FAZE.md`, README, Q14 (kateri zemljevid). Q12 kandidat preimenovan v R11.

**Ni narejeno in zakaj:** koda — uporabnik je izrecno rekel, da zdaj še ne dodajamo.

**Spremembe obnašanja:** nobene. **Meritve:** nobene.

**Naslednja seja:** nespremenjeno od seje 67 (M3.8b preverba, M3.9 ali naslednji M5-S paket).

### 2026-10-06 (67) — M5-S S1: A/B predzavrnitve tarč v svetu

**Paket:** M5-S S1 · **Stanje:** končano

**Narejeno:**
- A/B na idle-500 in boj-500, po 3 ponovitve v svežem svetu (`ponovitve-run` + `perf-run -RwTarget 0|1`),
  T1–T6 in P1–P9 zelena v vseh štirih serijah, med serijami ni tekel noben drug Java proces.
  [meritev](meritve/2026-10-06-M5-S1-predzavrnitev-tarc.md)
- **idle-500:** alokacija 342,4 → 21 MB/s, GC 2,52 → 0,10 ms/s, MSPT p95 41,9 → 9,96 ms, p99 45,1 → 10,7,
  µs/NPC 29,0 → 9,31, ticki >25 ms 2.016 → 6; ~108,8 M predzavrnjenih kandidatov v 300 s. Vse daleč
  nad razponom. A se ujema z baselinom M2.6 (koda pri stikalu 0 stroška ne spremeni).
- **boj-500:** vse razlike v šumu (p95 10,5/10,5, alokacija 91,6/91,1) → predzavrnitev v boju ne škodi.
- Popravek `perf-run.ps1`: števec `rwtarget.predzavrnjenih` se je zaradi gnezdenih oklepajev v opisu
  načina bral kot −1.

**Ugotovitve:**
- **Model iz [09](09-PERFORMANCE-RAZISKAVA.md) potrjen:** v originalu je bil nad 25 ms natanko vsak tretji
  tick (iskalnik tarč na tri tike), p50 pa ostane 3,4 ms. S tem je potrjen tudi vrstni red M5-S.
- Navodilo seje 66 je bilo napačno: `ponovitve-run` sprejme samo eno celico, `-Counts` je obvezen.
- Boj-500 p50 je danes 9,18 ms v obeh načinih, v baselinu 5. 10. 7,34 (7,34–8,91) — nepojasnjeno,
  hipoteza (stanje stroja ali spremembe po baselinu); na ta A/B ne vpliva.

**Spremembe obnašanja:** nobene nove (stikalo iz seje 66 ostane privzeto 0).

**Meritve:** M5-S S1 A/B (tabela Meritve).

**Odprto za uporabnika:** Q13 — naj bo `RwTargetPrefilter` privzeto vklopljen?

**Naslednja seja:** M3.8b preverba (`.\hb-run.ps1` po `.\testworld-run.ps1`, nato GUI v klientu), M3.9,
ali naslednji strežniški paket iz [09](09-PERFORMANCE-RAZISKAVA.md) (profil JFR za boj).

### 2026-10-05 (66) — M5-S S1: predzavrnitev v iskalniku tarč pred raytraceom

**Paket:** M5-S S1 ([09](09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** delno — koda in testi zeleni, A/B v svetu ni pognan

**Narejeno:**
- `rework/ai/TargetPrefilter` + en klic v `NPCAttackSelector.isEntityApplicable` (za preverjanjem
  dosega in zdravja, pred `canSee`). Kandidata zavrne pred raytraceom **samo, ko je gotovo**, da bi
  original vrnil `false`: ni stražarjeve tarče, ni tarče spremljevalca-stražarja, kandidat ni igralec
  (oz. je igralec v kreativnem načinu / z `disableDamage`), NPC kandidat je ubit ali ga frakcija ne
  napada. Sicer teče original nespremenjen. `NPCAttackSelector` je bil že prenesen (identičen
  reference-src), zato ločen commit prenosa ni bil potreben.
- Stikalo `RwTargetPrefilter` (privzeto 0 = original), ukaz `/rwtarget [0|1|reset]` s števcem
  predzavrnitev (marker `RWTARGET`), `perf-run.ps1 -RwTarget N` (odtis `rwtarget`, veličina
  `rwtarget.predzavrnjenih`).
- `TargetPrefilterTest` (6): izčrpno čez 1.296 kombinacij pogojev × vse razrešitve neznank ×
  vidnost/nevidnost/dom — kadar predzavrnitev zavrne, model originala vedno vrne `false` in nikoli
  ne vrže izjeme. Build 214/214 + 31/31, `verify-package` PASS (64).

**Ugotovitve (iz kode, pomembno za S1):**
- Premisa iz [09](09-PERFORMANCE-RAZISKAVA.md) (»preverjanja so brez stranskih učinkov razen
  `PlayerData.get`«) je **premalo stroga**: `Faction.isAggressiveToPlayer` →
  `PlayerFactionData.getFactionPoints` ob prvem stiku igralca s frakcijo sproži skriptni dogodek
  `FactionUpdateEvent` in zapiše točke. Original to naredi samo za **vidnega** igralca. Zato igralec,
  ki ga NPC lahko napade, ostane v originalnem vrstnem redu (raytrace pred frakcijo). Za idle-500 to ni
  pomembno (igralca ni).
- Neskladno stanje (`job == 3` brez `JobGuard`, vloga 6 brez `RoleCompanion`, frakcija `null`) v
  originalu vrže izjemo samo za vidnega kandidata; predzavrnitev o njem ne odloča.
- Edina razlika je vsebina predpomnilnika `EntitySenses` (briše se vsak tick).

**Ni narejeno in zakaj:**
- **A/B v svetu:** ves čas seje sta tekla uporabnikov `ladja_mod` (gradle + Minecraft) in Minecraft
  iz CurseForge; P9 bi zagon zavrnil, meritev bi bila tudi neveljavna. Prav tako `hb-run.ps1` (M3.8b).

**Spremembe obnašanja:** nobene privzeto. Z `RwTargetPrefilter=1` se izbrane tarče ne spremenijo
(test); spremeni se samo, za katere kandidate se izvede raytrace.

**Meritve:** nobene.

**Naslednja seja (ko ne teče noben drug Minecraft/gradle):**
1. `.\testworld.ps1`, nato A in B z isto serijo celic (po 3 ponovitve, ~80 min vsaka):
   `.\ponovitve-run.ps1 -Scenarij perf -Dodatno @('-Variants','idle','-RwTarget','0')` in
   `... '-RwTarget','1'`. Pričakovano po modelu iz 09: idle-500 alokacija s 344 MB/s proti ravni
   idle-50/boj, ticki >25 ms skoraj nič; `rwtarget.predzavrnjenih` > 0. Če alokacija ne pade, je
   model napačen → profil (JFR). Kandidat pod šumom se zavrže (M5.5).
2. M3.8b preverba (`hb-run.ps1` + GUI v klientu).

### 2026-10-05 (65c) — klientske optimizacije: ideje iz UEBS2 dopisane v seznam K

Na vprašanje uporabnika (kako UEBS2 zmore milijone enot, učinkovita klientska optimizacija): v
[09](09-PERFORMANCE-RAZISKAVA.md) dodani **K10** (instanced renderer), **K11** (tanjši klientski tick
oddaljenih), **K12** (okluzija, najprej Entity Culling), **K13** (skini v teksturnem nizu), impostor kot
zadnja stopnja **K5**; predlagan vrstni red M5-K: K0 → K5 → K12 → K13 → K10. Zniževanje ločljivosti
tekstur po gostoti ocenjeno kot neučinkovito za FPS (ozko grlo so klici na NPC, ne tekstura) — zapisano z
razlogom. Brez kode, brez meritev; vse čaka na harness K0 (M2.4r).

### 2026-10-05 (65b) — M3.8b: GUI za hitbox način

**Paket:** M3.8b · **Stanje:** delno — koda in testi zeleni, preverba v svetu in klientu ni narejena

**Narejeno:**
- Prenos `DataDisplay` in `GuiNpcDisplay` brez funkcionalnih sprememb (ločen commit): 63 metod, 58
  enakih, 3 razlika v obliki, 2 ročno pregledani (`audit/m38b-bytecode-primerjava.txt`). Orodje
  `primerjaj-bytecode` prepisano v node (`dev/tools/primerjaj-bytecode.js`), ker tu ni Pythona;
  primerjati je treba z reobfusciranim `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`.
- Vrstica **Hitbox** (Original / Solid / Smart) na zavihku Display; pot podatkov brez novih paketov
  (`rework/entity/HitboxGui`, 7 testov). [zapis](meritve/2026-10-05-M3.8-hitbox-nacini.md)
- Build 208/208 + 31/31 (`HitboxGuiTest` izključen iz `testOriginal`), `verify-package` PASS (64).

**Ni narejeno in zakaj:**
- `hb-run.ps1` je zavrnil zagon po P9: tekel je uporabnikov drug projekt (`ladja_mod`, gradle +
  Minecraft). Varovala nisem obšla. Spawn podatki so se spremenili samo za NPC-je z načinom.
- GUI v klientu z igralcem ni preverjen (tudi M3.9 potrebuje klienta).

**Spremembe obnašanja:** nobene privzeto. NPC z načinom ima v spawn podatkih ključ `RwHitboxStored`.

**Naslednja seja:** `.\hb-run.ps1` (svet je postavljen s `testworld-run`), nato v klientu: urejevalnik
NPC-ja → Display → Hitbox → Solid, zapri, `/rwhitbox status` mora pokazati solid; ponovno odpri GUI.

### 2026-10-05 (65) — M3.6 zaključen: ponovni zagon v obeh načinih

**Paket:** M3.6 (naloga 0.3 iz [08](08-PREGLED-IN-PLAN.md)) · **Stanje:** končano

**Narejeno:**
- Ponovni zagon `m36-run.ps1` po dopolnitvi scenarija 24. 9. (ločen doseg napada). Način 1 **zelen**
  (A1–A6): 10 tavajočih v dosegu, vsi z zakasnitvijo 10 tickov = kontrola. Način 0 dvakrat: vsak
  zagon posebej pade na A2 (1 oz. 7 vzorcev v dosegu, potrebnih 8 — NPC-ji so odtavali dlje);
  združeno 8 vzorcev, mediana 25 > 20, 6/8 nad kontrolo → napaka originala ponovljena.
  [zapis §6](meritve/2026-09-24-M3.6-prioriteta-napada.md)
- **P2 potrjen:** v načinu 1 so vse zakasnitve nad 20 izven `aggroRange` ob ukazu.
- Najdena past v postopku: sam `testworld.ps1` ustvari svet z vanilla spawnom (743, −231), chunki
  prog niso naloženi in `noppes clone spawn` tiho ne naredi nič. Prvi par zagonov je zato padel.
  `m36-run.ps1` zdaj takoj pade z navodilom (`testworld-run.ps1`) in ob izjemi ustavi server
  (preverjeno: 28 s, server čisto ustavljen). Navodila v scenariju popravljena.

**Ugotovitve:** čisti učinek napake prioritete pri NPC-ju v dosegu je 10–20 tickov; velike zakasnitve
prvega zagona (do 120) so bile večinoma vzorci izven dosega (P2).

**Spremembe obnašanja:** nobene nove (`RwAttackPriority` iz 24. 9.). Skripta `m36-run.ps1`: hitrejši padec.

**Meritve:** M3.6 ponovni zagon (tabela Meritve). Opomba: med zagonom b je uporabnik zagnal drug
Minecraft (`ladja_mod`); M3.6 meri v tickih, zato to na izid ne vpliva.

**Odprto za uporabnika:** popraviti P2 (utripajoč napad izven `aggroRange`)?

**Naslednja seja:** M3.8b (GUI hitbox), M3.9, M5-S S1. Svet v `dev/run` je trenutno prazen (po
preverbi padca) — pred naslednjim scenarijem `.\testworld-run.ps1`.

### 2026-10-05 (64b) — M3.8: hitbox načini SOLID in SMART (R6)

**Paket:** M3.8 · **Stanje:** končano (GUI v M3.8b)

Uporabnik je R6 dopolnil: poleg obstoječega odrivanja (ORIGINAL) **SOLID** (NPC-ja odrivanje ne premakne) in **SMART** (moč odrivanja po velikosti hitboxa, ščit v roki ga oteži; kompatibilnost s ščiti modov). Zasnova **D-025**: krmili se delež vanilla potiska v `applyEntityCollision`/`collideWithEntity`, ne trdi collision box. Stikalo `RwHitbox` (privzeto 0), per-NPC NBT `RwHitboxMode`, ukaz `/rwhitbox`.

**Preverjeno:** 201/201 + 31/31 testov, `verify-package` PASS, scenarij `hb-run.ps1` H0–H8 zelen (smart 5 proti 15: 1,93 v0 / 0,07 v0; ščit 4/3 / 2/3; solid 0). Scenarij je našel in popravljen je bil hrošč: trk, ki ga sproži originalni NPC, je solid NPC-ja odrinil. [zapis](meritve/2026-10-05-M3.8-hitbox-nacini.md)

**Spremembe obnašanja:** samo pod `RwHitbox=1` in na NPC-jih z `RwHitboxMode`; privzeto nespremenjeno.

**Naslednja seja:** M3.8b (GUI), M3.9 (solid × jahanje, igralec v klientu), M5-S S1.

### 2026-10-05 (64) — integracija `m7-next`; M2.6b zaščita merilnega zagona

**Paket:** Faza 0, naloge 0.1 in 0.2 iz [`08-PREGLED-IN-PLAN.md`](08-PREGLED-IN-PLAN.md)
**Stanje:** končano (zagon baselina M2.6 ostane)

**Narejeno:**
- **0.1:** veja `origin/codex/m7-next` (28. 9.: M7.6 hitrost, M7.7 serije, M7.8 zaključek, M7.9 sled,
  M7.10c `crowdYield`, V4 sprejet) je prenesena v `codex/m7-cnpc-integration` s cherry-pickom treh
  commitov; njen prvi commit (`6ee402b`) je bil rekonstrukcija lokalnih commitov in ima isto kodo
  kot `98c3d62`. Konflikti samo v dnevniku (oba vnosa ohranjena). Pregled 3. 10. te veje ni poznal —
  naloga 0.4 (A/B in odločitev D-022) je bila že narejena. Privzeta pot knjižnice popravljena za obe
  postavitvi repozitorija (`c102c50`).
- **0.2 → M2.6b:** diagnoza iz pregleda (čiščenje celic) je bila napačna. Server se je 27. 9. sesul z
  `NoClassDefFoundError`, ker je bil runtime jar, ki ga je imel odprtega, prepisan; v isti mapi je
  tekel drug baseline zagon. **Reproducirano 5. 10.** (enak podpis, tudi `SquadManager` ob ustavitvi).
  Popravek: kopija runtime jarja na zagon (`build.gradle`), zaklepa `.scenarij.lock`/`.baseline.lock`,
  merilo **P9** (tuj gradle/Minecraft pred in med celico), takojšnja zaznava sesutja, P2 brez stare
  vrstice, `taskkill /T` ob prisilni ustavitvi. [zapis](meritve/2026-10-05-M2.6b-zascita-zagona.md)
- Najden in popravljen **P8**: manjkajoč `jvm.gc` (brez GC zbirke v kratki meritvi) je štel kot napaka
  in skril `alok.*`.

**Preverjeno v seji (prvič na uporabnikovem računalniku):** `buildPatchedMod` (test 193/193,
testOriginal 31/31), `verify-package` PASS (62 razredov), samotest 18/18, trije zagoni v svetu
(sabotaža skupnega jarja + tuj gradle; sabotaža odprte kopije = reprodukcija; čist zagon P1–P9 zelen).

**Spremembe obnašanja:** nobene v modu. Build: dev server in klient tečeta s kopijo runtime jarja.

**Naslednja seja:** zagon baselina M2.6, ponovni zagon M3.6, PR v `main`.

**Baseline M2.6 zelen (5. 10., 14:26–17:50):** prvi poskus (13:31) je v celici skripte-50 našel 4 NPC-je iz
boj-500 — `noppes slay npcs` brez števila pobije samo v radiju 120 blokov od konzole; popravek: radij
2000 in merilo P0 (svet pred spawnom prazen). Drugi zagon: 3/3 ponovitve, 27/27 celic zelenih, enak
odtis. Ključno: idle-500 MSPT p50 3,47 / p95 44,0 ms, alokacije 344 MB/s (enako skripte-500) — skoraj
ves strošek je iskanje tarče (kandidat S1); boj-500 p95 le 9,7 ms. [baseline](meritve/baseline-2026-10-05.md)

**Dopolnitev med zagonom baselina (brez builda):** naloga **0.6** (README, oznake NB.x = M7.x v
`07-BARITONE-OZADJE.md`) in **M3.7 zaključen brez spremembe obnašanja** — premisa ovržena iz kode
(`minRange` doseg kvečjemu poveča, izklop hitboxa ga ne zmanjša); nov kandidat za zgoščevanje v
radiusu enega bloka: NPC brez hitboxa ima `canBePushed() == false` in ga vanilla
`collideWithNearbyEntities` (`getTeamCollisionPredicate`) ne odriva — *nereproducirano, hipoteza*;
scenarij in morebiten popravek v M3.8. [analiza](meritve/2026-10-05-M3.7-analiza-minrange.md)

### 2026-10-03 (63) — pregled projekta; M8 izločen iz plana (D-023)

Ocena stanja in plan preostalega dela: [`08-PREGLED-IN-PLAN.md`](08-PREGLED-IN-PLAN.md). Na
zahtevo uporabnika je **M8 (migracija CustomNPC+, R3) izločen** (D-023). Ugotovljeno: baseline
M2.6 (27. 9.) je padel pri prehodu idle-500 → boj-50, ker je v svetu ostalo 500 NPC-jev
prejšnje celice — napaka čiščenja v merilni skripti, ne v modu.

**Naslednji korak:** Faza 0 iz plana — PR veje v `main`, popravek čiščenja med celicami in
zagon M2.6, ponovni zagon M3.6, serija A/B za Baritona in odločitev D-022, formacije M4.14b.

### 2026-09-28 — M7.9–M7.10: vzrok grla, čakanje v gneči, V4 sprejet

M7.9 (10×, `-Sled`): grlo B je nestabilno zaradi gneče v vratih (~800 trkov, 12–15 preklicev
`movement_timeout` v vsakem zagonu), zamik rezultata iskanja (0–1 tick) jo samo sproži. M7.10:
knjižnica `crowdYield` — NPC počaka za entiteto pred sabo, ki se premika ali ima pot in manjši ID,
čakanje ne šteje v časovno omejitev premika. Tri različice po 5×; M7.10c: 8/8 na obeh grlih in
odprtem v vseh zagonih. `nav-run.ps1 -Umik` (→ `-PnpcbCrowdYield=true`, `dev/build.gradle`).
V4 sprejet z znano omejitvijo (zadnji prihod na skupni cilj 2–6 s kasneje).
[meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md). `audit/*.csv` (sledi, ~2 MB) v `.gitignore`.

**Naslednji korak:** commit + push, nato ponovni zagon M3.6 (`.\m36-run.ps1`, `.\m36-run.ps1 -Nacin 1`).

### 2026-09-28 — M7.9: sled nedeterminizma grla B; vrata V4 popravljena

**Paket:** M7.9 (knjižnica `npcbaritone`, izven vrstnega reda faz CNPC; potrdil uporabnik).

Po M7.8 je Baritone na grlu B edini nestabilen: 8, 8, 4 od 8, vanilla vedno 6. V slabem zagonu
(`audit/m25c-nav-2026-09-28-1053`, p1) je 7/8 v zastoju in knjižnica je iskala 77-krat namesto 68.
Sled loči dve hipotezi: rezultat asinhronega iskanja se uporabi v različnem ticku, ali pa se
skupina zagozdi v vratih in izvajalec pot prekliče. Koda in zapis v knjižnici
(`docs/04-STANJE.md` 28. 9., `PathTrace` stolpci 18–24, `tools/sled_grlo.py`).

**Tukaj:** `nav-run.ps1 -Sled` (samo z `-Ozadje baritone`) vklopi `/npcb trace on` pred fazo A, po
NAV-SUM sled izpiše (`NPCB-TRACE-DUMP`) in jo kopira ob zapis zagona kot `<zapis>-sled.csv`
(merila »M7.9: …«). Vanilla zagon in zagon brez `-Sled` sta nespremenjena. PowerShell parser
0 napak (pwsh 7.4), regex in ime ciljne datoteke preverjena na sintetičnem vnosu.

**Vrata V4** (D-022, knjižnica D-043): veličina 1 »ni slabše«, 2–3 boljša mediana in najslabši
Baritonov zagon ni slabši od vanille, Baritone ≥ 5 ponovitev. Izid M7.8 ostane »ni prestan«.

**Naslednji korak:** v `..\npcbaritone-m7` `.\dev.ps1 build --offline`, nato tukaj
`.\ponovitve-run.ps1 -Ponovitev 10 -Dodatno @('-Ozadje','baritone','-Sled','-BaritoneRoot','..\npcbaritone-m7')`.

### 2026-09-28 — M7.8: izmerjene poti, R1 in odločitev V4

[Zaključna meritev](meritve/2026-09-28-M7.8-zakljucek.md): 3/3 vanilla in 3/3
Baritone N1–N15/T1–T6 zelena. Dejanske poti so pri obeh 8/8 cele; razmerje
dolžine p50 je Baritone 1,049 proti vanilla 1,131. Na grlu A prispe 6/8
proti 1/8; na grlu B ena Baritonova ponovitev zaostane (4/8 proti 6/8).
Strežniški tick brez shranjevanja je pri Baritonu nižji čez razpon, števca
iskanj pa nista primerljiva. **V4 ni prestan**, ker veličina 1 ni boljša
in B ni stabilno boljši; globalni `RwNavBackend` ostane privzeto 0.

R1 je razkril zastoj nosilca z jahačem pod Baritonom (1,37 bloka v fazi A).
`RwNavBackend` zdaj izključi tudi nosilca s potnikom. Po popravku R1 v svetu
zelen: 26/26 NPC-jev, 8/8 jahačev odklopljenih, 8 prostih nosilcev pripetih,
nosilci z jahači prevozijo 25,35 bloka. Build in `verify-package.ps1` zelena.

### 2026-09-28 — M7.6: hitrost in ponovljena kontrola

Ločeni worktree knjižnice in CNPC. `buildPatchedMod` in `verify-package` zelena;
knjižnica `build` zeleni testi. Popravljena pot do API jarja in razredov knjižnice pri
zagonu iz druge delovne kopije (`NPCB_MOD_DIR`, `-BaritoneRoot`). Vanilla in Baritone
scenarij M2.7 sta oba prestala N1–N15; Baritone tudi B1–B5, 16/16 pripetih.

Meritev [hitrosti in A/B](meritve/2026-09-28-M7.6-hitrost-in-ab.md): po popravku
`moveForward` v knjižnici je hitrost odprte proge A 0,1566 proti vanilli 0,1482
bloka/tick. Prvi posamezni zagon na grlu B je dal 4/8 proti vanilli 6/8.
Nato sta bili opravljeni [obe seriji po tri ponovitve](meritve/2026-09-28-M7.7-ab-tri-ponovitve.md):
grlo A Baritone 4–6/8 proti vanilla 1/8, grlo B 6–8/8 proti 6/8. Dolžina
Baritonove poti in primerljiv strošek iskanj ostajata odprta; V4 ni potrjen.

### 2026-09-27 (62) — prvi A/B zagon M7.6 ovrednoten

Baritone zagon zelen (N1–N15, B1–B5, 16/16 pripetih), vanilla prva ponovitev zelena.
Baritone 8/8 pri 40 tickih na obeh progah, vanilla G 1/8 (A) in 6/8 (B). Večji del razlike v
času je **hitrost**: knjižnica je v `own` ignorirala `speed × 0,7` iz `navigateTo` — popravljeno
v knjižnici (D-042). [meritev](meritve/2026-09-27-M7.6-ab-prvi-zagon.md). Serija vanilla je
padla v ponovitvi 2 na gradle `:extractNatives` (zaklenjeni natives); `nav-run.ps1` zdaj enkrat
poskusi znova po 20 s.

**Naslednji korak:** knjižnica `.\dev.ps1 build --offline`, nato `.\testworld.ps1` +
`.\nav-run.ps1 -Ozadje baritone`; ko je `gib(max)` na progi O blizu vanilli, obe seriji po 3.

### 2026-09-27 (61) — M7.2–M7.4 commitani; M7.6 A/B scenarij (`nav-run -Ozadje baritone`)

**Paket:** M7 knjižnice `npcbaritone` (izven vrstnega reda faz CNPC, D-022).

**Commitano delo seje (4) knjižnice** (`3e458aa`, prej necommitano na tej veji): most
`NpcBaritoneBridge` (samo API 2, `compileOnly`), stikalo `RwNavBackend` (config 0 + NBT ključ,
zapisan samo pri 1), izbira samo za kopenskega NPC-ja brez jahanja in formacije (U7), ponovna
namestitev po `updateTasks()` (U2), `/rwnav`. Tam preverjeno: `buildPatchedMod` (192/192),
`verify-package`, dedicated smoke. Prevod v oblaku na tem drevesu: 102 + 25 datotek, 192/192.

**M7.6 (v tej seji, prevedeno, čaka na zagon):** `nav-run.ps1 -Ozadje baritone` —
[scenarij](scenariji/M7.6-ab-baritone.md). Server z `-PnpcBaritoneDev -PrwNavBackend=1`,
`/rwnav on NAV_Walk`, `/npcb perf` okoli faz A/B; merila B1–B5; odtis `ozadje=baritone`, da se
serija ne pomeša z vanilla. `/rwnav status` zdaj izpiše tudi `pripetih=` (dejansko pripeti).
Vanilla zagon je nespremenjen (brez `-P`, isti odtis). Preverjeno: PowerShell parser 0 napak,
bralnika `RWNAV`/`NPCB-PERF` in `New-NavZapis` na sintetičnem logu (pwsh 7.4), JUnit 192/192.

**Znano:** sonda (veličine 1, 2, 5) je vanilla `PathFinder` tudi pri Baritonu — za A/B štejejo
`cele=` iz igre, `NAV-CAS` ter `bar.*` iz knjižnice; veličina 2 za Baritona še ni merjena.

**Naslednji korak:** knjižnica `.\dev.ps1 build --offline`, nato `.\testworld.ps1` in
`.\nav-run.ps1 -Ozadje baritone` (en zagon, B1–B5 + N1–N15); ko je zelen, obe seriji po tri
ponovitve (`.\ponovitve-run.ps1 -Scenarij nav` in `... -Dodatno @('-Ozadje','baritone')`).

### 2026-09-27 (60) — integracija lokalne veje (M4.14a); D-022 Baritone kot izbirno ozadje; revizija navigatorja

**Paket:** vzdrževanje repozitorija + M7.1 knjižnice `npcbaritone` (izven vrstnega reda faz CNPC —
izrecna potrditev uporabnika 2026-09-27)
**Stanje:** koda CNPC ni funkcionalno spremenjena; formacije integrirane, dokumentacija D-022

**Integracija:** mapa `Desktop\customNPC_rework` je bila na stanju 18. 9. z enim nepushanim
commitom (M4.14a formacije, lokalni D-017). `git pull --rebase` je 27. 9. ustavil konflikt
(D-017–D-020 so na `origin` že zasedeni) in je bil prekinjen brez izgube. Lokalna veja je
shranjena v `backup/lokalni-main-20260927`; `main` je postavljen na `origin/main` (M3.6), M4.14a
prenesen s cherry-pickom (`CustomNpcs`: obe registraciji ukazov; dokumenti: vse vrstice origin +
formacije) in njegova odločitev preštevilčena **D-017 → D-021**. Preverjeno v seji: 99 virov
`src/patch` prevedenih z `javac --release 8`, **189/189 testov**.

**D-022** (`01-ARHITEKTURA.md` §9): stopnja D iz D-012 se odpre samo kot izbirno ozadje
(`RwNavBackend`) prek zunanje knjižnice `npcbaritone`; vanilla privzeta; samo `movementType == 0`;
sprejem po M2.7 s tremi ponovitvami. Knjižnica: 200 NPC-jev, glavna nit p95 1,64 ms/tick.

**Revizija** [`07-BARITONE-OZADJE.md`](07-BARITONE-OZADJE.md): klici navigatorja v `ai/**` in
`EntityNPCInterface` so po M3.1–M3.6 enaki izvirniku; ukrepi U1–U7. Najpomembnejši:
**U2** — `updateTasks()` ob vsaki posodobitvi AI ustvari nov vanilla navigator; **U7** —
formacije (`Squad`, `STEER`) vsak tick nastavijo pot z eno točko, zato člani enote ostanejo na
vanilla navigatorju; **U1** — `EntityAIAttackTarget.shouldExecute` preverja pot z vanilla A*.
V knjižnici popravljena napaka, ki bi jo sprožil CNPC skriptni `getNavigationPath` (NPE med
iskanjem; knjižnica `2ee0df2`).

**Naslednji korak:** knjižnica API 2 (U2 ponovna namestitev, U6 hitrost, U3/U4 vrata na
instanco), nato M7.2 (`NpcBaritoneBridge`), M7.3 (stikalo), M7.4 (`updateTasks`, U2, U7).
Formacije: dimni test F1–F12 v svetu še čaka (kot 18. 9.).

---

### 2026-09-24 (59) — M3.6 prvi zagon ovrednoten; scenarij dopolnjen z dosegom napada

**Paket:** M3.6 · **Stanje:** delno — popravek deluje, A5 strogo pade na enem vzorcu; **ponovni zagon čaka na uporabnika**.

**Izid prvega zagona** (`audit/m36-napad-n0.log`, `-n1.log`, [zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)):
A1, A2, A3, A6 zelena v obeh načinih. **A4: napaka PONOVLJENA** — tavajoči mediana 30 / max 120 tickov,
2 brez napada; kontrola 10 / 10. **Način 1:** mediana 10, 17 od 20 tavajočih 10 tickov, nihče brez
napada, a max 50 > kontrola + 10 → **A5 PADLO** (en vzorec 50, en 30).

**Hipoteza za ostanek (nereproducirana):** `EntityAIAttackTarget.shouldContinueExecuting` ustavi napad
izven `aggroRange` (16, Čebiševa razdalja), `shouldExecute` ga brez te preverbe spet začne → NPC, ki je
ob ukazu odtaval predaleč, ima utripajoč napad. Obnašanje originala, neodvisno od prioritete.

**Narejeno:**

- `m36-control.js`: ob ukazu Čebiševa razdalja do tarče (`d=` v `M36-R`), oznaka `o` za vzorce nad
  `aggroRange`, ki ne štejejo v A2–A5; `M36-LAT` dobi polja `izven`, `izvenNikoli`, `latIzvenMed`,
  `latIzvenMax` (na koncu vrstice, stari regex ostane veljaven). Vstavljeno v `M36_Control.json` z
  `vstavi-skripto.py` (ujemanje preverjeno).
- `m36-run.ps1`: bere nova polja; če jih ni (stara skripta v JSON), preverba pade. **Popravljena
  napaka scenarija:** krmilnik iz prejšnjega zagona je shranjen v svetu in se ob nalaganju oglasi
  (`M36-INIT`/`M36-FIND`, vidno v n1 ob 11:07:57), A1 pa je bral prvi `M36-FIND`. Zdaj se štejejo samo
  markerji po spawnu, izid se bere od zadnjega `M36-INIT`.

**Preverjeno v seji:** skripta pognana v Node na mock svetu (NPC izven dosega → `ot50`, izločen iz
statistike); `m36-run.ps1` razčlenjen s PowerShell 7.4.6 (0 napak); korak 8 pognan na sintetičnem logu
(stari + novi krmilnik → vse zeleno, vzorec `o` izločen) in na **pravem** `m36-napad-n1.log` (pravilno
pade: A5 50 proti 10 in manjkajoča polja `izven`).

**Ni preverjeno:** ponovni zagon v svetu. Hipoteza je potrjena, če so v načinu 1 vse zakasnitve nad 20
označene z `o`.

**Ni del tega commita:** `dev/build.gradle` (izključitev testov `rework/**` iz `testOriginal`),
`vse-testi.ps1` in `ZAZENI-VSE-TESTE.bat` sta necommitana sprememba izven te seje in ostajata nedotaknjena.

**Opomba za seje:** `git status` v lupini na uporabnikovem računalniku pusti `.git/index.lock` (brisanje v
mapi ni dovoljeno); premaknjen je v `.git/_to_delete/`, ki ga uporabnik lahko pobriše. Za git v tej lupini
uporabljaj `git --no-optional-locks`.

**Spremembe obnašanja:** nobene (samo scenarij).

**Naslednja seja:** ovrednotiti ponovni zagon M3.6; če A5 zeleno → M3.6 zaključen, M3.7.

### 2026-09-24 (58) — M3.6 preusmerjen: mutex biti so pravilni, napaka je v prioriteti napada

**Paket:** M3.6 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Premisa ovržena (pred kodiranjem, z uporabnikom dogovorjena preusmeritev, D-020).** `AiMutex`
`PASSIVE/LOOK/PATHING` = 1/2/4 so po vrednosti vanilla MOVE/LOOK/JUMP. `EntityAIAttackTarget` ima 3,
enako kot vanilla `EntityAIAttackMelee`; vsi gibalni taski imajo bit 1, zato z napadom po
`EntityAITasks.canUse` hkrati ne teče noben. „Dodaj `PATHING`“ bi spremenil samo razmerje do skoka.
Popravljeno v `02-ZAHTEVE.md` (dve mesti), D-012, D-019, `03-FAZE.md`.

**Resnična napaka (javap na originalu):** `setResponse` doda napad s `taskCount` brez `++`, zato
`EntityAIWander`/`EntityAIMovingPath` dobi isto prioriteto; enake prioritete vanilla ne prekine.
`Wander` se ob napadu ne umakne sam → NPC, ki tava, tarčo napade šele na koncu poti tavanja.
`MovingPath` se umakne sam (`isAttacking()`), NPC s projektilom napake nima.

**Narejeno:**

- `rework/ai/AttackPriority` (čista funkcija `nextPriority`) in `CommandRwAttack` (`/rwattack 0|1`,
  ob spremembi vsem naloženim NPC-jem `updateAI = true`, marker `RWATTACK`); config
  `RwAttackPriority` (privzeto 0); klic v `EntityNPCInterface.setResponse` za bojnimi taski.
- `AttackPriorityTest` (8) na **pravem** vanilla `EntityAITasks`: `originalWanderingBlocksAttack`
  pokaže napako, `fixAttackPreemptsWandering` popravek; dva testa ovržeta staro premiso.
- Scenarij `m36-run.ps1` + `m36-setup-commands.txt` + `m36-control.js` + štiri fixture `M36_*`
  ([opis](scenariji/M3.6-napad-med-tavanjem.md)): proga T (tavajo) proti kontroli S (stojijo), trije
  krogi, zakasnitev od `setAttackTarget` do poti k tarči; A4 diagnoza v načinu 0, A5 preverba v načinu 1.

**Preverjeno v seji:** vseh 84 izvornih datotek `src/patch` + testi prevedeni z `javac --release 8` (JDK 11 na
uporabnikovem računalniku, knjižnice iz gradle predpomnilnika, samo branje); **35/35 testov
`rework/**` zelenih**; `m36-run.ps1` razčlenjen s PowerShell 7.4.6 (0 napak), regexi preizkušeni na
sintetičnem logu; `m36-control.js` preverjen z Node in pognan na mock svetu; vstavitev v JSON preverjena.

**Ni preverjeno:** gradle build, zagon v svetu (A1–A6). `verify-package.ps1` bo pokazal nova razreda `rework/ai/AttackPriority` in
`CommandRwAttack` ter spremenjena `EntityNPCInterface` in `CustomNpcs`.

**Stranske ugotovitve (hipoteze, niso popravljene):** `EntityAIMoveIndoors`/`EntityAIFindShade` imata
prioriteto pred napadom in se ob napadu ne umakneta; napad in strelski napad imata isto prioriteto.
[zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)

**Spremembe obnašanja:** nobene privzeto; z `RwAttackPriority=1` (tabela).

**Naslednja seja:** ovrednotiti `audit/m36-napad-n0.log` in `-n1.log`, nato M3.7.

### 2026-09-24 (57) — M3.5 zaključen: E9 zeleno

**Paket:** M3.5 · **Stanje:** zaključeno.

`r1-run.ps1` v načinu 0 in 1 (`audit/m22-r1.log`, `m22-r1-k1.log`, oba `R1-SUM mount=8`): višina
jahačev pred mountom 1,900, po mountu 1,463 v obeh; **po sestopu 1,463 v načinu 0 (napaka ponovljena)
in 1,900 v načinu 1 (popravek)**. Faza A enaka kot v M3.4 (M 1,19 / 22,57). Odjemalec ni preverjen.
[zapis](meritve/2026-09-24-M3.5-hitbox.md)

**Spremembe obnašanja:** nobene privzeto. **Naslednja seja:** M3.6.

### 2026-09-24 (56) — M3.5: hitbox jahača po sestopu

**Paket:** M3.5 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Popravek predpostavke iz `02-ZAHTEVE.md` §R1.** Trditev „`updateHitbox()` se ob `startRiding` ne
pokliče“ velja za `EntityNPCInterface`, **ne pa za `EntityCustomNpc`** (običajni NPC): ta v originalu
override-a `startRiding` in po `super` brezpogojno pokliče `updateHitbox()` (`javap -c` na
`customnpcs-dev-runtime.jar`). Mount je torej že pravilen (višina × 0,77). Manjka samo **sestop**:
`dismountRidingEntity` ne override-a nihče, zato jahač po sestopu ostane skrčen, dokler ga ne osveži
animacija, smrt ali reset.

**Narejeno:**

- `EntityNPCInterface.dismountRidingEntity()`: po `super` pokliče `updateHitbox()` (virtualno, torej
  `EntityCustomNpc.updateHitbox` z modelom), če je entiteta prej jahala in `RiderState.hitboxFollowsRiding`
  (način ≠ 0, D-019). Pokriva vse poti sestopa: ukaz/skripta, `removePassengers` ob smrti nosilca,
  odjemalca prek `SPacketSetPassengers`. Forge `EntityMountEvent` ni uporabljen, ker se sproži **pred**
  spremembo stanja (v njem je `isRiding()` še star) — odstop od opisa paketa v `03-FAZE.md`.
- Scenarij: `r1-control.js` izpiše `R1-HB faza=mount` (višina jahačev pred in po mountu) in na koncu po
  sestopu vseh jahačev `R1-HB faza=sestop`; vstavljeno v `R1_Control.json` z `vstavi-skripto.py`
  (ujemanje preverjeno), sintaksa preverjena z Node. `r1-run.ps1`: **E9** — v načinu 0 diagnoza
  (pričakovano: napaka ponovljena), z `-Krmiljenje 1|2` preverba, da se višina vrne (±0,01).
  Sestop je po koncu faze C, zato meritev faz A–C ni spremenjena.
- Test `hitboxFollowsRidingOnlyInFixModes` (skupaj 18/18 v seji).

**Ni preverjeno:** v svetu; odjemalčeva stran (dedicated server nima odjemalca).

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` (tabela).

**Naslednja seja:** ovrednotiti E9 v obeh zagonih, nato M3.6.

### 2026-09-24 (55) — M3.4 zaključen: regresija zelena v vseh treh načinih

**Paket:** M3.4 · **Stanje:** zaključeno.

159 testov zelenih; `r1-run` v načinih 0, 1 in 2 brez napak. Način 0 = original (A M 1,19). Način 1
in 2: E8 zeleno (A M 22,8 bloka proti kontroli 19–20). **Način 2 v fazi C nosilcev ne premakne** —
pričakovano, ker v načinu 2 jahačeva pot nikoli ne krmili; konjenica s poveljnikom ostaja v načinu 1.
Faze B/C niso deterministične (tudi kontrola se premika med zagoni) — za A/B primerjati fazo A.
`tpTo` in gating sledenja scenarij ne izvede — samo testi. [zapis](meritve/2026-09-24-M3.4-regresija.md)

**Spremembe obnašanja:** nobene privzeto. **Naslednja seja:** M3.5.

### 2026-09-24 (54) — M3.4: gibalni AI jahača in `tpTo` pod istim stikalom

**Paket:** M3.4 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Odločitev o obsegu.** Blokiranje prek mutex bitov ni izvedljivo: vsi gibalni in tudi bojni taski
CustomNPCs uporabljajo `PASSIVE` (`EntityAIAttackTarget` `LOOK+PASSIVE`, M3.6), zato bi task z
`PASSIVE` jahaču ugasnil tudi napad in `EntityAIWorldLines`. Namesto tega eksplicitna preverba v
dveh gibalnih taskih, ki je še nista imela (`EntityAIWander` in `EntityAIReturn` že preverjata
`isRiding()`). Bojni taski ostanejo: pot jahača v boju je v načinu 1 ukaz nosilcu (osnova M3.10).

**Narejeno:**

- `RiderState.riderMovementBlocked(način, jahaNpcNosilca, nosilecImaPot)`: 0 → nikoli; 1 → ko ima
  nosilec-NPC svojo pot (takrat bi pot jahača itak zavrgli, `decide` = MOUNT — test to preveri za vse
  kombinacije); 2 → vedno na nosilcu-NPC. Uporabljeno v `EntityAIFollow.canExcute` in
  `EntityAIMovingPath.shouldExecute/shouldContinueExecuting` (ob prekinitvi se indeks poti vrne, kot
  pri napadu).
- `RiderState.teleport` + `EntityNPCInterface.tpTo`: v načinih 1/2 se jahač na nosilcu-NPC ne
  teleportira sam (vanilla `updatePassenger` bi ga vsak tick vrnil), ampak se teleportira **nosilec**
  in jahač gre z njim; na nosilcu, ki ni NPC, `tpTo` ne naredi nič. Način 0: original.
- Brez novega stikala: vse je vezano na `RwMountSteering` (D-019).
- Testi: `RiderStateTest` +4 (skupaj **17/17** z `MountGuardTest`).

**Preverjeno v seji:** prevedeno z `javac --release 8` (spremenjeni `EntityAIFollow`,
`EntityAIMovingPath`, `EntityNPCInterface`, `RiderState`, `MountGuard`); 17/17 testov zelenih.

**Ni preverjeno:** v svetu. Scenarij R1 nima sledilcev ne poti premikanja, zato lahko v svetu
preveri samo, da se nič ne podre (regresija). Neposreden scenarij za `tpTo` jahača (sledilec na
nosilcu, lastnik daleč) ne obstaja — predlog za M3.9 ali ob M3.10.

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` (tabela).

**Naslednja seja:** ovrednotiti zagone, nato M3.5.

### 2026-09-24 (53) — M3.3 zaključen: R1 popravljen v svetu

**Paket:** M3.3 · **Stanje:** zaključeno.

Uporabnik je pognal `.\dev.ps1 test --offline` (**155 testov, 0 padlih**; `RiderStateTest` 9/9,
`MountGuardTest` 4/4), build, `.\r1-run.ps1` in `.\r1-run.ps1 -Krmiljenje 1`.

- **Način 0 = original:** faza A proga M 1,19 bloka, `navig=0/8` — enako kot 24. 9.
- **Način 1:** faza A proga M `navig>0` v 20/20 vzorcih, prevoženo 22,70 (kontrola 20,43), vseh
  8 jahačev ostane na nosilcih; **E8 zeleno**. Faza B: proga M do cilja 0,60 (prej 24,31),
  stopnica in vrata prečkana. Faza C nespremenjena (namerno vanilla, ko ima pot samo jahač).
- Brez napak v obeh logih.

**Ugotovitve:** jahač brez poti v fazi C je **ponovljiv** (razpon 32,96, kot 24. 9.) — ostaja odprto.
`audit/package-verification.txt` (07:46) je iz starega `…-workspace.jar`, ne iz tega builda.

**Spremembe obnašanja:** nobene privzeto (glej tabelo; `RwMountSteering=1` je preverjen popravek R1).

**Meritve:** [zapis](meritve/2026-09-24-M3.3-R1-popravek.md)

**Naslednja seja:** M3.4.

### 2026-09-24 (52) — M3.3: `RiderState` in popravek R1 pod stikalom

**Paket:** M3.3 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Narejeno:**

- `rework/entity/RiderState` — enoten vir resnice o jahanju: vloga (`NONE`/`RIDER`/`MOUNT`/`RIDER_AND_MOUNT`)
  in odločitev **kdo krmili** nosilca (`decide(način, nosilecJeNpc, nosilecImaPot, jahačImaPot)`), brez
  Minecraft tipov. Načini: **0 original** (privzeto, D-007), **1** nosilec-NPC krmili sam, jahač prevzame
  samo, ko ima pot on in nosilec ne (vanilla spider jockey ostane osnova za M3.10), **2** nosilec-NPC krmili
  vedno. Nosilec, ki ni NPC, je v vseh načinih vanilla.
- `rework/entity/MountGuard` — v `EntityNPCInterface.onLivingUpdate` jahača pred `super.onLivingUpdate()`
  zajame pot, hitrost poti in move helper nosilca (natanko polja, ki jih prepiše `EntityMoveHelper.read`,
  preverjeno z `javap -c`), po njem jih vrne. Pot se vrne z zapisom polja, ne s `setPath`, ker `setPath`
  ponastavi zaznavo zataknitve. Dostop do zaščitenih polj: `net.minecraft.pathfinding.RwNavigatorAccess`
  in `net.minecraft.entity.ai.RwMoveHelperAccess` (vzorec `RwWorldAccess`).
- Stikalo: config `RwMountSteering` (0/1/2, privzeto 0) in ukaz `/rwmount [0|1|2]` (med tekom, ne zapiše
  v config; odgovor v log z markerjem `RWMOUNT`).
- `r1-run.ps1 -Krmiljenje 1|2`: pred scenarijem pošlje `rwmount`, izpis gre v `audit/m22-r1-k<N>.log`,
  doda merilo **E8** (faza A: nosilci M imajo pot in proga M prevozi ≥ 20 blokov). Brez parametra je zagon
  enak kot pred M3.3.
- Testi: `RiderStateTest` (9, izčrpna tabela odločitev) in `MountGuardTest` (4): test
  `withoutGuardRiderErasesMountPath` ponovi vanilla prepis in pokaže R1 (pot `null`, `WAIT`), test
  `guardRestoresPathAndMoveHelper` pokaže, da ga varovalo razveljavi.

**Preverjeno v seji:** nova razreda + spremenjena `EntityNPCInterface` in `CustomNpcs` prevedeni z
`javac --release 8` proti mapiranim MC razredom in `customnpcs-dev-runtime.jar` (guava/netty/authlib
nadomeščeni z minimalnimi nadomestki izven repozitorija, D-014); **13/13 testov zelenih**. `r1-run.ps1`
razčlenjen s PowerShell 7.4 (0 napak).

**Ni preverjeno:** build z gradlom, nalaganje v igri, E8 v svetu. `verify-package.ps1` bo pokazal nove
razrede: `RiderState` (+ `$Role`, `$Steering`), `MountGuard`, `CommandRwMount`, `RwNavigatorAccess`,
`RwMoveHelperAccess`, poleg spremenjenih `EntityNPCInterface` in `CustomNpcs`.

**Znana meja (zapisana v kodi):** ko imata pot oba (nosilec in jahač), vanilla `setPath` nosilcu ponastavi
zasebno stanje zaznave zataknitve; varovalo vrne pot, tega stanja ne. V fazah A/B se to ne zgodi.

**Ni v tem paketu:** jahač od osmih brez poti v fazi C (odprto vprašanje, ni R1); gating AI taskov in
`tpTo` (M3.4); `updateHitbox` ob mountu (M3.5).

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` nosilec-NPC obdrži svojo pot (tabela
"Sprejete spremembe obnašanja").

**Za uporabnika:**
```
.\dev.ps1 test --offline
.\dev.ps1 build --offline
.\r1-run.ps1                  # mora ponoviti izid 24. 9. (E7 POTRJENO, proga M v fazi A obtiči)
.\r1-run.ps1 -Krmiljenje 1    # E8: proga M v fazi A pride do cilja
```

**Naslednja seja:** ovrednotiti oba zagona (`audit/m22-r1.log`, `audit/m22-r1-k1.log`), zapis v
`docs/meritve/`, nato M3.4.

### 2026-09-24 (51) — M3.2 zaključen: faza C potrdi mehanizem

**Paket:** M3.2 · **Stanje:** zaključeno.

Uporabnik je pognal `.\r1-run.ps1` (09:07–09:10). Faze A in B ponovijo izid; v fazi C
(`navigateTo` jahačem) imajo nosilci proge M pot **natanko takrat kot njihovi jahači**
(`navig == navigJ` v 20/20 vzorcih: 7/7, 4/4, 0/0), prečkajo stopnico z = 20 in vrata
z = 28 in pridejo na cilj (0,64) v ~180 tickih — 2,8× hitreje od proge S (napoved ≥ 2,1×
iz hitrosti 1,5 proti 0,7). S tem je potrjen tudi del „ne znajo čez bloke“.
[izid](meritve/2026-09-24-M3.2-R1-diagnoza.md#izid-faze-c-24-9)

**Napaka v mojem merilu E7:** prva različica je gledala povprečni `navig` faze C ≥ 4/8; ker
nosilci pridejo na cilj v tretjini faze in pot tam legitimno konča, je bilo povprečje 3,0 in
skripta je izpisala `OVRZENO` na potrjenem izidu. Popravljeno na ujemanje `navig == navigJ`
+ premik ≥ 10; na tem izpisu preizkušeno s PowerShell 7.4 → `POTRJENO`.

**Odprto (ne R1):** en jahač od osmih v fazi C ni dobil poti že od prvega vzorca; njegov
nosilec je ostal na startu. Vzorec nima po-NPC izpisa. Pogledati v M3.3.

**Spremembe obnašanja:** nobene.

**Naslednja seja:** M3.3.

### 2026-09-24 (50) — M3.2: nosilcu pot izbriše jahač, v vanilla kodi

**Paket:** M3.2 · **Stanje:** mehanizem najden in dokazan na bytecode; potrditev v svetu
(faza C, E7) čaka na `.\r1-run.ps1`.

**Najprej zaključek M3.1.** `verify-package.ps1` 23. 9. ni zapisal novega poročila, ker
primerja `CustomNPCs_1.12.2-01Oct19-workspace.jar`, ki ga naredi samo `buildPatchedMod`
(ta je od 15. 9.), ne `build`. Namesto tega je seja preverila tisto, kar je v zagonu
dejansko teklo: `customnpcs-dev-runtime.jar` (23. 9. 14:51) proti
`customnpcs-mapped-01Oct19.jar` — 1716 vnosov originala, **nobeden ne manjka**, spremenjenih
62 in dodanih 37, **vsi v naboru 99 razredov** `customnpcs-patch-classes`; nič drugega ni
spremenjeno. M3.1 je s tem zaključen. `verify-package.ps1` ostane za predajo (M10) in
zahteva `.\dev.ps1 buildPatchedMod --offline`.

**Mehanizem.** `EntityLiving.updateEntityActionState()` (vanilla, `final`) na koncu AI ticka
vsake entitete, ki jaha živo entiteto, naredi `nosilec.getNavigator().setPath(jahač.getPath(), 1.5)`
in `nosilec.getMoveHelper().read(jahač.getMoveHelper())` — vanilla spider jockey. Jahač se
posodobi takoj za nosilcem (`World.updateEntityWithOptionalForce`, potniki po nosilcu). Jahač
brez cilja nosilcu torej vsak tick izbriše pot (`setPath(null)` → `currentPath = null`) in
ga postavi v `WAIT`. Preverjeno s CFR na mapiranih razredih iz
`dev/build/tmp/recompileMc/compiled` (`EntityLiving`, `PathNavigate`, `PathNavigateGround`,
`EntityMoveHelper`, `World`), ne po spominu.

**Razloži vse izmerjeno:** `navig=0/8` v vseh vzorcih; faza A 23. 9. raste **natanko za
0,06 bloka na osvežitev `navigateTo`** (en tick gibanja na klic — podpis mehanizma); faza B
8,9 bloka, ker `EntityAIAttackTarget` prepotuje vsakih 4–10 tickov; zastoj pred stopnico
(skok se ne dokonča, ker `read()` stanje `JUMPING` prepiše v `WAIT` — ta del je hipoteza).
**`canNavigate()` ni vzrok** (nosilec je na tleh tako kot kontrola). Ostali kandidati §R1
(mutex, `minRange`, `updateHitbox`, `tpTo`) R1 ne pojasnijo in ostanejo v svojih paketih.

**Faza C** v `r1-control.js` (ticki 940–1340): `navigateTo` dobijo **jahači** proge M.
Napoved: nosilci M dobijo pot in vozijo s hitrostjo 1,5 (proti 0,7 proge S). `r1-run.ps1`
čaka še `R1-B-END` in `R1-C-START`, zahteva ≥ 20 vzorcev tudi za CM/CS (E5) in izpiše
diagnozo **E7** (POTRJENO / OVRZENO / NEODLOCENO; ne podre zagona). Vzorec ima novo polje
`navigJ=` na koncu, regex ga sprejme kot neobvezno. Skripta vstavljena v `R1_Control.json`
z `vstavi-skripto.py` (ujemanje preverjeno); JS sintaksa preverjena z Node, `r1-run.ps1`
razčlenjen s PowerShell 7.4 (0 napak) in `Read-Samples` preizkušen na sintetičnem izpisu.

**Ni preverjeno:** zagon faze C v svetu.

**Spremembe obnašanja:** nobene (samo scenarij).

**Posledica za M3.3:** popravek ni v AI nosilca, ampak v odločitvi, kdo krmili. Predlog v
[diagnozi](meritve/2026-09-24-M3.2-R1-diagnoza.md): jahač-NPC v `onLivingUpdate` pred
`super` zajame pot in move helper nosilca in ju po njem vrne (pod stikalom); vanilla
krmiljenje z jahačem ostane osnova za M3.10.

**Naslednja seja:** ovrednotiti E7 iz `audit/m22-r1.log`, nato M3.3.

### 2026-09-23 (49) — M3.1: prenos `EntityNPCInterface` in `noppes/npcs/ai/**` brez funkcionalnih sprememb

**Paket:** M3.1 · **Stanje:** preneseno in preverjeno na ravni bytecode; zagon v svetu čaka na uporabnika.
M2.6 baseline po navodilu uporabnika odložen (koda je pripravljena).

**Narejeno:** 37 razredov iz `reference-src` v `src/patch/java` (1 + 36 v `ai/`), plus
`net/minecraft/world/RwWorldAccess.java`. Rekonstrukcijski popravki (samo ti):

- `dataManager.register/set(X, (Object)v)` → brez `(Object)` (19× v `EntityNPCInterface`, 2× `CombatHandler`); `set(Walking, (!noPath() ? 1 : 0))` → `!noPath()` (CFR je boolean izpisal kot int)
- surovi `List`/`ArrayList` pri for-each → tipizirani (4×); `Object event` → `NpcEvent.TargetEvent`
- `EntityAIAvoidTarget`, `EntityAIStalkTarget`: CFR je spremenljivko dodelil samo v eni veji ternarnega izraza → rekonstruirano po `javap` (komentar v kodi)
- `EntityAIOpenAnyDoor`: `ImmutableSet` → `ImmutableSet<IProperty<?>>`
- `world.pathListener` je v dev `protected` (v igri ga naredi javnega `cnpcs_at.cfg`) → `RwWorldAccess.pathListener(world)` v istem paketu, brez refleksije

**Preverjeno v seji:** vseh 74 razredov `src/patch` prevedenih z `javac --release 8` proti
mapiranim MC razredom in pravim knjižnicam iz uporabnikovega gradle predpomnilnika (guava 21,
netty 4.1.9, authlib, log4j; dostop odobren 23. 9., samo branje). **Bytecode primerjava z
originalom** (`dev/tools/primerjaj-bytecode.py`, izid `audit/m31-bytecode-primerjava.txt`):
37/37 razredov, 558 metod — 508 enakih, 38 razlik samo v obliki prevajalnika, 12 ročno
pregledanih in enakovrednih (verizne dodelitve, združeni returni, tip iteratorja, namerni
`RwWorldAccess`).

**Ni preverjeno:** nalaganje v igri. Build, `verify-package.ps1` (pričakovan nov seznam
razredov: 37 prenesenih + `RwWorldAccess`), `testworld-run`, `r1-run` in `nav-run` morajo ostati zeleni.

**Spremembe obnašanja:** nobene.

**Zagon v svetu (23. 9., 14:51–14:55):** build z novimi razredi (`customnpcs-patch-classes`: 99 razredov, med njimi 36 `ai/`, `EntityNPCInterface`, `RwWorldAccess`); `testworld-run` zelen (8× `TW-OK`, `TW-SCRIPT-OK`); `r1-run` prišel do `R1-SUM mount=8`, brez napak, in **ponovi izid 17. 9.**: proga M obtiči (razpon 7,00, do cilja 24,3), proga S se razleti (razpon 33). `verify-package.ps1` ni zapisal novega poročila (`audit/package-verification.txt` je od 11. 9.) — še ni preverjeno.

**Naslednja seja:** `verify-package.ps1`, nato M3.2 (diagnoza R1).

### 2026-09-23 (48) — preverba spawna z `nogui`: hipoteza ovržena, D-017

Idle 500, 20 + 60 s, `nogui` deluje: GC 2,2 / 2,0 ms na sekundo, brez polnih zbirk.
Naenkrat: TPS 14,5, p50 14,2, p95 206 ms, 291 tickov > 50 ms. Razpršeno: TPS 13,7, p50 16,5,
p95 218 ms, 275 tickov > 50 ms. **Razlike ni** in počasni ticki so enakomerno po fazah
`tick % 10` → sinhroni `ticksExisted % 10` ni vzrok. **D-017: baseline samo s spawnom naenkrat.**
Odprto: ~23 % tickov nad 100 ms brez GC, poti in autosava (µs/NPC 136–144). Pri prvem zagonu
M2.4 (11:51) je bila ista celica µs/NPC 58 in TPS 20 — **razlika med zagoni je večja od
razlike med načini**, zato baseline zahteva miren računalnik (brez IDE, brskalnika).

### 2026-09-23 (47) — preverba spawna: meritev je merila okno serverja, ne moda

Idle 500, 20 + 60 s. Naenkrat: TPS 9,4, p50 14,7 ms, p95 377 ms; razpršeno: TPS 8,5, p50 17,3 ms,
p95 394 ms. **Obe neveljavni:** 118–121 polnih GC v 60 s (2/s, vsak s parom mlade zbirke),
354 ms GC na sekundo, stara generacija po GC pa samo 119 MB od 1820. To je `System.gc()`:
okno dedicated serverja (`StatsComponent`) ga kliče vsakih 500 ms (`StatsComponent.java:30,43`).
Popravek: `runServer` dobi `nogui` (`dev/build.gradle`). JvmProbe je to ujel v prvem zagonu.
**Posledica:** vse dosedanje meritve MSPT (M2.1, M2.7, M2.4) so tekle z oknom in vsebujejo
dve polni zbirki na sekundo; pred baselinom jih ne primerjati. Odločitev o spawnu (D-017) ni
mogoča iz teh dveh zagonov — preverba se ponovi z `nogui`.

### 2026-09-23 (46) — M2.6: GC/alokacije, baseline skripta, preverba sinhronega spawna

**Paket:** M2.6 · **Stanje:** koda narejena in preverjena v seji; zagon čaka na uporabnika

**Narejeno:** `rework/diag/JvmProbe.java` (+6 ključev v `DiagKeys`, klic v `Diag.reset`/
`snapshot`/`setEnabled(false)`), `JvmProbeTest` (5 testov); `perf-run.ps1`: P8, veličine GC
in alokacij, `-Razprseno`, `-SerijaDir`, `-Counts 50,200` iz ukazne vrstice; `baseline-run.ps1`
(B1–B3, mediana/razpon po celicah, `audit/m26-baseline-*.json`, `docs/meritve/baseline-*.md`);
scenarij `docs/scenariji/M2.6-baseline.md`.

**Preverjeno v seji:** 20 razredov `rework/**` prevedenih z `javac --release 8` proti
mapiranim razredom (D-014); **95 testov instrumentacije zelenih** (`Diag`, `DiagSnapshot`,
`DiagChunkPlan`, `Distribution`, `SlowTicks`, `NavProbe`, `JvmProbe`). `JvmProbe` na pravem
JVM-u: 64 MB alokacij izmerjenih kot 67,3 MB, `System.gc()` viden v `jvm.gc` in `jvm.gc.old`.
`perf-run.ps1` in `baseline-run.ps1` sintaktično čista (PowerShell 7.4.6); `baseline-run.ps1`
pognan nad ponarejenim `perf-run.ps1` (3 ponovitve × 4 celice): B1–B3 zelena, mediana in
razpon pravilna. Razpršen spawn da iste položaje kot spawn naenkrat (boj 500: 100 ukazov).

**Ugotovitve:**

- **`-Counts 50,200` prek `-File` pride kot en niz** in se pretvori v `50200`. Obe skripti
  zdaj vhod razbijeta sami. Ujel ga je šele test `baseline-run.ps1` nad ponarejeno skripto.
- **Popravek istega dne:** `[string[]]$Counts` ostane tipiziran tudi po prireditvi, zato je
  bil `500` niz in `'500' -gt 1000` je v primerjavi nizov resničen: `perf-run.ps1 -Counts 500`
  je padel z "Več kot 1000 NPC-jev". Parameter je zdaj `$CountsIn` (alias `-Counts`),
  `$Counts` je `[int[]]` (obe skripti). Test nad ponarejenim `perf-run` tega ni ujel, ker je
  ponaredek imel svoj param blok.
- **`ticksExisted` se ne shranjuje v NBT**, zato so po restartu vsi ob zagonu naloženi NPC-ji
  v isti fazi `% 10`. Sinhroni spawn scenarija je torej realen za zagon serverja, razpršen za
  NPC-je, ki pridejo s chunki kasneje.

**Spremembe obnašanja:** nobene (instrumentacija je privzeto izklopljena, D-007).

**Naslednja seja:** oceniti preverbo spawna, zapisati D-017, pognati baseline.

### 2026-09-23 (45) — M2.4 pognan: P1–P7 zelena, idle ni prost

**Paket:** M2.4 · **Stanje:** končano (brez M2.4r)

Preverbeni zagon (20 s + 60 s, en zagon) je v vseh 9 celicah zelen. Številke niso baseline.
Idle 500: MSPT p50 9,7 ms, p95 98,6 ms, 353 tickov nad 50 ms brez iskanja poti in brez
autosava; µs/NPC raste z N (27 → 58). **Hipoteza:** vsi NPC-ji celice so spawnani v istem
ticku, zato periodično delo (`ticksExisted % 10`) pade v isti tick — lahko artefakt
scenarija. Odločiti pred M2.6. [Zapis](meritve/2026-09-23-M2.4-obremenitve-preverba.md).

**Naslednja seja:** M2.6 — najprej preverba spawna z zamikom (ugotovitev 2), nato baseline po protokolu z GC.

### 2026-09-23 (44) — M2.4: merilne obremenitve 50/200/500, brez zagona

**Paket:** M2.4
**Stanje:** koda, fixture in scenarij narejeni in preverjeni v seji; zagon v svetu čaka na uporabnika. Render (M2.4r) odprt.

**Narejeno:**

- **`perf-run.ps1`** — celice varianta × N v enem serverju (zagon A pribije spawn, zagon B
  meri). Na celico: slay, `noppes clone grid`, `rwdiag chunks on`, krmilnik, ogrevanje,
  merjenje, posnetek, krmilnik še enkrat, branje posnetka `.json`. Merila **P1–P7**, zapis
  celice prek `meritve-lib.ps1`, poročilo `audit/m24-perf-<čas>.md`. Privzeto po protokolu
  (120 s + 300 s), `-Seconds`/`-WarmupSeconds` za hitro preverbo.
- **Fixture** `PERF_Idle`, `PERF_BojA`, `PERF_BojB` (frakciji 1/2, 100000 HP, udarec 1, ranljiva — predloge M0.6 so `Invulnerable`),
  `PERF_Skripte`, `PERF_Kontrola` — naredi jih **`dev/testworld/perf-fixture.py`** iz
  `T_Stand`/`T_Scripted`; vsaka zamenjava mora zadeti natanko enkrat.
- **`perf-kontrola.js`** (frakciji sovražni, štetje po imenu, števec skript, `despawn`) in
  **`perf-skripte.js`** (tipična lahka skripta na `tick`).
- `perf-setup-commands.txt` (gamerule, `maxEntityCramming 0`, spawn).
- `ponovitve-run.ps1 -Scenarij perf` za ponovitve ene celice.
- Scenarij `docs/scenariji/M2.4-obremenitve.md`.
- Isto sejo: oznaka `dev\run\world\rework-scenarij.txt` v `nav-run.ps1` in
  `rwdiag-run.ps1` (tudi `perf-run.ps1` jo zapiše in preveri).

**Preverjeno v seji:**

- Vseh pet `.ps1` (perf, ponovitve, nav, rwdiag, meritve-lib) brez sintaktičnih napak
  (PowerShell 7.4.6 `Parser::ParseFile`).
- Ukazi za spawn vseh devetih celic izpisani in preverjeni proti prostoru (500 v boju do z = 11).
- Bralniki `Read-Chunks`, `Read-Kontrola`, `Read-Frakcije`, `Read-DumpJsonPath`, `Get-Dist`
  pognani nad ponarejenim logom z dvojno vrstico `[FINE/CustomNPCs]` (ta se ne šteje) in nad
  posnetkom v obliki prave `.json` datoteke.
- Obe skripti sta bili po vložitvi pognani v `node` nad lažnim API-jem: krmilnik nastavi
  frakciji, prešteje po imenu, izpiše obe vrstici in se odstrani enkrat, ne ob vsakem ticku.

**Ugotovitve:**

- **Slay pred budnim svetom ne odstrani ničesar.** `noppes slay npcs` samo označi `isDead`;
  brez igralca in brez prisilnih chunkov `updateEntities` po 300 tickih ne teče, mrtvi NPC-ji
  ostanejo v `loadedEntityList` in `DiagChunkLoader.scan` jih šteje. Zato se chunki vklopijo
  pred prvim slayem in ostanejo vklopljeni čez vse celice.
- **Privzete frakcije se ne bojujejo med sabo** (`attackFactions` je prazen). Boj brez
  nastavitve frakcij bi meril dve mirujoči skupini z drugim imenom.
- **Posnetek `.json` že ima** `world.npc.loaded`, `world.npc.killed` in `npc.update.window`
  (čas posodobitev NPC-jev); iz tega sta merilo P4 in veličina `npc.us` brez nove kode v `rework/diag`.

**Ni narejeno in zakaj:**

- **Render (M2.4r):** dedicated server nima izrisa; potrebuje klient z igralcem.
- **Alokacije/GC:** `rwdiag` jih ne meri; spada v M2.6, preden se baseline zapiše.

**Spremembe obnašanja:** nobene (samo scenarij in fixture).

**Meritve:** nobene (zagon čaka).

**Naslednja seja:** `.\testworld.ps1`, `.\perf-run.ps1 -Seconds 60 -WarmupSeconds 20`; ob
zelenem zagonu M2.6.

### 2026-09-21 (43) — M2.7b pognan: razdelitev dela, šum pa je drugje, kot sem mislil

**Paket:** M2.7b (ovrednotenje dveh zagonov in popravek)
**Stanje:** **delno.** Razdelitev na hladno in ogreto je pravilna in preverjena, hipoteza o
vzroku šuma pa je bila napačna. Popravek (ogrevanje iskalnika) je narejen in čaka na zagon.

**Narejeno:**

- Ovrednotena zagona `.\nav-run.ps1` ob 12:23 (N1–N14 zelena) in 12:28 (padlo N14) ter zagon
  `.\rwdiag-run.ps1` ob 12:55 (neveljaven).
- **Ogrevanje iskalnika** v `nav-run.ps1`: `-OgrevalnihPometanj` (privzeto 2 na progo), izid
  se zavrže, nato `rwdiag reset`. Novo merilo **N15**.
- **N14 prepisan** iz časovnega praga v strukturno invarianto.
- `rwdiag-run.ps1`: takojšen padec z navodilom, če v svetu ni `T_Scripted`; `odtis.npc` ne
  nosi več kumulativnih NPC-tickov.
- Meritev [`2026-09-21-M2.7b-ogrevanje.md`](meritve/2026-09-21-M2.7b-ogrevanje.md) in razdelek
  M2.7b v scenariju.

**Ugotovitve:**

- **Razdelitev se sešteje povsod:** `prviN = iskanj = 8` in `ponN = ponovitev = 56` v vseh
  osmih pometanjih obeh zagonov. Vse, kar ni čas, je med zagonoma identično — `delezCelih`
  1,000, razmerje 1,151 / 1,236, prispelo `1/8 ob 180/180/180` in `6/8 ob 220/240/280`, razpon
  pri grlu 3,78 / 3,92 / 4,78 / 4,76.
- **Šumni pas se ni zaprl in se je pri eni veličini razširil:** `O.ponP50.poA` 254,0 proti
  42,0 µs, torej 143 % proti 114 % pred M2.7b.
- **Vzrok je stanje JVM-a med pometanji, ne število vzorcev.** `usSkupaj` zadnjih dveh
  pometanj: 9.069 in 18.076 µs (12:23) proti 4.579 in 2.939 µs (12:28). V enem zagonu se je
  JIT do konca ogrel, v drugem ne. Vseh 56 ogretih vzorcev enega pometanja si deli isto stanje
  JVM-a, zato večji vzorec zmanjša šum znotraj pometanja, ves pomemben šum pa je med njimi.
- **Razdelitev kljub temu ni bila odveč:** brez nje in brez `usSkupaj` bi bila ta razlaga
  skrita v mešanici. Meritev je ovrgla hipotezo in hkrati dala orodje, ki pove, zakaj.
- **Merilo N14 sem napisal napačno.** Časovni prag med dvema enako ogretima številkama ni
  merilo: predznak razlike je naključen (73,7 proti 69,6 pade, 42,0 proti 44,0 ne). Merilo
  veljavnosti mora biti invarianta, ki drži po konstrukciji.
- **Zagon rwdiag je meril napačen svet.** `world.npc.loaded = 17` — NAV prizorišče, ki ga je
  pustil `nav-run.ps1`, brez `T_Scripted`. Merila S1–S7 še vedno niso ovrednotena. To je tretji
  primer te vrste napake ta teden.

**Ni narejeno in zakaj:**

- Ali ogrevanje šumni pas res zapre, pove šele serija po enem zelenem zagonu.
- S1–S7 čakajo na pravilno zaporedje `.\testworld.ps1` → `.\testworld-run.ps1` → `.\rwdiag-run.ps1`.
- M2.4 in M2.6 nista začeta.

**Spremembe obnašanja:** nobene v modu. Spremenjeni sta merilni skripti; `rework/diag` je
tokrat nedotaknjen.

**Preverjeno brez sveta:** `Parser::ParseFile` na `nav-run.ps1` in `rwdiag-run.ps1` brez napak;
**novi N14 pognan nad resničnim logom zagona 12:28** — zelen v vseh štirih pometanjih, medtem
ko stara oblika pade v tretjem; ločitev ogrevalnih od merjenih pometanj preverjena nad
sintetičnim nizom osmih pometanj (ogrevalna G#0, G#1, O#0, O#1; merjena G#2, G#3, O#2, O#3).

**Naslednja seja:** `.\nav-run.ps1` (N1–N15, štiri merjena in štiri ogrevalna pometanja;
prebrati `5 cena celega pometanja` za obe merjeni — če sta si blizu, je ogrevanje zadostovalo),
nato `.\ponovitve-run.ps1 -Scenarij nav`. Ločeno: `.\testworld.ps1`, `.\testworld-run.ps1`,
`.\rwdiag-run.ps1` za S1–S7.

---

### 2026-09-21 (42) — M2.7b: peta veličina razdeljena na hladno in ogreto

**Paket:** M2.7b (koda in testi)
**Stanje:** **koda narejena, čaka na zagon.** Prevedeno in testirano v seji; v svetu še ne.

**Narejeno:**

- **`NavProbe` ima tri časovne porazdelitve namesto ene:** `firstNanos` (prvo iskanje na
  NPC, hladno), `repeatNanos` (ponovitve, ogreto) in `searchNanos` (vsota obojega, ostane
  zaradi že zapisanih meritev). Nova polja v vrstici sonde: `prviN`, `prviP50`, `prviP95`,
  `ponN`, `ponP50`, `ponP95`, `ponMax`, `usSkupaj` — vsa **na koncu** vrstice, da stari del
  regexa in že zapisane meritve berejo isto kot prej.
- **`NavSweep.DEFAULT_REPEATS` 3 → 8.** Pri osmih NPC-jih to da 8 hladnih in **56** ogretih
  vzorcev na pometanje namesto 8 in 16.
- **`nav-run.ps1`:** `-SweepRepeats` privzeto 8, branje novih polj, merili **N13** (vsaj 40
  ogretih vzorcev) in **N14** (ogreto ni počasnejše od hladnega), nove vrstice v izhodiščni
  tabeli in nove veličine v strojnem zapisu za `ponovitve-run.ps1`.
- Razdelek **M2.7b** v scenariju M2.7; 6 novih testov v `NavProbeTest` (21 → 27).

**Ugotovitve:**

- **Vzrok šuma ni bil samo premalo vzorcev, ampak dve populaciji v eni posodi.** Prvo
  iskanje na NPC plača nalaganje razredov in hladen JIT, ponovitev meri algoritem. Pri 8
  hladnih in 16 ogretih vzorcih je bil `usP95` praktično *drugo najpočasnejše hladno iskanje
  od osmih* — zato razpon do 114 % med sicer determinisičnimi ponovitvami serije (M2.5c).
- **To se vidi na enačbi, ne šele v svetu.** Na preverbeni vrstici, ki jo je izpisal
  prevedeni `NavProbe`, je mešani `usP95` 327,7 µs, ogreti `ponP95` pa 72,0 µs: mešanica
  poroča četrtkrat več, kot stane iskanje, ki ga A/B primerja.
- **Višje od 8 ponovitev se ne splača.** Pometanje teče v enem samem server ticku, zato vsako
  dodatno iskanje pokvari meritev MSPT v istem zagonu. `usSkupaj` ta vpliv naredi merljiv —
  in s tem odpre odprto vprašanje iz M2.7, zakaj je bil najpočasnejši tick (232 ms) tick s
  pathfindingom.
- **Stare in nove serije se ne mešajo tiho.** Odtis nosi `pometanjPon`, zato mešana serija
  pade na T4, nove veličine v starem zapisu pa na T5 (M2.5c). To je namen.

**Ni narejeno in zakaj:**

- Ni še pognano v svetu. Ali se šumni pas res zapre, pove šele serija
  `.\ponovitve-run.ps1 -Scenarij nav` po enem zelenem `.\nav-run.ps1`.
- Merila S1–S7 še vedno čakajo na `.\rwdiag-run.ps1`.

**Spremembe obnašanja:** nobene v modu za igralca. Spremenjena je diagnostika (`rework/diag`),
ki teče samo ob `rwdiag on`.

**Preverjeno brez sveta:** vseh **14** razredov `rework/diag` prevedenih z `javac --release 8`
proti mapiranim razredom in `customnpcs-mapped-01Oct19.jar` (manjkajoča `javax.annotation.Nullable`
in `com.google.common.collect.ImmutableSetMultimap` nadomeščena z minimalnima nadomestkoma
**izven** repozitorija); **27/27 `NavProbeTest` zelenih**; `Parser::ParseFile` na `nav-run.ps1`
brez napak; **pogodba Java ↔ PowerShell preverjena nad resnično vrstico**, ki jo je izpisal
prevedeni `NavProbe` — regex jo ujame, vseh osem novih polj pristane v pravi skupini,
`Format-Sonda` jo izpiše in N13/N14 sta nad njo zelena.

**Naslednja seja:** pognati `.\nav-run.ps1` (pričakovano N1–N14 zelena, `ponN = 56` na
pometanje), nato serijo `.\ponovitve-run.ps1 -Scenarij nav` in preveriti, ali je razpon
ogretih veličin padel pod 20 %. Vzporedno je še vedno odprt `.\rwdiag-run.ps1` (S1–S7).

---

### 2026-09-21 (41) — M2.3 zaključen: L1–L12 zelena, P1 ovržen

**Paket:** M2.3 (četrti zagon)
**Stanje:** **zaključeno.** Scenarij je veljaven v vseh štirih fazah, R2 je izmerjen, P1 je
razsojen z meritvijo in ne s sklepanjem.

**Narejeno:**

- Ovrednoten **četrti zagon** (21. 9. ob 07:45, `audit/m23-r2-2026-09-21-0745.md`).
- V meritev dopisan razdelek „Četrti zagon 21. 9. ob 07:45".
- V scenariju označeno, da je hipoteza faze D ovržena; M2.3 zaprt v tabelah stanja.

**Ugotovitve:**

- **L1–L12 zelena.** Vseh dvanajst prog ima `prevozeno + doCilja` prvega vzorca med 16,41 in
  18,06 (v zagonu 13:49 sta bili dve pri 10,94 in 11,88). Premik postavitve iz `endPhase` v
  `startPhase` je razhod odpravil, L12 ga nadzira.
- **Razsodba je prvič prišla v poročilo:** `P1 OVRZEN: proga P se premika na mrezi (17,63)
  in izven nje (17,67, gib=0,2734)`.
- **Hipoteza `pathFollow` 0,45 proti `FlyingMoveHelper` 0,5 je ovržena.** Faza D je začela
  natanko na z = −16,0; leteči NPC-ji so se premikali že v prvem vzorcu (`gib = 0,1183`) in
  fazo končali pri 15,93 prevoženih z `cele = 5/6`. Odmik pol bloka letečega NPC-ja ne ustavi.
- **Faza C proge P je prvič veljavna** (`cele = 6/6`, prevozi 16,34); v zagonu 13:49 je bila
  onesnažena in njenih številk ni bilo dovoljeno uporabljati.
- **R2 potrjen dvakrat zapored.** Faze A, B in C se med zagonoma 13:49 in 07:45 ujemajo tam,
  kjer sta oba veljavna: proga F čez zid 0/6 pri enem `navigateTo`, 6/6 ob osveženi poti,
  `cele = 6/6` pri vanilla AI. Ugotovitev „R2 je zastarela delna pot" stoji na dveh zagonih.
- **R-P1b — kaj je od P1 ostalo.** Stari `resetToStart` je postavljal na **iste koordinate**
  kot današnji `resetOffGrid`; edina preostala razlika je bila, da se je postavitev zgodila
  40 tickov pred fazo. Sama koordinata je oproščena (faza D 07:45) in sam razmik tudi
  (zagon 13:49, z = −15,5) — zmrznitev je zahtevala **oboje hkrati**. Recept je zapisan v
  meritvi; poskus (faza E) se požene šele, če ga M4 potrebuje.

**Ni narejeno in zakaj:**

- R-P1b ni razsojen. Ni blokade in M2.3 je dostavil, čemur je bil namenjen; dodajati peto
  fazo v zdaj zelen diagnostični scenarij za nevtralno vprašanje bi bilo širjenje obsega.
- Merila S1–S7 (M2.5a/b) še niso bila strojno ovrednotena — `.\rwdiag-run.ps1` ostaja odprt.
- M2.4, M2.6 in M2.7b niso začeti.

**Spremembe obnašanja:** nobene. Ta seja je samo brala in dokumentirala.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Četrti zagon ob 07:45"

**Naslednja seja:** pognati `.\rwdiag-run.ps1` (merila S1–S7, odločitveno drevo je v razdelku
M2.5a scenarija), nato **M2.7b** ali **M2.4**.

---

### 2026-09-21 (40) — M2.3 tretji zagon ovrednoten: R2 je zastarela pot, P1 še ne odločen

**Paket:** M2.3 (ovrednotenje zagona 13:49) + združitev vej
**Stanje:** **delno.** R2 je reproduciran in izmerjen, kar je bil namen paketa. P1 ostaja
odprt, ker je faza D padla na vhodnem pogoju; popravek je narejen, zagon je na uporabniku.

**Narejeno:**

- **Združitev vej.** Lokalni commit `c0ca7e1` (delo 17. 9. na namizni postaji) je bil
  rebasean na `origin/main`, ki je medtem odšel 11 commitov naprej (M2.5b, M2.5c, M2.7).
  Konflikt je bil samo v tem dnevniku; vnos 17. 9. je dobil številko 39, ker je bila 30 na
  `origin/main` že zasedena.
- Ovrednoten **tretji zagon** (13:49, `audit/m23-r2-2026-09-17-1349.md`, log
  `audit/m23-r2.log`), ki ga prejšnja seja ni videla — datoteka je bila nesledena.
- V meritev dopisan razdelek „Tretji zagon 17. 9. ob 13:49".
- **`r2-control.js`:** postavitev NPC-jev se je preselila iz `endPhase` v `startPhase`, tik
  pred `rememberStart`.
- **`r2-run.ps1`:** novo merilo **L12**, popravljena razsodba o P1 in popravljen razdelek
  `## Razsodba o P1`, ki prej sploh ni prišel v poročilo.
- Skripta vstavljena v oba klona `R2_Control.json` (7556 znakov).

**Ugotovitve:**

- **Zmrznitve iz zagonov 12:17 in 12:37 v tretjem zagonu ni nikjer.** `prevozeno` ni 0,00 v
  nobeni fazi in progi, `gib` je povsod nad nič. Edina sprememba je bil popravljeni
  `resetToStart`.
- **R2 je reproduciran in obhod izmerjen.** Leteča proga F pride čez zid **0/6** pri enem
  samem `navigateTo` (faza A, `cele = 0/6`, prevozi 7,95), **6/6** ob osveženi poti (faza B,
  prevozi 16,36) in `cele = 6/6` pri vanilla AI (faza C). Kopenska kontrola ne pride čez v
  nobeni fazi (L5 zelen). Razlika med A in B/C je **ena sama: ali se pot osvežuje.**
- **Zato R2 v tem prizorišču ni okvara letenja, ampak zastarela delna pot** — isti
  mehanizem kot Q11 pri M2.2, le da ga tam povzroči domet in tu ovira. Popravek v M4 mora
  osveževati pot, ne spreminjati `FlyingMoveHelper`.
- **Faza D P1 ne razsodi, ker se ni začela tam, kjer bi se morala.** Med `R2-C-END`
  (tick 1460) in `R2-D-START` (tick 1500) mine 40 server tickov; v njih se je prosta leteča
  proga premaknila 7,4 bloka nazaj proti cilju, čeprav je `endPhase` pred postavitvijo
  poklical `clearNavigation()` in `setAttackTarget(null)`. Onesnaženi sta natanko progi z
  neovirano potjo do cilja (`C P` in `D P`); progi za zidom v 40 tickih nista prišli nikamor.
- **Vsota `prevozeno + doCilja` prvega vzorca to pokaže brez ugibanja.** Deset čistih prog je
  med 16,39 in 17,34, obe onesnaženi pa pri 10,94 in 11,88. Iz tega je nastalo merilo L12.
- **Posledica za že zapisane številke:** proga P v fazi C zagona 13:49 je prav tako
  neveljavna in se ne sme uporabljati. Prej tega ni bilo videti.
- **Dve napaki orodja.** Razsodba o P1 sploh ni prišla v poročilo (blok je bil dopisan v
  `$head` pred izračunom `$p1` in pred `$head = @()` v koraku 9), merilo razsodbe pa je
  merilo „ali je proga prišla do cilja" (`prevozenoMax >= 12`), ne „ali je zmrznila".

**Ni narejeno in zakaj:**

- P1 ni razsojen. Za to je potreben četrti zagon na Windowsu.
- M2.4, M2.6 in M2.7b niso začeti; merila S1–S7 (M2.5a/b) še niso bila strojno ovrednotena.

**Spremembe obnašanja:** nobene. Spremenjena sta scenarij in merilna skripta, ne mod.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Tretji zagon ob 13:49"

**Preverjeno brez sveta:** `node --check` (vseh 6 skript testnega sveta),
`Parser::ParseFile` na `r2-run.ps1` (0 napak, PowerShell 7.4.6 v oblačnem okolju),
`preveri-skladnost.py` (SKLADNOST OK), `preveri-markerje.js` (264 vzorčnih vrstic, POGODBA OK),
`vstavi-skripto.py` na obeh klonih (7556 znakov, ujemanje preverjeno).

**Naslednja seja:** pognati `.\r2-run.ps1` četrtič in prebrati razdelek `## Razsodba o P1`
iz poročila; če L12 pade, je onesnažena še katera proga in izid te faze ne velja. Nato
`.\rwdiag-run.ps1` (merila S1–S7) in **M2.7b** ali **M2.4**.

---

### 2026-09-17 (39) — P1 zožen na `FlyingMoveHelper`; poskus s pol bloka pripravljen

*(delo s 17. 9. na namizni postaji; v `origin/main` združeno 21. 9., zato je številka vnosa 39 in ne 30)*

**Paket:** M2.3 (razčlenitev pojava P1)
**Stanje:** **delno.** Dve od treh razlag sta ovrženi z meritvijo, tretja ima kodno podlago
in poskus, ki jo potrdi ali ovrže. Zagon je na uporabniku.

**Narejeno:**

- Ovrednoten drugi zagon (12:37, `audit/m23-r2-2026-09-17-1237.md`), z `dStarost` in `gib`.
- V meritev dopisan razdelek „Ponovitev 17. 9. ob 12:37 — P1 dobi odgovor".
- **Popravljen `resetToStart`**: NPC se vrne na zapomnjeno izhodišče, ne na konstanto
  `START_Z`. To je bila tiha napaka scenarija, ne moda.
- **Nova faza D** v `r2-control.js`: enaka fazi B, samo izhodišče je pol bloka izven mreže
  (`resetOffGrid`) — torej natanko stanje obeh dosedanjih zagonov.
- `r2-run.ps1`: faza D v seznamu faz, merilo **L11** (kopenska kontrola v fazi D) in
  razsodba `P1 POTRJEN / OVRŽEN / NEODLOČEN`, ki jo skripta izpiše sama in zapiše v
  poročilo.
- `preveri-markerje.js` razširjen na fazo D (markerja `R2-D-START`/`R2-D-END`, 264 vzorčnih
  vrstic).

**Ugotovitve:**

- **`dStarost` je 20 v vseh fazah in progah.** Svet entitete posodablja ves čas; razlaga
  „svet jih ne posodablja" je **ovržena**.
- **`gib` je točno 0** v vseh 22 vzorcih obeh zmrznjenih faz, na istih NPC-jih pa je v fazi A
  dosegel 0,2306. Razlaga „gibanje je, a ga poje `move()`" je **ovržena**. Ostane
  `FlyingMoveHelper`, ki gibanja ne doda.
- **Drugi zagon je znak za znak enak prvemu** v vseh starih veličinah. Scenarij je
  determinističen, zato je primerjava med zagonoma veljavna.
- **Iz samih številk se da prebrati, kje je razlika.** `doCiljaMin` po resetu je 16,01, kar
  ustreza z = −16,0 (meja bloka); v fazi A se izide samo z = −15,5 (sredina bloka). Reset je
  NPC-je premikal za pol bloka, česar ni načrtoval nihče.
- **Kodna podlaga hipoteze:** `PathNavigate.pathFollow:286-292` prestevilči vozlišče samo pri
  odmiku < 0,45 (`0,75 − width/2`, `width = 0,6` iz `updateHitbox:1083-1086`), medtem ko
  `FlyingMoveHelper:39` doda gibanje samo pri `d3 > 0,5`. Odmik **točno 0,5** ne zadosti ne
  enemu ne drugemu. `EntityMoveHelper` takega praga nima — zato kopenska proga teče naprej.
- Če se to potrdi, P1 ni napaka scenarija, ampak **bug originala**, in ne velja samo za
  teleport: enako obtiči leteči NPC, ki se ustavi na taki koordinati po prihodu na cilj ali
  po spawnu. Takrat gre v M4 skupaj z obhodom ovire.

**Ni narejeno in zakaj:**

- Hipoteza ni potrjena. Za to je potreben zagon na Windowsu.
- Merili L4a in L4b bosta v naslednjem zagonu povedali tudi, ali je popravek reseta zalegel;
  če proga P v fazi B še vedno ne pride do cilja, vzrok ni bilo izhodišče.
- M2.4 in M2.7 nista začeta.

**Spremembe obnašanja:** nobene. Spremenjen je scenarij, ne mod.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Ponovitev ob 12:37"

**Preverjeno brez sveta:** `node --check`, `vstavi-skripto.py` (7445 znakov, oba klona),
`Parser::ParseFile` na `r2-run.ps1` in `matrika-run.ps1`, `preveri-markerje.js` s fazo D
skozi `Read-Samples` (264 vzorcev, 4 faze × 3 proge po 22), `preveri-skladnost.py`.

**Naslednja seja:** pognati `.\r2-run.ps1`, prepisati razsodbo o P1 v meritev, nato **M2.4**
ali **M2.7**.
### 2026-09-18 (39a) — M4.14a: formacije v Javi (skupina NPC-jev kot ena enota)

*Integrirano na `origin/main` (M3.6) 27. 9.: ta vnos je nastal na lokalni veji 18. 9. in ni bil pushan; odločitev preštevilčena D-017 → D-021.*

**Paket:** M4.14a (nov, izven vrstnega reda faz — izrecna zahteva uporabnika)
**Stanje:** **koda in testi narejeni; v svetu ni pognano** (M4.14b čaka na build in zagon)

**Ozadje:** uporabnik vodi vojsko NPC-jev s chat skripto (`legija`, `obramba`, `march`), ki
premik izvede s timerji na igralcu in `navigateTo` za vsakega NPC-ja posebej. Vprašal je,
ali bi v Javi lahko marširali skupaj, in naročil zasnovo, vpis v načrt in prototip. Paket je
pred M2 zaključkom, zato ga protokol (§2 "ne začenjaj paketa iz kasnejšega milestona")
sicer ne bi dovolil; izjema je zapisana v D-021.

**Narejeno:**

- Zasnova [`06-FORMACIJE.md`](06-FORMACIJE.md): šest vzrokov, zakaj skripta razpade (z
  mesti v kodi), model sidra in mest, zaprta zanka, ozko grlo, faze, parametri, kaj je in
  kaj ni preverjeno.
- Paket `rework/formation` (15 datotek). Jedro brez Minecrafta: `SquadPlanner`,
  `FormationShape` (legija/obramba/kolona/march z razmiki iz skripte), `SlotAssigner`
  (dodelitev brez križanja), `PathTrack`, `FormationMath`. Adapter: `Squad`,
  `FormationMoveTask` (prioriteta −1, maska `PASSIVE|LOOK`), `SquadManager` (na event bus
  samo, dokler obstaja enota), `WorldTerrain`, ukaz `/rwsquad`, `FormationApi` za skripte.
- `CustomNpcs.java`: registracija `/rwsquad` in `SquadManager.clear()` ob ustavitvi.
- `build.gradle`: nova testa izključena iz `testOriginal`.
- Scenarij [`M4.14-formacije.md`](scenariji/M4.14-formacije.md): dimni test F1–F12 in A/B
  proti skripti na prizorišču M2.7 (FA1–FA6).
- Predelana uporabnikova chat skripta [`formacije/vojska-chat.js`](formacije/vojska-chat.js)
  (preverjena z `node --check` in z nadomestnim svetom v Node).
- `03-FAZE.md` (M4.14, M4.14a–c), `01-ARHITEKTURA.md` (D-021), `README.md`.

**Preverjeno v seji (D-014):** celotno drevo `rework/**` in popravljen `CustomNpcs.java` se
prevedeta z `javac --release 8` proti mapiranim razredom (za obstoječo kodo sta bila potrebna
nadomestka `GameProfile` in `ImmutableSetMultimap` izven repozitorija). 21 novih testov
zelenih. Simulacija pokaže: 20 članov prehodi 80 blokov, noben med pohodom ne zaostane več kot
5,5 bloka; sidro se prilagodi najpočasnejšemu; obtičan član enoto najprej ustavi, po 80
tickih je opravičen in enota gre naprej; pred vrati se formacija stisne in za njimi razpre;
delna pot se nadaljuje; končna mesta so na sredini blokov in unikatna.

**Ni narejeno in zakaj:**

- Build, zagon in dimni test v svetu — seja nima Gradla ne Minecrafta; to je M4.14b.
- `nav-control.js` še nima načina `FORMACIJA` za A/B (FA1–FA6); naslednja seja.
- Leteči in plavajoči NPC-ji niso bili niti razmišljeni do konca: drug navigator in move
  helper; zapisano kot omejitev.

**Ugotovitve:**

- **`setMovingType(0)` v skripti pusti `EntityAIReturn` aktiven.** Ko NPC nima poti in ni
  na domu, gre domov (`EntityAIReturn.shouldExecute`, movingType 0 →
  `!isVeryNearAssignedPlace()`, meja ±0,2 bloka). Uporabnikovi NPC-ji se zato po prihodu
  vrnejo na staro mesto, razen če imajo izklopljen "return to start". Formacije to rešijo s
  `setStartPos` ob koncu.
- **`Walking` flag CustomNPCs je vezan na navigatorjevo pot** (`EntityNPCInterface:464`), od
  njega pa sta odvisna `addVelocity` (knockback) in render pri standing type 3. Zato člani
  hodijo prek poti z eno točko in ne prek golega `moveHelper.setMoveTo`.
- **Hipoteza, nereproducirana:** `testOriginal` izključi `DiagTest` in sorodne, ne pa
  `SlowTicksTest` in `NavProbeTest`, ki prav tako uporabljata razreda iz `rework/diag`. Če
  gradle `testOriginal` ni bil pognan od M2.5a, lahko pade z `NoClassDefFoundError`. Ne
  popravljam (ni moj paket); preveri naslednji `.\dev.ps1 testOriginal`.

**Spremembe obnašanja:** nov ukaz `/rwsquad` in `FormationApi`; brez klica nobene (tabela).

**Meritve:** nobene v svetu.

**Naslednja seja:** uporabnik naj požene `.\dev.ps1 buildPatchedMod --offline`,
`.\verify-package.ps1` (pričakovano: `CustomNpcs.class` spremenjen kot prej, novih 26 razredov
v `rework/formation`) in dimni test F1–F12. Nato način `FORMACIJA` v `nav-control.js` in A/B.

---

### 2026-09-18 (38) — prva serija ponovitev: scenarij M2.7 je determinističen, šumen je samo čas

**Paket:** M2.5c (zagon)
**Stanje:** **zaključeno** — serija je veljavna in zapisana

**Izid:** `.\ponovitve-run.ps1` (tri ponovitve, vsaka svež svet, 2,6 / 2,1 / 2,0 minute):
**T1–T6 zelena**, vsaka ponovitev tudi N1–N12 zelena, odtis enak v vseh treh.
Polni zapis: [meritev](meritve/2026-09-18-M2.5c-ponovitve-nav.md).

**Glavna ugotovitev:** od 59 veličin jih ima **46 razpon nič**. Vse, kar opisuje vedenje
skupine in kakovost poti (veličine 1, 2, 3, 4 in 6), je med zagoni **bitno enako** — kdo
prispe, kdaj prispe, kako dolga je pot, kako se skupina stisne pred vrati. Za A/B v
M4.10–M4.13 to pomeni, da statistika ni potrebna: en zagon pred in en po, in vsaka razlika
je resnična.

**Šumna je izključno veličina 5** (µs na eno iskanje), vseh osem njenih oblik, najslabša
`O.usP95.start` z **114,3 %** razpona. Vzrok ni navigacija, ampak premalo vzorcev: eno
pometanje naredi **8 iskanj**, p50 in p95 pa sta percentila nad temi osmimi.

| | prag za A/B |
|---|---|
| veličine 1, 2, 3, 4 | vsaka sprememba je pomenljiva (razpon 0) |
| veličina 6 (dodelitev poti na tick) | ≥ 3,4 % |
| veličina 5, ogreto (p50) | ≥ 5 % na progi G, ≥ 47 % na progi O |
| veličina 5, hladno (p50) | ≥ 25 % na progi G, ≥ 64 % na progi O |

**Ugotovitve:**

- **Determinizem drži tudi čez dan in čez sejo.** Zagon 17. 9. (druga seja, drug svet) ima
  za iste veličine iste vrednosti: 1/8 ob 180/180/180, 6/8 ob 220/240/280, razpon 3,78 in
  4,78, `novihPoti` 372. To je četrta neodvisna potrditev iste številke.
- **Hladno pometanje meri JIT, ne algoritma** (`G.usMax`: 3 192 µs hladno proti 181 µs
  ogreto). Za A/B se uporablja ogreto pometanje; hladno ostane ločen podatek o ceni prvega
  iskanja.
- **M4.11 in M5.6 sta blokirana na meritvi, ne na kodi.** Oba ciljata ceno iskanja, torej
  edino veličino, ki v tej obliki ni merljiva. Zato nov paket **M2.7b**: več vzorcev na
  pometanje (ali skupni čas namesto percentilov).
- Protokol je v praksi poceni: cela serija traja 7 minut, ker `nav-run.ps1` sam traja dve.

**Ni narejeno in zakaj:** serija za `rwdiag-run.ps1` (merila S1–S7) ni tekla; zapis zagona
ima, požene pa se s `.\ponovitve-run.ps1 -Scenarij rwdiag`.

**Spremembe obnašanja:** nobene.

**Meritve:** [`meritve/2026-09-18-M2.5c-ponovitve-nav.md`](meritve/2026-09-18-M2.5c-ponovitve-nav.md).

**Naslednja seja:** **M2.7b** (več vzorcev za veličino 5, da postane merljiva), nato
**M2.4** ali **M2.6**.

---

### 2026-09-18 (37) — M2.5c: protokol ponovitev in zapis zagona

**Paket:** M2.5c
**Stanje:** **zaključeno** (koda in samotest); prva serija ponovitev v svetu je na uporabniku

**Izhodišče:** tabela M2.7 je nastala iz **enega** zagona, zato nima razpona. Dokler ga
nima, se o razliki med dvema zagonoma ne da reči, ali je izboljšava ali šum — in po
`05-SEJA-PROTOKOL.md` je M4.10–M4.13 ni dovoljeno razglasiti za izboljšavo.

**Narejeno:**

- `meritve-lib.ps1` — strojno berljiv **zapis zagona** (`Write-MeritevJson` /
  `Read-MeritevJson`, shema 1) z dvema ločenima deloma: **odtis** (pogoji, pod katerimi je
  meritev nastala; med ponovitvami se ne sme razlikovati) in **veličine** (kar se meri;
  razpon čez ponovitve *je* merilni šum). Plus `Get-Mediana` in `Get-Sum`.
- `ponovitve-run.ps1` — N ponovitev, vsaka svež svet (`.\testworld.ps1`) in svoj proces;
  združevanje po imenih veličin, izpis šumnega pasu (mediana, razpon, relativni razpon,
  oznaka `enaka` / `stabilna` / `SUMNA`), poročilo v `audit\m25c-<scenarij>-<datum>.md`,
  merila **T1–T6**.
- `ponovitve-samotest.ps1` — 7 primerov, **16 trditev**, brez Minecrafta: ponarejeni
  scenarij zapiše vnaprej znane številke. Pokriva zeleno pot, obe oznaki, spremenjen odtis,
  premalo ponovitev, padel zagon in manjkajočo veličino.
- `nav-run.ps1` in `rwdiag-run.ps1` dobita `-JsonPath` in po poročilu zapišeta zapis zagona.
  Mapiranje imen je v `nav-run.ps1` izločeno v čisto funkcijo `New-NavZapis`, da se da
  preveriti brez Minecrafta.
- `docs/scenariji/M2.5c-ponovitve.md`.

**Preverjeno v seji (brez Minecrafta):**

- `.\ponovitve-samotest.ps1`: **16/16 zelenih** v oblačnem PowerShellu 7.4.6. Test je bil
  najprej pokvarjen namerno (`Get-Sum` vrne razpon 0) — takrat pade na oznakah `stabilna`
  in `SUMNA`, kar potrjuje, da trditvi nista prazni.
- `New-NavZapis` pognan nad **pravim logom zagona z 17. 9.** (`audit\m27-nav.log`, funkcije
  izluščene iz `nav-run.ps1` prek AST, brez podvajanja kode): 59 veličin in odtis
  `npc=16 progaG=8 progaO=8 vrataX=-10 zidZ=72 ciljZ=78 zidBlokov=140 pometanj=4`. Številke
  se ujemajo z objavljeno tabelo (1,151 / 1,236; 237,6 / 67,6 µs; 1/8 in 6/8; razpon 3,78 in
  4,78; `naTick` 0,313, max 16). Zapis se tudi prebere nazaj.
- Vseh pet skript sintaktično brez napak.

**Ugotovitve:**

- **Ponovitev brez reseta sveta ni ponovitev.** Svet v `dev\run\world` se med zagoni
  ohrani z vsemi NPC-ji vred; drugi zagon `nav-run.ps1` bi meril 32 NPC-jev namesto 16.
  Zato je reset del protokola, merilo T4 (enak odtis) pa ga ujame tudi, če odpove. Dobra
  novica: N2 v `nav-run.ps1` bi tak zagon tako ali tako podrl — napaka je glasna, ne tiha.
- **Odtis mora biti ločen od veličin.** Če sta v istem loncu, je mediana čez tri zagone
  lahko povprečje treh različnih poskusov in izgleda enako verodostojno kot meritev.
- **Prag 20 % je oznaka, ne pravilo.** Pravi prag za posamezno veličino je njen izmerjeni
  razpon; zato T6 ničesar ne podre, samo izračuna.
- Kjer je mediana 0 (npr. `ai.naTickP95`), relativni razpon ne obstaja; stolpec pove `n/a`
  namesto izmišljenega nadomestka.

**Ni narejeno in zakaj:**

- **Serija ponovitev ni pognana** — potrebuje Windows, Javo 8 in Minecraft. Tabela M2.7
  ostane enozagonska, dokler uporabnik ne požene `.\ponovitve-run.ps1`.
- `r2-run.ps1` zapisa zagona še nima; dobi ga, ko bo pojav P1 razrešen in bo scenarij spet
  dal veljavne številke (sicer bi zapisovali zapise neveljavnih zagonov).
- Ocena trajanja serije: trije zagoni `nav-run.ps1` zaporedoma, vsak s svojim resetom sveta.

**Spremembe obnašanja:** nobene v modu. Spremembe so samo v merilnih skriptah; `-JsonPath`
je neobvezen in brez njega se vedenje obeh skript ne spremeni.

**Odločitev:** **D-016** v [`01-ARHITEKTURA.md`](01-ARHITEKTURA.md) §9 — kaj je ponovitev,
zakaj je reset sveta njen del in zakaj sta odtis in veličine ločena.

**Meritve:** nobene nove (paket ne meri, ampak pove, kdaj je meritev veljavna).

**Naslednja seja:** prva serija `.\ponovitve-run.ps1` (če jo je uporabnik pognal → tabela
M2.7 dobi razpon in gre v `docs/meritve/`), sicer **M2.4** (50/200/500 NPC-jev) ali
**M2.6** (baseline originala).

---


### 2026-09-17 (36) — M2.7 pognan: N1–N12 zelena, izhodiščna tabela obstaja

**Paket:** M2.7 (zagon)
**Stanje:** **zaključeno** — meritev je veljavna in zapisana

**Izid:** `.\nav-run.ps1` po popravku dveh zagonov: zid 140 blokov, 16 NPC-jev,
`chunki=35 zavrnjeni=0`, scenarij od `NAV-INIT` do `NAV-SUM`, **merila N1–N12 zelena**.
Polni zapis: [meritev](meritve/2026-09-17-M2.7-navigacija-baseline.md).

**Šest veličin (original):**

| # | Veličina | G (grlo) | O (odprto) |
|---|---|---|---|
| 1 | delež celih poti | 1,000 | 1,000 |
| 2 | razmerje dolžine p50 / p95 | 1,151 / 1,236 | 1,151 / 1,236 |
| 3 | prispelo, faza A / faza B | 1/8 · 6/8 (mediana 240) | 8/8 · 8/8 (mediana 200) |
| 4 | razpon pri grlu, A / B | 3,78 / 3,92 | 4,78 / 4,76 |
| 5 | µs na iskanje p50, hladno / ogreto | 237,6 / 67,6 | 167,9 / 77,8 |
| 6 | dodelitev poti na tick povp. / p95 / max | 0,313 / 0 / 16 (skupaj) | |

**Ugotovitve:**

- **Delna pot se v tem prizorišču ne pojavi** (32/32 celih). Pri 14 blokih in vratih na osi
  cilja proračun 200 vozlišč in domet 32 nista omejitev. **M4.10 dobi s tem vhodni pogoj:
  potrebuje prizorišče z razdaljo čez `NpcNavRange` ali s pravim obvozom**, sicer je delež
  celih poti trivialno 1,000 in popravek ni merljiv.
- **Grlo se pokaže v času, ne v geometriji.** Razmerje dolžine je na obeh progah enako;
  razlika je 1/8 proti 8/8 (faza A) in 6/8 proti 8/8 (faza B).
- **En klic `navigateTo` proti osveženemu: 1/8 proti 6/8.** Izhodiščna številka za M4.10 in
  M4.13 in razlaga, zakaj neosvežena skripta izgleda kot okvarjen AI (past Q11).
- **Iskanja so sunkovita:** p95 = 0 na tick, max 16. Najpočasnejši ticki (232 / 57 / 54 /
  48 ms, brez chunkov in brez autosave) sovpadajo z vzorčnimi ticki skripte, ko dobi vseh
  16 NPC-jev pot hkrati. To je primer, ki ga naslavlja M5.6; pripis je sočasnost, dokaz
  pride z M3.1.
- **Skupina je pri grlu ožja, ne širša** (3,8 proti 4,8) — stisne se pred vrati.
- Ob tem zagonu sta prvič tekli tudi vrstici M2.5a/M2.5b: `RWDIAG-SAVE izlocenih=1 … maxVsi=232,031 maxBrez=232,031` — **najpočasnejši tick tokrat ni bil autosave**, ampak tick s pathfindingom. Merila S1–S7 so v `rwdiag-run.ps1` in še niso bila strojno ovrednotena.

**Ni narejeno in zakaj:** meritev je **en zagon**; protokol zahteva tri (M2.5c še ni).
Dva NPC-ja od osmih na progi G ne prispeta niti v fazi B — kandidata sta zamašek pred
vrati in `canNavigate()`, loči ju šele M3.1.

**Spremembe obnašanja:** nobene.

**Meritve:** [`meritve/2026-09-17-M2.7-navigacija-baseline.md`](meritve/2026-09-17-M2.7-navigacija-baseline.md).

**Naslednja seja:** **M2.5c** (protokol ponovitev, da bo tabela imela razpon) ali **M2.4**
(50/200/500 NPC-jev). Pred M4.10 je treba dodati prizorišče z razdaljo čez 32 blokov.

---

### 2026-09-17 (35) — Prvi zagon M2.7: prizorišče 750 blokov od spawna

**Paket:** M2.7 (zagon), brez sprememb v modu
**Stanje:** vzrok imenovan in odpravljen; ponovni zagon je na uporabniku

**Kaj se je zgodilo:** `.\nav-run.ps1` je padel na N2 in nato desetkrat iztekel v timeout.
V logu: `Cannot place blocks outside of the world` (zid), nobenega odziva na 16 ukazov
`noppes clone spawn`, `RWDIAG-CHUNKS … npc=0 razlog=v svetovih ni nalozenih NPC-jev`.

**Vzrok:** seme testnega sveta ima spawn na **(743, 4, −231)**, dedicated server brez
igralca pa drži naložene samo chunke v območju ±128 blokov okoli spawna
(`World.isSpawnChunk`). Prizorišče M2.7 pri `z = 64…78` je bilo 750 blokov proč, torej
sploh ni bilo naloženo: `fill` je padel, `noppes clone spawn` pa **ni javil ničesar** in
NPC-jev ni bilo. `setup-commands.txt` (M0.6) in `fixture-run.ps1` imata zato
`setworldspawn 0 4 0`; `nav-setup-commands.txt` ga ni imel.

**Narejeno:**

- `setworldspawn 0 4 0` v `nav-setup-commands.txt`, s pojasnilom, zakaj brez njega scenarij
  tiho odpove.
- `nav-run.ps1`: bralnik `Read-FillBlocks` in merilo **„zid je postavljen"** (`fill` mora
  javiti vsaj 100 blokov) — preverja **učinek** in ne dejanja.
- `Read-Chunks` zdaj bere **obe** obliki vrstice `RWDIAG-CHUNKS`; prej je ob zavrnitvi
  regex zgrešil in scenarij je povedal „chunki niso naloženi" namesto „v svetu ni NPC-jev".
- Ob `npc = 0` ali nepostavljenem zidu scenarij **pade takoj** namesto desetih minut
  čakanja na markerje, ki jih ne bo.

**Ugotovitve:**

- **`noppes clone spawn` v nenaložen chunk molči.** Ne javi napake in ne postavi ničesar.
  Edini znak je posredni (`npc=0`), zato ga mora scenarij izrecno preveriti.
- Isti razred napake kot 17. 9. pri `server.properties` (dnevnik 32): preverba dejanja
  („poslanih 30 ukazov") je bila zelena, učinka pa ni bilo. Vsak nov scenarij mora imeti
  merilo, ki prebere **posledico** v svetu.

**Preverjeno v seji:** `nav-run.ps1` sintaktično brez napak; `Read-Chunks` in
`Read-FillBlocks` sta pognana nad **pravim logom padlega zagona** in dasta
`npc=0, razlog=v svetovih ni nalozenih NPC-jev` ter `0 blokov` — torej bi novi merili
vzrok imenovali v prvih tridesetih sekundah.

**Spremembe obnašanja:** nobene.

**Meritve:** nobene — zagon ni dal veljavnih podatkov.

**Dopolnitev po drugem zagonu:** sam `setworldspawn 0 4 0` **ne zadošča** — `fill` je spet
javil „Cannot place blocks outside of the world". Chunke okoli novega spawna naloži šele
naslednji zagon serverja (`prepareSpawnArea`), zato ima M0.6 (`testworld-run.ps1`) **dva
zagona**: A pribije spawn in shrani, B postavi prizorišče. `nav-run.ps1` ima zdaj isto.
Opomba v M0.6 je bila zapisana že 16. 9. („Zakaj trije zagoni") — nov scenarij je moral
prevzeti postopek, ne samo ukaz.

**Naslednja seja:** ponovni `.\nav-run.ps1`.

---

### 2026-09-17 (34) — M2.7: merila kakovosti navigacije (sonda, opazovalec, scenarij)

**Paket:** M2.7 (del a)
**Stanje:** koda, testi, scenarij in merila končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Izhodišče:** D-012 pravi, da vanilla pathfindinga ne prepisujemo, ampak ga popravljamo po
stopnjah — vsako pod stikalom in z meritvijo. Meritve pa ni bilo: paketi M4.10–M4.13 in
M5.6 so ostali brez številke, proti kateri bi se lahko izmerili. M2.7 je ta manjkajoči
vhodni pogoj.

**Narejeno:**

- **`rework/diag/NavProbe.java`** — merilni razred brez Minecraft tipov (kot `SlowTicks`):
  delež celih poti, razmerje dolžina/zračna razdalja, doseg delne poti in porazdelitev
  časa iskanja. Ima vrstico z markerjem (`RWNAV-SONDA`, pod drugim markerjem tudi
  `RWNAV-POMET`), razdelek v posnetku in JSON.
- **`rework/diag/NavSweep.java`** — pometanje: vsak merjeni NPC poišče pot do istega cilja.
  Filter po predponi imena, ponovitve, poročilo `naTleh` in `chunkiForced` v isti vrstici.
- **Opazovalec v `DiagEventCollector`** — `nav.ai.path.new`, `nav.ai.paths.per.tick` in
  `nav.ai.navigating`; vrstica `RWNAV-AI` v posnetku.
- **Ukaz `rwdiag nav <x y z [ponovitev] [maxNpc] [imePredpona]>`** in `rwdiag nav status`.
- **Scenarij:** `dev/testworld/nav-control.js` (dve progi, dve fazi), `nav-setup-commands.txt`,
  fixture `NAV_WalkG`, `NAV_WalkO`, `NAV_Control`, `preveri-nav-markerje.js` in
  **`nav-run.ps1` z merili N1–N12**. Dokument: `docs/scenariji/M2.7-navigacija.md`.
- **23 novih testov** (21 `NavProbeTest`, 2 v `DiagSnapshotTest`); skupaj **84 testov
  instrumentacije, vsi zeleni**, prevedeno z `javac --release 8` v seji.
- Odločitev **D-015** (kako se meri in zakaj tako).

**Preverjeno v seji:**

- Prevedenih vseh 18 razredov `rework/diag` proti mapiranim Forge/Minecraft razredom;
  84/84 testov zelenih.
- `nav-run.ps1` sintaktično brez napak (PowerShell 7.4.6 `Parser::ParseFile`).
- **Razčlenjevalnik je pognan nad pravim izpisom, ne nad izmišljenim.** Vrstice
  `RWNAV-POMET`, `RWNAV-SONDA` in `RWNAV-AI` je ustvarila prevedena koda, vrstice `NAV-S`,
  `NAV-CAS` in `NAV-PRISPEL` pa minificirana `nav-control.js` nad ponarejenim svetom
  (`node preveri-nav-markerje.js`). Bralniki `Read-Samples`, `Read-Cas`, `Read-Pometi`,
  `Read-Sonda` in `Read-NavAi` iz `nav-run.ps1` so bili nato pognani nad tem logom, skupaj
  s podvojenimi vrsticami `[FINE/CustomNPCs]`: štiri pometanja so prebrana kot štiri in ne
  kot osem.
- `vstavi-skripto.py` je potrdil, da je skripta v `NAV_Control.json` po normalizaciji
  identična viru.

**Ugotovitve:**

- **Navigatorjeva pot ni merilno orodje.** `PathNavigate.getPathToPos` (`:111-124`) si cilj
  zapomni v `targetPos` in ob istem cilju vrne že izračunano pot. Sonda, ki bi ga
  uporabila, bi spremenila stanje tistega, kar meri, pri ponovitvah pa bi merila branje
  predpomnilnika namesto iskanja poti. Lasten `PathFinder` nad istim `NodeProcessor`-jem
  meri isto kodo brez stranskega učinka.
- **`Path.getTarget()` je `@SideOnly(Side.CLIENT)`** (`Path.java:152`). Celosti poti se na
  strežniku torej ne da prebrati iz poti same; primerjati je treba zadnjo točko s ciljnim
  vozliščem. To je drobna podrobnost, ki bi brez branja dekompiliranega izpisa prišla na
  dan šele ob `NoSuchMethodError` v svetu.
- **Razmerje dolžine sme meriti samo cela pot.** Razmerje delne poti primerja dolžino poti,
  ki nikamor ne pride, z razdaljo do cilja, ki ga ni dosegla — čim prej bi iskanje obupalo,
  tem „boljše" bi bilo razmerje. Test
  `partialPathIsExcludedFromTheRatioButNotFromTheShare` drži to mejo.
- **Ponovitve iskanja ne smejo skozi isto pot kot meritev kakovosti.** Prva različica je
  ponovitve zapisovala kot navadna iskanja in delež celih poti bi bil odvisen od števila
  ponovitev. Ločena `recordTimeOnly` to odpravi; test to zahteva.
- **Šesta veličina je spodnja meja in tako je tudi zapisana.** Opazovalec šteje spremembo
  identitete objekta `Path`, torej uspešne dodelitve; iskanje, ki vrne `null` ali ga
  `canNavigate()` zavrne, je zanj nevidno. Številka, ki bi se delala natančnejšo, kot je,
  bi bila slabša od odsotne.
- **Vzorec razčlenjevalnika iz prave kode je poceni in ujame razred napake, ki ga sicer
  ujame šele zagon.** Isti prijem kot pri M2.5b; tokrat je pokazal, da je bil pomožni
  vzorec regexa shranjen v spremenljivki, ki bi ob preimenovanju tiho dala prazen niz —
  zato je zdaj funkcija `Get-SondaVzorec`, katere manjkajoč klic pade takoj.

**Ni narejeno in zakaj:**

- **Meritve ni**, ker `nav-run.ps1` potrebuje Windows, Javo 8 in gradle z Minecraftom.
  Tabela šestih veličin nastane ob prvem zagonu; do takrat M4.10–M4.13 in M5.6 ostanejo
  zaprti.
- Letečih NPC-jev scenarij ne meri (ima jih M2.3) in cene pri stotinah NPC-jev ne (M2.4).
- `push` ni narejen — poverilnice so na Windows strani. Uporabnik požene
  `git push origin main`; lokalno sta pred originom dva commita (M2.5b in ta).

**Spremembe obnašanja:** nobene v modu. Sonda se sproži samo z ukazom, opazovalec pa teče
le, dokler je merjenje vklopljeno (zbiralnik je takrat že prijavljen na event bus).

**Meritve:** nobene nove.

**Naslednja seja:** prebrati izid `.\nav-run.ps1` (N1–N12) in šest veličin prepisati v
`docs/meritve/`; če je N10 zelen in N1–N9 padejo, je napaka v prizorišču, ne v sondi. Nato
**M2.4** ali **M2.5c**.

---

### 2026-09-17 (33) — M2.5b: autosave tick ima svojo porazdelitev

**Paket:** M2.5 (del b)
**Stanje:** koda, testi in scenarij končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Izhodišče:** meritev 17. 9. je vprašanje iz M2.5a zaprla — prepad `p95 → p99` je bilo
ogrevanje (81,8 ms → 4,3 ms), `max` pa je v vsakem zagonu en sam tick, ki sovpada z
autosave (58 / 74 / 87 / 124 ms). Ostalo je vprašanje, kaj s tem tickom: pustiti ga v
porazdelitvi pomeni, da primerjava pred/po meri vanilla shranjevanje; izbrisati ga pomeni
lagati, ker server ta čas res porabi.

**Narejeno:**

- `Diag.tick(...)` vsak tick uvrsti v natanko eno od treh veder: `server.tick.ns.nosave`
  (porazdelitev brez autosave), števec `server.tick.save` (izločeni) in števec
  `server.tick.nocontext` (tick brez konteksta, stara pot `Diag.tick(long)`). Tick brez
  konteksta **ne** gre med čiste: ne vemo, ali je v njem tekel autosave, in ugibanje bi
  porazdelitvi dalo videz natančnosti, ki je nima.
- `server.tick.ns` ostane nespremenjen, tabela najpočasnejših tickov tudi — autosave tick
  je v obeh še naprej viden. Nova je samo **druga** porazdelitev istega ticka.
- Posnetek izpiše vrstico z markerjem:
  `RWDIAG-SAVE izlocenih=1 brezKonteksta=0 ostalo=1199 p99vsi=0.705 p99brez=0.700 maxVsi=87.000 maxBrez=0.700`.
- `rwdiag-run.ps1`: bralnik `Read-SaveLine` in merila **S5–S7**. S5 trdi, da se vedra
  seštejejo v `n` porazdelitve (izločitev ne izgubi in ne šteje dvakrat), S6 da rep brez
  autosave ni daljši od celotnega, S7 pa je navzkrižna preverba dveh **neodvisnih**
  mehanizmov: kadar `max` pade pod proračun šele z izločitvijo autosave, mora biti prva
  vrstica tabele najpočasnejših tickov označena s `save > 0`. Če si porazdelitev in
  tabela nasprotujeta, je eden od njiju pokvarjen.
- 6 novih testov (4 v `DiagTest`, 2 v `DiagSnapshotTest`); skupaj **61 testov
  instrumentacije, vsi zeleni**, prevedeno z `javac --release 8` v seji.
- Scenarij: nov razdelek **M2.5b** v `docs/scenariji/M2.1-diag.md`.

**Preverjeno v seji:**

- Prevedeno in pognano: 61/61 zelenih.
- `rwdiag-run.ps1` sintaktično brez napak (PowerShell 7.4.6 `Parser::ParseFile`).
- Bralnik `Read-SaveLine` in merila S5–S7 so **pognana nad pravim posnetkom**, ki ga je
  ustvarila ista koda (1200 tickov, eden z autosave): S5 zelen (1 + 0 + 1199 = 1200),
  S6 zelen (`p99 0,705 → 0,700`, `max 87,000 → 0,700`), pogoj za S7 izpolnjen.

**Ugotovitve:**

- **Izločitev mora biti preverljiva, ne samo narejena.** Brez S5 bi bila napaka v uvrščanju
  (tick v dveh vedrih ali v nobenem) neopazna — porazdelitev bi izgledala lepše, ker bi ji
  manjkali ticki. Vsota veder je najcenejša možna zaščita.
- Ločitev je tudi priprava na **M2.6**: baseline originala mora navesti obe številki,
  sicer primerjava po posegu v AI ni poštena v nobeno smer.

**Ni narejeno in zakaj:**

- **M2.5c — protokol ponovitev** (koliko zagonov, kako se povprečijo, kolikšen razpon je
  še sprejemljiv) ostaja. Protokol iz `01-ARHITEKTURA.md` §7 zahteva 3 ponovitve; skripta
  jih danes ne zna pognati v enem zagonu in razpona ne izpiše.
- Zagona v svetu ni: `rwdiag-run.ps1` potrebuje Windows, Javo 8 in gradle z Minecraftom.

**Spremembe obnašanja:** nobene v modu. Nova porazdelitev in dva števca se polnijo samo,
dokler je merjenje vklopljeno.

**Meritve:** nobene nove.

**Naslednja seja:** prebrati izid `.\rwdiag-run.ps1` (S5–S7) in se odločiti, ali gre
naprej M2.5c (protokol ponovitev) ali M2.4 (scenariji 50/200/500). Če `brezKonteksta > 0`,
najprej najti klicno mesto, ki še kliče `Diag.tick(long)` brez konteksta.

---

### 2026-09-17 (32) — Zakaj je `r2-run.ps1` obstal: napačen svet, ne zanka

**Paket:** M2.3 (zagon), brez sprememb v modu
**Stanje:** vzrok imenovan, scenarija zaščitena; ponovni zagon je na uporabniku

**Kaj se je zgodilo:**

- Uporabnik je javil, da se je `.\r2-run.ps1` "zaciklal". Ni bila zanka: skripta je čakala
  na markerje `R2-INIT` in `R2-{A,B,C}-START`, ki jih ni moglo biti — v svetu ni bilo
  nobenega NPC-ja. V logu je bilo 20× `Could not find clone file` / `Unknown npc` in
  `RWDIAG-CHUNKS ... npc=0 razlog=v svetovih ni nalozenih NPC-jev`. Trije timeouti po
  180 s dajo ~10 minut navideznega obstanka, nato bi skripta padla z NEUSPESNO.
- **Vzrok:** `dev\run\server.properties` je bil od smoke testa (`level-name=m05-smoke`),
  zato je server naložil svet `dev\run\m05-smoke`. `r2-run.ps1` fixture kopira v
  `dev\run\world` — v drug svet. Na tej postaji testni svet sploh ni bil postavljen:
  v `dev\run\world` je bila samo mapa `customnpcs`, brez `level.dat`.
- Preverba "štiri R2 fixture datoteke so v svetu (kopiranih 4)" je bila **zelena in
  neresnična**: datoteke so res bile kopirane, samo ne v svet, ki se je zagnal.

**Narejeno:**

- `r1-run.ps1` in `r2-run.ps1` ob zagonu primerjata `level-name` in `level-seed` z
  `dev\testworld\server.properties`. Ob neujemanju scenarij **pade takoj**, še pred zagonom
  serverja, in pove, naj se požene `.\testworld.ps1`. Popravek ene vrstice (samo
  `level-name`) je bil zavrnjen: smoke test prepiše cel `server.properties`, zato bi
  meritev tekla v drugem okolju (seed, vidna razdalja, težavnost), le v pravem svetu.
- `fixture-run.ps1` in `matrika-run.ps1` popravka ne potrebujeta — `testworld.ps1` kličeta
  sama in s tem dobita pravi `server.properties`.
- Preverjeno v seji: oba scenarija sta sintaktično brez napak (PowerShell 7.4.6
  `Parser::ParseFile`), logika preverbe pa je **pognana** nad tremi pravimi datotekami
  (smoke properties, seme, popravljeno stanje) — smoke properties pade, seme in
  popravljeno stanje gresta skozi.

**Ugotovitve:**

- Merilo, ki preverja **dejanje** (datoteke so kopirane) namesto **učinka** (server jih
  vidi), ne varuje pred ničimer. Pravilna oblika je tista iz M2.1d: pogoj meritve se
  prebere iz sistema, ki ga meri — tu iz `server.properties`, ki ga bere server.
- Ta razred napake je drag na tak način, da ga je lahko spregledati: nič ni padlo, nič ni
  javilo napake, samo počakalo je deset minut in potem povedalo napačno zgodbo.

**Spremembe obnašanja:** nobene v modu; samo dve skripti.

**Meritve:** nobene — zagon ni dal veljavnih podatkov.

**Naslednja seja:** ponovni zagon `.\r2-run.ps1` po `.\testworld.ps1` (uporabnik je svet
medtem že postavil; `server.properties` je spet od testnega sveta).

---

### 2026-09-17 (31) — Združitev vej: podvojen M2.2, M2.5a prestavljen na origin

**Paket:** brez novega paketa — sanacija razhajanja med delovnima postajama
**Stanje:** končano; `push` čaka na uporabnika

**Kaj se je zgodilo:**

- Seja je na namizni postaji začela z `git status` in nadaljevala nedokončan M2.2 iz seje
  16. 9.: prevedla kodo, dopisala dva testa, napisala scenarij, popravila tri napake v
  `rwr1-run.ps1` in commitala (`ec9ede6`).
- Uporabnik je nato rekel, naj pogledam git. `origin/main` je bil star: stal je pri
  `b7827cd` (M2.1d), ker `git fetch` na tej postaji pade na posredniškem strežniku
  (`HTTP 403`). Prava oddaljena veja je bila `37e8d74` — **devet commitov naprej**.
- Na originu so bili **M2.2 (R1 reproduciran in izmerjen), M0.7 (M0 zaključen) in M2.3
  (R2, faza A)**. M2.2 je tam rešen povsem drugače: brez Java zbiralnika, s skriptami
  NPC-jev (`r1-control.js`) in `r1-run.ps1`. **Današnji M2.2 je bil torej podvojeno delo.**

**Narejeno:**

- Oddaljena veja prenesena v repozitorij skozi oblačno okolje z `git bundle` (recept je v
  Znanih omejitvah), `origin/main` je zdaj pravilen.
- Današnji M2.2 **umaknjen z `main` na vejo `m22-seja`** (`ec9ede6`). Ni izbrisan in ni
  združen: M2.2 je na originu zaključen in dvakratna reprodukcija iste zahteve bi bila
  breme, ne varnost.
- **M2.5a prestavljen (rebase) na `origin/main`.** Razrešeni trki: `README.md`,
  `docs/04-STANJE.md` (glava, tabela M2, dnevnik, Znane omejitve, rep porazdelitve) in
  `docs/01-ARHITEKTURA.md`.
- **Trk ID-jev odločitev:** obe veji sta uporabili `D-012` (tu pripis počasnih tickov, na
  originu navigacija). Naši odločitvi sta preštevilčeni v **D-013** (tabela top-N namesto
  profilerja) in **D-014** (seja prevaja in testira sama); vse sklice v dnevniku sem
  popravil. Odločitev originala `D-012` (navigacija) ostane nedotaknjena.
- Preverjeno po združitvi: 17 razredov `rework/**` prevedenih z `javac --release 8`,
  **55 testov instrumentacije zelenih**, `rwdiag-run.ps1` po samodejni združitvi
  **sintaktično brez napak** (PowerShell 7.4.6 `Parser::ParseFile` v oblačnem okolju —
  postopek z originala, prvič uporabljen tudi tu).

**Ugotovitve:**

- **Ista vrsta napake je bila najdena dvakrat neodvisno.** Origin jo je popravil v
  `rwdiag-run.ps1` (`49f7889`, "n-ti zadetek nad celim logom je bral dvojnik prvega"),
  ta seja pa v `rwr1-run.ps1` (merilo R1-1 je primerjalo fazo A samo s sabo). Vzrok je
  isti: `reply()` piše markerje v log **in** pošiljatelju, zato je vsak marker podvojen.
  To ni naključje dveh skript, ampak lastnost ukaza — vsak nov razčlenjevalnik markerjev
  mora brati sidrano na oznako faze, ne po vrstnem redu pojavitve.
- **Prepad `p95 → p99` je že pojasnjen**: origin ga je zaprl z ogrevanjem (81,8 ms → 4,3 ms),
  `max` pa je vsakič en tick z autosave. Odprto vprašanje iz M2.5a je s tem odgovorjeno,
  merila S1–S4 pa ostanejo uporabna kot strojna potrditev pripisa.
- **Iz današnjega M2.2 ostanejo uporabne tri ugotovitve**, tudi če koda ostane na veji:
  ime NPC-ja s presledkom ali podpičjem tiho razbije razčlenjevanje povzetka in stolpce
  CSV (velja za vsak marker, ki ime piše nezaščiteno); `/summon` v 1.12.2 izpiše
  `Object successfully summoned`, ne `Summoned new <ime>`; in zgornja ugotovitev o
  podvojenih markerjih.

**Ni narejeno in zakaj:**

- `push` ni narejen — poverilnice so na Windows strani, lupina seje pa nima ne njih ne
  dostopa do GitHuba. Uporabnik požene `git push origin main`; lokalno sta pred originom
  **dva** commita (M2.5a), veja `m22-seja` pa ostane samo lokalno, dokler je ne potisne
  posebej.
- Veja `m22-seja` ni prečiščena in ne bo vzdrževana. Če se M2.5/M2.7 kdaj lotita merjenja
  jahanja, je tam pripravljen Java zbiralnik z 78 zelenimi testi.

**Spremembe obnašanja:** nobene. Koda se ni spremenila; spremenila se je zgodovina veje.

**Meritve:** nobene.

**Naslednja seja:** **najprej preveri, ali je `origin/main` res svež** (če `fetch` pade,
uporabi recept z bundlom iz Znanih omejitev), šele nato izberi paket. Prvi na vrsti je
ponovni zagon `.\r2-run.ps1` z `dStarost` in `gib` (pojav P1), ob prvem `.\rwdiag-run.ps1`
pa še vrstica pripisa S1–S4.

---

### 2026-09-16 (30) — M2.5a: pripis počasnih tickov + seja prevaja in testira sama

> **Opomba o vrstnem redu.** Ta vnos je nastal 16. 9. na drugi delovni postaji in je v
> skupno zgodovino prišel šele 17. 9., ko sta se veji združili. Zato stoji pred vnosi
> z dne 17. 9., čeprav ima starejši datum. Številka (30) je naslednja prosta; vnos (18)
> je bil ob združitvi že zaseden.


**Paket:** M2.5 (del a)
**Stanje:** koda in testi končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Narejeno:**

- `rework/diag/SlowTicks.java` — tabela 16 najpočasnejših tickov meritve s kontekstom:
  zaporedna številka ticka, čas od začetka meritve, trajanje, koliko NPC-jev je tiknilo,
  koliko chunkov se je naložilo/odložilo, koliko svetov se je shranilo. Ločeno še števci,
  koliko tickov je preseglo 10, 25, 50 in 100 ms. Brez Minecraft tipov, zato enotsko
  testljiv; na vroči poti ena primerjava na tick brez alokacije.
- `DiagKeys`: `chunk.load`, `chunk.unload`, `world.save`.
- `DiagEventCollector`: naročnine na `ChunkEvent.Load`, `ChunkEvent.Unload` in
  `WorldEvent.Save` (vse s preverbo `world.isRemote`), števci na tick se ob `START`
  počistijo in se ob `END` predajo tabeli skupaj z indeksom ticka.
- `Diag.tick(...)` dobi različico s kontekstom; stara `tick(long)` ostane in zapiše
  kontekst kot neznan (`-`), da obstoječi testi in klici ostanejo veljavni.
- `DiagSnapshot`: nov razdelek v berljivem izpisu in ključ `slowTicks` v JSON.
- `rwdiag-run.ps1`: merila **S1–S4**, bralnika `Read-SlowTable` in `Read-TickMillis`
  (`server.tick.ns` je v tabeli izpisan v ms z decimalkami, zato ga stari bralnik celih
  števil ni prebral) ter vrstica pripisa
  `skupaj N, z autosave X, z nalaganjem chunkov Y, brez obojega Z`.
- 22 novih testov (`SlowTicksTest`); skupaj 55 testov instrumentacije, vsi zeleni.
- Scenarij: nov razdelek **M2.5a** v `docs/scenariji/M2.1-diag.md`. Odločitvi **D-013**
  (zakaj tabela top-N in ne profiler) in **D-014** (seja prevaja in testira sama).

**Ugotovitve:**

- **Seja lahko od zdaj sama prevaja in poganja teste.** Priključena mapa je dosegljiva
  skozi lupino na uporabnikovem računalniku, prevajanje pa teče v oblačnem okolju proti
  mapiranim razredom iz `dev/build/tmp/recompileMc/compiled` (28 MB, Forge + Minecraft z
  mapiranimi imeni) in `dev/libs/customnpcs-mapped-01Oct19.jar`. Manjkata samo `guava` in
  `log4j`, ki v repozitoriju ne obstajata; za prevajanje ju nadomestita dva minimalna
  nadomestka **izven** repozitorija. S tem je preverjeno: sintaksa, imena in podpisi Forge
  API-jev (`ChunkEvent.Load#getChunk`, `WorldEvent.Save`, `WorldEvent#getWorld`) in vsa
  logika brez Minecrafta. 27 testov iz M1, ki potrebujejo `log4j` v času izvajanja, se v
  seji ne da pognati; te še vedno pokrije `.\dev.ps1 test --offline`.
- Napaka, ki jo je ujel prvi test: števci ravni so bili najprej šteti od najvišje ravni
  navzdol z ustavitvijo pri prvi nedoseženi — tick z 30 ms zato ni bil štet niti pri
  10 ms. Test `tickOverALevelCountsInEveryLowerLevelToo` je to pokazal takoj. Ravni se
  odslej štejejo navzgor. To je natanko razlog za D-014: napaka bi sicer prišla v svet.
- Aritmetika, ki jo bo izid potrdil ali ovrgel: autosave teče vsakih 900 tickov
  (`MinecraftServer.tick():762`), torej sta v 60-sekundni meritvi to **največ dva** ticka.
  Počasnih tickov je bilo približno 12. Če jih pripis pripiše autosave, se nekaj ne ujema
  in je treba pogledati, kaj še piše v istem ticku.

**Ni narejeno in zakaj:**

- Meritev ni ponovljena. Zagon `rwdiag-run.ps1` potrebuje Windows, PowerShell, Javo 8 in
  gradle z Minecraftom; seja ima lupino na uporabnikovem računalniku, ne pa tega okolja.
- `rwdiag-run.ps1` spet ni sintaktično preverjen (ni PowerShella). Vsi štirje novi
  regularni izrazi pa **so** preverjeni proti pravemu izpisu posnetka, ki ga je seja
  ustvarila lokalno, vključno s tem, da stari bralnik celih števil še vedno bere
  `npc.per.tick`.
- M2.5 ni zaključen: izločitev autosave ticka iz percentilov in protokol ponovitev
  (koliko zagonov, kako se povprečijo) ostajata.
- **Commit `f3ea71f` je narejen, `push` ne.** Lupina seje je Linux in nima dostopa do
  Windows Credential Managerja, zato `git push` vpraša za geslo, ki ga ni. Uporabnik
  požene `git push origin main` iz korena projekta. Enkraten pogoj tega paketa je bil tudi
  `rm .git/index.lock` — `git fetch` iz prejšnje seje je pustil zaklep, brisanje pa je v
  priključeni mapi privzeto onemogočeno, dokler ga uporabnik ne dovoli.

**Spremembe obnašanja:** nobene v modu. Trije novi števci in tabela se polnijo samo,
dokler je merjenje vklopljeno; zbiralnik je na event bus prijavljen samo takrat. Nobena
nova koda ne posega v entitete, shranjevanje ali mrežo.

**Meritve:** nobene nove — ta paket je orodje, s katerim bo naslednja meritev povedala
vzrok namesto številke.

**Naslednja seja:** pognati `.\dev.ps1 test --offline` (pričakovano 55 testov
instrumentacije zelenih) in `.\rwdiag-run.ps1`, nato prebrati vrstico
`pripis tickov nad 50 ms: …`. Odločitveno drevo je v razdelku M2.5a scenarija:
chunki → podaljšaj ogrevanje in ponovi; autosave → poročaj posebej; brez obojega → rep je
delo v ticku in naslednji korak so klicna mesta (M2.1b), ne porazdelitve. Šele nato M2.2
(reprodukcija R1; uporabnik je potrdil, da sta oba NPC-ja CustomNPC).
### 2026-09-17 (29) — M2.3 pognan: R2 reproduciran v fazi A, fazi B in C razveljavi zmrznitev

**Paket:** M2.3 (reprodukcija R2)
**Stanje:** **delno.** Faza A je veljavna in R2 je reproduciran. Fazi B in C sta
razveljavljeni zaradi novega pojava; scenarij je razširjen tako, da ga naslednji zagon
razloži.

**Narejeno:**

- Ovrednoten zagon uporabnika z dne 17. 9. ob 12:17 (`audit/m23-r2.log`,
  `audit/m23-r2-2026-09-17-1217.md`).
- Napisana meritev [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](meritve/2026-09-17-M2.3-R2-reprodukcija.md).
- V `dev/testworld/r2-control.js` dodani merjeni veličini `dStarost` (`getAge()`, torej
  `ticksExisted`) in `gib` (`motionX/Y/Z`); skripta na novo vložena v `R2_Control.json`.
- V `r2-run.ps1` dodana merili **L9** (entitete se tikajo) in **L10** (merilnik gibanja
  dela), funkciji `Min-DStarost` in `Max-Gib` ter razdelek poročila „Proge, ki se niso
  premaknile", ki razlago pove sam.
- `preveri-markerje.js` dopolnjen z `getAge`/`getMotion*` in preverbo, da ima vsaka vzorčna
  vrstica novi polji.
- Scenarij `docs/scenariji/M2.3-R2.md` dopolnjen z obema veličinama, meriloma L9/L10 in
  razlago, zakaj sta nastala.

**Ugotovitve — R2 (faza A, veljavna):**

- **Leteči NPC se pri oviri obnaša kot kopenski.** Proga F (leteči + zid) prevozi 7,95
  bloka in obstane **pri zidu**; proga W (kopenski + isti zid) prevozi 6,93. `cezOviro` je
  0/6 pri obeh. Zid je visok 4 bloke; leteči se dvigne za en blok (y max 5,00) in tam
  obvisi.
- **Letenje samo po sebi deluje.** Proga P (leteči, prosto) prevozi 16,27 bloka in pride na
  1,00 bloka do cilja, `cele=6/6`. Razlika F proti P je zato razlika **ovire**, ne letenja.
- **Najpomembnejša številka je `cele=0/6` na progi F, že v prvem vzorcu.** Leteči NPC ne
  obstane zato, ker bi vodenje odpovedalo, ampak zato, ker **iskanje poti ne vrne poti čez
  zid**. Pot čez zid je ≈ 24 blokov, torej pod `NpcNavRange` 32 — domet ni vzrok in past
  Q11 je izključena že s postavitvijo.
- Skupina se pred oviro **zgosti** (`razpon` 6,65 → 1,26), enako kot pri R1.

**Ugotovitve — pojav P1 (fazi B in C, neveljavni):**

- Po resetu z `setPosition` se **nobeden od dvanajstih letečih NPC-jev ne premakne niti za
  0,01 bloka** v 900 tickih, v dveh različnih načinih vodenja. Kopenska proga se premika
  normalno, torej reset sam po sebi ni pokvarjen.
- Navigator zanje ves čas javlja `navig=6/6` in na prosti progi `cele=6/6`: **pot je cela,
  premika ni.**
- Števec `npc.update` kaže 21,99 klicev na tick čez vseh 1531 tickov, torej svet entitete
  posodablja; razlaga „svet jih ne posodablja" je malo verjetna, a je števec agregaten.
- Ostanejo tri razlage, ki se izključujejo (ne posodablja se / vodenje ne doda gibanja /
  gibanje poje `move()`). Med njimi loči natanko par `dStarost` + `gib`, ki je zdaj dodan.
  **Razlaga pride iz zagona, ne iz ugibanja** — to je pravilo, ki se je pri Q11 že enkrat
  izplačalo.

**Ni narejeno in zakaj:**

- P1 ni pojasnjen. Za to je potreben zagon na Windowsu, ki ga seja ne more izvesti.
- M2.4 in M2.7 nista začeta; smiselna sta šele, ko je M2.3 veljaven v vseh treh fazah.
- Vzrok, zakaj iskanje poti ne vrne poti čez zid, ni dokazan. Omejitev 200 vozlišč
  (`PathFinder.findPath:65`) je hipoteza; dokaz zahteva klicna mesta v `PathNavigate`
  (M2.1b, čaka na M3.1).

**Spremembe obnašanja:** nobene. Ta seja ni spremenila moda.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](meritve/2026-09-17-M2.3-R2-reprodukcija.md)

**Preverjeno brez sveta:**

| Kaj | Kako | Izid |
|---|---|---|
| `r2-control.js` je veljaven ES5 | `node --check` | zeleno |
| skripta je vložena v klon | `vstavi-skripto.py` | zeleno, 6997 znakov |
| `r2-run.ps1` in `matrika-run.ps1` se razčlenita | PowerShell 7.4.6 `Parser::ParseFile` | zeleno |
| **novi polji prideta skozi razčlenjevalnik** | `preveri-markerje.js` + `Read-Samples`/`Min-DStarost`/`Max-Gib` iz AST-ja | zeleno: `dStarost(min)=20`, `gib(max)=0,04` na gibajoči progi in 0 na mirujoči |
| koordinate se ujemajo med tremi datotekami | `preveri-skladnost.py` | zeleno |

**Naslednja seja:** pognati `.\r2-run.ps1`, razdelek „Proge, ki se niso premaknile" prepisati
v meritev, P1 razložiti ali zavreči, nato **M2.4** ali **M2.7**.

---

### 2026-09-17 (29) — Q12 odprt: uvoz zunanjih 3D modelov ni v načrtu

**Paket:** brez (vprašanje uporabnika, brez spremembe kode)

Uporabnik je vprašal, ali rework kje predvideva podporo za **prenesene modele** (npr. model
zmaja s spleta, dodan prek kode, URL-ja ali lokalne namestitve), ki bi jih NPC lahko prevzel,
in ali to morda že pokriva R3 (migracija 1.7.10+).

**Ne pokriva.** Preverjeno v `docs/02-ZAHTEVE.md` (R3), `docs/03-FAZE.md` (M8.1–M8.11) in
`README.md`: katalog M8 ne vsebuje uvoza modelov, ker tega nima niti CustomNPC+.

**Kaj mod zna danes (iz dekompilata):**

- `INPCDisplay.setSkinUrl()` / `setCapeTexture()` / `setOverlayTexture()` — samo **teksture**,
  ne geometrija.
- `INPCDisplay.setModel(String id)` (`entity/data/DataDisplay.java:405`) — NPC prevzame model
  **katerekoli registrirane entitete** iz Forge registryja (`ModelData.entityClass`). Zmaj iz
  že nameščenega moda torej deluje že zdaj; samostojna datoteka ne.
- `ModelData` / `ModelPartData` in `client/model/` (`ModelNpcDragon`, `ModelPony`, part paketi)
  so **hardcoded Java `ModelBase` razredi**. Runtime loaderja za modelne datoteke ni.

**Kaj bi dodajanje pomenilo (nepotrjena ocena, XL):** loader formata (`.bbmodel` je
najverjetnejši kandidat), mapiranje kosti na obstoječi part sistem (sicer odpadejo oprema,
skaliranje po delih in hitbox), vezava na animacijski format iz M7 — ki mora govoriti o
kosteh zunanjega modela, ne o fiksnih Steve delih —, distribucija modela do vseh igralcev in
render cache invalidacija (tveganje je že zapisano v `PLAN_IMPLEMENTACIJE.md`).

**Odločitev:** zaenkrat **samo odprto vprašanje Q12**, brez nove zahteve in brez faze.
Odgovor ima smisel šele, ko je M7 dovolj jasen, da je znano, na kakšen skelet se model veže.

---

### 2026-09-17 (28) — M2.3 napisan: reprodukcija R2 s tremi progami, brez zagona

**Paket:** M2.3 (reprodukcija R2 — leteči NPC in ovira)
**Stanje:** **delno.** Vse, kar se da narediti brez Windowsa, je narejeno in preverjeno.
Zagon v svetu je na uporabniku: `.\r2-run.ps1`.

**Narejeno:**

- [`docs/scenariji/M2.3-R2.md`](scenariji/M2.3-R2.md) — scenarij s tremi progami, osmimi
  vzorčenimi veličinami in merili L1–L8.
- `dev/testworld/r2-control.js` — skripta krmilnika, tri faze.
- Štirje fixture kloni: `R2_Flyer` (`MovementType 1`), `R2_Walker`, `R2_Target`, `R2_Control`;
  skripta je v `R2_Control.json` vložena z `vstavi-skripto.py`, ne ročno.
- `dev/testworld/r2-setup-commands.txt` — prizorišče in 21 NPC-jev, idempotentno.
- `r2-run.ps1` — gonilnik po vzoru `r1-run.ps1`, s poročilom v `audit/m23-r2-<datum>.md`.
- `matrika-run.ps1` ima novo stopnjo `r2`; vrstica IA4 matrike zabeležena kot „scenarij
  obstaja, čaka na zagon".
- `dev/testworld/preveri-markerje.js` — nova preverba pogodbe markerjev **brez Minecrafta**.
- `dev/testworld/preveri-skladnost.py` — nova preverba, da si koordinate scenarija ne
  nasprotujejo med `r2-control.js`, `r2-setup-commands.txt` in `r2-run.ps1`.

**Zakaj tri proge in ne ena.** Zahteva govori samo o „letečem NPC-ju z oviro". Če taka
skupina obstane, to samo po sebi ne pove, ali je kriva ovira, letenje ali navigacija
nasploh — zadnje je natanko past Q11. Scenarij zato vodi hkrati: **F** leteči + zid,
**W** kopenski + isti zid, **P** leteči brez ovire. F proti P izolira oviro, F proti W
izolira letenje.

**Ugotovitve iz kode (vse pred zagonom, iz dekompilacije in Forge-patchanega Minecrafta):**

- `FlyingMoveHelper.java:50`: ob oviri se akcija postavi na `WAIT` in **nič drugega** —
  obhoda ni. Smer se popravi le vsake 4 ticke (`:35`).
- `FlyingMoveHelper` **nikoli ne prebere `this.speed`**, ki mu ga navigator nastavi
  (`PathNavigateFlying.java:78`), ampak vedno `MOVEMENT_SPEED / 2.5`. Hitrost iz
  `navigateTo(x,y,z,speed)` torej na letenje **ne vpliva**. To je novo in gre v R2.
- `EntityNPCFlying.travel()` pri `canFly()` gravitacije sploh ne doda; ostane samo dušenje
  0,91. Leteči NPC, ki obstane, torej **obvisi** in ne pade — simptom je mirovanje v zraku.
- Vanilla `FlyingNodeProcessor` širi vozlišča v 3D, `PathFinder.findPath` pa se ustavi po
  **200 vozliščih** in vrne **delno** pot. V 3D je 200 vozlišč bistveno manj napredka kot
  v 2D, zato je delna pot pri letenju verjetnejša kot pri hoji.
- Zaradi tega scenarij meri `cele=b/N`: koliko poti se res konča pri cilju.
  `getNavigationPath()` (`EntityLivingWrapper.java:41-51`) vrne zadnjo točko poti in prav
  ta številka loči „ni poti" od „pot do zidu". M2.2 tega še ni meril.

**Zakaj so razdalje take, kot so.** Start → cilj je 16 blokov, zid je 4 bloke visok in
75 blokov širok. Pot čez zid je dolga ≈ 24 blokov, torej **pod** `NpcNavRange` 32: če
proga F ne pride, vzrok **ni** domet iskanja poti in izid je enoznačen. Obhod okoli konca
zidu je dolg ≈ 40–52 blokov, torej zunaj dometa — zid je prava ovira, ne dekoracija.

**Preverjeno brez sveta:**

| Kaj | Kako | Izid |
|---|---|---|
| `r2-control.js` je veljaven ES5 | `node --check`, tudi po minifikaciji | zeleno |
| skripta je pravilno vložena v klon | `vstavi-skripto.py` (primerja vir in vloženo) | zeleno, 6316 znakov |
| `r2-run.ps1` in `matrika-run.ps1` se razčlenita | PowerShell 7.4.6 `Parser::ParseFile` | zeleno |
| **markerji se ujemajo z razčlenjevalnikom** | `preveri-markerje.js` + funkcije iz `r2-run.ps1` nad izpisom | zeleno: 9 markerjev po enkrat, 22 vzorcev na fazo in progo (L6 zahteva 20) |
| koordinate se ujemajo med tremi datotekami | `preveri-skladnost.py` | zeleno: 6/6/6 + 3 cilji, zid visok 4, pot čez zid ≈ 24 < 32, obhod 40–52 > 32 |

Zadnja vrstica je pomembna: pri M0.7 je merilo padlo prav zato, ker se izpis in
razčlenjevalnik nista ujemala. Zdaj se to preveri brez zagona sveta.

**Ni narejeno in zakaj:**

- **Scenarij ni pognan.** Seja ima na uporabnikovem računalniku linuxovo lupino brez
  PowerShella, Gradla in Minecrafta. Zagon, meritev in vpis v `docs/meritve/` so naslednji
  korak in so na uporabniku.
- µs na iskanje poti in iskanj na tick (veličini 5 in 6 iz M2.7) nista merjeni — zahtevata
  klicna mesta v `PathNavigate`, torej M2.1b, ki čaka na prenos razredov v M3.1.
- Načina C (dragon/momentum) in nagiba ni kaj meriti; še ne obstajata.

**Spremembe obnašanja:** nobene. Ta seja ni spremenila moda; dodala je scenarij, fixture in
skripte.

**Meritve:** nobene — scenarij še ni pognan.

**Naslednja seja:** pognati `.\r2-run.ps1`, izid zapisati v `docs/meritve/2026-…-M2.3-R2-reprodukcija.md`
in v tabelo Meritve, nato **M2.4** ali **M2.7**.

---

### 2026-09-17 (27) — Prehod 1 integracijske matrike je zelen; M0.7 je s tem pognan

**Paket:** M0.7 (zaključek)
**Stanje:** **končano in pognano.** `audit/m07-matrika-2026-09-17-1133.md`, commit `49f7889`:
**4 stopnje zelene, 0 rdečih, 0 preskočenih.**

| Stopnja | Izid | Trajanje |
|---|---|---|
| verify-package | zeleno | 1 s |
| M0.6 testni svet W1–W8 | zeleno | 172 s |
| M2.1 diagnostika D1–D7, C1–C6 | zeleno | 118 s |
| M2.2 reprodukcija R1 E1–E6 | zeleno | 109 s |

Prehod 1 ni več samo dokument: postopek iz §3 matrike je izveden od začetka do konca, iz
enega ukaza, s poročilom, ki nosi hash jarja in commit. Vrstice IN1, IN2, IN4, IN6, IA7, IS1,
IL1 in IL7 so s tem **zelene v svetu**, ne samo na papirju.

**Q11 popravek je hkrati potrjen z meritvijo.** V fazi A ima kontrolna proga zdaj **8/8 poti**
in prevozi 24,68 bloka (prej 4/8 in 22,71), v fazi B pride do cilja (0,40 bloka). Proga M ima
**0/8 poti v obeh fazah** — ugotovitev R1 drži neodvisno od Q11. Ponovitev je dopisana v
[meritev](meritve/2026-09-17-M2.2-R1-reprodukcija.md).

**Nova drobtina za M3:** proga M je tu prevozila 1,19 bloka (prvič 0,06). Ponavljajoči
`navigateTo` torej premakne nosilca za dober blok, čeprav `isNavigating()` ostane `false` —
premik brez poti. Ne spremeni izida, gre pa v M3 kot opažanje.

**Spremembe obnašanja:** nobene. Ta seja ni spremenila ne moda ne skript, samo zapisala izid.

**Naslednja seja:** **M2.3** (reprodukcija R2 — leteči NPC čez steno na x=20) ali **M2.7**
(navigacijski sklop, vhodni pogoj za M4/M5). Prehod 1 se odslej pogane pred vsako predajo
paketa; prehod 2 (klient, GUI, IC1–IC6) ostane odprt in zahteva igralca.

---

### 2026-09-17 (26) — Drugi zagon: fixture zelen, D7b lazno negativen

**Paket:** M0.7 (drugi zagon in popravek harnessa)
**Stanje:** `fixture-run.ps1` **zelen v celoti**; `matrika-run.ps1` zelen do M2.1, kjer je padel
zaradi napake v merilni skripti, ne v modu. Popravljeno; tretji zagon je na uporabniku.

**Fixture — deluje:**

- Zagon B: `TW-FIX-QUEST id=1 ime=TW_Quest tip=5 nov=1`, `TW-FIX-DIALOG id=1 ime=TW_Dialog
  quest=1 nov=1`, `TW-FIX-ATTACH npc=T_Trader slot=0 dialog=1`, `TW-FIX-DONE`.
- Zagon C po restartu: ista dva objekta z **`nov=0`** in istim id — round-trip z diska drži.
- V semenu sta zdaj `customnpcs/quests/TW/1.json` in `customnpcs/dialogs/TW/1.json`, oba
  zapisana z modovim `NBTJsonUtil` (`ModRev 18`, `DialogQuest 1`). **IC1 in IC2 nista več
  blokirana na GUI.**
- Iz na novo shranjenega `T_Trader.json` je v seme prenesen **samo blok `NPCDialogOptions`**
  (`Dialog 1`, `Title TW_Dialog`, `DialogSlot 0`). Cela datoteka bi prinesla še runtime šum
  (`Motion`, `OnGround`, `Air`, `Fire`, trader polja) in tiho spremenila fixture, s tem pa
  primerljivost meritev. `fixture-run.ps1` zdaj ta prenos naredi ciljno (merilo F17).

**Matrika — W1–W8 zeleno, M2.1 rdeče, in to je bila napaka meritve:**

`rwdiag-run.ps1` je javil `ticki mirujejo po izklopu (1227 -> 1267)`. Vzrok: **vsak odgovor
ukaza `/rwdiag` je v logu dvakrat** — enkrat kot konzolni odgovor
(`[minecraft/DedicatedServer]`) in enkrat prek `LogWriter` (`[FINE/CustomNPCs]`,
`CommandRwDiag:133`). Trije posnetki so torej dali šest `RWDIAG-OK` vrstic, `Read-OkMarker`
pa je n-ti zadetek štel nad celim logom: "drugi posnetek" je bil v resnici **dvojnik prvega**.
Prave številke iz istega loga: dump1 = 1227, dump2 = 1267, dump3 = 1267 — **D7b drži**, ticki
po `rwdiag off` res mirujejo. Isti zamik je delal `ticki so med merjenjem rasli` lažno zeleno.

**Popravek:** nova `Get-MarkerText` vrne log **brez** `[FINE/CustomNPCs]` vrstic; `Wait-ForCount`,
`Read-OkMarker` in `Read-ChunkMarker` štejejo samo konzolni kanal. `Read-Counter` in
`Read-Distribution` berejo tabelo posnetka in ostaneta na celem logu.

**Ugotovitev za naprej:** marker, ki gre v dva kanala, ni marker, ki se ga da šteti. Vsako
merilo oblike "n-ti zadetek" mora povedati, nad katerim kanalom šteje.

**Meritev iz tega zagona** (ni baseline, samo kontekst): 1227 server tickov v 61,3 s, 8 NPC-jev,
`npc.per.tick` p50 = 8, `npc.tick.gap` max = 1, MSPT p95 = 1,475 ms od 50 ms proračuna (2,9 %).

**Spremembe obnašanja:** v modu nobene. Popravljena sta `rwdiag-run.ps1` in `fixture-run.ps1`.

**Naslednja seja:** tretji zagon `matrika-run.ps1` (pričakovano zeleno do vključno M2.2), nato
**M2.3** ali **M2.7**.

---

### 2026-09-17 (25) — Prvi zagon M0.7: tri napake, vse v mojih skriptah

**Paket:** M0.7 (popravki po prvem realnem zagonu)
**Stanje:** uporabnik je pognal `.\fixture-run.ps1` in `.\matrika-run.ps1`; oba sta padla,
vzroki so najdeni in popravljeni. Ponoven zagon je na uporabniku.

**Kaj je padlo in zakaj:**

1. **`fixture-run.ps1` — `script errored`, noben `TW-FIX-*` marker**
   (`audit/m07-fixture-b.log:170`). Vzrok ni bil mod in ne API, ampak **dve vzporedni
   minifikaciji skripte**: `vstavi-skripto.py` iz M2.2 (briše `//` kjerkoli in izid preveri)
   in moja lastna PowerShell različica v `fixture-run.ps1` (brisala je samo cele
   komentarjske vrstice). Komentar na koncu vrstice `q.setType(5); // 5 = manual…` je po
   strnitvi v eno vrstico zakomentiral **preostanek skripte** → Nashorn sintaktična napaka.
   Stack trace gre v NPC-jev `console` v NBT, v log pride samo `script errored`, zato je
   izgledalo kot napaka moda.
   **Popravek:** PowerShell minifikacija je odstranjena. Skripto vloži izključno
   `vstavi-skripto.py`, in to **ob commitu**; runner samo preveri, da je vložena, in fixture
   prekopira v svet — enak vzorec kot `r1-run.ps1`. Vložena skripta je sintaktično preverjena
   (`new Function(code)` v node) in v repozitoriju.
2. **`verify-testworld.ps1` — W1 lažno negativen.** Merilo je zahtevalo **točno 8** datotek v
   `clones/1`, mapa pa je skupna in ima zdaj 13 (8 M0.6 + 4 R1 + TW_Control). W2–W8 so bile
   zelene, `TW-OK` 8× v B in C — svet je bil torej v redu, padlo je merilo.
   **Popravek:** W1 zdaj preverja, da je prisotnih vseh **8 pričakovanih** fixturov, tuje pa
   samo prešteje in poimenuje.
3. **`matrika-run.ps1` — poročilo brez števcev** (`Izid | zeleno, rdece, 2 preskoceno`).
   `.Count` na praznem rezultatu `Where-Object` v PS 5.1 ni `0`, ampak nič, in `-f` izpiše
   prazen niz. **Popravek:** `@(...)` okoli vseh treh. Poleg tega je `Tee-Object` pisal
   UTF-16 (log neberljiv v grepu in git diffu) in `Out-File -Encoding UTF8` je dodal BOM v
   naslov poročila — oboje zdaj piše UTF-8 brez BOM.

**Ugotovitev, ki velja širše:** dve implementaciji istega koraka sta v tem projektu že drugič
tiha napaka (prvič ročno lepljenje ukazov v konzolo, M0.6). Pravilo: en korak, eno orodje,
in orodje samo preveri svoj izid.

**Kaj je zagon vseeno dokazal:** `verify-package` zeleno; testni svet zeleno v W2–W8 po
restartu; `matrika-run.ps1` se je ustavil ob prvi rdeči stopnji, kot je zamišljeno, in
napisal `audit/m07-matrika-2026-09-17-1026.md` s hashem jarja in commitom.

**Spremembe obnašanja:** v modu nobene. Popravljeni so trije razvojni skripti.

**Meritve:** nobene.

**Naslednja seja:** rezultat ponovnega zagona; nato **M2.3** ali **M2.7**.

---

### 2026-09-17 (24) — Ovrednotenje navigacije in umestitev v plan (D-012)

**Paket:** načrtovanje
**Stanje:** končano — odločitev zapisana, paketi umeščeni

**Vprašanje uporabnika:** kako izvedljivo je napisati lasten `navigateTo`, ki bi bil bistveno
boljši od obstoječega.

**Ugotovitev, ki odloči:** od štirih simptomov navigacije, izmerjenih 17. 9., **nobeden ni
A\* algoritem**.

| Simptom | Dejanski vzrok | Sloj |
|---|---|---|
| nosilec z jahačem nima poti (0/8) | predpogoj `PathNavigateGround.canNavigate()` | vanilla predpogoj |
| obstanek pri 22,71 bloka | 200 vozlišč + `NpcNavRange` = 32, in en sam klic | konfiguracija in način uporabe |
| `navig` niha 0–4/8 med napadom | mutex biti `EntityAIAttackTarget:38` — *24. 9. ovrženo (D-020); verjetno osvežitev poti vsakih 4–10 tickov* | **CustomNPCs AI** |
| zgoščevanje na kup | `minRange` vezan na `npc.width` (`:98`) | **CustomNPCs AI** |

Prepis A\* bi tri od štirih pustil nedotaknjene. Dva sta že pokrita z M3.6 in M3.7.

**Kje je vanilla res slab** (vse preverjeno v dekompiliranem Minecraftu v projektu):
proračun 200 vozlišč vrne delno pot (`PathFinder:65`); manhattanska cena **in** hevristika pri
8-smernem gibanju (`PathPoint:86`) sistematično odrivata diagonale; domet vezan na
`FOLLOW_RANGE`; nič deljenja dela med NPC-ji, vsak z novim `ChunkCache` čez ~6×6 chunkov
(`PathNavigate:126-129`); `checkForStuck` po 100 tickih tiho počisti pot.

To so **namerne varovalke za MSPT**, ne napake. Pri 27 dejavnih NPC-jih je p99 že 115 ms od
50 ms proračuna, zato bi večji proračun vozlišč brez predpomnjenja šel naravnost v lag.

**Odločitev D-012:** vanilla pathfindinga ne prepisujemo. Popravki gredo po stopnjah, vsak
pod stikalom in z A/B meritvijo; brez meritve se kandidat zavrže.

**Dodano v plan:**

| ID | Paket | Zakaj |
|---|---|---|
| **M2.7** | merila kakovosti navigacije — šest veličin na originalu | brez njih je „boljše" nemerljivo; vhodni pogoj za ves sklop |
| **M4.10** | nadaljevanje delne poti | poceni, odpravi najbolj viden simptom (navidezne ovire) |
| **M4.11** | lasten `NodeProcessor` z realno ceno diagonale | odpravi cikcakanje; omejen obseg, podprta razširitvena točka |
| **M4.12** | lasten `PathNavigate` — **samo če** M4.10, M4.11 in M5.6 ne zadostujejo | drago; brez dokaza se ne začne |
| **M4.13** | mehkejše sledenje poti — samo če je trzanje merljivo | kozmetika, dokler ni številke |
| **M5.6** | deljenje in predpomnjenje poti, **povišano** iz „šele če" v redni paket | največji pričakovani dobitek v celem sklopu; 8 NPC-jev proti istemu cilju = 8 iskanj |

M4 se preimenuje v „Gibanje: navigacija po tleh in letenje"; prej je pokrival samo letenje.

**Zavrnjeno za zdaj:** asinhrono iskanje poti, flow fieldi za skupinsko gibanje, lasten
gibalni sklad. Async je edini del sklopa z resnično visokim tveganjem (svet se med iskanjem
spreminja), druga dva spremenita občutek gibanja vseh obstoječih NPC-jev. Odprejo se šele, če
po M4.10, M4.11 in M5.6 merila M2.7 še vedno padajo.

**Spremembe obnašanja:** nobene — to je načrtovalna seja.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 ali M2.4 po obstoječem vrstnem redu; M2.7 pred začetkom M4.

---

### 2026-09-17 (23) — Q11 zaprt: meja iskanja poti, ne ovira

**Paket:** M2.2 (popravek merilne naprave)
**Stanje:** končano — popravek narejen in preverjen; ponovitev meritve ni pognana

**Vprašanje:** zakaj se je kontrolna proga v fazi A ustavila pri 22,71 bloka, tik pred drugo
stopnico, čeprav je prvo stopnico in vrata prešla.

**Odgovor: ovira ni bila kriva.** Vanilla A* ima dve trdi meji, obe preverjeni v
dekompiliranem Minecraftu v projektu:

| Meja | Kje | Posledica |
|---|---|---|
| proračun **200 vozlišč** | `PathFinder.findPath:65` | vrne **delno pot** do najbližjega doseženega vozlišča, ne `null` |
| dolžina poti `< maxDistance` | `PathFinder.findPath:94` | `maxDistance` = `getPathSearchRange()` = `FOLLOW_RANGE` |
| `FOLLOW_RANGE = NpcNavRange` | `EntityNPCInterface.java:334`; `dev/run/config/CustomNpcs.cfg` → `NpcNavRange=32` | pot, daljša od 32, ni mogoča v enem klicu |

Proga je dolga **natanko 32 blokov**, z obvozom do vrat pa več. En sam `navigateTo` zato ne
more vrniti cele poti in NPC obstane na koncu delne. Skripta je `navigateTo` klicala enkrat
na začetku faze A; `EntityAIAttackTarget` v fazi B se prepathga sam — **zato je proga S v
fazi B cilj dosegla, v fazi A pa ne.** Vse tri opazke se ujamejo z isto razlago.

**Na ugotovitev o R1 to ne vpliva.** Delna pot bi bila še vedno pot: `isNavigating()` bi bil
`true` in `prevozeno` > 0. Proga M je imela 0/8 poti in 0,06 bloka. Meja pojasni samo
stransko opažanje o kontroli, ne razlike M/S.

**Narejeno:**

- `r1-control.js` v fazi A `navigateTo` ponovi ob vsakem vzorcu; vzorec se vzame **pred**
  osvežitvijo, da `navig=` pove stanje ob koncu intervala, ne takoj po svežem klicu.
- Nastal je `dev/testworld/vstavi-skripto.py`: vstavi vir v polje `Script` clone JSON in
  **sam preveri**, da sta vir in vstavljena različica po normalizaciji identična. Doslej je
  bila to ročna operacija, ki jo je scenarij sam označil za pot do tihe napake.
- Preverjeno: `R1_Control.json` se od prejšnjega commita razlikuje **samo** v vrstici
  `Script`; izluščena minificirana skripta gre skozi `node --check`.

**Ni narejeno in zakaj:** ponovitev `.\r1-run.ps1` s popravkom ni pognana — potrebuje Windows.
Ni nujna za izid M2.2; dala bi čistejšo kontrolo (proga S bi v fazi A prišla do cilja).

**Spremembe obnašanja:** nobene v modu.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 (reprodukcija R2) z upoštevanim pravilom o dometu poti.

---

### 2026-09-17 (22) — M0.7: prehod 1 in fixture kot dve skripti

**Paket:** M0.7 (avtomatizacija)
**Stanje:** koda napisana in pregledana proti izvorni kodi; **ni bila pognana** — seja nima
PowerShella, Gradla ne Minecrafta. Prvi zagon je na uporabniku.

**Narejeno:**

- **`matrika-run.ps1`** — prehod 1 matrike v enem zagonu. Orkestrator: pogane `dev.ps1 build`,
  `verify-package.ps1`, `testworld-run.ps1`, `rwdiag-run.ps1` in `r1-run.ps1`, ujame izhodne kode
  in napiše `audit\m07-matrika-<datum>.md` s SHA-256 jarja, `git` commitom, trajanji in seznamom
  vrstic matrike, ki jih prehod 1 ne pokriva. Privzeto se ustavi ob prvi padli stopnji
  (`-ContinueOnFail` to izklopi), zna `-SkipBuild` in `-Only <stopnja>`.
- **`fixture-run.ps1` + `dev/testworld/tw-fixture.js`** — quest in dialog fixture **brez GUI-ja**.
  Ključna ugotovitev: `noppes quest/dialog` ne zna `create`, zato je bila IC1/IC2 blokada videti
  trdna — a scripting API ima `IQuestCategory.create()` in `IDialogCategory.create()`
  (`api/handler/data/IQuestCategory.java:14`, `IDialogCategory.java:14`), `Quest.save()` pa gre
  skozi `QuestController.saveQuest` → `NBTJsonUtil.SaveFile`, torej **isto save pot kot GUI**.
  Formata ne interpretiramo; ustvari ga mod.
- Druga ugotovitev: quest kategorija nastane iz **imena mape** (`QuestController.java:94`), zato
  je edina stvar, ki jo pripravimo mi, prazna mapa `quests\TW` — kategorije brez tega ni, ker
  `QuestController.load()` na svežem svetu ne naredi privzete (dialogi jo dobijo,
  `loadDefaultDialogs`).
- Trije zagoni po vzorcu M0.5: A pribije world spawn, B ustvari `TW_Quest` (tip 5 manual, nagrada
  1× `minecraft:stone`) in `TW_Dialog`, poveže dialog s questom, ga pripne na `T_Trader`
  (`ICustomNpc.setDialog`) in NPC-ja shrani z `noppes clone add`, C po restartu isti objekt
  **najde** namesto ustvari (`nov=0`) — round-trip dokaz. Merila F1–F16.
- `tw-fixture.js` je vir resnice; `TW_Control.json` ima oznako `@@SCRIPT@@`, ki jo skripta ob
  zagonu zamenja s strnjeno vsebino `.js`. Enaka disciplina kot pri `r1-control.js`, le da je
  vlaganje zdaj skriptirano in ne ročno.
- Dokumentacija: §3 in §4 matrike prepisana, `dev/testworld/README.md` dopolnjen.

**Ni narejeno in zakaj:**

- Quest tipa `item` ostane GUI delo: `QuestItem.items` ni v API-ju, zato ga skripta ne nastavi.
  Fixture je tipa `manual`; za IC2 to zadošča, za item objective ne.
- Klikanje dialoga in dejanski prevzem questa zahtevata prijavljenega igralca → prehod 2.
- Nobena skripta ni bila pognana. `noppes clone add` je bil preverjen v izvorni kodi
  (`CmdClone.java:50-71` — primerja **ime**, ne selektorja, in išče 80 blokov okoli pošiljatelja,
  kar za konzolo pri `0,0,0` in `T_Trader` pri `8,4,8` drži), ne pa v svetu.

**Spremembe obnašanja:** v modu nobene. Nova sta dva razvojna skripta in en fixture krmilnik;
`verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** rezultat prvega zagona `matrika-run.ps1` in `fixture-run.ps1` zapisati v ta
dnevnik, popraviti, kar pade, nato **M2.3** ali **M2.4**.

---

### 2026-09-17 (21) — M0 zaključen: integracijska matrika (M0.7) in M0.8 kot blokada

**Paket:** M0.7 (+ formalni zaključek M0.8)
**Stanje:** končano — merila X1–X5 izpolnjena

**Narejeno:**

- Nov dokument [`scenariji/M0.7-integracijska-matrika.md`](scenariji/M0.7-integracijska-matrika.md):
  funkcionalna matrika iz `PLAN_IMPLEMENTACIJE.md` §6 je prepisana v **47 oštevilčenih preverb**
  (IN NPC, IA AI, IC vsebina, IS skripte, IL lifecycle, IK klient, IO omrežje). Vsaka vrstica ima
  tip (**A** avtomatizirana / **R** ročna), imenovan dokaz (marker v logu, datoteka, NBT diff) in
  pošteno stanje pokritosti.
- Postopek izvedbe v treh prehodih: avtomatski server tek iz obstoječih skript
  (`verify-package.ps1`, `testworld-run.ps1`, `rwdiag-run.ps1`, `r1-run.ps1`), ročni klientski
  prehod in zapis izida v `audit\m07-matrika-<datum>.md`. Pravilo: **matrika ni nikoli zelena po
  opustitvi** — neizvedena vrstica ni uspeh.
- Zapisan enkratni postopek za quest/dialog fixture v GUI-ju (§4), ki odblokira IC1 in IC2.
- **M0.8 zaprt kot zabeležena blokada.** Uporabnik je potrdil, da nima dostopa do dejanskega
  modpacka, Forge builda, configa ne kopije pravega sveta in da nima nobenega pokvarjenega clone
  zapisa. Vpliv je razčlenjen po milestonih v §6 matrike; izhodni kriterij M0 to izrecno dovoljuje.
- `docs/03-FAZE.md`: M0 označen kot zaključen z datumom in sklicem na izpolnjene izhodne kriterije.

**Trenutna pokritost matrike:** 6 vrstic pokrito (IN1, IN4, IA7, IS1, IL1, IL7), 4 delno (IN2, IN6,
IA3, IL6), 3 blokirano (IC1, IC2 na GUI fixture; IO4 na M0.8), 34 ni pokrito. To je izhodišče,
ne pomanjkljivost — matriko polnijo milestoni M1–M10.

**Ni narejeno in zakaj:**

- Matrika ni bila **pognana** — seja nima PowerShella, Gradla ne Minecrafta (glej Znane omejitve).
  Prvi realni prehod 1 naj se izvede ob zaključku naslednjega paketa.
- Quest in dialog fixture ostajata odprta; brez GUI-ja ju ni mogoče narediti, ugibanje NBT
  strukture pa protokol prepoveduje.
- Nobena vrstica ni bila avtomatizirana na novo; M0.7 je dokument, ne koda. Premik vrstic iz **R**
  v **A** je delo pripadajočih milestonov.

**Spremembe obnašanja:** nobene. Koda ni bila spremenjena, `verify-package.ps1` ostane pri istem
seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** **M2.3** (reprodukcija R2 — leteči NPC čez steno na x=20) ali **M2.4**
(merilni scenariji 50/200/500 NPC-jev). Pred predajo naslednjega paketa se prvič požene prehod 1
integracijske matrike.

---

### 2026-09-17 (20) — M2.2 zaključen: R1 je reproduciran in izmerjen

**Paket:** M2.2
**Stanje:** končano — E1–E6 zelena

**Izid v enem stavku:** nosilec z jahačem **nikoli ne dobi navigacijske poti**
(`isNavigating()` je `false` v 40 od 40 vzorcev), medtem ko je identičen nosilec brez jahača
v istem svetu in istem ticku na poti in prevozi 22,7 bloka.

Polni zapis: [`meritve/2026-09-17-M2.2-R1-reprodukcija.md`](meritve/2026-09-17-M2.2-R1-reprodukcija.md).

| Faza | Veličina | **M (jahači)** | **S (kontrola)** |
|---|---|---|---|
| A `navigateTo` | nosilcev s potjo | **0/8 ves čas** | 8/8 |
| A | prevoženo povp. / max | **0,06 / 0,06** | 19,25 / 22,71 |
| A | do cilja min | 31,44 (start 31,9) | 8,83 |
| B `setAttackTarget` | nosilcev s potjo | **0/8 ves čas** | 0–4/8 |
| B | prevoženo povp. / max | 8,26 / 9,17 | 19,62 / **32,70** |
| B | razpon skupine | 13,45 → **7,00** | 5,04 → **33,53** |
| B | do cilja min | 24,31 | **0,51** (cilj) |
| obe | jahačev na nosilcu | **8/8 ves čas** | — |
| obe | odstopanje jahač↔nosilec | **0,00 ves čas** | — |

**Kar spremeni zahtevo R1:**

- **Odprto vprašanje 2 v §R1 je odgovorjeno: nosilec poti sploh ne dobi.** Zahteva je
  predpostavljala, da jo dobi in jo nekaj kvari.
- **Hipoteza `EntityAIFollow.tpTo` se tu ne uresniči.** `odstopMax = 0,00` v vseh 40 vzorcih;
  jahač ostane točno na nosilcu. `EntityAIFollow` zahteva lastnika, fixture ga nima. R1 se
  reproducira **brez** te poti.
- **Vez jahač–nosilec je stabilna** (`jahacev=8` ves čas, tudi čez teleport med fazama).
  Pokvarjena ni vez, pokvarjena je navigacija nosilca.
- **„Ne znajo čez bloke“ je izmerjeno.** V fazi B se nosilci z jahači ustavijo pri z ≈ 19,7 in
  tam obstanejo 240 tickov. Stopnica je pri z = 20.
- **Zgoščevanje obstaja, a ni „radius enega bloka“**: razpon pade na 7,00, kontrola gre v
  nasprotno smer na 33,53. Za radius enega bloka §R1 imenuje poseben pogoj
  (`hasHitbox == false` → `minRange ≈ 0`); ti fixture hitbox imajo, zato ta pogoj **ni bil
  preizkušen**. Ločen scenarij.

**Vodilna hipoteza mehanizma — še ni dokazana.** V fazi B se nosilci premikajo, pa `isNavigating()`
ostane `false`; premikajo se torej mimo navigatorja. `PathNavigateGround.canNavigate()`
(`:32-35`) zahteva `onGround || isInLiquid || isRiding`, pri čemer `isRiding()` velja za
**jahača**, ne za nosilca. Preverjena in ovržena sta bila `addInteract()` (kliče ga samo
`interact` in `EntityAIWander`, ta pa se pri `MovingState 0` ne doda) in `EntityAIReturn`
(`ReturnToStart: 0b`). Test, ki hipotezo odloči: števec `onGround` / `canNavigate()` /
`noPath()` na nosilec na tick → **M2.1b oziroma M3.1**. Do takrat je to hipoteza.

**Stranski ugotovitvi:**

- Tudi kontrolna proga v fazi A ne pride do cilja: obstane pri 22,71 bloka, tik pred **drugo**
  stopnico (z = 36), `navig` pade z 8/8 na 4/8. Prvo stopnico in vrata je prešla (razpon se
  pri vratih stisne na 2,87 — lijak). Zakaj druga stopnica ustavi skupino, prva pa ne, je novo
  odprto vprašanje. Na razliko M/S ne vpliva, ker velja za obe progi.
- `navig` niha (0–4/8) tudi na kontrolni progi v fazi B, kar se ujema z dokazano napako mutex
  bitov v `EntityAIAttackTarget:38`. To je neodvisno od jahanja. *(24. 9.: mutex biti niso napaka, D-020.)*

**Meritve:** 27 NPC-jev, 945 tickov: `npc.update.window` 207,3 µs/NPC, `server.tick.ns`
p50 = 1,93 ms, p95 = 10,75 ms, p99 = 115,3 ms. **Ni baseline** in se ne sme primerjati z M2.1d
(66,6 µs pri 8 mirujočih NPC-jih) — tu jih 16 pathfinda čez oviro.

**Spremembe obnašanja:** nobene.

**Naslednja seja:** M2.3 (reprodukcija R2) ali M2.4 (50/200/500). Vzrok R1 se dokaže šele z
instrumentacijo iz M2.1b/M3.1 in ne sme se ga razglasiti prej.

---

### 2026-09-17 (19) — M2.2: preverba artefaktov in dve blokirni napaki pred zagonom

**Paket:** M2.2 (preverba)
**Stanje:** delno — vse, kar se da preveriti brez sveta, je preverjeno; zagon ostaja

**Kontekst:** `git pull` je pokazal, da je oddaljena veja enaka lokalni in da je vse delo
seje 18 (fixture, `r1-control.js`, `r1-setup-commands.txt`, `r1-run.ps1`, scenarij)
necommitano. Seja 18 je naštela tri stvari, ki jih ni mogla preveriti. Ta seja jih je.

**Zaprte vrzeli iz seje 18:**

| Vrzel iz seje 18 | Kako je zaprta | Izid |
|---|---|---|
| `r1-run.ps1` ni sintaktično preverjen | PowerShell 7.4.6 v oblačnem okolju, `Parser::ParseFile` | 2165 žetonov, **0 napak** |
| minificirana skripta v `R1_Control.json` morda ni enaka viru | normalizacija obeh (brez komentarjev, poenoteni narekovaji, stisnjeni presledki) in primerjava znak za znak | **identični** |
| fixture morda vsebujejo več kot naštete spremembe | `diff` vsakega `R1_*` proti osnovi `T_*` | **natanko naštete spremembe, nič drugega** |

Dodatno preverjeno: vsi klici API-ja v skripti obstajajo v `reference-src` s pravimi
podpisi, in `IWorld.getAllEntities(int)` vrne **`IEntity[]`**, ne `List` — pri `List` bi
`all.length` v Nashornu tiho vrnil `undefined` in `R1-MOUNT` bi javil `n=0`.

**Napaka 1 — nosilci ne morejo skozi edina vrata v zidu.** `T_Carrier` in s tem
`R1_Carrier` imata `DoorInteract: 2` (onemogočeno). Tedaj
`EntityNPCInterface.doorInteractType()` (`:867-881`) ne doda vratnega AI taska in nastavi
`setBreakDoors(false)`, kar je `canOpenDoors = false` (`PathNavigateGround.java:317-320`).
V `WalkNodeProcessor.getPathNodeType` (`:359-362`) zaprta lesena vrata postanejo `WALKABLE`
samo ob `canOpenDoors && canEnterDoors`; sicer ostanejo `DOOR_WOOD_CLOSED` s ceno `-1.0F`,
enako kot `BLOCKED` (`PathNodeType.java:20`).

Vrata so edini prehod skozi tri bloke visok zid pri z = 28, torej pot do cilja za nosilce
**sploh ne bi obstajala** — na obeh progah enako. Pohod bi se ustavil 16 blokov od starta,
merilo E2 (kontrolna proga prevozi vsaj 20 blokov) bi padlo in izid bi izgledal kot okvara
navigacije. **Popravek:** `R1_Carrier` dobi `DoorInteract: 1`. Jahači, cilja in krmilnik
ostanejo pri `2`, ker ne navigirajo.

**Napaka 2 — merilni kanal bi lahko molčal.** Ves izpis gre skozi
`npc.executeCommand('/say …')`. `NPCWrapper.executeCommand` (`:201-206`) vrže
`CustomNPCsException`, če command bloki niso vklopljeni, `NoppesUtilServer.runCommand`
(`:226-229`) pa v istem primeru samo zapiše opozorilo. Pri `enable-command-block=false` v
logu ne bi bilo **nobene** vrstice `R1-*` in vse preverbe bi padle hkrati — videti kot
pokvarjena skripta, v resnici nastavitev serverja. V `dev/run/server.properties` je
trenutno `true` (iz M0.6), a nikjer ni bilo zapisano kot pogoj. `r1-run.ps1` to odslej
preveri v koraku 1 in po potrebi popravi.

**Tretja ugotovitev, ki ne blokira, a spreminja branje izida.** `isWalking()`
(`EntityNPCInterface.java:1417`) je `movingType != 0 || isAttacking() || isFollower() ||
Walking` in nadzoruje edino `addVelocity` (`:1020`). Navigacije ne blokira, zato
`MovingState: 0` skriptnemu `navigateTo` ne škodi. Pomeni pa, da v fazi A nosilci **ne
dobivajo sunkov ob medsebojnih trkih**, v fazi B pa jih, ker je `isAttacking()` takrat
resničen. Veličina `razpon` zato **ni primerljiva med fazo A in fazo B**. Primerljiva je
samo med progama M in S znotraj iste faze — kar je edina primerjava, ki jo scenarij trdi,
tako da merila ostanejo veljavna.

**Preverjeno tudi, da ne bo presenečenja:** `EntityAIReturn` se v `addRegularEntries()`
(`:900`) doda **brezpogojno**, a `shouldExecute()` (`:40`) se konča takoj ob
`!ais.shouldReturnHome()`. `ReturnToStart: 0b` iz seje 18 torej res izklopi vlečenje domov.

**Ni narejeno in zakaj:**

- **Scenarij še ni pognan.** Lupina te seje na uporabnikovem računalniku je linuxova in
  nima PowerShella, Gradla ne Minecrafta. Zagon je na uporabniku; dogovorjeno 17. 9.
- Krmilna skripta še vedno ni tekla v Nashornu. Preverjeni so sintaksa, podpisi API-ja in
  vračilni tipi, ne pa izvedba.

**Spremembe obnašanja:** v modu nobene. Spremenjena sta testni fixture in zagonska skripta.

**Meritve:** nobene.

**Naslednja seja:** uporabnik požene `.\r1-run.ps1` iz korena projekta; seja prebere
`audit\m22-r1.log`, ovrednoti E1–E6 in zapiše razliko M/S v `docs/meritve/`. Če E1 pade
(mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v zahtevo. Če pade E2, je
pokvarjena kontrola, ne mod.

---

### 2026-09-15 (18) — M2.2: raziskava in scenarij reprodukcije R1

**Paket:** M2.2 (priprava)
**Stanje:** delno — scenarij je zasnovan in zapisan, fixture in skripta še nista narejena

**Narejeno — raziskava, ki odpre M2.2 brez prenosa kode:**

| Vprašanje | Odgovor | Vir |
|---|---|---|
| Kako posaditi NPC na NPC brez GUI-ja? | `IEntity.setMount(IEntity)` iz skripte | `api/wrapper/EntityWrapper.java:414-420` |
| Gre to skozi vanilla `startRiding`? | **da**, `startRiding(entity, true)` | isto, `:418` |
| Ali gre `ItemMounter` po isti poti? | **ne**, paket `EnumPacketServer.SpawnRider` | `items/ItemMounter.java:31` |
| Ali nosilec sploh dobi pot? | `isNavigating()`, `getNavigationPath()` | `api/wrapper/EntityLivingWrapper.java:43-58` |
| Kako pognati skupino proti cilju? | `navigateTo(x,y,z,speed)`, `setAttackTarget(living)` | isto, `:32`, `:65` |
| Kako najti fixture NPC-je? | `world.getAllEntities(2)` + `hasTag(...)` | `WorldWrapper.java:279-297` |

- **Cela reprodukcija se da napisati kot skripta na enem krmilnem NPC-ju.** Nič ni treba
  prenesti v `src/patch/java`. To je pomembno: M2 ne sme začeti prenašati
  `EntityNPCInterface`, ker je to M3.1. S tem odpade zadnji razlog za prepletanje M2 in M3.
- S tem je **odgovorjeno tudi na odprto vprašanje 4 iz `02-ZAHTEVE.md` §R1** (ali mount
  sploh gre skozi `startRiding`) — za skriptno pot da. Za pot prek `ItemMounter` ne;
  popravek v M3 mora pokriti obe in reprodukcija prek skripte o drugi ne pove nič.
- Scenarij zapisan v [`docs/scenariji/M2.2-R1.md`](scenariji/M2.2-R1.md): prizorišče,
  potek v dveh fazah, šest merjenih veličin in merila E1–E6.

**Ugotovitev, ki spreminja zahtevo:** zahteva predpisuje „8 jahačev na 8 nosilcih“, **brez
kontrolne skupine**. Tako postavljen poskus ne dokaže ničesar — če se nosilci slabo
premikajo, je lahko vzrok jahanje ali pa se skupina osmih CustomNPC-jev tako premika tudi
brez njih. Scenarij zato vodi **dve vzporedni progi hkrati**, v istem svetu in istem ticku:
proga M (8 nosilcev z jahači) in proga S (8 istih nosilcev brez jahačev). Vsaka številka se
poroča kot par M/S; razlika je ugotovitev, absolutna vrednost ni.

Druga sprememba: scenarij ima dve fazi, ne eno. Faza A je prosta pot (`navigateTo`) — čista
navigacija; faza B je `setAttackTarget`, torej uporabnikov dejanski primer („ko grejo
napadat“) in edina, ki sproži `EntityAIAttackTarget` s sumljivimi mutex biti in `minRange`,
odvisnim od širine.

**Narejeno — gradnja (ista seja, po zasnovi):**

- `dev/testworld/r1-control.js` — **vir** krmilne skripte, berljiv in komentiran. V
  `R1_Control.json` je vstavljena minificirana (polje `Script` je en sam niz). Skripta
  najde NPC-je po tagih, pari jahače z nosilci po koordinati x, vodi obe fazi in izpisuje
  vzorce z markerjem `R1-S`.
- Štirje fixture, **generirani iz obstoječih pravih NBT zapisov** (pravilo iz
  `dev/testworld/README.md`), s točno naštetimi spremembami in nič drugim:
  `R1_Carrier` (iz `T_Carrier`), `R1_Rider` (`T_Rider`), `R1_Target` (`T_Stand`),
  `R1_Control` (`T_Scripted`).
- `dev/testworld/r1-setup-commands.txt` — prizorišče in 26 NPC-jev.
- `r1-run.ps1` — zagon in samodejno ovrednotenje E1–E6, vključno z izpisom razlike M/S.

**Ugotovitve iz gradnje — vse preverjene v kodi, nobena po spominu:**

- **Hrastova vrata se v 1.12.2 registrirajo kot `minecraft:wooden_door`, ne `oak_door`**
  (`net/minecraft/init/Blocks.java:392`). Edina vrsta vrat s starim imenom; ugibanje bi
  tiho spodletelo in zid bi ostal brez prehoda.
- Spodnja polovica vrat meta `3` = `FACING NORTH`: `BlockDoor.getStateFromMeta` (`:423`)
  bere `EnumFacing.getHorizontal(meta & 3).rotateYCCW()`.
- **`ReturnToStart` je v osnovnih fixture `1b`.** To požene `EntityAIReturn` in vleče NPC
  nazaj domov; pri 32 blokov dolgi progi bi sámo po sebi ustavilo pohod in bi izgledalo kot
  okvara jahanja. V vseh štirih R1 fixture je `0b`.
- `setAttackTarget(null)` je varen (`EntityLivingWrapper:65-71`, `EntityLivingBaseWrapper:69-75`).
- **Vrstni red zagona ni poljuben.** Krmilna skripta začne šteti ob spawnu, zato se
  `R1_Control` spawna **šele po** `rwdiag chunks on` in ogrevanju. Sicer bi se scenarij po
  300 tickih ustavil sredi faze A in bi izgledal kot okvara AI — natanko past M2.1d.
- Ena sama datoteka `R1_Carrier` streže obema progama; skripta ju loči po `x < 10`. Tako
  sta nosilca na obeh progah zagotovo identična, kar je pri kontrolni skupini bistvo.

**Ni narejeno in zakaj:**

- Scenarij ni bil pognan. `r1-run.ps1` ni sintaktično preverjen (v seji ni PowerShella) in
  krmilna skripta ni tekla v Nashornu — preverjeno je le, da se izvorna in minificirana
  različica razčlenita (`node --check`) in da se vsi klici API-ja ujemajo s podpisi v
  `reference-src`.
- Merilo E6 (zapis razlike M/S v `docs/meritve/`) nastane šele po zagonu.

**Spremembe obnašanja:** v modu nobene. Vse novo je v testnem svetu in v skriptah.

**Meritve:** nobene — reprodukcija še ni tekla.

**Naslednja seja:** pognati `.\r1-run.ps1`, ovrednotiti E1–E6 in zapisati razliko M/S v
`docs/meritve/`. Če E1 pade (mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v
zahtevo. Če E2 pade, je pokvarjena kontrola in scenarij, ne mod.

---

### 2026-09-15 (17) — M2.1d: pogoj meritve (prisilno naloženi chunki)

**Paket:** M2.1 (del d)
**Stanje:** končano — C1–C6 zelena v svetu isti dan ob 14:07

**Narejeno:**

- `rework/diag/DiagChunkPlan.java` — **čista** logika načrta: pretvorba blok→chunk (z
  `floor >> 4`, ne deljenjem, ki pri negativnih koordinatah pokrije napačen chunk),
  odstranjevanje podvojenih chunkov, razširjanje po obročih in proračun. Brez Minecraft
  tipov, zato je edini del paketa, ki se da enotsko testirati.
- `rework/diag/DiagChunkLoader.java` — lastni `ForgeChunkManager` ticketi tipa `NORMAL`,
  prisilno nalaganje chunkov vseh naloženih NPC-jev, osvežitev enkrat na sekundo prek
  `WorldTickEvent` (ta prinese svet s sabo, zato razred ne potrebuje poti do
  `MinecraftServer` izven ukaza), sprostitev ob `chunks off` in ob `FMLServerStoppedEvent`.
- `CommandRwDiag`: nov podukaz `/rwdiag chunks <on [obroč]|off|status>` z markerjem
  `RWDIAG-CHUNKS`. **Namenoma ni del `rwdiag on`** — meritev brez prisilno naloženih
  chunkov je veljavna le z igralcem v svetu, in ta razlika mora biti v scenariju vidna.
- `DiagKeys`: `world.chunks.forced` (vzorči se vsako sekundo skozi celo meritev) in
  `diag.chunks.added`. Prvi **zapiše pogoj meritve v sam posnetek**, da meritve brez pogoja
  ni več mogoče pomotoma brati kot veljavne.
- `rwdiag-run.ps1`: `chunks on` pred `rwdiag on`, nova merila **C1–C6**, stikali
  `-ChunkRadius` in `-NoChunks`. **C3 in C4 sta tisto, kar 15. 9. ni bilo preverjeno in
  zato ni bilo opaženo:** vsi števci so rasli, samo NPC-ji niso tikali. Skripta zdaj bere
  `npc.per.tick` in `npc.tick.gap` iz tabele posnetka in pade, če NPC-ji nehajo tikati.
- 10 novih JUnit testov (`DiagChunkPlanTest`); v seji je bilo zelenih vseh 33 testov
  instrumentacije, prevedenih z `javac --release 8`.
- Odločitev **D-011** v `01-ARHITEKTURA.md`; scenarij `docs/scenariji/M2.1-diag.md`
  dopolnjen z razdelkom M2.1d in merili C1–C7.

**Ugotovitve:**

- **Mod je že registriran chunkloader.** `CustomNpcs.load` kliče
  `ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkController())`. To je
  bilo bistveno: `ForgeChunkManager.requestTicket` **vrže `RuntimeException`**, če mod
  nima registriranega `LoadingCallback`. Nova koda zato callbacka ne dodaja in ne spreminja.
- Proračun: 25 chunkov na ticket, 200 ticketov (`dev\run\config\forgeChunkLoading.cfg`),
  torej do 5000 chunkov. Omejitev v kodi je 400 chunkov na svet (16 ticketov).
- **Dve napaki v originalnem `ChunkController`** (najdeni med branjem, nista popravljeni —
  protokol prepoveduje popravljati mimogrede):
  1. `getTicket` (`:40-56`) ob uspešnem ustvarjanju ticketa vrne `null` namesto ticketa.
     Klicatelj torej misli, da je ustvarjanje spodletelo, čeprav je ticket shranjen.
  2. `ticketsLoaded` (`:70`) preverja `tickets.contains(npc)`, kjer je `tickets` seznam
     ticketov, `npc` pa entiteta — pogoj je vedno `false` in je mrtev.
  Nista razporejeni v milestone; spadata k opravilu „Chunk Loader“ (job 8), ki v M2 ni
  predmet dela. To je tudi razlog, da si merilni pogoj ne izposodi tega razreda.

**Preverjeno isti dan ob 14:07 (uporabnik je pognal, seja je preverila izpise in posnetke):**

- **Deluje.** `npc.update` = **9832** (prej 1968), kar je 8,007 na tick; `npc.update` / 8 =
  1229 = število tickov. NPC-ji so tikali **v vsakem ticku celotne meritve**.
  `npc.tick.gap` ima 1227 vzorcev in vsi so 1 — brez ene same luknje. `npc.per.tick`
  p50 = 8 (prej 0).
- **C1–C6 zelena**: `chunki=9 tiketi=1 obroc=1 zavrnjeni=0 npc=8`; po `chunks off`
  `chunki=0 tiketi=0`; `TW-SCRIPT-OK`, `TW-SCRIPT-TICK-10`, 0 `ERROR` iz `noppes.*`,
  `BUILD SUCCESSFUL`.
- Polni zapis: [`docs/meritve/2026-09-15-M2.1d-prva-veljavna.md`](meritve/2026-09-15-M2.1d-prva-veljavna.md).

**Napaka v merilu, ki jo je ta zagon razkril:** C5 je bilo napisano kot `min = max` in bi
**zavrglo prav to, prvo veljavno meritev projekta**. Nabor je zrasel z 9 na 12 chunkov, ker
so trije NPC-ji zatavali iz pokritega območja in jih je osvežitev pokrila
(`diag.chunks.added = 3`) — to je delo zbiralnika, ne napaka. Pravo merilo je `min > 0`:
padec na 0 pomeni, da pogoj ni držal, rast ne pomeni ničesar slabega. Popravljeno v
`rwdiag-run.ps1` in v scenariju. Prestrogo merilo, ki zavrže veljavno meritev, je enako
škodljivo kot ohlapno, ki spusti neveljavno.

**Ni narejeno in zakaj:**

- `rwdiag-run.ps1` ni bil sintaktično preverjen v seji (ni PowerShella); preverila ga je
  šele uporabnikova izvedba, ki je tekla do konca.
- Prepad p95/p99 ni razčiščen (glej spodaj); to je M2.5, ne ta paket.

**Spremembe obnašanja:** nov podukaz `/rwdiag chunks` (raven dovoljenja 2). Dokler se ne
pokliče, ta koda ne pokliče ničesar iz `ForgeChunkManager` in ni prijavljena na event bus.
Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** prva veljavna meritev projekta. p50 = 0,557 ms, p95 = 1,638 ms od 50 ms
proračuna pri 8 NPC-jih. **p99 = 81,789 ms in max = 269,019 ms se ne smeta navajati**,
dokler prepad ni pojasnjen.

**Prepad med p95 in p99 — postavljen in isti dan zaprt.** Prva meritev je imela p95 = 1,6 ms
in p99 = **81,8 ms**. Kandidat 1 (priklop ticketa sproži nalaganje chunka, to pa je delo na
server niti) je bil potrjen s ponovitvijo ob 14:19 z **10 s ogrevanja** med `chunks on` in
`rwdiag on` — edina sprememba:

| | brez ogrevanja (14:07) | z ogrevanjem (14:19) | |
|---|---|---|---|
| `server.tick.ns` p50 | 0,557 ms | **0,410 ms** | −26 % |
| `server.tick.ns` p95 | 1,638 ms | **1,016 ms** | −38 % |
| `server.tick.ns` **p99** | **81,789 ms** | **4,325 ms** | **faktor 19** |
| `server.tick.ns` max | 269,019 ms | 86,863 ms | autosave, en tick |
| `npc.update.window` | 182,5 µs / NPC | **66,6 µs / NPC** | −64 % |

Kandidata 2 (autosave) in 3 (delo NPC-jev) sta s tem izključena kot razlaga prepada.
Presenečenje ob strani: napihnjen je bil tudi `npc.update.window` — ker se meri kot razmik
do naslednje entitete, je tick z nalaganjem chunka ta razmik raztegnil. Vsaka meritev brez
ogrevanja torej precenjuje tudi porabo na NPC. **Ogrevanje je odslej privzeto** in je pogoj
vsake meritve, enako kot prisilno naloženi chunki.

**Merodajni rezultat pri 8 NPC-jih, brez igralca, 61,3 s:** p50 = 0,410 ms in p95 = 1,016 ms
od 50 ms proračuna; NPC-ji tikajo v vsakem ticku; zgornja meja porabe na NPC je 66,6 µs.
Še vedno **ni baseline** — 8 NPC-jev je preverba naprave, ne scenarij.

**Naslednja seja:** **M2.2** — reprodukcija R1 (8 jahačev na 8 nosilcih, oba CustomNPC).

---

### 2026-09-15 (16) — M2.1c zaključen: zakaj NPC-ji nehajo tikati

**Paket:** M2.1 (del c)
**Stanje:** končano — vzrok je imenovan z datoteko in vrstico, ne z domnevo

**Narejeno:**

- Uporabnik je pognal `.\rwdiag-run.ps1` s števcem `npc.update.blocked` (tretja meritev,
  12:47). Izid: **`npc.update.blocked` = 0 in `entity.update.blocked` = 0** v 61 sekundah.
- Preden je ničla sprejeta kot podatek, je bilo preverjeno, da števec ni tiho mrtev —
  in to na **jarju, ki ga je server res naložil** (`customnpcs-dev-runtime.jar`, žig
  12:46:54; gradle je izvedel `:compileJava`, `:devRuntimeMod`, `:jar`, nobenega kot
  `UP-TO-DATE`). `DiagKeys.class` vsebuje oba ključa, `DiagEventCollector.class` ima
  `onCanUpdate` z `@SubscribeEvent` in parametrom `EntityEvent$CanUpdate`.
- **Mehanizem 1 izključen.** `World.updateEntityWithOptionalForce` (`World.java:2139-2155`)
  doseže `ForgeEventFactory.canEntityUpdate` samo, kadar preverba območja ±32 blokov pade.
  Nobenega dogodka ⇒ preverba ni padla niti enkrat.
- **Mehanizem 2 imenovan.** `WorldServer.updateEntities()` (`WorldServer.java:628-644`):

  ```java
  if (this.playerEntities.isEmpty() && getPersistentChunks().isEmpty()) {
      if (this.updateEntityTick++ >= 300) { return; }
  } else { this.resetUpdateEntityTick(); }
  ```

  Vanilla neha posodabljati entitete **300 tickov (15 s)** po nalaganju sveta, če ni
  igralca in ni prisilno naloženega chunka. Zanka do entitet sploh ne pride.
- Aritmetika se ujema pri vseh treh zagonih (247, 246 in 237 tickov z NPC-ji, glede na
  zamik med `Done` in `rwdiag on`; ločljivost žigov v logu je 1 s = ±20 tickov).
- Razrešeno tudi navidezno protislovje "`world.tick` šteje, NPC-ji pa ne tikajo":
  `MinecraftServer.updateTimeLightAndEntities` kliče `worldserver.tick()` (`:831`) in
  `worldserver.updateEntities()` (`:842`) ločeno, `onPostWorldTick` pa je za obema (`:851`).
- Polni zapis, tabele in številke:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Ugotovitve:**

- **Vse tri dosedanje meritve so neveljavne kot izhodišče.** Za ~80 % svojega trajanja so
  merile prazen tek, zato se `server.tick.ns` percentili ne smejo navajati kot baseline.
  Prva uporabna številka pride šele po M2.1d.
- Ista vrstica, ki je vzrok, je tudi popravek. Dovolj je, da **eno** od dvojega ne drži.
  `ForgeChunkManager` ticket je strožje jamstvo kot igralec: odklene zanko
  `updateEntities()` (pogoj na ravni sveta) **in** postavi `range = 0` v preverbi ±32
  blokov (pogoj na ravni chunka), in ne potrebuje človeka pred zaslonom. Scenarij mora
  pokriti chunke vseh merjenih NPC-jev, ne le enega.
- Najdaljši tick je bil 123,9 ms (prej 74,5 in 58,1) in še vedno sovpada z autosave. Merilo
  za M2.5 ostaja nespremenjeno: autosave tick izločiti ali poročati posebej.

**Ni narejeno in zakaj:**

- M2.1d ni začet. Po protokolu je to svoj paket s svojim commitom; poleg tega bo takoj po
  njem treba ponoviti meritev, kar spet zahteva uporabnika.
- Commit ni narejen v seji — seja ne more poganjati `git`. Vsebina commita: ta dnevnik,
  dopolnjena meritev in obstoječa (že napisana) koda M2.1c, če še ni commitana.

**Spremembe obnašanja:** nobene. Za M2.1c ni bila spremenjena nobena vrstica kode; ta seja
je samo pognala, preverila in razložila.

**Meritve:** tretja meritev M2.1, `audit\m21-rwdiag.log`, posnetki
`dev\run\logs\rwdiag\rwdiag-20260915-1248*`. Zeleno: `TW-SCRIPT-OK`, 0 `ERROR` vrstic iz
`noppes.*`, 0 `script errored`, `BUILD SUCCESSFUL`, števci po `rwdiag off` mirujejo
(1268/1968 → 1268/1968).

**Naslednja seja:** **M2.1d** — `ForgeChunkManager` ticket za chunke z merjenimi NPC-ji,
pod stikalom in privzeto izklopljen (D-007), plus merilo: ista meritev mora pokazati
`npc.per.tick` p50 = 8 čez celotno trajanje in `npc.tick.gap` max = 1 pri ≥ 1200 vzorcih.
Šele nato M2.2 (reprodukcija R1; uporabnik je potrdil, da sta oba NPC-ja CustomNPC).

---

### 2026-09-15 (15) — M2.1a: instrumentacija `rework/diag`

**Paket:** M2.1 (del a)
**Stanje:** koda napisana in enotsko preverjena; gradle, `verify-package.ps1` in zagon v
svetu mora pognati uporabnik (seja še vedno ne more zaganjati ukazov na njegovem računalniku)

**Narejeno:**

- Nov paket `dev/src/patch/java/noppes/npcs/rework/diag/` z devetimi datotekami:
  `Diag` (vstopna točka, vklop/izklop, kljuci, posnetek), `DiagKey` (števec + čas na
  `LongAdder`), `Distribution` (logaritemski koši, p50/p95/p99), `DiagSnapshot`
  (nespremenljiv posnetek, tabela + JSON), `DiagKeys` (imena velicin na enem mestu),
  `DiagDump` (zapis v `logs/rwdiag/`), `DiagEventCollector` (zbiranje na Forge dogodkih),
  `CommandRwDiag` (ukaz `/rwdiag`) in `package-info`.
- **Izklopljena instrumentacija ne stane nič.** Zbiralnik se na Forge event bus prijavi
  šele ob `/rwdiag on` in se ob `off` odjavi, zato ni klica na entiteto na tick. Vsa
  klicna mesta v kodi moda pa se najprej vprašajo za eno `volatile` polje.
- 23 novih JUnit testov (`DiagTest`, `DistributionTest`, `DiagSnapshotTest`). Pokrivajo:
  izklopljeno stanje ne šteje nič, vklop počisti prejšnje števce, meritev, ki prečka
  vklop ali izklop, se zavrže, natančnost percentilov, ekstremne vrednosti, točnost
  štetja pod osmimi nitmi hkrati, in to, da se posnetek med izpisom ne premika.
- `CustomNpcs.java`: tri vrstice — registracija ukaza, `-Drwdiag=on` ob zagonu serverja in
  izklop ob `FMLServerStoppedEvent`.
- `dev/build.gradle`: novi testi so izključeni iz `testOriginal`, ker se nanašajo na
  razrede, ki jih v nedotaknjenem originalu ni.
- `docs/scenariji/M2.1-diag.md`: kaj se meri, kako se bere, merila D1–D7 in postopek.

**Ugotovitve:**

- Meritev časa posodobitve NPC-ja je zaenkrat **približek**: `EntityNPCInterface` še ni
  prenesen, Forge pa da le dogodek ob začetku posodobitve entitete. Čas se zato meri kot
  razmik do naslednje žive entitete v istem ticku — to je zgornja meja, ne točna poraba.
  Točna meritev pride z M3.1 in takrat se stolpca primerjata.
- Povprečje je pri performancu neuporabno, zato `Distribution` hrani porazdelitev v
  logaritemskih koših. 32 podkošev na potenco dvojke da ~3,2 % napake navzgor pri 16 KB na
  velicino; prvotnih 8 podkošev (~12 %) je bilo pri MSPT premalo natančnih — p95 in p99
  sta padla v isti koš.
- Ukaz je namenoma `/rwdiag` in ne podukaz `/noppes`: instrumentacija mora delovati tudi,
  če je z mod ukazi kaj narobe, in ne sme spreminjati obstoječega ukaznega drevesa.

**Ni narejeno in zakaj:**

- Števci za izračune poti, klice skript in čas po AI taskih (M2.1b) so **rezervirani z
  imenom, a prazni**. Njihova klicna mesta so v `EntityNPCInterface`, paketu `ai` in
  `ScriptContainer`, ki še niso preneseni v `src/patch/java`. Prenos teh razredov je
  vsebina M3.1 in M5.1; delati ga zdaj bi pomenilo velik prenos brez pripadajočega
  popravka, kar je v nasprotju s pravilom "en paket = en commit = ena stvar".
- Koda ni prevedena z gradle in ni tekla v Minecraftu. Jedro paketa (brez Minecrafta) je
  bilo prevedeno z `javac --release 8` in vseh 23 testov je v seji zelenih, razreda
  `DiagEventCollector` in `CommandRwDiag` pa se opirata na Forge in MC API, zato ju je
  preverila samo primerjava s klici v `reference-src` (`CommandNoppes`, `ScriptPlayerEventHandler`).

**Spremembe obnašanja:** nov ukaz `/rwdiag` (raven dovoljenja 2) in nov, privzeto
izklopljen zbiralnik meritev. Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** nobene — to je paket, ki meritve šele omogoči.

**Preverjeno isti dan ob 10:52 (uporabnik je pognal, seja je preverila izpise in datoteke):**

- **D1 zeleno** — `.\dev.ps1 test`: `DiagTest` 10, `DistributionTest` 8, `DiagSnapshotTest` 5,
  skupaj 23 testov, 0 napak, 0 napak izvajanja (`dev/build/test-results/test/`). Testi zdaj
  tečejo na Javi 8 na uporabnikovem računalniku, ne le na OpenJDK 21 v seji.
- **D2 zeleno** — `testOriginal` je tekel brez treh novih razredov, kot mora.
- **D3 zeleno** — seja je sama primerjala `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`
  z originalom: 25 spremenjenih razredov (vsi pričakovani, isti nabor kot prej + `CustomNpcs`),
  27 dodanih (11 `rework/data` + 11 `rework/diag` + notranji razredi), **0 odstranjenih**.
  `CommandRwDiag` in `DiagEventCollector` sta v jarju, kar pomeni, da se Forge in MC API, ki ju
  seja ni mogla prevesti, dejansko prevedeta — to je bilo edino resno tveganje tega paketa.
- **Commit in push narejena** — `c28b654` "M2.1a: instrumentacija rework/diag + ukaz /rwdiag";
  `origin/main` kaže na isti commit. V indeksu je vseh 9 datotek paketa, 3 testi, scenarij in
  posodobljeni dokumenti.
- **D4–D7 zelena ob 12:17** (`.\rwdiag-run.ps1`, izpis `audit\m21-rwdiag.log`):
  - **D4** — server z novim jarjem se naloži in čisto ustavi, `TW-SCRIPT-OK` je v logu,
    **nobene `ERROR` vrstice iz `noppes.*`**, nobenega `script errored`, `BUILD SUCCESSFUL`.
  - **D5** — `RWDIAG-OK ticki=1226 npc=1896` po 61 s merjenja.
  - **D6** — trije posnetki v `dev\run\logs\rwdiag\`, vsak v `.txt` in `.json`, nobeden prazen.
  - **D7** — pred vklopom `RWDIAG stanje=off ticki=0`; po `rwdiag off` sta dva posnetka v
    razmiku 10 s pokazala **enaka** števca (1246 / 1896), kar dokazuje, da je zbiralnik res
    odjavljen z event busa in ne le tiho.
- Da se ročno tipkanje v gradle konzolo ne ponovi (15. 9. je dvakrat tiho odpovedalo), je
  nastala skripta **`rwdiag-run.ps1`** — isti mehanizem kot `testworld-run.ps1`: požene
  server, pošlje `rwdiag status|on|dump|off`, primerja števce pred in po izklopu, preveri
  zapisane posnetke, ustavi server in izpiše prvo tabelo. Izhodna koda 0 = D4–D7 zelena.
- **Prva številka in prvo presenečenje.** MSPT p50 = 0,16 ms, p95 = 1,21 ms — pri osmih
  NPC-jih je server prazen. Toda `npc.per.tick` ima **p50 = 0** in je binaren (0 ali 8):
  NPC-ji tikajo le v približno vsakem petem ticku, ne v vsakem. To ni napaka števca, ampak
  lastnost sveta brez igralca; razčistiti jo je treba **pred** M2.4, sicer bodo merilni
  scenariji merili nekaj drugega, kot mislimo. Podrobnosti, kandidati in naslednji korak:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).
- Najdaljši tick 74,5 ms sovpada z autosave. M2.5 mora autosave tick izločiti ali poročati
  posebej, sicer bo p99 vedno meril shranjevanje in ne AI.

**M2.1c — instrumentacija za razčiščenje `npc.per.tick` (isti dan, 12:2x):**

Da odgovor ne bo ugibanje, je zbiralnik dobil pet novih velicin, vse še vedno samo na Forge
dogodkih in samo dokler je merjenje vklopljeno:

- `npc.tick.gap` — razmik v tickih med dvema tickoma z NPC-ji. Enakomeren razmik pomeni
  sistematiko, razmetan pomeni chunk loading.
- `world.tick` — server ticki po svetovih, vsota čez dimenzije.
- `world.entities.loaded`, `world.npc.loaded`, `world.npc.killed`, `world.players` —
  vzorčenje `loadedEntityList` in `playerEntities` enkrat na sekundo, sešteto čez vse svetove.

`world.npc.killed` je tam zaradi pasti iz M0.6: ubit CustomNPC ostane v svetu in čaka na
respawn. Če je meritev tekla nad mrtvimi NPC-ji, so številke o nečem drugem, kot mislimo.

Uporabljeni so samo tisti deli MC API-ja, ki so potrjeni v `reference-src`
(`loadedEntityList` in `playerEntities` v `NPCSpawning`, `isKilled()` v
`EntityNPCInterface`); `addedToChunk` in `provider.getDimension()` sta bila namenoma
izpuščena, ker ju v dekompilatu ni bilo mogoče potrditi, prevesti pa jih seja ne more.
Jedro paketa se je v seji znova prevedlo z `javac --release 8` in 23 testov je zelenih.

**Druga meritev (12:32) je vprašanje razčistila do polovice.** Novi števci so izključili dva
kandidata od treh in pokazali nekaj, česar prva meritev ni mogla: `npc.tick.gap` ima 246
vzorcev in **vsi so 1**, skupaj 1976 = 8 × 247. NPC-ji so torej tikali v **247 zaporednih
tickih in nato nikoli več** — ne "vsak peti tick", ampak en strnjen blok na začetku.
`world.npc.loaded` je 62 vzorcev zapored točno 8, `world.npc.killed` 0, `world.tick` 1229
pri 1228 server tickih, `world.players` 0.

**Ugotovitev: približno 14 sekund po nalaganju sveta vanilla neha posodabljati entitete,
ker v svetu ni igralca.** Mod s tem nima nič. To pomeni, da sta obe dosedanji meritvi merili
prvih ~14 s dogajanja in nato prazen tek — in razloži, zakaj je MSPT tako nizek.

Ostaneta dva mehanizma: (1) `updateEntityWithOptionalForce` preskoči posodobitev, ker
območje 32 blokov okoli entitete ni naloženo — takrat Forge posije `EntityEvent.CanUpdate`;
(2) zanka `updateEntities` do entitet sploh ne pride. Zato je dodan števec
**`npc.update.blocked`**, ki šteje `EntityEvent.CanUpdate` za NPC-je in dogodka ne
spreminja. Naslednji zagon loči mehanizma brez ugibanja.

**Posledica za M2.4/M2.6, ki velja ne glede na izid:** vsak merilni scenarij mora
eksplicitno določiti, ali je v svetu igralec, ali pa morajo biti chunki z NPC-ji prisilno
naloženi (`ForgeChunkManager`). Dokler to ni urejeno, nobena številka iz M2 ni primerljiva
s produkcijskim strežnikom. Podrobnosti:
[`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Naslednja seja:** pognati `.\rwdiag-run.ps1` s števcem `npc.update.blocked` in dokončati
odgovor, nato urediti pogoj meritve (igralec ali prisilno naloženi chunki) in šele nato M2.2
(reprodukcija R1). Uporabnik je potrdil, da sta pri R1 **oba NPC-ja CustomNPC** (jahač in
nosilec), zato reprodukcija ne potrebuje vanilla konja.

---

### 2026-09-15 (14) — M0.6 zaključen: skriptiran scenarij testnega sveta

**Paket:** M0.6
**Stanje:** končano

**Narejeno:**

- `testworld-run.ps1` požene cel scenarij brez človeka: trije server zagoni, ukazi na
  standardni vhod, izpis v `audit/m06-testworld-{a,b,c}.log`, na koncu `verify-testworld.ps1`
  nad logoma B in C. Mehanizem je prevzet iz `smoke-server.ps1` (M0.5).
- **Izid: vse preverbe zelene.** Zagon B `32 entities deleted` in `TW-OK` 8×; zagon C po
  restartu `TW-OK` 8×, `TW-SCRIPT-OK` 1×, `TW-SCRIPT-TICK-10` 1×. Nobene `ERROR` vrstice iz
  `noppes.*`, nobenega `script errored`. Scenarij je tekel nad svetom, onesnaženim z 32
  NPC-ji iz neuspelih poskusov, in ga je sam počistil — s tem je dokazana idempotentnost.
- Merila W1–W8 veljajo; M0.6 je izhodni pogoj za M2 in je izpolnjen.

**Ni narejeno in zakaj:**

- Quest in dialog fixture ostajata v M0.7 — obojega ni mogoče sestaviti brez GUI-ja,
  ugibanje NBT strukture pa protokol prepoveduje.
- Igralec se še ni povezal v testni svet; `PlayerData.save` (B1/B2 v realnem obratovanju)
  zato še ni pokrit. Prav tako M0.7.

**Ugotovitve — štiri pasti, vsaka je stala en neuspel zagon:**

1. **Ročno lepljenje ukazov v gradle konzolo je nezanesljivo.** Prvi poskus je izgledal kot
   okvara testnega sveta: `noppes clone list 1` je izpisal vseh 8 imen, a
   `execute @e[tag=testworld]` je javil `found nothing`. V logu ni bilo **nobenega**
   `gamerule`, `fill` ali `clone spawn` — vsebina `setup-commands.txt` ni nikoli prišla do
   serverja. Odslej vsak scenarij pošilja ukaze skriptirano.
2. **`runServer` je lahko `UP-TO-DATE` in se sploh ne zažene.** ForgeGradle prijavi `dev/run`
   kot izhod naloge; če se med dvema zagonoma tam nič ne spremeni, Gradle nalogo preskoči,
   build se konča v 20 s in skripta čaka na marker do timeouta. Popravljeno v
   `dev/build.gradle` z `outputs.upToDateWhen { false }` za `runClient` in `runServer`.
   Velja tudi za `smoke-server.ps1`.
3. **`/kill` CustomNPC-ja ne odstrani, ampak ga respawna.** `DataStats` ima privzeto
   `RespawnTime = 20` in `SpawnCycle = 0` (`:30-31`); ubit NPC se shrani kot mrtev in ob
   nalaganju sveta oživi. Po dveh poskusih je bilo v svetu 24 in nato 32 NPC-jev, čeprav je
   `kill` vsakič javil, da jih je pobil. Pravi ukaz je `noppes slay npcs` →
   `entity.isDead = true` (`CmdSlay.java:140`). **Pomembno za M2**, kjer je število NPC-jev
   merjena količina.
4. **Zapis datoteke na uporabnikov disk je lahko tiho neuspešen.** Dvakrat je orodje javilo
   uspešen zapis, na disku pa je ostala prejšnja verzija (`setup-commands.txt` 1321 B namesto
   1654 B, `testworld-run.ps1` 9828 B namesto 10400 B), zato je zagon tekel s staro logiko in
   izgledal kot nova napaka v modu. Odslej seja po vsakem zapisu preveri velikost datoteke,
   preden zahteva zagon.

Poleg tega: `fill` javi `No blocks filled`, kadar so bloki že na mestu — izpis zgleda kot
odpoved, pa ni. In `EntityNPCInterface` kliče `EventHooks.onNPCTick` vsak 10. tick (`:358`),
torej je script tick 2 Hz na NPC; vsi ti klici gredo skozi en statičen `lock` v
`ScriptContainer.run`, kar je vhodni podatek za M5.

**Spremembe obnašanja:** v modu nobene. `dev/build.gradle` je dobil
`outputs.upToDateWhen { false }` za `runClient` in `runServer`; to je razvojno orodje, ne
del zapakiranega moda, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** M0.7 — integracijska matrika. Prvi korak zahteva uporabnika: v GUI-ju
narediti en quest in en dialog, pripeti dialog na fixture NPC-ja, nato se
`world/customnpcs/quests/` in `dialogs/` posname v `dev/testworld/`.

---

### 2026-09-15 (13) — M0.6: seme testnega sveta in samodejno ovrednotenje

**Paket:** M0.6
**Stanje:** delno — vse datoteke so pripravljene in preverjene proti izvorni kodi;
scenarij še ni bil pognan, ker seja ni mogla zagnati ukazov na uporabnikovem računalniku.

**Narejeno:**

- Pregled semena, ki je nastalo ob koncu prejšnje seje (`dev/testworld/`, `testworld.ps1`,
  `docs/scenariji/M0.6-testni-svet.md`). Prejšnja seja jih ni ne commitala ne zabeležila
  v ta dnevnik; to je bila odprta luknja in je zdaj zaprta.
- **Verifikacija semena proti izvorni kodi** (ne proti spominu):
  - `CmdClone.spawn` ima podpis `<name> <tab> [[world:]x,y,z] [newname]` —
    `setup-commands.txt` ga uporablja pravilno (`CmdClone.java:112`).
  - `CmdClone.grid` ima `<name> <tab> <lenght> <width> [[world:]x,y,z]` — kot je navedeno
    v scenariju za M2.4 (`CmdClone.java:173`).
  - `DataScript.readFromNBT` bere `Scripts`, `ScriptLanguage` in `ScriptEnabled` iz
    **korenskega** compounda entitete; `T_Scripted.json` ima vse tri na pravem mestu.
  - `ScriptContainer.readFromNBT` bere `Script`, `Console`, `ScriptList` — struktura v
    fixture datoteki se ujema.
  - Vseh 8 fixture datotek se ujema s tabelo v `dev/testworld/README.md`
    (`MovementType`, `MovingState`, `Role`, `Invulnerable`, `PersistenceRequired`, tagi).
- **Popravljeno merilo W5.** Prvotno merilo je bilo "`T_Scripted` ne vrže script napake",
  kar je lažno pozitivno, če scripting sploh ne teče. `DataScript.isEnabled()` zahteva
  `ScriptEnabled` **in** `ScriptController.HasStart` **in** `CustomNpcs.EnableScripting`
  (`CustomNpcs.java:271` postavi `HasStart=false` ob load, `:293` na `true` šele ob
  `FMLServerStartedEvent`). Merilo zdaj zahteva pozitiven izpis iz same skripte.
- Skripta v `T_Scripted` zato ob `init` požene `/say TW-SCRIPT-OK` in ob 200. `tick`
  `/say TW-SCRIPT-TICK-200`. Pot je `NPCWrapper.executeCommand` →
  `NoppesUtilServer.runCommand`, ki zahteva vklopljene command bloke; seme jih ima
  (`enable-command-block=true`). Izhod `/say` pride v konzolo tudi brez igralca.
- Nov `verify-testworld.ps1` samodejno ovrednoti W1–W8 iz `dev/run/logs/latest.log` in
  datotečnega sistema ter vrne izhodno kodo 0/1. S tem scenarij ni več odvisen od ročnega
  branja loga in ga lahko katerakoli seja ponovi enako.
- Scenarij in `dev/testworld/README.md` posodobljena; postopek ima zdaj eksplicitno fazo
  restarta za W6.

**Ni narejeno in zakaj:**

- Scenarij ni pognan iz seje. Windows posodobitev z 8. 9. je onemogočila, da bi seja
  dosegla uporabnikov datotečni sistem prek lupine; seja lahko datoteke bere in piše, ne
  pa izvajati gradle, server ali git. Zagon je naredil uporabnik.
- Commit in push je naredil uporabnik. Push je bil najprej zavrnjen: na `origin/main` sta
  bila commita `M0.2r` in `M0.5` z **druge delovne postaje**, ki ju ta postaja ni imela.
  Delo je bilo združeno brez force-pusha; oba M0.5 zapisa (ročni z 11. 9. in skriptirani
  s 14. 9.) sta ohranjena, dnevnik pa preštevilčen, ker sta obe veji uporabili številko (10).
- W6 in W7 sta preverljiva šele po dejanskem zagonu; do takrat M0.6 ostane *delno*.

**Ugotovitve:**

- `ScriptContainer.run` sinhronizira na **statičnem** `lock` objektu, torej si vse skripte
  vseh NPC-jev delijo eno ključavnico. To je konkreten dokaz za kandidata iz M5
  (odstranitev globalnega script locka) in za R7; zabeleženo, ne popravljeno.
- `ScriptContainer` ob napaki pokliče `NoppesUtilServer.NotifyOPs(... " script errored")`,
  kar je iskalni niz za negativno stran W5.
- Prejšnja seja je fixture NPC-je generirala iz pravega NPC zapisa (`ModRev 18`, polna
  lista atributov z `generic.flyingSpeed`), ne iz ugibanja. Pravilo iz
  `dev/testworld/README.md` je torej spoštovano; edina ročna sprememba v tej seji je
  vsebina niza `Script`, kar ni sprememba strukture NBT.

**Spremembe obnašanja:** nobene — spremenjeno je le testno seme in dokumentacija; v
`src/patch/java` ni sprememb, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene

**Odprto:** projekt zdaj teče na **dveh delovnih postajah**. Vsaka seja mora pred delom
pognati `git fetch origin` in preveriti, ali je `origin/main` pred njo; sicer se dnevnik
razide, kot se je tokrat.

**Naslednja seja:** zapisati rezultat W1–W8 iz uporabnikovega zagona; če je PASS, zapreti
M0.6 in odpreti M0.7 (integracijska matrika + quest/dialog fixture iz GUI-ja).

---

### 2026-09-14 (12) — M0.5: dedicated-server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- `dev/build.gradle` poveže `runServer.standardInput = System.in`. Gradlov `JavaExec`
  privzeto poda prazen vhod, zato je bila konzola dev serverja doslej mrtva in serverja
  ni bilo mogoče niti čisto ustaviti z `stop`. Deluje samo z `--no-daemon`.
- Dodan `smoke-server.ps1`: skriptiran, ponovljiv smoke test s 13 preverbami. Svet
  `m05-smoke` se vsakič zbriše in ustvari na novo (FLAT, seed 20260914, brez spawna mobov,
  `max-tick-time=-1`), ukazi gredo na standardni vhod, izpisi v `audit\m05-server-*.log`.
- Postopek zapisan kot ponovljiv scenarij v `docs/scenariji/M0.5-server-smoke.md`.
- Izid: vseh 13 preverb zeleno. Server naloži 5 modov, `Done (1.337s)`, NPC se ustvari,
  preživi `save-all flush` in restart, po restartu konzola izpiše `[SmokeNPC] M05-NPC-PRESENT`.
- Minecraft EULA je bila sprejeta na izrecno zahtevo uporabnika; skripta je zapiše samo
  z zastavico `-AcceptEula`.

**Ni narejeno in zakaj:**

- Igralec v svetu ni bil uporabljen; NPC je ustvarjen z `summon` in NBT, ne prek GUI-ja.
  Preverjen je en NPC in en NBT ključ, ne celotno stanje NPC-ja. To je obseg M0.7.
- M0.6 (testni svet z znanimi NPC-ji, questi, dialogi in skriptami) ostaja ločen paket;
  M0.5 mu je pripravil determinističen svet in spawn.

**Ugotovitve:**

- `CommandSummon` v 1.12.2 javi `Cannot summon the object out of the world` tudi takrat,
  ko **ciljni chunk ni naložen**, ne samo pri neveljavnih koordinatah. Spawn superflat
  sveta lahko pade do 256 blokov od izhodišča, zato chunk `0,0` ni nujno med spawn chunki.
  Zato ima test tri zagone: prvi pribije world spawn na `0 5 0`.
- `execute @e[…] ~ ~ ~ say` je boljša preverba kot `testfor`, ker konzola izpiše ime NPC-ja
  v oklepaju. En marker dokaže troje: entiteta obstaja, je pravega tipa in NBT ključ `Name`
  je preživel round-trip skozi popravljeno save pot.
- `save-all` v 1.12.2 izpiše `Saved the world`, ne `Saved the game`. Prvi osnutek skripte je
  čakal na napačen niz in 180 s lovil timeout.

**Spremembe obnašanja:** nobene v modu. Dev server zdaj bere konzolo; to je razvojno orodje,
ne del zapakiranega moda.

**Meritve:** nobene

**Naslednja seja:** M0.6 — testni svet z znanimi NPC-ji, questi, dialogi in skriptami, na
podlagi determinističnega sveta iz M0.5.

---

### 2026-09-14 (11) — M0.2r: obnova razvojnega okolja na novi delovni postaji

**Paket:** M0.2r (neplaniran, blokiral je vse ostalo)
**Stanje:** končano

**Narejeno:**

- Projekt je bil prestavljen na drug računalnik. `dev/libs`, `dev/baseline` in `.tools` so v
  `.gitignore`, zato na novi postavitvi **ni bilo mapiranega originala** in nič se ni prevajalo.
- `dev/build.gradle` je dobil task `exportMappedOriginal` (aktiven samo z `-PremapOriginal`),
  ki regenerira `dev/libs/customnpcs-mapped-01Oct19.jar` iz SHA-256-preverjenega originala po
  isti ForgeGradle deobf poti kot prvotna postavitev.
- `obnovi-okolje.ps1` preveri JDK 8 in hashe, nato požene `setupDecompWorkspace`, remap,
  `testOriginal` + `test`, `buildPatchedMod` in `verify-package.ps1`. Gradle koraki najprej
  poskusijo `--offline` in ob neuspehu ponovijo z omrežjem.
- Rezultat: `testOriginal` 31, `test` 47, build zapakira 42 razredov, `verify-package.ps1`
  pregleda 1716 vnosov in najde 25 pričakovanih zamenjav — **enako kot pred selitvijo**.

**Ni narejeno in zakaj:**

- `downloads\forge-1.12.2-14.23.5.2847-mdk.zip` na tej postaji ne obstaja in ni bil obnovljen;
  Gradle cache ima vse potrebno, zato paket ni blokiran. Vnos v locku ostane kot zapis izvora.

**Ugotovitve:**

- Tri napake v prvem osnutku remap taska, vse potrjene z izvedbo, ne z branjem:
  1. `deobfCompile files(...)` ForgeGradle 2.3 zavrne — zahteva `ExternalModuleDependency`.
  2. `flatDir` ignorira group in odgovori tudi na ForgeGradlovo lastno poizvedbo
     `deobf.<group>:<ime>:<verzija>`, zato vrne **neremapiran original** namesto deobf
     rezultata in remap se sploh ne zgodi. Rešitev je `ivy` repozitorij z `[organisation]`
     v vzorcu.
  3. Remap sproži šele `deobfCompileDummyTask`; brez te odvisnosti task ne naredi ničesar.
- `$ErrorActionPreference = 'Stop'` v PowerShellu spremeni **vsako vrstico stderr** zunanjega
  programa v terminating error. Že `java -version` je prekinil skripto. Native klici zato
  tečejo z `Continue`, uspeh pa se presoja po `$LASTEXITCODE`.
- Regeneriran jar **ni bitno enak** hashu v `environment-lock.json`, vsebinsko pa je enak:
  1716 vnosov se ujema po imenu, vseh **815 resourcev je bitno enakih**, remapiranih je 571
  razredov. Deobf izhod ForgeGradla torej ni bitno ponovljiv med postavitvami. Lock je
  posodobljen; razlog je zapisan kot odločitev **D-010**.

**Spremembe obnašanja:** nobene. Normalen build je nespremenjen; remap koda se naloži samo
z `-PremapOriginal`.

**Meritve:** nobene

**Naslednja seja:** M0.5.

---

### 2026-09-11 (10) — M0.5: dedicated server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- Scenarij je zapisan kot ponovljiv postopek v [`scenariji/M0.5-server-smoke.md`](scenariji/M0.5-server-smoke.md)
  z devetimi merili sprejemljivosti, tako da ga lahko katerakoli kasnejša seja ponovi.
- Dedicated server se zažene iz popravljenega drevesa: Forge 14.23.5.2847, Java 1.8.0_492,
  5 modov naloženih, `Done (12.031s)` ob prvem in `Done (2.077s)` ob ponovnem zagonu.
- NPC `SmokeM05` (`customnpcs:CustomNpc`) spawnan na `(0,72,0)`, dodan v clone tab 1,
  nato `save-all flush` in `stop`.
- Po restartu: `M05-SURVIVED` 1× in `M05-TAG-OK` 1× — entiteta **in** vanilla `Tags`
  so preživeli serializacijo. `noppes clone list 1` še vedno vsebuje `SmokeM05`,
  `noppes clone spawn` uspe in `M05-AFTER-CLONE-SPAWN` se pojavi 2×.
- `world/customnpcs/clones/1/SmokeM05.json` (7399 B) je na disku brez ostankov `.tmp` ali
  `.bak`; vsebina ima pričakovane ključe `Name`, `Tags`, `Invulnerable`, `NoGravity`.
  To je prva potrditev popravka M1.5a na dejanskem strežniku, ne samo v testu.
- Oba `stop`-a sta čista: `Saving players` → `Saving worlds` → `Unloading dimension 0`
  brez izjeme; drain world seje iz M1.6 ni javil nečistega zaključka.
- V logu ni nobene napake iz `noppes.*`. Edina `ERROR` vnosa sta okoljska in prisotna tudi
  pri originalu (maven library folder, manjkajoči FML podpisi).

**Ni narejeno in zakaj:**

- Pot ob prijavi igralca (`PlayerData.save`, torej B1 in B2 v realnem obratovanju) ni pokrita,
  ker se noben klient ni povezal. Spada v M0.6/M0.7, kjer bo testni svet imel tudi igralca.
- Git commit te seje ni bil narejen iz seje; razlog je nova omejitev orodij spodaj.

**Ugotovitve:**

- Ukazi iz server konzole se izvajajo na `(0,0,0)`, ne na world spawnu `(132,64,-32)`,
  `/noppes clone add` pa išče NPC-je v radiju 80 blokov od pošiljatelja. Vsak kasnejši
  konzolni scenarij mora entitete postaviti blizu izhodišča ali uporabiti klienta.
- V shranjenem clone JSON je `"id": "customnpcs:customnpc"` z malimi črkami, registry ime
  entitete pa je `customnpcs:CustomNpc`. Za `/summon` in `@e[type=...]` je obvezna oblika
  z velikimi črkami. Zaenkrat neškodljivo, a kandidat za preverbo pri M8.
- Clone datoteke niso strog JSON (`1b`, `1.0f`). To je originalni format, ki ga `NbtJson`
  namerno ohranja; strog JSON parser jih ne prebere in to ni napaka.

**Spremembe obnašanja:** nobene

**Meritve:** nobene; zagon 12,0 s oziroma 2,1 s je informativen podatek, ne meritev po protokolu.

**Naslednja seja:** M0.6 — ponovljiv testni svet z znanimi NPC-ji, dialogi in skriptami.

---

### 2026-09-11 (9) — M1.9: fault injection varnih zapisov

**Paket:** M1.9
**Stanje:** končano

**Narejeno:**

- `SafeFileWriter` je dobil package-private datotečno mejo za deterministično fault injection;
  javni API in produkcijska pot še vedno uporabljata neposredno `java.nio.file.Files` implementacijo.
- Dodanih je 9 scenarijev: prekinitev med pisanjem pred zamenjavo, zaklenjen/zavrnjen cilj,
  zavrnjen dostop do imenika, nepodprt atomski premik, odpoved fallback namestitve, prekinitev
  po fallback zamenjavi, odpoved same obnove, pokvarjen JSON kandidat in restart z orphaned `.bak`.
- Pri vsaki odpovedi test zahteva, da ostane zadnja verzija na cilju ali kot eksplicitno
  obnovljiv `.bak`; začasne `.tmp` datoteke se ne puščajo.
- Normalna atomska zamenjava je ponovno uspešno tekla na dejanskem Windows datotečnem sistemu;
  redke OS veje uporabljajo deterministično vržene standardne Java NIO izjeme.
- Vseh 31 primerjalnih testov ter 16 testov writerjev, session lifecyclea in fault injection
  je zelenih v Javi 8.
- Build zapakira 42 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- M1.4 in M1.8 ostajata zavestno odložena brez konkretne poškodovane uporabnikove datoteke.
- M1.7 ni potreben, ker se save format ni spremenil.

**Ugotovitve:**

- Če odpovesta tako namestitev kot obnova cilja, `SafeFileWriter` pusti staro vsebino v `.bak`
  in pripne napako obnove kot suppressed exception; podatki so še vedno obnovljivi.
- Orphaned backup se ne prepiše samodejno. Writer zavrne nov zapis, da ohrani zadnjo znano
  obnovljivo verzijo za pregled namesto ugibanja.

**Spremembe obnašanja:** nobene; dodana testna meja ni del javnega API-ja.

**Meritve:** nobene

**Naslednja seja:** M0.5 dedicated-server smoke, nato M0.6 testni svet kot vhod za M2.

---

### 2026-09-11 (8) — M1.6: lifecycle asinhronih world zapisov

**Paket:** M1.6
**Stanje:** končano

**Narejeno:**

- `CustomNpcs` je bil prenesen v prevedljivo drevo in shranjen v ločenem baseline commitu;
  popravljeni so bili samo trije generični artefakti dekompilacije.
- Dodan `rework/data/WorldSaveSession`: ločen enonitni executor za vsako server-world sejo,
  z absolutnim rootom sveta, zajetim ob `FMLServerAboutToStartEvent`.
- `PlayerData.save` ob zahtevi zajame NBT snapshot in ime cilja ter ga preda aktivni seji;
  ne izračunava več poti znotraj pozno izvedene naloge na globalnem schedulerju.
- Ob `FMLServerStoppedEvent` seja najprej preneha sprejemati zapise in izprazni vrsto, šele
  nato se `CustomNpcs.Server` nastavi na `null`. Nečist drain je eksplicitno zabeležen kot napaka.
- Če save izjemoma nastane brez aktivne server seje, se pot zajame in zapis izvede sinhrono;
  podatki se ne odložijo na neupravljano globalno vrsto.
- Testi pokrijejo svet A → drain → svet B z istim imenom igralca, zavrnitev poznega zapisa ter
  propagacijo I/O napake v rezultat draina. Primerjalni bytecode test potrdi staro in novo vezavo.
- Vseh 31 primerjalnih testov ter 7 testov varnih writerjev/session je zelenih v Javi 8.
- Build zapakira 40 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Zaklenjen cilj, zavrnjen dostop in procesne prekinitve spadajo v M1.9 fault injection.
- Splošni `CustomNPCsScheduler` ni ugasnjen, ker ga uporabljajo klient, paketni sendi, GUI zamiki
  in `BankData`; slednji je ločen bug B4 in se ne sme spreminjati v tem paketu.

**Ugotovitve:**

- Edini pregledani datotečni zapis na globalnem schedulerju je bil `PlayerData.save`; drugi
  uporabniki izvajajo omrežne ali GUI naloge, zato bi globalni shutdown povzročil regresije.

**Spremembe obnašanja:** pending player zapisi se ob ustavitvi izpraznijo pred resetom serverja;
zapis starega sveta ne more uporabiti poti novega sveta.

**Meritve:** nobene

**Naslednja seja:** M1.9, fault injection in zaključni kriteriji integritete podatkov.

---

### 2026-09-11 (7) — M1.5d: varen zapis stisnjenega NBT

**Paket:** M1.5 (četrti del)
**Stanje:** končano

**Narejeno:**

- `BankController`, `FactionController`, `GlobalDataController`, `TransportController`,
  `RecipeController`, `SpawnController`, klientov `PresetController` in `SchematicController`
  so bili vsak preneseni v prevedljivo drevo z ločenim baseline commitom.
- Dodan `rework/data/CompressedNbtFile`, ki stisnjeni NBT zapiše skozi `SafeFileWriter`, ga
  ponovno prebere in zahteva enak NBT pred atomsko zamenjavo cilja.
- Vseh osem controllerjev zdaj zapisuje neposredno na končno pot; odstranjena so zaporedja
  `.dat_new` → brisanje/preimenovanje in neposredno prepisovanje schematic datoteke.
- Regresijski test je pred popravkom dokazano padel, po popravku pa bytecode vseh controllerjev
  potrdi odsotnost nevarnih suffixov oziroma uporabo novega varnega writerja.
- Vseh 29 primerjalnih testov ter 5 testov varnih writerjev je zelenih v Javi 8.
- Build zapakira 32 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 20 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Lifecycle asinhronih player zapisov je ločen problem B2 in ostaja M1.6.
- Širši fault-injection scenariji (zaklenjen cilj, zavrnjen dostop, prekinitev procesa) so M1.9.

**Ugotovitve:**

- `CompressedStreamTools.writeCompressed` zapre podani stream. `CompressedNbtFile` zato uporabi
  non-closing ovoj, da lahko `SafeFileWriter` po koncu stiskanja še izvede `flush` in disk `sync`.
- Prvotni seznam šestih world controllerjev ni bil popoln; isti problem sta imela tudi klientov
  preset zapis in neposreden schematic zapis, zato sta vključena v isti paket.

**Spremembe obnašanja:** banke, factioni, globalni podatki, transporti, recepti, spawni,
klientovi preseti in schematiki ob neuspelem zapisu ohranijo zadnjo veljavno datoteko.

**Meritve:** nobene

**Naslednja seja:** M1.6, upravljan lifecycle asinhronih zapisov.

---

### 2026-09-11 (6) — M1.5c: varen zapis sinhronih JSON datotek

**Paket:** M1.5 (tretji del)
**Stanje:** končano

**Narejeno:**

- `DialogController`, `QuestController`, `LinkedNpcController` in `RoleTrader` so bili vsak
  preneseni v prevedljivo drevo in shranjeni v ločenem baseline commitu pred spremembo vedenja.
- Pri `RoleTrader` so bili odstranjeni samo dekompilatorski casti `Object`, ki se z generičnim
  `ItemStack` seznamom niso prevedli; runtime vedenje baselinea se s tem ne spremeni.
- Vsi štirje zapisi zdaj podajo končno `.json` pot neposredno v `NBTJsonUtil.SaveFile`.
  Odstranjeno je zunanje zaporedje `_new` → izbriši cilj → nepreverjen `renameTo`.
- Novi regresijski test pregleda bytecode konstantne nize: proti originalu potrdi stari nevarni
  suffix, proti popravljeni kodi pa zahteva njegovo odsotnost. Pred popravkom je test dokazano padel.
- Vseh 27 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 20 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 12 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Banke, factioni, globalni podatki, transporti, recepti in spawni uporabljajo stisnjeni NBT;
  spadajo v M1.5d in zahtevajo svoj validator za binarni format.
- Lifecycle asinhronih player zapisov ostaja M1.6.

**Ugotovitve:**

- `ScriptController` že zapisuje neposredno na končne poti, zato je postal varen z globalnim
  popravkom `NBTJsonUtil.SaveFile` iz M1.5a in ne potrebuje lastnega prenosa v tem paketu.

**Spremembe obnašanja:** dialogi, questi, linked NPC podatki in trader marketi ohranijo zadnjo
veljavno JSON datoteko, če nov zapis ali njegova validacija odpove.

**Meritve:** nobene

**Naslednja seja:** M1.5d, varen zapis controllerjev s stisnjenim NBT.

---

### 2026-09-11 (5) — M1.5b: varen zapis PlayerData

**Paket:** M1.5 (drugi del)
**Stanje:** končano

**Narejeno:**

- `PlayerData` prenesen v prevedljivo drevo brez funkcionalnih sprememb in shranjen v ločenem
  baseline commitu.
- Dodan karakterizacijski test privzetega `PlayerData` NBT snapshota; zelen je proti originalu
  in obnovljenemu razredu.
- Asinhroni zapis igralca ne uporablja več `_new` → izbriši cilj → nepreverjen `renameTo`,
  ampak ciljno JSON datoteko preda `NBTJsonUtil.SaveFile` oziroma `SafeFileWriter`.
- Vseh 22 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 15 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 7 dovoljenih zamenjav.

**Ni narejeno in zakaj:**

- Zajem poti/sveta in zaključevanje async vrste ostajata nespremenjena; to je M1.6, ne B1.
- Drugi JSON in stisnjeni NBT controllerji ostajajo M1.5c/d.

**Ugotovitve:**

- `PlayerData` povezuje B1 in B2, zato sta bila atomski zapis in async lifecycle namenoma
  ločena v zaporedna paketa.

**Spremembe obnašanja:** veljaven player JSON se ne izbriše pred uspešnim zapisom in validacijo novega.

**Meritve:** nobene

**Naslednja seja:** M1.5c, sinhroni JSON controllerji.

---

### 2026-09-11 (4) — M1.5a: varen zapis clone JSON

**Paket:** M1.5 (prvi del)
**Stanje:** delno

**Narejeno:**

- Dodan `rework/data/SafeFileWriter`: začasna datoteka v isti mapi, `flush` + disk `sync`,
  validacija pred zamenjavo, atomski `Files.move` in preverjen fallback z obnovitvenim `.bak`.
- `NBTJsonUtil.SaveFile` zdaj kandidat ponovno prebere in zahteva bitno enakovreden NBT,
  preden ga namesti na ciljno pot.
- `ServerCloneController` ne uporablja več nevarnega zaporedja izbriši-staro → `renameTo`;
  clone JSON zapiše neposredno skozi varni writer.
- Štirje novi testi pokrijejo prvi zapis, zamenjavo, zavrnjeno validacijo z ohranitvijo stare
  datoteke in osirotel recovery backup. 21 primerjalnih testov ostaja zelenih v obeh načinih.
- Build zapakira 14 razredov; `verify-package` je preveril 1716 originalnih vnosov in našel
  samo 6 dovoljenih zamenjav originalnih razredov.
- Po uporabnikovem pojasnilu je R9 označen kot minoren; M1.4 in M1.8 sta odložena, R9 migracija
  ni potrebna. Splošna B1/B2 zaščita ostaja, ker varuje vse podatke sveta.
- Dodan ignore za `*.log.gz`; pomotoma sledljiv Gradle log je odstranjen samo iz indeksa.

**Ni narejeno in zakaj:**

- Preostali JSON in stisnjeni-NBT controllerji še uporabljajo lastne `_new`/`_old` poti.
  Prenos vsakega originalnega razreda zahteva ločen preverjen baseline, zato ostanejo M1.5b.
- Polni fault-injection nabor (zaklenjena datoteka, prekinitev procesa, zavrnjen dostop) je
  še vedno M1.9.

**Ugotovitve:**

- Nova koda v originalnem testnem classpathu ne obstaja; `SafeFileWriterTest` zato teče samo
  pod `test`, medtem ko vseh 21 karakterizacijskih testov še vedno teče tudi pod `testOriginal`.

**Spremembe obnašanja:** clone JSON se namesti šele po trajnem zapisu in uspešni validaciji.

**Meritve:** nobene

**Naslednja seja:** M1.5b, preklop preostalih controllerjev na `SafeFileWriter`.

---

### 2026-09-11 (3) — M0.4: sledljivo git izhodišče

**Paket:** M0.4
**Stanje:** končano

**Narejeno:**

- Inicializiran git repozitorij na veji `main`; preverjena sta korenski in Gradlov
  `.gitignore`, zato lokalni JDK, odvisnosti, build izhodi, razvojni svetovi in logi niso
  vključeni v zgodovino.
- Na uporabnikovem računalniku pognan `.\dev.ps1 testOriginal test --offline`: vseh 21 testov
  je zelenih proti originalu in popravljeni kodi.
- Pognana `.\dev.ps1 buildPatchedMod --offline` in `.\verify-package.ps1`: build uspešen,
  zapakiranih je 8 obnovljenih razredov; preverjenih je 1716 originalnih vnosov, spremenjenih
  je samo 5 dovoljenih originalnih class datotek. Preostali 3 so novi `NbtJson` razredi.
- Ustvarjen prvi commit, ki predstavlja preverjeno izhodišče za nadaljnje pakete.

**Ni narejeno in zakaj:**

- M0.5–M0.8 ostajajo ločeni paketi; protokol dovoljuje en delovni paket na sejo.

**Ugotovitve:**

- Prejšnja omejitev, da lokalnih ukazov ni mogoče zaganjati, ne velja več.

**Spremembe obnašanja:** nobene

**Meritve:** nobene

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`).

---

### 2026-09-11 (2) — M1.1–M1.3: NBT↔JSON serializer

**Paket:** M1.1, M1.2, M1.3
**Stanje:** končano

**Narejeno:**

- Postavljeno oblačno prevajalno in testno okolje z **pravimi** projektnimi artefakti
  (mapiran CustomNPCs JAR, `forgeSrc-1.12.2-14.23.5.2847.jar`, guava 21.0, commons-io 2.5,
  log4j 2.8.1). S tem je bilo mogoče trditve preveriti z izvedbo, ne samo z branjem.
- **Empirično potrjenih 9 napak** v `NBTJsonUtil` in **ovrženi 2** prvotni hipotezi.
  Vse podrobnosti: [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md).
- Napisan nadomestni serializer `noppes/npcs/rework/data/NbtJson.java` in shim
  `noppes/npcs/util/NBTJsonUtil.java`, ki ohrani vse javne podpise originala.
- Napisana testa `NbtJsonBaselineTest` (17 testov) in `NbtJsonFuzzTest`. Oba tečeta v dveh
  načinih: pod `testOriginal` trdita originalno napačno obnašanje, pod `test` popravljeno.
  Ločevanje gre prek refleksijske preverbe polja `NBTJsonUtil.REWORK_SERIALIZER`.
- V oblaku zeleno v obeh načinih: 18/18 novih testov.

**Ugotovitve:**

- Diferencialni fuzz čez 4000 naključnih NBT struktur: **original izgubi ali spremeni podatke
  v 1598 primerih (40 %)**, novi v 0.
- **Zapisano besedilo je znak za znak enako originalnemu v vseh 4000 primerih.** Vse napake so
  bile v logiki pretvorbe, ne v formatu. Posledica: za R9 **ni potrebna migracija**, obstoječe
  datoteke se berejo takoj, in nespremenjen CustomNPCs zna brati datoteke, ki jih zapišemo mi.
- Najhujše potrjene napake: long array vrednosti prisekane na 8 bitov; `NBTTagList` bytov,
  intov ali longov se tiho spremeni v array tip (zato `getTagList` vrne prazno in nastavitev
  se "vrne nazaj"); prazen tipiziran array izgubi tip; nizi izgubijo vodilne presledke; niz,
  ki se konča z `\`, `NaN`, `Infinity`, prazen ključ in compound s >3000 ključi naredijo
  **celo datoteko neberljivo**.
- Hitrost na compoundu s 1500 ključi: zapis 235 ms → 2,9 ms, branje 2386 ms → 6,5 ms.
  Ker gredo skozi ta razred vse clone, dialog, quest, faction in player datoteke ob zagonu
  sveta, je to tudi prispevek k **R5**.
- Dve prvotni hipotezi sta bili napačni (byte/long array zapis; `SaveFile` ne zapre writerja).
  `javap` na originalnem bytecode ju je ovrgel. Pravilo iz `05-SEJA-PROTOKOL.md` se je
  izplačalo že prvič.

**Ni narejeno in zakaj:**

- Lokalni build in testi takrat še niso bili pognani; izvedeni so bili v naslednji seji
  M0.4 in so uspešni.
- M1.4–M1.9 ostajajo odprti.

**Spremembe obnašanja:** glej tabelo "Sprejete spremembe obnašanja".

**Meritve:** [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md)

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`), nato M1.4.

---

### 2026-09-11 (1) — Postavitev načrta reworka

**Narejeno:**

- Pregledano obstoječe okolje (`OKOLJE.md`, `PLAN_IMPLEMENTACIJE.md`, `dev/build.gradle`,
  `environment-lock.json`, `audit/package-verification.txt`)
- Prebrani ključni razredi v `dev/reference-src/`: `EntityNPCInterface`, `EntityNPCFlying`,
  `FlyingMoveHelper`, `EntityAIFollow`, `EntityAIAttackTarget`, `ServerCloneController`,
  `ScriptController`, `ScriptContainer`, `NBTJsonUtil`, `ItemMounter`, `EntityChairMount`
- Raziskana referenčna projekta: BetaZavr 1.12.2-Unofficial in KAMKEEL CustomNPC+ (1.7.10)
- Sprejete odločitve D-001 do D-007 (`01-ARHITEKTURA.md`)
- Napisana dokumentacija: `README.md`, `docs/01-ARHITEKTURA.md`, `docs/02-ZAHTEVE.md`,
  `docs/03-FAZE.md`, `docs/04-STANJE.md`, `docs/05-SEJA-PROTOKOL.md`

**Ključne ugotovitve iz kode (podrobno v `02-ZAHTEVE.md`):**

- **R7 in R5 sta povezana**: `ScriptContainer.java:51,142` ima **en globalen `static` lock za
  vse skripte vseh NPC-jev**. Najverjetnejše največje ozko grlo pri skriptanih NPC-jih.
- **R1**: `EntityNPCInterface` ne override-a nobene passenger metode; `updateHitbox()` se ob
  spremembi jahanja ne pokliče; `EntityAIFollow.java:70` teleportira jahača z mounta;
  `EntityAIAttackTarget.java:98` veže napadalni doseg na `npc.width`, kar pri majhnem hitboxu
  sili NPC-je v kup.
- **R4 in R7 sta isti problem**: strojno berljiva shema, tipi, validacija, hitra povratna
  zanka. Zato M7 gradi na M6.
- **CustomNPC+ ima R7, R4 in del R2 že implementirane**. Glavni referenčni vir.

**Naslednja seja:** začetek razvoja.

---

## Odprta vprašanja za uporabnika

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | **odgovorjeno 17. 9.** — uporabnik nima dostopa; zabeleženo kot blokada (M0.7 §6), ponovno odpreti pred M10 |
| Q2 | Kopija sveta s problematičnimi NPC-ji | M1.4 | zaprto — uporabnik je nima; paket odložen |
| Q3 | Konkretna pokvarjena clone JSON datoteka | M1.4 | zaprto — ne obstaja; paket odložen |
| Q4 | Katera nastavitev se vrne nazaj? | R9 | odgovorjeno — follower role, action `waiting` se po clone lahko vrne v `following`; minorno |
| Q5 | Pri R1 — jahač in nosilec sta oba CustomNPC, ali je eden vanilla mob (konj)? | M2.2 | **odgovorjeno 15. 9.** — oba sta CustomNPC |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | **odprto, a ne blokira M2.3** — scenarij meri samostojno letenje, kar je podlaga za oba primera; odgovor je potreben šele za obseg M4 |
| Q7 | Pri R3 — katerih 5–8 funkcij CustomNPC+ je najbolj pomembnih? | M8.2 | **zaprto 3. 10.** — M8 izločen iz plana (D-023) |
| Q8 | Pri R8 — kateri provider (Anthropic / OpenAI / lokalni model)? | M9.3 | odprto |
| Q9 | Koliko NPC-jev je "veliko" v tvojem primeru? 100? 500? 2000? | M2.4, cilj za M5 | **odgovorjeno 15. 9.** — cilj še ni določen; merimo 50/200/500 in se odločimo po podatkih |
| Q10 | Ali strežnik, kjer to teče, sploh ima izhodni internetni dostop? | M9.1 | odprto |
| Q11 | Zakaj skupina obstane pred **drugo** stopnico (z = 36), prvo (z = 20) pa prestopi? | M2.3/M2.4, kakovost scenarijev | **zaprto 17. 9.** — ni bila ovira, ampak domet iskanja poti; glej Znane omejitve |
| Q12 | Uvoz zunanjih 3D modelov (npr. prenesen model zmaja) kot model NPC-ja — je to v obsegu? Če da: kateri format (`.bbmodel` / OBJ / JSON), in kako model pride do igralcev (resource pack / lokalna mapa / prenos z URL-ja / push s strežnika)? | nova zahteva (R11?; R10 je od 6. 10. poveljevanje prek zemljevida); odvisna od M7, ker zunanji model prinese svoj skelet | **odprto, odprto 17. 9.** — danes mod zna samo (a) teksture prek URL-ja (`skinUrl`, `capeTexture`, `overlayTexture`) in (b) prevzem modela **registrirane entitete** (`setModel(id)` → `ModelData.entityClass`, `DataDisplay:405`), torej model iz že nameščenega moda. Geometrija iz datoteke ne obstaja: vsi modeli so hardcoded `ModelBase` razredi (`client/model/`). V M8 katalogu tega ni, ker tega nima niti CustomNPC+. Ocena XL; odločitev šele ko je M7 (animacijski skelet) jasen |
| Q13 | Naj bo predzavrnitev tarč (`RwTargetPrefilter`) privzeto vklopljena? | M5, D-007 | **odločeno 7. 10.: da, način 2 (S1 + S2)** → D-027 |
| Q15 | Naj nosilec z NPC jahačem pod `RwHitbox=1` sprejme potisk (masa nosilec + jahač)? | M3.9, R6 × R1 | **odločeno 7. 10.: da** → D-028 |
| Q16 | Ali sme projekt za vanilla vroče točke dobiti **vanilla posege** (npr. prek MixinBooterja)? Največji preostali strošek idle je vanilla `Entity.move` (26–29 % CPU, od tega poizvedba entitet za trke 11,8 %), ki ga CNPC prek lastnih razredov ne doseže; Lithium to v novejših verzijah reši brez spremembe obnašanja, za 1.12 porta ni. Pomeni novo odvisnost in poseg izven CNPC razredov (D-006) | M5, D-006 | **odločeno 8. 10.: da, zapakirano v CNPC jar (lasten coremod, brez zunanjega moda)** → D-031, paketa M5.15/M5.16 |
| Q17 | Naj bo pomnjenje tipa vozlišča v sledenju poti (S14b, `RwPathFollowCache=1`) privzeto vklopljeno? Izid je enak originalu (0 neujemanj v 1,25 M primerjav v svetu), boj-500 MSPT −33 % | M5.11, D-007 | **odločeno 9. 10.: da, in splošno pravilo** → D-032 |
| Q18 | Popravek »prvi ukaz po zagonu serverja se izgubi« (ogrevanje vhoda z `list`) je zdaj samo v `perf-run.ps1`. Vsaka od 11 scenarijskih skript ima svojo kopijo `Start-DevServer`/`Send-Command`; v `nav-run`, `rwdiag-run`, `r1/r2-run`, `m36/m39-run`, `hb-run`, `fixture-run`, `testworld-run` in `smoke-server` lahko izgubljen prvi ukaz tiho spremeni pogoj meritve (npr. izgubljen `gamerule`). Naj se `Start-DevServer`, `Send-Command` in `Prime-ServerInput` preselijo v `meritve-lib.ps1` in popravijo vse skripte (svoj paket)? | vsi scenariji, veljavnost meritev | **odprto 10. 10.**; priporočilo: da |
| Q19 | Ostali trije klicatelji `Server.sendAssociatedData` (`UPDATE_NPC` ob uredbi NPC-ja, `PARTICLE`, `DELETE_NPC`) še pregledajo ~450 chunkov na klic. Niso na tick, zato izmerjene koristi ni; ob masovnem spawnu/brisanju (500 NPC-jev) pa je to 225.000 pregledov chunkov. Naj se preusmerijo tudi ti (S7b)? | M5, R5 | **odprto 10. 10.**; priporočilo: samo če scenarij pokaže številko |
| Q20 | Enakost S7 za **neprazen** seznam prejemnikov v svetu ni bila preverjena s pravim igralcem (dedicirani strežnik scenarija jih ne ima); stoji na viru vanille, 200.000 legah v testu in preverbi na NPC-jih. Naj se pred izdajo doda preverba v klientu (`/rwblink 2` z igralcem v svetu)? | M5.13, M10 | **odprto 10. 10.**; priporočilo: da, skupaj z M3.8b GUI preverbo |
| Q21 | Naj NPC, ki do svoje tarče **nima poti**, 20 tickov ne poskuša znova (S5, `RwPathNegCache=1`)? Dobitek v prizorišču nedosegljive tarče: MSPT **17,85 → 14,46 ms (−19 %)**, alokacija 292 → 135 MB/s, ticki > 25 ms 45 → 10. Cena: v 59 % preskočenih iskanj bi original dal delno pot, zato NPC do 20 tickov ne gre proti tarči (stoji, namesto da se drenja ob oviri); ko se pot odpre, reagira do 20 tickov kasneje. Ni enakovredno originalu, zato D-032 ne velja | M5.6, D-007 | **odprto 10. 10.**; priporočilo: da |
| Q14 | Pri R10 — kateri zemljevid: lasten iz `ladja_mod` (izbira kot pri ladjah), JourneyMap, Xaero ali nov v CNPC? | M11.3 | **odprto 6. 10.**, ne blokira do M11 |

---

## Odprti pojavi (nereproducirani — hipoteze)

| ID | Pojav | Kje se vidi | Stanje |
|---|---|---|---|
| P1 | Leteči NPC, ki obstane **natanko 0,5 bloka od sredine svojega vozlišča**, se ne premakne več: `gib = 0`, pot ostane cela, entiteta se normalno tika. Kopenski v istem svetu se premika | M2.3 fazi B in C, oba zagona 17. 9.; 12 NPC-jev, 0,00 bloka v 900 tickih, `dStarost = 20`, `gib = 0` | **zožen** — dve razlagi ovrženi z meritvijo. Ostane `FlyingMoveHelper`: `pathFollow` prestevilči pri < 0,45, `FlyingMoveHelper:39` premakne pri > 0,5. Potrdi ali ovrže ga **faza D** ob naslednjem zagonu |

| P2 | NPC z napadalno tarčo izven `aggroRange` (Čebiševa razdalja) ima napad, ki utripa: `shouldContinueExecuting` ga ustavi, `shouldExecute` brez preverbe dosega spet začne, `resetTask` vsakič pobriše pot | M3.6 5. 10.: v načinu 1 so **vse** zakasnitve nad 20 tickov (7 vzorcev, do 100) izven dosega ob ukazu; v načinu 0 izven dosega do 160 | **potrjeno 5. 10. (korelacija v scenariju M3.6)** — obnašanje originala v obeh načinih; ali ga popraviti, odloči uporabnik |

Pravilo iz `05-SEJA-PROTOKOL.md`: bug brez reprodukcije je hipoteza. P1 ima meritev, nima
pa še razlage in ni ločen od možne napake scenarija, zato je tu in ne med bugi.

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
| 2026-10-10 | NPC, ki do tarče nima poti, 20 tickov ne poskuša znova (S5); **ni** enak izid — do 20 tickov zamika | M5.6, D-007, Q21 | `RwPathNegCache` | 0 (original) |
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

---

## Znane omejitve

- Meritve so bile narejene na OpenJDK 21, produkcijski build je Java 8.
- Dekompilacija **ni** izvorna koda. To je bilo tokrat dvakrat potrjeno v praksi: dve trditvi
  iz prvotne analize sta bili napačni. Sumljivo logiko vedno preveri z `javap`.
- Vseh 746 dekompiliranih datotek **ni** ročno pregledanih.
- Združljivost z uporabnikovim dejanskim modpackom ni preverjena (Q1) — **trajna blokada do M10**;
  uporabnik gradiv nima, vpliv je razčlenjen v `scenariji/M0.7-integracijska-matrika.md` §6.
- Bugi iz `PLAN_IMPLEMENTACIJE.md`: B1 in B2 sta v M1.5/M1.6, B5 v M6.5. B3, B4, B6, B7, B8
  še niso razporejeni v milestone.
- Dedicated server je preverjen (M0.5). Igranje v svetu z igralcem, GUI in questi še ni — to so vrstice
  IC1–IC6, IK1–IK5 in IL2–IL3 integracijske matrike (M0.7), vse še nepokrite.
- **Seja ima lupino na uporabnikovem računalniku, a linuxovo.** *(popravljeno 17. 9.;
  prej je tu pisalo, da lupine sploh ni.)* Priključene mape so v njej pod
  `$HOME/mnt/<mapa>`, na voljo so `git`, `python3`, `node`, `jq`, `sed`, `diff`. Kar v njej
  **ne gre**, je vse, kar potrebuje Windows: `.\dev.ps1`, `.\*-run.ps1`, gradle build,
  `verify-package.ps1` in zagon Minecrafta. Ti ostajajo na uporabniku. Delitev dela je
  torej: seja bere, piše, ureja, preverja in commita; uporabnik poganja build in svet ter
  javi log nazaj.
- **PowerShell skripte se da sintaktično preveriti brez Windowsa.** V oblačnem okolju
  PowerShell 7.4.6 (`Parser::ParseFile`) prebere `.ps1` in vrne napake razčlenjevanja.
  Izvedbe ne nadomesti, tipkarske napake pa ujame; uporabljeno 17. 9. na `r1-run.ps1`.
  PowerShell v oblačnem vsebniku **ni prednameščen** — seja ga vsakič potegne z GitHuba
  (`powershell-7.4.6-linux-x64.tar.gz`); to je nekaj deset sekund in gre skozi posredniški
  strežnik brez težav.
- **Pogodbo med skripto v svetu in razčlenjevalnikom loga se da preveriti brez Minecrafta.**
  `node dev/testworld/preveri-markerje.js` minificira `r2-control.js` enako kot
  `vstavi-skripto.py`, jo požene nad ponarejenim svetom in prešteje markerje; funkcije
  `Read-Samples`/`Read-Setup` iz `r2-run.ps1` se nato poženejo nad tem izpisom (iz AST-ja,
  brez glavnega telesa skripte). Ujame natanko razred napake iz M0.7 (izpis in
  razčlenjevalnik se nista ujemala). O obnašanju NPC-jev ne pove nič in zagona ne nadomesti.
- ~~Datotek, globljih od 7 map pod **priključeno** mapo, ni mogoče prenesti v sejo.~~
  **Ne velja več od 17. 9.:** lupina vidi celotno drevo priključene mape, ne glede na
  globino, in prenos v sejo za branje ni potreben. Prejšnje besedilo ostaja kot zgodovina:
  **rešeno 15. 9.:** poleg korena projekta je zdaj priključena tudi mapa `dev`, s čimer sta
  pod mejo `dev/src/patch/java/noppes/npcs/rework/…` (7 map) in dekompilirani Minecraft v
  `dev/build/tmp/recompileMc/sources/net/minecraft/…` (7 map). Če nova seja teh datotek ne
  vidi, mora uporabnik v namizni aplikaciji dodati mapo `CustomNPC_mod_rework\dev` —
  priključitev globlje mape (`…\src\patch\java`) ni potrebna in koren sam ne zadošča.
- **Seja sama prevede `noppes/npcs/rework/**` in požene njegove teste** (D-014), v oblačnem
  okolju proti mapiranim razredom iz `dev/build/tmp/recompileMc/compiled` in
  `dev/libs/customnpcs-mapped-01Oct19.jar`; manjkajoča guava in log4j se nadomestita z
  minimalnima nadomestkoma **izven** repozitorija. To preveri sintakso, podpise Forge
  API-jev in vso logiko brez Minecrafta — ne pa nalaganja moda ali obnašanja v svetu.
- **Prenesena datoteka se v seji ne osveži, če ima isto ime kot prej** — 16. 9. je prvi
  poskus tiho prevedel stare izvorne datoteke. Vsak nov prenos gre pod novim imenom.
- **Dekompiliran, Forge-patchan Minecraft je v projektu** in je verodostojnejši vir od
  spomina: `dev/build/tmp/recompileMc/sources/` (izvorna koda) in `…/compiled/` (razredi).
  Nastane ob `setupDecompWorkspace`. Uporabljen v M2.1c za `WorldServer` in `World`.
- Projekt teče na dveh delovnih postajah proti istemu `origin/main`. Seja začne z
  `git fetch origin` in preveri, ali je oddaljena veja pred lokalno.
- **Na namizni postaji `git fetch` ne deluje** *(ugotovljeno 17. 9.)*: lupina gre skozi
  posredniški strežnik, ki za `github.com` vrne `HTTP 403 ... after CONNECT`. Posledica je
  bila resna — seja je cel dan delala proti `origin/main`, ki je stal pri `b7827cd`, in
  na slepo podvojila že narejen M2.2. **Obvod**, ko `fetch` pade: oblačno okolje seje do
  GitHuba pride, zato tam `git clone`, nato
  `git bundle create <ime>.bundle main --not <zadnji skupni commit>`, bundle se prenese v
  `dev/build/tmp/` in lokalno se pobere z
  `git fetch <pot-do-bundla> main:refs/remotes/origin/main`. Bundle iz 9 commitov je imel
  128 KB. **Dokler `origin/main` ni potrjeno svež, seja ne sme začeti novega paketa.**
- **Seja lahko commita, ne more pa pushati.** *(ugotovljeno 17. 9.)* V sejini lupini
  `git fetch` in `git ls-remote` delujeta, `git push` pa pade z
  `could not read Username for 'https://github.com'`: poverilnice so na Windows strani
  (Git Credential Manager), v linuxovi lupini jih ni. Seja ima tudi svojo identiteto
  neznano, zato commita z `git -c user.name=… -c user.email=…`. **Push je vedno na
  uporabniku**, in seja mu to na koncu pove.
- **`smoke-server.ps1` prepiše `dev\run\server.properties` s svojim** (`level-name=m05-smoke`,
  seed `20260914`, druga vidna razdalja in težavnost). `r1-run.ps1` in `r2-run.ps1` pa
  fixture kopirata v `dev\run\world`, zato po smoke testu tečeta proti **drugemu svetu**:
  vsak `noppes clone spawn` javi `Could not find clone file`, v svetu ni NPC-jev in
  scenarij ~10 minut čaka na markerje, ki jih ne bo. Zgodilo se je 17. 9. Od takrat oba
  scenarija ob zagonu primerjata `level-name` in `level-seed` z `dev\testworld\server.properties`
  in ob neujemanju **padeta takoj** z navodilom, naj se požene `.\testworld.ps1`.
- **`.git/index.lock` zna ostati za sabo.** Brisanje datotek je v priključeni mapi privzeto
  izklopljeno, zato prekinjen `git` ukaz pusti `index.lock` in vsak naslednji `git add` ali
  `git commit` pade. Seja mora takrat zaprositi za dovoljenje za brisanje in datoteko
  odstraniti; zgodilo se je 17. 9.
- **Zapis iste datoteke dvakrat v isti seji lahko tiho ne uspe.** 15. 9. je drugi zapis
  `docs/04-STANJE.md` javil uspeh, na disku pa je ostala prejšnja verzija (45 479 B namesto
  46 946 B). Zapis pod novim imenom je uspel takoj. Pravilo: po vsakem zapisu preveri
  velikost datoteke, ob neujemanju zapiši pod novim imenom.
- `npc.update.window` je zgornja meja, ne točna poraba časa na NPC; točna meritev pride z M3.1.
- **NPC-ja ni mogoče poslati dlje od `NpcNavRange` (32) z enim samim klicem `navigateTo`.**
  Vanilla A* se prekine po 200 vozliščih in vrne **delno** pot (`PathFinder.findPath:65`),
  dolžino poti pa omeji na `getPathSearchRange()` = `FOLLOW_RANGE` = `NpcNavRange`
  (`:94`, `EntityNPCInterface.java:334`). NPC obstane na koncu delne poti in to izgleda kot
  ovira ali okvara AI — 17. 9. je stalo eno napačno razlago (Q11). **Vsak scenarij, ki pelje
  NPC dlje od 32 blokov ali po obvozu, mora pot osveževati.** Vanilla AI taski to počnejo
  sami, skripte ne.
- **Postavitev ob koncu faze ni vhodni pogoj naslednje faze.** *(ugotovljeno 21. 9.)* V
  scenariju M2.3 je `endPhase` NPC-je postavil na izhodišče, meritev pa se je začela 40
  server tickov pozneje — in v teh 40 tickih se je leteča proga s prosto potjo premaknila
  7,4 bloka nazaj proti cilju, čeprav sta pred postavitvijo tekla `clearNavigation()` in
  `setAttackTarget(null)`. Poskus z „eno samo spremenljivko" je bil s tem razveljavljen,
  preden se je začel, in tega se po končnih številkah ne vidi. **Pravilo: vsaka faza se
  postavi sama, v istem ticku kot začne, in vsak scenarij ima merilo, ki vhodni pogoj
  preveri iz loga** (M2.3 ima za to L12: `prevozeno + doCilja` prvega vzorca mora biti vsaj
  dolžina proge).
- **Peta veličina (µs na iskanje) potrebuje ogrevanje iskalnika, ne več vzorcev.**
  *(izmerjeno 21. 9., M2.7b)* Dva zagona istega scenarija na istem stroju, pet minut narazen:
  `usSkupaj` zadnjih dveh pometanj 9.069 in 18.076 µs proti 4.579 in 2.939 µs — v enem se je
  JIT do konca ogrel, v drugem ne. Več vzorcev na pometanje tega ne zapre, ker si vsi vzorci
  enega pometanja delijo isto stanje JVM-a: večji vzorec zmanjša šum *znotraj* pometanja, ves
  pomemben šum pa je *med* pometanji. Od M2.7b `nav-run.ps1` požene dve ogrevalni pometanji na
  progo, ju zavrže in požene `rwdiag reset`. **Enako velja za vsako prihodnjo časovno meritev
  kode, ki je JIT še ni videl.**
- **Merilo veljavnosti mora biti invarianta, ne časovni prag.** *(21. 9.)* Prva oblika merila
  N14 je zahtevala `ponP50 ≤ prviP50` („ogreto ni počasnejše od hladnega") in je ustavila
  serijo na razliki **6 %** — ko je JVM ogret, sta obe številki enaki in predznak razlike je
  naključen. Nadomestila jo je invarianta `prviN = iskanj` in `ponN = ponovitev`, ki mora
  držati po konstrukciji. **Časovna opažanja gredo v tabelo, kjer jih človek prebere, ne v
  `Check`, kjer šum ustavi serijo.** Isti prijem imata N9 in L12.
- **Scenariji si podajajo `dev\run\world` in naslednji meri, kar je pustil prejšnji.**
  *(21. 9., tretjič ta teden)* `.\rwdiag-run.ps1` je ob 12:55 meril svet, ki ga je pustil
  `.\nav-run.ps1`: `world.npc.loaded = 17` namesto 21 in brez `T_Scripted`. Merilo je padlo
  šele po dveh minutah in ni povedalo, zakaj, meritev pa je bila takrat ze posneta. Odtlej
  scenarij takrat **ustavi server in pove, naj se požene `.\testworld.ps1` in
  `.\testworld-run.ps1`**. Prvič se je isto zgodilo 17. 9. pri `r2-run.ps1` (server.properties
  od smoke testa). **Pravilo: vsak scenarij, ki potrebuje določen svet, to preveri in pade
  takoj.**
- **Meritev brez prisilno naloženih chunkov ali brez igralca je neveljavna** po 300 tickih
  (`WorldServer.updateEntities():628-644`). Vsak posnetek ima zato `world.chunks.forced`;
  če je ta 0 in je `world.players` 0, posnetek meri prazen tek. Velja za vse meritve M2+.
- **Vsaka meritev potrebuje ogrevanje.** Priklop chunk ticketa sproži nalaganje chunka na
  server niti in to se šteje v meritev: brez ogrevanja je bil p99 81,8 ms namesto 4,3 ms in
  `npc.update.window` 182,5 µs namesto 66,6 µs. `rwdiag-run.ps1` čaka 10 s
  (`-WarmupSeconds`); pri M2.4 s stotinami chunkov je treba to dolžino **izmeriti**
  (dva zagona zapored, primerjava p99), ne ugibati.
- `server.tick.ns` **max** je pri vsakem zagonu en sam tick, ki sovpada z autosave
  (58 / 74 / 87 / 124 ms). M2.5 ga mora izločiti ali poročati posebej.
- Od M2.5a ima posnetek **tabelo najpočasnejših tickov s kontekstom** (koliko NPC-jev je
  tiknilo, koliko chunkov se je naložilo, ali je tekel autosave) in števce tickov nad
  10/25/50/100 ms. Merila S1–S4 v `rwdiag-run.ps1` bodo ob prvem zagonu pripis autosave
  ticka potrdila ali ovrgla strojno, ne z branjem številk.
- **Sonda kakovosti navigacije (M2.7) sme teči samo iz server niti.** `NodeProcessor` je
  deljen z navigatorjem entitete, `PathFinder.findPath` pa ga na začetku inicializira in na
  koncu počisti. Iz ukaza (server nit) je to varno, iz druge niti bi bila okvara in ne
  meritev. Prav tako pometanje **zavrne delo, dokler ni `rwdiag on`**: `NavProbe` se ob
  vklopu počisti, zato bi pometanje pred vklopom izginilo in scenarij bi bral prazno meritev.
- **`Path.getTarget()` je `@SideOnly(Side.CLIENT)`** (`Path.java:152`), zato se celost poti
  na strežniku ugotovi s primerjavo zadnje točke poti s ciljnim vozliščem (meja 2,0 bloka).
- **`nav.ai.path.new` je spodnja meja, ne število iskanj poti.** Vanilla ob iskanju ne pošlje
  dogodka; zbiralnik zato šteje spremembo identitete objekta `Path`, torej uspešne dodelitve.
  Iskanje, ki vrne `null` ali ga `canNavigate()` zavrne, je nevidno. Cena navigacije na tick
  se do M3.1 ocenjuje kot zmnožek časa enega iskanja (sonda) in te pogostosti.
- Že pokvarjenih datotek na disku nova koda ne popravlja; M1.4 je po navodilu uporabnika
  odložen, dokler ne obstaja konkreten primer.
