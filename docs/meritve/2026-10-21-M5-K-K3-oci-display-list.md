# M5-K K3 — oči iz display lista (A/B 21. 10. 2026) — **zavrženo**

Paket K3 v [03-FAZE](../03-FAZE.md) (Q28: za K4). Profil [M5-K.1](2026-10-18-M5-K.1-baseline-profil.md):
oči (`LayerEyes`) 10 % render niti pri idle-200, 14 % pri idle-500, ~3,5 µs na NPC. Koda in preverba
pikslov: seja 97 (19. 10.), commit `49bd247` (`rework/render/EyesList`, stikalo `RwEyesList`, privzeto 0);
1:1 prenos `LayerEyes` `7881e50`. Scenarij [M5-K.0](../scenariji/M5-K.0-klient.md), isti pogoji kot
baseline M5-K.1 (RD 8, FOV 70, 854 × 480, GUI viden, 30 + 60 s, svež svet vsak zagon).

## 1. Enakost slike (iz seje 97)

`EyesCheck` (`k-run -Oci 1 -PreveriOci`, merili K13/K14): idle-200, serije svet, vzorci (60 ključev),
utrip, vse tudi v povečavi ×8 — **0 razlik** barve in globine (povečava pokrije 341.191 pikslov, druga
barva oči spremeni 511), utrip po originalu. [`audit/m5k3-preverba.out`](../../audit/m5k3-preverba.out).

## 2. A/B (3 + 3 ponovitve na celico)

Pognal nočni upravljalnik (OPS.1/OPS.2) 21. 10. 07:47–08:13, testi `k3-200-off`, `k3-200-on`, `k3-500-off`,
`k3-500-on` (`ponovitve-run.ps1 -Scenarij klient -Ponovitev 3 -Dodatno -Variant idle -N 200|500 -Oci 0|1`),
koda s `sourceFingerprint` `c5289db1…72a0d`, na isti kodi zeleni tudi `unit`, `build`, `package`.
Pred vsako serijo: aktivno namizje, 120 s brez vnosa, brez drugih Java procesov, CPU ≤ 20 %; med
serijami ni bilo vnosa (idle 561 → 1729 s). T1–T6 zelena v vseh štirih serijah, **K0–K8 zelena v vseh
12 zagonih** (kamera odmik 0,0000), odtis T4 enak znotraj serije in med serijama celice razen
`rwEyesList`. K14 v vklopljenih zagonih potrdi pot z listom (npr. idle-500 p1: 3.916.069 izrisov oči z
listom, 0 po originalu). Lokalna poročila (git-ignored; tabele spodaj so prepisane iz njih): `audit/m25c-klient-2026-10-21-0747.md` (200/0), `-0754.md` (200/1),
`-0801.md` (500/0), `-0807.md` (500/1).

Mediana, razpon [min–max] čez 3 ponovitve; čas sličice v ms.

| celica | veličina | original (`RwEyesList=0`) | display list (`=1`) | sprememba | razpona |
|---|---|---|---|---|---|
| idle-200 | p50 | 6,451 [6,433–6,507] | 6,644 [6,510–6,660] | **+3,0 %** | ločena, **v slabšo smer** |
| idle-200 | p95 | 7,030 [6,979–7,030] | 7,149 [7,016–7,165] | +1,7 % | se prekrivata |
| idle-200 | p99 | 7,369 [7,315–9,355] | 7,413 [7,355–7,553] | +0,6 % | se prekrivata |
| idle-200 | FPS | 151,83 [151,40–152,64] | 148,68 [148,20–151,42] | −2,1 % | se prekrivata (za 0,02) |
| idle-500 | p50 | 11,435 [11,280–11,787] | 11,754 [11,733–11,856] | +2,8 % | se prekrivata |
| idle-500 | p95 | 12,652 [12,616–12,967] | 13,068 [12,952–13,133] | +3,3 % | se prekrivata |
| idle-500 | p99 | 14,892 [14,361–15,284] | 15,824 [15,485–15,945] | +6,3 % | ločena, **v slabšo smer** |
| idle-500 | FPS | 85,08 [82,65–85,95] | 82,72 [81,92–82,74] | −2,8 % | se prekrivata |

Integrirani strežnik (kontrola): idle-200 3,17 / 3,02 ms, idle-500 18,57 / 19,00 ms na tick — v šumu,
pričakovano, ker stikalo je samo klientsko. Original se ujema z baseline M5-K.1 (p50 idle-200 6,570,
idle-500 11,571).

**Izid:** display list oči **ne izboljša** nobene veličine čez razpon; kjer sta razpona ločena
(idle-200 p50, idle-500 p99), je list **počasnejši**. Po D-016 ni dokaza koristi, zato D-032 (privzeto
vklopljeno) ne velja → **K3 zavržen**, `RwEyesList` ostane **0** (original). Sprememba obnašanja ni.

## 3. Zakaj (hipoteza, ne izmerjeno)

**[H]** Geometrija oči je nekaj štirikotnikov na NPC. Original jih pošlje s `Tessellator.draw`
(odjemalski kazalci + `glDrawArrays`); list za isto geometrijo doda `glPushAttrib(GL_CURRENT_BIT)` /
`glPopAttrib`, `glCallList` in uskladitev `GlStateManager` (`color` pred, `resetColor` po). Pri tako
majhnem izrisu je cena v gonilniku na izris (stanje, klic), ne v prenosu oglišč, zato list prenosa ne
skrajša, ovoj pa doda delo. To je ista lekcija kot K14 ([zapis](2026-10-19-M5-K-K14-pokrivalo-en-list.md) §4):
profil pove, kje je čas, ne pa, ali ga zmanjša drugačna pot do gonilnika. Zgornja meja (oči izpuščene)
ni bila izmerjena; za morebitni nov paket oči (npr. izris v prehodu glave, odstranitev dveh
`setupFogColor`, iz [09](../09-PERFORMANCE-RAZISKAVA.md) K3) jo je treba izmeriti najprej.

## 4. Koda

Koda poskusa (`EyesList`, `EyesCheck`, stikalo) ostane v drevesu s privzeto 0, ker so obvezni testi
paketa (unit/build/package + 4 serije) tekli na točno tej kodi; odstranitev (kot K14 `4370184`) je
ločen čistilni korak z lastnim unit/build/package. 1:1 prenos `LayerEyes` (`7881e50`) ostane kot pri K14.
