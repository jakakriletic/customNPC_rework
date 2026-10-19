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
| Zadnja posodobitev | **2026-10-19** (seja 97) — **M5-K K3 delno**: oči iz display lista v kodi (`RwEyesList`, privzeto 0), preverba pikslov 0 razlik (tudi v povečavi ×8); **A/B ni pognan** (uporabnik rabi računalnik), meritev 20. 10. z `audit/m5k3-ab.ps1`. Prej isti dan K4 zaključen (`RwLabelCache=1`, idle-200 −7 %) |
| Trenutni milestone | **M5 — Performance**: M5-S (strežnik) zaključen 16. 10. (M5.8: idle 10×, boj 1,5×, nedosegljiva 1,4× pri p95 ≤ 25 ms); M5-K (klient): K.0 in K.1 zaključena, K14 zavržen, K14b v kodi (privzeto 0, Q30) in K4 (privzeto 1) 19. 10., K3 v kodi (A/B 20. 10.). M3 (jedro entitete) zaključen do M3.9 (GUI za hitbox način M3.8b v klientu odprt); M2 zaključen (baseline M2.6 zelen 5. 10.) |
| Naslednji paketi | **A/B K3** (`audit/m5k3-ab.ps1`, ~40 min brez miške nad oknom MC; vklop K14b Q30), nato **K5** (LOD), **M5-V** (vidnost NPC-jev do 24 chunkov, D-034/D-035), K12, ostali K-paketi. Pred izdajo **Q22** (pravi Forge strežnik: jar od M5.16 privzeto spreminja vanilla bajtkodo, produkcijska pot je preverjena samo statično). **M4.0** (raziskava M4) je lahko vmes; vrstni red v `03-FAZE.md` (D-030). Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **155** datotek `.java` v `src/patch` (19. 10.; +1 prenos 1:1 `LayerEyes`, +2 K3), od tega **90** v `noppes/npcs/rework/…` (`ai` 5, `core` 9, `data` 5, `diag` 22, `entity` 13, `formation` 15, `nav` 12, `net` 3, `render` 6) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **306 JUnit** v 51 datotekah (19. 10.; +4 `EyesListTest`, +2 `LayerEyesBaselineTest`; vsi zeleni) + **40 primerjalnih testov originala** (`:testOriginal` proti nespremenjenemu binarju). Dodatno **20** preverb dedicated-server smoka (M0.5) in **16 trditev samotesta protokola ponovitev** (`.ponovitve-samotest.ps1`, M2.5c) |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 95 → 1, novejši zgoraj).*

### 2026-10-19 (97) — M5-K K3: oči iz display lista (koda, A/B jutri)

**Paket:** M5-K K3 · **Stanje:** **delno** — koda, testi in preverba pikslov narejeni, **A/B ni pognan**
(uporabnik potrebuje računalnik; meritev 20. 10.). `RwEyesList` je zato privzeto 0.

**Narejeno:**
- Razčlenitev profila M5-K.1 (idle-500): 80 % časa `LayerEyes` je `Tessellator.draw` (`glDrawArrays` 54 %,
  `glVertexPointer` 23 %), menjave stanja in `setupFogColor` skupaj < 15 %.
- 1:1 prenos `LayerEyes` (7881e50; bajtkoda 8/8 enakih; `LayerEyesBaselineTest`).
- `rework/render/EyesList` (49bd247): ob prvem srečanju ključa (tip, vzorec, barva, sijaj pri živem NPC-ju,
  debelina in barva obrvi) se isti `Tessellator.draw` posname v display list (`glDrawArrays` z razrešenimi
  vrednostmi, kot vanilla chunki brez VBO) znotraj `glPushAttrib(GL_CURRENT_BIT)`; nato `callList`.
  `GlStateManager` uskladen (`color(1,1,1,1)` pred, `resetColor()` po, kot `WorldVertexBufferUploader`).
  Utrip in OptiFine po originalu; LRU 256. `EyesListTest` (4). Stikalo `RwEyesList`.
- Harness `EyesCheck` (`k-run -Oci 1 -PreveriOci`, merili K13/K14): idle-200, serije svet, vzorci
  (60 ključev), utrip — vse še v povečavi ×8 (projekcija); **0 razlik** barve in globine (povečava pokrije
  341.191 pikslov, druga barva oči spremeni 511), utrip gre po originalu. `audit/m5k3-preverba.out`.
