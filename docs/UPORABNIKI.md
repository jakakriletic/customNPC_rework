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
- Put the **same jar on the server and on every client** where you can. Mixed pairs passed the client
  matrix with default settings: an original client on a rework server, and a rework client on an
  original server (see §6). An original client cannot join a dedicated server with `RwArmies=1`.
- **Back up the whole world folder and `config/` before the first start** (see §5).
- On first start, missing `Rw…` keys are added to `config/CustomNpcs.cfg` with the defaults below.
  Keys already present keep their value.
- Armies: for the snapshot set `RwArmies=1` on the server and on clients. A client with 1 on a server with
  0 gets no answer and stays connected; a client with 0 on a server with 1 was not tested.

For Xaero army commands install `xaerofleetcommand-1.12.2-v0.1.13.jar` on both client and server;
the client also needs Xaero's World Map. Enable `RwArmies` and formations on the server.
Open the **Armies** map tab, click a flag to select only that group (double click: every flag of its
army) and right-click a destination at any distance in the current
dimension (an unloaded chunk is loaded or generated for the click; water or lava moves the target to the nearest
safe ground within 8 blocks). Free for all (default), Formation/Swarm, stance and Stop use current server rights. The tab is hidden
without a compatible provider or visible positions. See [controls](../integrations/xaero-fleet-command/ARMY-MAP.md).
This adapter also includes Winter storm. Build validation passed; live map validation is pending.

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
| `RwAnimCore` | 0 | built-in animation core: NPCs with `RwRig` drawn as models with bones, clips, data attacks, `/rwrig` (server and clients; see "Animated NPCs") |
| `RwLodStart` | 0 | client: distant NPCs without eyes, labels and 3D headwear beyond this many blocks |
| `RwArmies` | 0 | armies: `/rwarmy`, `ArmyApi` and the channel `rwarmy`, register and move orders saved in `data/rwarmies.dat` (read at startup; 0 keeps `RwArmy` names on NPCs); also `RwArmyClusterRadius` (48, 1–512), `RwArmyClusterMin` (8, 1–1024), `RwArmyCompany` (8, 1–24; only 8 tested in-world), `RwArmyMaxMarches` (2, 0–16: armies with an active move that hold travelling chunk tickets; 0 = no tickets) |
| `RwLinkedOwnFields` | 0 | Linked NPCs: each member keeps its own home, orientation, moving path and place on that path when group data is applied; 0 = original (see §6). Does not repair values already overwritten |
| `RwLinkedGroups` | 0 | Linked NPCs: per-group transfer categories (scripts off by default), undo of the last change, `/rwlinked`, the boxes in *Global → Linked NPCs* and multiple selection in the wand list (read at startup); 0 = original (see §6) |
| `RwLinkedBudget` | 50 | with `RwLinkedGroups=1`: group transfers per server tick; a member over the budget waits 20 ticks. Transfers when a chunk loads are not limited. 0 = no limit |
| `RwJavaClasspath` | empty | enables Java scripts (see §4); `auto` = classpath built from the running server |

### Per-NPC settings (saved in the NPC)

A new NPC (since 12. 11. 2026) has no extra eye layer (the eyes of its skin) and does not respawn
(Stats -> Respawn: No); saved and cloned NPCs keep their own values.

| NBT key | Where to set | Values |
|---|---|---|
| `RwHitboxMode` | NPC GUI (hitbox) | 0 vanilla, 1 solid, 2 smart; needs `RwHitbox=1` |
| `RwNavBackend` | AI → Movement → Backend | 0 vanilla, 1 Baritone; needs the global permission |
| `RwRig` | `/rwrig set/apply`, `RigApi`, clone JSON | compound: `Model`, `Texture`, `Scale`, `Clips`, `Idle`/`Walk`/`Hurt`/`Death`; drawn and played with `RwAnimCore=1`, kept with 0 |
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

