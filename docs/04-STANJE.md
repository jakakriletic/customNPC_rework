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
| Zadnja posodobitev | **2026-10-21** (seja 109) — **M5-V.5 zaključen**: izris L3 (impostorji iz atlasa v enem `BufferBuilder`, billboard, lightmap, megla, interpolacija, šiv po `entityId`, coremod v `RenderGlobal.renderEntities`), D-047; vseh 5 zahtevanih testov zelenih na kodi `92f1a22b…` (vd 24/5000: 4831 impostorjev v vsaki sličici, najdlje 368 blokov, sonda na 368 vidna; hoja čez mejo 64 brez vrzeli) |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 0, Q30) in K4 (privzeto 1) 19. 10., K3 zavržen 21. 10. (A/B brez koristi), K5 L1 v kodi 21. 10. (privzeto 0, D-039); M5-V.1 zaključen 21. 10. (varianta A zavrnjena, D-043), M5-V.2 (strežnik `rwfar`) zaključen 21. 10. (privzeto 0, D-044), M5-V.3 (opis videza) zaključen 21. 10. (D-045), M5-V.4 (peka impostorjev) zaključen 21. 10. (D-046), M5-V.5 (izris L3) zaključen 21. 10. (D-047). M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **M5-V.6** (L2: sledeni oddaljeni NPC-ji kot impostorji), nato V.7, V.8 (vidnost NPC-jev do 24 chunkov, D-034/D-035; Q30 vklop K14b še odprt), K12, ostali K-paketi. Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** narejen 20. 10. (D-038); od M4 se lahko vrineta **M4.14b** (formacije v svetu, samo zagon) in **M4.16** (scenarij letenja), ko računalnik ni v rabi. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **178** datotek `.java` v `src/patch` (21. 10.; +5 M5-V.5: 3 `rework/far`, 1 `rework/core`, 1 `rework/diag`), od tega **113** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 25, `entity` 13, `far` 18, `formation` 15, `nav` 12, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **375 JUnit** (metod `@Test`, preštetih z grep 21. 10.; +19 M5-V.5) v 64 datotekah — zeleni na kodi `92f1a22b…` + **40 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
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
| M5 Performance | **M5-S zaključen 16. 10.**; M5-K.0 (16. 10.) in M5-K.1 (18. 10.) zaključena, K14 zavržen, K14b v kodi (19. 10., `RwHeadwearSkip` privzeto 0, Q30: −25 % idle-500), K4 (19. 10., `RwLabelCache=1`, −7 % idle-200), K3 zavržen (21. 10., A/B brez koristi, `RwEyesList=0`), K5 L1 (21. 10., `RwLodStart=0`, D-039: idle-500 −24 % pri vklopu); M5-V.1 zaključen 21. 10. (D-043), M5-V.2 (D-044), M5-V.3 (D-045), M5-V.4 (D-046) in M5-V.5 (D-047) zaključeni 21. 10.; naprej M5-V.6 (D-034, D-035) | M5-S: S1, S2, S14b, S4, S7, S5 (D-037, `RwPathNegCache=1`), S14c, S16, M5.15 (coremod), M5.16 (`RwMoveBoxes`) vsi privzeto vklopljeni po D-032; M5.2 zavržen. Podrobnosti po paketih: [arhiv, tabele §2](arhiv/04-STANJE-tabele.md); razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 107 → 1, novejši zgoraj).*


### 2026-10-21 (109) — M5-V.5: nadomestki in izris L3 — zaključeno

**Paket:** M5-V.5 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `92f1a22b…`: `unit`, `build`,
`package`, `v5-izris-24-64-5000`, `v5-izris-10-64-1000` (V0–V24 zeleni v obeh zagonih).

**Narejeno (D-047):**
- `FarRender` (en `BufferBuilder` za vse nadomestke, pol-vidni v drugem z alfo 0,15; cilindrični billboard, okvir po
  kotu `ImpostorFrames.frameFor`, alfa test 0,1, lightmap kraja `World.getCombinedLight`, megla; šiv po `entityId`,
  mrtva entiteta z nadomestkom se ne nariše), `FarInterp` (premik v 1 s od prikazanega položaja, skok nad 16 blokov),
  coremod `RenderEntitiesPatch` (`RenderGlobal.renderEntities` pred `"blockentities"`, prehod 0; rezerva
  `RenderWorldLastEvent`), ukaz `/rwimpostor`. `FarBand.PREFETCH`: strežnik videz izračuna 16 blokov pred notranjo
  mejo, da je ob prehodu že pečen.
