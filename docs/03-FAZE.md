# 03 — Faze in delovni paketi

Vsak milestone ima: **cilj**, **vhodni pogoj**, **delovne pakete**, **izhodni kriterij** in
**tveganja**. Milestone ni zaključen, dokler ni izpolnjen izhodni kriterij — ne ko je koda
napisana.

Oznake velikosti: **S** = ena seja · **M** = 2–4 seje · **L** = 5–10 sej · **XL** = več kot 10.
Ocene so grobe in se popravljajo v `docs/04-STANJE.md`.

**Ta plan ni dogma (D-029, 8. 10. 2026).** Paketi spodaj so delno nastali s šibkejšimi AI modeli.
Vsak milestone se zato začne s paketom **M?.0 — raziskava izvedbe** (koda, meritve, zunanji
viri, kritika vsakega paketa), ki lahko pakete spremeni, zavrže, doda ali prerazvrsti. Postopek:
[`05-SEJA-PROTOKOL.md` §1a](05-SEJA-PROTOKOL.md). Milestone brez zaključenega M?.0 se ne začne;
za milestone, ki že teče (M5), M5.0 velja pred naslednjim paketom.

---

## Pregled

| Milestone | Cilj | Zahteve | Velikost |
|---|---|---|---|
| **M0** | Temelj: okolje, build, testi, git | — | **zaključeno** |
| **M1** | Integriteta podatkov | R9, B1, B2 | L |
| **M2** | Diagnostika, reprodukcije, baseline meritve | podpora R1, R5 | M |
| **M3** | Jedro entitete: mount in solid hitbox | R1, R6 | L |
| **M4** | Gibanje: navigacija po tleh in letenje | R2 | L |
| **M5** | Performance AI | R5 | L |
| **M6** | Scripting platforma v Javi | R7 | XL |
| **M7** | Animacijski sistem in AI avtorsko okolje — jedro v `customNPC_entities_mod` (D-024) | R4 | M |
| ~~**M8**~~ | ~~Migracija funkcij iz CustomNPC+~~ — **izločeno iz plana** (D-023) | R3 | — |
| **M9** | Chatbot | R8 | M |
| **M10** | Release kandidat | — | M |
| **M11** | Poveljevanje skupinam prek zemljevida — **zadnja faza**, ko je vse ostalo večinoma narejeno (D-026) | R10 | L |

---

## M0 — Temelj

**Cilj:** ponovljiv build, dokazano enako obnašanje kot original, delujoča testna in
razvojna zanka, sledljiva zgodovina sprememb.

**Vhodni pogoj:** originalni JAR s preverjenim hashom.

### Paketi

| ID | Paket | Stanje | Vel. |
|---|---|---|---|
| M0.1 | Dekompilacija, audit, `reference-src` | **narejeno** | — |
| M0.2 | Gradle okolje, zaklenjene verzije, `environment-lock.json` | **narejeno** | — |
| M0.3 | Karakterizacijski testi + `verify-package.ps1` + smoke test klienta | **narejeno** | — |
| M0.4 | **Git repozitorij** — `git init`, prvi commit, `.gitignore` preverjen | **narejeno** | S |
| M0.5 | **Dedicated server smoke test** — `runServer`, EULA, spawn/save/restart | **narejeno** — `docs/scenariji/M0.5-server-smoke.md` | S |
| M0.6 | **Testni svet** z znanimi NPC-ji, questi, dialogi in skriptami, kot ponovljiv seed | **narejeno** — `.\testworld-run.ps1`, `docs/scenariji/M0.6-testni-svet.md` | S |
| M0.7 | **Integracijska matrika** zapisana kot ponovljiv postopek v `docs/scenariji/` | **narejeno** — `docs/scenariji/M0.7-integracijska-matrika.md` | S |
| M0.8 | Pridobiti od uporabnika: modpack, Forge verzija, config, kopijo pravega sveta, primer pokvarjenega clone NPC-ja | **zaključeno kot zabeležena blokada** (17. 9.) — gradiv ni; vpliv v M0.7 §6 | S |

**Izhodni kriterij:**
- `git log` ima zgodovino; nobena sprememba ni več neizsledljiva
- `runServer` se zažene, NPC preživi save + restart — **izpolnjeno 2026-09-14**
- obstaja testni svet, ki ga lahko katerakoli seja odpre in ponovi scenarij
- M0.8 je bodisi izpolnjen bodisi zabeležen kot blokada z jasnim vplivom

**Tveganja:** brez M0.8 je združljivost z uporabnikovim dejanskim modpackom neznanka do M10.

**M0 je zaključen 2026-09-17.** Vsi štirje izhodni kriteriji so izpolnjeni: git zgodovina
obstaja, `runServer` in restart sta dokazana (M0.5), testni svet je ponovljiv (M0.6), M0.8 pa
je zabeležen kot blokada z opisanim vplivom v
[`scenariji/M0.7-integracijska-matrika.md`](scenariji/M0.7-integracijska-matrika.md) §6.
Integracijska matrika je s tem odprta in se polni skozi M1–M10.

---

## M1 — Integriteta podatkov

**Cilj:** nobena shranjena datoteka se ne more več tiho pokvariti ali izgubiti.
Rešuje **R9** in hkrati **B1** in **B2** iz `PLAN_IMPLEMENTACIJE.md`.

**Vhodni pogoj:** M0.4 (git), M0.8 (primer pokvarjene datoteke — zaželeno, ne blokada).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M1.1 | Prenos `NBTJsonUtil` in `ServerCloneController` v `src/patch/java`; karakterizacijski testi, ki **dokumentirajo trenutne napake** (R9-a do R9-j) | M |
| M1.2 | `javap` verifikacija R9-d (byte/long array `toString`) in R9-e (`SaveFile` writer) na originalnem bytecode | S |
| M1.3 | Nov tipno varen NBT↔JSON serializer v `rework/data/` + fuzz round-trip testi za vse NBT tipe | M |
| M1.4 | Bralnik za stare in pokvarjene datoteke | **odloženo** — uporabnik nima takih datotek; ne ugibamo | M |
| M1.5 | `SafeFileWriter` — atomski zapis z verifikacijo; preklop **vseh** controllerjev nanj (popravek B1) | M |
| M1.6 | Lifecycle asinhronih zapisov (popravek B2): zajem poti in seje ob zahtevi, kontroliran shutdown | M |
| M1.7 | Verzioniranje `SaveFormat` + migracija | **ni potrebno za R9** — format je ostal združljiv | S |
| M1.8 | `.\dev.ps1 auditClones` | **odloženo** — ni konkretnih poškodovanih datotek | S |
| M1.9 | Fault injection testi: zaklenjena datoteka, zavrnjen dostop, I/O napaka, prekinitev pred/po zamenjavi, pokvarjen JSON, restart med migracijo | M |

