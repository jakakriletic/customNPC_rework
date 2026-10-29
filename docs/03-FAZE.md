# 03 — Faze in delovni paketi

**Q92 (29. 10.): [obseg v1.0](10-OBSEG-V1.md) prevlada nad tem planom; ostalo je v2. M6.0–M6.12 zaključeni (D-090–D-094, Q191); naslednji ostanek M7 po raziskavi [M7.0](raziskave/M7.0-animacije-integracija.md) (D-095): ~~M7.V~~ (Q199) → ~~M7.9~~ (Q202) → ~~M7.5~~ (Q206); M7 v1 zaključen; M10.0 raziskava zaključena (D-096), ~~M10.1a~~ zaprt brez sprejema (Q220, vrstice v M10.1b), naslednji M10.1b; M7.8 v2. M3.8b in M4.15b/c zaključeni, M4.15a potrditev 2000 nedosegljivih odložena. Pri privzeto 0 cena ni pogoj (izmeri se enkrat); največ 2 poskusa na paket, kratek zapis dokazil.**

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
| **M5** | Performance AI: strežnik (M5-S), klient (M5-K, D-033) in vidnost NPC-jev na daljavo (M5-V, D-034) | R5 | L |
| **M6** | Scripting platforma v Javi | R7 | XL |
| **M7** | Animacijski sistem in AI avtorsko okolje — jedro v `customNPC_entities_mod` (D-024) | R4 | M |
| ~~**M8**~~ | ~~Migracija funkcij iz CustomNPC+~~ — **izločeno iz plana** (D-023) | R3 | — |
| **M9** | Chatbot | R8 | M |
| **M10** | Release kandidat | — | M |
| **M11** | Vojske in poveljevanje prek zemljevida (Xaero) in poveljniške palice — **zadnja faza**, ko je vse ostalo večinoma narejeno (D-026) | R10 | L |
| **M12** | Nalaganje skinov prek URL-ja: omejen prenos, timeouti, diskovni predpomnilnik, omejitev velikosti — na koncu, za M11 (zahteva uporabnika 26. 10.) | R5 | M |
| **M13** | Urejanje več NPC-jev hkrati: popravljeni in razširjeni povezani NPC-ji (Linked NPC), več izbir v seznamu palice — neodvisen, po M4 (zahteva uporabnika 26. 10.) | — | M |
| **M14** | Instanciran izris bližnjih NPC-jev: K10a (VBO, ista slika) → K10 (senčilnik, merilo vizualno enako, odločitev lastnika 26. 10.) — po M13, pred M6 | R5 | L |

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
| M3.8 | **R6**: `RwHitboxMode` ORIGINAL / SOLID / SMART (velikost + ščit) + NBT + ukaz `/rwhitbox` + testi; GUI v M3.8b (D-025, zahteva uporabnika 5. 10.) | **zaključeno 5. 10.** — scenarij `hb-run.ps1` H0–H8 zelen, 201/201 testov; [zapis](meritve/2026-10-05-M3.8-hitbox-nacini.md) |
| ~~M3.8b~~ | Display → Hitbox, obstoječe tri izbire; geometrija vrstic 21 px, fizika/NBT ista | **zaključeno 29. 10. (Q123–Q126), poskus 2:** unit/build/package in GUI 216/216 PASS na `b2719356…`, clone/restart šestih istih UUID, 18 PNG pregledanih; globalno 0/1 in HasHitbox potrjena. Prvi FAIL ostane reprodukcija; M3.9 gibanje igralca nepokrito. [Dokazila](meritve/2026-10-29-M3.8b-gui.md) |
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
kakovosti navigacije) za pakete M4.10–M4.13, **M4.16** (scenarij letenja in dometa) za vse pakete
letenja in M4.10.

**Plan po raziskavi M4.0 (20. 10., D-038):** [zapis](raziskave/M4.0-gibanje-in-letenje.md). Glavne
ugotovitve: vanilla 1.12 leteči NPC sledi poti **brez preverbe ovir** (`PathNavigateFlying.isDirectPathBetweenPoints`
vedno `true`, skok na zadnjo točko iste višine); `FlyingMoveHelper` je vanilla `GhastMoveHelper` (sunki
vsake 4 ticke, po prihodu do ~3,5 bloka drsenja, **[H]** model); cilj dlje od `NpcNavRange` vrne **brez
poti**, ne delne poti; CustomNPC+ ima isto iskanje kot vanilla 1.12, razlika je v sledenju in krmiljenju.
M4.1, M4.4, M4.9 in M4.12 zavrženi ali združeni, M4.11 pogojen, dodana M4.16 in M4.17.

