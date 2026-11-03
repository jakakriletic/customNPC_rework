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
- Armies: for the snapshot set `RwArmies=1` on the server and on clients. A client with 1 on a server with
  0 gets no answer and stays connected; a client with 0 on a server with 1 was not tested.

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
| `RwAnimHooks` | 0 | `animationStart/animationEvent/animationEnd` script hooks for every action step (needs `customnpcentities`; read at startup); 2 = also walk/idle/death (`event.source`); >=1 also enables `blendTo` and `/npcanim` (set on server and clients) |
| `RwLodStart` | 0 | client: distant NPCs without eyes, labels and 3D headwear beyond this many blocks |
| `RwArmies` | 0 | armies: `/rwarmy`, `ArmyApi` and the channel `rwarmy`, register and move orders saved in `data/rwarmies.dat` (read at startup; 0 keeps `RwArmy` names on NPCs); also `RwArmyClusterRadius` (48, 1–512), `RwArmyClusterMin` (8, 1–1024), `RwArmyCompany` (8, 1–24; only 8 tested in-world) |
| `RwJavaClasspath` | empty | enables Java scripts (see §4); `auto` = classpath built from the running server |

### Per-NPC settings (saved in the NPC)

| NBT key | Where to set | Values |
|---|---|---|
| `RwHitboxMode` | NPC GUI (hitbox) | 0 vanilla, 1 solid, 2 smart; needs `RwHitbox=1` |
| `RwNavBackend` | AI → Movement → Backend | 0 vanilla, 1 Baritone; needs the global permission |
| `RwArmy` | AI → Army..., command staff, `/rwarmy assign`, `ArmyApi.assign` | army name (string); none = no key; needs `RwArmies=1` to change |
| `RwFlightMode` | AI → Movement, first row (`Fly: Orig` / `Fly: Hover`), visible when Navigation = Flying | 0 original, 1 Hover (holds position, stops without sliding); saved Momentum (2) is shown disabled |

Choose Flying in Navigation to show the flight button, select Original or Hover, then close
Movement and the AI editor to save. Switching back to Ground hides the button but preserves
the stored flight mode; Hover applies only while flying. A new NPC defaults to Original.

### Commands (op)

`/rwdiag` (measurements, off by default), `/rwmount`, `/rwattack`, `/rwtarget`, `/rwpath`,
`/rwnav`, `/rwhitbox`, `/rwsquad` (formations), `/rwfar`, `/rwimpostor`, and diagnostics
`/rwcollide`, `/rwdata`, `/rwmove`, `/rwblink`, `/rwcore`. Runtime changes made by a command
are not written to the config.

With `RwAnimHooks>=1`: `/npcanim reload [entityId]` loads animations from
`config/customnpcs/animations/<entityId>.animation.json` (no file = the clips from the mod jar) and
`/npcanim status` shows the current revision. Every player online must accept the files first;
otherwise nothing changes. Running actions and `Animators` queues of the changed NPCs are stopped.
The reload lasts until the server stops.

With `RwArmies=1`: `/rwarmy create <army> [player|uuid:<uuid>|faction:<id>]` (from the console the
owner is required), `assign <army> <npc name|selector> [range]` (name: loaded NPCs within 100 blocks,
at most 220), `unassign <npc name|selector> [range]`, `list`, `info <army>`, `remove <army>`. Army names
are 1–32 letters, digits, `_` or `-` and ignore case. Scripts: `Java.type("noppes.npcs.rework.army.ArmyApi")`
with `create`, `remove`, `assign`, `unassign`, `armyOf`, `list`, `exists`, `owner`, `members`, `canView`,
`canCommand`, `visibleArmies`, `grant`, `revoke`, `move`, `stop`, `status`. Scripts act with operator rights.

Army permissions: an op creates armies. The owner (own armies) or an op (all armies) gives a player a right:
`/rwarmy grant <player> view|command <army|all>`, `/rwarmy revoke <player> view|command|all [army|all]`,
`/rwarmy grants [player]`, `/rwarmy players [prefix]` (online and already seen players). `command` includes
`view` and allows `assign`, `unassign`, `move` and `stop`. `all` also covers later armies: the owner's own, or every army
when an op grants it. Only the owner or an op removes an army. Rights are checked on every command, so a
revoke or deop applies to the next one. An army you may not see answers as `unknown army`.

`/rwarmy clusters [army]` shows the clusters of the armies you may see: members within
`RwArmyClusterRadius` blocks horizontally form one cluster. The largest is the main one, a smaller one with at least
`RwArmyClusterMin` members is a group, the rest are stragglers. An army with no loaded members stays listed with
its last known position.

