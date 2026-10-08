# M5-S P1 — profil JFR strežniške niti pri 500 NPC-jih (8. 10. 2026)

**Vprašanje** ([09](../09-PERFORMANCE-RAZISKAVA.md), »manjkajoča merilna koraka«): rwdiag pove, *koliko*
stane tick in alokacija, ne pa *kje*. Po S1 + S2 je idle-500 pri p95 5,2 ms in ~1 MB alokacij na tick,
boj-500 pa pri p50 ~9 ms. Kateri strežniški paket naj bo naslednji?

## Postopek

- `.\testworld.ps1`, nato `.\perf-run.ps1 -Variants idle,boj,skripte -Counts 500 -RwTarget 2 -Jfr`
  (zagon `2026-10-08-0925`, 120 s ogrevanja, 300 s merjenja na celico, ena ponovitev).
- `-Jfr` (novo): v merilnem oknu vsake celice `jcmd <pid> JFR.start settings=profile` in `JFR.stop`
  (JDK 8u492 Temurin, brez dodatnih JVM zastavic). Povzetek z `dev/tools/jfr-povzetek.js`;
  verige klicateljev z `--klicatelji <metoda>`. Posnetki so lokalni (`audit/jfr/`, ni v gitu); povzetki:
  [idle-500](2026-10-08-M5-S-P1/idle-500.md), [boj-500](2026-10-08-M5-S-P1/boj-500.md), [skripte-500](2026-10-08-M5-S-P1/skripte-500.md).
- `RwTarget 2` = trenutni privzetek (D-027). P1–P9 in J1–J3 zelena v vseh treh celicah.

**Profil ne pokvari meritve** (primerjava z nepreprofiliranimi zagoni S2, mediana in razpon treh
ponovitev): idle-500 p95 5,11 ms (S2 B: 5,24, razpon 0,13), µs/NPC 6,35 (6,60, razpon 0,30), alokacija
20,7 MB/s (20,6); boj-500 p50 9,44 ms (8,65, razpon 1,44). Ocena alokacij iz JFR TLAB vzorcev (20,6 MB/s
pri idle, 93,4 pri boju) se ujema z `jvm.alloc.server` (20,7 in 93,6) — utež TLAB je pravilna.

Vzorec CPU nastane samo, ko nit izvaja Javo, zato so deleži **deleži CPU časa strežniške niti**, ne
ticka. Vzorcev: idle 1.469, boj 4.360, skripte 1.501 (vzorčenje 10 ms).

## MSPT tega zagona (s profilom)

| celica | MSPT povp | p50 | p95 | p99 | µs/NPC | alok. MB/s |
|---|---:|---:|---:|---:|---:|---:|
| idle-500 | 3,25 | 3,08 | 5,11 | 6,42 | 6,35 | 20,7 |
| boj-500 | 9,39 | 9,44 | 11,27 | 13,37 | 18,62 | 93,6 |
| skripte-500 | 3,78 | 3,08 | 9,44 | 11,01 | 7,46 | 26,3 |

## Kje gre CPU (vključni delež vzorcev strežniške niti)

| veja | idle | boj | skripte | koda |
|---|---:|---:|---:|---|
| `PathNavigate.pathFollow` → `PathNavigateGround.isDirectPathBetweenPoints` | 0 | **48,3 %** (44,1 % iz vrstice 175) | 0 | vanilla, `EntityLiving.updateEntityActionState` → `onUpdateNavigation` |
| — od tega `World.getBlockState` → `ChunkProviderServer.getLoadedChunk` (hash) | — | **27,3 %** | — | iskanje chunka za vsak blok posebej |
| `PathNavigate.getPathToPos` (iskanje poti) | 0 | 2,0 % | 0 | vanilla |
| `EntityNPCInterface.travel` → `Entity.move` | 29,1 % | 17,7 % | 34,6 % | vanilla gibanje (tudi stoječi NPC vsak tick kliče `move` z gravitacijo) |
| — od tega `World.getCollisionBoxes` (bloki + entitete) | 19,7 % | 11,4 % | 25,6 % | vanilla |
| `EntityNPCInterface.onCollide` | **12,0 %** | 3,1 % | **15,1 %** | CNPC, AABB vsak 4. tick (`EntityNPCInterface:1219`) |
| `EntityDataManager.get` (bralni lock + `HashMap`) | 11,6 % | 4,9 % | 8,3 % | vanilla; kličejo `getHealth`, `isAttacking`, `isKilled`, `getFlag` |
| `ModelEyeData.update` → `Server.sendAssociatedData` | **6,5 %** | 2,4 % | **6,0 %** | CNPC, utrip oči: AABB 160 blokov za `EntityPlayerMP` |
| `EntityLivingBase.collideWithNearbyEntities` | 8,4 % | 2,6 % | 9,6 % | vanilla potisk |
| `EntityAIClosestTarget` (iskalnik tarč, S1 + S2) | 1,8 % | 0 | 2,5 % | CNPC |
| `ScriptContainer` | 0 | 0 | 1,8 % | CNPC |