**Vrstni red M4 za v1.0 (Q92, prevlada nad D-038):** ~~M4.2~~ (D-086) → ~~M4.3~~ (D-087) → M4.15a (obravnavan z omejitvami, D-088) → ~~M4.15b~~ (D-089) → ~~M4.15c~~ (Q122); tudi M3.8b zaključen, naslednji raziskava M6.0. Ostali nedokončani paketi spodaj so v2. M4.14b/M4.16/M4.16b/M4.10/M4.17 so že obravnavani; odprta FA merila in Q82/Q84 ostanejo zapisana, ne blokirajo v1.0.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| **M4.0** | **Raziskava izvedbe (D-029), pred vsemi ostalimi paketi M4.** Letenje (CustomNPC+, vanilla `PathNavigateFlying`/`FlyingNodeProcessor`, drugi modi z letečimi entitetami, 3D A* in alternative) in kopenska navigacija (izmerjeno: R2 je zastarela delna pot, M2.3; sledenje poti je v boju 48 % CPU, M5-S P1; Baritone kot izbirno ozadje, D-022). Za vsak paket M4.1–M4.14 odločitev ohrani/spremeni/zavrzi/dodaj, zapis v `docs/raziskave/`, prilagoditev tega razdelka | **narejeno 20. 10.** — [zapis](raziskave/M4.0-gibanje-in-letenje.md), D-038, Q31 |
| ~~M4.1~~ | ~~Branje CustomNPC+ flying implementacije; odločitev: port ali lasten 3D pathfinder~~ | **zavrženo — opravljeno v M4.0**: `FlyPathFinder` je 1.12 `FlyingNodeProcessor` na 1.7.10 (200 vozlišč, filter dometa, hevristika = kvadrat razdalje); uporabne ideje (žarek pri sledenju ≤ 6 blokov, upoštevana hitrost, zavoj ≤ 20°/tick, višinska meja, spust brez poti) gredo v M4.17, M4.3 in Q31. Brez prenosa kode (D-002) |
| ~~M4.2~~ | **Zaključeno 29. 10. (D-086, Q6/Q31/Q91):** `FlightMode` + root int `RwFlightMode`, stalni ID-ji 0/1/2, original brez ključa; manjkanje/neznan ID/napačen tip → original. Samo shranjena izbira; unit/build/package in M4.16 180/180 zeleni na `d7a9f810…`. Hover v M4.3; Momentum (M4.6) in GUI M4.2b sta v2 po Q92. [Dokazila](meritve/2026-10-29-M4.2-nacin-letenja.md) | S |
| ~~M4.3~~ | **Zaključeno 29. 10. (D-087, Q93–Q97), poskus 1:** `HoverMoveHelper` pri `RwFlightMode=1`, privzeto original; hitrost navigatorja × `MOVEMENT_SPEED`, yaw ≤20°, ustavljanje A/C drsenje 0, P 6/6 in C/A 0,522; save/restart 30 letečih potrjen. Unit/build/package in štirje sprejemni ID-ji zeleni, privzeto 180/180 = M4.16. Cena enkrat patrulja/boj p95 8,913/2,425 ms, podatek po Q92; poti čez ovire ostajajo omejitev, celoten M4 ni zaključen. `FLYING_SPEED` je že registriran (stara premisa o manjkajočem atributu ovržena). [Dokazila](meritve/2026-10-29-M4.3-hover.md) | S/M |
| ~~M4.4~~ | ~~Popravek `isNotColliding` — vzorčenje po hitboxu, obhod namesto `WAIT`~~ | **zavrženo kot samostojen paket**: ostanek ghasta; z vidno črto pri sledenju (M4.17) in `move()` v novem helperju ni potreben, v originalnem načinu ostane |
| M4.5 | **Odločitvena točka (spremenjeno, D-038):** iskanje poti za leteče. Izvede se **samo**, če M4.10 + M4.17 + M4.3 ne izpolnijo meril M4.16. Obseg: podrazred `PathFinder` za leteče po vzoru 1.21.1 (evklidska cena, hevristika ×1,5, proračun `FOLLOW_RANGE·16`, brez filtra dometa do cilja), po potrebi Lazy Theta\*. Brez oktalnega drevesa in **brez asinhronega iskanja** (D-012) | L → M |
| M4.6 | `MomentumMoveHelper` — inercija, omejen kotni pospešek, minimalna hitrost, dviganje/spuščanje; **zasledovanje točke vnaprej po poti** (»pure pursuit«) in redčenje poti, ker se točke na 1 blok in minimalna hitrost izključujeta. Obseg po Q6/Q31 | M |
| M4.7 | **Spremenjeno:** pitch vanilla že sinhronizira (`EntityTrackerEntry`); paket je render nagiba telesa in roll, izpeljan na klientu iz spremembe yaw (brez novega paketa ali `DataParameter`, **[H]**). Uskladitev z `customNPC_entities_mod` (D-024) | M → S/M |
| M4.8 | Vrata, zavetje in vodna navigacija za leteče NPC-je (trenutno zgodnji `return` pri `canFly()`) | **odloženo** — brez zahteve in meritve; `FlyingNodeProcessor` vrata že prehaja, manjka odpiranje. Na konec, samo na zahtevo · S |
| ~~M4.9~~ | ~~Meritve: strošek 3D pathfindinga proti obstoječemu, pri 50/200 letečih NPC-jih~~ | **združeno v M4.16**: cena se meri v vsakem paketu proti celici `leteci-200` |
| ~~M4.10~~ | **Zaključeno 26. 10. (seja 155, D-084, Q75–Q78)** — pod `RwPathResume` (privzeto 0): stikalo 0 = M4.16 (180/180), z nadaljevanjem faza A DF 0/6 → 6/6, Z 0/6 → 6/6, DW 6/6 pri 1200 tickih (pri 600 fizično prepočasen, Q78), P enako, V/L 0/6 (M4.17); M2.7 brez poslabšanja. [Zapis](meritve/2026-10-26-M4.10-nadaljevanje.md). Prvotni opis: **Nadaljevanje delne poti in cilj izven dometa** (premisa razširjena v M4.0). Vanilla A* se prekine po 200 vozliščih in vrne **delno** pot (`PathFinder.findPath:65`), dolžino pa omeji na `getPathSearchRange()` = `FOLLOW_RANGE` = `NpcNavRange` = 32 (`EntityNPCInterface.java:362`); **sosede, ki so od cilja dlje kot 32, zavrne** (`findPathOptions`), zato cilj ~33+ blokov stran vrne `null`, ne delne poti. Popravek: (a) ob prihodu na konec delne poti se pot obnovi, (b) pri cilju izven dometa vmesni cilj na črti proti cilju (≤ domet − 4); oboje dokler cilj ni dosežen ali dokazano nedosegljiv, z omejitvijo števila obnov. Kopenski in leteči. Upoštevati S5 (`RwPathNegCache` delne poti zavrne za 20 tickov). Prizorišče *daleč* iz M4.16. Pod stikalom, privzeto original. **V teku (seja 154, D-084, Q75–Q77):** `PathResume` + `RwPathNavigateGround`/nov `RwPathNavigateFlying`, stikalo `RwPathResume` (privzeto 0, `/rwpath nadaljuj`), unit +12 | S |
| M4.11 | **Lasten `NodeProcessor`** — realna cena diagonale (1,41 namesto manhattanskih 2) in pregledani malusi `PathNodeType`. Vanilla uporablja manhattansko ceno **in** hevristiko pri 8-smernem gibanju (`PathPoint.distanceManhattan:86`), zato poti sistematično bežijo od diagonal; od tod stopničasto cikcakanje. A/B proti merilom M2.7 | **pogojno, po M4.13 (D-038)**: premisa ni v meritvah (razmerje poti 1,131–1,151, M2.7); izvede se samo, če M4.13 brez rezanja vogalov pokaže daljše poti ali počasnejši prihod čez razpon · M |
| ~~M4.12~~ | ~~**Odločitvena točka: lasten `PathNavigate` z lastnim A\***. Izvede se **samo**, če M4.10, M4.11 in M5.6 po merilih M2.7 ne zadostujejo~~ | **zavrženo (D-038)**: stopnjo D pokrije izbirni Baritone (D-022, D-036); ožja različica za leteče je M4.5 |
| M4.13 | Mehkejše sledenje poti (**S15**): napredovanje po razdalji in geometrijskem testu smeri po vzoru 1.21.1, brez branja blokov v `pathFollow` (`PathNavigate:272-308`). **Edini kopenski paket z izmerjeno ceno:** sledenje poti je po S14b še **34,5 % ticka boja-500** ([M5.11](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md)). Sprememba gibanja (NPC ne reže več vogalov) → stikalo, privzeto original; A/B CPU **in** kakovost (M2.7 + proga *vogal* iz M4.16); o privzeti vrednosti po meritvi odloči uporabnik. Za njim pogojno M4.11 | S |
| ~~M4.16~~ | **Zaključeno 26. 10. (seja 151, D-081, Q68)** — original v treh ponovitvah, razpon 0 v vseh 180 veličinah: en `navigateTo` zid/vogal/labirint/daleč 0/6 (daleč `navig0` 0/6 tudi kopenski), osvežen zid 6/6 (215/270 tickov), vogal 0/6; prosta kontrola 6/6, drsenje po prihodu ≥ 1,76/2,38 bloka (mediana/max, spodnja meja), hitrost 0,5 proti 1,0 = 0,97 (ne velja); labirint brez premika. Pragovi izhodnega kriterija nespremenjeni. [Zapis](meritve/2026-10-26-M4.16-letenje.md). Prvotni opis: **Scenarij letenja in dometa (novo, D-038)** — vhod za vse pakete letenja in M4.10. Proge v `r2-run.ps1`: *zid* (obstoječa F), *vogal* (L-zid na isti višini), *daleč* (cilj 48 blokov, prosto; enako za kopenske), *labirint* (fiksna postavitev). Veličine: prispeli z enim `navigateTo` in z osveževanjem, čas do cilja, **drsenje po prihodu**, `cele`, hitrost proti `navigateTo(speed)`. Perf celica **`leteci-200`** (patrulja in boj) v `perf-run.ps1`. Tri ponovitve originala (D-016). Brez produkcijske kode. **26. 10. (seja 151, D-081):** proge v ločenem `letenje-run.ps1` (`r2-run.ps1` je stopnja matrike, ostane), šest prog (zid, vogal, labirint, daleč leteči/kopenski, prosta kontrola), faze en klic / osvežen / hitrost 0,5; celica `leteci-200` izločena v M4.16b. [Scenarij](scenariji/M4.16-letenje.md) | S |
| ~~M4.16b~~ | **Zaključeno 26. 10. (seja 152, D-082, Q69–Q71)** — varianti `leteci` (patrulja) in `leteci-boj` v `perf-run.ps1`, tri ponovitve originala, merili L1/L2: patrulja p95 8,651 (8,651–9,175) ms, `npc.us` 17,16; boj p95 2,228 (2,228–2,294) ms, `npc.us` 7,62; okvirno proti M2.6 patrulja dražja od stoječega `idle-200`, leteči boj cenejši od `boj-200`. [Zapis](meritve/2026-10-26-M4.16b-leteci-200.md). Prvotni opis: **Celica `leteci-200`** (patrulja in boj) v `perf-run.ps1`, tri ponovitve originala; izhodišče cene za M4.17/M4.3 (D-081) | S |
| ~~M4.17~~ | **Zaključeno 29. 10. (Q90, odločitev lastnika):** sprejet poskus 4 na `b8f98d28…` (11 ID-jev zelenih, 180/180, obe cenovni meji), privzeto 0; dodatni ID-ji Q87/Q88 se ne izvajajo. Zgodovina (seja 156, D-085, Q79–Q84): `FlightFollow` + prepis `isDirectPathBetweenPoints` v `RwPathNavigateFlying` pod `RwFlyLineOfSight` (privzeto 0). Poskus 1 (`75269123…`): stikalo 0 = M4.16, labirint se premakne (z M4.10 6/6), vogal 0/6 (vzrok iskanje → M4.5, Q82), z M4.10 zid 5/6 (Q84); cena patrulja v meji, **boj p95 +32 % — ni sprejet (Q83)**; poskus 2 (`b79530e1…`) = preverba unije pred koraki: letenje 180/180 = poskus 1, boj p95 2,818 > 2,812 — ni sprejet (Q85); poskus 3 (`346e8b08…`) = Forge dogodek trkov enkrat namesto v vsaki celici: letenje 180/180, alokacija +0,6 MB/s, boj p95 2,884 > 2,883 — ni sprejet (Q86); poskus 4 (`b8f98d28…`) = chunk enkrat na stolpec, zrak brez verige trkov: letenje 180/180 = poskusi 1–3, patrulja p95 9,175 ≤ 10,0925 in boj 2,687 ≤ 2,8116 ms; 11 testov zelenih, še 12 ID-jev poskusov 1–3 čaka rezultate na trenutni kodi (Q87), paket še ni zaključen. Seja 160 (Q88): aktivna naloga ima nov odtis `adf047b4…` in nima rezultatov, zato zahtevanih vseh 23 ID-jev na istem trenutnem odtisu, merila nespremenjena. [Zapis](meritve/2026-10-26-M4.17-vidna-crta.md). Prvotni opis: **Sledenje poti letečega NPC-ja z vidno črto (novo, D-038).** `RwPathNavigateFlying` (podrazred kot `RwPathNavigateGround`): `isDirectPathBetweenPoints` s pravo preverbo (premik hitboxa ali žarki po vogalih hitboxa), skok omejen na nekaj blokov; velik model napreduje po razdalji. Stikalo, privzeto original. A/B na progah *vogal* in *labirint*, cena na `leteci-200` | S |
| M4.14 | **Formacije** (novo 18. 9., na zahtevo uporabnika, D-017): skupina NPC-jev se premika kot ena enota — ena pot za vse (vodja), zaprta zanka hitrosti, stiskanje skozi ozka grla, sidranje na koncu. Ukaz `/rwsquad` in `FormationApi` za skripte; brez ukaza paket ni prijavljen in ne doda taska. Zasnova: [`06-FORMACIJE.md`](06-FORMACIJE.md) | M |
| M4.14a | Jedro (`SquadPlanner`, oblike, dodelitev mest, pot sidra) + adapter (`Squad`, `FormationMoveTask`, `SquadManager`, ukaz, API) + 21 testov v simulaciji | **narejeno 18. 9.** — prevedeno in zeleno v seji (D-014), v svetu ni pognano |
| ~~M4.14b~~ | ~~Preverjanje v svetu: F1–F12 in A/B FA1–FA6~~. **Zaključeno 26. 10. (seja 149, D-079) kot preverba, ne izboljšava:** na 984dc27a… vsi ID-ji izvedeni (harness-i/unit/build/package zeleni, sledi 1.664/8.960/8.960 popolne); padejo samo FA1/2 (merilo skupnega cilja proti 8 mestom; 8/8 mest OK), FA4 G/O 7,571–8,592/6,42 (geometrija kolone skozi vrata) in FA5 G 19 > 4 (člani ne sledijo skupni poti → M4.14d). Pragovi isti; F1–F12 odprti (ročno, uporabnik). Zgodovina: **Delno 22. 10. (seja 140, D-056–D-073)**: na c9edad1b… harness/unit/build/package uspejo, vseh 16 adapterjevih testov zelenih. A/B G/O DONE 237/40 in 8/8 mest; FA1/2, FA4 9,564/6,416 >6 ter FA5 G 29 >5 še padejo. D-073: trace-v1 popoln (1.664 zapisov), FA neuspehi ostajajo; manjkajoča recovery-harness-v2 in recovery-trace-v2 na isti kodi pred novim posegom. **26. 10. (seja 145, D-077):** navigator sled popolna, 5 izgub obvoza G je vanilla iztek s podedovanim `timeoutTimer`; ponastavitev ob novem iskanju člana, nov odtis čaka teste. **26. 10. (seja 147, D-078):** na 984dc27a… unit/build/package/navigator-harness uspejo; navigator sled potrdi D-077 (`iskanj_izgub` G 7 → 0). FA5 G 19 > 4, FA1/2 in FA4 8,041/6,417 še padejo. Koda zamrznjena, preostali ID-ji na istem odtisu, sledenje skupni poti v M4.14d. Sprejemni pogoji/pragovi isti; F1–F12 odprti. [Scenarij](scenariji/M4.14-formacije.md), [dokazi](meritve/2026-10-22-M4.14b-formacije.md) | S |
| M4.14d | Člani v PATH sledijo skupni poti enote (06-FORMACIJE §1, M5.6) namesto `tryMoveToXYZ`, kadar sta član in cilj ob njej; pravo iskanje samo ob obtičanju, članu daleč od poti in replanu. Najprej unit reprodukcije skupin iz razčlenitve D-078 (prvo iskanje kolone, opravičilo, premik točke kolone, prehod v SETTLING), nato navigator sled in A/B. Merilo FA5 G ≤ 1 + obnove + obtičanja; FA pragovi, scena in config isti (D-078). **Poskus 1 (način TRACK, D-080) izmerjen 26. 10. (seja 150) in zavrnjen (Q65):** na 61bf7d8f… FA5 G formalno uspe (11 ≤ 11, 12 ≤ 15), a obtičanja G 3–6 → 10–14 in G DONE 233–246 → 318/348 tickov; koda v `884175d`, produkcija povrnjena na `cbad3c3`. **Odprto, odloženo** do novega pristopa (gneča ob vratih); dodatno merilo: G DONE ≤ 246, obtičanja G ≤ 6. **Poskus 2 (D-083, Q72, seja 153):** iskanje samo ob dokazu (opravičilo ne vsili iskanja, preverba ravne črte), TRACK samo naprej in samo ob zaprti črti, eno iskanje na obtičanje; unit +12. **Izmerjen 26. 10. in zavrnjen (Q74):** na 6f39a760… G iskanj 18 → 4, obtičanja 3, a G DONE 288 > 246 in FA5 G 5 > 4 (FA4 G 9,326); koda v `bf02674`, produkcija povrnjena na `cbad3c3`. **Odloženo** do per-članske analize zamude ob vratih. [Zapis 2](meritve/2026-10-26-M4.14d-poskus2.md) [Zapis](meritve/2026-10-26-M4.14d-sledenje-poti.md) | S |
| M4.14c | Šele po M4.14b in M2.4: cena na tick pri 50/200 članih; parametri iz `06-FORMACIJE.md` §6 po meritvi | S |
| M4.15 | **Izbira navigacijskega ozadja v GUI NPC-ja** (D-036, zahteva uporabnika 15. 10.). Izbira na NPC-ju že obstaja (NBT `RwNavBackend`, ukaz `/rwnav`, ponovni priklop po `updateTasks` — M7/NB, D-022), v GUI pa je ni in igralec ne vidi, s čim NPC dejansko hodi. Vhod: M4.0 (primerjava Baritone : vanilla) in M4.15a | M |
| M4.15a | **Raziskovalno obravnavan 29. 10., sprejem lestvic neuspešen (D-088/Q106–Q107):** kompatibilnost 18/18 zelena, 12 celic boj/nedosegljiva ×500/1000/2000 ×0/1 enkrat po Q92. Obe lestvici exit 1 samo U1 pri 2000 nedosegljivih (881/800 <1000); potrditev te obremenitve in skupne porabe pomnilnika odložena. Pet veljavnih parov in fallback sonda podpirata neodvisni M4.15b: vanilla/Baritone, dejansko ozadje/razlog, brez obljube pospešitve ali kapacitete. Privzeto 0, brez ponavljanja ali sprememb pogojev/meril. [Dokazila](meritve/2026-10-29-M4.15a-baritone.md) | S |
| ~~M4.15b~~ | **Zaključeno 29. 10. (D-089, Q108–Q116):** AI → Movement → Backend, izbira vanilla/Baritone 0/1 in strežniški posnetek dejanskega navigatorja/razloga; prehodno stanje samo v odgovoru, legacy save ohrani izbiro, obstoječi NBT tipi/packet ID-ji/enum vrstni red isti. Privzeto 0; sedem controller testov zelenih na `d71d75d4…`, 34 trditev zaščite properties in 97/97 v vsaki od treh strežniških pogodb. Klik/reload ostane M4.15c; brez obljube kapacitete. [Dokazila](meritve/2026-10-29-M4.15b-gui.md) | M |
| ~~M4.15c~~ | **Zaključeno 29. 10. (Q117–Q122):** wand/GUI izbira 0/1, hoja/let/hoja, save/restart istega UUID; integrirano z/brez knjižnice 70/70 ×2, dedicated s knjižnico/klient brez nje 68/68, 30 PNG pregledanih. Vseh šest controller testov zelenih na `c91ad234…`; config/runtime isti, produkcija M4.15b ista. [Dokazila](meritve/2026-10-29-M4.15c-klient.md) | S |

