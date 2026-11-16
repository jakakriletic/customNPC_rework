# Q458 — `ArmyApi.members` brez umirajočih članov (2026-11-16)

**Paket:** omejitev §6 po Q407/Q454 točka 1 · **Scenarij:** `army-edge-run.ps1` (M11.9, `dev/testworld/army-edge-control.js`)
· **Stikala:** nespremenjena, `RwArmies` privzeto 0.

## Vzrok [K]

`ArmyCommandService.members` je člane filtriral z `!e.isDead`. Vanilla `EntityLivingBase.onDeathUpdate` postavi
`isDead` šele pri `deathTime == 20`, zato je član z zdravjem 0 še do 20 tickov veljal za člana; NPC, ki čaka na
respawn (`isKilled()`), pa ves čas čakanja. Stanje stare kode izhaja iz branja kode, ne iz ločenega zagona.

## Popravek

Filter je zdaj `e.isEntityAlive()` (vanilla: `!isDead && health > 0`; `EntityNPCInterface` doda `!isKilled()`).
Isti seznam uporabljajo ukazi, pohodi (M11.6 tiketi), `/rwarmy list` (`loaded=`) in `ArmyApi.members`, zato
umirajoč ali na respawn čakajoč član ne dobi ukaza, ne drži tiketa in se ne šteje kot naložen. Ko oživi, je spet član.
Pri `RwArmies=0` metoda vrne prazen seznam kot prej.

## Zagoni (controller, odtis `88b5c53b…`, vsi PASS)

| Test | Poročilo | Izid |
|---|---|---|
| `unit`, `build`, `package` | — | PASS |
| `q458-edges-v1` | `audit/m119-army-2026-11-16-125102` | vse provere M11.9 PASS; nova `loss-members-exclude-dying`: takoj po smrtni škodi (še naloženi) `listed=0 members=6` |

## Meje

- En zagon, dev dedicated brez igralca; čakanje na respawn je pokrito samo s kodo (`isKilled`), ne z zagonom.
- Stara koda ni bila zagnana z novo provero (Q90: brez dodatnega teka); napaka izhaja iz vanilla `onDeathUpdate`.
