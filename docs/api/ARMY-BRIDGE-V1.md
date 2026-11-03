# Army bridge API v1 (M11.7a)

This is the frozen reflection contract for optional map integrations. It has no
Xaero dependency. M11.7b supplies the required Xaero adapter (Q346); this document
does not claim that adapter is installed or tested.

## Discovery and calls

Load `noppes.npcs.rework.army.ArmyCommandService` with the mod classloader and call
the public **static** methods below (`Method.invoke(null, ...)`). No singleton or
CNPC constructor is needed. Require `getApiVersion() == 1`. Missing classes,
unsupported versions, missing methods, linkage failures, reflection failures or
unexpected result types mean integration unavailable: hide army controls and keep
the other map features working. Do not fall back to commands or the trusted script API.

```java
int getApiVersion()
List<ArmyMapMarker> armiesVisibleTo(MinecraftServer server, UUID player, int dimension)
Reply submitMoveOrder(MinecraftServer server, UUID player, String army, int dimension,
    double x, double y, double z, String shape, double speed, String stance)
Reply cancelOrder(MinecraftServer server, UUID player, String army)
Reply setOrderEngagement(MinecraftServer server, UUID player, String army, String stance)
```

The service class name and these signatures, including primitive types, are v1.
`MinecraftServer` is `net.minecraft.server.MinecraftServer`, `UUID` is `java.util.UUID`,
`List` is `java.util.List`. A bridge only compiles against Minecraft/Forge and JDK;
the return classes are reflected, never imported. Production Minecraft class names
in the bridge's own bytecode are handled by its normal Forge reobfuscation.
Additional internal actor-based and script methods are **not** the bridge contract.

Minimal discovery example:

```java
Class<?> service = Class.forName("noppes.npcs.rework.army.ArmyCommandService");
int version = (Integer) service.getMethod("getApiVersion").invoke(null);
if (version != 1) throw new IllegalStateException("unsupported army API");
Method visible = service.getMethod("armiesVisibleTo",
    MinecraftServer.class, UUID.class, int.class);
List<?> markers = (List<?>) visible.invoke(null, server, sender.getUniqueID(), dimension);
```

## Authority, scheduling and transport

- Call on the **server thread**, after scheduling the authenticated connection's
  request with `server.addScheduledTask`. Use its current server and player UUID;
  never accept a UUID, operator flag or `ArmyActor` supplied in a client payload.
- The bridge methods require an online player who has not disconnected. Missing
  identity/server, offline players or the wrong thread return empty markers / a
  refused reply without reading the register or changing orders. They do not queue
  work themselves. With `RwArmies=0` they return empty/refused before touching game state;
  the API version can still be discovered. Its presence alone does not mean enabled.
- Each call resolves the player's current operator status and current saved grants.
  View includes only visible armies; command includes view. Command is needed for
  move, cancel and engagement. Hidden and nonexistent names receive the same reply.
  Revocation or deop affects the next call; it does not cancel an existing order.
- These are trusted **in-process** calls, not a network protocol. The map adapter owns
  bounded request/response codecs, authenticated sender resolution, rate limiting
  and server-thread scheduling. It must bound selections and requests before
  invoking the service. Do not expose actor/SYSTEM methods to clients.
  The existing CNPC `rwarmy` channel and its staff-only actions are unchanged.
- One army per action. A bridge's multiple selection is a series of independent
  results, not an atomic transaction. A successful move means intent was saved,
  **not arrival**. Cancel stops the existing order; it does not delete the army.

## Marker values

`noppes.npcs.rework.army.ArmyMapMarker` is final, with no public constructor or
mutable public fields. The returned list is unmodifiable. All getters are public,
no-argument instance methods; values are copies and remain unchanged after later
orders or permission changes. Refresh before acting; the server rechecks authority.

