// M4.16 - scenarij letenja in dometa. Scenarij: docs/scenariji/M4.16-letenje.md
//
// Skripta tece na krmilnem NPC-ju LET_Control. En skriptni tick je 10 server tickov
// (izmerjeno pri M2.2). Vse meritve gredo v konzolo z markerjem LET-*, ker je log
// edini kanal, ki ga .\letenje-run.ps1 lahko prebere.
//
// Sest vzporednih prog hkrati, v istem svetu in istem ticku, vsaka 6 NPC-jev:
//   Z   leteci  + zid cez vso sirino (kot M2.3 F)    merjeno stanje
//   V   leteci  + L-zid, cilj za vogalom na isti visini
//   L   leteci  + zaprt labirint (strop, tri stene z izmenicnimi odprtinami)
//   DF  leteci  + prosto, cilj 48 blokov stran (cez NpcNavRange 32)
//   DW  kopenski + prosto, cilj 48 blokov stran (isto za kopenske)
//   P   leteci  + prosto, cilj 16 blokov stran       kontrola, da letenje sploh dela
//
// Tri faze, vsaka 600 server tickov, vsaka z izhodisca:
//   A  navigateTo en sam klic, hitrost 1.0
//   B  navigateTo osvezen vsakih 20 tickov, hitrost 1.0
//   C  navigateTo en sam klic, hitrost 0.5 (ali navigator hitrost sploh upostevaj)

var TICKS_PER_SCRIPT_TICK = 10;
var SETUP_AT = 4;
var PHASE_LEN = 60;
var PHASES = [
    { name: 'A', start: 6, refresh: false, speed: 1.0 },
    { name: 'B', start: 70, refresh: true, speed: 1.0 },
    { name: 'C', start: 134, refresh: false, speed: 0.5 }
];
var SUM_AT = 198;
var SAMPLE_EVERY = 2;
var REFRESH_EVERY = 2;
var GOAL_Y = 4;

// Pot velja za celo, ce je zadnja tocka poti blizje cilju od te meje (kot M2.3).
var CELA_TOLERANCA = 2.0;
// NPC je prispel, ko je kdajkoli v fazi blizje cilju od te meje. Ista meja kot cele.
var PRISPEL = 2.0;
// Pod tem premikom med dvema vzorcema NPC steje za mirujocega (kot M2.3).
var ZASTOJ_PRAG = 0.05;

// Proge po x. Cilji so sredine blokov; tarca stoji na cilju.
var LANES = [
    { id: 'Z',  tag: 'm416flyer',  xMin: -90, xMax: -40, gx: -69.5, gz: -63.5 },
    { id: 'V',  tag: 'm416flyer',  xMin: -40, xMax: 5,   gx: -13.5, gz: -59.5 },
    { id: 'L',  tag: 'm416flyer',  xMin: 5,   xMax: 35,  gx: 20.5,  gz: -59.5 },
    { id: 'DF', tag: 'm416flyer',  xMin: 35,  xMax: 62,  gx: 50.5,  gz: -95.5 },
    { id: 'DW', tag: 'm416walker', xMin: 62,  xMax: 88,  gx: 75.5,  gz: -95.5 },
    { id: 'P',  tag: 'm416flyer',  xMin: 88,  xMax: 130, gx: 100.5, gz: -63.5 }
];

var t = 0;
var phase = null;
var st = {};

function say(npc, msg) { npc.executeCommand('/say ' + msg); }

function byX(a, b) { return a.getX() - b.getX(); }

