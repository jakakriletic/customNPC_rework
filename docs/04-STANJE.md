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
| Zadnja posodobitev | **2026-10-29** (seja 169) — **M6.0 raziskava zaključena (D-090, Q127–Q131)**. Koda/bytecode in primarni viri preverjeni, vsak paket kritično ocenjen. Samo dokumentacija, brez novih meritev in brez spremembe merilnih vhodov; [dokazi in meje](raziskave/M6.0-java-scripting.md). |
| Trenutni milestone | **M6 — Scripting, M6.0 zaključena, implementacija še ni začeta** po D-029/Q92. JDK 8 JavaCompiler prvi kandidat; tipizirani obstoječi API in Java adapter, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M6.1 prototip → M6.2–10 (M6.6 samo zaradi pravilnosti) → M6.12 → M6.11 player/world → ostanek M7 → M10** po [Q92](10-OBSEG-V1.md). M6.1 potrebuje jezik/diagnostiko/no-compiler, cold/warm podatke in typed API povezavo v dev ter zapakiranem Forge runtime. Brez zgodovinskih ponovitev; privzeto izklopljeno brez cenovne meje, največ 2 poskusa/paket. M4.15a potrditev 2000 nedosegljivih odložena; Q22/OptiFine/Entity Culling za release, M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **195** datotek `.java` v `src/patch` (29. 10.; M3.8b +1 diagnostična sonda), od tega **127** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 28, `entity` 13, `far` 21, `formation` 15, `movement` 2, `nav` 18, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; build/package PASS na `b2719356…`: **74** pričakovanih spremenjenih vnosov (73 razredov originala + manifest coremoda) |
| Testi | **476 JUnit** metod `@Test` v **74** datotekah + **40 primerjalnih testov originala**. M3.8b: unit/build/package in GUI/restart zeleni na `b2719356…`, 216/216. M4.15b/c pretekli PASS ostanejo veljavni za svoje odtise; brez ponavljanja letenj/cen (Q90/Q92). FA sprejem M4.14b ostaja neuspešen (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
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
| M6 Scripting | **M6.0 raziskava zaključena, implementacija ni začeta** | [Raziskava](raziskave/M6.0-java-scripting.md), D-090; naslednji M6.1, v1 obseg po Q92 |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 167 → 1, novejši zgoraj).*


### 2026-10-29 (169) — M6.0: raziskava Java scriptinga zaključena

**Paket:** M6.0 (D-090, Q127–Q131) · **Stanje:** zaključen z raziskavo po D-029.
Preverjena originalna koda/bytecode, stari profil in primarni viri; vsak M6 paket ocenjen.
JDK 8 JavaCompiler prvi kandidat, obstoječi typed API/handler in JS privzet; trusted-only,
cache bajtov z ločenim stanjem, M6.6 samo zaradi pravilnosti. Plan/zahteve usklajeni s Q92.
[En zapis dokazov, odločitev in meril](raziskave/M6.0-java-scripting.md).
Samo dokumentacija: koda/harnessi/configi/lock isti, testov ali Minecrafta nismo zaganjali
in novih meritev ni. M6.1 mora šele dokazati compiler/Forge povezavo in čase; ni trditve
o delujočem Java runtime ali prihranku. Naslednji M6.1; nepovezane datoteke ohranjene.

### 2026-10-29 (168) — M3.8b: GUI, clone in restart zaključeni

**Paket:** M3.8b (Q123–Q126) · **Stanje:** zaključen, poskus 2, fizika/podatkovna pogodba ista.
Vsi štirje controller testi PASS na `b2719356…`: unit/build/package in m38b-gui-v1,
147/147 + 69/69; wand/Display paketi, globalno 0/1, HasHitbox, clone in save/restart potrjeni.
Šest istih UUID po drugem JVM, vseh 18 PNG vizualno pregledanih, config/options isti.
Prvi GUI FAIL (`667b844b…`, spodnji rob 251 > 240) ostane reprodukcija; razmik 21 px
da spodnji rob 233 na istem zaslonu, brez spremembe meril, ločljivosti ali fizike.
[En zapis meril/dokazil](meritve/2026-10-29-M3.8b-gui.md).
Po PASS samo dokumentacija, brez ponovitev (Q90/Q92); naslednji raziskava M6.0 (D-029).
M3.9 gibanje igralca med dvema SOLID s tem še ni izmerjeno; nepovezane datoteke ohranjene.