With `RwLinkedGroups=1`: `/rwlinked list`, `/rwlinked info <group>`, `/rwlinked undo <group>` and
`/rwlinked categories <group> default|none|all|<names>|+name|-name ...`, with the names `appearance`, `name`, `stats`,
`ai`, `inventory`, `advanced`, `scripts`, `transform`. `undo` swaps the group with `<group>.json_prev`, so a second
`undo` brings the change back. In the NPC menu, *Global → Linked NPCs* has the same boxes, **Apply** and **Undo last change**
(server and client both need the switch; a box changes nothing until you press Apply).

With the switch on both sides, right-click the NPC wand in the air to open its list. Click a row to select it,
Ctrl-click to add or remove a row, and Shift-click to select a range. Type in the top field to filter by name;
rows hidden by the filter leave the selection. **Same name** selects matching visible NPCs within the radius of
the player (default 32 blocks, 0 = any distance); the primary row itself stays selected even outside that radius.
The last selected row is primary: **Edit** and **Tp to** use it. **Copy from** copies selected categories from it
to the other selected NPCs once, without making a link, and keeps each target's home, orientation and path.
**Link to group** uses an existing group and its categories. **Delete** (after confirmation) and **Reset** act on
the selection. Rows show the group and its loaded member count. **Reset All** and **Freeze NPCs** keep their original scope.
The server checks operator level 2, the wand in the main hand and the original action permission on every batch;
copying scripts also requires the scripter permission. Without the switch or batch rights, the list stays original.

With the switch on both sides, the mob cloner (*Clones* and *Server* tabs) has a **Linked: Yes/No** button
(`Clone = group`): with Yes, **Spawn** places the clone linked to the group named after the clone. A new group starts
from the clone; an existing group is used as it is and is not overwritten. **MobSpawner** is disabled while Yes is set.
The server checks operator level 2, the Linked NPCs and mob spawner permissions, the cloner in hand, a safe group name
and a nearby loaded target before placing. After you close the menu of a linked NPC whose group changed, chat reports
how many loaded members were updated and how many are pending; unloaded members update when their chunk loads
(their number is not known).

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

`/rwarmy move <army> <x> <y> <z> [legion|column|ring|swarm|freeforall[:shape]] [speed] [passive|engage]` moves loaded ground members in
the sender's dimension (relative `~` coordinates work; default legion, speed 3, range 0.5–6).
The army uses companies of 8 sharing one route per leg; stragglers join first. Long moves use
bounded real paths (at most 24 Manhattan blocks, also limited by navigation range minus 8).
All companies must arrive before the next leg; intermediate legs keep the original home and final intent.
The move stores its company size and resumes after a restart when members load. `/rwarmy moveflag <army> <flagX> <flagZ> <x> <y> <z> [shape] [speed] [passive|engage]` moves only the cluster
(map flag) nearest flagX/flagZ (default shape `freeforall`): when it is not the whole army its members are split off
into a new army `<army>-2` (then `-3`, ...) with the same owner and selected grants, and only that army moves; the
rest keeps its order. The map does the same when you select single flags. `/rwarmy stop <army>` cancels it;
`/rwarmy status <army>` needs `view` and reports WAITING/JOINING/MOVING/ARRIVED/BLOCKED/CANCELLED.
No complete route or a timeout means BLOCKED; issue a new move to retry. Formations must be enabled
(do not set `-Drwformation=off`). Scripts: `ArmyApi.move(army, dimension, x, y, z, shape, speed)`
returns a boolean, as does `stop(army)`; `status(army)` returns text.

