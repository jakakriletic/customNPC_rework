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
| Zadnja posodobitev | **2026-10-28** (seja 156) — **M4.17 poskus 1 izmerjen, ni sprejet** (D-085, Q79–Q84): pod `RwFlyLineOfSight` (privzeto 0) stikalo 0 = M4.16 (180/180), labirint se premakne (z M4.10 6/6 z enim `navigateTo`), vogal 0/6 (vzrok iskanje → M4.5, Q82), z M4.10 zid 5/6 (Q84); patrulja p95 v meji, boj p95 2,490 → 3,277 (+32 %) čez mejo (Q83); poskus 2 (unija pred koraki, enak izid) v kodi, čaka meritve. Prej (seja 155) — **M4.10 zaključen** (D-084, Q75–Q78): pod `RwPathResume` (privzeto 0) stikalo 0 = M4.16 (180/180); z nadaljevanjem faza A DF 0/6 → 6/6, Z 0/6 → 6/6, DW 6/6 pri 1200 tickih (pri 600 prepočasen, Q78), P enako, V/L 0/6; M2.7 brez poslabšanja. Prej (seja 154): koda M4.10. Prej (seja 153): **M4.14d poskus 2 zavrnjen** (D-083, Q74): G iskanj 18 → 4, obtičanja 3, a G DONE 288 > 246 in FA5 G 5 > 4; produkcija povrnjena na `cbad3c3`, M4.14d odložen. Prej (seja 152): **M4.16b zaključen** (D-082, Q69–Q71): 200 letečih v originalu, patrulja MSPT p95 8,651 (8,651–9,175) ms, `npc.us` 17,16; leteči boj p95 2,228 (2,228–2,294) ms, `npc.us` 7,62; L1/L2/P5 zeleni. Prej (seja 151): **M4.16 zaključen** (D-081, Q68): original na štirih progah z enim `navigateTo` 0/6, osvežen zid 6/6, drsenje po prihodu ≥ 1,76/2,38, hitrost iz `navigateTo` ne velja; razpon 0. Prej (seja 150): **M4.14d poskus 1 (TRACK) zavrnjen** (Q65): FA5 G formalno uspe, a obtičanja G 3–6 → 10–14 in G DONE 233–246 → 318/348; produkcija povrnjena na `cbad3c3`, M4.14d odložen do novega pristopa. Prej (seja 149): **M4.14b zaključen kot preverba, ne izboljšava** (D-079/Q62): na zamrznjenem 984dc27a… imajo vsi ID-ji D-078 rezultat; harness-i, unit, build in package uspejo, sledi popolne (trace-v1 1.664, recovery in navigator 8.960, `iskanj_izgub` G 0). Padejo samo FA1/2 (merilo skupnega cilja proti 8 mestom; 8/8 mest OK), FA4 G 7,571–8,592 / O 6,42 (geometrija kolone skozi vrata) in FA5 G 19 > 4 (člani ne sledijo skupni poti → M4.14d). Pragovi isti, F1–F12 odprti (ročno, uporabnik). |
| Trenutni milestone | **M4 — Gibanje in navigacija** po D-038; raziskava M4.0 že opravljena 20. 10. **M5 zaključen 22. 10.**: strežnik M5.8, klient M5-K.2, daljna vidnost M5-V.8 in regresija M5.Z. M3 do M3.9 (GUI M3.8b odprt); M2 zaključen 5. 10. |
| Naslednji paketi | **M4.17 poskus 2** (meritve: enak izid 180/180 kot poskus 1, cena boja ≤ original + 10 %) → M4.2 → M4.3 (Hover) → M4.5 odločitev (vogal, Q82) → (M4.14d odložen: poskusa 1 in 2 zavrnjena, Q65/Q74; meje G DONE ≤ 246, obtičanja ≤ 6) → Hover (D-038, raziskava M4.0 že narejena). Neodvisno in pred izdajo **Q22** (pravi Forge/SRG pot zapakiranega coremoda); M3.8b ter preverba OptiFine/Entity Culling v nameščenem okolju. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **185** datotek `.java` v `src/patch` (28. 10.; +1 M4.17: `rework/nav/FlightFollow`), od tega **120** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `nav` 15, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **444 JUnit** metod `@Test` v **71** datotekah (28. 10.; +10 M4.17 `FlightFollowTest`) + **40 primerjalnih testov originala**. Na 984dc27a… unit/build/package in vsi trije formacijski harness-i uspejo; FA sprejem M4.14b pade (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
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
| M4 Gibanje | **M4.0, M4.16, M4.16b in M4.10 zaključeni; M4.14b zaključen 26. 10. kot preverba (D-079)** | [dokazi](meritve/2026-10-22-M4.14b-formacije.md): FA1/2, FA4 G/O in FA5 G padejo z zapisanimi vzroki, pragovi isti; F1–F12 odprti. M4.14d poskusa 1 in 2 zavrnjena (Q65, Q74, [zapis 1](meritve/2026-10-26-M4.14d-sledenje-poti.md), [zapis 2](meritve/2026-10-26-M4.14d-poskus2.md)). M4.16 zaključen 26. 10. ([zapis](meritve/2026-10-26-M4.16-letenje.md)), M4.16b 26. 10. ([zapis](meritve/2026-10-26-M4.16b-leteci-200.md)). M4.10 zaključen 26. 10. (D-084, [zapis](meritve/2026-10-26-M4.10-nadaljevanje.md), pod stikalom, privzeto 0). M4.17 poskus 1 ni sprejet (Q83, [zapis](meritve/2026-10-26-M4.17-vidna-crta.md)); poskus 2 → Hover; M4.15 neodvisno |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 154 → 1, novejši zgoraj).*