`HashMap$TreeNode.root` je pri idle 9,2 % lastnega časa, kličejo ga `EntityDataManager.getEntry` →
`HashMap.get`. Drevesni koš pri `Integer` ključih 0…~40 ni mogoč, zato je to skoraj gotovo pripis
JIT-a sosednji kodi; šteje se kot del `EntityDataManager.get`.

## Kje gre alokacija (idle-500, 20,6 MB/s)

| razred / mesto | delež |
|---|---:|
| `AxisAlignedBB` (vsega) | **54,2 %** |
| — `AxisAlignedBB.offset` in `grow` v `Block.addCollisionBoxToList` / `getCollisionBoxes` (vanilla `Entity.move`) | ~50 % |
| `EntityAITasks.canUse` / `onUpdateTasks` (iteratorji, vanilla) | ~10 % |
| `ClassInheritanceMultiMap.iterator` (poizvedbe po chunkih) | 4,9 % |
| `DataTimers.update:91` (`new ArrayList` vsak tick, M5.4) | 3,8 % |
| `LivingUpdateEvent` (Forge) | 2,2 % |
| `NpcEvent.CollideEvent` (`EventHooks.onNPCCollide:115`) | 1,9 % |

**Odgovor na odprto vprašanje S2** (»preostala alokacija ~1 MB/tick ni iz iskalnika tarč«): ~55 % je
vanilla `Entity.move` (trki z bloki in entitetami), CNPC sam (`DataTimers`, `CollideEvent`, `onCollide`)
prispeva ~8 %.

## Ugotovitve

1. **Boj: ozko grlo ni iskanje poti, ampak sledenje poti.** `pathFollow` vsak tick preveri neposredno pot
   od najbolj oddaljene točke na isti višini nazaj (`isDirectPathBetweenPoints` → `isSafeToStandAt` →
   `getPathNodeType` za vsak blok širine entitete, s sosedi). Vsak `getBlockState` gre skozi
   `World.getChunkFromChunkCoords` → hash `ChunkProviderServer`. Iskanje poti (`getPathToPos`) je 2 %.
   Prioriteta M5.6 (deljenje poti) in S5 (negativni predpomnilnik poti) je zato za boj **nižja**, kot je
   ocenjeval [09](../09-PERFORMANCE-RAZISKAVA.md).
2. **Idle in skripte: največji posamezen strošek je vanilla gibanje** (29–35 %), ki ga ni mogoče
   preskočiti brez spremembe obnašanja. Med CNPC kodo sta največja `onCollide` (12–15 %, S4) in utrip oči
   (6 %, S7).
3. **Iskalnik tarč po S1 + S2 ni več pomemben** (≤ 2,5 %). S3 (ožji kvader poizvedbe) je zato
   brezpredmeten, S8 (razpršitev faze AI) pa nima več osnove: tikov nad 10 ms pri idle skoraj ni.
4. Skripte (prazna `tick` skripta) stanejo malo (1,8 %); razlika skripte proti idle v p95 (9,4 proti
   5,1 ms) ni v `ScriptContainer` — hipoteza: `onCollide` z dogodki in več trkov (nepreverjeno).

## Posledica za vrstni red M5-S

Nov kandidat **S14** in nov vrstni red sta zapisana v [09](../09-PERFORMANCE-RAZISKAVA.md):
**S14** (sledenje poti brez hash iskanja chunka za vsak blok, boj ~27–48 %) → **S4** (`onCollide` brez
opazovalca, idle/skripte ~12–15 %) → **S7** (prejemniki utripa oči po `playerEntities`, ~6 %). S3 in S8
se umakneta.
