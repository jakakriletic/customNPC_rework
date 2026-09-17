// Preverba pogodbe med skripto v svetu in razclenjevalnikom loga - brez Minecrafta.
//
// Zakaj to obstaja: 17. 9. (M0.7, dnevnik 26) je merilo padlo, ker se je marker v logu
// pojavil dvakrat in je razclenjevalnik stel napacen zadetek. Napaka ni bila v modu,
// ampak v pogodbi med izpisom in branjem. Tak razred napake se da ujeti brez zagona
// sveta: skripto pozenemo nad ponarejenim svetom in pogledamo, kaj bi sla v konzolo.
//
// To NE nadomesti zagona. Ne pove nicesar o tem, kako se NPC obnasa - samo to, da je
// skripta sintakticno veljavna po minifikaciji, da so vsi markerji izpisani in da je
// vzorcev dovolj za merilo L6.
//
// Uporaba (iz korena mape, potrebuje node):
//     node dev/testworld/preveri-markerje.js
//
// Izpis gre v dev/testworld/markerji-vzorec.txt; to datoteko lahko potem prebere
// razclenjevalnik iz r2-run.ps1 in se prepricamo, da se regexi ujemajo.

var fs = require('fs');
var path = require('path');
var vm = require('vm');

var here = __dirname;
var src = fs.readFileSync(path.join(here, 'r2-control.js'), 'utf8');

// Enaka minifikacija kot v vstavi-skripto.py: tocno tisto, kar bo v clone JSON-u.
var mini = src.replace(/\/\/[^\n]*/g, '').replace(/"/g, "'").replace(/\s+/g, ' ').trim();

var lines = [];
var uuid = 0;

function Ent(tag, x, y, z, opts) {
    opts = opts || {};
    this._tag = tag; this._x = x; this._y = y; this._z = z;
    this._uuid = 'u' + (++uuid);
    this._nav = opts.nav === undefined ? true : opts.nav;
    this._final = opts.final || null;
    this._step = opts.step || 0;
    this._age = 0;
}
Ent.prototype.hasTag = function (t) { return this._tag === t; };
Ent.prototype.getX = function () { return this._x; };
Ent.prototype.getY = function () { return this._y; };
Ent.prototype.getZ = function () { return this._z; };
Ent.prototype.getUUID = function () { return this._uuid; };
Ent.prototype.isNavigating = function () { return this._nav; };
Ent.prototype.getNavigationPath = function () { return this._final; };
Ent.prototype.navigateTo = function () { };
Ent.prototype.clearNavigation = function () { };
Ent.prototype.setAttackTarget = function () { };
Ent.prototype.setPosition = function (x, y, z) { this._x = x; this._y = y; this._z = z; };
// Dodano 17. 9. skupaj z diagnostiko zmrznitve: en skriptni tick je 10 server tickov.
Ent.prototype.getAge = function () { return this._age; };
Ent.prototype.getMotionX = function () { return 0; };
Ent.prototype.getMotionY = function () { return 0; };
Ent.prototype.getMotionZ = function () { return -this._step / 10; };

function pos(x, y, z) {
    return {
        getX: function () { return x; },
        getY: function () { return y; },
        getZ: function () { return z; }
    };
}

var ents = [];
function add(tag, x, z, opts) { var e = new Ent(tag, x, 4, z, opts); ents.push(e); return e; }

var i;
// proga F: leteci + zid; pot se konca pri zidu, NPC obstane
for (i = 0; i < 6; i++) { add('r2flyer', -35 + i * 2, -16, { final: pos(-30, 4, -23), step: 0 }); }
// proga W: kopenski + isti zid
for (i = 0; i < 6; i++) { add('r2walker', -15 + i * 2, -16, { final: pos(-10, 4, -23), step: 0 }); }
// proga P: leteci, prosto; cela pot in premik proti cilju
for (i = 0; i < 6; i++) { add('r2flyer', 27 + i * 2, -16, { final: pos(32, 4, -32), step: 0.4 }); }
add('r2target', -30, -32, { nav: false });
add('r2target', -10, -32, { nav: false });
add('r2target', 32, -32, { nav: false });

var world = { getAllEntities: function () { return ents; } };
var npc = {
    executeCommand: function (c) { lines.push(c.replace(/^\/say /, '')); },
    getWorld: function () { return world; }
};

var sandbox = { Math: Math };
vm.createContext(sandbox);
vm.runInContext(mini, sandbox);   // namenoma minificirana razlicica, ne vir

sandbox.init({ npc: npc });
for (var t = 1; t <= 194; t++) {
    for (var k = 0; k < ents.length; k++) {
        ents[k]._age += 10;
        if (ents[k]._step) { ents[k]._z -= ents[k]._step; }
    }
    sandbox.tick({ npc: npc });
}

var out = path.join(here, 'markerji-vzorec.txt');
fs.writeFileSync(out, lines.join('\n') + '\n');

var napake = [];
var obvezni = ['R2-INIT', 'R2-SETUP ', 'R2-A-START', 'R2-A-END', 'R2-B-START', 'R2-B-END',
               'R2-C-START', 'R2-C-END', 'R2-D-START', 'R2-D-END', 'R2-SUM'];
for (i = 0; i < obvezni.length; i++) {
    var n = lines.filter(function (l) { return l.indexOf(obvezni[i]) === 0; }).length;
    if (n !== 1) { napake.push('marker ' + obvezni[i] + ' se pojavi ' + n + '-krat, pricakovano 1'); }
}
var faze = ['A', 'B', 'C', 'D'];
var proge = ['F', 'W', 'P'];
for (i = 0; i < faze.length; i++) {
    for (var j = 0; j < proge.length; j++) {
        var pre = 'R2-S faza=' + faze[i] + ' ';
        var suf = ' proga=' + proge[j] + ' ';
        var m = lines.filter(function (l) { return l.indexOf(pre) === 0 && l.indexOf(suf) > 0; }).length;
        if (m < 20) { napake.push('faza ' + faze[i] + ' proga ' + proge[j] + ' ima ' + m + ' vzorcev, merilo L6 zahteva 20'); }
    }
}

// Diagnostiki dStarost in gib sta del pogodbe z r2-run.ps1 od 17. 9. naprej; brez njiju
// razclenjevalnik vzorca ne prepozna in faza B ostane neberljiva kot pri prvem zagonu.
var vzorci = lines.filter(function (l) { return l.indexOf('R2-S ') === 0; });
var diag = /dStarost=-?\d+\/-?\d+ gib=[\d.]+\/[\d.]+$/;
var brez = vzorci.filter(function (l) { return !diag.test(l); }).length;
if (brez > 0) { napake.push(brez + ' vzorcnih vrstic nima polj dStarost in gib'); }

console.log('vzorcnih vrstic: ' + vzorci.length);
console.log('vrstic skupaj: ' + lines.length);
console.log('zapisano v:    ' + out);
if (napake.length === 0) {
    console.log('POGODBA MARKERJEV OK');
    process.exit(0);
}
napake.forEach(function (n) { console.log('NAPAKA: ' + n); });
process.exit(1);
