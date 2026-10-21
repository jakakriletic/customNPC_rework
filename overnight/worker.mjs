import fs from 'node:fs';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { readJson, writeJson, now, nativeEnvironment, parseAgent, fingerprint, desktopState, subscriptionStatus, toolDirectory, lock, processJob } from './lib.mjs';

export function tail(file, bytes = 4 * 1024 * 1024) {
  if (!fs.existsSync(file)) return '';
  const fd = fs.openSync(file, 'r');
  try { const size = fs.fstatSync(fd).size; const b = Buffer.alloc(Math.min(size, bytes)); fs.readSync(fd, b, 0, b.length, Math.max(0, size - b.length)); return b.toString('utf8'); }
  finally { fs.closeSync(fd); }
}
export async function execute(spec) {
  const releaseAwake = spec.test?.desktop ? await lock(spec.directory, { keepAwake: true }) : async () => {};
  try { return await executeChild(spec); }
  finally { await releaseAwake(); }
}
async function executeChild(spec) {
  const output = path.join(spec.directory, 'stdout.log');
  const errorOutput = path.join(spec.directory, 'stderr.log');
  if (spec.profile) {
    const auth = subscriptionStatus(spec.profile);
    if (!auth.ok) return { kind: 'auth', message: auth.reason };
  }
  if (fs.existsSync(path.join(spec.directory, 'CANCEL'))) return { kind: 'cancelled', message: 'Prekinjeno pred zagonom' };
  const result = await new Promise(resolve => {
    const out = fs.openSync(output, 'a'); const err = fs.openSync(errorOutput, 'a');
    let child; let timer; let cancelTimer; let timedOut = false; let cancelled = false; let finished = false;
    const finish = value => { if (finished) return; finished = true; clearTimeout(timer); clearInterval(cancelTimer); fs.closeSync(out); fs.closeSync(err); resolve({ ...value, cancelled }); };
    try {
      child = spawn(spec.command, spec.arguments, { cwd: spec.cwd, env: nativeEnvironment(spec.profile), windowsHide: true, stdio: ['pipe', out, err] });
    } catch (e) { finish({ code: -1, error: e.message }); return; }
    child.on('error', e => finish({ code: -1, error: e.message }));
    child.on('close', code => finish({ code: code ?? -1, timedOut }));
    child.stdin.on('error', () => {});
    child.stdin.end(spec.prompt || '');
    const killOwnedChild = () => {
      if (child.pid) {
        if (process.platform === 'win32') spawnSync('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], { windowsHide: true, timeout: 15000, stdio: 'ignore' });
        else child.kill('SIGTERM');
      }
    };
    timer = setTimeout(() => { timedOut = true; killOwnedChild(); }, spec.timeoutSeconds * 1000);
    cancelTimer = setInterval(() => {
      if (!cancelled && fs.existsSync(path.join(spec.directory, 'CANCEL'))) { cancelled = true; killOwnedChild(); }
    }, 250);
  });
  if (result.cancelled) return { kind: 'cancelled', message: 'Uporabnik je ustavil operacijo; delni test ni sprejet', stdout: output, stderr: errorOutput };
  if (result.error || result.timedOut) return { kind: 'error', message: result.error || 'Operacija je dosegla casovno mejo; pred nadaljevanjem preglej delne spremembe', stdout: output, stderr: errorOutput };
  if (spec.type === 'agent') return { ...parseAgent(spec.profile.kind, tail(output), tail(errorOutput), result.code, spec.finalFile, spec.retryMinutes), stdout: output, stderr: errorOutput };
  let afterDesktop;
  if (spec.test.desktop) {
    try { afterDesktop = desktopState(); }
    catch (e) { return { kind: 'test', success: false, retryable: true, message: e.message, stdout: output, stderr: errorOutput }; }
  }
  const sourceChanged = fingerprint(spec.fingerprintConfig) !== spec.sourceFingerprint;
  const inputChanged = afterDesktop && (afterDesktop.lastInputTick !== spec.beforeDesktop.lastInputTick || afterDesktop.desktop.toLowerCase() !== 'default' || afterDesktop.session !== afterDesktop.consoleSession);
  const retryable = inputChanged || /PADLO\s+K[058]\b|Tuj java proces|Scenarij .* ze tece|zaklep.*zaseden/i.test(tail(output, 100000) + tail(errorOutput, 100000));
  return {
    kind: 'test', success: result.code === 0 && !sourceChanged && !inputChanged,
    exitCode: result.code, retryable: Boolean(retryable && !sourceChanged), sourceChanged,
    message: sourceChanged ? 'Koda ali merilni vhodi so se med testom spremenili' : inputChanged ? 'Med testom je bil zaznan vnos ali sprememba namizja; meritev ni sprejeta' : result.code === 0 ? 'Test uspesen' : 'Test ni uspel; glej log',
    stdout: output, stderr: errorOutput, afterDesktop
  };
}

async function main() {
  const specFile = process.argv[2];
  if (!specFile) throw new Error('Manjka pot do operacije');
  const spec = readJson(specFile);
  const claim = path.join(spec.directory, 'claim.json');
  try { fs.writeFileSync(claim, JSON.stringify({ pid: process.pid, startedAt: now() }), { flag: 'wx' }); }
  catch (e) { if (e.code === 'EEXIST') return; throw e; }
  // AI tools can orphan searches or daemons; the job stops them after the result is saved.
  let releaseJob = async () => {};
  if (spec.type === 'agent') {
    try { releaseJob = await processJob(); }
    catch (e) { process.stderr.write(`${now()} ${e.message}; AI tece brez zbiranja podprocesov\n`); }
  }
  let result;
  try { result = await execute(spec); }
  catch (e) { result = { kind: 'error', message: e.stack || e.message }; }
  writeJson(path.join(spec.directory, 'result.json'), { ...result, finishedAt: now() });
  await releaseJob();
}
if (process.argv[1] && path.resolve(process.argv[1]) === path.join(toolDirectory, 'worker.mjs')) {
  main().catch(e => { process.stderr.write(`${e.stack}\n`); process.exitCode = 1; });
}
