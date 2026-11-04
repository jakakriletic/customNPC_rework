// M13.4 controller probe. Never loads the remote chunk; reads provider.getLoadedChunk only.
// The probe itself is in vanilla spawn chunks (setworldspawn 0 4 0); no forced chunks/tickets.
var L = Java.type('noppes.npcs.controllers.LinkedNpcController');
var Log = Java.type('noppes.npcs.LogWriter');
var BP = Java.type('net.minecraft.util.math.BlockPos');
var AL = Java.type('java.util.ArrayList');
var IA = Java.type('int[]');
var NPC = Java.type('noppes.npcs.entity.EntityNPCInterface');
var UUID = Java.type('java.util.UUID');
var age = 0, stage = 0, farRef = null, deniedTicks = 0, deniedDone = false, costStart = 0, nanosBefore = 0, appliedBefore = 0;
var STEVE = 'customnpcs:textures/entity/humanmale/steve.png';
var T2 = 'customnpcs:textures/entity/humanfemale/doctorstephanie.png';
var T1 = 'customnpcs:textures/entity/humanfemale/magestephanie.png';
// Liveness is independent of the group and recipient guards below. A silent
// return must not look like evidence that a remote chunk failed to unload.
Java.type('java.lang.System').out.println('CLONERPROBE evaluated');
function check(key, ok, detail) {
    Log.info('CLONERPROBE CHECK key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
    if (!ok) throw new Error(key + ': ' + detail);
}
function mark(text) { Log.info('CLONERPROBE ' + text); }
function at(world, x) {
    var all = world.loadedEntityList;
    for (var i = 0; i < all.size(); i++) {
        var e = all.get(i);
        if (e instanceof NPC && Math.abs(e.posX - (x + 0.5)) < 1 && e.posZ > 2 && e.posZ < 5) return e;
    }
    return null;
}
function fields(n) {
    var nbt = n.ais.writeToNBT(new (Java.type('net.minecraft.nbt.NBTTagCompound'))());
    return {uuid:String(n.getUniqueID()), x:n.posX, y:n.posY, z:n.posZ,
        own:String(nbt.getTag('StartPosNew')) + '/' + n.ais.orientation + '/' + n.ais.movingPos
            + '/' + String(nbt.getTag('MovingPathNew'))};
}
function snapshot(e, x) {
    var d = e.npc.getStoreddata(), w = e.npc.getWorld().getMCWorld(), n = at(w, x), key = 'snap' + x;
    if (n == null || d.has(key)) return;
    var points = new AL(), p = new IA(3);
    p[0] = x; p[1] = 4; p[2] = 8; points.add(p);
    n.ais.setMovingPath(points);
    // Nashorn passes x + 1 as Double; |0 keeps it an int so BlockPos(int,int,int) is unambiguous.
    n.ais.setStartPos(new BP((x + 1) | 0, 4, 4));
    n.ais.orientation = 90;
    n.ais.movingPos = 0;
    d.put(key, JSON.stringify(fields(n)));
    mark('snapshot x=' + x + ' ' + d.get(key));
}
function same(e, x) {
    var d = e.npc.getStoreddata(), n = at(e.npc.getWorld().getMCWorld(), x);
    if (n == null) return false;
    var old = JSON.parse(String(d.get('snap' + x))), now = fields(n);
    check('own-fields-' + x + '-stage' + stage, old.uuid === now.uuid && old.own === now.own
        && Math.abs(old.x-now.x) < 0.01 && Math.abs(old.y-now.y) < 0.01 && Math.abs(old.z-now.z) < 0.01,
        JSON.stringify(now));
    return true;
}
function body(e) {
    age += 10;
    var d = e.npc.getStoreddata(), w = e.npc.getWorld().getMCWorld(), provider = w.getChunkProvider();
    var link = L.Instance, g = link.getData('LCGuard');
    if (age === 10) mark('ready');
    if (age % 100 === 0) {
        mark('heartbeat age=' + age + ' stage=' + stage + ' group=' + (g != null)
            + ' loaded=' + w.loadedEntityList.size() + ' near=' + (at(w, 604) != null)
            + ' far=' + (at(w, 1002) != null) + ' farSnapshot=' + d.has('snap1002')
            + ' remoteLoaded=' + (provider.getLoadedChunk(62, 0) != null));
    }
    if (g == null) return;
    var far = at(w, 1002), near = at(w, 604), a = at(w, 600);
    if (d.has('awaitRestart')) {
        if (far != null && String(far.display.getSkinTexture()) === T1 && !d.has('restartDone')) {
            stage = 5;
            check('restart-categories', String(far.display.getName()) === 'LCGuard', far.display.getName());
            same(e, 1002);
            d.put('restartDone', 1);
            mark('restart-done');
        }
        var players = w.playerEntities;
        if (!deniedDone && players.size() > 0 && !players.get(0).canUseCommand(2, 'rwlinked')) {
            deniedTicks += 10;
            if (deniedTicks >= 120) {
                check('server-denied-spawn', at(w, 612) == null && link.getData('LCForbidden') == null, 'no entity or group created');
                deniedDone = true; mark('denied-done');
            }
        }
        return;
    }
    snapshot(e, 600); snapshot(e, 604); snapshot(e, 1002);
    if (far != null) farRef = far;
    var unloaded = provider.getLoadedChunk(62, 0) == null && far == null;
    if (stage === 0 && d.has('snap1002') && unloaded && a != null && near != null) {
        check('remote-chunk-unloaded', w.getPersistentChunks().isEmpty(), 'provider null and no forced chunks');
        stage = 1; mark('unloaded1');
        costStart = Java.type('java.lang.System').nanoTime();
        nanosBefore = link.budget.nanos; appliedBefore = link.budget.applied;
    } else if (stage === 1 && String(g.data.getString('Texture')) === T2 && near != null
            && String(near.display.getSkinTexture()) === T2) {
        check('edit-while-unloaded', unloaded && farRef != null && String(farRef.display.getSkinTexture()) === STEVE, 'unloaded recipient not ticked');
        check('loaded-category-name', String(near.display.getName()) === 'LCGuard', near.display.getName());
        same(e, 604); same(e, 600);
        mark('DATA one-edit loaded=1 applied=' + (link.budget.applied-appliedBefore)
            + ' transferMicros=' + ((link.budget.nanos-nanosBefore)/1000)
            + ' wallMillisIncludingGUI=' + ((Java.type('java.lang.System').nanoTime()-costStart)/1000000));
        stage = 2; mark('edited1');
    } else if (stage === 2 && far != null && String(far.display.getSkinTexture()) === T2) {
        check('reload-categories', String(far.display.getName()) === 'LCGuard', far.display.getName());
        same(e, 1002); stage = 3; mark('loaded1');
    } else if (stage === 3 && unloaded && a != null && near != null) {
        stage = 4; mark('unloaded2');
    } else if (stage === 4 && String(g.data.getString('Texture')) === T1 && near != null
            && String(near.display.getSkinTexture()) === T1) {
        check('edit-before-restart-unloaded', unloaded && String(farRef.display.getSkinTexture()) === T2, 'remote stays at previous revision until loaded');
        same(e, 604); same(e, 600);
        d.put('awaitRestart', 1);
        mark('restart-ready');
    }
}

// Any throw is printed on stdout with its Java/JS stack before the container stops the probe,
// so a silent stop cannot be mistaken for a probe still waiting on its gate.
function tick(e) {
    try {
        body(e);
    } catch (x) {
        var out = Java.type('java.lang.System').out;
        out.println('CLONERPROBE EXCEPTION age=' + age + ' stage=' + stage + ' ' + x + (x && x.stack ? ' stack=' + x.stack : ''));
        if (x instanceof Java.type('java.lang.Throwable')) x.printStackTrace(out);
        throw x;
    }
}