**Izhodni kriterij** (merljiv na scenariju M4.16, D-038; pragovi z **[H]** se potrdijo ali popravijo
po treh ponovitvah originala):
- leteči NPC pride od A do B na progah *zid*, *vogal*, *daleč* (48 blokov) in *labirint* z **enim**
  `navigateTo`: 6/6 prispelih v vseh ponovitvah (original M4.16, tri ponovitve: zid, vogal, daleč in labirint 0/6 **[M]**)
- način Hover: drsenje po prihodu ≤ 0,25 bloka **[H]**; hitrost iz `navigateTo` velja
- način Momentum: NPC zavija po krivulji, ne v pravem kotu; ima minimalno hitrost
- celica `leteci-200`: cenovna meja izmerjeni razpon + 10 % velja za privzeti vklop; pri privzeto 0
  se cena izmeri enkrat kot podatek, brez cenovne meje (Q92); ostala funkcionalna merila ostanejo
- obstoječi leteči NPC-ji brez `RwFlightMode` (in z izklopljenimi stikali) se obnašajo **enako kot prej**

**Izhodni kriterij za formacije (M4.14):** F1–F12 in FA1–FA6 iz scenarija M4.14b; brez
tega paket ostane "v kodi", ne "narejen". M4.14 **ni** odvisen od M3.1: ne spreminja
`EntityNPCInterface`, ampak AI task doda od zunaj (D-017).

**Izhodni kriterij za kopenski del:** vsak od M4.10–M4.13 ima A/B meritev proti merilom
M2.7, ki presega merilni šum; kandidat, ki ga ne, se **zavrže** in se ne obdrži „za vsak
slučaj".

**Tveganja:** 3D pathfinding je klasično ozko grlo. Če se izkaže za predrag, je fallback
"smer + izogibanje oviram" brez polnega A*. Leteča cena danes ni izmerjena (perf scenariji
letečih nimajo) — zato celica `leteci-200` v M4.16 pred prvim posegom. M4.17 doda preverbo, ki je
original nima (vsak tick sledenja) — strošek je treba izmeriti, ne predpostaviti.

**Kaj je tu zavrnjeno in zakaj** (D-012): asinhrono iskanje poti, flow fieldi za skupinsko
gibanje in lasten gibalni sklad. Prvo je edini del tega sklopa z resnično visokim tveganjem
(svet se med iskanjem spreminja), drugi dve spremenita občutek gibanja vseh obstoječih
NPC-jev. Odprejo se šele, če po M4.10, M4.11 in M5.6 merila M2.7 še vedno padajo.

---

## M5 — Performance AI

**Cilj:** **R5** — merljivo več NPC-jev pri istem MSPT (**M5-S**, strežnik) **in** pri istem času sličice
(**M5-K**, klient). Od D-033 (14. 10.) sta obvezna oba dela; M5 je zaključen šele, ko sta zaključena oba.

**Vhodni pogoj:** M2.6 (baseline meritve). Brez njih se M5 ne začne.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| **M5.0** | **Raziskava preostanka M5 (D-029)** | **zaključeno 8. 10.** — [zapis](raziskave/M5.0-preostanek-performance.md); plan spodaj je prepisan po njem (D-030) |
| M5-S S1, S2 | Predzavrnitev tarč in poizvedba samo po igralcih (`RwTargetPrefilter`, D-027) | **zaključeno 6./7. 10.** — idle-500 p95 44 → 5,2 ms |
| M5-S P1 | Profil JFR strežniške niti (idle/boj/skripte × 500) | **zaključeno 8. 10.** |
| **M5.10** | **Odločilni poskus za boj:** števec ns okoli `isDirectPathBetweenPoints` in celega `pathFollow`, štetje kandidatov (`/rwpath cas 1`, `perf-run -RwPathCas`), pogoj veljavnosti A/B boja (`dev/tools/ab-boj.js`) | **zaključeno 9. 10.** — delež `pathFollow` v boju-500 **47,7–53,6 %** ticka (prag 25 % presežen); na `pathFollow` vedno 0 ali 1 kandidat, vsak uspe; šum boja je v delu na klic (µs/kandidata razpon 24,5 %), ne v številu klicev (1,1 %) → pogoj ±10 % je nujen, ne zadosten. [zapis](meritve/2026-10-09-M5.10-delez-sledenja-poti.md) |
| ~~M5.11~~ | **S14b — pomnjenje tipa vozlišča za čas enega klica `isDirectPathBetweenPoints`** v `RwPathNavigateGround` (ključ = pozicija; entiteta in svet med klicem enaka; `currentEntity` nastaviti kot vanilla). Po M5.10 zadošča en klic: v boju ima `pathFollow` en kandidat, ostane ~3× ponavljanje istih mest v klicu (model); kandidati so ~49 % ticka, ~56 % alokacij boja je v teh ocenah. Enakost z obstoječim načinom preverbe (2). **A/B pri istem boju:** preklapljanje načina med tekom v izmenjujočih se oknih s števci ločeno po načinu, ključna veličina µs na kandidata; nato MSPT s ponovitvami in `ab-boj.js`. Kandidat pod šumom gre iz kode (tudi S14) | **zaključeno 9. 10.** — enakost 0 / 1,25 M; v istem boju µs/kandidat 0,40×; celi zagoni boj-500 MSPT povp 8,73 → **5,88 ms**, p95 9,96 → 7,21; S14 v šumu in odstranjen, `RwPathFollowCache` 1 = S14b, privzeto 1 (D-032). [zapis](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md) |
| ~~M5.12~~ | **S4 — `onCollide` brez opazovalca:** brez skripte `collide` in brez poslušalca `CollideEvent` na `WrapperNpcAPI.EVENT_BUS` preskočiti poizvedbo (idle 12 %, skripte 15 % CPU). Nadomesti M5.3 | **zaključeno 9. 10.** — NPC-ji s skriptami ostanejo original (`runScript` ima stranske učinke); idle-500 p95 5,24 → 3,93 ms, p99 6,16 → 4,13, povp −14 % (v šumu); `RwCollideSkip` privzeto 1 (D-032). [zapis](meritve/2026-10-09-M5.12-oncollide-brez-opazovalca.md) |
| ~~M5.13~~ | **S7 — prejemniki utripa oči** po `world.playerEntities` z istim testom presečišča namesto AABB 160 blokov (idle 6,5 %) | **zaključeno 10. 10.** — iskanje prejemnikov 55,42 → **0,12 µs** (A/B v istem zagonu, razpon iskanj 0,2 %); idle-500 celi zagoni (3 + 3) MSPT povp 2,824 → **2,570 ms**, p50 2,621 → 2,359, p95 4,325 → **3,932**, p99 4,719 → 4,325, µs/NPC 5,54 → 5,05 — vsakič nad šumom; enakost 0 neujemanj (4.327 primerjav v svetu + 200.000 leg v testu); `RwBlinkRecipients` privzeto 1 (D-032). [zapis](meritve/2026-10-10-M5.13-prejemniki-utripa-oci.md) |
| ~~M5.9~~ | **Scenarij nedosegljive tarče** (`perf-run` varianta): NPC-ji s tarčo, do katere ni poti (stolp/zid). Edina podlaga za M5.2, M5.6 in S5 — v obstoječih celicah je iskanje poti 2 % | **zaključeno 10. 10.** — varianta `nedosegljiva` (pas ograje okoli skupine B: tarča vidna, brez poti), instrumentacija `PathSearch` (`/rwpath iskanje 1`). Izid: iskanje poti **2,45 % ticka pri `boj`-500 proti 51,18 % pri `nedosegljiva`-500**, eno iskanje 3,5 proti **180,5 µs** (vanilla A* porabi ves proračun 200 vozlišč), MSPT 9,23 → **17,73 ms**, alokacija 91 → **293 MB/s**, 0 celih poti. **M5.2, M5.6 in S5 dobijo podlago.** [zapis](meritve/2026-10-10-M5.9-nedosegljiva-tarca.md) |
| ~~M5.2~~ | *Pogoj M5.9 izpolnjen 10. 10.:* AI budget / scheduler novih izračunov poti na tick, poštena vrsta. **Brez** zmanjšanja tick frekvence NPC-jev; sprememba obnašanja → stikalo, privzeto original (D-007) | **zavrženo 16. 10.** (D-037): S5 privzeto vklopljen odstrani isti strošek brez zakasnitve NPC-jev s potjo |
| ~~M5.6/S5~~ | *Pogoj M5.9 izpolnjen 10. 10.:* negativni predpomnilnik (S5) — **zaključeno 10. 10.**: `nedosegljiva`-500 MSPT povp **17,854 → 14,462 ms (−19 %)**, p95 22,02 → 18,35, alokacija 292,5 → 135,2 MB/s, preskočenih 17,46 iskanj na tick (35 %); **ni enakovreden originalu** (pri trajanju 20 tickov bi se odgovor razlikoval v 59 %, pri 4 tickih ni preskokov), zato privzeto 0 in **Q21** za uporabnika. [zapis](meritve/2026-10-10-M5.6-negativni-predpomnilnik.md). Deljenje poti (druga polovica paketa) ostane odprto. Prej: Premisa »največji dobitek v navigacijskem sklopu« je za boj ovržena (P1: 2 %), za nedosegljivo tarčo pa potrjena (51 %). **S5 je najcenejši prvi korak** (65 % iskanj vrne delno pot, 35 % `null`); sprememba obnašanja → stikalo | M |
| ~~M5.17~~ | **S14c — pomnjenje tipa vozlišča med celim iskanjem poti** — **zaključeno 10. 10.**: enakost 239,5 M primerjav ocen / **0 neujemanj**; 184,0 → **144,9 µs** na iskanje (−21 %), `nedosegljiva`-500 MSPT povp 17,959 → **15,253 ms (−15 %)**, alokacija 291,5 → 162,8 MB/s, delež zadetkov 52,5 %; `boj`-500 nevtralen (zadetkov 35 %, iskanje je tam 2,45 % ticka), zato **privzeto 1** (D-032). [zapis](meritve/2026-10-10-M5.17-pomnjenje-med-iskanjem.md). Prej: (ne le med `isDirectPathBetweenPoints`, kot S14b): v `findPath` isto pozicijo oceni večkrat (vsako vozlišče preveri sosede, sosedje se prekrivajo). Enak dokaz enakosti kot S14b (ena nit, svet med klicem nespremenjen, isti `currentEntity`, isti `blockaccess`), zato **brez spremembe obnašanja** → kandidat za D-032. Tarča: 190,5 µs na iskanje v `nedosegljiva`-500 (M5.9), kjer je iskanje 51 % ticka; v `boj`-500 je iskanje 2,45 %, zato tam dobitka ni pričakovati. Dodan 10. 10. po M5.6 | S |
| ~~M5.14~~ | **S16 — senčna polja `EntityDataManager`** za CNPC-jeve ključe (`Attacking`, `IsDead`, `Walking`), osvežena v `notifyDataManagerChange`. Idle ≤ ~5–8 % | **zaključeno 10. 10.** — enakost **52,0 M primerjav / 0 neujemanj** (24,0 M idle + 27,9 M boj; ~20 branj na NPC na tick); idle-500 MSPT povp 3,023 → **2,863 ms (−5,3 %)**, p95 4,588 → 4,325, µs/NPC 5,92 → 5,60 — nad šumom; boj ugoden (mediana −15 %), a v šumu; ključi `Attacking`, `IsDead`, `Walking`, `FactionData`; **privzeto 1** (D-032). [zapis](meritve/2026-10-10-M5.14-sencna-polja.md) |
| ~~M5.15~~ | **Infrastruktura vanilla posegov (D-031):** lasten coremod v CNPC jarju (`rework/core`: `IFMLLoadingPlugin`, `IClassTransformer`), manifest `FMLCorePlugin` + `FMLCorePluginContainsFMLMod` v `devRuntimeMod` in `buildPatchedMod`, imena SRG v produkciji in MCP v dev okolju, `verify-package` pozna nove vnose. Brez posega v obnašanje (prazen transformer z dnevnikom), dimni test dev server + zapakiran jar | **zaključeno 11. 10.** — dev: dimni test M0.5 vseh 18 preverb zelenih, coremod naložen (marker `RWCORE`, `runtimeDeobf=0`, imena MCP), zagon strežnika 0,72–0,92 s (prej 0,93–0,95); jar: `verify-package` PASS (66 = 65 razredov + manifest, razlika manifesta preverjena vnos za vnosom), coremod se ne sklicuje na noben CNPC/vanilla razred (`javap`, constant pool); ukaz `/rwcore`. **Zagon na pravem Forge strežniku ni preverjen** (dev je deobfuskiran, projekt pravega strežnika nima) → Q22. [zapis](meritve/2026-10-11-M5.15-coremod-infrastruktura.md) |
| ~~M5.16~~ | **`Entity.move` brez odvečne poizvedbe entitet za trke:** `World.getCollisionBoxes(Entity, AABB)` za vsak premik poišče entitete v kvadru, čeprav prispevajo škatlo samo razredi, ki prepišejo `getCollisionBoundingBox`/`getCollisionBox(Entity)` (čoln, minecart, shulker, NPC pod `RwHitbox`, modni). Števec takih entitet po svetu (`IWorldEventListener.onEntityAdded/Removed`, razred preverjen z refleksijo enkrat) — poizvedba se preskoči samo, ko je števec 0 **in** razred premikajoče se entitete ne prepiše `getCollisionBox(Entity)`; `GetCollisionBoxesEvent` ostane. Prepisan razred se šteje konzervativno (tudi če včasih vrne `null`). Idle 11,8 % CPU v gosti mreži [M], v redkem razporedu manj [H]. Velja za vse entitete, ne le NPC-je | **zaključeno 14. 10.** — prvi pravi poseg v vanilla bajtkodo (ena instrukcija: `INVOKEVIRTUAL getEntitiesWithinAABBExcludingEntity` → `INVOKESTATIC CollisionGate.entitiesForCollision`); enakost dokazana po kodi (v vanilli prispevata škatlo samo `EntityBoat`, `EntityMinecart` in `EntityShulker`, CNPC NPC-ji ne), s 15 testi (4 nad pravimi bajti `net.minecraft.world.World`) in v svetu (**945.162 primerjav / 0 neujemanj**); idle-500 (5 + 5 ponovitev, ločeni razponi) MSPT povp 2,787 → **2,540 ms (−8,9 %)**, p50 2,556 → 2,294, p95 4,194 → **3,867**, µs/NPC 5,47 → 4,98, alokacija 20,7 → 20,0 MB/s, **100 % klicev preskočenih** (505 na tick); boj-500 brez regresije (mediane rahlo nižje, v šumu); **privzeto 1** (D-032). [zapis](meritve/2026-10-14-M5.16-trki-brez-poizvedbe.md) |
| ~~M5.8~~ | **Kapaciteta proti baselineu** (izhodni kriterij): največji N pri MSPT p95 ≤ 25 in ≤ 50 ms, original proti vsem sprejetim stikalom, idle in boj, gosta mreža in redek razpored (razmik ≥ 3 bloke) | **zaključeno 16. 10.** — [zapis](meritve/2026-10-16-M5.8-kapaciteta.md) |
| ~~M5.1~~ | Globalni script lock → **preseljen v M6.6** (D-030): `ScriptContainer` je v skripte-500 1,8 % CPU, lock je nesporen; ostane zaradi pravilnosti, brez cilja MSPT | — |
| ~~M5.3~~ | Deduplikacija po chunku → **zavrženo** (D-030), nadomesti M5.12 (S4) | — |
| ~~M5.4~~ | Alokacijska higiena → **zavrženo kot samostojen paket** (D-030): `DataTimers` ~0,8 MB/s, ostalo le pri skriptah — pod šumom | — |
| ~~M5.5~~ | Meritev po vsakem → ni paket, je pravilo (D-016) | — |
| ~~M5.Z~~ | **Regresijski prehod za zaprtje M5 (D-054, D-055, Q35/Q36)** — **zaključeno 22. 10.**: controller unit/build/package ter `matrika-run.ps1 -Privzeto 0/1` (po 5/5 stopenj; build ločeno) in novi `sled-run.ps1` zeleni na `c3e19fe4…`. Sledi 97/75 dogodkov in 19 razmikov timerja 7 po 8 tickov identični; B3 ohranjen. Napačna oznaka M2.7 popravljena: M2.7 je navigacijska sonda, ne timer sled. Config nespremenjen, diagnostična lastnost obnovi privzete vrednosti kode. [Zapis](meritve/2026-10-22-M5.Z-regresija.md); nepokrite vrstice matrike ostanejo nepokrite | S–M |
| ~~M5.7~~ | Prostorski indeks → **zavrženo** (D-030, chunk sekcije so že indeks; drag je gostota mreže); render LOD → K5 v M5-K | — |

