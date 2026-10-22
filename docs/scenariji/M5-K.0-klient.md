# Scenarij M5-K.0 — klientski merilni harness (M2.4r)

**To je merilni paket, ne popravek.** Dostavi meritev časa sličice razvojnega klienta z igralcem,
na kateri M5-K.1 izmeri baseline originala in pod katero se meri vsak K-paket (D-033).

Predhodniki: [`M2.4-obremenitve.md`](M2.4-obremenitve.md) (celice in prizorišče, ki jih harness
prevzame), [`M2.5c-ponovitve.md`](M2.5c-ponovitve.md) (ponovitve, šumni pas).

---

## Sestavni deli

| del | vloga |
|---|---|
| `k-run.ps1` | ena celica na zagon: zapiše navodilo, zažene `runClient -PrwHarness=<navodilo>`, preveri merila K0–K8 |
| `rework/diag/HarnessJob` | bralnik navodila (properties), brez Minecraft tipov, JUnit `HarnessJobTest` |
| `rework/diag/ClientHarness` | stroj stanj v klientu (`@SideOnly(CLIENT)`), namesti ga `CustomNpcs` prek refleksije **samo** z `-Drwharness` |
| `rework/diag/FrameStats` | p50/p95/p99/max, FPS, 1 % in 0,1 % low iz časov sličic, JUnit `FrameStatsTest` |
| `ponovitve-run.ps1 -Scenarij klient` | tri ponovitve in šumni pas (D-016) |

Brez lastnosti `rwharness` se v igri ne spremeni nič; `options.txt` se ne shrani.

## Potek v klientu

glavni meni → integrirani svet `rwk-<varianta>-<N>` (vsak zagon **svež** FLAT svet s semenom
`customnpc-rework`, fixture `PERF_*` iz `dev/testworld/customnpcs`) → spectator + teleport →
5 s nalaganja chunkov → ukazi prizorišča (isti generator kot `perf-run.ps1`, prebran z AST, ne
prepisan) → umiritev in štetje NPC-jev → ogrevanje (privzeto 30 s) → merjenje (privzeto 60 s,
razlika `System.nanoTime()` med zaporednima `RenderTickEvent` START) → JSON → izhod.

Nastavitve: VSync izklopljen, `limitFramerate` 260 (neomejeno), brez view bobbinga, RD 8, FOV 70.
**GUI je od M5-K.1 (18. 10.) viden:** F1 (`hideGUI`) v 1.12 skrije tudi vsa imena NPC-jev
(`canRenderName` → `isGuiEnabled`), zato je M5-K.0 meril brez imen; staro vedenje `-SkrijGui`. Kamera: južno od mreže, yaw 180, pitch proti sredini; najbolj oddaljen NPC mora biti
≤ 60 blokov (domet sledilnika `tracker(64, …)`), sicer ga strežnik ne pošlje.

## Merila

| | merilo |
|---|---|
| K0 | pred zagonom ne teče tuj gradle build ali Minecraft (zaklep `.scenarij.lock`) |
| K1 | klient je zapisal JSON v fazi `MEASURE`, brez napak |
| K2 | vsi ukazi prizorišča so uspeli (`fill … air` brez spremembe je dovoljen) |
| K3 | v klientu je natanko pričakovano število NPC-jev (`boj`/`nedosegljiva`: N + kontrola) |
| K4 | upodobljenih entitet v ≥ 80 % sekundnih vzorcev ≥ N (idle, skripte) oz. ≥ 0,9 N (boj) |
| K5 | kamera fiksna: odmik ≤ 0,01 bloka in ≤ 0,01° |
| K6 | VSync 0, limit ≥ 260, RD in FOV kot zahtevano |
| K7 | vsaj 10 sličic/s in trajanje merjenja ±2 s |
| K8 | med ogrevanjem in merjenjem ni tekel tuj java proces |
| K16 | (s `-Far`, M5-V.6) L2 po geometriji prizorišča, brez nepečenih in napak — glej spodaj (M5-K.2: brez `-Dalec 1` natanko 0) |

## Uporaba

```
.\k-run.ps1 -Variant idle -N 50 -Warmup 10 -Measure 20     # hiter preizkus
.\k-run.ps1 -Variant idle -N 200
.\ponovitve-run.ps1 -Scenarij klient -Dodatno @('-Variant','idle','-N','200')
.\k-run.ps1 -Variant idle -N 500 -Vzorci 1     # M5-K.1: profil render niti (StackSampler), ni baseline
.\k-run.ps1 -Variant idle -N 50 -Jfr           # JFR; za render nit premalo vzorcev (M5-K.1)
```

