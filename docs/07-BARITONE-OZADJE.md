# 07 — Baritone kot izbirno ozadje navigacije (M7 knjižnice `npcbaritone`)

> **Oznake (5. 10.):** v CNPC se to delo odslej imenuje **NB.x** (NB = navigacija Baritone).
> **NB.x = M7.x** iz zapisov in commitov 27.–28. 9. (npr. NB.6 = M7.6 A/B scenarij, NB.10c =
> M7.10c `crowdYield`). Stare oznake ostanejo v imenih datotek `docs/meritve/*M7.*` in v
> zgodovini; **M7 v `03-FAZE.md` je animacijski sistem (R4)**. Knjižnica `npcbaritone` ima
> svoje milestone (M7 = integracija s CNPC) in jih ne preimenuje.

Odločitev: **D-022** (`01-ARHITEKTURA.md` §9). Knjižnica: repozitorij
`barittone_for_npc_rework`, milestone M7, javni API 2 (`npcbaritone-<ver>-api.jar`).
Ta dokument je **M7.1**: vsak klic navigatorja v CNPC, kaj naredi pod Baritonovim
adapterjem in ali je potreben ukrep. Stanje: 2026-09-27, na `main` po M3.6 in M4.14a. Klici so bili zbrani v `dev/reference-src`
(01Oct19, številke vrstic od tam) in preverjeni v `dev/src/patch`: prenos M3.1 in popravki
M3.2–M3.6 **niso spremenili nobenega klica navigatorja** v `ai/**` in `EntityNPCInterface`
(edina razlika je oblika casta pri `Walking`). Novi razredi `rework/**` so v §2.3.

---

## 1. Pogodba adapterja (`BaritonePathNavigate`, knjižnica M6)

Adapter je podrazred `PathNavigateGround`; vanilla taski ga kličejo nespremenjeno.

| Metoda | Pod Baritonom |
|---|---|
| `tryMoveToXYZ` | optimistično `true`, če je cilj naložen; iskanje teče na iskalni niti. Isti cilj (±1 blok) ne sproži novega iskanja |
| `tryMoveToEntityLiving` | sledenje; nov cilj, ko se tarča premakne > 2 bloka (največ vsakih 10 tickov) |
| `noPath` | `false` med iskanjem in hojo; `true` po prihodu, neuspehu, v mirovanju in med pavzo po `clearPath` |
| `clearPath` | pavza; isti cilj v 10 tickih nadaljuje brez iskanja |
| `setPath(vanilla)` | cilj = zadnja točka (`GoalNear` r=1); `setPath(null)` kot vanilla (`false`) |
| `getPath` | vanilla `Path` iz Baritonove poti; med iskanjem ena točka na cilju (od `2ee0df2`) |
| `getPathToPos` / `getPathToXYZ` / `getPathToEntityLiving` | **vanilla, sinhrono** (hibrid) — proračun 200 vozlišč ostane |
| `setSpeed(s)` | `s > 1,0` dovoli sprint, sicer hoja; hitrost "kot igralec" (`speedMode=player`) ali lastna (`own`) |
| neuspeh | `FAILED`, `noPath()=true`; isti cilj je 20 tickov zavrnjen |

Taski, ki si navigator shranijo v konstruktorju, se ob `attach` preusmerijo (knjižnica `55f4245`).

---

## 2. Klici v CNPC (105 zadetkov v izvirniku, 28 datotek, brez `client/`; + `rework/**` v §2.3)

Oznake: **OK** — deluje enako ali bolje; **razlika** — dokumentirano drugačno obnašanje;
**ukrep** — brez popravka integracija ni pravilna.

### 2.1 AI taski (`noppes/npcs/ai`)

