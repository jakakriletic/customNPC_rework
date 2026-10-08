# M5-S S14 — sledenje poti prek predpomnilnika chunkov (8. 10. 2026)

**Hipoteza** (profil JFR, [P1](2026-10-08-M5-S-P1-profil-jfr.md)): v boju-500 je 48 % CPU strežniške
niti v vanilla `pathFollow` → `isDirectPathBetweenPoints`, od tega 27 % v
`ChunkProviderServer.getLoadedChunk` (hash iskanje chunka za vsak prebrani blok). Če bloke beremo prek
predpomnilnika chunkov za čas enega klica, mora MSPT boja pasti za velik del teh 27 %.

**Poseg:** `RwPathNavigateGround` (navigator vsakega kopenskega NPC-ja, `EntityNPCInterface`),
`MemoBlockAccess`, `ChunkMemo` (4 reže po parnosti chunka), stikalo `RwPathFollowCache` (privzeto 0 =
`super`, torej original), ukaz `/rwpath [0|1|2|reset]`, `perf-run.ps1 -RwPath N`.
Test `ChunkMemoTest` (4), build zelen (253 testov), `verify-package` PASS (64, novi razredi samo dodani).

## Enakost v svetu (način 2 = oboje, vrne original)

`.\perf-run.ps1 -Variants boj -Counts 500 -RwTarget 2 -RwPath 2 -Seconds 120 -WarmupSeconds 60`:
**616.276 primerjav, 0 neujemanj** (R1, R2 zelena). 683,9 M branj blokov, 1,12 M iskanj chunka —
611× manj iskanj kot v originalu; **~1.110 branj blokov na en klic**.

## A/B boj-500 (`ponovitve-run`, `-RwTarget 2`, A = `-RwPath 0`, B = `-RwPath 1`)

| veličina | A mediana (razpon), 3 ponovitve | B pon. 1 | B pon. 2 | B pon. 3 |
|---|---:|---:|---:|---:|
| MSPT povp | 8,96 (0,30) | 8,84 | 7,85 | *neveljavna* |
| MSPT p50 | 8,91 (0,52) | 8,91 | 7,73 | |
| MSPT p95 | 10,22 (0,52) | 10,22 | 8,91 | |
| µs/NPC | 17,75 (0,58) | 17,50 | 15,54 | |
| alokacija MB/s | 90,9 (1,7) | 90,6 | 36,7 | |

B ponovitev 3 je P9 zavrnil: med celico sta tekla uporabnikov gradle build in Minecraft (`ladja_mod`).

## Izid: **nedokazano**

Ena veljavna ponovitev B je enaka A, druga je ~12 % hitrejša; to je pod pragom, ki bi ga dve ponovitvi
lahko dokazali. `RwPathFollowCache` ostane privzeto 0. Profil je strošek iskanja chunka očitno
precenil (podobno kot pripis `HashMap$TreeNode.root`, glej P1): JFR v JDK 8 vzorec v vgrajeni (inlined)
kodi pripiše najbližjemu okvirju, zato je delež posamezne vanilla metode le približek.

Ugotovitev, ki ostane: strošek je **število branj**, ne iskanje chunka — ~1.110 branj blokov na klic, ker
`isSafeToStandAt` na vsakem koraku po črti znova oceni okno `sizeX × sizeZ` (z `checkNeighborBlocks`
3 × 3), okna sosednjih korakov pa se večinoma prekrivajo. Kandidat **S14b**: tip vozlišča
(`getPathNodeType` za (x, y, z) in velikost) si zapomniti za čas enega klica. Odprto za M5.0 (D-029).

Nepojasnjeno: B ponovitev 2 ima alokacijo 36,7 MB/s namesto ~90 (tudi pri A stabilno 90,9). Hipoteza:
drugačen potek boja v tej ponovitvi; vpliva na MSPT, zato ponovitev 2 sama ni dokaz.