`-Vzorci <ms>` doda merilo **J3** (vsaj 100 vzorcev/s) in povzetek `<izhod>-skladi.md`
(`dev/tools/skladi-povzetek.js`); `-Jfr` doda J1/J2. Oba stanejo čas sličice, zato nista baseline.

### M5-V.6: L2 (`-Dalec 1 -Far 0|2 -ImpostorStart 56`, merilo K16)

```
.\ponovitve-run.ps1 -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','500','-Dalec','1','-RenderDistance','10','-Far','0')
.\ponovitve-run.ps1 -Scenarij klient -Ponovitev 3 -Dodatno @('-Variant','idle','-N','500','-Dalec','1','-RenderDistance','10','-Far','2','-ImpostorStart','56')
```

- **Prizorišče `-Dalec 1`**: ista mreža kot idle, kamera 40 blokov nad tlemi (y 44), najbližja vrsta 36 blokov
  vodoravno (pogled navzdol na množico, npr. z vzpetine na mesto). Vsi NPC-ji so znotraj sledilnika (Chebyshev
  ≤ 60, sicer jih strežnik ne pošlje) in dometa upodobitve vanille (RD 10: 80 blokov, NPC z robom 1,0), zato K4
  (upodobljenih ≥ N) velja tudi pri L2 — `countEntitiesRendered` šteje entiteto, preden `RenderLivingEvent.Pre`
  prekliče izris. Razdalje 54,2–60,3 (200) in 54,2–69,5 (500) blokov; pri pragu 56 je v L2 **137 od 200** in
  **437 od 500** NPC-jev.
- **K16**: `RwFarView` kot zahtevano; z 0 nobenega L2 (0 preklicev); z 2 je število impostorjev L2 v vsaki
  sličici (min in max) enako geometriji prizorišča (getDistanceSq od nog NPC-ja do igralca, kot `FarRender`)
  ±2 %, v merilnem oknu 0 NPC-jev brez pečenega videza, 0 napak izračuna videza, 0 izven dometa upodobitve,
  poseg coremoda uspel in 0 sličic prek rezerve.
- A/B (D-016): 3 + 3 ponovitve na N, primerjava p50/p95/p99 in FPS; izid v `docs/meritve/`.

### M5-V.7: posebni NPC-ji (`-Posebni 1`, merilo K17)

```
.\k-run.ps1 -Variant idle -N 50 -RenderDistance 10 -Dalec 1 -Far 2 -ImpostorStart 56 -Posebni 1
```

- Potrebuje `-Dalec 1 -Far 2`. Poleg mreže 9 klonov `RWV7_*` v vrsti 4 bloke za mrežo (vsi onkraj praga 56):
  `Velik` (`Size 15`), `Polviden` (`NpcVisible 2`), `Nevidni` (`NpcVisible 1`), `Kip` (`IsStatue`, brez hitboxa →
  izven dometa upodobitve vanille), `Jahac` na `Nosilec` (`rwhitbox mount`), `Mrtev` (ubit, `RespawnTime` 100000),
  `Morf` (`EntityClass` prašič), `Url` (URL skin z lokalnega `dev/tools/skin-streznik.js` na 127.0.0.1:18731, odgovor
  šele po 4 s, magenta `FF00FF`). Harness šteje po imenu (`specialPrefix=RWV7_`): ponujen, preklican L2,
  pol-viden, izven dometa, med jahanjem; povprečna barva pečenega okvirja 0.
- **K17a–j**: vsi v svetu; velik, morf in URL so impostor; pol-viden pol-prozoren impostor; neviden, ubit,
  jahač in nosilec niso impostor; `Kip` se ne ponudi, a se nariše kot impostor izven dometa; URL okvir magenta
  (R in B vsaj 2·G + 40), peka je čakala na prenos (`impPrenosCakanj` > 0), strežnik skina dobil ≥ 1 zahtevo;
  brez OptiFine `FarCompat` neaktiven (0 izklopljenih sličic, 0 senčnih preskokov). K16 prišteje geometriji mreže 5 L2
  na sličico (velik, pol-viden, kip izven dometa, morf, URL) in pričakuje izven dometa 1.

