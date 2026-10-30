# P3 — vogal in labirint pri `RwPathResume` / `RwFlyLineOfSight` (2026-10-30)

**Paket:** P3 (Q248, Q82, Q256) · **Scenarij:** [`../scenariji/M4.16-letenje.md`](../scenariji/M4.16-letenje.md)
(proge V in L) · **Izhodišča:** [M4.10](2026-10-26-M4.10-nadaljevanje.md), [M4.17](2026-10-26-M4.17-vidna-crta.md).

## Vzrok (Q82)

Na vogalu se vanilla pot vseh 6 NPC-jev konča ob steni na (−14, 4, −53), 7 blokov pred ciljem
(M4.17: `konecNav` 6/6, `cele0` 0/6, do cilja 6,96). Vanilla `PathFinder.findPath` ima mejo
**200 vozlišč** vpisano v zasebno metodo; 3D frontier ob kraku 20 × 16 jo porabi, preden pride okoli
konca. Nadaljevanje (M4.10) z istega mesta najde isto točko in odneha (napredek < 1 blok).
Simulacija vanilla A* (`FlyingNodeProcessor`, 6 sosedov, domet 32) na geometriji proge V:
z začetka je potrebnih 279–877 vozlišč (x = −20 ne pride v dometu 32 niti brez meje), z mesta ob
steni **~253**. Unit test `RwPathFinderTest` to ponovi na mreži brez sveta.

## Popravek (Q256, pod stikalom `RwPathResume`, privzeto 0)

- `net.minecraft.pathfinding.RwPathFinder`: vanilla `PathFinder` vrstico za vrstico z nastavljivo
  mejo vozlišč; pri 200 enak vanilli (unit test na vogalu in prostem).
- `RwPathNavigateFlying.continuePath`: kadar obnova z vanilla iskanjem ne napreduje
  (`PathResume.stalls`), **enkrat** išče z mejo `WIDE_NODES = 1024` (isti ChunkCache in domet);
  če ta napreduje vsaj 1 blok, nadomesti vanilla izid. Sicer se nadaljevanje ustavi kot prej.
- Pri `RwPathResume=0` se `continuePath` ne izvede (nadaljevanje ni aktivno), torej original.
- Števci v `/rwpath`: `nadSirokih`, `nadSirokihNapredek`, `nadSirokihUs` (cena kot podatek, Q92).
- Labirint je že rešen s kombinacijo M4.10 + M4.17 (6/6 v fazi A); P3 ga preveri na novi kodi.
- Kopenski navigator (`RwPathNavigateGround`) ni spremenjen (omejitev).

## Testi (controller, `a7b11f8d…`, poskus 1)

| Test ID | Namen | Rezultat |
|---|---|---|
| `unit`, `build`, `package` | +4 `RwPathFinderTest`, +1 `PathResumeTest` | PASS |
| `p3-letenje-orig-v1` | stikala 0 = M4.16 (ena ponovitev, Q250) | PASS, LT1–LT9; **180/180 = `audit/m25c-letenje-2026-10-26-1623.md`** — `audit/m416-letenje-2026-10-30-211745.md` |
| `p3-letenje-nad-v1` | `RwPathResume=1`: vogal | PASS, LT1–LT9 — `audit/m416-letenje-2026-10-30-212041.md` |
| `p3-letenje-vidnad-v1` | `RwPathResume=1` + `RwFlyLineOfSight=1`: vogal in labirint | PASS, LT1–LT9 — `audit/m416-letenje-2026-10-30-212337.md` |

Odtis `config` je na vseh treh zagonih `BDCB0345…` (26. 10.: `711BDDC3…`); razlika so ključi
`Rw…`, dodani po 26. 10. (M6/M7/P1, vsi privzeto 0), LT9 (config pred = po) je zelen.

**Izid (faza A, prispeli / do cilja konec):**

| proga | original | M4.10 nad (26. 10.) | P3 nad | M4.17 v4 vid+nad | P3 vid+nad |
|---|---|---|---|---|---|
| V (vogal) | 0/6, 6,96 | 0/6, 6,96 | 0/6, 8,83 | 0/6, 7,04 | **6/6, 0,82** (čas med 170) |
| L (labirint) | 0/6, 10,60 | 0/6, 10,60 | 0/6, 10,60 | 6/6, 0,97 | **6/6, 0,97** |

- vid+nad: od 180 veličin M4.17 poskusa 4 (`audit/m25c-letenje-2026-10-28-1358.md`) se razlikuje
  samo proga V (A/B/C: 0/6 → 6/6, do cilja 7,04/7,27/6,99 → 0,82/0,86/0,87); Z 5/6 (Q84), L, DF,
  DW, P enaki. Merilo vogal A 6/6 in labirint A 6/6 izpolnjeno, ostale proge niso slabše.
- Samo nad (brez vidne črte): od 180 veličin M4.10 (`…-1508.md`) se razlikuje samo V: široko iskanje
  najde pot okoli vogala, vanilla sledenje poti pa preseka vogal in obtiči (do cilja 6,96 → 8,83,
  0/6). Vogal zato zahteva **obe** stikali (omejitev).

**Cena (podatek, Q92, ena ponovitev):** `nadSirokih` nad 42 (napredek 42), skupaj 31.630 µs =
**753 µs/iskanje**; vid+nad 29 (napredek 29), 23.702 µs = **817 µs/iskanje**. Široko iskanje teče
samo ob obnovi brez napredka, enkrat na obnovo; pri 0 nikoli.

**Omejitve:** kopenski navigator ni spremenjen; mejo 1024 izbere simulacija ene geometrije; brez
vidne črte vogal ostane 0/6; cena v boju/patrulji (`leteci-200`) ni izmerjena (privzeto 0).
**Sprejeto pod stikali, privzeto 0 (Q92), poskus 1.**
