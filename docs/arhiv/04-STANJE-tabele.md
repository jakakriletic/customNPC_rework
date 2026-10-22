# Arhiv: tabele iz 04-STANJE

Dobesedno prenesene vsebine iz `docs/04-STANJE.md` (stanje 2026-10-19, pred prestrukturiranjem).
Povezave so popravljene na relativno pot iz mape `docs/arhiv/`; besedilo je nespremenjeno.
Kazalo: §1 Trenutno stanje (polna) · §2 Napredek po milestonih (polna) · §3 M1/M2 po paketih ·
§4 Vsa vprašanja Q1–Q29 · §5 Znane omejitve.

Pravila vzdrževanja: odločeno vprašanje se v `04-STANJE.md` izbriše in tu v §4 posodobi;
nove omejitve/pasti se dopisujejo na konec §5.

---

## 1. Trenutno stanje (polna tabela, 2026-10-18)

| | |
|---|---|
| Zadnja posodobitev | **2026-10-18** (seja 93: **M5-K.1 zaključen** — baseline klienta 5 celic × 3 ponovitve, T1–T6 zelena (p50 idle-50/200/500 2,15 / 6,57 / 11,57 ms, boj-200/500 5,78 / 12,55 ms); **napaka pogoja M5-K.0**: F1 skrije vsa imena NPC-jev (idle-200 4,78 → 6,57 ms z imeni), harness zdaj z vidnim GUI-jem; JFR ujame premalo vzorcev render niti (38 v 14 s), zato lasten vzorčevalnik `StackSampler` (`k-run -Vzorci 1`): NPC-ji ~90 % render niti, model 31–38 %, **pokrivalo 19–26 % (6,7 µs/NPC, nov paket K14)**, ime + naslov 20 % pri 200 v dometu (~8 µs na napis), oči 10–14 %; vrstni red ni obrnjen, **Q28** (K14/K4/K3 pred K5?); [zapis](../meritve/2026-10-18-M5-K.1-baseline-profil.md)). Prej seja 92: **M5-K.0 zaključen** — klientski harness `k-run.ps1` + `rework/diag/ClientHarness`, merila K0–K8 zelena v idle-50/200/500 in boj-200; sličica p50 idle-200 4,78 ms, idle-500 11,40 ms (85 FPS); trije popravki harnessa (locale, boj K3/K4); [zapis](../meritve/2026-10-16-M5-K.0-harness.md); naslednji M5-K.1). Prej seja 91: **M5.8 zaključen** — skrajšane potrditve K3 po Q27, vse zelene: pri p95 ≤ 25 ms idle O 350 / S 3500 (**10×**), boj O 1000 / S 1500 (**1,5×**), nedosegljiva O 500 / S 700 (**1,4×**); [zapis](../meritve/2026-10-16-M5.8-kapaciteta.md); strežniški del M5-S končan, naslednji M5-K.0). Prej seja 90: **M5.8 lestvica končana** — vse celice obkrožene; p95 ≤ 25 ms: idle O 350–400 / S 3500–4000 (**~10×**), boj O 1000–1500 / S 1500–2000 (**~1,5×**, pri istem N −38 %), nedosegljiva O 500–600 / S 700–800 (~1,4×); ≤ 50 ms idle S 7000–8000 (~13×); napaka merilnega pogoja (meja prisilnih chunkov 400 v boju ≥ 2000) popravljena → 2000 + merilo P4c; **potrditev K3 idle: 350 → 3500 pri 25 ms (10×), razponi ločeni**; ostale potrditve ustavljene na zahtevo uporabnika, obseg Q27; nov paket **M4.15** (izbira vanilla/Baritone v GUI, D-036); [zapis](../meritve/2026-10-15-M5.8-lestvica.md)). Prej seja 89: M5.8 delno (idle lestvici). Prej seja 88: **Q26 → D-035**, `RwFarLos` privzeto 0). Prej seja 87: odgovori na **Q23–Q25 → D-035** — M5-V po sprejemu privzeto vklopljen, cilj dedicirani strežnik (`view-distance=24` = 5,4× več tickajočih chunkov, izmeri M5-V.1), vrstni red M5-K.0 → M5-K.1 → K5 → M5-V → K12; novo Q26 (`RwFarLos`). Prej seja 86: **M5-V.0** — raziskava vidnosti NPC-jev na daljavo po zahtevi uporabnika: NPC-ji so vidni samo do ±64 blokov, ker jih strežnik dlje ne pošlje (`tracker(64, 3, true)`); izbrana varianta B (nadomestki po kanalu `rwfar` + impostorji z mipmapi v enem klicu), nov obvezen modul **M5-V** na koncu M5-K (D-034), Q23–Q25; [raziskava](../raziskave/M5-V.0-daljna-vidnost.md). Prej seja 85: **M5.16 zaključen** — prvi pravi poseg v vanilla bajtkodo: `World.getCollisionBoxes` preskoči poizvedbo entitet za trke, kadar v svetu nihče ne more prispevati trkalne škatle; enakost **945.162 primerjav / 0 neujemanj** v boju in po kodi (v vanilli prispevata samo `EntityBoat`/`EntityMinecart` in `EntityShulker`), idle-500 (5 + 5 ponovitev, **ločeni razponi**) MSPT povp 2,787 → **2,540 ms (−8,9 %)**, p95 4,194 → **3,867**, µs/NPC 5,47 → 4,98, 100 % klicev preskočenih; boj-500 brez regresije; 15 novih testov (4 nad pravimi bajti `net.minecraft.world.World`), dimni test M0.5 **20/20** (tudi s stikalom na 1), `verify-package` PASS → **`RwMoveBoxes` privzeto 1** (D-032). Produkcijski zagon ostane neprever­jen → Q22 postane najbolj nujno. Prej seja 84: **M5.15 zaključen** — infrastruktura vanilla posegov: lasten coremod v CNPC jarju (`rework/core`), manifest `FMLCorePlugin` + `FMLCorePluginContainsFMLMod`, `verify-package` razliko manifesta preveri vnos za vnosom; dimni test M0.5 18/18 zelenih, coremod naložen, transformer brez posega; zagon na pravem Forge strežniku ni preverjen → Q22. Prej seje 79-83. **Seja 83: M5.14/S16** — senčna polja CNPC ključev `EntityDataManager`: enakost **0 neujemanj / 52,0 M primerjav**, idle-500 MSPT **3,02 → 2,86 ms (−5,3 %)**, p95 4,59 → 4,33 → **privzeto 1** (D-032). **Seja 82: M5.17/S14c** — pomnjenje tipa vozlišča med celim iskanjem poti: enakost **0 neujemanj / 239,5 M primerjav**, 184,0 → **144,9 µs** na iskanje, `nedosegljiva`-500 MSPT **17,96 → 15,25 ms (−15 %)**, `boj`-500 nevtralen → **privzeto 1** (D-032). **Seja 81: M5.6/S5** — negativni predpomnilnik iskanja poti: v prizorišču nedosegljive tarče MSPT **17,85 → 14,46 ms (−19 %)**, alokacija 292 → 135 MB/s, a **ni enakovreden originalu** (59 % preskokov bi original odgovoril drugače) → privzeto 0 in **Q21**; nov kandidat brez spremembe obnašanja: M5.17 (S14c). **Seja 80: M5.9 zaključen** — scenarij nedosegljive tarče (varianta `nedosegljiva`, pas ograje: tarča vidna, brez poti) in instrumentacija iskanja poti: iskanje je **2,45 % ticka pri boju proti 51,18 % pri nedosegljivi tarči**, eno iskanje 3,5 proti **180,5 µs** (vanilla A* porabi ves proračun 200 vozlišč), MSPT 9,23 → **17,73 ms**, alokacija 91 → 293 MB/s → **M5.2, M5.6 in S5 dobijo podlago**. Prej seja 79: **M5.13 zaključen** — S7, prejemniki utripa oči po `world.playerEntities`: iskanje 55,42 → **0,12 µs**, idle-500 celi zagoni MSPT povp 2,824 → **2,570 ms**, p95 4,325 → **3,932**, p99 4,719 → 4,325 — vse nad šumom; enakost 0 neujemanj (4.327 primerjav v svetu, 200.000 leg v testu); `RwBlinkRecipients` privzeto 1 (D-032). Ob tem odpravljena blokada scenarijev: **prvi ukaz po zagonu serverja se je izgubljal** (gradlov posredovalnik vhoda), zdaj se vhod ogreje z `list` — Q18, Q19, Q20 odprta. Prej seja 78: **M5.12 zaključen** — S4, `onCollide` brez opazovalca preskoči poizvedbo: idle-500 p95 5,24 → **3,93 ms**, p99 6,16 → **4,13**, povp 3,53 → 3,02 (v šumu A); enakost po kodi + test + poslušalec v svetu (K2/K3); `RwCollideSkip` privzeto 1 (D-032). Prej seja 77: Q17 → **D-032**, optimizacija z dokazano enakim izidom in izmerjeno koristjo je privzeto vklopljena, `RwPathFollowCache` privzeto 1. **M5.11 zaključen** — S14b, pomnjenje tipa vozlišča v sledenju poti: boj-500 MSPT povp **8,73 → 5,88 ms (−33 %)**, p95 9,96 → 7,21, alokacija 89 → 58 MB/s, enakost 0 / 1,25 M primerjav; S14 v istem boju v šumu in odstranjen; stikalo `RwPathFollowCache` preštevilčeno (1 = S14b), privzeto 1 (D-032). Prej seja 76: **M5.10 zaključen** — neposredno izmerjeno: `pathFollow` je v boju-500 **47,7–53,6 %** ticka (prag 25 % za M5.11 presežen); na `pathFollow` vedno 0 ali 1 kandidat; šum boja je v delu na klic, ne v številu klicev. Prej seja 75: **M5.0 zaključen** — raziskava preostanka M5: boj je edina celica brez napredka, njegov strošek je ocena tipa vozlišča v vanilla sledenju poti (~1.110 branj na klic, model se ujema), lock in iskanje poti nista ozko grlo; plan M5 prepisan (D-030), Q16. Prej seja 74: **D-029** — vsak milestone se začne z raziskavo izvedbe M?.0, plan ni dogma; M5-S S14 v kodi, enakost dokazana 0/616.276, A/B nedokazan. Prej seja 73: **M5-S P1 profil JFR** — boj-500: 48 % CPU strežniške niti v vanilla `pathFollow` → `isDirectPathBetweenPoints` (27 % hash iskanje chunka), iskanje poti 2 %; idle: `Entity.move` 29 %, `onCollide` 12 %, utrip oči 6 %; nov kandidat S14, vrstni red S14 → S4 → S7. Prej seja 72: odločitvi Q13 → D-027, `RwTargetPrefilter` privzeto 2; Q15 → D-028, nosilec z NPC posadko sprejme potisk. Prej seja 71: **M5-S S2 zaključen** — poizvedba iskalnika tarč samo po igralcih, `RwTargetPrefilter=2`: idle-500 p95 10,2 → 5,2 ms, boj nevtralen) |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; znana omejitev: zadnji prihod na skupni cilj 2–6 s kasneje. Izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](../meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Trenutni milestone | **M3 — jedro entitete**: M3.1–M3.5 zaključeni (**M3.5 zaključen 24. 9.:** E9 v svetu — način 0 po sestopu 1,463, način 1 1,900). **M3.6 preusmerjen in v kodi 24. 9.:** premisa o mutex bitih ovržena (D-020), popravljena je prioriteta napada pred tavanjem pod stikalom `RwAttackPriority`. **M3.6 zaključen 5. 10.:** način 1 zelen (tavajoči v dosegu 10/10 vzorcev z zakasnitvijo 10 = kontrola), način 0 napako ponovi (združeno 6/8 nad kontrolo, mediana 25), hipoteza P2 (`aggroRange`) potrjena. M3.7 in M3.8 zaključena 5. 10. R1 popravljen pod stikalom `RwMountSteering=1` (M3.3). M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | ~~M5.8~~, ~~M5-K.0~~ (16. 10.) in ~~M5-K.1~~ (**18. 10.**); zdaj **K5** po D-035, ali najprej **K14 → K4 → K3** (brez spremembe videza, skupaj ~45 % render niti), če uporabnik tako odloči v **Q28** — od **D-033** (14. 10.) je klientska optimizacija obvezen del M5; na koncu M5-K obvezen modul **M5-V** (vidnost NPC-jev do 24 chunkov, D-034); po Q25 (D-035) vrstni red **M5-K.0 → M5-K.1 → K5 → M5-V.1 … M5-V.8 → K12** → ostali K-paketi; ~~M5.2~~ zavržen (D-037); pred izdajo **Q22** (pravi Forge strežnik: od M5.16 jar privzeto spreminja vanilla bajtkodo, pot v produkciji je preverjena samo statično); **M4.0** (raziskava M4) je lahko vmes; vrstni red v `03-FAZE.md` (D-030), [raziskava M5.0](../raziskave/M5.0-preostanek-performance.md), [M5.11](../meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md). Q17 odločeno → D-032. **M4.0** je lahko vmes (raziskava, brez merjenja). Vzporedno: M3.8b GUI v klientu (z igralcem). Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Prevedljivih razredov | **141** datotek `.java` v `src/patch` (18. 10.; +3 harness M5-K.0, +1 `StackSampler`), od tega **81** v `noppes/npcs/rework/…` (`ai` 5, `core` 9, `data` 5, `diag` 19, `entity` 13, `formation` 15, `nav` 12, `net` 3) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **66** vnosov (65 razredov originala + manifest coremoda). (Števci so prešteti 14. 10.; prejšnja vrstica je navajala 128/48, kar se z nobenim štetjem ne ujema) |
| Testi | **287 JUnit** v 44 datotekah (18. 10.; +2 `StackSamplerTest`, `HarnessJobTest` razširjen; vsi zeleni 18. 10. skupaj s testOriginal 35/0; prej 285 po M5-K.0) + **35 primerjalnih testov originala** (naloga `:testOriginal` proti nespremenjenemu binarju); vsi zeleni 14. 10. Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c, zelene 18. 9. v oblačnem PowerShellu). (Vrstica je bila do 10. 10. zastarela — navajala je stanje iz 21. 9.) |
| Odprti pojavi | **nobenega blokirnega.** P1 je 21. 9. **ovržen** z meritvijo: faza D je začela natanko na z = −16,0 in leteči NPC-ji so se premikali že v prvem vzorcu (`gib = 0,1183`, prevozili 15,93). Hipoteza `pathFollow` 0,45 proti `FlyingMoveHelper` 0,5 je padla. Ostane **R-P1b**: stara zmrznitev je zahtevala postavitev izven mreže **in** 40 tickov mirovanja pred `navigateTo`; recept je zapisan, poskus (faza E) se požene šele, če ga M4 potrebuje |
| Ugotovitev M2.3 (21. 9.) | **R2 v tem prizorišču ni okvara letenja, ampak zastarela delna pot.** En sam `navigateTo` da pot, ki se konča pred oviro (`cele = 0/6`) in je nič ne zamenja; osvežena pot (faza B) in vanilla AI (faza C) isto skupino spravita čez. Popravek v M4 mora osveževati pot, ne spreminjati move helperja |
| Zagoni 23. 9. | **`nav-run` N1–N15 zelena** (z ogrevanjem: začetno pometanje G/O 2.903 / 4.343 µs, po fazi A 13.173 / 16.572 µs — razlika je ponovljiva, ne šum; A/B primerjati samo začetna pometanja). R2 ponovljen: faza A G 1/8, faza B 6/8. **`rwdiag-run` zelen, 0 padlih** (S1–S7 prvič ovrednotena v svetu): MSPT p50/p95/p99 = 0,54/1,15/2,36 ms, max 58,9 ms; brez autosave ticka max 52,7 ms, p99 2,16 ms; 8 NPC-jev tika vsak tick. Dodana oznaka `dev\run\world\rework-scenarij.txt`: nav-run jo zapiše, nav-run in rwdiag-run ob njej takoj padeta (dvakrat se je zgodilo, da je scenarij tekel na napačnem svetu). Vrstni red: testworld → testworld-run → rwdiag-run → nav-run zadnji |
| Ogrevanje (M2.7b, 21. 9.) | **Peta veličina potrebuje ogrevanje iskalnika, tako kot meritev potrebuje ogrevanje chunkov.** Dva zagona pet minut narazen: `usSkupaj` zadnjih dveh pometanj 9.069 / 18.076 µs proti 4.579 / 2.939 µs. Več vzorcev tega ne reši — vseh 56 ogretih vzorcev enega pometanja si deli isto stanje JVM-a. [zapis](../meritve/2026-09-21-M2.7b-ogrevanje.md) |
| Formacije (M4.14a) | koda iz 18. 9. **integrirana 27. 9.** na M3.6 (odločitev **D-021**, prej lokalno D-017); +15 razredov v `rework/formation`, 21 testov; v svetu še ne pognana |
| Šumni pas (M2.5c, 18. 9.) | **46 veličin od 59 ima razpon nič** čez tri ponovitve; vse, kar opisuje vedenje skupine in kakovost poti, je deterministično do enega ticka. Šumna je samo veličina 5 (µs na iskanje), do **114 %** — zato A/B na ceni iskanja (M4.11, M5.6) do M2.7b ni merljiv. [zapis](../meritve/2026-09-18-M2.5c-ponovitve-nav.md) |
| Odprta vprašanja iz M2.7 | dva NPC-ja od osmih na progi z grlom ne prispeta niti ob osveženi poti (zamašek ali `canNavigate()`, loči M3.1); najpočasnejši tick meritve (232 ms) ni bil autosave, ampak tick s pathfindingom |
| Blokade | Q1 je 17. 9. zabeležena kot **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6–Q8, Q10 in Q12 odprta; specifična R9 forenzika je po navodilu uporabnika odložena, ne blokirana |
| Omejitev orodij | **spremenjeno 5. 10.**: seja teče na uporabnikovem Windows računalniku (DESKTOP-SN494C0) s PowerShellom, Gradlom in JDK 8 — **sama poganja build, `verify-package` in scenarije v svetu**. Pred zagonom scenarija odstrani `NoDefaultCurrentDirectoryInExePath` (glej `OKOLJE.md`). Python ni nameščen (node je). Starejši opis (linuxova lupina brez Gradla, 17. 9.) velja za oblačne seje |

