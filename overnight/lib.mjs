import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const toolDirectory = path.dirname(fileURLToPath(import.meta.url));
export const schemaPath = path.join(toolDirectory, 'decision.schema.json');
export const now = () => new Date().toISOString();
export const readJson = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
export function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temporary = `${file}.${process.pid}.${crypto.randomUUID()}.tmp`;
  const fd = fs.openSync(temporary, 'wx');
  try { fs.writeFileSync(fd, `${JSON.stringify(value, null, 2)}\n`); fs.fsyncSync(fd); }
  finally { fs.closeSync(fd); }
  fs.renameSync(temporary, file);
}
export function within(root, relative) {
  const absolute = path.resolve(root, relative);
  const delta = path.relative(root, absolute);
  if (delta === '..' || delta.startsWith(`..${path.sep}`) || path.isAbsolute(delta)) throw new Error(`Pot zunaj projekta: ${relative}`);
  return absolute;
}
export function loadConfig(file) {
  const c = readJson(file);
  if (c.version !== 1) throw new Error('Nepodprta verzija konfiguracije');
  c.projectRoot = fs.realpathSync(path.resolve(path.dirname(file), c.projectRoot));
  c.stateDirectory = within(c.projectRoot, c.stateDirectory);
  if (!Array.isArray(c.profiles) || !Array.isArray(c.fingerprintPaths)) throw new Error('Manjkajo profili ali fingerprintPaths');
  const ids = new Set();
  for (const p of c.profiles) {
    if (!/^[a-z0-9-]+$/.test(p.id) || ids.has(p.id) || !['codex', 'claude'].includes(p.kind)) throw new Error('Neveljaven ali podvojen profil');
    ids.add(p.id);
    if (p.configDirectory) p.configDirectory = within(c.projectRoot, p.configDirectory);
  }
  for (const [id, t] of Object.entries(c.tests)) {
    if (!/^[a-z0-9-]+$/.test(id)) throw new Error('Neveljavno ime testa');
    t.script = within(c.projectRoot, t.script);
    if (!t.script.endsWith('.ps1') || !fs.statSync(t.script).isFile()) throw new Error(`Testna skripta ne obstaja: ${id}`);
    if (!t.parameters || typeof t.parameters !== 'object' || Array.isArray(t.parameters)) throw new Error(`Neveljavni parametri: ${id}`);
    if (!(Number(t.timeoutSeconds) > 0)) throw new Error(`Manjka casovna meja testa: ${id}`);
  }
  for (const key of ['minimumIdleSeconds', 'maximumCpuPercent', 'unknownQuotaRetryMinutes', 'maximumIterations', 'maximumTestRetries', 'agentTimeoutSeconds']) {
    if (!Number.isFinite(c[key]) || c[key] < 0) throw new Error(`Neveljavna nastavitev: ${key}`);
  }
  if (c.maximumCpuPercent > 100 || c.unknownQuotaRetryMinutes === 0 || c.maximumIterations === 0 || c.agentTimeoutSeconds === 0) throw new Error('Neveljavna meja CPU, ponovitev ali casovnega okna');
  return c;
}
export function fingerprint(c) {
  const hash = crypto.createHash('sha256');
  hash.update(JSON.stringify(c.tests || {})); hash.update('\0');
  const visit = file => {
    const stat = fs.lstatSync(file);
    if (stat.isSymbolicLink()) throw new Error(`Simbolicna povezava v merilnih vhodih: ${file}`);
    if (stat.isDirectory()) {
      for (const name of fs.readdirSync(file).sort()) visit(path.join(file, name));
    } else if (stat.isFile()) {
      hash.update(path.relative(c.projectRoot, file).replaceAll('\\', '/')); hash.update('\0');
      hash.update(fs.readFileSync(file)); hash.update('\0');
    }
  };
  for (const relative of [...c.fingerprintPaths].sort()) visit(within(c.projectRoot, relative));
  // New scenario scripts are measurement inputs even if absent from the original path list.
  for (const script of [...new Set(Object.values(c.tests || {}).map(t => t.script).filter(Boolean))].sort()) visit(within(c.projectRoot, script));
  return hash.digest('hex');
}
export function alive(pid) {
  if (!Number.isInteger(pid) || pid < 1) return false;
  try { process.kill(pid, 0); return true; } catch (e) { return e.code === 'EPERM'; }
}
export async function lock(directory, { keepAwake = false } = {}) {
  if (process.platform === 'win32') {
    const name = `Global\\CustomNPCOvernight-${crypto.createHash('sha256').update(path.resolve(directory).toLowerCase()).digest('hex')}`;
    const args = ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', path.join(toolDirectory, 'windows.ps1'), '-Mode', 'Lease', '-LeaseName', name];
    if (keepAwake) args.push('-KeepAwake');
    return await new Promise((resolve, reject) => {
      const child = spawn('powershell.exe', args, { windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
      let ready = false; let output = ''; let errors = ''; let finishClose;
      const closed = new Promise(r => { finishClose = r; });
      const timer = setTimeout(() => { child.kill(); reject(new Error('Windows lease se ni odzval')); }, 20000);
      child.stdin.on('error', () => {});
      child.stderr.on('data', data => { errors += data; });
      child.on('error', e => { clearTimeout(timer); reject(e); });
      child.on('close', () => { finishClose(); clearTimeout(timer); if (!ready) reject(new Error(output.includes('BUSY') ? 'Upravljalnik ze tece' : `Windows lease ni uspel: ${errors.slice(-800)}`)); });
      child.stdout.on('data', data => {
        output += data;
        if (!ready && output.includes('READY')) {
          ready = true; clearTimeout(timer);
          resolve(async () => { child.stdin.end('\n'); await closed; });
        }
      });
    });
  }
  const file = path.join(directory, 'controller.lock');
  fs.mkdirSync(directory, { recursive: true });
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      fs.writeFileSync(file, JSON.stringify({ pid: process.pid }), { flag: 'wx' });
      return () => { if (fs.existsSync(file) && readJson(file).pid === process.pid) fs.unlinkSync(file); };
    } catch (e) {
      if (e.code !== 'EEXIST') throw e;
      let owner;
      try { owner = readJson(file); } catch { throw new Error('Necitljiv zaklep upravljalnika; preveri proces pred odstranitvijo'); }
      if (alive(owner.pid)) throw new Error(`Upravljalnik ze tece (PID ${owner.pid})`);
      fs.unlinkSync(file);
    }
  }
  throw new Error('Zaklepa ni bilo mogoce pridobiti');
}
export function executable(name) {
  if (path.isAbsolute(name)) return fs.existsSync(name) ? name : null;
  for (const directory of (process.env.PATH || '').split(path.delimiter)) {
    for (const extension of process.platform === 'win32' ? ['.exe', ''] : ['']) {
      const candidate = path.join(directory.replace(/^"|"$/g, ''), name + extension);
      try { if (fs.statSync(candidate).isFile()) return candidate; } catch { /* next path */ }
    }
  }
  if (process.platform === 'win32' && name === 'claude') {
    const candidate = path.join(process.env.USERPROFILE || '', '.local', 'bin', 'claude.exe');
    if (fs.existsSync(candidate)) return candidate;
    for (const editor of ['.vscode', '.cursor']) {
      const extensions = path.join(process.env.USERPROFILE || '', editor, 'extensions');
      if (!fs.existsSync(extensions)) continue;
      for (const extension of fs.readdirSync(extensions).filter(n => n.startsWith('anthropic.claude-code-')).sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))) {
        const bundled = path.join(extensions, extension, 'resources', 'native-binary', 'claude.exe');
        if (fs.existsSync(bundled)) return bundled;
      }
    }
  }
  return null;
}
export function nativeEnvironment(profile = {}) {
  const env = { ...process.env };
  // Keep subscription runs on the native login. Never fall back to metered API keys.
  const overrides = new Set(['OPENAI_API_KEY', 'CODEX_API_KEY', 'CODEX_ACCESS_TOKEN', 'ANTHROPIC_API_KEY', 'ANTHROPIC_AUTH_TOKEN', 'ANTHROPIC_BASE_URL', 'CLAUDE_CODE_OAUTH_TOKEN', 'CLAUDE_CODE_USE_BEDROCK', 'CLAUDE_CODE_USE_VERTEX', 'CLAUDE_CODE_USE_FOUNDRY']);
  for (const name of Object.keys(env)) if (overrides.has(name.toUpperCase())) delete env[name];
  delete env.NoDefaultCurrentDirectoryInExePath;
  delete env.CLAUDECODE;
  delete env.CLAUDE_CONFIG_DIR;
  if (profile.kind === 'claude' && profile.configDirectory) env.CLAUDE_CONFIG_DIR = profile.configDirectory;
  return env;
}
export function subscriptionStatus(profile) {
  const exe = executable(profile.executable);
  if (!exe) return { ok: false, reason: `Ni izvrsljive datoteke: ${profile.executable}` };
  const result = spawnSync(exe, profile.kind === 'codex' ? ['login', 'status'] : ['auth', 'status'], { encoding: 'utf8', env: nativeEnvironment(profile), windowsHide: true, timeout: 20000 });
  if (result.status !== 0) return { ok: false, reason: 'Native CLI prijava ni potrjena; uporabi login' };
  if (profile.kind === 'codex') return /using ChatGPT/i.test(result.stdout + result.stderr) ? { ok: true, reason: 'ChatGPT prijava' } : { ok: false, reason: 'Prijava ni ChatGPT narocnina' };
  try {
    const status = JSON.parse(result.stdout);
    return status.loggedIn === true && status.authMethod === 'claude.ai' ? { ok: true, reason: 'Claude.ai prijava' } : { ok: false, reason: 'Prijava ni native Claude.ai; API/setup-token pot ni uporabljena' };
  } catch { return { ok: false, reason: 'Razlicica CLI ne vrne preverljivega auth status JSON' }; }
}
export function desktopState() {
  if (process.platform !== 'win32') throw new Error('Minecraft desktop guard zahteva Windows');
  const result = spawnSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', path.join(toolDirectory, 'windows.ps1'), '-Mode', 'Desktop'], { encoding: 'utf8', windowsHide: true, timeout: 20000 });
  if (result.status !== 0) throw new Error(`Desktop guard ni uspel: ${(result.stderr || result.error?.message || '').slice(-500)}`);
  return JSON.parse(result.stdout.replace(/^\uFEFF/, '').trim());
}
export function desktopReason(d, c) {
  if (d.desktop.toLowerCase() !== 'default' || d.session !== d.consoleSession) return 'Windows nima aktivnega lokalnega namizja (zaklep ali oddaljena seja)';
  if (d.idleSeconds < c.minimumIdleSeconds) return `Cakam na ${c.minimumIdleSeconds} s brez vnosa`;
  if (d.javaProcesses > 0) return `Cakam: tece ${d.javaProcesses} drugih Java procesov`;
  if (d.cpuPercent > c.maximumCpuPercent) return `Cakam: CPU ${d.cpuPercent} % > ${c.maximumCpuPercent} %`;
  return null;
}
export function decision(value, c) {
  if (!value || !['test', 'done', 'blocked', 'idle'].includes(value.action) || typeof value.summary !== 'string' || typeof value.handoff !== 'string' || !Array.isArray(value.testIds)) throw new Error('AI ni vrnil veljavnega nacrta');
  if (value.summary.length > 12000 || value.handoff.length > 16000 || value.testIds.length > 30) throw new Error('AI nacrt je prevelik');
  if (Object.keys(value).some(k => !['action', 'summary', 'handoff', 'testIds', 'questions', 'testDefinitions', 'verification', 'evidence'].includes(k))) throw new Error('Nepricakovana polja AI nacrta');
  const definitions = {};
  if (!Array.isArray(value.questions || []) || (value.questions || []).length > 30) throw new Error('Neveljaven dnevnik odlocitev');
  for (const q of value.questions || []) for (const key of ['question', 'decision', 'rationale']) if (typeof q[key] !== 'string' || q[key].length > 6000) throw new Error('Neveljavna odlocitev');
  if (!Array.isArray(value.testDefinitions || []) || (value.testDefinitions || []).length > 30) throw new Error('Neveljaven katalog');
  for (const t of value.testDefinitions || []) {
    if (!/^[a-z0-9-]{1,80}$/.test(t.id) || Object.hasOwn(c.tests, t.id) || Object.hasOwn(definitions, t.id)) throw new Error('Test ze obstaja ali ima neveljavno ime');
    const script = fs.realpathSync(within(c.projectRoot, t.script)); within(c.projectRoot, script);
    if (!script.toLowerCase().endsWith('.ps1') || !fs.statSync(script).isFile()) throw new Error('Neveljavna testna skripta');
    const parameters = JSON.parse(t.parametersJson);
    if (!parameters || typeof parameters !== 'object' || Array.isArray(parameters)) throw new Error('Neveljavni parametri');
    for (const [key, v] of Object.entries(parameters)) {
      if (!/^[a-zA-Z][a-zA-Z0-9]*$/.test(key) || !(Array.isArray(v) ? v.every(x => ['string', 'number', 'boolean'].includes(typeof x)) : ['string', 'number', 'boolean'].includes(typeof v))) throw new Error('Neveljavni literalni parametri');
    }
    if (!Number.isInteger(t.timeoutSeconds) || t.timeoutSeconds < 1 || t.timeoutSeconds > 14400 || typeof t.desktop !== 'boolean') throw new Error('Neveljavna meja testa');
    // Every new scenario uses desktop guards; only the known non-client scripts can opt out.
    const desktop = !['dev.ps1', 'verify-package.ps1'].includes(path.relative(c.projectRoot, script).replaceAll('\\', '/')) || t.desktop;
    definitions[t.id] = { script, parameters, desktop, quiet: true, timeoutSeconds: t.timeoutSeconds };
  }
  if (value.verification !== undefined && !['tests', 'research'].includes(value.verification)) throw new Error('Neveljavno preverjanje');
  if (!Array.isArray(value.evidence || []) || (value.evidence || []).length > 30 || (value.evidence || []).some(e => typeof e !== 'string' || e.length > 500)) throw new Error('Neveljavna dokazila');
  if (value.action === 'test' && value.testIds.length === 0) throw new Error('Prazen seznam testov');
  for (const id of value.testIds) if (!Object.hasOwn(c.tests, id) && !Object.hasOwn(definitions, id)) throw new Error(`AI je zahteval neznan test: ${id}`);
  return { ...value, definitions };
}
export function quotaReset(error, currentMs, defaultMinutes) {
  for (const candidate of [error?.resets_at, error?.reset_at, error?.resetAt]) {
    const value = typeof candidate === 'number' ? (candidate < 1e12 ? candidate * 1000 : candidate) : Date.parse(candidate);
    if (Number.isFinite(value) && value > currentMs) return { until: new Date(value).toISOString(), exact: true };
  }
  if (Number(error?.retry_after) > 0) return { until: new Date(currentMs + Number(error.retry_after) * 1000).toISOString(), exact: true };
  return { until: new Date(currentMs + defaultMinutes * 60000).toISOString(), exact: false };
}
export function classifyFailure(errors, stderr, exitCode, defaultMinutes, currentMs = Date.now()) {
  // Inspect provider error envelopes, never arbitrary successful model prose or tool logs.
  const candidates = errors.length ? errors : exitCode !== 0 ? [{ message: stderr.slice(-8000) }] : [];
  for (const error of candidates) {
    const message = String(error.message || error.result || '');
    const code = String(error.code || error.type || '');
    if (/usage_limit_reached|rate_limit_exceeded|insufficient_quota|rate_limit_error/i.test(code) || /you['’]ve hit (?:your|the) (?:usage )?limit|usage limit (?:reached|exceeded)|rate limit (?:reached|exceeded)|weekly limit reached|too many requests/i.test(message)) {
      return { kind: 'quota', message: message.slice(0, 2000), ...quotaReset(error, currentMs, defaultMinutes) };
    }
    if (/authentication_error|unauthorized/i.test(code) || /not logged in|please (?:log|sign) in|authentication required|invalid api key|oauth token.*expired/i.test(message)) return { kind: 'auth', message: message.slice(0, 2000) };
  }
  return { kind: 'error', message: String(candidates[0]?.message || stderr || `Izhod ${exitCode}`).slice(-4000) };
}
export function parseAgent(kind, stdout, stderr, exitCode, finalFile, retryMinutes) {
  const errors = []; let value; let session; let completed = false;
  if (kind === 'codex') {
    for (const line of stdout.split(/\r?\n/)) {
      let e; try { e = JSON.parse(line); } catch { continue; }
      if (e.type === 'thread.started') session = e.thread_id;
      if (e.type === 'turn.completed') completed = true;
      if (e.type === 'turn.failed' && e.error) { completed = false; errors.push(e.error); }
      if (e.type === 'error') errors.push(e.error || e);
    }
    if (fs.existsSync(finalFile)) { try { value = readJson(finalFile); } catch { /* invalid response is failure */ } }
  } else {
    let envelope;
    try { envelope = JSON.parse(stdout.trim()); } catch { /* some versions print JSONL */ }
    if (!envelope) for (const line of stdout.split(/\r?\n/)) { try { const e = JSON.parse(line); if (e.type === 'result') envelope = e; } catch { /* non-JSON line */ } }
    if (envelope) {
      session = envelope.session_id;
      if (envelope.is_error) errors.push({ message: envelope.result || envelope.errors?.join('\n') || envelope.subtype, ...envelope.error });
      else {
        value = envelope.structured_output;
        if (!value && typeof envelope.result === 'string') { try { value = JSON.parse(envelope.result); } catch { /* invalid response */ } }
      }
    }
  }
  if (exitCode === 0 && (errors.length === 0 || completed) && value) return { kind: 'success', decision: value, session };
  return { ...classifyFailure(errors, stderr, exitCode, retryMinutes), session };
}
export function agentArguments(profile, finalFile, { fullAccess = false } = {}) {
  if (fullAccess && profile.kind === 'codex') return ['exec', '--json', '--dangerously-bypass-approvals-and-sandbox', '--output-schema', schemaPath, '--output-last-message', finalFile, '-'];
  if (fullAccess) return ['-p', '--output-format', 'json', '--json-schema', fs.readFileSync(schemaPath, 'utf8'), '--dangerously-skip-permissions', '--permission-prompts', 'none'];
  if (profile.kind === 'codex') return ['exec', '--json', '--approve-for-me', '--output-schema', schemaPath, '--output-last-message', finalFile, '-'];
  return ['-p', '--output-format', 'json', '--json-schema', fs.readFileSync(schemaPath, 'utf8'), '--permission-mode', 'acceptEdits', '--permission-prompts', 'none', '--allowedTools', 'Read', 'Edit', 'Write', 'Glob', 'Grep', 'Bash(git status *)', 'Bash(git diff *)', 'Bash(git log *)', 'Bash(git show *)', 'Bash(git add *)', 'Bash(git commit *)', 'Bash(git fetch *)', 'Bash(git pull *)', 'Bash(git push *)'];
}
