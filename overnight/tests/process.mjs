import fs from 'node:fs';
const [, , mode, file] = process.argv;
if (mode === 'success') { console.log('Synthetic deterministic test PASS'); }
else if (mode === 'fail') { console.error('Synthetic deterministic test FAIL'); process.exitCode = 1; }
else if (mode === 'change') { fs.appendFileSync(file, '\nchanged by synthetic test'); console.log('PASS but input changed'); }
else if (mode === 'delay') { await new Promise(r => setTimeout(r, 1500)); fs.writeFileSync(file, 'finished'); console.log('Detached synthetic test PASS'); }
else if (mode === 'launch') {
  const { spawn } = await import('node:child_process');
  const child = spawn(process.execPath, [new URL('../worker.mjs', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'), file], { detached: true, windowsHide: true, stdio: 'ignore' });
  child.unref();
}
else throw new Error('Unknown fixture mode');
