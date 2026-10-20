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
| Zadnja posodobitev | **2026-10-20** (seja 99) — **OPS.1**: lokalni nočni upravljalnik (`nocni-run.ps1`, [navodila](../overnight/README.md)); 29/29 testov krmiljenja, trajna vrsta in native Codex/Claude adapterji. Pravi cikel še nepreverjen, Claude prijavi čakata uporabnika; A/B K3 ostane naslednji projektni korak. |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 0, Q30) in K4 (privzeto 1) 19. 10., K3 v kodi (A/B 20. 10.). M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **A/B K3** (`audit/m5k3-ab.ps1`, ~40 min brez miške nad oknom MC; vklop K14b Q30), nato **K5** (LOD), **M5-V** (vidnost NPC-jev do 24 chunkov, D-034/D-035), K12, ostali K-paketi. Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** narejen 20. 10. (D-038); od M4 se lahko vrineta **M4.14b** (formacije v svetu, samo zagon) in **M4.16** (scenarij letenja), ko računalnik ni v rabi. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **155** datotek `.java` v `src/patch` (19. 10.; +1 prenos 1:1 `LayerEyes`, +2 K3), od tega **90** v `noppes/npcs/rework/…` (`ai` 5, `core` 9, `data` 5, `diag` 22, `entity` 13, `formation` 15, `nav` 12, `net` 3, `render` 6) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **306 JUnit** v 51 datotekah (19. 10.; +4 `EyesListTest`, +2 `LayerEyesBaselineTest`; vsi zeleni) + **40 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
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
| M5 Performance | **M5-S zaključen 16. 10.**; M5-K.0 (16. 10.) in M5-K.1 (18. 10.) zaključena, K14 zavržen, K14b v kodi (19. 10., `RwHeadwearSkip` privzeto 0, Q30: −25 % idle-500), K4 (19. 10., `RwLabelCache=1`, −7 % idle-200), K3 v kodi (`RwEyesList=0`, A/B 20. 10.); naprej K5 → M5-V (Q28, Q29, D-035) | M5-S: S1, S2, S14b, S4, S7, S5 (D-037, `RwPathNegCache=1`), S14c, S16, M5.15 (coremod), M5.16 (`RwMoveBoxes`) vsi privzeto vklopljeni po D-032; M5.2 zavržen. Podrobnosti po paketih: [arhiv, tabele §2](arhiv/04-STANJE-tabele.md); razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 97 → 1, novejši zgoraj).*

### 2026-10-20 (99) — OPS.1: nočni upravljalnik testov in AI iteracij

**Paket:** OPS.1 (lokalno razvojno orodje) · **Stanje:** implementirano in krmiljenje preverjeno;
**pravi Minecraft/AI cikel še nepreverjen**, Claude native prijavi čakata uporabnika.

**Narejeno:**
- `nocni-run.ps1`, `ZAZENI-NOCNI.bat` in `overnight/`: trajna vrsta, ločeni delovni procesi,
  nadaljevanje zaključene celice po prekinitvi upravljalnika, strukturirane AI odločitve,
  Codex/Claude native CLI, predaja rezultatov med profili in čakanje na kvoto. Ni API ključev
  ali branja/kopiranja prijavnih žetonov. Native prijava se preveri pred AI klicem.
- Začetna lokalna vrsta vsebuje samo že pripravljeni K3 (unit/build/package + idle-200/500,
  oči 0/1, po 3 ponovitve). AI ne more odstraniti obveznih testov ali zaključiti s starimi
  rezultati. SHA-256 vhodov prepreči mešanje različnih verzij. K3 ni označen kot opravljen.
- Pred klientom: aktivno lokalno namizje, 120 s brez vnosa, brez drugih Java procesov in
  CPU ≤ 20 %. Vnos med serijo pomeni neveljavno meritev. Globalni Windows mutex prepreči dva
  upravljalnika; procesna zahteva zadrži spanje brez trajne spremembe power nastavitev.
- Navodila in meje: [`overnight/README.md`](../overnight/README.md). Lokalne poti/stanje/native
  profili so git-ignored. Ponovljiv urnik ni nameščen; uporabnik ročno zažene 8-urno okno.

**Preverjanje:** **29/29** Node testov: parserji ponudnikov, kvote/failover/reset, nespremenljivi
obvezni testi, meje iteracij/ponovitev, izhodni kode/timeout, Windows mutex, literalni PowerShell
argumenti (presledki, šumniki, `$()`) in resnični ločeni sintetični proces po izhodu upravljalnika.
`init`, `doctor`, suhi zagon ter Node/PowerShell sintaksa uspešni. Pravi Minecraft, Gradle build
in modeli pri tej postavitvi niso bili zagnani. Codex ChatGPT auth potrjen; Claude Code 2.1.282
najden v VS Code, `auth status` vrne brez prijave.

