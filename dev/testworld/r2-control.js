// M2.3 - reprodukcija R2: leteci NPC in ovira. Scenarij: docs/scenariji/M2.3-R2.md
//
// Skripta tece na krmilnem NPC-ju R2_Control. En skriptni tick je 10 server tickov
// (izmerjeno pri M2.2). Vse meritve gredo v konzolo z markerjem R2-*, ker je log
// edini kanal, ki ga .\r2-run.ps1 lahko prebere.
//
// Tri proge hkrati, v istem svetu in istem ticku:
//   F  leteci  + zid    merjeno stanje
//   W  kopenski + zid   pove, ali je zid sploh ovira
//   P  leteci  + prosto pove, ali letenje deluje brez ovire
// Brez obeh kontrol razlika ne pomeni nicesar; razlogi v scenariju.

var TICKS_PER_SCRIPT_TICK = 10;
var SETUP_AT = 4;
var A_START = 6;
var A_END = 50;
var B_START = 54;
var B_END = 98;
var C_START = 102;
var C_END = 146;
var SAMPLE_EVERY = 2;

var START_Z = -16;
var WALL_Z = -24;
var GOAL_Z = -32;
var GOAL_Y = 4;
var GOAL_X_F = -30;
var GOAL_X_W = -10;
var GOAL_X_P = 32;
var SPEED = 1.0;

// Pot velja za celo, ce je zadnja tocka poti blizje cilju od te meje. Meja ni 0,
// ker so tocke poti blokovne koordinate, cilj pa sredina bloka.
var CELA_TOLERANCA = 2.0;
// Pod tem premikom med dvema vzorcema NPC steje za mirujocega. To je stevilcna
// oblika stanja WAIT iz FlyingMoveHelper:50.
var ZASTOJ_PRAG = 0.05;

var t = 0;
var phase = '-';
var laneF = [];
var laneW = [];
var laneP = [];
var targetF = null;
var targetW = null;
var targetP = null;
var startPos = {};
var prevPos = {};

function say(npc, msg) { npc.executeCommand('/say ' + msg); }

function byX(a, b) { return a.getX() - b.getX(); }

function dist3(a, b) {
    var dx = a.getX() - b.getX();
    var dy = a.getY() - b.getY();
    var dz = a.getZ() - b.getZ();
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function distToGoal(e, gx) {
    var dx = e.getX() - gx;
    var dy = e.getY() - GOAL_Y;
    var dz = e.getZ() - GOAL_Z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function collect(npc) {
    var all = npc.getWorld().getAllEntities(2);
    laneF = [];
    laneW = [];
    laneP = [];
    targetF = null;
    targetW = null;
    targetP = null;
    for (var i = 0; i < all.length; i++) {
        var e = all[i];
        if (e.hasTag('r2target')) {
            if (e.getX() < -20) { targetF = e; }
            else if (e.getX() < 10) { targetW = e; }
            else { targetP = e; }
        } else if (e.hasTag('r2walker')) {
            laneW.push(e);
        } else if (e.hasTag('r2flyer')) {
            if (e.getX() < 0) { laneF.push(e); } else { laneP.push(e); }
        }
    }
    laneF.sort(byX);
    laneW.sort(byX);
    laneP.sort(byX);
}

function rememberStart(list) {
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        var p = { x: e.getX(), y: e.getY(), z: e.getZ() };
        startPos[e.getUUID()] = p;
        prevPos[e.getUUID()] = { x: p.x, y: p.y, z: p.z };
    }
}

function travelled(e) {
    var s = startPos[e.getUUID()];
    if (!s) { return 0; }
    var dx = e.getX() - s.x;
    var dy = e.getY() - s.y;
    var dz = e.getZ() - s.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function movedSinceLast(e) {
    var p = prevPos[e.getUUID()];
    if (!p) { return 1e9; }
    var dx = e.getX() - p.x;
    var dy = e.getY() - p.y;
    var dz = e.getZ() - p.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function markPrev(e) {
    prevPos[e.getUUID()] = { x: e.getX(), y: e.getY(), z: e.getZ() };
}

function celaPot(e, gx) {
    var p = e.getNavigationPath();
    if (!p) { return false; }
    var dx = p.getX() - gx;
    var dy = p.getY() - GOAL_Y;
    var dz = p.getZ() - GOAL_Z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz) < CELA_TOLERANCA;
}

function spread(list) {
    var max = 0;
    for (var i = 0; i < list.length; i++) {
        for (var j = i + 1; j < list.length; j++) {
            var d = dist3(list[i], list[j]);
            if (d > max) { max = d; }
        }
    }
    return max;
}

function driveNavigate(list, gx) {
    for (var i = 0; i < list.length; i++) {
        list[i].navigateTo(gx, GOAL_Y, GOAL_Z, SPEED);
    }
}

function driveAttack(list, target) {
    if (target === null) { return; }
    for (var i = 0; i < list.length; i++) {
        list[i].setAttackTarget(target);
    }
}

function stopAll(list) {
    for (var i = 0; i < list.length; i++) {
        list[i].clearNavigation();
        list[i].setAttackTarget(null);
    }
}

function resetToStart(list) {
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        var s = startPos[e.getUUID()];
        var x = s ? s.x : e.getX();
        e.setPosition(x, GOAL_Y, START_Z);
    }
}

function sample(npc, lane, list, gx) {
    if (list.length === 0) { return; }
    var navig = 0;
    var cele = 0;
    var cez = 0;
    var zastoj = 0;
    var travelSum = 0;
    var travelMax = 0;
    var ySum = 0;
    var yMax = -1e9;
    var goalMin = 1e9;
    var goalSum = 0;
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        if (e.isNavigating()) { navig++; }
        if (celaPot(e, gx)) { cele++; }
        if (e.getZ() < WALL_Z) { cez++; }
        if (movedSinceLast(e) < ZASTOJ_PRAG) { zastoj++; }
        var tr = travelled(e);
        travelSum += tr;
        if (tr > travelMax) { travelMax = tr; }
        var y = e.getY();
        ySum += y;
        if (y > yMax) { yMax = y; }
        var g = distToGoal(e, gx);
        goalSum += g;
        if (g < goalMin) { goalMin = g; }
        markPrev(e);
    }
    var n = list.length;
    say(npc, 'R2-S faza=' + phase
        + ' tick=' + (t * TICKS_PER_SCRIPT_TICK)
        + ' proga=' + lane
        + ' navig=' + navig + '/' + n
        + ' cele=' + cele + '/' + n
        + ' prevozenoPovp=' + (travelSum / n).toFixed(2)
        + ' prevozenoMax=' + travelMax.toFixed(2)
        + ' yPovp=' + (ySum / n).toFixed(2)
        + ' yMax=' + yMax.toFixed(2)
        + ' cezOviro=' + cez + '/' + n
        + ' doCiljaMin=' + goalMin.toFixed(2)
        + ' doCiljaPovp=' + (goalSum / n).toFixed(2)
        + ' razpon=' + spread(list).toFixed(2)
        + ' zastoj=' + zastoj + '/' + n);
}