M1.5 se zaradi pravil postopnega prenosa originalnih razredov izvaja v manjših preverljivih
korakih: **a)** clone JSON, **b)** `PlayerData`, **c)** preostali sinhroni JSON controllerji,
**d)** controllerji s stisnjenim NBT. Vsak korak dobi baseline commit pred funkcionalno spremembo.

**Izhodni kriterij:**
- round-trip test čez vse NBT tipe (vključno z `byte[]`, `int[]`, `long[]`, prazni seznami,
  gnezdenimi strukturami, unicode, ubežnimi znaki) je **bit-identičen**
- po vsakem fault injection scenariju ostane obnovljiva zadnja veljavna verzija
- dodatna obnova/audit se odpre samo, če se pojavi konkretna poškodovana datoteka
- `verify-package.ps1` pokaže točno pričakovan seznam spremenjenih razredov

**Tveganja:**
- Nov serializer, ki bere staro datoteko drugače kot stari, lahko "popravi" nekaj, kar je
  bilo namerno. Zato M1.4 vedno naredi backup in poroča, nikoli tiho ne spreminja.
- Windows `renameTo` / `ATOMIC_MOVE` semantika se mora testirati **na Windows**, ne na Linuxu.

---

## M2 — Diagnostika, reprodukcije, baseline

**Cilj:** znati dokazati, da je bug prisoten, in izmeriti, koliko nekaj stane — preden karkoli
spremenimo.

**Vhodni pogoj:** M0.6 (testni svet).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M2.1 | `rework/diag/` — instrumentacija: koliko NPC-jev tika, koliko izračunov poti, koliko script klicev, koliko časa v katerem AI tasku | M |
| M2.1a | jedro `rework/diag`, zbiralnik na Forge dogodkih, ukaz `/rwdiag`, 23 testov | **narejeno** — `docs/scenariji/M2.1-diag.md` |
| M2.1b | klicna mesta za pot (M3.1), AI taske (M3.1) in skripte (M6.6, prej M5.1) | odprto — vezano na prenos teh razredov |
| M2.2 | Reprodukcija **R1**: 8 jahačev na 8 nosilcih, skupen cilj, log pozicij po ticku | S |
| M2.3 | Reprodukcija **R2**: leteči NPC z oviro med seboj in ciljem; izmeriti, ali sploh pride | S |
| M2.3a | scenarij, fixture (`R2_Flyer/Walker/Target/Control`), `r2-control.js` in `r2-run.ps1`: tri proge (leteči+zid, kopenski+zid, leteči prosto), merila L1–L8 | **narejeno** 17. 9. — `docs/scenariji/M2.3-R2.md`; čaka na prvi zagon v svetu |
| M2.4 | Scenariji za meritve: 50 / 200 / 500 NPC-jev, ločeno idle / combat / scripts / render | **koda narejena** 23. 9. — `perf-run.ps1`, `docs/scenariji/M2.4-obremenitve.md`; idle/boj/skripte, merila P1–P7; čaka na prvi zagon v svetu. Render ostane kot M2.4r (potrebuje klient) |
| M2.5 | Merilni protokol kot skripta: 2 min ogrevanja, 5 min merjenja, 3 ponovitve, izpis MSPT p50/p95/p99, alokacije, GC | M |
| M2.5c | **Protokol ponovitev**: `meritve-lib.ps1` (strojno berljiv zapis zagona: odtis pogojev proti izmerjenim veličinam), `ponovitve-run.ps1` (N zagonov v svežem svetu, združevanje, šumni pas, merila T1–T6) in `ponovitve-samotest.ps1` (16 trditev brez Minecrafta). `nav-run.ps1` in `rwdiag-run.ps1` dobita `-JsonPath` | **narejeno** 18. 9. — `docs/scenariji/M2.5c-ponovitve.md`; serija pognana 18. 9., T1–T6 zelena; `docs/meritve/2026-09-18-M2.5c-ponovitve-nav.md` |
| M2.6 | **Baseline meritve originala** — zapis v `docs/meritve/baseline-<datum>.md` | **zaključeno 5. 10.** — [baseline](meritve/baseline-2026-10-05.md); šum: MSPT p50 razpon večinoma < 5 %, `ticki >50 ms` pri idle-500 in skripte-500 zelo šumni (3–26, 52–137) |
| M2.7 | **Merila kakovosti navigacije** (novo 17. 9., podlaga za M4.10–M4.12 in M5.6). Šest veličin, izmerjenih na originalu: (1) delež zahtev, ki vrnejo **celo** pot, ne delne; (2) dolžina poti proti zračni razdalji; (3) čas do cilja za skupino 8 NPC-jev; (4) razpon skupine na ozkem grlu; (5) µs na eno iskanje poti; (6) iskanj poti na tick. **Brez teh številk je vsak poseg v navigacijo nemerljiv** in ga po `05-SEJA-PROTOKOL.md` ni dovoljeno razglasiti za izboljšavo | S |
| M2.7a | sonda `rwdiag nav` (`NavProbe`, `NavSweep`), opazovalec dodelitev poti, scenarij z dvema progama (`NAV_WalkG/WalkO/Control`, `nav-control.js`, `nav-run.ps1`), merila N1–N12 | **narejeno** 17. 9. — `docs/scenariji/M2.7-navigacija.md`; čaka na prvi zagon v svetu |
| M2.7b | **Več vzorcev za veličino 5** (µs na iskanje): osem iskanj na pometanje da p50/p95 z razponom do 114 % (M2.5c, 18. 9.). Možnosti: združevanje več pometanj, več ciljev, ali skupni čas namesto percentilov. **Vhodni pogoj za A/B v M4.11 in M5.6** | S |

