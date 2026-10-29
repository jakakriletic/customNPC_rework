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
| Zadnja posodobitev | **2026-10-29** (seja 184) — **M7.9 zaključen** (Q200–Q202): refleksijski `Animators.of(npc)` / `IAnimator` z omejeno FIFO; m79-api-v1 28/28 in m79-absent-v1 10/10, m69 53 tipov/614 članov PASS na `1a2a2b75…`. [Dokazi](meritve/2026-10-29-M7.9-animator-api.md). Prej M7.V (Q199). |
| Trenutni milestone | **M7 — ostanek integracije animacij** (M7.0 raziskava zaključena, D-095); M6 Scripting v1 zaključen (M6.0–M6.12) po D-090/Q92. JDK 8 JavaCompiler (Q141); Java za NPC hooke pri `ScriptLanguage=Java`, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M7.5 animacijski hooki → M10** (D-095; ~~M7.V~~ Q199 in ~~M7.9~~ Q202 zaključena; M7.8 v2) po [Q92](10-OBSEG-V1.md). Uspešnih serij se ne ponavlja (Q90). Privzeto izklopljeno brez cenovne meje, največ 2 poskusa/paket. M4.15a potrditev 2000 nedosegljivih odložena; Q22/OptiFine/Entity Culling za release, M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **220** datotek `.java` v `src/patch` (M7.9: `rework/anim` 3 + `AnimationApiProbe`), od tega **150** rework (`script` 18) in 3 dostopni razredi v `net/minecraft/…`; build/package PASS na `1a2a2b75…`: **78** pričakovanih spremenjenih vnosov (77 razredov originala + manifest coremoda) |
| Testi | **525** metod `@Test` v **83** datotekah; `testOriginal` **45** preverjanj. M7.9 m69-api-v1 6.855 preverjanj, m79-api-v1/m79-absent-v1 PASS na `1a2a2b75…`; M6.11 m611 12/12 PASS na `e3e30ca1…`; M6.8 unit/build/package in m68 27/27 PASS na `36620163…`, produkcija ista, brez ponavljanja (Q90/Q92). FA sprejem M4.14b ostaja neuspešen (D-079); smoke/protokol dodatno 20/16 trditev |
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
| M6 Scripting | **v1 zaključen (M6.0–M6.12)** | [Player/world](meritve/2026-10-29-M6.11-player-world.md) m611 12/12, `IPlayer`/`IWorld` v API (Q191); [izbira jezika](meritve/2026-10-29-M6.12-izbira-jezika.md) 101 trditev (Q187); modpack classpath ročni (Q184); item/block/quest/dialog/faction v2 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | [M7.0 raziskava](raziskave/M7.0-animacije-integracija.md) (D-095); [M7.V zagon](meritve/2026-10-29-M7.V-entities-zagon.md) 37/37 klipov (Q199); [M7.9 `Animators`](meritve/2026-10-29-M7.9-animator-api.md) (Q202); naprej M7.5; M7.8 reload v2 |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 182 → 1, novejši zgoraj).*

### 2026-10-29 (184) — M7.9: refleksijski script animator

**Paket:** M7.9 (Q200–Q202) · **Stanje:** zaključen, sprejet v poskusu 1 na `1a2a2b75…`.
`Animators.of(npc)` / `IAnimator`, FIFO največ 32 na entiteto, šibke reference in poslušalec samo
ob čakajoči vrsti; originalni razredi in entities mod nespremenjeni. Šest unit primerov, API
generator in `animation-api-run.ps1` za JS/Java z dejanskim modom ter brez njega.
unit/build/package, m69-api-v1 (53 tipov/614 članov), m79-api-v1 28/28 (JS = Java FIFO
`hurt,idle,hurt`) in m79-absent-v1 10/10 PASS; getterji 1,41 µs/trojico (podatek).
[Dokazi/omejitve](meritve/2026-10-29-M7.9-animator-api.md). Naslednji M7.5 → M10.

### 2026-10-29 (183) — M7.V: zagon rework + entities 0.7.0

**Paket:** M7.V (Q198–Q199) · **Stanje:** zaključen, poskus 2, brez produkcijske kode.
Nov harness `m7v-entities-run.ps1` (pakiran dedicated strežnik, rework + `customnpcentities-0.7.0`).
Controller **m7v-entities-v2 PASS na `2905a99d…`**: 5 mobov, vseh 37 klipov predvajanih in
končanih (1.134 vzorcev, hitbox ves čas = definicija moba), save/stop/start ohrani UUID, razred,
hitbox in animacijski NBT; brez povezovalnih in nalagalnih napak. Poskus 1 lažno negativen
(INFO »missing mods«, Q199). [Dokazi/omejitve](meritve/2026-10-29-M7.V-entities-zagon.md).
Naslednji M7.9 → M7.5 → M10.

