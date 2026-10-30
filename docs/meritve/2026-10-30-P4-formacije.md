# P4 — formacije po merilih D-079 (2026-10-30)

**Paket:** P4 (Q248, D-079, Q257–Q262) · **Scenarij:** M4.14b proga G (zid z vrati širine 1) in O
(odprto), legija 8 članov (`formacije-run.ps1 -P4`) · **Izhodišče:** [M4.14b](2026-10-22-M4.14b-formacije.md).

## Merila (Q257)

D-079 pravi, da sta FA1/2 (prihod v 2 bloka skupnega cilja) in FA4 (premer ≤ 6) lastnosti meril
in geometrije: mesta legije(8,4) so do 3 bloke od cilja, idealni premer 6,325. Q257: FA1/2 se
sodita po 8/8 različnih zasedenih končnih mestih, FA4 po največjem zaostanku neopravičenega člana
za ukazanim ciljem med pohodom ≤ LAG_STOP + 1 = 6 (`zaostanek_max` iz `RWSQUAD konec`). Izvirni
vrednosti ostaneta v poročilu kot NOTE (ne vrata). FA3, FA5, FA6 nespremenjeni; varovalki M4.14d
(G DONE ≤ 246 tickov, obtičanja ≤ 6) sta vrata. Scena, config in pragovi so isti.

## Popravek (Q258–Q262, `SquadPlanner`, `Squad`, `PathTrack`)

- Zaprta ravna črta do mesta: član gre na točko kolone (rang × 1,4 za sidrom, na skupni poti).
- Zaprta črta do kateregakoli cilja: hoja po skupni poti proti točki 2 bloka pred projekcijo
  člana, brez iskanja. Opravičilo zaostanka ne vsili iskanja; iskanje dovoli samo obtičanje.
- `lineClear` preveri tudi kratke črte (< 1,5 bloka, Q259b); član, ki mu planer sam omeji hitrost,
  ni obtičan (Q261); prosta preverjena črta 6–14 blokov hodi STEER namesto PATH (Q262), član v HOLD,
  ki ga odrinejo > 6 blokov, ostane PATH (D-080).
- Zaostanek se ne šteje ob zamenjavi vrste cilja (mesto ↔ točka kolone) prvih 30 tickov in ne za
  ciljem za članom (Q259c).
- Unit: +7 testov (`SquadPlannerTest`, `SquadAdapterTest`), med njimi legija skozi vrata širine 1
  s fizičnim zidom.

## Testi (controller, `e964773f…`, poskus 1)

| Test ID | Rezultat |
|---|---|
| `unit`, `build`, `package` | PASS (550 `@Test`) |
| `p4-harness-v1` (`formacije-samotest.ps1`) | PASS |
| `p4-formacije-v1` | PASS — `audit/p4-formacije-2026-10-30-215808.md` |

Štiri prejšnje izvedbe `unit` (popravki znotraj poskusa 1, Q259a/Q260) so padle pred funkcijsko
serijo; nobena serija ni bila ponovljena.

## Izid FA (G / O)

| Merilo | M4.14b (22. 10.) | P4 G | P4 O |
|---|---|---|---|
| FA1/2-mesta, 8/8 hkrati | 8/8 / 8/8 | **8/8** (tick 100) | **8/8** (tick 40) |
| FA3, O vsa mesta ≤ 275 in ≤ +25 % reference | uspe | — | uspe |
| FA4-zaostanek ≤ 6 | — | **1,088** | **0,391** |
| FA5, prava iskanja ≤ 1 + replan + obtičanja | G 29–117 > meja | **1 ≤ 1** | 1 ≤ 1 |
| FA6, en DONE, 0 neporavnanih | G DONE 237 / MARCHING | **DONE 101** | DONE 40 |
| varovalki G: DONE ≤ 246, obtičanja ≤ 6 | — | 101, 0 | — |
| NOTE FA1/2 skupni cilj 8/8 | 5/8, 3/8 | 4/8 (ni izpolnjeno) | 3/8 (ni izpolnjeno) |
| NOTE FA4 premer ≤ 6 | 9,564 / 6,416 | 8,034 (ni izpolnjeno) | 6,428 (ni izpolnjeno) |

`RWSQUAD konec` G: `faza=DONE clanov=8/8 na_mestu=8 iskanj=0 obnov=0 obtical=0 kolona=0
neporavnani=0 tick=101 iskanj_izgub=0 po_poti=0 zaostanek_max=1.088`; O: `DONE tick=40 iskanj=0
obtical=0 po_poti=0 zaostanek_max=0.391`. Kontrola A (NAVIGATE brez formacije) G in O enaka
(neformacijsko obnašanje nespremenjeno), config SHA256 isti.

## Omejitve

- Sprejem velja za merila po Q257; izvirni skupni cilj in premer ostaneta neizpolnjena (geometrija).
- Ena ponovitev (Q250), ena scena; F1–F12 (ročni test z igralcem) ostanejo odprti.
- `kolona`/`po_poti` sta stanje ob koncu, ne skupno število; uporaba poti med vrati ni ločeno šteta.
- Zaostanek ob hitrem preklapljanju vrste cilja bi lahko skril pravi zaostanek (Q259c).