- Testi 306/0 + testOriginal 40/0, `verify-package` PASS (71 vnosov).

**Kje je obstal / naslednji korak (natančno):** pognati `audit/m5k3-ab.ps1` (idle-200 in idle-500, `-Oci 0/1`,
3 + 3; ~40 min; **med meritvijo se miške nad oknom Minecrafta ne premika** — prvi poskus 19. 10. je padel
na K5, kamera 6,9°; skripta zdaj ponovi tudi padec K5). Če sta razpona ločena → `RwEyesList` privzeto 1
(D-032; dev `CustomNpcs.cfg` ostane 0), vrstici v registrih (spremembe obnašanja, meritve), zapis
`docs/meritve/2026-10-2?-M5-K-K3-oci-display-list.md`, K3 v `03-FAZE` prečrtan. Če ne → zavrženo kot K14.

**Ugotovitve:**
- Hitri zagon idle-200 (1 + 1, brez ponovitev) je bil neuporaben: dva zapuščena procesa `find /` sta
  pojedla CPU (p95 ~12 ms namesto ~7). Pravilo merjenja: pred A/B preveri tudi ne-java procese z velikim CPU.
- Oči so pri kameri M5-K le nekaj pikslov na NPC; preverba pikslov brez povečave je skoraj slepa
  (49 pikslov občutljivosti), zato povečava ×8.

**Spremembe obnašanja:** nobene (`RwEyesList=0`).

**Naslednja seja:** A/B K3 (zgoraj), nato K5 (LOD). Brez meritev je lahko vmes **M4.0** (raziskava M4).

### 2026-10-19 (96) — M5-K K4: napisi NPC-jev iz display lista

**Paket:** M5-K K4 · **Stanje:** **zaključeno** (`RwLabelCache` **privzeto 1**, D-032).

**Narejeno:**
- 1:1 prenos `RenderNPCInterface` (0abb22e; bajtkoda 26 metod: 18 enakih, 8 razlik v obliki, 0 za pregled;
  `RenderNPCInterfaceBaselineTest`), nato trije klici `drawString` v `renderLivingLabel` → `rwDrawLabel`.
- `rework/render/LabelCache`: ob drugem izrisu istega niza (barva, Unicode, bidi) posname ukaze v display list
  (`GL_COMPILE_AND_EXECUTE`) in jih nato predvaja; predpomnilnik `GlStateManager` (alfa, barva, tekstura) se
  pred snemanjem in po predvajanju uskladi. Original za prvi izris, podrazrede `FontRenderer`, OptiFine,
  `§k`/`§n`/`§m`; listi se pobrišejo ob ponovnem nalaganju virov; LRU 1.024. `LabelCacheTest` (2).
- Harness `LabelCheck` (`k-run -Napisi 1 -PreveriNapise`, merili K11/K12): idle-200, 174 napisov v dometu,
  serije ime testnega sveta in barvne kode/šumniki **0 razlik** barve in globine (drug naslov 2.817 pikslov).
- A/B 3 + 3 (`audit/m5k4-ab.ps1`): idle-200 p50 6,556 → **6,100 ms (−7,0 %)**, idle-50 2,164 → **2,025 (−6,4 %)**,
  p95 −6,1 / −7,6 %, razponi ločeni. [zapis](meritve/2026-10-19-M5-K-K4-napisi-display-list.md)
- Testi 300/0 + testOriginal 38/0, `verify-package` PASS (70 vnosov).

**Ni narejeno in zakaj:** napis bližje kot 4 bloke (drugi prehod brez globinskega testa) gre po isti poti,
v prizorišču ga ni in ni posebej preverjen s pikslom.

**Ugotovitve:** list prihrani ~⅓ stroška napisov (klici JNI); ostanek je izvajanje ~34 znakov v takojšnjem
načinu v gonilniku. Večji dobitek bi zahteval VBO/atlas za vse napise (ni v obsegu K4).

**Spremembe obnašanja:** nobene vidne; `RwLabelCache=1` privzeto (isti ukazi GL, 0 razlik pikslov). Dev
`CustomNpcs.cfg` drži 0, zato scenariji merijo original.

**Naslednja seja:** **K3** (oči, 3,5 µs na NPC, 10–14 % render niti); Q30 še odprto.

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
| Dnevnik sej: vnosi od seje 95 (19. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q29, **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
