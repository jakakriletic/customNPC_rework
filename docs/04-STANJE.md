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
| Zadnja posodobitev | **2026-10-19** (seja 95) — **M5-K K14b v kodi**, stikalo `RwHeadwearSkip` (privzeto 0): pokrivalo izpusti štirikotnike, ki vzorčijo samo prozorne teksle; idle-500 p50 **11,49 → 8,60 ms (−25 %)**, idle-200 −18 %, preverba pikslov 7 serij 0 razlik. Strogo enaka različica (z robom) je pod šumom in zavržena; vklop privzeto → **Q30**. Zapis: [meritev](meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md) |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 0, Q30) 19. 10., naslednji K4. M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **K4 → K3** (brez spremembe videza; Q28, Q29; K14b narejen, vklop Q30), nato **K5** (LOD), **M5-V** (vidnost NPC-jev do 24 chunkov, D-034/D-035), K12, ostali K-paketi. Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** (raziskava M4) je lahko vmes; vrstni red v `03-FAZE.md` (D-030). Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **149** datotek `.java` v `src/patch` (19. 10.; +1 prenos 1:1 `LayerHeadwear`, +5 K14b), od tega **86** v `noppes/npcs/rework/…` (`ai` 5, `core` 9, `data` 5, `diag` 20, `entity` 13, `formation` 15, `nav` 12, `net` 3, `render` 4) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **69** vnosov (68 razredov originala + manifest coremoda) |
| Testi | **297 JUnit** v 47 datotekah (19. 10.; +8 `HeadwearSkipTest`, +1 `LayerHeadwearBaselineTest`; vsi zeleni) + **37 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
| Odprti pojavi | nobenega blokirnega; P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6, Q8, Q10, Q12 in Q14 odprta; specifična R9 forenzika odložena, ne blokirana |
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
| M4 Gibanje | **M4.14a v kodi** (izven vrstnega reda) | formacije prevedene in testirane v simulaciji, v svetu še ne — [zasnova](06-FORMACIJE.md), [scenarij](scenariji/M4.14-formacije.md); M4.15 (izbira vanilla/Baritone v GUI, D-036) |
| M5 Performance | **M5-S zaključen 16. 10.**; M5-K.0 (16. 10.) in M5-K.1 (18. 10.) zaključena, K14 zavržen, K14b v kodi (19. 10., `RwHeadwearSkip` privzeto 0, Q30: −25 % idle-500); naprej K4 → K3 → K5 → M5-V (Q28, Q29, D-035) | M5-S: S1, S2, S14b, S4, S7, S5 (D-037, `RwPathNegCache=1`), S14c, S16, M5.15 (coremod), M5.16 (`RwMoveBoxes`) vsi privzeto vklopljeni po D-032; M5.2 zavržen. Podrobnosti po paketih: [arhiv, tabele §2](arhiv/04-STANJE-tabele.md); razdelitev M5-S/M5-K v [09](09-PERFORMANCE-RAZISKAVA.md) |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 92 → 1, novejši zgoraj).*

### 2026-10-19 (95) — M5-K K14b: pokrivalo brez prozornih štirikotnikov

**Paket:** M5-K K14b · **Stanje:** **zaključeno** (koda pod stikalom, privzeto 0; vklop Q30).

**Narejeno:**
- **Q29 odločeno** (uporabnik: "nared kar misls da bi blo bolje" → priporočilo K14b pred K4).
- 1:1 prenos `LayerHeadwear` (1903772, bajtkoda 5/5), nato `rework/render/HeadwearQuads`, `HeadwearMask`,
  `HeadwearSkip`, `Model2DRenderer.rwCompileFiltered`, stikalo `RwHeadwearSkip`; harness `HeadwearCheck`
  (`k-run -Pokrivalo 1 -PreveriPokrivalo`, merili K9/K10).
- Preverba pikslov: 7 serij × 200 NPC-jev (skin, vzorec, 5 pravih skinov CNPC) **0 razlik** barve in globine.
- A/B 3 + 3 (`audit/m5k14b-ab.ps1`): idle-500 p50 11,493 → **8,595 ms (−25 %)**, p95 −24 %; idle-200 6,521 →
  **5,317 (−18 %)**; razponi ≤ 3,6 %, ločeni. [zapis](meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md)
