# Server-side (TPS/MSPT) performance hotspots in CustomNPCs 1.12.2 (01Oct19) and gaps in the M5 plan

Conventions used in this note:
- `REF` = `dev/reference-src/noppes/npcs/...` (decompiled original, line numbers as cited in project docs).
- `PATCH` = `dev/src/patch/java/noppes/npcs/...` (compiled rework classes; line numbers differ from REF by roughly +20 in `EntityNPCInterface`).
- `MC` = vanilla 1.12.2 / Forge 14.23.5.2847 sources, read from `C:\Users\jakak\.gradle\caches\minecraft\net\minecraftforge\forge\1.12.2-14.23.5.2847\snapshot\20171003\forgeSrc-1.12.2-14.23.5.2847-sources.jar` (path given as `MC net/minecraft/...`).
- Measurements: `audit/m26-baseline-2026-09-27-1230/p1/idle-{50,200,500}.json` (the only protocol-length data that exists: 120 s warm-up, 300 s measurement, nogui, 1 repetition; the boj/skripte cells crashed, see `docs/meritve/2026-10-05-M2.6b-zascita-zagona.md`), `audit/m24-perf-*-2026-09-23-*.json` and `docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md` (short runs, 09-23 11:51 runs were with the server GUI calling `System.gc()` every 500 ms, so not comparable, per `docs/04-STANJE.md:621-630`).
- No gradle/java/Minecraft process was run for this note (a baseline measurement was running). All findings come from reading code and existing measurement files.

## 1. Per-tick path of one NPC on the server: frequency, cost class, scaling

### Takeaway
Per tick, a default idle NPC is cheap except for one item: target acquisition (`EntityAIClosestTarget` + `NPCAttackSelector`). It runs on 1 of every 3 ticks with a 1/4 chance, scans a 65x33x65 box and does a line-of-sight raytrace for every living entity within `aggroRange` (16) **before** the faction check. In a friendly crowd this costs O(N^2) raytraces, and nothing else on the idle path comes close. All other recurring work is either O(1) per NPC or throttled (every 4, 10 or 20 ticks).

### Cited Findings
Call chain (server). `EntityCustomNpc.onUpdate` → `EntityNPCInterface.onUpdate` → `EntityLiving/EntityLivingBase.onUpdate` → `EntityNPCInterface.onLivingUpdate` → `EntityLivingBase.onLivingUpdate` → `EntityLiving.updateEntityActionState`. That last call runs `despawnEntity`, `senses.clearSensingCache()`, `targetTasks.onUpdateTasks()`, `tasks.onUpdateTasks()`, `navigator.onUpdateNavigation()`, then the move/look/jump helpers — [MC net/minecraft/entity/EntityLiving.java:830-860](forgeSrc sources jar).

Every tick, per NPC:
- `EntityCustomNpc.onUpdate` → `modelData.eyes.update(this)`: a `nextInt(140)` roll. On a hit it sends `EYE_BLINK` through `Server.sendAssociatedData`, which runs a 160-block player AABB query (see Q4) — [REF entity/EntityCustomNpc.java:60-79](dev/reference-src/noppes/npcs/entity/EntityCustomNpc.java); [REF ModelEyeData.java:61-77](dev/reference-src/noppes/npcs/ModelEyeData.java). Eyes are on by default (`EnableDefaultEyes = true`, [REF CustomNpcs.java:135](dev/reference-src/noppes/npcs/CustomNpcs.java)).
- `timers.update()` allocates `new ArrayList<>(timers.values())` every tick, even when there are no timers — [PATCH entity/data/DataTimers.java:90-94](dev/src/patch/java/noppes/npcs/entity/data/DataTimers.java); [REF EntityNPCInterface.java:365]. Already in plan M5.4.
- `onLivingUpdate` (server branch): `dataManager.set(Walking, !noPath)`, `set(Interacting, …)` (packets only on change), `combatHandler.update()` (O(1)), `onCollide()` (returns early except every 4th tick), `bossInfo.setPercent` if bossbar > 0 — [PATCH entity/EntityNPCInterface.java:432-524](dev/src/patch/java/noppes/npcs/entity/EntityNPCInterface.java).
- Rework hooks `RwNavBackend.beforeTick` and `MountGuard.beforeRiderTick` return immediately in the default/ORIGINAL mode — [PATCH rework/nav/RwNavBackend.java:40-45](dev/src/patch/java/noppes/npcs/rework/nav/RwNavBackend.java); [PATCH rework/entity/MountGuard.java:59-63](dev/src/patch/java/noppes/npcs/rework/entity/MountGuard.java).
- Vanilla per-tick costs that every NPC pays: `move()` collision (`getCollisionBoxes` includes an entity AABB query), `collideWithNearbyEntities` (an AABB query on its own box), and `despawnEntity` → `getClosestPlayerToEntity` (O(players); CNPC never sets `persistenceRequired`, the grep found no hit). Inference from vanilla structure; not instrumented.

Every 3 ticks (`EntityAITasks.tickRate = 3`): `shouldExecute()` of every non-running task, for both `targetTasks` and `tasks`. On the other ticks only `shouldContinueExecuting()` of running tasks runs, and `updateTask()` of running tasks runs every tick — [MC net/minecraft/entity/ai/EntityAITasks.java:21,64-118]. The phase is `tickCount++ % 3` per entity, counted from construction. So NPCs constructed in the same tick (a `clone grid` spawn, or every NPC loaded at server start) evaluate AI in the same tick.

Default task set created in `updateTasks()` ([PATCH EntityNPCInterface.java:773-947]; [REF :756]):
- targetTasks: `EntityAIClearTarget` (cheap; `combatHandler.checkTarget` every 10 ticks), vanilla `EntityAIHurtByTarget`, **`EntityAIClosestTarget(npc, EntityLivingBase.class, 4, directLOS, false, NPCAttackSelector)`**, `EntityAIOwnerHurtByTarget`, `EntityAIOwnerHurtTarget` (cheap).
- tasks: `EntityAIWaterNav`, optional door/shelter tasks, `EntityAIReturn`, `EntityAIFollow`, `EntityAIWatchClosest` (5 blocks, chance 0.002), `EntityAILook`, `EntityAIWorldLines` (1/1800), `EntityAIJob`, `EntityAIRole`, `EntityAIAnimation`, optional transform, response tasks (`EntityAIAttackTarget`, optional ranged/tactical), and a movement task (`EntityAIWander` / `EntityAIMovingPath`).