**Izhodni kriterij:**
- R1 in R2 imata reprodukcijo, ki jo lahko ponovi katerakoli seja in ki jasno pokaže napako
- obstaja baseline tabela, proti kateri se meri vsaka kasnejša sprememba
- instrumentacija se da izklopiti brez merljive režije

**Tveganja:** če stroj ne zmore 500 NPC-jev, se stopnja zaključi in zapiše nasičenje —
to je veljaven rezultat, ne neuspeh.

---

## M3 — Jedro entitete: mount in solid hitbox

**Cilj:** **R1** (jahanje NPC na NPC) in **R6** (solid hitbox). Skupaj, ker oba posegata v
hitbox, kolizije in passenger logiko v `EntityNPCInterface`.

**Vhodni pogoj:** M2.2 (reprodukcija R1), M1 (da se nove nastavitve varno shranijo).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M3.1 | Prenos `EntityNPCInterface` in celotnega `noppes/npcs/ai/` v `src/patch/java`, brez funkcionalnih sprememb + baseline testi | **zaključeno 23./24. 9.** — bytecode enakovreden (`audit/m31-bytecode-primerjava.txt`); testworld zelen, R1 ponovi izid; runtime JAR = mapiran original + natanko 99 prevedenih razredov |
| M3.2 | Diagnoza R1 na podlagi reprodukcije: kdo dejansko ne deluje, nosilec ali jahač; potrditev ali ovržba kandidatov iz `02-ZAHTEVE.md` | **zaključeno 24. 9.** — vanilla `updateEntityActionState` jahača briše pot nosilcu; potrjeno v svetu (faza C, E7: navig nosilca = navig jahača v 20/20, proga M na cilju). [diagnoza](meritve/2026-09-24-M3.2-R1-diagnoza.md) |
| M3.3 | `rework/entity/RiderState` — enoten vir resnice o jahanju | **zaključeno 24. 9.** — `RiderState` + `MountGuard`, stikalo `RwMountSteering`/`/rwmount` (privzeto 0 = original); v svetu z načinom 1 proga M pride do cilja (E8 zeleno), način 0 ponovi original. [zapis](meritve/2026-09-24-M3.3-R1-popravek.md) |
| M3.4 | Gating AI taskov med jahanjem; prepoved `EntityAIFollow.tpTo` na jahaču | **zaključeno 24. 9.** — `EntityAIFollow`/`EntityAIMovingPath` ne tečeta, ko krmili nosilec; `tpTo` jahača premakne nosilca; pod `RwMountSteering`; regresija R1 zelena v načinih 0/1/2 ([zapis](meritve/2026-09-24-M3.4-regresija.md)) |
| M3.5 | `updateHitbox()` ob spremembi jahanja (Forge `EntityMountEvent`) | **zaključeno 24. 9.** — mount že pravilen v originalu (`EntityCustomNpc.startRiding`); popravek samo za sestop, v `dismountRidingEntity` (ne `EntityMountEvent`, ker se sproži pred spremembo); pod `RwMountSteering`; E9 v svetu: način 0 po sestopu 1,463 (napaka), način 1 1,900 ([zapis](meritve/2026-09-24-M3.5-hitbox.md)) |
| M3.6 | ~~Popravek mutex bitov `EntityAIAttackTarget`~~ → **prioriteta napada pred gibanjem** (pod stikalom `RwAttackPriority`, z A/B scenarijem `m36-run.ps1`). Preusmerjeno 24. 9. z odobritvijo uporabnika (D-020): mutex biti napada so enaki vanilla, napaka je v prioriteti | **zaključeno 5. 10.** — način 1 zelen (tavajoči v dosegu 10/10 z zakasnitvijo kontrole), način 0 napako ponovi (6/8 nad kontrolo), hipoteza `aggroRange` (P2) potrjena ([zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)) |
| M3.7 | Ločitev `minRange` od `npc.width`; spodnja meja napadalnega dosega | **zaključeno 5. 10. brez spremembe obnašanja** — premisa ovržena iz kode: doseg = max(r² + wt, (2w)² + wt), izklop hitboxa ga ne zmanjša. Nov kandidat za zgoščevanje (brez hitboxa se NPC-ji ne odrivajo) čaka na scenarij in spada v M3.8. [analiza](meritve/2026-10-05-M3.7-analiza-minrange.md) |
| M3.8 | **R6**: `RwHitboxMode` ORIGINAL / SOLID / SMART (velikost + ščit) + NBT + ukaz `/rwhitbox` + testi; GUI v M3.8b (D-025, zahteva uporabnika 5. 10.) | **zaključeno 5. 10.** — scenarij `hb-run.ps1` H0–H8 zelen, 201/201 testov; **M3.8b GUI v kodi 5. 10.** (vrstica Hitbox na zavihku Display), čaka na `hb-run` in preverbo v klientu ([zapis](meritve/2026-10-05-M3.8-hitbox-nacini.md)) |
| M3.9 | Preverba solid × mount: solid nosilec pod jahačem, solid NPC na poti drugega NPC-ja, igralec ujet med dvema | **zaključeno 7. 10.** — `m39-run.ps1` J1–J6, P1–P4 zelena; igralec iz kode in s testom (klient skupaj z M3.8b); ugotovitev Q15: jahan nosilec in solid NPC se ne razmakneta ([zapis](meritve/2026-10-07-M3.9-solid-jahanje.md)) |
| M3.10 | *Opcijsko, nova funkcija:* jahač kot "commander" nosilca — konjenica, kjer nosilec sledi jahačevemu cilju | M |

**Izhodni kriterij:**
- reprodukcija iz M2.2 je zelena: 8 konjenikov doseže cilj, se razporedi po dosegu orožja
  in ne konča v kupu enega bloka
