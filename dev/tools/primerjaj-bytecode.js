#!/usr/bin/env node
// Primerja prevedene razrede z originalnim bytecode (prenos brez funkcionalnih sprememb).
// Node razlicica primerjaj-bytecode.py (M3.8b, 5. 10.: na uporabnikovem racunalniku ni Pythona);
// pravila normalizacije in izpis so enaki.
//
// Za vsak razred primerja metode po normaliziranem `javap -c`: indeksi konstant, cilji skokov
// in stevilke lokalnih spremenljivk se ne stejejo. Metode, ki se razlikujejo:
//   oblika     isti multiset 'pomenskih' ukazov - razlika prevajalnika, ne obnasanja
//   PREGLED    razlicen multiset - rocni pregled je obvezen, izid gre v audit/
//
// Uporaba:
//   node primerjaj-bytecode.js <orig-dir|jar> <nov-dir> <razred> [<razred> ...]
// Potrebuje `javap` (JDK 8+) na PATH ali v okoljski spremenljivki JAVAP.
'use strict';
const { execFileSync } = require('child_process');

const KEEP = new RegExp('^(invoke\\w+|getfield|putfield|getstatic|putstatic|new|anewarray|newarray|instanceof|ldc\\w*|' +
    '[bs]ipush|[ifld]const_\\w+|aconst_null|[ifld](add|sub|mul|div|rem|neg|shl|shr|ushr|and|or|xor)|' +
    '[ifld]2[ifldbcs]|athrow|arraylength|[ifldabcs]aload|[ifldabcs]astore|iinc|[fd]cmp[lg]|lcmp|monitor\\w+)');

function metode(cp, cls) {
    const out = execFileSync(process.env.JAVAP || 'javap', ['-c', '-p', '-cp', cp, cls], { encoding: 'utf8', maxBuffer: 64 << 20 });
    const m = new Map();
    let cur = null;
    for (const line of out.replace(/\r/g, '').split('\n')) {
        const h = /^  (\S.*?);?$/.exec(line);
        if (h && !line.startsWith('    ')) {
            cur = h[1];
            m.set(cur, []);
            continue;
        }
        if (cur && /^\s+\d+: /.test(line)) {
            let i = line.replace(/^\s+\d+: /, '');
            i = i.replace(/#\d+(, *\d+)?\s*/g, '');
            i = i.replace(/\b(goto|if\w*|jsr)\s+\d+/g, '$1 L');
            i = i.replace(/(tableswitch|lookupswitch).*/, '$1');
            m.get(cur).push(i.trim());
        }
    }
    return m;
}

function pomen(ins) {
    const c = new Map();
    for (let i of ins) {
        if (!KEEP.test(i)) continue;
        i = i.replace('invokeinterface', 'invokevirtual');
        i = i.replace(/^ldc\w*\s+/, 'ldc ');
        i = i.replace(/^iinc\s+\d+,\s*/, 'iinc ');
        c.set(i, (c.get(i) || 0) + 1);
    }
    return c;
}

function razlika(x, y) {
    const d = {};
    for (const [k, v] of x) {
        const r = v - (y.get(k) || 0);
        if (r > 0) d[k] = r;
    }
    return d;
}

function enak(a, b) {
    if (!a || !b) return a === b;
    return a.length === b.length && a.every((v, i) => v === b[i]);
}

function main(argv) {
    if (argv.length < 3) {
        console.log('uporaba: node primerjaj-bytecode.js <orig-dir|jar> <nov-dir> <razred> [<razred> ...]');
        return 2;
    }
    const [orig, nov, ...razredi] = argv;
    let vse = 0, enake = 0, oblika = 0;
    const pregled = [];
    for (const cls of razredi) {
        const a = metode(orig, cls), b = metode(nov, cls);
        for (const m of [...new Set([...a.keys(), ...b.keys()])].sort()) {
            vse++;
            if (enak(a.get(m), b.get(m))) { enake++; continue; }
            const x = pomen(a.get(m) || []), y = pomen(b.get(m) || []);
            const minus = razlika(x, y), plus = razlika(y, x);
            if (Object.keys(minus).length === 0 && Object.keys(plus).length === 0) { oblika++; continue; }
            pregled.push([cls, m, minus, plus]);
        }
    }
    console.log(`razredov ${razredi.length}, metod ${vse}: enakih ${enake}, razlika v obliki ${oblika}, za PREGLED ${pregled.length}`);
    for (const [cls, m, minus, plus] of pregled) {
        console.log(`PREGLED ${cls} :: ${m}`);
        console.log(`    samo original: ${JSON.stringify(minus)}`);
        console.log(`    samo nov:      ${JSON.stringify(plus)}`);
    }
    return 0;
}

process.exit(main(process.argv.slice(2)));
