# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki ga nova seja prebere na začetku in ga vsaka seja na koncu posodobi.**
Če seja tega ne naredi, je naslednja seja slepa.

Od 2026-10-19 je datoteka **namerno kratka** (cilj ≤ ~25 KB), da jo nova seja prebere poceni.
Vse starejše je dobesedno v arhivu; po potrebi ga išči z `grep` (glej "Kje je kaj" na koncu in
`05-SEJA-PROTOKOL.md` §1 in §3). Tu ostanejo samo: stanje, milestoni, **zadnja 2 vnosa dnevnika**,
odprta vprašanja in odprti pojavi.

---

## Trenutno stanje

| | |
|---|---|
| Zadnja posodobitev | **2026-10-29** (seja 164) — **M4.3 zaključen, poskus 1 (D-087)**: vsi štirje sprejemni controller ID-ji zeleni na `3eba45ab…`; privzeto 180/180 = original, Hover H0–H4 tudi po restartu. Cena enkrat kot podatek (Q92), privzeto original; omejitve poti zapisane v [dokazilih](meritve/2026-10-29-M4.3-hover.md). |
| Trenutni milestone | **M4 — Gibanje in navigacija** po D-038; raziskava M4.0 že opravljena 20. 10. **M5 zaključen 22. 10.**: strežnik M5.8, klient M5-K.2, daljna vidnost M5-V.8 in regresija M5.Z. M3 do M3.9 (GUI M3.8b odprt); M2 zaključen 5. 10. |
| Naslednji paketi | **M4.15a/b/c → M3.8b → M6 → ostanek M7 → M10** po [v1.0 (Q92)](10-OBSEG-V1.md), ki prevlada nad D-038/03-FAZE. Vse zunaj seznama je v2 (tudi M4.2b/M4.5/M4.6, Q82/Q84). Pri privzeto 0 cena ni pogoj, ena meritev; največ 2 poskusa/paket. Q22 in preverba nameščenih OptiFine/Entity Culling ostanejo nepokriti release preverbi; M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **187** datotek `.java` v `src/patch` (29. 10.; +1 M4.3: `rework/movement/HoverMoveHelper`), od tega **122** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `movement` 2, `nav` 15, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** pričakovanih spremenjenih vnosov (70 razredov originala + manifest coremoda), PASS |
| Testi | **468 JUnit** metod `@Test` v **73** datotekah (29. 10.; M4.3 +9 zelenih `HoverMoveHelperTest`, 469 izvedenih primerov) + **40 primerjalnih testov originala**. M4.3 unit/build/package zeleni, nato štirje sprejemni ID-ji na `3eba45ab…`; vhodi uspešnih unit/build/package ostanejo isti (Q90/Q96). FA sprejem M4.14b ostaja neuspešen (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
| Odprti pojavi | nobenega blokirnega; Q84 (M4.17 + M4.10 zid 6/6 → 5/6, vzrok [H], ovira za privzeti vklop kombinacije); P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q8, Q10, Q12 in Q14 odprta; Q6/Q31 razrešeni v D-086. Specifična R9 forenzika odložena, ne blokirana |
| Omejitev orodij | Windows (DESKTOP-SN494C0), PowerShell, Gradle in JDK 8; **v avtonomni izmeni teste, build in Minecraft zaganja samo upravljalnik** (DIRECT OWNER INSTRUCTION). Seja ureja datoteke in zahteva test ID-je. Python ni nameščen (node je) |

Polna različica te tabele pred skrajšanjem (vključno s povzetki ~20 starejših sej v celici
"Zadnja posodobitev" in vrsticami Zagoni 23. 9., Ogrevanje, Šumni pas, Formacije):
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §1.

---

## Napredek po milestonih

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **zaključeno** | M0.1–M0.7 narejeno (+ M0.2r obnova okolja); M0.8 zabeležen kot blokada z opisanim vplivom |
| M1 Integriteta podatkov | **zaključeno** | M1.1–M1.3, M1.5, M1.6 in M1.9 narejeni; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | **zaključeno 5. 10.** (razen M2.4r render, prestavljen v M5-K) | M2.1–M2.7 preverjeni v svetu; baseline M2.6 zelen 5. 10. — [baseline](meritve/baseline-2026-10-05.md) |
| M3 Jedro entitete | **zaključeno do M3.9** | M3.1–M3.5 (R1 reproduciran, popravljen pod `RwMountSteering=1`), M3.6 (D-020, `RwAttackPriority`), M3.7, M3.8 (`RwHitbox`), M3.9 (7. 10.). Odprto: M3.8b GUI v klientu (z igralcem), Q15 → D-028 |
| M4 Gibanje | **M4.3 zaključen**; M4.0/M4.2/M4.16/M4.16b/M4.10/M4.17 zaključeni | [Hover dokazila](meritve/2026-10-29-M4.3-hover.md), nato M4.15a/b/c po Q92. Celoten M4 izhodni kriterij ni izpolnjen; M4.14b brez FA sprejema (D-079), M4.14d v2, F1–F12 odprti. |
| M5 Performance | **zaključeno 22. 10. (D-055)** | M5-S: kapaciteta M5.8; M5-K: sprejem M5-K.2; M5-V: sprejem M5-V.8 in privzeti vklop V.8b; M5.Z: obe matriki in sled zeleni. [Regresija](meritve/2026-10-22-M5.Z-regresija.md); nepokrita release preverjanja ostanejo M10 |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | ostanejo script API/hooki, uskladitev verzije CNPC, reload; [plan](08-PREGLED-IN-PLAN.md) |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 162 → 1, novejši zgoraj).*