- konjenica prehodi stopnico in vrata
- SOLID NPC se ne da odriniti; drugi NPC-ji ga obidejo; igralec se ne zatakne
- vse tri hitbox nastavitve preživijo save/load in clone

**Tveganja:**
- Sprememba prioritete napada (M3.6) spremeni obnašanje vseh NPC-jev brez projektila, ki tavajo
  ali hodijo po poti, ne samo konjenice. Obvezno stikalo in A/B meritev.
- `getCollisionBoundingBox()` ne-`null` na premikajočem se NPC-ju lahko potisne igralca
  v steno. Testirati posebej.

---

## M4 — Gibanje: navigacija po tleh in letenje

**Cilj:** **R2** — uporabno letenje v treh načinih — in odprava tistih slabosti kopenske
navigacije, ki so **izmerjene**, ne občutene (D-012).

**Vhodni pogoj:** M3.1 (`ai/` paket prenesen), M2.3 (reprodukcija R2), **M2.7** (merila
kakovosti navigacije) za pakete M4.10–M4.13.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| **M4.0** | **Raziskava izvedbe (D-029), pred vsemi ostalimi paketi M4.** Letenje (CustomNPC+, vanilla `PathNavigateFlying`/`FlyingNodeProcessor`, drugi modi z letečimi entitetami, 3D A* in alternative) in kopenska navigacija (izmerjeno: R2 je zastarela delna pot, M2.3; sledenje poti je v boju 48 % CPU, M5-S P1; Baritone kot izbirno ozadje, D-022). Za vsak paket M4.1–M4.14 odločitev ohrani/spremeni/zavrzi/dodaj, zapis v `docs/raziskave/`, prilagoditev tega razdelka | S |
| M4.1 | Branje CustomNPC+ flying implementacije; odločitev: port ali lasten 3D pathfinder | S |
| M4.2 | `rework/movement/FlightMode` enum + NBT ključ `RwFlightMode` + GUI; privzeto = original | S |
| M4.3 | `HoverMoveHelper` — creative-style, brez inercije, hitro dušenje | M |
| M4.4 | Popravek `isNotColliding` — vzorčenje po hitboxu, obhod namesto `WAIT` | S |
| M4.5 | 3D pathfinding (lasten A* ali port) z oceno stroška; integracija s `PathNavigateFlying` | L |
| M4.6 | `MomentumMoveHelper` — inercija, omejen kotni pospešek, minimalna hitrost, dviganje/spuščanje | M |
| M4.7 | Pitch in roll: server→klient sinhronizacija + render | M |
| M4.8 | Vrata, zavetje in vodna navigacija za leteče NPC-je (trenutno zgodnji `return` pri `canFly()`) | S |
| M4.9 | Meritve: strošek 3D pathfindinga proti obstoječemu, pri 50/200 letečih NPC-jih | S |
| M4.10 | **Nadaljevanje delne poti.** Vanilla A* se prekine po 200 vozliščih in vrne **delno** pot (`PathFinder.findPath:65`), dolžino pa omeji na `getPathSearchRange()` = `FOLLOW_RANGE` = `NpcNavRange` = 32 (`EntityNPCInterface.java:334`). NPC obstane na koncu delne poti in to izgleda kot ovira. Popravek: ob prihodu na konec delne poti se pot obnovi, dokler cilj ni dosežen ali dokazano nedosegljiv, z omejitvijo števila obnov. Pod stikalom, privzeto original | S |
| M4.11 | **Lasten `NodeProcessor`** — realna cena diagonale (1,41 namesto manhattanskih 2) in pregledani malusi `PathNodeType`. Vanilla uporablja manhattansko ceno **in** hevristiko pri 8-smernem gibanju (`PathPoint.distanceManhattan:86`), zato poti sistematično bežijo od diagonal; od tod stopničasto cikcakanje. A/B proti merilom M2.7 | M |
| M4.12 | **Odločitvena točka: lasten `PathNavigate` z lastnim A\***. Izvede se **samo**, če M4.10, M4.11 in M5.6 po merilih M2.7 ne zadostujejo. Obseg: lasten proračun vozlišč, hevristika in domet; `Path`, `PathPoint` in move helper ostanejo vanilla, da vsi obstoječi AI taski delujejo nespremenjeno. **Brez asinhronega iskanja** | L |
| M4.13 | Mehkejše sledenje poti: toleranca do waypointa in „string pulling" v `pathFollow` (`PathNavigate:271-305`). Samo če M2.7 pokaže trzanje in striženje vogalov kot merljiv pojav, ne kot občutek. **Vhod iz M5.0 (S15):** to isto zanko (`isDirectPathBetweenPoints` za vsako točko nazaj) Mojang v 1.21.1 za kopenske mobe ne izvaja več — napredovanje po razdalji in geometrijskem testu smeri, brez branja blokov; v boju-500 je ta zanka največji strošek CPU. Sprememba gibanja, zato tu in ne v M5; skupaj z M4.11 (stopničaste poti se brez rezanja vidijo) | S |
| M4.14 | **Formacije** (novo 18. 9., na zahtevo uporabnika, D-017): skupina NPC-jev se premika kot ena enota — ena pot za vse (vodja), zaprta zanka hitrosti, stiskanje skozi ozka grla, sidranje na koncu. Ukaz `/rwsquad` in `FormationApi` za skripte; brez ukaza paket ni prijavljen in ne doda taska. Zasnova: [`06-FORMACIJE.md`](06-FORMACIJE.md) | M |
| M4.14a | Jedro (`SquadPlanner`, oblike, dodelitev mest, pot sidra) + adapter (`Squad`, `FormationMoveTask`, `SquadManager`, ukaz, API) + 21 testov v simulaciji | **narejeno 18. 9.** — prevedeno in zeleno v seji (D-014), v svetu ni pognano |
| M4.14b | Preverjanje v svetu: dimni test F1–F12 in A/B proti skripti na prizorišču M2.7 (FA1–FA6). Za A/B mora `nav-control.js` dobiti način `FORMACIJA`. [Scenarij](scenariji/M4.14-formacije.md) | S |
| M4.14c | Šele po M4.14b in M2.4: cena na tick pri 50/200 članih; parametri iz `06-FORMACIJE.md` §6 po meritvi | S |

