// M10.4 controller fixture only: packaged candidate, 200 workers + one observer.
// No production changes. All world operations run on the server thread.
var OUT = @@OUTPUT@@, REQUEST = @@REQUEST@@;
var s = null, dead = false;
function log(x) { Java.type('java.lang.System').out.println('M104 ' + x); }
function must(ok, detail) { if (!ok) { throw new Error(detail); } }
function read(path) {
    return JSON.parse(String(new java.lang.String(Java.type('java.nio.file.Files').readAllBytes(Java.type('java.nio.file.Paths').get(path)), 'UTF-8')));
}
function write(path, obj) {
    var f = new java.io.File(path), tmp = new java.io.File(path + '.tmp');
    var w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(tmp), 'UTF-8');
    try { w.write(JSON.stringify(obj)); } finally { w.close(); }
    Java.type('java.nio.file.Files').move(tmp.toPath(), f.toPath(), Java.type('java.nio.file.StandardCopyOption').REPLACE_EXISTING);
}
function field(obj, name) {
    var c = obj.getClass();
    while (c !== null) {
        try { var f = c.getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
        catch (e) { if (!(e instanceof Java.type('java.lang.NoSuchFieldException'))) { throw e; } }
        c = c.getSuperclass();
    }
    throw new Error('Missing field ' + name);
}
function attach(n, code) {
    var h = n.getMCEntity().script; h.setLanguage('ECMAScript'); h.setEnabled(true);
    var tab = new (Java.type('noppes.npcs.controllers.ScriptContainer'))(h); tab.script = code; h.getScripts().add(tab);
}
function spawn(w, name, x, z) {
    var n = Java.type('noppes.npcs.api.NpcAPI').Instance().spawnNPC(w.getMCWorld(), x, 4, z);
    n.setName(name); n.setHome(x, 4, z); n.getAi().setMovingType(0);
    n.getAi().setInteractWithNPCs(false); n.getAi().setStopOnInteract(false);
    n.getStats().setMaxHealth(1000); n.setHealth(1000);
    n.getStats().setHealthRegen(0); n.getStats().setCombatRegen(0);
    return n;
}
function setup(npc) {
    s = { workers: [], patrol: [], combat: [], scripted: [], steps: 0, seq: -1, started: 0, hourStart: 0 };
    var w = npc.getWorld();
    for (var i = 0; i < 80; i++) {
        var x = -56 + (i % 10) * 10, z = -56 + Math.floor(i / 10) * 6;
        var p = spawn(w, 'SOAK_P' + i, x, z);
        var path = new java.util.ArrayList();
        path.add(Java.to([x, 4, z], 'int[]')); path.add(Java.to([x + 6, 4, z], 'int[]'));
        p.getMCEntity().ais.setMovingPath(path); p.getAi().setMovingPathType(1, false); p.getAi().setMovingType(2);
        s.patrol.push({ n: p, x: p.getX(), z: p.getZ(), distance: 0, previous: 0 }); s.workers.push(p);
    }
    // 40 independent melee pairs, zero weapon knockback. Hits are real AI damage.
    // P1 (Q250): full knockback resistance, otherwise vanilla hit knockback walks each pair
    // ~1,000-2,000 blocks away over hours (M10.4 attempt 2). No teleport, no AI/entity reset.
    for (var j = 0; j < 40; j++) {
        var bx = -56 + (j % 10) * 10, bz = 8 + Math.floor(j / 10) * 8;
        var a = spawn(w, 'SOAK_A' + j, bx, bz), b = spawn(w, 'SOAK_B' + j, bx + 2, bz);
        a.setFaction(1); b.setFaction(2);
        for each (var c in [a, b]) { c.getStats().getMelee().setStrength(1); c.getStats().getMelee().setKnockback(0); c.getStats().getMelee().setDelay(20); c.getStats().setResistance(3, 2); }
        a.setAttackTarget(b); b.setAttackTarget(a);
        s.combat.push({ n: a, target: b, hp: a.getHealth(), damage: 0, previous: 0, hx: bx, hz: bz }, { n: b, target: a, hp: b.getHealth(), damage: 0, previous: 0, hx: bx + 2, hz: bz });
        s.workers.push(a, b);
    }
    for (var k = 0; k < 40; k++) {
        var n = spawn(w, 'SOAK_S' + k, -56 + (k % 10) * 10, 48 + Math.floor(k / 10) * 4);
        attach(n, "function tick(e){if(!e.npc.getTimers().has(104)){e.npc.getTimers().start(104,20,true);}}"
            + "function timer(e){if(e.id===104){var d=e.npc.getTempdata();d.put('soak',Number(d.has('soak')?d.get('soak'):0)+1);e.npc.getStoreddata().put('soak',d.get('soak'));}}");
        s.scripted.push({ n: n, previous: 0 }); s.workers.push(n);
    }
    s.player = new (Java.type('noppes.npcs.controllers.data.PlayerData'))();
    s.player.playername = 'M104_Offline'; s.player.uuid = '2978a942-7037-49f8-954e-32402847b104';
    // Submit real asynchronous CNPC writes throughout the run, even with no connected player.
    s.player.save(false);
    npc.getTimers().forceStart(1040, 20, true);
    log('READY workers=200 patrol=80 combat=80 scripted=40 observer=1');
}
function gcCount() {
    var sum = 0, found = false, beans = Java.type('java.lang.management.ManagementFactory').getGarbageCollectorMXBeans();
    for (var i = 0; i < beans.size(); i++) {
        var b = beans.get(i), name = String(b.getName());
        if (/Old|MarkSweep/.test(name)) { sum += Number(b.getCollectionCount()); found = true; }
    }
    must(found, 'No full-GC MXBean'); return sum;
}
function snapshot(npc, seq) {
    var now = Number(java.lang.System.nanoTime()), D = Java.type('noppes.npcs.rework.diag.Diag');
    var session = Java.type('noppes.npcs.rework.data.WorldSaveSession').current();
    must(session !== null, 'No active save session');
    var executor = field(session, 'executor'), pool = field(executor, 'e');
    must(pool instanceof Java.type('java.util.concurrent.ThreadPoolExecutor'), 'Unexpected save executor');
    var queue = Number(pool.getQueue().size()), active = Number(pool.getActiveCount());
    must(field(session, 'firstFailure').get() === null, 'Asynchronous write failed');
    // Never drain/clear/shutdown the executor to manufacture an empty queue.
    // Retry on the next real timer tick, with an outer 30 s timeout. Never report a busy sample as empty.
    if (queue !== 0 || active !== 0) { return false; }
    var completed = Number(pool.getCompletedTaskCount());
    must(completed >= Math.floor(s.steps / 20) + 1, 'Missing completed async writes');
    var file = new java.io.File(session.getWorldRoot(), 'playerdata/' + s.player.uuid + '.json');
    var persisted = Java.type('noppes.npcs.api.NpcAPI').Instance().getINbt(Java.type('noppes.npcs.util.NBTJsonUtil').LoadFile(file));
    var quests = persisted.getCompound('QuestData').getList('CompletedQuests', 10);
    must(quests.length === 1 && quests[0].getInteger('Quest') === 104
        && Number(quests[0].getLong('Date')) === Math.floor(s.steps / 20) * 20, 'Latest async write missing on disk');
    var moved = 0, damaged = 0, timers = 0, minDistance = 1e99, minTicks = 1e99, maxDrift = 0;
    for each (var p in s.patrol) {
        var dist = p.distance - p.previous; minDistance = Math.min(minDistance, dist); if (dist > 1) { moved++; } p.previous = p.distance;
    }
    for each (var c in s.combat) {
        if (c.damage > c.previous) { damaged++; } c.previous = c.damage;
        var fx = c.n.getX() - c.hx, fz = c.n.getZ() - c.hz; maxDrift = Math.max(maxDrift, Math.sqrt(fx * fx + fz * fz));
    }
    for each (var t in s.scripted) {
        var n = Number(t.n.getTempdata().get('soak')), delta = n - t.previous;
        minTicks = Math.min(minTicks, delta); if (delta > 0) { timers++; } t.previous = n;
    }
    var all = npc.getWorld().getAllEntities(2), seen = {}, alive = 0;
    for each (var entity in all) { if (String(entity.getName()).indexOf('SOAK_') === 0) { seen[String(entity.getUUID())] = true; } }
    for each (var worker in s.workers) { if (worker.isAlive() && seen[String(worker.getUUID())]) { alive++; } }
    must(alive === 200 && Object.keys(seen).length === 200, 'Worker set changed');
    must(moved === 80 && damaged === 80 && timers === 40, 'Inactive workload ' + moved + '/' + damaged + '/' + timers);
    var before = gcCount(); java.lang.System.gc(); var after = gcCount();
    must(after > before, 'Full GC did not complete');
    var M = Java.type('java.lang.management.ManagementFactory');
    var heap = Number(M.getMemoryMXBean().getHeapMemoryUsage().getUsed());
    var threads = Number(M.getThreadMXBean().getThreadCount());
    var diag = D.snapshot(), hist = diag.distribution('server.tick.ns');
    var result = { success: true, seq: seq, elapsedSeconds: s.started === 0 ? 0 : (now - s.started) / 1e9,
        hourSeconds: s.hourStart === 0 ? 0 : (now - s.hourStart) / 1e9,
        alive: alive, moved: moved, damaged: damaged, timers: timers, minDistance: minDistance, minTimerDelta: minTicks,
        heap: heap, threads: threads, gcBefore: before, gcAfter: after, queue: queue, active: active,
        writes: Math.floor(s.steps / 20), writesCompleted: completed, persistedStep: Number(quests[0].getLong('Date')),
        ticks: Number(hist.count()), p95ns: Number(hist.percentile(0.95)),
        // P1 data only: combat drift from spawn and forced diag chunks (release mode, Q249).
        maxDrift: maxDrift, forcedChunks: Number(Java.type('noppes.npcs.rework.diag.DiagChunkLoader').forcedChunks()) };
    if (seq > 0 && seq % 12 === 0) {
        write(OUT + '-hour' + (seq / 12) + '.json', JSON.parse(String(diag.toJson())));
    }
    if (seq === 0 || seq % 12 === 0) { Java.type('noppes.npcs.rework.diag.DiagEventCollector').enable(); s.hourStart = Number(java.lang.System.nanoTime()); }
    if (seq === 0) { s.started = s.hourStart; }
    write(OUT + '-' + seq + '.json', result); log('SAMPLE ' + seq + ' success=true'); return true;
}
function tick(e) {
    if (dead || s !== null) { return; }
    try { setup(e.npc); } catch (err) { dead = true; log('FAILED ' + err); }
}
function timer(e) {
    if (dead || e.id !== 1040 || s === null) { return; }
    try {
        s.steps++;
        for each (var p in s.patrol) {
            var dx = p.n.getX() - p.x, dz = p.n.getZ() - p.z;
            p.distance += Math.sqrt(dx * dx + dz * dz); p.x = p.n.getX(); p.z = p.n.getZ();
        }
        // Reissue targets only if lost, preserving ordinary combat AI work.
        for each (var c in s.combat) {
            var hp = c.n.getHealth(); c.damage += Math.max(0, c.hp - hp);
            // Vanilla attributes may clamp maximum health. Restore health before death, without
            // resetting entities, AI, targets or the monotonically accumulated actual damage.
            must(c.n.isAlive(), 'Combat worker died');
            if (hp < 500) { c.n.setHealth(c.n.getMaxHealth()); }
            c.hp = c.n.getHealth();
            if (c.n.getAttackTarget() === null) { c.n.setAttackTarget(c.target); }
        }
        if (s.steps % 20 === 0) {
            s.player.questData.finishedQuests.put(java.lang.Integer.valueOf(104), java.lang.Long.valueOf(String(s.steps)));
            s.player.save(false);
        }
        if (new java.io.File(REQUEST).isFile()) {
            var r = read(REQUEST);
            if (r.seq === s.seq + 1) { if (snapshot(e.npc, r.seq)) { s.seq = r.seq; } }
            else { must(r.seq === s.seq, 'Out of order request'); }
        }
    } catch (err) { dead = true; log('FAILED ' + err); }
}