| Getter | Exact return type | Meaning |
|---|---|---|
| `getArmyName()` | `String` | Registered spelling, case-insensitive army identifier |
| `getOwnerName()` | `String` | Display label only, may be empty; never authority |
| `isCommandable()` | `boolean` | Command right at capture time |
| `getKind()` | `String` | `main`, `group`, `straggler`, or `last_known` |
| `getDimension()`, `getX()`, `getY()`, `getZ()` | `int` | Marker dimension and rounded cluster centre (block coordinates) |
| `getMemberCount()` | `int` | Loaded members of this cluster; 0 for last-known marker |
| `getLoadedCount()` | `int` | Loaded members of the whole army across dimensions, not a persistent total |
| `getOmittedClusters()`, `getOmittedMembers()` | `int` | Army-wide counts outside the existing 16-cluster cap |
| `getLastSeen()` | `long` | Overworld world time of the saved main position; -1 means unknown |
| `getOrderState()` | `String` | `NONE`, `WAITING`, `JOINING`, `MOVING`, `ARRIVED`, `BLOCKED`, `CANCELLED` |
| `getOrderMode()` | `String` | Empty without an order, otherwise `FORMATION` or `SWARM` |
| `getOrderShape()` | `String` | Empty without an order, otherwise `LEGIJA`, `KOLONA`, `OBRAMBA` |
| `getOrderEngagement()` | `String` | Empty without an order, otherwise `PASSIVE` or `ENGAGE` |

One army may have several markers. `getArmyName()` identifies the **whole army**
for orders; a cluster has no independent command identity. Filter by the requested
dimension without promoting a group/straggler to main. Only when the army has no
loaded clusters anywhere does a saved position become a `last_known` marker in its
saved dimension; an army with neither clusters nor saved position has no map marker.
Orphan NPC names and hidden armies are omitted. Last seen is not freshness of the
live cluster: it changes only with the saved-position policy (M11.2, 16 blocks or
dimension change), and may be older after restart.

The snapshot performs a loaded-entity scan and may update the saved last position;
poll only while the map needs it and at most once per second per player, as on the
existing CNPC snapshot channel. Copy values into the bridge's own bounded wire DTOs.
Do not sum `getLoadedCount()` across multiple markers of the same army.

## Actions and replies

Move accepts the same canonical shape words as `/rwarmy`: `legion`, `column`, `ring`,
`swarm` (the existing Slovenian aliases also work). Speed is finite in [0.5, 6.0].
Coordinates must be finite, |x| and |z| <= 29999984, and 0 <= y < 256; the target
dimension must be loaded. This API does **not** infer terrain height from x/z.
The adapter must resolve a safe ground y on the server or refuse the destination.
Loaded members must be eligible living ground NPCs in the target dimension, at most
2048, with formations enabled. Existing no-loaded-member orders may wait for tickets.
`stance` is `passive` or `engage` (case insensitive); null on move retains the previous
stance, initially engage. `setOrderEngagement` requires an existing order and explicit
stance. Internal enum names in marker values are not move input words.

The reply class is `noppes.npcs.rework.army.ArmyCommandService$Reply`:

| Getter | Exact return type | Meaning |
|---|---|---|
| `isAccepted()` | `boolean` | Action accepted/refused by the service |
| `getMessage()` | `String` | Human-readable diagnostic prefixed `RWARMY ` |

Never parse the message for authority or state. A null or malformed reflected reply
is a failed integration call. Existing public fields remain for internal callers.

## Compatibility and evidence limits

Changing/removing a listed signature, getter type or meaning requires a new API
version and an updated consumer. Additions may be ignored by a v1 consumer. Snapshot
wire version is independent and unchanged. `ArmyApi.getApiVersion()` also reports 1.

M11.7a acceptance is the reflection unit contract plus build/package, per M11.0 §5.3.
It covers off/no-world refusal, copied marker values, dimension/last-position handling,
rights-filtered snapshots and exact reflective discovery. It does not prove a live
external bridge, production reobfuscation at that boundary, or new movement behavior.
Those belong to M11.7b and its reported limits. March distance remains one path within
`NpcNavRange` (M11.5 deferred); restart resume may block (M11.6). `RwArmies` stays 0 by
default; cost is measured in M11.10.