**Izhodni kriterij:**
- leteči NPC pride od A do B skozi labirint z ovirami, brez obtičanja
- način Hover: NPC se ustavi na mestu, ne drseva
- način Momentum: NPC zavija po krivulji, ne v pravem kotu; ima minimalno hitrost
- pathfinding pri 200 letečih NPC-jih ne pojé več kot dogovorjen delež MSPT-ja
- obstoječi leteči NPC-ji brez `RwFlightMode` se obnašajo **enako kot prej**

**Izhodni kriterij za formacije (M4.14):** F1–F12 in FA1–FA6 iz scenarija M4.14b; brez
tega paket ostane "v kodi", ne "narejen". M4.14 **ni** odvisen od M3.1: ne spreminja
`EntityNPCInterface`, ampak AI task doda od zunaj (D-017).

**Izhodni kriterij za kopenski del:** vsak od M4.10–M4.13 ima A/B meritev proti merilom
M2.7, ki presega merilni šum; kandidat, ki ga ne, se **zavrže** in se ne obdrži „za vsak
slučaj".

**Tveganja:** 3D pathfinding je klasično ozko grlo. Če se izkaže za predrag, je fallback
"smer + izogibanje oviram" brez polnega A*.

**Kaj je tu zavrnjeno in zakaj** (D-012): asinhrono iskanje poti, flow fieldi za skupinsko
gibanje in lasten gibalni sklad. Prvo je edini del tega sklopa z resnično visokim tveganjem
(svet se med iskanjem spreminja), drugi dve spremenita občutek gibanja vseh obstoječih
NPC-jev. Odprejo se šele, če po M4.10, M4.11 in M5.6 merila M2.7 še vedno padajo.

---

## M5 — Performance AI

**Cilj:** **R5** — merljivo več NPC-jev pri istem MSPT.

**Vhodni pogoj:** M2.6 (baseline meritve). Brez njih se M5 ne začne.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| **M5.0** | **Raziskava preostanka M5 (D-029)** | **zaključeno 8. 10.** — [zapis](raziskave/M5.0-preostanek-performance.md); plan spodaj je prepisan po njem (D-030) |
| M5-S S1, S2 | Predzavrnitev tarč in poizvedba samo po igralcih (`RwTargetPrefilter`, D-027) | **zaključeno 6./7. 10.** — idle-500 p95 44 → 5,2 ms |
| M5-S P1 | Profil JFR strežniške niti (idle/boj/skripte × 500) | **zaključeno 8. 10.** |
| **M5.10** | **Odločilni poskus za boj:** števec ns okoli `isDirectPathBetweenPoints` in celega `pathFollow`, štetje kandidatov (`/rwpath cas 1`, `perf-run -RwPathCas`), pogoj veljavnosti A/B boja (`dev/tools/ab-boj.js`) | **zaključeno 9. 10.** — delež `pathFollow` v boju-500 **47,7–53,6 %** ticka (prag 25 % presežen); na `pathFollow` vedno 0 ali 1 kandidat, vsak uspe; šum boja je v delu na klic (µs/kandidata razpon 24,5 %), ne v številu klicev (1,1 %) → pogoj ±10 % je nujen, ne zadosten. [zapis](meritve/2026-10-09-M5.10-delez-sledenja-poti.md) |
| ~~M5.11~~ | **S14b — pomnjenje tipa vozlišča za čas enega klica `isDirectPathBetweenPoints`** v `RwPathNavigateGround` (ključ = pozicija; entiteta in svet med klicem enaka; `currentEntity` nastaviti kot vanilla). Po M5.10 zadošča en klic: v boju ima `pathFollow` en kandidat, ostane ~3× ponavljanje istih mest v klicu (model); kandidati so ~49 % ticka, ~56 % alokacij boja je v teh ocenah. Enakost z obstoječim načinom preverbe (2). **A/B pri istem boju:** preklapljanje načina med tekom v izmenjujočih se oknih s števci ločeno po načinu, ključna veličina µs na kandidata; nato MSPT s ponovitvami in `ab-boj.js`. Kandidat pod šumom gre iz kode (tudi S14) | **zaključeno 9. 10.** — enakost 0 / 1,25 M; v istem boju µs/kandidat 0,40×; celi zagoni boj-500 MSPT povp 8,73 → **5,88 ms**, p95 9,96 → 7,21; S14 v šumu in odstranjen, `RwPathFollowCache` 1 = S14b, privzeto 0 (Q17). [zapis](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md) |
| M5.12 | **S4 — `onCollide` brez opazovalca:** brez skripte `collide` in brez poslušalca `CollideEvent` na `WrapperNpcAPI.EVENT_BUS` preskočiti poizvedbo (idle 12 %, skripte 15 % CPU). Nadomesti M5.3 | S |
| M5.13 | **S7 — prejemniki utripa oči** po `world.playerEntities` z istim testom presečišča namesto AABB 160 blokov (idle 6,5 %) | S |
| M5.9 | **Scenarij nedosegljive tarče** (`perf-run` varianta): NPC-ji s tarčo, do katere ni poti (stolp/zid). Edina podlaga za M5.2, M5.6 in S5 — v obstoječih celicah je iskanje poti 2 % | S |
| M5.2 | *Pogojno na M5.9:* AI budget / scheduler novih izračunov poti na tick, poštena vrsta. **Brez** zmanjšanja tick frekvence NPC-jev; sprememba obnašanja → stikalo | M |
| M5.6 | *Pogojno na M5.9:* deljenje in predpomnjenje poti ter negativni predpomnilnik (S5). Premisa »največji dobitek v navigacijskem sklopu« je za boj ovržena (P1: 2 %) | M |
| M5.14 | **S16 — senčna polja `EntityDataManager`** za CNPC-jeve ključe (`Attacking`, `IsDead`, `Walking`), osvežena v `notifyDataManagerChange`. Idle ≤ ~5–8 % | S |
| M5.15 | **Infrastruktura vanilla posegov (D-031):** lasten coremod v CNPC jarju (`rework/core`: `IFMLLoadingPlugin`, `IClassTransformer`), manifest `FMLCorePlugin` + `FMLCorePluginContainsFMLMod` v `devRuntimeMod` in `buildPatchedMod`, imena SRG v produkciji in MCP v dev okolju, `verify-package` pozna nove vnose. Brez posega v obnašanje (prazen transformer z dnevnikom), dimni test dev server + zapakiran jar | S |
| M5.16 | **`Entity.move` brez odvečne poizvedbe entitet za trke:** `World.getCollisionBoxes(Entity, AABB)` za vsak premik poišče entitete v kvadru, čeprav prispevajo škatlo samo razredi, ki prepišejo `getCollisionBoundingBox`/`getCollisionBox(Entity)` (čoln, minecart, shulker, NPC pod `RwHitbox`, modni). Števec takih entitet po svetu (`IWorldEventListener.onEntityAdded/Removed`, razred preverjen z refleksijo enkrat) — poizvedba se preskoči samo, ko je števec 0 **in** razred premikajoče se entitete ne prepiše `getCollisionBox(Entity)`; `GetCollisionBoxesEvent` ostane. Prepisan razred se šteje konzervativno (tudi če včasih vrne `null`). Idle 11,8 % CPU v gosti mreži [M], v redkem razporedu manj [H]. Velja za vse entitete, ne le NPC-je | S |
| M5.8 | **Kapaciteta proti baselineu** (izhodni kriterij): največji N pri MSPT p95 ≤ 25 in ≤ 50 ms, original proti vsem sprejetim stikalom, idle in boj, gosta mreža in redek razpored (razmik ≥ 3 bloke) | S |
| ~~M5.1~~ | Globalni script lock → **preseljen v M6.6** (D-030): `ScriptContainer` je v skripte-500 1,8 % CPU, lock je nesporen; ostane zaradi pravilnosti, brez cilja MSPT | — |
| ~~M5.3~~ | Deduplikacija po chunku → **zavrženo** (D-030), nadomesti M5.12 (S4) | — |
| ~~M5.4~~ | Alokacijska higiena → **zavrženo kot samostojen paket** (D-030): `DataTimers` ~0,8 MB/s, ostalo le pri skriptah — pod šumom | — |
| ~~M5.5~~ | Meritev po vsakem → ni paket, je pravilo (D-016) | — |
| ~~M5.7~~ | Prostorski indeks → **zavrženo** (D-030, chunk sekcije so že indeks; drag je gostota mreže); render LOD → K5 v M5-K | — |

