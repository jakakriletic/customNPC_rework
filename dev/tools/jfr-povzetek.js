#!/usr/bin/env node
// M5-S P1 (8. 10.): povzetek JFR posnetka strezniske meritve (perf-run.ps1 -Jfr).
//
// Zakaj: 09-PERFORMANCE-RAZISKAVA.md zahteva profil klicnega sklada, preden se izbere naslednji
// strezniski paket (S3, S4, S8 ...). rwdiag pove, KOLIKO stane tick in alokacija, ne pa KJE.
//
// Kaj izpise (za eno nit, privzeto 'Server thread'):
//   vzorci   jdk.ExecutionSample: lastni cas po metodi (vrh sklada), vkljucni cas po metodi
//            (vsaka metoda enkrat na sklad) za noppes.* in net.minecraft.*, ter lastni cas po
//            prvem okvirju iz noppes.* (koliko CPU-ja je 'pod' posamezno CNPC metodo)
//   alokacije jdk.ObjectAllocationInNewTLAB (utez tlabSize) + OutsideTLAB (utez allocationSize):
//            po razredu objekta, po prvem ne-JDK okvirju in vkljucno po noppes.* metodi.
//            JFR alokacije vzorci (nov TLAB), zato so deleži ocena, ne stevilo bajtov.
//
// Vzorec ExecutionSample nastane samo, ko nit izvaja Javo; cakanje med ticki se ne steje.
// Delez je zato delez CPU casa niti, ne delez ticka.
//
// Uporaba:
//   node dev/tools/jfr-povzetek.js <posnetek.jfr> [--nit "Server thread"] [--top 25] [--md izhod.md]
//   node dev/tools/jfr-povzetek.js <posnetek.jfr> --klicatelji java.util.HashMap$TreeNode.root [--globina 4]
//            verige klicateljev metode (CPU vzorci), da se vrh sklada poveze s kodo
// jfr.exe: okoljska JFR ali .tools/jdk8/bin/jfr.exe v korenu repozitorija.
'use strict';
const fs = require('fs');
const path = require('path');
const readline = require('readline');
const { spawn, execFileSync } = require('child_process');

function args() {
    const a = process.argv.slice(2);
    const o = { file: null, nit: 'Server thread', top: 25, md: null, klicatelji: null, globina: 4 };
    for (let i = 0; i < a.length; i++) {
        if (a[i] === '--nit') o.nit = a[++i];
        else if (a[i] === '--top') o.top = parseInt(a[++i], 10);
        else if (a[i] === '--md') o.md = a[++i];
        else if (a[i] === '--klicatelji') o.klicatelji = a[++i];
        else if (a[i] === '--globina') o.globina = parseInt(a[++i], 10);
        else o.file = a[i];
    }
    if (!o.file) {
        console.error('Uporaba: node jfr-povzetek.js <posnetek.jfr> [--nit "Server thread"] [--top 25] [--md izhod.md]');
        process.exit(2);
    }
    return o;
}

const JFR = process.env.JFR || path.join(__dirname, '..', '..', '.tools', 'jdk8', 'bin', 'jfr.exe');
// Brez tega jfr izpise '3,3 MB' (slovenska decimalna vejica).
const LOCALE = ['-J-Duser.language=en', '-J-Duser.country=US'];

// 'noppes.npcs.ai.EntityAIWander.shouldExecute() line: 12' -> metoda brez parametrov in vrstica
function frame(line) {
    const t = line.trim();
    const p = t.indexOf('(');
    const m = p > 0 ? t.substring(0, p) : t;
    const l = /line: (\d+)/.exec(t);
    return { m, at: l ? m + ':' + l[1] : m };
}

const JDK = /^(java|javax|sun|jdk|com\.sun)\./;
const isNoppes = (m) => m.startsWith('noppes.');
const isMc = (m) => m.startsWith('net.minecraft.') || m.startsWith('net.minecraftforge.');

function bytes(s) {
    const m = /([\d.]+)\s*(bytes|kB|MB|GB)/.exec(s);
    if (!m) return 0;
    const k = { bytes: 1, kB: 1024, MB: 1024 * 1024, GB: 1024 * 1024 * 1024 }[m[2]];
    return parseFloat(m[1]) * k;
}

