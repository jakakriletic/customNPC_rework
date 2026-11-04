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
| Zadnja posodobitev | **2026-11-04** (seja 225) — M11.5 sprejet (Q418): vseh devet controller testov PASS na `299d947a…`, dolg/kratek pohod in obe ticket nadaljevanji ARRIVED. Dokumentacija zaključena, brez ponovitev (Q90). [Zapis](meritve/2026-11-03-M11.5-dolg-pohod.md). |
| Trenutni milestone | **M11.5 zaključen pod stikalom**, M13.0–M13.3 zaključeni; **naslednji M13.4**. v1.0 sprejet z omejitvami; M10.3/IO4 odprta (Q1). |
| Naslednji paketi | **M13.4** kloner kot povezan, obvestilo in preverba z igralcem/nenaloženimi chunki → omejitve `UPORABNIKI.md` §6, odložene samo zaradi 2 poskusov (Q407). M11.5 sprejet Q418. M12 na koncu; M15 po odločitvi lastnika. Q90/Q92 veljata s spremembami Q407: brez meje poskusov ali proračuna testov, timeout ≤14400 s; kratki zapisi in privzeto izklopljena stikala brez cenovne meje. |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **301** datoteka `.java` v `src/patch`, od tega **222** rework (+1 v M11.5). Build/package PASS na `299d947a…`; 1716 originalnih vnosov, 87 pričakovanih sprememb. |
| Testi | **812** metod `@Test` v **113** datotekah (+17 v M11.5). Vseh devet sprejemnih testov na `299d947a…` PASS; short 50, long 70, swarm 49, teren 53, strict ticket 94 in parna cena 200. |
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
| M11 Poveljevanje prek zemljevida | **M11.5 sprejet po Q407** | 32 članov prehodi ≥200 z restartom, oba ticket resume in kratka noga 200 ARRIVED; devet PASS na `299d947a…` (Q418). Celoten izhod M11 (dolgi pohod 200 skozi teren/nenaložen svet in živa mapa) ostaja nepokrit. |
| M13 Povezani NPC-ji | **M13.0–M13.3 zaključeni** (v2, Q379) | Lastna polja (`RwLinkedOwnFields`), kategorije/undo/proračun in GUI (`RwLinkedGroups`), [M13.3 seznam palice](meritve/2026-11-04-M13.3-palica.md) sprejet Q411; obe stikali privzeto 0. M13.4 po M11.5 (Q407); celoten M13 izhod še ni potrjen. |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 223 → 1, novejši zgoraj).*

### 2026-11-04 (225) — M11.5: dolg pohod in popravek nadaljevanja (sprejet)

**Paket:** M11.5 (Q412–Q418, D-104) · **Stanje:** sprejet pod stikalom; `RwArmies` ostane 0.
Omejene prave poti ohranijo čete/pasove/smer; zadnji prehodni meter vodi isti move helper, brez širjenja prihoda.
Staro pot vodje pred sidrnim iskanjem počisti: native isti cilj sicer vrne tudi staro delno pot (Q416).
Na `299d947a…` vseh devet controller testov PASS: unit (+17), build/package, long 70, short 50,
swarm 49, teren 53, strict ticket 94 in parna cena. Oba ticket resume ARRIVED; 32 članov prehodi
najmanj 238,267, konča največ 9,440 od cilja z razmikom ≥2,000; po restartu 8 etap. Ovira BLOCKED, pri 0 bitna enakost.
Cena 200, ena ponovitev 60+180 s: p95 idle/pohod 6,685/10,748 ms, alokacija 48,2/74,1 MB/s, nič tickov >50 ms.
Kratka noga ARRIVED; zadnjih 25 končnih čet v logu 200 na mestu/0 neporavnanih (aktivni vzorec ni dokaz).
Meje: ravni dolg pohod 32, ločeni ticket 8/2/2 in kratki teren/roj; dolgi pohod 200 skozi teren/nenaložen svet in živa mapa še nista dokazana.
Po seriji samo dokumentacija, brez ponavljanja PASS (Q90). Noben workload iz seje, nepovezano delo ohranjeno.
Dokumentacija in kazala posodobljeni; commit/push zaključi paket. Naslednji M13.4; Q1 ostane.
[Dokazi, hipoteza in merila](meritve/2026-11-03-M11.5-dolg-pohod.md).

### 2026-11-04 (224) — M13.3: več izbir v seznamu palice (sprejet)

**Paket:** M13.3 (Q400–Q411; Q407 odločitev lastnika) · **Stanje:** sprejet pod stikalom (Q411); `RwLinkedGroups` ostane 0.
Na odtisu `6f69c790…` controller unit/build/package, `m133-wand-v2` (96) in `m132b-gui-v2` (120) PASS; vseh 14 PNG pregledanih.
Potrjeni filter, isto ime v radiu, Ctrl/Shift izbor, kopija videza 3, povezava 3, reset 1, delete 2, urejanje primarnega in zavrnitev po deop.
Q405 (stara polja po odvzemu pravic) dokazan tudi vizualno: prazna polja, Loaded members: 0.
Q410 potrjen: `footer-within-frame` in linked PNG pokažeta izbor pred Freeze in status pod njim, oba v okvirju.
Po seriji samo dokumentacija; brez ponavljanja uspešnih testov (Q90). Uporabniški vodič, changelog, register in kazala posodobljeni.
Meje: en klient/dev par, šest NPC-jev; Shift/Ctrl vstavi harness; cena ni merjena, največ 1000 vrstic. Noben workload iz seje; nepovezano delo ohranjeno.
Naslednji paket po sprejemu je M11.5 (Q407), nato M13.4; M10.3 blokiran Q1.
[Dokazi in meje](meritve/2026-11-04-M13.3-palica.md).
