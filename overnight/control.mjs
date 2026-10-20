import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { readJson, writeJson, now, alive } from './lib.mjs';

export function publish(c, event) {
  writeJson(path.join(c.stateDirectory, 'events', `${crypto.randomUUID()}.json`), { ...event, at: now() });
}
export function importEvents(c, s) {
  const directory = path.join(c.stateDirectory, 'events');
  if (!fs.existsSync(directory)) return;
  for (const name of fs.readdirSync(directory).filter(v => v.endsWith('.json')).sort()) {
    const file = path.join(directory, name); const e = readJson(file);
    if (e.type === 'login' && c.profiles.some(p => p.id === e.id)) s.profiles[e.id] = { enabledOverride: true };
    if (e.type === 'verified' && c.profiles.some(p => p.id === e.id)) {
      const previous = s.profiles[e.id] || {};
      s.profiles[e.id] = { ...previous, enabledOverride: true, disabled: false, reason: previous.until ? previous.reason : undefined };
    }
    if (e.type === 'retry') {
      const task = s.tasks.find(t => t.id === e.id);
      if (task && ['blocked', 'waiting'].includes(task.status)) {
        task.status = 'queued'; task.retries = 0; task.failures = 0; task.iteration = 0;
        if (task.phase === 'tests') { task.testIndex = 0; task.sourceFingerprint = null; }
      }
    }
    // Persist before acknowledgement: event replay is idempotent.
    writeJson(path.join(c.stateDirectory, 'state.json'), s); fs.unlinkSync(file);
  }
}
export function running(c) {
  try { const value = readJson(path.join(c.stateDirectory, 'controller.json')); return alive(value.pid) ? value : null; } catch { return null; }
}
export function cancelActive(s) {
  if (s.active) fs.writeFileSync(path.join(s.active.directory, 'CANCEL'), now());
}
export function interrupted(s, result) {
  const task = s.tasks.find(t => t.id === s.active?.taskId);
  if (!task) return;
  task.status = 'queued'; task.failures = 0;
  task.handoff = `${task.handoff}\nOperation interrupted by owner. Inspect partial edits and git diff before resuming. Completed test cells remain valid only for their source fingerprint.`.slice(-16000);
  if (s.active.type === 'agent') task.phase = 'agent';
  task.summary = result.message || 'Prekinjeno; napredek shranjen';
}
