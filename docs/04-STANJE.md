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
| Zadnja posodobitev | **2026-10-26** (seja 153) — **M4.14d poskus 2 zavrnjen** (D-083, Q74): G iskanj 18 → 4, obtičanja 3, a G DONE 288 > 246 in FA5 G 5 > 4; produkcija povrnjena na `cbad3c3`, M4.14d odložen. Prej (seja 152): **M4.16b zaključen** (D-082, Q69–Q71): 200 letečih v originalu, patrulja MSPT p95 8,651 (8,651–9,175) ms, `npc.us` 17,16; leteči boj p95 2,228 (2,228–2,294) ms, `npc.us` 7,62; L1/L2/P5 zeleni. Prej (seja 151): **M4.16 zaključen** (D-081, Q68): original na štirih progah z enim `navigateTo` 0/6, osvežen zid 6/6, drsenje po prihodu ≥ 1,76/2,38, hitrost iz `navigateTo` ne velja; razpon 0. Prej (seja 150): **M4.14d poskus 1 (TRACK) zavrnjen** (Q65): FA5 G formalno uspe, a obtičanja G 3–6 → 10–14 in G DONE 233–246 → 318/348; produkcija povrnjena na `cbad3c3`, M4.14d odložen do novega pristopa. Prej (seja 149): **M4.14b zaključen kot preverba, ne izboljšava** (D-079/Q62): na zamrznjenem 984dc27a… imajo vsi ID-ji D-078 rezultat; harness-i, unit, build in package uspejo, sledi popolne (trace-v1 1.664, recovery in navigator 8.960, `iskanj_izgub` G 0). Padejo samo FA1/2 (merilo skupnega cilja proti 8 mestom; 8/8 mest OK), FA4 G 7,571–8,592 / O 6,42 (geometrija kolone skozi vrata) in FA5 G 19 > 4 (člani ne sledijo skupni poti → M4.14d). Pragovi isti, F1–F12 odprti (ročno, uporabnik). |
| Trenutni milestone | **M4 — Gibanje in navigacija** po D-038; raziskava M4.0 že opravljena 20. 10. **M5 zaključen 22. 10.**: strežnik M5.8, klient M5-K.2, daljna vidnost M5-V.8 in regresija M5.Z. M3 do M3.9 (GUI M3.8b odprt); M2 zaključen 5. 10. |
| Naslednji paketi | **M4.10** → (M4.14d odložen: poskusa 1 in 2 zavrnjena, Q65/Q74; meje G DONE ≤ 246, obtičanja ≤ 6) → M4.17 → Hover (D-038, raziskava M4.0 že narejena). Neodvisno in pred izdajo **Q22** (pravi Forge/SRG pot zapakiranega coremoda); M3.8b ter preverba OptiFine/Entity Culling v nameščenem okolju. Odprto: PR `codex/m7-cnpc-integration` → `main` |
| Baritone (M7, izven vrstnega reda) | **V4 sprejet 28. 9.** (M7.10c): z `crowdYield` 8/8 na obeh grlih (vanilla 1/8, 6/8) in odprtem, razpon 0; izbirno ozadje, privzeto izklopljeno (`RwNavBackend=0`). [meritev](meritve/2026-09-28-M7.10-cakanje-v-gneci.md) |
| Prevedljivih razredov | **182** datotek `.java` v `src/patch` (22. 10.; +1 M5.Z: `rework/diag/ConfigDefaults`), od tega **117** v `noppes/npcs/rework/…` (`ai` 5, `core` 10, `data` 5, `diag` 26, `entity` 13, `far` 21, `formation` 15, `nav` 12, `net` 3, `render` 7) in 3 dostopni razredi v `net/minecraft/…`; `verify-package`: **71** vnosov (70 razredov originala + manifest coremoda) |
| Testi | **422 JUnit** metod `@Test` v **69** datotekah (26. 10.; +2 D-077) + **40 primerjalnih testov originala**. Na 984dc27a… unit/build/package in vsi trije formacijski harness-i uspejo; FA sprejem M4.14b pade (D-079). Dodatno **20** preverb dedicated-server smoka in **16** trditev samotesta protokola ponovitev |
| Odprti pojavi | nobenega blokirnega; P1 (ovržen 21. 9., ostane R-P1b) in P2 — glej razdelek spodaj |
| Blokade | Q1 je **trajna blokada do M10** (uporabnik nima dostopa do modpacka/sveta); Q6, Q8, Q10, Q12, Q14 in Q31 odprta; specifična R9 forenzika odložena, ne blokirana |
| Omejitev orodij | Windows (DESKTOP-SN494C0), PowerShell, Gradle in JDK 8; **v avtonomni izmeni teste, build in Minecraft zaganja samo upravljalnik** (DIRECT OWNER INSTRUCTION). Seja ureja datoteke in zahteva test ID-je. Python ni nameščen (node je) |

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
| M4 Gibanje | **M4.0, M4.16 in M4.16b zaključeni; M4.14b zaključen 26. 10. kot preverba (D-079)** | [dokazi](meritve/2026-10-22-M4.14b-formacije.md): FA1/2, FA4 G/O in FA5 G padejo z zapisanimi vzroki, pragovi isti; F1–F12 odprti. M4.14d poskusa 1 in 2 zavrnjena (Q65, Q74, [zapis 1](meritve/2026-10-26-M4.14d-sledenje-poti.md), [zapis 2](meritve/2026-10-26-M4.14d-poskus2.md)). M4.16 zaključen 26. 10. ([zapis](meritve/2026-10-26-M4.16-letenje.md)), M4.16b 26. 10. ([zapis](meritve/2026-10-26-M4.16b-leteci-200.md)). Naslednji M4.10 → M4.17 → Hover; M4.15 neodvisno |
| M5 Performance | **zaključeno 22. 10. (D-055)** | M5-S: kapaciteta M5.8; M5-K: sprejem M5-K.2; M5-V: sprejem M5-V.8 in privzeti vklop V.8b; M5.Z: obe matriki in sled zeleni. [Regresija](meritve/2026-10-22-M5.Z-regresija.md); nepokrita release preverjanja ostanejo M10 |
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
[`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) (vnosi 151 → 1, novejši zgoraj).*


### 2026-10-26 (153) — M4.14d poskus 2 izmerjen in zavrnjen

**Paket:** M4.14d (D-083, Q72–Q74) · **Stanje:** **zavrnjeno, odloženo**.
[Zapis](meritve/2026-10-26-M4.14d-poskus2.md). AI ni zaganjal testov/builda/Minecrafta.

Poskus 2: opravičilo ne vsili iskanja, preverba ravne črte (`straightWalkable`), TRACK samo
naprej ob zaprti črti, eno iskanje na obtičanje; unit +12 (Q73: M4.14b test prilagojen z zidom).
Na 6f39a760… unit, build, package in navigator-harness-v3 zeleni; navigator-trace-v3 izhod 1:
G `iskanj` 18 → 4 (+8 TRACK), obtičanja 3 (≤ 6), a **G DONE 288 > 246**, **FA5 G 5 > 4**,
FA4 G 9,326. Q74: ni sprejet; koda v `bf02674`, produkcija in testi povrnjeni na `cbad3c3`
(422 JUnit v 69 datotekah, 182 razredov, po grep). M4.14d odložen do per-članske analize zamude
ob vratih. Naslednji M4.10.

### 2026-10-26 (152) — M4.16b: celica `leteci-200` izmerjena

**Paket:** M4.16b (D-082, Q69–Q71) · **Stanje:** **zaključeno**.
[Zapis](meritve/2026-10-26-M4.16b-leteci-200.md). AI ni zaganjal testov/builda/Minecrafta.

Brez produkcijske kode. Varianti `leteci` (N × `PERF_Leteci`, tava v `WalkingRange 16`) in
`leteci-boj` (`PERF_LetA/LetB`, frakciji 1↔2) v `perf-run.ps1`; krmilnik izpiše `letecih`,
`vZraku`, `odDoma`, merili L1/L2. Na 34b3574d… unit/build/package, `m416b-leteci-v1` in
`m416b-letboj-v1` zeleni (T1–T6; v vsakem zagonu P1–P9, L1, L2 oz. P5). Patrulja: p50 1,475,
p95 8,651 (8,651–9,175), p99 10,224 ms, `npc.us` 17,16, 65 MB/s, `odDoma` 197–200. Boj: p50 1,507,
p95 2,228 (2,228–2,294), p99 2,753 ms, `npc.us` 7,62, 12,4 MB/s, s ciljem/ranjenih 200/200.
Okvirno proti M2.6 (Q71, drug dan in koda): patrulja dražja od stoječega `idle-200` (p95 +40 %,
vsebuje tavanje), leteči boj cenejši od `boj-200` (p95 −43 %). Naslednji M4.14d (nov pristop).



---

## Odprta vprašanja za uporabnika

Samo **odprta** vprašanja (in Q1 kot trajna blokada). Ko je vprašanje odločeno, se vrstica
**prestavi** v [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) §4 (tam so vsa vprašanja
Q1–Q29 s stanjem na 2026-10-19; odločitev se zapiše tam in v `01-ARHITEKTURA.md` §9).
Nova vprašanja dobijo naslednjo prosto številko (zadnja uporabljena: **Q62** → D-079; Q61 → D-078; Q60 → D-077; Q59 → D-076; Q58 → D-075; Q57 → D-074; Q56 → D-073; Q55 → D-072; Q54 → D-071; Q53 → D-070; Q52 → D-069; Q51 → D-068; Q50 → D-067; Q49 → D-066; Q48 → D-065; Q47 → D-064; Q46 → D-063; Q45 → D-062; Q44 → D-061; Q43 → D-060; Q42 → D-059; Q41 → D-058; Q40 → D-057; Q37–Q39 → D-056; Q35/Q36 → D-055).

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
| Dnevnik sej: vnosi od seje 140 (22. 10.) nazaj do seje 1 (11. 9., postavitev načrta) | [`arhiv/04-STANJE-dnevnik.md`](arhiv/04-STANJE-dnevnik.md) | `grep` po paketu (npr. `M5.16`), seji ali ključni besedi |
| Polna tabela "Trenutno stanje" (18. 10.), polna tabela milestonov, M1/M2 po paketih, **vsa** vprašanja Q1–Q56 (odločena), **Znane omejitve** (pasti scenarijev, pravila merjenja, okolje) | [`arhiv/04-STANJE-tabele.md`](arhiv/04-STANJE-tabele.md) | `grep`; pred pisanjem novega scenarija preglej §5 Znane omejitve |
| **Sprejete spremembe obnašanja**, **Prevzeta koda**, **Meritve** (živi registri; nove vrstice se dodajo tam) | [`04-REGISTRI.md`](04-REGISTRI.md) | ko dodajaš stikalo, prevzeto kodo ali meritev; `grep` po stikalu (`Rw…`) |
| Polni zapisi meritev | [`meritve/`](meritve/) | po povezavi iz registra |