`/rwarmy move <army> <x> <y> <z> [legion|column|ring|swarm] [speed] [passive|engage]` moves loaded ground members in
the sender's dimension (relative `~` coordinates work; default legion, speed 3, range 0.5–6).
The army uses companies of 8 sharing one route; stragglers join first. The move stores its company
size and resumes after a restart when members load. `/rwarmy stop <army>` cancels it;
`/rwarmy status <army>` needs `view` and reports WAITING/JOINING/MOVING/ARRIVED/BLOCKED/CANCELLED.
No complete route or a timeout means BLOCKED; issue a new move to retry. Formations must be enabled
(do not set `-Drwformation=off`). Scripts: `ArmyApi.move(army, dimension, x, y, z, shape, speed)`
returns a boolean, as does `stop(army)`; `status(army)` returns text.

`swarm` follows the shared route immediately with stable scattered offsets instead of forming rows.
`passive` rejects attack and revenge targets during an active march or join. `engage` lets a member
fight, then return to its company destination; a living fighter prevents the company from completing.
Omitting the stance keeps the previous stance (initially `engage`). `/rwarmy stance <army> passive|engage`
changes it for an existing order and rebuilds active companies. Both `move` and `stance` need current
`command` rights. Passive ends at arrival, BLOCKED or stop; it does not change factions or invulnerability.
Mode and stance survive restart. Scripts can add `stance` after `speed` in `ArmyApi.move(...)`, or call
`ArmyApi.engagement(army, stance)`; these trusted script calls act with operator rights.

### Army Command Staff (M11.8a)

Set `RwArmies=1` on both sides. Get `customnpcs:army_staff` from Creative Tools or
`/give <player> customnpcs:army_staff` (no recipe). Hold it in the main hand and right-click
an NPC to edit its army, or air/ground to open the army list. An op holding the NPC wand
can also use AI → Army.... Enter an existing army name and click **Set NPC army**; empty
clears membership. Creating an army remains an op command with an owner.

**Nearby (256)** lists loaded clusters within 256 horizontal blocks in your dimension;
**All armies** includes visible empty/unloaded armies and their last position. Counts are
loaded members only. Select an army, choose legion/column/ring/swarm and passive/engage,
then **Aim / right-click**: look at safe ground and right-click again. Sneak + right-click
cancels aiming. The server checks the ray through loaded chunks, up to 256 blocks; a miss
keeps the previous order. **Stop** cancels; **Return here** uses your current position.
Staff moves use speed 1. **Assign within 16** asks for confirmation and assigns loaded NPCs
in a 16-block sphere (up to 256; more changes nothing). Existing assignment permissions apply
to each NPC; an unassigned NPC still needs an op or script. Every action checks current rights.
The staff remains registered but inactive when armies are disabled.

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
- Animations: `Animators.of(npc)` (`play`, `blendTo`, `queue`, `stop`, `getClip`, `getTick`, `getClips`) works
  only with `customnpcentities` 0.7.0; without it `isAvailable()` is false and calls do nothing.
  After `/npcanim reload` a queued or running clip of a changed NPC is stopped; play it again.

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
  200-node limit. Cost in flying combat with the wider search was not measured.
- Baritone backend (`RwNavBackend`, P8, [note](meritve/2026-11-03-P8-baritone-cena.md)): at 2000 NPCs with an
  unreachable target, MSPT 87.5 → 65.8 ms (TPS 11.4 → 15.1, still below 20); in combat 43.9 → 41.5 ms
  (p99 58.7 → 60.8). Live heap after full GC is higher: +157 MB after the unreachable test, +449 MB after
  both tests in one server; the library keeps about 213 settings objects per NPC created, which may not be
  released when NPCs are removed (not proven). One run on one machine; capacity is not promised.
- Formations (P4, Q257): on the door and open test courses all 8 slots are filled (door 101 ticks,
  open 40), one path search per squad, no stuck members, member lag ≤ 1.1 blocks. Members do not
  all end within 2 blocks of the goal and the squad is wider than 6 blocks at a 1-block door; this
  is the legion shape, not a fault. One test run, one scene; the manual in-game checks (F1–F12)
  were not done.