Vrstni red M5-S: **~~M5.10~~ → ~~M5.11~~ → ~~M5.12~~ → ~~M5.13~~ → ~~M5.9~~ → ~~M5.6/S5~~ → ~~M5.17 (S14c)~~ → ~~M5.14 (S16)~~ → ~~M5.15~~ → ~~M5.16~~ → ~~M5.8~~** → **~~M5-K.0~~ → ~~M5-K.1~~ → ~~K14~~ (zavržen 19. 10.) → ~~K14b~~ (19. 10., Q29; vklop Q30) → ~~K4~~ (19. 10., `RwLabelCache` privzeto 1) → ~~K3~~ (zavržen 21. 10., A/B brez koristi, `RwEyesList=0`) → ~~K5~~ (L1 21. 10., `RwLodStart` privzeto 0, D-039) → ~~M5-V.1~~ (21. 10., D-043) → ~~M5-V.2~~ (21. 10., D-044) → ~~M5-V.3~~ (21. 10., D-045) → ~~M5-V.4~~ (21. 10., D-046) → ~~M5-V.5~~ (21. 10., D-047) → ~~M5-V.6~~ (21. 10., D-048) → ~~M5-V.7~~ (22. 10., D-050) → ~~M5-V.8~~ (22. 10., D-051) → ~~M5-V.8b~~ (22. 10., `RwFarView` privzeto 2) → ~~K12~~ (22. 10., D-052, `RwRenderBox` privzeto 1) → ~~ponovna ocena K13/K10~~ (22. 10., D-053: K13 zavržen, K10 odložen izven M5) → ~~K3c~~ (22. 10., čistilni korak) → ~~M5-K.2~~ (22. 10., sprejem klienta, D-054) → ~~M5.Z~~ (22. 10., regresijski prehod, zaprtje M5, D-055)** (D-033, D-034, D-035, D-053, D-054); M5.2 po odgovoru na Q21. Celica `nedosegljiva` gre v M5.8 (kapaciteta brez nje je optimistična).
Sledenje poti brez branja blokov po vzoru 1.21.1 (S15, sprememba gibanja) ni v M5, ampak vhod za M4.13.
M5-K (klient) je od D-033 obvezen del M5 in sledi takoj za M5.8 — glej razdelek **M5-K** spodaj. Obvezen modul **M5-V** (vidnost NPC-jev do 24 chunkov z impostorji, D-034) gre po Q25 (D-035) takoj za M5-K.1 in K5.
Vanilla `Entity.move` (idle 26–29 %): Q16 odločeno → D-031 (lasten coremod v CNPC jarju), paketa M5.15 in M5.16.

### M5-K — klient (D-033)

Izhodišče je raziskava [09](09-PERFORMANCE-RAZISKAVA.md) (5. 10.): klientskih meritev **ni**; en gol NPC po
kodi stane ~18 `glCallList`, 25–40 matričnih operacij in nekaj neposrednih izrisov (oči, senca, ime),
pri 300 vidnih NPC-jih ≈5.400 klicev listov na sličico — obremenitev procesorja in gonilnika, ne GPU
**[H, iz kode]**. Kot pri M5-S (profil JFR je obrnil vrstni red) se vrstni red K-paketov potrdi šele z
meritvijo.

