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
| `RwJavaClasspath` | empty | enables Java scripts (see §4); `auto` = classpath built from the running server |

### Per-NPC settings (saved in the NPC)

| NBT key | Where to set | Values |
|---|---|---|
| `RwHitboxMode` | NPC GUI (hitbox) | 0 vanilla, 1 solid, 2 smart; needs `RwHitbox=1` |
| `RwNavBackend` | AI → Movement → Backend | 0 vanilla, 1 Baritone; needs the global permission |
| `RwFlightMode` | AI → Movement, first row (`Fly: Orig` / `Fly: Hover`), visible when Navigation = Flying | 0 original, 1 Hover (holds position, stops without sliding); saved Momentum (2) is shown disabled |

Choose Flying in Navigation to show the flight button, select Original or Hover, then close
Movement and the AI editor to save. Switching back to Ground hides the button but preserves
the stored flight mode; Hover applies only while flying. A new NPC defaults to Original.

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
- Needs a JDK 8 (`javac`) and `RwJavaClasspath`. `auto` (recommended) uses everything the server
  loaded (Forge, mods, libraries); on a packaged server it writes once
  `customnpcs-java/minecraft-<key>.jar` with the game classes under SRG names (~2 s at first start,
  reused after restart, rebuilt when mods change). A file or classpath set by hand still works.
  Empty = Java tabs stop with a clear error; JS is unaffected.
- NPC, player, forge, block, door and item script GUIs offer Java. A handler hook is a public method
  named after the hook with one event parameter; a wrong event type stops the tab with `hook.signature`.
  Projectile scripts stay JS only.
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

- **Client compatibility matrix only partly passed** (P2, Q254): with the rework on both sides,
  login sync, dialog/quest clicks, trader, bank, skin download failure and resource reload pass on
  an integrated and a dedicated server; mail to an offline player and relogin pass on integrated
  only. Mixed original/rework client–server pairs are not validated. An original client on a
  dedicated server can miss the CustomNPCs login sync (an original bug, fixed only in the rework
  client). Mail cannot be sent to a player who first joined after the server started until the
  next restart (original behaviour). Tested on a dev client with an offline player, not a
  packaged client (Q22). Server-side original vs rework comparison passed only on a partial
  matrix (M10.1c, Q225).
- **Long-run stability:** a 4 h soak with 200 NPCs ran without a server fault (49/49 samples, p95
  MSPT 3.28 → 3.47 ms), but the heap after full GC grew 168 → 242 MB (+44 %) while the test world
  kept expanding; a slow leak is not ruled out (P1). `/rwdiag chunks` is a diagnostic tool only.
- **No real modpack test** (M10.3 blocked, Q1). OptiFine, shaders and Entity Culling are handled
  by fallbacks but were not checked in a full modpack; the client coremod in a packaged client was
  not tested.
- Performance: against the 5 Oct baseline 8 of 9 cells were faster, idle-500 p95 −92.9 %
  ([M10.2](meritve/2026-10-30-M10.2-baseline.md)); measured on a dev server, one machine.
- `RwPathResume`/`RwFlyLineOfSight` (P3, Q256): the flying corner and maze test courses pass
  6/6 only with **both** switches on; `RwPathResume` alone still fails the corner. The wider search
  (1024 nodes, once per stalled resume, ~0.8 ms) is flying only; walking NPCs keep the vanilla
  200-node limit. Cost in flying combat with the wider search was not measured. Baritone backend has open
  cost limits ([M4.15a](meritve/2026-10-29-M4.15a-baritone.md)).
- Formations (P4, Q257): on the door and open test courses all 8 slots are filled (door 101 ticks,
  open 40), one path search per squad, no stuck members, member lag ≤ 1.1 blocks. Members do not
  all end within 2 blocks of the goal and the squad is wider than 6 blocks at a 1-block door; this
  is the legion shape, not a fault. One test run, one scene; the manual in-game checks (F1–F12)
  were not done.
- `RwAnimHooks` observes once per tick; events of a step that a script stops later in the same
  tick are not reported; no walk/idle/death hooks; `blendTo` and `/npcanim reload` are v2.
- Java scripts (P5, Q263–Q268): `RwJavaClasspath=auto` and Java in the player/forge/block/door/item
  GUIs were tested on a packaged Forge server without other mods. Classes that other mods change
  with coremods or mixins are seen in their Forge form; no real modpack was tested (Q1).
- `RwFlightMode` now has a GUI (P6): Original/Hover selection, cloning and restart passed on an
  integrated dev client (137 checks). The 10 screenshots were reviewed, but the movement tutorial
  toast covers the new button; its label and bounds were checked by the GUI harness. Packaged
  and dedicated clients were not tested for P6. Momentum flight mode stays v2 and cannot be
  selected in the GUI; an existing NBT value of 2 is preserved.

The full list of behaviour changes is in the [changelog](CHANGELOG.md) and in
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja).