- Testi 297/0 + testOriginal 37/0, `verify-package` PASS (69 vnosov).

**Ni narejeno in zakaj:** privzeti vklop — ni *dokazano* enako (fragment točno na robni črti lahko v
originalu vzame sosednji teksel), zato po D-007 → **Q30**.

**Ugotovitve:** strogo enaka maska (z robom enega teksla) pri standardni postavitvi skina ne izpusti
skoraj ničesar, ker so ob klobuku neprozorni teksli glave, telesa in rok (in čez `GL_REPEAT` noge) —
A/B v šumu, zavržena. Pravilo merjenja: pri dokazu "izpusti nevidno geometrijo" preveri sosednje teksle
v pravi postavitvi teksture, preden izmeriš.

**Spremembe obnašanja:** nobene privzeto (`RwHeadwearSkip=0`).

**Naslednja seja:** **K4** (imena: ~8 µs na napis, 20 % render niti pri 200 NPC-jih v dometu), nato K3.

### 2026-10-19 (94) — M5-K K14 zavržen: en list pokrivala ne pomaga, geometrija pa

**Paket:** M5-K K14 · **Stanje:** **zaključeno — zavrženo** (izid meritve, ne nedokončano delo).

**Narejeno:**
- Prenos 1:1 `Model2DRenderer` in `ModelHeadwear` v `src/patch` (`b896cfc`; `primerjaj-bytecode` 0 za pregled,
  `ModelHeadwearBaselineTest`). Ostane.
- Poskus `RwHeadwearList` (`03ab3bc`): 6 delov pokrivala v en display list (način 1 kliče liste otrok, 2 raven
  list), `HeadwearState` spremlja stanje otrok. Dokaz v klientu `k-run -PreveriPokrivalo` (`HeadwearCheck`, FBO,
  načini 0/1/2/0, skin + neprozorna testna tekstura): **0 razlik** barve in globine; med igro 0 prevezav.
- A/B 3 + 3 (`audit/m5k14-ab.ps1`): idle-500 p50 11,589 → 11,423 ms (razpona se prekrivata), idle-200
  6,538 → 6,590 → **zavrženo**, koda odstranjena v `4370184` (vrne se z `git revert 4370184`).
- Zgornja meja `HeadWearType=0` (3 ponovitve): idle-500 p50 **11,589 → 8,907 ms (−23 %)**, p95 −22 %.
- `dev/tools/skini-pokrivalo.js`: 41 % od 239 humanoidnih skinov CNPC ima povsem prozorno pokrivalo, prazni so
  60 % delov in 70 % trakov ekstruzije. [zapis](meritve/2026-10-19-M5-K-K14-pokrivalo-en-list.md)

**Ni narejeno in zakaj:** privzeti vklop (D-032) — ni dobitka nad šumom.

**Ugotovitve:**
- **Napačna premisa K14:** profil z enim listom pokaže pokrivalo še vedno 26,1 % — čas je v gonilniku pri
  izvajanju **816 oglišč na NPC** (6 × 34 štirikotnikov `renderItemIn2D`), ne v ~30 klicih GL. Pravilo merjenja
  dodano v Znane omejitve (arhiv §5).
- Skin fixture `humanmale/steve.png` ima prozoren klobuk: pokrivalo stane 23 % sličice in ne nariše niti
  enega piksla → nov kandidat **K14b** (izpusti geometrijo prozornih tekselov, ista slika) → **Q29**.

**Spremembe obnašanja:** nobene (stikalo poskusa odstranjeno; `verify-package` 68 vnosov: +2 razreda prenosa 1:1).

**Meritve:** enakost pikslov, A/B idle-500/idle-200 (3 + 3), zgornja meja (3), profil idle-500 z enim listom.

**Naslednja seja:** po odgovoru na Q29 **K14b** (priporočeno) ali **K4** (imena) po Q28.

