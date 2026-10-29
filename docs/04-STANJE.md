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
| Zadnja posodobitev | **2026-10-29** (seja 166) — **M4.15b zaključen (D-089/Q116)**. Vseh sedem zahtevanih controller testov zelenih na `d71d75d4…`, 34 properties trditev in 97/97 v vsaki od treh strežniških pogodb. Po testih samo dokumentacija; naslednji M4.15c. [Dokazila](meritve/2026-10-29-M4.15b-gui.md). |
| Trenutni milestone | **M4 — Gibanje in navigacija** po D-038; raziskava M4.0 že opravljena 20. 10. **M5 zaključen 22. 10.**: strežnik M5.8, klient M5-K.2, daljna vidnost M5-V.8 in regresija M5.Z. M3 do M3.9 (GUI M3.8b odprt); M2 zaključen 5. 10. |
| Naslednji paketi | **M4.15c → M3.8b → M6 → ostanek M7 → M10** po [v1.0 (Q92)](10-OBSEG-V1.md). M4.15b GUI/pogodba zaključen, klient s klikanjem in save/reload sveta še odprt. M4.15a potrditev 2000 nedosegljivih/skupnega pomnilnika odložena. Brez zgodovinskih ponovitev (Q90); zunaj seznama v2, privzeto 0 brez cenovne meje, največ 2 poskusa/paket. Q22 in OptiFine/Entity Culling nepokriti release preverbi; M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **193** datotek `.java` v `src/patch` (29. 10.; M4.15b +3 originalni in +3 rework), od tega **125** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `movement` 2, `nav` 18, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; build/package PASS na `d71d75d4…`: **74** pričakovanih spremenjenih vnosov (73 razredov originala + manifest coremoda) |
| Testi | **476 JUnit** metod `@Test` v **74** datotekah + **40 primerjalnih testov originala**. M4.15b: properties/unit/build/package in vse tri strežniške pogodbe zeleni na `d71d75d4…`; 34 properties trditev, 97/97 ×3. Klient/reload M4.15c odprt; brez ponavljanja letenj/cen (Q90/Q92). FA sprejem M4.14b ostaja neuspešen (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
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
| M4 Gibanje | **M4.3 in M4.15b zaključena; naslednji M4.15c** | [M4.15b](meritve/2026-10-29-M4.15b-gui.md) pogodba zelena; M4.15a potrditev 2000 nedosegljivih odložena. Celoten M4 izhodni kriterij ni izpolnjen; M4.14b brez FA sprejema (D-079), M4.14d v2, F1–F12 odprti. |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 164 → 1, novejši zgoraj).*


### 2026-10-29 (166) — M4.15b: GUI izbira in strežniška pogodba zaključena

**Paket:** M4.15b (D-089, Q108–Q116) · **Stanje:** zaključeno, poskus 1 produkcije.
Osnovni prenos preverjen/potisnjen kot `599751f`; nato AI → Movement → Backend 0/1,
strežniško dejansko ozadje/razlog in legacy save brez spremembe izbire.
Vseh sedem zahtevanih controller testov zelenih na `d71d75d4…`: 34 properties trditev,
unit/build/package in 97/97 ×3 za library/global 1/1, 1/0, 0/1; config/runtime SHA isti.
Predhodna FAIL ostaneta FAIL; zaščita preveri iste surove properties zapise in obnovi
izvirne bajte pred končnim SHA. Privzeto vanilla, brez sprememb tick/gibalne kode.
[En zapis dokazil in omejitev](meritve/2026-10-29-M4.15b-gui.md). Naslednji M4.15c:
klikanje, svet save/reload in klient/dedicated povezava še niso potrjeni.
Po uspešnih testih samo dokumentacija; brez ponovitev, baseline/ZAZENI-BASELINE/audit ohranjeni.

### 2026-10-29 (165) — M4.15a: analiza izvedenih meritev, odložena potrditev

**Paket:** M4.15a (D-088, Q98–Q107) · **Stanje:** **raziskovalno obravnavan, lestvici neuspešni**.
Na nespremenjenem `f0606b11…` controller samotest in kompatibilnost zelena (18/18),
obe šestcelični lestvici exit 1 zaradi U1 pri 2000 nedosegljivih (881/800 <1000 tarč).
Ostalih 10 celic uspešnih; neuspešni par ni A/B referenca, sprejemni seznam ni zelen.
Q106: brez znižanja U1 ali ponavljanja; potrditev 2000 in skupne porabe pomnilnika odložena.
Q107: naslednji neodvisni GUI M4.15b/c, vanilla/Baritone 0/1, strežniško dejansko ozadje
in razlog fallbacka; opozorilo o odvisnosti cene od prizora, brez obljube kapacitete.
[En zapis pogojev, rezultatov in odločitev](meritve/2026-10-29-M4.15a-baritone.md).
Produkcija, harnessi in merilni vhodi po uspešnih testih isti; seja ni zaganjala testov.
Commit vsebuje paketni harness in dokumentacijo; nepovezani baseline, ZAZENI-BASELINE in audit ohranjeni.