| ID | Paket | Vel. |
|---|---|---|
| ~~**M5-K.0**~~ | **zaključeno 16. 10.** ([zapis](meritve/2026-10-16-M5-K.0-harness.md); različice videza in ponovitve v M5-K.1) **Harness M2.4r** (K0): razvojni klient z igralcem na svetovih M2.4 (50/200/500, kasneje več), fiksna kamera (teleport, yaw/pitch), brez view bobbinga in VSynca, fiksna razdalja upodobitve, ločljivost in GUI; ~30 s ogrevanja, 60 s beleženja `System.nanoTime()` razlik v `RenderTickEvent`; poročilo p50/p95/p99 časa sličice, FPS, 1 % in 0,1 % low, `countEntitiesRendered`, GC, število tekstur; različice gol NPC, oklep + orožje, ime + naslov, oblački, delci, URL skini; tri ponovitve (D-016). Merila v slogu P1–P9 (vidnih NPC-jev je res N, kamera res fiksna, brez tujega procesa) | M |
| ~~**M5-K.1**~~ | **zaključeno 18. 10.** ([zapis](meritve/2026-10-18-M5-K.1-baseline-profil.md)) — baseline 5 celic × 3 ponovitve (idle-50/200/500, boj-200/500; p50 2,15 / 6,57 / 11,57 / 5,78 / 12,55 ms); napaka pogoja M5-K.0 popravljena (F1 skrije imena, zdaj GUI viden); JFR za render nit ne zadošča (38 vzorcev), zato lasten vzorčevalnik `StackSampler` (`k-run -Vzorci 1`): NPC-ji ~90 % render niti, model 31–38 %, **pokrivalo 19–26 %** (nov K14), imena 20 % pri 200 v dometu, oči 10–14 %. Vrstni red ni obrnjen; K14/K4/K3 pred K5 → **Q28**. Prej: Baseline in profil klienta: original pri 50/200/500 vidnih NPC-jih; profil render niti — kje gre čas sličice. **Potrdi ali obrne** vrstni red spodaj (D-029) | S |
| ~~M5-K K14~~ | **zavrženo 19. 10.** ([zapis](meritve/2026-10-19-M5-K-K14-pokrivalo-en-list.md)): vseh 6 delov `ModelHeadwear` v enem display listu da **isto sliko** (0 razlik pikslov in globine), a p50 idle-500 −1,4 % v šumu, idle-200 +0,8 %. Profil z enim listom: pokrivalo še vedno 26 % — čas je v gonilniku pri **816 ogliščih na NPC**, ne v klicih. Zgornja meja brez 6 delov: idle-500 p50 −23 %. Ostane prenos 1:1 `Model2DRenderer`/`ModelHeadwear` (`b896cfc`); koda poskusa in preverba pikslov v `03ab3bc` | S |
| ~~M5-K K14b~~ | **zaključeno 19. 10.** ([zapis](meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md)) — `RwHeadwearSkip`, privzeto **1 od 21. 10.** (Q30 → D-049; prej 0): pokrivalo izpusti štirikotnike, ki vzorčijo samo prozorne teksle; idle-500 p50 −25 %, idle-200 −18 %, preverba pikslov 7 serij 0 razlik; strogo enaka različica (z robom) v šumu, zavržena. Prej: **Pokrivalo brez prozorne geometrije** (dodano 19. 10. po K14, **Q29**): list pokrivala na teksturo, ki izpusti dele in trakove ekstruzije `renderItemIn2D`, ki vzorčijo samo teksle z alfo 0 (alfa test jih zavrže, globine ne pišejo → ista slika). 41 % od 239 humanoidnih skinov CNPC nima nobenega neprozornega teksla pokrivala, prazni so 60 % delov in 70 % trakov. Dobitek do zgornje meje −23 % p50 (idle-500). Odprto: branje alfe (`SimpleTexture`, URL skini asinhrono, menjava skina), OptiFine/senčilniki brez alfa testa. Dokaz: `HeadwearCheck` iz `03ab3bc` (`git revert 4370184`) z dodanim načinom | S–M |
| ~~M5-K K5~~ | **L1 zaključen 21. 10.** ([zapis](meritve/2026-10-21-M5-K-K5-lod-L1.md)) — `rework/render/Lod`, `RwLodStart` (bloki, privzeto **0**, D-039 / Q32): onkraj praga brez oči, ploščato pokrivalo modela namesto 3D, brez imena; idle-500 pri 32 blokih p50 −24 %, p95/p99 −21 %, idle-200 v šumu; K15 potrdi delež L1 60,6 %. Ni narejeno (ponovna ocena po M5-V): največje število upodobljenih, prekrivne teksture/delci, histereza praga. Prej: LOD in proračun upodobitve po razdalji — stopnja **L1** (model brez oči, glinta, prekrivne teksture, delcev, imen) in največje število upodobljenih NPC-jev. **Impostor (zadnja stopnja) je od 14. 10. v M5-V** (L2/L3, skupen atlas in izris). Spremeni videz → stikalo, privzeto original | M |
| ~~M5-K K12~~ | **zaključeno 22. 10.** ([zapis](meritve/2026-10-22-M5-K-K12-okvir.md), D-052 / Q33) — ~~Okluzija: najprej združljivost z Entity Culling (render bounding box velikih NPC-jev, `isInRangeToRenderDist`), lastna izvedba samo, če ne zadošča~~. `RwRenderBox` (privzeto **1**): okvir upodobitve = hitbox ∪ okvir modela (Entity Culling ga bere prek RenderLib), NPC brez hitboxa in ležeč NPC dobita domet stoječega; K18 zeleno, A/B idle-500 v šumu. **Lastna okluzija zavržena** (Entity Culling zadošča in bere naš okvir) | S |
| ~~M5-K K13~~ | **zavrženo kot samostojen paket 22. 10.** (D-053, [ponovna ocena](raziskave/M5-K-K13-K10-ponovna-ocena.md)): vezava teksture v profilu M5-K.1 ni merljiva, zgornja meja ~1–2 % pod šumom; L2/L3 imata lasten atlas. Ostane le kot podkorak K10. Prej: Skini v teksturnem nizu/atlasu — ena vezava teksture za vse NPC-je; pogoj za K10 | M |
| ~~M5-K K10~~ | **26. 10.: prestavljeno v M14** (odločitev lastnika: merilo vizualno enako, vklop). **Odloženo izven M5 22. 10.** (D-053): premisa »manj klicev« z K14/K3 delno ovržena (strošek so oglišča v gonilniku), po D-051 so modeli samo znotraj 56 blokov, lasten GLSL za fiksno pot ne bo piksel-enak. Ponovno odprtje, če M5-K.2 ali poročilo pokaže ≥ 300 modelov znotraj 56 blokov s p95 > 16,7 ms ob privzetih stikalih; prvi korak tedaj poskus **K10a** (deli modela iz VBO, fiksna pot). Prej: Instanced renderer: vsi vidni NPC-ji istega modela v en klic; največji vzvod pri tisočih vidnih in največje tveganje (OptiFine, senčilniki, `customNPC_entities_mod`) | L |
| ~~M5-K K3c~~ | **zaključeno 22. 10.** — ~~**Čistilni korak K3** (D-053): odstrani kodo zavrženega poskusa `RwEyesList` (`EyesList`, `EyesCheck`, ključ harnessa `eyesList`, veja v `LayerEyes`); popravi besedilo komentarja `RwRenderBox` in javadoc `RenderBox` (64/38,4 blokov pri RD 8, 1,0/80 pri RD 10). Brez spremembe obnašanja (`RwEyesList` je bil privzeto 0); dokaz `unit`/`build`/`package`~~ Odstranjeni tudi `EyesListTest`, izločitev v `build.gradle`, parametra `k-run.ps1 -Oci`/`-PreveriOci` in merili K13/K14; `LayerEyes` je spet 1:1 z 7881e50 + K5 (`Lod.far`). Zgodovinski test ID-ji `k3-*` in `audit/m5k3-ab.ps1` ne delujejo več (parameter `-Oci` odstranjen) | S |
| ~~M5-K.2~~ | **zaključeno 22. 10.** ([zapis](meritve/2026-10-22-M5-K.2-sprejem-klienta.md), D-054 / Q34) — p95 idle-200 7,095 → **5,356 ms**, idle-500 12,825 → **9,817**, dalec-200 5,405 → **1,851**, dalec-500 13,017 → **2,942**; 1 % low 102,5 → 139,8 / 54,4 → 67,0 / 134,9 → 364,6 / 50,2 → 259,6 FPS; vsi razponi ločeni, brez regresije; pogoj K10 ni izpolnjen (500 modelov, p95 9,8 ms). Harness: vsa stikala eksplicitno prek posla (`dev/run` ostane 0), K12 iz geometrije, K16 natanko 0 brez `-Dalec 1`. Prvotni opis: ~~**Sprejem klienta (izhodni kriterij M5, D-053):** A/B 3 + 3 idle-200 in idle-500 (prizorišče M5-K.1) in `-Dalec 1` (prizorišče M5-V.6), original proti vsem sprejetim privzetim stikalom (`RwHeadwearSkip`, `RwLabelCache`, `RwFarView=2`, `RwRenderBox`); zapis p50/p95/p99 in 1 % low~~ | S–M |
| ~~M5-K K4~~ | **zaključeno 19. 10.** ([zapis](meritve/2026-10-19-M5-K-K4-napisi-display-list.md)) — `RwLabelCache`, **privzeto 1** (D-032): ime in naslov iz display lista od drugega izrisa niza; preverba pikslov 0 razlik, idle-200 p50 −7,0 %, idle-50 −6,4 % (razponi ločeni). Dobitek ~⅓ stroška napisov; ostanek je izvajanje ~34 znakov v gonilniku | S |
| ~~M5-K K3~~ | **zavrženo 21. 10.** ([zapis](meritve/2026-10-21-M5-K-K3-oci-display-list.md)): geometrija oči iz display lista (`RwEyesList`, 49bd247) da isto sliko (0 razlik, tudi ×8), a A/B 3 + 3 brez koristi: p50 idle-200 6,451 → 6,644 ms (+3,0 %, razpona ločena v slabšo smer), idle-500 11,435 → 11,754 (+2,8 %, prekrivanje), p99 idle-500 +6,3 % (ločena, slabše). `RwEyesList` ostane 0; koda poskusa odstranjena v K3c (22. 10.). Ostane 1:1 prenos `LayerEyes` (7881e50) | S |
| M5-K K1, K2, K6–K9, K11 | Manjši paketi iz [09](09-PERFORMANCE-RAZISKAVA.md) (delci, mikropopravki, oči, imena in oblački, skini, omrežje, morph, animirani NPC-ji, redkejši klientski tick). **Po profilu M5-K.1 (18. 10.):** K4 (imena, ~8 µs na napis, 20 % pri 200 v dometu) in K3 (oči, 3,5 µs na NPC, 10–14 %) sta merljiva; K1, K2, K6 v celicah M2.4 nista (brez delcev in URL skinov), uvrstita se, ko prizorišče to vsebuje. **22. 10. (D-053):** K2 in K11 zavržena (ostalo v `doRender` 3–7,5 %, klientski tick ≤ 3,3 %, pod šumom); K1, K6–K9 odloženi do prizorišča ali poročila, ki pokaže strošek | S–M |

### M5-V — vidnost NPC-jev na daljavo, impostorji do 24 chunkov (D-034)

Zahteva uporabnika 14. 10.: NPC-ji naj bodo vidni do **24 chunkov** kot nekaj pikslov velika slika, brez
vpliva na performance; modul je **obvezen**. Raziskava: [M5-V.0](raziskave/M5-V.0-daljna-vidnost.md).

**Odločitve uporabnika 14. 10. (Q23–Q25 → D-035):** po sprejemu (M5-V.8) **privzeto vklopljeno**
(strežnik `RwFarView=1`, klient `RwFarView=2`); cilj je **dedicirani strežnik**; M5-V gre **takoj za
M5-K.1 in K5**, pred K12, K13 in K10.

**Posledica cilja "strežnik":** za 24 chunkov mora biti `view-distance=24`. Minecraft 1.12 nima ločene
razdalje simulacije (prišla je v 1.18), zato vsak naložen chunk tudi tika — pri 24 je to 49² = 2.401 chunkov
na igralca proti 441 pri 10 (**5,4×**), z vsemi entitetami in polnim AI NPC-jev. To je vanilla cena, ki je
M5-V ne more odpraviti (uspavanje oddaljenih NPC-jev je v R5 prepovedano); M5-V.1 jo izmeri, da je odločitev
operaterja informirana. Na strežniku z manjšim `view-distance` je doseg M5-V `(view-distance − 1) · 16`.

Ključna ugotovitev raziskave: NPC-je danes vidiš do **±64 blokov (4 chunki v vsako smer)**, ker jih CNPC
registrira s `tracker(64, 3, true)` in jih strežnik dlje **ne pošlje** **[K]**; klient bi jih pri RD ≥ 20
narisal do 160 blokov. Samo klientski LOD zato ne zadošča. Dvig dometa sledilnika (varianta A) je drag
(33× površina, GZIP spawn NBT na NPC, polna klientska entiteta) **[K/H]**. Izbrana je **varianta B**:
NPC-ji onkraj sledilnika gredo na klient kot **lahki nadomestki** (~16 B, ~1 Hz, samo ob spremembi) po
lastnem kanalu `rwfar`, videz enkrat na videz; klient speče **impostor** (8 smeri, atlas z mipmapi —
"slabša tekstura z razdaljo") in vse oddaljene NPC-je nariše v **enem klicu**. Isti impostor nadomesti
model tudi pri sledenih NPC-jih onkraj ~56 blokov (L2, FPS dobitek). Stopnje: L0 original, L1 = K5,
**L2** impostor iz prave entitete, **L3** impostor iz nadomestka (do `min(RwFarRange, (vd−1)·16)`).

