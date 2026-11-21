// M12.1 controller-owned asynchronous loopback fixture. No external network or dependencies.
// node url-skin-fixture.js <directory>; ready.json contains an ephemeral loopback port.
'use strict';
const fs = require('fs');
const http = require('http');
const zlib = require('zlib');
const crypto = require('crypto');
const dir = process.argv[2];
if (!dir || !fs.statSync(dir).isDirectory()) throw Error('existing fixture directory required');
const path = require('path');
const log = o => fs.appendFileSync(path.join(dir, 'http.jsonl'), JSON.stringify({...o, time: Date.now()}) + '\n');
const crc = b => { let c = -1; for (const v of b) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0); } return (c ^ -1) >>> 0; };
const chunk = (t, b) => { const data = Buffer.concat([Buffer.from(t), b]); const a = Buffer.alloc(4), c = Buffer.alloc(4); a.writeUInt32BE(b.length); c.writeUInt32BE(crc(data)); return Buffer.concat([a, data, c]); };
function png(w, h, alpha = false, noise = false) {
    const head = Buffer.alloc(13); head.writeUInt32BE(w); head.writeUInt32BE(h, 4); head[8] = 8; head[9] = 6;
    const raw = Buffer.alloc(h * (w * 4 + 1));
    let rng = 123456789;
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
        const i = y * (w * 4 + 1) + 1 + x * 4, a = ((x >> 2) + (y >> 2)) % 2;
        raw[i] = a ? 224 : 32; raw[i + 1] = a ? 48 : 208; raw[i + 2] = a ? 144 : 224;
        raw[i + 3] = alpha && x % 8 < 4 ? 80 : 255;
        if (noise) for (let k=0;k<4;k++) { rng ^= rng << 13; rng ^= rng >>> 17; rng ^= rng << 5; raw[i+k] = rng & 255; }
    }
    return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', head), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const files = {normal: png(64,64), legacy: png(64,32), hd: png(512,512), alpha: png(64,64,true), rectangle: png(128,64), oversized: png(2049,64), bigbody: png(1536,1536,false,true)};
if (files.bigbody.length <= 8*1024*1024) throw Error('bigbody must exceed the proposed encoded-byte limit');
const manifest = {};
for (const [key, body] of Object.entries(files)) {
    fs.writeFileSync(path.join(dir, key + '.png'), body);
    manifest[key] = {bytes: body.length, sha256: crypto.createHash('sha256').update(body).digest('hex')};
}
fs.writeFileSync(path.join(dir, 'manifest.json'), JSON.stringify(manifest, null, 2));
let active = 0, peak = 0, seq = 0;
const sockets = new Set(), timers = new Set();
function later(fn, ms) { const t = setTimeout(() => { timers.delete(t); fn(); }, ms); timers.add(t); }
const server = http.createServer((req,res) => {
    const id = ++seq, start = Date.now(); let bytes = 0;
    active++; peak = Math.max(peak, active); log({event:'start',id,url:req.url,active,peak});
    res.on('close', () => { active--; log({event:'end',id,url:req.url,status:res.statusCode,bytes,elapsed:Date.now()-start,active,peak}); });
    const mode = req.url.split('/')[2];
    function send(status, body) {
        if (res.destroyed) return;
        res.writeHead(status, {'Content-Type':'image/png','Content-Length':body.length,'Cache-Control':'public, max-age=86400'});
        bytes += body.length; res.end(body);
    }
    if (mode === 'missing') { send(404, Buffer.from('missing')); return; }
    if (mode === 'invalid') { send(200, Buffer.from('not an image')); return; }
    if (mode === 'redirect') { res.writeHead(302, {Location:req.url}); res.end(); return; }
    if (mode === 'headers' || mode === 'body') {
        if (mode === 'body') { res.writeHead(200, {'Content-Type':'image/png','Content-Length':files.normal.length}); res.flushHeaders(); }
        // Cleanup only AFTER the harness's 20s observation. The unchanged original has no timeout.
        later(() => {
            log({event:'fixture-close',id,url:req.url});
            if (mode === 'headers') {
                // An EOF before any HTTP headers makes JDK8 transparently retry
                // GET and hang for another 30s. End this cleanup with a complete
                // error response and close the socket. Nothing is sent before
                // the original 30s deadline; the 20s observation stays unchanged.
                res.writeHead(503, {'Content-Length':0,'Connection':'close'});
                res.end();
            } else { res.destroy(); }
        }, 30000); return;
    }
    if (mode === 'trickle') {
        res.writeHead(200, {'Content-Type':'image/png','Content-Length':files.normal.length}); res.flushHeaders();
        let n = 0;
        const drip = () => { if (res.destroyed) return; if (n < files.normal.length) { res.write(files.normal.subarray(n,n+1)); bytes++; n++; later(drip,1000); } else res.end(); };
        drip(); later(() => { log({event:'fixture-close',id,url:req.url}); res.destroy(); },30000); return;
    }
    later(() => send(200, files[mode] || files.normal), mode === 'slow' ? 3500 : 1500);
});
server.on('connection', s => { sockets.add(s); s.on('close', () => sockets.delete(s)); });
server.listen(0, '127.0.0.1', () => fs.writeFileSync(path.join(dir, 'ready.json'), JSON.stringify({port:server.address().port,delayMs:1500,hangCloseMs:30000})));
const stop = setInterval(() => {
    if (!fs.existsSync(path.join(dir, 'stop'))) return;
    clearInterval(stop); for (const t of timers) clearTimeout(t); for (const s of sockets) s.destroy();
    server.close(() => { log({event:'stopped',active,peak}); });
},100);