Vrstni red M5-S: **~~M5.10~~ → ~~M5.11~~ → M5.12 → M5.13 → M5.9 → (M5.2 / M5.6 po izidu) → M5.14 → M5.15 → M5.16 → M5.8.**
Sledenje poti brez branja blokov po vzoru 1.21.1 (S15, sprememba gibanja) ni v M5, ampak vhod za M4.13.
M5-K (klient, K0–K13 v [09](09-PERFORMANCE-RAZISKAVA.md)) je ločena veja z vhodnim pogojem M2.4r.
Vanilla `Entity.move` (idle 26–29 %): Q16 odločeno → D-031 (lasten coremod v CNPC jarju), paketa M5.15 in M5.16.

**Izhodni kriterij:**
- vsak sprejet paket ima A/B meritev, ki presega šum
- nobena funkcionalna regresija v matriki iz `PLAN_IMPLEMENTACIJE.md` §6
- timerji in script trace se ujemajo z referenco
- zapisano, koliko NPC-jev zdaj zmore isti stroj proti baselineu (M5.8)

**Tveganja:** S4 mora zanesljivo zaznati poslušalce na vodilu CNPC API-ja (tudi registrirane kasneje),
sicer tiho izgine dogodek; A/B boja je šumen zaradi poteka boja — po M5.10 zaradi dela na klic, ne števila klicev, zato A/B pri istem boju (M5.11). Tveganje odstranitve globalnega
locka je od D-030 v M6.6.

---

## M6 — Scripting platforma v Javi

**Cilj:** **R7** — pisanje NPC skript v Javi, s prevajanjem, tipi in diagnostiko, ki jo AI
lahko uporabi.

**Vhodni pogoj:** M1 (varno shranjevanje skript in njihovih podatkov).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M6.1 | Prototip prevajalnika; odločitev **Janino** vs `javax.tools.JavaCompiler`; meritev časa prevajanja | M |
| M6.2 | `rework/script/hooks/` — hook registry; ponovna implementacija obstoječih dogodkov skozenj, brez spremembe vrstnega reda in frekvence | M |
| M6.3 | `rework/script/api/` — tipiziran API za NPC (najmanjši uporaben nabor) | M |
| M6.4 | Izolacija: `ClassLoader` na skripto, hot reload, cache prevedenih razredov | M |
| M6.5 | **Diagnostika napak**: prevajalne napake z datoteko/vrstico/stolpcem; runtime izjeme s stack traceom skripte; skripta se po popravku spet zažene (popravek **B5**) | M |
| M6.6 | Odstranitev globalnega locka (prej M5.1, preseljen z D-030: v M5 ni dobitka MSPT, ostane zaradi pravilnosti statičnih `Current`/`CurrentType`) | — |
| M6.7 | `.\dev.ps1 compileScripts` — prevajanje vseh skript **brez Minecrafta** | S |
| M6.8 | Varnostna omejitev: whitelist paketov, prepoved refleksije / `java.io` / `java.net` / `System.exit`; bytecode verifikacija | M |
| M6.9 | Generator API dokumentacije: `docs/api/API.json`, `API.md`, `HOOKS.md` | M |
| M6.10 | Projektna predloga `scripts/java/template/` z gradle/IDE nastavitvijo za autocomplete | S |
| M6.11 | Razširitev API-ja: player, world, item, block, quest, dialog, faction | L |
| M6.12 | Združljivost: obstoječe JS skripte delujejo nespremenjeno; izbira jezika na NPC-ju | S |

