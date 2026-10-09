#!/usr/bin/env node
// M5.10 - pogoj veljavnosti A/B na boju (D-030, raziskava M5.0 par. 3.4).
//
// Boj je sumen zaradi poteka boja, ne zaradi stroja: baseline boj-500 ima razpon p50 20 %, ena
// ponovitev S14 je imela alokacijo 36,7 namesto ~90 MB/s, torej drugacno kolicino sledenja poti.
// MSPT dveh vej je zato primerljiv samo, ce je sledenja poti v vseh zagonih priblizno enako:
// klici pathFollow in kandidatov (isDirectPathBetweenPoints) na tick morajo biti v vseh zagonih
// obeh vej znotraj +-10 % od skupne mediane. Sicer se primerja cas na klic, ne MSPT.
// Pogoj je nujen, ne zadosten: M5.10 (9. 10.) je pokazal 1 % razpona klicev in 14 % razpona MSPT,
// ker se med zagoni spreminja delo na klic (geometrija boja), ne stevilo klicev.
//
// Vhod: zapisi zagonov perf-run (JSON, meritve-lib) z velicinami rwpath.* - zagon mora biti
// pognan z -RwPath N -RwPathCas. Veje loci argument '--'.
//
//     node dev/tools/ab-boj.js audit/A-p1.json audit/A-p2.json audit/A-p3.json -- audit/B-p1.json ...
//     node dev/tools/ab-boj.js --prag 0.10 ...
//
// Izhodna koda: 0 = nujni pogoj izpolnjen, 3 = pogoj ni izpolnjen (primerjaj cas
// na klic), 2 = napaka vhoda.

'use strict';
const fs = require('fs');
const path = require('path');

const POGOJ = ['rwpath.sledenjNaTick', 'rwpath.kandidatovNaTick'];
const MSPT = ['mspt.povp', 'mspt.p50', 'mspt.p95', 'npc.us', 'alok.MBnaS'];
const NA_KLIC = ['rwpath.sledenje.usNaKlic', 'rwpath.kandidat.usNaKlic', 'rwpath.kandidatovNaSledenje',
    'rwpath.sledenje.delez', 'rwpath.kandidat.delez'];

function preberi(p) {
    const s = fs.readFileSync(p, 'utf8').replace(/^﻿/, '');
    const j = JSON.parse(s);
    if (!j.velicine) throw new Error(p + ': ni velicin (ni zapis meritve)');
    return { pot: p, ime: path.basename(p), odtis: j.odtis || {}, v: j.velicine, uspeh: j.uspeh };
}

function mediana(xs) {
    const s = xs.slice().sort((a, b) => a - b);
    const n = s.length;
    if (n === 0) return NaN;
    return n % 2 ? s[(n - 1) / 2] : (s[n / 2 - 1] + s[n / 2]) / 2;
}

function fmt(x) {
    if (x === undefined || x === null || Number.isNaN(x)) return '-';
    return Math.abs(x) >= 100 ? x.toFixed(0) : x.toFixed(2);
}

// Cista funkcija pogoja (izvozena za preverbo): vse vrednosti znotraj +-prag od skupne mediane.
function pogojVeljavnosti(vrednosti, prag) {
    const m = mediana(vrednosti);
    const odmiki = vrednosti.map((x) => (m > 0 ? Math.abs(x - m) / m : Infinity));
    return { mediana: m, najvecjiOdmik: Math.max(...odmiki), ok: odmiki.every((o) => o <= prag) };
}

function main(argv) {
    let prag = 0.10;
    const veje = [[]];
    for (let i = 0; i < argv.length; i++) {
        if (argv[i] === '--prag') { prag = Number(argv[++i]); continue; }
        if (argv[i] === '--') { veje.push([]); continue; }
        veje[veje.length - 1].push(argv[i]);
    }
    if (veje.length !== 2 || veje[0].length === 0 || veje[1].length === 0 || !(prag > 0)) {
        console.error('Uporaba: node dev/tools/ab-boj.js [--prag 0.10] A1.json [A2.json ...] -- B1.json [B2.json ...]');
        return 2;
    }
    const A = veje[0].map(preberi);
    const B = veje[1].map(preberi);
    const vsi = A.concat(B);
    for (const z of vsi) {
        for (const k of POGOJ) {
            if (typeof z.v[k] !== 'number') {
                console.error(z.ime + ': manjka ' + k + ' (zagon ni bil pognan z -RwPathCas)');
                return 2;
            }
        }
        if (!z.uspeh) console.log('POZOR: ' + z.ime + ' je oznacen kot neuspesen');
    }

    const nacin = (zs) => [...new Set(zs.map((z) => z.odtis.rwpath))].join(',');
    console.log('A: ' + A.length + ' zagonov (rwpath=' + nacin(A) + '), B: ' + B.length + ' zagonov (rwpath=' + nacin(B) + ')');
    console.log('');
    const glava = 'velicina'.padEnd(30) + 'A mediana'.padStart(11) + 'A razpon'.padStart(10) + 'B mediana'.padStart(11) + 'B razpon'.padStart(10) + 'B/A'.padStart(8);
    console.log(glava);
    console.log('-'.repeat(glava.length));
    for (const k of POGOJ.concat(MSPT, NA_KLIC)) {
        const a = A.map((z) => z.v[k]).filter((x) => typeof x === 'number');
        const b = B.map((z) => z.v[k]).filter((x) => typeof x === 'number');
        if (a.length === 0 && b.length === 0) continue;
        const ma = mediana(a), mb = mediana(b);
        const ra = a.length ? Math.max(...a) - Math.min(...a) : NaN;
        const rb = b.length ? Math.max(...b) - Math.min(...b) : NaN;
        const r = ma > 0 ? (mb / ma).toFixed(3) : '-';
        console.log(k.padEnd(30) + fmt(ma).padStart(11) + fmt(ra).padStart(10) + fmt(mb).padStart(11) + fmt(rb).padStart(10) + r.padStart(8));
    }
    console.log('');

    let ok = true;
    for (const k of POGOJ) {
        const p = pogojVeljavnosti(vsi.map((z) => z.v[k]), prag);
        const zunaj = vsi.filter((z) => Math.abs(z.v[k] - p.mediana) / p.mediana > prag).map((z) => z.ime + ' ' + fmt(z.v[k]));
        console.log((p.ok ? 'OK     ' : 'PADLO  ') + k + ': skupna mediana ' + fmt(p.mediana) + ', najvecji odmik '
            + (100 * p.najvecjiOdmik).toFixed(1) + ' % (prag ' + (100 * prag).toFixed(0) + ' %)'
            + (zunaj.length ? ' - zunaj: ' + zunaj.join(', ') : ''));
        ok = ok && p.ok;
    }
    console.log('');
    if (ok) {
        console.log('NUJNI POGOJ IZPOLNJEN: klicev sledenja poti je v vseh zagonih enako. Ni zadosten: v M5.10');
        console.log('je imel MSPT pri 1 % razponu klicev razpon 14 % (sum je v delu na klic) - glej razpon usNaKlic.');
        return 0;
    }
    console.log('POGOJ NI IZPOLNJEN: kolicina boja se razlikuje; primerjaj cas na klic (rwpath.*.usNaKlic), ne MSPT.');
    return 3;
}

if (require.main === module) {
    try {
        process.exitCode = main(process.argv.slice(2));
    } catch (e) {
        console.error(String(e && e.message ? e.message : e));
        process.exitCode = 2;
    }
}

module.exports = { pogojVeljavnosti, mediana };
