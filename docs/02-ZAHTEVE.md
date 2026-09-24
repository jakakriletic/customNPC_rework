# 02 — Zahteve R1–R9, analiza in dokazi iz kode

Za vsako zahtevo: kaj uporabnik hoče, kaj **dokazano** piše v kodi, kandidati za vzrok,
kaj je treba še preveriti, in kaj je predlagana rešitev.

**Pomembna omejitev:** razen tam, kjer piše **[potrjeno]**, je vse sklicevanje na kodo na
dekompiliran izpis v `dev/reference-src/`, pridobljen s CFR 0.152. Dekompilacija ni izvorna
koda. Te trditve še niso bile reproducirane v igri — reprodukcija je prvi delovni paket
vsake zahteve. Kako hitro se dekompilat zlaže, kaže razdelek "Ovrženo" pri R9.

Oznake:

- **[potrjeno]** — preverjeno z izvedbo prave originalne kode ali z `javap` na bytecode
- **[dokazano]** — dekompiliran izpis to nedvoumno počne, a še ni izvedeno
- **[kandidat]** — verjeten vzrok, a potrebuje reprodukcijo
- **[preveri]** — potrebna dodatna preverba pred kakršnimkoli popravkom

---

## R1 — NPC mount uniči AI in premikanje nosilca

> *"Ko daš NPC-ju da se mounta na drugega NPC-ja, uniči AI in premikanje NPC-ja ki nosi
> drugega. Konjenica: ko grejo napadat se skupaj grupirajo v radiusu enega bloka, premikanje
> je čudno, ne znajo čez bloke."*

**Faza:** M3 · **Velikost:** velika · **Tveganje:** srednje

### Kaj kaže koda

**[dokazano] `EntityNPCInterface` ne override-a nobene passenger metode.**
Iskanje po `dev/reference-src/noppes/npcs/entity/EntityNPCInterface.java` najde samo
`removePassengers()` na vrstici 1291 (v `setDead()`). Ni override-a za:

| Metoda | Posledica privzete vanilla implementacije |
|---|---|
| `updatePassenger(Entity)` | jahač se vsak tick postavi na `posX, posY + getMountedYOffset() + passenger.getYOffset(), posZ` |
| `getMountedYOffset()` | privzeto `height * 0.75`; NPC-ju se višina spreminja (glej spodaj) → offset niha |
| `getControllingPassenger()` | vedno `null` → nosilec ni nikoli krmiljen od jahača |
| `canBeSteered()` | `false` |
| `canRiderInteract()` | privzeto |

**[dokazano] `updateHitbox()` (vrstica 1075) je odvisen od `isRiding()`, a se ob spremembi
stanja jahanja ne pokliče.**

> **Popravek 24. 9. (M3.5):** velja za `EntityNPCInterface`; `EntityCustomNpc.startRiding` ga v
> originalu pokliče, zato je mount pravilen. Manjka samo sestop (`dismountRidingEntity`).

```java
} else if (this.isRiding()) {            // vrstica 1079
    this.width = 0.6f;
    this.height = this.baseHeight * 0.77f;
}
```

Klicna mesta `updateHitbox()`: `EntityNPCInterface.java:368, 459, 486, 1139, 1300` in
`ai/EntityAIAnimation.java:97`. Nobeno od njih ni ob `startRiding` / `dismountRidingEntity`.
Torej NPC po vstopu na mount obdrži staro višino in po sestopu obdrži skrčeno višino, dokler
ga nekaj drugega ne osveži. Napačna višina vpliva na `getMountedYOffset()` nosilca, na
kolizije in na `EntityAIAttackTarget` (glej spodaj).

**[dokazano] AI taski ne preverjajo `isRiding()`.**

`ai/EntityAIFollow.java:67` kliče `getNavigator().tryMoveToEntityLiving(...)` brez preverbe,
ali NPC sploh lahko hodi. Še huje, vrstica 70:

```java
this.npc.tpTo(this.owner);
```

`tpTo()` (`EntityNPCInterface.java:1760`) kliče `setLocationAndAngles(...)` — **na jahaču,
ki sedi na nosilcu**. To se vsak tick bije z `updatePassenger()` nosilca. To je zelo močan
kandidat za "premikanje je čudno".

`ai/EntityAIAttackTarget.java:75,90` prav tako brezpogojno vozita navigator.

**~~[dokazano] `EntityAIAttackTarget` konstruktor nastavi napačne mutex bite.~~ — ovrženo 24. 9. (M3.6).**
Prvotni zapis je trdil, da napad ne rezervira `PATHING` in da zato z njim hkrati tečejo `Wander`,
`Follow`, `Return` in `MovingPath`. **To ne drži:** `AiMutex` 1/2/4 so vanilla biti MOVE/LOOK/JUMP,
napad ima 1 + 2 = 3 kot vanilla `EntityAIAttackMelee`, vsi gibalni taski imajo bit 1, zato z napadom
hkrati ne teče noben (`EntityAITasks.canUse`). „Popravek“ s `PATHING` bi spremenil le razmerje do skoka
(`EntityAIPounceTarget`).

**[dokazano na bytecode, M3.6] Resnična napaka pri dodajanju napada: napad nima svoje prioritete.**
`setResponse` doda napad s `taskCount` brez `++`, zato `EntityAIWander` (ali `EntityAIMovingPath`)
dobi isto prioriteto; napad ga zato ne more prekiniti in NPC, ki tava, tarčo napade šele na koncu
poti tavanja. [zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md)

**[dokazano] minimalni napadalni doseg je odvisen od širine NPC-ja.**

```java
// EntityAIAttackTarget.java:98
double minRange = this.npc.width * 2.0f * this.npc.width * 2.0f + this.entityTarget.width;
```

Pri `display.getHasHitbox() == false` postavi `updateHitbox()` širino na `1.0E-5f`
(`EntityNPCInterface.java:1088`). Takrat je `minRange` praktično 0 in NPC se mora dobesedno
zlepiti s ciljem, preden napade → **to je zelo verjetna razlaga za "grupirajo se v radiusu
enega bloka"**. Isti izraz je občutljiv na napačno višino/širino iz prejšnje točke.

**[kandidat] `updateTasks()` (vrstica 744) med jahanjem na novo ustvari navigator in
moveHelper** (vrstice 759–769), vključno z `world.pathListener.onEntityRemoved/onEntityAdded`.
Klice sproži `updateAI = true` z vrstic 308, 693, 1050, 1140. Če se to zgodi, medtem ko NPC
jaha, je novonastali `PathNavigateGround` v nedefiniranem stanju glede jahanja. **[preveri]**

**[kandidat] `onCollide()` (vrstica ~1151) pri jahanju vsakič računa unijo AABB-jev:**

```java
axisalignedbb = this.getRidingEntity() != null && this.getRidingEntity().isEntityAlive()
    ? this.getEntityBoundingBox().union(this.getRidingEntity().getEntityBoundingBox()).grow(1.0, 0.0, 1.0)
    : this.getEntityBoundingBox().grow(1.0, 0.5, 1.0);
```