| ID | Paket | Vel. |
|---|---|---|
| **M5-V.0** | **Raziskava (D-029)** | **zaključeno 14. 10.** — [zapis](raziskave/M5-V.0-daljna-vidnost.md); D-034; Q23–Q26 odločena → D-035 |
| ~~M5-V.1~~ | **Odločilni poskus + prizorišče "daleč"**: **zaključeno 21. 10.** ([zapis](meritve/2026-10-21-M5-V.1-dalec.md), D-040–D-043): 8 celic × 3 zelene; varianta A pri vd 24 / 5.000 +157 ms sličice in +28 KB/s → zavrnjena, B ostane; vd 10 → 24 sama ×5,9–6,4 MSPT; izhodišče V.8 so celice originala. Prvotni opis: svet z 1.000 / 5.000 NPC-ji v pasu do 384 blokov, igralec na vzpetini (razširitev harnessa M5-K.0), na **dediciranem strežniku s priključenim klientom** (D-035); celice original (sledilnik 64) in varianta A (domet 368 prek preizkusnega stikala), obe pri `view-distance` **10 in 24** (vanilla cena razdalje, ločena od cene M5-V); MSPT z igralcem, bajti/s proti klientu, čas sličice in klientski tick, porazdelitev velikosti spawn NBT; merilo veljavnosti: vidnih NPC-jev je res N | M |
| ~~M5-V.2~~ | **zaključeno 21. 10.** (`rework/far`, D-044; `v-run.ps1 -Far 1`, V0–V18 zeleni): 24/64/5000 4870 nadomestkov, rwfar 89,2 µs/tick/igralca; 10/64/1000 175, 6,0 µs; mirujoči 0 B/s; privzeto 0. Prvotni opis: **Strežnik**: register NPC-jev (`IWorldEventListener`, vzorec M5.16), krožna kadenca ~1 Hz na igralca, pas Chebyshev `(sledilnik − 8, min(RwFarRange, (vd−1)·16)]`, `isPlayerWatchingChunk`, nevidni (`display.visible == 1`) nikoli, delta posnetkov, format ~16 B na vnos, kanal `rwfar` + `HELLO` (samo klientom, ki zahtevajo), `/rwfar`; stikali `RwFarView`, `RwFarRange` (privzeto 0 / 384); `RwFarLos` (preverba vidne črte, privzeto 0 po Q26 → D-035) je izbirni dodatek tega paketa ali kasnejši. JUnit: kodirnik, delta, pas | M |
| ~~M5-V.3~~ | **zaključeno 21. 10.** (`FarLook`, `FarLooks`, `FarRequests`, D-045; V0–V19 zeleni): 4870 / 175 klonov `PERF_Idle` → **1** videz, klient 1 `REQ` / 1 `LOOK`, hash preverjen, 0 nadomestkov brez opisa; rwfar 93,4 / 6,2 µs/tick/igralca; privzeto 0. Prvotni opis: **Opis videza**: podmnožica spawn NBT **brez imena in naslova** (razred, display, oklep, orožje, `ModelData`, poza), 64-bit hash (kloni si ga delijo), LRU, invalidacija na poteh `UPDATE_NPC`, `REQ`/odgovor z omejitvijo hitrosti. JUnit: kloni isti hash, sprememba videza nov hash, ime ne vpliva | S |
| ~~M5-V.4~~ | **zaključeno 21. 10.** (`ImpostorFrames`, `ImpostorSlots`, `ImpostorAtlas`, `ImpostorBaker`, `ImpostorCheck`, D-046; `v-run.ps1 -Far 1 -Impostor 1`, V0–V21 zeleni): 9 pečenih, 0 nadomestkov brez peke, 0 napak; peka povp. 12,3 / 8,9 ms (največ 49,9 / 32,5 ms); piksli pri 56 blokih 8 videzov × 8 kotov IoU 0,884–0,926 (najslabši kot 0,789), razlika 4,2–22,0 (≤ 32), občutljivost 95,5 (> 64); `RwImpostorAtlas` 2048. Prvotni opis: **Klient: peka impostorjev**: lažna entiteta iz opisa (precedens `GuiNpcMobSpawnerAdd`), FBO, 8 kotov, samodejni obrez, atlas 2048² z robovi, mipmapi z ohranjanjem pokritosti alfe, LRU, proračun 1 videz na sličico, samodejni izklop brez FBO. Primerjava pikslov impostor ↔ model pri 56 blokih | L |
| ~~M5-V.5~~ | **zaključeno 21. 10.** (`FarRender`, `FarInterp`, `RenderEntitiesPatch`, `FarDrawCheck`, `/rwimpostor`, D-047; `v-run.ps1 -Far 1 -Impostor 1 -Izris 1`, V0–V24 zeleni): vd 24/5000 narisanih 4831 impostorjev v vsaki sličici, najdlje 368 blokov, izris povp. 1,3 ms; sonda na 368 blokih vidna v pravi sličici (114 pikslov); hoja čez mejo 64 brez vrzeli in dvojnih. Prvotni opis: **Klient: nadomestki in izris L3**: shramba, interpolacija, šiv z živimi entitetami (isti `entityId`, 8 blokov prekrivanja), en `BufferBuilder` (`POSITION_TEX_COLOR_NORMAL_LMAP`), cilindrični billboard, izbira okvirja po kotu, lightmap, megla; poseg coremoda v `RenderGlobal.renderEntities` pred `"blockentities"` (prehod 0) z rezervo `RenderWorldLastEvent`; frustum, histereza. Merilo: NPC viden na ≥ 368 blokih pri RD 24 (SP), brez izginotja na meji 64 | M |
| ~~M5-V.6~~ | **zaključeno 21. 10.** (`ImpostorL2`, L2 v `FarRender`, `RwImpostorStart`, D-048; `k-run.ps1 -Dalec 1 -Far 0/2 -ImpostorStart 56`, K16 zelen v 12 zagonih): A/B 3 + 3 pri RD 10, L2 137/200 in 437/500 NPC-jev: idle-200 p50 4,886 → 2,013 ms (−58,8 %), idle-500 11,591 → 2,331 ms (−79,9 %), razponi ločeni ([zapis](meritve/2026-10-21-M5-V.6-L2.md)); veja izven dometa vanille (brez hitboxa) v svetu še ne izmerjena. Prvotni opis: **L2**: sledeni NPC-ji onkraj `RwImpostorStart` (privzeto 56) prek preklicljivega `RenderLivingEvent.Pre` (brez popravka CNPC rendererja); tudi NPC-ji brez hitboxa, ki danes izginejo na 38 blokih. Merilo: čas sličice pri 200/500 vidnih **nižji** od originala (A/B, D-016) | M |
| ~~M5-V.7~~ | **zaključeno 22. 10.** (`FarCompat`, `SkinDownload`, `RwFarShaders` privzeto 0, popravek pol-vidnih v L2, D-050; `k-run.ps1 -Posebni 1`, K17a–j zelen, regresija V0–V24 zelena): senčila OptiFine → samodejni izklop (stikalo `RwFarShaders`), senčni prehod → brez impostorjev, URL skin → peka čaka na prenos; velik, morf, URL, pol-viden in brez hitboxa izven dometa vanille so impostor, neviden/ubit/jahajoč model ([zapis](meritve/2026-10-21-M5-V.7-zdruzljivost.md)); OptiFine, Entity Culling in `customNPC_entities_mod` preverjeni samo statično [K]. Prvotni opis: **Združljivost**: OptiFine brez/s senčilniki (senčni prehod), Entity Culling, `customNPC_entities_mod`, morph, veliki, jahajoči, mrtvi, pol-vidni NPC-ji, URL skini; vsaka nekompatibilnost ima samodejni izklop ali stikalo | M |
| ~~M5-V.8~~ | **zaključeno 22. 10.** ([zapis](meritve/2026-10-22-M5-V.8-sprejem.md), D-051; `ponovitve-run.ps1 -Scenarij sprejem`, 12 serij × 3, V0–V26 zelene): vd 24 / 5.000 sličica p95 9,188 → 8,893 ms pri `RwFarView` 2 (−0,3, v šumu), 10,873 pri 1 (+1,7, L3 brez L2 cilja ne izpolni); `rwfar` 91,5 µs/tick/igralca, 0 B/s mirovanje, 513 B/s hoja, VRAM ≈ 21,6 MiB [K], meja 64 brez vrzeli, sonda vidna na 368 blokih → privzeto **2**; sprememba v kodi je **M5-V.8b** (unit/build/package). Prvotni opis: **Sprejem**: A/B celic M5-V.1 z `RwFarView` 0/1/2, tri ponovitve, posnetki pri 64/128/256/368 blokih, zapis v `docs/meritve/`; ob uspehu **privzeto vklopljeno** (D-035: strežnik `RwFarView=1`, klient `RwFarView=2`) | S |
| ~~M5-V.8b~~ | **zaključeno 22. 10.** (`RwFarView` privzeto 2 v kodi, `unit`/`build`/`package`). Prvotni opis: **Privzeti vklop (D-051)**: `CustomNpcs.RwFarView` privzeto **2** in komentar konfiguracije (vrednost 1 na klientu ni priporočena), vrstice obnašanja v 04-REGISTRI, README; `dev/run` ostane 0. Dokaz: `unit`, `build`, `package` (brez novih meritev, sprejem je M5-V.8) | S |

Vrstni red M5-V: **V.1 → V.2 → V.3 → V.4 → V.5 → V.6 → V.7 → V.8**; V.2–V.3 (strežnik) in V.4 (peka) sta
neodvisna. Do sprejema (M5-V.8) so stikala privzeto 0; po sprejemu so **privzeto vklopljena** (Q23 → D-035,
izjema od D-007 po odločitvi uporabnika). Obstoječi configi obdržijo svojo vrednost kot pri D-032; `dev/run`
ostane 0, da baseline meri original.
Pogoj, ki ga modul ne odpravi: NPC obstaja samo v naloženem chunku — v enem igralcu do RD, na dediciranem
strežniku do `view-distance` (cilj po Q24 → D-035).

**Izhodni kriterij:**
- vsak sprejet paket ima A/B meritev, ki presega šum
- nobena funkcionalna regresija v matriki iz `PLAN_IMPLEMENTACIJE.md` §6
- timerji in script trace se ujemajo z referenco
- zapisano, koliko NPC-jev zdaj zmore isti stroj proti baselineu (M5.8)
- **klient (D-033):** zapisano, kakšen je čas sličice (p95, 1 % low) pri 200 in 500 vidnih NPC-jih
  z originalom in z vsemi sprejetimi K-paketi; sprememba videza je izključno za stikalom
- **vidnost na daljavo (D-034, M5-V):** NPC viden na ≥ 368 blokih na **dediciranem strežniku** z `view-distance=24` in klientom z RD 24; pri 5.000
  oddaljenih NPC-jih dodatni čas sličice p95 ≤ 1,0 ms, strežnik MSPT p95 +≤ 0,1 ms na igralca, omrežje
  idle ≤ 1 KB/s in hoja ≤ 20 KB/s na igralca, VRAM ≤ 32 MB; brez utripanja na mejah stopenj

**Preverba izhodnega kriterija (22. 10., D-054, D-055) — M5 zaključen:**
- A/B nad šumom: izpolnjeno za vse pakete, sprejete zaradi hitrosti; izjema po zasnovi sta K12 (`RwRenderBox`, popravek
  vidne napake, A/B v šumu, D-052) in `RwFarView` 2 pri vd 24 / 5.000 (cilj je bil +≤ 1,0 ms, ne dobitek, D-051)
- funkcionalna matrika §6: **prehod 1 ponovljen v M5.Z** s configom in privzetimi vrednostmi kode, oba 5/5 zeleno; brez regresije v izvedenih preverjanjih. Klientski dokazi M5 so v K.2/V.8; celotna release matrika, preostale nepokrite vrstice in dejanski modpack ostanejo M10
- timerji in script trace proti referenci: **izpolnjeno za NPC (M5.Z)** — 97/75 dogodkov pred/po restartu in 19 razmikov timerja 7 po 8 tickov identični; referenca isti jar s stikali iz configa (0), kandidat privzeta koda. B3 ohranjen; player/block/item dogodki s tem niso potrjeni
- kapaciteta strežnika: izpolnjeno (M5.8, 16. 10.: idle 10×, boj 1,5×, nedosegljiva 1,4×)
- klient: **izpolnjeno** (M5-K.2: p95 in 1 % low pri 200/500 v dveh prizoriščih, vsa stikala z 0 vrnejo original)
- vidnost na daljavo: izpolnjeno (M5-V.8, D-051; VRAM izračunan [K], ne izmerjen)

**Tveganja:** S4 mora zanesljivo zaznati poslušalce na vodilu CNPC API-ja (tudi registrirane kasneje),
sicer tiho izgine dogodek; A/B boja je šumen zaradi poteka boja — po M5.10 zaradi dela na klic, ne števila klicev, zato A/B pri istem boju (M5.11). Tveganje odstranitve globalnega
locka je od D-030 v M6.6.

---

## M6 — Scripting platforma v Javi

**Cilj:** **R7** — pisanje NPC skript v Javi, s prevajanjem, tipi in diagnostiko, ki jo AI
lahko uporabi.

**Vhodni pogoj:** M1 (varno shranjevanje skript in njihovih podatkov).

