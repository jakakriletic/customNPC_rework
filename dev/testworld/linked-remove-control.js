// Q459 evidence only. The harness prepends: var MODE = 'seed' | 'offafter' | 'on' | 'onafter';
// Removal calls the same controller method as the LinkedRemove packet.
var L = Java.type('noppes.npcs.controllers.LinkedNpcController');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var F = Java.type('java.io.File');
var age = 0, checks = 0, failed = 0, done = false;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('LINKEDRM CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function file(name) { return new F(L.Instance.getDir(), name); }
function end(npc) {
    done = true;
    Log.info('LINKEDRM DONE mode=' + MODE + ' checks=' + checks + ' failed=' + failed);
    npc.despawn();
}
function tick(e) {
    var npc = e.npc, Link = L.Instance;
    if (done) { return; }
    age += 10;
    if (age !== 40) { return; }
    if (MODE === 'seed') {
        check('seed-switch', S.RwLinkedGroups === 0, S.RwLinkedGroups);
        Link.addData('q459off'); Link.addData('q459on'); Link.addData('q459keep');
        check('seed-files', file('q459off.json').isFile() && file('q459on.json').isFile() && file('q459keep.json').isFile(), 'three group files');
        Link.removeData('Q459OFF');
        check('seed-removed-in-memory', Link.getData('q459off') === null, 'getData');
        // The original bug (Q393): the file stays on disk.
        check('seed-original-keeps-file', file('q459off.json').isFile(), file('q459off.json').isFile());
    } else if (MODE === 'offafter') {
        check('offafter-switch', S.RwLinkedGroups === 0, S.RwLinkedGroups);
        check('offafter-original-group-back', Link.getData('q459off') !== null, 'removed group reloaded from its file');
    } else if (MODE === 'on') {
        check('on-switch', S.RwLinkedGroups === 1, S.RwLinkedGroups);
        check('on-group-present', Link.getData('q459on') !== null, 'before removal');
        check('on-prev-present', file('q459on.json_prev').isFile(), 'kept version seeded by the harness');
        Link.removeData('Q459ON');
        check('on-removed-in-memory', Link.getData('q459on') === null, 'getData');
        check('on-file-deleted', !file('q459on.json').exists(), file('q459on.json').exists());
        check('on-prev-deleted', !file('q459on.json_prev').exists(), file('q459on.json_prev').exists());
        check('on-other-group-kept', Link.getData('q459keep') !== null && file('q459keep.json').isFile(), 'q459keep');
    } else {
        check('onafter-switch', S.RwLinkedGroups === 1, S.RwLinkedGroups);
        check('onafter-group-stays-gone', Link.getData('q459on') === null, 'after restart');
        check('onafter-files-still-absent', !file('q459on.json').exists() && !file('q459on.json_prev').exists(), 'after restart');
        check('onafter-other-groups', Link.getData('q459keep') !== null && Link.getData('q459off') !== null, 'q459keep, q459off');
    }
    end(npc);
}