---

## 2. Napredek po milestonih (polna tabela, 2026-10-18)

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **zaključeno** | M0.1–M0.7 narejeno (+ M0.2r obnova okolja); M0.8 zabeležen kot blokada z opisanim vplivom |
| M1 Integriteta podatkov | **zaključeno** | M1.1–M1.3, M1.5, M1.6 in M1.9 narejeni; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | **zaključeno 5. 10.** (razen M2.4r render, prestavljen v M5-K kot klientski harness) | M2.1–M2.7 preverjeni v svetu; **baseline M2.6 zelen 5. 10.** (3 ponovitve × 9 celic, B1–B3 in P0–P9) — [baseline](../meritve/baseline-2026-10-05.md) |
| M3 Jedro entitete | **v teku** — M3.1–M3.5 zaključeni (vzrok R1 potrjen, R1 popravljen pod stikalom, regresija zelena v načinih 0/1/2, hitbox po sestopu); M3.6 zaključen 5. 10. (D-020, `RwAttackPriority`), M3.7 zaključen brez spremembe, M3.8 zaključen (`RwHitbox`), M3.8b strežniško zelen 7. 10. (GUI v klientu odprt), **M3.9 zaključen 7. 10.** (Q15 odprto) | analiza narejena in **reprodukcija R1 obstaja**; glej R1 in R6 |
| M4 Gibanje | **M4.14a v kodi** (izven vrstnega reda, na zahtevo uporabnika) | R2 analiza narejena; formacije (M4.14) prevedene in testirane v simulaciji, v svetu še ne — [zasnova](../06-FORMACIJE.md), [scenarij](../scenariji/M4.14-formacije.md) |
| M5 Performance | **M5-S zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms)**; **M5-K.0 zaključen 16. 10., M5-K.1 zaključen 18. 10.** (baseline klienta + profil render niti: NPC-ji ~90 %, pokrivalo 19–26 %, imena 20 %, oči 10–14 %; nov K14, Q28); od D-033 (14. 10.) dva obvezna dela: M5-S (strežnik) in M5-K (klient, ni začet)**, od D-034 še modul **M5-V** (vidnost na daljavo; **M5-V.0 raziskava zaključena 14. 10.**); **M5.0 zaključen 8. 10.** (plan prepisan, D-030); **M5.10 zaključen 9. 10.** (`pathFollow` 50 % ticka boja-500 → M5.11 se izvede); **M5.11 zaključen 9. 10.** (S14b: boj-500 MSPT povp 8,73 → 5,88 ms, p50 5,8 proti baselineu 8,9; S14 odstranjen; privzeto 1 po D-032); **M5.12 zaključen 9. 10.** (S4: idle-500 p95 5,24 → 3,93 ms, p99 6,16 → 4,13; privzeto 1); **M5.13 zaključen 10. 10.** (S7: iskanje prejemnikov 55,42 → 0,12 µs, idle-500 MSPT povp 2,824 → 2,570 ms, p95 4,325 → 3,932, vse nad šumom; privzeto 1); **M5.9 zaključen 10. 10.** (nedosegljiva tarča: iskanje poti 51 % ticka proti 2,45 % v boju → M5.2/M5.6/S5 dobijo podlago); **M5.6/S5 zaključen 10. 10.** (−19 % MSPT v tem prizorišču, a sprememba obnašanja → privzeto 0, Q21); **M5.17/S14c zaključen 10. 10.** (−15 % MSPT pri nedosegljivi tarči, enakost 239,5 M primerjav, privzeto 1); **M5.14/S16 zaključen 10. 10.** (idle-500 −5,3 %, enakost 52,0 M primerjav, privzeto 1); **M5.15 zaključen 11. 10.** (coremod v CNPC jarju, transformer brez posega; dev dokazan, produkcijski zagon Q22); **M5.16 zaključen 14. 10.** (prvi poseg v vanilla bajtkodo: idle-500 MSPT povp 2,787 → 2,540 ms (−8,9 %), p95 4,194 → 3,867, 5 + 5 ponovitev z ločenimi razponi, enakost 945.162 primerjav / 0 neujemanj, boj brez regresije; privzeto 1); **S1 zaključen 6. 10.** (`RwTargetPrefilter`: idle-500 alokacija 342 → 21 MB/s, MSPT p95 41,9 → 9,96 ms, boj-500 nevtralen); **S2 zaključen 7. 10.** (način 2: p95 → 5,24 ms, µs/NPC 9,7 → 6,6, ticki >10 ms 370 → 6, boj nevtralen); **profil JFR 8. 10.** (nov vrstni red S14 → S4 → S7; S3 in S8 umaknjena) | del že pokrit z M1.3, glej meritve; razdelitev M5-S/M5-K v [09](../09-PERFORMANCE-RAZISKAVA.md) |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | ostanejo script API/hooki, uskladitev verzije CNPC, reload; [plan](../08-PREGLED-IN-PLAN.md) |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](../08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

---

## 3. Paketi po milestonih (M1, M2)

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
| M2.2 | Reprodukcija R1 (8 jahačev na 8 nosilcih + kontrolna skupina brez jahačev) | **zaključeno** — E1–E6 zelena, R1 reproduciran; [meritev](../meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| M2.3 | Reprodukcija R2 (leteči NPC in ovira) | **zaključeno 21. 9. — L1–L12 zelena v štirih fazah, R2 reproduciran in obhod izmerjen, P1 ovržen**. Štirje zagoni; odprt ostane samo R-P1b. [meritev](../meritve/2026-09-17-M2.3-R2-reprodukcija.md), [scenarij](../scenariji/M2.3-R2.md) |
| M2.5a | Pripis počasnih tickov: `SlowTicks` + merila S1–S4 v `rwdiag-run.ps1` | **koda in testi narejeni** (22 testov, prevedeno v seji 16. 9.); čaka na prvi zagon v svetu |
| M2.4 | Merilni scenariji 50 / 200 / 500 NPC-jev | **zaključeno 23. 9.** — P1–P7 zelena v vseh 9 celicah (preverba 20+60 s); [meritev](../meritve/2026-09-23-M2.4-obremenitve-preverba.md), [scenarij](../scenariji/M2.4-obremenitve.md). Render (M2.4r) odprt |
| M2.5 | Merilni protokol kot skripta | **zaključeno** — M2.5a (pripis počasnih tickov), M2.5b (izločitev autosave ticka) in M2.5c (protokol ponovitev) narejeni; vsi trije čakajo na zagon v svetu |
| M2.5b | Izločitev autosave ticka: `server.tick.ns.nosave` + merila S5–S7 | **koda in testi narejeni** (6 testov, prevedeno v seji); čaka na prvi zagon v svetu |
| M2.5c | **Protokol ponovitev**: zapis zagona (`meritve-lib.ps1`), združevanje N zagonov v svežem svetu in šumni pas (`ponovitve-run.ps1`), merila T1–T6 | **zaključeno 18. 9. — serija pognana, T1–T6 zelena**; tabela M2.7 ima razpon; [scenarij](../scenariji/M2.5c-ponovitve.md), [meritev](../meritve/2026-09-18-M2.5c-ponovitve-nav.md) |
| M2.7b | Več vzorcev za veličino 5 (µs na iskanje) | **pognano dvakrat 21. 9.; razdelitev na `prvi*`/`pon*` in `usSkupaj` delujeta, več vzorcev pa šuma ni zaprlo** — vzrok je stanje JVM-a med pometanji. Dodano **ogrevanje iskalnika** (`-OgrevalnihPometanj 2` + `rwdiag reset`) in merilo **N15**; N14 prepisan iz časovnega praga v strukturno invarianto. Čaka na ponovni zagon in serijo. [meritev](../meritve/2026-09-21-M2.7b-ogrevanje.md), [scenarij](../scenariji/M2.7-navigacija.md) |
| M2.6 | Baseline originala | **zaključeno 5. 10.** — 3 ponovitve × 9 celic, B1–B3 in P0–P9 zelena; [tabela](../meritve/baseline-2026-10-05.md), [strojni zapis](../meritve/2026-10-05-M2.6-baseline.json). Pred tem M2.6b (zaščita zagona) in popravek čiščenja (slay radij 120 → 2000, P0) |
| M2.7 | **Merila kakovosti navigacije** (šest veličin, izmerjenih na originalu) | **zaključeno 17. 9. — N1–N12 zelena, tabela obstaja**; [meritev](../meritve/2026-09-17-M2.7-navigacija-baseline.md), [scenarij](../scenariji/M2.7-navigacija.md). Vhodni pogoj za M4.10 je dopolnjen: potrebno je prizorišče z razdaljo čez `NpcNavRange` |

---

---

## 4. Odprta vprašanja za uporabnika — vsa (Q1–Q29), stanje 2026-10-19

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | **odgovorjeno 17. 9.** — uporabnik nima dostopa; zabeleženo kot blokada (M0.7 §6), ponovno odpreti pred M10 |
| Q2 | Kopija sveta s problematičnimi NPC-ji | M1.4 | zaprto — uporabnik je nima; paket odložen |
| Q3 | Konkretna pokvarjena clone JSON datoteka | M1.4 | zaprto — ne obstaja; paket odložen |
| Q4 | Katera nastavitev se vrne nazaj? | R9 | odgovorjeno — follower role, action `waiting` se po clone lahko vrne v `following`; minorno |
| Q5 | Pri R1 — jahač in nosilec sta oba CustomNPC, ali je eden vanilla mob (konj)? | M2.2 | **odgovorjeno 15. 9.** — oba sta CustomNPC |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | **odprto, a ne blokira M2.3** — scenarij meri samostojno letenje, kar je podlaga za oba primera; odgovor je potreben šele za obseg M4. **M4.0 (20. 10.):** priporočilo — M4 naredi samostojno letenje (Hover, Momentum za AI zmaje); vozilo, ki ga krmili igralec, ostane ločen paket po R1 in M4.6 (CustomNPC+ ga ima kot ločeno vlogo »mount«) |
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
| Q21 | Naj NPC, ki do svoje tarče **nima poti**, 20 tickov ne poskuša znova (S5, `RwPathNegCache=1`)? Dobitek v prizorišču nedosegljive tarče: MSPT **17,85 → 14,46 ms (−19 %)**, alokacija 292 → 135 MB/s, ticki > 25 ms 45 → 10. Cena: v 59 % preskočenih iskanj bi original dal delno pot, zato NPC do 20 tickov ne gre proti tarči (stoji, namesto da se drenja ob oviri); ko se pot odpre, reagira do 20 tickov kasneje. Ni enakovredno originalu, zato D-032 ne velja | M5.6, D-007 | **odločeno 16. 10.: da → D-037**, `RwPathNegCache` privzeto 1, M5.2 zavržen |
| Q22 | Naj se postavi minimalen **pravi Forge strežnik** (ne dev) za dimni test zapakiranega jarja? Od M5.15 je jar hkrati coremod in mod; dev okolje je deobfuskirano in reobfuskiranega jarja ne more naložiti, zato je produkcijska pot preverjena samo statično (manifest, razredi, odvisnosti coremoda). To je tudi vhod v M10 (release) | M5.15, M5.16, M10 | **odprto 11. 10., nujnost narasla 14. 10.**: od M5.16 jar **privzeto** (`RwMoveBoxes=1`) spreminja bajtkodo `net.minecraft.world.World`, in prav tisto, kar v produkciji ni preverjeno, so imena SRG. Če poseg v produkciji ne uspe, se obnašanje ne pokvari (transformer vrne iste bajte, vrata niso klicana, `/rwmove` pokaže `poseg=0`) — izgubi se samo korist. Priporočilo: da, pred M5.8 |
| Q23 | Vidnost na daljavo (M5-V): naj bo po sprejemu (M5-V.8) **privzeto vklopljena** (`RwFarView` 1 na strežniku, 2 na klientu)? Spremeni videz, zato po D-007 odloči uporabnik. Poleg tega: sme strežnik klientu razkriti pozicije NPC-jev do `view-distance` brez preverbe vidnosti (`RwFarLos=0`; na PvP strežniku je to "radar", imena in naslovi se ne pošljejo nikoli)? | M5-V, D-007, D-034 | **odločeno 14. 10.: da** → D-035 (strežnik `RwFarView=1`, klient `RwFarView=2` po sprejemu M5-V.8); del o preverbi vidnosti v chatu ni bil zastavljen → Q26 |
| Q24 | Kje boš NPC-je gledal na 24 chunkov: v **enem igralcu/LAN** (deluje takoj — integrirani strežnik naloži chunke do RD) ali na **dediciranem strežniku**? Tam mora operater nastaviti `view-distance=24` (vanilla cena nalaganja in ticka vseh chunkov, ne cena M5-V); NPC-ji onkraj naloženih chunkov (varianta C) bi brez LOD terena lebdeli v praznini | M5-V obseg | **odločeno 14. 10.: strežnik** → D-035; M5-V.1 meri na dediciranem strežniku pri `view-distance` 10 in 24 (v 1.12 ni ločene razdalje simulacije: 5,4× več tickajočih chunkov — vanilla cena); varianta C ostane zavržena |
| Q25 | Vrstni red M5-V: dobesedno "na koncu M5-K" (po K5, K12, K13, K10) ali **takoj po M5-K.1 in K5**? M5-V ne potrebuje K13/K10, stopnja L2 pa zmanjša vrednost K10 (instancing ostane le za gnečo blizu) | M5-K/M5-V vrstni red | **odločeno 14. 10.: po priporočilu** → D-035; M5-K.0 → M5-K.1 → K5 → M5-V → K12 → ostali; K13/K10 po M5-V.8 ponovno oceniti |
| Q27 | Obseg potrditev M5.8 (K3): po polnem protokolu ostane ~14 h stroja (boj in nedosegljiva pri 25 ms ~4 h, pari pri 50 ms ~8 h, `-Razprseno` ~2 h). Predlog skrajšave: (a) potrdi samo N tik pod pragom, (b) mejo 50 ms vzemi iz lestvice, (c) okno 60 + 180 s (S-idle 3500 je imel razpon p95 4 %) — skupaj ~2–3 h. Polni protokol ali skrajšan? | M5.8 izhodni kriterij | **odločeno 16. 10.: skrajšan** — (a) + (b) + (c); zaganjalnik `audit/m58-skrajsano.ps1` (S-boj 1500, O-boj 1000, S-ned 700, O-ned 500; 3 ponovitve, 60 + 180 s) |
| Q28 | Vrstni red M5-K po profilu M5-K.1: D-035 določa M5-K.1 → **K5** (LOD, spremeni videz daleč) → M5-V. Profil pokaže tri cenejše pakete **brez spremembe videza**, ki pomagajo tudi blizu igralca, kjer K5 ne seže: **K14** pokrivalo v enem listu (19–26 % render niti), **K4** predpomnilnik napisov (20 % pri 200 NPC-jih v 22,6 bloka), **K3** oči (10–14 %). Naj gredo **K14 → K4 → K3 pred K5** (priporočeno; M5-V se zamakne za ~3 seje) ali ostane D-035 (K5 takoj)? | M5-K vrstni red | **odločeno 19. 10.: da** — K14 → K4 → K3 → K5 → M5-V (dopolni D-035) |
| Q29 | K14 (en list pokrivala) je zavržen: ista slika, a brez dobitka, ker je čas v geometriji (816 oglišč na NPC), ne v klicih. Brez 6 delov pokrivala je idle-500 p50 −23 %, 41 % od 239 skinov CNPC pa nima nobenega neprozornega teksla pokrivala (tudi skin fixture). Naj se pred K4 vstavi **K14b**: list pokrivala na teksturo, ki izpusti dele in trakove samo s prozornimi teksli (ista slika, dokaz s primerjavo pikslov, nato D-032)? Tveganje: OptiFine/senčilniki brez alfa testa bi prozorne fragmente pisali v globino | vrstni red M5-K (Q28), K14b, K4 | **odločeno 19. 10.: K14b pred K4** (uporabnik: "nared kar misls da bi blo bolje"); K14b narejen, vklop → Q30 |
| Q30 | **K14b** je v kodi (`RwHeadwearSkip`, privzeto 0): pokrivalo ne riše štirikotnikov, ki vzorčijo samo prozorne teksle. idle-500 p50 **11,49 → 8,60 ms (−25 %)**, idle-200 6,52 → 5,32 (−18 %), 3 + 3 ponovitve, razponi ločeni; preverba pikslov 7 serij × 200 NPC-jev (skin, vzorec, 5 pravih skinov) **0 razlik** barve in globine. Ni pa *dokazano* enako: fragment točno na robni črti štirikotnika lahko v originalu vzame sosednji neprozoren teksel (enopikselni rob klobuka); strogo enaka različica (z robom) ne da dobitka. Naj bo **privzeto 1** (priporočeno; razlika, če obstaja, je posamezen piksel na robu klobuka) ali ostane 0? | M5-K K14b, privzete nastavitve | **odločeno 21. 10.** — uporabnik: »Ja, Q30: K14b (RwHeadwearSkip=1) naj bo privzeto vklopljen. Zapisi odlocitev.« → `RwHeadwearSkip` privzeto 1, D-049 |
| Q31 | **Obseg nastavitev letenja (R2, po raziskavi M4.0).** Plan M4 naredi načine *original* / *Hover* / *Momentum* (`RwFlightMode`). CustomNPC+ ima ob tem še nastavitve, ki jih R2 ne omenja: **višinska meja** nad tlemi, **spust na tla, ko NPC nima poti** (danes leteči NPC obvisi v zraku), **hoja, ko je NPC tik nad tlemi**, ločena **hitrost dviganja**. Naj M4 doda (a) samo višinsko mejo in spust brez poti v načinu Hover (priporočeno: brez spusta lebdeči NPC po koncu poti visi, kar je del pritožbe »kinda ass«), (b) vse štiri, ali (c) nobene (samo trije načini)? | M4.2, M4.3, obseg R2 | **odprto 20. 10.**; priporočilo (a); ne blokira M4.16, M4.10, M4.17 |
| Q32 | **K5 L1** (`RwLodStart`): NPC dlje od praga brez oči, s ploščatim pokrivalom modela in brez imena. idle-500 pri 32 blokih (60,6 % NPC-jev daleč) p50 −24 %, idle-200 brez spremembe. Ker spremeni videz oddaljenih NPC-jev: privzeto 0 ali vklop (npr. 32)? | M5-K K5, privzete nastavitve | **odločeno 21. 10. samostojno (seja 102, pooblastilo lastnika) → D-039: privzeto 0**, vklop po izbiri uporabnika ([zapis](../meritve/2026-10-21-M5-K-K5-lod-L1.md)) |
| Q33 | **K12** (`RwRenderBox`): okvir upodobitve objame model, NPC brez hitboxa in ležeč NPC dobita domet stoječega. Ni dokazano enako (nariše se več NPC-jev), A/B idle-500 v šumu, K18 dokaže, da original odreže do 0,75 bloka modela. Privzeto 0 (D-007) ali 1? | M5-K K12, privzete nastavitve | **odločeno 22. 10. (samostojno po pooblastilu lastnika, seja 115): 1** → D-052 — popravek vidne napake brez izmerjene cene; originalno narisan NPC ni nikoli izločen; 0 vrne original |
| Q34 | **Ali je M5 zaključen po M5-K.2?** Klientski del izhodnega kriterija (p95 in 1 % low pri 200/500, original proti vsem sprejetim stikalom) je izpolnjen, a funkcionalna matrika `PLAN_IMPLEMENTACIJE.md` §6 in primerjava timerjev/script trace z referenco od M5 nista bili ponovljeni na kodi s privzetimi stikali | zaprtje M5, vrstni red (M4) | **odločeno 22. 10. (samostojno po pooblastilu lastnika, seja 118): ne še** → D-054 — klientski del M5 zaprt; M5 se zapre po regresijskem paketu **M5.Z** (`matrika-run.ps1` + primerjava M2.7), ker je izhodni kriterij konjunkcija in od 17. 9. ni bilo prehoda matrike |
| Q35 | Kako izvesti M5.Z z dejanskimi privzetimi stikali kode, ne da bi spremenili merilni config z ničlami? | veljavnost regresije M5.Z | **odločeno 22. 10. samostojno (seja 119) → D-055:** `ConfigDefaults` zajame statična `@ConfigProp Rw*` pred configom; `rwprivzeto=1` jih po nalaganju obnovi, 0 samo izpiše. Matrika potrdi način v vseh svežih logih. Config ostane isti; 23 polj in 12 razlik potrjenih v obeh scenarijih |
| Q36 | Katera referenca dokazuje timer/script trace, če M2.7 meri navigacijo in zgodovinske timer sledi ni; ali B3 popraviti v M5.Z? | merila M5.Z, zaprtje M5 | **odločeno 22. 10. samostojno (seja 119) → D-055:** napačno oznako M2.7 popravimo, nov scenarij M5.Z primerja isti popravljeni jar s stikali 0 proti privzetim vrednostim kode (to ni nespremenjeni distribucijski jar). B3 ohranimo in dokumentiramo: 19 razmikov po 8 tickov v obeh krakih. Sledi 97/75 dogodkov identični, matriki zeleni → M5 zaprt v izvedeni pokritosti, release matrika M10 in zunanje omejitve ostanejo odprte |
| Q26 | M5-V na strežniku: nadomestki razkrijejo pozicije NPC-jev do `view-distance` tudi za zidovi (imena in naslovi se ne pošljejo nikoli). Naj bo `RwFarLos` privzeto **0** (brez preverbe vidnosti, najcenejše) ali **1** (strežnik pošlje samo NPC-je v vidni črti igralca, raytrace ~1 Hz na par igralec–NPC)? | M5-V.2, M5-V.8 | **odločeno 14. 10.: po priporočilu, privzeto 0** → D-035; operater lahko vklopi `RwFarLos=1` |
| Q14 | Pri R10 — kateri zemljevid: lasten iz `ladja_mod` (izbira kot pri ladjah), JourneyMap, Xaero ali nov v CNPC? | M11.3 | **odprto 6. 10.**, ne blokira do M11 |

---

## 5. Znane omejitve


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
- **Delež v profilu ni dobitek manj klicev** (M5-K K14, 19. 10.): lastni čas v `nglCallList` vsebuje izvajanje
  vsebine lista v gonilniku (oglišča), ne samo klic. En list namesto 30 klicev pokrivala ni premaknil deleža (26,0 →
  26,1 %), izpustitev geometrije pa −23 %. Pred paketom »manj klicev« izmeri zgornjo mejo (izpusti del in izmeri).
- **Klientski A/B: miška in ne-java procesi** (M5-K K3, 19. 10.): testni klient prevzame miško; premik miške nad
  njegovim oknom zasuka kamero in zagon pade na K5 (6,9°). Med klientsko meritvijo se računalnika ne uporablja.
  Zaščita P9/K0/K8 vidi samo javo/gradle: zapuščena procesa `find /` sta dvignila p95 idle-200 s ~7 na ~12 ms.
  Pred A/B preveri procese z velikim CPU (`Get-Process | Sort-Object CPU -Descending`).

- **M5.Z (22. 10. 2026):** starost NPC-ja `getAge()` po restartu nadaljuje shranjeno vrednost, zato časovno omejena faza po restartu uporablja razliko od svojega `init`. Prvi sled zagon je pri starosti 252 prezgodaj končal fazo 2 v obeh krakih; po popravku so sledi 97/75 dogodkov enake. `rwprivzeto=0` pomeni config istega popravljenega jarja, ne distribucijskega originala. Celoten zapis: [M5.Z](../meritve/2026-10-22-M5.Z-regresija.md).
