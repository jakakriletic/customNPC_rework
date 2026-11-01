// Disposable P7a controller fixture. Production hooks run on the Forge world tick END observer.
var p7probe = null;
function tick(e) {
    if (e.npc.getTempdata().has('p7')) { return; }
    e.npc.getTempdata().put('p7', 1);
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.AnimationJournalProbe');
        p7probe = new Probe(e.npc, @@HOOKS@@, @@OUTPUT@@);
        e.npc.getTimers().start(7701, 0, true);
    } catch (err) { Java.type('java.lang.System').out.println('P7 FAILED ' + err); }
}
function timer(e) {
    if (e.id !== 7701 || p7probe === null) { return; }
    try { if (p7probe.step()) { e.npc.getTimers().stop(7701); } }
    catch (err) { Java.type('java.lang.System').out.println('P7 FAILED ' + err); }
}