### M5-K K12: okvir upodobitve (`-Posebni 1 -RenderBox 0|1`, merilo K18)

```
.k-run.ps1 -Variant idle -N 50 -RenderDistance 10 -Dalec 1 -Far 2 -ImpostorStart 56 -Posebni 1 -RenderBox 1
```

- `-RenderBox` nastavi `RwRenderBox` za zagon (`renderBox` v poslu; -1 = iz configa) in doda NPC `RWV7_Mec` (diamantni
  meč v roki); brez `-RenderBox` ostane prizorišče M5-V.7 nespremenjeno. Harness ob koncu merjenja zapiše `okvir`
  (`RenderBoxAudit`): za vse NPC-je ali okvir objame hitbox, ali je NPC v dometu od kamere, in koliko pečen obris
  modela iz atlasa L2 sega čez okvir (meja 0,05 × Size/5 bloka).
- **K18a–e** pri 1: okvir objame hitbox pri vseh; obris za 6 posebnih; izven dometa 0 (`Kip` v dometu); obris ne sega
  čez okvir. Pri 0 (občutljivost): izven dometa ≥ 1 (`Kip` izven), obris `Kip` sega > 0,3 bloka čez hitbox.
  K16/K17e se prilagodita: pri 1 je `Kip` v dometu vanille in preklican L2 (izven dometa 0).
- A/B cene: `ponovitve-run.ps1 -Scenarij klient -Ponovitev 3 -Dodatno -Variant,idle,-N,500,-RenderBox,0|1`.

### M5-K.2: sprejem klienta (original proti vsem sprejetim privzetim stikalom)

```
.\k-run.ps1 -Variant idle -N 500 -Pokrivalo 0 -Napisi 0 -Far 0 -RenderBox 0                       # original
.\k-run.ps1 -Variant idle -N 500 -Pokrivalo 1 -Napisi 1 -Far 2 -ImpostorStart 56 -RenderBox 1     # privzeto
# prizorisce M5-V.6: dodaj -Dalec 1 -RenderDistance 10
```

- `dev/run/config/CustomNpcs.cfg` ostane nespremenjen (vsa stikala 0, original). Obe strani nastavita **vsa** klientska
  stikala z odločitvijo o privzeti vrednosti eksplicitno prek posla (`headwearSkip`, `labelCache`, `farView`,
  `impostorStart`, `renderBox`), zato K10, K12 in K16 v vsakem zagonu potrdijo, da je bila vrednost res uporabljena.
  `RwLodStart` (privzeto 0 = original, D-039) in `RwImpostorAtlas`/`RwFarShaders` (privzeto = vrednost v configu) ostanejo
  iz configa. Strežniška stikala M5-S integriranega strežnika ostanejo 0 (sprejeta so z M5.8; tu se meri klient).
- **K12** pri `-Napisi 1` in idle: pričakovanje napisov iz geometrije (`renderName` riše ime samo do √512 = 22,6 bloka);
  v idle-500 in `-Dalec 1` je vsa mreža dlje, zato 0 napisov z listom. Pas ±1 blok okoli meje sprejme oboje.
- **K16** pri `-Far 2` brez `-Dalec 1`: vsa mreža je bliže od praga 56, zato natanko 0 L2; z `-Dalec 1` mora biti L2 > 0.
- Test ID-ji: `k2-{200,500}-{orig,priv}` in `k2-dalec-{200,500}-{orig,priv}` (vsak `ponovitve-run -Scenarij klient`, 3 zagoni).

## Omejitve (znane)

- Integrirani strežnik teče v istem procesu; njegov MSPT se zapiše (`integriraniStreznikMsPovp`),
  a deli CPU s klientom. Celice z dediciranim strežnikom so M5-V.1.
- Različice videza (oklep, ime, oblački, delci, URL skini) iz opisa paketa še niso v fixturah;
  dodajo se v M5-K.1, ko profil pokaže, katere so pomembne.
- Parametra `-Oci`/`-PreveriOci` (merili K13/K14, M5-K K3) sta odstranjena v K3c 22. 10.; zgodovinski zagoni z
  `-Oci` se ne dajo ponoviti na sedanji kodi (za to je commit 49bd247).
- Ločljivost okna je privzeta (854 × 480) in se zapiše v JSON (`sirina`, `visina`).
