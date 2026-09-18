# 06 — Formacije: skupina NPC-jev kot ena enota (M4.14)

Zasnova paketa `noppes.npcs.rework.formation`. Dokument opisuje, **zakaj** paket obstaja,
**kako** deluje, **kaj** je preverjeno in kaj ne ter kako se ga preveri v svetu.

| | |
|---|---|
| Nastalo | 2026-09-18, seja 39, na izrecno zahtevo uporabnika |
| Zahteva | ni R1–R9; najbližje R5 (performance pri veliko NPC-jih) in kopenski del M4 |
| Odločitev | [D-021](01-ARHITEKTURA.md) |
| Koda | `dev/src/patch/java/noppes/npcs/rework/formation/` (15 datotek) |
| Testi | `FormationGeometryTest` (14), `SquadPlannerTest` (7) — 21 zelenih v seji |
| Stanje | **koda in testi narejeni (M4.14a); v svetu še ni bilo pognano (M4.14b)** |

---

## 1. Izhodišče: kaj dela uporabnikova skripta in zakaj razpade

Uporabnik vodi vojsko NPC-jev s chat skripto (`legija`, `obramba`, `march`, `recruit` …).
Premik izvede tako, da za vsakega NPC-ja razdeli ravno črto do cilja na do 16 točk in za
vsako točko nastavi **timer na igralcu**; timer ob sprožitvi pokliče `navigateTo`.

Ugotovitve iz kode, ne iz občutka:

| # | Vzrok | Kje | Posledica v igri |
|---|---|---|---|
| 1 | Vsak NPC išče svojo pot. N NPC-jev = N neodvisnih A\* iskanj | `EntityLivingWrapper.navigateTo` → `tryMoveToXYZ` | pri vratih vsak izbere malo drugo pot, formacija razpade; cena raste linearno z N |
| 2 | Timerji so **odprta zanka**: čas med točkami je izračunan vnaprej iz ocenjene hitrosti | skripta, `schedule_move` | kdor se zatakne, dobi naslednjo točko vseeno; nihče ne počaka, nihče ne pohiti |
| 3 | Vmesne točke so linearna interpolacija, tudi `y` | skripta | na hribu so točke v zraku ali v zemlji; pathfinder jih nekako popravlja |
| 4 | `navigateTo` čez več kot `NpcNavRange` (32) vrne **delno** pot | `PathFinder.findPath:65`, `EntityNPCInterface:334` (glej M4.10) | NPC obstane na koncu delne poti |
| 5 | `setMovingType(0)` pusti `EntityAIReturn` aktiven: ko NPC nima poti in ni na domu, gre **domov** | `EntityAIReturn.shouldExecute` (movingType 0 → `!isVeryNearAssignedPlace()`) | NPC se po prihodu vrne na staro mesto, razen če ima izklopljen "return to start" |
| 6 | Do 360 timerjev in `tempdata` ključev, vse na igralcu | skripta | ob odjavi igralca premik obvisi |

Ključna je vrstica 2: skripta **ne vidi**, kje so NPC-ji, zato jih ne more držati skupaj.
Tega se v JavaScriptu s timerji ne da popraviti, v Javi pa je to nekaj vrstic.

## 2. Kaj paket naredi drugače

1. **Ena pot za celo enoto.** Pot izračuna en sam član (vodja, najbližji središču) z
   vanilla navigatorjem. Ostali poti ne iščejo, ampak sledijo svojemu **mestu** v
   formaciji. To je M5.6 (deljenje poti), omejeno na eno enoto in brez predpomnilnika.
2. **Sidro in mesta.** Formacija ima sidro, ki potuje po skupni poti. Mesto člana je sidro +
   odmik (`Slot`), zasukan za smer formacije. Smer sledi poti z največ 4° na tick, zato se
   široka formacija na zavoju zasuka kot celota, namesto da bi drsela.
