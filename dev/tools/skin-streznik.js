#!/usr/bin/env node
// M5-V.7 (D-050): lokalni HTTP streznik skina za celico "URL skin" (k-run.ps1 -Posebni 1).
//
// Na vsako zahtevo GET odgovori z enobarvnim skinom 64 x 64 (PNG, privzeto magenta FF00FF), a sele po
// zamiku (privzeto 4000 ms). Zamik zagotovi, da peka impostorja naleti na prenos v teku: brez cakanja
// na prenos (stanje pred M5-V.7) bi spekla privzeti skin, s cakanjem barvo prenesenega skina.
// Vsako zahtevo zapise kot vrstico v dnevnik (cas, pot, zamik).
//
// Uporaba: node dev/tools/skin-streznik.js <port> <dnevnik.txt> [zamikMs] [RRGGBB]
'use strict';
const fs = require('fs');
const http = require('http');
const zlib = require('zlib');

const port = parseInt(process.argv[2], 10);
const log = process.argv[3];
const delay = process.argv[4] ? parseInt(process.argv[4], 10) : 4000;
const color = process.argv[5] || 'FF00FF';
if (!(port > 0) || !log || !/^[0-9A-Fa-f]{6}$/.test(color)) {
    console.error('uporaba: node skin-streznik.js <port> <dnevnik.txt> [zamikMs] [RRGGBB]');
    process.exit(2);
}

const CRC = (() => {
    const t = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
        t[n] = c;
    }
    return t;
})();
function crc32(buf) {
    let c = -1;
    for (const b of buf) c = CRC[(c ^ b) & 0xFF] ^ (c >>> 8);
    return (c ^ -1) >>> 0;
}
function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(td));
    return Buffer.concat([len, td, crc]);
}
function png(w, h, rgb) {
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(w, 0);
    ihdr.writeUInt32BE(h, 4);
    ihdr[8] = 8;   // bitov na kanal
    ihdr[9] = 6;   // RGBA
    const raw = Buffer.alloc(h * (1 + w * 4));
    for (let y = 0; y < h; y++) {
        const row = y * (1 + w * 4);
        raw[row] = 0;
        for (let x = 0; x < w; x++) {
            const i = row + 1 + x * 4;
            raw[i] = rgb[0]; raw[i + 1] = rgb[1]; raw[i + 2] = rgb[2]; raw[i + 3] = 255;
        }
    }
    return Buffer.concat([Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
        chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

const body = png(64, 64, [0, 2, 4].map((i) => parseInt(color.substr(i, 2), 16)));
fs.writeFileSync(log, '');
http.createServer((req, res) => {
    const t0 = Date.now();
    setTimeout(() => {
        res.writeHead(200, { 'Content-Type': 'image/png', 'Content-Length': body.length });
        res.end(body);
        fs.appendFileSync(log, `${new Date(t0).toISOString()} ${req.method} ${req.url} zamik=${Date.now() - t0}\n`);
    }, delay);
}).listen(port, '127.0.0.1', () => {
    fs.appendFileSync(log, `POSLUSA 127.0.0.1:${port} zamik=${delay} barva=${color}\n`);
});
