# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki ga nova seja prebere na začetku in ga vsaka seja na koncu posodobi.**
Če seja tega ne naredi, je naslednja seja slepa.

Od 2026-10-19 je datoteka **namerno kratka** (cilj ≤ ~25 KB), da jo nova seja prebere poceni.
Vse starejše je dobesedno v arhivu; po potrebi ga išči z `grep` (glej "Kje je kaj" na koncu in
`05-SEJA-PROTOKOL.md` §1 in §3). Tu ostanejo samo: stanje, milestoni, **zadnja 2 vnosa dnevnika**,
odprta vprašanja in odprti pojavi.

---

## Trenutno stanje

| | |
|---|---|
| Zadnja posodobitev | **2026-10-22** (seja 116) — **ponovna ocena K13/K10** (D-053, [raziskava](raziskave/M5-K-K13-K10-ponovna-ocena.md)), brez kode in novih meritev: K13 zavržen kot samostojen paket, K10 odložen izven M5 (premisa »manj klicev« z K14/K3 delno ovržena, po D-051 so modeli samo znotraj 56 blokov), K2/K11 zavržena. Naslednja: K3c (čistilni korak) in M5-K.2 (sprejem klienta, izhodni kriterij M5). |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 1 od 21. 10., D-049) in K4 (privzeto 1) 19. 10., K3 zavržen 21. 10. (A/B brez koristi), K5 L1 v kodi 21. 10. (privzeto 0, D-039); M5-V.1 zaključen 21. 10. (varianta A zavrnjena, D-043), M5-V.2 (strežnik `rwfar`) zaključen 21. 10. (privzeto 0, D-044), M5-V.3 (opis videza) zaključen 21. 10. (D-045), M5-V.4 (peka impostorjev) zaključen 21. 10. (D-046), M5-V.5 (izris L3) zaključen 21. 10. (D-047), M5-V.6 (L2) zaključen 21. 10. (D-048), M5-V.7 (združljivost) zaključen 22. 10. (D-050), M5-V.8 (sprejem, D-051) in M5-V.8b (`RwFarView` privzeto 2) 22. 10. — **M5-V zaključen**; K12 (okvir upodobitve, `RwRenderBox` privzeto 1, D-052) 22. 10.; ponovna ocena K13/K10 (D-053: K13 zavržen, K10 odložen) 22. 10. M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **K3c** (odstranitev kode poskusa `RwEyesList`, popravek besedila `RwRenderBox`), nato **M5-K.2** (sprejem klienta, izhodni kriterij M5) — D-053; K13 zavržen, K10 odložen izven M5. Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** narejen 20. 10. (D-038); od M4 se lahko vrineta **M4.14b** (formacije v svetu, samo zagon) in **M4.16** (scenarij letenja), ko računalnik ni v rabi. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **183** datotek `.java` v `src/patch` (22. 10.; +2 K12: `rework/render/RenderBox`, `rework/diag/RenderBoxAudit`), od tega **118** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `nav` 12, `net` 3, `render` 8) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **399 JUnit** (metod `@Test`, preštetih z grep 22. 10.; +12 K12) v 68 datotekah — zeleni na kodi `a435cbb9…` + **40 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
| Odprti pojavi | nobenega blokirnega; P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6, Q8, Q10, Q12, Q14 in Q31 odprta; specifična R9 forenzika odložena, ne blokirana |
| Omejitev orodij | Seja teče na uporabnikovem Windows računalniku (DESKTOP-SN494C0) s PowerShellom, Gradlom in JDK 8 — **sama poganja build, `verify-package` in scenarije v svetu**. Pred zagonom scenarija odstrani `NoDefaultCurrentDirectoryInExePath` (glej `OKOLJE.md`). Python ni nameščen (node je) |

Polna različica te tabele pred skrajšanjem (vključno s povzetki ~20 starejših sej v celici
"Zadnja posodobitev" in vrsticami Zagoni 23. 9., Ogrevanje, Šumni pas, Formacije):
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §1.

---

