# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki ga nova seja prebere na začetku in ga vsaka seja na koncu posodobi.**
Če seja tega ne naredi, je naslednja seja slepa.

Od 2026-10-19 je datoteka **namerno kratka** (cilj ≤ ~25 KB), da jo nova seja prebere poceni.
Vse starejše je dobesedno v arhivu; po potrebi ga išči z `grep` (glej "Kje je kaj" na koncu in
`05-SEJA-PROTOKOL.md` §1 in §3). Tu ostanejo samo: stanje, milestoni, **zadnja 2 vnosa dnevnika**,
odprta vprašanja in odprti pojavi.

---

## Trenutno stanje

**14. 11. — Premikanje konjenice s palico.** Pri ukazu za premik se pot in napredovanje
računata pri tleh pod konjem. Štirje konjeniki prečkajo stopnico in se ustavijo na ločenih
mestih. [Zapis](meritve/2026-11-14-command-staff-varied.md).

**14. 11. — Izbor Command Staff in Konjenik.** Krog med držanjem sledi terenu ali NPC-ju
pod merkom. Konjenik iz klona pride v sedlu, palica ga izbere in pošlje do cilja.
[Zapis](meritve/2026-11-14-command-staff-varied.md).

**14. 11. — Command Staff in Varied Commodities.** Ločena palica izbira talne NPC-je s krogom,
ureja njihove ukaze in jih pošlje na ločene položaje. Združeni JAR je izdelan za izdaji 21Nov19
in 31Mar23. [Zapis](meritve/2026-11-14-command-staff-varied.md).

**14. 11. — Army GUI in M15.** Global → Armies registrira vojsko brez ukaza.
M15 uporablja vgrajeno animacijsko jedro M16: mape bitij na strežniku, prenos modela,
teksture in klipov klientom ter izbira modela v Display. [Zapis](meritve/2026-11-14-armies-m15.md).

| | |
|---|---|
| Zadnja posodobitev | **2026-11-11** (seja 230) — M11.11 `freeforall` sprejet: 199/199 skozi progo z grlom po restartu, zaprti član edini blokiran, Xaero v0.1.10 nameščen (Q446). [Zapis](meritve/2026-11-11-M11.11-freeforall.md). |
| Trenutni milestone | **Omejitve `UPORABNIKI.md` §6 po Q407**: matrika P2 zaprta (Q436), naslednji soak heap (Q437). M13 zaključen pod stikali, M11.5 sprejet. v1.0 sprejet z omejitvami; M10.3/IO4 odprta (Q1). |
| Naslednji paketi | **Soak: rast heapa po full GC (P1, Q437)**, nato ostale omejitve `UPORABNIKI.md` §6 po Q407. M12 na koncu; M15 po odločitvi lastnika. Q90/Q92 veljata s spremembami Q407: brez meje poskusov ali proračuna testov, timeout ≤14400 s; kratki zapisi in privzeto izklopljena stikala brez cenovne meje. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **309** datotek `.java` v `src/patch`, od tega **229** rework. Build/package PASS na M11.11. |
| Testi | **827** testov v **116** razredih (826 metod `@Test`). Unit PASS na M11.11; klientska matrika PASS na `e22ef824…`. |
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
| M10 Release | **M10.1c primerjalno, M10.5 rollback in M10.2 baseline sprejeti** (Q225/Q236/Q242) | [M10.2](meritve/2026-10-30-M10.2-baseline.md) 9/9 brez alarma p95; [M10.5](meritve/2026-10-30-M10.5-rollback.md) 58/58, obnova 42 datotek; IN3/IA3/IS3/IL5/IL6 delni, M10.1a/b brez sprejema, M10.1d kot [P2 zelen](meritve/2026-10-30-P2-klientska-matrika.md) (Q436); celoten release odprt. M10.4 soak v2 (Q240). [M10.6](meritve/2026-10-30-M10.6-dokumentacija.md) dokumentacija zaključena (Q244). [M10.7](meritve/2026-10-30-M10.7-changelog.md) changelog zaključen (Q246). Obseg v1 izčrpan; M10.3/IO4 blokirana (Q1) |
| M11 Poveljevanje prek zemljevida | **M11.11 sprejet** (`freeforall`) | 199/199 razmetanih skozi progo z grlom po restartu, zaprti član edini blokiran ([zapis](meritve/2026-11-11-M11.11-freeforall.md)); Xaero v0.1.10 nameščen. M11.5 sprejet po Q407. Celoten izhod M11 (200 skozi nenaložen svet in živa mapa) ostaja nepokrit. |
| M13 Povezani NPC-ji | **M13.0–M13.5 zaključeni pod stikali** (v2, Q433) | Lastna polja (`RwLinkedOwnFields`), kategorije/undo/proračun, GUI, seznam palice, kloner (`RwLinkedGroups`); obe stikali privzeto 0. [M13.5](meritve/2026-11-04-M13.5-sto.md): 100 z eno izbiro in sprememba do vseh. Izhod pokrit z ločenimi scenariji, ne z enim skupnim. |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 227 → 1, novejši zgoraj).*

