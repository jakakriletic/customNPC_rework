# M5-V.0 — Raziskava: vidnost NPC-jev na daljavo (impostorji do 24 chunkov)

**Datum:** 2026-10-14 · **Paket:** M5-V.0 (D-029, raziskava, brez produkcijske kode) · **Odločitev:** D-034

Oznake: **[K]** iz kode (CNPC `dev/reference-src`, vanilla/Forge 1.12.2 viri v
`dev/build/tmp/recompileMc/sources`), **[M]** izmerjeno, **[H]** hipoteza ali ocena, **[W]** zunanji vir.
Ta raziskava **ne vsebuje nobene meritve [M]** — klientski harness (M5-K.0) še ne obstaja. Vse številke
zmogljivosti so ocene in se v paketih M5-V.1 in M5-V.8 zamenjajo z meritvami.

---

## 0. Povzetek

Zahteva uporabnika (14. 10. 2026):

> *"Zdj mi gre ful nazivce ker vidim npcje sele pr nvm 8ih chunkih, a bi se dal narest da ce bi bli
> nakonc ful bl na idle performance friendly in na client performance friendly da bi dodal se un effekt
> k sm pozabu kk se rece ampak opis je da dlje koka je entity, slabse kvalitete je textura in bi z tem
> nardil da bi se npcje videlo tudi na 24 chunkov daleč, samo pač bi bila textura sm par pixlov."*

Ugotovitve na kratko:

