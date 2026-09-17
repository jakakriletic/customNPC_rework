// Preverba pogodbe med nav-control.js in razclenjevalnikom v .\nav-run.ps1 - brez Minecrafta.
//
// Zakaj to obstaja: 17. 9. (M0.7, dnevnik 26) je merilo padlo, ker se je marker v logu
// pojavil dvakrat in je razclenjevalnik prebral napacen zadetek. Napaka ni bila v modu,
// ampak v pogodbi med izpisom in branjem. Tak razred napake se da ujeti brez zagona
// sveta: skripto pozenemo nad ponarejenim svetom in pogledamo, kaj bi slo v konzolo.
//
// To NE nadomesti zagona. O obnasanju NPC-jev ne pove nicesar - samo to, da je skripta
// po minifikaciji sintakticno veljavna, da so vsi markerji izpisani, da je vzorcev
// dovolj za merilo N4 in da vzorcna vrstica nosi vsa polja, ki jih merila berejo.
//
// Uporaba (iz korena mape, potrebuje node):
//     node dev/testworld/preveri-nav-markerje.js

var fs = require('fs');
var path = require('path');
var vm = require('vm');

var here = __dirname;
var src = fs.readFileSync(path.join(here, 'nav-control.js'), 'utf8');

// Enaka minifikacija kot v vstavi-skripto.py: tocno tisto, kar bo v clone JSON-u.
var mini = src.replace(/\/\/[^\n]*/g, '').replace(/"/g, "'").replace(/\s+/g, ' ').trim();

var lines = [];
var uuid = 0;

var START_Z = 64;
var WALL_Z = 72;
var GOAL_Z = 78;

function Ent(tag, x, z, opts) {
    opts = opts || {};
    this._tag = tag; this._x = x; this._y = 4; this._z = z;
    this._uuid = 'u' + (++uuid);
    this._nav = opts.nav === undefined ? true : opts.nav;
    this._final = opts.final || null;
    this._step = opts.step || 0;
    this._stopZ = opts.stopZ === undefined ? GOAL_Z : opts.stopZ;
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
Ent.prototype.setPosition = function (x, y, z) { this._x = x; this._y = y; this._z = z; };
Ent.prototype.getAge = function () { return this._age; };
Ent.prototype.getMotionX = function () { return 0; };
Ent.prototype.getMotionY = function () { return 0; };
Ent.prototype.getMotionZ = function () { return this._step / 10; };

function pos(x, y, z) {
    return {
        getX: function () { return x; },
        getY: function () { return y; },
        getZ: function () { return z; }
    };
}

var ents = [];
function add(tag, x, z, opts) { var e = new Ent(tag, x, z, opts); ents.push(e); return e; }

var i;
// proga G: 8 kopenskih pred zidom z vrati pri x = -10.
// Trije pridejo skozi vrata, pet jih obstane na koncu delne poti tik pred zidom - to je
// pojav, ki ga faza A meri, in hkrati najbolj neugoden primer za razclenjevalnik.
for (i = 0; i < 8; i++) {
    var skozi = i < 3;
    add('navwalkg', -14 + i, START_Z, {
        final: skozi ? pos(-10, 4, GOAL_Z) : pos(-10, 4, WALL_Z - 1),
        step: skozi ? 0.9 : 0.35,
        stopZ: skozi ? GOAL_Z : WALL_Z - 1
    });
}
// proga O: 8 kopenskih na odprtem, vsi pridejo do cilja
for (i = 0; i < 8; i++) {
    add('navwalko', 12 + i, START_Z, { final: pos(15, 4, GOAL_Z), step: 0.9, stopZ: GOAL_Z });
}
add('navtarget', -10, GOAL_Z, { nav: false, step: 0 });
add('navtarget', 15, GOAL_Z, { nav: false, step: 0 });

var world = { getAllEntities: function () { return ents; } };
var npc = {
    executeCommand: function (c) { lines.push(c.replace(/^\/say /, '')); },
    getWorld: function () { return world; }
};

var sandbox = { Math: Math };
vm.createContext(sandbox);
vm.runInContext(mini, sandbox);   // namenoma minificirana razlicica, ne vir

sandbox.init({ npc: npc });
for (var t = 1; t <= 110; t++) {
    for (var k = 0; k < ents.length; k++) {
        var e = ents[k];
        e._age += 10;
        if (e._step && e._z < e._stopZ) {
            e._z = Math.min(e._stopZ, e._z + e._step);
            // priblizevanje ciljnemu x, da skupina skozi vrata res gre skozi vrata
            if (e._tag === 'navwalkg' && e._z > WALL_Z - 4) { e._x = -10; }
        }
    }
    sandbox.tick({ npc: npc });
}

var out = path.join(here, 'nav-markerji-vzorec.txt');
fs.writeFileSync(out, lines.join('\n') + '\n');

var napake = [];
var obvezni = ['NAV-INIT', 'NAV-SETUP ', 'NAV-A-START', 'NAV-A-END', 'NAV-B-START',
               'NAV-B-END', 'NAV-SUM'];
for (i = 0; i < obvezni.length; i++) {
    var n = lines.filter(function (l) { return l.indexOf(obvezni[i]) === 0; }).length;
    if (n !== 1) { napake.push('marker ' + obvezni[i] + ' se pojavi ' + n + '-krat, pricakovano 1'); }
}

var faze = ['A', 'B'];
var proge = ['G', 'O'];
for (i = 0; i < faze.length; i++) {
    for (var j = 0; j < proge.length; j++) {
        var pre = 'NAV-S faza=' + faze[i] + ' ';
        var suf = ' proga=' + proge[j] + ' ';
        var m = lines.filter(function (l) { return l.indexOf(pre) === 0 && l.indexOf(suf) > 0; }).length;
        if (m < 20) {
            napake.push('faza ' + faze[i] + ' proga ' + proge[j] + ' ima ' + m
                + ' vzorcev, merilo N4 zahteva 20');
        }
        var cas = lines.filter(function (l) {
            return l.indexOf('NAV-CAS faza=' + faze[i] + ' proga=' + proge[j] + ' ') === 0;
        }).length;
        if (cas !== 1) {
            napake.push('faza ' + faze[i] + ' proga ' + proge[j] + ' ima ' + cas
                + ' vrstic NAV-CAS, pricakovano 1');
        }
    }
}

// Polja, ki jih berejo merila. Manjkajoce polje ne pade kot napaka, ampak kot prazna
// meritev - zato je to preverba in ne domneva.
var vzorci = lines.filter(function (l) { return l.indexOf('NAV-S ') === 0; });
var oblika = new RegExp('^NAV-S faza=[AB] tick=\\d+ proga=[GO]'
    + ' navig=\\d+/\\d+ cele=\\d+/\\d+ prispelo=\\d+/\\d+ cez=\\d+/\\d+ cezDalec=\\d+/\\d+'
    + ' priGrlu=\\d+/\\d+ prevozenoPovp=[\\d.]+ prevozenoMax=[\\d.]+'
    + ' doCiljaMin=[\\d.]+ doCiljaPovp=[\\d.]+ razpon=[\\d.]+ razponGrlo=[\\d.]+'
    + ' zastoj=\\d+/\\d+ dStarost=-?\\d+/-?\\d+ gib=[\\d.]+/[\\d.]+$');
var brez = vzorci.filter(function (l) { return !oblika.test(l); });
if (brez.length > 0) {
    napake.push(brez.length + ' vzorcnih vrstic ne ustreza obliki, npr.: ' + brez[0]);
}

// Cas do cilja mora biti skladen s stevilom prihodov: NAV-CAS prispelo=k/n in k vrstic
// NAV-PRISPEL za isto fazo in progo. Ce se razideta, je ena od obeh stevilk izmisljena.
for (i = 0; i < faze.length; i++) {
    for (var q = 0; q < proge.length; q++) {
        var casLine = lines.filter(function (l) {
            return l.indexOf('NAV-CAS faza=' + faze[i] + ' proga=' + proge[q] + ' ') === 0;
        })[0];
        if (!casLine) { continue; }
        var mm = /prispelo=(\d+)\/(\d+)/.exec(casLine);
        var prihodov = lines.filter(function (l) {
            return l.indexOf('NAV-PRISPEL faza=' + faze[i] + ' proga=' + proge[q] + ' ') === 0;
        }).length;
        if (mm && parseInt(mm[1], 10) !== prihodov) {
            napake.push('faza ' + faze[i] + ' proga ' + proge[q] + ': NAV-CAS pravi '
                + mm[1] + ' prihodov, vrstic NAV-PRISPEL pa je ' + prihodov);
        }
    }
}

console.log('vzorcnih vrstic: ' + vzorci.length);
console.log('vrstic skupaj:   ' + lines.length);
console.log('zapisano v:      ' + out);
if (napake.length === 0) {
    console.log('POGODBA MARKERJEV OK');
    process.exit(0);
}
napake.forEach(function (n) { console.log('NAPAKA: ' + n); });
process.exit(1);