**M6.0 zaključen 29. 10. (D-090, Q127–Q131):** [raziskava](raziskave/M6.0-java-scripting.md).
JDK 8 JavaCompiler je v M6.1 izbran (Q141) in potrjen v dev/zapakiranem Forge; hitrost izvajanja Java/JS še ni
izmerjena. Java samo za zaupanja vredne avtorje, JS ostane privzet. Obstoječi API,
handlerji in jezikovni GUI se razširijo, ne nadomestijo. Kritika vsakega paketa in njegovi
dokazi so v raziskavi; pri prvem prenosu originalnega razreda velja ločen baseline commit.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| ~~M6.0~~ | Raziskava izvedbe zaključena; brez sprememb merilnih vhodov in brez novih meritev | S |
| ~~M6.1~~ | **Zaključeno 29. 10. (Q132–Q141), poskus 1:** JDK JavaCompiler izbran; Janino zavrne lambdo in sprejme napačen generik. Typed API 15/15 v dev in zapakiranem Forge, brez compilerja jasna napaka, JS isti; warm 17–31 ms kot podatek. [Dokazila](meritve/2026-10-29-M6.1-prevajalnik.md) | M |
| ~~M6.2~~ | **Zaključeno 29. 10. (Q142–Q147), poskus 2 (harness):** popis 16 NPC hookov + string hook pravilo, Java veja v `DataScript` pri `ScriptLanguage=Java`; JS/Java sled tabov, busa in preklica enaka zlatemu nizu, 14/14 v dev in zapakiranem Forge. Modpack rabi ročni `RwJavaClasspath` (M6.7/M6.12). [Dokazila](meritve/2026-10-29-M6.2-hooki.md) | M |
| ~~M6.3~~ | **Zaključeno 29. 10. (Q148–Q150), poskus 1:** izbiren osnovni razred `NpcScript` (16 tipiziranih no-op hookov + `log`) na obstoječih `noppes.npcs.api` vmesnikih; tipizirana sled = JS zlati niz, `@Override` napaka z vrstico, 14/14 v zapakiranem Forge. [Dokazila](meritve/2026-10-29-M6.3-pogodba.md) | M |
| ~~M6.4~~ | ~~Loader/instanca na NPC/tab/generacijo, omejen cache bytecode; atomski reload in invalidacija vključitev/API, brez deljenja static stanja~~ — zaključen 29. 10. ([zapis](meritve/2026-10-29-M6.4-reload.md), Q151–Q156) | M |
| ~~M6.5~~ | **Zaključeno 29. 10. (Q157–Q163), poskus 2 (harness):** source map JS/Java, runtime stack z izvorom, B5 reprodukcija na originalnih bajtih in obnova po reloadu tudi prej manjkajočega hooka. Šest controller testov PASS na `7ec18d6e…`, m65 15/15, m62 14/14, m64 15/15. [Dokazila](meritve/2026-10-29-M6.5-napake.md) | M |
| ~~M6.6~~ | **Zaključeno 29. 10. (D-092, Q164–Q168), poskus 2 (harness):** reprodukcija 48/48; obnova gnezdenega Current v finally, JS tudi CurrentType, binarno polje in lock ostaneta. Pet controller testov PASS na `af46a352…`, 88/88 in JS/Java regresija 16/16. [Dokazila](meritve/2026-10-29-M6.6-context.md) | — |
| ~~M6.7~~ | **Zaključeno 29. 10. (D-093, Q169–Q172), poskus 2 (harness):** `.\dev.ps1 compileScripts` brez Minecrafta, skupni compiler/API/source map, JSON/exit status brez izvajanja razredov/procesorjev. CLI 4,979/4,881 s, JS/Java 16/16, source/B5 17/17; unit/build/package PASS. [Dokazila in odtisa](meritve/2026-10-29-M6.7-cli.md) | S |
| ~~M6.8~~ | **Zaključeno 29. 10. (D-094, Q173–Q176), poskus 1:** obstoječe JS/Java pravice, OpsOnly in reload 27/27; unit/build/package PASS na `36620163…`. Java file/reflection dovoljena, brez sandboxa; odprti originalni default ni trust meja. Produkcijske pravice/runtime isti. [Politika in dokazi](meritve/2026-10-29-M6.8-pravice.md) | M |
| ~~M6.9~~ | **Zaključeno 29. 10. (Q177–Q180), poskus 1:** razvojni generator `docs/api/API.json`, `API.md`, `HOOKS.md`; izrecen v1 nabor/izključitve, 49 tipov/515 članov/16 hookov, 5.661 preverjanj in prevedeni primeri PASS na `0ff903b4…`. Bajtno deterministično, runtime isti. [Dokazi](meritve/2026-10-29-M6.9-api.md) | M |
| ~~M6.10~~ | **Zaključeno 29. 10. (Q181–Q183), poskus 1:** `scripts/java/template/` Gradle Java/IDEA/Eclipse predloga z dejanskim API classpath ter istim CLI/compilerjem/binderjem; typed StarterNpc z vključitvijo in trije negativni primeri (vrstica/koda). m610-template-v1 53 preverjanj PASS na `53ca9c0d…`, runtime isti. [Dokazi](meritve/2026-10-29-M6.10-predloga.md) | S |
| ~~M6.11~~ | **Zaključeno 29. 10. (Q188–Q191), poskus 1:** samo player/world (Q92): `IPlayer`/`IWorld` v objavljenem API (51 tipov/606 članov) in preveden primer v HOOKS.md; runtime isti. m611-packaged-v1 12/12 (JS = Java sled, zapis bloka, tipizirani napaki) in m69-api-v1 PASS na `e3e30ca1…`; ogret par JS 8,80 / Java 2,07 µs kot podatek. Item/block/quest/dialog/faction v2. [Dokazi](meritve/2026-10-29-M6.11-player-world.md) | M |
| ~~M6.12~~ | **Zaključeno 29. 10. (Q184–Q187), poskus 1:** Java v obstoječi izbiri jezika samo pri `RwJavaClasspath` (privzeto originalni seznam, samo NPC GUI); GUI/save/clone/JVM restart in JS→Java→JS, brez compilerja `compiler.unavailable`. m612-gui-v1 101 trditev in m62-packaged-v3 16/16 PASS na `48c60f8e…`. Samodejni modpack classpath v2 (Q184). [Dokazi](meritve/2026-10-29-M6.12-izbira-jezika.md) | S |

Vrstni red: M6.1–M6.10 (M6.6 pogojno) → M6.12 → minimalni M6.11 → M7.
Varnostna politika M6.8 velja že od prvega Java opt-in; potrdi se v svojem paketu.

**Izhodni kriterij:**
- Java skripta se prevede, naloži, teče in se hot-reloada brez restarta strežnika
- prevajalna napaka da uporabno sporočilo z vrstico, v igri in v logu
- `compileScripts` vrne rezultat v nekaj sekundah
- obstoječe JS skripte v testnem svetu delujejo identično kot prej
- `API.json` je popoln za izrecno objavljen v1 nabor in strojno berljiv
- Java deluje v zapakiranem Forge okolju; loaderji ne delijo stanja med NPC-ji/tabi
- izmerjena razlika Java vs JS na istem scenariju/sledi je zapisana (en ogret par kot
  podatek po Q92, cold compile posebej); raziskava ne dokazuje pospeška

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

> **2026-10-29 (M7.0, D-095, Q192–Q197):** v1 = ~~**M7.V**~~ (zagon rework + entities 0.7.0 na 01Oct19, zaključen 29. 10., Q198–Q199, [dokazi](meritve/2026-10-29-M7.V-entities-zagon.md)) → ~~**M7.9**~~ `Animators.of(npc)` (zaključen 29. 10., Q200–Q202, [dokazi](meritve/2026-10-29-M7.9-animator-api.md)) → ~~**M7.5**~~ hooki `animationStart/animationEnd/animationEvent` (`RwAnimHooks` privzeto 0; zaključen 29. 10., Q203–Q206, [dokazi](meritve/2026-10-29-M7.5-animacijski-hooki.md)). Most `rework/anim` z refleksijo, entities mod nespremenjen. M7.8 reload in `blendTo` v2. [Raziskava](raziskave/M7.0-animacije-integracija.md).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M7.1 | Branje animacijske implementacije CustomNPC+; odločitev port vs novo | S |
| M7.2 | `rework/anim/` podatkovni model: `Animation`, `Frame`, `PartTransform`, interpolacija, looping | M |
| M7.3 | Runtime: deterministično izvajanje na serverju, sinhronizacija na klient (predlog: pošlji "predvajaj X od tika T", klient interpolira) | L |
| M7.4 | Render integracija z obstoječim `ModelData` / `ModelPartData` | M |
| ~~M7.5~~ | ~~Hooki: `onAnimationStart`, `onAnimationEnd`, `onFrameEnter`, `onFrameExit`~~ zaključen kot `animationStart/animationEnd/animationEvent` za `RwAnimHooks` (Q206, [dokazi](meritve/2026-10-29-M7.5-animacijski-hooki.md)) | S |
| M7.6 | **Tekstovni format** + JSON Schema + `ANIMATION-GUIDE.md` s koordinatnim sistemom, imeni delov telesa, enotami in 10+ komentiranimi primeri | M |
| M7.7 | **Validator** `.\dev.ps1 validateAnim <datoteka>` z napakami po vrsticah | S |
| M7.8 | `/npcanim reload` v igri za preview brez restarta | S |
| ~~M7.9~~ | ~~Script API: `npc.getAnimator().play/queue/blendTo/stop`, tipizirano~~ zaključen kot `Animators.of(npc)` (Q202); `blendTo` v2 | S |
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
`docs/04-REGISTRI.md` z izvorom in licenco.

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
| ~~M10.0~~ | **Raziskava zaključena 29. 10. (D-096, Q207–Q214):** [zapis](raziskave/M10.0-release-kandidat.md); M10.1 razdeljen na a/b, vrstni red M10.1a → M10.1b → M10.5 → M10.4 → M10.2 → M10.6 → M10.7 | S |
| ~~M10.1a~~ | **Zaprt 29. 10. brez sprejema (Q220):** oba poskusa padla v harnessu na kraku O, K se ni zagnal; vse vrstice prestavljene v M10.1b. [Zapis](meritve/2026-10-29-M10.1a-matrika.md) | M |
| M10.1b | Strežniške A/B vrstice iz M10.1a (`m10-matrika-run.ps1`, razsodba O = K) + klientski prehod: login/playerdata, dialog/quest klik, pošta, skin napaka, F3+T, original ↔ kandidat klient/strežnik | M |
| M10.2 | Meritve proti baselineu iz M2.6, vse scenarije | S |
| M10.3 | Test na **kopiji** uporabnikovega pravega sveta | M |
| M10.4 | Soak test, priporočeno 24 h z dejansko aktivnostjo | S |
| M10.5 | **Rollback vaja** — dokazano vrnjen original JAR + podatki | S |
| M10.6 | Uporabniška dokumentacija: nove nastavitve, migracija, znane omejitve | M |
| M10.7 | Changelog z eksplicitnim seznamom sprememb obnašanja | S |

**Izhodni kriterij:** `PLAN_IMPLEMENTACIJE.md` §6 "Sprejem" v celoti izpolnjen, plus
uspešna rollback vaja.

---

## M11 — Vojske in poveljevanje prek zemljevida

**Cilj:** NPC-ji z isto vojsko (`RwArmy`, nastavljeno v GUI NPC-ja ali s poveljniško palico) se
na zemljevidu pokažejo kot ena oznaka s številom; igralec jo izbere, klikne cilj in vojska gre
tja skupaj — v formaciji, pohodu ali roju, pasivno ali z napadom. Isto prek palice: vojske v
okolici s ciljem iz pogleda (žarek) in seznam vseh vojsk. Zahteva R10, dopolnjena 26. 10.
**Raziskava M11.0 (26. 10.):** [zapis](raziskave/M11.0-vojske-prek-zemljevida.md) — ime vojske
z lastnikom namesto gole številke, grozd z nastavljivim pragom, vojska iz čet (obstoječi `Squad`),
zemljevid prek obstoječega `xaerofleetcommand` iz `ladja_mod` (predlog odgovora na Q14).

