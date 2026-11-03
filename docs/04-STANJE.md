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
| Zadnja posodobitev | **2026-11-03** (seja 217) — M11.7a pogodba mostu v1 sprejeta na poskusu 1 (Q361–Q363, D-103). Controller unit/build/package PASS na `05586c4b…`. [Dokazi/meje](meritve/2026-11-03-M11.7a-pogodba.md). |
| Trenutni milestone | **M11 — vojske in poveljevanje** (v2, Q295). v1.0 sprejet z omejitvami `UPORABNIKI.md` §6 (P1–P8 zaključeni); M10.3/IO4 ostaneta odprta (Q1). |
| Naslednji paketi | **M11 po [M11.0 §5.3](raziskave/M11.0-vojske-prek-zemljevida.md):** ~~M11.1~~ register + `RwArmy` + `/rwarmy` (Q303–Q308) → ~~M11.1b~~ dovoljenja (Q309–Q316) → ~~M11.2~~ grozdi/posnetek (Q317–Q323) → ~~M11.3~~ čete (Q324–Q329) → ~~M11.4~~ roj/drža (Q330–Q335) → ~~M11.8a~~ palica/GUI (Q340) → M11.5 dolg pohod (odložen v2, Q348) → ~~M11.6~~ ticket (Q349–Q356) → ~~M11.8b~~ dovoljenja GUI (Q360) → ~~M11.7a~~ pogodba v1 (Q361–Q363) → **M11.7b zemljevid** (Q346) → M11.9 → M11.10. Q90/Q92/Q250 veljajo; vsak paket posodobi `UPORABNIKI.md` in `CHANGELOG.md`. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **283** datotek `.java` v `src/patch`, od tega **206** rework (+ArmyMapMarker); build/package PASS na `05586c4b…` (475 class datotek, 85 pričakovanih sprememb originala). |
| Testi | **740** metod `@Test` v **106** datotekah (+10 `ArmyBridgeContractTest`). Na `05586c4b…` unit/build/package PASS; brez ponovitev GUI/gibanja (Q90). |
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
| M11 Poveljevanje prek zemljevida | **v teku** (v2, Q295) | M11.0–M11.4, M11.8a/b in M11.6 ticket (pod stikalom, Q355) zaključeni; M11.5 odložen v v2 (Q348); M11.7a pogodba v1 sprejeta (Q361–Q363); naslednji obvezni Xaero most M11.7b (Q346) |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 215 → 1, novejši zgoraj).*

### 2026-11-03 (217) — M11.7a: pogodba zunanjih mostov v1 (sprejet)

**Paket:** M11.7a (Q361–Q363, D-103) · **Stanje:** sprejet na poskusu 1.
Statični UUID klici v ArmyCommandService v1 zahtevajo server nit/prijavljenega igralca ter sveže pravice.
Immutable ArmyMapMarker kopira obstoječe grozde in stanje namere, Reply ima odsevna getterja;
interni/skriptni klici, kanal in tick nespremenjeni, `RwArmies` ostane 0.
Deset novih unit preverb zamrzne podpise in odsevno uporabo, podatke, pravice, dimenzije in fallback.
Dokumentirana meja zaupanja/omrežja; živi zunanji most je naslednji paket M11.7b po Q346.
Controller unit (+10)/build/package PASS na `05586c4b…`; 475 class datotek, 85 pričakovanih sprememb originala.
Živ most, offline/wrong-thread na pravem strežniku in produkcijska reobf meja še niso dokazani; cena M11.10.
Brez zagona workloadov iz seje; nepovezano delo ohranjeno. Naslednji obvezni M11.7b, M10.3 blokiran Q1.
[Pogodba](api/ARMY-BRIDGE-V1.md), [dokazi/meje](meritve/2026-11-03-M11.7a-pogodba.md).

### 2026-11-03 (216) — M11.8b: dovoljenja GUI (sprejet)

**Paket:** M11.8b (Q357–Q360, D-102) · **Stanje:** sprejet na poskusu 1; `RwArmies` ostane 0.
Pregledane in nadaljevane delne spremembe prejšnjega izvajalca. Zavihek Permissions uporablja M11.1b servis,
online/usercache seznam, predpono in strani, izbor vojsk s kljukicami ter vidi/upravlja none/selected/all.
Vsaka sprememba preveri palico/sveže pravice; zaporedni večizbor se ustavi ob zavrnitvi, brez rollbacka prejšnjih klikov.
Na `24828502…` unit (+9)/build/package, m118b-permissions-gui-v1 101 in m118a-gui-v1 97 PASS.
Potrjeni pravi deop, omejitev na lastne vojske, restart obeh dodeliteljev in bitna enakost registra pri 0; pet PNG pregledanih.
Meje: en pravi dev klient/dedicated, offline prejemnik, tri vidne vrstice s pomikom; tutorial delno prekriva vrh, cena M11.10.
Seja ni zaganjala workloadov; nepovezano delo ohranjeno. Naslednji M11.7a, nato M11.7b (Q346), M10.3 ostaja blokiran Q1.
[Merila/odločitve/meje](meritve/2026-11-03-M11.8b-dovoljenja-gui.md).
