# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki se spreminja vsako sejo.** Vsaka seja ga na koncu posodobi.
Če seja tega ne naredi, je naslednja seja slepa.

---

## Trenutno stanje

| | |
|---|---|
| Zadnja posodobitev | **2026-09-17** |
| Trenutni milestone | **M2 — diagnostika** (M2.1 in **M2.2** zaključena); M0.7/M0.8 čakata na uporabnika, M1 je zaključen |
| Naslednji paketi | **M2.3** (reprodukcija R2) ali **M2.4** (50/200/500 NPC-jev); **M0.7** takoj ko uporabnik naredi quest in dialog v GUI-ju. Vzrok R1 (`canNavigate`/`onGround`) dokaže šele instrumentacija v M2.1b/M3.1 |
| Trenutni milestone | **M2 — diagnostika** (M2.1 in **M2.2** zaključena); **M0 zaključen 17. 9.** (M0.7 narejen, M0.8 zabeležen kot blokada), M1 je zaključen |
| Naslednji paketi | uporabnik pozene `.\matrika-run.ps1` in `.\fixture-run.ps1` (prvi zagon, 17. 9.), nato **M2.3** (reprodukcija R2) ali **M2.4** (50/200/500 NPC-jev); **M2.7** je nov in je vhodni pogoj za navigacijski sklop M4/M5. Vzrok R1 (`canNavigate`/`onGround`) dokaže šele instrumentacija v M2.1b/M3.1 |
| Prevedljivih razredov | 32 — prejšnjih 21 + 11 v `rework/diag` (M2.1d doda `DiagChunkPlan` in `DiagChunkLoader`) |
| Testi | 31 primerjalnih v obeh načinih + 16 za varne writerje/session/fault injection + **33 za instrumentacijo** (23 + 10 novih za `DiagChunkPlan`); zeleni. Dodatno 13 preverb dedicated-server smoka (M0.5), zelene |
| Blokade | Q1 in Q5–Q10 odprta; specifična R9 forenzika je po navodilu uporabnika odložena, ne blokirana |
| Omejitev orodij | **spremenjeno 17. 9.**: seja ima lupino na uporabnikovem računalniku, a **linuxovo** in brez PowerShella, Gradla in Minecrafta. Bere, piše, ureja, `git`, `python3`, `node`, `jq` — da. `.\dev.ps1`, `.\*-run.ps1`, build in zagon sveta — **ne**, to poganja uporabnik. Podrobnosti v Znanih omejitvah |

---

## Napredek po milestonih

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **v teku** (≈90 %) | M0.1–M0.6 narejeno (+ M0.2r obnova okolja); M0.7–M0.8 odprto |
| M1 Integriteta podatkov | **zaključeno** | M1.1–M1.3, M1.5, M1.6 in M1.9 narejeni; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | **v teku** (≈60 %) | M2.1 in M2.2 zaključena in preverjena v svetu; **R1 je reproduciran in izmerjen** |
| M3 Jedro entitete | ni začeto | analiza narejena in **reprodukcija R1 obstaja**; glej R1 in R6 |
| M4 Gibanje | ni začeto | analiza narejena, glej R2 |
| M5 Performance | ni začeto | del že pokrit z M1.3, glej meritve |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | ni začeto | analiza narejena, glej R4 |
| M8 CustomNPC+ | ni začeto | katalog še ne obstaja |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |

### M1 po paketih

| ID | Paket | Stanje |
|---|---|---|
| M1.1 | Karakterizacijski testi, ki dokumentirajo napake | **narejeno** |
| M1.2 | `javap` verifikacija sumljive logike | **narejeno** — dve hipotezi ovrženi |
| M1.3 | Nov tipno varen NBT↔JSON serializer + fuzz testi | **narejeno** |
| M1.4 | Bralnik za že pokvarjene datoteke, popravek tipov kjer je mogoče | **odloženo** — ni vhodnih datotek, R9 je minoren |
| M1.5 | `SafeFileWriter` + preklop vseh controllerjev (B1) | **narejeno** — clone, player, sinhroni JSON in stisnjeni NBT |
| M1.6 | Lifecycle asinhronih zapisov (B2) | **narejeno** — session executor, zajeta pot/snapshot, drain pred resetom |
| M1.7 | Verzioniranje `SaveFormat` + migracija | **ni več nujno za R9** — format nespremenjen |
| M1.8 | `.\dev.ps1 auditClones` | **odloženo** — brez konkretnih poškodovanih datotek |
| M1.9 | Fault injection testi | **narejeno** — 9 determinističnih odpovednih scenarijev |

### M2 po paketih

