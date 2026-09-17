// M2.7 - merila kakovosti navigacije. Scenarij: docs/scenariji/M2.7-navigacija.md
//
// Skripta tece na krmilnem NPC-ju NAV_Control. En skriptni tick je 10 server tickov
// (izmerjeno pri M2.2). Vse meritve gredo v konzolo z markerjem NAV-*, ker je log
// edini kanal, ki ga .\nav-run.ps1 lahko prebere.
//
// Dve progi hkrati, v istem svetu in istem ticku, obe kopenski:
//   G  skupina 8 + zid z enimi vrati   merjeno stanje (ozko grlo)
//   O  skupina 8 na odprtem            pove, ali skupina sploh pride 16 blokov dalec
// Brez kontrole O cas skupine G ne pomeni nicesar: 'ni prisla' ima dve povsem razlicni
// razlagi (ozko grlo ali navigacija sploh ne dela) in vsaka vodi v drug popravek.
//
// Dve fazi, ker se M4.10 meri prav na razliki med njima:
//   A  navigateTo enkrat        pokaze, kako dalec pride NPC z eno samo potjo
//   B  navigateTo ob vsakem vzorcu   pokaze cas do cilja, ko se pot osvezuje
// Vanilla A* vrne delno pot (PathFinder.java:65) in NPC obstane na njenem koncu; faza A
// je zato izhodiscna meritev za paket M4.10 (nadaljevanje delne poti).

var TICKS_PER_SCRIPT_TICK = 10;
var SETUP_AT = 4;
var A_START = 6;
var A_END = 50;
var B_START = 54;
var B_END = 110;
var SAMPLE_EVERY = 2;

var START_Z = 64;
var WALL_Z = 72;
var GOAL_Z = 78;
var GOAL_Y = 4;
var GOAL_X_G = -10;
var GOAL_X_O = 15;
var DOOR_X = -10;
var SPEED = 1.0;

// Pot velja za celo, ce je zadnja tocka poti blizje cilju od te meje. Meja ni 0, ker so
// tocke poti blokovne koordinate, cilj pa sredina bloka. Ista meja je v NavProbe.java.
var CELA_TOLERANCA = 2.0;
// Do te razdalje od cilja steje NPC za prispelega.
var PRISPEL = 2.0;
// Pod tem premikom med dvema vzorcema NPC steje za mirujocega.
var ZASTOJ_PRAG = 0.05;
// Pas okoli zidu, v katerem se meri razpon skupine na ozkem grlu.
var GRLO_PAS = 3.0;
// NPC za zidom in dlje od tega od vrat je zid obsel; takrat grlo ni grlo. Za progo O,
// kjer zidu ni, ista stevilka pove, koliko jih je zaslo vstran od svoje proge.
var DALEC_OD_VRAT = 8.0;

var t = 0;
var phase = '-';
var phaseStart = 0;
var laneG = [];
var laneO = [];
var startPos = {};
var prevPos = {};
var prevAge = {};
var arrived = {};
var arrivalTimes = {};
var grloMax = {};
var razponMax = {};
var cezDalecMax = {};

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
    laneG = [];
    laneO = [];
    for (var i = 0; i < all.length; i++) {
        var e = all[i];
        if (e.hasTag('navwalkg')) { laneG.push(e); }
        else if (e.hasTag('navwalko')) { laneO.push(e); }
    }
    laneG.sort(byX);
    laneO.sort(byX);
}

