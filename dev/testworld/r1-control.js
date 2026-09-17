// M2.2 - krmilna skripta reprodukcije R1 (NPC jaha NPC).
//
// Tece na fixture NPC-ju R1_Control. Vir resnice je ta datoteka; v R1_Control.json je
// vstavljena kot ena vrstica. Ce se spremeni tu, jo je treba znova vstaviti (glej
// docs/scenariji/M2.2-R1.md).
//
// POZOR - enota casa. EntityNPCInterface klice EventHooks.onNPCTick vsak 10. server tick
// (EntityNPCInterface.java:358), zato je tu 1 "tick" = 10 server tickov = 0,5 s. Vse
// konstante spodaj so v SKRIPTNIH tickih; ob izpisu se pretvorijo v server ticke.
//
// Nashorn na Javi 8: brez let, brez puscicnih funkcij, brez sablonskih nizov.
//
// POZOR - domet iskanja poti. Vanilla A* ima dve trdi meji: proracun 200 vozlisc
// (PathFinder.java:65) in dolzino poti, omejeno z getPathSearchRange() = FOLLOW_RANGE =
// CustomNpcs.NpcNavRange (privzeto 32, EntityNPCInterface.java:334). Nasa proga je dolga
// natanko 32 blokov, z obvozom do vrat pa vec. En sam navigateTo zato vrne DELNO pot in
// NPC obstane na njenem koncu - kar je 17. 9. izgledalo kot ovira pri z = 36 (Q11).
// Zato se v fazi A navigateTo ponovi ob vsakem vzorcu; vsak klic tedaj isce samo se
// preostanek poti. Vanilla AI taski delajo enako (EntityAIAttackTarget se prepathga sam,
// zato je proga S v fazi B prisla do cilja, v fazi A pa ne).

var TICKS_PER_SCRIPT_TICK = 10;

var MOUNT_AT = 4;      // 40 server tickov po init
var A_START = 6;
var A_END = 46;        // 400 server tickov = 20 s vozne faze
var B_START = 50;
var B_END = 90;
var SAMPLE_EVERY = 2;  // vsakih 20 server tickov -> 20 vzorcev na fazo

var LANE_SPLIT_X = 10; // x < 10 -> proga M (mounted), sicer proga S (solo)
var START_Z = 12;
var GOAL_Z = 44;
var GOAL_Y = 4;
var GOAL_X_M = 0;
var GOAL_X_S = 24;
var SPEED = 1.0;

var t = 0;
var phase = "-";
var carriersM = [];
var carriersS = [];
var riders = [];
var targetM = null;
var targetS = null;
var startPos = {};   // uuid -> {x, z}
var mountedAtStart = 0;

function say(npc, msg) {
    npc.executeCommand("/say " + msg);
}

function byX(a, b) {
    return a.getX() - b.getX();
}

function dist2d(a, b) {
    var dx = a.getX() - b.getX();
    var dz = a.getZ() - b.getZ();
    return Math.sqrt(dx * dx + dz * dz);
}

function distTo(e, x, z) {
    var dx = e.getX() - x;
    var dz = e.getZ() - z;
    return Math.sqrt(dx * dx + dz * dz);
}

function collect(npc) {
    var all = npc.getWorld().getAllEntities(2);
    carriersM = [];
    carriersS = [];
    riders = [];
    targetM = null;
    targetS = null;
    for (var i = 0; i < all.length; i++) {
        var e = all[i];
        if (e.hasTag("r1carrier")) {
            if (e.getX() < LANE_SPLIT_X) { carriersM.push(e); } else { carriersS.push(e); }
        } else if (e.hasTag("r1rider")) {
            riders.push(e);
        } else if (e.hasTag("r1target")) {
            if (e.getX() < LANE_SPLIT_X) { targetM = e; } else { targetS = e; }
        }
    }
    carriersM.sort(byX);
    carriersS.sort(byX);
    riders.sort(byX);
}

function rememberStart(list) {
    for (var i = 0; i < list.length; i++) {
        startPos[list[i].getUUID()] = { x: list[i].getX(), z: list[i].getZ() };
    }
}

function mountAll(npc) {
    var n = Math.min(carriersM.length, riders.length);
    var ok = 0;
    for (var i = 0; i < n; i++) {
        riders[i].setMount(carriersM[i]);
    }
    for (var j = 0; j < n; j++) {
        if (riders[j].getMount() !== null) { ok++; }
    }
    mountedAtStart = ok;
    say(npc, "R1-MOUNT n=" + ok + " od=" + n
        + " progaM=" + carriersM.length + " progaS=" + carriersS.length
        + " jahacev=" + riders.length
        + " ciljM=" + (targetM !== null ? "da" : "NE")
        + " ciljS=" + (targetS !== null ? "da" : "NE"));
}

