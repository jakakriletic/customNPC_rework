// M7.V disposable fixture only (controller, packaged Forge server with customnpcentities 0.7.0).
// Uses only the CNPC script API and the entities mod's own public members (no SRG names), so
// the same text runs on an obfuscated server. @@PHASE@@ is "run" (every clip of every animated
// mob, sampled each tick) or "verify" (after a real restart: identity, hitbox, NBT, one replay).
var M7V_PHASE = @@PHASE@@;
var M7V_OUT = @@OUTPUT@@;
var M7V_TIMER = 7701;
var m7v = null;

function m7vLog(text) { Java.type('java.lang.System').out.println(text); }

function m7vCheck(key, ok, detail) {
    m7v.checks.push({ key: key, ok: !!ok, detail: String(detail) });
}

function m7vDiscover(npc) {
    var all = npc.getWorld().getAllEntities(2);
    var mobs = [];
    for (var i = 0; i < all.length; i++) {
        var mc = all[i].getMCEntity();
        var cls = String(mc.getClass().getName());
        if (cls.indexOf('dev.jakak.customnpcentities.') !== 0) { continue; }
        var animated = typeof mc.entityId === 'function';
        var row = { entity: all[i], mc: mc, cls: cls, uuid: String(all[i].getUUID()), animated: animated,
            id: animated ? String(mc.entityId()) : 'ice_golem',
            width: all[i].getWidth(), height: all[i].getHeight() };
        if (animated) {
            var def = mc.mob();
            row.mobWidth = def.width; row.mobHeight = def.height;
            var names = mc.animations().sortedClipNames();
            row.clipNames = [];
            for (var k = 0; k < names.size(); k++) { row.clipNames.push(String(names.get(k))); }
        }
        mobs.push(row);
    }
    mobs.sort(function (a, b) { return a.id < b.id ? -1 : (a.id > b.id ? 1 : 0); });
    return mobs;
}

function m7vHitboxOk(row) {
    // Rework's EntityNPCInterface would give 0.6/5*size; the entities override gives the mob definition.
    return Math.abs(row.entity.getWidth() - row.mobWidth) < 1e-4 && Math.abs(row.entity.getHeight() - row.mobHeight) < 1e-4;
}

function m7vStartLane(lane) {
    var clip = lane.queue[lane.next];
    var c = lane.mc.animations().clip(clip);
    lane.current = { name: clip, lengthTicks: c.lengthTicks(), played: !!lane.mc.playClip(clip), startTick: m7v.tick,
        samples: 0, playingSamples: 0, maxAge: -1, wrongClip: 0, hitboxBad: 0, ended: false, endTick: -1, ageMonotonic: true, lastAge: -1 };
}

function m7vSampleLane(lane) {
    var cur = lane.current;
    var action = lane.mc.getAction();
    cur.samples++;
    if (!m7vHitboxOk(lane.row)) { cur.hitboxBad++; }
    if (action !== 0) {
        cur.playingSamples++;
        if (String(lane.mc.clipForAction(action)) !== cur.name) { cur.wrongClip++; }
        var age = lane.mc.getActionAge();
        if (age < cur.lastAge) { cur.ageMonotonic = false; }
        cur.lastAge = age;
        if (age > cur.maxAge) { cur.maxAge = age; }
    } else {
        cur.ended = true; cur.endTick = m7v.tick;
    }
    var timedOut = m7v.tick - cur.startTick > cur.lengthTicks + 40;
    if (cur.ended || timedOut || !cur.played) {
        delete cur.lastAge;
        lane.results.push(cur);
        lane.current = null;
        lane.next++;
    }
}

function m7vFinish(npc) {
    npc.getTimers().stop(M7V_TIMER);
    var ok = true;
    for (var i = 0; i < m7v.checks.length; i++) { if (!m7v.checks[i].ok) { ok = false; } }
    m7v.proof.success = ok;
    m7v.proof.checks = m7v.checks;
    var Files = Java.type('java.nio.file.Files');
    var Paths = Java.type('java.nio.file.Paths');
    var text = new java.lang.String(JSON.stringify(m7v.proof, null, 1));
    Files.write(Paths.get(M7V_OUT), text.getBytes('UTF-8'));
    m7vLog('M7V ' + M7V_PHASE.toUpperCase() + ' DONE success=' + ok);
    if (M7V_PHASE === 'run') { npc.despawn(); }
}