| ID | Paket | Stanje |
|---|---|---|
| M2.1a | `rework/diag` jedro + zbiralnik na Forge dogodkih + ukaz `/rwdiag` | **zaključeno** — D1–D7 zelena, `.\rwdiag-run.ps1` |
| M2.1b | Klicna mesta za pot, skripte in AI taske | **čaka na prenos** `EntityNPCInterface`/`ai` (M3.1) in `ScriptContainer` (M5.1) |
| M2.1c | Števci za razčiščenje `npc.per.tick` = 0 | **zaključeno** — vzrok imenovan in dokazan, glej meritev |
| M2.1d | Pogoj meritve: `ForgeChunkManager` ticket za chunke z merjenimi NPC-ji | **zaključeno** — C1–C6 zelena v svetu, prva veljavna meritev obstaja |
| M2.2 | Reprodukcija R1 (8 jahačev na 8 nosilcih + kontrolna skupina brez jahačev) | **zaključeno** — E1–E6 zelena, R1 reproduciran; [meritev](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| M2.3 | Reprodukcija R2 (leteči NPC in ovira) | ni začeto |
| M2.4 | Merilni scenariji 50 / 200 / 500 NPC-jev | ni začeto |
| M2.5 | Merilni protokol kot skripta | ni začeto |
| M2.6 | Baseline meritve originala | ni začeto |
| M2.7 | **Merila kakovosti navigacije** (šest veličin, izmerjenih na originalu) | **nov paket 17. 9.** — podlaga za M4.10–M4.12 in M5.6; brez njega se navigacijski sklop ne začne |

---

## Dnevnik sej

### 2026-09-17 (24) — Ovrednotenje navigacije in umestitev v plan (D-012)

**Paket:** načrtovanje
**Stanje:** končano — odločitev zapisana, paketi umeščeni

**Vprašanje uporabnika:** kako izvedljivo je napisati lasten `navigateTo`, ki bi bil bistveno
boljši od obstoječega.

**Ugotovitev, ki odloči:** od štirih simptomov navigacije, izmerjenih 17. 9., **nobeden ni
A\* algoritem**.

| Simptom | Dejanski vzrok | Sloj |
|---|---|---|
| nosilec z jahačem nima poti (0/8) | predpogoj `PathNavigateGround.canNavigate()` | vanilla predpogoj |
| obstanek pri 22,71 bloka | 200 vozlišč + `NpcNavRange` = 32, in en sam klic | konfiguracija in način uporabe |
| `navig` niha 0–4/8 med napadom | mutex biti `EntityAIAttackTarget:38` | **CustomNPCs AI** |
| zgoščevanje na kup | `minRange` vezan na `npc.width` (`:98`) | **CustomNPCs AI** |

Prepis A\* bi tri od štirih pustil nedotaknjene. Dva sta že pokrita z M3.6 in M3.7.

**Kje je vanilla res slab** (vse preverjeno v dekompiliranem Minecraftu v projektu):
proračun 200 vozlišč vrne delno pot (`PathFinder:65`); manhattanska cena **in** hevristika pri
8-smernem gibanju (`PathPoint:86`) sistematično odrivata diagonale; domet vezan na
`FOLLOW_RANGE`; nič deljenja dela med NPC-ji, vsak z novim `ChunkCache` čez ~6×6 chunkov
(`PathNavigate:126-129`); `checkForStuck` po 100 tickih tiho počisti pot.

To so **namerne varovalke za MSPT**, ne napake. Pri 27 dejavnih NPC-jih je p99 že 115 ms od
50 ms proračuna, zato bi večji proračun vozlišč brez predpomnjenja šel naravnost v lag.

**Odločitev D-012:** vanilla pathfindinga ne prepisujemo. Popravki gredo po stopnjah, vsak
pod stikalom in z A/B meritvijo; brez meritve se kandidat zavrže.

**Dodano v plan:**

| ID | Paket | Zakaj |
|---|---|---|
| **M2.7** | merila kakovosti navigacije — šest veličin na originalu | brez njih je „boljše" nemerljivo; vhodni pogoj za ves sklop |
| **M4.10** | nadaljevanje delne poti | poceni, odpravi najbolj viden simptom (navidezne ovire) |
| **M4.11** | lasten `NodeProcessor` z realno ceno diagonale | odpravi cikcakanje; omejen obseg, podprta razširitvena točka |
| **M4.12** | lasten `PathNavigate` — **samo če** M4.10, M4.11 in M5.6 ne zadostujejo | drago; brez dokaza se ne začne |
| **M4.13** | mehkejše sledenje poti — samo če je trzanje merljivo | kozmetika, dokler ni številke |
| **M5.6** | deljenje in predpomnjenje poti, **povišano** iz „šele če" v redni paket | največji pričakovani dobitek v celem sklopu; 8 NPC-jev proti istemu cilju = 8 iskanj |

M4 se preimenuje v „Gibanje: navigacija po tleh in letenje"; prej je pokrival samo letenje.

**Zavrnjeno za zdaj:** asinhrono iskanje poti, flow fieldi za skupinsko gibanje, lasten
gibalni sklad. Async je edini del sklopa z resnično visokim tveganjem (svet se med iskanjem
spreminja), druga dva spremenita občutek gibanja vseh obstoječih NPC-jev. Odprejo se šele, če
po M4.10, M4.11 in M5.6 merila M2.7 še vedno padajo.

**Spremembe obnašanja:** nobene — to je načrtovalna seja.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 ali M2.4 po obstoječem vrstnem redu; M2.7 pred začetkom M4.

---

### 2026-09-17 (23) — Q11 zaprt: meja iskanja poti, ne ovira

**Paket:** M2.2 (popravek merilne naprave)
**Stanje:** končano — popravek narejen in preverjen; ponovitev meritve ni pognana

**Vprašanje:** zakaj se je kontrolna proga v fazi A ustavila pri 22,71 bloka, tik pred drugo
stopnico, čeprav je prvo stopnico in vrata prešla.

**Odgovor: ovira ni bila kriva.** Vanilla A* ima dve trdi meji, obe preverjeni v
dekompiliranem Minecraftu v projektu:

| Meja | Kje | Posledica |
|---|---|---|
| proračun **200 vozlišč** | `PathFinder.findPath:65` | vrne **delno pot** do najbližjega doseženega vozlišča, ne `null` |
| dolžina poti `< maxDistance` | `PathFinder.findPath:94` | `maxDistance` = `getPathSearchRange()` = `FOLLOW_RANGE` |
| `FOLLOW_RANGE = NpcNavRange` | `EntityNPCInterface.java:334`; `dev/run/config/CustomNpcs.cfg` → `NpcNavRange=32` | pot, daljša od 32, ni mogoča v enem klicu |

Proga je dolga **natanko 32 blokov**, z obvozom do vrat pa več. En sam `navigateTo` zato ne
more vrniti cele poti in NPC obstane na koncu delne. Skripta je `navigateTo` klicala enkrat
na začetku faze A; `EntityAIAttackTarget` v fazi B se prepathga sam — **zato je proga S v
fazi B cilj dosegla, v fazi A pa ne.** Vse tri opazke se ujamejo z isto razlago.

**Na ugotovitev o R1 to ne vpliva.** Delna pot bi bila še vedno pot: `isNavigating()` bi bil
`true` in `prevozeno` > 0. Proga M je imela 0/8 poti in 0,06 bloka. Meja pojasni samo
stransko opažanje o kontroli, ne razlike M/S.

**Narejeno:**

- `r1-control.js` v fazi A `navigateTo` ponovi ob vsakem vzorcu; vzorec se vzame **pred**
  osvežitvijo, da `navig=` pove stanje ob koncu intervala, ne takoj po svežem klicu.
- Nastal je `dev/testworld/vstavi-skripto.py`: vstavi vir v polje `Script` clone JSON in
  **sam preveri**, da sta vir in vstavljena različica po normalizaciji identična. Doslej je
  bila to ročna operacija, ki jo je scenarij sam označil za pot do tihe napake.
- Preverjeno: `R1_Control.json` se od prejšnjega commita razlikuje **samo** v vrstici
  `Script`; izluščena minificirana skripta gre skozi `node --check`.

**Ni narejeno in zakaj:** ponovitev `.\r1-run.ps1` s popravkom ni pognana — potrebuje Windows.
Ni nujna za izid M2.2; dala bi čistejšo kontrolo (proga S bi v fazi A prišla do cilja).

**Spremembe obnašanja:** nobene v modu.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 (reprodukcija R2) z upoštevanim pravilom o dometu poti.

---

### 2026-09-17 (21) — M0 zaključen: integracijska matrika (M0.7) in M0.8 kot blokada

**Paket:** M0.7 (+ formalni zaključek M0.8)
**Stanje:** končano — merila X1–X5 izpolnjena

**Narejeno:**

- Nov dokument [`scenariji/M0.7-integracijska-matrika.md`](scenariji/M0.7-integracijska-matrika.md):
  funkcionalna matrika iz `PLAN_IMPLEMENTACIJE.md` §6 je prepisana v **47 oštevilčenih preverb**
  (IN NPC, IA AI, IC vsebina, IS skripte, IL lifecycle, IK klient, IO omrežje). Vsaka vrstica ima
  tip (**A** avtomatizirana / **R** ročna), imenovan dokaz (marker v logu, datoteka, NBT diff) in
  pošteno stanje pokritosti.
- Postopek izvedbe v treh prehodih: avtomatski server tek iz obstoječih skript
  (`verify-package.ps1`, `testworld-run.ps1`, `rwdiag-run.ps1`, `r1-run.ps1`), ročni klientski
  prehod in zapis izida v `audit\m07-matrika-<datum>.md`. Pravilo: **matrika ni nikoli zelena po
  opustitvi** — neizvedena vrstica ni uspeh.
- Zapisan enkratni postopek za quest/dialog fixture v GUI-ju (§4), ki odblokira IC1 in IC2.
- **M0.8 zaprt kot zabeležena blokada.** Uporabnik je potrdil, da nima dostopa do dejanskega
  modpacka, Forge builda, configa ne kopije pravega sveta in da nima nobenega pokvarjenega clone
  zapisa. Vpliv je razčlenjen po milestonih v §6 matrike; izhodni kriterij M0 to izrecno dovoljuje.
- `docs/03-FAZE.md`: M0 označen kot zaključen z datumom in sklicem na izpolnjene izhodne kriterije.

**Trenutna pokritost matrike:** 6 vrstic pokrito (IN1, IN4, IA7, IS1, IL1, IL7), 4 delno (IN2, IN6,
IA3, IL6), 3 blokirano (IC1, IC2 na GUI fixture; IO4 na M0.8), 34 ni pokrito. To je izhodišče,
ne pomanjkljivost — matriko polnijo milestoni M1–M10.

**Ni narejeno in zakaj:**

- Matrika ni bila **pognana** — seja nima PowerShella, Gradla ne Minecrafta (glej Znane omejitve).
  Prvi realni prehod 1 naj se izvede ob zaključku naslednjega paketa.
- Quest in dialog fixture ostajata odprta; brez GUI-ja ju ni mogoče narediti, ugibanje NBT
  strukture pa protokol prepoveduje.
- Nobena vrstica ni bila avtomatizirana na novo; M0.7 je dokument, ne koda. Premik vrstic iz **R**
  v **A** je delo pripadajočih milestonov.

**Spremembe obnašanja:** nobene. Koda ni bila spremenjena, `verify-package.ps1` ostane pri istem
seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** **M2.3** (reprodukcija R2 — leteči NPC čez steno na x=20) ali **M2.4**
(merilni scenariji 50/200/500 NPC-jev). Pred predajo naslednjega paketa se prvič požene prehod 1
integracijske matrike.

---

### 2026-09-17 (20) — M2.2 zaključen: R1 je reproduciran in izmerjen

**Paket:** M2.2
**Stanje:** končano — E1–E6 zelena

**Izid v enem stavku:** nosilec z jahačem **nikoli ne dobi navigacijske poti**
(`isNavigating()` je `false` v 40 od 40 vzorcev), medtem ko je identičen nosilec brez jahača
v istem svetu in istem ticku na poti in prevozi 22,7 bloka.

Polni zapis: [`meritve/2026-09-17-M2.2-R1-reprodukcija.md`](meritve/2026-09-17-M2.2-R1-reprodukcija.md).

| Faza | Veličina | **M (jahači)** | **S (kontrola)** |
|---|---|---|---|
| A `navigateTo` | nosilcev s potjo | **0/8 ves čas** | 8/8 |
| A | prevoženo povp. / max | **0,06 / 0,06** | 19,25 / 22,71 |
| A | do cilja min | 31,44 (start 31,9) | 8,83 |
| B `setAttackTarget` | nosilcev s potjo | **0/8 ves čas** | 0–4/8 |
| B | prevoženo povp. / max | 8,26 / 9,17 | 19,62 / **32,70** |
| B | razpon skupine | 13,45 → **7,00** | 5,04 → **33,53** |
| B | do cilja min | 24,31 | **0,51** (cilj) |
| obe | jahačev na nosilcu | **8/8 ves čas** | — |
| obe | odstopanje jahač↔nosilec | **0,00 ves čas** | — |

**Kar spremeni zahtevo R1:**

- **Odprto vprašanje 2 v §R1 je odgovorjeno: nosilec poti sploh ne dobi.** Zahteva je
  predpostavljala, da jo dobi in jo nekaj kvari.
- **Hipoteza `EntityAIFollow.tpTo` se tu ne uresniči.** `odstopMax = 0,00` v vseh 40 vzorcih;
  jahač ostane točno na nosilcu. `EntityAIFollow` zahteva lastnika, fixture ga nima. R1 se
  reproducira **brez** te poti.
- **Vez jahač–nosilec je stabilna** (`jahacev=8` ves čas, tudi čez teleport med fazama).
  Pokvarjena ni vez, pokvarjena je navigacija nosilca.
- **„Ne znajo čez bloke“ je izmerjeno.** V fazi B se nosilci z jahači ustavijo pri z ≈ 19,7 in
  tam obstanejo 240 tickov. Stopnica je pri z = 20.
- **Zgoščevanje obstaja, a ni „radius enega bloka“**: razpon pade na 7,00, kontrola gre v
  nasprotno smer na 33,53. Za radius enega bloka §R1 imenuje poseben pogoj
  (`hasHitbox == false` → `minRange ≈ 0`); ti fixture hitbox imajo, zato ta pogoj **ni bil
  preizkušen**. Ločen scenarij.

**Vodilna hipoteza mehanizma — še ni dokazana.** V fazi B se nosilci premikajo, pa `isNavigating()`
ostane `false`; premikajo se torej mimo navigatorja. `PathNavigateGround.canNavigate()`
(`:32-35`) zahteva `onGround || isInLiquid || isRiding`, pri čemer `isRiding()` velja za
**jahača**, ne za nosilca. Preverjena in ovržena sta bila `addInteract()` (kliče ga samo
`interact` in `EntityAIWander`, ta pa se pri `MovingState 0` ne doda) in `EntityAIReturn`
(`ReturnToStart: 0b`). Test, ki hipotezo odloči: števec `onGround` / `canNavigate()` /
`noPath()` na nosilec na tick → **M2.1b oziroma M3.1**. Do takrat je to hipoteza.

**Stranski ugotovitvi:**

- Tudi kontrolna proga v fazi A ne pride do cilja: obstane pri 22,71 bloka, tik pred **drugo**
  stopnico (z = 36), `navig` pade z 8/8 na 4/8. Prvo stopnico in vrata je prešla (razpon se
  pri vratih stisne na 2,87 — lijak). Zakaj druga stopnica ustavi skupino, prva pa ne, je novo
  odprto vprašanje. Na razliko M/S ne vpliva, ker velja za obe progi.
- `navig` niha (0–4/8) tudi na kontrolni progi v fazi B, kar se ujema z dokazano napako mutex
  bitov v `EntityAIAttackTarget:38`. To je neodvisno od jahanja.

**Meritve:** 27 NPC-jev, 945 tickov: `npc.update.window` 207,3 µs/NPC, `server.tick.ns`
p50 = 1,93 ms, p95 = 10,75 ms, p99 = 115,3 ms. **Ni baseline** in se ne sme primerjati z M2.1d
(66,6 µs pri 8 mirujočih NPC-jih) — tu jih 16 pathfinda čez oviro.

**Spremembe obnašanja:** nobene.

**Naslednja seja:** M2.3 (reprodukcija R2) ali M2.4 (50/200/500). Vzrok R1 se dokaže šele z
instrumentacijo iz M2.1b/M3.1 in ne sme se ga razglasiti prej.

---

### 2026-09-17 (19) — M2.2: preverba artefaktov in dve blokirni napaki pred zagonom

**Paket:** M2.2 (preverba)
**Stanje:** delno — vse, kar se da preveriti brez sveta, je preverjeno; zagon ostaja

**Kontekst:** `git pull` je pokazal, da je oddaljena veja enaka lokalni in da je vse delo
seje 18 (fixture, `r1-control.js`, `r1-setup-commands.txt`, `r1-run.ps1`, scenarij)
necommitano. Seja 18 je naštela tri stvari, ki jih ni mogla preveriti. Ta seja jih je.

**Zaprte vrzeli iz seje 18:**

| Vrzel iz seje 18 | Kako je zaprta | Izid |
|---|---|---|
| `r1-run.ps1` ni sintaktično preverjen | PowerShell 7.4.6 v oblačnem okolju, `Parser::ParseFile` | 2165 žetonov, **0 napak** |
| minificirana skripta v `R1_Control.json` morda ni enaka viru | normalizacija obeh (brez komentarjev, poenoteni narekovaji, stisnjeni presledki) in primerjava znak za znak | **identični** |
| fixture morda vsebujejo več kot naštete spremembe | `diff` vsakega `R1_*` proti osnovi `T_*` | **natanko naštete spremembe, nič drugega** |

Dodatno preverjeno: vsi klici API-ja v skripti obstajajo v `reference-src` s pravimi
podpisi, in `IWorld.getAllEntities(int)` vrne **`IEntity[]`**, ne `List` — pri `List` bi
`all.length` v Nashornu tiho vrnil `undefined` in `R1-MOUNT` bi javil `n=0`.

**Napaka 1 — nosilci ne morejo skozi edina vrata v zidu.** `T_Carrier` in s tem
`R1_Carrier` imata `DoorInteract: 2` (onemogočeno). Tedaj
`EntityNPCInterface.doorInteractType()` (`:867-881`) ne doda vratnega AI taska in nastavi
`setBreakDoors(false)`, kar je `canOpenDoors = false` (`PathNavigateGround.java:317-320`).
V `WalkNodeProcessor.getPathNodeType` (`:359-362`) zaprta lesena vrata postanejo `WALKABLE`
samo ob `canOpenDoors && canEnterDoors`; sicer ostanejo `DOOR_WOOD_CLOSED` s ceno `-1.0F`,
enako kot `BLOCKED` (`PathNodeType.java:20`).

Vrata so edini prehod skozi tri bloke visok zid pri z = 28, torej pot do cilja za nosilce
**sploh ne bi obstajala** — na obeh progah enako. Pohod bi se ustavil 16 blokov od starta,
merilo E2 (kontrolna proga prevozi vsaj 20 blokov) bi padlo in izid bi izgledal kot okvara
navigacije. **Popravek:** `R1_Carrier` dobi `DoorInteract: 1`. Jahači, cilja in krmilnik
ostanejo pri `2`, ker ne navigirajo.

**Napaka 2 — merilni kanal bi lahko molčal.** Ves izpis gre skozi
`npc.executeCommand('/say …')`. `NPCWrapper.executeCommand` (`:201-206`) vrže
`CustomNPCsException`, če command bloki niso vklopljeni, `NoppesUtilServer.runCommand`
(`:226-229`) pa v istem primeru samo zapiše opozorilo. Pri `enable-command-block=false` v
logu ne bi bilo **nobene** vrstice `R1-*` in vse preverbe bi padle hkrati — videti kot
pokvarjena skripta, v resnici nastavitev serverja. V `dev/run/server.properties` je
trenutno `true` (iz M0.6), a nikjer ni bilo zapisano kot pogoj. `r1-run.ps1` to odslej
preveri v koraku 1 in po potrebi popravi.

**Tretja ugotovitev, ki ne blokira, a spreminja branje izida.** `isWalking()`
(`EntityNPCInterface.java:1417`) je `movingType != 0 || isAttacking() || isFollower() ||
Walking` in nadzoruje edino `addVelocity` (`:1020`). Navigacije ne blokira, zato
`MovingState: 0` skriptnemu `navigateTo` ne škodi. Pomeni pa, da v fazi A nosilci **ne
dobivajo sunkov ob medsebojnih trkih**, v fazi B pa jih, ker je `isAttacking()` takrat
resničen. Veličina `razpon` zato **ni primerljiva med fazo A in fazo B**. Primerljiva je
samo med progama M in S znotraj iste faze — kar je edina primerjava, ki jo scenarij trdi,
tako da merila ostanejo veljavna.

**Preverjeno tudi, da ne bo presenečenja:** `EntityAIReturn` se v `addRegularEntries()`
(`:900`) doda **brezpogojno**, a `shouldExecute()` (`:40`) se konča takoj ob
`!ais.shouldReturnHome()`. `ReturnToStart: 0b` iz seje 18 torej res izklopi vlečenje domov.

**Ni narejeno in zakaj:**

- **Scenarij še ni pognan.** Lupina te seje na uporabnikovem računalniku je linuxova in
  nima PowerShella, Gradla ne Minecrafta. Zagon je na uporabniku; dogovorjeno 17. 9.
- Krmilna skripta še vedno ni tekla v Nashornu. Preverjeni so sintaksa, podpisi API-ja in
  vračilni tipi, ne pa izvedba.

**Spremembe obnašanja:** v modu nobene. Spremenjena sta testni fixture in zagonska skripta.

**Meritve:** nobene.

**Naslednja seja:** uporabnik požene `.\r1-run.ps1` iz korena projekta; seja prebere
`audit\m22-r1.log`, ovrednoti E1–E6 in zapiše razliko M/S v `docs/meritve/`. Če E1 pade
(mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v zahtevo. Če pade E2, je
pokvarjena kontrola, ne mod.

---

### 2026-09-15 (18) — M2.2: raziskava in scenarij reprodukcije R1

**Paket:** M2.2 (priprava)
**Stanje:** delno — scenarij je zasnovan in zapisan, fixture in skripta še nista narejena

**Narejeno — raziskava, ki odpre M2.2 brez prenosa kode:**

| Vprašanje | Odgovor | Vir |
|---|---|---|
| Kako posaditi NPC na NPC brez GUI-ja? | `IEntity.setMount(IEntity)` iz skripte | `api/wrapper/EntityWrapper.java:414-420` |
| Gre to skozi vanilla `startRiding`? | **da**, `startRiding(entity, true)` | isto, `:418` |
| Ali gre `ItemMounter` po isti poti? | **ne**, paket `EnumPacketServer.SpawnRider` | `items/ItemMounter.java:31` |
| Ali nosilec sploh dobi pot? | `isNavigating()`, `getNavigationPath()` | `api/wrapper/EntityLivingWrapper.java:43-58` |
| Kako pognati skupino proti cilju? | `navigateTo(x,y,z,speed)`, `setAttackTarget(living)` | isto, `:32`, `:65` |
| Kako najti fixture NPC-je? | `world.getAllEntities(2)` + `hasTag(...)` | `WorldWrapper.java:279-297` |

- **Cela reprodukcija se da napisati kot skripta na enem krmilnem NPC-ju.** Nič ni treba
  prenesti v `src/patch/java`. To je pomembno: M2 ne sme začeti prenašati
  `EntityNPCInterface`, ker je to M3.1. S tem odpade zadnji razlog za prepletanje M2 in M3.
- S tem je **odgovorjeno tudi na odprto vprašanje 4 iz `02-ZAHTEVE.md` §R1** (ali mount
  sploh gre skozi `startRiding`) — za skriptno pot da. Za pot prek `ItemMounter` ne;
  popravek v M3 mora pokriti obe in reprodukcija prek skripte o drugi ne pove nič.
- Scenarij zapisan v [`docs/scenariji/M2.2-R1.md`](scenariji/M2.2-R1.md): prizorišče,
  potek v dveh fazah, šest merjenih veličin in merila E1–E6.

**Ugotovitev, ki spreminja zahtevo:** zahteva predpisuje „8 jahačev na 8 nosilcih“, **brez
kontrolne skupine**. Tako postavljen poskus ne dokaže ničesar — če se nosilci slabo
premikajo, je lahko vzrok jahanje ali pa se skupina osmih CustomNPC-jev tako premika tudi
brez njih. Scenarij zato vodi **dve vzporedni progi hkrati**, v istem svetu in istem ticku:
proga M (8 nosilcev z jahači) in proga S (8 istih nosilcev brez jahačev). Vsaka številka se
poroča kot par M/S; razlika je ugotovitev, absolutna vrednost ni.

Druga sprememba: scenarij ima dve fazi, ne eno. Faza A je prosta pot (`navigateTo`) — čista
navigacija; faza B je `setAttackTarget`, torej uporabnikov dejanski primer („ko grejo
napadat“) in edina, ki sproži `EntityAIAttackTarget` s sumljivimi mutex biti in `minRange`,
odvisnim od širine.

**Narejeno — gradnja (ista seja, po zasnovi):**

- `dev/testworld/r1-control.js` — **vir** krmilne skripte, berljiv in komentiran. V
  `R1_Control.json` je vstavljena minificirana (polje `Script` je en sam niz). Skripta
  najde NPC-je po tagih, pari jahače z nosilci po koordinati x, vodi obe fazi in izpisuje
  vzorce z markerjem `R1-S`.
- Štirje fixture, **generirani iz obstoječih pravih NBT zapisov** (pravilo iz
  `dev/testworld/README.md`), s točno naštetimi spremembami in nič drugim:
  `R1_Carrier` (iz `T_Carrier`), `R1_Rider` (`T_Rider`), `R1_Target` (`T_Stand`),
  `R1_Control` (`T_Scripted`).
- `dev/testworld/r1-setup-commands.txt` — prizorišče in 26 NPC-jev.
- `r1-run.ps1` — zagon in samodejno ovrednotenje E1–E6, vključno z izpisom razlike M/S.

**Ugotovitve iz gradnje — vse preverjene v kodi, nobena po spominu:**

- **Hrastova vrata se v 1.12.2 registrirajo kot `minecraft:wooden_door`, ne `oak_door`**
  (`net/minecraft/init/Blocks.java:392`). Edina vrsta vrat s starim imenom; ugibanje bi
  tiho spodletelo in zid bi ostal brez prehoda.
- Spodnja polovica vrat meta `3` = `FACING NORTH`: `BlockDoor.getStateFromMeta` (`:423`)
  bere `EnumFacing.getHorizontal(meta & 3).rotateYCCW()`.
- **`ReturnToStart` je v osnovnih fixture `1b`.** To požene `EntityAIReturn` in vleče NPC
  nazaj domov; pri 32 blokov dolgi progi bi sámo po sebi ustavilo pohod in bi izgledalo kot
  okvara jahanja. V vseh štirih R1 fixture je `0b`.
- `setAttackTarget(null)` je varen (`EntityLivingWrapper:65-71`, `EntityLivingBaseWrapper:69-75`).
- **Vrstni red zagona ni poljuben.** Krmilna skripta začne šteti ob spawnu, zato se
  `R1_Control` spawna **šele po** `rwdiag chunks on` in ogrevanju. Sicer bi se scenarij po
  300 tickih ustavil sredi faze A in bi izgledal kot okvara AI — natanko past M2.1d.
- Ena sama datoteka `R1_Carrier` streže obema progama; skripta ju loči po `x < 10`. Tako
  sta nosilca na obeh progah zagotovo identična, kar je pri kontrolni skupini bistvo.

**Ni narejeno in zakaj:**

- Scenarij ni bil pognan. `r1-run.ps1` ni sintaktično preverjen (v seji ni PowerShella) in
  krmilna skripta ni tekla v Nashornu — preverjeno je le, da se izvorna in minificirana
  različica razčlenita (`node --check`) in da se vsi klici API-ja ujemajo s podpisi v
  `reference-src`.
- Merilo E6 (zapis razlike M/S v `docs/meritve/`) nastane šele po zagonu.

**Spremembe obnašanja:** v modu nobene. Vse novo je v testnem svetu in v skriptah.

**Meritve:** nobene — reprodukcija še ni tekla.

**Naslednja seja:** pognati `.\r1-run.ps1`, ovrednotiti E1–E6 in zapisati razliko M/S v
`docs/meritve/`. Če E1 pade (mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v
zahtevo. Če E2 pade, je pokvarjena kontrola in scenarij, ne mod.

---

### 2026-09-15 (17) — M2.1d: pogoj meritve (prisilno naloženi chunki)

**Paket:** M2.1 (del d)
**Stanje:** končano — C1–C6 zelena v svetu isti dan ob 14:07

**Narejeno:**

- `rework/diag/DiagChunkPlan.java` — **čista** logika načrta: pretvorba blok→chunk (z
  `floor >> 4`, ne deljenjem, ki pri negativnih koordinatah pokrije napačen chunk),
  odstranjevanje podvojenih chunkov, razširjanje po obročih in proračun. Brez Minecraft
  tipov, zato je edini del paketa, ki se da enotsko testirati.
- `rework/diag/DiagChunkLoader.java` — lastni `ForgeChunkManager` ticketi tipa `NORMAL`,
  prisilno nalaganje chunkov vseh naloženih NPC-jev, osvežitev enkrat na sekundo prek
  `WorldTickEvent` (ta prinese svet s sabo, zato razred ne potrebuje poti do
  `MinecraftServer` izven ukaza), sprostitev ob `chunks off` in ob `FMLServerStoppedEvent`.
- `CommandRwDiag`: nov podukaz `/rwdiag chunks <on [obroč]|off|status>` z markerjem
  `RWDIAG-CHUNKS`. **Namenoma ni del `rwdiag on`** — meritev brez prisilno naloženih
  chunkov je veljavna le z igralcem v svetu, in ta razlika mora biti v scenariju vidna.
- `DiagKeys`: `world.chunks.forced` (vzorči se vsako sekundo skozi celo meritev) in
  `diag.chunks.added`. Prvi **zapiše pogoj meritve v sam posnetek**, da meritve brez pogoja
  ni več mogoče pomotoma brati kot veljavne.
- `rwdiag-run.ps1`: `chunks on` pred `rwdiag on`, nova merila **C1–C6**, stikali
  `-ChunkRadius` in `-NoChunks`. **C3 in C4 sta tisto, kar 15. 9. ni bilo preverjeno in
  zato ni bilo opaženo:** vsi števci so rasli, samo NPC-ji niso tikali. Skripta zdaj bere
  `npc.per.tick` in `npc.tick.gap` iz tabele posnetka in pade, če NPC-ji nehajo tikati.
- 10 novih JUnit testov (`DiagChunkPlanTest`); v seji je bilo zelenih vseh 33 testov
  instrumentacije, prevedenih z `javac --release 8`.
- Odločitev **D-011** v `01-ARHITEKTURA.md`; scenarij `docs/scenariji/M2.1-diag.md`
  dopolnjen z razdelkom M2.1d in merili C1–C7.

**Ugotovitve:**

- **Mod je že registriran chunkloader.** `CustomNpcs.load` kliče
  `ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkController())`. To je
  bilo bistveno: `ForgeChunkManager.requestTicket` **vrže `RuntimeException`**, če mod
  nima registriranega `LoadingCallback`. Nova koda zato callbacka ne dodaja in ne spreminja.
- Proračun: 25 chunkov na ticket, 200 ticketov (`dev\run\config\forgeChunkLoading.cfg`),
  torej do 5000 chunkov. Omejitev v kodi je 400 chunkov na svet (16 ticketov).
- **Dve napaki v originalnem `ChunkController`** (najdeni med branjem, nista popravljeni —
  protokol prepoveduje popravljati mimogrede):
  1. `getTicket` (`:40-56`) ob uspešnem ustvarjanju ticketa vrne `null` namesto ticketa.
     Klicatelj torej misli, da je ustvarjanje spodletelo, čeprav je ticket shranjen.
  2. `ticketsLoaded` (`:70`) preverja `tickets.contains(npc)`, kjer je `tickets` seznam
     ticketov, `npc` pa entiteta — pogoj je vedno `false` in je mrtev.
  Nista razporejeni v milestone; spadata k opravilu „Chunk Loader“ (job 8), ki v M2 ni
  predmet dela. To je tudi razlog, da si merilni pogoj ne izposodi tega razreda.

**Preverjeno isti dan ob 14:07 (uporabnik je pognal, seja je preverila izpise in posnetke):**

- **Deluje.** `npc.update` = **9832** (prej 1968), kar je 8,007 na tick; `npc.update` / 8 =
  1229 = število tickov. NPC-ji so tikali **v vsakem ticku celotne meritve**.
  `npc.tick.gap` ima 1227 vzorcev in vsi so 1 — brez ene same luknje. `npc.per.tick`
  p50 = 8 (prej 0).
- **C1–C6 zelena**: `chunki=9 tiketi=1 obroc=1 zavrnjeni=0 npc=8`; po `chunks off`
  `chunki=0 tiketi=0`; `TW-SCRIPT-OK`, `TW-SCRIPT-TICK-10`, 0 `ERROR` iz `noppes.*`,
  `BUILD SUCCESSFUL`.
- Polni zapis: [`docs/meritve/2026-09-15-M2.1d-prva-veljavna.md`](meritve/2026-09-15-M2.1d-prva-veljavna.md).

**Napaka v merilu, ki jo je ta zagon razkril:** C5 je bilo napisano kot `min = max` in bi
**zavrglo prav to, prvo veljavno meritev projekta**. Nabor je zrasel z 9 na 12 chunkov, ker
so trije NPC-ji zatavali iz pokritega območja in jih je osvežitev pokrila
(`diag.chunks.added = 3`) — to je delo zbiralnika, ne napaka. Pravo merilo je `min > 0`:
padec na 0 pomeni, da pogoj ni držal, rast ne pomeni ničesar slabega. Popravljeno v
`rwdiag-run.ps1` in v scenariju. Prestrogo merilo, ki zavrže veljavno meritev, je enako
škodljivo kot ohlapno, ki spusti neveljavno.

**Ni narejeno in zakaj:**

- `rwdiag-run.ps1` ni bil sintaktično preverjen v seji (ni PowerShella); preverila ga je
  šele uporabnikova izvedba, ki je tekla do konca.
- Prepad p95/p99 ni razčiščen (glej spodaj); to je M2.5, ne ta paket.

**Spremembe obnašanja:** nov podukaz `/rwdiag chunks` (raven dovoljenja 2). Dokler se ne
pokliče, ta koda ne pokliče ničesar iz `ForgeChunkManager` in ni prijavljena na event bus.
Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** prva veljavna meritev projekta. p50 = 0,557 ms, p95 = 1,638 ms od 50 ms
proračuna pri 8 NPC-jih. **p99 = 81,789 ms in max = 269,019 ms se ne smeta navajati**,
dokler prepad ni pojasnjen.

**Prepad med p95 in p99 — postavljen in isti dan zaprt.** Prva meritev je imela p95 = 1,6 ms
in p99 = **81,8 ms**. Kandidat 1 (priklop ticketa sproži nalaganje chunka, to pa je delo na
server niti) je bil potrjen s ponovitvijo ob 14:19 z **10 s ogrevanja** med `chunks on` in
`rwdiag on` — edina sprememba:

| | brez ogrevanja (14:07) | z ogrevanjem (14:19) | |
|---|---|---|---|
| `server.tick.ns` p50 | 0,557 ms | **0,410 ms** | −26 % |
| `server.tick.ns` p95 | 1,638 ms | **1,016 ms** | −38 % |
| `server.tick.ns` **p99** | **81,789 ms** | **4,325 ms** | **faktor 19** |
| `server.tick.ns` max | 269,019 ms | 86,863 ms | autosave, en tick |
| `npc.update.window` | 182,5 µs / NPC | **66,6 µs / NPC** | −64 % |

Kandidata 2 (autosave) in 3 (delo NPC-jev) sta s tem izključena kot razlaga prepada.
Presenečenje ob strani: napihnjen je bil tudi `npc.update.window` — ker se meri kot razmik
do naslednje entitete, je tick z nalaganjem chunka ta razmik raztegnil. Vsaka meritev brez
ogrevanja torej precenjuje tudi porabo na NPC. **Ogrevanje je odslej privzeto** in je pogoj
vsake meritve, enako kot prisilno naloženi chunki.

**Merodajni rezultat pri 8 NPC-jih, brez igralca, 61,3 s:** p50 = 0,410 ms in p95 = 1,016 ms
od 50 ms proračuna; NPC-ji tikajo v vsakem ticku; zgornja meja porabe na NPC je 66,6 µs.
Še vedno **ni baseline** — 8 NPC-jev je preverba naprave, ne scenarij.

**Naslednja seja:** **M2.2** — reprodukcija R1 (8 jahačev na 8 nosilcih, oba CustomNPC).

---

### 2026-09-15 (16) — M2.1c zaključen: zakaj NPC-ji nehajo tikati

**Paket:** M2.1 (del c)
**Stanje:** končano — vzrok je imenovan z datoteko in vrstico, ne z domnevo

**Narejeno:**

- Uporabnik je pognal `.\rwdiag-run.ps1` s števcem `npc.update.blocked` (tretja meritev,
  12:47). Izid: **`npc.update.blocked` = 0 in `entity.update.blocked` = 0** v 61 sekundah.
- Preden je ničla sprejeta kot podatek, je bilo preverjeno, da števec ni tiho mrtev —
  in to na **jarju, ki ga je server res naložil** (`customnpcs-dev-runtime.jar`, žig
  12:46:54; gradle je izvedel `:compileJava`, `:devRuntimeMod`, `:jar`, nobenega kot
  `UP-TO-DATE`). `DiagKeys.class` vsebuje oba ključa, `DiagEventCollector.class` ima
  `onCanUpdate` z `@SubscribeEvent` in parametrom `EntityEvent$CanUpdate`.
- **Mehanizem 1 izključen.** `World.updateEntityWithOptionalForce` (`World.java:2139-2155`)
  doseže `ForgeEventFactory.canEntityUpdate` samo, kadar preverba območja ±32 blokov pade.
  Nobenega dogodka ⇒ preverba ni padla niti enkrat.
- **Mehanizem 2 imenovan.** `WorldServer.updateEntities()` (`WorldServer.java:628-644`):

  ```java
  if (this.playerEntities.isEmpty() && getPersistentChunks().isEmpty()) {
      if (this.updateEntityTick++ >= 300) { return; }
  } else { this.resetUpdateEntityTick(); }
  ```

  Vanilla neha posodabljati entitete **300 tickov (15 s)** po nalaganju sveta, če ni
  igralca in ni prisilno naloženega chunka. Zanka do entitet sploh ne pride.
- Aritmetika se ujema pri vseh treh zagonih (247, 246 in 237 tickov z NPC-ji, glede na
  zamik med `Done` in `rwdiag on`; ločljivost žigov v logu je 1 s = ±20 tickov).
- Razrešeno tudi navidezno protislovje "`world.tick` šteje, NPC-ji pa ne tikajo":
  `MinecraftServer.updateTimeLightAndEntities` kliče `worldserver.tick()` (`:831`) in
  `worldserver.updateEntities()` (`:842`) ločeno, `onPostWorldTick` pa je za obema (`:851`).
- Polni zapis, tabele in številke:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Ugotovitve:**

- **Vse tri dosedanje meritve so neveljavne kot izhodišče.** Za ~80 % svojega trajanja so
  merile prazen tek, zato se `server.tick.ns` percentili ne smejo navajati kot baseline.
  Prva uporabna številka pride šele po M2.1d.
- Ista vrstica, ki je vzrok, je tudi popravek. Dovolj je, da **eno** od dvojega ne drži.
  `ForgeChunkManager` ticket je strožje jamstvo kot igralec: odklene zanko
  `updateEntities()` (pogoj na ravni sveta) **in** postavi `range = 0` v preverbi ±32
  blokov (pogoj na ravni chunka), in ne potrebuje človeka pred zaslonom. Scenarij mora
  pokriti chunke vseh merjenih NPC-jev, ne le enega.
- Najdaljši tick je bil 123,9 ms (prej 74,5 in 58,1) in še vedno sovpada z autosave. Merilo
  za M2.5 ostaja nespremenjeno: autosave tick izločiti ali poročati posebej.

**Ni narejeno in zakaj:**

- M2.1d ni začet. Po protokolu je to svoj paket s svojim commitom; poleg tega bo takoj po
  njem treba ponoviti meritev, kar spet zahteva uporabnika.
- Commit ni narejen v seji — seja ne more poganjati `git`. Vsebina commita: ta dnevnik,
  dopolnjena meritev in obstoječa (že napisana) koda M2.1c, če še ni commitana.

**Spremembe obnašanja:** nobene. Za M2.1c ni bila spremenjena nobena vrstica kode; ta seja
je samo pognala, preverila in razložila.

**Meritve:** tretja meritev M2.1, `audit\m21-rwdiag.log`, posnetki
`dev\run\logs\rwdiag\rwdiag-20260915-1248*`. Zeleno: `TW-SCRIPT-OK`, 0 `ERROR` vrstic iz
`noppes.*`, 0 `script errored`, `BUILD SUCCESSFUL`, števci po `rwdiag off` mirujejo
(1268/1968 → 1268/1968).

**Naslednja seja:** **M2.1d** — `ForgeChunkManager` ticket za chunke z merjenimi NPC-ji,
pod stikalom in privzeto izklopljen (D-007), plus merilo: ista meritev mora pokazati
`npc.per.tick` p50 = 8 čez celotno trajanje in `npc.tick.gap` max = 1 pri ≥ 1200 vzorcih.
Šele nato M2.2 (reprodukcija R1; uporabnik je potrdil, da sta oba NPC-ja CustomNPC).

---

### 2026-09-15 (15) — M2.1a: instrumentacija `rework/diag`

**Paket:** M2.1 (del a)
**Stanje:** koda napisana in enotsko preverjena; gradle, `verify-package.ps1` in zagon v
svetu mora pognati uporabnik (seja še vedno ne more zaganjati ukazov na njegovem računalniku)

**Narejeno:**

- Nov paket `dev/src/patch/java/noppes/npcs/rework/diag/` z devetimi datotekami:
  `Diag` (vstopna točka, vklop/izklop, kljuci, posnetek), `DiagKey` (števec + čas na
  `LongAdder`), `Distribution` (logaritemski koši, p50/p95/p99), `DiagSnapshot`
  (nespremenljiv posnetek, tabela + JSON), `DiagKeys` (imena velicin na enem mestu),
  `DiagDump` (zapis v `logs/rwdiag/`), `DiagEventCollector` (zbiranje na Forge dogodkih),
  `CommandRwDiag` (ukaz `/rwdiag`) in `package-info`.
- **Izklopljena instrumentacija ne stane nič.** Zbiralnik se na Forge event bus prijavi
  šele ob `/rwdiag on` in se ob `off` odjavi, zato ni klica na entiteto na tick. Vsa
  klicna mesta v kodi moda pa se najprej vprašajo za eno `volatile` polje.
- 23 novih JUnit testov (`DiagTest`, `DistributionTest`, `DiagSnapshotTest`). Pokrivajo:
  izklopljeno stanje ne šteje nič, vklop počisti prejšnje števce, meritev, ki prečka
  vklop ali izklop, se zavrže, natančnost percentilov, ekstremne vrednosti, točnost
  štetja pod osmimi nitmi hkrati, in to, da se posnetek med izpisom ne premika.
- `CustomNpcs.java`: tri vrstice — registracija ukaza, `-Drwdiag=on` ob zagonu serverja in
  izklop ob `FMLServerStoppedEvent`.
- `dev/build.gradle`: novi testi so izključeni iz `testOriginal`, ker se nanašajo na
  razrede, ki jih v nedotaknjenem originalu ni.
- `docs/scenariji/M2.1-diag.md`: kaj se meri, kako se bere, merila D1–D7 in postopek.

**Ugotovitve:**

- Meritev časa posodobitve NPC-ja je zaenkrat **približek**: `EntityNPCInterface` še ni
  prenesen, Forge pa da le dogodek ob začetku posodobitve entitete. Čas se zato meri kot
  razmik do naslednje žive entitete v istem ticku — to je zgornja meja, ne točna poraba.
  Točna meritev pride z M3.1 in takrat se stolpca primerjata.
- Povprečje je pri performancu neuporabno, zato `Distribution` hrani porazdelitev v
  logaritemskih koših. 32 podkošev na potenco dvojke da ~3,2 % napake navzgor pri 16 KB na
  velicino; prvotnih 8 podkošev (~12 %) je bilo pri MSPT premalo natančnih — p95 in p99
  sta padla v isti koš.
- Ukaz je namenoma `/rwdiag` in ne podukaz `/noppes`: instrumentacija mora delovati tudi,
  če je z mod ukazi kaj narobe, in ne sme spreminjati obstoječega ukaznega drevesa.

**Ni narejeno in zakaj:**

- Števci za izračune poti, klice skript in čas po AI taskih (M2.1b) so **rezervirani z
  imenom, a prazni**. Njihova klicna mesta so v `EntityNPCInterface`, paketu `ai` in
  `ScriptContainer`, ki še niso preneseni v `src/patch/java`. Prenos teh razredov je
  vsebina M3.1 in M5.1; delati ga zdaj bi pomenilo velik prenos brez pripadajočega
  popravka, kar je v nasprotju s pravilom "en paket = en commit = ena stvar".
- Koda ni prevedena z gradle in ni tekla v Minecraftu. Jedro paketa (brez Minecrafta) je
  bilo prevedeno z `javac --release 8` in vseh 23 testov je v seji zelenih, razreda
  `DiagEventCollector` in `CommandRwDiag` pa se opirata na Forge in MC API, zato ju je
  preverila samo primerjava s klici v `reference-src` (`CommandNoppes`, `ScriptPlayerEventHandler`).

**Spremembe obnašanja:** nov ukaz `/rwdiag` (raven dovoljenja 2) in nov, privzeto
izklopljen zbiralnik meritev. Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** nobene — to je paket, ki meritve šele omogoči.

**Preverjeno isti dan ob 10:52 (uporabnik je pognal, seja je preverila izpise in datoteke):**

- **D1 zeleno** — `.\dev.ps1 test`: `DiagTest` 10, `DistributionTest` 8, `DiagSnapshotTest` 5,
  skupaj 23 testov, 0 napak, 0 napak izvajanja (`dev/build/test-results/test/`). Testi zdaj
  tečejo na Javi 8 na uporabnikovem računalniku, ne le na OpenJDK 21 v seji.
- **D2 zeleno** — `testOriginal` je tekel brez treh novih razredov, kot mora.
- **D3 zeleno** — seja je sama primerjala `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`
  z originalom: 25 spremenjenih razredov (vsi pričakovani, isti nabor kot prej + `CustomNpcs`),
  27 dodanih (11 `rework/data` + 11 `rework/diag` + notranji razredi), **0 odstranjenih**.
  `CommandRwDiag` in `DiagEventCollector` sta v jarju, kar pomeni, da se Forge in MC API, ki ju
  seja ni mogla prevesti, dejansko prevedeta — to je bilo edino resno tveganje tega paketa.
- **Commit in push narejena** — `c28b654` "M2.1a: instrumentacija rework/diag + ukaz /rwdiag";
  `origin/main` kaže na isti commit. V indeksu je vseh 9 datotek paketa, 3 testi, scenarij in
  posodobljeni dokumenti.
- **D4–D7 zelena ob 12:17** (`.\rwdiag-run.ps1`, izpis `audit\m21-rwdiag.log`):
  - **D4** — server z novim jarjem se naloži in čisto ustavi, `TW-SCRIPT-OK` je v logu,
    **nobene `ERROR` vrstice iz `noppes.*`**, nobenega `script errored`, `BUILD SUCCESSFUL`.
  - **D5** — `RWDIAG-OK ticki=1226 npc=1896` po 61 s merjenja.
  - **D6** — trije posnetki v `dev\run\logs\rwdiag\`, vsak v `.txt` in `.json`, nobeden prazen.
  - **D7** — pred vklopom `RWDIAG stanje=off ticki=0`; po `rwdiag off` sta dva posnetka v
    razmiku 10 s pokazala **enaka** števca (1246 / 1896), kar dokazuje, da je zbiralnik res
    odjavljen z event busa in ne le tiho.
- Da se ročno tipkanje v gradle konzolo ne ponovi (15. 9. je dvakrat tiho odpovedalo), je
  nastala skripta **`rwdiag-run.ps1`** — isti mehanizem kot `testworld-run.ps1`: požene
  server, pošlje `rwdiag status|on|dump|off`, primerja števce pred in po izklopu, preveri
  zapisane posnetke, ustavi server in izpiše prvo tabelo. Izhodna koda 0 = D4–D7 zelena.
- **Prva številka in prvo presenečenje.** MSPT p50 = 0,16 ms, p95 = 1,21 ms — pri osmih
  NPC-jih je server prazen. Toda `npc.per.tick` ima **p50 = 0** in je binaren (0 ali 8):
  NPC-ji tikajo le v približno vsakem petem ticku, ne v vsakem. To ni napaka števca, ampak
  lastnost sveta brez igralca; razčistiti jo je treba **pred** M2.4, sicer bodo merilni
  scenariji merili nekaj drugega, kot mislimo. Podrobnosti, kandidati in naslednji korak:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).
- Najdaljši tick 74,5 ms sovpada z autosave. M2.5 mora autosave tick izločiti ali poročati
  posebej, sicer bo p99 vedno meril shranjevanje in ne AI.

**M2.1c — instrumentacija za razčiščenje `npc.per.tick` (isti dan, 12:2x):**

Da odgovor ne bo ugibanje, je zbiralnik dobil pet novih velicin, vse še vedno samo na Forge
dogodkih in samo dokler je merjenje vklopljeno:

- `npc.tick.gap` — razmik v tickih med dvema tickoma z NPC-ji. Enakomeren razmik pomeni
  sistematiko, razmetan pomeni chunk loading.
- `world.tick` — server ticki po svetovih, vsota čez dimenzije.
- `world.entities.loaded`, `world.npc.loaded`, `world.npc.killed`, `world.players` —
  vzorčenje `loadedEntityList` in `playerEntities` enkrat na sekundo, sešteto čez vse svetove.

`world.npc.killed` je tam zaradi pasti iz M0.6: ubit CustomNPC ostane v svetu in čaka na
respawn. Če je meritev tekla nad mrtvimi NPC-ji, so številke o nečem drugem, kot mislimo.

Uporabljeni so samo tisti deli MC API-ja, ki so potrjeni v `reference-src`
(`loadedEntityList` in `playerEntities` v `NPCSpawning`, `isKilled()` v
`EntityNPCInterface`); `addedToChunk` in `provider.getDimension()` sta bila namenoma
izpuščena, ker ju v dekompilatu ni bilo mogoče potrditi, prevesti pa jih seja ne more.
Jedro paketa se je v seji znova prevedlo z `javac --release 8` in 23 testov je zelenih.

**Druga meritev (12:32) je vprašanje razčistila do polovice.** Novi števci so izključili dva
kandidata od treh in pokazali nekaj, česar prva meritev ni mogla: `npc.tick.gap` ima 246
vzorcev in **vsi so 1**, skupaj 1976 = 8 × 247. NPC-ji so torej tikali v **247 zaporednih
tickih in nato nikoli več** — ne "vsak peti tick", ampak en strnjen blok na začetku.
`world.npc.loaded` je 62 vzorcev zapored točno 8, `world.npc.killed` 0, `world.tick` 1229
pri 1228 server tickih, `world.players` 0.

**Ugotovitev: približno 14 sekund po nalaganju sveta vanilla neha posodabljati entitete,
ker v svetu ni igralca.** Mod s tem nima nič. To pomeni, da sta obe dosedanji meritvi merili
prvih ~14 s dogajanja in nato prazen tek — in razloži, zakaj je MSPT tako nizek.

Ostaneta dva mehanizma: (1) `updateEntityWithOptionalForce` preskoči posodobitev, ker
območje 32 blokov okoli entitete ni naloženo — takrat Forge posije `EntityEvent.CanUpdate`;
(2) zanka `updateEntities` do entitet sploh ne pride. Zato je dodan števec
**`npc.update.blocked`**, ki šteje `EntityEvent.CanUpdate` za NPC-je in dogodka ne
spreminja. Naslednji zagon loči mehanizma brez ugibanja.

**Posledica za M2.4/M2.6, ki velja ne glede na izid:** vsak merilni scenarij mora
eksplicitno določiti, ali je v svetu igralec, ali pa morajo biti chunki z NPC-ji prisilno
naloženi (`ForgeChunkManager`). Dokler to ni urejeno, nobena številka iz M2 ni primerljiva
s produkcijskim strežnikom. Podrobnosti:
[`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Naslednja seja:** pognati `.\rwdiag-run.ps1` s števcem `npc.update.blocked` in dokončati
odgovor, nato urediti pogoj meritve (igralec ali prisilno naloženi chunki) in šele nato M2.2
(reprodukcija R1). Uporabnik je potrdil, da sta pri R1 **oba NPC-ja CustomNPC** (jahač in
nosilec), zato reprodukcija ne potrebuje vanilla konja.

---

### 2026-09-15 (14) — M0.6 zaključen: skriptiran scenarij testnega sveta

**Paket:** M0.6
**Stanje:** končano

**Narejeno:**

- `testworld-run.ps1` požene cel scenarij brez človeka: trije server zagoni, ukazi na
  standardni vhod, izpis v `audit/m06-testworld-{a,b,c}.log`, na koncu `verify-testworld.ps1`
  nad logoma B in C. Mehanizem je prevzet iz `smoke-server.ps1` (M0.5).
- **Izid: vse preverbe zelene.** Zagon B `32 entities deleted` in `TW-OK` 8×; zagon C po
  restartu `TW-OK` 8×, `TW-SCRIPT-OK` 1×, `TW-SCRIPT-TICK-10` 1×. Nobene `ERROR` vrstice iz
  `noppes.*`, nobenega `script errored`. Scenarij je tekel nad svetom, onesnaženim z 32
  NPC-ji iz neuspelih poskusov, in ga je sam počistil — s tem je dokazana idempotentnost.
- Merila W1–W8 veljajo; M0.6 je izhodni pogoj za M2 in je izpolnjen.

**Ni narejeno in zakaj:**

- Quest in dialog fixture ostajata v M0.7 — obojega ni mogoče sestaviti brez GUI-ja,
  ugibanje NBT strukture pa protokol prepoveduje.
- Igralec se še ni povezal v testni svet; `PlayerData.save` (B1/B2 v realnem obratovanju)
  zato še ni pokrit. Prav tako M0.7.

**Ugotovitve — štiri pasti, vsaka je stala en neuspel zagon:**

1. **Ročno lepljenje ukazov v gradle konzolo je nezanesljivo.** Prvi poskus je izgledal kot
   okvara testnega sveta: `noppes clone list 1` je izpisal vseh 8 imen, a
   `execute @e[tag=testworld]` je javil `found nothing`. V logu ni bilo **nobenega**
   `gamerule`, `fill` ali `clone spawn` — vsebina `setup-commands.txt` ni nikoli prišla do
   serverja. Odslej vsak scenarij pošilja ukaze skriptirano.
2. **`runServer` je lahko `UP-TO-DATE` in se sploh ne zažene.** ForgeGradle prijavi `dev/run`
   kot izhod naloge; če se med dvema zagonoma tam nič ne spremeni, Gradle nalogo preskoči,
   build se konča v 20 s in skripta čaka na marker do timeouta. Popravljeno v
   `dev/build.gradle` z `outputs.upToDateWhen { false }` za `runClient` in `runServer`.
   Velja tudi za `smoke-server.ps1`.
3. **`/kill` CustomNPC-ja ne odstrani, ampak ga respawna.** `DataStats` ima privzeto
   `RespawnTime = 20` in `SpawnCycle = 0` (`:30-31`); ubit NPC se shrani kot mrtev in ob
   nalaganju sveta oživi. Po dveh poskusih je bilo v svetu 24 in nato 32 NPC-jev, čeprav je
   `kill` vsakič javil, da jih je pobil. Pravi ukaz je `noppes slay npcs` →
   `entity.isDead = true` (`CmdSlay.java:140`). **Pomembno za M2**, kjer je število NPC-jev
   merjena količina.
4. **Zapis datoteke na uporabnikov disk je lahko tiho neuspešen.** Dvakrat je orodje javilo
   uspešen zapis, na disku pa je ostala prejšnja verzija (`setup-commands.txt` 1321 B namesto
   1654 B, `testworld-run.ps1` 9828 B namesto 10400 B), zato je zagon tekel s staro logiko in
   izgledal kot nova napaka v modu. Odslej seja po vsakem zapisu preveri velikost datoteke,
   preden zahteva zagon.

Poleg tega: `fill` javi `No blocks filled`, kadar so bloki že na mestu — izpis zgleda kot
odpoved, pa ni. In `EntityNPCInterface` kliče `EventHooks.onNPCTick` vsak 10. tick (`:358`),
torej je script tick 2 Hz na NPC; vsi ti klici gredo skozi en statičen `lock` v
`ScriptContainer.run`, kar je vhodni podatek za M5.

**Spremembe obnašanja:** v modu nobene. `dev/build.gradle` je dobil
`outputs.upToDateWhen { false }` za `runClient` in `runServer`; to je razvojno orodje, ne
del zapakiranega moda, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** M0.7 — integracijska matrika. Prvi korak zahteva uporabnika: v GUI-ju
narediti en quest in en dialog, pripeti dialog na fixture NPC-ja, nato se
`world/customnpcs/quests/` in `dialogs/` posname v `dev/testworld/`.

---

### 2026-09-15 (13) — M0.6: seme testnega sveta in samodejno ovrednotenje

**Paket:** M0.6
**Stanje:** delno — vse datoteke so pripravljene in preverjene proti izvorni kodi;
scenarij še ni bil pognan, ker seja ni mogla zagnati ukazov na uporabnikovem računalniku.

**Narejeno:**

- Pregled semena, ki je nastalo ob koncu prejšnje seje (`dev/testworld/`, `testworld.ps1`,
  `docs/scenariji/M0.6-testni-svet.md`). Prejšnja seja jih ni ne commitala ne zabeležila
  v ta dnevnik; to je bila odprta luknja in je zdaj zaprta.
- **Verifikacija semena proti izvorni kodi** (ne proti spominu):
  - `CmdClone.spawn` ima podpis `<name> <tab> [[world:]x,y,z] [newname]` —
    `setup-commands.txt` ga uporablja pravilno (`CmdClone.java:112`).
  - `CmdClone.grid` ima `<name> <tab> <lenght> <width> [[world:]x,y,z]` — kot je navedeno
    v scenariju za M2.4 (`CmdClone.java:173`).
  - `DataScript.readFromNBT` bere `Scripts`, `ScriptLanguage` in `ScriptEnabled` iz
    **korenskega** compounda entitete; `T_Scripted.json` ima vse tri na pravem mestu.
  - `ScriptContainer.readFromNBT` bere `Script`, `Console`, `ScriptList` — struktura v
    fixture datoteki se ujema.
  - Vseh 8 fixture datotek se ujema s tabelo v `dev/testworld/README.md`
    (`MovementType`, `MovingState`, `Role`, `Invulnerable`, `PersistenceRequired`, tagi).
- **Popravljeno merilo W5.** Prvotno merilo je bilo "`T_Scripted` ne vrže script napake",
  kar je lažno pozitivno, če scripting sploh ne teče. `DataScript.isEnabled()` zahteva
  `ScriptEnabled` **in** `ScriptController.HasStart` **in** `CustomNpcs.EnableScripting`
  (`CustomNpcs.java:271` postavi `HasStart=false` ob load, `:293` na `true` šele ob
  `FMLServerStartedEvent`). Merilo zdaj zahteva pozitiven izpis iz same skripte.
- Skripta v `T_Scripted` zato ob `init` požene `/say TW-SCRIPT-OK` in ob 200. `tick`
  `/say TW-SCRIPT-TICK-200`. Pot je `NPCWrapper.executeCommand` →
  `NoppesUtilServer.runCommand`, ki zahteva vklopljene command bloke; seme jih ima
  (`enable-command-block=true`). Izhod `/say` pride v konzolo tudi brez igralca.
- Nov `verify-testworld.ps1` samodejno ovrednoti W1–W8 iz `dev/run/logs/latest.log` in
  datotečnega sistema ter vrne izhodno kodo 0/1. S tem scenarij ni več odvisen od ročnega
  branja loga in ga lahko katerakoli seja ponovi enako.
- Scenarij in `dev/testworld/README.md` posodobljena; postopek ima zdaj eksplicitno fazo
  restarta za W6.

**Ni narejeno in zakaj:**

- Scenarij ni pognan iz seje. Windows posodobitev z 8. 9. je onemogočila, da bi seja
  dosegla uporabnikov datotečni sistem prek lupine; seja lahko datoteke bere in piše, ne
  pa izvajati gradle, server ali git. Zagon je naredil uporabnik.
- Commit in push je naredil uporabnik. Push je bil najprej zavrnjen: na `origin/main` sta
  bila commita `M0.2r` in `M0.5` z **druge delovne postaje**, ki ju ta postaja ni imela.
  Delo je bilo združeno brez force-pusha; oba M0.5 zapisa (ročni z 11. 9. in skriptirani
  s 14. 9.) sta ohranjena, dnevnik pa preštevilčen, ker sta obe veji uporabili številko (10).
- W6 in W7 sta preverljiva šele po dejanskem zagonu; do takrat M0.6 ostane *delno*.

**Ugotovitve:**

- `ScriptContainer.run` sinhronizira na **statičnem** `lock` objektu, torej si vse skripte
  vseh NPC-jev delijo eno ključavnico. To je konkreten dokaz za kandidata iz M5
  (odstranitev globalnega script locka) in za R7; zabeleženo, ne popravljeno.
- `ScriptContainer` ob napaki pokliče `NoppesUtilServer.NotifyOPs(... " script errored")`,
  kar je iskalni niz za negativno stran W5.
- Prejšnja seja je fixture NPC-je generirala iz pravega NPC zapisa (`ModRev 18`, polna
  lista atributov z `generic.flyingSpeed`), ne iz ugibanja. Pravilo iz
  `dev/testworld/README.md` je torej spoštovano; edina ročna sprememba v tej seji je
  vsebina niza `Script`, kar ni sprememba strukture NBT.

**Spremembe obnašanja:** nobene — spremenjeno je le testno seme in dokumentacija; v
`src/patch/java` ni sprememb, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene

**Odprto:** projekt zdaj teče na **dveh delovnih postajah**. Vsaka seja mora pred delom
pognati `git fetch origin` in preveriti, ali je `origin/main` pred njo; sicer se dnevnik
razide, kot se je tokrat.

**Naslednja seja:** zapisati rezultat W1–W8 iz uporabnikovega zagona; če je PASS, zapreti
M0.6 in odpreti M0.7 (integracijska matrika + quest/dialog fixture iz GUI-ja).

---

### 2026-09-14 (12) — M0.5: dedicated-server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- `dev/build.gradle` poveže `runServer.standardInput = System.in`. Gradlov `JavaExec`
  privzeto poda prazen vhod, zato je bila konzola dev serverja doslej mrtva in serverja
  ni bilo mogoče niti čisto ustaviti z `stop`. Deluje samo z `--no-daemon`.
- Dodan `smoke-server.ps1`: skriptiran, ponovljiv smoke test s 13 preverbami. Svet
  `m05-smoke` se vsakič zbriše in ustvari na novo (FLAT, seed 20260914, brez spawna mobov,
  `max-tick-time=-1`), ukazi gredo na standardni vhod, izpisi v `audit\m05-server-*.log`.
- Postopek zapisan kot ponovljiv scenarij v `docs/scenariji/M0.5-server-smoke.md`.
- Izid: vseh 13 preverb zeleno. Server naloži 5 modov, `Done (1.337s)`, NPC se ustvari,
  preživi `save-all flush` in restart, po restartu konzola izpiše `[SmokeNPC] M05-NPC-PRESENT`.
- Minecraft EULA je bila sprejeta na izrecno zahtevo uporabnika; skripta je zapiše samo
  z zastavico `-AcceptEula`.

**Ni narejeno in zakaj:**

- Igralec v svetu ni bil uporabljen; NPC je ustvarjen z `summon` in NBT, ne prek GUI-ja.
  Preverjen je en NPC in en NBT ključ, ne celotno stanje NPC-ja. To je obseg M0.7.
- M0.6 (testni svet z znanimi NPC-ji, questi, dialogi in skriptami) ostaja ločen paket;
  M0.5 mu je pripravil determinističen svet in spawn.

**Ugotovitve:**

- `CommandSummon` v 1.12.2 javi `Cannot summon the object out of the world` tudi takrat,
  ko **ciljni chunk ni naložen**, ne samo pri neveljavnih koordinatah. Spawn superflat
  sveta lahko pade do 256 blokov od izhodišča, zato chunk `0,0` ni nujno med spawn chunki.
  Zato ima test tri zagone: prvi pribije world spawn na `0 5 0`.
- `execute @e[…] ~ ~ ~ say` je boljša preverba kot `testfor`, ker konzola izpiše ime NPC-ja
  v oklepaju. En marker dokaže troje: entiteta obstaja, je pravega tipa in NBT ključ `Name`
  je preživel round-trip skozi popravljeno save pot.
- `save-all` v 1.12.2 izpiše `Saved the world`, ne `Saved the game`. Prvi osnutek skripte je
  čakal na napačen niz in 180 s lovil timeout.

**Spremembe obnašanja:** nobene v modu. Dev server zdaj bere konzolo; to je razvojno orodje,
ne del zapakiranega moda.

**Meritve:** nobene

**Naslednja seja:** M0.6 — testni svet z znanimi NPC-ji, questi, dialogi in skriptami, na
podlagi determinističnega sveta iz M0.5.

---

### 2026-09-14 (11) — M0.2r: obnova razvojnega okolja na novi delovni postaji

**Paket:** M0.2r (neplaniran, blokiral je vse ostalo)
**Stanje:** končano

**Narejeno:**

- Projekt je bil prestavljen na drug računalnik. `dev/libs`, `dev/baseline` in `.tools` so v
  `.gitignore`, zato na novi postavitvi **ni bilo mapiranega originala** in nič se ni prevajalo.
- `dev/build.gradle` je dobil task `exportMappedOriginal` (aktiven samo z `-PremapOriginal`),
  ki regenerira `dev/libs/customnpcs-mapped-01Oct19.jar` iz SHA-256-preverjenega originala po
  isti ForgeGradle deobf poti kot prvotna postavitev.
- `obnovi-okolje.ps1` preveri JDK 8 in hashe, nato požene `setupDecompWorkspace`, remap,
  `testOriginal` + `test`, `buildPatchedMod` in `verify-package.ps1`. Gradle koraki najprej
  poskusijo `--offline` in ob neuspehu ponovijo z omrežjem.
- Rezultat: `testOriginal` 31, `test` 47, build zapakira 42 razredov, `verify-package.ps1`
  pregleda 1716 vnosov in najde 25 pričakovanih zamenjav — **enako kot pred selitvijo**.

**Ni narejeno in zakaj:**

- `downloads\forge-1.12.2-14.23.5.2847-mdk.zip` na tej postaji ne obstaja in ni bil obnovljen;
  Gradle cache ima vse potrebno, zato paket ni blokiran. Vnos v locku ostane kot zapis izvora.

**Ugotovitve:**

- Tri napake v prvem osnutku remap taska, vse potrjene z izvedbo, ne z branjem:
  1. `deobfCompile files(...)` ForgeGradle 2.3 zavrne — zahteva `ExternalModuleDependency`.
  2. `flatDir` ignorira group in odgovori tudi na ForgeGradlovo lastno poizvedbo
     `deobf.<group>:<ime>:<verzija>`, zato vrne **neremapiran original** namesto deobf
     rezultata in remap se sploh ne zgodi. Rešitev je `ivy` repozitorij z `[organisation]`
     v vzorcu.
  3. Remap sproži šele `deobfCompileDummyTask`; brez te odvisnosti task ne naredi ničesar.
- `$ErrorActionPreference = 'Stop'` v PowerShellu spremeni **vsako vrstico stderr** zunanjega
  programa v terminating error. Že `java -version` je prekinil skripto. Native klici zato
  tečejo z `Continue`, uspeh pa se presoja po `$LASTEXITCODE`.
- Regeneriran jar **ni bitno enak** hashu v `environment-lock.json`, vsebinsko pa je enak:
  1716 vnosov se ujema po imenu, vseh **815 resourcev je bitno enakih**, remapiranih je 571
  razredov. Deobf izhod ForgeGradla torej ni bitno ponovljiv med postavitvami. Lock je
  posodobljen; razlog je zapisan kot odločitev **D-010**.

**Spremembe obnašanja:** nobene. Normalen build je nespremenjen; remap koda se naloži samo
z `-PremapOriginal`.

**Meritve:** nobene

**Naslednja seja:** M0.5.

---

### 2026-09-11 (10) — M0.5: dedicated server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- Scenarij je zapisan kot ponovljiv postopek v [`scenariji/M0.5-server-smoke.md`](scenariji/M0.5-server-smoke.md)
  z devetimi merili sprejemljivosti, tako da ga lahko katerakoli kasnejša seja ponovi.
- Dedicated server se zažene iz popravljenega drevesa: Forge 14.23.5.2847, Java 1.8.0_492,
  5 modov naloženih, `Done (12.031s)` ob prvem in `Done (2.077s)` ob ponovnem zagonu.
- NPC `SmokeM05` (`customnpcs:CustomNpc`) spawnan na `(0,72,0)`, dodan v clone tab 1,
  nato `save-all flush` in `stop`.
- Po restartu: `M05-SURVIVED` 1× in `M05-TAG-OK` 1× — entiteta **in** vanilla `Tags`
  so preživeli serializacijo. `noppes clone list 1` še vedno vsebuje `SmokeM05`,
  `noppes clone spawn` uspe in `M05-AFTER-CLONE-SPAWN` se pojavi 2×.
- `world/customnpcs/clones/1/SmokeM05.json` (7399 B) je na disku brez ostankov `.tmp` ali
  `.bak`; vsebina ima pričakovane ključe `Name`, `Tags`, `Invulnerable`, `NoGravity`.
  To je prva potrditev popravka M1.5a na dejanskem strežniku, ne samo v testu.
- Oba `stop`-a sta čista: `Saving players` → `Saving worlds` → `Unloading dimension 0`
  brez izjeme; drain world seje iz M1.6 ni javil nečistega zaključka.
- V logu ni nobene napake iz `noppes.*`. Edina `ERROR` vnosa sta okoljska in prisotna tudi
  pri originalu (maven library folder, manjkajoči FML podpisi).

**Ni narejeno in zakaj:**

- Pot ob prijavi igralca (`PlayerData.save`, torej B1 in B2 v realnem obratovanju) ni pokrita,
  ker se noben klient ni povezal. Spada v M0.6/M0.7, kjer bo testni svet imel tudi igralca.
- Git commit te seje ni bil narejen iz seje; razlog je nova omejitev orodij spodaj.

**Ugotovitve:**

- Ukazi iz server konzole se izvajajo na `(0,0,0)`, ne na world spawnu `(132,64,-32)`,
  `/noppes clone add` pa išče NPC-je v radiju 80 blokov od pošiljatelja. Vsak kasnejši
  konzolni scenarij mora entitete postaviti blizu izhodišča ali uporabiti klienta.
- V shranjenem clone JSON je `"id": "customnpcs:customnpc"` z malimi črkami, registry ime
  entitete pa je `customnpcs:CustomNpc`. Za `/summon` in `@e[type=...]` je obvezna oblika
  z velikimi črkami. Zaenkrat neškodljivo, a kandidat za preverbo pri M8.
- Clone datoteke niso strog JSON (`1b`, `1.0f`). To je originalni format, ki ga `NbtJson`
  namerno ohranja; strog JSON parser jih ne prebere in to ni napaka.

**Spremembe obnašanja:** nobene

**Meritve:** nobene; zagon 12,0 s oziroma 2,1 s je informativen podatek, ne meritev po protokolu.

**Naslednja seja:** M0.6 — ponovljiv testni svet z znanimi NPC-ji, dialogi in skriptami.

---

### 2026-09-11 (9) — M1.9: fault injection varnih zapisov

**Paket:** M1.9
**Stanje:** končano

**Narejeno:**

- `SafeFileWriter` je dobil package-private datotečno mejo za deterministično fault injection;
  javni API in produkcijska pot še vedno uporabljata neposredno `java.nio.file.Files` implementacijo.
- Dodanih je 9 scenarijev: prekinitev med pisanjem pred zamenjavo, zaklenjen/zavrnjen cilj,
  zavrnjen dostop do imenika, nepodprt atomski premik, odpoved fallback namestitve, prekinitev
  po fallback zamenjavi, odpoved same obnove, pokvarjen JSON kandidat in restart z orphaned `.bak`.
- Pri vsaki odpovedi test zahteva, da ostane zadnja verzija na cilju ali kot eksplicitno
  obnovljiv `.bak`; začasne `.tmp` datoteke se ne puščajo.
- Normalna atomska zamenjava je ponovno uspešno tekla na dejanskem Windows datotečnem sistemu;
  redke OS veje uporabljajo deterministično vržene standardne Java NIO izjeme.
- Vseh 31 primerjalnih testov ter 16 testov writerjev, session lifecyclea in fault injection
  je zelenih v Javi 8.
- Build zapakira 42 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- M1.4 in M1.8 ostajata zavestno odložena brez konkretne poškodovane uporabnikove datoteke.
- M1.7 ni potreben, ker se save format ni spremenil.

**Ugotovitve:**

- Če odpovesta tako namestitev kot obnova cilja, `SafeFileWriter` pusti staro vsebino v `.bak`
  in pripne napako obnove kot suppressed exception; podatki so še vedno obnovljivi.
- Orphaned backup se ne prepiše samodejno. Writer zavrne nov zapis, da ohrani zadnjo znano
  obnovljivo verzijo za pregled namesto ugibanja.

**Spremembe obnašanja:** nobene; dodana testna meja ni del javnega API-ja.

**Meritve:** nobene

**Naslednja seja:** M0.5 dedicated-server smoke, nato M0.6 testni svet kot vhod za M2.

---

### 2026-09-11 (8) — M1.6: lifecycle asinhronih world zapisov

**Paket:** M1.6
**Stanje:** končano

**Narejeno:**

- `CustomNpcs` je bil prenesen v prevedljivo drevo in shranjen v ločenem baseline commitu;
  popravljeni so bili samo trije generični artefakti dekompilacije.
- Dodan `rework/data/WorldSaveSession`: ločen enonitni executor za vsako server-world sejo,
  z absolutnim rootom sveta, zajetim ob `FMLServerAboutToStartEvent`.
- `PlayerData.save` ob zahtevi zajame NBT snapshot in ime cilja ter ga preda aktivni seji;
  ne izračunava več poti znotraj pozno izvedene naloge na globalnem schedulerju.
- Ob `FMLServerStoppedEvent` seja najprej preneha sprejemati zapise in izprazni vrsto, šele
  nato se `CustomNpcs.Server` nastavi na `null`. Nečist drain je eksplicitno zabeležen kot napaka.
- Če save izjemoma nastane brez aktivne server seje, se pot zajame in zapis izvede sinhrono;
  podatki se ne odložijo na neupravljano globalno vrsto.
- Testi pokrijejo svet A → drain → svet B z istim imenom igralca, zavrnitev poznega zapisa ter
  propagacijo I/O napake v rezultat draina. Primerjalni bytecode test potrdi staro in novo vezavo.
- Vseh 31 primerjalnih testov ter 7 testov varnih writerjev/session je zelenih v Javi 8.
- Build zapakira 40 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Zaklenjen cilj, zavrnjen dostop in procesne prekinitve spadajo v M1.9 fault injection.
- Splošni `CustomNPCsScheduler` ni ugasnjen, ker ga uporabljajo klient, paketni sendi, GUI zamiki
  in `BankData`; slednji je ločen bug B4 in se ne sme spreminjati v tem paketu.

**Ugotovitve:**

- Edini pregledani datotečni zapis na globalnem schedulerju je bil `PlayerData.save`; drugi
  uporabniki izvajajo omrežne ali GUI naloge, zato bi globalni shutdown povzročil regresije.

**Spremembe obnašanja:** pending player zapisi se ob ustavitvi izpraznijo pred resetom serverja;
zapis starega sveta ne more uporabiti poti novega sveta.

**Meritve:** nobene

**Naslednja seja:** M1.9, fault injection in zaključni kriteriji integritete podatkov.

---

### 2026-09-11 (7) — M1.5d: varen zapis stisnjenega NBT

**Paket:** M1.5 (četrti del)
**Stanje:** končano

**Narejeno:**

- `BankController`, `FactionController`, `GlobalDataController`, `TransportController`,
  `RecipeController`, `SpawnController`, klientov `PresetController` in `SchematicController`
  so bili vsak preneseni v prevedljivo drevo z ločenim baseline commitom.
- Dodan `rework/data/CompressedNbtFile`, ki stisnjeni NBT zapiše skozi `SafeFileWriter`, ga
  ponovno prebere in zahteva enak NBT pred atomsko zamenjavo cilja.
- Vseh osem controllerjev zdaj zapisuje neposredno na končno pot; odstranjena so zaporedja
  `.dat_new` → brisanje/preimenovanje in neposredno prepisovanje schematic datoteke.
- Regresijski test je pred popravkom dokazano padel, po popravku pa bytecode vseh controllerjev
  potrdi odsotnost nevarnih suffixov oziroma uporabo novega varnega writerja.
- Vseh 29 primerjalnih testov ter 5 testov varnih writerjev je zelenih v Javi 8.
- Build zapakira 32 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 20 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Lifecycle asinhronih player zapisov je ločen problem B2 in ostaja M1.6.
- Širši fault-injection scenariji (zaklenjen cilj, zavrnjen dostop, prekinitev procesa) so M1.9.

**Ugotovitve:**

- `CompressedStreamTools.writeCompressed` zapre podani stream. `CompressedNbtFile` zato uporabi
  non-closing ovoj, da lahko `SafeFileWriter` po koncu stiskanja še izvede `flush` in disk `sync`.
- Prvotni seznam šestih world controllerjev ni bil popoln; isti problem sta imela tudi klientov
  preset zapis in neposreden schematic zapis, zato sta vključena v isti paket.

**Spremembe obnašanja:** banke, factioni, globalni podatki, transporti, recepti, spawni,
klientovi preseti in schematiki ob neuspelem zapisu ohranijo zadnjo veljavno datoteko.

**Meritve:** nobene

**Naslednja seja:** M1.6, upravljan lifecycle asinhronih zapisov.

---

### 2026-09-11 (6) — M1.5c: varen zapis sinhronih JSON datotek

**Paket:** M1.5 (tretji del)
**Stanje:** končano

**Narejeno:**

- `DialogController`, `QuestController`, `LinkedNpcController` in `RoleTrader` so bili vsak
  preneseni v prevedljivo drevo in shranjeni v ločenem baseline commitu pred spremembo vedenja.
- Pri `RoleTrader` so bili odstranjeni samo dekompilatorski casti `Object`, ki se z generičnim
  `ItemStack` seznamom niso prevedli; runtime vedenje baselinea se s tem ne spremeni.
- Vsi štirje zapisi zdaj podajo končno `.json` pot neposredno v `NBTJsonUtil.SaveFile`.
  Odstranjeno je zunanje zaporedje `_new` → izbriši cilj → nepreverjen `renameTo`.
- Novi regresijski test pregleda bytecode konstantne nize: proti originalu potrdi stari nevarni
  suffix, proti popravljeni kodi pa zahteva njegovo odsotnost. Pred popravkom je test dokazano padel.
- Vseh 27 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 20 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 12 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Banke, factioni, globalni podatki, transporti, recepti in spawni uporabljajo stisnjeni NBT;
  spadajo v M1.5d in zahtevajo svoj validator za binarni format.
- Lifecycle asinhronih player zapisov ostaja M1.6.

**Ugotovitve:**

- `ScriptController` že zapisuje neposredno na končne poti, zato je postal varen z globalnim
  popravkom `NBTJsonUtil.SaveFile` iz M1.5a in ne potrebuje lastnega prenosa v tem paketu.

**Spremembe obnašanja:** dialogi, questi, linked NPC podatki in trader marketi ohranijo zadnjo
veljavno JSON datoteko, če nov zapis ali njegova validacija odpove.

**Meritve:** nobene

**Naslednja seja:** M1.5d, varen zapis controllerjev s stisnjenim NBT.

---

### 2026-09-11 (5) — M1.5b: varen zapis PlayerData

**Paket:** M1.5 (drugi del)
**Stanje:** končano

**Narejeno:**

- `PlayerData` prenesen v prevedljivo drevo brez funkcionalnih sprememb in shranjen v ločenem
  baseline commitu.
- Dodan karakterizacijski test privzetega `PlayerData` NBT snapshota; zelen je proti originalu
  in obnovljenemu razredu.
- Asinhroni zapis igralca ne uporablja več `_new` → izbriši cilj → nepreverjen `renameTo`,
  ampak ciljno JSON datoteko preda `NBTJsonUtil.SaveFile` oziroma `SafeFileWriter`.
- Vseh 22 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 15 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 7 dovoljenih zamenjav.

**Ni narejeno in zakaj:**

- Zajem poti/sveta in zaključevanje async vrste ostajata nespremenjena; to je M1.6, ne B1.
- Drugi JSON in stisnjeni NBT controllerji ostajajo M1.5c/d.

**Ugotovitve:**

- `PlayerData` povezuje B1 in B2, zato sta bila atomski zapis in async lifecycle namenoma
  ločena v zaporedna paketa.

**Spremembe obnašanja:** veljaven player JSON se ne izbriše pred uspešnim zapisom in validacijo novega.

**Meritve:** nobene

**Naslednja seja:** M1.5c, sinhroni JSON controllerji.

---

### 2026-09-11 (4) — M1.5a: varen zapis clone JSON

**Paket:** M1.5 (prvi del)
**Stanje:** delno

**Narejeno:**

- Dodan `rework/data/SafeFileWriter`: začasna datoteka v isti mapi, `flush` + disk `sync`,
  validacija pred zamenjavo, atomski `Files.move` in preverjen fallback z obnovitvenim `.bak`.
- `NBTJsonUtil.SaveFile` zdaj kandidat ponovno prebere in zahteva bitno enakovreden NBT,
  preden ga namesti na ciljno pot.
- `ServerCloneController` ne uporablja več nevarnega zaporedja izbriši-staro → `renameTo`;
  clone JSON zapiše neposredno skozi varni writer.
- Štirje novi testi pokrijejo prvi zapis, zamenjavo, zavrnjeno validacijo z ohranitvijo stare
  datoteke in osirotel recovery backup. 21 primerjalnih testov ostaja zelenih v obeh načinih.
- Build zapakira 14 razredov; `verify-package` je preveril 1716 originalnih vnosov in našel
  samo 6 dovoljenih zamenjav originalnih razredov.
- Po uporabnikovem pojasnilu je R9 označen kot minoren; M1.4 in M1.8 sta odložena, R9 migracija
  ni potrebna. Splošna B1/B2 zaščita ostaja, ker varuje vse podatke sveta.
- Dodan ignore za `*.log.gz`; pomotoma sledljiv Gradle log je odstranjen samo iz indeksa.

**Ni narejeno in zakaj:**

- Preostali JSON in stisnjeni-NBT controllerji še uporabljajo lastne `_new`/`_old` poti.
  Prenos vsakega originalnega razreda zahteva ločen preverjen baseline, zato ostanejo M1.5b.
- Polni fault-injection nabor (zaklenjena datoteka, prekinitev procesa, zavrnjen dostop) je
  še vedno M1.9.

**Ugotovitve:**

- Nova koda v originalnem testnem classpathu ne obstaja; `SafeFileWriterTest` zato teče samo
  pod `test`, medtem ko vseh 21 karakterizacijskih testov še vedno teče tudi pod `testOriginal`.

**Spremembe obnašanja:** clone JSON se namesti šele po trajnem zapisu in uspešni validaciji.

**Meritve:** nobene

**Naslednja seja:** M1.5b, preklop preostalih controllerjev na `SafeFileWriter`.

---

### 2026-09-11 (3) — M0.4: sledljivo git izhodišče

**Paket:** M0.4
**Stanje:** končano

**Narejeno:**

- Inicializiran git repozitorij na veji `main`; preverjena sta korenski in Gradlov
  `.gitignore`, zato lokalni JDK, odvisnosti, build izhodi, razvojni svetovi in logi niso
  vključeni v zgodovino.
- Na uporabnikovem računalniku pognan `.\dev.ps1 testOriginal test --offline`: vseh 21 testov
  je zelenih proti originalu in popravljeni kodi.
- Pognana `.\dev.ps1 buildPatchedMod --offline` in `.\verify-package.ps1`: build uspešen,
  zapakiranih je 8 obnovljenih razredov; preverjenih je 1716 originalnih vnosov, spremenjenih
  je samo 5 dovoljenih originalnih class datotek. Preostali 3 so novi `NbtJson` razredi.
- Ustvarjen prvi commit, ki predstavlja preverjeno izhodišče za nadaljnje pakete.

**Ni narejeno in zakaj:**

- M0.5–M0.8 ostajajo ločeni paketi; protokol dovoljuje en delovni paket na sejo.

**Ugotovitve:**

- Prejšnja omejitev, da lokalnih ukazov ni mogoče zaganjati, ne velja več.

**Spremembe obnašanja:** nobene

**Meritve:** nobene

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`).

---

### 2026-09-11 (2) — M1.1–M1.3: NBT↔JSON serializer

**Paket:** M1.1, M1.2, M1.3
**Stanje:** končano

**Narejeno:**

- Postavljeno oblačno prevajalno in testno okolje z **pravimi** projektnimi artefakti
  (mapiran CustomNPCs JAR, `forgeSrc-1.12.2-14.23.5.2847.jar`, guava 21.0, commons-io 2.5,
  log4j 2.8.1). S tem je bilo mogoče trditve preveriti z izvedbo, ne samo z branjem.
- **Empirično potrjenih 9 napak** v `NBTJsonUtil` in **ovrženi 2** prvotni hipotezi.
  Vse podrobnosti: [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md).
- Napisan nadomestni serializer `noppes/npcs/rework/data/NbtJson.java` in shim
  `noppes/npcs/util/NBTJsonUtil.java`, ki ohrani vse javne podpise originala.
- Napisana testa `NbtJsonBaselineTest` (17 testov) in `NbtJsonFuzzTest`. Oba tečeta v dveh
  načinih: pod `testOriginal` trdita originalno napačno obnašanje, pod `test` popravljeno.
  Ločevanje gre prek refleksijske preverbe polja `NBTJsonUtil.REWORK_SERIALIZER`.
- V oblaku zeleno v obeh načinih: 18/18 novih testov.

**Ugotovitve:**

- Diferencialni fuzz čez 4000 naključnih NBT struktur: **original izgubi ali spremeni podatke
  v 1598 primerih (40 %)**, novi v 0.
- **Zapisano besedilo je znak za znak enako originalnemu v vseh 4000 primerih.** Vse napake so
  bile v logiki pretvorbe, ne v formatu. Posledica: za R9 **ni potrebna migracija**, obstoječe
  datoteke se berejo takoj, in nespremenjen CustomNPCs zna brati datoteke, ki jih zapišemo mi.
- Najhujše potrjene napake: long array vrednosti prisekane na 8 bitov; `NBTTagList` bytov,
  intov ali longov se tiho spremeni v array tip (zato `getTagList` vrne prazno in nastavitev
  se "vrne nazaj"); prazen tipiziran array izgubi tip; nizi izgubijo vodilne presledke; niz,
  ki se konča z `\`, `NaN`, `Infinity`, prazen ključ in compound s >3000 ključi naredijo
  **celo datoteko neberljivo**.
- Hitrost na compoundu s 1500 ključi: zapis 235 ms → 2,9 ms, branje 2386 ms → 6,5 ms.
  Ker gredo skozi ta razred vse clone, dialog, quest, faction in player datoteke ob zagonu
  sveta, je to tudi prispevek k **R5**.
- Dve prvotni hipotezi sta bili napačni (byte/long array zapis; `SaveFile` ne zapre writerja).
  `javap` na originalnem bytecode ju je ovrgel. Pravilo iz `05-SEJA-PROTOKOL.md` se je
  izplačalo že prvič.

**Ni narejeno in zakaj:**

- Lokalni build in testi takrat še niso bili pognani; izvedeni so bili v naslednji seji
  M0.4 in so uspešni.
- M1.4–M1.9 ostajajo odprti.

**Spremembe obnašanja:** glej tabelo "Sprejete spremembe obnašanja".

**Meritve:** [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md)

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`), nato M1.4.

---

### 2026-09-11 (1) — Postavitev načrta reworka

**Narejeno:**

- Pregledano obstoječe okolje (`OKOLJE.md`, `PLAN_IMPLEMENTACIJE.md`, `dev/build.gradle`,
  `environment-lock.json`, `audit/package-verification.txt`)
- Prebrani ključni razredi v `dev/reference-src/`: `EntityNPCInterface`, `EntityNPCFlying`,
  `FlyingMoveHelper`, `EntityAIFollow`, `EntityAIAttackTarget`, `ServerCloneController`,
  `ScriptController`, `ScriptContainer`, `NBTJsonUtil`, `ItemMounter`, `EntityChairMount`
- Raziskana referenčna projekta: BetaZavr 1.12.2-Unofficial in KAMKEEL CustomNPC+ (1.7.10)
- Sprejete odločitve D-001 do D-007 (`01-ARHITEKTURA.md`)
- Napisana dokumentacija: `README.md`, `docs/01-ARHITEKTURA.md`, `docs/02-ZAHTEVE.md`,
  `docs/03-FAZE.md`, `docs/04-STANJE.md`, `docs/05-SEJA-PROTOKOL.md`

**Ključne ugotovitve iz kode (podrobno v `02-ZAHTEVE.md`):**

- **R7 in R5 sta povezana**: `ScriptContainer.java:51,142` ima **en globalen `static` lock za
  vse skripte vseh NPC-jev**. Najverjetnejše največje ozko grlo pri skriptanih NPC-jih.
- **R1**: `EntityNPCInterface` ne override-a nobene passenger metode; `updateHitbox()` se ob
  spremembi jahanja ne pokliče; `EntityAIFollow.java:70` teleportira jahača z mounta;
  `EntityAIAttackTarget.java:98` veže napadalni doseg na `npc.width`, kar pri majhnem hitboxu
  sili NPC-je v kup.
- **R4 in R7 sta isti problem**: strojno berljiva shema, tipi, validacija, hitra povratna
  zanka. Zato M7 gradi na M6.
- **CustomNPC+ ima R7, R4 in del R2 že implementirane**. Glavni referenčni vir.

**Naslednja seja:** začetek razvoja.

---

## Odprta vprašanja za uporabnika

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | odprto |
| Q2 | Kopija sveta s problematičnimi NPC-ji | M1.4 | zaprto — uporabnik je nima; paket odložen |
| Q3 | Konkretna pokvarjena clone JSON datoteka | M1.4 | zaprto — ne obstaja; paket odložen |
| Q4 | Katera nastavitev se vrne nazaj? | R9 | odgovorjeno — follower role, action `waiting` se po clone lahko vrne v `following`; minorno |
| Q5 | Pri R1 — jahač in nosilec sta oba CustomNPC, ali je eden vanilla mob (konj)? | M2.2 | **odgovorjeno 15. 9.** — oba sta CustomNPC |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | odprto |
| Q7 | Pri R3 — katerih 5–8 funkcij CustomNPC+ je najbolj pomembnih? | M8.2 | odprto — najprej katalog |
| Q8 | Pri R8 — kateri provider (Anthropic / OpenAI / lokalni model)? | M9.3 | odprto |
| Q9 | Koliko NPC-jev je "veliko" v tvojem primeru? 100? 500? 2000? | M2.4, cilj za M5 | **odgovorjeno 15. 9.** — cilj še ni določen; merimo 50/200/500 in se odločimo po podatkih |
| Q10 | Ali strežnik, kjer to teče, sploh ima izhodni internetni dostop? | M9.1 | odprto |
| Q11 | Zakaj skupina obstane pred **drugo** stopnico (z = 36), prvo (z = 20) pa prestopi? | M2.3/M2.4, kakovost scenarijev | **zaprto 17. 9.** — ni bila ovira, ampak domet iskanja poti; glej Znane omejitve |

---

## Prevzeta koda

| Datum | Kaj | Izvor | Licenca | Kje v našem drevesu |
|---|---|---|---|---|
| — | — | — | — | — |

Nič prevzetega. `NbtJson` je napisan na novo; format posnema original, koda ne.

---

## Sprejete spremembe obnašanja

| Datum | Sprememba | Zahteva / bug | Stikalo | Privzeto |
|---|---|---|---|---|
| 2026-09-11 | Long array vrednosti se berejo z `getLong()` namesto `getByte()` | R9-a | ne | popravljeno |
| 2026-09-11 | Prazen `[B;]`/`[I;]`/`[L;]` obdrži svoj tip namesto da postane prazen seznam | R9-b2 | ne | popravljeno |
| 2026-09-11 | `NBTTagList` bytov/intov/longov se ne pretvori več v array tip | R9-b | ne | popravljeno |
| 2026-09-11 | Nizi obdržijo vodilne presledke in prelome vrstic | R9-d | ne | popravljeno |
| 2026-09-11 | Niz, ki se konča z `\`, `NaN`, `Infinity` in prazen ključ se preberejo namesto da vržejo | R9-e, e2, e3 | ne | popravljeno |
| 2026-09-11 | Parser ne rekurzira več na ključ; namesto `StackOverflowError` je omejitev globine 512 z jasno napako | R9-e4 | ne | popravljeno |
| 2026-09-11 | Prazen ključ se zapiše kot `"": vrednost` namesto da se izpusti | R9-e3 | ne | popravljeno |
| 2026-09-11 | `SaveFile` po pisanju eksplicitno flusha | — | ne | popravljeno |
| 2026-09-11 | Clone JSON se zapiše, sinhronizira in validira pred atomsko zamenjavo | B1 / R9-f | ne | varni zapis |
| 2026-09-11 | Player JSON se zapiše in validira pred zamenjavo; stari ostane ob napaki | B1 | ne | varni zapis |
| 2026-09-11 | Dialog, quest, linked NPC in trader market JSON se zapišejo neposredno skozi validirano atomsko zamenjavo | B1 | ne | varni zapis |
| 2026-09-11 | World controllerji, klientovi preseti in schematiki uporabljajo validiran atomski zapis stisnjenega NBT | B1 | ne | varni zapis |
| 2026-09-11 | Player save vrsta je vezana na world sejo in se izprazni pred resetom server globalov | B2 | ne | varen lifecycle |
| 2026-09-15 | Nov ukaz `/rwdiag` in zbiralnik meritev `rework/diag` | M2.1 | da — `/rwdiag on\|off`, `-Drwdiag=on` | izklopljeno |

Vse zgornje so popravki tihe izgube podatkov, zato so brez stikala in privzeto vklopljene.
Format datotek se ne spremeni, zato ni migracije. Izjema je zadnji stolpec pri praznem
ključu: tam original ni znal zapisati berljive datoteke, mi jo znamo, original pa jo zna
prebrati.

---

## Meritve

| Datum | Scenarij | Rezultat | Zapis |
|---|---|---|---|
| 2026-09-11 | NBT↔JSON round-trip, 4000 naključnih struktur | original 1598 napak (40 %), novi 0 | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON zapis, 1500 ključev (68 KB) | 235,3 ms → 2,9 ms (81×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON branje, 1500 ključev | 2386,3 ms → 6,5 ms (367×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-15 | M2.1 prvi posnetek: 8 NPC-jev, 61 s, brez igralca | MSPT p50 0,16 ms / p95 1,21 ms; `npc.per.tick` p50 = 0 | [zapis](meritve/2026-09-15-M2.1-prvi-posnetek.md) |
| 2026-09-17 | M2.2 R1: 8 nosilcev z jahači proti 8 brez, dve fazi, 945 tickov | proga M `isNavigating` **0/8 ves čas**, prevozeno 0,06 bloka; kontrola 8/8 in 22,71 bloka | [zapis](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| 2026-09-17 | poraba pri 27 dejavnih NPC-jih (ni baseline) | `npc.update.window` 207,3 µs/NPC; MSPT p95 10,75 ms, p99 115,3 ms | [zapis](meritve/2026-09-17-M2.2-R1-reprodukcija.md) |
| — | baseline MSPT še ni izmerjen (M2.6) | — | — |

Meritve so tekle na OpenJDK 21 v oblačnem okolju, ne na Javi 8. Ponovitev na Javi 8 je odprta.

---

## Znane omejitve

- Meritve so bile narejene na OpenJDK 21, produkcijski build je Java 8.
- Dekompilacija **ni** izvorna koda. To je bilo tokrat dvakrat potrjeno v praksi: dve trditvi
  iz prvotne analize sta bili napačni. Sumljivo logiko vedno preveri z `javap`.
- Vseh 746 dekompiliranih datotek **ni** ročno pregledanih.
- Združljivost z uporabnikovim dejanskim modpackom ni preverjena (Q1).
- Bugi iz `PLAN_IMPLEMENTACIJE.md`: B1 in B2 sta v M1.5/M1.6, B5 v M6.5. B3, B4, B6, B7, B8
  še niso razporejeni v milestone.
- Dedicated server je preverjen (M0.5). Igranje v svetu z igralcem, GUI in questi še ni (M0.6/M0.7).
- **Seja ima lupino na uporabnikovem računalniku, a linuxovo.** *(popravljeno 17. 9.;
  prej je tu pisalo, da lupine sploh ni.)* Priključene mape so v njej pod
  `$HOME/mnt/<mapa>`, na voljo so `git`, `python3`, `node`, `jq`, `sed`, `diff`. Kar v njej
  **ne gre**, je vse, kar potrebuje Windows: `.\dev.ps1`, `.\*-run.ps1`, gradle build,
  `verify-package.ps1` in zagon Minecrafta. Ti ostajajo na uporabniku. Delitev dela je
  torej: seja bere, piše, ureja, preverja in commita; uporabnik poganja build in svet ter
  javi log nazaj.
- **PowerShell skripte se da sintaktično preveriti brez Windowsa.** V oblačnem okolju
  PowerShell 7.4.6 (`Parser::ParseFile`) prebere `.ps1` in vrne napake razčlenjevanja.
  Izvedbe ne nadomesti, tipkarske napake pa ujame; uporabljeno 17. 9. na `r1-run.ps1`.
- ~~Datotek, globljih od 7 map pod **priključeno** mapo, ni mogoče prenesti v sejo.~~
  **Ne velja več od 17. 9.:** lupina vidi celotno drevo priključene mape, ne glede na
  globino, in prenos v sejo za branje ni potreben. Prejšnje besedilo ostaja kot zgodovina:
  **rešeno 15. 9.:** poleg korena projekta je zdaj priključena tudi mapa `dev`, s čimer sta
  pod mejo `dev/src/patch/java/noppes/npcs/rework/…` (7 map) in dekompilirani Minecraft v
  `dev/build/tmp/recompileMc/sources/net/minecraft/…` (7 map). Če nova seja teh datotek ne
  vidi, mora uporabnik v namizni aplikaciji dodati mapo `CustomNPC_mod_rework\dev` —
  priključitev globlje mape (`…\src\patch\java`) ni potrebna in koren sam ne zadošča.
- **Dekompiliran, Forge-patchan Minecraft je v projektu** in je verodostojnejši vir od
  spomina: `dev/build/tmp/recompileMc/sources/` (izvorna koda) in `…/compiled/` (razredi).
  Nastane ob `setupDecompWorkspace`. Uporabljen v M2.1c za `WorldServer` in `World`.
- Projekt teče na dveh delovnih postajah proti istemu `origin/main`. Seja začne z
  `git fetch origin` in preveri, ali je oddaljena veja pred lokalno.
- **Zapis iste datoteke dvakrat v isti seji lahko tiho ne uspe.** 15. 9. je drugi zapis
  `docs/04-STANJE.md` javil uspeh, na disku pa je ostala prejšnja verzija (45 479 B namesto
  46 946 B). Zapis pod novim imenom je uspel takoj. Pravilo: po vsakem zapisu preveri
  velikost datoteke, ob neujemanju zapiši pod novim imenom.
- `npc.update.window` je zgornja meja, ne točna poraba časa na NPC; točna meritev pride z M3.1.
- **NPC-ja ni mogoče poslati dlje od `NpcNavRange` (32) z enim samim klicem `navigateTo`.**
  Vanilla A* se prekine po 200 vozliščih in vrne **delno** pot (`PathFinder.findPath:65`),
  dolžino poti pa omeji na `getPathSearchRange()` = `FOLLOW_RANGE` = `NpcNavRange`
  (`:94`, `EntityNPCInterface.java:334`). NPC obstane na koncu delne poti in to izgleda kot
  ovira ali okvara AI — 17. 9. je stalo eno napačno razlago (Q11). **Vsak scenarij, ki pelje
  NPC dlje od 32 blokov ali po obvozu, mora pot osveževati.** Vanilla AI taski to počnejo
  sami, skripte ne.
- **Meritev brez prisilno naloženih chunkov ali brez igralca je neveljavna** po 300 tickih
  (`WorldServer.updateEntities():628-644`). Vsak posnetek ima zato `world.chunks.forced`;
  če je ta 0 in je `world.players` 0, posnetek meri prazen tek. Velja za vse meritve M2+.
- **Vsaka meritev potrebuje ogrevanje.** Priklop chunk ticketa sproži nalaganje chunka na
  server niti in to se šteje v meritev: brez ogrevanja je bil p99 81,8 ms namesto 4,3 ms in
  `npc.update.window` 182,5 µs namesto 66,6 µs. `rwdiag-run.ps1` čaka 10 s
  (`-WarmupSeconds`); pri M2.4 s stotinami chunkov je treba to dolžino **izmeriti**
  (dva zagona zapored, primerjava p99), ne ugibati.
- `server.tick.ns` **max** je pri vsakem zagonu en sam tick, ki sovpada z autosave
  (58 / 74 / 87 / 124 ms). M2.5 ga mora izločiti ali poročati posebej.
- Že pokvarjenih datotek na disku nova koda ne popravlja; M1.4 je po navodilu uporabnika
  odložen, dokler ne obstaja konkreten primer.
