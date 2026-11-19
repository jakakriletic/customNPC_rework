// Controller-only M16.7c2. PHASE and SIGNAL are prepended by the PowerShell scenario.
var N = Java.type('net.minecraft.nbt.NBTTagCompound'), L = Java.type('net.minecraft.nbt.NBTTagList');
var W = Java.type('noppes.npcs.rework.data.CloneWorkspace'), RC = Java.type('noppes.npcs.rework.data.ReadableClone');
var C = Java.type('noppes.npcs.controllers.ServerCloneController'), S = Java.type('noppes.npcs.CustomNpcs');
var Assets = Java.type('noppes.npcs.rework.rig.RigCreatureAssets'), Models = Java.type('noppes.npcs.rework.rig.RigModels');
var NPC = Java.type('noppes.npcs.entity.EntityNPCInterface'), API = Java.type('noppes.npcs.api.NpcAPI');
var Files = Java.type('java.nio.file.Files'), Paths = Java.type('java.nio.file.Paths');
var Str = Java.type('java.lang.String'), UTF8 = Java.type('java.nio.charset.StandardCharsets').UTF_8;
var Log = Java.type('noppes.npcs.LogWriter');
var stage = 0, age = 0, checks = 0, failed = 0, done = false, original = {}, uid = {}, beforeRead;
var root = S.Dir.toPath().resolve('creatures');
function check(key, ok) {
    checks++; if (!ok) { failed++; }
    Log.info('WSLIVE CHECK phase=' + PHASE + ' key=' + key + ' ok=' + (ok ? 1 : 0));
}
function write(path, text) { Files.write(path, new Str(text).getBytes(UTF8)); }
function text(path) { return '' + new Str(Files.readAllBytes(path), UTF8); }
function diskSnapshot() {
    var result = {};
    function visit(file) {
        if (file.isDirectory()) { var children = file.listFiles(); for (var i = 0; i < children.length; i++) { visit(children[i]); } }
        else { result['' + file.getAbsolutePath()] = Files.readAllBytes(file.toPath()); }
    }
    for (var id in {a_rig:1,b_plain:1,c_disabled:1}) { visit(root.resolve(id).toFile()); }
    return result;
}
function sameFiles(before) {
    var after = diskSnapshot(), Arrays = Java.type('java.util.Arrays');
    if (Object.keys(before).length !== Object.keys(after).length) { return false; }
    for (var key in before) { if (!after[key] || !Arrays.equals(before[key],after[key])) { return false; } }
    return true;
}
function snap(npc) { var n = new N(); npc.writeToNBTAtomically(n); C.Instance.cleanTags(n); return n; }
function cmd(server, text) { return server.getCommandManager().executeCommand(server, text); }
function snapshot() { var f = Assets.class.getDeclaredField('server'); f.setAccessible(true); return f.get(null); }
function wireSafe() {
    var s = snapshot(), f = s.getClass().getDeclaredField('data'); f.setAccessible(true);
    var bundle = Java.type('noppes.npcs.rework.anim.ReloadCodec').decodeBundle(f.get(s)), keys = bundle.keySet().iterator();
    while (keys.hasNext()) { var key = '' + keys.next(); if (!/^[bagt]_[a-z0-9_]+$/.test(key)) { return false; } }
    var raw = '' + new Str(f.get(s),UTF8);
    return raw.indexOf('public class LiveTab') < 0 && raw.indexOf('liveHelper') < 0 && raw.indexOf('clone-workspace') < 0 && raw.indexOf('WS_Rig_v1') < 0;
}
function npc(world, name, x) {
    var it = world.loadedEntityList.iterator();
    while (it.hasNext()) { var n = it.next(); if (n instanceof NPC && ('' + n.getName()) === name && n.isEntityAlive() && (x === undefined || Math.abs(n.posX - x) < 0.1)) { return n; } }
    return null;
}
function code(language, tab, rev) {
    if (language === 'Java') {
        return 'import noppes.npcs.api.event.NpcEvent;\r\npublic class LiveTab' + tab + ' {\r\n' +
            ' public void init(NpcEvent.InitEvent e) { e.npc.getStoreddata().put("live' + tab + '", LiveHelper.value("' + rev + '")); System.out.println("WSLIVE RAN Java ' + rev + ' tab=' + tab + ' name=" + e.npc.getName()); }\r\n}\r\n';
    }
    return 'function init(e) { liveHelper(e, "live' + tab + '", "' + rev + '"); Java.type("java.lang.System").out.println("WSLIVE RAN JS ' + rev + ' tab=' + tab + ' name=" + e.npc.getName()); }\n';
}
function clips(rev) {
    return '{"animations":{"workspace_pose_' + rev + '":{"animation_length":60,"loop":true,"bones":{"armL":{"rotation":{"0.0":[-70,0,0],"60.0":[-70,0,0]}}}}}}';
}
function fixture(mc, language, name, model, enabled) {
    var n = snap(mc); n.setString('Name', name); n.setString('ScriptLanguage', language); n.setBoolean('ScriptEnabled', enabled);
    n.setInteger('MaxHealth', 37); n.removeTag('StoredData');
    var tabs = new L();
    for (var i = 0; i < 2; i++) {
        var t = new N(), includes = new L(), inc = new N();
        t.setString('Script', code(language, i, 'v1'));
        inc.setString('Line', language === 'Java' ? 'live-helper.java' : 'live-helper.js'); includes.appendTag(inc);
        t.setTag('ScriptList', includes); t.setTag('Console', new L()); tabs.appendTag(t);
    }
    n.setTag('Scripts', tabs); n.removeTag('RwRig');
    if (model) {
        var r = new N(); r.setString('Model', model); r.setString('Clips', clips('v1')); r.setString('Idle', 'workspace_pose_v1');
        n.setTag('RwRig', r);
    }
    return n;
}
function spawn(server, id, x) { check('spawn-' + id + '-' + x, cmd(server, 'rwcreature spawn ' + id + ' ' + x + '.0 4.0 5.0') === 1); }
function fields(npc, expected, key) {
    var n = snap(npc), a = n.getTagList('Scripts', 10), b = expected.getTagList('Scripts', 10);
    var same = a.tagCount() === 2 && b.tagCount() === 2 && ('' + n.getString('ScriptLanguage')) === ('' + expected.getString('ScriptLanguage')) && n.getBoolean('ScriptEnabled') === expected.getBoolean('ScriptEnabled');
    for (var i = 0; i < 2 && same; i++) {
        same = ('' + a.getCompoundTagAt(i).getString('Script')) === ('' + b.getCompoundTagAt(i).getString('Script')) &&
            a.getCompoundTagAt(i).getTag('ScriptList').equals(b.getCompoundTagAt(i).getTag('ScriptList'));
    }
    check(key + '-language-tabs-includes-enabled', same);
    check(key + '-rig-bindings-clips', n.getCompoundTag('RwRig').equals(expected.getCompoundTag('RwRig')));
    check(key + '-health-position-start', npc.getMaxHealth() === expected.getInteger('MaxHealth') && Math.abs(npc.posY - 4) < 0.1 && npc.ais.startPos().getY() === 4);
    var data = API.Instance().getIEntity(npc).getStoreddata();
    check(key + '-scripts-run-or-disabled', expected.getBoolean('ScriptEnabled') ?
        ('' + data.get('live0')) === (key.indexOf('v2') >= 0 ? 'v2' : 'v1') && ('' + data.get('live1')) === 'v1' : data.get('live0') === null && data.get('live1') === null);
}
function setup(mc, server) {
    // Candidate model sorts AFTER a_rig, exercising resolution against the whole revision.
    var ordinary = root.resolve('z_model'); Files.createDirectories(ordinary);
    write(ordinary.resolve('creature.json'), JSON.stringify({base:'knight', name:'WS_Ordinary',
        script:'function init(e) { e.npc.getStoreddata().put("ordinary","ran"); }'}));
    write(ordinary.resolve('animations.json'), '{"animations":{"m15_pose":{"animation_length":2}}}');
    var ordinaryClone = fixture(mc, 'Java', 'ignored by M15', '', false);
    write(ordinary.resolve('clone.json'), RC.encode(ordinaryClone));
    original.a_rig = fixture(mc, 'Java', 'WS_Rig_v1', 'z_model', true);
    original.b_plain = fixture(mc, 'ECMAScript', 'WS_Plain_v1', '', true);
    original.c_disabled = fixture(mc, 'ECMAScript', 'WS_Disabled', 'knight', false);
    for (var id in original) { W.unpack(root, id, original[id]); }
    beforeRead = diskSnapshot();
    // A staging marker must never block a reload or become a pack.
    Files.createDirectories(root.resolve('.clone-workspace-incomplete'));
    write(root.resolve('.clone-workspace-incomplete/clone-workspace.json'), 'broken');
    cmd(server, 'rwcreature reload');
    check('loaded-workspace-ids', Assets.ids().contains('a_rig') && Assets.ids().contains('b_plain') && Assets.ids().contains('c_disabled'));
    if (!Assets.ids().contains('a_rig') || !Assets.ids().contains('b_plain') || !Assets.ids().contains('c_disabled') || !Assets.ids().contains('z_model')) {
        throw new Error('Fixture reload did not publish the four required packs');
    }
    check('workspace-id-not-model', Models.get('a_rig') === null && Models.get('b_plain') === null && Models.get('c_disabled') === null);
    check('wire-has-only-model-assets', wireSafe());
    spawn(server, 'a_rig', -3); spawn(server, 'b_plain', 0); spawn(server, 'c_disabled', 3); spawn(server, 'z_model', 6);
}
function verifyInitial(world) {
    var specs = [['a_rig','WS_Rig_v1',-3], ['b_plain','WS_Plain_v1',0], ['c_disabled','WS_Disabled',3]];
    for (var i = 0; i < specs.length; i++) {
        var spec = specs[i];
        var n = npc(world, spec[1], spec[2]); if (n === null) { return false; }
    }
    return true;
}
function edited(id, language, name) {
    var n = original[id].copy(); n.setString('Name', name); n.setInteger('MaxHealth', 41);
    write(root.resolve(id + '/clone.json'), RC.encode(n));
    write(root.resolve(id + (language === 'Java' ? '/script.java' : '/script.js')), code(language, 0, 'v2'));
    if (language === 'Java') { write(root.resolve(id + '/animations.json'), clips('v2'));
        n.getCompoundTag('RwRig').setString('Idle', 'workspace_pose_v2');
        write(root.resolve(id + '/clone.json'), RC.encode(n)); }
}
function changed(world, server) {
    var acceptedFiles = diskSnapshot();
    fields(npc(world,'WS_Rig_v1',-3), original.a_rig, 'old-rig-v1');
    fields(npc(world,'WS_Plain_v1',0), original.b_plain, 'old-plain-v1');
    fields(npc(world,'WS_Disabled',3), original.c_disabled, 'disabled-v1');
    var rig = npc(world,'WS_Rig_v2',-6), plain = npc(world,'WS_Plain_v2',-9);
    fields(rig, W.read(root, 'a_rig'), 'new-rig-v2'); fields(plain, W.read(root, 'b_plain'), 'new-plain-v2');
    check('fresh-identity-startpos', ('' + rig.getUniqueID()) !== uid.rig && ('' + plain.getUniqueID()) !== uid.plain && rig.ais.startPos().getX() === -6 && plain.ais.startPos().getX() === -9);
    var m15 = npc(world,'WS_Ordinary',6), n = snap(m15);
    check('ordinary-m15-replaces-clone-rig-and-scripts', ('' + m15.rwRig.model) === 'z_model' && ('' + m15.script.getLanguage()) === 'ECMAScript' && m15.script.getScripts().size() === 1 && n.getBoolean('ScriptEnabled') && ('' + API.Instance().getIEntity(m15).getStoreddata().get('ordinary')) === 'ran' && m15.rwRig.library().has('m15_pose'));
    // A valid ordinary change plus one bad workspace must publish NEITHER side.
    var javaPath = root.resolve('a_rig/script.java'), saved = text(javaPath), accepted = snapshot();
    var clipPath = root.resolve('a_rig/animations.json'), validClips = text(clipPath);
    write(clipPath, '{"animations":[]}');
    write(root.resolve('z_model/animations.json'), '{"animations":{"should_not_publish":{"animation_length":2}}}');
    cmd(server, 'rwcreature reload');
    check('bad-clips-keeps-entire-snapshot', accepted.equals(snapshot()) && !Models.get('z_model').clips.has('should_not_publish'));
    write(clipPath, validClips);
    Files.delete(javaPath);
    write(root.resolve('z_model/animations.json'), '{"animations":{"should_not_publish":{"animation_length":2}}}');
    cmd(server, 'rwcreature reload');
    check('bad-reload-keeps-entire-model-snapshot', accepted.equals(snapshot()) && !Models.get('z_model').clips.has('should_not_publish') && Models.get('z_model').clips.has('m15_pose'));
    cmd(server, 'rwcreature deploy a_rig -12.0 4.0 5.0');
    check('bad-deploy-no-spawn', npc(world,'WS_Rig_v2',-12) === null);
    spawn(server, 'a_rig', -12);
    check('bad-reload-keeps-clone-snapshot', npc(world,'WS_Rig_v2',-12) !== null);
    write(javaPath, saved); write(root.resolve('z_model/animations.json'), '{"animations":{"m15_pose":{"animation_length":2}}}');
    check('reload-spawn-never-rewrites-workspaces', sameFiles(acceptedFiles));
    cmd(server, 'rwrig play WS_Rig_v2 workspace_pose_v2');
    // Broken startup entry is skipped, healthy workspaces still load after restart.
    var bad = root.resolve('broken_start'); Files.createDirectories(bad);
    write(bad.resolve('clone-workspace.json'), '{"$format":"customnpcs-clone-workspace","version":99,"scriptTabCount":0,"sources":[]}');
}
function tick(e) {
    if (done) { return; }
    var mc = e.npc.getMCEntity(), world = mc.world, server = mc.getServer();
    try {
        age++;
        if (PHASE === 'restart') {
            if (age < 10 || !verifyInitial(world) || npc(world,'WS_Rig_v2',-6) === null) { return; }
            if (stage === 0) {
                check('startup-skips-broken-keeps-healthy', !Assets.ids().contains('broken_start') && Assets.ids().contains('a_rig') && Assets.ids().contains('b_plain') && Assets.ids().contains('c_disabled'));
                spawn(server, 'a_rig', -15); spawn(server, 'b_plain', -18);
                stage = 1; age = 0; return;
            }
            var rigNow = W.read(root,'a_rig'), plainNow = W.read(root,'b_plain');
            var rigOld = rigNow.copy(), plainOld = plainNow.copy();
            rigOld.setInteger('MaxHealth',37); rigOld.getTagList('Scripts',10).getCompoundTagAt(0).setString('Script',code('Java',0,'v1'));
            rigOld.getCompoundTag('RwRig').setString('Clips',clips('v1')); rigOld.getCompoundTag('RwRig').setString('Idle','workspace_pose_v1');
            plainOld.setInteger('MaxHealth',37); plainOld.getTagList('Scripts',10).getCompoundTagAt(0).setString('Script',code('ECMAScript',0,'v1'));
            fields(npc(world,'WS_Rig_v1',-3),rigOld,'restart-old-rig-v1'); fields(npc(world,'WS_Plain_v1',0),plainOld,'restart-old-plain-v1');
            fields(npc(world,'WS_Disabled',3),W.read(root,'c_disabled'),'restart-disabled-v1');
            fields(npc(world,'WS_Rig_v2',-6),rigNow,'restart-existing-rig-v2'); fields(npc(world,'WS_Plain_v2',-9),plainNow,'restart-existing-plain-v2');
            fields(npc(world,'WS_Rig_v2',-15),rigNow,'restart-new-rig-v2'); fields(npc(world,'WS_Plain_v2',-18),plainNow,'restart-new-plain-v2');
            cmd(server,'rwrig play WS_Rig_v2 workspace_pose_v2'); done = true;
        } else if (stage === 0) {
            setup(mc,server); stage = 1; age = 0;
        } else if (stage === 1 && age >= 10 && verifyInitial(world)) {
            fields(npc(world,'WS_Rig_v1',-3), original.a_rig, 'initial-rig-v1');
            fields(npc(world,'WS_Plain_v1',0), original.b_plain, 'initial-plain-v1');
            fields(npc(world,'WS_Disabled',3), original.c_disabled, 'initial-disabled-v1');
            uid.rig = '' + npc(world,'WS_Rig_v1',-3).getUniqueID(); uid.plain = '' + npc(world,'WS_Plain_v1',0).getUniqueID();
            check('initial-read-spawn-no-rewrite',sameFiles(beforeRead));
            stage = 2; Log.info('WSLIVE READY');
        } else if (stage === 2 && Files.exists(Paths.get(SIGNAL)) && text(Paths.get(SIGNAL)).trim() === 'edit') {
            edited('a_rig','Java','WS_Rig_v2'); edited('b_plain','ECMAScript','WS_Plain_v2');
            spawn(server,'a_rig',9);
            check('spawn-before-reload-uses-snapshot',npc(world,'WS_Rig_v1',9) !== null && npc(world,'WS_Rig_v2',9) === null);
            cmd(server,'rwcreature deploy a_rig -6.0 4.0 5.0'); stage = 3; age = 0;
        } else if (stage === 3 && npc(world,'WS_Rig_v2',-6) !== null) {
            spawn(server,'b_plain',-9); stage = 4; age = 0;
        } else if (stage === 4 && age >= 10 && npc(world,'WS_Plain_v2',-9) !== null) {
            changed(world,server); done = true;
        }
    } catch (failure) { failed++; done = true; Log.info('WSLIVE ERROR ' + failure + ' ' + (failure.stack || '')); }
    if (done) { Log.info('WSLIVE DONE phase=' + PHASE + ' checks=' + checks + ' failed=' + failed); }
}
