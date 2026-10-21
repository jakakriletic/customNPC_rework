import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import http from 'node:http';
import { fileURLToPath } from 'node:url';
import { decision, agentArguments, fingerprint, readJson, writeJson, loadConfig, alive } from '../lib.mjs';
import { newState, makeTask, agentPrompt, applyAgentResult, selectProfile, tick } from '../runner.mjs';
import { execute } from '../worker.mjs';
import { publish, importEvents, interrupted } from '../control.mjs';
import { createDashboard } from '../dashboard.mjs';

const fixtures = fileURLToPath(new URL('./process.mjs', import.meta.url));
const plan = (action, extra = {}) => ({ action, summary: 'Evidence-based result', handoff: 'Next step', testIds: [], questions: [], testDefinitions: [], verification: 'tests', evidence: [], ...extra });
function temporary(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'cnpc-autonomy-'));
  t.after(() => { assert.equal(path.dirname(root), fs.realpathSync(os.tmpdir())); fs.rmSync(root, { recursive: true, force: true }); }); return root;
}
function config(t) {
  const projectRoot = temporary(t); fs.writeFileSync(path.join(projectRoot, 'input.txt'), 'stable');
  return { version: 1, projectRoot, stateDirectory: path.join(projectRoot, '.overnight'), tests: {}, profiles: [], fingerprintPaths: ['input.txt'], autonomy: { enabled: true, fullAccess: true }, minimumIdleSeconds: 120, maximumCpuPercent: 20, maximumIterations: 6, maximumTestRetries: 2, unknownQuotaRetryMinutes: 15, agentTimeoutSeconds: 3600 };
}
async function until(fn, timeout = 15000) { const deadline = Date.now() + timeout; while (Date.now() < deadline) { if (await fn()) return; await new Promise(r => setTimeout(r, 100)); } throw new Error('Condition timed out'); }
test('autonomous native CLI arguments remove approval gates only for this invocation', () => {
  const codex = agentArguments({ kind: 'codex' }, 'result.json', { fullAccess: true });
  assert.ok(codex.includes('--dangerously-bypass-approvals-and-sandbox')); assert.ok(!codex.includes('--approve-for-me'));
  const claude = agentArguments({ kind: 'claude' }, 'result.json', { fullAccess: true });
  assert.ok(claude.includes('--dangerously-skip-permissions')); assert.ok(!claude.includes('--allowedTools')); assert.equal(claude.at(-1), 'none');
  assert.ok(!agentArguments({ kind: 'codex' }, 'result.json').includes('--dangerously-bypass-approvals-and-sandbox'));
});
test('unattended prompt explicitly authorizes decisions and preserves measurement ownership', t => {
  const c = config(t); const prompt = agentPrompt(c, makeTask('Plan', 'next project package'));
  assert.match(prompt, /Do not ask the user questions/); assert.match(prompt, /answer it yourself/); assert.match(prompt, /Do NOT launch Minecraft/); assert.match(prompt, /Never claim unperformed validation/);
});
test('no fixed repair iteration cap applies in authorized autonomous mode', t => {
  const c = config(t); c.tests.unit = {}; const task = makeTask('repair', 'goal'); task.iteration = 100;
  const s = newState(); s.active = { profileId: 'codex' };
  applyAgentResult(c, s, task, { kind: 'success', decision: plan('test', { testIds: ['unit'], questions: [{ question: 'A or B?', decision: 'B', rationale: 'Measured evidence' }] }) }, 'v1');
  assert.equal(task.phase, 'tests'); assert.notEqual(task.status, 'blocked'); assert.equal(s.questions[0].decision, 'B');
});
test('new scenarios are literal, guarded, persisted and cannot replace acceptance tests', t => {
  const c = config(t); fs.writeFileSync(path.join(c.projectRoot, 'scenario.ps1'), 'Write-Output PASS');
  const item = { id: 'new-client', script: 'scenario.ps1', parametersJson: '{"Args":["a b","$(literal)"]}', desktop: false, timeoutSeconds: 60 };
  const d = decision(plan('test', { testIds: ['new-client'], testDefinitions: [item] }), c);
  assert.equal(d.definitions['new-client'].desktop, true); assert.deepEqual(d.definitions['new-client'].parameters.Args, ['a b', '$(literal)']);
  const s = newState(); s.active = { profileId: 'codex' }; const task = makeTask('scenario', 'goal');
  applyAgentResult(c, s, task, { kind: 'success', decision: plan('test', { testIds: ['new-client'], testDefinitions: [item] }) }, fingerprint(c));
  assert.ok(s.testCatalogue['new-client']); const first = fingerprint(c);
  fs.appendFileSync(path.join(c.projectRoot, 'scenario.ps1'), '\nWrite-Output CHANGED'); assert.notEqual(fingerprint(c), first);
  assert.throws(() => decision(plan('test', { testIds: ['new-client'], testDefinitions: [item] }), c));
  assert.throws(() => decision(plan('test', { testIds: ['outside'], testDefinitions: [{ ...item, id: 'outside', script: '../outside.ps1' }] }), c));
});
test('research completion requires actual documents and unchanged source with no pending tests', t => {
  const c = config(t); fs.writeFileSync(path.join(c.projectRoot, 'research.md'), '# Findings');
  const run = (changed, required, evidence) => { const task = makeTask('research', 'goal', required); const s = newState(); s.active = { profileId: 'codex', sourceFingerprint: 'v1' }; applyAgentResult(c, s, task, { kind: 'success', decision: plan('done', { verification: 'research', evidence }) }, changed ? 'v2' : 'v1'); return task; };
  assert.equal(run(false, [], ['research.md']).status, 'done');
  for (const task of [run(true, [], ['research.md']), run(false, ['unit'], ['research.md']), run(false, [], ['missing.md'])]) assert.equal(task.status, 'blocked');
});
test('continuous controller selects follow-up work and waits when every account is unavailable', async t => {
  const c = config(t); const s = newState(); s.continuous = true;
  s.tasks.push({ ...makeTask('previous', 'goal'), status: 'done' });
  await tick(c, s); assert.equal(s.tasks.length, 2); assert.equal(s.tasks[1].status, 'queued'); assert.match(s.message, /prijavo/);
  await tick(c, s); assert.equal(s.tasks.length, 2);
});
test('idle decision waits without busy-looping or falsely completing unfinished code', async t => {
  const c = config(t); const s = newState(); s.continuous = true; s.active = { profileId: 'codex', sourceFingerprint: 'v1' };
  const task = makeTask('plan', 'goal'); s.tasks.push(task);
  applyAgentResult(c, s, task, { kind: 'success', decision: plan('idle') }, 'v1'); s.active = null;
  assert.equal(task.status, 'waiting'); await tick(c, s, { fingerprint: () => 'v1' }); assert.equal(s.tasks.length, 1); assert.equal(task.status, 'waiting');
  await tick(c, s, { fingerprint: () => 'v2' }); assert.equal(task.status, 'queued');
  const changed = makeTask('unfinished', 'goal'); s.active = { profileId: 'codex', sourceFingerprint: 'v1' };
  applyAgentResult(c, s, changed, { kind: 'success', decision: plan('idle') }, 'v2'); assert.notEqual(changed.status, 'waiting');
});
test('login events can enable a second profile while controller owns the state', t => {
  const c = config(t); c.profiles = [{ id: 'claude-2', enabled: false, executable: process.execPath }];
  const s = newState(); s.profiles['claude-2'] = { disabled: true };
  publish(c, { type: 'login', id: 'claude-2' }); importEvents(c, s);
  assert.equal(selectProfile(c, s).profile.id, 'claude-2');
  s.active = { profileId: 'claude-2' }; const task = makeTask('quota', 'goal');
  applyAgentResult(c, s, task, { kind: 'quota', until: '2099-01-01T00:00:00Z', exact: true, message: 'limit' }, 'v1'); assert.equal(s.profiles['claude-2'].enabledOverride, true);
  publish(c, { type: 'verified', id: 'claude-2' }); importEvents(c, s);
  assert.equal(s.profiles['claude-2'].until, '2099-01-01T00:00:00Z'); assert.equal(selectProfile(c, s).profile, undefined);
});
test('interrupt preserves previous test evidence and resumes the incomplete cell', () => {
  const s = newState(); const task = makeTask('client', 'goal', ['first', 'second']); task.testIndex = 1; task.results.push({ testId: 'first', success: true }); s.tasks.push(task); s.active = { taskId: task.id, type: 'test' };
  interrupted(s, {}); assert.equal(task.testIndex, 1); assert.equal(task.results.length, 1); assert.equal(task.status, 'queued');
});
test('cancel terminates the owned process tree and leaves an unrelated process alive', async t => {
  const c = config(t); fs.mkdirSync(c.stateDirectory); const pids = path.join(c.projectRoot, 'pids.json');
  const unrelated = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { windowsHide: true, stdio: 'ignore' });
  try {
    const work = execute({ type: 'test', command: process.execPath, arguments: [fixtures, 'tree', pids], cwd: c.projectRoot, directory: c.stateDirectory, timeoutSeconds: 30, test: {}, sourceFingerprint: fingerprint(c), fingerprintConfig: c });
    await until(() => fs.existsSync(pids)); const ids = readJson(pids);
    fs.writeFileSync(path.join(c.stateDirectory, 'CANCEL'), 'stop'); const result = await work;
    assert.equal(result.kind, 'cancelled'); await until(() => !alive(ids.parent) && !alive(ids.child)); assert.equal(alive(unrelated.pid), true);
  } finally { unrelated.kill(); await new Promise(r => unrelated.once('close', r)); }
});
test('AI job stops an orphaned descendant after its parent already exited', { skip: process.platform !== 'win32' }, async t => {
  const root = temporary(t); const pids = path.join(root, 'pids.json');
  const fixture = spawn(process.execPath, [fixtures, 'orphan', pids], { windowsHide: true, stdio: 'ignore' });
  await new Promise(r => fixture.once('close', r)); const ids = readJson(pids);
  try { assert.ok(ids.orphan > 0); assert.equal(alive(ids.middle), false); await until(() => !alive(ids.orphan)); }
  finally { if (alive(ids.orphan)) process.kill(ids.orphan); }
});
test('local dashboard rejects cross-origin requests and controls a real synthetic run', async t => {
  const c = config(t); fs.writeFileSync(path.join(c.projectRoot, 'scenario.ps1'), "Write-Output 'synthetic running'\nStart-Sleep -Seconds 30\n");
  c.tests.synthetic = { script: path.join(c.projectRoot, 'scenario.ps1'), parameters: {}, timeoutSeconds: 60 };
  const configFile = path.join(c.projectRoot, 'config.json'); writeJson(configFile, c);
  const state = newState(); state.tasks.push(makeTask('Synthetic only', 'No AI or Minecraft', ['synthetic'])); writeJson(path.join(c.stateDirectory, 'state.json'), state);
  const app = await createDashboard(loadConfig(configFile), configFile);
  const html = await (await fetch(app.url)).text(); const token = html.match(/name="overnight-token" content="([a-f0-9]+)"/)[1];
  const headers = { 'X-Overnight-Token': token, Origin: app.url, 'Content-Type': 'application/json' };
  const get = async () => (await fetch(`${app.url}/api/status`, { headers })).json();
  const post = async route => { const response = await fetch(`${app.url}/api/${route}`, { method: 'POST', headers, body: '{}' }); const v = await response.json(); assert.equal(response.status, 200, JSON.stringify(v)); return v; };
  try {
    assert.equal((await fetch(`${app.url}/api/status`)).status, 403);
    assert.equal((await fetch(`${app.url}/api/start`, { method: 'POST', headers: { ...headers, Origin: 'http://evil.example' }, body: '{}' })).status, 403);
    const hostStatus = await new Promise((resolve, reject) => { const req = http.get(`${app.url}/api/status`, { headers: { ...headers, Host: 'evil.example' } }, res => { res.resume(); resolve(res.statusCode); }); req.on('error', reject); });
    assert.equal(hostStatus, 403);
    await post('start'); await post('start'); await until(async () => { const s = await get(); return s.active && fs.existsSync(path.join(s.active.directory, 'stdout.log')) && fs.readFileSync(path.join(s.active.directory, 'stdout.log'), 'utf8').includes('synthetic running'); });
    assert.equal((await fetch(`${app.url}/api/probe`, { method: 'POST', headers, body: '{}' })).status, 400);
    await post('pause'); assert.equal((await get()).paused, true); await post('resume'); assert.equal((await get()).paused, false);
    await post('stop'); await until(async () => !(await get()).controller);
    const stopped = await get(); assert.equal(stopped.active, null); assert.equal(stopped.tasks[0].status, 'queued'); assert.equal(stopped.tasks[0].results.length, 0);
    // A fresh Start resumes the same incomplete cell; Stop remains effective.
    await post('start'); await until(async () => Boolean((await get()).active)); await post('stop'); await until(async () => !(await get()).controller);
    assert.equal((await get()).tasks.length, 1);
  } finally {
    fs.writeFileSync(path.join(c.stateDirectory, 'STOP'), 'cleanup');
    await until(async () => !(await get()).controller); await app.close();
  }
});