`EntityAIClosestTarget.shouldExecute` ([PATCH ai/target/EntityAIClosestTarget.java:47-58](dev/src/patch/java/noppes/npcs/ai/target/EntityAIClosestTarget.java), identical in REF):
- `rng.nextInt(4) != 0` → return, so on average one scan per 12 ticks per idle NPC.
- `getEntitiesWithinAABB(EntityLivingBase, bb.grow(d0, ceil(d0/2), d0), selector)` with `d0 = FOLLOW_RANGE = NpcNavRange = 32` (minimum 16) — [PATCH CustomNpcs.java:134-135,203-204](dev/src/patch/java/noppes/npcs/CustomNpcs.java); [PATCH EntityNPCInterface.java:354,1086]; [MC EntityAITarget.java:115-119]. That gives a 65x~34x65-block box, about 5x5 chunks, followed by `Collections.sort`.
- The predicate `NPCAttackSelector.isEntityApplicable` ([PATCH ai/selector/NPCAttackSelector.java:34-81](dev/src/patch/java/noppes/npcs/ai/selector/NPCAttackSelector.java)) checks, in order: alive, not self, `isInRange(aggroRange)` (an axis-aligned cube, default aggroRange 16, [REF entity/data/DataStats.java:28]), health. **Then at line 38, `if (directLOS && !getEntitySenses().canSee(entity)) return false;`** (`directLOS` defaults to `true`, [REF entity/data/DataAI.java:42]). Only after that come the return-home distance check, guard job, companion guard, player aggression (`faction.isAggressiveToPlayer`, line 69) and NPC aggression (`attackOtherFactions && faction.isAggressiveToNpc`, line 76).
- `EntitySenses.canSee` caches only within one tick (`clearSensingCache` every tick) in two `ArrayList`s, so `contains` is linear. A cache miss calls `canEntityBeSeen` → `world.rayTraceBlocks(eye, eye)` — [MC net/minecraft/entity/ai/EntitySenses.java:12-42]; [MC EntityLivingBase.java:2772-2775]. `rayTraceBlocks` allocates `new Vec3d` and `new BlockPos` and calls `getBlockState` on every voxel step, up to 200 steps — [MC net/minecraft/world/World.java:~1040-1167].
- Result: for an idle NPC whose faction attacks nobody, every living entity within 16 blocks is raytraced and then rejected by the faction check.

Every 10 ticks: `calculateStartYPos` plus `EventHooks.onNPCTick` ([REF EntityNPCInterface.java:358]). `onNPCTick` allocates a `NpcEvent.UpdateEvent`, calls `script.runScript(TICK)` (returns immediately if scripts are disabled) and always calls `WrapperNpcAPI.EVENT_BUS.post(event)` — [REF EventHooks.java:142-149](dev/reference-src/noppes/npcs/EventHooks.java).

Every 4 ticks: `onCollide()` does `getEntitiesWithinAABB(EntityLivingBase, bb.grow(1,0.5,1))`. For each touching entity it allocates a `CollideEvent` (which calls `getIEntity`), calls `runScript(COLLIDE)` and posts to `WrapperNpcAPI.EVENT_BUS`, whether or not any script or listener exists — [REF EntityNPCInterface.java:1152-1165]; [REF EventHooks.java:133-140]; [REF api/event/NpcEvent.java:47-55].

Every 20 ticks: `faction = getFaction()` (HashMap lookup), `advanced.scenes.update()`, regen, a mob scan only for factions with `getsAttacked` (default `false`, [REF controllers/data/Faction.java:35]), the linked-NPC check, flushing `updateClient` (a full spawn NBT, see Q4), and `updateTasks()` only if `updateAI` — [PATCH EntityNPCInterface.java:443-474].

`calculateStartYPos` is effectively O(1). The loop exits on the first iteration because `Block.getBoundingBox` never returns null (it returns `FULL_BLOCK_AABB` by default and `BlockAir` overrides only the collision box), so the cost is one `getBlockState` and one AABB allocation every 10 ticks — [PATCH EntityNPCInterface.java:1401-1416]; [MC net/minecraft/block/Block.java:464-467]; [MC net/minecraft/block/BlockAir.java:31-34].

Other idle-path tasks are cheap:
- `EntityAILook.updateTask` runs every tick while idle. It calls `getClosestPlayerToEntity(16)` every tick only for `standingType == 2` — [PATCH ai/EntityAILook.java].
- `EntityAIAnimation` sets the animation plus `updateHitbox` plus two `setPosition` calls, only while the animation is changing — [PATCH ai/EntityAIAnimation.java].
- `EntityAIWatchClosest` triggers with chance 0.002 per evaluation — [PATCH ai/EntityAIWatchClosest.java].
- `EntityAIWaterNav` checks only `isInWater`.

Jobs and roles are throttled by their own counters (10/20/30 evaluations). Because `EntityAIJob`/`EntityAIRole` `shouldExecute` runs only every 3 ticks, these counters stretch to 30/60 ticks: JobFollower `ticks = 10`, JobItemGiver 10, RoleTransporter 10, JobChunkLoader 20 (player box 48), RolePostman `ticksExisted % 20`, JobHealer by `speed`, JobSpawner `nextInt(30)` → 81^3 box — [REF roles/JobFollower.java:51], [REF roles/JobChunkLoader.java:53], [REF roles/JobHealer.java:67], [REF roles/JobSpawner.java:286-298,391-398]. JobGuard's `isEntityApplicable` builds a string (`"entity."+getEntityString+".name"`) and does a linear `ArrayList.contains` per candidate mob — [REF roles/JobGuard.java:42-47].

### Inferences
- The only per-tick component that is both frequent and O(N) per NPC (so O(N^2) per server tick) on the default idle path is the `ClosestTarget`/`NPCAttackSelector` scan. Everything else is O(1) or O(local density).
- With `tickRate = 3` and same-tick construction, the scans of all NPCs land on one tick in three. This would produce the bimodal MSPT seen in measurements (Q7).
- In combat, `EntityAIClosestTarget` is in the "using" state and its `shouldExecute` is not re-run, so the scan stops. This explains why µs/NPC in `boj` is lower than idle at 500 NPCs, which the project currently lists as "nepojasnjeno" ([docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md](docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md) finding 4).

