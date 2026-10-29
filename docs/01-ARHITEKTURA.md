# 01 — Arhitektura in odločitve

Ta dokument pove **zakaj** je projekt postavljen tako, kot je. Preden karkoli spremeniš,
preveri, ali je odločitev tu že sprejeta. Novo odločitev dopiši v "Dnevnik odločitev" na dnu.

---

## 1. Osnova (build base)

**Odločitev D-001: build base ostane uradni `CustomNPCs_1.12.2-(01Oct19).jar`.**

SHA-256 `cafacade45fb2aa6ac52956487889d0f4a9d4f6a1682ea28a1f7aca1ba100fa1`.

Razlog:

- Okolje v `dev/` je že zgrajeno okoli tega JAR-a in **dokazano byte-identično** originalu
  razen razredov, ki jih zavestno prevajamo (`audit/package-verification.txt`).
- `PLAN_IMPLEMENTACIJE.md` (audit 746 dekompiliranih datotek, bugi B1–B8) se nanaša
  na točno to verzijo. Zamenjava osnove bi ta audit razveljavila.
- Uporabnikovi obstoječi svetovi in shranjeni NPC-ji so na tej verziji.

**Odločitev D-002: BetaZavr in CustomNPC+ sta referenčni implementaciji, ne osnova.**

