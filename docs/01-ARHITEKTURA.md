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
  vpis v `docs/04-STANJE.md`.
- [`KAMKEEL/CustomNPC-Plus`](https://github.com/KAMKEEL/CustomNPC-Plus) — 1.7.10, ima že
  implementirano to, kar zahtevata R7 (Java scripting prek Janino), R4 (frame-based
  animacije z in-game urejevalnikom) in del R2 (flying AI s 3D pathfindingom) ter 154+
  script hookov. **To je glavni vir za R3.** Koda je za 1.7.10, torej vsaka funkcija
  potrebuje port, ne kopiranje.

Praktično: preden implementiramo karkoli iz R1–R9, **najprej pogledamo, kako sta to rešila
ta dva projekta**. To prihrani dneve. Ampak vsako prevzeto rešitev zapišemo v
`docs/04-STANJE.md` z izvorom in licenco.

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
z zapisom v `docs/04-STANJE.md`.

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

Rezultati gredo v `docs/meritve/<datum>-<paket>.md` in se povzamejo v `docs/04-STANJE.md`.

---

## 8. Licence in izvor kode

| Vir | Licenca | Kaj smemo |
|---|---|---|
| CustomNPCs original | Noppes, glej `dev/LICENSE.txt` | lokalna predelava; distribucija po pogojih originala |
| BetaZavr fork | CC BY-NC 3.0 | branje, ideje, prevzem z atribucijo; **ne komercialno** |
| CustomNPC+ (KAMKEEL) | branch po dovoljenju avtorja | branje, ideje; port zahteva preverbo pogojev |

**Pravilo:** vsak prevzet blok kode dobi v izvorni datoteki komentar z izvorom, verzijo in
licenco, in vnos v `docs/04-STANJE.md` → "Prevzeta koda".

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
| D-012 | 2026-09-17 | **Vanilla pathfindinga ne prepisujemo.** Popravljamo ga po stopnjah A→B→C, vsako pod stikalom in z meritvijo; stopnja D (async iskanje, flow fieldi, lasten gibalni sklad) je **zavrnjena**, dokler ne pade pogoj spodaj | Meritev M2.2 je pokazala, da od štirih opaženih simptomov navigacije **nobeden ni A\* algoritem**: dva sta napaki CustomNPCs AI (mutex biti `EntityAIAttackTarget:38` — *24. 9. ovrženo, D-020* —, `minRange` vezan na `npc.width` `:98`), eden je predpogoj `PathNavigateGround.canNavigate()`, eden pa način uporabe (en sam `navigateTo` čez več kot `NpcNavRange`). Prepis A\* bi tri od štirih pustil nedotaknjene. Vanilla A\* ima resnične slabosti (proračun 200 vozlišč `PathFinder:65`, manhattanska cena pri 8-smernem gibanju `PathPoint:86`, domet vezan na `FOLLOW_RANGE`), a so to **namerne varovalke za MSPT**, ne napake: pri 27 dejavnih NPC-jih je p99 že 115 ms od 50 ms proračuna, zato bi večji proračun vozlišč brez predpomnjenja šel naravnost v lag. Največji pričakovani dobitek ni kakovost poti, ampak **cena**: 8 NPC-jev proti istemu cilju izvede 8 neodvisnih iskanj (M5.6). Stopnja D se odpre šele, če po M4.10, M4.11 in M5.6 merila iz M2.7 še vedno padajo; sama po sebi prinese race conditione med iskanjem in spreminjanjem sveta, kar je edini del tega sklopa z resnično visokim tveganjem |
| D-013 | 2026-09-16 | Rep porazdelitve `server.tick.ns` se pripiše s tabelo 16 najpočasnejših tickov s kontekstom (NPC-ji, `ChunkEvent.Load`/`Unload`, `WorldEvent.Save`), ne s profilerjem in ne s hranjenjem vseh tickov | Percentili povedo, koliko tickov je počasnih, ne pa katerih in zakaj; prva veljavna meritev ima `p95 = 1,6 ms` in `p99 = 81,8 ms` in trije kandidati so bili enako verjetni. Hranjenje vseh tickov pri 20 TPS in urah merjenja ni mogoče, sampling profiler pa bi spremenil to, kar merimo, in bi zahteval agenta v dev zagonu. Tabela top-N je na vroči poti ena primerjava brez alokacije, kontekst pa pride iz dogodkov, ki jih zbiralnik že posluša. Ločeni števci tickov nad 10/25/50/100 ms povedo pogostost, ki je iz tabele ni mogoče brati. Pripis ne trdi vzročnosti, samo sočasnost — in merilo S2 (najpočasnejši tick v tabeli = `max` porazdelitve) dokazuje, da oba merila gledata isti tick |
| D-014 | 2026-09-16 | Seja prevaja in testira `noppes/npcs/rework/**` sama, proti mapiranim razredom iz `dev/build/tmp/recompileMc/compiled`; uporabnikov gradle zagon ostane edina avtoriteta za obnašanje v igri | Do 15. 9. je bila vsaka vrstica kode nepreverjena, dokler je ni pognal uporabnik — `rwdiag-run.ps1` je bil 15. 9. objavljen brez sintaktične preverbe. Forge in Minecraft razreda sta v projektu že prevedena in mapirana, zato sta dovolj za `javac --release 8` in JUnit; manjkata samo zunanji knjižnici (guava, log4j), ki nista v repozitoriju, zato se za prevajanje nadomestita z minimalnima začasnima nadomestkoma **izven** repozitorija. Kar se s tem preveri: sintaksa, imena in podpisi Forge API-jev, logika brez Minecrafta. Kar se s tem **ne** preveri: nalaganje moda, obnašanje v svetu, meritve. Zato se paket še vedno ne šteje za narejenega, dokler ne steče `rwdiag-run.ps1` |
| D-015 | 2026-09-17 | **Kakovost navigacije se meri s sondo z lastnim `PathFinder`-jem nad `NodeProcessor`-jem entitete**, ne z `navigator.getPathToXYZ`; pogostost iskanj se opazuje kot dodelitev novega objekta `Path` in je **spodnja meja**, ne točno število | Iz opazovanja AI-ja se kakovost poti ne da prebrati: NPC na koncu **delne** poti izgleda enako kot NPC, ki se ne premika, in prav ta zamenjava je 17. 9. stala eno napačno razlago (Q11). Sonda zato sproži iskanje z znanim ciljem. Navigatorjeva pot za to ni uporabna, ker si `getPathToPos` cilj zapomni v `targetPos` in ob istem cilju vrne že izračunano pot (`PathNavigate.java:111-124`): sonda bi spremenila stanje tega, kar meri, in pri ponovitvah merila branje predpomnilnika namesto iskanja. `getNodeProcessor()` je javen, `PathFinder.findPath(IBlockAccess, EntityLiving, BlockPos, float)` pa je natanko tisto, kar kliče navigator, zato sonda meri isto kodo brez stranskega učinka — a **samo iz server niti**, ker je `NodeProcessor` deljen in ga `findPath` inicializira in počisti. Celost poti se ne bere iz `Path.getTarget()`, ker je ta `@SideOnly(Side.CLIENT)` (`Path.java:152`), ampak iz primerjave zadnje točke poti s ciljnim vozliščem (meja 2,0 bloka, ista kot v scenariju). Šesta veličina (iskanj na tick) nima dogodka in klicnih mest v M2 ni mogoče instrumentirati (`EntityNPCInterface` pride z M3.1), zato zbiralnik šteje spremembo **identitete** objekta `Path`: to so uspešne dodelitve poti, ne vsa iskanja. Iskanje, ki vrne `null` ali ga zavrne `canNavigate()`, je za ta števec nevidno, zato je številka spodnja meja; cena navigacije na tick se oceni kot zmnožek veličin 5 in 6, točno število pa pride z M3.1 |
| D-016 | 2026-09-18 | **Ena ponovitev je cel zagon merilne skripte v svežem svetu**, ne ponovno branje istega loga in ne drugo pometanje sonde v istem zagonu; zapis zagona loči **odtis** (pogoji meritve) od **veličin** (kar se meri), združuje pa se po imenih veličin in ne z regexom čez formatirano tabelo | Protokol §7 zahteva tri ponovitve, tabela M2.7 pa je nastala iz enega zagona in zato nima razpona — brez razpona ni praga, nad katerim sme A/B v M4.10–M4.13 sploh trditi izboljšavo. Svet v `dev\run\world` se med zagoni ohrani z vsemi NPC-ji vred, zato bi drugi zagon `nav-run.ps1` brez reseta meril 32 NPC-jev namesto 16; reset (`.\testworld.ps1`) je zato del ponovitve, merilo T4 (enak odtis v vseh ponovitvah) pa ga ujame tudi, kadar odpove ali ga kdo izklopi. Ločnica odtis/veličine je nujna, ker bi bila mediana čez zagone z različnimi pogoji povprečje različnih poskusov in bi izgledala enako verodostojno kot meritev. Združevanje prek strojno berljivega zapisa (`meritve-lib.ps1`, shema 1) namesto prek `.md` tabele: sprememba širine stolpca ne sme tiho pokvariti agregata. Prag 20 % je samo oznaka `stabilna`/`SUMNA`; pravi prag posamezne veličine je njen izmerjeni razpon, in kadar je mediana 0, se relativni razpon ne izračuna (`n/a`), ker bi bil nadomestek številka, ki izgleda kot meritev in ni |
| D-017 | 2026-09-23 | **Baseline M2.6 se meri s spawnom naenkrat**, `perf-run.ps1 -Razprseno` ostane za primerjavo | Preverba idle 500 (z `nogui`): razpršen spawn ne zmanjša repa (p95 206 proti 218 ms, ticki > 50 ms 291 proti 275), počasni ticki so enakomerno po fazah `tick % 10`. Hipoteza sinhronega `ticksExisted % 10` je ovržena; spawn naenkrat je enostavnejši in enak stanju po restartu serverja |
| D-018 | 2026-09-24 | **Kdo krmili nosilca, odloča `RiderState`, izvede pa se kot razveljavitev vanilla prepisa** (zajem stanja nosilca pred tickom jahača, vrnitev po njem), ne kot poseg v AI nosilca ali v `updateEntityActionState` | `EntityLiving.updateEntityActionState` je `final` in je vanilla spider jockey; coremod/ASM zaradi ene vrstice ni upravičen. Nosilec se posodobi pred potniki, zato zajeto stanje je natanko stanje po nosilčevem AI ticku. Nosilci, ki niso NPC, ostanejo vanilla v vseh načinih; vanilla krmiljenje z jahačem ostane na voljo (način 1) kot osnova za M3.10 |
| D-019 | 2026-09-24 | **Popravki jahanja (M3.3–M3.5) si delijo eno stikalo `RwMountSteering`**; gating gibalnih taskov je eksplicitna preverba v tasku, ne mutex | Gating in `tpTo` sta posledica odločitve, kdo krmili — ločeno stikalo bi dovolilo nesmiselne kombinacije (nosilec krmili, jahač pa ga vsak tick preteleportira). Mutex bitov ni mogoče uporabiti, dokler M3.6 ne popravi `EntityAIAttackTarget` (vsi taski delijo `PASSIVE`). *Popravek 24. 9. (M3.6): `PASSIVE` je vanilla bit MOVE in napad ga ima namerno (kot `EntityAIAttackMelee`); razlog za eksplicitno preverbo ostane — gibalnega taska jahača z mutexom ni mogoče ugasniti, ne da bi ugasnili tudi napad* |
| D-020 | 2026-09-24 | **M3.6 ne spreminja mutex bitov; popravi prioriteto napada** (`RwAttackPriority`, privzeto 0). Mutex biti CustomNPCs taskov se berejo kot vanilla MOVE=1, LOOK=2, JUMP=4, ne po imenih v `AiMutex` | `AiMutex.PASSIVE/LOOK/PATHING` = 1/2/4 so po vrednosti vanilla MOVE/LOOK/JUMP. Napad ima 3 kot vanilla `EntityAIAttackMelee`, zato z njim noben gibalni task ne teče hkrati — premisa M3.6 in vrstica „mutex biti“ v D-012 sta bili napačni. Dodatek `PATHING` bi spremenil le razmerje do skoka. Javap pa pokaže, da `setResponse` napadu ne poveča `taskCount`, zato ima `EntityAIWander` isto prioriteto in ga napad ne more prekiniti. Preusmeritev je odobril uporabnik ([zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)) |

