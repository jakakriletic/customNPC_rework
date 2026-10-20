const $ = id => document.getElementById(id);
const token = document.querySelector('meta[name="overnight-token"]').content;
let current; let pending = false; let timer; let toastTimer; let rendered = '';
const labels = { queued: 'V vrsti', running: 'V teku', done: 'Zaključeno', blocked: 'Odloženo', waiting: 'Čaka na novo delo' };
const time = value => new Date(value).toLocaleString('sl-SI', { day: 'numeric', month: 'numeric', hour: '2-digit', minute: '2-digit' });
function element(tag, className, value) { const el = document.createElement(tag); if (className) el.className = className; if (value !== undefined) el.textContent = value; return el; }
function toast(message, error = false) { $('toast').textContent = message; $('toast').className = error ? 'error' : ''; $('toast').hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => { $('toast').hidden = true; }, error ? 10000 : 6000); }
async function api(route, data) {
  const r = await fetch(`/api/${route}`, { method: data === undefined ? 'GET' : 'POST', headers: { 'X-Overnight-Token': token, ...(data === undefined ? {} : { 'Content-Type': 'application/json' }) }, ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
  const value = await r.json(); if (!r.ok) throw new Error(value.error || 'Zahteva ni uspela'); return value;
}
async function command(route, data = {}) {
  if (pending && route !== 'stop') return;
  pending = true; renderControls();
  try { const value = await api(route, data); toast(value.message); await refresh(); return true; }
  catch (e) { toast(e.message, true); return false; }
  finally { pending = false; renderControls(); }
}
function renderControls() {
  if (!current) return;
  const running = Boolean(current.controller || current.starting);
  $('start').disabled = running || pending;
  $('stop').disabled = (!running && !current.active) || current.stopping;
  $('pause').disabled = !running || current.stopping || pending;
  $('pause').textContent = current.paused ? '▶  Nadaljuj' : 'Ⅱ  Pavza po koraku';
  $('probe').disabled = pending || current.probing || running || Boolean(current.active);
  $('probe').textContent = current.probing ? 'Preverjam …' : 'Preveri prijave ↻';
}
function render(s) {
  current = s; $('connection').textContent = 'Lokalna povezava'; $('connection').className = 'connection';
  const signature = JSON.stringify(s);
  if (signature === rendered) { renderControls(); return; }
  rendered = signature;
  const task = s.tasks.find(t => t.id === s.active?.taskId) || s.tasks.find(t => ['queued', 'running'].includes(t.status));
  const running = Boolean(s.controller);
  $('status-tag').textContent = s.stopping ? 'USTAVLJANJE' : s.paused && running ? 'PAVZA PO KORAKU' : running ? s.active ? 'DELO POTEKA' : 'ČAKANJE' : 'PRIPRAVLJENO';
  $('status-tag').className = `status-tag ${s.stopping ? 'stopping' : running ? 'running' : ''}`;
  $('work-heading').textContent = task?.title || (running ? 'Izbiram naslednji projektni korak' : 'Pripravljeno za naslednjo izmeno');
  $('work-message').textContent = s.message || 'Pripravljeno';
  $('run-time').textContent = s.controller ? `Začetek ${time(s.controller.startedAt)}` : 'Ni časovne omejitve';
  const count = task?.tests.length || 0; const index = task?.testIndex || 0;
  $('progress-bar').style.width = `${count ? Math.min(index / count * 100, 100) : 0}%`;
  $('progress-label').textContent = count ? `${Math.min(index, count)} / ${count} testnih korakov · ${task.iteration} AI iteracij` : task ? `${task.iteration} AI iteracij` : 'Čakam na zagon';
  $('phase-label').textContent = s.active?.testId || (s.active?.profileId ? `AI · ${s.active.profileId}` : running ? 'Čakanje na naslednji korak' : 'Ustavljeno');
  $('permission-label').textContent = s.autonomy?.fullAccess ? 'Dostopi brez potrditev' : 'Omejeni CLI dostopi';
  $('stat-done').textContent = s.tasks.filter(t => t.status === 'done').length;
  $('stat-tests').textContent = s.tasks.reduce((sum, t) => sum + t.results.filter(r => r.success).length, 0);
  $('stat-decisions').textContent = (s.questions || []).length;
  $('stat-queue').textContent = s.tasks.filter(t => ['queued', 'running'].includes(t.status)).length;
  const profiles = s.profiles.map((p, i) => {
    const card = element('article', 'profile'); const header = element('div', 'profile-header');
    header.append(element('span', `profile-logo ${p.kind}`, p.kind === 'claude' ? '✳' : '◎'));
    const title = element('div'); title.append(element('h3', '', p.kind === 'codex' ? 'Codex' : `Claude ${p.id.endsWith('2') ? '2' : '1'}`), element('div', 'provider', `${String(i + 1).padStart(2, '0')} · ${p.kind === 'codex' ? 'ChatGPT naročnina' : p.isolated ? 'Ločen Claude profil' : 'Privzeti Claude profil'}`)); header.append(title);
    const quota = p.health.until && Date.parse(p.health.until) > Date.now();
    const authMissing = p.health.disabled || p.auth.ok === false;
    const text = s.active?.profileId === p.id ? 'Trenutno dela' : quota ? `Kvota do ${time(p.health.until)}${p.health.exact ? '' : ' · ocena'}` : authMissing ? 'Potrebna prijava' : p.auth.ok ? 'Prijava potrjena' : !p.enabled ? 'Še ni povezan' : 'Preveri prijavo';
    card.append(header, element('div', `profile-status ${authMissing ? 'error' : ''}`, text), element('p', 'profile-reason', quota ? p.health.reason || 'Čakam na obnovitev kvote' : p.health.disabled ? p.health.reason : p.auth.reason));
    const login = element('button', 'secondary', 'Prijava ↗'); login.disabled = Boolean(s.active); login.onclick = () => command('login', { id: p.id }); card.append(login); return card;
  }); $('profiles').replaceChildren(...profiles);
  const tasks = [...s.tasks].reverse().slice(0, 30).map((t, i) => {
    const row = element('article', 'task'); row.append(element('span', 'task-number', String(s.tasks.length - i).padStart(2, '0')));
    const content = element('div', 'task-content'); content.append(element('h3', '', t.title), element('p', '', t.summary || t.goal), element('div', 'task-meta', `${time(t.createdAt)} · ${t.iteration} AI iteracij · ${t.results.length} testnih zapisov`));
    const right = element('div'); right.append(element('span', `task-badge ${t.status}`, labels[t.status] || t.status));
    if (['blocked', 'waiting'].includes(t.status)) { const retry = element('button', 'retry', 'Poskusi znova ↻'); retry.onclick = () => command('retry', { id: t.id }); right.append(retry); }
    row.append(content, right); return row;
  }); $('tasks').replaceChildren(...(tasks.length ? tasks : [element('div', 'empty', 'Vrsta je prazna. Ob zagonu AI izbere naslednji korak iz projektnega načrta.')]));
  const questions = [...(s.questions || [])].reverse().slice(0, 40).map(q => {
    const item = element('article', 'decision-entry'); item.append(element('small', '', `${time(q.at)} · ${q.profileId} · ${q.taskTitle}`), element('h3', '', q.question));
    const answer = element('p'); answer.append(element('strong', '', 'Odločitev: '), document.createTextNode(q.decision)); item.append(answer, element('p', '', q.rationale)); return item;
  }); $('decision-count').textContent = (s.questions || []).length;
  $('questions').replaceChildren(...(questions.length ? questions : [element('div', 'empty', 'Tu se bodo pojavile odločitve, ki jih AI sprejme med delom. Vprašanj ti med izvajanjem ne bo treba potrjevati.')]));
  $('project-path').textContent = s.project; renderControls();
}
async function refresh() {
  try { render(await api('status')); }
  catch (e) { $('connection').textContent = 'Povezava prekinjena'; $('connection').className = 'connection offline'; $('start').disabled = true; $('work-message').textContent = `${e.message}. Ponovno odpri upravljalnik, če se povezava ne obnovi.`; }
}
async function logs() { try { const value = await api('log'); $('log-content').textContent = [value.controller, value.stdout, value.stderr].filter(Boolean).join('\n\n') || 'Za zdaj ni izpisov.'; } catch (e) { toast(e.message, true); } }
$('start').onclick = () => command('start'); $('stop').onclick = () => command('stop');
$('pause').onclick = () => command(current?.paused ? 'resume' : 'pause'); $('probe').onclick = () => command('probe');
$('add-toggle').onclick = () => { $('add-form').hidden = !$('add-form').hidden; $('add-toggle').setAttribute('aria-expanded', !$('add-form').hidden); if (!$('add-form').hidden) $('goal').focus(); };
$('add-cancel').onclick = () => { $('add-form').hidden = true; $('add-toggle').setAttribute('aria-expanded', 'false'); };
$('add-form').onsubmit = async e => { e.preventDefault(); if (await command('add', { goal: $('goal').value })) { $('goal').value = ''; $('add-form').hidden = true; $('add-toggle').setAttribute('aria-expanded', 'false'); } };
$('logs').ontoggle = () => { if ($('logs').open) logs(); }; $('refresh-log').onclick = logs;
async function poll() { clearTimeout(timer); await refresh(); timer = setTimeout(poll, document.hidden ? 15000 : 3000); }
document.addEventListener('visibilitychange', () => { if (!document.hidden) poll(); }); poll();
