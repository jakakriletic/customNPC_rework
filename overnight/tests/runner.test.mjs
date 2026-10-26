import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { writeJson, readJson, fingerprint, decision, classifyFailure, parseAgent, quotaReset, resetFromMessage, desktopReason, lock, nativeEnvironment, within, agentArguments, codexInstalled, executable } from '../lib.mjs';
import { newState, makeTask, applyAgentResult, applyTestResult, selectProfile, tick, importInbox } from '../runner.mjs';
import { execute } from '../worker.mjs';

const fixtures = fileURLToPath(new URL('./process.mjs', import.meta.url));
const worker = fileURLToPath(new URL('../worker.mjs', import.meta.url));
function temporary(t) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'cnpc-overnight-test-'));
  t.after(() => {
    // Every path is an absolute directory freshly created under the OS temp folder.
    assert.equal(path.dirname(directory), fs.realpathSync(os.tmpdir()));
    fs.rmSync(directory, { recursive: true, force: true });
  }); return directory;
}
const c = { tests: { unit: {}, client: {} }, maximumIterations: 6, maximumTestRetries: 2, minimumIdleSeconds: 120, maximumCpuPercent: 20 };
const plan = (action, testIds = []) => ({ action, testIds, summary: 'summary', handoff: 'next step' });
function activeState(profileId = 'claude-1') { return { ...newState(), active: { profileId } }; }

