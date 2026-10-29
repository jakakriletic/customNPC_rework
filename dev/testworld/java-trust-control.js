// M6.8 disposable server fixture; never installed into an owner's world.
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m68')) { return; }
    d.put('m68', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.TrustProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('M68 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M68 FAILED ' + error);
    }
}
