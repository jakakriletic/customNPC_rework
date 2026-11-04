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
| Zadnja posodobitev | **2026-11-04** (seja 222) — M13.2 sprejet na poskusu 1 pod stikalom (Q389–Q394): kategorije skupine, skripte kot nova kategorija, razveljavitev `.json_prev`, proračun prenosov in `/rwlinked`; `RwLinkedGroups` ostane 0. [Zapis](meritve/2026-11-04-M13.2-kategorije.md). |
| Trenutni milestone | **M13 — povezani NPC-ji** (v2, Q379): ~~M13.0~~, ~~M13.1~~, ~~M13.2~~, naslednji **M13.2b** (GUI, Q392). M11 zaključen z omejitvami 4. 11.; v1.0 sprejet z omejitvami `UPORABNIKI.md` §6 (P1–P8 zaključeni); M10.3/IO4 ostaneta odprta (Q1). |
| Naslednji paketi | **M11 po [M11.0 §5.3](raziskave/M11.0-vojske-prek-zemljevida.md):** ~~M11.1~~ register + `RwArmy` + `/rwarmy` (Q303–Q308) → ~~M11.1b~~ dovoljenja (Q309–Q316) → ~~M11.2~~ grozdi/posnetek (Q317–Q323) → ~~M11.3~~ čete (Q324–Q329) → ~~M11.4~~ roj/drža (Q330–Q335) → ~~M11.8a~~ palica/GUI (Q340) → M11.5 dolg pohod (odložen v2, Q348) → ~~M11.6~~ ticket (Q349–Q356) → ~~M11.8b~~ dovoljenja GUI (Q360) → ~~M11.7a~~ pogodba v1 (Q361–Q363) → ~~M11.7b zemljevid~~ (Q346/Q369) → ~~M11.9~~ robni primeri (Q370–Q373) → ~~M11.10~~ cena (Q374–Q378). **M13 (Q379):** ~~M13.1~~ lastna polja (Q380–Q388, [zapis](meritve/2026-11-04-M13.1-povezani.md)) → ~~M13.2~~ kategorije, razveljavitev, postopen prenos (Q389–Q394, [zapis](meritve/2026-11-04-M13.2-kategorije.md)) → **naslednji M13.2b** polja in gumb v *Global → Linked NPCs* s pravim klientom → M13.3 → M13.4; M12 na koncu (zahteva uporabnika), M15 po odločitvi lastnika. Q90/Q92/Q250 veljajo; vsak paket posodobi `UPORABNIKI.md` in `CHANGELOG.md`. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **287** datotek `.java` v `src/patch`, od tega **210** rework (+LinkedCategories, LinkedBudget, CommandRwLinked); build/package PASS na `ae658355…` (85 pričakovanih sprememb originala). |
| Testi | **768** metod `@Test` v **108** datotekah (+20 `LinkedCategoriesTest`). Na `ae658355…` unit/build/package PASS; brez ponovitev GUI/gibanja (Q90). |
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
| M11 Poveljevanje prek zemljevida | **zaključen z omejitvami 4. 11.** (v2, Q295) | M11.0–M11.4, M11.8a/b in M11.6 ticket (pod stikalom, Q355) zaključeni; M11.5 odložen v v2 (Q348); M11.7a pogodba v1 sprejeta (Q361–Q363); M11.7b most/build in 6 namestitev zaključeni (Q369); M11.9 robni primeri sprejeti (Q373); M11.10 cena izmerjena (Q378); **zaključen z omejitvami**, izhodni kriterij čez `NpcNavRange`/nenaložene chunke v2 |
| M13 Povezani NPC-ji | **M13.0–M13.2 zaključeni** (v2, Q379) | [M13.1](meritve/2026-11-04-M13.1-povezani.md) lastna polja pod `RwLinkedOwnFields` (privzeto 0, Q387); [M13.2](meritve/2026-11-04-M13.2-kategorije.md) kategorije, razveljavitev, proračun pod `RwLinkedGroups` (privzeto 0, Q389); naslednji M13.2b |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 220 → 1, novejši zgoraj).*

### 2026-11-04 (222) — M13.2: kategorije, razveljavitev in postopen prenos (sprejet)

**Paket:** M13.2 (Q389–Q394) · **Stanje:** sprejet na poskusu 1 pod stikalom; `RwLinkedGroups` ostane 0.
Pri 1 skupina prenese samo izbrane kategorije (privzeto vse razen skript, kar je original), skripte brez konzole, `/rwlinked undo` zamenja z `.json_prev`, največ `RwLinkedBudget` (50) prenosov na tick.
Pri 0 prvotna pot: brez ukaza, ključa, `.json_prev` in števcev; član brez poti obdrži dom (zapre omejitev M13.1).
Controller `m132-groups-v2` PASS (100 skriptnih preverb) na `8f1ad57c…`; unit/build/package PASS na `ae658355…` (nato samo skripta harnessa, Q394). v1 je bila napaka harnessa.
Cena: 200 članov v 80 tickih, največ 50/tick, ~91 µs/prenos; največji tick 68,3 ms ni pripisan prenosu, MSPT ni merjen.
Meje: brez igralca/GUI-ja (M13.2b, Q392); `advanced`/`transform` samo razvrstitev; pri `RwLinkedOwnFields=0` povezava še premakne dom člana s potjo; nalaganje chunka zunaj proračuna; `removeData` originala ne izbriše datoteke (Q393, ni popravljeno).
Noben workload zagnan iz seje; nepovezano delo ohranjeno. Naslednji M13.2b; M10.3 blokiran Q1.
[Dokazi in omejitve](meritve/2026-11-04-M13.2-kategorije.md).

### 2026-11-04 (221) — M13.1: povezani NPC-ji ohranijo svoja polja (sprejet)

**Paket:** M13.1 (Q380–Q388) · **Stanje:** sprejet na poskusu 2 pod stikalom; `RwLinkedOwnFields` ostane 0.
Pri 1 član s potjo ohrani dom, usmerjenost, pot in indeks poti, tudi po ponovnem nalaganju sveta; ime in statistike se še prenesejo.
Pri 0 reproducirano: usmerjenost 270 in indeks 0 od urejenega, dom na prvo točko lastne poti (ne dom urejenega; hipoteza M13.0 §2.1 popravljena, Q384).
`RwArmy` in `RwNavBackend` ostaneta članova v obeh načinih. Controller `m131-linked-v4` PASS na `9a23c60a…`; unit/build/package PASS na `d8a328b1…` (nato samo trditev harnessa, Q386).
v1, v2 in v3 so bile napake harnessa (Q383, Q384, Q386). Meje: brez igralca/GUI-ja, dva člana, člani brez poti niso preverjeni, vklop ne popravi že prepisanih vrednosti, besedilo `@ConfigProp` popravi M13.2 (Q388).
Noben workload zagnan iz seje; nepovezano delo ohranjeno. Naslednji M13.2; M10.3 blokiran Q1.
[Dokazi in omejitve](meritve/2026-11-04-M13.1-povezani.md).
