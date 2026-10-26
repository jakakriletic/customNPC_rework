# M5-K — ponovna ocena K13, K10 in ostalih K-paketov (22. 10. 2026)

Paket po D-035 (3): "K13 in K10 se po M5-V.8 ponovno ocenita", in po D-029: plan ne sledimo slepo.
Brez produkcijske kode in brez novih meritev v svetu; vir so obstoječe meritve projekta in koda.
Odločitev: **D-053**.

Oznake: **[K]** iz kode, **[M]** izmerjeno v projektu, **[H]** hipoteza ali ocena, **[W]** zunanji vir.

---

## 1. Kaj je izmerjeno (vhodi)

| Vir | Ugotovitev |
|---|---|
| [M5-K.1 profil](../meritve/2026-10-18-M5-K.1-baseline-profil.md) §3 | Pri 200/500 vidnih NPC-jih je ~90 % render niti v NPC-jih; lastni čas v nativnih GL klicih (`nglCallList` 45–56 %). Model 9,8–10,9 µs, pokrivalo 6,7 µs, oči 3,5 µs na NPC, ime 8,2 µs na napis **[M]**. Skupina za vezavo tekstur ne obstaja; `bindTexture` je v "ostalo v `doRender`" (3,0–7,5 % skupaj z vsem ostalim) **[M]** |
| [K14](../meritve/2026-10-19-M5-K-K14-pokrivalo-en-list.md) | 6 display listov pokrivala v **enem** listu: ista slika, a idle-500 p50 −1,4 % (šum). Čas ni v številu klicev, temveč v gonilniku sorazmerno **ogliščem** (816 na NPC) **[M]**. Odstranitev oglišč (K14b) je dala −25 % **[M]** |
| [K3](../meritve/2026-10-21-M5-K-K3-oci-display-list.md) | Oči iz display lista namesto immediate mode: brez koristi (+3 %) **[M]**. Drugi dokaz, da manj klicev ≠ hitreje |
| [M5-V.6](../meritve/2026-10-21-M5-V.6-L2.md) | L2 (impostor v enem `BufferBuilder`, "nič klicev na NPC") na prizorišču `-Dalec 1`: idle-500 p50 11,591 → 2,331 ms (−79,9 %) **[M]**. To je praktična zgornja meja "instanciranega" izrisa: ~0,2 µs na impostor |
| [M5-V.8](../meritve/2026-10-22-M5-V.8-sprejem.md), D-051 | `RwFarView` je od M5-V.8b **privzeto 2**: vsak sledeni NPC dlje od 56 blokov je že impostor **[K]**. Modeli ostanejo samo znotraj 56 blokov |
| [K12](../meritve/2026-10-22-M5-K-K12-okvir.md) | Gneča 500 NPC-jev znotraj ~23 blokov (prizorišče M2.4): p50 11,48 ms, p95 12,85–12,91 ms (`dev/run`: K14b, L2 in okvir izklopljeni) **[M]** |
| [K5 L1](../meritve/2026-10-21-M5-K-K5-lod-L1.md) | Izbirna stopnja L1 od 32 blokov: idle-500 p50 −24 % **[M]** (privzeto 0, D-039) |

## 2. K13 — skini v nizu/atlasu: **zavržen kot samostojen paket**

- Premisa 09 je bila: menjava vezave teksture na vsak NPC z drugačnim skinom stane. V profilu M5-K.1
  vezava teksture ne nastopa kot skupina; `GlStateManager.bindTexture` preskoči enak id **[K]**, ena
  menjava pa je en sam GL klic proti ~30 µs izrisa NPC-ja **[H]** → zgornja meja reda 1–2 %, pod šumom
  A/B klienta (p50 razpon do 9 %, M5-K.1 §2) **[H]**.
- Daljni izris (L2/L3) že ima lasten atlas (M5-V.4, D-046), torej K13 za M5-V ni potreben.
- K13 ostane le kot **podkorak K10**, če se K10 kdaj začne (instanciran izris potrebuje en vezan
  teksturni objekt). Sam po sebi ni kandidat, ki bi presegel šum (protokol §2 "Meritve").

## 3. K10 — instancirani izris: **odložen izven M5** (s pogojem za ponovno odprtje)

1. **Premisa je delno ovržena.** Raziskava 09 je K10 utemeljila z "nič klicev na NPC". K14 in K3 sta
   izmerila, da zmanjšanje števila klicev pri isti geometriji ne pomaga; strošek je sorazmeren ogliščem,
   ki jih gonilnik obdela na CPU **[M]**. Instancing bi to odpravil le, če oglišča preidejo v
   GPU-medpomnilnik (VBO) **in** se poza delov računa v senčilniku — torej lasten GLSL, ki ponovi
   fiksno pot (luč, lightmap, megla, alfa test, rdeča barva udarca, prekrivne teksture, glint) **[K]**.
2. **Vrednost je po D-051 padla** (kot je napovedala raziskava M5-V.0 §8). Daleč od 56 blokov je
   vsak NPC že impostor; K10 bi pospešil samo gnečo modelov znotraj 56 blokov. Takšna gneča (500 v
   ~23 blokih) danes stane ~11,5 ms p50 brez K14b; s K14b (privzeto 1, −25 %) okoli 8,6 ms **[H, iz
   K14b A/B]**, z izbirnim L1 še manj. To je ~115 FPS pri 500 modelih na merilnem stroju.