### 2026-10-28 (156) — M4.17 poskus 1: vidna črta izmerjena, ni sprejeta (cena v boju)

**Paket:** M4.17 (D-085, Q79–Q84) · **Stanje:** **v teku** — poskus 1 izmerjen in zapisan,
poskus 2 (enak izid, nižja cena) v kodi. [Zapis](meritve/2026-10-26-M4.17-vidna-crta.md). AI ni
zaganjal testov/builda/Minecrafta.

Koda poskusa 1: nov `rework/nav/FlightFollow` (pravilo brez sveta), prepis
`isDirectPathBetweenPoints` v `RwPathNavigateFlying`, stikalo `RwFlyLineOfSight` (privzeto 0,
`/rwpath vidno`), `FlightFollowTest` +10; harness `letenje-run.ps1 -VidnaCrta`, `perf-run.ps1
-RwVidno`; ključ `RwFlyLineOfSight=0` ročno v `dev/run/config`. Na `75269123…` vseh 11 zahtevanih
zelenih: stikalo 0 = M4.16 (180/180); labirint 0 → 0,125 b/t (B 6/6, z M4.10 A 6/6); vogal 0/6
(pot se že v originalu konča pred krakom → iskanje, M4.5, Q82); z M4.10 zid 6/6 → 5/6 (Q84,
odprt pojav); patrulja p95 8,913 = original; **boj p95 2,490 → 3,277 (+32 %)** čez mejo → ni
sprejet (Q83). Poskus 2: `FlightFollow.sweep` najprej preveri unijo vseh korakov (ena preverba
namesto ~12), izid dokazljivo enak. Zahtevani: unit, build, package, `m416-samotest-v1`,
`m416-letenje-v1`, `m417-letenje-vid-v2`, `m417-letenje-vidnad-v2` (180/180 = poskus 1),
`m416b-leteci-v1`, `m417-leteci-vid-v2`, `m416b-letboj-v1`, `m417-letboj-vid-v2`.

### 2026-10-26 (155) — M4.10 izmerjen in zaključen

**Paket:** M4.10 (D-084, Q75–Q78) · **Stanje:** **zaključeno**, pod stikalom `RwPathResume`,
privzeto 0. [Zapis](meritve/2026-10-26-M4.10-nadaljevanje.md). AI ni zaganjal testov/builda/Minecrafta.

Seja 154 je napisala kodo (`PathResume`, `RwPathNavigateGround`, nov `RwPathNavigateFlying`,
`PathResumeTest` +12; 434 JUnit v 70 datotekah, 184 razredov, 119 v rework) in harness
`-Nadaljevanje`; na 8d2dc7f5… DW 0/6 pri 600 tickih (Q78: 48 blokov pri 0,066 b/t ne gre v fazo),
zato `-TrajanjeFaze` in ID-ja `m410-letenje-dolgo-*`. Na `b53a0638…` zeleni unit, build, package,
`m416-samotest-v1`, `m416-letenje-v1` (180/180 = M4.16), `m410-letenje-nad-v1` (DF/Z 6/6),
`m410-letenje-dolgo-orig-v1` (DF/DW `navig0` 0/6), `m410-letenje-dolgo-nad-v1` (DF 6/6 220/230,
DW 6/6 710/720), `m410-nav-orig-v1`/`-nad-v1` (12/12 `pot.*` enakih, µs v šumu),
`m414b-navigator-harness-v3`; `m414b-navigator-trace-v3` pade samo na FA kot D-079. Privzeto 0
(Q77). Naslednji M4.17.

---

