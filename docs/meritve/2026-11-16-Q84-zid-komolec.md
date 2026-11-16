# Q84 — zid 5/6 pri vidni črti + nadaljevanju: komolec (2026-11-16)

**Paket:** omejitev §6 po Q407/Q454, Q455–Q457 · **Scenarij:** [`../scenariji/M4.16-letenje.md`](../scenariji/M4.16-letenje.md)
(`letenje-run.ps1`, 6 prog × 3 faze) · **Izhodišči:** [M4.17](2026-10-26-M4.17-vidna-crta.md),
[P3](2026-10-30-P3-vogal-labirint.md). Stikala nespremenjena: `RwFlyLineOfSight` in `RwPathResume` privzeto 0.

## Vzrok [M]

Sled `q455-zid-sled-v1` (`audit/m416-letenje-2026-11-16-122656-b.log`, odtis `a48129…`; test je padel samo
na LT9, ker je nova gradnja configu dopisala ključe — podatki sledi so veljavni): NPC i=4 proge Z po obnovi
poti miruje pred robom zida z gibanjem točno 0 in `nav=1`; ob ticku ~300 vanilla `checkForStuck` pot
izbriše in M4.10 nima česa nadaljevati. Gibanje 0 je `WAIT` originalnega `FlyingMoveHelper.isNotColliding`:
polni hitbox na ravni črti do točke poti na robu zida zadane rob. Hipoteza M4.17 (zmanjšan hitbox) ovržena.

## Popravek (samo pri `RwFlyLineOfSight=1`)

`RwPathNavigateFlying.steer`: če je helper prejšnji tick dal `WAIT` in je ravna črta do trenutne točke
zaprta (`FlightFollow.moveClear` = logika `isNotColliding`), NPC cilja **komolec** — najprej navpično pri
višjem cilju, sicer najprej vodoravno, drugi vrstni red je rezerva; 0,6 bloka prestrela; največ 60 tickov
oziroma dokler ravna črta ni prosta. Brez prostega komolca ostane original. Števec `vidKomolcev` v `/rwpath`.
Pri 0 se koda ne izvede. Unit +4 (`FlightFollowTest`).

## Zagoni (controller, odtis `e72ac98c…`, vsi PASS, LT1–LT9)

| Test | Poročilo | Izid |
|---|---|---|
| `unit`, `build`, `package` | — | PASS |
| `q455-letenje-orig-v1` (stikala 0) | `audit/m416-letenje-2026-11-16-123732` | **180/180 enakih** P3 orig `…-10-30-211745` (razlikujeta se le čas zagona in SHA configa zaradi novih ključev) |
| `q455-letenje-vid-v1` | `…-124032` | vse proge enake M4.17 vid (A: Z 0/6, V 0/6, L do cilja 2,95; B: Z 6/6, L 6/6); `vidKomolcev` 3 |
| `q455-letenje-vidnad-v1` | `…-124336` | **Z 6/6 v A, B in C** (prej 5/6, 5/6 v A/C); `vidKomolcev` 5 |

Vid + nad, proga Z proti P3 `212337`:

| Faza | P3 (prej) | zdaj |
|---|---|---|
| A | 5/6, 190/600, do cilja 2,70 | **6/6**, 190/240, 1,21 |
| B | 6/6, 220/260 | 6/6, 220/240 |
| C | 5/6, 195/600, do cilja 2,70 | **6/6**, 195/250, 1,21 |

Vse ostale vrstice (V, L, DF, DW, P v A/B/C) so enake P3 vid+nad do zadnje decimalke; V, L in DF ostanejo 6/6.

## Meje

En zagon na serijo (vse prejšnje serije te proge imajo razpon 0). Cena v boju (`leteci-boj`) ni ponovno
merjena: komolec teče samo po `WAIT` z zaprto ravno črto, v boju na prostem M4.17 izmeril 0 zaprtih preverb —
podatek iz kode in M4.17, ne iz nove meritve. DW ostaja 0/6 (Q78, kopenski cilj).
