// M4.14b diagnostic overlay, appended ONLY by formacije-run.ps1 -Trace.
// Reads private squad state; never writes a member, planner, path or movement input.
// Default: every tick through 80, then every 20. Recovery diagnostic: every tick through 560.
// Timer/logging overhead makes this a diagnostic run, not acceptance/performance proof.
var formTraceTimer = 414;
var formTraceStartAge = 0;
var formTraceEntries = [];
var formTraceFields = {};
var formTraceLastTick = -1;
// Only the separately registered recovery diagnostic enables dense sampling.
var formTraceRecovery = typeof formTraceRecovery !== 'undefined' && formTraceRecovery;
var formTracePathIds = null;
var formTraceNextPathId = 0;

function formTracePathId(path) {
    if (!path) { return 0; }
    if (!formTracePathIds) { formTracePathIds = new (Java.type('java.util.IdentityHashMap'))(); }
    if (!formTracePathIds.containsKey(path)) { formTracePathIds.put(path, ++formTraceNextPathId); }
    return Number(formTracePathIds.get(path));
}

function formTraceRecoveryState(squad, i, path) {
    if (!formTraceRecovery) { return ''; }
    var route = formTraceRead(squad, 'recovery')[i];
    var end = route ? route.getFinalPathPoint() : null;
    return ' navId=' + formTracePathId(path) + ' recoveryId=' + formTracePathId(route)
        + ' recoveryIndex=' + (route ? route.getCurrentPathIndex() : -1)
        + ' recoveryLength=' + (route ? route.getCurrentPathLength() : 0)
        + ' recoveryFinished=' + (!route || route.isFinished())
        + ' recoveryEnd=' + (end ? formTraceXYZ(end.x, end.y, end.z) : 'none')
        + ' lastPathTick=' + formTraceRead(squad, 'lastPathTick')[i]
        + ' lastPathTarget=' + formTraceNumber(formTraceRead(squad, 'lastPathX')[i])
        + ',' + formTraceNumber(formTraceRead(squad, 'lastPathZ')[i])
        + ' trailUntil=' + formTraceRead(formTraceRead(squad, 'planner'), 'trailUntil')[i]
        + ' byInterval=' + formTraceRead(squad, 'byInterval')
        + ' byMove=' + formTraceRead(squad, 'byMove') + ' byLost=' + formTraceRead(squad, 'byLost');
}

function formTraceField(type, name, object) {
    var key = String(type.getName()) + ':' + name;
    var field = formTraceFields[key];
    if (!field) {
        field = type.getDeclaredField(name);
        field.setAccessible(true);
        formTraceFields[key] = field;
    }
    return field.get(object);
}

function formTraceRead(object, name) {
    return formTraceField(object.getClass(), name, object);
}

function formTraceNumber(value) { return Number(value).toFixed(6); }
function formTraceXYZ(x, y, z) {
    return formTraceNumber(x) + ',' + formTraceNumber(y) + ',' + formTraceNumber(z);
}
function formTraceLog(text) { Java.type('noppes.npcs.LogWriter').info(text); }

function formTraceCapture(npc) {
    var type = Java.type('noppes.npcs.rework.formation.SquadManager').class;
    var manager = formTraceField(type, 'registered', null);
    if (!manager) { throw new Error('FORM-TRACE missing squad manager'); }
    var squads = formTraceRead(manager, 'squads').values().iterator();
    var memberCount = 0;
    while (squads.hasNext()) {
        var squad = squads.next();
        var label = String(formTraceRead(squad, 'label'));
        if (label !== 'NAV_WalkG' && label !== 'NAV_WalkO') { continue; }
        var members = formTraceRead(squad, 'members');
        var snapshots = [];
        for (var i = 0; i < members.length; i++) { snapshots.push(members[i]); }
        memberCount += members.length;
        formTraceEntries.push({ lane: label.slice(-1), squad: squad,
            planner: formTraceRead(squad, 'planner'), members: snapshots });
    }
    if (formTraceEntries.length !== 2 || memberCount !== 16) {
        throw new Error('FORM-TRACE expected two squads / sixteen members');
    }
    formTraceStartAge = npc.getAge();
    formTraceLog('FORM-TRACE start squads=2 members=16 through=560 '
        + (formTraceRecovery ? 'denseThrough=560 stride=1 recovery=true' : 'denseThrough=80 stride=20'));
    // B3: DataTimers repeats after interval + 1 ticks. Zero therefore samples
    // each server tick without changing the timer implementation or NAV cadence.
    npc.getTimers().start(formTraceTimer, 0, true);
}