// Prebere dogodke iz 'jfr print' po vrsticah in za vsakega poklice fn({tip, polja, sklad}).
function beri(file, events, fn) {
    return new Promise((resolve, reject) => {
        const p = spawn(JFR, [...LOCALE, 'print', '--events', events, '--stack-depth', '64', file]);
        const rl = readline.createInterface({ input: p.stdout });
        let cur = null, inStack = false;
        rl.on('line', (line) => {
            if (!cur) {
                const h = /^(jdk\.\w+) \{$/.exec(line);
                if (h) { cur = { tip: h[1], polja: {}, sklad: [] }; inStack = false; }
                return;
            }
            if (line === '}') { fn(cur); cur = null; return; }
            if (inStack) {
                if (line.trim() === ']') { inStack = false; return; }
                if (line.trim() !== '...') cur.sklad.push(frame(line));
                return;
            }
            const f = /^  (\w+) = (.*)$/.exec(line);
            if (f) {
                if (f[1] === 'stackTrace' && f[2] === '[') inStack = true;
                else cur.polja[f[1]] = f[2];
            }
        });
        let err = '';
        p.stderr.on('data', (d) => { err += d; });
        p.on('error', reject);
        p.on('close', (code) => code === 0 ? resolve() : reject(new Error('jfr print: ' + code + ' ' + err)));
    });
}

const nitIme = (s) => { const m = /^"([^"]*)"/.exec(s || ''); return m ? m[1] : ''; };
const add = (map, k, v) => map.set(k, (map.get(k) || 0) + v);
const urejeno = (map) => [...map.entries()].sort((a, b) => b[1] - a[1]);