## Napredek po milestonih

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **zaključeno** | M0.1–M0.7 narejeno (+ M0.2r obnova okolja); M0.8 zabeležen kot blokada z opisanim vplivom |
| M1 Integriteta podatkov | **zaključeno** | M1.1–M1.3, M1.5, M1.6 in M1.9 narejeni; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | **zaključeno 5. 10.** (razen M2.4r render, prestavljen v M5-K) | M2.1–M2.7 preverjeni v svetu; baseline M2.6 zelen 5. 10. — [baseline](meritve/baseline-2026-10-05.md) |
| M3 Jedro entitete | **zaključeno do M3.9** | M3.1–M3.5 (R1 reproduciran, popravljen pod `RwMountSteering=1`), M3.6 (D-020, `RwAttackPriority`), M3.7, M3.8 (`RwHitbox`), M3.9 (7. 10.). Odprto: M3.8b GUI v klientu (z igralcem), Q15 → D-028 |
| M4 Gibanje | **M4.0 zaključen 20. 10.**; M4.14a v kodi (izven vrstnega reda) | [raziskava M4.0](raziskave/M4.0-gibanje-in-letenje.md), D-038: vrstni red M4.14b → M4.16 (scenarij) → M4.10 → M4.17 → M4.2/M4.3 → (M4.5) → M4.6 → M4.7; M4.13 → (M4.11) in M4.15 neodvisno; M4.1/M4.4/M4.9/M4.12 zavrženi ali združeni. Formacije v svetu še ne — [zasnova](06-FORMACIJE.md); M4.15 (D-036); Q6, Q31 |
| M5 Performance | **M5-S zaključen 16. 10.**; M5-K.0 (16. 10.) in M5-K.1 (18. 10.) zaključena, K14 zavržen, K14b (19. 10., `RwHeadwearSkip` privzeto 1 od 21. 10., D-049: −25 % idle-500), K4 (19. 10., `RwLabelCache=1`, −7 % idle-200), K3 zavržen (21. 10., A/B brez koristi, `RwEyesList=0`), K5 L1 (21. 10., `RwLodStart=0`, D-039: idle-500 −24 % pri vklopu); M5-V.1 zaključen 21. 10. (D-043), M5-V.2 (D-044), M5-V.3 (D-045), M5-V.4 (D-046), M5-V.5 (D-047) in M5-V.6 (D-048, L2: idle-500 −80 % pri vklopu) zaključeni 21. 10., M5-V.7 (D-050, združljivost) in M5-V.8 (D-051, sprejem) in M5-V.8b (`RwFarView` privzeto 2) 22. 10., K12 (`RwRenderBox` privzeto 1, D-052) 22. 10., ponovna ocena K13/K10 (D-053) 22. 10. | M5-S: S1, S2, S14b, S4, S7, S5 (D-037, `RwPathNegCache=1`), S14c, S16, M5.15 (coremod), M5.16 (`RwMoveBoxes`) vsi privzeto vklopljeni po D-032; M5.2 zavržen. Podrobnosti po paketih: [arhiv, tabele §2](arhiv/04-STANJE-tabele.md); razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | **jedro narejeno v ločenem modu** `customNPC_entities_mod` (v0.7.0, D-024) | ostanejo script API/hooki, uskladitev verzije CNPC, reload; [plan](08-PREGLED-IN-PLAN.md) |
| ~~M8 CustomNPC+~~ | **izločeno** (D-023, 3. 10.) | [pregled in plan](08-PREGLED-IN-PLAN.md) |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |
| M11 Poveljevanje prek zemljevida | ni začeto — **zadnja faza** (D-026, 6. 10.) | R10; vhodni pogoj M4.14b, M4.10, M5; Q14 |

