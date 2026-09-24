// M3.6 - krmilna skripta: zakasnitev napada pri NPC-ju, ki tava.
//
// Tece na fixture NPC-ju M36_Control. Vir resnice je ta datoteka; v M36_Control.json je
// vstavljena z vstavi-skripto.py (glej docs/scenariji/M3.6-napad-med-tavanjem.md).
//
// Napaka, ki jo scenarij meri: EntityNPCInterface.setResponse doda EntityAIAttackTarget na
// isto prioriteto kot EntityAIWander (taskCount++ manjka). Vanilla EntityAITasks.canUse ne
// dovoli prekinitve taska z enako prioriteto, EntityAIWander pa se ob napadu ne umakne sam.
// Pricakovano v nacinu 0: NPC, ki ob ukazu tava, zacne napad sele na koncu poti tavanja.
// Z /rwattack 1 (AttackPriority) zacne takoj.
//
// Dve progi v istem svetu in istem ticku:
//   proga T  8 NPC-jev, ki tavajo (MovingState 1)  - merjeno stanje
//   proga S  8 istih NPC-jev, ki stojijo            - kontrola: cista reakcija napada
//
// Enota casa: onNPCTick je vsak 10. server tick, zato je 1 skriptni tick = 10 server tickov.
// Zakasnitev je v SERVER tickih z locljivostjo 10.
//
// Kdaj NPC napada: koncna tocka njegove poti je do 2,5 bloka od tarce ali je sam do 3 blokov
// od nje. Poti tavanja imajo koncno tocko drugje (okolica starta), pot napada pa pri tarci.
//
// Nashorn na Javi 8: brez let, brez puscicnih funkcij, brez sablonskih nizov.

var TICKS_PER_SCRIPT_TICK = 10;
var FIND_AT = 2;
var FIRST_ROUND = 6;
var ROUNDS = 3;
var ROUND_LEN = 30;    // skriptnih tickov na krog
var MEASURE = 20;      // skriptnih tickov merjenja po ukazu (200 server tickov)
var LANE_SPLIT_X = 75;
var START_Y = 4;
var HIT_PATH = 2.5;
var HIT_NEAR = 3.0;
// Doseg napada fixture NPC-jev (AggroRange 16 v M36_Wander/M36_Stand.json). EntityAIAttackTarget
// ima v shouldContinueExecuting preverbo isInRange(tarca, aggroRange) - Cebiseva razdalja, ne
// evklidska - v shouldExecute pa je nima. NPC, ki je ob ukazu dlje, zato napad vsakih nekaj tickov
// ustavi (resetTask pobrise pot) in znova zacne. To je obnasanje originala, ne napaka prioritete,
// zato taki vzorci (oznaka "o") ne stejejo v A4/A5; porocani so posebej (M3.6, 24. 9.).
var AGGRO = 16;

var t = 0;
var wanderers = [];
var standers = [];
var targetT = null;
var targetS = null;
var startPos = {};
var round = 0;
var roundStart = -1;
var state = {};        // uuid -> {tav: bool, lat: int}
var allT = [];         // {tav, lat} cez vse kroge
var allS = [];

function say(npc, msg) {
    npc.executeCommand("/say " + msg);
}

function byX(a, b) {
    return a.getX() - b.getX();
}

function d2(ax, az, bx, bz) {
    var dx = ax - bx;
    var dz = az - bz;
    return Math.sqrt(dx * dx + dz * dz);
}

function collect(npc) {
    var all = npc.getWorld().getAllEntities(2);
    for (var i = 0; i < all.length; i++) {
        var e = all[i];
        if (e.hasTag("m36wander")) {
            wanderers.push(e);
        } else if (e.hasTag("m36stand")) {
            standers.push(e);
        } else if (e.hasTag("m36target")) {
            if (e.getX() < LANE_SPLIT_X) { targetT = e; } else { targetS = e; }
        }
    }
    wanderers.sort(byX);
    standers.sort(byX);
    remember(wanderers);
    remember(standers);
}

function remember(list) {
    for (var i = 0; i < list.length; i++) {
        startPos[list[i].getUUID()] = { x: list[i].getX(), z: list[i].getZ() };
    }
}

function navigating(list) {
    var n = 0;
    for (var i = 0; i < list.length; i++) {
        if (list[i].isNavigating()) { n++; }
    }
    return n;
}

function cheb(e, target) {
    return Math.max(Math.abs(e.getX() - target.getX()), Math.abs(e.getZ() - target.getZ()));
}

function attacks(e, target) {
    if (d2(e.getX(), e.getZ(), target.getX(), target.getZ()) <= HIT_NEAR) { return true; }
    var p = e.getNavigationPath();
    if (p === null) { return false; }
    return d2(p.getX() + 0.5, p.getZ() + 0.5, target.getX(), target.getZ()) <= HIT_PATH;
}

