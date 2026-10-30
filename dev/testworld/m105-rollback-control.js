// M10.5 only. Packaged O -> K -> K restart -> O -> O restart, then restored O.
// Public original CNPC API and stable CNPC/Forge members; no MCP/SRG Minecraft member calls.
var RB_PHASE = @@PHASE@@;
var RB_OUT = @@OUTPUT@@;
var RB_SEED = @@SEED@@;
var RB_K = @@CANDIDATE@@;
var RB_PLAYER = 'M105_Player';
var RB_UUID = '2978a942-7037-49f8-954e-32402847b105';
var RB_JAVA = 'import noppes.npcs.rework.script.NpcScript;\nimport noppes.npcs.api.event.NpcEvent;\npublic class RollbackNpc extends NpcScript {\n @Override public void tick(NpcEvent.UpdateEvent e) { e.npc.getTempdata().put("rb-java", "ran"); }\n}\n';

function rbApi() { return Java.type('noppes.npcs.api.NpcAPI').Instance(); }
function rbLog(s) { Java.type('java.lang.System').out.println('M105 ' + s); }
function rbRead(path) {
    return JSON.parse(String(new java.lang.String(Java.type('java.nio.file.Files').readAllBytes(Java.type('java.nio.file.Paths').get(path)), 'UTF-8')));
}
function rbCheck(proof, key, ok, detail) {
    proof.checks.push({ key: key, ok: !!ok, detail: String(detail) });
    if (!ok) { throw new Error(key + ': ' + detail); }
}
function rbFind(w, name) {
    var all = w.getAllEntities(2);
    for (var i = 0; i < all.length; i++) { if (String(all[i].getName()) === name) { return all[i]; } }
    return null;
}
function rbNames(w) {
    var out = [], all = w.getAllEntities(2);
    for (var i = 0; i < all.length; i++) { var n = String(all[i].getName()); if (n.indexOf('RB_') === 0) { out.push(n); } }
    return out.sort();
}
function rbSpawn(w, name, x) {
    var n = rbApi().spawnNPC(w.getMCWorld(), x, 4, 8);
    n.setName(name); n.setHome(x, 4, 8); n.getAi().setMovingType(0);
    n.getStats().setMaxHealth(37); n.setHealth(37);
    n.getInventory().setRightHand(w.createItem('minecraft:iron_sword', 0, 1));
    n.getStoreddata().put('rb-persistent', name);
    return n;
}
function rbCat(list) {
    for (var i = 0; i < list.size(); i++) { if (String(list.get(i).getName()) === 'M105') { return list.get(i); } }
    throw new Error('Missing M105 category');
}
function rbPlayer() {
    return Java.type('noppes.npcs.controllers.PlayerDataController').instance.getDataFromUsername(
        Java.type('net.minecraftforge.fml.common.FMLCommonHandler').instance().getMinecraftServerInstance(), RB_PLAYER);
}
function rbTab(n) { return n.getMCEntity().script.getScripts().get(0); }
function rbData(w, qid, did, player) {
    var names = rbNames(w), uuids = {};
    for (var i = 0; i < names.length; i++) { uuids[names[i]] = String(rbFind(w, names[i]).getUUID()); }
    var q = rbApi().getQuests().get(qid), d = rbApi().getDialogs().get(did);
    return { names: names, uuids: uuids, qid: qid, did: did, questName: String(q.getName()),
        questText: String(q.getLogText()), dialogName: String(d.getName()), dialogText: String(d.getText()),
        playerNbt: String(rbApi().getINbt(player.getNBT()).toJsonString()) };
}
function rbCommon(proof, w, expected) {
    rbCheck(proof, 'npc-set', rbNames(w).join(',') === expected.names.join(','), rbNames(w));
    for (var i = 0; i < expected.names.length; i++) {
        var name = expected.names[i], n = rbFind(w, name);
        rbCheck(proof, 'uuid:' + name, n !== null && String(n.getUUID()) === expected.uuids[name], n === null ? 'missing' : n.getUUID());
        var item = n.getInventory().getRightHand();
        rbCheck(proof, 'state:' + name, n.getMaxHealth() === 37 && String(n.getStoreddata().get('rb-persistent')) === name
            && String(item.getName()) === 'minecraft:iron_sword' && item.getStackSize() === 1, name);
    }
    var q = rbApi().getQuests().get(expected.qid), d = rbApi().getDialogs().get(expected.did);
    rbCheck(proof, 'quest-dialog-readable', q !== null && d !== null && String(q.getName()) === expected.questName
        && String(q.getLogText()) === expected.questText && String(d.getName()) === expected.dialogName
        && String(d.getText()) === expected.dialogText && d.getQuest() !== null && d.getQuest().getId() === expected.qid
        && rbFind(w, 'RB_Base').getDialog(0).getId() === expected.did, 'same IDs, text and NPC/quest links');
    var p = rbPlayer();
    rbCheck(proof, 'playerdata-readable-equal', p !== null && String(p.playername) === RB_PLAYER && String(p.uuid) === RB_UUID
        && rbApi().getINbt(p.getNBT()).isEqual(rbApi().stringToNbt(expected.playerNbt)), 'all CNPC playerdata, no masks; synthetic offline record');
    return p;
}
function rbModes(proof, w, candidate) {
    var f = rbFind(w, 'RB_Flight'), tag = f.getEntityNbt();
    rbCheck(proof, 'rework-nbt', candidate ? tag.getInteger('RwFlightMode') === 1 && tag.getInteger('RwHitboxMode') === 1
        : !tag.has('RwFlightMode') && !tag.has('RwHitboxMode'), candidate ? 'both modes=1 persisted' : 'original ignores/drops both rework keys');
    var j = rbFind(w, 'RB_Java'), raw = j.getMCEntity();
    rbCheck(proof, 'java-source-preserved', String(raw.script.getLanguage()) === 'Java' && raw.script.getEnabled()
        && raw.script.getScripts().size() === 1 && String(rbTab(j).script) === RB_JAVA, 'source/language/enabled unchanged');
    // Actual EventHooks dispatch; do not change/disable the Java script for the original.
    j.getTempdata().remove('rb-java');
    Java.type('noppes.npcs.EventHooks').onNPCTick(raw);
    rbCheck(proof, 'java-execution', candidate ? String(j.getTempdata().get('rb-java')) === 'ran' && !rbTab(j).errored
        : !j.getTempdata().has('rb-java') && rbTab(j).errored, candidate ? 'Java hook ran' : 'no Java engine: inert tab, errored=true, no thrown exception');
}
function rbRun(proof, npc) {
    var w = npc.getWorld(), Int = Java.type('java.lang.Integer');
    var candidate = RB_PHASE === 'candidate' || RB_PHASE === 'candidate-restart';
    var rework = false;
    try { Java.type('noppes.npcs.rework.core.RwCoreMod'); rework = true; } catch (absent) { }
    rbCheck(proof, 'runtime-arm', rework === candidate, rework ? 'K' : 'O');
    if (RB_PHASE === 'seed') {
        var base = rbSpawn(w, 'RB_Base', 4);
        var q = rbCat(rbApi().getQuests().categories()).create();
        q.setType(5); q.setName('RB_Quest'); q.setLogText('before-candidate'); q.setCompleteText('rollback'); q.setNpcName('RB_Base'); q.save();
        var d = rbCat(rbApi().getDialogs().categories()).create();
        d.setName('RB_Dialog'); d.setText('before-candidate'); d.setQuest(q); d.save(); base.setDialog(0, d);
        var p = new (Java.type('noppes.npcs.controllers.data.PlayerData'))();
        p.playername = RB_PLAYER; p.uuid = RB_UUID;
        p.dialogData.dialogsRead.add(Int.valueOf(d.getId()));
        var data = new (Java.type('noppes.npcs.controllers.data.QuestData'))(Java.type('noppes.npcs.controllers.QuestController').instance.quests.get(Int.valueOf(q.getId())));
        p.questData.activeQuests.put(Int.valueOf(q.getId()), data); p.save(false);
        proof.data = rbData(w, q.getId(), d.getId(), p);
        rbCheck(proof, 'seed-fixture', proof.data.names.join(',') === 'RB_Base' && p.questData.activeQuests.size() === 1, 'NPC, quest/dialog, active quest and read dialog in playerdata');
        return;
    }
    var expected = rbRead(RB_PHASE === 'candidate' || RB_PHASE === 'restored' ? RB_SEED : RB_K).data;
    var p = rbCommon(proof, w, expected);
    if (RB_PHASE === 'candidate') {
        var f = rbSpawn(w, 'RB_Flight', 8), nbt = f.getEntityNbt();
        nbt.setInteger('RwFlightMode', 1); nbt.setInteger('RwHitboxMode', 1); f.setEntityNbt(nbt);
        var j = rbSpawn(w, 'RB_Java', 12), handler = j.getMCEntity().script;
        handler.setLanguage('Java'); handler.setEnabled(true);
        var tab = new (Java.type('noppes.npcs.controllers.ScriptContainer'))(handler); tab.script = RB_JAVA; handler.getScripts().add(tab);
        var q = rbApi().getQuests().get(expected.qid), d = rbApi().getDialogs().get(expected.did);
        q.setLogText('written-by-candidate'); q.save(); d.setText('written-by-candidate'); d.save();
        p.questData.activeQuests.remove(Int.valueOf(expected.qid));
        p.questData.finishedQuests.put(Int.valueOf(expected.qid), Java.type('java.lang.Long').valueOf('1234567890123')); p.save(false);
        rbModes(proof, w, true);
        proof.data = rbData(w, expected.qid, expected.did, p);
        rbCheck(proof, 'candidate-fixture', proof.data.names.join(',') === 'RB_Base,RB_Flight,RB_Java'
            && proof.data.playerNbt !== expected.playerNbt && proof.data.uuids.RB_Base === expected.uuids.RB_Base, 'candidate wrote new entities, settings, source, content and player progress');
    } else if (RB_PHASE !== 'restored') {
        rbModes(proof, w, candidate);
        proof.data = rbData(w, expected.qid, expected.did, p);
    } else {
        proof.data = rbData(w, expected.qid, expected.did, p);
        rbCheck(proof, 'pre-candidate-restored', rbFind(w, 'RB_Flight') === null && rbFind(w, 'RB_Java') === null
            && p.questData.activeQuests.containsKey(Int.valueOf(expected.qid)) && p.questData.finishedQuests.isEmpty(), 'candidate progress/entities absent; old active quest restored');
    }
}
function tick(e) {
    var temp = e.npc.getTempdata();
    if (temp.has('m105-once')) { return; } temp.put('m105-once', true);
    var proof = { phase: RB_PHASE, success: false, checks: [] };
    try { rbRun(proof, e.npc); proof.success = true; }
    catch (error) { proof.error = String(error); rbLog('FAILED ' + RB_PHASE + ' ' + error); }
    finally {
        var text = new java.lang.String(JSON.stringify(proof, null, 1));
        Java.type('java.nio.file.Files').write(Java.type('java.nio.file.Paths').get(RB_OUT), text.getBytes('UTF-8'));
        e.npc.despawn(); rbLog(RB_PHASE.toUpperCase() + ' DONE success=' + proof.success);
    }
}
