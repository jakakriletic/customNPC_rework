// M11.9 evidence only. Workers are never teleported, healed or moved by this script.
// Terrain and initial positions are built by the controller before orders begin.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var Damage = Java.type('net.minecraft.util.DamageSource');
var RCon = Java.type('net.minecraft.network.rcon.RConConsoleSource');
var names = ['water', 'gap', 'throat', 'trap', 'loss'];
var centres = [0.5, 48.5, 96.5, 144.5, 192.5];
var owner = 'uuid:00000000-0000-4000-8000-000000000119';
var age = 0, phaseAge = 0, checks = 0, failed = 0, phase = '', done = false;
var finished = {}, last = {}, metrics = {}, injected = false, stable = {}, victims = {};
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('ARMYEDGE CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function status(name) { return String(A.status(name)); }
function state(name) { var m = /state=([A-Z]+)/.exec(status(name)); return m ? m[1] : ''; }
function command(text) {
    var rcon = new RCon(S.Server); S.Server.getCommandManager().executeCommand(rcon, text);
    return String(rcon.getLogContents());
}
function end(npc, label) {
    done = true; npc.getStoreddata().put('m119done', 1);
    Log.info('ARMYEDGE DONE phase=' + label + ' checks=' + checks + ' failed=' + failed);
}
function observe(name) {
    var members = A.members(name), m = metrics[name];
    for (var i = 0; i < members.length; i++) {
        var n = members[i], mc = n.getMCEntity(), id = String(n.getUUID());
        // A no-respawn victim stays loaded at health 0 for its 20-tick vanilla death; survivors stay measured.
        if (mc.isDead || mc.isKilled() || victims[id]) { continue; }
        var x = n.getX(), y = n.getY(), z = n.getZ(), old = last[id];
        if (!isFinite(x) || !isFinite(y) || !isFinite(z)) { m.finite = false; }
        m.minY = Math.min(m.minY, y); m.maxZ = Math.max(m.maxZ, z);
        m.minHealth = Math.min(m.minHealth, n.getHealth());
        if (old) {
            m.maxStep = Math.max(m.maxStep, Math.sqrt(Math.pow(x - old[0], 2) + Math.pow(z - old[2], 2)));
        }
        last[id] = [x, y, z]; m.samples++;
    }
}
function report(name, expected, count, x, z, radius) {
    var members = A.members(name), m = metrics[name], furthest = 0, leastMove = 1e9;
    check(name + '-state', state(name) === expected, status(name));
    check(name + '-members', members.length === count, members.length);
    check(name + '-finite', m.finite, m.samples);
    check(name + '-samples', m.samples > 0, m.samples);
    // Tick hook samples every ten ticks. At speed 3 a normal step is well below six blocks.
    check(name + '-no-position-jump', m.maxStep <= 6, m.maxStep);
    check(name + '-alive', m.minHealth > 0, m.minHealth);
    check(name + '-no-fall', m.minY >= (name === 'water' ? 2.8 : 3.8), m.minY);
    for (var i = 0; i < members.length; i++) {
        var n = members[i], d = n.getStoreddata();
        furthest = Math.max(furthest, Math.sqrt(Math.pow(n.getX() - x, 2) + Math.pow(n.getZ() - z, 2)));
        leastMove = Math.min(leastMove, Math.sqrt(Math.pow(n.getX() - Number(d.get('m119x')), 2)
            + Math.pow(n.getZ() - Number(d.get('m119z')), 2)));
        Log.info('ARMYEDGE POSITION army=' + name + ' uuid=' + n.getUUID() + ' x=' + n.getX()
            + ' y=' + n.getY() + ' z=' + n.getZ() + ' health=' + n.getHealth());
    }
    if (expected === 'ARRIVED') {
        check(name + '-all-at-goal', furthest <= radius, furthest);
        check(name + '-all-walked', leastMove >= 12, leastMove);
    }
    if (name === 'gap') { check('gap-no-crossing', m.maxZ < 7, m.maxZ); }
    Log.info('ARMYEDGE METRIC army=' + name + ' state=' + state(name) + ' samples=' + m.samples
        + ' minY=' + m.minY + ' maxZ=' + m.maxZ + ' minHealth=' + m.minHealth
        + ' maxStep=' + m.maxStep + ' maxGoal=' + furthest + ' minMove=' + leastMove);
}
function setup(npc) {
    var w = npc.getWorld();
    var blocks = [[0,3,8,'minecraft:water'], [48,0,8,'minecraft:air'], [48,3,8,'minecraft:air'],
        [94,4,10,'minecraft:stone'], [95,4,10,'minecraft:air'], [96,4,10,'minecraft:air'],
        [97,4,10,'minecraft:stone'], [141,4,-3,'minecraft:air'], [141,6,-3,'minecraft:stone'],
        [140,4,-3,'minecraft:stone'], [142,4,-3,'minecraft:stone']];
    for (var b = 0; b < blocks.length; b++) {
        var p = blocks[b], actual = String(w.getBlock(p[0], p[1], p[2]).getName());
        check('terrain-block-' + b, actual === p[3], p + ' actual=' + actual);
    }
    var all = npc.getWorld().getAllEntities(2);
    for (var j = 0; j < names.length; j++) {
        var name = names[j], count = 0;
        metrics[name] = {finite:true, minY:1e9, maxZ:-1e9, minHealth:1e9, maxStep:0, samples:0};
        check(name + '-create', A.create(name, owner), 'new register entry');
        for (var i = 0; i < all.length; i++) {
            var n = all[i]; if (String(n.getName()) !== 'EDGE_' + name) { continue; }
            check(name + '-assign-' + count, A.assign(n, name), n.getUUID());
            n.getStoreddata().put('m119x', n.getX()); n.getStoreddata().put('m119z', n.getZ());
            // True permanent deaths, rather than delete-as-a-substitute or a respawn after twenty seconds.
            n.getStats().setRespawnType(3); count++;
        }
        check(name + '-fixture-eight', count === 8 && A.members(name).length === 8, count);
        observe(name);
        check(name + '-move', A.move(name, 0, centres[j], 4, 24.5, 'legion', 3, 'passive'), status(name));
    }
    phase = 'terrain'; phaseAge = 0;
}
function losses() {
    var members = A.members('loss');
    if (injected || members.length !== 8 || state('loss') !== 'MOVING') { return; }
    var walked = 0;
    for (var i = 0; i < members.length; i++) { walked = Math.max(walked,
        Math.abs(members[i].getZ() - Number(members[i].getStoreddata().get('m119z')))); }
    if (walked < 1.5) { return; }
    injected = true;
    check('loss-mid-march', true, status('loss') + ' walked=' + walked);
    for (var j = 0; j < 2; j++) {
        var victim = members[j].getMCEntity();
        victims[String(members[j].getUUID())] = true;
        check('loss-damage-' + j, victim.attackEntityFrom(Damage.OUT_OF_WORLD, 1000), victim.getUniqueID());
        check('loss-dead-' + j, !victim.isEntityAlive() && victim.getHealth() <= 0, victim.getHealth());
    }
    var left = members[2]; left.getStoreddata().put('m119left', 1);
    check('loss-unassign', A.unassign(left), left.getUUID());
    check('loss-unassigned-nbt', A.armyOf(left) === '', A.armyOf(left));
    Log.info('ARMYEDGE LOSS killed=2 left=1 ' + status('loss'));
}
function verifyDeparture(npc) {
    var all = npc.getWorld().getAllEntities(2), left = 0, living = 0;
    for (var i = 0; i < all.length; i++) {
        var n = all[i]; if (String(n.getName()) !== 'EDGE_loss') { continue; }
        if (!n.getMCEntity().isDead && !n.getMCEntity().isKilled()) { living++; }
        if (n.getStoreddata().has('m119left')) { left++; check('left-still-independent', A.armyOf(n) === '', n.getUUID()); }
    }
    check('loss-no-respawn', living === 6 && left === 1, 'living=' + living + ' left=' + left);
}
function tick(e) {
    var npc = e.npc;
    if (done || npc.getStoreddata().has('m119done') || !A.enabled()) { return; }
    age += 10; phaseAge += 10; if (age < 40) { return; }
    if (phase === '') {
        if (String(npc.getName()) === 'EDGE_Resume') {
            check('reload-intent', status('loss').indexOf('target=192.5,4.0,0.5') >= 0
                && status('loss').indexOf('stance=PASSIVE') >= 0, status('loss'));
            check('reload-five-members', A.members('loss').length === 5, A.members('loss').length);
            verifyDeparture(npc); phase = 'resume'; phaseAge = 0;
        } else { setup(npc); return; }
    }
    if (phase === 'terrain') {
        losses();
        for (var j = 0; j < names.length; j++) {
            var name = names[j]; if (finished[name]) { continue; }
            observe(name);
            var s = state(name);
            if (phaseAge % 100 === 0) { Log.info('ARMYEDGE SAMPLE ' + status(name)); }
            if ((s === 'ARRIVED' || s === 'BLOCKED') && (name !== 'loss' || injected) || phaseAge >= 2600) {
                report(name, name === 'gap' || name === 'trap' ? 'BLOCKED' : 'ARRIVED', name === 'loss' ? 5 : 8,
                    centres[j], 24.5, 8);
                finished[name] = true;
            }
        }
        for (var k = 0; k < names.length; k++) { if (!finished[names[k]]) { return; } }
        check('loss-injected', injected, 'two deaths and one unassignment while MOVING');
        verifyDeparture(npc);
        var survivors = A.members('loss');
        for (var b = 0; b < survivors.length; b++) {
            stable[String(survivors[b].getUUID())] = [survivors[b].getX(), survivors[b].getZ()];
        }
        phase = 'stable'; phaseAge = 0; return;
    }
    if (phase === 'stable' && phaseAge >= 100) {
        check('arrival-stays-terminal', state('loss') === 'ARRIVED', status('loss'));
        var m = A.members('loss');
        for (var i = 0; i < m.length; i++) {
            var p = stable[String(m[i].getUUID())];
            check('arrival-holds-' + i, p !== undefined && Math.sqrt(Math.pow(m[i].getX() - p[0], 2)
                + Math.pow(m[i].getZ() - p[1], 2)) <= 1.2, m[i].getUUID());
        }
        // The terrain results remain failures if any failed; never issue a new approach to obtain a pass.
        if (failed > 0) { end(npc, 'terrain'); return; }
        check('reload-return-order', A.move('loss', 0, 192.5, 4, 0.5, 'legion', 1, 'passive'), status('loss'));
        phase = 'restart'; phaseAge = 0; return;
    }
    if (phase === 'restart' && phaseAge >= 40 && state('loss') === 'MOVING') {
        check('reload-before-arrival', true, status('loss'));
        npc.getStoreddata().put('m119done', 1); done = true;
        Log.info('ARMYEDGE MID restart checks=' + checks + ' failed=' + failed + ' ' + status('loss')); return;
    }
    if (phase === 'resume') {
        if (phaseAge % 100 === 0) { Log.info('ARMYEDGE RESUME ' + status('loss')); }
        if (state('loss') === 'ARRIVED' || state('loss') === 'BLOCKED' || phaseAge >= 2600) {
            check('reload-arrival', state('loss') === 'ARRIVED', status('loss'));
            var all = A.members('loss'); check('reload-five-survivors', all.length === 5, all.length);
            for (var a = 0; a < all.length; a++) {
                var n = all[a], distance = Math.sqrt(Math.pow(n.getX() - 192.5, 2) + Math.pow(n.getZ() - 0.5, 2));
                check('reload-geometric-' + a, distance <= 8 && Math.abs(n.getY() - 4) < 0.2, distance);
                Log.info('ARMYEDGE RELOADPOSITION uuid=' + n.getUUID() + ' x=' + n.getX() + ' y=' + n.getY() + ' z=' + n.getZ());
            }
            verifyDeparture(npc);
            check('reload-stop', A.stop('loss'), status('loss'));
            check('reload-cancelled', state('loss') === 'CANCELLED', status('loss'));
            end(npc, 'resume');
        }
    }
    if (phase !== 'terrain' && phase !== 'resume' && phaseAge >= 2600) {
        check('phase-timeout', false, phase + ' ' + status('loss')); end(npc, phase);
    }
}
