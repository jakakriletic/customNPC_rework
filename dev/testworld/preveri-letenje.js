// M4.16 - preverba skripte krmilnika brez Minecrafta (.\letenje-samotest.ps1).
//
// Prebere skripto IZ NAMESCENEGA LET_Control.json (kar bi dobil Nashorn), postavi
// ponarejen svet iz letenje-setup-commands.txt in skripto pozene skozi vse faze.
// Gibanje je namenoma preprosto (ravna crta proti cilju, brez sten, po koncu poti
// drsenje 0,5 bloka v 50 tickih; cilj dlje od 32 nima poti). Ujame samo neujemanje med skripto,
// postavitvijo in branjem harnessa; o obnasanju NPC-jev ne pove nicesar.
//
// Uporaba: node preveri-letenje.js <LET_Control.json> <letenje-setup-commands.txt>

'use strict';
var fs = require('fs');

// Clone datoteka je NBT v JSON-u (0b, 1.0f), ne cisti JSON; polje Script se zato
// izreze z istim vzorcem kot v letenje-lib.ps1 in odkodira kot JSON niz.
var cloneText = fs.readFileSync(process.argv[2], 'utf8');
var scripts = cloneText.match(/"Script"\s*:\s*"(?:\\.|[^"\\])*"/g);
if (!scripts || scripts.length !== 1) { throw new Error('LET_Control mora imeti natanko eno skripto'); }
var source = JSON.parse(scripts[0].replace(/^"Script"\s*:\s*/, ''));
if (source.length === 0) { throw new Error('skripta v LET_Control je prazna'); }
var setup = fs.readFileSync(process.argv[3], 'utf8').split(/\r?\n/);

var RANGE = 32;
var STEP = 0.2;
var entities = [];
var uuid = 0;

function Entity(name, x, y, z) {
    this.name = name;
    this.x = x; this.y = y; this.z = z;
    this.mx = 0; this.my = 0; this.mz = 0;
    this.age = 0;
    this.id = 'u' + (uuid++);
    this.tags = [];
    this.goal = null;
    this.speed = 0;
    this.slide = 0;
    if (name === 'LET_Flyer') { this.tags = ['m416', 'm416flyer']; }
    if (name === 'LET_Walker') { this.tags = ['m416', 'm416walker']; }
    if (name === 'LET_Target') { this.tags = ['m416', 'm416target']; }
}
Entity.prototype.getX = function () { return this.x; };
Entity.prototype.getY = function () { return this.y; };
Entity.prototype.getZ = function () { return this.z; };
Entity.prototype.getUUID = function () { return this.id; };
Entity.prototype.getAge = function () { return this.age; };
Entity.prototype.hasTag = function (t) { return this.tags.indexOf(t) >= 0; };
Entity.prototype.setPosition = function (x, y, z) { this.x = x; this.y = y; this.z = z; };
Entity.prototype.setMotionX = function (v) { this.mx = v; };
Entity.prototype.setMotionY = function (v) { this.my = v; };
Entity.prototype.setMotionZ = function (v) { this.mz = v; };
Entity.prototype.getMotionX = function () { return this.mx; };
Entity.prototype.getMotionY = function () { return this.my; };
Entity.prototype.getMotionZ = function () { return this.mz; };
Entity.prototype.setAttackTarget = function () { };
Entity.prototype.clearNavigation = function () { this.goal = null; this.slide = 0; };
Entity.prototype.navigateTo = function (x, y, z, speed) {
    var dx = x - this.x, dy = y - this.y, dz = z - this.z;
    if (Math.sqrt(dx * dx + dy * dy + dz * dz) > RANGE) { this.goal = null; return; }
    this.goal = { x: x, y: y, z: z };
    this.speed = speed;
};
Entity.prototype.isNavigating = function () { return this.goal !== null; };
Entity.prototype.getNavigationPath = function () {
    if (this.goal === null) { return null; }
    var g = this.goal;
    return { getX: function () { return Math.floor(g.x); }, getY: function () { return g.y; }, getZ: function () { return Math.floor(g.z); } };
};
Entity.prototype.step = function () {
    this.age++;
    if (this.goal !== null) {
        var dx = this.goal.x - this.x, dy = this.goal.y - this.y, dz = this.goal.z - this.z;
        var d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        var s = STEP * this.speed;
        if (d <= s) {
            this.mx = dx; this.my = dy; this.mz = dz;
            this.x = this.goal.x; this.y = this.goal.y; this.z = this.goal.z;
            this.slide = 0.5;
            this.goal = null;
        } else {
            this.mx = dx / d * s; this.my = dy / d * s; this.mz = dz / d * s;
            this.x += this.mx; this.y += this.my; this.z += this.mz;
        }
    } else if (this.slide > 0) {
        var v = Math.min(0.01, this.slide);
        this.z -= v;
        this.slide -= v;
        this.mz = -v;
    } else {
        this.mx = 0; this.my = 0; this.mz = 0;
    }
};

for (var i = 0; i < setup.length; i++) {
    var m = /^noppes clone spawn (\w+) 1 (-?\d+),(-?\d+),(-?\d+)\s*$/.exec(setup[i].trim());
    if (m) { entities.push(new Entity(m[1], +m[2] + 0.5, +m[3], +m[4] + 0.5)); }
}

var out = [];
var control = new Entity('LET_Control', 0.5, 4, -39.5);
var world = { getAllEntities: function (type) { return type === 2 ? entities.slice(0) : []; } };
var npc = {
    getWorld: function () { return world; },
    executeCommand: function (cmd) {
        if (cmd.indexOf('/say ') !== 0) { throw new Error('nepricakovan ukaz ' + cmd); }
        out.push('[Server thread/INFO]: [Server] ' + cmd.substring(5));
    }
};

var api = new Function(source + '\nreturn { init: init, tick: tick };')();
api.init({ npc: npc });
for (var tickNo = 1; tickNo <= 2100; tickNo++) {
    for (var j = 0; j < entities.length; j++) { entities[j].step(); }
    control.step();
    if (tickNo % 10 === 0) { api.tick({ npc: npc }); }
}
process.stdout.write(out.join('\n') + '\n');