### 2026-10-29 (164) — M4.3: Hover zaključen

**Paket:** M4.3 (D-087, Q93–Q97) · **Stanje:** **zaključen, poskus 1, privzeto original**.
`RwFlightMode=1` izbere `HoverMoveHelper`: hitrost navigatorja × MOVEMENT_SPEED,
približevanje, ustavljanje in yaw ≤20°/tick. Controller unit/build/package zeleni;
štirje sprejemni ID-ji zeleni na `3eba45ab…`, 180/180 median privzetega načina = M4.16,
H0–H4 potrjeni tudi po restartu 30 letečih. P 6/6 A/B/C, C/A 0,522; drsenje A/C 0.
Cena enkrat: patrulja/boj p95 8,913/2,425 ms (podatek po Q92, brez cenovne meje).
Ovire/domet in aktivno ponovno ukazovanje B ostanejo omejitve; celoten M4 ni sprejet.
[En zapis dokazil in odločitev](meritve/2026-10-29-M4.3-hover.md). Testov seja ni zagnala,
po njih se spreminja le dokumentacija. Naslednji M4.15a/b/c; nepovezane datoteke ohranjene.

### 2026-10-29 (163) — M4.2: shranjeni način letenja zaključen

**Paket:** M4.2 (D-086, Q6/Q31/Q91) · **Stanje:** **zaključen**.
`FlightMode` + root int `RwFlightMode` (0/1/2, original brez ključa); helperji ostanejo isti.
Controller `unit`, `build`, `package`, `m416-letenje-v1` zeleni na `d7a9f810…`; 71 pričakovanih
vnosov, vseh 180 median = M4.16, razpon 0. Osem novih testov pokrije NBT/clone/setter,
ne celotnega save/restart entitete ali fizike. [Dokazila in odločitve](meritve/2026-10-29-M4.2-nacin-letenja.md).
Q6: samostojni NPC-ji; Q31: samo načini brez dodatkov; Q91: root int po obstoječem vzorcu.
Naslednji M4.3 → M4.15a/b/c po Q92; Momentum/GUI M4.2b v2. Koda po testih nespremenjena,
uspešne serije se ne ponavljajo. Nepovezani controller, baseline, ZAZENI-BASELINE in audit ohranjeni.
