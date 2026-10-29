// M6.6 disposable fixture: nested ownership through real chest/projectile wrappers.
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m66')) { return; }
    d.put('m66', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.ContextProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@, @@CONTEXT_FIXED@@);
        System.out.println('M66 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M66 FAILED ' + error);
    }
}