Polna tabela s podrobnimi opombami in tabeli **M1 po paketih** / **M2 po paketih**:
[`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §2–§3.

---

## Dnevnik sej

*V tej datoteki ostaneta samo zadnja 2 vnosa (novejši zgoraj). Starejši:
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 114 → 1, novejši zgoraj).*


### 2026-10-22 (116) — M5-K: ponovna ocena K13 in K10 — zaključeno

**Paket:** ponovna ocena K13/K10 po D-035 (3) (D-053) · **Stanje:** **zaključeno** — raziskava brez produkcijske kode in
brez novih zagonov; vhodi so obstoječe meritve ([raziskava](raziskave/M5-K-K13-K10-ponovna-ocena.md)).

**Ugotovitve:** K14 in K3 sta izmerila, da manj klicev pri isti geometriji ne pomaga (strošek so oglišča v gonilniku),
zato je premisa K10 »nič klicev na NPC« delno ovržena; po D-051 so modeli samo znotraj 56 blokov, L2 pa je že izmerjena
zgornja meja »instanciranega« izrisa (idle-500 −80 %). Vezava teksture (K13) v profilu M5-K.1 ni merljiva.

**Odločitve (D-053):** K13 zavržen kot samostojen paket; K10 odložen izven M5 (ponovno odprtje: ≥ 300 modelov znotraj
56 blokov s p95 > 16,7 ms ob privzetih stikalih, tedaj najprej poskus K10a z VBO); K2 in K11 zavržena; K1, K6–K9 odloženi
do prizorišča, ki jih vsebuje; cena izrisa L3 zavržena kot paket; pas `rwfar` po chunkih in PBO peka odložena s pogojem.

**Spremembe obnašanja:** nobene. **Minecraft meritve:** nobene.

**Naslednji korak:** **K3c** — odstranitev kode poskusa `RwEyesList` in popravek besedila `RwRenderBox`/`RenderBox`
(64/38,4 blokov pri RD 8, 1,0/80 pri RD 10), dokaz `unit`/`build`/`package`; nato **M5-K.2** (sprejem klienta,
izhodni kriterij M5: original proti vsem privzetim stikalom, idle-200/500 in `-Dalec 1`, p95 in 1 % low).
Neodvisno: M4.14b/M4.16; ročna preverba z OptiFine in Entity Culling; Q22.

### 2026-10-22 (115) — M5-K K12: okvir upodobitve in Entity Culling — zaključeno

**Paket:** M5-K K12 (D-052, Q33) · **Stanje:** **zaključeno** — vseh 7 zahtevanih testov zelenih na kodi `a435cbb9…`:
`unit`, `build`, `package`, `k12-okvir-on`, `k12-okvir-off` (K1–K8, K16, K17a–j, K18a–e) in A/B `k12-500-off`/`-on`
(3 + 3, T1–T6). Prvi `k12-okvir-on` na `0db45ac6…` je padel na K18d (ubit `RWV7_Mrtev` z dometom ležečega hitboxa
53,3 bloka, kamera ~58,5) → ležeč/ubit NPC dobi domet stoječega.

**Raziskava** ([zapis](meritve/2026-10-22-M5-K-K12-okvir.md) §1): CNPC ne prepiše `getRenderBoundingBox` ne
`isInRangeToRenderDist`; Entity Culling 6.5.0 / RenderLib 1.4.5 okluzijo računa na predpomnjenem
`getRenderBoundingBox` [K, jar]. Lastna okluzija zavržena.

**Narejeno:** `rework/render/RenderBox` (okvir = hitbox ∪ model; domet brez hitboxa in ležečih), prepisa v
`EntityNPCInterface` (klient), `CustomNpcs.RwRenderBox` (privzeto **1**); harness `HarnessJob.renderBox`,
`RenderBoxAudit` (okvir, domet, pečen obris L2 proti okvirju), `FarRender.bakedLook`, `k-run.ps1 -RenderBox` +
`RWV7_Mec` + K18; JUnit +12 (`RenderBoxTest` 8, `RenderBoxAuditTest` 3, `HarnessJobTest` +1).

**Izid:** z okvirjem izven dometa 0, obris čez okvir 0 (največ 0,091 bloka); brez okvirja izven dometa 2, obris čez
hitbox do 0,747 bloka (meč). idle-500 p50 11,492 → 11,483, p95 12,849 → 12,907 ms (prekrivanje) → cena v šumu.

**Spremembe obnašanja moda:** z novim configom (ali brez ključa) se NPC na robu zaslona/za oviro (Entity Culling)
nariše, kjer ga je original odrezal; NPC brez hitboxa in ležeč NPC imata domet stoječega (Size 5, RD 8: 64 namesto
38,4 / 42,7 bloka). `dev/run/config/CustomNpcs.cfg` ima `RwRenderBox=0` (merilni pogoji nespremenjeni).
**Minecraft meritve:** 2 + 1 (padel) zagona `k12-okvir`, 6 zagonov A/B. Entity Culling in OptiFine v svetu nista izmerjena.

**Naslednji korak:** ostali K-paketi — **K13 in K10 ponovno oceniti** (D-035; kandidati iz M5-V.8: cena izrisa L3,
pas `rwfar` po chunkih, PBO peka). Ob naslednji spremembi kode popraviti besedilo komentarja `RwRenderBox` in javadoc
`RenderBox` (64/38,4 blokov pri RD 8, ne 66/40). Neodvisno: čistilni korak K3, M4.14b/M4.16; ročna preverba z
OptiFine in Entity Culling.

---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q33**, odločena 22. 10. → D-052; Q32 → D-039; Q30 odločen 21. 10. → D-049).

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | **odgovorjeno 17. 9.** — uporabnik nima dostopa; zabeleženo kot blokada (M0.7 §6), ponovno odpreti pred M10 |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | **odprto, a ne blokira M2.3** — scenarij meri samostojno letenje, kar je podlaga za oba primera; odgovor je potreben šele za obseg M4. **M4.0 (20. 10.):** priporočilo — M4 naredi samostojno letenje (Hover, Momentum za AI zmaje); vozilo, ki ga krmili igralec, ostane ločen paket po R1 in M4.6 (CustomNPC+ ga ima kot ločeno vlogo »mount«) |
| Q8 | Pri R8 — kateri provider (Anthropic / OpenAI / lokalni model)? | M9.3 | odprto |
| Q10 | Ali strežnik, kjer to teče, sploh ima izhodni internetni dostop? | M9.1 | odprto |
| Q12 | Uvoz zunanjih 3D modelov (npr. prenesen model zmaja) kot model NPC-ja — je to v obsegu? Če da: kateri format (`.bbmodel` / OBJ / JSON), in kako model pride do igralcev (resource pack / lokalna mapa / prenos z URL-ja / push s strežnika)? | nova zahteva (R11?; R10 je od 6. 10. poveljevanje prek zemljevida); odvisna od M7, ker zunanji model prinese svoj skelet | **odprto, odprto 17. 9.** — danes mod zna samo (a) teksture prek URL-ja (`skinUrl`, `capeTexture`, `overlayTexture`) in (b) prevzem modela **registrirane entitete** (`setModel(id)` → `ModelData.entityClass`, `DataDisplay:405`), torej model iz že nameščenega moda. Geometrija iz datoteke ne obstaja: vsi modeli so hardcoded `ModelBase` razredi (`client/model/`). V M8 katalogu tega ni, ker tega nima niti CustomNPC+. Ocena XL; odločitev šele ko je M7 (animacijski skelet) jasen |
| Q14 | Pri R10 — kateri zemljevid: lasten iz `ladja_mod` (izbira kot pri ladjah), JourneyMap, Xaero ali nov v CNPC? | M11.3 | **odprto 6. 10.**, ne blokira do M11 |
| Q18 | Popravek »prvi ukaz po zagonu serverja se izgubi« (ogrevanje vhoda z `list`) je zdaj samo v `perf-run.ps1`. Vsaka od 11 scenarijskih skript ima svojo kopijo `Start-DevServer`/`Send-Command`; v `nav-run`, `rwdiag-run`, `r1/r2-run`, `m36/m39-run`, `hb-run`, `fixture-run`, `testworld-run` in `smoke-server` lahko izgubljen prvi ukaz tiho spremeni pogoj meritve (npr. izgubljen `gamerule`). Naj se `Start-DevServer`, `Send-Command` in `Prime-ServerInput` preselijo v `meritve-lib.ps1` in popravijo vse skripte (svoj paket)? | vsi scenariji, veljavnost meritev | **odprto 10. 10.**; priporočilo: da |
| Q19 | Ostali trije klicatelji `Server.sendAssociatedData` (`UPDATE_NPC` ob uredbi NPC-ja, `PARTICLE`, `DELETE_NPC`) še pregledajo ~450 chunkov na klic. Niso na tick, zato izmerjene koristi ni; ob masovnem spawnu/brisanju (500 NPC-jev) pa je to 225.000 pregledov chunkov. Naj se preusmerijo tudi ti (S7b)? | M5, R5 | **odprto 10. 10.**; priporočilo: samo če scenarij pokaže številko |
| Q20 | Enakost S7 za **neprazen** seznam prejemnikov v svetu ni bila preverjena s pravim igralcem (dedicirani strežnik scenarija jih ne ima); stoji na viru vanille, 200.000 legah v testu in preverbi na NPC-jih. Naj se pred izdajo doda preverba v klientu (`/rwblink 2` z igralcem v svetu)? | M5.13, M10 | **odprto 10. 10.**; priporočilo: da, skupaj z M3.8b GUI preverbo |
| Q31 | **Obseg nastavitev letenja (R2, po raziskavi M4.0).** Plan M4 naredi načine *original* / *Hover* / *Momentum* (`RwFlightMode`). CustomNPC+ ima ob tem še nastavitve, ki jih R2 ne omenja: **višinska meja** nad tlemi, **spust na tla, ko NPC nima poti** (danes leteči NPC obvisi v zraku), **hoja, ko je NPC tik nad tlemi**, ločena **hitrost dviganja**. Naj M4 doda (a) samo višinsko mejo in spust brez poti v načinu Hover (priporočeno: brez spusta lebdeči NPC po koncu poti visi, kar je del pritožbe »kinda ass«), (b) vse štiri, ali (c) nobene (samo trije načini)? | M4.2, M4.3, obseg R2 | **odprto 20. 10.**; priporočilo (a); ne blokira M4.16, M4.10, M4.17 |
| Q22 | Naj se postavi minimalen **pravi Forge strežnik** (ne dev) za dimni test zapakiranega jarja? Od M5.15 je jar hkrati coremod in mod; dev okolje je deobfuskirano in reobfuskiranega jarja ne more naložiti, zato je produkcijska pot preverjena samo statično (manifest, razredi, odvisnosti coremoda). To je tudi vhod v M10 (release) | M5.15, M5.16, M10 | **odprto 11. 10., nujnost narasla 14. 10.**: od M5.16 jar **privzeto** (`RwMoveBoxes=1`) spreminja bajtkodo `net.minecraft.world.World`, in prav tisto, kar v produkciji ni preverjeno, so imena SRG. Če poseg v produkciji ne uspe, se obnašanje ne pokvari (transformer vrne iste bajte, vrata niso klicana, `/rwmove` pokaže `poseg=0`) — izgubi se samo korist. Priporočilo: da, pred M5.8 |

---

## Odprti pojavi (nereproducirani — hipoteze)

| ID | Pojav | Kje se vidi | Stanje |
|---|---|---|---|
| P1 | Leteči NPC, ki obstane **natanko 0,5 bloka od sredine svojega vozlišča**, se ne premakne več: `gib = 0`, pot ostane cela, entiteta se normalno tika. Kopenski v istem svetu se premika | M2.3 fazi B in C, oba zagona 17. 9.; 12 NPC-jev, 0,00 bloka v 900 tickih, `dStarost = 20`, `gib = 0` | **zožen** — dve razlagi ovrženi z meritvijo. Ostane `FlyingMoveHelper`: `pathFollow` prestevilči pri < 0,45, `FlyingMoveHelper:39` premakne pri > 0,5. Potrdi ali ovrže ga **faza D** ob naslednjem zagonu |

| P2 | NPC z napadalno tarčo izven `aggroRange` (Čebiševa razdalja) ima napad, ki utripa: `shouldContinueExecuting` ga ustavi, `shouldExecute` brez preverbe dosega spet začne, `resetTask` vsakič pobriše pot | M3.6 5. 10.: v načinu 1 so **vse** zakasnitve nad 20 tickov (7 vzorcev, do 100) izven dosega ob ukazu; v načinu 0 izven dosega do 160 | **potrjeno 5. 10. (korelacija v scenariju M3.6)** — obnašanje originala v obeh načinih; ali ga popraviti, odloči uporabnik |

Pravilo iz `05-SEJA-PROTOKOL.md`: bug brez reprodukcije je hipoteza. P1 ima meritev, nima
pa še razlage in ni ločen od možne napake scenarija, zato je tu in ne med bugi.

---

## Kje je kaj (kazalo arhiva in registrov)

| Vsebina | Datoteka | Kdaj ga odpreš |
|---|---|---|
| Dnevnik sej: vnosi od seje 114 (22. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q33 (odločena), **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
