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
| Zadnja posodobitev | **2026-10-30** (seja 190) — **M10.1d zaprt brez sprejema** (Q233): na `62940ab3…` prevod v2 PASS, vse 4 funkcionalne celice FAIL, nič PNG; oba poskusa porabljena, validacija v2. Produkcija ista, brez ponovitev; naslednji neodvisni M10.5 rollback po Q92. [Zapis](meritve/2026-10-30-M10.1d-klient.md). |
| Trenutni milestone | **M10 — release** (v1 obseg Q92); M7 v1 zaključen (M7.0, M7.V, M7.9, M7.5; D-095); M6 Scripting v1 zaključen (M6.0–M6.12) po D-090/Q92. JDK 8 JavaCompiler (Q141); Java za NPC hooke pri `ScriptLanguage=Java`, privzeto JS. Paketi M4 v1.0 obravnavani z omejitvami; celoten M3/M4 izhodni kriterij ni potrjen. |
| Naslednji paketi | **M10.5 rollback → M10.4 soak 4 h → M10.2 → M10.6 → M10.7** (M10.1d zaprt brez sprejema, validacija v2 Q233; M10.1c primerjalno sprejet Q225) po [Q92](10-OBSEG-V1.md) in D-096 (~~M10.0~~ Q207–Q214; M10.1a/M10.1b zaprta brez sprejema Q220/Q223; M7 v1 zaključen, M7.8 v2). Uspešnih serij se ne ponavlja (Q90). Privzeto izklopljeno brez cenovne meje, največ 2 poskusa/paket. M4.15a potrditev 2000 nedosegljivih odložena; Q22/OptiFine/Entity Culling za release, M10.3 blokiran (Q1). |
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
| M10 Release | **M10.1c O = K primerjalno sprejet** (Q225); M10.1a/b nista sprejeta | [M10.1c](meritve/2026-10-29-M10.1c-matrika.md) 13/13 + 9/9 na krak; IN3/IA3/IS3/IL5/IL6 delni; M10.1d zaprt brez sprejema (Q233), naslednji M10.5; M10.3/IO4 blokirana (Q1) |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 188 → 1, novejši zgoraj).*

### 2026-10-30 (190) — M10.1d: klientski harness

**Paket:** M10.1d (Q226–Q233) · **Stanje:** zaprt brez sprejema, validacija v2 (Q92).
Ločen klientski testni mod proti originalnemu API, brez spremembe produkcije. Na `62940ab3…`
controller prevod v2 PASS; integrated FAIL (`Missing Locked`), dedicated/original-server FAIL
(`Timeout JOIN`), original-client FAIL (`Timeout ACK:SCENE`). Povezave/prijave potrjene v logih,
vendar nobena celica ni dosegla offline/restart/verify, nič PNG; nobena nova vsebinska vrstica
ni sprejeta. Oba poskusa porabljena, brez popravka/ponavljanja/tretjega poskusa (Q90/Q92).
[Dokazi in omejitve](meritve/2026-10-30-M10.1d-klient.md). Harness in razsodba se arhivirata s commit/push;
celoten release sprejem ni potrjen. Naslednji neodvisni paket M10.5 rollback po D-096/Q92.

### 2026-10-29 (189) — M10.1c: strežniški O = K

**Paket:** M10.1c (Q225) · **Stanje:** primerjalno sprejet, poskus 1.
Controller `m101c-matrika-v1` PASS na `62d88a26…`: O/K 13/13 run + 9/9 verify, normalizirani NBT in sledi enaki,
zaščiteni vhodi nespremenjeni. Produkcija/harness ostaneta ista; uspešnega teka ne ponavljamo (Q90).
IN3/IA3/IS3/IL5/IL6 ostanejo delno pokriti (zlasti `unloaded=False` v obeh krakih ni unload dokaz in IL6 ne meri
save vrste); celoten release sprejem ni potrjen. [Zapis](meritve/2026-10-29-M10.1c-matrika.md).
Naslednji M10.1d klient po Q92/Q224; M10.3/IO4 blokirana (Q1).
