import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn, spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';
import { toolDirectory, loadConfig, readJson, writeJson, alive, now, subscriptionStatus, lock } from './lib.mjs';
import { newState, makeTask } from './runner.mjs';
import { running, publish, cancelActive } from './control.mjs';
import { tail } from './worker.mjs';

const delay = ms => new Promise(r => setTimeout(r, ms));
const stateOf = c => { try { return readJson(path.join(c.stateDirectory, 'state.json')); } catch { return newState(); } };
function launchRunner(c, configFile) {
  const log = fs.openSync(path.join(c.stateDirectory, 'controller.log'), 'a');
  try {
    const child = spawn(process.execPath, [path.join(toolDirectory, 'runner.mjs'), 'run', '--continuous', '--config', configFile], { cwd: c.projectRoot, detached: true, windowsHide: true, stdio: ['ignore', log, log] });
    child.on('error', e => fs.appendFileSync(path.join(c.stateDirectory, 'controller.log'), `\n${e.message}\n`)); child.unref(); return child;
  } finally { fs.closeSync(log); }
}
export async function createDashboard(c, configFile, port = 0) {
  fs.mkdirSync(c.stateDirectory, { recursive: true });
  const token = crypto.randomBytes(32).toString('hex');
  let origin; let starting = null; let probing = null; const auth = {};
  const uiFile = path.join(c.stateDirectory, 'ui.json');
  const pauseFile = path.join(c.stateDirectory, 'PAUSE'); const stopFile = path.join(c.stateDirectory, 'STOP');
  async function start(preserveStop = false) {
    if (running(c)) return;
    if (starting) return starting;
    starting = (async () => {
      if (!preserveStop) { fs.rmSync(stopFile, { force: true }); fs.rmSync(pauseFile, { force: true }); }
      const child = launchRunner(c, configFile);
      for (let i = 0; i < 60; i++) {
        if (running(c)) return;
        if (child.exitCode !== null || !alive(child.pid)) throw new Error(tail(path.join(c.stateDirectory, 'controller.log'), 2500) || 'Upravljalnik se ni zagnal');
        await delay(100);
      }
      throw new Error('Zagon se ni potrdil v 6 s; preveri dnevnik');
    })();
    try { await starting; } finally { starting = null; }
  }
  function status() {
    const s = stateOf(c); const controller = running(c);
    const inbox = path.join(c.stateDirectory, 'inbox');
    const pending = fs.existsSync(inbox) ? fs.readdirSync(inbox).filter(v => v.endsWith('.json')).map(n => readJson(path.join(inbox, n))) : [];
    const eventDir = path.join(c.stateDirectory, 'events');
    const loginIds = fs.existsSync(eventDir) ? fs.readdirSync(eventDir).filter(v => v.endsWith('.json')).map(n => readJson(path.join(eventDir, n))).filter(e => ['login', 'verified'].includes(e.type)).map(e => e.id) : [];
    return { ...s, tasks: [...s.tasks, ...pending.filter(t => !s.tasks.some(v => v.id === t.id))], controller, starting: Boolean(starting), paused: fs.existsSync(pauseFile), stopping: Boolean(controller && fs.existsSync(stopFile)), project: c.projectRoot,
      profiles: c.profiles.map(p => ({ id: p.id, kind: p.kind, enabled: p.enabled || s.profiles[p.id]?.enabledOverride || loginIds.includes(p.id), health: s.profiles[p.id] || {}, auth: auth[p.id] || { reason: 'Prijava se preveri pred uporabo' }, isolated: Boolean(p.configDirectory) })),
      autonomy: c.autonomy, probing: Boolean(probing) };
  }
  async function body(req) {
    let text = ''; for await (const part of req) { text += part; if (text.length > 20000) throw new Error('Zahteva je prevelika'); }
    return text ? JSON.parse(text) : {};
  }
  const server = http.createServer(async (req, res) => {
    const json = (code, value) => { res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8' }); res.end(JSON.stringify(value)); };
    res.setHeader('Cache-Control', 'no-store'); res.setHeader('X-Content-Type-Options', 'nosniff'); res.setHeader('Referrer-Policy', 'no-referrer');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    try {
      if (req.headers.host !== new URL(origin).host || (req.headers.origin && req.headers.origin !== origin) || (req.headers['sec-fetch-site'] && !['same-origin', 'none'].includes(req.headers['sec-fetch-site']))) return json(403, { error: 'Dovoljen je samo lokalni vmesnik' });
      const url = new URL(req.url, origin);
      if (req.method === 'GET' && url.pathname === '/health') return json(200, { app: 'customnpc-overnight', pid: process.pid });
      const staticFiles = { '/': ['index.html', 'text/html'], '/app.js': ['app.js', 'text/javascript'], '/style.css': ['style.css', 'text/css'] };
      if (req.method === 'GET' && staticFiles[url.pathname]) {
        const [file, type] = staticFiles[url.pathname]; let content = fs.readFileSync(path.join(toolDirectory, 'ui', file), 'utf8');
        if (file === 'index.html') content = content.replace('%%TOKEN%%', token);
        res.writeHead(200, { 'Content-Type': `${type}; charset=utf-8` }); return res.end(content);
      }
      if (req.headers['x-overnight-token'] !== token || (req.method !== 'GET' && req.headers.origin !== origin)) return json(403, { error: 'Neveljavna lokalna seja; osvezi stran' });
      if (req.method === 'GET' && url.pathname === '/api/status') return json(200, status());
      if (req.method === 'GET' && url.pathname === '/api/log') {
        const s = stateOf(c); let operation = s.active?.directory;
        if (!operation) {
          const dir = path.join(c.stateDirectory, 'operations');
          if (fs.existsSync(dir)) operation = fs.readdirSync(dir).filter(n => /^[0-9a-f-]{36}$/.test(n)).map(n => path.join(dir, n)).sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs)[0];
        }
        return json(200, { controller: tail(path.join(c.stateDirectory, 'controller.log'), 16000), stdout: operation ? tail(path.join(operation, 'stdout.log'), 24000) : '', stderr: operation ? tail(path.join(operation, 'stderr.log'), 12000) : '' });
      }
      if (req.method !== 'POST') return json(404, { error: 'Ni najdeno' });
      const data = await body(req);
      if (url.pathname === '/api/start') { await start(); return json(200, { message: 'Neprekinjeno delo zagnano' }); }
      if (url.pathname === '/api/stop') {
        fs.writeFileSync(stopFile, now()); const s = stateOf(c); cancelActive(s);
        if (!running(c) && s.active) await start(true);
        return json(200, { message: 'Prekinitev zahtevana; ustavljam trenutno operacijo' });
      }
      if (url.pathname === '/api/pause') { fs.writeFileSync(pauseFile, now()); return json(200, { message: 'Po trenutni operaciji sledi pavza' }); }
      if (url.pathname === '/api/resume') { fs.rmSync(pauseFile, { force: true }); if (!running(c)) await start(); return json(200, { message: 'Nadaljujem' }); }
      if (url.pathname === '/api/add') {
        if (typeof data.goal !== 'string' || !data.goal.trim() || data.goal.length > 10000) throw new Error('Vnesi cilj (do 10.000 znakov)');
        const task = makeTask(data.goal.trim().slice(0, 100), data.goal.trim()); writeJson(path.join(c.stateDirectory, 'inbox', `${task.id}.json`), task); return json(200, { message: 'Naloga dodana v vrsto' });
      }
      if (url.pathname === '/api/retry') {
        if (!stateOf(c).tasks.some(t => t.id === data.id && ['blocked', 'waiting'].includes(t.status))) throw new Error('Naloge ni mogoce ponoviti');
        publish(c, { type: 'retry', id: data.id }); return json(200, { message: 'Ponovitev shranjena za naslednji korak' });
      }
      if (url.pathname === '/api/login') {
        const p = c.profiles.find(p => p.id === data.id); if (!p) throw new Error('Neznan profil');
        // Interactive login opens only on the owner's explicit button click.
        const child = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', path.join(toolDirectory, 'ui-launch.ps1'), '-Mode', 'Login', '-ConfigFile', configFile, '-ProfileId', p.id], { windowsHide: true, stdio: 'ignore' }); child.on('error', () => {}); child.unref();
        return json(200, { message: `Odprto prijavno okno za ${p.id}. Po prijavi izberi Preveri prijave.` });
      }
      if (url.pathname === '/api/probe') {
        if (running(c) || stateOf(c).active) throw new Error('Za preverjanje prijav najprej ustavi delo, da ne motis meritve');
        if (!probing) {
          probing = (async () => {
            for (const p of c.profiles) {
              const result = await new Promise(resolve => {
                const child = spawn(process.execPath, [path.join(toolDirectory, 'dashboard.mjs'), 'probe', '--config', configFile, '--profile', p.id], { windowsHide: true, stdio: ['ignore', 'pipe', 'ignore'] });
                let output = ''; child.stdout.on('data', b => { output += b; }); child.on('error', () => resolve({ ok: false, reason: 'Preverjanje ni uspelo' }));
                child.on('close', () => { try { resolve(JSON.parse(output)); } catch { resolve({ ok: false, reason: 'Preverjanje ni uspelo' }); } });
              });
              auth[p.id] = { ...result, checkedAt: now() }; if (result.ok) publish(c, { type: 'verified', id: p.id });
            }
          })().finally(() => { probing = null; });
        }
        return json(200, { message: 'Preverjam native narocniske prijave ...' });
      }
      return json(404, { error: 'Neznan ukaz' });
    } catch (e) { json(400, { error: e.message }); }
  });
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', resolve); });
  origin = `http://127.0.0.1:${server.address().port}`;
  writeJson(uiFile, { pid: process.pid, url: origin, startedAt: now() });
  return { server, url: origin, close: () => new Promise(resolve => server.close(resolve)) };
}
async function main() {
  const args = process.argv.slice(2); const command = args[0] || 'launch';
  const value = key => args.includes(key) ? args[args.indexOf(key) + 1] : undefined;
  const configFile = path.resolve(value('--config') || path.join(toolDirectory, 'config.local.json')); const c = loadConfig(configFile);
  if (command === 'probe') { const p = c.profiles.find(p => p.id === value('--profile')); if (!p) throw new Error('Neznan profil'); console.log(JSON.stringify(subscriptionStatus(p))); return; }
  if (command === 'serve') {
    const release = await lock(path.join(c.stateDirectory, 'dashboard'));
    try {
      const app = await createDashboard(c, configFile, Number(value('--port') || 0)); console.log(app.url);
      await new Promise(resolve => { process.once('SIGINT', resolve); process.once('SIGTERM', resolve); }); await app.close();
    } finally { await release(); } return;
  }
  if (command !== 'launch') throw new Error('Uporabi launch ali serve');
  fs.mkdirSync(c.stateDirectory, { recursive: true }); const uiFile = path.join(c.stateDirectory, 'ui.json');
  async function existing() {
    try { const v = readJson(uiFile); if (!alive(v.pid) || !/^http:\/\/127\.0\.0\.1:\d+$/.test(v.url)) return null; const r = await fetch(`${v.url}/health`, { signal: AbortSignal.timeout(1000) }); const health = await r.json(); return health.app === 'customnpc-overnight' && health.pid === v.pid ? v.url : null; } catch { return null; }
  }
  let url = await existing();
  if (!url) {
    const log = fs.openSync(path.join(c.stateDirectory, 'dashboard.log'), 'a');
    try { const child = spawn(process.execPath, [path.join(toolDirectory, 'dashboard.mjs'), 'serve', '--config', configFile], { cwd: c.projectRoot, detached: true, windowsHide: true, stdio: ['ignore', log, log] }); child.on('error', e => console.error(e.message)); child.unref(); }
    finally { fs.closeSync(log); }
    for (let i = 0; i < 50 && !url; i++) { await delay(100); url = await existing(); }
    if (!url) throw new Error('Vmesnik se ni zagnal; preveri .overnight/dashboard.log');
  }
  console.log(url);
  if (!args.includes('--no-open')) spawnSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', path.join(toolDirectory, 'ui-launch.ps1'), '-Mode', 'Open', '-Url', url], { windowsHide: true, stdio: 'inherit' });
}
if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) main().catch(e => { console.error(e.message); process.exitCode = 1; });
