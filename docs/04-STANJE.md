# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki ga nova seja prebere na začetku in ga vsaka seja na koncu posodobi.**
Če seja tega ne naredi, je naslednja seja slepa.

Od 2026-10-19 je datoteka **namerno kratka** (cilj ≤ ~25 KB), da jo nova seja prebere poceni.
Vse starejše je dobesedno v arhivu; po potrebi ga išči z `grep` (glej "Kje je kaj" na koncu in
`05-SEJA-PROTOKOL.md` §1 in §3). Tu ostanejo samo: stanje, milestoni, **zadnja 2 vnosa dnevnika**,
odprta vprašanja in odprti pojavi.

---

## Trenutno stanje

**16. 11. — M16.5 cena izmerjena (Q462).** 200 idle rigov: p95 0/1 = 7,017/5,822 ms;
vseh pet zahtevanih testov PASS na `dce93d19…`. Naslednje M16.6 (berljivi kloni), `RwAnimCore` ostane 0.
[Zapis](meritve/2026-11-16-M16.5-cena-rig.md).

**16. 11. — Odstranjena skupina Linked NPC se ne vrne (Q459).** Pri `RwLinkedGroups=1` `removeData` izbriše datoteko skupine in `.json_prev`; `q459-remove-v1` PASS. Naslednje: M16 (Q444). [Zapis](meritve/2026-11-16-Q459-linked-remove.md).

**16. 11. — `ArmyApi.members` brez umirajočih (Q458).** Filter `isEntityAlive()` namesto `!isDead`; `q458-edges-v1` PASS. Naslednje: Linked `removeData` (Q393), nato M16 (Q444). [Zapis](meritve/2026-11-16-Q458-armyapi-members.md).

**16. 11. — Q84 zid odpravljen (Q457).** Pri `RwFlyLineOfSight=1` + `RwPathResume=1` proga zid 6/6 v vseh fazah (komolec po `WAIT`); stikali ostaneta 0. Naslednje: `ArmyApi.members` (Q454). [Zapis](meritve/2026-11-16-Q84-zid-komolec.md).

**16. 11. — M10.4 heap sprejet (Q452).** Rast heapa v soaku je vanilla `CombatTracker` v neskončnem
boju; `RwCombatEntryCap` (privzeto 0) jo pri 64 ustavi. Naslednje: ostale omejitve §6 (Q453), nato M16.
[Zapis](meritve/2026-11-04-M10.4-heap.md).

**14. 11. — Odrivanje konjenice.** Prekrivajoči se konji z NPC-jezdeci se razmaknejo
tudi v mirovanju. [Zapis](meritve/2026-11-14-command-staff-varied.md).

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
| Zadnja posodobitev | **2026-11-16** (seja 234) — M16.5 sprejet (Q462): obe celici 200 idle rigov in unit/build/package PASS na `dce93d19…`. [Zapis](meritve/2026-11-16-M16.5-cena-rig.md). |
| Trenutni milestone | **M16 — vgrajeno animacijsko jedro** (Q444): M16.5 zaključen z navedenimi mejami; obstoječega jedra 12.–14. 11. ne podvajamo. M10.3/IO4 ostajata blokirana Q1. |
| Naslednji paketi | **M16.6: berljivi kloni**, stari JSON združljiv, uvoz/izvoz brez izgub. Nato ostanek plana po Q444; M12 na koncu. Q90/Q92: brez ponavljanja uspešnih serij na nespremenjeni kodi, pri privzeto 0 cena samo podatek, kratki zapisi; Q407: brez meje poskusov ali proračuna testov, timeout ≤14400 s. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **342** datotek `.java` v `src/patch`, od tega **261** rework. Build/package PASS na `dce93d19…`. |
| Testi | **859** testov v zadnjem JUnit rezultatu, **122** testnih datotek. M16.5: unit (+3 `ConfigDefaultsTest`), build/package in obe celici PASS na `dce93d19…`. |
| Odprti pojavi | nobenega blokirnega; Q84 odpravljen 16. 11. (Q457); P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
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
| M16 Vgrajeno animacijsko jedro | **M16.5 zaključen, naslednji M16.6** (Q462) | Jedro 12. 11., dedicated dva klienta/reload/pozni vstop 14. 11.; [cena 200 idle rigov](meritve/2026-11-16-M16.5-cena-rig.md) izmerjena 16. 11. Privzeto `RwAnimCore=0`, brez trditve o bitni enakosti slik. |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 232 → 1, novejši zgoraj).*

### 2026-11-16 (234) — M16.5: cena 200 animiranih NPC-jev (sprejet)

**Paket:** manjkajoča celica M16.5 (Q460–Q462) · **Stanje:** zaključen, `RwAnimCore` ostane 0.
Na `dce93d19…` PASS unit (+3 testi), build/package in `m165-rig-200-off/on-v1`; controller zagona, brez ponavljanja (Q90).
200 idle vitezov: p50/p95 0 = 6,445/7,017 ms, 1 = 5,307/5,822 ms; 2.220.800 izrisov riga pri 1.
To je cena različnih izrisov, ne enake slike; pri 0 originalna veja kode, brez bitne primerjave slike. Dva klienta/reload/pozni vstop iz ločenega M15 teka 14. 11.
Naslednji paket M16.6: berljivi kloni brez izgub, stari JSON ostane berljiv. M10.3 blokiran Q1. [Zapis](meritve/2026-11-16-M16.5-cena-rig.md).

### 2026-11-16 (233) — Q459: odstranjena skupina Linked NPC se ne vrne (sprejet)

**Paket:** omejitev §6 po Q407/Q454 točka 2 (Q393, Q459) · **Stanje:** sprejet pod `RwLinkedGroups` (privzeto 0).
Pri 1 `removeData` izbriše `<skupina>.json` in `.json_prev`; pri 0 original (datoteka ostane, skupina se po restartu vrne).
Na `d5ffbe14…` PASS unit (+4 `LinkedRemoveTest`), build, package in `q459-remove-v1` (4 zagoni 0/0/1/1, 47 + 17 provera): Q393 reproduciran pri 0, popravljen pri 1.
Meje: samo strežnik, odstranitev iz skripte, en zagon. Naslednje: M16 (Q444). M10.3 ostane blokiran Q1. [Zapis](meritve/2026-11-16-Q459-linked-remove.md).