function rememberStart(list) {
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        var p = { x: e.getX(), y: e.getY(), z: e.getZ() };
        startPos[e.getUUID()] = p;
        prevPos[e.getUUID()] = { x: p.x, y: p.y, z: p.z };
        prevAge[e.getUUID()] = e.getAge();
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

// Koliko server tickov je entiteta prestala od prejsnjega vzorca. Pricakovano je
// SAMPLE_EVERY * TICKS_PER_SCRIPT_TICK = 20. Nic pomeni, da World.updateEntity te
// entitete sploh ne poklice - takrat meritev ne govori o navigaciji, ampak o
// posodabljanju entitet (M2.1d, pojav P1 iz M2.3).
function ageDelta(e) {
    var a = prevAge[e.getUUID()];
    if (a === undefined) { return -1; }
    return e.getAge() - a;
}

function motionLen(e) {
    var mx = e.getMotionX();
    var my = e.getMotionY();
    var mz = e.getMotionZ();
    return Math.sqrt(mx * mx + my * my + mz * mz);
}

function markPrev(e) {
    prevPos[e.getUUID()] = { x: e.getX(), y: e.getY(), z: e.getZ() };
    prevAge[e.getUUID()] = e.getAge();
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

function stopAll(list) {
    for (var i = 0; i < list.length; i++) {
        list[i].clearNavigation();
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

function key(lane) { return phase + lane; }

function resetPhaseState(lane, list) {
    arrived[key(lane)] = [];
    grloMax[key(lane)] = 0;
    razponMax[key(lane)] = 0;
    cezDalecMax[key(lane)] = 0;
}

function sample(npc, lane, list, gx, doorX) {
    if (list.length === 0) { return; }
    var k = key(lane);
    var navig = 0;
    var cele = 0;
    var prispelo = 0;
    var zastoj = 0;
    var cez = 0;
    var cezDalec = 0;
    var travelSum = 0;
    var travelMax = 0;
    var goalMin = 1e9;
    var goalSum = 0;
    var ageMin = 1e9;
    var ageMax = -1e9;
    var gibSum = 0;
    var gibMax = 0;
    var priGrlu = [];
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        var da = ageDelta(e);
        if (da < ageMin) { ageMin = da; }
        if (da > ageMax) { ageMax = da; }
        var gl = motionLen(e);
        gibSum += gl;
        if (gl > gibMax) { gibMax = gl; }
        if (e.isNavigating()) { navig++; }
        if (celaPot(e, gx)) { cele++; }
        var g = distToGoal(e, gx);
        goalSum += g;
        if (g < goalMin) { goalMin = g; }
        if (g < PRISPEL) {
            prispelo++;
            if (arrived[k].indexOf(e.getUUID()) < 0) {
                arrived[k].push(e.getUUID());
                noteArrival(lane, (t - phaseStart) * TICKS_PER_SCRIPT_TICK);
                say(npc, 'NAV-PRISPEL faza=' + phase + ' proga=' + lane
                    + ' tick=' + ((t - phaseStart) * TICKS_PER_SCRIPT_TICK)
                    + ' kdo=' + arrived[k].length + '/' + list.length);
            }
        }
        if (e.getZ() > WALL_Z) {
            cez++;
            if (Math.abs(e.getX() - doorX) > DALEC_OD_VRAT) { cezDalec++; }
        }
        if (Math.abs(e.getZ() - WALL_Z) <= GRLO_PAS) { priGrlu.push(e); }
        if (movedSinceLast(e) < ZASTOJ_PRAG) { zastoj++; }
        var tr = travelled(e);
        travelSum += tr;
        if (tr > travelMax) { travelMax = tr; }
        markPrev(e);
    }
    var n = list.length;
    var razpon = spread(list);
    var razponGrlo = priGrlu.length > 1 ? spread(priGrlu) : 0;
    if (razpon > razponMax[k]) { razponMax[k] = razpon; }
    if (razponGrlo > grloMax[k]) { grloMax[k] = razponGrlo; }
    if (cezDalec > cezDalecMax[k]) { cezDalecMax[k] = cezDalec; }
    say(npc, 'NAV-S faza=' + phase
        + ' tick=' + ((t - phaseStart) * TICKS_PER_SCRIPT_TICK)
        + ' proga=' + lane
        + ' navig=' + navig + '/' + n
        + ' cele=' + cele + '/' + n
        + ' prispelo=' + prispelo + '/' + n
        + ' cez=' + cez + '/' + n
        + ' cezDalec=' + cezDalec + '/' + n
        + ' priGrlu=' + priGrlu.length + '/' + n
        + ' prevozenoPovp=' + (travelSum / n).toFixed(2)
        + ' prevozenoMax=' + travelMax.toFixed(2)
        + ' doCiljaMin=' + goalMin.toFixed(2)
        + ' doCiljaPovp=' + (goalSum / n).toFixed(2)
        + ' razpon=' + razpon.toFixed(2)
        + ' razponGrlo=' + razponGrlo.toFixed(2)
        + ' zastoj=' + zastoj + '/' + n
        + ' dStarost=' + ageMin + '/' + ageMax
        + ' gib=' + (gibSum / n).toFixed(4) + '/' + gibMax.toFixed(4));
}

// Cas do cilja je uporaben samo skupaj s tem, koliko jih je prislo. Mediana osmih
// prihodov in mediana dveh prihodov sta razlicni stevilki z istim imenom.
function timeLine(npc, lane, list) {
    var k = key(lane);
    var times = arrivalTimes[k] ? arrivalTimes[k] : [];
    var sorted = times.slice().sort(function (a, b) { return a - b; });
    var prvi = sorted.length > 0 ? sorted[0] : -1;
    var zadnji = sorted.length > 0 ? sorted[sorted.length - 1] : -1;
    var mediana = sorted.length > 0 ? sorted[Math.floor((sorted.length - 1) / 2)] : -1;
    say(npc, 'NAV-CAS faza=' + phase
        + ' proga=' + lane
        + ' prispelo=' + sorted.length + '/' + list.length
        + ' prviTick=' + prvi
        + ' medianaTick=' + mediana
        + ' zadnjiTick=' + zadnji
        + ' razponGrloMax=' + grloMax[k].toFixed(2)
        + ' razponMax=' + razponMax[k].toFixed(2)
        + ' cezDalecMax=' + cezDalecMax[k]);
}

function noteArrival(lane, ticks) {
    var k = key(lane);
    if (!arrivalTimes[k]) { arrivalTimes[k] = []; }
    arrivalTimes[k].push(ticks);
}

// Progi imata svojo os: pri G je to os vrat, pri O pa os same proge. Ista stevilka
// cezDalec zato pri G pomeni 'zid je obsel', pri O pa 'zasel je s proge'.
function sampleAll(npc) {
    sample(npc, 'G', laneG, GOAL_X_G, DOOR_X);
    sample(npc, 'O', laneO, GOAL_X_O, GOAL_X_O);
}

function navigateAll() {
    driveNavigate(laneG, GOAL_X_G);
    driveNavigate(laneO, GOAL_X_O);
}

function startPhase(npc, name, marker) {
    phase = name;
    phaseStart = t;
    rememberStart(laneG);
    rememberStart(laneO);
    resetPhaseState('G', laneG);
    resetPhaseState('O', laneO);
    arrivalTimes[key('G')] = [];
    arrivalTimes[key('O')] = [];
    say(npc, marker);
}

function endPhase(npc, marker) {
    sampleAll(npc);
    timeLine(npc, 'G', laneG);
    timeLine(npc, 'O', laneO);
    say(npc, marker);
    stopAll(laneG);
    stopAll(laneO);
    resetToStart(laneG);
    resetToStart(laneO);
    phase = '-';
}

function init(e) {
    t = 0;
    say(e.npc, 'NAV-INIT skriptni tick = ' + TICKS_PER_SCRIPT_TICK + ' server tickov');
}

function tick(e) {
    t++;
    var npc = e.npc;

    if (t === SETUP_AT) {
        collect(npc);
        rememberStart(laneG);
        rememberStart(laneO);
        say(npc, 'NAV-SETUP progaG=' + laneG.length + ' progaO=' + laneO.length
            + ' vrataX=' + DOOR_X + ' zidZ=' + WALL_Z + ' ciljZ=' + GOAL_Z);
        return;
    }

    if (t === A_START) { startPhase(npc, 'A', 'NAV-A-START navigateTo en sam klic'); navigateAll(); return; }
    if (t === A_END)   { endPhase(npc, 'NAV-A-END'); return; }

    if (t === B_START) { startPhase(npc, 'B', 'NAV-B-START navigateTo osvezen ob vsakem vzorcu'); navigateAll(); return; }
    if (t === B_END) {
        endPhase(npc, 'NAV-B-END');
        say(npc, 'NAV-SUM progaG=' + laneG.length + ' progaO=' + laneO.length);
        return;
    }

    if (phase === '-') { return; }
    if (t % SAMPLE_EVERY !== 0) { return; }

    sampleAll(npc);
    if (phase === 'B') { navigateAll(); }
}