function driveNavigate(list, goalX) {
    for (var i = 0; i < list.length; i++) {
        list[i].navigateTo(goalX, GOAL_Y, GOAL_Z, SPEED);
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

function resetToStart(list, riderList) {
    for (var i = 0; i < list.length; i++) {
        var c = list[i];
        c.setPosition(c.getX(), GOAL_Y, START_Z);
    }
    if (riderList) {
        for (var j = 0; j < riderList.length; j++) {
            var r = riderList[j];
            if (r.getMount() === null) {
                r.setPosition(r.getX(), GOAL_Y, START_Z);
            }
        }
    }
}

// Najvecja razdalja med dvema nosilcema. To je stevilka za "grupirajo se v radiusu
// enega bloka" - brez nje je ta simptom samo obcutek.
function spread(list) {
    var max = 0;
    for (var i = 0; i < list.length; i++) {
        for (var j = i + 1; j < list.length; j++) {
            var d = dist2d(list[i], list[j]);
            if (d > max) { max = d; }
        }
    }
    return max;
}

function sample(npc, lane, list, goalX, riderList) {
    if (list.length === 0) { return; }
    var navig = 0;
    var travelSum = 0;
    var travelMax = 0;
    var goalMin = 1e9;
    var goalSum = 0;
    for (var i = 0; i < list.length; i++) {
        var c = list[i];
        if (c.isNavigating()) { navig++; }
        var s = startPos[c.getUUID()];
        var tr = s ? Math.sqrt((c.getX() - s.x) * (c.getX() - s.x) + (c.getZ() - s.z) * (c.getZ() - s.z)) : 0;
        travelSum += tr;
        if (tr > travelMax) { travelMax = tr; }
        var g = distTo(c, goalX, GOAL_Z);
        goalSum += g;
        if (g < goalMin) { goalMin = g; }
    }
    var mounted = 0;
    var offsetMax = 0;
    if (riderList) {
        for (var j = 0; j < riderList.length; j++) {
            var r = riderList[j];
            var m = r.getMount();
            if (m !== null) {
                mounted++;
                var off = dist2d(r, m);
                if (off > offsetMax) { offsetMax = off; }
            }
        }
    }
    say(npc, "R1-S faza=" + phase
        + " tick=" + (t * TICKS_PER_SCRIPT_TICK)
        + " proga=" + lane
        + " navig=" + navig + "/" + list.length
        + " prevozenoPovp=" + (travelSum / list.length).toFixed(2)
        + " prevozenoMax=" + travelMax.toFixed(2)
        + " razpon=" + spread(list).toFixed(2)
        + " doCiljaMin=" + goalMin.toFixed(2)
        + " doCiljaPovp=" + (goalSum / list.length).toFixed(2)
        + " jahacev=" + (riderList ? mounted : -1)
        + " odstopMax=" + (riderList ? offsetMax.toFixed(2) : "-"));
}

function init(e) {
    t = 0;
    say(e.npc, "R1-INIT skriptni tick = " + TICKS_PER_SCRIPT_TICK + " server tickov");
}

function tick(e) {
    t++;
    var npc = e.npc;

    if (t === MOUNT_AT) {
        collect(npc);
        rememberStart(carriersM);
        rememberStart(carriersS);
        mountAll(npc);
        return;
    }

    if (t === A_START) {
        phase = "A";
        say(npc, "R1-A-START navigateTo skupni cilj");
        driveNavigate(carriersM, GOAL_X_M);
        driveNavigate(carriersS, GOAL_X_S);
        return;
    }

    if (t === A_END) {
        sample(npc, "M", carriersM, GOAL_X_M, riders);
        sample(npc, "S", carriersS, GOAL_X_S, null);
        say(npc, "R1-A-END");
        stopAll(carriersM);
        stopAll(carriersS);
        resetToStart(carriersM, riders);
        resetToStart(carriersS, null);
        phase = "-";
        return;
    }

    if (t === B_START) {
        phase = "B";
        rememberStart(carriersM);
        rememberStart(carriersS);
        say(npc, "R1-B-START setAttackTarget");
        driveAttack(carriersM, targetM);
        driveAttack(carriersS, targetS);
        return;
    }

    if (t === B_END) {
        sample(npc, "M", carriersM, GOAL_X_M, riders);
        sample(npc, "S", carriersS, GOAL_X_S, null);
        say(npc, "R1-B-END");
        stopAll(carriersM);
        stopAll(carriersS);
        phase = "-";
        say(npc, "R1-SUM mount=" + mountedAtStart + " progaM=" + carriersM.length
            + " progaS=" + carriersS.length);
        return;
    }

    if (phase === "-") { return; }
    if (t % SAMPLE_EVERY !== 0) { return; }
    sample(npc, "M", carriersM, GOAL_X_M, riders);
    sample(npc, "S", carriersS, GOAL_X_S, null);
    // Vzorec se vzame PRED osvezitvijo poti, da navig= pove stanje ob koncu intervala,
    // ne stanja takoj po svezem klicu.
    if (phase === "A") {
        driveNavigate(carriersM, GOAL_X_M);
        driveNavigate(carriersS, GOAL_X_S);
    }
}