### Gaps
- There is no per-phase breakdown of an NPC tick. `npc.update.window` measures the gap between successive `LivingUpdateEvent`s, not individual phases ([PATCH rework/diag/DiagEventCollector.java:195-217]). No CPU call-stack profile exists, although the protocol asks for one (`PLAN_IMPLEMENTACIJE.md` §6).

## 2. World queries per NPC (box sizes, frequency) and O(N^2) interactions

### Takeaway
There are five recurring per-NPC entity queries plus several event-driven ones. The O(N^2) one is target acquisition: a 65x34x65 box every ~12 ticks, with raytraces for all entities within 16 blocks before the faction filter. A second, event-driven O(N^2) burst happens when combat starts (defend-faction scan with raytraces). Vanilla collision queries add O(local density^2) in piles.

### Cited Findings
| Query | Box | Frequency (per NPC) | Per-candidate work | Source |
|---|---|---|---|---|
| Target acquisition | `grow(32, 16, 32)` = 65x~34x65 blocks, class `EntityLivingBase` | every 3rd tick × 1/4 = 1/12 ticks, only while the NPC has no target | `isInRange(16)` then **raytrace** (directLOS) then faction | [PATCH ai/target/EntityAIClosestTarget.java:48-53]; [PATCH ai/selector/NPCAttackSelector.java:34-38] |
| `onCollide` | `grow(1, 0.5, 1)`, `EntityLivingBase` | every 4 ticks | per touching entity: event alloc + script dispatch + Forge bus post | [REF EntityNPCInterface.java:1152-1165] |
| Mob aggro (getsAttacked) | `grow(16,16,16)`, `EntityMob` | every 20 ticks, only if faction `getsAttacked` (default false) and not attacking | `canSee` per mob without a target | [REF EntityNPCInterface.java:438-443] |
| Defend faction on hit | `grow(32,16,32)`, `EntityNPCInterface` | per damage taken while not attacking | up to 3 `canSee` raytraces per same-faction defender NPC | [PATCH EntityNPCInterface.java:666-672]; [REF :638] |
| Attack line / `saySurrounding` | `grow(20,20,20)`, `EntityPlayer` | per new target if an attack line exists | `ServerChatEvent` post + fake player copy | [PATCH EntityNPCInterface.java:713-715,1021-1036] |
| `Server.sendAssociatedData` (eye blink, `updateClient`) | `grow(160,160,160)`, `EntityPlayerMP` | blink: 1/140 per tick (≈ every 7 s); `updateClient`: ≤ 1 per 20 ticks if dirty | AABB over up to ~21x21 chunks × 16 sections | [REF Server.java:93-115]; [REF ModelEyeData.java:68-72] |
| `Server.sendRangedData` (sounds) | `grow(16)` | living sound / hurt / step sounds if a custom sound is set | — | [REF entity/data/DataAdvanced.java:221-228]; [REF Server.java:119-140] |
| Wander NPC-interact | `grow(walkingRange, ≤7, walkingRange)` (default 10) | after passing the `movingPause` gate (1/80), then 1/6 | selector | [PATCH ai/EntityAIWander.java:50-55,83-93] |
| Avoid target (onAttack=2 / tactical 3) | `grow(distance, 3, distance)` | each evaluation | `canSee` | [PATCH ai/EntityAIAvoidTarget.java:55-66] |
| Watch closest | `grow(5,3,5)`, `findNearestEntityWithinAABB` | chance 0.002 per evaluation | — | [PATCH ai/EntityAIWatchClosest.java:35-48] |
| JobSpawner / JobChunkLoader / JobHealer / JobFollower / ItemGiver / Postman / Transporter / Conversation | 40 / 48 / range / range / 3-10 / 10-20 / 6 / range | throttled 10-30 evaluations | — | [REF roles/*.java] (lines in Q1) |
| Vanilla move collision + `collideWithNearbyEntities` | own bb (+0.25) | every tick | push/collide per overlapping entity | vanilla `EntityLivingBase`/`World.getCollisionBoxes` |

- `World.getEntitiesWithinAABB` widens the chunk range by `World.MAX_ENTITY_RADIUS` (default 2.0) — [MC net/minecraft/world/World.java:74]. CNPC **raises this global static** whenever an NPC's half-width exceeds it and never lowers it — [REF EntityNPCInterface.java:1092]; [REF entity/EntityCustomNpc.java:110]. One large NPC (big `Size`, or a model entity such as a dragon) therefore widens every entity AABB query in the server.
- The M2.4 idle scenario places NPCs one block apart in a 25-wide grid (500 = 25×20) with `AggroRange 16`, `DirectLOS 1b` and faction 0 — [docs/scenariji/M2.4-obremenitve.md](docs/scenariji/M2.4-obremenitve.md); [dev/testworld/customnpcs/clones/1/PERF_Idle.json](dev/testworld/customnpcs/clones/1/PERF_Idle.json). Each NPC therefore has roughly 50 / 180 / 425 other NPCs inside its 16-block cube at N = 50 / 200 / 500.

### Inferences
- Estimated raytraces at 500 idle NPCs: 500 NPCs × (1/4) scans per AI cycle ≈ 125 scans in one tick out of three, × ~425 candidates ≈ 53k raytraces per heavy tick. Each ray steps about 10-15 voxels and allocates 2 objects per step (≈ 1 KB per ray): about 53 MB per 3 ticks, or about 18 MB per tick. **Measured: 17.5 MB/tick** (`alok.KBnaTick` 17546.5 in idle-500, 27.9). The same model gives ≈ 3 MB/tick at 200 (measured 2.48) and ≈ 0.2 MB/tick at 50 (measured 0.213). The fit is close enough that this scan very likely accounts for nearly all idle allocation and the superlinear time.
- `EntitySenses`' linear `contains` over ~425 cached entries adds ~90k pointer comparisons per scan (~11M per heavy tick at 500). This is secondary but not negligible.
- In real towns (many NPCs of one friendly faction near each other, `directLOS` on by default), this is the dominant cost, not a scenario artifact.
- When combat starts (an explosion, or arrows hitting many non-attacking NPCs at once), the defend-faction scan does O(N) raytraces per hit, which bursts to O(N^2).

### Gaps
- Per-raytrace cost on this machine was not measured. Use the sampling profiler or a µs counter around `NPCAttackSelector` to confirm. The `World.MAX_ENTITY_RADIUS` value reached in real worlds is unknown.

## 3. Pathfinding call sites, frequency and ChunkCache cost

### Takeaway
Each path search builds a `ChunkCache` of radius `FOLLOW_RANGE + 8` = 40 blocks (81^3) and runs up to 200 A* node expansions. It costs ~70 µs warm and ~240 µs cold (p50), with a 3.4 ms maximum measured. Repath cadences are throttled except for two patterns not addressed in the plan:
1. `EntityAIAttackTarget.shouldExecute` computes a full path every 3 ticks while the target is unreachable.
2. Vanilla `PathWorldListener` iterates all NPC navigators on every block change and can recompute paths synchronously inside `setBlockState`.

### Cited Findings
- `PathNavigate.getPathToPos`/`getPathToEntityLiving` returns the cached `currentPath` only if it is unfinished and the target block is unchanged. Otherwise it builds `new ChunkCache(world, pos ± (range+8))` and calls `pathFinder.findPath` — [MC net/minecraft/pathfinding/PathNavigate.java:111-165]. `PathFinder` stops after 200 iterations — [MC net/minecraft/pathfinding/PathFinder.java:63-65]. Range is `FOLLOW_RANGE` = `NpcNavRange` = 32.
- Measured path search cost (M2.7): p50/p95 cold 237.6/1540.1 µs, warm 67.6/135.2 µs; max 3381.6 µs — [docs/meritve/2026-09-17-M2.7-navigacija-baseline.md:44-49,76](docs/meritve/2026-09-17-M2.7-navigacija-baseline.md). Searches are bursty: "p95 = 0 na tick, max 16". The slowest ticks (232 ms) coincided with 16 NPCs getting paths simultaneously — [docs/04-STANJE.md:1254-1260](docs/04-STANJE.md).
- Call sites and cadence (PATCH `ai/`):
  - `EntityAIAttackTarget`: `shouldExecute` calls `getPathToEntityLiving` on **every evaluation (3 ticks) while the NPC has a target and the task is not running**. If the path is null (unreachable target), this repeats indefinitely. `updateTask` repaths to the target every `4 + nextInt(7)` ticks (avg 7) — [PATCH ai/EntityAIAttackTarget.java:47-56,85-91].
  - `EntityAIRangedAttack` `tryMoveToEntityLiving` under `moveTries` — [PATCH ai/EntityAIRangedAttack.java:64-69].
  - `EntityAIFollow` repaths every 10 ticks, with `tpTo` fallback (several `getBlockState` calls) — [PATCH ai/EntityAIFollow.java].
  - `EntityAIReturn`: `startExecuting` paths, then on `noPath` repaths every 10 ticks (`stuckTicks = 10`) up to 5 times, then teleports; `findRandomTargetBlockTowards` if > NpcNavRange. For `MovingType 0` it triggers whenever the NPC is more than ±0.2 blocks from its start (`isVeryNearAssignedPlace`) — [PATCH ai/EntityAIReturn.java:30-150]; [PATCH EntityNPCInterface.java:1385-1392].
  - `EntityAIWander`: `movingPause` (default true, [REF entity/data/DataAI.java:64]) gates to 1/80 per evaluation, so roughly one new wander path per ~240 ticks. With `movingPause` false, a new path on every evaluation once idle. `RandomPositionGenerator` tries 10 candidates, each calling `getBlockPathWeight` → `getLightBrightness` + `getBlockState` ([PATCH EntityNPCInterface.java:953-962]). Stops entirely when `idleTime ≥ 100` (no player within 32 blocks resets it in vanilla `despawnEntity`) — [PATCH ai/EntityAIWander.java:50,105-125].
  - `EntityAIMovingPath`: gated by `nextInt(40)` when `movingPause`, otherwise every evaluation on `noPath` — [PATCH ai/EntityAIMovingPath.java:32,65].
  - Tactical tasks (ZigZag, Orbit, Stalk, Ambush, Dodge, Panic, Avoid) path on their own timers and do extra raytraces/opaque scans (Ambush/Stalk shelter search loops) — [PATCH ai/EntityAIZigZagTarget.java:66-84]; [PATCH ai/EntityAIAmbushTarget.java:95-115]; [PATCH ai/EntityAIStalkTarget.java:130-152].
  - Shade/indoor tasks: 10 random `canSeeSky` + `getBlockPathWeight` probes — [PATCH ai/EntityAIFindShade.java:60-66]; [PATCH ai/EntityAIMoveIndoors.java:61-66].
  - Jobs: JobFarmer and JobBuilder use `tryMoveToXYZ` — [REF roles/JobFarmer.java:151,204]; [REF roles/JobBuilder.java:119,140].
- `PathWorldListener.notifyBlockUpdate` loops over **all** registered navigators on every block change that alters a collision box. For each with an unfinished path near the change it calls `updatePath()`, which recomputes the path synchronously if more than 20 ticks have passed since the last update (otherwise it flags `tryUpdatePath`) — [MC net/minecraft/pathfinding/PathWorldListener.java:21-49]; [MC PathNavigate.java:78-96,229-236]. NPC navigators are registered there on every `updateTasks()` — [PATCH EntityNPCInterface.java:789-801].

### Inferences
- An NPC holding a target it cannot reach (behind a wall, across water, a flying player) recomputes a full A* (up to 3.4 ms cold) every 3 ticks for as long as it keeps the target. Hundreds of NPCs aggroed on an unreachable player would saturate the tick. M5.2 (budget) would cap this, but neither M5.2 nor M5.6 mentions negative-result caching.
- Block churn (redstone, farms, NPC door opening, JobBuilder/JobFarmer) costs O(N_navigators) per changed block. Synchronous re-paths inside `setBlockState` cascade when many NPCs path through the same area. M5.6 mentions invalidation on block change but not the listener's O(N) loop.
- Crowd pushing can repeatedly trigger `EntityAIReturn` for standing NPCs (±0.2 tolerance), causing up to 5 paths per 50 ticks plus a teleport.

### Gaps
- There is no count of path searches per cause (attack/return/follow/listener). `nav.ai.paths.per.tick` exists but is not attributed by task. The idle scenario showed 0 path searches ([docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md] finding 1).

## 4. Network/sync cost from the server per NPC

### Takeaway
Steady-state per-NPC traffic is mostly vanilla tracker traffic (updateFrequency 3, range 64, velocity on). CNPC adds three things:
- a periodic eye-blink packet (~every 7 s per NPC), each preceded by a 160-block player AABB query on the main thread;
- full spawn-NBT re-sends on `updateClient` (GZIP, ≤ 1 per 20 ticks, or immediately via the scripting API);
- a GZIP-compressed spawn NBT per player per NPC on tracking start, on the main thread.

### Cited Findings
- Every NPC class is registered with `tracker(64, 3, true)` — [REF CustomEntities.java:64-76](dev/reference-src/noppes/npcs/CustomEntities.java). The vanilla tracker sends relative move/look every 3 ticks if changed, head look when yaw changes by ≥ 1/256, metadata when dirty, and velocity on `velocityChanged` — [MC net/minecraft/entity/EntityTrackerEntry.java:188-322].
- DataManager parameters registered per NPC: RoleData, JobData, FactionData, Animation, Walking, Interacting, IsDead, Attacking — [PATCH EntityNPCInterface.java:360-370]. `Walking`/`Interacting` are `set` every tick but are dirty only on change, as is `Animation` via `setCurrentAnimation` ([PATCH :1699-1702]).
- Eye blink: `nextInt(140) == 1` per tick per alive server NPC → `Server.sendAssociatedData(npc, EYE_BLINK, id)` — [REF ModelEyeData.java:61-77]. `sendAssociatedData` does `getEntitiesWithinAABB(EntityPlayerMP, grow(160))` on the calling (main) thread, then schedules serialization and sends on the single-thread `CustomNPCsScheduler` executor, copying the buffer for each player — [REF Server.java:93-115]; [REF util/CustomNPCsScheduler.java](dev/reference-src/noppes/npcs/util/CustomNPCsScheduler.java).
- `updateClient()` sends `writeSpawnData()` (display, MaxHealth, armor/weapons maps, AI display fields, role/job, Bard/Puppet/Companion NBT, full `ModelData` NBT) as `UPDATE_NPC` via `sendAssociatedData` — [PATCH EntityNPCInterface.java:526-531,1529-1565]. The flag is flushed every 20 ticks ([PATCH :467-469]). `DataDisplay` setters are mostly guarded against no-op changes. `DataInventory.setArmor/setRightHand/setLeftHand` and `DataStats.setHideDeadBody` are not guarded, and `NPCWrapper.updateClient()` (script API) sends immediately — [REF entity/data/DataDisplay.java:161-451]; [REF entity/data/DataInventory.java:96-131]; [REF api/wrapper/NPCWrapper.java:266-268].
- `fillBuffer` writes NBT through `Server.writeNBT`, which GZIPs into a fresh `ByteArrayOutputStream` each time — [REF Server.java:265-273].
- Tracking start: `EntityTrackerEntry.createSpawnPacket` → `FMLNetworkHandler.getEntitySpawningPacket` → `FMLMessage.EntitySpawnMessage.toBytes` → `IEntityAdditionalSpawnData.writeSpawnData(tmpBuf)` — [MC EntityTrackerEntry.java:399,516-523]; [MC net/minecraftforge/fml/common/network/internal/FMLMessage.java:213]. CNPC's `writeSpawnData(ByteBuf)` builds the NBT and GZIPs it — [PATCH EntityNPCInterface.java:1520-1527].
- Speech, sounds and chat bubbles go through `Server.sendData`/`sendRangedData`, with `saySurrounding` doing a 20-block player query plus a `ServerChatEvent` — [PATCH EntityNPCInterface.java:1021-1049]; [REF entity/data/DataAdvanced.java:221-228].
- Player-side sync per player tick: `PlayerData.updateClient` → `SYNC_END` NBT — [REF ServerTickHandler.java:21-31].

### Inferences
- Eye blinks at 500 NPCs: ~3.6 AABB queries of ~441 chunk columns (only loaded columns get section scans) per tick, plus ~71 small packets/s for every player within 160 blocks. Restricting recipients to `EntityTracker` watchers, or looping over `world.playerEntities` with the same box test, would give the same visible result without the chunk scan.
- A player login or teleport into an area with N NPCs causes N `writeSpawnData` + GZIP calls (a new `Deflater` each) on the main thread in one tick: a latency spike proportional to N × NBT size.
- Scripts that set inventory every tick produce one full-NBT `UPDATE_NPC` per second per NPC. Scripts calling `npc.updateClient()` produce one per call.

### Gaps
- Spawn-NBT size per NPC (bytes before and after GZIP) and the share of bandwidth by packet type are not measured. Render/client cost is out of scope for this note (M2.4r is open).
- I did not verify on which thread Forge's embedded channel runs `toBytes` beyond the call chain cited. The inference that it runs on the main thread follows from `generatePacketFrom` being called inside `createSpawnPacket`.

## 5. Saving: per-NPC NBT write cost and autosave

### Takeaway
On every autosave (every 900 ticks), vanilla writes every loaded chunk that contains entities, so every NPC is serialized in one tick on the main thread. NPC NBT contains everything, including the inline script text and up to 40 console entries per script tab. No per-NPC save cost has been measured.

### Cited Findings
- `MinecraftServer.tick`: `tickCounter % 900 == 0` → `saveAllWorlds(true)` — [MC net/minecraft/server/MinecraftServer.java:762-766]. `ChunkProviderServer.saveChunks(all)` saves every chunk where `needsSaving(all)`. With `all = true`, any chunk with entities counts as needing a save if `worldTime != lastSaveTime` — [MC net/minecraft/world/chunk/Chunk.java:1019-1033]; [MC net/minecraft/world/gen/ChunkProviderServer.java:226-251].
- `EntityNPCInterface.writeEntityToNBT` writes display, stats, ai, script, timers, advanced, role, job, inventory, transform, and more. `EntityCustomNpc` adds `NpcModelData` — [PATCH EntityNPCInterface.java:1090-1115]; [REF entity/EntityCustomNpc.java:45-49]. `ScriptContainer.writeToNBT` stores `Script` (full inline text), `Console` (TreeMap ≤ 40 entries) and `ScriptList` — [REF controllers/ScriptContainer.java:93-98,180-190].
- Existing data: the autosave tick shows 58/74/87/124 ms max at 8-27 NPCs ([docs/04-STANJE.md:1424-1428]). In the 27.9 idle baseline, `mspt.brezSave.max == mspt.max` (150.1 ms at 500), so the worst tick was not a save tick.

### Inferences
- Save cost scales with N × (NBT size). Cloned NPCs with inline scripts duplicate the script text in every NPC's NBT and in every save. Script includes (`ScriptList`) store only names.
- Changing what is saved would violate the "NBT types must not change" rule only if keys or types change. Skipping unchanged work (for example caching an immutable `ModelData` NBT) is possible but needs exact dirty tracking.

### Gaps
- There is no measurement of per-NPC NBT bytes or of autosave ms versus N at 200/500 NPCs. The 27.9 baseline had no autosave inside the extracted max; `p99 brez save` equals p99.

## 6. Script engine (Nashorn): per-NPC cost, global lock, event frequency, timers, forge-event bridge

### Takeaway
Steady-state script cost per NPC is low at 50/200. Each scripted NPC owns a separate Nashorn engine (its own Context and Global), which compiles its script separately and costs heap. The static lock is uncontended on the server thread, so M5.1 is mainly a correctness change, not a throughput one.

The serious script-related hotspot not in the plan is the **Forge-event bridge**. When any Forge script is enabled, every Forge event, including `LivingUpdateEvent` for every entity every tick, is wrapped with string building and posted to `addScheduledTask`.

### Cited Findings
- `DataScript.runScript` returns immediately when scripts are disabled. Otherwise it may fire `onNPCInit` after a reload and loops over the containers — [REF entity/data/DataScript.java:56-73].
- `ScriptContainer.run` returns early on `errored`, `!hasCode()`, `unknownFunctions.contains(type)` or scripting disabled. Missing handlers are cached after the first `NoSuchMethodException`. On each call it then does:
  - `synchronized(lock)` on a static interned `"lock"`;
  - `new StringWriter` + `new PrintWriter`;
  - two `setWriter` calls;
  - `engine.getFactory().getLanguageName().equals("lua")` on every call;
  - `invokeFunction(type, event)`;
  - `sw.getBuffer().toString().trim()` → `appandConsole`.
  — [REF controllers/ScriptContainer.java:51-53,123-175]. `hasCode()` calls `getFullCode()`. When `!init` (always for an empty container), that re-concatenates and allocates a new `HashSet` for `unknownFunctions` on every call — [REF :106-120,200-202].
- Engine per container: `setEngine` → `ScriptController.getEngineByName` → `factory.getScriptEngine()`, a new engine per container, i.e. per NPC tab — [REF controllers/ScriptContainer.java:203-218]; [REF controllers/ScriptController.java:259-265]. Nashorn args come from the system property `nashorn.args` (`-strict`) — [REF controllers/ScriptController.java:66]; [PATCH CustomNpcs.java:130-131].
- Separate engines prevent Nashorn from reusing cached compiled scripts; the recommended pattern is one engine with separate bindings — [cometd issue #737](https://github.com/cometd/cometd/issues/737); [nashorn-dev mailing list, "Reusing compiled JS code"](https://mail.openjdk.org/pipermail/nashorn-dev/2014-October/003760.html).
- Event frequency per NPC:
  - `tick` every 10 ticks, all in phase for NPCs constructed in the same tick (`ticksExisted` is not saved, so all NPCs loaded at startup share the phase, [docs/04-STANJE.md:641-643]);
  - `collide` per touching entity every 4 ticks;
  - `timer` per DataTimers firing;
  - target / targetLost / damaged / meleeAttack / rangedLaunched / died / interact on events — [REF EventHooks.java:88-160].
  Each event allocates an `NpcEvent` and posts on `WrapperNpcAPI.EVENT_BUS` even when scripts are off.
- **Forge-event bridge.** `ScriptPlayerEventHandler.registerForgeEvents()` reflectively registers one handler for every concrete public event class in `net.minecraftforge.event.*` and `net.minecraftforge.fml.common.*`, excluding a few such as terraingen, GenericEvent, EntityConstructing, PotentialSpawns, render/client ticks and GetCollisionBoxes. This includes `LivingEvent.LivingUpdateEvent` — [REF ScriptPlayerEventHandler.java:377-405,421-453]; registered at [REF CustomNpcs.java:242]. For each event with Forge scripts enabled:
  - `EventHooks.onForgeEntityEvent` → `getIEntity` + `new ForgeEvent.EntityEvent`;
  - `onForgeEvent` builds the function name with `getClass().getName()`, `lastIndexOf`, `substring`, `replace` and `uncapitalize` (several String allocations per event);
  - `ForgeScriptData.runScript` → `CustomNpcs.Server.addScheduledTask(lambda)`, i.e. one `ListenableFutureTask` queued per event, running the scripts on a later tick;
  - a post to `WrapperNpcAPI.EVENT_BUS`.
  — [REF EventHooks.java:478-516]; [REF controllers/data/ForgeScriptData.java:55-74]. When Forge scripts are disabled, the handler returns at its first check, but the ASM listener is still invoked for every event.
- Measured: skripte 50/200 ≈ idle (µs/NPC 27.8 vs 27.4 and 45.5 vs 44.6). At 500, p95 is 159 ms vs 99 ms and the max is 518 ms (09-23 GUI run, not comparable to baseline) — [docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md]. No protocol baseline for skripte exists (27.9 crashed before boj/skripte).

### Inferences
- `synchronized(lock)` on the single server thread is uncontended (all NPC scripts run on the server thread; Forge scripts are re-queued to it). The cost per acquisition is therefore a lock fast path, and **removing the lock (M5.1) is unlikely to change MSPT measurably**. R5's claim that it is "najverjetneje največje posamezno ozko grlo" is not supported by the code or by skripte-50/200 ≈ idle. Its value is correctness (`Current`/`CurrentType` static) and preparation for M6.
- The 500-script tail more likely comes from the 1-in-10-tick phase burst (500 `invokeFunction` calls in the same tick), per-engine heap/GC pressure (500 Globals), and first-run compilation per engine. All of these are NOT addressed by M5.1/M5.4.
- With Forge scripts enabled, ≥ N_entities scheduled futures and several string allocations happen per tick even if the script defines no matching function. That is O(entities) overhead unrelated to NPC logic, and it breaks event cancellation semantics (it runs async on a later tick).
- Sharing compiled code across NPCs (one engine with per-NPC Bindings/Global) would remove the compile and memory cost, but it is close to the forbidden "shared script engine" (quests/timers isolation risk) and needs its own project.

### Gaps
- Per-call µs of `invokeFunction` with a trivial event, heap per Nashorn engine, and compile time per engine are not measured. GC/allocation numbers for skripte-500 under nogui do not exist yet (the current 3.5 h baseline should provide them).

## 7. What existing measurement data says is expensive

### Takeaway
Idle cost grows quadratically with N in both time and allocation. At 500 idle NPCs, exactly one tick in three is heavy (> 25 ms). Both facts match the target-acquisition scan model (Q1/Q2). Pathfinding and autosave are not the idle drivers.

### Cited Findings
- 27.9 baseline, idle (nogui, 120 + 300 s, 1 repetition):

  | N | p50 | p95 | p99 | µs/NPC | alloc KB/tick | MB/s | GC ms/s | ticks > 25 ms / total |
  |---|---|---|---|---|---|---|---|---|
  | 50 | 0.451 | 0.885 | 1.212 | 9.08 | 213.2 | 4.2 | 0.01 | 0 / 6027 |
  | 200 | 1.507 | 7.078 | 8.913 | 14.65 | 2479.9 | 48.4 | 0.33 | 1 / 6024 |
  | 500 | 3.867 | 46.137 | 53.477 | 31.98 | 17546.5 | 342.7 | 5.59 | **2008 / 6024** (> 10 ms: 2019) |

  — [audit/m26-baseline-2026-09-27-1230/p1/idle-50.json, idle-200.json, idle-500.json](audit/m26-baseline-2026-09-27-1230/p1/).
- Allocation per NPC per tick: 4.3 → 12.4 → 35.1 KB (from the table). Total allocation scales ≈ N^1.8 (50→200) and ≈ N^2.1 (200→500).
- 09-23 short runs (GUI, `System.gc()` every 500 ms): idle µs/NPC 27 → 45 → 58; boj 84 → 70 → 51; skripte 28 → 46 → 77; "slow ticks evenly distributed across `tick % 10` phases" (D-017) — [docs/meritve/2026-09-23-M2.4-obremenitve-preverba.md]; [docs/04-STANJE.md:612-630].
- The nogui spawn-spread test (09-23 14:27 vs 14:32, saturated at TPS 13.7-14.5): ticks > 50 ms were 275/827 (33 %) spread vs 291/891 (33 %) simultaneous; ticks > 10 ms were 807/827 vs 650/891 — [audit/m24-perf-idle-500-2026-09-23-1427.json, -1432.json]. The "spread" mode sends commands 350 ms wall-clock apart (≈ 7 ticks at 20 TPS, fewer when lagging) — [perf-run.ps1:60-63,415-423](perf-run.ps1).
- Boj-50 (10-05, 30 s): 17.67 µs/NPC, 459 KB/tick, versus idle-50 at 7.5 µs and 214 KB — [audit/m24-perf-boj-50-2026-10-05-1318.json].
- Path search ~70 µs warm / ~240 µs cold; bursts of 16 searches per tick produce 232 ms ticks — [docs/meritve/2026-09-17-M2.7-navigacija-baseline.md]; [docs/04-STANJE.md:1254-1260].

### Inferences
- "Exactly 1/3 of ticks heavy" is the signature of `EntityAITasks.tickRate = 3` with phase-aligned NPCs. The D-017 observation (even distribution over `tick % 10`) is consistent with this, since gcd(3, 10) = 1, and does not refute AI-phase alignment.
- The spread-spawn test does not cleanly test the mod-3 phase: wall-clock spacing under lag does not guarantee a uniform distribution of `tickCount % 3`, and those runs were saturated. A direct check is to attribute slow ticks by `serverTick % 3`, or to log `EntityAITasks` phase per NPC.
- The scan model reproduces allocation within ~0-25 % at all three N (Q2). Rough time: 53k raytraces at ~0.8 µs ≈ 45 ms, matching p95 46 ms at 500.

### Gaps
- There is only one repetition. The boj/skripte protocol cells are missing (crash 27.9), and the 3.5 h baseline running now should fill them. No per-phase profile, no `tick % 3` attribution, no measurement with players connected (network/tracker cost), no autosave cell at 200/500.

## 8. Candidates NOT in the M5 plan (ranked by expected gain/risk) and compatibility notes

### Takeaway
The largest missing item is reordering or short-circuiting the target-acquisition predicate (LOS raytrace after the cheap faction/hostility checks). It is behaviour-identical, small to implement, and should remove most idle O(N^2) cost and allocation. Next come query-box tightening, observer-less event skipping (which replaces M5.3's "per chunk" idea), negative path caching, the Forge-script bridge, eye-blink recipients, and AI phase spreading.

M5.1's expected throughput gain should be downgraded. `calculateStartYPos` can be dropped as a candidate.

### Cited Findings (code facts each candidate rests on — see Q1-Q6 for line refs)
- `NPCAttackSelector` does the raytrace at line 38, before the faction checks at lines 58-79 ([PATCH ai/selector/NPCAttackSelector.java]). The query box uses `FOLLOW_RANGE` (32) while the predicate accepts only entities within `aggroRange` (16) ([PATCH ai/target/EntityAIClosestTarget.java:51-52]).
- `onCollide` and `onNPCTick` allocate and post events unconditionally ([REF EventHooks.java:133-149]).
- `EntityAIAttackTarget.shouldExecute` computes a path on every evaluation ([PATCH ai/EntityAIAttackTarget.java:47-56]).
- Forge bridge per-event string ops plus `addScheduledTask` ([REF EventHooks.java:500-516]; [REF controllers/data/ForgeScriptData.java:55-70]).
- `sendAssociatedData` 160-block AABB per eye blink ([REF Server.java:93-97]; [REF ModelEyeData.java:68-72]).
- `EntityAITasks` `tickRate = 3` phase ([MC EntityAITasks.java:21,68]).
- `World.MAX_ENTITY_RADIUS` is raised globally by NPC hitboxes ([REF EntityNPCInterface.java:1092]; [REF entity/EntityCustomNpc.java:110]).

### Inferences — ranked candidate list (gain / risk / rule conflicts)
1. **C1: Target-selector predicate order (NOT in plan).**
   - Change: evaluate the cheap hostility tests first: player aggression, `attackOtherFactions && isAggressiveToNpc`, guard/companion-guard lists, return-home distance. Do `canSee` last.
   - Result: identical, because all conditions are ANDed with LOS, and the guard checks and `isAggressiveToPlayer` are side-effect-free apart from `PlayerData.get`. `EntitySenses` cache contents change, but they only affect performance.
   - Expected gain: very high in friendly crowds; this is most of the idle O(N^2) cost and ~17 MB/tick at 500.
   - Risk: low. Keep `rng.nextInt(4)` consumption unchanged.
   - Switch: needed by project rule (default = original order).
2. **C2: "No possible target" short-circuit (NOT in plan).**
   - If the NPC can never match (faction attacks no NPC faction or `attackOtherFactions` false, no guard targets, no companion guard, and no `EntityPlayerMP` inside the box), skip the `getEntitiesWithinAABB` entirely after rolling the RNG.
   - Gain: high. Risk: low-medium; player aggression depends on faction points, so check players explicitly.
3. **C3: Query box tightened to `min(FOLLOW_RANGE, aggroRange + MAX_ENTITY_RADIUS)` on x/z and the intersection on y (NOT in plan).**
   - The result set is identical (the predicate already restricts to the aggro cube), and the sort order is preserved because of stable sort and chunk iteration order.
   - Gain: medium (4× smaller area at the default 16 vs 32). Risk: low; edge cases are entity bb vs position.
4. **C4: Observer-less event skipping (partially overlaps M5.3, but a different method).**
   - If `!script.isEnabled()` (or no container defines `collide`/`tick`) and `WrapperNpcAPI.EVENT_BUS` has no listeners for that event class, skip the `onCollide` AABB query and the `UpdateEvent`/`CollideEvent` allocations.
   - Gain: medium (removes one AABB query per 4 ticks per NPC). Risk: low-medium; listener detection needs Forge internals or reflection.
   - Recommend this instead of M5.3's "enkrat na chunk" dedupe, which is complex and changes ordering.
5. **C5: Negative path cache in `EntityAIAttackTarget.shouldExecute` (NOT explicit in M5.2/M5.6).**
   - Remember a null path for (own block, target block) for K ticks.
   - Gain: high in the unreachable-target case. Risk: medium (behaviour change when the world changes) → switch.
   - Fits under the M5.2 budget as a sub-item.
6. **C6: Forge-script bridge (NOT in plan; part of M6 territory).**
   - Cache the function name per event class (`ClassValue`); behaviour-identical, low risk.
   - Do not schedule when every container already has the function in `unknownFunctions` and init is current; behaviour-identical, low risk.
   - Optionally drop `LivingUpdateEvent` forwarding behind a switch; this is a behaviour change.
   - Gain: high only on servers with Forge scripts enabled; O(entities).
7. **C7: Eye blink / `sendAssociatedData` recipients (NOT in plan).**
   - Replace the 160-block chunk AABB with a loop over `world.playerEntities` using the same box test (identical recipients), or with tracker watchers (equivalent on the client). Skip entirely when no players are present.
   - Gain: medium at hundreds of NPCs, with players. Risk: low. Packet IDs are unchanged.
8. **C8: AI phase spreading (NOT in plan; D-017 rejected spawn spreading on evidence that does not test mod 3).**
   - Initialise `EntityAITasks.tickCount` to `entityId % 3` (needs an AT/reflection on vanilla), and optionally offset the `onNPCTick` phase by `entityId % 10`.
   - Gain: flattens p95/p99 (1/3 heavy ticks); does not reduce the average. Not "lowering tick frequency".
   - Risk: medium. It changes which tick each NPC evaluates and the script tick phase, which can matter to scripts expecting synchrony → switch.
   - Verify the mod-3 hypothesis first (attribute slow ticks by `tick % 3`).
9. **C9: `ScriptContainer` micro-allocations beyond M5.4.** Cache the `isLua` boolean per engine (instead of `getFactory().getLanguageName().equals` each call), avoid re-creating `unknownFunctions` in `getFullCode` for empty containers, and size the `StringWriter` lazily. Low gain, low risk.
10. **C10: Defend-faction scan on damage (NOT in plan).** Order the cheap checks (`defendFaction`, same faction id, `isKilled`) are already first; the raytraces follow. Bound with a per-tick dedupe when many hits land in one tick. Low-medium gain (combat start bursts), medium risk.
11. **C11: Spawn-NBT GZIP caching per NPC with a dirty revision (NOT in plan for NPCs; the plan only covers player login sync).** Gain: login/teleport spikes. Risk: medium-high (many mutation paths). Packet format unchanged.
12. **C12: `World.MAX_ENTITY_RADIUS` inflation.** Document and measure. Capping it would break correct detection of large NPCs (behaviour change). Low priority.
13. **C13: Vanilla crowd collision O(k^2) (`collideWithNearbyEntities`, move collision) in piles.** Out of CNPC scope except via `canBePushed`/hitbox options (behaviour change). List only.

Plan-level corrections:
- **M5.1:** keep it for correctness, but do not expect an MSPT gain (the lock is uncontended on the server thread).
- **PLAN §5 "izračun startY":** drop it; the loop exits on its first iteration.
- **M5.3 faction check:** it only applies to factions with `getsAttacked = true` (default false), so its default-case gain is ~0.
- **M5.6:** add the `PathWorldListener` O(N) loop and synchronous `updatePath` to its invalidation design.

Rule conflicts:
- C8 touches timing (allowed behind a switch; it is not a frequency reduction).
- Engine sharing for compile reuse conflicts with the "shared script engine" ban.
- Sleeping/LOD for distant NPCs and multithreaded AI remain forbidden.
- None of C1-C7 change packet IDs, enum order or NBT types.

### Gaps
- None of the candidate gains are measured. C1/C2/C3 should be A/B'd on idle-500 (expect `alok.KBnaTick` to fall from ~17.5 MB toward the boj/idle-50 level, and the ">25 ms ticks" count to collapse). The 3.5 h baseline running now will supply the missing boj/skripte baselines needed for C4-C6.