**Izhodni kriterij:**
- Java skripta se prevede, naloži, teče in se hot-reloada brez restarta strežnika
- prevajalna napaka da uporabno sporočilo z vrstico, v igri in v logu
- `compileScripts` vrne rezultat v nekaj sekundah
- obstoječe JS skripte v testnem svetu delujejo identično kot prej
- `API.json` je popoln in strojno berljiv
- izmerjena razlika v performanceu Java vs JS na istem scenariju je zapisana

**Tveganja:**
- Varnostna izolacija prevedene Jave je težka. Če se izkaže, da je z Janino ni mogoče
  zanesljivo uveljaviti, se to **jasno dokumentira** kot omejitev (Java skripting samo za
  zaupanja vredne avtorje), namesto da se pretvarjamo, da je sandboxan.
- Hook registry ne sme spremeniti vrstnega reda ali frekvence dogodkov
  (`PLAN_IMPLEMENTACIJE.md` §2).

---

## M7 — Animacijski sistem in AI avtorsko okolje

**Cilj:** **R4** — AI piše animacije v tekstovnem formatu, jih validira in uporabi iz skript.

> **2026-10-03 (D-024):** jedro (M7.1–M7.3, M7.4 za lastne modele, M7.6, M7.7, M7.10) je že
> narejeno v ločenem modu `customNPC_entities_mod` (v0.7.0). Ostanejo script API/hooki,
> uskladitev verzije CNPC (05Jul20 proti 01Oct19), reload in po želji animacije za navadne
> CNPC NPC-je. Ocena **M namesto XL**; stanje po paketih v
> [`08-PREGLED-IN-PLAN.md`](08-PREGLED-IN-PLAN.md) Faza 5.

**Vhodni pogoj:** M6 (API shema, validator, hook registry, hitra povratna zanka).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M7.1 | Branje animacijske implementacije CustomNPC+; odločitev port vs novo | S |
| M7.2 | `rework/anim/` podatkovni model: `Animation`, `Frame`, `PartTransform`, interpolacija, looping | M |
| M7.3 | Runtime: deterministično izvajanje na serverju, sinhronizacija na klient (predlog: pošlji "predvajaj X od tika T", klient interpolira) | L |
| M7.4 | Render integracija z obstoječim `ModelData` / `ModelPartData` | M |
| M7.5 | Hooki: `onAnimationStart`, `onAnimationEnd`, `onFrameEnter`, `onFrameExit` | S |
| M7.6 | **Tekstovni format** + JSON Schema + `ANIMATION-GUIDE.md` s koordinatnim sistemom, imeni delov telesa, enotami in 10+ komentiranimi primeri | M |
| M7.7 | **Validator** `.\dev.ps1 validateAnim <datoteka>` z napakami po vrsticah | S |
| M7.8 | `/npcanim reload` v igri za preview brez restarta | S |
| M7.9 | Script API: `npc.getAnimator().play/queue/blendTo/stop`, tipizirano | S |
| M7.10 | Blending in prehodi med animacijami | M |
| M7.11 | *Opcijsko:* in-game urejevalnik | L |

**Izhodni kriterij:**
- animacija, napisana **samo** iz `ANIMATION-GUIDE.md` in sheme, brez gledanja v kodo,
  se validira in pravilno predvaja — to je dejanski test "AI jo razume"
- validator zavrne nepravilno animacijo z napako, iz katere je popravek očiten
- animacija se sproži iz Java skripte in hooki se sprožijo ob pravih frameih
- hitbox se med animacijo pravilno posodablja

**Tveganja:**
- Sinhronizacija animacij na klient je pri mnogo NPC-jih drag. Zato M7.3 pošilja ukaz,
  ne stanja, in se meri.
- Če se tekstovni format izkaže za pretežek za AI, je popravek v **dokumentaciji in
  validatorju**, ne v formatu.

---

## M8 — Migracija funkcij iz CustomNPC+ (izločeno, D-023)

> **2026-10-03: izločeno iz plana na zahtevo uporabnika** (D-023). Paketi spodaj ostanejo kot
> zapis za morebitno kasnejšo vrnitev; nobena druga faza nanje ne čaka. CustomNPC+ ostane
> **referenca** za M4 (letenje), M6 (Janino) in M7 (animacije). Pregled in plan:
> [`08-PREGLED-IN-PLAN.md`](08-PREGLED-IN-PLAN.md).

**Cilj:** **R3** — prenos izbranih funkcij iz 1.7.10 CustomNPC+ na 1.12.2.

**Vhodni pogoj:** M6 (hook registry — večina funkcij CustomNPC+ visi na hookih).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M8.1 | **Katalog**: prebrati CustomNPC+ repo, tabela `funkcija → razredi → 1.12.2 ekvivalent → trud → odvisnosti` v `docs/06-CNPCPLUS-KATALOG.md` | M |
| M8.2 | **Prioritizacija z uporabnikom** — izbor 5–8 funkcij; ostalo ostane v katalogu | S |
| M8.3 | Preverba licenčnih pogojev za prevzem kode | S |
| M8.4 | Razširjeni script hooki (do ~154), ki jih M6 še nima | L |
| M8.5 | Ability sistem s fazami | L |
| M8.6 | Dopolnitev taktičnih variant (primerjava z obstoječimi v 1.12.2) | M |
| M8.7 | Custom effects nad vanilla napitki | M |
| M8.8 | Party sistem | L |
| M8.9 | Profile sistem (več karakternih slotov) | L |
| M8.10 | HUD z quest trackingom | M |
| M8.11 | Ekonomija / auction house — **najnižja prioriteta** | XL |

M8.4–M8.11 se izvajajo **samo v obsegu, izbranem v M8.2**.

**Izhodni kriterij:** vsaka prenesena funkcija ima svoj NBT ključ, stikalo, test in vnos v
`docs/04-STANJE.md` z izvorom in licenco.

**Tveganja:** visoko. 1.7.10 → 1.12.2 ni port, ampak ponovna implementacija. Ocene iz
CustomNPC+ repozitorija se ne prenesejo.

---

## M9 — Chatbot

**Cilj:** **R8** — NPC-ji z AI pogovorom.