function command(list, target) {
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        var tav = e.isNavigating() && !attacks(e, target);
        var d = cheb(e, target);
        state[e.getUUID()] = { tav: tav, lat: -1, d: Math.round(d), out: d > AGGRO };
        e.setAttackTarget(target);
    }
}

function observe(list, target, ticks) {
    for (var i = 0; i < list.length; i++) {
        var s = state[list[i].getUUID()];
        if (s.lat < 0 && attacks(list[i], target)) { s.lat = ticks; }
    }
}

function release(list) {
    for (var i = 0; i < list.length; i++) {
        var e = list[i];
        e.setAttackTarget(null);
        e.clearNavigation();
        var p = startPos[e.getUUID()];
        e.setPosition(p.x, START_Y, p.z);
    }
}

function roundLine(npc, lane, list, acc) {
    var parts = [];
    var dist = [];
    var tav = 0;
    for (var i = 0; i < list.length; i++) {
        var s = state[list[i].getUUID()];
        if (s.tav) { tav++; }
        parts.push((s.out ? "o" : "") + (s.tav ? "t" : "") + s.lat);
        dist.push(s.d);
        acc.push({ tav: s.tav, lat: s.lat, out: s.out });
    }
    say(npc, "M36-R krog=" + round + " proga=" + lane + " n=" + list.length
        + " tavajocih=" + tav + " lat=" + parts.join(",") + " d=" + dist.join(","));
}

function median(a) {
    if (a.length === 0) { return -1; }
    a.sort(function (x, y) { return x - y; });
    var m = Math.floor(a.length / 2);
    return a.length % 2 === 1 ? a[m] : (a[m - 1] + a[m]) / 2;
}

function summary(npc, lane, acc) {
    // Vse razen izven/izvenNikoli/latIzven* steje samo vzorce v dosegu napada (!out).
    var tavLat = [];
    var ostLat = [];
    var outLat = [];
    var tav = 0;
    var never = 0;
    var out = 0;
    var outNever = 0;
    for (var i = 0; i < acc.length; i++) {
        if (acc[i].out) {
            out++;
            if (acc[i].lat < 0) { outNever++; } else { outLat.push(acc[i].lat); }
            continue;
        }
        if (acc[i].tav) { tav++; }
        if (acc[i].lat < 0) { never++; continue; }
        if (acc[i].tav) { tavLat.push(acc[i].lat); } else { ostLat.push(acc[i].lat); }
    }
    var tavMax = tavLat.length > 0 ? Math.max.apply(null, tavLat) : -1;
    var ostMax = ostLat.length > 0 ? Math.max.apply(null, ostLat) : -1;
    var outMax = outLat.length > 0 ? Math.max.apply(null, outLat) : -1;
    say(npc, "M36-LAT proga=" + lane + " vzorcev=" + acc.length + " tavajocih=" + tav
        + " nikoli=" + never
        + " latTavMed=" + median(tavLat) + " latTavMax=" + tavMax
        + " latOstaliMed=" + median(ostLat) + " latOstaliMax=" + ostMax
        + " izven=" + out + " izvenNikoli=" + outNever
        + " latIzvenMed=" + median(outLat) + " latIzvenMax=" + outMax);
}

function init(e) {
    t = 0;
    say(e.npc, "M36-INIT skriptni tick = " + TICKS_PER_SCRIPT_TICK + " server tickov");
}

function tick(e) {
    t++;
    var npc = e.npc;

    if (t === FIND_AT) {
        collect(npc);
        say(npc, "M36-FIND tava=" + wanderers.length + " stoji=" + standers.length
            + " ciljT=" + (targetT !== null ? "da" : "NE")
            + " ciljS=" + (targetS !== null ? "da" : "NE"));
        return;
    }
    if (t < FIRST_ROUND || targetT === null || targetS === null) { return; }

    var k = t - FIRST_ROUND;
    var r = Math.floor(k / ROUND_LEN);
    var inRound = k % ROUND_LEN;
    if (r >= ROUNDS) {
        if (r === ROUNDS && inRound === 0) {
            summary(npc, "T", allT);
            summary(npc, "S", allS);
            say(npc, "M36-SUM krogov=" + ROUNDS + " tava=" + wanderers.length
                + " stoji=" + standers.length);
        }
        return;
    }

    if (inRound === 0) {
        round = r + 1;
        roundStart = t;
        say(npc, "M36-NAPAD krog=" + round + " navigT=" + navigating(wanderers) + "/"
            + wanderers.length + " navigS=" + navigating(standers) + "/" + standers.length);
        command(wanderers, targetT);
        command(standers, targetS);
        return;
    }
    if (inRound <= MEASURE) {
        var ticks = (t - roundStart) * TICKS_PER_SCRIPT_TICK;
        observe(wanderers, targetT, ticks);
        observe(standers, targetS, ticks);
        if (inRound === MEASURE) {
            roundLine(npc, "T", wanderers, allT);
            roundLine(npc, "S", standers, allS);
            release(wanderers);
            release(standers);
        }
    }
}
