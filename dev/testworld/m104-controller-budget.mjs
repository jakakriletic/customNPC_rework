// Controller-only deterministic checks; imports do not run worker.main or child processes.
import assert from 'node:assert/strict';
import { operationTimeoutSeconds } from '../../overnight/worker.mjs';
assert.equal(operationTimeoutSeconds({ type: 'test', timeoutSeconds: 120 }), 120);
assert.equal(operationTimeoutSeconds({ type: 'agent', timeoutSeconds: 1200 }), 1200);
const good = { type: 'test', timeoutSeconds: 14400, test: { script: 'C:\\repo\\m104-soak-run.ps1', parameters: { SetupAllowanceSeconds: 1800 } } };
assert.equal(operationTimeoutSeconds(good), 16200);
for (const change of [
  { type: 'agent' }, { timeoutSeconds: 120 },
  { test: { script: 'another.ps1', parameters: { SetupAllowanceSeconds: 1800 } } },
  ...[0, 1801, '1800', null].map(v => ({ test: { script: good.test.script, parameters: { SetupAllowanceSeconds: v } } }))
]) assert.throws(() => operationTimeoutSeconds({ ...good, ...change }), /reserved/);
console.log('M104 controller budget PASS checks=10');