- [`BetaZavr/CustomNPCs_1.12.2-Unofficial`](https://github.com/BetaZavr/CustomNPCs_1.12.2-Unofficial)
  — polna 1.12.2 source koda, aktivna. Beremo, primerjamo, portamo ideje. Licenca **CC BY-NC 3.0**
  omejuje komercialno distribucijo; dobesedno prevzemanje kode zahteva atribucijo in
  vpis v `docs/04-REGISTRI.md` → "Prevzeta koda".
- [`KAMKEEL/CustomNPC-Plus`](https://github.com/KAMKEEL/CustomNPC-Plus) — 1.7.10, ima že
  implementirano to, kar zahtevata R7 (Java scripting prek Janino), R4 (frame-based
  animacije z in-game urejevalnikom) in del R2 (flying AI s 3D pathfindingom) ter 154+
  script hookov. **To je glavni vir za R3.** Koda je za 1.7.10, torej vsaka funkcija
  potrebuje port, ne kopiranje.

Praktično: preden implementiramo karkoli iz R1–R9, **najprej pogledamo, kako sta to rešila
ta dva projekta**. To prihrani dneve. Ampak vsako prevzeto rešitev zapišemo v
`docs/04-REGISTRI.md` z izvorom in licenco.

---

## 2. Kako raste izvorna koda

**Odločitev D-003: postopno širjenje prevedljivega source drevesa.**

Trenutno stanje: prevaja se **1 razred**, ostalo je original binary.

```
dev/reference-src/   ←  dekompiliran izpis, samo za branje, ni prevedljiv
        │
        │  ko razred RES potrebujemo
        ▼
dev/src/patch/java/  ←  razredi, ki se dejansko prevajajo in spreminjajo
        │
        ▼
dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar
        (original + naši razredi; verify-package.ps1 dokaže razliko)
```

Postopek za vsak nov razred:

1. Prenesi datoteko iz `reference-src` v `src/patch/java`.
2. Popravi **samo napake dekompilacije**, nič funkcionalnega.
3. Zgradi. Če se ne prevede, popravi dalje — še vedno brez funkcionalnih sprememb.
4. Napiši baseline test, ki **potrdi obstoječe obnašanje** (tudi če je napačno).
5. `testOriginal` in `test` morata dati enak rezultat.
6. **Šele zdaj** delaj funkcionalno spremembo, v ločenem commitu, z novim testom.

Zakaj ne vsega naenkrat: 746 datotek naenkrat pomeni tedne dela, med katerim ni mogoče
ločiti "napaka dekompilacije" od "regresija, ki sem jo pravkar naredil".

**Izjema — celi paketi.** Ko en delovni paket dokazano potrebuje večino nekega paketa
(npr. `noppes/npcs/ai/` za R1+R2+R5), se ta paket prenese naenkrat, a še vedno v dveh
korakih: najprej prenos brez funkcionalnih sprememb + baseline testi, potem spremembe.

---

## 3. Kompatibilnostna politika

Velja vse iz `PLAN_IMPLEMENTACIJE.md`, razdelek 2. Povzetek plus dopolnitve za rework:

### Kar se NE sme spremeniti brez posebne odločitve

- mod ID, registry imena, entity ID-ji
- obstoječi NBT/JSON ključi in njihovi **tipi**
- javni script API podpisi (tudi tisti s tipkarskimi napakami — dodatki jih lahko uporabljajo)
- packet ID-ji oziroma vrstni red enumov (`EnumPacketClient`, `EnumPacketServer`)
- frekvenca in vrstni red script dogodkov

### Kar se sme dodajati

- **novi** NBT ključi z novimi imeni (stari mod jih ignorira)
- novi script hooki (stare skripte jih ne definirajo → `unknownFunctions` jih preskoči)
- novi packet tipi **na koncu** enuma
- nove AI taske, dokler privzeta konfiguracija ohrani obstoječe obnašanje

### Save format in migracija

**Odločitev D-004: enosmerna avtomatska migracija z obveznim backupom.**

- Vsaka shranjena datoteka dobi polje `SaveFormat` (int). Odsotnost = verzija 0 (original).
- Ob prvem nalaganju starejše verzije: **najprej backup** v `<ime>.v<N>.bak`, potem pretvorba,
  potem zapis, potem ponovno branje in preverjanje.
- Če preverjanje ne uspe, se novi zapis zavrže in obdrži original. Napaka gre v log in OP-jem.
- Migracija ni reverzibilna. Po migraciji original CustomNPCs teh datotek morda ne bo bral.
  To je zavestna odločitev; backup je zaščita.

### Privzeto izklopljeno

Vsaka sprememba, ki spremeni obnašanje v igri (nov način letenja, solid hitbox, LOD za AI,
Java scripting), se uvaja s **stikalom v configu ali na NPC-ju**, privzeto v načinu, ki
posnema original. Šele ko je funkcija dokazana, se lahko privzeta vrednost spremeni —
z zapisom v `docs/04-REGISTRI.md` → "Sprejete spremembe obnašanja".

---

## 4. Ciljna platforma

**Odločitev D-005: samo Minecraft 1.12.2.** Brez multi-verzijske abstrakcije.

Zaklenjeno okolje (`environment-lock.json`):

| | |
|---|---|
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2847 |
| Mappings | `snapshot_20171003` |
| Gradle | 4.9, ForgeGradle 2.3-SNAPSHOT |
| Java | Temurin 8u492-b09 (lokalno v `.tools/jdk8`) |

Java 8 je **obvezna**, ne izbira: Nashorn (obstoječi JS scripting) je del JDK 8 in odstranjen
v novejših. Menjava Jave je samostojen projekt in ni del tega reworka.

---

## 5. Ciljna struktura kode

Nova koda gre v ločen namespace, da je vedno jasno, kaj je original in kaj naše:

```
noppes/npcs/…                    original (patchan po potrebi)
noppes/npcs/rework/              VSA nova koda
    ├── data/                    save format, migracije, serializacija (M1)
    ├── diag/                    profiling, instrumentacija, test scenariji (M2)
    ├── entity/                  passenger/mount, hitbox (M3)
    ├── movement/                flight modes, navigatorji, move helperji (M4)
    ├── ai/                      scheduler, LOD, budget (M5)
    ├── script/                  Java scripting platforma (M6)
    │     ├── api/               tipiziran API, ki ga vidijo skripte
    │     ├── compile/           prevajanje, cache, hot reload
    │     └── hooks/             hook registry
    ├── anim/                    animacijski sistem (M7)
    ├── chat/                    chatbot providerji (M9)
    └── compat/                  mostovi na originalni API
```

Pravila:

- Nova koda **ne sme** biti pogoj za delovanje originalne poti. Če se `rework` sloj izklopi
  v configu, mora mod delovati kot original.
- Vsak podpaket ima svoj `package-info.java` z eno povedjo, kaj počne in v kateri fazi je nastal.
- Nič v `rework/` ne kliče drugega `rework/` podpaketa brez vmesnika, razen `data/` in `compat/`,
  ki sta skupna.

---

## 6. Testna strategija

Trije nivoji, vsak z drugim namenom:

| Nivo | Kaj | Kje | Kdaj teče |
|---|---|---|---|
| **Karakterizacijski** | dokaže, da obnovljena koda dela isto kot original | `dev/src/test/java`, taska `test` in `testOriginal` | vsak build |
| **Enotni** | dokaže, da nov popravek dela | isto | vsak build |
| **Integracijski v igri** | scenariji v razvojnem svetu | `dev/run/saves/` + skripte v `docs/` | pred zaključkom paketa |

**Pravilo:** vsak bug iz R1–R9 dobi najprej **reprodukcijo**, ki pade, potem popravek, ki jo
naredi zeleno. Bug brez reprodukcije se v `docs/04-STANJE.md` označi kot
*"nereproduciran — hipoteza"*, ne kot popravljen.

Integracijski scenariji se zapišejo kot ponovljiv postopek (seed, ukazi, pričakovan izid) v
`docs/scenariji/`, da jih lahko ponovi katerakoli seja.

---

## 7. Meritve

Ne obstaja "to je hitrejše" brez številke. Minimalni protokol (iz `PLAN_IMPLEMENTACIJE.md` §6):

- isti stroj, isti JVM parametri, isti seed in scenarij
- 2 min ogrevanja, 5 min merjenja, vsaj 3 ponovitve
- meri se: MSPT p50/p95/p99 in max, TPS, alokacije/s, GC premori, heap po GC
- klient posebej: frame time p95/p99

Rezultati gredo v `docs/meritve/<datum>-<paket>.md` in se povzamejo v `docs/04-REGISTRI.md` → "Meritve" (in v dnevnik v `docs/04-STANJE.md`).

---

## 8. Licence in izvor kode

| Vir | Licenca | Kaj smemo |
|---|---|---|
| CustomNPCs original | Noppes, glej `dev/LICENSE.txt` | lokalna predelava; distribucija po pogojih originala |
| BetaZavr fork | CC BY-NC 3.0 | branje, ideje, prevzem z atribucijo; **ne komercialno** |
| CustomNPC+ (KAMKEEL) | branch po dovoljenju avtorja | branje, ideje; port zahteva preverbo pogojev |

**Pravilo:** vsak prevzet blok kode dobi v izvorni datoteki komentar z izvorom, verzijo in
licenco, in vnos v `docs/04-REGISTRI.md` → "Prevzeta koda".

Ta rework je za osebno/lokalno uporabo. Pred kakršnokoli javno distribucijo je treba
pogoje preveriti posebej.

---

## 9. Dnevnik odločitev

Vsaka nova arhitekturna odločitev gre sem, z ID-jem, datumom in razlogom.
Odločitve se ne brišejo — če se preglasi, se doda nova, ki staro označi za nadomeščeno.

| ID | Datum | Odločitev | Razlog |
|---|---|---|---|
| D-001 | 2026-09-11 | Build base = uradni 01Oct19 JAR | obstoječe preverjeno okolje, veljaven audit, združljivost z uporabnikovimi svetovi |
| D-002 | 2026-09-11 | BetaZavr in CustomNPC+ sta referenca, ne osnova | ohranimo baseline; licenčne in tehnične omejitve |
| D-003 | 2026-09-11 | Postopno širjenje source drevesa | vsak korak preverljiv; `verify-package.ps1` ostane uporaben |
| D-004 | 2026-09-11 | Enosmerna migracija save formata z backupom | čist nov format brez bremena, a brez izgube obstoječih NPC-jev |
| D-005 | 2026-09-11 | Samo 1.12.2, brez multi-verzijske abstrakcije | vsa energija v eno verzijo; Java 8 zaradi Nashorna ostane |
| D-006 | 2026-09-11 | Vsa nova koda v `noppes/npcs/rework/` | jasna meja med originalom in reworkom; možnost izklopa |
| D-007 | 2026-09-11 | Vsaka funkcionalna sprememba privzeto v original načinu | brez tihih sprememb obnašanja obstoječih svetov |
| D-008 | 2026-09-11 | Forenzika in migracija za minorni R9 sta odloženi; splošna B1/B2 zaščita ostane | uporabnik nima pokvarjenih datotek; simptoma `waiting` → `following` ne širimo v drag obnovitveni projekt, ko imajo R1–R8 večji vpliv |
| D-009 | 2026-09-11 | Asinhroni world zapisi imajo executor na server sejo; splošni `CustomNPCsScheduler` ostane ločen | world pot in snapshot se zajameta ob zahtevi, shutdown izprazni samo podatkovne zapise; klientskih paketov in GUI zamikov ne smemo prekiniti z ugašanjem globalnega schedulerja |
| D-010 | 2026-09-14 | Mapiran original se na novi delovni postaji regenerira; identiteto dokazujejo testi in `verify-package.ps1`, ne SHA-256 iz locka | `dev/libs/` je v `.gitignore`, zato se prvotni artefakt ob selitvi izgubi. Deobf izhod ForgeGradla ni bitno ponovljiv med postavitvami, vsebina pa je: vseh 1716 vnosov se ujema po imenu, vseh 815 resourcev je bitno enakih, remapiranih je 571 razredov. `testOriginal` (31), `test` (47) in `verify-package.ps1` (25 pricakovanih zamenjav) dajo enak rezultat kot pred selitvijo, zato se hash v locku posodobi na novo vrednost |
| D-011 | 2026-09-15 | Pogoj vsake meritve M2 je prisilno naložen chunk (`ForgeChunkManager` ticket), ne igralec v svetu; instrumentacija ima zanj svoje tickete in ne uporablja modovega `ChunkController` | `WorldServer.updateEntities()` (`:628-644`) neha posodabljati entitete 300 tickov po nalaganju sveta, kadar je `playerEntities` prazen **in** `getPersistentChunks()` prazen. Ticket odklene oba vratarja hkrati — zanko na ravni sveta in `range = 0` v preverbi ±32 blokov na ravni chunka — in ne potrebuje človeka pred zaslonom, zato je scenarij ponovljiv in skriptabilen. Modov `ChunkController` (opravilo 8) se ne uporabi, ker merilni pogoj ne sme biti odvisen od kode, ki je predmet meritve. Pogoj se zapiše v sam posnetek kot `world.chunks.forced`, da meritve brez njega ni mogoče pomotoma brati kot veljavne |
| D-012 | 2026-09-17 | *(Stopnja D delno nadomeščena z D-022 — samo kot izbirno ozadje iz zunanje knjižnice.)* **Vanilla pathfindinga ne prepisujemo.** Popravljamo ga po stopnjah A→B→C, vsako pod stikalom in z meritvijo; stopnja D (async iskanje, flow fieldi, lasten gibalni sklad) je **zavrnjena**, dokler ne pade pogoj spodaj | Meritev M2.2 je pokazala, da od štirih opaženih simptomov navigacije **nobeden ni A\* algoritem**: dva sta napaki CustomNPCs AI (mutex biti `EntityAIAttackTarget:38` — *24. 9. ovrženo, D-020* —, `minRange` vezan na `npc.width` `:98`), eden je predpogoj `PathNavigateGround.canNavigate()`, eden pa način uporabe (en sam `navigateTo` čez več kot `NpcNavRange`). Prepis A\* bi tri od štirih pustil nedotaknjene. Vanilla A\* ima resnične slabosti (proračun 200 vozlišč `PathFinder:65`, manhattanska cena pri 8-smernem gibanju `PathPoint:86`, domet vezan na `FOLLOW_RANGE`), a so to **namerne varovalke za MSPT**, ne napake: pri 27 dejavnih NPC-jih je p99 že 115 ms od 50 ms proračuna, zato bi večji proračun vozlišč brez predpomnjenja šel naravnost v lag. Največji pričakovani dobitek ni kakovost poti, ampak **cena**: 8 NPC-jev proti istemu cilju izvede 8 neodvisnih iskanj (M5.6). Stopnja D se odpre šele, če po M4.10, M4.11 in M5.6 merila iz M2.7 še vedno padajo; sama po sebi prinese race conditione med iskanjem in spreminjanjem sveta, kar je edini del tega sklopa z resnično visokim tveganjem |
| D-013 | 2026-09-16 | Rep porazdelitve `server.tick.ns` se pripiše s tabelo 16 najpočasnejših tickov s kontekstom (NPC-ji, `ChunkEvent.Load`/`Unload`, `WorldEvent.Save`), ne s profilerjem in ne s hranjenjem vseh tickov | Percentili povedo, koliko tickov je počasnih, ne pa katerih in zakaj; prva veljavna meritev ima `p95 = 1,6 ms` in `p99 = 81,8 ms` in trije kandidati so bili enako verjetni. Hranjenje vseh tickov pri 20 TPS in urah merjenja ni mogoče, sampling profiler pa bi spremenil to, kar merimo, in bi zahteval agenta v dev zagonu. Tabela top-N je na vroči poti ena primerjava brez alokacije, kontekst pa pride iz dogodkov, ki jih zbiralnik že posluša. Ločeni števci tickov nad 10/25/50/100 ms povedo pogostost, ki je iz tabele ni mogoče brati. Pripis ne trdi vzročnosti, samo sočasnost — in merilo S2 (najpočasnejši tick v tabeli = `max` porazdelitve) dokazuje, da oba merila gledata isti tick |
| D-014 | 2026-09-16 | Seja prevaja in testira `noppes/npcs/rework/**` sama, proti mapiranim razredom iz `dev/build/tmp/recompileMc/compiled`; uporabnikov gradle zagon ostane edina avtoriteta za obnašanje v igri | Do 15. 9. je bila vsaka vrstica kode nepreverjena, dokler je ni pognal uporabnik — `rwdiag-run.ps1` je bil 15. 9. objavljen brez sintaktične preverbe. Forge in Minecraft razreda sta v projektu že prevedena in mapirana, zato sta dovolj za `javac --release 8` in JUnit; manjkata samo zunanji knjižnici (guava, log4j), ki nista v repozitoriju, zato se za prevajanje nadomestita z minimalnima začasnima nadomestkoma **izven** repozitorija. Kar se s tem preveri: sintaksa, imena in podpisi Forge API-jev, logika brez Minecrafta. Kar se s tem **ne** preveri: nalaganje moda, obnašanje v svetu, meritve. Zato se paket še vedno ne šteje za narejenega, dokler ne steče `rwdiag-run.ps1` |
| D-015 | 2026-09-17 | **Kakovost navigacije se meri s sondo z lastnim `PathFinder`-jem nad `NodeProcessor`-jem entitete**, ne z `navigator.getPathToXYZ`; pogostost iskanj se opazuje kot dodelitev novega objekta `Path` in je **spodnja meja**, ne točno število | Iz opazovanja AI-ja se kakovost poti ne da prebrati: NPC na koncu **delne** poti izgleda enako kot NPC, ki se ne premika, in prav ta zamenjava je 17. 9. stala eno napačno razlago (Q11). Sonda zato sproži iskanje z znanim ciljem. Navigatorjeva pot za to ni uporabna, ker si `getPathToPos` cilj zapomni v `targetPos` in ob istem cilju vrne že izračunano pot (`PathNavigate.java:111-124`): sonda bi spremenila stanje tega, kar meri, in pri ponovitvah merila branje predpomnilnika namesto iskanja. `getNodeProcessor()` je javen, `PathFinder.findPath(IBlockAccess, EntityLiving, BlockPos, float)` pa je natanko tisto, kar kliče navigator, zato sonda meri isto kodo brez stranskega učinka — a **samo iz server niti**, ker je `NodeProcessor` deljen in ga `findPath` inicializira in počisti. Celost poti se ne bere iz `Path.getTarget()`, ker je ta `@SideOnly(Side.CLIENT)` (`Path.java:152`), ampak iz primerjave zadnje točke poti s ciljnim vozliščem (meja 2,0 bloka, ista kot v scenariju). Šesta veličina (iskanj na tick) nima dogodka in klicnih mest v M2 ni mogoče instrumentirati (`EntityNPCInterface` pride z M3.1), zato zbiralnik šteje spremembo **identitete** objekta `Path`: to so uspešne dodelitve poti, ne vsa iskanja. Iskanje, ki vrne `null` ali ga zavrne `canNavigate()`, je za ta števec nevidno, zato je številka spodnja meja; cena navigacije na tick se oceni kot zmnožek veličin 5 in 6, točno število pa pride z M3.1 |
| D-016 | 2026-09-18 | **Ena ponovitev je cel zagon merilne skripte v svežem svetu**, ne ponovno branje istega loga in ne drugo pometanje sonde v istem zagonu; zapis zagona loči **odtis** (pogoji meritve) od **veličin** (kar se meri), združuje pa se po imenih veličin in ne z regexom čez formatirano tabelo | Protokol §7 zahteva tri ponovitve, tabela M2.7 pa je nastala iz enega zagona in zato nima razpona — brez razpona ni praga, nad katerim sme A/B v M4.10–M4.13 sploh trditi izboljšavo. Svet v `dev\run\world` se med zagoni ohrani z vsemi NPC-ji vred, zato bi drugi zagon `nav-run.ps1` brez reseta meril 32 NPC-jev namesto 16; reset (`.\testworld.ps1`) je zato del ponovitve, merilo T4 (enak odtis v vseh ponovitvah) pa ga ujame tudi, kadar odpove ali ga kdo izklopi. Ločnica odtis/veličine je nujna, ker bi bila mediana čez zagone z različnimi pogoji povprečje različnih poskusov in bi izgledala enako verodostojno kot meritev. Združevanje prek strojno berljivega zapisa (`meritve-lib.ps1`, shema 1) namesto prek `.md` tabele: sprememba širine stolpca ne sme tiho pokvariti agregata. Prag 20 % je samo oznaka `stabilna`/`SUMNA`; pravi prag posamezne veličine je njen izmerjeni razpon, in kadar je mediana 0, se relativni razpon ne izračuna (`n/a`), ker bi bil nadomestek številka, ki izgleda kot meritev in ni |
| D-017 | 2026-09-23 | **Baseline M2.6 se meri s spawnom naenkrat**, `perf-run.ps1 -Razprseno` ostane za primerjavo | Preverba idle 500 (z `nogui`): razpršen spawn ne zmanjša repa (p95 206 proti 218 ms, ticki > 50 ms 291 proti 275), počasni ticki so enakomerno po fazah `tick % 10`. Hipoteza sinhronega `ticksExisted % 10` je ovržena; spawn naenkrat je enostavnejši in enak stanju po restartu serverja |
| D-018 | 2026-09-24 | **Kdo krmili nosilca, odloča `RiderState`, izvede pa se kot razveljavitev vanilla prepisa** (zajem stanja nosilca pred tickom jahača, vrnitev po njem), ne kot poseg v AI nosilca ali v `updateEntityActionState` | `EntityLiving.updateEntityActionState` je `final` in je vanilla spider jockey; coremod/ASM zaradi ene vrstice ni upravičen. Nosilec se posodobi pred potniki, zato zajeto stanje je natanko stanje po nosilčevem AI ticku. Nosilci, ki niso NPC, ostanejo vanilla v vseh načinih; vanilla krmiljenje z jahačem ostane na voljo (način 1) kot osnova za M3.10 |
| D-019 | 2026-09-24 | **Popravki jahanja (M3.3–M3.5) si delijo eno stikalo `RwMountSteering`**; gating gibalnih taskov je eksplicitna preverba v tasku, ne mutex | Gating in `tpTo` sta posledica odločitve, kdo krmili — ločeno stikalo bi dovolilo nesmiselne kombinacije (nosilec krmili, jahač pa ga vsak tick preteleportira). Mutex bitov ni mogoče uporabiti, dokler M3.6 ne popravi `EntityAIAttackTarget` (vsi taski delijo `PASSIVE`). *Popravek 24. 9. (M3.6): `PASSIVE` je vanilla bit MOVE in napad ga ima namerno (kot `EntityAIAttackMelee`); razlog za eksplicitno preverbo ostane — gibalnega taska jahača z mutexom ni mogoče ugasniti, ne da bi ugasnili tudi napad* |
| D-020 | 2026-09-24 | **M3.6 ne spreminja mutex bitov; popravi prioriteto napada** (`RwAttackPriority`, privzeto 0). Mutex biti CustomNPCs taskov se berejo kot vanilla MOVE=1, LOOK=2, JUMP=4, ne po imenih v `AiMutex` | `AiMutex.PASSIVE/LOOK/PATHING` = 1/2/4 so po vrednosti vanilla MOVE/LOOK/JUMP. Napad ima 3 kot vanilla `EntityAIAttackMelee`, zato z njim noben gibalni task ne teče hkrati — premisa M3.6 in vrstica „mutex biti“ v D-012 sta bili napačni. Dodatek `PATHING` bi spremenil le razmerje do skoka. Javap pa pokaže, da `setResponse` napadu ne poveča `taskCount`, zato ima `EntityAIWander` isto prioriteto in ga napad ne more prekiniti. Preusmeritev je odobril uporabnik ([zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)) |
| D-021 | 2026-09-18 | **Formacije (M4.14) so vklopljive z ukazom, ne z nastavitvijo, in NPC-ja ne spreminjajo.** Enoto vodi paket `rework/formation`: en član poišče pot z vanilla navigatorjem, ostali sledijo svojemu mestu okoli sidra na tej poti; člani hodijo prek vanilla `PathNavigate` s potjo z eno točko, pravo iskanje poti dobijo samo, ko so daleč ali obtičijo. V NPC-ja pride kot AI task s prioriteto −1 in masko `PASSIVE|LOOK`, dodan od zunaj in vrnjen, če ga `updateTasks` pobriše; dodajanje in odstranjevanje sta odložena na začetek ticka sveta. Ob koncu se nastavita `startPos` in `orientation` (zastavica `brezsidra` to izklopi) | Uporabnik je izrecno zahteval, da se premik vojske iz skripte (timerji + `navigateTo`) prenese v Javo. D-012 zavrača flow fielde in lasten gibalni sklad za **vse** NPC-je, ker bi spremenila občutek gibanja obstoječih svetov; ta paket ni ne eno ne drugo: iskanje poti ostane vanilla, hoja ostane vanilla, deli se samo pot znotraj enote (M5.6 v malem). Stikalo po D-007 je tu sam ukaz: brez `/rwsquad` ali `FormationApi` paket ni prijavljen na event bus in ne doda nobenega taska, zato je obnašanje brez ukaza enako originalu; `-Drwformation=off` izklopi še ukaz. Task od zunaj namesto popravka `EntityNPCInterface.updateTasks`, ker je ta razred del M3.1 in bi njegov prenos zahteval ločen commit z baseline testom; ko M3.1 pride, se lahko task registrira tam. Skriptni API ostane nespremenjen (kompatibilnostna politika §3), skripta pride do `FormationApi` z `Java.type`, ker Nashorn v CustomNPCs nima `ClassFilter`-ja. Paket je izven vrstnega reda faz (M2 še ni zaključen), zato se ne šteje za narejenega, dokler ne tečeta F1–F12 in FA1–FA6 |
| D-022 | 2026-09-27 | **Stopnja D iz D-012 se odpre samo kot izbirno ozadje navigacije** (`RwNavBackend`) prek zunanje knjižnice `npcbaritone` (repozitorij `barittone_for_npc_rework`, API 1). Vanilla ostane privzeta; Baritone dobi samo NPC, ki ima ozadje izrecno vklopljeno (globalno v configu in na NPC-ju), samo `movementType == 0` (hoja) in samo velikosti, ki jih knjižnica podpira (D-019/D-028 knjižnice). Brez knjižnice v `mods/` ali z izklopljenim stikalom se CNPC obnaša bitno enako kot prej. Ozadje ostane v CNPC samo, če na scenariju M2.7 s tremi ponovitvami (D-016) izboljša veličine 1–3 čez izmerjeni razpon in ne poslabša 5–6 (vrata V4 knjižnice) | Pogoj iz D-012 ("race conditioni med iskanjem in spreminjanjem sveta") je naslovljen v knjižnici, ne v CNPC: iskanje teče na lastnih nitih nad posnetkom chunkov, ujetim na glavni niti; izvajalec vsak tick ponovno preveri cene naslednjih premikov in pot prekine, ko se svet spremeni. Izmerjeno v knjižnici (M5, 2026-09-27): 200 NPC-jev, prispevek glavne niti p95 **1,64 ms/tick** [1,56–1,66] v treh ponovitvah, 1 h stres z rušenjem brez izjeme. Knjižnica je preverjena v klientu (dev, Forge s pravim Baritonom, vanilla klient) in s tečaji T1–T4. Uporabnik je odločitev potrdil 2026-09-27. Revizija klicev navigatorja v CNPC: [`07-BARITONE-OZADJE.md`](07-BARITONE-OZADJE.md) |
| D-023 | 2026-10-03 | **M8 (migracija funkcij iz CustomNPC+, R3) je izločen iz plana.** Paketi M8.1–M8.11 ostanejo zapisani v `03-FAZE.md`, a nobena faza nanje ne čaka; CustomNPC+ ostane referenca za letenje (M4), Janino (M6) in animacije (M7) | Odločitev uporabnika. Projekt je po 62 sejah v M3, M8 pa je XL z visokim tveganjem (1.7.10 → 1.12.2 je ponovna implementacija) in brez izbranih funkcij (Q7). Pregled in plan: [`08-PREGLED-IN-PLAN.md`](08-PREGLED-IN-PLAN.md) |
| D-024 | 2026-10-03 | **Animacijsko jedro (R4/M7) je ločen mod `customNPC_entities_mod`**, ne nov `rework/anim`. M7 v reworku se zoži na integracijo: uskladitev verzije CNPC, script API/hooki, reload; M7.4 za navadne CNPC NPC-je samo na zahtevo. Ocena M namesto XL | Jedro že obstaja in deluje (v0.7.0, 5 mobov, YAML klipi, validator, Java/Python parity test, blending). Ponovna implementacija bi podvojila delo. Ločen mod ohrani pravilo, da je rework brez dodatkov bitno enak originalu. Odprto: entities mod je preveden proti CNPC 05Jul20, rework proti 01Oct19 |
| D-025 | 2026-10-05 | **R6 hitbox načini (M3.8) krmilijo odrivanje, ne trdnega trka.** Per-NPC NBT `RwHitboxMode` (0 = original, ključa ni; 1 = SOLID; 2 = SMART), globalno stikalo `RwHitbox` (privzeto 0 = vanilla za vse). NPC z načinom preusmeri `applyEntityCollision` in `collideWithEntity` v `rework/entity/RwHitbox`: vanilla smer in velikost potiska, delež vsake strani po `HitboxWeights`. Klient dobi učinkovit način v spawn NBT (ključ samo, ko ni 0), ker igralec odriva sam sebe na klientu. NPC brez hitboxa (`IsStatue`) ostane nespremenjen | Uporabnik je 5. 10. zahtevo opisal kot »da se ga ne da premakniti z rinjenjem« in dodal SMART (velikost, ščit). Trdi `getCollisionBoundingBox` iz prvotnega predloga R6 bi NPC-ja naredil oviro za pathfinding in premikajočo se steno, ki igralca lahko potisne v blok — tveganje, ki ga zahteva ne potrebuje. Odrivanje je ena točka (dva overrida), packet ID-ji in obstoječi NBT tipi ostanejo; nov NBT ključ in nov ključ v spawn NBT stari klient prezre. Knockback udarcev in voda nista del načina |
| D-026 | 2026-10-06 | **Poveljevanje skupinam prek zemljevida (R10) je nova zadnja faza M11**, po M10. Skupina je na zemljevidu ena oznaka, klik na cilj sproži pohod v formaciji. Nov promet gre prek ločenega kanala `rework`, obstoječi packet ID-ji ostanejo. Abstraktni pohod izven naloženih chunkov potrebuje lastno odločitev v M11.6 | Odločitev uporabnika: »čisto final faza, ko imamo že vse večinoma narejeno«. Gradi na M4.14 (formacije, pot sidra), M4.10 in M5, zato je smiselna šele po njih; ne sme zamakniti obstoječih faz |
| D-027 | 2026-10-07 | **`RwTargetPrefilter` je privzeto 2** (S1 + S2: predzavrnitev nesovražnih kandidatov pred raytraceom in poizvedba samo po igralcih, ko NPC drugih tarč ne more imeti). Izjema od D-007 (privzeto = original). Obstoječi configi obdržijo svojo vrednost (tudi `dev/run`, kjer je izrecno 0, zato baseline M2.6 in scenariji merijo original); nova namestitev dobi 2 | Odločitev uporabnika (Q13). Izbrane tarče so po izčrpnih testih ekvivalence enake originalu (`TargetPrefilterTest`), razlika je samo vsebina predpomnilnika `EntitySenses` znotraj ticka. Izmerjeno: idle-500 µs/NPC 32 → 6,6, p95 46 → 5,2 ms, boj nevtralen ([S1](meritve/2026-10-06-M5-S1-predzavrnitev-tarc.md), [S2](meritve/2026-10-07-M5-S2-poizvedba-igralci.md)). Vrnitev: `RwTargetPrefilter=0` |
| D-028 | 2026-10-07 | **Pod `RwHitbox=1` NPC nosilec, na katerem sedijo samo NPC-ji, sprejme potisk** z maso nosilca in vseh (tudi posrednih) jahačev. Velja samo na poti `RwHitbox` (vsaj ena stran ima način); nosilec z igralcem in trk dveh originalnih NPC-jev ostaneta vanilla | Odločitev uporabnika (Q15). Vanilla entiteti s potnikom potiska ne doda (`isBeingRidden`), zato se jahan nosilec in SOLID NPC nista razmaknila nikoli (M3.9, J4 0 / 0) — konjenica se je prekrivala s solid pehoto. Igralec krmili nosilca na klientu, zato zanj vanilla ostane. Izmerjeno v `m39-run.ps1`: J4 nosilec v0 / solid 0, J5 delež po masi nosilec + jahač ([zapis](meritve/2026-10-07-M3.9-solid-jahanje.md)) |
| D-029 | 2026-10-08 | **Pred začetkom vsakega milestona je obvezna raziskava izvedbe (paket M?.0), plan pa ni dogma.** Raziskava pregleda kodo, izmerjene podatke in zunanje vire (drugi modi, Paper/Sponge/Forge, literatura) ter za vsak paket milestona odloči: ohrani, spremeni, zavrzi ali dodaj. Rezultat je zapis v `docs/raziskave/` in posodobljeni `03-FAZE.md`, `02-ZAHTEVE.md`, README in po potrebi nove odločitve D-0NN. Velja tudi za nadaljevanje M5 (M5.0) in za M4 (M4.0). Spremembe, ki spremenijo obseg ali cilj zahteve, gredo kot vprašanje uporabniku v `04-STANJE.md` | Odločitev uporabnika. Plan, zahteve in del odločitev so nastali s šibkejšimi AI modeli, zato ni gotovo, da so pravilno zasnovani. Že dvakrat se je izkazalo, da je bila premisa napačna: mutex biti v M3.6 (D-020) in ocena, da je v boju strošek iskanje poti, ko je profil pokazal sledenje poti (M5-S P1, 8. 10.). Slepo sledenje planu bi porabilo seje za napačne pakete |
| D-030 | 2026-10-08 | **Plan M5 po raziskavi M5.0.** M5.1 (globalni script lock) se preseli v M6.6 brez cilja MSPT; M5.3 in M5.7 (prostorski indeks) se zavržeta, M5.4 kot samostojen paket prav tako; M5.2 in M5.6 sta pogojna na nov scenarij nedosegljive tarče (M5.9). Dodani: M5.10 (neposredna meritev sledenja poti in pogoj veljavnosti A/B boja), M5.11 (S14b — pomnjenje tipa vozlišča), M5.12 (S4), M5.13 (S7), M5.14 (S16 — senčna polja `EntityDataManager`), M5.8 (kapaciteta). Sledenje poti brez branja blokov (S15, vzor 1.21.1) je sprememba gibanja in gre v M4.13 | Profil JFR in A/B serije: `ScriptContainer` 1,8 % (skripte-500), iskanje poti 2 % (boj-500), sledenje poti največji strošek boja; model zank vanilla `isDirectPathBetweenPoints` da ~1.100 branj blokov na klic, izmerjeno 1.110. Cilj R5 je nespremenjen, spremeni se samo pot do njega. [Zapis](raziskave/M5.0-preostanek-performance.md) |
| D-031 | 2026-10-08 | **Optimizacije smejo spremeniti vanilla/Forge razrede, če so zapakirane v CNPC jar.** Izvedba je lasten FML coremod v istem jarju (`IFMLLoadingPlugin` + `IClassTransformer`, ASM, ki ga že prinašata LaunchWrapper/Forge), brez novega obveznega moda in brez vgrajene kopije Mixina. Vsak poseg ima stikalo s privzetim originalom (D-007), dokaz enakosti in A/B nad šumom (D-016). Razširi D-006: koda ostane v `noppes/npcs/rework/` (podpaket `rework/core`), cilj posega pa je lahko vanilla razred. Izbirni zunanji modi (npr. `npcbaritone`, D-022) ostanejo izbirni | Odločitev uporabnika (Q16): »ne bit ujet v CNPC — če nekaj drastično izboljša performance in ga lahko zapakiraš v mod, naredi«. MixinBooter kot zunanji mod bi bil nova obvezna odvisnost; Mixin, vgrajen v naš jar, bi trčil z modpacki, ki Mixin že naložijo (MixinBooter, VanillaFix ipd.). Lasten transformer ne potrebuje ničesar, kar strežnik že nima. Cena: ASM je krhkejši od Mixina, a cilj (1.12.2, Forge 2847+) se ne spreminja |
| D-032 | 2026-10-09 | **Optimizacija z dokazano enakim izidom in izmerjeno koristjo je privzeto vklopljena.** Pogoja: dokaz enakosti (test ekvivalence in/ali način preverbe v svetu brez neujemanj) in A/B nad šumom (D-016). Stikalo ostane (vrnitev na original), privzeta vrednost pa je optimizirana. D-007 še velja za vse, kar **spremeni obnašanje** (drugačen izid, časovni potek, kakovost gibanja): tam ostane privzeto original. Obstoječi configi obdržijo svojo vrednost (tudi `dev/run`, kjer so stikala izrecno 0, da baseline in scenariji merijo original); nova namestitev dobi optimizirano. Prvi primer: `RwPathFollowCache` privzeto 1 (S14b, Q17); D-027 je bil isti primer pred tem pravilom | Odločitev uporabnika (Q17): »če narediš nekaj, kar je boljše od originala, to obdržiš«. Posamezno vprašanje za vsako dokazano enako optimizacijo bi le zamikalo izid; D-007 je bil namenjen tihim spremembam obnašanja, ki jih take optimizacije nimajo. S14b: 0 neujemanj v 1.245.605 primerjavah, boj-500 MSPT povp 8,73 → 5,88 ms ([M5.11](meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md)). Vrnitev: `RwPathFollowCache=0` |
| D-033 | 2026-10-14 | **Klientska optimizacija (FPS, čas sličice) je obvezen del R5/M5, ne izbirna veja.** M5 ima dva dela: **M5-S** (strežnik, MSPT/TPS) in **M5-K** (klient, čas sličice p95/p99, FPS, 1 % low). M5 je zaključen šele, ko sta zaključena oba. Vrstni red: **M5.8 → M5-K.0 (harness M2.4r) → M5-K.1 (profil klienta) → K-paketi** v vrstnem redu, ki ga potrdi ali spremeni profil (izhodišče iz [09](09-PERFORMANCE-RAZISKAVA.md): K5 → K12 → K13 → K10). Pravila M5-S veljajo nespremenjena: A/B nad šumom (D-016), dokazano enak izid → privzeto vklopljeno (D-032); kar spremeni videz (LOD, impostor, redkejši klientski tick, razdalja imen), je sprememba obnašanja in ostane privzeto original (D-007) | Odločitev uporabnika 14. 10.: »ja to je NUJNI modul optimizacije. Jst kle hocem server side in client side optimizacijo«. Do takrat je bil M5-K v `03-FAZE.md` samo stavek »ločena veja z vhodnim pogojem M2.4r«, brez mesta v vrstnem redu, izhodni kriterij M5 pa čisto strežniški — po M5.8 bi plan šel na M5.2/M4.0/M6 in klient bi obvisel. Ob tem je strežnik po M5.16 v idle prišel do točke, kjer je klient ozko grlo: strežnik zdrži 3500+ stoječih NPC-jev (lestvica M5.8), en NPC pa stane ~18 klicev display listov na sličico (09, **[H, iz kode]**) |
| D-034 | 2026-10-14 | **Vidnost NPC-jev na daljavo (M5-V) je obvezen modul R5/M5, na koncu M5-K.** Izvedba je **varianta B** iz [raziskave M5-V.0](raziskave/M5-V.0-daljna-vidnost.md): NPC-ji onkraj dometa vanilla sledilnika gredo na klient kot lahki nadomestki (~16 B, ~1 Hz, samo ob spremembi) po **lastnem kanalu `rwfar`** (CNPC kanali, ID-ji paketov in NBT nespremenjeni), videz enkrat na videz (hash, brez imena in naslova); klient speče impostor (8 smeri, atlas z mipmapi) in vse oddaljene NPC-je nariše v enem klicu; isti impostor nadomesti model sledenih NPC-jev onkraj `RwImpostorStart` (L2). Domet sledilnika CNPC (64) se **ne** dvigne (varianta A je samo primerjalna celica M5-V.1). Strežnik pošilja samo klientom, ki to zahtevajo (`HELLO`). Videz se spremeni → stikala privzeto original (D-007), dokler ne odloči Q23. Pravila R5 veljajo: brez spremembe frekvence ticka, AI ali vanilla sledilnika | Zahteva uporabnika 14. 10.: »nakonc client side performenca bos pa dodal se tale modul za nujno implementacijo … da bi se npcje videlo tudi na 24 chunkov daleč, samo pač bi bila textura sm par pixlov … neb impactal performance«. Raziskava: NPC-ji so danes vidni do ±64 blokov, ker jih strežnik dlje ne pošlje (`tracker(64, 3, true)`, `EntityTrackerEntry.isVisibleTo`); klient bi jih pri RD ≥ 20 narisal do 160 blokov — samo klientski LOD dometa ne poveča. Dvig dometa na 368 blokov pomeni 33× površino, GZIP spawn NBT na NPC in polno klientsko entiteto; pri 24 chunkih je NPC visok ~4 px, zato je impostor vizualno enakovreden modelu |
| D-035 | 2026-10-14 | **Parametri M5-V po odgovorih Q23–Q25.** (1) Po sprejemu (M5-V.8, cilji izpolnjeni) je vidnost na daljavo **privzeto vklopljena**: strežnik `RwFarView=1`, klient `RwFarView=2` — izjema od D-007 po izrecni odločitvi uporabnika; do sprejema 0, obstoječi configi obdržijo vrednost (kot D-032), `dev/run` ostane 0. (2) Cilj je **dedicirani strežnik**: doseg `(view-distance − 1) · 16`, za 24 chunkov `view-distance=24`; M5-V.1 meri na dediciranem strežniku s klientom pri `view-distance` 10 in 24. (3) Vrstni red M5: **M5-K.0 → M5-K.1 → K5 → M5-V.1 … M5-V.8 → K12 → ostali K-paketi**; K13 in K10 se po M5-V.8 ponovno ocenita. (4) `RwFarLos` je privzeto **0** (Q26): strežnik pošlje vse NPC-je v dosegu brez preverbe vidne črte, imena in naslovi se ne pošljejo nikoli; operater lahko vklopi `RwFarLos=1` | Odgovori uporabnika 14. 10.: Q23 »da«, Q24 »strežnik«, Q25 »nared po priporočanju«, Q26 »naredi kar je najbol priporočeno« (vanilla znotraj 64 blokov prav tako pošilja vse NPC-je ne glede na zidove; raytrace bi stal ~1 Hz na par igralec–NPC). Posledica Q24: Minecraft 1.12 nima ločene razdalje simulacije (prišla je v 1.18), zato `view-distance=24` pomeni 2.401 tickajočih chunkov na igralca proti 441 pri 10 (5,4×), z vsemi entitetami in polnim AI NPC-jev; to je vanilla cena, ki je M5-V ne more odpraviti, ker je uspavanje oddaljenih NPC-jev v R5 prepovedano. M5-V ne potrebuje K13/K10, stopnja L2 pa zmanjša vrednost K10 |
| D-036 | 2026-10-15 | **Navigacijsko ozadje se izbira na posameznem NPC-ju v GUI; privzeto ostane vanilla.** Paket **M4.15** v M4 doda v nastavitve AI izbiro *vanilla* / *Baritone* in prikaz **dejanskega** ozadja z razlogom, kadar izbira ne velja. Uporabi obstoječi NBT ključ `RwNavBackend` in globalno stikalo iz D-022; nič novega v obstoječih paketih, tipih ali enumih. Baritone **ni** kandidat za privzeto ozadje vseh NPC-jev, dokler M4.15a ne izmeri cene pri 500–2000 NPC-jih (boj, nedosegljiva tarča) in obnašanja pri velikostih ≠ igralčeve, letečih in jahačih | Zahteva uporabnika 15. 10.: »dodaj v plan … GUI okno, ki bo povedalo, ali uporabljaš vanilla ali Baritone path finding«. Kritična ocena v seji 90: Baritone je izmerjeno boljši v kakovosti poti (grla 8/8 proti 1/8–6/8 z `crowdYield`, krajše poti), cena pri velikem N pa ni izmerjena (knjižnica: 200 NPC-jev 1,64 ms/tick glavne niti, D-022); model gibanja predpostavlja igralčev hitbox in fiziko, letenja in jahačev ne pokrije. Izbira na NPC-ju omogoči Baritona tam, kjer se splača (patrulje, spremstvo, formacije, M11), brez cene za ostale |
| D-037 | 2026-10-16 | **`RwPathNegCache` (S5, M5.6) je privzeto 1, M5.2 se zavrže.** NPC, čigar iskanje poti do tarče ni našlo cele poti, 20 tickov ne išče znova poti do (skoraj) iste tarče. To je **sprememba obnašanja** (izjema od D-007, D-032 ne velja): v 59 % preskokov bi original vrnil delno pot do ovire, NPC zato do 20 tickov stoji namesto da se drenja ob oviri, ko se pot odpre, reagira do 20 tickov kasneje. Stikalo ostane (0 = original). M5.2 (proračun iskanj na tick) rešuje isti strošek grobo, zakasni tudi NPC-je s potjo, in po S5 nima podlage | Odgovor uporabnika na **Q21** (16. 10.) po priporočilu seje 91. Dobitek [M5.6](meritve/2026-10-10-M5.6-negativni-predpomnilnik.md): `nedosegljiva`-500 MSPT povp 17,854 → 14,462 ms (−19 %), p95 22,02 → 18,35, alokacija 292 → 135 MB/s, ticki > 25 ms 45 → 10. Nedosegljiva tarča je najslabša celica M5.8 (1,4×). Če se zakasnitev ob odprtju poti izkaže za moteče, se doda razveljavitev ob spremembi bloka v bližini (ni v planu) |
| D-038 | 2026-10-20 | **Plan M4 po raziskavi M4.0.** M4.1 (branje CustomNPC+) je opravljen v M4.0, M4.4 (`isNotColliding`) in M4.9 (meritve 3D) se združita v nova paketa, M4.12 (lasten `PathNavigate` z A\*) se zavrže, M4.11 (cena diagonale) je pogojen na M4.13, M4.5 (3D iskanje) postane odločitvena točka po vzoru 1.21.1 (podrazred `PathFinder`, brez lastnega iskalnika, oktalnega drevesa in asinhronosti). Dodana **M4.16** (scenarij letenja in dometa: proge zid/vogal/daleč/labirint, drsenje, celica `leteci-200`) kot vhod za vse pakete letenja in M4.10, ter **M4.17** (`RwPathNavigateFlying`: sledenje poti letečega z vidno črto). M4.10 pokrije tudi cilj izven dometa. Vrstni red znotraj M4: M4.14b → M4.16 → M4.10 → M4.17 → M4.2 → M4.3 → (M4.5) → M4.6 → M4.7; M4.13 → (M4.11) in M4.15 neodvisno. Izboljšave letenja so podrazredi vanilla navigatorja in novi helperji pod stikali, privzeto original (D-007); koda CustomNPC+ se ne prenaša (D-002) | Raziskava [M4.0](raziskave/M4.0-gibanje-in-letenje.md): vanilla 1.12 `PathNavigateFlying.isDirectPathBetweenPoints` vedno vrne `true` (leteči NPC cilja zadnjo točko iste višine, tudi za vogalom); `FlyingMoveHelper` je `GhastMoveHelper` (sunki vsake 4 ticke, do ~3,5 bloka drsenja po modelu); cilj dlje od `NpcNavRange` vrne `null`, ne delne poti; CustomNPC+ ima isto iskanje kot 1.12. Napačna premisa plana je bila, da je jedro problema iskalnik (M4.1/M4.5, L) — iz kode je jedro v sledenju in krmiljenju. Brez scenarija z vogalom, razdaljo > 32 in drsenjem noben paket letenja ni merljiv (M2.3 ima samo zid čez vso širino). Obseg R2 se ne spremeni; dodatne nastavitve po vzoru CustomNPC+ so vprašanje Q31 |
| D-039 | 2026-10-21 | **`RwLodStart` (M5-K K5, stopnja L1) je privzeto 0 (original); vklop je izbira uporabnika ali operaterja.** NPC dlje od praga se upodobi brez oči, s ploščato plastjo pokrivala modela (videz `HeadWearType=0`) in brez imena/oblačkov. To spremeni videz, zato D-032 ne velja in ostane D-007. Priporočena vrednost ob vklopu je 32 blokov (izmerjena); ime pri pragu ≥ 23 ni prizadeto, ker ga original skrije onkraj 22,6 bloka. Paket K5 se šteje kot zaključen za L1; največje število upodobljenih NPC-jev, prekrivne teksture/delci in histereza na pragu niso narejeni in se ponovno ocenijo po M5-V (L2 impostor nadomesti L1 pri večjih razdaljah) | **Q32**, odločeno samostojno po pooblastilu lastnika (seja 102). A/B [K5](meritve/2026-10-21-M5-K-K5-lod-L1.md): idle-500 p50 11,464 → 8,691 ms (−24 %), p95/p99 −21 %, FPS +31 %, razponi ločeni; idle-200 (0 % onkraj praga) v šumu. Za privzeti vklop bi bila potrebna odločitev lastnika o videzu (kot pri Q30); dobitek pri 500 NPC-jih je primerljiv s K14b, ki je slikovno enak, zato je vklop K14b (Q30) boljši kandidat za privzeto pot |
| D-040 | 2026-10-21 | **Metoda odločilnega poskusa M5-V.1.** (1) Dedicated server in klient tečeta hkrati iz `dev`, strežnik v lastnem svežem svetu prek `runServer --world` (`-PrwWorld`), da `dev/run/world` in `server.properties` ostaneta nespremenjena; skripta pred postavitvijo preveri, da je strežnik res v tem svetu. (2) Prizorišče se vedno spawna pri razdalji 24, celica `view-distance` 10 se nato nastavi med tekom s `PlayerList.setViewDistance` (`/rwdiag vd`), kar vanilla naredi ob spremembi nastavitve (posodobi `PlayerChunkMap` in mejo sledilnika `(vd − 1) · 16`); tako je v obeh celicah isti svet, naložen pa samo do razdalje, kot na pravem strežniku. (3) Varianta A je **diagnostični ukaz** `/rwdiag track <bloki>` (refleksija na `EntityRegistration.trackingRange` razredov NPC-jev, pred spawnom), ne stikalo v konfiguraciji: je samo primerjalna celica, ne rešitev (M5-V.0 §4), zato ne sme obstajati kot nastavitev za igralce. Klient pri A dobi `renderDistanceWeight` 6 (sicer bi ga `isInRangeToRenderDist` pri RD 24 omejil na ~165 blokov). (4) Bajti proti klientu so žični (števec na začetku klientovega Netty cevovoda), torej točno to, kar strežnik pošlje temu igralcu | Odločeno samostojno po pooblastilu lastnika (seja 103), ker plan M5-V.1 metode ne predpiše. Alternativi — začasno prepisovanje `dev/run/server.properties` (tveganje, da ostane spremenjen in pokvari baseline) in konfiguracijsko stikalo dometa (nova nastavitev brez namena) — sta slabši. Scenarij: [M5-V.1](scenariji/M5-V.1-dalec.md) |
| D-041 | 2026-10-21 | **Merilo V12 v M5-V.1 je veljavnost vzorca, ne hitrost.** Zahteva ≥ 300 sličic v merilnem oknu (≥ 3 vzorci nad p99 in v 1 % low), ≥ 15 klientskih tickov/s (klient ni zastal) in okno 60 ± 2 s; FPS pod 10/s se izpiše kot opomba in je izmerjena velikost celice. Merilni pogoji (okno 60 s, ogrevanje 30 s, kamera, nastavitve) ostanejo enaki za vse celice | Odločeno samostojno po pooblastilu lastnika (seja 103). Poskusni zagon 24/368/5000 (21. 10.) je izpolnil vse druge kriterije (V9 5000/5000 upodobljenih, V11 brez ponastavitve uteži v merjenju) in dal 356 sličic v 60 s (6 FPS) pri 1201 klientskem ticku: prvotni prag 10 FPS je zavrnil prav ceno, ki jo poskus meri (varianta A je draga). Daljše okno za počasne celice bi spremenilo merilne pogoje med celicami, zato ni izbrano |
| D-042 | 2026-10-21 | **`/rwdiag vd` po zmanjšanju razdalje izprazni chunke zunaj `PlayerChunkMap` takoj in v celoti** (`FarProbe.dropOutsideView`: `queueUnload` vseh naloženih chunkov, ki jih `PlayerChunkMap` ne vsebuje, nato `ChunkProviderServer.tick` v istem ticku, dokler se število ne ustali; chunki spawna in vsiljeni ostanejo). Merilo V5 (naloženih NPC-jev = pričakovano iz geometrije chunkov) ostane nespremenjeno | Odločeno samostojno po pooblastilu lastnika (seja 104). Celica v1-10-64-1000 (p1, 21. 10. 09:39) je padla na V5/V6: 300 namesto 230 NPC-jev, 749 namesto 697 chunkov, poskusni zagon iste celice pa je dal 230. Vzrok je vanilla: `queueUnload` ne zagotavlja izpraznitve (Javadoc Forge), `tick` izprazni ≤ 100 chunkov na tick, vsak `getLoadedChunk` (npr. `isAreaLoaded` ±32 blokov pri posodobitvi NPC-ja ob robu) pa chunk iz vrste umakne, zato ostane naključen pas naložen in strežniški del celice (MSPT, naloženih NPC-jev) ni ponovljiv. Strežnik, zagnan pri razdalji 10, tega pasu nima, zato takojšnja izpraznitev obnovi namen D-040 (2). Alternativi — rahljanje V5 (merilo bi sprejelo neponovljiv prizor) ali čakanje na avtosave (`saveAllChunks` chunke spet uvrsti, NPC-ji ob robu jih spet umaknejo) — sta slabši |
| D-043 | 2026-10-21 | **Izid M5-V.1: varianta A je zavrnjena z meritvijo, varianta B (D-034) in cilj vd 24 (D-035) ostaneta.** Pri `view-distance` 24 in 5.000 NPC-jih domet 368 doda **+156,8 ms** sličice p50 (+184 ms p95, 128 → 6 FPS), **+28,4 KB/s** v mirovanju (~6 B/s in ~1,3 kB spawna na sledenega NPC-ja), MSPT pa se ne spremeni razločljivo. Izhodišče za M5-V.8 so celice originala (domet 64) iz [zapisa](meritve/2026-10-21-M5-V.1-dalec.md). Strežniški cilj "MSPT p95 +≤ 0,1 ms na igralca" se v M5-V.2/V.8 meri z lastnim nanosekundnim števcem kode `rwfar` na igralca (`/rwdiag`), MSPT celice ostane kontrolna velikost. Opomba za operaterja: vd 10 → 24 sama stane 5,8–6,5× MSPT (4–4,3× naloženih NPC-jev, 5,4× chunkov), 5.000 naloženih NPC-jev pri 24 na merilnem stroju ne drži 20 TPS (p50 56,6 ms) — vanilla cena, ki je M5-V ne spremeni | Odločeno samostojno po pooblastilu lastnika (seja 105) na podlagi matrike 8 celic × 3 (vse V0–V15 zelene). Varianta A presega cilja sličice (~184×) in omrežja (~28×), zato ni kandidat niti pri vd 10 (domet 144: +4 ms pri 1.000). A/B MSPT ima razpone 0,07–4,2 ms in 0,1 ms ne razloči; daljše okno ali več ponovitev bi spremenilo merilne pogoje in še vedno ne doseglo ločljivosti |
| D-044 | 2026-10-21 | **Izvedba strežnika M5-V.2 (kanal `rwfar`).** (1) **En ključ `RwFarView`** za strežnik in klient (v enem igralcu si delita JVM in config): strežnik pošilja pri vrednosti ≥ 1, klient z 1 pošlje `HELLO` (L3, M5-V.5), z 2 še L2 (M5-V.6); `RwFarRange` 16..1024, privzeto 384, omejen z `(vd − 1) · 16` in z dometom iz `HELLO` (`RD · 16`). (2) Pozicija se pošlje kot **spodnjih 24 bitov** v 1/8 bloka, klient jo razvije okoli svoje pozicije (`FarCodec.unwrap24`) — velja na vsaki koordinati sveta (absolutnih ±1.048.576 blokov iz raziskave bi omejilo svet). (3) Id videza je do M5-V.3 **0** (neznan); zastavica hoje iz dejanskega premika (> 0,01 bloka/tick), ne iz `isWalking()` (ta je resničen že za NPC z nastavljenim tavanjem). (4) Sporočila: `HELLO`, `BYE` (izklop na klientu), `SNAP` (odstranitve + vnosi, ≤ 1.024 postavk na sporočilo), `CLEAR` (izklop na strežniku). (5) Register NPC-jev (`IWorldEventListener`) je prijavljen vedno, tudi pri 0 — samo vodi seznam, igre ne spremeni — da `/rwfar 1` deluje med tekom. (6) Števec časa na igralca (D-043) je v `/rwfar` (`usNaTickNaIgralca`, `poIgralcih`), ne v `/rwdiag`: koda in ukaz sta v istem paketu, `/rwdiag` ostane splošna instrumentacija. (7) M5-V.2 vsebuje tudi **klientsko shrambo brez izrisa** (`FarClient`), da se kanal preveri od konca do konca v svetu (`v-run.ps1 -Far 1`, merila V16–V18); izris je M5-V.5. (8) `RwFarLos` (izbiren po planu) se odloži za M5-V.8 (privzeto 0 po Q26) | Odločeno samostojno po pooblastilu lastnika (seja 106). (1) Dva ključa z istim imenom v istem configu nista mogoča, ločena ključa pa bi v enem igralcu zahtevala dve nastavitvi za eno funkcijo. (2) Strošek je enak (9 B), meja sveta odpade. (3)/(4) Samo spremembe stanja so promet; napačna zastavica hoje bi ob vsaki spremembi tavanja pošiljala vnose brez premika. (6) A/B MSPT 0,1 ms ne razloči (D-043). (7) Brez prejemnika bi bil kanal preverjen samo z JUnit; preverba v svetu je pogoj, da se V.4/V.5 gradita na izmerjenem kanalu. Zmogljivost (A/B z originalom) ostane M5-V.8 |
| D-045 | 2026-10-21 | **Opis videza M5-V.3.** (1) Opis = podmnožica spawn NBT po **beli listi**: ime registra razreda (`Cls`), iz `display` `SkinUrl`, `Texture`, `CloakTexture`, `GlowTexture`, `UsingSkinUrl`, `Size`, `SkinColor`, `NoLivingAnimation`, nato `Armor`, `Weapons`, `StandingState`, `Orientation`, odmiki telesa, `ModelData`; **brez** imena, naslova, `ShowName`, boss bara, statistik, vlog/jobov (tudi Puppet, po raziskavi §5.2) in hitboxa; `SkinUsername` samo pri `UsingSkinUrl == 1` in **brez `Properties`** (podpisane teksture nosijo čas). Pol-vidnost nosi zastavica vnosa. (2) Hash = FNV-1a 64 + končno mešanje čez **kanonično** obliko (ključi urejeni), nikoli 0. (3) V vnos gre **kratek id** (`varint`, 1–2 B), ne 8-bajtni hash: strežniška tabela `FarLooks` deli `Look` med NPC-ji z istim hashem, id-ji se v tabeli nikoli ne ponovijo, nedejavni videzi v LRU 256. `LOOK` nosi id + hash + GZIP opis, klient hrani id → hash na svet in hash → opis v LRU 512 čez svetove. (4) Hash se ne računa na tick: NPC brez videza gre v vnos z 0 in v vrsto, ki se prazni ob koncu strežnikovega ticka, **največ 64 opisov na tick**; `UPDATE_NPC` (`EntityNPCInterface.updateClient`) NPC postavi v vrsto, stari id velja do ponovnega izračuna. Čas opisov se šteje v `usNaTickNaIgralca`. (5) `REQ` (≤ 256 id-jev) dobi odgovor samo za id-je, ki jih je igralec prejel, vsak id največ enkrat do novega `HELLO`, vrsta ≤ 1.024, **≤ 8 odgovorov na tick na igralca**; klient ponovi zahtevo po 10 s. (6) `FarCodec.VERSION` = 2 | Odločeno samostojno po pooblastilu lastnika (seja 107). (1) Bela lista: kar ni na njej, ne more razkriti zgodbe ali razbiti deljenja med kloni; nov vizualni ključ se doda zavestno. (3) 16 B vnos ostane (raziskava §5.1); hash v vnosu bi vnos povečal za polovico pri vsakem premiku. (4) Prvi krog pri 5.000 NPC-jih bi sicer v enem ticku izračunal 5.000 opisov. (5) Zaščita pred zlorabo: klient ne more zahtevati tujih videzov ali z istim id-jem množiti prometa. Preverba: JUnit (`FarLookTest`, `FarLooksTest`, `FarRequestsTest`) in V19 v svetu |
| D-046 | 2026-10-21 | **Peka impostorjev M5-V.4 (klient).** (1) Lažna entiteta iz opisa (`FarClient.description`, ni v svetu; precedens `GuiNpcMobSpawnerAdd`) se z rendererjem CNPC (`Render.doRender`, brez sence, ognja in imena) nariše v lasten FBO 128 × 256 z **ortogonalno** projekcijo pod **8 koti** (yaw `k · 45°`), s standardno osvetlitvijo entitet in **brez lightmapa** (svetlobo kraja doda izris M5-V.5). (2) Samodejni obrez: unija alfe vseh kotov, simetrično okoli osi entitete; če se dotika roba, se vidno polje podvoji (največ 2×). Okvir se ploskovno pomanjša v največ **32 × 64** tekslov, poravnan na dno celice 40 × 72 (rob 4 prozorne teksle). (3) Atlas: ena kvadratna RGBA tekstura, **4 stopnje mipmap** z ohranjeno pokritostjo alfe (Castano, bisekcija faktorja alfe), celice/trakovi poravnani na 8, en trak 8 okvirjev na videz; `GL_NEAREST` pri povečavi, `GL_NEAREST_MIPMAP_LINEAR` pri pomanjšavi. Stran `RwImpostorAtlas` (1024/2048/4096, privzeto **2048** = 168 videzov, ~21 MB VRAM; omejeno na mejo GPU). Trakovi po **LRU** (`ImpostorSlots`). (4) Proračun **en videz na sličico** (`RenderTickEvent` START); vrsta iz `FarClient` (nov `LOOK` ali vnos z znanim, nepečenim videzom). Skin igralca/URL: peka se odloži, dokler renderer ne pozna teksture (največ 600 poskusov). Brez FBO se peka trajno izklopi (`noFbo`). Ob izklopu `RwFarView` se atlas sprosti. (5) Preverba `ImpostorCheck` (samo harness, `impostorCheck=1`): model in ploskev impostorja z okvirjem istega kota v FBO 128² s perspektivo, ki ima pri **56 blokih** toliko pikslov na blok kot zaslon 1080 p pri FOV 70; merila, določena **pred** meritvijo: IoU silhuete ≥ **0,70** in povprečna razlika barve v preseku ≤ **32**/255 za vsak videz, združeno čez njegovih 8 kotov (najslabši kot se izpiše; 8 videzov × 8 kotov: osnova, 5 pravih skinov CNPC, velikost 10 in 2), občutljivost (enobarvni skin proti osnovi) > **64** | Odločeno samostojno po pooblastilu lastnika (seja 108). (1) Ortogonalna projekcija da okvir, neodvisen od razdalje, kar billboard L3 potrebuje; lightmap v peki bi zapekel svetlobo mesta peke. (2)/(3) 32 × 64 je ~2,5× ločljivost NPC-ja na 56 blokih (~25 px, raziskava M5-V.0 §3), zato mipmapi zadoščajo za večje razdalje; ohranjanje pokritosti prepreči, da bi tanki deli (roke, noge) z razdaljo izginili. 2048 je točka raziskave §5.4; 4096 za strežnike z mnogo različnimi videzi. (4) Peka enega videza stane povprečno 8,9–12,3 ms (največ 32–50 ms) zaradi `glReadPixels` — en videz na sličico omeji zastoj na eno sličico na nov videz. (5) Občutljivost dokaže, da merilo razlike zazna napačen skin; brez nje bi IoU sam sprejel napačno teksturo. Preverba: JUnit (`ImpostorFramesTest`, `ImpostorSlotsTest`) in V20/V21 v svetu |
| D-047 | 2026-10-21 | **Izris L3 M5-V.5 (klient).** (1) `FarRender` nariše vse nadomestke iz `FarClient` v **enem** `BufferBuilder` (lasten format položaj, UV, barva, normala, lightmap — `DefaultVertexFormats` 1.12 formata `POSITION_TEX_COLOR_NORMAL_LMAP` nima; normala je za senčilnike), pol-vidne (`F_HALF_VISIBLE`) v drugega z alfo 0,15 brez pisanja globine kot vanilla; cilindrični billboard, okvir `ImpostorFrames.frameFor` iz smeri kamera → NPC in yaw posnetka, alfa test 0,1, brez osvetlitve GL (osvetlitev entitet je zapečena), svetloba kraja `World.getCombinedLight` v lightmapu (osvežena ob menjavi bloka in krožno na 128 sličic; dokler chunk ni naložen, polna nebesna). (2) **Interpolacija**: nov posnetek začne premik pri trenutno prikazanem položaju in ga konča v 1 s (= kadenca strežnika); skok nad 16 blokov brez premika. (3) **Šiv**: nadomestek se ne nariše, če ima `WorldClient` živo entiteto z istim `entityId`; s strežniškim prekrivanjem 8 blokov (D-044) je to tudi **histereza** meje L0/L1 ↔ L3 — klient nima lastnega praga, zato ga tudi ne more utripati. Nepečen videz se ne nariše (brez nadomestne silhuete), zato strežnik videz izračuna že v predpasu 16 blokov pred notranjo mejo (`FarBand.PREFETCH`); mrtva entiteta z nadomestkom (po `SPacketDestroyEntities` do naslednjega ticka še v chunku) se ne nariše (`RenderLivingEvent.Pre` preklican, števec `FarRender.deadHidden`). (4) **Mesto v sličici**: coremod `RenderEntitiesPatch` v `RenderGlobal.renderEntities` tik pred edino konstanto `"blockentities"` vstavi `FarRender.renderEntities(renderViewEntity, camera, partialTicks)` (nevtralno za sklad, `COMPUTE_MAXS`; brez sidra ali z več sidri iste bajte), klic riše samo v prehodu 0; frustum iz istega prehoda. Če klica v sličici ni bilo, `RenderWorldLastEvent` (rezerva) nariše z lastnim frustumom in meglo prek `EntityRenderer.setupFog` (refleksija). Lastnost `rwcore.patch.render`, ukaz `/rwimpostor`. (5) Preverbe (harness, `v-run.ps1 -Izris 1`): V22 izris v merjenju, V23 A/B pikslov **sonde** na robu pasu (vd 24: 368 blokov) v pravi sličici s FOV 10, usmerjeno vanjo, V24 prehod meje sledilnika s hitrostjo hoje brez vrzeli | Odločeno samostojno po pooblastilu lastnika (seja 109). (1) En klic risanja je jedro variante B (raziskava M5-V.0 §5.5); alfa test z ohranjeno pokritostjo mipmapov (D-046) ohrani silhueto brez razvrščanja. (2) Brez interpolacije bi hodeč NPC skakal ~1 blok na sekundo. (3) Raziskava §5.3: id-ji so isti, zato šiv ne potrebuje razdalje; prvi zagon V24 (9c004fa8…) je dal 15 vrzeli (NPC, ki je med hojo šele vstopil v pas, je prvi krog dobil videz 0 → nepečen ob uničenju žive entitete; 15/170 se ujema s pričakovanim deležem ~9 %) in 400 dvojnih (mrtva entiteta ostane v chunku do ticka, ~5 sličic na tick); histereza pragov L1/L2 (`RwImpostorStart`) je stvar M5-V.6. (4) Sidro `"blockentities"` je enkratno v Forge 1.12.2-14.23.5.2847 (JUnit na pravih bajtih); tam sta megla in lightmap stanje entitet, `RenderWorldLastEvent` je po izklopu megle in po prosojni plasti. (5) Okno 854 × 480 pri FOV 70 da NPC-ju na 368 blokih ~1 × 2 piksla — test bi bil odvisen od podpikselne lege; FOV 10 je »daljnogled«, megla je odvisna od razdalje, zato dokaže, da izris na robu pasu ni porezan (frustum, daljna ravnina, megla). Sonda je najbolj oddaljen nadomestek vsaj 12 blokov pred koncem megle `RD · 16`: pri vd 10 je diagonalni NPC na 168,8 bloka za koncem megle (160) in tudi v originalu neviden (prvi zagon 1538c62d…: 0 pikslov); pri vd 24 meja 372 ≥ 368, merilo ostane. Hoja (4,3 bloka/s) je merodajna hitrost; nad ~8 blokov/s je lahko vrzel do ~1 s (omejitev, M5-V.8). Preverba: JUnit (`FarInterpTest`, `RenderEntitiesPatchTest`, `FarDrawCheckTest`) in V22–V24 v svetu |
| D-048 | 2026-10-21 | **Stopnja L2 M5-V.6 (klient).** (1) Z `RwFarView=2` sledeni NPC, ki ga vanilla ponudi v izris sveta (prehod 0) in je od kamere dlje od `RwImpostorStart` (privzeto 56, omejeno 8..512; `getDistanceSq` do `renderViewEntity` kot K5; histereza 4 bloke, `ImpostorL2`), dobi preklican `RenderLivingEvent.Pre` (prioriteta `LOWEST`, brez popravka rendererja CNPC; senca ostane vanilla) in se nariše kot impostor v istem `BufferBuilder` kot L3 (D-047) z interpoliranim položajem in yaw telesa. Izjeme (ostane model): animacija ≠ 0, neviden (`display.visible 1`, `isInvisible`), ubit, jaha ali je jahan, videz še ni pečen. (2) **Videz na klientu** iz prave entitete: `FarLook.describe` spawn NBT-ja (isti opis kot strežnik, D-045), največ 16 izračunov na sličico, znova po vsakem `readSpawnData` (`EntityNPCInterface.rwLookVersion`); pečen videz delijo L2 in L3. (3) Sledeni NPC, ki ga vanilla zaradi dometa upodobitve ne ponudi (`isInRangeToRender3d`, npr. brez hitboxa onkraj ~38 blokov pri RD 8), se nariše kot impostor do konca megle z lastnim frustumom, ne glede na prag. (4) Privzeto `RwFarView=0` do sprejema M5-V.8 (D-035) | Odločeno samostojno po pooblastilu lastnika (seja 110). (1) Raziskava M5-V.0 §5.6: preklic dogodka je edina pot brez posega v `RenderNPCInterface`; `LOWEST` pusti drugim modom (npr. `customNPC_entities_mod`) prednost. Histereza kot pri meji L3 prepreči utripanje modela/impostorja na pragu. (2) Strežnik id videza pošlje samo za NPC-je v pasu L3; za sledene ga klient izračuna sam iz istih podatkov, kar ne potrebuje novega sporočila in da isti hash (kloni → 1 peka). Proračun 16/sličico omeji zastoj ob vstopu v svet. (3) Zahteva iz 03-FAZE (NPC-ji brez hitboxa danes izginejo na 38 blokih). (4) L2 spremeni videz (brez animacije udov, oči, imena), zato po D-032 ni »dokazano enako«. Meritev ([zapis](meritve/2026-10-21-M5-V.6-L2.md)): A/B 3 + 3, prizorišče `-Dalec 1`, RD 10: idle-200 p50 4,886 → **2,013 ms (−58,8 %)**, idle-500 11,591 → **2,331 ms (−79,9 %)**, p95/p99 −56…−79 %, razponi ločeni; K16 L2 137/200 in 437/500 = geometrija. Veja izven dometa vanille v svetu ni izmerjena (števec 0; M5-V.7/V.8) |
| D-049 | 2026-10-21 | **`RwHeadwearSkip` (M5-K K14b) je privzeto 1.** Pokrivalo (`HeadWearType=1`) ne riše štirikotnikov, ki vzorčijo samo teksle z alfo 0. To ni *dokazano* enako (fragment točno na robni črti štirikotnika lahko v originalu vzame sosednji neprozoren teksel), zato D-032 sam ne zadošča in je vklop izjema od D-007 po odločitvi lastnika, kot D-037. Stikalo ostane (0 = original); varovala ostanejo nespremenjena — original se uporabi z OptiFine, za teksture, ki niso natanko `SimpleTexture` (URL skini, skini igralcev), vire z `.mcmeta`, otroke, ki niso `Model2DRenderer`, in kadar alfa test ni vklopljen. Obstoječi configi obdržijo svojo vrednost (tudi `dev/run` z izrecnim `RwHeadwearSkip=0`, zato scenariji in baseline merijo original kot doslej); nova namestitev dobi 1 | Odgovor lastnika na **Q30** (21. 10. 2026), dobesedno: »Ja, Q30: K14b (RwHeadwearSkip=1) naj bo privzeto vklopljen. Zapisi odlocitev.« Dokaz ostane meritev 19. 10. ([zapis](meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md)), novih meritev ni: A/B 3 + 3, idle-500 p50 11,493 → 8,595 ms (−25 %), idle-200 6,521 → 5,317 (−18 %), razponi ločeni; preverba pikslov `HeadwearCheck` 7 serij × 200 NPC-jev (skin testnega sveta, vzorec, 5 pravih skinov CNPC) 0 razlik barve in globine, občutljivost 8.656 pikslov. Morebitna razlika je posamezen piksel na robu klobuka; D-039 je K14b že označil za boljšega kandidata za privzeto pot kot L1 |
| D-050 | 2026-10-22 | **Združljivost izrisa L2/L3 M5-V.7 (klient).** (1) **OptiFine senčila**: `FarCompat` prek refleksije bere `Shaders.shaderPackLoaded` in `isShadowPass`; z aktivnim paketom senčil in `RwFarShaders=0` (privzeto) klient riše original (brez preklica L2, impostorjev L3, skrivanja mrtvih in peke), z `RwFarShaders=1` impostorje riše tudi s senčili; v senčnem prehodu nikoli impostorjev (NPC v L2 se v senčni mapi ne nariše). OptiFine z neberljivimi polji = senčila aktivna. (2) **URL skin / skin igralca**: peka čaka, dokler se tekstura prenaša (`SkinDownload`: slike ni, nit prenosa živi; največ 30 s), nato speče, kar ima renderer. (3) **Pol-viden NPC** (`NpcVisible 2`) je v L2 pol-prozoren impostor: `FarRender.l2()` ne kliče `isInvisible()` (CNPC ga vrne za vsak `NpcVisible != 0`) — ista latentna napaka je bila v M5-V.6 (pol-vidni NPC-ji so v L2 ostali model). Neviden (1), ubit, jahajoč/jahan ostanejo izven L2. (4) Entity Culling in `customNPC_entities_mod` brez stikala (skladna). (5) Privzeto `RwFarView=0` ostane (D-048) | Odločeno samostojno po pooblastilu lastnika (seja 112). (1) Statična analiza OptiFine 1.12.2 HD U G5: sidro `"blockentities"` ostane enkrat, a tik pred njim `Shaders.endEntities`/`beginBlockEntities` (impostor bi šel skozi program bločnih entitet) in senčni prehod pokliče `renderEntities` drugič; videz pod senčili ni preverljiv brez OptiFine v razvojnem okolju, zato varna privzeta pot = original (raziskava M5-V.0 §7 je predvidela stikalo). (2) Prej je peka po postavitvi `textureLocation` trajno spekla privzeti skin, ker `RenderNPCInterface` lokacijo postavi ob začetku prenosa. (3) Odkrito z merilom K17c (prvi zagon padel). (4) Entity Culling (RenderLib 1.4.5) prekliče `Render.shouldRender` zakritim → `RenderLivingEvent.Pre` se ne sproži, kar je pravilno; veja izven dometa vanille ima lasten frustum. Entitete `customNPC_entities_mod` dedujejo `EntityCustomNpc`, peka ustvari pravi razred prek `EntityList` → njihov renderer. Meritev ([zapis](meritve/2026-10-21-M5-V.7-zdruzljivost.md)): `v7-posebni` K17a–j zeleno (velik, pol-viden, neviden, brez hitboxa izven dometa vanille, jahač/nosilec, ubit, morf, URL skin s 4 s zamika), regresija L3 V0–V24 zelena; OptiFine, Entity Culling in `customNPC_entities_mod` v svetu niso izmerjeni (niso nameščeni v razvojnem klientu) — statično [K] |
| D-051 | 2026-10-22 | **Sprejem M5-V.8: vidnost na daljavo je sprejeta, `RwFarView` postane privzeto 2.** (1) A/B celic M5-V.1 (domet 64, vd 10/24 × 1.000/5.000) z `RwFarView` 0/1/2, 3 ponovitve, vseh 36 zagonov veljavnih: pri vd 24 / 5.000 sličica p95 9,188 ms (0) → **8,893 ms (2, −0,295, razpona se prekrivata)**, 10,873 ms (1, +1,685, ločeno); `rwfar` 91,5 µs/tick/igralca (≤ 100); `rwfar` 0 B/s v mirovanju, 513 B/s med hojo, žica nespremenjena; VRAM ≈ 21,6 MiB [K]; meja 64 brez vrzeli in dvojnih; sonda na 368 blokih vidna. Cilji D-034/D-035 so izpolnjeni pri **2**. (2) Privzeto `RwFarView=2` (en ključ za obe strani: strežnik pošilja pri ≥ 1, klient riše L2 + L3); obstoječi configi obdržijo vrednost, `dev/run` ostane 0. Sprememba v kodi je ločen paket **M5-V.8b** (komentar konfiguracije, unit/build/package), ker bi sprememba kode razveljavila odtis sprejemnih meritev. (3) Vrednost 1 na klientu (L3 brez L2) cilja sličice ne izpolni in ni priporočena; zapisano v komentarju konfiguracije | Odločeno samostojno po pooblastilu lastnika (seja 113) po D-035 (1): vklop je vezan na izpolnjene cilje, ti so izpolnjeni pri privzeti klientski vrednosti 2. Vprašanje, ali neizpolnjen cilj pri 1 blokira vklop: ne — D-035 za klient predpisuje 2, vrednost 1 je strežniška/izbirna; slabost je zapisana. Omejitev: dobiček L2 je odvisen od prizorišča (70 od 170 sledenih onkraj 56); na prizorišču brez sledenih onkraj 56 bi L3 dodal ~1,7 ms → kandidat optimizacije izrisa L3 v naslednjih K-paketih. [Zapis](meritve/2026-10-22-M5-V.8-sprejem.md) |
| D-052 | 2026-10-22 | **`RwRenderBox` (M5-K K12) je privzeto 1 — izjema od D-007.** Okvir upodobitve NPC-ja (`getRenderBoundingBox`, ki ga uporabljata frustum vanille in okluzija Entity Culling prek RenderLib `IBoundingBoxCache`) je unija hitboxa in okvira modela (roke, predmet v roki, ležeče telo, skale delov, morf); domet upodobitve stoječega NPC-ja s hitboxom ostane original, **NPC brez hitboxa** in **ležeč/ubit NPC (ne morf)** pa dobita domet stoječega NPC-ja s hitboxom (Size 5: 64 namesto 38,4 oz. 42,7 bloka pri RD 8). Sprememba obnašanja: na robu zaslona in za ovirami (Entity Culling) se nariše več NPC-jev, NPC brez hitboxa in ležeč NPC ne izgineta prej kot stoječ; originalno narisan NPC ni nikoli izločen (okvir ⊇ hitbox, domet ≥ original). **Lastne okluzije ni**: Entity Culling bere naš okvir. 0 = original | Odločeno samostojno po pooblastilu lastnika (Q33, seja 115). D-032 ne velja (izid ni enak), D-007 bi pustil vidno napako (izginjanje rok/meča na robu, NPC brez hitboxa pri 38 blokih). Dokaz: K18 (`k12-okvir-on/off`) — obris modela sega do 0,75 bloka čez hitbox, z okvirjem največ 0,09 (Size 15); A/B idle-500 3 + 3 v šumu (p95 12,849 → 12,907 ms, razpona se prekrivata), torej brez izmerjene cene. Entity Culling in OptiFine v svetu nista izmerjena (nista nameščena) [K]. [Zapis](meritve/2026-10-22-M5-K-K12-okvir.md) |
| D-053 | 2026-10-22 | **Ponovna ocena K13 in K10 po M5-V.8 (D-035 (3)): K13 zavržen kot samostojen paket, K10 odložen izven M5 v zalogo, K2 in K11 zavržena; naslednja paketa sta čistilni korak K3 in M5-K.2 (sprejem klienta, izhodni kriterij M5).** (1) K13: vezava teksture v profilu M5-K.1 ni merljiva skupina, ena menjava je en GL klic proti ~30 µs izrisa NPC-ja → zgornja meja ~1–2 %, pod šumom; L2/L3 imata lasten atlas. Ostane le kot podkorak K10. (2) K10: premisa »nič klicev na NPC« je z K14 in K3 delno ovržena (manj klicev pri isti geometriji ne pomaga, strošek je sorazmeren ogliščem v gonilniku); po D-051 so modeli samo znotraj 56 blokov, zato K10 pospeši samo bližnjo gnečo; zahteva lasten GLSL za fiksno pot in skoraj zagotovo ni piksel-enak (D-032 → ostal bi izklopljen); izhodni kriterij M5 ga ne zahteva. Ponovno odprtje: M5-K.2 ali poročilo pokaže ≥ 300 modelov znotraj 56 blokov s p95 > 16,7 ms ob privzetih stikalih; prvi korak tedaj poskus K10a (deli modela iz VBO, fiksna pot). (3) K1, K6–K9 odloženi do prizorišča, ki jih vsebuje; cena izrisa L3 (~0,26 µs/impostor) zavržena kot paket; pas `rwfar` po chunkih in PBO peka odložena s pogojem (meja 100 µs presežena / zatikanje izmerjeno) | Odločeno samostojno po pooblastilu lastnika (seja 116) iz obstoječih meritev, brez novih zagonov. [Raziskava](raziskave/M5-K-K13-K10-ponovna-ocena.md). Protokol §2: kandidat, ki ne preseže šuma, se zavrže; L-paket z visokim tveganjem in dobitkom samo v redki bližnji gneči ni upravičen pred zaključkom M5 in M4 |
| D-054 | 2026-10-22 | **M5-K.2 (sprejem klienta) je zaključen: klientski del izhodnega kriterija M5 (D-033) je izpolnjen; M5 kot celota se zapre šele po regresijskem paketu M5.Z.** A/B 3 + 3 original (vsa klientska stikala 0) proti vsem sprejetim privzetim stikalom (`RwHeadwearSkip` 1, `RwLabelCache` 1, `RwFarView` 2 z `RwImpostorStart` 56, `RwRenderBox` 1) v prizorišču M5-K.1 (RD 8) in M5-V.6 (`-Dalec 1`, RD 10): p95 idle-200 7,095 → 5,356 ms (−24,5 %), idle-500 12,825 → 9,817 (−23,5 %), dalec-200 5,405 → 1,851 (−65,8 %), dalec-500 13,017 → 2,942 (−77,4 %); 1 % low 102,5 → 139,8, 54,4 → 67,0, 134,9 → 364,6, 50,2 → 259,6 FPS; p50 in p99 enako, vsi razponi ločeni, regresije na privzeti strani ni. Pogoj ponovnega odprtja K10 (D-053) ni izpolnjen: 500 modelov znotraj ~42 blokov ima p95 9,817 ms (< 16,7). Ostali točki izhodnega kriterija M5 (funkcionalna matrika `PLAN_IMPLEMENTACIJE.md` §6, timerji in script trace proti referenci) od M5 nista bili ponovljeni na kodi s privzetimi stikali → nov paket **M5.Z** (prehod `matrika-run.ps1` in primerjava M2.7 na trenutni kodi) | Odločeno samostojno po pooblastilu lastnika (Q34, seja 118). Izhodni kriterij M5 je konjunkcija; od zadnjega prehoda matrike (17. 9.) so privzeto vklopljena stikala M5-S in M5-K (D-032, D-037, D-049, D-051, D-052), zato zaprtje M5 brez regresijskega prehoda ne bi bilo dokazano. Dokaz: 8 serij × 3 na `332af8c4…`, T1–T6 in K1–K16 zeleni v vseh 24 zagonih. [Zapis](meritve/2026-10-22-M5-K.2-sprejem-klienta.md) |
| D-055 | 2026-10-22 | **M5.Z zaključen, M5 zaprt v načrtovanem obsegu regresijskih dokazov (Q35, Q36).** Q35: diagnostična JVM lastnost `rwprivzeto` (Gradle `rwPrivzeto`): 0 samo izpiše config, 1 po `loadConfig()` obnovi pred nalaganjem zajete vrednosti statičnih `@ConfigProp Rw*`; brez lastnosti vrednosti ostanejo iz configa. Datoteke configa in merilni pogoji ostanejo nespremenjeni; 23 polj, 12 razlik. Matrika preveri način v vsakem svežem logu. Q36: M2.7 meri navigacijo in nima timer sledi, zato napačno oznako v M5.Z nadomesti nov scenarij `sled-run.ps1`: referenca je isti popravljeni jar s stikali iz configa (0), kandidat s privzetimi vrednostmi kode. Nespremenjeni binar se preverja ločeno s `testOriginal`. Ohranjen B3 (razmik timerja 7 po restartu 8 tickov v obeh krakih), brez popravka izven obsega. Naslednji paket M4.14b → M4.16 po D-038 | Samostojno po pooblastilu lastnika (seja 119). Vseh šest zahtevanih testov zelenih na `c3e19fe4…`: unit, build, package (71 vnosov), sled in obe matriki (5/5 stopenj vsaka). Sled 97/75 dogodkov ter 19 razmikov po 8 tickov enaki; optimizirana collide pot potrjena (60 preskokov). Prvi neuspeh v obeh krakih je bil absolutna starost po restartu; faza 2 sedaj meri od init. Brez regresije v izvedeni pokritosti; nepokrite vrstice M0.7, Q1/Q22 in nameščeni render dodatki ostajajo za M10 oziroma ločene preverbe. [Zapis](meritve/2026-10-22-M5.Z-regresija.md) |
| D-056 | 2026-10-22 | **M4.14b: prva preverba brez spreminjanja produkcijskega planerja ali sprejemnih pragov (Q37/Q38).** Q37: izolirana osnovna kopija sveta in dva kraka prek `-PrwWorld`, oba s privzetimi stikali kode (`-PrwPrivzeto=1`); config in uporabnikov svet ostaneta ista, klici navigacije se razlikujejo samo v fazi B. Q38: ohranjena prvotna merila FA1–FA6, dodatna meritev unikatnih zasedenih mest (<0,4 bloka), ker oddaljenost <2 od skupnega cilja ne zajame zunanjih stolpcev. Razpon ≤6 se še vedno preverja, čeprav je idealni premer legije 4×2 že 6,325. FA5 šteje začetno iskanje (+1 k obstoječemu števcu), ne identitet `Path`. Noben sklep o sprejemu pred controller rezultati in dimnim delom F1–F12 | Samostojno po pooblastilu lastnika, seji 120/121. **Q39:** prvi A/B pade pred meritvijo na deljenju stdout datoteke; popravimo samo bralnik (`FileShare.ReadWrite`), dodamo regresijsko pogodbo z odprtim zapisovalnikom in zahtevamo sveže controller rezultate. To ni dokaz napake formacij. Stara merila in geometrija niso skladni; to je razlog za dvojni izpis in razlago neuspehov, ne za prikaz lažnega uspeha. Izoliran `formacije-run.ps1` omogoča prvo reprodukcijo brez posega v stare merilne vhode; pogodbo preverja ločen samotest. [Priprava, brez meritev](meritve/2026-10-22-M4.14b-formacije.md) |
| D-057 | 2026-10-22 | **M4.14b po prvem izmerjenem neuspehu: reprodukcija pred popravkom (Q40).** Ohranimo vse pragove, geometrijo in merilne vhode; FA1/2 in FA4 ostanejo neuspešni tudi ob znanem neskladju s širino legije. Ne ponavljamo istega A/B brez novega pristopa. Najprej controller `unit` z dvema novima reprodukcijama: nova pridobitev mesta ob prehodu MARCHING → SETTLING in smer kroga ob neposrednem zaključku DONE. Šele nato ciljan popravek, nova celotna preverba in dimni del F1–F12. Nobene spremembe produkcijskega planerja v tej seji | Samostojno po pooblastilu lastnika (seja 122). Na `360f3f60…` harness/unit/build/package uspejo; A/B ne: G 0/8, obstane 7,2 bloka pred koncem poti, 117 iskanj proti meji 36; O doseže 8 mest pri ticku 40, nato napaka naraste na 0,555 bloka. Ti podatki ne dokazujejo še vzroka. Novi reprodukciji sta pripravljeni, ne izvedeni; pričakovani rdeči rezultat ni sprejem paketa. [Izmerjeni neuspeh in naslednja preverba](meritve/2026-10-22-M4.14b-formacije.md) |
| D-058 | 2026-10-22 | **M4.14b: po potrjeni reprodukciji omejimo popravek na dokazani napaki faz (Q41).** `enterSettling` počisti `settled`, da se mora član na novem končnem mestu ponovno ustaviti znotraj 0,4 bloka; DONE ob prehodu objavi HOLD in pravilni `faceYaw` tudi za krog, preden ga adapter uporabi za sidranje. Vsi pragovi, geometrija, hitrosti, adapter in merilni vhodi ostanejo isti. Zahtevamo sveže harness/unit/build/package/A/B; zastoja pri vratih in drsenja po zaključku ne razglasimo za odpravljena brez meritve | Samostojno po pooblastilu lastnika (seja 123). Controller `unit` na `dcbd1219…` ob 13:41 UTC izvede 402 primera, od tega padeta natanko `settlingRequiresNewArrivalWhenTheFinalSlotChanges` (prezgodnji DONE) in `completionPublishesOutwardFacingForEveryRingMember` (manjkajoči yaw); ostalih 400 uspe. Popravek še nima controller rezultatov; F1–F12 in celotni sprejem ostanejo odprti. [Dokaz in nadaljevanje](meritve/2026-10-22-M4.14b-formacije.md) |
| D-059 | 2026-10-22 | **M4.14b: po zelenih reprodukcijah in neuspešnem A/B ločena bralna sled adapterja (Q42).** Pred nadaljnjim posegom v produkcijo `formacije-run.ps1 -Trace` v krak FORMACIJA vgradi ločen overlay, ki beleži položaje, motion, gibalni vhod/helper, planerjeve cilje/zastavice in navigator. Reference ohrani po DONE. Poseben prefix `m414b-trace-*`, pokritost 16 × 104 vzorcev; vsi stari FA preizkusi ostanejo aktivni. Dodatno zapisovanje pomeni diagnostični, ne sprejemni/performance zagon; po ciljanem popravku ostane obvezen običajni A/B. Timer samo na krmilniku uporablja interval 0 zaradi originalnega B3; produkcijski planer/adapter, geometrija, hitrosti in pragovi nespremenjeni | Samostojno po pooblastilu lastnika (seja 124). Na `a99e030c…` harness/unit/build/package uspejo, obe novi reprodukciji zeleni, vendar G ostane 7,2 bloka pred koncem s 116 pravimi iskanji proti meji 35; O DONE pri 38, nato napaka 0,555. To ne dokazuje vzroka. Pred novim popravkom opazujemo predtek članov kolone in preostalo gibanje po čiščenju poti. Zahtevan novi `m414b-trace-v1` in razširjeni samotest, še ne izvedena; brez trditve o odpravi ali sprejemu. [Dokazi in načrt](meritve/2026-10-22-M4.14b-formacije.md) |
| D-060 | 2026-10-22 | **M4.14b: sled prevedemo v reprodukcije dejanskega adapterja pred popravkom (Q43).** Po popolni sledi zahtevamo controller `unit` s šestimi `SquadAdapterTest` primeri: HOLD in finish ustavita horizontalno vztrajnost/vhod/helper brez teleportacije ali spremembe gravitacije; STEER ne dodaja predteka točki kolone ali mirujočega sidra. Kontroli ohranita predtek pri premikajočem se sidru in odsotnost predteka v SETTLING. Test uporablja pravo Squad; navigator samo beleži setPath, brez sveta. Produkcije v tej seji ne spreminjamo. Po vsebinski reprodukciji ciljan popravek in svež običajni sprejem, vključno s F1–F12 | Samostojno po pooblastilu lastnika (seja 125). Na `c6d7fe1c…` controller harness uspe, diagnostična sled zajame 1.664 veljavnih zapisov, FA še padejo. O/0 po odstranitvi poti brez vhoda drsi z motionZ 0,146174 in trenjem 0,546; G dobi predtek proti zidu tudi ob cilju za članom in hitrosti sidra 0. To ne dokazuje vseh vzrokov G ali uspeha popravka. Štiri vsebinske reprodukcije naj bi padle, dve kontroli uspeli; še neizvedeno. [Analiza in načrt](meritve/2026-10-22-M4.14b-formacije.md) |
| D-061 | 2026-10-22 | **M4.14b: po potrjeni reprodukciji omejimo popravek na zaustavitev in predtek adapterja (Q44).** HOLD in upravičen član ob finish počistita pot, helper postavita na WAIT ter ustavita AI hitrost, vhod naprej/stran in motionX/Z; položaj, gravitacija in motionY ostanejo isti. LEAD=2 velja samo v MARCHING pri premikajočem sidru za običajno mesto; kolona in mirujoče sidro dobita cilj brez predteka. Šest testov adapterja, geometrija, merilni vhodi in sprejemni pragovi ostanejo isti. Pred zaključkom zahtevamo sveže harness/unit/build/package, običajni A/B in diagnostično sled ter F1–F12 | Samostojno po pooblastilu lastnika (seja 126). Controller unit na `fba333de…` ob 14:23:36 UTC izvede 408 primerov, natanko štiri nove vsebinske reprodukcije padejo, obe kontroli uspeta; brez napake fixture. Cilji 71/72 namesto 69/70 ter motionX 0,012 in motionZ 0,146174 namesto 0 potrdijo obe poti adapterja. Popravek še ni preverjen in ne dokazuje prehoda vrat ali FA5. [Izidi in obseg popravka](meritve/2026-10-22-M4.14b-formacije.md) |
| D-062 | 2026-10-22 | **M4.14b: po preverjenem popravku adapterja najprej manjkajoča sled iste kode (Q45).** Na `9c3650a7…` harness/unit/build/package in vseh šest testov adapterja uspejo; običajni A/B potrdi stabilnih 8/8 mest G/O in DONE (234/40 tickov), vendar FA1/2, FA4 in FA5 G ostanejo neuspešni. Pred novim posegom zahtevamo `m414b-trace-v1` na istem odtisu za članski kontekst preostalih zahtev PATH in preklopov STEER/PATH. Produkcije, števca in sprejemnih meril ne spreminjamo; že zelenih testov brez novih vhodov ne ponavljamo. Nato vsebinske reprodukcije pred ciljnim popravkom in ločen dimni del F1–F12 | Samostojno po pooblastilu lastnika (seja 127). G doseže 8 mest pri vzorcu 240, pri 560 napaka 0,384; O od 40 do 560 napaka 0,240. Premer MARCHING 6,540/6,428 >6, FA5 G 57 >5. Števec je število zahtev adapterja, sam zase ne dokazuje toliko izvedb A*: vanilla lahko vrne trenutno pot, S5 zahtevo preskoči. To je razlog za diagnozo, ne za znižanje števca ali opustitev neuspelega testa. V tej seji samo dokumentacija; paket ostane odprt, brez commita/pusha pred sprejemom. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md) |
| D-063 | 2026-10-22 | **M4.14b: pred popravkom življenjske dobe poti dve reprodukciji in štiri kontrole (Q46).** Popolna sled na `9c3650a7…` pokaže periodično zahtevo ob aktivni poti/istem cilju ter prekinitev večtočkovnega obvoza ob PATH → STEER. `SquadAdapterTest` zahteva ohranitev aktivne poti brez periodične zahteve, zadržanje obvoza k istemu cilju tudi ob STEER ter ohranitev korekcije po zaključku, novega cilja in ponovitve po izgubi ali neuspehu poti. Najprej controller unit z dvema pričakovanima vsebinskima neuspehoma; produkcija, števec, harness in pogoji ostanejo isti. Po potrjeni reprodukciji ciljan popravek in svež sprejem | Samostojno po pooblastilu lastnika (seja 128). Controller sled ob 14:42:50 UTC: 1.664/1.664 veljavnih zapisov, G/O DONE in stabilnih 8/8 mest, FA5 G 55 proti meji 5; FA1/2 in FA4 še padeta. G/7 pri 51 znova zahteva pot s ciljem −9,5/65,661600 in indeksom 1/10; G/2 med 120 in 140 ob istem cilju preklopi z 9 na 1 točko. To ne dokazuje toliko izvedb A* ali odprave FA5. Kontrole preprečijo slepo zatiranje vseh zahtev. Preverba fizike/vrat in F1–F12 ostane zahtevana; brez spremembe hitrosti ali pragov. [Dokazi in pogodbe](meritve/2026-10-22-M4.14b-formacije.md) |
| D-064 | 2026-10-22 | **M4.14b: adapter ohrani nedokončan obvoz zadnjega iskanja (Q47).** Unit na `63f20805…` potrdi obe reprodukciji D-063 (`activeRecoveryDoesNotRepeatRequestJustBecauseIntervalElapsed`, `steerNearTargetKeepsUnfinishedRecoveryDetour`), štiri kontrole uspejo; 414 testov, 2 neuspešna. `Squad` si zapomni pot zadnjega `tryMoveToXYZ` člana. Dokler je ista pot v navigatorju, ni končana in se cilj od iskanja ni premaknil za več kot 3 bloke, PATH ne sproži periodične zahteve, STEER pa obvoza ne zamenja z enotočkovno potjo (samo posodobi hitrost). Nova zahteva ostane ob `noPath` (izgubljena/končana/očiščena pot), premiku cilja >3 ali neuspehu po cooldownu | Samostojno po pooblastilu lastnika (seja 129). Prag 3 bloke je isti kot obstoječi `moved > 3.0` za novo zahtevo, zato ni novega parametra. Obtičanje obravnava vanilla `checkForStuck`, ki pot pobriše → `noPath`. Števca, hitrosti, pragov FA, harnessa in pogojev ne spreminjamo. Učinek na FA5/FA1/2/FA4 ni dokazan, dokler ni svežih controller testov; F1–F12 ostanejo odprti. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md) |
| D-065 | 2026-10-22 | **M4.14b: uspešno končano iskanje se ne ponavlja; ostanek prevozi STEER (Q48).** Na `637ad98f…` (D-064) so harness/unit/build/package zeleni, A/B pade samo še na FA1/2 (skupni cilj), FA4 6,458/6,403 in FA5 G 36 proti meji 5; G 21 od 35 zahtev v oknu 200–220 ob prehodu v SETTLING pri štirih upravičenih članih. V PATH je `nav.noPath()` resničen tudi za **končano** pot, zato član v kazni obtičanja (`pathUntil`, 60 tickov) zahteva isto točko vsakih 5 tickov. Popravek: če je pot zadnjega iskanja prehojena, se konča ≤ `ARRIVED` = 1,5 bloka od cilja, cilj ni premaknjen >3 in je član v dosegu `STEER_MAX`, adapter postavi enotočkovno pot brez iskanja. Takojšnja ponovitev samo za opuščeno nedokončano pot; neuspešno ali delno iskanje počaka `PATH_INTERVAL` | Samostojno po pooblastilu lastnika (seja 130). Dva nova testa adapterja (`completedRecoveryAtTargetSteersInsteadOfRepeatingSearch`, `completedPartialRecoveryRetriesOnlyAfterInterval`) sta po statični analizi [K] na `637ad98f…` rdeča (pogoj `noPath && ticks-last>=5`), controller pa ju izvede samo na kodi s popravkom: odstopanje od reda D-063 zavestno, ker sprožilec iz kode ni dvoumen in vsak cikel stane celoten prehod. Novi parameter `ARRIVED` (1,5 < `GOAL_REACHED` 2). Pragovi FA, števec, hitrosti, geometrija in merilni vhodi isti. FA1/2 in FA4 sta neskladni z geometrijo legije 4×2 (D-056) in ostaneta zapisana neuspeha; F1–F12 odprti. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md) |
| D-066 | 2026-10-22 | **M4.14b: razlogi članskih iskanj v `RWSQUAD`, brez spremembe obnašanja (Q49).** Na `31c1fb2b…` (D-065) A/B: FA5 G 32 (prej 36) proti meji 5, 17 zahtev v oknu 200–220 ob prehodu v SETTLING. Hipoteza: člani s kaznijo `pathUntil` ponovno iščejo ob premiku cilja >3 bloke, ob SETTLING pa vsak član v PATH še enkrat. Pred posegom v adapter `Squad` šteje vsako člansko zahtevo v en razlog (`iskanj_rok`/`iskanj_premik`/`iskanj_izgub`) ter `iskanj_postav` (SETTLING) in `iskanj_dalec` (napaka > `STEER_MAX`). Števec `iskanj`, merila FA in pragovi nespremenjeni. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md) |