| Datoteka:vrstica | Klic | Pod Baritonom | Ocena |
|---|---|---|---|
| `EntityAIAmbushTarget:68,72,76` | `noPath`, `tryMoveToXYZ` (zaklon), `clearPath` | zaklon doseže tudi čez teren, kjer vanilla odneha | OK |
| `EntityAIAnimation:34` | `!noPath()` → animacija hoje | med iskanjem (1–2 ticka) že hoja | razlika (kozmetika) |
| `EntityAIAttackTarget:51` | `getPathToEntityLiving` (pogoj `shouldExecute`) | **vanilla A\***: če vanilla poti ne najde, napad ne začne — Baritonova prednost se tu ne pozna | **ukrep U1** |
| `EntityAIAttackTarget:75,83,90` | `setPath(p,1.3)`, `clearPath`, `tryMoveToEntityLiving(1.3)` | cilj = konec vanilla poti, nato sledenje tarči s sprintom | OK |
| `EntityAIAvoidTarget:37,44` | navigator v polju (konstruktor) | preusmerjen ob `attach` (`55f4245`); ob ponovni gradnji taskov glej U2 | OK |
| `EntityAIAvoidTarget:83,88,92` | `getPathToXYZ` (vanilla) + `setPath` | beg do točke, ki jo najde vanilla | OK (hibrid) |
| `EntityAIAvoidTarget:102,104` | `setSpeed(1.2/1.0)` | 1,2 dovoli sprint | OK |
| `EntityAIDodgeShoot:58,62` | `noPath`, `tryMoveToXYZ(1.2)` | | OK |
| `EntityAIFindShade:53,57` | `noPath`, `tryMoveToXYZ` | | OK |
| `EntityAIFollow:44,49,67` | `noPath`, `clearPath`, `tryMoveToEntityLiving` | sledenje lastniku; preverjeno v knjižnici (volk, `selftest`) | OK |
| `EntityAILook:34` | `noPath()` → ogleduje se samo v mirovanju | | OK |
| `EntityAIMoveIndoors:54,58` | `noPath`, `tryMoveToXYZ` | preverjeno v knjižnici (vaščan, `selftest`) | OK |
| `EntityAIMovingPath:26,44,45,59` | patrulja: `noPath` → naslednja točka | nedosegljiva točka: `FAILED` po iskanju, nato naslednja | razlika (čas) |
| `EntityAIOpenAnyDoor:55` | `getPath()` → vrata ob poti | branje deluje, null je obravnavan; **Baritone železnih vrat ne šteje za prehodna**, zato NPC s "odpri vsa vrata" pot mimo njih ne načrtuje | **ukrep U4** |
| `EntityAIOrbitTarget:63,86,92` | `clearPath`, `noPath`, `tryMoveToXYZ` | | OK |
| `EntityAIPanic:47,54` | `tryMoveToXYZ`, `noPath` | | OK |
| `EntityAIRangedAttack:52,67,69` | `clearPath`, `tryMoveToEntityLiving(1.0)` | | OK |
| `EntityAIReturn:42,76,86,91,96,147,148,153` | vrnitev domov: `noPath`, `clearPath` + `tryMoveToXYZ` | `clearPath` in nov cilj v istem ticku: nov cilj začne iskanje | OK |
| `EntityAISprintToTarget:28` | `noPath` | | OK |
| `EntityAIStalkTarget:54,71,75,83` | `clearPath`, `noPath`, `tryMoveToXYZ(1.0/1.33)` | | OK |
| `EntityAIWander:50,78,121,125` | naključni cilj, `noPath`; `clearPath` na sosednjem NPC-ju | nedosegljiv naključni cilj: `FAILED` po iskanju namesto takojšnjega `false` | razlika (čas) |
| `EntityAIWaterNav:20` | `((PathNavigateGround) nav).setCanSwim(true)` | adapter je `PathNavigateGround`, cast uspe; nastavitev na Baritona ne vpliva (voda po njegovih premikih) | OK |
| `EntityAIZigZagTarget:59,66,84` | `getPathToEntityLiving` (vanilla), `tryMoveToXYZ` | | OK (hibrid) |
| `target/EntityAIClearTarget:45` | `clearPath` | | OK |

### 2.2 Entiteta, podatki, vloge, API

| Datoteka:vrstica | Klic | Pod Baritonom | Ocena |
|---|---|---|---|
| `EntityNPCInterface:761–768` (`updateTasks`) | **nov `navigator` in `moveHelper`** ob vsaki posodobitvi AI (urejanje NPC-ja, nalaganje, smrt) | adapter bi bil tiho zamenjan z vanilla | **ukrep U2** |
| `EntityNPCInterface:879–880` | `setBreakDoors(aiDoor != null)` | cast uspe, na Baritona ne vpliva | glej U3 |
| `EntityNPCInterface:464` | `Walking = !noPath()` | | OK |
| `EntityNPCInterface:559,1137,1262,1298,1771` | `clearPath` (dogodki) | | OK |
| `DataAI:437` (`setAvoidsWater`) | `setPathPriority(WATER, …)` — samo vanilla | **Baritone nastavitev prezre** | **ukrep U5** |
| `DataScenes:309–312` | `clearPath`, `getPathToPos` (vanilla), `setPath` | če vanilla ne najde poti, prizor NPC-ja ne premakne (kot prej) | razlika (hibrid) |
| `JobBuilder:119,140,174`, `JobFarmer:151,157,204,212,266,307` | `tryMoveToXYZ`, `clearPath`, `noPath` (mutex) | | OK |
| `RoleCompanion:592` | `clearPath` | | OK |
| `api/wrapper/EntityLivingWrapper:33–56` | skriptni `navigateTo` (`speed*0.7`), `getNavigationPath`, `isNavigating` | `getNavigationPath` je med iskanjem padel z NPE — **popravljeno v knjižnici** (`2ee0df2`) | OK |

