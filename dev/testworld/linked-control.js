// M13.1 evidence only. The harness prepends: var MODE = 'off' | 'on' | 'reload';
// Linking and saving call the same controller methods as the LinkedSet and NpcMenuClose packets.
var L = Java.type('noppes.npcs.controllers.LinkedNpcController');
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var AL = Java.type('java.util.ArrayList');
var IA = Java.type('int[]');
var owner = 'uuid:00000000-0000-4000-8000-000000000131';
var age = 0, checks = 0, failed = 0, done = false, a = null, b = null, start = null;
var side = { off: 0, on: 10 };
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('LINKED CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function end(npc) {
    done = true; npc.getStoreddata().put('m131done', 1);
    Log.info('LINKED DONE mode=' + MODE + ' checks=' + checks + ' failed=' + failed);
}
function point(x, y, z) { var p = new IA(3); p[0] = x; p[1] = y; p[2] = z; return p; }
function own(n, x, z, orientation, length, pos) {
    var mc = n.getMCEntity(), path = new AL();
    mc.ais.orientation = orientation;
    for (var i = 1; i <= length; i++) { path.add(point(x, 4, z + 5 * i)); }
    // DataAI.setMovingPath moves the home to the first path point, so the home goes last.
    mc.ais.setMovingPath(path);
    n.setHome(x, 4, z);
    mc.ais.movingPos = pos;
}
function fields(n) {
    var mc = n.getMCEntity(), path = mc.ais.getMovingPath(), first = path.size() > 0 ? path.get(0) : null;
    return { home: n.getHomeX() + ',' + n.getHomeY() + ',' + n.getHomeZ(), orientation: mc.ais.orientation,
        length: path.size(), first: first ? first[0] + ',' + first[1] + ',' + first[2] : '', pos: mc.ais.movingPos,
        name: String(n.getDisplay().getName()), health: n.getStats().getMaxHealth(), army: String(A.armyOf(n)),
        nav: mc.getRwNavBackend(), linked: String(mc.linkedName), x: n.getX(), z: n.getZ() };
}
function show(label, f) {
    Log.info('LINKED FIELDS mode=' + MODE + ' npc=' + label + ' home=' + f.home + ' orientation=' + f.orientation
        + ' path=' + f.length + '@' + f.first + ' pos=' + f.pos + ' name=' + f.name + ' maxHealth=' + f.health
        + ' army=' + f.army + ' nav=' + f.nav + ' linked=' + f.linked + ' x=' + f.x + ' z=' + f.z);
}
function find(npc, phase) {
    var all = npc.getWorld().getAllEntities(2);
    a = null; b = null;
    for (var i = 0; i < all.length; i++) {
        var d = all[i].getStoreddata();
        if (!d.has('m131role') || String(d.get('m131phase')) !== phase) { continue; }
        if (String(d.get('m131role')) === 'A') { a = all[i]; } else { b = all[i]; }
    }
}
// Q384: the original loadNpcData ends with setMovingPath, which moves a pathed member's home to its own
// first path point; only RwLinkedOwnFields=1 puts the member's own home back.
function expectHome(label, f, x, z, unlinked) {
    if (MODE === 'off' && !unlinked) { check('off-' + label + '-home-pathstart', f.home === x + ',4,' + (z + 5), f.home); }
    else { check(label + '-home-own', f.home === x + ',4,' + z, f.home); }
}
function expectOwn(label, f, z, unlinked) {
    expectHome(label, f, 40, z, unlinked);
    check(label + '-orientation-own', f.orientation === 90, f.orientation);
    check(label + '-path-own', f.length === 3 && f.first === '40,4,' + (z + 5), f.length + '@' + f.first);
    check(label + '-pathpos-own', f.pos === 1, f.pos);
}
function expectEntityTags(label, f, phase) {
    check(label + '-army-kept', f.army === 'm131' + phase, f.army);
    check(label + '-navbackend-kept', f.nav === 1, f.nav);
}
function tick(e) {
    var npc = e.npc;
    if (done || npc.getStoreddata().has('m131done')) { return; }
    age += 10;
    var Link = L.Instance, z = side[MODE] === undefined ? 10 : side[MODE];
    if (MODE === 'reload') {
        if (age < 40) { return; }
        check('switch-on', S.RwLinkedOwnFields === 1, S.RwLinkedOwnFields);
        find(npc, 'on');
        check('reload-found', a !== null && b !== null, a + ' ' + b);
        if (a === null || b === null) { end(npc); return; }
        var fa = fields(a), fb = fields(b); show('A', fa); show('B', fb);
        // The harness renamed the group in m131on.json while the server was down: only the load-time merge brings it.
        check('reload-group-name-applied-B', fb.name === 'M131File_on', fb.name);
        check('reload-group-name-applied-A', fa.name === 'M131File_on', fa.name);
        check('reload-shared-health', fb.health === 77, fb.health);
        check('reload-still-linked', fb.linked === 'm131on', fb.linked);
        expectOwn('reload-B', fb, 10);
        expectEntityTags('reload-B', fb, 'on');
        check('reload-A-home-own', fa.home === '0,4,10', fa.home);
        find(npc, 'off');
        if (a !== null && b !== null) {
            var oa = fields(a), ob = fields(b); show('offA', oa); show('offB', ob);
            Log.info('LINKED DATA off-member-after-reload home=' + ob.home + ' orientation=' + ob.orientation + ' pos=' + ob.pos);
        }
        end(npc); return;
    }
    if (age === 20) {
        check('switch-value', S.RwLinkedOwnFields === (MODE === 'on' ? 1 : 0), S.RwLinkedOwnFields);
        check('armies-on', A.enabled(), A.enabled());
        var all = npc.getWorld().getAllEntities(2);
        for (var i = 0; i < all.length; i++) {
            var name = String(all[i].getName());
            if (name === 'M131A_' + MODE) { a = all[i]; } else if (name === 'M131B_' + MODE) { b = all[i]; }
        }
        check('workers-found', a !== null && b !== null, a + ' ' + b);
        if (a === null || b === null) { end(npc); return; }
        a.getStoreddata().put('m131role', 'A'); a.getStoreddata().put('m131phase', MODE);
        b.getStoreddata().put('m131role', 'B'); b.getStoreddata().put('m131phase', MODE);
        own(a, 0, z, 270, 2, 0);
        own(b, 40, z, 90, 3, 1);
        check('army-create', A.create('m131' + MODE, owner), 'm131' + MODE);
        check('army-assign-B', A.assign(b, 'm131' + MODE), b.getUUID());
        b.getMCEntity().setRwNavBackend(1);
        start = fields(b); show('B-before', start);
        expectOwn('before-B', start, z, true);
        return;
    }
    if (age === 40) {
        Link.addData('m131' + MODE);
        a.getMCEntity().linkedName = 'm131' + MODE; Link.loadNpcData(a.getMCEntity());
        b.getMCEntity().linkedName = 'm131' + MODE; Link.loadNpcData(b.getMCEntity());
        check('linked-A', a.getMCEntity().linkedData !== null, a.getMCEntity().linkedName);
        check('linked-B', b.getMCEntity().linkedData !== null, b.getMCEntity().linkedName);
        var fb = fields(b); show('B-linked', fb);
        expectOwn('linked-empty-group-B', fb, z);
        return;
    }
    if (age === 60) {
        a.getDisplay().setName('M131Shared_' + MODE);
        a.getStats().setMaxHealth(77);
        // Shared AI flag: with the original merge B then walks to the edited NPC's home.
        a.getMCEntity().ais.setReturnsHome(true);
        Link.saveNpcData(a.getMCEntity());
        Log.info('LINKED SAVED mode=' + MODE + ' time=' + a.getMCEntity().linkedData.time
            + ' lastB=' + b.getMCEntity().linkedLast);
        return;
    }
    if (age === 140) {
        var fa = fields(a), fb = fields(b); show('A', fa); show('B', fb);
        // Shared data reaches the other loaded member through the original tick path in both modes.
        check('shared-name-B', fb.name === 'M131Shared_' + MODE, fb.name);
        check('shared-health-B', fb.health === 77, fb.health);
        expectHome('A', fa, 0, z);
        check('A-orientation-own', fa.orientation === 270, fa.orientation);
        expectEntityTags('B', fb, MODE);
        if (MODE === 'off') {
            // Original behaviour, the M13.0 defect: B takes the edited NPC's orientation and path index; its
            // home is reset to its own first path point, not A's (Q384).
            check('off-B-home-not-A', fb.home !== '0,4,' + z && fb.home !== '0,4,' + (z + 5), fb.home);
            expectHome('B-after-save', fb, 40, z);
            check('off-B-orientation-overwritten', fb.orientation === 270, fb.orientation);
            check('off-B-pathpos-overwritten', fb.pos === 0, fb.pos);
            check('off-B-path-kept-original', fb.length === 3, fb.length);
        } else {
            expectOwn('on-B', fb, z);
        }
        check('shared-return-home-B', b.getMCEntity().ais.getReturnsHome() === true, b.getMCEntity().ais.getReturnsHome());
        return;
    }
    if (age === 400) {
        var fm = fields(b), dx = fm.x - start.x, dz = fm.z - start.z;
        Log.info('LINKED DATA moved mode=' + MODE + ' dx=' + dx + ' dz=' + dz + ' home=' + fm.home);
        if (MODE === 'on') { check('on-B-stays-home', Math.abs(dx) < 1.5 && Math.abs(dz) < 1.5, dx + ',' + dz); }
        end(npc);
    }
}
