#!/usr/bin/env node
// M5-K.1 (18. 10.): povzetek vzorcev sklada render niti (k-run.ps1 -Vzorci, rework/diag/StackSampler).
//
// Zakaj ne JFR: JFR v JDK 8 je v 14 s ujel samo 38 vzorcev 'Client thread' (render nit je vecino
// casa v JNI, nativni vzorcevalnik pa na 20 ms vzame eno nit od vseh v JNI). StackSampler gleda
// samo render nit, ne glede na stanje.
//
// Vhod: vrstice 'stevilo<TAB>koren;...;list' (okvirji razred.metoda).
// Izpis: (1) deleži po fazah slicice in po delih upodobitve NPC-ja (prvo pravilo, ki se ujema s
//        skladom, zmaga; vrstni red je od najbolj specificnega), (2) lastni cas po metodi (list),
//        (3) vkljucni cas po metodi (noppes.*, net.minecraft*, org.lwjgl*).
//
// Uporaba: node dev/tools/skladi-povzetek.js <skladi.txt> [--top 30] [--md izhod.md]
'use strict';
const fs = require('fs');
const path = require('path');

const a = process.argv.slice(2);
const o = { file: null, top: 30, md: null };
for (let i = 0; i < a.length; i++) {
    if (a[i] === '--top') o.top = parseInt(a[++i], 10);
    else if (a[i] === '--md') o.md = a[++i];
    else o.file = a[i];
}
if (!o.file) { console.error('Uporaba: node skladi-povzetek.js <skladi.txt> [--top 30] [--md izhod.md]'); process.exit(2); }

// Skupine: [ime, regex nad celim zlozenim skladom]. Vrstni red = prednost.
const SKUPINE = [
    ['NPC: oci (LayerEyes)', /noppes\.npcs\.client\.layer\.LayerEyes\./],
    ['NPC: ime in naslov', /noppes\.npcs\.client\.renderer\.RenderNPCInterface\.(renderName|renderLivingLabel)|net\.minecraft\.client\.renderer\.entity\.Render\.renderLivingLabel/],
    ['NPC: oblacki (RenderChatMessages)', /noppes\.npcs\.client\.RenderChatMessages\./],
    ['NPC: senca in ogenj', /Render\.(renderShadow|doRenderShadowAndFire|renderEntityOnFire)/],
    ['NPC: pokrivalo (LayerHeadwear, Model2DRenderer)', /noppes\.npcs\.client\.layer\.LayerHeadwear\./],
    ['NPC: sloji CNPC (ostali deli modela)', /noppes\.npcs\.client\.layer\./],
    ['NPC: vanilla sloji (oklep, predmet)', /net\.minecraft\.client\.renderer\.entity\.layers\./],
    ['NPC: model (ModelBase.render in deli)', /RenderLivingBase\.renderModel|noppes\.npcs\.client\.model\./],
    ['NPC: ostalo v doRender (matrike, stanje, animacija)', /noppes\.npcs\.client\.renderer\.(RenderNPCInterface|RenderCustomNpc)\./],
    ['entitete: ostalo (frustum, seznam, ne-NPC)', /RenderGlobal\.renderEntities/],
    ['teren (sloji blokov, chunki)', /RenderGlobal\.(renderBlockLayer|setupTerrain|updateChunks)|ChunkRenderDispatcher/],
    ['nebo, oblaki, vreme, delci', /RenderGlobal\.(renderSky|renderClouds)|EntityRenderer\.(renderRainSnow|addRainParticles)|ParticleManager/],
    ['HUD (GuiIngame)', /GuiIngame|renderGameOverlay/],
    ['zamenjava medpomnilnika (Display.update)', /Minecraft\.updateDisplay|org\.lwjgl\.opengl\.Display\.update/],
    ['klientski tick (runTick)', /Minecraft\.runTick/],
    ['izris: ostalo (EntityRenderer)', /EntityRenderer\./],
    ['ostalo', /./],
];

const vrstice = fs.readFileSync(o.file, 'utf8').split(/\r?\n/).filter((l) => l.includes('\t'));
let vsi = 0;
const skupine = new Map(), self = new Map(), incl = new Map();
const add = (m, k, v) => m.set(k, (m.get(k) || 0) + v);
const zanimivo = (f) => /^(noppes\.|net\.minecraft|org\.lwjgl)/.test(f);
for (const l of vrstice) {
    const t = l.indexOf('\t');
    const n = parseInt(l.substring(0, t), 10);
    const sklad = l.substring(t + 1);
    vsi += n;
    add(skupine, SKUPINE.find(([, re]) => re.test(sklad))[0], n);
    const okvirji = sklad.split(';');
    add(self, okvirji[okvirji.length - 1], n);
    const vid = new Set();
    for (const f of okvirji) if (zanimivo(f) && !vid.has(f)) { vid.add(f); add(incl, f, n); }
}

const pct = (x) => (vsi > 0 ? (100 * x / vsi).toFixed(1) : '0.0');
const urejeno = (m) => [...m.entries()].sort((x, y) => y[1] - x[1]);
const ime = (s) => '`' + s.replace(/\|/g, '\\|') + '`';
const L = ['# Povzetek skladov render niti: ' + path.basename(o.file), '', 'Vzorcev: ' + vsi + '.'];
const tabela = (naslov, glava, v) => {
    L.push('', '### ' + naslov, '', '| ' + glava.join(' | ') + ' |', '|' + glava.map((g, i) => (i === 0 ? '---' : '---:')).join('|') + '|');
    for (const r of v) L.push('| ' + r.join(' | ') + ' |');
};
tabela('Faze slicice in deli upodobitve NPC-ja (prvo ujemanje)', ['skupina', 'vzorcev', '%'],
    SKUPINE.map(([k]) => k).filter((k) => skupine.has(k)).map((k) => [k, skupine.get(k), pct(skupine.get(k))]));
tabela('Lastni cas po metodi (list sklada)', ['metoda', 'vzorcev', '%'], urejeno(self).slice(0, o.top).map(([k, v]) => [ime(k), v, pct(v)]));
tabela('Vkljucni cas (noppes.*, net.minecraft*, org.lwjgl*)', ['metoda', 'vzorcev', '%'], urejeno(incl).slice(0, o.top * 2).map(([k, v]) => [ime(k), v, pct(v)]));
const out = L.join('\n') + '\n';
if (o.md) fs.writeFileSync(o.md, out, 'utf8');
process.stdout.write(out);
