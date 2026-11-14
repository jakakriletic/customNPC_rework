# Changelog — CustomNPCs Rework 1.12.2

## v2 development

- Optional single-JAR build with Varied Commodities, with its original Forge mod and registry IDs.
- Command Staff selects ground NPCs in a growing circle, applies six behaviour modes and sends
  the selection to separate formation slots (`RwCommandStaff=1`).

- NPC wand → **Global → Armies** lets operators create and list armies in game.
- With `RwAnimCore=1`, **Display → Rig model...** selects a built-in or server creature model.
- Server creature folders support custom models, textures, animation events, clone settings and scripts.
  `/rwcreature deploy` synchronizes assets to clients and places a complete pack.

## v1.0 candidate (2026-10-30)

Base: **CustomNPCs 1.12.2 (01Oct19)**, Forge 14.23.5.2847, Java 8. Settings, installation and
rollback: [UPORABNIKI.md](UPORABNIKI.md). Source of every entry: the register
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja) and the `@ConfigProp` text in
`CustomNpcs.java`. 0 is always the original behaviour.

This is a release **candidate**: the 4 h soak (M10.4) left an open heap question and a real
modpack test (M10.3, Q1) was not run. The client matrix passed later on a dev pair (Q436). See [UPORABNIKI.md §6](UPORABNIKI.md#6-known-limitations-of-this-release).

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
| Path continuation to goals beyond the navigation range (M4.10, D-084) | `RwPathResume`, `/rwpath nadaljuj` | flying: stalled resume retries once with a 1024-node search (P3); corner/maze pass only together with `RwFlyLineOfSight`; walking keeps the 200-node limit |
| Flying NPC line-of-sight check while following a path (M4.17, D-085) | `RwFlyLineOfSight`, `/rwpath vidno` | attempt-1 cost +32 % in flying combat (Q83) |
| Hover flight: direct 3D movement, holds position, stops without sliding (M4.3, D-087) | NBT `RwFlightMode`=1; AI → Movement → `Fly: Hover` when Navigation = Flying (P6) | Original remains default; saved Momentum shown disabled and preserved |
| Baritone navigation backend per NPC, GUI AI → Movement → Backend (M4.15b/c, D-089) | `RwNavBackend` + NBT `RwNavBackend`, `/rwnav` | needs `npcbaritone` library; P8 cost at 2000 NPCs measured once ([note](meritve/2026-11-03-P8-baritone-cena.md)): lower MSPT, live heap +157/+449 MB, possible per-NPC settings retention in the library |
| Formations with home and facing at the end (M4.14) | `/rwsquad`, `FormationApi` | P4: members go in column through narrow doors and follow the shared path without extra searches; acceptance per Q257 (slots, lag), one scene |
| Client LOD: NPCs beyond N blocks without eyes, name, chat bubbles and 3D headwear (M5-K K5) | `RwLodStart` (0) | changes the look of distant NPCs |
| **Java NPC scripts** (`ScriptLanguage=Java`) with the same hooks as JS, `IPlayer`/`IWorld` access, API docs and IDE template (M6.1–M6.11, D-090) | `RwJavaClasspath` (empty = off, `auto` = from the server, P5) | trusted authors only, no sandbox; needs JDK 8; NPC, player, forge, block, door and item scripts (P5); not projectile |
| Animation API `Animators.of(npc)`: `play`, `queue`, `stop`, `getClip`, `getTick`, `getClips` (M7.9) | always present; does nothing without `customnpcentities` 0.7.0 | |
| Animation hooks `animationStart`, `animationEvent`, `animationEnd` in JS and Java (M7.5, D-095); exact order of every action step and walk/idle/death layers with `event.source` (P7a); `IAnimator.blendTo(clip, ticks)` with a client pose transition (P7b); `/npcanim reload [entityId]` from `config/customnpcs/animations/` with confirmation by every client (P7c) | `RwAnimHooks` (0; 1 = actions + `blendTo` + `/npcanim`, 2 = + walk/idle/death), read at startup | needs `customnpcentities` 0.7.0; layers are script calls only; `blendTo` tested on an integrated client only; reload stops running actions of changed NPCs, dev pairs only, not kept across restart |
| Armies: register saved with the world, army name `RwArmy` on the NPC, `/rwarmy create/assign/unassign/list/info/remove`, script `ArmyApi` (M11.1, v2) | `RwArmies` (0), read at startup | owner is a player UUID or a faction; no map yet; counts loaded NPCs only ([note](meritve/2026-11-03-M11.1-register.md)) |
| Army permissions: `/rwarmy grant/revoke/grants/players`, *view* and *command* for selected or all armies, checked on the server at every command (M11.1b, v2) | `RwArmies` (0), read at startup | `/rwarmy` is now open to non-ops, each action is checked; `command` covers `assign`/`unassign`, `move`/`stop` and `stance`; Permissions GUI added in M11.8b; full permission matrix uses stand-ins ([note](meritve/2026-11-03-M11.1b-dovoljenja.md)) |
| Army clusters and snapshot: `/rwarmy clusters`, channel `rwarmy` answering requests only with the armies a player may see, last known position in the register (M11.2, v2) | `RwArmies` (0), `RwArmyClusterRadius` (48), `RwArmyClusterMin` (8), read at startup | no client GUI draws it yet; cost not measured; tested with one dev client on a dev dedicated server ([note](meritve/2026-11-03-M11.2-grozdi-posnetek.md)) |
| Army companies and saved moves: `/rwarmy move/stop/status`, `ArmyApi.move/stop/status`, one route with company offsets and join for stragglers; active intent resumes after restart (M11.3, v2) | `RwArmies` (0), `RwArmyCompany` (8, 1–24) | 32 ground NPCs in 4 companies of 8, flat legion, dev dedicated only; no long routes, production chunk tickets or combat yet; cost pending M11.10 ([note](meritve/2026-11-03-M11.3-cete.md)) |
| Army swarm and stance: `move … swarm … passive|engage`, `/rwarmy stance`, `ArmyApi.engagement`; saved mode/stance, scattered offsets without forming rows, passive rejects targets during active march/join, engage yields to combat and retains the member's destination (M11.4, v2) | `RwArmies` (0) | one swarm of 8 after restart, real melee kill and return to legion, current rights, stop and equality at 0; flat dev dedicated only; target selected by harness; multiple swarm companies and other combat modes untested; cost M11.10 ([note](meritve/2026-11-03-M11.4-roj-drza.md)) |
| Army Command Staff and AI → Army...: edit existing memberships, confirmed assignment within 16 blocks, Nearby/All lists, server ray target, modes/stance, stop/return with current rights (M11.8a, v2) | `RwArmies` (0); `customnpcs:army_staff` always registered, inactive at 0 | GUI 97 and snapshot regression 90 checks, restart and switch 0, five PNGs reviewed; one real dev client + dev dedicated, 3 NPCs on flat ground; tutorial partly covers Refresh, status limited to two lines; integrated/packaged pairs, larger groups and cost untested ([note](meritve/2026-11-03-M11.8a-palica.md)) |
| Army travelling chunk tickets: armies with an active move hold the chunks of their members plus one ring (at most 49), queued by name, hold the area of the saved last position after a restart, released at arrival/block/stop/server stop, leftovers released at world load (M11.6, v2) | `RwArmies` (0), `RwArmyMaxMarches` (2, 0–16), read at startup | dev dedicated only; M11.5 now proves unload and restart resume ARRIVED after clearing a stale anchor path; long routes of 200 through unloaded chunks remain untested ([original note](meritve/2026-11-03-M11.6-ticket.md), [fix](meritve/2026-11-03-M11.5-dolg-pohod.md)) |
| Army long marches: bounded real paths per leg, stable companies/offsets/direction, saved final goal; finish the last walkable metre with the same move helper and clear stale paths before a new anchor search (M11.5, v2) | `RwArmies` (0) | nine controller tests PASS on one fingerprint: 32 walk over 200 blocks with restart, short/swarm/terrain/ticket regressions, 200-member short final leg ARRIVED; one paired cost repetition p95 6.685 → 10.748 ms, allocation 48.2 → 74.1 MB/s. No widened arrival radius; ordinary squads unchanged. Long difficult routes and live map untested ([note](meritve/2026-11-03-M11.5-dolg-pohod.md)) |
| Army Command Staff Permissions tab: online/cached players, army checkboxes, view/command none/selected/all, server grants and live authority (M11.8b, v2) | `RwArmies` (0) | GUI 101 and staff regression 97 checks, restart/deop/0, five PNGs reviewed; one real dev client + dev dedicated, offline grantee; multiple armies processed sequentially, prior confirmed changes remain on denial/timeout; three reply lines with wheel scroll, 2000-character cap, tutorial partly covers top row; integrated/packaged pairs, concurrent grantors and cost untested ([note](meritve/2026-11-03-M11.8b-dovoljenja-gui.md)) |
| Army edge cases checked without production changes: water, gap, throat, closed-in member, deaths and departure during a march, holding at the goal, world reload mid-return (M11.9, v2) | `RwArmies` (0) | one company of 8 per army on a short route, dev dedicated with forced chunks; one closed-in member blocks the whole army; `ArmyApi.members` can return a dying member for up to 20 ticks ([note](meritve/2026-11-03-M11.9-robni-primeri.md)) |
| Army move without waiting: `freeforall` (`freeforall:column|ring`) in `/rwarmy move`, `ArmyApi.move`, the staff and the Xaero map (**Free for all**, default there; adapter v0.1.13, wire 0.6.2-armies; a map target may be at any distance, its chunk is loaded or generated; a click selects one flag and moving part of an army splits it off, also `/rwarmy moveflag`). One shared route, each member walks it at its own pace, a stuck member searches its own way, a blocked one never stops the others; the shape is taken at the goal (M11.11, v2) | `RwArmies` (0) | 199 scattered members over an obstacle course after a restart, one walled-in member blocked alone; 100 to a target 600 blocks away in never generated terrain with tickets only; dev dedicated only; march p95 15.2 ms vs idle 7.1 with 200 ([note](meritve/2026-11-11-M11.11-freeforall.md)) |
| Army march cost measured, no behaviour change: one army of 200 marching costs server MSPT p95 10.75 ms and 72.7 MB/s against 6.42 ms and 50.4 MB/s idle; travelling tickets add no visible cost; a real client commands 200 through the staff (M11.10, v2) | `RwArmies` (0) | historical measurement before M11.5, one repetition, dev dedicated on flat ground; the short-leg BLOCKED limitation is fixed and rechecked in M11.5 above; live Xaero map is a manual check ([note](meritve/2026-11-03-M11.10-cena.md)) |
| Linked NPCs keep their own home, orientation, moving path and place on that path when group data is applied, also after a world reload (M13.1, v2) | `RwLinkedOwnFields` (0) | two members with paths, dev dedicated, no player or GUI; members without a path not tested; already overwritten values are not restored; cost not measured. [Note](meritve/2026-11-04-M13.1-povezani.md) |
| Linked NPC groups: transfer categories per group with scripts as a new category, undo of the last change (`<group>.json_prev`), transfers per tick limited, `/rwlinked` (M13.2, v2) | `RwLinkedGroups` (0), `RwLinkedBudget` (50) | three members and 200 for the budget, dev dedicated, no player or GUI; `advanced`/`transform` only by key lists; with `RwLinkedOwnFields=0` linking still moves the home of a member with a path; chunk-load transfers not limited; MSPT not measured. [Note](meritve/2026-11-04-M13.2-kategorije.md) |
| Linked NPC groups in the GUI: category boxes, Apply and undo in *Global → Linked NPCs*, channel `rwlinked`, rights checked by the server on every request (M13.2b, v2) | `RwLinkedGroups` (0), both sides | one real dev client and dedicated server, two members; with a server at 0 the screen stays the original; packaged and integrated pairs not run |
| Wand list: multiple selection, name filter and same-name radius; link to an existing group, copy selected categories once, delete and reset the selection; rows show group and loaded count. Denied group replies clear stale boxes/count (M13.3, v2) | `RwLinkedGroups` (0), both sides | unit/build/package and 96 + 120 controller checks passed on one fingerprint; 14 screenshots reviewed. Six NPCs, dev dedicated pair; physical modifier keys, 100+ selections and packet cost not measured. [Note](meritve/2026-11-04-M13.3-palica.md) |
| Mob cloner: **Linked** option places a clone linked to the group named after it (new group from the clone, an existing group is not overwritten); after editing a linked NPC chat reports updated/pending loaded members (M13.4, v2) | `RwLinkedGroups` (0), both sides | one real dev client and dedicated server, three members, one in a truly unloaded chunk and after a restart; deop and 0/mixed pair checked; 127 controller checks, 9 screenshots reviewed. [Note](meritve/2026-11-04-M13.4-kloner.md) |
| Wand list footer keeps the group count with three digit selections; 100 placed NPCs linked with one selection and edited through the group (M13.5, v2) | `RwLinkedGroups` (0), both sides | one real dev client and dedicated server, 62 controller checks, 5 screenshots reviewed; all 100 loaded, MSPT and packets not measured |
| Diagnostics and measurements (M2.1) | `/rwdiag`, `-Drwdiag=on` | `/rwdiag chunks` never releases chunks and logs an NPE on stop (Q241) |
| Xaero army adapter v0.1.9: Armies tab, flags, whole-army selection, formation/swarm/stance/stop, current server rights and safe loaded surface targets; preserves fleet/Winter storm (M11.7b, v2) | `RwArmies` (0), same adapter on both ends | 83 unit/regression tests + build/reobf package passed; source synced and six installations updated with backups. Live map, production reflection, ground resolution and networked rights still untested; 256 markers/16 armies; price M11.10 ([note](meritve/2026-11-03-M11.7b-xaero-most.md), [controls](../integrations/xaero-fleet-command/ARMY-MAP.md)) |
| Army bridge API v1: frozen static UUID methods, current server authority, immutable map markers and reflection getters (M11.7a, v2) | `RwArmies` (0); version discovery always available | reflection unit contract (+10), build and package passed; no map adapter installed by this package, live bridge and production reflection boundary still untested ([contract](api/ARMY-BRIDGE-V1.md), [note](meritve/2026-11-03-M11.7a-pogodba.md)) |

Diagnostic commands: `/rwtarget`, `/rwhitbox`, `/rwfar`, `/rwimpostor`, `/rwcollide`, `/rwdata`,
`/rwmove`, `/rwblink`, `/rwcore`. Runtime changes made by commands are not saved to the config.

### Compatibility

- File formats unchanged; extra NBT keys `Rw…`. No migration.
- Rollback to the original jar works ([M10.5](meritve/2026-10-30-M10.5-rollback.md), 58/58); the
  original drops `Rw…` keys on its next save and keeps Java scripts as text without running them.
- Same jar on server and every client; mixed original/rework pairs are not validated.

**Later fix (Q435):** a dedicated server at `RwArmies=0` no longer registers `customnpcs:army_staff`, so an
original client can join it. A world saved at 1 still starts at 0 without a question, but staves leave inventories.
An original client cannot join a dedicated server at 1.

### Moved to v2

4 h soak, Momentum flight mode,
Java API for items/blocks/quests/dialogs/factions.