test('source and test parameter changes invalidate the same measurement identity', t => {
  const root = temporary(t); fs.writeFileSync(path.join(root, 'source.java'), 'original');
  const config = { projectRoot: root, fingerprintPaths: ['source.java'], tests: { unit: { mode: 0 } } };
  const first = fingerprint(config); assert.equal(fingerprint(config), first);
  config.tests.unit.mode = 1; assert.notEqual(fingerprint(config), first);
  config.tests.unit.mode = 0; fs.appendFileSync(path.join(root, 'source.java'), ' change'); assert.notEqual(fingerprint(config), first);
});
test('path escape is rejected', t => { const root = temporary(t); assert.throws(() => within(root, '../other.ps1')); });
test('JSON state replacement preserves Unicode and valid JSON', t => { const file = path.join(temporary(t), 'state.json'); writeJson(file, { value: 'miška' }); writeJson(file, { value: 'oči' }); assert.deepEqual(readJson(file), { value: 'oči' }); });
test('queued packages retain creation order rather than random UUID filename order', t => {
  const directory = temporary(t); const s = newState();
  const first = { ...makeTask('first', 'goal'), createdAt: '2026-10-20T01:00:00Z' };
  const second = { ...makeTask('second', 'goal'), createdAt: '2026-10-20T02:00:00Z' };
  writeJson(path.join(directory, 'inbox', 'z.json'), first); writeJson(path.join(directory, 'inbox', 'a.json'), second);
  importInbox({ stateDirectory: directory }, s); assert.deepEqual(s.tasks.map(v => v.title), ['first', 'second']); assert.equal(fs.readdirSync(path.join(directory, 'inbox')).length, 0);
});
test('a second controller cannot acquire a live lock', async t => { const root = temporary(t); const release = await lock(root); await assert.rejects(() => lock(root), /ze tece/); await release(); const next = await lock(root); await next(); });
test('unrecognized or empty test plans are rejected', () => {
  assert.throws(() => decision(plan('test', ['unknown']), c)); assert.throws(() => decision(plan('test'), c));
  assert.throws(() => decision({ ...plan('done'), command: 'arbitrary command' }, c));
});
test('a successful answer mentioning rate limits cannot trigger account rotation', t => {
  const final = path.join(temporary(t), 'decision.json'); writeJson(final, { ...plan('done'), summary: "We fixed the usage limit reached warning" });
  const result = parseAgent('codex', '{"type":"turn.completed"}', '', 0, final, 15);
  assert.equal(result.kind, 'success');
});
test('a recovered transient Codex error does not override a completed turn', t => {
  const final = path.join(temporary(t), 'decision.json'); writeJson(final, plan('test', ['unit']));
  const stdout = [JSON.stringify({ type: 'error', message: 'reconnecting' }), JSON.stringify({ type: 'turn.completed' })].join('\n');
  assert.equal(parseAgent('codex', stdout, '', 0, final, 15).kind, 'success');
});
test('structured Codex quota failure retains the provider reset time', () => {
  const stdout = JSON.stringify({ type: 'turn.failed', error: { code: 'usage_limit_reached', message: 'Limit', resets_at: '2099-01-01T00:00:00Z' } });
  const result = parseAgent('codex', stdout, '', 1, 'nonexistent-file', 15);
  assert.equal(result.kind, 'quota'); assert.equal(result.exact, true); assert.match(result.until, /^2099/);
});
test('Claude success envelope and quota envelope are distinguished', () => {
  assert.equal(parseAgent('claude', JSON.stringify({ type: 'result', is_error: false, structured_output: plan('test', ['unit']) }), '', 0, '', 15).kind, 'success');
  assert.equal(parseAgent('claude', JSON.stringify({ type: 'result', is_error: true, result: "You've hit your limit · resets 6pm" }), '', 0, '', 15).kind, 'quota');
});
test('an unknown reset is explicitly an estimate rather than an assumed 5 hours', () => {
  const time = Date.parse('2026-10-20T10:00:00Z'); const result = quotaReset({}, time, 15);
  assert.equal(result.exact, false); assert.equal(result.until, '2026-10-20T10:15:00.000Z');
});
test('Claude session limit envelope is quota with the stated reset time', () => {
  const stdout = JSON.stringify({ type: 'result', is_error: true, api_error_status: 429, result: "You've hit your session limit · resets 5:30pm (Europe/Ljubljana)" });
  const time = Date.parse('2026-10-21T15:15:26Z'); const result = classifyFailure([{ message: JSON.parse(stdout).result }], '', 1, 15, time);
  assert.equal(result.kind, 'quota'); assert.equal(result.exact, true); assert.equal(result.until, '2026-10-21T15:30:00.000Z');
  assert.equal(parseAgent('claude', stdout, '', 1, '', 15).kind, 'quota');
  assert.equal(parseAgent('claude', JSON.stringify({ type: 'result', is_error: true, api_error_status: 429, result: 'Overloaded' }), '', 1, '', 15).kind, 'quota');
});
test('stated reset times roll to the next day, month form and noon/midnight', () => {
  const time = Date.parse('2026-10-21T16:00:00Z');
  assert.equal(new Date(resetFromMessage('resets 5:30pm (Europe/Ljubljana)', time)).toISOString(), '2026-10-22T15:30:00.000Z');
  assert.equal(new Date(resetFromMessage('resets 12am (UTC)', time)).toISOString(), '2026-10-22T00:00:00.000Z');
  assert.equal(new Date(resetFromMessage('resets 12pm (UTC)', time)).toISOString(), '2026-10-22T12:00:00.000Z');
  assert.equal(new Date(resetFromMessage('weekly limit · resets Oct 23, 5pm (Europe/Ljubljana)', time)).toISOString(), '2026-10-23T15:00:00.000Z');
  assert.equal(resetFromMessage('resets 3:55pm (UTC)', time), time + 60000);
  assert.equal(resetFromMessage('resets 13pm (UTC)', time), null);
  assert.equal(resetFromMessage('resets 5pm (Not/AZone)', time), null);
  assert.equal(resetFromMessage('no reset here', time), null);
});
test('permission, tool-turn and generic failures are not treated as free quota', () => {
  for (const message of ['permission denied', 'max turns reached', 'test output includes HTTP 429']) assert.equal(classifyFailure([{ message }], '', 1, 15).kind, 'error');
});
test('quota pauses one profile, another profile is selected, and the first recovers', () => {
  const config = { ...c, profiles: [{ id: 'claude-1', enabled: true, executable: process.execPath }, { id: 'codex', enabled: true, executable: process.execPath }] };
  const s = activeState(); const task = makeTask('test', 'goal');
  applyAgentResult(config, s, task, { kind: 'quota', message: 'limit', until: '2099-01-01T00:00:00Z', exact: true }, 'v1');
  assert.equal(selectProfile(config, s).profile.id, 'codex');
  s.profiles.codex = { until: '2099-01-02T00:00:00Z' }; assert.equal(selectProfile(config, s).profile, undefined);
  assert.ok(selectProfile(config, s).nextAt > Date.now());
  assert.equal(selectProfile(config, s, Date.parse('2099-01-01T00:00:01Z')).profile.id, 'claude-1');
});
test('auth failures disable only their profile', () => {
  const s = activeState(); const task = makeTask('test', 'goal');
  applyAgentResult(c, s, task, { kind: 'auth', message: 'login required' }, 'v1');
  assert.equal(s.profiles['claude-1'].disabled, true); assert.equal(task.status, 'queued');
});
test('an AI cannot complete a task with stale or incomplete benchmark evidence', () => {
  const task = makeTask('K3', 'goal', ['unit', 'client']); task.phase = 'agent';
  task.results = [{ testId: 'unit', success: true, sourceFingerprint: 'v1' }, { testId: 'client', success: true, sourceFingerprint: 'old' }];
  applyAgentResult(c, activeState(), task, { kind: 'success', decision: plan('done') }, 'v1'); assert.equal(task.status, 'blocked');
});
test('requesting a smaller test batch does not erase required acceptance tests', () => {
  const task = makeTask('K3', 'goal', ['unit', 'client']);
  applyAgentResult(c, activeState(), task, { kind: 'success', decision: plan('test', ['unit']) }, 'v2');
  task.results = [{ testId: 'unit', success: true, sourceFingerprint: 'v2' }];
  applyAgentResult(c, activeState(), task, { kind: 'success', decision: plan('done') }, 'v2'); assert.equal(task.status, 'blocked');
});
test('a valid whole batch permits completion and retains handoff', () => {
  const task = makeTask('K3', 'goal', ['unit', 'client']);
  task.results = ['unit', 'client'].map(testId => ({ testId, success: true, sourceFingerprint: 'v1' }));
  applyAgentResult(c, activeState(), task, { kind: 'success', decision: plan('done') }, 'v1'); assert.equal(task.status, 'done'); assert.equal(task.handoff, 'next step');
});
test('iteration cap blocks repeated unsuccessful repair cycles', () => {
  const task = makeTask('K3', 'goal'); task.iteration = 5;
  applyAgentResult(c, activeState(), task, { kind: 'success', decision: plan('test', ['unit']) }, 'v1'); assert.equal(task.status, 'blocked');
});
test('contaminated measurements are retried with a finite limit', () => {
  const task = makeTask('K3', 'goal', ['client']);
  const result = { kind: 'test', success: false, retryable: true, message: 'input detected' };
  for (let i = 0; i < 3; i++) applyTestResult(c, task, { testId: 'client', sourceFingerprint: 'v1' }, result);
  assert.equal(task.status, 'blocked'); assert.equal(task.testIndex, 0);
});
test('successful deterministic tests advance without an AI call', () => {
  const task = makeTask('K3', 'goal', ['unit', 'client']);
  applyTestResult(c, task, { testId: 'unit', sourceFingerprint: 'v1' }, { kind: 'test', success: true });
  assert.equal(task.testIndex, 1); assert.equal(task.phase, 'tests');
  applyTestResult(c, task, { testId: 'client', sourceFingerprint: 'v1' }, { kind: 'test', success: true }); assert.equal(task.phase, 'agent');
});
test('locked, busy and active desktops do not admit a Minecraft measurement', () => {
  const ready = { desktop: 'Default', session: 1, consoleSession: 1, idleSeconds: 180, javaProcesses: 0, cpuPercent: 5 };
  assert.equal(desktopReason(ready, c), null);
  for (const change of [{ desktop: 'Winlogon' }, { idleSeconds: 0 }, { javaProcesses: 1 }, { cpuPercent: 90 }, { consoleSession: 2 }]) assert.ok(desktopReason({ ...ready, ...change }, c));
});
test('source changes between benchmark cells block mixing their results', async t => {
  const directory = temporary(t); const task = makeTask('K3', 'goal', ['unit']); task.sourceFingerprint = 'old';
  const s = newState(); s.tasks.push(task);
  await tick({ ...c, stateDirectory: directory }, s, { fingerprint: () => 'changed' }); assert.equal(task.status, 'blocked'); assert.equal(s.active, null);
});
test('a quiet build also waits while the PC is busy instead of disturbing a live benchmark', async t => {
  const directory = temporary(t); const task = makeTask('build', 'goal', ['unit']); const s = newState(); s.tasks.push(task);
  await tick({ ...c, stateDirectory: directory, tests: { unit: { quiet: true } } }, s, { fingerprint: () => 'v1', desktop: () => ({ desktop: 'Default', session: 1, consoleSession: 1, idleSeconds: 180, javaProcesses: 1, cpuPercent: 5 }) });
  assert.equal(s.active, null); assert.equal(task.status, 'queued'); assert.match(s.message, /Java/);
});
test('recovered result is applied once and the next test index is persisted', async t => {
  const directory = temporary(t); const task = makeTask('K3', 'goal', ['unit', 'client']); const s = newState(); s.tasks.push(task);
  s.active = { directory, taskId: task.id, type: 'test', testId: 'unit', sourceFingerprint: 'v1' };
  writeJson(path.join(directory, 'result.json'), { kind: 'test', success: true });
  await tick({ ...c, stateDirectory: directory }, s, { fingerprint: () => 'v1' });
  writeJson(path.join(directory, 'state.json'), s); const recovered = readJson(path.join(directory, 'state.json'));
  assert.equal(recovered.active, null); assert.equal(recovered.tasks[0].testIndex, 1); assert.equal(recovered.tasks[0].results.length, 1);
});
test('subscription adapter removes API and cross-profile environment overrides', () => {
  const previous = process.env.ANTHROPIC_API_KEY; const previousConfig = process.env.CLAUDE_CONFIG_DIR;
  process.env.ANTHROPIC_API_KEY = 'dummy-for-test'; process.env.CLAUDE_CONFIG_DIR = 'wrong-profile';
  try { const env = nativeEnvironment({ kind: 'claude', configDirectory: 'profile-two' }); assert.equal(env.ANTHROPIC_API_KEY, undefined); assert.equal(env.CLAUDE_CONFIG_DIR, 'profile-two'); }
  finally { if (previous === undefined) delete process.env.ANTHROPIC_API_KEY; else process.env.ANTHROPIC_API_KEY = previous; if (previousConfig === undefined) delete process.env.CLAUDE_CONFIG_DIR; else process.env.CLAUDE_CONFIG_DIR = previousConfig; }
  assert.ok(!agentArguments({ kind: 'codex' }, 'final.json').some(arg => arg.includes('bypass')));
});
test('worker executes a real synthetic process and rejects an input mutation', async t => {
  const directory = temporary(t); fs.writeFileSync(path.join(directory, 'input.txt'), 'stable');
  const fc = { projectRoot: directory, fingerprintPaths: ['input.txt'] };
  const spec = { directory, command: process.execPath, arguments: [fixtures, 'success'], cwd: directory, type: 'test', test: {}, timeoutSeconds: 20, sourceFingerprint: fingerprint(fc), fingerprintConfig: fc };
  assert.equal((await execute(spec)).success, true);
  spec.arguments = [fixtures, 'change', path.join(directory, 'input.txt')]; const changed = await execute(spec);
  assert.equal(changed.success, false); assert.equal(changed.sourceChanged, true);
});
test('worker timeout stops its own synthetic process and reports an unfinished operation', async t => {
  const directory = temporary(t); fs.writeFileSync(path.join(directory, 'input.txt'), 'stable');
  const fc = { projectRoot: directory, fingerprintPaths: ['input.txt'] };
  const result = await execute({ directory, command: process.execPath, arguments: [fixtures, 'delay', path.join(directory, 'should-not-exist')], cwd: directory, type: 'test', test: {}, timeoutSeconds: 0.1, sourceFingerprint: fingerprint(fc), fingerprintConfig: fc });
  assert.equal(result.kind, 'error'); assert.equal(fs.existsSync(path.join(directory, 'should-not-exist')), false);
});
test('Windows PowerShell receives arrays as data and a restarted controller finishes the queue without AI', { skip: process.platform !== 'win32' }, async t => {
  const directory = temporary(t);
  fs.writeFileSync(path.join(directory, 'input.txt'), 'stable');
  fs.writeFileSync(path.join(directory, 'fake test.ps1'), 'param([string[]]$Dodatno,[string]$ResultPath)\n@{values=$Dodatno} | ConvertTo-Json | Set-Content -LiteralPath $ResultPath -Encoding UTF8\n');
  const parameters = { Dodatno: ['-Variant', 'a value with spaces', 'literal $(never execute)', 'miška'], ResultPath: path.join(directory, 'arguments.json') };
  const config = { version: 1, projectRoot: '.', stateDirectory: '.overnight', minimumIdleSeconds: 0, maximumCpuPercent: 20, unknownQuotaRetryMinutes: 15, maximumIterations: 2, maximumTestRetries: 1, agentTimeoutSeconds: 30, fingerprintPaths: ['input.txt'], profiles: [], tests: { mock: { script: 'fake test.ps1', parameters, timeoutSeconds: 30 } } };
  const configFile = path.join(directory, 'config.json'); writeJson(configFile, config);
  const s = newState(); s.tasks.push(makeTask('synthetic', 'synthetic test only', ['mock'])); writeJson(path.join(directory, '.overnight', 'state.json'), s);
  const cli = fileURLToPath(new URL('../runner.mjs', import.meta.url));
  const start = spawnSync(process.execPath, [cli, 'run', '--once', '--config', configFile], { encoding: 'utf8', timeout: 20000, windowsHide: true });
  assert.equal(start.status, 0, start.stderr); assert.ok(readJson(path.join(directory, '.overnight', 'state.json')).active);
  const resume = spawn(process.execPath, [cli, 'run', '--test-only', '--hours', '0.01', '--config', configFile], { windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  let stderr = ''; resume.stderr.on('data', data => { stderr += data; }); resume.stdout.resume();
  const code = await new Promise((resolve, reject) => { resume.on('error', reject); resume.on('close', resolve); });
  assert.equal(code, 0, stderr);
  const saved = readJson(path.join(directory, '.overnight', 'state.json'));
  assert.equal(saved.active, null); assert.equal(saved.tasks[0].phase, 'agent'); assert.equal(saved.tasks[0].results.length, 1); assert.equal(saved.tasks[0].results[0].success, true);
  assert.deepEqual(readJson(path.join(directory, 'arguments.json')).values, parameters.Dodatno);
});
test('detached test finishes after its launching controller exits', async t => {
  const directory = temporary(t); fs.writeFileSync(path.join(directory, 'input.txt'), 'stable');
  const fc = { projectRoot: directory, fingerprintPaths: ['input.txt'] };
  const spec = { directory, command: process.execPath, arguments: [fixtures, 'delay', path.join(directory, 'done.txt')], cwd: directory, type: 'test', test: {}, timeoutSeconds: 20, sourceFingerprint: fingerprint(fc), fingerprintConfig: fc };
  const specFile = path.join(directory, 'spec.json'); writeJson(specFile, spec);
  const launcher = spawn(process.execPath, [fixtures, 'launch', specFile], { windowsHide: true, stdio: 'ignore' });
  await new Promise((resolve, reject) => { launcher.on('error', reject); launcher.on('close', resolve); });
  const result = path.join(directory, 'result.json'); const deadline = Date.now() + 15000;
  while (!fs.existsSync(result) && Date.now() < deadline) await new Promise(r => setTimeout(r, 100));
  assert.ok(fs.existsSync(result)); assert.equal(readJson(result).success, true); assert.equal(fs.readFileSync(path.join(directory, 'done.txt'), 'utf8'), 'finished');
});

test('codex executable follows the desktop app to its newest bin hash', t => {
  const root = temporary(t);
  for (const [hash, time] of [['old1', 1000], ['new2', 2000]]) {
    fs.mkdirSync(path.join(root, hash)); const exe = path.join(root, hash, 'codex.exe');
    fs.writeFileSync(exe, ''); fs.utimesSync(exe, time, time);
  }
  fs.mkdirSync(path.join(root, 'tools')); fs.writeFileSync(path.join(root, 'tools', 'rg.exe'), '');
  assert.equal(codexInstalled(root), path.join(root, 'new2', 'codex.exe'));
  assert.equal(codexInstalled(path.join(root, 'missing')), null);
});
