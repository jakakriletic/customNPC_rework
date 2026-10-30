# CustomNPCs Rework 1.12.2 — user guide (v1.0 candidate)

This is the one user-facing document for server owners and modpack makers (M10.6). It covers
installation, the new settings, migration and rollback, and known limitations. Every number and
claim links to the measurement that supports it; anything not measured is said to be unproven.

## 1. What it is

A patched build of **CustomNPCs 1.12.2 (01Oct19)**. It keeps the original file formats, GUIs and
script API and adds, behind settings:

- data-loss fixes in NBT/JSON saving (always on, no setting);
- server and client performance work (on by default only where the result is proven identical or
  the owner accepted the change);
- optional NPC features: steering of NPC mounts, attack priority, hitbox modes, Hover flight,
  path continuation, Baritone navigation, formations (`/rwsquad`), far NPC rendering;
- NPC scripts in **Java** (besides JS), and an animation API/hooks for the optional mod
  `customnpcentities` 0.7.0.

## 2. Installation

- Minecraft 1.12.2, Forge 14.23.5.2847 (the tested version), Java 8.
- Replace `CustomNPCs_1.12.2-(01Oct19).jar` in `mods/` with the rework jar. Do not keep both.
- Put the **same jar on the server and on every client.** Mixing an original client with a rework
  server (or the reverse) was not validated (see §6).
- **Back up the whole world folder and `config/` before the first start** (see §5).
- On first start, missing `Rw…` keys are added to `config/CustomNpcs.cfg` with the defaults below.
  Keys already present keep their value.

## 3. Global settings (`config/CustomNpcs.cfg`)

The config file holds a full English description of every key. Short form, 0 is always the
original behaviour:

### On by default — same result, faster

| Key | Default | Side | What it does |
|---|---|---|---|
| `RwTargetPrefilter` | 2 | server | target search rejects certainly non-hostile candidates before the line-of-sight check |
| `RwPathFollowCache` | 1 | server | path following reuses block evaluations |
| `RwPathSearchMemo` | 1 | server | path search reuses block evaluations |
| `RwMoveBoxes` | 1 | server | skips an empty entity collision query |
| `RwDataShadow` | 1 | server | NPC data-manager reads from plain fields |
| `RwCollideSkip` | 1 | server | skips collide query without a script or listener |
| `RwBlinkRecipients` | 1 | server | eye-blink packet recipients from the player list |
| `RwLabelCache` | 1 | client | NPC name/title from a display list |

Except for `RwTargetPrefilter`, value 2 on these keys is a verify mode for tests only (double cost).

### On by default — changes behaviour or look

| Key | Default | Side | Change |
|---|---|---|---|
| `RwPathNegCache` | 1 | server | an NPC with no path to its target waits 20 ticks before searching again (reacts up to 20 ticks later) |
| `RwHeadwearSkip` | 1 | client | 3D headwear skips fully transparent quads; single edge pixels may differ |
| `RwRenderBox` | 1 | client | culling box covers the whole model; NPCs without a hitbox are visible as far as NPCs with one |
| `RwFarView` | 2 | both | NPCs beyond the tracker range are sent as light placeholders and drawn as impostors (no limb animation, eyes or name) |
| `RwFarRange` | 384 | server | max placeholder distance, also limited by (view distance − 1) × 16 |
| `RwImpostorStart` | 56 | client | tracked NPCs farther than this are drawn as impostors (`RwFarView=2`) |
| `RwImpostorAtlas` | 2048 | client | impostor atlas size (≈21 MB VRAM) |
| `RwFarShaders` | 0 | client | with an OptiFine shader pack, impostors are off and NPCs are drawn as original |

### Off by default — opt-in features

| Key | Default | What 1 does |
|---|---|---|
| `RwMountSteering` | 0 | NPC mount with NPC rider steers itself (2 = always) |
| `RwAttackPriority` | 0 | melee attack starts before finishing a wander/move path |
| `RwHitbox` | 0 | honours per-NPC `RwHitboxMode`; also `RwHitboxShieldWeight` (200), `RwHitboxShieldItems` |
| `RwPathResume` | 0 | walking/flying NPCs continue partial paths to goals beyond the navigation range |
| `RwFlyLineOfSight` | 0 | flying NPCs follow paths around corners instead of flying at walls |
| `RwNavBackend` | 0 | permits NPCs with per-NPC `RwNavBackend=1` to use Baritone (needs the `npcbaritone` library) |
| `RwAnimHooks` | 0 | `animationStart/animationEvent/animationEnd` script hooks (needs `customnpcentities`; read at startup) |
| `RwLodStart` | 0 | client: distant NPCs without eyes, labels and 3D headwear beyond this many blocks |
| `RwJavaClasspath` | empty | enables Java NPC scripts (see §4) |

