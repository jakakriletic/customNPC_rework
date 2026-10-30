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
| Zadnja posodobitev | **2026-10-30** (seja 201) — **P6 GUI Original/Hover sprejet** (Q269–Q271): unit/build/package in `p6-gui-v1` PASS na `81e79f30…`, 137/137, clone/restart šestih UUID-jev; toast na PNG prekriva novi gumb (vizualna omejitev). [Zapis](meritve/2026-10-30-P6-flight-gui.md). |
| Trenutni milestone | **M10 — release** (v1 obseg Q92); M7 v1 zaključen (M7.0, M7.V, M7.9, M7.5; D-095); M6 Scripting v1 zaključen (M6.0–M6.12) po D-090/Q92. JDK 8 JavaCompiler (Q141); Java za NPC hooke pri `ScriptLanguage=Java`, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **Popravki omejitev v1.0 P1–P8** po [Q248](10-OBSEG-V1.md): P1 soak (zaključen z omejitvijo heapa) → P2 klientska matrika (zaključen z omejitvijo) → P3 `RwPathResume`/`RwFlyLineOfSight` (sprejet pod stikali) → P4 formacije (sprejet po Q257) → P5 Java GUI/classpath (sprejet) → P6 GUI `RwFlightMode` (sprejet) → **P7 naslednji:** animacije → P8 Baritone; vsak posodobi `UPORABNIKI.md` §6. Po P8 čakamo odločitev lastnika o izdaji (brez v2). Q90/Q92 veljata: uspešnih serij ne ponavljamo, največ 2 poskusa/paket. M10.3/IO4 blokirana (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **235** datotek `.java` v `src/patch`, od tega **158** rework; P6: `FlightGui` in `FlightGuiHarness`. Build/package PASS na `81e79f30…`: 370 razredov v jarju, **85** pričakovanih razlik originala (84 razredov + manifest). |
| Testi | **567** metod `@Test` v **88** datotekah; `testOriginal` **45** preverjanj. P6 unit/build/package in `p6-gui-v1` PASS na `81e79f30…` (79/79 + 58/58). Predhodnih uspešnih serij ne ponavljamo (Q90). |
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
| M4 Gibanje | **Paketi v1.0 obravnavani z omejitvami** | [M4.15c](meritve/2026-10-29-M4.15c-klient.md) GUI/restart zelen; M4.15a potrditev 2000 nedosegljivih odložena. Celoten M4 izhodni kriterij ni izpolnjen; M4.14b brez FA sprejema (D-079); P4 FA sprejet po Q257 ([zapis](meritve/2026-10-30-P4-formacije.md)), F1–F12 odprti. |
| M5 Performance | **zaključeno 22. 10. (D-055)** | M5-S: kapaciteta M5.8; M5-K: sprejem M5-K.2; M5-V: sprejem M5-V.8 in privzeti vklop V.8b; M5.Z: obe matriki in sled zeleni. [Regresija](meritve/2026-10-22-M5.Z-regresija.md); nepokrita release preverjanja ostanejo M10 |
| M6 Scripting | **v1 zaključen (M6.0–M6.12)** | [Player/world](meritve/2026-10-29-M6.11-player-world.md) m611 12/12, `IPlayer`/`IWorld` v API (Q191); [izbira jezika](meritve/2026-10-29-M6.12-izbira-jezika.md) 101 trditev (Q187); P5: Java v vseh skriptnih GUI-jih, `RwJavaClasspath=auto` (Q265); item/block/quest/dialog/faction API v2 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | [M7.0 raziskava](raziskave/M7.0-animacije-integracija.md) (D-095); [M7.V zagon](meritve/2026-10-29-M7.V-entities-zagon.md) 37/37 klipov (Q199); [M7.9 `Animators`](meritve/2026-10-29-M7.9-animator-api.md) (Q202); [M7.5 hooki](meritve/2026-10-29-M7.5-animacijski-hooki.md) (Q206); v1 zaključen; M7.8 reload v2 |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | **M10.1c primerjalno, M10.5 rollback in M10.2 baseline sprejeti** (Q225/Q236/Q242) | [M10.2](meritve/2026-10-30-M10.2-baseline.md) 9/9 brez alarma p95; [M10.5](meritve/2026-10-30-M10.5-rollback.md) 58/58, obnova 42 datotek; IN3/IA3/IS3/IL5/IL6 delni, M10.1a/b/d brez sprejema; celoten release odprt. M10.4 soak v2 (Q240). [M10.6](meritve/2026-10-30-M10.6-dokumentacija.md) dokumentacija zaključena (Q244). [M10.7](meritve/2026-10-30-M10.7-changelog.md) changelog zaključen (Q246). Obseg v1 izčrpan; M10.3/IO4 blokirana (Q1) |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 199 → 1, novejši zgoraj).*

### 2026-10-30 (201) — P6: GUI za način letenja (sprejet)

**Paket:** P6 (Q248, Q269–Q271) · **Stanje:** funkcionalno sprejet, poskus 1; Original ostane privzet.
Na 81e79f30… unit/build/package in p6-gui-v1 PASS: 79/79 + 58/58, šest istih UUID-jev po restartu,
kloni z novimi UUID-ji, Original/Hover/Momentum in ground↔flying potrjeni. AI → Movement ponudi
Fly: Orig/Hover samo za leteče; Momentum onemogočen in ohranjen, legacy save ne spremeni načina.
Vseh 10 PNG pregledanih; tutorial toast prekriva novi gumb, zato berljivost ni vizualno dokazana,
napis/meje in dejanski kliki so preverjeni s harnessom. Brez nove tick kode ali ponovitve po Q90.
Naslednji P7 animacije. [Zapis](meritve/2026-10-30-P6-flight-gui.md).

### 2026-10-30 (200) — P5: Java v vseh skriptnih GUI-jih in `RwJavaClasspath=auto` (sprejet)

**Paket:** P5 (Q248, Q263–Q268) · **Stanje:** sprejet na poskusu 2, privzeto prazno. Na 6531b341… zeleni unit/build/package,
`p5-handlers-v1` 19/19 in regresija `m62-packaged-v3`. Player/forge/block/door/item: Java tab = JS sled, vezava po imenu, napačen
tip dogodka → `hook.signature`. `auto`: viri nalagalnika + enkrat zapisan jar Minecraft/Forge razredov skozi Forgeovo verigo
(3.601 razredov, 1,98 s; restart reused 7 ms). Poskus 1 17/19: Forge razredi na disku obfuscirani (`amu`) → v jar (Q268).
Omejitve: coremodi/mixini modov ne, pravi modpack ne (Q1), projektil brez Jave. Naslednji P6. [Zapis](meritve/2026-10-30-P5-java-handlerji.md).

