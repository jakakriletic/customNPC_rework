# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki ga nova seja prebere na začetku in ga vsaka seja na koncu posodobi.**
Če seja tega ne naredi, je naslednja seja slepa.

Od 2026-10-19 je datoteka **namerno kratka** (cilj ≤ ~25 KB), da jo nova seja prebere poceni.
Vse starejše je dobesedno v arhivu; po potrebi ga išči z `grep` (glej "Kje je kaj" na koncu in
`05-SEJA-PROTOKOL.md` §1 in §3). Tu ostanejo samo: stanje, milestoni, **zadnja 2 vnosa dnevnika**,
odprta vprašanja in odprti pojavi.

---

## Trenutno stanje

**18. 11. — M14.4b sprejet (Q545/D-109).** Lastnik je naročil vklop optimizacij:
`RwNpcInstancing=1`, `RwLodStart=32`, `RwCombatEntryCap=64`. Izrecne vrednosti v obstoječih
configih veljajo še naprej. M5.Z obe matriki 5/5 in sled PASS. Klient: bližnji K22
500/500, oddaljeni 437 L2 + 63 modelov, brez GL napak. [Regresija in meje](meritve/2026-11-18-M14.4b-privzeti-rezimi.md).

**18. 11. — M16.7c0 pogodba zaključena (Q542–Q544, D-108).** M15 spawn izgubi
kartice/jezik in rig klona; določeni ločen marker mape, tipni adapter, točni izvlečki,
konflikti in no-clobber zapis. Samo raziskava, produkcija/testni vhodi in odtis
`6fd26ee4…` nespremenjeni; nobenega novega testa ali meritve. Naslednji c1 disk/ukazi,
nato c2 živa vključitev; funkcija M16.7c še ni izvedena.
[Pogodba in merila](raziskave/M16.7c0-pogodba-mape-klona.md).

**18. 11. — M14.4a sprejet (Q539–Q541).** Vseh osem controller testov (24 tekov)
PASS na trenutnem `6fd26ee4…`. Idle-500 p95 9,9147 → 8,0180 ms (−19,13 %),
razpona ločena; druge celice brez regresije p95. Produkcija nespremenjena,
takrat `RwNpcInstancing=0`. Uspešnih testov ne ponavljaj (Q90).
[Dokaz in meje](meritve/2026-11-18-M14.4a-3plus3.md).
**18. 11. — M14.3b7 sprejet (Q529–Q532).** Vseh pet controller testov PASS na
`e6c8610f…`: CNPC MPM deli, tuj renderer/model, Classic/64×32 bitno na originalnem
izrisu; pravi MPM jar izoliran, NPC ob igralcu s krili ostane batch. 34/34 preverb,
PNG pregledani. En idle-500 par p95 −17,51 %. Sledi OptiFine/shader; `RwNpcInstancing=0`.
[Dokaz in meje](meritve/2026-11-18-M14.3b7-mpm-renderer.md).
**18. 11. — M14.3b6 sprejet (Q526–Q528).** Vseh pet controller testov PASS na
`2c433b7c…`: URL guard `skinType=2`, 14/14 preverb, štiri bitne URL rezerve in
dve običajni batch obnovi, 24 pregledanih PNG. En idle-500 par p95 −16,82 %.
Sledi MPM/neznani renderer, nato OptiFine/shader; `RwNpcInstancing=0`.
[Dokaz in meje](meritve/2026-11-18-M14.3b6-url.md).
**18. 11. — M14.3b5 sprejet (Q524/D-107).** Vseh 11 zahtevanih controller testov
PASS na `bb8346fa…`: stara formula ponovi globinsko napako, popravljena prestane
146 regresijskih slik, 212 živih slik in 825 sond (max globina 0, RGB max2/255),
3377 svetovnih sličic brez napačne poti ali GL napak. 895+51 JUnit, build/package
in shader regresije PASS; en idle-500 par p95 10,1121 → 7,9463 ms (−21,42 %).
Pregledanih 16 živih PNG, 3 K22 in kontaktni listi scene/kontekstov; brez novega
zagona. Produkcija/testi nespremenjeni; poznejši Forge časovni komentar v
`splash.properties` je razložen v zapisu. `RwNpcInstancing=0`; naslednji URL v prenosu.
[Pogodba in rezultati](meritve/2026-11-18-M14.3b5-hoja-boj.md).

**18. 11. — M14.3b4 sprejet (Q511–Q512).** `m143b-fault-v1`, unit, build in package PASS
na `34a102f6…`: 20/20 preverb, 6 bitno enakih A/B/A2 vrstic ob napakah compile/link/draw,
brez ponavljanja gradnje, originalni izris in obnova po reloadu. Vseh 605 zaščitenih vhodov
je še enakih. Vbrizgane napake na enem GPU, brez nove cene; `RwNpcInstancing=0`.
Sledi dejanska hoja/boj in preostala matrika Q500; širša M14.3/vklop ostajata odprta.
[Dokaz in meje](meritve/2026-11-18-M14.3b4-napaka-programa.md).

