# M5-K K14b — pokrivalo brez prozornih štirikotnikov (19. 10. 2026)

Paket K14b v [03-FAZE](../03-FAZE.md) (Q29: pred K4). Predhodnik: [K14](2026-10-19-M5-K-K14-pokrivalo-en-list.md)
(en list ne pomaga, čas je v 816 ogliščih na NPC; brez pokrivala idle-500 p50 −23 %).

## 1. Ideja in koda

Pokrivalo (`HeadWearType=1`, privzeto) riše 6 delov `Model2DRenderer` po 34 štirikotnikov
(`renderItemIn2D`: spredaj, zadaj, 8 + 8 trakov stolpcev, 8 + 8 trakov vrstic). Štirikotnik, ki vzorči
samo teksle z alfo 0, da fragmente z alfo 0; alfa test (`GL_GREATER`, ref 0,1 v vanilla) jih zavrže, zato
ne spremeni ne barve ne globine **[K]**. Za vsako teksturo se slika prebere enkrat in za vsak del določi:
nič / nekateri (lasten list z izbranimi štirikotniki) / vsi (originalni list).

- 1:1 prenos `LayerHeadwear` (commit 1903772; bajtkoda 5/5, `audit/m5k14b-bytecode-primerjava.txt`).
- `rework/render/HeadwearQuads` (enaka oglišča kot original, test bajtno), `HeadwearMask`, `HeadwearSkip`;
  `Model2DRenderer.rwCompileFiltered`, stikalo **`RwHeadwearSkip`** (0 = original).
- Original se uporabi, kadar pogoj ni dokazan: OptiFine, tekstura ni natanko `SimpleTexture` (URL, skini
  igralcev), vir ima `.mcmeta`, otrok ni `Model2DRenderer`, alfa test po `GlStateManager` ni vklopljen
  (`GL_GREATER` ref ≥ 0 ali `GL_GEQUAL` ref > 0). Ob ponovnem nalaganju virov se predpomnilnik pobriše.

## 2. Napačna premisa: z robom (dokazano enako) se ne izpusti skoraj nič

Strog dokaz mora šteti tudi teksel **tik za robom** obsega (fragment točno na robni črti, napaka
interpolacije, `GL_REPEAT` čez rob teksture). V standardni postavitvi skina so ob klobuku neprozorni teksli
glave, telesa in rok (in čez ovijanje noge), zato z robom pri skinu testnega sveta **noben** od 6 delov ni
prazen (vsi "nekateri"). **[M]** A/B (3 + 3, `audit/m5k14b-ab.ps1`, mediana):

| celica | p50 / p95 original | z robom (dokazano enako) | brez roba (`RwHeadwearSkip=1`) |
|---|---|---|---|
| idle-500 | 11,493 / 12,786 ms | 11,615 / 12,912 (v šumu) | **8,595 / 9,728 (−25 % / −24 %)** |
| idle-200 | 6,521 / 7,033 ms | 6,625 / 7,148 (v šumu) | **5,317 / 5,846 (−18 % / −17 %)** |
| FPS idle-500 / idle-200 | 84,2 / 151,2 | 83,6 / 149,1 | **112,7 / 185,6** |

Razponi vseh serij ≤ 3,6 %, razpona originala in brez roba se ne prekrivata. Različica z robom je pod šumom
in **zavržena**; ostane samo različica brez roba (vrednost 1).

## 3. Preverba slike (`HeadwearCheck`, `k-run -Pokrivalo 1 -PreveriPokrivalo`)

V eni sličici (iste matrike, `partialTicks`, stanje) se 200 NPC-jev idle-200 nariše v lasten framebuffer:
original → K14b → original; primerja se barva (RGBA8) in globina (float) vseh pikslov.

| serija | pikslov z NPC-ji | delov nič / nekaj / vse | razlik barva / globina | ponovitev |
|---|---|---|---|---|
| skin testnega sveta (`humanmale/steve.png`) | 36.439 | 1200 / 0 / 0 | 0 / 0 | 0 |
| vzorec (45 % alfa 0, 5 % alfa 1–25, ena ploskev prazna) | 38.076 | 200 / 200 / 800 | 0 / 0 | 0 |
| `elffemale` | 37.043 | 400 / 800 / 0 | 0 / 0 | 0 |
| `elfmage` | 37.466 | 0 / 200 / 1000 | 0 / 0 | 0 |
| `elfqueenformal` | 36.567 | 600 / 600 / 0 | 0 / 0 | 0 |
| `elfsoldierhelmet` | 37.376 | 200 / 800 / 200 | 0 / 0 | 0 |
| `blood_elf_mage` | 36.478 | 800 / 400 / 0 | 0 / 0 | 0 |

Vzorec brez pokrivala spremeni 8.656 pikslov (preverba je občutljiva). Med 20 s merjenja 0 vrnitev na
original. **To ni dokaz za vse primere:** fragment točno na robni črti štirikotnika lahko v originalu vzame
sosednji neprozoren teksel (enopikselni "bleed" na robu klobuka). V 7 serijah × 200 NPC-jev se to ni
zgodilo nikoli; po D-007 pa sprememba, ki ni dokazano enaka, ostane **privzeto izklopljena** → **Q30**.

**Odločitev 21. 10. 2026 (Q30 → D-049):** lastnik je odgovoril »Ja, Q30: K14b (RwHeadwearSkip=1) naj bo privzeto
vklopljen.« `RwHeadwearSkip` je od takrat **privzeto 1**; 0 vrne original, varovala iz §1 ostanejo. Novih meritev
ni bilo: dokaz ostaneta A/B iz §2 in preverba pikslov iz §3. `dev/run/config/CustomNpcs.cfg` ima izrecno 0, zato
scenariji še naprej merijo original, razen kjer ga `-Pokrivalo` nastavi.

## 4. Testi

`HeadwearSkipTest` (8): oglišča `emit` z vsemi štirikotniki bajtno enaka originalnemu `renderItemIn2D`;
izpuščeni ne spremenijo ostalih (50 naključnih mask); obseg uv ustreza ogliščem originala; maska z robom in
brez (rob, ovijanje, HD 128²). `LayerHeadwearBaselineTest` (zgradba, zelen na originalu in prenosu).
Skupaj 297 JUnit + testOriginal, `verify-package` PASS (69 vnosov).

## Ponovitev

```
.\k-run.ps1 -Variant idle -N 200 -Warmup 10 -Measure 20 -Pokrivalo 1 -PreveriPokrivalo
powershell -File audit\m5k14b-ab.ps1     # opomba: serije s1 so merile poskus z robom (odstranjen)
```