function sampleAll(npc) {
    sample(npc, 'F', laneF, GOAL_X_F);
    sample(npc, 'W', laneW, GOAL_X_W);
    sample(npc, 'P', laneP, GOAL_X_P);
}

function navigateAll() {
    driveNavigate(laneF, GOAL_X_F);
    driveNavigate(laneW, GOAL_X_W);
    driveNavigate(laneP, GOAL_X_P);
}

function endPhase(npc, marker) {
    sampleAll(npc);
    say(npc, marker);
    stopAll(laneF);
    stopAll(laneW);
    stopAll(laneP);
    resetToStart(laneF);
    resetToStart(laneW);
    resetToStart(laneP);
    phase = '-';
}

function startPhase(npc, name, marker) {
    phase = name;
    rememberStart(laneF);
    rememberStart(laneW);
    rememberStart(laneP);
    say(npc, marker);
}

function init(e) {
    t = 0;
    say(e.npc, 'R2-INIT skriptni tick = ' + TICKS_PER_SCRIPT_TICK + ' server tickov');
}

function tick(e) {
    t++;
    var npc = e.npc;

    if (t === SETUP_AT) {
        collect(npc);
        rememberStart(laneF);
        rememberStart(laneW);
        rememberStart(laneP);
        say(npc, 'R2-SETUP progaF=' + laneF.length
            + ' progaW=' + laneW.length
            + ' progaP=' + laneP.length
            + ' ciljF=' + (targetF !== null ? 'da' : 'NE')
            + ' ciljW=' + (targetW !== null ? 'da' : 'NE')
            + ' ciljP=' + (targetP !== null ? 'da' : 'NE'));
        return;
    }

    if (t === A_START) { startPhase(npc, 'A', 'R2-A-START navigateTo en sam klic'); navigateAll(); return; }
    if (t === A_END)   { endPhase(npc, 'R2-A-END'); return; }

    if (t === B_START) { startPhase(npc, 'B', 'R2-B-START navigateTo osvezen ob vsakem vzorcu'); navigateAll(); return; }
    if (t === B_END)   { endPhase(npc, 'R2-B-END'); return; }

    if (t === C_START) {
        startPhase(npc, 'C', 'R2-C-START setAttackTarget');
        driveAttack(laneF, targetF);
        driveAttack(laneW, targetW);
        driveAttack(laneP, targetP);
        return;
    }
    if (t === C_END) {
        endPhase(npc, 'R2-C-END');
        say(npc, 'R2-SUM progaF=' + laneF.length + ' progaW=' + laneW.length + ' progaP=' + laneP.length);
        return;
    }

    if (phase === '-') { return; }
    if (t % SAMPLE_EVERY !== 0) { return; }

    sampleAll(npc);
    if (phase === 'B') { navigateAll(); }
}
