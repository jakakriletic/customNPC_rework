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
| Zadnja posodobitev | **2026-10-29** (seja 179) — **M6.10 zaključen, poskus 1** (Q181–Q183). Controller m610-template-v1 PASS na `53ca9c0d…`: 53 preverjanj, Java 8 IDE/CLI predloga, typed NPC z vključitvijo, trije negativni primeri z datoteko/vrstico/kodo. Produkcija/runtime ista; [dokazi in omejitve](meritve/2026-10-29-M6.10-predloga.md). |
| Trenutni milestone | **M6 — Scripting, M6.0–M6.10 zaključeni, naslednji M6.12** po D-090/Q92. JDK 8 JavaCompiler (Q141); Java za NPC hooke pri `ScriptLanguage=Java`, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M6.12 → M6.11 player/world → ostanek M7 → M10** po [Q92](10-OBSEG-V1.md). Uspešnih serij se ne ponavlja (Q90). Privzeto izklopljeno brez cenovne meje, največ 2 poskusa/paket. M4.15a potrditev 2000 nedosegljivih odložena; Q22/OptiFine/Entity Culling za release, M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **213** datotek `.java` v `src/patch` (M6.8: `TrustProbe`), od tega **143** rework (`script` 16) in 3 dostopni razredi v `net/minecraft/…`; build/package PASS na `36620163…`: **78** pričakovanih spremenjenih vnosov (77 razredov originala + manifest coremoda), 311 zapakiranih class datotek |
| Testi | **519** metod `@Test` v **82** datotekah; `testOriginal` **45** preverjanj. M6.9 m69-api-v1 5.661 preverjanj PASS na `0ff903b4…`; M6.8 unit/build/package in m68 27/27 PASS na `36620163…`, produkcija ista, brez ponavljanja (Q90/Q92). FA sprejem M4.14b ostaja neuspešen (D-079); smoke/protokol dodatno 20/16 trditev |
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
| M3 Jedro entitete | **zaključeno do M3.9, M3.8b GUI potrjen** | [M3.8b](meritve/2026-10-29-M3.8b-gui.md) 216/216, clone/restart in 18 PNG potrjeni; fizično gibanje igralca med dvema SOLID ostane nepokrito, celoten M3 izhodni kriterij ni potrjen. Q15 → D-028 |
| M4 Gibanje | **Paketi v1.0 obravnavani z omejitvami** | [M4.15c](meritve/2026-10-29-M4.15c-klient.md) GUI/restart zelen; M4.15a potrditev 2000 nedosegljivih odložena. Celoten M4 izhodni kriterij ni izpolnjen; M4.14b brez FA sprejema (D-079), M4.14d v2, F1–F12 odprti. |
| M5 Performance | **zaključeno 22. 10. (D-055)** | M5-S: kapaciteta M5.8; M5-K: sprejem M5-K.2; M5-V: sprejem M5-V.8 in privzeti vklop V.8b; M5.Z: obe matriki in sled zeleni. [Regresija](meritve/2026-10-22-M5.Z-regresija.md); nepokrita release preverjanja ostanejo M10 |
| M6 Scripting | **M6.0–M6.9 zaključeni** | [API reference](meritve/2026-10-29-M6.9-api.md) 5.661 preverjanj PASS, deterministični 49 tipov/16 hookov in prevedeni primeri (Q180); runtime isti, brez sandboxa; naslednji M6.10 |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 177 → 1, novejši zgoraj).*

### 2026-10-29 (179) — M6.10: Java 8 IDE predloga

**Paket:** M6.10 (Q181–Q183) · **Stanje:** zaključen, poskus 1.
`scripts/java/template/`: Gradle Java/IDEA/Eclipse, dejanski API classpath, isti compile-only
CLI in typed StarterNpc z vključitvijo. Controller **m610-template-v1 PASS na `53ca9c0d…`**:
53 preverjanj, IDE/CLI exit 0, negativni CLI exit 1 z `WrongHook/WrongType/BrokenCounterText.java:5`
in compiler kodami; razredi brez inicializacije, isti binder. Ukaz 14.470 ms kot podatek.
Produkcija/JS/Java runtime isti; brez ponavljanja M6 serij (Q90).
[Dokazi/omejitve](meritve/2026-10-29-M6.10-predloga.md). IDE uporabniška seja/Forge izvajanje
ni nov dokaz. Q92 usklajen; naslednji M6.12.

### 2026-10-29 (178) — M6.9: generator API reference

**Paket:** M6.9 (Q177–Q180) · **Stanje:** zaključen, poskus 1.
Generator iz javac API simbolov in istega hook registry pripravi API.json/API.md/HOOKS.md.
Izrecni v1 nabor/izključitve, generiki, dedovanje, event polja in 16 Java primerov;
odsoten Javadoc/imena so označena. Razvojni sourceSet ostane izven moda; runtime isti.
Controller m69-api-v1 **PASS na `0ff903b4…`**: 5.661 preverjanj, 49 tipov/515 članov,
16 prevedenih primerov, napačen tip zavrnjen z lokacijo; neodvisni popis in bajtna enakost
med JVM/obrnjenim odkrivanjem potrjena. Celoten ukaz 7.372,120 ms kot podatek.
[Dokazi in omejitve](meritve/2026-10-29-M6.9-api.md); produkcija/zapakirani mod isti,
brez Minecrafta ali izvajanja primerov. Q92 usklajen; naslednji M6.10, brez ponovitev.
