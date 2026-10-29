// M4.15b: real server editor contract. Installed only in a disposable test world.
var Gui = Java.type('noppes.npcs.rework.nav.NavGuiData');
var Nav = Java.type('noppes.npcs.rework.nav.RwNavBackend');
var Nbt = Java.type('net.minecraft.nbt.NBTTagCompound');
var Loader = Java.type('net.minecraftforge.fml.common.Loader');
var age = 0, checks = 0, failed = 0, actors = {}, control;
var names = ['Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier'];

function check(key, ok) {
    checks++;
    if (!ok) { failed++; }
    control.executeCommand('/say NAVGUI CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0));
}

function reason(specific) {
    if (Nav.globalMode() === 0) { return 'global_off'; }
    if (!Loader.isModLoaded('npcbaritone')) { return 'missing_mod'; }
    return specific;
}

function inspect(name, phase, selected, expected) {
    var mc = actors[name].getMCEntity();
    var nav = mc.getNavigator();
    var ai = mc.ais.writeToNBT(new Nbt());
    var data = Gui.reply(mc), status = data.getCompoundTag(Gui.STATUS);
    var key = phase + '-' + name;
    check(key + '-types', data.hasKey(Gui.CHOICE, 3) && data.hasKey(Gui.STATUS, 10)
        && status.hasKey('Actual', 3) && status.hasKey('Reason', 8));
    check(key + '-choice', data.getInteger(Gui.CHOICE) === selected);
    var runtimeBaritone = String(mc.getNavigator().getClass().getName()) === 'si.ladja.npcbaritone.forge.BaritonePathNavigate';
    check(key + '-actual', status.getInteger('Actual') === (runtimeBaritone ? 1 : 0)
        && status.getInteger('Actual') === (expected === 'active' ? 1 : 0));
    check(key + '-reason', String(status.getString('Reason')) === expected);
    data.removeTag(Gui.CHOICE);
    data.removeTag(Gui.STATUS);
    check(key + '-ai', ai.equals(data));
    check(key + '-readOnly', nav === mc.getNavigator() && ai.equals(mc.ais.writeToNBT(new Nbt())));
    var saved = new Nbt();
    mc.writeEntityToNBT(saved);
    check(key + '-persistent', !saved.hasKey(Gui.STATUS)
        && (selected === 0 ? !saved.hasKey(Gui.CHOICE) : saved.getInteger(Gui.CHOICE) === 1));
}

function saveNormal(selected, movement, legacy) {
    var mc = actors.Normal.getMCEntity();
    var data = mc.ais.writeToNBT(new Nbt());
    data.setInteger('MovementType', movement);
    if (!legacy) { data.setInteger(Gui.CHOICE, selected); }
    // The server must ignore an echoed/forged runtime status.
    var forged = new Nbt();
    forged.setInteger('Actual', 1);
    forged.setString('Reason', 'active');
    data.setTag(Gui.STATUS, forged);
    mc.updateAI = false;
    mc.updateClient = false;
    Gui.save(mc, data);
    check('save-' + age + '-choice', mc.getRwNavBackend() === selected);
    check('save-' + age + '-effects', mc.updateAI && mc.updateClient && mc.getHealth() === mc.getMaxHealth());
    data.removeTag(Gui.CHOICE);
    data.removeTag(Gui.STATUS);
    check('save-' + age + '-ai', data.equals(mc.ais.writeToNBT(new Nbt())));
}

function tick(e) {
    age += 10;
    control = e.npc;
    if (age === 10) {
        var all = e.npc.getWorld().getAllEntities(2), found = 0;
        for (var i = 0; i < all.length; i++) {
            var name = String(all[i].getName());
            if (name.indexOf('NAVGUI_') === 0 && name !== 'NAVGUI_Control') {
                actors[name.substring(7)] = all[i]; found++;
            }
        }
        check('six-actors', found === 6);
        actors.Rider.setMount(actors.Carrier);
    }
    if (age === 60) {
        var expected = ['active', 'unsupported', 'movement', 'movement', 'riding', 'carrying'];
        for (var j = 0; j < names.length; j++) { inspect(names[j], 'initial', 1, reason(expected[j])); }
    }
    if (age === 80) { saveNormal(0, 0, false); }
    if (age === 100) {
        inspect('Normal', 'off', 0, 'selected_vanilla');
        saveNormal(1, 0, false);
    }
    if (age === 120) {
        inspect('Normal', 'on', 1, reason('active'));
        saveNormal(1, 1, true); // legacy client changes movement but omits backend key
    }
    if (age === 140) {
        inspect('Normal', 'flying', 1, reason('movement'));
        saveNormal(1, 0, true);
    }
    if (age === 160) {
        inspect('Normal', 'rebuilt', 1, reason('active'));
        e.npc.executeCommand('/rwsquad kolona NAVGUI_Normal 40 4 0');
    }
    if (age === 180) {
        inspect('Normal', 'squad', 1, reason('squad'));
        e.npc.executeCommand('/rwsquad stop NAVGUI_Normal');
    }
    if (age === 200) {
        inspect('Normal', 'released', 1, reason('active'));
        e.npc.executeCommand('/say NAVGUI DONE checks=' + checks + ' failed=' + failed
            + ' mod=' + (Loader.isModLoaded('npcbaritone') ? 1 : 0) + ' global=' + Nav.globalMode());
        e.npc.despawn();
    }
}