### 2026-11-11 (230) — M11.11: pohod `freeforall` brez čakanja (sprejet)

**Paket:** M11.11 (Q445, Q446) · **Stanje:** sprejet pod stikalom; `RwArmies` ostane 0. Interaktivna seja na zahtevo lastnika.
Q446: hibrid namesto »vsak svojo pot«: ena skupna pot po odsekih, vsak član hodi s svojo hitrostjo, lastno iskanje samo odrinjen, obtičal ali oddaljen član; mesto v obliki šele na cilju, blokiran ne zadrži nikogar.
`army-ffa-run.ps1` PASS (`m1111-ffa-2026-11-11-221824`): 199 razmetanih + 1 zaprt, zid z grlom 3, hrib, stebri, ribnik, restart pri 139 za zidom; 199/199 na mestu, zaprti edini blokiran, 0 skokov, razmik ≥0,83. Unit 827/827, build, paket PASS.
Cena `army-cost-run.ps1 -Cell ffa`: pohod p95 15,20 / idle 7,08 ms, 110 MB/s (čete M11.10: 10,75 / 6,42). Trije neuspeli teki zapisani (luknja v poti, `fill hollow`, razmik).
Xaero v0.1.11 (`0.6.1-armies`, gumb Free for all privzet, cilj na poljubni razdalji): 84 JUnit + reobf PASS, sinhroniziran v `ladja_mod`, nameščen v 7/7 mods mapah z backupom. Živa mapa ostane ročna.
M11.11b dolg pohod `army-ffa-far-run.ps1` PASS: 100/100 do cilja 600 blokov v negeneriranem svetu brez igralca, samo tiketi vojske (`freeforall` do 400 chunkov).
Meje: dev dedicated, ploska proga, brez nenaloženega sveta, klienta in boja med pohodom. Naslednje: M10.4 heap (229, v delu) in ostale §6. [Zapis](meritve/2026-11-11-M11.11-freeforall.md).

### 2026-11-04 (228) — P2: klientska matrika zelena (sprejet)

**Paket:** P2/M10.1d po Q407 (Q434–Q437) · **Stanje:** sprejet; vse štiri celice PASS na `e22ef824…`, `RwArmies` ostane 0.
Q434: harness restart pred offline mailom tudi na dedicated. Q435: napaka produkcije iz M11.8a, palica na strežniku je originalnemu klientu zaprla povezavo; dedicated jo zdaj registrira samo pri `RwArmies>=1`, shranjeni vnos se prezre. D-101 zožen.
`p2b-dedicated`, `-original-client`, `-original-server`, `-integrated`, `m118a-gui-v1` (98), `m118b-permissions-gui-v1` (102), unit/build/package PASS. Pet PNG pregledanih.
Meje: en dev par, privzeta stikala, ni pakiranega klienta (Q22); O klient pri 1 ni pognan na končni kodi. Palice se pri 1→0 na dedicated izgubijo.
Naslednji soak heap (Q437). M10.3 blokiran Q1. [Zapis](meritve/2026-10-30-P2-klientska-matrika.md).