Teče vsak 4. tick za vsakega NPC-ja. Pri konjenici to podvoji strošek. Performance, ne bug.

### Kaj je treba še preveriti

1. ~~**Reprodukcija.**~~ **Narejeno 17. 9. (M2.2)**, s kontrolno progo brez jahačev.
   Zapis: [`meritve/2026-09-17-M2.2-R1-reprodukcija.md`](meritve/2026-09-17-M2.2-R1-reprodukcija.md).
2. ~~**Kdo od obeh je pokvarjen.**~~ **Odgovorjeno 17. 9.: nosilec, in sicer tako, da poti
   sploh ne dobi.** `isNavigating()` je `false` v 40 od 40 vzorcev proge z jahači in `true`
   na kontrolni progi. Jahač nosilcu **ne** prepisuje pozicije: `odstopMax = 0,00` v vseh
   vzorcih, ker `EntityAIFollow` zahteva lastnika in ga fixture nima. Vprašanje se zato
   preoblikuje: **zakaj `tryMoveTo*` na nosilcu ne da poti.** Vodilni kandidat je
   `PathNavigateGround.canNavigate()` (`:32-35`), ki zahteva `onGround`; `isRiding()` v istem
   izrazu velja za jahača, ne za nosilca. Dokaz zahteva števec `onGround` / `canNavigate()` /
   `noPath()` na tick (M2.1b/M3.1) in ga **še ni**.
   **→ 24. 9. (M3.2) mehanizem najden v vanilla kodi, `canNavigate()` ni vzrok.**
   `EntityLiving.updateEntityActionState` (final) v ticku *jahača* naredi
   `nosilec.getNavigator().setPath(jahač.getPath(), 1.5)` in
   `nosilec.getMoveHelper().read(jahač.getMoveHelper())`; jahač brez poti nosilcu pot vsak
   tick izbriše. Pojasni vseh šest izmerjenih številk. **Potrjeno v svetu 24. 9.** (faza C,
   E7): ko pot dobijo jahači, imajo nosilci pot natanko takrat kot jahači (20/20 vzorcev),
   prečkajo stopnico in vrata in pridejo na cilj. [diagnoza](meritve/2026-09-24-M3.2-R1-diagnoza.md)
2b. **Zgoščevanje v radiusu enega bloka ni reproducirano.** Izmerjeno je zgoščevanje na 7
   blokov pred oviro. Za radius enega bloka je treba preizkusiti pogoj
   `display.getHasHitbox() == false` (`minRange` v `EntityAIAttackTarget:98` postane ≈ 0).
   Ločen scenarij.
3. ~~**Vanilla `EntityLivingBase.travel` in `EntityLiving.updateEntityActionState`**~~ —
   **preverjeno 24. 9.** na mapiranem bytecode (`dev/build/tmp/recompileMc/compiled`, CFR):
   glej točko 2. `travel` nosilca je v redu (kontrola vozi), pokvarjen je vhod vanj.
4. Ali `ItemMounter` / `EnumPacketServer.SpawnRider` sploh gresta skozi `startRiding()` ali
   kaj drugega (`items/ItemMounter.java:31`).

### Predlagana rešitev

1. **`RiderState` sloj** v `rework/entity/`: NPC ve, ali je jahač, nosilec ali prost, in to
   stanje je edini vir resnice za AI, hitbox in navigacijo.
2. **Gating AI taskov.** Vsi taski, ki pišejo navigator, dobijo skupno preverbo
   `canPathfind()` → `false`, ko je NPC jahač. `EntityAIFollow.tpTo` se med jahanjem ne sme
   izvesti nikoli.
3. ~~**Popravek mutex bitov**~~ — premisa ovržena (M3.6). Namesto tega **prioriteta napada pred
   gibanjem** (`RwAttackPriority`), pod stikalom, z A/B scenarijem `m36-run.ps1`.
4. **`updateHitbox()` ob vsaki spremembi jahanja**, prek Forge `EntityMountEvent`.
5. **Ločitev `minRange` od širine**: uvesti spodnjo mejo, neodvisno od `hasHitbox`.
6. **Krmiljenje nosilca** (opcijsko, za konjenico): jahač lahko postane "commander" nosilca —
   nosilec podeduje cilj jahača in se premika, jahač samo napada. To je nova funkcija, ne
   bugfix, in gre v ločen paket.

---

## R2 — Letenje NPC-jev

> *"Letenje od NPC-jev je kinda ass. Rad bi opcijo da leti kot creative, ali pa da leti kot
> ender dragon, za možno delanje letal."*

**Faza:** M4 · **Velikost:** velika · **Tveganje:** srednje

### Kaj kaže koda

**[dokazano] Trenutno obstajata samo dva načina gibanja, določena z `ais.movementType`:**

```java
// EntityNPCInterface.java:760-770
if (this.ais.movementType == 1) {        // leteči
    this.moveHelper = new FlyingMoveHelper(this);
    this.navigator = new PathNavigateFlying(this, this.world);
} else if (this.ais.movementType == 2) { // plavajoči
    this.moveHelper = new FlyingMoveHelper(this);
    this.navigator = new PathNavigateSwimmer(this, this.world);
} else {                                  // pešec
    this.moveHelper = new EntityMoveHelper(this);
    this.navigator = new PathNavigateGround(this, this.world);
    this.tasks.addTask(0, new EntityAIWaterNav(this));
}
```

**[dokazano] `FlyingMoveHelper` je preprost in se ob prvi oviri ustavi.**

`ai/FlyingMoveHelper.java`:

- vrstica 35: `courseChangeCooldown = 4` — smer se popravlja samo vsake 4 ticke
- vrstica 41: `speed = MOVEMENT_SPEED / 2.5` — hitrost letenja je izpeljana iz hodne hitrosti
- vrstici 40, 50: če `isNotColliding()` vrne `false`, se akcija postavi na `WAIT` —
  **NPC se preprosto ustavi in ne poišče obhoda**
- vrstice 55–67: `isNotColliding()` vzorči po 1 blok naenkrat vzdolž ravne črte; za vse, kar
  ni ravna prosta linija, je neuporabno

**[dokazano, 17. 9.] `FlyingMoveHelper` ignorira hitrost, ki mu jo da navigator.**
`PathNavigateFlying.onUpdateNavigation` (`:78`) kliče `getMoveHelper().setMoveTo(x, y, z,
this.speed)`, `FlyingMoveHelper.onUpdateMoveHelper` pa polja `this.speed` **nikoli ne
prebere** — hitrost vedno izpelje iz `MOVEMENT_SPEED / 2.5` (`:41`). Parameter `speed` v
`navigateTo(x, y, z, speed)` torej na letenje ne vpliva; na hojo, kjer vanilla
`EntityMoveHelper` to polje uporablja, pa vpliva. Vsaka skripta, ki poskuša letečega NPC-ja
upočasniti ali pospešiti prek `navigateTo`, tiho ne naredi ničesar.

