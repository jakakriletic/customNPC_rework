# M5-S S2 — A/B poizvedbe iskalnika tarč samo po igralcih (2026-10-07)

**Hipoteza** ([09](../09-PERFORMANCE-RAZISKAVA.md), S2): po S1 iskalnik tarč pri idle-500 še vedno vsak
tretji tick (z verjetnostjo 1/4) vpraša kvader 65 × 34 × 65 blokov za vse `EntityLivingBase`, sortira ~425
kandidatov po razdalji in za vsakega izvede predikat do predzavrnitve. NPC brez stražarja,
spremljevalca-stražarja in `AttackOtherFactions` lahko po predikatu izbere samo igralca, zato zadostuje
poizvedba po `EntityPlayerMP`. Če hipoteza drži, se mora preostanek težkih tickov (~1/3 tickov nad
10 ms po S1) izničiti, alokacija pa se ne sme bistveno spremeniti (raytraceov po S1 ni več).

**Poseg:** `TargetPrefilter.scanClass` v `EntityAIClosestTarget.shouldExecute` po metu RNG, način
`RwTargetPrefilter=2` (S1 + S2; commit `3b712a8`). A = `-RwTarget 1` (samo S1), B = `-RwTarget 2`.
Izčrpen test ekvivalence: `TargetPrefilterTest.playersOnlyScanDropsOnlyCandidatesOriginalRejects`.

## Postopek

- Stroj: DESKTOP-SN494C0. `.\testworld.ps1`, nato
  `.\ponovitve-run.ps1 -Scenarij perf -AcceptEula -Dodatno @('-Variants',<v>,'-Counts','500','-RwTarget',<1|2>)`,
  3 ponovitve na celico v svežem svetu, ogrevanje 120 s, merjenje 300 s.
- Vrstni red: idle-500 A (17:50), idle-500 B (18:14), boj-500 A (18:38), boj-500 B (19:03). T1–T6 in
  P1–P9 zelena v vseh štirih serijah. Prvi poskus idle A (17:42) je P9 pravilno zavrnil: med celico se je
  zagnal uporabnikov gradle build (`ladja_mod/pirate-mca`); ponovljen v celoti.
- Zbirna poročila: [idle A](2026-10-07-M5-S2/idle500-A.md), [idle B](2026-10-07-M5-S2/idle500-B.md),
  [boj A](2026-10-07-M5-S2/boj500-A.md), [boj B](2026-10-07-M5-S2/boj500-B.md)
  (kopije `audit/m25c-perf-2026-10-07-{1750,1814,1838,1903}.md`).

## Rezultat — idle-500

Mediana (razpon) treh ponovitev.

| veličina | A (S1) | B (S1 + S2) | sprememba |
|---|---:|---:|---:|
| MSPT povp | 4,94 (0,10) | **3,41** (0,15) | −31 % |
| MSPT p50 | 3,41 (0,13) | 3,21 (0,20) | −6 %, na meji šuma |
| MSPT p95 | 10,22 (0,26) | **5,24** (0,13) | −49 % |
| MSPT p99 | 11,27 (0) | **5,51** (0,13) | −51 % |
| MSPT max brez autosave | 14,17 (5,17) | **7,21** (0,93) | −49 % |
| MSPT max | 28,2 (11,3) | 29,4 (1,5) | v šumu (autosave tick) |
| µs/NPC | 9,74 (0,20) | **6,60** (0,30) | −32 % |
| ticki >10 ms | 370 (106) | **6** (0) | −98 % |
| ticki >25 ms | 5 (6) | 4 (2) | v šumu |
| alokacija MB/s | 21 (0,4) | 20,6 (0,4) | v šumu |
| GC ms/s | 0,11 | 0,11 | enako |
| `rwtarget.predzavrnjenih` | 108,6 M | **0** | poizvedba NPC-jev ne vrne več |
| `rwtarget.zozenih` | 0 | 252.327 (2.012) | ≈ 500 × 300 s × 20 / 3 / 4 = 250.000 |

A se ujema z B iz S1 (6. 10.: p95 9,96, µs/NPC 9,31, alokacija 21 MB/s) — stanje stroja je primerljivo.

## Rezultat — boj-500

| veličina | A (S1) | B (S1 + S2) |
|---|---:|---:|
| MSPT p50 | 8,91 (1,57) | 8,65 (1,44) |
| MSPT p95 | 9,96 (1,57) | 9,96 (1,31) |
| µs/NPC | 17,44 (2,89) | 17,19 (2,45) |
| alokacija MB/s | 89,4 (50,7) | 89,4 (15) |
| `rwtarget.zozenih` | 0 | **0** |

Bojni NPC-ji imajo `AttackOtherFactions`, zato S2 zanje ne velja (`zozenih = 0`) in razlike so v šumu —
S2 boja ne podraži. V obeh serijah je bila prva ponovitev hitrejša od ostalih dveh (p50 7,3–7,5 proti
8,9); vzrok ni znan, na A/B ne vpliva, ker se ponovi na obeh straneh.

## Sklep

S2 je **sprejet kot način 2** stikala `RwTargetPrefilter` (privzeto ostane 0, D-007): pri mirujočih
prijaznih NPC-jih prepolovi p95/p99 in odstrani skoraj vse tike nad 10 ms, v boju je nevtralen. Skupaj
z S1 je idle-500 od originala (baseline M2.6: p95 46 ms, µs/NPC 32) zdaj p95 5,2 ms in 6,6 µs/NPC.
Preostanek alokacije (~1 MB/tick) ni iz iskalnika tarč — naslednji kandidat zanj je profil (JFR) ali S4.
