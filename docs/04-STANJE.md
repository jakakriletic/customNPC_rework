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
| Zadnja posodobitev | **2026-10-29** (seja 162) — **M4.17 zaključen** po odločitvi lastnika (Q90): sprejet poskus 4 na `b8f98d28…` (11 ID-jev zelenih, 180/180, obe cenovni meji), privzeto `RwFlyLineOfSight=0`. Ponovna validacija Q87/Q88 se ne izvaja. |
| Trenutni milestone | **M4 — Gibanje in navigacija** po D-038; raziskava M4.0 že opravljena 20. 10. **M5 zaključen 22. 10.**: strežnik M5.8, klient M5-K.2, daljna vidnost M5-V.8 in regresija M5.Z. M3 do M3.9 (GUI M3.8b odprt); M2 zaključen 5. 10. |
| Naslednji paketi | **M4.2** → M4.3 (Hover) → M4.5 (vogal, Q82) po D-038. M4.14d odložen (Q65/Q74; G DONE ≤ 246, obtičanja ≤ 6). Neodvisno in pred izdajo Q22 (pravi Forge/SRG), M3.8b in preverba nameščenih OptiFine/Entity Culling. PR `codex/m7-cnpc-integration` → `main` odprt. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **185** datotek `.java` v `src/patch` (28. 10.; +1 M4.17: `rework/nav/FlightFollow`), od tega **120** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `nav` 15, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **451 JUnit** metod `@Test` v **71** datotekah (28. 10.; M4.17 `FlightFollowTest` 17: +10 poskus 1, +3 poskus 2, +3 poskus 3, +1 poskus 4) + **40 primerjalnih testov originala**. Na 984dc27a… unit/build/package in vsi trije formacijski harness-i uspejo; FA sprejem M4.14b pade (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
| Odprti pojavi | nobenega blokirnega; Q84 (M4.17 + M4.10 zid 6/6 → 5/6, vzrok [H], ovira za privzeti vklop kombinacije); P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6, Q8, Q10, Q12, Q14 in Q31 odprta; specifična R9 forenzika odložena, ne blokirana |
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
| M4 Gibanje | **M4.0, M4.16, M4.16b, M4.10 in M4.17 zaključeni** | M4.17 sprejet 29. 10. po odločitvi lastnika (Q90), poskus 4, privzeto 0; naslednji M4.2. [Zapis](meritve/2026-10-26-M4.17-vidna-crta.md). M4.14b preverba brez FA sprejema (D-079), M4.14d odložen (Q65/Q74), F1–F12 odprti. |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 160 → 1, novejši zgoraj).*


### 2026-10-29 (162) — M4.17 zaključen po odločitvi lastnika

**Paket:** M4.17 (D-085, Q90) · **Stanje:** **zaključen**, sprejet pod stikalom, privzeto 0.
Lastnik je zahteval sprejem brez ponovne validacije 23 ID-jev (Q88). Dokaz je celotna serija
poskusa 4 na `b8f98d28…`: 11 obveznih ID-jev zelenih, letenje 180/180 enako poskusom 1–3,
patrulja p95 9,175 ≤ 10,0925 ms in boj 2,687 ≤ 2,8116 ms. `dev/src` se po 28. 10. 12:38 UTC ni
spremenil; nov odtis izvira iz popravka controllerja (OPS.9) in ID-ja `m417-mcp-cache-v1`.
Q87/Q88 zaprti, Q81 (privzeto 0), Q82 (vogal → M4.5) in Q84 ostanejo. Naslednji: M4.2.
[Zapis](meritve/2026-10-26-M4.17-vidna-crta.md).

### 2026-10-29 (161) — M4.17: obnova manjkajočega MCP artefakta pred validacijo

**Paket:** M4.17 (D-085, Q89) · **Stanje:** **v teku**, sprejem ni zaključen.
Controllerjev unit na `adf047b4…` je uspel, build pa pade pri `extractMcpMappings`:
za `de.oceanlabs.mcp:mcp_snapshot:20171003-1.12` ni offline predpomnilnika.
Pregled potrdi samo starejša imenika 20170908/20170927; zaklenjene različice ne menjamo.
[Zapis](meritve/2026-10-26-M4.17-vidna-crta.md).

**Q89:** controller naj prek obstoječega `dev.ps1` izvede `extractMcpMappings` brez
`--offline` (nov ID `m417-mcp-cache-v1`), nato vseh 23 obveznih ID-jev z običajnim
offline buildom. Katalog testov spremeni odtis, zato tudi unit potrebuje nov rezultat.
Ob neuspehu prenosa naj se pred meritvami razišče konkreten log. Meje in scenariji isti;
letenje 180/180 proti referencam, cena proti originalu istega odtisa (zgornji rob p95 × 1,1).
AI ni zaganjal testov/builda/Minecrafta in ni spreminjal kode, configa ali zaklepa okolja.
Q82/Q84 in privzeto 0 ostajajo; commit/push M4.17 čaka veljavno celotno validacijo.
Nepovezani runner, baseline in audit so ohranjeni.