function formTraceSample(npc, elapsed) {
    if (elapsed <= 0 || elapsed > 560 || elapsed === formTraceLastTick) { return; }
    if (!formTraceRecovery && elapsed > 80 && elapsed % 20 !== 0) { return; }
    formTraceLastTick = elapsed;
    for (var s = 0; s < formTraceEntries.length; s++) {
        var entry = formTraceEntries[s];
        var p = entry.planner;
        var lag = formTraceRead(p, 'lagTicks');
        var pathUntil = formTraceRead(p, 'pathUntil');
        for (var i = 0; i < entry.members.length; i++) {
            var member = entry.members[i];
            var nav = member.getNavigator();
            var helper = member.getMoveHelper();
            var path = nav.getPath();
            var end = path ? path.getFinalPathPoint() : null;
            var next = path && !path.isFinished()
                ? path.getPathPointFromIndex(path.getCurrentPathIndex()) : null;
            var slot = p.slot(i);
            formTraceLog('FORM-D proga=' + entry.lane + ' tick=' + elapsed + ' i=' + i
                + ' uuid=' + member.getUniqueID() + ' age=' + member.ticksExisted
                + ' plannerTick=' + p.tick() + ' faza=' + p.phase()
                + ' finished=' + formTraceRead(entry.squad, 'finished')
                + ' pos=' + formTraceXYZ(member.posX, member.posY, member.posZ)
                + ' motion=' + formTraceXYZ(member.motionX, member.motionY, member.motionZ)
                + ' input=' + formTraceNumber(member.moveForward) + ',' + formTraceNumber(member.moveStrafing)
                + ' aiSpeed=' + formTraceNumber(member.getAIMoveSpeed())
                + ' helper=' + helper.action + ' helperSpeed=' + formTraceNumber(helper.getSpeed())
                + ' helperTarget=' + formTraceXYZ(helper.getX(), helper.getY(), helper.getZ())
                + ' target=' + formTraceXYZ(p.targetX[i], p.targetY[i], p.targetZ[i])
                + ' final=' + formTraceXYZ(formTraceRead(p, 'finalX')[i], formTraceRead(p, 'finalY')[i], formTraceRead(p, 'finalZ')[i])
                + ' slot=' + formTraceNumber(slot.side) + ',' + formTraceNumber(slot.back)
                + ' mode=' + p.mode[i] + ' speed=' + formTraceNumber(p.speed[i])
                + ' error=' + formTraceNumber(p.error[i]) + ' settled=' + p.settled[i]
                + ' trailing=' + p.trailing[i] + ' excused=' + p.excused[i] + ' lagTicks=' + lag[i]
                + ' pathUntil=' + pathUntil[i] + ' noPath=' + nav.noPath()
                + ' pathIndex=' + (path ? path.getCurrentPathIndex() : -1)
                + ' pathLength=' + (path ? path.getCurrentPathLength() : 0)
                + ' pathNext=' + (next ? formTraceXYZ(next.x, next.y, next.z) : 'none')
                + ' pathEnd=' + (end ? formTraceXYZ(end.x, end.y, end.z) : 'none')
                + ' anchor=' + formTraceXYZ(p.anchorX(), p.anchorY(), p.anchorZ())
                + ' anchorSpeed=' + formTraceNumber(p.anchorSpeed()) + ' yaw=' + formTraceNumber(p.yaw())
                + ' remaining=' + formTraceNumber(p.remaining())
                + ' searches=' + formTraceRead(entry.squad, 'pathRequests') + ' stuck=' + p.stuckEvents()
                + formTraceRecoveryState(entry.squad, i, path));
        }
    }
}

var formTraceOriginalFormationAll = formationAll;
formationAll = function (npc) {
    formTraceOriginalFormationAll(npc);
    formTraceCapture(npc);
};

var formTraceOriginalEndPhase = endPhase;
endPhase = function (npc, marker) {
    if (phase === 'B') {
        formTraceSample(npc, npc.getAge() - formTraceStartAge);
        npc.getTimers().stop(formTraceTimer);
        formTraceLog('FORM-TRACE end tick=' + (npc.getAge() - formTraceStartAge));
    }
    formTraceOriginalEndPhase(npc, marker);
};

function timer(e) {
    if (e.id === formTraceTimer && phase === 'B') {
        formTraceSample(e.npc, e.npc.getAge() - formTraceStartAge);
    }
}