3. **Zaprta zanka.** Sidro gre s hitrostjo `0,8 × najpočasnejši član`. Če najslabši član
   zaostaja več kot 2 bloka, sidro upočasni; pri 5 blokih stoji. Član, ki zaostaja, dobi
   večjo hitrost; član pred mestom manjšo.
4. **Ne čaka večno.** Član, ki enoto zadržuje več kot 80 tickov (zaostanek nad 4 bloki), je
   *opravičen*: enota gre naprej, on jo lovi z iskanjem poti. Član dlje od 14 blokov je
   *zaostanek* in enote ne zadržuje; če je zaostankov več kot polovica, enota stoji.
5. **Ozko grlo.** Če na mestu člana ni mogoče stati (zid, luknja), gre član na točko na poti
   za sidrom, po vrstnem redu vrst. Formacija se pred vrati sama stisne v kolono in se za
   njimi spet razpre. Ni posebne logike za vrata; zadošča vprašanje "ali se tu da stati".
6. **Faze.** `FORMING` (postroji se na začetku poti) → `MARCHING` → `SETTLING` (vsak gre na
   končno, na sredino bloka poravnano, unikatno mesto) → `DONE`. Pri poti, krajši od 12
   blokov, ali z zastavico `takoj` se `FORMING` in `MARCHING` preskočita.
7. **Sidranje.** Ob koncu se `startPos` (dom) in `orientation` vsakega člana nastavita na
   novo mesto. `EntityAIReturn` ga zato drži tam, namesto da bi ga odpeljal na staro mesto
   (vzrok 5). Zastavica `brezsidra` to izklopi.
8. **Dodelitev mest brez križanja.** Skripta je vsakemu mestu dala najbližjega prostega
   NPC-ja (požrešno, križanja). Tu se člani in mesta uredijo v istem lokalnem sistemu:
   sprednji člani dobijo sprednjo vrsto, v vrsti od leve proti desni. Pri krogu po kotu, z
   zasukom, ki minimizira vsoto kvadratov razdalj.

## 3. Kako član hodi

Član ima vsak tick enega od treh ukazov:

| Ukaz | Kdaj | Izvedba |
|---|---|---|
| `HOLD` | na mestu | počisti pot, obrne se v smer formacije (krog: stran od središča) |
| `STEER` | do 6 blokov od cilja | navigator dobi **pot z eno samo točko**, med pohodom 2 bloka pred mestom. Iskanja ni. Hojo, skok na stopnico in animacijo naredita vanilla `PathNavigate` in `EntityMoveHelper`; CustomNPCs vidi `Walking = 1` kot pri vsaki poti |
| `PATH` | dlje od 6 blokov, obtičal (< 0,5 bloka premika v 30 tickih) ali opravičen | pravo iskanje poti `tryMoveToXYZ`; največ 4 na enoto na tick in največ enkrat na 20 tickov na člana |

Hitrost: planer računa v blokih na tick, navigator pa hoče množitelj atributa. Na tleh je
končna hitrost približno `2,2 × (atribut × speedIn)²` — pospešek je kvadraten, ker
`EntityMoveHelper` nastavi `moveForward` in `AIMoveSpeed` na isto vrednost, trenje tal je
0,546. Formula se uporabi za pretvorbo; napako popravi zaprta zanka. Uporabnikova skripta
je imela isti kvadrat v `movement_speed` (`nav_speed² × …`).

`hitrost` v ukazu je v **enotah skripte** (`nav_speed`, privzeto 3 za razpored in 2 za
march), da se občutek ne spremeni.

## 4. Kako se vključi v NPC-ja, ne da bi spremenili `EntityNPCInterface`

`EntityNPCInterface` še ni prenesen (M3.1), zato ga paket ne spreminja:

- Vsak član dobi `FormationMoveTask` s prioriteto **−1** (CustomNPCs šteje od 0 navzgor) in
  masko `PASSIVE | LOOK`. Dokler teče, ne tečejo wander, return-home, moving path, follow in
  watch-closest — natanko tisto, kar se je v skripti tepelo z `navigateTo`.