### Per-NPC settings (saved in the NPC)

| NBT key | Where to set | Values |
|---|---|---|
| `RwHitboxMode` | NPC GUI (hitbox) | 0 vanilla, 1 solid, 2 smart; needs `RwHitbox=1` |
| `RwNavBackend` | AI → Movement → Backend | 0 vanilla, 1 Baritone; needs the global permission |
| `RwFlightMode` | NBT only (no GUI in v1) | 0 original, 1 Hover (holds position, stops without sliding) |

### Commands (op)

`/rwdiag` (measurements, off by default), `/rwmount`, `/rwattack`, `/rwtarget`, `/rwpath`,
`/rwnav`, `/rwhitbox`, `/rwsquad` (formations), `/rwfar`, `/rwimpostor`, and diagnostics
`/rwcollide`, `/rwdata`, `/rwmove`, `/rwblink`, `/rwcore`. Runtime changes made by a command
are not written to the config.

## 4. Java NPC scripts

- **Trusted script authors only.** Java runs with the full permissions of the server process;
  there is no sandbox. On servers with untrusted players set `OpsOnly=true` and trust every op,
  or restrict scripting and all other edit/import paths with your permission system. The original
  open default is not a security boundary.
- Needs a JDK 8 (`javac`) and `RwJavaClasspath` set by hand to a file or classpath containing the
  CustomNPCs API and the game classes of this exact runtime. Empty = Java tabs stop with a clear
  error; JS is unaffected.
- Only the NPC script GUI offers Java (player, forge, block, door and item scripts keep the original languages).
- API: [API.md](api/API.md), hooks and examples: [HOOKS.md](api/HOOKS.md), IDE template:
  [scripts/java/template](../scripts/java/template/README.md). v1 covers NPC, player and world;
  items, blocks, quests, dialogs and factions in Java are planned for v2.
- Animations: `Animators.of(npc)` (`play`, `queue`, `stop`, `getClip`, `getTick`, `getClips`) works
  only with `customnpcentities` 0.7.0; without it `isAvailable()` is false and calls do nothing.

## 5. Migration and rollback

- **No migration is needed.** The rework reads original worlds as they are and writes the same
  formats plus a few extra NBT keys (`Rw…`).
- **Rollback to the original jar works** in the measured drill
  ([M10.5](meritve/2026-10-30-M10.5-rollback.md), 58/58): the original loaded all NPCs with the same
  UUIDs, gear, stored data, quests, dialogs and player progress. On its next save it **drops the
  `Rw…` keys** (flight, hitbox, backend modes) and **Java scripts are kept as text but do not run**.
- Restoring the backup from before the switch is byte-exact but **loses all later progress**.
- Recommended order: stop server → back up world and config → swap jar on server and clients → start.

## 6. Known limitations of this release

Not proven, so not claimed:

- **Client compatibility matrix not passed** (M10.1a/b/d closed without acceptance, Q233): login,
  dialog/quest clicks, mail and mixed original/rework client–server pairs are not validated.
  Server-side original vs rework comparison passed only on a partial matrix (M10.1c, Q225).
- **Long-run stability:** the 4 h soak ran 2 h 25 min without a server fault, then the test's
  diagnostic chunk loader hit its limit (Q240); 4 h stability is not proven. `/rwdiag chunks`
  never releases chunks and logs an NPE on stop (Q241); do not use it on production servers.
- **No real modpack test** (M10.3 blocked, Q1). OptiFine, shaders and Entity Culling are handled
  by fallbacks but were not checked in a full modpack; the client coremod in a packaged client was
  not tested.
- Performance: against the 5 Oct baseline 8 of 9 cells were faster, idle-500 p95 −92.9 %
  ([M10.2](meritve/2026-10-30-M10.2-baseline.md)); measured on a dev server, one machine.
- `RwPathResume`/`RwFlyLineOfSight`: corner and maze cases still fail; Baritone backend has open
  cost limits ([M4.15a](meritve/2026-10-29-M4.15a-baritone.md)).
- Formations do not meet all planned acceptance criteria (D-079).
- `RwAnimHooks` observes once per tick; events of a step that a script stops later in the same
  tick are not reported; no walk/idle/death hooks; `blendTo` and `/npcanim reload` are v2.
- `RwJavaClasspath` is manual; automatic modpack classpath is v2.
- `RwFlightMode` has no GUI; Momentum flight mode is v2.

The full list of behaviour changes is in the changelog (M10.7) and in
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja).
