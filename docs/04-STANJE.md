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
| Zadnja posodobitev | **2026-10-22** (seja 114) — **M5-V.8b zaključen**: `RwFarView` privzeto **2** v kodi (D-051; `dev/run` ostane 0), dokaz `unit`/`build`/`package`. Pred tem (seja 113): **M5-V.8 zaključen**: sprejem vidnosti na daljavo, 12 serij × 3 zelenih na kodi `9f9125b0…`; vd 24 / 5.000 sličica p95 9,188 → 8,893 ms pri `RwFarView` 2 (10,873 pri 1), `rwfar` 91,5 µs/tick/igralca, 0 B/s mirovanje → D-051 privzeto 2, sprememba v kodi je M5-V.8b ([zapis](meritve/2026-10-22-M5-V.8-sprejem.md)). Pred tem (seja 112): **M5-V.7 zaključen**: združljivost L2/L3 (D-050): senčila OptiFine → samodejni izklop (`RwFarShaders` privzeto 0), senčni prehod brez impostorjev, peka čaka na prenos URL skina, popravek pol-vidnih NPC-jev v L2; `v7-posebni` K17a–j in regresija V0–V24 zelena na kodi `a369f197…`; OptiFine, Entity Culling in `customNPC_entities_mod` samo statično. Pred tem (seja 111): **Q30 odločen**: K14b `RwHeadwearSkip` privzeto **1** (D-049, odločitev lastnika, brez novih meritev). Pred tem (seja 110): **M5-V.6 zaključen**: L2 (sledeni NPC-ji onkraj `RwImpostorStart` 56 kot impostorji, preklic `RenderLivingEvent.Pre`), D-048; vseh 8 zahtevanih testov zelenih na kodi `6db12833…` (A/B 3 + 3, RD 10, prizorišče `-Dalec 1`: idle-200 p50 4,886 → 2,013 ms, idle-500 11,591 → 2,331 ms; L2 137/200 in 437/500 = geometrija); privzeto `RwFarView=0` |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 1 od 21. 10., D-049) in K4 (privzeto 1) 19. 10., K3 zavržen 21. 10. (A/B brez koristi), K5 L1 v kodi 21. 10. (privzeto 0, D-039); M5-V.1 zaključen 21. 10. (varianta A zavrnjena, D-043), M5-V.2 (strežnik `rwfar`) zaključen 21. 10. (privzeto 0, D-044), M5-V.3 (opis videza) zaključen 21. 10. (D-045), M5-V.4 (peka impostorjev) zaključen 21. 10. (D-046), M5-V.5 (izris L3) zaključen 21. 10. (D-047), M5-V.6 (L2) zaključen 21. 10. (D-048), M5-V.7 (združljivost) zaključen 22. 10. (D-050), M5-V.8 (sprejem, D-051) in M5-V.8b (`RwFarView` privzeto 2) 22. 10. — **M5-V zaključen**. M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **K12** (okluzija, združljivost z Entity Culling), nato ostali K-paketi (K13, K10 ponovno oceniti; kandidati iz M5-V.8: cena izrisa L3, pas `rwfar` po chunkih, PBO peka). Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** narejen 20. 10. (D-038); od M4 se lahko vrineta **M4.14b** (formacije v svetu, samo zagon) in **M4.16** (scenarij letenja), ko računalnik ni v rabi. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **181** datotek `.java` v `src/patch` (22. 10.; +2 M5-V.7: `rework/far/FarCompat`, `SkinDownload`), od tega **116** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 25, `entity` 13, `far` 21, `formation` 15, `nav` 12, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **387 JUnit** (metod `@Test`, preštetih z grep 22. 10.; +1 M5-V.8) v 66 datotekah — zeleni na kodi `9f9125b0…` + **40 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
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
| M5 Performance | **M5-S zaključen 16. 10.**; M5-K.0 (16. 10.) in M5-K.1 (18. 10.) zaključena, K14 zavržen, K14b (19. 10., `RwHeadwearSkip` privzeto 1 od 21. 10., D-049: −25 % idle-500), K4 (19. 10., `RwLabelCache=1`, −7 % idle-200), K3 zavržen (21. 10., A/B brez koristi, `RwEyesList=0`), K5 L1 (21. 10., `RwLodStart=0`, D-039: idle-500 −24 % pri vklopu); M5-V.1 zaključen 21. 10. (D-043), M5-V.2 (D-044), M5-V.3 (D-045), M5-V.4 (D-046), M5-V.5 (D-047) in M5-V.6 (D-048, L2: idle-500 −80 % pri vklopu) zaključeni 21. 10., M5-V.7 (D-050, združljivost) in M5-V.8 (D-051, sprejem) in M5-V.8b (`RwFarView` privzeto 2) 22. 10. | M5-S: S1, S2, S14b, S4, S7, S5 (D-037, `RwPathNegCache=1`), S14c, S16, M5.15 (coremod), M5.16 (`RwMoveBoxes`) vsi privzeto vklopljeni po D-032; M5.2 zavržen. Podrobnosti po paketih: [arhiv, tabele §2](arhiv/04-STANJE-tabele.md); razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 112 → 1, novejši zgoraj).*


