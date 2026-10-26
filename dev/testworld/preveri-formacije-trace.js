// Controller-only contracts for the actual minified diagnostic overlay.
// The fixture is deliberately synthetic; it proves logging and read-only access only.
var fs = require('fs');
var path = require('path');
var vm = require('vm');
var assert = require('assert');
var source = fs.readFileSync(path.join(__dirname, 'formacije-trace.js'), 'utf8');
var navigatorError = process.argv[3] === 'navigator-error';
var navigator = process.argv[3] === 'navigator' || navigatorError;
var recovery = process.argv[3] === 'recovery' || navigator;
if (navigator) { source += '\n' + fs.readFileSync(path.join(__dirname, 'formacije-navigacija-trace.js'), 'utf8'); }
var output = [];
var age = 1000;
var activeTimer = false;
var started = 0;
var ended = 0;
// D-076: on a dedicated server a direct Vec3d member access throws NoClassDefFoundError
// (stripped Vec2f); only reflective Field.get on the Vec3d type may read x/y/z.
var vec3dValues = new WeakMap();
var vec3dType = { getName: function () { return 'net.minecraft.util.math.Vec3d'; }, getDeclaredField: function (key) {
    return { setAccessible: function (value) { assert.strictEqual(value, true); },
        get: function (object) { return vec3dValues.get(object)[key]; } };
} };
function vec3d(x, y, z) {
    var value = {};
    ['x', 'y', 'z'].forEach(function (key) { Object.defineProperty(value, key, { get: function () {
        throw new Error('java.lang.NoClassDefFoundError: net/minecraft/util/math/Vec2f');
    } }); });
    vec3dValues.set(value, {x: x, y: y, z: z});
    return Object.freeze(value);
}
function type(name) {
    return { getName: function () { return name; }, getDeclaredField: function (key) {
        return { setAccessible: function (value) { assert.strictEqual(value, true); },
            getType: function () { assert.strictEqual(key, 'lastPosCheck'); return vec3dType; },
            get: function (object) {
            if (navigatorError && key === 'timeoutLimit' && age === 1200) { throw new Error('synthetic read failure'); }
            return (object || managerType)[key];
        } };
    } };
}
var squadType = type('Squad');
var plannerType = type('SquadPlanner');
var managerType = type('SquadManager');
var navType = type('noppes.npcs.rework.nav.RwPathNavigateGround');
var baseNavType = type('net.minecraft.pathfinding.PathNavigate');
var entities = [];
function vector(value) { return Object.freeze([value,value,value,value,value,value,value,value]); }
function planner() {
    var p = { getClass: function () { return plannerType; },
        lagTicks: vector(0), pathUntil: vector(0), trailUntil: vector(0), finalX: vector(0.5), finalY: vector(4), finalZ: vector(78.5),
        targetX: vector(0.5), targetY: vector(4), targetZ: vector(78.5), mode: vector(0), speed: vector(0),
        error: vector(0.3), settled: vector(true), trailing: vector(false), excused: vector(false),
        slot: function () { return Object.freeze({side: 1, back: -1}); },
        tick: function () { return Math.min(age - 1000, 38); },
        phase: function () { return age - 1000 < 38 ? 'SETTLING' : 'DONE'; },
        anchorX: function () { return 0.5; }, anchorY: function () { return 4; }, anchorZ: function () { return 78.5; },
        anchorSpeed: function () { return 0; }, yaw: function () { return 0; }, remaining: function () { return 0; },
        stuckEvents: function () { return 0; } };
    return Object.freeze(p);
}
function member(index) {
    var endpoint = Object.freeze({ x: 0, y: 4, z: 78 });
    var route = Object.freeze({
        getFinalPathPoint: function () { return endpoint; }, isFinished: function () { return false; },
        getCurrentPathIndex: function () { return 0; }, getCurrentPathLength: function () { return 1; },
        getPathPointFromIndex: function () { return endpoint; }
    });
    // Equal geometry, different identity: a replaced navigation path must not
    // be mistaken for the retained recovery object in the dense diagnostic.
    var replacement = Object.freeze(Object.assign({}, route));
    var navigation = { getPath: function () {
        if (navigator && index === 0 && age === 1100) { return null; }
        return index % 2 ? null : (age < 1100 ? route : replacement);
    }, noPath: function () { return this.getPath() === null; },
        getClass: function () { return navType; }, ticksAtLastPos: 900,
        lastPosCheck: vec3d(0.5, 4, 78.5), tryUpdatePath: false, lastTimeUpdated: 0,
        targetPos: Object.freeze({getX: function () { return 0; }, getY: function () { return 4; }, getZ: function () { return 78; }}) };
    Object.defineProperty(navigation, 'totalTicks', { get: function () { return age; } });
    Object.defineProperty(navigation, 'timeoutCachedNode', { get: function () {
        return age === 1100 ? vec3d(0, 0, 0) : vec3d(0, 4, 78);
    } });
    Object.defineProperty(navigation, 'timeoutTimer', { get: function () { return age === 1100 ? 0 : 500; } });
    Object.defineProperty(navigation, 'timeoutLimit', { get: function () { return age === 1100 ? 0 : 100; } });
    Object.defineProperty(navigation, 'lastTimeoutCheck', { get: function () { return 100000 + age * 50 - 1; } });
    Object.freeze(navigation);
    var helper = Object.freeze({ action: 'WAIT', getSpeed: function () { return 0; },
        getX: function () { return 0.5; }, getY: function () { return 4; }, getZ: function () { return 78.5; } });
    var e = { posX: 0.5, posY: 4, posZ: 78.8, motionX: 0, motionY: 0, motionZ: 0.1,
        moveForward: 0, moveStrafing: 0, getUniqueID: function () { return 'member' + index; },
        getNavigator: function () { return navigation; }, getMoveHelper: function () { return helper; },
        getAIMoveSpeed: function () { return 0; } };
    Object.defineProperty(e, 'ticksExisted', { get: function () { return age; } });
    entities.push(e);
    return Object.freeze(e);
}
var squads = ['G', 'O'].map(function (lane, k) {
    var members = [];
    for (var i = 0; i < 8; i++) { members.push(member(8 * k + i)); }
    var squad = { getClass: function () { return squadType; }, label: 'NAV_Walk' + lane,
        planner: planner(), members: Object.freeze(members), pathRequests: 0,
        recovery: Object.freeze(members.map(function (e) { return e.getNavigator().getPath(); })),
        lastPathTick: vector(0), lastPathX: vector(0.5), lastPathZ: vector(78.5),
        byInterval: 0, byMove: 0, byLost: 0 };
    Object.defineProperty(squad, 'finished', { get: function () { return age >= 1038; } });
    return Object.freeze(squad);
});
var manager = { getClass: function () { return managerType; }, squads: {
    values: function () { return { iterator: function () {
        var i = 0;
        return { hasNext: function () { return i < squads.length; }, next: function () { return squads[i++]; } };
    } }; }
} };
managerType.registered = manager;
var npc = { getAge: function () { return age; }, getTimers: function () { return {
    start: function (id, ticks, repeat) {
        assert.strictEqual(id, 414); assert.strictEqual(ticks, 0); assert.strictEqual(repeat, true);
        assert.strictEqual(activeTimer, false); activeTimer = true;
    }, stop: function (id) { assert.strictEqual(id, 414); activeTimer = false; }
}; } };
function IdentityMap() { this.keys = []; this.values = []; }
IdentityMap.prototype.containsKey = function (key) { return this.keys.indexOf(key) >= 0; };
IdentityMap.prototype.put = function (key, value) { this.keys.push(key); this.values.push(value); };
IdentityMap.prototype.get = function (key) { return this.values[this.keys.indexOf(key)]; };
var context = vm.createContext({ phase: 'B', formTraceRecovery: recovery,
    formationAll: function (arg) { assert.strictEqual(arg, npc); started++; },
    endPhase: function (arg, marker) {
        assert.strictEqual(activeTimer, false); assert.strictEqual(arg, npc); assert.strictEqual(marker, 'NAV-B-END');
        assert.strictEqual(output[output.length - 1], 'FORM-TRACE end tick=560'); ended++;
    }, Java: { type: function (name) {
        if (name === 'noppes.npcs.rework.formation.SquadManager') { return { class: managerType }; }
        if (name === 'noppes.npcs.LogWriter') { return { info: function (text) { output.push(text); } }; }
        if (name === 'java.util.IdentityHashMap') { return IdentityMap; }
        if (name === 'net.minecraft.pathfinding.PathNavigate') { return { class: baseNavType }; }
        if (name === 'java.lang.System') { return { currentTimeMillis: function () { return 100000 + age * 50; } }; }
        throw new Error('Unexpected Java.type ' + name);
    } }
});
source = source.replace(/\/\/[^\n]*/g, '').replace(/"/g, "'").replace(/\s+/g, ' ').trim();
vm.runInContext(source, context);
context.formationAll(npc);
assert.strictEqual(started, 1);
context.timer({ npc: npc, id: 414 }); // elapsed=0 must not use uninitialised errors
for (var t = 1; t <= 560; t++) {
    age++;
    var before = output.length;
    context.timer({ npc: npc, id: 999 });
    assert.strictEqual(output.length, before);
    context.timer({ npc: npc, id: 414 });
    context.timer({ npc: npc, id: 414 }); // same tick must not duplicate records
}
context.endPhase(npc, 'NAV-B-END');
assert.strictEqual(ended, 1);
var rows = output.filter(function (line) { return line.indexOf('FORM-D ') === 0; });
assert.strictEqual(rows.length, recovery ? 16 * 560 : 16 * (80 + 24));
assert(rows.some(function (line) { return line.indexOf('proga=O tick=40 i=0 ') >= 0 && line.indexOf('faza=DONE finished=true') >= 0; }));
assert(rows.some(function (line) { return line.indexOf('tick=560 ') >= 0; }));
assert(rows.some(function (line) { return line.indexOf('pathNext=none pathEnd=none') >= 0; }));
assert(rows.some(function (line) { return line.indexOf('pathNext=0.000000,4.000000,78.000000') >= 0; }));
assert(rows.every(function (line) { return line.indexOf('undefined') < 0 && line.indexOf('NaN') < 0; }));
var navErrors = output.filter(function (line) { return line.indexOf('FORM-NAVERR ') === 0; });
if (navigatorError) {
    // A failed private read must be visible and must not stop later samples.
    assert.strictEqual(navErrors.length, 3);
    assert(navErrors[0].indexOf('FORM-NAVERR step=timeoutLimit count=1 error=Error:_synthetic_read_failure') === 0);
    assert.strictEqual(rows.filter(function (line) { return line.indexOf(' navError=timeoutLimit') >= 0; }).length, 16);
} else { assert.strictEqual(navErrors.length, 0); }
if (recovery) {
    function fieldsAt(tick, member) {
        var line = rows.filter(function (r) { return r.indexOf('proga=G tick=' + tick + ' i=' + member + ' ') >= 0; })[0];
        var fields = {};
        line.replace(/(\w+)=([^ ]+)/g, function (_, key, value) { fields[key] = value; });
        return fields;
    }
    var before = fieldsAt(99, 0);
    var after = fieldsAt(100, 0);
    assert.notStrictEqual(before.navId, '0');
    assert.strictEqual(before.navId, before.recoveryId);
    assert.strictEqual(after.recoveryId, before.recoveryId);
    assert.notStrictEqual(after.navId, after.recoveryId);
    if (navigator) {
        assert.strictEqual(after.navId, '0');
        assert.notStrictEqual(fieldsAt(101, 0).navId, '0');
        assert.strictEqual(before.navTimeoutMs, '500');
        assert.strictEqual(after.navTimeoutMs, '0');
        assert.strictEqual(after.navTimeoutLimit, '0.000000');
        assert.strictEqual(after.navTimeoutNode, '0.000000,0.000000,0.000000');
        assert.strictEqual(after.navTicks, '1100');
        assert.strictEqual(after.navClass, 'noppes.npcs.rework.nav.RwPathNavigateGround');
    } else { assert.strictEqual(fieldsAt(101, 0).navId, after.navId); }
    assert.notStrictEqual(fieldsAt(99, 2).navId, before.navId);
    assert.strictEqual(fieldsAt(99, 1).navId, '0');
    assert.strictEqual(fieldsAt(99, 1).recoveryId, '0');
    assert.strictEqual(fieldsAt(99, 1).recoveryIndex, '-1');
    assert.strictEqual(fieldsAt(560, 0).recoveryId, before.recoveryId);
} else {
    assert(rows.every(function (line) { return line.indexOf(' recoveryId=') < 0; }));
}
// Frozen fixture objects/arrays make writes fail to change their values even under ES5.
assert(entities.every(function (e) { return e.posZ === 78.8 && e.motionZ === 0.1 && e.moveForward === 0; }));
if (process.argv[2]) { fs.writeFileSync(process.argv[2], output.join('\n'), 'utf8'); }
console.log('M4.14b diagnostic overlay: timer, reflection, complete sampling and post-DONE contracts; no Minecraft validation.');
