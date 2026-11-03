# Xaero army bridge source snapshot (M11.7b)

The build input is `../advantura_estetika_mod/xaero-winter-storm`, relative to the
repository root. This directory records
the complete main/test/QA source tree and build configuration of the user-owned,
non-git Winter storm project so the controller fingerprint covers its changes.
`inputs.json` also freezes wrapper and dependency binary hashes; those binaries
remain in the external project and its backup. No DaVinci or CNPC code is embedded.

Controller test: `../../xaero-army-bridge-run.ps1`. It checks both copies before
and after `gradlew.bat test build --no-daemon --stacktrace`, reads the JUnit reports,
checks the production jar, and records the external input manifest and artifact hash.
The agent does not run the build or start Minecraft. Acceptance and deployment are
pending controller results; this snapshot alone is not runtime proof.

The controller's first invocation stopped before compilation in RFG 1.4.9's
legacy Fernflower download predicate. Its shaded Download task throws in offline
mode when that unused output is absent, before RFG skips it for Minecraft 1.12.2.
Normal dependency resolution is permitted for the corrected `m117b-bridge-v2`
scenario; plugin, wrapper, local libraries and source hashes remain unchanged.
No acceptance assertions or test selection were removed.

The second invocation compiled the sources and ran 83 JUnit tests: 82 passed.
One malformed-packet fixture attempted to append a byte to a fixed-capacity buffer
before calling the codec. It now allocates room for that same trailing byte;
the rejection assertion is unchanged. Only this test and its manifest hash changed.
`m117b-bridge-v3` requests completion of the same acceptance scenario, including
the production build/package that did not complete after the failed test task.

Version: artifact `1.12.2-v0.1.9`, FML/wire `0.6.0-armies`. This preserves the full
v0.1.8-winter feature set and adds a fourth Armies tab when visible CNPC markers
are available. Old fleet/winter packet IDs 0–9 remain; new army IDs are 10–13.
The client sends those only to a server advertising the new version.

The tab uses the frozen [Army bridge API](../../docs/api/ARMY-BRIDGE-V1.md).
Click/Shift-click, double click by owner, Ctrl+drag, Ctrl+A, groups Ctrl+1–9/1–9,
Space and right click mirror the fleet gestures, with independent selection.
Shape/speed/stance are explicit; X stops. Flags show each cluster's loaded count;
tooltips show the whole army's loaded count, kind, state, omitted counts and last seen.
The line is the last requested destination, not a server-confirmed route or arrival.

Requests are bounded (256 markers, 16 armies, strings up to 256 UTF-8 bytes),
rate-limited before scheduling and handled on the server thread using the connected
player UUID. Each operation rechecks permissions in CNPC. Missing/incompatible/off
CNPC returns no markers; army controls disappear without affecting ships or winter.
Targets must be in the player's current dimension, in a loaded chunk, on a solid
surface with two clear blocks. Water, lava, fire, cactus, magma and unloaded ground
are refused; this version does not generate terrain or infer a cave floor.

Backups of both projects are in `.overnight/backup-xaero-20261103-m117b/` at the CNPC
repository root. No git operation is allowed in `ladja_mod`. Do not touch pirate-mca.
Final source sync, release copies and replacement of installed jars follow Q346
only after verification. Keep archives and old installed binaries intact in backups.