- `RwAnimHooks` (P7a, [note](meritve/2026-11-01-P7a-dnevnik-animacij.md)): every action start, clip
  event and stop is now reported in order, also several in one tick and a stop in the tick of an event;
  `RwAnimHooks=2` adds walk/idle/death (server-side script calls only). Tested on a packaged server with
  `customnpcentities` 0.7.0 only; if that mod's class does not match, the hooks stay off with an error.
  The end of death is reported at respawn; removing an NPC calls no hook.
- `blendTo(clip, ticks)` (P7b, [note](meritve/2026-11-01-P7b-blendto.md)): needs `RwAnimHooks>=1` on the
  server (otherwise it returns false) and on the client (otherwise the clip starts without a transition).
  The pose moves linearly from the last drawn pose over 0–200 ticks; a new action interrupts, death is never
  blended. Tested on an integrated dev client only, not on a dedicated server or packaged client; a player
  who starts tracking the NPC mid-blend sees the plain clip; client cost not measured.
- `/npcanim reload` (P7c, [note](meritve/2026-11-01-P7c-reload.md)): needs `RwAnimHooks>=1`. Tested on
  a dev client with an integrated and a dev dedicated server and one player; packaged pairs not tested
  and cost not measured. A player who refuses or does not confirm within 10 s aborts the reload; a
  player joining later who cannot accept the files is disconnected. Not kept across a server restart.
- Java scripts (P5, Q263–Q268): `RwJavaClasspath=auto` and Java in the player/forge/block/door/item
  GUIs were tested on a packaged Forge server without other mods. Classes that other mods change
  with coremods or mixins are seen in their Forge form; no real modpack was tested (Q1).
- `RwFlightMode` now has a GUI (P6): Original/Hover selection, cloning and restart passed on an
  integrated dev client (137 checks). The 10 screenshots were reviewed, but the movement tutorial
  toast covers the new button; its label and bounds were checked by the GUI harness. Packaged
  and dedicated clients were not tested for P6. Momentum flight mode stays v2 and cannot be
  selected in the GUI; an existing NBT value of 2 is preserved.
- Armies (`RwArmies`, M11.1/M11.1b/M11.2/M11.3/M11.4/M11.8a, [note](meritve/2026-11-03-M11.1-register.md),
  [permissions](meritve/2026-11-03-M11.1b-dovoljenja.md), [clusters](meritve/2026-11-03-M11.2-grozdi-posnetek.md),
  [companies](meritve/2026-11-03-M11.3-cete.md), [swarm/stance](meritve/2026-11-03-M11.4-roj-drza.md)):
  short moves, passive swarm and combat return work for loaded ground members. An NPC without an army can be
  assigned only by an op or a script. Faction membership gives no rights. Permissions have no GUI yet
  (M11.8b). The full permissions matrix used stand-in players; staff deop/view/revoke was also tested
  with one real client. `members`, `list` and `info`
  count loaded NPCs only; after `remove`, NPCs in unloaded chunks keep the old name without effect.
  The register and permissions were tested on a dev dedicated server only. Clusters and the snapshot were tested
  with one real dev client on a dev dedicated server, not on integrated or packaged pairs. The
  [staff GUI](meritve/2026-11-03-M11.8a-palica.md) passed assignment, bulk assignment of 3, aiming,
  physical cluster movement/return, stop, current rights, restart and switch 0. Five PNGs were reviewed;
  a tutorial toast partly covers Refresh and long status text can be cut after two lines. Other resolutions,
  larger bulk groups, integrated/packaged staff pairs and difficult terrain were not tested. The ray's
  vanilla step limit may shorten its reach. Map integration is M11.7; cost is M11.10. After a restart the
  time of the last position can be older than the last time the army was seen; the position itself is kept.
  Movement was tested with 32 NPCs in four companies of 8, legion on flat ground, on a dev dedicated
  server without a client, including a restart mid-march. Column/ring have unit geometry checks only;
  larger companies and manual `/rwsquad` takeover were not tested. M11.4 proved one swarm of 8 after restart,
  six active passive samples, one real melee kill and return of all 8 to a legion company on flat ground,
  dev dedicated only. The combat target was assigned by the harness; natural enemy acquisition, multiple
  swarm companies and ranged/mounted/flying combat were not tested. Macro offsets keep a constant
  direction around turns. At most 2048 loaded members; no complete path means BLOCKED. Long routes,
  moving chunk tickets and difficult terrain are M11.5/M11.6/M11.9; no progress is guaranteed in unloaded
  chunks. Rebuilding companies can change membership. Cost is pending M11.10; no performance claim.

The full list of behaviour changes is in the [changelog](CHANGELOG.md) and in
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja).
