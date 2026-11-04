// M13.2 evidence only. The harness prepends: var MODE = 'off' | 'on' | 'reload' | 'budget';
// Linking and saving call the same controller methods as the LinkedSet and NpcMenuClose packets.
var L = Java.type('noppes.npcs.controllers.LinkedNpcController');
var Cat = Java.type('noppes.npcs.rework.linked.LinkedCategories');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var Api = Java.type('noppes.npcs.api.NpcAPI');
var SC = Java.type('noppes.npcs.controllers.ScriptContainer');
var AL = Java.type('java.util.ArrayList');
var IA = Java.type('int[]');
var NBT = Java.type('net.minecraft.nbt.NBTTagCompound');
var CODE = 'function init(e) { e.npc.getStoreddata().put("m132init", 1); }\n'
    + '// a "quoted" comment\n'
    + 'function tick(e) { e.npc.getStoreddata().put("m132ran", 1); }\n';
var GROUP = 'm132' + (MODE === 'reload' ? 'on' : MODE);
var age = 0, checks = 0, failed = 0, done = false, a = null, b = null, c = null, startB = null, startC = null, tabB = null;
var workers = [], savedAt = 0, applied0 = 0, deferred0 = 0, nanos0 = 0, time0 = 0, undoAt = -1;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('LINKED2 CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function end(npc) {
    done = true; npc.getStoreddata().put('m132done', 1);
    Log.info('LINKED2 DONE mode=' + MODE + ' checks=' + checks + ' failed=' + failed);
}
function cmd(npc, text) {
    var out = String(Api.Instance().executeCommand(npc.getWorld(), 'rwlinked ' + text));
    Log.info('LINKED2 CMD ' + text + ' -> ' + out);
    return out;
}
function point(x, y, z) { var p = new IA(3); p[0] = x; p[1] = y; p[2] = z; return p; }
function own(n, x, z, orientation, length, pos) {
    var mc = n.getMCEntity(), path = new AL();
    mc.ais.orientation = orientation;
    for (var i = 1; i <= length; i++) { path.add(point(x, 4, z + 5 * i)); }
    if (length > 0) { mc.ais.setMovingPath(path); }
    n.setHome(x, 4, z);
    mc.ais.movingPos = pos;
}
function fields(n) {
    var mc = n.getMCEntity(), tabs = mc.script.getScripts();
    return { home: n.getHomeX() + ',' + n.getHomeY() + ',' + n.getHomeZ(), orientation: mc.ais.orientation,
        pos: mc.ais.movingPos, name: String(n.getDisplay().getName()), texture: String(n.getDisplay().getSkinTexture()),
        health: n.getStats().getMaxHealth(), walking: mc.ais.walkingRange, minExp: mc.inventory.getExpMin(),
        scripts: tabs.size(), code: tabs.size() > 0 ? String(tabs.get(0).script) : '', enabled: mc.script.getEnabled(),
        x: n.getX(), z: n.getZ() };
}
function show(label, f) {
    Log.info('LINKED2 FIELDS mode=' + MODE + ' npc=' + label + ' home=' + f.home + ' orientation=' + f.orientation
        + ' pos=' + f.pos + ' name=' + f.name + ' maxHealth=' + f.health + ' walking=' + f.walking
        + ' minExp=' + f.minExp + ' scripts=' + f.scripts + ' enabled=' + f.enabled + ' x=' + f.x + ' z=' + f.z);
}
function role(n, name) { n.getStoreddata().put('m132role', name); n.getStoreddata().put('m132phase', MODE); }
function findNamed(npc) {
    var all = npc.getWorld().getAllEntities(2);
    for (var i = 0; i < all.length; i++) {
        var name = String(all[i].getName());
        if (name === 'M132A_' + MODE) { a = all[i]; } else if (name === 'M132B_' + MODE) { b = all[i]; }
        else if (name === 'M132C_' + MODE) { c = all[i]; }
    }
}
function findRoles(npc, phase) {
    var all = npc.getWorld().getAllEntities(2);
    for (var i = 0; i < all.length; i++) {
        var d = all[i].getStoreddata();
        if (!d.has('m132role') || String(d.get('m132phase')) !== phase) { continue; }
        var r = String(d.get('m132role'));
        if (r === 'A') { a = all[i]; } else if (r === 'B') { b = all[i]; } else { c = all[i]; }
    }
}
function link(n, group) {
    var mc = n.getMCEntity();
    mc.linkedName = group; L.Instance.loadNpcData(mc);
    check('linked-' + n.getName(), mc.linkedData !== null, mc.linkedName);
}
function classifyWriter(label, writer, category, except) {
    var keys = writer.getKeySet().iterator(), bad = '', count = 0;
    while (keys.hasNext()) {
        var key = String(keys.next()); count++;
        if (except && except[key] !== undefined) {
            if (Cat.categoryOf(key) !== except[key]) { bad += key + ' '; }
        } else if (Cat.categoryOf(key) !== category) { bad += key + ' '; }
    }
    check('classify-' + label, count > 0 && bad === '', count + ' keys, wrong: ' + bad);
}
function ownOrPathStart(label, f, x, z) { check(label + '-home-own', f.home === x + ',4,' + z, f.home); }
function setupWorkers(npc) {
    findNamed(npc);
    check('workers-found', a !== null && b !== null && c !== null, a + ' ' + b + ' ' + c);
    if (a === null || b === null || c === null) { end(npc); return false; }
    role(a, 'A'); role(b, 'B'); role(c, 'C');
    own(a, 0, 10, 270, 2, 0);
    own(b, 40, 10, 90, 3, 1);
    own(c, 20, 30, 180, 0, 0);
    startB = fields(b); startC = fields(c);
    show('B-before', startB); show('C-before', startC);
    return true;
}
function addScript(n) {
    var mc = n.getMCEntity(), tab = new SC(mc.script);
    tab.script = CODE;
    tab.console.put(java.lang.Long.valueOf(1), 'console text stays on this NPC');
    mc.script.getScripts().add(tab);
    mc.script.setLanguage('ECMAScript');
    mc.script.setEnabled(true);
}
function tickOff(npc) {
    var Link = L.Instance;
    if (age === 20) {
        check('groups-off', S.RwLinkedGroups === 0, S.RwLinkedGroups);
        check('own-off', S.RwLinkedOwnFields === 0, S.RwLinkedOwnFields);
        var commands = npc.getWorld().getMCWorld().getMinecraftServer().getCommandManager().getCommands();
        check('no-command-at-0', !commands.containsKey('rwlinked'), commands.containsKey('rwlinked'));
        setupWorkers(npc);
    } else if (age === 40) {
        Link.addData(GROUP); link(a, GROUP); link(b, GROUP); link(c, GROUP);
    } else if (age === 60) {
        a.getDisplay().setName('G_off'); a.getStats().setMaxHealth(77); addScript(a);
        Link.saveNpcData(a.getMCEntity());
    } else if (age === 140) {
        var fb = fields(b), fc = fields(c); show('B', fb); show('C', fc);
        check('off-B-name', fb.name === 'G_off', fb.name);
        check('off-B-health', fb.health === 77, fb.health);
        check('off-B-no-scripts', fb.scripts === 0, fb.scripts);
        check('off-B-orientation-overwritten', fb.orientation === 270, fb.orientation);
        check('off-B-home-pathstart', fb.home === '40,4,15', fb.home);
        // M13.1 left this unchecked: a member without a path gets its own home back from setMovingPath.
        ownOrPathStart('off-C-nopath', fc, 20, 30);
        check('off-C-name', fc.name === 'G_off', fc.name);
        var data = Link.getData(GROUP);
        check('off-no-previous', !Link.hasPrevious(data), Link.hasPrevious(data));
        check('off-no-scripts-in-group', !data.data.hasKey('Scripts'), data.data.hasKey('Scripts'));
        check('off-budget-untouched', Number(Link.budget.applied) === 0 && Number(Link.budget.nanos) === 0, Link.budget.applied);
        end(npc);
    }
}
function tickOn(npc) {
    var Link = L.Instance, data = Link.getData(GROUP), fb, fc, fa;
    if (age === 20) {
        check('groups-on', S.RwLinkedGroups === 1, S.RwLinkedGroups);
        check('own-off-first', S.RwLinkedOwnFields === 0, S.RwLinkedOwnFields);
        check('budget-default', S.RwLinkedBudget === 50, S.RwLinkedBudget);
        if (!setupWorkers(npc)) { return; }
        var mc = a.getMCEntity();
        var nameKeys = {Name: Cat.NAME, Title: Cat.NAME};
        classifyWriter('display', mc.display.writeToNBT(new NBT()), Cat.APPEARANCE, nameKeys);
        classifyWriter('stats', mc.stats.writeToNBT(new NBT()), Cat.STATS);
        classifyWriter('ai', mc.ais.writeToNBT(new NBT()), Cat.AI);
        classifyWriter('inventory', mc.inventory.writeEntityToNBT(new NBT()), Cat.INVENTORY);
        classifyWriter('advanced', mc.advanced.writeToNBT(new NBT()), Cat.ADVANCED);
        classifyWriter('transform', mc.transform.writeToNBT(new NBT()), Cat.TRANSFORM);
        classifyWriter('scripts', mc.script.writeToNBT(new NBT()), Cat.SCRIPTS);
    } else if (age === 40) {
        Link.addData(GROUP); link(a, GROUP); link(b, GROUP); link(c, GROUP);
        data = Link.getData(GROUP);
        check('default-categories', data.categories === Cat.DEFAULT, Cat.toText(data.categories));
        check('default-scripts-off', !Cat.has(data.categories, Cat.SCRIPTS), Cat.toText(data.categories));
        check('undo-nothing', cmd(npc, 'undo ' + GROUP).indexOf('done=0') >= 0, 'no version kept yet');
    } else if (age === 60) {
        check('command-sets', cmd(npc, 'categories ' + GROUP + ' appearance stats').indexOf('now=appearance,stats') >= 0, Cat.toText(data.categories));
        check('command-rejects', cmd(npc, 'categories ' + GROUP + ' wings').indexOf('rejected') >= 0, Cat.toText(data.categories));
        check('command-kept-after-reject', data.categories === (Cat.APPEARANCE | Cat.STATS), Cat.toText(data.categories));
        check('command-unknown-group', cmd(npc, 'undo nosuchgroup').indexOf('unknown') >= 0, 'nosuchgroup');
        // The link at age 40 ran with AI on and own fields off, which moved B's home (Q384). Set it again for the AI-off checks.
        check('link-default-B-home-pathstart', fields(b).home === '40,4,15', fields(b).home);
        b.setHome(40, 4, 10);
    } else if (age === 80) {
        a.getDisplay().setName('G_name1');
        a.getDisplay().setSkinTexture('customnpcs:textures/entity/humanmale/m132.png');
        a.getStats().setMaxHealth(77);
        a.getMCEntity().ais.walkingRange = 33;
        a.getMCEntity().inventory.setExp(7, 7);
        Link.saveNpcData(a.getMCEntity());
        check('first-save-keeps-no-empty-version', !Link.hasPrevious(data), Link.hasPrevious(data));
    } else if (age === 140) {
        fb = fields(b); fc = fields(c); show('B', fb); show('C', fc);
        check('cat-B-appearance-on', fb.texture.indexOf('m132.png') >= 0, fb.texture);
        check('cat-B-stats-on', fb.health === 77, fb.health);
        check('cat-C-stats-on', fc.health === 77, fc.health);
        check('cat-B-name-off', fb.name === startB.name, fb.name);
        check('cat-B-ai-off', fb.walking === startB.walking && fb.walking !== 33, fb.walking);
        check('cat-B-inventory-off', fb.minExp === startB.minExp && fb.minExp !== 7, fb.minExp);
        // RwLinkedOwnFields is 0 here: with AI off the member keeps all of it without setMovingPath moving the home.
        check('cat-B-home-ai-off', fb.home === '40,4,10', fb.home);
        check('cat-B-orientation-ai-off', fb.orientation === 90, fb.orientation);
        check('cat-B-pathpos-ai-off', fb.pos === 1, fb.pos);
        check('cat-group-is-complete', data.data.getString('Name') === 'G_name1' && data.data.getInteger('WalkingRange') === 33, data.data.getString('Name'));
    } else if (age === 160) {
        check('command-adds-scripts', cmd(npc, 'categories ' + GROUP + ' +scripts').indexOf('now=appearance,stats,scripts') >= 0, Cat.toText(data.categories));
        addScript(a);
        Link.saveNpcData(a.getMCEntity());
    } else if (age === 220) {
        fb = fields(b); fc = fields(c);
        check('scripts-B-code', fb.scripts === 1 && fb.code === CODE, fb.scripts);
        check('scripts-C-code', fc.scripts === 1 && fc.code === CODE, fc.scripts);
        check('scripts-B-enabled', fb.enabled === true, fb.enabled);
        check('scripts-B-runs', b.getStoreddata().has('m132ran') && b.getStoreddata().has('m132init'), b.getStoreddata().has('m132ran'));
        check('scripts-group-no-console', !data.data.getTagList('Scripts', 10).getCompoundTagAt(0).hasKey('Console'), 'group');
        check('scripts-B-no-console-copied', b.getMCEntity().script.getScripts().get(0).console.isEmpty(), 'member');
        tabB = b.getMCEntity().script.getScripts().get(0);
    } else if (age === 240) {
        a.getStats().setMaxHealth(88);
        Link.saveNpcData(a.getMCEntity());
    } else if (age === 300) {
        fb = fields(b);
        check('save-reaches-B', fb.health === 88, fb.health);
        check('same-script-kept-running', b.getMCEntity().script.getScripts().get(0) === tabB, 'same container');
        check('previous-kept', Link.hasPrevious(data), Link.hasPrevious(data));
        check('undo-1', cmd(npc, 'undo ' + GROUP).indexOf('done=1') >= 0, 'first undo');
    } else if (age === 360) {
        fb = fields(b); fa = fields(a);
        check('undo-1-B', fb.health === 77, fb.health);
        check('undo-1-edited-too', fa.health === 77, fa.health);
        check('undo-2', cmd(npc, 'undo ' + GROUP).indexOf('done=1') >= 0, 'undo of the undo');
    } else if (age === 420) {
        fb = fields(b);
        check('undo-2-B-change-back', fb.health === 88, fb.health);
        S.RwLinkedOwnFields = 1;
        time0 = data.time;
        var keep = data.data.copy();
        a.getMCEntity().ais.movingPos = 1; a.getMCEntity().ais.orientation = 180;
        Link.saveNpcData(a.getMCEntity());
        check('walking-is-no-change', Number(data.time) === Number(time0) && data.data.equals(keep), data.time - time0);
    } else if (age === 440) {
        check('command-none', cmd(npc, 'categories ' + GROUP + ' none').indexOf('now=none') >= 0, Cat.toText(data.categories));
        check('categories-bump-time', data.time > time0, data.time - time0);
        a.getDisplay().setName('G_name2');
        Link.saveNpcData(a.getMCEntity());
    } else if (age === 500) {
        fb = fields(b);
        check('none-B-name-own', fb.name === startB.name, fb.name);
        check('none-B-health-kept', fb.health === 88, fb.health);
        check('command-default', cmd(npc, 'categories ' + GROUP + ' default').indexOf('now=appearance,name,stats,ai,inventory,advanced,transform') >= 0, Cat.toText(data.categories));
    } else if (age === 560) {
        fb = fields(b); fc = fields(c); show('B', fb); show('C', fc);
        check('default-B-name', fb.name === 'G_name2', fb.name);
        check('default-B-ai', fb.walking === 33, fb.walking);
        check('default-B-inventory', fb.minExp === 7, fb.minExp);
        check('default-B-scripts-stay', fb.scripts === 1, fb.scripts);
        // Own fields are on since age 420.
        check('default-B-home-own', fb.home === '40,4,10', fb.home);
        check('default-B-orientation-own', fb.orientation === 90, fb.orientation);
        check('default-B-pathpos-own', fb.pos === 1, fb.pos);
        ownOrPathStart('default-C-nopath', fc, 20, 30);
        check('default-C-orientation-own', fc.orientation === 180, fc.orientation);
        check('default-B-stays', Math.abs(fb.x - startB.x) < 1.5 && Math.abs(fb.z - startB.z) < 1.5, (fb.x - startB.x) + ',' + (fb.z - startB.z));
        check('group-list', cmd(npc, 'info ' + GROUP).indexOf('loaded=3') >= 0, 'three members');
        check('command-stats', cmd(npc, 'categories ' + GROUP + ' stats').indexOf('now=stats') >= 0, Cat.toText(data.categories));
        end(npc);
    }
}
function tickReload(npc) {
    var Link = L.Instance, data = Link.getData(GROUP), fb;
    if (age === 40) {
        check('reload-switches', S.RwLinkedGroups === 1 && S.RwLinkedOwnFields === 1, S.RwLinkedGroups + ',' + S.RwLinkedOwnFields);
        findRoles(npc, 'on');
        check('reload-found', a !== null && b !== null && c !== null, a + ' ' + b + ' ' + c);
        if (a === null || b === null || c === null) { end(npc); return; }
        check('reload-categories-from-file', data.categories === Cat.STATS, Cat.toText(data.categories));
        fb = fields(b); show('B', fb);
        // The harness changed name and health in the group file while the server was down.
        check('reload-stats-applied', fb.health === 99, fb.health);
        check('reload-name-off', fb.name === 'G_name2', fb.name);
        check('reload-group-has-file-name', data.data.getString('Name') === 'G_file', data.data.getString('Name'));
        check('reload-home-own', fb.home === '40,4,10', fb.home);
        check('reload-member-script', fb.scripts === 1 && fb.code === CODE, fb.scripts);
        check('reload-previous-kept', Link.hasPrevious(data), Link.hasPrevious(data));
        check('reload-undo', cmd(npc, 'undo ' + GROUP).indexOf('done=1') >= 0, 'undo after restart');
    } else if (age === 100) {
        fb = fields(b);
        check('reload-undo-B', fb.health === 88, fb.health);
        // The kept version was saved with the scripts category on; it went through the file and a restart.
        check('reload-group-script-survives-file', String(data.data.getTagList('Scripts', 10).getCompoundTagAt(0).getString('Script')) === CODE, 'quotes and line breaks');
        end(npc);
    }
}
function tickBudget(npc) {
    var Link = L.Instance, world = npc.getWorld(), i;
    if (age === 40) {
        check('budget-switch', S.RwLinkedGroups === 1 && S.RwLinkedBudget === 50, S.RwLinkedBudget);
        var clones = Api.Instance().getClones();
        for (i = 0; i < 200; i++) {
            workers.push(clones.spawn(5.5 + 2 * (i % 15), 4, 30.5 + 2 * Math.floor(i / 15), 1, 'M132W', world));
        }
        check('budget-spawned', workers.length === 200 && workers[199] !== null, workers.length);
    } else if (age === 60) {
        Link.addData(GROUP);
        for (i = 0; i < workers.length; i++) {
            workers[i].getMCEntity().linkedName = GROUP; Link.loadNpcData(workers[i].getMCEntity());
        }
    } else if (age === 80) {
        var bud = Link.budget;
        applied0 = bud.applied; deferred0 = bud.deferred; nanos0 = bud.nanos; savedAt = age;
        workers[0].getDisplay().setName('G_big');
        Link.saveNpcData(workers[0].getMCEntity());
    } else if (age > 80) {
        var count = 0;
        for (i = 0; i < workers.length; i++) {
            if (String(workers[i].getDisplay().getName()) === 'G_big') { count++; }
        }
        if (count < 200 && age < savedAt + 600) { return; }
        var b2 = Link.budget, applied = b2.applied - applied0, deferred = b2.deferred - deferred0;
        var ticks = npc.getWorld().getMCWorld().getMinecraftServer().tickTimeArray, max = 0;
        for (i = 0; i < 100; i++) { max = Math.max(max, ticks[i]); }
        var micros = applied > 0 ? Math.round((b2.nanos - nanos0) / applied / 1000) : 0;
        Log.info('LINKED2 DATA budget members=' + count + ' ticksToAll=' + (age - savedAt) + ' applied=' + applied
            + ' deferred=' + deferred + ' maxPerTick=' + b2.maxPerTick + ' microsPerTransfer=' + micros
            + ' maxTickMsLast100=' + (max / 1000000));
        check('budget-all-reached', count === 200, count);
        check('budget-limit-held', b2.maxPerTick <= 50 && b2.maxPerTick > 0, b2.maxPerTick);
        check('budget-spread-over-ticks', deferred > 0, deferred);
        end(npc);
    }
}
function tick(e) {
    var npc = e.npc;
    if (done || npc.getStoreddata().has('m132done')) { return; }
    age += 10;
    if (MODE === 'off') { tickOff(npc); }
    else if (MODE === 'on') { tickOn(npc); }
    else if (MODE === 'reload') { tickReload(npc); }
    else { tickBudget(npc); }
}
