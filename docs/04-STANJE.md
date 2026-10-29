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
| Zadnja posodobitev | **2026-10-29** (seja 188) — **M10.1b zaprt brez sprejema** (Q223): oba kraka zelena po vrsticah, O = K padel samo na naključni barvi oči; zaključek O = K v M10.1c, klient M10.1d (Q224). [Zapis](meritve/2026-10-29-M10.1b-matrika.md). Prej M10.1a (Q220). |
| Trenutni milestone | **M10 — release** (v1 obseg Q92); M7 v1 zaključen (M7.0, M7.V, M7.9, M7.5; D-095); M6 Scripting v1 zaključen (M6.0–M6.12) po D-090/Q92. JDK 8 JavaCompiler (Q141); Java za NPC hooke pri `ScriptLanguage=Java`, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M10.1c (zaključek strežniškega O = K) → M10.1d (klient) → M10.5 rollback → M10.4 soak 4 h → M10.2 → M10.6 → M10.7** po [Q92](10-OBSEG-V1.md) in D-096 (~~M10.0~~ Q207–Q214; M10.1a/M10.1b zaprta brez sprejema Q220/Q223; M7 v1 zaključen, M7.8 v2). Uspešnih serij se ne ponavlja (Q90). Privzeto izklopljeno brez cenovne meje, največ 2 poskusa/paket. M4.15a potrditev 2000 nedosegljivih odložena; Q22/OptiFine/Entity Culling za release, M10.3 blokiran (Q1). |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **224** datotek `.java` v `src/patch` (M7.5: `rework/anim` +3 + `AnimationHooksProbe`), od tega **154** rework in 3 dostopni razredi v `net/minecraft/…`; build/package PASS na `e80e2658…`: **78** pričakovanih spremenjenih vnosov (77 razredov originala + manifest coremoda) |
| Testi | **534** metod `@Test` v **85** datotekah; `testOriginal` **45** preverjanj. M7.5 m75-api-v1 6.998 preverjanj, m75-hooks-on/off-v1 in m62-packaged-v3 PASS na `e80e2658…`; M7.9 m79-api-v1/m79-absent-v1 PASS na `1a2a2b75…`; M6.11 m611 12/12 PASS na `e3e30ca1…`; M6.8 unit/build/package in m68 27/27 PASS na `36620163…`, produkcija ista, brez ponavljanja (Q90/Q92). FA sprejem M4.14b ostaja neuspešen (D-079); smoke/protokol dodatno 20/16 trditev |
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
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | [M7.0 raziskava](raziskave/M7.0-animacije-integracija.md) (D-095); [M7.V zagon](meritve/2026-10-29-M7.V-entities-zagon.md) 37/37 klipov (Q199); [M7.9 `Animators`](meritve/2026-10-29-M7.9-animator-api.md) (Q202); [M7.5 hooki](meritve/2026-10-29-M7.5-animacijski-hooki.md) (Q206); v1 zaključen; M7.8 reload v2 |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | **M10.0 raziskava zaključena** (D-096); M10.1a/M10.1b nista sprejeta (Q220, Q223) | [raziskava](raziskave/M10.0-release-kandidat.md); naslednji M10.1c (O = K z barvo oči v paleti), nato M10.1d klient; M10.3 blokiran (Q1) |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 186 → 1, novejši zgoraj).*

### 2026-10-29 (188) — M10.1b: strežniške A/B vrstice (ni sprejet)

**Paket:** M10.1b (Q221–Q224) · **Stanje:** zaprt brez sprejema, oba poskusa porabljena (Q92).
Razsodba IN3/IA5 popravljena po Q221, IN6 maskira izpeljane `Trans[XYZ]` (Q222, padec poskusa 1). Poskus 2
`m101b-matrika-v2`: oba kraka 13/13 run, 9/9 verify, IN6 in IL6 OK, sled razsodb O = K enaka; padec samo na
O = K NBT zaradi naključne `Eyes.Color` (`nextInt(23)` v obeh jarih, Q223). M10.1c ponovi O/K tek z barvo
"v paleti" (`m101c-matrika-v1`), klientske vrstice M10.1d (Q224). [Zapis](meritve/2026-10-29-M10.1b-matrika.md).

### 2026-10-29 (187) — M10.1a: strežniška A/B matrika (ni sprejeta)

**Paket:** M10.1a (Q215–Q220) · **Stanje:** zaprt brez sprejema, oba poskusa porabljena (Q92).
Harness `m10-matrika-run.ps1` (krak O distribucijski original, krak K kandidat). Poskus 1 padel na
`spawnNPC(IWorld)` v originalu (Q219); poskus 2 `m101a-matrika-v2` v kraku O 11/13 OK, padca IN3 `NpcModelData`
(oči klona naključne v originalu) in IA5 (stara entiteta v overworldu isti tick) sta strožja od originala; K se ni
zagnal. Vse vrstice z istim harnessom in razsodbo O = K prestavljene v M10.1b (Q220).
[Zapis](meritve/2026-10-29-M10.1a-matrika.md). Naslednji M10.1b.