- CustomNPCs ob vsaki spremembi AI nastavitev pobriše vse taske (`updateTasks` →
  `clearTasks`). Enota zato task vsak tick preveri in ga po potrebi vrne.
- Task se doda in odstrani **samo na začetku ticka sveta**, nikoli iz klica skripte, ker
  lahko skripta teče med tickom NPC-ja, ko vanilla `EntityAITasks` iterira po taskih.
- Boj: privzeto član v boju (`isAttacking()`) zapusti formacijo, njegov task se ustavi in
  napadalni task prevzame. Po boju se vrne na mesto. Zastavica `drzi` to izklopi: task
  ostane aktiven in zaradi maske napadalni task ne more začeti.
- Član, ki se 40 tickov ni posodobil (chunk ni naložen), izpade iz enote.

## 5. Vmesnik

### Ukaz (dovoljenje 2)

```
/rwsquad legija  <ime> <x> <y> <z> [sirina=5] [yaw|~] [hitrost=3] [doseg=100] [zastavice]
/rwsquad obramba <ime> <x> <y> <z> [polmer=5] [hitrost=3] [doseg=100] [zastavice]
/rwsquad kolona  <ime> <x> <y> <z> [vrst=2]   [yaw|~] [hitrost=3] [doseg=100] [zastavice]
/rwsquad march   <ime> <x> <y> <z> [hitrost=2] [doseg=100] [zastavice]
/rwsquad stop [ime] [doseg]
/rwsquad status
```

Zastavice: `drzi`, `brezsidra`, `takoj`. Koordinate sprejmejo `~`. Yaw `~` (privzeto) je
smer pošiljatelja, zaokrožena na 90°, kot v skripti. NPC-ji se iščejo po imenu v kvadru
`doseg` okoli pošiljatelja. Vsak odgovor gre tudi v log z markerjem `RWSQUAD`, ob koncu
enote pa `RWSQUAD konec …` z vsemi števci.

### Skripta

Obstoječi script API se ne spremeni (kompatibilnostna politika). Skripta pride do razreda z
`Java.type`, ker Nashorn v CustomNPCs nima `ClassFilter`-ja (`ScriptController:70`):

```js
var F = Java.type("noppes.npcs.rework.formation.FormationApi");
var npcs = event.player.world.getNearbyEntities(x, y, z, 100, 2);
event.player.message(F.legija(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, 5, 90, 3, ""));
```

Metode: `legija`, `obramba`, `kolona`, `march`, `move`, `stop`, `status`. Celoten primer,
predelana uporabnikova chat skripta: [`formacije/vojska-chat.js`](formacije/vojska-chat.js).

### Stikalo

`-Drwformation=off` izklopi ukaz in API. Brez ukaza ali klica iz skripte paket **ni
prijavljen na event bus in ne doda nobenega taska**, zato je obnašanje obstoječih svetov
enako originalu in cena nič (D-007).

## 6. Parametri

Vsi so konstante v `SquadPlanner` in `Squad`. Izbrani so po presoji, **ne po meritvi**;
M4.14b jih mora potrditi ali popraviti.

| Parameter | Vrednost | Pomen |
|---|---|---|
| `CRUISE_FRACTION` | 0,8 | sidro gre z 80 % hitrosti najpočasnejšega |
| `LAG_SLOW` / `LAG_STOP` | 2 / 5 blokov | začetek upočasnjevanja / sidro stoji |
| `HOLDING`, `WAIT_LIMIT` | 4 bloki, 80 tickov | kdaj je član opravičen |
| `STRAGGLER` | 14 blokov | član ne zadržuje več enote |
| `STEER_MAX` | 6 blokov | dlje od tega pravo iskanje poti |
| `TURN_RATE` | 4 °/tick | zasuk formacije |
| `TRAIL_SPACING` | 1,4 bloka | razmik v koloni skozi ozko grlo |
| `FORM_SKIP` | 12 blokov | krajša pot brez postrojitve |
| `PATH_BUDGET`, `PATH_INTERVAL` | 4 na tick, 20 tickov | omejitev pravih iskanj |
| `LEAD` | 2 bloka | točka pred mestom med pohodom |
| `STUCK_WINDOW`, `STUCK_MOVE` | 30 tickov, 0,5 bloka | zaznava obtičanja |