**18. 11. — M14.3b3 sprejet (Q507–Q510).** `m143b-l2-v3`, unit, build in package PASS
na `6ef1a126…`: živi prehod L1/L2 pri `RwNpcInstancing` 0/1/0 brez dvojnega ali
manjkajočega NPC-ja (620/620 sličic, 35/35 preverb), globinska vidnost in šest PNG
potrjeni. Produkcija nespremenjena. Sledi napaka programa in preostala matrika Q500;
širša M14.3/vklop odprta. [Dokaz in meje](meritve/2026-11-18-M14.3b3-prehod-L2.md).

**18. 11. — M14.3b2 sprejet (Q504–Q506).** Vsi štirje controller testi PASS na
`f03e496c…`: 36/36 preverb, 16 A/B/A2 vrstic M16/GUI/izrisa peke, posebne poti
bitno enake in običajni RGB max 1/255 znotraj Q475. Pregledanih 49 PNG;
604 zaščiteni vhodi ostajajo enaki tudi po unit/build/package. Produkcija
nespremenjena; brez nove cene ali ponavljanja uspešnih testov Q90.
Sledi M14.3b3 živi prehod L2, nato preostala matrika; širša M14.3/vklop odprta,
`RwNpcInstancing=0`. [Dokaz in meje](meritve/2026-11-18-M14.3b2-konteksti.md).

**18. 11. — M14.3b1 sprejet (Q503).** Vseh pet zahtevanih controller testov PASS
na `928e7110…`: 54/54 preverb, 25 slikovnih vrstic celotnega living rendererja,
vidno ime in bitne special rezerve. En idle-500 par p95 10,0943 → 8,0247 ms
(−20,50 %), brez rezerv/GL napak; 532 cenovnih vhodov še enakih.
Sledi M14.3b2 (M16, GUI/peka), nato ostale meje Q500; širša M14.3b/vklop
ostajata odprta, `RwNpcInstancing=0`. Uspešnih testov ne ponavljaj (Q90).
[Dokaz in meje](meritve/2026-11-18-M14.3b-celoten-izris.md).

**18. 11. — M14.3a sprejet (Q499).** Popravljene tuje vezave VAO/programa znotraj
sličice in sprostitev mrež/programa ob reloadu/izhodu tudi pri stikalu 0.
Vseh pet controller testov PASS na `c9704453…`: 895 JUnit, 28/28 native preverb,
10/10 bitno enakih slikovnih vrstic; idle-500 p95 10,0773 → 8,1998 ms (−18,63 %)
v enem paru, brez fallbackov/GL napak. Sledi širša živa matrika M14.3b;
native GL ni OptiFine/shader dokaz. `RwNpcInstancing=0`, M14.4 ostaja odprt.
Uspešnih testov ne ponavljaj. [Zapis](meritve/2026-11-18-M14.3a-GL-meja.md).

**18. 11. — M14.2a sprejet pod stikalom 0 (Q495).** Vseh pet controller testov PASS
na `7ef49d0c…`: K22 200/200, RGB max1/255, 3 PNG pregledani; idle-500 par
p95 10,2679 → 8,0747 ms (−21,36 %), brez rezerv/GL napak. Native GL matrike
in en izris na NPC ostanejo; sledi M14.3 združljivost, `RwNpcInstancing=0`.
Celoten M14 in privzeti vklop še nista sprejeta; uspešnih serij ne ponavljamo (Q90).
[Zapis](meritve/2026-11-18-M14.2-en-izris.md).

**18. 11. — M16.7b sprejet (Q485, Q486).** `clone.json` v mapi bitja sprejme berljivi format.
`unit`, `build`, `package` in `m167-creature-readable-v2` (29 preverb) PASS na `cb6e5e27…`.
Naslednji M14.2 instancing pod `RwNpcInstancing=0`; M16.7c po M14.
[Zapis](meritve/2026-11-18-M16.7b-mapa-bitja.md).

**16. 11. — M14.1b zavrnjen (Q480–Q483).** Na `37b6b476…` PASS `unit`, `build`, `package`,
ponovni dokaz delov (bitno enako) in `m141b-vbo-cost-pair-v2`. Idle-500 p95 10,02 → 18,71 ms,
p50 8,70 → 16,91 ms pri enakem GC; cena je sama oddaja 11 VBO izrisov/NPC namesto `glCallList`.
Po M14.0 §5 kandidat odstranjen (koda v `5d598d5`), produkcija vrnjena na M14.1a. Naslednji M14.2 instancing.
[Zapis](meritve/2026-11-16-M14.1b-vbo.md).

