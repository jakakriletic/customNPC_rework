// Controller-only M16.7c1 proof. PHASE is prepended by clone-workspace-run.ps1.
var N = Java.type('net.minecraft.nbt.NBTTagCompound');
var L = Java.type('net.minecraft.nbt.NBTTagList');
var C = Java.type('noppes.npcs.controllers.ServerCloneController');
var S = Java.type('noppes.npcs.CustomNpcs');
var RC = Java.type('noppes.npcs.rework.data.ReadableClone');
var RF = Java.type('noppes.npcs.rework.data.ReadableCloneFiles');
var W = Java.type('noppes.npcs.rework.data.CloneWorkspace');
var Files = Java.type('java.nio.file.Files');
var F = Java.type('java.io.File');
var UTF8 = Java.type('java.nio.charset.StandardCharsets').UTF_8;
var Str = Java.type('java.lang.String');
var Arrays = Java.type('java.util.Arrays');
var Log = Java.type('noppes.npcs.LogWriter');
var NPC = Java.type('noppes.npcs.entity.EntityNPCInterface');
var API = Java.type('noppes.npcs.api.NpcAPI');
var done = false, started = false, count = 0, failed = 0, age = 0;
function check(key, ok) {
    count++; if (!ok) { failed++; }
    Log.info('WORKSPACEPROBE CHECK phase=' + PHASE + ' key=' + key + ' ok=' + (ok ? 1 : 0));
}
function txt(path) { return '' + new Str(Files.readAllBytes(path), UTF8); }
function write(path, value) { Files.write(path, new Str(value).getBytes(UTF8)); }
function snap(npc) { var n = new N(); npc.writeToNBTAtomically(n); C.Instance.cleanTags(n); return n; }
function cloneFile(name) { return new F(new F(C.Instance.getDir(), '1'), name + '.json'); }
function locate(world, name) {
    var it = world.loadedEntityList.iterator();
    while (it.hasNext()) { var n = it.next(); if (n instanceof NPC && ('' + n.getName()) === name && n.isEntityAlive()) { return n; } }
    return null;
}
function cmd(server, text) { return server.getCommandManager().executeCommand(server, text); }
function code(language, tab, value) {
    if (language === 'Java') {
        return 'import noppes.npcs.api.event.NpcEvent;\r\npublic class WorkspaceTab' + tab + ' {\r\n' +
            ' public void init(NpcEvent.InitEvent e) { e.npc.getStoreddata().put("ws' + tab + '", "' + value + '"); System.out.println("WORKSPACERAN ' + value + ' tab=' + tab + ' name=" + e.npc.getName()); }\r\n}\r\n';
    }
    return 'function init(e) { e.npc.getStoreddata().put("ws' + tab + '", "' + value + '"); Java.type("java.lang.System").out.println("WORKSPACERAN ' + value + ' tab=' + tab + ' name=" + e.npc.getName()); }\n';
}
function setup(mc, server, language, sourceReadable, index) {
    var suffix = language === 'Java' ? 'java' : 'js', id = PHASE + '_' + suffix;
    var sourceName = 'Source ' + id, outputName = 'Packed ' + id;
    var n = snap(mc); n.setString('Name', 'Template_' + id); n.setString('ScriptLanguage', language); n.setBoolean('ScriptEnabled', true);
    // These fields would be removed by cleanTags. Stored clone unpack must keep them exactly.
    n.setString('RwCommandStaff', 'archived data'); n.setLong('UUIDMost', 12345); n.setLong('UUIDLeast', 67890 + index);
    n.setLong('workspace-unknown', Java.type('java.lang.Long').MAX_VALUE);
    var tabs = new L();
    for (var i = 0; i < 2; i++) {
        var tab = new N(); tab.setString('Script', code(language, i, 'original_' + id));
        var includes = new L(), include = new N(); include.setString('Line', 'workspace-helper.' + suffix); includes.appendTag(include); tab.setTag('ScriptList', includes);
        var console = new L(), row = new N(); row.setLong('Long', 7); row.setString('String', 'saved console'); console.appendTag(row); tab.setTag('Console', console);
        tab.setString('workspace-tab-extra', 'preserve me'); tabs.appendTag(tab);
    }
    n.setTag('Scripts', tabs);
    var rig = new N(); rig.setString('Model', 'knight'); rig.setString('Clips', '{"animations":{}}'); n.setTag('RwRig', rig);
    RF.save(cloneFile(sourceName), n, sourceReadable);
    var originalBytes = Files.readAllBytes(cloneFile(sourceName).toPath());
    check(id + '-unpack-command', cmd(server, 'rwcreature unpack clone 1 ' + id + ' ' + sourceName) === 1);
    var creatures = S.Dir.toPath().resolve('creatures'), dir = creatures.resolve(id);
    check(id + '-typed-unpack', RC.same(n, W.read(creatures, id)));
    check(id + '-exact-code-bytes', Arrays.equals(new Str(code(language, 0, 'original_' + id)).getBytes(UTF8), Files.readAllBytes(dir.resolve('script.' + suffix))));
    var expected = n.copy(), changedCode = code(language, 0, 'edited_' + id), changedClips = '{\r\n "animations": {}\r\n}\n';
    write(dir.resolve('script.' + suffix), changedCode); write(dir.resolve('animations.json'), changedClips);
    expected.getTagList('Scripts', 10).getCompoundTagAt(0).setString('Script', changedCode);
    expected.getCompoundTag('RwRig').setString('Clips', changedClips);
    expected.setString('Name', 'Edited setting ' + id);
    var inline = n.copy(); inline.setString('Name', expected.getString('Name')); write(dir.resolve('clone.json'), RC.encode(inline));
    check(id + '-pack-command', cmd(server, 'rwcreature pack ' + id + ' 1 ' + outputName) === 1);
    var output = RF.load(cloneFile(outputName));
    check(id + '-typed-pack', RC.same(expected, output));
    check(id + '-output-format', RF.hasEnvelope(txt(cloneFile(outputName).toPath())) === (S.RwReadableClones === 1));
    check(id + '-source-unchanged', Arrays.equals(originalBytes, Files.readAllBytes(cloneFile(sourceName).toPath())));
    var packedBytes = Files.readAllBytes(cloneFile(outputName).toPath());
    check(id + '-no-overwrite', cmd(server, 'rwcreature pack ' + id + ' 1 ' + outputName) === 0 && Arrays.equals(packedBytes, Files.readAllBytes(cloneFile(outputName).toPath())));
    check(id + '-spawn-command', cmd(server, 'noppes clone spawn ' + outputName.replace(/ /g, '%') + ' 1 ' + (4 + index * 4) + ',4,3 WSPlaced_' + id) === 1);
    return id;
}
function assertLive(world, id) {
    var n = locate(world, 'WSPlaced_' + id), a = n === null ? null : API.Instance().getIEntity(n);
    check(id + '-live-language-tabs-rig', n !== null && ('' + n.script.getLanguage()) === (id.indexOf('java') >= 0 ? 'Java' : 'ECMAScript') && n.script.getScripts().size() === 2 && ('' + n.rwRig.model) === 'knight');
    check(id + '-first-tab-runs', a !== null && ('' + a.getStoreddata().get('ws0')) === 'edited_' + id);
    check(id + '-second-tab-runs', a !== null && ('' + a.getStoreddata().get('ws1')) === 'original_' + id);
}
function tick(e) {
    if (done) { return; }
    var mc = e.npc.getMCEntity(), server = mc.getServer(), world = mc.world;
    try {
        if (!started) {
            started = true;
            check('switch', S.RwReadableClones === (PHASE === 'off' ? 0 : 1));
            if (PHASE !== 'verify') {
                setup(mc, server, 'Java', false, PHASE === 'off' ? 0 : 2);
                setup(mc, server, 'ECMAScript', true, PHASE === 'off' ? 1 : 3);
                check('bad-id-refused', cmd(server, 'rwcreature unpack clone 1 ../escape Source ' + PHASE + '_java') === 0);
                check('bad-tab-refused', cmd(server, 'rwcreature pack ' + PHASE + '_java 0 Invalid') === 0);
                check('bad-name-refused', cmd(server, 'rwcreature pack ' + PHASE + '_java 1 NUL') === 0);
                check('multi-selector-refused', cmd(server, 'rwcreature unpack npc @e[type=customnpcs:customnpc] ' + PHASE + '_multiple') === 0);
                check('non-npc-refused', cmd(server, 'rwcreature unpack npc @e[type=minecraft:pig,c=1] ' + PHASE + '_pig') === 0);
                // Exercise permission level 2 (existing command level) vs the new writer level 4.
                // Use vanilla's real sender wrapper: Java.extend cannot bridge
                // Nashorn and Forge's LaunchClassLoader for ICommandSender.
                var Wrapper = Java.type('net.minecraft.command.CommandSenderWrapper');
                var limited = Wrapper.create(server).withPermissionLevel(2).withSendCommandFeedback(false);
                check('level2-writer-refused', limited.canUseCommand(2, 'rwcreature') && !limited.canUseCommand(4, 'rwcreature') &&
                    server.getCommandManager().executeCommand(limited, 'rwcreature list') === 1 &&
                    server.getCommandManager().executeCommand(limited, 'rwcreature unpack clone 1 ' + PHASE + '_denied Source ' + PHASE + '_java') === 0 &&
                    !new F(S.Dir, 'creatures/' + PHASE + '_denied').exists());
            }
            return;
        }
        age++;
        if (age < 5) { return; } // Real script ticks, not sleeping or forcing an init hook.
        done = true;
        var ids = PHASE === 'verify' ? ['off_java', 'off_js', 'on_java', 'on_js'] : [PHASE + '_java', PHASE + '_js'];
        for (var j = 0; j < ids.length; j++) { assertLive(world, ids[j]); }
        if (PHASE !== 'verify') {
            var live = locate(world, 'WSPlaced_' + PHASE + '_java'), before = snap(live), root = S.Dir.toPath().resolve('creatures');
            check('unpack-live-command', cmd(server, 'rwcreature unpack npc ' + live.getUniqueID() + ' ' + PHASE + '_live') === 1);
            check('unpack-live-clean-snapshot', RC.same(before, W.read(root, PHASE + '_live')) && RC.same(before, snap(live)));
            check('pack-live-command', cmd(server, 'rwcreature pack ' + PHASE + '_live 1 Live ' + PHASE) === 1);
            check('pack-live-exact', RC.same(before, RF.load(cloneFile('Live ' + PHASE))));
        }
    } catch (failure) {
        failed++; Log.info('WORKSPACEPROBE ERROR ' + failure + ' ' + (failure.stack || ''));
        done = true;
    }
    if (done) { Log.info('WORKSPACEPROBE DONE phase=' + PHASE + ' checks=' + count + ' failed=' + failed); }
}