## 7. Kaj je preverjeno in kaj ne

**Preverjeno v seji** (D-014: `javac --release 8` proti mapiranim razredom, JUnit):

- ves paket se prevede proti pravim podpisom Forge/Minecraft/CustomNPCs
  (`EntityAITasks.taskEntries`, `PathNavigate.setPath`, `DataAI.setStartPos`,
  `ais.orientation`, `updateClient` …);
- geometrija: konvencija yawa (desno je pri yaw 0 −X), oblike z razmikom iz skripte,
  dodelitev brez križanja, pot sidra po dolžini loka (14 testov);
- planer v simulaciji s točkastimi člani: 20 članov prehodi 80 blokov in noben med pohodom
  ne zaostane več kot `LAG_STOP + 0,5`; sidro se prilagodi najpočasnejšemu; enota počaka
  zaostalega, obtičanega pa po 80 tickih opraviči in gre naprej; pred vrati v zidu se
  stisne (≥ 4 člani v koloni) in za njimi spet razpre (0); delna pot se nadaljuje; prazna
  nadaljevanja končajo s `cilj_nedosegljiv`; končna mesta so na sredini blokov in unikatna;
  krog na koncu gleda ven (7 testov).

**Ni preverjeno** (to je M4.14b in ga poganja uporabnik):

- nalaganje moda z novim ukazom in dejansko obnašanje v svetu;
- ali pot z eno točko res da gladko hojo brez trzanja (`pathFollow` konča točko pri
  ~0,45 bloka; `LEAD` 2 to naj bi preprečil);
- ali formula za hitrost drži za NPC-je CustomNPCs (atribut = `walkingSpeed / 20`);
- leteči in plavajoči NPC-ji (drug navigator in move helper) — **niso testirani**;
- interakcija z `EntityAIReturn` po sidranju pri NPC-jih z `bodyOffset` ≠ 5;
- cena na tick pri 50/200 članih (brez M2.4 ni primerjave).

## 8. Preverjanje v svetu — scenarij M4.14b

Podrobnosti v [`scenariji/M4.14-formacije.md`](scenariji/M4.14-formacije.md). Na kratko:
isti dve progi kot M2.7 (G = zid z enimi vrati, O = odprto), 16 NPC-jev, en zagon s
skripto (`navigateTo` s timerji, izhodišče) in en z `/rwsquad legija`. Veličine M2.7
(prispelo, razpon pri grlu, iskanj na tick) so deterministične (M2.5c), zato zadošča en
zagon na stran.

## 9. Omejitve in tveganja

- **Ni obstojno.** Enote živijo samo v pomnilniku; ob ponovnem zagonu strežnika izginejo.
  Obstojno je samo sidranje (dom in orientacija) po koncu.
- **En NPC je lahko samo v eni enoti.** Nov ukaz ga iz stare enote izpusti.
- **Pot z eno točko ne preverja ovir med članom in mestom.** Če je vmes zid, zaznava
  obtičanja v 30 tickih preklopi na pravo iskanje poti. Za vmesne luknje (padec) ni zaščite,
  razen tega, da mesto samo mora biti stojno.
- **Števec M2.7 "iskanj na tick" bo med pohodom višji,** ker zbiralnik šteje spremembe
  identitete objekta `Path` (D-015), pot z eno točko pa se zamenja vsakič, ko se ciljni blok
  premakne. To niso iskanja. Pri A/B je treba to ločiti (M4.14b).
- **D-012** zavrača flow fielde in lasten gibalni sklad za *vse* NPC-je. Ta paket ni ne eno
  ne drugo: iskanje poti ostane vanilla, hojo izvajata vanilla `PathNavigate` in
  `EntityMoveHelper`, vklopi pa se samo z ukazom. Glej D-021.