function m7vRow(row) {
    var out = { id: row.id, cls: row.cls, uuid: row.uuid, animated: row.animated, width: row.width, height: row.height };
    if (row.animated) { out.mobWidth = row.mobWidth; out.mobHeight = row.mobHeight; out.clips = row.clipNames; }
    return out;
}

function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m7v')) { return; }
    d.put('m7v', 1);
    try {
        m7v = { tick: 0, checks: [], lanes: [], proof: { phase: M7V_PHASE, mobs: [] } };
        var mobs = m7vDiscover(e.npc);
        var ids = [];
        for (var i = 0; i < mobs.length; i++) { ids.push(mobs[i].id); m7v.proof.mobs.push(m7vRow(mobs[i])); }
        m7vCheck('five-mobs', ids.join(',') === 'cave_bear,giant_spider,ice_golem,troll,warg', ids.join(','));
        for (var j = 0; j < mobs.length; j++) {
            var row = mobs[j];
            if (!row.animated) {
                m7vCheck('golem-class', row.cls === 'dev.jakak.customnpcentities.entity.EntityIceGolem' && row.width > 0 && row.height > 0, row.cls + ' ' + row.width + 'x' + row.height);
                continue;
            }
            m7vCheck(row.id + '-hitbox-is-mob-def', m7vHitboxOk(row), row.width + 'x' + row.height + ' def ' + row.mobWidth + 'x' + row.mobHeight);
            m7vCheck(row.id + '-idle-at-start', row.mc.getAction() === 0, row.mc.getAction());
            if (M7V_PHASE === 'verify') {
                var nbt = row.entity.getEntityNbt();
                m7vCheck(row.id + '-nbt-anim-cooldowns', nbt.has('AnimCooldowns') && nbt.getType('AnimCooldowns') === 11, 'type=' + nbt.getType('AnimCooldowns'));
            }
            // run: every clip; verify: one replay of the first clip after the real restart.
            var queue = M7V_PHASE === 'run' ? row.clipNames : [row.clipNames[0]];
            m7v.lanes.push({ row: row, mc: row.mc, queue: queue, next: 0, current: null, results: [] });
        }
        // B3: DataTimers repeats after interval + 1 ticks, so 0 samples every server tick.
        e.npc.getTimers().start(M7V_TIMER, 0, true);
        m7vLog('M7V ' + M7V_PHASE.toUpperCase() + ' START mobs=' + mobs.length);
    } catch (error) {
        m7vLog('M7V FAILED ' + error);
    }
}

function timer(e) {
    if (e.id !== M7V_TIMER || m7v === null) { return; }
    try {
        m7v.tick++;
        var busy = false;
        for (var i = 0; i < m7v.lanes.length; i++) {
            var lane = m7v.lanes[i];
            if (lane.current !== null) { m7vSampleLane(lane); }
            if (lane.current === null && lane.next < lane.queue.length) { m7vStartLane(lane); }
            if (lane.current !== null || lane.next < lane.queue.length) { busy = true; }
        }
        if (busy && m7v.tick < 2400) { return; }
        m7vCheck('all-lanes-finished', !busy, m7v.tick);
        m7v.proof.lanes = [];
        for (var l = 0; l < m7v.lanes.length; l++) {
            var ln = m7v.lanes[l];
            var bad = [];
            for (var r = 0; r < ln.results.length; r++) {
                var c = ln.results[r];
                // Ends exactly when the server age reaches lengthTicks; sampled once per tick.
                var good = c.played && c.ended && c.playingSamples > 0 && c.wrongClip === 0 && c.hitboxBad === 0 && c.ageMonotonic
                    && c.maxAge >= c.lengthTicks - 2 && c.maxAge <= c.lengthTicks && c.endTick - c.startTick <= c.lengthTicks + 2;
                if (!good) { bad.push(c.name); }
            }
            m7vCheck(ln.row.id + '-clips', bad.length === 0 && ln.results.length === ln.queue.length, ln.results.length + '/' + ln.queue.length + ' bad=' + bad.join(','));
            m7vCheck(ln.row.id + '-hitbox-after', m7vHitboxOk(ln.row), ln.row.entity.getWidth() + 'x' + ln.row.entity.getHeight());
            m7v.proof.lanes.push({ id: ln.row.id, results: ln.results });
        }
        m7vFinish(e.npc);
    } catch (error) {
        m7vLog('M7V FAILED ' + error);
        try { e.npc.getTimers().stop(M7V_TIMER); } catch (ignored) { }
    }
}