### 2.3 Novi razredi `rework/**` (M3.2–M3.6, M4.14a)

| Razred | Klic | Pod Baritonom | Ocena |
|---|---|---|---|
| `rework/entity/MountGuard` (M3.3) | zajame in obnovi `currentPath` in hitrost navigatorja nosilca (`RwNavigatorAccess`) | adapter vanilla polja `currentPath` ne uporablja; obnova nima učinka | M7.8: Baritone je izključen tudi za nosilca s potnikom |
| `rework/entity/RiderState` (M3.3–M3.5) | kdo krmili nosilca | Baritone se za jahača in zasedenega nosilca ne uporablja | M7.8 |
| `rework/formation/Squad` (M4.14a) `drive`, način `STEER` | **vsak tick** `setPath` z eno točko malo pred mestom člana | pod adapterjem vsaka točka, premaknjena za > 1 blok, pomeni nov Baritonov cilj in iskanje — nepotrebna obremenitev in sunkovitost | **ukrep U7** |
| `rework/formation/Squad:148,314` | `getPathToXYZ` (vanilla) za vodjo | pot vodje ostane vanilla | OK (hibrid) |
| `rework/formation/Squad` `HOLD`, konec | `noPath`, `clearPath` | | OK |
| `rework/diag/*` (`NavSweep`, `DiagEventCollector`) | sonda z lastnim `PathFinder` (D-015), opazovanje poti | meri vanilla sondo, ne adapterja | OK (meritev ostane primerljiva) |

### 2.4 Česar klici ne pokažejo

| Tema | Pod Baritonom | Ocena |
|---|---|---|
| hitrost NPC-ja (`getSpeed()`, atribut) | `speedMode` je v knjižnici globalen; `player` = 4,3 m/s ne glede na nastavitev NPC-ja | **ukrep U6** |
| domet iskanja (`NpcNavRange`, `FOLLOW_RANGE`) | Baritone ga ne bere | M7.5 |
| `movementType` 1 (let) in 2 (plavanje) | `PathNavigateFlying`/`Swimmer` — Baritone se ne uporablja | izključeno (D-022) |
| NPC na nosilcu ali NPC, ki nosi potnika (D-018/D-019 jahanje) | oba uporabljata vanilla navigator; R1 je pokazal zastoj zasedenega nosilca pod Baritonom | M7.8 |

---

## 3. Ukrepi

| ID | Ukrep | Kje | Paket |
|---|---|---|---|
| U1 | `getPathToEntityLiving`/`getPathToPos` pod Baritonom: ostane vanilla (hibrid) ali optimistična pot proti tarči. Odloči A/B na M2.7 — sprememba vpliva na veličino 1 | knjižnica, pod stikalom | M7.5–M7.7 |
| U2 | Po `updateTasks()` adapter znova namestiti (in preusmeriti shranjene navigatorje novih taskov). V knjižnici: `reinstall` v API (današnji `attach` za že pripeto entiteto ne zamenja navigatorja znova) | CNPC `updateTasks` + knjižnica API | M7.2, M7.4 |
| U3 | `doorInteract` (0 razbij, 1 odpri, 2 nič) → Baritonov profil: knjižnica lesena vrata odpira vedno, zato bi NPC z "nič" vrata odpiral | knjižnica (nastavitev vrat na instanco) + CNPC preslikava | M7.5 |
| U4 | "Odpri vsa vrata" (železna): nastavitev, ki železna vrata šteje za prehodna, če jih NPC zna odpreti | knjižnica | M7.5 |
| U5 | `avoidsWater` → Baritonov profil (cena vode) | CNPC preslikava | M7.5 |
| U6 | hitrost na instanco (`own` = hitrost NPC-ja) v API | knjižnica API + CNPC | M7.5 |
| U7 | člani formacije (`Squad`, način `STEER`) ostanejo na vanilla navigatorju, dokler so v enoti; vodja sme imeti Baritona | CNPC `rework/formation` + most | M7.4 |

**Predpogoj v CNPC je izpolnjen:** `EntityNPCInterface` in `ai/**` sta prenesena v
`dev/src/patch` (M3.1, `396b607`), zato je M7.4 (veja v `updateTasks()`) izvedljiv. Vrstni red:
knjižnični del U2–U6 (API 2), nato M7.2 (most samo prek API in `Loader.isModLoaded`), M7.3
(globalno stikalo), M7.4 (`updateTasks`, U2, U7).