---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q30**).

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | **odgovorjeno 17. 9.** — uporabnik nima dostopa; zabeleženo kot blokada (M0.7 §6), ponovno odpreti pred M10 |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | **odprto, a ne blokira M2.3** — scenarij meri samostojno letenje, kar je podlaga za oba primera; odgovor je potreben šele za obseg M4 |
| Q8 | Pri R8 — kateri provider (Anthropic / OpenAI / lokalni model)? | M9.3 | odprto |
| Q10 | Ali strežnik, kjer to teče, sploh ima izhodni internetni dostop? | M9.1 | odprto |
| Q12 | Uvoz zunanjih 3D modelov (npr. prenesen model zmaja) kot model NPC-ja — je to v obsegu? Če da: kateri format (`.bbmodel` / OBJ / JSON), in kako model pride do igralcev (resource pack / lokalna mapa / prenos z URL-ja / push s strežnika)? | nova zahteva (R11?; R10 je od 6. 10. poveljevanje prek zemljevida); odvisna od M7, ker zunanji model prinese svoj skelet | **odprto, odprto 17. 9.** — danes mod zna samo (a) teksture prek URL-ja (`skinUrl`, `capeTexture`, `overlayTexture`) in (b) prevzem modela **registrirane entitete** (`setModel(id)` → `ModelData.entityClass`, `DataDisplay:405`), torej model iz že nameščenega moda. Geometrija iz datoteke ne obstaja: vsi modeli so hardcoded `ModelBase` razredi (`client/model/`). V M8 katalogu tega ni, ker tega nima niti CustomNPC+. Ocena XL; odločitev šele ko je M7 (animacijski skelet) jasen |
| Q14 | Pri R10 — kateri zemljevid: lasten iz `ladja_mod` (izbira kot pri ladjah), JourneyMap, Xaero ali nov v CNPC? | M11.3 | **odprto 6. 10.**, ne blokira do M11 |
| Q18 | Popravek »prvi ukaz po zagonu serverja se izgubi« (ogrevanje vhoda z `list`) je zdaj samo v `perf-run.ps1`. Vsaka od 11 scenarijskih skript ima svojo kopijo `Start-DevServer`/`Send-Command`; v `nav-run`, `rwdiag-run`, `r1/r2-run`, `m36/m39-run`, `hb-run`, `fixture-run`, `testworld-run` in `smoke-server` lahko izgubljen prvi ukaz tiho spremeni pogoj meritve (npr. izgubljen `gamerule`). Naj se `Start-DevServer`, `Send-Command` in `Prime-ServerInput` preselijo v `meritve-lib.ps1` in popravijo vse skripte (svoj paket)? | vsi scenariji, veljavnost meritev | **odprto 10. 10.**; priporočilo: da |
| Q19 | Ostali trije klicatelji `Server.sendAssociatedData` (`UPDATE_NPC` ob uredbi NPC-ja, `PARTICLE`, `DELETE_NPC`) še pregledajo ~450 chunkov na klic. Niso na tick, zato izmerjene koristi ni; ob masovnem spawnu/brisanju (500 NPC-jev) pa je to 225.000 pregledov chunkov. Naj se preusmerijo tudi ti (S7b)? | M5, R5 | **odprto 10. 10.**; priporočilo: samo če scenarij pokaže številko |
| Q20 | Enakost S7 za **neprazen** seznam prejemnikov v svetu ni bila preverjena s pravim igralcem (dedicirani strežnik scenarija jih ne ima); stoji na viru vanille, 200.000 legah v testu in preverbi na NPC-jih. Naj se pred izdajo doda preverba v klientu (`/rwblink 2` z igralcem v svetu)? | M5.13, M10 | **odprto 10. 10.**; priporočilo: da, skupaj z M3.8b GUI preverbo |
| Q30 | **K14b** je v kodi (`RwHeadwearSkip`, privzeto 0): pokrivalo ne riše štirikotnikov, ki vzorčijo samo prozorne teksle. idle-500 p50 **11,49 → 8,60 ms (−25 %)**, idle-200 6,52 → 5,32 (−18 %), 3 + 3 ponovitve, razponi ločeni; preverba pikslov 7 serij × 200 NPC-jev (skin, vzorec, 5 pravih skinov) **0 razlik** barve in globine. Ni pa *dokazano* enako: fragment točno na robni črti štirikotnika lahko v originalu vzame sosednji neprozoren teksel (enopikselni rob klobuka); strogo enaka različica (z robom) ne da dobitka. Naj bo **privzeto 1** (priporočeno; razlika, če obstaja, je posamezen piksel na robu klobuka) ali ostane 0? | M5-K K14b, privzete nastavitve | odprto |
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
| Dnevnik sej: vnosi od seje 91 (16. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q29, **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