**D-066/Q49, nadaljevanje (seja 132):** na `44f3390b…` harness/unit/build/package uspejo;
A/B `audit/m414b-2026-10-22-205327` izmeri 31 članskih zahtev G: rok 1, premik 20,
opuščena pot 10, od tega 8 v SETTLING. FA5 G z začetnim iskanjem 32 > 6; FA1/2 in FA4
še neuspešni. Agregat ne pove, kdo je pot zamenjal, zato pred popravkom zahtevamo samo
manjkajoči `m414b-trace-v1` na isti kodi. Že zelenih testov brez novih vhodov ne ponavljamo;
produkcija, števec in merila nespremenjeni. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

**Izid D-022 (28. 9. 2026).** [M7.8 A/B](meritve/2026-09-28-M7.8-zakljucek.md) je dal 8/8 celih poti pri obeh ozadjih, zato vrata V4 niso prestana. Globalni `RwNavBackend` ostane privzeto 0; Baritone se ne sprejme kot CNPC privzeto ozadje. R1 je dodatno pokazal, da adapter na nosilcu z jahačem obstane, ker `MountGuard` ne obnovi njegovega zasebnega stanja; zato sta iz Baritona izključena tako jahač kot nosilec s potnikom.

**Popravek vrat V4 (28. 9. 2026, knjižnica D-043).** Veličina 1 je bila pri vanilli že 8/8, zato »izboljša veličine 1–3« ni bilo izpolnljivo. Od zdaj: veličina 1 ni slabša; veličini 2 in 3 imata boljšo mediano **in** Baritonov najslabši zagon ni slabši od vanille; Baritone najmanj 5 ponovitev. Veličini 5–6 štejeta šele, ko obe ozadji merita isti dogodek. Izid M7.8 se s tem ne spremeni (grlo B najslabši 4/8 proti 6/8). Vzrok nestabilnosti grla B išče M7.9 (sled knjižnice, `nav-run.ps1 -Sled`).

