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
| Zadnja posodobitev | **2026-10-29** (seja 168) — **M3.8b zaključen (Q123–Q126)**. Vsi štirje zahtevani controller testi PASS na `b2719356…`, 216/216 trditev in šest istih UUID po restartu; vseh 18 PNG pregledanih. Poskus 2 odpravi odrezano vrstico z razmikom 21 px; prvi FAIL ostane reprodukcija, fizika ista. [Dokazila](meritve/2026-10-29-M3.8b-gui.md). |
| Trenutni milestone | **Naslednji M6 — Scripting, najprej raziskava M6.0** po D-029/Q92. Paketi M4 v1.0 obravnavani z zapisanimi omejitvami; M5 zaključen 22. 10., M3 do M3.9 z GUI M3.8b preverjenim 29. 10.; celoten M3 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M6.0 raziskava → M6 v1.0 → ostanek M7 → M10** po [v1.0 (Q92)](10-OBSEG-V1.md). M3.8b in M4.15b/c zaključeni; M4.15a potrditev 2000 nedosegljivih odložena. Q90: brez zgodovinskih ponovitev; zunaj seznama v2, privzeto 0 brez cenovne meje, največ 2 poskusa/paket. Q22 in OptiFine/Entity Culling ostanejo release preverbe, M10.3 blokiran (Q1). |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 166 → 1, novejši zgoraj).*


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

### 2026-10-29 (167) — M4.15c: GUI in save/restart zaključena

**Paket:** M4.15c (Q117–Q122) · **Stanje:** zaključen, brez spremembe produkcije M4.15b.
Vseh šest zahtevanih controller testov zelenih na `c91ad234…`: unit/build/package,
integrirano s knjižnico/brez nje 70/70 ×2, dedicated s knjižnico/klient brez nje 68/68.
Wand → AI → Movement → Backend, izbira 0/1, hoja/let/hoja in save/restart istega UUID
potrjeni; vseh 30 novih PNG vizualno pregledanih, vsebina panela berljiva.
Config/runtime/options isti; dedicated properties in ops obnovljeni, čisti izhodi.
Prejšnji FAIL ostanejo FAIL; Q119–Q121 popravijo dokazno kodo, ne sprejemnih meril.
[En zapis dokazil in omejitev](meritve/2026-10-29-M4.15c-klient.md).
Po uspešnih testih samo dokumentacija, brez ponovitev. Naslednji M3.8b po Q92;
M4.15a odložena potrditev ostane. Baseline, ZAZENI-BASELINE in audit izven commita.
