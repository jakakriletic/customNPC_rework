# M5-S S1 — A/B predzavrnitve v iskalniku tarč (2026-10-06)

**Hipoteza** ([09](../09-PERFORMANCE-RAZISKAVA.md), zadnji odstavek pred zaključkom): pri mirujočih
stotinah NPC-jev prevladuje raytrace vidnosti v `NPCAttackSelector` za kandidate, ki jih NPC itak ne bi
napadel. Če hipoteza drži, mora na idle-500 alokacija (~17,5 MB/tick) pasti proti ravni boj/idle-50,
ticki nad 25 ms pa se morajo skoraj izničiti.

**Poseg:** `rework/ai/TargetPrefilter` pod stikalom `RwTargetPrefilter` (koda iz seje 66, commit
`070d4ce`). A = `-RwTarget 0` (original), B = `-RwTarget 1`.

## Postopek

- Stroj: DESKTOP-SN494C0, med serijami ni tekel noben drug Java proces (P9 zelen v vseh zagonih).
- `.\ponovitve-run.ps1 -Scenarij perf -AcceptEula -Dodatno @('-Variants',<v>,'-Counts','500','-RwTarget',<0|1>)`,
  3 ponovitve na celico, vsaka v svežem svetu, ogrevanje 120 s, merjenje 300 s, spawn naenkrat.
  `ponovitve-run` sprejme samo eno celico na serijo (`-JsonPath`), zato `-Counts` ni izpuščen.
- Vrstni red: idle-500 A (08:49), idle-500 B (09:13), boj-500 A (09:38), boj-500 B (10:02).
  Merila T1–T6 in P1–P9 zelena v vseh štirih serijah.
- Zbirna poročila s šumnim pasom vseh 25 veličin: [idle A](2026-10-06-M5-S1/idle500-A.md),
  [idle B](2026-10-06-M5-S1/idle500-B.md), [boj A](2026-10-06-M5-S1/boj500-A.md),
  [boj B](2026-10-06-M5-S1/boj500-B.md) (kopije `audit/m25c-perf-2026-10-06-{0849,0913,0938,1002}.md`;
  zapisi ponovitev `audit/m24-perf-2026-10-06-*-p{1,2,3}.json` lokalno, `audit/` ni v gitu).

## Rezultat — idle-500

Mediana (razpon min–max) treh ponovitev.

| veličina | A (original) | B (predzavrnitev) | sprememba |
|---|---:|---:|---:|
| alokacija MB/s | 342,4 (342,2–342,5) | **21** (21–21) | −94 % |
| alokacija KB/tick | 17.531 | **1.074** | −94 % |
| GC ms/s | 2,52 (2,28–2,69) | **0,10** (0,10–0,10) | −96 % |
| MSPT povp | 14,57 (14,51–15,26) | **4,71** (4,70–4,78) | −68 % |
| MSPT p50 | 3,41 (3,41–3,47) | 3,41 (3,34–3,47) | v šumu |
| MSPT p95 | 41,9 (41,9–44,0) | **9,96** (9,96–9,96) | −76 % |
| MSPT p99 | 45,1 (44,0–47,2) | **10,7** (10,7–11,0) | −76 % |
| MSPT max | 63,7 (47,5–79,4) | **34,0** (33,7–34,2) | −47 % |
| µs/NPC | 29,0 (28,9–30,4) | **9,31** (9,27–9,44) | −68 % |
| ticki >10 ms | 2.022 (2.016–2.024) | **237** (220–285) | −88 % |
| ticki >25 ms | 2.016 (2.016–2.016) | **6** (3–6) | −99,7 % |
| ticki >50 ms | 6 (0–10) | **0** (0–0) | |
| predzavrnjenih kandidatov v 300 s | 0 | 108,8 M (108,3–108,8 M) | |

Vse razlike razen p50 so večkratnik razpona — daleč nad šumnim pasom. A se ujema z baselinom M2.6
(5. 10.: p95 44,0, alokacija 344 MB/s, µs/NPC 29,9), zato koda pri `RwTargetPrefilter=0` ne
spremeni stroška.

**Model iz 09 je potrjen:** v A je bil počasen natanko vsak tretji tick (2.016 od 6.049 nad 25 ms,
p50 pa nespremenjen 3,4 ms) — to je iskalnik tarč, ki teče na tri tike. Predzavrnitev ta tick skoraj
izniči. Preostala alokacija 21 MB/s je pod linearno ekstrapolacijo idle-50 (4,1 MB/s × 10).

## Rezultat — boj-500 (kontrola, da predzavrnitev ne škodi)

V boju je večina kandidatov sovražnikov, zato predzavrnitev skoraj ne poseže (1.370 / 4.343 / 0
predzavrnjenih), dodatno preverjanje pa se izvede za vsakega kandidata.

| veličina | A | B |
|---|---:|---:|
| alokacija MB/s | 91,6 (88,7–92,6) | 91,1 (86,8–91,8) |
| MSPT povp | 9,15 (9,10–9,17) | 9,26 (8,74–9,34) |
| MSPT p95 | 10,5 (10,5–10,7) | 10,5 (9,96–10,5) |
| MSPT p99 | 12,3 (12,1–12,6) | 11,8 (11,5–11,8) |
| µs/NPC | 18,1 (18,0–18,1) | 18,3 (17,3–18,5) |
| ticki >25 ms | 6 (6–6) | 7 (6–7) |
| boj tekel (P5) | 500/500 | 500/500 |

Vse razlike so znotraj razpona: **nevtralno**.

## Ugotovitve ob meritvi

- **Napaka skripte (popravljena):** `perf-run.ps1` je števec iz odgovora `/rwtarget` bral z regexom
  `\([^)]*\)`, opis načina pa vsebuje gnezdene oklepaje, zato je bila vrednost −1. Zdaj
  `.*? predzavrnjenih=(\d+)`. Meritve ni prizadela; idle A p1/p2 imata v zapisu −1 namesto 0.
- **Boj-500 p50 danes 9,18 ms, v baselinu 5. 10. 7,34 (7,34–8,91):** razlika je v obeh načinih,
  alokacija (91 proti 90 MB/s) je enaka. Ni pojasnjeno — možni vzroki: stanje stroja, spremembe
  privzetih poti po baselinu (M3.6–M3.8 pri stikalih 0). Ne vpliva na ta A/B (A in B istega dne).
  Zapisano kot hipoteza, ne preiskano.

## Odločitev

Kandidat S1 je **obdržan** (preseže šum za velikostni red). Stikalo ostane privzeto 0 po D-007;
ali naj bo privzeto vklopljeno, je vprašanje za uporabnika (obnašanje je po testu enako, spremeni se
samo vsebina predpomnilnika `EntitySenses`).