**[dokazano, 17. 9.] Leteči NPC, ki obstane, obvisi v zraku.** `EntityNPCFlying.travel()`
pri `canFly()` gravitacije sploh ne doda; ostane samo dušenje 0,91 (`:67-78`). Ko se move
helper postavi na `WAIT`, NPC torej ne pade nazaj na tla, ampak miruje tam, kjer je. To je
opazni simptom, ki ga uporabnik opiše kot „obtiči".

**[dokazano, 17. 9.] Delna pot je pri letenju verjetnejša kot pri hoji.**
`PathNavigateFlying` uporablja vanilla `FlyingNodeProcessor`, ki širi vozlišča v treh
dimenzijah, `PathFinder.findPath` (`:65`) pa se ustavi po **200 vozliščih** in vrne najboljše
doseženo vozlišče, ne cilja. Isti proračun v 3D pokrije bistveno manj napredka kot v 2D.
Skupaj z `maxDistance = FOLLOW_RANGE = NpcNavRange = 32` (`:94`, `EntityNPCInterface.java:334`)
to pomeni, da leteči NPC pogosto dobi pot, ki se konča **pred** oviro — kar iz igre izgleda
enako kot okvara AI. Meri se to z `getNavigationPath()`; scenarij M2.3 to poroča kot `cele=b/N`.

**[izmerjeno, 21. 9. — M2.3, dva zagona]** V prizorišču M2.3 (start 16 blokov od cilja, med
njima zid 4 bloke visok in 75 širok, obhod čez `NpcNavRange`) je izmerjeno tole, za šest
letečih NPC-jev na progo, s kopensko in prosto kontrolno progo v istem ticku:

| Vodenje | Čez zid | `cele` | prevoženo |
|---|---|---|---|
| en sam `navigateTo(cilj)` | **0/6** | **0/6** | 7,95 |
| `navigateTo(cilj)` osvežen vsakih 20 tickov | **6/6** | 3/6 | 16,36 |
| `setAttackTarget` (pot osvežuje vanilla AI) | 5/6 | **6/6** | 14,86 |

Kopenska kontrola ne pride čez zid v nobenem od treh vodenj (zid je torej res ovira), prosta
lečeča kontrola pride čez v vseh treh (letenje torej samo po sebi dela). **Razlika med prvo
in drugima dvema vrsticama je ena sama: ali se pot osvežuje.**

Posledica za popravek: **v tem prizorišču ovire ne ustavi `WAIT` iz `isNotColliding()`, ampak
zastarela delna pot.** `cele = 0/6` pri enem samem klicu pomeni, da se pot sploh ne konča pri
cilju — NPC pot poslušno odleti do konca in tam obstane, ker mu je nihče ne zamenja. To je
isti mehanizem kot Q11 pri M2.2, le da ga tam povzroči domet iskanja in tu ovira.
Trditev velja za pot čez oviro, krajšo od `NpcNavRange` = 32; za daljše razdalje prizorišča
še ni (M4.10). [meritev](meritve/2026-09-17-M2.3-R2-reprodukcija.md)

**[ovrženo z meritvijo, 21. 9.]** Hipoteza, da letečega NPC-ja ustavi odmik **točno 0,5
bloka** od sredine vozlišča (`PathNavigate.pathFollow:286` preštevilči pri < 0,45,
`FlyingMoveHelper:39` doda gibanje pri > 0,5), **ne drži**: faza D scenarija M2.3 je začela
natanko na taki koordinati in NPC-ji so se premikali že v prvem vzorcu. Obe številki iz kode
sta pravilni, sklep iz njiju pa ni bil.

**[dokazano] `EntityNPCFlying.travel()` (vrstice 45–90) je kopija vanilla `EntityFlying`
fizike**, s posebnostjo, da pri `movementType == 2` sili `motionY = -0.15` izven vode
(vrstici 50–52). Drsenje (`0.91f`, `0.16277136f`) je prevzeto iz hodne fizike, kar da
"plavajoč, len" občutek.

**[dokazano] leteči NPC-ji nimajo interakcije z vrati in ne iščejo zavetja pred soncem:**
`doorInteractType()` (vrstica 869) in del `seekShelter()` (vrstica 888) zgodaj vrneta, če `canFly()`.

### Kaj hoče uporabnik

| Način | Obnašanje | Uporaba |
|---|---|---|
| **A — Ground** | kot zdaj | privzeto, nespremenjeno |
| **B — Hover / creative** | prosto gibanje v 3D, brez gravitacije, hitro ustavljanje, brez inercije | lebdeči čuvaji, droni, NPC v zraku |
| **C — Dragon / momentum** | inercija, krivuljni zavoji, minimalna hitrost, nagib (pitch/roll), dviganje in spuščanje | letala, zmaji, vozila |
| **D — Swim** | kot zdaj, ločeno od letenja | vodni NPC-ji |

### Predlagana rešitev

1. **Nov enum `FlightMode`** v `rework/movement/`, shranjen v novem NBT ključu
   (`RwFlightMode`). `ais.movementType` ostane nedotaknjen za združljivost; nova vrednost
   se upošteva samo, če je prisotna.
2. **Trije move helperji**:
   - `HoverMoveHelper` — neposredno pozicijsko vodenje, hitro dušenje, brez inercije
   - `MomentumMoveHelper` — model z omejenim kotnim pospeškom, minimalno hitrostjo in
     nagibom; osnova za letala
   - obstoječi `FlyingMoveHelper` ostane kot združljivostni način
3. **3D pathfinding.** `PathNavigateFlying` v 1.12.2 je šibek. Dve poti:
   - preslikati pristop iz CustomNPC+ ("smart pathfinding in 3D space")
   - ali lasten `AStar3D` s cenovno funkcijo, ki kaznuje bližino blokov
   Odločitev pade po branju CustomNPC+ implementacije v M4.1.
4. **Osveževanje poti — po meritvi prva stvar, ki jo je treba popraviti.** M2.3 je pokazal,
   da isto skupino čez isto oviro spravi že to, da se pot osvežuje (6/6 proti 0/6). Vsak
   `PathNavigateFlying`, ki dobi pot z `cele = false`, jo mora znova poiskati, namesto da NPC
   odleti do konca delne poti in tam obstane. To je majhen popravek z izmerjenim učinkom in
   gre pred 3D pathfinding.
5. **Popravek `isNotColliding`**: vzorčenje po dejanskem hitboxu z več žarki, ne po eni črti,
   in obhod namesto `WAIT`. **Po meritvi to ni bil vzrok obstanka v prizorišču M2.3**, zato
   gre za izboljšavo in ne za popravek te napake.
6. **Render podpora za nagib.** Trenutno se NPC-ju nastavlja samo `rotationYaw`
   (`FlyingMoveHelper.java:48`). Za način C je potreben pitch in po možnosti roll →
   sprememba na render strani, klient/server sinhronizacija.
7. **Ločen paket "vozila"** (letala): NPC kot vozilo, ki ga igralec krmili. Odvisen od R1
   (passenger sloj) in od načina C. **Ne** del M4; predviden po M8.

