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
- Keep state/docs current; commit only your package and push without force. Native CLI
  permissions are configured per process by the controller, not by changing global settings.
