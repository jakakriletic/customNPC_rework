// Controller-only harness contract test. Mock movement is not evidence of in-world success.
var fs = require('fs');
var vm = require('vm');
var assert = require('assert');
var path = require('path');
var source = fs.readFileSync(path.join(__dirname, 'nav-control.js'), 'utf8');
assert.strictEqual((source.match(/var NAV_MODE = 'NAVIGATE';/g) || []).length, 1);

function run(mode) {
    var calls = { A: 0, B: 0, formation: 0, stop: 0 };
    var lines = [];
    var ctx;
    var entities = [];
    function Ent(lane, i) {
        this.lane = lane; this.id = lane + i; this.x = (lane === 'G' ? -14 : 12) + i;
        this.y = 4; this.z = 64; this.age = 0;
    }
    Ent.prototype.getX = function () { return this.x; };
    Ent.prototype.getY = function () { return this.y; };
    Ent.prototype.getZ = function () { return this.z; };
    Ent.prototype.getAge = function () { return this.age; };
    Ent.prototype.getUUID = function () { return this.id; };
    Ent.prototype.hasTag = function (tag) { return tag === 'navwalk' + this.lane.toLowerCase(); };
    Ent.prototype.getMotionX = Ent.prototype.getMotionY = Ent.prototype.getMotionZ = function () { return 0; };
    Ent.prototype.isNavigating = function () { return false; };
    Ent.prototype.getNavigationPath = function () { return null; };
    Ent.prototype.clearNavigation = function () {};
    Ent.prototype.setPosition = function (x, y, z) { this.x = x; this.y = y; this.z = z; };
    Ent.prototype.navigateTo = function () { calls[ctx.phase]++; };
    for (var i = 0; i < 8; i++) { entities.push(new Ent('G', i)); entities.push(new Ent('O', i)); }
    var shape = [];
    for (i = 0; i < 8; i++) { shape.push({ side: -3 + 2 * (i % 4), back: i < 4 ? -1 : 1 }); }
    var api = {
        legija: function (list, x, y, z, width, yaw, speed, flags) {
            assert.strictEqual(list.length, 8); assert.strictEqual(width, 4);
            assert.strictEqual(yaw, 0); assert.strictEqual(speed, 3); assert.strictEqual(flags, '');
            calls.formation++;
            // Deliberately synthetic: exercise independent unique-slot measurement only.
            list.forEach(function (e, j) { e.setPosition(Math.floor(x - shape[j].side) + 0.5, y, Math.floor(z - shape[j].back) + 0.5); });
            return 'RWSQUAD enota=1';
        },
        status: function () { return 'RWSQUAD ni aktivnih enot'; },
        stop: function (list) { assert.strictEqual(list.length, 16); calls.stop++; return 'RWSQUAD stop izpusceni=16'; }
    };
    var npc = { getWorld: function () { return { getAllEntities: function () { return entities; } }; },
        executeCommand: function (cmd) { lines.push(cmd.replace(/^\/say /, '')); } };
    ctx = vm.createContext({ Java: {
        type: function (name) {
            if (name === 'noppes.npcs.rework.formation.FormationApi') { return api; }
            if (name === 'noppes.npcs.rework.formation.FormationShape') { return { legion: function () { return shape; } }; }
            throw new Error('Unexpected Java.type ' + name);
        },
        to: function (list, type) { assert.strictEqual(type, 'java.lang.Object[]'); return list; }
    } });
    var src = source.replace("var NAV_MODE = 'NAVIGATE';", "var NAV_MODE = '" + mode + "';");
    // Same minification as the PowerShell installer.
    src = src.replace(/\/\/[^\n]*/g, '').replace(/"/g, "'").replace(/\s+/g, ' ').trim();
    vm.runInContext(src, ctx);
    ctx.init({ npc: npc });
    for (i = 0; i < 110; i++) { entities.forEach(function (e) { e.age += 10; }); ctx.tick({ npc: npc }); }
    assert(lines.indexOf('FORM-MODE ' + mode) >= 0);
    assert.strictEqual(calls.A, 16);
    assert.strictEqual(calls.formation, mode === 'FORMACIJA' ? 2 : 0);
    assert.strictEqual(calls.stop, mode === 'FORMACIJA' ? 1 : 0);
    assert.strictEqual(calls.B, mode === 'FORMACIJA' ? 0 : 16 * 28);
    assert.strictEqual(lines.filter(function (s) { return s.indexOf('NAV-SUM ') === 0; }).length, 1);
    if (mode === 'FORMACIJA') {
        ['G', 'O'].forEach(function (lane) {
            assert(lines.indexOf('FORM-CAS proga=' + lane + ' mesta=8/8 vsiTick=20') >= 0);
            assert.strictEqual(lines.filter(function (s) { return s.indexOf('FORM-S proga=' + lane + ' ') === 0; }).length, 28);
        });
    }
}
run('NAVIGATE');
run('FORMACIJA');
console.log('M4.14b: minified controller contracts pass in both modes; no Minecraft validation claimed.');