---

## R3 — Migracija funkcij iz CustomNPC+ (1.7.10) na 1.12.2

> *"V CustomNPC 1.7.10+ je ful ful več funkcij ki bi jih rad migriral na 1.12.2."*

**Faza:** M8, deloma raztreseno · **Velikost:** zelo velika · **Tveganje:** visoko

Gre za [`KAMKEEL/CustomNPC-Plus`](https://github.com/KAMKEEL/CustomNPC-Plus). Po opisu
projekta ima nad originalom:

| Funkcija | Kje v našem načrtu |
|---|---|
| Java scripting prek Janino, lambde, polni razredi | **R7 / M6** — to je referenčna implementacija |
| 154+ script hookov (NPC, player, block, item, quest, dialog, ability) | **R7 / M6** |
| Frame-based animacijski sistem z in-game urejevalnikom, po delih telesa | **R4 / M7** |
| Script hooki za animacije (start, end, frame enter, frame exit) | **R4 / M7** |
| Flying NPC-ji s smart 3D pathfindingom | **R2 / M4** |
| Taktike: ambush, pounce, dodge-shoot, zigzag, stalk, orbit, sprint-to-target, leap | **delno že v 1.12.2** — primerjati in dopolniti (M8) |
| Ability sistem s fazami | M8 |
| Party sistem | M8 |
| Profile sistem (več karakternih slotov) | M8 |
| Custom effects nad vanilla napitki | M8 |
| HUD z quest trackingom | M8 |
| Auction house / ekonomija | M8, nizka prioriteta |

### Kako se to dela

**To ni port kode.** 1.7.10 in 1.12.2 imata drugačen entity, NBT, render, packet in
registry model. Za vsako funkcijo:

1. **Katalogizacija** (M8.1): prebrati CustomNPC+ repo in narediti tabelo
   `funkcija → razredi → 1.12.2 ekvivalent → ocena truda → odvisnosti`.
   Rezultat gre v `docs/06-CNPCPLUS-KATALOG.md`.
2. **Prioritizacija** z uporabnikom. 30+ funkcij ni realno; izberemo 5–8.
3. **Za vsako:** ločen paket z lastnim NBT ključem, stikalom, testom in dokumentacijo.

### Kaj je treba še preveriti

- Točni licenčni pogoji CustomNPC+ za prevzem kode (avtor navaja dovoljenje za svojo vejo
  za 1.7.10, kar ni isto kot dovoljenje za naš port).
- Ali je za posamezno funkcijo bolje portati ali napisati na novo — pogosto je novo hitreje.

---

## R4 — AI-prijazno okolje za NPC animacije

> *"Rad bi naredil svoje okolje namenjeno da je maksimalno razumljivo AI-ju za izdelavo NPC
> animacij in integracijo teh v skripte, da bi mi AI delal animacije."*

**Faza:** M7 · **Velikost:** zelo velika · **Tveganje:** visoko · **Odvisnost:** M6 (R7)

### Kaj kaže koda

**[dokazano] 1.12.2 CustomNPCs nima animacijskega sistema, samo fiksne poze.**

- `EntityNPCInterface` hrani `currentAnimation` kot en `int` v `dataManager` (`Animation`),
  sinhroniziran na klient (vrstice 481–487)
- `ai/EntityAIAnimation.java` preklaplja med nekaj stanji in kliče `npc.updateHitbox()` (vrstica 97)
- `ModelData`, `ModelPartData`, `ModelPartConfig`, `ModelEyeData` opisujejo **statično**
  konfiguracijo modela, ne časovnih krivulj

Torej: animacijski sistem je treba **zgraditi**, ne popraviti.

### Kaj mora nastati

Tri ločene stvari, po tem vrstnem redu:

**1. Animacijski podatkovni model in runtime** (`rework/anim/`)

- `Animation` = zaporedje `Frame`, vsak `Frame` = trajanje + set `PartTransform`
  (rotacija, translacija, scale po delu telesa)
- interpolacija (linear, ease, step), looping, blending med animacijami
- deterministično izvajanje na serverju + sinhronizacija na klient
- hooki: `onAnimationStart`, `onAnimationEnd`, `onFrameEnter`, `onFrameExit`
- CustomNPC+ ima to rešeno — M7.1 je branje njihove implementacije

**2. Tekstovni format, ki ga AI razume** — jedro te zahteve

Ključna ugotovitev: LLM je dober pri pisanju **strukturiranega teksta s shemo in primeri**,
slab pri ugibanju binarnih formatov in GUI klikih. Zato:

- animacija je **ena datoteka** v deklarativnem formatu (JSON s shemo ali DSL),
  berljiva in pisljiva brez orodja
- zraven leži **strojno berljiva shema** (JSON Schema) + `ANIMATION-GUIDE.md` s
  koordinatnim sistemom, imeni delov telesa, enotami (stopinje/ticki), mejami in 10+
  komentiranimi primeri od preprostega do kompleksnega
- **validator kot CLI ukaz** (`.\dev.ps1 validateAnim <datoteka>`), ki vrne konkretne napake
  z vrstico — AI popravlja na podlagi napak, ne ugiba
- **`/npcanim reload`** v igri za takojšen preview brez restarta

**3. Integracija v skripte**

Animacije se sprožajo iz skript (R7): `npc.getAnimator().play("draw_sword")`,
`.queue(...)`, `.blendTo(...)`, `.stop()`. Vse tipizirano, da AI dobi autocomplete in
compile-time napake namesto runtime tišine.

### Zakaj je to odvisno od M6

AI-prijazno okolje in Java scripting sta **isti problem**: strojno berljiva shema, tipi,
validacija, hitra povratna zanka. Če se gradita ločeno, nastaneta dva nezdružljiva sistema.
Zato M6 najprej postavi skupno infrastrukturo (API shema, generator dokumentacije, validator),
M7 jo uporabi za animacije.

### Kaj je treba še preveriti

- Ali je in-game urejevalnik sploh potreben, ali zadošča tekstovni format + hot reload.
  (Predlog: tekstovni format prvi, urejevalnik kasneje in opcijsko.)
- Kako daleč gre sinhronizacija: ali animacija teče na serverju in se pošilja, ali se pošlje
  samo ukaz "predvajaj X od tika T" in klient interpolira. Drugo je bistveno cenejše.

---

## R5 — Performance optimizacija AI

> *"Rad bi da se optimizira performance AI NPC-jev da s tem omogoči čim več NPC-jev v svetu."*

**Faza:** M5 · **Velikost:** velika · **Tveganje:** visoko (tiho spreminjanje obnašanja)

### Kaj kaže koda

**[dokazano] En globalen lock za VSE skripte vseh NPC-jev.**

```java
// controllers/ScriptContainer.java:51
private static final String lock = "lock";
...
// vrstica 142
synchronized (lock) { ... }
```

To je `static`, torej ga **vsi `ScriptContainer` na strežniku delijo**. Vsak script dogodek
vsakega NPC-ja se serializira skozi en monitor. Poleg tega je to internirani string literal —
katerakoli druga koda, ki se sinhronizira na `"lock"`, se zaklene z njim.
**To je najverjetneje največje posamezno ozko grlo pri velikem številu skriptanih NPC-jev.**

Dodatno: `Current` in `CurrentType` (vrstici 52–53) sta statična, torej je model
"en script naenkrat" vgrajen v zasnovo.

**~~[dokazano] Konflikt AI taskov okrog navigatorja~~ — ovrženo 24. 9. (M3.6):** mutex biti
napada so enaki vanilla `EntityAIAttackMelee`, gibalni taski z njim ne tečejo hkrati. Glej R1 in
[zapis](meritve/2026-09-24-M3.6-prioriteta-napada.md).

**[dokazano] Vsak `getFullCode()` sestavlja kodo z zaporedno konkatenacijo**
(`ScriptContainer.java:106-120`), vsak `run()` ustvari nov `StringWriter` + `PrintWriter`
(vrstici 145–146) — tudi če skripta ničesar ne izpiše.

**[dokazano] `NBTJsonUtil.ConvertList` (vrstica 251) in `ReadValue` (vrstici 160, 167)
gradita nize znak po znak z `+`** → kvadratna kompleksnost pri velikih datotekah.

**[dokazano] `onCollide()` teče vsak 4. tick za vsakega NPC-ja** in dela
`getEntitiesWithinAABB` (`EntityNPCInterface.java:1151+`).

**[dokazano] `onLivingUpdate` vsakih 20 tickov naredi `getEntitiesWithinAABB(EntityMob.class,
box.grow(16,16,16))`** za vse NPC-je s `faction.getsAttacked` (vrstice 435–441). To je
32×32×32 blokov iskanja na NPC-ja na sekundo.

Ostali kandidati so že katalogizirani v `PLAN_IMPLEMENTACIJE.md` §5 — ta tabela ostane v veljavi.

### Predlagana rešitev, po vrsti

1. **Izmeri prvo.** M2 postavi baseline: 50/200/500 NPC-jev, ločeno idle / combat /
   scripts / render. Brez tega se ne dela nič.
2. **Odstranitev globalnega script locka** (M5.1) — lock na `ScriptContainer` instanco,
   ne statičen; `Current`/`CurrentType` v `ThreadLocal` ali v kontekst objekt. Pozor:
   to lahko razkrije skrite race conditione v obstoječih skriptah → stikalo, privzeto staro.
3. **AI budget / scheduler** (M5.2): omejitev števila novih izračunov poti na tick globalno,
   s pošteno vrsto. NPC-ji, ki so na vrsti, dobijo pot; ostali počakajo tick ali dva.
   **To ne sme zmanjšati tick frekvence NPC-jev** — `PLAN_IMPLEMENTACIJE.md` §2 to izrecno
   prepoveduje, ker bi razbilo timerje in questne skripte.
4. **Deduplikacija poizvedb po svetu** (M5.3): `getEntitiesWithinAABB` za faction check in
   `onCollide` se lahko računa enkrat za skupino NPC-jev v istem chunku.
5. **Alokacijska higiena** (M5.4): `StringBuilder`, lazy writerji, zgodnji izhodi pri praznih
   zbirkah (`DataTimers.update()` iz `PLAN_IMPLEMENTACIJE.md`).
6. **Šele nato** dražje stvari: cache poti, prostorski indeks, LOD.

### Izrecno prepovedano brez ločenega projekta

Večnitni AI, deljen script engine med NPC-ji, globalno redkejši NPC tick, uspavanje
oddaljenih NPC-jev. Vse to spremeni obnašanje questov, timerjev in scriptane logike.

---

## R6 — Solid hitbox

> *"Rad bi dal opcijo še solid hitboxa da se ga ne da premakniti kot je to narejeno v 1.16.5."*

**Faza:** M3 · **Velikost:** majhna · **Tveganje:** nizko

### Kaj kaže koda

**[dokazano] Trenutno obstaja samo dvojno stanje "ima hitbox / nima hitboxa":**

```java
// EntityNPCInterface.java:1598
public boolean canBeCollidedWith() { return !this.isKilled() && this.display.getHasHitbox(); }
// vrstica 1602
public boolean canBePushed() { return super.canBePushed() && this.display.getHasHitbox(); }
// vrstica 1606
public EnumPushReaction getPushReaction() {
    return this.display.getHasHitbox() ? super.getPushReaction() : EnumPushReaction.IGNORE;
}
```

**[dokazano] `applyEntityCollision` ni override-an**, torej NPC-ja odrivajo igralci in druge
entitete po vanilla pravilih.

**[dokazano] `getCollisionBoundingBox()` ni override-an** → NPC ni trdna ovira; entitete
gredo skozi njegov prostor in ga samo potiskajo.

### Predlagana rešitev

Nov način prikaza hitboxa — trojno stanje namesto dvojnega, shranjeno v novem NBT ključu
`RwHitboxMode` (obstoječi `HasHitbox` ostane nedotaknjen):

| Način | `canBeCollidedWith` | `canBePushed` | `getCollisionBoundingBox` | `applyEntityCollision` |
|---|---|---|---|---|
| `NONE` | false | false | null | no-op |
| `NORMAL` (privzeto) | true | true | null | vanilla |
| `SOLID` | true | **false** | **AABB entitete** | **no-op** |

`getCollisionBoundingBox()` z ne-`null` vrednostjo naredi entiteto trdno oviro v 1.12.2 —
enako kot čoln ali shulker. **[preveri]** kako se to obnese pri:

- pathfindingu drugih NPC-jev (ali jih `PathNavigateGround` obide ali se zaletavajo)
- igralcu, ki ostane ujet med dvema solid NPC-jema
- NPC-jih, ki se premikajo (trdna premikajoča ovira lahko potisne igralca v steno)
- jahanju (R1) — solid nosilec pod jahačem

Zaradi zadnje točke gresta R6 in R1 v isti milestone.

---

## R7 — Scripting v Javi namesto JavaScripta

> *"Scriptanje NPC-jev bi rad preuredil da ni več star beden JavaScript ampak da bi se dalo
> scripte pisati direktno v Javi in bi s tem maksimiziral performance. Tudi to bi večinoma
> dajal AI-jem, zato bi bilo okolje čim bolj prilagojeno AI-ju."*

**Faza:** M6 · **Velikost:** zelo velika · **Tveganje:** visoko

### Kaj kaže koda

**[dokazano] Trenutno: JSR-223 engine-i, primarno Nashorn.**

`controllers/ScriptController.java:61-110` registrira Nashorn (`ecmascript`, `.js`),
opcijsko Kotlin JSR-223, in pobere vse ostale JSR-223 engine-e s classpatha.
`ScriptContainer.run()` (vrstica 129) naredi `engine.eval(fullCode)` ob prvi uporabi in nato
`((Invocable) engine).invokeFunction(type, event)` (vrstica 166).

**[dokazano] Vse to teče pod enim globalnim lockom** (vrstica 142) — glej R5.

**[dokazano] Napake so tihe.** Manjkajoča funkcija → `NoSuchMethodException` → ime gre v
`unknownFunctions` (vrstica 170) in se nikoli več ne kliče. Katerakoli druga napaka →
`errored = true` (vrstica 173) in **cela skripta se ustavi do reloada**. Za AI, ki piše
skripte, je to najslabši možni feedback.

**[dokazano] Reload logika ima bug (B5 iz `PLAN_IMPLEMENTACIJE.md`):** vrstica 130 vrne
zgodaj zaradi `unknownFunctions`/`errored`, **preden** vrstica 137 preveri, ali je koda novejša.

### Ali je Java scripting sploh smiseln — da, in zakaj

- **Performance:** prevedena Java koda teče kot navaden bytecode, brez JS↔Java marshallinga
  in brez interpretacije. To je realen dobiček, ne teoretičen.
- **Za AI:** tipi so **preverljivi pred zagonom**. AI, ki napiše `npc.setHealth("full")`,
  dobi napako pri prevajanju z vrstico, namesto tihega neuspeha čez tri dni.
- **Precedens:** CustomNPC+ to že dela z Janino ("real Java code compiled by the Janino
  engine, full class and lambda support"). Ni raziskovalni projekt.

### Predlagana arhitektura

```
rework/script/
├── api/          tipiziran API (IRwNpc, IRwPlayer, IRwWorld, IRwEvent…)
├── compile/      prevajanje + cache + hot reload
├── hooks/        registry dogodkov
└── compat/       stari JS ostane delujoč
```

**Prevajalnik.** Dve možnosti, odločitev v M6.1 po prototipu:

| | Janino | `javax.tools.JavaCompiler` (JDK 8) |
|---|---|---|
| Velikost | ~1 MB embedded | že v JDK |
| Podpora jezika | podmnožica Jave (lambde da, generics omejeno) | polna Java 8 |
| Zahteva JDK | ne | **da** — uporabnik mora imeti JDK, ne JRE |
| Precedens | CustomNPC+ ga uporablja | — |

Predlog: **Janino primarno** (deluje povsod), `JavaCompiler` kot opcijski "full" način.

**Izolacija.** Vsaka skripta se prevede v svoj `ClassLoader`, da se da razred ob reloadu
zavreči. Globalne spremenljivke ostanejo izolirane po NPC-ju
(`PLAN_IMPLEMENTACIJE.md` §2 to izrecno zahteva).

**Varnost.** Prevedena Java ima polni dostop do JVM. Potrebna je whitelist paketov in
prepoved refleksije, `java.io`, `java.net`, `System.exit`. **[preveri]** koliko se to da
uveljaviti z Janino; verjetno z bytecode verifikacijo po prevajanju.

**Napake, ki jih AI lahko popravi.** To je ključno:

- napaka prevajanja → **datoteka, vrstica, stolpec, sporočilo**, v log in v igro
- runtime izjema → stack trace z imenom skripte in vrstico, ne samo `errored = true`
- skripta se ne sme trajno onemogočiti; po popravku in reloadu mora spet teči (popravek B5)
- CLI: `.\dev.ps1 compileScripts` prevede vse skripte **brez zagona Minecrafta** →
  AI dobi povratno zanko v sekundah namesto minutah

**Strojno berljiv API.** Generator, ki iz API vmesnikov naredi:

- `docs/api/API.json` — polna shema (razredi, metode, parametri, tipi, opisi)
- `docs/api/API.md` — človeško berljivo
- `docs/api/HOOKS.md` — vsi hooki s podpisi in primeri
- predlogo projekta (`scripts/java/template/`), ki se prevede v IDE-ju z autocomplete

**Stari JS ostane.** Ne odstranjujemo Nashorna. NPC dobi izbiro jezika; obstoječe skripte
delujejo naprej. To ni pogajanje — obstoječi svetovi bi drugače nehali delovati.

### Faznost znotraj M6

1. M6.1 prototip prevajalnika + odločitev Janino / JavaCompiler
2. M6.2 hook registry in tipiziran API za NPC (najmanjši uporaben nabor)
3. M6.3 hot reload + cache + izolacija ClassLoaderjev
4. M6.4 diagnostika napak + `compileScripts` CLI
5. M6.5 generator API dokumentacije + projektna predloga
6. M6.6 popravek B5 + odstranitev globalnega locka (skupaj z M5.1)
7. M6.7 širjenje API-ja na player, world, item, block, quest, dialog

---

## R8 — Chatbot v NPC-jih

> *"Možnost dodajanja chatbotov v NPC-je, da bi podajali možnost odgovorov in pogovorov.
> Recimo da se kar v GUI-ju poda AI API ključ."*

**Faza:** M9 · **Velikost:** srednja · **Tveganje:** srednje (omrežje, ključi, cena)

### Kaj kaže koda

**[dokazano] Obstoječi dialog sistem je statično drevo:** `controllers/DialogController.java`,
`roles/RoleDialog.java`, `quests/QuestDialog.java`. Dialog ima fiksne opcije in pogoje
razpoložljivosti (`EnumAvailabilityDialog`).

**[dokazano] `roles/JobConversation.java`** obstaja za pogovore med NPC-ji — uporabno kot vzor.

**[dokazano] Obstaja precedens za asinhrone omrežne klice:** `client/ImageDownloadAlt.java`
ustvari nit za prenos, **brez connect/read timeouta** (že zabeleženo v
`PLAN_IMPLEMENTACIJE.md` §5). Te napake ne ponovimo.

### Predlagana rešitev

**Arhitektura:**

```
rework/chat/
├── provider/     ChatProvider vmesnik + implementacije (Anthropic, OpenAI-kompatibilno)
├── persona/      system prompt, karakter, spomin, meje na NPC-ju
├── net/          async HTTP z timeouti, retry, rate limit, circuit breaker
└── gui/          nastavitve v NPC editorju
```

**Ključne zahteve:**

1. **Nikoli na glavni niti.** Klic gre v ločen executor, odgovor se vrne na server nit prek
   vrste. NPC med čakanjem pokaže "…" in ima timeout.
2. **API ključ.** V GUI se vnese, shrani se **v server config, ne v NPC NBT** — sicer gre
   ključ v clone tab, v world save in potencialno na klient. V NPC-ju je samo ime profila.
   Config datoteka ne sme iti v git (`.gitignore`).
3. **Persona na NPC-ju:** system prompt, ime, ton, meje, dovoljena dolžina odgovora,
   kaj NPC ve in česa ne.
4. **Spomin pogovora** z omejitvijo (zadnjih N izmenjav na igralca, TTL). Shranjuje se
   ločeno od NPC NBT, da ne napihuje clone datotek.
5. **Rate limit in strošek.** Na igralca in globalno. Konfigurabilen mesečni limit.
   Brez tega je en griefer drag račun.
6. **Fallback.** Ob napaki, timeoutu ali doseženem limitu NPC pade nazaj na statičen dialog.
7. **Ne pošiljaj osebnih podatkov.** Igralčevo ime je še sprejemljivo; UUID, IP, koordinate
   in vsebina inventarja ne gredo v prompt brez izrecne nastavitve.
8. **Integracija s skriptami (R7):** hook `onChatRequest` / `onChatResponse`, da skripta
   lahko spremeni prompt ali prestreže odgovor.

### Kaj je treba še preveriti

- Ali ima okolje, v katerem strežnik teče, sploh izhodni dostop do interneta.
- Kateri providerji: predlog je **pluggable vmesnik** + Anthropic + poljuben
  OpenAI-kompatibilen endpoint. Lokalni modeli (Ollama, LM Studio) so s tem samodejno
  podprti, ker ponujajo OpenAI-kompatibilen API.
- Ali odgovor teče na klient kot chat sporočilo ali skozi obstoječi dialog GUI.

---

## R9 — Clone follower: stanje `waiting` se lahko povrne v `following`

> *"Boljše shranjevanje v clone tabu. Starim NPC-jem ki jih imam dolgo shranjene se zna
> zgoditi da se jim JSON vrednosti kar pomešajo (npr. following gre iz waiting na following)."*

**Faza:** M1 · **Velikost:** majhna po pojasnilu uporabnika · **Tveganje:** nizko · **Prioriteta:** nizka po osnovnem popravku

Pojasnilo uporabnika 2026-09-11: ne gre za zbirko pokvarjenih datotek, temveč za minoren
robni primer pri kloniranju NPC-ja z vlogo follower in trenutno akcijo `waiting`; akcija se
lahko vrne v `following`. Splošne napake serializerja so bile kljub temu resnične in so že
popravljene. Brez konkretnega save primera ne porabljamo nadaljnjega časa za forenziko,
migracijo ali ugibanje specifičnega ključa.

### Stanje: POTRJENO EMPIRIČNO (2026-09-11)

Vse spodnje trditve so bile preverjene tako, da se je **prava originalna koda** iz
`dev/libs/customnpcs-mapped-01Oct19.jar` pognala proti pravim 1.12.2 NBT razredom iz
`forgeSrc-1.12.2-14.23.5.2847.jar`. Ni več hipotez. Polna meritev:
[`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md).

**Diferencialni fuzz, 4000 naključnih NBT struktur: original izgubi ali spremeni podatke v
1598 primerih (40 %).**

Dve prvotni hipotezi sta se izkazali za **napačni** — glej "Ovrženo" spodaj.
Implementacija popravka: `noppes/npcs/rework/data/NbtJson.java` +
shim `noppes/npcs/util/NBTJsonUtil.java`. Testi:
`local.customnpcs.NbtJsonBaselineTest`, `local.customnpcs.NbtJsonFuzzTest`.

### Kaj kaže koda

Vse v `dev/reference-src/noppes/npcs/util/NBTJsonUtil.java` in
`controllers/ServerCloneController.java`.

**[potrjeno] R9-a — long array se bere z `getByte()`:**

```java
// NBTJsonUtil.java:146
arr[i] = ((NBTTagLong)list.removeTag(0)).getByte();
```

Vsak `long` se prisekan na spodnjih 8 bitov. **Potrjeno:** `[L;1L, 300L, 1234567890123L]`
se po enem save/load cikla vrne kot `[L;1L, 44L, -53L]`.

**[potrjeno] R9-b — seznami se tiho pretvorijo v array tipe:**

```java
// NBTJsonUtil.java:124-141
if (list.getTagType() == 3) { … return new NBTTagIntArray(arr); }   // seznam intov → int array
if (list.getTagType() == 1) { … return new NBTTagByteArray(arr); }  // seznam bytov → byte array
if (list.getTagType() == 4) { … return new NBTTagLongArray(arr); }  // seznam longov → long array
```

Pravi `NBTTagList` intov se ob nalaganju spremeni v `NBTTagIntArray`. Ko ga koda nato bere z
`compound.getTagList(key, 3)`, dobi **prazen seznam**, ker se tip ne ujema. Rezultat: vrednost
izgine in polje pade na privzeto vrednost. **To je natanko oblika simptoma, ki ga opisuje
uporabnik** — nastavitev se sama "vrne nazaj".

**Potrjeno:** `NBTTagList` dveh intov se vrne kot `NBTTagIntArray` (tip 9 → 11), in
`getTagList(key, 3)` na rezultatu vrne **prazen seznam**. Enako za sezname bytov (9 → 7) in
longov (9 → 12).

**[potrjeno] R9-b2 — prazen tipiziran array izgubi tip.** `[B;]` (tip 7), `[I;]` (tip 11) in
`[L;]` (tip 12) se vsi vrnejo kot prazen `NBTTagList` (tip 9).

**[potrjeno] R9-c — neprepoznana vrednost tiho postane prazen string:**

```java
// NBTJsonUtil.java:82-84
NBTBase base = NBTJsonUtil.ReadValue(json);
if (base == null) { base = new NBTTagString(); }
```

Tiha zamenjava tipa namesto napake.

**[potrjeno] R9-d — nizi izgubijo vodilne presledke in prelome vrstic.**

`JsonFile.cut()` (vrstica 320) naredi `.trim()` na preostanku, tudi takoj po odpirajočem
narekovaju. **Potrjeno:** `"  abc"` se vrne kot `"abc"`, `"\nabc"` kot `"abc"`, `" "` kot `""`.
Vsako ime NPC-ja, vrstica dialoga ali opis predmeta z vodilnim presledkom se tiho spremeni.

**[potrjeno] R9-e — niz, ki se konča z `\`, naredi datoteko neberljivo.**

Obravnava ubežnih znakov (vrstice 156–161) po `\\` ostane v stanju `ignore`, zato poje
zaključni narekovaj in teče čez konec besedila. **Potrjeno:** `"path\"` vrže
`StringIndexOutOfBoundsException` in **cele datoteke ni več mogoče naložiti**.

**[potrjeno] R9-e2 — `NaN` in `Infinity` naredita datoteko neberljivo.**

`toLowerCase()` na vrstici 169 spremeni `NaNd` v `nand`; `Double.parseDouble("nan")` vrže
`NumberFormatException` → `JsonException`. **Potrjeno** za double in float.

**[potrjeno] R9-e3 — prazen ključ naredi datoteko neberljivo.**

`ReadTag` (vrstica 205) prazen ključ izpusti in zapiše samo vrednost; `FillCompound`
(vrstice 75–78) tako vrstico zavrne z *"Expected key after ,"*. **Potrjeno.**

**[potrjeno] R9-e4 — velik compound vrže `StackOverflowError`.**

`FillCompound` se rekurzivno kliče za vsak ključ (vrstica 93). **Potrjeno:** compound z 2000
ključi še gre, s 4000 vrže `StackOverflowError` pri privzeti velikosti sklada.

**[potrjeno] R9-e5 — obe smeri sta kvadratni.**

Compound s 1500 ključi (68 KB): zapis 235 ms, branje **2386 ms**. Nova implementacija:
2,9 ms in 6,5 ms. Ker se vse clone, dialog, quest, faction in player datoteke berejo ob
zagonu sveta, je to tudi prispevek k **R5**.

#### Ovrženo

Dve trditvi iz prvotne analize sta se ob preverbi izkazali za napačni. Zapisani sta tu, ker
je vzorec poučen:

- ~~R9-d (staro) — byte in long array se zapišeta neparsljivo.~~ **Napačno.**
  `NBTTagByteArray.toString()` v 1.12.2 vrne `[B;1B,2B]`, `NBTTagIntArray.toString()` vrne
  `[I;1,2]`; oba se pravilno preberata nazaj. Pokvarjen je samo long array, in sicer pri
  **branju** (R9-a), ne pri pisanju.
- ~~R9-e (staro) — `SaveFile` ne zapre writerja.~~ **Napačno.** `javap -c` na originalnem
  bytecode pokaže `OutputStreamWriter.close()` v obeh vejah (offset 36 in 49). Opozorilo
  CFR-ja *"Removed try catching itself"* je bilo artefakt dekompilerja.

To je natanko razlog za pravilo v `05-SEJA-PROTOKOL.md`: sumljivo logiko najprej preveri z
`javap`, šele nato popravljaj.

**[dokazano] R9-f — nevaren zapis clone datoteke (isti vzorec kot B1):**

```java
// ServerCloneController.java:143-149
File file  = new File(dir, filename + "_new");
File file2 = new File(dir, filename);
NBTJsonUtil.SaveFile(file, compound);
if (file2.exists()) { file2.delete(); }
file.renameTo(file2);                    // rezultat se ne preveri
```

Zaporedje je: zapiši novo → **izbriši staro** → preimenuj. Če `renameTo` na Windows ne uspe
(odprt file handle, antivirus, zaklenjena datoteka), je stara datoteka že izbrisana in
ostane samo `ime.json_new`. `getClones()` (vrstici 162–164) filtrira `.endsWith(".json")`,
zato tak NPC **izgine iz clone taba**, čeprav datoteka obstaja.

**[dokazano] R9-g — migracija starega formata briše vir prezgodaj:**

```java
// ServerCloneController.java:57-69
Map<…> clones = this.loadOldClones(file);
file.delete();                    // izbriše clonednpcs.dat
…
for (…) this.saveClone(tab, name, map.get(name));   // šele zdaj zapiše, brez preverbe
```

Enak vzorec kot B1 v `PLAN_IMPLEMENTACIJE.md`.

**[dokazano] R9-h — krhka refleksija:**

```java
// NBTJsonUtil.java:199
return ObfuscationReflectionHelper.getPrivateValue(NBTTagList.class, list, 1);
```

Dostop do privatnega polja **po indeksu**, ne po imenu. Ob katerikoli spremembi mappingov
tiho vrne napačno polje.

**[dokazano] R9-i — parser je rekurziven po ključu** (`FillCompound`, vrstica 93) in po
vrednosti — velika clone datoteka lahko povzroči `StackOverflowError` sredi nalaganja.

**[dokazano] R9-j — `toLowerCase()` na vsaki neoznačeni vrednosti** (vrstica 169) in
lomljivo sledenje narekovajem v `keyIndex()` (vrstice 297–309), ki ne obravnava
ubežnih narekovajev v ključu.

### Predlagana rešitev

1. ~~**Karakterizacijski testi najprej** (M1.1)~~ — **narejeno 2026-09-11.**
   `local.customnpcs.NbtJsonBaselineTest` (17 testov) in `local.customnpcs.NbtJsonFuzzTest`
   tečeta v obeh načinih: pod `testOriginal` trdita originalno napačno obnašanje, pod `test`
   popravljeno. Oba taska sta zelena.
2. ~~**Zamenjava serializerja** (M1.2/M1.3)~~ — **narejeno 2026-09-11.**
   `noppes/npcs/rework/data/NbtJson.java` + shim `noppes/npcs/util/NBTJsonUtil.java`.
   Format datotek je **nespremenjen** (4000/4000 fuzz primerov zapiše znak za znak enako
   besedilo kot original), zato za R9 ni potrebna migracija. Prvotno načrtovana točka:
   - eksplicitnim zapisom tipa za vsako vrednost (ni ugibanja iz suffiksa)
   - polno podporo za vse NBT tipe, vključno z `byte[]`, `int[]`, `long[]` in praznimi seznami
   - iterativnim parserjem (brez rekurzije)
   - `StringBuilder` namesto konkatenacije
   - `Files.readAllBytes` / `BufferedWriter` s `flush()` in `close()`
3. **Bralna združljivost za že pokvarjene datoteke — odloženo.** Uporabnik takih datotek
   nima; brez konkretnega primera ne ugibamo tipov ali porabljamo časa za migrator.
4. **Atomski zapis** (M1.4): skupen `SafeFileWriter` v `rework/data/` —
   temp datoteka v isti mapi → `flush` + `sync` → `close` → **preveri z branjem** →
   `Files.move(ATOMIC_MOVE)` z fallbackom → šele nato pobriši backup.
   **Vsi** controllerji (Clone, Bank, Faction, GlobalData, Transport, Recipe, Spawn, Script)
   gredo skozenj. To je hkrati popravek B1.
5. **Verzioniranje — ni potrebno za R9.** Novi serializer ohranja isti format.
6. **Orodje `auditClones` — odloženo.** Ponovno se odpre samo ob konkretnem poškodovanem saveu.

### Kaj je treba še preveriti

- Specifičnega `waiting` → `following` simptoma brez uporabnikove datoteke ne reproduciramo;
  po njegovi oceni ni vreden dodatnega razvojnega časa.
- Ali so bile te napake **dovolj** za uporabnikov simptom. Potrjene napake se sprožijo pri
  seznamih bytov/intov/longov, praznih tipiziranih arrayih, nizih z vodilnim presledkom in
  posebnih številskih vrednostih. Lastni `NBTTags` helperji CustomNPCs-a vsak int zavijejo v
  compound, zato jih R9-b ne zadene — zadene pa vsak tak vzorec v vanilla entity NBT-ju,
  v NBT-ju predmetov in v podatkih drugih modov, shranjenih na NPC-ju.
- Če se prioriteta kdaj spremeni, je treba najprej identificirati konkreten ključ v
  `RoleFollower` / `JobFollower`, ne širiti splošnega migracijskega sistema.

---

## Povzetek odvisnosti

```
R9 (M1) ──┬──> R7 (M6) ──┬──> R4 (M7)
          │              └──> R8 (M9)
          └──> R3 (M8)
          
R1 (M3) ──┬──> R2 (M4) ──> vozila/letala (po M8)
R6 (M3) ──┘

M2 (meritve) ──> R5 (M5) ──> R7 (M6)  [odstranitev script locka je skupna]
```

Osnovni R9 serializer je končan. Kritična pot je zdaj **B1/B2 → M2 → R1/R6 → R2/R5 →
R7 → R4**; dodatna R9 forenzika ni več na kritični poti.