- `FarDrawCheck` (harness): sonda na robu pasu, A/B/C pikslov v pravi sličici s FOV 10; faza `SEAM` s hitrostjo hoje.
  `v-run.ps1 -Izris 1` z merili V22–V24
  ([scenarij](scenariji/M5-V.1-dalec.md#m5-v5-izris-l3-v22v24--far-1--impostor-1--izris-1)).
- JUnit: `FarInterpTest` (9), `RenderEntitiesPatchTest` (4, prave bajte Forge 1.12.2), `FarDrawCheckTest` (4),
  `FarBandTest` (+1), `HarnessJobTest` (+1); izključeni iz `testOriginal`.

**Izid v svetu (ena ponovitev na celico, `-Far 1 -Impostor 1 -Izris 1`, nočni upravljalnik):**

| Celica | Narisanih min/max (pričakovano) | Živih v šivu | Najdlje | Izris povp. / max | V23 sonda | V24 vrzeli / dvojnih / prehodov | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 4831 / 4831 (4820..4841) | 40 | **368,0** | 1.305,6 / 18.404 µs | 368 blokov: 114 px, razlika 165, kontrola 0 | **0 / 0** / 170 + 150 | 9,42 / 14,45 (FPS 99,9) | 887 | 58,72 / 71,303 |
| vd 10, domet 64, 1.000 | 176 / 176 (170..176) | 0 | 168,8 | 53,1 / 160,5 µs | 148 blokov: 810 px, razlika 98, kontrola 0 | **0 / 0** / 25 + 25 | 1,23 / 2,10 (FPS 761,8) | 170 | 1,245 / 1,704 |

- Poseg coremoda uspel v obeh zagonih, 0 sličic prek rezerve; 0 izven frustuma, 0 nadomestkov brez peke.
- Pot do zelenega: prvi zagon (`9c004fa8…`) je dal v V24 15 vrzeli (videz 0 v prvem krogu → nepečen ob prehodu) in
  400 dvojnih (mrtva entiteta v chunku do ticka) — popravljeno s predpasom videza in skrivanjem mrtve entitete.
  Drugi (`1538c62d…`) je pri vd 10 dal 0 pikslov sonde na 168,8 bloka (za koncem megle 160, neviden tudi v
  originalu) — sonda je odslej najbolj oddaljen nadomestek ≥ 12 blokov pred koncem megle; merilo pri vd 24 ostane 368.
- Sličica pri vd 24/5000: p50 9,42 ms proti 7,47 ms v M5-V.4 brez izrisa (ena ponovitev, ne A/B) — izris 4831
  impostorjev stane ~1,3 ms na sličico, kar se ujema s števcem `FarRender`. Primerjava z originalom je M5-V.8.
- Surovi izidi: git-ignored `audit/v-24-64-5000-izris-2026-10-21-141027*`, `audit/v-10-64-1000-izris-2026-10-21-141445*`.

**Spremembe obnašanja moda:** pri privzetih nastavitvah nobene vidne; coremod spremeni bajtkodo
`RenderGlobal.renderEntities` (klic, ki se pri `RwFarView=0` takoj vrne — registrirano v 04-REGISTRI).
**Minecraft meritve:** 6 zagonov `-Izris 1` (2 zelena na končni kodi).

**Za M5-V.8:** `FarRender` riše tudi nadomestke za koncem megle (`RD · 16`), kar je odvečno delo; hitrejše premikanje
od ~8 blokov/s lahko na meji pusti vrzel do ~1 s.

**Naslednji korak:** **M5-V.6** (L2: sledeni oddaljeni NPC-ji kot impostorji, `RwFarView=2`). Neodvisno: čistilni
korak K3, M4.14b/M4.16; Q30 odprt.

### 2026-10-21 (108) — M5-V.4: peka impostorjev na klientu — zaključeno

**Paket:** M5-V.4 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `86da496c…`: `unit`, `build`,
`package`, `v4-imp-24-64-5000`, `v4-imp-10-64-1000` (V0–V21 zeleni v obeh zagonih).

**Narejeno (D-046):**
- `ImpostorFrames` (čiste funkcije brez GL: postavitev traku 8 celic 40 × 72, obrez, ploskovno pomanjšanje v
  ≤ 32 × 64, mipmapi z ohranjeno pokritostjo alfe, izbira okvirja po kotu), `ImpostorSlots` (LRU trakov),
  `ImpostorAtlas` (tekstura GL, 4 stopnje mipmap, stran `RwImpostorAtlas`, privzeto 2048 = 168 videzov),
  `ImpostorBaker` (vrsta, en videz na sličico, lažna entiteta, FBO 128 × 256, ortogonalno pod 8 koti, brez
  lightmapa; odlog pri skinu igralca/URL; izklop brez FBO). `FarClient` videz postavi v vrsto peke ob `LOOK`
  in ob vnosu z znanim, nepečenim videzom; ob izklopu `RwFarView` sprosti atlas.
- `ImpostorCheck` (harness): primerjava pikslov impostor ↔ model pri 56 blokih (perspektiva z gostoto pikslov
  zaslona 1080 p, FOV 70) za 8 videzov × 8 kotov in kontrola občutljivosti; merila določena pred meritvijo.
- Harness: `impostorCheck=1`, polja `imp*` v JSON klienta; `v-run.ps1 -Impostor 1` z meriloma V20/V21
  ([scenarij](scenariji/M5-V.1-dalec.md#m5-v4-peka-impostorjev-v20v21--far-1--impostor-1)).
- JUnit: `ImpostorFramesTest` (9), `ImpostorSlotsTest` (3), `HarnessJobTest` (+1); izključeni iz `testOriginal`.

**Izid v svetu (ena ponovitev na celico, `-Far 1 -Impostor 1`, nočni upravljalnik):**

| Celica | Pečenih / v atlasu / brez peke / napak | Peka povp. / max ms | rwfar µs/tick/igralca | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 9 / 9 / **0** / 0 | 12,30 / 49,89 | 92,123 | 7,47 / 11,96 (FPS 125,0) | 915 | 57,672 / 71,303 |
| vd 10, domet 64, 1.000 | 9 / 9 / **0** / 0 | 8,87 / 32,46 | 6,051 | 1,16 / 1,48 (FPS 827,1) | 172 | 1,147 / 1,573 |

Primerjava pikslov (enaka v obeh zagonih; IoU in razlika združeno čez 8 kotov, prag IoU ≥ 0,70, razlika ≤ 32/255):

| Videz | IoU (najslabši kot) | Razlika barve | Piksli model / impostor | Okvir |
|---|---|---|---|---|
| osnova (`PERF_Idle` iz kanala) | 0,884 (0,818) | 7,6 | 1656 / 1788 | 32 × 58 |
| `elffemale.png` | 0,902 (0,848) | 14,8 | 1676 / 1820 | 32 × 58 |
| `elfmage.png` | 0,912 (0,864) | 11,5 | 1702 / 1828 | 32 × 58 |
| `elfqueenformal.png` | 0,906 (0,856) | 21,7 | 1676 / 1808 | 32 × 58 |
| `elfsoldierhelmet.png` | 0,911 (0,856) | 22,0 | 1692 / 1820 | 32 × 58 |
| `blood_elf_mage.png` | 0,902 (0,856) | 15,1 | 1672 / 1804 | 32 × 58 |
| velikost 10 | 0,926 (0,789) | 4,2 | 6540 / 6828 | 32 × 57 |
| velikost 2 | 0,894 (0,789) | 12,5 | 239 / 244 | 32 × 57 |

- Občutljivost: enobarvni skin proti modelu osnove da razliko **95,5** (> 64), torej merilo razlike zazna napačno
  teksturo. Impostor ima 2–9 % več pokritih pikslov kot model (robovi pomanjšanja in mipmapov), IoU ostane ≥ 0,88.
- 9 pečenih = 1 videz iz kanala (vsi kloni) + 8 videzov preverbe; 0 odloženih, 0 obrezanih na robu, FBO na voljo.
- Peka enega videza stane 9–12 ms povprečno, največ 50 ms (`glReadPixels`); proračun en videz na sličico omeji
  zastoj na eno sličico na nov videz. Za M5-V.8: pri mnogo različnih videzih ob vstopu v svet je to zaporedje
  počasnih sličic — kandidat za asinhrono branje (PBO).
- Sličica/MSPT brez A/B (izrisa impostorjev še ni, M5-V.5); primerjava z originalom je M5-V.8.
- Surovi izidi: git-ignored `audit/v-24-64-5000-imp-2026-10-21-131120*`, `audit/v-10-64-1000-imp-2026-10-21-131528*`.

**Spremembe obnašanja moda:** nobene pri privzetih nastavitvah (`RwFarView=0`: peka se ne zgodi, atlas se ne
ustvari). **Minecraft meritve:** 2 zagona `-Far 1 -Impostor 1`.

**Naslednji korak:** **M5-V.5** (nadomestki in izris L3 iz atlasa). Neodvisno: čistilni korak K3, M4.14b/M4.16;
Q30 odprt.

---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q32**, odločeno → D-039).

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
| Q30 | **K14b** je v kodi (`RwHeadwearSkip`, privzeto 0): pokrivalo ne riše štirikotnikov, ki vzorčijo samo prozorne teksle. idle-500 p50 **11,49 → 8,60 ms (−25 %)**, idle-200 6,52 → 5,32 (−18 %), 3 + 3 ponovitve, razponi ločeni; preverba pikslov 7 serij × 200 NPC-jev (skin, vzorec, 5 pravih skinov) **0 razlik** barve in globine. Ni pa *dokazano* enako: fragment točno na robni črti štirikotnika lahko v originalu vzame sosednji neprozoren teksel (enopikselni rob klobuka); strogo enaka različica (z robom) ne da dobitka. Naj bo **privzeto 1** (priporočeno; razlika, če obstaja, je posamezen piksel na robu klobuka) ali ostane 0? | M5-K K14b, privzete nastavitve | odprto |
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
| Dnevnik sej: vnosi od seje 103 (21. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q29, **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