async function main() {
    const o = args();
    const sum = execFileSync(JFR, [...LOCALE, 'summary', o.file], { encoding: 'utf8' });
    const durM = /Duration: (\d+) s/.exec(sum);
    const sekund = durM ? parseInt(durM[1], 10) : 0;

    if (o.klicatelji) {
        // Verige klicateljev metode (prvi okvir od vrha, ki se zacne z danim nizom), globina o.globina.
        let vsi = 0, zadetkov = 0;
        const verige = new Map();
        await beri(o.file, 'jdk.ExecutionSample', (e) => {
            if (nitIme(e.polja.sampledThread) !== o.nit) return;
            vsi++;
            const i = e.sklad.findIndex((f) => f.m.startsWith(o.klicatelji));
            if (i < 0) return;
            zadetkov++;
            add(verige, e.sklad.slice(i, i + 1 + o.globina).map((f) => f.at).join(' <- '), 1);
        });
        console.log(`# Klicatelji ${o.klicatelji}: ${zadetkov} od ${vsi} vzorcev niti ${o.nit}\n`);
        for (const [k, v] of urejeno(verige).slice(0, o.top)) console.log(`${v}\t${(100 * v / vsi).toFixed(1)} %\t${k}`);
        return;
    }

    // --- vzorci CPU ---
    let vzorcev = 0, vsehVzorcev = 0, drugeNiti = new Map();
    const self = new Map(), selfAt = new Map(), incl = new Map(), podNoppes = new Map();
    await beri(o.file, 'jdk.ExecutionSample', (e) => {
        vsehVzorcev++;
        const n = nitIme(e.polja.sampledThread);
        if (n !== o.nit) { add(drugeNiti, n, 1); return; }
        vzorcev++;
        if (e.sklad.length === 0) return;
        add(self, e.sklad[0].m, 1);
        add(selfAt, e.sklad[0].at, 1);
        const vid = new Set();
        for (const f of e.sklad) {
            if ((isNoppes(f.m) || isMc(f.m)) && !vid.has(f.m)) { vid.add(f.m); add(incl, f.m, 1); }
        }
        const prviN = e.sklad.find((f) => isNoppes(f.m));
        add(podNoppes, prviN ? prviN.m : '(brez noppes okvirja)', 1);
    });

    // --- alokacije ---
    let alokB = 0, alokVseB = 0, alokDogodkov = 0;
    const poRazredu = new Map(), poMestu = new Map(), alokIncl = new Map(), alokNoppes = new Map();
    await beri(o.file, 'jdk.ObjectAllocationInNewTLAB,jdk.ObjectAllocationOutsideTLAB', (e) => {
        const w = e.tip === 'jdk.ObjectAllocationInNewTLAB' ? bytes(e.polja.tlabSize) : bytes(e.polja.allocationSize);
        alokVseB += w;
        if (nitIme(e.polja.eventThread) !== o.nit) return;
        alokDogodkov++;
        alokB += w;
        const razred = (e.polja.objectClass || '?').replace(/ \(classLoader.*$/, '');
        add(poRazredu, razred, w);
        const prvi = e.sklad.find((f) => !JDK.test(f.m));
        add(poMestu, prvi ? prvi.at : '(samo JDK)', w);
        const vid = new Set();
        for (const f of e.sklad) {
            if ((isNoppes(f.m) || isMc(f.m)) && !vid.has(f.m)) { vid.add(f.m); add(alokIncl, f.m, w); }
        }
        const prviN = e.sklad.find((f) => isNoppes(f.m));
        add(alokNoppes, prviN ? prviN.m : '(brez noppes okvirja)', w);
    });

    const L = [];
    const pct = (x, t) => t > 0 ? (100 * x / t).toFixed(1) : '0.0';
    const mb = (b) => (b / (1024 * 1024)).toFixed(1);
    const tabela = (naslov, glava, vrstice) => {
        L.push('', '### ' + naslov, '', '| ' + glava.join(' | ') + ' |', '|' + glava.map((g, i) => i === 0 ? '---' : '---:').join('|') + '|');
        for (const v of vrstice) L.push('| ' + v.join(' | ') + ' |');
    };
    const ime = (s) => '`' + s.replace(/\|/g, '\\|') + '`';

    L.push('# Povzetek JFR: ' + path.basename(o.file), '');
    L.push('Nit: `' + o.nit + '`. Trajanje posnetka: ' + sekund + ' s.');
    L.push('Vzorcev CPU niti: ' + vzorcev + ' (vseh niti ' + vsehVzorcev + '). Alokacij niti (ocena iz TLAB vzorcev): ' +
        mb(alokB) + ' MB' + (sekund ? ' = ' + (alokB / (1024 * 1024) / sekund).toFixed(1) + ' MB/s' : '') +
        ' (vse niti ' + mb(alokVseB) + ' MB, dogodkov niti ' + alokDogodkov + ').');

    tabela('CPU: lastni cas po metodi (vrh sklada)', ['metoda', 'vzorcev', '%'],
        urejeno(self).slice(0, o.top).map(([k, v]) => [ime(k), v, pct(v, vzorcev)]));
    tabela('CPU: lastni cas po vrstici', ['mesto', 'vzorcev', '%'],
        urejeno(selfAt).slice(0, o.top).map(([k, v]) => [ime(k), v, pct(v, vzorcev)]));
    tabela('CPU: vkljucni cas (noppes.* in net.minecraft*)', ['metoda', 'vzorcev', '%'],
        urejeno(incl).slice(0, o.top * 2).map(([k, v]) => [ime(k), v, pct(v, vzorcev)]));
    tabela('CPU: po prvem noppes okvirju od vrha sklada', ['metoda', 'vzorcev', '%'],
        urejeno(podNoppes).slice(0, o.top).map(([k, v]) => [ime(k), v, pct(v, vzorcev)]));
    tabela('Alokacije: po razredu objekta', ['razred', 'MB', '%'],
        urejeno(poRazredu).slice(0, o.top).map(([k, v]) => [ime(k), mb(v), pct(v, alokB)]));
    tabela('Alokacije: po prvem ne-JDK okvirju', ['mesto', 'MB', '%'],
        urejeno(poMestu).slice(0, o.top).map(([k, v]) => [ime(k), mb(v), pct(v, alokB)]));
    tabela('Alokacije: vkljucno (noppes.* in net.minecraft*)', ['metoda', 'MB', '%'],
        urejeno(alokIncl).slice(0, o.top * 2).map(([k, v]) => [ime(k), mb(v), pct(v, alokB)]));
    tabela('Alokacije: po prvem noppes okvirju od vrha sklada', ['metoda', 'MB', '%'],
        urejeno(alokNoppes).slice(0, o.top).map(([k, v]) => [ime(k), mb(v), pct(v, alokB)]));
    tabela('Druge niti (vzorcev CPU)', ['nit', 'vzorcev'], urejeno(drugeNiti).slice(0, 10).map(([k, v]) => [ime(k), v]));

    const out = L.join('\n') + '\n';
    if (o.md) fs.writeFileSync(o.md, out, 'utf8');
    process.stdout.write(out);
}

main().catch((e) => { console.error(e.message); process.exit(1); });
