# Armies on Xaero's World Map — v0.1.9

Install the same `xaerofleetcommand-1.12.2-v0.1.9.jar` on client and server.
The client needs Xaero's World Map; the server needs CustomNPC Rework with
`RwArmies=1` and formations enabled. This module has no required CNPC/DaVinci
dependency: without either provider its other features continue to work.
Winter storm from v0.1.8-winter is included, also on installations upgraded from v0.1.7.

Create and assign armies with the command staff or `/rwarmy`. When a visible army
has a position in your current dimension, open Xaero's world map and choose
**Armies** (or cycle tabs with Tab). Flags show cluster member counts; hollow flags
show last known positions. Hover for army/owner, kind, loaded count, state and stance.
The army count is loaded members, not a saved total. The layer sends at most 256 markers.

- Click a flag to select its whole army; Shift-click adds/removes. Double click
  selects armies of the same owner. Ctrl+drag selects a rectangle; Ctrl+A all.
- Choose Legion, Column, Ring or Swarm, speed 0.5–6, and the stance for new orders.
  Right click a destination to move. It must be safe, already loaded ground in
  your current dimension; unloaded terrain, water/lava and hazardous surfaces are refused.
- **Apply stance** changes existing orders; **Stop** or X cancels them. Escape
  clears selection, Space centres it. Ctrl+1–9 saves a selection; 1–9 recalls it.
- Up to 16 armies can be selected. Each receives an independent server result.
  A move being accepted means saved intent, not arrival. The line shows the last
  requested target only. March distance is still limited by `NpcNavRange`;
  restarting an existing march can leave it blocked (CNPC M11.5/M11.6 limits).

Only granted armies appear; view-only flags cannot be commanded. The server uses
your connected identity and checks rights again for every action. A stale selection
does not retain authority after revoke/deop. No CNPC or no visible positions means
no army controls. The existing World, Ship's map and Winter storm tabs remain.

New army packets require FML version `0.6.0-armies` at the other end. A new client
can still use fleet/winter on old servers, but not armies; update both ends to use
armies. Artifact version and FML wire version are intentionally distinct, following
the existing v0.1.8-winter convention.

M11.7b validation is controller test/build/package. Live map, actual networked rights,
surface resolution and production reflection still require a manual client check;
no such runtime check is claimed by the build report. Price is measured in M11.10.