`freeforall` (M11.11; also `freeforall:column`, `freeforall:ring`, default legion at the goal) does not wait:
there are no companies and no join phase. One route is searched per leg as before, but every member walks it
at its own pace on its own lane (the route shifted sideways by where it stood, up to 32 blocks), so the army keeps
its width instead of a column; where a lane is blocked the member takes the shared route and fans out again after it. A member pushed aside walks back onto it, one that starts away from it or gets stuck runs its own
search (bounded like a leg, sideways on repeated failures). A member that still cannot get through is reported
blocked and never holds the others. Each member has its own slot in the chosen shape around the goal. The army
is ARRIVED when every loaded member holds its slot or has stayed blocked for 30 s (BLOCKED if nobody arrived);
`status` shows arrived/moving/blocked/fighting counts. A member that joins or leaves does not restart the others.
The map and the staff offer it as **Free for all**, the default on both (in single player too). Legion, column,
ring and swarm still use companies, which can block in rough terrain when stragglers find no path to the main group.

`swarm` follows the shared route immediately with stable scattered offsets instead of forming rows.
`passive` rejects attack and revenge targets during an active march or join. `engage` lets a member
fight, then return to its company destination; a living fighter prevents the company from completing.
Omitting the stance keeps the previous stance (initially `engage`). `/rwarmy stance <army> passive|engage`
changes it for an existing order and rebuilds active companies. Both `move` and `stance` need current
`command` rights. Passive ends at arrival, BLOCKED or stop; it does not change factions or invulnerability.
Mode and stance survive restart. Scripts can add `stance` after `speed` in `ArmyApi.move(...)`, or call
`ArmyApi.engagement(army, stance)`; these trusted script calls act with operator rights.

### Army Command Staff (M11.8a)

Set `RwArmies=1` on both sides. A dedicated server has the staff only with `RwArmies=1`, and an original
client cannot join such a server. Get `customnpcs:army_staff` from Creative Tools or
`/give <player> customnpcs:army_staff` (no recipe). Hold it in the main hand and right-click
an NPC to edit its army, or air/ground to open the army list. An op holding the NPC wand
can also use **Global → Armies → Create army** to register a new army, then **AI → Army...**
to select it from the list or enter its name and click **Set NPC army**. The operator who creates
the army owns it. An empty selection clears membership. The create command also accepts another
player or faction as owner.

**Nearby (256)** lists loaded clusters within 256 horizontal blocks in your dimension;
**All armies** includes visible empty/unloaded armies and their last position. Counts are
loaded members only. Select an army, choose legion/column/ring/swarm/freeforall and passive/engage,
then **Aim / right-click**: look at safe ground and right-click again. Sneak + right-click
cancels aiming. The server checks the ray through loaded chunks, up to 256 blocks; a miss
keeps the previous order. **Stop** cancels; **Return here** uses your current position.
Staff moves use speed 1. **Assign within 16** asks for confirmation and assigns loaded NPCs
in a 16-block sphere (up to 256; more changes nothing). Existing assignment permissions apply
to each NPC; an unassigned NPC still needs an op or script. Every action checks current rights.
With `RwArmies=0` the staff is inactive on a client and in single player. A dedicated server at 0 does not
have the item at all: a world saved at 1 starts without a question, but staves are dropped from inventories
and do not come back when you return to 1.

**Permissions (M11.8b):** owners and ops can choose an online or cached player (four per
page); enter a name prefix and use **Find players** to narrow the list (up to 50 matches).
Choose **Select armies** and check armies, or type one army name. **View/Command: selected**
adds that right; **all** includes future armies in your owner/op scope; **none** clears
that right in your scope. Command includes view. **Revoke army** removes explicit rights
for the checked/typed armies; an all-armies grant or another grantor's rights may remain.
**Show grants** shows server records; scroll the reply with the mouse wheel. Checkboxes
select the next action, rather than showing current grants. Changes are checked on the
server at every click. Multiple armies are processed in order; denial or timeout stops
the remaining changes and keeps those already confirmed. Closing the GUI stops the queue.

### Animated NPCs: models with bones (M16, `RwAnimCore=1`)

Set `RwAnimCore=1` on the server **and** every client. Then any NPC can wear a model with bones
instead of the biped, with its own animations and attacks, without `customnpcentities` and without
rebuilding the jar:

