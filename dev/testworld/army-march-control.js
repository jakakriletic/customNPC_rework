// M11.5: observe real member positions; never teleport a member after spawn.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var age = 0, checks = 0, failed = 0, done = false, watch = false;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('ARMYLONG CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function finish(npc, phase) {
    done = true; npc.getStoreddata().put('m115done', 1);
    Log.info('ARMYLONG DONE phase=' + phase + ' checks=' + checks + ' failed=' + failed);
}
function setup(npc) {
    check('range32', S.NpcNavRange === 32, S.NpcNavRange);
    check('create', A.create('north', 'faction:0'), 'army');
    var all = npc.getWorld().getAllEntities(2), n = 0;
    for (var i = 0; i < all.length; i++) {
        if (String(all[i].getName()) !== 'LONG_M') { continue; }
        check('assign-' + n, A.assign(all[i], 'north'), all[i].getUUID());
        all[i].getStoreddata().put('m115x', all[i].getX());
        all[i].getStoreddata().put('m115z', all[i].getZ()); n++;
    }
    check('fixtures32', n === 32 && A.members('north').length === 32, n);
    check('move240', A.move('north', 0, 0, 4, 240, 'legion', 3), A.status('north'));
    finish(npc, 'setup'); watch = true;
}
function observe(npc) {
    var status = String(A.status('north')), members = A.members('north');
    if (watch) {
        if (status.indexOf('state=BLOCKED') >= 0) {
            Log.info('ARMYLONG FAILURE before-restart ' + status); watch = false; return;
        }
        var minZ = 1e9, maxZ = -1e9;
        for (var i = 0; i < members.length; i++) { minZ = Math.min(minZ, members[i].getZ()); maxZ = Math.max(maxZ, members[i].getZ()); }
        if (members.length === 32 && minZ >= 35 && maxZ < 120 && status.indexOf('state=MOVING') >= 0) {
            Log.info('ARMYLONG MID members=32 minZ=' + minZ + ' maxZ=' + maxZ + ' ' + status); watch = false;
        }
        return;
    }
    if (status.indexOf('state=BLOCKED') >= 0 || age > 10000) {
        for (var j = 0; j < members.length; j++) {
            Log.info('ARMYLONG DEBUG uuid=' + members[j].getUUID() + ' x=' + members[j].getX()
                + ' y=' + members[j].getY() + ' z=' + members[j].getZ());
        }
        check('arrival', false, status); finish(npc, 'resume'); return;
    }
    if (status.indexOf('state=ARRIVED') < 0) { return; }
    check('arrival', true, status);
    check('four-companies', status.indexOf('companies=4 members=32') >= 0, status);
    check('multiple-paths', /totalPaths=([0-9]+)/.exec(status)[1] >= 2 && status.indexOf('paths=1') >= 0, status);
    check('members32', members.length === 32, members.length);
    var positions = [], minMove = 1e9, maxGoal = 0;
    for (var i = 0; i < members.length; i++) {
        var m = members[i], d = m.getStoreddata();
        check('saved-origin-' + i, d.has('m115x') && d.has('m115z'), m.getUUID());
        var moved = Math.sqrt(Math.pow(m.getX() - Number(d.get('m115x')), 2) + Math.pow(m.getZ() - Number(d.get('m115z')), 2));
        var goal = Math.sqrt(m.getX() * m.getX() + Math.pow(m.getZ() - 240, 2));
        minMove = Math.min(minMove, moved); maxGoal = Math.max(maxGoal, goal);
        positions.push([m.getX(), m.getZ()]);
        Log.info('ARMYLONG POSITION uuid=' + m.getUUID() + ' x=' + m.getX() + ' y=' + m.getY() + ' z=' + m.getZ() + ' moved=' + moved + ' goal=' + goal);
    }
    check('all-walked200', minMove >= 200, minMove);
    check('all-near-goal', maxGoal <= 12, maxGoal);
    var minPair = 1e9;
    for (var a = 0; a < positions.length; a++) {
        for (var b = a + 1; b < positions.length; b++) {
            minPair = Math.min(minPair, Math.sqrt(Math.pow(positions[a][0] - positions[b][0], 2) + Math.pow(positions[a][1] - positions[b][1], 2)));
        }
    }
    check('distinct-slots', minPair >= 1, minPair);
    finish(npc, 'resume');
}
function tick(e) {
    if (watch) { observe(e.npc); return; }
    if (done || e.npc.getStoreddata().has('m115done') || !A.enabled()) { return; }
    age += 10; if (age < 40) { return; }
    if (String(e.npc.getName()) === 'LONG_Resume') { observe(e.npc); }
    else { setup(e.npc); }
}