**Naslednji korak:** uporabnik izvede `login claude-1` in `login claude-2`, nato prvi pravi
`run --hours 8` (ali `--test-only`) med odsotnostjo. Preveriti nočni desktop/zaklep in prvo
pravo integracijo. Prihodnji K5/M5-V scenariji še niso v katalogu; manjkajoča merila vrnejo
blokado za pregled. Nadaljnji vrstni red M5 in odprta Q so nespremenjeni.

**Spremembe obnašanja moda:** nobene. **Minecraft meritve:** nobene.

### 2026-10-20 (98) — M4.0: raziskava izvedbe M4 (gibanje in letenje)

**Paket:** M4.0 · **Stanje:** **zaključeno** (raziskava po §1a, brez produkcijske kode in brez zagonov
Minecrafta — uporabnik je delal na računalniku).

**Narejeno:**
- Koda letenja in navigacije: CNPC `FlyingMoveHelper`/`EntityNPCFlying`, vanilla 1.12 `PathNavigate`,
  `PathNavigateFlying`, `PathFinder`, `FlyingNodeProcessor`, `EntityGhast`, `EntityFlyHelper`; za primerjavo
  1.21.1 (`FlyingPathNavigation`, `FlyingMoveControl`, `PathNavigation`, `PathFinder`) in CustomNPC+ (`master`,
  `ai/pathfinder/`). Pregled vseh meritev gibanja (M2.3, M2.7, M2.5c, M5.6, M5.9–M5.11, M7.8, M7.10) z ločitvijo
  izmerjenega od ocenjenega. Zunanji viri (ARLR, Pufferfish, Lithium, SVO/Warframe, Theta\*, bug tracker).
- Zapis [`raziskave/M4.0-gibanje-in-letenje.md`](raziskave/M4.0-gibanje-in-letenje.md); `03-FAZE` M4 prepisan
  (D-038: M4.1/M4.4/M4.9/M4.12 zavrženi ali združeni, M4.5 in M4.11 pogojna, nova M4.16 scenarij in M4.17 vidna
  črta, merljiv izhodni kriterij); R2 v `02-ZAHTEVE` dopolnjen; README; Q31; priporočilo k Q6.

**Ugotovitve:**
- **Leteči NPC sledi poti brez preverbe ovir** [K]: vanilla `PathNavigateFlying.isDirectPathBetweenPoints` vedno
  vrne `true`, `pathFollow` skoči na zadnjo točko iste višine (tudi za vogalom), nato `isNotColliding` → `WAIT`.
  Obtičanje ob vodoravnem vogalu je [H] — M2.3 (zid čez vso širino, pot čez vrh) ga ne more videti.
- **`FlyingMoveHelper` = vanilla `GhastMoveHelper`** [K]: sunek vsake 4 ticke, po prihodu do ~3,5 bloka drsenja
  ([H] model). Časovna zaščita vozlišča je za leteče izklopljena (`getAIMoveSpeed()` = 0) [K], zato razlaga
  R-P1b s to zaščito odpade.
- **Cilj dlje od `NpcNavRange` vrne `null`, ne delne poti** [K] (`findPathOptions` zavrne sosede > 32 od cilja) —
  M4.10 razširjen na vmesni cilj.
- **CustomNPC+ ima isto iskanje kot vanilla 1.12**; uporabne so ideje okoli sledenja in krmiljenja (žarek ≤ 6
  blokov, hitrost, zavoj, višinska meja, spust brez poti). Port odpade.
- Kopensko: edini izmerjen strošek je sledenje poti (34,5 % boja-500 po S14b) → M4.13 (S15) ostane najbolj podprt.

**Spremembe obnašanja:** nobene. **Meritve:** nobene.

**Naslednja seja:** A/B K3 (`audit/m5k3-ab.ps1`, ko računalnik ni v rabi), nato po M5 vrstnem redu. Od M4 se lahko
vrineta M4.14b (formacije, samo zagon) in M4.16 (scenarij letenja, brez produkcijske kode). Odgovor na Q31 ni nujen
pred M4.16/M4.10/M4.17.


---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q31**).

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
| Dnevnik sej: vnosi od seje 96 (19. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q29, **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
