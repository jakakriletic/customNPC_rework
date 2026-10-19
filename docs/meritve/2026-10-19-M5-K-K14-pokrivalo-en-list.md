# M5-K K14 — pokrivalo v enem display listu (19. 10. 2026) — **zavrženo**

Paket K14 v [03-FAZE](../03-FAZE.md), izhodišče profil [M5-K.1](2026-10-18-M5-K.1-baseline-profil.md)
(pokrivalo 19–26 % render niti, 6,7 µs na NPC). Scenarij [M5-K.0](../scenariji/M5-K.0-klient.md), isti
pogoji kot baseline M5-K.1 (RD 8, FOV 70, 854 × 480, GUI viden, 30 + 60 s, svež svet vsak zagon).
Koda poskusa: commit `03ab3bc`, odstranjena v `4370184` (vrne se z `git revert 4370184`).

## 1. Kaj je bilo narejeno

- **Prenos 1:1** `Model2DRenderer` in `ModelHeadwear` v `src/patch` (`b896cfc`): `primerjaj-bytecode`
  25 metod, 24 enakih, 1 razlika v obliki, 0 za pregled ([audit](../../audit/m5k14-bytecode-primerjava.txt));
  `ModelHeadwearBaselineTest` (6 otrok, točke vrtenja, koti, merila, debelina, uv). **Ostane.**
- **Poskus** pod stikalom `RwHeadwearList` (privzeto 0): `rework/render/HeadwearList` posname zaporedje
  `push` / `postRender` / `callList` / `pop` vseh 6 otrok v en list — način 1 kliče nespremenjene liste
  otrok, način 2 je raven list z geometrijo. `HeadwearState` spremlja točke vrtenja, kote, `showModel`
  in `isHidden`; ob spremembi se list prezgradi (po 8 prevezavah original).

**[K]** Otroci so konstantni: nastavi jih konstruktor `ModelHeadwear`, primerek je zaseben v
`LayerHeadwear` (`preRender` nastavi samo `config = null` in `bipedHeadwear.isHidden`), `boxList` in
`childModels` pokrivala nihče drug ne bere ali piše (`reference-src`, `customNPC_entities_mod`). **[M]** Med
igro 0 prevezav (merilo K10, ~1 M izrisov z listom v 20 s).

## 2. Dokaz enakosti slike

`HeadwearCheck` (harness, `k-run -PreveriPokrivalo`): v eni sličici (`RenderWorldLastEvent`, iste matrike,
isti `partialTicks`) se vseh 200 NPC-jev idle-200 nariše v lasten framebuffer v načinih 0 → 1 → 2 → 0,
po vsakem prehodu se prebere barva (RGBA8) in globina (float) vseh pikslov.

| serija | pikslov z NPC-ji | razlik barva / globina, način 1 | način 2 | ponovitev 0 → 0 |
|---|---|---|---|---|
| skin fixture (`humanmale/steve.png`) | 37.317 | 0 / 0 | 0 / 0 | 0 |
| neprozorna testna tekstura (vsak teksel svoja barva) | 39.022 | 0 / 0 | 0 / 0 | 0 |

Brez pokrivala (`HeadWearType=0`) se s testno teksturo spremeni 11.838 pikslov — preverba je občutljiva.
S skinom fixture pa je slika z in brez pokrivala **enaka**: območje klobuka v `steve.png` je v celoti
prozorno, alfa test zavrže vse fragmente pokrivala (zato druga serija).

## 3. A/B (3 ponovitve, `audit/m5k14-ab.ps1`; mediana, razpon v oklepaju)

| celica | p50 original | p50 en list (način 1) | p95 original | p95 en list |
|---|---|---|---|---|
| idle-500 | 11,589 (11,51–11,62) | 11,423 (11,41–11,59) | 12,810 | 12,638 |
| idle-200 | 6,538 (6,535–6,546) | 6,590 (6,571–6,636) | 7,048 | 7,170 |
| idle-500, **`HeadWearType=0`** (zgornja meja) | 11,589 | **8,907** (8,85–9,01) | 12,810 | **10,029** |

T1–T6 in K0–K8 zelena v vseh serijah. Posamezni zagoni idle-500 način 0 / 1 / 2: p50 11,41 / 11,28 / 11,37.

**Izid:** en list ne preseže šuma (idle-500 −1,4 % s prekrivajočima razponoma, idle-200 +0,8 %) →
**K14 zavržen** (05 §2). Način 2 (raven list) ni boljši od načina 1.

## 4. Zakaj — profil z enim listom

`k-run -Variant idle -N 500 -Pokrivalo 1 -Vzorci 1`: pokrivalo **26,1 %** render niti (baseline 26,0 %),
lastni čas `nglCallList` 58,7 %. Čas ni v ~30 klicih GL na NPC (JNI, matrike), temveč v gonilniku, ki
izvaja vsebino listov: 6 delov × 34 štirikotnikov (2 lici + 32 trakov ekstruzije `renderItemIn2D`) =
**816 oglišč na NPC**, več kot ves model (~300). **[M]** Zgornja meja brez 6 delov (ena vanilla kocka
24 oglišč): idle-500 p50 −23 %, p95 −22 %.

Napaka premise v M5-K.1: delež sklada v `nglCallList` je bil pripisan številu klicev; je pa čas izvajanja
vsebine lista. Pravilo za naslednje K-pakete: **profil pove, kje je čas, ne pa, ali pomaga manj klicev** —
pred paketom »manj klicev« preveri zgornjo mejo (izpusti del in izmeri).

## 5. Posledica: K14b — manj geometrije pokrivala ob isti sliki

Teksel z alfo 0 ne da nobenega fragmenta (alfa test entitet `GL_GREATER`, prag 0,1 ali 0,0039 pri nevidnih)
in ne piše globine, ker alfa test zavrže fragment pred testom globine **[K]**. Del ali trak ekstruzije, ki
vzorči samo take teksle, je torej neviden. Pregled 239 humanoidnih skinov v jarju originala (human*, elf*,
dwarf*, orc*, furry*, skeleton, zombie; `dev/tools/skini-pokrivalo.js` na izvlečenih PNG, alfa > 0 = viden) **[M]**:

| | delež |
|---|---|
| skini brez enega samega neprozornega teksla pokrivala | **97 / 239 (41 %)** |
| skini z vsemi 6 deli vidnimi | 23 / 239 (10 %) |
| prazni deli (8 × 8 vseh prozornih) | 864 / 1.434 (60 %) |
| prazni trakovi ekstruzije | 32.050 / 45.888 (70 %) |

Kandidat K14b: list pokrivala na teksturo, ki izpusti dele in trakove samo s prozornimi teksli (ravni list
iz poskusa, način 2, z izločanjem); ista slika (dokaz z `HeadwearCheck`), za skine brez klobuka dobitek do
zgornje meje −23 % p50 pri 500 NPC-jih. Odprto: branje alfe teksture (`SimpleTexture` iz vira, URL skini
`ThreadDownloadImageData` asinhrono, menjava teksture NPC-ja), OptiFine/senčilniki brez alfa testa → **Q29**.

## Ponovitev

```
.\k-run.ps1 -Variant idle -N 200 -Warmup 10 -Measure 20 -Pokrivalo 1 -PreveriPokrivalo   # po git revert 4370184
powershell -File audit\m5k14-ab.ps1
```