3. **Tveganje ostaja največje v M5-K**: OptiFine senčila in senčni prehod, `customNPC_entities_mod`
   (lastni modeli in poze), MorePlayerModels deli, oklep, sloji. Sprememba mora biti piksel-enaka, sicer
   po D-032 ostane izklopljena — kar pri lastnem GLSL za fiksno pot ni realno pričakovati **[H]**.
   Velikost L (09) proti dobitku samo v redki bližnji gneči.
4. **Izhodni kriterij M5 K10 ne zahteva.** Za klient zahteva le, da je *zapisan* čas sličice (p95,
   1 % low) pri 200 in 500 vidnih NPC-jih z originalom in z vsemi sprejetimi K-paketi (03-FAZE, M5).

**Odločitev:** K10 (in z njim K13) se umakne iz vrstnega reda M5 v zaloga (backlog). **Pogoj za
ponovno odprtje:** meritev sprejema klienta (M5-K.2) ali poročilo uporabnika pokaže prizorišče z
≥ 300 modeli znotraj 56 blokov in p95 > 16,7 ms (pod 60 FPS) z vsemi privzetimi stikali. Tedaj prvi
korak ni polni K10, temveč poceni odločilni poskus **K10a**: deli modela iz VBO namesto display listov
(fiksna pot, ista slika), ki loči "oglišča v gonilniku" od "oglišča sploh" **[H]**.

**Dopolnitev 26. 10. 2026 — odločitev lastnika:** razlike, ki s prostim očesom niso vidne, so
sprejemljive; K10 naj se naredi in vklopi. Merilo za K10 je zato **vizualno enako** namesto
piksel-enako (izjema od D-032 samo za K10), pogoj za ponovno odprtje odpade. Izvedba v novem
milestonu **M14** (`03-FAZE.md`): najprej K10a, nato K10 s senčilnikom, samodejni povratek pri
senčilniških paketih in nepodprtih modelih.

## 4. Ostali K-paketi in kandidati iz M5-V.8

| Paket | Odločitev | Razlog |
|---|---|---|
| K2 mikropopravki | **zavržen** | Profil: "ostalo v `doRender`" 3–7,5 %, klientski tick ≤ 3,3 % **[M]**; posamezni mikropopravki so pod šumom |
| K11 redkejši klientski tick | **zavržen** | Klientski tick 1,1–3,3 % render niti **[M]**; sprememba videza (gladkost) za dobitek pod šumom |
| K1 delci, K6 skini, K7 omrežje, K8 morph, K9 `customNPC_entities_mod` | **odloženi** (ostanejo v 09) | V prizoriščih M2.4/M5-K jih ni, profil jih ne vidi; uvrstijo se, ko prizorišče ali poročilo pokaže strošek (kot že zapisano v 03-FAZE) |
| Čistilni korak K3 | **naslednji paket** | `RwEyesList`, `EyesList`, `EyesCheck` in ključ `eyesList` so koda zavrženega poskusa (K3, 21. 10.); odstranitev je skladna s protokolom ("ne obdrži za vsak slučaj"). Hkrati popravek besedila `RwRenderBox`/`RenderBox` (64/38,4 in 1,0/80 namesto 66/40 in 1,03/82, K12) |
| Cena izrisa L3 (1,25 ms / 4.831) | **zavržen kot paket** | ~0,26 µs na impostor z že obstoječim frustumom **[M, K]**; pri vd 24 je konec megle 384 > rob pasu 368, zato preskok "onkraj megle" nič ne prihrani **[K]**. Cilj D-034 je izpolnjen pri privzeti 2 |
| Pas `rwfar` po chunkih (91,5 µs) | **odložen** | Pod mejo 100 µs/tick/igralca, ki je na igralca, zato z več igralci ne raste čez mejo **[M]**; odpre se, če meritev preseže mejo |
| Asinhrona peka (PBO), 26–34 ms na videz | **odložen** | Izmerjen je zastoj ene sličice na nov videz, največ ena peka na sličico **[M]**; število različnih videzov v realnem svetu ni izmerjeno. Odpre se, če M5-K.2 ali poročilo pokaže zatikanje ob vstopu |

## 5. Nov vrstni red konca M5

1. **K3 čistilni korak** (+ popravek besedila `RwRenderBox`), dokaz `unit`/`build`/`package`; brez
   spremembe obnašanja (`RwEyesList` je privzeto 0).
2. **M5-K.2 — sprejem klienta (izhodni kriterij M5):** A/B idle-200 in idle-500 (prizorišče M5-K.1) in
   `-Dalec 1` (prizorišče M5-V.6), original proti **vsem sprejetim privzetim** stikalom (`RwHeadwearSkip`,
   `RwLabelCache`, `RwFarView=2`, `RwRenderBox`), 3 + 3; zapis p50/p95/p99 in 1 % low. Harness potrebuje
   način, ki zagon izvede s privzetimi vrednostmi kode namesto eksplicitnih vrednosti v `dev/run`.
3. Zaključek M5 (ostane Q22, odvisen od produkcijskega strežnika), nato M4 po D-038 (M4.14b, M4.16 …).