| Model | Bones / clips |
|---|---|
| `steve`, `legionary` | the plain player body split at elbows and knees, drawn with the NPC's own CustomNPCs skin (64x64: file, URL or player); `legionary` also holds a gladius and a scutum. All human clips plus shield_advance (scutum raised, upper body only, the legs keep walking), shield_stab, shield_bash |
| `humanoid`, `knight`, `skeleton_warrior` | one human skeleton (elbows, knees, sword, shield); idle, walk, guard, slash, overhead, thrust, block, kick, spin, leap_slam, warcry, salute, wave, hurt, death |
| `troll`, `ogre` | giant with a club; idle, walk, smash, sweep, stomp, pound, backhand, throw, roar, hurt, death |
| `warg` | bite, maul, pounce, howl (+ idle, walk, hurt, death) |
| `giant_spider` | bite, spit, leap, brood, threat (+ idle, walk, hurt, death) |
| `cave_bear` | bite, swipe, maul, charge, rear_roar (+ idle, walk, hurt, death) |

`/rwrig models` lists them with their clips. More models: put `<id>.geo.json` (Bedrock/Blockbench
geometry), `<id>.png` and `<id>.animation.json` in `customnpcs/rigs/` (next to `clones/`) on the
server and the clients, then `/rwrig reload`.

**In game:** open an NPC with the wand and choose **Display → Rig model...**. Choose a built-in
rig or a server creature model, or **Original** to clear the rig. The selection saves with the
rest of the Display settings. The equivalent commands are `/rwrig set @near knight` (nearest NPC
within 16 blocks, or a name prefix), `/rwrig clear @near` and `/rwrig play @near leap_slam`.

**Everything is in the NPC (`RwRig` in its NBT)**, so the clone tab, `/noppes clone` and spawners
keep it:

```json
"RwRig": {"Model": "knight", "Scale": 1.25, "Clips": "{\"animations\":{...}}", "Idle": "guard"}
```

`Clips` holds the NPC's own clips (Bedrock `.animation.json` format: bones, rotation/position/scale
keyframes in seconds, `easing`, `loop`); they add to and replace the model's by name.