### 2026-10-22 (114) — M5-V.8b: `RwFarView` privzeto 2 — zaključeno

**Paket:** M5-V.8b (D-051) · **Stanje:** **zaključeno** — zahtevani `unit`, `build`, `package` zeleni na kodi
`5c4754b8…` (22. 10. 2026; brez novih meritev v svetu; sprejem je M5-V.8 na kodi `9f9125b0…`).

**Narejeno:** `CustomNpcs.RwFarView` privzeto **2** (prej 0) in komentar konfiguracije z izidom sprejema (vrednost 1 na
klientu ni priporočena); komentar `FarServer`; vrstice obnašanja `RwFarView`, `RwFarRange`, `RwImpostorStart`,
`RwImpostorAtlas`, `RwFarShaders` in coremoda `RenderGlobal` v 04-REGISTRI kažejo privzeto 2; README, 03-FAZE.

**Spremembe obnašanja moda:** z novim configom (ali brez ključa) strežnik pošilja nadomestke po kanalu `rwfar`
klientom s `HELLO`, klient riše L2 (sledeni onkraj 56 blokov) in L3 (onkraj sledilnika do `RwFarRange`/vd). Obstoječi
configi s ključem obdržijo vrednost; `dev/run/config/CustomNpcs.cfg` ima `RwFarView=0` (merilni pogoji nespremenjeni),
`dev/run-m7` ključa nima in dobi 2. Stari klient ali klient z 0 ne pošlje `HELLO` in ne dobi nič.
**Minecraft meritve:** nobene.

**Naslednji korak:** **K12**, nato ostali K-paketi (K13, K10 ponovno oceniti; kandidati iz M5-V.8: cena izrisa L3,
pas `rwfar` po chunkih, PBO peka). Neodvisno: čistilni korak K3, M4.14b/M4.16; ročna preverba z OptiFine.

### 2026-10-22 (113) — M5-V.8: sprejem vidnosti na daljavo — zaključeno (vklop v kodi M5-V.8b)

**Paket:** M5-V.8 · **Stanje:** **zaključeno** — vseh 15 zahtevanih testov zelenih na kodi `9f9125b0…`: `unit`, `build`,
`package` in 12 serij `v8-{10,24}-{1000,5000}-f{0,1,2}` (3 ponovitve, V0–V26 in T1–T6 zelene v 36 zagonih).

**Narejeno:** harness sprejema — `HarnessJob` `walk` in `snap.N`, `ClientHarness` hoja po merjenju (žični in `rwfar`
bajti) in posnetki pri FOV merjenja in 10, `FarRender.DRAWN_IDS` vsebuje tudi L2 (V24 preveri mejo model ↔ L2);
`v-run.ps1 -Sprejem 1 -Far 0|1|2` (V25, V26), `ponovitve-run.ps1 -Scenarij sprejem`; `HarnessJobTest` +1;
[scenarij](scenariji/M5-V.1-dalec.md).

**Izid** ([zapis](meritve/2026-10-22-M5-V.8-sprejem.md)): vd 24 / 5.000 sličica p95 9,188 ms (0) → **8,893** (2, v
šumu) / **10,873** (1, +1,685, ločeno); vd 10 / 5.000 4,936 → 3,658 (2); `rwfar` 88–92 µs/tick/igralca; `rwfar`
0 B/s mirovanje, 513 B/s hoja, žica brez spremembe; V24 0 vrzeli / 0 dvojnih; sonda vidna na 368 blokih; VRAM
≈ 21,6 MiB [K]. Cilji D-035 izpolnjeni pri **2** → **D-051**: privzeto `RwFarView=2`. L3 sam (1) cilja sličice ne
izpolni — zapisano; na prizorišču brez sledenih onkraj 56 bi L2 ne kompenziral (~+1,7 ms).

**Odločitev o razdelitvi:** sprememba privzete vrednosti v kodi bi spremenila odtis in razveljavila 12 serij meritev,
zato je ločen paket **M5-V.8b** (koda + komentar + vrstice obnašanja, `unit`/`build`/`package`), kot pri Q30.

**Spremembe obnašanja moda:** pri privzetih nastavitvah še nobene (`RwFarView=0` do M5-V.8b).
**Minecraft meritve:** 36 zagonov sprejema (+ 3 starejše serije `0837/0847/0857` na prejšnji kodi, niso v presoji).

**Naslednji korak:** **M5-V.8b** (privzeto `RwFarView=2`), nato K12 in ostali K-paketi (K13, K10 ponovno oceniti;
kandidati iz M5-V.8: cena izrisa L3, pas `rwfar` po chunkih, PBO peka). Neodvisno: čistilni korak K3, M4.14b/M4.16;
ročna preverba z OptiFine.

---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q32**, odločeno → D-039; Q30 odločen 21. 10. → D-049).

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
| Dnevnik sej: vnosi od seje 111 (21. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q29, **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
