// M5-K K14 (19. 10.): pregled prozornosti pokrivala v skinih. Uporaba: node skini-pokrivalo.js <png> [<png> ...] (VERBOSE=1 izpise vsak skin)
// Za vsak PNG skin: koliko od 6 delov pokrivala (ModelHeadwear, uv 8x8 pri y 0..16, x 32..64)
// ima vsaj en neprozoren teksel (alfa > 0) in koliko trakov ekstruzije (stolpcev/vrstic) je ves prozoren.
const fs = require('fs'), path = require('path'), zlib = require('zlib');
function png(buf) {
    let o = 8, w, h, bd, ct, idat = [], pal = null, trns = null;
    while (o < buf.length) {
        const len = buf.readUInt32BE(o), type = buf.toString('ascii', o + 4, o + 8), d = buf.slice(o + 8, o + 8 + len);
        if (type === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); bd = d[8]; ct = d[9]; if (d[12]) throw 'interlace'; }
        else if (type === 'IDAT') idat.push(d);
        else if (type === 'PLTE') pal = d;
        else if (type === 'tRNS') trns = d;
        o += 12 + len;
    }
    if (bd !== 8) throw 'bitdepth ' + bd;
    const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ct];
    const raw = zlib.inflateSync(Buffer.concat(idat)), stride = w * ch, out = Buffer.alloc(h * stride);
    for (let y = 0; y < h; y++) {
        const f = raw[y * (stride + 1)], line = raw.slice(y * (stride + 1) + 1, (y + 1) * (stride + 1));
        for (let x = 0; x < stride; x++) {
            const a = x >= ch ? out[y * stride + x - ch] : 0, b = y ? out[(y - 1) * stride + x] : 0, c = (x >= ch && y) ? out[(y - 1) * stride + x - ch] : 0;
            let v = line[x];
            if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
            else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c); }
            out[y * stride + x] = v & 255;
        }
    }
    const alpha = (x, y) => {
        const i = y * stride + x * ch;
        if (ct === 6) return out[i + 3]; if (ct === 4) return out[i + 1];
        if (ct === 3) return trns && out[i] < trns.length ? trns[out[i]] : 255;
        return 255;
    };
    return { w, h, alpha };
}
const parts = [[32, 8], [48, 8], [40, 8], [56, 8], [40, 0], [48, 0]];
const files = process.argv.slice(2);
let tot = 0, prazni = 0, polni = 0, trakov = 0, praznihTrakov = 0, delov = 0, praznihDelov = 0;
for (const f of files) {
    let im; try { im = png(fs.readFileSync(f)); } catch (e) { continue; }
    if (im.w < 64 || im.h < 16) continue;
    const s = im.w / 64; // HD skini
    let vidnihDelov = 0;
    for (const [px, py] of parts) {
        let any = false;
        const col = new Array(8).fill(false), row = new Array(8).fill(false);
        for (let ty = 0; ty < 8; ty++) for (let tx = 0; tx < 8; tx++) {
            let a = 0;
            for (let yy = 0; yy < s; yy++) for (let xx = 0; xx < s; xx++) a = Math.max(a, im.alpha((px + tx) * s + xx, (py + ty) * s + yy));
            if (a > 0) { any = true; col[tx] = true; row[ty] = true; }
        }
        delov++; if (!any) praznihDelov++; else vidnihDelov++;
        // ekstruzija: 2 x 8 stolpcev in 2 x 8 vrstic
        for (let k = 0; k < 8; k++) { trakov += 4; if (!col[k]) praznihTrakov += 2; if (!row[k]) praznihTrakov += 2; }
    }
    tot++;
    if (vidnihDelov === 0) prazni++;
    if (vidnihDelov === 6) polni++;
    if (process.env.VERBOSE) console.log(vidnihDelov, path.basename(f));
}
console.log(`skinov ${tot}: brez vidnega pokrivala ${prazni}, vseh 6 delov vidnih ${polni}; delov praznih ${praznihDelov}/${delov}; trakov ekstruzije praznih ${praznihTrakov}/${trakov}`);