**Vhodni pogoj:** M4.14d, M4.14c (cena 50/200 članov) in M4.10 zaključeni; M5 dovolj, da 200+
NPC-jev v pohodu ne preseže proračuna ticka. Po odločitvi uporabnika (D-026) se faza začne šele,
ko je vse ostalo večinoma narejeno.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| ~~M11.0~~ | Raziskava izvedbe — **narejeno 26. 10.** ([zapis](raziskave/M11.0-vojske-prek-zemljevida.md)); ob začetku M11 se preveri proti tedanjemu stanju M4 | S |
| M11.1 | Register vojsk (`WorldSavedData`: ime, lastnik igralec/frakcija, barva, privzeta oblika in drža, zadnja znana lega) in NBT `RwArmy` na NPC-ju (privzeto brez = original); ukaz `/rwarmy`, skriptni API | S–M |
| M11.1b | **Dovoljenja drugim igralcem** (zahteva lastnika 26. 10.): lastnik (za svoje) ali op (za vse) igralcu dodeli *vidi* in *upravlja* — izbrane vojske ali **vse** (tudi kasneje ustvarjene); upravlja vključuje vidi. Seznam prijavljenih in že videnih igralcev (`usercache`), shranjen UUID. Posnetek pošlje samo vidne vojske, vsak ukaz se preveri na strežniku; odvzem velja ob naslednji osvežitvi, nov ukaz zavrnjen. `/rwarmy grant/revoke`; GUI v M11.8. [M11.0 §3.5a](raziskave/M11.0-vojske-prek-zemljevida.md) | S–M |
| M11.2 | Grozdi in posnetek: NPC-ji iste vojske po razdalji (radij, prag v configu), glavni grozd + zaostali; povzetek strežnik → klient prek ločenega kanala `rework`, osvežitev samo ob odprtem zemljevidu/GUI | S |
| M11.3 | Vojska iz čet: razrez na čete (privzeto do 16–24 članov), ena pot vojske, makro formacija čet, vsaka četa obstoječi `Squad`; zaostali dobijo »pridruži se« | M |
| M11.4 | Načini in drža: formacija / pohod / **roj** (brez mest, ohlapen odmik) in **pasivno / napad** (boj med pohodom, vrnitev v četo) | M |
| M11.5 | Dolg pohod: razrez poti vojske na vmesne točke (gradi na M4.10). Če meritev pokaže slabe poti vodje tudi po M4.10, oceni vodjo na Baritonu ([07 §4](07-BARITONE-OZADJE.md), ocena 26. 10.) | M |
| M11.6 | Nenaloženi chunki: najprej chunk ticket, ki potuje z vojsko, z omejitvijo hkratnih pohodov v configu; abstraktni pohod samo, če meritev ticketa pokaže nesprejemljivo ceno (nova odločitev D-0NN) | M–L |
| M11.7 | Zemljevid: most CNPC → `xaerofleetcommand` (`ladja_mod`; odsev, fail-closed, kot `DavincisFleetBridge`), marker prapora s številom, izbira načina/drže ob ukazu, preverba pravic (lastnik / op), omejitev paketov. Ladje in vojske v istem vmesniku | M |
| M11.8 | GUI: polje vojske v nastavitvah NPC-ja; **poveljniška palica** — desni klik na NPC (pokaži/nastavi), množična dodelitev v svetu, zavihka *V okolici* (cilj z žarkom iz pogleda, doseg naloženih blokov) in *Vse vojske* (lega, število, stanje, ustavi/vrni) ter *Dovoljenja* (igralci, vidi/upravlja: nič / izbrane / vse, M11.1b) | M |
| M11.9 | Robni primeri: voda, prepadi, grla, obtičali člani, obnašanje na cilju, smrt/odhod članov, ponovno nalaganje sveta med pohodom | M |
| M11.10 | Meritev: pohod 200 NPC-jev (vojska iz čet) proti baselinu M2.6 (MSPT, alokacija), cena ticketov M11.6; preverba v klientu z igralcem (zemljevid, palica, GUI) | S–M |

**Izhodni kriterij:** 200 NPC-jev ene vojske doseže cilj, izbran na zemljevidu in s palico, na
prizorišču z grlom in na razdalji čez `NpcNavRange`, tudi čez nenaložene chunke; ukaz preživi
shranjevanje; igralec brez dovoljenja vojske ne vidi in ne more ukazovati; brez `RwArmy` je obnašanje enako originalu; MSPT p95 med pohodom izmerjen in v
dogovorjenem pasu.

**Tveganja:** nenaloženi chunki (M11.6) so največja neznanka; kohezija formacij še ne izpolni FA
(D-079), zato vojska iz čet ostane v preverjeni velikosti enote; Xaero adapter je vezan na verzijo
1.37.8 in ali je Xaero v uporabnikovem modpacku, je neznano (Q1); GUI in zemljevid se dokončno
preverita samo ročno v klientu.

---

## M12 — Nalaganje skinov prek URL-ja

**Cilj:** URL skini (skinType 2) se zanesljivo in poceni naložijo tudi pri stotinah NPC-jev z
različnimi URL-ji. Zahteva uporabnika 26. 10. 2026: uporablja skine iz datotek **in** prek URL-ja,
URL je zanj pomemben; paket naj se izvede na koncu, za ostalimi milestoni.

**Izhodišče [K]** ([raziskava](raziskave/performance/client_code.md) Q3, `PLAN_IMPLEMENTACIJE.md`
vrstica »Prenos skinov«): enak URL se že v originalu naloži samo enkrat (`skins/<md5(url)>` v
`TextureManager`), datotečni skini prek `SimpleTexture` prav tako. Manjka: `ImageDownloadAlt` za
vsak URL odpre svojo nit brez connect/read timeouta in brez ponovitve ob napaki; ni diskovnega
predpomnilnika (`cacheFile` je `null`, vsak zagon klienta prenese vse znova); velikost slike ni
omejena (2048×2048 ≈ 16 MiB GPU + 16 MiB kopa); `BufferedImage` po nalaganju na GPU ostane v
pomnilniku; skinType 1 brez tekstur vsako sličico znova kliče `loadSkinFromCache`. Atlas skinov
za bližnje NPC-je je zavržen (K13, D-053); M5-V.7 peka impostorja že čaka na prenos URL skina.

**Vhodni pogoj:** M12.0 raziskava; prizorišče z URL skini (lokalni strežnik skinov iz M5-V.7,
`k-run.ps1 -Posebni 1`) razširjeno na mnogo različnih URL-jev.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M12.0 | Raziskava izvedbe (D-029): ponovno branje poti `RenderNPCInterface.getEntityTexture` → `ImageDownloadAlt`, vanilla `SkinManager`, interakcija z impostorji (M5-V.7 `SkinDownload`) in `customNPC_entities_mod` | S |
| M12.1 | Scenarij in baseline: 50/200/500 NPC-jev z različnimi URL-ji, tudi počasen, mrtev in prevelik URL; veličine: niti, čas do prikaza vseh skinov, zatikanje sličic ob nalaganju na GPU, pomnilnik (kopa + GPU) | S |
| M12.2 | Omejen izvajalec prenosov s timeouti in ponovitvijo z zamikom; identične sočasne zahteve združene; napaka → privzeti skin brez zastoja | S–M |
| M12.3 | Diskovni predpomnilnik URL skinov (ključ md5 URL-ja, veljavnost/ETag, varno ob pokvarjeni datoteki) | M |
| M12.4 | Omejitev velikosti slike in sprostitev `BufferedImage` po nalaganju na GPU; razporeditev nalaganj na GPU čez več sličic | S |
| M12.5 | skinType 1: oznaka »ni na voljo, poskusi ob ticku T« namesto klica vsako sličico | S |

Vse pod stikali, privzeto original; privzeti vklop po D-032 (dokazano enako + izmerjeno hitreje).

**Izhodni kriterij:** na scenariju M12.1 brez visečih niti ob mrtvem URL-ju, ponovni zagon klienta
ne prenese že shranjenih skinov, prevelik skin ne preseže omejitve, zatikanje ob nalaganju
izmerjeno proti originalu (3 ponovitve, D-016); prikaz skinov enak originalu.

---

## M13 — Urejanje več NPC-jev hkrati (povezani NPC-ji)

**Cilj:** sprememba enega NPC-ja (skin, statistike, AI, inventar …) se prenese na vse »iste«
NPC-je v svetu, tudi na nenaložene, brez ponovnega postavljanja; v seznamu palice (desni klik v
zrak) se da izbrati in urejati več NPC-jev. Zahteva uporabnika 26. 10. 2026.
**Raziskava M13.0 (26. 10.):** [zapis](raziskave/M13.0-mnozicno-urejanje.md) — CNPC to že ima
kot skrite »Linked NPC« (`LinkedNpcController`), ki pa prepišejo dom in usmerjenost vseh
povezanih NPC-jev, prenesejo vse ali nič, ne prenesejo skript in zahtevajo povezovanje enega po
enega. Zasnova popravi in razširi ta mehanizem namesto novega identifikatorja.

**Vhodni pogoj:** nič iz M4–M11. **Vrstni red:** neodvisen; izvede se po M4, pred M6
(uporabnik ga lahko prestavi).

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| ~~M13.0~~ | Raziskava — **narejeno 26. 10.** ([zapis](raziskave/M13.0-mnozicno-urejanje.md)); ob začetku preveri na kodi v `src/patch` | S |
| M13.1 | Reprodukcija in popravek: povezani NPC-ji ohranijo svoj dom, usmerjenost, pot, lego, `RwArmy`, `RwNavBackend` (stikalo, privzeto original, D-007); scenarij z dvema povezanima NPC-jema na različnih mestih | S |
| M13.2 | Kategorije prenosa na skupino (videz, ime in naslov, statistike, AI, inventar, advanced, skripte, transform), skripte nova kategorija; razveljavitev zadnje spremembe (`.json_prev`); postopen prenos pri velikem številu naloženih članov | M |
| M13.3 | Seznam palice z več izbirami: Shift/Ctrl-klik, filter po imenu, »vsi z istim imenom v radiu«; dejanja: poveži s skupino, kopiraj nastavitve z izbranega NPC-ja (enkratno, s kategorijami), izbriši, ponastavi; prikaz skupine in števila članov | M |
| M13.4 | Kloner »postavi kot povezan«; obvestilo »posodobljeno N NPC-jev«; preverba v klientu z igralcem (GUI, nenaloženi chunki, ponovno nalaganje sveta, dedicirani strežnik) | S |

**Izhodni kriterij:** sprememba enega povezanega NPC-ja se prenese na vse povezane, tudi v
nenaloženih chunkih ob nalaganju, samo v izbranih kategorijah; dom in lega vsakega ostaneta
njegova; 100 že postavljenih NPC-jev se poveže z eno izbiro v seznamu; razveljavitev vrne
prejšnje stanje; brez stikala obnašanje enako originalu.

**Tveganja:** obstoječi svetovi s povezanimi NPC-ji (popravek doma pod stikalom); prenos na
stotine naloženih NPC-jev hkrati (cena, paketi); GUI se dokončno preveri samo ročno.

---

## M14 — Instanciran izris bližnjih NPC-jev (K10a → K10)

**Cilj:** model NPC-ja znotraj dometa impostorjev (56 blokov) ne stane več ~10 µs na NPC, ampak
blizu cene impostorja (~0,2 µs, M5-V.6). Ocena **[H]**: pri 500 bližnjih NPC-jih p95 ~9,8 →
~5–6 ms. Pomaga samo gneči modelov blizu (bitke, mesta, tabori).

**Odločitev lastnika (26. 10. 2026):** za K10 velja merilo **vizualno enako** namesto piksel-enako
(izjema od D-032 samo za ta milestone): razlike, ki jih s prostim očesom ne vidiš, ne preprečijo
privzetega vklopa. Lastnik je zahteval vklop. Ozadje: lasten senčilnik mora ponoviti fiksno pot
1.12 (luč, lightmap, megla, alfa test, rdeč blisk udarca, prekrivne teksture, glint), kar da drobne
razlike zaokroževanja. [Ponovna ocena K10](raziskave/M5-K-K13-K10-ponovna-ocena.md) §3.

**Vhodni pogoj:** nič; pogoj za ponovno odprtje iz D-053 nadomesti odločitev lastnika.
**Vrstni red:** po M13, pred M6.

### Paketi

| ID | Paket | Vel. |
|---|---|---|
| M14.1 | **K10a** — deli modela iz VBO namesto display listov, fiksna pot, **ista slika** (piksel-enakost kot doslej). A/B idle-200/500 na prizorišču M5-K.1, 3 ponovitve; loči »oglišča v gonilniku« od »oglišč sploh«. Če je hitreje, privzeto vklopljeno po D-032 | S–M |
| M14.2 | **K10** — instanciran izris z lastnim senčilnikom: vsi enaki deli/modeli v enem klicu, poza v senčilniku, ponovljena fiksna pot. Merilo **vizualno enako**: primerjava posnetkov na vseh prizoriščih M5-K in M5-V.7 (prag razlike na kanal in delež pikslov določi paket vnaprej, preden vidi rezultat) + ročni ogled lastnika v klientu; A/B kot M14.1 | L |
| M14.3 | Združljivost in povratek: OptiFine s senčilniškim paketom → samodejno original (kot `FarCompat`, D-050); nepodprti modeli in sloji (lastni deli, MorePlayerModels, oklep, morf, rdeč blisk, glint) → original za tega NPC-ja; stikalo za izklop | M |
| M14.4 | Sprejem: original proti vsem privzetim stikalom (kot M5-K.2) z M14 vklopljenim, regresija M5.Z | S |

**Izhodni kriterij:** p95 pri idle-500 izmerljivo nižji od M5-K.2 (9,817 ms) z ločenima razponoma;
lastnik razlike v klientu ne vidi; s senčilniškim paketom in pri nepodprtih modelih slika enaka
originalu; stikalo vrne original.

**Tveganja:** obseg ponovitve fiksne poti; raznolikost CNPC modelov; gonilniki (Intel/AMD/NVIDIA);
če K10a ne pokaže koristi, je dobitek M14.2 še vedno samo ocena.

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