**Sprejem V4 (28. 9. 2026).** Po M7.9 (vzrok nestabilnosti grla je gneča) in M7.10c (knjižnica: čakanje v gneči, `crowdYield`) je Baritone v 5 zagonih prispel 8/8 na grlu A (vanilla 1/8), 8/8 na grlu B (vanilla 6/8) in 8/8 na odprtem, z razponom 0 ([meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md)). Uporabnik je V4 sprejel z znano omejitvijo: pri več NPC-jih na istem ciljnem bloku zadnji prispe 2–6 s kasneje. Baritone z `crowdYield` ostane **izbirno** ozadje; `RwNavBackend` in `crowdYield` sta privzeto izklopljena, brez njiju je obnašanje nespremenjeno.

### D-067 — M4.14b: po popolni sledi reprodukcija zaključka recovery (2026-10-22, Q50)

**Vprašanje:** ali po sledi na `44f3390b…` znova popravljati adapter ali najprej
preveriti, ali HOLD res zaključi planerjevo zahtevo PATH?

**Odločitev:** najprej controller `unit` s tremi novimi javno opazljivimi prehodi v
`SquadPlannerTest`: doseženo mesto HOLD → premik sidra zahteva STEER; nedoseženo mesto
ostane PATH; odriv nad STEER_MAX tudi po HOLD zahteva PATH. Fixture sproži obtičanje
z dejanskim planerjevim detektorjem, brez vpisovanja zasebnih polj. Pričakovan je en
vsebinski neuspeh in dve zeleni kontroli, kar še ni izvedeno. Produkcija je nespremenjena.