**Attacks are data.** A clip's `events` act on the server at their time: `hit` (area damage =
`damage` x the NPC's melee strength; `shape` circle|arc, `forward`/`side`/`radius`/`arc`/`dir` in
blocks and degrees, measured from the NPC and scaled with its size; `knockback`, `lift`, `effect`, `fire`; only enemies by faction), `sound`
(`minecraft:...` or a built-in `troll.roar`), `lunge` (`forward`, `up`), `heal` (`amount`). Every
event also calls the script hook `animationEvent` (`event.type`, `event.data` = the event's JSON),
plus `animationStart` and `animationEnd`.

**How an AI makes an animated boss in 5 minutes (no Java, no rebuild):**

1. Write one *rig package* `customnpcs/rigs/<name>.rig.json` (example:
   `rig_src/examples/knight_boss.rig.json`, `rig_src/examples/legionary.rig.json`: raises the scutum when it
   engages, advances behind it, stabs or bashes in reach, raises it again): `model`, optional `name`, `scale`, `idle`/`walk`/
   `hurt`/`death`, `skinTexture`/`skinUrl`/`skinPlayer` (for `steve`/`legionary`), `clips` (own clips) and
   `script` (JS lines). A skin file goes to `customnpcs/assets/customnpcs/textures/entity/humanmale/`
   (CustomNPCs reads that folder as a resource pack): `"skinTexture": "customnpcs:textures/entity/humanmale/<file>.png"`. The script picks attacks, e.g.
   in `tick`: `Animators.of(e.npc).play('slash')` when the target is close; `meleeAttack`:
   `e.setCanceled(true)` so damage comes only from the clips' `hit` events.
2. In game: `/rwrig apply @near <name>.rig.json` (problems are listed in chat).
3. Save the NPC with the clone tool. The clone carries model, clips and script.

Scripts: `var Animators = Java.type('noppes.npcs.rework.anim.Animators')` (`play`, `queue`, `stop`,
`blendTo(clip, ticks)`, `getClip`, `getClips`) and `var Rig = Java.type('noppes.npcs.rework.rig.RigApi')`
(`setModel`, `setTexture`, `setScale`, `setClips`, `bind(role, clip)`, `applyPackage`, `models`).

New clips with preview pictures (Python 3 with numpy, pyyaml, Pillow): named poses in
`rig_src/<model>/poses.yaml`, a clip in `rig_src/<model>/<clip>.anim.yaml` (beats of poses, easing,
events), `py -3 tools/rig/anim/preview.py clip <model> <clip>` draws it with the hit areas,
`py -3 tools/rig/anim/compile.py <model>` writes the JSON. Copy a clip from the result into an
NPC's `Clips` (or a package), or rebuild to ship it built in.

### Creature folders (M15, `RwAnimCore=1`)

Put a directory such as `customnpcs/creatures/epic_troll/` on the **server** beside `rigs/`.
Use a lowercase directory ID with letters, digits or underscores.

| File | Contents |
|---|---|
| `creature.json` | For a variant, for example `{"base":"troll","name":"Epic Troll"}`. For an original model, omit `base` and use a new directory ID. Supports the `name`, `scale`, skin and idle/walk/hurt/death settings of a rig package. |
| `animations.json` | Bedrock clips under `animations`, including timed events. Reuse a built-in clip name to override it or give the new attack its own name for a script or `/rwrig play`. |
| `model.geo.json`, `texture.png` | Required for an original model. Use the rig renderer's per-face UV format and a PNG up to 1024×1024. |
| `clone.json` | Optional CustomNPCs server clone as a starting point for stats, AI, inventory and other NPC settings. |
| `script.js` | Optional server-side script. Use the existing `Animators` API to pick attacks. A script supplied with a pack replaces the clone's script tabs on spawn. |

The server loads packs at startup. In a running world use **`/rwcreature deploy epic_troll [x y z]`**
after copying files to reload the directory and place the NPC in one command. Use
**`/rwcreature reload`** to refresh the files without placing an NPC. A bad file refuses the live
reload and leaves the current packs active. The server
sends the model, PNG and clips to clients with the switch enabled, including players who join later.
The NPC script stays on the server. `/rwcreature list` lists valid packs.

Use **`/rwcreature spawn epic_troll [x y z]`** to place a new NPC from the pack. Omit coordinates
to use your position. **Display → Rig model...** changes the model of an existing NPC. Spawn applies
the pack's NPC settings and script. Changes to the pack's model and clips take effect on reload,
including for existing NPCs. The files for any custom models used in the world must remain on the
server across restarts.

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

- **Client compatibility matrix passed in all four cells on a dev pair** (Q436): rework on both sides on
  an integrated and a dedicated server, an original client on a rework dedicated server, and a rework
  client on an original dedicated server. Login sync, dialog/quest clicks, trader, bank, skin download
  failure, resource reload, mail to an offline player and relogin all pass. This covers default settings
  only (`RwArmies=0`), one offline player and a few close NPCs, on a dev client and not a packaged
  client (Q22). Mixed pairs with features switched on were not run. An original client on a server at
  `RwArmies=1` was not run on the final code; Forge refused it for the staff entry in an earlier run,
  so expect a refusal. An original client on a dedicated server can miss the CustomNPCs login sync (an
  original bug, fixed only in the rework client; the harness asked for one extra sync there). Mail
  cannot be sent to a player who first joined after the server started until the next restart
  (original behaviour). Server-side original vs rework comparison passed only on a partial matrix
  (M10.1c, Q225).
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
- Armies (`RwArmies`, M11.1/M11.1b/M11.2/M11.3/M11.4/M11.8a/b, [note](meritve/2026-11-03-M11.1-register.md),
  [permissions](meritve/2026-11-03-M11.1b-dovoljenja.md), [clusters](meritve/2026-11-03-M11.2-grozdi-posnetek.md),
  [companies](meritve/2026-11-03-M11.3-cete.md), [swarm/stance](meritve/2026-11-03-M11.4-roj-drza.md)):
  short moves, passive swarm and combat return work for loaded ground members. An NPC without an army can be
  assigned only by an op or a script. Faction membership gives no rights. The full permissions matrix
  used stand-in players; staff deop/view/revoke was also tested
  with one real client. `members`, `list` and `info`
  count loaded NPCs only; after `remove`, NPCs in unloaded chunks keep the old name without effect.
  The register and permissions were tested on a dev dedicated server only. Clusters and the snapshot were tested
  with one real dev client on a dev dedicated server, not on integrated or packaged pairs. The
  [staff GUI](meritve/2026-11-03-M11.8a-palica.md) passed assignment, bulk assignment of 3, aiming,
  physical cluster movement/return, stop, current rights, restart and switch 0. Five PNGs were reviewed;
  a tutorial toast partly covers Refresh and long status text can be cut after two lines. Other resolutions,
  larger bulk groups, integrated/packaged staff pairs and difficult terrain were not tested. The
  [Permissions tab](meritve/2026-11-03-M11.8b-dovoljenja-gui.md) passed 101 checks with one real dev
  client and dev dedicated server, an offline grantee and five test cache profiles; no simultaneous
  grantors or integrated/packaged pair. Five PNGs were reviewed; tutorial partly covers the top row.
  Replies show three lines with wheel scrolling and are capped at 2000 characters (truncation marked).
  Multiple selected armies are not an atomic transaction. The ray's
  vanilla step limit may shorten its reach. The [bridge API v1](api/ARMY-BRIDGE-V1.md) is frozen
  (M11.7a): reflection contract, build and package passed, including 10 new unit tests. This package
  now has a Xaero adapter v0.1.9 (M11.7b): 83/83 unit/regression tests, build and reobf package passed,
  installed in six existing mods folders with old jars backed up. Live map, networked rights, ground resolution,
  online/offline and wrong-thread calls and production reflection remain untested. Since v0.1.11 a map target may be in an unloaded chunk (loaded or generated
  for the click, nearest safe surface within 8 blocks; not runtime-tested from the map); no cave/path safety guarantee, at most 256 markers/16 selected armies.
  After a restart the time of the last position can be older than the last time the army was seen;
  the position itself is kept.
  Movement was tested with 32 NPCs in four companies of 8, legion on flat ground, on a dev dedicated
  server without a client, including a restart mid-march. Column/ring have unit geometry checks only;
  larger companies and manual `/rwsquad` takeover were not tested. M11.4 proved one swarm of 8 after restart,
  six active passive samples, one real melee kill and return of all 8 to a legion company on flat ground,
  dev dedicated only. The combat target was assigned by the harness; natural enemy acquisition, multiple
  swarm companies and ranged/mounted/flying combat were not tested. Macro offsets keep a constant
  direction around turns. At most 2048 loaded members; no complete path means BLOCKED.
  [M11.5 long marches](meritve/2026-11-03-M11.5-dolg-pohod.md) now pass: 32 ground members in four companies
  walk at least 238 blocks to a 240-block goal with a restart, `NpcNavRange=32` and forced chunks.
  Legs keep company membership, offsets and direction; only the final leg anchors home. Each leg requests
  a real path within `min(24, NpcNavRange-8, FOLLOW_RANGE-8)` Manhattan blocks. A blocked intermediate point
  or a leg that does not reduce distance stops the march, even if a larger detour exists. This is not a global
  route planner. Long routes with 200, difficult terrain or multiple swarm companies remain untested.
  M11.9 edge cases passed on a dev dedicated server without a player, with forced chunks and one
  company of 8 per army on a short route: shallow water and a two-block throat arrive; a wide gap blocks without a
  fall; two deaths and a departure during a march leave five who arrive and hold; a world reload resumes the saved
  order. With companies, one member closed in stops the whole army as BLOCKED even when the other seven are at the goal.
  [M11.11 `freeforall`](meritve/2026-11-11-M11.11-freeforall.md) removes that dependency: 199 scattered members crossed
  a wall with one 3-wide gap, a stepped hill, pillars and a pond to their own legion slots after a restart, while the
  walled-in member alone was reported blocked. Dev dedicated without a client, flat world with a built course and forced
  chunks; unloaded terrain, combat during the march, column/ring at the goal and the live map were not tested. It costs
  more than companies: march p95 15.2 ms against idle 7.1 ms with 200 (companies 10.75 against 6.42, another day).
  The route is still searched in bounded legs (turned up to ±90° when a leg fails); it is not a global planner.
  A far march passed too: 100 members walked to a target 600 blocks away in never generated flat terrain with no
  player, held only by the army's own chunk tickets (`RwArmyMaxMarches`, a free-for-all army may hold up to 400
  chunks); all 100 took their slot. Natural terrain far away and a restart during such a march were not tested.
  `ArmyApi.members` can return a dying member for up to 20 ticks, so scripts should check health. M11.6 travelling chunk tickets (`RwArmyMaxMarches`, accepted under the
  switch, Q355): at most that many armies hold tickets (members' chunks plus one ring, at most 49 chunks); further
  armies march without tickets and get no progress in unloaded chunks. M11.5 clears a stale member path before
  each new anchor request; the strict ticket regression now requires and proves ARRIVED after both unload
  and restart on flat ground (8/2/2 members, no player or diagnostic tickets). This separate short scene does
  not prove a 200-member long route through unloaded chunks. Tickets left by a crash
  are released only at the next world load; members more than about ±32 blocks from the others may be left
  behind. Tested only on a dev dedicated server on flat ground; no separate ticket cost was visible in M11.10. Rebuilding companies can change membership.
  [M11.10 cost](meritve/2026-11-03-M11.10-cena.md), one repetition on a dev dedicated server on flat ground: one army
  of 200 (25 companies) marching with new 28-block orders every 20 s costs server MSPT p95 10.75 ms and 72.7 MB/s
  allocation, against 6.42 ms and 50.4 MB/s for the same 200 idle NPCs (+67 % p95, +44 % allocation); no tick
  over 50 ms. With only M11.6 tickets loading the world: p95 9.70 ms. With a real client commanding 200 through the
  staff: server p95 10.22 ms and the army arrived. Combat, terrain, 50/500 and several armies were not measured.
  With M11.5, the final walkable metre uses the same move helper to reach the unchanged arrival radius;
  ordinary `/rwsquad` keeps its old behaviour. The 200-member short final leg now ARRIVED, with all 25 companies
  settled in the final log. New paired cost data (one repetition, same fixture and 60+180 s window): idle p95
  6.685 ms / 48.2 MB/s, march 10.748 ms / 74.1 MB/s, no tick over 50 ms. The active-company sample in the cost
  JSON is incomplete and is not a final settling check; the final company summaries are the evidence.
  The live Xaero map with an army must be checked by hand in a real modpack client.
- **Linked NPCs (`RwLinkedOwnFields`, off by default):** with 0 the original behaviour stays: every member takes the
  orientation and path index of the NPC edited last, and a member with a path gets the first point of its own path
  as its home, so it can walk there. With 1 a member with a path keeps these
  ([M13.1](meritve/2026-11-04-M13.1-povezani.md)), also after a world reload. Turning the switch on does not restore
  values that were already overwritten. Tested on a dev dedicated server without a player or the GUI;
  a chunk unloading while the server runs, rotation and `RwFlightMode` were not tested in the world.
  A member without a path keeps its home with 0 too, but takes the orientation.
- **Linked NPC groups (`RwLinkedGroups`, off by default):** with 1 a group passes on only its selected categories, and
  scripts are a new category, off by default ([M13.2](meritve/2026-11-04-M13.2-kategorije.md)). Tested with `/rwlinked` and script calls on a dev dedicated
  server: three members, then 200 for the budget. `advanced` and `transform` were checked only by key lists, scripts only in JS. Undo has one level.
  With `RwLinkedOwnFields=0`, linking still moves the home of a member with a path, as in the original; turn both on to
  keep it. Saving after the `scripts` category is turned off removes the scripts from the group, members keep theirs.
  Going from 1 back to 0 in the same world was not tested. Budget: 200 members got a change in 80 ticks at 50 per tick,
  about 91 µs each; server MSPT and client packets were not measured, and transfers when a chunk loads are not limited.
  The *Global → Linked NPCs* boxes and undo button ([M13.2b](meritve/2026-11-04-M13.2b-gui.md)) were tested with one real dev client, a dev
  dedicated server and two members; packaged and integrated pairs and several editors at once were not. With a server at 0
  the screen stays the original; a client at 0 with a server at 1 was not run. The server checks rights on every request.
  M13.3 fixes the stale display after rights are removed: a denied reply clears the previous group's boxes and loaded count.
- **Wand multiple selection ([M13.3](meritve/2026-11-04-M13.3-palica.md), `RwLinkedGroups`, off by default):**
  tested with one real dev client, a dev dedicated server and six NPCs: filter, same-name radius, selection, appearance copy,
  link, delete, reset, primary edit, rights removed, both sides at 0 and client 1/server 0. At most 1000 nearest loaded NPCs
  in the player's dimension are listed; group counts include only loaded members. Long footer text is shortened.
  Shift/Ctrl states were inserted by the harness; physical modifier keys, all copy categories, packaged/integrated
  pairs and simultaneous editors were not tested. Server MSPT, allocation and packet cost were not measured.
  The original tutorial overlay can cover the top right button. The original list can merge same-name rows at the same distance;
  the new list uses entity IDs.
- **Cloner "place as linked" and update message ([M13.4](meritve/2026-11-04-M13.4-kloner.md), `RwLinkedGroups`, off by default):**
  tested with one real dev client, a dev dedicated server and one group of three members: new and existing group from a
  local and a server clone, a member in a truly unloaded chunk (no forced chunks) that updates when loaded and after a
  restart while keeping its own home, orientation and path, rights removed with the cloner open, both sides at 0 and
  client 1/server 0. The message counts only loaded members. Packaged/integrated pairs, simultaneous editors and large
  groups were not tested; MSPT was not measured (one transfer about 0.6 ms as data).
- **100 linked NPCs ([M13.5](meritve/2026-11-04-M13.5-sto.md), `RwLinkedGroups`, off by default):** with one real dev client and a dev dedicated
  server, 100 placed NPCs were selected with *Same name*, linked with one action and all received an edit of one member;
  both sides at 0 kept the original list. All 100 were loaded and within the 32 block radius, with `RwLinkedOwnFields=0`.
  The M13 exit rests on separate scenarios: unloaded members, categories, undo and own fields were tested with a few members,
  not with 100. Packaged/integrated pairs and physical Shift/Ctrl were not tested; MSPT and packets were not measured.
  Saving one group rewrites the files of all groups (original behaviour).
- **Original bug, not fixed:** removing a Linked NPC group does not delete its file, so the group comes back after a
  restart (from the code, not run).

The full list of behaviour changes is in the [changelog](CHANGELOG.md) and in
[04-REGISTRI.md](04-REGISTRI.md#sprejete-spremembe-obnašanja).
