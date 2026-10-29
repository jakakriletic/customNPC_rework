// Pre-launch check: AI CLI updates must not silently break the overnight controller.
// Usage: node overnight/predzagon.mjs   (exit 0 = safe to launch, 1 = fix first)
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { toolDirectory, loadConfig, executable, nativeEnvironment, subscriptionStatus, agentArguments } from './lib.mjs';

const c = loadConfig(path.join(toolDirectory, 'config.local.json'));
let failed = false;
const report = (ok, text) => { if (!ok) failed = true; console.log(`${ok ? 'OK  ' : 'NAPAKA'} ${text}`); };

for (const p of c.profiles.filter(p => p.enabled)) {
  const exe = executable(p.executable);
  report(Boolean(exe), `${p.id}: izvrsljiva datoteka ${exe || `ni najdena (${p.executable})`}`);
  if (!exe) continue;
  const version = spawnSync(exe, ['--version'], { encoding: 'utf8', env: nativeEnvironment(p), windowsHide: true, timeout: 20000 });
  report(version.status === 0, `${p.id}: verzija ${(version.stdout || version.stderr || String(version.error)).trim().split('\n')[0]}`);
  const auth = subscriptionStatus(p);
  report(auth.ok, `${p.id}: prijava ${auth.reason}`);
  // Every flag the controller passes must still appear in the CLI help.
  const help = spawnSync(exe, p.kind === 'codex' ? ['exec', '--help'] : ['--help'], { encoding: 'utf8', env: nativeEnvironment(p), windowsHide: true, timeout: 20000 });
  const text = `${help.stdout}${help.stderr}`;
  const flags = agentArguments(p, 'final.json', { fullAccess: Boolean(c.autonomy?.fullAccess) }).filter(a => a.startsWith('-') && a !== '-' && a !== '-p');
  const missing = flags.filter(f => !text.includes(f));
  report(missing.length === 0, `${p.id}: zastavice ${missing.length ? `manjkajo ${missing.join(' ')}` : flags.join(' ')}`);
}
process.exit(failed ? 1 : 0);