**Razlog:** controller sled `audit/m414b-trace-2026-10-22-210115` ima 1.664/1.664 zapisov
brez vrzeli; G/5 pri 180 drži mesto (HOLD, pot null), kazen PATH pa ostane do 211 in pri
200 znova vodi v PATH. Koda v veji HOLD ne zaključi `pathUntil`. To je ožji dokazljiv
problem od poskusa razložiti vseh 31 zahtev. Med 200–220 sled vzorči le robova, ne identitet
`recovery` in vzroka vsake zamenjave, zato 7 zahtev zaradi izgube ne pripišemo izključno
HOLD ali drugemu AI-ju. Po potrjeni reprodukciji ciljan popravek in sveži zahtevani testi;
FA merila, geometrija, hitrosti, merilni config in F1–F12 ostanejo nespremenjeni/odprti.
[Podrobni dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-068 — M4.14b: pridobljeni HOLD zaključi kazen PATH (2026-10-22, Q51)

**Vprašanje:** kaj po potrjeni reprodukciji D-067 izničiti ob pridobitvi mesta?

**Odločitev:** v veji HOLD planer nastavi samo `pathUntil[i] = 0`. `trailUntil`
ostane nespremenjen: član ohrani kolono skozi ozko grlo. Adapter še vedno ohrani
nedokončan aktivni obvoz; pogoj `e > STEER_MAX` še vedno zahteva PATH po večjem odrivu.
Ne spreminjamo harnessa, geometrije, hitrosti, FA pragov ali merilnega configa.

**Razlog:** controller unit na `cc58e6dc…` (operacija `c8f7c280…`, 19:10:58 UTC)
izvede 419 primerov, natanko eden pade: `acquiredRecoverySlotResumesWithSteerWhenAnchorMoves`
pričakuje STEER (1), dobi PATH (2). Vse predhodne trditve fixture uspejo, kontroli
`unrecoveredBlockedMemberStillNeedsPath` in `acquiredSlotStillNeedsPathAfterDistantDisplacement`
prav tako. To potrdi ozek vzrok, ne celotnega FA5. Tri reprodukcije ostanejo nespremenjene.
Popravek zahteva sveže controller rezultate za vseh šest sprejemnih ID-jev; dosedanji
neuspehi FA1/2/4/5 in odprti F1–F12 niso odpravljeni ali oproščeni s to odločitvijo.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-069 — M4.14b: po D-068 zahtevamo manjkajočo sled iste kode (2026-10-22, Q52)

**Vprašanje:** ali po zelenem unit in neuspešnem A/B znova spreminjati produkcijo ali
najprej dokončati že zahtevano diagnostiko?

**Odločitev:** samo `m414b-trace-v1` na nespremenjenih izvornih/testnih vhodih
`e225e880…`. Že zeleni harness/unit/build/package in običajni A/B se ne ponavljajo.
Sled mora ohraniti vseh 1.664 zapisov, prvotna FA merila in prehod HOLD → premik
sidra. V tej seji spreminjamo samo dokumentacijo; ne zatiramo zahtev PATH brez dokaza.

**Razlog:** controller na tem odtisu potrdi reprodukcijo D-067 po popravku in obe
kontroli. Običajni A/B `audit/m414b-2026-10-22-211704` pa še pade: G/O DONE 244/40,
8/8 končnih mest, FA5 G 30 >5, FA4 6,540/6,416 >6 ter FA1/2 neuspešni.
Od 29 članskih zahtev G jih je 19 zaradi premika cilja in 10 zaradi opuščene poti;
8 v SETTLING, 6 nad STEER_MAX. Agregati ne dokazujejo trenutka ali povzročitelja
vsake izgube. Manjkajoča sled je nov diagnostični dokaz, ne ponavljanje blokiranega
paketa ali sprejem. F1–F12 in celotni M4.14b ostanejo odprti.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-070 — M4.14b: gosta diagnostika identitet obvoza pred novim popravkom (2026-10-22, Q53)

**Vprašanje:** ali popolna sled D-069 pojasni preostalih 19 premikov in 10 izgub poti
ali pred posegom potrebujemo opazovanje med 20-tickovnimi vzorci?

**Odločitev:** nov ločen `m414b-recovery-trace-v2` (`-TraceRecovery`) in pripadajoči
`m414b-recovery-harness-v2`; produkcija ostane. Diagnostika mora dati 8.960 zapisov,
vsak član vsak tick do 560, ter ločeni stabilni identiteti trenutne poti in shranjenega
obvoza, njegovo napredovanje/končanost, zadnji iskalni tick/cilj in razloge zahtev.
Običajni A/B in v1 ohranita svoje pogoje; vsi FA pragovi ostanejo aktivni. Novih rezultatov
še ni. Pred popravkom se iz novih podatkov izbere ciljno reprodukcijo; sledi sama ne
uporabljamo kot performance ali sprejemni dokaz.

**Razlog:** controller na e225e880… potrdi 1.664/1.664 zapisov brez vrzeli in G/5
HOLD s `pathUntil=0` pri 180 → STEER pri 200. FA5 G pa ostane 30 >5. Med 200 in 220
naraste 13 → 29 članskih zahtev; obstoječa sled vmes ne vzorči in ne beleži identitete
`recovery`. Njene izgube zato ni mogoče pripisati zgolj vanilli, drugemu AI-ju ali HOLD.
Zbiralnik prek lastne `IdentityHashMap` le bere objekte, ne spreminja poti ali vhodov.
S tem dobimo nov dokaz namesto ponovitve istega neuspešnega testa ali špekulativnega
zatiranja PATH. F1–F12 in M4.14b ostanejo odprti; po spremembah harnessa so novi
diagnostični testi še zahtevani, stari odtis jih ne potrjuje.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-071 — M4.14b: reprodukcija prekinitve obvoza ob novem STEER cilju (2026-10-22, Q54)

**Vprašanje:** kateri naslednji poseg podpirajo podatki popolne recovery sledi, ne da bi
ugibali o vseh desetih izgubah poti ali sprostili FA5?

**Odločitev:** najprej controller `unit` z novo reprodukcijo
`changedSteerTargetDoesNotCutAcrossUnfinishedRecovery` in kontrolo
`changedSteerTargetDoesNotResurrectClearedRecovery`. Produkcijska koda v tej seji
ostane ista. Pogodba: STEER ohrani še aktiven nedokončan obvoz tudi ob premiku cilja;
spremenjeni PATH sme izrecno zahtevati zamenjavo (obstoječa kontrola), končane ali
počiščene poti se ne obuja. Popravek šele po izmerjenem neuspehu reprodukcije.

**Razlog:** na `1f792335…` harness uspe, recovery sled je popolna (8.960 zapisov),
vendar sprejem pade. G/7 pri 165 še hodi po recovery 180 (indeks 4/10), pri 166
je STEER z nav 387 dolžine 1, recovery pa še vedno 180/4/10. Cilj se ob izteku
kolone premakne prek zidu. `activeRecovery` vrne false zaradi premika nad 3 bloke,
veja STEER pa sama ustvari ravno pot. Pri 201 član stoji pred zidom, pri 202 PATH
zahteva novo iskanje, razvrščeno kot izguba stare poti. To je določen mehanizem,
ne razlaga vseh zahtev. G/4 pri 203 in G/7 pri 208 ločeno izgubita navigatorjevo
pot (`navId=0`); vzrok tega ostane nedokazan. Vsa FA merila in F1–F12 ostanejo odprta.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-072 — M4.14b: STEER ohrani aktiven obvoz tudi ob novem cilju (2026-10-22, Q55)

**Vprašanje:** kako po potrjeni reprodukciji D-071 preprečiti ravno prečkanje ovire,
ne da bi preprečili izrecno novo iskanje v PATH ali obujali neveljavne poti?

**Odločitev:** `activeRecovery` preveri samo identiteto poti v navigatorju in njeno
nedokončanost. STEER taki poti posodobi hitrost in je ne zamenja. V PATH obstoječi
pogoj premika cilja nad 3 bloke še vedno izrecno zahteva novo iskanje z istim
proračunom in cooldownom. Končane, počiščene ali zamenjane poti niso aktivni obvozi.
Reprodukcija in kontrole, harness, geometrija, hitrosti in FA pragovi ostanejo isti.

**Razlog:** controller `unit` na `59d50662…`, operacija `62d567fc-22b1-4c8d-b40f-018a1d663eb6`
(19:48:49 UTC), izvede 421 primerov z natanko enim vsebinskim neuspehom:
`changedSteerTargetDoesNotCutAcrossUnfinishedRecovery` pri trditvi o identiteti poti.
Kontrola za počiščeno pot in vseh 14 drugih adapterjevih testov uspejo, vključno z
izrecno zamenjavo v PATH. To podpira ozek popravek, ne trditve o odpravi vseh FA5
zahtev. Popravek še nima rezultatov; zahtevana je sveža controller validacija vseh
osmih ID-jev. F1–F12 in M4.14b ostanejo odprti, commit/push po zaključeni validaciji.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-073 — M4.14b: dokončanje diagnostike D-072 pred novim posegom (2026-10-22, Q56)

**Vprašanje:** ali po zelenem unit in še neuspešnem A/B spreminjati adapter ali
najprej dokončati manjkajoče diagnostične prehode na isti kodi?

**Odločitev:** zahtevamo samo še `m414b-trace-v1`, `m414b-recovery-harness-v2` in
`m414b-recovery-trace-v2`. Produkcija, testi, harness in vsi merilni pogoji ostanejo
isti; že zelenih testov in običajnega A/B ne ponavljamo. Sledi ohranita zahtevi
1.664/8.960 zapisov in vse FA preizkuse. Tudi popolna sled ni uspešen sprejem.

**Razlog:** na `c9edad1b…` uspejo harness/unit/build/package, vseh 16 adapterjevih
testov je zelenih. Običajni A/B pa pokaže G/O DONE 237/40 in 8/8 končnih mest,
FA5 G 29 >5 (20 premikov, 8 izgub) ter FA4 G 9,564 >6. Ohranjanje obvoza torej
še ni dokaz ohranitve skupine ali odprave vseh iskanj. Agregat ne pove, katere
poti se izgubijo ali predolgo ohranijo; nov poseg brez goste sledi bi bil ugibanje.
FA1/2, FA4, FA5 G in F1–F12 ostanejo odprti, brez spremembe pragov ali trditve
o zaključku paketa. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

**Nadaljevanje 22. 10., seja 140 (Q56):** controller v1 na istem c9edad1b…
(`d38f2386-f016-4036-a368-eb632ee521ce`, 20:08:04 UTC) da 1.664 popolnih zapisov,
a sprejem še pade (FA1/2/4, FA5 G 29 >5). Pri 200 je premer 9,563664 med G/1 in
G/7 že v MARCHING; pri 200–220 nastane še 14 zahtev. V1 nima identitet poti in
vmesnih tickov, zato ne dokazuje avtorjev izgub ali ohranitve istega obvoza G/7.
Odločitev ostaja: brez novega posega, zahtevana samo še recovery-harness-v2 in
recovery-trace-v2 na isti kodi z 8.960 zapisi in nespremenjenimi FA merili.
Že izvedenih testov ne ponavljamo. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-074 — M4.14b: ločena diagnostika vanilla izgube poti (2026-10-22, Q57)

**Vprašanje:** kaj po popolni recovery sledi še manjka za utemeljen popravek FA5?

**Odločitev:** nov, ločen `formacije-run.ps1 -TraceNavigator` in ID-ja
`m414b-navigator-harness-v3` / `m414b-navigator-trace-v3`. Bralni dodatek k recovery
overlayju beleži zasebno stanje `PathNavigate`: tickovno in stensko časovno omejitev,
zadnjo preverjeno pozicijo/vozlišče, odloženo obnovo in predpomnjeni cilj. Bralnik zahteva
8.960 popolnih zapisov in izpiše prehode aktivna pot → null z obema posnetkoma ter
podpisom izničenja treh timeout polj. To je podpis stanja, ne zajet klicni sklad.
Vsi FA preizkusi ostanejo; produkcijska koda, geometrija, hitrosti, okna in config isti.
Obstoječa načina trace/recovery dodatka ne vključita. Nova testa še nista izvedena.

**Razlog:** recovery na c9edad1b… je popoln (8.960), vendar FA5 G še 29 >5.
G/7 pri 165–166 ohrani isti obvoz 172/4/10; pri 170 ga izrecni PATH zamenja.
Pri 182 navigator postane null ob STEER, recovery 387/8/11 ostane nedokončan.
Izgube brez HOLD so vidne tudi pri G/4 (151, 202), G/6 (199) in G/0 (212).
Vzrok ni zajet. `javap -c -p` mapirane vanille potrdi, da `checkForStuck`
pred `clearPath` v stenski timeout veji izniči node/timer/limit, navadni `clearPath`
pa samo currentPath. To je nova preverljiva hipoteza, ne dovoljenje za izklop zaščite
ali obujanje invalidiranih poti. Gostejši izpis sam vpliva na stenski čas, zato niti
potrjen podpis ne nadomesti reprodukcije in običajnega A/B po morebitnem popravku.
F1–F12 in M4.14b ostanejo odprti. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-075 — M4.14b: navigator dodatek ne sme ugasniti krmilnika (2026-10-26, Q58)

**Vprašanje:** vsi trije zagoni `-TraceNavigator` so padli z `script errored` ob prvem
vzorcu, sklad ni ohranjen, statična in `jjs` proba pa napake ne ponovita. Ponoviti isti
zagon, opustiti navigator diagnostiko ali spremeniti dodatek?

**Odločitev:** dodatek ujame vsako napako branja, zapiše `FORM-NAVERR step=<polje>`
(največ 3) in v zapis `navError=<polje>`; krmilnik teče naprej. Bralnik vsak
`FORM-NAVERR` zavrne (fail closed). ID-ja ostaneta `m414b-navigator-harness-v3`/
`-trace-v3` (definiciji nespremenjeni, nov odtis). Produkcija in FA merila isti.

**Razlog:** isti zagon brez spremembe bi dal isti neinformativni izid; opustitev bi pustila
FA5 brez dokaza o avtorju izgub. Ujeta napaka pokaže točno polje in izjemo in hkrati ohrani
FA izide istega zagona. Diagnostika ostane diagnostika, ne sprejem. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-076 — M4.14b: Vec3d v navigator dodatku samo z refleksijo (2026-10-26, Q59)

**Vprašanje:** po D-075 zagon `m414b-navigator-trace-v3` (odtis 21a3b9a3…) teče do konca,
a vsak vzorec zapiše `FORM-NAVERR step=lastPosCheck error=java.lang.NoClassDefFoundError:
net/minecraft/util/math/Vec2f`. Kaj popraviti?

**Odločitev:** komponente `Vec3d` (`lastPosCheck`, `timeoutCachedNode`) dodatek bere samo
z refleksijskimi `Field` objekti tipa polja (`getDeclaredField('lastPosCheck').getType()`),
nikoli z `value.x`. Pogodbeni test dobi `Vec3d` dvojnik, ki ob neposrednem dostopu vrže isto
napako. ID-ja ostaneta `-v3` (definiciji nespremenjeni, nov odtis). Produkcija in FA merila ista.

**Razlog:** forgeSrc 2847 potrdi: `Vec2f` je `@SideOnly(CLIENT)` in na dedicated strežniku
odstranjen, `Vec3d.fromPitchYawVector(Vec2f)` pa ni. Nashorn ob prvem dostopu do člana
introspektira vse metode razreda in pade; `jjs` na `forgeBin` (s klientskimi razredi) tega ne
ponovi, kar pojasni D-075. `BlockPos`/`Vec3i` takih podpisov nimata. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-077 — M4.14b: novo iskanje člana začne svoje okno vanilla časovne meje (2026-10-26, Q60)

**Vprašanje:** navigator sled na 4a7a6623… je popolna (8.960 zapisov, brez `FORM-NAVERR`).
Kdo izniči nedokončane obvoze G in kaj popraviti?

**Ugotovitev [M/K]:** vseh pet izgub obvoza (G/7@182, G/1@200, G/4@202, G/0@212, G/3@214;
poti 11/–/4/11/13 točk, `byInterval=1`) ima podpis vanilla izteka `checkForStuck`
(vozlišče `ZERO`, čas 0, meja 0 v istem ticku). Vsak obvoz se je začel s podedovanim
`timeoutTimer` **6.748/5.020/1.996/5.714/9.113 ms** (G/1: novo iskanje 402 pade že v ticku 200): forgeSrc 2847 čas ponastavi samo ob
izteku, ne ob novem vozlišču ali novi poti, STEER pot z eno točko pa ga vsak tick polni.
Ob prvi menjavi na bližnje vozlišče se meja (`3 × razdalja / AIMoveSpeed`) izračuna majhna in
pot pade. Ostalih 14 podpisanih izničenj (tick 12–25 obeh prog, G/4@151) so STEER poti z eno
točko, ki jih Squad naslednji tick spet postavi (brez iskanja). Izničenja brez podpisa so
lastni `clearPath` (SETTLING HOLD na zadnji točki, zamenjava obvoza) ali konec 1-točkovne poti.

**Odločitev:** po uspešnem `tryMoveToXYZ` člana Squad pokliče
`RwNavigatorAccess.restartTimeout`, ki nastavi isto stanje, kot ga vanilla nastavi po svojem
izteku (vozlišče `ZERO`, čas 0, meja 0; refleksija MCP/SRG `field_188557_k/_188558_l/_188560_n`).
Časovna meja ostane aktivna za novo pot in preverba premika na 100 tickov ostane nespremenjena;
STEER, `replan` in neuspela iskanja se ne dotaknejo. Unit reprodukcija (G/7@170 s 6.748 ms)
in kontrola, da zastala pot še vedno poteče. FA pragovi, scena, hitrosti in config isti.

**Razlog:** podpis je potrjen v vseh petih primerih in pojasni `byLost` (7 izgub v
recovery sledi); ne izklopi zaščite in ne obuja izničenih poti. Pričakovano zmanjša samo
FA5 izgube; FA5 G ima še 20 zahtev zaradi premika cilja, FA1/2 in FA4 imata geometrijska
neskladja (§ Ugotovitve iz kode) in ostaneta merjena brez spremembe praga.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-078 — M4.14b: zamrznitev kode po D-077, sledenje skupni poti v nov paket M4.14d (2026-10-26, Q61)

**Vprašanje:** navigator sled na 984dc27a… potrdi D-077 (`iskanj_izgub` 7 → 0, izničenj 28 → 17,
nobene izgube obvoza G). FA5 G še pade z 18 zahtevami `iskanj_premik` (meja 4). Popraviti
naprej v M4.14b ali kodo zamrzniti in zaključiti preostale ID-je?

**Ugotovitev [M]:** od 18 zahtev so 3 obtičanja (dovoljena), 4 prva iskanja kolone za zidom,
2 opravičila zaostanka (`excused` ne šteje kot obtičanje), 4 premiki točke kolone >3 bloke in
5 prehod v SETTLING. 13 od 15 nedovoljenih ima cilj na skupni poti enote, ki je že poiskana;
06-FORMACIJE §1 pravi, da člani tej poti sledijo, Squad pa za PATH vedno pokliče
`tryMoveToXYZ`. To je manjkajoč del zasnove, ne napaka s podpisom.

**Odločitev:** koda M4.14b ostane na 984dc27a; na istem odtisu se zaženejo še
`m414b-harness-v1`, `m414b-ab-v1`, `m414b-trace-v1`, `m414b-recovery-harness-v2` in
`m414b-recovery-trace-v2`, neuspeli FA se zapišejo z vzroki in M4.14b se ne razglasi za
izboljšavo. Nov paket **M4.14d**: pot člana v PATH iz točk skupne poti, kadar sta član in cilj
ob njej; pravo iskanje ostane za obtičanje, člana daleč od poti in replan. Najprej unit
reprodukcije skupin iz razčlenitve. FA pragovi, scena in config nespremenjeni.

**Razlog:** en dokazan popravek na paket. Sledenje skupni poti spremeni vedenje vseh
članov v koloni, zato potrebuje svoje reprodukcije in svoj A/B. FA1/2 in FA4 imata ločena
geometrijska vzroka, ki ju M4.14d ne reši. F1–F12 so ročni dimni test v klientu z
igralcem in ostanejo odprti. [Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-079 — M4.14b: zaključek kot preverba, ne izboljšava (2026-10-26, Q62)

**Vprašanje:** na zamrznjenem odtisu 984dc27a… imajo vsi ID-ji D-078 rezultat. Harness-i,
unit, build in package uspejo; ab-v1, trace-v1, recovery-trace-v2 (8.960 zapisov,
`iskanj_izgub` G 0) in navigator-trace-v3 padejo samo na FA1/2, FA4 G/O in FA5 G. Ali M4.14b
zapreti, čeprav FA niso izpolnjeni?

**Odločitev:** M4.14b se zapre kot **opravljena preverba v svetu, ne kot izboljšava** (pravilo
scenarija: merilo, ki pade, se zapiše in paket se ne razglasi za izboljšavo). Neuspehi z vzroki:
FA1/2 — merilo M2.7 šteje bližino skupnemu cilju, formacija pa postavi člane na 8 različnih
mest (nadomestna preverba 8/8 mest uspe G in O); FA4 — geometrija kolone skozi dvoblokovna
vrata (G 7,571–8,592), O 6,42 je idealni premer LEGIJE 6,325 + odstopanje članov od mest; FA5 G — člani ne
sledijo skupni poti, gre v **M4.14d**. Pragovi, scena in config ostanejo; meril ne mehčamo.
F1–F12 so ročni test z igralcem in ostanejo odprti (odvisno od uporabnika).

**Razlog:** dodatno popravljanje v M4.14b bi mešalo pakete; dokazi so popolni in vezani na en
odtis. FA1/2 in FA4 sta lastnosti meril/geometrije, ne napake, ki bi jo M4.14d ali M4.16
rešil; o morebitni spremembi meril odloča samo nova utemeljena odločitev, ne ta paket.
[Dokazi](meritve/2026-10-22-M4.14b-formacije.md).

### D-080 — M4.14d: člani ob skupni poti ji sledijo (način TRACK) namesto iskanja (2026-10-26, Q63)

**Vprašanje (Q63):** kdaj član v PATH sledi skupni poti enote namesto `tryMoveToXYZ`, kako
daleč od poti sme biti in kaj je varovalka, če sledenje ne uspe?

**Odločitev:** planer doda način **`TRACK`** (3). Kadar bi bil član po starem pravilu v PATH
(napaka > `STEER_MAX` ali kazen opravičila `pathUntil`), **ni** pa v kazni obtičanja
(`stuckUntil`, nov števec, nastavi ga samo `detectStuck`), in sta **član in njegov cilj
največ `TRACK_JOIN` = 4,0 bloka** vodoravno od skupne poti, dobi TRACK: `Squad` sestavi pot iz
točk skupne poti od projekcije člana do projekcije cilja (`PathTrack.between`, tudi nazaj),
doda cilj in jo preda navigatorju z ponastavljeno časovno mejo (D-077). Pot ostane, dokler jo
navigator ima in se cilj ne premakne za več kot `TRACK_REBUILD` = 1 blok; potem se sestavi
znova, spet brez iskanja. Pravo iskanje ostane za: obtičanje (tudi v TRACK — `detectStuck`
zdaj velja za STEER **in** TRACK), člana ali cilj dlje od 4 blokov od poti, enoto brez poti
(`brez_poti`, ravna nadomestna črta ni prehodna pot) in replan. Nedokončan obvoz iz iskanja ima
prednost (kot pri STEER). Nov števec `sledenj=` v `RWSQUAD` šteje sestavljene poti TRACK; v
`iskanj` se ne šteje.

**Razlog:** D-078 je pokazal, da ima 13 od 15 nedovoljenih iskanj G cilj na že poiskani skupni
poti (06-FORMACIJE §1, M5.6). Meja 4 bloke: v sledi 984dc27a so bili vsi člani G ob teh
iskanjih ≤ 3,4 bloka od poti (bočni odmik LEGIJE je 3 bloke), končna mesta ≤ 3,2 bloka od konca
poti; ravni priključek ≤ 4 je krajši od STEER dosega (6), ki ga planer že šteje za ravno hojo.
Varovalka obtičanja v TRACK nadomesti dosedanjo predpostavko, da iskanje vedno uspe; FA5 meja
(1 + obnove + obtičanja) ostane ista. Formacije so nova koda brez originala, zato brez stikala
(enako kot M4.14a). Merila, scena in config se ne spremenijo. Unit reprodukcije:
`SquadPlannerTest` (+7) in `SquadAdapterTest` (+7).

**Dopolnitev (seja 150, Q64):** navigator sled na ae8a889f… (popolna, 8.960) je FA5 G
znižala 19 → 15, meja pa je 7 (`obtical=6`). Razčlenitev 14 iskanj po članu/ticku: 6 jih je
ob obtičanjih (6@61; 0, 2, 5@151; 4, 6@181), **8 je ponovitev med kaznijo obtičanja**
(`iskanj_premik`: 6@201, 4@202, 2@204, 0@205, 4/6@208, 2@209, 0@210) — `stuckUntil` je
60 tickov silil PATH in vsak premik cilja nad 3 bloke je sprožil novo iskanje, čeprav so bili
vsi ti člani in cilji ≤ 3,2 bloka od skupne poti. Popravek: obtičanje dovoli **eno** pravo
iskanje — `Squad` po izdanem iskanju pokliče `SquadPlanner.searched(i)`, ki počisti
`stuckUntil` (kazen `pathUntil` ostane). Član nato prehodi obvoz (ima prednost) in potem
sledi skupni poti; dlje od 4 blokov od poti ostane PATH kot prej. Nova obtičanja se še vedno
štejejo in vsako dovoli eno iskanje, kar je točno FA5 meja. Unit: `SquadPlannerTest` +2.

**Zavrnitev (seja 150, Q65):** na 61bf7d8f… navigator sled (8.960) in ab-v1 FA5 G formalno
uspeta (11 ≤ 11, 12 ≤ 15), a meja raste z obtičanji, ki jih TRACK sam povzroči: obtičanja G
3–6 → 10–14, G DONE 233–246 → 318/348 tickov, FA4 G 6,932 / 10,609 (M4.14b 7,571–8,592).
Vsota iskanj in obtičanj se ne zmanjša, gibanje je počasnejše. Obtičanja so ob zidu bočno od
vrat, pogosto v TRACK s ciljem kolone za članom (hipoteza: gneča na sredinski črti). Poskus
**ni sprejet**: koda ostane v zgodovini (`884175d`), produkcija in testi povrnjeni na
`cbad3c3` (= izmerjeni 984dc27a…). M4.14d ostane odprt; nov poskus mora poleg FA5 ohraniti
G DONE ≤ 246 tickov in obtičanja G ≤ 6. Pragovi, scena in config isti.
[Dokazi](meritve/2026-10-26-M4.14d-sledenje-poti.md).

### D-081 — M4.16: ločen harness letenja, cena `leteci-200` v M4.16b (2026-10-26, Q66, Q67)

**Vprašanji:** (Q66) ali M4.16 obsega proge in celico `leteci-200` v enem paketu? (Q67) ali se
proge *vogal*, *daleč* in *labirint* dodajo v `r2-run.ps1`, kot pravi vrstica v 03-FAZE?

**Odločitev:** M4.16 se razdeli: **M4.16** so proge in veličine kakovosti letenja (zid, vogal,
labirint, daleč leteči in kopenski, prosta kontrola; faze en klic / osvežen / polovična
hitrost), **M4.16b** je celica `leteci-200` (patrulja in boj) v `perf-run.ps1`. Proge dobijo
**lasten harness** `letenje-run.ps1` (svež svet `-PrwWorld` na zagon, config nespremenjen in
preverjen s SHA, tri ponovitve prek `ponovitve-run.ps1 -Scenarij letenje`); `r2-run.ps1` ostane
nespremenjen. Scenarij: [`scenariji/M4.16-letenje.md`](scenariji/M4.16-letenje.md).

**Razlog:** `r2-run.ps1` je stopnja `r2` integracijske matrike, na kateri je M5 zaprt (M5.Z,
D-055); sprememba bi spremenila že izmerjeno merilo. Celica `perf-run.ps1` ima svoj protokol
(lestvica, ogrevanje, MSPT, P-merila) in drug izid (cena, ne kakovost); en paket bi mešal dva
načina merjenja. Vrstni red D-038 se ne spremeni: M4.16b je pred prvim posegom v letenje
(M4.17), ker meri ceno, ki jo M4.17 doda. Brez produkcijske kode.

**Izid (26. 10., Q68):** na `2882787d…` tri ponovitve, vseh 180 veličin razpon 0; original z enim
`navigateTo` na zid/vogal/labirint/daleč 0/6, osvežen zid 6/6, drsenje po prihodu ≥ 1,76/2,38
bloka, hitrost iz `navigateTo` ne velja (0,97). Pragovi izhodnega kriterija M4 ostanejo: 6/6 je
pri ničelnem razponu strogo merilo, drsenje ≤ 0,25 ostane **[H]** do meritve Hover (M4.3).
[Dokazi](meritve/2026-10-26-M4.16-letenje.md).

### D-082 — M4.16b: celica `leteci-200` kot dve varianti `perf-run.ps1` (2026-10-26, Q69, Q70)

**Vprašanji:** (Q69) »patrulja in boj« v eni celici ali v dveh? (Q70) kako dokazati, da je
patrulja res tekla (brez tega bi celica merila mirujoče leteče NPC-je)?

**Odločitev:** dve varianti scenarija M2.4, isti protokol (ogrevanje 120 s, merjenje 300 s, merila
P1–P9): **`leteci`** (N × `PERF_Leteci`, tava v `WalkingRange 16`, frakcija 0, postavitev kot
`idle`) in **`leteci-boj`** (N/2 `PERF_LetA` + N/2 `PERF_LetB`, frakciji 1↔2, 100.000 HP,
postavitev kot `boj`). Fixture so iste zamenjave predloge `T_Flyer` kot obstoječe
`PERF_Idle`/`PERF_BojA/B` predloge `T_Stand` (`perf-fixture.py`), torej se od kopenskih razlikujejo
samo v `MovementType 1`, `MovingState 1` (tavanje) in `WalkingRange 16`. Krmilnik `PERF_Kontrola`
na koncu izpisa doda `leteci/letA/letB`, `letecih` (način gibanja 1), `vZraku` in `odDoma`.
Novi merili: **L1** letecih = N v letečih celicah (0 v ostalih), **L2** pri `leteci` brez cilja in
ranjenih ter ob koncu `odDoma ≥ N/4`. Vsaka varianta ima svoj test ID (`ponovitve-run.ps1` dovoli
eno celico na ponovitev): `m416b-leteci-v1`, `m416b-letboj-v1`, tri ponovitve originala.

**Razlog:** mešana celica (patrulja, ki preide v boj) bi imela fazo prehoda znotraj merilnega okna
in vrednost, ki ni ne ena ne druga obremenitev. Ločeni celici sta primerljivi z obstoječima
kopenskima `idle-200`/`boj-200` (ista mreža, isti HP, isti udarec), M4.17 pa doda ceno ravno na
sledenje poti (patrulja) in lovljenje tarče (boj). N/4 je spodnja meja, ki jo tavanje z dosegom
16 in premori med premiki brez težav preseže, mirujoča ali obtičala celica pa ne. Ostali kopenski
izpisi ostanejo veljavni (nova polja so na koncu, bralec jih sprejme kot neobvezna).

**Izid (2026-10-26, seja 152, Q71):** na `34b3574d…` sta `m416b-leteci-v1` in `m416b-letboj-v1`
zelena (T1–T6, v vsakem zagonu P1–P9 in L1, L2 oz. P5). `leteci-200`: MSPT p50 1,475
(1,475–1,606), p95 8,651 (8,651–9,175), p99 10,224, `npc.us` 17,16 (16,82–18,06), 65 MB/s,
`odDoma` 197–200. `leteci-boj-200`: p50 1,507, p95 2,228 (2,228–2,294), p99 2,753, `npc.us` 7,62
(7,53–7,75), 12,4 MB/s, s ciljem in ranjenih 200/200. Proti kopenskima celicama M2.6 (drug dan
in koda, okvirno, Q71): patrulja letečih dražja od stoječega `idle-200` (p95 +40 %, vsebuje
ceno tavanja), leteči boj cenejši od `boj-200` (p95 −43 %, µs/NPC −52 %). To je izhodišče cene
za M4.17 in M4.3. [Dokazi](meritve/2026-10-26-M4.16b-leteci-200.md).

### D-083 — M4.14d poskus 2: iskanje samo ob dokazu, TRACK samo naprej in samo ob zaprti ravni črti (2026-10-26, Q72)

**Vprašanje (Q72):** kakšen nov pristop k M4.14d, ki zmanjša prava iskanja G brez porasta
obtičanj in časa do DONE (dodatno merilo Q65: G DONE ≤ 246 tickov, obtičanja G ≤ 6), ne da bi
ponovil zavrnjeni poskus 1 (D-080)?

**Odločitev:** pravo iskanje (`tryMoveToXYZ`) samo ob **dokazu**, da ravna hoja ne gre:
1. **Opravičilo ne vsili iskanja.** `lagTicks > WAIT_LIMIT` nastavi le `excused` in točko kolone
   (`trailUntil`), ne več `pathUntil`. Zaostanek ni dokaz zaprte črte; tega da `detectStuck`.
2. **Preverba ravne črte** (`SquadPlanner.straightWalkable`): v korakih 0,4 bloka po sredini in na
   obeh robovih (polširina 0,29, tik pod 0,3 NPC-ja) mora biti `Terrain.standY` stojno z največ
   enim blokom višinske razlike med koraki. Rezultat velja do 10 tickov, če se član in cilj ne
   premakneta za 0,3. Do napake 1,5 bloka se ne preverja; dlje od `STRAGGLER` (14) šteje kot
   neznano (ne prosto).
3. **Način člana:** kazen obtičanja → PATH (eno iskanje; `Squad` po izdanem iskanju pokliče
   `searched(i)`, ki kazen počisti — kot dopolnitev Q64). Sicer: črta zaprta **in** član in cilj
   ≤ 4 bloke od skupne poti **in** cilj vsaj 0,5 bloka **pred** članom vzdolž poti → **TRACK**
   (pot iz točk skupne poti od projekcije člana do projekcije cilja in cilj, brez iskanja; samo
   naprej, nikoli nazaj). Sicer napaka ≤ `STEER_MAX` → STEER kot prej; dlje: točka kolone po
   prosti črti → STEER, vse ostalo → PATH.
4. TRACK v `Squad`: pot ostane, dokler jo navigator ima in se cilj ne premakne za več kot
   `TRACK_REBUILD` = 3 bloke (ista meja kot ponovno iskanje PATH); nova pot dobi ponastavljeno
   vanilla časovno mejo (D-077). Nedokončan obvoz iz iskanja ima prednost. `detectStuck` velja za
   STEER in TRACK. Enota brez poti (`brez_poti`) TRACK ne uporablja (pade v PATH). Nov števec
   `sledenj=` v `RWSQUAD`; v `iskanj` se ne šteje.

**Razlika od poskusa 1:** poskus 1 je TRACK dal vsakemu članu, ki bi bil po starem pravilu v PATH
(tudi po opravičilu in s ciljem **za** članom), zato je člane ob zidu vlekel nazaj po sredinski
črti in povzročil gnečo (obtičanja 3–6 → 10–14). Poskus 2 pusti STEER, kjer je črta prosta (tudi
bočno ob vratih, ko je cilj že poravnan z odprtino), TRACK samo naprej skozi ozko grlo in nazaj
samo pravo iskanje.

**Razlog:** razčlenitev D-078 in Q64: 9 iskanj G v tickih 193–207 je bilo ob kazni opravičila,
vsi člani ≤ 4,6 bloka od cilja; 8 ponovitev `iskanj_premik` med kaznijo obtičanja. Podpis obtičanj
poskusa 1 (Q65) so TRACK cilji za članom (151, 181, 193, 211). Formacije so nova koda brez
originala, zato brez stikala (kot M4.14a). FA pragovi, scena, config in meje Q65 ostanejo iste.
Unit reprodukcije: `SquadPlannerTest` +9, `SquadAdapterTest` +4. Obstoječi test M4.14b
`acquiredSlotStillNeedsPathAfterDistantDisplacement` je zahteval PATH zunaj `STEER_MAX` tudi na
prosti ravni črti, kar je v nasprotju s pravilom 3; njegov namen (prihod na mesto ne sme ukiniti
iskanja zunaj dosega) zdaj preverja z zidom med članom in mestom, nov test pa STEER po prosti
črti (Q73).

**Sprejem:** unit, build, package, `m414b-navigator-harness-v3`, `m414b-navigator-trace-v3`,
`m414b-ab-v1` na istem odtisu; FA5 G ≤ 1 + obnove + obtičanja, G DONE ≤ 246, obtičanja G ≤ 6.
Če katero od teh pade, poskus 2 ni sprejet (kot Q65) in produkcija se povrne na `cbad3c3`.

**Izid (2026-10-26, seja 153, Q74):** na `6f39a760…` unit, build, package in
`m414b-navigator-harness-v3` zeleni; `m414b-navigator-trace-v3` izhod 1: G `iskanj` 18 → 4
(+8 TRACK), obtičanja 3 (≤ 6 velja), a **G DONE 288 > 246** in **FA5 G 5 > 4**, FA4 G 9,326.
Poskus 2 ni sprejet; koda v `bf02674`, produkcija in testi povrnjeni na `cbad3c3`. M4.14d
odložen do per-članske analize zamude ob vratih; naslednji M4.10.
[Dokazi](meritve/2026-10-26-M4.14d-poskus2.md).

### D-084 — M4.10: nadaljevanje delne poti in vmesni cilj izven dometa pod `RwPathResume` (2026-10-26, Q75–Q77)

**Vprašanje:** kako NPC pride do cilja, ki ga vanilla iskalnik ne doseže v enem iskanju (M4.16:
proga *daleč* 48 blokov `navig0` 0/6 za leteče in kopenske; *zid* 0/6, ker NPC obstane na koncu
delne poti 9 blokov pred ciljem), brez lastnega A\* (M4.12 zavržen, D-038) in brez spremembe
originala pri izklopljenem stikalu?

**Raziskava:** M4.0 §1 točka 2 in §5 (vrstica M4.10) zadostujeta; nove raziskave ni bilo treba.
Iz kode 1.12.2 **[K]**: `PathFinder.findPath` sprejme vozlišče samo pri `distanceFromOrigin <
maxDistance`, kjer je `distanceFromOrigin` vsota manhattanskih korakov; zato cilj, ki je od NPC-ja
manhattansko ≥ domet (`FOLLOW_RANGE` = `NpcNavRange` = 32), nikoli ne dobi cele poti.

**Odločitev:** nov razred `rework/nav/PathResume` (pravilo brez sveta) in navigatorja
`RwPathNavigateGround` (obstoječ) ter nov `RwPathNavigateFlying` (podrazred `PathNavigateFlying`,
v `EntityNPCInterface` namesto vanilla razreda; tja pride tudi M4.17). Pri `RwPathResume=1`:
1. **Ukaz** je vsak zunanji `getPathToPos` (`tryMoveToXYZ`, `navigateTo`, AI taski). Cilj se
   zapomni. Če je manhattansko ≥ domet, se išče do **vmesnega cilja** na črti proti cilju,
   manhattansko domet − 4 (= 28; pri dometu < 8 polovica), celoštevilsko zaokroženo proti NPC-ju.
2. Ko je pot ukaza **prehojena** (`isFinished`) in ni končala ≤ 1,5 bloka od cilja (leteči cilj
   je blok višje), navigator v istem `onUpdateNavigation` poišče novo pot od mesta, kjer NPC je
   (spet z vmesnim ciljem, če je treba). AI taski zato vmesnega konca ne vidijo kot `noPath`.
3. **Konec:** pot konča pri cilju; nova pot ne konča vsaj 1 blok bliže cilju kot vse dosedanje
   ali je ni (cilj je od tam dokazano nedosegljiv); 16 obnov na ukaz; `clearPath` (vanilla
   obtičanje/iztek ostane, skripta), pot do entitete, `setPath` s tujo potjo ali nov ukaz.
4. Iskanje obnove gre **mimo** negativnega predpomnilnika S5 (`RwPathNegCache`): ta bi delno pot
   do istega cilja zavrnil 20 tickov (M4.0). `updatePath` in pot do entitete nista ukaz (S5 velja).
5. Stikalo med tekom: `/rwpath nadaljuj 0|1`; odgovor `RWPATH` ima števce `nadZacetih`,
   `nadVmesnih`, `nadObnov`, `nadPrispelih`, `nadBrezNapredka`, `nadMeja`, `nadPrekinjenih`.

**Razlog:** najmanjši poseg, ki pokrije obe polovici premise M4.0 (delna pot in cilj izven
dometa) za kopenske in leteče, ne da bi spreminjal iskalnik (proračun 200 vozlišč, filter
dometa) ali sledilnik. Pri 0 je vsaka metoda `super` in stanje se ne vklopi (izhodni kriterij
M4: »obstoječi NPC-ji z izklopljenimi stikali se obnašajo enako kot prej«). To je sprememba
obnašanja (D-007), zato stikalo, **privzeto 0**; o privzetem vklopu odloči meritev in Q77.

**Sprejem (Q76):** unit (`PathResumeTest`, 12 reprodukcij na geometriji M4.16), build, package;
`m416-letenje-v1` na novi kodi s stikalom 0 mora dati iste veličine kot M4.16 (razpon 0);
`m410-letenje-nad-v1` (isti scenarij, `-Nadaljevanje 1`) mora na *daleč* (DF in DW) dati
`prispeli` 6/6 v fazi A z enim `navigateTo`, LT1–LT9 in T1–T6 zeleni; proga P (kontrola) ne sme
biti slabša. M2.7 (`m410-nav-orig-v1` proti `m410-nav-nad-v1`): nobena veličina se ne sme
poslabšati čez šumni pas. `m414b-*` na isti kodi zaradi spremenjenega kopenskega navigatorja
(pri 0 pričakovano enako `cbad3c3`; FA neuspehi D-079 ostanejo).

**Izid (26. 10., seja 155, odtis `b53a0638…`):** stikalo 0 da vseh 180 median M4.16 (razpon 0).
Z nadaljevanjem v fazi A: DF 0/6 → **6/6** (220/230 tickov), Z 0/6 → **6/6** (205/250), P
enako, V in L ostaneta 0/6 (M4.17). DW pri 600 tickih 0/6, ker kopenski NPC pri 0,066 b/t 48
blokov ne prehodi v fazi (Q78; `navig0` 6/6, na poti); pri 1200 tickih (`-TrajanjeFaze 1200`)
**6/6** v 710/720 tickih, original pri 1200 ostane `navig0` 0/6. M2.7: vseh 12 `pot.*` enakih,
razlike samo v µs stolpcih znotraj šuma. `m414b-navigator-trace-v3` pade na istih FA kot D-079.
Merila Q76 so izpolnjena z izjemo DW pri 600, ki jo pojasni Q78. **M4.10 sprejet pod stikalom,
privzeto 0** (Q77): sprememba obnašanja, cena na `leteci-200` ni izmerjena; o privzetem vklopu
odloči kasnejši sprejem M4 skupaj z M4.17. [Zapis](meritve/2026-10-26-M4.10-nadaljevanje.md).

### D-085 — M4.17: vidna črta pri sledenju poti letečega NPC-ja pod `RwFlyLineOfSight` (2026-10-26, Q79–Q81)

**Vprašanje:** kako leteči NPC z enim `navigateTo` obleti vogal in sledi poti skozi hodnike
(M4.16: *vogal* 0/6, NPC-ji obstanejo ob steni; *labirint* 0/6 brez premika), ne da bi menjali
iskalnik (M4.5 je pogojen) in brez spremembe originala pri izklopljenem stikalu?

**Raziskava:** M4.0 §1 točka 1, §2.2 in §4 zadostujejo; nove raziskave ni bilo treba. Iz kode
1.12.2 **[K]**: `PathNavigate.pathFollow` vsak tick za točke poti na višini `floor(posY)` od
najdaljše proti trenutni kliče `isDirectPathBetweenPoints(položaj NPC-ja, točka)`; prva, ki vrne
`true`, postane trenutna. `PathNavigateFlying.isDirectPathBetweenPoints` prehodi celice (DDA), a
nobene ne prebere in vedno vrne `true` (razen ničelne razdalje). CustomNPC+ (`trimPath`) skoči
samo na točke ≤ 6 blokov in samo ob prostem žarku; 1.21.1 samo za eno točko ob prostem žarku
**[K-CNPC+, K-1.21]**.

**Odločitev:** nov razred `rework/nav/FlightFollow` (pravilo brez sveta) in prepis
`isDirectPathBetweenPoints` v `RwPathNavigateFlying` (D-084). Pri `RwFlyLineOfSight=1` je točka
neposredno dosegljiva samo, če:
1. je od NPC-ja evklidsko ≤ **6 blokov** (CustomNPC+; razdalja se preveri pred branjem blokov) in
2. hitbox NPC-ja, zmanjšan za **0,05 bloka** na vsaki strani, premaknjen od trenutnega položaja do
   točke v enakih korakih ≤ min(0,5, širina) bloka (najmanj 0,1), v nobenem položaju ne zadane
   bloka (`World.collidesWithAnyBlock`: samo bloki, brez entitet, kot vanilla `isNotColliding`
   brez entitetnega dela).
Sicer ostane prejšnja točka; če ni proste nobene, vanilla zanka indeksa ne spremeni in NPC leti
k trenutni točki (obtičanje po 100 tickih ostane vanilla). Sledilnik (`pathFollow`), pogoj iste
višine, napredovanje po razdalji `width/2` (za velike modele je v 1.12 že po razdalji),
`FlyingMoveHelper` in iskalnik ostanejo nespremenjeni. Stikalo med tekom: `/rwpath vidno 0|1`;
odgovor `RWPATH` ima `vidno=` in števce `vidKlicev`, `vidDalec`, `vidZaprtih`, `vidProstih`.

**Razlog:** najmanjši poseg, ki odpravi izmerjeni vzrok (skok skozi steno), in edini, ki ga plan
M4.17 predvideva; premik hitboxa namesto enega žarka, ker žarek po sredini pusti rob hitboxa
zadeti vogal (reprodukcija `FlightFollowTest.diagonalaCezRobKrakaJeZaprta`). Zmanjšanje 0,05,
ker NPC po drsenju ali pristanku stoji stotinko v tleh in bi sicer izgubil vse kandidate. Pri 0
je metoda `super` (izhodni kriterij M4: »z izklopljenimi stikali enako kot prej«). Sprememba
obnašanja (D-007) → stikalo, **privzeto 0** (Q81).

**Sprejem (Q80):** unit (`FlightFollowTest`), build, package, `m416-samotest-v1`;
`m416-letenje-v1` s stikalom 0 = M4.16 (180/180); `m417-letenje-vid-v1` (`-VidnaCrta 1`): *vogal*
v fazi A 6/6 prispelih, *labirint* se premakne (prevoženo > 0), kontrola P in ostale proge ne
slabše po številu prispelih; `m417-letenje-vidnad-v1` (`-VidnaCrta 1 -Nadaljevanje 1`, končna
kombinacija M4.10 + M4.17): izid *labirinta* z enim `navigateTo` zapisan (cilj 6/6). Cena:
`m417-leteci-vid-v1`/`m417-letboj-vid-v1` proti `m416b-leteci-v1`/`m416b-letboj-v1`, oboje na
isti kodi (Q81), MSPT p95 ≤ razpon originala + 10 % **[H]** (izhodni kriterij M4).

**Izid poskusa 1 (seja 156, odtis `75269123…`, Q82–Q84):** stikalo 0 = M4.16 (180/180).
Z vidno črto se *labirint* premakne (0 → 0,125 b/t, B 6/6), z M4.10 vred **6/6 z enim
`navigateTo`**; drsenje na prosti progi 2,375 → 2,057. *Vogal* ostane 0/6 tudi z nadaljevanjem:
pot se že v originalu konča pred krakom (`cele0` 0/6) — vzrok je iskanje (200 vozlišč), ne
sledilnik; hipoteza M4.0 §2.2 za vogal ovržena, vogal gre v odločitveno točko **M4.5** (Q82).
Kombinacija z M4.10 da *zid* 5/6 proti 6/6 samega M4.10 (Q84, vzrok [H]). Cena: patrulja p95
8,913 = original (meja izpolnjena), **boj p95 2,490 → 3,277 ms (+32 %)**, `npc.us` 9,50 → 12,79,
+20 MB/s — meja + 10 % ni izpolnjena. **Poskus 1 ni sprejet (Q83).** Poskus 2: pred premikom po
korakih ena preverba unije vseh korakov (če je prosta, je prost vsak korak; sicer nespremenjeni
koraki) — enak izid z manj klici `collidesWithAnyBlock`. **Izid poskusa 2 (`b79530e1…`, Q85):**
letenje 180/180 enako poskusu 1 (tudi z nadaljevanjem), patrulja v meji, **boj p95 2,556 → 2,818 >
2,812** (+10,3 %, en korak ločljivosti), `npc.us` 9,69 → 11,02, +7,8 MB/s; vsako prosto preverbo
odloči unija. Preostala cena je Forge `GetCollisionBoxesEvent` v **vsaki celici** zanke
`World.getCollisionBoxes` (World:1470). **Poskus 2 ni sprejet.** Poskus 3: dogodek enkrat pred
zanko, nato `FlightFollow.scan` (prepis zanke originala) — izid enak za vsakega poslušalca, ki je
funkcija svojih argumentov (vse objave v originalu imajo iste argumente in prazen seznam).
**Izid poskusa 3 (`346e8b08…`, Q86):** letenje 180/180 enako poskusom 1/2, alokacija boja +7,8 →
+0,6 MB/s, a **boj p95 2,556 (2,425–2,621) → 2,884 > 2,883**; preostanek je ~81 branj celice na
preverbo (~9.260/tick). **Poskus 3 ni sprejet.** Poskus 4: chunk enkrat na stolpec
(`Chunk.getBlockState` = `World.getBlockState`), zrak brez verige `addCollisionBoxToList` (`NULL_AABB`)
— enak izid, cenejša celica. [Zapis](meritve/2026-10-26-M4.17-vidna-crta.md).

**Izid poskusa 4 (29. 10., seja 159, `b8f98d28…`, Q87):** tri primerjave po 180 median
letenja so enake poskusu 3 (in s tem 1/2 ter pri stikalu 0 M4.16). Cena patrulje p95 9,175 →
9,175 ≤ 10,0925; boja 2,556 → **2,687 ≤ 2,8116 ms**, `npc.us` 9,97 → 10,57, alokacija
12,4 → 12,6 MB/s. Enakost in cenovna meja uspeta. **Zaključek še čaka:** vseh 11 ID-jev
poskusa 4 je zelenih, vendar trenutni obvezni seznam vsebuje tudi 12 ID-jev poskusov 1–3,
ki še nimajo rezultatov na trenutnem odtisu. Q87: zahtevamo te ID-je brez spremembe kode ali
parametrov; stari rezultati niso dokaz nove kode, čeprav so parametri teh ID-jev enaki v4.
Vse dodatne serije ocenimo po istih merilih; morebitne razlike ali presežke raziščemo.
M4.17 do takrat ostane v teku, brez končnega sprejema. Q81 (privzeto 0), Q82 (vogal → M4.5)
in Q84 (zid v kombinaciji 5/6, ovira za privzeti vklop) ostanejo nespremenjeni.

**Nadaljevanje validacije (29. 10., seja 160, Q88):** aktivna naloga nima rezultatov,
controller pa sporoča nov odtis `adf047b4…`. Starih rezultatov na `b8f98d28…` ne prenašamo.
Zahtevamo vseh 23 ID-jev iz Q87 na trenutnih nespremenjenih vhodih (unit/build/package,
samotest, original letenja, originala cene ter štiri variante m417 v1–v4). Vsako serijo
ocenimo; merila in parametri se ne spremenijo. Cenovna meja se izračuna iz originala istega
odtisa (zgornji rob razpona p95 × 1,1), ne iz stare absolutne številke. Privzeto stikalo 0,
Q82 in Q84 ter zahtevana enakost 180/180 ostanejo. Pred veljavnimi rezultati ni zaključka
ali commita paketa.

**Obnova predpomnilnika (29. 10., seja 161, Q89):** na `adf047b4…` je controllerjev
`unit` uspel, `build --offline` pa je padel pri `extractMcpMappings`: za zaklenjeni
`de.oceanlabs.mcp:mcp_snapshot:20171003-1.12` ni lokalnega artefakta. To ni rezultat
sprejemnih meritev in ne razlog za menjavo mappings. Controller naj skozi obstoječi
`dev.ps1` enkrat izvede `extractMcpMappings` brez `--offline` (nov ID
`m417-mcp-cache-v1`), nato celotno obvezno validacijo z običajnim offline buildom.
`environment-lock.json`, koda, config, scene in pragovi ostanejo nespremenjeni.
Registracija novega ID-ja spremeni controllerjev odtis (katalog je njegov vhod), zato
se tudi `unit` ponovi na odtisu po registraciji; rezultat na `adf047b4…` ne nadomesti
novega. Če prenos ne uspe, se razišče njegov konkretni izpis pred meritvami; starega
jarja ali druge različice mappings ne uporabimo kot nadomestilo.

**Zaključek (29. 10., seja 162):** Q90 (lastnik, 29. 10.): M4.17 je sprejet na podlagi celotne serije poskusa 4 na `b8f98d28…`. Vseh 11 obveznih ID-jev je zelenih, letenje 180/180, obe cenovni meji izpolnjeni. Dodatnih 12 ID-jev poskusov 1–3 (Q87) in ponovne validacije 23 ID-jev (Q88) ne izvajamo. Razlog: lastnik izrecno zahteva hitrejši napredek in manjšo porabo časa in žetonov. Produkcijska koda in harnessi se od te serije niso spremenili (`dev/src` brez sprememb po 28. 10. 12:38 UTC). Nov odtis izvira samo iz popravka controllerja `overnight/lib.mjs` (OPS.9, iskanje `claude.exe`) in kataloškega ID-ja `m417-mcp-cache-v1`. Q87 in Q88 sta zaprti. Privzeto `RwFlyLineOfSight=0` (Q81), Q82 (vogal → M4.5) in Q84 ostanejo. **M4.17 zaključen.**

### D-086 — M4.2: pogodba shranjenega načina letenja (2026-10-29, Q6/Q31/Q91)

Samostojno po pooblastilu lastnika: ORIGINAL=0/HOVER=1/MOMENTUM=2, izrecni ID-ji, ne ordinal.
Q91: root NBT int `RwFlightMode` v obstoječih hookih entitete; original brez ključa,
manjkanje/neznan ID/napačen tip → original. Sledi vzorcu `RwNavBackend`, brez prenosa DataAI.
`movementType`, originalni NBT tipi in omrežni ID-ji ostanejo isti; M4.2 izbiro samo shrani.
Q6: samostojni NPC-ji, igralčevo vozilo ločeno po R2 §7. Q31: (c), samo načini brez dodatkov
pristajanja/višine; samodejni spust nasprotuje lebdenju, dodatki nimajo izmerjene potrebe.
Q92: v1.0 nadaljuje s Hover M4.3; Momentum/M4.6 in GUI M4.2b sta v2, ID 2 je rezervirana izbira.
Sprejeto: controller unit/build/package/M4.16 zeleni na `d7a9f810…`, 180/180 median = original.
[Pogodba, omejitve in dokazila](meritve/2026-10-29-M4.2-nacin-letenja.md).

### D-087 — M4.3: opt-in Hover krmilnik (2026-10-29, Q93–Q97)

Q93: `RwFlightMode=1` izbere Hover samo za leteče, hitrost navigatorja × `MOVEMENT_SPEED`;
atribut že sledi NPC hitrosti, `FLYING_SPEED` je tudi že registriran, novega ne uvajamo.
Q94: neposredna hitrost, približevanje ≤ polovica razdalje/tick, yaw ≤20°, brez ukaza stop;
lastništvo hitrosti zavira tudi zunanje sunke. `travel`, trki in iskanje poti ostanejo isti.
Q97: poskus 1 sprejet pod izbiro, privzeto original: unit/build/package, 180/180 enakost,
H0–H4 in restart 30 letečih zeleni; cena enkrat kot podatek po Q92. Omejitve poti in odmik
ob ponovnih ukazih B ostanejo, celoten M4 ni zaključen. Q95/Q96 sta popravka dokazne kode,
brez spremembe produkcije ali meril. [Rezultati in razlogi](meritve/2026-10-29-M4.3-hover.md).

### D-088 — M4.15a: meritve izbirnega ozadja pred GUI (2026-10-29, Q98–Q107)

Q98: podatkovna lestvica 500/1000/2000 boj/nedosegljiva, enkrat 60+120 s/stran (Q92),
brez cenovne meje ali trditve o potrjeni kapaciteti; privzeto globalno 0.
Q99: obstoječi compiled runtime knjižnice na obeh straneh; SHA razredov/resources
in obeh configov v odtisu, izbrani NPC-ji 1 in dejanski priklop pred/po merjenju.
Q100: `largeEntities=false`; velik NPC vanilla fallback, let/plavanje/montirani izključeni;
po razmontiranju/rebuild priklop nazaj. Kompatibilnost 18/18 zelena.
Q106: vseh 12 celic izmerjenih; obe lestvici exit 1 zaradi U1 pri 2000 nedosegljivih,
potrditev te obremenitve odložena, ne sprejeta. Brez ponavljanja ali spremembe meril.
Q107: GUI vanilla/Baritone 0/1, globalni config samo dovoljenje; strežniško dejansko
ozadje/razlog, brez obljube pospešitve/kapacitete. Produkcija ista; naslednji M4.15b.
[Pogoji, merila in odločitve](meritve/2026-10-29-M4.15a-baritone.md).

### D-089 — M4.15b: prehodno stanje navigacije v AI editorju (2026-10-29, Q111–Q113)

Obstoječi AI Get/Save paketi, brez novih ID-jev; int izbira `RwNavBackend` 0/1,
`RwNavGuiStatus` samo v odgovoru, ne v shranjenem AI/svetu. Manjkajoč/neveljaven
save ključ ohrani izbiro starega klienta; poslano dejansko stanje se ignorira.
Strežnik prebere API priklop in dejanski navigator, brez mutacije. UI je posnetek
ob odprtju z razlogom fallbacka in oznako neshranjene izbire; lokalni config ni vir.
Privzeto vanilla in ista tick koda; sprejem zahteva unit/build/package in tri
strežniške pogodbe (z/brez knjižnice, globalno 0/1); klient/reload ostaneta M4.15c.
Q116: sedem zahtevanih controller testov zelenih na `d71d75d4…`; M4.15b zaključen.
[Pogodba, omejitve in rezultati](meritve/2026-10-29-M4.15b-gui.md).
Q122: M4.15c zaključen na `c91ad234…`, šest controller testov zelenih, GUI/save/restart
70/70 ×2 + 68/68 in 30 PNG pregledanih; opt-in sonda, produkcija M4.15b ista.
[Klientova dokazila in odločitve Q117–Q122](meritve/2026-10-29-M4.15c-klient.md).