1. **Efekt ima ime.** Splošno je to **LOD** (*level of detail*, stopnje podrobnosti po razdalji). Za
   teksture je to **mipmapping** (vnaprej pomanjšane različice teksture, GPU sam izbere manjšo, ko je
   predmet daleč). Za celo entiteto je zadnja stopnja LOD **impostor** (ali *billboard*/*sprite*): namesto
   3D modela se nariše ena ploskev z vnaprej upodobljeno sliko NPC-ja. Pri 24 chunkih je NPC visok
   **~4 piksle** (§3), zato je impostor tam vizualno nerazločljiv od modela.
2. **Ozko grlo ni klient, ampak strežnik.** NPC-je vidiš do **±64 blokov = 4 chunke v vsako smer**
   (8 chunkov premera — to je tvojih "8 chunkov"), ker jih CNPC registrira s `tracker(64, 3, true)` in
   strežnik klientu oddaljenih NPC-jev **sploh ne pošlje** [K]. Klient bi jih pri razdalji upodobitve
   ≥ 20 sicer narisal do 160 blokov [K]. Samo klientski LOD zato **ne more** pokazati NPC-ja na 24 chunkih.
3. **Naivna rešitev (dvig dometa sledilnika na 368 blokov) je draga**: 33× večja površina, za vsakega
   NPC-ja celoten GZIP spawn NBT, polna klientska entiteta s tickom in paketi gibanja vsake 3 tike [K/H].
   To je točno tisto, česar uporabnik ne želi ("neb impactal performance").
4. **Priporočena zasnova (varianta B):** NPC-ji onkraj dometa sledilnika gredo na klient kot
   **lahki nadomestki** (≈16 B: id, pozicija, smer, id videza, zastavice) po **ločenem kanalu**
   ~1-krat na sekundo in samo ob spremembi; videz se pošlje **enkrat na videz** (kloni si ga delijo).
   Klient iz videza **enkrat speče impostor** (8 smeri v atlasu z mipmapi — to je uporabnikov
   "slabša tekstura z razdaljo") in **vse oddaljene NPC-je nariše v enem klicu**. Klientska entiteta,
   klientski tick in vanilla paketi gibanja za oddaljene NPC-je ne obstajajo.
5. Ista slika (impostor) se uporabi tudi za **sledene NPC-je onkraj ~48–64 blokov** (stopnja L2): tam
   nadomesti ~18 klicev display listov na NPC z 1/5000 enega klica — to je obenem največji klientski
   vzvod za FPS (prej "zadnja stopnja K5").
6. Pogoj, ki ga mod ne more odpraviti: NPC obstaja samo v **naloženem chunku**. V enem igralcu je to
   razdalja upodobitve (integrirani strežnik jo prevzame) [K], na dediciranem strežniku pa
   `view-distance` iz `server.properties`. Za 24 chunkov na strežniku mora operater nastaviti
   `view-distance=24` (vanilla cena nalaganja chunkov, ne cena tega modula) → **Q24**.

Ocena cene pri 5.000 oddaljenih NPC-jih [H]: klient ≤ ~1 ms na sličico, strežnik ~10–15 µs na tick na
igralca, omrežje ~80 KB enkratno + ~0–15 KB/s, VRAM ~21 MB. Cilji so v §6, preverijo se v M5-V.8.

---

## 1. Zakaj danes vidiš NPC-je samo do ~4 chunkov — tri omejitve

### 1a. Strežniški sledilnik (veže) [K]

- Vsi NPC razredi so registrirani z `registerNewentity(name, 64, 3, true)` → `tracker(64, 3, true)`
  (`noppes/npcs/CustomEntities.java:62-75`): domet 64 blokov, posodobitev vsake 3 tike, hitrost.
- Domet je **kvadrat**, ne krog: `EntityTrackerEntry.isVisibleTo` preveri |dx| ≤ i in |dz| ≤ i,
  `i = min(range, maxRange)` (`net/minecraft/entity/EntityTrackerEntry.java:492-498`).
- `maxRange = (viewDistance − 1) · 16` (`EntityTracker.java:64, 434`); pri razdalji 24 je to 368 blokov.
  Domet CNPC (64) je manjši, zato veže **64**.
- Izven tega kvadrata strežnik pošlje `SPacketDestroyEntities` — klient NPC-ja ne pozna več.

### 1b. Klientski domet upodobitve (ne veže pri RD ≥ 8) [K]

- `Entity.isInRangeToRenderDist(d²)`: `povprečni rob AABB × 64 × renderDistanceWeight`
  (`net/minecraft/entity/Entity.java:1823-1834`).
- `renderDistanceWeight = clamp(renderDistanceChunks / 8, 1, 2.5)` (`RenderGlobal.java:903`).
- Privzeti NPC (velikost 5): 0,6 × 1,8 × 0,6 → povprečni rob 1,0 → **64 blokov pri RD ≤ 8,
  160 blokov pri RD ≥ 20**.
- **Stranska ugotovitev:** NPC z izklopljenim hitboxom ima širino `1.0E-5`
  (`EntityNPCInterface.java:1076-1090`) → povprečni rob 0,6 → domet **38 blokov** pri RD 8 (96 pri RD 20).
  Tak NPC izgine prej kot ostali. Stopnja L2 (§5.6) to popravi mimogrede.
- CNPC ne prepiše `isInRangeToRenderDist`, `shouldRender` ali `getRenderBoundingBox` (09, [K]).

### 1c. Naloženi chunki (meja vsega) [K]

- Entiteta obstaja samo v naloženem chunku. Integrirani strežnik (en igralec, LAN) nastavi
  `viewDistance = renderDistanceChunks` (`IntegratedServer.java:197`), torej so pri RD 24 NPC-ji do
  24 chunkov **naloženi in tickajo že danes** — samo poslani niso.
- Dedicirani strežnik: `view-distance` v `server.properties`. Pri 10 je največja smiselna razdalja
  ~(10−1)·16 = 144 blokov, ne glede na ta modul. Klient tam nima niti terena.
- Entitete blizu roba naloženega območja ne tickajo (`World.updateEntityWithOptionalForce` zahteva
  naložene chunke ±32 blokov) — njihov nadomestek je statičen, kar je pravilno.

| RD klienta (SP) | 1a sledilnik | 1b render (rob 1,0) | 1c chunki | Vidiš NPC do |
|---|---|---|---|---|
| 8 | 64 (kvadrat) | 64 | 128 | **64** |
| 12 | 64 | 96 | 192 | **64** |
| 16 | 64 | 128 | 256 | **64** |
| 24 | 64 | 160 | 384 | **64** |

**Sklep:** pri vseh razdaljah veže strežniški sledilnik. Brez spremembe na strežniški strani ni mogoče
videti NPC-ja dlje od 64 blokov. Varianta D (samo klient) je zato zavržena (§4).

---

## 2. Kako se imenuje "efekt" in kaj od tega dejansko pomaga

| Ime | Kaj je | Ali pomaga tukaj |
|---|---|---|
| **LOD** (level of detail) | po razdalji cenejša predstavitev predmeta | da — krovni pojem |
| **Mipmapping** | piramida pomanjšanih tekstur (64×64, 32×32, …); GPU izbere stopnjo po velikosti na zaslonu | da, za **kakovost**: brez mipmapov oddaljen NPC "migeta" (aliasing); FPS ne spremeni |
| **Impostor / billboard / sprite** | ploskev z vnaprej upodobljeno sliko predmeta, obrnjena proti kameri | da, za **FPS in domet**: 1 ploskev namesto ~18 klicev listov |
| Okluzija (culling) | ne riši, kar je zakrito | že K12 (Entity Culling) |

Entitetne teksture v vanilli 1.12 **nimajo mipmapov** in so `GL_NEAREST`: `SimpleTexture` naloži z
`uploadTextureImageAllocate(..., blur=false, clamp=false)` → `setTextureBlurMipmap(false, false)`
(`TextureUtil.java:195, 273-283`); mipmape dobi samo atlas blokov (`TextureMap.mipmapLevels`) [K].
Zato bi bil oddaljen NPC z navadno teksturo nemiren. Impostor atlas (§5.4) ima mipmape — to je točno
uporabnikov opis "dlje ko je, slabše kvalitete je tekstura".

**Uskladitev z raziskavo 09** (»zniževanje ločljivosti tekstur — zavrženo kot ukrep za FPS«): ostane
pravilno. Manjša tekstura sama ne zmanjša klicev. FPS in domet prineseta impostor in nadomestki;
mipmap je del impostorja zaradi kakovosti slike.

---

## 3. Koliko pikslov je NPC na daljavo

Višina na zaslonu: `h_px = H · h / (2 · d · tan(FOV/2))`; H = 1080, FOV = 70° (vanilla "Normal"),
h = 1,8 bloka → `h_px ≈ 1388 / d` [izračun].

| Razdalja | 32 | 64 (4 chunki) | 128 (8) | 192 (12) | 256 (16) | 384 (24) |
|---|---|---|---|---|---|---|
| Višina NPC-ja [px] | 43 | 22 | 11 | 7 | 5 | **3,6** |
| Širina (0,6 bloka) [px] | 14 | 7 | 3,6 | 2,4 | 1,8 | **1,2** |

- Na 24 chunkih je NPC **~4 × 1 piksel**. Uporabnikova intuicija drži: tam je model nesmisel.
- Impostor okvir **32 × 64 tekslov** je pri 64 blokih ~1,5–3× nadvzorčen; dlje GPU izbere mipmap.
  Večji okvir (48 × 96) je smiseln samo, če se impostor začne bliže kot ~48 blokov.
- Površina: kvadrat sledilnika ima stranico 128 (16.384 m²), kvadrat do 368 blokov 736 (541.696 m²) →
  **33× več površine**. Pri enaki gostoti je 200 vidnih NPC-jev danes ≈ **6.600** na 24 chunkih.
  Zasnova mora skalirati na tisoče — en klic za vse, nič na entiteto.

---

## 4. Variante in kritika

| | Varianta | Kako | Za | Proti | Odločitev |
|---|---|---|---|---|---|
| **A** | Dvig dometa sledilnika (npr. 368) + `renderDistanceWeight` | konfiguracija dometa ob registraciji / `EntityTrackerEntry.range` | najmanj kode; NPC je pravi | vsak NPC = **GZIP spawn NBT** (`Server.writeNBT`, `Server.java:265-273`; vsebuje display, oklep, orožje, `ModelData`, `EntityNPCInterface.java:1475-1510`) ob vsakem vstopu v domet; paketi gibanja vsake 3 tike vsem igralcem; polna klientska entiteta s tickom; nato še ~18 klicev listov na sličico. Pri ×33 površine linearno ×33 | **samo kot primerjalna celica** v M5-V.1 (izmeri, kako drago je), ne kot rešitev |
| **B** | **Nadomestki po ločenem kanalu + impostorji** | strežnik pošilja lahke posnetke NPC-jev onkraj dometa sledilnika; klient riše impostorje v enem klicu | cena neodvisna od CNPC tickov; brez klientske entitete; 1 klic; videz enkrat na videz | največ kode (strežnik + klient + peka); združljivost OptiFine | **priporočeno** |
| C | B + "duhovi" iz nenaloženih chunkov | strežnik hrani zadnje pozicije NPC-jev ob odložitvi chunka (vzor Bobby za teren) | NPC-ji dlje od `view-distance` | klient tam nima terena (NPC lebdi v praznini); 1.12 LOD teren je samo FarPlaneTwo (alfa) | **zavrženo** za M5-V; ponovno odpreti samo ob Q24 = dedicirani strežnik + LOD teren |
| D | Samo klient | `isInRangeToRenderDist` / teža | trivialno | klient oddaljenih NPC-jev ne pozna (§1a) — ne reši ničesar razen NPC-jev brez hitboxa | **zavrženo** (vključeno v L2) |

**Zunanji vzori [W]:**
- *I See You Over There!* in *Entity Render Distance Extender* povečata domet upodobitve **in**
  sledenja; avtor prvega izrecno piše, da klient brez večjega dometa sledenja oddaljenih entitet ne pozna
  — to je varianta A in potrjuje §1a.
- *Spyglass Entity Distance* razširi domet samo med zumiranjem s spyglassom (omeji ceno variante A).
- *Render Distant Players* je najbližji vzor varianti B: strežnik prek **lastnega paketa** sporoči
  klientu igralca onkraj dometa (z raytraceom vidnosti), klient nariše **pomanjšan nadomestni model**.
- *Bobby* hrani prejete chunke na klientu za prikaz onkraj `view-distance` (vzor variante C, samo teren).
- Impostorji v igrah: Unreal Impostor Baker in oktaedrski impostorji (Fortnite drevesa; izbira in mešanje
  najbližjih zajetih pogledov) — za NPC-je pri ≤ 22 px zadošča 8 smeri okoli navpične osi brez mešanja.
- Paper/Spigot `entity-tracking-range` po kategorijah: operaterji domet **znižujejo** zaradi CPU in
  pasovne širine — neposreden dokaz, da je varianta A draga.
- Moda za 1.12, ki bi mobe risal kot impostorje, raziskava ni našla; tudi Distant Horizons entitet ne
  riše (odprte težave s prikazom block entitet skozi LOD).

---

## 5. Priporočena arhitektura (varianta B)

### 5.0 Stopnje

| Stopnja | Razdalja (privzeto, nastavljivo) | Kaj se riše | Kdo pozna NPC-ja |
|---|---|---|---|
| L0 | 0 – 32 | original (vse) | vanilla sledilnik |
| L1 | 32 – `RwImpostorStart` | model brez oči, glinta, prekrivne teksture, delcev, imen (to je **K5**) | vanilla sledilnik |
| **L2** | `RwImpostorStart` (48–64) – 64 | **impostor** iz pravega klientskega NPC-ja; vanilla izris preklican | vanilla sledilnik |
| **L3** | 64 – min(`RwFarRange`, (vd−1)·16) | **impostor** iz nadomestka | kanal `rwfar` |

Histereza na vsaki meji (npr. ±4 bloke), da NPC na meji ne utripa med stopnjama.

### 5.1 Strežnik — register in kanal `rwfar`

1. **Register NPC-jev po svetu**: `IWorldEventListener.onEntityAdded/Removed` (isti vzorec kot
   `rework/core/CollisionWatcher`, M5.16), samo `EntityNPCInterface`. Brez iskanja po
   `loadedEntityList`.
2. **Kadenca**: igralec `p` je obdelan na ticku `t`, ko `(t + p.entityId) % 20 == 0` (krožno, en igralec
   ~1-krat na sekundo, obremenitev razpršena po tickih). Bližnji pas (64–128) po želji 4 Hz.
3. **Pas**: NPC gre v nadomestke za igralca, če velja
   `trackerRange − 8 < max(|dx|, |dz|) ≤ min(RwFarRange, (vd − 1) · 16)` (Chebyshev kot vanilla; 8 blokov
   prekrivanja s sledilnikom, glej šiv v §5.3), in `isPlayerWatchingChunk(npc.chunkX, npc.chunkZ)`
   (klient ima teren), in `display.getVisible() != 1` (`EntityNPCInterface.java:1634`; nevidni se
   **nikoli** ne pošljejo), in NPC ni mrtev s skritim truplom.
4. **Delta**: za vsakega igralca zadnji poslani posnetek `id → (qx, qy, qz, yaw, videz, zastavice)`.
   Pošlje se samo: novi, spremenjeni (premik > 0,5 bloka, yaw > 11,25°, sprememba videza/zastavic) in
   odstranjeni id-ji. Mirujoči NPC-ji po prvem posnetku stanejo **0 B/s**.
5. **Format** (lasten kanal `rwfar`, `NetworkRegistry.newEventDrivenChannel`; CNPC kanali in ID-ji
   paketov nespremenjeni): `varint n`; na vnos `varint entityId` (~3 B), pozicija 3 × 24 bitov v 1/8
   bloka (±1.048.576 blokov, 9 B), `yaw` 1 B, `varint videz` (~2 B), zastavice 1 B
   (hodi / sedi / leži / jaha / pol-viden / boj) → **≈ 16 B**. Odstranitev: `varint id`. Paketi nad
   pragom kompresije vanilla že stisne z zlib.
6. **Rokovanje**: klient z vklopljenim `RwFarView` pošlje `HELLO(verzija, želeni domet)`. Strežnik
   pošilja samo takim klientom (stari klient ali izklopljeno stikalo = 0 prometa).
7. **Ocena cene [H]**: preverba enega NPC-ja (razdalja, hash chunka, delta) ~50 ns → 5.000 NPC-jev =
   0,25 ms na igralca na sekundo = **~12,5 µs na tick na igralca**. Omrežje: prvi posnetek 5.000 × 16 B =
   80 KB (pred zlib), nato samo premiki: 1.000 hodečih × 16 B × 1 Hz ≈ 16 KB/s. Brez vpliva na AI,
   frekvenco ticka ali vanilla sledilnik (ni v nasprotju s prepovedmi v R5).

### 5.2 Opis videza in deduplikacija

- **Vsebina** (podmnožica `writeSpawnData`): ime registra razreda entitete (CNPC ima ~27 razredov),
  `display` **brez imena in naslova**, `Armor`, `Weapons`, `ModelData`, `StandingState`, `Orientation`,
  odmiki telesa, pol-vidnost. Brez jobov/vlog (Bard/Puppet/Companion), brez statistik.
- **Id videza** = 64-bitni hash serializiranega opisa; strežnik hrani `hash → opis` (LRU). Kloni iz
  istega klona imajo isti hash → en opis za vse.
- **Invalidacija**: opis se izračuna leno in zavrže ob vsaki poti, ki že danes pošlje `UPDATE_NPC`
  (urejanje NPC-ja, skripte, ki spremenijo display) in ob spremembi opreme. Hash se ne računa na tick.
- **Zahteva/odgovor**: klient za neznan id pošlje `REQ(id)`; strežnik odgovori z GZIP opisom samo za id-je,
  ki jih je ta igralec dejansko prejel (omejitev hitrosti, zaščita pred zlorabo).
- **Zasebnost [H]**: brez imena/naslova oddaljeni NPC ne razkrije zgodbe. Pozicije pa so vidne do
  `view-distance` (enako kot vanilla znotraj sledilnika, le dlje) — na PvP strežniku je to "radar".
  Stikalo `RwFarLos` (raytrace vidnosti kot Render Distant Players) je kasnejša možnost → Q26.

### 5.3 Klient — nadomestki in šiv s sledilnikom

- Shramba `entityId → nadomestek` (pozicija prej/zdaj, čas posnetka, yaw, videz, zastavice).
  **Interpolacija** med zadnjima posnetkoma (zamik ~1 s je na 100+ blokih neopazen; brez nje bi hodeč NPC
  na 384 blokih skakal ~8 px na sekundo).
- **Šiv**: id-ji vanilla entitet in nadomestkov so isti (strežnikov `entityId`). Če v `WorldClient`
  obstaja živa entiteta z istim id-jem, nadomestek ne riše. 8 blokov prekrivanja pasu (§5.1) pomeni, da
  nadomestek pride, **preden** vanilla pošlje uničenje → NPC na meji ne izgine niti za en okvir.
- Ob menjavi dimenzije, odklopu ali izklopu stikala se shramba izprazni.

### 5.4 Peka impostorjev (atlas)

1. Iz opisa videza klient zgradi **lažno entiteto** (ni dodana v svet): `EntityList.createEntityByIDFromName`
   + `readSpawnData`/`display.readToNBT`. Precedens: CNPC to že dela v GUI-jih
   (`client/gui/GuiNpcMobSpawnerAdd.java:47` `createEntityFromNBT`, `GuiNPCInterface.drawNpc:427`).
2. **FBO** (vanilla `Framebuffer`; brez podpore FBO — `OpenGlHelper.isFramebufferEnabled()` — se modul
   izklopi): ortogonalna projekcija, model v stoji (ali sedi/leži po zastavicah), `RenderManager.renderEntity`
   pod **8 koti** (0°, 45°, …, 315°), standardna osvetlitev entitet.
3. **Samodejni obrez**: enkrat na videz preberi alfo (`glReadPixels` malega okvirja) in shrani pravokotnik
   UV + svetovno velikost ploskve (veliki NPC-ji, krila, orožja; velikost 1–30).
4. **Atlas** 2048² RGBA (privzeto; 1024² za šibke GPU): okvirji 32 × 64 + 2 teksla prozornega roba,
   8 okvirjev = trak 272 × 68 → ~210 videzov. **LRU**: najdlje neuporabljen videz se prepiše.
   Fiksni cevovod 1.12 ne more vzorčiti `GL_TEXTURE_2D_ARRAY` brez senčilnika, zato navaden 2D atlas.
5. **Mipmapi** (to je "slabša tekstura z razdaljo"): največ 4 stopnje, `GL_LINEAR_MIPMAP_LINEAR` (ali
   `NEAREST_MIPMAP_LINEAR` za piksel videz). Z alfa testom se silhueta v manjših stopnjah stanjša ali
   izgine → **ohranjanje pokritosti** alfe po stopnjah (Castaño, *Computing Alpha Mipmaps*).
6. **Proračun peke**: največ 1 videz na sličico (≈ izris 8 NPC-jev, [H] 0,1–0,3 ms). Dokler videz ni
   pečen: L2 riše pravi model, L3 nič (ali nevtralno silhueto, stikalo).
7. **Animacija**: privzeto statična poza. Hojo nakaže zastavica (izbirno 2–4 okvirji hoje → 4× več
   prostora v atlasu, stikalo `RwImpostorWalk`).

### 5.5 Risanje v enem klicu

- En `BufferBuilder`, format `POSITION_TEX_COLOR_NORMAL_LMAP` (normala je potrebna za OptiFine
  senčilnike), 4 oglišča na NPC. **Cilindrični billboard** (vrtenje samo okoli navpične osi).
- **Izbira okvirja**: kot med smerjo kamera → NPC in yaw NPC-ja, zaokrožen na 45°.
- **Svetloba**: `world.getCombinedLight(pos)` v koordinatah lightmapa (noč in jame se upoštevajo kot pri
  entitetah); vrednost se osveži, ko nadomestek zamenja blok.
- **Megla**: rišemo **znotraj prehoda entitet**, kjer je megla vklopljena — impostorji na robu razdalje
  izginjajo v meglo kot teren. `RenderWorldLastEvent` je za to neprimeren: kliče se po
  `GlStateManager.disableFog()` in po prosojni plasti (`EntityRenderer.java:1460-1485`).
- **Mesto v sličici**: poseg coremoda (D-031) v `RenderGlobal.renderEntities` tik pred
  `profiler.endStartSection("blockentities")` (`RenderGlobal.java:719`) — niz `"blockentities"` je
  zanesljivo sidro za ASM, stanje GL je tam stanje entitet; samo `MinecraftForgeClient.getRenderPass() == 0`.
  Rezerva, če poseg ne uspe: `RenderWorldLastEvent` z ročno nastavljeno meglo (stikalo pove, katera pot teče).
- **Frustum**: `ICamera` iz istega prehoda; brez okluzijskih poizvedb (globinski test poskrbi za pravilno
  sliko, cena je ena ploskev).
- Pol-vidni NPC-ji: mešanje z alfa, kot original.
- **Ocena [H]**: ~100–150 ns na NPC (frustum + 4 oglišča) → 5.000 NPC-jev **0,5–0,75 ms** na sličico;
  20.000 oglišč × 36 B = 720 KB prenosa na sličico. Če meritev pokaže preveč: medpomnilnik se zgradi
  na novo samo, ko se kamera premakne > 0,5 bloka ali se spremeni nadomestek.

### 5.6 L2 — sledeni NPC-ji kot impostorji (FPS)

- Forge `RenderLivingEvent.Pre` je preklicljiv in se sproži v `RenderLivingBase.doRender`
  (`RenderLivingBase.java:86`) — **brez popravka CNPC rendererja**. Če je NPC dlje od `RwImpostorStart`
  in je videz pečen: dodaj v paket, prekliči vanilla izris.
- Ker vanilla NPC-ja onkraj `isInRangeToRenderDist` sploh ne ponudi v izris, modul za sledene NPC-je onkraj
  te meje (NPC brez hitboxa, §1b) prav tako sam doda impostor → odpravi izginjanje na 38 blokih.
- Videz za L2 se izračuna na klientu iz prave entitete (isti hash kot strežnik, ker je opis isti).
- To je del, ki **zmanjša** čas sličice: vsak NPC v pasu 48–64 blokov stane 1 ploskev namesto
  ~18 `glCallList` + oči + senca. Z razlogom je bil impostor v 09 označen kot "največji potencial".

### 5.7 Stikala in privzete vrednosti

Videz se spremeni → D-007: privzeto original, dokler uporabnik ne odloči drugače. **Odločeno 14. 10. (Q23 → D-035):**
stolpec "privzeto" velja do sprejema M5-V.8; po sprejemu sta strežnik `RwFarView=1` in klient `RwFarView=2`.

| Stikalo | Stran | Vrednosti | Privzeto |
|---|---|---|---|
| `RwFarView` | strežnik | 0 = ne pošiljaj, 1 = pošiljaj nadomestke klientom, ki jih zahtevajo | 0 |
| `RwFarRange` | strežnik | največji domet v blokih (omejen z (vd−1)·16) | 384 |
| `RwFarView` | klient | 0 = original, 1 = L3 (nadomestki), 2 = L2 + L3 | 0 |
| `RwImpostorStart` | klient | začetek L2 v blokih | 56 |
| `RwImpostorAtlas` | klient | 1024 / 2048 / 4096 | 2048 |
| `RwImpostorWalk` | klient | 0 = statično, 1 = okvirji hoje | 0 |
| `RwFarLos` | strežnik | 0 = brez preverbe vidnosti, 1 = raytrace (kasneje) | 0 |

Ukaza `/rwfar` (strežnik: števci vnosov, bajtov, µs) in `/rwimpostor` (klient: videzi v atlasu, peke,
čas izrisa, preklopi stopenj).

---

## 6. Proračun in cilji (vhod za izhodni kriterij)

| Veličina | Ocena [H] pri 5.000 oddaljenih NPC-jih | Cilj (M5-V.8) |
|---|---|---|
| Klient, čas izrisa impostorjev | 0,5–0,75 ms / sličico | **≤ 1,0 ms** p95 dodatno pri 5.000 |
| Klient, sledeni NPC-ji v L2 | manj kot original | čas sličice p95 **nižji** od originala pri 200/500 vidnih |
| Klient, peka | ≤ 1 videz / sličico | brez sličice > 2× mediane zaradi peke |
| Strežnik | ~12,5 µs / tick / igralca | MSPT p95 **+≤ 0,1 ms** pri 1 igralcu in 5.000 NPC-jih |
| Omrežje | 80 KB enkratno, 0–16 KB/s | idle **≤ 1 KB/s**, hoja ≤ 20 KB/s na igralca |
| VRAM | ~21 MB (2048² + mipmapi) | ≤ 32 MB |
| Vidnost | — | NPC viden na **≥ 368 blokov** pri RD 24 v SP; brez utripanja na mejah stopenj |

Primerjava z varianto A (M5-V.1) mora pokazati, da je B pri istem N cenejša na **vseh treh** straneh
(strežnik, omrežje, klient); sicer se zasnova ponovno pretehta (D-029).

---

## 7. Tveganja in združljivost

| Tveganje | Opis | Ukrep |
|---|---|---|
| OptiFine prepiše `RenderGlobal.renderEntities` | sidro `"blockentities"` morda drugje ali ga ni [H] | poseg preveri sidro; ob neuspehu rezerva `RenderWorldLastEvent`; matrika: vanilla / OptiFine / OptiFine + senčilniki |
| Senčilniki (BSL, Sildur's) | entitete gredo skozi `gbuffers_entities`; senčni prehod pokliče `renderEntities` še enkrat | normala v formatu; v senčnem prehodu (`Shaders.isShadowPass`, refleksija) impostorje izpusti (stikalo) |
| Entity Culling (K12) | ta mod preskoči `doRender` zakritih entitet | L2 dela v `RenderLivingEvent.Pre`, torej se zakriti ne dodajo — skladno |
| `customNPC_entities_mod` (D-024) | animirani modeli, drugi rendererji | peka gre skozi `RenderManager`, torej skozi njihov renderer; poza mirovanja; preverba v V7 |
| Morph NPC-ji, `ModelData` deli, veliki NPC-ji, jahanje | drugačni rendererji in velikosti | samodejni obrez (§5.4); jahač in nosilec ločena nadomestka z zastavico "jaha" (sedeča poza) |
| URL skini | skin še ni prenesen ob peki | peka čaka na naloženo teksturo; ob zamenjavi skina nov hash |
| Brez FBO / zelo stari GPU | `Framebuffer` nepodprt | modul se izklopi sam, `/rwimpostor` to izpiše |
| Coremod v produkciji (Q22) | imena SRG za `RenderGlobal` niso preverjena na pravem klientu | ista pot kot M5.15/M5.16; neuspel poseg = rezerva, ne okvara |
| Uhajanje informacij | pozicije NPC-jev vidne do `view-distance` | brez imen v opisu; `RwFarLos` kasneje; strežnik odloča (`RwFarView` strežnika) |
| Napačen videz (pop) | razlika model ↔ impostor na meji L1/L2 | peka z isto osvetlitvijo, histereza, primerjava pikslov v V4 |

---

## 8. Kritika plana M5-K (ohrani / spremeni / zavrzi / dodaj)

| Paket | Odločitev | Zakaj |
|---|---|---|
| M5-K.0 harness | **spremeni (razširi)** | potrebuje še prizorišče "daleč" (tisoče NPC-jev v pasu do 384 blokov, kamera na vzpetini), števec bajtov kanala in strežniški MSPT z igralcem — sicer M5-V nima meritve |
| M5-K.1 profil | ohrani | določi, ali je L2 res največji klientski vzvod |
| K5 LOD | **spremeni (razdeli)** | K5 obdrži L1 (model brez podrobnosti) in proračun upodobitve; **impostor gre v M5-V** (L2/L3 si delita atlas in izris) |
| K12 okluzija | ohrani | neodvisno; L2 je skladen z Entity Culling |
| K13 skini v nizu | ohrani, **ni pogoj za M5-V** | impostor atlas je lasten; K13 ostane pogoj za K10 |
| K10 instancing | ohrani, a **vrednost pade** | če so NPC-ji onkraj ~56 blokov impostorji, ostane za instancing le gneča blizu; po M5-V.8 ponovno oceniti |
| K11 redkejši klientski tick | ohrani | nadomestki nimajo klientskega ticka; K11 velja samo za sledene |
| **M5-V** | **dodaj** (D-034) | zahteva uporabnika; ločen modul, po Q25 (D-035) takoj za M5-K.1 in K5 |

Premisa, ki jo ta raziskava popravi: v planu je bil impostor **samo FPS ukrep** za NPC-je, ki jih klient
že pozna. Uporabnikov problem je **domet**, in ta je strežniški (§1). Brez kanala `rwfar` impostor
uporabniku ne bi pokazal niti enega NPC-ja več.

---

## 9. Paketi M5-V

| ID | Paket | Izhod / merilo | Vel. |
|---|---|---|---|
| **M5-V.0** | Raziskava (ta dokument) | **zaključeno 14. 10.** | — |
| **M5-V.1** | **Odločilni poskus + prizorišče "daleč"**: svet z 1.000 / 5.000 NPC-ji v pasu do 384 blokov, igralec na vzpetini; celice: original (sledilnik 64) in **varianta A** (domet 368 prek preizkusnega stikala); merila: MSPT strežnika z igralcem, bajti/s proti klientu (števec na `NetworkManager`), čas sličice in klientski tick (harness M5-K.0), velikost spawn NBT (porazdelitev). Merilo veljavnosti: vidnih NPC-jev je res N | številke cene A → potrdi ali ovrže potrebo po B | M |
| **M5-V.2** | **Strežnik**: register NPC-jev, krožna kadenca, pas, filtri, delta, format, kanal `rwfar` + `HELLO`, `/rwfar`; stikali `RwFarView`, `RwFarRange` | JUnit: kodirnik naprej/nazaj, delta, pas (Chebyshev, prekrivanje), nevidni izpuščeni; v svetu: bajti in µs na tick | M |
| **M5-V.3** | **Opis videza**: podmnožica spawn NBT brez imena/naslova, 64-bit hash, LRU, invalidacija na poteh `UPDATE_NPC`, `REQ`/odgovor z omejitvijo | JUnit: kloni → isti hash; vsaka sprememba videza → nov hash; ime ne spremeni hasha; opis ne vsebuje imena | S |
| **M5-V.4** | **Klient: peka in atlas**: lažna entiteta, FBO, 8 kotov, samodejni obrez, atlas z robom, mipmapi z ohranjanjem pokritosti, LRU, proračun 1 videz/sličico, izklop brez FBO | primerjava pikslov impostor ↔ model pri 56 blokih (povprečna absolutna razlika pod pragom); JUnit za ohranjanje pokritosti in razporeditev atlasa | L |
| **M5-V.5** | **Klient: nadomestki in izris L3**: shramba, interpolacija, šiv z živimi entitetami, en `BufferBuilder`, billboard, izbira okvirja, lightmap, megla, poseg v `renderEntities` (sidro `"blockentities"`) z rezervo, frustum, histereza | NPC viden na ≥ 368 blokih pri RD 24 (SP); brez izginotja na meji 64 (posnetek 20 sličic prečkanja); čas izrisa na `/rwimpostor` | M |
| **M5-V.6** | **L2**: sledeni NPC-ji onkraj `RwImpostorStart` prek `RenderLivingEvent.Pre`; NPC-ji brez hitboxa | čas sličice pri 200/500 vidnih **nižji** od originala (A/B, D-016) | M |
| **M5-V.7** | **Združljivost**: OptiFine (brez in s senčilniki, senčni prehod), Entity Culling, `customNPC_entities_mod`, morph, veliki, jahajoči, mrtvi, pol-vidni, URL skini | matrika z izidom vsake celice; vsaka nekompatibilnost ima samodejni izklop ali stikalo | M |
| **M5-V.8** | **Sprejem**: A/B vseh celic M5-V.1 z `RwFarView` 0/1/2 (3 ponovitve, D-016), vizualni posnetki pri 64/128/256/368 blokih, zapis v `docs/meritve/` | cilji iz §6 izpolnjeni; odločitev o privzeti vrednosti po Q23 | S |

Vrstni red: **M5-V.1 → V.2 → V.3 → V.4 → V.5 → V.6 → V.7 → V.8**, v M5 takoj za M5-K.1 in K5 (Q25 → D-035). V.2–V.3 (strežnik) in V.4 (peka) sta
neodvisna in se lahko zamenjata; V.5 potrebuje oboje. V.6 lahko gre takoj za V.4, če M5-K.1 pokaže, da je
klient pri 200–500 vidnih ozko grlo.

---

## 10. Odprta vprašanja (prenesena v `04-STANJE.md`)

### Odgovori uporabnika 14. 10. 2026 (→ D-035)

- **Q23: da** — po sprejemu (M5-V.8) privzeto vklopljeno: strežnik `RwFarView=1`, klient `RwFarView=2`.
  Drugi del vprašanja (pozicije brez preverbe vidnosti) v chatu ni bil zastavljen → ločeno kot **Q26**.
- **Q24: strežnik** — cilj je dedicirani strežnik. Posledice:
  1. Za 24 chunkov mora operater nastaviti `view-distance=24`. Minecraft 1.12 nima ločene razdalje
     simulacije (prišla v 1.18): vsak naložen chunk tika, pri 24 je to 49² = 2.401 chunkov na igralca proti
     441 pri 10 (**5,4×**), z vsemi entitetami in polnim AI NPC-jev **[K/izračun]**. To je vanilla cena,
     M5-V je ne more odpraviti — "uspavanje oddaljenih NPC-jev" je v R5 izrecno prepovedano. Edina pot mimo
     bi bila ločena razdalja pošiljanja in simulacije (vzor 1.18), ki je ločen projekt.
  2. M5-V.1 meri na **dediciranem strežniku s priključenim klientom**, pri `view-distance` 10 in 24, da je
     vanilla cena razdalje ločena od cene M5-V.
  3. Cena na igralca (kadenca, delta, omrežje) je linearna v številu igralcev **[H]**; dev okolje ima en
     klient, zato se več igralcev oceni iz meritve enega (omejitev, zapisana v M5-V.8).
  4. Varianta C ostane zavržena: na strežniku z manjšim `view-distance` klient onkraj nima terena.
  5. Q22 (pravi Forge strežnik) postane pogoj za izdajo tudi za M5-V; poseg v `RenderGlobal` je klientski
     in potrebuje preverbo imen SRG še na pravem klientu.
- **Q25: po priporočilu** — vrstni red M5: M5-K.0 → M5-K.1 → K5 → **M5-V.1 … M5-V.8** → K12 → ostali
  K-paketi; K13 in K10 se po M5-V.8 ponovno ocenita.
- **Q26 — odločeno 14. 10.: po priporočilu, `RwFarLos=0`** (→ D-035). Vprašanje je bilo: `RwFarLos`: na strežniku nadomestki razkrijejo pozicije NPC-jev do
  `view-distance` tudi za zidovi (imena in naslovi nikoli). Naj ostane privzeto 0 (brez raytracea, najcenejše)
  ali naj strežnik privzeto pošilja samo NPC-je v vidni črti? Priporočilo: 0 — NPC-ji niso igralci, vanilla
  znotraj 64 blokov prav tako pošilja vse; operater lahko vklopi.

### Prvotna vprašanja

- **Q23** — Privzeta vrednost: naj bo daljna vidnost po sprejemu (M5-V.8) **privzeto vklopljena**
  (`RwFarView` 1 na strežniku, 2 na klientu)? Spremeni videz (D-007), zato odloči uporabnik. In: ali sme
  strežnik razkriti pozicije NPC-jev do `view-distance` brez preverbe vidnosti (`RwFarLos=0`)?
  Priporočilo: da (v enem igralcu ni tveganja); `RwFarLos` šele na zahtevo.
- **Q24** — Kje igraš: v **enem igralcu/LAN** (24 chunkov deluje takoj, ker integrirani strežnik naloži
  RD) ali na **dediciranem strežniku**? Tam mora biti `view-distance=24`, kar je vanilla cena (nalaganje in
  tick vseh chunkov, ne samo NPC-jev). Varianta C (NPC-ji onkraj naloženih chunkov) bi brez LOD terena
  pomenila NPC-je v praznini. Priporočilo: cilj SP/LAN in strežniki z velikim `view-distance`; C ostane
  zavržena.
- **Q25** — Vrstni red: dobesedno "na koncu M5-K" (po K5, K12, K13, K10) ali **takoj po M5-K.1**? M5-V
  ne potrebuje K13/K10, L2 pa zmanjša vrednost K10. Priporočilo: M5-V takoj po M5-K.1 in K5 (L1), K13/K10
  po M5-V.8 ponovno oceniti.

---

## 11. Viri

- [I See You Over There! (Modrinth)](https://modrinth.com/mod/i-see-you-over-there),
  [izvorna koda](https://github.com/Estecka/mc-I-See-You-Over-There) — domet upodobitve po FOV; brez
  večjega dometa sledenja klient entitet ne pozna.
- [Entity Render Distance Extender (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/entity-render-distance-extender)
  — nastavljiv domet upodobitve in sledenja (varianta A).
- [Spyglass Entity Distance (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/spyglass-entity-distance)
  — razširitev dometa samo med zumiranjem.
- [Render Distant Players (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/render-distant-players)
  — strežniški raytrace + lasten paket + pomanjšan nadomestni model (vzor variante B).
- [Bobby (Modrinth)](https://modrinth.com/mod/bobby) — klientski predpomnilnik chunkov onkraj `view-distance`.
- [FarPlaneTwo (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/farplanetwo) — LOD teren za 1.12.2, alfa.
- [Flywheel #264](https://github.com/Engine-Room/Flywheel/issues/264) — block entitete skozi Distant Horizons LOD.
- [Unreal Engine — Impostor Baker](https://dev.epicgames.com/documentation/en-us/unreal-engine/impostor-baker-plugin-in-unreal-engine)
  — impostorji v Fortnite, izbira in mešanje zajetih pogledov.
- [Godot Octahedral Impostors](https://github.com/SIsilicon/Godot-Octahedral-Impostors) — odprta izvedba po Shaderbits.
- [Paper — optimizacija omrežja](https://mintlify.wiki/PaperMC/Paper/optimization/network) in
  [konfiguracija Paper](https://wiki.deinserverhost.de/en/wiki/paper-configuration) — `entity-tracking-range` po kategorijah.
- I. Castaño, *Computing Alpha Mipmaps* (The Witness blog, 2010) — ohranjanje pokritosti alfe v mipmapih.
- Notranje: [09-PERFORMANCE-RAZISKAVA](../09-PERFORMANCE-RAZISKAVA.md) (K5, K10, K12, K13, stroški izrisa NPC-ja),
  [M5.16](../meritve/2026-10-14-M5.16-trki-brez-poizvedbe.md) (vzorec posega coremoda in `IWorldEventListener`).
