# Changelog — CustomNPCs Rework 1.12.2

## v1.0 candidate (2026-10-30)

Base: **CustomNPCs 1.12.2 (01Oct19)**, Forge 14.23.5.2847, Java 8. Settings, installation and
rollback: [UPORABNIKI.md](UPORABNIKI.md). Source of every entry: the register
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja) and the `@ConfigProp` text in
`CustomNpcs.java`. 0 is always the original behaviour.

This is a release **candidate**: the client matrix (M10.1a/b/d), the 4 h soak (M10.4) and a real
modpack test (M10.3, Q1) did not pass or were not run. See [UPORABNIKI.md §6](UPORABNIKI.md#6-known-limitations-of-this-release).

### Behaviour changes ON by default

You notice these without changing any setting. Set the key to 0 to get the original.

| Change | Key (default) | Side |
|---|---|---|
| An NPC with no path to its target waits 20 ticks before searching again; it can react up to 20 ticks later (M5.6, D-037) | `RwPathNegCache` (1) | server |
| NPCs beyond the tracker range are sent as light placeholders (`rwfar` channel) and drawn as impostors: no limb animation, eyes or name; half-visible NPCs are half-transparent impostors (M5-V, D-051) | `RwFarView` (2), `RwFarRange` (384) | both |
| Tracked NPCs farther than 56 blocks are drawn as impostors; NPCs out of vanilla render range stay visible as impostors up to the fog (M5-V.6) | `RwImpostorStart` (56), `RwImpostorAtlas` (2048) | client |
| With an active OptiFine shader pack the original renderer is used (no impostors) unless enabled (M5-V.7) | `RwFarShaders` (0) | client |
| The render box covers the whole model, so NPCs at the screen edge or partly behind cover are drawn; NPCs without a hitbox and lying/dead NPCs have the render range of a standing NPC with a hitbox (M5-K K12, D-052) | `RwRenderBox` (1) | client |
| 3D headwear skips fully transparent quads; single edge pixels may differ (M5-K K14b, D-049) | `RwHeadwearSkip` (1) | client |
| Client coremod inserts a call into `RenderGlobal.renderEntities`; with `RwFarView=0` it only returns (M5-V.5) | `RwFarView` | client |

### Same result, faster — ON by default

Proven identical in tests (D-032): `RwTargetPrefilter` (2), `RwPathFollowCache`, `RwPathSearchMemo`,
`RwMoveBoxes`, `RwDataShadow`, `RwCollideSkip`, `RwBlinkRecipients`, `RwLabelCache` (all 1).
Measured against the 5 Oct baseline: 8 of 9 cells faster, idle-500 p95 MSPT −92.9 %
([M10.2](meritve/2026-10-30-M10.2-baseline.md), dev server).

### Fixes — always on, no setting

- NBT JSON: long arrays read as longs; empty `[B;]`/`[I;]`/`[L;]` keep their type; byte/int/long
  lists stay lists; strings keep leading spaces and line breaks; a string ending in `\`, `NaN`,
  `Infinity` and an empty key are read instead of throwing; empty key is written; nesting is
  limited to 512 with a clear error instead of `StackOverflowError` (R9).
- Saving: clone, player, dialog, quest, linked NPC, trader market, world controller, client
  preset and schematic files are written, validated and atomically replaced; the old file stays
  on error. `SaveFile` flushes. The player save queue is bound to the world session (B1, B2).
- Scripts: a nested event restores the previous `Current` (and `CurrentType` in JS) (M6.6);
  reload restores a JS tab after an error and a previously missing hook (M6.5).
- Client: CustomNPCs packets are handed to the client thread before the player is looked up, so
  the login sync (dialogs, quests, factions, player data) is no longer dropped when it arrives
  before the join packet is processed (P2, Q251).

### New opt-in features — OFF by default

| Feature | Key / where | Notes |
|---|---|---|
| NPC mount ridden by an NPC steers itself; rider `tpTo` moves the mount; full hitbox after dismount (M3.3–M3.5) | `RwMountSteering` 0/1/2, `/rwmount` | |
| Melee attack starts before a wander/move path ends (M3.6) | `RwAttackPriority`, `/rwattack` | |
| Per-NPC hitbox modes solid/smart with shield weight; NPC carrier with NPC crew pushed with total mass (M3.8, D-028); mode in the NPC GUI (M3.8b) | `RwHitbox`, NBT `RwHitboxMode`, `RwHitboxShieldWeight` (200), `RwHitboxShieldItems` | |
| Path continuation to goals beyond the navigation range (M4.10, D-084) | `RwPathResume`, `/rwpath nadaljuj` | corner/maze cases still fail |
| Flying NPC line-of-sight check while following a path (M4.17, D-085) | `RwFlyLineOfSight`, `/rwpath vidno` | attempt-1 cost +32 % in flying combat (Q83) |
| Hover flight: direct 3D movement, holds position, stops without sliding (M4.3, D-087) | NBT `RwFlightMode`=1 | no GUI in v1 |
| Baritone navigation backend per NPC, GUI AI → Movement → Backend (M4.15b/c, D-089) | `RwNavBackend` + NBT `RwNavBackend`, `/rwnav` | needs `npcbaritone` library |
| Formations with home and facing at the end (M4.14) | `/rwsquad`, `FormationApi` | not all acceptance criteria met (D-079) |
| Client LOD: NPCs beyond N blocks without eyes, name, chat bubbles and 3D headwear (M5-K K5) | `RwLodStart` (0) | changes the look of distant NPCs |
| **Java NPC scripts** (`ScriptLanguage=Java`) with the same hooks as JS, `IPlayer`/`IWorld` access, API docs and IDE template (M6.1–M6.11, D-090) | `RwJavaClasspath` (empty = off) | trusted authors only, no sandbox; needs JDK 8; NPC scripts only |
| Animation API `Animators.of(npc)`: `play`, `queue`, `stop`, `getClip`, `getTick`, `getClips` (M7.9) | always present; does nothing without `customnpcentities` 0.7.0 | |
| Animation hooks `animationStart`, `animationEvent`, `animationEnd` in JS and Java (M7.5, D-095) | `RwAnimHooks` (0), read at startup | once per tick; no walk/idle/death hooks |
| Diagnostics and measurements (M2.1) | `/rwdiag`, `-Drwdiag=on` | `/rwdiag chunks` never releases chunks and logs an NPE on stop (Q241) |

Diagnostic commands: `/rwtarget`, `/rwhitbox`, `/rwfar`, `/rwimpostor`, `/rwcollide`, `/rwdata`,
`/rwmove`, `/rwblink`, `/rwcore`. Runtime changes made by commands are not saved to the config.

### Compatibility

- File formats unchanged; extra NBT keys `Rw…`. No migration.
- Rollback to the original jar works ([M10.5](meritve/2026-10-30-M10.5-rollback.md), 58/58); the
  original drops `Rw…` keys on its next save and keeps Java scripts as text without running them.
- Same jar on server and every client; mixed original/rework pairs are not validated.

### Moved to v2

Full client matrix and 4 h soak, `/npcanim reload` and `blendTo`, Momentum flight mode and a
flight GUI, automatic Java classpath, Java API for items/blocks/quests/dialogs/factions.