function d3(ax, ay, az, bx, by, bz) {
    var dx = ax - bx;
    var dy = ay - by;
    var dz = az - bz;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function toGoal(e, lane) { return d3(e.getX(), e.getY(), e.getZ(), lane.gx, GOAL_Y, lane.gz); }

function median(values) {
    if (values.length === 0) { return 0; }
    var v = values.slice(0);
    v.sort(function (a, b) { return a - b; });
    var m = Math.floor(v.length / 2);
    if (v.length % 2 === 1) { return v[m]; }
    return (v[m - 1] + v[m]) / 2;
}

function laneFor(x, tag) {
    for (var i = 0; i < LANES.length; i++) {
        var lane = LANES[i];
        if (x >= lane.xMin && x < lane.xMax && (tag === null || lane.tag === tag)) { return lane; }
    }
    return null;
}

function collect(npc) {
    for (var i = 0; i < LANES.length; i++) { LANES[i].members = []; LANES[i].target = null; }
    var all = npc.getWorld().getAllEntities(2);
    for (var j = 0; j < all.length; j++) {
        var e = all[j];
        var lane = null;
        if (e.hasTag('m416target')) {
            lane = laneFor(e.getX(), null);
            if (lane !== null) { lane.target = e; }
        } else if (e.hasTag('m416flyer')) {
            lane = laneFor(e.getX(), 'm416flyer');
            if (lane !== null) { lane.members.push(e); }
        } else if (e.hasTag('m416walker')) {
            lane = laneFor(e.getX(), 'm416walker');
            if (lane !== null) { lane.members.push(e); }
        }
    }
    for (var k = 0; k < LANES.length; k++) { LANES[k].members.sort(byX); }
}

function rememberHome() {
    for (var i = 0; i < LANES.length; i++) {
        var list = LANES[i].members;
        for (var j = 0; j < list.length; j++) {
            var e = list[j];
            st[e.getUUID()] = { hx: e.getX(), hy: e.getY(), hz: e.getZ() };
        }
    }
}

function avgToGoal(lane) {
    var sum = 0;
    for (var j = 0; j < lane.members.length; j++) { sum += toGoal(lane.members[j], lane); }
    return lane.members.length > 0 ? sum / lane.members.length : -1;
}

function celaPot(e, lane) {
    var p = e.getNavigationPath();
    if (!p) { return false; }
    return d3(p.getX(), p.getY(), p.getZ(), lane.gx, GOAL_Y, lane.gz) < CELA_TOLERANCA;
}

function motionLen(e) {
    var mx = e.getMotionX();
    var my = e.getMotionY();
    var mz = e.getMotionZ();
    return Math.sqrt(mx * mx + my * my + mz * mz);
}

function stopAll() {
    for (var i = 0; i < LANES.length; i++) {
        var list = LANES[i].members;
        for (var j = 0; j < list.length; j++) {
            list[j].clearNavigation();
            list[j].setAttackTarget(null);
        }
    }
}

function navigateLane(lane, speed) {
    for (var j = 0; j < lane.members.length; j++) {
        lane.members[j].navigateTo(lane.gx, GOAL_Y, lane.gz, speed);
    }
}

// Postavitev in meritev zacneta v istem ticku (lekcija M2.3, zagon 13:49): ustavi,
// postavi na izhodisce, izbrisi gibanje, sele nato navigateTo in zapomni stanje.
function startPhase(npc, ph) {
    stopAll();
    phase = ph;
    for (var i = 0; i < LANES.length; i++) {
        var lane = LANES[i];
        for (var j = 0; j < lane.members.length; j++) {
            var e = lane.members[j];
            var s = st[e.getUUID()];
            e.setPosition(s.hx, s.hy, s.hz);
            e.setMotionX(0);
            e.setMotionY(0);
            e.setMotionZ(0);
        }
    }
    for (var k = 0; k < LANES.length; k++) {
        var ln = LANES[k];
        var d0 = avgToGoal(ln);
        navigateLane(ln, ph.speed);
        var navig = 0;
        var cele = 0;
        for (var m = 0; m < ln.members.length; m++) {
            var en = ln.members[m];
            var nav = en.isNavigating();
            if (nav) { navig++; }
            if (celaPot(en, ln)) { cele++; }
            var sx = st[en.getUUID()];
            sx.px = en.getX(); sx.py = en.getY(); sx.pz = en.getZ();
            sx.sampleAge = en.getAge();
            sx.samplePx = sx.px; sx.samplePy = sx.py; sx.samplePz = sx.pz;
            sx.navDist = 0; sx.navTicks = 0; sx.wasNav = nav;
            sx.arrT = -1; sx.endNav = false; sx.drift = 0;
            sx.ex = 0; sx.ey = 0; sx.ez = 0;
        }
        say(npc, 'LET-START faza=' + ph.name + ' proga=' + ln.id + ' N=' + ln.members.length
            + ' hitrost=' + ph.speed.toFixed(2) + ' doCilja0=' + d0.toFixed(2)
            + ' navig0=' + navig + ' cele0=' + cele);
    }
    say(npc, 'LET-' + ph.name + '-START');
}

// Vsak skriptni tick: pot, hitrost, prihod in drsenje po koncu navigacije.
// Drsenje je odmik od polozaja, kjer je NPC prvic zakljucil navigacijo (isNavigating
// je padel z da na ne); vzorec je na 10 tickov, zato je to spodnja meja.
function update(elapsed) {
    for (var i = 0; i < LANES.length; i++) {
        var lane = LANES[i];
        for (var j = 0; j < lane.members.length; j++) {
            var e = lane.members[j];
            var s = st[e.getUUID()];
            var x = e.getX();
            var y = e.getY();
            var z = e.getZ();
            var d = d3(x, y, z, s.px, s.py, s.pz);
            if (s.wasNav) { s.navDist += d; s.navTicks += TICKS_PER_SCRIPT_TICK; }
            s.px = x; s.py = y; s.pz = z;
            if (s.arrT < 0 && toGoal(e, lane) <= PRISPEL) { s.arrT = elapsed; }
            var nav = e.isNavigating();
            if (!s.endNav && s.wasNav && !nav) {
                s.endNav = true;
                s.ex = x; s.ey = y; s.ez = z;
            }
            if (s.endNav) {
                var dd = d3(x, y, z, s.ex, s.ey, s.ez);
                if (dd > s.drift) { s.drift = dd; }
            }
            s.wasNav = nav;
        }
    }
}

function sampleLane(npc, lane, elapsed) {
    var n = lane.members.length;
    if (n === 0) { return; }
    var navig = 0, cele = 0, prispeli = 0, zastoj = 0;
    var goalMin = 1e9, goalSum = 0, travelMax = 0, yMax = -1e9;
    var ageMin = 1e9, ageMax = -1e9, gibSum = 0, gibMax = 0;
    for (var j = 0; j < n; j++) {
        var e = lane.members[j];
        var s = st[e.getUUID()];
        var da = e.getAge() - s.sampleAge;
        if (da < ageMin) { ageMin = da; }
        if (da > ageMax) { ageMax = da; }
        s.sampleAge = e.getAge();
        var gl = motionLen(e);
        gibSum += gl;
        if (gl > gibMax) { gibMax = gl; }
        if (e.isNavigating()) { navig++; }
        if (celaPot(e, lane)) { cele++; }
        if (s.arrT >= 0) { prispeli++; }
        var x = e.getX();
        var y = e.getY();
        var z = e.getZ();
        if (d3(x, y, z, s.samplePx, s.samplePy, s.samplePz) < ZASTOJ_PRAG) { zastoj++; }
        s.samplePx = x; s.samplePy = y; s.samplePz = z;
        var tr = d3(x, y, z, s.hx, s.hy, s.hz);
        if (tr > travelMax) { travelMax = tr; }
        if (y > yMax) { yMax = y; }
        var g = toGoal(e, lane);
        goalSum += g;
        if (g < goalMin) { goalMin = g; }
    }
    say(npc, 'LET-S faza=' + phase.name + ' tick=' + elapsed + ' proga=' + lane.id
        + ' navig=' + navig + '/' + n
        + ' cele=' + cele + '/' + n
        + ' prispeli=' + prispeli + '/' + n
        + ' doCiljaMin=' + goalMin.toFixed(2)
        + ' doCiljaPovp=' + (goalSum / n).toFixed(2)
        + ' prevozenoMax=' + travelMax.toFixed(2)
        + ' yMax=' + yMax.toFixed(2)
        + ' zastoj=' + zastoj + '/' + n
        + ' dStarost=' + ageMin + '/' + ageMax
        + ' gib=' + (gibSum / n).toFixed(4) + '/' + gibMax.toFixed(4));
}

function sampleAll(npc, elapsed) {
    for (var i = 0; i < LANES.length; i++) { sampleLane(npc, LANES[i], elapsed); }
}

// Povzetek faze na progo. Cas do cilja je cenzuriran: kdor ne pride, steje s trajanjem
// faze, zato mediana obstaja vedno in je manjsa od trajanja samo, ce je prispela vecina.
function summarize(npc, lane) {
    var n = lane.members.length;
    var limit = PHASE_LEN * TICKS_PER_SCRIPT_TICK;
    var times = [];
    var drifts = [];
    var speeds = [];
    var prispeli = 0;
    var endSum = 0;
    var endMin = 1e9;
    for (var j = 0; j < n; j++) {
        var e = lane.members[j];
        var s = st[e.getUUID()];
        if (s.arrT >= 0) { prispeli++; times.push(s.arrT); } else { times.push(limit); }
        if (s.endNav) { drifts.push(s.drift); }
        if (s.navTicks >= 20) { speeds.push(s.navDist / s.navTicks); }
        var g = toGoal(e, lane);
        endSum += g;
        if (g < endMin) { endMin = g; }
    }
    var dMax = 0;
    for (var k = 0; k < drifts.length; k++) { if (drifts[k] > dMax) { dMax = drifts[k]; } }
    var vSum = 0;
    for (var m = 0; m < speeds.length; m++) { vSum += speeds[m]; }
    var tMax = 0;
    for (var q = 0; q < times.length; q++) { if (times[q] > tMax) { tMax = times[q]; } }
    say(npc, 'LET-K faza=' + phase.name + ' proga=' + lane.id + ' N=' + n
        + ' prispeli=' + prispeli
        + ' casMed=' + median(times).toFixed(0)
        + ' casMax=' + tMax
        + ' konecNav=' + drifts.length
        + ' drsenjeMax=' + dMax.toFixed(3)
        + ' drsenjeMed=' + median(drifts).toFixed(3)
        + ' hitrostN=' + speeds.length
        + ' hitrost=' + (speeds.length > 0 ? vSum / speeds.length : 0).toFixed(4)
        + ' doCiljaKonec=' + (n > 0 ? endSum / n : -1).toFixed(2)
        + ' doCiljaKonecMin=' + endMin.toFixed(2));
}

function endPhase(npc, elapsed) {
    update(elapsed);
    sampleAll(npc, elapsed);
    for (var i = 0; i < LANES.length; i++) { summarize(npc, LANES[i]); }
    say(npc, 'LET-' + phase.name + '-END');
    stopAll();
    phase = null;
}

function init(e) {
    t = 0;
    say(e.npc, 'LET-INIT skriptni tick = ' + TICKS_PER_SCRIPT_TICK + ' server tickov');
}

function tick(e) {
    t++;
    var npc = e.npc;

    if (t === SETUP_AT) {
        collect(npc);
        rememberHome();
        var line = 'LET-SETUP';
        for (var i = 0; i < LANES.length; i++) {
            var lane = LANES[i];
            line += ' ' + lane.id + '=' + lane.members.length + '/' + (lane.target !== null ? 'da' : 'NE')
                + '/' + avgToGoal(lane).toFixed(2);
        }
        say(npc, line);
        return;
    }

    for (var p = 0; p < PHASES.length; p++) {
        var ph = PHASES[p];
        if (t === ph.start) { startPhase(npc, ph); return; }
        if (t === ph.start + PHASE_LEN && phase === ph) { endPhase(npc, PHASE_LEN * TICKS_PER_SCRIPT_TICK); return; }
    }

    if (t === SUM_AT) {
        stopAll();
        var sum = 'LET-SUM';
        for (var k = 0; k < LANES.length; k++) { sum += ' ' + LANES[k].id + '=' + LANES[k].members.length; }
        say(npc, sum);
        return;
    }

    if (phase === null) { return; }
    var rel = t - phase.start;
    var elapsed = rel * TICKS_PER_SCRIPT_TICK;
    update(elapsed);
    if (rel % SAMPLE_EVERY === 0) { sampleAll(npc, elapsed); }
    if (phase.refresh && rel % REFRESH_EVERY === 0) {
        for (var r = 0; r < LANES.length; r++) { navigateLane(LANES[r], phase.speed); }
    }
}
