import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
const [, , mode, file] = process.argv;
if (mode === 'success') { console.log('Synthetic deterministic test PASS'); }
else if (mode === 'fail') { console.error('Synthetic deterministic test FAIL'); process.exitCode = 1; }
else if (mode === 'change') { fs.appendFileSync(file, '\nchanged by synthetic test'); console.log('PASS but input changed'); }
else if (mode === 'delay') { await new Promise(r => setTimeout(r, 1500)); fs.writeFileSync(file, 'finished'); console.log('Detached synthetic test PASS'); }
else if (mode === 'launch') {
  const { spawn } = await import('node:child_process');
  const child = spawn(process.execPath, [fileURLToPath(new URL('../worker.mjs', import.meta.url)), file], { detached: true, windowsHide: true, stdio: 'ignore' });
  child.unref();
}
else if (mode === 'tree') {
  const { spawn } = await import('node:child_process');
  const child = spawn(process.execPath, ['-e', 'setInterval(()=>{},1000)'], { windowsHide: true });
  fs.writeFileSync(file, JSON.stringify({ parent: process.pid, child: child.pid }));
  setInterval(() => {}, 1000);
}
else if (mode === 'orphan') {
  const { spawn } = await import('node:child_process'); const { processJob } = await import('../lib.mjs');
  const release = await processJob();
  // The middle process exits at once, so the grandchild loses its parent like an abandoned tool search.
  const middle = spawn(process.execPath, ['-e', "const c=require('node:child_process').spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{detached:true,windowsHide:true,stdio:'ignore'});c.unref();console.log(c.pid)"], { windowsHide: true, stdio: ['ignore', 'pipe', 'ignore'] });
  let out = ''; middle.stdout.on('data', d => { out += d; }); await new Promise(r => middle.once('close', r));
  fs.writeFileSync(file, JSON.stringify({ middle: middle.pid, orphan: Number(out.trim()) }));
  await release();
}
else throw new Error('Unknown fixture mode');
