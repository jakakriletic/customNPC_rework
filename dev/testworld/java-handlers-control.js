// P5 disposable fixture only. Runs the player/forge/block/door/item Java probe once; the tempdata
// guard keeps it to one run.
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('p5')) { return; }
    d.put('p5', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.HandlerJavaProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('P5 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('P5 FAILED ' + error);
    }
}
