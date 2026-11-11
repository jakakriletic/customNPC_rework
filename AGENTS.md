# Shared repository workflow

- Use `origin` (`https://github.com/jakakriletic/customNPC_rework.git`) as the shared repository.
- After completing and verifying requested work, commit only the changes made for that task and push the current branch to `origin`.
- Preserve unrelated or pre-existing working-tree changes; never include them in a commit without explicit instruction.
- Before pushing, integrate remote changes safely. Never force-push or rewrite shared history unless explicitly requested.
- If a push is rejected or a merge/rebase conflict occurs, stop and resolve it carefully without discarding anyone's work.

## Unattended controller sessions

When the direct task prompt says `DIRECT OWNER INSTRUCTION` for the local `overnight/`
controller, the owner has authorized continuous autonomous development until Stop:

- Do not ask the owner engineering questions or wait for routine confirmations. Choose
  the best supported solution yourself, including previously open Q decisions. Record
  the question, answer and reason in the structured `questions` result and the project
  decision records. This authorization supersedes older instructions to ask a Q first.
- Work through the existing project plan one coherent package at a time. Inspect partial
  changes after interruption/failover. Preserve unrelated work, original binaries,
  environment locks, measurement conditions and acceptance criteria.
- Let the controller launch tests, builds and Minecraft. Register new scenario scripts
  with `testDefinitions` and request their IDs. Never run a parallel measurement.
- Missing real credentials, unavailable hardware/data or failed measurements remain real
  facts: document and defer that dependent package, then continue independent plan work.
  Never fabricate proof or mark a package complete just to keep the queue moving.
- Owner standing rule (Q90): never rerun a series that already passed on unchanged production
  code and harnesses, and request only the tests needed to accept the current code. No reruns of
  rejected earlier attempts, no extra "just in case" repetitions; controller-only changes do not
  require revalidation (see `docs/05-SEJA-PROTOKOL.md` §2 Meritve).
- Owner decision Q92: `docs/10-OBSEG-V1.md` defines the v1.0 scope and order and overrides
  `docs/03-FAZE.md`; packages outside it are v2 and are not started. Switches that stay
  default 0 have no cost gate (measure cost once as data); the attempt limit is removed by Q407;
  keep docs short (one
  measurement note + one 04-STANJE entry per package).
- Owner decision Q248: the v1.0 scope is done; next work is the ordered fix list P1–P8 in
  `docs/10-OBSEG-V1.md` ("Popravki omejitev v1.0"). Each fix updates `docs/UPORABNIKI.md` §6.
  P1–P8 are done.
- Owner decision Q295 (3. 11. 2026): v1.0 is accepted with all limits in `docs/UPORABNIKI.md` §6.
  Continue with v2, **M11 (armies and map command) first**, without waiting for its old entry
  conditions; see "Odločitev o izdaji in začetek v2" in `docs/10-OBSEG-V1.md`. Q90/Q92 still apply, as changed by Q407.
- Owner decision Q346: the Xaero map bridge (M11.7a/b, `../ladja_mod/milestones/xaero-fleet-command`)
  is required, not optional. Build on the newest source `../advantura_estetika_mod/xaero-winter-storm`
  (v0.1.8-winter), sync the result into `ladja_mod/milestones/xaero-fleet-command`, and install the
  new jar in every `mods/` folder that has `xaerofleetcommand` (old jar moved to `.overnight/` backup).
  No git on the owner's `ladja_mod` branch; never touch `mod/pirate-mca`. Details in `docs/10-OBSEG-V1.md`.
- Owner decision Q407 (4. 11. 2026) replaces the 2-attempt limit of Q92 and the Q250 time budget:
  no attempt limit and no per-package test budget. Diagnose, fix and retest until the feature works;
  defer only on a real external impossibility. Test as much as needed and where needed, no needless
  tests or repeats (Q90 stays). Only a hang guard remains (`timeoutSeconds` <= 14400). After M13.3,
  do M11.5 (long march beyond `NpcNavRange`) next, then continue M13, then fix limits in
  `docs/UPORABNIKI.md` §6 that were left only by the 2-attempt rule.
- Owner decision Q445 (4. 11. 2026): next package is M11.11, army move mode `freeforall` (each member
  goes from its own position at once, no companies, no waiting; also a Xaero button), before
  continuing M10.4 and the other §6 fixes. Details in `docs/03-FAZE.md` M11.11. **Done 11. 11. (Q446 hybrid:
  one shared route, no waiting); continue with M10.4.**
- Owner decision Q444 (4. 11. 2026): after the §6 fixes, do M16 (built-in animation core modelled on
  `../ladja_mod` clips/controls/`scripts/gusarji`, read-only) before M12, M14 and M15; new animations
  must be one data file for the AI, no Java or rebuild. Details in `docs/03-FAZE.md` M16.
- Keep state/docs current; commit only your package and push without force. Native CLI
  permissions are configured per process by the controller, not by changing global settings.
