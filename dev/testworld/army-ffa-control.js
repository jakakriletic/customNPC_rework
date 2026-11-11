// M11.11: free-for-all march over an obstacle course; observe real positions only, never teleport.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var Log = Java.type('noppes.npcs.LogWriter');
var last = {}, jumps = 0;
// A member that moves more than 10 blocks between two script ticks (10 game ticks) did not walk.
function jumpCheck(members) {
    for (var i = 0; i < members.length; i++) {
        var m = members[i], id = String(m.getUUID()), p = last[id];
        if (p && Math.sqrt(Math.pow(m.getX() - p[0], 2) + Math.pow(m.getZ() - p[1], 2)) > 10) {
            jumps++;
            Log.info('ARMYFFA JUMP uuid=' + id + ' from=' + p[0] + ',' + p[1] + ' to=' + m.getX() + ',' + m.getY() + ',' + m.getZ());
        }
        last[id] = [m.getX(), m.getZ()];
    }
}
var GOAL_Z = 115, age = 0, checks = 0, failed = 0, done = false, watch = false;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('ARMYFFA CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function finish(npc, phase) {
    done = true; npc.getStoreddata().put('m1111done', 1);
    Log.info('ARMYFFA DONE phase=' + phase + ' checks=' + checks + ' failed=' + failed);
}
function setup(npc) {
    check('create', A.create('ffa', 'faction:0'), 'army');
    var all = npc.getWorld().getAllEntities(2), n = 0, box = 0;
    for (var i = 0; i < all.length; i++) {
        var name = String(all[i].getName());
        if (name !== 'FFA_M' && name !== 'FFA_Box') { continue; }
        check('assign-' + n, A.assign(all[i], 'ffa'), all[i].getUUID());
        all[i].getStoreddata().put('m1111x', all[i].getX());
        all[i].getStoreddata().put('m1111z', all[i].getZ()); n++;
        if (name === 'FFA_Box') { box++; Log.info('ARMYFFA BOX y=' + all[i].getY() + ' x=' + all[i].getX() + ' z=' + all[i].getZ()); }
    }
    check('fixtures200', n === 200 && box === 1 && A.members('ffa').length === 200, n + ' box=' + box);
    check('move-freeforall', A.move('ffa', 0, 0, 4, GOAL_Z, 'freeforall', 3, 'engage'), A.status('ffa'));
    finish(npc, 'setup'); watch = true;
}
function observe(npc) {
    var status = String(A.status('ffa')), members = A.members('ffa');
    jumpCheck(members);
    if (watch) {
        if (status.indexOf('state=BLOCKED') >= 0 || status.indexOf('state=ARRIVED') >= 0) {
            Log.info('ARMYFFA FAILURE before-restart ' + status); watch = false; return;
        }
        var past = 0;
        for (var i = 0; i < members.length; i++) { if (members[i].getZ() >= 35) { past++; } }
        if (age % 200 === 0) { Log.info('ARMYFFA PROGRESS past-wall=' + past + ' ' + status); }
        age += 10;
        if (past >= 100 && status.indexOf('state=MOVING') >= 0) {
            Log.info('ARMYFFA MID past-wall=' + past + ' ' + status); watch = false;
        }
        return;
    }
    if (age % 400 === 0) { Log.info('ARMYFFA PROGRESS ' + status); }
    if (status.indexOf('state=BLOCKED') < 0 && status.indexOf('state=ARRIVED') < 0 && age < 12000) { return; }
    check('arrived-state', status.indexOf('state=ARRIVED') >= 0, status);
    var free = 0, near = 0, boxStill = false, minMove = 1e9, positions = [];
    for (var i = 0; i < members.length; i++) {
        var m = members[i], d = m.getStoreddata(), name = String(m.getName());
        var goal = Math.sqrt(m.getX() * m.getX() + Math.pow(m.getZ() - GOAL_Z, 2));
        Log.info('ARMYFFA POSITION uuid=' + m.getUUID() + ' name=' + name + ' x=' + m.getX() + ' y=' + m.getY()
            + ' z=' + m.getZ() + ' goal=' + goal);
        if (name === 'FFA_Box') { boxStill = m.getX() > 57 && m.getX() < 63 && m.getZ() > -3 && m.getZ() < 3; continue; }
        free++;
        if (goal <= 25) { near++; positions.push([m.getX(), m.getZ()]); }
        var moved = Math.sqrt(Math.pow(m.getX() - Number(d.get('m1111x')), 2) + Math.pow(m.getZ() - Number(d.get('m1111z')), 2));
        minMove = Math.min(minMove, moved);
    }
    check('no-jumps', jumps === 0, jumps);
    check('members200', members.length === 200, members.length);
    var arrived = /arrived=([0-9]+)/.exec(status), blocked = /blocked=([0-9]+)/.exec(status);
    check('enclosed-stays-boxed', boxStill, 'box');
    check('free-near-goal', near >= 195, near + '/' + free);
    check('reported-arrived', arrived !== null && Number(arrived[1]) >= 195, status);
    var minPair = 1e9;
    for (var a = 0; a < positions.length; a++) {
        for (var b = a + 1; b < positions.length; b++) {
            minPair = Math.min(minPair, Math.sqrt(Math.pow(positions[a][0] - positions[b][0], 2) + Math.pow(positions[a][1] - positions[b][1], 2)));
        }
    }
    check('distinct-slots', minPair >= 0.8, minPair);
    finish(npc, 'resume');
}
function tick(e) {
    if (watch) { observe(e.npc); return; }
    if (done || e.npc.getStoreddata().has('m1111done') || !A.enabled()) { return; }
    age += 10; if (age < 40) { return; }
    if (String(e.npc.getName()) === 'FFA_Resume') { observe(e.npc); }
    else { setup(e.npc); }
}
