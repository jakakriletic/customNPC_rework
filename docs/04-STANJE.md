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
| Zadnja posodobitev | **2026-11-03** (seja 212) — **M11.4 sprejet na poskusu 1** (Q330–Q335). Vseh pet controller testov PASS na `92c32038…`: pasivni roj po restartu, pravi melee in vrnitev 8/8 ter regresija čet. Naslednji M11.8a. [Dokazi/meje](meritve/2026-11-03-M11.4-roj-drza.md). |
| Trenutni milestone | **M11 — vojske in poveljevanje** (v2, Q295). v1.0 sprejet z omejitvami `UPORABNIKI.md` §6 (P1–P8 zaključeni); M10.3/IO4 ostaneta odprta (Q1). |
| Naslednji paketi | **M11 po [M11.0 §5.3](raziskave/M11.0-vojske-prek-zemljevida.md):** ~~M11.1~~ register + `RwArmy` + `/rwarmy` (Q303–Q308) → ~~M11.1b~~ dovoljenja (Q309–Q316) → ~~M11.2~~ grozdi/posnetek (Q317–Q323) → ~~M11.3~~ čete (Q324–Q329) → ~~M11.4~~ roj/drža (Q330–Q335) → **M11.8a palica** → M11.5 dolg pohod → M11.6 ticket → M11.8b dovoljenja GUI → M11.7a/b zemljevid → M11.9 → M11.10. Q90/Q92/Q250 veljajo; vsak paket posodobi `UPORABNIKI.md` in `CHANGELOG.md`. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **273** datotek `.java` v `src/patch`, od tega **196** rework (nov `ArmySwarm`); build/package PASS na `92c32038…`, 1716 originalnih vnosov in 85 pričakovanih sprememb. |
| Testi | **699** metod `@Test` v **102** datotekah; novi `ArmySwarmTest` (13) samo v `test`. Na `92c32038…` unit/build/package, `m114-swarm-v1` (49 + JS 25/25 in 141/141) in `m114-companies-regress-v1` (50 + JS 52/52 in 41/41) PASS. Brez ponovitev (Q90). |
| Odprti pojavi | nobenega blokirnega; Q84 (M4.17 + M4.10 zid 6/6 → 5/6, vzrok [H], ovira za privzeti vklop kombinacije); P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q8, Q10 in Q12 odprta; Q14 odločen (Q296); Q6/Q31 razrešeni v D-086. Specifična R9 forenzika odložena, ne blokirana |
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
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | [M7.0 raziskava](raziskave/M7.0-animacije-integracija.md) (D-095); [M7.V zagon](meritve/2026-10-29-M7.V-entities-zagon.md) 37/37 klipov (Q199); [M7.9 `Animators`](meritve/2026-10-29-M7.9-animator-api.md) (Q202); [M7.5 hooki](meritve/2026-10-29-M7.5-animacijski-hooki.md) (Q206); prvotni v1 zaključen; P7.0 raziskava (D-098); [P7a strežniški hooki](meritve/2026-11-01-P7a-dnevnik-animacij.md) sprejeti (Q281); [P7b `blendTo`](meritve/2026-11-01-P7b-blendto.md) sprejet (Q286); [P7c reload](meritve/2026-11-01-P7c-reload.md) sprejet (Q291) |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | **M10.1c primerjalno, M10.5 rollback in M10.2 baseline sprejeti** (Q225/Q236/Q242) | [M10.2](meritve/2026-10-30-M10.2-baseline.md) 9/9 brez alarma p95; [M10.5](meritve/2026-10-30-M10.5-rollback.md) 58/58, obnova 42 datotek; IN3/IA3/IS3/IL5/IL6 delni, M10.1a/b/d brez sprejema; celoten release odprt. M10.4 soak v2 (Q240). [M10.6](meritve/2026-10-30-M10.6-dokumentacija.md) dokumentacija zaključena (Q244). [M10.7](meritve/2026-10-30-M10.7-changelog.md) changelog zaključen (Q246). Obseg v1 izčrpan; M10.3/IO4 blokirana (Q1) |
| M11 Poveljevanje prek zemljevida | **v teku** (v2, Q295) | M11.0–M11.4 zaključeni; [roj/drža](meritve/2026-11-03-M11.4-roj-drza.md) sprejeta na poskusu 1 (Q335): restart, passive in pravi boj/vrnitev 8/8; naslednji M11.8a palica |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 210 → 1, novejši zgoraj).*

### 2026-11-03 (212) — M11.4: roj/drža/vrnitev (sprejet)

**Paket:** M11.4 (Q330–Q335, D-100) · **Stanje:** sprejet na poskusu 1; `RwArmies` ostane 0.
Trajni Mode/Stance, neurejeni odmiki roja brez postrojevanja, move/stance z aktualnimi pravicami;
aktivni passive zavrne attack/revenge, engage začasno prepusti gibanje boju in zadrži konec čete do vrnitve.
Na `92c32038…` PASS: unit (+13), build, package, `m114-swarm-v1` (49 + JS 25/25 in 141/141),
`m114-companies-regress-v1` (50 + JS 52/52 in 41/41). Šest aktivnih pasivnih vzorcev, roj 8/8 po restartu,
razmik ≥2,138; AI ubije golema 100→0, član prehodi 5,906 bloka v boj, nato vseh 8 nazaj (razmik ≥1,948).
Stop sprosti pasivnost, pri 0 register bitno enak. Prvi neveljavni harness ne šteje poskusa (Q334);
popravek samo v harnessu, isti config/seed/merila. Omejitve: dev dedicated, ravno, ena četa;
naravno iskanje tarč, veččetni roj in drugi načini boja še brez dokaza, cena M11.10. Brez ponovitev Q90.
Naslednji M11.8a; M10.3 ostane blokiran Q1. [Dokazi/meje](meritve/2026-11-03-M11.4-roj-drza.md).

### 2026-11-03 (211) — M11.3: čete in trajen ukaz (sprejet)

**Paket:** M11.3 (Q324–Q329, D-099) · **Stanje:** sprejet na poskusu 2; `RwArmies` ostane 0.
`ArmyOrder` v registru, `ArmyCompanies`, `ArmyMarches`; `RwArmyCompany=8`, deljena pot z makro odmiki in omejitvijo napredka,
join za zaostale; `/rwarmy move/stop/status` in API z aktualnimi pravicami. Poskus 1 pade na oddaljenem join;
popravek uporablja pravo pot do vmesnega cilja ≤24 blokov (Q328), harness in merila nespremenjena.
Na `8582e946…` PASS: unit (+17), build, package, `m113-companies-v1` (50 + JS 52/52 in 41/41), `m111b-regress-v1` 68/68.
4 × 8, ena pot, restart med MOVING; vseh 32 fizično prispe, najmanjši razmik 1,870, največ 9,463 od cilja.
Pri 0 register bitno enak, brez ukaza/obnovljenih čet. Omejitve: dev dedicated, legion na ravnem, brez cene (M11.10),
klienta, dolgih poti, produkcijskih ticketov in boja. Naslednji M11.4. [Dokazi in meje](meritve/2026-11-03-M11.3-cete.md).