**16. 11. — M14.1a sprejet (Q476–Q479).** `unit`, `build`, `package` in
`m141a-baseline-v1` PASS na `07b888c3…`. GPU baseline potrdi vseh 93 primerjav
posameznih delov modela z bitno enakima RGBA/depth in preverjenimi GL stanji;
vseh šest PNG pregledanih. Sledi M14.1b, omejen VBO.
[Dokaz in meje](meritve/2026-11-16-M14.1a-baseline.md).

**16. 11. — M14.0 raziskava zaključena (Q473–Q475, D-106).** Pregled trenutnega izrisa,
K10/K13/K14/K3 in M16 dokazov; načrt omejenega VBO pred instanciranjem, fallback od začetka.
Samo dokumentacija, produkcija/testni vhodi na `72ad0c90…`, brez novih zagonov.
Naslednji M14.1a: nevtralen prenos in baseline. [Raziskava](raziskave/M14.0-izris-bliznjih-npc.md).

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
| Zadnja posodobitev | **2026-11-18** (seja 254) — M14.4b privzeti optimizacijski režimi (Q545/D-109). [Regresija](meritve/2026-11-18-M14.4b-privzeti-rezimi.md). |
| Trenutni milestone | **M14.4b privzeti vklop po Q545.** M16.7c0 raziskava zaključena, funkcija odprta do c1+c2. |
| Naslednji paketi | **M16.7c1: diskovni adapter/ukazi**, nato c2 startup/reload/spawn po [pogodbi](raziskave/M16.7c0-pogodba-mape-klona.md). M12 zadnji, M10.3 blokiran Q1. Q90/Q92: brez ponavljanja uspešnih serij na nespremenjeni kodi, pri privzeto 0 cena samo podatek, kratek zapis; obseg `10-OBSEG-V1.md`. Q407: brez meje poskusov/testnega proračuna, timeout ≤14400 s. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **360** datotek `.java` v `src/patch`, od tega **278** rework. Build/package M14.4b PASS, 90 pričakovanih sprememb. |
| Testi | **895** patched + **51** original testov v M14.4b JUnit rezultatu, **127** testnih datotek. |
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
| M16 Vgrajeno animacijsko jedro | **M16.7c0 pogodba zaključena** (Q542–Q544/D-108) | [Pogodba](raziskave/M16.7c0-pogodba-mape-klona.md); naslednji c1 disk/ukazi, nato c2 živa vključitev. Funkcija odprta. M16.7b sprejet; `RwAnimCore=0`, `RwReadableClones=1` (Q484). |
| M14 Izris bližnjih NPC-jev | **M14.4b sprejet** (Q545) | `RwNpcInstancing=1`. [3+3 dokaz](meritve/2026-11-18-M14.4a-3plus3.md): idle-500 p95 −19,13 %, ločena razpona, brez regresije p95 drugje. [Zaključna regresija](meritve/2026-11-18-M14.4b-privzeti-rezimi.md). |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 252 → 1, novejši zgoraj).*

### 2026-11-18 (254) — M14.4b: privzeti optimizacijski režimi

**Paket:** M14.4b (Q545/D-109). Lastnik je po poročilu naročil privzeti vklop.
Nove vrednosti: `RwNpcInstancing=1`, `RwLodStart=32`, `RwCombatEntryCap=64`.
L1 poenostavi videz za pragom, combat cap skrajša zgodovino zadetkov. Obstoječe
izrecne vrednosti configa imajo prednost. [Dokaz in meje](meritve/2026-11-18-M14.4b-privzeti-rezimi.md).
JUnit, build, paket, obe matriki, sled in oba klientska scenarija PASS.

### 2026-11-18 (253) — M16.7c0: pogodba mape klona

**Paket:** M16.7c0 (Q542–Q544, D-108) · **Stanje:** raziskava zaključena, izvedba odprta.
Obstoječi M15 spawn ponastavi rig in prepiše vse skripte v ECMAScript; neposredno
razpakiranje prve kartice zato ne izpolni tipnega roundtripa. Določeni ločen marker,
vse kartice/jeziki, točni izvlečki z inline rezervo, konfliktna pravila in varni novi cilji.
Zamrznjena merila korpusa ≥89, negativnih primerov, pravih ukazov ter živega reload/spawn.
Produkcija/testi/pogoji in controller odtis `6fd26ee4…` nespremenjeni, brez novih zagonov
ali meritev (Q90). Naslednji c1 disk/ukazi, nato c2 živa vključitev; celoten M16.7c še ni
izveden. M14.4b še čaka ogled lastnika. [Pogodba, viri in meje](raziskave/M16.7c0-pogodba-mape-klona.md).

