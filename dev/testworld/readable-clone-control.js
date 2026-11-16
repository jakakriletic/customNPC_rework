// M16.6 controller-only server proof. Harness prepends PHASE and GATE (unique to this run).
var C = Java.type('noppes.npcs.controllers.ServerCloneController');
var PD = Java.type('noppes.npcs.controllers.data.PlayerData');
var S = Java.type('noppes.npcs.CustomNpcs');
var RC = Java.type('noppes.npcs.rework.data.ReadableClone');
var RF = Java.type('noppes.npcs.rework.data.ReadableCloneFiles');
var NJ = Java.type('noppes.npcs.util.NBTJsonUtil');
var N = Java.type('net.minecraft.nbt.NBTTagCompound');
var F = Java.type('java.io.File');
var Files = Java.type('java.nio.file.Files');
var UTF8 = Java.type('java.nio.charset.StandardCharsets').UTF_8;
var JString = Java.type('java.lang.String');
var Arrays = Java.type('java.util.Arrays');
var API = Java.type('noppes.npcs.api.NpcAPI');
var Log = Java.type('noppes.npcs.LogWriter');
var done = false, count = 0, failed = 0;
function check(key, ok, detail) {
    count++; if (!ok) { failed++; }
    Log.info('READABLEPROBE CHECK key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function file(name) { return new F(new F(C.Instance.getDir(), '1'), name + '.json'); }
function text(f) { return new JString(Files.readAllBytes(f.toPath()), UTF8); }
function getAt(world, x) {
    var it = world.loadedEntityList.iterator();
    while (it.hasNext()) {
        var e = it.next();
        if (e instanceof Java.type('noppes.npcs.entity.EntityNPCInterface') && Math.abs(e.posX - (x + 0.5)) < 0.8 && Math.abs(e.posZ - 3.5) < 0.8) { return e; }
    }
    return null;
}
function rigScript(key, n) {
    var ok = n !== null && ('' + n.rwRig.model) === 'knight' && ('' + n.rwRig.clips).indexOf('rc_probe') >= 0 && n.script.enabled && n.script.scripts.size() === 1;
    var ran = n !== null && ('' + API.Instance().getIEntity(n).getStoreddata().get('readableProbe')) === ('ran:' + API.Instance().getIEntity(n).getUUID());
    check(key + '-rig-script', ok && ran, 'rig, clip, enabled tab and server execution=' + ran);
}
function snapshot(n) { var t = new N(); n.writeToNBTAtomically(t); C.Instance.cleanTags(t); return t; }
function tick(e) {
    if (done || !new F(GATE).isFile()) { return; }
    var contents = '' + text(new F(GATE));
    if (contents.indexOf('run') < 0) { return; }
    done = true;
    try {
        var mc = e.npc.getMCEntity(), server = mc.getServer(), world = mc.world;
        var source = getAt(world, 2), player = server.getPlayerList().getPlayerByUsername('M166_Player');
        var on = PHASE === 'on' || PHASE === 'verify';
        check('switch', S.RwReadableClones === (on ? 1 : 0), S.RwReadableClones);
        rigScript('source', source);
        if (PHASE !== 'verify') {
            var name = 'RC_' + PHASE + 'Server', actual = C.Instance.getCloneData(null, name, 1);
            var expected = PD.get(player).cloned;
            check('server-clonesave-roundtrip', expected !== null && actual !== null && RC.same(expected, actual), 'exact cleaned PlayerData.cloned from real CloneSave');
            check('server-format', on ? RF.hasEnvelope(text(file(name))) : ('' + text(file(name))) === ('' + NJ.Convert(expected)), on ? 'readable' : 'exact legacy bytes');
            var before = snapshot(source), commandName = 'RC_' + PHASE + 'Command';
            server.getCommandManager().executeCommand(server, 'noppes clone add ReadableSeed 1 ' + commandName);
            var after = C.Instance.getCloneData(null, commandName, 1);
            check('command-add-roundtrip', after !== null && RC.same(before, after), 'same server tick snapshot after cleanTags');
            var target = PHASE === 'off' ? 10 : PHASE === 'on' ? 12 : 14;
            var missing = getAt(world,target) === null;
            server.getCommandManager().executeCommand(server, 'noppes clone spawn ' + commandName + ' 1 ' + target + ',3,3 RC_CommandPlaced_' + PHASE);
            check('command-spawn-rig', missing && getAt(world,target) !== null && ('' + getAt(world,target).rwRig.model) === 'knight', 'actual command spawn into previously empty position');
        }
        if (PHASE === 'on') {
            var upgrade = file('RC_upgrade'), old = Files.readAllBytes(upgrade.toPath());
            C.Instance.getCloneData(null,'RC_upgrade',1);
            check('legacy-read-no-migration', Arrays.equals(old,Files.readAllBytes(upgrade.toPath())) && !new F(upgrade.getPath() + '.v0.bak').exists(), 'read left legacy bytes intact');
            var expectedUpgrade = snapshot(source);
            server.getCommandManager().executeCommand(server,'noppes clone add ReadableSeed 1 RC_upgrade');
            check('legacy-overwrite-backup', Arrays.equals(old,Files.readAllBytes(new F(upgrade.getPath() + '.v0.bak').toPath())), 'exact original bytes');
            check('legacy-upgrade-roundtrip', RC.same(expectedUpgrade,C.Instance.getCloneData(null,'RC_upgrade',1)), 'explicit command overwrite');
            var copy = Files.readAllBytes(new F(upgrade.getPath() + '.v0.bak').toPath());
            server.getCommandManager().executeCommand(server,'noppes clone add ReadableSeed 1 RC_upgrade');
            check('backup-not-replaced',Arrays.equals(copy,Files.readAllBytes(new F(upgrade.getPath() + '.v0.bak').toPath())),'second explicit save');
        }
        if (PHASE === 'verify') {
            check('restart-server-read', C.Instance.getCloneData(null,'RC_onServer',1) !== null && C.Instance.getCloneData(null,'RC_upgrade',1) !== null, 'new JVM reads readable storage');
            rigScript('local-placement',getAt(world,18)); rigScript('server-placement',getAt(world,22));
            getAt(world,18).display.setName('RC_LocalPlaced'); getAt(world,22).display.setName('RC_ServerPlaced');
        }
        if (PHASE === 'back') {
            var original = file('RC_onServer'), bytes = Files.readAllBytes(original.toPath());
            var newData = C.Instance.getCloneData(null,'RC_onServer',1);
            check('readable-at-zero',newData !== null && Arrays.equals(bytes,Files.readAllBytes(original.toPath())),'reading new format at 0 never writes');
            C.Instance.saveClone(1,'RC_legacyExport',newData);
            check('legacy-reexport',('' + text(file('RC_legacyExport'))) === ('' + NJ.Convert(newData)) && RC.same(newData,NJ.LoadFile(file('RC_legacyExport'))),'export at 0 for rollback');
            var empty = getAt(world,16) === null;
            server.getCommandManager().executeCommand(server,'noppes clone spawn RC_onServer 1 16,3,3 RC_OffReadable');
            check('readable-command-spawn-at-zero',empty && getAt(world,16) !== null && ('' + getAt(world,16).rwRig.model) === 'knight','command consumes readable clone while writer is 0');
        }
        var names = C.Instance.getClones(1);
        check('clone-list-no-artifacts', !names.contains('CLONE-FORMAT') && !names.contains('RC_upgrade.json.v0.bak') && names.contains('ReadableSeed'), 'real getClones');
    } catch (err) { check('unexpected-error',false,'' + err); }
    Log.info('READABLEPROBE DONE phase=' + PHASE + ' checks=' + count + ' failed=' + failed);
    e.npc.despawn();
}
