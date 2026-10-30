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
  default 0 have no cost gate (measure cost once as data); at most 2 attempts per package,
  then accept under the switch with noted limits or defer to v2; keep docs short (one
  measurement note + one 04-STANJE entry per package).
- Owner decision Q248: the v1.0 scope is done; next work is the ordered fix list P1–P8 in
  `docs/10-OBSEG-V1.md` ("Popravki omejitev v1.0"). Each fix updates `docs/UPORABNIKI.md` §6.
  After P8 wait for the owner's release decision; do not start v2 packages.
- Owner time budget Q250: no test longer than 30 min (controller rejects `timeoutSeconds` > 1800),
  ~30 min of tests per package, 1 performance repetition (3 only when turning a switch on by
  default), one-pass baselines, soak at most 1 h (rule 5 in `docs/10-OBSEG-V1.md`).
- Keep state/docs current; commit only your package and push without force. Native CLI
  permissions are configured per process by the controller, not by changing global settings.