**Vhodni pogoj:** M6 (hooki `onChatRequest` / `onChatResponse`), M1 (varno shranjevanje).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M9.1 | Preverba: ali ima okolje strežnika izhodni internetni dostop | S |
| M9.2 | `rework/chat/net/` — async HTTP klient s **connect in read timeoutom**, retry, rate limit, circuit breaker. Nikoli na glavni niti. | M |
| M9.3 | `ChatProvider` vmesnik + Anthropic + poljuben OpenAI-kompatibilen endpoint (s tem so podprti tudi lokalni modeli) | M |
| M9.4 | Shranjevanje API ključa **v server config, ne v NPC NBT**; `.gitignore`; ključ nikoli na klient | S |
| M9.5 | Persona na NPC-ju: system prompt, ton, meje, dolžina odgovora; GUI | M |
| M9.6 | Spomin pogovora z omejitvijo (N izmenjav na igralca, TTL), ločen od NPC NBT | M |
| M9.7 | Rate limit in stroškovni limit na igralca in globalno | S |
| M9.8 | Fallback na statični dialog ob napaki, timeoutu ali limitu | S |
| M9.9 | Script hooka `onChatRequest` / `onChatResponse` | S |
| M9.10 | Preverba, kateri podatki gredo v prompt — brez UUID, IP, koordinat in inventarja brez izrecne nastavitve | S |

**Izhodni kriterij:**
- pogovor z NPC-jem deluje, strežnik med čakanjem ne lagga (izmerjeno)
- izpad omrežja, timeout in dosežen limit dajo fallback, ne crash
- API ključ ni v nobeni datoteki sveta, clone datoteki ali paketu na klient
- rate limit dokazano deluje

**Tveganja:** strošek. Brez M9.7 lahko en igralec generira velik račun.

---

## M10 — Release kandidat

**Cilj:** kandidat, ki se ga da varno pognati na uporabnikovem pravem svetu.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M10.1 | Celotna funkcionalna matrika iz `PLAN_IMPLEMENTACIJE.md` §6 | M |
| M10.2 | Meritve proti baselineu iz M2.6, vse scenarije | S |
| M10.3 | Test na **kopiji** uporabnikovega pravega sveta | M |
| M10.4 | Soak test, priporočeno 24 h z dejansko aktivnostjo | S |
| M10.5 | **Rollback vaja** — dokazano vrnjen original JAR + podatki | S |
| M10.6 | Uporabniška dokumentacija: nove nastavitve, migracija, znane omejitve | M |
| M10.7 | Changelog z eksplicitnim seznamom sprememb obnašanja | S |

**Izhodni kriterij:** `PLAN_IMPLEMENTACIJE.md` §6 "Sprejem" v celoti izpolnjen, plus
uspešna rollback vaja.

---

## M11 — Poveljevanje skupinam prek zemljevida

**Cilj:** skupina NPC-jev (npr. 200 istoimenskih) je na zemljevidu ena oznaka; igralec jo izbere,
klikne cilj in skupina gre tja v formaciji. Zahteva R10.

**Vhodni pogoj:** M4.14b in M4.10 zaključena; M5 dovolj, da 200+ NPC-jev v pohodu ne preseže
proračuna ticka; odgovor na Q14 (kateri zemljevid). Po odločitvi uporabnika (D-026) se faza
začne šele, ko je vse ostalo večinoma narejeno.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M11.1 | Določitev skupine: po imenu ali oznaki (`RwSquadId` NBT, privzeto brez); povezava z `Squad` iz M4.14a | S |
| M11.2 | Strežnik → klient: periodičen povzetek skupine (ime, središče, število, stanje) prek **ločenega kanala** `rework` | S |
| M11.3 | Adapter zemljevida (glej Q14): ena ikona na skupino, izbira | S–M |
| M11.4 | Klient → strežnik: ukaz »pohod na (x, z)«; preverba pravic (lastnik / op) | S |
| M11.5 | Dolg pohod: razrez poti sidra na vmesne točke (gradi na M4.10) | M |
| M11.6 | Odločitev in izvedba za neaktivne chunke: potujoč chunk ticket ali abstraktni pohod (nova odločitev D-0NN, ker se dotika »uspavanja oddaljenih NPC-jev«) | M–L |
| M11.7 | Robni primeri: voda, prepadi, grla, obtičali člani, obnašanje na cilju | M |
| M11.8 | Meritev: pohod 200 NPC-jev proti baselinu M2.6 (MSPT, alokacija) | S |

**Izhodni kriterij:** 200 NPC-jev doseže cilj, izbran na zemljevidu, na prizorišču z grlom in na
razdalji čez `NpcNavRange`; brez stikala je obnašanje enako originalu; MSPT p95 med pohodom
izmerjen in v dogovorjenem pasu.

**Tveganja:** neaktivni chunki (M11.6) so največja neznanka; abstraktni pohod potrebuje varno
shranjevanje stanja ob izklopu strežnika.

---

## Kaj je izrecno izven obsega

Do nadaljnjega, ne glede na to, kako mamljivo se zdi:

- port na 1.16.5 / 1.20.1 ali katerokoli drugo verzijo (D-005)
- menjava Jave 8 (Nashorn)
- večnitni AI
- deljen script engine med NPC-ji
- globalno redkejši NPC tick ali uspavanje oddaljenih NPC-jev
- sprememba omrežnega protokola / packet formata
- javna distribucija (odprta licenčna vprašanja, glej `01-ARHITEKTURA.md` §8)

Vsaka od teh je samostojen projekt s svojim kompatibilnostnim načrtom.

---

## Vzporednost

Če je časa več, se lahko hkrati vodita **dve ločeni veji**:

```
veja A (podatki in scripting):   M1 → M6 → M7 → M9
veja B (entiteta in gibanje):    M2 → M3 → M4 → M5
```

Stikališče je `EntityNPCInterface` (veja B) proti `ScriptContainer` (veja A) — prekrivata se
samo pri M6.6 (prej M5.1). Če se dela vzporedno, gre ta paket v vejo A in veja B ga ne dotika.
